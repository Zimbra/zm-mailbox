/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;

import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;


public class ModifyAppointment extends CalendarRequest {

    private static Log sLog = LogFactory.getLog(ModifyAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("ModifyAppointment");

    private static final String[] TARGET_APPT_PATH = new String[] { MailService.A_ID };
    protected String[] getProxiedIdPath()     { return TARGET_APPT_PATH; }
    protected boolean checkMountpointProxy()  { return false; }

    // very simple: generate a new UID and send a REQUEST
    protected static class ModifyAppointmentParser extends ParseMimeMessage.InviteParser {
        protected Mailbox mmbox;
        protected Invite mInv;
        
        ModifyAppointmentParser(Mailbox mbox, Invite inv) {
            mmbox = mbox;
            mInv = inv;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraContext lc, Account account, Element inviteElem) throws ServiceException {
            List atsToCancel = new ArrayList();

            ParseMimeMessage.InviteParserResult toRet = CalendarUtils.parseInviteForModify(account, inviteElem, mInv, atsToCancel);

            // send cancellations to any invitees who have been removed...
            updateRemovedInvitees(lc, account, mmbox, mInv.getAppointment(), mInv, atsToCancel);
            
            return toRet;
        }
    };

    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            ItemId iid = new ItemId(request.getAttribute(MailService.A_ID), lc);
            int compNum = (int) request.getAttributeLong(MailService.E_INVITE_COMPONENT, 0);
            sLog.info("<ModifyAppointment id=" + iid + " comp=" + compNum + ">");
            
            synchronized(mbox) {
                Appointment appt = mbox.getAppointmentById(octxt, iid.getId());
                if (appt == null) {
                    throw MailServiceException.NO_SUCH_APPOINTMENT(iid.toString(), "Could not find appointment");
                }
                Invite inv = appt.getInvite(iid.getSubpartId(), compNum);
                if (inv == null) {
                    throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
                }
                
                // response
                Element response = lc.createElement(MailService.MODIFY_APPOINTMENT_RESPONSE);
                
                return modifyAppointment(lc, request, acct, mbox, appt, inv, response);
            } // synchronized on mailbox                
        } finally {
            sWatch.stop(startTime);
        }        
    }
    
    protected static Element modifyAppointment(ZimbraContext lc, Element request, Account acct, Mailbox mbox,
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

        sendCalendarMessage(lc, appt.getFolderId(), acct, mbox, dat, response);

        return response;        
    }
    
    protected static void updateRemovedInvitees(ZimbraContext lc, Account acct, Mailbox mbox, Appointment appt, Invite inv, List toCancel)
    throws ServiceException {
        if (!inv.thisAcctIsOrganizer(acct)) {
            // we ONLY should update the removed attendees if we are the organizer!
            return;
        }
        
        CalSendData dat = new CalSendData();
        dat.mSaveToSent = shouldSaveToSent(acct);
        dat.mOrigId = inv.getMailItemId();
        dat.mReplyType = TYPE_REPLY;

        String text = "You have been removed from the Attendee list by the organizer";
        String subject = "CANCELLED: " + inv.getName();
        
        for (Iterator cancelIter = toCancel.iterator(); cancelIter.hasNext(); ) {
            ZAttendee cancelAt = (ZAttendee)cancelIter.next();
            
            if (sLog.isDebugEnabled()) {
                sLog.debug("Sending cancellation message \"" + subject + "\" to " +
                           cancelAt.getAddress().toString());
            }
            
            dat.mInvite = CalendarUtils.buildCancelInviteCalendar(acct, inv, text, cancelAt);
            Calendar cal = dat.mInvite.toICalendar();
            
            dat.mMm = CalendarUtils.createDefaultCalendarMessage(acct, 
                    cancelAt.getAddress(), subject, text, inv.getUid(), cal);
            
            sendCalendarMessage(lc, appt.getFolderId(), acct, mbox, dat, null); 
        }
    }
}
