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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import static com.zimbra.cs.service.mail.WaitSetRequest.TypeEnum.t;
import static org.bouncycastle.asn1.x500.style.RFC4519Style.l;

public class MailboxManager {

    private static MailboxManager sInstance;

    /** Maps account IDs (<code>String</code>s) to mailbox IDs
     *  (<code>Integer</code>s).  <i>Every</i> mailbox in existence on the
     *  server appears in this mapping. */
    private Map<String, Integer> mailboxIds;

    /**
     * Maps mailbox IDs ({@link Integer}s) to either
     * <ul>
     *  <li>a loaded {@link Mailbox}, or
     *  <li>a {@link SoftReference} to a loaded {@link Mailbox}, or
     *  <li>a {@link MaintenanceContext} for the mailbox.
     * </ul>
     * Mailboxes are faulted into memory as needed, but may drop from memory when the SoftReference expires due to
     * memory pressure combined with a lack of outstanding references to the {@link Mailbox}.  Only one {@link Mailbox}
     * per user is cached, and only that {@link Mailbox} can process user requests.
     */
    private MailboxMap cache;

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

    public MailboxManager() throws ServiceException {
        DbConnection conn = null;
        synchronized (this) {
            try {
                conn = DbPool.getConnection();
                mailboxIds = DbMailbox.listMailboxes(conn, this);
                cache = createCache();
            } finally {
                DbPool.quietClose(conn);
            }
        }
    }

    protected MailboxMap createCache() {
        return new MailboxMap(LC.zimbra_mailbox_manager_hardref_cache.intValue());
    }

