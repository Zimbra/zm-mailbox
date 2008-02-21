/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
        // go back one more day, for GMT+nnnn timezones
        moreDays++;
        calStart.add(Calendar.DAY_OF_YEAR, -1 * moreDays);
        long start = calStart.getTimeInMillis();

        Calendar calEnd = (Calendar) cal.clone();
        calEnd.add(Calendar.MONTH, monthFrom + numMonths);
        calEnd.add(Calendar.DAY_OF_YEAR, -1);  // last day of the last month in range
        // go forward to Saturday of the week
        moreDays = Calendar.SATURDAY - calEnd.get(Calendar.DAY_OF_WEEK);
        // go forward one more day, for GMT-nnnn timezones
        moreDays++;
        // go forward one more day to cover through the end of the day
        moreDays++;
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
