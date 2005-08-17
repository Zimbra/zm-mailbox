/*
 * Created on Oct 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap;

import org.apache.commons.collections.map.LRUMap;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 * 01/13/05 Greg Solovyev - added an option to keep a map only by IDs
 * */
public class LiquidLdapEntryCache {
    
    private LRUMap mNamedCache;
    private LRUMap mIdCache;
    
    private int mRefreshTTL;

/**
 * @param maxItems
 * @param refreshTTL
 */
    public LiquidLdapEntryCache(int maxItems, int refreshTTL) {
    	mNamedCache = new LRUMap(maxItems);
    	mIdCache = new LRUMap(maxItems);
        mRefreshTTL = refreshTTL;
    }

    public synchronized void clear() {
    	mNamedCache.clear();
        mIdCache.clear();
    }

    public synchronized void remove(LdapNamedEntry entry) {
        if (entry != null) {
           	mNamedCache.remove(entry.getName());
           	mIdCache.remove(entry.getId());
        }
    }
    
    public synchronized void put(LdapNamedEntry entry) {
        if (entry != null) {
           	mNamedCache.put(entry.getName(), entry);
           	mIdCache.put(entry.getId(), entry);
        }
    }
    
    public synchronized LdapNamedEntry get(String key) {
        LdapNamedEntry e = (LdapNamedEntry) mNamedCache.get(key);
        if(e==null)
        	e = (LdapNamedEntry) mIdCache.get(key);
        if (e != null && mRefreshTTL != 0) {
            try { 
                e.refreshIfStale(mRefreshTTL);
            } catch (ServiceException se) {
                remove(e);
                e = null;
            }        
        }
        return e;       
    }
    
    public synchronized LdapNamedEntry getById(String key) {
        LdapNamedEntry e = (LdapNamedEntry) mIdCache.get(key);

        if (e != null && mRefreshTTL != 0) {
            try { 
                e.refreshIfStale(mRefreshTTL);
            } catch (ServiceException se) {
                remove(e);
                e = null;
            }        
        }
        return e;       
    }
    
    public synchronized LdapNamedEntry getByname(String key) {
        LdapNamedEntry e = (LdapNamedEntry) mNamedCache.get(key);

        if (e != null && mRefreshTTL != 0) {
            try { 
                e.refreshIfStale(mRefreshTTL);
            } catch (ServiceException se) {
                remove(e);
                e = null;
            }        
        }
        return e;       
    }
}
