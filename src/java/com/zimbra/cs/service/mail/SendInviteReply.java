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
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

import net.fortuna.ical4j.model.Calendar;

/**
 * @author tim
 */
public class SendInviteReply extends CalendarRequest {

    private static Log sLog = LogFactory.getLog(SendInviteReply.class);
    private static StopWatch sWatch = StopWatch.getInstance("SendInviteReply");
    
    private static final String[] TARGET_APPT_PATH = new String[] { MailService.A_ID };
    protected String[] getProxiedIdPath()     { return TARGET_APPT_PATH; }
    protected boolean checkMountpointProxy()  { return false; }

    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();

        try {
            ZimbraContext lc = getZimbraContext(context);
            Mailbox mbox = getRequestedMailbox(lc);
            Account acct = getRequestedAccount(lc);
            OperationContext octxt = lc.getOperationContext();

            ItemId iid = new ItemId(request.getAttribute(MailService.A_ID), lc);
            int compNum = (int) request.getAttributeLong(MailService.A_APPT_COMPONENT_NUM);

            String verbStr = request.getAttribute(MailService.A_VERB);
            ParsedVerb verb = parseVerb(verbStr);
            
            boolean updateOrg = request.getAttributeBool(MailService.A_APPT_UPDATE_ORGANIZER, true);
            // FIXME -- HACK until client is fixed
            updateOrg = true;

            if (sLog.isInfoEnabled()) {
                sLog.info("<SendInviteReply id=" + iid.toString(lc) + " verb=" + verb + " updateOrg=" + updateOrg + "> " + lc.toString());
            }
            
            Element response = lc.createElement(MailService.SEND_INVITE_REPLY_RESPONSE);
            
            synchronized (mbox) {
                Invite oldInv = null;
                int apptId; 
                int inviteMsgId;
                Appointment appt;
                
                // the user could be accepting EITHER the original-mail-item (id="nnn") OR the
                // appointment (id="aaaa-nnnn") --- work in both cases
                if (iid.hasSubpart()) {
                    // directly accepting the appointment
                    apptId = iid.getId();
                    inviteMsgId = iid.getSubpartId();
                    appt = mbox.getAppointmentById(octxt, apptId); 
                    oldInv = appt.getInvite(inviteMsgId, compNum);
                } else {
                    // accepting the message: go find the appointment and then the invite
                    inviteMsgId = iid.getId();
                    Message msg = mbox.getMessageById(octxt, inviteMsgId);
                    Message.ApptInfo info = msg.getApptInfo(compNum);
                    apptId = info.getAppointmentId();
                    appt = mbox.getAppointmentById(octxt, apptId);
                    oldInv = appt.getInvite(inviteMsgId, compNum);  
                }
                
                // see if there is a specific Exception being referenced by this reply...
                Element exc = request.getOptionalElement("exceptId");
                ParsedDateTime exceptDt = null;
                if (exc != null) {
                    Invite tmp = new Invite(new TimeZoneMap(acct.getTimeZone())); 
                    exceptDt = CalendarUtils.parseDateTime(exc, null, tmp);
                }
                
                if (updateOrg) {
                    String replySubject = getReplySubject(verb, oldInv);
                    
                    CalSendData csd = new CalSendData();
                    csd.mOrigId = oldInv.getMailItemId();
                    csd.mReplyType = TYPE_REPLY;
                    csd.mSaveToSent = shouldSaveToSent(acct);
                    csd.mInvite = CalendarUtils.replyToInvite(acct, oldInv, verb, replySubject, exceptDt);

                    Calendar iCal = csd.mInvite.toICalendar();
                    
                    ParseMimeMessage.MimeMessageData parsedMessageData = new ParseMimeMessage.MimeMessageData();
                    
                    // did they specify a custom <m> message?  If so, then we don't have to build one...
                    Element msgElem = request.getOptionalElement(MailService.E_MSG);
                    if (msgElem != null) {
                        MimeBodyPart[] mbps = new MimeBodyPart[1];
                        mbps[0] = CalendarUtils.makeICalIntoMimePart(oldInv.getUid(), iCal);
                        
                        // the <inv> element is *NOT* allowed -- we always build it manually
                        // based on the params to the <SendInviteReply> and stick it in the 
                        // mbps (additionalParts) parameter...
                        csd.mMm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, mbps, 
                                ParseMimeMessage.NO_INV_ALLOWED_PARSER, parsedMessageData);
                    } else {
                        // build a default "Accepted" response
                        csd.mMm = createDefaultReply(acct.getName(), oldInv, replySubject, verb, iCal); 
                    }
                    
                    sendCalendarMessage(lc, appt.getFolderId(), acct, mbox, csd, response, false);  
                }
                
                RecurId recurId = null;
                if (exceptDt != null) {
                    recurId = new RecurId(exceptDt, RecurId.RANGE_NONE);
                }
                ZAttendee me = oldInv.getMatchingAttendee(acct);
                String cnStr = null;
                String addressStr = acct.getName();
                String role = IcalXmlStrMap.ROLE_OPT_PARTICIPANT;
                int seqNo = oldInv.getSeqNo();
                long dtStamp = oldInv.getDTStamp();
                if (me != null) { 
                    if (me.hasCn()) {
                        cnStr = me.getCn();
                    }
                    addressStr = me.getAddress();
                    if (me.hasRole()) {
                        role = me.getRole();
                    }
                }

                mbox.modifyPartStat(octxt, apptId, recurId, cnStr, addressStr, role, verb.getXmlPartStat(), Boolean.FALSE, seqNo, dtStamp);

                // move the invite to the Trash if (a) the user wants it and (b) the user is doing the action themselves
                if (acct.getBooleanAttr(Provisioning.A_zimbraPrefDeleteInviteOnReply, true) && !lc.isDelegatedRequest()) {
                    try {
                        mbox.move(octxt, inviteMsgId, MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_TRASH);
                    } catch (MailServiceException.NoSuchItemException nsie) {
                        sLog.debug("can't move nonexistent invite to Trash: " + inviteMsgId);
                    }
                }
            }

            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    public static String getReplySubject(ParsedVerb verb, Invite inv) {
        return verb + ": " + inv.getName();
    }

