/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.util;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

public final class DateUtil {
    private static final String[] MONTH_NAME = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
        "Oct", "Nov", "Dec"
    };

    public static String toImapDateTime(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);

        int tzoffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
        char tzsign = tzoffset > 0 ? '+' : '-';
        tzoffset = Math.abs(tzoffset);

        return String.format("%02d-%s-%d %02d:%02d:%02d %c%02d%02d",
            cal.get(Calendar.DAY_OF_MONTH), MONTH_NAME[cal.get(Calendar.MONTH)],
            cal.get(Calendar.YEAR), cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
            tzsign, tzoffset / 60, tzoffset % 60);
    }
}
