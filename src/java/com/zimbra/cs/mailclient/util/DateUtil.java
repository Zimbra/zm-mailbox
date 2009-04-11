/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
