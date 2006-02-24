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

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.cs.account.Provisioning;

/**
 * @author schemers
 * */
class LdapGroupEntryCache {

    static class LdapGroupEntry {
        private String mGroupId;
        private String[] mMemberOf;
        private long mLoadtime;
        
        LdapGroupEntry(LdapDistributionList list) {
            mGroupId = list.getGroupId();
            mMemberOf = list.getMultiAttr(Provisioning.A_zimbraMemberOf);
            mLoadtime = System.currentTimeMillis();
        }
        
        public String getGroupId() { return mGroupId; }
        public String[] getMemberOf() { return mMemberOf; }
        public boolean isStale(long ageInMillis) {
            return mLoadtime + ageInMillis < System.currentTimeMillis(); 
        }
    }
    
    private LRUMap mIdCache;
    
    private int mRefreshTTL;

/**
 * @param maxItems
 * @param refreshTTL
 */
    public LdapGroupEntryCache(int maxItems, int refreshTTL) {
        mIdCache = new LRUMap(maxItems);
        mRefreshTTL = refreshTTL;
    }

    public synchronized void clear() {
        mIdCache.clear();
    }

    public synchronized void remove(String groupId) {
        if (groupId != null) {
            mIdCache.remove(groupId);
        }
    }

    public synchronized LdapGroupEntry put(LdapDistributionList list) {
        LdapGroupEntry entry = new LdapGroupEntry(list);
        mIdCache.put(entry.getGroupId(), entry);
        return entry;
    }

    public synchronized LdapGroupEntry getByGroupId(String key) {
        LdapGroupEntry e = (LdapGroupEntry) mIdCache.get(key);
        if (e != null && mRefreshTTL != 0 && e.isStale(mRefreshTTL)) {
            remove(key);
            e  = null;
        }
        return e;
    }
}
