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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.common.service.ServiceException;

public final class ParsedDateTime {
    
    /**
     * This means that "Date" events are treated as having a time of 00:00:00 in the
     * creator's default timezone, UNLESS they have the "UTC only" flag set
     */
    private static final boolean OUTLOOK_COMPAT_ALLDAY =
        LC.calendar_outlook_compatible_allday_events.booleanValue();
    
    public static void main(String[] args) {
        ICalTimeZone utc = ICalTimeZone.getUTC();
    	TimeZoneMap tzmap = new TimeZoneMap(utc);
    	try {
            ParsedDateTime t1 = ParsedDateTime.parse("20050910", tzmap, null, utc);
            System.out.println(t1);
        } catch (ParseException e) {
            System.out.println("Caught "+e);
            e.printStackTrace();
        }
    }

    // YYYYMMDD'T'HHMMSSss'Z' YYYY MM DD 'T' HH MM SS Z
    static Pattern sDateTimePattern = Pattern
            .compile("(\\d{4})(\\d{2})(\\d{2})(?:T(\\d{2})(\\d{2})(\\d{2})(Z)?)?");

    public static ParsedDateTime fromUTCTime(long utc) {
        return new ParsedDateTime(new java.util.Date(utc));
    }
    
    public static ParsedDateTime parseUtcOnly(String str) throws ParseException {
        return parse(str, null, null, null, true);
    }    

    public static ParsedDateTime parse(String str, TimeZoneMap tzmap, ICalTimeZone tz, ICalTimeZone localTZ)
    throws ParseException {
        return parse(str, tzmap, tz, localTZ, false);
    }

    private static ParsedDateTime parse(String str,
    									TimeZoneMap tzmap,
    									ICalTimeZone tz,
    									ICalTimeZone localTZ,
    									boolean utcOnly)
    throws ParseException {
    	// Time zone map is required unless utcOnly == true.
    	assert(tzmap != null || utcOnly);

    	Matcher m = sDateTimePattern.matcher(str);
        
        if (m.matches()) {
            int year, month, date;
            int hour = -1;
            int minute = -1;
            int second = -1;
            boolean zulu = false;
            
            year = Integer.parseInt(m.group(1));
            month = Integer.parseInt(m.group(2)) - 1; // java months are
            // 0-indexed!
            date = Integer.parseInt(m.group(3));

            if (m.group(4) != null) { // has a T....part
                hour = Integer.parseInt(m.group(4));
                minute = Integer.parseInt(m.group(5));
                second = Integer.parseInt(m.group(6));

                // Ignore TZ part if this is just a DATE, per RFC
                if (m.group(7) != null && m.group(7).equals("Z")) {
                    zulu = true;
                }
                
                if (zulu || utcOnly) {
                    // RFC2445 Section 4.3.5 Date-Time
                    // FORM #2: DATE WITH UTC TIME
                    tz = ICalTimeZone.getUTC();
                } else if (tz == null) {
                    // RFC2445 Section 4.3.5 Date-Time
                    // FORM #1: DATE WITH LOCAL TIME
                    tz = localTZ;
                }
                //else {
                // RFC2445 Section 4.3.5 Date-Time
                // FORM #3: DATE WITH LOCAL TIME AND TIME ZONE REFERENCE
                //}
            } else {
            	// no timezone if it is a DATE entry....note that we *DO* need a
            	// 'local time zone' as a fallback: if we're in OUTLOOK_COMPAT_ALLDAY
            	// mode we will need to use this local time zone as the zone to render
            	// the appt in (remember, outlook all-day-appts must be 0:00-0:00 in 
            	// the client's timezone!)
            	tz = null;
            }
            
            GregorianCalendar cal = new GregorianCalendar();
            if (zulu || utcOnly) {
                cal.setTimeZone(ICalTimeZone.getUTC());
            } else {
                if (tz == null)
                    tz = localTZ;

                if (tz != null) { // localTZ could have been null
                    cal.setTimeZone(tz);
                    if (tzmap != null)
	                    tzmap.add(tz);
                }
            }

            cal.clear();

            boolean hasTime = false;

            if (hour >= 0) {
                cal.set(year, month, date, hour, minute, second);
                hasTime = true;
            } else {
                cal.set(year, month, date, 0, 0, 0);
            }
            return new ParsedDateTime(cal, tz, hasTime);
        } else {
            if (str.length() == 9 && str.charAt(8) == 'Z') {
                // Some systems/sites are known to generate dates with
                // year, month, date followed by "Z".  That's an invalid
                // format, but we'll try to work with it, by ignoring
                // the unnecessary "Z".
            	//
            	// Since they requested "Z", we'll pass in UTC as their 'default timezone' 
            	// just in case it somehow comes to that (ie Outlook hack)
                return parse(str.substring(0, 8), tzmap, tz, ICalTimeZone.getUTC(), utcOnly);
            } else
                throw new ParseException("Invalid date/time specified: " + str, 0);
        }
    }

