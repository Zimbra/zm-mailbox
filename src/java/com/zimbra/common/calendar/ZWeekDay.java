/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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