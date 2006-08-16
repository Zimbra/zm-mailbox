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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.service.ServiceException;

/**
 * Time zone class that combines java.util.SimpleTimeZone and iCalendar
 * VTIMEZONE data.
 *
 * Unlike the requirements in RFC2445 iCalendar spec, this class supports
 * VTIMEZONEs with at most one STANDARD component and at most one DAYLIGHT
 * component.  Each STANDARD or DAYLIGHT component supports RRULE property
 * but not RDATE property.  Each RRULE must specify exactly one day per
 * year and that day must be a day-of-week in Nth week of a certain month.
 *
 * In plain English, this class supports time zones that do not use
 * daylight savings time, and time zones that define daylight savings rule
 * with a single daylight-begin date as day-of-week in a particular week of
 * a particular month, and likewise for daylight-end date.  For example,
 * the rule must be something like "daylight savings time begins on the
 * first Sunday of April and ends on the last Sunday of October."
 *
 * Fully RFC2445-compliant VTIMEZONE supports multiple STANDARD and
 * DAYLIGHT components, with each component specifying the onset rule with
 * RRULE, RDATE, or not specify the rule at all.  But this full capability
 * should not be necessary to support most time zones in actual use.
 */
public class ICalTimeZone extends SimpleTimeZone
{
    public static class SimpleOnset {
        private int mWeek       = 0;
        private int mDayOfWeek  = 0;
        private int mMonth      = 0;
        private int mDayOfMonth = 0;
        private int mHour       = 0;
        private int mMinute     = 0;
        private int mSecond     = 0;

        public int getWeek()       { return mWeek; }       // week 1, 2, 3, 4, -1 (last)
        public int getDayOfWeek()  { return mDayOfWeek; }  // 1=Sunday, 2=Monday, etc.
        public int getMonth()      { return mMonth; }      // 1=January, 2=February, etc.
        public int getDayOfMonth() { return mDayOfMonth; }
        public int getHour()       { return mHour; }       // 0..23
        public int getMinute()     { return mMinute; }     // 0..59
        public int getSecond()     { return mSecond; }     // 0..59

        // whether this onset is defined with a recurrence rule or as a specific date
        public boolean hasRule() {
            return mWeek != 0;
        }

        public SimpleOnset(int week, int dayOfWeek, int month, int dayOfMonth,
                           int hour, int minute, int second) {
            mWeek = week;
            mDayOfWeek = dayOfWeek;
            mMonth = month;
            mDayOfMonth = dayOfMonth;
            mHour = hour;
            mMinute = minute;
            mSecond = second;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("week=").append(mWeek);
            sb.append(", dayOfWeek=").append(mDayOfWeek);
            sb.append(", month=").append(mMonth);
            sb.append(", dayOfMonth=").append(mDayOfMonth);
            sb.append(", hour=").append(mHour);
            sb.append(", minute=").append(mMinute);
            sb.append(", second=").append(mSecond);
            sb.append(", hasRule=").append(hasRule());
            return sb.toString();
        }
    }
    
    private static final String DEFAULT_DTSTART = "16010101T000000";

    protected boolean mHasDaylight = false;

    protected int    mStandardOffset = 0;
    protected String mDayToStdDtStart = DEFAULT_DTSTART;
    protected String mDayToStdRule = null;

    protected int    mDaylightOffset = 0; 
    protected String mStdToDayDtStart = DEFAULT_DTSTART;
    protected String mStdToDayRule = null;

    private SimpleOnset mStandardOnset;
    private SimpleOnset mDaylightOnset;

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("TZID=").append(getID());
        sb.append("\nSimpleTimeZone: ").append(super.toString());
        sb.append("\nmHasDaylight=").append(mHasDaylight);
        sb.append("\nmStandardOffset=").append(mStandardOffset);
        sb.append(", mDayToStdDtStart=").append(mDayToStdDtStart);
        sb.append(", mDayToStdRule=\"").append(mDayToStdRule).append("\"");
        sb.append("\nmStandardOnset=\"").append(mStandardOnset).append("\"");
        sb.append("\nmDaylightOffset=").append(mDaylightOffset);
        sb.append(", mStdToDayDtStart=").append(mStdToDayDtStart);
        sb.append(", mStdToDayRule=\"").append(mStdToDayRule).append("\"");
        sb.append("\nmDaylightOnset=\"").append(mDaylightOnset).append("\"");
        return sb.toString();
    }


    private static final String FN_TZ_NAME          = "tzid";
    private static final String FN_STD_OFFSET       = "so";
    private static final String FN_HAS_DAYLIGHT     = "hd";
    private static final String FN_DAYLIGHT_OFFSET  = "do";
    private static final String FN_DAYTOSTD_DTSTART = "d2ss";
    private static final String FN_STDTODAY_DTSTART = "s2ds";
    private static final String FN_DAYTOSTD_RULE    = "d2sr";
    private static final String FN_STDTODAY_RULE    = "s2dr";

    private static ICalTimeZone sUTC = new ICalTimeZone("Z",
                                                        0,
                                                        "16010101T000000",
                                                        null,
                                                        0,
                                                        "16010101T000000",
                                                        null);

