/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Oct 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.cache;

import java.util.List;
import java.util.Map;

import com.zimbra.common.util.MapUtil;
import com.zimbra.common.stats.Counter;
import com.zimbra.common.stats.HitRateCounter;
import com.zimbra.cs.account.NamedEntry;

/**
 * @author schemers
 **/
public class NamedEntryCache<E extends NamedEntry> implements INamedEntryCache<E> {
    
    private Map mNameCache;
    private Map mIdCache;
    
    private long mRefreshTTL;
    private Counter mHitRate = new HitRateCounter();
   

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

    @Override
    public synchronized void clear() {
        mNameCache.clear();
        mIdCache.clear();
    }

    @Override
    public synchronized void remove(String name, String id) {
        mNameCache.remove(name);
        mIdCache.remove(id);
    }
    
    @Override
    public synchronized void remove(E entry) {
        if (entry != null) {
            mNameCache.remove(entry.getName());
            mIdCache.remove(entry.getId());
        }
    }
    
    @Override
    public synchronized void put(E entry) {
        if (entry != null) {
            CacheEntry<E> cacheEntry = new CacheEntry<E>(entry, mRefreshTTL);
            mNameCache.put(entry.getName(), cacheEntry);
            mIdCache.put(entry.getId(), cacheEntry);
        }
    }
    
    @Override
    public synchronized void replace(E entry) {
        remove(entry);
        put(entry);
    }

    @Override
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
    
    @Override
    public synchronized E getById(String key) {
        return get(key, mIdCache);
    }
    
    @Override
    public synchronized E getByName(String key) {
        return get(key.toLowerCase(), mNameCache);
    }
    
    @Override
    public synchronized int getSize() {
        return mIdCache.size();
    }
    
  
    /**
     * Returns the cache hit rate as a value between 0 and 100.
     */
    @Override
    public synchronized double getHitRate() {
        return mHitRate.getAverage();
    }
}
