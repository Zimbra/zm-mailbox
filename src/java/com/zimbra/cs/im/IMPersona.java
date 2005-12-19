package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.im.IMBuddy.SubType;
import com.zimbra.cs.im.IMChat.Participant;
import com.zimbra.cs.im.IMPresence.Show;
import com.zimbra.cs.im.IMSubscriptionEvent.Op;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;

/**
 * @author tim
 *
 * A single "persona" in the IM world
 */
public class IMPersona {

    private IMAddr mAddr;
    private Map<IMAddr, IMBuddy> mBuddyList = new HashMap();
    private Map<String, IMGroup> mGroups = new HashMap();
    private Map<String, IMChat> mChats = new HashMap();
    private boolean mSubsLoaded = false;
    
    private Set<Session> mListeners = new HashSet();
    
    
    public String toString() {
        return new Formatter().format("PERSONA:%s  Presence:%s SubsLoaded:%s", 
                mAddr, mMyPresence, mSubsLoaded ? "YES" : "NO").toString();
    }
    
    IMPersona(IMAddr addr) {
        assert(addr != null);
        mAddr = addr; 
    }
    
    public IMAddr getAddr() { return mAddr; }
    
    void loadSubs() {
        if (!mSubsLoaded) {
            for (IMBuddy buddy : mBuddyList.values()) {
                if (buddy.getSubType().isOutgoing()) {
                    IMProbeEvent event = new IMProbeEvent(mAddr, buddy.getAddress());
                    IMRouter.getInstance().postEvent(event);
                }
            }
            mSubsLoaded = true;
        }
    }
    
    // these TWO parameters make up my presence - the first one is the presence
    // I have saved in the DB, and the second is a flag if I am online or offline
    private IMPresence mMyPresence = new IMPresence(Show.ONLINE, (byte)1, null);
    private boolean mIsOnline = false;
    

    /**
     * Finds an existing group OR CREATES ONE
     * @param name
     * @return
     */
    IMGroup getOrCreateGroup(String name) {
        IMGroup toRet = mGroups.get(name);
        if (toRet == null) {
            toRet = new IMGroup(name);
            mGroups.put(name, toRet);
        }
        return toRet;
    }
    
    /**
     * Finds an existing buddy OR CREATES ONE for the specified addess 
     * @param address
     * @param name
     * @return
     */
    private IMBuddy getOrCreateBuddy(IMAddr address, String name) {
        IMBuddy toRet = mBuddyList.get(address);
        if (toRet == null) {
            toRet = new IMBuddy(address, name);
            mBuddyList.put(address, toRet);
        }
        return toRet;
    }
    