    public static MimeMessage createDefaultReply(String from, Invite inv, String replySubject, ParsedVerb verb, Calendar iCal)
    throws ServiceException {
        /////////
        // Build the default text part and add it to the mime multipart
        StringBuffer replyText = new StringBuffer(from);
        replyText.append(" has ");
        replyText.append(verb.toString());
        replyText.append(" your invitation");
        
        return CalendarUtils.createDefaultCalendarMessage(from, inv.getOrganizer().getCalAddress().getSchemeSpecificPart(), replySubject, 
                replyText.toString(), inv.getUid(), iCal);
    }
    
    public final static class ParsedVerb {
        String mName;
        String mPartStat;      // XML participant status
        public ParsedVerb(String name, String xmlPartStat) {
            mName = name;
            mPartStat = xmlPartStat;
        }
        public String toString() { return mName; }
        public String getXmlPartStat() { return mPartStat; }
    }
    
    public final static ParsedVerb VERB_ACCEPT = new ParsedVerb("ACCEPT", IcalXmlStrMap.PARTSTAT_ACCEPTED);
    public final static ParsedVerb VERB_DECLINE = new ParsedVerb("DECLINE", IcalXmlStrMap.PARTSTAT_DECLINED);
    public final static ParsedVerb VERB_TENTATIVE = new ParsedVerb("TENTATIVE", IcalXmlStrMap.PARTSTAT_TENTATIVE);
    
    protected static HashMap /* string, parsedverb */ sVerbs;
    static {
        sVerbs = new HashMap();
        sVerbs.put(MailService.A_APPT_ACCEPT, VERB_ACCEPT); 
        sVerbs.put(MailService.A_APPT_DECLINE, VERB_DECLINE); 
        sVerbs.put(MailService.A_APPT_TENTATIVE, VERB_TENTATIVE); 
    }
    
    public static ParsedVerb parseVerb(String str) throws ServiceException
    {
        Object obj = sVerbs.get(str.toLowerCase());
        if (obj != null)
            return (ParsedVerb)obj;
        throw ServiceException.INVALID_REQUEST("Unknown Reply Verb: " + str, null);
    }
}
