/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackUtil;
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

            if (!CallbackUtil.logStartup(Provisioning.A_zimbraMailPurgeSleepInterval)) {
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

        Set<Integer> purgePendingMailboxes = new HashSet<Integer>();
        while (true) {
            List<Integer> mailboxIds = getMailboxIds();
            boolean slept = false;

            for (int i = 0; i < mailboxIds.size(); i++) {
                int mailboxId = mailboxIds.get(i);
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
                    if (mm.isMailboxLoadedAndAvailable(mailboxId) || purgePendingMailboxes.contains(mailboxId)) {
                        attemptedPurge = true;
                        Mailbox mbox = mm.getMailboxById(mailboxId);
                        Account account = mbox.getAccount();
                        Provisioning prov = Provisioning.getInstance();
                        if (!Provisioning.ACCOUNT_STATUS_MAINTENANCE.equals(account.getAccountStatus(prov)) &&
                                !account.isIsExternalVirtualAccount()) {
                            ZimbraLog.addAccountNameToContext(account.getName());
                            boolean purgedAll = mbox.purgeMessages(null);
                            if (!purgedAll) {
                                ZimbraLog.purge.info("Not all messages were purged.  Scheduling mailbox to be purged again.");
                                mailboxIds.add(mailboxId);
                            }
                            ZimbraLog.purge.debug("Purging search history for mailbox %d", mailboxId);
                            mbox.purgeSearchHistory(null);
                            Config.setInt(Config.KEY_PURGE_LAST_MAILBOX_ID, mbox.getId());
                        } else {
                            ZimbraLog.purge.debug("Skipping mailbox %d because the account is in maintenance status or is an external virtual account.", mailboxId);
                        }
                    } else {
                        ZimbraLog.purge.debug("Skipping mailbox %d because it is not loaded into memory.", mailboxId);
                    }
                } catch (ServiceException se) {
                    if (ServiceException.WRONG_HOST.equals(se.getCode())) {
                        if (ZimbraLog.purge.isDebugEnabled()) {
                            ZimbraLog.purge.debug("not purging mailbox moved to other host ", se);
                        } else {
                            ZimbraLog.purge.info("not purging mailbox %d; account moved to another host", mailboxId);
                        }
                    } else {
                        ZimbraLog.purge.warn("Unable to purge mailbox %d", mailboxId, se);
                    }
                } catch (OutOfMemoryError oome) {
                    Zimbra.halt("Ran out of memory while purging mailboxes", oome);
                } catch (Throwable t) {
                    ZimbraLog.purge.warn("Unable to purge mailbox %d", mailboxId, t);
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

            try {
                long lastPurgeMaxDuration = Provisioning.getInstance().getLocalServer().getLastPurgeMaxDuration();
                purgePendingMailboxes = MailboxManager.getInstance().getPurgePendingMailboxes(System.currentTimeMillis() - lastPurgeMaxDuration);
            } catch (ServiceException e) {
                ZimbraLog.purge.warn("Unable to get purge pending mailboxes ", e);
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
        sSleepInterval = CallbackUtil.getTimeInterval(Provisioning.A_zimbraMailPurgeSleepInterval, sSleepInterval);
        return sSleepInterval;
    }

    /**
     * Returns all the mailbox id's in purge order, starting with the one
     * after {@link Config#KEY_PURGE_LAST_MAILBOX_ID}.
     */
    private List<Integer> getMailboxIds() {
        List<Integer> mailboxIds = new ArrayList<Integer>();

        try {
            mailboxIds = CallbackUtil.getSortedMailboxIdList();
            // Reorder id's so that we start with the one after the last purged
            int lastId = Config.getInt(Config.KEY_PURGE_LAST_MAILBOX_ID, 0);
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
