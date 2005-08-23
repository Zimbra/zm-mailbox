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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.service.ServiceException;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RecurrenceId;


public final class ParsedDateTime {
    
    
    public static void main(String[] args) {
        ICalTimeZone utc = ICalTimeZone.getUTC();
        try {
            ParsedDateTime t1 = ParsedDateTime.parse("20050910", null, null, utc);
            System.out.println(t1);
        } catch (ParseException e) {
            System.out.println("Caught "+e);
            e.printStackTrace();
        }
    }

    // YYYYMMDD'T'HHMMSSss'Z' YYYY MM DD 'T' HH MM SS Z
    static Pattern sDateTimePattern = Pattern
            .compile("(\\d{4})(\\d{2})(\\d{2})(?:T(\\d{2})(\\d{2})(\\d{2})(Z)?)?");

    public static ParsedDateTime parse(Property prop, TimeZoneMap tzmap)
            throws ParseException {
        assert (prop instanceof DtStart || prop instanceof DtEnd || prop instanceof RecurrenceId);
        
        TzId paramTzId = (TzId)prop.getParameters().getParameter(Parameter.TZID);
        String tzid = null;
        if (paramTzId != null) {
            tzid = paramTzId.getValue();
        }
        
        if (tzid != null && tzid.equals("null")) {
            tzid = null;
        }
        
        String dateStr = prop.getValue();

        ICalTimeZone tz = tzmap.getTimeZone(tzid);

        return parse(dateStr, tzid, tz, tzmap.getLocalTimeZone());
    }

    public static ParsedDateTime parse(String str, String tzName, ICalTimeZone tz, ICalTimeZone localTZ)
            throws ParseException {
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

            if (m.group(4) != null) { // T....part
                hour = Integer.parseInt(m.group(4));
                minute = Integer.parseInt(m.group(5));
                second = Integer.parseInt(m.group(6));

                // Ignore TZ part if this is just a DATE, per RFC
                if (m.group(7) != null && m.group(7).equals("Z")) {
                    zulu = true;
                }
                if (zulu) {
                    // RFC2445 Section 4.3.5 Date-Time
                    // FORM #2: DATE WITH UTC TIME
                    tzName = "Z";
                } else if (tz == null) {
                    // RFC2445 Section 4.3.5 Date-Time
                    // FORM #1: DATE WITH LOCAL TIME
                    tz = localTZ;
                    tzName = localTZ.getID();
                }
                //else {
                // RFC2445 Section 4.3.5 Date-Time
                // FORM #3: DATE WITH LOCAL TIME AND TIME ZONE REFERENCE
                //}
            } else {
                tz = null;
                tzName = null;
            }
            
            GregorianCalendar cal = new GregorianCalendar();
            if (zulu) {
                cal.setTimeZone(ICalTimeZone.getUTC());
            } else if (tz != null) {
                cal.setTimeZone(tz);
            }

            cal.clear();

            boolean hasTime = false;

            if (hour >= 0) {
                cal.set(year, month, date, hour, minute, second);
                hasTime = true;
            } else {
                cal.set(year, month, date);
            }
            return new ParsedDateTime(cal, tzName, hasTime);
        } else {
            throw new ParseException("Invalid TimeString specified: " + str, 0);
        }
    }

    public static ParsedDateTime parse(String str, TimeZoneMap tzmap)
    throws ParseException {
        if (str == null)
            return null;
        ICalTimeZone tz = null;
        String tzid = null;
        if (str.startsWith("TZID=")) {
            int colonIdx = str.indexOf(":");
            tzid = str.substring(5, colonIdx); // 5 for 'TZID='
            tz = tzmap.getTimeZone(tzid);
            str = str.substring(colonIdx + 1);
        }
        if (tzid != null && tzid.equals("null")) {
            tzid = null;
        }
        return parse(str, tzid, tz, tzmap.getLocalTimeZone());
    }
    
    public static ParsedDateTime MAX_DATETIME;
    static {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2099, 1, 1);
        MAX_DATETIME = new ParsedDateTime(cal, null, false);
    }
    
    public net.fortuna.ical4j.model.Date iCal4jDate() throws ServiceException {
        try {
            if (mHasTime) {
                return new net.fortuna.ical4j.model.DateTime(getDateTimePartString());
            } else {
                return new net.fortuna.ical4j.model.Date(this.getDateTimePartString());
            }
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Caught ParseException: "+e, e);
        }
    }

    private GregorianCalendar mCal;
    
    public TimeZone getTimeZone() { return mCal.getTimeZone(); }

    // can't rely on cal.isSet, even though we want to -- because cal.toString()
    // sets the flag!!!
    private boolean mHasTime = false;

    /*
     * Date-time values of this type are said to be "floating" and are not bound
     * to any time zone in particular. They are used to represent the same hour,
     * minute, and second value regardless of which time zone is currently being
     * observed.
     */