//    private String mTzId = null;

    public static ICalTimeZone getUTC() { return sUTC; }

    public Metadata encodeAsMetadata() {
        Metadata meta = new Metadata();
        if (getID() == null) {
            System.out.println("null tzid!");
        }
        meta.put(FN_TZ_NAME, getID());
        meta.put(FN_HAS_DAYLIGHT, mHasDaylight);

        meta.put(FN_STD_OFFSET, mStandardOffset);
        meta.put(FN_DAYTOSTD_DTSTART, mDayToStdDtStart); 
        meta.put(FN_DAYTOSTD_RULE, mDayToStdRule);

        meta.put(FN_DAYLIGHT_OFFSET, mDaylightOffset);
        meta.put(FN_STDTODAY_DTSTART, mStdToDayDtStart);  
        meta.put(FN_STDTODAY_RULE, mStdToDayRule);
        return meta;
    }

    public String getStandardDtStart() { return mDayToStdDtStart; }
    public String getStandardRule()    { return mDayToStdRule; }
    public String getDaylightDtStart() { return mStdToDayDtStart; }
    public String getDaylightRule()    { return mStdToDayRule; }

    public static ICalTimeZone decodeFromMetadata(Metadata m) throws ServiceException {
        String tzId;
        if (m.containsKey(FN_TZ_NAME))
            tzId = m.get(FN_TZ_NAME);
        else
            tzId = "unknown time zone";
        return new ICalTimeZone(tzId, m);
    }
    
    private ICalTimeZone(String tzId, Metadata meta) throws ServiceException {
        super(0, tzId);
//        mTzId = tzId;
        mHasDaylight = meta.getBool(FN_HAS_DAYLIGHT, false);

        mStandardOffset = (int) meta.getLong(FN_STD_OFFSET, 0);
        mDayToStdDtStart = meta.get(FN_DAYTOSTD_DTSTART, null);
        mDayToStdRule = meta.get(FN_DAYTOSTD_RULE, null);

        mDaylightOffset = (int) meta.getLong(FN_DAYLIGHT_OFFSET, mStandardOffset);
        mStdToDayDtStart = meta.get(FN_STDTODAY_DTSTART, mDayToStdDtStart);
        mStdToDayRule = meta.get(FN_STDTODAY_RULE, null);

        initFromICalData();
    }
    
    private void initFromICalData() {
        setRawOffset(mStandardOffset);
        if (mHasDaylight) {
            mStandardOnset = parseOnset(mDayToStdRule, mDayToStdDtStart);
            mDaylightOnset = parseOnset(mStdToDayRule, mStdToDayDtStart);

            SimpleTimeZoneRule stzDaylight =
                new SimpleTimeZoneRule(mDaylightOnset);
            setStartRule(stzDaylight.mMonth,
                         stzDaylight.mDayOfMonth,
                         stzDaylight.mDayOfWeek,
                         stzDaylight.mDtStartMillis);
            SimpleTimeZoneRule stzStandard =
                new SimpleTimeZoneRule(mStandardOnset);
            setEndRule(stzStandard.mMonth,
                       stzStandard.mDayOfMonth,
                       stzStandard.mDayOfWeek,
                       stzStandard.mDtStartMillis);
            setDSTSavings(mDaylightOffset - mStandardOffset);
        }
    }

    private static class SimpleTimeZoneRule {
        // onset rule values transformed to suit SimpleTimeZone API
        public int mMonth;
        public int mDayOfMonth;
        public int mDayOfWeek;
        public int mDtStartMillis;

        public SimpleTimeZoneRule(SimpleOnset onset) {
            // iCalendar month is 1-based.  Java month is 0-based.
            mMonth = onset.getMonth() - 1;
            if (onset.hasRule()) {
                int week = onset.getWeek();
                if (week < 0) {
                    // For specifying day-of-week of last Nth week of month,
                    // e.g. -2SA for Saturday of 2nd to last week of month,
                    // java.util.SimpleTimeZone wants negative week number
                    // in dayOfMonth.
                    mDayOfMonth = week;
    
                    mDayOfWeek = onset.getDayOfWeek();
                } else {
                    // For positive week, onset date is day of week on or
                    // after day of month.  First week is day 1 through day 7,
                    // second week is day 8 through day 14, etc.
                    mDayOfMonth = (week - 1) * 7 + 1;
    
                    // Another peculiarity of java.util.SimpleTimeZone class.
                    // For positive week, day-of-week must be specified as
                    // a negative value.
                    mDayOfWeek = -1 * onset.getDayOfWeek();
                }
            } else {
                mDayOfMonth = onset.getDayOfMonth();
                mDayOfWeek = 0;
            }
            mDtStartMillis =
                onset.getHour() * 3600000 + onset.getMinute() * 60000 + onset.getSecond() * 1000;
        }
    }

    private static SimpleOnset parseOnset(String rrule, String dtstart) {
        int week = 0;
        int dayOfWeek = 0;
        int month = 0;
        int dayOfMonth = 0;
        int hour = 0;
        int minute = 0;
        int second = 0;

        if (rrule != null) {
            for (StringTokenizer t = new StringTokenizer(rrule.toUpperCase(), ";=");
                 t.hasMoreTokens();) {
                String token = t.nextToken();
                if ("BYMONTH".equals(token)) {
                    month = Integer.parseInt(t.nextToken());
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
                    week = negative ? -1 * weekNum : weekNum;

                    Integer day = (Integer) sDayOfWeekMap.get(value);
                    if (day == null)
                        throw new IllegalArgumentException("Invalid day of week value: " + value);
                    dayOfWeek = day.intValue();
                } else {
                    String s = t.nextToken();  // skip value of unused param
                }
            }
        } else {
            // No RRULE provided.  Get month and day of month from DTSTART.
            week = 0;
            month = dayOfMonth = 1;
            if (dtstart != null) {
                try {
                    month = Integer.parseInt(dtstart.substring(4, 6));
                    dayOfWeek = Integer.parseInt(dtstart.substring(6, 8));
                } catch (StringIndexOutOfBoundsException se) {}
            }
        }

        if (dtstart != null) {
            // Discard date and decompose time fields.
            try {
                int indexOfT = dtstart.indexOf('T');
                hour = Integer.parseInt(dtstart.substring(indexOfT + 1, indexOfT + 3));
                minute = Integer.parseInt(dtstart.substring(indexOfT + 3, indexOfT + 5));
                second = Integer.parseInt(dtstart.substring(indexOfT + 5, indexOfT + 7));
            } catch (StringIndexOutOfBoundsException se) {
                hour = minute = second = 0;
            } catch (NumberFormatException ne) {
                hour = minute = second  = 0;
            }
        }

        return new SimpleOnset(week, dayOfWeek, month, dayOfMonth,
                               hour, minute, second);
    }

    // maps Java weekday number to iCalendar weekday name
    private static String sDayOfWeekNames[] = new String[Calendar.SATURDAY + 1];
    static {
        sDayOfWeekNames[0] = "XX";  // unused
        sDayOfWeekNames[Calendar.SUNDAY]    = "SU";  // 1
        sDayOfWeekNames[Calendar.MONDAY]    = "MO";  // 2
        sDayOfWeekNames[Calendar.TUESDAY]   = "TU";  // 3
        sDayOfWeekNames[Calendar.WEDNESDAY] = "WE";  // 4
        sDayOfWeekNames[Calendar.THURSDAY]  = "TH";  // 5
        sDayOfWeekNames[Calendar.FRIDAY]    = "FR";  // 6
        sDayOfWeekNames[Calendar.SATURDAY]  = "SA";  // 7
    }

    // maps iCalendar weekday name to Java weekday number
    private static Map /*<String, Integer>*/ sDayOfWeekMap = new HashMap(7);
    static {
        sDayOfWeekMap.put("SU", new Integer(Calendar.SUNDAY));     // 1
        sDayOfWeekMap.put("MO", new Integer(Calendar.MONDAY));     // 2
        sDayOfWeekMap.put("TU", new Integer(Calendar.TUESDAY));    // 3
        sDayOfWeekMap.put("WE", new Integer(Calendar.WEDNESDAY));  // 4
        sDayOfWeekMap.put("TH", new Integer(Calendar.THURSDAY));   // 5
        sDayOfWeekMap.put("FR", new Integer(Calendar.FRIDAY));     // 6
        sDayOfWeekMap.put("SA", new Integer(Calendar.SATURDAY));   // 7
    }
    
    /**
     * Return the standard offset in milliseconds.
     * local = UTC + offset
     */
    public int getStandardOffset() {
        return mStandardOffset;
    }

    /**
     * Return the onset rule/time for transitioning from daylight to standard
     * time.  Null is returned if DST is not in use.
     */
    public SimpleOnset getStandardOnset() {
        return mStandardOnset;
    }

    /**
     * Return the daylight offset in milliseconds.
     * Value is same as standard offset is DST is not used.
     * local = UTC + offset
     */
    public int getDaylightOffset() {
        return mDaylightOffset;
    }

    /**
     * Return the onset rule/time for transitioning from standard to daylight
     * time.  Null is returned if DST is not in use.
     */
    public SimpleOnset getDaylightOnset() {
        return mDaylightOnset;
    }

    /**
     * 
     * @param tzId       iCal TZID string
     * @param stdOffset  standard time offset from UTC in milliseconds
     * @param stdDtStart iCal datetime string specifying the beginning of the
     *                   period for which stdRRule applies.  The format is
     *                   "YYYYMMDDThhmmss" with 24-hour hour.  In practice,
     *                   the date portion is set to some very early date, like
     *                   "16010101", and only the time portion varies according
     *                   to the rules of the time zone.
     * @param stdRRule   iCal recurrence rule for transition into standard
     *                   time (i.e. transition out of daylight time)
     *                   e.g. "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=10;BYDAY=-1SU"
     * @param dayOffset  daylight time offset from UTC in milliseconds
     * @param dayDtStart iCal datetime string specifying the beginning of the
     *                   period for which dayRRUle applies
     * @param dayRRule   iCal recurrence rule for transition into daylight
     *                   time
     */
    public ICalTimeZone(String tzId,
                    int stdOffset, String stdDtStart, String stdRRule,
                    int dayOffset, String dayDtStart, String dayRRule) {
        super(0, tzId);
        mHasDaylight = stdOffset != dayOffset;
        mStandardOffset = stdOffset;
        if (stdDtStart != null)
            mDayToStdDtStart = stdDtStart;
        mDayToStdRule = stdRRule;
        mDaylightOffset = dayOffset;
        if (dayDtStart != null)
            mStdToDayDtStart = dayDtStart;
        else
            mStdToDayDtStart = mDayToStdDtStart;
        mStdToDayRule = dayRRule;
        initFromICalData();
    }

    public ICalTimeZone(String tzId,
                        int standardOffset, SimpleOnset standardOnset,
                        int daylightOffset, SimpleOnset daylightOnset) {
        super(0, tzId);
        mStandardOffset = standardOffset;
        mDaylightOffset = daylightOffset;
        setRawOffset(mStandardOffset);
        if (mDaylightOffset != mStandardOffset &&
            standardOnset != null && daylightOnset != null) {
            mHasDaylight = true;
            mDayToStdDtStart = toICalDtStart(standardOnset);
            mDayToStdRule    = toICalRRule(standardOnset);
            mStdToDayDtStart = toICalDtStart(daylightOnset);
            mStdToDayRule    = toICalRRule(daylightOnset);
            mStandardOnset  = standardOnset;
            mDaylightOnset  = daylightOnset;

            SimpleTimeZoneRule stzDaylight =
                new SimpleTimeZoneRule(daylightOnset);
            setStartRule(stzDaylight.mMonth,
                         stzDaylight.mDayOfMonth,
                         stzDaylight.mDayOfWeek,
                         stzDaylight.mDtStartMillis);
            SimpleTimeZoneRule stzStandard =
                new SimpleTimeZoneRule(standardOnset);
            setEndRule(stzStandard.mMonth,
                       stzStandard.mDayOfMonth,
                       stzStandard.mDayOfWeek,
                       stzStandard.mDtStartMillis);
            setDSTSavings(mDaylightOffset - mStandardOffset);
        }
    }

    

