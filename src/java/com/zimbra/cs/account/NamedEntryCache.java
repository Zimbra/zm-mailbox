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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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

import org.apache.commons.collections.map.LRUMap;

/**
 * @author schemers
 **/
public class NamedEntryCache<E extends NamedEntry> {
    
    private LRUMap mNameCache;
    private LRUMap mIdCache;
    
    private long mRefreshTTL;

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
        mNameCache = new LRUMap(maxItems);
        mIdCache = new LRUMap(maxItems);
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
    private E get(String key, LRUMap cache) {
        CacheEntry<E> ce = (CacheEntry<E>) cache.get(key);
        if (ce != null) {
            if (mRefreshTTL != 0 && ce.isStale()) {
                remove(ce.mEntry);
                return null;
            } else {
                return ce.mEntry;
            }
        } else {
            return null;
        }
    }
    
    public synchronized E getById(String key) {
        return get(key, mIdCache);
    }
    
    public synchronized E getByName(String key) {
        return get(key.toLowerCase(), mNameCache);
    }
}
