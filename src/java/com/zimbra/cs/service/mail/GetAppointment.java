package com.liquidsys.coco.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.mailbox.Appointment;
import com.liquidsys.coco.mailbox.Invite;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.calendar.Recurrence;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.session.PendingModifications.Change;
import com.liquidsys.soap.DocumentHandler;
import com.liquidsys.soap.LiquidContext;

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
        
        LiquidContext lc = getLiquidContext(context);
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
