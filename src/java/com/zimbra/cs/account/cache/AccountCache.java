/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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

import com.zimbra.common.stats.Counter;
import com.zimbra.common.stats.HitRateCounter;
import com.zimbra.common.util.MapUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapCache;

public class AccountCache implements IAccountCache {

    private final Map<String, CacheEntry> mNameCache;
    private final Map<String, CacheEntry> mIdCache;
    private final Map<String, CacheEntry> mAliasCache;
    private final Map<String, CacheEntry> mForeignPrincipalCache;
    private final Counter mHitRate = new HitRateCounter();
    private final FreshnessChecker freshnessChecker;

    private final long mRefreshTTL;

    static class CacheEntry {
        long mLifetime;
        public long lastFreshCheckTime;
        Account mEntry;
        CacheEntry(Account entry, long expires) {
            mEntry = entry;
            lastFreshCheckTime = System.currentTimeMillis();
            mLifetime = lastFreshCheckTime + expires;
        }

        boolean isStale() {
            return mLifetime < System.currentTimeMillis();
        }
    }

/**
 * @param maxItems
 * @param refreshTTL
 */
    public AccountCache(int maxItems, long refreshTTL, FreshnessChecker freshnessChecker) {
        mNameCache = MapUtil.newLruMap(maxItems);
        mIdCache = MapUtil.newLruMap(maxItems);
        mAliasCache = MapUtil.newLruMap(maxItems);
        mForeignPrincipalCache = MapUtil.newLruMap(maxItems);

        mRefreshTTL = refreshTTL;
        this.freshnessChecker = freshnessChecker;
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
            LdapCache.validateCacheEntry(entry);
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

    private Account get(String key, Map<String, CacheEntry> cache) {
        CacheEntry ce = cache.get(key);
        if (ce != null) {
            if ((mRefreshTTL != 0 && ce.isStale()) || staleByFreshness(ce)) {
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

    private boolean staleByFreshness(CacheEntry ce) {
        if (freshnessChecker == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < (ce.lastFreshCheckTime + LdapCache.ldapCacheFreshnessCheckLimitMs())) {
            return false; // Avoid checking too often
        }
        boolean stale = freshnessChecker.isStale(ce.mEntry);
        ce.lastFreshCheckTime = now;
        return stale;
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

