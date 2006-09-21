/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.util.L10nUtil.MsgKey;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;


public class ModifyAppointment extends CalendarRequest {

    private static Log sLog = LogFactory.getLog(ModifyAppointment.class);

    private static final String[] TARGET_APPT_PATH = new String[] { MailService.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_APPT_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }

    // very simple: generate a new UID and send a REQUEST
    protected static class ModifyAppointmentParser extends ParseMimeMessage.InviteParser {
        protected Mailbox mmbox;
        protected Invite mInv;
        
        ModifyAppointmentParser(Mailbox mbox, Invite inv) {
            mmbox = mbox;
            mInv = inv;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, Account account, Element inviteElem) throws ServiceException {
            List<ZAttendee> atsToCancel = new ArrayList<ZAttendee>();

            ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteForModify(account, inviteElem, mInv, atsToCancel, !mInv.hasRecurId());

            // send cancellations to any invitees who have been removed...
            updateRemovedInvitees(lc, account, mmbox, mInv.getAppointment(), mInv, atsToCancel);
            
            return toRet;
        }
    };

    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        ItemId iid = new ItemId(request.getAttribute(MailService.A_ID), lc);
        int compNum = (int) request.getAttributeLong(MailService.E_INVITE_COMPONENT, 0);
        sLog.info("<ModifyAppointment id=" + iid + " comp=" + compNum + ">");
        
        synchronized(mbox) {
            Appointment appt = mbox.getAppointmentById(octxt, iid.getId());
            if (appt == null) {
                throw MailServiceException.NO_SUCH_APPT(iid.toString(), "Could not find appointment");
            }
            Invite inv = appt.getInvite(iid.getSubpartId(), compNum);
            if (inv == null) {
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
            }
            
            // response
            Element response = lc.createElement(MailService.MODIFY_APPOINTMENT_RESPONSE);
            
            return modifyAppointment(lc, request, acct, mbox, appt, inv, response);
        } // synchronized on mailbox                
    }
    
    protected static Element modifyAppointment(ZimbraSoapContext lc, Element request, Account acct, Mailbox mbox,
            Appointment appt, Invite inv, Element response) throws ServiceException
    {
        // <M>
        Element msgElem = request.getElement(MailService.E_MSG);
        
        ModifyAppointmentParser parser = new ModifyAppointmentParser(mbox, inv);
        
        CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);
        
        // If we are sending this update to other people, then we MUST be the organizer!
        if (!inv.thisAcctIsOrganizer(acct)) {
            try {
                Address[] rcpts = dat.mMm.getAllRecipients();
                if (rcpts != null && rcpts.length > 0) {
                    throw MailServiceException.MUST_BE_ORGANIZER("ModifyAppointment");
                }
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
            }
        }

        sendCalendarMessage(lc, appt.getFolderId(), acct, mbox, dat, response, false);

        return response;        
    }
    
    protected static void updateRemovedInvitees(
            ZimbraSoapContext lc, Account acct, Mailbox mbox,
            Appointment appt, Invite inv,
            List<ZAttendee> toCancel)
    throws ServiceException {
        if (!inv.thisAcctIsOrganizer(acct)) {
            // we ONLY should update the removed attendees if we are the organizer!
            return;
        }

        boolean onBehalfOf = lc.isDelegatedRequest();
        Account authAcct = lc.getAuthtokenAccount();
        Locale locale = !onBehalfOf ? acct.getLocale() : authAcct.getLocale();

        CalSendData dat = new CalSendData(acct, authAcct, onBehalfOf);
        dat.mOrigId = inv.getMailItemId();
        dat.mReplyType = MailSender.MSGTYPE_REPLY;

        String text = L10nUtil.getMessage(MsgKey.calendarCancelRemovedFromAttendeeList, locale);

        if (sLog.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Sending cancellation message for \"");
            sb.append(inv.getName()).append("\" to ");
            sb.append(getAttendeesAddressList(toCancel));
            sLog.debug(sb.toString());
        }

        List<Address> rcpts = CalendarMailSender.toListFromAttendees(toCancel);
        try {
            dat.mInvite = CalendarUtils.buildCancelInviteCalendar(acct, authAcct.getName(), onBehalfOf, inv, text, toCancel);
            ZVCalendar cal = dat.mInvite.newToICalendar();
            dat.mMm = CalendarMailSender.createCancelMessage(acct, rcpts, onBehalfOf, authAcct, inv, null, text, cal);
            sendCalendarCancelMessage(lc, appt.getFolderId(),
                                      acct, mbox, dat, false);
        } catch (ServiceException ex) {
            String to = getAttendeesAddressList(toCancel);
            ZimbraLog.calendar.debug("Could not inform attendees ("+to+") that they were removed from meeting "+inv.toString()+" b/c of exception: "+ex.toString());
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
}
