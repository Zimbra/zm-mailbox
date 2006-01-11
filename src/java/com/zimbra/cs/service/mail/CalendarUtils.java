/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.JMSession;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZRecur;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.soap.Element;

public class CalendarUtils {
    static MimeBodyPart makeICalIntoMimePart(String uid,
            ZCalendar.ZVCalendar iCal) throws ServiceException {
        try {
            MimeBodyPart mbp = new MimeBodyPart();

            String filename = "meeting.ics";
            mbp.setDataHandler(new DataHandler(new CalendarDataSource(iCal,
                    uid, filename)));

            return mbp;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Failure creating MimeBodyPart for InviteReply", e);
        }
    }

    /**
     * Builds the TO: list for appointment updates by iterating over the list of
     * ATTENDEEs
     * 
     * @param iter
     * @return
     */
    static List /* String */toListFromAts(List /* ZAttendee */list) {
        List /* String */toList = new ArrayList();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            ZAttendee curAt = (ZAttendee) iter.next();
            toList.add(curAt.getAddress());
        }
        return toList;
    }

    public static MimeMessage createDefaultCalendarMessage(String fromAddr,
            String addr, String subject, String text, String uid,
            ZCalendar.ZVCalendar cal) throws ServiceException {
        List list = new ArrayList();
        list.add(addr);
        return createDefaultCalendarMessage(fromAddr, list, subject, text, uid, cal);
    }

    public static MimeMessage createDefaultCalendarMessage(String fromAddr,
            List /* String */toAts, String subject, String text, String uid,
            ZCalendar.ZVCalendar cal) throws ServiceException {
        try {
            MimeMessage mm = new MimeMessage(JMSession.getSession()) {
                protected void updateHeaders() throws MessagingException {
                    String msgid = getMessageID();
                    super.updateHeaders();
                    if (msgid != null)
                        setHeader("Message-ID", msgid);
                }
            };
            MimeMultipart mmp = new MimeMultipart("alternative");
            mm.setContent(mmp);

            // ///////
            // TEXT part (add me first!)
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(text); // implicitly sets content-type to
                                    // "text/plain"
            mmp.addBodyPart(textPart);

            // ///////
            // CALENDAR part
            MimeBodyPart icalPart = CalendarUtils
                    .makeICalIntoMimePart(uid, cal);
            mmp.addBodyPart(icalPart);

            // ///////
            // MESSAGE HEADERS
            mm.setSubject(subject);

            Address[] addrs = new Address[toAts.size()];

            for (int i = toAts.size() - 1; i >= 0; i--) {
                String toAddr = (String) toAts.get(i);
                InternetAddress addr = new InternetAddress(toAddr);
                addrs[i] = addr;
            }
            mm.addRecipients(javax.mail.Message.RecipientType.TO, addrs);
            mm.setFrom(new InternetAddress(fromAddr));
            mm.setSentDate(new Date());
            mm.saveChanges();

            return mm;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(
                    "Messaging Exception while building InviteReply", e);
        }
    }

    /**
     * Useful for sync and import, parse an <inv> that is specified using raw
     * iCalendar data in the format: <inv> <content uid="UID" summary="summary">
     * RAW VCALENDAR </content> </inv>
     * 
     * @param account
     * @param inviteElem
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteRaw(Account account,
            Element inviteElem) throws ServiceException {
        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();

        Element content = inviteElem.getElement("content");
        toRet.mUid = content.getAttribute("uid");
        toRet.mSummary = content.getAttribute("summary");

        String calStr = content.getText();
        StringReader reader = new StringReader(calStr);
        toRet.mCal = ZCalendarBuilder.build(reader);

        List<Invite> invs = Invite.createFromCalendar(account, toRet.mSummary,
                toRet.mCal, false);

        toRet.mInvite = invs.get(0);

        return toRet;
    }

    public static final boolean RECUR_NOT_ALLOWED = false;

    public static final boolean RECUR_ALLOWED = true;

    /**
     * Parse an <inv> element
     * 
     * @param account
     * @param inviteElem
     * @param tzMap
     *            TimeZoneMap of invite we might want to refer to (eg we are an
     *            Exception to it)
     * @param uid
     * @param recurrenceIdAllowed
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteForCreate(
            Account account, Element inviteElem, TimeZoneMap tzMap, String uid,
            boolean recurrenceIdAllowed, boolean recurAllowed)
            throws ServiceException {
        if (tzMap == null) {
            tzMap = new TimeZoneMap(account.getTimeZone());
        }
        Invite create = new Invite(ICalTok.PUBLISH.toString(), tzMap);

        CalendarUtils.parseInviteElementCommon(account, inviteElem, create,
                recurAllowed);

        if (uid == null || uid.equals("")) {
            uid = inviteElem.getAttribute(MailService.A_UID, null);
            if (uid == null) {
                uid = LdapUtil.generateUUID();
            }
        }
        create.setUid(uid);

        if (recurrenceIdAllowed) {
            Element e = inviteElem.getElement("exceptId");
            ParsedDateTime dt = parseDateTime(e, tzMap, create);
            RecurId recurId = new RecurId(dt, RecurId.RANGE_NONE);
            create.setRecurId(recurId);
        } else {
            if (inviteElem.getOptionalElement("exceptId") != null) {
                throw MailServiceException.INVALID_REQUEST(
                        "May not specify an <exceptId> in this request", null);
            }
        }

        // DTSTAMP
        create.setDtStamp(new Date().getTime());

        // SEQUENCE
        create.setSeqNo(0);

        ZVCalendar iCal = create.newToICalendar();

        String summaryStr = create.getName() != null ? create.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = uid;
        toRet.mSummary = summaryStr;
        toRet.mInvite = create;

        return toRet;
    }

    /**
     * Parse an <inv> element in a Modify context -- existing UID, etc
     * 
     * @param inviteElem
     * @param oldInv
     *            is the Default Invite of the appointment we are modifying
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteForModify(
            Account account, Element inviteElem, Invite oldInv,
            List /* Attendee */attendeesToCancel, boolean recurAllowed)
            throws ServiceException {
        Invite mod = new Invite(ICalTok.PUBLISH.toString(), oldInv
                .getTimeZoneMap());

        CalendarUtils.parseInviteElementCommon(account, inviteElem, mod, recurAllowed);

        // use UID from old inv
        String uid = oldInv.getUid();
        mod.setUid(oldInv.getUid());

        // DTSTAMP
        mod.setDtStamp(new Date().getTime());

        // SEQUENCE
        mod.setSeqNo(oldInv.getSeqNo() + 1);

        if (oldInv.hasRecurId()) {
            mod.setRecurId(oldInv.getRecurId());
        }

        // compare the new attendee list with the existing one...if attendees
        // have been removed, then
        // we need to send them individual cancelation messages
        List /* ZAttendee */newAts = mod.getAttendees();
        List /* ZAttendee */oldAts = oldInv.getAttendees();
        for (Iterator iter = oldAts.iterator(); iter.hasNext();) {
            ZAttendee cur = (ZAttendee) iter.next();
            if (!attendeeListContains(newAts, cur)) {
                attendeesToCancel.add(cur);
            }
        }

        ZVCalendar iCal = mod.newToICalendar();

        String summaryStr = "";
        if (mod.getName() != null) {
            summaryStr = mod.getName();
        }

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = uid;
        toRet.mSummary = summaryStr;
        toRet.mInvite = mod;

        return toRet;
    }

    // TRUE if the list contains the atendee, comparing by URI
    private static boolean attendeeListContains(List /* ZAttendee */list,
            ZAttendee at) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            ZAttendee cur = (ZAttendee) iter.next();
            if (cur.addressesMatch(at)) {
                return true;
            }
        }
        return false;
    }

    static RecurId parseRecurId(Element e, TimeZoneMap invTzMap, Invite inv)
            throws ServiceException {
        String range = e.getAttribute("range", "");

        ParsedDateTime dt = parseDateTime(e, invTzMap, inv);
        return new RecurId(dt, range);
    }

    /**
     * Parse a date from the enclosed element. If the element has a TimeZone
     * reference, then
     * 
     * @param e
     * @param referencedTimeZones
     * @return obj[0] is a Date, obj[1] is a TimeZone
     * @throws ServiceException
     */
    static ParsedDateTime parseDateTime(Element e, TimeZoneMap invTzMap,
            Invite inv) throws ServiceException {
        String d = e.getAttribute(MailService.A_APPT_DATETIME, null);
        String tz = e.getAttribute(MailService.A_APPT_TIMEZONE, null);
        return parseDateTime(e.getName(), d, tz, invTzMap, inv);
    }

    private static ParsedDateTime parseDateTime(String eltName, String d,
            String tzName, TimeZoneMap invTzMap, Invite inv)
            throws ServiceException {
        try {
            ICalTimeZone zone = null;
            if (tzName != null) {
                zone = lookupAndAddToTzList(tzName, invTzMap, inv);
            }
            return ParsedDateTime.parse(d, zone, inv.getTimeZoneMap()
                    .getLocalTimeZone());
        } catch (ParseException ex) {
            throw MailServiceException.INVALID_REQUEST("could not parse time "
                    + d + " in element " + eltName, ex);
        }
    }

    private static ICalTimeZone lookupAndAddToTzList(String tzId,
            TimeZoneMap invTzMap, Invite inv) throws ServiceException {
        // Workaround for bug in Outlook, which double-quotes TZID parameter
        // value in properties like DTSTART, DTEND, etc. Use unquoted tzId.
        int len = tzId.length();
        if (len >= 2 && tzId.charAt(0) == '"' && tzId.charAt(len - 1) == '"') {
            tzId = tzId.substring(1, len - 1);
        }

        ICalTimeZone zone = null;

        if (tzId.equals("")) {
            return null;
        }

        WellKnownTimeZone knownTZ = Provisioning.getInstance().getTimeZoneById(
                tzId);
        if (knownTZ != null) {
            zone = knownTZ.toTimeZone();
        }

        if (zone == null) {
            // Could be a custom TZID during modify operation of invite from
            // external calendar system. Look up the TZID from the invite.
            if (invTzMap != null) {
                zone = invTzMap.getTimeZone(tzId);
            }

            if (zone == null) {
                throw MailServiceException.INVALID_REQUEST(
                        "invalid time zone \"" + tzId + "\"", null);
            }
        }
        if (!inv.getTimeZoneMap().contains(zone))
            inv.getTimeZoneMap().add(zone);
        return zone;
    }