//    private boolean mHasTimeZone = false; // if false and hasTime, then

//    private boolean mUTCTime = false;
    private String mTzName;

    private ParsedDateTime(GregorianCalendar cal, String tzName, boolean hasTime) {
        mCal = cal;
        mHasTime = hasTime;
        mTzName = tzName;
//        mHasTimeZone = hasTimeZone;
//        mUTCTime = utcTime;
//        System.out.println("Initializing ParsedDateTime to " + cal);
    }

    public ParsedDateTime add(ParsedDuration dur) {
        GregorianCalendar cal = (GregorianCalendar) mCal.clone();

        cal.add(java.util.Calendar.WEEK_OF_YEAR, dur.getWeeks());
        cal.add(java.util.Calendar.DAY_OF_YEAR, dur.getDays());
        cal.add(java.util.Calendar.HOUR_OF_DAY, dur.getHours());
        cal.add(java.util.Calendar.MINUTE, dur.getMins());
        cal.add(java.util.Calendar.SECOND, dur.getSecs());

        return new ParsedDateTime(cal, mTzName, mHasTime);
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

        // RFC2445 4.3.6 durations allow Weeks OR DATE'T'TIME -- but weeks must be alone
        // I don't understand quite why, but that's what the spec says...
        
        int weeks = 0, days = 0, hours = 0, mins = 0, secs = 0;
        
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

        return ParsedDuration.parse(negative, weeks, days, hours, mins, secs);
    }

    public Date getDate() {
        return mCal.getTime();
    }

    public String getDateTimePartString() {
        DecimalFormat fourDigitFormat = new DecimalFormat("0000");
        DecimalFormat twoDigitFormat = new DecimalFormat("00");

        StringBuffer toRet = new StringBuffer();

        toRet.append(fourDigitFormat.format(mCal.get(java.util.Calendar.YEAR)));
        toRet.append(twoDigitFormat
                .format(mCal.get(java.util.Calendar.MONTH) + 1));
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
                toRet.append(twoDigitFormat.format(mCal
                        .get(java.util.Calendar.SECOND)));
            } else {
                toRet.append("00");
            }

            if (mTzName != null && mTzName.equals("Z")) {
                toRet.append("Z");
            }

        }
        return toRet.toString();
    }
    
    public boolean isUTC() {
        if (mTzName!=null && mTzName.equals("Z")) {
            return true;
        } else {
            return false;
        }
    }
    
    public String getTZName() {
        if (mHasTime && mTzName!=null && !mTzName.equals("Z")) {
            return mTzName; 
        }
        return null;
    }

    public String getTZParamString() {
        if (mHasTime && mTzName!=null && !mTzName.equals("Z")) {
            return "TZID="+mTzName+":";
        }
        return "";
    }

    public long getUtcTime() {
        return mCal.getTimeInMillis();
    }

    public boolean hasTime() {
        return mHasTime;
    }

    public String toString() {
        return getTZParamString() + getDateTimePartString();
    }
}