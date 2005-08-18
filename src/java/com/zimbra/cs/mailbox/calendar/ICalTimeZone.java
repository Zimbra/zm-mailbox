package com.zimbra.cs.mailbox.calendar;

import java.text.ParseException;
import java.util.Calendar;
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
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;

import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.UtcOffset;
import net.fortuna.ical4j.model.component.Daylight;
import net.fortuna.ical4j.model.component.Standard;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.TzId;
import net.fortuna.ical4j.model.property.TzOffsetFrom;
import net.fortuna.ical4j.model.property.TzOffsetTo;

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
    private static Log sLog = LogFactory.getLog(ICalTimeZone.class);

    private static final String FN_TZ_NAME          = "tzid";
    private static final String FN_STD_OFFSET       = "so";
    private static final String FN_HAS_DAYLIGHT     = "hd";
    private static final String FN_DAYLIGHT_OFFSET  = "do";
    private static final String FN_DAYTOSTD_DTSTART = "d2ss";
    private static final String FN_STDTODAY_DTSTART = "s2ds";
    private static final String FN_DAYTOSTD_RULE    = "d2sr";
    private static final String FN_STDTODAY_RULE    = "s2dr";

    private static final String DEFAULT_DTSTART = "16010101T020000";

    private static ICalTimeZone sUTC = new ICalTimeZone("GMT",
                                                        0,
                                                        "16010101T000000",
                                                        null,
                                                        0,
                                                        "16010101T000000",
                                                        null);

    private String mTzId = null;
    private boolean mHasDaylight = false;

    private int    mStdOffset = 0;
    private String mDayToStdDtStart = DEFAULT_DTSTART;
    private String mDayToStdRule = null;

    private int    mDaylightOffset = 0; 
    private String mStdToDayDtStart = DEFAULT_DTSTART;
    private String mStdToDayRule = null;


    public static ICalTimeZone getUTC() { return sUTC; }

    public Metadata encodeAsMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_TZ_NAME, mTzId);
        meta.put(FN_HAS_DAYLIGHT, mHasDaylight);

        meta.put(FN_STD_OFFSET, mStdOffset);
        meta.put(FN_DAYTOSTD_DTSTART, mDayToStdDtStart); 
        meta.put(FN_DAYTOSTD_RULE, mDayToStdRule);

        meta.put(FN_DAYLIGHT_OFFSET, mDaylightOffset);
        meta.put(FN_STDTODAY_DTSTART, mStdToDayDtStart);  
        meta.put(FN_STDTODAY_RULE, mStdToDayRule);
        return meta;
   }

    public static ICalTimeZone decodeFromMetadata(Metadata m) throws ServiceException {
        String tzId = m.get(FN_TZ_NAME);
        if (tzId == null)
            tzId = "unknown time zone";
        return new ICalTimeZone(tzId, m);
    }

    private ICalTimeZone(String tzId, Metadata meta) throws ServiceException {
        super(0, tzId);
        mTzId = tzId;
        mHasDaylight = meta.getBool(FN_HAS_DAYLIGHT, false);

        mStdOffset = (int) meta.getLong(FN_STD_OFFSET, 0);
        mDayToStdDtStart = meta.get(FN_DAYTOSTD_DTSTART, null);
        mDayToStdRule = meta.get(FN_DAYTOSTD_RULE, null);

        mDaylightOffset = (int) meta.getLong(FN_DAYLIGHT_OFFSET, mStdOffset);
        mStdToDayDtStart = meta.get(FN_STDTODAY_DTSTART, mDayToStdDtStart);
        mStdToDayRule = meta.get(FN_STDTODAY_RULE, null);

        commonInit();
    }

    public ICalTimeZone(String tzId,
                        int stdOffset, String stdDtStart, String stdRRule,
                        int dayOffset, String dayDtStart, String dayRRule) {
        super(0, tzId);
        mTzId = tzId;
        mHasDaylight = stdOffset != dayOffset;
        mStdOffset = stdOffset;
        if (stdDtStart != null)
            mDayToStdDtStart = stdDtStart;
        mDayToStdRule = stdRRule;
        mDaylightOffset = dayOffset;
        if (dayDtStart != null)
            mStdToDayDtStart = dayDtStart;
        else
            mStdToDayDtStart = mDayToStdDtStart;
        mStdToDayRule = dayRRule;
        commonInit();
    }

    public ICalTimeZone(String tzId, VTimeZone vtz) throws ServiceException {
        super(0, tzId);
        mTzId = tzId;

        ComponentList c = vtz.getTypes();

        Standard std = (Standard) c.getComponent(net.fortuna.ical4j.model.component.SeasonalTime.STANDARD);
        if (std != null) {
            PropertyList props = std.getProperties();
            TzOffsetTo tzTo = (TzOffsetTo) props.getProperty(Property.TZOFFSETTO);
            if (tzTo != null)
                mStdOffset = (int) tzTo.getOffset().getOffset();
            
            Property d = props.getProperty(Property.DTSTART);
            if (d != null)
                mDayToStdDtStart = ((DtStart) d).getValue();
    
            Property r = props.getProperty(Property.RRULE);
            if (r!= null) {
                RRule rrule = (RRule) r;
                mDayToStdRule = rrule.getRecur().toString();
                if (sLog.isDebugEnabled()) {
                    sLog.debug("DayToStdRule=\"" + mDayToStdRule + "\"");
                }
            } else {
                // TODO - deal with timezones without rules for cutover
                // TODO - deal with timezones with RDATE instead of RRULE
            }
        }

        Daylight daylight = (Daylight) c.getComponent(net.fortuna.ical4j.model.component.SeasonalTime.DAYLIGHT);
        if (daylight != null) {
            mHasDaylight = true;
            PropertyList props = daylight.getProperties();
            TzOffsetTo tzTo = (TzOffsetTo) props.getProperty(Property.TZOFFSETTO);
            if (tzTo != null)
                mDaylightOffset = (int) tzTo.getOffset().getOffset();
            
            Property d = props.getProperty(Property.DTSTART);
            if (d != null)
                mStdToDayDtStart = ((DtStart) d).getValue();

            Property r = props.getProperty(Property.RRULE);
            if (r!= null) {
                RRule rrule = (RRule) r;
                mStdToDayRule = rrule.getRecur().toString();
                if (sLog.isDebugEnabled()) {
                    sLog.debug("StdToDayRule=\"" + mStdToDayRule + "\"");
                }
            } else {
                // TODO - deal with timezones without rules for cutover
                // TODO - deal with timezones with RDATE instead of RRULE
                mHasDaylight = false;
            }
        } else {
        	mDaylightOffset = mStdOffset;
        }

        if (std == null && daylight == null)
            throw MailServiceException.INVALID_REQUEST("VTIMEZONE must have at least one STANDARD or DAYLIGHT (TZID=" + tzId + ")", null);

        commonInit();
    }

    private void commonInit() {
        setRawOffset(mStdOffset);
        if (mHasDaylight) {
            int stdDtStart = dtStartToTimeInt(mDayToStdDtStart);
            OnsetRule stdOnset = toOnsetRule(mDayToStdRule);
            int dayDtStart = dtStartToTimeInt(mStdToDayDtStart);
            OnsetRule dayOnset = toOnsetRule(mStdToDayRule);

            setStartRule(dayOnset.month, dayOnset.dayOfMonth, dayOnset.dayOfWeek, dayDtStart);
            setEndRule(stdOnset.month, stdOnset.dayOfMonth, stdOnset.dayOfWeek, stdDtStart);
            setDSTSavings(mDaylightOffset - mStdOffset);
        }
    }

    /**
     * Discard date and return time part of DTSTART as number of milliseconds.
     * @param dtstart yyyymoddThhmmss
     * @return (hh * 3600 + mm * 60 + ss) * 1000
     */
    private static int dtStartToTimeInt(String dtstart) {
        try {
            int indexOfT = dtstart.indexOf('T');
            int hour = Integer.parseInt(dtstart.substring(indexOfT + 1, indexOfT + 3));
            int min = Integer.parseInt(dtstart.substring(indexOfT + 3, indexOfT + 5));
            int sec = Integer.parseInt(dtstart.substring(indexOfT + 5, indexOfT + 7));
            return (hour * 3600 + min * 60 + sec) * 1000;
        } catch (StringIndexOutOfBoundsException se) {
            return 0;
        } catch (NumberFormatException ne) {
            return 0;
        }
    }

    private static Map /*<String, Integer>*/ sDayOfWeekMap = new HashMap(7);
    static {
        sDayOfWeekMap.put("MO", new Integer(Calendar.MONDAY));
        sDayOfWeekMap.put("TU", new Integer(Calendar.TUESDAY));
        sDayOfWeekMap.put("WE", new Integer(Calendar.WEDNESDAY));
        sDayOfWeekMap.put("TH", new Integer(Calendar.THURSDAY));
        sDayOfWeekMap.put("FR", new Integer(Calendar.FRIDAY));
        sDayOfWeekMap.put("SA", new Integer(Calendar.SATURDAY));
        sDayOfWeekMap.put("SU", new Integer(Calendar.SUNDAY));
    }

    private static class OnsetRule {
        private int month = 0;
        private int dayOfMonth = 0;
        private int dayOfWeek = 0;
    }

    /**
     * Parse an iCalendar recurrence rule into info suitable for passing
     * into SimpleTimeZone constructor.
     * @param rrule
     * @return
     */
    private OnsetRule toOnsetRule(String rrule) {
        OnsetRule onset = new OnsetRule();
        if (rrule == null)
            return onset;
        for (StringTokenizer t = new StringTokenizer(rrule.toUpperCase(), ";=");
             t.hasMoreTokens();) {
            String token = t.nextToken();
            if ("BYMONTH".equals(token)) {
                // iCalendar month is 1-based.  Java month is 0-based.
                onset.month = Integer.parseInt(t.nextToken()) - 1;
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

                Integer day = (Integer) sDayOfWeekMap.get(value);
                if (day == null)
                    throw new IllegalArgumentException("Invalid day of week value: " + value);

                if (negative) {
                    // For specifying day-of-week of last Nth week of month,
                    // e.g. -2SA for Saturday of 2nd to last week of month,
                    // java.util.SimpleTimeZone wants negative week number
                    // in dayOfMonth.
                    onset.dayOfMonth = -1 * weekNum;

                    onset.dayOfWeek = day.intValue();
                } else {
                    // For positive weekNum, onset date is day of week on or
                    // after day of month.  First week is day 1 through day 7,
                    // second week is day 8 through day 14, etc.
                	onset.dayOfMonth = (weekNum - 1) * 7 + 1;

                    // Another peculiarity of java.util.SimpleTimeZone class.
                    // For positive weekNum, day-of-week must be specified as
                    // a negative value.
                    onset.dayOfWeek = -day.intValue();
                }
            } else {
                String s = t.nextToken();  // skip value of unused param
            }
        }
        return onset;
    }

    public VTimeZone toVTimeZone() throws ServiceException {
        TzId tzId = new TzId(getID());

        PropertyList tzProps = new PropertyList();
        tzProps.add(tzId);

        ComponentList tzComponents = new ComponentList();

        if (mDayToStdDtStart != null) {
            DtStart standardTzStart = null;
            try {
                standardTzStart = new DtStart(new ParameterList(), mDayToStdDtStart);
            } catch (ParseException e) {
            	throw MailServiceException.INVALID_REQUEST("Parse error on VTIMEZONE STANDARD DTSTART: " + mDayToStdDtStart, e);
            }
            TzOffsetTo standardTzOffsetTo = new TzOffsetTo(new ParameterList(), new UtcOffset(mStdOffset));
            TzOffsetFrom standardTzOffsetFrom = new TzOffsetFrom(new UtcOffset(mDaylightOffset));
    
            PropertyList standardTzProps = new PropertyList();
            standardTzProps.add(standardTzStart);
            standardTzProps.add(standardTzOffsetTo);
            standardTzProps.add(standardTzOffsetFrom);
    
            if (mDayToStdRule != null) {
            	RRule standardTzRRule = null;
                try {
                    standardTzRRule = new RRule(new ParameterList(), mDayToStdRule);
                } catch (ParseException e) {
                    throw MailServiceException.INVALID_REQUEST("Parse error on VTIMEZONE STANDARD RRULE: " + mDayToStdRule, e);
                }
                standardTzProps.add(standardTzRRule);
            }
    
            tzComponents.add(new Standard(standardTzProps));
        }

        if (mStdToDayDtStart != null) {
            DtStart daylightTzStart = null;
            try {
                daylightTzStart = new DtStart(new ParameterList(), mStdToDayDtStart);
            } catch (ParseException e) {
                throw MailServiceException.INVALID_REQUEST("Parse error on VTIMEZONE DAYLIGHT DTSTART: " + mStdToDayDtStart, e);
            }
            TzOffsetTo daylightTzOffsetTo = new TzOffsetTo(new ParameterList(), new UtcOffset(mDaylightOffset));
            TzOffsetFrom daylightTzOffsetFrom = new TzOffsetFrom(new UtcOffset(mStdOffset));

            PropertyList daylightTzProps = new PropertyList();
            daylightTzProps.add(daylightTzStart);
            daylightTzProps.add(daylightTzOffsetTo);
            daylightTzProps.add(daylightTzOffsetFrom);

            if (mStdToDayRule != null) {
                RRule daylightTzRRule = null;
                try {
                    daylightTzRRule = new RRule(new ParameterList(), mStdToDayRule);
                } catch (ParseException e) {
                    throw MailServiceException.INVALID_REQUEST("Parse error on VTIMEZONE DAYLIGHT RRULE: " + mStdToDayRule, e);
                }
                daylightTzProps.add(daylightTzRRule);
            }

            tzComponents.add(new Daylight(daylightTzProps));
        }

        return new VTimeZone(tzProps, tzComponents);
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
                System.out.println("  Standard Offset: " + offsetToHHMM(tz.mStdOffset));
                System.out.println("  Standard Start:  " + tz.mDayToStdDtStart);
                System.out.println("  Standard RRule:  " + tz.mDayToStdRule);

                defStandardTime = parseHHMMSS(tz.mDayToStdDtStart);
                defDaylightTime = parseHHMMSS(tz.mStdToDayDtStart);
                if (!defStandardTime.equals(actualStandardTime) ||
                    !defDaylightTime.equals(actualDaylightTime)) {
                    System.out.println("ERROR: Onset times don't match between definition and actual.");
                	goodOrBad = false;
                }

                // TODO: Verify defined and actual offsets in standard/daylight match.
                // TODO: Verify actual day-of-week in week-of-month matches the rule.
            }
            return goodOrBad;
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
}
