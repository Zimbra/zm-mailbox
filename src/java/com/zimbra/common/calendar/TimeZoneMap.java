/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.calendar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.util.ZimbraLog;

public class TimeZoneMap implements Cloneable {

    static HashMap<ZWeekDay, Integer> sDayWeekDayMap;
    static {
        sDayWeekDayMap = new HashMap<ZWeekDay, Integer>();
        sDayWeekDayMap.put(ZWeekDay.SU, new Integer(java.util.Calendar.SUNDAY));
        sDayWeekDayMap.put(ZWeekDay.MO, new Integer(java.util.Calendar.MONDAY));
        sDayWeekDayMap.put(ZWeekDay.TU, new Integer(java.util.Calendar.TUESDAY));
        sDayWeekDayMap.put(ZWeekDay.WE, new Integer(java.util.Calendar.WEDNESDAY));
        sDayWeekDayMap.put(ZWeekDay.TH, new Integer(java.util.Calendar.THURSDAY));
        sDayWeekDayMap.put(ZWeekDay.FR, new Integer(java.util.Calendar.FRIDAY));
        sDayWeekDayMap.put(ZWeekDay.SA, new Integer(java.util.Calendar.SATURDAY));
    }

    private Map<String /* real TZID */, ICalTimeZone> mTzMap;
    private Map<String /* alias */, String /* real TZID */> mAliasMap;
    private ICalTimeZone mLocalTZ;


    /**
     *
     * @param localTZ local time zone of user account
     */
    public TimeZoneMap(ICalTimeZone localTZ) {
        mTzMap = new HashMap<String, ICalTimeZone>();
        mAliasMap = new HashMap<String, String>();
        mLocalTZ = localTZ;
    }
    
    public Map<String, ICalTimeZone> getMap() {
        return mTzMap;
    }
    
    public Map<String, String> getAliasMap() {
        return mAliasMap;
    }

    public boolean contains(ICalTimeZone tz) {
        if (tz != null)
            return mTzMap.containsKey(tz.getID());
        else
            return false;
    }

    /**
     *
     * @param localTZ local time zone of user account
     */
    public TimeZoneMap(Map<String, ICalTimeZone> z, Map<String, String> a, ICalTimeZone localTZ) {
        mTzMap = z;
        mAliasMap = a;
        mLocalTZ = localTZ;
    }

    public ICalTimeZone getTimeZone(String tzid) {
        tzid = sanitizeTZID(tzid);
        ICalTimeZone tz = mTzMap.get(tzid);
        if (tz == null) {
            tzid = mAliasMap.get(tzid);
            if (tzid != null)
                tz = mTzMap.get(tzid);
        }
        return tz;
    }

    public ICalTimeZone getLocalTimeZone() {
        return mLocalTZ;
    }

    public Iterator<ICalTimeZone> tzIterator() {
        return mTzMap.values().iterator();
    }

    /**
     * Merge the other timezone map into this one
     *
     * @param other
     */
    public void add(TimeZoneMap other) {
        mAliasMap.putAll(other.mAliasMap);
        for (Iterator<Entry<String, ICalTimeZone>> it = other.mTzMap.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, ICalTimeZone> entry = it.next();
            ICalTimeZone zone = entry.getValue();
            if (!mTzMap.containsKey(zone.getID()))
                add(zone);
        }
    }

    public void add(ICalTimeZone tz) {
        String tzid = tz.getID();
        String canonTzid = null;
        if (!DebugConfig.disableCalendarTZMatchByID) {
            canonTzid = TZIDMapper.canonicalize(tzid);
            ICalTimeZone canonTz = WellKnownTimeZones.getTimeZoneById(canonTzid);
            if (canonTz != null) {
                mTzMap.put(canonTzid, canonTz);
                if (!tzid.equals(canonTzid))
                    mAliasMap.put(tzid, canonTzid);
                return;
            }
        }
        if (!DebugConfig.disableCalendarTZMatchByRule) {
            ICalTimeZone ruleMatch = WellKnownTimeZones.getBestMatch(tz);
            if (ruleMatch != null) {
                String realTzid = ruleMatch.getID();
                mTzMap.put(realTzid, ruleMatch);
                if (!tzid.equals(realTzid))
                    mAliasMap.put(tzid, realTzid);
                return;
            }
        }
        mTzMap.put(tzid, tz);
    }

    public static String sanitizeTZID(String tzid) {
        // Workaround for bug in Outlook, which double-quotes TZID parameter
        // value in properties like DTSTART, DTEND, etc. Use unquoted tzId.
        int len = tzid.length();
        if (len >= 2 && tzid.charAt(0) == '"' && tzid.charAt(len - 1) == '"') {
            return tzid.substring(1, len - 1);
        }
        return tzid;
    }

    public ICalTimeZone lookupAndAdd(String tzId) {
        tzId = sanitizeTZID(tzId);
        if (tzId.equals(""))
            return null;

        if (!DebugConfig.disableCalendarTZMatchByID)
            tzId = TZIDMapper.canonicalize(tzId);

        ICalTimeZone zone = getTimeZone(tzId);
        if (zone == null) {
            // Is it a system-defined TZ?
            zone = WellKnownTimeZones.getTimeZoneById(tzId);
            if (zone != null)
                add(zone);
            else {
                ZimbraLog.calendar.warn(
                        "Encountered time zone with no definition: TZID=" +
                        tzId);
            }
        }
        return zone;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("{");
        buf.append("LocalTz = ").append(mLocalTZ).append("; others {");
        for (ICalTimeZone i : mTzMap.values()) {
            buf.append(i).append("; ");
        }
        return buf.append("} }").toString();
    }

    // Reduce the timezone map to contain only the TZIDs passed in.
    public void reduceTo(Set<String> tzids) {
        for (Iterator<Map.Entry<String, String>> iter = mAliasMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, String> entry = iter.next();
            String aliasTzid = entry.getKey();
            if (!tzids.contains(aliasTzid))
                iter.remove();
        }
        for (Iterator<Map.Entry<String, ICalTimeZone>> iter = mTzMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, ICalTimeZone> entry = iter.next();
            String id = entry.getKey();
            if (!tzids.contains(id))
                iter.remove();
        }
    }
    
    public TimeZoneMap clone() {
        TimeZoneMap retMap = new TimeZoneMap(mLocalTZ);
        retMap.mTzMap.putAll(mTzMap);
        retMap.mAliasMap.putAll(mAliasMap);
        return retMap;
    }
}
