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

package com.zimbra.cs.fb;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.fb.FreeBusy.Interval;
import com.zimbra.cs.fb.FreeBusy.IntervalList;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZRecur;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.ZRecur.ZWeekDay;

public class WorkingHours {

    public static FreeBusy getWorkingHours(Account authAcct, boolean asAdmin, Account account, long start, long end)
    throws ServiceException {
        // If free/busy viewing is blocked, so is viewing working hours.
        AccessManager accessMgr = AccessManager.getInstance();
        boolean accountAceAllowed = accessMgr.canDo(authAcct, account, User.R_viewFreeBusy, asAdmin);
        if (!accountAceAllowed)
            return FreeBusy.nodataFreeBusy(account.getName(), start, end);

        // Get the working hours preference and parse it.
        String workingHoursPref = account.getPrefCalendarWorkingHours();
        HoursByDay workingHoursByDay = new HoursByDay(workingHoursPref);

        // Build a recurrence rule for each day of the week and expand over the time range.
        IntervalList intervals = new IntervalList(start, end);
        ICalTimeZone tz = ICalTimeZone.getAccountTimeZone(account);
        TimeZoneMap tzmap = new TimeZoneMap(tz);
        StartSpec startSpec = new StartSpec(start, tz);
        for (int day = 1; day <= 7; ++day) {
            TimeRange timeRange = workingHoursByDay.getHoursForDay(day);
            if (timeRange.enabled) {
                IRecurrence rrule = getRecurrenceForDay(day, startSpec, timeRange, tz, tzmap);
                List<Instance> instances = rrule.expandInstances(0, start, end);
                for (Instance inst : instances) {
                    Interval ival = new Interval(inst.getStart(), inst.getEnd(), IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE);
                    intervals.addInterval(ival);
                }
            }
        }
        // Invert FREE and BUSY_UNAVAILABLE in the intervals so that working hours are displayed as free and non-working
        // hours are shown as out-of-office.
        for (Iterator<Interval> iter = intervals.iterator(); iter.hasNext(); ) {
            Interval interval = iter.next();
            String status = interval.getStatus();
            interval.setStatus(invertStatus(status));
        }
        return new FreeBusy(account.getName(), intervals, start, end);
    }

    public static void validateWorkingHoursPref(String pref) throws ServiceException {
        new HoursByDay(pref);
    }

    private static String invertStatus(String status) {
        if (IcalXmlStrMap.FBTYPE_FREE.equals(status))
            return IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE;
        else
            return IcalXmlStrMap.FBTYPE_FREE;
    }

    private static IRecurrence getRecurrenceForDay(int dayOfWeek, StartSpec startSpec, TimeRange timeRange, ICalTimeZone tz, TimeZoneMap tzmap)
    throws ServiceException {
        // DTSTART
        String dateStr = startSpec.getDateString(dayOfWeek);
        String dtStartStr;
        if (tz.sameAsUTC())
            dtStartStr = String.format("%sT%02d%02d00Z", dateStr, timeRange.start.hour, timeRange.start.minute);
        else
            dtStartStr = String.format("TZID=\"%s\":%sT%02d%02d00", tz.getID(), dateStr, timeRange.start.hour, timeRange.start.minute);
        ParsedDateTime dtStart;
        try {
            dtStart = ParsedDateTime.parse(dtStartStr, tzmap);
        } catch (ParseException e) {
            throw ServiceException.INVALID_REQUEST("Bad date/time value \"" + dtStartStr + "\"", e);
        }
        // DURATION
        ParsedDuration dur = timeRange.getDuration();
        // RRULE
        String dayName = DayOfWeekName.lookup(dayOfWeek);
        String ruleStr = String.format("FREQ=WEEKLY;INTERVAL=1;BYDAY=%s", dayName);
        return new Recurrence.SimpleRepeatingRule(dtStart, dur, new ZRecur(ruleStr, tzmap), null);
    }

    private static class DayOfWeekName {
        private static ZWeekDay[] sNames = new ZWeekDay[] {
            ZWeekDay.SU, ZWeekDay.MO, ZWeekDay.TU, ZWeekDay.WE, ZWeekDay.TH, ZWeekDay.FR, ZWeekDay.SA
        };
        public static String lookup(int dayOfWeek) throws ServiceException {
            if (dayOfWeek >= 1 && dayOfWeek <= sNames.length)
                return sNames[dayOfWeek - 1].toString();
            throw ServiceException.INVALID_REQUEST("Invalid day of week " + dayOfWeek, null);
        }
    }

    private static class HourMinute {
        public int hour;
        public int minute;
        public HourMinute(int hh, int mm) {
            hour = hh;
            minute = mm;
        }

        public boolean laterThan(HourMinute other) {
            return this.hour > other.hour || (this.hour == other.hour && this.minute > other.minute);
        }

        // timeStr must have the format HHMM.  Hour range is 0-24, and minute range is 0-59.
        // Special value "2400" is allowed for denoting end time of the working hours that coincides with the end of the day.
        public HourMinute(String hhmmStr, boolean isEndTime, String dayStr)
        throws ServiceException {
            boolean good = false;
            if (hhmmStr.length() == 4) {
                try {
                    int hh = Integer.parseInt(hhmmStr.substring(0, 2));
                    int mm = Integer.parseInt(hhmmStr.substring(2));
                    if ((hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59) || (isEndTime && hh == 24 && mm == 0)) {
                        hour = hh;
                        minute = mm;
                        good = true;
                    }
                } catch (NumberFormatException e) {}
            }
            if (!good)
                throw ServiceException.INVALID_REQUEST(
                        "Working hours spec day section \"" + dayStr + "\" has invalid " +
                        (isEndTime ? "end" : "start") + " time \"" + hhmmStr + "\"", null);
        }
    }

