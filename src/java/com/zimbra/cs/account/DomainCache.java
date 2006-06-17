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

import org.apache.commons.collections.map.LRUMap;

/**
 * @author schemers
 **/
public class DomainCache {
    
    private LRUMap mNameCache;
    private LRUMap mIdCache;
    private LRUMap mVirtualHostnameCache;
    
    private long mRefreshTTL;

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
    
/**
 * @param maxItems
 * @param refreshTTL
 */
    public DomainCache(int maxItems, long refreshTTL) {
        mNameCache = new LRUMap(maxItems);
        mIdCache = new LRUMap(maxItems);
        mVirtualHostnameCache = new LRUMap(maxItems);        
        mRefreshTTL = refreshTTL;
    }

    public synchronized void clear() {
        mNameCache.clear();
        mIdCache.clear();
        mVirtualHostnameCache.clear();
    }

    public synchronized void remove(Domain entry) {
        if (entry != null) {
            mNameCache.remove(entry.getName());
            mIdCache.remove(entry.getId());
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mVirtualHostnameCache.remove(vh.toLowerCase());
        }
    }
    
    public synchronized void put(Domain entry) {
        if (entry != null) {
            CacheEntry cacheEntry = new CacheEntry(entry, mRefreshTTL);
            mNameCache.put(entry.getName(), cacheEntry);
            mIdCache.put(entry.getId(), cacheEntry);
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);            
            for (String vh : vhost)
                mVirtualHostnameCache.put(vh.toLowerCase(), cacheEntry);            
        }
    }

    @SuppressWarnings("unchecked")
    private Domain get(String key, LRUMap cache) {
        CacheEntry ce = (CacheEntry) cache.get(key);
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
    
    public synchronized Domain getById(String key) {
        return get(key, mIdCache);
    }
    
    public synchronized Domain getByName(String key) {
        return get(key.toLowerCase(), mNameCache);
    }
    
    public synchronized Domain getByVirtualHostname(String key) {
        return get(key.toLowerCase(), mVirtualHostnameCache);
    }    
}
