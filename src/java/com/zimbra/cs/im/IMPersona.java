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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.AuthToken;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Roster;
import org.xmpp.packet.IQ.Type;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.im.IMBuddy.SubType;
import com.zimbra.cs.im.IMChat.Participant;
import com.zimbra.cs.im.IMMessage.Lang;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.im.IMPresence.Show;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author tim
 *
 * A single "persona" in the IM world
 */
public class IMPersona {

    private IMAddr mAddr;
    private Map<IMAddr, IMBuddy> mBuddyList = new HashMap<IMAddr, IMBuddy>();
    private Map<String, IMGroup> mGroups = new HashMap<String, IMGroup>();
    private Map<String, IMChat> mChats = new HashMap<String, IMChat>();
    private boolean mSubsLoaded = false;
    
    private Set<Session> mListeners = new HashSet<Session>();
    
//    private FakeClientSession mXMPPSession;
    private ClientSession mXMPPSession;
    
    // these TWO parameters make up my presence - the first one is the presence
    // I have saved in the DB, and the second is a flag if I am online or offline
    private IMPresence mMyPresence = new IMPresence(Show.ONLINE, (byte)1, null);
    private boolean mIsOnline = false;
    
    public String toString() {
        return new Formatter().format("PERSONA:%s  Presence:%s SubsLoaded:%s", 
                mAddr, mMyPresence, mSubsLoaded ? "YES" : "NO").toString();
    }
    
    IMPersona(IMAddr addr) {
        assert(addr != null);
        mAddr = addr;
    }
    
    void init() {
        try {
            if (Provisioning.getInstance().getLocalServer().getBooleanAttr(Provisioning.A_zimbraXMPPEnabled, false)) {
                mXMPPSession = new ClientSession(Provisioning.getInstance().getLocalServer().getName(), new FakeClientConnection(this), XMPPServer.getInstance().getSessionManager().nextStreamID());
                
                AuthToken at = new AuthToken(mAddr.getAddr());
                mXMPPSession.setAuthToken(at);
                try {
                    mXMPPSession.setAuthToken(at, XMPPServer.getInstance().getUserManager(), "zcs");
                    
                } catch (UserNotFoundException ex) {
                    System.out.println(ex.toString());
                    ex.printStackTrace();
                }
                
                // have to request roster immediately -- as soon as we send our first presence update, we will start
                // receiving presence notifications from our buddies -- and if we don't have the roster loaded, then we 
                // will not know what to do with them...
                Roster rosterRequest = new Roster();
                xmppRoute(rosterRequest);
            }
        } catch (ServiceException ex) {
            ZimbraLog.im.warn("Caught Exception checking if XMPP enabled " + ex.toString(), ex);
        }
    }
    
    public IMAddr getAddr() { return mAddr; }
    
    void loadSubs() {
        if (!mSubsLoaded) {
            mSubsLoaded = true;
        }
    }
    
    /**
     * Callback -- called from the IMRouter thread.  May ONLY lock the current persona.
     * @param packet
     */
    synchronized void process(String text) {
        ZimbraLog.im.info("IMPersona("+getAddr()+") processing text packet: "+text);
    }
    
