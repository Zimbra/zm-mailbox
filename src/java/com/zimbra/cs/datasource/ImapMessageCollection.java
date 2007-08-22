/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class ImapMessageCollection
implements Iterable<ImapMessage>
{
    private Map<Integer, ImapMessage> mByItemId = new HashMap<Integer, ImapMessage>();
    private Map<Long, ImapMessage> mByUid = new HashMap<Long, ImapMessage>();
    
    public void add(ImapMessage msg) {
        mByItemId.put(msg.getItemId(), msg);
        mByUid.put(msg.getUid(), msg);
    }
    
    public void remove(ImapMessage msg) {
        mByItemId.remove(msg.getItemId());
        mByUid.remove(msg.getUid());
    }
    
    public ImapMessage getByItemId(int itemId) {
        return mByItemId.get(itemId);
    }
    
    public ImapMessage getByUid(long uid) {
        return mByUid.get(uid);
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
}
