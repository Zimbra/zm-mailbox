/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016, 2019 Synacor, Inc.
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
import com.zimbra.cs.mailbox.Metadata;

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
     * Value stored in the Metadata.FN_POP3_UID ("p3uid").  If this field is empty, empty
     * string will be set
     */
    private String uid;

    /**
     * save enough info from the Message so we don't have to keep a reference to it.
     */
    public Pop3Message(Message msg) {
        this.id = msg.getId();
        this.size = msg.getSize();
        this.digest = msg.getDigest();
        this.uid = msg.getPop3Uid();
    }

    @Deprecated
    public Pop3Message(int id, long size, String digest) {
       this(id, size, digest, null);
    }

    public Pop3Message(int id, long size, String digest, Metadata metadata) {
        this.id = id;
        this.size = size;
        this.digest = digest;
        this.uid = (null != metadata) ? metadata.get(Metadata.FN_POP3_UID, null) : "";
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

    public String getUid(Boolean useCustomPop3UID) {
        if (false == useCustomPop3UID || null == uid || uid.isEmpty()) {
            // Classic UID string
            return id + "." + digest;
        } else {
            // Custom UID stored in the metadata
            return uid;
        }
    }

	void setUid(String uid) {
		this.uid = uid;
	}
}
