/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.MailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZSharedFileInputStream;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.AddInviteData;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class CalendarRequest extends MailDocumentHandler {

    private MailItem.Type type = MailItem.Type.UNKNOWN;
    protected MailItem.Type getItemType() {
        return type;
    }

    public CalendarRequest() {
        if (this instanceof AppointmentRequest) {
            type = MailItem.Type.APPOINTMENT;
        } else if (this instanceof TaskRequest) {
            type = MailItem.Type.TASK;
        }
    }

    protected static class CalSendData extends ParseMimeMessage.MimeMessageData {
        ItemId mOrigId; // orig id if this is a reply
        String mReplyType;
        String mIdentityId;
        MimeMessage mMm;
        Invite mInvite;
        boolean mDontNotifyAttendees;
        AddInviteData mAddInvData;
    }

    /**
     *
     * parses an <m> element using the passed-in InviteParser
     *
     * @param zsc
     * @param octxt
     * @param msgElem
     * @param acct
     * @param mbox
     * @param inviteParser
     * @return
     * @throws ServiceException
     */
    protected static CalSendData handleMsgElement(ZimbraSoapContext zsc, OperationContext octxt, Element msgElem,
                                                  Account acct, Mailbox mbox, ParseMimeMessage.InviteParser inviteParser)
    throws ServiceException {
        CalSendData csd = new CalSendData();

        assert(inviteParser.getResult() == null);

        // check to see if this message is a reply -- if so, then we'll want to note that so
        // we can more-correctly match the conversations up
        String origId = msgElem.getAttribute(MailConstants.A_ORIG_ID, null);
        csd.mOrigId = origId == null ? null : new ItemId(origId, zsc);
        csd.mReplyType = msgElem.getAttribute(MailConstants.A_REPLY_TYPE, MailSender.MSGTYPE_REPLY);
        csd.mIdentityId = msgElem.getAttribute(MailConstants.A_IDENTITY_ID, null);

        // parse the data
        csd.mMm = ParseMimeMessage.parseMimeMsgSoap(zsc, octxt, mbox, msgElem, null, inviteParser, csd);

        // FIXME FIXME FIXME -- need to figure out a way to get the FRAGMENT data out of the initial
        // message here, so that we can copy it into the DESCRIPTION field in the iCalendar data that
        // goes out...will make for much better interop!

        assert(inviteParser.getResult() != null);

        csd.mInvite = inviteParser.getResult().mInvite;

        return csd;
    }

    protected static String getOrigHtml(MimeMessage mm, String defaultCharset) throws ServiceException {
        try {
            for (MPartInfo mpi : Mime.getParts(mm)) {
                if (mpi.getContentType().equals(MimeConstants.CT_TEXT_HTML))
                    return Mime.getStringContent(mpi.getMimePart(), defaultCharset);
            }
            return null;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException "+e, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException "+e, e);
        }
    }

    protected static void patchCalendarURLs(MimeMessage mm, String htmlStr, String localURL, String orgAddress, String uid, String attendee, String invId) throws ServiceException
    {
        try {
            boolean changed = false;

            String accept = buildUrl(localURL, orgAddress, uid, attendee, invId, "ACCEPT");
            String decline = buildUrl(localURL, orgAddress, uid, attendee, invId, "DECLINE");
            String tentative = buildUrl(localURL, orgAddress, uid, attendee, invId, "TENTATIVE");

            for (MPartInfo mpi : Mime.getParts(mm)) {
                if (mpi.getContentType().equals(MimeConstants.CT_TEXT_HTML)) {
                    String str = htmlStr;

                    str = str.replaceFirst("href=\"@@ACCEPT@@\"", accept);
                    str = str.replaceFirst("href=\"@@DECLINE@@\"", decline);
                    str = str.replaceFirst("href=\"@@TENTATIVE@@\"", tentative);

                    System.out.println(str);
                    mpi.getMimePart().setContent(str, MimeConstants.CT_TEXT_HTML);
                    changed = true;

                    break; // only match one part
                }
            }

            if (changed) {
                mm.saveChanges();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException "+e, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException "+e, e);
        }
    }

    protected static String buildUrl(String localURL, String orgAddress, String uid, String attendee, String invId, String verb)
    {
        StringBuffer toRet = new StringBuffer("href=\"").append(localURL);
        toRet.append("/service/pubcal/reply?org=").append(orgAddress);
        toRet.append("&uid=").append(uid);
        toRet.append("&at=").append(attendee);
        toRet.append("&v=").append(verb);
        toRet.append("&invId=").append(invId);
        toRet.append('\"');

        return toRet.toString();
    }

    protected static Element sendCalendarMessage(
        ZimbraSoapContext zsc,
        OperationContext octxt,
        int apptFolderId,
        Account acct,
        Mailbox mbox,
        CalSendData csd,
        Element response,
        MailSendQueue sendQueue)
    throws ServiceException {
        return sendCalendarMessage(zsc, octxt, apptFolderId, acct, mbox, csd, response, true, true, sendQueue, false);
    }

    protected static Element sendCalendarMessage(
        ZimbraSoapContext zsc,
        OperationContext octxt,
        int apptFolderId,
        Account acct,
        Mailbox mbox,
        CalSendData csd,
        Element response,
        MailSendQueue sendQueue,
        boolean updatePrevFolders)
    throws ServiceException {
        return sendCalendarMessage(zsc, octxt, apptFolderId, acct, mbox, csd, response, true, true, sendQueue, updatePrevFolders);
    }

    protected static Element sendCalendarMessage(
            ZimbraSoapContext zsc,
            OperationContext octxt,
            int apptFolderId,
            Account acct,
            Mailbox mbox,
            CalSendData csd,
            Element response,
            boolean updateOwnAppointment,
            boolean forceSend,
            MailSendQueue sendQueue)
        throws ServiceException {
        return sendCalendarMessage(zsc, octxt, apptFolderId, acct, mbox,
                csd, response, updateOwnAppointment,
                forceSend, sendQueue, false);
    }

    protected static Element sendCalendarMessage(
            ZimbraSoapContext zsc,
            OperationContext octxt,
            int apptFolderId,
            Account acct,
            Mailbox mbox,
            CalSendData csd,
            Element response,
            boolean updateOwnAppointment,
            boolean forceSend,
            MailSendQueue sendQueue,
            boolean updatePrevFolders)
        throws ServiceException {
            return sendCalendarMessageInternal(zsc, octxt, apptFolderId,
                                               acct, mbox, csd, response,
                                               updateOwnAppointment, forceSend,
                                               sendQueue, updatePrevFolders);
        }

    /**
     * Send a cancellation iCalendar email and optionally cancel sender's
     * appointment.
     * @param zsc
     * @param apptFolderId
     * @param acct
     * @param mbox
     * @param csd
     * @param cancelOwnAppointment if true, sender's appointment is canceled.
     *                             if false, sender's appointment is not
     *                             canceled. (this may be appropriate when
     *                             sending out cancellation message to
     *                             removed attendees)
     * @return
     * @throws ServiceException
     */
    protected static Element sendCalendarCancelMessage(
        ZimbraSoapContext zsc,
        OperationContext octxt,
        int apptFolderId,
        Account acct,
        Mailbox mbox,
        CalSendData csd,
        boolean cancelOwnAppointment,
        MailSendQueue sendQueue)
    throws ServiceException {
        return sendCalendarMessageInternal(zsc, octxt, apptFolderId, acct, mbox, csd,
                                           null, cancelOwnAppointment, true, sendQueue);
    }

    private static Element sendCalendarMessageInternal(
        ZimbraSoapContext zsc,
        OperationContext octxt,
        int apptFolderId,
        Account acct,
        Mailbox mbox,
        CalSendData csd,
        Element response,
        boolean updateOwnAppointment,
        boolean forceSend,
        MailSendQueue sendQueue)
    throws ServiceException {
        return sendCalendarMessageInternal(zsc, octxt, apptFolderId, acct,
                mbox, csd, response, updateOwnAppointment, forceSend, sendQueue,
                false);
    }

    /**
     * Send an iCalendar email message and optionally create/update/cancel
     * corresponding appointment/invite in sender's calendar.
     * @param zsc
     * @param apptFolderId
     * @param acct
     * @param mbox
     * @param csd
     * @param response
     * @param updateOwnAppointment if true, corresponding change is made to
     *                             sender's calendar
     * @param forceSend
     * @param sendQueue
     * @param updatePrevFolders if true, updates prevFolders field with Trash in database
     * @return
     * @throws ServiceException
     */
    private static Element sendCalendarMessageInternal(
            ZimbraSoapContext zsc,
            OperationContext octxt,
            int apptFolderId,
            Account acct,
            Mailbox mbox,
            CalSendData csd,
            Element response,
            boolean updateOwnAppointment,
            boolean forceSend,
            MailSendQueue sendQueue,
            boolean updatePrevFolders)
        throws ServiceException {
        boolean onBehalfOf = isOnBehalfOfRequest(zsc);
        boolean notifyOwner = onBehalfOf && acct.getBooleanAttr(Provisioning.A_zimbraPrefCalendarNotifyDelegatedChanges, false);
        if (notifyOwner) {
            try {
                InternetAddress addr = AccountUtil.getFriendlyEmailAddress(acct);
                csd.mMm.addRecipient(javax.mail.Message.RecipientType.TO, addr);
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("count not add calendar owner to recipient list", e);
            }
        }

        // Never send a notification to the person making the SOAP request
        // in a non-delegated request.
        if (!onBehalfOf) {
            String[] aliases = acct.getMailAlias();
            String[] addrs;
            if (aliases != null && aliases.length > 0) {
                addrs = new String[aliases.length + 1];
                addrs[0] = acct.getAttr(Provisioning.A_mail);
                for (int i = 0; i < aliases.length; i++) {
                    addrs[i + 1] = aliases[i];
                }
            } else {
                addrs = new String[1];
                addrs[0] = acct.getAttr(Provisioning.A_mail);
            }
            try {
                Mime.removeRecipients(csd.mMm, addrs);
            } catch (MessagingException e) {}
        }

        ParsedMessage pm = new ParsedMessage(csd.mMm, false);

        if (csd.mInvite.getFragment() == null || csd.mInvite.getFragment().equals("")) {
            csd.mInvite.setFragment(pm.getFragment(acct.getLocale()));
        }

        boolean willNotify = false;
        if (!csd.mDontNotifyAttendees) {
            try {
                Address[] rcpts = csd.mMm.getAllRecipients();
                willNotify = rcpts != null && rcpts.length > 0;
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
            }
        }

        // Validate the addresses first.
        if (!csd.mInvite.isCancel() && !forceSend && willNotify) {
            try {
                MailUtil.validateRcptAddresses(JMSession.getSmtpSession(mbox.getAccount()), csd.mMm.getAllRecipients());
            } catch (MessagingException mex) {
                if (mex instanceof SendFailedException) {
                    SendFailedException sfex = (SendFailedException) mex;
                    throw MailServiceException.SEND_ABORTED_ADDRESS_FAILURE("invalid addresses", sfex, sfex.getInvalidAddresses(), sfex.getValidUnsentAddresses());
                }
            }
        }

        AddInviteData aid = null;
        File tempMmFile = null;
        boolean queued = false;
        try {
            if (willNotify) {
                // Write out the MimeMessage to a temp file and create a new MimeMessage from the file.
                // If we don't do this, we get into trouble during modify appointment call.  If the blob
                // is bigger than the streaming threshold (e.g. appointment has a big attachment), the
                // MimeMessage object is attached to the current blob file.  But the Mailbox.addInvite()
                // call below updates the blob to a new mod_content (hence new path).  The attached blob
                // thus having been deleted, the MainSender.sendMimeMessage() call that follows will attempt
                // to read from a non-existent file and fail.  We can avoid this situation by writing the
                // to-be-emailed mime message to a temp file, thus detaching it from the appointment's
                // current blob file.  This is inefficient, but safe.
                OutputStream os = null;
                InputStream is = null;
                try {
                    tempMmFile = File.createTempFile("zcal", "tmp");

                    os = new FileOutputStream(tempMmFile);
                    csd.mMm.writeTo(os);
                    ByteUtil.closeStream(os);
                    os = null;

                    is = new ZSharedFileInputStream(tempMmFile);
                    csd.mMm = new FixedMimeMessage(JMSession.getSmtpSession(acct), is);
                } catch (IOException e) {
                    if (tempMmFile != null)
                        tempMmFile.delete();
                    throw ServiceException.FAILURE("error creating calendar message content", e);
                } catch (MessagingException e) {
                    if (tempMmFile != null)
                        tempMmFile.delete();
                    throw ServiceException.FAILURE("error creating calendar message content", e);
                } finally {
                    ByteUtil.closeStream(os);
                    ByteUtil.closeStream(is);
                }
            }

            // First, update my own appointment.  It is important that this happens BEFORE the call to send email,
            // because email send will delete uploaded attachments as a side-effect.
            if (updateOwnAppointment) {
                aid = mbox.addInvite(octxt, csd.mInvite, apptFolderId, pm, updatePrevFolders);
            }

            // Next, notify any attendees.
            if (willNotify) {
                MailSendQueueEntry entry = new MailSendQueueEntry(octxt, mbox, csd, tempMmFile);
                sendQueue.add(entry);
                queued = true;
            }
        } finally {
            // Delete the temp file if it wasn't queued.
            if (tempMmFile != null && !queued) {
                tempMmFile.delete();
            }
        }

        if (updateOwnAppointment && response != null && aid != null) {
            csd.mAddInvData = aid;
            ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
            String id = ifmt.formatItemId(aid.calItemId);
            response.addAttribute(MailConstants.A_CAL_ID, id);
            if (csd.mInvite.isEvent())
                response.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, id);  // for backward compat
            response.addAttribute(MailConstants.A_CAL_INV_ID, ifmt.formatItemId(aid.calItemId, aid.invId));
            if (Invite.isOrganizerMethod(csd.mInvite.getMethod())) {
                response.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, aid.modSeq);
                response.addAttribute(MailConstants.A_REVISION, aid.rev);
            }
        }

        return response;
    }

    protected static Element echoAddedInvite(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Mailbox mbox,
                                             AddInviteData aid, int maxSize, boolean wantHtml, boolean neuter)
    throws ServiceException {
        CalendarItem calItem = mbox.getCalendarItemById(octxt, aid.calItemId);
        Invite inv = calItem.getInvite(aid.invId, aid.compNum);
        String recurIdZ = null;
        if (inv != null && inv.getRecurId() != null)
            recurIdZ = inv.getRecurId().getDtZ();
        ItemId iid = new ItemId(calItem, aid.invId);
        Element echoElem = parent.addElement(MailConstants.E_CAL_ECHO);
        ToXML.encodeInviteAsMP(echoElem, ifmt, octxt, calItem, recurIdZ, iid, null, maxSize, wantHtml, neuter, null, false, false);
        return echoElem;
    }

    protected static void sendOrganizerChangeMessage(ZimbraSoapContext zsc, OperationContext octxt,
            CalendarItem calItem, Account acct, Mailbox mbox, MailSendQueue sendQueue) {
        try {
            Account authAccount = getAuthenticatedAccount(zsc);
            Invite[] invites = calItem.getInvites();
            for (Invite inv : invites) {
                List<Address> rcpts = CalendarMailSender.toListFromAttendees(inv.getAttendees());
                if (rcpts.size() > 0) {
                    CalSendData csd = new CalSendData();
                    csd.mInvite = inv;
                    csd.mOrigId = new ItemId(mbox, inv.getMailItemId());
                    csd.mMm = CalendarMailSender.createOrganizerChangeMessage(
                            acct, authAccount, zsc.isUsingAdminPrivileges(), calItem, csd.mInvite, rcpts);
                    sendCalendarMessageInternal(zsc, octxt, calItem.getFolderId(), acct, mbox, csd,
                                                null, false, true, sendQueue);
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Ignoring error while sending organizer change message", e);
        }
    }

    private static String getAttendeesAddressList(List<ZAttendee> list) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (ZAttendee a : list) {
            if (i > 0) sb.append(", ");
            sb.append(a.getAddress());
        }
        return sb.toString();
    }

    private static final long MSECS_PER_DAY = 24 * 60 * 60 * 1000;

    // Check if invite is relevant after the given time.  Invite is relevant if its DTSTART
    // or RECURRENCE-ID comes after the reference time.  For all-day appointment, look back
    // 24 hours to account for possible TZ difference.
    protected static boolean inviteIsAfterTime(Invite inv, long time) {
        long startUtc = 0;
        ParsedDateTime dtStart = inv.getStartTime();
        if (dtStart != null)
            startUtc = dtStart.getUtcTime();
        long ridUtc = 0;
        RecurId rid = inv.getRecurId();
        if (rid != null) {
            ParsedDateTime ridDt = rid.getDt();
            if (ridDt != null)
                ridUtc = ridDt.getUtcTime();
        }
        long invTime = Math.max(startUtc, ridUtc);
        if (inv.isAllDayEvent())
            time -= MSECS_PER_DAY;
        return invTime >= time;
    }

    // Notify attendees following an update to the series of a recurring appointment.  Only the
    // added attendees are notified if notifyAllAttendees is false.  If it is true all attendees
    // for each invite are notified.  (Some invites may have more attendees than others.)
    protected static void notifyCalendarItem(
            ZimbraSoapContext zsc, OperationContext octxt, Account acct, Mailbox mbox, CalendarItem calItem,
            boolean notifyAllAttendees, List<ZAttendee> addedAttendees, boolean ignorePastExceptions, MailSendQueue sendQueue)
    throws ServiceException {
        boolean onBehalfOf = isOnBehalfOfRequest(zsc);
        Account authAcct = getAuthenticatedAccount(zsc);
        boolean hidePrivate =
            !calItem.isPublic() && !calItem.allowPrivateAccess(authAcct, zsc.isUsingAdminPrivileges());
        Address from = AccountUtil.getFriendlyEmailAddress(acct);
        Address sender = null;
        if (onBehalfOf)
            sender = AccountUtil.getFriendlyEmailAddress(authAcct);
        List<Address> addedRcpts = CalendarMailSender.toListFromAttendees(addedAttendees);

        long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();

        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
            // Refresh the cal item so we see the latest blob, whose path may have been changed
            // earlier in the current request.
            calItem = mbox.getCalendarItemById(octxt, calItem.getId());

             List<Invite> invites = new ArrayList<Invite>(Arrays.asList(calItem.getInvites()));

            // Get exception instances.  These will be included in the series update email.
            List<Invite> exceptions = new ArrayList<Invite>();

            Iterator<Invite> iterator = invites.iterator();
            while (iterator.hasNext()) {
                Invite invite = (Invite) iterator.next();
                if (invite.hasRecurId()) {
                    exceptions.add(invite);
                    iterator.remove();
                }
            }

            // Send the update invites.
            boolean didExceptions = false;
            for (Invite inv : invites) {
                if (ignorePastExceptions && inv.hasRecurId() && !inviteIsAfterTime(inv, now)) {
                    continue;
                }

                // Make the new iCalendar part to send.
                ZVCalendar cal = inv.newToICalendar(!hidePrivate);
                // For series invite, append the exception instances.
                if (inv.isRecurrence() && !didExceptions) {
                    // Find the VEVENT/VTODO for the series.
                    ZComponent seriesComp = null;
                    for (Iterator<ZComponent> compIter = cal.getComponentIterator(); compIter.hasNext(); ) {
                        ZComponent comp = compIter.next();
                        ICalTok compName = comp.getTok();
                        if (ICalTok.VEVENT.equals(compName) || ICalTok.VTODO.equals(compName)) {
                            if (comp.getProperty(ICalTok.RRULE) != null) {
                                seriesComp = comp;
                                break;
                            }
                        }
                    }
                    for (Invite except : exceptions) {
                        if (except.isCancel() && seriesComp != null) {
                            // Cancels are added as EXDATEs in the series VEVENT/VTODO.
                            RecurId rid = except.getRecurId();
                            if (rid != null && rid.getDt() != null) {
                                ZProperty exdate = rid.getDt().toProperty(ICalTok.EXDATE, false);
                                seriesComp.addProperty(exdate);
                            }
                        } else {
                            // Exception instances are added as additional VEVENTs/VTODOs.
                            ZComponent exceptComp = except.newToVComponent(false, !hidePrivate);
                            cal.addComponent(exceptComp);
                        }
                    }
                    didExceptions = true;
                }

                // Compose email using the existing MimeMessage as template and send it.
                MimeMessage mmInv = calItem.getSubpartMessage(inv.getMailItemId());
                List<Address> rcpts;
                if (notifyAllAttendees) {
                    rcpts = CalendarMailSender.toListFromAttendees(inv.getAttendees());
                } else {
                    rcpts = addedRcpts;
                }
                if (rcpts != null && !rcpts.isEmpty()) {
                    MimeMessage mmModify = CalendarMailSender.createCalendarMessage(authAcct, from, sender, rcpts, mmInv, inv, cal, true);
                    CalSendData csd = new CalSendData();
                    csd.mMm = mmModify;
                    csd.mOrigId = new ItemId(mbox, inv.getMailItemId());
                    MailSendQueueEntry entry = new MailSendQueueEntry(octxt, mbox, csd, null);
                    sendQueue.add(entry);
                }
            }
        }
    }

    protected static void addRemoveAttendeesInExceptions(
            OperationContext octxt, Mailbox mbox, CalendarItem calItem,
            List<ZAttendee> toAdd, List<ZAttendee> toRemove,
            boolean ignorePastExceptions)
    throws ServiceException {
        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
            // Refresh the cal item so we see the latest blob, whose path may have been changed
            // earlier in the current request.
            calItem = mbox.getCalendarItemById(octxt, calItem.getId());

            long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
            boolean first = true;
            Invite invites[] = calItem.getInvites();
            for (Invite inv : invites) {
                // Ignore exceptions in the past.
                if (ignorePastExceptions && inv.hasRecurId() && !inviteIsAfterTime(inv, now)) {
                    continue;
                }

                // Make a copy of the invite and add/remove attendees.
                boolean modified = false;
                Invite modify = inv.newCopy();
                modify.setMailItemId(inv.getMailItemId());
                List<ZAttendee> existingAts = modify.getAttendees();
                List<ZAttendee> addList = new ArrayList<ZAttendee>(toAdd);  // Survivors are added.
                for (Iterator<ZAttendee> iter = existingAts.iterator(); iter.hasNext(); ) {
                    ZAttendee existingAt = iter.next();
                    String existingAtEmail = existingAt.getAddress();
                    if (existingAtEmail != null) {
                        // Check if the attendee being added is already in the invite.
                        for (Iterator<ZAttendee> iterAdd = addList.iterator(); iterAdd.hasNext(); ) {
                            ZAttendee at = iterAdd.next();
                            if (existingAtEmail.equalsIgnoreCase(at.getAddress())) {
                                iterAdd.remove();
                            }
                        }
                        // Check if existing attendee matches an attendee being removed.
                        for (ZAttendee at : toRemove) {
                            if (existingAtEmail.equalsIgnoreCase(at.getAddress())) {
                                iter.remove();
                                modified = true;
                                break;
                            }
                        }
                    }
                }
                // Duplicates have been eliminated from addList.  Add survivors to the invite.
                if (!addList.isEmpty()) {
                    for (ZAttendee at : addList) {
                        modify.addAttendee(at);
                    }
                    modified = true;
                }

                // Save the modified invite.
                if (modified) {  // No need to re-save the series that was already updated in this request.
                    // If invite had no attendee before, its method is set to PUBLISH.  It must be changed to
                    // REQUEST so that the recipient can handle it properly.
                    if (!modify.isCancel())
                        modify.setMethod(ICalTok.REQUEST.toString());
                    // DTSTAMP - Rev it.
                    modify.setDtStamp(now);
                    // Save the modified invite, using the existing MimeMessage for the exception.
                    MimeMessage mmInv = calItem.getSubpartMessage(modify.getMailItemId());
                    ParsedMessage pm = mmInv != null ? new ParsedMessage(mmInv, false) : null;
                    mbox.addInvite(octxt, modify, calItem.getFolderId(), pm, true, false, first);
                    first = false;

                    // Refresh calItem after update in mbox.addInvite.
                    calItem = mbox.getCalendarItemById(octxt, calItem.getId());
                }
            }
        }
    }

    protected static void notifyRemovedAttendees(ZimbraSoapContext zsc, OperationContext octxt, Account acct,
            Mailbox mbox, CalendarItem calItem, Invite invToCancel, List<ZAttendee> removedAttendees,
            MailSendQueue sendQueue)
    throws ServiceException {
        boolean onBehalfOf = isOnBehalfOfRequest(zsc);
        Account authAcct = getAuthenticatedAccount(zsc);
        Locale locale = !onBehalfOf ? acct.getLocale() : authAcct.getLocale();

        CalSendData dat = new CalSendData();
        dat.mOrigId = new ItemId(mbox, invToCancel.getMailItemId());
        dat.mReplyType = MailSender.MSGTYPE_REPLY;

        String text = L10nUtil.getMessage(MsgKey.calendarCancelRemovedFromAttendeeList, locale);

        if (ZimbraLog.calendar.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Sending cancellation message for \"");
            sb.append(invToCancel.getName()).append("\" to ");
            sb.append(getAttendeesAddressList(removedAttendees));
            ZimbraLog.calendar.debug(sb.toString());
        }

        List<Address> rcpts = CalendarMailSender.toListFromAttendees(removedAttendees);
        try {
            dat.mInvite = CalendarUtils.buildCancelInviteCalendar(
                    acct, authAcct, zsc.isUsingAdminPrivileges(), onBehalfOf, calItem, invToCancel, text, removedAttendees);
            ZVCalendar cal = dat.mInvite.newToICalendar(true);
            dat.mMm = CalendarMailSender.createCancelMessage(
                    acct, authAcct, zsc.isUsingAdminPrivileges(), onBehalfOf, rcpts, calItem, invToCancel, text, cal);

            // If we are sending this cancellation to other people, then we MUST be the organizer!
            if (!dat.mInvite.isOrganizer() && rcpts != null && !rcpts.isEmpty())
                throw MailServiceException.MUST_BE_ORGANIZER("updateRemovedInvitees");

            sendCalendarCancelMessage(zsc, octxt, calItem.getFolderId(), acct, mbox, dat, false, sendQueue);
        } catch (ServiceException ex) {
            String to = getAttendeesAddressList(removedAttendees);
            ZimbraLog.calendar.debug(
                    "Could not inform attendees (" + to + ") that they were removed from meeting " +
                    invToCancel.toString() + " b/c of exception: " + ex.toString());
        }
    }

    /**
     * Is this an on-behalf-of request?  It is if the requested and authenticated account are different.
     * Special case for ZDesktop: If auth account is the "local" account, consider the request to NOT be
     * on-behalf-of.
     * @param zsc
     * @return
     * @throws ServiceException
     */
    public static boolean isOnBehalfOfRequest(ZimbraSoapContext zsc) throws ServiceException {
        if (!zsc.isDelegatedRequest())
            return false;
        return !AccountUtil.isZDesktopLocalAccount(zsc.getAuthtokenAccountId());
    }

    protected static class MailSendQueueEntry {
        OperationContext octxt;
        Mailbox mbox;
        CalSendData csd;
        File file;

        MailSendQueueEntry(OperationContext octxt, Mailbox mbox, CalSendData csd, File file) {
            this.octxt = octxt;
            this.mbox = mbox;
            this.csd = csd;
            this.file = file;
        }

        public void send() throws ServiceException {
            try {
                // All calendar-related emails are sent in sendpartial mode.
                CalendarMailSender.sendPartial(octxt, mbox, csd.mMm, csd.uploads, csd.mOrigId,
                        csd.mReplyType, csd.mIdentityId, false);
            } finally {
                if (file != null) {
                    file.delete();
                }
            }
        }
    }

    protected static class MailSendQueue {
        Queue<MailSendQueueEntry> queue = new LinkedList<MailSendQueueEntry>();

        public void add(MailSendQueueEntry entry) {
            queue.add(entry);
        }

        public void send() {
            while (!queue.isEmpty()) {
                MailSendQueueEntry entry = queue.remove();
                if (entry != null) {
                    try {
                        entry.send();
                    } catch (ServiceException e) {
                        ZimbraLog.calendar.warn("ignoring error while sending calendar email", e);
                    }
                }
            }
        }
    }
}
