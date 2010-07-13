/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Oct 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.List;
import java.util.Map;

import com.zimbra.common.util.MapUtil;

import com.zimbra.common.stats.Counter;

/**
 * @author schemers
 **/
public class NamedEntryCache<E extends NamedEntry> {
    
    private Map mNameCache;
    private Map mIdCache;
    
    private long mRefreshTTL;
    private Counter mHitRate = new Counter();

    static class CacheEntry<E extends NamedEntry> {
        long mLifetime;
        E mEntry;
        CacheEntry(E entry, long expires) {
            mEntry = entry;
            mLifetime = System.currentTimeMillis() + expires;
        }
        
        boolean isStale() {
            return mLifetime < System.currentTimeMillis();
        }
    }
    
/**
 * @param maxItems
 * @param refreshTTL
 */
    public NamedEntryCache(int maxItems, long refreshTTL) {
        mNameCache = MapUtil.newLruMap(maxItems);
        mIdCache = MapUtil.newLruMap(maxItems);
        mRefreshTTL = refreshTTL;
    }

    public synchronized void clear() {
        mNameCache.clear();
        mIdCache.clear();
    }

    public synchronized void remove(String name, String id) {
        mNameCache.remove(name);
        mIdCache.remove(id);
    }
    
    public synchronized void remove(E entry) {
        if (entry != null) {
            mNameCache.remove(entry.getName());
            mIdCache.remove(entry.getId());
        }
    }
    
    public synchronized void put(E entry) {
        if (entry != null) {
            CacheEntry<E> cacheEntry = new CacheEntry<E>(entry, mRefreshTTL);
            mNameCache.put(entry.getName(), cacheEntry);
            mIdCache.put(entry.getId(), cacheEntry);
        }
    }

    public synchronized void put(List<E> entries, boolean clear) {
        if (entries != null) {
            if (clear) clear();
            for (E e: entries)
                put(e);
        }
    }

    @SuppressWarnings("unchecked")
    private E get(String key, Map cache) {
        CacheEntry<E> ce = (CacheEntry<E>) cache.get(key);
        if (ce != null) {
            if (mRefreshTTL != 0 && ce.isStale()) {
                remove(ce.mEntry);
                mHitRate.increment(0);
                return null;
            } else {
                mHitRate.increment(100);
                return ce.mEntry;
            }
        } else {
            mHitRate.increment(0);
            return null;
        }
    }
    
    public synchronized E getById(String key) {
        return get(key, mIdCache);
    }
    
    public synchronized E getByName(String key) {
        return get(key.toLowerCase(), mNameCache);
    }
    
    public synchronized int getSize() {
        return mIdCache.size();
    }
    
    /**
     * Returns the cache hit rate as a value between 0 and 100.
     */
    public synchronized double getHitRate() {
        return mHitRate.getAverage();
    }
}
