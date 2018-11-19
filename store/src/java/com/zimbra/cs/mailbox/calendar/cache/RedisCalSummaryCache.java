/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.redis.RedisUtils;
import com.zimbra.cs.mailbox.RedissonClientHolder;

public class RedisCalSummaryCache {
    private RedissonClient client;

    public RedisCalSummaryCache() {
        client = RedissonClientHolder.getInstance().getRedissonClient();
    }

    private String getCacheName(String accountId) {
        return RedisUtils.createAccountRoutedKey(accountId, String.format("CALSUMMARY"));
    }

    private RMap<Integer, String> getMap(String accountId) {
        return client.getMap(getCacheName(accountId));
    }

    protected CalendarData get(String accountId, int itemId) {
        RMap<Integer, String> calSummaryMap = getMap(accountId);
        if (calSummaryMap == null) {
            ZimbraLog.calendar.debug("No Redis calendar summary cache for %s", accountId);
            return null;
        }
        CalendarData calendarData = deserialize(calSummaryMap.get(itemId));
        ZimbraLog.calendar.trace("RedisCalSummaryCache.get(%s,%s) returning %s", accountId, itemId, calendarData);
        return calendarData;
    }

    protected void put(String accountId, int itemId, CalendarData calendarData) {
        RMap<Integer, String> calSummaryMap = getMap(accountId);
        if (calSummaryMap == null) {
            ZimbraLog.calendar.debug("No Redis calendar summary cache for %s to PUT to", accountId);
            return;
        }
        ZimbraLog.calendar.trace("RedisCalSummaryCache.put(%s,%s,%s)", accountId, itemId, calendarData);
        String encoded = serialize(calendarData);
        if (encoded == null) {
            calSummaryMap.fastRemove(itemId);  // RMap does not allow null keys or values
        } else {
            calSummaryMap.fastPut(itemId, serialize(calendarData));
        }
    }

    protected void purge(String accountId) {
        RMap<Integer, String> calSummaryMap = getMap(accountId);
        ZimbraLog.calendar.trace("RedisCalSummaryCache.purge(%s)", accountId);
        calSummaryMap.clear();
    }

    public String serialize(CalendarData value) {
        if (value == null) {
            return null;
        }
        Metadata meta = value.encodeMetadata();
        return (meta == null) ? null : meta.toString();
    }

    public CalendarData deserialize(String encoded) {
        if (encoded == null) {
            return null;
        }
        Metadata meta = null;
        try {
            meta = new Metadata(encoded);
            return new CalendarData(meta);
        } catch (ServiceException se) {
            ZimbraLog.calendar.warnQuietly("Problem deserializing String into CalendarData - returning null", se);
            return null;
        }
    }
}
