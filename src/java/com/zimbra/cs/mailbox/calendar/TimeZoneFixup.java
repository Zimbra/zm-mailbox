/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;

public class TimeZoneFixup {

    // Timezones that changed in 2007
    //
    // [US/Canada changes]
    // TZID:(GMT-09.00) Alaska
    // TZID:(GMT-08.00) Pacific Time (US & Canada)
    // TZID:(GMT-07.00) Mountain Time (US & Canada)
    // TZID:(GMT-06.00) Central Time (US & Canada)
    // TZID:(GMT-05.00) Eastern Time (US & Canada)
    // TZID:(GMT-04.00) Atlantic Time (Canada)
    //
    // TZID:(GMT-03.00) Brasilia
    // TZID:(GMT+02.00) Cairo
    // TZID:(GMT+02.00) Jerusalem
    // TZID:(GMT+05.30) Sri Jayawardenepura (was +06.00)
    //
    // [Australia]
    // TZID:(GMT+08.00) Perth (No DST in 2006, DST in 2007)
    // 

    private static class TZPolicy {
        private String mTZID;
        private long mStandardOffset = 0;
        private SimpleOnset mStandardOnset;
        private long mDaylightOffset = 0;
        private SimpleOnset mDaylightOnset;

        // from an ICalTimeZone object
        public TZPolicy(ICalTimeZone itz) {
            mTZID = itz.getID();
            mStandardOffset = itz.getStandardOffset();
            mStandardOnset = itz.getStandardOnset();
            mDaylightOffset = itz.getDaylightOffset();
            mDaylightOnset = itz.getDaylightOnset();
        }

        // week number and week day policy
        public TZPolicy(String tzid,
                        long stdOffsetMins, int stdMonth, int stdWeek, int stdDayOfWeek,
                        long dstOffsetMins, int dstMonth, int dstWeek, int dstDayOfWeek) {
            mTZID = tzid;
            mStandardOffset = stdOffsetMins * 60000;
            // hour/minute/second don't matter.  Just use 02:00:00.
            mStandardOnset =
                new SimpleOnset(stdWeek, stdDayOfWeek, stdMonth, 0, 2, 0, 0, true);
            mDaylightOffset = dstOffsetMins * 60000;
            mDaylightOnset =
                new SimpleOnset(dstWeek, dstDayOfWeek, dstMonth, 0, 2, 0, 0, true);
        }

        // day of month policy
        public TZPolicy(String tzid,
                        long stdOffsetMins, int stdMonth, int stdDayOfMonth,
                        long dstOffsetMins, int dstMonth, int dstDayOfMonth) {
            mTZID = tzid;
            mStandardOffset = stdOffsetMins * 60000;
            // hour/minute/second don't matter.  Just use 02:00:00.
            mStandardOnset =
                new SimpleOnset(0, 0, stdMonth, stdDayOfMonth, 2, 0, 0, true);
            mDaylightOffset = dstOffsetMins * 60000;
            mDaylightOnset =
                new SimpleOnset(0, 0, dstMonth, dstDayOfMonth, 2, 0, 0, true);
        }

        // no-DST policy
        public TZPolicy(String tzid, long stdOffsetMins) {
            mTZID = tzid;
            mStandardOffset = mDaylightOffset = stdOffsetMins * 60000;
        }

        public boolean equals(Object otherObj) {
            if (otherObj == null || !(otherObj instanceof TZPolicy))
                return false;
            TZPolicy other = (TZPolicy) otherObj;
            return mStandardOffset == other.mStandardOffset &&
                   mDaylightOffset == other.mDaylightOffset &&
                   sameOnset(mStandardOnset, other.mStandardOnset) &&
                   sameOnset(mDaylightOnset, other.mDaylightOnset);
        }

