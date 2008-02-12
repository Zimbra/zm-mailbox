/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbMailbox.NewMboxId;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.redolog.op.CreateMailbox;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class MailboxManager {

    public static enum FetchMode {
        AUTOCREATE, // create the mailbox if it doesn't exist
        DO_NOT_AUTOCREATE, // fetch from DB if not in memory, but don't create it if it isn't in the DB
        ONLY_IF_CACHED, // don't fetch from the DB, only return if cached
        ; 
    }
    
    /**
     * Listener for mailbox loading
     */
    public static interface Listener {
        /** Called whenever a mailbox has left Maintenance mode */
        public void mailboxAvailable(Mailbox mbox) throws ServiceException;
        
        /** Called whenever a mailbox is loaded */
        public void mailboxLoaded(Mailbox mbox) throws ServiceException;
        
        /** Called whenever a mailbox is created */
        public void mailboxCreated(Mailbox mbox) throws ServiceException;
        
    }

    public static final class MailboxLock {
        private final String accountId;
        private final int    mailboxId;
        private Mailbox mailbox;
        private List<Thread> allowedThreads;

        MailboxLock(String acct, int id)  { this(acct, id, null); }
        MailboxLock(String acct, int id, Mailbox mbox) {
            accountId = acct.toLowerCase();  mailboxId = id;
            mailbox = mbox;
            allowedThreads = new ArrayList<Thread>();
            allowedThreads.add(Thread.currentThread());
        }

        String getAccountId()  { return accountId; }
        int getMailboxId()     { return mailboxId; }
        Mailbox getMailbox()   { return mailbox; }

        public synchronized void registerAllowedThread(Thread t) {
            allowedThreads.add(t);
        }

        synchronized boolean canAccess() {
            Thread curr = Thread.currentThread();
            for (Thread t : allowedThreads) {
                if (curr == t)
                    return true;
            }
            return false;
        }

        synchronized void markUnavailable()  {
            mailbox = null;
            allowedThreads.clear();
        }
        void cacheMailbox(Mailbox mbox) {
            if (mbox.getId() == mailboxId && mbox.getAccountId().equalsIgnoreCase(accountId))
                mailbox = mbox;
        }
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

    private void notifyMailboxAvailable(Mailbox mbox) throws ServiceException {
        for (Listener listener : mListeners) 
            listener.mailboxAvailable(mbox);
    }
    
    private void notifyMailboxLoaded(Mailbox mbox) throws ServiceException {
        for (Listener listener : mListeners) 
            listener.mailboxLoaded(mbox);
    }
    
    private void notifyMailboxCreated(Mailbox mbox) throws ServiceException {
        for (Listener listener : mListeners) 
            listener.mailboxCreated(mbox);
    }
    

    private static MailboxManager sInstance;

    /** Maps account IDs (<code>String</code>s) to mailbox IDs
     *  (<code>Integer</code>s).  <i>Every</i> mailbox in existence on the
     *  server appears in this mapping. */
    private Map<String, Integer> mMailboxIds;

    /** Maps mailbox IDs (<code>Integer</code>s) to either <ul>
     *     <li>a {@link SoftReference} to a loaded <code>Mailbox</code>, or
     *     <li>a {@link MailboxLock} for the mailbox.</ul><p>
     *  Mailboxes are faulted into memory as needed, but may drop from memory
     *  when the SoftReference expires due to memory pressure combined with a
     *  lack of outstanding references to the <code>Mailbox</code>.  Only one
     *  <code>Mailbox</code> per user is cached, and only that
     *  <code>Mailbox</code> can process user requests. */
    private Map<Integer, Object> mMailboxCache;

    public MailboxManager() throws ServiceException {
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            mMailboxIds = DbMailbox.getMailboxes(conn);
            mMailboxCache = new HashMap<Integer, Object>();
        } finally {
            DbPool.quietClose(conn);
        }
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

    public static void setInstance(MailboxManager mmgr) {
        sInstance = mmgr;
    }

    public void startup()   { }
    public void shutdown()  { }


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
        if (account == null)
            throw new IllegalArgumentException();
        return getMailboxByAccountId(account.getId());
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
     *  is <code>true</code>.
     *
     * @param accountId   The id of the account whose mailbox we want.
     * @param autocreate  <code>true</code> to create the mailbox if needed,
     *                    <code>false</code> to just return <code>null</code>
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
     *  mailbox if one doesn't already exist and <code>autocreate</code>
     *  is <code>true</code>.
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
        if (accountId == null)
            throw new IllegalArgumentException();
    
        Integer mailboxKey;
        synchronized (this) {
            mailboxKey = mMailboxIds.get(accountId.toLowerCase());
        }
        if (mailboxKey != null)
            return getMailboxById(mailboxKey.intValue(), fetchMode, false);
        else if (fetchMode != FetchMode.AUTOCREATE)
            return null;
    
        // auto-create the mailbox if this is the right host...
        Account account = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        if (!Provisioning.onLocalServer(account))
            throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);

        synchronized (this) {
            mailboxKey = mMailboxIds.get(accountId.toLowerCase());
        }
        if (mailboxKey != null)
            return getMailboxById(mailboxKey.intValue(), fetchMode, false);
        else
            return createMailbox(null, account);
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
    
    private Mailbox getMailboxById(int mailboxId, FetchMode fetchMode, boolean skipMailHostCheck)
    throws ServiceException {
        
        // see bug 19088 - we do NOT want to call this while holding the mgr lock, because
        // we need the Mailbox instantiation code to run w/o the lock held.
        assert(!Thread.holdsLock(this));
        
        if (mailboxId <= 0)
            throw MailServiceException.NO_SUCH_MBOX(mailboxId);

        long startTime = ZimbraPerf.STOPWATCH_MBOX_GET.start();
        
        synchronized (this) {
            // check to see if the mailbox has already been cached
            Object cached = retrieveFromCache(mailboxId, true);
            if (cached instanceof Mailbox) {
                ZimbraPerf.STOPWATCH_MBOX_GET.stop(startTime);
                ZimbraPerf.COUNTER_MBOX_CACHE.increment(100);
                return (Mailbox) cached;
            }
        }

        if (fetchMode == FetchMode.ONLY_IF_CACHED)
            return null;

        ZimbraPerf.COUNTER_MBOX_CACHE.increment(0);
        Connection conn = null;
        MailboxData data;
        try {
            // fetch the Mailbox data from the database
            conn = DbPool.getConnection();
            data = DbMailbox.getMailboxStats(conn, mailboxId);
            if (data == null)
                throw MailServiceException.NO_SUCH_MBOX(mailboxId);
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }

        Mailbox mbox = instantiateMailbox(data);

        if (!skipMailHostCheck) {
            // The host check here makes sure that sessions that were
            // already connected at the time of mailbox move are not
            // allowed to continue working with this mailbox which is
            // essentially a soft-deleted copy.  The WRONG_HOST
            // exception forces the clients to reconnect to the new
            // server.
            Account account = mbox.getAccount();
            if (!Provisioning.onLocalServer(account))
                throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);
        }

        synchronized (this) {
            // avoid the race condition by re-checking the cache and using that data (if any)
            Object cached = retrieveFromCache(mailboxId, false);
            if (cached instanceof Mailbox) {
            	mbox = (Mailbox)cached;
            } else {
                // cache the newly-created Mailbox object
                if (cached instanceof MailboxLock)
                    ((MailboxLock) cached).cacheMailbox(mbox);
                else
                    cacheMailbox(mbox);
            }
        }

        // now, make sure the mailbox is initialized -- we do this after releasing 
        // the Mgr lock so that filesystem IO and other longer operations don't 
        // block the system
        if (mbox.finishInitialization())
            // if TRUE, then this was the mailbox's first initialization, so we need to
            // notify listeners of the mailbox being loaded
            notifyMailboxLoaded(mbox);

        ZimbraPerf.STOPWATCH_MBOX_GET.stop(startTime);
        return mbox;
    }
    
    /** @return A list of *hard references* to all currently-loaded mailboxes which are not
     *     .   in MAINTENANCE mode.  Caller must be careful to not hang onto this list for
     *         very long or else mailboxes will not be purged. */
    public synchronized List<Mailbox> getAllLoadedMailboxes() {
        List<Mailbox> mboxes = new ArrayList<Mailbox>(mMailboxCache.size());
        for (Object o : mMailboxCache.values()) {
            if (o instanceof SoftReference) {
                Mailbox mbox = (Mailbox) ((SoftReference) o).get();
                if (mbox != null)
                    mboxes.add(mbox);
            } else if (o instanceof Mailbox) {
                mboxes.add((Mailbox)o);
            } else if (o instanceof MailboxLock) {
                MailboxLock lock = (MailboxLock)o;
                if (lock.canAccess()) {
                    mboxes.add(lock.getMailbox());
                } 
            }
        }
        return mboxes;
    }
    
    /**
     * @param mailboxId
     * @return TRUE if the specified mailbox is in-memory and not in maintenance mode,
     *         if false, then caller can assume that one of the @link{Listener} APIs 
     *         be called for this mailbox at some point in the future, if this mailbox
     *         is ever accessed
     */
    public synchronized boolean isMailboxLoadedAndAvailable(int mailboxId) {
        Object cached = mMailboxCache.get(mailboxId);
        if (cached == null) {
            return false;
        }
        if (cached instanceof SoftReference) {
            Mailbox mbox = (Mailbox) ((SoftReference) cached).get();
            return (mbox != null);
        } else if (cached instanceof MailboxLock) {
            MailboxLock lock = (MailboxLock) cached;
            return (lock.canAccess());
        } else {
            return true;
        }
    }

    private Object retrieveFromCache(int mailboxId, boolean trackGC) throws MailServiceException {
        synchronized (this) {
            Object cached = mMailboxCache.get(mailboxId);
            if (cached instanceof SoftReference) {
                Mailbox mbox = (Mailbox) ((SoftReference) cached).get();
                if (mbox == null && trackGC)
                    ZimbraLog.mailbox.debug("mailbox " + mailboxId + " has been GCed; reloading");
                return mbox;
            } else if (cached instanceof MailboxLock) {
                MailboxLock lock = (MailboxLock) cached;
                if (!lock.canAccess())
                    throw MailServiceException.MAINTENANCE(mailboxId);
                if (lock.getMailbox() != null)
                    return lock.getMailbox();
            }
            // if we've retrieved NULL or a Mailbox or an accessible lock, return it
            return cached;
        }
    }

    Mailbox instantiateMailbox(MailboxData data) throws ServiceException {
        return new Mailbox(data);
    }

    private Mailbox cacheMailbox(Mailbox mailbox) {
        if (LC.zimbra_mailbox_purgeable.booleanValue())
            mMailboxCache.put(mailbox.getId(), new SoftReference<Mailbox>(mailbox));
        else
            mMailboxCache.put(mailbox.getId(), mailbox);
        return mailbox;
    }


    public MailboxLock beginMaintenance(String accountId, int mailboxId) throws ServiceException {
        Mailbox mbox = getMailboxByAccountId(accountId, false);
        if (mbox == null) {
            synchronized (this) {
                if (mMailboxIds.get(accountId.toLowerCase()) == null) {
                    MailboxLock lock = new MailboxLock(accountId, mailboxId);
                    mMailboxCache.put(mailboxId, lock);
                    return lock;
                }
            }
            mbox = getMailboxByAccountId(accountId);
        }
    
        // mbox is non-null, and mbox.beginMaintenance() will throw if it's already in maintenance
        synchronized (mbox) {
            MailboxLock lock = mbox.beginMaintenance();
            synchronized (this) {
                mMailboxCache.put(mailboxId, lock);
            }
            return lock;
        }
    }

    public void endMaintenance(MailboxLock lock, boolean success, boolean removeFromCache) throws ServiceException {
        if (lock == null)
            throw ServiceException.INVALID_REQUEST("no lock provided", null);
        
        Mailbox availableMailbox = null;
    
        synchronized (this) {
            Object obj = mMailboxCache.get(lock.getMailboxId());
            if (obj != lock)
                throw MailServiceException.MAINTENANCE(lock.getMailboxId());

            // start by removing the lock from the Mailbox object cache
            mMailboxCache.remove(lock.getMailboxId());

            Mailbox mbox = lock.getMailbox();
            if (success) {
                // XXX: don't recall the rationale for re-setting this...
                mMailboxIds.put(lock.getAccountId().toLowerCase(), lock.getMailboxId());

                if (mbox != null) {
                    assert(lock == mbox.getMailboxLock() ||
                           mbox.getMailboxLock() == null);  // restore case
    
                    // Backend data may have changed while mailbox was in
                    // maintenance mode.  Invalidate all caches.
                    mbox.purge(MailItem.TYPE_UNKNOWN);
    
                    if (removeFromCache) {
                        // We're going to let the Mailbox drop out of the
                        // cache and eventually get GC'd.  Some immediate
                        // cleanup is necessary though.
                        MailboxIndex mi = mbox.getMailboxIndex();
                        if (mi != null)
                            mi.flush();
                        // Note: mbox is left in maintenance mode.
                    } else {
                        mbox.endMaintenance(success);
                        cacheMailbox(lock.getMailbox());
                    }
                    availableMailbox = mbox;
                }
            } else {
                // on failed maintenance, mark the Mailbox object as off-limits to everyone
                if (mbox != null)
                    mbox.endMaintenance(success);
                lock.markUnavailable();
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
            return mMailboxIds.size();
        }
    }


    /** Returns an array of all the mailbox IDs on this host in an undefined
     *  order. Note that <code>Mailbox</code>es are lazily created, so this is
     *  not the same as the set of mailboxes for accounts whose
     *  <code>zimbraMailHost</code> LDAP attribute points to this server. */
    public int[] getMailboxIds() {
        int i = 0;
        synchronized (this) {
            Collection<Integer> col = mMailboxIds.values();
            int[] mailboxIds = new int[col.size()];
            for (int o : col)
                mailboxIds[i++] = o;
            return mailboxIds;
        }
    }


    /** Returns an array of the account IDs of all the mailboxes on this host.
     *  Note that <code>Mailbox</code>es are lazily created, so this is not
     *  the same as the set of accounts whose <code>zimbraMailHost</code> LDAP
     *  attribute points to this server.*/
    public String[] getAccountIds() {
        int i = 0;
        synchronized (this) {
            Set<String> set = mMailboxIds.keySet();
            String[] accountIds = new String[set.size()];
            for (String o : set)
                accountIds[i++] = o;
            return accountIds;
        }
    }


    /** Returns the zimbra IDs and approximate sizes for all mailboxes on
     *  the system.  Note that mailboxes are created lazily, so there may be
     *  accounts homed on this system for whom there is is not yet a mailbox
     *  and hence are not included in the returned <code>Map</code>.  Sizes
     *  are checkpointed frequently, but there is no guarantee that the
     *  approximate sizes are currently accurate.
     *  
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - an error occurred while accessing
     *        the database; a SQLException is encapsulated</ul> */
    public Map<String, Long> getMailboxSizes() throws ServiceException {
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            return DbMailbox.getMailboxSizes(conn);
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
    public Mailbox createMailbox(Mailbox.OperationContext octxt, Account account) throws ServiceException {
        if (account == null)
            throw ServiceException.FAILURE("createMailbox: must specify an account", null);
        if (!Provisioning.onLocalServer(account))
            throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);
    
        Mailbox mailbox = null;
    
        synchronized (this) {
            // check to make sure the mailbox doesn't already exist
            Integer mailboxKey = mMailboxIds.get(account.getId().toLowerCase());
            if (mailboxKey != null)
                return getMailboxById(mailboxKey.intValue());

            // didn't have the mailbox in the database; need to create one now
            CreateMailbox redoRecorder = new CreateMailbox(account.getId());
    
            Connection conn = null;
            MailboxData data = null;
            boolean success = false;
            try {
                conn = DbPool.getConnection();
                data = new MailboxData();
                data.accountId = account.getId();
    
                // create the mailbox row and the mailbox database
                CreateMailbox redoPlayer = (octxt == null ? null : (CreateMailbox) octxt.getPlayer());
                int id = (redoPlayer == null ? Mailbox.ID_AUTO_INCREMENT : redoPlayer.getMailboxId());

                NewMboxId newMboxId = DbMailbox.createMailbox(conn, id, data.accountId, account.getName(), -1);
                data.id = newMboxId.id;
                data.schemaGroupId = newMboxId.groupId;
                DbMailbox.createMailboxDatabase(conn, data.id, data.schemaGroupId);

                // The above initialization of data is incomplete because it
                // is missing the message/index volume information.  Query
                // the database to get it.
                DbMailbox.getMailboxVolumeInfo(conn, data);

                mailbox = instantiateMailbox(data);
                // the existing Connection is used for the rest of this transaction...
                mailbox.beginTransaction("createMailbox", octxt, redoRecorder, conn);
                // create the default folders
                mailbox.initialize();

                // cache the accountID-to-mailboxID and mailboxID-to-Mailbox relationships
                mMailboxIds.put(data.accountId.toLowerCase(), new Integer(data.id));
                cacheMailbox(mailbox);
                redoRecorder.setMailboxId(mailbox.getId());

                success = true;
            } catch (ServiceException e) {
                // Log exception here, just in case.  If badness happens during rollback
                // the original exception will be lost.
                ZimbraLog.mailbox.error("Error during mailbox creation", e);
                throw e;
            } catch (Throwable t) {
                ZimbraLog.mailbox.error("Error during mailbox creation", t);
                throw ServiceException.FAILURE("createMailbox", t);
            } finally {
                try {
                    if (mailbox != null) {
                        mailbox.endTransaction(success);
                        conn = null;
                    } else {
                        if (conn != null)
                            conn.rollback();
                    }
                } finally {
                    if (conn != null)
                        DbPool.quietClose(conn);
                }
            }
        }

        // now, make sure the mailbox is initialized -- we do this after releasing 
        // the Mgr lock so that filesystem IO and other longer operations don't 
        // block the system
        if (mailbox.finishInitialization())
            notifyMailboxCreated(mailbox);
        
        return mailbox;
    }

    void markMailboxDeleted(Mailbox mailbox) {
        synchronized (this) {
            mMailboxIds.remove(mailbox.getAccountId().toLowerCase());
            mMailboxCache.remove(mailbox.getId());
        }
    }


    public void dumpMailboxCache() {
        StringBuilder sb = new StringBuilder();
        sb.append("MAILBOX CACHE DUMPS\n");
        sb.append("----------------------------------------------------------------------\n");
        synchronized (this) {
            for (Map.Entry<String,Integer> entry : mMailboxIds.entrySet())
                sb.append("1) key=" + entry.getKey() + " (hash=" + entry.getKey().hashCode() + "); val=" + entry.getValue() + "\n");
            for (Map.Entry<Integer,Object> entry : mMailboxCache.entrySet())
                sb.append("2) key=" + entry.getKey() + "; val=" + entry.getValue() + "(class= " + entry.getValue().getClass().getName() + ",hash=" + entry.getValue().hashCode() + ")");
        }
        sb.append("----------------------------------------------------------------------\n");
        ZimbraLog.mailbox.debug(sb.toString());
    }

}
