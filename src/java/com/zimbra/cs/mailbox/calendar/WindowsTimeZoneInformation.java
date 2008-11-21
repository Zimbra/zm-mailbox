/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;

/**
 * Java representation of Windows TIME_ZONE_INFORMATION structure in Winbase.h
 * @author jhahm
 *
 */
public class WindowsTimeZoneInformation {

    // In our iCal code we don't distinguish between standard name and
    // daylight name.  There is only one name for a time zone.  So we
    // don't map Windows fields StandardName[32] and DaylightName[32].
    private String mName;
    private String mStandardName;  // Windows field StandardName[32]
    private String mDaylightName;  // Windows field DaylightName[32]

    private int mBiasMins;
    private WindowsSystemTime mStandardDate;
    private int mStandardBiasMins;  // standard offset = mBias + mStandardBias

    private WindowsSystemTime mDaylightDate;
    private int mDaylightBiasMins;  // daylight offset = mBias + mDaylightBias

    private int mStandardOffsetMillis;
    private int mDaylightOffsetMillis;

    public WindowsTimeZoneInformation(String name,
                                      int biasMins,
                                      WindowsSystemTime standardDate,
                                      int standardBiasMins,
                                      String standardName,
                                      WindowsSystemTime daylightDate,
                                      int daylightBiasMins,
                                      String daylightName) {
        mName             = name;
        mBiasMins         = biasMins;
        mStandardDate     = standardDate;
        mStandardBiasMins = standardBiasMins;
        mDaylightDate     = daylightDate;
        mDaylightBiasMins = daylightBiasMins;

        mStandardOffsetMillis = -1 * (mBiasMins + mStandardBiasMins) * 60 * 1000;
        mDaylightOffsetMillis = -1 * (mBiasMins + mDaylightBiasMins) * 60 * 1000;

        mStandardName = standardName;
        mDaylightName = daylightName;
    }

    /**
     * Windows TIME_ZONE_INFORMATION structure has separate standard and
     * daylight time zone names, both limited to 31 characters.  We don't
     * do that in this Java object.  Instead we provide a single time zone
     * name that is independent of DST.  Also note the name is not limited
     * to 31 characters.  In fact, most names are longer than that.
     * @return
     */
    public String getName() {
        return mName;
    }

    public long getBiasMins()                  { return mBiasMins; }
    public WindowsSystemTime getStandardDate() { return mStandardDate; }
    public int getStandardBiasMins()           { return mStandardBiasMins; }
    public int getStandardOffset()             { return mStandardOffsetMillis; }
    public String getStandardName()            { return mStandardName; }
    public WindowsSystemTime getDaylightDate() { return mDaylightDate; }
    public int getDaylightBiasMins()           { return mDaylightBiasMins; }
    public int getDaylightOffset()             { return mDaylightOffsetMillis; }
    public String getDaylightName()            { return mDaylightName; }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TIME_ZONE_INFORMATION {\n");
        sb.append("    Zone Name    = ").append(mName).append("\n");
        sb.append("    Bias         = ").append(mBiasMins).append("\n");
        sb.append("    StandardDate = ").append(mStandardDate).append("\n");
        sb.append("    StandardBias = ").append(mStandardBiasMins).append("\n");
        sb.append("    StandardName = ").append(mStandardName).append("\n");
        sb.append("    DaylightDate = ").append(mDaylightDate).append("\n");
        sb.append("    DaylightBias = ").append(mDaylightBiasMins).append("\n");
        sb.append("    DaylightName = ").append(mDaylightName).append("\n");
        sb.append("}");
        return sb.toString();
    }

    public ICalTimeZone toICal() {
        SimpleOnset standardOnset = null;
        if (mStandardDate != null)
            standardOnset = mStandardDate.toSimpleOnset();
        SimpleOnset daylightOnset = null;
        if (mDaylightDate != null)
            daylightOnset = mDaylightDate.toSimpleOnset();

        return ICalTimeZone.lookup(
                mName,
                mStandardOffsetMillis,
                standardOnset,
                mStandardName,
                mDaylightOffsetMillis,
                daylightOnset,
                mDaylightName);
    }

    public static WindowsTimeZoneInformation fromICal(ICalTimeZone icalTz) {
        WindowsSystemTime standardDate =
            WindowsSystemTime.fromSimpleOnset(icalTz.getStandardOnset());
        WindowsSystemTime daylightDate =
            WindowsSystemTime.fromSimpleOnset(icalTz.getDaylightOnset());
        // Notice Windows and iCalendar use opposite signs
        // for time zone offset/bias values.  iCal offset is
        // in milliseconds, while Windows bias values are in minutes.
        int bias = -1 * icalTz.getStandardOffset();
        int daylightBias = -1 * icalTz.getDaylightOffset() - bias;
        return new WindowsTimeZoneInformation(
                icalTz.getID(), bias / 60 / 1000,
                standardDate, 0, icalTz.getStandardTzname(),
                daylightDate, daylightBias / 60 / 1000, icalTz.getDaylightTzname());
    }
}
