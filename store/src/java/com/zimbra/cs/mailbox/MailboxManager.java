/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.redolog.op.CreateMailbox;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;

import static com.zimbra.cs.mailbox.Mailbox.*;
import com.zimbra.cs.mailbox.Mailbox.MailboxTransaction;

public class MailboxManager {

    public final MailboxCacheManager cacheManager = new DistributedMailboxCacheManager();

    public static enum FetchMode {
        AUTOCREATE,         // create the mailbox if it doesn't exist
        DO_NOT_AUTOCREATE,  // fetch from DB if not in memory, but don't create it if it isn't in the DB
        ONLY_IF_CACHED,     // don't fetch from the DB, only return if cached
        ;
    }

    /**
     * Listener for mailbox loading
     */
    public static interface Listener {
        /** Called whenever a mailbox has left Maintenance mode */
        public void mailboxAvailable(Mailbox mbox);

        /** Called whenever a mailbox is loaded */
        public void mailboxLoaded(Mailbox mbox);

        /** Called whenever a mailbox is created */
        public void mailboxCreated(Mailbox mbox);

        /** Called whenever a mailbox is deleted from this server.
         *  Could mean the mailbox was moved to another server, or could mean really deleted */
        public void mailboxDeleted(String accountId);
    }

    private ConcurrentHashMap<String, MailboxMaintenance> maintenanceLocks = new ConcurrentHashMap<String, MailboxMaintenance>();

    private CopyOnWriteArrayList<AdditionalQuotaProvider> mAdditionalQuotaProviders = new CopyOnWriteArrayList<AdditionalQuotaProvider>();

    public void addAdditionalQuotaProvider(AdditionalQuotaProvider additionalQuotaProvider) {
        assert(!mAdditionalQuotaProviders.contains(additionalQuotaProvider));
        mAdditionalQuotaProviders.add(additionalQuotaProvider);
    }

    public void removeAdditionalQuotaProvider(AdditionalQuotaProvider additionalQuotaProvider) {
        assert(mAdditionalQuotaProviders.contains(additionalQuotaProvider));
        mAdditionalQuotaProviders.remove(additionalQuotaProvider);
    }

    public void clearAdditionalQuotaProviders() {
        mAdditionalQuotaProviders.clear();
    }

    List<AdditionalQuotaProvider> getAdditionalQuotaProviders() {
        return mAdditionalQuotaProviders;
    }

    private CopyOnWriteArrayList<Listener> mListeners = new CopyOnWriteArrayList<Listener>();

    public void addListener(Listener listener) {
        assert(!mListeners.contains(listener));
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        assert(mListeners.contains(listener));
        mListeners.remove(listener);
    }

    private void notifyMailboxAvailable(Mailbox mbox) {
        if (ZimbraLog.mbxmgr.isInfoEnabled())
            ZimbraLog.mbxmgr.info("Mailbox "+mbox.getId()+ " account "+mbox.getAccountId()+" AVAILABLE");
        for (Listener listener : mListeners)
            listener.mailboxAvailable(mbox);
    }

    private void notifyMailboxLoaded(Mailbox mbox) {
        if (ZimbraLog.mbxmgr.isInfoEnabled())
            ZimbraLog.mbxmgr.info("Mailbox "+mbox.getId()+ " account "+mbox.getAccountId()+" LOADED");
        for (Listener listener : mListeners)
            listener.mailboxLoaded(mbox);
    }

    private void notifyMailboxCreated(Mailbox mbox) {
        if (ZimbraLog.mbxmgr.isInfoEnabled())
            ZimbraLog.mbxmgr.info("Mailbox "+mbox.getId()+ " account "+mbox.getAccountId()+" CREATED");
        for (Listener listener : mListeners)
            listener.mailboxCreated(mbox);
    }

    private void notifyMailboxDeleted(String accountId) {
        if (ZimbraLog.mbxmgr.isInfoEnabled())
            ZimbraLog.mbxmgr.info("Mailbox for account "+accountId+" DELETED");
        for (Listener listener : mListeners)
            listener.mailboxDeleted(accountId);
    }

    private static MailboxManager sInstance;

    public MailboxManager() throws ServiceException {
    }

    /**
     * TODO: In order to allow sub-classing MailboxManager, MailboxManager
     * should be interface instead of concrete class. Until then, this dummy
     * constructor allows subclasses not to rely on DB.
     *
     * @param extend dummy
     */
    protected MailboxManager(boolean extend) throws ServiceException {
    }

