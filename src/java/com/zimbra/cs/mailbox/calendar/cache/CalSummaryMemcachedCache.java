/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.BigByteArrayMemcachedMap;
import com.zimbra.common.util.memcached.ByteArraySerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

public class CalSummaryMemcachedCache {

    private BigByteArrayMemcachedMap<CalSummaryKey, CalendarData> mMemcachedLookup;

    CalSummaryMemcachedCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        CalSummarySerializer serializer = new CalSummarySerializer();
        mMemcachedLookup = new BigByteArrayMemcachedMap<CalSummaryKey, CalendarData>(memcachedClient, serializer); 
    }

    private static class CalSummarySerializer implements ByteArraySerializer<CalendarData> {
        
        public byte[] serialize(CalendarData value) {
            try {
                return value.encodeMetadata().toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.calendar.warn("Unable to serialize data for calendar summary cache", e);
                return null;
            }
        }

        public CalendarData deserialize(byte[] bytes) throws ServiceException {
            if (bytes != null) {
                String encoded;
                try {
                    encoded = new String(bytes, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    ZimbraLog.calendar.warn("Unable to deserialize data for calendar summary cache", e);
                    return null;
                }
                Metadata meta = new Metadata(encoded);
                return new CalendarData(meta);
            } else {
                return null;
            }
        }
    }

    public CalendarData getForRange(CalSummaryKey key, long rangeStart, long rangeEnd)
    throws ServiceException {
        CalendarData calData = mMemcachedLookup.get(key);
        if (calData != null && rangeStart >= calData.getRangeStart() && rangeEnd <= calData.getRangeEnd())
            return calData.getSubRange(rangeStart, rangeEnd);
        else
            return null;
    }

    public void put(CalSummaryKey key, CalendarData calData) throws ServiceException {
        mMemcachedLookup.put(key, calData);
    }

    void purgeMailbox(Mailbox mbox) throws ServiceException {
        String accountId = mbox.getAccountId();
        List<Folder> folders = mbox.getCalendarFolders(null, SortBy.NONE);
        List<CalSummaryKey> keys = new ArrayList<CalSummaryKey>(folders.size());
        for (Folder folder : folders) {
            CalSummaryKey key = new CalSummaryKey(accountId, folder.getId());
            keys.add(key);
        }
        mMemcachedLookup.removeMulti(keys);
    }

    void notifyCommittedChanges(PendingModifications mods, int changeId) {
        Set<CalSummaryKey> keysToInvalidate = new HashSet<CalSummaryKey>();
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof Folder) {
                    Folder folder = (Folder) item;
                    byte viewType = folder.getDefaultView();
                    if (viewType == MailItem.TYPE_APPOINTMENT || viewType == MailItem.TYPE_TASK) {
                        CalSummaryKey key = new CalSummaryKey(folder.getMailbox().getAccountId(), folder.getId());
                        keysToInvalidate.add(key);
                    }
                }
            }
        }
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                if (whatChanged instanceof Folder) {
                    Folder folder = (Folder) whatChanged;
                    byte viewType = folder.getDefaultView();
                    if (viewType == MailItem.TYPE_APPOINTMENT || viewType == MailItem.TYPE_TASK) {
                        CalSummaryKey key = new CalSummaryKey(folder.getMailbox().getAccountId(), folder.getId());
                        keysToInvalidate.add(key);
                    }
                }
            }
        }
        if (mods.deleted != null) {
            // This code gets called even for non-calendar items, for example it's called for every email
            // being emptied from Trash.  But there's no way to short circuit out of here because the delete
            // notification doesn't tell us the item type of what's being deleted.  Oh well.
            for (Map.Entry<ModificationKey, Object> entry : mods.deleted.entrySet()) {
                Object deletedObj = entry.getValue();
                if (deletedObj instanceof Folder) {
                    Folder folder = (Folder) deletedObj;
                    byte viewType = folder.getDefaultView();
                    if (viewType == MailItem.TYPE_APPOINTMENT || viewType == MailItem.TYPE_TASK) {
                        CalSummaryKey key = new CalSummaryKey(folder.getMailbox().getAccountId(), folder.getId());
                        keysToInvalidate.add(key);
                    }
                } else if (deletedObj instanceof Integer) {
                    // We only have item id.  Assume it's a folder id and issue a delete.
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null) continue;  // just to be safe
                    int itemId = ((Integer) deletedObj).intValue();
                    CalSummaryKey key = new CalSummaryKey(acctId, itemId);
                    keysToInvalidate.add(key);
                }
                // Let's not worry about hard deletes of invite/reply emails.  It has no practical benefit.
                // Besides, when deletedObj is an Integer, we can't tell if it's a calendaring Message.
            }
        }
        try {
            mMemcachedLookup.removeMulti(keysToInvalidate);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify ctag info cache.  Some cached data may become stale.", e);
        }
    }
}
