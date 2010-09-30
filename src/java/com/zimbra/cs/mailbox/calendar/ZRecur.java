/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.calendar;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Zimbra;

public class ZRecur {

    public static enum Frequency { DAILY, HOURLY, MINUTELY, MONTHLY, SECONDLY, WEEKLY, YEARLY }
    
    public static enum ZWeekDay { 
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
    
    public static class ZWeekDayNum {
        public static class DayOnlyComparator implements Comparator<ZWeekDayNum>
        {
            public int compare(ZWeekDayNum lhs, ZWeekDayNum rhs)
            {
                return lhs.mDay.getCalendarDay() - rhs.mDay.getCalendarDay();
            }
            
        }
        
        public ZWeekDay mDay;
        public int mOrdinal; // -4,-3,-2,-1,+1,+2,+3,+4
        
        public ZWeekDayNum() {}
        public ZWeekDayNum(int ord, ZWeekDay day) { mOrdinal = ord; mDay = day; };
        
        public String toString() {
            if (mOrdinal != 0) 
                return Integer.toString(mOrdinal)+mDay;
            else
                return mDay.toString();
        }
    };
    
    private static enum Tokens {
        BYDAY, BYHOUR, BYMINUTE, BYMONTH, BYMONTHDAY, BYSECOND, BYSETPOS, BYWEEKNO, 
        BYYEARDAY, COUNT, FREQ, INTERVAL, UNTIL, WKST;
    }

    // Max date known to calendaring = 01/01/9000 00:00:00 in UTC
    // This value is comfortably smaller than MySQL DATETIME column's max value of 9999-12-31 23:59:59
    private static final long MAX_DATE_MILLIS = 221845392000000L;
    // hard limits to recurrence expansion
    private static class ExpansionLimits {
        public int maxInstances;
        public int maxDays;
        public int maxWeeks;
        public int maxMonths;
        public int maxYears;
        public int maxYearsOtherFreqs;
    }
    private static ExpansionLimits sExpansionLimits;
    static {
        sExpansionLimits = new ExpansionLimits();
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            String val;
            val = server.getAttr(Provisioning.A_zimbraCalendarRecurrenceMaxInstances);
            sExpansionLimits.maxInstances = val == null ? 0 : Integer.parseInt(val);
            val = server.getAttr(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays);
            sExpansionLimits.maxDays = val == null ? 730 : Integer.parseInt(val);
            val = server.getAttr(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks);
            sExpansionLimits.maxWeeks = val == null ? 520 : Integer.parseInt(val);
            val = server.getAttr(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths);
            sExpansionLimits.maxMonths = val == null ? 360 : Integer.parseInt(val);
            val = server.getAttr(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears);
            sExpansionLimits.maxYears = val == null ? 100 : Integer.parseInt(val);
            val = server.getAttr(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears);
            sExpansionLimits.maxYearsOtherFreqs = val == null ? 1 : Integer.parseInt(val);
        } catch (NumberFormatException e) {
            Zimbra.halt("Can't initialize recurrence expansion limits", e);
        } catch (ServiceException e) {
            Zimbra.halt("Can't initialize recurrence expansion limits", e);
        }
    }

    private Date estimateEndTimeByUntilAndHardLimits(ParsedDateTime dtStart) throws ServiceException {
        boolean forever = false;
        Calendar hardEnd = dtStart.getCalendarCopy();
        Frequency freq = mFreq;
        if (freq == null)
            freq = Frequency.WEEKLY;
        switch (freq) {
        case WEEKLY:
            int weeks = sExpansionLimits.maxWeeks;
            if (weeks <= 0)
                forever = true;
            else
                hardEnd.add(Calendar.WEEK_OF_YEAR, weeks);
            break;
        case MONTHLY:
            int months = sExpansionLimits.maxMonths;
            if (months <= 0)
                forever = true;
            else
                hardEnd.add(Calendar.MONTH, months);
            break;
        case YEARLY:
            int years = sExpansionLimits.maxYears;
            if (years <= 0)
                forever = true;
            else
                hardEnd.add(Calendar.YEAR, years);
            break;
        case DAILY:
            int days = sExpansionLimits.maxDays;
            if (days <= 0)
                forever = true;
            else
                hardEnd.add(Calendar.DAY_OF_YEAR, days);
            break;
        default:
            int otherFreqYears = Math.max(sExpansionLimits.maxYearsOtherFreqs, 1);
            hardEnd.add(Calendar.YEAR, otherFreqYears);
        }
        Date d;
        if (forever)
            d = new Date(MAX_DATE_MILLIS);
        else
            d = hardEnd.getTime();
        if (mUntil != null) {
            Date until = mUntil.getDateForRecurUntil(dtStart.getTimeZone());
            if (until.before(d))
                d = until;
        }
        return d;
    }

    public Date getEstimatedEndTime(ParsedDateTime dtStart) throws ServiceException {
        if (dtStart == null)
            return null;
        return estimateEndTimeByUntilAndHardLimits(dtStart);
        // We can't estimate by COUNT because BYxxx rule parts yield too many possibilities.
    }

    public static String listAsStr(List<? extends Object> l) {
        StringBuffer toRet = new StringBuffer();
        boolean first = true;
        for (Object obj  : l) {
            if (!first)
                toRet.append(',');
            else
                first = false;
            toRet.append(obj.toString());
        }
            
        return toRet.toString();
    }
    
