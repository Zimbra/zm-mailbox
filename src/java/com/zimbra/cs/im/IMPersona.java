/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.AuthToken;
import org.jivesoftware.wildfire.group.Group;
import org.jivesoftware.wildfire.group.GroupManager;
import org.jivesoftware.wildfire.group.GroupNotFoundException;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.xmpp.component.ComponentException;
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
import com.zimbra.cs.im.interop.Interop.ServiceName;
import com.zimbra.cs.im.interop.Interop.UserStatus;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ClassLogger;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;

/**
 * A single "persona" in the IM world
 */
public class IMPersona extends ClassLogger {
    private static final String FN_ADDRESS = "a";
    private static final String FN_PRESENCE = "p";
    private static final String FN_INTEROP_SERVICE_PREFIX  = "isvc-";

    /**
     * @param octxt
     * @param mbox
     * @param addr
     * @return
     * @throws ServiceException
     */
    public static IMPersona loadPersona(Mailbox mbox)
                throws ServiceException {
        IMPersona toRet = null;
        Metadata meta = mbox.getConfig(null, "im");
        IMAddr addr = new IMAddr(mbox.getAccount().getName());
        
        HashMap<String /*ServiceName*/, Map<String, String>> interopReg = new HashMap<String/*ServiceName*/, Map<String, String>>();
        
        if (meta != null) {
            // FIXME: how are config entries getting written w/o an ADDRESS
            // setting?
            String mdAddr = meta.get(FN_ADDRESS, null);
            if (mdAddr != null && mdAddr.equals(addr.getAddr())) {
                toRet = new IMPersona(mbox, addr);
                IMPresence presence = IMPresence.decodeMetadata(meta.getMap(FN_PRESENCE));
                toRet.mMyPresence = presence;
            }

            // iterate through the metadata, looking for entries with "isvc-" keys,
            // those are interop registration data blobs
            for (Object o : meta.asMap().entrySet()) {
                Map.Entry<String, Object> e = (Map.Entry<String, Object>)o;
                if (e.getKey().startsWith(FN_INTEROP_SERVICE_PREFIX)) {
                    Map<String, String> svcData = new HashMap<String,String>();

                    // this is a nested metadata -- iterate it, and put all the 
                    // values into a <String,String> hash
                    Metadata tmp = (Metadata)e.getValue();
                    for (Object tmpo: tmp.asMap().entrySet()) {
                        Map.Entry tmpEntry = (Map.Entry)tmpo;
                        svcData.put((String)tmpEntry.getKey(), (String)tmpEntry.getValue());
                    }
                    
                    // put the <String,String> hash into interopReg 
                    String svcName = e.getKey().substring(FN_INTEROP_SERVICE_PREFIX.length());
                    interopReg.put(svcName, svcData);
                }
            }
        }
        if (toRet == null)
            toRet = new IMPersona(mbox, addr);
        toRet.mInteropRegistrationData = interopReg;        
        return toRet;
    }

    private Mailbox mMailbox = null;
    private boolean mHaveInitialRoster = false;
    private String mDefaultPrivacyListName = null;
    private Map<IMAddr, IMSubscribedNotification> mRoster = new HashMap<IMAddr, IMSubscribedNotification>();
    private Map<IMAddr, IMSubscribeNotification> mPendingSubscribes = new HashMap<IMAddr, IMSubscribeNotification>();
    private Map<IMAddr, IMPresence> mBufferedPresence = new HashMap<IMAddr, IMPresence>();
    private IMAddr mAddr;
    private Map<String, IMChat> mChats = new HashMap<String, IMChat>();
    private int mCurChatId = 0;
    private int mCurRequestId = 0;
    private Map<String, IMGroup> mGroups = new HashMap<String, IMGroup>();
    private Set<Session> mListeners = new HashSet<Session>();
    
    // these TWO parameters make up my presence - the first one is the presence
    // I have saved in the DB, and the second is a flag if I am online or
    // offline
    private IMPresence mMyPresence = new IMPresence(Show.ONLINE, (byte) 1, null);
    private boolean mIsOnline = false;
//    private HashSet<String> mSharedGroups = new HashSet<String>();
    private ClientSession mXMPPSession;
    private HashMap<String /*ServiceName*/, Map<String, String>> mInteropRegistrationData;

