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

import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public class LocalCalListCache implements CalListCache {
    protected ConcurrentHashMap<String, CalList> map = new ConcurrentHashMap<>();

    public LocalCalListCache() {
    }

    @VisibleForTesting
    void flush() {
        map.clear();
    }

    @Override
    public CalList get(String accountId) throws ServiceException{
        return map.get(accountId);
    }

    @Override
    public void put(String accountId, CalList calList) throws ServiceException {
        map.put(accountId, calList);
    }

    @Override
    public void remove(Mailbox mbox) throws ServiceException {
        map.remove(mbox.getAccountId());
    }
}
