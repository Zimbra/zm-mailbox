/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 VMware, Inc.
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
package com.zimbra.cs.account.cache;

import java.util.Map;

import com.zimbra.common.util.MapUtil;

import com.zimbra.common.stats.Counter;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class AccountCache implements IAccountCache {
    
    private Map<String, CacheEntry> mNameCache;
    private Map<String, CacheEntry> mIdCache;
    private Map<String, CacheEntry> mAliasCache;
    private Map<String, CacheEntry> mForeignPrincipalCache;
    private Counter mHitRate = new Counter();
    
    private long mRefreshTTL;

    static class CacheEntry {
        long mLifetime;
        Account mEntry;
        CacheEntry(Account entry, long expires) {
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
    public AccountCache(int maxItems, long refreshTTL) {
        mNameCache = MapUtil.newLruMap(maxItems);
        mIdCache = MapUtil.newLruMap(maxItems);
        mAliasCache = MapUtil.newLruMap(maxItems); 
        mForeignPrincipalCache = MapUtil.newLruMap(maxItems);  
        
        mRefreshTTL = refreshTTL;
    }

    @Override
    public synchronized void clear() {
        mNameCache.clear();
        mIdCache.clear();
        mAliasCache.clear();
        mForeignPrincipalCache.clear();
    }

    @Override
    public synchronized void remove(Account entry) {
        if (entry != null) {
            mNameCache.remove(entry.getName());
            mIdCache.remove(entry.getId());
            
            String aliases[] = entry.getMultiAttr(Provisioning.A_zimbraMailAlias);            
            for (String alias : aliases) {
                mAliasCache.remove(alias);
            }
            
            String fps[] = entry.getMultiAttr(Provisioning.A_zimbraForeignPrincipal);            
            for (String fp : fps) {
                mForeignPrincipalCache.remove(fp);
            }
        }
    }
    
    @Override
    public synchronized void put(Account entry) {
        if (entry != null) {
            CacheEntry cacheEntry = new CacheEntry(entry, mRefreshTTL);
            mNameCache.put(entry.getName(), cacheEntry);
            mIdCache.put(entry.getId(), cacheEntry);
            
            String aliases[] = entry.getMultiAttr(Provisioning.A_zimbraMailAlias);            
            for (String alias : aliases) {
                mAliasCache.put(alias, cacheEntry); 
            }
            
            String fps[] = entry.getMultiAttr(Provisioning.A_zimbraForeignPrincipal);            
            for (String fp : fps) {
                mForeignPrincipalCache.put(fp, cacheEntry); 
            }
        }
    }
    
    @Override
    public synchronized void replace(Account entry) {
        remove(entry);
        put(entry);
    }

    private Account get(String key, Map cache) {
        CacheEntry ce = (CacheEntry) cache.get(key);
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
    public synchronized Account getById(String key) {
        return get(key, mIdCache);
    }
    
    @Override
    public synchronized Account getByName(String key) {
        Account acct = get(key.toLowerCase(), mNameCache);
        if (acct != null) {
            return acct;
        } else {
            return get(key.toLowerCase(), mAliasCache);
        }
    }
    
    @Override
    public synchronized Account getByForeignPrincipal(String key) {
        return get(key, mForeignPrincipalCache);
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

