/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2010 Zimbra, Inc.
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
import java.util.GregorianCalendar;

import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;

/**
 * Java representation of Windows SYSTEMTIME structure in Winbase.h
 * @author jhahm
 *
 */
public class WindowsSystemTime {

    private int mYear;
    private int mMonth;
    private int mDayOfWeek;
    private int mDay;
    private int mHour;
    private int mMinute;
    private int mSecond;
    private int mMilliseconds;

    public WindowsSystemTime(int year, int month, int dayOfWeek, int day,
                             int hour, int minute, int second,
                             int milliseconds) {
        mYear = year;
        mMonth = month;
        mDayOfWeek = dayOfWeek;
        mDay = day;
        mHour = hour;
        mMinute = minute;
        mSecond = second;
        mMilliseconds = milliseconds;

        if (mYear == 0) {
            // we have a recurrence rule as opposed to an absolute time
            if (mDayOfWeek < 0 || mDayOfWeek > 6)
                throw new IllegalArgumentException(
                        "DayOfWeek field must be in [0, 6] range");
            if (mDay < 0 || mDay > 5)
                throw new IllegalArgumentException(
                        "Day field specifying week number must be in [1, 5] range");
        }
    }

    public int getYear() { return mYear; }
    public int getMonth() { return mMonth; }
    public int getDayOfWeek() { return mDayOfWeek; }
    public int getDay() { return mDay; }
    public int getHour() { return mHour; }
    public int getMinute() { return mMinute; }
    public int getSecond() { return mSecond; }
    public int getMilliseconds() { return mMilliseconds; }

    public String toString() {
        StringBuffer sb = new StringBuffer(200);
        sb.append("SYSTEMTIME {\n");
        sb.append("    wYear         = ").append(mYear).append("\n");
        sb.append("    wMonth        = ").append(mMonth).append("\n");
        sb.append("    wDayOfWeek    = ").append(mDayOfWeek).append("\n");
        sb.append("    wDay          = ").append(mDay).append("\n");
        sb.append("    wHour         = ").append(mHour).append("\n");
        sb.append("    wMinute       = ").append(mMinute).append("\n");
        sb.append("    wSecond       = ").append(mSecond).append("\n");
        sb.append("    wMilliseconds = ").append(mMilliseconds).append("\n");
        sb.append("}");
        return sb.toString();
    }

    public SimpleOnset toSimpleOnset() {
        if (mYear != 0) return null;
        int week = mDay;
        if (week == 5)
            week = -1;
        return new SimpleOnset(week, mDayOfWeek + 1, mMonth, mDay,
                               mHour, mMinute, mSecond);
    }

    public static WindowsSystemTime fromSimpleOnset(SimpleOnset onset) {
        if (onset == null) return null;
        int week = onset.getWeek();
        if (week == -1)
            week = 5;
        if (week != 0) {
            return new WindowsSystemTime(
                    0, onset.getMonth(), onset.getDayOfWeek() - 1, week,
                    onset.getHour(), onset.getMinute(), onset.getSecond(), 0);
        } else {
            Calendar cal = new GregorianCalendar();
            int year = cal.get(Calendar.YEAR);
            cal.set(year, onset.getMonth() - 1, onset.getDayOfMonth(), onset.getHour(), onset.getMinute());
            return new WindowsSystemTime(
                    year, onset.getMonth(), cal.get(Calendar.DAY_OF_WEEK) - 1, onset.getDayOfMonth(),
                    onset.getHour(), onset.getMinute(), onset.getSecond(), 0);
        }
    }
}
