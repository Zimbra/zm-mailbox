/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

// cache key for an account
public class AccountKey implements MemcachedKey {
    private String mAccountId;

    public AccountKey(String accountId) {
        mAccountId = accountId;
    }

    public String getAccountId() { return mAccountId; }

    public boolean equals(Object other) {
        if (other instanceof AccountKey) {
            AccountKey otherKey = (AccountKey) other;
            return mAccountId.equals(otherKey.mAccountId);
        }
        return false;
    }

    public int hashCode() {
        return mAccountId.hashCode();
    }

    // MemcachedKey interface
    public String getKeyPrefix() { return MemcachedKeyPrefix.CALENDAR_LIST; }
    public String getKeyValue() { return mAccountId; }
}
