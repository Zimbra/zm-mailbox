/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;

public class LocalCtagInfoCache implements CtagInfoCache {
    protected Map<String, CtagInfo> map = new ConcurrentHashMap<>();

    public LocalCtagInfoCache() {
    }

    @VisibleForTesting
    public void flush() {
        map.clear();
    }

    protected String key(String accountId, int folderId) {
        return accountId + ":" + folderId;
    }

    protected Collection<String> keys(Collection<Pair<String,Integer>> pairs) {
        Collection<String> result = new ArrayList<>();
        for (Pair<String,Integer> pair: pairs) {
            result.add(key(pair.getFirst(), pair.getSecond()));
        }
        return result;
    }

    @Override
    public CtagInfo get(String accountId, int folderId) throws ServiceException {
        return map.get(key(accountId, folderId));
    }

    @Override
    public Map<Pair<String,Integer>, CtagInfo> get(List<Pair<String,Integer>> keys) throws ServiceException {
        Map<Pair<String,Integer>, CtagInfo> result = new HashMap<>();
        for (Pair<String,Integer> key: keys) {
            String keyStr = key(key.getFirst(), key.getSecond());
            result.put(key, map.get(keyStr));
        }
        return result;
    }

    @Override
    public void put(String accountId, int folderId, CtagInfo value) throws ServiceException {
        map.put(key(accountId, folderId), value);
    }

    @Override
    public void put(Map<Pair<String,Integer>, CtagInfo> pairs) throws ServiceException {
        for (Pair<String,Integer> key: pairs.keySet()) {
            String accountId = key.getFirst();
            Integer folderId = key.getSecond();
            CtagInfo value = pairs.get(key);
            map.put(key(accountId, folderId), value);
        }
    }

    @Override
    public void remove(String accountId, int folderId) throws ServiceException {
        map.remove(key(accountId, folderId));
    }

    @Override
    public void remove(List<Pair<String,Integer>> keys) throws ServiceException {
        for (Pair<String,Integer> key: keys) {
            String accountId = key.getFirst();
            Integer folderId = key.getSecond();
            map.remove(key(accountId, folderId));
        }
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        List<Folder> folders = mbox.getCalendarFolders(null, SortBy.NONE);
        for (Folder folder : folders) {
            remove(mbox.getAccountId(), folder.getId());
        }
    }
}
