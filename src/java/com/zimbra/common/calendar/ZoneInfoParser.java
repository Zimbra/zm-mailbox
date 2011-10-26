/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.calendar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Map.Entry;

// tzdata text input file format is described in zic man page.

public class ZoneInfoParser {

    private static <T extends Comparable<? super T>> int nullsCompare(T o1, T o2, boolean nullFirst) {
        if (o1 == null && o2 == null)
            return 0;
        else if (o1 != null && o2 != null)
            return o1.compareTo(o2);
        else if (o1 != null && o2 == null)
            return nullFirst ? 1: -1;
        else  // o1 == null && o2 != null
            return nullFirst ? -1 : 1;
    }

    public static class TZDataParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public TZDataParseException(String msg) {
            super(msg);
        }
    }

    private static class Year {
        // Year is integer, "maximum" or its abbreviation, or "minimum" or its abbreviation.
        // "maximum" and "minimum" are parsed as Integer.MAX_VALUE and Integer.MIN_VALUE, respectively.
        private static int parseYear(String year) throws TZDataParseException {
            try {
                return Integer.parseInt(year);
            } catch (NumberFormatException e) {
                String lyear = year.toLowerCase();
                if ("maximum".startsWith(lyear))
                    return Integer.MAX_VALUE;
                else if ("minimum".startsWith(lyear))
                    return Integer.MIN_VALUE;
                else
                    throw new TZDataParseException("Invalid year \"" + year + "\"");
            }
        }
    }

    private static class Month {
        private static final String JAN = "january";
        private static final String FEB = "february";
        private static final String MAR = "march";
        private static final String APR = "april";
        private static final String MAY = "may";
        private static final String JUN = "june";
        private static final String JUL = "july";
        private static final String AUG = "august";
        private static final String SEP = "september";
        private static final String OCT = "october";
        private static final String NOV = "november";
        private static final String DEC = "december";

        private static final String[] sMonths = { JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC };
        private static final String[] sAbbrevs =
            { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

        // 1 = Jaunary, ..., 12 = December
        public static int parseMonth(String str) throws TZDataParseException {
            String mon = str.toLowerCase();
            for (int i = 0; i < sMonths.length; ++i) {
                if (sMonths[i].startsWith(mon))
                    return ++i;
            }
            throw new TZDataParseException("Invalid month \"" + str + "\"");
        }

        public static String toString(int mon) {
            if (mon >= 1 && mon <= sAbbrevs.length)
                return sAbbrevs[mon - 1];
            else
                return null;
        }

    }

    public static enum Weekday {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY;

        public static Weekday lookUp(String str) {
            if (str != null && str.length() >= 2) {
                String s = str.toUpperCase();
                for (Weekday wd : values()) {
                    if (wd.toString().startsWith(s))
                        return wd;
                }
            }
            return null;
        }

        private static final Weekday[] sByIndex = { SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY };

        public static Weekday lookUp(int index) {
            if (index >= 1 && index <= sByIndex.length)
                return sByIndex[index - 1];
            else
                return null;
        }

        // Converts Weekday into java.util.Calendar.JANUARY - DECEMBER integers.
        public static int toInt(Weekday wkday) {
            for (int i = 0; i < sByIndex.length; ++i) {
                if (sByIndex[i].equals(wkday))
                    return i + 1;
            }
            return -1;
        }
    }

    public static class Day implements Comparable<Day> {
        public static enum DayType {
            ON,            // on day N
            WEEKNUM,       // on Nth weekday (N=-1 => last week of the month)
            BEFORE,        // W<D (last weekday W before date D; e.g. Sun<8)
            ON_OR_BEFORE,  // W<=D (last weekday W on or before date D; e.g. Sun<=8)
            ON_OR_AFTER,   // W>=D (first weekday W on or after date D; e.g. Sun>=8)
            AFTER;         // W>D (first weekday W after date D; e.g. Sun>8)
        }

        private DayType mType;
        private int mWeeknum;      // used only when mType == WEEKNUM
        private Weekday mWeekday;  // ignored when mType == ON
        private int mDate;         // ignored when mType == WEEKNUM

        public Day(int date) {
            mType = DayType.ON;
            mDate = date;
        }

        public Day(int weeknum, Weekday wkday) {
            mType = DayType.WEEKNUM;
            mWeeknum = weeknum;
            mWeekday = wkday;
            mDate = 0;
        }

        public Day(String str) throws TZDataParseException {
            try {
                int d = Integer.parseInt(str);
                mType = DayType.ON;
                mDate = d;
            } catch (NumberFormatException e) {}

            if (mType == null) {
                String val = str.toLowerCase();
                if (val.startsWith("last")) {
                    mType = DayType.WEEKNUM;
                    mWeeknum = -1;
                    mWeekday = Weekday.lookUp(val.substring(4));
                    if (mWeekday == null)
                        throw new TZDataParseException("Invalid day \"" + str + "\"");
                } else {
                    String[] fields;
                    if (str.contains("<=")) {
                        mType = DayType.ON_OR_BEFORE;
                        fields = str.split("<=", 2);
                    } else if (str.contains(">=")) {
                        mType = DayType.ON_OR_AFTER;
                        fields = str.split(">=", 2);
                    } else if (str.contains("<")) {
                        mType = DayType.BEFORE;
                        fields = str.split("<", 2);
                    } else if (str.contains(">")){
                        mType = DayType.AFTER;
                        fields = str.split(">", 2);
                    } else {
                        throw new TZDataParseException("Invalid day \"" + str + "\"");
                    }
                    if (fields.length < 2)
                        throw new TZDataParseException("Invalid day \"" + str + "\"");
                    mWeekday = Weekday.lookUp(fields[0]);
                    if (mWeekday == null)
                        throw new TZDataParseException("Invalid day \"" + str + "\"");
                    try {
                        mDate = Integer.parseInt(fields[1]);
                    } catch (NumberFormatException e) {
                        throw new TZDataParseException("Invalid day \"" + str + "\"");
                    }
                }
            }
        }

        public DayType getType() { return mType; }
        public int getWeeknum() { return mWeeknum; }
        public int getDate() { return mDate; }
        public Weekday getWeekday() { return mWeekday; }

        public String toString() {
            if (DayType.ON.equals(mType))
                return Integer.toString(mDate);

            String wday = mWeekday.toString();
            wday = wday.substring(0, 1) + wday.toLowerCase().substring(1, 3);

            if (DayType.WEEKNUM.equals(mType)) {
                if (mWeeknum == -1)
                    return "last" + wday;
                return Integer.toString(mWeeknum) + wday;
            }

            String oper = null;
            switch (mType) {
            case BEFORE:
                oper = "<";
                break;
            case ON_OR_BEFORE:
                oper = "<=";
                break;
            case ON_OR_AFTER:
                oper = ">=";
                break;
            case AFTER:
                oper = ">";
                break;
            }

            if (oper != null)
                return wday + oper + Integer.toString(mDate);

            return "type=" + mType + ", weeknum=" + mWeeknum + ", wkday=" + mWeekday + ", date=" + mDate;
        }

        public int getDateIn(int year, int month) {
            if (DayType.ON.equals(mType))
                return mDate;
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.set(year, month - 1, 1, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            int numModays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int wkday = Weekday.toInt(mWeekday);
            if (DayType.WEEKNUM.equals(mType)) {
                if (mWeeknum >= 1 && mWeeknum <= 5) {
                    int firstDate = Math.min(mWeeknum * 7 + 1, numModays);
                    int lastDate = Math.min(firstDate + 6, numModays);
                    for (int i = firstDate; i <= lastDate; ++i) {
                        cal.set(Calendar.DAY_OF_MONTH, i);
                        if (cal.get(Calendar.DAY_OF_WEEK) == wkday)
                            return i;
                    }
                } else if (mWeeknum == -1) {
                    int firstDate = Math.max(numModays, 1);
                    int lastDate = Math.max(numModays - 6, 1);
                    for (int i = firstDate; i >= lastDate; --i) {
                        cal.set(Calendar.DAY_OF_MONTH, i);
                        if (cal.get(Calendar.DAY_OF_WEEK) == wkday)
                            return i;
                    }
                }
            } else {
                int date = mDate;
                DayType dtype = mType;
                if (DayType.BEFORE.equals(dtype)) {
                    --date;
                    dtype = DayType.ON_OR_BEFORE;
                } else if (DayType.AFTER.equals(dtype)) {
                    ++date;
                    dtype = DayType.ON_OR_AFTER;
                }
                if (DayType.ON_OR_BEFORE.equals(dtype)) {
                    int firstDate = Math.min(date, 1);
                    int lastDate = Math.min(firstDate - 6, 1);
                    for (int i = firstDate; i >= lastDate; --i) {
                        cal.set(Calendar.DAY_OF_MONTH, i);
                        if (cal.get(Calendar.DAY_OF_WEEK) == wkday)
                            return i;
                    }
                } else if (DayType.ON_OR_AFTER.equals(dtype)) {
                    int firstDate = Math.min(date, numModays);
                    int lastDate = Math.min(firstDate + 6, numModays);
                    for (int i = firstDate; i <= lastDate; ++i) {
                        cal.set(Calendar.DAY_OF_MONTH, i);
                        if (cal.get(Calendar.DAY_OF_WEEK) == wkday)
                            return i;
                    }
                }
            }
            return 1;
        }

        private int estimateWeeknum() {
            if (mWeeknum != 0)
                return mWeeknum;
            if (DayType.ON.equals(mType))
                return (mDate - 1) / 7 + 1;
            int date = mDate;
            DayType dtype = mType;
            if (DayType.AFTER.equals(dtype)) {
                ++date;
                dtype = DayType.ON_OR_AFTER;
            } else if (DayType.BEFORE.equals(dtype)) {
                --date;
                dtype = DayType.ON_OR_BEFORE;
            }
            if (DayType.ON_OR_BEFORE.equals(dtype))
                date -= 6;  // go to beginning of the week
            date = Math.max(date, 1);
            return (date - 1) / 7 + 1;
        }

        // Comparison order:
        // 1) week number
        //    If mWeeknum==0, weeknum is derived from mType and mDate assuming a 7-day range.
        // 2) DayType
        // 3) weekday
        // Ordering for Day has no intrinsic meaning.  We just need something consistent.
        public int compareTo(Day other) {
            if (other == null)
                throw new NullPointerException();

            int comp;
            int wknum = estimateWeeknum();
            if (wknum == -1) wknum = 6;
            int wknumOther = other.estimateWeeknum();
            if (wknumOther == -1) wknum = 6;
            comp = wknum - wknumOther;
            if (comp != 0)
                return comp;

            DayType dt[] = { mType, other.getType() };
            int dtIndex[] = new int[2];
            for (int i = 0; i < dt.length; ++i) {
                switch (dt[i]) {
                case ON:
                    dtIndex[i] = 0;
                    break;
                case BEFORE:
                    dtIndex[i] = 1;
                    break;
                case ON_OR_BEFORE:
                    dtIndex[i] = 2;
                    break;
                case ON_OR_AFTER:
                    dtIndex[i] = 3;
                    break;
                case AFTER:
                    dtIndex[i] = 4;
                    break;
                default:
                    dtIndex[i] = 5;
                }
            }
            comp = dtIndex[0] - dtIndex[1];
            if (comp != 0)
                return comp;

            return Weekday.toInt(mWeekday) - Weekday.toInt(other.getWeekday());
        }
    }

    // Represents either duration in seconds, or time in seconds from midnight.
    public static class Time implements Comparable<Time> {
        public static enum TimeType { WALL_TIME, STANDARD_TIME, UTC_TIME }

        private int mHour;
        private int mMin;
        private int mSec;
        private boolean mNegative;
        private TimeType mType;

        public Time(boolean negative, int hour, int min, int sec, TimeType type) {
            mNegative = negative;
            mHour = hour;
            mMin = min;
            mSec = sec;
            mType = type;
            if (mHour == 0 && mMin == 0 && mSec == 0)
                mNegative = false;
            normalize();
        }

        // 2       time in hours
        // 2:00    time in hours and minutes
        // 15:00   24-hour format time (for times after noon)
        // 1:28:14 time in hours, minutes, and seconds
        // optional sign prefix ('+' or '-')
        // optional clock type indicator suffix 'w' (default), 's' or 'u'
        public Time(String str) throws TZDataParseException {
            char suffix = str.toLowerCase().charAt(str.length() - 1);
            String val;
            if (suffix >= '0' && suffix <= '9') {
                mType = TimeType.WALL_TIME;
                val = str;
            } else {
                if (suffix == 'w')
                    mType = TimeType.WALL_TIME;
                else if (suffix == 's')
                    mType = TimeType.STANDARD_TIME;
                else if (suffix == 'u')
                    mType = TimeType.UTC_TIME;
                else
                    throw new TZDataParseException("Invalid time type \"" + suffix + "\"");
                val = str.substring(0, str.length() - 1);
            }
            char sign = val.charAt(0);
            if (sign == '-') {
                mNegative = true;
                val = val.substring(1);
            } else if (sign == '+') {
                val = val.substring(1);
            }
            String[] fields = val.split(":");
            try {
                mHour = Integer.parseInt(fields[0]);
                if (fields.length >= 2)
                    mMin = Integer.parseInt(fields[1]);
                if (fields.length == 3)
                    mSec = Integer.parseInt(fields[2]);
                else if (fields.length > 3)
                    throw new TZDataParseException("Invalid time value \"" + str + "\"");
            } catch (NumberFormatException e) {
                throw new TZDataParseException("Invalid time value \"" + str + "\"");
            }

            if (mHour == 0 && mMin == 0 && mSec == 0)
                mNegative = false;

            normalize();
        }

        public boolean isNegative() { return mNegative; }
        public int getHour() { return mHour; }
        public int getMinute() { return mMin; }
        public int getSecond() { return mSec; }
        public int getDuration() { return (mHour * 3600 + mMin * 60 + mSec) * (mNegative ? -1 : 1); }
        public TimeType getType() { return mType; }

        private void normalize() {
            // Keep minutes and seconds under 60.
            if (mSec >= 60) {
                mMin += mSec / 60;
                mSec %= 60;
            }
            if (mMin >= 60) {
                mHour += mMin / 60;
                mMin %= 60;
            }
        }

        public String toString() {
            String suffix;
            switch (mType) {
            case STANDARD_TIME:
                suffix = "s";
                break;
            case UTC_TIME:
                suffix = "u";
                break;
            default:
                suffix = "";
            }
            String sign = mNegative ? "-" : "";
            if (mSec != 0)
                return String.format("%s%d:%02d:%02d%s", sign, mHour, mMin, mSec, suffix);
            else
                return String.format("%s%d:%02d%s", sign, mHour, mMin, suffix);
        }

        public int compareTo(Time other) {
            if (other == null)
                throw new NullPointerException();

            int duration = getDuration();
            int otherDuration = other.getDuration();
            if (duration != otherDuration)
                return duration - otherDuration;

            // Force uniform (but meaningless) ordering of time types.
            int tt, ttOther;
            if (mType != null) {
                switch (mType) {
                case WALL_TIME:
                    tt = 0;
                    break;
                case STANDARD_TIME:
                    tt = 1;
                    break;
                case UTC_TIME:
                    tt = 2;
                    break;
                default:
                    tt = 4;
                }
            } else
                tt = 5;
            TimeType otherType = other.getType();
            if (otherType != null) {                
                switch (mType) {
                case WALL_TIME:
                    ttOther = 0;
                    break;
                case STANDARD_TIME:
                    ttOther = 1;
                    break;
                case UTC_TIME:
                    ttOther = 2;
                    break;
                default:
                    ttOther = 4;
                }
            } else
                ttOther = 5;
            return tt - ttOther;
        }
    }

    public static class Until implements Comparable<Until> {
        private int mYear;
        private int mMonth;
        private Day mDay;
        private Time mTime;

        public Until(List<String> tokens) throws TZDataParseException {
            int numFields = tokens.size();
            if (numFields < 1)
                throw new TZDataParseException("Missing year in UNTIL");
            mYear = Year.parseYear(tokens.get(0));
            if (numFields >= 2)
                mMonth = Month.parseMonth(tokens.get(1));
            else
                mMonth = 1;
            if (numFields >= 3)
                mDay = new Day(tokens.get(2));
            else
                mDay = new Day(1);
            if (numFields >= 4)
                mTime = new Time(tokens.get(3));
            else
                mTime = new Time(false, 0, 0, 0, Time.TimeType.WALL_TIME);
        }

        public int getYear() { return mYear; }
        public int getMonth() { return mMonth; }
        public Day getDay() { return mDay; }
        public Time getTime() { return mTime; }

        public String toString() {
            String mon = Month.toString(mMonth);
            String day = mDay.toString();
            if (mTime.getDuration() != 0)
                return String.format("%d %s %s %s", mYear, mon, day, mTime);
            else
                return String.format("%d %s %s", mYear, mon, day);
        }

        // Comparison order:
        // 1) year
        // 2) month
        // 3) day
        // 4) time
        public int compareTo(Until other) {
            if (other == null)
                throw new NullPointerException();

            int comp;

            // year
            // Don't subtract because mYear can be Integer.MIN_VALUE.
            int otherYear = other.getYear();
            if (mYear < otherYear)
                return -1;
            else if (mYear > otherYear)
                return 1;

            // month
            comp = mMonth - other.getMonth();
            if (comp != 0)
                return comp;

            // day
            comp = nullsCompare(mDay, other.getDay(), true);
            if (comp != 0)
                return comp;

            // time
            return nullsCompare(mTime, other.getTime(), true);
        }
    }

    public static class RuleLine {
        private String mName;
        private int mFromYear;
        private int mToYear;
        private String mType;
        private int mIn;
        private Day mOn;
        private Time mAt;
        private Time mSave;
        private String mLetter;

        public RuleLine(List<String> tokens) throws TZDataParseException {
            if (tokens.size() < 9)
                throw new TZDataParseException("Not enough fields in a Rule line");
            mName = tokens.get(0);
            mFromYear = Year.parseYear(tokens.get(1));
            String toYear = tokens.get(2);
            if ("only".equalsIgnoreCase(toYear))
                mToYear = mFromYear;
            else
                mToYear = Year.parseYear(toYear);
            mType = tokens.get(3);
            mIn = Month.parseMonth(tokens.get(4));
            mOn = new Day(tokens.get(5));
            mAt = new Time(tokens.get(6));
            mSave = new Time(tokens.get(7));
            mLetter = tokens.get(8);
        }

        public String getName() { return mName; }
        public int getFromYear() { return mFromYear; }
        public int getToYear() { return mToYear; }
        public String getType() { return mType; }
        public int getIn() { return mIn; }
        public Day getOn() { return mOn; }
        public Time getAt() { return mAt; }
        public Time getSave() { return mSave; }
        public String getLetter() { return mLetter; }

        public boolean isCurrent() { return mToYear == Integer.MAX_VALUE; }

        public String toString() {
            String fromYear, toYear;
            if (mFromYear == Integer.MAX_VALUE)
                fromYear = "max";
            else if (mFromYear == Integer.MIN_VALUE)
                fromYear = "min";
            else
                fromYear = Integer.toString(mFromYear);
            if (mToYear == mFromYear)
                toYear = "only";
            else if (mToYear == Integer.MAX_VALUE)
                toYear = "max";
            else if (mToYear == Integer.MIN_VALUE)
                toYear = "min";
            else
                toYear = Integer.toString(mToYear);
            String in = Month.toString(mIn);
            return String.format("%s %s %s %s %s %s %s %s %s",
                                 mName, fromYear, toYear, mType, in, mOn, mAt, mSave, mLetter);
        }
    }

    public static class ZoneLine implements Comparable<ZoneLine> {
        public static enum RuleSaveType { RULE, SAVE };

        private String mName;
        private Time mGmtOff;  // in seconds
        private RuleSaveType mRuleSaveType;
        private String mRuleName;
        private Time mSave;
        private String mAbbrevFormat;
        private Until mUntil;
        private Rule mRule;

        public ZoneLine(List<String> tokens) throws TZDataParseException {
            int numFields = tokens.size();
            if (numFields < 4)
                throw new TZDataParseException("Not enough fields in a Zone line");
            mName = tokens.get(0);
            if ("Etc/UTC".equalsIgnoreCase(mName)) {
                // Map Etc/UTC to built-in "UTC" time zone.
                mName = "UTC";
            }
            mGmtOff = new Time(tokens.get(1));
            String ruleSave = tokens.get(2);
            if (ruleSave.equals("-")) {
                mRuleSaveType = RuleSaveType.SAVE;
                mSave = new Time(false, 0, 0, 0, Time.TimeType.WALL_TIME);
            } else {
                try {
                    mSave = new Time(ruleSave);
                    mRuleSaveType = RuleSaveType.SAVE;
                } catch (TZDataParseException e) {
                    mRuleSaveType = RuleSaveType.RULE;
                    mRuleName = ruleSave;
                }
            }
            mAbbrevFormat = tokens.get(3);
            if (numFields > 4)
                mUntil = new Until(tokens.subList(4, numFields));
        }

        public boolean hasUntil() { return mUntil != null; }

        public String getName() { return mName; }
        public Time getGmtOff() { return mGmtOff; }
        private String getRuleName() { return mRuleName; }
        public boolean hasRule() { return RuleSaveType.RULE.equals(mRuleSaveType); }
        public boolean hasSave() { return RuleSaveType.SAVE.equals(mRuleSaveType); }
        public String getAbbrevFormat() { return mAbbrevFormat; }
        public Until getUntil() { return mUntil; }

        private void setRule(Rule rule) { mRule = rule; }
        public Rule getRule() { return mRule; }
        public Time getSave() { return mSave; }

        public String toString() {
            String ruleSave;
            if (RuleSaveType.RULE.equals(mRuleSaveType)) {
                ruleSave = mRuleName;
            } else {
                if (mSave.getDuration() == 0)
                    ruleSave = "-";
                else
                    ruleSave = mSave.toString();
            }
            if (mUntil != null)
                return String.format("%s %s %s %s %s", mName, mGmtOff, ruleSave, mAbbrevFormat, mUntil);
            else
                return String.format("%s %s %s %s", mName, mGmtOff, ruleSave, mAbbrevFormat);
        }

        public long getUntilInMillis() {
            if (mUntil == null)
                return Long.MAX_VALUE;
            int year = mUntil.getYear();
            int month = mUntil.getMonth();
            int moday = mUntil.getDay().getDateIn(year, month);
            Time ut = mUntil.getTime();
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.set(year, month - 1, moday, ut.getHour(), ut.getMinute(), ut.getSecond());
            cal.set(Calendar.MILLISECOND, 0);
            long millis = cal.getTimeInMillis();

            // Simplify: Let's assume hh:mm:ss always denotes STANDARD time, not WALL/UTC.
            millis -= mGmtOff.getDuration() * 1000;
            return millis;
        }

        // Comparison order:
        // 1) NAME
        // 2) UNTIL (null UNTIL is later than any non-null UNTIL)
        // 3) GMTOFF
        // 4) SAVE
        // 4) RULES
        // 6) FORMAT
        // GMTOFF/SAVE/RULES/FORMAT ordering is meaningless; done only for consistent ordering.
        public int compareTo(ZoneLine other) {
            if (other == null)
                throw new NullPointerException();

            int comp;

            // NAME
            comp = nullsCompare(mName, other.getName(), true);
            if (comp != 0)
                return comp;

            // UNTIL
            comp = nullsCompare(mUntil, other.getUntil(), false);
            if (comp != 0)
                return comp;

            // GMTOFFSET
            int offset = mGmtOff != null ? mGmtOff.getDuration() : 0;
            int otherOffset = other.getGmtOff() != null ? other.getGmtOff().getDuration() : 0;
            comp = offset - otherOffset;
            if (comp != 0)
                return comp;

            // SAVE
            Time otherSave = other.getSave();
            if (mSave != null) {
                if (otherSave != null) {
                    comp = mSave.getDuration() - otherSave.getDuration();
                    if (comp != 0)
                        return comp;
                } else
                    return 1;
            } else if (otherSave != null) {
                return -1;
            }

            // RULES
            comp = nullsCompare(mRuleName, other.getRuleName(), true);
            if (comp != 0)
                return comp;

            // FORMAT
            return nullsCompare(mAbbrevFormat, other.getAbbrevFormat(), true);
        }
    }

    public static class Leap {
        private int mYear;
        private int mMonth;             // 1=January, ..., 12=December
        private int mDay;
        private Time mTime;
        private boolean mCorrPositive;  // true=+, false=-
        private boolean mRolling;       // true=Rolling, false=Stationary

        public Leap(List<String> tokens) throws TZDataParseException {
            if (tokens.size() < 6)
                throw new TZDataParseException("Not enough fields in a Leap line");
            mYear = Year.parseYear(tokens.get(0));
            mMonth = Month.parseMonth(tokens.get(1));
            try {
                mDay = Integer.parseInt(tokens.get(2));
            } catch (NumberFormatException e) {
                throw new TZDataParseException("Invalid day of the month \"" + tokens.get(2) + "\"");
            }
            mTime = new Time(tokens.get(3));
            char corr = tokens.get(4).charAt(0);
            if (corr == '+')
                mCorrPositive = true;
            else if (corr == '-')
                mCorrPositive = false;
            else
                throw new TZDataParseException("Invalid CORR value \"" + tokens.get(4) + "\"");
            String rs = tokens.get(5).toLowerCase();
            if ("rolling".startsWith(rs))
                mRolling = true;
            else if ("stationary".startsWith(rs))
                mRolling = false;
            else
                throw new TZDataParseException("Invalid R/S value \"" + tokens.get(5) + "\"");
        }

        public int getYear() { return mYear; }
        public int getMonth() { return mMonth; }
        public int getDay() { return mDay; }
        public Time getTime() { return mTime; }
        public boolean isCorrPositive() { return mCorrPositive; }
        public boolean isRolling() { return mRolling; }
    }

    public static class Rule {
        private String mName;
        private List<RuleLine> mRuleLines;

        public Rule(String name) {
            mName = name;
            mRuleLines = new ArrayList<RuleLine>();
        }

        public String getName() { return mName; }
        private void addRuleLine(RuleLine rl) { mRuleLines.add(rl); }
        public List<RuleLine> getRuleLines() { return mRuleLines; }
    }

    public static class Zone {
        private String mName;
        private Set<ZoneLine> mZoneLines;
        private Set<String> mAliases;

        public Zone(String name) {
            mName = name;
            mZoneLines = new TreeSet<ZoneLine>();
            mAliases = new TreeSet<String>();  // sorted
        }

        public String getName() { return mName; }
        public Set<ZoneLine> getZoneLines() { return mZoneLines; }
        public Set<String> getAliases() { return mAliases; }

        private void addZoneLine(ZoneLine zl) { mZoneLines.add(zl); }
        private void addAlias(String alias) { mAliases.add(alias); }
    }


    private boolean mAnalyzed;
    private Map<String /* Rule NAME */, Rule> mRules;
    private Map<String /* Zone NAME */, Zone> mZones;
    private Map<String /* LINK TO (alias) */, String /* LINK FROM (real TZ) */> mLinks;
    private List<Leap> mLeaps;

    public ZoneInfoParser() {
        mRules = new HashMap<String, Rule>();
        mZones = new HashMap<String, Zone>();
        mLinks = new HashMap<String, String>();
        mLinks.put("Etc/UTC", "UTC");  // Map Etc/UTC to built-in "UTC" time zone.
        mLeaps = new ArrayList<Leap>();
    }

    private static enum LineType {
        RULE, ZONE, LINK, LEAP, UNKNOWN;

        public static LineType lookUp(String str) {
            LineType lt = UNKNOWN;
            if (str != null) {
                try {
                    lt = valueOf(str.toUpperCase());
                } catch (IllegalArgumentException e) {}
            }
            return lt;
        }
    }

    public void readTzdata(Reader reader) throws IOException, ParseException {
        char dquote = '"';
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        tokenizer.resetSyntax();
        tokenizer.wordChars(32, 126);
        tokenizer.whitespaceChars(' ', ' ');
        tokenizer.whitespaceChars('\t', '\t');
        tokenizer.whitespaceChars(0, 20);
        tokenizer.commentChar('#');
        tokenizer.quoteChar(dquote);
        tokenizer.eolIsSignificant(true);

        List<String> tokenList = new ArrayList<String>();
        LineType lineType = LineType.UNKNOWN;
        boolean atLineStart = true;

        int ttype;
        int prevTtype = StreamTokenizer.TT_EOL;  // used for empty line detection
        while ((ttype = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            int lineNum = tokenizer.lineno();
            try {
                if (ttype == StreamTokenizer.TT_WORD || ttype == dquote) {
                    String token = tokenizer.sval;
                    if (atLineStart) {
                        lineType = LineType.lookUp(token);
                        if (LineType.UNKNOWN.equals(lineType))
                            throw new ParseException("Invalid line type", lineNum);
                    } else {
                        tokenList.add(token);
                    }
                    atLineStart = false;
                } else if (ttype == StreamTokenizer.TT_EOL) {
                    if (prevTtype == StreamTokenizer.TT_EOL) {
                        prevTtype = ttype;
                        continue;
                    }
                    atLineStart = true;
                    switch (lineType) {
                    case RULE:
                        RuleLine rl = new RuleLine(tokenList);
                        String rname = rl.getName();
                        Rule rule = mRules.get(rname);
                        if (rule == null) {
                            rule = new Rule(rname);
                            mRules.put(rname, rule);
                        }
                        rule.addRuleLine(rl);
                        break;
                    case ZONE:
                        ZoneLine zl = new ZoneLine(tokenList);
                        String zname = zl.getName();
                        Zone zone = mZones.get(zname);
                        if (zone == null) {
                            zone = new Zone(zname);
                            mZones.put(zname, zone);
                        }
                        zone.addZoneLine(zl);
                        // If Zone line had UNTIL, next line is a continuation of the same Zone.
                        if (zl.hasUntil()) {
                            atLineStart = false;
                            tokenList.clear();
                            tokenList.add(zname);
                        }
                        break;
                    case LINK:
                        if (tokenList.size() < 2)
                            throw new ParseException("Not enough fields in a Link line", lineNum);
                        String real = tokenList.get(0);
                        String alias = tokenList.get(1);
                        if (!alias.equals(real))
                            mLinks.put(alias, real);
                        break;
                    case LEAP:
                        Leap leap = new Leap(tokenList);
                        mLeaps.add(leap);
                        break;
                    }
                    if (atLineStart) {
                        tokenList.clear();
                        lineType = LineType.UNKNOWN;
                    }
                } else if (ttype == StreamTokenizer.TT_NUMBER) {
                    // shouldn't happen
                    throw new ParseException("Invalid parser state: TT_NUMBER found", lineNum);
                }
                prevTtype = ttype;
            } catch (TZDataParseException e) {
                ParseException pe = new ParseException("Parse error: " + e.getMessage(), lineNum);
                pe.initCause(e);
                throw pe;
            }
        }
    }

    public void analyze() {
        // Link rules to zones.
        for (Iterator<Entry<String, Zone>> ziter = mZones.entrySet().iterator(); ziter.hasNext(); ) {
            Entry<String, Zone> zentry = ziter.next();
            Zone zone = zentry.getValue();
            Set<ZoneLine> zlines = zone.getZoneLines();
            for (ZoneLine zline : zlines) {
                if (zline.hasRule()) {
                    String rname = zline.getRuleName();
                    Rule rule = mRules.get(rname);
                    if (rule == null) {
                        System.err.println("Unknown rule: " + rname);
                        continue;
                    }
                    zline.setRule(rule);
                }
            }
        }

        // Flatten links map.
        List<String> aliasesToRemove = new ArrayList<String>();
        Map<String, String> flattenedLinks = new HashMap<String, String>();
        for (Iterator<Entry<String, String>> liter = mLinks.entrySet().iterator(); liter.hasNext(); ) {
            Entry<String, String> lentry = liter.next();
            String alias = lentry.getKey();
            String real = lentry.getValue();
            if (!mZones.containsKey(real)) {
                aliasesToRemove.add(alias);
                while ((real = mLinks.get(real)) != null) {
                    if (mZones.containsKey(real)) {
                        if (!alias.equals(real))
                            flattenedLinks.put(alias, real);
                        break;
                    }
                }
            }
        }
        for (String alias : aliasesToRemove) {
            mLinks.remove(alias);
        }
        for (Iterator<Entry<String, String>> liter = flattenedLinks.entrySet().iterator(); liter.hasNext(); ) {
            Entry<String, String> lentry = liter.next();
            String alias = lentry.getKey();
            String real = lentry.getValue();
            mLinks.put(alias, real);
        }

        // Register the aliases to zones.
        for (Iterator<Entry<String, String>> liter = mLinks.entrySet().iterator(); liter.hasNext(); ) {
            Entry<String, String> lentry = liter.next();
            String alias = lentry.getKey();
            String real = lentry.getValue();
            Zone zone = mZones.get(real);
            if (zone != null)
                zone.addAlias(alias);
            else
                System.err.println("Invalid state!  Link \"" + alias + "\" points to a non-existent zone \"" + real + "\".");
        }

        mAnalyzed = true;
    }

    public Zone getZone(String zoneName) throws TZDataParseException {
        if (!mAnalyzed)
            throw new TZDataParseException("not alayzed yet");

        Zone zone = mZones.get(zoneName);
        if (zone != null)
            return zone;
        String realZoneName = mLinks.get(zoneName);
        if (realZoneName != null)
            return mZones.get(realZoneName);
        return null;
    }

    public Collection<Zone> getZones() throws TZDataParseException {
        if (!mAnalyzed)
            throw new TZDataParseException("not alayzed yet");
        return mZones.values();
    }

    public static void main(String[] args) throws Exception {
        ZoneInfoParser parser = new ZoneInfoParser();
        for (String fname : args) {
            File f = new File(fname);
            System.out.println("Processing: " + fname);
            Reader r = new FileReader(f);
            try {
                parser.readTzdata(r);
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                System.err.println("Line: " + e.getErrorOffset());
                e.printStackTrace();
            } finally {
                r.close();
            }
        }
    }
}
