/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.WellKnownTimeZone;
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
                                      WindowsSystemTime daylightDate,
                                      int daylightBiasMins) {
        mName             = name;
        mBiasMins         = biasMins;
        mStandardDate     = standardDate;
        mStandardBiasMins = standardBiasMins;
        mDaylightDate     = daylightDate;
        mDaylightBiasMins = daylightBiasMins;

        mStandardOffsetMillis = -1 * (mBiasMins + mStandardBiasMins) * 60 * 1000;
        mDaylightOffsetMillis = -1 * (mBiasMins + mDaylightBiasMins) * 60 * 1000;
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
    public WindowsSystemTime getDaylightDate() { return mDaylightDate; }
    public int getDaylightBiasMins()           { return mDaylightBiasMins; }
    public int getDaylightOffset()             { return mDaylightOffsetMillis; }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("TIME_ZONE_INFORMATION {\n");
        sb.append("    Zone Name    = ").append(mName).append("\n");
        sb.append("    Bias         = ").append(mBiasMins).append("\n");
        sb.append("    StandardDate = ").append(mStandardDate).append("\n");
        sb.append("    StandardBias = ").append(mStandardBiasMins).append("\n");
        sb.append("    DaylightDate = ").append(mDaylightDate).append("\n");
        sb.append("    DaylightBias = ").append(mDaylightBiasMins).append("\n");
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

        return new ICalTimeZone(
                mName,
                mStandardOffsetMillis,
                standardOnset,
                mDaylightOffsetMillis,
                daylightOnset);
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
                standardDate, 0,
                daylightDate, daylightBias / 60 / 1000);
    }

    public static void main(String args[]) throws Exception {
        int badConvs = 0;
        List tzList = Provisioning.getInstance().getAllTimeZones();
        for (Iterator iter = tzList.iterator(); iter.hasNext(); ) {
            WellKnownTimeZone wktz = (WellKnownTimeZone) iter.next();
            ICalTimeZone ical1 = wktz.toTimeZone();
            WindowsTimeZoneInformation win = WindowsTimeZoneInformation.fromICal(ical1);
            ICalTimeZone ical2 = win.toICal();

            System.out.println("TIMEZONE: " + wktz.getName());
            System.out.println("--------------------------------------------------");
            System.out.println("iCal original:\n" + ical1);
            System.out.println("    " + ical1.getStandardDtStart() + ", " + ical1.getStandardRule());
            System.out.println("    " + ical1.getDaylightDtStart() + ", " + ical1.getDaylightRule());
            System.out.println("iCal again:\n" + ical2);
            System.out.println("    " + ical2.getStandardDtStart() + ", " + ical2.getStandardRule());
            System.out.println("    " + ical2.getDaylightDtStart() + ", " + ical2.getDaylightRule());
            System.out.println("Windows:\n" + win);
            System.out.println();

            if (!ical2.hasSameRules(ical1)) {
                badConvs++;
                System.out.println("Conversion is BAD.");
            }
        }
        System.out.println("--------------------------------------------------");
        System.out.println("Bad Conversions = " + badConvs);
    }
}