        // std offset (25 hours * 4 quarter hours = 100 ==> 7 bits)
        // 2 bits for DST offset direction
        // for standard and daylight:
        // month (4 bits), week num (3 bits), week day (3 bits), type 1 (1 bit)
        // month (4 bits), month day (5 bits), padding (1 bit), type 0 (1 bit)
        // ==> 7 + 2 + 2 * 11 = 31 bits
        public int hashCode() {
            int offset = (int) (mStandardOffset / 1000 / 60);  // minutes
            offset += (25 * 60);  // add 25 hours (GMT-12:00 to GMT+13:00 range)
            offset /= 15;  // number of 15-mins
            offset <<= 2;
            int hash = offset << 24;
            int dstDirection = 0;
            if (mDaylightOffset > mStandardOffset)
                dstDirection = 1;
            else if (mDaylightOffset < mStandardOffset)
                dstDirection = 2;
            hash |= dstDirection << 22;
            if (mDaylightOffset != mStandardOffset) {
                if (mStandardOnset != null)
                    hash |= hashOnset(mStandardOnset) << 11;
                if (mDaylightOnset != null)
                    hash |= hashOnset(mDaylightOnset);
            }
            return hash;
        }

        private static int hashOnset(SimpleOnset onset) {
            int hash = onset.getMonth() << 7;
            int week = onset.getWeek();
            if (week != 0) {
                if (week == -1)
                    week = 5;
                week <<= 4;
                hash |= week;
                hash |= onset.getDayOfWeek() << 1;
                hash |= 1;  // type 1
            } else {
                hash |= onset.getDayOfMonth() << 2;
            }
            return hash;
        }

        private static boolean sameOnset(SimpleOnset os1, SimpleOnset os2) {
            if (os1 == null) {
                return os2 == null;
            } else if (os2 == null) {
                return os1 == null;
            }

            if (os1.getMonth() != os2.getMonth())
                return false;

            int week1 = os1.getWeek();
            if (week1 != 0) {
                return week1 == os2.getWeek() && os1.getDayOfWeek() == os2.getDayOfWeek();
            } else {
                return os1.getDayOfMonth() == os2.getDayOfMonth();
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TZID=").append(mTZID);
            sb.append(", stdOffset=").append(mStandardOffset);
            if (mStandardOnset != null)
                sb.append(", stdOnset=[").append(mStandardOnset.toString()).append("]");
            sb.append(", dstOffset=").append(mDaylightOffset);
            if (mDaylightOnset != null)
                sb.append(", dstOnset=[").append(mDaylightOnset.toString()).append("]");
            return sb.toString();
        }

        // Does this timezone use daylight savings time but have identical
        // onset date/rule for standard and daylight times?
        public boolean isBug15241() {
            return mStandardOffset != mDaylightOffset &&
                   mStandardOnset != null &&
                   mDaylightOnset != null &&
                   sameOnset(mStandardOnset, mDaylightOnset);
        }
    }

    private static ICalTimeZone fixTZ(ICalTimeZone oldTZ, Map<TZPolicy, ICalTimeZone> fixmap) {
        TZPolicy oldPolicy = new TZPolicy(oldTZ);
        ICalTimeZone newTZ = fixmap.get(oldPolicy);
        if (newTZ != null) {
            ZimbraLog.calendar.info(
                    "Found replacement timezone: old=" +
                    oldTZ.getID() + ", new=" + newTZ.getID());
            return newTZ.cloneWithNewTZID(oldTZ.getID());
        } else if (oldPolicy.isBug15241()) {
            ZimbraLog.calendar.info(
                "Found DST timezone without onset rules: " +
                oldTZ.getID() + "; converting to non-DST timezone");
            return new ICalTimeZone(oldTZ.getID(), oldTZ.getStandardOffset());
        } else {
            return null;
        }
    }

    // returns the number of timezones fixed up
    private static int fixTZMap(TimeZoneMap tzmap, Map<TZPolicy, ICalTimeZone> fixmap) {
        int numFixed = 0;
        if (tzmap == null) return numFixed;
        List<ICalTimeZone> newTZList = new ArrayList<ICalTimeZone>();
        for (Iterator<ICalTimeZone> iter = tzmap.tzIterator(); iter.hasNext(); ) {
            ICalTimeZone tz = iter.next();
            ICalTimeZone newTZ = fixTZ(tz, fixmap);
            if (newTZ != null) {
                iter.remove();
                newTZList.add(newTZ);
            }
        }
        for (ICalTimeZone newTZ : newTZList) {
            tzmap.add(newTZ);
            numFixed++;
        }
        ICalTimeZone newLocalTZ = fixTZ(tzmap.getLocalTimeZone(), fixmap);
        if (newLocalTZ != null) {
            tzmap.setLocalTimeZone(newLocalTZ);
            numFixed++;
        }
        return numFixed;
    }

