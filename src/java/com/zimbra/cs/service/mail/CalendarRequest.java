/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
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
     * @param octxt TODO
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
            List /*<MPartInfo>*/ parts = Mime.getParts(mm);
            for (Iterator it = parts.iterator(); it.hasNext(); ) {
                MPartInfo mpi = (MPartInfo) it.next();
                
                if (mpi.getContentType().equals(Mime.CT_TEXT_HTML))
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
            
            List /*<MPartInfo>*/ parts = Mime.getParts(mm);
            for (Iterator it = parts.iterator(); it.hasNext(); ) {
                MPartInfo mpi = (MPartInfo) it.next();
                
                if (mpi.getContentType().equals(Mime.CT_TEXT_HTML)) {
                    String str = htmlStr;
                    
                    str = str.replaceFirst("href=\"@@ACCEPT@@\"", accept);
                    str = str.replaceFirst("href=\"@@DECLINE@@\"", decline);
                    str = str.replaceFirst("href=\"@@TENTATIVE@@\"", tentative);
                    
                    System.out.println(str);
                    mpi.getMimePart().setContent(str, Mime.CT_TEXT_HTML);
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
        boolean ignoreFailedAddresses)
    throws ServiceException {
        return sendCalendarMessageInternal(zsc, octxt, apptFolderId,
                                           acct, mbox, csd, response,
                                           ignoreFailedAddresses, true, true);
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
     *                             sending out cancelleation message to
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
                                           null, true, cancelOwnAppointment, true);
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
     * @param ignoreFailedAddresses
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
        boolean ignoreFailedAddresses,
        boolean updateOwnAppointment,
        boolean mustBeOrganizer)
    throws ServiceException {
        synchronized (mbox) {
            if (csd.mInvite.hasOrganizer()) {
                boolean isOrganizer = csd.mInvite.isOrganizer();
                ICalTok method = ICalTok.lookup(csd.mInvite.getMethod());
                switch (method) {
                case REQUEST:
                case PUBLISH:
                case ADD:
                case DECLINECOUNTER:
                    // Check if organizer is set to someone other than the mailbox
                    // owner and reject any such request.  If we didn't, attendees would
                    // receive an invitation from user A claiming the meeting was
                    // organized by user B.  (Saw this behavior with Consilient.)
                    if (!isOrganizer && mustBeOrganizer) {
                        String orgAddress = csd.mInvite.getOrganizer().getAddress();
                        throw ServiceException.INVALID_REQUEST(
                                "Cannot create/modify an appointment/task with organizer set to " +
                                orgAddress + " when using account " + acct.getName(),
                                null);
                    }
                    break;
                case CANCEL:
                    if (!isOrganizer && mustBeOrganizer) {
                        boolean hasRcpts = false;
                        try {
                            hasRcpts = csd.mMm.getAllRecipients() != null;
                        } catch (MessagingException e) {
                            throw ServiceException.FAILURE("Error examining recipients in a calendar message", e);
                        }
                        if (hasRcpts)
                            throw ServiceException.INVALID_REQUEST(
                                    "Must be organizer to send cancellation message to attendees", null);
                    }
                    break;
                case REPLY:
                case COUNTER:
                    // nothing to check
                    break;
                }
            }

            boolean onBehalfOf = zsc.isDelegatedRequest();
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
                String[] aliases = acct.getAliases();
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

            ItemId msgId = null;

            int[] ids = null;
            // First, update my own appointment.  It is important that this happens BEFORE the call to sendMimeMessage,
            // because sendMimMessage will delete uploaded attachments as a side-effect.
            if (updateOwnAppointment)
                ids = mbox.addInvite(octxt, csd.mInvite, apptFolderId, pm);

            // Next, notify any attendees.
            if (!csd.mDontNotifyAttendees)
                msgId = mbox.getMailSender().sendMimeMessage(
                        octxt, mbox, csd.mMm, csd.newContacts, csd.uploads,
                        csd.mOrigId, csd.mReplyType, csd.mIdentityId, ignoreFailedAddresses, true);

            if (updateOwnAppointment && response != null && ids != null) {
                ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
                String id = ifmt.formatItemId(ids[0]);
                response.addAttribute(MailConstants.A_CAL_ID, id);
                if (csd.mInvite.isEvent())
                    response.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, id);  // for backward compat
                response.addAttribute(MailConstants.A_CAL_INV_ID, ifmt.formatItemId(ids[0], ids[1]));
                if (msgId != null)
                    response.addUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, ifmt.formatItemId(msgId));
            }
        }
        
        return response;
    }

    protected static Element sendOrganizerChangeMessage(
            ZimbraSoapContext zsc, OperationContext octxt,
            CalendarItem calItem, Account acct, Mailbox mbox,
            Element response)
    throws ServiceException {
        Account authAccount = getAuthenticatedAccount(zsc);
        Invite[] invites = calItem.getInvites();
        for (Invite inv : invites) {
            List<Address> rcpts = CalendarMailSender.toListFromAttendees(inv.getAttendees());
            if (rcpts.size() > 0) {
                CalSendData csd = new CalSendData();
                csd.mInvite = inv;
                csd.mOrigId = new ItemId(mbox, inv.getMailItemId());
                csd.mMm = CalendarMailSender.createOrganizerChangeMessage(acct, authAccount, calItem, csd.mInvite, rcpts);
                sendCalendarMessageInternal(zsc, octxt, calItem.getFolderId(), acct, mbox, csd,
                                            response, true, true, false);
            }
        }
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

    protected static void updateRemovedInvitees(ZimbraSoapContext zsc, OperationContext octxt, Account acct,
            Mailbox mbox, CalendarItem calItem, Invite inv, List<ZAttendee> toCancel)
    throws ServiceException {
        if (!inv.isOrganizer()) {
            // we ONLY should update the removed attendees if we are the organizer!
            return;
        }

        boolean onBehalfOf = zsc.isDelegatedRequest();
        Account authAcct = getAuthenticatedAccount(zsc);
        Locale locale = !onBehalfOf ? acct.getLocale() : authAcct.getLocale();

        CalSendData dat = new CalSendData();
        dat.mOrigId = new ItemId(mbox, inv.getMailItemId());
        dat.mReplyType = MailSender.MSGTYPE_REPLY;

        String text = L10nUtil.getMessage(MsgKey.calendarCancelRemovedFromAttendeeList, locale);

        if (ZimbraLog.calendar.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Sending cancellation message for \"");
            sb.append(inv.getName()).append("\" to ");
            sb.append(getAttendeesAddressList(toCancel));
            ZimbraLog.calendar.debug(sb.toString());
        }

        List<Address> rcpts = CalendarMailSender.toListFromAttendees(toCancel);
        try {
            dat.mInvite = CalendarUtils.buildCancelInviteCalendar(acct, authAcct, onBehalfOf, inv, text, toCancel);
            ZVCalendar cal = dat.mInvite.newToICalendar(true);
            dat.mMm = CalendarMailSender.createCancelMessage(acct, rcpts, onBehalfOf, authAcct, calItem, inv, text, cal);
            sendCalendarCancelMessage(zsc, octxt, calItem.getFolderId(), acct, mbox, dat, false);
        } catch (ServiceException ex) {
            String to = getAttendeesAddressList(toCancel);
            ZimbraLog.calendar.debug("Could not inform attendees ("+to+") that they were removed from meeting "+inv.toString()+" b/c of exception: "+ex.toString());
        }
    }
}
