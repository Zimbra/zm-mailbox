/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

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
public class TimeoutMap<K, V> implements Map<K, V> {
    
    private long timeoutMillis;
    private Map<K, V> map = new HashMap<K, V>();
    private Map<Long, K> timestamps = new TreeMap<Long, K>();
    private long lastTimestamp = 0;

    public TimeoutMap(long timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeout value: " + timeoutMillis);
        }
        this.timeoutMillis = timeoutMillis;
    }
    
    public int size() {
        prune();
        return map.size();
    }

    public boolean isEmpty() {
        prune();
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        prune();
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        prune();
        return map.containsValue(value);
    }

    public V get(Object key) {
        prune();
        return map.get(key);
    }

    public V put(K key, V value) {
        prune();
        timestamps.put(getTimestamp(), key);
        return map.put(key, value);
    }

    public V remove(Object key) {
        prune();
        return map.remove(key);
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        prune();
        for (K key : t.keySet())
            timestamps.put(getTimestamp(), key);
        map.putAll(t);
    }

    public void clear() {
        timestamps.clear();
        map.clear();
    }

    public Set<K> keySet() {
        prune();
        return map.keySet();
    }

    public Collection<V> values() {
        prune();
        return map.values();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        prune();
        return map.entrySet();
    }
    
    /**
     * Removes all entries that have timed out.
     */
    private void prune() {
        long now = System.currentTimeMillis();
        Iterator<Long> i = timestamps.keySet().iterator();
        while (i.hasNext()) {
            Long timestamp = i.next();
            if (now - timestamp > timeoutMillis) {
                map.remove(timestamps.get(timestamp));
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
        if (now <= lastTimestamp) {
            now = lastTimestamp + 1;
        }
        lastTimestamp = now;
        return lastTimestamp;
    }

    public void setTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}
