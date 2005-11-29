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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.im;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IMBuddy {
    private String mAddress;
    private String mName;
    private IMPresence mPresence;
    private SubType mSubType;
    
    private Map<String, IMChat> mChats = new HashMap();
    private List<IMGroup> mGroups = new LinkedList();
    
    public enum SubType {
        TO, FROM, BOTH, NONE;
    }
    
    IMBuddy(String address, String name)
    {
        mAddress = address;
        mName = name;
        mPresence = null;
        mSubType = SubType.NONE;
    }
    
    public String getAddress() { return mAddress; }
    public String getName() { return mName; }
    
    public void setSubType(SubType st) { mSubType = st; }

    void clearGroups() {
        mGroups.clear();
    }
    
    void removeGroup(IMGroup group) {
        mGroups.remove(group);
    }
    
    void addGroup(IMGroup group) {
        if (!mGroups.contains(group)) {
            mGroups.add(group);
        }
    }
    
    public void setPresence(IMPresence presence) {
        mPresence = presence;
    }
    
    public void addChat(String threadId, IMChat chat) {
        mChats.put(threadId, chat);
    }
    
    public Map<String, IMChat> chats() {
        return Collections.unmodifiableMap(mChats);
    }
    
    public List<IMGroup> groups() {
        return Collections.unmodifiableList(mGroups);
    }
    
    public int numGroups() {
        return mGroups.size();
    }
    
    public IMPresence getPresence() { 
        return mPresence;
    }
    
    public SubType getSubType() { return mSubType; }

    
    
}
