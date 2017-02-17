/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.datasource.imap;

final class FolderSyncState {
    private long lastFetchedUid;
    private long lastUidNext;
    private int lastChangeId;

    public long getLastFetchedUid() {
        return lastFetchedUid;
    }

    public long getLastUidNext() {
        return lastUidNext;
    }

    public int getLastChangeId() {
        return lastChangeId;
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

    public void updateLastFetchedUid(long uid) {
        if (uid > lastFetchedUid) {
            lastFetchedUid = uid;
        }
    }

    public String toString() {
        return String.format(
            "{lastFetchedUid=%d,lastUidNext=%d,lastChangeId=%d}",
            lastFetchedUid, lastUidNext, lastChangeId);
    }
}