    /**
     * Callback -- called from the IMRouter thread.  May ONLY lock the current persona.
     * @param packet
     */
    synchronized void process(Packet packet) {
        ZimbraLog.im.info("IMPersona("+getAddr()+") processing packet: "+packet.toXML());

        boolean toMe = true;
        
        if (packet.getTo() != null && !packet.getTo().toBareJID().equals(this.getAddr().getAddr()))  {
            if (!(packet instanceof Message))
                return;
            
            toMe = false;
        }
        
        if (packet instanceof Message) {
            Message msg = (Message)packet;
            String toAddr = msg.getTo().getNode() + '@'+  msg.getTo().getDomain();
            String fromAddr = msg.getFrom().getNode() + '@' + msg.getFrom().getDomain();
            String threadId = msg.getThread();
            
            String subject = msg.getSubject();
            String body = msg.getBody();
            
            if ((subject == null || subject.length() == 0) &&
                        (body == null || body.length() == 0))
                return;  // ignore empty message for now (<composing> update!)

            IMMessage immsg = new IMMessage(subject==null?null:new TextPart(subject),
                        body==null?null:new TextPart(body));
            
            immsg.setFrom(new IMAddr(fromAddr));
            
//            assert(toAddr.equals(this.getAddr().getAddr()));
            
            List<IMAddr> toList = new ArrayList<IMAddr>(1);
            toList.add(this.getAddr());
            
            String remoteAddr;
            if (toMe)
                remoteAddr = fromAddr;
            else
                remoteAddr = toAddr;
            
            if (threadId == null || threadId.length() == 0) {
                for (IMChat cur : mChats.values()) {
                    // find an existing point-to-point chat with that user in it
                    if ((cur.participants().size() <=  2) && (cur.lookupParticipant(new IMAddr(remoteAddr)) != null)) {
                        threadId = cur.getThreadId();
                        break;
                    }
                }
                if (threadId == null || threadId.length() == 0) {
                    threadId = remoteAddr;
                }
            }
            
            IMChat chat = getOrCreateChat(threadId, immsg.getFrom());
            int seqNo = chat.addMessage(immsg.getFrom(), null, null, immsg);
            
            IMMessageNotification notification = new IMMessageNotification(immsg.getFrom(), chat.getThreadId(), immsg, seqNo);
            
            try {
                postIMNotification(notification);
            } catch (ServiceException ex) {
                ZimbraLog.im.warn("Caught ServiceException %s", ex);
            }            
            
            
        } else if (packet instanceof Presence) {
            Presence pres = (Presence)packet;
            Presence.Type ptype = pres.getType();
            
            if (ptype == null) {
                onRemotePresenceChange(pres, Show.ONLINE);
            } else {
                Presence reply = null;
                switch (ptype) {
                    case unavailable:
                        onRemotePresenceChange(pres, Show.OFFLINE);
                        break;
                    case subscribe:
                        // auto-accept for now
                        reply = new Presence();
                        reply.setType(Presence.Type.subscribed);
                        reply.setTo(pres.getFrom());
                        xmppRoute(reply);
                        break;
                    case subscribed:
                    {
                        ZimbraLog.im.info("Presence.subscribed: " +pres.toString());
                        
                        IMAddr address = IMAddr.fromJID(pres.getFrom());
                        
                        // it could potentially have been deleted, so re-create it if necessary
                        IMBuddy buddy = getOrCreateBuddy(address, address.toString());
                        
                        SubType st = buddy.getSubType();
                        if (!st.isOutgoing())
                            buddy.setSubType(st.setOutgoing());
                        
                        try {
                            postIMNotification(new IMSubscribedNotification(address, buddy.getName(), null, false));
                            postIMNotification(new IMPresenceUpdateNotification(address, buddy.getPresence()));
                        } catch(ServiceException ex) {
                            ZimbraLog.im.warn("Caught Exception: " + ex.toString(), ex);
                        }
                    }
                    break;
                    case unsubscribe:
                        ZimbraLog.im.info("Presence.unsubscribe: " +pres.toString());
                        // auto-accept for now
                        reply = new Presence();
                        reply.setTo(pres.getFrom());
                        reply.setFrom(pres.getTo());
                        reply.setType(Presence.Type.unsubscribed);
                        xmppRoute(reply);
                        break;
                    case unsubscribed:
                    {
                        ZimbraLog.im.info("Presence.unsubscribed: " +pres.toString());
                        
//                        IMAddr address = IMAddr.fromJID(pres.getFrom());
//                        IMBuddy buddy = getOrCreateBuddy(address, address.toString());
    //
//                        SubType st = buddy.getSubType();
//                        if (st.isOutgoing())
//                            buddy.setSubType(st.clearOutgoing());
//                      
//                        try {
//                            postIMNotification(new SubscribedNotification(address, buddy.getName(), null, true));
//                        } catch(ServiceException ex) {
//                            ZimbraLog.im.warn("Caught Exception: " + ex.toString(), ex);
//                        }
                        
                    }
                    break;
                    case probe:
                        ZimbraLog.im.info("Presence.probe: " +pres.toString());
                        try {
                            pushMyPresence(pres.getFrom());
                        } catch(ServiceException ex) {}
                        break;
                    case error:
                        ZimbraLog.im.info("Presence.error: " +pres.toString());
                        break;
                }
            }
        } else if (packet instanceof Roster) {
            Roster roster = (Roster)packet;

            ZimbraLog.im.info("Got a roster: "+roster.toXML());
            if (roster.getType() == Type.result) {
                mBuddyList.clear();
                
                for (Roster.Item item : roster.getItems()) {
                    IMAddr buddyAddr = IMAddr.fromJID(item.getJID());
                    IMBuddy newBuddy = new IMBuddy(buddyAddr, item.getName());
                    
                    boolean doAdd = false;
                    
                    Roster.Subscription subscript = item.getSubscription();
                    switch (subscript) {
                        case none:
                            newBuddy.setSubType(SubType.NONE);
                            break;
                        case to:
                            doAdd = true;
                            newBuddy.setSubType(SubType.TO);
                            break;
                        case from:
                            newBuddy.setSubType(SubType.FROM);
                            break;
                        case both:
                            doAdd = true;
                            newBuddy.setSubType(SubType.BOTH);
                            break;
                        case remove:
                            newBuddy.setSubType(SubType.NONE);
                            break;
                    }
                    
                    if (doAdd) {
                        newBuddy.setPresence(new IMPresence(Show.OFFLINE, (byte) 0, "offline"));
                        
                        if (mBuddyList.containsKey(buddyAddr)) {
                            System.out.println("Key: "+buddyAddr+ " already in buddy list!\n");
                        }
                        
                        mBuddyList.put(buddyAddr, newBuddy);
                        
                        for (String group : item.getGroups()) {
                            if (!mGroups.containsKey(group))
                                mGroups.put(group, new IMGroup(group));
                            newBuddy.addGroup(mGroups.get(group));
                        }

                        try {
                            postIMNotification(IMSubscribedNotification.create(buddyAddr, newBuddy.getName(), newBuddy.groups(), false));
                            postIMNotification(new IMPresenceUpdateNotification(buddyAddr, newBuddy.getPresence()));
                        } catch(ServiceException ex) {
                            ZimbraLog.im.warn("Caught Exception: " + ex.toString(), ex);
                        }
                        
                        
//                        // HACKHACK
//                        {
//                            Presence reply;
//                            reply = new Presence();
//                            reply.setTo(newBuddy.getAddress().makeJID());
//                            reply.setFrom(mAddr.makeJID());
//                            reply.setType(Presence.Type.probe);
//                            xmppRoute(reply);
//                        }
                        
                        
                    } else {
                        try {
                            postIMNotification(IMSubscribedNotification.create(buddyAddr, newBuddy.getName(), newBuddy.groups(), true));
                        } catch(ServiceException ex) {
                            ZimbraLog.im.warn("Caught Exception: " + ex.toString(), ex);
                        }
                    }
                }
            } else if (roster.getType() == Type.set) {
                for (Roster.Item item : roster.getItems()) {
                    IMAddr buddyAddr = IMAddr.fromJID(item.getJID());
                    
                    boolean doAdd = false;
                    
                    IMBuddy newBuddy = getOrCreateBuddy(buddyAddr, item.getName());
                    Roster.Subscription subscript = item.getSubscription();
                    switch (subscript) {
                        case none:
                            newBuddy.setSubType(SubType.NONE);
                            break;
                        case to:
                            doAdd = true;
                            newBuddy.setSubType(SubType.TO);
                            break;
                        case from:
                            newBuddy.setSubType(SubType.FROM);
                            break;
                        case both:
                            doAdd = true;
                            newBuddy.setSubType(SubType.BOTH);
                            break;
                        case remove:
                            newBuddy.setSubType(SubType.NONE);
                            break;
                    }
                    
                    if (doAdd) {
                    
                        newBuddy.setPresence(new IMPresence(Show.OFFLINE, (byte) 0, "offline"));
                        
                        newBuddy.clearGroups();
                        for (String group : item.getGroups()) {
                            if (!mGroups.containsKey(group))
                                mGroups.put(group, new IMGroup(group));
                            newBuddy.addGroup(mGroups.get(group));
                        }
                        
                        try {
                            postIMNotification(IMSubscribedNotification.create(buddyAddr, newBuddy.getName(), newBuddy.groups(), false));
                            postIMNotification(new IMPresenceUpdateNotification(buddyAddr, newBuddy.getPresence()));
                        } catch(ServiceException ex) {
                            ZimbraLog.im.warn("Caught Exception: " + ex.toString(), ex);
                        }
                        
//                        // HACKHACK
//                        {
//                            Presence reply;
//                            reply = new Presence();
//                            reply.setTo(newBuddy.getAddress().makeJID());
//                            reply.setFrom(mAddr.makeJID());
//                            reply.setType(Presence.Type.probe);
//                            xmppRoute(reply);
//                        }
                        
                        
                    } else {
                        try {
                            mBuddyList.remove(newBuddy.getAddress());
                            postIMNotification(IMSubscribedNotification.create(buddyAddr, newBuddy.getName(), newBuddy.groups(), true));
                        } catch(ServiceException ex) {
                            ZimbraLog.im.warn("Caught Exception: " + ex.toString(), ex);
                        }
                    }                        
                }
                
            }
            mSubsLoaded = true;
        }
    }
    
