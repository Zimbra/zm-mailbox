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
                        "DayOfWeek field must be in [0, 6] range", null);
            if (mDay < 0 || mDay > 5)
                throw new IllegalArgumentException(
                        "Day field specifying week number must be in [1, 5] range",
                        null);
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
}
