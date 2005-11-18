package com.zimbra.cs.mailbox.calendar;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.cs.service.ServiceException;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.parameter.Value;


public class ZRecur {
    public ZRecur(String str) throws ServiceException {
        TimeZoneMap tzmap = new TimeZoneMap(ICalTimeZone.getUTC());
        parse(str, tzmap);
        System.out.println("PARSED RECUR:\n"+myString());
        
        try {
            mRecur = new Recur(str);
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Parsing Recur string: \""+str+"\"", e);
        }
    }
    
    public String toString() {
        return mRecur.toString();
    }

    public ZRecur(Recur recur) throws ServiceException {
        mRecur = recur;
    }
    
    public static enum Frequency { SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY };
    
    public List<java.util.Date> expandRecurrence(ParsedDateTime dtStart, long rangeStart, long rangeEnd) throws ServiceException {
        List<java.util.Date> toRet = new LinkedList<java.util.Date>();
        
        
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
        
        
        
        switch (mFreq) {
        case SECONDLY:
            /*
             * BYSECOND - match iff second in list
             * BYMINUTE - match iff minute in list
             * BYHOUR - match iff hour in list
             * BYDAY - match iff day in list
             * BYMONTHDAY - match iff monthday in list
             * BYYEARDAY - match iff yearday in list
             * BYWEEKNO -- YEARLY ONLY
             * BYMONTH
             *
             * 
             */
            
            break;
        case MINUTELY:
            /*
             * BYSECOND - for each listed second in each minute 
             * BYMINUTE - match iff in minute list
             * BYHOUR
             * BYDAY
             * BYMONTHDAY
             * BYYEARDAY
             * BYWEEKNO -- YEARLY ONLY
             * BYMONTH
             * BYSETPOS
             * 
             * 
             * 
             * 
             */
            
            break;
        case HOURLY:
            /*
             * BYSECOND - for each listed second
             * BYMINUTE - for each listed minute in hour
             * BYHOUR - match iff in hour list
             * BYDAY - for each day listed
             * BYMONTHDAY - only those monthdays
             * BYYEARDAY - only those yeardays
             * BYMONTH - only those months
             * 
             *
             * 
             */
            
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
             *  for each (INTERVAL)DAY { 
             *    if (byMonth && !month matches)
             *      curDay = set MONTH to matching month
             *      continue
             *    else  
             *      if (byYearDay && !yearday matches)
             *        curDay = set DAY to next matching yearday
             *        continue
             *      else  
             *        if (byMonthday && !monthday matches)
             *          curDay = skip to next matching monthday
             *          continue
             *        else  
             *          if (byDay && !day in list)
             *            curDay = skip to next mathcing byDay
             *            continue
             *          else  
             *            if (!byHour or FOR EACH HOUR IN HOURLIST)
             *              if (!byMinute or FOR EACH MINUTE IN MINLIST)
             *                if (!bySecond or FOR EACH SECOND IN LIST)
             *                  ----add to list---
             *     
             *     check against BYSETPOS
             *                    
             *     curDay+=1 day
             * } while (count check & until check & rangeEnd check)
             * 
             */
            
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
             *   if (byMonth && !month matches)
             *     curDay = set MONTH to DtStart in next matching month
             *     continue
             *   else  
             *     if (byYearDay && !yearday matches)
             *       curDay = set date to next matching yearday
             *       continue
             *     else
             *       if (byMonthDay && !monthday matches)
             *         curDay = skip to next matching monthday  
             *         continue
             *       else
             *         if (!byDay or FOREACH day in list)
             *           if (!byHour or FOREACH hour in list)
             *             if (!byMinute or FOREACH minute in list)
             *               if (!bySecond or FOREACH second in list)
             *                 ----add to list----
             *         
             *    check against BYSETPOS
             *    
             *    curDay += 1 week
             * } while (count check & until check & rangeEnd check)
             * 
             */
            
            break;
        case MONTHLY:
            /*
             * BYSECOND
             * BYMINUTE
             * BYHOUR 
             * BYDAY - can have ordinal
             * BYMONTHDAY 
             * BYYEARDAY 
             * BYWEEKNO - IGNORED YEARLY RULE ONLY
             * BYMONTH - once
             * 
             * do {

             * 
             */
            
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
            
            break;
        }
        
        return toRet;
    }
    
    private int effectiveWeekStartDay() {
        if (mWkSt != null) 
            return mWkSt.getCalendarDay();
        else
            return Calendar.MONDAY;
    }
    
    public static enum ZWeekDay { 
        SU, MO, TU, WE, TH, FR, SA;
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
        public int mOrdinal; // -4,-3,-2,-1,+1,+2,+3,+4
        public ZWeekDay mDay;
        
        public ZWeekDayNum(int ord, ZWeekDay day) { mOrdinal = ord; mDay = day; }
        public ZWeekDayNum() {};
        
        public String toString() {
            if (mOrdinal != 0) 
                return Integer.toString(mOrdinal)+mDay;
            else
                return mDay.toString();
        }
    }
    
    public static String listAsStr(List l) {
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
    
    public Frequency getFrequency() {
        return Frequency.valueOf(mRecur.getFrequency());
    }

    public int getInterval() { 
        return mRecur.getInterval(); 
    }
    
    public ParsedDateTime getUntil() { 
        net.fortuna.ical4j.model.Date date = mRecur.getUntil();
        if (date != null) 
            return ParsedDateTime.fromUTCTime(date.getTime());
        else
            return null;
    }

    public int getCount() { 
        return mRecur.getCount();
    }
    
    public List<Integer> getBySecondList() { 
        return (List<Integer>)(mRecur.getSecondList());
    }
    
    public List<Integer> getByMinuteList() {
        return (List<Integer>)(mRecur.getMinuteList());
    }
    
    public List<Integer> getByHourList() { 
        return (List<Integer>)(mRecur.getHourList());
    }
    
    public List<ZWeekDayNum> getByDayList() {
        List<ZWeekDayNum> toRet = new ArrayList<ZWeekDayNum>();
        
        WeekDayList wdl = mRecur.getDayList();
        for (WeekDay wd : ((List<WeekDay>)wdl)) {
            ZWeekDay zwd = ZWeekDay.valueOf(wd.getDay());
            
            toRet.add(new ZWeekDayNum(wd.getOffset(), zwd));
        }
        return toRet;
    }
    
    public List<Integer> getByMonthDayList() {
        return (List<Integer>)(mRecur.getMonthDayList());
    }
    
    public List<Integer> getByYearDayList() { 
        return (List<Integer>)(mRecur.getYearDayList());        
    }
    
    public List<Integer> getByWeekNoList() { 
        return (List<Integer>)(mRecur.getWeekNoList());        
    }
    
    public List<Integer> getByMonthList() { 
        return (List<Integer>)(mRecur.getMonthList());        
    }
    
    public List<Integer> getBySetPosList() { 
        return (List<Integer>)(mRecur.getSetPosList());        
    }
    
    public ZWeekDay getWkSt() {
        String wkSt = mRecur.getWeekStartDay();
        if (wkSt != null) 
            return ZWeekDay.valueOf(wkSt);
        else
            return null;
    }
    
//    public List<Date> expandDates(mDtStart.iCal4jDate(), dateStart, endDate, Value.DATE_TIME);
    public List<java.util.Date> expandRecurrenceOverRange(ParsedDateTime dtStart, long rangeStart, long rangeEnd) throws ServiceException {
        net.fortuna.ical4j.model.DateTime dateStart = new net.fortuna.ical4j.model.DateTime(rangeStart);
        net.fortuna.ical4j.model.DateTime dateEnd = new net.fortuna.ical4j.model.DateTime(rangeEnd);
        
        List dateList = mRecur.getDates(dtStart.iCal4jDate(), dateStart, dateEnd, Value.DATE_TIME);
        
        return dateList;
    }
    

    Recur getRecur() { return mRecur; }
    
    private Recur mRecur;
    
    private Frequency mFreq = null;
    private ParsedDateTime mUntil = null;
    private int mCount = 0;
    private int mInterval = 0;
    
    private List<Integer> mBySecondList = new ArrayList<Integer>();
    private List<Integer> mByMinuteList = new ArrayList<Integer>();
    private List<Integer> mByHourList = new ArrayList<Integer>();
    private List<ZWeekDayNum> mByDayList = new ArrayList<ZWeekDayNum>();
    private List<Integer> mByMonthDayList = new ArrayList<Integer>();
    private List<Integer> mByYearDayList = new ArrayList<Integer>();
    private List<Integer> mByWeekNoList = new ArrayList<Integer>();
    private List<Integer> mByMonthList = new ArrayList<Integer>();
    private List<Integer> mBySetPosList = new ArrayList<Integer>();
    
    
    private ZWeekDay mWkSt = null;
    
    
    String myString() {
        StringBuffer toRet = new StringBuffer("FREQ=").append(mFreq);
        
        if (mUntil != null) 
            toRet.append(';').append("UNTIL=").append(mUntil);
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
    
    private void parse(String str, TimeZoneMap tzmap) throws ServiceException {
        boolean atFreq = false;
        
        try {
        for (String tok : str.split(";")) {
            String[] s = tok.split("=");
            if (s.length != 2) 
                throw ServiceException.FAILURE("Parse error for recur \""+str+"\" at token \""+tok+"\"", null);
            
            String rhs = s[1];
            
            switch(Tokens.valueOf(s[0])) { 
            case FREQ:
                mFreq = Frequency.valueOf(rhs);
                break;
            case UNTIL:
                mUntil = ParsedDateTime.parse(rhs, tzmap);
                break;
            case COUNT:
                mCount = Integer.parseInt(rhs);
                break;
            case INTERVAL:
                mInterval = Integer.parseInt(rhs);
                break;
            case BYSECOND:
                parseIntList(rhs, mBySecondList);
                break;
            case BYMINUTE:
                parseIntList(rhs, mByMinuteList);
                break;
            case BYHOUR:
                parseIntList(rhs, mByHourList);
                break;
            case BYDAY:
                parseByDayList(rhs, mByDayList);
                break;
            case BYMONTHDAY:
                parseIntList(rhs, mByMonthDayList);
                break;
            case BYYEARDAY:
                parseIntList(rhs, mByYearDayList);
                break;
            case BYWEEKNO:
                parseIntList(rhs, mByWeekNoList);
                break;
            case BYMONTH:
                parseIntList(rhs, mByMonthList);
                break;
            case BYSETPOS:
                parseIntList(rhs, mBySetPosList);
                break;
            case WKST:
                mWkSt = ZWeekDay.valueOf(rhs);
                break;
            }
        }
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Parse error for recur \""+str+"\"", null);
        }
    }
    
    private void parseIntList(String str, List<Integer> list) {
        for (String s : str.split(",")) 
            list.add(new Integer(s));
    }
    
    private void parseByDayList(String str, List<ZWeekDayNum> list) {
        for (String s : str.split(",")) {
            ZWeekDayNum wdn = new ZWeekDayNum();
            
            String dayStr = s;
            
            if (s.length() > 2) {
                String numStr = s.substring(0,s.length()-2);
                dayStr = dayStr.substring(s.length()-2);
                wdn.mOrdinal = Integer.parseInt(numStr);
            }
            wdn.mDay = ZWeekDay.valueOf(dayStr);
            
            list.add(wdn);
        }
    }
    
    
    private static enum Tokens {
        FREQ, UNTIL, COUNT, INTERVAL, BYSECOND, BYMINUTE, BYHOUR, BYDAY, 
        BYMONTHDAY, BYYEARDAY, BYWEEKNO, BYMONTH, BYSETPOS, WKST;
    }
    
}
