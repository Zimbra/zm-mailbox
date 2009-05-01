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

// cache key for an account
public class AccountKey {
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
}
