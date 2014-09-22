/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util.memcached;


public class StringBasedMemcachedKey implements MemcachedKey {
    protected String prefix;
    protected String key;

    public StringBasedMemcachedKey(String prefix, String key) {
        this.prefix = prefix;
        this.key = key;
    }

    public String getKey() { return key; }

    public boolean equals(Object other) {
        if (other instanceof StringBasedMemcachedKey) {
            StringBasedMemcachedKey otherKey = (StringBasedMemcachedKey) other;
            return (key.equals(otherKey.key));
        }
        return false;
    }

    public int hashCode() {
        return key.hashCode();
    }

    /**
     * Returns the memcached key prefix.  Can be null.
     * @return
     */
    public String getKeyPrefix() {return prefix;}

    /**
     * Returns the memcached key value, without prefix.
     * @return
     */
    public String getKeyValue() {return key;}
}
