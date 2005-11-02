/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import java.util.List;

import org.apache.commons.collections.map.LRUMap;

/**
 * @author schemers
 * 01/13/05 Greg Solovyev - added an option to keep a map only by IDs
 * */
public class ZimbraLdapEntryCache {
    
    private LRUMap mNamedCache;
    private LRUMap mIdCache;
    
    private int mRefreshTTL;

/**
 * @param maxItems
 * @param refreshTTL
 */
    public ZimbraLdapEntryCache(int maxItems, int refreshTTL) {
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

    public synchronized void put(List entries, boolean clear) {
        if (entries != null) {
            if (clear) clear();
            for (int i=0; i < entries.size(); i++) 
                put((LdapNamedEntry) entries.get(i));
        }
    }

    public synchronized LdapNamedEntry getById(String key) {
        LdapNamedEntry e = (LdapNamedEntry) mIdCache.get(key);
        if (e != null && mRefreshTTL != 0 && e.isStale(mRefreshTTL)) {
            remove(e);
            e  = null;
        }
        return e;
    }
    
    public synchronized LdapNamedEntry getByName(String key) {
        LdapNamedEntry e = (LdapNamedEntry) mNamedCache.get(key);
        if (e != null && mRefreshTTL != 0 && e.isStale(mRefreshTTL)) {
            remove(e);
            e  = null;
        }
        return e;       
    }
}
