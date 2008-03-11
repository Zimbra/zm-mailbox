/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.tzfixup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.WellKnownTimeZones;
import com.zimbra.cs.mailbox.calendar.tzfixup.TimeZoneFixupRules.Matcher;
import com.zimbra.cs.service.mail.CalendarUtils;

/*

<?xml version="1.0" encoding="utf-8"?>
<tzfixup xmlns="url:zimbraTZFixup">
  <!-- specify multiple fixupRule's -->
  <fixupRule>
    <!-- if timezone matches any of the criteria, replace with the timezone in <replace> -->
    <!-- specify one or many criteria; each type can be specified multiple times -->
    <match>
      <!-- match the timezone's TZID string -->
      <tzid id="[TZID]"/>

      <!-- match the GMT offset of a timezone that doesn't use daylight savings time -->
      <nonDst offset="[GMT offset in minutes]"/>

      <!-- match DST timezone based on transition rules specified as month and week number + week day -->
      <rules stdoff="[GMT offset in minutes during standard time]"
             dayoff="[GMT offset in minutes during daylight savings time]">
        <standard mon="[1..12]" week="[-1, 1..4]" wkday="[1..7]"/>
        <daylight mon="[1..12]" week="[-1, 1..4]" wkday="[1..7]"/>
        <!-- mon=1 means January, mon=12 means December -->
        <!-- week=-1 means last week of the month -->
        <!-- wkday=1 means Sunday, wkday = 7 means Saturday -->
      </rules>

      <!-- match DST timezone based on transition rules specified as month and day of month -->
      <!-- This case is rare and is typically required only for timezones introduced by buggy code. -->
      <dates stdoff="[GMT offset in minutes during standard time]"
             dayoff="[GMT offset in minutes during daylight savings time]">
        <standard mon="[1..12]" mday="[1..31]"/>
        <daylight mon="[1..12]" mday="[1..31]"/>
        <!-- mon=1 means January, mon=12 means December -->
      </dates>
    </match>

    <!-- timezone matching any of the above criteria is replaced with this timezone -->
    <replace>
      <!-- lookup a well-known timezone from /opt/zimbra/conf/timezones.ics file -->
      <wellKnownTz id="[well-known TZID]">
      OR
      <!-- full timezone definition as documented in soap-calendar.txt -->
      <tz id="[custom TZID]" ... />
    </replace>
  </fixupRule>
</tzfixup>

Note the TZID value of the replacement timezone is not used.  The replaced timezone will retain the
original TZID because the appointment/task containing the timezone has other properties/parameters
that refer to the existing TZID.  Only the definition of the timezone is replaced.

 */
public class XmlFixupRules {

    public static final String E_TZFIXUP = "tzfixup";
    private static final String E_FIXUP_RULE = "fixupRule";
    private static final String E_MATCH = "match";
    private static final String E_TZID = "tzid";
    private static final String E_NON_DST = "nonDst";
    private static final String E_RULES = "rules";
    private static final String E_DATES = "dates";
    private static final String E_STANDARD = "standard";
    private static final String E_DAYLIGHT = "daylight";
    private static final String E_REPLACE = "replace";
    private static final String E_WELL_KNOWN_TZ = "wellKnownTz";

    private static final String A_ID = "id";
    private static final String A_OFFSET = "offset";
    private static final String A_STDOFF = "stdoff";
    private static final String A_DAYOFF = "dayoff";
    private static final String A_MON = "mon";
    private static final String A_WEEK = "week";
    private static final String A_WKDAY = "wkday";
    private static final String A_MDAY = "mday";

    private static void parseMatchers(Element matchElem, ICalTimeZone replacementTZ, List<Matcher> matchers)
    throws ServiceException {
        for (Iterator<Element> elemIter = matchElem.elementIterator(); elemIter.hasNext(); ) {
            Element elem = elemIter.next();
            String elemName = elem.getName();
            if (elemName.equals(E_TZID)) {
                String tzid = elem.getAttribute(A_ID);
                matchers.add(new Matcher(tzid, replacementTZ));
            } else if (elemName.equals(E_NON_DST)) {
                long offset = elem.getAttributeLong(A_OFFSET);
                matchers.add(new Matcher(offset, replacementTZ));
            } else if (elemName.equals(E_RULES)) {
                long stdOffset = elem.getAttributeLong(A_STDOFF);
                long dstOffset = elem.getAttributeLong(A_DAYOFF);
                Element stdElem = elem.getElement(E_STANDARD);
                int stdMon = (int) stdElem.getAttributeLong(A_MON);
                int stdWeek = (int) stdElem.getAttributeLong(A_WEEK);
                int stdWkday = (int) stdElem.getAttributeLong(A_WKDAY);
                Element dstElem = elem.getElement(E_DAYLIGHT);
                int dstMon = (int) dstElem.getAttributeLong(A_MON);
                int dstWeek = (int) dstElem.getAttributeLong(A_WEEK);
                int dstWkday = (int) dstElem.getAttributeLong(A_WKDAY);
                Matcher m = new Matcher(stdOffset, stdMon, stdWeek, stdWkday,
                                        dstOffset, dstMon, dstWeek, dstWkday,
                                        replacementTZ);
                matchers.add(m);
            } else if (elemName.equals(E_DATES)) {
                long stdOffset = elem.getAttributeLong(A_STDOFF);
                long dstOffset = elem.getAttributeLong(A_DAYOFF);
                Element stdElem = elem.getElement(E_STANDARD);
                int stdMon = (int) stdElem.getAttributeLong(A_MON);
                int stdMday = (int) stdElem.getAttributeLong(A_MDAY);
                Element dstElem = elem.getElement(E_DAYLIGHT);
                int dstMon = (int) dstElem.getAttributeLong(A_MON);
                int dstMday = (int) dstElem.getAttributeLong(A_MDAY);
                Matcher m = new Matcher(stdOffset, stdMon, stdMday,
                                        dstOffset, dstMon, dstMday,
                                        replacementTZ);
                matchers.add(m);
            }
        }
    }

    private static void parseFixupRule(Element fixupRuleElem, List<Matcher> matchers)
    throws ServiceException {
        Element matchElem = fixupRuleElem.getElement(E_MATCH);
        ICalTimeZone replacementTZ = null;
        Element replaceElem = fixupRuleElem.getElement(E_REPLACE);
        Element wellKnownTzElem = replaceElem.getOptionalElement(E_WELL_KNOWN_TZ);
        if (wellKnownTzElem != null) {
            String tzid = wellKnownTzElem.getAttribute(A_ID);
            replacementTZ = WellKnownTimeZones.getTimeZoneById(tzid);
            if (replacementTZ == null)
                throw ServiceException.FAILURE("Unknown TZID \"" + tzid + "\"", null);
        } else {
            Element tzElem = replaceElem.getOptionalElement(MailConstants.E_CAL_TZ);
            if (tzElem == null)
                throw ServiceException.FAILURE("Neither <tz> nor <wellKnownTz> found in <replace>", null);
            replacementTZ = CalendarUtils.parseTzElement(tzElem);
        }
        parseMatchers(matchElem, replacementTZ, matchers);
    }

    public static List<Matcher> parseTzFixup(Element tzFixupElem)
    throws ServiceException {
        List<Matcher> matchers = new ArrayList<Matcher>();
        for (Iterator<Element> elemIter = tzFixupElem.elementIterator(E_FIXUP_RULE); elemIter.hasNext(); ) {
            Element fixupRuleElem = elemIter.next();
            parseFixupRule(fixupRuleElem, matchers);
        }
        return matchers;
    }
}
