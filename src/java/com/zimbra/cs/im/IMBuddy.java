/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xmpp.packet.Roster;

public class IMBuddy {
    private IMAddr mAddress;
    private String mName;
    private IMPresence mPresence;
    private SubType mSubType;
    private Roster.Ask mAsk;
    
    private Map<String, IMChat> mChats = new HashMap<String, IMChat>();
    private List<IMGroup> mGroups = new LinkedList<IMGroup> ();
    
    /**
     * @author tim
     *
     * The SubType represents the current confirmed state of the subscription.
     * We might be trying to change the state, depending on the ASK value,
     * but that is not reflected in the SubType
     *
     * VERY confusing lingo here -- think about this this way:
     * 
     *   If I have a subscription TO you, then I will receive your info.
     *   If you have a subscription FROM me, then you must send me your info. 
     */
    public enum SubType {
        TO /*OUTGOING SUBSCRIPTION: i receive your data */ , 
        FROM /*INCOMING SUBSCRIPTION: I send */, 
        BOTH, NONE,
        UNSET; // null
        
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
                case UNSET: case NONE: case TO: return TO;
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
                case UNSET: case NONE: case FROM: return FROM;
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
                case UNSET: return UNSET;
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
                case UNSET: return UNSET;
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
        mSubType = SubType.UNSET;
    }
    
    public String toString() {
        return "BUDDY: "+mAddress+"("+mName+") ["+mPresence == null ? "null" : mPresence.toString() + "]";
    }
    
    void setAsk(Roster.Ask ask) { mAsk = ask; }
    
    void setName(String name) { mName = name; }

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
    
    public Roster.Subscription getSubscription() {
        switch (mSubType) {
            case UNSET: return Roster.Subscription.none;
            case TO: return Roster.Subscription.to;
            case FROM: return Roster.Subscription.from;
            case BOTH: return Roster.Subscription.both;
            case NONE: return Roster.Subscription.none;
        }
        return null;
    }
    public SubType getSubType() { return mSubType; }
    public Roster.Ask getAsk() { return mAsk; }
    
}
