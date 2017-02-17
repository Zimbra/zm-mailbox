/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;


public class ImapMessageCollection implements Iterable<ImapMessage> {
    private Map<Integer, ImapMessage> mByItemId = new HashMap<Integer, ImapMessage>();
    private Map<Long, ImapMessage> mByUid = new HashMap<Long, ImapMessage>();
    // Tracked message with a UID of 0 did not return a UID when appended to a
    // remote folder. In this case, we will try to fill in the correct uid
    // when we fetch the message later (see bug 26347).
    private Map<Integer, ImapMessage> mNoUid = new HashMap<Integer, ImapMessage>();

    public void add(ImapMessage msg) {
        mByItemId.put(msg.getItemId(), msg);
        long uid = msg.getUid();
        if (uid > 0) {
            mByUid.put(msg.getUid(), msg);
        } else {
            mNoUid.put(msg.getItemId(), msg);
        }
    }

    public ImapMessage getByItemId(int itemId) {
        return mByItemId.get(itemId);
    }
    
    public ImapMessage getByUid(long uid) {
        return mByUid.get(uid);
    }

    public Collection<ImapMessage> getNoUid() {
        return mNoUid.values();
    }
    
    public boolean containsItemId(int itemId) {
        return mByItemId.containsKey(itemId);
    }
    
    public boolean containsUid(long uid) {
        return mByUid.containsKey(uid);
    }
    
    public int size() {
        return mByItemId.size();
    }

    public Iterator<ImapMessage> iterator() {
        return mByItemId.values().iterator();
    }
    
    public Set<Long> getUids() {
        return mByUid.keySet();
    }
    
    public Set<Integer> getItemIds() {
        return mByItemId.keySet();
    }

    public long getLastUid() {
        long maxUid = 0;
        for (long uid : mByUid.keySet()) {
            if (uid > maxUid) maxUid = uid;
        }
        return maxUid;
    }
}
