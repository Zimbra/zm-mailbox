/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Jun 13, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.util.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.map.LRUMap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbSearch.SearchResult;
import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.BrowseResult.DomainItem;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.MailboxManager.MailboxLock;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.TimeZoneFixup;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.pop3.Pop3Message;
import com.zimbra.cs.redolog.op.*;
import com.zimbra.cs.service.FeedManager;
import com.zimbra.cs.session.AllAccountsRedoCommitCallback;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMailbox.Options;

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
    public static final int ID_FOLDER_NOTEBOOK  = 12;
    public static final int ID_FOLDER_AUTO_CONTACTS = 13;
    public static final int ID_FOLDER_IM_LOGS   = 14;
    public static final int ID_FOLDER_TASKS     = 15;
    public static final int ID_FOLDER_BRIEFCASE = 16;

    public static final int HIGHEST_SYSTEM_ID = 16;
    public static final int FIRST_USER_ID     = 256;

    private static final String MD_CONFIG_VERSION = "ver";


    public static final class MailboxData {
        public int     id;
        public int     schemaGroupId;
        public String  accountId;
        public long    size;
        public int     contacts;
        public short   indexVolumeId;
        public int     lastItemId;
        public int     lastChangeId;
        public long    lastChangeDate;
        public int     lastWriteDate;
        public int     recentMessages;
        public int     trackSync;
        public boolean trackImap;
        public Set<String> configKeys;
    }

    private static final class MailboxChange {
        private static final int NO_CHANGE = -1;

        long       timestamp = System.currentTimeMillis();
        int        depth     = 0;
        boolean    active;
        Connection conn      = null;
        RedoableOp recorder  = null;
        Map<MailItem, IndexItemEntry>  indexItems = new HashMap<MailItem, IndexItemEntry>();
        Map<Integer, MailItem> itemCache = null;
        OperationContext octxt = null;
        TargetConstraint tcon  = null;

        Integer  sync     = null;
        Boolean  imap     = null;
        long     size     = NO_CHANGE;
        int      itemId   = NO_CHANGE;
        int      changeId = NO_CHANGE;
        int      contacts = NO_CHANGE;
        int     accessed = NO_CHANGE;
        int     recent  = NO_CHANGE;
        Pair<String,Metadata> config = null;

        PendingModifications mDirty = new PendingModifications();
        List<Object> mOtherDirtyStuff = new LinkedList<Object>();

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
            } else {
                if (ZimbraLog.mailbox.isDebugEnabled())
                    ZimbraLog.mailbox.debug("  increasing stack depth to " + depth + " (" + caller + ')');
        }
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

        RedoableOp getRedoPlayer()   { return (octxt == null ? null : octxt.getPlayer()); }
        RedoableOp getRedoRecorder() { return recorder; }
        
        private static final class IndexItemEntry {
            IndexItemEntry(boolean deleteFirst, Object data) { 
                mDeleteFirst = deleteFirst;
                mData = data;
            }
            boolean mDeleteFirst;
            Object mData;
        }
        
        void addIndexedItem(MailItem item, boolean deleteFirst, Object data)  { 
            indexItems.put(item, new IndexItemEntry(deleteFirst, data)); 
        }

        void reset() {
            if (conn != null)
                DbPool.quietClose(conn);
            active = false;
            conn = null;  octxt = null;  tcon = null;
            depth = 0;
            size = changeId = itemId = contacts = accessed = recent = NO_CHANGE;
            sync = null;  config = null;
            itemCache = null;  indexItems.clear();
            mDirty.clear();  mOtherDirtyStuff.clear();
            if (ZimbraLog.mailbox.isDebugEnabled())
                ZimbraLog.mailbox.debug("clearing change");
        }
    }

    public static class OperationContext {
        public static final boolean CHECK_CREATED = false, CHECK_MODIFIED = true;

        private Account    authuser;
        private boolean    isAdmin;
        private Session    session;
        private RedoableOp player;
        private String     requestIP;
        
        boolean changetype = CHECK_CREATED;
        int     change = -1;

        public OperationContext(RedoableOp redoPlayer) {
            player = redoPlayer;
        }
        public OperationContext(Account acct) {
            this(acct, false);
        }
        public OperationContext(Mailbox mbox) throws ServiceException {
            this(mbox.getAccount());
        }
        public OperationContext(Account acct, boolean admin) {
            authuser = acct;  isAdmin = admin;
        }
        public OperationContext(String accountId) throws ServiceException {
            authuser = Provisioning.getInstance().get(AccountBy.id, accountId);
            if (authuser == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        }
        public OperationContext(AuthToken auth) throws ServiceException {
            String accountId = auth.getAccountId();
            isAdmin = auth.isAdmin() || auth.isDomainAdmin();
            authuser = Provisioning.getInstance().get(AccountBy.id, accountId);
            if (authuser == null && !auth.isZimbraUser()) {
                if (auth.getDigest() != null)
                    authuser = new ACL.GuestAccount(auth);
                else
                    authuser = ACL.ANONYMOUS_ACCT;
            }
            if (authuser == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        }
        public OperationContext(OperationContext octxt) {
            player     = octxt.player;      session = octxt.session;
            authuser   = octxt.authuser;    isAdmin = octxt.isAdmin;
            changetype = octxt.changetype;  change  = octxt.change;
        }

        public OperationContext setChangeConstraint(boolean checkModified, int changeId) {
            changetype = checkModified;  change = changeId;  return this;
        }
        public OperationContext unsetChangeConstraint() {
            changetype = CHECK_CREATED;  change = -1;  return this;
        }

        public OperationContext setSession(Session s) {
            session = s;  return this;
        }
        Session getSession() {
            return session;
        }

        public RedoableOp getPlayer() {
            return player;
        }
        long getTimestamp() {
            return (player == null ? System.currentTimeMillis() : player.getTimestamp());
        }
        int getChangeId() {
            return (player == null ? -1 : player.getChangeId());
        }
        public boolean needRedo() {
            return player == null || !player.getUnloggedReplay();
        }

        public Account getAuthenticatedUser() {
            return authuser;
        }
        public boolean isUsingAdminPrivileges() {
            return isAdmin;
        }
        public boolean isDelegatedRequest(Mailbox mbox) {
            return authuser != null && !authuser.getId().equalsIgnoreCase(mbox.getAccountId());
        }
        
        public OperationContext setRequestIP(String addr) {
            requestIP = addr;  return this;
        }
        public String getRequestIP() {
            return requestIP;
        }
    }

    // TODO: figure out correct caching strategy
    private static final int MAX_ITEM_CACHE_WITH_LISTENERS    = LC.zimbra_mailbox_active_cache.intValue();
    private static final int MAX_ITEM_CACHE_WITHOUT_LISTENERS = LC.zimbra_mailbox_inactive_cache.intValue();
    private static final int MAX_MSGID_CACHE = 10;

    private int           mId;
    private MailboxData   mData;
    private MailboxChange mCurrentChange = new MailboxChange();

    private Map<Integer, Folder> mFolderCache;
    private Map<Object, Tag>     mTagCache;
    private SoftReference<Map<Integer, MailItem>> mItemCache = new SoftReference<Map<Integer, MailItem>>(null);
    private LRUMap       mConvHashes     = new LRUMap(MAX_MSGID_CACHE);
    private LRUMap       mSentMessageIDs = new LRUMap(MAX_MSGID_CACHE);
    private Set<Session> mListeners      = new HashSet<Session>();

    private MailboxLock  mMaintenance = null;
    private MailboxIndex mMailboxIndex = null;
    private IMPersona mPersona = null;
    private MailboxVersion mVersion = null;

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
    /** flag: when a message is a valid Invite */
    public Flag mInviteFlag;
    /** flag: urgent messages */
    public Flag mUrgentFlag;
    /** flag: low-priority messages */
    public Flag mBulkFlag;
    /** flag: items with accessible past revisions */
    public Flag mVersionedFlag;
    /** flag: IMAP-subscribed folders */
    public Flag mSubscribedFlag;
    /** flag: exclude folder from free-busy calculations */
    public Flag mExcludeFBFlag;
    /** flag: folders "checked" for display in the web UI */
    public Flag mCheckedFlag;
    /** flag: whether a folder does not inherit permissions from its parent */
    public Flag mNoInheritFlag;

    /** the full set of message flags, in order */
    final Flag[] mFlags = new Flag[31];

    /** used by checkInitialization() to make sure the init steps happen only once */
    private boolean mInitializationComplete = false;

    /** debug test code -- for batched indexing testing */
    private Set<Integer> mMessagesToIndex = new HashSet<Integer>();
    private static final int sDebugBatchMessageIndexSize = LC.debug_batch_message_indexing.intValue();

    /**
     * Constructor
     * 
     * @param data
     * @throws ServiceException
     */
    Mailbox(MailboxData data) throws ServiceException {
        mId   = data.id;
        mData = data;
        mData.lastChangeDate = System.currentTimeMillis();
        initFlags();
        // version init done in checkInitialization()
        // index init done in checkInitialization()
    }
    
    /**
     * Called by the MailboxManager before returning the mailbox, this
     * function makes sure the Mailbox is fully initialized (index initialized,
     * version check, etc etc).  
     * 
     * Any mailbox-initialization steps that require I/O should be done in this 
     * API and not in the Mailbox constructor since the Mailbox constructor can
     * conceivably be run twice in a race (even though the MailboxMgr makes sure 
     * only one instance of a particular mailbox "wins")
     * 
     * @return TRUE if we did some work (this was the mailbox's first init) or
     *         FALSE if mailbox was already initialized
     * @throws ServiceException
     */
    synchronized boolean finishInitialization() throws ServiceException {
    	if (!mInitializationComplete) {
    		// init the index
            if (!DebugConfig.disableIndexing)
                mMailboxIndex = new MailboxIndex(this, null);
            
            if (mVersion == null) { 
                // if we've just initialized() the mailbox, then the version will
                // be set in the config, but it won't be comitted yet -- and unfortunately 
                // getConfig() won't return us the proper value in this case....so just
                // use the local mVersion value if it is already set (that's the case
                // if we are initializing a new mailbox) and otherwise we'll read the 
                // mailbox version from config
                Metadata md = getConfig(null, MD_CONFIG_VERSION);
                mVersion = MailboxVersion.fromMetadata(md);
            }
            
            // check for mailbox upgrade
    		if (!getVersion().atLeast(1, 2)) {
    			// Version (1.0,1.1)->1.2 Re-Index all contacts 
    			Set<Byte> types = new HashSet<Byte>();
    			types.add(MailItem.TYPE_CONTACT);
    			reIndex(null, types, null, COMPLETED_REINDEX_CONTACTS_V1_2, false); 
    		}
    		
            // same prescription for both the 1.2 -> 1.3 and 1.3 -> 1.4 migrations
            if (!getVersion().atLeast(1, 4)) {
                recalculateFolderAndTagCounts();
                updateVersion(new MailboxVersion((short) 1, (short) 4));
    		}
    		
    		// done!
    		mInitializationComplete = true;
    		return true;
    	}
    	return false;
    }
    

    /** Returns the server-local numeric ID for this mailbox.  To get a
     *  system-wide, persistent unique identifier for the mailbox, use
     *  {@link #getAccountId()}. */
    public int getId() {
        return mId;
    }

    /** Returns the ID of this mailbox's Account.
     * 
     * @see #getAccount() */
    public String getAccountId() {
        return mData.accountId;
    }

    /** Returns which MBOXGROUP<N> database this mailbox is homed in. */
    public int getSchemaGroupId() {
        return mData.schemaGroupId;
    }

    /** Returns the {@link Account} object for this mailbox's owner.  At
     *  present, each account can have at most one <tt>Mailbox</tt>.
     *  
     * @throws AccountServiceException if no account exists */
    public synchronized Account getAccount() throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.id, getAccountId());
        if (acct != null)
            return acct;
        ZimbraLog.mailbox.warn("no account found in directory for mailbox " + mId +
                    " (was expecting " + getAccountId() + ')');
        throw AccountServiceException.NO_SUCH_ACCOUNT(mData.accountId);
    }

    synchronized public IMPersona getPersona() throws ServiceException {
        if (mPersona == null) {
            mPersona = IMPersona.loadPersona(this);
        } 
        return mPersona;
    }

    /** Returns the Mailbox's Lucene index. */
    public MailboxIndex getMailboxIndex() {
        return mMailboxIndex;
    }

    /** Retuns the ID of the volume this <tt>Mailbox</tt>'s index lives on. */
    public short getIndexVolume() {
        return mData.indexVolumeId;
    }

    MailboxLock getMailboxLock() {
        return mMaintenance;
    }

    /** Returns a {@link MailSender} object that can be used to send mail,
     *  save a copy to the Sent folder, etc. */
    public MailSender getMailSender() {
        return new MailSender();
    }


    /** Returns the list of all <code>Mailbox</code> listeners of a given type.
     *  Returns all listeners when the passed-in type is <tt>null</tt>. */
    public synchronized List<Session> getListeners(Session.Type stype) {
        if (mListeners.isEmpty())
            return Collections.emptyList();
        else if (stype == null)
            return new ArrayList<Session>(mListeners);

        List<Session> sessions = new ArrayList<Session>(mListeners.size());
        for (Session s : mListeners)
            if (s.getType() == stype)
                sessions.add(s);
        return sessions;
    }

    boolean hasListeners(Session.Type stype) {
        if (mListeners.isEmpty())
            return false;
        else if (stype == null)
            return true;

        for (Session s : mListeners)
            if (s.getType() == stype)
                return true;
        return false;
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
        
        if (mPersona != null) 
            mPersona.purgeListeners(); // do this BEFORE purgeListeners
        
        Set<Session> purged = new HashSet<Session>(mListeners);
        for (Session session : purged)
            SessionCache.clearSession(session);
        // this may be redundant, as Session.doCleanup should dequeue
        //   the listener, but empty the list here just to be sure
        mListeners.clear();
    }

    /** Posts an IM-related notification to all the Mailbox's sessions. */
    public synchronized void postIMNotification(IMNotification imn) {
        for (Session session : mListeners)
            session.notifyIM(imn);
    }

    /** Returns whether the server is keeping track of message deletes
     *  (etc.) for sync clients.  By default, sync tracking is off.
     * 
     * @see #beginTrackingSync */
    boolean isTrackingSync() {
        return (mCurrentChange.sync == null ? mData.trackSync : mCurrentChange.sync) > 0;
    }

    /** Returns whether the server is keeping track of message moves
     *  for imap clients.  By default, imap tracking is off.
     * 
     * @see #beginTrackingImap */
    public boolean isTrackingImap() {
        return (mCurrentChange.imap == null ? mData.trackImap : mCurrentChange.imap);
    }

    /** Returns the operation timestamp as a UNIX int with 1-second
     *  resolution.  This time is set at the start of the Mailbox
     *  transaction and should match the <tt>long</tt> returned
     *  by {@link #getOperationTimestampMillis}. */
    public int getOperationTimestamp() {
        return (int) (mCurrentChange.timestamp / 1000);
    }

    /** Returns the operation timestamp as a Java long with full
     *  millisecond resolution.  This time is set at the start of
     *  the Mailbox transaction and should match the <tt>int</tt>
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
            DbMailbox.updateMailboxStats(this);
    }

    /** Returns the change sequence number for the current transaction.
     *  If a change number has not yet been assigned to the transaction,
     *  assigns one.<p>
     * 
     *  Every write to the database is assigned a monotonically-increasing
     *  (though not necessarily gap-free) change number.  All writes in
     *  a single transaction receive the same change number.  This change
     *  number is persisted as <tt>MAIL_ITEM.MOD_METADATA</tt> in all
     *  non-delete cases, as <tt>MAIL_ITEM.MOD_CONTENT</tt> for any 
     *  items that were created or had their "content" modified, and as
     *  <tt>TOMBSTONE.SEQUENCE</tt> for hard deletes. */
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
                DbMailbox.updateMailboxStats(this);
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

    RedoableOp getRedoRecorder() {
        return mCurrentChange.recorder;
    }

    PendingModifications getPendingModifications() {
        return mCurrentChange.mDirty;
    }


    /** Returns the {@link Account} for the authenticated user for the
     *  transaction.  Returns <tt>null</tt> if none was supplied in the
     *  transaction's {@link Mailbox.OperationContext} or if the authenticated
     *  user is the same as the <tt>Mailbox</tt>'s owner. */
    Account getAuthenticatedAccount() {
        Account authuser = null;
        if (mCurrentChange.active && mCurrentChange.octxt != null)
            authuser = mCurrentChange.octxt.getAuthenticatedUser();
        // XXX if the current authenticated user is the owner, it will return null.
        // later on in Folder.checkRights(), the same assumption is used to validate
        // the access.
        if (authuser != null && authuser.getId().equals(getAccountId()))
            authuser = null;
        return authuser;
    }

    /** Returns whether the authenticated user for the transaction is using
     *  any admin privileges they might have.  Admin users not using privileges
     *  are exactly like any other user and cannot access any folder they have
     *  not explicitly been granted access to.
     * 
     * @see #getAuthenticatedAccount() */
    boolean isUsingAdminPrivileges() {
        return mCurrentChange.active && mCurrentChange.octxt != null && mCurrentChange.octxt.isUsingAdminPrivileges();
    }

    /** Returns whether the authenticated user has full access to this
     *  <tt>Mailbox</tt>.   The following users have full access:<ul>
     *    <li>the mailbox's owner
     *    <li>all global admin accounts (if using admin privileges)
     *    <li>appropriate domain admins (if using admin privileges)</ul>
     * 
     * @see #getAuthenticatedAccount()
     * @see #isUsingAdminPrivileges() */
    boolean hasFullAccess() throws ServiceException {
        Account authuser = getAuthenticatedAccount();
        // XXX: in Mailbox, authuser is set to null if authuser == owner.
        if (authuser == null || getAccountId().equals(authuser.getId()))
            return true;
        if (mCurrentChange.active && mCurrentChange.octxt != null)
            return AccessManager.getInstance().canAccessAccount(authuser, getAccount(), isUsingAdminPrivileges());
        return false;
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

    /** Returns the last time that the mailbox had a write op caused by a SOAP
     *  session.  This value is written both right after the session's first
     *  write op as well as right after the session expires.
     * 
     * @see #recordLastSoapAccessTime(long) */
    public synchronized long getLastSoapAccessTime() {
        long lastAccess = (mCurrentChange.accessed == MailboxChange.NO_CHANGE ? mData.lastWriteDate : mCurrentChange.accessed) * 1000L;
        for (Session s : mListeners) {
            if (s instanceof SoapSession)
                lastAccess = Math.max(lastAccess, ((SoapSession) s).getLastWriteAccessTime());
        }
        return lastAccess;
    }

    /** Records the last time that the mailbox had a write op caused by a SOAP
     *  session.  This value is written both right after the session's first
     *  write op as well as right after the session expires.
     * 
     * @see #getLastSoapAccessTime() */
    public synchronized void recordLastSoapAccessTime(long time) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("recordLastSoapAccessTime", null);
            if (time > mData.lastWriteDate) {
                mCurrentChange.accessed = (int) (time / 1000);
                DbMailbox.recordLastSoapAccess(this);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns the number of "recent" messages in the mailbox.  A message is
     *  considered "recent" if (a) it's not a draft or a sent message, and
     *  (b) it was added since the last write operation associated with any
     *  SOAP session. */
    public int getRecentMessageCount() {
        return (mCurrentChange.recent == MailboxChange.NO_CHANGE ? mData.recentMessages : mCurrentChange.recent);
    }

    /** Returns the number of contacts currently in the mailbox.
     * 
     * @see #updateContactCount(int) */
    public int getContactCount() {
        return (mCurrentChange.contacts == MailboxChange.NO_CHANGE ? mData.contacts : mCurrentChange.contacts);
    }

    /** Updates the count of contacts currently in the mailbox.  The
     *  administrator can place a limit on a user's contact count by setting
     *  the <tt>zimbraContactMaxNumEntries</tt> COS attribute.  Contacts
     *  in the Trash still count against this quota.
     * 
     * @param delta  The change in contact count, negative to decrease.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>mail.TOO_MANY_CONTACTS</tt> - if the user's contact
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
        if ((item.getFlagBitmask() & Flag.BITMASK_UNCACHED) == 0)
        mCurrentChange.mDirty.recordCreated(item);
    }

    /** Adds the item to the current change's list of items deleted during
     *  the transaction.
     * @param item  The deleted item. */
    void markItemDeleted(MailItem item) {
        if ((item.getFlagBitmask() & Flag.BITMASK_UNCACHED) == 0)
        mCurrentChange.mDirty.recordDeleted(item);
    }

    /** Adds the item id to the current change's list of items deleted during
     *  the transaction.
     * @param itemId  The deleted item's id. */
    void markItemDeleted(byte type, int itemId) {
        mCurrentChange.mDirty.recordDeleted(mData.accountId, itemId, type);
    }

    /** Adds the item ids to the current change's list of items deleted during
     *  the transaction.
     * @param typesMask Mask of all MailItem types listed in itemIds.  see {@link MailItem#typeToBitmask}a
     * @param itemIds  The list of deleted items' ids. */
    void markItemDeleted(int typesMask, List<Integer> itemIds) {
        mCurrentChange.mDirty.recordDeleted(mData.accountId, itemIds, typesMask);
    }

    /** Adds the item to the current change's list of items modified during
     *  the transaction.
     * @param item    The modified item.
     * @param reason  The bitmask describing the modified item properties.
     * @see com.zimbra.cs.session.PendingModifications.Change */
    void markItemModified(MailItem item, int reason) {
        if ((item.getFlagBitmask() & Flag.BITMASK_UNCACHED) == 0)
        mCurrentChange.mDirty.recordModified(item, reason);
    }

    /** Adds the object to the current change's list of non-{@link MailItem}
     *  objects affected during the transaction.  Among these "dirty" items
     *  can be:<ul>
     *    <li>The {@link Blob} or {@link MailboxBlob} for a newly-created file.
     *    <li>The {@link MailItem.PendingDelete} holding blobs and index
     *        entries to be cleaned up after a {@link MailItem#delete}.
     *    <li>The SHA1 hash of a conversation's subject stored in
     *        {@link #mConvHashes}.</ul>
     * 
     * @param obj  The relevant object.
     * @see #commitCache(Mailbox.MailboxChange)
     * @see #rollbackCache(Mailbox.MailboxChange) */
    void markOtherItemDirty(Object obj) {
        mCurrentChange.mOtherDirtyStuff.add(obj);
    }

    /** Adds the MailItem to the current change's list of things that need
     *  to be added to the Lucene index once the current transaction has
     *  committed.
     * 
     * @param item  The MailItem to be indexed.
     * @param deleteFirst True if we need to delete this item from the index before indexing it again
     * @param data  The extra data to be used for the indexing step.
     * @see #commitCache(Mailbox.MailboxChange) */
    void queueForIndexing(MailItem item, boolean deleteFirst,  Object data) {
        if (item.getIndexId() > 0)
        mCurrentChange.addIndexedItem(item, deleteFirst, data);
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
     *  any {@link Session}s listening on this Mailbox and flushes all changes
     *  to the search index of this Mailbox.
     * 
     * @return A new MailboxLock token for use in a subsequent call to
     *         {@link MailboxManager#endMaintenance(Mailbox.MailboxLock, boolean, boolean)}.
     * @throws ServiceException MailServiceException.MAINTENANCE if the
     *         <tt>Mailbox</tt> is already in maintenance mode. */
    synchronized MailboxLock beginMaintenance() throws ServiceException {
        if (mMaintenance != null)
            throw MailServiceException.MAINTENANCE(mId);
        ZimbraLog.mailbox.info("Locking mailbox %d for maintenance.", getId());
        mMaintenance = new MailboxLock(mData.accountId, mId, this);

        purgeListeners();
        if (mMailboxIndex != null)
            mMailboxIndex.flush();
        return mMaintenance;
    }

    synchronized void endMaintenance(boolean success) throws ServiceException {
        if (mMaintenance == null)
            throw ServiceException.FAILURE("mainbox not in maintenance mode", null);

        if (success) {
            ZimbraLog.mailbox.info("Ending maintenance on mailbox %d.", getId());
            mMaintenance = null;
        } else {
            ZimbraLog.mailbox.info("Ending maintenance and marking mailbox %d as unavailable.", getId());
            mMaintenance.markUnavailable();
    }
    }


    void beginTransaction(String caller, OperationContext octxt) throws ServiceException {
        beginTransaction(caller, System.currentTimeMillis(), octxt, null, null);
    }
    private void beginTransaction(String caller, OperationContext octxt, RedoableOp recorder) throws ServiceException {
        long timestamp = octxt == null ? System.currentTimeMillis() : octxt.getTimestamp();
        beginTransaction(caller, timestamp, octxt, recorder, null);
    }
    void beginTransaction(String caller, OperationContext octxt, RedoableOp recorder, Connection conn) throws ServiceException {
        long timestamp = octxt == null ? System.currentTimeMillis() : octxt.getTimestamp();
        beginTransaction(caller, timestamp, octxt, recorder, conn);
    }
    private void beginTransaction(String caller, long time, OperationContext octxt, RedoableOp recorder, Connection conn) throws ServiceException {
        mCurrentChange.startChange(caller, octxt, recorder);

        // if a Connection object was provided, use it
        if (conn != null)
            setOperationConnection(conn);

        boolean needRedo = octxt == null || octxt.needRedo();
        // have a single, consistent timestamp for anything affected by this operation
        mCurrentChange.setTimestamp(time);
        if (recorder != null && needRedo)
            recorder.start(time);

        // if the caller has specified a constraint on the range of affected items, store it
        if (recorder != null && needRedo && octxt != null && octxt.change > 0)
            recorder.setChangeConstraint(octxt.changetype, octxt.change);

        // if we're redoing an op, preserve the old change ID
        if (octxt != null && octxt.getChangeId() >= 0)
            setOperationChangeID(octxt.getChangeId());
        if (recorder != null && needRedo)
            recorder.setChangeId(getOperationChangeID());

        // keep a hard reference to the item cache to avoid having it GCed during the op 
        Map<Integer, MailItem> cache = mItemCache.get();
        if (cache == null) {
            cache = new LinkedHashMap<Integer, MailItem>(MAX_ITEM_CACHE_WITH_LISTENERS, (float) 0.75, true);
            mItemCache = new SoftReference<Map<Integer, MailItem>>(cache);
            ZimbraLog.cache.debug("created a new MailItem cache for mailbox " + getId());
        }
        mCurrentChange.itemCache = cache;

        // don't permit mailbox access during maintenance
        if (mMaintenance != null && !mMaintenance.canAccess())
            throw MailServiceException.MAINTENANCE(mId);

        // we can only start a redoable operation as the transaction's base change
        if (recorder != null && needRedo && mCurrentChange.depth > 1)
            throw ServiceException.FAILURE("cannot start a logged transaction from within another transaction", null);

        // we'll need folders and tags loaded in order to handle ACLs
        loadFoldersAndTags();
    }


    /** Returns the set of configuration info for the given section.
     *  We segment the mailbox-level configuration data into "sections" to
     *  allow server applications to store their config separate from all
     *  other apps.  (So the IMAP server could store and retrieve the
     *  <tt>"imap"</tt> config section, etc.)
     * 
     * @param octxt    The context for this request (e.g. auth user id).
     * @param section  The config section to fetch.
     * @perms full access to the mailbox (see {@link #hasFullAccess()})
     * @return The {@link Metadata} representing the appropriate section's
     *         configuration information, or <tt>null</tt> if none is
     *         found or if the caller does not have sufficient privileges
     *         to read the mailbox's config. */
    public synchronized Metadata getConfig(OperationContext octxt, String section) throws ServiceException {
        if (section == null || section.equals(""))
            return null;

        // note: defaulting to true, not false...
        boolean success = true;
        try {
            beginTransaction("getConfig", octxt, null);

            // make sure they have sufficient rights to view the config
            if (!hasFullAccess())
                return null;
            if (mData.configKeys == null || !mData.configKeys.contains(section))
                return null;

            String config = DbMailbox.getConfig(this, section);
            if (config == null)
                return null;
            try {
                return new Metadata(config);
            } catch (ServiceException e) {
                success = false;
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
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have
     *        sufficient permissions</ul>
     * @see #getConfig(OperationContext, String) */
    public synchronized void setConfig(OperationContext octxt, String section, Metadata config) throws ServiceException {
        if (section == null)
            throw new IllegalArgumentException();

        SetConfig redoPlayer = new SetConfig(mId, section, config);
        boolean success = false;
        try {
            beginTransaction("setConfig", octxt, redoPlayer);

            // make sure they have sufficient rights to view the config
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            mCurrentChange.mDirty.recordModified(this, Change.MODIFIED_CONFIG);
            mCurrentChange.config = new Pair<String,Metadata>(section, config);
            DbMailbox.updateConfig(this, section, config);
            success = true;
        } finally {
            endTransaction(success);
        }
    }


    private Map<Integer, MailItem> getItemCache() throws ServiceException {
        if (!mCurrentChange.isActive())
            throw ServiceException.FAILURE("cannot access item cache outside a transaction", null);
        return mCurrentChange.itemCache;
    }

    private void clearItemCache() {
        if (mCurrentChange.isActive())
            mCurrentChange.itemCache.clear();
        else
            mItemCache.clear();
    }

    void cache(MailItem item) throws ServiceException {
        if (item == null || (item.getFlagBitmask() & Flag.BITMASK_UNCACHED) != 0)
            return;

        if (item instanceof Tag) {
            if (item instanceof Flag)
                mFlags[((Flag) item).getIndex()] = (Flag) item;
            if (mTagCache != null) {
                Tag tag = (Tag) item;
                mTagCache.put(tag.getId(), tag);
                mTagCache.put(tag.getName().toLowerCase(), tag);
            }
        } else if (item instanceof Folder) {
            if (mFolderCache != null)
                mFolderCache.put(item.getId(), (Folder) item);
        } else {
            getItemCache().put(item.getId(), item);
        }

        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("cached " + MailItem.getNameForType(item) + " " + item.getId() + " in mailbox " + getId());
    }

    void uncache(MailItem item) throws ServiceException {
        if (item == null)
            return;

        if (item instanceof Tag) {
            if (mTagCache == null)
                return;
            mTagCache.remove(item.getId());
            mTagCache.remove(((Tag) item).getName().toLowerCase());
        } else if (item instanceof Folder) {
            if (mFolderCache == null)
                return;
            mFolderCache.remove(item.getId());
        } else {
            getItemCache().remove(item.getId());
        }

        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("uncached " + MailItem.getNameForType(item) + " " + item.getId() + " in mailbox " + getId());

        item.uncacheChildren();
    }

    /** Removes an item from the <tt>Mailbox</tt>'s item cache.  If the
     *  item has any children, they are also uncached.  <i>Note: This function
     *  cannot be used to uncache {@link Tag}s and {@link Folder}s.  You must
     *  call {@link #uncache(MailItem)} to remove those items from their
     *  respective caches.</i>
     * 
     * @param itemId  The id of the item to uncache */
    void uncacheItem(Integer itemId) throws ServiceException {
        MailItem item = getItemCache().remove(itemId);
        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("uncached item " + itemId + " in mailbox " + getId());
        if (item != null)
            item.uncacheChildren();
    }

    /** Removes all items of a specified type from the <tt>Mailbox</tt>'s
     *  caches.  There may be some collateral damage: purging non-tag,
     *  non-folder types will drop the entire item cache.
     * 
     * @param type  The type of item to completely uncache. */
    void purge(byte type) {
        switch (type) {
            case MailItem.TYPE_FOLDER:
            case MailItem.TYPE_MOUNTPOINT:
            case MailItem.TYPE_SEARCHFOLDER:  mFolderCache = null;  break;
            case MailItem.TYPE_FLAG:
            case MailItem.TYPE_TAG:           mTagCache = null;     break;
            default:                          clearItemCache();     break;
            case MailItem.TYPE_UNKNOWN:       mFolderCache = null;  mTagCache = null;  clearItemCache();  break;
        }

        if (ZimbraLog.cache.isDebugEnabled())
            ZimbraLog.cache.debug("purged " + MailItem.getNameForType(type) + " cache in mailbox " + getId());
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
     *  This method does <u>not</u> have hooks for inserting arbitrary folders,
     *  tags, or messages into a new mailbox.
     * 
     * @see Folder#create(int, Mailbox, Folder, String, byte, byte, int, byte, String) */
    synchronized void initialize() throws ServiceException {
        // the new mailbox's caches are created and the default set of tags are
        // loaded by the earlier call to loadFoldersAndTags in beginTransaction

        byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_DONT_TRACK_COUNTS;
        Folder root = Folder.create(ID_FOLDER_ROOT, this, null, "ROOT",     hidden, MailItem.TYPE_UNKNOWN,      0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_TAGS,          this, root, "Tags",          hidden, MailItem.TYPE_TAG,          0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_CONVERSATIONS, this, root, "Conversations", hidden, MailItem.TYPE_CONVERSATION, 0, MailItem.DEFAULT_COLOR, null);

        byte system = Folder.FOLDER_IS_IMMUTABLE;
        Folder userRoot = Folder.create(ID_FOLDER_USER_ROOT, this, root, "USER_ROOT", system, MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_INBOX,    this, userRoot, "Inbox",    system, MailItem.TYPE_MESSAGE, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_TRASH,    this, userRoot, "Trash",    system, MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_SPAM,     this, userRoot, "Junk",     system, MailItem.TYPE_MESSAGE, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_SENT,     this, userRoot, "Sent",     system, MailItem.TYPE_MESSAGE, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_DRAFTS,   this, userRoot, "Drafts",   system, MailItem.TYPE_MESSAGE, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_CONTACTS, this, userRoot, "Contacts", system, MailItem.TYPE_CONTACT, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_NOTEBOOK, this, userRoot, "Notebook", system, MailItem.TYPE_WIKI,    0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_CALENDAR, this, userRoot, "Calendar", system, MailItem.TYPE_APPOINTMENT, Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_TASKS,    this, userRoot, "Tasks",    system, MailItem.TYPE_TASK,        Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_AUTO_CONTACTS, this, userRoot, "Emailed Contacts", system, MailItem.TYPE_CONTACT, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_IM_LOGS,  this, userRoot, "Chats",    system, MailItem.TYPE_MESSAGE, 0, MailItem.DEFAULT_COLOR, null);
        Folder.create(ID_FOLDER_BRIEFCASE, this, userRoot, "Briefcase", system, MailItem.TYPE_DOCUMENT, 0, MailItem.DEFAULT_COLOR, null);
        

        mCurrentChange.itemId = getInitialItemId();
        DbMailbox.updateMailboxStats(this);
        
        // set the version to CURRENT
        Metadata md = new Metadata();
        mVersion = new MailboxVersion(MailboxVersion.CURRENT());
        mVersion.writeToMetadata(md);
        DbMailbox.updateConfig(this, MD_CONFIG_VERSION, md);
    }

    int getInitialItemId() { return FIRST_USER_ID; }

    private void initFlags() throws ServiceException {
        // flags will be added to mFlags array via call to cache() in MailItem constructor
        mSentFlag      = Flag.instantiate(this, "\\Sent",       Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_FROM_ME);
        mAttachFlag    = Flag.instantiate(this, "\\Attached",   Flag.FLAG_GENERIC,         Flag.ID_FLAG_ATTACHED);
        mReplyFlag     = Flag.instantiate(this, "\\Answered",   Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_REPLIED);
        mForwardFlag   = Flag.instantiate(this, "\\Forwarded",  Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_FORWARDED);
        mCopiedFlag    = Flag.instantiate(this, "\\Copied",     Flag.FLAG_GENERIC,         Flag.ID_FLAG_COPIED);
        mFlaggedFlag   = Flag.instantiate(this, "\\Flagged",    Flag.FLAG_GENERIC,         Flag.ID_FLAG_FLAGGED);
        mDraftFlag     = Flag.instantiate(this, "\\Draft",      Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_DRAFT);
        mDeletedFlag   = Flag.instantiate(this, "\\Deleted",    Flag.FLAG_GENERIC,         Flag.ID_FLAG_DELETED);
        mNotifiedFlag  = Flag.instantiate(this, "\\Notified",   Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_NOTIFIED);
        mUnreadFlag    = Flag.instantiate(this, "\\Unread",     Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_UNREAD);
        mInviteFlag     = Flag.instantiate(this, "\\Invite",     Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_INVITE);
        mUrgentFlag     = Flag.instantiate(this, "\\Urgent",     Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_HIGH_PRIORITY);
        mBulkFlag       = Flag.instantiate(this, "\\Bulk",       Flag.FLAG_IS_MESSAGE_ONLY, Flag.ID_FLAG_LOW_PRIORITY);
        mVersionedFlag  = Flag.instantiate(this, "\\Versioned",  Flag.FLAG_GENERIC,         Flag.ID_FLAG_VERSIONED);

        mSubscribedFlag = Flag.instantiate(this, "\\Subscribed", Flag.FLAG_IS_FOLDER_ONLY,  Flag.ID_FLAG_SUBSCRIBED);
        mExcludeFBFlag = Flag.instantiate(this, "\\ExcludeFB",  Flag.FLAG_IS_FOLDER_ONLY,  Flag.ID_FLAG_EXCLUDE_FREEBUSY);
        mCheckedFlag   = Flag.instantiate(this, "\\Checked",    Flag.FLAG_IS_FOLDER_ONLY,  Flag.ID_FLAG_CHECKED);
        mNoInheritFlag  = Flag.instantiate(this, "\\NoInherit",  Flag.FLAG_IS_FOLDER_ONLY,  Flag.ID_FLAG_NO_INHERIT);
    }

    private void loadFoldersAndTags() throws ServiceException {
        // if the persisted mailbox sizes aren't available, we *must* recalculate
        boolean initial = mData.contacts < 0 || mData.size < 0;

        if (mFolderCache != null && mTagCache != null && !initial)
            return;
        ZimbraLog.cache.info("Initializing folder and tag caches for mailbox " + getId());

        try {
            Map<MailItem.UnderlyingData, Long> folderData = new HashMap<MailItem.UnderlyingData, Long>();
            Map<MailItem.UnderlyingData, Long> tagData    = new HashMap<MailItem.UnderlyingData, Long>();
            MailboxData stats = DbMailItem.getFoldersAndTags(this, folderData, tagData, initial);

            boolean persist = stats != null;
            if (stats != null) {
                if (mData.size != stats.size) {
                    mCurrentChange.mDirty.recordModified(this, Change.MODIFIED_SIZE);
                    ZimbraLog.mailbox.debug("setting mailbox size to " + stats.size + " (was " + mData.size + ") for mailbox " + mId);
                    mData.size = stats.size;
                }
                if (mData.contacts != stats.contacts) {
                    ZimbraLog.mailbox.debug("setting contact count to " + stats.contacts + " (was " + mData.contacts + ") for mailbox " + mId);
                    mData.contacts = stats.contacts;
                }
                DbMailbox.updateMailboxStats(this);
            }

                mFolderCache = new HashMap<Integer, Folder>();
                // create the folder objects and, as a side-effect, populate the new cache
            for (Map.Entry<MailItem.UnderlyingData, Long> entry : folderData.entrySet()) {
                Folder folder = (Folder) MailItem.constructItem(this, entry.getKey());
                if (entry.getValue() > 0)
                    folder.setSize(folder.getItemCount(), entry.getValue());
            }
                // establish the folder hierarchy
                for (Folder folder : mFolderCache.values()) {
                    Folder parent = mFolderCache.get(folder.getParentId());
                    // FIXME: side effect of this is that parent is marked as dirty...
                    if (parent != null)
                        parent.addChild(folder);
                    if (persist)
                        folder.saveFolderCounts(initial);
                }

                mTagCache = new HashMap<Object, Tag>();
                // create the tag objects and, as a side-effect, populate the new cache
            for (MailItem.UnderlyingData ud : tagData.keySet()) {
                    Tag tag = new Tag(this, ud);
                    if (persist)
                        tag.saveTagCounts();
                }
                // flags don't change and thus can be reused in the new cache
                for (int i = 0; i < mFlags.length; i++) {
                    if (mFlags[i] == null)
                        continue;
                    ZimbraLog.mailbox.debug(i + ": " + mFlags[i]);
                    cache(mFlags[i]);
                }
        } catch (ServiceException e) {
            mTagCache = null;
            mFolderCache = null;
            throw e;
        }
    }

    synchronized void recalculateFolderAndTagCounts() throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("recalculateFolderAndTagCounts", null);

            // force the recalculation of all folder/tag/mailbox counts and sizes
            mTagCache = null;
            mFolderCache = null;
            mData.contacts = -1;
            loadFoldersAndTags();

            success = true;
        } finally {
            endTransaction(success);
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
            lock = MailboxManager.getInstance().beginMaintenance(mData.accountId, mId);
        } catch (MailServiceException e) {
            // Ignore wrong mailbox exception.  It may be thrown if we're
            // redoing a DeleteMailbox that was interrupted when server
            // crashed in the middle of the operation.  Database says the
            // mailbox has been deleted, but there may be other files that
            // still need to be cleaned up.
            if (!MailServiceException.WRONG_MAILBOX.equals(e.getCode()))
                throw e;
        }

        boolean needRedo = octxt == null || octxt.needRedo();
        DeleteMailbox redoRecorder = new DeleteMailbox(mId);
        boolean success = false;
        try {
            beginTransaction("deleteMailbox", octxt, redoRecorder);
            if (needRedo)
                redoRecorder.log();

            try {
                // remove all the relevant entries from the database
                Connection conn = getOperationConnection();
                    DbMailbox.clearMailboxContent(this);
                    DbMailbox.deleteMailbox(conn, this);

                success = true;
            } finally {
                // commit the DB transaction before touching the store!  (also ends the operation)
                endTransaction(success);
            }

            if (success) {
                // remove all traces of the mailbox from the Mailbox cache
                //   (so anyone asking for the Mailbox gets NO_SUCH_MBOX or creates a fresh new empty one with a different id)
                MailboxManager.getInstance().markMailboxDeleted(this);

                // attempt to nuke the store and index
                // FIXME: we're assuming a lot about the store and index here; should use their functions
                try {
                    if (mMailboxIndex != null)
                        mMailboxIndex.deleteIndex();
                } catch (IOException iox) { }
                try {
                    StoreManager sm = StoreManager.getInstance();
                    for (Volume vol : Volume.getAll())
                        sm.deleteStore(this, vol.getId());
                } catch (IOException iox) { }

                // twiddle the mailbox lock [must be the last command of this function!]
                //   (so even *we* can't access this Mailbox going forward)
                if (lock != null)
                    lock.markUnavailable();
            }
        } finally {
            if (needRedo) {
                if (success)
                    redoRecorder.commit();
                else
                    redoRecorder.abort();
            }
        }
    }


    public synchronized MailboxVersion getVersion() { return mVersion; }
    
    synchronized void updateVersion(MailboxVersion vers) throws ServiceException {
        mVersion = new MailboxVersion(vers);
        Metadata md = getConfig(null, Mailbox.MD_CONFIG_VERSION);
        
        if (md == null)
            md = new Metadata();
        
        mVersion.writeToMetadata(md);
        setConfig(null, Mailbox.MD_CONFIG_VERSION, md);
    }
    
    
    /**
     * Status of current reindexing operation for this mailbox, or NULL 
     * if a re-index is not in progress
     */
    public static class ReIndexStatus {
        public int mNumProcessed = 0;
        public int mNumToProcess = 0;
        public int mNumFailed = 0;
        public boolean mCancel = false;

        public String toString() {
            String status = "Completed " + mNumProcessed + " out of " + mNumToProcess + " (" + mNumFailed + " failures)";
            return (mCancel ? "--CANCELLING--  " : "") + status;
        }

        public Object clone() {
            ReIndexStatus toRet = new ReIndexStatus();
            toRet.mNumProcessed = mNumProcessed;
            toRet.mNumToProcess = mNumToProcess;
            toRet.mNumFailed = mNumFailed;
            return toRet;
        }
    }
    
    /** Status of current reindexing operation for this mailbox, or NULL 
     *  if a re-index is not in progress. */
    private ReIndexStatus mReIndexStatus = null;

    public synchronized boolean isReIndexInProgress() {
        return mReIndexStatus != null;
    }

    public synchronized ReIndexStatus getReIndexStatus() {
        return mReIndexStatus;
    }
    
    private static final int COMPLETED_REINDEX_CONTACTS_V1_2 = 100;
    
    /**
     * Some long-running transactions (e.g. ReIndexing) might not complete while convienently in the stack
     * of a particular caller: e.g. if the server goes down while a re-indexing is in progress, it is restarted
     * when the server comes back up, and then completes sometime later.
     * 
     * This function is a general purpose completion routine for handler code.
     * 
     * This function should *NOT* throw ServiceException, since by definition there is nothing on the callstack
     * that can properly handle the exception.  Error handling should happen in this function.
     * 
     * @param completionId
     */
    synchronized void completion(int completionId) {
        switch(completionId) {
            case COMPLETED_REINDEX_CONTACTS_V1_2:
                // check current version, just in case someone updated the version while
                // we were gone
                if (!getVersion().atLeast(1,2)) {
                    try {
                        updateVersion(new MailboxVersion((short)1,(short)2));
                    } catch (ServiceException e) {
                        ZimbraLog.mailbox.warn("Could not update version in Mailbox v1.2 schema upgrade.", e);
                    }
                }
                break;
        }
    }

    /**
     * Re-Index all items in this mailbox.  This can be a *very* expensive operation (upwards of an hour to run
     * on a large mailbox on slow hardware).  We are careful to unlock the mailbox periodically so that the
     * mailbox can still be accessed while the reindex is running, albeit at a slower rate.
     * 
     * @param typesOrNull  If NULL then all items are re-indexed, otherwise only the specified types are reindexed.
     * @param itemIdsOrNull List of ItemIDs to reindex.  If this is specified, typesOrNull MUST be null
     * @param completionId Since this is a long-running operation (and it might conceivably be interrupted and then
     *                              run after a server restart) the caller can pass a completionId to this function.  When the
     *                              re-indexing has completed, the Mailbox's Completion() function is called with the passed-in
     *                              Integer.  A value of '0' means "don't run a completion function".
     * @throws ServiceException
     */
    public void reIndex(OperationContext octxt, Set<Byte> typesOrNull, Set<Integer> itemIdsOrNull, int completionId, boolean skipDelete) throws ServiceException {
        ReindexMailbox redoRecorder = new ReindexMailbox(mId, typesOrNull, itemIdsOrNull, completionId, skipDelete);
        
        if (typesOrNull != null && itemIdsOrNull != null)
            throw ServiceException.INVALID_REQUEST("Must only specify one of Types, ItemIds to Mailbox.reIndex", null);
            
        boolean needRedo = octxt == null || octxt.needRedo();

        Collection<SearchResult> msgs = null;
        boolean redoInitted = false;
        boolean indexDeleted = false;

        try {
            //
            // First step, with the mailbox locked: 
            //     -- get a list of all messages in the mailbox
            //     -- delete the index
            //
            synchronized(this) {
                if (isReIndexInProgress())
                    throw ServiceException.ALREADY_IN_PROGRESS(Integer.toString(mId), mReIndexStatus.toString());
                
                boolean success = false;
                try {
                    // Don't pass redoRecorder to beginTransaction.  We have already
                    // manually called log() on redoRecorder because this is a long-
                    // running transaction, and we don't want endTransaction to log it
                    // again, resulting in two entries for the same operation in redolog.
                    beginTransaction("reIndex", octxt, null);
                    if (needRedo) {
                        redoRecorder.start(getOperationTimestampMillis());
                        redoRecorder.log();
                        redoInitted = true;
                    }

                    DbSearchConstraints c = new DbSearchConstraints();
                    c.mailbox = this;
                    c.sort = DbSearch.SORT_BY_DATE;
                    if (itemIdsOrNull != null)
                        c.itemIds = itemIdsOrNull; 
                    else if (typesOrNull != null)
                        c.types = typesOrNull;

                    msgs = new ArrayList<SearchResult>();
                    DbSearch.search(msgs, getOperationConnection(), c, SearchResult.ExtraData.NONE);
                    
                    if (!skipDelete) {
                    if (itemIdsOrNull != null || typesOrNull != null) {
                        // NOT reindexing everything: delete manually
                        int[] itemIds = new int[msgs.size()];
                        int i = 0;
                        for (SearchResult s : msgs)
                            itemIds[i++] = s.indexId;
                        
                        if (mMailboxIndex != null)
                            mMailboxIndex.deleteDocuments(itemIds);
                        indexDeleted = true;
                    } else {
                        // reindexing everything, just delete the index
                        if (mMailboxIndex != null)
                            mMailboxIndex.deleteIndex();
                        indexDeleted = true;
                    }
                    }

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
            for (Iterator<SearchResult> iter = msgs.iterator(); iter.hasNext();) {
                if (ZimbraLog.mailbox.isDebugEnabled() && ((mReIndexStatus.mNumProcessed % 2000) == 0)) {
                    ZimbraLog.mailbox.debug("Re-Indexing: Mailbox "+getId()+" on item "+mReIndexStatus.mNumProcessed+" out of "+msgs.size());
                }
                
                synchronized(this) {
                    if (mReIndexStatus.mCancel) {
                        ZimbraLog.mailbox.warn("CANCELLING re-index of Mailbox "+getId()+" before it is complete.  ("+mReIndexStatus.mNumProcessed+" processed out of "+msgs.size()+")");                            
                        throw ServiceException.INTERRUPTED("ReIndexing Canceled");
                    }
                    mReIndexStatus.mNumProcessed++;
                    SearchResult sr = iter.next();
                    try {
                        MailItem item = getItemById(null, sr.id, sr.type);
                        item.reindex(null, false /* already deleted above */, null);
                    } catch(ServiceException  e) {
                        mReIndexStatus.mNumFailed++;
                        ZimbraLog.mailbox.info("Re-Indexing: Mailbox " +getId()+ " had error on item "+sr.id+".  Item will not be indexed.", e);
                    } catch(java.lang.RuntimeException e) {
                        mReIndexStatus.mNumFailed++;
                        ZimbraLog.mailbox.info("Re-Indexing: Mailbox " +getId()+ " had error on item "+sr.id+".  Item will not be indexed.", e);
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
                        +" items in " + (end-start) + "ms.  (avg "+avg+"ms/item= "+mps+" items/sec)"
                        +" ("+mReIndexStatus.mNumFailed+" failed) ");
            
            if (completionId > 0)
                completion(completionId);
            
        } finally {
            mReIndexStatus = null;

            if (mMailboxIndex != null)
                mMailboxIndex.flush();
            if (redoInitted) {
                if (indexDeleted) {
                    // there's no meaningful way to roll this transaction back once data is deleted.
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

    /** Recalculates the size, metadata, etc. for an existing MailItem and
     *  persists that information to the database.  Maintains any existing
     *  mutable metadata.  Updates mailbox and folder sizes appropriately.
     * 
     * @param id    The item ID of the MailItem to reanalyze.
     * @param type  The item's type (e.g. {@link MailItem#TYPE_MESSAGE}).
     * @param data  The (optional) extra item data for indexing (e.g.
     *              a Message's {@link ParsedMessage}. */
    public synchronized void reanalyze(int id, byte type, Object data) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("reanalyze", null);
            MailItem item = getItemById(null, id, type);
            item.reanalyze(data);
            success = true;
        } finally {
            endTransaction(success);
        }
    }


    /** Returns the access rights that the user has been granted on this
     *  item.  The owner of the {@link Mailbox} has all rights on all items
     *  in the Mailbox, as do all admin accounts.  All other users must be
     *  explicitly granted access.  <i>(Tag sharing and negative rights not
     *  yet implemented.)</i>  This operation will succeed even if the
     *  authenticated user from the {@link Mailbox.OperationContext} does
     *  not have {@link ACL#RIGHT_READ} on the requested item.<p>
     * 
     *  If you want to know if an account has {@link ACL#RIGHT_WRITE} on an
     *  item, call<pre>
     *    (mbox.getEffectivePermissions(new OperationContext(acct), itemId) &
     *         ACL.RIGHT_WRITE) != 0</pre>
     * 
     * @param octxt    The context (authenticated user, redo player, other
     *                 constraints) under which this operation is executed.
     * @param itemId   The item whose permissions we need to query.
     * @param type     The item's type, or {@link MailItem#TYPE_UNKNOWN}.
     * @return An OR'ed-together set of rights, e.g. {@link ACL#RIGHT_READ}
     *         and {@link ACL#RIGHT_INSERT}.
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.NO_SUCH_ITEM</tt> - the specified item does not
     *        exist
     *    <li><tt>service.FAILURE</tt> - if there's a database failure,
     *        LDAP error, or other internal error</ul>
     * @see ACL
     * @see MailItem#checkRights(short, Account, boolean) */
    public synchronized short getEffectivePermissions(OperationContext octxt, int itemId, byte type) throws ServiceException {
        // fetch the item outside the transaction so we get it even if the
        //   authenticated user doesn't have read permissions on it
        MailItem item = getItemById(null, itemId, type);

        boolean success = false;
        try {
            beginTransaction("getEffectivePermissions", octxt);
            // use ~0 to query *all* rights; may need to change this when we do negative rights
            short rights = item.checkRights((short) ~0, getAuthenticatedAccount(), isUsingAdminPrivileges());
            success = true;
            return rights;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns whether this type of {@link MailItem} is definitely preloaded
     *  in one of the <tt>Mailbox</tt>'s caches.
     * 
     * @param type  The type of <tt>MailItem</tt>.
     * @return <tt>true</tt> if the item is a {@link Folder} or {@link Tag}
     *         or one of their subclasses.
     * @see #mTagCache
     * @see #mFolderCache */
    public static boolean isCachedType(byte type) {
        return type == MailItem.TYPE_FOLDER || type == MailItem.TYPE_SEARCHFOLDER ||
               type == MailItem.TYPE_TAG    || type == MailItem.TYPE_FLAG ||
               type == MailItem.TYPE_MOUNTPOINT;
    }

    private <T extends MailItem> T checkAccess(T item) throws ServiceException {
        if (item == null || item.canAccess(ACL.RIGHT_READ))
            return item;
        throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
    }

    /**
     * Returns the <tt>MailItem</tt> with the specified id. 
     * @throws NoSuchItemException if the item does not exist
     */
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

    /**
     * Returns <tt>MailItem</tt>s with the specified ids. 
     * @throws NoSuchItemException any item does not exist
     */
    public synchronized MailItem[] getItemById(OperationContext octxt, Collection<Integer> ids, byte type) throws ServiceException {
        return getItemById(octxt, ArrayUtil.toIntArray(ids), type);
    }

    /**
     * Returns <tt>MailItem</tt>s with the specified ids. 
     * @throws NoSuchItemException any item does not exist
     */
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

    MailItem[] getItemById(Collection<Integer> ids, byte type) throws ServiceException {
        return getItemById(ArrayUtil.toIntArray(ids), type);
    }

    MailItem[] getItemById(int[] ids, byte type) throws ServiceException {
        if (!mCurrentChange.active)
            throw ServiceException.FAILURE("must be in transaction", null);
        if (ids == null)
            return null;

        MailItem items[] = new MailItem[ids.length];
        Set<Integer> uncached = new HashSet<Integer>();

        // try the cache first
        Integer miss = null;
        boolean relaxType = false;
        for (int i = 0; i < ids.length; i++) {
            // special-case -1 as a signal to return null...
            if (ids[i] == ID_AUTO_INCREMENT) {
                items[i] = null;
            } else {
                Integer key = ids[i];
                MailItem item = getCachedItem(key, type);
                // special-case virtual conversations
                if (item == null && ids[i] <= -FIRST_USER_ID) {
                    if (!MailItem.isAcceptableType(type, MailItem.TYPE_CONVERSATION))
                        throw MailItem.noSuchItem(ids[i], type);
                    Message msg = getCachedMessage(key = -ids[i]);
                    if (msg != null) {
                        if (msg.getConversationId() == ids[i])
                            item = new VirtualConversation(this, msg);
                    } else {
                        relaxType = true;
                    }
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
                    MailItem item = getCachedItem(-ids[i]);
                    if (!(item instanceof Message))
                        throw MailItem.noSuchItem(ids[i], type);
                    else if (item.getParentId() == ids[i])
                        items[i] = new VirtualConversation(this, (Message) item);
                    else {
                        items[i] = getCachedItem(item.getParentId());
                        if (items[i] == null)
                            uncached.add(item.getParentId());
                    }
                } else
                    if ((items[i] = getCachedItem(ids[i])) == null)
                        throw MailItem.noSuchItem(ids[i], type);
            }

        // special case asking for VirtualConversation but having it be a real Conversation
        if (!uncached.isEmpty()) {
            MailItem.getById(this, uncached, MailItem.TYPE_CONVERSATION);
            for (int i = 0; i < ids.length; i++)
                if (ids[i] <= -FIRST_USER_ID && items[i] == null) {
                    MailItem item = getCachedItem(-ids[i]);
                    if (!(item instanceof Message) || item.getParentId() == ids[i])
                        throw ServiceException.FAILURE("item should be cached but is not: " + -ids[i], null);
                    items[i] = getCachedItem(item.getParentId());
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
            item = mTagCache.get(key);
        if (item == null && mFolderCache != null)
            item = mFolderCache.get(key);
        if (item == null)
            item = getItemCache().get(key);

        logCacheActivity(key, item == null ? MailItem.TYPE_UNKNOWN : item.getType(), item);
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
                    item = mTagCache.get(key);
                break;
            case MailItem.TYPE_MOUNTPOINT:
            case MailItem.TYPE_SEARCHFOLDER:
            case MailItem.TYPE_FOLDER:
                if (mFolderCache != null)
                    item = mFolderCache.get(key);
                break;
            default:
                item = getItemCache().get(key);
            break;
        }

        if (item != null && !MailItem.isAcceptableType(type, item.mData.type))
            item = null;

        logCacheActivity(key, type, item);
        return item;
    }

    public synchronized MailItem getItemFromUnderlyingData(MailItem.UnderlyingData data) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getItemFromUd", null);
//            data.flags |= Flag.BITMASK_UNCACHED;
            MailItem item = getItem(data);
            success = true;
            return item;
        } finally {
            endTransaction(success);
//            data.flags &= ~Flag.BITMASK_UNCACHED;
        }
    }

    /** translate from the DB representation of an item to its Mailbox abstraction */
    MailItem getItem(MailItem.UnderlyingData data) throws ServiceException {
        if (data == null)
            return null;
        MailItem item = getCachedItem(data.id, data.type);
        // XXX: should we sanity-check the cached version to make sure all the data matches?
        if (item != null)
            return item;
        return MailItem.constructItem(this, data);
    }

    /** Returns a current or past revision of an item.  Item version numbers
     *  are 1-based and incremented each time the "content" of the item changes
     *  (e.g. editing a draft, modifying a contact's fields).  If the requested
     *  revision does not exist, either because the version number is out of
     *  range or because the requested revision has not been retained, returns
     *  <tt>null</tt>. */
    public synchronized MailItem getItemRevision(OperationContext octxt, int id, byte type, int version) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getItemRevision", octxt);
            MailItem revision = checkAccess(getItemById(id, type)).getRevision(version);

            success = true;
            return revision;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns a {@link List} containing all available revisions of an item,
     *  both current and past.  These revisions are returned in increasing
     *  order of their 1-based "version", with the current revision always
     *  present and listed last. */
    public synchronized List<MailItem> getAllRevisions(OperationContext octxt, int id, byte type) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getAllRevisions", octxt);
            MailItem item = checkAccess(getItemById(id, type));
            List<MailItem> revisions = new ArrayList<MailItem>(item.loadRevisions());
            revisions.add(item);

            success = true;
            return revisions;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Fetches a <tt>MailItem</tt> by its IMAP id.
     * @throws MailServiceException if there is no <tt>MailItem</tt> with the given id.
     * @see MailServiceException#NO_SUCH_ITEM
     */
    public synchronized MailItem getItemByImapId(OperationContext octxt, int imapId, int folderId) throws ServiceException {
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemByImapId", octxt);

            MailItem item = checkAccess(getCachedItem(imapId));
            // in general, the item will not have been moved and its id will be the same as its IMAP id.
            if (item == null) {
                try {
                    item = checkAccess(MailItem.getById(this, imapId));
                    if (item.getImapUid() != imapId)
                        item = null;
                } catch (NoSuchItemException nsie) { }
            }
            // if it's not found, we have to search on the non-indexed IMAP_ID column...
            if (item == null)
                item = checkAccess(MailItem.getByImapId(this, imapId, folderId));

            if (isCachedType(item.getType()) || item.getImapUid() != imapId || item.getFolderId() != folderId)
                throw MailServiceException.NO_SUCH_ITEM(imapId);
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    /** Fetches an item by path relative to {@link #ID_FOLDER_USER_ROOT}.
     * @see #getItemByPath(OperationContext, String, int) */
    public synchronized MailItem getItemByPath(OperationContext octxt, String path) throws ServiceException {
        return getItemByPath(octxt, path, ID_FOLDER_USER_ROOT);
    }

    /** Fetches an item by path.  If the path begins with <tt>/</tt>, it's
     *  considered an absolute path relative to {@link #ID_FOLDER_USER_ROOT}.
     *  If it doesn't, it's computed relative to the passed-in folder ID.<p>
     *  
     *  This can return anything with a name; at present, that is limited to
     *  {@link Folder}s, {@link Tag}s, and {@link Document}s. */
    public synchronized MailItem getItemByPath(OperationContext octxt, String name, int folderId) throws ServiceException {
        if (name == null || name.equals(""))
            return getFolderById(octxt, folderId);

        if (name.equals("/"))
        	return getFolderById(octxt, ID_FOLDER_USER_ROOT);
        
        if (name.startsWith("/")) {
            folderId = ID_FOLDER_USER_ROOT;
            name = name.substring(1);
        }
        if (name.endsWith("/"))
            name = name.substring(0, name.length() - 1);

        Folder parent = getFolderById(null, folderId);
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemByName", octxt);

            int slash = name.lastIndexOf('/');
            if (slash != -1) {
                for (String segment : name.substring(0, slash).split("/"))
                    if ((parent = parent.findSubfolder(segment)) == null)
                        throw MailServiceException.NO_SUCH_FOLDER(name);
                name = name.substring(slash + 1);
            }

            MailItem item = null;
            if (folderId == ID_FOLDER_TAGS) {
                item = getTagByName(name);
            } else {
                // check for the specified item -- folder first, then document
                item = parent.findSubfolder(name);
                if (item == null) {
                    checkAccess(parent);
                    item = getItem(DbMailItem.getByName(this, parent.getId(), name, MailItem.TYPE_DOCUMENT));
                }
            }
            // make sure the item is visible to the requester
            if (checkAccess(item) == null)
                throw MailServiceException.NO_SUCH_ITEM(name);
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns all the MailItems of a given type, optionally in a specified folder */
    public synchronized List<MailItem> getItemList(OperationContext octxt, byte type) throws ServiceException {
        return getItemList(octxt, type, -1);
    }

    public synchronized List<MailItem> getItemList(OperationContext octxt, byte type, int folderId) throws ServiceException {
        return getItemList(octxt, type, folderId, DbSearch.SORT_NONE);
    }

    public synchronized List<MailItem> getItemList(OperationContext octxt, byte type, int folderId, byte sort) throws ServiceException {
        if (type == MailItem.TYPE_UNKNOWN)
            return Collections.emptyList();
        List<MailItem> result = new ArrayList<MailItem>();

        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemList", octxt);

            Folder folder = folderId == -1 ? null : getFolderById(folderId);

            if (folder == null) {
                if (!hasFullAccess())
                    throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            } else {
                if (!folder.canAccess(ACL.RIGHT_READ, getAuthenticatedAccount(), isUsingAdminPrivileges()))
                    throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }

            if (type == MailItem.TYPE_TAG) {
                if (folderId != -1 && folderId != ID_FOLDER_TAGS)
                    return Collections.emptyList();
                for (Map.Entry<Object, Tag> entry : mTagCache.entrySet())
                    if (entry.getKey() instanceof String)
                        result.add(entry.getValue());
                Comparator<MailItem> comp = MailItem.getComparator(sort);
                if (comp != null)
                    Collections.sort(result, comp);
                success = true;
            } else if (type == MailItem.TYPE_FOLDER || type == MailItem.TYPE_SEARCHFOLDER || type == MailItem.TYPE_MOUNTPOINT) {
                for (Folder subfolder : mFolderCache.values())
                    if (subfolder.getType() == type)
                        if (folder == null || subfolder.getParentId() == folderId)
                            result.add(subfolder);
                Comparator<MailItem> comp = MailItem.getComparator(sort);
                if (comp != null)
                    Collections.sort(result, comp);
                success = true;
            } else {
            List<MailItem.UnderlyingData> dataList = null;
            if (folder != null)
                dataList = DbMailItem.getByFolder(folder, type, sort);
            else
                dataList = DbMailItem.getByType(this, type, sort);
            if (dataList == null)
                return Collections.emptyList();
            for (MailItem.UnderlyingData data : dataList)
                if (data != null)
                    result.add(getItem(data));
            	// except for sort == SORT_BY_NAME_NAT,
            // sort was already done by the DbMailItem call...
            	if ((sort & DbSearch.SORT_BY_NAME_NATURAL_ORDER) > 0)
                    Collections.sort(result, MailItem.getComparator(sort));
            success = true;
            }
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
            beginTransaction("listItemIds", octxt);

            Folder folder = getFolderById(folderId);
            List<SearchResult> srList = DbMailItem.listByFolder(folder, type, true);
            if (srList == null)
                return null;
            int i = 0, result[] = new int[srList.size()];
            for (SearchResult sr : srList)
                result[i++] = sr.id;
            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized List<ImapMessage> openImapFolder(OperationContext octxt, int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("openImapFolder", octxt);

            Folder folder = getFolderById(folderId);
            List<ImapMessage> i4list = DbMailItem.loadImapFolder(folder);
            success = true;
            return i4list;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized List<Pop3Message> openPop3Folder(OperationContext octxt, int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("openPop3Folder", octxt);

            Folder folder = getFolderById(folderId);
            List<Pop3Message> p3list = DbMailItem.loadPop3Folder(folder);
            success = true;
            return p3list;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized int countImapRecent(OperationContext octxt, int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("openImapFolder", octxt);

            Folder folder = checkAccess(getFolderById(folderId));
            int recent = DbMailItem.countImapRecent(folder);
            success = true;
            return recent;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized void beginTrackingImap() throws ServiceException {
        if (isTrackingImap())
            return;

        TrackImap redoRecorder = new TrackImap(mId);
        boolean success = false;
        try {
            beginTransaction("beginTrackingImap", null, redoRecorder);

            DbMailbox.startTrackingImap(this);
            mCurrentChange.imap = Boolean.TRUE;

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void beginTrackingSync() throws ServiceException {
        if (isTrackingSync())
            return;

        TrackSync redoRecorder = new TrackSync(mId);
        boolean success = false;
        try {
            beginTransaction("beginTrackingSync", null, redoRecorder);

            DbMailbox.startTrackingSync(this);
            mCurrentChange.sync = getLastChangeID();

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void recordImapSession(int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("recordImapSession", null);
            getFolderById(folderId).checkpointRECENT();
            success = true;
        } finally {
            endTransaction(success);
        }
    }


    public synchronized List<Integer> getTombstones(int lastSync) throws ServiceException {
        if (!isTrackingSync())
            throw ServiceException.FAILURE("not tracking sync", null);

        boolean success = false;
        try {
            beginTransaction("getTombstones", null);
            MailItem.TypedIdList tombstones = DbMailItem.readTombstones(this, lastSync);
            success = true;
            return tombstones.getAll();
        } finally {
            endTransaction(success);
        }
    }

    public synchronized MailItem.TypedIdList getTombstoneSet(int lastSync) throws ServiceException {
        if (!isTrackingSync())
            throw ServiceException.FAILURE("not tracking sync", null);

        boolean success = false;
        try {
            beginTransaction("getTombstones", null);
            MailItem.TypedIdList tombstones = DbMailItem.readTombstones(this, lastSync);
            success = true;
            return tombstones;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized List<Folder> getModifiedFolders(final int lastSync) throws ServiceException {
        return getModifiedFolders(lastSync, MailItem.TYPE_UNKNOWN);
    }

    public synchronized List<Folder> getModifiedFolders(final int lastSync, final byte type) throws ServiceException {
        if (lastSync >= getLastChangeID())
            return Collections.emptyList();

        List<Folder> modified = new ArrayList<Folder>();
        boolean success = false;
        try {
            beginTransaction("getModifiedFolders", null);
            for (Folder subfolder : getFolderById(ID_FOLDER_ROOT).getSubfolderHierarchy()) {
                if (type == MailItem.TYPE_UNKNOWN || subfolder.getType() == type)
                    if (subfolder.getModifiedSequence() > lastSync)
                        modified.add(subfolder);
            }
            success = true;
            return modified;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized List<Tag> getModifiedTags(OperationContext octxt, int lastSync) throws ServiceException {
        if (lastSync >= getLastChangeID())
            return Collections.emptyList();

        List<Tag> modified = new ArrayList<Tag>();
        boolean success = false;
        try {
            beginTransaction("getModifiedTags", octxt);
            if (hasFullAccess()) {
                for (Map.Entry<Object, Tag> entry : mTagCache.entrySet())
                    if (entry.getKey() instanceof String) {
                        Tag tag = entry.getValue();
                        if (tag.getModifiedSequence() > lastSync && !(tag instanceof Flag))
                            modified.add(tag);
                    }
            }
            success = true;
            return modified;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns the IDs of all items modified since a given change number.
     *  Will not return modified folders or tags; for these you need to call
     *  {@link #getModifiedFolders(long, byte)} or
     *  {@link #getModifiedTags(OperationContext, long)}.  Modified items not
     *  visible to the caller (i.e. the caller lacks {@link ACL#RIGHT_READ})
     *  are returned in a separate Integer List in the returned Pair.
     *  
     * @param octxt     The context for this request (e.g. auth user id).
     * @param lastSync  We return items with change ID larger than this value.
     * @return A {@link Pair} containing:<ul>
     *         <li>a List of the IDs of all caller-visible MailItems of the
     *             given type modified since the checkpoint, and
     *         <li>a List of the IDs of all items modified since the checkpoint
     *             but not currently visible to the caller</ul> */
    public synchronized Pair<List<Integer>,MailItem.TypedIdList> getModifiedItems(OperationContext octxt, int lastSync) throws ServiceException {
        return getModifiedItems(octxt, lastSync, MailItem.TYPE_UNKNOWN, null);
    }

    /** Returns the IDs of all items of the given type modified since a given
     *  change number.  Will not return modified folders or tags; for these
     *  you need to call {@link #getModifiedFolders(long, byte)} or
     *  {@link #getModifiedTags(OperationContext, long)}.  Modified items not
     *  visible to the caller (i.e. the caller lacks {@link ACL#RIGHT_READ})
     *  are returned in a separate Integer List in the returned Pair.  When
     *  <tt>type</tt> is {@link MailItem#TYPE_UNKNOWN}, all modified non-
     *  tag, non-folders are returned.
     *  
     * @param octxt     The context for this request (e.g. auth user id).
     * @param lastSync  We return items with change ID larger than this value.
     * @param type      The type of MailItems to return.
     * @return A {@link Pair} containing:<ul>
     *         <li>a List of the IDs of all caller-visible MailItems of the
     *             given type modified since the checkpoint, and
     *         <li>a List of the IDs of all items of the given type modified
     *             since the checkpoint but not currently visible to the
     *             caller</ul> */
    public synchronized Pair<List<Integer>,MailItem.TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, byte type) throws ServiceException {
        return getModifiedItems(octxt, lastSync, type, null);
    }

    private static final List<Integer> EMPTY_ITEMS = Collections.emptyList();

    public synchronized Pair<List<Integer>,MailItem.TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, byte type, Set<Integer> folderIds) throws ServiceException {
        if (lastSync >= getLastChangeID())
            return new Pair<List<Integer>,MailItem.TypedIdList>(EMPTY_ITEMS, new MailItem.TypedIdList());

        boolean success = false;
        try {
            beginTransaction("getModifiedItems", octxt);

            Set<Integer> visible = getVisibleFolderIds();
            if (folderIds == null)
                folderIds = visible;
            else if (visible != null)
                folderIds = SetUtil.intersect(folderIds, visible);

            Pair<List<Integer>,MailItem.TypedIdList> dataList = DbMailItem.getModifiedItems(this, type, lastSync, folderIds);
            if (dataList == null)
                return null;
            success = true;
            return dataList;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns a list of all <code>Folder</code>s the authenticated user has
     *  {@link ACL#RIGHT_READ} access to.  Returns <tt>null</tt> if the
     *  authenticated user has read access to the entire Mailbox. */
    public synchronized Set<Folder> getVisibleFolders(OperationContext octxt) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getVisibleFolders", octxt);
            Set<Folder> visible = getVisibleFolders();
            success = true;
            return visible;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns a list of all <code>Folder</code>s that the authenticated user
     *  from the current transaction has {@link ACL#RIGHT_READ} access to.
     *  Returns <tt>null</tt> if the authenticated user has read access to
     *  the entire Mailbox. */
    Set<Folder> getVisibleFolders() throws ServiceException {
        return getAccessibleFolders(ACL.RIGHT_READ);
    }

    /** Returns a list of all <code>Folder</code>s that the authenticated user
     *  from the current transaction has a certain set of rights on.  Returns
     *  <tt>null</tt> if the authenticated user has the required access on the
     *  entire Mailbox.
     * @param rights  The bitmask representing the required permissions. */
    Set<Folder> getAccessibleFolders(short rights) throws ServiceException {
        if (!mCurrentChange.isActive())
            throw ServiceException.FAILURE("cannot get visible hierarchy outside transaction", null);
        if (hasFullAccess())
            return null;

        boolean incomplete = false;
        Set<Folder> visible = new HashSet<Folder>();
        for (Folder folder : mFolderCache.values())
            if (folder.canAccess(rights))
                visible.add(folder);
            else
                incomplete = true;
        return incomplete ? visible : null;
    }

    /** Returns a list of the IDs of all <code>Folder</code>s that the
     *  current transaction's authenticated user has {@link ACL#RIGHT_READ}
     *  access on.  Returns <tt>null</tt> if the authenticated user has read
     *  access on the entire Mailbox. */
    Set<Integer> getVisibleFolderIds() throws ServiceException {
        Set<Folder> folders = getVisibleFolders();
        if (folders == null)
            return null;
        Set<Integer> visible = new HashSet<Integer>(folders.size());
        for (Folder folder : folders)
            visible.add(folder.getId());
        return visible;
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

    public synchronized List<Tag> getTagList(OperationContext octxt) throws ServiceException {
        List<Tag> tags = new ArrayList<Tag>();
        for (MailItem item : getItemList(octxt, MailItem.TYPE_TAG))
            tags.add((Tag) item);
        return tags;
    }

    public synchronized Tag getTagByName(String name) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getTagByName", null);

            if (name == null || name.equals(""))
                throw ServiceException.INVALID_REQUEST("tag name may not be null", null);
            Tag tag = mTagCache.get(name.toLowerCase());
            if (tag == null)
                throw MailServiceException.NO_SUCH_TAG(name);
            checkAccess(tag);
            success = true;
            return tag;
        } finally {
            endTransaction(success);
        }
    }


    /** Returns the folder with the specified id.
     * @throws NoSuchItemException if the folder does not exist */
    public synchronized Folder getFolderById(OperationContext octxt, int id) throws ServiceException {
        return (Folder) getItemById(octxt, id, MailItem.TYPE_FOLDER);
    }
    
    /** Returns the folder with the specified id.
     * @throws NoSuchItemException if the folder does not exist */
    Folder getFolderById(int id) throws ServiceException {
        return (Folder) getItemById(id, MailItem.TYPE_FOLDER);
    }
    
    /** Returns the folder with the specified parent and name.
     * @throws NoSuchItemException if the folder does not exist */
    public synchronized Folder getFolderByName(OperationContext octxt, int parentId, String name) throws ServiceException {
        boolean success = false;
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

    /** Returns the folder with the specified path, delimited by slashes (<tt>/</tt>).
     * @throws {@link NoSuchItemException} if the folder does not exist */
    public synchronized Folder getFolderByPath(OperationContext octxt, String name) throws ServiceException {
        if (name == null)
            throw MailServiceException.NO_SUCH_FOLDER(name);
        while (name.startsWith("/"))
            name = name.substring(1);                         // strip off the optional leading "/"
        while (name.endsWith("/"))
            name = name.substring(0, name.length() - 1);      // strip off the optional trailing "/"

        Folder folder = getFolderById(null, ID_FOLDER_USER_ROOT);

        boolean success = false;
        try {
            beginTransaction("getFolderByPath", octxt);
            if (!name.equals("")) {
                for (String segment : name.split("/"))
                    if ((folder = folder.findSubfolder(segment)) == null)
                        break;
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
    
    /**
     * Given a path, resolves as much of the path as possible and returns the folder and the unmatched part.
     * 
     * E.G. if the path is "/foo/bar/baz/gub" and this mailbox has a Folder at "/foo/bar" -- this API returns
     * a Pair containing that Folder and the unmatched part "baz/gub".  
     * 
     * If the returned folder is a Mountpoint, then it can be assumed that the remaining part is a subfolder in
     * the remote mailbox.
     * 
     * @param octxt
     * @param startingFolderId Folder to start from (pass Mailbox.ID_FOLDER_ROOT to start from the root)
     * @param path 
     * @return
     * @throws ServiceException
     */
    public synchronized Pair<Folder, String> getFolderByPathLongestMatch(OperationContext octxt, int startingFolderId, String path) throws ServiceException {
        if (path == null)
            throw MailServiceException.NO_SUCH_FOLDER(path);
        while (path.startsWith("/"))
            path = path.substring(1);                         // strip off the optional leading "/"
        while (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);      // strip off the optional trailing "/"
        
        if (path.length() == 0)
            throw MailServiceException.NO_SUCH_FOLDER("/" + path);
            
        Folder folder = getFolderById(null, startingFolderId); 
        assert(folder != null);

        boolean success = false;
        try {
            beginTransaction("getFolderByPathLongestMatch", octxt);

            String unmatched = null, segments[] = path.split("/");
            for (int i = 0; i < segments.length; i++) {
                Folder subfolder = folder.findSubfolder(segments[i]);
                if (subfolder == null) {
                    unmatched = StringUtil.join("/", segments, i, segments.length - i);
                    break;
                }
                folder = subfolder;
            }
            // apply the "read access" check to the returned folder...
            return new Pair<Folder, String>(checkAccess(folder), unmatched);
        } finally {
            endTransaction(success);
        }
    }

    public synchronized List<Folder> getFolderList(OperationContext octxt, byte sort) throws ServiceException {
        List<Folder> folders = new ArrayList<Folder>();
        for (MailItem item : getItemList(octxt, MailItem.TYPE_FOLDER, -1, sort))
            folders.add((Folder) item);
        return folders;
    }

    List<Folder> listAllFolders() {
        return new ArrayList<Folder>(mFolderCache.values());
    }


    public synchronized SearchFolder getSearchFolderById(OperationContext octxt, int searchId) throws ServiceException {
        return (SearchFolder) getItemById(octxt, searchId, MailItem.TYPE_SEARCHFOLDER);
    }
    SearchFolder getSearchFolderById(int searchId) throws ServiceException {
        return (SearchFolder) getItemById(searchId, MailItem.TYPE_SEARCHFOLDER);
    }


    public synchronized Mountpoint getMountpointById(OperationContext octxt, int mptId) throws ServiceException {
        return (Mountpoint) getItemById(octxt, mptId, MailItem.TYPE_MOUNTPOINT);
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

    public synchronized List<Contact> getContactList(OperationContext octxt, int folderId) throws ServiceException {
        return getContactList(octxt, folderId, DbSearch.SORT_NONE);
    }

    public synchronized List<Contact> getContactList(OperationContext octxt, int folderId, byte sort) throws ServiceException {
        List<Contact> contacts = new ArrayList<Contact>();
        for (MailItem item : getItemList(octxt, MailItem.TYPE_CONTACT, folderId, sort))
            contacts.add((Contact) item);
        return contacts;
    }

    /**
     * Returns the <tt>Message</tt> with the specified id. 
     * @throws NoSuchItemException if the item does not exist
     */
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

    public synchronized List<Message> getMessagesByConversation(OperationContext octxt, int convId) throws ServiceException {
        return getMessagesByConversation(octxt, convId, Conversation.SORT_ID_ASCENDING);
    }

    public synchronized List<Message> getMessagesByConversation(OperationContext octxt, int convId, byte sort) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getMessagesByConversation", octxt);
            List<Message> msgs = getConversationById(convId).getMessages(sort);
            if (!hasFullAccess()) {
                List<Message> visible = new ArrayList<Message>(msgs.size());
                for (Message msg : msgs)
                    if (msg.canAccess(ACL.RIGHT_READ))
                        visible.add(msg);
                msgs = visible;
            }
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


    public WikiItem getWikiById(OperationContext octxt, int id) throws ServiceException {
        return (WikiItem) getItemById(octxt, id, MailItem.TYPE_WIKI);
    }


    public Document getDocumentById(OperationContext octxt, int id) throws ServiceException {
        return (Document) getItemById(octxt, id, MailItem.TYPE_DOCUMENT);
    }

    Document getDocumentById(int id) throws ServiceException {
        return (Document) getItemById(id, MailItem.TYPE_DOCUMENT);
    }

    public synchronized List<Document> getDocumentList(OperationContext octxt, int folderId) throws ServiceException {
        return getDocumentList(octxt, folderId, DbSearch.SORT_NONE);
    }

    public synchronized List<Document> getDocumentList(OperationContext octxt, int folderId, byte sort) throws ServiceException {
        List<Document> docs = new ArrayList<Document>();
        for (MailItem item : getItemList(octxt, MailItem.TYPE_DOCUMENT, folderId, sort))
            docs.add((Document) item);
        return docs;
    }


    private void checkCalendarType(MailItem item) throws ServiceException {
        byte type = item.getType();
        if (type != MailItem.TYPE_APPOINTMENT && type != MailItem.TYPE_TASK)
            throw MailServiceException.NO_SUCH_CALITEM(item.getId());
    }

    public synchronized CalendarItem getCalendarItemById(OperationContext octxt, int id) throws ServiceException {
        MailItem item = getItemById(octxt, id, MailItem.TYPE_UNKNOWN);
        checkCalendarType(item);
        return (CalendarItem) item;
    }

    CalendarItem getCalendarItemById(int id) throws ServiceException {
        MailItem item = getItemById(id, MailItem.TYPE_UNKNOWN);
        checkCalendarType(item);
        return (CalendarItem) item;
    }

    CalendarItem getCalendarItem(MailItem.UnderlyingData data) throws ServiceException {
        return (CalendarItem) getItem(data);
    }

    public synchronized List getCalendarItemList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.TYPE_UNKNOWN, folderId);
    }


    public synchronized Appointment getAppointmentById(OperationContext octxt, int id) throws ServiceException {
        return (Appointment) getItemById(octxt, id, MailItem.TYPE_APPOINTMENT);
    }

    Appointment getAppointmentById(int id) throws ServiceException {
        return (Appointment) getItemById(id, MailItem.TYPE_APPOINTMENT);
    }

    public synchronized List getAppointmentList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.TYPE_APPOINTMENT, folderId);
    }


    public synchronized Task getTaskById(OperationContext octxt, int id) throws ServiceException {
        return (Task) getItemById(octxt, id, MailItem.TYPE_TASK);
    }

    Task getTaskById(int id) throws ServiceException {
        return (Task) getItemById(id, MailItem.TYPE_TASK);
    }

    public synchronized List getTaskList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.TYPE_TASK, folderId);
    }


    public synchronized ZVCalendar getZCalendarForCalendarItems(
            Collection<CalendarItem> calItems,
            boolean useOutlookCompatMode, boolean ignoreErrors, boolean allowPrivateAccess)
    throws ServiceException {
        ZVCalendar cal = new ZVCalendar();

        // REPLY
        cal.addProperty(new ZProperty(ICalTok.METHOD, ICalTok.PUBLISH.toString()));

        // timezones
        {
            ICalTimeZone localTz = ICalTimeZone.getAccountTimeZone(getAccount()); 
            TimeZoneMap tzmap = new TimeZoneMap(localTz);

            for (CalendarItem calItem : calItems)
                tzmap.add(calItem.getTimeZoneMap());

            // iterate the tzmap and add all the VTimeZone's 
            // (TODO: should this code live in TimeZoneMap???) 
            for (Iterator iter = tzmap.tzIterator(); iter.hasNext(); ) {
                ICalTimeZone cur = (ICalTimeZone) iter.next();
                cal.addComponent(cur.newToVTimeZone());
            }
        }

        // build all the event components and add them to the Calendar
        for (CalendarItem calItem : calItems)
            calItem.appendRawCalendarData(cal, useOutlookCompatMode, ignoreErrors, allowPrivateAccess);
        return cal;
    }

    public synchronized ZVCalendar getZCalendarForRange(
            OperationContext octxt,
            long start, long end, int folderId,
            boolean useOutlookCompatMode, boolean allowPrivateAccess)
    throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getCalendarForRange", octxt);
            Collection<CalendarItem> calItems = getCalendarItemsForRange(octxt, start, end, folderId, null);
            return getZCalendarForCalendarItems(calItems, useOutlookCompatMode, false, allowPrivateAccess);
        } finally {
            endTransaction(success);
        }
    }


    /** Returns a <tt>Collection</tt> of all {@link CalendarItem}s which
     *  overlap the specified time period.  There is no guarantee that the
     *  returned calendar items actually contain a recurrence within the range;
     *  all that is required is that there is some intersection between the
     *  (<tt>start</tt>, <tt>end</tt>) range and the period from the
     *  start time of the calendar item's first recurrence to the end time of
     *  its last recurrence.<p>
     * 
     *  If a <tt>folderId</tt> is specified, only calendar items
     *  in that folder are returned.  If {@link #ID_AUTO_INCREMENT} is passed
     *  in as the <tt>folderId</tt>, all calendar items not in
     *  <tt>Spam</tt> or <tt>Trash</tt> are returned.
     * 
     * @param type      If MailItem.TYPE_APPOINTMENT, return only appointments.
     *                  If MailItem.TYPE_TASK, return only tasks.
     *                  If MailItem.TYPE_UNKNOWN, return both.
     * @param octxt     The {@link Mailbox.OperationContext}.
     * @param start     The start time of the range, in milliseconds.
     * @param end       The end time of the range, in milliseconds.
     * @param folderId  The folder to search for matching calendar items, or
     *                  {@link #ID_AUTO_INCREMENT} to search all non-Spam and
     *                  Trash folders in the mailbox.
     * @perms {@link ACL#RIGHT_READ} on all returned calendar items.
     * @throws ServiceException */
    public synchronized Collection<CalendarItem> getCalendarItemsForRange(
            byte type,
            OperationContext octxt, long start, long end, 
            int folderId, int[] excludeFolders)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getCalendarItemsForRange", octxt);

            // if they specified a folder, make sure it actually exists
            if (folderId != ID_AUTO_INCREMENT)
                getFolderById(folderId);

            // get the list of all visible calendar items in the specified folder
            List<CalendarItem> calItems = new ArrayList<CalendarItem>();
            List<UnderlyingData> invData = DbMailItem.getCalendarItems(this, type, start, end, folderId, excludeFolders);
            for (MailItem.UnderlyingData data : invData) {
                try {
                    CalendarItem calItem = getCalendarItem(data);
                    if (folderId == calItem.getFolderId() || (folderId == ID_AUTO_INCREMENT && calItem.inMailbox()))
                        if (calItem.canAccess(ACL.RIGHT_READ))
                            calItems.add(calItem);
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn(
                            "Error while retrieving calendar item " +
                            data.id + " in mailbox " + mId +
                            "; skipping item", e);
                }
            }
            success = true;
            return calItems;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized Collection<CalendarItem> getCalendarItemsForRange(
            OperationContext octxt, long start, long end, 
            int folderId, int[] excludeFolders)
            throws ServiceException {
        return getCalendarItemsForRange(
                MailItem.TYPE_UNKNOWN, octxt, start, end, folderId, excludeFolders);
    }

    /**
     * Specifies the type of result we want from the call to search()
     */
    public static enum SearchResultMode {
        NORMAL,        // everything
        IMAP,          // only IMAP data
        MODSEQ,        // only the metadata modification sequence number
        IDS;           // only IDs
        
        public static SearchResultMode get(String value) throws ServiceException {
            if (value == null)
                return NORMAL;
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("Unknown resultMode value: "  +value, null);
            }
        }
    }
    
    /**
     * You **MUST** call {@link ZimbraQueryResults#doneWithSearchResults()} when you are done with the search results, otherwise
     * resources will be leaked.
     * 
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
    public ZimbraQueryResults search(OperationContext octxt, String queryString, byte[] types, SortBy sortBy, int chunkSize) 
    throws IOException, ParseException, ServiceException {
        SearchParams params = new SearchParams();
        params.setQueryStr(queryString);
        params.setTimeZone(null);
        params.setLocale(null);
        params.setTypes(types);
        params.setSortBy(sortBy);
        params.setChunkSize(chunkSize);
        params.setPrefetch(true);
        params.setMode(SearchResultMode.NORMAL);
        return search(SoapProtocol.Soap12, octxt, params);
    }
    
    /**
     * This is the preferred form of the API call.
     * 
     * You **MUST** call {@link ZimbraQueryResults#doneWithSearchResults()} when you are done with the search results, otherwise
     * resources will be leaked.
     * 
     * @param proto  The soap protocol the request is coming from.  Determines the type of Element we create for proxied results.
     * @param octxt  Operation Context
     * @param params Search Parameters
     * @return
     * @throws IOException
     * @throws ParseException
     * @throws ServiceException
     */
    public ZimbraQueryResults search(SoapProtocol proto, OperationContext octxt, SearchParams params) throws IOException, ParseException, ServiceException {
        if (octxt == null)
            throw ServiceException.INVALID_REQUEST("The OperationContext must not be null", null);
        
            synchronized (this) {
            if (mMessagesToIndex.size() > 0) {
                ZimbraLog.mailbox.debug("DEBUG_BATCH_MESSAGE_INDEXING -- indexing "+mMessagesToIndex.size()+" messages");
                reIndex(octxt, null, mMessagesToIndex, 0, true);
                mMessagesToIndex = new HashSet<Integer>();
                }
            }
        return MailboxIndex.search(proto, octxt, this, params);
        }
    
    /**
     * @param octxt
     * @param params
     * @return A "mailbox neutral" representation of the query string: ie one that is re-written so that all Folder names (and other by-name
     *         search parts) are re-written using ID's.  This is useful in some situations where you want to proxy the search
     *         request (since you cannot directly proxy a search request with local folder names in it)
     * @throws IOException
     * @throws ParseException
     * @throws ServiceException
     */
    public String getRewrittenQueryString(OperationContext octxt, SearchParams params) throws ParseException, ServiceException {
        if (octxt == null)
            throw ServiceException.INVALID_REQUEST("The OperationContext must not be null", null);
        
        // okay, lets run the search through the query parser -- this has the side-effect of
        // re-writing the query in a format that is OK to proxy to the other server
        ZimbraQuery zq = new ZimbraQuery(this, params);
        return zq.toQueryString();
    }
    
    public synchronized FreeBusy getFreeBusy(long start, long end) throws ServiceException {
        return FreeBusy.getFreeBusyList(this, start, end);
    }

    private void addDomains(HashMap<String, DomainItem> domainItems, HashSet<String> newDomains, int flag) {
        for (String domain : newDomains) {
            DomainItem di = domainItems.get(domain);
            if (di == null)
                domainItems.put(domain, di = new DomainItem(domain));
            di.setFlag(flag);
        }
    }

    public synchronized BrowseResult browse(OperationContext octxt, String browseBy) throws IOException, ServiceException {
        boolean success = false;
        try {
            beginTransaction("browse", octxt);
            if (!hasFullAccess())
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on this mailbox");
            if (browseBy != null)
                browseBy = browseBy.intern();

            BrowseResult browseResult = new BrowseResult();

            MailboxIndex idx = getMailboxIndex();
            if (idx != null) {
                if (browseBy == BROWSE_BY_ATTACHMENTS) {
                    idx.getAttachments(browseResult.getResult());
                } else if (browseBy == BROWSE_BY_DOMAINS) {
                    HashMap<String, DomainItem> domainItems = new HashMap<String, DomainItem>();
                    HashSet<String> set = new HashSet<String>();
    
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
            }
            success = true;
            return browseResult;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void dismissCalendarItemAlarm(OperationContext octxt, int calItemId, long dismissedAt)
    throws ServiceException {
        DismissCalendarItemAlarm redoRecorder = new DismissCalendarItemAlarm(getId(), calItemId, dismissedAt);
        boolean success = false;
        try {
            beginTransaction("setLastAlarm", octxt, redoRecorder);
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            if (calItem == null)
                throw MailServiceException.NO_SUCH_CALITEM(calItemId);
            calItem.updateNextAlarm(dismissedAt + 1);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public static class SetCalendarItemData {
        public Invite mInv;
        public boolean mForce;
        public ParsedMessage mPm;

        public String toString() {
            StringBuilder toRet = new StringBuilder();
            toRet.append("inv:").append(mInv.toString()).append("\n");
            toRet.append("force:").append(mForce ? "true\n" : "false\n");
            return toRet.toString();
        }
    }

    public synchronized int setCalendarItem(OperationContext octxt, int folderId,
                                            SetCalendarItemData defaultInv,
                                            SetCalendarItemData exceptions[],
                                            List<ReplyInfo> replies)
    throws ServiceException {
        return setCalendarItem(octxt, folderId, 0, 0, defaultInv, exceptions, replies, 0);
    }

    /**
     * @param octxt
     * @param exceptions can be NULL
     * @return calendar item ID 
     * @throws ServiceException
     */
    public synchronized int setCalendarItem(OperationContext octxt, int folderId, int flags, long tags,
                                            SetCalendarItemData defaultInv,
                                            SetCalendarItemData exceptions[],
                                            List<ReplyInfo> replies, long nextAlarm)
    throws ServiceException {
        flags = (flags & ~Flag.FLAG_SYSTEM);
        SetCalendarItem redoRecorder = new SetCalendarItem(getId(), attachmentsIndexingEnabled(), flags, tags);

        boolean success = false;
        try {
            beginTransaction("setCalendarItem", octxt, redoRecorder);
            SetCalendarItem redoPlayer = (octxt == null ? null : (SetCalendarItem) octxt.getPlayer());

            // allocate IDs for all of the passed-in invites (and the calendar item!) if necessary
            if (redoPlayer == null || redoPlayer.getCalendarItemId() == 0) {
                if (defaultInv != null)
                defaultInv.mInv.setInviteId(getNextItemId(Mailbox.ID_AUTO_INCREMENT));
                if (exceptions != null) {
                    for (SetCalendarItemData sad : exceptions)
                        sad.mInv.setMailItemId(getNextItemId(Mailbox.ID_AUTO_INCREMENT));
                }
            }
            redoRecorder.setData(defaultInv, exceptions, replies, nextAlarm);

            short volumeId = redoPlayer == null ? Volume.getCurrentMessageVolume().getId() : redoPlayer.getVolumeId();

            // Make a single list containing default and exceptions.
            int scidLen = (defaultInv != null ? 1 : 0) + (exceptions != null ? exceptions.length : 0);
            List<SetCalendarItemData> scidList = new ArrayList<SetCalendarItemData>(scidLen);
            if (defaultInv != null)
                scidList.add(defaultInv);
            if (exceptions != null) {
                for (SetCalendarItemData scid : exceptions) {
                    scidList.add(scid);
                }
            }

            boolean first = true;
            CalendarItem calItem = null;
            boolean calItemIsNew = true;
            long oldNextAlarm = 0;
            for (SetCalendarItemData scid : scidList) {
                if (first) {
                    // usually the default invite
                    first = false;
                    calItem = getCalendarItemByUid(scid.mInv.getUid());
                    calItemIsNew = calItem == null;
            if (calItemIsNew) {
                // ONLY create an calendar item if this is a REQUEST method...otherwise don't.
                        String method = scid.mInv.getMethod();
                        if ("REQUEST".equals(method) || "PUBLISH".equals(method)) {
                            calItem = createCalendarItem(
                                    folderId, volumeId, flags, tags,
                                    scid.mInv.getUid(), scid.mPm, scid.mInv, 0);
                } else {
                    return 0; // for now, just ignore this Invitation
                }
            } else {
                        // Preserve alarm time before any modification is made to the item.
                        AlarmData alarmData = calItem.getAlarmData();
                        if (alarmData != null)
                            oldNextAlarm = alarmData.getNextAt();

                calItem.setTags(flags, tags);
                        calItem.processNewInvite(scid.mPm, scid.mInv, scid.mForce, folderId, volumeId,
                                                 nextAlarm, true);
                    }
                    redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId(), volumeId);
                } else {
                    // exceptions
                    calItem.processNewInvite(scid.mPm, scid.mInv, scid.mForce, folderId, volumeId, 0);
                }
            }

            // Recompute alarm time after processing all Invites.
            if (nextAlarm == 0)
                nextAlarm = oldNextAlarm;
            calItem.updateNextAlarm(nextAlarm);

            // Override replies list if one is provided.
            // Null list means keep existing replies.  Empty list means to clear existing replies.
            // List with one or more replies means replacing existing replies.
            if (replies != null)
                calItem.setReplies(replies);
            
            queueForIndexing(calItem, !calItemIsNew, null);

            success = true;
            return calItem.getId();
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Fix up timezone definitions in all appointments/tasks in the mailbox.
     * @param octxt
     * @param after
     * @param country
     * @return
     * @throws ServiceException
     */
    public int fixAllCalendarItemTimeZone(
            OperationContext octxt, long after, String country)
    throws ServiceException {
        int numFixed = 0;
        ZimbraLog.calendar.info("Started: timezone fixup in calendar of mailbox " + getId());
        List[] lists = new List[2];
        lists[0] = getItemList(octxt, MailItem.TYPE_APPOINTMENT);
        lists[1] = getItemList(octxt, MailItem.TYPE_TASK);
        for (List items : lists) {
            for (Iterator iter = items.iterator(); iter.hasNext(); ) {
                Object obj = iter.next();
                if (!(obj instanceof CalendarItem))
                    continue;
                CalendarItem calItem = (CalendarItem) obj;
                long end = calItem.getEndTime();
                if (end < after)
                    continue;
                try {
                    numFixed += fixCalendarItemTimeZone(octxt, calItem.getId(), after, country);
                } catch (ServiceException e) {
                    ZimbraLog.calendar.error(
                            "Error fixing calendar item " + calItem.getId() +
                            " in mailbox " + getId() + ": " + e.getMessage(), e);
                }
            }
        }
        ZimbraLog.calendar.info(
                "Finished: timezone fixup in calendar of mailbox " +
                getId() + "; fixed " + numFixed + " timezone entries");
        return numFixed;
    }

    /**
     * Fix up timezone definitions in an appointment/task.  Fixup is
     * required when governments change the daylight savings policy.
     * @param octxt
     * @param calItemId
     * @param after only look at appointments/tasks that have instances after
     *              this date/time
     * @param country two-letter country code; apply fixup specific to this country;
     *                default is null and means fixup will apply only the unambiguous
     *                timezone changes
     * @return number of timezone objects that were modified
     * @throws ServiceException
     */
    public synchronized int fixCalendarItemTimeZone(
            OperationContext octxt, int calItemId, long after, String country)
    throws ServiceException {
        FixCalendarItemTimeZone redoRecorder =
            new FixCalendarItemTimeZone(getId(), calItemId, after, country);
        boolean success = false;
        try {
            beginTransaction("fixCalendarItemTimeZone", octxt, redoRecorder);
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            int numFixed = TimeZoneFixup.fixCalendarItem(calItem, country);
            if (numFixed > 0) {
                ZimbraLog.calendar.info("Fixed " + numFixed + " timezone entries in calendar item " + calItem.getId());
                calItem.saveMetadata();
                // Need to uncache and refetch the item because there are fields
                // in the appointment/task that reference the old, pre-fix version
                // of the timezones.  We can either visit them all and update them,
                // or simply invalidate the calendar item and refetch it.
                uncacheItem(calItemId);
                calItem = getCalendarItemById(octxt, calItemId);
                markItemModified(calItem, Change.MODIFIED_CONTENT | Change.MODIFIED_INVITE);
                success = true;
            }
            return numFixed;
        } finally {
            endTransaction(success);
        }
    }

    public int fixAllCalendarItemEndTime(OperationContext octxt)
    throws ServiceException {
        int numFixed = 0;
        ZimbraLog.calendar.info("Started: end time fixup in calendar of mailbox " + getId());
        List[] lists = new List[2];
        lists[0] = getItemList(octxt, MailItem.TYPE_APPOINTMENT);
        lists[1] = getItemList(octxt, MailItem.TYPE_TASK);
        for (List items : lists) {
            for (Iterator iter = items.iterator(); iter.hasNext(); ) {
                Object obj = iter.next();
                if (!(obj instanceof CalendarItem))
                    continue;
                CalendarItem calItem = (CalendarItem) obj;
                try {
                    numFixed += fixCalendarItemEndTime(octxt, calItem);
                } catch (ServiceException e) {
                    ZimbraLog.calendar.error(
                            "Error fixing calendar item " + calItem.getId() +
                            " in mailbox " + getId() + ": " + e.getMessage(), e);
                }
            }
        }
        ZimbraLog.calendar.info(
                "Finished: end time fixup in calendar of mailbox " +
                getId() + "; fixed " + numFixed + " timezone entries");
        return numFixed;
    }

    public synchronized int fixCalendarItemEndTime(OperationContext octxt, CalendarItem calItem)
    throws ServiceException {
        FixCalendarItemEndTime redoRecorder = new FixCalendarItemEndTime(getId(), calItem.getId());
        boolean success = false;
        try {
            beginTransaction("fixupCalendarItemEndTime", octxt, redoRecorder);
            int numFixed = calItem.fixRecurrenceEndTime();
            if (numFixed > 0) {
                ZimbraLog.calendar.info("Fixed calendar item " + calItem.getId());
                markItemModified(calItem, Change.MODIFIED_CONTENT | Change.MODIFIED_INVITE);
                success = true;
            }
            return numFixed;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized int[] addInvite(OperationContext octxt, Invite inv, int folderId, boolean force, ParsedMessage pm)
    throws ServiceException {
        return addInvite(octxt, inv, folderId, force, pm, 0);
    }

    /**
     * Directly add an Invite into the system...this process also gets triggered when we add a Message
     * that has a text/calendar Mime part: but this API is useful when you don't want to add a corresponding
     * message.
     * @param octxt
     * @param inv
     * @param force if true, then force override the existing calendar item, false use normal RFC2446 sequencing rules
     * @param pm NULL is OK here
     * 
     * @return int[2] = { calendar-item-id, invite-mail-item-id }  Note that even though the invite has a mail-item-id, that mail-item does not really exist, it can ONLY be referenced through the calendar item "calItemId-invMailItemId"
     * @throws ServiceException
     */
    public synchronized int[] addInvite(OperationContext octxt, Invite inv, int folderId, boolean force, ParsedMessage pm,
                                        long nextAlarm)
    throws ServiceException {
        if (pm == null) {
            inv.setDontIndexMimeMessage(true); // the MimeMessage is fake, so we don't need to index it
            MimeMessage mm = CalendarMailSender.createCalendarMessage(inv);
            pm = new ParsedMessage(mm, octxt == null ? System.currentTimeMillis() : octxt.getTimestamp(), true);
        }

        byte[] data = null;
        try {
            data = pm.getRawData();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Caught IOException", ioe);
        }

        CreateInvite redoRecorder = new CreateInvite(mId, inv, folderId, data, force);

        boolean success = false;
        try {
            beginTransaction("addInvite", octxt, redoRecorder);
            CreateInvite redoPlayer = (octxt == null ? null : (CreateInvite) octxt.getPlayer());
            short volumeId = redoPlayer == null ? Volume.getCurrentMessageVolume().getId() : redoPlayer.getVolumeId();

            if (redoPlayer == null || redoPlayer.getCalendarItemId() == 0) {
                inv.setInviteId(getNextItemId(Mailbox.ID_AUTO_INCREMENT));
            }

            boolean calItemIsNew = false;
            CalendarItem calItem = getCalendarItemByUid(inv.getUid());
            if (calItem == null) { 
                // ONLY create an calendar item if this is a REQUEST method...otherwise don't.
                if (inv.getMethod().equals("REQUEST") || inv.getMethod().equals("PUBLISH")) {
                    calItem = createCalendarItem(folderId, volumeId, 0, 0, inv.getUid(), pm, inv, 0);
                    calItemIsNew = true;
                } else {
//                  mLog.info("Mailbox " + getId()+" Message "+getId()+" SKIPPING Invite "+method+" b/c not a REQUEST and no CalendarItem could be found");
                    return null; // for now, just ignore this Invitation
                }
            } else {
                if (!checkItemChangeID(calItem))
                    throw MailServiceException.MODIFY_CONFLICT();
                calItem.processNewInvite(pm, inv, force, folderId, volumeId, nextAlarm);
            }
            if (calItem != null)
                queueForIndexing(calItem, !calItemIsNew, null);
            redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId(), volumeId);

            success = true;
            return new int[] { calItem.getId(), inv.getMailItemId() };
        } finally {
            endTransaction(success);
        }
    }

    public synchronized CalendarItem getCalendarItemByUid(String uid) throws ServiceException {
        return getCalendarItemByUid(null, uid);
    }
    public synchronized CalendarItem getCalendarItemByUid(OperationContext octxt, String uid) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getCalendarItemByUid", octxt);
            MailItem.UnderlyingData data = DbMailItem.getCalendarItem(this, uid);
            CalendarItem calItem = (CalendarItem) getItem(data);
            success = true;
            return calItem;
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
        if (pref == null) {                                 // default to no deduping
            return false;
        } else if (pref.equalsIgnoreCase(DEDUPE_ALL)) {     // remove all duplicates
            return true;
        } else if (pref.equalsIgnoreCase(DEDUPE_SECOND)) {  // receive if we're not a direct recipient (to, cc, bcc)
            try {
                return !AccountUtil.isDirectRecipient(acct, mm);
            } catch (Exception e) {
                return false;
            }
        } else if (pref.equalsIgnoreCase(DEDUPE_INBOX)) {   // move the existing mail from sent to inbox
                // XXX: not implemented
                return false;
        } else {
                return false;
    }
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

    /**
     * Process an iCalendar REPLY containing a single VEVENT or VTODO.
     * @param octxt
     * @param inv REPLY iCalendar object
     * @throws ServiceException
     */
    public synchronized void processICalReply(OperationContext octxt, Invite inv)
    throws ServiceException {
        ICalReply redoRecorder = new ICalReply(getId(), inv);
        boolean success = false;
        try {
            beginTransaction("iCalReply", octxt, redoRecorder);
            String uid = inv.getUid();
            CalendarItem calItem = getCalendarItemByUid(uid);
            if (calItem == null) {
                ZimbraLog.calendar.warn(
                        "Unknown calendar item UID " + uid + " in mailbox " + getId());
                return;
            }
            if (calItem.processNewInviteReply(inv, false)) {
                queueForIndexing(calItem, false, null);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    private void processICalReplies(OperationContext octxt, ZVCalendar cal)
    throws ServiceException {
        Account authuser = octxt == null ? getAccount() : octxt.getAuthenticatedUser();
        boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();

        List<Invite> components = Invite.createFromCalendar(getAccount(), null, cal, false);
        for (Invite inv : components) {
            String orgAddress;
            if (inv.hasOrganizer()) {
                ZOrganizer org = inv.getOrganizer();
                orgAddress = org.getAddress();
            } else {
                ZimbraLog.calendar.warn("No ORGANIZER found in REPLY.  Assuming current mailbox.");
                orgAddress = getAccount().getName();
            }
            if (AccountUtil.addressMatchesAccount(getAccount(), orgAddress)) {
                processICalReply(octxt, inv);
            } else {
                Account orgAccount = inv.getOrganizerAccount();
                // Unknown organizer
                if (orgAccount == null) {
                    ZimbraLog.calendar.warn("Unknown organizer " + orgAddress + " in REPLY");
                    continue;
                }
                if (Provisioning.onLocalServer(orgAccount)) {
                    // Run in the context of organizer's mailbox.
                    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(orgAccount);
                    OperationContext orgOctxt = new OperationContext(mbox);
                    mbox.processICalReply(orgOctxt, inv);
                } else {
                    // Organizer's mailbox is on a remote server.
                    String uri = AccountUtil.getSoapUri(orgAccount);
                    if (uri == null) {
                        ZimbraLog.calendar.warn("Unable to determine URI for organizer account " + orgAddress);
                        continue;
                    }
                    try {
                        // TODO: Get the iCalendar data from the
                        // MIME part since we already have it.
                        String ical;
                        StringWriter sr = null;
                        try {
                            sr = new StringWriter();
                            inv.newToICalendar(true).toICalendar(sr);
                            ical = sr.toString();
                        } finally {
                            if (sr != null)
                                sr.close();
                        }
                        Options options = new Options();
                        options.setAuthToken(new AuthToken(authuser, isAdminRequest).getEncoded());
                        options.setTargetAccount(orgAccount.getName());
                        options.setTargetAccountBy(AccountBy.name);
                        options.setUri(uri);
                        options.setNoSession(true);
                        ZMailbox zmbox = ZMailbox.getMailbox(options);
                        zmbox.iCalReply(ical);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Error while posting REPLY to organizer mailbox host", e);
                    } catch (AuthTokenException e) {
                        throw ServiceException.FAILURE("Error while posting REPLY to organizer mailbox host", e);
                    }
                }
            }
        }
    }

    public Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags, String tags, int conversationId)
    throws IOException, ServiceException {
        SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext();
        return addMessage(octxt, pm, folderId, noICal, flags, tags, conversationId, ":API:", sharedDeliveryCtxt);
    } 

    public Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags, String tags)
    throws IOException, ServiceException {
        SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext();
        return addMessage(octxt, pm, folderId, noICal, flags, tags, ID_AUTO_INCREMENT, ":API:", sharedDeliveryCtxt);
    }

    public Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags, String tags,
                String rcptEmail, SharedDeliveryContext sharedDeliveryCtxt)
    throws IOException, ServiceException {
        return addMessage(octxt, pm, folderId, noICal, flags, tags, ID_AUTO_INCREMENT, rcptEmail, sharedDeliveryCtxt);
    }

    public Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId,
                boolean noICal, int flags, String tagStr, int conversationId,
                String rcptEmail, SharedDeliveryContext sharedDeliveryCtxt)
    throws IOException, ServiceException {
        // make sure the message has been analyzed before taking the Mailbox lock
        pm.analyze();
        // and then actually add the message
        long start = ZimbraPerf.STOPWATCH_MBOX_ADD_MSG.start();

        // We process calendar replies here, where no transaction has yet
        // been started on the current mailbox.  This is because some replies
        // may require starting a transaction on another mailbox.  We thus avoid
        // starting a nested transaction, which doesn't work.
        //
        // In addition, the current mailbox is not locked/synchronized at this
        // point.  If we were synchronized and a reply processing enters a
        // synchronized method on another mailbox, we're locking two mailboxes
        // and that can easily lead to deadlocks.
        //
        // TODO: Generalize this technique for all calendar operations, not
        // just REPLY's.
        //
        if (!noICal) {
            try {
                ZVCalendar cal = pm.getiCalendar();
                if (cal != null) {
                    ICalTok method = cal.getMethod();
                    if (ICalTok.REPLY.equals(method)) {
                        processICalReplies(octxt, cal);
                        noICal = true;
                    }
                }
            } catch (Exception e) {
                ZimbraLog.calendar.warn("Error during calendar processing.  Continuing with message add", e);
            }
        }

        Message msg = addMessageInternal(octxt, pm, folderId, noICal, flags, tagStr, conversationId, rcptEmail, null, sharedDeliveryCtxt);
        ZimbraPerf.STOPWATCH_MBOX_ADD_MSG.stop(start);
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

        if (conversationId <= HIGHEST_SYSTEM_ID)
            conversationId = ID_AUTO_INCREMENT;

        boolean needRedo = octxt == null || octxt.needRedo();
        CreateMessage redoPlayer = (octxt == null ? null : (CreateMessage) octxt.getPlayer());
        boolean isRedo = redoPlayer != null;

        // quick check to make sure we don't deliver 5 copies of the same message
        String msgidHeader = pm.getMessageID();
        boolean isSent = ((flags & Flag.BITMASK_FROM_ME) != 0);
        boolean checkDuplicates = (!isRedo && msgidHeader != null);
        if (checkDuplicates && !isSent && mSentMessageIDs.containsKey(msgidHeader)) {
            Integer sentMsgID = (Integer) mSentMessageIDs.get(msgidHeader);
            // if the rules say to drop this duplicated incoming message, return null now
            // don't dedupe messages carrying calendar part
            if (pm.getiCalendar() == null && dedupe(pm.getMimeMessage(), sentMsgID))
                return null;
            // if we're not dropping the new message, see if it goes in the same conversation as the old sent message
            if (conversationId == ID_AUTO_INCREMENT) {
                conversationId = getConversationIdFromReferent(pm.getMimeMessage(), sentMsgID.intValue());
                if (debug)  ZimbraLog.mailbox.debug("  duplicate detected but not deduped (" + msgidHeader + "); " +
                            "will try to slot into conversation " + conversationId);
            }
        }

        String digest;
        int msgSize;
        try {
            digest = pm.getRawDigest();
            msgSize = pm.getRawSize();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to get message properties.", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to get message properties.", e);
        }
        CreateMessage redoRecorder = new CreateMessage(mId, rcptEmail, pm.getReceivedDate(), sharedDeliveryCtxt.getShared(),
                                                       digest, msgSize, folderId, noICal, flags, tagStr);
        StoreIncomingBlob storeRedoRecorder = null;

        // strip out unread flag for internal storage (don't do this before redoRecorder initialization)
        boolean unread = (flags & Flag.BITMASK_UNREAD) > 0;
        flags = flags & ~Flag.BITMASK_UNREAD;

        boolean isSpam = folderId == ID_FOLDER_SPAM;
        boolean isDraft = ((flags & Flag.BITMASK_DRAFT) != 0);

        Message msg = null;
        Blob blob = null;
        MailboxBlob mboxBlob = null;
        boolean success = false;
        Folder folder = null;
        
        try {
            beginTransaction("addMessage", octxt, redoRecorder);
            if (isRedo)
                rcptEmail = redoPlayer.getRcptEmail();

            // "having attachments" is currently tracked via flags
            if (pm.hasAttachments())
                flags |= Flag.BITMASK_ATTACHED;
            else
                flags &= ~Flag.BITMASK_ATTACHED;

            // priority is calculated from headers
            flags &= ~(Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
            flags |= pm.getPriorityBitmask();

            folder  = getFolderById(folderId);
            String subject = pm.getNormalizedSubject();
            long   tags    = Tag.tagsToBitmask(tagStr);

            // step 1: get an ID assigned for the new message
            int messageId  = getNextItemId(!isRedo ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());
            if (isRedo)
                conversationId = redoPlayer.getConvId();

            // step 2: figure out where the message belongs
            Conversation conv = null;
            String hash = null;
            if (!DebugConfig.disableConversation) {
                if (conversationId != ID_AUTO_INCREMENT) {
                    try {
                        conv = getConversationById(conversationId);
                        if (debug)  ZimbraLog.mailbox.debug("  fetched explicitly-specified conversation " + conv.getId());
                    } catch (ServiceException e) {
                        if (e.getCode() != MailServiceException.NO_SUCH_CONV)
                            throw e;
                        if (debug)  ZimbraLog.mailbox.debug("  could not find explicitly-specified conversation " + conversationId);
                    }
                } else if (!isRedo && !isSpam && !isDraft && pm.isReply()) {
                    conv = getConversationByHash(hash = getHash(subject));
                    if (debug)  ZimbraLog.mailbox.debug("  found conversation " + (conv == null ? -1 : conv.getId()) + " for hash: " + hash);
                    // the caller can specify the received date via ParsedMessge constructor or X-Zimbra-Received header
                    if (conv != null && pm.getReceivedDate() > conv.getDate() + Constants.MILLIS_PER_MONTH) {
                        // if the last message in the conv was more than 1 month ago, it's probably not related...
                        conv = null;
                        if (debug)  ZimbraLog.mailbox.debug("  but rejected it because it's too old");
                    }
                }
            }

            // step 3: create the message and update the cache
            //         and if the message is also an invite, deal with the calendar item
            Conversation convTarget = (conv instanceof VirtualConversation ? null : conv);
            if (convTarget != null && debug)
                ZimbraLog.mailbox.debug("  placing message in existing conversation " + convTarget.getId());

            short volumeId = !isRedo ? Volume.getCurrentMessageVolume().getId() : redoPlayer.getVolumeId();
            ZVCalendar iCal = pm.getiCalendar();
            msg = Message.create(messageId, folder, convTarget, pm, msgSize, digest,
                        volumeId, unread, flags, tags, dinfo, noICal, iCal);

            redoRecorder.setMessageId(msg.getId());

            // step 4: create a conversation for the message, if necessary
            if (!DebugConfig.disableConversation && convTarget == null) {
                if (conv == null && conversationId == ID_AUTO_INCREMENT) {
                    conv = VirtualConversation.create(this, msg);
                    if (debug)  ZimbraLog.mailbox.debug("  placed message " + msg.getId() + " in vconv " + conv.getId());
                    redoRecorder.setConvFirstMsgId(-1);
                } else {
                    Message[] contents = null;
                    VirtualConversation vconv = null;
                    if (!isRedo) {
                        vconv = (VirtualConversation) conv;
                        contents = (conv == null ? new Message[] { msg } : new Message[] { vconv.getMessage(), msg });
                    } else {
                        // Executing redo.
                        int convFirstMsgId = redoPlayer.getConvFirstMsgId();
                        Message convFirstMsg = null;
                        // If there was a virtual conversation, then...
                        if (convFirstMsgId > 0) {
                            try {
                                convFirstMsg = getMessageById(octxt, redoPlayer.getConvFirstMsgId());
                            } catch (MailServiceException e) {
                                if (!MailServiceException.NO_SUCH_MSG.equals(e.getCode()))
                                    throw e;
                                // The first message of conversation may have been deleted
                                // by user between the time of original operation and redo.
                                // Handle the case by skipping the updating of its
                                // conversation ID.
                            }
                            // The message may have become part of a real conversation
                            // between the original operation and redo.  Leave it alone
                            // in that case, and only join it to this message's conversation
                            // if it is still a standalone message.
                            if (convFirstMsg != null && convFirstMsg.getConversationId() < 0) {
                                contents = new Message[] { convFirstMsg, msg };
                                vconv = new VirtualConversation(this, convFirstMsg);
                            }
                        }
                        if (contents == null)
                            contents = new Message[] { msg };
                    }
                    redoRecorder.setConvFirstMsgId(vconv != null ? vconv.getMessageId() : -1);
                    conv = createConversation(contents, conversationId);
                    if (vconv != null) {
                        if (debug)  ZimbraLog.mailbox.debug("  removed vconv " + vconv.getId());
                        vconv.removeChild(vconv.getMessage());
                    }
                }
                if (!isSpam && !isDraft)
                    openConversation(conv, hash);
            } else {
                // conversation feature turned off
                redoRecorder.setConvFirstMsgId(-1);
            }
            redoRecorder.setConvId(conv != null && !(conv instanceof VirtualConversation) ? conv.getId() : -1);

            // step 5: store the blob
            // TODO: Add partition support.  Need to store as many times as there
            //       are unique partitions in the set of recipient mailboxes.
            StoreManager sm = StoreManager.getInstance();
            if (sharedDeliveryCtxt.isFirst()) {
                sharedDeliveryCtxt.setFirst(false);
                // This mailbox is the only recipient, or it is the first
                // of multiple recipients.  Save message to incoming directory if
                // it's not already there.
                Blob preexisting = sharedDeliveryCtxt.getPreexistingBlob();
                if (preexisting == null) {
                    InputStream in = pm.getRawInputStream();
                    if (!isRedo) {
                        blob = sm.storeIncoming(in, msgSize, null, msg.getVolumeId());
                    } else {
                        // If message was delivered to a single recipient, ignore the path in the
                        // redo item and store to a new incoming path.  (bug 22873)
                        String path = sharedDeliveryCtxt.getShared() ? redoPlayer.getPath() : null;
                        blob = sm.storeIncoming(in, msgSize, path, redoPlayer.getVolumeId());
                    }
                    in.close();
                } else {
                    // Blob was already stored in incoming by the caller.
                    sharedDeliveryCtxt.setBlob(preexisting);
                    blob = preexisting;
                }
                String blobPath = blob.getPath();
                short blobVolumeId = blob.getVolumeId();

                if (sharedDeliveryCtxt.getShared()) {
                    markOtherItemDirty(blob);

                    // Log entry in redolog for blob save.  Blob bytes are
                    // logged in StoreToIncoming entry.
                    if (needRedo) {
                        storeRedoRecorder = new StoreIncomingBlob(digest, msgSize, sharedDeliveryCtxt.getMailboxIdList());
                        storeRedoRecorder.start(getOperationTimestampMillis());
                        if (blob.isCompressed() || pm.wasMutated()) {
                            storeRedoRecorder.setBlobBodyInfo(getData(pm), blobPath, blobVolumeId);
                        } else {
                            storeRedoRecorder.setBlobBodyInfo(blob.getFile(), blobVolumeId);
                        }
                        storeRedoRecorder.log();
                    }

                    // Create a link in mailbox directory and leave the incoming
                    // copy alone, so other recipients can link to it later.
                    redoRecorder.setMessageLinkInfo(blobPath, blobVolumeId, msg.getVolumeId());
                    mboxBlob = sm.link(blob, this, messageId, msg.getSavedSequence(), msg.getVolumeId());
                } else {
                    // If the only recipient, move the incoming copy into
                    // mailbox directory.  This is more efficient than
                    // creating a link in mailbox directory and deleting
                    // incoming copy.
                    pm.closeFile();
                    mboxBlob = sm.renameTo(blob, this, messageId, msg.getSavedSequence(), msg.getVolumeId());

                    // In single-recipient case the blob bytes are logged in
                    // CreateMessage entry, to avoid having to write two
                    // redolog entries for a single delivery.
                    if (blob.isCompressed() || pm.wasMutated()) {
                        redoRecorder.setMessageBodyInfo(getData(pm), blobPath, blobVolumeId);
                    } else {
                        redoRecorder.setMessageBodyInfo(mboxBlob.getBlob().getFile(), blobVolumeId);
                    }
                }
            } else {
                blob = sharedDeliveryCtxt.getBlob();
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

            if (sDebugBatchMessageIndexSize == 0) {
            queueForIndexing(msg, false, pm);
            } else {
                mMessagesToIndex.add(msg.getId());
                if (mMessagesToIndex.size() > sDebugBatchMessageIndexSize) {
                    ZimbraLog.mailbox.debug("DEBUG_BATCH_MESSAGE_INDEXING -- indexing "+mMessagesToIndex.size()+" messages");
                    reIndex(octxt, null, mMessagesToIndex, 0, true);
                    mMessagesToIndex = new HashSet<Integer>();
                }
            }
            
            success = true;
        } finally {
            if (storeRedoRecorder != null) {
                if (success)  storeRedoRecorder.commit();
                else          storeRedoRecorder.abort();
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

        // step 6: remember the Message-ID header so that we can avoid receiving duplicates
        if (isSent && checkDuplicates)
            mSentMessageIDs.put(msgidHeader, new Integer(msg.getId()));

        if (msg != null) {
            String folderName = (folder == null ? "undefined" : folder.getName());
            ZimbraLog.mailbox.info("Added message: id=%d, digest=%s, folderId=%d, folderName=%s",
                msg.getId(), digest, folderId, folderName);
        }
        return msg;
    }

    private byte[] getData(ParsedMessage pm)
    throws ServiceException {
        try {
            return pm.getRawData();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to get MIME data.", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to get MIME data.", e);
        }
    }
    
    public static String getHash(String subject) {
        return ByteUtil.getSHA1Digest(subject.getBytes(), true);
    }

    // please keep this package-visible but not public
    void openConversation(Conversation conv, String hash) throws ServiceException {
        if (hash == null)
            hash = getHash(conv.getNormalizedSubject());
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
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contents.length; i++)
                sb.append(i == 0 ? "" : ",").append(contents[i].getId());
            ZimbraLog.mailbox.debug("  created conv " + conv.getId() + " holding msg(s): " + sb);
        }
        return conv;
    }
    
    public Message saveDraft(OperationContext octxt, ParsedMessage pm, int id) throws IOException, ServiceException{
        return saveDraft(octxt, pm, id, null, null, null);
    }

    public Message saveDraft(OperationContext octxt, ParsedMessage pm, int id, String origId, String replyType, String identityId)
    throws IOException, ServiceException {
        // make sure the message has been analzyed before taking the Mailbox lock
        pm.analyze();
        try {
            pm.getRawData();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
        // special-case saving a new draft
        if (id == ID_AUTO_INCREMENT) {
            Message.DraftInfo dinfo = null;
            if ((replyType != null && origId != null) || (identityId != null && !identityId.equals("")))
                dinfo = new Message.DraftInfo(replyType, origId, identityId);
            return addMessageInternal(octxt, pm, ID_FOLDER_DRAFTS, true, Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME, null,
                                      ID_AUTO_INCREMENT, ":API:", dinfo, new SharedDeliveryContext());
        } else {
            return saveDraftInternal(octxt, pm, id);
        }
    }

    private synchronized Message saveDraftInternal(OperationContext octxt, ParsedMessage pm, int id) throws IOException, ServiceException {
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

            short volumeId = redoPlayer == null ? Volume.getCurrentMessageVolume().getId() : redoPlayer.getVolumeId();

            // update the content and increment the revision number
            Blob blob = msg.setContent(data, digest, volumeId, pm);
            redoRecorder.setMessageBodyInfo(data, blob.getPath(), blob.getVolumeId());

            // NOTE: msg is now uncached (will this cause problems during commit/reindex?)
            queueForIndexing(msg, true, pm);
            success = true;
            return msg;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Modify the Participant-Status of your LOCAL data part of an calendar item -- this is used when you Reply to
     * an Invite so that you can track the fact that you've replied to it.
     * 
     * @param octxt
     * @param calItemId
     * @param recurId
     * @param cnStr
     * @param addressStr
     * @param cutypeStr
     * @param roleStr
     * @param partStatStr
     * @param rsvp
     * @param seqNo
     * @param dtStamp
     * @throws ServiceException
     */
    public synchronized void modifyPartStat(OperationContext octxt, int calItemId, RecurId recurId,
                String cnStr, String addressStr, String cutypeStr, String roleStr, String partStatStr, Boolean rsvp, int seqNo, long dtStamp) 
    throws ServiceException {

        ModifyInvitePartStat redoRecorder = new ModifyInvitePartStat(mId, calItemId, recurId, cnStr, addressStr, cutypeStr, roleStr, partStatStr, rsvp, seqNo, dtStamp);

        boolean success = false;
        try {
            beginTransaction("updateInvitePartStat", octxt, redoRecorder);

            CalendarItem calItem = getCalendarItemById(calItemId);

            Account acct = getAccount();

            calItem.modifyPartStat(acct, recurId, cnStr, addressStr, cutypeStr, roleStr, partStatStr, rsvp, seqNo, dtStamp);
            markItemModified(calItem, Change.MODIFIED_INVITE);

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized List<Integer> resetImapUid(OperationContext octxt, List<Integer> itemIds) throws ServiceException {
        SetImapUid redoRecorder = new SetImapUid(mId, itemIds);

        List<Integer> newIds = new ArrayList<Integer>();
        boolean success = false;
        try {
            beginTransaction("resetImapUid", octxt, redoRecorder);
            SetImapUid redoPlayer = (SetImapUid) mCurrentChange.getRedoPlayer();

            for (int id : itemIds) {
                MailItem item = getItemById(id, MailItem.TYPE_UNKNOWN);
                int imapId = redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapUid(id);
                item.setImapUid(getNextItemId(imapId));
                redoRecorder.setImapUid(item.getId(), item.getImapUid());
                newIds.add(item.getImapUid());
            }
            success = true;
            return newIds;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void setColor(OperationContext octxt, int itemId, byte type, byte color) throws ServiceException {
        setColor(octxt, new int[] { itemId }, type, color);
    }
    public synchronized void setColor(OperationContext octxt, int[] itemIds, byte type, byte color) throws ServiceException {
        ColorItem redoRecorder = new ColorItem(mId, itemIds, type, color);

        boolean success = false;
        try {
            beginTransaction("setColor", octxt, redoRecorder);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items)
                if (!checkItemChangeID(item))
                    throw MailServiceException.MODIFY_CONFLICT();

            for (MailItem item : items)
                item.setColor(color);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void alterTag(OperationContext octxt, int itemId, byte type, int tagId, boolean addTag) throws ServiceException {
        alterTag(octxt, new int[] { itemId }, type, tagId, addTag, null);
    }
    public synchronized void alterTag(OperationContext octxt, int itemId, byte type, int tagId, boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        alterTag(octxt, new int[] { itemId }, type, tagId, addTag, tcon);
    }
    public synchronized void alterTag(OperationContext octxt, int[] itemIds, byte type, int tagId, boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        AlterItemTag redoRecorder = new AlterItemTag(mId, itemIds, type, tagId, addTag, tcon);

        boolean success = false;
        try {
            beginTransaction("alterTag", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items) {
                if (!(item instanceof Conversation))
                    if (!checkItemChangeID(item) && item instanceof Tag)
                        throw MailServiceException.MODIFY_CONFLICT();
            }

            for (MailItem item : items) {
                if (item == null)
                    continue;
                if (tagId == Flag.ID_FLAG_UNREAD) {
                    item.alterUnread(addTag);
                } else {
                    Tag tag = (tagId < 0 ? getFlagById(tagId) : getTagById(tagId));
                    item.alterTag(tag, addTag);
                }
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
    public synchronized void setTags(OperationContext octxt, int[] itemIds, byte type, String flagStr, String tagIDs, TargetConstraint tcon)
    throws ServiceException {
        int flags = (flagStr == null ? MailItem.FLAG_UNCHANGED : Flag.flagsToBitmask(flagStr));
        long tags = (tagIDs == null ? MailItem.TAG_UNCHANGED : Tag.tagsToBitmask(tagIDs));
        setTags(octxt, itemIds, type, flags, tags, tcon);
    }
    public synchronized void setTags(OperationContext octxt, int itemId, byte type, int flags, long tags, TargetConstraint tcon)
    throws ServiceException {
        setTags(octxt, new int[] { itemId }, type, flags, tags, tcon);
    }
    public synchronized void setTags(OperationContext octxt, int[] itemIds, byte type, int flags, long tags, TargetConstraint tcon)
    throws ServiceException {
        if (flags == MailItem.FLAG_UNCHANGED && tags == MailItem.TAG_UNCHANGED)
            return;

        SetItemTags redoRecorder = new SetItemTags(mId, itemIds, type, flags, tags, tcon);

        boolean success = false;
        try {
            beginTransaction("setTags", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items)
                checkItemChangeID(item);

            for (MailItem item : items) {
                if (item == null)
                    continue;

                int iflags = flags;  long itags = tags;
                if ((iflags & MailItem.FLAG_UNCHANGED) != 0)
                    iflags = item.getFlagBitmask();
                if ((itags & MailItem.TAG_UNCHANGED) != 0)
                    itags = item.getTagBitmask();
    
                // Special-case the unread flag.  It's passed in as a flag from the outside,
                // but treated as a separate argument inside the mailbox.
                boolean iunread = (iflags & Flag.BITMASK_UNREAD) > 0;
                iflags &= ~Flag.BITMASK_UNREAD;
                item.setTags(iflags, itags);
                if (mUnreadFlag.canTag(item))
                    item.alterUnread(iunread);
            }

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized MailItem copy(OperationContext octxt, int itemId, byte type, int folderId) throws ServiceException {
        return copy(octxt, new int[] { itemId }, type, folderId).get(0);
    }
    public synchronized List<MailItem> copy(OperationContext octxt, int[] itemIds, byte type, int folderId) throws ServiceException {
        short volumeId = Volume.getCurrentMessageVolume().getId();
        CopyItem redoRecorder = new CopyItem(mId, type, folderId, volumeId);

        boolean success = false;
        try {
            beginTransaction("copy", octxt, redoRecorder);
            CopyItem redoPlayer = (CopyItem) mCurrentChange.getRedoPlayer();

            List<MailItem> result = new ArrayList<MailItem>();

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items)
            checkItemChangeID(item);

            for (MailItem item : items) {
                int srcId = item.getId();
                int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(srcId));

                MailItem copy = item.copy(getFolderById(folderId), newId, item.getVolumeId() == -1 ? -1 : volumeId);
                redoRecorder.setDestId(srcId, newId);

                result.add(copy);
            }

            success = true;
            return result;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while copying items", e);
        } finally {
            endTransaction(success);
        }
    }

    public synchronized List<MailItem> imapCopy(OperationContext octxt, int[] itemIds, byte type, int folderId) throws IOException, ServiceException {
        // this is an IMAP command, so we'd better be tracking IMAP changes by now...
        beginTrackingImap();

        for (int id : itemIds)
            if (id <= 0)
                throw MailItem.noSuchItem(id, type);

        short volumeId = Volume.getCurrentMessageVolume().getId();
        ImapCopyItem redoRecorder = new ImapCopyItem(mId, type, folderId, volumeId);

        boolean success = false;
        try {
            beginTransaction("icopy", octxt, redoRecorder);
            ImapCopyItem redoPlayer = (ImapCopyItem) mCurrentChange.getRedoPlayer();

            Folder target = getFolderById(folderId);

            // fetch the items to copy and make sure the caller is up-to-date on change IDs
            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items)
                checkItemChangeID(item);

            List<MailItem> result = new ArrayList<MailItem>();

            for (MailItem item : items) {
                int srcId = item.getId();
                int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(srcId));

                MailItem copy = item.icopy(target, newId, item.getVolumeId() == -1 ? -1 : volumeId);
                redoRecorder.setDestId(srcId, newId);
    
                result.add(copy);
            }

            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }

    /** Moves an item from one folder into another in the same Mailbox.  The
     *  target folder may not be a {@link Mountpoint} or {@link SearchFolder}.
     *  To move an item between Mailboxes, you must do the copy by hand, then
     *  remove the original.
     *  
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on the source folder
     * @param octxt     The context for this request (e.g. auth user id).
     * @param itemId    The ID of the item to move.
     * @param type      The type of the item or {@link MailItem#TYPE_UNKNOWN}.
     * @param targetId  The ID of the target folder for the move. */
    public synchronized void move(OperationContext octxt, int itemId, byte type, int targetId) throws ServiceException {
        move(octxt, new int[] { itemId }, type, targetId, null);
    }

    /** Moves an item from one folder into another in the same Mailbox.  The
     *  target folder may not be a {@link Mountpoint} or {@link SearchFolder}.
     *  To move an item between Mailboxes, you must do the copy by hand, then
     *  remove the original.
     *  
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on the source folder
     * @param octxt     The context for this request (e.g. auth user id).
     * @param itemId    The ID of the item to move.
     * @param type      The type of the item or {@link MailItem#TYPE_UNKNOWN}.
     * @param targetId  The ID of the target folder for the move.
     * @param tcon      An optional constraint on the item being moved. */
    public synchronized void move(OperationContext octxt, int itemId, byte type, int targetId, TargetConstraint tcon) throws ServiceException {
        move(octxt, new int[] { itemId }, type, targetId, tcon);
    }

    /** Moves a set of items into a given folder in the same Mailbox.  The
     *  target folder may not be a {@link Mountpoint} or {@link SearchFolder}.
     *  To move items between Mailboxes, you must do the copy by hand, then
     *  remove the originals.
     *  
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on all the the source folders
     * @param octxt     The context for this request (e.g. auth user id).
     * @param itemId    A list of the IDs of the items to move.
     * @param type      The type of the items or {@link MailItem#TYPE_UNKNOWN}.
     * @param targetId  The ID of the target folder for the move.
     * @param tcon      An optional constraint on the items being moved. */
    public synchronized void move(OperationContext octxt, int[] itemIds, byte type, int targetId, TargetConstraint tcon) throws ServiceException {
        MoveItem redoRecorder = new MoveItem(mId, itemIds, type, targetId, tcon);

        boolean success = false;
        try {
            beginTransaction("move", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            Folder target = getFolderById(targetId);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items)
                checkItemChangeID(item);

            int oldUIDNEXT = target.getImapUIDNEXT();
            boolean resetUIDNEXT = false;

            for (MailItem item : items) {
                // do the move...
                boolean moved = item.move(target);
                
                // ...and determine whether the move needs to cause an UIDNEXT change
                if (moved && !resetUIDNEXT && isTrackingImap() && (item instanceof Conversation || item instanceof Message || item instanceof Contact))
                    resetUIDNEXT = true;
            }

            // if this operation should cause the target folder's UIDNEXT value to change but it hasn't yet, do it here
            if (resetUIDNEXT && oldUIDNEXT == target.getImapUIDNEXT()) {
                MoveItem redoPlayer = (MoveItem) mCurrentChange.getRedoPlayer();
                redoRecorder.setUIDNEXT(getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getUIDNEXT()));
                target.updateUIDNEXT();
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void rename(OperationContext octxt, int id, byte type, String name, int folderId) throws ServiceException {
        if (name != null && name.startsWith("/")) {
            rename(octxt, id, type, name);
            return;
        }

        name = StringUtil.stripControlCharacters(name);
        if (name == null || name.equals(""))
            throw ServiceException.INVALID_REQUEST("cannot set name to empty string", null);

        RenameItem redoRecorder = new RenameItem(mId, id, type, name, folderId);

        boolean success = false;
        try {
            beginTransaction("rename", octxt, redoRecorder);

            MailItem item = getItemById(id, type);
            checkItemChangeID(item);
            if (folderId <= 0)
                folderId = item.getFolderId();

            String oldName = item.getName();
            
            item.rename(name, getFolderById(folderId));

            if (item instanceof Tag) {
                mTagCache.remove(oldName.toLowerCase());
                mTagCache.put(name.toLowerCase(), (Tag) item);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void rename(OperationContext octxt, int id, byte type, String path) throws ServiceException {
        if (path == null || !path.startsWith("/")) {
            rename(octxt, id, type, path, ID_AUTO_INCREMENT);
            return;
        }

        RenameItemPath redoRecorder = new RenameItemPath(mId, id, type, path);

        boolean success = false;
        try {
            beginTransaction("renameFolderPath", octxt, redoRecorder);
            RenameItemPath redoPlayer = (RenameItemPath) mCurrentChange.getRedoPlayer();

            MailItem item = getItemById(id, type);
            Folder parent;
            checkItemChangeID(item);

            String[] parts = path.substring(1).split("/");
            if (parts.length == 0)
                throw MailServiceException.ALREADY_EXISTS(path);
            int[] recorderParentIds = new int[parts.length - 1];
            int[] playerParentIds = redoPlayer == null ? null : redoPlayer.getParentIds();
            if (playerParentIds != null && playerParentIds.length != recorderParentIds.length)
                throw ServiceException.FAILURE("incorrect number of path segments in redo player", null);

            parent = getFolderById(ID_FOLDER_USER_ROOT);
            for (int i = 0; i < parts.length - 1; i++) {
                String name = MailItem.validateItemName(parts[i]);
                int subfolderId = playerParentIds == null ? ID_AUTO_INCREMENT : playerParentIds[i];
                Folder subfolder = parent.findSubfolder(name);
                if (subfolder == null)
                    subfolder = Folder.create(getNextItemId(subfolderId), this, parent, name);
                else if (subfolderId != ID_AUTO_INCREMENT && subfolderId != subfolder.getId())
                    throw ServiceException.FAILURE("parent folder id changed since operation was recorded", null);
                else if (!subfolder.getName().equals(name) && subfolder.isMutable())
                    subfolder.rename(name, parent);
                recorderParentIds[i] = subfolder.getId();
                parent = subfolder;
            }
            redoRecorder.setParentIds(recorderParentIds);

            String name = parts[parts.length - 1];

            item.rename(name, parent);

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Deletes the <tt>MailItem</tt> with the given id.  Does nothing
     * if the <tt>MailItem</tt> doesn't exist.
     */
    public synchronized void delete(OperationContext octxt, int itemId, byte type) throws ServiceException {
        delete(octxt, new int[] { itemId }, type, null);
    }

    public synchronized void delete(OperationContext octxt, MailItem item, TargetConstraint tcon) throws ServiceException {
        delete(octxt, new int[] { item.getId() }, item.getType(), tcon);
    }

    /** Deletes the <tt>MailItem</tt> with the given id.  If there is no such
     *  <tt>MailItem</tt>, nothing happens and no error is generated.  If the
     *  id maps to an existing <tt>MailItem</tt> of an incompatible type,
     *  however, an error is thrown. */
    public synchronized void delete(OperationContext octxt, int itemId, byte type, TargetConstraint tcon) throws ServiceException {
        delete(octxt, new int[] { itemId }, type, tcon);
    }

    /** Deletes the <tt>MailItem</tt>s with the given id.  If there is no
     *  <tt>MailItem</tt> for a given id, that id is ignored.  If the id maps
     *  to an existing <tt>MailItem</tt> of an incompatible type, however,
     *  an error is thrown. */
    public synchronized void delete(OperationContext octxt, int[] itemIds, byte type, TargetConstraint tcon) throws ServiceException {
        DeleteItem redoRecorder = new DeleteItem(mId, itemIds, type, tcon);

        boolean success = false;
        try {
            beginTransaction("delete", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            for (int id : itemIds) {
                if (id == ID_AUTO_INCREMENT)
                    continue;

                MailItem item;
                try {
                    item = getItemById(id, MailItem.TYPE_UNKNOWN);
                } catch (NoSuchItemException nsie) {
                    // trying to delete nonexistent things is A-OK!
                    continue;
                }

                // however, trying to delete messages and passing in a folder ID is not OK
                if (!MailItem.isAcceptableType(type, item.getType()))
                    throw MailItem.noSuchItem(id, type);
                if (!checkItemChangeID(item) && item instanceof Tag)
                    throw MailServiceException.MODIFY_CONFLICT();

                // delete the item, but don't write the tombstone until we're finished...
                item.delete(MailItem.DeleteScope.ENTIRE_ITEM, false);
            }

            // collect all the tombstones and write once
            MailItem.TypedIdList tombstones = collectPendingTombstones();
            if (tombstones != null && !tombstones.isEmpty())
                    DbMailItem.writeTombstones(this, tombstones);

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    MailItem.TypedIdList collectPendingTombstones() {
        if (!isTrackingSync())
            return null;

        MailItem.TypedIdList tombstones = new MailItem.TypedIdList();
        for (Object obj : mCurrentChange.mOtherDirtyStuff)
            if (obj instanceof MailItem.PendingDelete)
                tombstones.add(((MailItem.PendingDelete) obj).itemIds);
        return tombstones;
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
            queueForIndexing(note, false, null);
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
            queueForIndexing(note, true, null);
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

    CalendarItem createCalendarItem(int folderId, short volumeId, int flags, long tags, String uid,
                                    ParsedMessage pm, Invite invite, long nextAlarm)
    throws ServiceException {
        OperationContext octxt = getOperationContext();
        Account authAccount = octxt != null ? octxt.getAuthenticatedUser() : null;
        boolean denyPrivateAccess = authAccount != null && !Account.allowPrivateAccess(authAccount, getAccount());
        boolean isCalendarResource = getAccount() instanceof CalendarResource;
        // Don't allow creating a private appointment on behalf of another user,
        // unless that other user is a calendar resource.
        if (denyPrivateAccess && !invite.isPublic() && !isCalendarResource)
            throw ServiceException.PERM_DENIED("private appointment/task cannot be created on behalf of another user");

        // FIXME: assuming that we're in the middle of a AddInvite op
        CreateCalendarItemPlayer redoPlayer = (CreateCalendarItemPlayer) mCurrentChange.getRedoPlayer();
        CreateCalendarItemRecorder redoRecorder = (CreateCalendarItemRecorder) mCurrentChange.getRedoRecorder();

        int newCalItemId = redoPlayer == null ? Mailbox.ID_AUTO_INCREMENT : redoPlayer.getCalendarItemId();
        int createId = getNextItemId(newCalItemId);

        CalendarItem calItem = CalendarItem.create(createId, getFolderById(folderId), volumeId, flags, tags,
                                                   uid, pm, invite, nextAlarm);

        if (redoRecorder != null)
            redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId(), calItem.getVolumeId());
        return calItem;
    }

    public Contact createContact(OperationContext octxt, ParsedContact pc, int folderId, String tags) throws ServiceException {
        return createContactInternal(octxt, pc.analyze(this), folderId, tags);
    }

    private synchronized Contact createContactInternal(OperationContext octxt, ParsedContact pc, int folderId, String tags) throws ServiceException {
        CreateContact redoRecorder = new CreateContact(mId, folderId, pc, tags);

        boolean success = false;
        try {
            beginTransaction("createContact", octxt, redoRecorder);
            CreateContact redoPlayer = (CreateContact) mCurrentChange.getRedoPlayer();
            boolean isRedo = redoPlayer != null;

            int contactId = getNextItemId(isRedo ? redoPlayer.getContactId() : ID_AUTO_INCREMENT);
            short volumeId = -1;
            if (pc.hasAttachment()) {
                if (isRedo)
                    volumeId = redoPlayer.getVolumeId();
                if (volumeId == -1)
                volumeId = Volume.getCurrentMessageVolume().getId();
            }

            Contact con = Contact.create(contactId, getFolderById(folderId), volumeId, pc, tags);

            if (pc.hasAttachment()) {
                try {
                    StoreManager sm = StoreManager.getInstance();
                    Blob blob = sm.storeIncoming(pc.getBlob(), pc.getDigest(), null, volumeId);
                    MailboxBlob mblob = sm.renameTo(blob, this, contactId, getOperationChangeID(), volumeId);

                    markOtherItemDirty(mblob);
                } catch (IOException ioe) {
                    throw ServiceException.FAILURE("could not save contact blob", ioe);
                }
            }

            redoRecorder.setContactId(con.getId());
            redoRecorder.setVolumeId(volumeId);
            queueForIndexing(con, false, pc);

            success = true;
            return con;
        } finally {
            endTransaction(success);
        }
    }

    public void modifyContact(OperationContext octxt, int contactId, ParsedContact pc) throws ServiceException {
        modifyContactInternal(octxt, contactId, pc.analyze(this));
    }

    public synchronized void modifyContactInternal(OperationContext octxt, int contactId, ParsedContact pc) throws ServiceException {
        ModifyContact redoRecorder = new ModifyContact(mId, contactId, pc);

        boolean success = false;
        try {
            beginTransaction("modifyContact", octxt, redoRecorder);
            ModifyContact redoPlayer = (ModifyContact) mCurrentChange.getRedoPlayer();

            Contact con = getContactById(contactId);
            if (!checkItemChangeID(con))
                throw MailServiceException.MODIFY_CONFLICT();

            short volumeId = -1;
            if (pc.hasAttachment()) {
                if (redoPlayer != null)
                    volumeId = redoPlayer.getVolumeId();
                if (volumeId == -1)
                    volumeId = Volume.getCurrentMessageVolume().getId();
            }

            try {
                if (pc.getBlob() == null && con.getDigest() == null)
                    con.setFields(pc);
                else
                    con.setContent(pc.getBlob(), pc.getDigest(), volumeId, pc);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("could not save contact blob", ioe);
            }

            redoRecorder.setVolumeId(volumeId);
            queueForIndexing(con, true, pc);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * @see #createFolder(OperationContext, String, int, byte, byteint, byte, String)
     */
    public synchronized Folder createFolder(OperationContext octxt, String name, int parentId, byte defaultView, int flags, byte color, String url)
    throws ServiceException {
		return createFolder(octxt, name, parentId, (byte)0, defaultView, flags, color, url);
	}

    public synchronized Folder createFolder(OperationContext octxt, String name, int parentId, byte attrs, byte defaultView, int flags, byte color, String url)
    throws ServiceException {
        CreateFolder redoRecorder = new CreateFolder(mId, name, parentId, attrs, defaultView, flags, color, url);

        boolean success = false;
        try {
            beginTransaction("createFolder", octxt, redoRecorder);
            CreateFolder redoPlayer = (CreateFolder) mCurrentChange.getRedoPlayer();

            int folderId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getFolderId());
            Folder folder = Folder.create(folderId, this, getFolderById(parentId), name, attrs, defaultView, flags, color, url);
            redoRecorder.setFolderId(folder.getId());
            success = true;
            return folder;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * @see #createFolder(OperationContext, String, int, byte, int, byte, String)
     */
    public synchronized Folder createFolder(OperationContext octxt, String path, byte attrs, byte defaultView) throws ServiceException {
        return createFolder(octxt, path, attrs, defaultView, 0, MailItem.DEFAULT_COLOR, null);
    }

    /**
     * Creates a folder.  Implicitly creates any parent folders in <tt>path</tt> if necessary.
     * 
     * @param octxt the operation context
     * @param path the slash-separated folder path
     * @param attrs the folder attributes, or <tt>0</tt> for the default attributes
     * @param defaultView the folder view, or <tt>0</tt> for the default view
     * @param flags the folder flags, or <tt>0</tt> for no flags
     * @param color the folder color, or {@link MailItem#DEFAULT_COLOR}
     * @param url the folder URL, or <tt>null</tt>
     * @return the new folder
     * @see Folder#getAttributes()
     * @see Folder#getDefaultView()
     * @see MailItem#getColor()
     * 
     * @throws ServiceException if the folder creation fails
     */
    public synchronized Folder createFolder(OperationContext octxt, String path, byte attrs, byte defaultView, int flags, byte color, String url)
    throws ServiceException {
        if (path == null)
            throw ServiceException.FAILURE("null path passed to Mailbox.createFolderPath", null);
        if (!path.startsWith("/"))
            path = '/' + path;
        if (path.endsWith("/") && path.length() > 1)
            path = path.substring(0, path.length() - 1);

        CreateFolderPath redoRecorder = new CreateFolderPath(mId, path, attrs, defaultView, flags, color, url);

        boolean success = false;
        try {
            beginTransaction("createFolderPath", octxt, redoRecorder);
            CreateFolderPath redoPlayer = (CreateFolderPath) mCurrentChange.getRedoPlayer();

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
                    subfolder = Folder.create(getNextItemId(folderId), this, folder, parts[i], (byte) 0,
                                              last ? defaultView : MailItem.TYPE_UNKNOWN, flags, color, last ? url : null);
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

    public synchronized void grantAccess(OperationContext octxt, int folderId, String grantee, byte granteeType, short rights, String args) throws ServiceException {
        GrantAccess redoPlayer = new GrantAccess(mId, folderId, grantee, granteeType, rights, args);

        boolean success = false;
        try {
            beginTransaction("grantAccess", octxt, redoPlayer);

            Folder folder = getFolderById(folderId);
            checkItemChangeID(folder);
            folder.grantAccess(grantee, granteeType, rights, args);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void revokeAccess(OperationContext octxt, int folderId, String grantee) throws ServiceException {
        RevokeAccess redoPlayer = new RevokeAccess(mId, folderId, grantee);

        boolean success = false;
        try {
            beginTransaction("revokeAccess", octxt, redoPlayer);

            Folder folder = getFolderById(folderId);
            checkItemChangeID(folder);
            folder.revokeAccess(grantee);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void setPermissions(OperationContext octxt, int folderId, ACL acl) throws ServiceException {
        SetPermissions redoPlayer = new SetPermissions(mId, folderId, acl);

        boolean success = false;
        try {
            beginTransaction("setPermissions", octxt, redoPlayer);

            Folder folder = getFolderById(folderId);
            checkItemChangeID(folder);
            folder.setPermissions(acl);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void setFolderUrl(OperationContext octxt, int folderId, String url) throws ServiceException {
        SetFolderUrl redoRecorder = new SetFolderUrl(mId, folderId, url);

        boolean success = false;
        try {
            beginTransaction("setFolderUrl", octxt, redoRecorder);

            Folder folder = getFolderById(folderId);
            checkItemChangeID(folder);
            folder.setUrl(url);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public synchronized void synchronizeFolder(OperationContext octxt, int folderId) throws ServiceException {
        Folder folder = getFolderById(octxt, folderId);
        if (!folder.getUrl().equals(""))
            importFeed(octxt, folderId, folder.getUrl(), true);
    }

    public synchronized void importFeed(OperationContext octxt, int folderId, String url, boolean subscription) throws ServiceException {
        if (url == null || url.equals(""))
            return;

        // get the remote data, skipping anything we've already seen (if applicable)
        Folder folder = getFolderById(octxt, folderId);
        Folder.SyncData fsd = subscription ? folder.getSyncData() : null;
        FeedManager.SubscriptionData sdata = FeedManager.retrieveRemoteDatasource(getAccount(), url, fsd);

        // clear out the folder if we're replacing the previous content
        if (subscription && (folder.getDefaultView() == MailItem.TYPE_APPOINTMENT || folder.getDefaultView() == MailItem.TYPE_TASK))
            emptyFolder(octxt, folderId, false);

        // if there's nothing to add, we can short-circuit here
        if (sdata.items.isEmpty())
            return;

        // disable modification conflict checks, as we've already wiped the folder and we may hit an appoinment >1 times
        OperationContext octxtNoConflicts = new OperationContext(octxt).unsetChangeConstraint();

        // add the newly-fetched items to the folder
        for (Object obj : sdata.items) {
            try {
                if (obj instanceof Invite)
                    addInvite(octxtNoConflicts, (Invite) obj, folderId, true, null);
                else if (obj instanceof ParsedMessage)
                    addMessage(octxtNoConflicts, (ParsedMessage) obj, folderId, true, Flag.BITMASK_UNREAD, null);
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException", e);
            }
        }

        // update the subscription to avoid downloading items twice
        if (subscription && sdata.lastDate > 0) {
            try {
                setSubscriptionData(octxt, folderId, sdata.lastDate, sdata.lastGuid);
            } catch (Exception e) {
                ZimbraLog.mailbox.warn("could not update feed metadata", e);
            }
        }
    }

    public synchronized void setSubscriptionData(OperationContext octxt, int folderId, long date, String guid) throws ServiceException {
        SetSubscriptionData redoRecorder = new SetSubscriptionData(mId, folderId, date, guid);

        boolean success = false;
        try {
            beginTransaction("setSubscriptionData", octxt, redoRecorder);
            getFolderById(folderId).setSubscriptionData(guid, date);
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

    public synchronized SearchFolder createSearchFolder(OperationContext octxt, int folderId, String name, String query, String types, String sort, byte color)
    throws ServiceException {
        CreateSavedSearch redoRecorder = new CreateSavedSearch(mId, folderId, name, query, types, sort, color);

        boolean success = false;
        try {
            beginTransaction("createSearchFolder", octxt, redoRecorder);
            CreateSavedSearch redoPlayer = (CreateSavedSearch) mCurrentChange.getRedoPlayer();

            int searchId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getSearchId());
            SearchFolder search = SearchFolder.create(searchId, getFolderById(folderId), name, query, types, sort, color);
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

    public synchronized Mountpoint createMountpoint(OperationContext octxt, int folderId, String name, String ownerId, int remoteId, byte view, int flags, byte color)
    throws ServiceException {
        CreateMountpoint redoRecorder = new CreateMountpoint(mId, folderId, name, ownerId, remoteId, view, flags, color);

        boolean success = false;
        try {
            beginTransaction("createMountpoint", octxt, redoRecorder);
            CreateMountpoint redoPlayer = (CreateMountpoint) mCurrentChange.getRedoPlayer();

            int mptId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getId());
            Mountpoint mpt = Mountpoint.create(mptId, getFolderById(folderId), name, ownerId, remoteId, view, flags, color);
            redoRecorder.setId(mpt.getId());
            success = true;
            return mpt;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * This is the nightly "delete old messages based on some criteria" thing that gets spawned
     * by a cron job.  It can hopefully be moved into a zmmailbox script at some point
     * 
     * Purge means 'delete forever' in this case.
     * 
     * @param octxt
     * @throws ServiceException
     */
    public synchronized void purgeMessages(OperationContext octxt) throws ServiceException {
        ZimbraLog.mailbox.info("Purging messages.");

        Account acct = getAccount();
        
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("System retention policy: Trash=%s, Junk=%s, All messages=%s",
                acct.getAttr(Provisioning.A_zimbraMailTrashLifetime),
                acct.getAttr(Provisioning.A_zimbraMailSpamLifetime),
                acct.getAttr(Provisioning.A_zimbraMailMessageLifetime));
            ZimbraLog.mailbox.debug("User-specified retention policy: Inbox read=%s, Inbox unread=%s, Sent=%s, Junk=%s, Trash=%s",
                acct.getAttr(Provisioning.A_zimbraPrefInboxReadLifetime),
                acct.getAttr(Provisioning.A_zimbraPrefInboxUnreadLifetime),
                acct.getAttr(Provisioning.A_zimbraPrefSentLifetime),
                acct.getAttr(Provisioning.A_zimbraPrefJunkLifetime),
                acct.getAttr(Provisioning.A_zimbraPrefTrashLifetime));
        }

        int globalTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailMessageLifetime, 0) / 1000);
        int systemTrashTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailTrashLifetime, 0) / 1000);
        int systemJunkTimeout  = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailSpamLifetime, 0) / 1000);

        int userInboxReadTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraPrefInboxReadLifetime, 0) / 1000);
        int userInboxUnreadTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraPrefInboxUnreadLifetime, 0) / 1000);
        int userTrashTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraPrefTrashLifetime, 0) / 1000);
        int userJunkTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraPrefJunkLifetime, 0) / 1000);
        int userSentTimeout = (int) (acct.getTimeInterval(Provisioning.A_zimbraPrefSentLifetime, 0) / 1000);

        int trashTimeout = pickTimeout(systemTrashTimeout, userTrashTimeout);
        int junkTimeout = pickTimeout(systemJunkTimeout, userJunkTimeout);

        if (globalTimeout <= 0 && trashTimeout <= 0 && junkTimeout <= 0 &&
            userInboxReadTimeout <= 0 && userInboxReadTimeout <= 0 &&
            userInboxUnreadTimeout <= 0 && userSentTimeout <= 0)
            // Nothing to do
            return;

        // sanity-check the really dangerous value...
        if (globalTimeout > 0 && globalTimeout < Constants.SECONDS_PER_MONTH) {
            // this min is also used by POP3 EXPIRE command. update Pop3Handler.MIN_EPXIRE_DAYS if it changes.
            ZimbraLog.mailbox.warn("global message timeout < 1 month; defaulting to 31 days");
            globalTimeout = Constants.SECONDS_PER_MONTH;
        }

        PurgeOldMessages redoRecorder = new PurgeOldMessages(mId);

        boolean success = false;
        try {
            beginTransaction("purgeMessages", octxt, redoRecorder);

            // get the folders we're going to be purging
            Folder trash = getFolderById(ID_FOLDER_TRASH);
            Folder junk  = getFolderById(ID_FOLDER_SPAM);
            Folder sent = getFolderById(ID_FOLDER_SENT);
            Folder inbox = getFolderById(ID_FOLDER_INBOX);

            if (globalTimeout > 0)
                Folder.purgeMessages(this, null, getOperationTimestamp() - globalTimeout, null);
            if (trashTimeout > 0)
                Folder.purgeMessages(this, trash, getOperationTimestamp() - trashTimeout, null);
            if (junkTimeout > 0)
                Folder.purgeMessages(this, junk, getOperationTimestamp() - junkTimeout, null);
            if (userInboxReadTimeout > 0)
                Folder.purgeMessages(this, inbox, getOperationTimestamp() - userInboxReadTimeout, false);
            if (userInboxUnreadTimeout > 0)
                Folder.purgeMessages(this, inbox, getOperationTimestamp() - userInboxUnreadTimeout, true);
            if (userSentTimeout > 0)
                Folder.purgeMessages(this, sent, getOperationTimestamp() - userSentTimeout, null);

            // collect all the tombstones and write once
            MailItem.TypedIdList tombstones = collectPendingTombstones();
            if (tombstones != null && !tombstones.isEmpty())
                DbMailItem.writeTombstones(this, tombstones);

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns the smaller non-zero value, or <tt>0</tt> if both
     *  <tt>t1</tt> and <tt>t2</tt> are <tt>0</tt>. */
    private int pickTimeout(int t1, int t2) {
        if (t1 == 0)
            return t2;
        if (t2 == 0)
            return t1;
        return Math.min(t1, t2);
    }

    public synchronized void purgeImapDeleted(OperationContext octxt) throws ServiceException {
        PurgeImapDeleted redoRecorder = new PurgeImapDeleted(mId);
        boolean success = false;
        try {
            beginTransaction("purgeImapDeleted", octxt, redoRecorder);

            Set<Folder> purgeable = getAccessibleFolders((short) (ACL.RIGHT_READ | ACL.RIGHT_DELETE));
            MailItem.PendingDelete info = DbMailItem.getImapDeleted(this, purgeable);
            MailItem.delete(this, info, null, MailItem.DeleteScope.ENTIRE_ITEM, true);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public Document addDocumentRevision(OperationContext octxt, int docId, byte type, byte[] rawData, String author) throws ServiceException {
        Document doc = getDocumentById(octxt, docId);
        ParsedDocument pd = new ParsedDocument(rawData, doc.getName(), doc.getContentType(), System.currentTimeMillis(), author);
        return addDocumentRevision(octxt, docId, type, pd);
    }

    public synchronized Document addDocumentRevision(OperationContext octxt, int docId, byte type, ParsedDocument pd) throws ServiceException {
        AddDocumentRevision redoRecorder = new AddDocumentRevision(mId, pd.getDigest(), pd.getSize(), 0);

        boolean success = false;
        try {
            beginTransaction("addDocumentRevision", octxt, redoRecorder);

            Document doc = getDocumentById(docId);

            redoRecorder.setAuthor(pd.getCreator());
            redoRecorder.setDocument(doc);
            short volumeId = Volume.getCurrentMessageVolume().getId();
            // TODO: simplify the redoRecorder by not subclassing from CreateMessage
            redoRecorder.setMessageBodyInfo(pd.getContent(), "", volumeId);

            doc.setContent(pd.getContent(), pd.getDigest(), volumeId, pd);
            queueForIndexing(doc, false, pd);

            success = true;
            return doc;
        } catch (IOException ioe) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(ioe);
        } finally {
            endTransaction(success);
        }
    }

    public WikiItem createWiki(OperationContext octxt, int folderId, String wikiword, String author, byte[] rawData)
    throws ServiceException {
        return (WikiItem) createDocument(octxt, folderId, wikiword, WikiItem.WIKI_CONTENT_TYPE, author, rawData, MailItem.TYPE_WIKI);
    }

    public Document createDocument(OperationContext octxt, int folderId, String filename, String mimeType, String author, byte[] rawData)
    throws ServiceException {
        return createDocument(octxt, folderId, filename, mimeType, author, rawData, MailItem.TYPE_DOCUMENT);
    }
    	
    public Document createDocument(OperationContext octxt, int folderId, String filename, String mimeType, String author, byte[] rawData, byte type)
    throws ServiceException {
        ParsedDocument pd = new ParsedDocument(rawData, filename, mimeType, System.currentTimeMillis(), author);
        return createDocument(octxt, folderId, pd, type);
    }

    public synchronized Document createDocument(OperationContext octxt, int folderId, ParsedDocument pd, byte type)
    throws ServiceException {
        boolean success = false;
        try {
            SaveDocument redoRecorder = new SaveDocument(mId, pd.getDigest(), pd.getSize(), folderId);

            beginTransaction("createDoc", octxt, redoRecorder);
            redoRecorder.setFilename(pd.getFilename());
            redoRecorder.setMimeType(pd.getContentType());
            redoRecorder.setAuthor(pd.getCreator());
            redoRecorder.setItemType(type);
            
            SaveDocument redoPlayer = (octxt == null ? null : (SaveDocument) octxt.getPlayer());
            int itemId  = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());
            short volumeId = redoPlayer == null ? Volume.getCurrentMessageVolume().getId() : redoPlayer.getVolumeId();

            redoRecorder.setMessageBodyInfo(pd.getContent(), "", volumeId);

            Document doc;
            if (type == MailItem.TYPE_DOCUMENT)
                doc = Document.create(itemId, getFolderById(folderId), volumeId, pd.getFilename(), pd.getContentType(), pd);
            else if (type == MailItem.TYPE_WIKI)
                doc = WikiItem.create(itemId, getFolderById(folderId), volumeId, pd.getFilename(), pd);
            else
                throw MailServiceException.INVALID_TYPE(type);

            redoRecorder.setMessageId(doc.getId());
            doc.setContent(pd.getContent(), pd.getDigest(), volumeId, pd);

            queueForIndexing(doc, false, pd);
            success = true;
            return doc;
        } catch (IOException ioe) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(ioe);
        } finally {
            endTransaction(success);
        }
    }
    
    public Message updateOrCreateChat(OperationContext octxt, ParsedMessage pm, int id) throws IOException, ServiceException {
        // make sure the message has been analzyed before taking the Mailbox lock
        pm.analyze();
        try {
            pm.getRawData();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
        // special-case saving a new Chat
        if (id == ID_AUTO_INCREMENT) {
            return createChat(octxt, pm, ID_FOLDER_IM_LOGS, Flag.BITMASK_FROM_ME, null);
        } else {
            Message toRet = updateChat(octxt, pm, id);
            return toRet;
        }
    }
    
    public synchronized Chat createChat(OperationContext octxt, ParsedMessage pm, 
                int folderId, int flags, String tagsStr) throws IOException, ServiceException {
        if (pm == null)
            throw ServiceException.INVALID_REQUEST("null ParsedMessage when adding chat to mailbox " + mId, null);
        
        byte[] data;
        String digest;
        int msgSize;
        Chat chat = null;
        try {
            data = pm.getRawData();  
            digest = pm.getRawDigest();  
            msgSize = pm.getRawSize();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
        
        CreateChat redoRecorder = new CreateChat(mId, digest, msgSize, folderId, flags, tagsStr);
        StoreManager sm = StoreManager.getInstance();
        boolean success = false;
        Blob blob = null;
        
        try {
            beginTransaction("createChat", octxt, redoRecorder);

            long   tags    = Tag.tagsToBitmask(tagsStr);
            CreateChat redoPlayer = (octxt == null ? null : (CreateChat) octxt.getPlayer());
            int itemId  = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());
            short volumeId = redoPlayer == null ? Volume.getCurrentMessageVolume().getId() : redoPlayer.getVolumeId();
            blob = sm.storeIncoming(data, digest, null, volumeId);
            redoRecorder.setMessageBodyInfo(data, blob.getPath(), blob.getVolumeId());
            markOtherItemDirty(blob);
            chat = Chat.create(itemId, getFolderById(folderId), pm, msgSize, digest, volumeId, false, flags, tags);
            redoRecorder.setMessageId(chat.getId());
            sm.link(blob, this, itemId, chat.getSavedSequence(), chat.getVolumeId());
            queueForIndexing(chat, false, pm);
            success = true;
        } finally {
            endTransaction(success);
        }
        return chat;
    }
    
    public synchronized Chat updateChat(OperationContext octxt, ParsedMessage pm, int id) throws IOException, ServiceException {
        byte[] data;
        String digest;
        int size;
        try {
            data = pm.getRawData();  
            digest = pm.getRawDigest();  
            size = pm.getRawSize();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
        
        SaveChat redoRecorder = new SaveChat(mId, id, digest, size, -1, 0, null);
        boolean success = false;
        try {
            beginTransaction("saveChat", octxt, redoRecorder);
            SaveChat redoPlayer = (SaveChat) mCurrentChange.getRedoPlayer();
            Chat chat = (Chat)getItemById(id, MailItem.TYPE_CHAT);
            
            if (!chat.isMutable()) 
                throw MailServiceException.IMMUTABLE_OBJECT(id);
            if (!checkItemChangeID(chat))
                throw MailServiceException.MODIFY_CONFLICT();

            // content changed, so we're obliged to change the IMAP uid
            int imapID = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapId());
            redoRecorder.setImapId(imapID);

            short volumeId = redoPlayer == null ? Volume.getCurrentMessageVolume().getId() : redoPlayer.getVolumeId();

            // update the content and increment the revision number
            Blob blob = chat.setContent(data, digest, volumeId, pm);
            redoRecorder.setMessageBodyInfo(data, blob.getPath(), blob.getVolumeId());

            // NOTE: msg is now uncached (will this cause problems during commit/reindex?)
            queueForIndexing(chat, true, pm);
            success = true;
            return chat;
        } finally {
            endTransaction(success);
        }
        
        
    }


    // Coordinate other conflicting operations (such as backup) and shared delivery, delivery of a message to
    // multiple recipients.  Such operation on a mailbox and shared delivery
    // are mutually exclusive.  More precisely, the op may not begin
    // when there is a shared delivery in progress for the mailbox.
    // Delivery of a shared message to the mailbox must be denied and
    // deferred when the mailbox is being operated on or has a request
    // for such op pending.
    private static class SharedDeliveryCoordinator {
        public int mNumDelivs;
        public boolean mSharedDeliveryAllowed;
        public SharedDeliveryCoordinator() {
            mNumDelivs = 0;
            mSharedDeliveryAllowed = true;
        }
    }

    private SharedDeliveryCoordinator mSharedDelivCoord =
        new SharedDeliveryCoordinator();

    /**
     * Puts mailbox in shared delivery mode.  A shared delivery is delivery of
     * a message to multiple recipients.  Conflicting op on mailbox is disallowed
     * while mailbox is in shared delivery mode.  (See bug 2187)
     * Conversely, a shared delivery may not start on a mailbox that is
     * currently being operated on or when there is a pending op request.
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
        synchronized (mSharedDelivCoord) {
            assert(mSharedDelivCoord.mNumDelivs >= 0);
            if (mSharedDelivCoord.mSharedDeliveryAllowed) {
                mSharedDelivCoord.mNumDelivs++;
                if (ZimbraLog.mailbox.isDebugEnabled()) {
                    ZimbraLog.mailbox.debug("# of shared deliv incr to " + mSharedDelivCoord.mNumDelivs +
                                " for mailbox " + getId());
                }
                return true;
            } else {
                // If request for other ops is pending on this mailbox, don't allow
                // any more shared deliveries from starting.
                return false;
            }
        }
    }

    /**
     * @see com.zimbra.cs.mailbox.Mailbox#beginSharedDelivery()
     */
    public void endSharedDelivery() {
        synchronized (mSharedDelivCoord) {
            mSharedDelivCoord.mNumDelivs--;
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                ZimbraLog.mailbox.debug("# of shared deliv decr to " + mSharedDelivCoord.mNumDelivs +
                            " for mailbox " + getId());
            }
            assert(mSharedDelivCoord.mNumDelivs >= 0);
            if (mSharedDelivCoord.mNumDelivs == 0) {
                // Wake up any waiting backup thread.
                mSharedDelivCoord.notifyAll();
            }
        }
    }

    /**
     * Turns shared delivery on/off.  If turning off, waits until the op can begin,
     * i.e. until all currently ongoing shared deliveries finish.  A thread
     * turning shared delivery off must turn it on at the end of the operation, otherwise
     * no further shared deliveries are possible to the mailbox.
     * @param onoff
     */
    public void setSharedDeliveryAllowed(boolean onoff) {
        synchronized (mSharedDelivCoord) {
            if (onoff) {
                // allow shared delivery
                mSharedDelivCoord.mSharedDeliveryAllowed = true;
            } else {
                // disallow shared delivery
                mSharedDelivCoord.mSharedDeliveryAllowed = false;
            }
            mSharedDelivCoord.notifyAll();
        }
    }

    /**
     * Wait until shared delivery is completed on this mailbox.  Other conflicting ops may begin when
     * there is no shared delivery in progress.  Call setSharedDeliveryAllowed(false)
     * before calling this method.
     *
     */
    public void waitUntilSharedDeliveryCompletes() {
        synchronized (mSharedDelivCoord) {
            while (mSharedDelivCoord.mNumDelivs > 0) {
                try {
                    mSharedDelivCoord.wait(3000);
                    ZimbraLog.misc.info("wake up from wait for completion of shared delivery; mailbox=" + getId() + 
                                " # of shared deliv=" + mSharedDelivCoord.mNumDelivs);
                } catch (InterruptedException e) {}
            }
        }
    }

    /**
     * Tests whether shared delivery is completed on this mailbox.  Other conflicting ops may begin when
     * there is no shared delivery in progress.
     */
    public boolean isSharedDeliveryComplete() {
        synchronized (mSharedDelivCoord) {
            return mSharedDelivCoord.mNumDelivs < 1;
        }
    }


    /**
     * Be very careful when changing code in this method.  The order of almost
     * every line of code is important to ensure correct redo logging and crash
     * recovery.
     * @param success
     * @throws ServiceException
     */
    synchronized void endTransaction(boolean success) throws ServiceException {
        if (!mCurrentChange.isActive()) {
            // would like to throw here, but it might cover another exception...
            ZimbraLog.mailbox.warn("cannot end a transaction when not inside a transaction", new Exception());
            return;
        }
        if (!mCurrentChange.endChange())
            return;

        Connection conn = mCurrentChange.conn;
        ServiceException exception = null;

        // update mailbox size and folder unread/message counts
        if (success) {
            try {
                snapshotCounts();
            } catch (ServiceException e) {
                exception = e;
                success = false;
            }
        }

        // Failure case is very simple.  Just rollback the database and cache
        // and return.  We haven't logged anything to the redo log for this
        // transaction, so no redo cleanup is necessary.
        if (!success) {
            if (conn != null)
                DbPool.quietRollback(conn);
            rollbackCache(mCurrentChange);
            if (exception != null)
                throw exception;
            return;
        }

        boolean needRedo = true;
        if (mCurrentChange.octxt != null)
            needRedo = mCurrentChange.octxt.needRedo();
        RedoableOp redoRecorder = mCurrentChange.recorder;
        IndexItem indexRedo = null;
        Map<MailItem, MailboxChange.IndexItemEntry> itemsToIndex = mCurrentChange.indexItems;
        boolean indexingNeeded = !itemsToIndex.isEmpty() && !DebugConfig.disableIndexing;

        // 1. Log the change redo record for main transaction.
        //    If indexing is to be followed, log this entry
        //    without requiring fsync, because logging for
        //    indexing entry will do fsync, which will fsync
        //    this entry at the same time.
        if (redoRecorder != null && needRedo)
            redoRecorder.log(!indexingNeeded);

        boolean allGood = false;
        try {
            if (indexingNeeded) {
                
                // See bug 15072 - we need to clear mCurrentChange.indexItems (it is stored in a temporary)
                // here, just in case item.reindex() recurses into a new transaction...
                mCurrentChange.indexItems = new HashMap<MailItem, MailboxChange.IndexItemEntry>();
                
                for (Map.Entry<MailItem, MailboxChange.IndexItemEntry> entry : itemsToIndex.entrySet()) {
                    MailItem item = entry.getKey();

                    if (needRedo) {
                        indexRedo = new IndexItem(mId, item.getId(), item.getType(), entry.getValue().mDeleteFirst);
                        indexRedo.start(getOperationTimestampMillis());
                        indexRedo.setParentOp(redoRecorder);
                    }

                    // 2. Index the item before committing the main
                    // transaction.  This allows us to fail the entire
                    // transaction when indexing fails.  Write the change
                    // record for indexing only after indexing actually
                    // works.
                    item.reindex(indexRedo, entry.getValue().mDeleteFirst, entry.getValue().mData);

                    // 3. Write the change redo record for indexing
                    //    sub-transaction to guarantee that it appears in the
                    //    redo log stream before the commit record for main
                    //    transaction.  If main transaction commit record is
                    //    written first and the server crashes before writing
                    //    the indexing change record, we won't be able to
                    //    re-execute indexing during crash recovery, and we will
                    //    end up with an unindexed item.
                    if (needRedo)
                        indexRedo.log();
                }
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
                if (needRedo) {
                    if (indexRedo != null)
                        indexRedo.abort();
                    if (redoRecorder != null)
                        redoRecorder.abort();
                }
                if (conn != null)
                    DbPool.quietRollback(conn);
                rollbackCache(mCurrentChange);
            }
        }

        if (allGood) {
            if (needRedo) {
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
                if (redoRecorder != null) {
                    
                    if (mCurrentChange.mDirty != null && mCurrentChange.mDirty.changedTypes != 0) {
                        // if an "all accounts" waitset is active, and this change has an appropriate type,
                        // then we'll need to set a commit-callback
                        AllAccountsRedoCommitCallback cb =
                            AllAccountsRedoCommitCallback.getRedoCallbackIfNecessary(getAccountId(), mCurrentChange.mDirty.changedTypes);
                        if (cb != null) {
                            redoRecorder.setCommitCallback(cb);
                        }
                    }
                    redoRecorder.commit();
                }
    
                // 6. The commit redo record for indexing sub-transaction is
                //    written in batch by another thread.  To avoid the batch
                //    commit thread's writing commit-index before this thread's
                //    writing commit-main (step 5 above), the index redo object
                //    is initialized to block the commit attempt by default.
                //    At this point we've written the commit-main record, so
                //    unblock the commit on indexing.
                if (indexRedo != null)
                    indexRedo.allowCommit();
            }

            // 7. We are finally done with database and redo commits.
            //    Cache update comes last.
            commitCache(mCurrentChange);
        }
    }

    public static final int NON_DELIVERY_FLAGS = Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME | Flag.BITMASK_COPIED | Flag.BITMASK_DELETED;

    void snapshotCounts() throws ServiceException {
        // for write ops, update the "new messages" count in the DB appropriately
        RedoableOp recorder = mCurrentChange.recorder;
        if (recorder != null && mCurrentChange.getRedoPlayer() == null) {
            boolean isNewMessage = recorder.getOpCode() == RedoableOp.OP_CREATE_MESSAGE;
            if (isNewMessage) {
                CreateMessage cm = (CreateMessage) recorder;
                if (cm.getFolderId() == ID_FOLDER_SPAM || cm.getFolderId() == ID_FOLDER_TRASH)
                    isNewMessage = false;
                else if ((cm.getFlags() & NON_DELIVERY_FLAGS) != 0)
                    isNewMessage = false;
                else if (mCurrentChange.octxt != null && mCurrentChange.octxt.getSession() != null && !mCurrentChange.octxt.isDelegatedRequest(this))
                    isNewMessage = false;
            }
            boolean isSoapRequest = mCurrentChange.octxt != null && mCurrentChange.octxt.getSession() instanceof SoapSession;
            if (isNewMessage)
                mCurrentChange.recent = mData.recentMessages + 1;
            else if (isSoapRequest && mData.recentMessages != 0)
                mCurrentChange.recent = 0;
        }

        if (mCurrentChange.recent != MailboxChange.NO_CHANGE || mCurrentChange.size != MailboxChange.NO_CHANGE || mCurrentChange.contacts != MailboxChange.NO_CHANGE)
            DbMailbox.updateMailboxStats(this);

        if (mCurrentChange.mDirty != null && mCurrentChange.mDirty.hasNotifications()) {
            if (mCurrentChange.mDirty.created != null) {
                for (MailItem item : mCurrentChange.mDirty.created.values()) {
                    if (item instanceof Folder && item.getSize() != 0)
                        ((Folder) item).saveFolderCounts(false);
                    else if (item instanceof Tag && item.isUnread())
                        ((Tag) item).saveTagCounts();
                }
            }

            if (mCurrentChange.mDirty.modified != null) {
                for (Change change : mCurrentChange.mDirty.modified.values()) {
                    if ((change.why & (Change.MODIFIED_UNREAD | Change.MODIFIED_SIZE)) != 0 && change.what instanceof Folder)
                        ((Folder) change.what).saveFolderCounts(false);
                    else if ((change.why & Change.MODIFIED_UNREAD) != 0 && change.what instanceof Tag)
                        ((Tag) change.what).saveTagCounts();
                }
            }
        }
    }

    private void commitCache(MailboxChange change) {
        assert(Thread.holdsLock(this));
        if (change == null)
            return;

        // save for notifications (below)
        PendingModifications dirty = null;
        if (change.mDirty != null && change.mDirty.hasNotifications()) {
            dirty = change.mDirty;
            change.mDirty = new PendingModifications();
        }

        Session source = mCurrentChange.octxt == null ? null : mCurrentChange.octxt.getSession();

        try {
            // the mailbox data has changed, so commit the changes
            if (change.sync != null)
                mData.trackSync = change.sync;
            if (change.imap != null)
                mData.trackImap = change.imap;
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
            if (change.accessed != MailboxChange.NO_CHANGE)
                mData.lastWriteDate = change.accessed;
            if (change.recent != MailboxChange.NO_CHANGE)
                mData.recentMessages = change.recent;
            if (change.config != null) {
                if (change.config.getSecond() == null) {
                    if (mData.configKeys != null)
                        mData.configKeys.remove(change.config.getFirst());
                } else {
                    if (mData.configKeys == null)
                        mData.configKeys = new HashSet<String>(1);
                    mData.configKeys.add(change.config.getFirst());
                }
            }

            // accumulate all the info about deleted items; don't care about committed changes to external items
            MailItem.PendingDelete deleted = null;
            for (Object obj : change.mOtherDirtyStuff)
                if (obj instanceof MailItem.PendingDelete)
                    deleted = ((MailItem.PendingDelete) obj).add(deleted);

            // delete any index entries associated with items deleted from db
            if (deleted != null && deleted.indexIds != null && deleted.indexIds.size() > 0 && mMailboxIndex != null) {
                try {
                    int[] indexIds = new int[deleted.indexIds.size()];
                    for (int i = 0; i < deleted.indexIds.size(); i++)
                        indexIds[i] = deleted.indexIds.get(i);
                    int[] deletedIds = mMailboxIndex.deleteDocuments(indexIds);
                    if (deletedIds != indexIds)
                        ZimbraLog.mailbox.warn("could not delete all index entries for items: " + deleted.itemIds.getAll());
                } catch (IOException e) {
                    ZimbraLog.mailbox.warn("ignoring error while deleting index entries for items: " + deleted.itemIds.getAll(), e);
                }
            }
                
            // delete any blobs associated with items deleted from db/index
            StoreManager sm = StoreManager.getInstance();
            if (deleted != null && deleted.blobs != null) {
                for (MailboxBlob blob : deleted.blobs) {
                    try {
                        if (blob != null)
                        sm.delete(blob);
                    } catch (IOException e) {
                        ZimbraLog.mailbox.warn("could not delete blob " + blob.getPath() + " during commit");
                    }
            }
            }
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during cache commit", e);
        } finally {
            // keep our MailItem cache at a reasonable size
            trimItemCache();
            // make sure we're ready for the next change
            change.reset();
        }

        // committed changes, so notify any listeners
        if (!mListeners.isEmpty() && dirty != null && dirty.hasNotifications()) {
            for (Session session : new ArrayList<Session>(mListeners)) {
                try {
                    session.notifyPendingChanges(dirty, mData.lastChangeId, source);
                } catch (RuntimeException e) {
                    ZimbraLog.mailbox.error("ignoring error during notification", e);
                }
        }
    }
    }

    private void rollbackCache(MailboxChange change) {
        if (change == null)
            return;

        try {
            // rolling back changes, so purge dirty items from the various caches
            Map cache = change.itemCache;
            for (Map map : new Map[] {change.mDirty.created, change.mDirty.deleted, change.mDirty.modified}) {
                if (map != null) {
                    for (Object obj : map.values()) {
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
                }
            }

            // roll back any changes to external items
            // FIXME: handle mOtherDirtyStuff:
            //    - LeafNodeInfo (re-index all un-indexed files)
            //    - MailboxBlob  (delink/remove new file)
            //    - String       (remove from mConvHashes map)
            StoreManager sm = StoreManager.getInstance();
            for (Object obj : change.mOtherDirtyStuff) {
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
                } else if (obj instanceof String && obj != null) {
                    mConvHashes.remove(obj);
                }
            }
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during cache rollback", e);
        } finally {
            // keep our MailItem cache at a reasonable size
            trimItemCache();
            // toss any pending changes to the Mailbox object and get ready for the next change
            change.reset();
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

    private void logCacheActivity(Integer key, byte type, MailItem item) {
        // The global item cache counter always gets updated
        if (!isCachedType(type)) {
            ZimbraPerf.COUNTER_MBOX_ITEM_CACHE.increment(item == null ? 0 : 1);
        }

        // The per-access log only gets updated when cache or perf debug logging
        // is on
        if (!ZimbraLog.cache.isDebugEnabled())
            return;

        if (item == null) {
            ZimbraLog.cache.debug("Cache miss for item " + key + " in mailbox " + getId());
            return;
        }

        // Don't log cache hits for folders, search folders and tags.  We always
        // keep these in memory, so cache hits are not interesting.
        if (isCachedType(type))
            return;
        ZimbraLog.cache.debug("Cache hit for " + MailItem.getNameForType(type) + " " + key + " in mailbox " + getId());
    }

    private static final String CN_ID         = "id";
    private static final String CN_ACCOUNT_ID = "account_id";
    private static final String CN_NEXT_ID    = "next_item_id";
    private static final String CN_SIZE       = "size";

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mailbox: {");
        sb.append(CN_ID).append(": ").append(mId).append(", ");
        sb.append(CN_ACCOUNT_ID).append(": ").append(mData.accountId).append(", ");
        sb.append(CN_NEXT_ID).append(": ").append(mData.lastItemId).append(", ");
        sb.append(CN_SIZE).append(": ").append(mData.size);
        sb.append("}");
        return sb.toString();
    }
}
