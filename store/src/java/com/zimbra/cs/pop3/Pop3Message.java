/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.pop3;

import com.zimbra.cs.mailbox.Message;

/**
 * one for each message in the mailbox
 *
 * @since Nov 26, 2004
 * @author schemers
 */
public final class Pop3Message {
    private boolean retrieved = false;
    private boolean deleted = false;
    private int id;
    private long size; // raw size from blob store
    private String digest;

    /**
     * save enough info from the Message so we don't have to keep a reference to it.
     */
    public Pop3Message(Message msg) {
        this(msg.getId(), msg.getSize(), msg.getDigest());
    }

    public Pop3Message(int id, long size, String digest) {
        this.id = id;
        this.size = size;
        this.digest = digest;
    }

    long getSize() {
        return size;
    }

    int getId() {
        return id;
    }

    void setRetrieved(boolean value) {
        retrieved = value;
    }

    boolean isRetrieved() {
        return retrieved;
    }

    void setDeleted(boolean value) {
        deleted = value;
    }

    boolean isDeleted() {
        return deleted;
    }

    String getDigest() {
        return digest;
    }
}
