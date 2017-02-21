/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.oauth;

import com.zimbra.common.util.memcached.MemcachedKey;

public class OAuthTokenCacheKey implements MemcachedKey {

    public static final String REQUEST_TOKEN_PREFIX = "req:";
    public static final String ACCESS_TOKEN_PREFIX = "acc:";

    private String mToken;
    private String mKeyPrefix;
    private String mKeyVal;

    public OAuthTokenCacheKey(String consumer_token,String key_prefix) {

        mToken = consumer_token;
        mKeyPrefix = key_prefix;
        mKeyVal = mToken;
    }

    public String getCounsumerToken() { return mToken; }

    public boolean equals(Object other) {
        if (other instanceof OAuthTokenCacheKey) {
            OAuthTokenCacheKey otherKey = (OAuthTokenCacheKey) other;
            return mKeyVal.equals(otherKey.mKeyVal);
        }
        return false;
    }

    public int hashCode()    { return mKeyVal.hashCode(); }
    public String toString() { return mKeyVal; }

    // MemcachedKey interface
    public String getKeyPrefix() { return mKeyPrefix; }
    public String getKeyValue() { return mKeyVal; }
}
