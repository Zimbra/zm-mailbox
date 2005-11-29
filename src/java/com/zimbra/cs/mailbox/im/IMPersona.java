package com.zimbra.cs.mailbox.im;

import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.im.IMChat.Participant;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 *
 * A single "persona" in the IM world
 */
public class IMPersona {

    private String mAddr;
    
    IMPersona(String addr) {
        mAddr = addr; 
    }
    
    public String getAddr() { return mAddr; }
    
    
    Map<String/*address*/ , IMBuddy> mBuddyList = new HashMap();
    Map<String, IMGroup> mGroups = new HashMap();
    Map<String, String> mIncomingSubs = new HashMap();
    Map<String, IMChat> mChats = new HashMap();
    
    private IMPresence mMyPresence = null;

    IMGroup getGroup(String name) {
        IMGroup toRet = mGroups.get(name);
        if (toRet == null) {
            toRet = new IMGroup(name);
            mGroups.put(name, toRet);
        }
        return toRet;
    }
    
    IMBuddy getBuddy(String address, String name) {
        IMBuddy toRet = mBuddyList.get(address);
        if (toRet == null) {
            toRet = new IMBuddy(address, name);
            mBuddyList.put(address, toRet);
        }
        return toRet;
    }
    
    IMChat getChat(String thread, String fromAddress) {
        IMChat toRet = mChats.get(thread);
        if (toRet == null) {
            Participant part;
            
            // do we have a buddy for this chat?  If so, then use the buddy's info
            IMBuddy buddy = mBuddyList.get(fromAddress);
            
            if (buddy != null)
                part = new IMChat.Participant(buddy.getAddress(), null, buddy.getName());
            else
                part = new IMChat.Participant(fromAddress);
            
            toRet = new IMChat(thread, part);
            mChats.put(thread, toRet);
        }
        return toRet;
    }
    
    public IMChat lookupChatOrNull(String threadId) {
        return mChats.get(threadId);
    }
    
    
    public void addOutgoingSubscription(String address, String name, String[] groups) {
        IMBuddy buddy = getBuddy(address, name);
        
        for (String grpName : groups) {
            IMGroup group = this.getGroup(grpName);
            buddy.addGroup(group);
        }
        
        switch (buddy.getSubType()) {
        case FROM:
            buddy.setSubType(IMBuddy.SubType.BOTH);
            break;
        case NONE:
            buddy.setSubType(IMBuddy.SubType.TO);
            break;
        }
        
        IMRouter.getInstance().pushAddOutgoingSubscription(this, address);
    }
    
    public void removeOutgoingSubscription(String address, String name, String[] groups) {
        IMBuddy buddy = getBuddy(address, name);
        
        if (groups == null) {
            buddy.clearGroups();
        } else {
            for (String grpName : groups) {
                IMGroup group = this.getGroup(grpName);
                buddy.removeGroup(group);
            }
        }
        
        if (buddy.numGroups() == 0) {
            switch (buddy.getSubType()) {
            case TO:
                buddy.setSubType(IMBuddy.SubType.NONE);
                break;
            case BOTH:
                buddy.setSubType(IMBuddy.SubType.FROM);
                break;
            }
        }
        
        IMRouter.getInstance().pushRemoveOutgoingSubscription(this, address);
    }
    
    public Collection<IMGroup> groups() {
        return Collections.unmodifiableCollection(mGroups.values());
    }
    
    public Collection<IMChat> chats() {
        return Collections.unmodifiableCollection(mChats.values());
    }
    
    public Collection<IMBuddy> buddies() {
        return Collections.unmodifiableCollection(mBuddyList.values());
    }
    
    Collection<String> incomingSubs() {
        return Collections.unmodifiableCollection(mIncomingSubs.values());
    }
    
    
    Map<String, String> getIncomingSubs() {
        return Collections.unmodifiableMap(mIncomingSubs);
    }
    
    public void addIncomingSubscription(String address)
    {
        if (!mIncomingSubs.containsKey(address)) 
            mIncomingSubs.put(address, address);
    }
    
    public void removeIncomingSubscription(String address)
    {
        mIncomingSubs.remove(address);
    }
    
    public void setMyPresence(IMPresence presence) {
        mMyPresence = presence;
        IMRouter.getInstance().pushPresenceUpdates(this);
    }
    public IMPresence getMyPresence() { return mMyPresence; }
    
    /**
     * Creates a new chat to "address" (sending a message)
     * 
     * @param address
     * @param message
     */
    public String newChat(String address, IMMessage message) {
        String threadId = LdapUtil.generateUUID();

        // add the other user as the first participant in this chat
        IMChat.Participant part;
        
        // do we have a buddy for this chat?  If so, then use the buddy's info
        IMBuddy buddy = mBuddyList.get(address);
        
        if (buddy != null)
            part = new IMChat.Participant(buddy.getAddress(), null, buddy.getName());
        else
            part = new IMChat.Participant(address);
        
        IMChat toRet = new IMChat(threadId, part);
        
        mChats.put(threadId, toRet);
        
        sendMessage(toRet, message);

        return threadId;
    }
    
    /**
     * Sends a message over an existing chat
     * 
     * @param address
     * @param message
     */
    public void sendMessage(String threadId, IMMessage message) throws ServiceException {
        IMChat chat = mChats.get(threadId);
        if (chat == null) {
            // FIXME
            throw ServiceException.FAILURE("No such chat: "+threadId, null);
        }            
        sendMessage(chat, message);
    }
    
    private void sendMessage(IMChat chat, IMMessage message) {
        chat.addMessage(message);
        
        IMRouter.getInstance().pushNewMessage(this, chat, message);
    }
    
    public void removeChat(IMChat chat) {
        IMRouter.getInstance().pushCloseChat(this, chat);
        
        mChats.remove(chat.getThreadId());
    }
    
    //
    // HANDLE* functions deal with incoming changes from other Personas
    //
    
    
    public void handlePresenceUpdate(String from, IMPresence presence) {
        IMBuddy buddy = mBuddyList.get(from);
        if (buddy != null) {
            buddy.setPresence(presence);
        }
    }
    
    public void handleMessage(String from, String threadId, IMMessage message) {
        IMChat chat = getChat(threadId, from);
        chat.addMessage(from, null, null, message);
    }
    
}
