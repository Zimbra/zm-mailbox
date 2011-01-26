/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
