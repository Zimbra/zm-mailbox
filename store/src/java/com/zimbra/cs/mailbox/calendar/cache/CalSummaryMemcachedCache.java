/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import com.zimbra.cs.session.PendingLocalModifications;
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
        CalSummarySerializer() { }

        @Override
        public byte[] serialize(CalendarData value) {
            try {
                return value.encodeMetadata().toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                ZimbraLog.calendar.warn("Unable to serialize data for calendar summary cache", e);
                return null;
            }
        }

        @Override
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

    void notifyCommittedChanges(PendingLocalModifications mods, int changeId) {
        Set<CalSummaryKey> keysToInvalidate = new HashSet<CalSummaryKey>();
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                if (whatChanged instanceof Folder) {
                    Folder folder = (Folder) whatChanged;
                    MailItem.Type viewType = folder.getDefaultView();
                    if (viewType == MailItem.Type.APPOINTMENT || viewType == MailItem.Type.TASK) {
                        CalSummaryKey key = new CalSummaryKey(folder.getAccountId(), folder.getId());
                        keysToInvalidate.add(key);
                    }
                }
            }
        }
        if (mods.deleted != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.deleted.entrySet()) {
                MailItem.Type type = (MailItem.Type) entry.getValue().what;
                if (type == MailItem.Type.FOLDER) {
                    // We only have item id.  Assume it's a folder id and issue a delete.
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null)
                        continue;  // just to be safe
                    CalSummaryKey key = new CalSummaryKey(acctId, entry.getKey().getItemId());
                    keysToInvalidate.add(key);
                }
                // Let's not worry about hard deletes of invite/reply emails.  It has no practical benefit.
            }
        }
        try {
            mMemcachedLookup.removeMulti(keysToInvalidate);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Unable to notify ctag info cache.  Some cached data may become stale.", e);
        }
    }
}
