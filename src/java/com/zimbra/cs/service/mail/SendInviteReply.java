/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 2, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.*;
import javax.mail.internet.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.*;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.ZimbraContext;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.RecurrenceId;

/**
 * @author tim
 */
public class SendInviteReply extends SendMsg {

    private static Log sLog = LogFactory.getLog(SendInviteReply.class);
    private static StopWatch sWatch = StopWatch.getInstance("SendInviteReply");
    
    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();

        try {
            ZimbraContext lc = getZimbraContext(context);
            Mailbox mbox = getRequestedMailbox(lc);
            Account acct = this.getRequestedAccount(lc);
            OperationContext octxt = lc.getOperationContext();

//            int invId = (int)request.getAttributeLong("id");
            ParsedItemID pid = ParsedItemID.parse(request.getAttribute("id"));
            int compNum = (int)request.getAttributeLong("compNum");
            
            String verbStr = request.getAttribute(MailService.A_VERB);
            ParsedVerb verb = parseVerb(verbStr);
            
            boolean updateOrg = request.getAttributeBool(MailService.A_APPT_UPDATE_ORGANIZER, true);
            // FIXME -- HACK until client is fixed
            updateOrg = true;
            
            if (sLog.isInfoEnabled()) {
                sLog.info("<SendInviteReply id=" + pid.toString() + " verb=" + verb +" updateOrg="+updateOrg+"> " + lc.toString());
            }
            
            int replyId = 0;
            
            synchronized (mbox) {
                Invite inv = null;
                
                int apptId; 
                int inviteMsgId;
                
                // the user could be accepting EITHER the original-mail-item (id="nnn") OR the
                // appointment (id="aaaa-nnnn") --- work in both cases
                if (pid.hasSubId()) {
                    // directly accepting the appointment
                    apptId = pid.getItemIDInt();
                    inviteMsgId = pid.getSubIdInt();
                    Appointment appt = mbox.getAppointmentById(apptId); 
                    inv = appt.getInvite(inviteMsgId, compNum);
                } else {
                    // accepting the message: go find the appointment and then the invite
                    inviteMsgId = pid.getItemIDInt();
                    Message msg = mbox.getMessageById(inviteMsgId);
                    Message.ApptInfo info = msg.getApptInfo(compNum);
                    apptId = info.getAppointmentId();
                    Appointment appt = mbox.getAppointmentById(apptId);
                    inv = appt.getInvite(inviteMsgId, compNum);  
                }
                
                List /*<ICalTimeZone>*/ tzsReferenced = new ArrayList();
                
                // see if there is a specific Exception being referenced by this reply...
                Element exc = request.getOptionalElement("exceptId");
                ParsedDateTime exceptDt = null;
                if (exc != null) {
                    exceptDt = CalendarUtils.parseDateTime(exc, null, tzsReferenced, acct.getTimeZone());
                }
                
                if (updateOrg) {
                    String replySubject = this.getReplySubject(verb, inv);
                    
                    Calendar iCal = CalendarUtils.buildReplyCalendar(acct, inv, verb, replySubject, tzsReferenced, exceptDt);
//                  System.out.println("GENERATED ICAL:\n"+iCal.toString());
                    
                    MimeMessage toSend = null;
                    
                    ParseMimeMessage.MimeMessageData parsedMessageData = new ParseMimeMessage.MimeMessageData();
                    
                    // did they specify a custom <m> message?  If so, then we don't have to build one...
                    Element msgElem = request.getOptionalElement(MailService.E_MSG);
                    if (msgElem != null) {
                        MimeBodyPart[] mbps = new MimeBodyPart[1];
                        mbps[0] = CalendarUtils.makeICalIntoMimePart(inv.getUid(), iCal);
                        
                        // the <inv> element is *NOT* allowed -- we always build it manually
                        // based on the params to the <SendInviteReply> and stick it in the 
                        // mbps (additionalParts) parameter...
                        toSend = ParseMimeMessage.parseMimeMsgSoap(octxt, mbox, msgElem, mbps, 
                                ParseMimeMessage.NO_INV_ALLOWED_PARSER, parsedMessageData);
                    } else {
                        // build a default "Accepted" response
                        toSend = createDefaultReply(acct, inv, replySubject, verb, iCal); 
                    }
                    
                    try {
                        System.out.println(iCal.toString());
                    } catch(Exception e) {}
                    
                    replyId = sendMimeMessage(octxt, mbox, acct, shouldSaveToSent(acct), parsedMessageData, 
                            toSend, inv.getMailItemId(), TYPE_REPLY);
                    
                }
                
                RecurId recurId = null;
                if (exceptDt != null) {
                    recurId = new RecurId(exceptDt, RecurId.RANGE_NONE);
                }
                Attendee me = inv.getMatchingAttendee(acct);
                String cnStr = null;
                String addressStr = acct.getName();
                String role = IcalXmlStrMap.ROLE_OPT_PARTICIPANT;
                int seqNo = inv.getSeqNo();
                long dtStamp = inv.getDTStamp();
                if (me != null) {
                    cnStr = null;
                    if (me.getParameters().getParameter(Parameter.CN) != null) {
                        cnStr = me.getParameters().getParameter(Parameter.CN).getValue();
                    }
                    addressStr = me.getCalAddress().getSchemeSpecificPart();
                    role = IcalXmlStrMap.sRoleMap.toXml(me.getParameters().getParameter(Parameter.ROLE).getValue());
                }
                
//              mbox.modifyInvitePartStat(octxt, apptId, inviteMsgId, compNum, false, verb.getXmlPartStat());
                mbox.modifyPartStat(octxt, apptId, recurId, cnStr, addressStr, role, verb.getXmlPartStat(), Boolean.FALSE, seqNo, dtStamp);
                        
                if (acct.getBooleanAttr(Provisioning.A_zimbraPrefDeleteInviteOnReply, true)) {
                    mbox.move(octxt, inviteMsgId, MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_TRASH);
                }
            }
            
            Element response = lc.createElement(MailService.SEND_INVITE_REPLY_RESPONSE);
            if (replyId != 0) {
                response.addAttribute(MailService.A_ID, replyId);
            }

            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    String getReplySubject(ParsedVerb verb, Invite inv) {
        StringBuffer toRet = new StringBuffer(verb.toString());
        toRet.append(": ");
        toRet.append(inv.getName());
        
        return toRet.toString();
    }
    

    MimeMessage createDefaultReply(Account acct, Invite inv, 
            String replySubject, ParsedVerb verb, Calendar iCal) throws ServiceException 
    {
        /////////
        // Build the default text part and add it to the mime multipart
        StringBuffer replyText = new StringBuffer(acct.getName());
        replyText.append(" has ");
        replyText.append(verb.toString());
        replyText.append(" your invitation");
        
        return CalendarUtils.createDefaultCalendarMessage(acct, inv.getOrganizer().getCalAddress(), replySubject, 
                replyText.toString(), inv.getUid(), iCal);
    }
    
    protected final static class ParsedVerb {
        String name;
        String xmlPartStat;      // XML participant status
        String iCalPartStat;     // iCal participant status
        public ParsedVerb(String name, String xmlPartStat, String iCalPartStat) {
            this.name = name;
            this.xmlPartStat = xmlPartStat;
            this.iCalPartStat = iCalPartStat;
        }
        public String toString() { return name; }
        String getXmlPartStat() { return xmlPartStat; }
        String getICalPartStat() { return iCalPartStat; }
    }
    
    protected final static ParsedVerb VERB_ACCEPT =
        new ParsedVerb("ACCEPT",
                       IcalXmlStrMap.PARTSTAT_ACCEPTED,
                       PartStat.ACCEPTED.getValue());
    protected final static ParsedVerb VERB_DECLINE =
        new ParsedVerb("DECLINE",
                       IcalXmlStrMap.PARTSTAT_DECLINED,
                       PartStat.DECLINED.getValue());
    protected final static ParsedVerb VERB_TENTATIVE =
        new ParsedVerb("TENTATIVE",
                       IcalXmlStrMap.PARTSTAT_TENTATIVE,
                       PartStat.TENTATIVE.getValue());
    
    protected static HashMap /* string, parsedverb */ sVerbs;
    static {
        sVerbs = new HashMap();
        sVerbs.put(MailService.A_APPT_ACCEPT, VERB_ACCEPT); 
        sVerbs.put(MailService.A_APPT_DECLINE, VERB_DECLINE); 
        sVerbs.put(MailService.A_APPT_TENTATIVE, VERB_TENTATIVE); 
    }
    
    protected ParsedVerb parseVerb(String str) throws ServiceException
    {
        Object obj = sVerbs.get(str.toLowerCase());
        if (obj != null)
            return (ParsedVerb)obj;
        throw ServiceException.INVALID_REQUEST("Unknown Reply Verb: " + str, null);
    }
}
