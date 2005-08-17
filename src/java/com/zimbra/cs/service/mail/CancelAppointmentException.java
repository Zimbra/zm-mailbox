package com.liquidsys.coco.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.mailbox.Appointment;
import com.liquidsys.coco.mailbox.Invite;
import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.util.ParsedItemID;
import com.liquidsys.coco.stats.StopWatch;
import com.liquidsys.soap.LiquidContext;

public class CancelAppointmentException extends CancelAppointment {
    private static Log sLog = LogFactory.getLog(CancelAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("CancelAppointmentException");
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            LiquidContext lc = getLiquidContext(context);
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
