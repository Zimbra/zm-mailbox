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
package com.zimbra.cs.im;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.StringUtil;

public class IMBuddy {
    private IMAddr mAddress;
    private String mName;
    private IMPresence mPresence;
    private SubType mSubType;
    
    private Map<String, IMChat> mChats = new HashMap();
    private List<IMGroup> mGroups = new LinkedList();
    
    /**
     * @author tim
     *
     * VERY confusing lingo here -- think about this this way:
     * 
     *   If I have a subscription TO you, then I will receive your info.
     *   If you have a subscription FROM me, then you must send me your info. 
     */
    public enum SubType {
        TO /*OUTGOING SUBSCRIPTION: i receive your data */ , 
        FROM /*INCOMING SUBSCRIPTION: I send */, 
        BOTH, NONE;
        
        /**
         * @return TRUE if I receive your data 
         */
        boolean isOutgoing() { 
            return (this == TO || this == BOTH); 
        }
        
        /**
         * @return TRUE if I send you my data
         */
        boolean isIncoming() {
            return (this == FROM || this == BOTH); 
        }
        
        /**
         * @return a SubType with the "i receive your data" bit ON
         */
        SubType setOutgoing() {
            switch (this) {
            case NONE: case TO: return TO;
            case FROM: case BOTH: return BOTH;
            }
            assert(false);
            return null;
        }
        
        /**
         * @return a SubType with the "i send you data" bit ON
         */
        SubType setIncoming() {
            switch (this) {
            case NONE: case FROM: return FROM;
            case TO: case BOTH: return BOTH;
            }
            assert(false);
            return null;
        }
        
        /**
         * @return a SubType with the "i send you data" bit CLEARED
         */
        SubType clearOutgoing() {
            switch(this) {
            case FROM: case BOTH: return FROM;
            case NONE: case TO: return NONE;
            }
            assert(false); 
            return null;
        }
        
        /**
         * @return a SubType with the "i receive your data" bit CLEARED
         */
        SubType clearIncoming() {
            switch(this) {
            case NONE: case FROM: return NONE;
            case TO: case BOTH: return TO;
            }
            assert(false); 
            return null;
        }
    }
    
    IMBuddy(IMAddr address, String name)
    {
        mAddress = address;
        mName = name;
        mPresence = null;
        mSubType = SubType.NONE;
    }
    
    public String toString() {
        return "BUDDY: "+mAddress+"("+mName+")";
    }
    
    void setSubType(SubType st) { mSubType = st; }

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
    
    void setPresence(IMPresence presence) {
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
    
    public IMAddr getAddress() { return mAddress; }
    
    public String getName() { return mName; }
    
    public IMPresence getPresence() { 
        return mPresence;
    }
    
    public SubType getSubType() { return mSubType; }
    
    private static final String FN_ADDRESS     = "a"; 
    private static final String FN_GROUP       = "g";
    private static final String FN_NAME        = "n";
    private static final String FN_NUM_GROUPS  = "ng"; 
    private static final String FN_SUBTYPE    = "s"; 
    
    Metadata encodeAsMetadata()
    {
        Metadata meta = new Metadata();

        meta.put(FN_ADDRESS, mAddress);
        
        if (!StringUtil.isNullOrEmpty(mName)) {
            meta.put(FN_NAME, mName);
        }
        meta.put(FN_SUBTYPE, mSubType.toString());
        meta.put(FN_NUM_GROUPS, mGroups.size());
        int offset = 0;
        for (IMGroup group : mGroups) {
            meta.put(FN_GROUP+offset, group.getName());
            offset++;
        }
        
        return meta;
    }
    
    static IMBuddy decodeFromMetadata(Metadata meta, IMPersona persona) throws ServiceException
    {
        String address = meta.get(FN_ADDRESS);
        String name = meta.get(FN_NAME, null);
        
        IMBuddy toRet = new IMBuddy(new IMAddr(address), name);
        
        try {
            toRet.setSubType(SubType.valueOf(meta.get(FN_SUBTYPE)));
        } catch (IllegalArgumentException e) {
            toRet.setSubType(SubType.NONE);
        }
        
        int numGroups = (int)meta.getLong(FN_NUM_GROUPS);

        for (int i = 0; i < numGroups; i++) {
            IMGroup group = persona.getGroup(meta.get(FN_GROUP+i));
            toRet.addGroup(group);
        }
        
        return toRet;
    }
}
