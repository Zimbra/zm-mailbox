/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;


public class ImapMessageCollection implements Iterable<ImapMessage> {
    private Map<Integer, ImapMessage> mByItemId = new HashMap<Integer, ImapMessage>();
    private Map<Long, ImapMessage> mByUid = new HashMap<Long, ImapMessage>();
    // Tracked message with a UID of 0 did not return a UID when appended to a
    // remote folder. In this case, we will look up the message by checksum
    // later when it is downloaded (see bug 26347).
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

    public void remove(ImapMessage msg) {
        mByItemId.remove(msg.getItemId());
        long uid = msg.getUid();
        if (uid > 0) {
            mByUid.remove(msg.getUid());
        } else {
            mNoUid.remove(msg.getItemId());
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

    public long getMaxUid() {
        long maxUid = 0;
        for (long uid : mByUid.keySet()) {
            if (uid > maxUid) maxUid = uid;
        }
        return maxUid;
    }
}