    private IMChat getOrCreateChat(String thread, IMAddr fromAddress) {
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
    
    static class SubscribedNotification implements IMNotification {
        IMAddr mAddr;
        String mName;
        String[] mGroups;
        boolean mRemove;
        
        SubscribedNotification(IMAddr address, String name, String[] groups, boolean remove) {
            mAddr = address;
            mName = name;
            mGroups = groups;
            mRemove = remove;
        }
        
        public Element toXml(Element parent) {
            Element e;
            if (mRemove) { 
                e = parent.addElement(IMService.E_UNSUBSCRIBED);
            } else {
                e = parent.addElement(IMService.E_SUBSCRIBED);
                e.addAttribute(IMService.A_NAME, mName);
            }
            
            if (mGroups != null) {
                e.addAttribute(IMService.A_GROUPS, StringUtil.join(",", mGroups));
            }
            
            e.addAttribute(IMService.A_TO, mAddr.getAddr());

            return e;
        }
    }
    
    public synchronized void addListener(Session session) {
        if (mListeners.size() == 0) {
            mIsOnline = true;
            try {
                pushMyPresence();
            } catch(ServiceException e) {
                e.printStackTrace();
            }
        }
        mListeners.add(session);
    }
    
    public synchronized void removeListener(Session session) {
        mListeners.remove(session);
        if (mListeners.size() == 0) {
            mIsOnline = false;
            try {
                pushMyPresence();
            } catch(ServiceException e) {
                e.printStackTrace();
            }
        }
    }
    
    synchronized void postIMNotification(IMNotification not) throws ServiceException {
        for (Session session : mListeners) 
            session.notifyIM(not);
    }
    
    synchronized void flush(OperationContext octxt) throws ServiceException {
        IMRouter.getInstance().flush(octxt, this);
    }
    
    public synchronized void addOutgoingSubscription(OperationContext octxt, IMAddr address, String name, String[] groups) throws ServiceException 
    { 
        IMBuddy buddy = getOrCreateBuddy(address, name);
        
        buddy.clearGroups();

        buddy.setName(name);

        for (String grpName : groups) {
            IMGroup group = this.getOrCreateGroup(grpName);
            buddy.addGroup(group);
        }
        
        SubType st = buddy.getSubType();
        if (!st.isOutgoing())
            buddy.setSubType(st.setOutgoing());
            
        IMEvent event = new IMSubscriptionEvent(mAddr, address, Op.SUBSCRIBE);
        IMRouter.getInstance().postEvent(event);
        
        postIMNotification(new SubscribedNotification(address, name, groups, false));
        flush(octxt);
    }
    
    public synchronized void removeOutgoingSubscription(OperationContext octxt, IMAddr address, String name, String[] groups) throws ServiceException
    {
        mBuddyList.remove(address);

        IMEvent event = new IMSubscriptionEvent(mAddr, address, Op.UNSUBSCRIBE);
        IMRouter.getInstance().postEvent(event);

        postIMNotification(new SubscribedNotification(address, name, groups, true));
        
        flush(octxt);
    }
    
    public Iterable<IMGroup> groups() {
        return new Iterable<IMGroup>() { 
            public Iterator<IMGroup>iterator() {
                return (Collections.unmodifiableCollection(mGroups.values())).iterator();                
            } 
        }; 
    }
    
    public Iterable<IMChat> chats() {
        return new Iterable<IMChat>() { 
            public Iterator<IMChat>iterator() {
                return (Collections.unmodifiableCollection(mChats.values())).iterator();                
            } 
        }; 
    }
    
    public Iterable<IMBuddy> buddies() {
        return new Iterable<IMBuddy>() { 
            public Iterator<IMBuddy>iterator() {
                return (Collections.unmodifiableCollection(mBuddyList.values())).iterator();
            } 
        }; 
    }
    

    synchronized void pushMyPresence() throws ServiceException
    {
        IMPresence presence = getEffectivePresence();
        
        ArrayList<IMAddr> targets = new ArrayList();
        
        for (IMBuddy buddy : buddies()) {
            if (buddy.getSubType().isIncoming()) 
                targets.add(buddy.getAddress());
        }
        
        IMPresenceUpdateEvent event = new IMPresenceUpdateEvent(mAddr, presence, targets);
        IMRouter.getInstance().postEvent(event);
        
        // need to send it to myself in case there I have other sessions listening...
        postIMNotification(event);
        
    }
    
    
    public synchronized void setMyPresence(OperationContext octxt, IMPresence presence) throws ServiceException
    {
        mMyPresence = presence;
        flush(octxt);
        pushMyPresence();
    }
    
    public IMPresence getEffectivePresence() {
        if (mIsOnline) 
            return mMyPresence;
        else 
            return new IMPresence(Show.OFFLINE, mMyPresence.getPriority(), mMyPresence.getStatus());
    }
    
    /**
     * Creates a new chat to "address" (sending a message)
     * 
     * @param address
     * @param message
     */
    public synchronized String newChat(OperationContext octxt, IMAddr address, IMMessage message) throws ServiceException
    {
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
        
        sendMessage(octxt, toRet, message);

        return threadId;
    }
    
    /**
     * Sends a message over an existing chat
     * 
     * @param address
     * @param message
     */
    public synchronized void sendMessage(OperationContext octxt, String threadId, IMMessage message) throws ServiceException 
    {
        IMChat chat = mChats.get(threadId);
        if (chat == null) {
            // FIXME
            throw ServiceException.FAILURE("No such chat: "+threadId, null);
        }            
        sendMessage(octxt, chat, message);
    }
    
    private synchronized void sendMessage(OperationContext octxt, IMChat chat, IMMessage message) throws ServiceException
    {
        message.setFrom(mAddr);
        
        int seqNo = chat.addMessage(message);
        
        ArrayList toList = new ArrayList();
        for (Participant part : chat.participants()) {
            if (!part.getAddress().equals(mAddr))
                toList.add(part.getAddress());
        }
        
        flush(octxt);
        
        IMSendMessageEvent event = new IMSendMessageEvent(mAddr, chat.getThreadId(), toList, message);

        postIMNotification(event.getNotificationEvent(seqNo));
        
        IMRouter.getInstance().postEvent(event);
    }
    
    public synchronized void closeChat(OperationContext octxt, IMChat chat) throws ServiceException {
        ArrayList<IMAddr> toList = new ArrayList();
        for (Participant part : chat.participants()) {
            if (!part.getAddress().equals(mAddr))
                toList.add(part.getAddress());
        }
        
        mChats.remove(chat.getThreadId());
        
        IMLeftChatEvent event = new IMLeftChatEvent(mAddr, chat.getThreadId(), toList);
        IMRouter.getInstance().postEvent(event);
    }
    
    public synchronized void addUserToChat(IMChat chat, IMAddr addr) throws ServiceException {
        ArrayList<IMAddr> toList = new ArrayList();
        for (Participant part : chat.participants()) {
            if (!part.getAddress().equals(mAddr))
                toList.add(part.getAddress());
        }
        
        // do we have a buddy for this new user?  If so, then use the buddy's info
        IMBuddy buddy = mBuddyList.get(addr);
        IMChat.Participant part;
        
        if (buddy != null)
            part = new IMChat.Participant(buddy.getAddress(), null, buddy.getName());
        else
            part = new IMChat.Participant(addr);
        
        chat.addParticipant(part);
        
        IMEnteredChatEvent event = new IMEnteredChatEvent(mAddr, chat.getThreadId(), toList, addr);
        IMRouter.getInstance().postEvent(event);
    }
    
    private static final String FN_ADDRESS     = "a";
    private static final String FN_PRESENCE    = "p";
    private static final String FN_NUM_BUDDIES = "nb";
    private static final String FN_BUDDY       = "b";
    
    synchronized Metadata encodeAsMetatata()
    {
        Metadata meta = new Metadata();
        
        meta.put(FN_ADDRESS, mAddr);
        meta.put(FN_PRESENCE, mMyPresence.encodeAsMetadata());
        meta.put(FN_NUM_BUDDIES, mBuddyList.size());
        int offset = 0;
        for (IMBuddy buddy : mBuddyList.values()) {
            meta.put(FN_BUDDY+offset, buddy.encodeAsMetadata());
            offset++;
        }
        
        return meta;
    }
    
    static IMPersona decodeFromMetadata(IMAddr address, Metadata meta) throws ServiceException
    {
        // FIXME: how are config entries getting written w/o an ADDRESS setting?  
        String mdAddr = meta.get(FN_ADDRESS, null); 
        if (mdAddr!=null && mdAddr.equals(address.getAddr())) {
            IMPersona toRet = new IMPersona(address);

            IMPresence presence = IMPresence.decodeMetadata(meta.getMap(FN_PRESENCE));
            toRet.mMyPresence = presence;
            
            int numBuddies = (int)meta.getLong(FN_NUM_BUDDIES);
            for (int i = 0; i < numBuddies; i++) {
                IMBuddy buddy = IMBuddy.decodeFromMetadata(meta.getMap(FN_BUDDY+i), toRet);
                assert(!buddy.getAddress().equals(address));
                toRet.mBuddyList.put(buddy.getAddress(), buddy);
            }
            assert(toRet.getAddr().equals(address));
            return toRet;
        } else {
            // addresses don't match!  clear buddy list!
            return new IMPersona(address);
        }
    }

    /**
     * HANDLE* functions are called by IMEvent execution
     * 
     * @param octxt
     * @param address
     * @throws ServiceException
     */
    void handleAddIncomingSubscription(IMAddr address) throws ServiceException
    {
        IMBuddy buddy = this.getOrCreateBuddy(address, null);
        SubType st = buddy.getSubType();
        if (!st.isIncoming()) {
            buddy.setSubType(st.setIncoming());
        }
        
        // send my presence to the newly-subscribed person
        if (mMyPresence != null) {
            ArrayList<IMAddr> target= new ArrayList();
            target.add(address);
            
            // no need to send it to myself here
            IMPresenceUpdateEvent event = new IMPresenceUpdateEvent(mAddr, mMyPresence, target); 
            IMRouter.getInstance().postEvent(event);
        }
            
        flush(null);
    }
    
    /**
     * HANDLE* functions are called by IMEvent execution
     * 
     * @param address
     * @throws ServiceException
     */
    void handleRemoveIncomingSubscription(IMAddr address) throws ServiceException
    {
        mBuddyList.remove(address);
        flush(null);
    }

    /**
     * HANDLE* functions are called by IMEvent execution
     * 
     * @param from
     * @param threadId
     * @throws ServiceException
     */
    void handleLeftChat(IMAddr from, String threadId) throws ServiceException {
        IMChat chat = mChats.get(threadId);
        if (chat != null) {
            chat.removeParticipant(from);
        }
    }
    
    public void handleAddChatUser(String threadId, IMAddr addr) throws ServiceException {
        IMChat chat = mChats.get(threadId);
            
        if (chat != null) {
            // do we have a buddy for this new user?  If so, then use the buddy's info
            IMBuddy buddy = mBuddyList.get(addr);
            IMChat.Participant part;
            
            if (buddy != null)
                part = new IMChat.Participant(buddy.getAddress(), null, buddy.getName());
            else
                part = new IMChat.Participant(addr);
            
            chat.addParticipant(part);
        }
    }
    
    
    /**
     * HANDLE* functions are called by IMEvent execution
     * 
     * @param from
     * @param presence
     */
    void handlePresenceUpdate(IMAddr from, IMPresence presence) {
        IMBuddy buddy = mBuddyList.get(from);
        if (buddy != null) {
            buddy.setPresence(presence);
        }
    }
    
    /**
     * HANDLE* functions are called by IMEvent execution
     * 
     * @param from
     * @param threadId
     * @param message
     * @return sequence number of message in chat
     */
    int handleMessage(IMAddr from, String threadId, IMMessage message) {
        IMChat chat = getOrCreateChat(threadId, from);
        return chat.addMessage(from, null, null, message);
    }
}
