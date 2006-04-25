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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;


public class GetICal extends WriteOpDocumentHandler {

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbx = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        String iidStr = request.getAttribute(MailService.A_ID, null);
        long rangeStart = request.getAttributeLong(MailService.A_APPT_START_TIME, -1);
        long rangeEnd = request.getAttributeLong(MailService.A_APPT_END_TIME, -1);
        
//        int compNum = (int)request.getAttributeLong(MailService.A_APPT_COMPONENT_NUM);
        int compNum = 0;
        
        try {
            try {
                ZVCalendar cal = null;
                if (iidStr != null) {
                	ItemId iid = new ItemId(iidStr, lc);
                    Appointment appt = mbx.getAppointmentById(octxt, iid.getId());
                    if (appt == null) {
                        throw MailServiceException.NO_SUCH_APPT(iid.toString(), "Could not find appointment");
                    }
                    Invite inv = appt.getInvite(iid.getSubpartId(), compNum);
                    if (inv == null) {
                        throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
                    }
                    cal = inv.newToICalendar();
                } else {
                    cal = mbx.getZCalendarForRange(octxt, rangeStart, rangeEnd, Mailbox.ID_FOLDER_CALENDAR);
                }
                
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
//                CalendarOutputter calOut = new CalendarOutputter();
                
                try {
                    OutputStreamWriter wout = new OutputStreamWriter(buf);
                    cal.toICalendar(wout);
                    wout.flush();
                    
                    Element response = lc.createElement(MailService.GET_ICAL_RESPONSE);
                    
                    Element icalElt = response.addElement(MailService.E_APPT_ICAL);
                    
                    icalElt.addAttribute(MailService.A_ID, iidStr);
                    
                    icalElt.addText(buf.toString());
                    
                    return response;
                } catch (IOException e) {
                    throw ServiceException.FAILURE("IO Exception while outputing Calendar for Invite: "+ iidStr + "-" + compNum, e);
                }
            } catch(MailServiceException.NoSuchItemException e) {
                throw ServiceException.FAILURE("Error could get default invite for Invite: "+ iidStr + "-" + compNum, e);
            }
        } catch(MailServiceException.NoSuchItemException e) {
            throw ServiceException.FAILURE("No Such Invite Message: "+ iidStr, e);
        }
    }
}
