/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.calendar.Alarm;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.Period;
import com.zimbra.cs.mailbox.calendar.RdateExdate;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.WellKnownTimeZones;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZRecur;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.L10nUtil.MsgKey;

import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class CalendarUtils {
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
            Account account, byte itemType, Element inviteElem, TimeZoneMap tzMap, String uid,
            boolean recurrenceIdAllowed, boolean recurAllowed)
            throws ServiceException {
        if (tzMap == null) {
            tzMap = new TimeZoneMap(ICalTimeZone.getAccountTimeZone(account));
        }
        Invite create = new Invite(ICalTok.PUBLISH.toString(), tzMap, false);

        CalendarUtils.parseInviteElementCommon(
                account, itemType, inviteElem, create, recurrenceIdAllowed, recurAllowed);

        // DTSTAMP
        if (create.getDTStamp() == 0) { //zdsync
        	create.setDtStamp(new Date().getTime());
        }

        // UID
        if (uid != null && uid.length() > 0)
            create.setUid(uid);

        ZVCalendar iCal = create.newToICalendar(true);

        String summaryStr = create.getName() != null ? create.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = create.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = create;

        return toRet;
    }

    static ParseMimeMessage.InviteParserResult parseInviteForCreateException(
            Account account, byte itemType, Element inviteElem, TimeZoneMap tzMap, String uid,
            Invite defaultInv)
    throws ServiceException {
        if (tzMap == null) {
            tzMap = new TimeZoneMap(ICalTimeZone.getAccountTimeZone(account));
        }
        Invite create = new Invite(ICalTok.PUBLISH.toString(), tzMap, false);

        CalendarUtils.parseInviteElementCommon(
                account, itemType, inviteElem, create, true, false);

        // DTSTAMP
        if (create.getDTStamp() == 0) { //zdsync
            create.setDtStamp(new Date().getTime());
        }

        // UID
        if (uid != null && uid.length() > 0)
            create.setUid(uid);

        // Don't allow changing organizer in an exception instance.
        create.setOrganizer(defaultInv.hasOrganizer()
                            ? new ZOrganizer(defaultInv.getOrganizer()) : null);
        create.setIsOrganizer(account);

        ZVCalendar iCal = create.newToICalendar(true);

        String summaryStr = create.getName() != null ? create.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = create.getUid();
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
            Account account, byte itemType, Element inviteElem, Invite oldInv,
            List<ZAttendee> attendeesToCancel, boolean recurAllowed)
            throws ServiceException {
        Invite mod = new Invite(ICalTok.PUBLISH.toString(), oldInv.getTimeZoneMap(), false);

        CalendarUtils.parseInviteElementCommon(
                account, itemType, inviteElem, mod, oldInv.hasRecurId(), recurAllowed);

        // DTSTAMP
        mod.setDtStamp(new Date().getTime());

        // UID
        mod.setUid(oldInv.getUid());

        // SEQUENCE
        mod.setSeqNo(oldInv.getSeqNo() + 1);

        if (oldInv.hasRecurId()) {
            mod.setRecurId(oldInv.getRecurId());
        }

        attendeesToCancel.addAll(getRemovedAttendees(oldInv, mod));

        ZVCalendar iCal = mod.newToICalendar(true);

        String summaryStr = "";
        if (mod.getName() != null) {
            summaryStr = mod.getName();
        }

        // HACK: Workaround for bug 8854/21749.  If <alarm> elements are missing, inherit
        // old alarms.  If any was given, even an empty one, clear all alarms.
        Element element = inviteElem;
        Element compElem = element.getOptionalElement(MailConstants.E_INVITE_COMPONENT);
        if (compElem != null)
            element = compElem;
        Iterator<Element> alarmsIter = element.elementIterator(MailConstants.E_CAL_ALARM);
        boolean hasAlarmElems = false;
        while (alarmsIter.hasNext()) {
            hasAlarmElems = true;
            break;
        }
        if (!hasAlarmElems) {
            for (Iterator<Alarm> alarmIter = oldInv.alarmsIterator(); alarmIter.hasNext(); ) {
                Alarm alarm = alarmIter.next();
                mod.addAlarm(alarm);
            }
        }

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = mod.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = mod;

        return toRet;
    }

    static ParseMimeMessage.InviteParserResult parseInviteForCancel(
            Account account, byte itemType, Element inviteElem, TimeZoneMap tzMap,
            boolean recurrenceIdAllowed, boolean recurAllowed)
            throws ServiceException {
        if (tzMap == null) {
            tzMap = new TimeZoneMap(ICalTimeZone.getAccountTimeZone(account));
        }
        Invite cancel = new Invite(ICalTok.CANCEL.toString(), tzMap, false);

        CalendarUtils.parseInviteElementCommon(
                account, itemType, inviteElem, cancel, recurrenceIdAllowed, recurAllowed);

        String uid = cancel.getUid();
        if (uid == null || uid.length() == 0)
            throw ServiceException.INVALID_REQUEST("Missing uid in a cancel invite", null);

        Invite sanitized =
            cancelInvite(account, null, false, cancel, cancel.getComment(),
                         cancel.getAttendees(), cancel.getRecurId(),
                         false);
        
        sanitized.setInviteId(cancel.getMailItemId()); //zdsync
        sanitized.setDtStamp(cancel.getDTStamp()); //zdsync

        ZVCalendar iCal = sanitized.newToICalendar(true);

        String summaryStr = sanitized.getName() != null ? sanitized.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = sanitized.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = sanitized;

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

    public static List<ZAttendee> getRemovedAttendees(Invite oldInv, Invite newInv) {
        List<ZAttendee> list = new ArrayList<ZAttendee>();
        // compare the new attendee list with the existing one...if attendees
        // have been removed, then
        // we need to send them individual cancelation messages
        List<ZAttendee> newAts = newInv.getAttendees();
        List<ZAttendee> oldAts = oldInv.getAttendees();
        for (Iterator iter = oldAts.iterator(); iter.hasNext();) {
            ZAttendee cur = (ZAttendee) iter.next();
            if (!attendeeListContains(newAts, cur)) {
                list.add(cur);
            }
        }
        return list;
    }

    static RecurId parseRecurId(Element e, TimeZoneMap invTzMap, Invite inv)
            throws ServiceException {
        String range = e.getAttribute(MailConstants.A_CAL_RANGE, "");

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
        String d = e.getAttribute(MailConstants.A_CAL_DATETIME, null);
        String tz = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
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
            return ParsedDateTime.parse(d, invTzMap, zone, inv.getTimeZoneMap()
                    .getLocalTimeZone());
        } catch (ParseException ex) {
            throw ServiceException.INVALID_REQUEST("could not parse time "
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

        zone = WellKnownTimeZones.getTimeZoneById(tzId);

        if (zone == null) {
            // Could be a custom TZID during modify operation of invite from
            // external calendar system. Look up the TZID from the invite.
            if (invTzMap != null) {
                zone = invTzMap.getTimeZone(tzId);
            }

            if (zone == null) {
                throw ServiceException.INVALID_REQUEST("invalid time zone \"" + tzId + "\"", null);
            }
        }
        if (!inv.getTimeZoneMap().contains(zone))
            inv.getTimeZoneMap().add(zone);
        return zone;
    }

    private static Recurrence.IRecurrence parseRecur(Element recurElt, TimeZoneMap invTzMap, Invite inv) 
    throws ServiceException {
        
        ParsedDuration dur = inv.getDuration();
        if (dur == null && inv.getStartTime() != null && inv.getEndTime() != null) {
            dur = inv.getEndTime().difference(inv.getStartTime());
        }
        
        ArrayList<IRecurrence> addRules = new ArrayList<IRecurrence>();
        ArrayList<IRecurrence> subRules = new ArrayList<IRecurrence>();
        
        for (Iterator iter= recurElt.elementIterator(); iter.hasNext();) {
            Element e = (Element)iter.next();
            
            boolean exclude = false;
            
            if (e.getName().equals(MailConstants.E_CAL_EXCLUDE)) {
                exclude = true;
            } else {
                if (!e.getName().equals(MailConstants.E_CAL_ADD)) {
                    continue;
                }
            }
            
            for (Iterator intIter = e.elementIterator(); intIter.hasNext();) 
            {
                Element intElt = (Element)intIter.next();
                
                if (intElt.getName().equals(MailConstants.E_CAL_DATES)) {
                    // handle RDATE or EXDATE
                    String tzid = intElt.getAttribute(MailConstants.A_CAL_TIMEZONE);
                    ICalTimeZone tz = invTzMap.lookupAndAdd(tzid);
                    RdateExdate rexdate = new RdateExdate(exclude ? ICalTok.EXDATE : ICalTok.RDATE, tz);

                    ICalTok valueType = null;
                    for (Iterator<Element> dtvalIter = intElt.elementIterator(MailConstants.E_CAL_DATE_VAL);
                         dtvalIter.hasNext(); ) {
                        ICalTok dtvalValueType = null;
                        Element dtvalElem = dtvalIter.next();
                        Element dtvalStart = dtvalElem.getElement(MailConstants.E_CAL_START_TIME);
                        String dtvalStartDateStr = dtvalStart.getAttribute(MailConstants.A_CAL_DATETIME);
                        ParsedDateTime dtStart =
                            parseDateTime(dtvalElem.getName(), dtvalStartDateStr, tzid, invTzMap, inv);

                        Element dtvalEnd = dtvalElem.getOptionalElement(MailConstants.E_CAL_END_TIME);
                        Element dtvalDur = dtvalElem.getOptionalElement(MailConstants.E_CAL_DURATION);
                        if (dtvalEnd == null && dtvalDur == null) {
                            if (dtStart.hasTime())
                                dtvalValueType = ICalTok.DATE_TIME;
                            else
                                dtvalValueType = ICalTok.DATE;
                            rexdate.addValue(dtStart);
                        } else {
                            dtvalValueType = ICalTok.PERIOD;
                            if (dtvalEnd != null) {
                                String dtvalEndDateStr = dtvalEnd.getAttribute(MailConstants.A_CAL_DATETIME);
                                ParsedDateTime dtEnd =
                                    parseDateTime(dtvalElem.getName(), dtvalEndDateStr, tzid, invTzMap, inv);
                                Period p = new Period(dtStart, dtEnd);
                                rexdate.addValue(p);
                            } else {
                                ParsedDuration d = ParsedDuration.parse(dtvalDur);
                                Period p = new Period(dtStart, d);
                                rexdate.addValue(p);
                            }
                        }

                        if (valueType == null) {
                            valueType = dtvalValueType;
                            rexdate.setValueType(valueType);
                        } else if (valueType != dtvalValueType)
                            throw ServiceException.INVALID_REQUEST(
                                    "Cannot mix different value types in a single <" +
                                    intElt.getName() + "> element", null);
                    }

                    Recurrence.SingleDates sd = new Recurrence.SingleDates(rexdate, dur);
                    if (exclude)
                        subRules.add(sd);
                    else
                        addRules.add(sd);

                } else if (intElt.getName().equals(MailConstants.E_CAL_RULE)) {
                    // handle RRULE or EXRULE

                    // Turn XML into iCal RECUR string, which will then be
                    // parsed by ical4j Recur object.

                    StringBuilder recurBuf = new StringBuilder(100);

                    String freq = IcalXmlStrMap.sFreqMap.toIcal(
                    		          intElt.getAttribute(MailConstants.A_CAL_RULE_FREQ));
                    recurBuf.append("FREQ=").append(freq);

                    for (Iterator ruleIter = intElt.elementIterator(); ruleIter.hasNext(); ) {
                    	Element ruleElt = (Element) ruleIter.next();
                        String ruleEltName = ruleElt.getName();
                        if (ruleEltName.equals(MailConstants.E_CAL_RULE_UNTIL)) {
                            recurBuf.append(";UNTIL=");
                            String d = ruleElt.getAttribute(MailConstants.A_CAL_DATETIME);
                            recurBuf.append(d);
                            
                            // If UNTIL has time part it must be specified
                            // as UTC time, i.e. ending in "Z".
                            // (RFC2445 Section 4.3.10 Recurrence Rule)
                            if (d.indexOf("T") >= 0)
                                if (d.indexOf("Z") <0)
                                    recurBuf.append('Z');

                            
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_COUNT)) {
                            int num = (int) ruleElt.getAttributeLong(MailConstants.A_CAL_RULE_COUNT_NUM, -1);
                            if (num > 0) {
                                recurBuf.append(";COUNT=").append(num);
                            } else {
                                throw ServiceException.INVALID_REQUEST(
                                    "Expected positive num attribute in <recur> <rule> <count>", null);
                            }
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_INTERVAL)) {
                            String ival = ruleElt.getAttribute(MailConstants.A_CAL_RULE_INTERVAL_IVAL);
                            recurBuf.append(";INTERVAL=").append(ival);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYSECOND)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYSECOND_SECLIST);
                            recurBuf.append(";BYSECOND=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYMINUTE)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYMINUTE_MINLIST);
                            recurBuf.append(";BYMINUTE=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYHOUR)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYHOUR_HRLIST);
                            recurBuf.append(";BYHOUR=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYDAY)) {
                            recurBuf.append(";BYDAY=");
                            int pos = 0;
                            for (Iterator bydayIter = ruleElt.elementIterator(MailConstants.E_CAL_RULE_BYDAY_WKDAY);
                                 bydayIter.hasNext();
                                 pos++) {
                            	Element wkdayElt = (Element) bydayIter.next();
                                if (pos > 0)
                                    recurBuf.append(",");
                                String ordwk = wkdayElt.getAttribute(MailConstants.A_CAL_RULE_BYDAY_WKDAY_ORDWK, null);
                                if (ordwk != null)
                                    recurBuf.append(ordwk);
                                String day = wkdayElt.getAttribute(MailConstants.A_CAL_RULE_DAY);
                                if (day == null || day.length() == 0)
                                    throw ServiceException.INVALID_REQUEST("Missing " +
                                                                               MailConstants.A_CAL_RULE_DAY + " in <" +
                                                                               ruleEltName + ">",
                                                                               null);
                                recurBuf.append(day);
                            }
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYMONTHDAY)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYMONTHDAY_MODAYLIST);
                            recurBuf.append(";BYMONTHDAY=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYYEARDAY)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYYEARDAY_YRDAYLIST);
                            recurBuf.append(";BYYEARDAY=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYWEEKNO)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYWEEKNO_WKLIST);
                            recurBuf.append(";BYWEEKNO=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYMONTH)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYMONTH_MOLIST);
                            recurBuf.append(";BYMONTH=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYSETPOS)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYSETPOS_POSLIST);
                            recurBuf.append(";BYSETPOS=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_WKST)) {
                            String day = ruleElt.getAttribute(MailConstants.A_CAL_RULE_DAY);
                            recurBuf.append(";WKST=").append(day);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_XNAME)) {
                            String name = ruleElt.getAttribute(MailConstants.A_CAL_RULE_XNAME_NAME, null);
                            if (name != null) {
                            	String value = ruleElt.getAttribute(MailConstants.A_CAL_RULE_XNAME_VALUE, "");
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
                        throw ServiceException.INVALID_REQUEST("Exception parsing <recur> <rule>", ex);
                    }
                    
                } else {
                    throw ServiceException.INVALID_REQUEST("Expected <date> or <rule> inside of "+e.getName()+", got "+
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
        String d = e.getAttribute(MailConstants.A_CAL_DATETIME);
        String tzId = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
        ICalTimeZone timezone = null;
        if (tzId != null) {
            timezone = lookupAndAddToTzList(tzId, tzMap, inv);
        }

        try {
            return ParsedDateTime.parse(d, tzMap, timezone, inv.getTimeZoneMap()
                    .getLocalTimeZone());
        } catch (ParseException pe) {
            throw ServiceException.INVALID_REQUEST("Caught ParseException: " + pe, pe);
        }
    }

    private static void parseTimeZones(Element parent, TimeZoneMap tzMap)
            throws ServiceException {
        assert (tzMap != null);
        for (Iterator iter = parent.elementIterator(MailConstants.E_CAL_TZ); iter
                .hasNext();) {
            Element tzElem = (Element) iter.next();
            ICalTimeZone tz = parseTzElement(tzElem);
            tzMap.add(tz);
        }
    }
    
    /**
     * Parse a <tz> definition, as described in soap-calendar.txt and soap.txt (SearchRequest)
     *
     * @param tzElem
     * @return
     * @throws ServiceException
     */
    public static ICalTimeZone parseTzElement(Element tzElem) throws ServiceException {
        String tzid = tzElem.getAttribute(MailConstants.A_ID);
        int standardOffset = (int) tzElem
                .getAttributeLong(MailConstants.A_CAL_TZ_STDOFFSET);
        int daylightOffset = (int) tzElem.getAttributeLong(
                MailConstants.A_CAL_TZ_DAYOFFSET, standardOffset);
        // minutes to milliseconds
        standardOffset *= 60 * 1000;
        daylightOffset *= 60 * 1000;

        SimpleOnset standardOnset = null;
        SimpleOnset daylightOnset = null;
        if (daylightOffset != standardOffset) {
            Element standard = tzElem
                    .getOptionalElement(MailConstants.E_CAL_TZ_STANDARD);
            Element daylight = tzElem
                    .getOptionalElement(MailConstants.E_CAL_TZ_DAYLIGHT);
            if (standard == null || daylight == null)
                throw ServiceException.INVALID_REQUEST(
                                "DST time zone missing standard and/or daylight onset",
                                null);
            standardOnset = parseSimpleOnset(standard);
            daylightOnset = parseSimpleOnset(daylight);
        }

        ICalTimeZone tz = new ICalTimeZone(tzid, standardOffset,
                standardOnset, daylightOffset, daylightOnset);
        return tz;
    }

    private static SimpleOnset parseSimpleOnset(Element element)
            throws ServiceException {
        int week = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_WEEK, 0);
        int wkday = (int) element
                .getAttributeLong(MailConstants.A_CAL_TZ_DAYOFWEEK, 0);
        int month = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_MONTH);
        int mday = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_DAYOFMONTH, 0);
        int hour = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_HOUR);
        int minute = (int) element
                .getAttributeLong(MailConstants.A_CAL_TZ_MINUTE);
        int second = (int) element
                .getAttributeLong(MailConstants.A_CAL_TZ_SECOND);
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
    private static void parseInviteElementCommon(
            Account account, byte itemType, Element element, Invite newInv,
            boolean recurrenceIdAllowed, boolean recurAllowed)
    throws ServiceException {

    	String invId = element.getAttribute(MailConstants.A_ID, null); //zdsync
    	
    	Element compElem = element.getOptionalElement(MailConstants.E_INVITE_COMPONENT);
    	if (compElem != null)
    		element = compElem;

    	String dts = element.getAttribute(MailConstants.A_CAL_DATETIME, null); //zdsync

    	TimeZoneMap tzMap = newInv.getTimeZoneMap();
        parseTimeZones(element.getParent(), tzMap);

        newInv.setItemType(itemType);

        // UID
        String uid = element.getAttribute(MailConstants.A_UID, null);
        if (uid == null || uid.length() == 0)
        		uid = LdapUtil.generateUUID();
        newInv.setUid(uid);

        // RECURRENCE-ID
        if (recurrenceIdAllowed) {
            Element e = element.getElement(MailConstants.E_CAL_EXCEPTION_ID);
            ParsedDateTime dt = parseDateTime(e, tzMap, newInv);
            RecurId recurId = new RecurId(dt, RecurId.RANGE_NONE);
            newInv.setRecurId(recurId);
        } else {
            if (element.getOptionalElement(MailConstants.E_CAL_EXCEPTION_ID) != null) {
                throw ServiceException.INVALID_REQUEST(
                        "May not specify an <exceptId> in this request",
                        null);
            }
        }

        boolean allDay = element.getAttributeBool(MailConstants.A_CAL_ALLDAY,
                false);
        newInv.setIsAllDayEvent(allDay);

        String name = element.getAttribute(MailConstants.A_NAME, "");
        String location = element.getAttribute(MailConstants.A_CAL_LOCATION, "");

        // SEQUENCE
        int seq = (int) element.getAttributeLong(MailConstants.A_CAL_SEQUENCE, 0);
        newInv.setSeqNo(seq);

        // SUMMARY (aka Name or Subject)
        newInv.setName(name);

        // DTSTART
        {
            Element s;
            if (newInv.isTodo())
                s = element.getOptionalElement(MailConstants.E_CAL_START_TIME);
            else
                s = element.getElement(MailConstants.E_CAL_START_TIME);
            if (s != null) {
                ParsedDateTime dt = parseDtElement(s, tzMap, newInv);
                if (dt.hasTime()) {
                    if (allDay) {
                        throw ServiceException.INVALID_REQUEST(
                                        "AllDay event must have DATE, not DATETIME for start time",
                                        null);
                    }
                } else {
                    if (!allDay) {
                        throw ServiceException.INVALID_REQUEST(
                                        "Request must have allDay=\"1\" if using a DATE start time instead of DATETIME",
                                        null);
                    }
                }
                newInv.setDtStart(dt);
            }
        }

        // DTEND (for VEVENT) or DUE (for VTODO)
        {
            Element e = element.getOptionalElement(MailConstants.E_CAL_END_TIME);
            if (e != null) {
                if (element.getOptionalElement(MailConstants.E_CAL_DURATION) != null) {
                    throw ServiceException.INVALID_REQUEST(
                                    "<comp> may have <e> end or <d> duration but not both",
                                    null);
                }
                ParsedDateTime dt = parseDtElement(e, tzMap, newInv);

                if (allDay && !newInv.isTodo()) {
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
                        throw ServiceException.INVALID_REQUEST(
                                        "AllDay event must have DATE, not DATETIME for start time",
                                        null);
                    }
                } else {
                    if (!allDay) {
                        throw ServiceException.INVALID_REQUEST(
                                        "Request must have allDay=\"1\" if using a DATE start time instead of DATETIME",
                                        null);
                    }
                }
                newInv.setDtEnd(dt);
            }
        }

        // DURATION
        {
            Element d = element.getOptionalElement(MailConstants.E_CAL_DURATION);
            if (d != null) {
                ParsedDuration pd = ParsedDuration.parse(d);
                newInv.setDuration(pd);
            }
        }

        // LOCATION
        newInv.setLocation(location);

        // STATUS
        String status = element.getAttribute(MailConstants.A_CAL_STATUS,
                newInv.isEvent() ? IcalXmlStrMap.STATUS_CONFIRMED : IcalXmlStrMap.STATUS_NEEDS_ACTION);
        validateAttr(IcalXmlStrMap.sStatusMap, MailConstants.A_CAL_STATUS, status);
        newInv.setStatus(status);

        // CLASS
        String classProp = element.getAttribute(MailConstants.A_CAL_CLASS, IcalXmlStrMap.CLASS_PUBLIC);
        validateAttr(IcalXmlStrMap.sClassMap, MailConstants.A_CAL_CLASS, classProp);
        newInv.setClassProp(classProp);

        // PRIORITY
        String priority = element.getAttribute(MailConstants.A_CAL_PRIORITY, null);
        newInv.setPriority(priority);

        if (newInv.isEvent()) {
            // FreeBusy
            String fb = element.getAttribute(MailConstants.A_APPT_FREEBUSY,
                    IcalXmlStrMap.FBTYPE_BUSY);
            newInv.setFreeBusy(fb);
    
            // TRANSPARENCY
            String transp = element.getAttribute(MailConstants.A_APPT_TRANSPARENCY,
                    IcalXmlStrMap.TRANSP_OPAQUE);
            validateAttr(IcalXmlStrMap.sTranspMap, MailConstants.A_APPT_TRANSPARENCY,
                    transp);
            newInv.setTransparency(transp);
        }

        if (newInv.isTodo()) {
            // PERCENT-COMPLETE
            String pctComplete = element.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, null);
            newInv.setPercentComplete(pctComplete);

            // COMPLETED
            String completed = element.getAttribute(MailConstants.A_TASK_COMPLETED, null);
            if (completed != null) {
                try {
                    ParsedDateTime c = ParsedDateTime.parseUtcOnly(completed);
                    newInv.setCompleted(c.getUtcTime());
                } catch (ParseException e) {
                    throw ServiceException.INVALID_REQUEST("Invalid COMPLETED value: " + completed, e);
                }
            }
        }

        // ATTENDEEs
        boolean hasAttendees = false;
        for (Iterator<Element> iter = element
                .elementIterator(MailConstants.E_CAL_ATTENDEE); iter.hasNext();) {
            ZAttendee at = ZAttendee.parse(iter.next());
            newInv.addAttendee(at);
            hasAttendees = true;
        }
        if (hasAttendees &&
            newInv.getMethod().equals(ICalTok.PUBLISH.toString())) {
            newInv.setMethod(ICalTok.REQUEST.toString());
        }

        // ORGANIZER
        Element orgElt = element
                .getOptionalElement(MailConstants.E_CAL_ORGANIZER);
        if (orgElt == null) {
            if (hasAttendees)
                throw ServiceException.INVALID_REQUEST(
                        "missing organizer when attendees are present", null);
        } else {
            ZOrganizer org = ZOrganizer.parse(orgElt);
            newInv.setOrganizer(org);
        }

        // Once we have organizer and attendee information, we can tell if this account is the
        // organizer in this invite or not.
        newInv.setIsOrganizer(account);

        // RECUR
        Element recur = element.getOptionalElement(MailConstants.A_CAL_RECUR);
        if (recur != null) {
            if (!recurAllowed) {
                throw ServiceException.INVALID_REQUEST(
                        "No <recur> allowed in an exception", null);
            }
            Recurrence.IRecurrence recurrence = parseRecur(recur, tzMap,
                    newInv);
            newInv.setRecurrence(recurrence);
        }

        // VALARMs
        Iterator<Element> alarmsIter = element.elementIterator(MailConstants.E_CAL_ALARM);
        while (alarmsIter.hasNext()) {
            Alarm alarm = Alarm.parse(alarmsIter.next());
            if (alarm != null)
                newInv.addAlarm(alarm);
        }

        List<ZProperty> xprops = parseXProps(element);
        for (ZProperty prop : xprops)
            newInv.addXProp(prop);

        newInv.validateDuration();
    	
        //zdsync: must set this only after recur is processed
    	if (invId != null) {
    	    try {
        	    int invIdInt = Integer.parseInt(invId);
        	    newInv.setInviteId(invIdInt);
    	    } catch (NumberFormatException e) {
    	        // ignore if invId is not a number, e.g. refers to a remote account
    	    }
    	}
    	if (dts != null) {
    		newInv.setDtStamp(Long.parseLong(dts));
    	}
    	Element fragment = element.getOptionalElement(MailConstants.E_FRAG);
    	if (fragment != null) {
    		newInv.setFragment(fragment.getText());
    	}
    }

    public static List<ZParameter> parseXParams(Element element) throws ServiceException {
        List<ZParameter> params = new ArrayList<ZParameter>();
        for (Iterator<Element> paramIter = element.elementIterator(MailConstants.E_CAL_XPARAM);
             paramIter.hasNext(); ) {
            Element paramElem = paramIter.next();
            String paramName = paramElem.getAttribute(MailConstants.A_NAME);
            String paramValue = paramElem.getAttribute(MailConstants.A_VALUE, null);    
            ZParameter xparam = new ZParameter(paramName, paramValue);
            params.add(xparam);
        }
        return params;
    }

    public static List<ZProperty> parseXProps(Element element) throws ServiceException {
        List<ZProperty> props = new ArrayList<ZProperty>();
        for (Iterator<Element> propIter = element.elementIterator(MailConstants.E_CAL_XPROP);
             propIter.hasNext(); ) {
            Element propElem = propIter.next();
            String propName = propElem.getAttribute(MailConstants.A_NAME);
            String propValue = propElem.getAttribute(MailConstants.A_VALUE, null);
            ZProperty xprop = new ZProperty(propName);
            xprop.setValue(propValue);
            List<ZParameter> xparams = parseXParams(propElem);
            for (ZParameter xparam : xparams) {
                xprop.addParameter(xparam);
            }
            props.add(xprop);
        }
        return props;
    }

    public static List<ReplyInfo> parseReplyList(Element element, TimeZoneMap tzMap)
    throws ServiceException {
        List<ReplyInfo> list = new ArrayList<ReplyInfo>();
        for (Iterator<Element> iter = element.elementIterator(MailConstants.E_CAL_REPLY);
             iter.hasNext(); ) {
            Element riElem = iter.next();
            String addr = riElem.getAttribute(MailConstants.A_CAL_ATTENDEE);
            ZAttendee at = new ZAttendee(addr);
            String sentBy = riElem.getAttribute(MailConstants.A_CAL_SENTBY, null);
            if (sentBy != null)
                at.setSentBy(sentBy);
            String partStat = riElem.getAttribute(MailConstants.A_CAL_PARTSTAT, null);
            if (partStat != null)
                at.setPartStat(partStat);
            int seq = (int) riElem.getAttributeLong(MailConstants.A_SEQ);
            long dtStamp = riElem.getAttributeLong(MailConstants.A_DATE);
            RecurId recurId = RecurId.fromXml(riElem, tzMap);
            ReplyInfo ri = new ReplyInfo(at, seq, dtStamp, recurId);
            list.add(ri);
        }
        return list;
    }

    private static void validateAttr(IcalXmlStrMap map, String attrName,
            String value) throws ServiceException {
        if (!map.validXml(value)) {
            throw ServiceException.INVALID_REQUEST("Invalid value '"
                    + value + "' specified for attribute:" + attrName, null);
        }

    }

    public static Invite buildCancelInviteCalendar(
            Account acct, Account senderAcct, boolean onBehalfOf, Invite inv,
            String comment, List<ZAttendee> forAttendees) throws ServiceException {
        return cancelInvite(acct, senderAcct, onBehalfOf, inv, comment, forAttendees, null);
    }

    public static Invite buildCancelInviteCalendar(
            Account acct, Account senderAcct, boolean onBehalfOf, Invite inv,
            String comment) throws ServiceException {
        return cancelInvite(acct, senderAcct, onBehalfOf, inv, comment, null, null);
    }

    public static Invite buildCancelInstanceCalendar(
            Account acct, Account senderAcct, boolean onBehalfOf, Invite inv,
            String comment, RecurId recurId) throws ServiceException {
        return cancelInvite(acct, senderAcct, onBehalfOf, inv, comment, null, recurId);
    }

    /**
     * See 4.2.10
     * 
     * Cancel an Invite for specified Attendees (or for ALL attendees if NULL
     * is passed)
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
    static Invite cancelInvite(
            Account acct, Account senderAcct, boolean onBehalfOf,
            Invite inv, String comment,
            List<ZAttendee> forAttendees, RecurId recurId)
    throws ServiceException {
        return cancelInvite(acct, senderAcct, onBehalfOf,
                            inv, comment, forAttendees, recurId, true);
    }

    private static Invite cancelInvite(
            Account acct, Account senderAcct, boolean onBehalfOf,
            Invite inv, String comment,
            List<ZAttendee> forAttendees, RecurId recurId,
            boolean incrementSeq)
    throws ServiceException {
        Invite cancel = new Invite(inv.getItemType(), ICalTok.CANCEL.toString(),
                                   inv.getTimeZoneMap(), inv.isOrganizer());

        // ORGANIZER
        if (inv.hasOrganizer()) {
            ZOrganizer org = new ZOrganizer(inv.getOrganizer());
            if (onBehalfOf && senderAcct != null)
                org.setSentBy(senderAcct.getName());
            cancel.setOrganizer(org);
        }

        // ATTENDEEs
        List<ZAttendee> attendees =
            forAttendees != null ? forAttendees : inv.getAttendees();
        for (ZAttendee a : attendees)
            cancel.addAttendee(a);

        cancel.setClassProp(inv.getClassProp());
        boolean hidePrivate = !inv.isPublic() && !Account.allowPrivateAccess(senderAcct, acct);
        Locale locale = acct.getLocale();
        if (hidePrivate) {
            // SUMMARY
            String sbj = L10nUtil.getMessage(MsgKey.calendarSubjectWithheld, locale);
            cancel.setName(CalendarMailSender.getCancelSubject(sbj, locale));
        } else {
            // SUMMARY
            cancel.setName(CalendarMailSender.getCancelSubject(inv.getName(), locale));

            // COMMENT
            if (comment != null && !comment.equals(""))
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

        // DTSTART, DTEND, and LOCATION (Outlook seems to require these, even
        // though they are optional according to RFC2446.)
        ParsedDateTime dtStart = recurId == null ? inv.getStartTime() : recurId.getDt();
        if (dtStart != null) {
            cancel.setDtStart(dtStart);
            ParsedDuration dur = inv.getEffectiveDuration();
            if (dur != null)
                cancel.setDtEnd(dtStart.add(dur));
        }
        cancel.setLocation(inv.getLocation());

        // SEQUENCE
        int seq = inv.getSeqNo();
        if (incrementSeq) {
            // Increment only if this account is the organizer.  If this
            // account is a non-organizer attendee, leave the sequence at
            // present value.  (bug 8465)
            if (acct != null && inv.isOrganizer())
                seq++;
        }
        cancel.setSeqNo(seq);

        // STATUS
        cancel.setStatus(IcalXmlStrMap.STATUS_CANCELLED);

        // DTSTAMP
        cancel.setDtStamp(new Date().getTime());

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
