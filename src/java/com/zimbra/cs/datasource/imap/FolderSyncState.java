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
package com.zimbra.cs.datasource.imap;

final class FolderSyncState {
    private long lastFetchedUid;
    private long lastUidNext;
    private int lastChangeId;
    private int lastModSeq;

    public long getLastFetchedUid() {
        return lastFetchedUid;
    }

    public long getLastUidNext() {
        return lastUidNext;
    }

    public int getLastChangeId() {
        return lastChangeId;
    }

    public int getLastModSeq() {
        return lastModSeq;
    }

    public void setLastFetchedUid(long uid) {
        lastFetchedUid = uid;
    }

    public void setLastUidNext(long lastUidNext) {
        this.lastUidNext = lastUidNext;
    }

    public void setLastChangeId(int lastChangeId) {
        this.lastChangeId = lastChangeId;
    }

    public void setLastModSeq(int lastModSeq) {
        this.lastModSeq = lastModSeq;
    }

    public void updateLastFetchedUid(long uid) {
        if (uid > lastFetchedUid) {
            lastFetchedUid = uid;
        }
    }

    public void update(MessageChanges fc) {
        lastChangeId = fc.getLastChangeId();
        lastModSeq = fc.getLastModSeq();
    }

    public String toString() {
        return String.format(
            "{lastFetchedUid=%d,lastUidNext=%d,lastChangeId=%d,lastModSeq=%d}",
            lastFetchedUid, lastUidNext, lastChangeId, lastModSeq);
    }
}
