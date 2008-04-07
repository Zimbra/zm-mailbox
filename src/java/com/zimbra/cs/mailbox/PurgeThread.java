/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Zimbra;

/**
 * Iterates all the mailboxes in the system, purges them one at a time
 * and sleeps between purges for a time interval specified by
 * {@link Provisioning#A_zimbraMailPurgeSleepInterval}.
 * 
 * @author bburtin
 */
public class PurgeThread
extends Thread {

    private static volatile PurgeThread sPurgeThread = null;
    private static Object THREAD_CONTROL_LOCK = new Object();
    private boolean mShutdownRequested = false;
    
    private PurgeThread() {
        setName("MailboxPurge");
    }

    /**
     * Starts up the mailbox purge thread.
     */
    public synchronized static void startup() {
        synchronized (THREAD_CONTROL_LOCK) {
            if (isRunning()) {
                ZimbraLog.purge.warn("Cannot start a second purge thread while another one is running.");
                return;
            }

            if (getSleepInterval() == 0) {
                ZimbraLog.purge.debug("Not starting purge thread because %s is 0",
                    Provisioning.A_zimbraMailPurgeSleepInterval);
                return;
            }

            // Log status
            try {
                String displayInterval = Provisioning.getInstance().getLocalServer().getAttr(
                    Provisioning.A_zimbraMailPurgeSleepInterval, null);
                ZimbraLog.purge.info("Starting purge thread with sleep interval %s", displayInterval);
            } catch (ServiceException e) {
                ZimbraLog.purge.warn("Unable to get %s.  Aborting thread startup.",
                    Provisioning.A_zimbraMailPurgeSleepInterval, e);
                return;
            }

            // Start thread
            sPurgeThread = new PurgeThread();
            sPurgeThread.start();
        }
    }

    /**
     * Returns <tt>true</tt> if the mailbox purge thread is currently running.
     */
    public synchronized static boolean isRunning() {
        synchronized (THREAD_CONTROL_LOCK) {
            if (sPurgeThread != null) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Shuts down the mailbox purge thread.  Does nothing if it is not running.
     */
    public synchronized static void shutdown() {
        synchronized (THREAD_CONTROL_LOCK) {
            if (sPurgeThread != null) {
                sPurgeThread.requestShutdown();
                sPurgeThread.interrupt();
                sPurgeThread = null;
            } else {
                ZimbraLog.purge.debug("shutdown() called, but purge thread is not running.");
            }
        }
    }
    
    /**
     * Iterates all mailboxes, purging one at a time and sleeping
     * between purges.
     */
    public void run() {
        while (true) {
            List<Integer> mailboxIds = getMailboxIds();
            int numPurged = 0;
            
            for (int mailboxId : mailboxIds) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                ZimbraLog.addMboxToContext(mailboxId);
                
                try {
                    MailboxManager mm = MailboxManager.getInstance();
                    if (mm.isMailboxLoadedAndAvailable(mailboxId)) {
                        // Look up mailbox and purge messages
                        Mailbox mbox = mm.getMailboxById(mailboxId);
                        Account account = mbox.getAccount();
                        ZimbraLog.addAccountNameToContext(account.getName());
                        mbox.purgeMessages(null);
                        numPurged++;
                        saveLastPurgedId(mailboxId);
                        
                        // Sleep or exit
                        long interval = getSleepInterval();
                        if (interval == 0) {
                            mShutdownRequested = true;
                        }
                        
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            ZimbraLog.purge.debug("Purge thread was interrupted.");
                            mShutdownRequested = true;
                        }
                        
                        if (mShutdownRequested) {
                            ZimbraLog.purge.info("Shutting down purge thread.");
                            return;
                        }
                    }
                } catch (Throwable t) {
                    if (t instanceof OutOfMemoryError) {
                        Zimbra.halt("Ran out of memory while purging mailboxes", t);
                    } else {
                        ZimbraLog.purge.warn("Unable to purge mailbox %d", mailboxId, t);
                    }
                }
            }

            // If nothing's getting purged, sleep to avoid a tight loop 
            if (numPurged == 0) {
                try {
                    long interval = getSleepInterval();
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    ZimbraLog.purge.info("Purge thread was interrupted.  Shutting down.");
                    mShutdownRequested = true;
                    return;
                }
            }
        }
    }
    
    private void requestShutdown() {
        mShutdownRequested = true;
    }
    
    private static long getSleepInterval() {
        long interval = 0;
        
        try {
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getLocalServer();
            interval = server.getTimeInterval(Provisioning.A_zimbraMailPurgeSleepInterval, 0);
        } catch (ServiceException e) {
            ZimbraLog.purge.warn("Unable to determine value of %s",
                Provisioning.A_zimbraMailPurgeSleepInterval, e);
        }
        
        return interval;
    }
    
    private void saveLastPurgedId(int mailboxId)
    throws ServiceException {
        // Update last purged id
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailLastPurgedMailboxId, Integer.toString(mailboxId));
        prov.modifyAttrs(server, attrs);
    }

    /**
     * Returns all the mailbox id's in purge order, starting with the one
     * after {@link Provisioning#A_zimbraMailLastPurgedMailboxId}.
     */
    private List<Integer> getMailboxIds() {
        List<Integer> mailboxIds = new ArrayList<Integer>();

        try {
            // Get sorted list of id's
            for (int id : MailboxManager.getInstance().getMailboxIds()) {
                mailboxIds.add(id);
            }
            Collections.sort(mailboxIds);
            
            // Reorder id's so that we start with the one after the last purged
            Server server = Provisioning.getInstance().getLocalServer();
            int lastId = server.getIntAttr(Provisioning.A_zimbraMailLastPurgedMailboxId, 0);
            for (int i = 0; i < mailboxIds.size(); i++) {
                if (mailboxIds.get(i) > lastId) {
                    Collections.rotate(mailboxIds, -i);
                    break;
                }
            }
            
        } catch (ServiceException e) {
            ZimbraLog.purge.warn("Unable to get mailbox id's", e);
            return new ArrayList<Integer>();
        }
        
        return mailboxIds;
    }
}