    public synchronized static MailboxManager getInstance() throws ServiceException {
        if (sInstance == null) {
            String className = LC.zimbra_class_mboxmanager.value();
            if (className != null && !className.equals("")) {
                try {
                    try {
                        sInstance = (MailboxManager) Class.forName(className).newInstance();
                    } catch (ClassNotFoundException cnfe) {
                        // ignore and look in extensions
                        sInstance = (MailboxManager) ExtensionUtil.findClass(className).newInstance();
                    }
                } catch (Exception e) {
                    ZimbraLog.account.error("could not instantiate MailboxManager interface of class '" + className + "'; defaulting to MailboxManager", e);
                }
            }
            if (sInstance == null)
                sInstance = new MailboxManager();
        }
        return sInstance;
    }

    @VisibleForTesting
    public static void setInstance(MailboxManager mmgr) {
        sInstance = mmgr;
    }

    public void startup() {
        MailboxIndex.startup();
    }

    public void shutdown() {}

    /** Returns the mailbox for the given account.  Creates a new mailbox
     *  if one doesn't already exist.
     *
     * @param account  The account whose mailbox we want.
     * @return The requested <code>Mailbox</code> object.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul> */
    public Mailbox getMailboxByAccount(Account account) throws ServiceException {
        return getMailboxByAccount(account, FetchMode.AUTOCREATE);
    }

    /** Returns the mailbox for the given account.  Creates a new mailbox
     *  if one doesn't already exist and <code>autocreate</code> is
     *  <tt>true</tt>.
     *
     * @param mailboxAccountId   The id of the account whose mailbox we want.
     * @param autocreate  <tt>true</tt> to create the mailbox if needed,
     *                    <tt>false</tt> to just return <code>null</code>
     * @return The requested <code>Mailbox</code> object, or <code>null</code>.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul> */
    public Mailbox getMailboxByAccount(Account account, boolean autocreate) throws ServiceException {
        return getMailboxByAccount(account, autocreate ? FetchMode.AUTOCREATE : FetchMode.DO_NOT_AUTOCREATE);
    }

    /** Returns the mailbox for the given account id.  Creates a new
     *  mailbox if one doesn't already exist and <code>fetchMode</code>
     *  is <code>FetchMode.AUTOCREATE</code>.
     *
     * @param mailboxAccountId   The id of the account whose mailbox we want.
     * @param fetchMode <code>FetchMode.ONLY_IF_CACHED</code> will return the mailbox only
     *                     if it is already cached in memory
     *                  <code>FetchMode.DO_NOT_AUTOCREATE</code>Will fetch the mailbox from
     *                     the DB if it is not cached, but will not create it.
     *                  <code>FetchMode.AUTOCREATE</code> to create the mailbox if needed
     * @return The requested <code>Mailbox</code> object, or <code>null</code>.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul> */
    public Mailbox getMailboxByAccount(Account account, FetchMode fetchMode) throws ServiceException {
        if (account == null)
            throw new IllegalArgumentException();
        return getMailboxByAccountId(account.getId(), fetchMode);
    }

    /** Returns the mailbox for the given account id.  Creates a new
     *  mailbox if one doesn't already exist.
     *
     * @param accountId  The id of the account whose mailbox we want.
     * @return The requested <code>Mailbox</code> object.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul> */
    public Mailbox getMailboxByAccountId(String accountId) throws ServiceException {
        return getMailboxByAccountId(accountId, FetchMode.AUTOCREATE);
    }

    /** Returns the mailbox for the given account id.  Creates a new
     *  mailbox if one doesn't already exist and <code>autocreate</code>
     *  is <tt>true</tt>.
     *
     * @param accountId   The id of the account whose mailbox we want.
     * @param autocreate  <tt>true</tt> to create the mailbox if needed,
     *                    <tt>false</tt> to just return <code>null</code>
     * @return The requested <code>Mailbox</code> object, or <code>null</code>.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul> */
    public Mailbox getMailboxByAccountId(String accountId, boolean autocreate) throws ServiceException {
        return getMailboxByAccountId(accountId, autocreate ? FetchMode.AUTOCREATE : FetchMode.DO_NOT_AUTOCREATE);
    }