//    public ICalTimeZone(String tzId,
//                        int stdOffset, String stdDtStart, String stdRRule,
//                        int dayOffset, String dayDtStart, String dayRRule) {
//        super(tzId,
//              stdOffset, stdDtStart, stdRRule,
//              dayOffset, dayDtStart, dayRRule);
//        mTzId = tzId;
//    }
//
//    public ICalTimeZone(String tzId,
//                        int standardOffset, SimpleOnset standardOnset,
//                        int daylightOffset, SimpleOnset daylightOnset) {
//        super(tzId,
//              standardOffset, standardOnset, daylightOffset, daylightOnset);
//        mTzId = tzId;
//    }
    
    private static String toICalDtStart(SimpleOnset onset) {
        String hourStr = Integer.toString(onset.getHour() + 100).substring(1);
        String minuteStr = Integer.toString(onset.getMinute() + 100).substring(1);
        String secondStr = Integer.toString(onset.getSecond() + 100).substring(1);
        StringBuffer sb = new StringBuffer("16010101T");
        sb.append(hourStr).append(minuteStr).append(secondStr);
        return sb.toString();
    }

    private static String toICalRRule(SimpleOnset onset) {
        if (!onset.hasRule()) return null;
        StringBuffer sb =
            new StringBuffer("FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=");
        sb.append(onset.getMonth()).append(";BYDAY=");
        sb.append(onset.getWeek()).append(sDayOfWeekNames[onset.getDayOfWeek()]);
        return sb.toString();
    }