    public static ParsedDateTime parse(ZProperty prop, TimeZoneMap tzmap)
    throws ParseException, ServiceException {
    	assert(tzmap != null);
        String tzname = prop.getParameterVal(ICalTok.TZID, null);
        
        ICalTimeZone tz = null;
        if (tzname != null) 
        	tz = tzmap.lookupAndAdd(tzname);

        if (tz == null) 
            tz = tzmap.getLocalTimeZone();
        
        return parse(prop.getValue(), tzmap, tz, tzmap.getLocalTimeZone());
    }

    public static ParsedDateTime parse(String str, TimeZoneMap tzmap)
    throws ParseException, ServiceException {
    	assert(tzmap != null);
        if (str == null) return null;

        String datetime;
        ICalTimeZone tz = null;
        int propValueColonIdx = str.lastIndexOf(':');  // colon before property value
        if (propValueColonIdx != -1) {
        	datetime = str.substring(propValueColonIdx + 1);

            int tzidIdx = str.indexOf("TZID=");
            if (tzidIdx != -1) {
                String tzid;
            	int valueParamIdx = str.lastIndexOf(";VALUE=");
            	if (valueParamIdx > tzidIdx)
            		tzid = str.substring(tzidIdx + 5, valueParamIdx);
            	else
            		tzid = str.substring(tzidIdx + 5, propValueColonIdx);

                if ((tzid.equals("GMT") || tzid.equals("UTC"))
                	&& !datetime.endsWith("Z")) {
                	datetime += "Z";
                } else {
                	tz = tzmap.lookupAndAdd(tzid);
                }
            }
        } else {
        	// no parameters; the whole thing is property value
        	datetime = str;
        }

        return parse(datetime, tzmap, tz, tzmap.getLocalTimeZone());
    }