    /** Returns the mailbox for the given account id.  Creates a new
     *  mailbox if one doesn't already exist and <code>fetchMode</code>
     *  is <code>FetchMode.AUTOCREATE</code>.
     *
     * @param accountId   The id of the account whose mailbox we want.
     * @param fetchMode <code>FetchMode.ONLY_IF_CACHED</code> will return the mailbox only
     *                     if it is already cached in memory
     *                  <code>FetchMode.DO_NOT_AUTOCREATE</code>Will fetch the mailbox from
     *                     the DB if it is not cached, but will not create it.
     *                  <code>FetchMode.AUTOCREATE</code> to create the mailbox if needed
     * @return The requested <code>Mailbox</code> object, or <code>null</code>.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul> */
    public Mailbox getMailboxByAccountId(String accountId, FetchMode fetchMode) throws ServiceException {
        return getMailboxByAccountId(accountId, fetchMode, false);
    }

    /** Returns the mailbox for the given account id.  Creates a new
     *  mailbox if one doesn't already exist and <code>fetchMode</code>
     *  is <code>FetchMode.AUTOCREATE</code>.
     *
     * @param accountId   The id of the account whose mailbox we want.
     * @param fetchMode <code>FetchMode.ONLY_IF_CACHED</code> will return the mailbox only
     *                     if it is already cached in memory
     *                  <code>FetchMode.DO_NOT_AUTOCREATE</code>Will fetch the mailbox from
     *                     the DB if it is not cached, but will not create it.
     *                  <code>FetchMode.AUTOCREATE</code> to create the mailbox if needed
     * @param skipMailHostCheck If true, don't do home server check.  WRONG_HOST exception will not be thrown.
     * @return The requested <code>Mailbox</code> object, or <code>null</code>.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul> */
    public Mailbox getMailboxByAccountId(String accountId, FetchMode fetchMode, boolean skipMailHostCheck) throws ServiceException {
        if (accountId == null)
            throw new IllegalArgumentException();

        Integer mailboxKey;
        synchronized (this) {
            mailboxKey = cacheManager.getMailboxKey(accountId.toLowerCase());
        }
        if (mailboxKey != null) {
            if (DebugConfig.mockMultiserverInstall)
                lookupAccountWithHostCheck(accountId, skipMailHostCheck);
            return getMailboxById(mailboxKey, fetchMode, skipMailHostCheck);
        } else if (fetchMode != FetchMode.AUTOCREATE) {
            return null;
        }

        // auto-create the mailbox if this is the right host...
        Account account = lookupAccountWithHostCheck(accountId, skipMailHostCheck);
        synchronized (this) {
            mailboxKey = cacheManager.getMailboxKey(accountId.toLowerCase());
        }
        if (mailboxKey != null)
            return getMailboxById(mailboxKey, fetchMode, skipMailHostCheck);
        else
            return createMailbox(null, account, skipMailHostCheck);
    }

    private Account lookupAccountWithHostCheck(String accountId, boolean skipMailHostCheck) throws ServiceException {
        Account account = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        if (!skipMailHostCheck && !Provisioning.onLocalServer(account))
            throw ServiceException.WRONG_HOST(account.getMailHost(), null);
        return account;
    }


    /** Returns the <code>Mailbox</code> with the given id.  Throws an
     *  exception if no such <code>Mailbox</code> exists.  If the
     *  <code>Mailbox</code> is undergoing maintenance, still returns the
     *  <code>Mailbox</code> if the calling thread is the holder of the lock.
     *
     * @param mailboxId  The id of the mailbox we want.
     * @return The requested <code>Mailbox</code> object.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.NO_SUCH_MBOX</code> - if no such mailbox exists (yet)
     *        on this server
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host
     *    <li><code>account.NO_SUCH_ACCOUNT</code> - if the mailbox's Account
     *        has been deleted</ul> */
    public Mailbox getMailboxById(int mailboxId) throws ServiceException {
        return getMailboxById(mailboxId, FetchMode.DO_NOT_AUTOCREATE, false);
    }