    private static class TimeRange {
        public HourMinute start;
        public HourMinute end;
        public boolean enabled;
        public TimeRange(HourMinute start, HourMinute end, boolean enabled) {
            this.start = start;
            this.end = end;
            this.enabled = enabled;
        }
        public ParsedDuration getDuration() {
            int diff = end.hour * 60 + end.minute - (start.hour * 60 + start.minute);
            int hours = diff / 60;
            int minutes = diff % 60;
            return ParsedDuration.parse(false, 0, 0, hours, minutes, 0);
        }
    }

    private static class HoursByDay {

        private Map<Integer /* 1..7 */, TimeRange> mHours = new HashMap<Integer, TimeRange>(7);

        public HoursByDay() {
            HourMinute hhmm0000 = new HourMinute(0, 0);
            HourMinute hhmm2400 = new HourMinute(24, 0);
            TimeRange allHours = new TimeRange(hhmm0000, hhmm2400, false);
            for (int day = 1; day <= 7; ++day) {
                mHours.put(day, allHours);
            }
        }

        public TimeRange getHoursForDay(int dayNum) {
            return mHours.get(dayNum);
        }

        public HoursByDay(String prefStr) throws ServiceException {
            if (!prefStr.matches("[^\\s]+"))
                throw ServiceException.INVALID_REQUEST("Working hours spec should not have whitespaces", null);
            if (prefStr.endsWith(","))
                throw ServiceException.INVALID_REQUEST("Working hours spec should not have trailing commas", null);

            int daySpecified[] = new int[] { 0, 0, 0, 0, 0, 0, 0 };  // tracks which days of the week are specified

            String days[] = prefStr.split(",");
            if (days.length != 7)
                throw ServiceException.INVALID_REQUEST("Working hours spec must specify all days of a week", null);
            for (int i = 0; i < days.length; ++i) {
                if (days[i].endsWith(":"))
                    throw ServiceException.INVALID_REQUEST(
                            "Working hours spec day section \"" + days[i] + "\" should not have trailing colons", null);
                String parts[] = days[i].split(":");
                if (parts.length != 4)
                    throw ServiceException.INVALID_REQUEST(
                            "Working hours spec day section \"" + days[i] + "\" must have 4 colon-separated parts", null);

                // First part is day number, 1 (Sunday) to 7 (Saturday).
                int dayNum = -1;
                try {
                    dayNum = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {}
                if (dayNum < 1 || dayNum > 7)
                    throw ServiceException.INVALID_REQUEST(
                            "Working hours spec day section \"" + days[i] + "\" has invalid day number (must be 1 to 7)", null);

                // Don't allow specifying the same day twice.
                if (daySpecified[dayNum-1] != 0)
                    throw ServiceException.INVALID_REQUEST(
                            "Working hours spec must not specify the same day more than once; found repeated day " + dayNum, null);
                daySpecified[dayNum-1] = 1;

                // Second part is a flag indicating if the working hours for the day are in effect ("Y") or not ("N").
                if (parts[1].length() != 1)
                    throw ServiceException.INVALID_REQUEST(
                            "Working hours spec day section \"" + days[i] + "\" has invalid on/off flag (must be Y or N)", null);
                char flag = parts[1].charAt(0);
                if (flag != 'Y' && flag != 'N')
                    throw ServiceException.INVALID_REQUEST(
                            "Working hours spec day section \"" + days[i] + "\" has invalid on/off flag (must be Y or N)", null);
                boolean enabled = flag == 'Y';

                // Third part is the start time of the working hours.  Format is HHMM with 24-hour hour.  Range is 0000 to 2359.
                HourMinute startTime = new HourMinute(parts[2], false, days[i]);

                // Fourth part is the end time of the working hours.  Range is 0000 to 2400.
                HourMinute endTime = new HourMinute(parts[3], true, days[i]);

                // End time cannot be earlier than start time.
                if (startTime.laterThan(endTime))
                    throw ServiceException.INVALID_REQUEST(
                            "Working hours spec day section \"" + days[i] + "\" has end time earlier than start time", null);

                mHours.put(dayNum, new TimeRange(startTime, endTime, enabled));
            }
        }
    }

    private static class StartSpec {
        private ICalTimeZone mTZ;
        private long mUtcTime;
        private Calendar mCal;

        public StartSpec(long time, ICalTimeZone tz) {
            mTZ = tz;
            mUtcTime = time;
            mCal = new GregorianCalendar(mTZ);
            mCal.setTimeInMillis(mUtcTime);
        }

        /**
         * Return the YYYYMMDD string for the first day matching the dayOfWeek on or after the start time.
         * @param dayOfWeek
         */
        public String getDateString(int dayOfWeek) {
            int startDayOfWeek = mCal.get(Calendar.DAY_OF_WEEK);
            Calendar cal;
            if (dayOfWeek == startDayOfWeek) {
                cal = mCal;
            } else {
                int daysDelta = (dayOfWeek + 7 - startDayOfWeek) % 7;
                cal = (Calendar) mCal.clone();
                cal.add(Calendar.DAY_OF_YEAR, daysDelta);
            }
            return String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
    }
}
