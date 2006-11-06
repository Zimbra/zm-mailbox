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
package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbMailbox.NewMboxId;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.redolog.op.CreateMailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class MailboxManager {

    public static final class MailboxLock {
        private final String accountId;
        private final int    mailboxId;
        private Thread  owner;
        private Mailbox mailbox;

        MailboxLock(String acct, int id)  { this(acct, id, null); }
        MailboxLock(String acct, int id, Mailbox mbox) {
            accountId = acct.toLowerCase();  mailboxId = id;
            owner = Thread.currentThread();  mailbox = mbox;
        }

        String getAccountId()  { return accountId; }
        int getMailboxId()     { return mailboxId; }
        Mailbox getMailbox()   { return mailbox; }

        boolean canAccess()     { return owner == Thread.currentThread(); }
        void markUnavailable()  { owner = null;  mailbox = null; }
        void cacheMailbox(Mailbox mbox) {
            if (mbox.getId() == mailboxId && mbox.getAccountId().equalsIgnoreCase(accountId))
                mailbox = mbox;
        }
    }


    private static MailboxManager sInstance;

    /** Maps account IDs (<code>String</code>s) to mailbox IDs
     *  (<code>Integer</code>s).  <i>Every</i> mailbox in existence on the
     *  server appears in this mapping. */
    private Map<String, Integer> mMailboxIds;

    /** Maps mailbox IDs (<code>Integer</code>s) to loaded
     *  <code>Mailbox</code>es.  Mailboxes are faulted into memory as needed,
     *  but are then cached for the life of the server or until explicitly
     *  unloaded.  Only one <code>Mailbox</code> per user is cached, and only
     *  that <code>Mailbox</code> can process user requests. */
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
                    sInstance = (MailboxManager) Class.forName(className).newInstance();
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

    public void shutdown() { }


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
        return getMailboxByAccountId(accountId, true);
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
        if (accountId == null)
            throw new IllegalArgumentException();
    
        Integer mailboxKey;
        synchronized (this) {
            mailboxKey = mMailboxIds.get(accountId.toLowerCase());
        }
        if (mailboxKey != null)
            return getMailboxById(mailboxKey.intValue());
        else if (!autocreate)
            return null;
    
        // auto-create the mailbox if this is the right host...
        Account account = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        if (!Provisioning.onLocalServer(account))
            throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);
        synchronized (this) {
            mailboxKey = mMailboxIds.get(accountId.toLowerCase());
            if (mailboxKey != null)
                return getMailboxById(mailboxKey.intValue());
            else
                return createMailbox(null, account);
        }
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
        return getMailboxById(mailboxId, false);
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
        if (mailboxId <= 0)
            throw MailServiceException.NO_SUCH_MBOX(mailboxId);
    
        synchronized (this) {
            Object obj = mMailboxCache.get(mailboxId);
            if (obj instanceof Mailbox) {
                return (Mailbox) obj;
            } else if (obj instanceof MailboxLock) {
                MailboxLock lock = (MailboxLock) obj;
                if (!lock.canAccess())
                    throw MailServiceException.MAINTENANCE(mailboxId);
                if (lock.getMailbox() != null)
                    return lock.getMailbox();
            }
    
            // fetch the Mailbox data from the database
            Connection conn = null;
            try {
                conn = DbPool.getConnection();

                MailboxData data = DbMailbox.getMailboxStats(conn, mailboxId);
                if (data == null)
                    throw MailServiceException.NO_SUCH_MBOX(mailboxId);
                Mailbox mailbox = instantiateMailbox(data);

                if (!skipMailHostCheck) {
                    // The host check here makes sure that sessions that were
                    // already connected at the time of mailbox move are not
                    // allowed to continue working with this mailbox which is
                    // essentially a soft-deleted copy.  The WRONG_HOST
                    // exception forces the clients to reconnect to the new
                    // server.
                    Account account = mailbox.getAccount();
                    if (!Provisioning.onLocalServer(account))
                        throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);
                }

                if (obj instanceof MailboxLock)
                    ((MailboxLock) obj).cacheMailbox(mailbox);
                else
                    mMailboxCache.put(mailboxId, mailbox);

                return mailbox;
            } finally {
                if (conn != null)
                    DbPool.quietClose(conn);
            }
        }
    }

    Mailbox instantiateMailbox(MailboxData data) throws ServiceException {
        return new Mailbox(data);
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
                    assert(lock == mbox.getMailboxLock());
    
                    // Backend data may have changed while mailbox was in
                    // maintenance mode.  Invalidate all caches.
                    mbox.purge(MailItem.TYPE_UNKNOWN);
    
                    if (removeFromCache) {
                        // We're going to let the Mailbox drop out of the
                        // cache and eventually get GC'd.  Some immediate
                        // cleanup is necessary though.
                        if (mbox.getMailboxIndex() != null)
                            mbox.getMailboxIndex().flush();
                        // Note: mbox is left in maintenance mode.
                    } else {
                        mbox.endMaintenance(success);
                        mMailboxCache.put(lock.getMailboxId(), lock.getMailbox());
                    }
                }
            } else {
                // on failed maintenance, mark the Mailbox object as off-limits to everyone
                if (mbox != null)
                    mbox.endMaintenance(success);
                lock.markUnavailable();
            }
        }
    }


    /** Returns an array of all the mailbox IDs on this host.  Note that
     *  <code>Mailbox</code>es are lazily created, so this is not the same
     *  as the set of mailboxes for accounts whose <code>zimbraMailHost</code>
     *  LDAP attribute points to this server. */
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
                CreateMailbox redoPlayer = (octxt == null ? null : (CreateMailbox) octxt.player);
                int id = (redoPlayer == null ? Mailbox.ID_AUTO_INCREMENT : redoPlayer.getMailboxId());

                NewMboxId newMboxId = DbMailbox.createMailbox(conn, id, data.accountId, account.getName());
                data.id = newMboxId.id;
                data.schemaGroupId = newMboxId.groupId;
                DbMailbox.createMailboxDatabase(data.id, data.schemaGroupId);

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
                mMailboxCache.put(new Integer(data.id), mailbox);
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

        return mailbox;
    }

    void markMailboxDeleted(Mailbox mbox) {
        synchronized (this) {
            mMailboxIds.remove(mbox.getAccountId().toLowerCase());
            mMailboxCache.remove(mbox.getId());
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
