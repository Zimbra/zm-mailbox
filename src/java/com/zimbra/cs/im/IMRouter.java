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

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public class IMRouter implements Runnable {
    
    private static final IMRouter sRouter = new IMRouter();
    public static IMRouter getInstance() { return sRouter; }
    
    private Map<IMAddr, IMPersona> mBuddyListMap = new HashMap();
    private LinkedBlockingQueue<IMEvent> mQueue = new LinkedBlockingQueue();
    private boolean mShutdown = false;
    private Timer mTimer;
    
    private IMRouter() {
        new Thread(this).start();
        mTimer = new Timer();
    }
    
    public Object getLock(IMAddr addr) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.name, addr.getAddr());
        if (acct != null) 
            return Mailbox.getMailboxByAccount(acct);
        else
            throw MailServiceException.NO_SUCH_MBOX(addr.getAddr());
    }
    
    void flush(OperationContext octxt, IMPersona persona) throws ServiceException {
        Mailbox mbox = Mailbox.getMailboxByAccount(Provisioning.getInstance().get(AccountBy.name, persona.getAddr().getAddr()));
        assert(persona.getAddr().getAddr().equals(mbox.getAccount().getName()));
        Metadata md = persona.encodeAsMetatata();
        mbox.setConfig(octxt, "im", md);
    }
    
    public synchronized IMPersona findPersona(OperationContext octxt, IMAddr addr, boolean loadSubs) throws ServiceException 
    {
        // first, just check the map
        IMPersona toRet = mBuddyListMap.get(addr);
        
        // okay, maybe it's an alias -- get the account and resolve it to the cannonical name
        if (toRet == null) {
            Account acct = Provisioning.getInstance().get(AccountBy.name, addr.getAddr());            
            String canonName = acct.getName();
            addr = new IMAddr(canonName);
            toRet = mBuddyListMap.get(addr);
        }
        
        // okay, we might have to create the persona
        if (toRet == null) {
            Mailbox mbox = Mailbox.getMailboxByAccount(Provisioning.getInstance().get(AccountBy.name, addr.getAddr()));
            Metadata md = mbox.getConfig(octxt, "im");
            if (md != null)
                toRet = IMPersona.decodeFromMetadata(addr, md);
                    
            if (toRet == null)
                toRet = new IMPersona(addr);
            
            mBuddyListMap.put(addr, toRet);
        }
        
        if (loadSubs) {
            toRet.loadSubs();
        }
        return toRet;
    }
    
    public synchronized IMPersona findPersona(OperationContext octxt, Mailbox mbox, boolean loadSubs) throws ServiceException 
    {
        IMAddr addr = new IMAddr(mbox.getAccount().getName());
        IMPersona toRet = mBuddyListMap.get(addr);
        if (toRet == null) {
            Metadata md = mbox.getConfig(octxt, "im");
            if (md != null)
                toRet = IMPersona.decodeFromMetadata(addr, md);
                    
            if (toRet == null)
                toRet = new IMPersona(addr);
            
            mBuddyListMap.put(addr, toRet);
        }
        
        if (loadSubs) {
            toRet.loadSubs();
        }
        return toRet;
    }
    
    
    Timer getTimer() { return mTimer; }
    
    /**
     * Post an event to the router's asynchronous execution queue.  The event
     * will happen at a later time and will be run without any locks held, to
     * avoid deadlock issues.
     * 
     * @param event
     */
    synchronized void postEvent(IMEvent event) {
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
                ZimbraLog.im.debug("Executing IMEvent: "+ event);
                event.run();
            } catch (InterruptedException ex) {
                
            } catch (Throwable e) {
                e.printStackTrace();
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
        
        mTimer.cancel();
    }
    
//    Mailbox getMailboxFromAddr(IMAddr addr) throws ServiceException {
//        return Mailbox.getMailboxByAccount(Provisioning.getInstance().getAccountByName(addr.getAddr()));
//    }
    
}
