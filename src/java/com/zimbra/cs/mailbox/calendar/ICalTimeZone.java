/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;

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
public class ICalTimeZone extends SimpleTimeZone {

    private static final long serialVersionUID = 1L;

    public static class SimpleOnset implements Comparable<SimpleOnset> {
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

        public SimpleOnset(int week, int dayOfWeek, int month, int dayOfMonth,
                int hour, int minute, int second) {
            this(week, dayOfWeek, month, dayOfMonth, hour, minute, second, false);
        }

        public SimpleOnset(int week, int dayOfWeek, int month, int dayOfMonth,
                           int hour, int minute, int second,
                           boolean skipBYMONTHDAYFixup) {
            mWeek = week;
            mDayOfWeek = dayOfWeek;
            mMonth = month;
            mDayOfMonth = dayOfMonth;
            mHour = hour;
            mMinute = minute;
            mSecond = second;

            if (!skipBYMONTHDAYFixup)
                applyBYMONTHDAYFixup();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("week=").append(mWeek);
            sb.append(", dayOfWeek=").append(mDayOfWeek);
            sb.append(", month=").append(mMonth);
            sb.append(", dayOfMonth=").append(mDayOfMonth);
            sb.append(", hour=").append(mHour);
            sb.append(", minute=").append(mMinute);
            sb.append(", second=").append(mSecond);
            return sb.toString();
        }

        /**
         * If rule is specified as a day of month rather than the combination
         * of week number and weekday, convert to week number/day style.
         * The year of current system time is used in the conversion.
         *
         */
        private void applyBYMONTHDAYFixup() {
            // already using week number/day style
            if (mWeek != 0 && mDayOfWeek != 0)
                return;

            if (mDayOfMonth != 0) {
                Calendar cal = new GregorianCalendar();
                int currentYear = cal.get(Calendar.YEAR);
                int month = mMonth - 1;
                cal.clear();
                cal.set(currentYear, month, mDayOfMonth);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                int week = ((mDayOfMonth - 1) / 7) + 1;
                // This was the last week if adding 7 days pus us in the next
                // month.
                cal.add(Calendar.DAY_OF_MONTH, 7);
                if (cal.get(Calendar.MONTH) > month)
                    week = -1;

                mWeek = week;
                mDayOfWeek = dayOfWeek;
                mDayOfMonth = 0;
            }
        }

        public int compareTo(SimpleOnset other) {
            int comp;
            comp = mMonth - other.mMonth;
            if (comp != 0) return comp;
            comp = mWeek - other.mWeek;
            if (comp != 0) return comp;
            if (mWeek != 0)
                comp = mDayOfWeek - other.mDayOfWeek;
            else
                comp = mDayOfMonth - other.mDayOfMonth;
            if (comp != 0) return comp;
            comp = mHour - other.mHour;
            if (comp != 0) return comp;
            comp = mMinute - other.mMinute;
            if (comp != 0) return comp;
            comp = mSecond - other.mSecond;
            return comp;
        }
    }

    private static final String DEFAULT_DTSTART = "19710101T000000";

    private int    mStandardOffset = 0;
    private String mDayToStdDtStart = DEFAULT_DTSTART;
    private String mDayToStdRule = null;
    private String mStandardTzname = null;

    private int    mDaylightOffset = 0; 
    private String mStdToDayDtStart = DEFAULT_DTSTART;
    private String mStdToDayRule = null;
    private String mDaylightTzname = null;

    private SimpleOnset mStandardOnset;
    private SimpleOnset mDaylightOnset;

