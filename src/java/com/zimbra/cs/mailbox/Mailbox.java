/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 13, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbMailItem.SearchResult;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.BrowseResult.DomainItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.op.*;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.util.*;


/**
 * @author schemers
 */
public class Mailbox {

    /* these probably should be ints... */
    public static final String BROWSE_BY_DOMAINS     = "domains";
    public static final String BROWSE_BY_OBJECTS     = "objects";
    public static final String BROWSE_BY_ATTACHMENTS = "attachments";

    public static final int ID_AUTO_INCREMENT   = -1;
    public static final int ID_FOLDER_USER_ROOT = 1;
    public static final int ID_FOLDER_INBOX     = 2;
    public static final int ID_FOLDER_TRASH     = 3;
    public static final int ID_FOLDER_SPAM      = 4;
    public static final int ID_FOLDER_SENT      = 5;
    public static final int ID_FOLDER_DRAFTS    = 6;
    public static final int ID_FOLDER_CONTACTS  = 7;
    public static final int ID_FOLDER_TAGS      = 8;
    public static final int ID_FOLDER_CONVERSATIONS = 9;
    public static final int ID_FOLDER_CALENDAR  = 10;
    public static final int ID_FOLDER_ROOT      = 11;

    public static final int HIGHEST_SYSTEM_ID = 11;
    public static final int FIRST_USER_ID     = 256;

    static final int  ONE_MONTH_SECS   = 60 * 60 * 24 * 31;
    static final long ONE_MONTH_MILLIS = ONE_MONTH_SECS * 1000L;

    /** Static cache for mailboxes.  Contains two separate mappings:<ul>
     *    <li>Maps account IDs (<code>String</code>s) to mailbox IDs
     *        (<code>Integer</code>s).  <i>Every</i> mailbox in existence
     *        on the server appears in this mapping.
     *    <li>Maps mailbox IDs (<code>Integer</code>s) to loaded
     *        <code>Mailbox</code>es.  Mailboxes are faulted into memory
     *        as needed, but are then cached for the life of the server
     *        or until explicitly unloaded.  Only one <code>Mailbox</code>
     *        per user is cached, and only that <code>Mailbox</code> can
     *        process user requests.</ul>
     *  When new mailboxes are created, they are added to both mappings.
     *  When mailboxes are deleted, they are removed from both mappings. */
    private static Map sMailboxCache;
        static {
            try {
                loadMailboxMap();
            } catch (ServiceException e) {
                ZimbraLog.mailbox.fatal("when loading mailbox/account id relationship");
                throw new RuntimeException(e);
            }
        }

    public static final class MailboxLock {
        final String accountId;
        final int    mailboxId;
        Thread  owner;
        Mailbox mailbox;
        
        MailboxLock(String acct, int mbox)  { accountId = acct.toLowerCase();  mailboxId = mbox;  owner = Thread.currentThread(); }
    }

    public static final class MailboxData {
        public int     id;
        public String  accountId;
        public long    size;
        public int     contacts;
        public int     lastItemId;
        public int     lastChangeId;
        public long    lastChangeDate;
        public boolean trackSync;
        public Metadata config;
        public short   indexVolumeId;
    }

    private static final class MailboxChange {
        private static final int NO_CHANGE = -1;

        long       timestamp = System.currentTimeMillis();
        int        depth     = 0;
        boolean    active;
        Connection conn      = null;
        RedoableOp recorder  = null;
        Map        itemCache = null;
        MailItem   indexItem = null;
        Object     indexData = null;
        OperationContext octxt = null;
        TargetConstraint tcon  = null;

        Boolean  sync     = null;
        long     size     = NO_CHANGE;
        int      itemId   = NO_CHANGE;
        int      changeId = NO_CHANGE;
        int      contacts = NO_CHANGE;
        Metadata config   = null;

        PendingModifications mDirty = new PendingModifications();
        LinkedList mOtherDirtyStuff = new LinkedList();

        void setTimestamp(long millis)   {
            if (depth == 1)
                timestamp = millis;
        }

        void startChange(String caller, OperationContext ctxt, RedoableOp op) {
            active = true;
            if (depth++ == 0) {
                octxt = ctxt;
                recorder = op;
                if (ZimbraLog.mailbox.isDebugEnabled())
                    ZimbraLog.mailbox.debug("beginning operation: " + caller);
            } else
                if (ZimbraLog.mailbox.isDebugEnabled())
                    ZimbraLog.mailbox.debug("  increasing stack depth to " + depth + " (" + caller + ')');
        }
        boolean endChange() {
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                if (depth <= 1) {
                    if (ZimbraLog.mailbox.isDebugEnabled())                    
                        ZimbraLog.mailbox.debug("ending operation" + (recorder == null ? "" : ": " + StringUtil.getSimpleClassName(recorder)));
                } else {
                    if (ZimbraLog.mailbox.isDebugEnabled())
                        ZimbraLog.mailbox.debug("  decreasing stack depth to " + (depth - 1));
                }
            }
            return (--depth == 0);
        }
        boolean isActive()  { return active; }

        Connection getConnection() throws ServiceException {
            if (conn == null) {
                conn = DbPool.getConnection();
                if (ZimbraLog.mailbox.isDebugEnabled())
                    ZimbraLog.mailbox.debug("  fetching new DB connection");
            }
            return conn;
        }

        RedoableOp getRedoPlayer()   { return (octxt == null ? null : octxt.player); }
        RedoableOp getRedoRecorder() { return recorder; }

        void setIndexedItem(MailItem item, Object data)  { indexItem = item;  indexData = data; }

