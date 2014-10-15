/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;

public class MemcachedCalListCache implements CalListCache {
    @Autowired protected ZimbraMemcachedClient memcachedClient;
    protected MemcachedMap<AccountKey, CalList> mMemcachedLookup;

    public MemcachedCalListCache() {
    }

    @PostConstruct
    public void init() {
        mMemcachedLookup = new MemcachedMap<AccountKey, CalList>(memcachedClient, new CalListSerializer());
    }

    @Override
    public CalList get(String accountId) throws ServiceException{
        AccountKey key = new AccountKey(accountId);
        return mMemcachedLookup.get(key);
    }

    @Override
    public void put(String accountId, CalList calList) throws ServiceException {
        AccountKey key = new AccountKey(accountId);
        mMemcachedLookup.put(key, calList);
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        AccountKey key = new AccountKey(mbox.getAccountId());
        mMemcachedLookup.remove(key);
    }


    private static class CalListSerializer implements MemcachedSerializer<CalList> {
        CalListSerializer() { }

        @Override
        public Object serialize(CalList value) {
            return value.encodeMetadata().toString();
        }

        @Override
        public CalList deserialize(Object obj) throws ServiceException {
            Metadata meta = new Metadata((String) obj);
            return new CalList(meta);
        }
    }
}
