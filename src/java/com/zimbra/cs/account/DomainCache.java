/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.util.MapUtil;

import com.zimbra.common.stats.Counter;
import com.zimbra.cs.account.Provisioning.DomainBy;

/**
 * @author schemers
 **/
public class DomainCache {
    
    private Map mNameCache;
    private Map mIdCache;
    private Map mVirtualHostnameCache;
    private Map mForeignNameCache;
    private Map mKrb5RealmCache;
    
    private long mRefreshTTL;
    private Counter mHitRate = new Counter();
    
    public enum GetFromDomainCacheOption {
        POSITIVE, // only get from positive cache
        NEGATIVE, // only get from negative cache
        BOTH;     // try positive cache first, if not found then try the negative cache
    }
    
    /*
     * for caching non-existing domains so we don't repeatedly search LDAP for domains 
     * that do not exist in Zimbra LDAP.
     * 
     * entries in the NegativeCache has the same TTS/max as this DomainCache.
     */
    private NegativeCache mNegativeCache;

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
    

    class NegativeCache {
        private Map mNegativeNameCache;
        private Map mNegativeIdCache;
        private Map mNegativeVirtualHostnameCache;
        private Map mNegativeForeignNameCache;
        private Map mNegativeKrb5RealmCache;

        private long mNERefreshTTL;
        
        /*
         * if for any reason we want to disable caching of non-existing entries
         * just set mEnabled to false, as a master switch for emergency fix.
         */
        private boolean mEnabled = true;
        
        private NegativeCache(int maxItems, long refreshTTL) {
            mNegativeNameCache = MapUtil.newLruMap(maxItems);
            mNegativeIdCache = MapUtil.newLruMap(maxItems);
            mNegativeVirtualHostnameCache = MapUtil.newLruMap(maxItems);  
            mNegativeForeignNameCache = MapUtil.newLruMap(maxItems);  
            mNegativeKrb5RealmCache = MapUtil.newLruMap(maxItems);   
            mNERefreshTTL = refreshTTL;
        }
        
        private void put(DomainBy domainBy, String key) {
            if (!mEnabled)
                return;
            
            NonExistingDomain nonExistingDomain = new NonExistingDomain();
            
            switch (domainBy) {
            case name:
                mNegativeNameCache.put(key, nonExistingDomain);
                break;
            case id:
                mNegativeIdCache.put(key, nonExistingDomain);
                break;
            case virtualHostname:
                mNegativeVirtualHostnameCache.put(key, nonExistingDomain);
                break;
            case foreignName:
                mNegativeForeignNameCache.put(key, nonExistingDomain);
                break;
            case krb5Realm:
                mNegativeKrb5RealmCache.put(key, nonExistingDomain);
                break;
            }
        }
        
        private NonExistingDomain get(DomainBy domainBy, String key) {
            if (!mEnabled)
                return null;
            
            switch (domainBy) {
            case name:
                return (NonExistingDomain)mNegativeNameCache.get(key);
            case id:
                return (NonExistingDomain)mNegativeIdCache.get(key);
            case virtualHostname:
                return (NonExistingDomain)mNegativeVirtualHostnameCache.get(key);
            case foreignName:
                return (NonExistingDomain)mNegativeForeignNameCache.get(key);
            case krb5Realm:
                return (NonExistingDomain)mNegativeKrb5RealmCache.get(key);
            }
            return null;
        }
        
        private void remove(DomainBy domainBy, String key) {
            if (!mEnabled)
                return;
            
            switch (domainBy) {
            case name:
                mNegativeNameCache.remove(key);
                break;
            case id:
                mNegativeIdCache.remove(key);
                break;
            case virtualHostname:
                mNegativeVirtualHostnameCache.remove(key);
                break;
            case foreignName:
                mNegativeForeignNameCache.remove(key);
                break;
            case krb5Realm:
                mNegativeKrb5RealmCache.remove(key);
                break;
            }
        }
        
        private void clean(DomainBy domainBy, String key, Domain entry) {
            mNegativeNameCache.remove(entry.getName());
            mNegativeIdCache.remove(entry.getId());
            
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mNegativeVirtualHostnameCache.remove(vh.toLowerCase());
            
            String foreignName[] = entry.getMultiAttr(Provisioning.A_zimbraForeignName);            
            for (String fn : foreignName)
                mNegativeForeignNameCache.remove(fn.toLowerCase());
            
            String krb5Realm = entry.getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
            if (krb5Realm != null)
                mNegativeKrb5RealmCache.remove(krb5Realm);
        }
        
