/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Config;
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
                ZimbraLog.purge.info("Not starting purge thread because %s is 0.",
                    Provisioning.A_zimbraMailPurgeSleepInterval);
                return;
            }

            // Log status
            try {
                String displayInterval = Provisioning.getInstance().getLocalServer().getAttr(
                    Provisioning.A_zimbraMailPurgeSleepInterval, null);
                ZimbraLog.purge.info("Starting purge thread with sleep interval %s.", displayInterval);
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
    @Override public void run() {
        // Sleep before doing work, to give the server time to warm up.  Also limits the amount
        // of random effect when determining the next mailbox id.
        long sleepTime = LC.purge_initial_sleep_ms.longValue();
        ZimbraLog.purge.info("Purge thread sleeping for %dms before doing work.", sleepTime);

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            ZimbraLog.purge.info("Shutting down purge thread.");
            sPurgeThread = null;
            return;
        }
        
        while (true) {
            List<Long> mailboxIds = getMailboxIds();
            boolean slept = false;
            
            for (int i = 0; i < mailboxIds.size(); i++) {
                long mailboxId = mailboxIds.get(i);
                if (mShutdownRequested) {
                    ZimbraLog.purge.info("Shutting down purge thread.");
                    sPurgeThread = null;
                    return;
                }
                ZimbraLog.addMboxToContext(mailboxId);

                // Purge the next mailbox
                boolean attemptedPurge = false;
                try {
                    MailboxManager mm = MailboxManager.getInstance();
                    if (mm.isMailboxLoadedAndAvailable(mailboxId)) {
                        attemptedPurge = true;
                        Mailbox mbox = mm.getMailboxById(mailboxId);
                        Account account = mbox.getAccount();
                        ZimbraLog.addAccountNameToContext(account.getName());
                        boolean purgedAll = mbox.purgeMessages(null);
                        if (!purgedAll) {
                            ZimbraLog.purge.info("Not all messages were purged.  Scheduling mailbox to be purged again.");
                            mailboxIds.add(mailboxId);
                        }
                        Config.setLong(Config.KEY_PURGE_LAST_MAILBOX_ID, mbox.getId());
                    } else {
                        ZimbraLog.purge.debug("Skipping mailbox %d because it is not loaded into memory.", mailboxId);
                    }
                } catch (Throwable t) {
                    if (t instanceof OutOfMemoryError) {
                        Zimbra.halt("Ran out of memory while purging mailboxes", t);
                    } else {
                        ZimbraLog.purge.warn("Unable to purge mailbox %d", mailboxId, t);
                    }
                }
                
                ZimbraLog.clearContext();
                if (attemptedPurge) {
                    // Sleep after every purge attempt.
                    sleep();
                    slept = true;
                }
           }

            // If nothing's getting purged, sleep to avoid a tight loop 
            if (!slept) {
                sleep();
            }
        }
    }
    
    /**
     * Sleeps for the time interval specified by {@link Provisioning#A_zimbraMailPurgeSleepInterval}.
     * If sleep is interrupted, sets {@link #mShutdownRequested} to <tt>true</tt>.
     */
    private void sleep() {
        long interval = getSleepInterval();
        ZimbraLog.purge.debug("Sleeping for %d milliseconds.", interval);
        
        if (interval > 0) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                ZimbraLog.purge.debug("Purge thread was interrupted.");
                mShutdownRequested = true;
            }
        } else {
            mShutdownRequested = true;
        }
    }
    
    private void requestShutdown() {
        mShutdownRequested = true;
    }
    
    /**
     * Stores the sleep interval, so that the purge thread doesn't
     * die if there's a problem talking to LDAP.  See bug 32639.
     */
    private static long sSleepInterval = 0;
    
    /**
     * Returns the current value of {@link Provisioning#A_zimbraMailPurgeSleepInterval},
     * or <tt>0</tt> if it cannot be determined.
     */
    private static long getSleepInterval() {
        try {
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getLocalServer();
            sSleepInterval = server.getTimeInterval(Provisioning.A_zimbraMailPurgeSleepInterval, 0);
        } catch (ServiceException e) {
            ZimbraLog.purge.warn("Unable to determine value of %s.  Using previous value: %d.",
                Provisioning.A_zimbraMailPurgeSleepInterval, sSleepInterval, e);
        }
        
        return sSleepInterval;
    }
    
    /**
     * Returns all the mailbox id's in purge order, starting with the one
     * after {@link Config#KEY_PURGE_LAST_MAILBOX_ID}.
     */
    private List<Long> getMailboxIds() {
        List<Long> mailboxIds = new ArrayList<Long>();

        try {
            // Get sorted list of id's
            for (long id : MailboxManager.getInstance().getMailboxIds()) {
                mailboxIds.add(id);
            }
            Collections.sort(mailboxIds);
            
            // Reorder id's so that we start with the one after the last purged
            long lastId = Config.getLong(Config.KEY_PURGE_LAST_MAILBOX_ID, 0);
            for (int i = 0; i < mailboxIds.size(); i++) {
                if (mailboxIds.get(i) > lastId) {
                    Collections.rotate(mailboxIds, -i);
                    break;
                }
            }
            
        } catch (ServiceException e) {
            ZimbraLog.purge.warn("Unable to get mailbox id's", e);
            return Collections.emptyList();
        }
        
        return mailboxIds;
    }
}
