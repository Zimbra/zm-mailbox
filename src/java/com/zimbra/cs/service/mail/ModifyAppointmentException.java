package com.liquidsys.coco.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.mailbox.Appointment;
import com.liquidsys.coco.mailbox.Invite;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.util.ParsedItemID;
import com.liquidsys.coco.stats.StopWatch;
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
