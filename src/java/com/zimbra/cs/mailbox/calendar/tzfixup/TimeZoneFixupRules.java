/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.tzfixup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;

public class TimeZoneFixupRules {

    private static enum MatchBy { TZID, OFFSET, RULES, DATES };

    public static class Matcher {
        private MatchBy mMatchBy;
        private String mTZID;
        private long mStandardOffset;
        private long mDaylightOffset;
        private SimpleOnset mStandardOnset;
        private SimpleOnset mDaylightOnset;
        private ICalTimeZone mReplacementTZ;

        // match on TZID
        public Matcher(String tzid, ICalTimeZone replacementTZ) {
            mMatchBy = MatchBy.TZID;
            mTZID = tzid != null ? tzid : "";
            mReplacementTZ = replacementTZ;
        }

        // match a non-DST timezone with GMT offset given in minutes
        public Matcher(long offset, ICalTimeZone replacementTZ) {
            mMatchBy = MatchBy.OFFSET;
            mStandardOffset = mDaylightOffset = offset * 60000;
            mReplacementTZ = replacementTZ;
        }

        // match on DST transition rules; transition time is ignored
        public Matcher(long stdOffsetMins, int stdMonth, int stdWeek, int stdDayOfWeek,
                       long dstOffsetMins, int dstMonth, int dstWeek, int dstDayOfWeek,
                       ICalTimeZone replacementTZ) {
            mMatchBy = MatchBy.RULES;
            mStandardOffset = stdOffsetMins * 60000;
            // hour/minute/second don't matter.  Just use 02:00:00.
            mStandardOnset =
                new SimpleOnset(stdWeek, stdDayOfWeek, stdMonth, 0, 2, 0, 0, true);
            mDaylightOffset = dstOffsetMins * 60000;
            mDaylightOnset =
                new SimpleOnset(dstWeek, dstDayOfWeek, dstMonth, 0, 2, 0, 0, true);
            mReplacementTZ = replacementTZ;
        }

        // match on DST transition dates; transition time is ignored
        public Matcher(long stdOffsetMins, int stdMonth, int stdDayOfMonth,
                       long dstOffsetMins, int dstMonth, int dstDayOfMonth,
                       ICalTimeZone replacementTZ) {
            mMatchBy = MatchBy.DATES;
            mStandardOffset = stdOffsetMins * 60000;
            // hour/minute/second don't matter.  Just use 02:00:00.
            mStandardOnset =
                new SimpleOnset(0, 0, stdMonth, stdDayOfMonth, 2, 0, 0, true);
            mDaylightOffset = dstOffsetMins * 60000;
            mDaylightOnset =
                new SimpleOnset(0, 0, dstMonth, dstDayOfMonth, 2, 0, 0, true);
            mReplacementTZ = replacementTZ;
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

        public ICalTimeZone getReplacementTZ() { return mReplacementTZ; }

        public boolean matches(ICalTimeZone tz) {
            switch (mMatchBy) {
            case TZID:
                return mTZID.equalsIgnoreCase(tz.getID());
            case OFFSET:
                return !tz.useDaylightTime() && tz.getRawOffset() == mStandardOffset;
            case RULES:
            case DATES:
                if (!tz.useDaylightTime())
                    return false;
                if (tz.getStandardOffset() != mStandardOffset || tz.getDaylightOffset() != mDaylightOffset)
                    return false;
                if (!sameOnset(mStandardOnset, tz.getStandardOnset()) || !sameOnset(mDaylightOnset, tz.getDaylightOnset()))
                    return false;
                return true;
            default:
                return false;
            }
        }
    }

    private List<Matcher> mMatchers;

    public TimeZoneFixupRules(Element tzFixupElem) throws ServiceException {
        mMatchers = XmlFixupRules.parseTzFixup(tzFixupElem);
    }

    public TimeZoneFixupRules(Map<String, ICalTimeZone> replacements) {
        mMatchers = new ArrayList<Matcher>();
        for (Map.Entry<String, ICalTimeZone> entry : replacements.entrySet()) {
            String oldTZID = entry.getKey();
            ICalTimeZone replacementTZ = entry.getValue();
            Matcher m = new Matcher(oldTZID, replacementTZ);
            mMatchers.add(m);
        }
    }

    /**
     * Replace the definition of a timezone if it matches any of the rules.  Replacement timezone
     * is returned, or null if no rule matched.  On match, an entry is added to the "replaced" map
     * linking old timezone ID to replacement timezone.
     * @param oldTZ
     * @param replacementMap
     * @return
     */
    private ICalTimeZone fixTZ(ICalTimeZone oldTZ, Map<String, ICalTimeZone> replaced) {
        for (Matcher matcher : mMatchers) {
            if (matcher.matches(oldTZ)) {
                ICalTimeZone newTZ = matcher.getReplacementTZ();
                String oldID = oldTZ.getID();
                ZimbraLog.calendar.info(
                        "Found replacement timezone: old=" + oldID + ", new=" + newTZ.getID());
                replaced.put(oldID, newTZ);
                return newTZ.cloneWithNewTZID(oldID);
            }
        }
        return null;
    }

    // returns the number of timezones fixed up
    private int fixTZMap(TimeZoneMap tzmap, Map<String, ICalTimeZone> replaced) {
        int numFixed = 0;
        if (tzmap == null) return 0;
        List<ICalTimeZone> newTZList = new ArrayList<ICalTimeZone>();
        for (Iterator<ICalTimeZone> iter = tzmap.tzIterator(); iter.hasNext(); ) {
            ICalTimeZone tz = iter.next();
            ICalTimeZone newTZ = fixTZ(tz, replaced);
            if (newTZ != null) {
                iter.remove();
                newTZList.add(newTZ);
            }
        }
        for (ICalTimeZone newTZ : newTZList) {
            tzmap.add(newTZ);
            numFixed++;
        }
        ICalTimeZone newLocalTZ = fixTZ(tzmap.getLocalTimeZone(), replaced);
        if (newLocalTZ != null) {
            tzmap.setLocalTimeZone(newLocalTZ);
            numFixed++;
        }
        return numFixed;
    }

    // returns the number of timezones fixed up
    public int fixCalendarItem(CalendarItem calItem, Map<String, ICalTimeZone> replaced) {
        int numFixed = 0;
        TimeZoneMap tzmap = calItem.getTimeZoneMap();
        numFixed += fixTZMap(tzmap, replaced);
        Invite[] invites = calItem.getInvites();
        for (Invite inv : invites) {
            if (inv != null) {
                TimeZoneMap tzmapInv = inv.getTimeZoneMap();
                numFixed += fixTZMap(tzmapInv, replaced);
            }
        }
        return numFixed;
    }
}