    // returns the number of timezones fixed up
    public static int fixCalendarItem(CalendarItem calItem, String country) {
        init();
        Map<TZPolicy, ICalTimeZone> map;
        if (country == null || !country.equalsIgnoreCase("AU"))
            map = sWorldMap;
        else
            map = sAustraliaMap;

        int numFixed = 0;
        TimeZoneMap tzmap = calItem.getTimeZoneMap();
        numFixed += fixTZMap(tzmap, map);
        Invite[] invites = calItem.getInvites();
        for (Invite inv : invites) {
            if (inv != null) {
                TimeZoneMap tzmapInv = inv.getTimeZoneMap();
                numFixed += fixTZMap(tzmapInv, map);
            }
        }
        return numFixed;
    }

    private static boolean sInited = false;
    private static final Object sInitGuard = new Object();
    private static Map<TZPolicy, ICalTimeZone> sWorldMap =
        new HashMap<TZPolicy, ICalTimeZone>();
    private static Map<TZPolicy, ICalTimeZone> sAustraliaMap =
        new HashMap<TZPolicy, ICalTimeZone>();

    private static void init() {
        if (!sInited) {
            synchronized (sInitGuard) {
                if (!sInited) {
                    initMaps();
                    sInited = true;
                }
            }
        }
    }