    private IMPersona(Mailbox mbox, IMAddr addr) {
        super(ZimbraLog.im);
        assert (addr != null);
        mAddr = addr;
        mMailbox = mbox;
        ZimbraLog.im.info("Creating IMPersona "+toString()+" at addr "+System.identityHashCode(this)+" mbox at addr "+System.identityHashCode(mbox));
    }

    /**
     * Active Sessions are tracked here so that we can use them to push
     * notifications to the client
     * 
     * @param session
     */
    public void addListener(Session session) throws ServiceException {
        synchronized(getLock()) {
            if (mListeners.size() == 0) {
                mIsOnline = true;
                connectToIMServer();
                try {
                    pushMyPresence();
                } catch (ServiceException e) {
                    e.printStackTrace();
                }
            }
            mListeners.add(session);
        }
    }
    
    /**
     * Active Sessions are tracked here so that we can use them to push
     * notifications to the client
     * 
     * @param session
     */
    public void removeListener(Session session) {
        synchronized(getLock()) {
            mListeners.remove(session);
            if (mListeners.size() == 0) {
                mIsOnline = false;
                try {
                    pushMyPresence();
                } catch (ServiceException e) {
                    e.printStackTrace();
                }
                disconnectFromIMServer();
            }
        }
    }

    /**
     * Mailbox is going into Maintenance mode, shut down the persona
     */
    public void purgeListeners() {
        synchronized(getLock()) {
            List<Session> toRemove = new ArrayList<Session>();
            toRemove.addAll(mListeners);
            for (Session s : toRemove) {
                removeListener(s);
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
    public void addOutgoingSubscription(OperationContext octxt, IMAddr address,
        String name, String[] groups) throws ServiceException {
        synchronized(getLock()) {
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
    }

    public void addUserToChat(OperationContext octxt, IMChat chat, IMAddr addr,
                String invitationMessage) throws ServiceException {
        synchronized(getLock()) {
            chat.addUserToChat(addr, invitationMessage);
        }
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
        synchronized(getLock()) {
            mPendingSubscribes.remove(toAddress);
            if (authorized) {
                Presence subscribed = new Presence(Presence.Type.subscribed);
                subscribed.setTo(toAddress.makeJID());
                xmppRoute(subscribed);
            }
            if (add) {
                addOutgoingSubscription(octxt, toAddress, name, groups);
            }
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
        synchronized(getLock()) {
            chat.closeChat();
            mChats.remove(chat.getThreadId());
        }
    }
    
    public void refreshChats(Session s) {
        synchronized(getLock()) {
            for (IMChat chat : mChats.values()) {
                int seqNo = chat.getFirstSeqNo();
                for (IMMessage msg : chat.messages()) {
                    IMMessageNotification not = new IMMessageNotification(msg.getFrom(), chat.getThreadId(), msg, seqNo);
                    postIMNotification(not, s); 
                    seqNo++;
                }
            }
        }
    }

    /**
     * @param packet
     *        An incoming packet
     * @return an IMChat if the packet is destined to a multiuser chat we have
     *         going or NULL otherwise
     */
    private IMChat findTargetMUC(Packet packet) {
        if (packet.getFrom() != null) {
            String threadId = packet.getFrom().getNode();
//          String threadId = packet.getFrom().toBareJID();
            if (threadId != null && threadId.length() > 0) {
                IMChat chat = getChat(threadId);
                if (chat != null && chat.isMUC())
                    return chat;
            }
        }
        return null;
    }

    /** Write this persona's metadata to persistent storage */
    private void flush(OperationContext octxt) throws ServiceException {
        Mailbox mbox = getMailbox();
        assert (getAddr().getAddr().equals(mbox.getAccount().getName()));
        Metadata meta = new Metadata();
        meta.put(FN_ADDRESS, mAddr);
        meta.put(FN_PRESENCE, mMyPresence.encodeAsMetadata());
        
        for (Map.Entry<String, Map<String, String>> svc : mInteropRegistrationData.entrySet()) {
            Metadata interopData = new Metadata();
            for (Map.Entry<String, String> entry : svc.getValue().entrySet()) {
                interopData.put(entry.getKey(), entry.getValue());
            }
            meta.put(FN_INTEROP_SERVICE_PREFIX+svc.getKey(), interopData);
        }
        
        mbox.setConfig(octxt, "im", meta);
    }

    @Override
    protected Object formatObject(Object o) {
        if (o instanceof org.xmpp.packet.Packet)
            return ((Packet) o).toXML();
        else
            return super.formatObject(o);
    }
    
    public void gatewayReconnect(Interop.ServiceName type) throws ServiceException {
        synchronized(getLock()) {
            try {
                Interop.getInstance().reconnectUser(type, mAddr.makeFullJID(getResource()));            
            } catch (Exception e) {
                throw ServiceException.FAILURE("Exception calling gatewayReconnect("+type.name(), e);
            }
        }
    }
    
    public void gatewayRegister(Interop.ServiceName type, String username, String password)
                throws ServiceException {
        
        synchronized(getLock()) {
            try {
                Interop.getInstance().connectUser(type, mAddr.makeFullJID(getResource()), username, password);
            } catch (com.zimbra.cs.im.interop.AlreadyConnectedComponentException ace) {
                try {
                    Interop.getInstance().disconnectUser(type, mAddr.makeFullJID(getResource()));
                    Interop.getInstance().connectUser(type, mAddr.makeFullJID(getResource()), username, password);
                } catch (Exception e) {
                    throw ServiceException.FAILURE("Exception calling Interop.connectUser("
                        + username + "," + password, e);
                }
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
    }

    public void gatewayUnRegister(Interop.ServiceName type) throws ServiceException {
        synchronized(getLock()) {
            try {
                Interop.getInstance().disconnectUser(type, mAddr.makeFullJID(getResource()));
            } catch (Exception e) {
                throw ServiceException.FAILURE("Exception calling Interop.disconnectUser()", e);
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
    }

    public IMAddr getAddr() {
        return mAddr;
    }
    
    public String getDomain() {
        return mAddr.getDomain();
    }
    
    /**
     * @return The set of gateways this user has access to
     */
    public List<Pair<ServiceName, UserStatus>> getAvailableGateways() {
        synchronized(getLock()) {
            List<Pair<ServiceName, UserStatus>>  ret = new LinkedList<Pair<ServiceName, UserStatus>>();     
            List<ServiceName> services = Interop.getInstance().getAvailableServices();
            for (ServiceName service : services) {
                try {
                    ret.add(new Pair<ServiceName, UserStatus>(service, Interop.getInstance().getRegistrationStatus(service, mAddr.makeFullJID(getResource()))));
                } catch (ComponentException ex) {
                    debug("Caught component exception trying to get registration status: ", ex);
                }
            }
            
            return ret;
        }
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
            if (thread == null)
                throw new IllegalArgumentException("Cannot create a chat with a NULL threadId");
            toRet = new IMChat(this, thread, part);
            mChats.put(thread, toRet);
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
        synchronized(getLock()) {
            return getChat(false, threadId, null);
        }
    }

    public IMPresence getEffectivePresence() {
        synchronized(getLock()) {
            if (mIsOnline)
                return mMyPresence;
            else
                return new IMPresence(Show.OFFLINE, mMyPresence.getPriority(), mMyPresence.getStatus());
        }
    }

    public String getFullJidAsString() {
        return mAddr + "/" + getResource();
    }

//    /**
//     * Finds an existing group
//     * 
//     * @param create
//     *        if TRUE will create the group if one doesn't exist
//     * @param name
//     * @return
//     */
//    private IMGroup getGroup(boolean create, String name) {
//        IMGroup toRet = mGroups.get(name);
//        if (toRet == null && create) {
//            toRet = new IMGroup(name);
//            mGroups.put(name, toRet);
//        }
//        return toRet;
//    }
    
    Map<String, String> getIMGatewayRegistration(Interop.ServiceName service) throws ServiceException {
        synchronized(getLock()) {
            return mInteropRegistrationData.get(service.name());
        }
    }
    
    void setIMGatewayRegistration(Interop.ServiceName service, Map<String, String> data) throws ServiceException {
        synchronized(getLock()) {
            mInteropRegistrationData.put(service.name(), data);
            flush(null);
        }
    }
    
    void removeIMGatewayRegistration(Interop.ServiceName service) throws ServiceException {
        synchronized(getLock()) {
            mInteropRegistrationData.remove(service.name());
            flush(null);
        }
    }

    @Override
    protected String getInstanceInfo() {
        return toString();
    }

    public Object getLock() {
        return mMailbox;
    }

    Mailbox getMailbox() throws ServiceException {
        return mMailbox;
    }

    String getMucDomain() throws ServiceException {
        return "conference." + getMailbox().getAccount().getDomainName();
    }

    public String getResource() {
        return "zcs";
    }

//    public void getRoster(OperationContext octxt) throws ServiceException {
//        if (mHaveInitialRoster) {
//            Roster rosterPacket = new Roster(Type.get);
//            xmppRoute(rosterPacket);
//        }
//    }
    
    public void refreshRoster(Session s)  {
        synchronized(getLock()) {
            if (mHaveInitialRoster) {
                // roster
                IMRosterNotification rosterNot = new IMRosterNotification();
                for (IMSubscribedNotification not: mRoster.values()) {
                    rosterNot.addEntry(not);
                }
                postIMNotification(rosterNot, s);
                
                // presence
                for (Map.Entry<IMAddr, IMPresence> entry : mBufferedPresence.entrySet()) {
                    IMPresenceUpdateNotification not= new IMPresenceUpdateNotification(entry.getKey(), entry.getValue());
                    postIMNotification(not, s);
                }
                
                // unanswered subscribe requests
                for (IMSubscribeNotification not : mPendingSubscribes.values()) {
                    postIMNotification(not, s);
                }
            }  
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
        boolean handled = false;
        
        switch (iq.getType()) {
            case error:
                ZimbraLog.im.debug("Ignoring IQ error packet: "+iq);
                return;
            case result:
            {
                org.dom4j.Element child = iq.getChildElement();
                if (child != null) {
                    if ("query".equals(child.getName())) {
                        if ("jabber:iq:privacy".equals(child.getNamespaceURI())) {
                            handlePrivacyResult(iq);
                            handled = true;
                        }
                    }
                }
                if (!handled)
                    ZimbraLog.im.debug("Ignoring IQ result packet: "+iq);
                return;
            }
            case set:
            {
                org.dom4j.Element child = iq.getChildElement();
                if (child != null && "query".equals(child.getName())) {
                    if ("jabber:iq:privacy".equals(child.getNamespaceURI())) {
                        handlePrivacySet(iq);
                        handled = true;
                    } else {
                        info("Ignorig unknown IQ set: "+iq.toString());
                    }
                }
                
                // respond to the SET
                IQ result= new IQ();
                result.setType(Type.result);
                result.setID(iq.getID());
                xmppRoute(result);
            }
            break;
            default:
            {
            }
        }
        
        if (!handled) {
            // is it a MUC iq?
            IMChat chat = findTargetMUC(iq);
            if (chat != null)
                chat.handleIQPacket(iq);
        }
    }

    private void handlePrivacySet(IQ iq) {
        // request default privacy list
        this.getDefaultPrivacyList();
    }
    
    private void handlePrivacyResult(IQ iq) {
        org.dom4j.Element child = iq.getChildElement();        
        ZimbraLog.im.debug("Received Privacy List Packet: "+iq);
        String idParts[] = iq.getID().split("-");
        boolean handled = false; 
        if (idParts.length > 0) {
            if ("getPrivLists".equals(idParts[0])) { 
                //
                // we got a response to our request for the LIST of privacy lists
                //
                ZimbraLog.im.debug("It's a list of our privacy lists!");
                mDefaultPrivacyListName = null;
                
                // find the default list
                org.dom4j.Element defaultList = child.element("default");
                if (defaultList != null) {
                    String defaultListName = defaultList.attributeValue("name", null);
                    if (defaultListName != null) {
                        handled = true;
                        mDefaultPrivacyListName = defaultListName;
                        requestPrivacyList(defaultListName);
                    }
                }
                if (!handled) {
                    handled = true;
                    // no default list: inform the client
                    IMPrivacyListNotification not = new IMPrivacyListNotification(new PrivacyList("default"));
                    postIMNotification(not);
                }
            } else if ("getDefaultPrivList".equals(idParts[0])) {
                //
                // we got a response to our request for the DEFAULT privacy list
                //
                ZimbraLog.im.debug("Received default privacy list: "+iq);
                handled = true;
                
                /*
                <list name='public'>
                  <item type='jid'
                      value='tybalt@example.com'
                      action='deny'
                      order='1'/>
                  <item action='allow' order='2'/>
                </list>
                 */

                // find the list
                org.dom4j.Element list = child.element("list");
                if (list != null) {
                    String name = list.attributeValue("name", null);
                    if (name != null) {
                        PrivacyList pl = new PrivacyList(name);
                        
                        for (Iterator<org.dom4j.Element> iter = (Iterator<org.dom4j.Element>)list.elementIterator("item");iter.hasNext();) {
                            org.dom4j.Element item = iter.next();
                            
                            String type = item.attributeValue("type", "jid");
                            String value = item.attributeValue("value", null);
                            String actionStr = item.attributeValue("action", "deny");
                            String order = item.attributeValue("order", null);
                                                        
                            if (value != null && order != null && "jid".equals(type)) {
                                int orderInt = Integer.parseInt(order);
                                
                                PrivacyListEntry.Action action = PrivacyListEntry.Action.valueOf(actionStr);

                                byte blockType = 0;
                                if (item.element("message") != null) 
                                    blockType |= PrivacyListEntry.BLOCK_MESSAGES;
                                if (item.element("presence-in") != null) 
                                    blockType |= PrivacyListEntry.BLOCK_PRESENCE_IN;
                                if (item.element("presence-out") != null) 
                                    blockType |= PrivacyListEntry.BLOCK_PRESENCE_OUT;
                                if (item.element("iq") != null) 
                                    blockType |= PrivacyListEntry.BLOCK_IQ;
                                if (blockType == 0)
                                    blockType = PrivacyListEntry.BLOCK_ALL;
                                
                                PrivacyListEntry entry = new PrivacyListEntry(new IMAddr(value), orderInt, action, blockType);
                                
                                try {
                                    pl.addEntry(entry);
                                } catch (PrivacyList.DuplicateOrderException e) {
                                    ZimbraLog.im.warn("Received an invalid PrivacyList from server: order was non-unique.  Ignoring: %s", item);
                                }
                            }
                        }
                        IMPrivacyListNotification not = new IMPrivacyListNotification(pl);
                        postIMNotification(not);
                    } // if name!=null
                }
            } else {
                handled = true;
//                if (false) {
//                    
//                    // some other privacy list update -- re-request our list of lists!
//                    // request list of privacy lists
//                    IQ requestList = new IQ();
//                    requestList.setType(Type.get);
//                    requestList.setID("getPrivLists-"+(mCurRequestId++));
//                    org.dom4j.Element query = requestList.setChildElement("query", "jabber:iq:privacy");
//                    xmppRoute(requestList);
//                }
            }
        } 
            
        if (!handled)
            ZimbraLog.im.debug("Ignoring unknown privacy IQ response: "+iq);
    }
    

    private void handleMessagePacket(boolean toMe, Message msg) {
        // is it a gateway notification?  If so, then stop processing it here
        Element xe = msg.getChildElement("x", "zimbra:interop");
        if (xe != null) {
            String username = null;
            Element usernameElt = xe.element("username");
            if (usernameElt != null) {
                username = usernameElt.getText();
            }
            
            String serviceName = msg.getFrom().toBareJID();
            String[] splits = serviceName.split("\\.");
            if (splits.length > 0)
                serviceName = splits[0];
            
            Element selt = xe.element("state");
            if (selt != null) {
                String state = selt.attributeValue("value");
                String delay = selt.attributeValue("delay", null);
                IMGatewayStateNotification not = 
                    new IMGatewayStateNotification(serviceName, state, delay);
                postIMNotification(not);
                // return; FIXME Tim: leave the text message until the client supports the notifications
            }
            Element otherLocation = xe.element("otherLocation");
            if (otherLocation != null) {
                IMOtherLocationNotification not = 
                    new IMOtherLocationNotification(serviceName,username);
                postIMNotification(not);
                // return; FIXME Tim: leave the text message until the client supports the notifications
            }
        }
        
        // either TO or FROM, depending which one isn't "me"
        JID remoteJID = (toMe ? msg.getFrom() : msg.getTo());
        // find the appropriate chat
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
                
                // try to use use the remote node of this message
                if (threadId == null || threadId.length() == 0) {
                    threadId = (toMe ? msg.getFrom() : msg.getTo()).getNode();
                    
                    // finally: just use the full remote JID (coming from a domain, e.g. 
                    // a transport agent
                    if (threadId == null || threadId.length() == 0) {
                        threadId = (toMe ? msg.getFrom() : msg.getTo()).toBareJID();
                    }
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
                IMAddr fromAddr = IMAddr.fromJID(pres.getFrom());
                IMPresence newPresence = new IMPresence(pres);
                mBufferedPresence.put(fromAddr, newPresence);
                IMPresenceUpdateNotification event = new IMPresenceUpdateNotification(fromAddr, newPresence);
                postIMNotification(event);
            } else {
                IMAddr fromAddr;
                Presence reply = null;
                switch (ptype) {
                    case unavailable:
                        // push presence notification
                        fromAddr = IMAddr.fromJID(pres.getFrom());
                        int prio = pres.getPriority();
                        IMPresence newPresence = new IMPresence(Show.OFFLINE, (byte) prio, pres.getStatus());
                        mBufferedPresence.put(fromAddr, newPresence);
                        IMPresenceUpdateNotification event = new IMPresenceUpdateNotification(fromAddr, newPresence);
                        postIMNotification(event);
                    break;
                    case subscribe: {
                        fromAddr = IMAddr.fromJID(pres.getFrom()); 
                        IMSubscribeNotification notify = new IMSubscribeNotification(fromAddr);
                        mPendingSubscribes.put(fromAddr, notify);
                        postIMNotification(notify);
                    }
                    break;
                    case subscribed: {
                        debug("Presence.subscribed: " + pres.toString());
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
        boolean isResult = false;
        
        switch (roster.getType()) {
            case result:
                isResult = true;
                mHaveInitialRoster = true;
                // fall through!
            case set:
                for (Roster.Item item : roster.getItems()) {
                    IMAddr buddyAddr = IMAddr.fromJID(item.getJID());
                    Roster.Subscription subscript = item.getSubscription();
                    
                    // do we need to tell the client about FROM subs?  We do need to tell the
                    // client if the sub is unsubscribed, but not in the case of a roster SET
                    if (roster.getType() == IQ.Type.set || subscript == Roster.Subscription.both || subscript == Roster.Subscription.to) {
                        boolean isTo = (subscript == Roster.Subscription.both || subscript == Roster.Subscription.to);
                        IMSubscribedNotification not = IMSubscribedNotification
                        .create(
                            buddyAddr,
                            item.getName(),
                            item.getGroups(),
                            isTo,
                            item.getAsk());
                        if (isTo) {
                            mRoster.put(buddyAddr, not);
                        } else {
                            mRoster.remove(buddyAddr);
                        }
                        if (!isResult) {
                            postIMNotification(not);
                        }
                    }
                }
                if (isResult) {
                    refreshRoster(null);
                }
            break;
            default:
                debug("Ignoring Roster packet of type %s", roster.getType());
        }
    }

    /**
     * Called when we have a listener (persona is online)
     */
    private void connectToIMServer() {
        try {
            if (mXMPPSession == null && Provisioning.getInstance().getLocalServer().getBooleanAttr(Provisioning.A_zimbraXMPPEnabled, false)) {
                mXMPPSession = new ClientSession(Provisioning.getInstance()
                    .getLocalServer().getName(), new FakeClientConnection(this),
                    XMPPServer.getInstance().getSessionManager().nextStreamID());
                AuthToken at = new AuthToken(mAddr.getAddr());
                mXMPPSession.setAuthToken(at);
                try {
                    mXMPPSession.setAuthToken(at, XMPPServer.getInstance().getUserManager(), this.getResource());
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
                
                // request list of privacy lists
                getDefaultPrivacyList();
            }
        } catch (ServiceException ex) {
            ZimbraLog.im.warn("Caught Exception checking if XMPP enabled "
                        + ex.toString(), ex);
        }
    }
    
    public void setPrivacyList(PrivacyList pl) {
        synchronized(getLock()) {
            // figure out what name to use
            String plName = pl.getName();
            if (plName == null) {
                plName = mDefaultPrivacyListName;
                if (plName == null)
                    plName = "default";
            }
            
            // update privacy list
            {
                IQ set = new IQ();
                set.setType(Type.set);
                org.dom4j.Element query = set.setChildElement("query", "jabber:iq:privacy");
                org.dom4j.Element list= query.addElement("list");
                list.addAttribute("name", plName);
                for (PrivacyListEntry e : pl) {
                    org.dom4j.Element item = list.addElement("item");
                    item.addAttribute("type", "jid");
                    item.addAttribute("value", e.getAddr().toString());
                    item.addAttribute("action", e.getAction().name());
                    item.addAttribute("order", Integer.toString(e.getOrder()));
                    if (e.getTypes() != PrivacyListEntry.BLOCK_ALL) {
                        if (e.isBlockMessages())
                            item.addElement("message");
                        if (e.isBlockPresenceOut())
                            item.addElement("presence-out");
                        if (e.isBlockPresenceIn())
                            item.addElement("presence-in");
                        if (e.isBlockIQ())
                            item.addElement("iq");
                    }
                }
                xmppRoute(set);
            }
            
            // if the default list wasn't already set, then set this list as the default
            if (mDefaultPrivacyListName == null) {
                IQ set = new IQ();
                set.setType(Type.set);
                org.dom4j.Element query = set.setChildElement("query", "jabber:iq:privacy");
                org.dom4j.Element defaultElt = query.addElement("default");
                defaultElt.addAttribute("name", plName);
                xmppRoute(set);
            }
        }
    }
    
    public void requestPrivacyList(String name) {
        synchronized(getLock()) {
            IQ request = new IQ();
            request.setType(Type.get);
            request.setID("getDefaultPrivList-"+(mCurRequestId++));
            org.dom4j.Element query = request.setChildElement("query", "jabber:iq:privacy");
            org.dom4j.Element list = query.addElement("list");
            list.addAttribute("name", name);
            xmppRoute(request);
        }
    }
    
    public void getDefaultPrivacyList() {
        synchronized(getLock()) {
            // request list of privacy lists, when we receive it we'll find the 
            // default and ask the server to list it for us
            IQ iq = new IQ();
            iq.setType(Type.get);
            iq.setID("getPrivLists-"+(mCurRequestId++));
            iq.setChildElement("query", "jabber:iq:privacy");
            xmppRoute(iq);
        }
    }
    
    private void disconnectFromIMServer() {
        assert(!mIsOnline);
        if (mXMPPSession != null) {
            mXMPPSession.getConnection().close();
            mXMPPSession = null;
            mHaveInitialRoster = false;
            mRoster = new HashMap<IMAddr, IMSubscribedNotification>();
            mPendingSubscribes = new HashMap<IMAddr, IMSubscribeNotification>();
            mBufferedPresence = new HashMap<IMAddr, IMPresence>();
        }
    }

//    public boolean inSharedGroup(String name) {
//        return mSharedGroups.contains(name);
//    }

    public void joinChat(String addr, String threadId) throws ServiceException {
        synchronized(getLock()) {
            if (threadId == null) 
                throw new IllegalArgumentException("Cannot joing a chat with a NULL threadId");
            IMChat chat = mChats.get(threadId);
            if (chat == null) {
                chat = new IMChat(this, threadId, null);
                mChats.put(threadId, chat);
            }
            chat.joinMUCChat(addr);
        }
    }

    public void joinSharedGroup(String name) throws ServiceException {
        synchronized(getLock()) {
            try {
                Group group = GroupManager.getInstance().getGroup(name);
                group.getAdmins().add(mAddr.makeJID());
            } catch (GroupNotFoundException ex) {}
        }
    }

    public void leaveSharedGroup(String name) throws ServiceException {
        synchronized(getLock()) {
            try {
                Group group = GroupManager.getInstance().getGroup(name);
                group.getAdmins().remove(mAddr.makeJID());
            } catch (GroupNotFoundException ex) {}
        }
    }

    /**
     * post a notification to all active sessions
     */
    void postIMNotification(IMNotification not) {
        postIMNotification(not, null);
    }
    
    /**
     * post a notification to just a single session
     */
    private void postIMNotification(IMNotification not, Session s) {
        if (s == null) { 
            for (Session session : mListeners) {
                session.notifyIM(not);
            }
        } else {
            s.notifyIM(not);
        }
    }
    
    void processIntercepted(Packet packet) {
        debug("Intercepted packet: %s", packet);
        if (packet instanceof Message) {
            processInternal(packet, true);
        }
    }

    /**
     * callback from the Router thread
     */
    void process(Packet packet) {
        debug("Incoming packet %s: ", packet);
        processInternal(packet, false);
    }
    
    private void processInternal(Packet packet, boolean intercepted) {
        // because we are receiving packets from the PacketInterceptor as well
        // as the session, we need to differentiate the outgoing from the
        // incoming packets
        boolean toMe = true;
        if (packet.getTo() != null
                    && !packet.getTo().toBareJID().equals(this.getAddr().getAddr())) {
            toMe = false;
        }
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

//    public void providerGroupAdd(String name) throws ServiceException {
//        if (mSharedGroups.add(name))
//            flush(null);
//    }
//
//    public void providerGroupRemove(String name) throws ServiceException {
//        if (mSharedGroups.remove(name))
//            flush(null);
//    }

    private void pushMyPresence() throws ServiceException {
        pushMyPresence(null);
    }

    private void pushMyPresence(JID sendTo) throws ServiceException {
        IMPresence presence = getEffectivePresence();
        if (sendTo == null) {
            // send it to my client in case there I have other sessions
            // listening...
            IMPresenceUpdateNotification event = new IMPresenceUpdateNotification(mAddr, presence);
            postIMNotification(event);
        }
        updateXMPPPresence(sendTo, presence);
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
        synchronized(getLock()) {
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
    }

    /**
     * Sends a message over an existing chat
     * 
     * @param address
     * @param message
     */
    public void sendMessage(OperationContext octxt, IMAddr toAddr, String threadId,
                IMMessage message) throws ServiceException {
        synchronized(getLock()) {
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
//                threadId = "chat-" + this.mAddr.getNode() + "-" + mCurChatId;
                threadId = "chat-" + this.mAddr.getNode() + "%" + this.mAddr.getDomain() + "-" + mCurChatId;
                mCurChatId++;
                // add the other user as the first participant in this chat
                IMChat.Participant part;
                part = new IMChat.Participant(toAddr);
                chat = new IMChat(this, threadId, part);
                assert(threadId != null);
                mChats.put(threadId, chat);
            }
            }
            String msg = (message.getBody(Lang.DEFAULT) != null ? message.getBody(Lang.DEFAULT).getPlainText() : null);
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
                } else if (msg.startsWith("/block")) {
                    // "/block foo@bar.com,bug@gub.com,fooz.com"
                    if (msg.equals("/block"))
                        msg = "/block ";

                    msg = msg.substring("/block ".length());
                    String[] addrs = msg.split(",");

                    String listName = mDefaultPrivacyListName;
                    if (listName == null)
                        listName = "default";

                    PrivacyList pl = new PrivacyList(listName);
                    int order = 1;
                    for (String s : addrs) {
                        if (s.length() > 0) {
                            PrivacyListEntry entry = new PrivacyListEntry(new IMAddr(s), order, PrivacyListEntry.Action.deny, PrivacyListEntry.BLOCK_ALL);
                            try {
                                pl.addEntry(entry);
                            } catch (PrivacyList.DuplicateOrderException e) { e.printStackTrace(); }
                            order++;
                        }
                    }

                    setPrivacyList(pl);
                }
            }
            chat.sendMessage(octxt, toAddr, threadId, message, this);
        }
    }

    public void setMyPresence(OperationContext octxt, IMPresence presence)
                throws ServiceException {
        synchronized(getLock()) {
            // TODO optimize out change-to-same eventually (leave for now, very
            // convienent as is)
            mMyPresence = presence;
            flush(octxt);
            pushMyPresence();
        }
    }

    @Override
    public String toString() {
        return new Formatter().format("IMPersona(%s  %s)", mAddr, mMyPresence)
                    .toString();
    }

    private void updateXMPPPresence(JID sendTo, IMPresence pres) {
        if (mXMPPSession != null) {
            Presence xmppPresence = pres.getXMPPPresence();
            
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
