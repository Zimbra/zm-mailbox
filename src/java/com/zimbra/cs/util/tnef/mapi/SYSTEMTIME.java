/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.util.tnef.mapi;

import java.io.IOException;
import java.text.DateFormatSymbols;

import net.freeutils.tnef.RawInputStream;

/**
 * @author Gren Elliot
 *
 * Represents MAPI SYSTEMTIME structure 
 * This can represent a time but is also used in timezone definition structures
 */
public class SYSTEMTIME {
    
    private int Year;  // For timezone definitions, normally 0.  If not is
                       // absolute date that occurs once only.
    private int Month;       // 1=Jan, 12=Dec.
                             // 0 implies there is no DST (used with TimeZones)
    private DayOfWeek dayOfWeek;
    private int Day;         // For timezone - the occurrence number of a particular
                             // day of the week (e.g. Monday) within the month.
                             // 5 means last one in the month
    private int Hour;
    private int Minute;
    private int Seconds;
    private int Milliseconds;
    

    public SYSTEMTIME(RawInputStream ris) throws IOException {
        Year = ris.readU16();
        Month = ris.readU16();
        readDayOfWeek(ris);
        Day = ris.readU16();
        Hour = ris.readU16();
        Minute = ris.readU16();
        Seconds = ris.readU16();
        Milliseconds = ris.readU16();
    }


    /**
     *
     * For timezone definitions, normally 0.
     * If not is absolute date that occurs once only.
     *
     * @return the Year
     */
    public int getYear() {
        return Year;
    }

    /**
     * 1=Jan, 12=Dec. 0 implies there is no DST
     *
     * @return the 1 based Month or 0
     */
    public int getMonth() {
        return Month;
    }

    /**
     * @return DayOfWeek
     */
    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * For timezone - the occurrence number of a particular
     *                day of the week (e.g. Monday) within the month.
     *                5 means last one in the month
     * @return the day
     */
    public int getDay() {
        return Day;
    }

    /**
     * @return the hour
     */
    public int getHour() {
        return Hour;
    }

    /**
     * @return the minute
     */
    public int getMinute() {
        return Minute;
    }

    /**
     * @return the seconds
     */
    public int getSeconds() {
        return Seconds;
    }

    /**
     * @return the milliseconds
     */
    public int getMilliseconds() {
        return Milliseconds;
    }

    /**
     * @return dayOfWeek e.g. Calendar.MONDAY
     */
    public int getJavaCalendarDayOfWeek() {
        return dayOfWeek.javaDOW();
    }

    /**
     * @return Number of milliseconds from the start of the Current Day
     */
    public int getMillisecsFromStartOfCurrentDay() {
        return (((((Hour * 60) + Minute) * 60) + Seconds) * 1000) + Milliseconds;
    }

    public String toString() {
        String dayNames[] = new DateFormatSymbols().getWeekdays();
        StringBuffer timeInfo = new StringBuffer();
        if (Year != 0) {
            timeInfo.append("Year=");  // if non-zero, interpret as absolute date that occurs once only.
            timeInfo.append(Year);
            timeInfo.append(" ");
        }
        timeInfo.append("Month=");  // if zero, implies no DST
        timeInfo.append(Month);
        timeInfo.append(" ");
        timeInfo.append("Transition Time=");
        timeInfo.append(Hour).append(":").append(Minute);
        timeInfo.append(" ");
        if (Seconds != 0) {
            timeInfo.append("Non-zero Seconds=");
            timeInfo.append(Seconds).append(":").append(Milliseconds);
            timeInfo.append(" ");
        }
        timeInfo.append("(DayOfWeek=");
        timeInfo.append(dayOfWeek);
        timeInfo.append(" OccurrenceNumber=");
        timeInfo.append(Day);
        timeInfo.append(")");
        return timeInfo.toString();
    }

    private void readDayOfWeek(RawInputStream ris) throws IOException {
        long dow = ris.readU16();

        for (DayOfWeek curr : DayOfWeek.values()) {
            if (curr.mapiPropValue() == dow) {
                dayOfWeek = curr;
                return;
            }
        }
    }
}
