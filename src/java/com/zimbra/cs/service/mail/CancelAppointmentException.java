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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
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
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.ZimbraContext;

public class CancelAppointmentException extends CancelAppointment {
    private static Log sLog = LogFactory.getLog(CancelAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("CancelAppointmentException");
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            ParsedItemID pid = ParsedItemID.Parse(request.getAttribute("id"));
            int compNum = (int)request.getAttributeLong("comp", 0);
            
            sLog.info("<CancelAppointmentException id="+pid+" comp="+compNum+">");
            
            synchronized(mbox) {
                Appointment appt = mbox.getAppointmentById(pid.getItemIDInt()); 
                Invite inv = appt.getInvite(pid.getSubIdInt(), compNum);
                
                Element recurElt = request.getOptionalElement("inst");
                if (recurElt != null) {
                    if (inv.hasRecurId()) {
                        throw MailServiceException.CANNOT_CANCEL_INSTANCE_OF_EXCEPTION(" for CancelAppointmentRequest("+pid+","+compNum+")");
                    }
                    cancelInstance(octxt, request, acct, mbox, inv, recurElt);
                } else {
                    cancelInvite(octxt, request, acct, mbox, inv);
                }
            } // synchronized on mailbox                
        
            Element response = lc.createElement(MailService.CANCEL_APPOINTMENT_EXCEPTION_RESPONSE);
            return response;
        } finally {
            sWatch.stop(startTime);
        }        
    }        
}
