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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IMChat {
    
    public static class Participant {
        private IMAddr mAddress;
        private String mName;
        private String mResource;
        
        private void init(IMAddr address, String resource, String name)
        {
            mAddress = address;
            mName = name;
            mResource = resource;
        }
        
        public IMAddr getAddress() { return mAddress; }
        public String getName()    { return mName; }
        public String mResource()  { return mResource; }
        
        public Participant(IMAddr address) {
            init(address, null, null);
        }
        public Participant(IMAddr address, String resource, String name) {
            init(address, resource, name);
        }
    }
    
    /**
     * Sequence # of the first message on the list: we do it this way so that we can truncate
     * from the beginning of the list w/o renumbering everything
     */
    int mFirstSeqNo = 0;
    
    public int getFirstSeqNo() { return mFirstSeqNo; }
    
    List<IMMessage> mMessages = new LinkedList();

    private String mThreadId;
    private Map<IMAddr, Participant> mParticipants = new HashMap();

    IMChat(String threadId, Participant initialPart)
    {
        mThreadId = threadId;
        mParticipants.put(initialPart.getAddress(), initialPart);
    }
    
    public String toString() {
        return "CHAT:"+mThreadId+"("+mParticipants.size()+" parts)";
    }
    
    public String getThreadId() { return mThreadId; }
    
    private Participant findParticipant(IMAddr addrFrom,String resourceFrom, String nameFrom)
    {
        Participant part = mParticipants.get(addrFrom);
        if (part == null) {
            part = new Participant(addrFrom, resourceFrom,  nameFrom);
            mParticipants.put(addrFrom, part);
            return part;
        }
        
        return mParticipants.get(addrFrom);
    }
    
    public Collection<Participant> participants() {
        return Collections.unmodifiableCollection(mParticipants.values());
    }

    public List<IMMessage> messages() {
        return Collections.unmodifiableList(mMessages);
    }
    
    
    void removeParticipant(IMAddr addr) 
    {
        mParticipants.remove(addr);
    }
    
    int addMessage(IMAddr addrFrom, String resourceFrom, String nameFrom, IMMessage msg)
    {
        // will trigger the add
        findParticipant(addrFrom, resourceFrom, nameFrom);
        
        mMessages.add(msg);
        
        return mMessages.size()+mFirstSeqNo;
    }
    
    /**
     * Message from us
     * 
     * @param msg
     */
    int addMessage(IMMessage msg)
    {
        mMessages.add(msg);
        return mMessages.size()+mFirstSeqNo;
    }
}