    /**
     * TODO: In order to allow sub-classing MailboxManager, MailboxManager
     * should be interface instead of concrete class. Until then, this dummy
     * constructor allows subclasses not to rely on DB.
     *
     * @param extend dummy
     */
    protected MailboxManager(boolean extend) {
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

    @VisibleForTesting
    public void clearCache() {
        cache.clear();
        mailboxIds.clear();
    }

    public void startup() {
        //NOOP
    }

    public void shutdown() {
        //NOOP
    }

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
            mailboxKey = mailboxIds.get(accountId.toLowerCase());
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
            mailboxKey = mailboxIds.get(accountId.toLowerCase());
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
            Object cached = retrieveFromCache(mailboxId, true);
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
                Object cached = retrieveFromCache(mailboxId, false);
                if (cached instanceof Mailbox) {
                    mbox = (Mailbox) cached;
                } else {
                    // cache the newly-created Mailbox object
                    if (cached instanceof MailboxMaintenance) {
                        ((MailboxMaintenance) cached).setMailbox(mbox);
                    } else {
                        cacheMailbox(mbox);
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
                    cache.put(mailboxId, maint);
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
        List<Mailbox> mboxes = new ArrayList<Mailbox>(cache.size());
        for (Object o : cache.values()) {
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
        for (Object o : cache.values()) {
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
        Object cached = cache.get(mailboxId);
        if (cached == null) {
            return false;
        }
        if (cached instanceof MailboxMaintenance) {
            return ((MailboxMaintenance) cached).canAccess();
        } else {
            return true;
        }
    }

    private Object retrieveFromCache(int mailboxId, boolean trackGC) throws MailServiceException {
        synchronized (this) {
            Object cached = cache.get(mailboxId, trackGC);
            if (cached instanceof MailboxMaintenance) {
                MailboxMaintenance maintenance = (MailboxMaintenance) cached;
                if (!maintenance.canAccess()) {
                    if (isMailboxLockedOut(maintenance.getAccountId())) {
                        throw MailServiceException.MAINTENANCE(mailboxId, "mailbox locked out for maintenance");
                    } else {
                        throw MailServiceException.MAINTENANCE(mailboxId);
                    }
                }
                if (maintenance.getMailbox() != null) {
                    return maintenance.getMailbox();
                }
            }
            // if we've retrieved NULL or a Mailbox or an accessible lock, return it
            return cached;
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

    protected synchronized void cacheAccount(String accountId, int mailboxId) {
        mailboxIds.put(accountId.toLowerCase(), Integer.valueOf(mailboxId));
    }

    private Mailbox cacheMailbox(Mailbox mailbox) {
        cache.put(mailbox.getId(), mailbox);
        return mailbox;
    }

    public MailboxMaintenance beginMaintenance(String accountId, int mailboxId) throws ServiceException {
        Mailbox mbox = getMailboxByAccountId(accountId, false);
        if (mbox == null) {
            synchronized (this) {
                if (mailboxIds.get(accountId.toLowerCase()) == null) {
                    MailboxMaintenance maintenance = new MailboxMaintenance(accountId, mailboxId);
                    cache.put(mailboxId, maintenance);
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
                cache.put(mailboxId, maintenance);
            }
            return maintenance;
        }
    }

    public void endMaintenance(MailboxMaintenance maintenance, boolean success, boolean removeFromCache)
            throws ServiceException {
        Preconditions.checkNotNull(maintenance);

        Mailbox availableMailbox = null;

        synchronized (this) {
            Object obj = cache.get(maintenance.getMailboxId());
            if (!obj.equals(maintenance)) {
                ZimbraLog.mailbox.debug("maintenance ended with wrong object. passed %s; expected %s", maintenance, obj);
                throw MailServiceException.MAINTENANCE(maintenance.getMailboxId(), "attempting to end maintenance with wrong object");
            }
            // start by removing the lock from the Mailbox object cache
            cache.remove(maintenance.getMailboxId());

            Mailbox mbox = maintenance.getMailbox();
            if (success) {
                // XXX: don't recall the rationale for re-setting this...
                cacheAccount(maintenance.getAccountId(), maintenance.getMailboxId());

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
                            cacheMailbox(maintenance.getMailbox());
                        } else {
                            ZimbraLog.mailbox.debug("still in maintenance; caching lock");
                            cache.put(mbox.getId(), mbox.getMaintenance());
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
            return mailboxIds.size();
        }
    }


    /** Returns an array of all the mailbox IDs on this host in an undefined
     *  order. Note that <code>Mailbox</code>es are lazily created, so this is
     *  not the same as the set of mailboxes for accounts whose
     *  <code>zimbraMailHost</code> LDAP attribute points to this server. */
    public int[] getMailboxIds() {
        int i = 0;
        synchronized (this) {
            Collection<Integer> col = mailboxIds.values();
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
            Set<String> set = mailboxIds.keySet();
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
            v = mailboxIds.get(accountId);
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
                requested = new ArrayList<Integer>(mailboxIds.values());
            } else {
                requested = new ArrayList<Integer>(accounts.size());
                for (NamedEntry account : accounts) {
                    Integer mailboxId = mailboxIds.get(account.getId());
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
                mailboxKey = mailboxIds.get(account.getId().toLowerCase());
                if (mailboxKey != null)
                    continue;
                // check if the mailbox is created by other server after this server's startup
                DbConnection conn = null;
                try {
                    conn = DbPool.getConnection();
                    mailboxKey = DbMailbox.getMailboxId(conn, account.getId());
                    if (mailboxKey != null && mailboxKey > 0) {
                        cacheAccount(account.getId(), mailboxKey);
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
        Mailbox.MailboxTransaction mboxTransaction = null;
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
            cacheAccount(data.accountId, data.id);
            cacheMailbox(mbox);
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
            mailboxIds.remove(accountId);
            cache.remove(mailbox.getId());
        }
        notifyMailboxDeleted(accountId);
    }

    public void dumpMailboxCache() {
        StringBuilder sb = new StringBuilder();
        sb.append("MAILBOX CACHE DUMPS\n");
        sb.append("----------------------------------------------------------------------\n");
        synchronized (this) {
            for (Map.Entry<String, Integer> entry : mailboxIds.entrySet())
                sb.append("1) key=" + entry.getKey() + " (hash=" + entry.getKey().hashCode() + "); val=" + entry.getValue() + "\n");
            for (Map.Entry<Integer, Object> entry : cache.entrySet())
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

    protected static class MailboxMap implements Map<Integer, Object> {
        final int mHardSize;
        final LinkedHashMap<Integer, Object> mHardMap;
        final HashMap<Integer, Object> mSoftMap;

        @SuppressWarnings("serial") MailboxMap(int hardSize) {
            mHardSize = Math.max(hardSize, 0);
            mSoftMap = new HashMap<Integer, Object>();
            mHardMap = new LinkedHashMap<Integer, Object>(mHardSize / 4, (float) .75, true) {
                @Override protected boolean removeEldestEntry(Entry<Integer, Object> eldest) {
                    if (size() <= mHardSize)
                        return false;

                    Object obj = eldest.getValue();
                    if (obj instanceof Mailbox)
                        obj = new SoftReference<Mailbox>((Mailbox) obj);
                    mSoftMap.put(eldest.getKey(), obj);
                    return true;
                }
            };
        }

        protected MailboxMap() {
            mHardSize = 0;
            mHardMap = null;
            mSoftMap = null;
        }

        @Override public void clear() {
            mHardMap.clear();
            mSoftMap.clear();
        }

        @Override public boolean containsKey(Object key) {
            return mHardMap.containsKey(key) || mSoftMap.containsKey(key);
        }

        @Override public boolean containsValue(Object value) {
            return mHardMap.containsValue(value) || mSoftMap.containsValue(value);
        }

        @Override public Set<Entry<Integer, Object>> entrySet() {
            Set<Entry<Integer, Object>> entries = new HashSet<Entry<Integer, Object>>(size());
            if (mHardSize > 0)
                entries.addAll(mHardMap.entrySet());
            entries.addAll(mSoftMap.entrySet());
            return entries;
        }

        @Override public Object get(Object key) {
            return get(key, false);
        }

        public Object get(Object key, boolean trackGC) {
            Object obj = mHardSize > 0 ? mHardMap.get(key) : null;
            if (obj == null) {
                obj = mSoftMap.get(key);
                if (obj instanceof SoftReference) {
                    obj = ((SoftReference<?>) obj).get();
                    if (trackGC && obj == null)
                        ZimbraLog.mailbox.debug("mailbox " + key + " has been GCed; reloading");
                }
            }
            return obj;
        }

        @Override public boolean isEmpty() {
            return mHardMap.isEmpty() && mSoftMap.isEmpty();
        }

        @Override public Set<Integer> keySet() {
            Set<Integer> keys = new HashSet<Integer>(size());
            if (mHardSize > 0)
                keys.addAll(mHardMap.keySet());
            keys.addAll(mSoftMap.keySet());
            return keys;
        }

        @Override public Object put(Integer key, Object value) {
            Object removed;
            if (mHardSize > 0) {
                removed = mHardMap.put(key, value);
                if (removed == null)
                    removed = mSoftMap.remove(key);
            } else {
                if (value instanceof Mailbox)
                    value = new SoftReference<Object>(value);
                removed = mSoftMap.put(key, value);
            }
            if (removed instanceof SoftReference)
                removed = ((SoftReference<?>) removed).get();
            return removed;
        }

        @Override public void putAll(Map<? extends Integer, ? extends Object> t) {
            for (Entry<? extends Integer, ? extends Object> entry : t.entrySet())
                put(entry.getKey(), entry.getValue());
        }

        @Override public Object remove(Object key) {
            Object removed = mHardSize > 0 ? mHardMap.remove(key) : null;
            if (removed == null) {
                removed = mSoftMap.remove(key);
                if (removed instanceof SoftReference)
                    removed = ((SoftReference<?>) removed).get();
            }
            return removed;
        }

        @Override public int size() {
            return mHardMap.size() + mSoftMap.size();
        }

        @Override public Collection<Object> values() {
            List<Object> values = new ArrayList<Object>(size());
            if (mHardSize > 0)
                values.addAll(mHardMap.values());
            for (Object o : mSoftMap.values()) {
                if (o instanceof SoftReference)
                    o = ((SoftReference<?>) o).get();
                values.add(o);
            }
            return values;
        }

        @Override public String toString() {
            return "<" + mHardMap.toString() + ", " + mSoftMap.toString() + ">";
        }
    }
}