    /** Returns the <code>Mailbox</code> with the given id.  Throws an
     *  exception if no such <code>Mailbox</code> exists.  If the
     *  <code>Mailbox</code> is undergoing maintenance, still returns the
     *  <code>Mailbox</code> if the calling thread is the holder of the lock.
     *
     * @param mailboxId  The id of the mailbox we want.
     * @param skipMailHostCheck If true, don't throw WRONG_HOST exception if
     *                          current host is not the mailbox's mail host.
     *                          Most callers should use getMailboxById(int),
     *                          which internally calls this method with
     *                          skipMailHostCheck=false.  Pass true only in the
     *                          special case of retrieving a mailbox for the
     *                          purpose of deleting it.
     * @return The requested <code>Mailbox</code> object.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.NO_SUCH_MBOX</code> - if no such mailbox exists (yet)
     *        on this server
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host
     *    <li><code>account.NO_SUCH_ACCOUNT</code> - if the mailbox's Account
     *        has been deleted and <code>skipMailHostCheck=false</code></ul> */
    public Mailbox getMailboxById(int mailboxId, boolean skipMailHostCheck)
    throws ServiceException {
        return getMailboxById(mailboxId, FetchMode.DO_NOT_AUTOCREATE, skipMailHostCheck);
    }

    protected Mailbox getMailboxById(int mailboxId, FetchMode fetchMode, boolean skipMailHostCheck)
    throws ServiceException {
        // see bug 19088 - we do NOT want to call this while holding the mgr lock, because
        // we need the Mailbox instantiation code to run w/o the lock held.
        assert(fetchMode == FetchMode.ONLY_IF_CACHED || !Thread.holdsLock(this));

        if (mailboxId <= 0)
            throw MailServiceException.NO_SUCH_MBOX(mailboxId);

        long startTime = ZimbraPerf.STOPWATCH_MBOX_GET.start();

        Mailbox mbox = null;
        synchronized (this) {
            // check to see if the mailbox has already been cached
            Object cached = cacheManager.retrieveFromCache(mailboxId, true, this);
            if (cached instanceof Mailbox) {
                ZimbraPerf.COUNTER_MBOX_CACHE.increment(100);
                mbox = (Mailbox) cached;
            }
        }

        if (fetchMode == FetchMode.ONLY_IF_CACHED && (mbox == null || !mbox.isOpen())) {
            // if the mailbox is in the middle of opening, deem it not cached rather than waiting.
            return null;
        }

        if (mbox == null) { // not found in cache
            ZimbraPerf.COUNTER_MBOX_CACHE.increment(0);
            MailboxData data;
            DbConnection conn = DbPool.getConnection();
            try {
                // fetch the Mailbox data from the database
                data = DbMailbox.getMailboxStats(conn, mailboxId);
                if (data == null) {
                    throw MailServiceException.NO_SUCH_MBOX(mailboxId);
                }
            } finally {
                conn.closeQuietly();
            }

            mbox = instantiateMailbox(data);
            Account account = mbox.getAccount();
            boolean isGalSyncAccount = AccountUtil.isGalSyncAccount(account);
            mbox.setGalSyncMailbox(isGalSyncAccount);

            if (!skipMailHostCheck && !Provisioning.onLocalServer(account)) {
                // The host check here makes sure that sessions that were
                // already connected at the time of mailbox move are not
                // allowed to continue working with this mailbox which is
                // essentially a soft-deleted copy.  The WRONG_HOST
                // exception forces the clients to reconnect to the new
                // server.
                throw ServiceException.WRONG_HOST(account.getMailHost(), null);
            }

            synchronized (this) {
                // avoid the race condition by re-checking the cache and using that data (if any)
                Object cached = cacheManager.retrieveFromCache(mailboxId, false, this);
                if (cached instanceof Mailbox) {
                    mbox = (Mailbox) cached;
                } else {
                    // cache the newly-created Mailbox object
                    if (cached instanceof MailboxMaintenance) {
                        ((MailboxMaintenance) cached).setMailbox(mbox);
                    } else {
                        cacheManager.cacheMailbox(mbox, this);
                    }
                }
            }
        }

        // now, make sure the mailbox is opened -- we do this after releasing MailboxManager lock so that filesystem IO
        // and other longer operations don't block the system.
        if (mbox.open()) {
            // if TRUE, then the mailbox is actually opened, so we need to notify listeners of the mailbox being loaded
            notifyMailboxLoaded(mbox);
        }

        ZimbraPerf.STOPWATCH_MBOX_GET.stop(startTime);

        if (maintenanceLocks.containsKey(mbox.getAccountId()) && mbox.getMaintenance() == null) {
            //case here where mailbox was unloaded (due to memory pressure or similar) but
            //it was in maintenance before so needs to be in maintenance now that it is reloaded
            MailboxMaintenance oldMaint = maintenanceLocks.get(mbox.getAccountId());
            MailboxMaintenance maint = null;
            synchronized (mbox) {
                maint = mbox.beginMaintenance();
                synchronized (this) {
                    cacheManager.cacheMailbox(mailboxId, maint);
                }
            }
            if (oldMaint.isNestedAllowed()) {
                maint.setNestedAllowed(true);
            }
            maint.removeAllowedThread(Thread.currentThread());
            maintenanceLocks.put(mbox.getAccountId(), maint);
        }
        return mbox;
    }

