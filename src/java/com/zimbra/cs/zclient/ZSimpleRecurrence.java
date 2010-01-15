/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.zclient.ZInvite.ZByDayWeekDay;
import com.zimbra.cs.zclient.ZInvite.ZByRule;
import com.zimbra.cs.zclient.ZInvite.ZByType;
import com.zimbra.cs.zclient.ZInvite.ZFrequency;
import com.zimbra.cs.zclient.ZInvite.ZRecurrence;
import com.zimbra.cs.zclient.ZInvite.ZRecurrenceRule;
import com.zimbra.cs.zclient.ZInvite.ZWeekDay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ZSimpleRecurrence {

    public enum ZSimpleRecurrenceType {
        NONE,
        DAILY,
        DAILY_WEEKDAY,
        DAILY_INTERVAL,
        WEEKLY,
        WEEKLY_BY_DAY,
        WEEKLY_CUSTOM,
        MONTHLY,
        MONTHLY_BY_MONTH_DAY,
        MONTHLY_RELATIVE,
        YEARLY,
        YEARLY_BY_DATE,
        YEARLY_RELATIVE,
        COMPLEX;

        public static ZSimpleRecurrenceType fromString(String s) throws ServiceException {
            try {
                return ZSimpleRecurrenceType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid type "+s+", valid values: "+ Arrays.asList(ZSimpleRecurrenceType.values()), e);
            }
        }

        public boolean isNone() { return this.equals(NONE); }
        public boolean isDaily() { return this.equals(DAILY); }
        public boolean isDailyWeekday() { return this.equals(DAILY_WEEKDAY); }
        public boolean isDailyInterval() { return this.equals(DAILY_INTERVAL); }
        public boolean isWeekly() { return this.equals(WEEKLY); }
        public boolean isWeeklyByDay() { return this.equals(WEEKLY_BY_DAY); }
        public boolean isWeeklyCustom() { return this.equals(WEEKLY_CUSTOM); }
        public boolean isMonthly() { return this.equals(MONTHLY); }
        public boolean isMonthlyByMonthDay() { return this.equals(MONTHLY_BY_MONTH_DAY); }
        public boolean isMonthlyRelative() { return this.equals(MONTHLY_RELATIVE); }
        public boolean isYearly() { return this.equals(YEARLY); }
        public boolean isYearlyByDate() { return this.equals(YEARLY_BY_DATE); }
        public boolean isYearlyRelative() { return this.equals(YEARLY_RELATIVE); }
        public boolean isComplex() { return this.equals(COMPLEX); }
    }

    public enum ZSimpleRecurrenceEnd {
        NEVER, COUNT, UNTIL;

        public static ZSimpleRecurrenceEnd fromString(String s) throws ServiceException {
            try {
                return ZSimpleRecurrenceEnd.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid end "+s+", valid values: "+ Arrays.asList(ZSimpleRecurrenceEnd.values()), e);
            }
        }

        public boolean isNever() { return this.equals(NEVER); }
        public boolean isCount() { return this.equals(COUNT); }
        public boolean isUntil() { return this.equals(UNTIL); }
    }


    private ZSimpleRecurrenceType mType;
    private ZSimpleRecurrenceEnd mEnd;
    private long mCount;
    private ZDateTime mUntilDate;

    /* NONE: needs no state */
    /* DAILY: need no state */
    /* DAILY_WEEKDAY: needs no state */
    /* DAILY_INTERVAL */
    private int mDailyInterval;
    /* WEEKLY: needs no state */
    /* WEEKLY_BY_DAY */
    private ZWeekDay mWeeklyByDay;
    /* WEEKLY_CUSTOM */
    private int mWeeklyInterval;
    private List<ZWeekDay> mWeeklyIntervalDays;
    /* MONTHLY: needs no state */
    /* MONTHLY_{BY_MONTH_DAY, RELATIVE} */
    private int mMonthlyInterval;
    /* MONTHLY_BY_MONTH_DAY */
    private int mMonthlyMonthDay;
    /* MONTHLY_RELATIVE */
    private ZByDayWeekDay mMonthlyRelativeDay;
    /* YEARLY: needs no state */
    /* YEARLY_BY_DATE */
    private int mYearlyByDateMonthDay;
    private int mYearlyByDateMonth;
    /* YEARLY_RELATIVE */
    private ZByDayWeekDay mYearlyRelativeDay;
    private int mYearlyRelativeMonth;

    private boolean empty(List<?> list) {
        return list == null || list.size() == 0;
    }

    public ZSimpleRecurrence(ZRecurrence r) {
        mEnd = ZSimpleRecurrenceEnd.NEVER;
        
        if (r == null) {
            mType = ZSimpleRecurrenceType.NONE;
            return;
        }

        if (    (r.getRules() == null || r.getRules().size() > 1) ||
                !empty(r.getDates()) || !empty(r.getExRules()) || !empty(r.getExDates())) {
            mType = ZSimpleRecurrenceType.COMPLEX;
            return;
        }
        ZRecurrenceRule rr = r.getRules().get(0);

        /* none of our simple rules have this set */
        if (rr.getWeekStart() != null) {
            mType = ZSimpleRecurrenceType.COMPLEX;
            return;
        }

        List<ZByRule> byRules = rr.getByRules();

        if (rr.getUntilDate() != null) {
            mEnd = ZSimpleRecurrenceEnd.UNTIL;
            mUntilDate = rr.getUntilDate();
        } else if (rr.getCount() > 0) {
            mCount = rr.getCount();
            mEnd = ZSimpleRecurrenceEnd.COUNT;
        } else {
            mEnd = ZSimpleRecurrenceEnd.NEVER;
        }

        switch (rr.getFrequency()) {
            case DAI:
                if (empty(byRules)) {
                    if (rr.getInterval() < 2) {
                        mType = ZSimpleRecurrenceType.DAILY; /////
                    } else {
                        mType = ZSimpleRecurrenceType.DAILY_INTERVAL; /////
                        mDailyInterval = rr.getInterval();
                    }
                    return;
                } else if (byRules != null && byRules.size() == 1 && byRules.get(0).getType() == ZByType.BY_DAY) {
                    ZByRule br = byRules.get(0);
                    List<ZByDayWeekDay> weekDays = br.getByDayWeekDays();
                    if (!empty(weekDays)) {
                        Set<ZWeekDay> days = EnumSet.noneOf(ZWeekDay.class);
                        for (ZByDayWeekDay d : weekDays) {
                            if (d.getWeekOrd() == 0)
                                days.add(d.getDay());
                        }
                        if (days.size() == 5 && days.containsAll(EnumSet.of(ZWeekDay.MO, ZWeekDay.TU,  ZWeekDay.WE, ZWeekDay.TH, ZWeekDay.FR))) {
                            mType = ZSimpleRecurrenceType.DAILY_WEEKDAY; /////
                            return;
                        }
                    }
                }
                break;
            case WEE:
                if (empty(byRules) && rr.getInterval() < 2 && mEnd == ZSimpleRecurrenceEnd.NEVER) {
                    mType = ZSimpleRecurrenceType.WEEKLY; /////
                    return;
                } else if (byRules != null && byRules.size() == 1 && byRules.get(0).getType() == ZByType.BY_DAY) {
                    ZByRule br = byRules.get(0);
                    List<ZByDayWeekDay> weekDays = br.getByDayWeekDays();

                    if (weekDays != null && weekDays.size() == 1 && rr.getInterval() <2) {
                        ZByDayWeekDay day = weekDays.get(0);
                        if (day.getWeekOrd() == 0) {
                            mType = ZSimpleRecurrenceType.WEEKLY_BY_DAY; /////
                            mWeeklyByDay = day.getDay();
                            return;
                        }
                    } else if (weekDays != null) {
                        mWeeklyInterval = rr.getInterval();
                        mWeeklyIntervalDays = new ArrayList<ZWeekDay>();
                        for (ZByDayWeekDay day : weekDays) {
                            if (day.getWeekOrd() == 0) {
                                mWeeklyIntervalDays.add(day.getDay());
                            }
                        }
                        if (mWeeklyIntervalDays.size() == weekDays.size()) {
                            Collections.sort(mWeeklyIntervalDays);
                            mType = ZSimpleRecurrenceType.WEEKLY_CUSTOM; /////
                            return;
                        }
                    }
                }
                break;
            case MON:
                if (empty(byRules) && rr.getInterval() < 2 && mEnd == ZSimpleRecurrenceEnd.NEVER) {
                    mType = ZSimpleRecurrenceType.MONTHLY; /////
                    return;
                } else if (byRules != null && byRules.size() == 1 && byRules.get(0).getType() == ZByType.BY_MONTHDAY) {
                    ZByRule br = byRules.get(0);
                    String[] days = br.getList();
                    if (days.length == 1) {
                        mMonthlyMonthDay = parseInt(days[0], -1);
                        if (mMonthlyMonthDay > 0) {
                            mType = ZSimpleRecurrenceType.MONTHLY_BY_MONTH_DAY; /////
                            mMonthlyInterval = rr.getInterval();
                            return;
                        }
                    }
                } else if (byRules != null && byRules.size() == 1 && byRules.get(0).getType() == ZByType.BY_DAY) {
                    ZByRule br = byRules.get(0);
                    List<ZByDayWeekDay> weekDays = br.getByDayWeekDays();
                    if (weekDays != null && weekDays.size() == 1) {
                        ZByDayWeekDay day = weekDays.get(0);
                        int ord = day.getWeekOrd();
                        if (ord == -1 || (ord > 0 && ord < 5)) {
                            mType = ZSimpleRecurrenceType.MONTHLY_RELATIVE; /////
                            mMonthlyRelativeDay = day;
                            mMonthlyInterval = rr.getInterval();
                            return;
                        }
                    }
                }
                break;
            case YEA:
                if (empty(byRules) && rr.getInterval() < 2 && mEnd == ZSimpleRecurrenceEnd.NEVER) {
                    mType = ZSimpleRecurrenceType.YEARLY; /////
                    return;
                } else if (rr.getInterval() < 2 && byRules != null && byRules.size() == 2) {
                    ZByRule bymonth = findType(byRules, ZByType.BY_MONTH);
                    ZByRule bymonthday = findType(byRules, ZByType.BY_MONTHDAY);
                    ZByRule byday = findType(byRules, ZByType.BY_DAY);
                    if (bymonthday != null && bymonth != null) {
                        String monthdays[] = bymonthday.getList();
                        String months[] = bymonth.getList();
                        if (monthdays.length == 1 && months.length == 1) {
                            mYearlyByDateMonthDay = parseInt(monthdays[0], -1);
                            mYearlyByDateMonth = parseInt(months[0], -1);
                            if (mYearlyByDateMonth != -1 && mYearlyByDateMonthDay != -1) {
                                mType = ZSimpleRecurrenceType.YEARLY_BY_DATE; /////
                                return;
                            }
                        }
                    } else if (byday != null && bymonth != null) {
                        String months[] = bymonth.getList();
                        List<ZByDayWeekDay> weekDays = byday.getByDayWeekDays();
                        if (weekDays != null && weekDays.size() == 1 && months.length == 1) {
                            mYearlyRelativeMonth = parseInt(months[0], -1);
                            ZByDayWeekDay day = weekDays.get(0);
                            int ord = day.getWeekOrd();
                            if (mYearlyRelativeMonth != -1 && (ord == -1 || (ord > 0 && ord < 5))) {
                                mType = ZSimpleRecurrenceType.YEARLY_RELATIVE;
                                mYearlyRelativeDay = day; /////
                                return;
                            }
                        }
                    }
                }
                break;
        }
        mType = ZSimpleRecurrenceType.COMPLEX;
    }

    /**
     *
     * @param value value to parse as int
     * @param defaultValue default value if can't parse
     * @return int value, or defaultValue if can't parse
     */
    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private ZByRule findType(List<ZByRule> byRules, ZByType type) {
        for (ZByRule rule : byRules)
            if (rule.getType() == type) return rule;
        return null;
    }

    public void setCount(long count) {
        mCount = count;
    }

    public long getCount() {
        return mCount;
    }

    public void setUntilDate(ZDateTime untilDate) {
        mUntilDate = untilDate;
    }
    
    public ZDateTime getUntilDate() {
        return mUntilDate;
    }

    public ZSimpleRecurrenceType getType() {
        return mType;
    }

    public void setType(ZSimpleRecurrenceType type) {
        mType = type;
    }

    public ZSimpleRecurrenceEnd getEnd() {
        return mEnd;
    }

    public void setEnd(ZSimpleRecurrenceEnd end) {
        mEnd = end;
    }

    public int getDailyInterval() {
        return mDailyInterval;
    }

    public void setDailyInterval(int dailyInterval) {
        mDailyInterval = dailyInterval;
    }

    public ZWeekDay getWeeklyByDay() {
        return mWeeklyByDay;
    }

    public void setWeeklyByDay(ZWeekDay weeklyByDay) {
        mWeeklyByDay = weeklyByDay;
    }

    public int getWeeklyInterval() {
        return mWeeklyInterval;
    }

    public void setWeeklyInterval(int weeklyInterval) {
        mWeeklyInterval = weeklyInterval;
    }

    public List<ZWeekDay> getWeeklyIntervalDays() {
        return mWeeklyIntervalDays;
    }

    public void setWeeklyIntervalDays(List<ZWeekDay> weeklyIntervalDays) {
        mWeeklyIntervalDays = weeklyIntervalDays;
    }

    public int getMonthlyInterval() {
        return mMonthlyInterval;
    }

    public void setMonthlyInterval(int monthlyInterval) {
        mMonthlyInterval = monthlyInterval;
    }

    public int getMonthlyMonthDay() {
        return mMonthlyMonthDay;
    }

    public void setMonthlyMonthDay(int monthlyMonthDay) {
        mMonthlyMonthDay = monthlyMonthDay;
    }

    public ZByDayWeekDay getMonthlyRelativeDay() {
        return mMonthlyRelativeDay;
    }

    public void setMonthlyRelativeDay(ZByDayWeekDay monthlyRelativeDay) {
        mMonthlyRelativeDay = monthlyRelativeDay;
    }

    public int getYearlyByDateMonthDay() {
        return mYearlyByDateMonthDay;
    }

    public void setYearlyByDateMonthDay(int yearlyByDateMonthDay) {
        mYearlyByDateMonthDay = yearlyByDateMonthDay;
    }

    public int getYearlyByDateMonth() {
        return mYearlyByDateMonth;
    }

    public void setYearlyByDateMonth(int yearlyByDateMonth) {
        mYearlyByDateMonth = yearlyByDateMonth;
    }

    public ZByDayWeekDay getYearlyRelativeDay() {
        return mYearlyRelativeDay;
    }

    public void setYearlyRelativeDay(ZByDayWeekDay yearlyRelativeDay) {
        mYearlyRelativeDay = yearlyRelativeDay;
    }

    public int getYearlyRelativeMonth() {
        return mYearlyRelativeMonth;
    }

    public void setYearlyRelativeMonth(int yearlyRelativeMonth) {
        mYearlyRelativeMonth = yearlyRelativeMonth;
    }

    public ZRecurrence getRecurrence() {
        ZRecurrence recur = new ZRecurrence();

        if (getType().isComplex() || getType().isNone())
            return recur;
        
        ZRecurrenceRule rule = new ZRecurrenceRule();
        List<ZRecurrenceRule> rules = new ArrayList<ZRecurrenceRule>();
        rules.add(rule);
        recur.setRules(rules);

        switch (getType()) {
            case DAILY:
                rule.setFrequency(ZFrequency.DAI);
                break;
            case DAILY_INTERVAL:
                rule.setFrequency(ZFrequency.DAI);
                rule.setInterval(getDailyInterval());
                break;
            case DAILY_WEEKDAY:
                rule.setFrequency(ZFrequency.DAI);
                rule.setByRules(new ArrayList<ZByRule>());
                rule.getByRules().add(
                        new ZByRule(ZByType.BY_DAY, null,
                                ZByDayWeekDay.getList(ZWeekDay.MO, ZWeekDay.TU, ZWeekDay.WE, ZWeekDay.TH, ZWeekDay.FR)));
                break;
            case WEEKLY:
                rule.setFrequency(ZFrequency.WEE);
                break;
            case WEEKLY_BY_DAY:
                rule.setFrequency(ZFrequency.WEE);
                rule.setByRules(new ArrayList<ZByRule>());
                rule.getByRules().add(new ZByRule(ZByType.BY_DAY, null, ZByDayWeekDay.getList(getWeeklyByDay())));
                break;
            case WEEKLY_CUSTOM:
                rule.setFrequency(ZFrequency.WEE);
                rule.setInterval(getWeeklyInterval());
                rule.setByRules(new ArrayList<ZByRule>());
                rule.getByRules().add(new ZByRule(ZByType.BY_DAY, null, ZByDayWeekDay.getList(getWeeklyIntervalDays())));
                break;
            case MONTHLY:
                rule.setFrequency(ZFrequency.MON);
                break;
            case MONTHLY_BY_MONTH_DAY:
                rule.setFrequency(ZFrequency.MON);
                rule.setInterval(getMonthlyInterval());
                rule.setByRules(new ArrayList<ZByRule>());
                rule.getByRules().add(new ZByRule(ZByType.BY_MONTHDAY, Integer.toString(getMonthlyMonthDay()), null));
                break;
            case MONTHLY_RELATIVE:
                rule.setFrequency(ZFrequency.MON);
                rule.setInterval(getMonthlyInterval());
                rule.setByRules(new ArrayList<ZByRule>());
                rule.getByRules().add(new ZByRule(ZByType.BY_DAY, null, Arrays.asList(getMonthlyRelativeDay())));
                break;
            case YEARLY:
                rule.setFrequency(ZFrequency.YEA);
                break;
            case YEARLY_BY_DATE:
                rule.setFrequency(ZFrequency.YEA);
                rule.setByRules(new ArrayList<ZByRule>());
                rule.getByRules().add(new ZByRule(ZByType.BY_MONTHDAY, Integer.toString(getYearlyByDateMonthDay()), null));
                rule.getByRules().add(new ZByRule(ZByType.BY_MONTH, Integer.toString(getYearlyByDateMonth()), null));
                break;
            case YEARLY_RELATIVE:
                rule.setFrequency(ZFrequency.YEA);
                rule.setByRules(new ArrayList<ZByRule>());
                rule.getByRules().add(new ZByRule(ZByType.BY_DAY, null, Arrays.asList(getYearlyRelativeDay())));
                rule.getByRules().add(new ZByRule(ZByType.BY_MONTH, Integer.toString(getYearlyRelativeMonth()), null));
                break;
        }
        
        switch(getEnd()) {
            case NEVER:
                break;
            case COUNT:
                rule.setCount((int)getCount());
                break;
            case UNTIL:
                rule.setUntilDate(getUntilDate());
                break;
        }
        
        return recur;
    }

}
