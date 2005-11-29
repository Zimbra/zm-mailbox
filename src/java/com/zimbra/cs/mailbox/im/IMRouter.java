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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.mailbox.im.IMChat.Participant;

public class IMRouter {
    
    private static final IMRouter sRouter = new IMRouter();
    
    public static IMRouter getInstance() { return sRouter; }
    
    Map<String, IMPersona> mBuddyListMap = new HashMap();
    
    public synchronized IMPersona findPersona(String address) 
    {
        IMPersona toRet = mBuddyListMap.get(address);
        if (toRet == null) {
           toRet = new IMPersona(address);
           mBuddyListMap.put(address, toRet);
        }
        
        return toRet;
    }
    
    void pushCloseChat(IMPersona from, IMChat chat)
    {
        // TODO writeme
    }
    
    void pushAddOutgoingSubscription(IMPersona from, String toAddr)
    {
        IMPersona to = findPersona(toAddr);
        to.addIncomingSubscription(from.getAddr());
    }
    
    void pushRemoveOutgoingSubscription(IMPersona from, String toAddr)
    {
        IMPersona to = findPersona(toAddr);
        to.addIncomingSubscription(from.getAddr());
    }
    
    void pushPresenceUpdates(IMPersona from)
    {
        for (String subscribee : from.incomingSubs()) {
            IMPersona other = findPersona(subscribee);
            other.handlePresenceUpdate(from.getAddr(), from.getMyPresence());
        }
    }
    
    void pushNewMessage(IMPersona from, IMChat chat, IMMessage msg)
    {
        for (Participant part : chat.participants()) {

            // we must not be in our own Participant list!
            assert(!from.equals(part.getAddress()));
            
            IMPersona other = findPersona(part.getAddress());
            other.handleMessage(from.getAddr(), chat.getThreadId(), msg);
        }
    }
}
