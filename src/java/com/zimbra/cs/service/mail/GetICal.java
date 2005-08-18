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
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
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
                    Message msg = mbx.getMessageById(msgId);
//                    Invite inv = msg.getInvite(0);
//                    cal = inv.getCalendar();
                } else {
                    cal = mbx.getCalendarForRange(octxt, rangeStart, rangeEnd);
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
