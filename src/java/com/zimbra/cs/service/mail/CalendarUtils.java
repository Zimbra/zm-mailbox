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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.JMSession;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Comment;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.DateTimeFormat;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.account.ldap.LdapUtil;


public class CalendarUtils {
    private static Log sLog = LogFactory.getLog(CalendarUtils.class);
    
    // HACKAHACKAHACKA -- for compatability with "outlook", use midnight-midnight for "all day" appointments
    private final static boolean USE_BROKEN_OUTLOOK_MODE = true;
    
    static MimeBodyPart makeICalIntoMimePart(String uid, Calendar iCal) 
    throws ServiceException
    {
        try {
            MimeBodyPart mbp = new MimeBodyPart();

            String filename = "meeting.ics";
            mbp.setDataHandler(new DataHandler(new CalendarDataSource(iCal, uid, filename))); 
            
            return mbp;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Failure creating MimeBodyPart for InviteReply", e);
        }
    }
    
    /**
     * Builds the TO: list for appointment updates by iterating over the list of ATTENDEEs
     * 
     * @param iter
     * @return
     */
    static List /* URI */ toListFromAts(List /* Attendee */ list) {
        List /* URI */ atURIs= new ArrayList();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Attendee curAt = (Attendee)iter.next();
            atURIs.add(curAt.getCalAddress());
        }
        return atURIs;
    }
    

    static MimeMessage createDefaultCalendarMessage(Account acct, URI toURI,
            String subject, String text, String uid, Calendar cal) throws ServiceException {
        List list = new ArrayList();
        list.add(toURI);
        return createDefaultCalendarMessage(acct, list, subject, text, uid, cal);
    }
    
    
    static MimeMessage createDefaultCalendarMessage(Account acct, List /* URI */ toAts,
            String subject, String text, String uid, Calendar cal) throws ServiceException 
    {
        try {
            MimeMessage mm = new MimeMessage(JMSession.getSession()) { protected void updateHeaders() throws MessagingException { String msgid = getMessageID(); super.updateHeaders(); if (msgid != null) setHeader("Message-ID", msgid); } };
            MimeMultipart mmp = new MimeMultipart("alternative");
            mm.setContent(mmp);

            /////////
            // TEXT part (add me first!) 
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(text); // implicitly sets content-type to "text/plain"
            mmp.addBodyPart(textPart);
            
            /////////
            // CALENDAR part
            MimeBodyPart icalPart = CalendarUtils.makeICalIntoMimePart(uid, cal);
            mmp.addBodyPart(icalPart);
            
            /////////
            // MESSAGE HEADERS
            mm.setSubject(subject);
            
            Address[] addrs = new Address[toAts.size()];
            
            for (int i = toAts.size()-1; i>=0; i--) {
                String toAddr = ((URI)toAts.get(i)).getSchemeSpecificPart();
                InternetAddress addr = new InternetAddress(toAddr);
                addrs[i] = addr;
            }
            mm.addRecipients(javax.mail.Message.RecipientType.TO, addrs);
            mm.setFrom(new InternetAddress(acct.getName()));
            mm.setSentDate(new Date());
            mm.saveChanges();
            
            return mm;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Messaging Exception while building InviteReply", e);
        }
    }
    
    /**
     * Parse an <inv> element in a CREATE context: new UID
     * 
     * @param inviteElem
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteForCreate(Account account, Element inviteElem) throws ServiceException 
    {
        return parseInviteForCreate(account, inviteElem, null, null, false);
    }
    
    
    /**
     * Parse an <inv> element 
     * 
     * @param inviteElem
     * @param uid -- optional UID to use
     * @param recurrenceId -- optional RECURRENCE_ID to add to VEvent
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteForCreate(
            Account account, Element inviteElem,
            TimeZoneMap tzMap,
            String uid, boolean recurrenceIdAllowed) throws ServiceException 
    {
        List /*<ICalTimeZone>*/ tzsReferenced = new ArrayList();

        Component invDat = CalendarUtils.parseInviteElementCommon(account, inviteElem, tzMap, tzsReferenced);
        VEvent event = (VEvent)invDat;
        
        if (uid == null || uid.equals("")) {
            uid = LdapUtil.generateUUID();
        }
        event.getProperties().add(new Uid(uid));
        
        if (recurrenceIdAllowed) {
            Element e = inviteElem.getElement("exceptId");
            ParsedDateTime dt = parseDateTime(e, tzMap, tzsReferenced, account.getTimeZone());
            RecurrenceId recurId = new RecurrenceId(dt.iCal4jDate());
            TimeZone tz = dt.getTimeZone();
            if (tz == ICalTimeZone.getUTC()) {
                recurId.setUtc(true);
            } else {
                recurId.getParameters().add(new TzId(tz.getID()));
            }
            event.getProperties().add(recurId);
        } else {
            if (inviteElem.getOptionalElement("exceptId") != null) {
                throw MailServiceException.INVALID_REQUEST("May not specify an <exceptId> in this request", null);
            }
        }
        
        // DTSTAMP
        event.getProperties().add(new DtStamp(new net.fortuna.ical4j.model.DateTime()));
      
      // SEQUENCE
        event.getProperties().add(new Sequence(0));

        // build the full calendar wrapper now...
        Calendar iCal = makeCalendar(Method.REQUEST);

        for (Iterator iter = tzsReferenced.iterator(); iter.hasNext();) {
            ICalTimeZone cur = (ICalTimeZone) iter.next();
            VTimeZone vtz = cur.toVTimeZone();
            iCal.getComponents().add(vtz);
        }
        
        String str = event.toString();
        System.out.println(str);
        
        iCal.getComponents().add(event);
        
        try {
            iCal.validate(true);
        } catch (ValidationException e) { 
            sLog.info("iCal Validation Exception in CreateAppointmentInviteParser", e);
            if (e.getCause() != null) {
                sLog.info("\tcaused by "+e.getCause(), e.getCause());
            }
        }
        
        String summaryStr = "";
        Property propSum = event.getProperties().getProperty(Property.SUMMARY);
        if (propSum != null) {
            summaryStr = ((Summary)propSum).getValue();
        }
        
        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = uid;
        toRet.mSummary = summaryStr;
        
        return toRet;
    }
    
    private static Calendar makeCalendar(Method method) {
        Calendar iCal = new Calendar();
        // PRODID, VERSION always required
        iCal.getProperties().add(new ProdId("Zimbra-Calendar-Provider"));
        iCal.getProperties().add(method);
        iCal.getProperties().add(Version.VERSION_2_0);

        return iCal;
    }
    

    
    /**
     * Parse an <inv> element in a Modify context -- existing UID, etc
     * 
     * @param inviteElem
     * @param inv is the Default Invite of the appointment we are modifying 
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteForModify(Account account, Element inviteElem, 
            Invite inv, List /* Attendee */ attendeesToCancel) throws ServiceException 
    {
        List /*<ICalTimeZone>*/ tzsReferenced = new ArrayList();

        Component invDat = CalendarUtils.parseInviteElementCommon(account, inviteElem, inv.getTimeZoneMap(), tzsReferenced);
        VEvent event = (VEvent)invDat;

        // use UID from old inv
        String uid = inv.getUid();
        event.getProperties().add(new Uid(uid));
        
        // DTSTAMP
        event.getProperties().add(new DtStamp(new net.fortuna.ical4j.model.DateTime()));
      
        // SEQUENCE
        event.getProperties().add(new Sequence(inv.getSeqNo()+1));
        
        if (inv.hasRecurId()) {
            event.getProperties().add(inv.getRecurId().getRecurrenceId(account));
        }
        
        // compare the new attendee list with the existing one...if attendees have been removed, then
        // we need to send them individual cancelation messages
        PropertyList newAts = event.getProperties().getProperties(Property.ATTENDEE);
        List /* Attendee */ oldAts = inv.getAttendees();
        for (Iterator iter = oldAts.iterator(); iter.hasNext();) {
            Attendee cur = (Attendee)iter.next();
            if (!listContains(newAts, cur)) {
                attendeesToCancel.add(cur);
            }
        }
        
        
        // build the full calendar wrapper: 
        Calendar iCal = makeCalendar(Method.REQUEST);
        
        for (Iterator iter = tzsReferenced.iterator(); iter.hasNext();) {
            ICalTimeZone cur = (ICalTimeZone) iter.next();
            VTimeZone vtz = cur.toVTimeZone();
            iCal.getComponents().add(vtz);
        }
        
        iCal.getComponents().add(event);
        
        try {
            iCal.validate(true);
        } catch (ValidationException e) { 
            sLog.info("iCal Validation Exception in ModifyAppointmentInviteParser", e);
            if (e.getCause() != null) {
                sLog.info("\tcaused by "+e.getCause(), e.getCause());
            }
        }
        
        String summaryStr = "";
        Property propSum = event.getProperties().getProperty(Property.SUMMARY);
        if (propSum != null) {
            summaryStr = ((Summary)propSum).getValue();
        }
        
        
        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = uid;
        toRet.mSummary = summaryStr;
        
        return toRet;
    }
    
    // TRUE if the list contains the atendee, comparing by URI
    private static boolean listContains(List /* attendee */ list, Attendee at) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Attendee cur = (Attendee)iter.next();
            if (cur.getCalAddress().equals(at.getCalAddress())) {
                return true;
            }
        }
        return false;
    }
    
    // TRUE if the list contains the atendee, comparing by URI
    private static boolean listContains(PropertyList list, Attendee at) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Attendee cur = (Attendee)iter.next();
            if (cur.getCalAddress().equals(at.getCalAddress())) {
                return true;
            }
        }
        return false;
    }

    static RecurId parseRecurId(Element e,
                                TimeZoneMap invTzMap,
                                List /*<ICalTimeZone>*/ referencedTimeZones,
                                ICalTimeZone localTZ) 
    throws ServiceException
    {
        String range = e.getAttribute("range", "");
        
        ParsedDateTime dt = parseDateTime(e, invTzMap, referencedTimeZones, localTZ);
        return new RecurId(dt, range);
    }

    /**
     * Parse a date from the enclosed element.  If the element has a TimeZone reference, then
     * 
     * @param e
     * @param referencedTimeZones
     * @return obj[0] is a Date, obj[1] is a TimeZone
     * @throws ServiceException
     */
    private static ParsedDateTime parseDateTime(Element e,
                                                TimeZoneMap invTzMap,
                                                List /*<ICalTimeZone>*/ referencedTimeZones,
                                                ICalTimeZone localTZ)
    throws ServiceException
    {
        String d = e.getAttribute(MailService.A_APPT_DATETIME, null);
        String tz = e.getAttribute(MailService.A_APPT_TIMEZONE, null);
        return parseDateTime(e.getName(), d, tz, invTzMap, referencedTimeZones, localTZ);
    }
    
    private static ParsedDateTime parseDateTime(String eltName,
                                                String d,
                                                String tz,
                                                TimeZoneMap invTzMap,
                                                List /*<ICalTimeZone>*/ tzsReferenced,
                                                ICalTimeZone localTZ)
    throws ServiceException
    {
        try {
            ICalTimeZone zone = null;
            if (tz != null)
                zone = lookupAndAddToTzList(tz, invTzMap, tzsReferenced);
            else if (d.indexOf('Z') == -1) {
                if (!tzsReferenced.contains(localTZ))
                    tzsReferenced.add(localTZ);
            }
            return ParsedDateTime.parse(d, tz, zone, localTZ);
        } catch (ParseException ex) {
            throw MailServiceException.INVALID_REQUEST("could not parse time "+d+" in element "+eltName, ex);
        }
    }

    private static ICalTimeZone lookupAndAddToTzList(String tzId,
    		                                         TimeZoneMap invTzMap,
													 List /*<ICalTimeZone>*/ tzsReferenced)
    throws ServiceException {
        // Workaround for bug in Outlook, which double-quotes TZID parameter
        // value in properties like DTSTART, DTEND, etc.  Use unquoted tzId.
        int len = tzId.length();
        if (len >= 2 && tzId.charAt(0) == '"' && tzId.charAt(len - 1) == '"')
            tzId = tzId.substring(1, len - 1);

        WellKnownTimeZone knownTZ = Provisioning.getInstance().getTimeZoneById(tzId);
        ICalTimeZone zone = null;
        if (knownTZ != null)
            zone = knownTZ.toTimeZone();
        if (zone == null) {
            // Could be a custom TZID during modify operation of invite from
            // external calendar system.  Look up the TZID from the invite.
        	if (invTzMap != null)
        		zone = invTzMap.getTimeZone(tzId);
            if (zone == null)
                throw MailServiceException.INVALID_REQUEST("invalid time zone \"" + tzId + "\"", null);
        }
        if (!tzsReferenced.contains(zone))
            tzsReferenced.add(zone);
        if (invTzMap != null)
            invTzMap.add(zone);
        return zone;
    }

    private static void testParse(String d, String tz, List tzsInUse, ICalTimeZone localTZ) 
    {        
        String elt = "<test";
        if (d != null) {
            elt += "d=\""+d+"\"";
        }
        if (tz != null) {
            elt += "tz=\""+tz+"\"";
        }

        elt+="/>";

        TimeZoneMap tzMap = new TimeZoneMap(localTZ);
        try {
            System.out.println("parseDate "+elt+" returns \""+parseDateTime("test", d, tz, tzMap, tzsInUse, localTZ)+"\"");
        } catch(ServiceException e) {
            System.out.println("Caught exception: "+e);
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        List tzs = new ArrayList();
        WellKnownTimeZone USPacific =
            Provisioning.getInstance()
            .getTimeZoneById("(GMT-08.00) Pacific Time (US & Canada) / Tijuana");
        ICalTimeZone pacificTZ = USPacific.toTimeZone(); 

        testParse("20050305T02000", null, tzs, pacificTZ);
        testParse("20050305T03000", "(GMT-08.00) Pacific Time (US & Canada) / Tijuana", tzs, pacificTZ);
        testParse("20050305", null, tzs, pacificTZ);
        testParse("20050305T190030", "(GMT-07.00) Mountain Time (US & Canada)", tzs, pacificTZ);

        System.out.println();
        for (Iterator iter = tzs.iterator(); iter.hasNext();) {
            ICalTimeZone tz = (ICalTimeZone) iter.next();
            System.out.println("TimeZone in use: " + tz.getID());
            VTimeZone vtz = tz.toVTimeZone();
            System.out.println(vtz.toString());
            System.out.println();
        }

        try {
            ParsedDuration.parse("P15DT5H20S");
            ParsedDuration.parse("-P1W");
            ParsedDuration.parse("PT12H27S");
            ParsedDuration.parse("-P12DT3S");
        } catch (ServiceException e) {
            System.out.println("Error: "+e);
            e.printStackTrace();
        }
    }
    
    private static void parseRecur(Element recurElt,
                                   Component comp,
                                   TimeZoneMap invTzMap,
                                   List /*<ICalTimeZone>*/ tzsReferenced,
                                   ICalTimeZone localTZ)
    throws ServiceException {
        for (Iterator iter= recurElt.elementIterator(); iter.hasNext();) {
            Element e = (Element)iter.next();
            
            boolean exclude = false;
            
            if (e.getName().equals(MailService.E_APPT_EXCLUDE)) {
                exclude = true;
            } else {
                if (!e.getName().equals(MailService.E_APPT_ADD)) {
                    throw MailServiceException.INVALID_REQUEST("<add> or <exclude> expected inside <recur>", null);
                }
            }
            
            for (Iterator intIter = e.elementIterator(); intIter.hasNext();) 
            {
                Element intElt = (Element)intIter.next();
                
                if (intElt.getName().equals(MailService.E_APPT_DATE)) {
                    // handle RDATE or EXDATE
                    
                    ParsedDateTime dt = parseDateTime(intElt, invTzMap, tzsReferenced, localTZ);
                    
                    try {
                        String dstr = intElt.getAttribute(MailService.A_APPT_DATETIME);
                        DateList dl;
                        boolean isDateTime = true;
                        try {
                            dl = new DateList(dstr, Value.DATE_TIME);
                        } catch (ParseException ex) {
                            dl = new DateList(dstr, Value.DATE);
                            isDateTime = false;
                        }
//                        
//                        if (cal.getTimeZone() == ICalTimeZone.getUTC()) {
////                            dl.setUtc(true);
//                        }
                        
                        Property prop;
                        if (exclude) {
                            prop = new ExDate(dl);
                        } else {
                            prop = new RDate(dl);
                        }
                        
                        if (isDateTime) {
                            if (dt.getTimeZone() != ICalTimeZone.getUTC()) {
                                prop.getParameters().add(new TzId(dt.getTZName()));
                            }
                        }
                        
                        comp.getProperties().add(prop);
                    } catch (Exception ex) {
                        throw MailServiceException.INVALID_REQUEST("Exception parsing <recur><date> d="+
                                intElt.getAttribute(MailService.A_APPT_DATETIME), ex);
                    }
                } else if (intElt.getName().equals(MailService.E_APPT_RULE)) {
                    // handle RRULE or EXRULE

                    // Turn XML into iCal RECUR string, which will then be
                    // parsed by ical4j Recur object.

                    StringBuffer recurBuf = new StringBuffer(100);

                    String freq = IcalXmlStrMap.sFreqMap.toIcal(
                    		          intElt.getAttribute(MailService.A_APPT_RULE_FREQ));
                    recurBuf.append("FREQ=").append(freq);

                    for (Iterator ruleIter = intElt.elementIterator(); ruleIter.hasNext(); ) {
                    	Element ruleElt = (Element) ruleIter.next();
                        String ruleEltName = ruleElt.getName();
                        if (ruleEltName.equals(MailService.E_APPT_RULE_UNTIL)) {
                            recurBuf.append(";UNTIL=");
                            String d = ruleElt.getAttribute(MailService.A_APPT_DATETIME);
                            String tz = ruleElt.getAttribute(MailService.A_APPT_TIMEZONE, null);
                            if (tz == null) {
                                recurBuf.append(d);
                            } else {
                                // If UNTIL has time part it must be specified
                                // as UTC time, i.e. ending in "Z".
                                // (RFC2445 Section 4.3.10 Recurrence Rule)
                                ParsedDateTime untilCal = parseDateTime(ruleEltName, d, tz, invTzMap, tzsReferenced, localTZ);
                                DateTimeFormat dtf = DateTimeFormat.getInstance();
                                String dtUTC = dtf.format(untilCal.getDate(), true);
                                recurBuf.append(dtUTC);
                            }
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_COUNT)) {
                            int num = (int) ruleElt.getAttributeLong(MailService.A_APPT_RULE_COUNT_NUM, -1);
                            if (num > 0) {
                                recurBuf.append(";COUNT=").append(num);
                            } else {
                                throw MailServiceException.INVALID_REQUEST(
                                    "Expected positive num attribute in <recur> <rule> <count>", null);
                            }
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_INTERVAL)) {
                            String ival = ruleElt.getAttribute(MailService.A_APPT_RULE_INTERVAL_IVAL);
                            recurBuf.append(";INTERVAL=").append(ival);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYSECOND)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYSECOND_SECLIST);
                            recurBuf.append(";BYSECOND=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYMINUTE)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYMINUTE_MINLIST);
                            recurBuf.append(";BYMINUTE=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYHOUR)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYHOUR_HRLIST);
                            recurBuf.append(";BYHOUR=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYDAY)) {
                            recurBuf.append(";BYDAY=");
                            int pos = 0;
                            for (Iterator bydayIter = ruleElt.elementIterator(MailService.E_APPT_RULE_BYDAY_WKDAY);
                                 bydayIter.hasNext();
                                 pos++) {
                            	Element wkdayElt = (Element) bydayIter.next();
                                if (pos > 0)
                                    recurBuf.append(",");
                                String ordwk = wkdayElt.getAttribute(MailService.A_APPT_RULE_BYDAY_WKDAY_ORDWK, null);
                                if (ordwk != null)
                                    recurBuf.append(ordwk);
                                String day = wkdayElt.getAttribute(MailService.A_APPT_RULE_DAY);
                                if (day == null || day.length() == 0)
                                    throw MailServiceException.INVALID_REQUEST("Missing " +
                                                                               MailService.A_APPT_RULE_DAY + " in <" +
                                                                               ruleEltName + ">",
                                                                               null);
                                recurBuf.append(day);
                            }
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYMONTHDAY)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYMONTHDAY_MODAYLIST);
                            recurBuf.append(";BYMONTHDAY=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYYEARDAY)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYYEARDAY_YRDAYLIST);
                            recurBuf.append(";BYYEARDAY=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYWEEKNO)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYWEEKNO_WKLIST);
                            recurBuf.append(";BYWEEKNO=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYMONTH)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYMONTH_MOLIST);
                            recurBuf.append(";BYMONTH=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_BYSETPOS)) {
                            String list = ruleElt.getAttribute(MailService.A_APPT_RULE_BYSETPOS_POSLIST);
                            recurBuf.append(";BYSETPOS=").append(list);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_WKST)) {
                            String day = ruleElt.getAttribute(MailService.A_APPT_RULE_DAY);
                            recurBuf.append(";WKST=").append(day);
                        } else if (ruleEltName.equals(MailService.E_APPT_RULE_XNAME)) {
                            String name = ruleElt.getAttribute(MailService.A_APPT_RULE_XNAME_NAME, null);
                            if (name != null) {
                            	String value = ruleElt.getAttribute(MailService.A_APPT_RULE_XNAME_VALUE, "");
                                // TODO: Escape/unescape value according to "text" rule.
                                recurBuf.append(";").append(name).append("=").append(value);
                            }
                        }
                    }  // iterate inside <rule>

                    try {
                        Recur recur = new Recur(recurBuf.toString());
                        Property prop;
                        if (exclude) {
                            prop = new ExRule(recur);
                        } else {
                            prop = new RRule(recur);
                        }
                        comp.getProperties().add(prop);
                    } catch (ParseException ex) {
                        throw MailServiceException.INVALID_REQUEST("Exception parsing <recur> <rule>", ex);
                    }
                    
                } else {
                    throw MailServiceException.INVALID_REQUEST("Expected <date> or <rule> inside of "+e.getName()+", got "+
                            intElt.getName(), null);
                }
            }    // iterate inside <add> or <exclude>
        } // iter inside <recur>
    }
    
    
    /**
     * UID, DTSTAMP, and SEQUENCE **MUST** be set by caller
     * 
     * @param account user receiving invite
     * @param element invite XML element
     * @param invTzMap time zone map from invite;
     *                 if this method is called during modify operation,
     *                 this map contains time zones before the modification;
     *                 null if called during create operation
     * @param tzsReferenced list of time zones used in this invite; if this
     *                      method is called during modify operation, this
     *                      list contains time zones after the modification
     * @return
     * @throws ServiceException
     */
    private static Component parseInviteElementCommon(Account account,
                                                      Element element,
                                                      TimeZoneMap invTzMap,
                                                      List /*<ICalTimeZone>*/ tzsReferenced) 
    throws ServiceException {
        ICalTimeZone localTZ = account.getTimeZone();

        String type = element.getAttribute(MailService.A_APPT_TYPE);
        boolean allDay = element.getAttributeBool(MailService.A_APPT_ALLDAY, false);
        
        String name = element.getAttribute(MailService.A_NAME);
        String location = element.getAttribute(MailService.A_APPT_LOCATION,"");
        String descriptionStr = null;

        //
        // construct the iCal using the iCal4j stuff
        //
//        int flags = 0;
//        if (allDay) { flags |= InviteMessage.Invite.APPT_FLAG_ALLDAY; }
        
        VEvent event = new VEvent();

//        // RECUR
        Element recur = element.getOptionalElement(MailService.A_APPT_RECUR);
        if (recur != null) {
            parseRecur(recur, event, invTzMap, tzsReferenced, localTZ);
        }
        
        // ORGANIZER
        Element orgElt = element.getOptionalElement(MailService.E_APPT_ORGANIZER);
        if (orgElt == null) {
            throw MailServiceException.INVALID_REQUEST("Event must have an Organizer", null);
        } else {
            String cn = orgElt.getAttribute(MailService.A_DISPLAY, null);
            String address = orgElt.getAttribute(MailService.A_ADDRESS);
            URI uri = null;
            try { 
                uri = new URI("MAILTO:" + address);
            } catch (java.net.URISyntaxException e) {
                throw ServiceException.FAILURE("Building Organizer URI", e);
            }
            
            Organizer org = new Organizer(uri);
            event.getProperties().add(org);
            ParameterList params = org.getParameters();
            if (cn != null) {
                params.add(new Cn(cn));
            }
        }
        
        if (allDay) {
            XProperty msAllDay = new XProperty("X-MICROSOFT-CDO-ALLDAYEVENT", "TRUE");
            event.getProperties().add(msAllDay);
        }
        
        // SUMMARY (aka Name or Subject)
        event.getProperties().add(new Summary(name));
        
        // DESCRIPTION
        if (descriptionStr != null) {
            event.getProperties().add(new Description(descriptionStr));
        }
        
        // DTSTART

        {
            Element s = element.getElement(MailService.E_APPT_START_TIME);
            DtStart dtstart = new DtStart();
            String d = s.getAttribute(MailService.A_APPT_DATETIME);
            boolean isDateTime = true;
            if (d.indexOf('T') == -1) {
                if (!allDay) {
                    throw MailServiceException.INVALID_REQUEST("Request must have allDay=\"1\" if using a DATE start time instead of DATETIME", null);
                } else {
                    if (USE_BROKEN_OUTLOOK_MODE) {
                        d = d + "T000000";
                        String tz = localTZ.getID();
                        dtstart.getParameters().add(new TzId(tz));
                        lookupAndAddToTzList(tz, invTzMap, tzsReferenced);
                    } else {
                        dtstart.getParameters().add(Value.DATE);
                        isDateTime = false;
                    }
                }
            } else {
                if (allDay) {
                    throw MailServiceException.INVALID_REQUEST("AllDay event must have DATE, not DATETIME for start time", null);
                }
            }

            try {
                dtstart.setValue(d);
            } catch (ParseException ex) {
                throw MailServiceException.INVALID_REQUEST("Exception parsing DTSTART: " + d, ex);
            }
            if (isDateTime) {
                if (d.indexOf('Z') != -1) {
                    dtstart.setUtc(true);
                } else {
                    String tz = s.getAttribute(MailService.A_APPT_TIMEZONE, null);
                    if (tz != null) {
                        dtstart.getParameters().add(new TzId(tz));
                        lookupAndAddToTzList(tz, invTzMap, tzsReferenced);
                    }
                }
            }
            event.getProperties().add(dtstart);
        }

        // DTEND
        {
            Element e = element.getOptionalElement(MailService.E_APPT_END_TIME);
            if (e != null) {
                if (element.getOptionalElement(MailService.E_APPT_DURATION) != null) {
                    throw MailServiceException.INVALID_REQUEST("<inv> may have <e> end or <d> duration but not both", null);
                }

                DtEnd dtend = new DtEnd();
                String d = e.getAttribute(MailService.A_APPT_DATETIME);
                boolean isDateTime = true;
                if (d.indexOf('T') == -1) {
                    if (USE_BROKEN_OUTLOOK_MODE) {
                        d += "T000000";
                        try {
                            ParsedDateTime parsed = ParsedDateTime.parse(d, null, null, localTZ);
                            ParsedDuration oneDay = ParsedDuration.parse("P1D");
                            
                            parsed = parsed.add(oneDay);
                            
                            dtend.getParameters().add(new TzId(parsed.getTZName()));
                            lookupAndAddToTzList(parsed.getTZName(), invTzMap, tzsReferenced);
                            d = parsed.getDateTimePartString();
                        } catch (ParseException ex) {
                            throw ServiceException.FAILURE("Error parsing end time for outlook compatibility", ex);
                        }                        
                    } else {
                        // If DTSTART has DATE value type rather than DATE-TIME,
                        // DTEND must also be a DATE.  (RFC2445 Section 4.6.1)
                        dtend.getParameters().add(Value.DATE);
                        isDateTime = false;
                    }
                } else {
                    if (allDay) {
                        throw MailServiceException.INVALID_REQUEST("AllDay event must have DATE for end time, not DATETIME", null);
                    }
                }
                try {
                    dtend.setValue(d);
                } catch (ParseException ex) {
                    throw MailServiceException.INVALID_REQUEST("Exception parsing DTEND: " + d, ex);
                }
                if (isDateTime) {
                    if (d.indexOf('Z') != -1) {
                        dtend.setUtc(true);
                    } else {
                        String tz = e.getAttribute(MailService.A_APPT_TIMEZONE, null);
                        if (tz != null) {
                            dtend.getParameters().add(new TzId(tz));
                            lookupAndAddToTzList(tz, invTzMap, tzsReferenced);
                        }
                    }
                }
                event.getProperties().add(dtend);
            }
        }
        
        // DURATION
        {
            Element d = element.getOptionalElement(MailService.E_APPT_DURATION);
            if (d!= null) {
                ParsedDuration pd = ParsedDuration.parse(d);
                Duration dur = new Duration();
                dur.setValue(pd.toString());
                
                event.getProperties().add(dur);
            }
        }
        
        // LOCATION
        event.getProperties().add(new Location(location));

        // STATUS
        String status = element.getAttribute(MailService.A_APPT_STATUS, IcalXmlStrMap.STATUS_CONFIRMED);
        event.getProperties().add(new Status(IcalXmlStrMap.sStatusMap.toIcal(status)));

        // Microsoft Outlook compatibility for free-busy status
        String freeBusy = element.getAttribute(MailService.A_APPT_FREEBUSY, null);
        if (freeBusy != null) {
            String outlookFreeBusy = IcalXmlStrMap.sOutlookFreeBusyMap.toIcal(freeBusy);
            event.getProperties().add(new XProperty(Invite.MICROSOFT_BUSYSTATUS,
                                                    outlookFreeBusy));
            event.getProperties().add(new XProperty(Invite.MICROSOFT_INTENDEDSTATUS,
                                                    outlookFreeBusy));
        }
        
        // TRANSPARENCY
        String transp = element.getAttribute(MailService.A_APPT_TRANSPARENCY, IcalXmlStrMap.TRANSP_OPAQUE);
        event.getProperties().add(new Transp(IcalXmlStrMap.sTranspMap.toIcal(transp)));

        // ATTENDEEs
        for (Iterator iter = element.elementIterator(MailService.E_APPT_ATTENDEE); iter.hasNext(); ) {
            Element cur = (Element)(iter.next());

            String cn = cur.getAttribute(MailService.A_DISPLAY, null);
            String address = cur.getAttribute(MailService.A_ADDRESS);
            URI uri = null;
            try { 
                uri = new URI("MAILTO:" + address);
            } catch (java.net.URISyntaxException e) {
                throw ServiceException.FAILURE("Building Attendee URI", e);
            }

            Attendee at = new Attendee(uri);
            ParameterList params = at.getParameters();
            
            String role = cur.getAttribute(MailService.A_APPT_ROLE);
            role = IcalXmlStrMap.sRoleMap.toIcal(role);
//            if (role==null || role.equals("")) {
//                throw MailServiceException.INVALID_REQUEST("Role "+cur.getAttribute(MailService.A_APPT_ROLE)+" is unrecognized", null);
//            }
            params.add(new Role(role));
            
            
            String partStat = cur.getAttribute(MailService.A_APPT_PARTSTAT);
            partStat = IcalXmlStrMap.sPartStatMap.toIcal(partStat);
//            if (status == null || status.equals("")) {
//                throw MailServiceException.INVALID_REQUEST("Status "+cur.getAttribute(MailService.A_APPT_STATUS)+" is unrecognized", null);
//            }
            params.add(new PartStat(partStat)); 
            
            boolean rsvp = false;
            if (partStat.equals(PartStat.NEEDS_ACTION.getValue())) {
                rsvp = true;
            }
            params.add(rsvp ? Rsvp.TRUE : Rsvp.FALSE);
            
            // ical4j doesn't deal with "" empty values correctly right now
            if ((cn != null) && (!cn.equals(""))) {
                params.add(new Cn(cn));
            }
            
            event.getProperties().add(at);
        }
        return event;
    }
    
    static List /*VEvent*/ cancelAppointment(Account acct, Appointment appt, String comment) throws ServiceException 
    {
        List toRet = new ArrayList();

        // for each invite, get the recurrence and add an UNTIL
        for (int i = appt.numInvites()-1; i >= 0; i--) {

            try {
                Invite inv = appt.getInvite(i);
                try {
                    VEvent event = cancelInvite(acct, inv, comment);
                    toRet.add(event);
                } catch (ServiceException e) {
                    sLog.debug("Error creating cancellation for invite "+i+" for appt "+appt.getId());
                }
            } catch (ServiceException e) {
                sLog.debug("Error could not get invite "+i+" for appt "+appt.getId());
            }

            
        }
        
        return toRet;
    }
    
    static Calendar buildReplyCalendar(Account acct, Invite inv, SendInviteReply.ParsedVerb verb, String replySubject) throws ServiceException 
    {
        VEvent event = CalendarUtils.replyToInvite(acct, inv, verb, replySubject);
        
        Calendar iCal = makeCalendar(Method.REPLY);
        iCal.getComponents().add(event);
        
        try {
            iCal.validate(true);
        } catch (ValidationException e) { 
            sLog.debug("iCal Validation Exception generating Reply", e);
            throw ServiceException.FAILURE("Failure generating iCalendar reply", e);
        }
        
        return iCal;
    }
    
    /**
     * RFC2446 4.2.2: 
     * 
     *   BEGIN:VCALENDAR
     *   PRODID:-//ACME/DesktopCalendar//EN
     *   METHOD:REPLY
     *   VERSION:2.0
     *   BEGIN:VEVENT
     *   ATTENDEE;PARTSTAT=ACCEPTED:Mailto:B@example.com
     *   ORGANIZER:MAILTO:A@example.com
     *   UID:calsrv.example.com-873970198738777@example.com
     *   SEQUENCE:0
     *   REQUEST-STATUS:2.0;Success
     *   DTSTAMP:19970612T190000Z
     *   END:VEVENT
     *   END:VCALENDAR
     * 
     * @param acct
     * @param inv
     * @param verb
     * @param replySubject
     * @return
     * @throws ServiceException
     */
    static VEvent replyToInvite(Account acct, Invite inv, SendInviteReply.ParsedVerb verb, String replySubject)
    throws ServiceException
    {
        VEvent event = new VEvent();
        
        // ATTENDEE -- send back this attendee with the proper status
        Attendee meReply = null;
        Attendee me = inv.getMatchingAttendee(acct);
        if (me != null) {
            meReply = new Attendee(me.getCalAddress());
            meReply.getParameters().add(new PartStat(verb.getICalPartStat()));
            event.getProperties().add(meReply);
        } else {
            String name = acct.getName();
            try {
                meReply = new Attendee(name);
                meReply.getParameters().add(new PartStat(verb.getICalPartStat()));
                event.getProperties().add(meReply);
            } catch(URISyntaxException e) {
                throw ServiceException.FAILURE("URISyntaxException "+name, e);
            }
        }
        
        // DTSTART (outlook seems to require this, even though it shouldn't)
        DtStart dtstart = new DtStart(new net.fortuna.ical4j.model.DateTime(inv.getStartTime().getUtcTime()));
        dtstart.setUtc(true);
        event.getProperties().add(dtstart);
        
        // ORGANIZER
        event.getProperties().add(inv.getOrganizer());
        
        // UID
        event.getProperties().add(new Uid(inv.getUid()));

        // RECURRENCE-ID (if necessary)
        if (inv.hasRecurId()) {
            event.getProperties().add(inv.getRecurId().getRecurrenceId(acct));
        }
        
        // SEQUENCE        
        event.getProperties().add(new Sequence(inv.getSeqNo()));
        
        // DTSTAMP
        // we should pick "now" -- but the dtstamp MUST be >= the one sent by the organizer,
        // so we'll use theirs if it is after "now"...
        Date now = new Date();
        Date dtStampDate = new Date(inv.getDTStamp());
        if (now.after(dtStampDate)) {
            dtStampDate = now;
        }
        event.getProperties().add(new DtStamp(new net.fortuna.ical4j.model.DateTime(dtStampDate)));
        
        // SUMMARY
        event.getProperties().add(new Summary(replySubject));
        
        return event;
    }
    
    
    static Calendar buildCancelInviteCalendar(Account acct, Invite inv, String comment, Attendee forAttendee) throws ServiceException
    {
        Calendar iCal = makeCalendar(Method.CANCEL);
        iCal.getComponents().add(cancelInvite(acct, inv, comment, forAttendee, null));
        return iCal;
    }
    
    static Calendar buildCancelInviteCalendar(Account acct, Invite inv, String comment) throws ServiceException
    {
        Calendar iCal = makeCalendar(Method.CANCEL);
        iCal.getComponents().add(cancelInvite(acct, inv, comment, null, null));
        return iCal;
    }
    
    static Calendar buildCancelInstanceCalendar(Account acct, Invite inv, String comment, RecurId recurId) throws ServiceException
    {
        Calendar iCal = makeCalendar(Method.CANCEL);
        
        iCal.getComponents().add(cancelInvite(acct, inv, comment, null, recurId));
        return iCal;
    }
    
    
    /**
     * See RFC2446 4.2.9
     * 
     *    BEGIN:VCALENDAR
     *    PRODID:-//ACME/DesktopCalendar//EN
     *    METHOD:CANCEL
     *    VERSION:2.0
     *    BEGIN:VEVENT
     *    ORGANIZER:Mailto:A@example.com
     *    ATTENDEE;TYPE=INDIVIDUAL;Mailto:A@example.com
     *    ATTENDEE;TYPE=INDIVIDUAL:Mailto:B@example.com
     *    ATTENDEE;TYPE=INDIVIDUAL:Mailto:C@example.com
     *    ATTENDEE;TYPE=INDIVIDUAL:Mailto:D@example.com
     *    COMMENT:Mr. B cannot attend. It's raining. Lets cancel.
     *    UID:calsrv.example.com-873970198738777@example.com
     *    SEQUENCE:1
     *    STATUS:CANCELLED
     *    DTSTAMP:19970613T190000Z
     *    END:VEVENT
     *    END:VCALENDAR
     *
     * @param inv Invite to create CANCEL for
     * @param comment Text to be included in iCalendar message COMMENT field
     * @throws ServiceException
     */
    static VEvent cancelInvite(Account acct, Invite inv, String comment) throws ServiceException
    {
        return cancelInvite(acct, inv, comment, null, null);
    }
    
    
    /**
     * See 4.2.10
     * 
     * Cancel an Invite for an Attendee (or for ALL attendees if NULL is passed) 
     * 
     * @param inv Invite being replied to
     * @param comment Comment message to be included in the response
     * @param forAttendee The Attendee to be removed from the meeting, or NULL if all attendees (meeting is cancelled)
     * @param recurId - the particular instance we are supposed to be cancelling.  If this is nonzero, then inv must be the DEFAULT (recurId=0) invite!
     * @return
     * @throws ServiceException
     */
    static VEvent cancelInvite(Account acct, Invite inv, String comment, Attendee forAttendee, RecurId recurId) throws ServiceException 
    {
        // VEVENT!
        VEvent event = new VEvent();
        
        // ORGANIZER (FIXME: should check to make sure it is us!) 
        event.getProperties().add(inv.getOrganizer());
        
        // ATTENDEEs
        if (forAttendee == null) {
            for (Iterator iter = inv.getAttendees().iterator(); iter.hasNext();)
            {
                Attendee at = (Attendee)iter.next();
                event.getProperties().add(at);
            }
        } else {
            event.getProperties().add(forAttendee);
        }
        
        // COMMENT
        if (comment != null && !comment.equals("")) {
            event.getProperties().add(new Comment(comment));
        }
        
        // UID
        event.getProperties().add(new Uid(inv.getUid()));

        // RECURRENCE-ID
        if (inv.hasRecurId()) {
            assert(recurId == null); // can't cancel an instance of the non-default invite FIXME throw exception
            event.getProperties().add(inv.getRecurId().getRecurrenceId(acct));
        } else {
            if (recurId != null) {
                event.getProperties().add(recurId.getRecurrenceId(acct));
            }
        }

        // DTSTART (outlook seems to require this, even though it shouldn't)
        DtStart dtstart = new DtStart(new net.fortuna.ical4j.model.DateTime(inv.getStartTime().getUtcTime()));
        dtstart.setUtc(true);
        event.getProperties().add(dtstart);
        
        // SEQUENCE
        event.getProperties().add(new Sequence(inv.getSeqNo()+1));
        
        // STATUS
        event.getProperties().add(Status.VEVENT_CANCELLED);
        
        // DTSTAMP
        event.getProperties().add(new DtStamp(new net.fortuna.ical4j.model.DateTime()));
        
        // SUMMARY
        event.getProperties().add(new Summary("CANCELLED: "+inv.getName()));
        
        return event;
    }

    
    // ical4j helper
    public static String paramVal(Property prop, String paramName) {
        return CalendarUtils.paramVal(prop, paramName, "");
    }


    // ical4j helper
        public static String paramVal(Property prop, String paramName, String defaultValue) {
          ParameterList params = prop.getParameters();
          Parameter param = params.getParameter(paramName);
          if (param == null) {
              return defaultValue;
          }
          return param.getValue();
        }
    //    
    //    static boolean getValueAsBoolean(String value) {
    //        if (value.equalsIgnoreCase("true")) {
    //            return true;
    //        } else {
    //            return false;
    //        }
    //    }
    
}