        void reset() {
            if (conn != null)
                DbPool.quietClose(conn);
            active = false;
            conn = null;  octxt = null;  tcon = null;
            depth = 0;
            size = changeId = itemId = contacts = NO_CHANGE;
            sync = null;  config = null;
            itemCache = null;  indexItem = null;  indexData = null;
            mDirty.clear();  mOtherDirtyStuff.clear();
            if (ZimbraLog.mailbox.isDebugEnabled())
                ZimbraLog.mailbox.debug("clearing change");
        }
    }

    public static final class OperationContext {
        public static final boolean CHECK_CREATED = false, CHECK_MODIFIED = true;

        Account    authuser;
        RedoableOp player;
        boolean    changetype = CHECK_CREATED;
        int        change = -1;

        public OperationContext(RedoableOp redoPlayer)  { player = redoPlayer; }
        public OperationContext(Account acct)           { authuser = acct; }
        public OperationContext(String accountId) throws ServiceException {
            authuser = Provisioning.getInstance().getAccountById(accountId);
            if (authuser == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        }

        public void setChangeConstraint(boolean checkCreated, int changeId) {
            changetype = checkCreated;  change = changeId;
        }

        long getTimestamp() {
            return (player == null ? System.currentTimeMillis() : player.getTimestamp());
        }
        int getChangeId() {
            return (player == null ? -1 : player.getChangeId());
        }
    }

    // TODO: figure out correct caching strategy
    private static final int MAX_ITEM_CACHE_WITH_LISTENERS    = 500;
    private static final int MAX_ITEM_CACHE_WITHOUT_LISTENERS = 30;
    private static final int MAX_MSGID_CACHE = 10;

    private int           mId;
    private MailboxData   mData;
    private MailboxChange mCurrentChange = new MailboxChange();

    private Map           mFolderCache;
    private Map           mTagCache;
    private SoftReference mItemCache      = new SoftReference(null);
    private LRUMap        mConvHashes     = new LRUMap(MAX_MSGID_CACHE);
    private LRUMap        mSentMessageIDs = new LRUMap(MAX_MSGID_CACHE);
    private Set           mListeners      = new HashSet();
    
    private MailboxLock  mMaintenance = null;
    private MailboxIndex mMailboxIndex = null;

    /** flag: messages sent by me */
    public Flag mSentFlag;
    /** flag: messages/contacts with attachments */
    public Flag mAttachFlag;
    /** flag: messages that have been replied to */
    public Flag mReplyFlag;
    /** flag: messages that have been forwarded */
    public Flag mForwardFlag;
    /** flag: messages that have been copied or that are copies */
    public Flag mCopiedFlag;
    /** flag: messages/contacts/etc. with the little red flag */
    public Flag mFlaggedFlag;
    /** flag: draft messages */
    public Flag mDraftFlag;
    /** flag: messages/folders/etc. in IMAP's "deleted-not-expunged" limbo state */
    public Flag mDeletedFlag;
    /** flag: messages that have read-receipt MDN sent */
    public Flag mNotifiedFlag;
    /** flag: unread messages */
    public Flag mUnreadFlag;
    /** flag: IMAP-subscribed folders */
    public Flag mSubscribeFlag;
    /** the full set of message flags, in order */
    final Flag[] mFlags = new Flag[31];


    private Mailbox(MailboxData data) throws ServiceException {
        mId   = data.id;
        mData = data;
        mData.lastChangeDate = System.currentTimeMillis();
        initFlags();
    }

    /** Returns the server-local numeric ID for this mailbox.  To get a
     *  system-wide, persistent unique identifier for the mailbox, use
     *  {@link #getAccountId()}. */
    public int getId() {
        return mId;
    }

    /** Returns the ID of this mailbox's Account.  This is a 36-character
     *  GUID, e.g. <code>"1b4e28ba-2fa1-11d2-883f-b9a761bde3fb"</code>.
     * 
     * @see #getAccount() */
    public String getAccountId() {
        return mData.accountId;
    }

    /** Returns the {@link Account} object for this mailbox's owner.  At
     *  present, each account can have at most one <code>Mailbox</code>. */
    public synchronized Account getAccount() throws ServiceException {
        Account acct = getAccount(getAccountId());
        if (acct != null)
            return acct;
        ZimbraLog.mailbox.warn("no account found in directory for mailbox " + mId +
                               " (was expecting " + getAccountId() + ')');
        throw AccountServiceException.NO_SUCH_ACCOUNT(mData.accountId);
    }

    /** Returns the {@link Account} object for the specified account ID.  This
     *  is just shorthand for {@link Provisioning#getAccountById(String)}.
     * 
     * @param accountId  The <code>zimbraId</code> to look up. */
    public static Account getAccount(String accountId) throws ServiceException {
        return Provisioning.getInstance().getAccountById(accountId);
    }


    /** Adds a {@link Session} to the set of listeners notified on Mailbox
     *  changes.
     * 
     * @param session  The Session registering for notifications.
     * @throws ServiceException  If the mailbox is in maintenance mode. */
    public synchronized void addListener(Session session) throws ServiceException {
        if (session == null)
            return;
        if (mMaintenance != null)
            throw MailServiceException.MAINTENANCE(mId);
        mListeners.add(session);
        if (ZimbraLog.mailbox.isDebugEnabled())
            ZimbraLog.mailbox.debug("adding listener: " + session);
    }

    /** Removes a {@link Session} from the set of listeners notified on
     *  Mailbox changes.
     * 
     * @param session  The listener to deregister for notifications. */
    public synchronized void removeListener(Session session) {
        mListeners.remove(session);
        if (ZimbraLog.mailbox.isDebugEnabled())
            ZimbraLog.mailbox.debug("clearing listener: " + session);
    }

    /** Cleans up and disconnects all {@link Session}s listening for
     *  notifications on this Mailbox.
     * 
     * @see SessionCache#clearSession(Session) */
    private void purgeListeners() {
        if (ZimbraLog.mailbox.isDebugEnabled())
            ZimbraLog.mailbox.debug("purging listeners");
        Set purged = new HashSet(mListeners);
        for (Iterator it = purged.iterator(); it.hasNext(); )
            SessionCache.clearSession((Session) it.next());
        // this may be redundant, as Session.doCleanup should dequeue
        //   the listener, but empty the list here just to be sure
        mListeners.clear();
    }


    /** Returns whether the server is keeping track of message deletes
     *  (etc.) for sync clients.  By default, sync tracking is off.
     * 
     * @see #beginTrackingSync */
    boolean isTrackingSync() {
        return (mCurrentChange.sync == null ? mData.trackSync : mCurrentChange.sync.booleanValue());
    }

    /** Returns the operation timestamp as a UNIX int with 1-second
     *  resolution.  This time is set at the start of the Mailbox
     *  transaction and should match the <code>long</code> returned
     *  by {@link #getOperationTimestampMillis}. */
    public int getOperationTimestamp() {
        return (int) (mCurrentChange.timestamp / 1000);
    }

    /** Returns the operation timestamp as a Java long with full
     *  millisecond resolution.  This time is set at the start of
     *  the Mailbox transaction and should match the <code>int</code>
     *  returned by {@link #getOperationTimestamp}. */
    public long getOperationTimestampMillis() {
        return mCurrentChange.timestamp;
    }

    /** Returns the timestamp of the last committed mailbox change.
     *  Note that this time is not persisted across server restart. */
    public long getLastChangeDate() {
        return mData.lastChangeDate;
    }

    /** Returns the change sequence number for the most recent
     *  transaction.  This will be either the change number for the
     *  current transaction or, if no database changes have yet been
     *  made in this transaction, the sequence number for the last
     *  committed change.
     * 
     * @see #getOperationChangeID */
    public int getLastChangeID() {
        return (mCurrentChange.changeId == MailboxChange.NO_CHANGE ? mData.lastChangeId : mCurrentChange.changeId);
    }

    private void setOperationChangeID(int changeFromRedo) throws ServiceException {
        if (mCurrentChange.changeId != MailboxChange.NO_CHANGE) {
            if (mCurrentChange.changeId == changeFromRedo)
                return;
            throw ServiceException.FAILURE("cannot specify change ID after change is in progress", null);
        }

        int lastId = getLastChangeID();
        int nextId = (changeFromRedo == ID_AUTO_INCREMENT ? lastId + 1 : changeFromRedo);

        // need to keep the current change ID regardless of whether it's a highwater mark
        mCurrentChange.changeId = nextId;
        if (nextId / DbMailbox.CHANGE_CHECKPOINT_INCREMENT > lastId / DbMailbox.CHANGE_CHECKPOINT_INCREMENT)
            DbMailbox.updateHighestChange(this);
    }

    /** Returns the change sequence number for the current transaction.
     *  If a change number has not yet been assigned to the transaction,
     *  assigns one.<p>
     * 
     *  Every write to the database is assigned a monotonically-increasing
     *  (though not necessarily gap-free) change number.  All writes in
     *  a single transaction receive the same change number.  This change
     *  number is persisted as <code>MAIL_ITEM.MOD_METADATA</code> in all
     *  non-delete cases, as <code>MAIL_ITEM.MOD_CONTENT</code> for any 
     *  items that were created or had their "content" modified, and as
     *  <code>tombstone.sequence</code> for hard deletes. */
    public int getOperationChangeID() throws ServiceException {
        if (mCurrentChange.changeId == MailboxChange.NO_CHANGE)
            setOperationChangeID(ID_AUTO_INCREMENT);
        return mCurrentChange.changeId;
    }

    /** @return whether the object has changed more recently than the client knows about */
    boolean checkItemChangeID(MailItem item) throws ServiceException {
        if (item == null)
            return true;
        return checkItemChangeID(item.getModifiedSequence(), item.getSavedSequence());
    }
    public boolean checkItemChangeID(int modMetadata, int modContent) throws ServiceException {
        if (mCurrentChange.octxt == null || mCurrentChange.octxt.change < 0)
            return true;
        OperationContext octxt = mCurrentChange.octxt;
        if (octxt.changetype == OperationContext.CHECK_CREATED && modContent > octxt.change)
            return false;
        else if (octxt.changetype == OperationContext.CHECK_MODIFIED && modMetadata > octxt.change)
            throw MailServiceException.MODIFY_CONFLICT();
        return true;
    }

    /** Returns the last id assigned to an item successfully created in the
     *  mailbox.  On startup, this value will be rounded up to the nearest
     *  100, so there may be gaps in the set of IDs actually assigned.
     * 
     * @see MailItem#getId()
     * @see DbMailbox#ITEM_CHECKPOINT_INCREMENT */
    public int getLastItemId() {
        return (mCurrentChange.itemId == MailboxChange.NO_CHANGE ? mData.lastItemId : mCurrentChange.itemId);
    }

    // Don't make this method package-visible.  Keep it private.
    //   idFromRedo: specific ID value to use during redo execution, or ID_AUTO_INCREMENT
    private int getNextItemId(int idFromRedo) throws ServiceException {
        int lastId = getLastItemId();
        int nextId = (idFromRedo == ID_AUTO_INCREMENT ? lastId + 1 : idFromRedo);

        if (nextId > lastId) {
            mCurrentChange.itemId = nextId;
            if (nextId / DbMailbox.ITEM_CHECKPOINT_INCREMENT > lastId / DbMailbox.ITEM_CHECKPOINT_INCREMENT)
                DbMailbox.updateHighestItem(this);
        }
        return nextId;
    }

    TargetConstraint getOperationTargetConstraint() {
        return mCurrentChange.tcon;
    }

    void setOperationTargetConstraint(TargetConstraint tcon) {
        mCurrentChange.tcon = tcon;
    }

    public OperationContext getOperationContext() {
        return (mCurrentChange.active ? mCurrentChange.octxt : null);
    }


    /** Returns the {@link Account} for the authenticated user for the
     *  transaction.  Returns <code>null</code> if none was supplied in the
     *  transaction's {@link Mailbox.OperationContext} or if the authenticated
     *  user is the same as the <code>Mailbox</code>'s owner.*/
    Account getAuthenticatedAccount() {
        Account authuser = null;
        if (mCurrentChange.active && mCurrentChange.octxt != null)
            authuser = mCurrentChange.octxt.authuser;
        if (authuser != null && authuser.getId().equals(getAccountId()))
            authuser = null;
        return authuser;
    }

    /** Returns whether the authenticated user has full access to this
     *  <code>Mailbox</code>.   The following users have full access:<ul>
     *    <li>the mailbox's owner
     *    <li>all admin accounts</ul>
     * 
     * @see #getAuthenticatedAccount() */
    boolean hasFullAccess() {
        Account authuser = getAuthenticatedAccount();
        return authuser == null || authuser.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
    }


    /** Returns the total (uncompressed) size of the mailbox's contents. */
    public long getSize() {
        return (mCurrentChange.size == MailboxChange.NO_CHANGE ? mData.size : mCurrentChange.size);
    }

    /** change the current size of the mailbox */
    void updateSize(long delta) throws ServiceException {
        updateSize(delta, true);
    }
    void updateSize(long delta, boolean checkQuota) throws ServiceException {
        if (delta == 0)
            return;
        // if we go negative, that's OK!  just pretend we're at 0.
        mCurrentChange.mDirty.recordModified(this, Change.MODIFIED_SIZE);
        mCurrentChange.size = Math.max(0, (mCurrentChange.size == MailboxChange.NO_CHANGE ? mData.size : mCurrentChange.size) + delta);

        if (delta < 0 || !checkQuota)
            return;
        long quota = getAccount().getLongAttr(Provisioning.A_zimbraMailQuota, 0);
        if (quota != 0 && mCurrentChange.size > quota)
            throw MailServiceException.QUOTA_EXCEEDED(quota);
    }

    /** Updates the count of contacts currently in the mailbox.  The
     *  administrator can place a limit on a user's contact count by setting
     *  the <code>zimbraContactMaxNumEntries</code> COS attribute.  Contacts
     *  in the Trash still count against this quota.
     * 
     * @param delta  The change in contact count, negative to decrease.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.TOO_MANY_CONTACTS</code> - if the user's contact
     *        quota would be exceeded</ul> */
    void updateContactCount(int delta) throws ServiceException {
        if (delta == 0)
            return;
        // if we go negative, that's OK!  just pretend we're at 0.
        mCurrentChange.contacts = Math.max(0, (mCurrentChange.contacts == MailboxChange.NO_CHANGE ? mData.contacts : mCurrentChange.contacts) + delta);

        if (delta < 0)
            return;
        int quota = getAccount().getIntAttr(Provisioning.A_zimbraContactMaxNumEntries, 0);
        if (quota != 0 && mCurrentChange.contacts > quota)
            throw MailServiceException.TOO_MANY_CONTACTS(quota);
    }


    /** Adds the item to the current change's list of items created during
     *  the transaction.
     * @param item  The created item. */
    void markItemCreated(MailItem item) {
        mCurrentChange.mDirty.recordCreated(item);
    }

    /** Adds the item to the current change's list of items deleted during
     *  the transaction.
     * @param item  The deleted item. */
    void markItemDeleted(MailItem item) {
        mCurrentChange.mDirty.recordDeleted(item);
    }

    /** Adds the item ids to the current change's list of items deleted during
     *  the transaction.
     * @param itemIds  The list of deleted items' ids. */
    void markItemDeleted(List itemIds) {
        for (int i = 0; i < itemIds.size(); i++)
            mCurrentChange.mDirty.recordDeleted(((Integer) itemIds.get(i)).intValue());
    }

    /** Adds the item to the current change's list of items modified during
     *  the transaction.
     * @param item    The modified item.
     * @param reason  The bitmask describing the modified item properties.
     * @see com.zimbra.cs.session.PendingModifications.Change */
    void markItemModified(MailItem item, int reason) {
        mCurrentChange.mDirty.recordModified(item, reason);
    }

    /** Adds the object to the current change's list of non-{@link MailItem}
     *  objects affected during the transaction.  Among these "dirty" items
     *  can be:<ul>
     *    <li>The {@link Blob} or {@link MailboxBlob} for a newly-created file.
     *    <li>The {@link MailItem.PendingDelete} holding blobs and index
     *        entries to be cleaned up after a {@link MailItem#delete}.
     *    <li>The SHA1 hash of a conversation's subject stored in
     *        {@link #mConvHashes}.
     * 
     * @param obj  The relevant object.
     * @see #commitCache(Mailbox.MailboxChange)
     * @see #rollbackCache(Mailbox.MailboxChange) */
    void markOtherItemDirty(Object obj) {
        mCurrentChange.mOtherDirtyStuff.add(obj);
    }


    public synchronized Connection getOperationConnection() throws ServiceException {
        if (!mCurrentChange.isActive())
            throw ServiceException.FAILURE("cannot fetch Connection outside transaction", new Exception());
        return mCurrentChange.getConnection();
    }
    private synchronized void setOperationConnection(Connection conn) throws ServiceException {
        if (!mCurrentChange.isActive())
            throw ServiceException.FAILURE("cannot set Connection outside transaction", new Exception());
        else if (conn == null)
            return;
        else if (mCurrentChange.conn != null)
            throw ServiceException.FAILURE("cannot set Connection for in-progress transaction", new Exception());
        mCurrentChange.conn = conn;
    }

    /** Puts the Mailbox into maintenance mode.  As a side effect, disconnects
     *  any {@link Session}s listening on this Mailbox.
     * 
     * @return A new MailboxLock token for use in a subsequent call to
     *         {@link #endMaintenance(Mailbox.MailboxLock, boolean, boolean)}.
     * @throws ServiceException MailServiceException.MAINTENANCE if the
     *         <code>Mailbox</code> is already in maintenance mode. */
    private synchronized MailboxLock beginMaintenance() throws ServiceException {
        if (mMaintenance != null)
            throw MailServiceException.MAINTENANCE(mId);
        mMaintenance = new MailboxLock(mData.accountId, mId);
        mMaintenance.mailbox = this;
        purgeListeners();
        return mMaintenance;
    }

    private void beginTransaction(String caller, OperationContext octxt) throws ServiceException {
        beginTransaction(caller, System.currentTimeMillis(), octxt, null, null);
    }
    private void beginTransaction(String caller, OperationContext octxt, RedoableOp recorder) throws ServiceException {
        long timestamp = octxt == null ? System.currentTimeMillis() : octxt.getTimestamp();
        beginTransaction(caller, timestamp, octxt, recorder, null);
    }
    private void beginTransaction(String caller, OperationContext octxt, RedoableOp recorder, Connection conn) throws ServiceException {
        long timestamp = octxt == null ? System.currentTimeMillis() : octxt.getTimestamp();
        beginTransaction(caller, timestamp, octxt, recorder, conn);
    }
    private void beginTransaction(String caller, OperationContext octxt, ParsedMessage pm, RedoableOp recorder) throws ServiceException {
        beginTransaction(caller, pm.getReceivedDate(), octxt, recorder, null);
    }
    private void beginTransaction(String caller, long time, OperationContext octxt, RedoableOp recorder, Connection conn) throws ServiceException {
        mCurrentChange.startChange(caller, octxt, recorder);

        // if a Connection object was provided, use it
        if (conn != null)
            setOperationConnection(conn);

        // have a single, consistent timestamp for anything affected by this operation
        mCurrentChange.setTimestamp(time);
        if (recorder != null)
            recorder.start(time);

        // if the caller has specified a constraint on the range of affected items, store it
        if (recorder != null && octxt != null && octxt.change > 0)
            recorder.setChangeConstraint(octxt.changetype, octxt.change);

        // if we're redoing an op, preserve the old change ID
        if (octxt != null && octxt.getChangeId() >= 0)
            setOperationChangeID(octxt.getChangeId());
        if (recorder != null)
            recorder.setChangeId(getOperationChangeID());

        // keep a hard reference to the item cache to avoid having it GCed during the op 
        Object cache = mItemCache.get();
        if (cache == null) {
            cache = new LinkedHashMap(MAX_ITEM_CACHE_WITH_LISTENERS, (float) 0.75, true);
            mItemCache = new SoftReference(cache);
            ZimbraLog.cache.debug("created a new MailItem cache for mailbox " + getId());
        }
        mCurrentChange.itemCache = (Map) cache;

        // don't permit mailbox access during maintenance
        if (mMaintenance != null && mMaintenance.owner != Thread.currentThread())
            throw MailServiceException.MAINTENANCE(mId);

        // we can only start a redoable operation as the transaction's base change
        if (recorder != null && mCurrentChange.depth > 1)
            throw ServiceException.FAILURE("cannot start a logged transaction from within another transaction", null);

        // we'll need folders and tags loaded in order to handle ACLs
        loadFoldersAndTags();
    }


    /** Returns the set of configuration info for the given section.
     *  We segment the mailbox-level configuration data into "sections" to
     *  allow server applications to store their config separate from all
     *  other apps.  (So the IMAP server could store and retrieve the
     *  <code>"imap"</code> config section, etc.)
     * 
     * @param octxt    The context for this request (e.g. auth user id).
     * @param section  The config section to fetch.
     * @perms full access to the mailbox (see {@link #hasFullAccess()})
     * @return The {@link Metadata} representing the appropriate section's
     *         configuration information, or <code>null</code> if none is
     *         found or if the caller does not have sufficient priviliges
     *         to read the mailbox's config. */
    public synchronized Metadata getConfig(OperationContext octxt, String section) throws ServiceException {
        if (section == null || section.equals(""))
            return null;

        boolean success = false;
        try {
            beginTransaction("getConfig", octxt, null);

            // make sure they have sufficient rights to view the config
            if (!hasFullAccess())
                return null;

            Metadata meta = mCurrentChange.config == null ? mData.config : mCurrentChange.config;
            if (meta == null)
                return null;
            String config = meta.get(section, null);
            if (config == null)
                return null;
            try {
                return new Metadata(config);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("could not decode config metadata for section:" + section);
                return null;
            }
        } finally {
            endTransaction(success);
        }
    }

    /** Sets the configuration info for the given section.  We segment the
     *  mailbox-level configuration data into "sections" to allow server
     *  applications to store their config separate from all other apps.
     * 
     * @param octxt    The context for this request (e.g. auth user id).
     * @param section  The config section to store.
     * @param config   The new config data for the section.
     * @perms full access to the mailbox (see {@link #hasFullAccess()})
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #getConfig(OperationContext, String) */
    public synchronized void setConfig(OperationContext octxt, String section, Metadata config) throws ServiceException {
        if (section == null)
            throw new IllegalArgumentException();
        // FIXME: needs redolog support

        boolean success = false;
        try {
            beginTransaction("setConfig", octxt, null);

            // make sure they have sufficient rights to view the config
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            mCurrentChange.mDirty.recordModified(this, Change.MODIFIED_CONFIG);
            mCurrentChange.config = new Metadata().copy(mData.config);
            if (config != null)
                mCurrentChange.config.put(section, config.toString());
            else
                mCurrentChange.config.remove(section);

            DbMailbox.updateConfig(this, mCurrentChange.config);
            success = true;
        } finally {
            endTransaction(success);
        }
    }


    private Map getItemCache() throws ServiceException {
        if (!mCurrentChange.isActive())
            throw ServiceException.FAILURE("cannot access item cache outside a transaction", null);
        return mCurrentChange.itemCache;
    }

    void cache(MailItem item) throws ServiceException {
        if (item == null)
            return;
        if (item instanceof Tag) {
            if (item instanceof Flag)
                mFlags[((Flag) item).getIndex()] = (Flag) item;
            if (mTagCache != null) {
                mTagCache.put(new Integer(item.getId()), item);
                mTagCache.put(((Tag) item).getName().toLowerCase(), item);
            }
        } else if (item instanceof Folder) {
            if (mFolderCache != null)
                mFolderCache.put(new Integer(item.getId()), item);
        } else
            getItemCache().put(new Integer(item.getId()), item);

        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("cached " + MailItem.getNameForType(item) + " " + item.getId() + " in mailbox " + getId());
    }

    void uncache(MailItem item) throws ServiceException {
        if (item == null)
            return;
        if (item instanceof Tag) {
            if (mTagCache == null)
                return;
            mTagCache.remove(new Integer(item.getId()));
            mTagCache.remove(((Tag) item).getName().toLowerCase());
        } else if (item instanceof Folder) {
            if (mFolderCache == null)
                return;
            mFolderCache.remove(new Integer(item.getId()));
        } else
            getItemCache().remove(new Integer(item.getId()));
        
        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("uncached " + MailItem.getNameForType(item) + " " + item.getId() + " in mailbox " + getId());
        
        item.uncacheChildren();
    }

    /** Removes an item from the <code>Mailbox</code>'s item cache.  If the
     *  item has any children, they are also uncached.  <i>Note: This function
     *  cannot be used to uncache {@link Tag}s and {@link Folder}s.  You must
     *  call {@link #uncache(MailItem)} to remove those items from their
     *  respective caches.</i>
     * 
     * @param itemId  The id of the item to uncache */
    void uncacheItem(Integer itemId) throws ServiceException {
        Object obj = getItemCache().remove(itemId);
        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("uncached item " + itemId + " in mailbox " + getId());
        if (obj != null && obj instanceof MailItem)
            ((MailItem) obj).uncacheChildren();
    }
    
    /** Removes all items of a specified type from the <code>Mailbox</code>'s
     *  caches.  There may be some collateral damage: purging non-tag,
     *  non-folder types will drop the entire item cache.
     * 
     * @param type  The type of item to completely uncache. */
    void purge(byte type) {
        switch (type) {
            case MailItem.TYPE_FOLDER:
            case MailItem.TYPE_SEARCHFOLDER:  mFolderCache = null; break;
            case MailItem.TYPE_FLAG:
            case MailItem.TYPE_TAG:           mTagCache = null;    break;
            default:                          mItemCache.clear();  break;
        }

        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("purged " + MailItem.getNameForType(type) + " cache in mailbox " + getId());
    }
    

    private static void loadMailboxMap() throws ServiceException {
        if (sMailboxCache != null)
            return;
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            sMailboxCache = DbMailbox.getMailboxes(conn);
        } finally {
            DbPool.quietClose(conn);
        }
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
    public static Mailbox getMailboxByAccount(Account account) throws ServiceException {
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
    public static Mailbox getMailboxByAccountId(String accountId) throws ServiceException {
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
    public static Mailbox getMailboxByAccountId(String accountId, boolean autocreate) throws ServiceException {
        if (accountId == null)
            throw new IllegalArgumentException();

        Integer mailboxKey;
        synchronized (sMailboxCache) {
            mailboxKey = (Integer) sMailboxCache.get(accountId.toLowerCase());
        }
        if (mailboxKey != null)
            return getMailboxById(mailboxKey.intValue());
        else if (!autocreate)
            return null;

        // auto-create the mailbox if this is the right host...
        Account account = getAccount(accountId);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        if (!account.isCorrectHost())
            throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);
        synchronized (sMailboxCache) {
            mailboxKey = (Integer) sMailboxCache.get(accountId.toLowerCase());
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
     *        lives on a different host</ul> */
    public static Mailbox getMailboxById(int mailboxId) throws ServiceException {
        if (mailboxId <= 0)
            throw MailServiceException.NO_SUCH_MBOX(mailboxId);

        synchronized (sMailboxCache) {
            Integer mailboxKey = new Integer(mailboxId);
            Object obj = sMailboxCache.get(mailboxKey);
            if (obj instanceof Mailbox)
                return (Mailbox) obj;
            else if (obj instanceof MailboxLock) {
                MailboxLock lock = (MailboxLock) obj;
                if (lock.owner != Thread.currentThread())
                    throw MailServiceException.MAINTENANCE(mailboxId);
                if (lock.mailbox != null)
                    return lock.mailbox;
            }

            Mailbox mailbox = null;
            Connection conn = null;
            boolean success = false;
            try {
                conn = DbPool.getConnection();
                MailboxData mdata = DbMailbox.getMailboxStats(conn, mailboxId);
                if (mdata == null)
                    throw MailServiceException.NO_SUCH_MBOX(mailboxId);
                mailbox = new Mailbox(mdata);

                mailbox.beginTransaction("getMailboxById", null, null, conn);
                if (obj instanceof MailboxLock)
                    ((MailboxLock) obj).mailbox = mailbox;
                else
                    sMailboxCache.put(mailboxKey, mailbox);
                DbMailItem.writeCheckpoint(mailbox);
                success = true;
                return mailbox;
            } finally {
                try {
                    if (mailbox != null) {
                        mailbox.endTransaction(success);
                        conn = null;
                    } else if (conn != null)
                        conn.rollback();
                } finally {
                    if (conn != null)
                        DbPool.quietClose(conn);
                }
            }
        }
    }
 

    public static MailboxLock beginMaintenance(String accountId, int mailboxId) throws ServiceException {
        synchronized (sMailboxCache) {
            Integer mailboxKey = (Integer) sMailboxCache.get(accountId.toLowerCase());
            if (mailboxKey != null) {
                if (mailboxKey.intValue() != mailboxId)
                    throw MailServiceException.WRONG_MAILBOX();
            } else {
                mailboxKey = new Integer(mailboxId);
                if (sMailboxCache.values().contains(mailboxKey))
                    throw MailServiceException.WRONG_MAILBOX();
            }

            Object obj = sMailboxCache.get(mailboxKey);
            MailboxLock lock = null;
            if (obj instanceof Mailbox)
                lock = ((Mailbox) obj).beginMaintenance();
            else if (obj instanceof MailboxLock)
                throw MailServiceException.MAINTENANCE(mailboxKey.intValue());
            else
                lock = new MailboxLock(accountId, mailboxId);
            sMailboxCache.put(mailboxKey, lock);
            return lock;
        }
    }
    public static void endMaintenance(MailboxLock lock, boolean success, boolean clearCache) throws ServiceException {
        if (lock == null)
            throw ServiceException.INVALID_REQUEST("no lock provided", null);
        synchronized (sMailboxCache) {
            Integer mailboxKey = new Integer(lock.mailboxId);
            Object obj = sMailboxCache.get(mailboxKey);
            if (obj == null || obj != lock)
                throw MailServiceException.MAINTENANCE(mailboxKey.intValue());
            sMailboxCache.remove(mailboxKey);
            if (success) {
                sMailboxCache.put(lock.accountId, mailboxKey);
                if (lock.mailbox != null && !clearCache) {
                    assert(lock == lock.mailbox.mMaintenance);
                    lock.mailbox.mMaintenance = null;
                    sMailboxCache.put(mailboxKey, lock.mailbox);
                }
            } else {
                // on failed maintenance, mark the Mailbox object as off-limits to everyone
                if (lock.mailbox != null && lock.mailbox.mMaintenance != null)
                    lock.mailbox.mMaintenance.owner = null;
            }
        }
    }

    /** Returns an array of all the mailbox IDs on this host.  Note that
     *  <code>Mailbox</code>es are lazily created, so this is not the same
     *  as the set of mailboxes for accounts whose <code>zimbraMailHost</code>
     *  LDAP attribute points to this server. */
    public static int[] getMailboxIds() {
        int i = 0, mailboxIds[] = null;
        synchronized (sMailboxCache) {
            Collection col = sMailboxCache.values();
            mailboxIds = new int[col.size()];
            // mMailboxCache contains accountId -> mailboxId mappings as well as
            //   mailboxId -> Mailbox mappings.  we just want to iterate over the first of these...
            for (Iterator it = col.iterator(); it.hasNext(); ) {
                Object o = it.next();
                if (o instanceof Integer)
                    mailboxIds[i++] = ((Integer) o).intValue();
            }
        }
        int[] result = new int[i];
        System.arraycopy(mailboxIds, 0, result, 0, i);
        return result;
    }
    
    /** Returns an array of the account IDs of all the mailboxes on this host.
     *  Note that <code>Mailbox</code>es are lazily created, so this is not
     *  the same as the set of accounts whose <code>zimbraMailHost</code> LDAP
     *  attribute points to this server.*/
    public static String[] getAccountIds() {
        int i = 0;
        String accountIds[] = null;
        synchronized (sMailboxCache) {
            Set set = sMailboxCache.keySet();
            accountIds = new String[set.size()];
            // mMailboxCache contains accountId -> mailboxId mappings as well as
            //   mailboxId -> Mailbox mappings.  we just want to iterate over the first of these...
            for (Iterator it = set.iterator(); it.hasNext(); ) {
                Object o = it.next();
                if (o instanceof String)
                    accountIds[i++] = (String) o;
            }
        }
        String[] result = new String[i];
        System.arraycopy(accountIds, 0, result, 0, i);
        return result;
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
    public static Mailbox createMailbox(OperationContext octxt, Account account) throws ServiceException {
        if (account == null)
            throw new IllegalArgumentException("createMailbox: must specify an account");
        if (!account.isCorrectHost())
            throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);

        Mailbox mailbox = null;

        synchronized (sMailboxCache) {
            // check to make sure the mailbox doesn't already exist
            Integer mailboxKey = (Integer) sMailboxCache.get(account.getId().toLowerCase());
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
                int id = (redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMailboxId());
                data.id = DbMailbox.createMailbox(conn, id, data.accountId, account.getName());
                DbMailbox.createMailboxDatabase(data.id);

                // The above initialization of data is incomplete because it
                // is missing the message/index volume information.  Query
                // the database to get it.
                DbMailbox.getMailboxVolumeInfo(conn, data);

                mailbox = new Mailbox(data);
                // the existing Connection is used for the rest of this transaction...
                mailbox.beginTransaction("createMailbox", octxt, redoRecorder, conn);
                // create the default folders
                mailbox.initialize();

                // cache the accountID-to-mailboxID and mailboxID-to-Mailbox relationships
                sMailboxCache.put(data.accountId, new Integer(data.id));
                sMailboxCache.put(new Integer(data.id), mailbox);
                redoRecorder.setMailboxId(mailbox.getId());

                success = true;
            } finally {
                try {
                    if (mailbox != null) {
                        mailbox.endTransaction(success);
                        conn = null;
                    } else {
                        if (conn != null)
                            conn.rollback();
                        if (data != null)
                            DbMailbox.dropMailboxDatabase(data.id);
                    }
                } finally {
                    if (conn != null)
                        DbPool.quietClose(conn);
                }
            }
        }

        return mailbox;
    }

    /** Creates the default set of immutable system folders in a new mailbox.
     *  These system folders have fixed ids (e.g. {@link #ID_FOLDER_INBOX})
     *  and are hardcoded in the server:<pre>
     *     MAILBOX_ROOT
     *       +--Tags
     *       +--Conversations
     *       +--&lt;other hidden system folders>
     *       +--USER_ROOT
     *            +--INBOX
     *            +--Trash
     *            +--Sent
     *            +--&lt;other immutable folders>
     *            +--&lt;user-created folders></pre>
     *  This function does <u>not</u> have hooks for inserting arbitrary
     *  folders, tags, or messages into a new mailbox.
     * 
     * @see Folder#create(int, Mailbox, Folder, String, byte) */
    private void initialize() throws ServiceException {
        // the new mailbox's caches are created and the default set of tags are
        // loaded by the earlier call to loadFoldersAndTags in beginTransaction

        byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_NO_UNREAD_COUNT;
        Folder root = Folder.create(ID_FOLDER_ROOT, this, null, "ROOT", hidden, MailItem.TYPE_UNKNOWN);
        Folder.create(ID_FOLDER_TAGS,          this, root, "Tags",          hidden, MailItem.TYPE_TAG);
        Folder.create(ID_FOLDER_CONVERSATIONS, this, root, "Conversations", hidden, MailItem.TYPE_CONVERSATION);

        byte system = Folder.FOLDER_IS_IMMUTABLE;
        Folder userRoot = Folder.create(ID_FOLDER_USER_ROOT, this, root, "USER_ROOT", system, MailItem.TYPE_UNKNOWN);
        Folder.create(ID_FOLDER_INBOX,    this, userRoot, "Inbox",    system, MailItem.TYPE_MESSAGE);
        Folder.create(ID_FOLDER_TRASH,    this, userRoot, "Trash",    system, MailItem.TYPE_UNKNOWN);
        Folder.create(ID_FOLDER_SPAM,     this, userRoot, "Junk",     system, MailItem.TYPE_MESSAGE);
        Folder.create(ID_FOLDER_SENT,     this, userRoot, "Sent",     system, MailItem.TYPE_MESSAGE);
        Folder.create(ID_FOLDER_DRAFTS,   this, userRoot, "Drafts",   system, MailItem.TYPE_MESSAGE);
        Folder.create(ID_FOLDER_CONTACTS, this, userRoot, "Contacts", system, MailItem.TYPE_CONTACT);
        Folder.create(ID_FOLDER_CALENDAR, this, userRoot, "Calendar", system, MailItem.TYPE_APPOINTMENT);
        mCurrentChange.itemId = FIRST_USER_ID;

        // and write a checkpoint to the tombstones table to help establish a change/date relationship
        DbMailItem.writeCheckpoint(this);
    }

    private void initFlags() throws ServiceException {
        // flags will be added to mFlags array via call to cache() in MailItem constructor
        mSentFlag      = Flag.instantiate(this, "\\Sent",       Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_FROM_ME);
        mAttachFlag    = Flag.instantiate(this, "\\Attached",   Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_ATTACHED);
        mReplyFlag     = Flag.instantiate(this, "\\Answered",   Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_REPLIED);
        mForwardFlag   = Flag.instantiate(this, "\\Forwarded",  Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_FORWARDED);
        mCopiedFlag    = Flag.instantiate(this, "\\Copied",     Flag.FLAG_GENERIC,         Flag.ID_FLAG_COPIED);
        mFlaggedFlag   = Flag.instantiate(this, "\\Flagged",    Flag.FLAG_GENERIC,         Flag.ID_FLAG_FLAGGED);
        mDraftFlag     = Flag.instantiate(this, "\\Draft",      Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_DRAFT);
        mDeletedFlag   = Flag.instantiate(this, "\\Deleted",    Flag.FLAG_GENERIC,         Flag.ID_FLAG_DELETED);
        mNotifiedFlag  = Flag.instantiate(this, "\\Notified",   Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_NOTIFIED);
        mUnreadFlag    = Flag.instantiate(this, "\\Unread",     Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_UNREAD);
        mSubscribeFlag = Flag.instantiate(this, "\\Subscribed", Flag.FLAG_IS_FOLDER_ONLY,  Flag.ID_FLAG_SUBSCRIBED);
    }

    private void loadFoldersAndTags() throws ServiceException {
        if (mFolderCache != null && mTagCache != null)
            return;
        ZimbraLog.cache.debug("Initializing folder and tag caches for mailbox " + getId());

        try {
            Map folderData = (mFolderCache == null ? new HashMap() : null);
            Map tagData    = (mTagCache == null ? new HashMap() : null);
            MailboxData stats = DbMailItem.getFoldersAndTags(this, folderData, tagData);

            if (stats != null) {
                if (mData.size != stats.size) {
                    mCurrentChange.mDirty.recordModified(this, Change.MODIFIED_SIZE);
                    ZimbraLog.mailbox.info("setting mailbox size to " + stats.size + " (was " + mData.size + ") for mailbox " + mId);
                    mData.size = stats.size;
                }
                if (mData.contacts != stats.contacts) {
                    ZimbraLog.mailbox.info("setting contact count to " + stats.contacts + " (was " + mData.contacts + ") for mailbox " + mId);
                    mData.contacts = stats.contacts;
                }
            }

            if (folderData != null) {
                mFolderCache = new HashMap();
                // create the folder objects and, as a side-effect, populate the new cache
                for (Iterator it = folderData.values().iterator(); it.hasNext(); )
                    MailItem.constructItem(this, (MailItem.UnderlyingData) it.next());
                // establish the folder hierarchy
                for (Iterator it = mFolderCache.values().iterator(); it.hasNext(); ) {
                    Folder folder = (Folder) it.next();
                    Folder parent = (Folder) mFolderCache.get(new Integer(folder.getParentId()));
                    // FIXME: side effect of this is that parent is marked as dirty...
                    if (parent != null)
                        parent.addChild(folder);
                }
            }

            if (tagData != null) {
                mTagCache = new HashMap();
                // create the tag objects and, as a side-effect, populate the new cache
                for (Iterator it = tagData.values().iterator(); it.hasNext(); )
                    new Tag(this, (Tag.UnderlyingData) it.next());
                // flags don't change and thus can be reused in the new cache
                for (int i = 0; i < mFlags.length; i++) {
                    if (mFlags[i] == null)
                        continue;
                    ZimbraLog.mailbox.debug(i + ": " + mFlags[i]);
                    cache(mFlags[i]);
                }
            }
        } catch (ServiceException e) {
            mTagCache = mFolderCache = null;
            throw e;
        }
    }

    public synchronized void deleteMailbox() throws ServiceException {
        deleteMailbox(null);
    }
    public synchronized void deleteMailbox(OperationContext octxt) throws ServiceException {
        // first, throw the mailbox into maintenance mode
        //   (so anyone else with a cached reference to the Mailbox can't use it)
        MailboxLock lock = null;
        try {
            lock = Mailbox.beginMaintenance(mData.accountId, mId);
        } catch (MailServiceException e) {
            // Ignore wrong mailbox exception.  It may be thrown if we're
            // redoing a DeleteMailbox that was interrupted when server
            // crashed in the middle of the operation.  Database says the
            // mailbox has been deleted, but there may be other files that
            // still need to be cleaned up.
            if (!MailServiceException.WRONG_MAILBOX.equals(e.getCode()))
                throw e;
        }

        DeleteMailbox redoRecorder = new DeleteMailbox(mId);
        boolean success = false;
        try {
            beginTransaction("deleteMailbox", octxt, redoRecorder);
            redoRecorder.log();

            try {
                // remove all the relevant entries from the database
                Connection conn = getOperationConnection();
                DbMailbox.deleteMailbox(conn, this);
                DbMailbox.dropMailboxDatabase(getId());
    
                success = true;
            } finally {
                // commit the DB transaction before touching the store!  (also ends the operation)
                endTransaction(success);
            }

            if (success) {
                // remove all traces of the mailbox from the Mailbox cache
                //   (so anyone asking for the Mailbox gets NO_SUCH_MBOX or creates a fresh new empty one with a different id)
                synchronized (sMailboxCache) {
                    sMailboxCache.remove(mData.accountId);
                    sMailboxCache.remove(new Integer(mId));
                }
                
                // attempt to nuke the store and index
                // FIXME: we're assuming a lot about the store and index here; should use their functions
                try {
                    getMailboxIndex().deleteIndex();
                } catch (IOException iox) { }
                try {
                    StoreManager sm = StoreManager.getInstance();
                    List volumes = Volume.getAll();
                    for (Iterator iter = volumes.iterator(); iter.hasNext(); ) {
                    	Volume vol = (Volume) iter.next();
                        sm.deleteStore(this, vol.getId());
                    }
                } catch (IOException iox) { }
    
                // twiddle the mailbox lock [must be the last command of this function!]
                //   (so even *we* can't access this Mailbox going forward)
                lock.owner   = null;
                lock.mailbox = null;
            }
        } finally {
            if (success)
                redoRecorder.commit();
            else
                redoRecorder.abort();
        }
    }

    public short getIndexVolume() {
        return mData.indexVolumeId;
    }
    
    public void reIndex() throws ServiceException {
        reIndex(null);
    }
    
    public static class ReIndexStatus {
        public int mNumProcessed = 0;
        public int mNumToProcess = 0;
        public int mNumFailed = 0;
        public boolean mCancel = false;
        
        public String toString() {
            String toRet = "Completed "+mNumProcessed+" out of " +mNumToProcess
            + " ("+mNumFailed +" failures";
            
            if (mCancel) 
                return "--CANCELLING--  "+toRet;
            else 
                return toRet;
        }
        
        public Object clone() {
            ReIndexStatus toRet = new ReIndexStatus();
            toRet.mNumProcessed = mNumProcessed;
            toRet.mNumToProcess = mNumToProcess;
            toRet.mNumFailed = mNumFailed;
            return toRet;
        }
    }
    
    
    /**
     * Status of current reindexing operation for this mailbox, or NULL 
     * if a re-index is not in progress
     */
    private ReIndexStatus mReIndexStatus = null;
    
    public synchronized boolean isReIndexInProgress() {
        return mReIndexStatus != null;
    }
    
    public synchronized ReIndexStatus getReIndexStatus() {
        return mReIndexStatus;
    }

    public void reIndex(OperationContext octxt) throws ServiceException {
        ReindexMailbox redoRecorder = new ReindexMailbox(mId);

        Collection msgs = null;
        boolean redoInitted = false;
        boolean indexDeleted = false;
        
        try {
            //
            // First step, with the mailbox locked: 
            //     -- get a list of all messages in the mailbox
            //     -- delete the index
            //
            synchronized(this) {
                boolean success = false;
                try {
                    // Don't pass redoRecorder to beginTransaction.  We have already
                    // manually called log() on redoRecorder because this is a long-
                    // running transaction, and we don't want endTransaction to log it
                    // again, resulting in two entries for the same operation in redolog.
                    beginTransaction("reIndex", octxt, null);
                    redoRecorder.start(getOperationTimestamp());
                    redoRecorder.log();
                    redoInitted = true;

                    DbMailItem.SearchConstraints c = new DbMailItem.SearchConstraints();
                    c.mailboxId = this.getId();
                    c.sort = DbMailItem.SORT_BY_DATE;
                    
                    msgs = DbMailItem.search(getOperationConnection(), c);
                    
                    MailboxIndex idx = getMailboxIndex();
                    idx.deleteIndex();
                    indexDeleted = true;
                    
                    success = true;
                } catch (IOException e) {
                    throw ServiceException.FAILURE("Error deleting index before re-indexing", e);
                } finally {
                    endTransaction(success);
                }
                mReIndexStatus = new ReIndexStatus();
                mReIndexStatus.mNumToProcess = msgs.size();
            }

            long start = System.currentTimeMillis();
            
            //
            // Second step:
            //      For each message in the list from above
            //      lock mailbox, re-index msg, release lock
            //
            for (Iterator iter = msgs.iterator(); iter.hasNext();) {
                synchronized(this) {
                    // loop 10 times with the lock held
                    for (int i = 0; i < 10 && iter.hasNext(); i++) { 
                        if (ZimbraLog.mailbox.isDebugEnabled() && ((mReIndexStatus.mNumProcessed % 2000) == 0)) {
                            ZimbraLog.mailbox.debug("Re-Indexing: Mailbox "+getId()+" on msg "+mReIndexStatus.mNumProcessed+" out of "+msgs.size());
                        }
                        
                        if (mReIndexStatus.mCancel) {
                            ZimbraLog.mailbox.warn("CANCELLING re-index of Mailbox "+getId()+" before it is complete.  ("+mReIndexStatus.mNumProcessed+" processed out of "+msgs.size()+")");                            
                            throw ServiceException.INTERRUPTED("ReIndexing Canceled");
                        }
                        
                        mReIndexStatus.mNumProcessed++;
                        MailboxIndex idx = getMailboxIndex();
                        SearchResult sr = (SearchResult) iter.next();
                        
                        try {
                            idx.reIndexItem(sr.id, sr.type);
                        } catch(ServiceException e) {
                            mReIndexStatus.mNumFailed++;
                            ZimbraLog.mailbox.info("Re-Indexing: Mailbox " +getId()+ " had error on msg "+sr.id+".  Message will not be indexed.", e);
                        }
                    }
                }
            }
            
            //
            // Final step: print some statistics
            //
            long end = System.currentTimeMillis();
            long avg = 0;
            long mps = 0;
            if (mReIndexStatus.mNumProcessed> 0) {
                avg = (end - start) / mReIndexStatus.mNumProcessed;
                mps = avg > 0 ? 1000 / avg : 0;
            }
            ZimbraLog.mailbox.info("Re-Indexing: Mailbox " + getId() + " COMPLETED.  Re-indexed "+mReIndexStatus.mNumProcessed
                    +" msgs in " + (end-start) + "ms.  (avg "+avg+"ms/msg = "+mps+" msgs/sec)"
                    +" ("+mReIndexStatus.mNumFailed+" failed) ");
            
        } finally {
            mReIndexStatus = null;
            
            getMailboxIndex().flush();
            if (redoInitted) {
                if (indexDeleted) {
                    // there's no meaningful way to roll this transaction back one the index is deleted!  
                    // Sooo, always commit it I guess....right?
                    //
                    // The failure mode is if some or all the messages don't re-index.  Right now we expect
                    // some failures, catch them, and ignore....maybe we need to return some error code to the 
                    // caller if the number of failures is nonzero...or maybe even a list of failed messages?
                    //
                    // TODO think about this some more...
                    redoRecorder.commit();
                } else {
                    redoRecorder.abort();
                }
            }
        }
    }
    
    public synchronized void retrieveRemoteCalendar(String uri, int folderId) throws ServiceException
    {
        HttpClient client = new HttpClient();
        
        client.setConnectionTimeout(10000);
        client.setTimeout(20000);
        
        GetMethod get = new GetMethod(uri);
        
        try {
            client.executeMethod(get);
        } catch (HttpException e) {
            throw ServiceException.FAILURE("HttpException: "+e, e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException: "+e, e);
        }
        
        List /* Invite */ invites = Invite.createFromRawICalendar(getAccount(), "", get.getResponseBodyAsString(), false);
        for (Iterator iter = invites.iterator(); iter.hasNext();) {
            Invite inv = (Invite)iter.next();
            addInvite(null, folderId, inv, true, null);
        }
    }


    /** Returns whether this type of {@link MailItem} is definitely preloaded
     *  in one of the <code>Mailbox</code>'s caches.
     * 
     * @param type  The type of <code>MailItem</code>.
     * @return <code>true</code> if the item is a {@link Folder} or {@link Tag}
     *         or one of their subclasses.
     * @see #mTagCache
     * @see #mFolderCache */
    public static boolean isCachedType(byte type) {
        return type == MailItem.TYPE_FOLDER || type == MailItem.TYPE_SEARCHFOLDER ||
               type == MailItem.TYPE_TAG    || type == MailItem.TYPE_FLAG ||
               type == MailItem.TYPE_MOUNTPOINT;
    }

    private MailItem checkAccess(MailItem item) throws ServiceException {
        if (item == null || item.canAccess(ACL.RIGHT_READ))
            return item;
        throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
    }

    /** mechanism for getting an item */
    public synchronized MailItem getItemById(OperationContext octxt, int id, byte type) throws ServiceException {
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemById", octxt);
            MailItem item = checkAccess(getItemById(id, type));
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    MailItem getItemById(int id, byte type) throws ServiceException {
        // try the cache first
        MailItem item = getCachedItem(new Integer(id), type);
        if (item != null)
            return item;

        // the tag and folder caches contain ALL tags and folders, so cache miss == doesn't exist
        if (isCachedType(type))
            throw MailItem.noSuchItem(id, type);

        if (id <= -FIRST_USER_ID) {
            // special-case virtual conversations
            if (type != MailItem.TYPE_CONVERSATION && type != MailItem.TYPE_UNKNOWN)
                throw MailItem.noSuchItem(id, type);
            Message msg = getCachedMessage(new Integer(-id));
            if (msg == null)
                msg = getMessageById(-id);
            if (msg.getConversationId() != id)
                return msg.getParent();
            else
                item = new VirtualConversation(this, msg);
        } else {
            // cache miss, so fetch from the database
            item = MailItem.getById(this, id, type);
        }
        return item;
    }

    /** mechanism for getting an item */
    public synchronized MailItem[] getItemById(OperationContext octxt, int[] ids, byte type) throws ServiceException {
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemById[]", octxt);
            MailItem[] items = getItemById(ids, type);
            // make sure all those items are visible...
            for (int i = 0; i < items.length; i++)
                checkAccess(items[i]);
            success = true;
            return items;
        } finally {
            endTransaction(success);
        }
    }

    MailItem[] getItemById(int[] ids, byte type) throws ServiceException {
        if (ids == null)
            return null;

        MailItem items[] = new MailItem[ids.length];
        Set uncached = new HashSet();

        // try the cache first
        Integer miss = null;
        boolean relaxType = false;
        for (int i = 0; i < ids.length; i++) {
            // special-case -1 as a signal to return null...
            if (ids[i] == ID_AUTO_INCREMENT)
                items[i] = null;
            else {
                Integer key = new Integer(ids[i]);
                MailItem item = getCachedItem(key, type);
                // special-case virtual conversations
                if (item == null && ids[i] <= -FIRST_USER_ID) {
                    if (!MailItem.isAcceptableType(type, MailItem.TYPE_CONVERSATION))
                        throw MailItem.noSuchItem(ids[i], type);
                    Message msg = getCachedMessage(key = new Integer(-ids[i]));
                    if (msg != null) {
                        if (msg.getConversationId() == ids[i])
                            item = new VirtualConversation(this, msg);
                    } else
                        relaxType = true;
                }
                items[i] = item;
                if (item == null)
                    uncached.add(miss = key);
            }
        }
        if (uncached.isEmpty())
            return items;

        // the tag and folder caches contain ALL tags and folders, so cache miss == doesn't exist
        if (isCachedType(type))
            throw MailItem.noSuchItem(miss.intValue(), type);

        // cache miss, so fetch from the database
        MailItem.getById(this, uncached, relaxType ? MailItem.TYPE_UNKNOWN : type);

        uncached.clear();
        for (int i = 0; i < ids.length; i++)
            if (ids[i] != ID_AUTO_INCREMENT && items[i] == null) {
                if (ids[i] <= -FIRST_USER_ID) {
                    // special-case virtual conversations
                    MailItem item = getCachedItem(new Integer(-ids[i]));
                    if (!(item instanceof Message))
                        throw MailItem.noSuchItem(ids[i], type);
                    else if (item.getParentId() == ids[i])
                        items[i] = new VirtualConversation(this, (Message) item);
                    else {
                        items[i] = getCachedItem(new Integer(item.getParentId()));
                        if (items[i] == null)
                            uncached.add(new Integer(item.getParentId()));
                    }
                } else
                    if ((items[i] = getCachedItem(new Integer(ids[i]))) == null)
                        throw MailItem.noSuchItem(ids[i], type);
                items[i] = items[i];
            }

        // special case asking for VirtualConversation but having it be a real Conversation
        if (!uncached.isEmpty()) {
            MailItem.getById(this, uncached, MailItem.TYPE_CONVERSATION);
            for (int i = 0; i < ids.length; i++)
                if (ids[i] <= -FIRST_USER_ID && items[i] == null) {
                    MailItem item = getCachedItem(new Integer(-ids[i]));
                    if (!(item instanceof Message) || item.getParentId() == ids[i])
                        throw ServiceException.FAILURE("item should be cached but is not: " + -ids[i], null);
                    items[i] = getCachedItem(new Integer(item.getParentId()));
                    if (items[i] == null)
                        throw MailItem.noSuchItem(ids[i], type);
                }
        }

        return items;
    }

    /** retrieve an item from the Mailbox's caches; return null if no item found */
    MailItem getCachedItem(Integer key) throws ServiceException {
        MailItem item = null;
        if (mTagCache != null)
            item = (MailItem) mTagCache.get(key);
        if (item == null && mFolderCache != null)
            item = (MailItem) mFolderCache.get(key);
        if (item == null)
            item = (MailItem) getItemCache().get(key);
        
        logCacheActivity(key, item);
        return item;
    }
    MailItem getCachedItem(Integer key, byte type) throws ServiceException {
        MailItem item = null;
        switch (type) {
            case MailItem.TYPE_UNKNOWN:
                return getCachedItem(key);
            case MailItem.TYPE_FLAG:
            case MailItem.TYPE_TAG:
                if (mTagCache != null)
                    item = (MailItem) mTagCache.get(key);
                break;
            case MailItem.TYPE_SEARCHFOLDER:
            case MailItem.TYPE_FOLDER:
                if (mFolderCache != null)
                    item = (MailItem) mFolderCache.get(key);
                break;
            default:
                item = (MailItem) getItemCache().get(key);
            break;
        }

        MailItem retVal = (item == null || MailItem.isAcceptableType(type, item.mData.type) ? item : null);
        logCacheActivity(key, retVal);
        return retVal;
    }

    /** translate from the DB representation of an item to its Mailbox abstraction */
    MailItem getItem(MailItem.UnderlyingData data) throws ServiceException {
        if (data == null)
            return null;
        MailItem item = getCachedItem(new Integer(data.id), data.type);
        // XXX: should we sanity-check the cached version to make sure all the data matches?
        if (item != null)
            return item;
        return MailItem.constructItem(this, data);
    }

    /** Returns all the MailItems of a given type, optionally in a specified folder */
    public synchronized List /*<MailItem>*/ getItemList(OperationContext octxt, byte type) throws ServiceException {
        return getItemList(octxt, type, -1);
    }
    public synchronized List /*<MailItem>*/ getItemList(OperationContext octxt, byte type, int folderId) throws ServiceException {
        if (type == MailItem.TYPE_UNKNOWN)
            return null;
        Folder folder = null;
        List result = new ArrayList();

        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemList", null);
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            if (folderId != -1)
                folder = getFolderById(folderId);

            if (type == MailItem.TYPE_TAG) {
                if (folderId != -1 && folderId != ID_FOLDER_TAGS)
                    return null;
                for (Iterator it = mTagCache.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    if (entry.getKey() instanceof String)
                        result.add(entry.getValue());
                }
                success = true;
                return result;
            } else if (type == MailItem.TYPE_FOLDER || type == MailItem.TYPE_SEARCHFOLDER) {
                for (Iterator it = mFolderCache.values().iterator(); it.hasNext(); ) {
                    Folder subfolder = (Folder) it.next();
                    if (folder == null || subfolder.getParentId() == folderId)
                        if ((type == MailItem.TYPE_SEARCHFOLDER) == (subfolder instanceof SearchFolder))
                            result.add(subfolder);
                }
                success = true;
                return result;
            }

            List dataList = null;
            if (folder != null)
                dataList = DbMailItem.getByFolder(folder, type);
            else
                dataList = DbMailItem.getByType(this, type);
            if (dataList == null)
                return null;
            for (Iterator it = dataList.iterator(); it.hasNext(); ) {
                MailItem.UnderlyingData data = (MailItem.UnderlyingData) it.next();
                if (data != null)
                    result.add(getItem(data));
            }
            success = true;
        } finally {
            endTransaction(success);
        }
        return result;
    }

    /** returns the list of IDs of items of the given type in the given folder 
     * @param octxt TODO*/
    public synchronized int[] listItemIds(OperationContext octxt, byte type, int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("listItemIds", null);
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            Folder folder = getFolderById(folderId);
            List idList = DbMailItem.listByFolder(folder, type, true);
            if (idList == null)
                return null;
            int i = 0, result[] = new int[idList.size()];
            for (Iterator it = idList.iterator(); it.hasNext(); )
                result[i++] = ((DbMailItem.SearchResult) it.next()).id;
            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }


    // FIXME: need to redolog this operation
    public void beginTrackingSync() throws ServiceException {
        if (isTrackingSync())
            return;

        boolean success = false;
        try {
            beginTransaction("beginTrackingSync", null);
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            mCurrentChange.sync = Boolean.TRUE;
            DbMailbox.startTrackingSync(this);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized String getTombstones(long lastSync) throws ServiceException {
        if (!isTrackingSync())
            throw ServiceException.FAILURE("not tracking sync", null);

        boolean success = false;
        try {
            beginTransaction("getTombstones", null);
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            String tombstones = DbMailItem.readTombstones(this, lastSync);
            success = true;
            return tombstones;
        } finally {
            endTransaction(success);
        }
    }

    /** @return a List of the MailItems of the given type modified since a given time */
    public synchronized List /*<MailItem>*/ getModifiedItems(byte type, long lastSync) throws ServiceException {
        if (type == MailItem.TYPE_UNKNOWN)
            return null;
        else if (lastSync >= getLastChangeID())
            return Collections.EMPTY_LIST;

        List result = new ArrayList();
        boolean success = false;
        try {
            beginTransaction("getModifiedItems", null);
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            if (type == MailItem.TYPE_TAG) {
                for (Iterator it = mTagCache.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    if (entry.getKey() instanceof String) {
                        Tag tag = (Tag) entry.getValue();
                        if (tag.getModifiedSequence() > lastSync && !(tag instanceof Flag))
                            result.add(entry.getValue());
                    }
                }
            } else if (type == MailItem.TYPE_FOLDER || type == MailItem.TYPE_SEARCHFOLDER) {
                for (Iterator it = mFolderCache.values().iterator(); it.hasNext(); ) {
                    Folder subfolder = (Folder) it.next();
                    if (type != MailItem.TYPE_SEARCHFOLDER || subfolder instanceof SearchFolder)
                        if (subfolder.getModifiedSequence() > lastSync)
                            result.add(subfolder);
                }
                Collections.sort(result);
            } else {
                List dataList = DbMailItem.getModifiedItems(this, type, lastSync);
                if (dataList == null)
                    return null;
                for (Iterator it = dataList.iterator(); it.hasNext(); ) {
                    MailItem.UnderlyingData data = (MailItem.UnderlyingData) it.next();
                    if (data != null)
                        result.add(getItem(data));
                }
            }
            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized Flag getFlagById(int id) throws ServiceException {
        // assume that flags are numbered from -1 to -mFlags.length
        Flag flag = null;
        if (id < 0 && id >= -mFlags.length)
            flag = mFlags[-id - 1];
        if (flag == null)
            throw MailServiceException.NO_SUCH_TAG(id);
        checkAccess(flag);
        return flag;
    }

    public synchronized Tag getTagById(OperationContext octxt, int id) throws ServiceException {
        return (Tag) getItemById(octxt, id, MailItem.TYPE_TAG);
    }
    Tag getTagById(int id) throws ServiceException {
        return (Tag) getItemById(id, MailItem.TYPE_TAG);
    }
    public synchronized List getTagList(OperationContext octxt) throws ServiceException {
        return getItemList(octxt, MailItem.TYPE_TAG);
    }
    public synchronized Tag getTagByName(String name) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getTagByName", null);

            if (name == null || name.equals(""))
                throw ServiceException.INVALID_REQUEST("tag name may not be null", null);
            Tag tag = (Tag) mTagCache.get(name.toLowerCase());
            if (tag == null)
                throw MailServiceException.NO_SUCH_TAG(name);
            checkAccess(tag);
            success = true;
            return tag;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized Folder getFolderById(OperationContext octxt, int id) throws ServiceException {
        return (Folder) getItemById(octxt, id, MailItem.TYPE_FOLDER);
    }
    Folder getFolderById(int id) throws ServiceException {
        return (Folder) getItemById(id, MailItem.TYPE_FOLDER);
    }
    public synchronized Folder getFolderByName(OperationContext octxt, int parentId, String name) throws ServiceException {
        boolean success = true;
        try {
            beginTransaction("getFolderByName", octxt);
            Folder folder = getFolderById(parentId).findSubfolder(name);
            if (folder == null)
                throw MailServiceException.NO_SUCH_FOLDER(name);
            if (!folder.canAccess(ACL.RIGHT_READ))
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on folder " + name);
            success = true;
            return folder;
        } finally {
            endTransaction(success);
        }
    }
    public synchronized Folder getFolderByPath(OperationContext octxt, String name) throws ServiceException {
        boolean success = true;
        try {
            beginTransaction("getFolderByPath", octxt);
            if (name == null)
                throw MailServiceException.NO_SUCH_FOLDER(name);
            if (name.startsWith("/"))
                name = name.substring(1);                         // strip off the optional leading "/"
            Folder folder = getFolderById(ID_FOLDER_USER_ROOT);
            if (!name.equals("")) {
                StringTokenizer tok = new StringTokenizer(name, "/");
                while (folder != null && tok.hasMoreTokens())
                    folder = folder.findSubfolder(tok.nextToken());
            }
            if (folder == null)
                throw MailServiceException.NO_SUCH_FOLDER("/" + name);
            if (!folder.canAccess(ACL.RIGHT_READ))
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on folder /" + name);
            success = true;
            return folder;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized SearchFolder getSearchFolderById(OperationContext octxt, int searchId) throws ServiceException {
        return (SearchFolder) getItemById(octxt, searchId, MailItem.TYPE_SEARCHFOLDER);
    }
    SearchFolder getSearchFolderById(int searchId) throws ServiceException {
        return (SearchFolder) getItemById(searchId, MailItem.TYPE_SEARCHFOLDER);
    }

    
    public synchronized Note getNoteById(OperationContext octxt, int noteId) throws ServiceException {
        return (Note) getItemById(octxt, noteId, MailItem.TYPE_NOTE);
    }
    Note getNoteById(int noteId) throws ServiceException {
        return (Note) getItemById(noteId, MailItem.TYPE_NOTE);
    }
    public synchronized List getNoteList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.TYPE_NOTE, folderId);
    }


    public synchronized Contact getContactById(OperationContext octxt, int id) throws ServiceException {
        return (Contact) getItemById(octxt, id, MailItem.TYPE_CONTACT);
    }
    Contact getContactById(int id) throws ServiceException {
        return (Contact) getItemById(id, MailItem.TYPE_CONTACT);
    }
    public synchronized List getContactList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.TYPE_CONTACT, folderId);
    }


    public synchronized Message getMessageById(OperationContext octxt, int id) throws ServiceException {
        return (Message) getItemById(octxt, id, MailItem.TYPE_MESSAGE);
    }
    Message getMessageById(int id) throws ServiceException {
        return (Message) getItemById(id, MailItem.TYPE_MESSAGE);
    }
    Message getMessage(MailItem.UnderlyingData data) throws ServiceException { 
        return (Message) getItem(data);
    }
    Message getCachedMessage(Integer id) throws ServiceException {
        return (Message) getCachedItem(id, MailItem.TYPE_MESSAGE);
    }

    public synchronized Message[] getMessagesByConversation(OperationContext octxt, int convId) throws ServiceException {
        return getMessagesByConversation(octxt, convId, Conversation.SORT_ID_ASCENDING);
    }
    public synchronized Message[] getMessagesByConversation(OperationContext octxt, int convId, byte sort) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getMessagesByConversation", octxt);
            Message[] msgs = getConversationById(convId).getMessages(sort);
            success = true;
            return msgs;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized Conversation getConversationById(OperationContext octxt, int id) throws ServiceException {
        return (Conversation) getItemById(octxt, id, MailItem.TYPE_CONVERSATION);
    }
    Conversation getConversationById(int id) throws ServiceException {
        return (Conversation) getItemById(id, MailItem.TYPE_CONVERSATION);
    }
    Conversation getConversation(MailItem.UnderlyingData data) throws ServiceException {
        return (Conversation) getItem(data);
    }
    Conversation getCachedConversation(Integer id) throws ServiceException {
        return (Conversation) getCachedItem(id, MailItem.TYPE_CONVERSATION);
    }

    Conversation getConversationByHash(String hash) throws ServiceException {
        Conversation conv = null;

        Integer convId = (Integer) mConvHashes.get(hash);
        if (convId != null)
            conv = getCachedConversation(convId);
        if (conv != null)
            return conv;

        // XXX: why not just do a "getConversationById()" if convId != null?
        MailItem.UnderlyingData data = DbMailItem.getByHash(this, hash);
        if (data == null || data.type == MailItem.TYPE_CONVERSATION)
            return getConversation(data);
        return (Conversation) getMessage(data).getParent();
    }

    public synchronized SenderList getConversationSenderList(int convId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getSenderList", null);

            Conversation conv = getConversationById(convId);
            SenderList sl = conv.getSenderList();

            success = true;
            return sl;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized Appointment getAppointmentById(OperationContext octxt, int id) throws ServiceException {
        return (Appointment) getItemById(octxt, id, MailItem.TYPE_APPOINTMENT);
    }
    Appointment getAppointmentById(int id) throws ServiceException {
        return (Appointment) getItemById(id, MailItem.TYPE_APPOINTMENT);
    }
    Appointment getAppointment(MailItem.UnderlyingData data) throws ServiceException { 
        return (Appointment) getItem(data);
    }
    public synchronized List getAppointmentList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.TYPE_APPOINTMENT, folderId);
    }

    public synchronized Calendar getCalendarForRange(OperationContext octxt, long start, long end)
    throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getCalendarForRange", octxt);

            Collection /* Appointment */ appts = getAppointmentsForRange(octxt, start, end, ID_AUTO_INCREMENT);
            Calendar cal = new Calendar();

            // PRODID, VERSION always required
            cal.getProperties().add(new ProdId("Zimbra-Calendar-Provider"));
            cal.getProperties().add(Version.VERSION_2_0);
            
            // REPLY
            cal.getProperties().add(Method.PUBLISH);
            
            // default timezone
            ICalTimeZone cur = getAccount().getTimeZone();
            VTimeZone vtz = cur.toVTimeZone();
            cal.getComponents().add(vtz);
            
            
            for (Iterator iter = appts.iterator(); iter.hasNext();) {
                Appointment appt = (Appointment)iter.next();
                
                appt.appendRawCalendarData(cal);
            }
            
            return cal;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns a <code>Collection</code> of all {@link Appointment}s which
     *  overlap the specified time period.  There is no guarantee that the
     *  returned appointments actually contain a recurrence within the range;
     *  all that is required is that there is some intersection between the
     *  (<code>start</code>, <code>end</code>) range and the period from the
     *  start time of the appointment's first recurrence to the end time of
     *  its last recurrence.<p>
     * 
     *  If a <code>folderId</code> is specified, only <code>Appointment</code>s
     *  in that folder are returned.  If {@link #ID_AUTO_INCREMENT} is passed
     *  in as the <code>folderId</code>, all <code>Appointment</code>s not in
     *  <code>Spam</code> or <code>Trash</code> are returned.
     * 
     * @param octxt     The {@link Mailbox.OperationContext}.
     * @param start     The start time of the range, in milliseconds.
     * @param end       The end time of the range, in milliseconds.
     * @param folderId  The folder to search for matching appointments, or
     *                  {@link #ID_AUTO_INCREMENT} to search all non-Spam and
     *                  Trash folders in the mailbox.
     * @perms {@link ACL#RIGHT_READ} on all returned appointments.
     * @throws ServiceException */
    public synchronized Collection /*<Appointment>*/ getAppointmentsForRange(OperationContext octxt, long start, long end, int folderId)
    throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getAppointmentsForRange", octxt);

            // if they specified a folder, make sure it actually exists
            if (folderId != ID_AUTO_INCREMENT)
                getFolderById(folderId);

            // get the list of all visible appointments in the specified folder
            List appointments = new ArrayList();
            List /* UnderlyingData */ invData = DbMailItem.getAppointments(this, start, end, folderId);
            for (Iterator iter = invData.iterator(); iter.hasNext(); ) {
                Appointment appt = getAppointment((MailItem.UnderlyingData) iter.next());
                if (folderId == appt.getFolderId() || (folderId == ID_AUTO_INCREMENT && appt.inMailbox()))
                    if (appt.canAccess(ACL.RIGHT_READ))
                        appointments.add(appt);
            }
            success = true;
            return appointments;
        } finally {
            endTransaction(success);
        }
    }
    
    /**
     * @param octxt
     * @param queryString
     * @param types
     * @param sortBy
     * @param chunkSize A hint to the search engine telling it the size of the result set you are expecting
     * @return
     * @throws IOException
     * @throws ParseException
     * @throws ServiceException
     */
    public ZimbraQueryResults search(OperationContext octxt, String queryString, byte[] types, int sortBy, int chunkSize) 
    throws IOException, ParseException, ServiceException {
        Account acct = getAccount();
        boolean includeTrash = 
            acct.getBooleanAttr(Provisioning.A_zimbraPrefIncludeTrashInSearch, false);
        boolean includeSpam = 
            acct.getBooleanAttr(Provisioning.A_zimbraPrefIncludeSpamInSearch, false);
        
        return search(octxt, queryString, types, sortBy, includeTrash, includeSpam, chunkSize);
    }

    /**
     * @param octxt
     * @param queryString
     * @param types
     * @param sortBy
     * @param includeTrash
     * @param includeSpam
     * @param chunkSize A hint to the search engine telling it the size of the result set you are expecting
     * @return
     * @throws IOException
     * @throws ParseException
     * @throws ServiceException
     */
    public synchronized ZimbraQueryResults search(OperationContext octxt, String queryString, byte[] types,
                                                  int sortBy, boolean includeTrash, boolean includeSpam, int chunkSize)
    throws IOException, ParseException, ServiceException {
        boolean success = false;
        try {
            beginTransaction("search", octxt);

            ZimbraQuery zq = new ZimbraQuery(queryString, this); 
            ZimbraQueryResults results = getMailboxIndex().search(zq, types, sortBy, includeTrash, includeSpam, chunkSize);
            success = true;
            return results;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized FreeBusy getFreeBusy(OperationContext octxt, long start, long end) throws ServiceException {
        return FreeBusy.getFreeBusyList(octxt, this, start, end);
    }

    private void addDomains(HashMap domainItems, HashSet newDomains, int flag) {
        for (Iterator it = newDomains.iterator(); it.hasNext(); ) {
            String domain = (String) it.next();
            DomainItem di = (DomainItem) domainItems.get(domain);
            if (di == null) {
                di = new DomainItem(domain);
                domainItems.put(domain, di);
            }
            di.setFlag(flag);
        }
    }

    public synchronized BrowseResult browse(OperationContext octxt, String browseBy) throws IOException, ServiceException {
        boolean success = true;
        try {
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on this mailbox");
            if (browseBy != null)
                browseBy = browseBy.intern();
    
            BrowseResult browseResult = new BrowseResult();
    
            MailboxIndex idx = getMailboxIndex();
            
            if (browseBy == BROWSE_BY_ATTACHMENTS) {
                idx.getAttachments(browseResult.getResult());
            } else if (browseBy == BROWSE_BY_DOMAINS) {
                HashMap domainItems = new HashMap();
                HashSet set = new HashSet();
                
                idx.getDomainsForField(LuceneFields.L_H_CC, set);
                addDomains(domainItems, set, DomainItem.F_CC);
                
                set.clear();
                idx.getDomainsForField(LuceneFields.L_H_FROM, set);
                addDomains(domainItems, set, DomainItem.F_FROM);
                
                set.clear();             
                idx.getDomainsForField(LuceneFields.L_H_TO, set);
                addDomains(domainItems, set, DomainItem.F_TO);
                
                browseResult.getResult().addAll(domainItems.values());
                
            } else if (browseBy == BROWSE_BY_OBJECTS) {
                idx.getObjects(browseResult.getResult());
            } else { 
                // throw exception?
            }
            success = true;
            return browseResult;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized MailboxIndex getMailboxIndex() throws ServiceException {
        if (mMailboxIndex == null)
            mMailboxIndex = new MailboxIndex(this, null);
        return mMailboxIndex;
    }
    
    public static class SetAppointmentData {
        public Invite mInv;
        public boolean mForce;
        public ParsedMessage mPm;
        
        public String toString() {
            StringBuffer toRet = new StringBuffer();
            toRet.append("inv:").append(mInv.toString()).append("\n");
            toRet.append("force:").append(mForce ? "true\n" : "false\n");
            toRet.append("pm:").append(mPm.getFragment()).append("\n");
            return toRet.toString();
        }
    }
    
    /**
     * @param octxt
     * @param defaultInvite
     * @param exceptions can be NULL
     * @return appointment ID 
     * @throws ServiceException
     */
    public synchronized int setAppointment(OperationContext octxt, int folderId, SetAppointmentData defaultInv, SetAppointmentData exceptions[])
    throws ServiceException {
        SetAppointment redoRecorder = new SetAppointment(getId());
        SetAppointment redoPlayer = (octxt == null ? null : (SetAppointment) octxt.player);
        
        boolean success = false;
        try {
            beginTransaction("setAppointment", octxt, redoRecorder);
            
            // allocate IDs for all of the passed-in invites (and the appointment!) if necessary
            if (redoPlayer == null || redoPlayer.getAppointmentId() == 0) {
                assert(defaultInv.mInv.getMailItemId() == 0);

                int mailItemId = getNextItemId(Mailbox.ID_AUTO_INCREMENT);
                defaultInv.mInv.setInviteId(mailItemId);

                if (exceptions != null) {
                    for (int i = 0; i < exceptions.length; i++) {
                        mailItemId = getNextItemId(Mailbox.ID_AUTO_INCREMENT);
                        exceptions[i].mInv.setMailItemId(mailItemId);
                    }
                }
            } else {
                // id already set before we stored the invite in the redoRecorder!!!
            }
            
            redoRecorder.setData(defaultInv, exceptions);

            // handle the DEFAULT appointment
            Appointment appt = getAppointmentByUid(defaultInv.mInv.getUid());
            if (appt == null) { 
                // ONLY create an appointment if this is a REQUEST method...otherwise don't.
                if (defaultInv.mInv.getMethod().equals("REQUEST") || defaultInv.mInv.getMethod().equals("PUBLISH")) {
                    appt = createAppointment(folderId, Volume.getCurrentMessageVolume().getId(), "",
                            defaultInv.mInv.getUid(), defaultInv.mPm, defaultInv.mInv);
                } else {
//                  mLog.info("Mailbox " + getId()+" Message "+getId()+" SKIPPING Invite "+method+" b/c not a REQUEST and no Appointment could be found");
                    return 0; // for now, just ignore this Invitation
                }
            } else {
                appt.removeAllInvites(); 
                appt.processNewInvite(defaultInv.mPm, defaultInv.mInv, defaultInv.mForce, Volume.getCurrentMessageVolume().getId());
            }
             
            // handle the exceptions!
            if (exceptions != null) {
                for (int i = 0; i < exceptions.length; i++) {
                    appt.processNewInvite(exceptions[i].mPm, exceptions[i].mInv, exceptions[i].mForce, Volume.getCurrentMessageVolume().getId());
                }
            }
            
            success = true;
            
            return appt.getId();
            
        } finally {
            endTransaction(success);
        }
    }
    
    /**
     * Directly add an Invite into the system...this process also gets triggered when we add a Message
     * that has a text/calendar Mime part: but this API is useful when you don't want to add a corresponding
     * message.
     * 
     * @param octxt
     * @param inv
     * @param pm
     * @param force if true, then force override the existing appointment, false use normal RFC2446 sequencing rules
     * @return int[2] = { appointment-id, invite-mail-item-id }  Note that even though the invite has a mail-item-id, that mail-item does not really exist, it can ONLY be referenced through the appointment "apptId-invMailItemId"
     * @throws ServiceException
     */
    public synchronized int[] addInvite(OperationContext octxt, int folder, Invite inv, boolean force, ParsedMessage pm)
    throws ServiceException {
        CreateInvite redoRecorder = new CreateInvite();
        CreateInvite redoPlayer = (octxt == null ? null : (CreateInvite) octxt.player);
        
        boolean success = false;
        try {
            beginTransaction("addInvite", octxt, redoRecorder);
            
            if (redoPlayer == null || redoPlayer.getAppointmentId() == 0) {
                assert(inv.getMailItemId() == 0); 
                int mailItemId = getNextItemId(Mailbox.ID_AUTO_INCREMENT);
                inv.setInviteId(mailItemId);
            } else {
                // id already set before we stored the invite in the redoRecorder!!!
            }
            
            redoRecorder.setInvite(inv);
            redoRecorder.setForce(force);
            redoRecorder.setFolderId(folder);
            
            Appointment appt = getAppointmentByUid(inv.getUid());
            if (appt == null) { 
                // ONLY create an appointment if this is a REQUEST method...otherwise don't.
                if (inv.getMethod().equals("REQUEST") || inv.getMethod().equals("PUBLISH")) {
                    appt = createAppointment(folder, Volume.getCurrentMessageVolume().getId(), "", inv.getUid(), pm, inv);
                } else {
//                  mLog.info("Mailbox " + getId()+" Message "+getId()+" SKIPPING Invite "+method+" b/c not a REQUEST and no Appointment could be found");
                  return null; // for now, just ignore this Invitation
                }
            } else {
                appt.processNewInvite(pm, inv, force, Volume.getCurrentMessageVolume().getId());
            }

            success = true;
            return new int[] { appt.getId(), inv.getMailItemId() };
        } finally {
            endTransaction(success);
        }
        
    }
    
    synchronized Appointment getAppointmentByUid(String uid) throws ServiceException {
        return getAppointmentByUid(null, uid);
    }
    public synchronized Appointment getAppointmentByUid(OperationContext octxt, String uid) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getAppointmentByUid", octxt);
            MailItem.UnderlyingData data = DbMailItem.getAppointment(this, uid);
            Appointment appt = (Appointment) getItem(data);
            success = true;
            return appt;
        } finally {
            endTransaction(success);
        }
    }

    private static final String DEDUPE_ALL    = "dedupeAll";
    private static final String DEDUPE_INBOX  = "moveSentMessageToInbox";
    private static final String DEDUPE_SECOND = "secondCopyifOnToOrCC";

    private boolean dedupe(MimeMessage mm, Integer sentMsgId) throws ServiceException {
        Account acct = getAccount();
        String pref = acct.getAttr(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf, null);
        if (pref == null)                               // default to no deduping
            return false;
        else if (pref.equalsIgnoreCase(DEDUPE_ALL))     // remove all duplicates
            return true;
        else if (pref.equalsIgnoreCase(DEDUPE_SECOND))  // receive if we're not a direct recipient (to, cc, bcc)
            try {
                return !AccountUtil.isDirectRecipient(acct, mm);
            } catch (Exception e) {
                return false;
            }
        else if (pref.equalsIgnoreCase(DEDUPE_INBOX))   // move the existing mail from sent to inbox
            // XXX: not implemented
            return false;
        else
            return false;
    }

    public int getConversationIdFromReferent(MimeMessage newMsg, int parentID) {
        try {
            // file into same conversation as parent message as long as subject hasn't really changed
            Message parentMsg = getMessageById(null, parentID);
            if (parentMsg.getNormalizedSubject().equals(ParsedMessage.normalize(newMsg.getSubject())))
                return parentMsg.getConversationId();
        } catch (Exception e) {
            if (!(e instanceof MailServiceException.NoSuchItemException))
                ZimbraLog.mailbox.warn("ignoring error while checking conversation: " + parentID, e);
        }
        return ID_AUTO_INCREMENT;
    }

    public synchronized Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags, String tags, int conversationId)
    throws IOException, ServiceException {
        SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext(false);
        return addMessage(octxt, pm, folderId, noICal, flags, tags, conversationId, ":API:", sharedDeliveryCtxt);
    } 

    public synchronized Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags, String tags)
    throws IOException, ServiceException {
        SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext(false);
        return addMessage(octxt, pm, folderId, noICal, flags, tags, Mailbox.ID_AUTO_INCREMENT, ":API:", sharedDeliveryCtxt);
    }

    public synchronized Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags, String tags,
                                           String rcptEmail, SharedDeliveryContext sharedDeliveryCtxt)
    throws IOException, ServiceException {
        return addMessage(octxt, pm, folderId, noICal, flags, tags, Mailbox.ID_AUTO_INCREMENT, rcptEmail, sharedDeliveryCtxt);
    }

    private static final StopWatch sWatch = StopWatch.getInstance("MailboxAddMessage");
    
    public synchronized Message addMessage(OperationContext octxt, ParsedMessage pm,
                                           int folderId, boolean noICal, int flags, String tagStr, int conversationId,
                                           String rcptEmail, SharedDeliveryContext sharedDeliveryCtxt)
    throws IOException, ServiceException {
        long start = sWatch.start();
        Message msg = addMessageInternal(octxt, pm, folderId, noICal, flags, tagStr, conversationId,
                                         rcptEmail, null, sharedDeliveryCtxt);
        sWatch.stop(start);
        return msg;
    }

    private synchronized Message addMessageInternal(OperationContext octxt, ParsedMessage pm, int folderId,
                                                    boolean noICal, int flags, String tagStr, int conversationId, 
                                                    String rcptEmail, Message.DraftInfo dinfo,
                                                    SharedDeliveryContext sharedDeliveryCtxt)
    throws IOException, ServiceException {
        if (pm == null)
            throw ServiceException.INVALID_REQUEST("null ParsedMessage when adding message to mailbox " + mId, null);

        boolean debug = ZimbraLog.mailbox.isDebugEnabled();

        byte[] data;
        String digest;
        int msgSize;
        try {
            data = pm.getRawData();
            digest = pm.getRawDigest();
            msgSize = pm.getRawSize();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }

        if (conversationId <= HIGHEST_SYSTEM_ID)
            conversationId = ID_AUTO_INCREMENT;

        CreateMessage redoPlayer = (octxt == null ? null : (CreateMessage) octxt.player);

        // quick check to make sure we don't deliver 5 copies of the same message
        String msgidHeader = pm.getMessageID();
        boolean isSent = ((flags & Flag.FLAG_FROM_ME) != 0);
        boolean checkDuplicates = (redoPlayer == null && msgidHeader != null);
        if (checkDuplicates && !isSent && mSentMessageIDs.containsKey(msgidHeader)) {
            Integer sentMsgID = (Integer) mSentMessageIDs.get(msgidHeader);
            // if the rules say to drop this duplicated incoming message, return null now
            if (dedupe(pm.getMimeMessage(), sentMsgID))
                return null;
            // if we're not dropping the new message, see if it goes in the same conversation as the old sent message
            if (conversationId == ID_AUTO_INCREMENT) {
                conversationId = getConversationIdFromReferent(pm.getMimeMessage(), sentMsgID.intValue());
                if (debug)  ZimbraLog.mailbox.debug("  duplicate detected but not deduped (" + msgidHeader + "); " +
                                                    "will try to slot into conversation " + conversationId);
            }
        }

        CreateMessage redoRecorder = new CreateMessage(mId, rcptEmail, sharedDeliveryCtxt.getShared(), digest, msgSize, folderId, noICal, flags, tagStr);
        StoreIncomingBlob storeRedoRecorder = null;

        // Strip out unread flag for internal storage.
        // This should not be done earlier than redoRecorder initialization.
        boolean unread = (flags & Flag.FLAG_UNREAD) > 0;
        flags = flags & ~Flag.FLAG_UNREAD;

        boolean isSpam = folderId == ID_FOLDER_SPAM;
        boolean isDraft = ((flags & Flag.FLAG_DRAFT) != 0);

        Message msg = null;
        Blob blob = null;
        MailboxBlob mboxBlob = null;
        boolean success = false;
        try {
            if (redoPlayer != null) {
                beginTransaction("addMessage", octxt, redoRecorder);
                rcptEmail = redoPlayer.getRcptEmail();
            } else
                beginTransaction("addMessage", octxt, pm, redoRecorder);

            // the caller can specify the received date via redo recorder, ParsedMessge constructor, or X-Zimbra-Received header
            long timestamp = getOperationTimestampMillis();

            // "having attachments" is currently tracked via flags
            if (pm.hasAttachments())
                flags |= Flag.FLAG_ATTACHED;
            else
                flags &= ~Flag.FLAG_ATTACHED;

            Folder folder  = getFolderById(folderId);
            String subject = pm.getNormalizedSubject();
            long   tags    = Tag.tagsToBitmask(tagStr);

            // step 1: get an ID assigned for the new message
            int messageId  = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());
            if (redoPlayer != null)
                conversationId = redoPlayer.getConversationId();

            // step 2: figure out where the message belongs
            Conversation conv = null;
            String hash = null;
            if (!DebugConfig.disableConversation) {
                if (conversationId != ID_AUTO_INCREMENT)
                    try {
                        conv = getConversationById(conversationId);
                        if (debug)  ZimbraLog.mailbox.debug("  fetched explicitly-specified conversation " + conv.getId());
                    } catch (ServiceException e) {
                        if (e.getCode() != MailServiceException.NO_SUCH_CONV)
                            throw e;
                        if (debug)  ZimbraLog.mailbox.debug("  could not find explicitly-specified conversation " + conversationId);
                    }
                else if (conv == null && !isSpam && !isDraft && pm.isReply()) {
                    conv = getConversationByHash(hash = getHash(subject));
                    if (debug)  ZimbraLog.mailbox.debug("  found conversation " + (conv == null ? -1 : conv.getId()) + " for hash: " + hash);
                    if (conv != null && timestamp > conv.getDate() + ONE_MONTH_MILLIS) {
                        conv = null;
                        if (debug)  ZimbraLog.mailbox.debug("  but rejected it because it's too old");
                    }
                }
            }

            // step 3: create the message and update the cache
            //         and if the message is also an invite, deal with the appointment
            Conversation convTarget = (conv instanceof VirtualConversation ? null : conv);
            if (convTarget != null && debug)
                ZimbraLog.mailbox.debug("  placing message in existing conversation " + convTarget.getId());

            short volumeId = redoPlayer == null ? Volume.getCurrentMessageVolume().getId()
                                                : redoPlayer.getVolumeId();
            Calendar iCal = pm.getiCalendar();
            msg = Message.create(messageId, folder, convTarget, pm, msgSize, digest,
                                 volumeId, unread, flags, tags, dinfo, noICal, iCal);
            
            redoRecorder.setMessageId(msg.getId());

            // step 4: create a conversation for the message, if necessary
            if (!DebugConfig.disableConversation && convTarget == null) {
                if (conv == null && conversationId == ID_AUTO_INCREMENT) {
                    conv = VirtualConversation.create(this, msg);
                    if (debug)  ZimbraLog.mailbox.debug("  placed message " + msg.getId() + " in vconv " + conv.getId());
                } else {
                    VirtualConversation vconv = (VirtualConversation) conv;
                    Message[] contents = (conv == null ? new Message[] { msg } : new Message[] { vconv.getMessage(), msg });
                    conv = createConversation(contents, conversationId);
                    if (vconv != null) {
                        if (debug)  ZimbraLog.mailbox.debug("  removed vconv " + vconv.getId());
                        vconv.removeChild(vconv.getMessage());
                    }
                }
                if (!isSpam && !isDraft)
                    openConversation(conv, hash);
            }
            redoRecorder.setConversationId(conv != null && !(conv instanceof VirtualConversation) ? conv.getId() : -1);

            // step 5: remember the Message-ID header so that we can avoid receiving duplicates
            if (isSent && checkDuplicates)
                mSentMessageIDs.put(msgidHeader, new Integer(msg.getId()));

            // step 6: store the blob
            // TODO: Add partition support.  Need to store as many times as there
            //       are unique partitions in the set of recipient mailboxes.
            blob = sharedDeliveryCtxt.getBlob();
            StoreManager sm = StoreManager.getInstance();
            if (blob == null) {
                // This mailbox is the only recipient, or it is the first
                // of multiple recipients.  Save message to incoming directory.
                if (redoPlayer == null)
                    blob = sm.storeIncoming(data, digest,
                                            null, msg.getVolumeId());
                else
                    blob = sm.storeIncoming(data, digest,
                                            redoPlayer.getPath(),
                                            redoPlayer.getVolumeId());
                String blobPath = blob.getPath();
                short blobVolumeId = blob.getVolumeId();

                if (sharedDeliveryCtxt.getShared()) {
                    markOtherItemDirty(blob);

                    // Log entry in redolog for blob save.  Blob bytes are
                    // logged in StoreToIncoming entry.
                    storeRedoRecorder = new StoreIncomingBlob(digest, msgSize);
                    storeRedoRecorder.start(timestamp);
                    storeRedoRecorder.setBlobBodyInfo(data, blobPath, blobVolumeId);
                    storeRedoRecorder.log();

                    // Create a link in mailbox directory and leave the incoming
                    // copy alone, so other recipients can link to it later.
                    redoRecorder.setMessageLinkInfo(blobPath, blobVolumeId, msg.getVolumeId());
                    mboxBlob = sm.link(blob, this, messageId, msg.getSavedSequence(), msg.getVolumeId());
                } else {
                    // If the only recipient, move the incoming copy into
                    // mailbox directory.  This is more efficient than
                    // creating a link in mailbox directory and deleting
                    // incoming copy.
                    mboxBlob = sm.renameTo(blob, this, messageId, msg.getSavedSequence(), msg.getVolumeId());

                    // In single-recipient case the blob bytes are logged in
                    // CreateMessage entry, to avoid having to write two
                    // redolog entries for a single delivery.
                    redoRecorder.setMessageBodyInfo(data, blobPath, blobVolumeId);
                }
            } else {
                String srcPath;
                Blob srcBlob;
                MailboxBlob srcMboxBlob = sharedDeliveryCtxt.getMailboxBlob();
                if (srcMboxBlob != null && srcMboxBlob.getMailbox().getId() == mId) {
                    // With filter rules, a message can be copied to one or
                    // more folders and optionally kept in Inbox, meaning
                    // one delivery can result in multiple deliveries.  But
                    // the first copy delivered will not know there are copies
                    // coming, and if there was only one recipient for the
                    // message, we will end up doing the rename case above.
                    // Second and later copies cannot link to the blob file
                    // in incoming directory because it was renamed out.
                    // Instead they have to link to the MailboxBlob file of
                    // the previous delivery.  (Bug 2283)
                    srcPath = srcMboxBlob.getPath();
                    srcBlob = srcMboxBlob.getBlob();
                } else {
                    // Second or later recipient in multi-recipient message.
                    // Link to blob in incoming directory.
                    srcPath = blob.getPath();
                    srcBlob = blob;
                }
                redoRecorder.setMessageLinkInfo(srcPath, srcBlob.getVolumeId(), msg.getVolumeId());
                mboxBlob = sm.link(srcBlob, this, messageId, msg.getSavedSequence(), msg.getVolumeId());
            }
            markOtherItemDirty(mboxBlob);

            mCurrentChange.setIndexedItem(msg, pm);
            success = true;
        } finally {
            // XXX: should we do this via mOtherDirtyStuff instead?
            if (!success && isSent && checkDuplicates)
                mSentMessageIDs.remove(msgidHeader);

            if (storeRedoRecorder != null) {
                if (success)
                    storeRedoRecorder.commit();
                else
                    storeRedoRecorder.abort();
            }

            endTransaction(success);

            if (success) {
                // Everything worked.  Update the blob field in ParsedMessage
                // so the next recipient in the multi-recipient case will link
                // to this blob as opposed to saving its own copy.
                sharedDeliveryCtxt.setBlob(blob);
                sharedDeliveryCtxt.setMailboxBlob(mboxBlob);
            }
        }

        if (msg != null && ZimbraLog.mailbox.isInfoEnabled())
            ZimbraLog.mailbox.info("Added message id=" + msg.getId() + " digest=" + digest +
                                   " mailbox=" + getId() + " rcpt=" + rcptEmail);
        return msg;
    }

    static String getHash(String subject) {
        return ByteUtil.getSHA1Digest(subject.getBytes(), true);
    }

    // please keep this package-visible but not public
    void openConversation(Conversation conv, String hash) throws ServiceException {
        if (hash == null)
            hash = getHash(conv.getSubject());
        conv.open(hash);
        markOtherItemDirty(hash);
        mConvHashes.put(hash, new Integer(conv.getId()));
    }

    // please keep this package-visible but not public
    void closeConversation(Conversation conv, String hash) throws ServiceException {
        if (hash == null)
            hash = getHash(conv.getSubject());
        conv.close(hash);
        mConvHashes.remove(hash);
    }

    // please keep this package-visible but not public
    Conversation createConversation(Message[] contents, int id) throws ServiceException {
        id = Math.max(id, ID_AUTO_INCREMENT);
        Conversation conv = Conversation.create(this, getNextItemId(id), contents);
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < contents.length; i++)
                sb.append(i == 0 ? "" : ",").append(contents[i].getId());
            ZimbraLog.mailbox.debug("  created conv " + conv.getId() + " holding msg(s): " + sb);
        }
        return conv;
    }

    public synchronized Message saveDraft(OperationContext octxt, ParsedMessage pm, int id, int origId, String replyType)
    throws IOException, ServiceException {
        // special-case saving a new draft
        if (id == ID_AUTO_INCREMENT) {
            Message.DraftInfo dinfo = null;
            if (replyType != null && origId > 0)
                dinfo = new Message.DraftInfo(replyType, origId);
            return addMessageInternal(octxt, pm, ID_FOLDER_DRAFTS, true, Flag.FLAG_DRAFT | Flag.FLAG_FROM_ME, null,
                                      ID_AUTO_INCREMENT, ":API:", dinfo, new SharedDeliveryContext(false));
        }

        byte[] data;
        String digest;
        int size;
        try {
            data = pm.getRawData();  digest = pm.getRawDigest();  size = pm.getRawSize();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }

        SaveDraft redoRecorder = new SaveDraft(mId, id, digest, size);
        boolean success = false;
        Blob blob = null;
        try {
            beginTransaction("saveDraft", octxt, redoRecorder);
            SaveDraft redoPlayer = (SaveDraft) mCurrentChange.getRedoPlayer();

            Message msg = getMessageById(id);
            if (!msg.isTagged(mDraftFlag))
                throw MailServiceException.IMMUTABLE_OBJECT(id);
            if (!checkItemChangeID(msg))
                throw MailServiceException.MODIFY_CONFLICT();

            // content changed, so we're obliged to change the IMAP uid
            int imapID = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapId());
            redoRecorder.setImapId(imapID);

            // update the content and increment the revision number
            msg.setContent(pm, digest, size, imapID);

            // write the content to the store
            short volumeId =
                redoPlayer == null ? Volume.getCurrentMessageVolume().getId()
                                   : redoPlayer.getVolumeId();
            StoreManager sm = StoreManager.getInstance();
            blob = sm.storeIncoming(data, digest, null, volumeId);
            MailboxBlob mb = sm.renameTo(blob, this, id, msg.getSavedSequence(), volumeId);
            redoRecorder.setMessageBodyInfo(data, blob.getPath(), blob.getVolumeId());
            markOtherItemDirty(mb);

            // NOTE: msg is now uncached (will this cause problems during commit/reindex?)
            mCurrentChange.setIndexedItem(msg, pm);
            success = true;
            return msg;
        } finally {
            endTransaction(success);
        }
    }
    
    /**
     * Modify the Participant-Status of your LOCAL data part of an appointment -- this is used when you Reply to
     * an Invite so that you can track the fact that you've replied to it.
     * 
     * @param octxt
     * @param apptId
     * @param recurId
     * @param cnStr
     * @param addressStr
     * @param roleStr
     * @param partStatStr
     * @param needsReply
     * @param seqNo
     * @param dtStamp
     * @throws ServiceException
     */
    public synchronized void modifyPartStat(OperationContext octxt, int apptId, RecurId recurId,
            String cnStr, String addressStr, String roleStr, String partStatStr, Boolean needsReply, int seqNo, long dtStamp) 
    throws ServiceException {
        
        ModifyInvitePartStat redoRecorder = new ModifyInvitePartStat(mId, apptId, recurId, cnStr, addressStr, roleStr, partStatStr, needsReply, seqNo, dtStamp);
        
        boolean success = false;
        try {
            beginTransaction("updateInvitePartStat", octxt, redoRecorder);

            Appointment appt = getAppointmentById(apptId);
            
            Account acct = getAccount();
        
            appt.modifyPartStat(acct, recurId, cnStr, addressStr, roleStr, partStatStr, needsReply, seqNo, dtStamp);
            markItemModified(appt, Change.MODIFIED_INVITE);
            
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void resetImapUid(OperationContext octxt, int[] msgIds) throws ServiceException {
        SetImapUid redoRecorder = new SetImapUid(mId, msgIds);

        boolean success = false;
        try {
            beginTransaction("resetImapUid", octxt, redoRecorder);
            SetImapUid redoPlayer = (SetImapUid) mCurrentChange.getRedoPlayer();

            for (int i = 0; i < msgIds.length; i++) {
                Message msg = getMessageById(msgIds[i]);
                int imapId = redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapUid(msgIds[i]);
                msg.setImapUid(getNextItemId(imapId));
                redoRecorder.setImapUid(msg.getId(), msg.getImapUID());
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void setColor(OperationContext octxt, int itemId, byte type, byte color) throws ServiceException {
        ColorItem redoRecorder = new ColorItem(mId, itemId, type, color);

        boolean success = false;
        try {
            beginTransaction("setColor", octxt, redoRecorder);

            MailItem item = getItemById(itemId, type);
            if (!checkItemChangeID(item))
                throw MailServiceException.MODIFY_CONFLICT();

            item.setColor(color);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void alterTag(OperationContext octxt, int itemId, byte type, int tagId, boolean addTag) throws ServiceException {
        alterTag(octxt, itemId, type, tagId, addTag, null);
    }
    public synchronized void alterTag(OperationContext octxt, int itemId, byte type, int tagId, boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        AlterItemTag redoRecorder = new AlterItemTag(mId, itemId, type, tagId, addTag, tcon);

        boolean success = false;
        try {
            beginTransaction("alterTag", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            MailItem item = getItemById(itemId, type);
            if (!(item instanceof Conversation))
                if (!checkItemChangeID(item) && item instanceof Tag)
                    throw MailServiceException.MODIFY_CONFLICT();

            if (tagId == Flag.ID_FLAG_UNREAD)
                item.alterUnread(addTag);
            else {
                Tag tag = (tagId < 0 ? getFlagById(tagId) : getTagById(tagId));
                // don't let the user tag things as "has attachments" or "draft"
                if (tag instanceof Flag && (tag.getBitmask() & Flag.FLAG_SYSTEM) != 0)
                    throw MailServiceException.CANNOT_TAG();
                item.alterTag(tag, addTag);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void setTags(OperationContext octxt, int itemId, byte type, int flags, long tags) throws ServiceException {
        setTags(octxt, itemId, type, flags, tags, null);
    }
    public synchronized void setTags(OperationContext octxt, int itemId, byte type, String flagStr, String tagIDs, TargetConstraint tcon)
    throws ServiceException {
        int flags = (flagStr == null ? MailItem.FLAG_UNCHANGED : Flag.flagsToBitmask(flagStr));
        long tags = (tagIDs == null ? MailItem.TAG_UNCHANGED : Tag.tagsToBitmask(tagIDs));
        setTags(octxt, itemId, type, flags, tags, tcon);
    }
    public synchronized void setTags(OperationContext octxt, int itemId, byte type, int flags, long tags, TargetConstraint tcon)
    throws ServiceException {
        if (flags == MailItem.FLAG_UNCHANGED && tags == MailItem.TAG_UNCHANGED)
            return;

        SetItemTags redoRecorder = new SetItemTags(mId, itemId, type, flags, tags, tcon);

        boolean success = false;
        try {
            beginTransaction("setTags", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            MailItem item = getItemById(itemId, type);
            checkItemChangeID(item);

            if ((flags & MailItem.FLAG_UNCHANGED) != 0)
                flags = item.getFlagBitmask();
            if ((tags & MailItem.TAG_UNCHANGED) != 0)
                tags = item.getTagBitmask();

            // Special-case the unread flag.  It's passed in as a flag from the outside,
            // but treated as a separate argument inside the mailbox.
            boolean unread = (flags & Flag.FLAG_UNREAD) > 0;
            flags &= ~Flag.FLAG_UNREAD;
            item.setTags(flags, tags);
            item.alterUnread(unread);

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized MailItem copy(OperationContext octxt, int itemId, byte type, int targetId) throws IOException, ServiceException {
        CopyItem redoRecorder = new CopyItem(mId, itemId, type, targetId);

        boolean success = false;
        try {
            beginTransaction("copy", octxt, redoRecorder);
            CopyItem redoPlayer = (CopyItem) mCurrentChange.getRedoPlayer();

            MailItem item = getItemById(itemId, type);
            checkItemChangeID(item);

            int newId;
            short destVolumeId;
            if (redoPlayer == null) {
            	newId = getNextItemId(ID_AUTO_INCREMENT);
                if (item.getVolumeId() != -1)
                    destVolumeId = Volume.getCurrentMessageVolume().getId();
                else
                    destVolumeId = -1;
            } else {
            	newId = getNextItemId(redoPlayer.getDestId());
                destVolumeId = redoPlayer.getDestVolumeId();
            }
            MailItem copy = item.copy(getFolderById(targetId), newId, destVolumeId);
            redoRecorder.setDestId(copy.getId());
            redoRecorder.setDestVolumeId(copy.getVolumeId());

            success = true;
            return copy;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void move(OperationContext octxt, int itemId, byte type, int targetId) throws ServiceException {
        move(octxt, itemId, type, targetId, null);
    }
    public synchronized void move(OperationContext octxt, int itemId, byte type, int targetId, TargetConstraint tcon)
    throws ServiceException {
        MoveItem redoRecorder = new MoveItem(mId, itemId, type, targetId, tcon);

        boolean success = false;
        try {
            beginTransaction("move", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            MailItem item = getItemById(itemId, type);
            checkItemChangeID(item);

            item.move(getFolderById(targetId));
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void delete(OperationContext octxt, int itemId, byte type) throws ServiceException {
        delete(octxt, itemId, type, null);
    }
    public synchronized void delete(OperationContext octxt, int itemId, byte type, TargetConstraint tcon) throws ServiceException {
        DeleteItem redoRecorder = new DeleteItem(mId, itemId, type, tcon);

        boolean success = false;
        try {
            beginTransaction("delete", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            MailItem item = getItemById(itemId, type);
            if (!checkItemChangeID(item) && item instanceof Tag)
                throw MailServiceException.MODIFY_CONFLICT();

            item.delete();
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized Tag createTag(OperationContext octxt, String name, byte color) throws ServiceException {
        name = StringUtil.stripControlCharacters(name);
        if (name == null || name.equals(""))
            throw ServiceException.INVALID_REQUEST("tag must have a name", null);

        CreateTag redoRecorder = new CreateTag(mId, name, color);

        boolean success = false;
        try {
            beginTransaction("createTag", octxt, redoRecorder);
            CreateTag redoPlayer = (CreateTag) mCurrentChange.getRedoPlayer();

            int tagId = (redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getTagId());
            if (tagId != ID_AUTO_INCREMENT)
                if (!Tag.validateId(tagId))
                    throw ServiceException.INVALID_REQUEST("invalid tag id " + tagId, null);

            if (tagId == ID_AUTO_INCREMENT) {
                for (tagId = MailItem.TAG_ID_OFFSET; tagId < MailItem.TAG_ID_OFFSET + MailItem.MAX_TAG_COUNT; tagId++)
                    if (mTagCache.get(new Integer(tagId)) == null)
                        break;
                if (tagId >= MailItem.TAG_ID_OFFSET + MailItem.MAX_TAG_COUNT)
                    throw MailServiceException.TOO_MANY_TAGS();
            }

            Tag tag = Tag.create(this, tagId, name, color);
            redoRecorder.setTagId(tagId);
            success = true;
            return tag;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void renameTag(OperationContext octxt, int id, String name) throws ServiceException {
        name = StringUtil.stripControlCharacters(name);
        if (name == null || name.equals(""))
            throw ServiceException.INVALID_REQUEST("tag must have a name", null);

        RenameTag redoRecorder = new RenameTag(mId, id, name);

        boolean success = false;
        try {
            beginTransaction("renameTag", octxt, redoRecorder);

            Tag tag = getTagById(id);
            if (!checkItemChangeID(tag))
                throw MailServiceException.MODIFY_CONFLICT();

            String oldName = tag.getName();
            tag.rename(name);

            mTagCache.remove(oldName.toLowerCase());
            mTagCache.put(name.toLowerCase(), tag);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized Note createNote(OperationContext octxt, String content, Rectangle location, byte color, int folderId)
    throws ServiceException {
        content = StringUtil.stripControlCharacters(content);
        if (content == null || content.equals(""))
            throw ServiceException.INVALID_REQUEST("note content may not be empty", null);

        CreateNote redoRecorder = new CreateNote(mId, folderId, content, color, location);

        boolean success = false;
        try {
            beginTransaction("createNote", octxt, redoRecorder);
            CreateNote redoPlayer = (CreateNote) mCurrentChange.getRedoPlayer();

            int noteId;
            short volumeId;
            if (redoPlayer == null) {
                noteId = getNextItemId(ID_AUTO_INCREMENT);
                volumeId = Volume.getCurrentMessageVolume().getId();
            } else {
                noteId = getNextItemId(redoPlayer.getNoteId());
                volumeId = redoPlayer.getVolumeId();
            }
            Note note = Note.create(noteId, getFolderById(folderId), volumeId, content, location, color);

            redoRecorder.setNoteId(note.getId());
            redoRecorder.setVolumeId(note.getVolumeId());
            mCurrentChange.setIndexedItem(note, null);
            success = true;
            return note;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void editNote(OperationContext octxt, int noteId, String content) throws ServiceException {
        content = StringUtil.stripControlCharacters(content);
        if (content == null || content.equals(""))
            throw ServiceException.INVALID_REQUEST("note content may not be empty", null);

        EditNote redoRecorder = new EditNote(mId, noteId, content);

        boolean success = false;
        try {
            beginTransaction("editNote", octxt, redoRecorder);

            Note note = getNoteById(noteId);
            checkItemChangeID(note);

            note.setContent(content);
            mCurrentChange.setIndexedItem(note, null);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void repositionNote(OperationContext octxt, int noteId, Rectangle location)
    throws ServiceException {
        if (location == null)
            throw new IllegalArgumentException("must specify note bounds");

        RepositionNote redoRecorder = new RepositionNote(mId, noteId, location);

        boolean success = false;
        try {
            beginTransaction("repositionNote", octxt, redoRecorder);

            Note note = getNoteById(noteId);
            checkItemChangeID(note);

            note.reposition(location);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    Appointment createAppointment(int folderId, short volumeId, String tags, String uid, ParsedMessage pm, Invite invite)
    throws ServiceException {
        // FIXME: assuming that we're in the middle of a CreateMessage op
        CreateAppointmentPlayer redoPlayer   = (CreateAppointmentPlayer) mCurrentChange.getRedoPlayer();
        CreateAppointmentRecorder redoRecorder = (CreateAppointmentRecorder) mCurrentChange.getRedoRecorder();

        int newApptId = redoPlayer == null ? Mailbox.ID_AUTO_INCREMENT : redoPlayer.getAppointmentId();
        int createId = getNextItemId(newApptId);

        Appointment appt = Appointment.create(createId, getFolderById(folderId), volumeId, tags, uid, pm, invite);

        if (redoRecorder != null)
        	redoRecorder.setAppointmentId(appt.getId());
        return appt;
    }

    public synchronized Contact createContact(OperationContext octxt, Map attrs, int folderId, String tags)
    throws ServiceException {
        CreateContact redoRecorder = new CreateContact(mId, folderId, attrs, tags);

        boolean success = false;
        try {
            beginTransaction("createContact", octxt, redoRecorder);
            CreateContact redoPlayer = (CreateContact) mCurrentChange.getRedoPlayer();

            int contactId;
            short volumeId;
            if (redoPlayer == null) {
            	contactId = getNextItemId(ID_AUTO_INCREMENT);
                volumeId = Volume.getCurrentMessageVolume().getId();
            } else {
            	contactId = getNextItemId(redoPlayer.getContactId());
                volumeId = redoPlayer.getVolumeId();
            }
            Contact con = Contact.create(contactId, getFolderById(folderId), volumeId, attrs, tags);

            redoRecorder.setContactId(con.getId());
            redoRecorder.setVolumeId(con.getVolumeId());
            mCurrentChange.setIndexedItem(con, null);
            success = true;
            return con;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void modifyContact(OperationContext octxt, int contactId, Map attrs, boolean replace)
    throws ServiceException {
        ModifyContact redoRecorder = new ModifyContact(mId, contactId, attrs, replace);

        boolean success = false;
        try {
            beginTransaction("modifyContact", octxt, redoRecorder);

            Contact con = getContactById(contactId);
            if (!checkItemChangeID(con))
                throw MailServiceException.MODIFY_CONFLICT();
            con.modify(attrs, replace);

            mCurrentChange.setIndexedItem(con, null);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized Folder createFolder(OperationContext octxt, String name, int parentId, byte defaultView) throws ServiceException {
        Folder.validateFolderName(name);
        String path = getFolderById(parentId).getPath() + (parentId == ID_FOLDER_USER_ROOT ? "" : "/") + name;
        return createFolder(octxt, path, (byte) 0, defaultView);
    }
    public synchronized Folder createFolder(OperationContext octxt, String path, byte attrs, byte defaultView) throws ServiceException {
        if (path != null) {
            path = path.trim();
            if (!path.startsWith("/"))
                path = path + "/";
            if (path.endsWith("/") && path.length() > 1)
                path = path.substring(0, path.length() - 1);
        }
        CreateFolder redoRecorder = new CreateFolder(mId, path, attrs, defaultView);

        boolean success = false;
        try {
            beginTransaction("createFolder", octxt, redoRecorder);
            CreateFolder redoPlayer = (CreateFolder) mCurrentChange.getRedoPlayer();

            String[] parts = path.substring(1).split("/");
            if (parts.length == 0)
                throw MailServiceException.ALREADY_EXISTS(path);
            int[] recorderFolderIds = new int[parts.length];
            int[] playerFolderIds = redoPlayer == null ? null : redoPlayer.getFolderIds();
            if (playerFolderIds != null && playerFolderIds.length != recorderFolderIds.length)
                throw ServiceException.FAILURE("incorrect number of path segments in redo player", null);

            Folder folder = getFolderById(ID_FOLDER_USER_ROOT);
            for (int i = 0; i < parts.length; i++) {
                boolean last = i == parts.length - 1;
                int folderId = playerFolderIds == null ? ID_AUTO_INCREMENT : playerFolderIds[i];
                Folder subfolder = folder.findSubfolder(parts[i]);
                if (subfolder == null)
                    subfolder = Folder.create(getNextItemId(folderId), this, folder, parts[i], (byte) 0, last ? defaultView : MailItem.TYPE_UNKNOWN);
                else if (folderId != ID_AUTO_INCREMENT && folderId != subfolder.getId())
                    throw ServiceException.FAILURE("parent folder id changed since operation was recorded", null);
                else if (last)
                    throw MailServiceException.ALREADY_EXISTS(path);
                recorderFolderIds[i] = subfolder.getId();
                folder = subfolder;
            }
            redoRecorder.setFolderIds(recorderFolderIds);
            success = true;
            return folder;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void grantAccess(OperationContext octxt, int folderId, String grantee, byte granteeType, short rights, boolean inherit) throws ServiceException {
        // FIXME: add redoplayer

        boolean success = false;
        try {
            beginTransaction("grantAccess", octxt, null);

            Folder folder = getFolderById(folderId);
            folder.grantAccess(grantee, granteeType, rights, inherit);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void revokeAccess(OperationContext octxt, int folderId, String grantee) throws ServiceException {
        // FIXME: add redoplayer

        boolean success = false;
        try {
            beginTransaction("revokeAccess", octxt, null);

            Folder folder = getFolderById(folderId);
            folder.revokeAccess(grantee);
            success = true;
        } finally {
            endTransaction(success);
        }
    }
    
    public synchronized void refreshFolder(OperationContext octxt, int folderId, String urlOrNull) throws ServiceException {
        
        Folder folder = getFolderById(folderId);
        
        if (urlOrNull != null) { // update the URL setting
            boolean success = false;
            try {
                beginTransaction("refreshFolder", octxt, null);
                
                checkItemChangeID(folder);
                
                String curUrl = folder.getUrl();
                if (urlOrNull.equals("")) {
                    // clear existing
                    if (curUrl != null) {
                        folder.setUrl(null);
                        folder.saveMetadata();
                    }
                } else if (curUrl == null || !curUrl.equals(urlOrNull)) {
                    // update existing
                    folder.setUrl(urlOrNull);
                    folder.saveMetadata();
                }
                success = true;
            } finally {
                endTransaction(success);
            }
        }
        
        {
            boolean success = false;
            try {
                beginTransaction("refreshFolderPart2", octxt, null);
                folder.empty(false);
                success = true;
            } finally {
                endTransaction(success);
            }
        }
        
        if (folder.getUrl() != null && !folder.getUrl().equals("")) {
            retrieveRemoteCalendar(folder.getUrl(), folderId);
        }
    }

    public synchronized void renameFolder(OperationContext octxt, int folderId, String name) throws ServiceException {
        RenameFolder redoRecorder = new RenameFolder(mId, folderId, name);

        boolean success = false;
        try {
            beginTransaction("renameFolder", octxt, redoRecorder);
            RenameFolder redoPlayer = (RenameFolder) mCurrentChange.getRedoPlayer();

            Folder folder = getFolderById(folderId), parent;
            checkItemChangeID(folder);

            if (name.startsWith("/")) {
                String[] parts = name.substring(1).split("/");
                if (parts.length == 0)
                    throw MailServiceException.ALREADY_EXISTS(name);
                int[] recorderParentIds = new int[parts.length - 1];
                int[] playerParentIds = redoPlayer == null ? null : redoPlayer.getParentIds();
                if (playerParentIds != null && playerParentIds.length != recorderParentIds.length)
                    throw ServiceException.FAILURE("incorrect number of path segments in redo player", null);

                parent = getFolderById(ID_FOLDER_USER_ROOT);
                for (int i = 0; i < parts.length - 1; i++) {
                    Folder.validateFolderName(parts[i]);
                    int subfolderId = playerParentIds == null ? ID_AUTO_INCREMENT : playerParentIds[i];
                    Folder subfolder = parent.findSubfolder(parts[i]);
                    if (subfolder == null)
                        subfolder = Folder.create(getNextItemId(subfolderId), this, parent, parts[i]);
                    else if (subfolderId != ID_AUTO_INCREMENT && subfolderId != subfolder.getId())
                        throw ServiceException.FAILURE("parent folder id changed since operation was recorded", null);
                    else if (!subfolder.getName().equals(parts[i]) && subfolder.isMutable())
                    	subfolder.rename(parts[i], parent);
                    recorderParentIds[i] = subfolder.getId();
                    parent = subfolder;
                }
                name = parts[parts.length - 1];
                redoRecorder.setParentIds(recorderParentIds);
            } else
                parent = folder.getFolder();
            folder.rename(name, parent);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void emptyFolder(OperationContext octxt, int folderId, boolean removeSubfolders)
    throws ServiceException {
        EmptyFolder redoRecorder = new EmptyFolder(mId, folderId, removeSubfolders);

        boolean success = false;
        try {
            beginTransaction("emptyFolder", octxt, redoRecorder);

            Folder folder = getFolderById(folderId);
            folder.empty(removeSubfolders);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized SearchFolder createSearchFolder(OperationContext octxt, int folderId, String name, String query, String types, String sort)
    throws ServiceException {
        CreateSavedSearch redoRecorder = new CreateSavedSearch(mId, folderId, name, query, types, sort);
        
        boolean success = false;
        try {
            beginTransaction("createSearchFolder", octxt, redoRecorder);
            CreateSavedSearch redoPlayer = (CreateSavedSearch) mCurrentChange.getRedoPlayer();

            int searchId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getSearchId());
            SearchFolder search = SearchFolder.create(searchId, getFolderById(folderId), name, query, types, sort);
            redoRecorder.setSearchId(search.getId());
            success = true;
            return search;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void modifySearchFolder(OperationContext octxt, int id, String query, String types, String sort)
    throws ServiceException {
        ModifySavedSearch redoRecorder = new ModifySavedSearch(mId, id, query, types, sort);

        boolean success = false;
        try {
            beginTransaction("modifySearchFolder", octxt, redoRecorder);

            SearchFolder search = getSearchFolderById(id);
            checkItemChangeID(search);

            search.changeQuery(query, types, sort);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized Mountpoint createMountpoint(OperationContext octxt, int folderId, String name, String ownerId, int remoteId, byte view)
    throws ServiceException {
        CreateMountpoint redoRecorder = new CreateMountpoint(mId, folderId, name, ownerId, remoteId, view);

        boolean success = false;
        try {
            beginTransaction("createMountpoint", octxt, redoRecorder);
            CreateMountpoint redoPlayer = (CreateMountpoint) mCurrentChange.getRedoPlayer();

            int mptId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getId());
            Mountpoint mpt = Mountpoint.create(mptId, getFolderById(folderId), name, ownerId, remoteId, view);
            redoRecorder.setId(mpt.getId());
            success = true;
            return mpt;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void purgeMessages(OperationContext octxt) throws ServiceException {
        Account acct = getAccount();
        int globalTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailMessageLifetime, 0) / 1000);
        int trashTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailTrashLifetime, 0) / 1000);
        int spamTimeout  = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailSpamLifetime, 0) / 1000);
        if (globalTimeout <= 0 && trashTimeout <= 0 && spamTimeout <= 0)
            return;

        // sanity-check the really dangerous value...
        if (globalTimeout > 0 && globalTimeout < ONE_MONTH_SECS) {
            // this min is also used by POP3 EXPIRE command. update Pop3Handler.MIN_EPXIRE_DAYS if it changes.
            ZimbraLog.mailbox.warn("global message timeout < 1 month; defaulting to 31 days");
            globalTimeout = ONE_MONTH_SECS;
        }

        PurgeOldMessages redoRecorder = new PurgeOldMessages(mId);

        boolean success = false;
        try {
            beginTransaction("purgeMessages", octxt, redoRecorder);

            // get the folders we're going to be purging
            Folder trash = getFolderById(ID_FOLDER_TRASH);
            Folder spam  = getFolderById(ID_FOLDER_SPAM);

            if (globalTimeout > 0)
                Folder.purgeMessages(this, null, getOperationTimestamp() - globalTimeout);
            if (trashTimeout > 0)
                Folder.purgeMessages(this, trash, getOperationTimestamp() - trashTimeout);
            if (spamTimeout > 0)
                Folder.purgeMessages(this, spam, getOperationTimestamp() - spamTimeout);

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Be very careful when changing code in this method.  The order of almost
     * every line of code is important to ensure correct redo logging and crash
     * recovery.
     * @param success
     * @throws ServiceException
     */
    private synchronized void endTransaction(boolean success)
    throws ServiceException {
        if (!mCurrentChange.isActive()) {
            // would like to throw here, but it might cover another exception...
            ZimbraLog.mailbox.warn("cannot end a transaction when not inside a transaction", new Exception());
            return;
        }
        if (!mCurrentChange.endChange())
            return;

        Connection conn = mCurrentChange.conn;

        // Failure case is very simple.  Just rollback the database and cache
        // and return.  We haven't logged anything to the redo log for this
        // transaction, so no redo cleanup is necessary.
        if (!success) {
            if (conn != null)
                DbPool.quietRollback(conn);
            rollbackCache(mCurrentChange);
            return;
        }

        RedoableOp redoRecorder = mCurrentChange.recorder;
        IndexItem indexRedo = null;
        MailItem itemToIndex = mCurrentChange.indexItem;
        boolean indexingNeeded = redoRecorder != null && itemToIndex != null && !DebugConfig.disableIndexing; 

        // 1. Log the change redo record for main transaction.
        //    If indexing is to be followed, log this entry
        //    without requiring fsync, because logging for
        //    indexing entry will do fsync, which will fsync
        //    this entry at the same time.
        if (redoRecorder != null)
            redoRecorder.log(!indexingNeeded);

        boolean allGood = false;
        try {
            if (indexingNeeded) {
                indexRedo = new IndexItem(mId,
                                          itemToIndex.getId(),
                                          itemToIndex.getType());
                indexRedo.start(getOperationTimestampMillis());
                indexRedo.setParentOp(redoRecorder);

                // 2. Index the item before committing the main
                // transaction.  This allows us to fail the entire
                // transaction when indexing fails.  Write the change
                // record for indexing only after indexing actually
                // works.
                itemToIndex.reindex(indexRedo, mCurrentChange.indexData);

                // 3. Write the change redo record for indexing
                //    sub-transaction to guarantee that it appears in the
                //    redo log stream before the commit record for main
                //    transaction.  If main transaction commit record is
                //    written first and the server crashes before writing
                //    the indexing change record, we won't be able to
                //    re-execute indexing during crash recovery, and we will
                //    end up with an unindexed item.
                indexRedo.log();
            }

            // 4. Commit the main transaction in database.
            if (conn != null)
                try {
                    conn.commit();
                } catch (Throwable t) {
                    // Any exception during database commit is a disaster
                    // because we don't know if the change is committed or
                    // not.  Force the server to abort.  Next restart will
                    // redo the operation to ensure the change is made and
                    // committed.  (bug 2121)
                    ZimbraLog.mailbox.fatal("Unable to commit database transaction.  Forcing server to abort.", t);
                    Runtime.getRuntime().exit(1);
                }
            allGood = true;
        } finally {
            if (!allGood) {
                // We will get here if indexing commit failed.
                // (Database commit hasn't happened.)
    
                // Write abort redo records to prevent the transactions from
                // being redone during crash recovery.
    
                // Write abort redo entries before doing database rollback.
                // If we do rollback first and server crashes, crash
                // recovery will try to redo the operation.
    
                // Write abort redo record for indexing transaction before writing
                // abort record for main.  This prevents indexing from
                // being redone during crash recovery when main transaction
                // was never committed.
                if (indexRedo != null)
                    indexRedo.abort();
                if (redoRecorder != null)
                    redoRecorder.abort();
                if (conn != null)
                    DbPool.quietRollback(conn);
                rollbackCache(mCurrentChange);
                return;
            }
        }

        // 5. Write commit record for main transaction.
        //    By writing the commit record for main transaction before
        //    calling MailItem.reindex(), we are guaranteed to see the
        //    commit-main record in the redo stream before
        //    commit-index record.  This order ensures that during
        //    crash recovery the main transaction is redone before
        //    indexing.  If the order were reversed, crash recovery
        //    would attempt to index an item which hasn't been created
        //    yet or would attempt to index the item with
        //    pre-modification value.  The first case would result in
        //    a redo error, and the second case would index the wrong
        //    value.
        if (redoRecorder != null)
            redoRecorder.commit();

        // 6. The commit redo record for indexing sub-transaction is
        //    written in batch by another thread.  To avoid the batch
        //    commit thread's writing commit-index before this thread's
        //    writing commit-main (step 5 above), the index redo object
        //    is initialized to block the commit attempt by default.
        //    At this point we've written the commit-main record, so
        //    unblock the commit on indexing.
        if (indexRedo != null)
            indexRedo.allowCommit();

        // 7. We are finally done with database and redo commits.
        //    Cache update comes last.
        commitCache(mCurrentChange);
    }

    private void commitCache(MailboxChange change) {
        try {
            // committing changes, so notify any listeners
            for (Iterator it = mListeners.iterator(); it.hasNext(); )
                ((Session) it.next()).notifyPendingChanges(change.mDirty);

            // don't care about committed changes to external items
            MailItem.PendingDelete deleted = null;
            for (Iterator it = change.mOtherDirtyStuff.iterator(); it.hasNext(); ) {
                Object obj = it.next();
                if (obj instanceof MailItem.PendingDelete)
                    deleted = ((MailItem.PendingDelete) obj).add(deleted);
            }

            // the mailbox data has changed, so commit the changes
            if (change.sync != null)
                mData.trackSync = change.sync.booleanValue();
            if (change.size != MailboxChange.NO_CHANGE)
                mData.size = change.size;
            if (change.itemId != MailboxChange.NO_CHANGE)
                mData.lastItemId = change.itemId;
            if (change.contacts != MailboxChange.NO_CHANGE)
                mData.contacts = change.contacts;
            if (change.changeId != MailboxChange.NO_CHANGE && change.changeId > mData.lastChangeId) {
                mData.lastChangeId   = change.changeId;
                mData.lastChangeDate = change.timestamp;
            }
            if (change.config != null)
                mData.config = change.config;

            // delete any index entries associated with items deleted from db
            if (deleted != null && deleted.indexIds != null && deleted.indexIds.size() > 0)
                try {
                    int[] indexIds = new int[deleted.indexIds.size()];
                    for (int i = 0; i < deleted.indexIds.size(); i++)
                        indexIds[i] = ((Integer) deleted.indexIds.get(i)).intValue();
                    int[] deletedIds = getMailboxIndex().deleteDocuments(indexIds);
                    if (deletedIds != indexIds)
                        ZimbraLog.mailbox.warn("could not delete all index entries for items: " + deleted.itemIds);
                } catch (IOException e) {
                    ZimbraLog.mailbox.warn("ignoring error while deleting index entries for items: " + deleted.itemIds, e);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.warn("ignoring error while getting index to delete entries for items: " + deleted.itemIds, e);
                }

            // delete any blobs associated with items deleted from db/index
            StoreManager sm = StoreManager.getInstance();
            if (deleted != null && deleted.blobs != null)
                for (Iterator it = deleted.blobs.iterator(); it.hasNext(); ) {
                    MailboxBlob blob = (MailboxBlob) it.next();
                    try {
                        sm.delete(blob);
                    } catch (IOException e) {
                        ZimbraLog.mailbox.warn("could not delete blob " + blob.getPath() + " during commit");
                    }
                }
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during cache commit", e);
        } finally {
            // keep our MailItem cache at a reasonable size
            trimItemCache();

            // get ready for the next change
            change.reset();
        }
    }

    private void rollbackCache(MailboxChange change) {
        try {
            // rolling back changes, so purge dirty items from the various caches
            Map cache = mCurrentChange.itemCache;
            Map[] maps = new Map[] {change.mDirty.created, change.mDirty.deleted, change.mDirty.modified};
            for (int i = 0; i < maps.length; i++)
                if (maps[i] != null)
                    for (Iterator it = maps[i].values().iterator(); it.hasNext(); ) {
                        Object obj = it.next();
                        if (obj instanceof Change)
                            obj = ((Change) obj).what;

                        if (obj instanceof Tag)
                            purge(MailItem.TYPE_TAG);
                        else if (obj instanceof Folder)
                            purge(MailItem.TYPE_FOLDER);
                        else if (obj instanceof MailItem && cache != null)
                            cache.remove(new Integer(((MailItem) obj).getId()));
                        else if (obj instanceof Integer && cache != null)
                            cache.remove(obj);
                    }

            // roll back any changes to external items
            // FIXME: handle mOtherDirtyStuff:
            //    - LeafNodeInfo (re-index all un-indexed files)
            //    - MailboxBlob         (delink/remove new file)
            //    - String       (remove from mConvHashes map)
            StoreManager sm = StoreManager.getInstance();
            for (Iterator it = change.mOtherDirtyStuff.iterator(); it.hasNext(); ) {
                Object obj = it.next();
                if (obj instanceof MailboxBlob) {
                    MailboxBlob blob = (MailboxBlob) obj;
                    try {
                        sm.delete(blob);
                    } catch (IOException e) {
                        ZimbraLog.mailbox.warn("could not delete blob " + blob.getPath() + " during rollback");
                    }
                } else if (obj instanceof Blob) {
                    Blob blob = (Blob) obj;
                    try {
                        sm.delete(blob);
                    } catch (IOException e) {
                        ZimbraLog.mailbox.warn("could not delete blob " + blob.getPath() + " during rollback");
                    }
                } else if (obj instanceof String && obj != null)
                    mConvHashes.remove(obj);
            }
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during cache rollback", e);
        } finally {
            // keep our MailItem cache at a reasonable size
            trimItemCache();

            // toss any pending changes to the Mailbox object and get ready for the next change
            mCurrentChange.reset();
        }
    }

    private void trimItemCache() {
        try {
            int sizeTarget = mListeners.isEmpty() ? MAX_ITEM_CACHE_WITHOUT_LISTENERS : MAX_ITEM_CACHE_WITH_LISTENERS;
            Map cache = mCurrentChange.itemCache;
            if (cache == null)
                return;
            int excess = cache.size() - sizeTarget;
            if (excess < 0)
                return;
            // cache the overflow to avoid the Iterator's ConcurrentModificationException
            Object[] overflow = new Object[excess];
            int i = 0;
            for (Iterator it = cache.values().iterator(); i < excess && it.hasNext(); ) {
                Object obj = it.next();
                if (obj instanceof MailItem)
                	overflow[i++] = obj;
                else
                    it.remove();
            }
            // trim the excess; note that "uncache" can cascade and take out child items
            while (--i >= 0) {
                if (cache.size() <= sizeTarget)
                    return;
                if (overflow[i] instanceof MailItem)
                    try {
                        uncache((MailItem) overflow[i]);
                    } catch (ServiceException e) { }
            }
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during item cache trim", e);
        }
    }

    public boolean attachmentsIndexingEnabled() throws ServiceException {
        return getAccount().getBooleanAttr(Provisioning.A_zimbraAttachmentsIndexingEnabled, true);
    }

    private void logCacheActivity(Integer key, MailItem item) {
        if (!ZimbraLog.cache.isDebugEnabled())
            return;
        if (item == null) {
            ZimbraLog.cache.debug("Cache miss for item " + key + " in mailbox " + getId());
            return;
        }

        // Don't log cache hits for folders, search folders and tags.  We always
        // keep these in memory, so cache hits are not interesting.
        byte type = item.getType();
        if (isCachedType(type))
            return;
        ZimbraLog.cache.debug("Cache hit for " + MailItem.getNameForType(type) + " " + key + " in mailbox " + getId());
    }


    private static final String CN_ID         = "id";
    private static final String CN_ACCOUNT_ID = "account_id";
    private static final String CN_NEXT_ID    = "next_item_id";
    private static final String CN_SIZE       = "size";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("mailbox: {");
        sb.append(CN_ID).append(": ").append(mId).append(", ");
        sb.append(CN_ACCOUNT_ID).append(": ").append(mData.accountId).append(", ");
        sb.append(CN_NEXT_ID).append(": ").append(mData.lastItemId).append(", ");
        sb.append(CN_SIZE).append(": ").append(mData.size);
        sb.append("}");
        return sb.toString();
    }


    // Coordinate backup and shared delivery, delivery of a message to
    // multiple recipients.  Full backup of a mailbox and shared delivery
    // are mutually exclusive.  More precisely, a backup may not begin
    // when there is a shared delivery in progress for the mailbox.
    // Delivery of a shared message to the mailbox must be denied and
    // deferred when the mailbox is being backed up or has a backup request
    // pending.
    private static class BackupAndSharedDeliveryCoordinator {
        public int mNumDelivs;
        public boolean mBackupModeOn;
        public BackupAndSharedDeliveryCoordinator() {
            mNumDelivs = 0;
            mBackupModeOn = false;
        }
    }

    private BackupAndSharedDeliveryCoordinator mBackupSharedDelivCoord =
        new BackupAndSharedDeliveryCoordinator();

    /**
     * Puts mailbox in shared delivery mode.  A shared delivery is delivery of
     * a message to multiple recipients.  Full backup on mailbox is disallowed
     * while mailbox is in shared delivery mode.  (See bug 2187)
     * Conversely, a shared delivery may not start on a mailbox that is
     * currently being backed up or when there is a pending backup request.
     * For example, thread A puts mailbox in shared delivery mode.  Thread B
     * then tries to backup the mailbox.  Backup cannot start until thread A is
     * done, but mailbox is immediately put into backup-pending mode.
     * Thread C then tries to do another shared delivery on the mailbox, but
     * is not allowed to do so because of thread B's pending backup request.
     * A thread that calls this method must call endSharedDelivery() after
     * delivering the message.
     * @return true if shared delivery may begin; false if shared delivery may
     *         not begin because of a pending backup request
     */
    public boolean beginSharedDelivery() {
        synchronized (mBackupSharedDelivCoord) {
            assert(mBackupSharedDelivCoord.mNumDelivs >= 0);
            if (!mBackupSharedDelivCoord.mBackupModeOn) {
                mBackupSharedDelivCoord.mNumDelivs++;
                return true;
            } else {
                // If backup request is pending on this mailbox, don't allow
                // any more shared deliveries from starting.
                return false;
            }
        }
    }

    /**
     * @see com.zimbra.cs.mailbox.Mailbox#beginSharedDelivery()
     */
    public void endSharedDelivery() {
        synchronized (mBackupSharedDelivCoord) {
            mBackupSharedDelivCoord.mNumDelivs--;
            assert(mBackupSharedDelivCoord.mNumDelivs >= 0);
            if (mBackupSharedDelivCoord.mNumDelivs == 0) {
                // Wake up any waiting backup thread.
                mBackupSharedDelivCoord.notifyAll();
            }
        }
    }

    /**
     * Turns backup mode on/off.  If turning on, waits until backup can begin,
     * i.e. until all currently ongoing shared deliveries finish.  A thread
     * turning backup mode on must turn it off at the end of backup, otherwise
     * no further shared deliveries are possible to the mailbox.
     * @param onoff
     */
    public void setBackupMode(boolean onoff) {
        synchronized (mBackupSharedDelivCoord) {
            if (onoff) {
                // turning on
                mBackupSharedDelivCoord.mBackupModeOn = true;
            } else {
                // turning off
                mBackupSharedDelivCoord.mBackupModeOn = false;
                mBackupSharedDelivCoord.notifyAll();
            }
        }
    }

    /**
     * Wait until backup can begin on this mailbox.  Backup may begin when
     * there is no shared delivery in progress.  Call setBackupMode(true)
     * before calling this method.
     *
     */
    public void waitUntilBackupAllowed() {
        synchronized (mBackupSharedDelivCoord) {
            while (mBackupSharedDelivCoord.mNumDelivs > 0) {
                try {
                    mBackupSharedDelivCoord.wait(3000);
                } catch (InterruptedException e) {}
            }
        }
    }

    /**
     * Tests whether backup may begin on this mailbox.  Backup may begin when
     * there is no shared delivery in progress.
     * @return
     */
    public boolean backupAllowed() {
        synchronized (mBackupSharedDelivCoord) {
            return mBackupSharedDelivCoord.mNumDelivs < 1;
        }
    }
}
