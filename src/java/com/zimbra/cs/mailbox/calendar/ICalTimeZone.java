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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import net.fortuna.ical4j.model.component.VTimeZone;

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
public class ICalTimeZone extends net.fortuna.ical4j.model.TimeZone
{
    private static Log sLog = LogFactory.getLog(ICalTimeZone.class);

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

    private String mTzId = null;

    public static ICalTimeZone getUTC() { return sUTC; }

    public Metadata encodeAsMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_TZ_NAME, mTzId);
        meta.put(FN_HAS_DAYLIGHT, mHasDaylight);

        meta.put(FN_STD_OFFSET, mStandardOffset);
        meta.put(FN_DAYTOSTD_DTSTART, mDayToStdDtStart); 
        meta.put(FN_DAYTOSTD_RULE, mDayToStdRule);

        meta.put(FN_DAYLIGHT_OFFSET, mDaylightOffset);
        meta.put(FN_STDTODAY_DTSTART, mStdToDayDtStart);  
        meta.put(FN_STDTODAY_RULE, mStdToDayRule);
        return meta;
    }

    public int    getStandardOffset()  { return mStandardOffset; }
    public String getStandardDtStart() { return mDayToStdDtStart; }
    public String getStandardRule()    { return mDayToStdRule; }
    public int    getDaylightOffset()  { return mDaylightOffset; }
    public String getDaylightDtStart() { return mStdToDayDtStart; }
    public String getDaylightRule()    { return mStdToDayRule; }

    public static ICalTimeZone decodeFromMetadata(Metadata m) throws ServiceException {
        String tzId = m.get(FN_TZ_NAME);
        if (tzId == null)
            tzId = "unknown time zone";
        return new ICalTimeZone(tzId, m);
    }
    
    public VTimeZone toVTimeZone() throws ServiceException {
        try {
            return super.calcVTimeZone();
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Caught ParseException trying to get VTimeZone for tzid="+mTzId, e);
        }
    }

    private ICalTimeZone(String tzId, Metadata meta) throws ServiceException {
        super(0, tzId);
        mTzId = tzId;
        mHasDaylight = meta.getBool(FN_HAS_DAYLIGHT, false);

        mStandardOffset = (int) meta.getLong(FN_STD_OFFSET, 0);
        mDayToStdDtStart = meta.get(FN_DAYTOSTD_DTSTART, null);
        mDayToStdRule = meta.get(FN_DAYTOSTD_RULE, null);

        mDaylightOffset = (int) meta.getLong(FN_DAYLIGHT_OFFSET, mStandardOffset);
        mStdToDayDtStart = meta.get(FN_STDTODAY_DTSTART, mDayToStdDtStart);
        mStdToDayRule = meta.get(FN_STDTODAY_RULE, null);

        initFromICalData();
    }

    public ICalTimeZone(String tzId, VTimeZone vtz) {
        super(tzId, vtz);
        mTzId = tzId;
    }

    public ICalTimeZone(String tzId,
                        int stdOffset, String stdDtStart, String stdRRule,
                        int dayOffset, String dayDtStart, String dayRRule) {
        super(tzId,
              stdOffset, stdDtStart, stdRRule,
              dayOffset, dayDtStart, dayRRule);
        mTzId = tzId;
    }

    public ICalTimeZone(String tzId,
                        int standardOffset, SimpleOnset standardOnset,
                        int daylightOffset, SimpleOnset daylightOnset) {
        super(tzId,
              standardOffset, standardOnset, daylightOffset, daylightOnset);
        mTzId = tzId;
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
    static int tzOffsetToTime(String utcOffset) {
        int toRet = 0;
        
        toRet += (Integer.parseInt(utcOffset.substring(1,3)) * MSEC_PER_HOUR);
        toRet += (Integer.parseInt(utcOffset.substring(3,5)) * MSEC_PER_MIN);
        if (utcOffset.length() >= 7) {
            toRet += (Integer.parseInt(utcOffset.substring(5,7)) * MSEC_PER_SEC);
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
            standard.addProperty(new ZProperty(ICalTok.RRULE, mDayToStdRule));
        }
        
        if (mStdToDayDtStart != null) {
            ZComponent daylight = new ZComponent(ICalTok.DAYLIGHT);
            vtz.addComponent(daylight);
            
            daylight.addProperty(new ZProperty(ICalTok.DTSTART, mStdToDayDtStart));
            daylight.addProperty(new ZProperty(ICalTok.TZOFFSETTO, timeToTzOffsetString(mDaylightOffset)));
            daylight.addProperty(new ZProperty(ICalTok.TZOFFSETFROM, timeToTzOffsetString(mStandardOffset)));
            daylight.addProperty(new ZProperty(ICalTok.RRULE, mStdToDayRule));
        }
        
        return vtz;
    }
    
    public static ICalTimeZone fromVTimeZone(ZComponent comp)
    {
        String tzname = comp.getPropVal(ICalTok.TZID, null);
        
        ZComponent daylight = comp.getComponent(ICalTok.DAYLIGHT);
        
        String daydtStart = null;
        int dayoffsetTime = 0;
        String dayrrule = null;
        
        String stddtStart = null;
        int stdoffsetTime = 0;
        String stdrrule = null;
        
        if (daylight != null) {
            daydtStart = daylight.getPropVal(ICalTok.DTSTART, null);
            String daytzOffsetTo = daylight.getPropVal(ICalTok.TZOFFSETTO, null);
            dayoffsetTime = tzOffsetToTime(daytzOffsetTo);  
            dayrrule = daylight.getPropVal(ICalTok.RRULE, null);
        }
        
        ZComponent standard = comp.getComponent(ICalTok.STANDARD); 
        if (standard != null) {
            stddtStart = standard.getPropVal(ICalTok.DTSTART, null);
            String stdtzOffsetTo = standard.getPropVal(ICalTok.TZOFFSETTO, null);
            stdoffsetTime = tzOffsetToTime(stdtzOffsetTo);  
            stdrrule = standard.getPropVal(ICalTok.RRULE, null);
        }
        
        ICalTimeZone tz = new ICalTimeZone(tzname, 
                stdoffsetTime, stddtStart, stdrrule,
                dayoffsetTime, daydtStart, dayrrule);

        return tz;
    }
}