private static Recurrence.IRecurrence parseRecur(Element recurElt, TimeZoneMap invTzMap, Invite inv) 
    throws ServiceException {
        
        ParsedDuration dur = inv.getDuration();
        if (dur == null) {
            dur = inv.getEndTime().difference(inv.getStartTime());
        }
        
        ArrayList addRules = new ArrayList();
        ArrayList subRules = new ArrayList();
        
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
                    
                    ParsedDateTime dt = parseDateTime(intElt, invTzMap, inv);
                    
                    try {
                        String dstr = intElt.getAttribute(MailService.A_APPT_DATETIME);
                        
                        // FIXME!! Need an IRecurrence imp that takes a
                        // DateList, then instantiate it here and
                        // add it to addRules or subRules!!!
                        
// DateList dl;
// boolean isDateTime = true;
// try {
// dl = new DateList(dstr, Value.DATE_TIME);
// } catch (ParseException ex) {
// dl = new DateList(dstr, Value.DATE);
// isDateTime = false;
// }
//
// // if (cal.getTimeZone() == ICalTimeZone.getUTC()) {
// // dl.setUtc(true);
// // }
//                        
// Property prop;
// if (exclude) {
// prop = new ExDate(dl);
// } else {
// prop = new RDate(dl);
// }
//                        
// if (isDateTime) {
// if (dt.getTimeZone() != ICalTimeZone.getUTC()) {
// prop.getParameters().add(new TzId(dt.getTZName()));
// }
// }
                        
                        if (exclude) {
                            // FIXME fix EXDATE
// subRules.add(new Recurrence.SingleInstanceRule())
                        } else {
                            // FIXME fix RDATE
// addRules.add(new Recurrence.SingleInstanceRule());
                        }
                        
                    } catch (Exception ex) {
                        throw MailServiceException.INVALID_REQUEST("Exception parsing <recur><date> d="+
                                intElt.getAttribute(MailService.A_APPT_DATETIME), ex);
                    }
                } else if (intElt.getName().equals(MailService.E_APPT_RULE)) {
                    // handle RRULE or EXRULE

                    // Turn XML into iCal RECUR string, which will then be
                    // parsed by ical4j Recur object.

                    StringBuilder recurBuf = new StringBuilder(100);

                    String freq = IcalXmlStrMap.sFreqMap.toIcal(
                    		          intElt.getAttribute(MailService.A_APPT_RULE_FREQ));
                    recurBuf.append("FREQ=").append(freq);

                    for (Iterator ruleIter = intElt.elementIterator(); ruleIter.hasNext(); ) {
                    	Element ruleElt = (Element) ruleIter.next();
                        String ruleEltName = ruleElt.getName();
                        if (ruleEltName.equals(MailService.E_APPT_RULE_UNTIL)) {
                            recurBuf.append(";UNTIL=");
                            String d = ruleElt.getAttribute(MailService.A_APPT_DATETIME);
                            recurBuf.append(d);
                            
                            // If UNTIL has time part it must be specified
                            // as UTC time, i.e. ending in "Z".
                            // (RFC2445 Section 4.3.10 Recurrence Rule)
                            if (d.indexOf("T") >= 0)
                                if (d.indexOf("Z") <0)
                                    recurBuf.append('Z');

                            
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
                                // TODO: Escape/unescape value according to
                                // "text" rule.
                                recurBuf.append(";").append(name).append("=").append(value);
                            }
                        }
                    }  // iterate inside <rule>

                    try { 
                        ZRecur recur = new ZRecur(recurBuf.toString(), invTzMap);
                        if (exclude) {
                            subRules.add(new Recurrence.SimpleRepeatingRule(inv.getStartTime(), dur, recur, null));
                        } else {
                            addRules.add(new Recurrence.SimpleRepeatingRule(inv.getStartTime(), dur, recur, null));
                        }
                    } catch (ServiceException ex) {
                        throw MailServiceException.INVALID_REQUEST("Exception parsing <recur> <rule>", ex);
                    }
                    
                } else {
                    throw MailServiceException.INVALID_REQUEST("Expected <date> or <rule> inside of "+e.getName()+", got "+
                            intElt.getName(), null);
                }
            }    // iterate inside <add> or <exclude>
        } // iter inside <recur>
        
        if (inv.getRecurId() != null) {
            return new Recurrence.ExceptionRule(inv.getRecurId(), inv.getStartTime(), dur, null, addRules, subRules);
        } else {
            return new Recurrence.RecurrenceRule(inv.getStartTime(), dur, null, addRules, subRules);
        }
    }

	static ParsedDateTime parseDtElement(Element e, TimeZoneMap tzMap,
            Invite inv) throws ServiceException {
        String d = e.getAttribute(MailService.A_APPT_DATETIME);
        String tzId = e.getAttribute(MailService.A_APPT_TIMEZONE, null);
        ICalTimeZone timezone = null;
        if (tzId != null) {
            timezone = lookupAndAddToTzList(tzId, tzMap, inv);
        }

        try {
            return ParsedDateTime.parse(d, timezone, inv.getTimeZoneMap()
                    .getLocalTimeZone());
        } catch (ParseException pe) {
            throw ServiceException.FAILURE("Caught ParseException: " + pe, pe);
        }
    }

    private static void parseTimeZones(Element parent, TimeZoneMap tzMap)
            throws ServiceException {
        assert (tzMap != null);
        for (Iterator iter = parent.elementIterator(MailService.E_APPT_TZ); iter
                .hasNext();) {
            Element tzElem = (Element) iter.next();
            String tzid = tzElem.getAttribute(MailService.A_ID);
            int standardOffset = (int) tzElem
                    .getAttributeLong(MailService.A_APPT_TZ_STDOFFSET);
            int daylightOffset = (int) tzElem.getAttributeLong(
                    MailService.A_APPT_TZ_DAYOFFSET, standardOffset);
            // minutes to milliseconds
            standardOffset *= 60 * 1000;
            daylightOffset *= 60 * 1000;

            SimpleOnset standardOnset = null;
            SimpleOnset daylightOnset = null;
            if (daylightOffset != standardOffset) {
                Element standard = tzElem
                        .getOptionalElement(MailService.E_APPT_TZ_STANDARD);
                Element daylight = tzElem
                        .getOptionalElement(MailService.E_APPT_TZ_DAYLIGHT);
                if (standard == null || daylight == null)
                    throw MailServiceException
                            .INVALID_REQUEST(
                                    "DST time zone missing standard and/or daylight onset",
                                    null);
                standardOnset = parseSimpleOnset(standard);
                daylightOnset = parseSimpleOnset(daylight);
            }

            ICalTimeZone tz = new ICalTimeZone(tzid, standardOffset,
                    standardOnset, daylightOffset, daylightOnset);
            tzMap.add(tz);
        }
    }

    private static SimpleOnset parseSimpleOnset(Element element)
            throws ServiceException {
        int week = (int) element.getAttributeLong(MailService.A_APPT_TZ_WEEK, 0);
        int wkday = (int) element
                .getAttributeLong(MailService.A_APPT_TZ_DAYOFWEEK, 0);
        int month = (int) element.getAttributeLong(MailService.A_APPT_TZ_MONTH);
        int mday = (int) element.getAttributeLong(MailService.A_APPT_TZ_DAYOFMONTH, 0);
        int hour = (int) element.getAttributeLong(MailService.A_APPT_TZ_HOUR);
        int minute = (int) element
                .getAttributeLong(MailService.A_APPT_TZ_MINUTE);
        int second = (int) element
                .getAttributeLong(MailService.A_APPT_TZ_SECOND);
        return new SimpleOnset(week, wkday, month, mday, hour, minute, second);
    }

    /**
     * UID, DTSTAMP, and SEQUENCE **MUST** be set by caller
     * 
     * @param account
     *            user receiving invite
     * @param element
     *            invite XML element
     * @param newInv
     *            Invite we are currently building up
     * @param oldTzMap
     *            time zone map from A DIFFERENT invite; if this method is
     *            called during modify operation, this map contains time zones
     *            before the modification; null if called during create
     *            operation
     * @return
     * @throws ServiceException
     */
    private static void parseInviteElementCommon(Account account,
            Element element, Invite newInv,
            boolean recurAllowed) throws ServiceException {
    	TimeZoneMap tzMap = newInv.getTimeZoneMap();
        parseTimeZones(element.getParent(), tzMap);

        boolean allDay = element.getAttributeBool(MailService.A_APPT_ALLDAY,
                false);
        newInv.setIsAllDayEvent(allDay);

        String name = element.getAttribute(MailService.A_NAME, "");
        String location = element.getAttribute(MailService.A_APPT_LOCATION, "");

        // ORGANIZER
        Element orgElt = element
                .getOptionalElement(MailService.E_APPT_ORGANIZER);
        if (orgElt == null) {
            throw MailServiceException.INVALID_REQUEST(
                    "Event must have an Organizer", null);
        } else {
            String cn = orgElt.getAttribute(MailService.A_DISPLAY, null);
            String address = orgElt.getAttribute(MailService.A_ADDRESS);

            ZOrganizer org = new ZOrganizer(cn, address);
            newInv.setOrganizer(org);
        }

        // SUMMARY (aka Name or Subject)
        newInv.setName(name);

        // DTSTART
        {
            Element s = element.getElement(MailService.E_APPT_START_TIME);
            ParsedDateTime dt = parseDtElement(s, tzMap, newInv);
            if (dt.hasTime()) {
                if (allDay) {
                    throw MailServiceException
                            .INVALID_REQUEST(
                                    "AllDay event must have DATE, not DATETIME for start time",
                                    null);
                }
            } else {
                if (!allDay) {
                    throw MailServiceException
                            .INVALID_REQUEST(
                                    "Request must have allDay=\"1\" if using a DATE start time instead of DATETIME",
                                    null);
                }
            }
            newInv.setDtStart(dt);
        }

        // DTEND
        {
            Element e = element.getOptionalElement(MailService.E_APPT_END_TIME);
            if (e != null) {
                if (element.getOptionalElement(MailService.E_APPT_DURATION) != null) {
                    throw MailServiceException
                            .INVALID_REQUEST(
                                    "<inv> may have <e> end or <d> duration but not both",
                                    null);
                }
                ParsedDateTime dt = parseDtElement(e, tzMap, newInv);

                if (allDay) {
                    // HACK ALERT: okay, campers, here's the deal.
                    // By definition, our end dates are EXCLUSIVE: DTEND is not
                    // included.. eg a meeting 7-8pm actually stops at 7:59
                    //
                    // This makes sense for normal appointments, but apparently
                    // this rule is confusing to people when making
                    // all-day-events
                    //
                    // For all-day-events, people want to say that a 1-day-long
                    // appointment starts on 11/1 and ends on 11/1, for example
                    // this is inconsistent (and incompatible with RFC2445) but
                    // it is what people want. Sooo, we to a bit of a hacky
                    // translation when sending/receiving all-day-events.
                    //     
                    dt = dt.add(ParsedDuration.ONE_DAY);
                }
                if (dt.hasTime()) {
                    if (allDay) {
                        throw MailServiceException
                                .INVALID_REQUEST(
                                        "AllDay event must have DATE, not DATETIME for start time",
                                        null);
                    }
                } else {
                    if (!allDay) {
                        throw MailServiceException
                                .INVALID_REQUEST(
                                        "Request must have allDay=\"1\" if using a DATE start time instead of DATETIME",
                                        null);
                    }
                }
                newInv.setDtEnd(dt);
            }
        }

        // DURATION
        {
            Element d = element.getOptionalElement(MailService.E_APPT_DURATION);
            if (d != null) {
                ParsedDuration pd = ParsedDuration.parse(d);
                newInv.setDuration(pd);
            }
        }

        // LOCATION
        newInv.setLocation(location);

        // FreeBusy
        String fb = element.getAttribute(MailService.A_APPT_FREEBUSY,
                IcalXmlStrMap.FBTYPE_BUSY);
        newInv.setFreeBusy(fb);

        // STATUS
        String status = element.getAttribute(MailService.A_APPT_STATUS,
                IcalXmlStrMap.STATUS_CONFIRMED);
        validateAttr(IcalXmlStrMap.sStatusMap, MailService.A_APPT_STATUS,
                status);
        newInv.setStatus(status);

        // TRANSPARENCY
        String transp = element.getAttribute(MailService.A_APPT_TRANSPARENCY,
                IcalXmlStrMap.TRANSP_OPAQUE);
        validateAttr(IcalXmlStrMap.sTranspMap, MailService.A_APPT_TRANSPARENCY,
                transp);
        newInv.setTransparency(transp);

        // ATTENDEEs
        for (Iterator iter = element
                .elementIterator(MailService.E_APPT_ATTENDEE); iter.hasNext();) {
            Element cur = (Element) (iter.next());

            String cn = cur.getAttribute(MailService.A_DISPLAY, null);
            String address = cur.getAttribute(MailService.A_ADDRESS);

            String role = cur.getAttribute(MailService.A_APPT_ROLE);
            validateAttr(IcalXmlStrMap.sRoleMap, MailService.A_APPT_ROLE, role);

            String partStat = cur.getAttribute(MailService.A_APPT_PARTSTAT);
            validateAttr(IcalXmlStrMap.sPartStatMap,
                    MailService.A_APPT_PARTSTAT, partStat);

            boolean rsvp = cur.getAttributeBool(MailService.A_APPT_RSVP, false);

            if (partStat.equals(IcalXmlStrMap.PARTSTAT_NEEDS_ACTION)) {
                rsvp = true;
            }

            ZAttendee at = new ZAttendee(address, cn, role, partStat,
                    rsvp ? Boolean.TRUE : Boolean.FALSE);

            if (newInv.getMethod().equals(ICalTok.PUBLISH.toString())) {
                newInv.setMethod(ICalTok.REQUEST.toString());
            }
            newInv.addAttendee(at);
        }

        // RECUR
        Element recur = element.getOptionalElement(MailService.A_APPT_RECUR);
        if (recur != null) {
            if (!recurAllowed) {
                throw ServiceException.FAILURE(
                        "No <recur> allowed in an exception", null);
            }
            Recurrence.IRecurrence recurrence = parseRecur(recur, tzMap,
                    newInv);
            newInv.setRecurrence(recurrence);
        }
    }

    private static void validateAttr(IcalXmlStrMap map, String attrName,
            String value) throws ServiceException {
        if (!map.validXml(value)) {
            throw MailServiceException.INVALID_REQUEST("Invalid value '"
                    + value + "' specified for attribute:" + attrName, null);
        }

    }

    // static List cancelAppointment(Account acct, Appointment appt, String
    // comment) {
    // List<ZComponent> toRet = new ArrayList<ZComponent>();
    //
    // // for each invite, get the recurrence and add an UNTIL
    // for (int i = appt.numInvites()-1; i >= 0; i--) {
    // Invite inv = appt.getInvite(i);
    // try {
    // ZComponent event = cancelInvite(acct, inv, comment, null,
    // null).newToVEvent();
    // toRet.add(event);
    // } catch (ServiceException e) {
    // sLog.debug("Error creating cancellation for invite "+i+" for appt
    // "+appt.getId());
    // }
    // }
    //        
    // return toRet;
    // }
    //    
    /**
     * RFC2446 4.2.2:
     * 
     * BEGIN:VCALENDAR PRODID:-//ACME/DesktopCalendar//EN METHOD:REPLY
     * VERSION:2.0 BEGIN:VEVENT ATTENDEE;PARTSTAT=ACCEPTED:Mailto:B@example.com
     * ORGANIZER:MAILTO:A@example.com
     * UID:calsrv.example.com-873970198738777@example.com SEQUENCE:0
     * REQUEST-STATUS:2.0;Success DTSTAMP:19970612T190000Z END:VEVENT
     * END:VCALENDAR
     * 
     * @param acct
     * @param oldInv
     * @param verb
     * @param replySubject
     * @return
     * @throws ServiceException
     */
    public static Invite replyToInvite(Account acct, Invite oldInv,
            SendInviteReply.ParsedVerb verb, String replySubject,
            ParsedDateTime exceptDt) throws ServiceException {
        Invite reply = new Invite(ICalTok.REPLY.toString(), new TimeZoneMap(
                acct.getTimeZone()));

        reply.getTimeZoneMap().add(oldInv.getTimeZoneMap());

        // ATTENDEE -- send back this attendee with the proper status
        ZAttendee meReply = null;
        ZAttendee me = oldInv.getMatchingAttendee(acct);
        if (me != null) {
            meReply = new ZAttendee(me.getAddress());
            meReply.setPartStat(verb.getXmlPartStat());
            meReply.setRole(me.getRole());
            meReply.setCn(me.getCn());
            reply.addAttendee(meReply);
        } else {
            String name = acct.getName();
            meReply = new ZAttendee(name);
            meReply.setPartStat(verb.getXmlPartStat());
            reply.addAttendee(meReply);
        }

        // DTSTART (outlook seems to require this, even though it shouldn't)
        reply.setDtStart(oldInv.getStartTime());

        // ORGANIZER
        reply.setOrganizer(oldInv.getOrganizer());

        // UID
        reply.setUid(oldInv.getUid());

        // RECURRENCE-ID (if necessary)
        if (exceptDt != null) {
            reply.setRecurId(new RecurId(exceptDt, RecurId.RANGE_NONE));
        } else if (oldInv.hasRecurId()) {
            reply.setRecurId(oldInv.getRecurId());
        }

        // SEQUENCE
        reply.setSeqNo(oldInv.getSeqNo());

        // DTSTAMP
        // we should pick "now" -- but the dtstamp MUST be >= the one sent by
        // the organizer,
        // so we'll use theirs if it is after "now"...
        Date now = new Date();
        Date dtStampDate = new Date(oldInv.getDTStamp());
        if (now.after(dtStampDate)) {
            dtStampDate = now;
        }
        reply.setDtStamp(dtStampDate.getTime());

        // SUMMARY
        reply.setName(replySubject);

        // System.out.println("REPLY: "+reply.toVEvent().toString());

        return reply;
    }

    static Invite buildCancelInviteCalendar(Account acct, Invite inv,
            String comment, ZAttendee forAttendee) throws ServiceException {
        return cancelInvite(acct, inv, comment, forAttendee, null);
    }

    static Invite buildCancelInviteCalendar(Account acct, Invite inv,
            String comment) throws ServiceException {
        return cancelInvite(acct, inv, comment, null, null);
    }

    static Invite buildCancelInstanceCalendar(Account acct, Invite inv,
            String comment, RecurId recurId) throws ServiceException {
        return cancelInvite(acct, inv, comment, null, recurId);
    }

    /**
     * See 4.2.10
     * 
     * Cancel an Invite for an Attendee (or for ALL attendees if NULL is passed)
     * 
     * See RFC2446 4.2.9
     * 
     * BEGIN:VCALENDAR PRODID:-//ACME/DesktopCalendar//EN METHOD:CANCEL
     * VERSION:2.0 BEGIN:VEVENT ORGANIZER:Mailto:A@example.com
     * ATTENDEE;TYPE=INDIVIDUAL;Mailto:A@example.com
     * ATTENDEE;TYPE=INDIVIDUAL:Mailto:B@example.com
     * ATTENDEE;TYPE=INDIVIDUAL:Mailto:C@example.com
     * ATTENDEE;TYPE=INDIVIDUAL:Mailto:D@example.com COMMENT:Mr. B cannot
     * attend. It's raining. Lets cancel.
     * UID:calsrv.example.com-873970198738777@example.com SEQUENCE:1
     * STATUS:CANCELLED DTSTAMP:19970613T190000Z END:VEVENT END:VCALENDAR
     * 
     * 
     * @param inv
     *            Invite being replied to
     * @param comment
     *            Comment message to be included in the response
     * @param forAttendee
     *            The Attendee to be removed from the meeting, or NULL if all
     *            attendees (meeting is cancelled)
     * @param recurId -
     *            the particular instance we are supposed to be cancelling. If
     *            this is nonzero, then inv must be the DEFAULT (recurId=0)
     *            invite!
     * @return
     * @throws ServiceException
     */
    static Invite cancelInvite(Account acct, Invite inv, String comment,
            ZAttendee forAttendee, RecurId recurId) throws ServiceException {
        // TimeZoneMap tzMap = new TimeZoneMap(acct.getTimeZone());
        Invite cancel = new Invite(ICalTok.CANCEL.toString(), comment, inv
                .getTimeZoneMap());

        // ORGANIZER (FIXME: should check to make sure it is us!)
        cancel.setOrganizer(inv.getOrganizer());

        // ATTENDEEs
        if (forAttendee == null) {
            for (Iterator iter = inv.getAttendees().iterator(); iter.hasNext();) {
                ZAttendee at = (ZAttendee) iter.next();
                cancel.addAttendee(at);
            }
        } else {
            cancel.addAttendee(forAttendee);
        }

        // COMMENT
        if (comment != null && !comment.equals("")) {
            cancel.setComment(comment);
        }

        // UID
        cancel.setUid(inv.getUid());

        // RECURRENCE-ID
        if (inv.hasRecurId()) {
            // FIXME: if RECURRENCE-ID can be a range (THISANDFUTURE) then we'll
            // need to be smarter here
            cancel.setRecurId(inv.getRecurId());
        } else {
            if (recurId != null) {
                cancel.setRecurId(recurId);
            }
        }

        // DTSTART (outlook seems to require this, even though it shouldn't)
        cancel.setDtStart(inv.getStartTime());

        // SEQUENCE
        cancel.setSeqNo(inv.getSeqNo() + 1);

        // STATUS
        cancel.setStatus(IcalXmlStrMap.STATUS_CANCELLED);

        // DTSTAMP
        cancel.setDtStamp(new Date().getTime());

        // SUMMARY
        cancel.setName("CANCELLED: " + inv.getName());

        return cancel;
    }

    //    
    // // ical4j helper
    // public static String paramVal(Property prop, String paramName) {
    // return CalendarUtils.paramVal(prop, paramName, "");
    // }
    //
    //
    // // ical4j helper
    // public static String paramVal(Property prop, String paramName, String
    // defaultValue) {
    // ParameterList params = prop.getParameters();
    // Parameter param = params.getParameter(paramName);
    // if (param == null) {
    // return defaultValue;
    // }
    // return param.getValue();
    // }
}
