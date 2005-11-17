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

package com.zimbra.cs.mailbox.calendar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;

public class WindowsTZUtils {

    public static WindowsTimeZoneInformation ICalToWindows(ICalTimeZone icalTz) {
        WindowsSystemTime standardDate =
            toWindowsSystemTime(icalTz.getStandardDtStart(),
                                icalTz.getStandardRule());
        WindowsSystemTime daylightDate =
            toWindowsSystemTime(icalTz.getDaylightDtStart(),
                                icalTz.getDaylightRule());
        // Notice Windows and iCalendar use opposite signs
        // for time zone offset/bias values.
        int bias = -1 * icalTz.getStandardOffset();
        int daylightBias = -1 * icalTz.getDaylightOffset() - bias;
        return new WindowsTimeZoneInformation(
                icalTz.getID(), bias / 60 / 1000,
                standardDate, 0,
                daylightDate, daylightBias / 60 / 1000);
    }

    public static ICalTimeZone WindowsToICal(WindowsTimeZoneInformation winTz) {
        WindowsSystemTime standardDate = winTz.getStandardDate();
        String standardDtStart = getICalDtStart(standardDate);
        String standardRule = getICalRRule(standardDate);

        WindowsSystemTime daylightDate = winTz.getDaylightDate();
        String daylightDtStart = getICalDtStart(daylightDate);
        String daylightRule = getICalRRule(daylightDate);

        return new ICalTimeZone(
                winTz.getName(),
                winTz.getStandardOffset(),
                standardDtStart,
                standardRule,
                winTz.getDaylightOffset(),
                daylightDtStart,
                daylightRule);

        // TODO: Notice we convert the numeric time data to formatted string
        // to pass into ICalTimeZone constructor.  In that class the string
        // is parsed and decomposed again.  Need to avoid this inefficiency
        // by adding an ICalTimeZone constructor that takes numeric arguments.
    }

    private static WindowsSystemTime toWindowsSystemTime(String dtStart,
                                                         String rule) {
        OnsetTime t = new OnsetTime(dtStart);
        OnsetRuleForWindows r = new OnsetRuleForWindows(rule);
        return new WindowsSystemTime(
                0, r.getMonth(), r.getDayOfWeek(), r.getWeekNum(),
                t.getHour(), t.getMinute(), t.getSecond(), 0);
    }

    // maps Windows weekday number to iCalendar weekday name
    private static String sDayOfWeekNames[] = new String[7];
    static {
        sDayOfWeekNames[0] = "SU";
        sDayOfWeekNames[1] = "MO";
        sDayOfWeekNames[2] = "TU";
        sDayOfWeekNames[3] = "WE";
        sDayOfWeekNames[4] = "TH";
        sDayOfWeekNames[5] = "FR";
        sDayOfWeekNames[6] = "SA";
    }

    // maps iCalendar weekday name to Windows weekday number
    private static Map /*<String, Integer>*/ sDayOfWeekMap = new HashMap(7);
    static {
        sDayOfWeekMap.put("SU", new Integer(0));
        sDayOfWeekMap.put("MO", new Integer(1));
        sDayOfWeekMap.put("TU", new Integer(2));
        sDayOfWeekMap.put("WE", new Integer(3));
        sDayOfWeekMap.put("TH", new Integer(4));
        sDayOfWeekMap.put("FR", new Integer(5));
        sDayOfWeekMap.put("SA", new Integer(6));
    }

    private static String getICalDtStart(WindowsSystemTime systime) {
        String hourStr = Integer.toString(systime.getHour() + 100).substring(1);
        String minuteStr = Integer.toString(systime.getMinute() + 100).substring(1);
        String secondStr = Integer.toString(systime.getSecond() + 100).substring(1);
        StringBuffer sb = new StringBuffer("16010101T");
        sb.append(hourStr).append(minuteStr).append(secondStr);
        return sb.toString();
    }

    private static String getICalRRule(WindowsSystemTime systime) {
        if (systime.getMonth() == 0) return null;
        StringBuffer sb =
            new StringBuffer("FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=");
        sb.append(systime.getMonth()).append(";BYDAY=");
        int day = systime.getDay();
        int weeknum = day < 5 ? day : -1;
        sb.append(weeknum).append(sDayOfWeekNames[systime.getDayOfWeek()]);
        return sb.toString();
    }

    private static class OnsetTime {
        private int mHour;
        private int mMinute;
        private int mSecond;

