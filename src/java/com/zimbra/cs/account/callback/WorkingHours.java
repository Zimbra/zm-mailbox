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
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

public class WorkingHours extends AttributeCallback {

    // Value must be a comma-separated string whose parts are colon-separated strings.
    // Each comma-separated part specifies the working hours of a day of the week.
    // Each day of the week must be specified exactly once.
    // 
    @Override
    public void preModify(Map context, String attrName, Object attrValue, Map attrsToModify, Entry entry, boolean isCreate)
    throws ServiceException {
        if (attrValue == null) return;  // Allow unsetting.
        if (!(attrValue instanceof String))
            throw ServiceException.INVALID_REQUEST(attrValue + " is a single-valued string", null);
        String value = (String) attrValue;
        if (value.length() == 0) return;  // Allow unsetting.
        if (!value.matches("[^\\s]+"))
            throw ServiceException.INVALID_REQUEST(attrName + " should not have whitespaces", null);
        if (value.endsWith(","))
            throw ServiceException.INVALID_REQUEST(attrName + " should not have trailing commas", null);

        int daySpecified[] = new int[] { 0, 0, 0, 0, 0, 0, 0 };  // tracks which days of the week are specified

        String days[] = value.split(",");
        if (days.length != 7)
            throw ServiceException.INVALID_REQUEST(attrName + " must specify all days of a week", null);
        for (int i = 0; i < days.length; ++i) {
            if (days[i].endsWith(":"))
                throw ServiceException.INVALID_REQUEST(
                        attrName + " day section \"" + days[i] + "\" should not have trailing colons", null);
            String parts[] = days[i].split(":");
            if (parts.length != 4)
                throw ServiceException.INVALID_REQUEST(
                        attrName + " day section \"" + days[i] + "\" must have 4 colon-separated parts", null);

            // First part is day number, 1 (Sunday) to 7 (Saturday).
            int dayNum = -1;
            try {
                dayNum = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {}
            if (dayNum < 1 || dayNum > 7)
                throw ServiceException.INVALID_REQUEST(
                        attrName + " day section \"" + days[i] + "\" has invalid day number (must be 1 to 7)", null);

            // Don't allow specifying the same day twice.
            if (daySpecified[dayNum-1] != 0)
                throw ServiceException.INVALID_REQUEST(
                        attrName + " must not specify the same day more than once; found repeated day " + dayNum, null);
            daySpecified[dayNum-1] = 1;

            // Second part is a flag indicating if the working hours for the day are in effect ("Y") or not ("N").
            if (parts[1].length() != 1)
                throw ServiceException.INVALID_REQUEST(
                        attrName + " day section \"" + days[i] + "\" has invalid on/off flag (must be Y or N)", null);
            char flag = parts[1].charAt(0);
            if (flag != 'Y' && flag != 'N')
                throw ServiceException.INVALID_REQUEST(
                        attrName + " day section \"" + days[i] + "\" has invalid on/off flag (must be Y or N)", null);

            // Third part is the start time of the working hours.  Format is HHMM with 24-hour hour.  Range is 0000 to 2359.
            HourMinute startTime = parseHourMinute(attrName, parts[2], false, days[i]);

            // Fourth part is the end time of the working hours.  Range is 0000 to 2400.
            HourMinute endTime = parseHourMinute(attrName, parts[3], true, days[i]);

            // End time cannot be earlier than start time.
            if (startTime.laterThan(endTime))
                throw ServiceException.INVALID_REQUEST(
                        attrName + " day section \"" + days[i] + "\" has end time earlier than start time", null);
        }
    }

    @Override
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
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
    }

    // timeStr must have the format HHMM.  Hour range is 0-24, and minute range is 0-59.
    // Special value "2400" is allowed for denoting end time of the working hours that coincides with the end of the day.
    private HourMinute parseHourMinute(String attrName, String timeStr, boolean isEndTime, String dayStr)
    throws ServiceException {
        if (timeStr.length() == 4) {
            try {
                int hh = Integer.parseInt(timeStr.substring(0, 2));
                int mm = Integer.parseInt(timeStr.substring(2));
                if ((hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59) || (isEndTime && hh == 24 && mm == 0))
                    return new HourMinute(hh, mm);
            } catch (NumberFormatException e) {}
        }
        throw ServiceException.INVALID_REQUEST(
                attrName + " day section \"" + dayStr + "\" has invalid " +
                (isEndTime ? "end" : "start") + " time \"" + timeStr + "\"", null);
    }
}
