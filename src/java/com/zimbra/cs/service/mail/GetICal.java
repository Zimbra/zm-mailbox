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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;


public class GetICal extends WriteOpDocumentHandler {

    private static StopWatch sWatch = StopWatch.getInstance("GetICal");
    
    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbx = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        int msgId = (int)request.getAttributeLong(MailService.A_ID, -1);
        long rangeStart = request.getAttributeLong(MailService.A_APPT_START_TIME, -1);
        long rangeEnd = request.getAttributeLong(MailService.A_APPT_END_TIME, -1);
        
//        int compNum = (int)request.getAttributeLong(MailService.A_APPT_COMPONENT_NUM);
        int compNum = 0;
        
        try {
            try {
                Calendar cal = null;
                if (msgId > 0) {
                    Message msg = mbx.getMessageById(octxt, msgId);
//                    Invite inv = msg.getInvite(0);
//                    cal = inv.getCalendar();
                } else {
                    cal = mbx.getCalendarForRange(octxt, rangeStart, rangeEnd, Mailbox.ID_FOLDER_CALENDAR);
                }
                
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                CalendarOutputter calOut = new CalendarOutputter();
                
                try {
                    calOut.output(cal, buf);
                    
                    Element response = lc.createElement(MailService.GET_ICAL_RESPONSE);
                    
                    Element icalElt = response.addElement(MailService.E_APPT_ICAL);
                    
                    icalElt.addAttribute(MailService.A_ID, msgId);
                    
                    icalElt.addText(buf.toString());
                    
                    return response;
                } catch (IOException e) {
                    throw ServiceException.FAILURE("IO Exception while outputing Calendar for Invite: "+ msgId + "-" + compNum, e);
                } catch (ValidationException e) {
                    throw ServiceException.FAILURE("Validation Exception while outputing Calendar for Invite: "+ msgId + "-" + compNum, e);
                }
            } catch(MailServiceException.NoSuchItemException e) {
                throw ServiceException.FAILURE("Error could get default invite for Invite: "+ msgId + "-" + compNum, e);
            }
        } catch(MailServiceException.NoSuchItemException e) {
            throw ServiceException.FAILURE("No Such Invite Message: "+ msgId, e);
        } finally {
            sWatch.stop(startTime);
        }
    }
    

}
