/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Metadata;

public class TimeZoneMap {

    static HashMap<ZRecur.ZWeekDay, Integer> sDayWeekDayMap;
    static {
        sDayWeekDayMap = new HashMap<ZRecur.ZWeekDay, Integer>();
        sDayWeekDayMap.put(ZRecur.ZWeekDay.SU, new Integer(java.util.Calendar.SUNDAY));
        sDayWeekDayMap.put(ZRecur.ZWeekDay.MO, new Integer(java.util.Calendar.MONDAY));
        sDayWeekDayMap.put(ZRecur.ZWeekDay.TU, new Integer(java.util.Calendar.TUESDAY));
        sDayWeekDayMap.put(ZRecur.ZWeekDay.WE, new Integer(java.util.Calendar.WEDNESDAY));
        sDayWeekDayMap.put(ZRecur.ZWeekDay.TH, new Integer(java.util.Calendar.THURSDAY));
        sDayWeekDayMap.put(ZRecur.ZWeekDay.FR, new Integer(java.util.Calendar.FRIDAY));
        sDayWeekDayMap.put(ZRecur.ZWeekDay.SA, new Integer(java.util.Calendar.SATURDAY));
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
    
    
    public boolean contains(ICalTimeZone tz) {
        return mTzMap.containsKey(tz.getID());
    }

    /**
     * 
     * @param localTZ local time zone of user account
     */
    private TimeZoneMap(Map<String, ICalTimeZone> z, Map<String, String> a, ICalTimeZone localTZ) {
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
    
    public Metadata encodeAsMetadata() {
        Metadata meta = new Metadata();
        Map<String /* real TZID */, Integer /* index */> tzIndex = new HashMap<String, Integer>();
        int nextIndex = 0;
        for (Iterator<Entry<String, ICalTimeZone>> iter = mTzMap.entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, ICalTimeZone> entry = iter.next();
            String tzid = entry.getKey();
            if (tzid == null || tzid.length() < 1)    // ignore null/empty TZIDs (bug 25183)
                continue;
            ICalTimeZone zone = entry.getValue();
            String realTzid = zone.getID();
            if (!tzIndex.containsKey(realTzid)) {
                meta.put("#" + nextIndex, zone.encodeAsMetadata());
                tzIndex.put(realTzid, nextIndex);
                ++nextIndex;
            }
        }
        for (Iterator<Entry<String, String>> iter = mAliasMap.entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, String> entry = iter.next();
            String alias = entry.getKey();
            String realTzid = entry.getValue();
            if (tzIndex.containsKey(realTzid)) {
                int index = tzIndex.get(realTzid);
                meta.put(alias, index);
            }
        }
        return meta;
    }

    /**
     * 
     * @param meta
     * @param localTZ local time zone of user account
     * @return
     * @throws ServiceException
     */
    public static TimeZoneMap decodeFromMetadata(Metadata meta, ICalTimeZone localTZ) throws ServiceException {
        Map map = meta.asMap();
        ICalTimeZone[] tzlist = new ICalTimeZone[map.size()];
        // first time, find the tz's
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Entry) it.next();
            String key = (String)entry.getKey();
            if (key != null && key.length() > 0) {  // ignore null/empty TZIDs (bug 25183)
                if (key.charAt(0) == '#') {
                    int idx = Integer.parseInt(key.substring(1));
                    ICalTimeZone tz = ICalTimeZone.decodeFromMetadata((Metadata) entry.getValue());
                    tzlist[idx] = tz;
                }
            }
        }

        Map<String, ICalTimeZone> tzmap = new HashMap<String, ICalTimeZone>();
        for (ICalTimeZone tz : tzlist) {
            if (tz != null)
                tzmap.put(tz.getID(), tz);
        }
        Map<String, String> aliasMap = new HashMap<String, String>();
        // second time, build the real map
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Entry) it.next();
            String tzid = (String) entry.getKey();
            if (tzid != null && tzid.length() > 0) {  // ignore null/empty TZIDs (bug 25183)
                if (tzid.charAt(0) != '#') {
                    int idx = -1;
                    try {
                        idx = Integer.parseInt(entry.getValue().toString());
                    } catch (NumberFormatException e) {}
                    if (idx >= 0 && idx < tzlist.length) {
                        ICalTimeZone tz = tzlist[idx];
                        if (tz != null) {
                            String realId = tz.getID();
                            if (!realId.equals(tzid))
                                aliasMap.put(tzid, realId);
                        }
                    }
                }
            }
        }
        
        return new TimeZoneMap(tzmap, aliasMap, localTZ);
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

    public String sanitizeTZID(String tzid) {
        // Workaround for bug in Outlook, which double-quotes TZID parameter
        // value in properties like DTSTART, DTEND, etc. Use unquoted tzId.
        int len = tzid.length();
        if (len >= 2 && tzid.charAt(0) == '"' && tzid.charAt(len - 1) == '"') {
            return tzid.substring(1, len - 1);
        }
        return tzid;
    }

    public ICalTimeZone lookupAndAdd(String tzId)
    throws ServiceException {
        tzId = sanitizeTZID(tzId);
        if (tzId.equals(""))
            return null;

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

    public String toString() {
        String s = "{";

        s += "LocalTz = " + mLocalTZ + "; others {";

        for (ICalTimeZone i : mTzMap.values()) {
            s += i +"; ";
        }

        s += "} }";
        return s;
    }
}
