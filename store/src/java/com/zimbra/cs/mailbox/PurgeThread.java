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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.DelayedIndexStatus;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackUtil;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.history.SearchHistory;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;
import com.zimbra.cs.service.admin.ManageIndex;
import com.zimbra.cs.util.AccountUtil;
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
            if (MailboxClusterUtil.isBackupRestorePod()) {
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
            return (sPurgeThread != null);
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
            ZimbraLog.purge.info("Shutting down purge thread - initial sleep was interrupted.");
            sPurgeThread = null;
            return;
        }

        // purgePendingMailboxes is the set of mailboxes which have not been purged since time X where X is
        // zimbraLastPurgeMaxDuration ago (default value 30 days)
        Set<Integer> purgePendingMailboxes = new HashSet<Integer>();
        SoapProvisioning soapProv = new SoapProvisioning();
        try {
            soapProv.soapSetURI(URLUtil.getAdminURL("localhost"));
            while (true) {
                Map<String, Integer> acctIdsToMboxId = getAccountIdToMailboxIdMap();
                Map<Integer, String> mboxIdToAcctId = new HashMap<>(acctIdsToMboxId.size());
                for (Map.Entry<String, Integer> entry : acctIdsToMboxId.entrySet()) {
                    mboxIdToAcctId.put(entry.getValue(), entry.getKey());
                }
                List<Integer> mailboxIds = sortMailboxIdsInPurgeOrder(acctIdsToMboxId.values());
                boolean slept = false;
                int numMailboxIds = mailboxIds.size();
                int numPurges = 0;
                for (int i = 0; i < mailboxIds.size(); i++) {
                    Integer mailboxId = mailboxIds.get(i);
                    if (mShutdownRequested) {
                        ZimbraLog.purge.info("Shutting down purge thread. - mShutdownRequested set");
                        sPurgeThread = null;
                        return;
                    }
                    String accountId = mboxIdToAcctId.get(mailboxId);
                    if (accountId == null) {
                        ZimbraLog.purge.info("Unable to map mailbox ID=%s to account - ignoring", mailboxId);
                        continue;
                    }
                    // Purge the next mailbox
                    if (attemptPurge(mailboxId, accountId, purgePendingMailboxes.contains(mailboxId), mailboxIds)) {
                        // Sleep after every purge attempt.
                        sleep();
                        slept = true;
                        numPurges++;
                    }
                }
                // If nothing's getting purged, sleep to avoid a tight loop
                if (!slept) {
                    sleep();
                }

                try {
                    long lastPurgeMaxDuration = Provisioning.getInstance().getLocalServer().getLastPurgeMaxDuration();
                    purgePendingMailboxes = MailboxManager.getInstance().getPurgePendingMailboxes(
                            System.currentTimeMillis() - lastPurgeMaxDuration);
                } catch (ServiceException e) {
                    ZimbraLog.purge.warn("Unable to get purge pending mailboxes ", e);
                }
                ZimbraLog.purge.info("Purge thread iteration: purged=%d numMboxes=%d reschedules=%d purgePending=%d",
                        numPurges, numMailboxIds, mailboxIds.size() - numMailboxIds, purgePendingMailboxes.size());
            }
        } finally {
            if (soapProv != null) {
                synchronized (soapProv) {
                    try {
                        soapProv.soapLogOut();
                    } catch (ServiceException se) {
                        ZimbraLog.purge.debug("Problem logging out for SoapProvisioning", se);
                    }
                }
            }
        }
    }

    /**
     * @param mailboxId Mailbox ID of mailbox to be purged
     * @param accountId Account ID associated with the mailbox
     * @param isPurgePending True if this mailbox hasn't been purged for at least lastPurgeMaxDuration.
     *                       This means we should potentially try to purge the mailbox even if it isn't loaded.
     * @param mailboxIds list of mailboxes we are currently processing.  May be appended to if purge incomplete
     * @return true if a purge was attempted for this mailbox
     */
    private boolean attemptPurge(Integer mailboxId, String accountId, boolean isPurgePending,
                                 List<Integer> mailboxIds) {
        ZimbraLog.addMboxToContext(mailboxId);
        boolean attemptedPurge = false;
        try {
            MailboxManager mm = MailboxManager.getInstance();
            if (mm.isMailboxLoadedAndAvailable(mailboxId) || isPurgePending) {
                Provisioning prov = Provisioning.getInstance();
                Account account = prov.get(Key.AccountBy.id, accountId);
                if (account == null) {
                    ZimbraLog.purge.info("Unable to get account for accountId='%s', skipping mailbox %d",
                            accountId, mailboxId);
                    return false;
                }
                if (account.isIsExternalVirtualAccount()) {
                    ZimbraLog.purge.info("Skip mailbox %d - account '%s' is an external virtual account",
                            mailboxId, account);
                    return false;
                }
                if (Provisioning.ACCOUNT_STATUS_MAINTENANCE.equals(account.getAccountStatus(prov))) {
                    ZimbraLog.purge.info("Skip mailbox %d - account '%s' is in maintenance", mailboxId, account);
                    return false;
                }
                if (!Provisioning.isMyIpAddress(Provisioning.affinityServer(account))) {
                    /* PurgeThread currently runs on all mailboxes, so only purging messages for
                     * accounts who have affinity for this server on basis that other mailboxes will handle their
                     * accounts.  If design changes to have a dedicated pod, can use PurgeMessageRequest SOAP calls.
                     * Previous commit in git history contains code which does this should we want to use it
                     * in the future. */
                    return false;
                }
                attemptedPurge = true;
                ZimbraLog.addAccountNameToContext(account.getName());
                Mailbox mbox = mm.getMailboxById(mailboxId);
                boolean purgedAll = mbox.purgeMessages(null);
                if (!purgedAll) {
                    ZimbraLog.purge.info("Not all messages were purged.  Scheduling mailbox to be purged again.");
                    mailboxIds.add(mailboxId);
                }
                if (SearchHistory.featureEnabled(account)) {
                    ZimbraLog.purge.debug("Purging search history for mailbox %d account %s",
                            mailboxId, accountId);
                    mbox.purgeSearchHistory(null);
                }

                disableIndexingIfNecessary(account, mbox);

                Config.setInt(Config.KEY_PURGE_LAST_MAILBOX_ID, mailboxId);
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
        return attemptedPurge;
    }

    private void disableIndexingIfNecessary(Account account, Mailbox mbox) throws ServiceException {
        if (AccountUtil.isGalSyncAccount(account)) {
            // don't purge gal accounts!
            return;
        }
        DelayedIndexStatus indexStatus = account.getDelayedIndexStatus();
        if (indexStatus == DelayedIndexStatus.suppressed || indexStatus == DelayedIndexStatus.waitingForSearch) {
            // indexing is already disabled
            return;
        }
        if (mbox.index.isReIndexInProgress()) {
            ZimbraLog.purge.debug("re-index is in progress for %s, skipping index purge");
            return;
        }
        long maxAge = account.getDelayedIndexInactiveAccountAge();
        if (maxAge == 0) {
            // age-based index deletion is disabled
            return;
        }
        Date lastAccess = account.getLastLogonTimestamp();
        if (lastAccess == null) {
            lastAccess = account.getCreateTimestamp();
            ZimbraLog.purge.debug("%s has never logged in - using account creation time %s as index deletion cutoff", account.getName(), lastAccess);
        }
        if (new Date().getTime() - lastAccess.getTime() > maxAge) {
            String maxAgeStr = account.getDelayedIndexInactiveAccountAgeAsString();
            ZimbraLog.purge.info("account %s has been inactive for more than %s; disabling indexing and deleting index data", account.getName(), maxAgeStr);
            OperationContext octxt = new OperationContext(account, account.isIsAdminAccount());
            ManageIndex.disableIndexing(account, mbox, octxt);
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

    private Map<String, Integer> getAccountIdToMailboxIdMap() {
        try {
            return MailboxManager.getInstance().cacheManager.getAccountIdToMailboxIdMap();
        } catch (ServiceException e) {
            ZimbraLog.purge.warn("Unable to get account id to mailbox id map", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Returns all the mailbox ids in purge order, starting with the one
     * after {@link Config#KEY_PURGE_LAST_MAILBOX_ID}.
     */
    private List<Integer> sortMailboxIdsInPurgeOrder(Collection<Integer> mailboxIds) {
        // Reorder ids so that we start with the one after the last purged
        List<Integer> list = new ArrayList(mailboxIds);
        Collections.sort(list);
        int lastId = Config.getInt(Config.KEY_PURGE_LAST_MAILBOX_ID, 0);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) > lastId) {
                Collections.rotate(list, -i);
                break;
            }
        }
        return list;
    }
}
