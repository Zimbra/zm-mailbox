/*
 * Created on Feb 17, 2005
 */
package com.liquidsys.coco.service.mail;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.mailbox.Appointment;
import com.liquidsys.coco.mailbox.Invite;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.util.ParsedItemID;
import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.calendar.InviteInfo;
import com.liquidsys.coco.mailbox.calendar.ParsedDuration;
import com.liquidsys.coco.stats.StopWatch;
import com.liquidsys.soap.LiquidContext;
import com.liquidsys.soap.WriteOpDocumentHandler;


/**
 * @author tim
 */
public class GetApptSummaries extends WriteOpDocumentHandler {

    private static Log mLog = LogFactory.getLog(GetApptSummaries.class);
    private static StopWatch sWatch = StopWatch.getInstance("GetApptSummaries");
    
    /* (non-Javadoc)
     * @see com.liquidsys.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context)
            throws ServiceException {

        long startTime = sWatch.start();
        try {
            LiquidContext lc = getLiquidContext(context);
            Mailbox mbx = getRequestedMailbox(lc);

            long rangeStart = request.getAttributeLong(MailService.A_APPT_START_TIME);
            long rangeEnd = request.getAttributeLong(MailService.A_APPT_END_TIME);

            Collection appointments = mbx.getAppointmentsForRange(rangeStart, rangeEnd);

            Element response = lc.createElement(MailService.GET_APPT_SUMMARIES_RESPONSE);
            for (Iterator aptIter = appointments.iterator(); aptIter.hasNext();) {
                
                Appointment appointment = (Appointment) aptIter.next();
                try {
                    
                    Element apptElt = response.getFactory().createElement(MailService.E_APPOINTMENT);
                    
                    apptElt.addAttribute("x_uid", appointment.getUid());
                    
//                    Element apptElt = response.addElement(MailService.E_APPOINTMENT);
                    Invite defaultInvite = appointment.getDefaultInvite();
                    
                    if (defaultInvite == null) {
                        mLog.info("Could not load defaultinfo for appointment with id="+appointment.getId()+" SKIPPING");
                        continue; // 
                    }
                    
                    ParsedDuration defDuration = defaultInvite.getEffectiveDuration();
                    if (defDuration == null) {
                        mLog.info("Could not load effective default duration for appointment id="+appointment.getId()+" SKIPPING");
                        continue;
                    }
                    
                    long defDurationMsecs = defDuration.getDurationAsMsecs(defaultInvite.getStartTime().getDate());
                    
                    // add all the instances:
                    boolean someInRange = false;
                    Collection instances = appointment.expandInstances(rangeStart, rangeEnd); 
                    for (Iterator instIter = instances.iterator(); instIter.hasNext();) {
                        Appointment.Instance inst = (Appointment.Instance)(instIter.next());
                        try {
                            InviteInfo invId = inst.getInviteInfo();
                            Invite inv = appointment.getInvite(invId.getMsgId(), invId.getComponentId());
                            
                            // figure out which fields are different from the default and put their data here...
                            ParsedDuration invDuration = inv.getEffectiveDuration();
                            long instStart = inst.getStart();
                            
                            if (instStart < rangeEnd && (invDuration.addToTime(instStart))>rangeStart) {
                                someInRange = true;
                            } else {
                                continue;
                            }
                            
                            
                            Element instElt = apptElt.addElement(MailService.E_INSTANCE);
                            
                            instElt.addAttribute(MailService.A_APPT_START_TIME, instStart);
                            
                            if (inst.isException()) {
                                instElt.addAttribute(MailService.A_APPT_IS_EXCEPTION, true);
                                
                                // testing temp removeme TODO
                                instElt.addAttribute("x_recurid", inv.getRecurId().toString());
                                
                                if ((defaultInvite.getMailItemId() != invId.getMsgId()) ||
                                        (defaultInvite.getComponentNum() != invId.getComponentId())) 
                                {
                                    ParsedItemID pid = ParsedItemID.Create(appointment.getId(), inst.getMailItemId());
                                    instElt.addAttribute(MailService.A_APPT_INV_ID, pid.toString());
                                    
                                    instElt.addAttribute(MailService.A_APPT_COMPONENT_NUM, inst.getComponentNum());
                                    
                                    // fragment has already been sanitized...
                                    Message msg = mbx.getMessageById(inst.getMailItemId()); 
                                    String frag = msg.getFragment();
                                    if (!frag.equals(""))
                                        instElt.addAttribute(MailService.E_FRAG, frag, Element.DISP_CONTENT);
                                }
                                
                                
                                if (defDurationMsecs != inst.getEnd()-inst.getStart())
                                    instElt.addAttribute(MailService.A_APPT_DURATION, inst.getEnd()-inst.getStart());
                                
                                if (!defaultInvite.getStatus().equals(inv.getStatus()))
                                    instElt.addAttribute(MailService.A_APPT_STATUS, inv.getStatus());

                                if (!defaultInvite.getPartStat().equals(inv.getPartStat()))
                                    instElt.addAttribute(MailService.A_APPT_PARTSTAT, inv.getPartStat());

                                if (!defaultInvite.getFreeBusy().equals(inv.getFreeBusy()))
                                    instElt.addAttribute(MailService.A_APPT_FREEBUSY, inv.getFreeBusy());
                                
                                if (!defaultInvite.getFreeBusyActual().equals(inv.getFreeBusyActual()))
                                    instElt.addAttribute(MailService.A_APPT_FREEBUSY_ACTUAL, inv.getFreeBusyActual());
                                
                                if (!defaultInvite.getTransparency().equals(inv.getTransparency()))
                                    instElt.addAttribute(MailService.A_APPT_TRANSPARENCY, inv.getTransparency());
                                
                                if (!defaultInvite.getName().equals(inv.getName()))
                                    instElt.addAttribute(MailService.A_NAME, inv.getName());
                                
                                if (!defaultInvite.getLocation().equals(inv.getLocation()))
                                    instElt.addAttribute(MailService.A_APPT_LOCATION, inv.getLocation());
                                
                                if (defaultInvite.isAllDayEvent() != inv.isAllDayEvent())
                                    instElt.addAttribute(MailService.A_APPT_ALLDAY, inv.isAllDayEvent());
                                if (defaultInvite.hasOtherAttendees() != inv.hasOtherAttendees())
                                    instElt.addAttribute(MailService.A_APPT_OTHER_ATTENDEES, inv.hasOtherAttendees());
                                if (defaultInvite.hasAlarm() != inv.hasAlarm())
                                    instElt.addAttribute(MailService.A_APPT_ALARM, inv.hasAlarm());
                                if (defaultInvite.isRecurrence() != inv.isRecurrence())
                                    instElt.addAttribute(MailService.A_APPT_RECUR, inv.isRecurrence());
                            }
                        } catch (MailServiceException.NoSuchItemException e) {
                            mLog.info("Error could not get instance "+inst.getMailItemId()+"-"+inst.getComponentNum()+
                                    " for appt "+appointment.getId(), e);
                        }
                    } // iterate all the instances
                    
                    
                    if (someInRange) { // if we found any appointments at all, we have to encode the "Default" data here
                        apptElt.addAttribute(MailService.A_APPT_STATUS, defaultInvite.getStatus());
                        apptElt.addAttribute(MailService.A_APPT_PARTSTAT, defaultInvite.getPartStat());
                        apptElt.addAttribute(MailService.A_APPT_FREEBUSY, defaultInvite.getFreeBusy());
                        apptElt.addAttribute(MailService.A_APPT_FREEBUSY_ACTUAL, defaultInvite.getFreeBusyActual());
                        apptElt.addAttribute(MailService.A_APPT_TRANSPARENCY, defaultInvite.getTransparency());
                        
                        apptElt.addAttribute(MailService.A_APPT_DURATION, defDurationMsecs);
                        apptElt.addAttribute(MailService.A_NAME, defaultInvite.getName());
                        apptElt.addAttribute(MailService.A_APPT_LOCATION, defaultInvite.getLocation());
                        
                        apptElt.addAttribute(MailService.A_ID, appointment.getId());
                        
                        ParsedItemID pid = ParsedItemID.Create(appointment.getId(), defaultInvite.getMailItemId());
                        apptElt.addAttribute(MailService.A_APPT_INV_ID, pid.toString());
                        
                        apptElt.addAttribute(MailService.A_APPT_COMPONENT_NUM, defaultInvite.getComponentNum());
                        
                        if (defaultInvite.isAllDayEvent())
                            apptElt.addAttribute(MailService.A_APPT_ALLDAY, defaultInvite.isAllDayEvent());
                        if (defaultInvite.hasOtherAttendees())
                            apptElt.addAttribute(MailService.A_APPT_OTHER_ATTENDEES, defaultInvite.hasOtherAttendees());
                        if (defaultInvite.hasAlarm())
                            apptElt.addAttribute(MailService.A_APPT_ALARM, defaultInvite.hasAlarm());
                        if (defaultInvite.isRecurrence())
                            apptElt.addAttribute(MailService.A_APPT_RECUR, defaultInvite.isRecurrence());
                        
                        { // FIXME -- shouldn't have to go back to the Mailbox to get the original Message here...
                            // fragment has already been sanitized...
                            Message defMsg = mbx.getMessageById(defaultInvite.getMailItemId()); 
                            String fragment = defMsg.getFragment();
                            if (!fragment.equals(""))
                                apptElt.addAttribute(MailService.E_FRAG, fragment, Element.DISP_CONTENT);
                        }
                        
                        response.addElement(apptElt);
                    }
                    
                } catch(MailServiceException.NoSuchItemException e) {
                    mLog.info("Error could not get default invite for Appt: "+ appointment.getId(), e);
                }
            }
            
            return response;
            
        } finally {
            sWatch.stop(startTime);
        }
    }
}