    /** @return A list of *hard references* to all currently-loaded mailboxes which are not
     *     .   in MAINTENANCE mode.  Caller must be careful to not hang onto this list for
     *         very long or else mailboxes will not be purged. */
    public synchronized List<Mailbox> getAllLoadedMailboxes() {
        List<Mailbox> mboxes = new ArrayList<Mailbox>(cacheManager.getMailboxCacheSize());
        for (Object o : cacheManager.getAllLoadedMailboxes()) {
            if (o instanceof Mailbox) {
                mboxes.add((Mailbox) o);
            } else if (o instanceof MailboxMaintenance) {
                MailboxMaintenance maintenance = (MailboxMaintenance) o;
                if (maintenance.canAccess()) {
                    mboxes.add(maintenance.getMailbox());
                }
            }
        }
        return mboxes;
    }

    /**
     * Returns the number of hard references to currently-loaded mailboxes, either in MAINTENANCE mode or not.
     */
    public synchronized int getCacheSize() {
        int count = 0;
        for (Object o : cacheManager.getAllLoadedMailboxes()) {
            if (o instanceof Mailbox || o instanceof MailboxMaintenance) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns TRUE if the specified mailbox is in-memory and not in maintenance mode, if false, then caller can assume
     * that one of the {@link Listener} APIs be called for this mailbox at some point in the future, if this mailbox is
     * ever accessed.
     */
    public synchronized boolean isMailboxLoadedAndAvailable(int mailboxId) {
        Object cached = cacheManager.getMailbox(mailboxId);
        if (cached == null) {
            return false;
        }
        if (cached instanceof MailboxMaintenance) {
            return ((MailboxMaintenance) cached).canAccess();
        } else {
            return true;
        }
    }

    /**
     * @throws ServiceException may be thrown by sub-classes
     */
    protected Mailbox instantiateMailbox(MailboxData data) throws ServiceException {
        return new Mailbox(data);
    }

    protected Mailbox instantiateExternalVirtualMailbox(MailboxData data) throws ServiceException {
        return new ExternalVirtualMailbox(data);
    }

    public MailboxMaintenance beginMaintenance(String accountId, int mailboxId) throws ServiceException {
        Mailbox mbox = getMailboxByAccountId(accountId, false);
        if (mbox == null) {
            synchronized (this) {
                if (cacheManager.getMailboxKey(accountId.toLowerCase()) == null) {
                    MailboxMaintenance maintenance = new MailboxMaintenance(accountId, mailboxId);
                    cacheManager.cacheMailbox(mailboxId, maintenance);
                    return maintenance;
                }
            }
            mbox = getMailboxByAccountId(accountId);
        }

        // mbox is non-null, and mbox.beginMaintenance() will throw if it's already in maintenance
        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
            MailboxMaintenance maintenance = mbox.beginMaintenance();
            synchronized (this) {
                cacheManager.cacheMailbox(mailboxId, maintenance);
            }
            return maintenance;
        }
    }

    public void endMaintenance(MailboxMaintenance maintenance, boolean success, boolean removeFromCache)
            throws ServiceException {
        Preconditions.checkNotNull(maintenance);

        Mailbox availableMailbox = null;

        synchronized (this) {
            Object obj = cacheManager.getMailbox(maintenance.getMailboxId());
            if (obj != maintenance) {
                ZimbraLog.mailbox.debug("maintenance ended with wrong object. passed %s; expected %s", maintenance, obj);
                throw MailServiceException.MAINTENANCE(maintenance.getMailboxId(), "attempting to end maintenance with wrong object");
            }
            // start by removing the lock from the Mailbox object cache
            cacheManager.removeMailbox(maintenance.getMailboxId());

            Mailbox mbox = maintenance.getMailbox();
            if (success) {
                // XXX: don't recall the rationale for re-setting this...
                cacheManager.cacheAccount(maintenance.getAccountId(), maintenance.getMailboxId());

                if (mbox != null) {
                    assert(maintenance == mbox.getMaintenance() || mbox.getMaintenance() == null); // restore case

                    if (removeFromCache) {
                        mbox.purge(MailItem.Type.UNKNOWN);
                        // We're going to let the Mailbox drop out of the cache and eventually get GC'd.
                        // Some immediate cleanup is necessary though.
                        IndexStore index = mbox.index.getIndexStore();
                        if (index != null) {
                            index.evict();
                        }
                        // Note: mbox is left in maintenance mode.
                    } else {
                        if (mbox.endMaintenance(success)) {
                            ZimbraLog.mailbox.debug("no longer in maintenace; caching mailbox");
                            cacheManager.cacheMailbox(maintenance.getMailbox(), this);
                        } else {
                            ZimbraLog.mailbox.debug("still in maintenance; caching lock");
                            cacheManager.cacheMailbox(mbox.getId(), mbox.getMaintenance());
                        }
                    }
                    availableMailbox = mbox;
                }
            } else {
                // on failed maintenance, mark the Mailbox object as off-limits to everyone
                if (mbox != null) {
                    mbox.endMaintenance(success);
                }
                maintenance.markUnavailable();
            }
        }

        if (availableMailbox != null)
            notifyMailboxAvailable(availableMailbox);
    }


    /**
     * Returns the total number of mailboxes on this host.
     * @return
     */
    public int getMailboxCount() {
        synchronized (this) {
            return cacheManager.getMailboxCount();
        }
    }


    /** Returns an array of all the mailbox IDs on this host in an undefined
     *  order. Note that <code>Mailbox</code>es are lazily created, so this is
     *  not the same as the set of mailboxes for accounts whose
     *  <code>zimbraMailHost</code> LDAP attribute points to this server. */
    public int[] getMailboxIds() {
        int i = 0;
        synchronized (this) {
            Collection<Integer> col = cacheManager.getMailboxIds();
            int[] mailboxIds = new int[col.size()];
            for (int id : col)
                mailboxIds[i++] = id;
            return mailboxIds;
        }
    }

    public Set<Integer> getPurgePendingMailboxes(long time) throws ServiceException {
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            return DbMailbox.listPurgePendingMailboxes(conn, time);
        } finally {
            DbPool.quietClose(conn);
        }

    }

