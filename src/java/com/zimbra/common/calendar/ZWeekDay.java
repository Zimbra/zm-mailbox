/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.calendar;

import java.util.Calendar;

public enum ZWeekDay { 
    FR, MO, SA, SU, TH, TU, WE;
    
    public int getCalendarDay() {
        switch (this) {
        case SU:
            return Calendar.SUNDAY;
        case MO:
            return Calendar.MONDAY;
        case TU:
            return Calendar.TUESDAY;
        case WE:
            return Calendar.WEDNESDAY;
        case TH:
            return Calendar.THURSDAY;
        case FR:
            return Calendar.FRIDAY;
        }
        return Calendar.SATURDAY;
    }
}