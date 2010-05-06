/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.datasource;

public class SyncState {
    private long lastUid = -1;
    private long exists = -1;
    private long unseen = -1;
    private int lastModSeq;

    public SyncState() {}

    public long getLastUid() { return lastUid; }
    public long getExists() { return exists; }
    public long getUnseen() { return unseen; }
    public int getLastModSeq() { return lastModSeq; }

    public void setExists(long exists) {
        this.exists = exists;
    }

    public void setUnseen(long unseen) {
        this.unseen = unseen;
    }

    public void setLastUid(long lastUid) {
        this.lastUid = lastUid;
    }

    public void updateLastUid(long uid) {
        if (uid > lastUid) {
            lastUid = uid;
        }
    }

    public void setLastModSeq(int lastModSeq) {
        this.lastModSeq = lastModSeq;
    }
    
    public String toString() {
        return String.format("{lastUid=%d,exists=%d,unseen=%d,lastModSeq=%d}",
                             lastUid, exists, unseen, lastModSeq);
    }
}
