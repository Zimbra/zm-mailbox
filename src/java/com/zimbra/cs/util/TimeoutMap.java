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

package com.zimbra.cs.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Simple implementation of a <code>HashMap</code> whose elements
 * time out after the specified time period.
 * 
 * @author bburtin
 *
 */
public class TimeoutMap implements Map {
    
    private long mTimeoutMillis;
    private Map mMap = new HashMap();
    private Map /* <Long, Object> */ mTimestamps = new TreeMap();
    private long mLastTimestamp = 0;

    public TimeoutMap(long timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeout value: " + timeoutMillis);
        }
        mTimeoutMillis = timeoutMillis;
    }
    
    public int size() {
        prune();
        return mMap.size();
    }

    public boolean isEmpty() {
        prune();
        return mMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        prune();
        return mMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        prune();
        return mMap.containsValue(value);
    }

    public Object get(Object key) {
        prune();
        return mMap.get(key);
    }

    public Object put(Object key, Object value) {
        prune();
        mTimestamps.put(getTimestamp(), key);
        return mMap.put(key, value);
    }

    public Object remove(Object key) {
        prune();
        return mMap.remove(key);
    }

    public void putAll(Map t) {
        prune();
        Iterator i = t.keySet().iterator();
        while (i.hasNext()) {
            mTimestamps.put(getTimestamp(), i.next());
        }
        mMap.putAll(t);
    }

    public void clear() {
        mTimestamps.clear();
        mMap.clear();
    }

    public Set keySet() {
        prune();
        return mMap.keySet();
    }

    public Collection values() {
        prune();
        return mMap.values();
    }

    public Set entrySet() {
        prune();
        return mMap.entrySet();
    }
    
    /**
     * Removes all entries that have timed out.
     */
    private void prune() {
        long now = System.currentTimeMillis();
        Iterator i = mTimestamps.keySet().iterator();
        while (i.hasNext()) {
            Long timestamp = (Long) i.next();
            if (now - timestamp.longValue() > mTimeoutMillis) {
                Object key = mTimestamps.get(timestamp);
                mMap.remove(key);
                i.remove();
            } else {
                // The timestamp map is sorted, so we know all other timestamps
                // are later
                return;
            }
        }
    }
    
    /**
     * Returns the current system timestamp, possibly adjusted by a few milliseconds.
     * Used to ensure that the timestamp TreeMap contains unique values.
     */
    private Long getTimestamp() {
        long now = System.currentTimeMillis();
        if (now <= mLastTimestamp) {
            now = mLastTimestamp + 1;
        }
        mLastTimestamp = now;
        return new Long(mLastTimestamp);
    }
}