    public static void main(String[] args) {
        ICalTimeZone tzUTC = ICalTimeZone.getUTC();
        TimeZoneMap tzmap = new TimeZoneMap(tzUTC);
        ParsedDateTime dtStart = null;
        try {
            dtStart = ParsedDateTime.parse("20050101T123456", tzmap, tzUTC, tzUTC);
        } catch(ParseException e) {
            System.out.println("Caught ParseException at start: "+e);
        }

        Date rangeStart;
        Date rangeEnd;
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setTimeZone(tzUTC);
        
        cal.set(2005, 4, 15, 0, 0, 0);
        rangeStart = cal.getTime();

        cal.set(2006, 0, 1, 0, 0, 0);
        rangeEnd = cal.getTime();
        

        try {
            ZRecur test = new ZRecur("FREQ=DAILY;BYMONTH=5,6", tzmap);
            System.out.println("\n\n"+test.toString()+"\n-------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                cal.setTimeZone(tzUTC);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        try {
            ZRecur test = new ZRecur("FREQ=DAILY;BYMONTH=5,6;BYDAY=TH,-1MO", tzmap);
            System.out.println("\n\n"+test.toString()+"\n-------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                cal.setTimeZone(tzUTC);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        try {
            ZRecur test = new ZRecur("FREQ=DAILY;BYMONTH=5,6;BYMONTHDAY=1,3,5,7,9,31", tzmap);
            System.out.println("\n\n"+test.toString()+"\n-------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        try {
            ZRecur test = new ZRecur("FREQ=DAILY;BYMONTH=5,6;BYMONTHDAY=1,3,5,7,9,31;BYDAY=SU,SA", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }

        
        try {
            ZRecur test = new ZRecur("FREQ=DAILY;BYMONTH=5,6;BYMONTHDAY=1,3,5,7,9,31;BYDAY=SU,SA;BYHOUR=21,0", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }

        try {
            ZRecur test = new ZRecur("FREQ=DAILY;BYMONTH=5,6;BYMONTHDAY=1,3,5,7,9,31;BYDAY=SU;BYHOUR=21,0;BYMINUTE=23", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }

        try {
            ZRecur test = new ZRecur("FREQ=DAILY;BYMONTH=5,6;BYMONTHDAY=1,3,5,7,9,31;BYDAY=SU;BYHOUR=1,21,0;BYSECOND=0,59", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }

        try {
            // parse error testing
            ZRecur test = new ZRecur("FREQ=DAILY;BIYMONTH=5,6;BYMONTHDAY=1,3,5,7,9,31;BYDAY=SU;BYHOUR=1,21,0;BYSECOND=0,59;BYSETPOS=1,-1,3,1000,,-1000", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        try {
            ZRecur test = new ZRecur("FREQ=HOURLY;BIYMONTH=6;BYMONTHDAY=1,3;BYHOUR=2,14", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        try {
            ZRecur test = new ZRecur("FREQ=HOURLY;BIYMONTH=6;BYMONTHDAY=1;;BYMINUTE=10;BYSECOND=11,12", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        

        cal.set(2010, 0, 1, 0, 0, 0);
        rangeEnd = cal.getTime();
        
        try {
            ZRecur test = new ZRecur("FREQ=YEARLY", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }


        try {
            ZRecur test = new ZRecur("FREQ=YEARLY;BYYEARDAY=-1", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
                
        try {
            ZRecur test = new ZRecur("FREQ=SECONDLY", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        try {
            ParsedDateTime myDtStart = ParsedDateTime.parse("16010101T020000", tzmap, tzUTC, tzUTC);
            ZRecur test = new ZRecur("FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=12;BYDAY=-1SU", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(myDtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ParseException e) {
            System.out.println("Caught ParseException"+e);
            e.printStackTrace();
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        
        cal.set(2010, 0, 1, 0, 0, 0);
        rangeEnd = cal.getTime();
        
        try {
            ZRecur test = new ZRecur("FREQ=YEARLY;BYMONTH=12;BYDAY=1WE", tzmap);
            System.out.println("\n\n"+test.toString()+"\n--------------------------------------------------------------");
            List<Date> dateList = test.expandRecurrenceOverRange(dtStart, rangeStart.getTime(), rangeEnd.getTime());
            for (Date d : dateList) {
                cal.setTime(d);
                System.out.printf("%tc\n", cal);
            }
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException"+e);
            e.printStackTrace();
        }
        

    }
    
    private List<ZWeekDayNum> mByDayList = new ArrayList<ZWeekDayNum>();
    private List<Integer> mByHourList = new ArrayList<Integer>();
    private List<Integer> mByMinuteList = new ArrayList<Integer>();
    private List<Integer> mByMonthDayList = new ArrayList<Integer>();
    private List<Integer> mByMonthList = new ArrayList<Integer>();
    private List<Integer> mBySecondList = new ArrayList<Integer>();
    
    private List<Integer> mBySetPosList = new ArrayList<Integer>();
    private List<Integer> mByWeekNoList = new ArrayList<Integer>();
    private List<Integer> mByYearDayList = new ArrayList<Integer>();
    
    private int mCount = 0;
    private Frequency mFreq = Frequency.WEEKLY;
    private int mInterval = 0;
    private ParsedDateTime mUntil = null;
    private ZWeekDay mWkSt = null;
    

    public ZRecur(String str, TimeZoneMap tzmap) throws ServiceException {
        parse(str, tzmap);
    }
    
    public List<Integer> getByHourList() {
        return this.mByHourList;
    }

    public void setByHourList(List<Integer> byHourList) {
        this.mByHourList = byHourList;
    }

    public List<Integer> getByMinuteList() {
        return this.mByMinuteList;
    }

    public void setByMinuteList(List<Integer> byMinuteList) {
        this.mByMinuteList = byMinuteList;
    }

    public List<Integer> getByMonthDayList() {
        return this.mByMonthDayList;
    }

    public void setByMonthDayList(List<Integer> byMonthDayList) {
        this.mByMonthDayList = byMonthDayList;
    }

    public List<Integer> getByMonthList() {
        return this.mByMonthList;
    }

    public void setByMonthList(List<Integer> byMonthList) {
        this.mByMonthList = byMonthList;
    }

    public List<Integer> getBySecondList() {
        return this.mBySecondList;
    }

    public void setBySecondList(List<Integer> bySecondList) {
        this.mBySecondList = bySecondList;
    }

    public List<Integer> getBySetPosList() {
        return this.mBySetPosList;
    }

    public void setBySetPosList(List<Integer> bySetPosList) {
        this.mBySetPosList = bySetPosList;
    }

    public List<Integer> getByWeekNoList() {
        return this.mByWeekNoList;
    }

    public void setByWeekNoList(List<Integer> byWeekNoList) {
        this.mByWeekNoList = byWeekNoList;
    }

    public List<Integer> getByYearDayList() {
        return this.mByYearDayList;
    }

    public void setByYearDayList(List<Integer> byYearDayList) {
        this.mByYearDayList = byYearDayList;
    }

    public int getCount() {
        return this.mCount;
    }

    public void setCount(int count) {
        this.mCount = count;
    }

    public Frequency getFrequency() {
        return this.mFreq;
    }

    public void setFrequency(Frequency freq) {
        this.mFreq = freq;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public void setInterval(int interval) {
        this.mInterval = interval;
    }

    public ParsedDateTime getUntil() {
        return this.mUntil;
    }

    public void setUntil(ParsedDateTime until) {
        this.mUntil = until;
    }

    public ZWeekDay getWkSt() {
        return this.mWkSt;
    }

    public void setWkSt(ZWeekDay wkSt) {
        this.mWkSt = wkSt;
    }

    public void setByDayList(List<ZWeekDayNum> byDayList) {
        this.mByDayList = byDayList;
    }

    public List<ZWeekDayNum> getByDayList() {
        return this.mByDayList;
    }

    public List<Date> expandRecurrenceOverRange(
        ParsedDateTime dtStart,
        long rangeStart,
        long rangeEnd)
    throws ServiceException {
        List<Date> toRet = new LinkedList<Date>();
        
        Date rangeStartDate = new Date(rangeStart);
        // subtract 1000ms (1sec) because the code in the method treats
        // end time as inclusive while the rangeEnd input argument is
        // exclusive value
        Date rangeEndDate = new Date(rangeEnd - 1000);
        Date dtStartDate = new Date(dtStart.getUtcTime());

        Date earliestDate;
        if (dtStartDate.after(rangeStartDate))
            earliestDate = dtStartDate;
        else
            earliestDate = rangeStartDate;
        
        if (mUntil != null) {
            Date until = mUntil.getDateForRecurUntil(dtStart.getTimeZone());
            if (until.before(rangeEndDate))
                rangeEndDate = until;
        }

        // Set limit of expansion count.
        int maxInstancesFromConfig = sExpansionLimits.maxInstances;
        int maxInstancesExpanded;
        if (maxInstancesFromConfig <= 0)
            maxInstancesExpanded = mCount;
        else if (mCount <= 0)
            maxInstancesExpanded = maxInstancesFromConfig;
        else
            maxInstancesExpanded = Math.min(mCount, maxInstancesFromConfig);
        int numInstancesExpanded = 1;  // initially 1 rather than 0 because DTSTART is always included

        // Set hard limit of expansion time range.  (bug 21989)
        Date hardEndDate = getEstimatedEndTime(dtStart);
        if (hardEndDate.before(rangeEndDate))
            rangeEndDate = hardEndDate;
        
        if (rangeEndDate.before(earliestDate))
            return toRet;

        GregorianCalendar cur = dtStart.getCalendarCopy();
        int baseMonthDay = cur.get(Calendar.DAY_OF_MONTH);
        boolean baseIsLeapDay = ((baseMonthDay == 29) && (cur.get(Calendar.MONTH) == Calendar.FEBRUARY));
        
        // until we hit rangeEnd, or we've SAVED count entries:
        //
        //     gather each set {
        //        
        //        
        //        
        //        curDate forward one INTERVAL
        //
        //     } 
        //     check Set against BYSETPOS & ranges & count
        //
        
        int interval = mInterval;
        if (interval <= 0) 
            interval = 1;

        // DTSTART is always part of the expansion, as long as it falls within
        // the range.
        if (!dtStartDate.before(earliestDate) && !dtStartDate.after(rangeEndDate))
            toRet.add(dtStartDate);

        int numConsecutiveIterationsWithoutMatchingInstance = 0;
        boolean pastHardEndTime = false;
        long numIterations = 0;  // track how many times we looped
        while (!pastHardEndTime && (maxInstancesExpanded <= 0 || numInstancesExpanded < maxInstancesExpanded)) {
            numIterations++;
            boolean curIsAtOrAfterEarliestDate = !cur.getTime().before(earliestDate);
            boolean curIsAfterEndDate = cur.getTime().after(rangeEndDate);
            List<Calendar> addList = new LinkedList<Calendar>();
            
            switch (mFreq) {
            case HOURLY:
                /*
                 * BYSECOND - for each listed second
                 * BYMINUTE - for each listed minute in hour
                 * BYHOUR - match iff in hour list
                 * BYDAY - for each day listed
                 * BYMONTHDAY - only those monthdays
                 * BYYEARDAY - only those yeardays
                 * BYMONTH - only those months
                 */
                if (!checkMonthList(cur))
                    continue;
                
                if (!checkYearDayList(cur))
                    continue;
                
                if (!checkMonthDayList(cur))
                    continue;
                
                if (!checkDayList(cur))
                    continue;
                
                if (!checkHourList(cur))
                    continue;
                
                addList.add((Calendar)(cur.clone()));
                
                cur.add(Calendar.HOUR_OF_DAY, interval);
                
                addList = expandHourList(addList);
                addList = expandMinuteList(addList);
                addList = expandSecondList(addList);                
                
                break;
            case DAILY:
                /*
                 * BYSECOND - for each listed second in day
                 * BYMINUTE - for each listed minute in day 
                 * BYHOUR - for each listed hour in day
                 * BYDAY - no ordinal allowed, match iff in day list
                 * BYMONTHDAY - only that day
                 * BYYEARDAY - only that day
                 * BYWEEKNO -- YEARLY ONLY
                 * BYMONTH - only that month
                 * 
                 * while (count check & until check & rangeEnd check) { 
                 *    if (byMonth && !month matches)
                 *      curDay = set MONTH to matching month
                 *      
                 *    if (byYearDay && !yearday matches)
                 *      curDay = set DAY to next matching yearday
                 *      
                 *    if (byMonthday && !monthday matches)
                 *      curDay = skip to next matching monthday
                 *      
                 *    if (byDay && !day in list)
                 *      curDay = skip to next mathcing byDay
                 *      
                 *    if (!byHour or FOR EACH HOUR IN HOURLIST)
                 *      if (!byMinute or FOR EACH MINUTE IN MINLIST)
                 *        if (!bySecond or FOR EACH SECOND IN LIST)
                 *          ----add to list---
                 *     
                 *     check against BYSETPOS
                 *                    
                 *     curDay+=1 day
                 * } 
                 * 
                 */
                
                if (!checkMonthList(cur))
                    continue;
                
                if (!checkYearDayList(cur))
                    continue;
                
                if (!checkMonthDayList(cur))
                    continue;
                
                if (!checkDayList(cur))
                    continue;
                
                addList.add((Calendar)(cur.clone()));
                
                cur.add(Calendar.DAY_OF_YEAR, interval);
                
                addList = expandHourList(addList);
                addList = expandMinuteList(addList);
                addList = expandSecondList(addList);
                break;
            case WEEKLY:
                /*
                 * BYSECOND - for every listed second
                 * BYMINUTE - for every listed minute
                 * BYHOUR - for every listed hour
                 * BYDAY - for every listed day
                 * BYMONTHDAY - MAYBE once a month
                 * BYYEARDAY - MAYBE once a year
                 * BYMONTH - iff month matches
                 * 
                 *  for each (INTERVAL)WEEK{ 
                 *    if (byMonth && !month matches)
                 *      curDay = set MONTH to DtStart in next matching month
                 *      
                 *    if (byYearDay && !yearday matches)
                 *      curDay = set date to next matching yearday
                 *      
                 *    if (byMonthDay && !monthday matches)
                 *      curDay = skip to next matching monthday
                 *
                 *    if (!byDay or FOREACH day in list)
                 *      if (!byHour or FOREACH hour in list)
                 *        if (!byMinute or FOREACH minute in list)
                 *          if (!bySecond or FOREACH second in list)
                 *            ----add to list----
                 *         
                 *    check against BYSETPOS
                 *    
                 *    curDay += 1 week
                 * } while (count check & until check & rangeEnd check)
                 * 
                 */
                if (!checkMonthList(cur))
                    continue;
                
                if (!checkYearDayList(cur))
                    continue;

                if (!checkMonthDayList(cur))
                    continue;

                addList.add((Calendar)(cur.clone()));
                
                cur.add(Calendar.WEEK_OF_YEAR, interval);
                
                addList = expandDayListForWeekly(addList);
                addList = expandHourList(addList);
                addList = expandMinuteList(addList);
                addList = expandSecondList(addList);
                break;
            case MONTHLY:
                if (!checkMonthList(cur))
                    continue;
                
                if (!checkYearDayList(cur))
                    continue;
                
                addList.add((Calendar)(cur.clone()));

                cur.set(Calendar.DAY_OF_MONTH, 1);
                cur.add(Calendar.MONTH, interval);
                int daysInMonth = cur.getActualMaximum(Calendar.DAY_OF_MONTH);
                cur.set(Calendar.DAY_OF_MONTH, Math.min(baseMonthDay, daysInMonth));
                
                addList = expandMonthDayList(addList);
                addList = expandDayListForMonthlyYearly(addList);
                addList = expandHourList(addList);
                addList = expandMinuteList(addList);
                addList = expandSecondList(addList);
                
                break;
            case YEARLY:
                /*
                 * BYSECOND
                 * BYMINUTE
                 * BYHOUR 
                 * BYDAY
                 * BYMONTHDAY 
                 * BYYEARDAY 
                 * BYWEEKNO - specified week
                 * BYMONTH - once
                 */
                if (baseIsLeapDay) {
                    // previously adding a year to a leap day will have rounded down to the 28th.
                    // If this happened, we need to be sure that if we are back in a leap
                    // year, it is back at 29th
                    cur.set(Calendar.DAY_OF_MONTH, cur.getActualMaximum(Calendar.DAY_OF_MONTH));
                }
                if (ignoreYearForRecurrenceExpansion(cur, baseIsLeapDay)) {
                    cur.add(Calendar.YEAR, interval);
                    break;
                }
                addList.add((Calendar)(cur.clone()));
                
                cur.add(Calendar.YEAR, interval);
                
                addList = expandMonthList(addList);
                addList = expandYearDayList(addList);
                
                addList = expandMonthDayList(addList);
                addList = expandDayListForMonthlyYearly(addList);
                addList = expandHourList(addList);
                addList = expandMinuteList(addList);
                addList = expandSecondList(addList);
                
                break;
            default:
                // MINUTELY and SECONDLY are intentionally not supported for performance reasons.
                return toRet;
            }
            
            addList = handleSetPos(addList);

            boolean noInstanceFound = true;
            boolean foundInstancePastEndDate = false;
            // add all the ones that match!
            for (Calendar addCal : addList) {
                Date toAdd = addCal.getTime();

                // We already counted DTSTART before the main loop, so don't
                // count it twice.
                if (toAdd.compareTo(dtStartDate) == 0) {
                    noInstanceFound = false;
                    continue;
                }

                // we still have expanded this instance, even if it isn't in our
                // current date window
                if (toAdd.after(dtStartDate))
                    numInstancesExpanded++;

                if (!toAdd.after(rangeEndDate)) {
                    if (!toAdd.before(earliestDate)) {
                        toRet.add(toAdd);
                        noInstanceFound = false;
                    }
                } else {
                    foundInstancePastEndDate = true;
                    break;
                }

                if (maxInstancesExpanded > 0 && numInstancesExpanded >= maxInstancesExpanded)
                    break;
            }

            // Detect invalid rule.  If the rule was invalid the current iteration, which is for the current
            // frequency interval, would have found no matching instance.  The next iteration will also find
            // no matching instance, and there is no need to keep iterating until we go past the hard end
            // time or COUNT/UNTIL limit.
            //
            // However, we have to make an exception for leap year.  An yearly rule looking for February 29th
            // will find no instance in up to 3 consecutive years before finding Feb 29th in the fourth year.
            //
            // So the invalid rule detection must look for at least 4 consecutive failed iterations.
            if (curIsAtOrAfterEarliestDate) {
                if (noInstanceFound)
                    numConsecutiveIterationsWithoutMatchingInstance++;
                else
                    numConsecutiveIterationsWithoutMatchingInstance = 0;
                if (numConsecutiveIterationsWithoutMatchingInstance >= 4) {
                    ZimbraLog.calendar.warn("Invalid recurrence rule: " + toString());
                    return toRet;
                }
            }

            pastHardEndTime = foundInstancePastEndDate || (noInstanceFound && curIsAfterEndDate);
        }

        return toRet;
    }

    public String toString() {
        StringBuffer toRet = new StringBuffer("FREQ=").append(mFreq);

        if (mUntil != null) {
            toRet.append(';').append("UNTIL=");
            toRet.append(mUntil.getDateTimePartString(false));
        }
        if (mCount > 0) 
            toRet.append(';').append("COUNT=").append(mCount);
        if (mInterval > 0) 
            toRet.append(';').append("INTERVAL=").append(mInterval);
        if (mBySecondList.size() > 0)
            toRet.append(';').append("BYSECOND=").append(listAsStr(mBySecondList));
        if (mByMinuteList.size() > 0)
            toRet.append(';').append("BYMINUTE=").append(listAsStr(mByMinuteList));
        if (mByHourList.size() > 0)
            toRet.append(';').append("BYHOUR=").append(listAsStr(mByHourList));
        if (mByDayList.size() > 0)
            toRet.append(';').append("BYDAY=").append(listAsStr(mByDayList));
        if (mByMonthDayList.size() > 0)
            toRet.append(';').append("BYMONTHDAY=").append(listAsStr(mByMonthDayList));
        if (mByYearDayList.size() > 0)
            toRet.append(';').append("BYYEARDAY=").append(listAsStr(mByYearDayList));
        if (mByWeekNoList.size() > 0)
            toRet.append(';').append("BYWEEKNO=").append(listAsStr(mByWeekNoList));
        if (mByMonthList.size() > 0)
            toRet.append(';').append("BYMONTH=").append(listAsStr(mByMonthList));
        if (mBySetPosList.size() > 0)
            toRet.append(';').append("BYSETPOS=").append(listAsStr(mBySetPosList));

        return toRet.toString();
    }
    /**
     * This version is for HOURLY/DAILY frequencies: it does NOT check the ordinal at all,
     * it only verifies that the day-of-the-week matches 
     * 
     * @param cal
     * @return
     */
    private boolean checkDayList(GregorianCalendar cal)
    {
        assert(mFreq!=Frequency.MONTHLY && mFreq!=Frequency.YEARLY && mFreq!=Frequency.WEEKLY);
        
        if (mByDayList.size() > 0) {
            for (ZWeekDayNum listCur: mByDayList) {
                int curDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (listCur.mDay.getCalendarDay() == curDayOfWeek)
                    return true;
                
                // since the DayOfWeek list is in week-order, if we hit a HIGHER one,
                // then we know out current one isn't in the list, and therefore
                // we should go to this one we just found in the list
                if (listCur.mDay.getCalendarDay() > curDayOfWeek) {
                    cal.set(Calendar.DAY_OF_WEEK, listCur.mDay.getCalendarDay());
                    return false;
                }
            }
            
            // we've not found a match AND we've not found a 
            // higher value in our list -- so wrap
            cal.set(Calendar.DAY_OF_WEEK, mByDayList.get(0).mDay.getCalendarDay());
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            return false;
        }
        return true;
    }
    
    /**
     * @param cal
     * @return
     */
    private boolean checkHourList(GregorianCalendar cal)
    {
        if (mByHourList.size() > 0) {
            for (Integer cur: mByHourList) {
                int curHour = cal.get(Calendar.HOUR_OF_DAY);
                if (curHour == cur.intValue())
                    return true;
                
                // since the month list is in order, if we hit a HIGHER month,
                // then we know out current month isn't in the list, and therefore
                // we should go to this next one
                if (cur > curHour) {
                    cal.set(Calendar.HOUR_OF_DAY, cur);
                    return false; // must re-start checks
                }
            }
            
            // we've not found a match AND we've not found a 
            // higher value in our list -- so wrap
            cal.set(Calendar.HOUR, mByHourList.get(0));
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return false; // must re-start checks
        }
        return true;
    }
    private boolean checkMonthDayList(GregorianCalendar cal)
    {
        if (mByMonthDayList.size() > 0) {
            for (Integer cur: mByMonthDayList) {
                int curMonthDay = cal.get(Calendar.DAY_OF_MONTH);
                if (cur == curMonthDay)
                    return true;
                
                // since the list is in order, if we hit a HIGHER one,
                // then we know out current one isn't in the list, and therefore
                // we should go to this one we just found in the list
                if (cur > curMonthDay) {
                    cal.set(Calendar.DAY_OF_MONTH, cur);
                    return false;
                }
            }
            
            // we've not found a match AND we've not found a 
            // higher value in our list -- so wrap
            cal.set(Calendar.DAY_OF_MONTH, mByMonthDayList.get(0));
            cal.add(Calendar.MONTH, 1);
            return false;
        }
        return true;
    }
    private boolean checkMonthList(GregorianCalendar cal)
    {
        if (mByMonthList.size() > 0) {
            for (Integer cur: mByMonthList) {
                int curMonth = cal.get(Calendar.MONTH)+1;
                if (cur == curMonth)
                    return true;
                
                // since the month list is in order, if we hit a HIGHER month,
                // then we know out current month isn't in the list, and therefore
                // we should go to this next one
                if (cur > curMonth) {
                    cal.set(Calendar.MONTH, cur-1);
                    return false; // must re-start checks
                }
            }
            
            // we've not found a match AND we've not found a 
            // higher value in our list -- so wrap
            cal.set(Calendar.MONTH, mByMonthList.get(0)-1);
            cal.add(Calendar.YEAR, 1);
            return false; // must re-start checks
        }
        return true;
    }
    private boolean checkYearDayList(GregorianCalendar cal)
    {
        if (mByYearDayList.size() > 0) {
            for (Integer cur: mByYearDayList) {
                int curYearDay = cal.get(Calendar.DAY_OF_YEAR);
                if (cur == curYearDay)
                    return true;
                
                // since the YearDay list is in order, if we hit a HIGHER one,
                // then we know out current one isn't in the list, and therefore
                // we should go to this one we just found in the list
                if (cur > curYearDay) {
                    cal.set(Calendar.DAY_OF_YEAR, cur);
                    return false;
                }
            }
            
            // we've not found a match AND we've not found a 
            // higher value in our list -- so wrap
            cal.set(Calendar.DAY_OF_YEAR, mByYearDayList.get(0));
            cal.add(Calendar.YEAR, 1);
            return false;
        }
        return true;
    }
    private List<Calendar> expandDayListForMonthlyYearly(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq==Frequency.MONTHLY || mFreq==Frequency.YEARLY);
        
        if (mByDayList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new ArrayList<Calendar>();
        Set<Integer> months = new HashSet<Integer>();
        
        
        for (Calendar cur : list) {
            int curYear = cur.get(Calendar.YEAR); 
            int curMonth = cur.get(Calendar.MONTH);
            if (!months.contains(curMonth)) {
                months.add(curMonth);
                
                for (ZWeekDayNum day : mByDayList) {
                    
                    // find all the cals matching this day-of-week
                    ArrayList<Integer> matching = new ArrayList<Integer>();
                    
                    cur.set(Calendar.DAY_OF_MONTH, 1);
                    do {
                        if (cur.get(Calendar.DAY_OF_WEEK) == day.mDay.getCalendarDay()) 
                            matching.add(cur.get(Calendar.DAY_OF_MONTH));
                        cur.add(Calendar.DAY_OF_MONTH, 1);
                    } while(cur.get(Calendar.MONTH) == curMonth);
                    
                    cur.set(Calendar.MONTH, curMonth);
                    cur.set(Calendar.YEAR, curYear);
                    
                    if (day.mOrdinal == 0) {
                        for (Integer matchDay: matching) {
                            cur.set(Calendar.DAY_OF_MONTH, matchDay);
                            toRet.add((Calendar)(cur.clone()));
                        }
                    } else {
                        if (day.mOrdinal > 0) {
                            if (day.mOrdinal <= matching.size()) {
                                cur.set(Calendar.DAY_OF_MONTH, matching.get(day.mOrdinal-1));
                                toRet.add((Calendar)(cur.clone()));
                            }
                        } else {
                            if ((-1 * day.mOrdinal)<=(matching.size())) {
                                cur.set(Calendar.DAY_OF_MONTH, matching.get(matching.size()+day.mOrdinal));
                                toRet.add((Calendar)(cur.clone()));
                            }
                        }
                    }
                } // foreach mByDayList
            } // month already seen?
        }

        
        // we unfortunately have to sort here because, for example, the "-1FR" could happen before the "-1TH"
        assert (toRet instanceof ArrayList);
        Collections.sort(toRet);
        
        return toRet;
    }

    /**
     * rfc5545 says :
     *    Recurrence rules may generate recurrence instances with an invalid
     *    date (e.g., February 30) or nonexistent local time (e.g., 1:30 AM
     *    on a day where the local time is moved forward by an hour at 1:00
     *    AM).  Such recurrence instances MUST be ignored and MUST NOT be
     *    counted as part of the recurrence set.
     * Therefore, ignore this year if the last day isn't 29th for patterns:
     *     RRULE:FREQ=YEARLY;BYMONTHDAY=29;BYMONTH=2
     *     and "RRULE:FREQ=YEARLY" where DTSTART is a leap day
     * 
     * @param cur
     * @param baseIsLeapDay
     * @return
     */
    private boolean ignoreYearForRecurrenceExpansion(Calendar cur, boolean baseIsLeapDay) {
        boolean ignoreThisYear = false;
        if (baseIsLeapDay && (cur.get(Calendar.DAY_OF_MONTH) != 29)) {
            ignoreThisYear = isSimpleRecurrence();
            if (!ignoreThisYear) {
                if ((mByMonthDayList.size() == 1) && (mByMonthDayList.get(0) == 29)) {
                    ignoreThisYear = (mByMonthList.isEmpty() ||
                                    ((mByMonthList.size() == 1) && (mByMonthList.get(0) == 2)));
                }
            }
        }
        return ignoreThisYear;
    }

    private boolean isSimpleRecurrence() {
        if (!mByDayList.isEmpty()) return false;
        if (!mByHourList.isEmpty()) return false;
        if (!mByMinuteList.isEmpty()) return false;
        if (!mByMonthDayList.isEmpty()) return false;
        if (!mByMonthList.isEmpty()) return false;
        if (!mBySecondList.isEmpty()) return false;
        if (!mBySetPosList.isEmpty()) return false;
        if (!mByWeekNoList.isEmpty()) return false;
        if (!mByYearDayList.isEmpty()) return false;
        return true;
    }

    /**
     * Very simple function b/c it can completely ignore the Ordinal value
     * 
     * @param list
     * @return
     */
    private List<Calendar> expandDayListForWeekly(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq==Frequency.WEEKLY);
        
        if (mByDayList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new LinkedList<Calendar>();
        
        for (Calendar cur : list) { 
            for (ZWeekDayNum day : mByDayList) {
                cur.set(Calendar.DAY_OF_WEEK, day.mDay.getCalendarDay());
                toRet.add((Calendar)(cur.clone()));
            }
        }
        
        return toRet;
    }
    private List<Calendar> expandHourList(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq==Frequency.DAILY || mFreq==Frequency.WEEKLY || mFreq==Frequency.MONTHLY || mFreq==Frequency.YEARLY);
        
        if (mByHourList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new LinkedList<Calendar>();
        
        for (Calendar cur : list) { 
            for (Integer hour : mByHourList) {
                cur.set(Calendar.HOUR_OF_DAY, hour);
                toRet.add((Calendar)(cur.clone()));
            }
        }
        
        return toRet;
    }
    private List<Calendar> expandMinuteList(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq != Frequency.MINUTELY && mFreq != Frequency.SECONDLY);
        
        if (mByMinuteList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new LinkedList<Calendar>();
        
        for (Calendar cur : list) { 
            for (Integer minute: mByMinuteList) {
                cur.set(Calendar.MINUTE, minute);
                toRet.add((Calendar)(cur.clone()));
            }
        }
        
        return toRet;
    }
    private List<Calendar> expandMonthDayList(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq==Frequency.MONTHLY || mFreq==Frequency.YEARLY);
        
        if (mByMonthDayList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new LinkedList<Calendar>();
        
        for (Calendar cur : list) { 
            int curMonth = cur.get(Calendar.MONTH);
            int lastMonthDay = cur.getActualMaximum(Calendar.DAY_OF_MONTH);
            boolean seenLastMonthDay = false;
            for (Integer moday: mByMonthDayList) {
                if (moday != 0) {
                    if (moday > 0) {
                        if (moday >= lastMonthDay) {
                            if (seenLastMonthDay)
                                continue;
                            seenLastMonthDay = true;
                            moday = lastMonthDay;
                        }
                        cur.set(Calendar.DAY_OF_MONTH, moday);
                    } else {
                        if (moday == -1) {
                            if (seenLastMonthDay)
                                continue;
                            seenLastMonthDay = true;
                        }
                        cur.set(Calendar.DAY_OF_MONTH, 1);
                        cur.roll(Calendar.DAY_OF_MONTH, moday);
                    }
                    assert(cur.get(Calendar.MONTH) == curMonth);
                    toRet.add((Calendar)(cur.clone()));
                }
            }
        }
        
        return toRet;
    }
    
    
    private List<Calendar> expandMonthList(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq==Frequency.YEARLY);
        
        if (mByMonthList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new LinkedList<Calendar>();
        
        for (Calendar cur : list) { 
            for (Integer month: mByMonthList) {
                cur.set(Calendar.MONTH, month-1);
                    
                toRet.add((Calendar)(cur.clone()));
            }
        }
        
        return toRet;
    }
    
    
    private List<Calendar> expandSecondList(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq != Frequency.SECONDLY);
        
        if (mBySecondList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new LinkedList<Calendar>();
        
        for (Calendar cur : list) { 
            for (Integer second: mBySecondList) {
                cur.set(Calendar.SECOND, second);
                toRet.add((Calendar)(cur.clone()));
            }
        }
        
        return toRet;
    }
    
    private List<Calendar> expandYearDayList(List<Calendar> list)
    {
        // this func ONLY works for expanding, NOT for contracting
        assert(mFreq==Frequency.YEARLY);
        
        if (mByYearDayList.size() <= 0)
            return list;
        
        List<Calendar> toRet = new LinkedList<Calendar>();
        Set<Integer> years = new HashSet<Integer>();
        
        
        for (Calendar cur : list) {
            int curYear = cur.get(Calendar.YEAR); 
            if (!years.contains(curYear)) {
                years.add(curYear);
                
                for (Integer yearDay : mByYearDayList) {
                    
                    if (yearDay > 0) 
                        cur.set(Calendar.DAY_OF_YEAR, yearDay);
                    else {
                        cur.set(Calendar.DAY_OF_YEAR, 1);
                        cur.roll(Calendar.DAY_OF_YEAR, yearDay);
                    }
                    
                    toRet.add((Calendar)(cur.clone()));
                }
            } // year already seen?
        }
        
        return toRet;
    }
    
    private List<Calendar> handleSetPos(List<Calendar> list)
    {
        if (mBySetPosList.size() <= 0) 
            return list;
        
        
        Calendar[] array = new Calendar[list.size()];
        array = list.toArray(array);
        
        LinkedList<Calendar> toRet = new LinkedList<Calendar>();
        
        ArrayList<Integer> idxsToInclude = new ArrayList<Integer>();
            
        for (Integer cur : mBySetPosList) {
            int idx = cur;
            if (idx>=-366 && idx <= 366 && idx!= 0) { 
                if (idx> 0)
                    idx--; // 1-indexed!
                else
                    idx = array.length + idx;
                
                if (idx>=0 && idx < array.length)
                    if (!idxsToInclude.contains(idx))
                        idxsToInclude.add(idx);
            }
        }
        
        Collections.sort(idxsToInclude);
        
        for (Integer idx : idxsToInclude) {
            toRet.add(array[idx]);
        }
        
        return toRet;
    }
    
    private void parse(String str, TimeZoneMap tzmap) throws ServiceException {
        try {
            for (String tok : str.split("\\s*;\\s*")) {
                String[] s = tok.split("\\s*=\\s*");
                if (s.length != 2) {
                    if (ZimbraLog.calendar.isDebugEnabled())
                        ZimbraLog.calendar.debug(new Formatter().format("Parse error for recur: \"%s\" at token \"%s\"", str, tok));
                    continue;
                }

                String rhs = s[1];
                // MS Exchange can add invalid spaces in RRULE part values.  Get rid of them.  (see bug 25169)
                rhs = rhs.replaceAll("\\s+", "");

                try {
                    switch(Tokens.valueOf(s[0])) { 
                    case FREQ:
                        mFreq = Frequency.valueOf(rhs);
                        break;
                    case UNTIL:
                        ParsedDateTime until = ParsedDateTime.parse(rhs, tzmap);
                        if (until != null) {
                            // RFC2445 4.3.10 Recurrence Rule:
                            // "If specified as a date-time value, then it MUST
                            // be specified in an UTC time format."
                            if (until.hasTime())
                                until.toUTC();
                            mUntil = until;
                        }
                        break;
                    case COUNT:
                        mCount = Integer.parseInt(rhs);
                        break;
                    case INTERVAL:
                        mInterval = Integer.parseInt(rhs);
                        break;
                    case BYSECOND:
                        parseIntList(rhs, mBySecondList, 0, 59, false);
                        break;
                    case BYMINUTE:
                        parseIntList(rhs, mByMinuteList, 0, 59, false);
                        break;
                    case BYHOUR:
                        parseIntList(rhs, mByHourList, 0, 23, false);
                        break;
                    case BYDAY:
                        parseByDayList(rhs, mByDayList);
                        break;
                    case BYMONTHDAY:
                        parseIntList(rhs, mByMonthDayList, -31, 31, true);
                        break;
                    case BYYEARDAY:
                        parseIntList(rhs, mByYearDayList, -366, 366, true);
                        break;
                    case BYWEEKNO:
                        parseIntList(rhs, mByWeekNoList, -53, 53, true);
                        break;
                    case BYMONTH:
                        parseIntList(rhs, mByMonthList, 1, 12, false);
                        break;
                    case BYSETPOS:
                        parseIntList(rhs, mBySetPosList, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
                        break;
                    case WKST:
                        mWkSt = ZWeekDay.valueOf(rhs);
                        break;
                    }
                } catch(IllegalArgumentException e) {
                    ZimbraLog.calendar.warn("Skipping RECUR token: \"%s\" in Recur \"%s\" due to parse error", s[0], str, e);
                }
            }
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Parse error for recur \""+str+"\"", e);
        }
    }

    private static int parseSignedInt(String str) {
        if (str == null)
            throw new NumberFormatException("null is not a number");
        int len = str.length();
        if (len == 0)
            throw new NumberFormatException("empty string is not a number");
        int num = 0;
        if (str.charAt(0) == '+') {
            if (len == 1)
                throw new NumberFormatException("+ is not a number");
            num = Integer.parseInt(str.substring(1));
        } else {
            num = Integer.parseInt(str);
        }
        return num;
    }

    private static int parseUnsignedInt(String str) {
        if (str == null)
            throw new NumberFormatException("null is not a number");
        int len = str.length();
        if (len == 0)
            throw new NumberFormatException("empty string is not a number");
        char sign = str.charAt(0);
        if (sign == '+' || sign == '-')
            throw new NumberFormatException("sign not allowed: " + str);
        return Integer.parseInt(str);
    }

    private void parseByDayList(String str, List<ZWeekDayNum> list) {
        for (String s : str.split("\\s*,\\s*")) {
            ZWeekDayNum wdn = new ZWeekDayNum();
            
            String dayStr = s;
            
            if (s.length() > 2) {
                String numStr = s.substring(0,s.length()-2);
                dayStr = dayStr.substring(s.length()-2);
                wdn.mOrdinal = parseSignedInt(numStr);
            }
            wdn.mDay = ZWeekDay.valueOf(dayStr);
            
            list.add(wdn);
        }

        // sort by DAY-OF-WEEK (necessary for the byDay checks to work)
        Collections.sort(list, new ZWeekDayNum.DayOnlyComparator());
    }

    private void parseIntList(String str, List<Integer> list, int min, int max, boolean signed) {
        for (String s : str.split("\\s*,\\s*"))
            try {
                int readInt;
                if (signed)
                    readInt = parseSignedInt(s);
                else
                    readInt = parseUnsignedInt(s);
                if (readInt >= min && readInt <= max) {
                    list.add(readInt);
                }
            } catch (Exception e) {
                ZimbraLog.calendar.debug(new Formatter().format("Skipping unparsable Recur int list entry: \"%s\" in parameter list: \"%s\"", s, str));
            }
        Collections.sort(list);
    }
}
