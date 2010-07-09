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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;

public final class ParsedDuration
{
    static public final int WEEKS = 1;
    static public final int DAYS = 2;
    static public final int HOURS = 3;
    static public final int MINUTES = 4;
    static public final int SECONDS = 5;
    
    private int mWeeks = 0;
    private int mDays = 0;
    private int mHours = 0;
    private int mMins = 0;
    private int mSecs = 0;
    private boolean mNegative = false;
    
    static Pattern sDurationWeekPattern = Pattern.compile("([+-]?P)(\\d+)W");
    static Pattern sDurationDayTimePattern = Pattern.compile("([+-]?P)(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?");
    
    public static final ParsedDuration ONE_DAY = new ParsedDuration(false, 0, 1, 0, 0, 0);
    public static final ParsedDuration NEGATIVE_ONE_DAY = new ParsedDuration(false, 0, -1, 0, 0, 0);
    public static final ParsedDuration ONE_WEEK = new ParsedDuration(false, 1, 0, 0, 0, 0);
    public static final ParsedDuration ONE_SECOND = new ParsedDuration(false, 0, 0, 0, 0, 1);
    public static final ParsedDuration NEGATIVE_ONE_SECOND = new ParsedDuration(true, 0, 0, 0, 0, 1);

    private ParsedDuration(boolean negative, int weeks, int days, int hours, int mins, int secs) {
        mNegative = negative;
        mWeeks = weeks;
        mDays = days;
        mHours = hours;
        mMins = mins;
        mSecs = secs;
    }
    
    public Object clone() {
        ParsedDuration toRet = new ParsedDuration();
        toRet.mWeeks = mWeeks;
        toRet.mDays = mDays;
        toRet.mHours = mHours;
        toRet.mMins = mMins;
        toRet.mSecs = mSecs;
        toRet.mNegative = mNegative;
        return toRet;
    }

    private static final int SECONDS_IN_MINUTE = 60;
    private static final int SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60;
    private static final int SECONDS_IN_DAY = SECONDS_IN_HOUR * 24;
    private static final int SECONDS_IN_WEEK = SECONDS_IN_DAY * 7;

    // Don't make this a public method because adding this seconds value to ParsedDateTime is not DST-safe.
    private int asSeconds() {
        return
            (mWeeks * SECONDS_IN_WEEK +
             mDays * SECONDS_IN_DAY +
             mHours * SECONDS_IN_HOUR +
             mMins * SECONDS_IN_MINUTE +
             mSecs) *
            (mNegative ? -1 : 1);
    }

    public int compareTo(ParsedDuration other) {
        return asSeconds() - other.asSeconds();
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof ParsedDuration))
            return false;
        return (compareTo((ParsedDuration) other) == 0);
    }
   
    public ParsedDuration add(int field, int amount) throws ServiceException {
        ParsedDuration dur = (ParsedDuration)clone();
        
        switch (field) {
        case WEEKS:
            dur.mWeeks+=amount;
            break;
        case DAYS:
            dur.mDays+=amount;
            break;
        case HOURS:
            dur.mHours+=amount;
            break;
        case MINUTES:
            dur.mMins+=amount;
            break;
        case SECONDS:
            dur.mSecs+=amount;
            break;
        default:
            throw ServiceException.INVALID_REQUEST("Unknown field in ParsedDuration.add("+field+","+amount+")",null);
        }
        return dur;
    }

    /**
     * Returns a new ParsedDuration object that is an absolute value of this duration.
     * @return
     */
    public ParsedDuration abs() {
        return ParsedDuration.parse(false, mWeeks, mDays, mHours, mMins, mSecs);
    }

    public String toString() {
        String start = "P";
        if (mNegative) {
            start = "-P";
        }
        
        StringBuffer toRet = new StringBuffer(start);
        if (mWeeks != 0) {
            assert(mDays == 0 && mHours == 0 && mMins == 0 && mSecs == 0);
            toRet.append(mWeeks+"W");
            return toRet.toString();
        }
        if (mDays != 0) {
            toRet.append(mDays+"D");
            if (mHours == 0 && mMins == 0 && mSecs == 0) {
                return toRet.toString();
            }
        }
        toRet.append("T");
        if (mHours!= 0) {
            toRet.append(mHours+"H");
        }
        if (mMins!= 0) {
            toRet.append(mMins+"M");
        }
        if (mSecs!= 0) {
            toRet.append(mSecs+"S");
        }
        return toRet.toString();
    }
    
