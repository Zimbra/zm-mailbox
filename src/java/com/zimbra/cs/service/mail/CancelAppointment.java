package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.property.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.LiquidContext;

public class CancelAppointment extends CalendarRequest {

    private static Log sLog = LogFactory.getLog(CancelAppointment.class);
    private static StopWatch sWatch = StopWatch.getInstance("CancelAppointment");
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        try {
            LiquidContext lc = getLiquidContext(context);
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();
            
            ParsedItemID pid = ParsedItemID.Parse(request.getAttribute("id"));
            int compNum = (int)request.getAttributeLong("comp");
            
            sLog.info("<CancelAppointment id="+pid+" comp="+compNum+">");
            
            synchronized (mbox) {
                Appointment appt = mbox.getAppointmentById(pid.getItemIDInt()); 
                Invite inv = appt.getInvite(pid.getSubIdInt(), compNum);

                if (appt == null) {
                    throw MailServiceException.NO_SUCH_APPOINTMENT(inv.getUid(), " for CancelAppointmentRequest("+pid+","+compNum+")");
                }
                
                Invite defaultInv = appt.getDefaultInvite();
                if (defaultInv.getMailItemId() != pid.getSubIdInt()) {
                    throw ServiceException.INVALID_REQUEST("Specified Invite ID "+pid+" is not the Default Invite for appt "+appt.getId(), null);
                }

/*******
 * TIM: remove the "cancel a single instance of a recurring appointment" from here, 
 * and put it in the "CancelAppointmentException" API.  The reason is that, from the 
 * client's perspective, cancelling a particular instance is the same thing as 
 * cancelling a particular exception --- or at least it is easier to think about it 
 * that way.  Leaving this commented-out code here for now until I've thought about 
 * it a little longer....
 *  

                Element recurElt = request.getOptionalElement("inst");
                if (recurElt != null) {
                    if (inv.hasRecurId()) {
                        throw MailServiceException.CANNOT_CANCEL_INSTANCE_OF_EXCEPTION(" for CancelAppointmentRequest("+pid+","+compNum+")");
                    }
                    cancelInstance(octxt, request, acct, mbox, inv, recurElt);
                } else {
                
*****/                
                    // if recur is not set, then we're cancelling the entire appointment...
                    
                    // first, pull a list of all the invites and THEN start cancelling them: since cancelling them
                    // will remove them from the appointment's list, we can get really confused if we just directly
                    // iterate through the list...
                    
                    Invite invites[] = new Invite[appt.numInvites()];
                    for (int i = appt.numInvites()-1; i>=0; i--) {
                        try {
                            invites[i] = appt.getInvite(i);
                        } catch (MailServiceException.NoSuchItemException e) {
                            sLog.info("Error couldn't load invite "+i+" for appointment "+appt.getId(), e);
                        }
                    }
                    
                    for (int i = invites.length-1; i >= 0; i--) {
                        if (invites[i] != null && invites[i].getMethod().equals(Method.REQUEST.getValue())) {
                            cancelInvite(octxt, request, acct, mbox, invites[i]);
                        }
                    }
//                }
            } // synchronized on mailbox
            
            Element response = lc.createElement(MailService.CANCEL_APPOINTMENT_RESPONSE);
            return response;
        } finally {
            sWatch.stop(startTime);
        }        
    }
    
    void cancelInstance(OperationContext octxt, Element request, Account acct, Mailbox mbox, Invite defaultInv, Element recurElt) 
    throws ServiceException {
        List /* ICalTimeZone */ referencedTimeZones = new ArrayList();
        RecurId recurId = CalendarUtils.parseRecurId(recurElt, defaultInv.getTimeZoneMap(), referencedTimeZones, acct.getTimeZone());

        cancelInstance(octxt, request, acct, mbox, defaultInv, recurId);
    }
    
    
    protected void cancelInstance(OperationContext octxt, Element request, Account acct, Mailbox mbox, Invite defaultInv, RecurId recurId) 
    throws ServiceException {
        String text = "The instance has been cancelled";
        String subject = "CANCELLED: "+defaultInv.getName();
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("Sending cancellation message \""+subject+"\" for instance "+recurId.toString()+" of invite "+ defaultInv.toString());
        }
        
        Calendar iCal= CalendarUtils.buildCancelInstanceCalendar(acct, defaultInv, text, recurId);
        
        CalSendData dat = new CalSendData();
        dat.mOrigId = defaultInv.getMailItemId();
        dat.mReplyType = TYPE_REPLY;
        dat.mSaveToSent = shouldSaveToSent(acct);
        
        // did they specify a custom <m> message?  If so, then we don't have to build one...
        Element msgElem = request.getOptionalElement(MailService.E_MSG);
        
        if (msgElem != null) {
            MimeBodyPart[] mbps = new MimeBodyPart[1];
            mbps[0] = CalendarUtils.makeICalIntoMimePart(defaultInv.getUid(), iCal);
            
            // the <inv> element is *NOT* allowed -- we always build it manually
            // based on the params to the <CancelAppointment> and stick it in the 
            // mbps (additionalParts) parameter...
            dat.mMm = ParseMimeMessage.parseMimeMsgSoap(octxt, mbox, msgElem, mbps, 
                    ParseMimeMessage.NO_INV_ALLOWED_PARSER, dat);
            
        } else {
            List /* URI */ atURIs = CalendarUtils.toListFromAts(defaultInv.getAttendees());

            dat.mMm = CalendarUtils.createDefaultCalendarMessage(acct, atURIs, subject, text, 
                    defaultInv.getUid(), iCal);
        }
        
        if (!defaultInv.thisAcctIsOrganizer(acct)) {
            try {
                Address[] rcpts = dat.mMm.getAllRecipients();
                if (rcpts != null && rcpts.length > 0) {
                    throw MailServiceException.MUST_BE_ORGANIZER("CancelAppointment");
                }
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
            }
        }
        
        sendCalendarMessage(octxt, acct, mbox, dat, null);
    }
    
    
    protected void cancelInvite(OperationContext octxt, Element request, Account acct, Mailbox mbox, Invite inv)
    throws ServiceException {
        String text = "The event has been cancelled";
        String subject = "CANCELLED: "+inv.getName();
        
        if (sLog.isDebugEnabled())
            sLog.debug("Sending cancellation message \""+subject+"\" for "+ inv.toString());
        
        Calendar iCal= CalendarUtils.buildCancelInviteCalendar(acct, inv, text);
        
        CalSendData dat = new CalSendData();
        dat.mOrigId = inv.getMailItemId();
        dat.mReplyType = TYPE_REPLY;
        dat.mSaveToSent = shouldSaveToSent(acct);
        
        // did they specify a custom <m> message?  If so, then we don't have to build one...
        Element msgElem = request.getOptionalElement(MailService.E_MSG);
        
        if (msgElem != null) {
            MimeBodyPart[] mbps = new MimeBodyPart[1];
            mbps[0] = CalendarUtils.makeICalIntoMimePart(inv.getUid(), iCal);
            
            // the <inv> element is *NOT* allowed -- we always build it manually
            // based on the params to the <CancelAppointment> and stick it in the 
            // mbps (additionalParts) parameter...
            dat.mMm = ParseMimeMessage.parseMimeMsgSoap(octxt, mbox, msgElem, mbps, 
                    ParseMimeMessage.NO_INV_ALLOWED_PARSER, dat);
            
        } else {
            List /* URI */ atURIs = CalendarUtils.toListFromAts(inv.getAttendees());

            dat.mMm = CalendarUtils.createDefaultCalendarMessage(acct, atURIs, subject, text, 
                    inv.getUid(), iCal);
        }
        
        if (!inv.thisAcctIsOrganizer(acct)) {
            try {
                Address[] rcpts = dat.mMm.getAllRecipients();
                if (rcpts != null && rcpts.length > 0) {
                    throw MailServiceException.MUST_BE_ORGANIZER("CancelAppointment");
                }
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Checking recipients of outgoing msg ", e);
            }
        }
        
        sendCalendarMessage(octxt, acct, mbox, dat, null);
    }
     
}
