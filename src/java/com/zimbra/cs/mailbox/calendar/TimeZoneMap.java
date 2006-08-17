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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;

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
    
    
    private Map<String, ICalTimeZone> mTzMap;
    private ICalTimeZone mLocalTZ;
    
    
    /**
     * 
     * @param localTZ local time zone of user account
     */
    public TimeZoneMap(ICalTimeZone localTZ) {
    	mTzMap = new HashMap<String, ICalTimeZone>();
        mLocalTZ = localTZ;
    }
    
    
    public boolean contains(ICalTimeZone tz) {
        return mTzMap.containsKey(tz.getID());
    }

    /**
     * 
     * @param localTZ local time zone of user account
     */
    private TimeZoneMap(Map<String, ICalTimeZone> m, ICalTimeZone localTZ) {
        mTzMap = m;
        mLocalTZ = localTZ;
    }

    /**
     * Returns account's local time zone if requested TZ can't be found.
     * @param tzid
     * @return
     */
    public ICalTimeZone getTimeZone(String tzid) {
        Object tz = mTzMap.get(tzid);
        ICalTimeZone toRet = (ICalTimeZone)tz;
        assert(toRet==null || toRet.getID().equals(tzid));
        return toRet;
    }

    public ICalTimeZone getLocalTimeZone() {
    	return mLocalTZ;
    }
    
    public Iterator tzIterator() {
        return mTzMap.values().iterator();
    }
    
    public Metadata encodeAsMetadata() {
        // don't put TZs in there muliple times
        Metadata meta = new Metadata();
        List<ICalTimeZone> tzList = new ArrayList<ICalTimeZone>();

        for (Iterator it = mTzMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            ICalTimeZone zone = (ICalTimeZone) entry.getValue();

            int idx = tzList.indexOf(zone);
            if (idx == -1) {
                idx = tzList.size();
                tzList.add(zone);
            }
            meta.put(key, idx);
        }

        for (int i = tzList.size() - 1; i >= 0; i--) {
            ICalTimeZone tz = (ICalTimeZone) tzList.get(i);
            meta.put("#" + i, tz.encodeAsMetadata());
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
            if (key.charAt(0) == '#') {
                int idx = Integer.parseInt(key.substring(1));
                ICalTimeZone tz = ICalTimeZone.decodeFromMetadata((Metadata) entry.getValue());
                tzlist[idx] = tz;
            }
        }

        Map<String, ICalTimeZone> tzmap = new HashMap<String, ICalTimeZone>();
        // second time, build the real map
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            if (key.charAt(0) != '#') {
                int idx = Integer.parseInt(entry.getValue().toString());
                if (tzlist[idx] != null) {
                    tzmap.put(key, tzlist[idx]);
                }
            }
        }
        
        return new TimeZoneMap(tzmap, localTZ);
    }
    
    /**
     * Merge the other timezone map into this one
     * 
     * @param other
     */
    public void add(TimeZoneMap other) {
        for (Iterator it = other.mTzMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            if (!mTzMap.containsKey(key)) {
                ICalTimeZone zone = (ICalTimeZone) entry.getValue();
                add(zone);
            }
        }
    }

    public void add(ICalTimeZone tz) {
    	mTzMap.put(tz.getID(), tz);
    }
}
