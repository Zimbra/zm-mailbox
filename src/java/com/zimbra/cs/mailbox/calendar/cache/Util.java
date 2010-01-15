/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;

public class Util {

    /**
     * Compare if two values are equal.  Nulls are considered equal.
     * @param <T>
     * @param val1
     * @param val2
     * @return
     */
    static <T> boolean sameValues(T val1, T val2) {
        if (val1 != null)
            return val1.equals(val2);
        else
            return val2 == null;
    }

    private static final int WEEEKS_IN_MONTH_VIEW = 6;

    /**
     * Get start/end times in millis for months range around now.
     * 
     * start time = midnight UTC of current month + N months
     * end time = midnight UTC of current month + (N + M) months
     * 
     * where N = monthFrom (can be negative) and M = numMonths.
     * 
     * @param now
     * @param monthFrom
     * @param numMonths
     * @return
     */
    static Pair<Long, Long> getMonthsRange(long now, int monthFrom, int numMonths) {
        Calendar cal = new GregorianCalendar(ICalTimeZone.getUTC());
        cal.setTimeInMillis(now);

        // midnight on 1st of this month in GMT
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Assume a week is always from Sunday to Saturday.

        Calendar calStart = (Calendar) cal.clone();
        calStart.add(Calendar.MONTH, monthFrom);
        // go back to Sunday of the week
        int moreDays = calStart.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        // go back one more day, for GMT+nnnn timezones (those ahead of GMT)
        ++moreDays;
        // go back 7 more days, because ZWC looks back up to a week to catch over due reminders
        moreDays += 7;
        calStart.add(Calendar.DAY_OF_YEAR, -1 * moreDays);
        long start = calStart.getTimeInMillis();

        Calendar calEnd = (Calendar) cal.clone();
        calEnd.add(Calendar.MONTH, monthFrom + numMonths);
        calEnd.add(Calendar.DAY_OF_YEAR, -1);  // beginning (midnight) of last day of the last month in range

        // Adjust for the extra weeks at the bottom of month view.  On a typical calendar showing
        // one week per line with Sunday of the first day of the week, a month can occupy 4 to 6
        // lines depending on the day of the week of the 1st of the month and the number of days
        // in the month.
        //
        // There is only one way for the 4 line case: February of non-leap year and the 1st is a
        // Sunday.  Most months take 5 lines.  The extreme case of 6-line month is a 31-day month
        // with the 1st falling on Saturday:
        //
        // pp pp pp pp pp pp 01
        // 02 03 04 05 06 07 08
        // 09 10 11 12 13 14 15
        // 16 17 18 19 20 21 22
        // 23 24 25 26 27 28 29
        // 30 31 nn nn nn nn nn
        //
        // The dates marked "pp" are from the previous month and "nn" dates are from the next month.
        // We can tell how many lines the current month is occupying by adding up the number of days
        // in the month and the number of "nn" dates, which is the extra days needed to get to the
        // end of that week.  In the minimum case the sum will be 28 and the maximum case will have
        // the sum greater than 35.
        int daysInFinalMonth = calEnd.getActualMaximum(Calendar.DAY_OF_MONTH);
        int extraDaysToEndOfWeek = Calendar.SATURDAY - calEnd.get(Calendar.DAY_OF_WEEK);
        int linesOccupied = (daysInFinalMonth + extraDaysToEndOfWeek + 6) / 7;
        int extraWeeks = Math.max(WEEEKS_IN_MONTH_VIEW - linesOccupied, 0);

        // go forward to Saturday of the week
        moreDays = extraDaysToEndOfWeek;
        // go forward two more weeks because minical shows 6 weeks all the time
        moreDays += extraWeeks * 7;
        // go forward one more day, for GMT-nnnn timezones (those behind GMT)
        ++moreDays;
        // go forward one more day to cover through the end of the day
        ++moreDays;
        calEnd.add(Calendar.DAY_OF_YEAR, moreDays);
        long end = Math.max(calEnd.getTimeInMillis(), start);

        return new Pair<Long, Long>(start, end);
    }

    /**
     * Returns the TZ offset in millis for the given instance, using the timezone in inv.
     * Returns null if inv is not an all-day appointment/task.
     * @param inv
     * @param instanceStart
     * @return
     */
    static Long getTZOffsetForInvite(Invite inv, long instanceStart) {
        if (inv.isAllDayEvent()) {
            ParsedDateTime dtStart = inv.getStartTime();
            if (dtStart != null) {
                ICalTimeZone tz = dtStart.getTimeZone();
                if (tz != null)
                    return new Long(tz.getOffset(instanceStart));
            }
        }
        return null;
    }
}
