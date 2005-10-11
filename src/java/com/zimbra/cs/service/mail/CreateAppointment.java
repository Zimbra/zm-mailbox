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

/*
 * Created on Feb 22, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.cs.service.util.ParsedItemID;

/**
 * @author tim
 */
public class CreateAppointment extends CalendarRequest {
    
    private static Log sLog = LogFactory.getLog(CreateAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("CreateAppointment");

    // very simple: generate a new UID and send a REQUEST
    protected static class CreateAppointmentInviteParser extends ParseMimeMessage.InviteParser { 
        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, null, null, false);
        }
    };
    
    protected static class CreateApptExceptionInviteParser extends ParseMimeMessage.InviteParser
    {
        private String mUid;
        private TimeZoneMap mTzMap;
        
        CreateApptExceptionInviteParser(String uid, TimeZoneMap tzMap)
        {
            mUid = uid;
            mTzMap = tzMap;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, mTzMap, mUid, true);
        }
    };
    
    
    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();
            
            String idStr = request.getAttribute("id", null);
            ParsedItemID pid = null;
            int compNum = 0;
            
            if (idStr != null) {
                pid = ParsedItemID.parse(idStr);
                compNum = (int)request.getAttributeLong("comp");
            }

            Element response = lc.createElement(MailService.CREATE_APPOINTMENT_RESPONSE);
            
            // <M>
            Element msgElem = request.getElement(MailService.E_MSG);
            

            if (pid == null) { 
                // no existing Appt referenced -- this is a new create!
                sLog.info("<CreateAppointment> " + lc.toString());
                
                int folder = (int) msgElem.getAttributeLong(MailService.A_FOLDER, Mailbox.ID_FOLDER_CALENDAR);
                
                CreateAppointmentInviteParser parser = new CreateAppointmentInviteParser();
                CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);

                return sendCalendarMessage(octxt, folder, acct, mbox, dat, response);
            } else {
                sLog.info("<CreateAppointment pid=" +pid.toString() +" comp="+ compNum + "> " + lc.toString());
                
                if (msgElem.getAttributeLong(MailService.A_FOLDER, -1) != -1) {
                    throw ServiceException.FAILURE("You may not specify a target Folder when creating an Exception for an existing appointment", null);
                }
                
                synchronized(mbox) {
                    Appointment appt = mbox.getAppointmentById(octxt, pid.getItemIDInt()); 
                    Invite inv = appt.getInvite(pid.getSubIdInt(), compNum);
                    
                    if (inv.hasRecurId()) {
                        throw ServiceException.FAILURE("Invite id="+pid+" comp="+compNum+" is not the a default invite", null);
                    }
                    
                    if (appt == null)
                        throw ServiceException.FAILURE("Could not find Appointment for id="+pid+" comp="+compNum+">", null);
                    else if (!appt.isRecurring())
                        throw ServiceException.FAILURE("Appointment "+appt.getId()+" is not a recurring appointment", null);

                    CreateApptExceptionInviteParser parser = new CreateApptExceptionInviteParser(appt.getUid(), inv.getTimeZoneMap());                
                    CalSendData dat = handleMsgElement(lc, msgElem, acct, mbox, parser);
                    
                    return sendCalendarMessage(octxt, appt.getFolderId(), acct, mbox, dat, response);
                }
            }
        } finally {
            sWatch.stop(startTime);
        }
    }
}
