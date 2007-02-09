/*
 * ***** BEGIN LICENSE BLOCK ***** Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 ("License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc. Portions created
 * by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc. All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.Arrays;
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
import org.jivesoftware.wildfire.group.Group;
import org.jivesoftware.wildfire.group.GroupManager;
import org.jivesoftware.wildfire.group.GroupNotFoundException;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Roster;
import org.xmpp.packet.IQ.Type;
import org.xmpp.packet.IQ;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.im.IMChat.Participant;
import com.zimbra.cs.im.IMMessage.Lang;
import com.zimbra.cs.im.IMPresence.Show;
import com.zimbra.cs.im.interop.Interop;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ClassLogger;
import com.zimbra.common.util.ZimbraLog;

/**
 * A single "persona" in the IM world
 */
public class IMPersona extends ClassLogger {
    private static final String FN_ADDRESS = "a";
    private static final String FN_PRESENCE = "p";

    /**
     * @param octxt
     * @param mbox
     * @param addr
     * @return
     * @throws ServiceException
     */
    static IMPersona loadPersona(OperationContext octxt, Mailbox mbox, IMAddr addr)
                throws ServiceException {
        IMPersona toRet = null;
        Metadata meta = mbox.getConfig(octxt, "im");
        if (meta != null) {
            // FIXME: how are config entries getting written w/o an ADDRESS
            // setting?
            String mdAddr = meta.get(FN_ADDRESS, null);
            if (mdAddr != null && mdAddr.equals(addr.getAddr())) {
                toRet = new IMPersona(addr, mbox);
                IMPresence presence = IMPresence.decodeMetadata(meta.getMap(FN_PRESENCE));
                toRet.mMyPresence = presence;
            }
        }
        if (toRet == null)
            toRet = new IMPersona(addr, mbox);
        toRet.init();
        return toRet;
    }

    private boolean mHaveInitialRoster = false;
    private IMAddr mAddr;
    private Map<String, IMChat> mChats = new HashMap<String, IMChat>();
    private int mCurChatId = 0;
    private Map<String, IMGroup> mGroups = new HashMap<String, IMGroup>();
    private boolean mIsOnline = false;
    private Set<Session> mListeners = new HashSet<Session>();
    private Mailbox mMailbox; // object used to lock the persona
    // these TWO parameters make up my presence - the first one is the presence
    // I have saved in the DB, and the second is a flag if I am online or
    // offline
    private IMPresence mMyPresence = new IMPresence(Show.ONLINE, (byte) 1, null);
    private HashSet<String> mSharedGroups = new HashSet<String>();
    private ClientSession mXMPPSession;

    private IMPersona(IMAddr addr, Mailbox lock) {
        super(ZimbraLog.im);
        assert (addr != null);
        mAddr = addr;
        mMailbox = lock;
    }

