/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.util.tnef.mapi;

import java.util.Calendar;

import net.fortuna.ical4j.model.WeekDay;

/**
 * The <code>DayOfWeek</code> enum is used to represent days of the week.
 * It is typically associated with elements of MAPI properties which
 * use the Sunday=0... convention.
 * e.g. the MAPI SYSTEMTIME structure dayOfWeek 
 * 
 * @author Gren Elliot
 *
 */
public enum DayOfWeek {
    SU (0x00000000, Calendar.SUNDAY, WeekDay.SU),
    MO (0x00000001, Calendar.MONDAY, WeekDay.MO),
    TU (0x00000002, Calendar.TUESDAY, WeekDay.TU),
    WE (0x00000003, Calendar.WEDNESDAY, WeekDay.WE),
    TH (0x00000004, Calendar.THURSDAY, WeekDay.TH),
    FR (0x00000005, Calendar.FRIDAY, WeekDay.FR),
    SA (0x00000006, Calendar.SATURDAY, WeekDay.SA);
 
    private final long MapiPropValue;
    private final int JavaDOW;
    private final WeekDay iCal4JWeekDay;

    DayOfWeek(long propValue, int javaDOW, WeekDay iCal4JDOW) {
        MapiPropValue = propValue;
        JavaDOW = javaDOW;
        iCal4JWeekDay = iCal4JDOW;
    }

    public long mapiPropValue() {
        return MapiPropValue;
    }

    public int javaDOW() {
        return this.JavaDOW;
    }

    public WeekDay iCal4JWeekDay() {
        return iCal4JWeekDay;
    }
    
    public static final DayOfWeek getDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
        case 0:
            return SU;
        case 1:
            return MO;
        case 2:
            return TU;
        case 3:
            return WE;
        case 4:
            return TH;
        case 5:
            return FR;
        case 6:
            return SA;
            default:
                return null;
        }
    }

}