    private static void initMaps() {
        TZPolicy policy;

        ICalTimeZone tz;

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-09.00) Alaska");
        policy = new TZPolicy(
                "2006 US/Alaska",
                -540, 10, -1, 1,
                -480, 4, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Alaska BYMONTHDAY using 2006 rule",
                -540, 10, 29,
                -480, 4, 2);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Alaska BYMONTHDAY using 2007 rule",
                -540, 11, 5,
                -480, 3, 12);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Alaska BYMONTHDAY using 2006 rule",
                -540, 10, 28,
                -480, 4, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Alaska BYMONTHDAY using 2007 rule",
                -540, 11, 4,
                -480, 3, 11);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "US/Alaska January 1st without rule (bug 15241)",
                -540, 1, 1,
                -480, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-08.00) Pacific Time (US & Canada)");
        policy = new TZPolicy(
                "2006 US/Pacific",
                -480, 10, -1, 1,
                -420, 4, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Pacific BYMONTHDAY using 2006 rule",
                -480, 10, 29,
                -420, 4, 2);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Pacific BYMONTHDAY using 2007 rule",
                -480, 11, 5,
                -420, 3, 12);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Pacific BYMONTHDAY using 2006 rule",
                -480, 10, 28,
                -420, 4, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Pacific BYMONTHDAY using 2007 rule",
                -480, 11, 4,
                -420, 3, 11);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "US/Pacific January 1st without rule (bug 15241)",
                -480, 1, 1,
                -420, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-07.00) Mountain Time (US & Canada)");
        policy = new TZPolicy(
                "2006 US/Mountain",
                -420, 10, -1, 1,
                -360, 4, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Mountain BYMONTHDAY using 2006 rule",
                -420, 10, 29,
                -360, 4, 2);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Mountain BYMONTHDAY using 2007 rule",
                -420, 11, 5,
                -360, 3, 12);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Mountain BYMONTHDAY using 2006 rule",
                -420, 10, 28,
                -360, 4, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Mountain BYMONTHDAY using 2007 rule",
                -420, 11, 4,
                -360, 3, 11);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "US/Mountain January 1st without rule (bug 15241)",
                -420, 1, 1,
                -360, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-06.00) Central Time (US & Canada)");
        policy = new TZPolicy(
                "2006 US/Central",
                -360, 10, -1, 1,
                -300, 4, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Central BYMONTHDAY using 2006 rule",
                -360, 10, 29,
                -300, 4, 2);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Central BYMONTHDAY using 2007 rule",
                -360, 11, 5,
                -300, 3, 12);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Central BYMONTHDAY using 2006 rule",
                -360, 10, 28,
                -300, 4, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Central BYMONTHDAY using 2007 rule",
                -360, 11, 4,
                -300, 3, 11);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "US/Central January 1st without rule (bug 15241)",
                -360, 1, 1,
                -300, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-05.00) Eastern Time (US & Canada)");
        policy = new TZPolicy(
                "2006 US/Eastern",
                -300, 10, -1, 1,
                -240, 4, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Eastern BYMONTHDAY using 2006 rule",
                -300, 10, 29,
                -240, 4, 2);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 US/Eastern BYMONTHDAY using 2007 rule",
                -300, 11, 5,
                -240, 3, 12);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Eastern BYMONTHDAY using 2006 rule",
                -300, 10, 28,
                -240, 4, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 US/Eastern BYMONTHDAY using 2007 rule",
                -300, 11, 4,
                -240, 3, 11);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "US/Eastern January 1st without rule (bug 15241)",
                -300, 1, 1,
                -240, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-04.00) Atlantic Time (Canada)");
        policy = new TZPolicy(
                "2006 Canada/Atlantic",
                -240, 10, -1, 1,
                -180, 4, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Canada/Atlantic BYMONTHDAY using 2006 rule",
                -240, 10, 29,
                -180, 4, 2);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Canada/Atlantic BYMONTHDAY using 2007 rule",
                -240, 11, 5,
                -180, 3, 12);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Canada/Atlantic BYMONTHDAY using 2006 rule",
                -240, 10, 28,
                -180, 4, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Canada/Atlantic BYMONTHDAY using 2007 rule",
                -240, 11, 4,
                -180, 3, 11);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "Canada/Atlantic January 1st without rule (bug 15241)",
                -240, 1, 1,
                -180, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-03.30) Newfoundland");
        policy = new TZPolicy(
                "2006 Canada/Newfoundland",
                -210, 10, -1, 1,
                -150, 4, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Canada/Newfoundland BYMONTHDAY using 2006 rule",
                -210, 10, 29,
                -150, 4, 2);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Canada/Newfoundland BYMONTHDAY using 2007 rule",
                -210, 11, 5,
                -150, 3, 12);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Canada/Newfoundland BYMONTHDAY using 2006 rule",
                -210, 10, 28,
                -150, 4, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Canada/Newfoundland BYMONTHDAY using 2007 rule",
                -210, 11, 4,
                -150, 3, 11);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "Canada/Newfoundland January 1st without rule (bug 15241)",
                -210, 1, 1,
                -150, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT-03.00) Brasilia");
        policy = new TZPolicy(
                "2006 Brasilia",
                -180, 2, 2, 1,
                -120, 10, 3, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Brasilia BYMONTHDAY using 2006 rule",
                -180, 2, 12,
                -120, 10, 15);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Brasilia BYMONTHDAY using 2007 rule",
                -180, 2, 26,
                -120, 11, 5);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Brasilia BYMONTHDAY using 2006 rule",
                -180, 2, 11,
                -120, 10, 21);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Brasilia BYMONTHDAY using 2007 rule",
                -180, 2, 25,
                -120, 11, 4);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT+02.00) Cairo");
        policy = new TZPolicy(
                "2006 Cairo",
                120, 9, -1, 4,
                180, 5, 1, 6);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Cairo BYMONTHDAY using 2006 rule",
                120, 9, 27,
                180, 5, 5);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Cairo BYMONTHDAY using 2007 rule",
                120, 9, 28,
                180, 4, 27);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Cairo BYMONTHDAY using 2006 rule",
                120, 9, 26,
                180, 5, 4);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Cairo BYMONTHDAY using 2007 rule",
                120, 9, 27,
                180, 4, 26);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT+02.00) Jerusalem");
        policy = new TZPolicy("2006 Jerusalem", 120);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Jerusalem BYMONTHDAY using 2007 rule",
                120, 9, 17,
                180, 3, 31);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Jerusalem BYMONTHDAY using 2007 rule",
                120, 9, 16,
                180, 3, 30);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT+03.30) Tehran");
        policy = new TZPolicy(
                "2006 Tehran",
                210, 9, 4, 3,
                270, 3, 1, 1);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Tehran BYMONTHDAY using 2006 rule",
                210, 9, 26,
                270, 3, 5);
        sWorldMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Tehran BYMONTHDAY using 2006 rule",
                210, 9, 25,
                270, 3, 4);
        sWorldMap.put(policy, tz);

        // fixup for bug 15241 for European countries
        tz = WellKnownTimeZones.getTimeZoneById("(GMT) Greenwich Mean Time - Dublin / Edinburgh / Lisbon / London");
        policy = new TZPolicy(
                "Europe GMT+0 January 1st without rule (bug 15241)",
                0, 1, 1,
                60, 1, 1);
        sWorldMap.put(policy, tz);
        tz = WellKnownTimeZones.getTimeZoneById("(GMT+01.00) Amsterdam / Berlin / Bern / Rome / Stockholm / Vienna");
        policy = new TZPolicy(
                "Europe GMT+1 January 1st without rule (bug 15241)",
                60, 1, 1,
                120, 1, 1);
        sWorldMap.put(policy, tz);
        tz = WellKnownTimeZones.getTimeZoneById("(GMT+02.00) Helsinki / Kyiv / Riga / Sofia / Tallinn / Vilnius");
        policy = new TZPolicy(
                "Europe GMT+2 January 1st without rule (bug 15241)",
                120, 1, 1,
                180, 1, 1);
        sWorldMap.put(policy, tz);
        tz = WellKnownTimeZones.getTimeZoneById("(GMT+03.00) Moscow / St. Petersburg / Volgograd");
        policy = new TZPolicy(
                "Europe GMT+3 January 1st without rule (bug 15241)",
                180, 1, 1,
                240, 1, 1);
        sWorldMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT+08.00) Perth");
        policy = new TZPolicy("2006 Australia/Perth", 480);
        sAustraliaMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Australia/Perth BYMONTHDAY using 2007 rule, rev 1",
                480, 3, 26,
                540, 12, 3);
        sAustraliaMap.put(policy, tz);
        // 2007 rule rev 1 from Microsoft had daylight onset of 1st Sunday of December.
        // They corrected it to last Sunday of October in patch rev 2 released on 02/08/2007.
        policy = new TZPolicy(
                "2007 Australia/Perth BYMONTHDAY using 2007 rule, rev 1",
                480, 3, 25,
                540, 12, 2);
        sAustraliaMap.put(policy, tz);
        policy = new TZPolicy(
                "2006 Australia/Perth BYMONTHDAY using 2007 rule, rev 2",
                480, 3, 26,
                540, 10, 29);
        sAustraliaMap.put(policy, tz);
        policy = new TZPolicy(
                "2007 Australia/Perth BYMONTHDAY using 2007 rule, rev 2",
                480, 3, 25,
                540, 10, 28);
        sAustraliaMap.put(policy, tz);
        policy = new TZPolicy(
                "Australia/Perth January 1st without rule (bug 15241)",
                480, 1, 1,
                540, 1, 1);
        sAustraliaMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT+09.30) Adelaide");
        policy = new TZPolicy(
                "Australia/Adelaide January 1st without rule (bug 15241)",
                570, 1, 1,
                630, 1, 1);
        sAustraliaMap.put(policy, tz);

        tz = WellKnownTimeZones.getTimeZoneById("(GMT+10.00) Canberra / Melbourne / Sydney");
        policy = new TZPolicy(
                "Australia/Sydney January 1st without rule (bug 15241)",
                600, 1, 1,
                660, 1, 1);
        sAustraliaMap.put(policy, tz);
    }
}
