/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.common.stats.Counter;
import com.zimbra.cs.account.Provisioning.DomainBy;

/**
 * @author schemers
 **/
public class DomainCache {
    
    private LRUMap mNameCache;
    private LRUMap mIdCache;
    private LRUMap mVirtualHostnameCache;
    private LRUMap mKrb5RealmCache;
    
    private long mRefreshTTL;
    private Counter mHitRate = new Counter();
    
    /*
     * for caching non-existing domains so we don't repeatedly search LDAP for domains that do not 
     * exist in Zimbra LDAP.
     * 
     * entries in the NonExistingCache has the same TTS/max as this DomainCache.
     */
    private NonExistingCache mNonExistingCache;

    static class CacheEntry {
        long mLifetime;
        Domain mEntry;
        CacheEntry(Domain entry, long expires) {
            mEntry = entry;
            mLifetime = System.currentTimeMillis() + expires;
        }
        
        boolean isStale() {
            return mLifetime < System.currentTimeMillis();
        }
    }
    
    public static class NonExistingDomain extends Domain {
        private NonExistingDomain() {
            super(null, null, null, null, null);
        }
    }
    

    class NonExistingCache {
        private LRUMap mNENameCache;
        private LRUMap mNEIdCache;
        private LRUMap mNEVirtualHostnameCache;
        private LRUMap mNEKrb5RealmCache;

        private long mNERefreshTTL;
        
        /*
         * if for any reason we want to disable caching of non-existing entries
         * just set mEnabled to false, as a master switch for emergency fix.
         */
        private boolean mEnabled = true;
        
        private NonExistingCache(int maxItems, long refreshTTL) {
            mNENameCache = new LRUMap(maxItems);
            mNEIdCache = new LRUMap(maxItems);
            mNEVirtualHostnameCache = new LRUMap(maxItems);  
            mNEKrb5RealmCache = new LRUMap(maxItems);   
            mNERefreshTTL = refreshTTL;
        }
        
        private void put(DomainBy domainBy, String key) {
            if (!mEnabled)
                return;
            
            NonExistingDomain nonExistingDomain = new NonExistingDomain();
            
            switch (domainBy) {
            case name:
                mNENameCache.put(key, nonExistingDomain);
                break;
            case id:
                mNEIdCache.put(key, nonExistingDomain);
                break;
            case virtualHostname:
                mNEVirtualHostnameCache.put(key, nonExistingDomain);
                break;
            case krb5Realm:
                mNEKrb5RealmCache.put(key, nonExistingDomain);
                break;
            }
        }
        
        private NonExistingDomain get(DomainBy domainBy, String key) {
            if (!mEnabled)
                return null;
            
            switch (domainBy) {
            case name:
                return (NonExistingDomain)mNENameCache.get(key);
            case id:
                return (NonExistingDomain)mNEIdCache.get(key);
            case virtualHostname:
                return (NonExistingDomain)mNEVirtualHostnameCache.get(key);
            case krb5Realm:
                return (NonExistingDomain)mNEKrb5RealmCache.get(key);
            }
            return null;
        }
        
        private void remove(DomainBy domainBy, String key) {
            if (!mEnabled)
                return;
            
            switch (domainBy) {
            case name:
                mNENameCache.remove(key);
                break;
            case id:
                mNEIdCache.remove(key);
                break;
            case virtualHostname:
                mNEVirtualHostnameCache.remove(key);
                break;
            case krb5Realm:
                mNEKrb5RealmCache.remove(key);
                break;
            }
        }
        
        private void clean(DomainBy domainBy, String key, Domain entry) {
            mNENameCache.remove(entry.getName());
            mNEIdCache.remove(entry.getId());
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mNEVirtualHostnameCache.remove(vh.toLowerCase());
            String krb5Realm = entry.getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
            if (krb5Realm != null)
                mNEKrb5RealmCache.remove(krb5Realm);
        }
        
        void clear() {
            mNENameCache.clear();
            mNEIdCache.clear();
            mNEVirtualHostnameCache.clear();
            mNEKrb5RealmCache.clear();
        }
    }
    
    
/**
 * @param maxItems
 * @param refreshTTL
 */
    public DomainCache(int maxItems, long refreshTTL, int maxItemsNonExisting, long refreshTTLNonExisting) {
        mNameCache = new LRUMap(maxItems);
        mIdCache = new LRUMap(maxItems);
        mVirtualHostnameCache = new LRUMap(maxItems);  
        mKrb5RealmCache = new LRUMap(maxItems);   
        mRefreshTTL = refreshTTL;
        
        mNonExistingCache = new NonExistingCache(maxItemsNonExisting, refreshTTLNonExisting);
    }

    public synchronized void clear() {
        mNameCache.clear();
        mIdCache.clear();
        mVirtualHostnameCache.clear();
        mKrb5RealmCache.clear();
        
        mNonExistingCache.clear();
    }

    public synchronized void remove(Domain entry) {
        if (entry != null) {
            mNameCache.remove(entry.getName());
            mIdCache.remove(entry.getId());
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mVirtualHostnameCache.remove(vh.toLowerCase());
            String krb5Realm = entry.getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
            if (krb5Realm != null)
                mKrb5RealmCache.remove(krb5Realm);
        }
    }
    
    public synchronized void removeNonExisting(DomainBy domainBy, String key) {
        mNonExistingCache.remove(domainBy, key);
    }
    
    public synchronized void put(DomainBy domainBy, String key, Domain entry) {
        if (entry != null) {
            // clean it from the non-existing cache first
            mNonExistingCache.clean(domainBy, key, entry);
            
            CacheEntry cacheEntry = new CacheEntry(entry, mRefreshTTL);
            mNameCache.put(entry.getName(), cacheEntry);
            mIdCache.put(entry.getId(), cacheEntry);
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mVirtualHostnameCache.put(vh.toLowerCase(), cacheEntry);            
            String krb5Realm = entry.getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
            if (krb5Realm != null)
                mKrb5RealmCache.put(krb5Realm, cacheEntry);
        } else {
            mNonExistingCache.put(domainBy, key);
        }
    }

    private Domain get(String key, LRUMap cache) {
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
    
    public synchronized Domain getById(String key) {
        Domain d = mNonExistingCache.get(DomainBy.id, key);
        if (d == null)
            return get(key, mIdCache);
        else
            return d;
    }
    
    public synchronized Domain getByName(String key) {
        Domain d = mNonExistingCache.get(DomainBy.name, key);
        if (d == null)
            return get(key.toLowerCase(), mNameCache);
        else
            return d;
    }
    
    public synchronized Domain getByVirtualHostname(String key) {
        Domain d = mNonExistingCache.get(DomainBy.virtualHostname, key);
        if (d == null)
            return get(key.toLowerCase(), mVirtualHostnameCache);
        else
            return d;
    }
    
    public synchronized Domain getByKrb5Realm(String key) {
        Domain d = mNonExistingCache.get(DomainBy.virtualHostname, key);
        if (d == null)
            return get(key.toLowerCase(), mKrb5RealmCache);
        else
            return d;
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