    private void onRemotePresenceChange(Presence pres, Show imShow) {
        // presence update
        IMAddr fromAddr = IMAddr.fromJID(pres.getFrom());
        
        Presence.Show pShow = pres.getShow();
        if (pShow != null) {
            switch (pShow) {
                case chat:
                    imShow = Show.CHAT;
                    break;
                case away:
                    imShow = Show.AWAY;
                    break;
                case xa: 
                    imShow = Show.XA;
                    break;
                case dnd:
                    imShow = Show.DND;
                    break;
            }
        }

        int prio = pres.getPriority();

        IMPresence newPresence = new IMPresence(imShow, (byte)prio, pres.getStatus());
        
        IMBuddy buddy = mBuddyList.get(fromAddr);
        if (buddy != null) {
            buddy.setPresence(newPresence);
            IMPresenceUpdateNotification event = new IMPresenceUpdateNotification(fromAddr, newPresence);

            try { 
                postIMNotification(event);
            } catch (ServiceException ex) {
                ZimbraLog.im.warn("Caught ServiceException: " + ex.toString(), ex);
            }
        } else {
            ZimbraLog.im.warn("Got presence update for buddy: "+fromAddr+" but wasn't in buddy list!");
        }
        
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
        
        Presence subscribePacket = new Presence(Presence.Type.subscribe);
        subscribePacket.setTo(address.makeJID());
        xmppRoute(subscribePacket);
    }
    