//    public static ParsedDuration parse(Property prop) throws ServiceException
//    {
//        return parse(((Duration)prop).getValue());
//    }
    
    public static ParsedDuration parse(boolean negative, int weeks, int days, int hours, int mins, int secs) {
        ParsedDuration toRet = new ParsedDuration();
        if (negative) {
            toRet.mNegative = true;
        } else {
            toRet.mNegative = false;
        }
        toRet.mWeeks = weeks;
        toRet.mDays = days;
        toRet.mHours = hours;
        toRet.mMins = mins;
        toRet.mSecs = secs;
        return toRet; 
    }
    
    private ParsedDuration()
    {
        mNegative = false;
        mWeeks = 0;
        mDays = 0;
        mHours = 0;
        mMins = 0;
        mSecs = 0;
    }
    
    public Element toXml(Element parent) {
        return toXml(parent, MailConstants.E_CAL_DURATION);
    }

    public Element toXml(Element parent, String name) {
        Element elt = parent.addElement(name);
        if (mNegative) {
            elt.addAttribute(MailConstants.A_CAL_DURATION_NEGATIVE, true);
        }
        
        if (mWeeks > 0) {
            elt.addAttribute(MailConstants.A_CAL_DURATION_WEEKS, mWeeks);
        } else {
            if (mDays > 0) {
                elt.addAttribute(MailConstants.A_CAL_DURATION_DAYS, mDays);
            }
            if (mHours> 0) {
                elt.addAttribute(MailConstants.A_CAL_DURATION_HOURS, mHours);
            }
            if (mMins> 0) {
                elt.addAttribute(MailConstants.A_CAL_DURATION_MINUTES, mMins);
            }
            if (mSecs> 0) {
                elt.addAttribute(MailConstants.A_CAL_DURATION_SECONDS, mSecs);
            }
        }
        return elt;
    }
    
    public static ParsedDuration parse(Element elt) throws ServiceException {
        ParsedDuration toRet = new ParsedDuration();
        toRet.mNegative = elt.getAttributeBool(MailConstants.A_CAL_DURATION_NEGATIVE, false);
        toRet.mWeeks = (int)elt.getAttributeLong(MailConstants.A_CAL_DURATION_WEEKS, 0);
        toRet.mDays= (int)elt.getAttributeLong(MailConstants.A_CAL_DURATION_DAYS, 0);
        toRet.mHours= (int)elt.getAttributeLong(MailConstants.A_CAL_DURATION_HOURS, 0);
        toRet.mMins = (int)elt.getAttributeLong(MailConstants.A_CAL_DURATION_MINUTES, 0);
        toRet.mSecs = (int)elt.getAttributeLong(MailConstants.A_CAL_DURATION_SECONDS, 0);
        
        if (toRet.mDays!=0 || toRet.mHours!=0 || toRet.mMins!=0 || toRet.mSecs!=0) {
            if (toRet.mWeeks != 0) {
                throw ServiceException.FAILURE("Weeks may not be specified with other granularity in duration element: "+
                        elt.getName(), null);
            }
        }
        
        return toRet;
    }

    private Date addToDate(Date date, boolean subtract) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int mult = mNegative ? -1 : 1;
        if (subtract)
            mult *= -1;
        cal.add(java.util.Calendar.WEEK_OF_YEAR, mult*mWeeks);
        cal.add(java.util.Calendar.DAY_OF_YEAR, mult*mDays);
        cal.add(java.util.Calendar.HOUR_OF_DAY, mult*mHours);
        cal.add(java.util.Calendar.MINUTE, mult*mMins);
        cal.add(java.util.Calendar.SECOND, mult*mSecs);

        return cal.getTime();
    }

    public Date addToDate(Date date) {
        return addToDate(date, false);
    }

    public Date subtractFromDate(Date date) {
        return addToDate(date, true);
    }

    public long addToTime(long utcTime) {
        return addToDate(new Date(utcTime), false).getTime();
    }

    public long subtractFromTime(long utcTime) {
        return addToDate(new Date(utcTime), true).getTime();
    }

    public static ParsedDuration parse(String durationStr) throws ServiceException {
        if (durationStr == null)
            return null;
        ParsedDuration retVal = new ParsedDuration();
        
        Matcher m = sDurationWeekPattern.matcher(durationStr);
        if (m.matches()) {
            String p = m.group(1);
            String wk = m.group(2);
            if (p.charAt(0) == '-')
                retVal.mNegative = true;
            retVal.mWeeks = Integer.parseInt(wk);
        } else {
            m = sDurationDayTimePattern.matcher(durationStr);
            if (m.matches()) {
                // P, Days, Hours, Mins, Secs
                String p = m.group(1);
                if (p.charAt(0) == '-') {
                    retVal.mNegative = true;
                }
                String da = m.group(2);
                if (da != null) {
                    retVal.mDays = Integer.parseInt(da);
                }
                String ho = m.group(3);
                if (ho!= null) {
                    retVal.mHours = Integer.parseInt(ho);
                }
                String mi = m.group(4);
                if (mi != null) {
                    retVal.mMins = Integer.parseInt(mi);
                }
                String se = m.group(5);
                if (se!= null) {
                    retVal.mSecs = Integer.parseInt(se);
                }
            } else {
                throw ServiceException.INVALID_REQUEST("Could not parse DURATION string: "+durationStr, null);
            }
        }
        
        return retVal;
    }

    public int getWeeks() {
        return mWeeks * (mNegative ? -1 : 1);
    }

    public int getDays() {
        return mDays * (mNegative ? -1 : 1);
    }

    public int getHours() {
        return mHours * (mNegative ? -1 : 1);
    }

    public int getMins() {
        return mMins * (mNegative ? -1 : 1);
    }

    public int getSecs() {
        return mSecs * (mNegative ? -1 : 1);
    }

    public boolean isMultipleOfDays() {
        return mHours == 0 && mMins == 0 && mSecs == 0 && (mDays != 0 || mWeeks != 0);
    }
}
