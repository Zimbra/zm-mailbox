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

public class CreateAppointmentException extends CreateAppointment {
    
    private static Log sLog = LogFactory.getLog(CreateAppointmentException.class);
    private static StopWatch sWatch = StopWatch.getInstance("CreateAppointmentException");

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
            
            String idStr = request.getAttribute("id");
            ParsedItemID pid = ParsedItemID.parse(idStr); 
            int compNum = (int)request.getAttributeLong("comp");
            
            Element response = lc.createElement(MailService.CREATE_APPOINTMENT_EXCEPTION_RESPONSE);
            
            // <M>
            Element msgElem = request.getElement(MailService.E_MSG);
            
            sLog.info("<CreateAppointmentException pid=" +pid.toString() +" comp="+ compNum + "> " + lc.toString());
            
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
        } finally {
            sWatch.stop(startTime);
        }
    }

}
