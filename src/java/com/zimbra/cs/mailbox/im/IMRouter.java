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
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public class IMRouter implements Runnable {
    
    private static final IMRouter sRouter = new IMRouter();
    
    public static IMRouter getInstance() { return sRouter; }
    
    Map<IMAddr, IMPersona> mBuddyListMap = new HashMap();
    LinkedBlockingQueue<IMEvent> mQueue = new LinkedBlockingQueue();
    public boolean mShutdown = false;
    
    private IMRouter() {
        new Thread(this).start();
    }
//    
//    public synchronized IMPersona findPersona(OperationContext octxt, IMAddr addr, boolean loadSubs) throws ServiceException 
//    {
//        IMPersona toRet = mBuddyListMap.get(addr);
//        if (toRet == null) {
//            toRet = new IMPersona(addr, null);
//            
//            mBuddyListMap.put(addr, toRet);
//        }
//        
//        if (loadSubs) {
//            toRet.loadSubs();
//        }
//        return toRet;
//    }
    
    public synchronized IMPersona findPersona(OperationContext octxt, Mailbox mbox, boolean loadSubs) throws ServiceException 
    {
        IMAddr addr = new IMAddr(mbox.getAccount().getName());
        IMPersona toRet = mBuddyListMap.get(addr);
        if (toRet == null) {
            Metadata md = mbox.getConfig(octxt, "im");
            if (md != null)
                toRet = IMPersona.decodeFromMetadata(mbox, addr, md);
                    
            if (toRet == null)
                toRet = new IMPersona(addr, mbox);
            
            mBuddyListMap.put(addr, toRet);
        }
        
        if (loadSubs) {
            toRet.loadSubs();
        }
        return toRet;
    }
    
    
    public synchronized boolean isPersonaLoaded(IMAddr address) {
        return mBuddyListMap.containsKey(address);
    }
    
    /**
     * Post an event to the router's asynchronous execution queue.  The event
     * will happen at a later time and will be run without any locks held, to
     * avoid deadlock issues.
     * 
     * @param event
     */
    public synchronized void postEvent(IMEvent event) {
        assert(!mShutdown);
        mQueue.add(event);
    }
    
    /**
     * Scrub the asynchronous execution queue.  
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (!mShutdown) {
            try {
                IMEvent event = mQueue.take();
                ZimbraLog.im.info("Executing IMEvent: "+ event);
                event.run();
            } catch (InterruptedException ex) {
                
            } catch (ServiceException ex) {
                ex.printStackTrace();
            }
        }
        
        for (IMEvent event = mQueue.poll(); event != null; event = mQueue.poll()) {
            try {
                event.run();
            } catch(ServiceException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public synchronized void shutdown() {
        mShutdown = true;

        // force the queue to wakeup...
        mQueue.add(new IMNullEvent());
    }
    
    Mailbox getMailboxFromAddr(IMAddr addr) throws ServiceException {
        return Mailbox.getMailboxByAccount(Provisioning.getInstance().getAccountByName(addr.getAddr()));
    }
    
    void postProbePresence(IMAddr fromAddr, IMAddr toAddr)
    {
        postEvent(new IMProbeEvent(fromAddr, toAddr));
    }
    
    void postPresenceUpdate(IMAddr fromAddr, IMPresence presence, List<IMAddr> targets)
    {
        postEvent(new IMPresenceUpdateEvent(fromAddr, presence, targets));
    }
    
    
    void postSendMessage(IMAddr fromAddr, String threadId, List<IMAddr> targets, IMMessage message)
    {
        postEvent(new IMSendMessageEvent(fromAddr, threadId, targets, message));
    }
    
    void postLeftChat(IMAddr fromAddr, String threadId, List<IMAddr> targets) {
        postEvent(new IMLeftChatEvent(fromAddr, threadId, targets));
    }
    
    void postSubscriptionUpdate(IMAddr fromAddr, IMAddr toAddr, IMSubscriptionEvent.Op op)
    {
        postEvent(new IMSubscriptionEvent(fromAddr, toAddr, op));
    }
    
    void onPresenceUpdate(IMAddr from, IMPresence presence, List<IMAddr> targets) throws ServiceException
    {
        for (IMAddr addr : targets) {
            Mailbox mbox = getMailboxFromAddr(addr);
            synchronized (mbox) {
                IMPersona persona = findPersona(null, mbox, false);
                persona.handlePresenceUpdate(from, presence);
            }
            
        }
    }
    
    void onNewMessage(IMAddr fromAddr, String threadId, List<IMAddr> toAddr, IMMessage msg) throws ServiceException
    {
        for (IMAddr addr : toAddr) {
            Mailbox mbox = getMailboxFromAddr(addr);
            synchronized (mbox) {
                IMPersona persona = findPersona(null, mbox, false);
                persona.handleMessage(fromAddr, threadId, msg);
            }
        }
    }
    
    void onCloseChat(IMAddr fromAddr, String threadId, List<IMAddr> targets) throws ServiceException {
        for (IMAddr addr : targets) {
            Mailbox mbox = getMailboxFromAddr(addr);
            synchronized (mbox) {
                IMPersona persona = findPersona(null, mbox, false);
                persona.handleLeftChat(fromAddr, threadId);
            }
        }
        
    }
    
    void onSubscriptionEvent(IMAddr fromAddr, IMAddr toAddr, IMSubscriptionEvent.Op op) throws ServiceException
    {
        Mailbox mbox = getMailboxFromAddr(toAddr);
        synchronized (mbox) {
            IMPersona persona = findPersona(null, mbox, false);
            switch (op) {
            case SUBSCRIBE:
                persona.handleAddIncomingSubscription(fromAddr);
                break;
            case UNSUBSCRIBE:
                persona.handleRemoveIncomingSubscription(fromAddr);
                break;
            }
        }
    }
}
