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
package com.zimbra.cs.account.ldap;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

/**
 * @author schemers
 **/
class ZimbraLdapDomainCache {
    
    private LRUMap mNameCache;
    private LRUMap mIdCache;
    private LRUMap mVirtualHostnameCache;
    
    private long mRefreshTTL;

/**
 * @param maxItems
 * @param refreshTTL
 */
    public ZimbraLdapDomainCache(int maxItems, long refreshTTL) {
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
            for (int i =0; i < vhost.length; i++) 
                mVirtualHostnameCache.remove(vhost[i].toLowerCase());
        }
    }
    
    public synchronized void put(LdapDomain entry) {
        if (entry != null) {
           	mNameCache.put(entry.getName(), entry);
           	mIdCache.put(entry.getId(), entry);
            String vhost[] = entry.getMultiAttr(Provisioning.A_zimbraVirtualHostname);
            for (int i =0; i < vhost.length; i++) 
                mVirtualHostnameCache.put(vhost[i].toLowerCase(), entry);
        }
    }

    public synchronized Domain getById(String key) {
        LdapDomain e = (LdapDomain) mIdCache.get(key);
        if (e != null && mRefreshTTL != 0 && e.isStale(mRefreshTTL)) {
            remove(e);
            e  = null;
        }
        return e;
    }

    public synchronized Domain getByName(String key) {
        LdapDomain e = (LdapDomain) mNameCache.get(key);
        if (e != null && mRefreshTTL != 0 && e.isStale(mRefreshTTL)) {
            remove(e);
            e  = null;
        }
        return e;       
    }

    public synchronized Domain getByVirtualHostname(String key) {
        LdapDomain e = (LdapDomain) mVirtualHostnameCache.get(key.toLowerCase());
        if (e != null && mRefreshTTL != 0 && e.isStale(mRefreshTTL)) {
            remove(e);
            e  = null;
        }
        return e;       
    }

}
