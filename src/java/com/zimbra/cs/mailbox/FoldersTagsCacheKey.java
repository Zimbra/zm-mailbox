/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import com.zimbra.common.util.memcached.MemcachedKey;
import com.zimbra.cs.memcached.MemcachedKeyPrefix;

public class FoldersTagsCacheKey implements MemcachedKey {
    private String mKeyStr;

    public FoldersTagsCacheKey(String accountId, int changeToken) {
        mKeyStr = accountId + ":" + changeToken;
    }

    public boolean equals(Object other) {
        if (other instanceof FoldersTagsCacheKey) {
            FoldersTagsCacheKey otherKey = (FoldersTagsCacheKey) other;
            return mKeyStr.equals(otherKey.mKeyStr);
        }
        return false;
    }

    public int hashCode() {
        return mKeyStr.hashCode();
    }

    // MemcachedKey interface
    public String getKeyPrefix() { return MemcachedKeyPrefix.MBOX_FOLDERS_TAGS; }
    public String getKeyValue() { return mKeyStr; }
}