        void clear() {
            mNegativeNameCache.clear();
            mNegativeIdCache.clear();
            mNegativeVirtualHostnameCache.clear();
            mNegativeForeignNameCache.clear();
            mNegativeKrb5RealmCache.clear();
        }
    }
    
    
/**
 * @param maxItems
 * @param refreshTTL
 */
    public DomainCache(int maxItems, long refreshTTL, int maxItemsNegative, long refreshTTLNegative) {
        mNameCache = MapUtil.newLruMap(maxItems);
        mIdCache = MapUtil.newLruMap(maxItems);
        mVirtualHostnameCache = MapUtil.newLruMap(maxItems);  
        mForeignNameCache = MapUtil.newLruMap(maxItems); 
        mKrb5RealmCache = MapUtil.newLruMap(maxItems);   
        mRefreshTTL = refreshTTL;
        
        mNegativeCache = new NegativeCache(maxItemsNegative, refreshTTLNegative);
    }

    public synchronized void clear() {
        mNameCache.clear();
        mIdCache.clear();
        mVirtualHostnameCache.clear();
        mForeignNameCache.clear();
        mKrb5RealmCache.clear();
        
        mNegativeCache.clear();
    }

    public synchronized void remove(Domain entry) {
        if (entry != null) {
            mNameCache.remove(entry.getName());
            mIdCache.remove(entry.getId());
            
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mVirtualHostnameCache.remove(vh.toLowerCase());
            
            String foreignName[] = entry.getMultiAttr(Provisioning.A_zimbraForeignName);            
            for (String fn : foreignName)
                mForeignNameCache.remove(fn.toLowerCase());
            
            String krb5Realm = entry.getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
            if (krb5Realm != null)
                mKrb5RealmCache.remove(krb5Realm);
        }
    }
    
    public synchronized void removeFromNegativeCache(DomainBy domainBy, String key) {
        mNegativeCache.remove(domainBy, key);
    }
    
    public synchronized void put(DomainBy domainBy, String key, Domain entry) {
        if (entry != null) {
            // clean it from the non-existing cache first
            mNegativeCache.clean(domainBy, key, entry);
            
            CacheEntry cacheEntry = new CacheEntry(entry, mRefreshTTL);
            mNameCache.put(entry.getName(), cacheEntry);
            mIdCache.put(entry.getId(), cacheEntry);
            
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mVirtualHostnameCache.put(vh.toLowerCase(), cacheEntry);          
            
            String foreignName[] = entry.getMultiAttr(Provisioning.A_zimbraForeignName);            
            for (String fn : foreignName)
                mForeignNameCache.put(fn.toLowerCase(), cacheEntry);  
            
            String krb5Realm = entry.getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
            if (krb5Realm != null)
                mKrb5RealmCache.put(krb5Realm, cacheEntry);
        } else {
            mNegativeCache.put(domainBy, key);
        }
    }

    private Domain get(String key, Map cache) {
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
    
    public synchronized Domain getById(String key, GetFromDomainCacheOption option) {
        
        switch (option) {
        case POSITIVE:
            return get(key, mIdCache);
        case NEGATIVE:
            return mNegativeCache.get(DomainBy.id, key);
        case BOTH:
            Domain d = get(key, mIdCache);
            if (d == null)
                d = mNegativeCache.get(DomainBy.id, key);
            return d;
        default:
            return null;
        }
    }
    
    public synchronized Domain getByName(String key, GetFromDomainCacheOption option) {
        
        switch (option) {
        case POSITIVE:
            return get(key.toLowerCase(), mNameCache);
        case NEGATIVE:
            return mNegativeCache.get(DomainBy.name, key);
        case BOTH:
            Domain d = get(key.toLowerCase(), mNameCache);
            if (d == null)
                d = mNegativeCache.get(DomainBy.name, key);
            return d;
        default:
            return null;
        }
    }
    
    public synchronized Domain getByVirtualHostname(String key, GetFromDomainCacheOption option) {
        
        switch (option) {
        case POSITIVE:
            return get(key.toLowerCase(), mVirtualHostnameCache);
        case NEGATIVE:
            return mNegativeCache.get(DomainBy.virtualHostname, key);
        case BOTH:
            Domain d = get(key.toLowerCase(), mVirtualHostnameCache);
            if (d == null)
                d = mNegativeCache.get(DomainBy.virtualHostname, key);
            return d;
        default:
            return null;
        }
    }
    
    public synchronized Domain getByForeignName(String key, GetFromDomainCacheOption option) {
        
        switch (option) {
        case POSITIVE:
            return get(key.toLowerCase(), mForeignNameCache);
        case NEGATIVE:
            return mNegativeCache.get(DomainBy.foreignName, key);
        case BOTH:
            Domain d = get(key.toLowerCase(), mForeignNameCache);
            if (d == null)
                d = mNegativeCache.get(DomainBy.foreignName, key);
            return d;
        default:
            return null;
        }
    }
    
    public synchronized Domain getByKrb5Realm(String key, GetFromDomainCacheOption option) {
        
        switch (option) {
        case POSITIVE:
            return get(key.toLowerCase(), mKrb5RealmCache);
        case NEGATIVE:
            return mNegativeCache.get(DomainBy.krb5Realm, key);
        case BOTH:
            Domain d = get(key.toLowerCase(), mKrb5RealmCache);
            if (d == null)
                d = mNegativeCache.get(DomainBy.krb5Realm, key);
            return d;
        default:
            return null;
        }
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
