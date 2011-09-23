/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.index.global;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.google.common.base.Preconditions;

/**
 * Global Item ID (160-bit) consists of Account ID (128-bit UUID) and Item ID (32-bit int), so that it's globally
 * unique.
 *
 * @author ysasaki
 */
public final class GlobalItemID {
    private final UUID account;
    private final int id;

    public GlobalItemID(String account, int id) {
        this.account = UUID.fromString(account);
        this.id = id;
    }

    GlobalItemID(byte[] raw) {
        Preconditions.checkArgument(raw.length == 20);
        ByteBuffer buf = ByteBuffer.wrap(raw);
        account = new UUID(buf.getLong(), buf.getLong());
        id = buf.getInt();
    }

    public String getAccount() {
        return account.toString();
    }

    public int getItemID() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GlobalItemID) {
            return toString().equals(o.toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return account.toString() + ':' + id;
    }

    byte[] toBytes() {
        return toBytes(account, id);
    }

    static byte[] toBytes(UUID account, int id) {
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.putLong(account.getMostSignificantBits()).putLong(account.getLeastSignificantBits());
        buf.putInt(id);
        return buf.array();
    }

    static byte[] toBytes(String account, int id) {
        return toBytes(UUID.fromString(account), id);
    }

}