    public ICalTimeZone cloneWithNewTZID(String tzid) {
        ICalTimeZone cloneTZ = new ICalTimeZone(
                tzid,
                mStandardOffset, mDayToStdDtStart, mDayToStdRule, mStandardTzname,
                mDaylightOffset, mStdToDayDtStart, mStdToDayRule, mDaylightTzname);
        return cloneTZ;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TZID=").append(getID());
        sb.append("\nSimpleTimeZone: ").append(super.toString());
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


    static final String FN_TZID = "tzid";
    private static final String FN_STANDARD_OFFSET  = "so";
    private static final String FN_DAYLIGHT_OFFSET  = "do";
    private static final String FN_DAYTOSTD_DTSTART = "d2ss";
    private static final String FN_STDTODAY_DTSTART = "s2ds";
    private static final String FN_DAYTOSTD_RULE    = "d2sr";
    private static final String FN_STDTODAY_RULE    = "s2dr";
    private static final String FN_STANDARD_TZNAME  = "sn";
    private static final String FN_DAYLIGHT_TZNAME  = "dn";

    private static ICalTimeZone sUTC = new ICalTimeZone("Z",
                                                        0, DEFAULT_DTSTART, null, "UTC",
                                                        0, DEFAULT_DTSTART, null, "UTC");

//    private String mTzId = null;

    /**
     * Returns the time zone for the given account.
     */
    public static ICalTimeZone getAccountTimeZone(Account account) {
        String tzid = account.getAttr(Provisioning.A_zimbraPrefTimeZoneId);
        tzid = TZIDMapper.canonicalize(tzid);
        ICalTimeZone timeZone = WellKnownTimeZones.getTimeZoneById(tzid);
        if (timeZone == null) {
            return sUTC;
        }
        return timeZone;
    }
    
    public static ICalTimeZone getUTC() { return sUTC; }

    public Metadata encodeAsMetadata() {
        Metadata meta = new Metadata();
        String tzid = getID();
        meta.put(FN_TZID, tzid);
        // For well-known time zone we only need the TZID.
        if (lookupByTZID(tzid) != null)
            return meta;

        meta.put(FN_STANDARD_OFFSET, mStandardOffset);
        meta.put(FN_DAYTOSTD_DTSTART, mDayToStdDtStart); 
        meta.put(FN_DAYTOSTD_RULE, mDayToStdRule);
        meta.put(FN_STANDARD_TZNAME, mStandardTzname);

        meta.put(FN_DAYLIGHT_OFFSET, mDaylightOffset);
        meta.put(FN_STDTODAY_DTSTART, mStdToDayDtStart);  
        meta.put(FN_STDTODAY_RULE, mStdToDayRule);
        meta.put(FN_DAYLIGHT_TZNAME, mDaylightTzname);
        return meta;
    }

    public String getStandardDtStart() { return mDayToStdDtStart; }
    public String getStandardRule()    { return mDayToStdRule; }
    public String getStandardTzname()  { return mStandardTzname; }
    public String getDaylightDtStart() { return mStdToDayDtStart; }
    public String getDaylightRule()    { return mStdToDayRule; }
    public String getDaylightTzname()  { return mDaylightTzname; }
    public boolean sameAsUTC()         { return mStandardOffset == 0 && mDaylightOffset == 0; }

    public static ICalTimeZone decodeFromMetadata(Metadata m) throws ServiceException {
        String tzid;
        if (m.containsKey(FN_TZID)) {
            tzid = m.get(FN_TZID);
            boolean hasDef = m.containsKey(FN_STANDARD_OFFSET);
            if (!DebugConfig.disableCalendarTZMatchByID || !hasDef) {
                ICalTimeZone tz = WellKnownTimeZones.getTimeZoneById(tzid);
                if (tz != null) {
                    return tz;
                } else if (!hasDef) {
                    ZimbraLog.calendar.debug("Unknown time zone \"" + tzid + "\" in metadata; using UTC instead");
                    return sUTC.cloneWithNewTZID(tzid);
                }
            }
        } else
            tzid = "unknown time zone";
        ICalTimeZone newTz = new ICalTimeZone(tzid, m);
        ICalTimeZone tz = lookupByRule(newTz, false);
        return tz;
    }
    
    private ICalTimeZone(String tzId, Metadata meta) throws ServiceException {
        super(0, tzId);

        mStandardOffset = (int) meta.getLong(FN_STANDARD_OFFSET, 0);
        mDayToStdDtStart = meta.get(FN_DAYTOSTD_DTSTART, null);
        mDayToStdRule = meta.get(FN_DAYTOSTD_RULE, null);
        mStandardTzname = meta.get(FN_STANDARD_TZNAME, null);

        mDaylightOffset = (int) meta.getLong(FN_DAYLIGHT_OFFSET, mStandardOffset);
        mStdToDayDtStart = meta.get(FN_STDTODAY_DTSTART, mDayToStdDtStart);
        mStdToDayRule = meta.get(FN_STDTODAY_RULE, null);
        mDaylightTzname = meta.get(FN_DAYLIGHT_TZNAME, null);

        initFromICalData(true);
    }

    private void initFromICalData(boolean fromMetadata) {
        int dstSavings = mDaylightOffset - mStandardOffset;
        if (dstSavings < 0) {
            // Must be an error in the TZ definition.  Swap the offsets.
            // (Saw this with Windows TZ for Windhoek, Namibia)
            int tmp = mStandardOffset;
            mStandardOffset = mDaylightOffset;
            mDaylightOffset = tmp;
            dstSavings *= -1;
        }
        setRawOffset(mStandardOffset);
        if (dstSavings != 0) {
            mStandardOnset = parseOnset(mDayToStdRule, mDayToStdDtStart, fromMetadata);
            mDaylightOnset = parseOnset(mStdToDayRule, mStdToDayDtStart, fromMetadata);

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
            setDSTSavings(dstSavings);
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
            int week = onset.getWeek();
            if (week != 0) {
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

    public static SimpleOnset parseOnset(String rrule, String dtstart, boolean fromMetadata) {
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
                    try {
                        month = Integer.parseInt(t.nextToken());
                    } catch (NumberFormatException ne) {}
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

                    Integer day = sDayOfWeekMap.get(value);
                    if (day == null)
                        throw new IllegalArgumentException("Invalid day of week value: " + value);
                    dayOfWeek = day.intValue();
                } else if ("BYMONTHDAY".equals(token)) {
                    try {
                        dayOfMonth = Integer.parseInt(t.nextToken());
                    } catch (NumberFormatException ne) {}
                } else {
                    t.nextToken();  // skip value of unused param
                }
            }
        } else {
            // No RRULE provided.  Get month and day of month from DTSTART.
            week = 0;
            month = dayOfMonth = 1;
            if (dtstart != null) {
                try {
                    month = Integer.parseInt(dtstart.substring(4, 6));
                    dayOfMonth = Integer.parseInt(dtstart.substring(6, 8));
                } catch (StringIndexOutOfBoundsException se) {
                } catch (NumberFormatException ne) {}
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
                               hour, minute, second, fromMetadata);
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
    private static Map<String, Integer> sDayOfWeekMap =
        new HashMap<String, Integer>(7);
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
     *                   "19710101", and only the time portion varies according
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
    public static ICalTimeZone lookup(String tzid,
            int stdOffset, String stdDtStart, String stdRRule, String stdTzname,
            int dayOffset, String dayDtStart, String dayRRule, String dayTzname) {
        ICalTimeZone tz = lookupByTZID(tzid);
        if (tz != null) return tz;
        ICalTimeZone newTz = new ICalTimeZone(
                tzid, stdOffset, stdDtStart, stdRRule, stdTzname, dayOffset, dayDtStart, dayRRule, dayTzname);
        tz = lookupByRule(newTz, true);
        return tz;
    }

    ICalTimeZone(String tzId,
                    int stdOffset, String stdDtStart, String stdRRule, String stdTzname,
                    int dayOffset, String dayDtStart, String dayRRule, String dayTzname) {
        super(0, tzId);
        mStandardOffset = stdOffset;
        if (stdDtStart != null)
            mDayToStdDtStart = stdDtStart;
        mDayToStdRule = stdRRule;
        mStandardTzname = stdTzname;
        mDaylightOffset = dayOffset;
        if (dayDtStart != null)
            mStdToDayDtStart = dayDtStart;
        else
            mStdToDayDtStart = mDayToStdDtStart;
        mStdToDayRule = dayRRule;
        mDaylightTzname = dayTzname;
        initFromICalData(false);
    }

    public static ICalTimeZone lookup(String tzid,
                                      int standardOffset, SimpleOnset standardOnset, String standardTzname,
                                      int daylightOffset, SimpleOnset daylightOnset, String daylightTzname) {
        ICalTimeZone tz = lookupByTZID(tzid);
        if (tz != null) return tz;
        ICalTimeZone newTz = new ICalTimeZone(tzid, standardOffset, standardOnset, standardTzname, daylightOffset, daylightOnset, daylightTzname);
        tz = lookupByRule(newTz, true);
        return tz;
    }

    private ICalTimeZone(String tzId,
                         int standardOffset, SimpleOnset standardOnset, String standardTzname,
                         int daylightOffset, SimpleOnset daylightOnset, String daylightTzname) {
        super(0, tzId);
        mStandardOffset = standardOffset;
        mDaylightOffset = daylightOffset;
        mStandardTzname = standardTzname;
        mDaylightTzname = daylightTzname;
        setRawOffset(mStandardOffset);
        if (mDaylightOffset != mStandardOffset &&
            standardOnset != null && daylightOnset != null) {
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

    private static String toICalDtStart(SimpleOnset onset) {
        String hourStr = Integer.toString(onset.getHour() + 100).substring(1);
        String minuteStr = Integer.toString(onset.getMinute() + 100).substring(1);
        String secondStr = Integer.toString(onset.getSecond() + 100).substring(1);
        StringBuilder sb = new StringBuilder("19710101T");
        sb.append(hourStr).append(minuteStr).append(secondStr);
        return sb.toString();
    }

    private static String toICalRRule(SimpleOnset onset) {
        int week = onset.getWeek();
        int mday = onset.getDayOfMonth();
        StringBuilder sb =
            new StringBuilder("FREQ=YEARLY;INTERVAL=1;BYMONTH=");
        sb.append(onset.getMonth());
        if (week != 0) {
            sb.append(";BYDAY=");
            sb.append(week).append(sDayOfWeekNames[onset.getDayOfWeek()]);
            sb.append(";WKST=MO");
        } else if (mday > 0) {
            sb.append(";BYMONTHDAY=").append(mday);
        } else {
            return null;
        }
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
                    -28800000, "19710101T020000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=10;BYDAY=-3FR", "FOO",
                    -25200000, "19710101T020000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=4;BYDAY=2TU", "BAR");
            if (!verifyTZRules(mytz, 2005))
                badCount++;
            System.out.println();

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

            StringBuilder sb = new StringBuilder();
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
            boolean hasDaylight = tz.mStandardOffset != tz.mDaylightOffset;
            if (!tz.useDaylightTime()) {
            	if (onsets[0] != null || onsets[1] != null) {
            		System.out.println("ERROR: Onset dates present in a non-DST TZ.");
                    goodOrBad = false;
                }
                System.out.println("  DST supported:   " + hasDaylight);
                System.out.println("  toString:\n" + tz.toString());
            } else {
            	if (onsets[0] == null || onsets[1] == null) {
            		System.out.println("ERROR: Not enough onset dates present in a DST TZ.");
                    goodOrBad = false;
                }
                // Dump TZ detail.
                System.out.println("  DST supported:   " + hasDaylight);
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
                    tz.getStandardOffset(), tz.getStandardOnset(), tz.getStandardTzname(),
                    tz.getDaylightOffset(), tz.getDaylightOnset(), tz.getDaylightTzname());
            return newtz;
        }

        private static ICalTimeZone cloneFromICalData(ICalTimeZone tz) {
            ICalTimeZone newtz = new ICalTimeZone(
                    tz.getID(),
                    tz.getStandardOffset(), tz.mDayToStdDtStart, tz.mDayToStdRule, tz.mStandardTzname,
                    tz.getDaylightOffset(), tz.mStdToDayDtStart, tz.mStdToDayRule, tz.mDaylightTzname);
            return newtz;
        }

        private static String parseHHMMSS(String text) {
        	int indexT = text.indexOf('T');
            if (indexT == -1)
                return null;
            String time = text.substring(indexT);
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
     * 
     * The sign character is required and each time component must be specified wth 2 digits,
     * but this method relaxes the rules a bit to accept data from buggy systems.  Omitted
     * sign means positive offset, and hour can be specified as a single digit.
     * 
     * @param utcOffset
     * @return
     */
    private static int tzOffsetTime(String utcOffset)
    throws ServiceException {
        try {
            int len = utcOffset != null ? utcOffset.length() : 0;
            if (len < 1) {
                throw ServiceException.INVALID_REQUEST(
                        "Invalid " +
                        ICalTok.TZOFFSETFROM + "/" + ICalTok.TZOFFSETTO +
                        " value \"" + utcOffset +
                        "\"; must have format \"+/-hhmm[ss]\"", null);
            }

            int offset;
            char signChar = utcOffset.charAt(0);
            int sign;
            if (signChar == '-') {
                sign = -1;
                offset = 1;
            } else {
                sign = 1;
                if (signChar == '+') {
                    offset = 1;
                } else {
                    // The first character should be '+' or '-' and so this case
                    // is invalid, but let's relax the rules a bit and allow omitting
                    // the sign.
                    offset = 0;
                }
            }

            int toRet = 0;
            try {
                if ((len - offset) % 2 == 0) {
                    toRet += Integer.parseInt(utcOffset.substring(offset, offset + 2)) *
                             MSEC_PER_HOUR;
                    offset += 2;
                } else {
                    // Single-digit hour is invalid, but there are systems that
                    // do this, notably WebEx.
                    toRet += Integer.parseInt(utcOffset.substring(offset, offset + 1)) *
                             MSEC_PER_HOUR;
                    offset++;
                }
                // MM part is required, but let's allow omitting.  (for American Express travel app)
                if (len - offset >= 2) {
                    toRet += Integer.parseInt(utcOffset.substring(offset, offset + 2)) *
                             MSEC_PER_MIN;
                    offset += 2;
                }
                if (len - offset >= 2)
                    toRet += Integer.parseInt(utcOffset.substring(offset, offset + 2)) *
                             MSEC_PER_SEC;
            } catch (NumberFormatException e) {
                throw ServiceException.INVALID_REQUEST(
                        "Invalid " +
                        ICalTok.TZOFFSETFROM + "/" + ICalTok.TZOFFSETTO +
                        " value \"" + utcOffset +
                        "\"; must have format \"+/-hhmm[ss]\"", e);
            }
            toRet *= sign;
            return toRet;
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST(
                    "Invalid " +
                    ICalTok.TZOFFSETFROM + "/" + ICalTok.TZOFFSETTO +
                    " value \"" + utcOffset +
                    "\"; must have format \"+/-hhmm[ss]\"", e);
        } catch (IndexOutOfBoundsException e) {
            throw ServiceException.INVALID_REQUEST(
                    "Invalid " +
                    ICalTok.TZOFFSETFROM + "/" + ICalTok.TZOFFSETTO +
                    " value \"" + utcOffset +
                    "\"; must have format \"+/-hhmm[ss]\"", e);
        }
    }
    
    /**
     * Input: msec GMT
     * Output: TZOFFSETTO: [+-]HHMM(SS)?
     * @param utcOffset
     * @return
     */
    static String timeToTzOffsetString(int time)
    {
        StringBuilder toRet = new StringBuilder(time > 0 ? "+" : "-");
       
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
            if (mStandardTzname != null)
                standard.addProperty(new ZProperty(ICalTok.TZNAME, mStandardTzname));
        }

        if (mStdToDayDtStart != null && (mStandardOffset != mDaylightOffset || mDayToStdDtStart == null)) {
            ZComponent daylight = new ZComponent(ICalTok.DAYLIGHT);
            vtz.addComponent(daylight);
            
            daylight.addProperty(new ZProperty(ICalTok.DTSTART, mStdToDayDtStart));
            daylight.addProperty(new ZProperty(ICalTok.TZOFFSETTO, timeToTzOffsetString(mDaylightOffset)));
            daylight.addProperty(new ZProperty(ICalTok.TZOFFSETFROM, timeToTzOffsetString(mStandardOffset)));
            if (mStdToDayRule != null)
	            daylight.addProperty(new ZProperty(ICalTok.RRULE, mStdToDayRule));
            if (mDaylightTzname != null)
                daylight.addProperty(new ZProperty(ICalTok.TZNAME, mDaylightTzname));
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
        return fromVTimeZone(comp, false);
    }

    static ICalTimeZone fromVTimeZone(ZComponent comp, boolean skipLookup)
    throws ServiceException {
        String tzid = comp.getPropVal(ICalTok.TZID, null);

        if (!skipLookup) {
            ICalTimeZone tz = lookupByTZID(tzid);
            if (tz != null) return tz;
        }

        ZComponent standard = null;
        ZComponent daylight = null;

        // Find the most recent STANDARD and DAYLIGHT components.  "Most recent"
        // means the component's DTSTART is later in time than that of all other
        // components.  Thus, if multiple STANDARD components are specified in
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
        	throw new IllegalArgumentException("VTIMEZONE has neither STANDARD nor DAYLIGHT: TZID=" + tzid);
        
        String stddtStart = null;
        int stdoffsetTime = 0;
        String stdrrule = null;
        String stdTzname = null;
        
        if (standard != null) {
            stddtStart = standard.getPropVal(ICalTok.DTSTART, null);
            String stdtzOffsetTo = standard.getPropVal(ICalTok.TZOFFSETTO, null);
            stdoffsetTime = tzOffsetTime(stdtzOffsetTo);
            stdTzname = standard.getPropVal(ICalTok.TZNAME, null);

            // Check if STANDARD defines a non-DST timezone.  If so, DAYLIGHT should be ignored if its
            // DTSTART is later than that of STANDARD. (bug 25176)
            String stdtzOffsetFrom = standard.getPropVal(ICalTok.TZOFFSETFROM, null);
            if (stdtzOffsetFrom != null) {
                int tzoffsetFromTime = tzOffsetTime(stdtzOffsetFrom);
                if (tzoffsetFromTime == stdoffsetTime && daylight != null) {
                    ZComponent moreRecent = moreRecentTzComp(standard, daylight);
                    if (moreRecent == standard)
                        daylight = null;
                }
            }

            if (daylight != null) {
                // Rule is interesting only if daylight savings is in use.
                stdrrule = standard.getPropVal(ICalTok.RRULE, null);
            }
        }
        
        String daydtStart = null;
        int dayoffsetTime = stdoffsetTime;
        String dayrrule = null;
        String dayTzname = null;
        
        if (daylight != null) {
            daydtStart = daylight.getPropVal(ICalTok.DTSTART, null);
            String daytzOffsetTo = daylight.getPropVal(ICalTok.TZOFFSETTO, null);
            dayoffsetTime = tzOffsetTime(daytzOffsetTo);  
            dayrrule = daylight.getPropVal(ICalTok.RRULE, null);
            dayTzname = daylight.getPropVal(ICalTok.TZNAME, null);

            // Check if DAYLIGHT defines a non-DST timezone.  If so and its DTSTART is later than
            // that of STANDARD, STANDARD should be discarded and DAYLIGHT should be used in its place.
            // Such a VTIMEZONE is invalid, but it's a possibility and we should do something reasonable.
            // This is the inverse of the case above with non-DST STANDARD with useless DAYLIGHT. (bug 25176)
            String daytzOffsetFrom = daylight.getPropVal(ICalTok.TZOFFSETFROM, null);
            if (daytzOffsetFrom != null) {
                int tzoffsetFromTime = tzOffsetTime(daytzOffsetFrom);
                if (tzoffsetFromTime == dayoffsetTime && standard != null) {
                    ZComponent moreRecent = moreRecentTzComp(standard, daylight);
                    if (moreRecent == daylight) {
                        // Make DAYLIGHT the new STANDARD.
                        standard = daylight;
                        stdoffsetTime = dayoffsetTime;
                        stddtStart = daydtStart;
                        stdrrule = dayrrule;
                        stdTzname = dayTzname;
                        daylight = null;
                        daydtStart = null;
                        dayrrule = null;
                        dayTzname = null;
                    }
                }
            }
        }
        
        ICalTimeZone newTz = new ICalTimeZone(tzid, 
                stdoffsetTime, stddtStart, stdrrule, stdTzname,
                dayoffsetTime, daydtStart, dayrrule, dayTzname);

        if (!skipLookup)
            newTz = lookupByRule(newTz, true);
        return newTz;
    }

    // Look up a well-known time zone by TZID.  A cloned object with given TZID is returned,
    // or null if TZID was unknown.  (No clone if input tzid matches the TZ looked up)
    private static ICalTimeZone lookupByTZID(String tzid) {
        if (!DebugConfig.disableCalendarTZMatchByID) {
            ICalTimeZone match = WellKnownTimeZones.getTimeZoneById(tzid);
            if (match != null) {
                if (match.getID().equals(tzid))
                    return match;
                else
                    return match.cloneWithNewTZID(tzid);
            }
        }
        return null;
    }

    // Lookup a well-known time zone by DST rule.
    private static ICalTimeZone lookupByRule(ICalTimeZone tz, boolean keepTZID) {
        if (!DebugConfig.disableCalendarTZMatchByRule) {
            ICalTimeZone match = WellKnownTimeZones.getBestMatch(tz);
            if (match != null) {
                if (keepTZID) {
                    // Return the matched TZ, but using the TZID of the passed-in tz.
                    String tzid = tz.getID();
                    if (match.getID().equals(tzid))
                        return match;
                    else
                        return match.cloneWithNewTZID(tzid);
                } else {
                    // Return the matched TZ.  TZID may be different than that of passed-in tz.
                    return match;
                }
            }
        }
        // No match.  Return the TZ that was passed in.
        return tz;
    }

    public static ICalTimeZone lookupMatchingWellKnownTZ(ICalTimeZone tz) {
        if (!DebugConfig.disableCalendarTZMatchByID) {
            ICalTimeZone match = WellKnownTimeZones.getTimeZoneById(tz.getID());
            if (match != null)
                return match;
        }
        if (!DebugConfig.disableCalendarTZMatchByRule) {
            ICalTimeZone match = WellKnownTimeZones.getBestMatch(tz);
            if (match != null)
                return match;
        }
        return tz;
    }
}
