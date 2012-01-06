/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.acl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

public final class EffectiveACLCache {

    private static EffectiveACLCache sTheInstance = new EffectiveACLCache();

    private MemcachedMap<EffectiveACLCacheKey, ACL> mMemcachedLookup;

    public static EffectiveACLCache getInstance() { return sTheInstance; }

    EffectiveACLCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        ACLSerializer serializer = new ACLSerializer();
        mMemcachedLookup = new MemcachedMap<EffectiveACLCacheKey, ACL>(memcachedClient, serializer);
    }

    private static class ACLSerializer implements MemcachedSerializer<ACL> {

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
            } catch (ServiceException e) {
                Metadata meta = new Metadata((String) obj);
                return new ACL(meta);
            }
        }
    }

    private ACL get(EffectiveACLCacheKey key) throws ServiceException {
        return mMemcachedLookup.get(key);
    }

    private void put(EffectiveACLCacheKey key, ACL data) throws ServiceException {
        mMemcachedLookup.put(key, data);
    }

    public static ACL get(String acctId, int folderId) throws ServiceException {
        EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, folderId);
        return sTheInstance.get(key);
    }

    public static void put(String acctId, int folderId, ACL acl) throws ServiceException {
        EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, folderId);

        // if no effective ACL, return an empty ACL
        if (acl == null)
            acl = new ACL();
        sTheInstance.put(key, acl);
    }

    public void purgeMailbox(Mailbox mbox) throws ServiceException {
        String accountId = mbox.getAccountId();
        List<Folder> folders = mbox.getFolderList(null, SortBy.NONE);
        List<EffectiveACLCacheKey> keys = new ArrayList<EffectiveACLCacheKey>(folders.size());
        for (Folder folder : folders) {
            EffectiveACLCacheKey key = new EffectiveACLCacheKey(accountId, folder.getId());
            keys.add(key);
        }
        mMemcachedLookup.removeMulti(keys);
    }

    public void notifyCommittedChanges(PendingModifications mods, int changeId) {
        Set<EffectiveACLCacheKey> keysToInvalidate = new HashSet<EffectiveACLCacheKey>();
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                // We only need to pay attention to modified folders whose modification involves
                // permission change or move to a new parent folder.
                if (whatChanged instanceof Folder && (change.why & (Change.ACL | Change.FOLDER)) != 0) {
                    Folder folder = (Folder) whatChanged;
                    // Invalidate all child folders because their inherited ACL will need to be recomputed.
                    String acctId = folder.getMailbox().getAccountId();
                    List<Folder> subfolders = folder.getSubfolderHierarchy();  // includes "folder" folder
                    for (Folder subf : subfolders) {
                        EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, subf.getId());
                        keysToInvalidate.add(key);
                    }
                }
            }
        }
        if (mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.deleted.entrySet()) {
                MailItem.Type type = (MailItem.Type) entry.getValue().what;
                if (type == MailItem.Type.FOLDER) {
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null)
                        continue;  // just to be safe
                    EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, entry.getKey().getItemId());
                    keysToInvalidate.add(key);
                }
            }
        }
        try {
            mMemcachedLookup.removeMulti(keysToInvalidate);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify folder acl cache.  Some cached data may become stale.", e);
        }
    }
}