    public synchronized void removeOutgoingSubscription(OperationContext octxt, IMAddr address, String name, String[] groups) throws ServiceException
    {
        // FIXME: what to do if the remote guy isn't there?? 
        mBuddyList.remove(address);
        
        Presence unsubscribePacket = new Presence(Presence.Type.unsubscribe);
        unsubscribePacket.setTo(address.makeJID());
        xmppRoute(unsubscribePacket);
        
        postIMNotification(new IMSubscribedNotification(address, name, groups, true));
    }
    
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
    
    public IMChat lookupChatOrNull(String threadId) {
        return mChats.get(threadId);
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
    
    private Mailbox getMailbox() throws ServiceException  {
        return MailboxManager.getInstance().getMailboxByAccount(Provisioning.getInstance().get(AccountBy.name, getAddr().getAddr()));
    }
    
    synchronized void flush(OperationContext octxt) throws ServiceException {
        Mailbox mbox = getMailbox();
        assert(getAddr().getAddr().equals(mbox.getAccount().getName()));
        Metadata md = encodeAsMetatata();
        mbox.setConfig(octxt, "im", md);
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
    
    public synchronized void setMyPresence(OperationContext octxt, IMPresence presence) throws ServiceException
    {
        // TODO optimize out change-to-same eventually (leave for now, very convienent as is)
        mMyPresence = presence;
        flush(octxt);
        pushMyPresence();
    }
    
    public synchronized IMPresence getEffectivePresence() {
        if (mIsOnline) 
            return mMyPresence;
        else 
            return new IMPresence(Show.OFFLINE, mMyPresence.getPriority(), mMyPresence.getStatus());
    }
    
    private synchronized void pushMyPresence() throws ServiceException
    {
        pushMyPresence(null);
    }

    private synchronized void pushMyPresence(JID sendTo) throws ServiceException
    {
        IMPresence presence = getEffectivePresence();
        
        if (sendTo == null) {
            // send it to my client in case there I have other sessions listening...
            IMPresenceUpdateNotification event = new IMPresenceUpdateNotification(mAddr, presence);
            postIMNotification(event);
        }
        
        updateXMPPPresence(sendTo, presence);
        
    }
    
    
    private void updateXMPPPresence(JID sendTo, IMPresence pres) {
        if (mXMPPSession != null) {
            Presence xmppPresence;
            
            if (pres.getShow() == Show.OFFLINE) {
                xmppPresence = new Presence(Presence.Type.unavailable);
            } else {
                xmppPresence = new Presence();
                
                if (pres.getShow() == Show.CHAT)
                    xmppPresence.setShow(Presence.Show.chat);
                else if (pres.getShow() == Show.AWAY)
                    xmppPresence.setShow(Presence.Show.away);
                else if (pres.getShow() == Show.XA)
                    xmppPresence.setShow(Presence.Show.xa);
                else if (pres.getShow() == Show.DND)
                    xmppPresence.setShow(Presence.Show.dnd);
            }
            
            if (pres.getStatus() != null && pres.getStatus().length() > 0)
                xmppPresence.setStatus(pres.getStatus());
            
            if (sendTo != null)
                xmppPresence.setTo(sendTo);
                
            xmppRoute(xmppPresence);
            
        }
    }
    
    private void xmppRoute(Packet packet) {
        if (mXMPPSession != null) {
            ZimbraLog.im.info("SENDING XMPP PACKET: "+packet.toXML());
            packet.setFrom(mXMPPSession.getAddress());
            
            XMPPServer.getInstance().getPacketRouter().route(packet);
        }
    }
    
//    /**
//     * Creates a new chat to "address" (sending a message)
//     * 
//     * @param octxt
//     * @param addressOtherParty user I am chatting with
//     * @param message
//     */
//    private String newChat(OperationContext octxt, IMAddr addressOtherParty, IMMessage message) throws ServiceException
//    {
//        String threadId = LdapUtil.generateUUID();
//
//        // add the other user as the first participant in this chat
//        IMChat.Participant part;
//        
//        // do we have a buddy for this chat?  If so, then use the buddy's info
//        IMBuddy buddy = mBuddyList.get(addressOtherParty);
//        
//        if (buddy != null)
//            part = new IMChat.Participant(buddy.getAddress(), null, buddy.getName());
//        else
//            part = new IMChat.Participant(addressOtherParty);
//        
//        IMChat toRet = new IMChat(getMailbox(), this, threadId, part);
//        
//        mChats.put(threadId, toRet);
//        
//        sendMessage(octxt, toRet, message);
//
//        return threadId;
//    }
    
    /**
     * Sends a message over an existing chat
     * 
     * @param address
     * @param message
     */
    public synchronized void sendMessage(OperationContext octxt, IMAddr toAddr, String threadId, IMMessage message) throws ServiceException 
    {
        IMChat chat = mChats.get(threadId);
        
        //
        // Find a chat with the right threadId, or find an open 1:1 chat with the target user
        // or create a new chat if necessary...
        //
        if (chat == null) {
            for (IMChat cur : mChats.values()) {
                // find an existing point-to-point chat with that user in it
                if (cur.participants().size() <=  2) {
                    if (cur.lookupParticipant(toAddr) != null) {
                        chat = cur;
                        break;
                    }
                }
            }
            if (chat == null) {
                //threadId = newChat(octxt, toAddr, message);
                threadId = toAddr.getAddr();
                
                // add the other user as the first participant in this chat
                IMChat.Participant part;
                
                // do we have a buddy for this chat?  If so, then use the buddy's info
                IMBuddy buddy = mBuddyList.get(toAddr);
                
                if (buddy != null)
                    part = new IMChat.Participant(buddy.getAddress(), null, buddy.getName());
                else
                    part = new IMChat.Participant(toAddr);
                
                chat = new IMChat(getMailbox(), this, threadId, part);
                
                mChats.put(threadId, chat);
            }
        }
        
        message.setFrom(mAddr);
        
        int seqNo = chat.addMessage(message);
        
        ArrayList<IMAddr> toList = new ArrayList<IMAddr>();
        for (Participant part : chat.participants()) {
            if (!part.getAddress().equals(mAddr))
                toList.add(part.getAddress());
        }
        
        IMMessageNotification notification = new IMMessageNotification(message.getFrom(), chat.getThreadId(), message, seqNo);
        postIMNotification(notification);
        
        for (IMAddr cur : toList) {
            Message xmppMsg = new Message();
            xmppMsg.setFrom(mAddr.makeJID());
            xmppMsg.setTo(cur.makeJID());
            
            if (message.getBody(Lang.DEFAULT) != null)
                xmppMsg.setBody(message.getBody(Lang.DEFAULT).getPlainText());
            
            if (message.getSubject(Lang.DEFAULT) != null)
                xmppMsg.setSubject(message.getSubject(Lang.DEFAULT).getPlainText());
            
            xmppMsg.setThread(chat.getThreadId());

            XMPPServer.getInstance().getMessageRouter().route(xmppMsg);
        }
    }
    
    public synchronized void closeChat(OperationContext octxt, IMChat chat) {
        chat.closeChat();
        
        // TODO: need to update the remote chat people if they are zimbra users
        mChats.remove(chat.getThreadId());
    }
    
    public synchronized void addUserToChat(IMChat chat, IMAddr addr) throws ServiceException {
        throw ServiceException.FAILURE("Unimplemented", null);
//        ArrayList<IMAddr> toList = new ArrayList<IMAddr>();
//        for (Participant part : chat.participants()) {
//            if (!part.getAddress().equals(mAddr))
//                toList.add(part.getAddress());
//        }
//        
//        // do we have a buddy for this new user?  If so, then use the buddy's info
//        IMBuddy buddy = mBuddyList.get(addr);
//        IMChat.Participant part;
//        
//        if (buddy != null)
//            part = new IMChat.Participant(buddy.getAddress(), null, buddy.getName());
//        else
//            part = new IMChat.Participant(addr);
//        
//        chat.addParticipant(part);
//        
//        IMEnteredChatEvent event = new IMEnteredChatEvent(mAddr, chat.getThreadId(), toList, addr);
//        IMRouter.getInstance().postEvent(event);
    }
    
    private static final String FN_ADDRESS     = "a";
    private static final String FN_PRESENCE    = "p";
    
    synchronized Metadata encodeAsMetatata()
    {
        Metadata meta = new Metadata();
        
        meta.put(FN_ADDRESS, mAddr);
        meta.put(FN_PRESENCE, mMyPresence.encodeAsMetadata());
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
            return toRet;
        }
        return new IMPersona(address);
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
            
            try {
                toRet = new IMChat(getMailbox(), this, thread, part);
                mChats.put(thread, toRet);
            } catch (ServiceException e) {
                ZimbraLog.im.warn("Caught Service Exception: " + e.toString(), e);
            }
        }
        return toRet;
    }
}
