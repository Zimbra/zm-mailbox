/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

// cache key for a calendar folder in an account
public class CalendarKey implements MemcachedKey {
    private String mAccountId;
    private int mFolderId;
    private String mKeyVal;

    public CalendarKey(String accountId, int folderId) {
        mAccountId = accountId;
        mFolderId = folderId;
        mKeyVal = mAccountId + ":" + folderId;
    }

    public String getAccountId() { return mAccountId; }
    public int getFolderId() { return mFolderId; }

    public boolean equals(Object other) {
        if (other instanceof CalendarKey) {
            CalendarKey otherKey = (CalendarKey) other;
            return mKeyVal.equals(otherKey.mKeyVal);
        }
        return false;
    }

    public int hashCode()    { return mKeyVal.hashCode(); }
    public String toString() { return mKeyVal; }

    // MemcachedKey interface
    public String getKeyPrefix() { return MemcachedKeyPrefix.CTAGINFO; }
    public String getKeyValue() { return mKeyVal; }
}
