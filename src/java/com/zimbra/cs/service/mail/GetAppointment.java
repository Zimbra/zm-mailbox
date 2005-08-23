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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author tim
 * 
 * This class exists only for testing / debugging.  It is not a published API and may change
 * without notice.
 *
 */
public class GetAppointment extends DocumentHandler 
{
    private static Log sLog = LogFactory.getLog(GetAppointment.class);

    public Element handle(Element request, Map context)
            throws ServiceException {
        
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbx = getRequestedMailbox(lc);
        sLog.info("<GetAppointment> " + lc.toString());
        
        int apptId = (int)request.getAttributeLong("id");
        
        Appointment appointment = mbx.getAppointmentById(apptId);

        Element response = lc.createElement(MailService.GET_APPOINTMENT_RESPONSE);
        
        Element apptElt = response.addElement(MailService.E_APPOINTMENT);
        apptElt.addAttribute("uid", appointment.getUid());
        
        synchronized(mbx) {
        
            for (int i = 0; i < appointment.numInvites(); i++) {
                Invite inv = appointment.getInvite(i);
                
                Element ie = apptElt.addElement(MailService.E_INVITE);
                ie.addAttribute("id", inv.getMailItemId());
                ie.addAttribute("compNum", inv.getComponentNum());
                if (inv.hasRecurId()) {
                    ie.addAttribute("recur_id", inv.getRecurId().toString());
                }
                
                ToXML.encodeInvite(ie, inv, Change.ALL_FIELDS);
            }
        }
        
        Recurrence.IRecurrence recur = appointment.getRecurrence();
        if (recur != null) {
            Element recurElt = apptElt.addElement("recur");
            recur.toXml(recurElt);
        }
        
        
        return response;
    }

}
