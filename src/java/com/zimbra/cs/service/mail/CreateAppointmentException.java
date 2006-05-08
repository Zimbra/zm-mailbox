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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;


public class CreateAppointmentException extends CreateAppointment {
    
    private static Log sLog = LogFactory.getLog(CreateAppointmentException.class);

    private static final String[] TARGET_APPT_PATH = new String[] { MailService.A_ID };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_APPT_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }

    protected static class CreateApptExceptionInviteParser extends ParseMimeMessage.InviteParser
    {
        private String mUid;
        private TimeZoneMap mTzMap;
        
        CreateApptExceptionInviteParser(String uid, TimeZoneMap tzMap) {
            mUid = uid;
            mTzMap = tzMap;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(ZimbraSoapContext lc, Account account, Element inviteElem)
        throws ServiceException {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, mTzMap, mUid, true, CalendarUtils.RECUR_NOT_ALLOWED);
        }
    };
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        ItemId iid = new ItemId(request.getAttribute(MailService.A_ID), lc);
        int compNum = (int) request.getAttributeLong(MailService.E_INVITE_COMPONENT);
        sLog.info("<CreateAppointmentException id=" + lc.formatItemId(iid) + " comp=" + compNum + "> " + lc.toString());
        
        // <M>
        Element msgElem = request.getElement(MailService.E_MSG);
        
        if (msgElem.getAttribute(MailService.A_FOLDER, null) != null) {
            throw ServiceException.FAILURE("You may not specify a target Folder when creating an Exception for an existing appointment", null);
        }
        
        Element response = lc.createElement(MailService.CREATE_APPOINTMENT_EXCEPTION_RESPONSE);
        synchronized(mbox) {
            Appointment appt = mbox.getAppointmentById(octxt, iid.getId()); 
            Invite inv = appt.getInvite(iid.getSubpartId(), compNum);
            
            if (inv.hasRecurId()) {
                throw MailServiceException.INVITE_OUT_OF_DATE("Invite id=" + lc.formatItemId(iid) + " comp=" + compNum + " is not the a default invite");
            }
            
            if (appt == null)
                throw MailServiceException.NO_SUCH_APPT(inv.getUid(), " for CreateAppointmentExceptionRequest(" + iid + "," + compNum + ")");
            else if (!appt.isRecurring())
                throw ServiceException.INVALID_REQUEST("Appointment " + appt.getId() + " is not a recurring appointment", null);
            
            CreateApptExceptionInviteParser parser = new CreateApptExceptionInviteParser(appt.getUid(), inv.getTimeZoneMap());                
            CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);
            
            return sendCalendarMessage(lc, appt.getFolderId(), acct, mbox, dat, response, false);
        }
    }

}
