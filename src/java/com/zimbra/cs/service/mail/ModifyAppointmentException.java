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
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.LiquidContext;


public class ModifyAppointmentException extends ModifyAppointment {
    private static Log sLog = LogFactory.getLog(ModifyAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("ModifyAppointmentException");
    
    public Element handle(Element request, Map context) throws ServiceException 
    {
        long startTime = sWatch.start();
        try {
            LiquidContext lc = getLiquidContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbx = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();
            
            ParsedItemID pid = ParsedItemID.Parse(request.getAttribute("id"));
            int compNum = (int)request.getAttributeLong("comp", 0);
            
            sLog.info("<ModifyAppointmentException id="+pid+" comp="+compNum+">");
            
            synchronized(mbx) {
                // have to cancel the appointment
                Appointment appt = mbx.getAppointmentById(pid.getItemIDInt()); 
                Invite inv = appt.getInvite(pid.getSubIdInt(), compNum);
                
                if (!inv.hasRecurId()) {
//                    throw ServiceException.INVALID_REQUEST("Called ModifyAppointmentException on invite "+inv.getMailItemId()+
//                            "with RECURRENCE_ID of 0", null);
                }
                
                // response
                Element response = lc.createElement(MailService.MODIFY_APPOINTMENT_EXCEPTION_RESPONSE);
                return modifyAppointment(octxt, request, acct, mbx, inv, response);
            } // synchronized on mailbox                
        } finally {
            sWatch.stop(startTime);
        }        
    }

}
