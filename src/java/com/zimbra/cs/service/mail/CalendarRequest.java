/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.Mime.FixedMimeMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class CalendarRequest extends MailDocumentHandler {

    private byte mItemType = MailItem.TYPE_UNKNOWN;
    protected byte getItemType() { return mItemType; }

    public CalendarRequest() {
        if (this instanceof AppointmentRequest)
            mItemType = MailItem.TYPE_APPOINTMENT;
        else if (this instanceof TaskRequest)
            mItemType = MailItem.TYPE_TASK;
    }

    protected static class CalSendData extends ParseMimeMessage.MimeMessageData {
        ItemId mOrigId; // orig id if this is a reply
        String mReplyType;
        String mIdentityId;
        MimeMessage mMm;
        Invite mInvite;
        boolean mDontNotifyAttendees;
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
        Element response)
    throws ServiceException {
        return sendCalendarMessage(zsc, octxt, apptFolderId, acct, mbox, csd, response, true);
    }

    protected static Element sendCalendarMessage(
            ZimbraSoapContext zsc,
            OperationContext octxt,
            int apptFolderId,
            Account acct,
            Mailbox mbox,
            CalSendData csd,
            Element response,
            boolean updateOwnAppointment)
        throws ServiceException {
            return sendCalendarMessageInternal(zsc, octxt, apptFolderId,
                                               acct, mbox, csd, response,
                                               updateOwnAppointment);
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
        boolean cancelOwnAppointment)
    throws ServiceException {
    	return sendCalendarMessageInternal(zsc, octxt, apptFolderId, acct, mbox, csd,
                                           null, cancelOwnAppointment);
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
        boolean updateOwnAppointment)
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
                for (int i = 0; i < aliases.length; i++)
                    addrs[i + 1] = aliases[i];
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
            csd.mInvite.setFragment(pm.getFragment());
        }

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
        File tempMmFile = null;
        try {
        	tempMmFile = File.createTempFile("zcal", "tmp");
        	tempMmFile.deleteOnExit();

            os = new FileOutputStream(tempMmFile);
            csd.mMm.writeTo(os);
            ByteUtil.closeStream(os);
            os = null;

            is = new FileInputStream(tempMmFile);
            csd.mMm = new FixedMimeMessage(JMSession.getSession(), is);
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

        int[] ids = null;
        ItemId msgId = null;
        boolean forceSendPartial = true;  // All calendar-related emails are sent in sendpartial mode.
        try {
            if (!csd.mInvite.isCancel()) {
                // For create/modify requests, we want to first update the local mailbox (organizer's)
                // and send invite emails only if local change succeeded.  This order is also necessary
                // because of the side-effect relating to attachments.  (see below comments)

                // First, update my own appointment.  It is important that this happens BEFORE the call to sendMimeMessage,
                // because sendMimMessage will delete uploaded attachments as a side-effect.
                if (updateOwnAppointment)
                    ids = mbox.addInvite(octxt, csd.mInvite, apptFolderId, pm);
                // Next, notify any attendees.
                if (!csd.mDontNotifyAttendees)
                    msgId = mbox.getMailSender().sendMimeMessage(
                            octxt, mbox, csd.mMm, csd.newContacts, csd.uploads,
                            csd.mOrigId, csd.mReplyType, csd.mIdentityId, forceSendPartial, false);
            } else {
                // But if we're sending a cancel request, send emails first THEN update the local mailbox.
                // This makes a difference if MTA is not running.  We'll avoid canceling organizer's copy
                // if we couldn't notify the attendees.
                //
                // This order has a problem when there's an attachment, but cancel requests should not
                // have an attachment, so we're okay.
                // Before sending email, make sure the requester has permission to cancel.
                CalendarItem calItem = mbox.getCalendarItemByUid(octxt, csd.mInvite.getUid());
                if (calItem != null)
                    calItem.checkCancelPermission(octxt.getAuthenticatedUser(), octxt.isUsingAdminPrivileges(), csd.mInvite);

                if (!csd.mDontNotifyAttendees)
                    msgId = mbox.getMailSender().sendMimeMessage(
                            octxt, mbox, csd.mMm, csd.newContacts, csd.uploads,
                            csd.mOrigId, csd.mReplyType, csd.mIdentityId, forceSendPartial, false);
                if (updateOwnAppointment)
                    ids = mbox.addInvite(octxt, csd.mInvite, apptFolderId, pm);
            }
        } finally {
            // Delete the temp file after we're done sending email.
            if (tempMmFile != null)
                tempMmFile.delete();
        }

        if (updateOwnAppointment && response != null && ids != null && ids.length >= 2) {
            ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
            String id = ifmt.formatItemId(ids[0]);
            response.addAttribute(MailConstants.A_CAL_ID, id);
            if (csd.mInvite.isEvent())
                response.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, id);  // for backward compat
            response.addAttribute(MailConstants.A_CAL_INV_ID, ifmt.formatItemId(ids[0], ids[1]));
            if (msgId != null)
                response.addUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, ifmt.formatItemId(msgId));
        }
        
        return response;
    }

    protected static Element sendOrganizerChangeMessage(
            final ZimbraSoapContext zsc, final OperationContext octxt,
            final CalendarItem calItem, final Account acct, final Mailbox mbox,
            Element response)
    throws ServiceException {
        Runnable r = new Runnable() {
            public void run() {
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
                                                        null, true);
                        }
                    }
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn("Ignoring error while sending organizer change message", e);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("OutOfMemoryError while sending organizer change message", e);
                }
            }
        };
        Thread senderThread = new Thread(r, "AnnounceOrganizerChangeSender");
        senderThread.setDaemon(true);
        senderThread.start();
        return response;
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

    protected static void updateAddedInvitees(ZimbraSoapContext zsc, OperationContext octxt, Account acct,
            Mailbox mbox, CalendarItem calItem, Invite seriesInv, List<ZAttendee> toAdd)
    throws ServiceException {
        if (!seriesInv.isOrganizer()) {
            // we ONLY should update the removed attendees if we are the organizer!
            return;
        }

        synchronized (mbox) {
            // Refresh the cal item so we see the latest blob, whose path may have been changed
            // earlier in the current request.
            calItem = mbox.getCalendarItemById(octxt, calItem.getId());
    
            boolean onBehalfOf = isOnBehalfOfRequest(zsc);
            Account authAcct = getAuthenticatedAccount(zsc);
            boolean hidePrivate =
                !calItem.isPublic() && !calItem.allowPrivateAccess(authAcct, zsc.isUsingAdminPrivileges());
            Address from = AccountUtil.getFriendlyEmailAddress(acct);
            Address sender = null;
            if (onBehalfOf)
                sender = AccountUtil.getFriendlyEmailAddress(authAcct);
            List<Address> rcpts = CalendarMailSender.toListFromAttendees(toAdd);
    
            long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
    
            Invite[] invites = calItem.getInvites();

            // Update invites to add the new attendees.
            boolean first = true;
            for (Invite inv : invites) {
                // Ignore exceptions in the past.
                if (inv.hasRecurId() && !inviteIsAfterTime(inv, now))
                    continue;
    
                // Make a copy of the invite and add the new attendees if necessary.
                boolean addedAttendees = false;
                Invite modify = inv.newCopy();
                modify.setMailItemId(inv.getMailItemId());
                for (ZAttendee at : toAdd) {
                    ZAttendee existing = modify.getMatchingAttendee(at.getAddress());
                    if (existing == null) {
                        modify.addAttendee(at);
                        addedAttendees = true;
                    }
                }
    
                // Save the modified invite.
                if (addedAttendees) {  // No need to re-save the series that was already updated in this request.
                    // If invite had no attendee before, its method is set to PUBLISH.  It must be changed to
                    // REQUEST so that the recipient can handle it properly.
                    if (!modify.isCancel())
                        modify.setMethod(ICalTok.REQUEST.toString());
                    // DTSTAMP - Rev it.
                    modify.setDtStamp(now);
                    // Save the modified invite, using the existing MimeMessage.
                    MimeMessage mmInv = calItem.getSubpartMessage(modify.getMailItemId());
                    ParsedMessage pm = mmInv != null ? new ParsedMessage(mmInv, false) : null;
                    mbox.addInvite(octxt, modify, calItem.getFolderId(), pm, true, false, first);
                    first = false;
                    // Refresh calItem to see the latest data saved.
                    calItem = mbox.getCalendarItemById(octxt, calItem.getId());
                }
            }

            // Refresh invite list to see the latest data saved in previous pass.
            invites = calItem.getInvites();

            // Get canceled instances in the future.  These will be included in the series update email.
            List<Invite> cancels = new ArrayList<Invite>();
            for (Invite inv : invites) {
                if (inv.isCancel() && inv.hasRecurId() && inviteIsAfterTime(inv, now))
                    cancels.add(inv);
            }

            boolean didCancels = false;
            for (Invite inv : invites) {
                // Ignore exceptions in the past.
                if (inv.hasRecurId() && !inviteIsAfterTime(inv, now))
                    continue;

                if (!inv.isCancel()) {
                    // Make the new iCalendar part to send.
                    ZVCalendar cal = inv.newToICalendar(!hidePrivate);
                    // For series invite, append the canceled instances.
                    if (inv.isRecurrence() && !didCancels) {
                        didCancels = true;
                        for (Invite cancel : cancels) {
                            ZComponent cancelComp = cancel.newToVComponent(true, !hidePrivate);
                            cal.addComponent(cancelComp);
                        }
                    }

                    // Compose email using the existing MimeMessage as template and send it.
                    MimeMessage mmInv = calItem.getSubpartMessage(inv.getMailItemId());
                    MimeMessage mmModify = CalendarMailSender.createCalendarMessage(from, sender, rcpts, mmInv, inv, cal, true);
                    mbox.getMailSender().sendMimeMessage(octxt, mbox, mmModify, null, null,
                            new ItemId(mbox, inv.getMailItemId()), null, null, true, false);
                }
            }
        }
    }

    protected static void updateRemovedInvitees(ZimbraSoapContext zsc, OperationContext octxt, Account acct,
            Mailbox mbox, CalendarItem calItem, Invite invToCancel, List<ZAttendee> toCancel)
    throws ServiceException {
        if (!invToCancel.isOrganizer()) {
            // we ONLY should update the removed attendees if we are the organizer!
            return;
        }

        synchronized (mbox) {
            // Refresh the cal item so we see the latest blob, whose path may have been changed
            // earlier in the current request.
            calItem = mbox.getCalendarItemById(octxt, calItem.getId());
    
            // If removing attendees from the series, also remove those attendees from all exceptions.
            if (invToCancel.isRecurrence()) {
                long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
                boolean first = true;
                Invite invites[] = calItem.getInvites();
                for (Invite inv : invites) {
                    // Ignore exceptions in the past.
                    if (inv.hasRecurId() && !inviteIsAfterTime(inv, now))
                        continue;

                    // Make a copy of the invite and remove canceled attendees.
                    boolean removedAttendees = false;
                    Invite modify = inv.newCopy();
                    modify.setMailItemId(inv.getMailItemId());
                    List<ZAttendee> existingAts = modify.getAttendees();
                    for (Iterator<ZAttendee> iter = existingAts.iterator(); iter.hasNext(); ) {
                        ZAttendee existingAt = iter.next();
                        String existingAtEmail = existingAt.getAddress();
                        if (existingAtEmail != null) {
                            boolean match = false;
                            for (ZAttendee at : toCancel) {
                                if (existingAtEmail.equalsIgnoreCase(at.getAddress())) {
                                    match = true;
                                    break;
                                }
                            }
                            if (match) {
                                iter.remove();
                                removedAttendees = true;
                            }
                        }
                    }
        
                    // Save the modified invite.
                    if (removedAttendees) {  // No need to re-save the series that was already updated in this request.
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
                sb.append(getAttendeesAddressList(toCancel));
                ZimbraLog.calendar.debug(sb.toString());
            }
    
            List<Address> rcpts = CalendarMailSender.toListFromAttendees(toCancel);
            try {
                dat.mInvite = CalendarUtils.buildCancelInviteCalendar(
                        acct, authAcct, zsc.isUsingAdminPrivileges(), onBehalfOf, calItem, invToCancel, text, toCancel);
                ZVCalendar cal = dat.mInvite.newToICalendar(true);
                dat.mMm = CalendarMailSender.createCancelMessage(
                        acct, authAcct, zsc.isUsingAdminPrivileges(), onBehalfOf, rcpts, calItem, invToCancel, text, cal);
    
                // If we are sending this cancellation to other people, then we MUST be the organizer!
                if (!dat.mInvite.isOrganizer() && rcpts != null && !rcpts.isEmpty())
                    throw MailServiceException.MUST_BE_ORGANIZER("updateRemovedInvitees");
    
                sendCalendarCancelMessage(zsc, octxt, calItem.getFolderId(), acct, mbox, dat, false);
            } catch (ServiceException ex) {
                String to = getAttendeesAddressList(toCancel);
                ZimbraLog.calendar.debug(
                        "Could not inform attendees (" + to + ") that they were removed from meeting " +
                        invToCancel.toString() + " b/c of exception: " + ex.toString());
            }
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
        String zdLocalAcctId = LC.zdesktop_local_account_id.value();
        if (zdLocalAcctId != null && zdLocalAcctId.equalsIgnoreCase(zsc.getAuthtokenAccountId()))
            return false;
        return true;
    }
}
