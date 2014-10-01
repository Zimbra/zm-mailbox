/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.acl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class MemcachedEffectiveACLCache implements EffectiveACLCache {
    protected MemcachedMap<EffectiveACLCacheKey, ACL> mMemcachedLookup;

    public MemcachedEffectiveACLCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        ACLSerializer serializer = new ACLSerializer();
        mMemcachedLookup = new MemcachedMap<EffectiveACLCacheKey, ACL>(memcachedClient, serializer);
    }

    private ACL get(EffectiveACLCacheKey key) throws ServiceException {
        return mMemcachedLookup.get(key);
    }

    private void put(EffectiveACLCacheKey key, ACL data) throws ServiceException {
        mMemcachedLookup.put(key, data);
    }

    public ACL get(String acctId, int folderId) throws ServiceException {
        EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, folderId);
        return get(key);
    }

    public void put(String acctId, int folderId, ACL acl) throws ServiceException {
        EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, folderId);

        // if no effective ACL, return an empty ACL
        if (acl == null)
            acl = new ACL();
        put(key, acl);
    }

    public void remove(Mailbox mbox) throws ServiceException {
        String accountId = mbox.getAccountId();
        List<Folder> folders = mbox.getFolderList(null, SortBy.NONE);
        List<EffectiveACLCacheKey> keys = new ArrayList<EffectiveACLCacheKey>(folders.size());
        for (Folder folder : folders) {
            EffectiveACLCacheKey key = new EffectiveACLCacheKey(accountId, folder.getId());
            keys.add(key);
        }
        mMemcachedLookup.removeMulti(keys);
    }

    public void remove(Set<Pair<String, Integer>> keys) throws ServiceException {
        Set<EffectiveACLCacheKey> keysToInvalidate = new HashSet<EffectiveACLCacheKey>();
        for (Pair<String, Integer> key: keys) {
            keysToInvalidate.add(new EffectiveACLCacheKey(key.getFirst(), key.getSecond()));
        }
        mMemcachedLookup.removeMulti(keysToInvalidate);
    }


    static class ACLSerializer implements MemcachedSerializer<ACL> {

        public ACLSerializer() { }

        @Override
        public Object serialize(ACL value) {
            return value.encode().toString();
        }

        @Override
        public ACL deserialize(Object obj) throws ServiceException {
            try {
                // first try with old serialization
                MetadataList meta = new MetadataList((String) obj);
                return new ACL(meta);
            } catch (Exception e) {
                Metadata meta = new Metadata((String) obj);
                return new ACL(meta);
            }
        }
    }

    static class EffectiveACLCacheKey implements MemcachedKey {
        private String mAcctId;
        private int mFolderId;
        private String mKeyVal;

        public EffectiveACLCacheKey(String acctId, int folderId) {
            mAcctId = acctId;
            mFolderId = folderId;
            mKeyVal = mAcctId + ":" + folderId;
        }

        public String getAccountId() { return mAcctId; }
        public int getFolderId() { return mFolderId; }

        public boolean equals(Object other) {
            if (other instanceof EffectiveACLCacheKey) {
                EffectiveACLCacheKey otherKey = (EffectiveACLCacheKey) other;
                return mKeyVal.equals(otherKey.mKeyVal);
            }
            return false;
        }

        public int hashCode()    { return mKeyVal.hashCode(); }
        public String toString() { return mKeyVal; }

        // MemcachedKey interface
        public String getKeyPrefix() { return MemcachedKeyPrefix.EFFECTIVE_FOLDER_ACL; }
        public String getKeyValue() { return mKeyVal; }
    }
}