    /** Returns an array of the account IDs of all the mailboxes on this host.
     *  Note that <code>Mailbox</code>es are lazily created, so this is not
     *  the same as the set of accounts whose <code>zimbraMailHost</code> LDAP
     *  attribute points to this server.*/
    public String[] getAccountIds() {
        int i = 0;
        synchronized (this) {
            Set<String> set = cacheManager.getAccountIds();
            String[] accountIds = new String[set.size()];
            for (String o : set)
                accountIds[i++] = o;
            return accountIds;
        }
    }


    /** Look up mailbox id by account id.
     *
     * @param accountId
     * @return
     */
    public int lookupMailboxId(String accountId) {
        Integer v;
        synchronized (this) {
            v = cacheManager.getMailboxKey(accountId);
        }
        return v != null ? v.intValue() : -1;
    }


    /** Returns the zimbra IDs and approximate sizes for all mailboxes on
     *  the system.  Note that mailboxes are created lazily, so there may be
     *  accounts homed on this system for whom there is is not yet a mailbox
     *  and hence are not included in the returned <code>Map</code>.
     *
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - an error occurred while accessing
     *        the database; a SQLException is encapsulated</ul> */
    public Map<String, Long> getMailboxSizes(List<NamedEntry> accounts) throws ServiceException {
        List<Integer> requested;
        synchronized (this) {
            if (accounts == null) {
                requested = new ArrayList<Integer>(cacheManager.getMailboxIds());
            } else {
                requested = new ArrayList<Integer>(accounts.size());
                for (NamedEntry account : accounts) {
                    Integer mailboxId = cacheManager.getMailboxKey(account.getId());
                    if (mailboxId != null)
                        requested.add(mailboxId);
                }
            }
        }

        DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            return DbMailbox.getMailboxSizes(conn, requested);
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }
    }


    /** Creates a <code>Mailbox</code> for the given {@link Account}, caches
     *  it for the lifetime of the server, inserts the default set of system
     *  folders, and returns it.  If the account's mailbox already exists on
     *  this sevrer, returns that and does not update the database.
     *
     * @param octxt    The context for this request (e.g. redo player).
     * @param account  The account to create a mailbox for.
     * @return A new or existing <code>Mailbox</code> object for the account.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.MAINTENANCE</code> - if the mailbox is in maintenance
     *        mode and the calling thread doesn't hold the lock
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.WRONG_HOST</code> - if the Account's mailbox
     *        lives on a different host</ul>
     * @see #initialize() */
    public Mailbox createMailbox(OperationContext octxt, Account account) throws ServiceException {
        return createMailbox(octxt, account, false);
    }

    private Mailbox createMailbox(OperationContext octxt, Account account, boolean skipMailHostCheck) throws ServiceException {
        if (account == null)
            throw ServiceException.FAILURE("createMailbox: must specify an account", null);
        if (!skipMailHostCheck && !Provisioning.onLocalServer(account))
            throw ServiceException.WRONG_HOST(account.getMailHost(), null);

        // the awkward structure here is to avoid calling getMailboxById while holding the lock
        Mailbox mbox = null;
        Integer mailboxKey = null;
        do {
            if (mailboxKey != null)
                return getMailboxById(mailboxKey);

            boolean isGalSyncAccount = AccountUtil.isGalSyncAccount(account);
            synchronized (this) {
                // check to make sure the mailbox doesn't already exist
                mailboxKey = cacheManager.getMailboxKey(account.getId().toLowerCase());
                if (mailboxKey != null)
                    continue;
                // check if the mailbox is created by other server after this server's startup
                DbConnection conn = null;
                try {
                    conn = DbPool.getConnection();
                    mailboxKey = DbMailbox.getMailboxId(conn, account.getId());
                    if (mailboxKey != null && mailboxKey > 0) {
                        cacheManager.cacheAccount(account.getId(), mailboxKey);
                        continue;
                    }
                } finally {
                    DbPool.quietClose(conn);
                }
                // didn't have the mailbox in the database; need to create one now
                mbox = createMailboxInternal(octxt, account, isGalSyncAccount);
            }
        } while (mbox == null);

        // now, make sure the mailbox is opened -- we do this after releasing the MailboxManager lock so that filesystem
        // IO and other longer operations don't block the system.
        if (mbox.open())
            notifyMailboxCreated(mbox);

        return mbox;
    }

    private synchronized Mailbox createMailboxInternal(OperationContext octxt, Account account, boolean isGalSyncAccount) throws ServiceException {
        CreateMailbox redoRecorder = new CreateMailbox(account.getId());

        Mailbox mbox = null;
        MailboxTransaction mboxTransaction = null;
        MailboxLock lock = null;
        DbConnection conn = DbPool.getConnection();
        try {
            CreateMailbox redoPlayer = (octxt == null ? null : (CreateMailbox) octxt.getPlayer());
            int id = (redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMailboxId());

            // create the mailbox row and the mailbox database
            MailboxData data;
            boolean created = false;
            try {
                data = DbMailbox.createMailbox(conn, id, account.getId(), account.getName(), -1);
                ZimbraLog.mailbox.info("Creating mailbox with id %d and group id %d for %s.", data.id, data.schemaGroupId, account.getName());
                created = true;
            } catch (ServiceException se) {
                if (MailServiceException.ALREADY_EXISTS.equals(se.getCode())) {
                    // mailbox for the account may be created by other server, re-fetch now.
                    id = DbMailbox.getMailboxId(conn, account.getId());
                    if (id > 0) {
                        data = DbMailbox.getMailboxStats(conn, id);
                    } else {
                        throw ServiceException.FAILURE("could not create mailbox", se);
                    }
                } else {
                    throw se;
                }
            }
            mbox = account.isIsExternalVirtualAccount() ?
                    instantiateExternalVirtualMailbox(data) : instantiateMailbox(data);
            mbox.setGalSyncMailbox(isGalSyncAccount);
            // the existing Connection is used for the rest of this transaction...
			lock = mbox.lock(true);
			mboxTransaction = mbox.new MailboxTransaction("createMailbox", octxt, lock, redoRecorder, conn);

            if (created) {
                // create the default folders
                mbox.initialize();
            }

            // cache the accountID-to-mailboxID and mailboxID-to-Mailbox relationships
            cacheManager.cacheAccount(data.accountId, data.id);
            cacheManager.cacheMailbox(mbox, this);
            redoRecorder.setMailboxId(mbox.getId());

            mboxTransaction.commit();
            
        } catch (ServiceException e) {
            // Log exception here, just in case.  If badness happens during rollback
            // the original exception will be lost.
            ZimbraLog.mailbox.error("Error during mailbox creation", e);
            throw e;
        } catch (OutOfMemoryError e) {
            Zimbra.halt("out of memory", e);
        } catch (Throwable t) {
            ZimbraLog.mailbox.error("Error during mailbox creation", t);
            throw ServiceException.FAILURE("createMailbox", t);
        } finally {
            try {
                if (mboxTransaction != null) {
                    mboxTransaction.close();
                } else {
                    conn.rollback();
                }
                if (lock != null) {
                    lock.close();
                }
            } finally {
                conn.closeQuietly();
            }
        }

        return mbox;
    }

    protected void markMailboxDeleted(Mailbox mailbox) {
        String accountId = mailbox.getAccountId().toLowerCase();
        synchronized (this) {
            maintenanceLocks.remove(accountId);
            cacheManager.removeMailboxId(accountId);
            cacheManager.removeMailbox(mailbox.getId());
        }
        notifyMailboxDeleted(accountId);
    }

    public void dumpMailboxCache() {
        StringBuilder sb = new StringBuilder();
        sb.append("MAILBOX CACHE DUMPS\n");
        sb.append("----------------------------------------------------------------------\n");
        synchronized (this) {
            for (Map.Entry<String, Integer> entry : cacheManager.getMailboxIdsByAccountId())
                sb.append("1) key=" + entry.getKey() + " (hash=" + entry.getKey().hashCode() + "); val=" + entry.getValue() + "\n");
            for (Map.Entry<Integer, Object> entry : cacheManager.getMailboxesById())
                sb.append("2) key=" + entry.getKey() + "; val=" + entry.getValue() + "(class= " + entry.getValue().getClass().getName() + ",hash=" + entry.getValue().hashCode() + ")");
        }
        sb.append("----------------------------------------------------------------------\n");
        ZimbraLog.mailbox.debug(sb.toString());
    }

    public void lockoutMailbox(String accountId) throws ServiceException {
        ZimbraLog.mailbox.debug("locking out mailbox for account %s", accountId);
        Mailbox mbox = getMailboxByAccountId(accountId);
        MailboxMaintenance maintenance = beginMaintenance(accountId, mbox.getId());
        maintenance.setNestedAllowed(true);
        maintenance.removeAllowedThread(Thread.currentThread());
        maintenanceLocks.put(mbox.getAccountId(), maintenance);
    }

    public void undoLockout(String accountId, boolean endMaintenance) throws ServiceException {
        ZimbraLog.mailbox.debug("undoing lockout for account %s", accountId);
        MailboxMaintenance maintenance = maintenanceLocks.remove(accountId);
        if (maintenance == null) {
            throw ServiceException.NOT_FOUND("No lock known for account " + accountId, null);
        }
        if (endMaintenance) {
            maintenance.registerAllowedThread(Thread.currentThread());
            endMaintenance(maintenance, true, false);
        }
    }

    public void undoLockout(String accountId) throws ServiceException {
        undoLockout(accountId, true);
    }

    public boolean isMailboxLockedOut(String accountId) {
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("Checking is locked for account %s? %s", accountId, maintenanceLocks.containsKey(accountId));
        }
        return maintenanceLocks.containsKey(accountId);
    }

    public void registerOuterMaintenanceThread(String accountId) throws MailServiceException {
        ZimbraLog.mailbox.debug("registering maintenance thread for account %s", accountId);
        MailboxMaintenance maintenance = maintenanceLocks.get(accountId);
        if (maintenance != null) {
            maintenance.registerOuterAllowedThread(Thread.currentThread());
        }
    }

    public void unregisterMaintenanceThread(String accountId) {
        ZimbraLog.mailbox.debug("unregistering maintenance thread for account %s", accountId);
        MailboxMaintenance maintenance = maintenanceLocks.get(accountId);
        if (maintenance != null) {
            maintenance.removeAllowedThread(Thread.currentThread());
        }
    }
}