    public static ParsedDateTime MAX_DATETIME;
    static {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2099, 1, 1);
        cal.setTimeZone(ICalTimeZone.getUTC());
        MAX_DATETIME = new ParsedDateTime(cal, ICalTimeZone.getUTC(), false);
    }
    
    private GregorianCalendar mCal;
    
    public ICalTimeZone getTimeZone() { return mICalTimeZone; }
    

    // can't rely on cal.isSet, even though we want to -- because cal.toString()
    // sets the flag!!!
    private boolean mHasTime = false;
    
    private ICalTimeZone mICalTimeZone;

    private ParsedDateTime(GregorianCalendar cal, ICalTimeZone iCalTimeZone, boolean hasTime) {
        mCal = cal;
        mHasTime = hasTime;
        mICalTimeZone = iCalTimeZone;
    }
    
    ParsedDateTime(java.util.Date utc) {
        mCal = new GregorianCalendar(ICalTimeZone.getUTC());
        mCal.setTime(utc);
        mICalTimeZone = ICalTimeZone.getUTC();
        mHasTime = true;
    }

    public ParsedDateTime add(ParsedDuration dur) {
        GregorianCalendar cal = (GregorianCalendar) mCal.clone();

        cal.add(java.util.Calendar.WEEK_OF_YEAR, dur.getWeeks());
        cal.add(java.util.Calendar.DAY_OF_YEAR, dur.getDays());
        cal.add(java.util.Calendar.HOUR_OF_DAY, dur.getHours());
        cal.add(java.util.Calendar.MINUTE, dur.getMins());
        cal.add(java.util.Calendar.SECOND, dur.getSecs());

        return new ParsedDateTime(cal, mICalTimeZone, mHasTime);
    }

    /**
     * Returns a new ParsedDateTime object that has the same time values
     * (hour, minutes, seconds, and millis) and time zone as this object and
     * the same date values (year, month, date) as "date" object.
     * @param other
     * @return
     */
    public ParsedDateTime cloneWithNewDate(ParsedDateTime date) {
        GregorianCalendar cal = (GregorianCalendar) mCal.clone();
        GregorianCalendar calDate = (GregorianCalendar) date.mCal;
        cal.set(calDate.get(java.util.Calendar.YEAR),
                calDate.get(java.util.Calendar.MONTH),
                calDate.get(java.util.Calendar.DAY_OF_MONTH));
        return new ParsedDateTime(cal, mICalTimeZone, mHasTime);
    }

    public Object clone() {
        GregorianCalendar cal = (GregorianCalendar) mCal.clone();
        return new ParsedDateTime(cal, mICalTimeZone, mHasTime);
    }

    public int compareTo(Date other) {
        return getDate().compareTo(other);
    }

    public int compareTo(long other) {
        long myTime = getDate().getTime();
        return (int) (myTime - other);
    }

    public int compareTo(Object other) {
        return compareTo(((ParsedDateTime) other).getDate());
    }

    public int compareTo(ParsedDateTime other) {
        return compareTo(other.getDate());
    }
    
    public boolean equals(Object obj) {
        return (compareTo(obj) == 0);
    }

    /**
     * Checks if the other ParsedDateTime object has the same hour, minute, second
     * and millisecond values, in the timezone of this object.
     * @param other
     * @return
     */
    public boolean sameTime(ParsedDateTime other) {
        if (other == null) return false;
        ICalTimeZone thisTZ = getTimeZone();
        ICalTimeZone otherTZ = other.getTimeZone();
        GregorianCalendar otherCal;
        if (thisTZ.equals(otherTZ)) {
            otherCal = other.mCal;
        } else {
            otherCal = new GregorianCalendar(thisTZ);
            otherCal.setTimeInMillis(mCal.getTimeInMillis());
        }
        return
            mCal.get(java.util.Calendar.HOUR_OF_DAY)
            == otherCal.get(java.util.Calendar.HOUR_OF_DAY) &&
            mCal.get(java.util.Calendar.MINUTE)
            == otherCal.get(java.util.Calendar.MINUTE) &&
            mCal.get(java.util.Calendar.SECOND)
            == otherCal.get(java.util.Calendar.SECOND) &&
            mCal.get(java.util.Calendar.MILLISECOND)
            == otherCal.get(java.util.Calendar.MILLISECOND);
    }


    static final long MSECS_PER_SEC = 1000;
    static final long MSECS_PER_MIN = MSECS_PER_SEC * 60;
    static final long MSECS_PER_HOUR = MSECS_PER_MIN * 60;
    static final long MSECS_PER_DAY = MSECS_PER_HOUR * 24;
    static final long MSECS_PER_WEEK = MSECS_PER_DAY * 7;

    public ParsedDuration difference(ParsedDateTime other) {
        long myTime = mCal.getTimeInMillis();
        long otherTime = other.mCal.getTimeInMillis();

        long diff = myTime - otherTime;
        
        boolean negative = false;
        if (diff < 0) {
            negative = true;
            diff *= -1;
        }

        int weeks = 0, days = 0, hours = 0, mins = 0, secs = 0;
        if (mHasTime || other.mHasTime) {
            // RFC2445 4.3.6 durations allow Weeks OR DATE'T'TIME -- but weeks must be alone
            // I don't understand quite why, but that's what the spec says...

            if ((diff >= MSECS_PER_WEEK) && (diff % MSECS_PER_WEEK == 0)) {
                weeks = (int) (diff / MSECS_PER_WEEK);
            } else {
                long dleft = diff;
    
                days = (int) (dleft / MSECS_PER_DAY);
                dleft = dleft % MSECS_PER_DAY;
                
                hours = (int) (dleft/ MSECS_PER_HOUR);
                dleft = dleft % MSECS_PER_HOUR;
                
                mins = (int) (dleft/ MSECS_PER_MIN);
                dleft = dleft % MSECS_PER_MIN;
                
                secs = (int) (dleft/ MSECS_PER_SEC);
            }
        } else {
            // All-day values.  Round to the nearest day boundary to deal with
            // daylight savings time transition dates.
            long dleft = diff;
            days = (int) (dleft / MSECS_PER_DAY);
            dleft = dleft % MSECS_PER_DAY;
            if (dleft >= MSECS_PER_DAY / 2)
                days++;
        }

        return ParsedDuration.parse(negative, weeks, days, hours, mins, secs);
    }

    public Date getDate() {
        return mCal.getTime();
    }

    /**
     * Return Date suitable for use as UNTIL parameter of recurrence rule.
     * Outlook always appends "T000000Z" to UNTIL, causing meeting instance
     * on the last day of recurrence to be excluded if the UNTIL value was
     * taken at face value.  (bug 5885)  To combat that, ignore the
     * time component and extend the UNTIL time to the end of the day
     * (23:59:59) in the time zone of DTSTART.  The same treatment is needed
     * if UNTIL is specified as date-only.
     * @return
     */
    public Date getDateForRecurUntil(ICalTimeZone dtStartTZ) {
        // if date-only or if time component looks like bogus
        // UTC midnight set by Outlook
        if (!mHasTime ||
            (isUTC() &&
             mCal.get(java.util.Calendar.HOUR_OF_DAY) == 0 &&
             mCal.get(java.util.Calendar.MINUTE) == 0 &&
             mCal.get(java.util.Calendar.SECOND) == 0 &&
             mCal.get(java.util.Calendar.MILLISECOND) == 0)) {
            // Extend to end of day in DTSTART's time zone.
            GregorianCalendar cal = new GregorianCalendar(dtStartTZ);
            cal.set(mCal.get(java.util.Calendar.YEAR),
                    mCal.get(java.util.Calendar.MONTH),
                    mCal.get(java.util.Calendar.DAY_OF_MONTH),
                    0, 0, 0);  // midnight
            // Add one day, then subtract 1 second.
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.add(java.util.Calendar.SECOND, -1);

            // Sanity check: If the above calculation somehow changes
            // UNTIL to an earlier time than what's in mCal, we should
            // stick to mCal. (the later of the two)
            Date until = cal.getTime();
            Date untilOriginal = mCal.getTime();
            if (until.before(untilOriginal))
                until = untilOriginal;
            return until;
        } else
            return getDate();
    }

    /**
     * @return The YYYYMMDD['T'HHMMSS[Z]] part  
     */
    public String getDateTimePartString() {
        return getDateTimePartString(OUTLOOK_COMPAT_ALLDAY);
    }

    /**
     * @return The YYYYMMDD['T'HHMMSS[Z]] part  
     */
    public String getDateTimePartString(boolean useOutlookCompatMode) {
        DecimalFormat fourDigitFormat = new DecimalFormat("0000");
        DecimalFormat twoDigitFormat = new DecimalFormat("00");

        StringBuffer toRet = new StringBuffer();

        toRet.append(fourDigitFormat.format(mCal.get(java.util.Calendar.YEAR)));
        toRet.append(twoDigitFormat.format(mCal.get(java.util.Calendar.MONTH) + 1));
        toRet.append(twoDigitFormat.format(mCal.get(java.util.Calendar.DATE)));

        // if HOUR is set, then assume it is a DateTime, otherwise assume it is
        // just a Date

        if (mHasTime) {
            toRet.append("T");

            toRet.append(twoDigitFormat.format(mCal
                    .get(java.util.Calendar.HOUR_OF_DAY)));

            if (mCal.isSet(java.util.Calendar.MINUTE)) {
                toRet.append(twoDigitFormat.format(mCal
                        .get(java.util.Calendar.MINUTE)));
            } else {
                toRet.append("00");
            }
            if (mCal.isSet(java.util.Calendar.SECOND)) {
                toRet.append(twoDigitFormat.format(mCal.get(java.util.Calendar.SECOND)));
            } else {
                toRet.append("00");
            }

            if (isUTC()) {
                toRet.append("Z");
            }
        } else if (useOutlookCompatMode) {
            toRet.append("T000000");
        	// OUTLOOK HACK -- remember, outlook all-day-appts 
        	// must be rendered as 0:00-0:00 in the client's timezone...but we
            // need to correctly fallback to UTC here (e.g. UNTILs w/ DATE values
            // that are implicitly therefore in UTC) in some cases.  Sheesh.
            if (getTZName() == null) {
                toRet.append("Z");
            }
        }
        
        return toRet.toString();
    }
    
    public boolean isUTC() {
        if (mICalTimeZone.getID()!=null && mICalTimeZone.getID().equals("Z")) {
            return true;
        } else {
            return false;
        }
    }

    public void toUTC() {
        if (!isUTC()) {
            mICalTimeZone = ICalTimeZone.getUTC();
            Date time = mCal.getTime();
            mCal.setTimeZone(mICalTimeZone);
            mCal.setTime(time);
        }
    }

    public void toTimeZone(ICalTimeZone tz) {
        if (mHasTime) {
            Date time = mCal.getTime();
            mICalTimeZone = tz;
            mCal.setTimeZone(mICalTimeZone);
            mCal.setTime(time);
        }
    }

    /**
     * @return The name of the TimeZone
     */
    public String getTZName() {
        if ((mHasTime || OUTLOOK_COMPAT_ALLDAY) && mICalTimeZone!=null && !isUTC() ) {
            return mICalTimeZone.getID();
        }
        return null;
    }

    /**
     * @return The full RFC2445 parameter string (ie "TZID=blah;") 
     */
    private String getTZParamString() {
        String tzName = getTZName();
        if (tzName != null) {
            return "TZID="+tzName+":";
        } else {
            return "";
        }
    }
    
    public GregorianCalendar getCalendarCopy() {
        return (GregorianCalendar)(mCal.clone());
    }
    

    public long getUtcTime() {
        return mCal.getTimeInMillis();
    }

    public boolean hasTime() {
        return mHasTime;
    }

    public void setHasTime(boolean hasTime) {
        mHasTime = hasTime;
    }

    public String toString() {
        if (mHasTime)
            return getTZParamString() + getDateTimePartString();
        else
            return "VALUE=DATE:" + getDateTimePartString(false);
    }
    
    public ZProperty toProperty(ICalTok tok, boolean useOutlookCompatMode) {
        ZProperty toRet = new ZProperty(tok, getDateTimePartString(useOutlookCompatMode));
        
        String tzName = getTZName();
        if (!useOutlookCompatMode && !hasTime()) {
            toRet.addParameter(new ZParameter(ICalTok.VALUE, ICalTok.DATE.toString()));
        } else {
            assert(isUTC() || tzName != null);
            if (tzName != null) {
                toRet.addParameter(new ZParameter(ICalTok.TZID, tzName));
            } 
        }
        return toRet;
    }

    void forceDateOnly() {
        mHasTime = false;
    }
}