// test main

    public static void main(String[] args) throws Exception {
        boolean pass = SelfTest.doit();
        if (!pass)
            System.exit(1);
    }

    private static class SelfTest {
        public static boolean doit() throws Exception {
            int badCount = 0;

            ICalTimeZone mytz = new ICalTimeZone(
                    "Custom TZ",
                    -28800000, "16010101T020000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=10;BYDAY=-3FR",
                    -25200000, "16010101T020000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=4;BYDAY=2TU");
            if (!verifyTZRules(mytz, 2005))
                badCount++;
            System.out.println();

            List tzList = Provisioning.getInstance().getAllTimeZones();
            for (Iterator iter = tzList.iterator(); iter.hasNext(); ) {
                WellKnownTimeZone wktz = (WellKnownTimeZone) iter.next();
                ICalTimeZone tz = wktz.toTimeZone();
                boolean good = verifyTZRules(tz, 2005);
                if (!good)
                    badCount++;
                System.out.println();
            }
            if (badCount == 0)
                System.out.println("All TZs are working correctly.");
            if (badCount > 0)
                System.out.println("There were " + badCount + " bad TZs.");

            return badCount == 0;
        }

        private static String offsetToHHMM(int offsetMillis) {
            offsetMillis /= 1000 * 60;  // offset in minutes now
            String sign = offsetMillis < 0 ? "-" : "+";
            offsetMillis = Math.abs(offsetMillis);
            int offsetHours = offsetMillis / 60;
            String offsetHoursStr = Integer.toString(offsetHours);
            if (offsetHours < 10)
                offsetHoursStr = "0" + offsetHoursStr;
            int offsetMins = offsetMillis % 60;
            String offsetMinsStr = Integer.toString(offsetMins);
            if (offsetMins < 10)
                offsetMinsStr = "0" + offsetMinsStr;
            return sign + offsetHoursStr + ":" + offsetMinsStr;
        }

        private static String toTimestamp(Calendar cal) {
            Calendar oneHourAgo = (Calendar) cal.clone();
            oneHourAgo.add(Calendar.HOUR_OF_DAY, -1);
            int hour = oneHourAgo.get(Calendar.HOUR_OF_DAY);
            hour++;  // Assume DST offset is always 1 hour.
            if (hour == 24)
                hour = 0;

            Calendar yesterday = (Calendar) cal.clone();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            Calendar tomorrow = (Calendar) cal.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            long beforeOffset = yesterday.getTimeZone().getOffset(yesterday.getTimeInMillis());
            long afterOffset = tomorrow.getTimeZone().getOffset(tomorrow.getTimeInMillis());

            int year = cal.get(Calendar.YEAR); 
            int month = cal.get(Calendar.MONTH); 
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);

            month += 1;
            String monthStr = Integer.toString(month);
            if (month < 10)
                monthStr = "0" + monthStr;
            String dayStr = Integer.toString(day);
            if (day < 10)
                dayStr = "0" + dayStr;
            String hourStr = Integer.toString(hour);
            if (hour < 10)
                hourStr = "0" + hourStr;
            String minuteStr = Integer.toString(minute);
            if (minute < 10)
                minuteStr = "0" + minuteStr;
            String secondStr = Integer.toString(second);
            if (second < 10)
                secondStr = "0" + secondStr;

            StringBuffer sb = new StringBuffer();
            sb.append(year).append("/");
            sb.append(monthStr).append("/");
            sb.append(dayStr).append(" T");
            sb.append(hourStr).append(minuteStr).append(secondStr);
            if (beforeOffset < afterOffset)
                sb.append(" - to daylight");
            else if (beforeOffset > afterOffset)
                sb.append(" - to standard");
            return sb.toString();
        }

        public static boolean verifyTZRules(ICalTimeZone tz, int year) {
            boolean goodOrBad = true;
            System.out.println("Verifying DST rules for time zone " + tz.getID());
            String tzStr = tz.toString();

            String defStandardTime = null;
            String defDaylightTime = null;
            String actualStandardTime = null;
            String actualDaylightTime = null;

            Calendar onsets[] = findOnsetDates(tz, year);
            for (int i = 0; i < onsets.length; i++) {
            	System.out.print("  Onset " + (i + 1) + ": ");
                if (onsets[i] == null) {
                	System.out.println("null");
                    continue;
                }
                String tstamp = toTimestamp(onsets[i]);
                System.out.println(tstamp);
                if (tstamp.indexOf("to standard") != -1)
                	actualStandardTime = parseHHMMSS(tstamp);
                else
                    actualDaylightTime = parseHHMMSS(tstamp);
            }
            if (!tz.useDaylightTime()) {
            	if (onsets[0] != null || onsets[1] != null) {
            		System.out.println("ERROR: Onset dates present in a non-DST TZ.");
                    goodOrBad = false;
                }
                System.out.println("  DST supported:   " + tz.mHasDaylight);
                System.out.println("  toString:\n" + tz.toString());
            } else {
            	if (onsets[0] == null || onsets[1] == null) {
            		System.out.println("ERROR: Not enough onset dates present in a DST TZ.");
                    goodOrBad = false;
                }
                // Dump TZ detail.
                System.out.println("  DST supported:   " + tz.mHasDaylight);
                System.out.println("  Daylight Offset: " + offsetToHHMM(tz.mDaylightOffset));
                System.out.println("  Daylight Start:  " + tz.mStdToDayDtStart);
                System.out.println("  Daylight RRule:  " + tz.mStdToDayRule);
                System.out.println("  Standard Offset: " + offsetToHHMM(tz.mStandardOffset));
                System.out.println("  Standard Start:  " + tz.mDayToStdDtStart);
                System.out.println("  Standard RRule:  " + tz.mDayToStdRule);
                System.out.println("  toString:\n" + tz.toString());

                defStandardTime = parseHHMMSS(tz.mDayToStdDtStart);
                defDaylightTime = parseHHMMSS(tz.mStdToDayDtStart);
                if (!defStandardTime.equals(actualStandardTime) ||
                    !defDaylightTime.equals(actualDaylightTime)) {
                    System.out.println("ERROR: Onset times don't match between definition and actual.");
                    System.out.println("defStandardTime    = " + defStandardTime);
                    System.out.println("actualStandardTime = " + actualStandardTime);
                    System.out.println("defDaylightTime    = " + defDaylightTime);
                    System.out.println("actualDaylightTime = " + actualDaylightTime);
                	goodOrBad = false;
                }
            }

            ICalTimeZone onsetClone = cloneFromSimpleOnset(tz);
            String onsetCloneStr = onsetClone.toString();
            if (!onsetClone.hasSameRules(tz)) {
                System.out.println("ERROR: Onset-clone doesn't have the same rules as original.");
                System.out.println("Onset-clone:\n" + onsetCloneStr);
                goodOrBad = false;
            }

            ICalTimeZone icalClone = cloneFromICalData(onsetClone);
            String icalCloneStr = icalClone.toString(); 
            if (!icalClone.hasSameRules(onsetClone)) {
                System.out.println("ERROR: iCal-clone doesn't have the same rules as Onset-clone.");
                System.out.println("iCal-clone:\n" + icalCloneStr);
                goodOrBad = false;
            }

            if (!tzStr.equals(onsetCloneStr)) {
                System.out.println("ERROR: Onset-clone's toString is different than original.");
                goodOrBad = false;
            }
            if (!tzStr.equals(icalCloneStr)) {
                System.out.println("ERROR: iCal-clone's toString is different than original.");
                goodOrBad = false;
            }

            return goodOrBad;
        }

        private static ICalTimeZone cloneFromSimpleOnset(ICalTimeZone tz) {
            ICalTimeZone newtz = new ICalTimeZone(
                    tz.getID(),
                    tz.getStandardOffset(), tz.getStandardOnset(),
                    tz.getDaylightOffset(), tz.getDaylightOnset());
            return newtz;
        }

        private static ICalTimeZone cloneFromICalData(ICalTimeZone tz) {
            ICalTimeZone newtz = new ICalTimeZone(
                    tz.getID(),
                    tz.getStandardOffset(), tz.mDayToStdDtStart, tz.mDayToStdRule,
                    tz.getDaylightOffset(), tz.mStdToDayDtStart, tz.mStdToDayRule);
            return newtz;
        }

        private static String parseHHMMSS(String text) {
        	int indexT = text.indexOf('T');
            if (indexT == -1)
                return null;
            String time = text.substring(indexT);
            int length = 1;
            if (time.length() < 7)
                return null;
            return time.substring(0, 7);
        }

        /**
         * Returns a two-element array of daylight/standard time onset
         * datetime for given time zone and year.  If onset dates are
         * not found (e.g. time zone doesn't use daylight savings time)
         * a non-null array is still returned but one or both elements
         * may be null.
         * @param tz
         * @param year
         * @return
         */
        public static Calendar[] findOnsetDates(ICalTimeZone tz, int year) {
            Calendar onsets[] = new Calendar[2];
        	Calendar cal = new GregorianCalendar(tz);
            cal.clear();
            cal.set(year, Calendar.JANUARY, 1, 12, 0, 0);  // noon on January 1st
            cal.set(Calendar.MILLISECOND, 0);
            // We rely on the fact onset times for all known time zones
            // are early in the AM, well before noon.

            onsets[0] = findOnsetDate(tz, cal, tz.getOffset(cal.getTimeInMillis()));
            if (onsets[0] != null) {
                Calendar firstDate = (Calendar) onsets[0].clone();
                firstDate.set(Calendar.HOUR_OF_DAY, 12);
                firstDate.set(Calendar.MINUTE, 0);
                firstDate.set(Calendar.SECOND, 0);
                firstDate.set(Calendar.MILLISECOND, 0);
                firstDate.add(Calendar.DAY_OF_YEAR, 1);
                onsets[1] = findOnsetDate(tz, firstDate, tz.getOffset(firstDate.getTimeInMillis()));
            }
            return onsets;
        }

        /**
         * Find the first onset date after the startDate and within the same
         * year.  The time fields of the returned onset date is the precise
         * onset time.  Null is returned if no onset date/time is found.
         * @param tz
         * @param startDate
         * @return
         */
        private static Calendar findOnsetDate(ICalTimeZone tz,
                                              Calendar startDate,
                                              long beforeOffset) {
            int startYear = startDate.get(Calendar.YEAR);
            Calendar onsetDate = (Calendar) startDate.clone();
            int incBy = INCBY_MONTH;
            while (true) {
                int year = onsetDate.get(Calendar.YEAR);
                if (year > startYear)
                    return null;
                long offset = tz.getOffset(onsetDate.getTimeInMillis());
                if (offset != beforeOffset) {
                    if (incBy == INCBY_MONTH) {
                        onsetDate.add(Calendar.MONTH, -1);
                        incBy = INCBY_DAY;
                    } else {
                        onsetDate = findOnsetTime(tz, onsetDate, beforeOffset);
                    	break;
                    }
                }

                if (incBy == INCBY_MONTH)
                    onsetDate.add(Calendar.MONTH, 1);
                else
                    onsetDate.add(Calendar.DAY_OF_YEAR, 1);
            }
            return onsetDate;
        }

        private static final int INCBY_MONTH = 0;
        private static final int INCBY_DAY = 1;
        private static final int INCBY_HOUR = 2;
        private static final int INCBY_MINUTE = 3;

        /**
         * Find the onset time within the given date.  We go back to 23:59:30
         * of previous day and scan forward computing the offset each hour.
         * Once the onset hour is identified, the hour interval is rescanned
         * at 15 minute interval, resulting in onset quarter-hour.  The
         * quarter-hour is scanned for each minute to find the precise minute
         * of onset.
         * @param tz
         * @param onsetDate
         * @return
         */
        private static Calendar findOnsetTime(ICalTimeZone tz,
                                              Calendar onsetDate,
                                              long beforeOffset) {
            String offsetStr = offsetToHHMM((int) beforeOffset);
            Calendar lowerBound = (Calendar) onsetDate.clone();
            lowerBound.set(Calendar.HOUR_OF_DAY, 0);
            lowerBound.set(Calendar.MINUTE, 0);
            lowerBound.set(Calendar.SECOND, 0);
            lowerBound.set(Calendar.MILLISECOND, 0);

            long lowerBoundMillis = lowerBound.getTimeInMillis();
            long upperBoundMillis = onsetDate.getTimeInMillis();
            Calendar onsetTime = (Calendar) lowerBound.clone();
            int incBy = INCBY_HOUR;
            while (true) {
                long millis = onsetTime.getTimeInMillis();
                if (millis >= upperBoundMillis)
                    return null;

                int offset = tz.getOffset(millis);
                if (offset != beforeOffset) {
                    if (millis == lowerBoundMillis)
                        break;
                    if (incBy == INCBY_HOUR) {
                        onsetTime.add(Calendar.HOUR_OF_DAY, -1);
                        incBy = INCBY_MINUTE;
                    } else {
                        // Finally got our answer!
                        break;
                    }
                }

                if (incBy == INCBY_HOUR)
                    onsetTime.add(Calendar.HOUR_OF_DAY, 1);
                else
                    onsetTime.add(Calendar.MINUTE, 1);
            }

            return onsetTime;
        }
    }
    
    private static final long MSEC_PER_HOUR = 1000 * 60 * 60;
    private static final long MSEC_PER_MIN = 1000 * 60;
    private static final long MSEC_PER_SEC = 1000;    
    
    /**
     * Input: TZOFFSETTO: [+-]HHMM(SS)?
     * Output: msec offset from GMT
     * @param utcOffset
     * @return
     */
    private static int tzOffsetToTime(String utcOffset)
    throws ServiceException {
        int len = utcOffset != null ? utcOffset.length() : 0;
        if (len != 5 && len != 7)
            throw ServiceException.INVALID_REQUEST(
                    "Invalid " +
                    ICalTok.TZOFFSETFROM + "/" + ICalTok.TZOFFSETTO +
                    " value \"" + utcOffset +
                    "\"; must have format \"+/-hhmm[ss]\"", null);

        int toRet = 0;
        try {
            toRet += (Integer.parseInt(utcOffset.substring(1,3)) * MSEC_PER_HOUR);
            toRet += (Integer.parseInt(utcOffset.substring(3,5)) * MSEC_PER_MIN);
            if (len == 7) {
                toRet += (Integer.parseInt(utcOffset.substring(5,7)) * MSEC_PER_SEC);
            }
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST(
                    "Invalid " +
                    ICalTok.TZOFFSETFROM + "/" + ICalTok.TZOFFSETTO +
                    " value \"" + utcOffset +
                    "\"; must have format \"+/-hhmm[ss]\"", e);
        }
        if (utcOffset.charAt(0) == '-') {
            toRet *= -1;
        }
        return toRet;
    }
    
    /**
     * Input: msec GMT
     * Output: TZOFFSETTO: [+-]HHMM(SS)?
     * @param utcOffset
     * @return
     */
    static String timeToTzOffsetString(int time)
    {
       StringBuffer toRet = new StringBuffer(time > 0 ? "+" : "-");
       
       time = Math.abs(time / 1000); // msecs->secs
       
       int secs = time % 60;
       time = time / 60;
       
       int mins = time % 60;
       int hours = time / 60;
       
       if (secs > 0) { 
           toRet.append(new Formatter().format("%02d%02d%02d", hours, mins, secs));
       } else {
           toRet.append(new Formatter().format("%02d%02d", hours, mins));
       }
       return toRet.toString();
    }
    
    public ZComponent newToVTimeZone()
    {
        ZComponent vtz = new ZComponent(ICalTok.VTIMEZONE);
        vtz.addProperty(new ZProperty(ICalTok.TZID, getID()));
        
        if (mDayToStdDtStart != null) {
            ZComponent standard = new ZComponent(ICalTok.STANDARD);
            vtz.addComponent(standard);
            
            standard.addProperty(new ZProperty(ICalTok.DTSTART, mDayToStdDtStart));
            standard.addProperty(new ZProperty(ICalTok.TZOFFSETTO, timeToTzOffsetString(mStandardOffset)));
            standard.addProperty(new ZProperty(ICalTok.TZOFFSETFROM, timeToTzOffsetString(mDaylightOffset)));
            if (mDayToStdRule != null)
	            standard.addProperty(new ZProperty(ICalTok.RRULE, mDayToStdRule));
        }

        if (mStdToDayDtStart != null) {
            ZComponent daylight = new ZComponent(ICalTok.DAYLIGHT);
            vtz.addComponent(daylight);
            
            daylight.addProperty(new ZProperty(ICalTok.DTSTART, mStdToDayDtStart));
            daylight.addProperty(new ZProperty(ICalTok.TZOFFSETTO, timeToTzOffsetString(mDaylightOffset)));
            daylight.addProperty(new ZProperty(ICalTok.TZOFFSETFROM, timeToTzOffsetString(mStandardOffset)));
            if (mStdToDayRule != null)
	            daylight.addProperty(new ZProperty(ICalTok.RRULE, mStdToDayRule));
        }

        return vtz;
    }

    /**
     * Return the time zone component (STANDARD and DAYLIGHT) with the more
     * recent DTSTART value.  If DTSTART values are identical, tzComp2 is
     * returned.  Null DTSTART value is considered earlier than any non-null
     * value.
     * @param tzComp1
     * @param tzComp2
     * @return
     */
    private static ZComponent moreRecentTzComp(ZComponent tzComp1,
                                               ZComponent tzComp2) {
        String dtStart1 = tzComp1.getPropVal(ICalTok.DTSTART, null);
        String dtStart2 = tzComp2.getPropVal(ICalTok.DTSTART, null);
        if (dtStart1 == null) return tzComp2;
        if (dtStart2 == null) return tzComp1;
        // String comparison works because the DTSTART strings have the format
        // "yyyymmddThhmiss".
        if (dtStart2.compareToIgnoreCase(dtStart1) >= 0)
            return tzComp2;
        else
            return tzComp1;
    }

    public static ICalTimeZone fromVTimeZone(ZComponent comp)
    throws ServiceException {
        String tzname = comp.getPropVal(ICalTok.TZID, null);

        ZComponent standard = null;
        ZComponent daylight = null;

        // Find the most recent STANDARD and DAYLIGHT components.  "Most recent"
        // means the component's DTSTART is later in time than that of all other
        // components.  Thus, if multiple STANDARD components are specifieid in
        // a VTIMEZONE, we end up using only the most recent definition.  The
        // assumption is that all other STANDARD components are for past dates
        // and they no longer matter.  We're forced to make this assumption
        // because this class is a subclass of SimpleTimeZone, which doesn't
        // allow historical information.
        for (Iterator<ZComponent> iter = comp.getComponentIterator();
             iter.hasNext(); ) {
            ZComponent tzComp = iter.next();
            if (tzComp == null) continue;
            ICalTok tok = tzComp.getTok();
            if (ICalTok.STANDARD.equals(tok)) {
                if (standard == null) {
                    standard = tzComp;
                    continue;
                } else
                    standard = moreRecentTzComp(standard, tzComp);
            } else if (ICalTok.DAYLIGHT.equals(tok)) {
                if (daylight == null) {
                    daylight = tzComp;
                    continue;
                } else
                    daylight = moreRecentTzComp(daylight, tzComp);
            }
        }

        // If both STANDARD and DAYLIGHT have no RRULE and their DTSTART has
        // the same month and date, they have the same onset date.  Discard
        // the older one.  (This happened with Asia/Singapore TZ definition
        // created by Apple iCal; see bug 7335.)
        if (standard != null && daylight != null) {
            String stdRule = standard.getPropVal(ICalTok.RRULE, null);
            String dayRule = daylight.getPropVal(ICalTok.RRULE, null);
            // The rules should be either both non-null or both null.
            // If only one is null then the VTIMEZONE is invalid, but we'll be
            // lenient and treat it as if both were null.
            if (stdRule == null || dayRule == null) {
                String stdStart = standard.getPropVal(ICalTok.DTSTART, null);
                String dayStart = daylight.getPropVal(ICalTok.DTSTART, null);
                if (stdStart != null && dayStart != null) {
                    try {
                        // stdStart = yyyymmddThhmiss
                        // stdStartMMDD = mmdd
                        String stdStartMMDD = stdStart.substring(4, 8);
                        String dayStartMMDD = dayStart.substring(4, 8);
                        if (stdStartMMDD.equals(dayStartMMDD)) {
                            standard = moreRecentTzComp(standard, daylight);
                            daylight = null;
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        // DTSTART values must have been malformed.  Just do
                        // something reasonable and go on.
                        standard = moreRecentTzComp(standard, daylight);
                        daylight = null;
                    }
                }
            }
        }

        // If only DAYLIGHT is given, make it the STANDARD.
        if (standard == null) {
        	standard = daylight;
        	daylight = null;
        }
        if (standard == null)
        	throw new IllegalArgumentException("VTIMEZONE has neither STANDARD nor DAYLIGHT: TZID=" + tzname);
        
        String stddtStart = null;
        int stdoffsetTime = 0;
        String stdrrule = null;
        
        if (standard != null) {
            stddtStart = standard.getPropVal(ICalTok.DTSTART, null);
            String stdtzOffsetTo = standard.getPropVal(ICalTok.TZOFFSETTO, null);
            stdoffsetTime = tzOffsetToTime(stdtzOffsetTo);
            if (daylight != null) {
                // Rule is interesting only if daylight savings is in use.
                stdrrule = standard.getPropVal(ICalTok.RRULE, null);
            }
        }
        
        String daydtStart = null;
        int dayoffsetTime = stdoffsetTime;
        String dayrrule = null;
        
        if (daylight != null) {
            daydtStart = daylight.getPropVal(ICalTok.DTSTART, null);
            String daytzOffsetTo = daylight.getPropVal(ICalTok.TZOFFSETTO, null);
            dayoffsetTime = tzOffsetToTime(daytzOffsetTo);  
            dayrrule = daylight.getPropVal(ICalTok.RRULE, null);
        }
        
        ICalTimeZone tz = new ICalTimeZone(tzname, 
                stdoffsetTime, stddtStart, stdrrule,
                dayoffsetTime, daydtStart, dayrrule);

        return tz;
    }
}
