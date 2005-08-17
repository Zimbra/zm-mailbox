package com.zimbra.cs.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.ParseMimeMessage;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.ZimbraContext;

public class CreateAppointmentException extends CreateAppointment 
{
    private static Log sLog = LogFactory.getLog(CreateAppointmentException.class);
    private static StopWatch sWatch = StopWatch.getInstance("CreateAppointmentException");

    
    protected static class CreateApptExceptionInviteParser implements ParseMimeMessage.InviteParser
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
    public Element handle(Element request, Map context) throws ServiceException 
    {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            OperationContext octxt = lc.getOperationContext();            
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            
            ParsedItemID pid = ParsedItemID.Parse(request.getAttribute("id"));
            int compNum = (int)request.getAttributeLong("comp");
            
            sLog.info("<CreateAppointmentException id="+pid+" comp="+compNum+">");

            synchronized(mbox) {
                Appointment appt = mbox.getAppointmentById(pid.getItemIDInt()); 
                Invite inv = appt.getInvite(pid.getSubIdInt(), compNum);
                
                if (inv.hasRecurId()) {
                    throw ServiceException.FAILURE("Invite id="+pid+" comp="+compNum+" is not the a default invite", null);
                }
                
                if (appt == null)
                    throw ServiceException.FAILURE("Could not find Appointment for id="+pid+" comp="+compNum+">", null);
                else if (!appt.isRecurring())
                    throw ServiceException.FAILURE("Appointment "+appt.getId()+" is not a recurring appointment", null);

                // <M>
                Element msgElem = request.getElement(MailService.E_MSG);
                
                CalSendData dat = handleMsgElement(octxt, msgElem, acct, mbox, new CreateApptExceptionInviteParser(appt.getUid(), inv.getTimeZoneMap()));
                
                Element response = lc.createElement(MailService.CREATE_APPOINTMENT_EXCEPTION_RESPONSE);            
                return sendCalendarMessage(octxt, acct, mbox, dat, response);
            }
            
        } finally {
            sWatch.stop(startTime);
        }
    }
}
