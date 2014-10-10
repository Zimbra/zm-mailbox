package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014 Zimbra, Inc.
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

import java.util.concurrent.ConcurrentMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MapMaker;
import com.zimbra.common.service.ServiceException;

public class LocalMailItemCache implements MailItemCache {
    protected ConcurrentMap<String, LocalItemCache> map;

    public LocalMailItemCache() {
        map = new MapMaker().weakValues().makeMap();
    }

    @VisibleForTesting
    void flush() {
        map.clear();
    }

    protected String key(Mailbox mbox) {
        return mbox.getAccountId();
    }

    @Override
    public MailItem get(Mailbox mbox, int itemId) throws ServiceException {
        LocalItemCache itemCache = map.get(key(mbox));
        if (itemCache == null) {
            return null;
        }
        return itemCache.get(itemId);
    }

    @Override
    public MailItem get(Mailbox mbox, String uuid) throws ServiceException {
        LocalItemCache itemCache = map.get(key(mbox));
        if (itemCache == null) {
            return null;
        }
        return itemCache.get(uuid);
    }

    @Override
    public void put(Mailbox mbox, MailItem item) throws ServiceException {
        LocalItemCache itemCache = map.get(key(mbox));
        if (itemCache == null) {
            itemCache = new LocalItemCache();
            map.put(key(mbox), itemCache);
        }
        itemCache.put(item);
    }

    @Override
    public MailItem remove(Mailbox mbox, int itemId) throws ServiceException {
        LocalItemCache itemCache = map.get(key(mbox));
        if (itemCache == null) {
            return null;
        }
        return itemCache.remove(itemId);
    }

    @Override
    public void remove(Mailbox mbox) {
        LocalItemCache itemCache = map.get(key(mbox));
        if (itemCache != null) {
            itemCache.clear();
        }
    }
}