        public int getHour()   { return mHour; }
        public int getMinute() { return mMinute; }
        public int getSecond() { return mSecond; }

        public OnsetTime(String dtStart) {
            if (dtStart != null) {
                int indexT = dtStart.indexOf('T');
                if (indexT < 0 || dtStart.length() < indexT + 7)
                    throw new IllegalArgumentException("Invalid dtStart string: " + dtStart);
                
                mHour   = Integer.parseInt(dtStart.substring(indexT + 1, indexT + 3));
                mMinute = Integer.parseInt(dtStart.substring(indexT + 3, indexT + 5));
                mSecond = Integer.parseInt(dtStart.substring(indexT + 5, indexT + 7));
            } else {
                mHour = mMinute = mSecond = 0;
            }
        }
    }

    /**
     * Class for parsing a time zone recurrence rule into components used to
     * initialize a Windows TIME_ZONE_INFORMATION structure.  Only the simple
     * rule in the style of "first Sunday of April every year" is supported.
     */
    private static class OnsetRuleForWindows {
        private int mMonth = 0;
        private int mWeekNum = 0;
        private int mDayOfWeek = 0;

        public int getMonth()     { return mMonth; }
        public int getWeekNum()   { return mWeekNum; }
        public int getDayOfWeek() { return mDayOfWeek; }

        public OnsetRuleForWindows(String rrule) {
            if (rrule != null) {
                for (StringTokenizer t = new StringTokenizer(rrule.toUpperCase(), ";=");
                     t.hasMoreTokens();) {
                    String token = t.nextToken();
                    if ("BYMONTH".equals(token)) {
                        // Both iCalendar month and Windows month are 1-based,
                        // unlike Java month which is 0-based.
                        mMonth = Integer.parseInt(t.nextToken());
                    } else if ("BYDAY".equals(token)) {
                        boolean negative = false;
                        int weekNum = 1;
                        String value = t.nextToken();

                        char sign = value.charAt(0);
                        if (sign == '-') {
                            negative = true;
                            value = value.substring(1);
                        } if (sign == '+') {
                            value = value.substring(1);
                        }

                        char num = value.charAt(0);
                        if (Character.isDigit(num)) {
                            weekNum = num - '0';
                            value = value.substring(1);
                        }

                        // Windows SYSTEMTIME week number has the range of [1, 5].
                        if (negative)
                            weekNum = weekNum * -1 + 6;
                        if (weekNum < 1 || weekNum > 5)
                            throw new IllegalArgumentException("Invalid week number value: " + value);
                        mWeekNum = weekNum;

                        Integer day = (Integer) sDayOfWeekMap.get(value);
                        if (day == null)
                            throw new IllegalArgumentException("Invalid day of week value: " + value);
                        mDayOfWeek = day.intValue();
                    } else {
                        String s = t.nextToken();  // skip value of unused param
                    }
                }
            } else {
                mMonth = 0;
                mWeekNum = 0;
                mDayOfWeek = 0;
            }
        }
    }

    public static void main(String args[]) throws Exception {
        int badConvs = 0;
        List tzList = Provisioning.getInstance().getAllTimeZones();
        for (Iterator iter = tzList.iterator(); iter.hasNext(); ) {
            WellKnownTimeZone wktz = (WellKnownTimeZone) iter.next();
            ICalTimeZone ical = wktz.toTimeZone();
            WindowsTimeZoneInformation win = ICalToWindows(ical);
            ICalTimeZone ical2 = WindowsToICal(win);

            System.out.println("TIMEZONE: " + wktz.getName());
            System.out.println("--------------------------------------------------");
            System.out.println("iCal original:\n" + ical);
            System.out.println("    " + ical.getStandardDtStart() + ", " + ical.getStandardRule());
            System.out.println("    " + ical.getDaylightDtStart() + ", " + ical.getDaylightRule());
            System.out.println("iCal again:\n" + ical2);
            System.out.println("    " + ical2.getStandardDtStart() + ", " + ical2.getStandardRule());
            System.out.println("    " + ical2.getDaylightDtStart() + ", " + ical2.getDaylightRule());
            System.out.println("Windows:\n" + win);
            System.out.println();

            if (!ical2.hasSameRules(ical)) {
                badConvs++;
                System.out.println("Conversion is BAD.");
            }
        }
        System.out.println("--------------------------------------------------");
        System.out.println("Bad Conversions = " + badConvs);
    }
}
