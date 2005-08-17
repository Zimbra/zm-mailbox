package com.zimbra.cs.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Message;
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