    /**
     * Active Sessions are tracked here so that we can use them to push
     * notifications to the client
     * 
     * @param session
     */
    public void addListener(Session session) {
        if (mListeners.size() == 0) {
            mIsOnline = true;
            try {
                pushMyPresence();
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }
        mListeners.add(session);
    }

    /**
     * @param octxt
     * @param address
     * @param name
     * @param groups
     * @throws ServiceException
     */
    public void addOutgoingSubscription(OperationContext octxt, IMAddr address,
                String name, String[] groups) throws ServiceException {
        // tell the server we want to add this to our roster
        Roster rosterPacket = new Roster(Type.set);
        rosterPacket.addItem(address.makeJID(), name, Roster.Ask.subscribe,
                    Roster.Subscription.to, Arrays.asList(groups));
        xmppRoute(rosterPacket);
        // tell the other user we want to subscribe
        Presence subscribePacket = new Presence(Presence.Type.subscribe);
        subscribePacket.setTo(address.makeJID());
        xmppRoute(subscribePacket);
    }

    public void addUserToChat(OperationContext octxt, IMChat chat, IMAddr addr,
                String invitationMessage) throws ServiceException {
        chat.addUserToChat(addr, invitationMessage);
    }

    /**
     * @param octxt
     * @param toAddress
     * @param authorized
     * @param add
     * @param name
     * @param groups
     * @throws ServiceException
     */
    public void authorizeSubscribe(OperationContext octxt, IMAddr toAddress,
                boolean authorized, boolean add, String name, String[] groups)
                throws ServiceException {
        if (authorized) {
            Presence subscribed = new Presence(Presence.Type.subscribed);
            subscribed.setTo(toAddress.makeJID());
            xmppRoute(subscribed);
        }
        if (add) {
            addOutgoingSubscription(octxt, toAddress, name, groups);
        }
    }

    /**
     * @return An unmodifiable collection of your IM Chats
     */
    public Iterable<IMChat> chats() {
        return new Iterable<IMChat>() {
            public Iterator<IMChat> iterator() {
                return (Collections.unmodifiableCollection(mChats.values())).iterator();
            }
        };
    }

    public void closeChat(OperationContext octxt, IMChat chat) {
        chat.closeChat();
        mChats.remove(chat.getThreadId());
    }

    /**
     * @param packet
     *        An incoming packet
     * @return an IMChat if the packet is destined to a multiuser chat we have
     *         going or NULL otherwise
     */
    private IMChat findTargetMUC(Packet packet) {
        String threadId = packet.getFrom().getNode();
        if (threadId != null && threadId.length() > 0) {
            IMChat chat = getChat(threadId);
            if (chat != null && chat.isMUC())
                return chat;
        }
        return null;
    }

    private void flush(OperationContext octxt) throws ServiceException {
        Mailbox mbox = getMailbox();
        assert (getAddr().getAddr().equals(mbox.getAccount().getName()));
        Metadata meta = new Metadata();
        meta.put(FN_ADDRESS, mAddr);
        meta.put(FN_PRESENCE, mMyPresence.encodeAsMetadata());
        mbox.setConfig(octxt, "im", meta);
    }

    @Override
    protected Object formatObject(Object o) {
        if (o instanceof org.xmpp.packet.Packet)
            return ((Packet) o).toXML();
        else
            return super.formatObject(o);
    }

    public void gatewayRegister(Interop.ServiceName type, String username, String password)
                throws ServiceException {
        try {
            Interop.connectUser(type, mAddr.makeFullJID(), username, password);
        } catch (Exception e) {
            throw ServiceException.FAILURE("Exception calling Interop.connectUser("
                        + username + "," + password, e);
        }
        // IQ iq = new IQ();
        // { // <iq>
        // iq.setFrom(mAddr.makeJID());
        // iq.setTo(getJIDForGateway(type));
        // iq.setType(Type.set);
        //        
        // org.dom4j.Element queryElt = iq.setChildElement("query",
        // "jabber:iq:register");
        // { // <query>
        // org.dom4j.Element usernameElt = queryElt.addElement("username");
        // usernameElt.setText(username);
        // org.dom4j.Element passwordElt = queryElt.addElement("password");
        // passwordElt.setText(password);
        // } // </query>
        // } // </iq>
        //        
        // xmppRoute(iq);
        // pushMyPresence();
        // return true;
    }

    public void gatewayUnRegister(Interop.ServiceName type) throws ServiceException {
        try {
            Interop.disconnectUser(Interop.ServiceName.msn, mAddr.makeFullJID());
        } catch (Exception e) {
            throw ServiceException.FAILURE("Exception calling Interop.disconnectUser()",
                        null);
        }
        // IQ iq = new IQ();
        // { // <iq>
        // iq.setFrom(mAddr.makeJID());
        // iq.setTo(getJIDForGateway(type));
        // iq.setType(Type.set);
        //        
        // org.dom4j.Element queryElt = iq.setChildElement("query",
        // "jabber:iq:register");
        // { // <query>
        // queryElt.addElement("remove");
        // } // </query>
        // } // </iq>
        //        
        // xmppRoute(iq);
        //        
        // return true;
    }

    public IMAddr getAddr() {
        return mAddr;
    }

    /**
     * @return The set of gateways this user has access to
     */
    public List<Interop.ServiceName> getAvailableGateways() {
        return Interop.getAvailableServices();
    }

    /**
     * @param create
     *        if TRUE then will create an empty chat if none found
     * @param thread
     * @param fromAddress
     * @return
     */
    private IMChat getChat(boolean create, String thread, IMAddr fromAddress) {
        IMChat toRet = mChats.get(thread);
        if (toRet == null && create) {
            Participant part;
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

    /**
     * Returns NULL if no chat found
     * 
     * @param create
     * @param fromAddress
     * @return
     */
    public IMChat getChat(String threadId) {
        return getChat(false, threadId, null);
    }

    public IMPresence getEffectivePresence() {
        if (mIsOnline)
            return mMyPresence;
        else
            return new IMPresence(Show.OFFLINE, mMyPresence.getPriority(), mMyPresence
                        .getStatus());
    }

    public String getFullJidAsString() {
        return mAddr + "/zcs";
    }

    /**
     * Finds an existing group
     * 
     * @param create
     *        if TRUE will create the group if one doesn't exist
     * @param name
     * @return
     */
    private IMGroup getGroup(boolean create, String name) {
        IMGroup toRet = mGroups.get(name);
        if (toRet == null && create) {
            toRet = new IMGroup(name);
            mGroups.put(name, toRet);
        }
        return toRet;
    }

    @Override
    protected String getInstanceInfo() {
        return toString();
    }

    public Object getLock() {
        return mMailbox;
    }

    private Mailbox getMailbox() throws ServiceException {
        return mMailbox;
    }

    String getMucDomain() throws ServiceException {
        return "conference." + mMailbox.getAccount().getDomainName();
    }

    public String getResource() {
        return "zcs";
    }

    public void getRoster(OperationContext octxt) throws ServiceException {
        if (mHaveInitialRoster) {
            Roster rosterPacket = new Roster(Type.get);
            xmppRoute(rosterPacket);
        }
    }

    /**
     * @return An unmodifiable collection of your IM Groups
     */
    public Iterable<IMGroup> groups() {
        return new Iterable<IMGroup>() {
            public Iterator<IMGroup> iterator() {
                return (Collections.unmodifiableCollection(mGroups.values())).iterator();
            }
        };
    }

    void handleIQPacket(boolean toMe, IQ iq) {
        // is it a MUC iq?
        IMChat chat = findTargetMUC(iq);
        if (chat != null)
            chat.handleIQPacket(iq);
    }

    private void handleMessagePacket(boolean toMe, Message msg) {
        // either TO or FROM, depending which one isn't "me"
        JID remoteJID = (toMe ? msg.getFrom() : msg.getTo());
        // Step 1: find the appropriate chat
        IMChat chat = findTargetMUC(msg);
        if (chat == null) {
            String threadId = msg.getThread();
            if (threadId == null || threadId.length() == 0) {
                // if possible, find an existing point-to-point chat with that
                // user in it
                for (IMChat cur : mChats.values()) {
                    if ((cur.participants().size() <= 2)
                                && (cur.getParticipant(new IMAddr(remoteJID)) != null)) {
                        threadId = cur.getThreadId();
                        break;
                    }
                }
                // as a final try, just use the remove addr of this message
                if (threadId == null || threadId.length() == 0) {
                    threadId = (toMe ? msg.getFrom() : msg.getTo()).getNode();
                }
            }
            chat = getChat(true, threadId, new IMAddr(remoteJID));
        }
        chat.handleMessagePacket(toMe, msg);
    }

    private void handlePresencePacket(boolean toMe, Presence pres) {
        // is this presence RE a MUC chatroom?
        IMChat chat = findTargetMUC(pres);
        if (chat != null) {
            chat.handlePresencePacket(toMe, pres);
        } else {
            if (pres.getChildElement("x", "http://jabber.org/protocol/muc#user") != null
                        || pres.getChildElement("x", "http://jabber.org/protocol/muc") != null) {
                info("Got MUC presence update but couldn't find Chat");
                return;
            }
            Presence.Type ptype = pres.getType();
            if (ptype == null) {
                Presence.Show pShow = pres.getShow();
                IMPresence.Show imShow = IMPresence.Show.ONLINE;
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
                onRemotePresenceChange(pres, imShow);
            } else {
                Presence reply = null;
                switch (ptype) {
                    case unavailable:
                        onRemotePresenceChange(pres, Show.OFFLINE);
                    break;
                    case subscribe: {
                        IMSubscribeNotification notify = new IMSubscribeNotification(
                                    IMAddr.fromJID(pres.getFrom()));
                        postIMNotification(notify);
                        if (true) { // TODO REMOVETHIS!
                            // auto-accept for now
                            reply = new Presence();
                            reply.setType(Presence.Type.subscribed);
                            reply.setTo(pres.getFrom());
                            xmppRoute(reply);
                        }
                    }
                    break;
                    case subscribed: {
                        debug("Presence.subscribed: " + pres.toString());
                        //IMAddr address = IMAddr.fromJID(pres.getFrom());
                        // // it could potentially have been deleted, so
                        // re-create it if necessary
                        // IMBuddy buddy = getBuddy(true, address,
                        // address.toString());
                        // buddy.setAsk(null);
                        // SubType st = buddy.getSubType();
                        // if (!st.isOutgoing())
                        // buddy.setSubType(st.setOutgoing());
                        // try {
                        // postIMNotification(IMSubscribedNotification.create(address,
                        // address.toString(), true, null));
                        // postIMNotification(new
                        // IMPresenceUpdateNotification(address,
                        // buddy.getPresence()));
                        // } catch(ServiceException ex) {
                        // ZimbraLog.im.warn("Caught Exception: " +
                        // ex.toString(), ex);
                        // }
                    }
                    break;
                    case unsubscribe: // remote user is unsubscribing from us
                        debug("Presence.unsubscribe: %s", pres);
                        // auto-accept
                        reply = new Presence();
                        reply.setTo(pres.getFrom());
                        reply.setFrom(pres.getTo());
                        reply.setType(Presence.Type.unsubscribed);
                        xmppRoute(reply);
                    break;
                    case unsubscribed: // you've unsubscribed
                    {
                        debug("Presence.unsubscribed: %s", pres);
                        IMAddr address = IMAddr.fromJID(pres.getFrom());
                        postIMNotification(IMSubscribedNotification.create(address,
                                    address.toString(), false, null));
                    }
                    break;
                    case probe:
                        debug("Presence.probe: %s", pres);
                        try {
                            pushMyPresence(pres.getFrom());
                        } catch (ServiceException ex) {}
                    break;
                    case error:
                        info("Presence.error: %s", pres);
                    break;
                }
            }
        }
    }

    private void handleRosterPacket(boolean toMe, Roster roster) {
        IMRosterNotification rosterNot = null;
        boolean doProbe = false;
        
        switch (roster.getType()) {
            case result:
                rosterNot = new IMRosterNotification();
                if (mHaveInitialRoster) 
                    doProbe = true;
                mHaveInitialRoster = true;
                // mBuddyList.clear();
                // fall through!
            case set:
                for (Roster.Item item : roster.getItems()) {
                    IMAddr buddyAddr = IMAddr.fromJID(item.getJID());
                    Roster.Subscription subscript = item.getSubscription();
                    IMSubscribedNotification not = IMSubscribedNotification
                                .create(
                                            buddyAddr,
                                            item.getName(),
                                            item.getGroups(),
                                            (subscript == Roster.Subscription.both || subscript == Roster.Subscription.to),
                                            item.getAsk());
                    if (rosterNot != null) {
                        rosterNot.addEntry(not);
                    } else {
                        postIMNotification(not);
                    }
                    if (doProbe) {
                        if (item.getJID().getNode() != null) {
                            if (!Interop.isInteropJid(item.getJID())) {
                                Presence probe = new Presence(Presence.Type.probe);
                                probe.setTo(item.getJID());
                                xmppRoute(probe);
                            }
                        }
                    }
                }
                if (doProbe)
                    Interop.refreshPresence(mAddr.makeFullJID());
                if (rosterNot != null) {
                    postIMNotification(rosterNot);
                }
            break;
            default:
                debug("Ignoring Roster packet of type %s", roster.getType());
        }
    }

    /**
     * Called after State has been loaded from stable storage
     */
    private void init() {
        try {
            if (Provisioning.getInstance().getLocalServer().getBooleanAttr(
                        Provisioning.A_zimbraXMPPEnabled, false)) {
                mXMPPSession = new ClientSession(Provisioning.getInstance()
                            .getLocalServer().getName(), new FakeClientConnection(this),
                            XMPPServer.getInstance().getSessionManager().nextStreamID());
                AuthToken at = new AuthToken(mAddr.getAddr());
                mXMPPSession.setAuthToken(at);
                try {
                    mXMPPSession.setAuthToken(at, XMPPServer.getInstance()
                                .getUserManager(), this.getResource());
                } catch (UserNotFoundException ex) {
                    System.out.println(ex.toString());
                    ex.printStackTrace();
                }
                // have to request roster immediately -- as soon as we send our
                // first presence update, we will start
                // receiving presence notifications from our buddies -- and if
                // we don't have the roster loaded, then we
                // will not know what to do with them...
                Roster rosterRequest = new Roster();
                xmppRoute(rosterRequest);
                
            }
        } catch (ServiceException ex) {
            ZimbraLog.im.warn("Caught Exception checking if XMPP enabled "
                        + ex.toString(), ex);
        }
    }

    public boolean inSharedGroup(String name) {
        return mSharedGroups.contains(name);
    }

    public void joinChat(String threadId) throws ServiceException {
        IMChat chat = mChats.get(threadId);
        if (chat == null) {
            chat = new IMChat(getMailbox(), this, threadId, null);
            mChats.put(threadId, chat);
        }
        chat.joinMUCChat(threadId);
    }

    public void joinSharedGroup(String name) throws ServiceException {
        try {
            Group group = GroupManager.getInstance().getGroup(name);
            group.getAdmins().add(mAddr.makeJID());
        } catch (GroupNotFoundException ex) {}
    }

    public void leaveSharedGroup(String name) throws ServiceException {
        try {
            Group group = GroupManager.getInstance().getGroup(name);
            group.getAdmins().remove(mAddr.makeJID());
        } catch (GroupNotFoundException ex) {}
    }

    private void onRemotePresenceChange(Presence pres, Show imShow) {
        // presence update
        IMAddr fromAddr = IMAddr.fromJID(pres.getFrom());
        int prio = pres.getPriority();
        IMPresence newPresence = new IMPresence(imShow, (byte) prio, pres.getStatus());
        IMPresenceUpdateNotification event = new IMPresenceUpdateNotification(fromAddr,
                    newPresence);
        postIMNotification(event);
    }

    void postIMNotification(IMNotification not) {
        for (Session session : mListeners)
            session.notifyIM(not);
    }

    /**
     * callback from the Router thread
     */
    void process(Packet packet) {
        // because we are receiving packets from the PacketInterceptor as well
        // as the session, we need to differentiate the outgoing from the
        // incoming packets
        boolean toMe = true;
        if (packet.getTo() != null
                    && !packet.getTo().toBareJID().equals(this.getAddr().getAddr())) {
            toMe = false;
        }
        debug("processing %s packet %s", toMe ? "INCOMING" : "OUTGOING", packet);
        if (packet instanceof Message) {
            handleMessagePacket(toMe, (Message) packet);
        } else if (packet instanceof Presence) {
            handlePresencePacket(toMe, (Presence) packet);
        } else if (packet instanceof Roster) {
            handleRosterPacket(toMe, (Roster) packet);
        } else if (packet instanceof IQ) {
            handleIQPacket(toMe, (IQ) packet);
        }
    }

    public void providerGroupAdd(String name) throws ServiceException {
        if (mSharedGroups.add(name))
            flush(null);
    }

    public void providerGroupRemove(String name) throws ServiceException {
        if (mSharedGroups.remove(name))
            flush(null);
    }

    private void pushMyPresence() throws ServiceException {
        pushMyPresence(null);
    }

    private void pushMyPresence(JID sendTo) throws ServiceException {
        IMPresence presence = getEffectivePresence();
        if (sendTo == null) {
            // send it to my client in case there I have other sessions
            // listening...
            IMPresenceUpdateNotification event = new IMPresenceUpdateNotification(mAddr,
                        presence);
            postIMNotification(event);
        }
        updateXMPPPresence(sendTo, presence);
    }

    /**
     * Active Sessions are tracked here so that we can use them to push
     * notifications to the client
     * 
     * @param session
     */
    public void removeListener(Session session) {
        mListeners.remove(session);
        if (mListeners.size() == 0) {
            mIsOnline = false;
            try {
                pushMyPresence();
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param octxt
     * @param address
     * @param name
     * @param groups
     * @throws ServiceException
     */
    public void removeOutgoingSubscription(OperationContext octxt, IMAddr address,
                String name, String[] groups) throws ServiceException {
        // tell the server to change our roster
        Roster rosterPacket = new Roster(Type.set);
        rosterPacket.addItem(address.makeJID(), name, Roster.Ask.unsubscribe,
                    Roster.Subscription.none, Arrays.asList(groups));
        xmppRoute(rosterPacket);
        // tell the other user we want to unsubscribe
        Presence unsubscribePacket = new Presence(Presence.Type.unsubscribe);
        unsubscribePacket.setTo(address.makeJID());
        xmppRoute(unsubscribePacket);
        postIMNotification(IMSubscribedNotification.create(address, name, groups, false,
                    Roster.Ask.unsubscribe));
    }

    /**
     * Sends a message over an existing chat
     * 
     * @param address
     * @param message
     */
    public void sendMessage(OperationContext octxt, IMAddr toAddr, String threadId,
                IMMessage message) throws ServiceException {
        //
        // Find a chat with the right threadId, or find an open 1:1 chat with
        // the target user
        // or create a new chat if necessary...
        IMChat chat = mChats.get(threadId);
        if (chat == null) {
            for (IMChat cur : mChats.values()) {
                // find an existing point-to-point chat with that user in it
                if (cur.participants().size() <= 2) {
                    if (cur.getParticipant(toAddr) != null) {
                        chat = cur;
                        break;
                    }
                }
            }
            if (chat == null) {
                // threadId = newChat(octxt, toAddr, message);
                // threadId = toAddr.getAddr();
                threadId = "chat-" + this.mAddr.getNode() + "-" + mCurChatId;
                mCurChatId++;
                // add the other user as the first participant in this chat
                IMChat.Participant part;
                part = new IMChat.Participant(toAddr);
                chat = new IMChat(getMailbox(), this, threadId, part);
                mChats.put(threadId, chat);
            }
        }
        String msg = message.getBody(Lang.DEFAULT).getPlainText();
        if (msg != null && msg.startsWith("/")) {
            if (msg.startsWith("/add ")) {
                String username = msg.substring("/add ".length()).trim();
                IMAddr newUser = new IMAddr(username);
                ZimbraLog.im
                            .info("Adding user: \"" + username + "\" to chat " + threadId);
                addUserToChat(null, chat, newUser, "Please join my chat");
                return;
            } else if (msg.startsWith("/join")) {
                String[] words = msg.split("\\s+");
                debug("Trying to join groupchat: %s", words[1]);
                chat.joinMUCChat(words[1]);
                return;
            } else if (msg.startsWith("/info")) {
                String info = chat.toString();
                message.addBody(new IMMessage.TextPart(info));
                IMMessageNotification notification = new IMMessageNotification(
                            new IMAddr("SYSTEM"), chat.getThreadId(), message, 0);
                postIMNotification(notification);
                return;
            } else if (msg.startsWith("/join_group")) {
                String[] words = msg.split("\\s+");
                debug("Trying to join shared group: %s", words[1]);
                joinSharedGroup(words[1]);
                return;
            } else if (msg.startsWith("/leave_group")) {
                String[] words = msg.split("\\s+");
                debug("Trying to leave shared group: %s", words[1]);
                leaveSharedGroup(words[1]);
                return;
            }
        }
        chat.sendMessage(octxt, toAddr, threadId, message);
    }

    public void setMyPresence(OperationContext octxt, IMPresence presence)
                throws ServiceException {
        // TODO optimize out change-to-same eventually (leave for now, very
        // convienent as is)
        mMyPresence = presence;
        flush(octxt);
        pushMyPresence();
    }

    @Override
    public String toString() {
        return new Formatter().format("IMPersona(%s  Presence:%s)", mAddr, mMyPresence)
                    .toString();
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
            if (sendTo == null) {
                for (IMChat chat : mChats.values()) {
                    chat.sendPresenceUpdate(xmppPresence);
                }
            } else {
                xmppPresence.setTo(sendTo);
            }
            xmppRoute(xmppPresence);
        }
    }

    void xmppRoute(Packet packet) {
        if (mXMPPSession != null) {
            ZimbraLog.im.debug("SENDING XMPP PACKET: " + packet.toXML());
            packet.setFrom(mXMPPSession.getAddress());
            IMRouter.getInstance().postEvent(packet);
        }
    }
}
