/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateTimeUtil {

    /**
     * Determines if the two dates are in the same day in the given time zone.
     * @param t1
     * @param t2
     * @param tz
     * @return
     */
    public static boolean sameDay(Date t1, Date t2, TimeZone tz) {
        Calendar cal1 = new GregorianCalendar(tz);
        cal1.setTime(t1);
        Calendar cal2 = new GregorianCalendar(tz);
        cal2.setTime(t2);
        return
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    public static boolean checkWithinTime(Timestamp timestamp, int timegap, TimeUnit timeunit) {
        if (TimeUnit.NANOSECONDS == timeunit) {
            if (TimeUnit.MILLISECONDS.toNanos(new Timestamp(System.currentTimeMillis()).getTime() - timestamp.getTime()) < timegap) {
                return true;
            }
        }
        if (TimeUnit.MICROSECONDS == timeunit) {
            if (TimeUnit.MILLISECONDS.toMicros(new Timestamp(System.currentTimeMillis()).getTime() - timestamp.getTime()) < timegap) {
                return true;
            }
        }
        if (TimeUnit.MILLISECONDS == timeunit) {
            if (TimeUnit.MILLISECONDS.toMillis(new Timestamp(System.currentTimeMillis()).getTime() - timestamp.getTime()) < timegap) {
                return true;
            }
        }
        if (TimeUnit.SECONDS == timeunit) {
            if (TimeUnit.MILLISECONDS.toSeconds(new Timestamp(System.currentTimeMillis()).getTime() - timestamp.getTime()) > timegap) {
                return true;
            }
        }
        if (TimeUnit.MINUTES == timeunit) {
            if (TimeUnit.MILLISECONDS
                    .toMinutes(new Timestamp(System.currentTimeMillis()).getTime() - timestamp.getTime()) < timegap) {
                return true;
            }
        }
        if (TimeUnit.HOURS == timeunit) {
            if (TimeUnit.MILLISECONDS.toHours(new Timestamp(System.currentTimeMillis()).getTime() - timestamp.getTime()) < timegap) {
                return true;
            }
        }
        if (TimeUnit.DAYS == timeunit) {
            if (TimeUnit.MILLISECONDS.toDays(new Timestamp(System.currentTimeMillis()).getTime() - timestamp.getTime()) < timegap) {
                return true;
            }
        }
        return false;
    }
}
