/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.mime.Rfc822ValidationInputStream;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.BufferStream;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.CopyInputStream;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailItem.QueryParams;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.fb.LocalFreeBusyProvider;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.DomainBrowseTerm;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.CalendarItem.Callback;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.FoldersTagsCache.FoldersTags;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailItem.PendingDelete;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.MailboxListener.ChangeNotification;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.mailbox.Tag.NormalizedTags;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.cache.CalSummaryCache.CalendarDataResult;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.mailbox.calendar.tzfixup.TimeZoneFixupRules;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.ParsedMessage.CalendarPartInfo;
import com.zimbra.cs.mime.ParsedMessageDataSource;
import com.zimbra.cs.mime.ParsedMessageOptions;
import com.zimbra.cs.pop3.Pop3Message;
import com.zimbra.cs.redolog.op.*;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FeedManager;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.service.util.SpamHandler.SpamReport;
import com.zimbra.cs.session.AllAccountsRedoCommitCallback;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.MailboxBlobDataSource;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;

/**
 * @since Jun 13, 2004
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
    @Deprecated
    public static final int ID_FOLDER_NOTEBOOK  = 12;      // no longer created in new mailboxes since Helix (bug 39647).  old mailboxes may still contain a system folder with id 12
    public static final int ID_FOLDER_AUTO_CONTACTS = 13;
    public static final int ID_FOLDER_IM_LOGS   = 14;
    public static final int ID_FOLDER_TASKS     = 15;
    public static final int ID_FOLDER_BRIEFCASE = 16;
    public static final int ID_FOLDER_COMMENTS  = 17;
    public static final int ID_FOLDER_PROFILE   = 18;

    public static final int HIGHEST_SYSTEM_ID = 18;
    public static final int FIRST_USER_ID     = 256;

    public static final class MailboxData implements Cloneable {
        public int id;
        public int schemaGroupId;
        public String accountId;
        public long size;
        public int contacts;
        public short indexVolumeId;
        public int lastBackupDate;
        public int lastItemId;
        public int lastChangeId;
        public long lastChangeDate;
        public int lastWriteDate;
        public int recentMessages;
        public int trackSync;
        public boolean trackImap;
        public Set<String> configKeys;
        public MailboxVersion version;

        @Override
        protected MailboxData clone() {
            MailboxData mbd = new MailboxData();
            mbd.id = id;
            mbd.schemaGroupId = schemaGroupId;
            mbd.accountId = accountId;
            mbd.size = size;
            mbd.contacts = contacts;
            mbd.indexVolumeId = indexVolumeId;
            mbd.lastItemId = lastItemId;
            mbd.lastChangeId  = lastChangeId;
            mbd.lastChangeDate = lastChangeDate;
            mbd.lastWriteDate = lastWriteDate;
            mbd.recentMessages = recentMessages;
            mbd.trackSync = trackSync;
            mbd.trackImap = trackImap;
            if (configKeys != null) {
                mbd.configKeys = new HashSet<String>(configKeys);
            }
            mbd.version = version;
            return mbd;
        }
    }

    static final class IndexItemEntry {
        final List<IndexDocument> documents;
        final MailItem item;

        IndexItemEntry(MailItem item, List<IndexDocument> docs) {
            this.item = item;
            this.documents = docs;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("id", item.getId()).toString();
        }
    }

    private final class MailboxChange {
        private static final int NO_CHANGE = -1;

        long timestamp = System.currentTimeMillis();
        int depth = 0;
        boolean active;
        DbConnection conn = null;
        RedoableOp recorder = null;
        List<IndexItemEntry> indexItems = new ArrayList<IndexItemEntry>();
        Map<Integer, MailItem> itemCache = null;
        OperationContext octxt = null;
        TargetConstraint tcon  = null;

        Integer sync = null;
        Boolean imap = null;
        long size = NO_CHANGE;
        int itemId = NO_CHANGE;
        int changeId = NO_CHANGE;
        int contacts = NO_CHANGE;
        int accessed = NO_CHANGE;
        int recent = NO_CHANGE;
        Pair<String, Metadata> config = null;

        PendingModifications dirty = new PendingModifications();
        final List<Object> otherDirtyStuff = new LinkedList<Object>();
        PendingDelete deletes = null;

        MailboxChange()  { }

        MailboxOperation getOperation() {
            if (recorder != null)
                return recorder.getOperation();
            return null;
        }

        void setTimestamp(long millis) {
            if (depth == 1)
                timestamp = millis;
        }

        void startChange(String caller, OperationContext ctxt, RedoableOp op) {
            active = true;
            if (depth++ == 0) {
                octxt = ctxt;
                recorder = op;
                ZimbraLog.mailbox.debug("beginning operation: %s", caller);
            } else {
                ZimbraLog.mailbox.debug("  increasing stack depth to %d (%s)", depth, caller);
            }
        }

        boolean endChange() {
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                if (depth <= 1) {
                    ZimbraLog.mailbox.debug("ending operation %s",
                            recorder == null ? "" : recorder.getClass().getSimpleName());
                } else {
                    ZimbraLog.mailbox.debug("  decreasing stack depth to %d", depth - 1);
                }
            }
            return (--depth == 0);
        }

        boolean isActive() {
            return active;
        }

        DbConnection getConnection() throws ServiceException {
            if (conn == null) {
                conn = DbPool.getConnection(Mailbox.this);
                ZimbraLog.mailbox.debug("  fetching new DB connection");
            }
            return conn;
        }

        RedoableOp getRedoPlayer() {
            return (octxt == null ? null : octxt.getPlayer());
        }

        RedoableOp getRedoRecorder() {
            return recorder;
        }

        /**
         * Add an item to the list of things to be indexed at the end of the current transaction
         */
        void addIndexItem(IndexItemEntry item) {
            indexItems.add(item);
        }

        void addPendingDelete(PendingDelete info) {
            if (deletes == null) {
                deletes = info;
            } else {
                deletes.add(info);
            }
        }

        boolean isMailboxRowDirty(MailboxData data) {
            if (recent != NO_CHANGE || size != NO_CHANGE || contacts != NO_CHANGE)
                return true;
            if (itemId != NO_CHANGE && itemId / DbMailbox.ITEM_CHECKPOINT_INCREMENT > data.lastItemId / DbMailbox.ITEM_CHECKPOINT_INCREMENT)
                return true;
            if (changeId != NO_CHANGE && changeId / DbMailbox.CHANGE_CHECKPOINT_INCREMENT > data.lastChangeId / DbMailbox.CHANGE_CHECKPOINT_INCREMENT)
                return true;
            return false;
        }

        void reset() {
            DbPool.quietClose(conn);
            this.active = false;
            this.conn = null;
            this.octxt = null;
            this.tcon = null;
            this.depth = 0;
            this.size = NO_CHANGE;
            this.changeId = NO_CHANGE;
            this.itemId = NO_CHANGE;
            this.contacts = NO_CHANGE;
            this.accessed = NO_CHANGE;
            this.recent = NO_CHANGE;
            this.sync = null;
            this.config = null;
            this.deletes = null;
            this.itemCache = null;
            this.indexItems.clear();
            this.dirty.clear();
            this.otherDirtyStuff.clear();

            ZimbraLog.mailbox.debug("clearing change");
        }
    }

    // This class handles all the indexing internals for the Mailbox
    public final MailboxIndex index;
    public final MailboxLock lock = new MailboxLock();

    // TODO: figure out correct caching strategy
    private static final int MAX_ITEM_CACHE_WITH_LISTENERS    = LC.zimbra_mailbox_active_cache.intValue();
    private static final int MAX_ITEM_CACHE_WITHOUT_LISTENERS = LC.zimbra_mailbox_inactive_cache.intValue();
    private static final int MAX_MSGID_CACHE = 10;

    private int           mId;
    private MailboxData   mData;
    private final MailboxChange currentChange = new MailboxChange();
    private List<Session> mListeners = new CopyOnWriteArrayList<Session>();

    private Map<Integer, Folder> mFolderCache;
    private Map<Object, Tag>     mTagCache;
    private SoftReference<Map<Integer, MailItem>> mItemCache = new SoftReference<Map<Integer, MailItem>>(null);
    private Map <String, Integer> mConvHashes     = MapUtil.newLruMap(MAX_MSGID_CACHE);
    private Map <String, Integer> mSentMessageIDs = MapUtil.newLruMap(MAX_MSGID_CACHE);

    private MailboxMaintenance maintenance;
    private IMPersona persona;
    private volatile boolean open = false;

    protected Mailbox(MailboxData data) {
        mId = data.id;
        mData = data;
        mData.lastChangeDate = System.currentTimeMillis();
        index = new MailboxIndex(this);
        // version init done in finishInitialization()
        // index init done in finishInitialization()
    }

    boolean isOpen() {
        return open;
    }

    /**
     * Called by the MailboxManager before returning the mailbox, this function makes sure the Mailbox is ready to use
     * (index initialized, version check, etc etc).
     * <p>
     * Any mailbox-open steps that require I/O should be done in this API and not in the Mailbox constructor since the
     * Mailbox constructor can conceivably be run twice in a race (even though the MailboxManager makes sure only one
     * instance of a particular mailbox "wins").
     *
     * @return TRUE if we did some work (this was the mailbox's first open) or FALSE if mailbox was already opened
     * @throws ServiceException
     */
    boolean open() throws ServiceException {
        if (open) { // already opened
            return false;
        }

        lock.lock();
        try {
            if (open) { // double checked locking
                return false;
            }

            index.open(); // init the index

            if (mData.version == null) {
                // if we've just initialized() the mailbox, then the version will
                // be set in the config, but it won't be comitted yet -- and unfortunately
                // getConfig() won't return us the proper value in this case....so just
                // use the local mVersion value if it is already set (that's the case
                // if we are initializing a new mailbox) and otherwise we'll read the
                // mailbox version from config
                Metadata md = getConfig(null, MailboxVersion.MD_CONFIG_VERSION);
                mData.version = MailboxVersion.fromMetadata(md);
            }

            // sanity-check the mailbox version
            if (mData.version.tooHigh()) {
                throw ServiceException.FAILURE("invalid mailbox version: " + mData.version + " (too high)", null);
            }

            if (!mData.version.atLeast(MailboxVersion.CURRENT)) { // check for mailbox upgrade
                if (!mData.version.atLeast(1, 2)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 1.2", getVersion());
                    index.upgradeMailboxTo1_2();
                }

                // same prescription for both the 1.2 -> 1.3 and 1.3 -> 1.4 migrations
                if (!mData.version.atLeast(1, 4)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 1.4", getVersion());
                    recalculateFolderAndTagCounts();
                    updateVersion(new MailboxVersion((short) 1, (short) 4));
                }

                if (!mData.version.atLeast(1, 5)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 1.5", getVersion());
                    index.indexAllDeferredFlagItems();
                }

                // bug 41893: revert folder colors back to mapped value
                if (!mData.version.atLeast(1, 7)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 1.7", getVersion());
                    MailboxUpgrade.upgradeTo1_7(this);
                    updateVersion(new MailboxVersion((short) 1, (short) 7));
                }

                // bug 41850: revert tag colors back to mapped value
                if (!mData.version.atLeast(1, 8)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 1.8", getVersion());
                    MailboxUpgrade.upgradeTo1_8(this);
                    updateVersion(new MailboxVersion((short) 1, (short) 8));
                }

                // bug 20620: track \Deleted counts separately
                if (!mData.version.atLeast(1, 9)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 1.9", getVersion());
                    purgeImapDeleted(null);
                    updateVersion(new MailboxVersion((short) 1, (short) 9));
                }

                // bug 39647: wiki to document migration
                if (!mData.version.atLeast(1, 10)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 1.10", getVersion());
                    // update the version first so that the same mailbox
                    // don't have to go through the migration again
                    // if it was called to open() during the migration.
                    updateVersion(new MailboxVersion((short) 1, (short) 10));
                    migrateWikiFolders();
                }

                if (!mData.version.atLeast(2, 0)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 2.0", getVersion());
                    MailboxUpgrade.upgradeTo2_0(this);
                    updateVersion(new MailboxVersion((short) 2, (short) 0));
                }

                // TAG and TAGGED_ITEM migration
                if (!mData.version.atLeast(2, 1)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 2.1", getVersion());
                    MailboxUpgrade.upgradeTo2_1(this);
                    updateVersion(new MailboxVersion((short) 2, (short) 1));
                }

                // mailbox version in ZIMBRA.MAILBOX table
                if (!mData.version.atLeast(2, 2)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 2.2", getVersion());
                    // writing the new version itself performs the upgrade!
                    updateVersion(new MailboxVersion((short) 2, (short) 2));
                }
            }

            // done!
            return open = true;
        } finally {
            lock.release();
        }
    }

    /** Returns the server-local numeric ID for this mailbox.  To get a
     *  system-wide, persistent unique identifier for the mailbox, use
     *  {@link #getAccountId()}. */
    public int getId() {
        return mId;
    }

    /** Returns which MBOXGROUP<N> database this mailbox is homed in. */
    public int getSchemaGroupId() {
        return mData.schemaGroupId;
    }

    /** Returns the ID of this mailbox's Account.
     *
     * @see #getAccount() */
    public String getAccountId() {
        return mData.accountId;
    }

    /** Returns the {@link Account} object for this mailbox's owner.  At
     *  present, each account can have at most one <tt>Mailbox</tt>.
     *
     * @throws AccountServiceException if no account exists */
    public Account getAccount() throws ServiceException {
        String accountId = getAccountId();
        Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (acct == null) {
            ZimbraLog.mailbox.warn("no account found in directory for mailbox %d (was expecting %s)", mId, accountId);
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        }
        return acct;
    }

    public IMPersona getPersona() throws ServiceException {
        lock.lock();
        try {
            if (persona == null) {
                persona = IMPersona.loadPersona(this);
            }
            return persona;
        } finally {
            lock.release();
        }
    }

    /** Retuns the ID of the volume this <tt>Mailbox</tt>'s index lives on. */
    public short getIndexVolume() {
        return mData.indexVolumeId;
    }

    /**
     * Returns the mailbox maintenance context if this mailbox is under maintenance, otherwise null.
     */
    public MailboxMaintenance getMaintenance() {
        return maintenance;
    }

    /** Returns a {@link MailSender} object that can be used to send mail,
     *  save a copy to the Sent folder, etc. */
    public MailSender getMailSender() throws ServiceException {
        MailSender sender = new MailSender();
        sender.setTrackBadHosts(true);
        Account account = getAccount();
        Domain domain = Provisioning.getInstance().getDomain(account);

        // Get the SMTP host list in random order.
        List<String> hosts = new ArrayList<String>();
        hosts.addAll(JMSession.getSmtpHosts(domain));
        if (hosts.size() > 1) {
            Collections.shuffle(hosts);
        }

        try {
            // Set the SMTP session properties on MailSender, so
            // that we don't require the caller to set them on
            // the message.
            sender.setSession(JMSession.getSmtpSession(account), hosts);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to get SMTP session for " + account, e);
        }

        return sender;
    }


    /** Returns the list of all <code>Mailbox</code> listeners of a given type.
     *  Returns all listeners when the passed-in type is <tt>null</tt>. */
    public List<Session> getListeners(Session.Type stype) {
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

        for (Session s : mListeners) {
            if (s.getType() == stype)
                return true;
        }
        return false;
    }

    /** Loookup a {@link Session} in the set of listeners on this mailbox. */
    public Session getListener(String sessionId) {
        if (sessionId != null) {
            for (Session session : mListeners) {
                if (sessionId.equals(session.getSessionId()))
                    return session;
            }
        }
        return null;
    }

    /** Adds a {@link Session} to the set of listeners notified on Mailbox
     *  changes.
     *
     * @param session  The Session registering for notifications.
     * @throws ServiceException  If the mailbox is in maintenance mode. */
    public void addListener(Session session) throws ServiceException {
        if (session == null) {
            return;
        }
        assert(session.getSessionId() != null);
        lock.lock();
        try {
            if (maintenance != null) {
                throw MailServiceException.MAINTENANCE(mId);
            }
            if (!mListeners.contains(session)) {
                mListeners.add(session);
            }
        } finally {
            lock.release();
        }
        ZimbraLog.mailbox.debug("adding listener: %s", session);
    }

    /** Removes a {@link Session} from the set of listeners notified on
     *  Mailbox changes.
     *
     * @param session  The listener to deregister for notifications. */
    public void removeListener(Session session) {
        mListeners.remove(session);

        if (ZimbraLog.mailbox.isDebugEnabled())
            ZimbraLog.mailbox.debug("clearing listener: " + session);
    }

    /** Cleans up and disconnects all {@link Session}s listening for
     *  notifications on this Mailbox.
     *
     * @see SessionCache#clearSession(Session) */
    private void purgeListeners() {
        ZimbraLog.mailbox.debug("purging listeners");

        if (persona != null) {
            persona.purgeListeners(); // do this BEFORE purgeListeners
        }
        for (Session session : mListeners) {
            SessionCache.clearSession(session);
        }
        // this may be redundant, as Session.doCleanup should dequeue
        //   the listener, but empty the list here just to be sure
        mListeners.clear();
    }

    /** Returns whether the server is keeping track of message deletes
     *  (etc.) for sync clients.  By default, sync tracking is off.
     *
     * @see #getSyncCutoff
     * @see #beginTrackingSync */
    boolean isTrackingSync() {
        return getSyncCutoff() > 0;
    }

    /** Returns the smallest change number that can be used as a sync token.
     *  If sync tracking is off, returns {@code 0}.
     *
     * @see #beginTrackingSync */
    public int getSyncCutoff() {
        return currentChange.sync == null ? mData.trackSync : currentChange.sync;
    }

    /** Returns whether the server is keeping track of message moves
     *  for imap clients.  By default, imap tracking is off.
     *
     * @see #beginTrackingImap */
    public boolean isTrackingImap() {
        return currentChange.imap == null ? mData.trackImap : currentChange.imap;
    }

    /** Returns the operation timestamp as a UNIX int with 1-second
     *  resolution.  This time is set at the start of the Mailbox
     *  transaction and should match the <tt>long</tt> returned
     *  by {@link #getOperationTimestampMillis}. */
    public int getOperationTimestamp() {
        return (int) (currentChange.timestamp / 1000L);
    }

    /** Returns the operation timestamp as a Java long with full
     *  millisecond resolution.  This time is set at the start of
     *  the Mailbox transaction and should match the <tt>int</tt>
     *  returned by {@link #getOperationTimestamp}. */
    public long getOperationTimestampMillis() {
        return currentChange.timestamp;
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
        return currentChange.changeId == MailboxChange.NO_CHANGE ?
                mData.lastChangeId : Math.max(mData.lastChangeId, currentChange.changeId);
    }

    private void setOperationChangeID(int changeFromRedo) throws ServiceException {
        if (currentChange.changeId != MailboxChange.NO_CHANGE) {
            if (currentChange.changeId == changeFromRedo) {
                return;
            }
            throw ServiceException.FAILURE("cannot specify change ID after change is in progress", null);
        }

        int lastId = getLastChangeID();
        int nextId = (changeFromRedo == ID_AUTO_INCREMENT ? lastId + 1 : changeFromRedo);

        // need to keep the current change ID regardless of whether it's a highwater mark
        currentChange.changeId = nextId;
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
        if (currentChange.changeId == MailboxChange.NO_CHANGE) {
            setOperationChangeID(ID_AUTO_INCREMENT);
        }
        return currentChange.changeId;
    }

    /** @return whether the object has changed more recently than the client knows about */
    boolean checkItemChangeID(MailItem item) throws ServiceException {
        if (item == null) {
            return true;
        }
        return checkItemChangeID(item.getModifiedSequence(), item.getSavedSequence());
    }

    public boolean checkItemChangeID(int modMetadata, int modContent) throws ServiceException {
        if (currentChange.octxt == null || currentChange.octxt.change < 0) {
            return true;
        }
        OperationContext octxt = currentChange.octxt;
        if (octxt.changetype == OperationContext.CHECK_CREATED && modContent > octxt.change) {
            return false;
        } else if (octxt.changetype == OperationContext.CHECK_MODIFIED && modMetadata > octxt.change) {
            throw MailServiceException.MODIFY_CONFLICT();
        }
        return true;
    }

    /** Returns the last id assigned to an item successfully created in the
     *  mailbox.  On startup, this value will be rounded up to the nearest
     *  100, so there may be gaps in the set of IDs actually assigned.
     *
     * @see MailItem#getId()
     * @see DbMailbox#ITEM_CHECKPOINT_INCREMENT */
    public int getLastItemId() {
        return currentChange.itemId == MailboxChange.NO_CHANGE ? mData.lastItemId : currentChange.itemId;
    }

    // Don't make this method package-visible.  Keep it private.
    //   idFromRedo: specific ID value to use during redo execution, or ID_AUTO_INCREMENT
    private int getNextItemId(int idFromRedo) {
        int lastId = getLastItemId();
        int nextId = idFromRedo == ID_AUTO_INCREMENT ? lastId + 1 : idFromRedo;

        if (nextId > lastId) {
            currentChange.itemId = nextId;
        }
        return nextId;
    }

    TargetConstraint getOperationTargetConstraint() {
        return currentChange.tcon;
    }

    void setOperationTargetConstraint(TargetConstraint tcon) {
        currentChange.tcon = tcon;
    }

    public OperationContext getOperationContext() {
        return currentChange.active ? currentChange.octxt : null;
    }

    RedoableOp getRedoPlayer() {
        return currentChange.getRedoPlayer();
    }

    RedoableOp getRedoRecorder() {
        return currentChange.recorder;
    }

    PendingModifications getPendingModifications() {
        return currentChange.dirty;
    }


    /** Returns the {@link Account} for the authenticated user for the
     *  transaction.  Returns <tt>null</tt> if none was supplied in the
     *  transaction's {@link OperationContext} or if the authenticated
     *  user is the same as the <tt>Mailbox</tt>'s owner. */
    Account getAuthenticatedAccount() {
        Account authuser = null;
        if (currentChange.active && currentChange.octxt != null) {
            authuser = currentChange.octxt.getAuthenticatedUser();
        }
        // XXX if the current authenticated user is the owner, it will return null.
        // later on in Folder.checkRights(), the same assumption is used to validate
        // the access.
        if (authuser != null && authuser.getId().equals(getAccountId())) {
            authuser = null;
        }
        return authuser;
    }

    /** Returns whether the authenticated user for the transaction is using
     *  any admin privileges they might have.  Admin users not using privileges
     *  are exactly like any other user and cannot access any folder they have
     *  not explicitly been granted access to.
     *
     * @see #getAuthenticatedAccount() */
    boolean isUsingAdminPrivileges() {
        return currentChange.active && currentChange.octxt != null && currentChange.octxt.isUsingAdminPrivileges();
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
        if (authuser == null || getAccountId().equals(authuser.getId())) {
            return true;
        }
        if (currentChange.active && currentChange.octxt != null) {
            return AccessManager.getInstance().canAccessAccount(authuser, getAccount(), isUsingAdminPrivileges());
        }
        return false;
    }

    /** Returns whether the authenticated user in the given op context has full access to this
     *  <tt>Mailbox</tt>.   The following users have full access:<ul>
     *    <li>the mailbox's owner
     *    <li>all global admin accounts (if using admin privileges)
     *    <li>appropriate domain admins (if using admin privileges)</ul> */
    public boolean hasFullAccess(OperationContext octxt) throws ServiceException {
        Account authuser = octxt != null ? octxt.getAuthenticatedUser() : null;
        // XXX: in Mailbox, authuser is set to null if authuser == owner.
        if (authuser == null || getAccountId().equals(authuser.getId())) {
            return true;
        }
        return AccessManager.getInstance().canAccessAccount(authuser, getAccount(), octxt.isUsingAdminPrivileges());
    }

    /** Returns true if the authenticated user has admin privileges over this
     * <tt>Mailbox</tt>.  Such users include:<ul>
     *   <li>all global admin accounts (if using admin privileges)</li>
     *   <li>appropriate domain admins (if using admin privileges)</li></ul> */
    public boolean hasFullAdminAccess(OperationContext octxt) throws ServiceException {
        return octxt == null || (octxt.isUsingAdminPrivileges() && hasFullAccess(octxt));
    }

    /** Returns the total (uncompressed) size of the mailbox's contents. */
    public long getSize() {
        return currentChange.size == MailboxChange.NO_CHANGE ? mData.size : currentChange.size;
    }

    /** change the current size of the mailbox */
    void updateSize(long delta) throws ServiceException {
        updateSize(delta, true);
    }

    void updateSize(long delta, boolean checkQuota) throws ServiceException {
        if (delta == 0)
            return;

        // if we go negative, that's OK!  just pretend we're at 0.
        long size = Math.max(0,
                (currentChange.size == MailboxChange.NO_CHANGE ? mData.size : currentChange.size) + delta);
        if (delta > 0 && checkQuota) {
            checkSizeChange(size);
        }

        currentChange.dirty.recordModified(currentChange.getOperation(), this, Change.SIZE, currentChange.timestamp);
        currentChange.size = size;
    }

    void checkSizeChange(long newSize) throws ServiceException {
        long quota = AccountUtil.getEffectiveQuota(getAccount());
        if (quota != 0 && newSize > quota) {
            throw MailServiceException.QUOTA_EXCEEDED(quota);
        }
    }

    /** Returns the last time that the mailbox had a write op caused by a SOAP
     *  session.  This value is written both right after the session's first
     *  write op as well as right after the session expires.
     *
     * @see #recordLastSoapAccessTime(long) */
    public long getLastSoapAccessTime() {
        lock.lock();
        try {
            long lastAccess = (currentChange.accessed == MailboxChange.NO_CHANGE ?
                    mData.lastWriteDate : currentChange.accessed) * 1000L;
            for (Session s : mListeners) {
                if (s instanceof SoapSession) {
                    lastAccess = Math.max(lastAccess, ((SoapSession) s).getLastWriteAccessTime());
                }
            }
            return lastAccess;
        } finally {
            lock.release();
        }
    }

    /** Records the last time that the mailbox had a write op caused by a SOAP
     *  session.  This value is written both right after the session's first
     *  write op as well as right after the session expires.
     *
     * @see #getLastSoapAccessTime() */
    public void recordLastSoapAccessTime(long time) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("recordLastSoapAccessTime", null);
            if (time > mData.lastWriteDate) {
                currentChange.accessed = (int) (time / 1000);
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
        return currentChange.recent == MailboxChange.NO_CHANGE ? mData.recentMessages : currentChange.recent;
    }

    /** Resets the mailbox's "recent message count" to 0.  A message is
     *  considered "recent" if (a) it's not a draft or a sent message, and
     *  (b) it was added since the last write operation associated with any
     *  SOAP session. */
    public void resetRecentMessageCount(OperationContext octxt) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("resetRecentMessageCount", octxt);
            if (getRecentMessageCount() != 0) {
                currentChange.recent = 0;
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns the number of contacts currently in the mailbox.
     *
     * @see #updateContactCount(int) */
    public int getContactCount() {
        return currentChange.contacts == MailboxChange.NO_CHANGE ? mData.contacts : currentChange.contacts;
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
        currentChange.contacts = Math.max(0, (currentChange.contacts == MailboxChange.NO_CHANGE ?
                mData.contacts : currentChange.contacts) + delta);

        if (delta < 0)
            return;
        int quota = getAccount().getContactMaxNumEntries();
        if (quota != 0 && currentChange.contacts > quota) {
            throw MailServiceException.TOO_MANY_CONTACTS(quota);
        }
    }


    /** Adds the item to the current change's list of items created during
     *  the transaction.
     * @param item  The created item. */
    void markItemCreated(MailItem item) {
        currentChange.dirty.recordCreated(item);
    }

    /** Adds the item to the current change's list of items deleted during the transaction.
     *
     * @param item  The item being deleted. */
    void markItemDeleted(MailItem item) {
        MailItem itemSnapshot = null;
        try {
            itemSnapshot = item.snapshotItem();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("could not snapshot to-be-deleted item", e);
        }
        if (itemSnapshot == null) {
            markItemDeleted(item.getType(), item.getId());
        } else {
            currentChange.dirty.recordDeleted(itemSnapshot, currentChange.getOperation(), currentChange.timestamp);
        }
    }

    /** Adds the item to the current change's list of items deleted during the transaction.
     *
     * @param type item type
     * @param itemId  The id of the item being deleted. */
    void markItemDeleted(MailItem.Type type, int itemId) {
        currentChange.dirty.recordDeleted(mData.accountId, itemId, type, currentChange.getOperation(), currentChange.timestamp);
    }

    /** Adds the items to the current change's list of items deleted during the transaction.
     *
     * @param idlist  The ids of the items being deleted. */
    void markItemDeleted(TypedIdList idlist) {
        currentChange.dirty.recordDeleted(mData.accountId, idlist, currentChange.getOperation(), currentChange.timestamp);
    }

    /** Adds the item to the current change's list of items modified during
     *  the transaction.
     *
     * @param item    The modified item.
     * @param reason  The bitmask describing the modified item properties.
     * @param preModifyItemSnapshot
     * @see com.zimbra.cs.session.PendingModifications.Change */
    void markItemModified(MailItem item, int reason, MailItem preModifyItemSnapshot) throws ServiceException {
        if (item.inDumpster()) {
            throw MailServiceException.IMMUTABLE_OBJECT(item.getId());
        }
        currentChange.dirty.recordModified(currentChange.getOperation(), item, reason, currentChange.timestamp, preModifyItemSnapshot);
    }

    /** Returns whether the given item has been marked dirty for a particular
     *  reason during the current transaction.  Outside of a transaction, this
     *  method will return {@code false}.
     * @param item  The {@code MailItem} we're interested in.
     * @param how   A bitmask of {@link Change} "why" flags (e.g. {@code
     *              Change#MODIFIED_FLAGS}).
     * @see Change */
    public boolean isItemModified(MailItem item, int how) {
        PendingModifications dirty = currentChange.dirty;
        if (!dirty.hasNotifications()) {
            return false;
        }
        PendingModifications.ModificationKey mkey = new PendingModifications.ModificationKey(item);
        if (dirty.created != null && dirty.created.containsKey(mkey)) {
            return true;
        }
        Change chg = dirty.modified == null ? null : dirty.modified.get(mkey);
        return chg != null && (chg.why & how) != 0;
    }

    /** Adds the object to the current change's list of non-{@link MailItem}
     *  objects affected during the transaction.  Among these "dirty" items
     *  can be:<ul>
     *    <li>The {@link Blob} or {@link MailboxBlob} for a newly-created file.
     *    <li>The {@link PendingDelete} holding blobs and index
     *        entries to be cleaned up after a {@link MailItem#deletes}.
     *    <li>The SHA1 hash of a conversation's subject stored in
     *        {@link #mConvHashes}.</ul>
     *
     * @param obj  The relevant object.
     * @see #commitCache(Mailbox.MailboxChange)
     * @see #rollbackCache(Mailbox.MailboxChange) */
    void markOtherItemDirty(Object obj) {
        if (obj instanceof PendingDelete) {
            currentChange.addPendingDelete((PendingDelete) obj);
        } else {
            currentChange.otherDirtyStuff.add(obj);
        }
    }

    public DbConnection getOperationConnection() throws ServiceException {
        lock.lock();
        try {
            if (!currentChange.isActive()) {
                throw ServiceException.FAILURE("cannot fetch Connection outside transaction", new Exception());
            }
            return currentChange.getConnection();
        } finally {
            lock.release();
        }
    }

    private void setOperationConnection(DbConnection conn) throws ServiceException {
        lock.lock();
        try {
            if (!currentChange.isActive()) {
                throw ServiceException.FAILURE("cannot set Connection outside transaction", new Exception());
            } else if (conn == null) {
                return;
            } else if (currentChange.conn != null) {
                throw ServiceException.FAILURE("cannot set Connection for in-progress transaction", new Exception());
            }
            currentChange.conn = conn;
        } finally {
            lock.release();
        }
    }

    /**
     * Puts the Mailbox into maintenance mode.  As a side effect, disconnects any {@link Session}s listening on this
     * {@link Mailbox} and flushes all changes to the search index of this {@link Mailbox}.
     *
     * @return A new {@link MailboxManager.MaintenanceContext} token for use in a subsequent call to
     * {@link MailboxManager#endMaintenance(MailboxManager.MaintenanceContext, boolean, boolean)}.
     * @throws ServiceException MailServiceException.MAINTENANCE if the {@link Mailbox} is already in maintenance mode.
     */
    MailboxMaintenance beginMaintenance() throws ServiceException {
        lock.lock();
        try {
            if (maintenance != null) {
                throw MailServiceException.MAINTENANCE(mId);
            }
            ZimbraLog.mailbox.info("Putting mailbox %d under maintenance.", getId());

            purgeListeners();
            index.evict();

            maintenance = new MailboxMaintenance(mData.accountId, mId, this);
            return maintenance;
        } finally {
            lock.release();
        }
    }

    void endMaintenance(boolean success) throws ServiceException {
        lock.lock();
        try {
            if (maintenance == null) {
                throw ServiceException.FAILURE("mainbox not in maintenance mode", null);
            }
            if (success) {
                ZimbraLog.mailbox.info("Ending maintenance on mailbox %d.", getId());
                maintenance = null;
            } else {
                ZimbraLog.mailbox.info("Ending maintenance and marking mailbox %d as unavailable.", getId());
                maintenance.markUnavailable();
            }
        } finally {
            lock.release();
        }
    }

    boolean isTransactionActive() {
        return currentChange.depth > 0;
    }

    protected void beginTransaction(String caller, OperationContext octxt) throws ServiceException {
        beginTransaction(caller, System.currentTimeMillis(), octxt, null, null);
    }

    protected void beginTransaction(String caller, OperationContext octxt, RedoableOp recorder) throws ServiceException {
        long timestamp = octxt == null ? System.currentTimeMillis() : octxt.getTimestamp();
        beginTransaction(caller, timestamp, octxt, recorder, null);
    }

    void beginTransaction(String caller, OperationContext octxt, RedoableOp recorder, DbConnection conn) throws ServiceException {
        long timestamp = octxt == null ? System.currentTimeMillis() : octxt.getTimestamp();
        beginTransaction(caller, timestamp, octxt, recorder, conn);
    }

    private void beginTransaction(String caller, long time, OperationContext octxt, RedoableOp recorder, DbConnection conn) throws ServiceException {
        assert !Thread.holdsLock(this) : "use MailboxLock";
        lock.lock();
        currentChange.startChange(caller, octxt, recorder);

        // if a Connection object was provided, use it
        if (conn != null) {
            setOperationConnection(conn);
        }

        boolean needRedo = needRedo(octxt);
        // have a single, consistent timestamp for anything affected by this operation
        currentChange.setTimestamp(time);
        if (recorder != null && needRedo) {
            recorder.start(time);
        }

        // if the caller has specified a constraint on the range of affected items, store it
        if (recorder != null && needRedo && octxt != null && octxt.change > 0) {
            recorder.setChangeConstraint(octxt.changetype, octxt.change);
        }

        // if we're redoing an op, preserve the old change ID
        if (octxt != null && octxt.getChangeId() > 0) {
            setOperationChangeID(octxt.getChangeId());
        }
        if (recorder != null && needRedo) {
            recorder.setChangeId(getOperationChangeID());
        }

        // keep a hard reference to the item cache to avoid having it GCed during the op
        Map<Integer, MailItem> cache = mItemCache.get();
        if (cache == null) {
            cache = new LinkedHashMap<Integer, MailItem>(MAX_ITEM_CACHE_WITH_LISTENERS, (float) 0.75, true);
            mItemCache = new SoftReference<Map<Integer, MailItem>>(cache);
            ZimbraLog.cache.debug("created a new MailItem cache for mailbox " + getId());
        }
        currentChange.itemCache = cache;

        // don't permit mailbox access during maintenance
        if (maintenance != null && !maintenance.canAccess()) {
            throw MailServiceException.MAINTENANCE(mId);
        }
        // we can only start a redoable operation as the transaction's base change
        if (recorder != null && needRedo && currentChange.depth > 1) {
            throw ServiceException.FAILURE("cannot start a logged transaction from within another transaction " +
                    "(current recorder=" + currentChange.recorder + ")", null);
        }
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
    public Metadata getConfig(OperationContext octxt, String section) throws ServiceException {
        if (Strings.isNullOrEmpty(section)) {
            return null;
        }

        // note: defaulting to true, not false...
        boolean success = true;
        try {
            beginTransaction("getConfig", octxt, null);
            // make sure they have sufficient rights to view the config
            if (!hasFullAccess()) {
                return null;
            }
            if (mData.configKeys == null || !mData.configKeys.contains(section)) {
                return null;
            }
            String config = DbMailbox.getConfig(this, section);
            if (config == null) {
                return null;
            }
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
    public void setConfig(OperationContext octxt, String section, Metadata config) throws ServiceException {
        Preconditions.checkNotNull(section);

        SetConfig redoPlayer = new SetConfig(mId, section, config);
        boolean success = false;
        try {
            beginTransaction("setConfig", octxt, redoPlayer);

            // make sure they have sufficient rights to view the config
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
            currentChange.dirty.recordModified(currentChange.getOperation(), this, Change.CONFIG, currentChange.timestamp);
            currentChange.config = new Pair<String,Metadata>(section, config);
            DbMailbox.updateConfig(this, section, config);
            success = true;
        } finally {
            endTransaction(success);
        }
    }


    private Map<Integer, MailItem> getItemCache() throws ServiceException {
        if (!currentChange.isActive()) {
            throw ServiceException.FAILURE("cannot access item cache outside a transaction active=" +
                    currentChange.active + ",depth=" + currentChange.depth, null);
        }
        return currentChange.itemCache;
    }

    private void clearItemCache() {
        if (currentChange.isActive()) {
            currentChange.itemCache.clear();
        } else {
            mItemCache.clear();
        }
    }

    void cache(MailItem item) throws ServiceException {
        if (item == null || item.isTagged(Flag.FlagInfo.UNCACHED))
            return;

        if (item instanceof Tag) {
            if (mTagCache != null) {
                mTagCache.put(item.getId(), (Tag) item);
                mTagCache.put(item.getName().toLowerCase(), (Tag) item);
            }
        } else if (item instanceof Folder) {
            if (mFolderCache != null) {
                mFolderCache.put(item.getId(), (Folder) item);
            }
        } else {
            getItemCache().put(item.getId(), item);
        }

        ZimbraLog.cache.debug("cached %s %d in mailbox %d", item.getType(), item.getId(), getId());
    }

    protected void uncache(MailItem item) throws ServiceException {
        if (item == null)
            return;

        if (item instanceof Tag) {
            if (mTagCache == null)
                return;
            mTagCache.remove(item.getId());
            mTagCache.remove(item.getName().toLowerCase());
        } else if (item instanceof Folder) {
            if (mFolderCache == null)
                return;
            mFolderCache.remove(item.getId());
        } else {
            getItemCache().remove(item.getId());
            MessageCache.purge(item);
        }

        ZimbraLog.cache.debug("uncached %s %d in mailbox %d", item.getType(), item.getId(), getId());

        uncacheChildren(item);
    }

    /** Removes an item from the <code>Mailbox</code>'s item cache.  If the
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
        if (item != null) {
            MessageCache.purge(item);
            uncacheChildren(item);
        } else {
            MessageCache.purge(this, itemId);
        }
    }

    /** Removes all this item's children from the <code>Mailbox</code>'s cache.
     *  Does not uncache the item itself. */
    void uncacheChildren(MailItem parent) throws ServiceException {
        if (parent == null || !parent.canHaveChildren())
            return;

        Collection<? extends MailItem> cached;
        if (!(parent instanceof Folder)) {
            cached = getItemCache().values();
        } else if (mFolderCache != null) {
            cached = mFolderCache.values();
        } else {
            return;
        }

        int parentId = parent.getId();
        List<MailItem> children = new ArrayList<MailItem>();
        for (MailItem item : cached) {
            if (item.getParentId() == parentId) {
                children.add(item);
            }
        }

        if (!children.isEmpty()) {
            for (MailItem child : children) {
                uncache(child);
            }
        }
    }

    /** Removes all items of a specified type from the <tt>Mailbox</tt>'s
     *  caches.  There may be some collateral damage: purging non-tag,
     *  non-folder types will drop the entire item cache.
     *
     * @param type  The type of item to completely uncache.  {@link MailItem#TYPE_UNKNOWN}
     * uncaches all items. */
    public void purge(MailItem.Type type) {
        lock.lock();
        try {
            switch (type) {
                case FOLDER:
                case MOUNTPOINT:
                case SEARCHFOLDER:
                    mFolderCache = null;
                    break;
                case FLAG:
                case TAG:
                    mTagCache = null;
                    break;
                default:
                    clearItemCache();
                    break;
                case UNKNOWN:
                    mFolderCache = null;
                    mTagCache = null;
                    clearItemCache();
                    break;
            }
        } finally {
            lock.release();
        }

        ZimbraLog.cache.debug("purged type=%s", type);
    }

    public static final Set<Integer> REIFIED_FLAGS = ImmutableSet.of(
            Flag.ID_FROM_ME, Flag.ID_ATTACHED, Flag.ID_REPLIED, Flag.ID_FORWARDED,
            Flag.ID_COPIED, Flag.ID_FLAGGED, Flag.ID_DRAFT, Flag.ID_DELETED,
            Flag.ID_NOTIFIED, Flag.ID_UNREAD, Flag.ID_HIGH_PRIORITY, Flag.ID_LOW_PRIORITY,
            Flag.ID_VERSIONED, Flag.ID_POPPED, Flag.ID_NOTE, Flag.ID_INVITE
    );

    /** Creates the default set of immutable system folders in a new mailbox.
     *  These system folders have fixed ids (e.g. {@link #ID_FOLDER_INBOX})
     *  and are hardcoded in the server:<pre>
     *     MAILBOX_ROOT
     *       +--Tags
     *       +--Conversations
     *       +--Comments
     *       +--Profile
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
    protected void initialize() throws ServiceException {
        // the new mailbox's caches are created and the default set of flags are
        // loaded by the earlier call to loadFoldersAndTags in beginTransaction
        lock.lock();
        try {
            createDefaultFolders();
            createDefaultFlags();

            currentChange.itemId = getInitialItemId();
            DbMailbox.updateMailboxStats(this);
        } finally {
            lock.release();
        }
    }

    /**
     * @see #initialize() */
    protected void createDefaultFolders() throws ServiceException {
        lock.lock();
        try {
            byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_DONT_TRACK_COUNTS;
            Folder root = Folder.create(ID_FOLDER_ROOT, this, null, "ROOT", hidden, MailItem.Type.UNKNOWN,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_TAGS, this, root, "Tags", hidden, MailItem.Type.TAG,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_CONVERSATIONS, this, root, "Conversations", hidden, MailItem.Type.CONVERSATION,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_COMMENTS, this, root, "Comments", hidden, MailItem.Type.COMMENT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_PROFILE, this, root, "Profile", hidden, MailItem.Type.DOCUMENT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);

            byte system = Folder.FOLDER_IS_IMMUTABLE;
            Folder userRoot = Folder.create(ID_FOLDER_USER_ROOT, this, root, "USER_ROOT", system, MailItem.Type.UNKNOWN,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_INBOX, this, userRoot, "Inbox", system, MailItem.Type.MESSAGE,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_TRASH, this, userRoot, "Trash", system, MailItem.Type.UNKNOWN,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_SPAM, this, userRoot, "Junk", system, MailItem.Type.MESSAGE,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_SENT, this, userRoot, "Sent", system, MailItem.Type.MESSAGE,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_DRAFTS, this, userRoot, "Drafts", system, MailItem.Type.MESSAGE,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_CONTACTS, this, userRoot, "Contacts", system, MailItem.Type.CONTACT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_CALENDAR, this, userRoot, "Calendar", system, MailItem.Type.APPOINTMENT,
                    Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_TASKS, this, userRoot, "Tasks", system, MailItem.Type.TASK,
                    Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_AUTO_CONTACTS, this, userRoot, "Emailed Contacts", system, MailItem.Type.CONTACT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_IM_LOGS,  this, userRoot, "Chats", system, MailItem.Type.MESSAGE,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
            Folder.create(ID_FOLDER_BRIEFCASE, this, userRoot, "Briefcase", system, MailItem.Type.DOCUMENT,
                    0, MailItem.DEFAULT_COLOR_RGB, null, null);
        } finally {
            lock.release();
        }
    }

    void createDefaultFlags() throws ServiceException {
        lock.lock();
        try {
            // for flags that we want to be searchable, put an entry in the TAG table
            for (int tagId : REIFIED_FLAGS) {
                DbTag.createTag(this, Flag.of(this, tagId).mData, null, false);
            }
        } finally {
            lock.release();
        }
    }

    int getInitialItemId() {
        return FIRST_USER_ID;
    }

    private void loadFoldersAndTags() throws ServiceException {
        // if the persisted mailbox sizes aren't available, we *must* recalculate
        boolean initial = mData.contacts < 0 || mData.size < 0;

        if (mFolderCache != null && mTagCache != null && !initial)
            return;
        ZimbraLog.cache.info("initializing folder and tag caches for mailbox %d", getId());

        try {
            DbMailItem.FolderTagMap folderData = new DbMailItem.FolderTagMap();
            DbMailItem.FolderTagMap tagData    = new DbMailItem.FolderTagMap();
            MailboxData stats = null;

            // Load folders and tags from memcached if we can.
            boolean loadedFromMemcached = false;
            if (!initial && !DebugConfig.disableFoldersTagsCache) {
                FoldersTagsCache ftCache = FoldersTagsCache.getInstance();
                FoldersTags ftData = ftCache.get(this);
                if (ftData != null) {
                    List<Metadata> foldersMeta = ftData.getFolders();
                    for (Metadata meta : foldersMeta) {
                        MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                        ud.deserialize(meta);
                        folderData.put(ud, null);
                    }
                    List<Metadata> tagsMeta = ftData.getTags();
                    for (Metadata meta : tagsMeta) {
                        MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                        ud.deserialize(meta);
                        tagData.put(ud, null);
                    }
                    loadedFromMemcached = true;
                }
            }

            if (!loadedFromMemcached) {
                stats = DbMailItem.getFoldersAndTags(this, folderData, tagData, initial);
            }

            boolean persist = stats != null;
            if (stats != null) {
                if (mData.size != stats.size) {
                    currentChange.dirty.recordModified(currentChange.getOperation(), this, Change.SIZE, currentChange.timestamp);
                    ZimbraLog.mailbox.debug("setting mailbox size to %d (was %d) for mailbox %d", stats.size, mData.size, mId);
                    mData.size = stats.size;
                }
                if (mData.contacts != stats.contacts) {
                    ZimbraLog.mailbox.debug("setting contact count to %d (was %d) for mailbox %d", stats.contacts, mData.contacts, mId);
                    mData.contacts = stats.contacts;
                }
                DbMailbox.updateMailboxStats(this);
            }

            mFolderCache = new HashMap<Integer, Folder>();
            // create the folder objects and, as a side-effect, populate the new cache
            for (Map.Entry<MailItem.UnderlyingData, DbMailItem.FolderTagCounts> entry : folderData.entrySet()) {
                Folder folder = (Folder) MailItem.constructItem(this, entry.getKey());
                DbMailItem.FolderTagCounts fcounts = entry.getValue();
                if (fcounts != null) {
                    folder.setSize(folder.getItemCount(), fcounts.deletedCount, fcounts.totalSize, fcounts.deletedUnreadCount);
                }
            }
            // establish the folder hierarchy
            for (Folder folder : mFolderCache.values()) {
                Folder parent = mFolderCache.get(folder.getFolderId());
                // FIXME: side effect of this is that parent is marked as dirty...
                if (parent != null) {
                    parent.addChild(folder, false);
                }
                // some broken upgrades ended up with CHANGE_DATE = NULL; patch it here
                boolean badChangeDate = folder.getChangeDate() <= 0;
                if (badChangeDate) {
                    markItemModified(folder, Change.INTERNAL_ONLY, folder.snapshotItem());
                    folder.mData.metadataChanged(this);
                }
                // if we recalculated folder counts or had to fix CHANGE_DATE, persist those values now
                if (persist || badChangeDate) {
                    folder.saveFolderCounts(initial);
                }
            }

            mTagCache = new HashMap<Object, Tag>(tagData.size() * 3);
            // create the tag objects and, as a side-effect, populate the new cache
            for (Map.Entry<MailItem.UnderlyingData, DbMailItem.FolderTagCounts> entry : tagData.entrySet()) {
                Tag tag = new Tag(this, entry.getKey());
                if (persist) {
                    tag.saveTagCounts();
                }
            }

            if (!loadedFromMemcached && !DebugConfig.disableFoldersTagsCache) {
                cacheFoldersTagsToMemcached();
            }
        } catch (ServiceException e) {
            mTagCache = null;
            mFolderCache = null;
            throw e;
        }
    }

    void cacheFoldersTagsToMemcached() throws ServiceException {
        lock.lock();
        try {
            List<Folder> folderList = new ArrayList<Folder>(mFolderCache.values());
            List<Tag> tagList = new ArrayList<Tag>();
            for (Map.Entry<Object, Tag> entry : mTagCache.entrySet()) {
                // A tag is cached twice, once by its id and once by name.  Dedupe.
                if (entry.getKey() instanceof String) {
                    tagList.add(entry.getValue());
                }
            }
            FoldersTags ftData = new FoldersTags(folderList, tagList);
            FoldersTagsCache ftCache = FoldersTagsCache.getInstance();
            ftCache.put(this, ftData);
        } finally {
            lock.release();
        }
    }

    public void recalculateFolderAndTagCounts() throws ServiceException {
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

    public void deleteMailbox() throws ServiceException {
        deleteMailbox(null);
    }

    public void deleteMailbox(OperationContext octxt) throws ServiceException {
        lock.lock();
        try {
            // first, throw the mailbox into maintenance mode
            //   (so anyone else with a cached reference to the Mailbox can't use it)
            MailboxMaintenance maintenance = null;
            try {
                maintenance = MailboxManager.getInstance().beginMaintenance(mData.accountId, mId);
            } catch (MailServiceException e) {
                // Ignore wrong mailbox exception.  It may be thrown if we're redoing a DeleteMailbox that was interrupted
                // when server crashed in the middle of the operation.  Database says the mailbox has been deleted, but
                // there may be other files that still need to be cleaned up.
                if (!MailServiceException.WRONG_MAILBOX.equals(e.getCode())) {
                    throw e;
                }
            }

            boolean needRedo = needRedo(octxt);
            DeleteMailbox redoRecorder = new DeleteMailbox(mId);
            boolean success = false;
            try {
                beginTransaction("deleteMailbox", octxt, redoRecorder);
                if (needRedo) {
                    redoRecorder.log();
                }
                try {
                    // remove all the relevant entries from the database
                    DbConnection conn = getOperationConnection();
                    DbMailbox.clearMailboxContent(this);
                    DbMailbox.deleteMailbox(conn, this);

                    // Remove all data related to this mailbox from memcached, so the data doesn't
                    // get used by another user later by mistake if/when mailbox id gets reused.
                    MemcachedCacheManager.purgeMailbox(this);

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
                        index.deleteIndex();
                    } catch (IOException iox) {
                        ZimbraLog.store.warn("Unable to delete index data.", iox);
                    }
                    try {
                        StoreManager.getInstance().deleteStore(this);
                    } catch (IOException iox) {
                        ZimbraLog.store.warn("Unable to delete message data.", iox);
                    }
                }
            } finally {
                if (needRedo) {
                    if (success) {
                        redoRecorder.commit();
                    } else {
                        redoRecorder.abort();
                    }
                }

                if (maintenance != null) {
                    if (success) {
                        // twiddle the mailbox lock [must be the last command of this function!]
                        //   (so even *we* can't access this Mailbox going forward)
                        maintenance.markUnavailable();
                    } else {
                        // end the maintenance if the delete is not successful.
                        MailboxManager.getInstance().endMaintenance(maintenance, success, true);
                    }
                }
            }
        } finally {
            lock.release();
        }
    }

    public void renameMailbox(String oldName, String newName) throws ServiceException {
        renameMailbox(null, oldName, newName);
    }

    public void renameMailbox(OperationContext octxt, String oldName, String newName) throws ServiceException {
        if (Strings.isNullOrEmpty(newName)) {
            throw ServiceException.INVALID_REQUEST("Cannot rename mailbox to empty name", null);
        }
        RenameMailbox redoRecorder = new RenameMailbox(mId, oldName, newName);
        boolean success = false;
        try {
            beginTransaction("renameMailbox", octxt, redoRecorder);

            DbMailbox.renameMailbox(this, newName);

            Account acct = getAccount();
            boolean imEnabledThisAcct = acct.isFeatureIMEnabled();
            boolean xmppEnabled = Provisioning.getInstance().getServer(acct).isXMPPEnabled();

            if (persona != null || (xmppEnabled && imEnabledThisAcct)) {
                getPersona().renamePersona(newName);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public MailboxVersion getVersion() {
        lock.lock(); // for memory visibility
        try {
            return mData.version;
        } finally {
            lock.release();
        }
    }

    void updateVersion(MailboxVersion vers) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("updateVersion", null);

            // remove the deprecated ZIMBRA.MAILBOX_METADATA version value, if present
            if (mData.configKeys != null && mData.configKeys.contains(MailboxVersion.MD_CONFIG_VERSION)) {
                currentChange.dirty.recordModified(currentChange.getOperation(), this, Change.CONFIG, currentChange.timestamp);
                currentChange.config = new Pair<String, Metadata>(MailboxVersion.MD_CONFIG_VERSION, null);
                DbMailbox.updateConfig(this, MailboxVersion.MD_CONFIG_VERSION, null);
            }

            // write the new version to ZIMBRA.MAILBOX
            MailboxVersion version = new MailboxVersion(vers);
            DbMailbox.updateVersion(this, version);
            mData.version = version;
            success = true;
        } finally {
            endTransaction(success);
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
    void reanalyze(int id, MailItem.Type type, Object data, long size) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("reanalyze", null);
            MailItem item;
            try {
                item = getItemById(id, type, false);
            } catch (NoSuchItemException e) { // fallback to dumpster
                item = getItemById(id, type, true);
            }
            item.reanalyze(data, size);
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
     *  authenticated user from the {@link OperationContext} does
     *  not have {@link ACL#RIGHT_READ} on the requested item.<p>
     *
     *  If you want to know if an account has {@link ACL#RIGHT_WRITE} on an
     *  item, call<pre>
     *    (mbox.getEffectivePermissions(new OperationContext(acct), itemId) &
     *         ACL.RIGHT_WRITE) != 0</pre>
     *
     * @param octxt    The context (authenticated user, redo player, other
     *                 constraints) under which this operation is executed.
     *                 Note, if the callstack is currently in a transaction,
     *                 this octxt will be ignored for right checking purpose;
     *                 the OperationContext object associated with the
     *                 top-most transaction will be used for right checking
     *                 purpose instead.
     *
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
    public short getEffectivePermissions(OperationContext octxt, int itemId, MailItem.Type type)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getEffectivePermissions", octxt);

            // fetch the item without perm check so we get it even if the
            // authenticated user doesn't have read permissions on it
            MailItem item = getItemById(itemId, type);

            // use ~0 to query *all* rights; may need to change this when we do negative rights
            short rights = item.checkRights((short) ~0, getAuthenticatedAccount(), isUsingAdminPrivileges());
            success = true;
            return rights;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * This API uses the credentials in authedAcct/asAdmin parameters.
     *
     * @see #getEffectivePermissions(OperationContext, int, com.zimbra.cs.mailbox.MailItem.Type)
     */
    public short getEffectivePermissions(Account authedAcct, boolean asAdmin, int itemId, MailItem.Type type)
            throws ServiceException {

        boolean success = false;
        try {
            beginTransaction("getEffectivePermissions", new OperationContext(authedAcct, asAdmin));

            // fetch the item without perm check so we get it even if the
            // authenticated user doesn't have read permissions on it
            MailItem item = getItemById(itemId, type);

            // use ~0 to query *all* rights; may need to change this when we do negative rights
            short rights = item.checkRights((short) ~0, authedAcct, asAdmin);
            success = true;
            return rights;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Returns whether this type of {@link MailItem} is definitely preloaded in one of the {@link Mailbox}'s caches.
     *
     * @param type  The type of <tt>MailItem</tt>.
     * @return <tt>true</tt> if the item is a {@link Folder} or {@link Tag} or one of their subclasses.
     * @see #mTagCache
     * @see #mFolderCache
     */
    public static boolean isCachedType(MailItem.Type type) {
        switch (type) {
            case FOLDER:
            case SEARCHFOLDER:
            case TAG:
            case FLAG:
            case MOUNTPOINT:
                return true;
            default:
                return false;
        }
    }

    protected <T extends MailItem> T checkAccess(T item) throws ServiceException {
        if (item == null || item.canAccess(ACL.RIGHT_READ)) {
            return item;
        } else {
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
        }
    }

    /** Makes a copy of the given item with {@link Flag#BITMASK_UNCACHED} set.
     *  This copy is not linked to its {@code Mailbox} and thus will not change
     *  when modifications are subsequently made to the original item.  The
     *  original item is unchanged.
     *  <p>
     *  This method should only be called <i>immediately</i> before returning
     *  an item from a public {@code Mailbox} method.  In order to handle
     *  recursive calls, item duplication occurs only when we're in a top-level
     *  transaction; otherwise, the original item is returned.
     * @see #snapshotFolders() */
    @SuppressWarnings("unchecked")
    private <T extends MailItem> T snapshotItem(T item) throws ServiceException {
        if (item == null || item.isTagged(Flag.FlagInfo.UNCACHED) || currentChange.depth > 1) {
            return item;
        }
        if (item instanceof Folder) {
            return mFolderCache == null ? item : (T) snapshotFolders().get(item.getId());
        }

        MailItem.UnderlyingData data = item.getUnderlyingData().clone();
        data.setFlag(Flag.FlagInfo.UNCACHED);
        data.metadata = item.encodeMetadata().toString();
        if (item instanceof VirtualConversation) {
            // VirtualConversations need to be special-cased since MailItem.constructItem() returns null for them
            return (T) new VirtualConversation(this, data);
        } else {
            return (T) MailItem.constructItem(this, data);
        }
    }

    /** Makes a copy of the {@code Mailbox}'s entire {@code Folder} tree with
     *  {@link Flag#BITMASK_UNCACHED} set on each copied folder.  This copy is
     *  not linked to its {@code Mailbox} and thus will not change when
     *  modifications are subsequently made to any of the folders.  The
     *  original folders are unchanged.
     *  <p>
     *  This method should only be called <i>immediately</i> before returning
     *  the folder set from a public {@code Mailbox} method.  In order to
     *  handle recursive calls, item duplication occurs only when we're in a
     *  top-level transaction; otherwise, the live folder cache is returned.
     *  <p>
     *  If the {@code Mailbox}'s folder cache is {@code null}, this method will
     *  also return {@code null}. */
    private Map<Integer, Folder> snapshotFolders() throws ServiceException {
        if (currentChange.depth > 1 || mFolderCache == null) {
            return mFolderCache;
        }
        Map<Integer, Folder> copies = new HashMap<Integer, Folder>();
        for (Folder folder : mFolderCache.values()) {
            MailItem.UnderlyingData data = folder.getUnderlyingData().clone();
            data.setFlag(Flag.FlagInfo.UNCACHED);
            data.metadata = folder.encodeMetadata().toString();
            copies.put(folder.getId(), (Folder) MailItem.constructItem(this, data));
        }
        for (Folder folder : copies.values()) {
            Folder parent = copies.get(folder.getFolderId());
            if (parent != null) {
                parent.addChild(folder, false);
            }
        }
        return copies;
    }

    private static Set<MailItem.Type> FOLDER_TYPES = EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.SEARCHFOLDER, MailItem.Type.MOUNTPOINT);

    /** Makes a deep copy of the {@code PendingModifications} object with
     *  {@link Flag#BITMASK_UNCACHED} set on each {@code MailItem} present in
     *  the {@code created} and {@code modified} hashes.  These copied {@code
     *  MailItem}s are not linked to their {@code Mailbox} and thus will not
     *  change when modifications are subsequently made to the contents of the
     *  {@code Mailbox}.  The original {@code PendingModifications} object and
     *  the {@code MailItem}s it references are unchanged.
     *  <p>
     *  This method should only be called <i>immediately</i> before notifying
     *  listeners of the changes from the currently-ending transaction. */
    private PendingModifications snapshotModifications(PendingModifications pms) throws ServiceException {
        if (pms == null) {
            return null;
        }
        assert(currentChange.depth == 0);

        Map<Integer, MailItem> cache = mItemCache.get();
        Map<Integer, Folder> folders =  mFolderCache == null || Collections.disjoint(pms.changedTypes, FOLDER_TYPES) ? mFolderCache : snapshotFolders();

        PendingModifications snapshot = new PendingModifications();

        if (pms.deleted != null && !pms.deleted.isEmpty()) {
            snapshot.recordDeleted(pms.deleted);
        }

        if (pms.created != null && !pms.created.isEmpty()) {
            for (MailItem item : pms.created.values()) {
                if (item instanceof Folder && folders != null) {
                    Folder folder = folders.get(item.getId());
                    if (folder == null) {
                        ZimbraLog.mailbox.warn("folder missing from snapshotted folder set: %d", item.getId());
                        folder = (Folder) item;
                    }
                    snapshot.recordCreated(folder);
                } else if (item instanceof Tag) {
                    if (((Tag) item).isListed()) {
                        snapshot.recordCreated(snapshotItem(item));
                    }
                } else {
                    // NOTE: if the folder cache is null, folders fall down here and should always get copy == false
                    if (cache != null && cache.containsKey(item.getId())) {
                        item = snapshotItem(item);
                    }
                    snapshot.recordCreated(item);
                }
            }
        }

        if (pms.modified != null && !pms.modified.isEmpty()) {
            for (Map.Entry<PendingModifications.ModificationKey, Change> entry : pms.modified.entrySet()) {
                Change chg = entry.getValue();
                if (!(chg.what instanceof MailItem)) {
                    snapshot.recordModified(entry.getKey(), chg);
                    continue;
                }

                MailItem item = (MailItem) chg.what;
                if (item instanceof Folder && folders != null) {
                    Folder folder = folders.get(item.getId());
                    if (folder == null) {
                        ZimbraLog.mailbox.warn("folder missing from snapshotted folder set: %d", item.getId());
                        folder = (Folder) item;
                    }
                    snapshot.recordModified(chg.op, folder, chg.why, chg.when, (MailItem) chg.preModifyObj);
                } else if (item instanceof Tag) {
                    if (((Tag) item).isListed()) {
                        snapshot.recordModified(chg.op, snapshotItem(item), chg.why, chg.when, (MailItem) chg.preModifyObj);
                    }
                } else {
                    // NOTE: if the folder cache is null, folders fall down here and should always get copy == false
                    if (cache != null && cache.containsKey(item.getId())) {
                        item = snapshotItem(item);
                    }
                    snapshot.recordModified(chg.op, item, chg.why, chg.when, (MailItem) chg.preModifyObj);
                }
            }
        }

        return snapshot;
    }

    /**
     * Returns the <tt>MailItem</tt> with the specified id.
     * @throws NoSuchItemException if the item does not exist
     */
    public MailItem getItemById(OperationContext octxt, int id, MailItem.Type type) throws ServiceException {
        return getItemById(octxt, id, type, false);
    }

    public MailItem getItemById(OperationContext octxt, int id, MailItem.Type type, boolean fromDumpster)
            throws ServiceException {
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemById", octxt);
            MailItem item = checkAccess(getItemById(id, type, fromDumpster));
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    MailItem getItemById(int id, MailItem.Type type) throws ServiceException {
        return getItemById(id, type, false);
    }

    MailItem getItemById(int id, MailItem.Type type, boolean fromDumpster) throws ServiceException {
        if (fromDumpster) {
            boolean noSuchItem = false;
            MailItem item = null;
            try {
                item = MailItem.getById(this, id, type, true);
            } catch (NoSuchItemException e) {
                noSuchItem = true;
            }
            if (item != null) {
                // Pretend the item doesn't exist in the dumpster if user is non-admin and item is too old or is a spam.
                OperationContext octxt = getOperationContext();
                if (!hasFullAdminAccess(octxt)) {
                    long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
                    long threshold = now - getAccount().getDumpsterUserVisibleAge();
                    if (item.getChangeDate() < threshold || item.inSpam()) {
                        noSuchItem = true;
                    }
                }
            }
            // Both truly non-existent case and filter case throw noSuchItem exception here.  This way the client
            // can't distinguish the two cases from the stack trace in the soap fault.
            if (noSuchItem) {
                throw MailItem.noSuchItem(id,  type);
            }
            return item;
        }

        // try the cache first
        MailItem item = getCachedItem(new Integer(id), type);
        if (item != null)
            return item;

        // the tag and folder caches contain ALL tags and folders, so cache miss == doesn't exist
        if (isCachedType(type))
            throw MailItem.noSuchItem(id, type);

        if (id <= -FIRST_USER_ID) {
            // special-case virtual conversations
            if (type != MailItem.Type.CONVERSATION && type != MailItem.Type.UNKNOWN) {
                throw MailItem.noSuchItem(id, type);
            }
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
    public MailItem[] getItemById(OperationContext octxt, Collection<Integer> ids, MailItem.Type type)
            throws ServiceException {
        return getItemById(octxt, ArrayUtil.toIntArray(ids), type);
    }

    /**
     * Returns <tt>MailItem</tt>s with the specified ids.
     * @throws NoSuchItemException any item does not exist
     */
    public MailItem[] getItemById(OperationContext octxt, int[] ids, MailItem.Type type) throws ServiceException {
        return getItemById(octxt, ids, type, false);
    }

    public MailItem[] getItemById(OperationContext octxt, int[] ids, MailItem.Type type, boolean fromDumpster)
            throws ServiceException {
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemById[]", octxt);
            MailItem[] items = getItemById(ids, type, fromDumpster);
            // make sure all those items are visible...
            for (int i = 0; i < items.length; i++) {
                checkAccess(items[i]);
            }
            success = true;
            return items;
        } finally {
            endTransaction(success);
        }
    }

    MailItem[] getItemById(Collection<Integer> ids, MailItem.Type type) throws ServiceException {
        return getItemById(ArrayUtil.toIntArray(ids), type);
    }

    MailItem[] getItemById(int[] ids, MailItem.Type type) throws ServiceException {
        return getItemById(ids, type, false);
    }

    private MailItem[] getItemById(int[] ids, MailItem.Type type, boolean fromDumpster) throws ServiceException {
        if (!currentChange.active) {
            throw ServiceException.FAILURE("must be in transaction", null);
        }
        if (ids == null) {
            return null;
        }
        MailItem items[] = new MailItem[ids.length];
        if (fromDumpster) {
            for (int i = 0; i < items.length; ++i) {
                int id = ids[i];
                if (id > 0) {
                    items[i] = getItemById(id, type, true);
                }
            }
            return items;
        }

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
                    if (!MailItem.isAcceptableType(type, MailItem.Type.CONVERSATION)) {
                        throw MailItem.noSuchItem(ids[i], type);
                    }
                    Message msg = getCachedMessage(-ids[i]);
                    if (msg != null) {
                        if (msg.getConversationId() == ids[i])
                            item = new VirtualConversation(this, msg);
                        else
                            item = getCachedConversation(key = msg.getConversationId());
                    } else {
                        // need to fetch the message in order to get its conv...
                        key = -ids[i];
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
        MailItem.getById(this, uncached, relaxType ? MailItem.Type.UNKNOWN : type);

        uncached.clear();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != ID_AUTO_INCREMENT && items[i] == null) {
                if (ids[i] <= -FIRST_USER_ID) {
                    // special-case virtual conversations
                    MailItem item = getCachedItem(-ids[i]);
                    if (!(item instanceof Message)) {
                        throw MailItem.noSuchItem(ids[i], type);
                    } else if (item.getParentId() == ids[i]) {
                        items[i] = new VirtualConversation(this, (Message) item);
                    } else {
                        items[i] = getCachedItem(item.getParentId());
                        if (items[i] == null)
                            uncached.add(item.getParentId());
                    }
                } else {
                    if ((items[i] = getCachedItem(ids[i])) == null)
                        throw MailItem.noSuchItem(ids[i], type);
                }
            }
        }

        // special case asking for VirtualConversation but having it be a real Conversation
        if (!uncached.isEmpty()) {
            MailItem.getById(this, uncached, MailItem.Type.CONVERSATION);
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] <= -FIRST_USER_ID && items[i] == null) {
                    MailItem item = getCachedItem(-ids[i]);
                    if (!(item instanceof Message) || item.getParentId() == ids[i])
                        throw ServiceException.FAILURE("item should be cached but is not: " + -ids[i], null);
                    items[i] = getCachedItem(item.getParentId());
                    if (items[i] == null)
                        throw MailItem.noSuchItem(ids[i], type);
                }
            }
        }

        return items;
    }

    /** retrieve an item from the Mailbox's caches; return null if no item found */
    MailItem getCachedItem(Integer key) throws ServiceException {
        MailItem item = null;
        if (key < 0) {
            item = Flag.of(this, key);
        }
        if (item == null && mTagCache != null) {
            item = mTagCache.get(key);
        }
        if (item == null && mFolderCache != null) {
            item = mFolderCache.get(key);
        }
        if (item == null) {
            item = getItemCache().get(key);
        }
        logCacheActivity(key, item == null ? MailItem.Type.UNKNOWN : item.getType(), item);
        return item;
    }

    MailItem getCachedItem(Integer key, MailItem.Type type) throws ServiceException {
        MailItem item = null;
        switch (type) {
            case UNKNOWN:
                return getCachedItem(key);
            case FLAG:
            case TAG:
                if (key < 0) {
                    item = Flag.of(this, key);
                } else if (mTagCache != null) {
                    item = mTagCache.get(key);
                }
                break;
            case MOUNTPOINT:
            case SEARCHFOLDER:
            case FOLDER:
                if (mFolderCache != null) {
                    item = mFolderCache.get(key);
                }
                break;
            default:
                item = getItemCache().get(key);
                break;
        }

        if (item != null && !MailItem.isAcceptableType(type, MailItem.Type.of(item.mData.type))) {
            item = null;
        }

        logCacheActivity(key, type, item);
        return item;
    }

    /** translate from the DB representation of an item to its Mailbox abstraction */
    MailItem getItem(MailItem.UnderlyingData data) throws ServiceException {
        if (data == null)
            return null;
        MailItem item = getCachedItem(data.id, MailItem.Type.of(data.type));
        // XXX: should we sanity-check the cached version to make sure all the data matches?
        if (item != null) {
            return item;
        }
        return MailItem.constructItem(this, data);
    }

    /** Returns a current or past revision of an item.  Item version numbers
     *  are 1-based and incremented each time the "content" of the item changes
     *  (e.g. editing a draft, modifying a contact's fields).  If the requested
     *  revision does not exist, either because the version number is out of
     *  range or because the requested revision has not been retained, returns
     *  <tt>null</tt>. */
    public MailItem getItemRevision(OperationContext octxt, int id, MailItem.Type type, int version)
            throws ServiceException {
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
    @SuppressWarnings("unchecked")
    public <T extends MailItem> List<T> getAllRevisions(OperationContext octxt, int id, MailItem.Type type)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getAllRevisions", octxt);
            T item = (T) checkAccess(getItemById(id, type));
            List<MailItem> previousRevisions = item.loadRevisions();
            List<T> result = new ArrayList<T>(previousRevisions.size());
            for (MailItem rev : previousRevisions) {
                result.add((T) rev);
            }
            result.add(item);

            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Fetches a <tt>MailItem</tt> by its IMAP id.
     * @throws MailServiceException if there is no <tt>MailItem</tt> with the given id.
     * @see MailServiceException#NO_SUCH_ITEM
     */
    public MailItem getItemByImapId(OperationContext octxt, int imapId, int folderId) throws ServiceException {
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemByImapId", octxt);

            MailItem item = checkAccess(getCachedItem(imapId));
            // in general, the item will not have been moved and its id will be the same as its IMAP id.
            if (item == null) {
                try {
                    item = checkAccess(MailItem.getById(this, imapId));
                    if (item.getImapUid() != imapId) {
                        item = null;
                    }
                } catch (NoSuchItemException nsie) {
                }
            }
            // if it's not found, we have to search on the non-indexed IMAP_ID column...
            if (item == null) {
                item = checkAccess(MailItem.getByImapId(this, imapId, folderId));
            }
            if (isCachedType(item.getType()) || item.getImapUid() != imapId || item.getFolderId() != folderId) {
                throw MailServiceException.NO_SUCH_ITEM(imapId);
            }
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    /** Fetches an item by path relative to {@link #ID_FOLDER_USER_ROOT}.
     * @see #getItemByPath(OperationContext, String, int) */
    public MailItem getItemByPath(OperationContext octxt, String path) throws ServiceException {
        return getItemByPath(octxt, path, ID_FOLDER_USER_ROOT);
    }

    /** Fetches an item by path.  If the path begins with <tt>/</tt>, it's
     *  considered an absolute path relative to {@link #ID_FOLDER_USER_ROOT}.
     *  If it doesn't, it's computed relative to the passed-in folder ID.<p>
     *
     *  This can return anything with a name; at present, that is limited to
     *  {@link Folder}s, {@link Tag}s, and {@link Document}s. */
    public MailItem getItemByPath(OperationContext octxt, String name, int folderId) throws ServiceException {
        if (name != null) {
            while (name.startsWith("/")) {
                folderId = ID_FOLDER_USER_ROOT;
                name = name.substring(1);
            }
            while (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
        }
        if (Strings.isNullOrEmpty(name)) {
            return getFolderById(octxt, folderId);
        }
        boolean success = false;
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemByPath", octxt);

            Folder parent = (Folder) getItemById(folderId, MailItem.Type.FOLDER);

            int slash = name.lastIndexOf('/');
            if (slash != -1) {
                for (String segment : name.substring(0, slash).split("/")) {
                    if ((parent = parent.findSubfolder(segment)) == null) {
                        throw MailServiceException.NO_SUCH_FOLDER(name);
                    }
                }
                name = name.substring(slash + 1);
            }

            MailItem item = null;
            if (folderId == ID_FOLDER_TAGS) {
                item = getTagByName(name);
            } else {
                // check for the specified item -- folder first, then document
                item = parent.findSubfolder(name);
                if (item == null) {
                    item = getItem(DbMailItem.getByName(this, parent.getId(), name, MailItem.Type.DOCUMENT));
                }
            }
            // make sure the item is visible to the requester
            if (checkAccess(item) == null) {
                throw MailServiceException.NO_SUCH_ITEM(name);
            }
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns all the MailItems of a given type, optionally in a specified folder */
    public List<MailItem> getItemList(OperationContext octxt, MailItem.Type type) throws ServiceException {
        return getItemList(octxt, type, -1);
    }

    public List<MailItem> getItemList(OperationContext octxt, MailItem.Type type, int folderId)
    throws ServiceException {
        return getItemList(octxt, type, folderId, SortBy.NONE);
    }

    public List<MailItem> getItemList(OperationContext octxt, MailItem.Type type, int folderId, SortBy sort)
    throws ServiceException {
        List<MailItem> result;
        boolean success = false;

        if (type == MailItem.Type.UNKNOWN) {
            return Collections.emptyList();
        }
        try {
            // tag/folder caches are populated in beginTransaction...
            beginTransaction("getItemList", octxt);

            Folder folder = folderId == -1 ? null : getFolderById(folderId);
            if (folder == null) {
                if (!hasFullAccess()) {
                    throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
                }
            } else {
                if (!folder.canAccess(ACL.RIGHT_READ, getAuthenticatedAccount(), isUsingAdminPrivileges())) {
                    throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
                }
            }

            switch (type) {
                case FOLDER:
                case SEARCHFOLDER:
                case MOUNTPOINT:
                    result = new ArrayList<MailItem>(mFolderCache.size());
                    for (Folder subfolder : mFolderCache.values()) {
                        if (subfolder.getType() == type || type == MailItem.Type.FOLDER) {
                            if (folder == null || subfolder.getFolderId() == folderId) {
                                result.add(subfolder);
                            }
                        }
                    }
                    success = true;
                    break;
                case TAG:
                    if (folderId != -1 && folderId != ID_FOLDER_TAGS) {
                        return Collections.emptyList();
                    }
                    result = new ArrayList<MailItem>(mTagCache.size() / 2);
                    for (Map.Entry<Object, Tag> entry : mTagCache.entrySet()) {
                        Tag tag = entry.getValue();
                        if (entry.getKey() instanceof String && tag.isListed()) {
                            result.add(tag);
                        }
                    }
                    success = true;
                    break;
                case FLAG:
                    if (folderId != -1 && folderId != ID_FOLDER_TAGS) {
                        return Collections.emptyList();
                    }
                    List<Flag> allFlags = Flag.allOf(this);
                    result = new ArrayList<MailItem>(allFlags.size());
                    for (Flag flag : allFlags) {
                        result.add(flag);
                    }
                    success = true;
                    break;
                default:
                    List<MailItem.UnderlyingData> dataList;
                    if (folder != null) {
                        dataList = DbMailItem.getByFolder(folder, type, sort);
                    } else {
                        dataList = DbMailItem.getByType(this, type, sort);
                    }
                    if (dataList == null) {
                        return Collections.emptyList();
                    }
                    result = new ArrayList<MailItem>(dataList.size());
                    for (MailItem.UnderlyingData data : dataList) {
                        if (data != null) {
                            result.add(getItem(data));
                        }
                    }
                    // DbMailItem call handles all sorts except SORT_BY_NAME_NAT
                    if (sort.getKey() == SortBy.Key.NAME_NATURAL_ORDER) {
                        sort = SortBy.NONE;
                    }
                    success = true;
                    break;
            }
        } finally {
            endTransaction(success);
        }

        Comparator<MailItem> comp = MailItem.getComparator(sort);
        if (comp != null) {
            Collections.sort(result, comp);
        }
        return result;
    }

    /** returns the list of IDs of items of the given type in the given folder. */
    public List<Integer> listItemIds(OperationContext octxt, MailItem.Type type, int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("listItemIds", octxt);
            Folder folder = getFolderById(folderId);
            List<Integer> ids = DbMailItem.listByFolder(folder, type, true);
            success = true;
            return ids;
        } finally {
            endTransaction(success);
        }
    }

    public TypedIdList getItemIds(OperationContext octxt, int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("listAllItemIds", octxt);
            Folder folder = getFolderById(folderId);
            TypedIdList ids = DbMailItem.listByFolder(folder, true);
            success = true;
            return ids;
        } finally {
            endTransaction(success);
        }
    }


    public List<ImapMessage> openImapFolder(OperationContext octxt, int folderId) throws ServiceException {
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

    public List<Pop3Message> openPop3Folder(OperationContext octxt, Set<Integer> folderIds, Date popSince)
    throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("openPop3Folder", octxt);
            ImmutableSet.Builder<Folder> folders = ImmutableSet.builder();
            for (int folderId : folderIds) {
                folders.add(getFolderById(folderId));
            }
            List<Pop3Message> p3list = DbMailItem.loadPop3Folder(folders.build(), popSince);
            success = true;
            return p3list;
        } finally {
            endTransaction(success);
        }
    }

    public int getImapRecent(OperationContext octxt, int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("openImapFolder", octxt);
            Folder folder = checkAccess(getFolderById(folderId));
            int recent = folder.getImapRECENT();
            success = true;
            return recent;
        } finally {
            endTransaction(success);
        }
    }

    public void beginTrackingImap() throws ServiceException {
        lock.lock();
        try {
            if (isTrackingImap()) {
                return;
            }
            TrackImap redoRecorder = new TrackImap(mId);
            boolean success = false;
            try {
                beginTransaction("beginTrackingImap", null, redoRecorder);
                DbMailbox.startTrackingImap(this);
                currentChange.imap = Boolean.TRUE;
                success = true;
            } finally {
                endTransaction(success);
            }
        } finally {
            lock.release();
        }
    }

    public void beginTrackingSync() throws ServiceException {
        lock.lock();
        try {
            if (isTrackingSync()) {
                return;
            }
            TrackSync redoRecorder = new TrackSync(mId);
            boolean success = false;
            try {
                beginTransaction("beginTrackingSync", null, redoRecorder);
                currentChange.sync = getLastChangeID();
                DbMailbox.startTrackingSync(this);
                success = true;
            } finally {
                endTransaction(success);
            }
        } finally {
            lock.release();
        }
    }

    public void recordImapSession(int folderId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("recordImapSession", null);
            getFolderById(folderId).checkpointRECENT();
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public List<Integer> listTombstones(int lastSync) throws ServiceException {
        return getTombstones(lastSync).getAll();
    }

    public TypedIdList getTombstones(int lastSync) throws ServiceException {
        lock.lock();
        try {
            if (!isTrackingSync()) {
                throw ServiceException.FAILURE("not tracking sync", null);
            } else if (lastSync < getSyncCutoff()) {
                throw MailServiceException.TOMBSTONES_EXPIRED();
            }

            boolean success = false;
            try {
                beginTransaction("getTombstones", null);
                TypedIdList tombstones = DbMailItem.readTombstones(this, lastSync);
                success = true;
                return tombstones;
            } finally {
                endTransaction(success);
            }
        } finally {
            lock.release();
        }
    }

    public List<Folder> getModifiedFolders(final int lastSync) throws ServiceException {
        return getModifiedFolders(lastSync, MailItem.Type.UNKNOWN);
    }

    public List<Folder> getModifiedFolders(final int lastSync, final MailItem.Type type) throws ServiceException {
        lock.lock();
        try {
            if (lastSync >= getLastChangeID()) {
                return Collections.emptyList();
            }

            List<Folder> modified = new ArrayList<Folder>();
            boolean success = false;
            try {
                beginTransaction("getModifiedFolders", null);
                for (Folder subfolder : getFolderById(ID_FOLDER_ROOT).getSubfolderHierarchy()) {
                    if (type == MailItem.Type.UNKNOWN || subfolder.getType() == type) {
                        if (subfolder.getModifiedSequence() > lastSync) {
                            modified.add(subfolder);
                        }
                    }
                }
                success = true;
                return modified;
            } finally {
                endTransaction(success);
            }
        } finally {
            lock.release();
        }
    }

    public List<Tag> getModifiedTags(OperationContext octxt, int lastSync) throws ServiceException {
        lock.lock();
        try {
            if (lastSync >= getLastChangeID()) {
                return Collections.emptyList();
            }
            List<Tag> modified = new ArrayList<Tag>();
            boolean success = false;
            try {
                beginTransaction("getModifiedTags", octxt);
                if (hasFullAccess()) {
                    for (Map.Entry<Object, Tag> entry : mTagCache.entrySet()) {
                        if (entry.getKey() instanceof String) {
                            Tag tag = entry.getValue();
                            if (tag.isListed() && tag.getModifiedSequence() > lastSync) {
                                modified.add(tag);
                            }
                        }
                    }
                }
                success = true;
                return modified;
            } finally {
                endTransaction(success);
            }
        } finally {
            lock.release();
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
    public Pair<List<Integer>,TypedIdList> getModifiedItems(OperationContext octxt, int lastSync)
            throws ServiceException {
        return getModifiedItems(octxt, lastSync, MailItem.Type.UNKNOWN, null);
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
    public Pair<List<Integer>,TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, MailItem.Type type)
            throws ServiceException {
        return getModifiedItems(octxt, lastSync, type, null);
    }

    public Pair<List<Integer>,TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, MailItem.Type type,
            Set<Integer> folderIds) throws ServiceException {
        lock.lock();
        try {
            if (lastSync >= getLastChangeID()) {
                return new Pair<List<Integer>, TypedIdList>(Collections.<Integer>emptyList(), new TypedIdList());
            }
            boolean success = false;
            try {
                beginTransaction("getModifiedItems", octxt);

                Set<Integer> visible = Folder.toId(getAccessibleFolders(ACL.RIGHT_READ));
                if (folderIds == null) {
                    folderIds = visible;
                } else if (visible != null) {
                    folderIds = SetUtil.intersect(folderIds, visible);
                }
                Pair<List<Integer>,TypedIdList> dataList = DbMailItem.getModifiedItems(this, type, lastSync, folderIds);
                if (dataList == null) {
                    return null;
                }
                success = true;
                return dataList;
            } finally {
                endTransaction(success);
            }
        } finally {
            lock.release();
        }
    }

    /**
     * Returns a list of all {@link Folder}s the authenticated user has {@link ACL#RIGHT_READ} access to. Returns
     * {@code null} if the authenticated user has read access to the entire Mailbox.
     *
     * @see #getAccessibleFolders(short)
     */
    public Set<Folder> getVisibleFolders(OperationContext octxt) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getVisibleFolders", octxt);
            Set<Folder> visible = getAccessibleFolders(ACL.RIGHT_READ);
            success = true;
            return visible;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Returns a list of all {@link Folder}s that the authenticated user from the current transaction has a certain set
     * of rights on. Returns {@code null} if the authenticated user has the required access on the entire Mailbox.
     *
     * @param rights bitmask representing the required permissions
     */
    Set<Folder> getAccessibleFolders(short rights) throws ServiceException {
        if (!currentChange.isActive()) {
            throw ServiceException.FAILURE("cannot get visible hierarchy outside transaction", null);
        }
        if (hasFullAccess()) {
            return null;
        }
        boolean incomplete = false;
        Set<Folder> visible = new HashSet<Folder>();
        for (Folder folder : mFolderCache.values()) {
            if (folder.canAccess(rights)) {
                visible.add(folder);
            } else {
                incomplete = true;
            }
        }
        return incomplete ? visible : null;
    }

    public Flag getFlagById(int flagId) throws ServiceException {
        Flag flag = Flag.of(this, flagId);
        if (flag == null) {
            throw MailServiceException.NO_SUCH_TAG(flagId);
        }
        return flag;
    }

    public List<Flag> getFlagList() throws ServiceException {
        return Flag.allOf(this);
    }

    public Tag getTagById(OperationContext octxt, int id) throws ServiceException {
        return (Tag) getItemById(octxt, id, MailItem.Type.TAG);
    }

    Tag getTagById(int id) throws ServiceException {
        return (Tag) getItemById(id, MailItem.Type.TAG);
    }

    public List<Tag> getTagList(OperationContext octxt) throws ServiceException {
        List<Tag> tags = new ArrayList<Tag>();
        for (MailItem item : getItemList(octxt, MailItem.Type.TAG)) {
            tags.add((Tag) item);
        }
        return tags;
    }

    /**
     * Returns the tag with the given name.
     *
     * @throws ServiceException
     *  <ul>
     *   <li>{@link ServiceException#INVALID_REQUEST} if the name is null or empty
     *   <li>{@link MailServiceException#NO_SUCH_TAG} if the tag does not exist
     *  </ul>
     */
    public Tag getTagByName(String name) throws ServiceException {
        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("tag name may not be null", null);
        }

        boolean success = false;
        try {
            beginTransaction("getTagByName", null);
            Tag tag = name.startsWith(Tag.FLAG_NAME_PREFIX) ? Flag.of(this, name) : mTagCache.get(name.toLowerCase());
            if (tag == null) {
                throw MailServiceException.NO_SUCH_TAG(name);
            }
            success = true;
            return tag;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns the folder with the specified id.
     * @throws NoSuchItemException if the folder does not exist */
    public Folder getFolderById(OperationContext octxt, int id) throws ServiceException {
        return (Folder) getItemById(octxt, id, MailItem.Type.FOLDER);
    }

    /** Returns the folder with the specified id.
     * @throws NoSuchItemException if the folder does not exist */
    Folder getFolderById(int id) throws ServiceException {
        return (Folder) getItemById(id, MailItem.Type.FOLDER);
    }

    /** Returns the folder with the specified parent and name.
     * @throws NoSuchItemException if the folder does not exist */
    public Folder getFolderByName(OperationContext octxt, int parentId, String name) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getFolderByName", octxt);
            Folder folder = getFolderById(parentId).findSubfolder(name);
            if (folder == null) {
                throw MailServiceException.NO_SUCH_FOLDER(name);
            } else if (!folder.canAccess(ACL.RIGHT_READ)) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on folder " + name);
            }
            success = true;
            return folder;
        } finally {
            endTransaction(success);
        }
    }

    /** Returns the folder with the specified path, delimited by slashes (<tt>/</tt>).
     * @throws {@link NoSuchItemException} if the folder does not exist */
    public Folder getFolderByPath(OperationContext octxt, String path) throws ServiceException {
        if (path == null) {
            throw MailServiceException.NO_SUCH_FOLDER(path);
        }

        while (path.startsWith("/")) {
            path = path.substring(1); // strip off the optional leading "/"
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1); // strip off the optional trailing "/"
        }

        Folder folder = getFolderById(null, ID_FOLDER_USER_ROOT);

        boolean success = false;
        try {
            beginTransaction("getFolderByPath", octxt); // for ACL check
            if (!path.isEmpty()) {
                for (String segment : path.split("/")) {
                    if ((folder = folder.findSubfolder(segment)) == null) {
                        break;
                    }
                }
            }

            if (folder == null) {
                throw MailServiceException.NO_SUCH_FOLDER("/" + path);
            } else if (!folder.canAccess(ACL.RIGHT_READ)) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on folder /" + path);
            }
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
     * @param baseFolderId Folder to start from (pass Mailbox.ID_FOLDER_ROOT to start from the root)
     * @throws ServiceException if the folder with {@code startingFolderId} does not exist or {@code path} is
     * {@code null} or empty.
     */
    public Pair<Folder, String> getFolderByPathLongestMatch(OperationContext octxt, int baseFolderId, String path)
            throws ServiceException {
        if (Strings.isNullOrEmpty(path)) {
            throw MailServiceException.NO_SUCH_FOLDER(path);
        }
        Folder folder = getFolderById(null, baseFolderId);  // Null ctxt avoids PERM_DENIED error when requester != owner.
        assert(folder != null);
        path = CharMatcher.is('/').trimFrom(path); // trim leading and trailing '/'
        if (path.isEmpty()) { // relative root to the base folder
            return new Pair<Folder, String>(checkAccess(folder), null);
        }

        boolean success = false;
        try {
            beginTransaction("getFolderByPathLongestMatch", octxt); // for ACL check
            String unmatched = null;
            String[] segments = path.split("/");
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

    public List<Folder> getFolderList(OperationContext octxt, SortBy sort) throws ServiceException {
        List<Folder> folders = new ArrayList<Folder>();
        for (MailItem item : getItemList(octxt, MailItem.Type.FOLDER, -1, sort)) {
            folders.add((Folder) item);
        }
        return folders;
    }

    List<Folder> listAllFolders() {
        return new ArrayList<Folder>(mFolderCache.values());
    }

    public static class FolderNode {
        public int mId;
        public String mName;
        public Folder mFolder;
        public List<FolderNode> mSubfolders = new ArrayList<FolderNode>();

        @Override
        public String toString() {
            return "" + mName + " [" + mId + "]" + (mFolder == null ? " (hidden)" : "");
        }
    }

    public FolderNode getFolderTree(OperationContext octxt, ItemId iid, boolean returnAllVisibleFolders)
            throws ServiceException {
        lock.lock();
        try {
            // get the root node...
            int folderId = iid != null ? iid.getId() : Mailbox.ID_FOLDER_USER_ROOT;
            Folder folder = getFolderById(returnAllVisibleFolders ? null : octxt, folderId);
            // for each subNode...
            Set<Folder> visibleFolders = getVisibleFolders(octxt);
            return handleFolder(folder, visibleFolders, returnAllVisibleFolders);
        } finally {
            lock.release();
        }
    }

    private FolderNode handleFolder(Folder folder, Set<Folder> visible, boolean returnAllVisibleFolders)
            throws ServiceException {
        boolean isVisible = visible == null || visible.remove(folder);
        if (!isVisible && !returnAllVisibleFolders) {
            return null;
        }
        // short-circuit if we know that this won't be in the output
        List<Folder> subfolders = folder.getSubfolders(null);
        if (!isVisible && subfolders.isEmpty()) {
            return null;
        }
        FolderNode node = new FolderNode();
        node.mId = folder.getId();
        node.mName = node.mId == Mailbox.ID_FOLDER_ROOT ? null : folder.getName();
        node.mFolder = isVisible ? folder : null;

        // if this was the last visible folder overall, no need to look at children
        if (isVisible && visible != null && visible.isEmpty()) {
            return node;
        }
        // write the subfolders' data to the response
        for (Folder subfolder : subfolders) {
            FolderNode child = handleFolder(subfolder, visible, returnAllVisibleFolders);
            if (child != null) {
                node.mSubfolders.add(child);
                isVisible = true;
            }
        }

        return isVisible ? node : null;
    }

    public List<Folder> getCalendarFolders(OperationContext octxt, SortBy sort) throws ServiceException {
        lock.lock();
        try {
            List<Folder> calFolders = new ArrayList<Folder>();
            for (MailItem item : getItemList(octxt, MailItem.Type.FOLDER, -1, sort)) {
                Folder f = (Folder) item;
                MailItem.Type view = f.getDefaultView();
                if (view == MailItem.Type.APPOINTMENT || view == MailItem.Type.TASK) {
                    calFolders.add((Folder) item);
                }
            }
            for (MailItem item : getItemList(octxt, MailItem.Type.MOUNTPOINT, -1, sort)) {
                Folder f = (Folder) item;
                MailItem.Type view = f.getDefaultView();
                if (view == MailItem.Type.APPOINTMENT || view == MailItem.Type.TASK) {
                    calFolders.add((Folder) item);
                }
            }
            return calFolders;
        } finally {
            lock.release();
        }
    }

    public SearchFolder getSearchFolderById(OperationContext octxt, int searchId) throws ServiceException {
        return (SearchFolder) getItemById(octxt, searchId, MailItem.Type.SEARCHFOLDER);
    }

    SearchFolder getSearchFolderById(int searchId) throws ServiceException {
        return (SearchFolder) getItemById(searchId, MailItem.Type.SEARCHFOLDER);
    }

    public Mountpoint getMountpointById(OperationContext octxt, int mptId) throws ServiceException {
        return (Mountpoint) getItemById(octxt, mptId, MailItem.Type.MOUNTPOINT);
    }


    public Note getNoteById(OperationContext octxt, int noteId) throws ServiceException {
        return (Note) getItemById(octxt, noteId, MailItem.Type.NOTE);
    }

    Note getNoteById(int noteId) throws ServiceException {
        return (Note) getItemById(noteId, MailItem.Type.NOTE);
    }

    public List<Note> getNoteList(OperationContext octxt, int folderId) throws ServiceException {
        return getNoteList(octxt, folderId, SortBy.NONE);
    }

    public List<Note> getNoteList(OperationContext octxt, int folderId, SortBy sort) throws ServiceException {
        List<Note> notes = new ArrayList<Note>();
        for (MailItem item : getItemList(octxt, MailItem.Type.NOTE, folderId, sort)) {
            notes.add((Note) item);
        }
        return notes;
    }

    public Chat getChatById(OperationContext octxt, int id) throws ServiceException {
        return (Chat) getItemById(octxt, id, MailItem.Type.CHAT);
    }

    Chat getChatById(int id) throws ServiceException {
        return (Chat) getItemById(id, MailItem.Type.CHAT);
    }

    public List<Chat> getChatList(OperationContext octxt, int folderId) throws ServiceException {
        return getChatList(octxt, folderId, SortBy.NONE);
    }

    public List<Chat> getChatList(OperationContext octxt, int folderId, SortBy sort) throws ServiceException {
        List<Chat> chats = new ArrayList<Chat>();
        for (MailItem item : getItemList(octxt, MailItem.Type.CHAT, folderId, sort)) {
            chats.add((Chat) item);
        }
        return chats;
    }

    public Contact getContactById(OperationContext octxt, int id) throws ServiceException {
        return (Contact) getItemById(octxt, id, MailItem.Type.CONTACT);
    }

    Contact getContactById(int id) throws ServiceException {
        return (Contact) getItemById(id, MailItem.Type.CONTACT);
    }

    public List<Contact> getContactList(OperationContext octxt, int folderId) throws ServiceException {
        return getContactList(octxt, folderId, SortBy.NONE);
    }

    public List<Contact> getContactList(OperationContext octxt, int folderId, SortBy sort) throws ServiceException {
        List<Contact> contacts = new ArrayList<Contact>();
        for (MailItem item : getItemList(octxt, MailItem.Type.CONTACT, folderId, sort)) {
            contacts.add((Contact) item);
        }
        return contacts;
    }

    /**
     * Returns the <tt>Message</tt> with the specified id.
     * @throws NoSuchItemException if the item does not exist
     */
    public Message getMessageById(OperationContext octxt, int id) throws ServiceException {
        return (Message) getItemById(octxt, id, MailItem.Type.MESSAGE);
    }

    Message getMessageById(int id) throws ServiceException {
        return (Message) getItemById(id, MailItem.Type.MESSAGE);
    }

    Message getMessage(MailItem.UnderlyingData data) throws ServiceException {
        return (Message) getItem(data);
    }

    Message getCachedMessage(Integer id) throws ServiceException {
        return (Message) getCachedItem(id, MailItem.Type.MESSAGE);
    }

    public List<Message> getMessagesByConversation(OperationContext octxt, int convId) throws ServiceException {
        return getMessagesByConversation(octxt, convId, SortBy.DATE_ASC, -1);
    }

    public List<Message> getMessagesByConversation(OperationContext octxt, int convId, SortBy sort, int limit)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getMessagesByConversation", octxt);
            List<Message> msgs = getConversationById(convId).getMessages(sort, limit);
            if (!hasFullAccess()) {
                List<Message> visible = new ArrayList<Message>(msgs.size());
                for (Message msg : msgs) {
                    if (msg.canAccess(ACL.RIGHT_READ)) {
                        visible.add(msg);
                    }
                }
                msgs = visible;
            }
            success = true;
            return msgs;
        } finally {
            endTransaction(success);
        }
    }

    public Conversation getConversationById(OperationContext octxt, int id) throws ServiceException {
        return (Conversation) getItemById(octxt, id, MailItem.Type.CONVERSATION);
    }

    Conversation getConversationById(int id) throws ServiceException {
        return (Conversation) getItemById(id, MailItem.Type.CONVERSATION);
    }

    Conversation getConversation(MailItem.UnderlyingData data) throws ServiceException {
        return (Conversation) getItem(data);
    }

    Conversation getCachedConversation(Integer id) throws ServiceException {
        return (Conversation) getCachedItem(id, MailItem.Type.CONVERSATION);
    }

    public Conversation getConversationByHash(OperationContext octxt, String hash) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getConversationByHash", octxt);
            Conversation conv = checkAccess(getConversationByHash(hash));
            success = true;
            return conv;
        } finally {
            endTransaction(success);
        }
    }

    Conversation getConversationByHash(String hash) throws ServiceException {
        Conversation conv = null;

        Integer convId = mConvHashes.get(hash);
        if (convId != null)
            conv = getCachedConversation(convId);
        if (conv != null)
            return conv;

        // XXX: why not just do a "getConversationById()" if convId != null?
        MailItem.UnderlyingData data = DbMailItem.getByHash(this, hash);
        if (data == null || data.type == MailItem.Type.CONVERSATION.toByte()) {
            return getConversation(data);
        }
        return (Conversation) getMessage(data).getParent();
    }

    public SenderList getConversationSenderList(int convId) throws ServiceException {
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
        return (WikiItem) getItemById(octxt, id, MailItem.Type.WIKI);
    }

    public Document getDocumentById(OperationContext octxt, int id) throws ServiceException {
        return (Document) getItemById(octxt, id, MailItem.Type.DOCUMENT);
    }

    Document getDocumentById(int id) throws ServiceException {
        return (Document) getItemById(id, MailItem.Type.DOCUMENT);
    }

    public List<Document> getDocumentList(OperationContext octxt, int folderId) throws ServiceException {
        return getDocumentList(octxt, folderId, SortBy.NONE);
    }

    public List<Document> getDocumentList(OperationContext octxt, int folderId, SortBy sort) throws ServiceException {
        lock.lock();
        try {
            List<Document> docs = new ArrayList<Document>();
            for (MailItem item : getItemList(octxt, MailItem.Type.DOCUMENT, folderId, sort)) {
                docs.add((Document) item);
            }
            return docs;
        } finally {
            lock.release();
        }
    }

    public Collection<CalendarItem.CalendarMetadata> getCalendarItemMetadata(int folderId, long start, long end)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getCalendarItemMetadata", null);
            Folder f = getFolderById(folderId);
            if (!f.canAccess(ACL.RIGHT_READ)) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
            success = true;
            return DbMailItem.getCalendarItemMetadata(f, start, end);
        } finally {
            endTransaction(success);
        }
    }

    private void checkCalendarType(MailItem item) throws ServiceException {
        MailItem.Type type = item.getType();
        if (type != MailItem.Type.APPOINTMENT && type != MailItem.Type.TASK) {
            throw MailServiceException.NO_SUCH_CALITEM(item.getId());
        }
    }

    public CalendarItem getCalendarItemById(OperationContext octxt, int id) throws ServiceException {
        lock.lock();
        try {
            MailItem item = getItemById(octxt, id, MailItem.Type.UNKNOWN);
            checkCalendarType(item);
            return (CalendarItem) item;
        } finally {
            lock.release();
        }
    }

    CalendarItem getCalendarItemById(int id) throws ServiceException {
        MailItem item = getItemById(id, MailItem.Type.UNKNOWN);
        checkCalendarType(item);
        return (CalendarItem) item;
    }

    CalendarItem getCalendarItem(MailItem.UnderlyingData data) throws ServiceException {
        return (CalendarItem) getItem(data);
    }

    public List<MailItem> getCalendarItemList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.Type.UNKNOWN, folderId);
    }

    public Appointment getAppointmentById(OperationContext octxt, int id) throws ServiceException {
        return (Appointment) getItemById(octxt, id, MailItem.Type.APPOINTMENT);
    }

    Appointment getAppointmentById(int id) throws ServiceException {
        return (Appointment) getItemById(id, MailItem.Type.APPOINTMENT);
    }

    public List<MailItem> getAppointmentList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.Type.APPOINTMENT, folderId);
    }


    public Task getTaskById(OperationContext octxt, int id) throws ServiceException {
        return (Task) getItemById(octxt, id, MailItem.Type.TASK);
    }

    Task getTaskById(int id) throws ServiceException {
        return (Task) getItemById(id, MailItem.Type.TASK);
    }

    public List<MailItem> getTaskList(OperationContext octxt, int folderId) throws ServiceException {
        return getItemList(octxt, MailItem.Type.TASK, folderId);
    }


    public TypedIdList listCalendarItemsForRange(OperationContext octxt, MailItem.Type type,
            long start, long end, int folderId) throws ServiceException {
        if (folderId == ID_AUTO_INCREMENT) {
            return new TypedIdList();
        }
        boolean success = false;
        try {
            beginTransaction("listCalendarItemsForRange", octxt);

            // if they specified a folder, make sure it actually exists
            getFolderById(folderId);

            // get the list of all visible calendar items in the specified folder
            TypedIdList ids = DbMailItem.listCalendarItems(this, type, start, end, folderId, null);
            success = true;
            return ids;
        } finally {
            endTransaction(success);
        }
    }

    public List<CalendarItem> getCalendarItems(OperationContext octxt, MailItem.Type type, int folderId)
            throws ServiceException {
        return getCalendarItemsForRange(octxt, type, -1, -1, folderId, null);
    }

    public List<CalendarItem> getCalendarItemsForRange(OperationContext octxt, long start, long end,
            int folderId, int[] excludeFolders) throws ServiceException {
        return getCalendarItemsForRange(octxt, MailItem.Type.UNKNOWN, start, end, folderId, excludeFolders);
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
     * @param octxt     The {@link OperationContext}.
     * @param type      If MailItem.TYPE_APPOINTMENT, return only appointments.
     *                  If MailItem.TYPE_TASK, return only tasks.
     *                  If MailItem.TYPE_UNKNOWN, return both.
     * @param start     The start time of the range, in milliseconds.
     *                  <tt>-1</tt> means to leave the start time unconstrained.
     * @param end       The end time of the range, in milliseconds.
     *                  <tt>-1</tt> means to leave the end time unconstrained.
     * @param folderId  The folder to search for matching calendar items, or
     *                  {@link #ID_AUTO_INCREMENT} to search all non-Spam and
     *                  Trash folders in the mailbox.
     *
     * @perms {@link ACL#RIGHT_READ} on all returned calendar items.
     * @throws ServiceException */
    public List<CalendarItem> getCalendarItemsForRange(OperationContext octxt, MailItem.Type type,
            long start, long end, int folderId, int[] excludeFolders) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getCalendarItemsForRange", octxt);

            // if they specified a folder, make sure it actually exists
            if (folderId != ID_AUTO_INCREMENT)
                getFolderById(folderId);

            // get the list of all visible calendar items in the specified folder
            List<CalendarItem> calItems = new ArrayList<CalendarItem>();
            List<MailItem.UnderlyingData> invData = DbMailItem.getCalendarItems(this, type, start, end, folderId, excludeFolders);
            for (MailItem.UnderlyingData data : invData) {
                try {
                    CalendarItem calItem = getCalendarItem(data);
                    if (folderId == calItem.getFolderId() || (folderId == ID_AUTO_INCREMENT && calItem.inMailbox())) {
                        if (calItem.canAccess(ACL.RIGHT_READ))
                            calItems.add(calItem);
                    }
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn("Error while retrieving calendar item " + data.id + " in mailbox " + mId + "; skipping item", e);
                }
            }
            success = true;
            return calItems;
        } finally {
            endTransaction(success);
        }
    }

    public List<Integer> getItemListByDates(OperationContext octxt, MailItem.Type type,
            long start, long end, int folderId, boolean descending) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getItemListByDates", octxt);
            List<Integer> msgIds = DbMailItem.getItemListByDates(this, type, start, end, folderId, descending);
            success = true;
            return msgIds;
        } finally {
            endTransaction(success);
        }
    }

    public void writeICalendarForCalendarItems(Writer writer, OperationContext octxt, Collection<CalendarItem> calItems,
        boolean useOutlookCompatMode, boolean ignoreErrors, boolean needAppleICalHacks, boolean trimCalItemsList)
            throws ServiceException {
        writeICalendarForCalendarItems(writer, octxt, calItems, null,
                useOutlookCompatMode, ignoreErrors, needAppleICalHacks, trimCalItemsList, false);
    }

    public void writeICalendarForCalendarItems(Writer writer, OperationContext octxt, Collection<CalendarItem> calItems,
            Folder f, boolean useOutlookCompatMode, boolean ignoreErrors, boolean needAppleICalHacks,
            boolean trimCalItemsList, boolean escapeHtmlTags) throws ServiceException {
        lock.lock();
        try {
            writer.write("BEGIN:VCALENDAR\r\n");
            if (f != null) {
                writer.write("X-WR-CALNAME:");
                writer.write(f.getName());
                writer.write("\r\n");
                writer.write("X-WR-CALID:");
                writer.write(new ItemId(f).toString());
                writer.write("\r\n");
            }

            ZProperty prop;
            prop = new ZProperty(ICalTok.PRODID, ZCalendar.sZimbraProdID);
            prop.toICalendar(writer, needAppleICalHacks);
            prop = new ZProperty(ICalTok.VERSION, ZCalendar.sIcalVersion);
            prop.toICalendar(writer, needAppleICalHacks);
            prop = new ZProperty(ICalTok.METHOD, ICalTok.PUBLISH.toString());
            prop.toICalendar(writer, needAppleICalHacks);

            // timezones
            ICalTimeZone localTz = Util.getAccountTimeZone(getAccount());
            TimeZoneMap tzmap = new TimeZoneMap(localTz);
            for (CalendarItem calItem : calItems) {
                tzmap.add(calItem.getTimeZoneMap());
            }
            // iterate the tzmap and add all the VTimeZone's
            for (Iterator<ICalTimeZone> iter = tzmap.tzIterator(); iter.hasNext(); ) {
                ICalTimeZone tz = iter.next();
                tz.newToVTimeZone().toICalendar(writer, needAppleICalHacks);
            }
            tzmap = null;  // help keep memory consumption low

            // build all the event components and add them to the Calendar
            for (Iterator<CalendarItem> iter = calItems.iterator(); iter.hasNext(); ) {
                CalendarItem calItem = iter.next();
                boolean allowPrivateAccess =
                    calItem.isPublic() ||
                    calItem.allowPrivateAccess(octxt.getAuthenticatedUser(), octxt.isUsingAdminPrivileges());
                if (trimCalItemsList)
                    iter.remove();  // help keep memory consumption low
                Invite[] invites = calItem.getInvites();
                if (invites != null && invites.length > 0) {
                    boolean appleICalExdateHack = LC.calendar_apple_ical_compatible_canceled_instances.booleanValue();
                    ZComponent[] comps = null;
                    try {
                        comps = Invite.toVComponents(invites, allowPrivateAccess,
                                useOutlookCompatMode, appleICalExdateHack);
                    } catch (ServiceException e) {
                        if (ignoreErrors) {
                            ZimbraLog.calendar.warn("Error retrieving iCalendar data for item " +
                                                    calItem.getId() + ": " + e.getMessage(), e);
                        } else {
                            throw e;
                        }
                    }
                    if (comps != null) {
                        for (ZComponent comp : comps) {
                            comp.toICalendar(writer, needAppleICalHacks, escapeHtmlTags);
                        }
                    }
                }
            }

            writer.write("END:VCALENDAR\r\n");
        } catch (IOException e) {
            throw ServiceException.FAILURE("Error writing iCalendar", e);
        } finally {
            lock.release();
        }
    }

    public void writeICalendarForRange(Writer writer, OperationContext octxt, long start, long end, int folderId,
            boolean useOutlookCompatMode, boolean ignoreErrors, boolean needAppleICalHacks) throws ServiceException {
        writeICalendarForRange(writer, octxt, start, end, folderId, useOutlookCompatMode, ignoreErrors,
                needAppleICalHacks, false);
    }

    public void writeICalendarForRange(Writer writer, OperationContext octxt, long start, long end, int folderId,
            boolean useOutlookCompatMode, boolean ignoreErrors, boolean needAppleICalHacks, boolean escapeHtmlTags)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("writeICalendarForRange", octxt);
            Collection<CalendarItem> calItems = getCalendarItemsForRange(octxt, start, end, folderId, null);
            writeICalendarForCalendarItems(writer, octxt, calItems, null, useOutlookCompatMode, ignoreErrors,
                    needAppleICalHacks, true, escapeHtmlTags);
        } finally {
            endTransaction(success);
        }
    }

    public CalendarDataResult getCalendarSummaryForRange(OperationContext octxt, int folderId, MailItem.Type type,
            long start, long end) throws ServiceException {
        lock.lock();
        try {
            Folder folder = getFolderById(folderId);
            if (!folder.canAccess(ACL.RIGHT_READ)) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on folder " + folder.getName());
            }
            return CalendarCacheManager.getInstance().getSummaryCache().getCalendarSummary(octxt, getAccountId(),
                    folderId, type, start, end, true);
        } finally {
            lock.release();
        }
    }

    public List<CalendarDataResult> getAllCalendarsSummaryForRange(OperationContext octxt, MailItem.Type type,
            long start, long end) throws ServiceException {
        boolean success = false;
        try {
            // folder cache is populated in beginTransaction...
            beginTransaction("getAllCalendarsSummaryForRange", octxt);
            success = true;
            List<CalendarDataResult> list = new ArrayList<CalendarDataResult>();
            for (Folder folder : listAllFolders()) {
                if (folder.inTrash() || folder.inSpam()) {
                    continue;
                }
                // Only look at folders of right view type.  We might have to relax this to allow appointments/tasks
                // in any folder, but that requires scanning too many folders each time, most of which don't contain
                // any calendar items.
                if (folder.getDefaultView() != type) {
                    continue;
                }
                if (!folder.canAccess(ACL.RIGHT_READ)) {
                    continue;
                }
                CalendarDataResult result = CalendarCacheManager.getInstance().getSummaryCache().
                    getCalendarSummary(octxt, getAccountId(), folder.getId(), type, start, end, true);
                if (result != null) {
                    list.add(result);
                }
            }
            return list;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * @param octxt
     * @param params
     * @return A "mailbox neutral" representation of the query string: ie one that is re-written so that all Folder names (and other by-name
     *         search parts) are re-written using ID's.  This is useful in some situations where you want to proxy the search
     *         request (since you cannot directly proxy a search request with local folder names in it)
     * @throws IOException
     * @throws ServiceException
     */
    public String getRewrittenQueryString(OperationContext octxt, SearchParams params) throws ServiceException {
        if (octxt == null)
            throw ServiceException.INVALID_REQUEST("The OperationContext must not be null", null);

        // okay, lets run the search through the query parser -- this has the side-effect of
        // re-writing the query in a format that is OK to proxy to the other server
        ZimbraQuery zq = new ZimbraQuery(octxt, SoapProtocol.Soap12, this, params);
        return zq.toQueryString();
    }

    public FreeBusy getFreeBusy(OperationContext octxt, long start, long end, int folder) throws ServiceException {
        return getFreeBusy(octxt, getAccount().getName(), start, end, folder, null);
    }

    public FreeBusy getFreeBusy(OperationContext octxt, long start, long end, Appointment exAppt)
            throws ServiceException {
        return getFreeBusy(octxt, getAccount().getName(), start, end, FreeBusyQuery.CALENDAR_FOLDER_ALL, exAppt);
    }

    public FreeBusy getFreeBusy(OperationContext octxt, String name, long start, long end, int folder)
            throws ServiceException {
        return getFreeBusy(octxt, name, start, end, folder, null);
    }

    public FreeBusy getFreeBusy(OperationContext octxt, String name, long start, long end, int folder,
            Appointment exAppt) throws ServiceException {
        lock.lock();
        try {
            Account authAcct;
            boolean asAdmin;
            if (octxt != null) {
                authAcct = octxt.getAuthenticatedUser();
                asAdmin = octxt.isUsingAdminPrivileges();
            } else {
                authAcct = null;
                asAdmin = false;
            }
            return LocalFreeBusyProvider.getFreeBusyList(authAcct, asAdmin, this, name, start, end, folder, exAppt);
        } finally {
            lock.release();
        }
    }

    public static enum BrowseBy {
        attachments, domains, objects;
    }

    /**
     * Return a list of all the {attachments} or {doamins} or {objects} in this Mailbox, optionally with a prefix string
     * or limited by maximum number.
     *
     * @param max Maximum number of results to return.  0 means "return all results"  If more than max entries exist,
     * only the first max are returned, sorted by frequency.
     */
    public List<BrowseTerm> browse(OperationContext octxt, BrowseBy browseBy, String regex, int max)
            throws IOException, ServiceException {
        boolean success = false;
        try {
            beginTransaction("browse", octxt);
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on this mailbox");
            }
            List<BrowseTerm> result = null;
            switch (browseBy) {
                case attachments:
                    result = index.getAttachmentTypes(regex);
                    break;
                case domains:
                    Map<String, DomainBrowseTerm> domains = new HashMap<String, DomainBrowseTerm>();
                    for (BrowseTerm term : index.getDomains(LuceneFields.L_H_FROM, regex)) {
                        DomainBrowseTerm domain = domains.get(term.getText());
                        if (domain == null) {
                            domain = new DomainBrowseTerm(term);
                            domains.put(term.getText(), domain);
                        }
                        domain.addField(DomainBrowseTerm.Field.FROM);
                    }
                    for (BrowseTerm term : index.getDomains(LuceneFields.L_H_TO, regex)) {
                        DomainBrowseTerm domain = domains.get(term.getText());
                        if (domain == null) {
                            domain = new DomainBrowseTerm(term);
                            domains.put(term.getText(), domain);
                        }
                        domain.addField(DomainBrowseTerm.Field.TO);
                    }
                    for (BrowseTerm term : index.getDomains(LuceneFields.L_H_CC, regex)) {
                        DomainBrowseTerm domain = domains.get(term.getText());
                        if (domain == null) {
                            domain = new DomainBrowseTerm(term);
                            domains.put(term.getText(), domain);
                        }
                        domain.addField(DomainBrowseTerm.Field.CC);
                    }
                    result = new ArrayList<BrowseTerm>(domains.values());
                    break;
                case objects:
                    result = index.getObjects(regex);
                    break;
                default:
                    assert false : browseBy;
            }

            Collections.sort(result, new Comparator<BrowseTerm>() {
                @Override
                public int compare(BrowseTerm o1, BrowseTerm o2) {
                    int retVal = o2.getFreq() - o1.getFreq();
                    if (retVal == 0) {
                        retVal = o1.getText().compareTo(o2.getText());
                    }
                    return retVal;
                }
            });

            if (max > 0 && result.size() > max) {
                result = result.subList(0, max);
            }

            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }

    public void dismissCalendarItemAlarm(OperationContext octxt, int calItemId, long dismissedAt)
            throws ServiceException {
        DismissCalendarItemAlarm redoRecorder = new DismissCalendarItemAlarm(getId(), calItemId, dismissedAt);
        boolean success = false;
        try {
            beginTransaction("dismissAlarm", octxt, redoRecorder);
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(calItemId);
            }
            MailItem itemSnapshot = calItem.snapshotItem();
            calItem.snapshotRevision();
            calItem.updateNextAlarm(dismissedAt + 1, true);
            markItemModified(calItem, Change.INVITE, itemSnapshot);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void snoozeCalendarItemAlarm(OperationContext octxt, int calItemId, long snoozeUntil)
            throws ServiceException {
        SnoozeCalendarItemAlarm redoRecorder = new SnoozeCalendarItemAlarm(getId(), calItemId, snoozeUntil);
        boolean success = false;
        try {
            beginTransaction("snoozeAlarm", octxt, redoRecorder);
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(calItemId);
            }
            MailItem itemSnapshot = calItem.snapshotItem();
            calItem.snapshotRevision();
            calItem.updateNextAlarm(snoozeUntil, false);
            markItemModified(calItem, Change.INVITE, itemSnapshot);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public static final class SetCalendarItemData {
        public Invite invite;
        public ParsedMessage message;

        @Override
        public String toString() {
            return Objects.toStringHelper(this).add("inv", invite).add("hasBody", message != null).toString();
        }
    }

    public static final class AddInviteData {
        public final int calItemId;
        public final int invId;
        public final int compNum;
        public final int modSeq;
        public final int rev;

        private AddInviteData(int calItemId, int invId, int compNum, int modSeq, int rev) {
            this.calItemId = calItemId;
            this.invId = invId;
            this.compNum = compNum;
            this.modSeq = modSeq;
            this.rev = rev;
        }
    }

    /**
     * @param exceptions can be NULL
     * @return calendar item ID
     */
    public CalendarItem setCalendarItem(OperationContext octxt, int folderId, int flags, String[] tags,
            SetCalendarItemData defaultInv, SetCalendarItemData[] exceptions, List<ReplyInfo> replies, long nextAlarm)
    throws ServiceException {
        flags = (flags & ~Flag.FLAGS_SYSTEM);
        SetCalendarItem redoRecorder = new SetCalendarItem(getId(), attachmentsIndexingEnabled(), flags, tags);

        boolean success = false;
        try {
            beginTransaction("setCalendarItem", octxt, redoRecorder);
            Tag.NormalizedTags ntags = new Tag.NormalizedTags(this, tags);

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

            CalendarItem calItem = null;

            // bug 19868: Preserve invId of existing Invites.  We have to do this before making any
            // calls to processNewInvite() because it'll delete all existing Invites and we'll lose
            // old invId information.
            if (!scidList.isEmpty()) {
                calItem = getCalendarItemByUid(scidList.get(0).invite.getUid());
                for (SetCalendarItemData scid : scidList) {
                    int idBeingSet = scid.invite.getMailItemId();
                    if (idBeingSet <= 0) {
                        if (calItem != null) {
                            Invite currInv = calItem.getInvite(scid.invite.getRecurId());
                            if (currInv != null) {
                                scid.invite.setInviteId(currInv.getMailItemId());
                                // Carry over local-only setting.
                                boolean currLO = currInv.isLocalOnly();
                                boolean newLO = scid.invite.isLocalOnly();
                                scid.invite.setLocalOnly(currLO && newLO);
                            } else {
                                scid.invite.setInviteId(getNextItemId(Mailbox.ID_AUTO_INCREMENT));
                            }
                        } else {
                            scid.invite.setInviteId(getNextItemId(Mailbox.ID_AUTO_INCREMENT));
                        }
                    }
                }

                // If modifying an existing calendar item, inherit intended f/b from old version
                // if new version doesn't specify it.  (bug 41002)
                if (calItem != null && calItem.getFolderId() != Mailbox.ID_FOLDER_TRASH) {
                    Invite currSeries = calItem.getDefaultInviteOrNull();
                    for (SetCalendarItemData scid : scidList) {
                        if (!scid.invite.hasFreeBusy()) {
                            Invite currInv = calItem.getInvite(scid.invite.getRecurId());
                            if (currInv == null) { // Inherit from series as fallback.
                                currInv = currSeries;
                            }
                            if (currInv != null && currInv.hasFreeBusy()) {
                                if (scid.invite.isTransparent()) {
                                    // New invite is transparent.  New intended f/b must be free.
                                    scid.invite.setFreeBusy(IcalXmlStrMap.FBTYPE_FREE);
                                } else {
                                    // New invite is opaque.
                                    if (IcalXmlStrMap.FBTYPE_FREE.equals(currInv.getFreeBusy())) {
                                        // An opaque invite cannot have intended f/b value of free.  Make it busy.
                                        scid.invite.setFreeBusy(IcalXmlStrMap.FBTYPE_BUSY);
                                    } else {
                                        // Current intended f/b has a non-free value, so keep it.  It's better
                                        // to preserve tentative or OOO value than to unconditionally change to busy.
                                        scid.invite.setFreeBusy(currInv.getFreeBusy());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // trace logging
            if (!scidList.isEmpty()) {
                Invite invLog = scidList.get(0).invite;
                String idStr = calItem != null ? Integer.toString(calItem.getId()) : "(new)";
                ZimbraLog.calendar.info("setCalendarItem: id=%s, folderId=%d, subject=\"%s\", UID=%s",
                        idStr, folderId, (invLog != null && invLog.isPublic() ? invLog.getName() : "(private)"),
                        invLog.getUid());
            }

            redoRecorder.setData(defaultInv, exceptions, replies, nextAlarm);

            boolean first = true;
            AlarmData oldAlarmData = null;
            for (SetCalendarItemData scid : scidList) {
                if (scid.message == null) {
                    scid.invite.setDontIndexMimeMessage(true); // the MimeMessage is fake, so we don't need to index it
                    String desc = scid.invite.getDescription();
                    if (desc != null && desc.length() > Invite.getMaxDescInMeta()) {
                        MimeMessage mm = CalendarMailSender.createCalendarMessage(scid.invite);
                        scid.message = new ParsedMessage(mm,
                                octxt == null ? System.currentTimeMillis() : octxt.getTimestamp(), true);
                    }
                }

                if (first) {
                    // usually the default invite
                    first = false;
                    if (calItem == null) {
                        // ONLY create an calendar item if this is a REQUEST method...otherwise don't.
                        String method = scid.invite.getMethod();
                        if ("REQUEST".equals(method) || "PUBLISH".equals(method)) {
                            try {
                                calItem = createCalendarItem(folderId, flags, ntags, scid.invite.getUid(), scid.message, scid.invite, null);
                            } catch (MailServiceException e) {
                                if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                                    //bug 49106 - did not find the appointment above in getCalendarItemByUid(), but the mail_item exists
                                    ZimbraLog.calendar.error("failed to create calendar item; already exists. cause: " +
                                            (scidList.isEmpty() ? "no items in uuid list." :
                                                "uuid not found in appointment: " + scidList.get(0).invite.getUid() +
                                                " or bad mail_item type"));
                                }
                                throw e;
                            }
                        } else {
                            return null; // for now, just ignore this Invitation
                        }
                    } else {
                        calItem.snapshotRevision();

                        // Preserve alarm time before any modification is made to the item.
                        if (calItem.getAlarmData() != null) {
                            oldAlarmData = (AlarmData) calItem.getAlarmData().clone();
                        }

                        calItem.setTags(flags, ntags);
                        calItem.processNewInvite(scid.message, scid.invite, folderId, nextAlarm, false, true);
                    }
                    redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId());
                } else {
                    // exceptions
                    calItem.processNewInvite(scid.message, scid.invite, folderId, nextAlarm, false, false);
                }
            }

            // Recompute alarm time after processing all Invites.
            if (nextAlarm == CalendarItem.NEXT_ALARM_KEEP_CURRENT) {
                nextAlarm = oldAlarmData != null ? oldAlarmData.getNextAt() : CalendarItem.NEXT_ALARM_FROM_NOW;
            }
            calItem.updateNextAlarm(nextAlarm, oldAlarmData, false);

            // Override replies list if one is provided.
            // Null list means keep existing replies.  Empty list means to clear existing replies.
            // List with one or more replies means replacing existing replies.
            if (replies != null) {
                calItem.setReplies(replies);
            }
            index.add(calItem);

            success = true;
            return calItem;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Fix up timezone definitions in all appointments/tasks in the mailbox.
     *
     * @param after only fix calendar items that have instances after this time
     */
    public int fixAllCalendarItemTZ(OperationContext octxt, long after, TimeZoneFixupRules fixupRules)
            throws ServiceException {
        int numFixedCalItems = 0;
        int numFixedTZs = 0;
        ZimbraLog.calendar.info("Started: timezone fixup in calendar of mailbox " + getId());
        List<List<MailItem>> lists = new ArrayList<List<MailItem>>(2);
        lists.add(getItemList(octxt, MailItem.Type.APPOINTMENT));
        lists.add(getItemList(octxt, MailItem.Type.TASK));
        for (List<MailItem> items : lists) {
            for (Iterator<MailItem> iter = items.iterator(); iter.hasNext(); ) {
                Object obj = iter.next();
                if (!(obj instanceof CalendarItem))
                    continue;
                CalendarItem calItem = (CalendarItem) obj;
                long end = calItem.getEndTime();
                if (end <= after)
                    continue;
                try {
                    int num = fixCalendarItemTZ(octxt, calItem.getId(), fixupRules);
                    numFixedTZs += num;
                    if (num > 0) {
                        numFixedCalItems++;
                    }
                } catch (ServiceException e) {
                    ZimbraLog.calendar.error("Error fixing calendar item " + calItem.getId() + " in mailbox " +
                            getId() + ": " + e.getMessage(), e);
                }
            }
        }
        ZimbraLog.calendar.info("Finished: timezone fixup in calendar of mailbox " + getId() + "; fixed " +
                numFixedTZs + " timezone entries in " + numFixedCalItems + " calendar items");
        return numFixedCalItems;
    }

    /**
     * Fix up timezone definitions in an appointment/task.  Fixup is
     * required when governments change the daylight savings policy.
     *
     * @param fixupRules rules specifying which timezones to fix and how
     * @return number of timezone objects that were modified
     */
    public int fixCalendarItemTZ(OperationContext octxt, int calItemId, TimeZoneFixupRules fixupRules)
            throws ServiceException {
        FixCalendarItemTZ redoRecorder = new FixCalendarItemTZ(getId(), calItemId);
        boolean success = false;
        try {
            beginTransaction("fixCalendarItemTimeZone2", octxt, redoRecorder);
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            MailItem itemSnapshot = calItem.snapshotItem();
            Map<String, ICalTimeZone> replaced = new HashMap<String, ICalTimeZone>();
            int numFixed = fixupRules.fixCalendarItem(calItem, replaced);
            if (numFixed > 0) {
                ZimbraLog.calendar.info("Fixed " + numFixed + " timezone entries in calendar item " + calItem.getId());
                redoRecorder.setReplacementMap(replaced);
                calItem.snapshotRevision();
                calItem.saveMetadata();
                // Need to uncache and refetch the item because there are fields
                // in the appointment/task that reference the old, pre-fix version
                // of the timezones.  We can either visit them all and update them,
                // or simply invalidate the calendar item and refetch it.
                uncacheItem(calItemId);
                calItem = getCalendarItemById(octxt, calItemId);
                markItemModified(calItem, Change.CONTENT | Change.INVITE, itemSnapshot);
                success = true;

                Callback cb = CalendarItem.getCallback();
                if (cb != null) {
                    cb.modified(calItem);
                }
            }
            return numFixed;
        } finally {
            endTransaction(success);
        }
    }

    public int fixAllCalendarItemEndTime(OperationContext octxt) throws ServiceException {
        int numFixed = 0;
        ZimbraLog.calendar.info("Started: end time fixup in calendar of mailbox " + getId());
        @SuppressWarnings("unchecked")
        List<MailItem>[] lists = new List[2];
        lists[0] = getItemList(octxt, MailItem.Type.APPOINTMENT);
        lists[1] = getItemList(octxt, MailItem.Type.TASK);
        for (List<MailItem> items : lists) {
            for (Iterator<MailItem> iter = items.iterator(); iter.hasNext(); ) {
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
                getId() + "; fixed " + numFixed + " entries");
        return numFixed;
    }

    public int fixCalendarItemEndTime(OperationContext octxt, CalendarItem calItem) throws ServiceException {
        FixCalendarItemEndTime redoRecorder = new FixCalendarItemEndTime(getId(), calItem.getId());
        boolean success = false;
        try {
            beginTransaction("fixupCalendarItemEndTime", octxt, redoRecorder);
            MailItem itemSnapshot = calItem.snapshotItem();
            int numFixed = calItem.fixRecurrenceEndTime();
            if (numFixed > 0) {
                ZimbraLog.calendar.info("Fixed calendar item " + calItem.getId());
                calItem.snapshotRevision();
                calItem.saveMetadata();
                markItemModified(calItem, Change.CONTENT | Change.INVITE, itemSnapshot);
                success = true;
            }
            return numFixed;
        } finally {
            endTransaction(success);
        }
    }

    public int fixAllCalendarItemPriority(OperationContext octxt) throws ServiceException {
        int numFixed = 0;
        ZimbraLog.calendar.info("Started: priority fixup in calendar of mailbox " + getId());
        @SuppressWarnings("unchecked")
        List<MailItem>[] lists = new List[2];
        lists[0] = getItemList(octxt, MailItem.Type.APPOINTMENT);
        lists[1] = getItemList(octxt, MailItem.Type.TASK);
        for (List<MailItem> items : lists) {
            for (Iterator<MailItem> iter = items.iterator(); iter.hasNext(); ) {
                Object obj = iter.next();
                if (!(obj instanceof CalendarItem))
                    continue;
                CalendarItem calItem = (CalendarItem) obj;
                try {
                    numFixed += fixCalendarItemPriority(octxt, calItem);
                } catch (ServiceException e) {
                    ZimbraLog.calendar.error(
                            "Error fixing calendar item " + calItem.getId() +
                            " in mailbox " + getId() + ": " + e.getMessage(), e);
                }
            }
        }
        ZimbraLog.calendar.info(
                "Finished: priority fixup in calendar of mailbox " +
                getId() + "; fixed " + numFixed + " entries");
        return numFixed;
    }

    public int fixCalendarItemPriority(OperationContext octxt, CalendarItem calItem) throws ServiceException {
        FixCalendarItemPriority redoRecorder = new FixCalendarItemPriority(getId(), calItem.getId());
        boolean success = false;
        try {
            beginTransaction("fixupCalendarItemPriority", octxt, redoRecorder);
            MailItem itemSnapshot = calItem.snapshotItem();
            int flags = calItem.mData.getFlags() & ~(Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
            Invite[] invs = calItem.getInvites();
            if (invs != null) {
                for (Invite cur : invs) {
                    String method = cur.getMethod();
                    if (method.equals(ICalTok.REQUEST.toString()) ||
                        method.equals(ICalTok.PUBLISH.toString())) {
                        if (cur.isHighPriority())
                            flags |= Flag.BITMASK_HIGH_PRIORITY;
                        if (cur.isLowPriority())
                            flags |= Flag.BITMASK_LOW_PRIORITY;
                    }
                }
            }
            int numFixed = 0;
            if (flags != calItem.mData.getFlags()) {
                ZimbraLog.calendar.info("Fixed calendar item " + calItem.getId());
                calItem.mData.setFlags(flags);
                calItem.snapshotRevision();
                calItem.saveMetadata();
                markItemModified(calItem, Change.INVITE, itemSnapshot);
                success = true;
                numFixed = 1;
            }
            return numFixed;
        } finally {
            endTransaction(success);
        }
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId)
            throws ServiceException {
        boolean addRevision = true;  // Always rev the calendar item.
        return addInvite(octxt, inv, folderId, null, false, false, addRevision);
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, ParsedMessage pm)
            throws ServiceException {
        boolean addRevision = true;  // Always rev the calendar item.
        return addInvite(octxt, inv, folderId, pm, false, false, addRevision);
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, boolean preserveExistingAlarms,
            boolean addRevision) throws ServiceException {
        return addInvite(octxt, inv, folderId, null, preserveExistingAlarms, false, addRevision);
    }

    /**
     * Directly add an Invite into the system...this process also gets triggered when we add a Message
     * that has a text/calendar Mime part: but this API is useful when you don't want to add a corresponding
     * message.
     * @param octxt
     * @param inv
     * @param pm NULL is OK here
     * @param preserveExistingAlarms
     * @param discardExistingInvites
     * @param addRevision if true and revisioning is enabled and calendar item exists already, add a revision
     *                    with current snapshot of the calendar item
     *
     * @return AddInviteData
     * @throws ServiceException
     */
    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, ParsedMessage pm,
            boolean preserveExistingAlarms, boolean discardExistingInvites, boolean addRevision)
            throws ServiceException {
        if (pm == null) {
            inv.setDontIndexMimeMessage(true); // the MimeMessage is fake, so we don't need to index it
            String desc = inv.getDescription();
            if (desc != null && desc.length() > Invite.getMaxDescInMeta()) {
                MimeMessage mm = CalendarMailSender.createCalendarMessage(inv);
                pm = new ParsedMessage(mm, octxt == null ? System.currentTimeMillis() : octxt.getTimestamp(), true);
            }
        }

        byte[] data = null;
        try {
            if (pm != null) {
                data = pm.getRawData();
            }
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("Caught IOException", ioe);
        }

        CreateInvite redoRecorder =
            new CreateInvite(mId, inv, folderId, data, preserveExistingAlarms, discardExistingInvites, addRevision);

        boolean success = false;
        try {
            beginTransaction("addInvite", octxt, redoRecorder);
            CreateInvite redoPlayer = (octxt == null ? null : (CreateInvite) octxt.getPlayer());

            if (redoPlayer == null || redoPlayer.getCalendarItemId() == 0) {
                int currId = inv.getMailItemId();
                if (currId <= 0) {
                    currId = Mailbox.ID_AUTO_INCREMENT;
                }
                inv.setInviteId(getNextItemId(currId));
            }

            CalendarItem calItem = getCalendarItemByUid(inv.getUid());
            boolean processed = true;
            if (calItem == null) {
                // ONLY create an calendar item if this is a REQUEST method...otherwise don't.
                if (inv.getMethod().equals("REQUEST") || inv.getMethod().equals("PUBLISH")) {
                    calItem = createCalendarItem(folderId, 0, null, inv.getUid(), pm, inv, null);
                } else {
                    return null; // for now, just ignore this Invitation
                }
            } else {
                if (!checkItemChangeID(calItem)) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }
                if (inv.getMethod().equals("REQUEST") || inv.getMethod().equals("PUBLISH")) {
                    // Preserve invId.  (bug 19868)
                    Invite currInv = calItem.getInvite(inv.getRecurId());
                    if (currInv != null) {
                        inv.setInviteId(currInv.getMailItemId());
                    }
                }
                if (addRevision) {
                    calItem.snapshotRevision();
                }
                processed = calItem.processNewInvite(pm, inv, folderId, CalendarItem.NEXT_ALARM_KEEP_CURRENT,
                        preserveExistingAlarms, discardExistingInvites);
            }

            if (Invite.isOrganizerMethod(inv.getMethod())) { // Don't update the index for replies. (bug 55317)
                index.add(calItem);
            }

            redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId());

            success = true;
            if (processed) {
                return new AddInviteData(calItem.getId(), inv.getMailItemId(), inv.getComponentNum(),
                        calItem.getModifiedSequence(), calItem.getSavedSequence());
            } else {
                return null;
            }
        } finally {
            endTransaction(success);
        }
    }

    public CalendarItem getCalendarItemByUid(String uid) throws ServiceException {
        return getCalendarItemByUid(null, uid);
    }

    public CalendarItem getCalendarItemByUid(OperationContext octxt, String uid) throws ServiceException {
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

    public Map<String, CalendarItem> getCalendarItemsByUid(OperationContext octxt, List<String> uids)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getCalendarItemsByUid", octxt);
            ArrayList<String> uidList = new ArrayList<String>(uids);
            Map<String,CalendarItem> calItems = new HashMap<String,CalendarItem>();
            List<MailItem.UnderlyingData> invData = DbMailItem.getCalendarItems(this, uids);
            for (MailItem.UnderlyingData data : invData) {
                try {
                    CalendarItem calItem = getCalendarItem(data);
                    calItems.put(calItem.getUid(), calItem);
                    uidList.remove(calItem.getUid());
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn("Error while retrieving calendar item %d; skipping item", data.id, e);
                }
            }
            success = true;
            for (String missingUid : uidList) {
                calItems.put(missingUid, null);
            }
            return calItems;
        } finally {
            endTransaction(success);
        }
    }

    private boolean dedupe(MimeMessage mm) throws ServiceException {
        Account acct = getAccount();
        switch (acct.getPrefDedupeMessagesSentToSelf()) {
            case dedupeAll:
                return true;

            case secondCopyifOnToOrCC:
                try {
                    return !AccountUtil.isDirectRecipient(acct, mm);
                } catch (Exception e) {
                    return false;
                }

            case dedupeNone:
            default:
                return false;
        }
    }

    public int getConversationIdFromReferent(MimeMessage newMsg, int parentID) {
        try {
            // file into same conversation as parent message as long as subject hasn't really changed
            Message parentMsg = getMessageById(null, parentID);
            if (parentMsg.getNormalizedSubject().equals(ParsedMessage.normalize(Mime.getSubject(newMsg)))) {
                return parentMsg.getConversationId();
            }
        } catch (Exception e) {
            if (!(e instanceof MailServiceException.NoSuchItemException)) {
                ZimbraLog.mailbox.warn("ignoring error while checking conversation: %d", parentID, e);
            }
        }
        return ID_AUTO_INCREMENT;
    }

    /**
     * Process an iCalendar REPLY containing a single VEVENT or VTODO.
     *
     * @param inv REPLY iCalendar object
     */
    public void processICalReply(OperationContext octxt, Invite inv) throws ServiceException {
        ICalReply redoRecorder = new ICalReply(getId(), inv);
        boolean success = false;
        try {
            beginTransaction("iCalReply", octxt, redoRecorder);
            String uid = inv.getUid();
            CalendarItem calItem = getCalendarItemByUid(uid);
            if (calItem == null) {
                ZimbraLog.calendar.warn("Unknown calendar item UID %d", uid);
                return;
            }
            calItem.snapshotRevision();
            calItem.processNewInviteReply(inv);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    private AuthToken getAuthToken(OperationContext octxt) throws ServiceException {
        AuthToken authToken = octxt == null ? null : octxt.getAuthToken();

        if (authToken == null) {
            Account authuser = octxt == null ? getAccount() : octxt.getAuthenticatedUser();
            boolean isAdminRequest = octxt == null ? false : octxt.isUsingAdminPrivileges();
            authToken = AuthProvider.getAuthToken(authuser, isAdminRequest);
        }
        return authToken;
    }

    private void processICalReplies(OperationContext octxt, ZVCalendar cal)
    throws ServiceException {
        // Reply from Outlook will usually have PRODID set to the following:
        //
        // Outlook2007+ZCO: PRODID:-//Microsoft Corporation//Outlook 12.0 MIMEDIR//EN
        // Outlook2010+ZCO: PRODID:-//Microsoft Corporation//Outlook 14.0 MIMEDIR//EN
        // Outlook20xx+Exchange: PRODID:Microsoft Exchange Server 2007
        //   (if Exchange is Exchange 2007; Exchange 2010 probably works similarly)
        //
        // Lowest common denominator is "Microsoft" substring.
        String prodId = cal.getPropVal(ICalTok.PRODID, null);
        boolean fromOutlook = prodId != null && prodId.toLowerCase().contains("microsoft");

        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(getAccount());
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
            if (acctMatcher.matches(orgAddress)) {
                // bug 62042: Fixup bad RECURRENCE-ID sent by Outlook when replying to an orphan instance.
                // Date is correct relative to the organizer's time zone, but time is set to 000000 and
                // the time zone is set to the attendee's time zone.  Fix it up by taking the series DTSTART
                // in the organizer's appointment and replacing the date part with the value from supplied
                // RECURRENCE-ID.
                if (fromOutlook && !inv.isAllDayEvent() && inv.hasRecurId()) {
                    RecurId rid = inv.getRecurId();
                    if (rid.getDt() != null && rid.getDt().hasZeroTime()) {
                        CalendarItem calItem = getCalendarItemByUid(octxt, inv.getUid());
                        if (calItem != null) {
                            Invite seriesInv = calItem.getDefaultInviteOrNull();
                            if (seriesInv != null) {
                                ParsedDateTime seriesDtStart = seriesInv.getStartTime();
                                if (seriesDtStart != null) {
                                    ParsedDateTime fixedDt = seriesDtStart.cloneWithNewDate(rid.getDt());
                                    RecurId fixedRid = new RecurId(fixedDt, rid.getRange());
                                    ZimbraLog.calendar.debug("Fixed up invalid RECURRENCE-ID with zero time; before=[%s], after=[%s]",
                                            rid, fixedRid);
                                    inv.setRecurId(fixedRid);
                                }
                            }
                        }
                    }
                }
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
                        ZimbraLog.calendar.warn("Unable to determine URI for organizer account %s", orgAddress);
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
                        options.setAuthToken(getAuthToken(octxt).toZAuthToken());
                        options.setTargetAccount(orgAccount.getName());
                        options.setTargetAccountBy(AccountBy.name);
                        options.setUri(uri);
                        options.setNoSession(true);
                        ZMailbox zmbox = ZMailbox.getMailbox(options);
                        zmbox.iCalReply(ical);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Error while posting REPLY to organizer mailbox host", e);
                    }
                }
            }
        }
    }

    public Message addMessage(OperationContext octxt, InputStream in, int sizeHint, Long receivedDate, DeliveryOptions dopt, DeliveryContext dctxt)
    throws IOException, ServiceException {
        int bufLen = Provisioning.getInstance().getLocalServer().getMailDiskStreamingThreshold();
        CopyInputStream cs = new CopyInputStream(in, sizeHint, bufLen, bufLen);
        in = cs;
        Blob blob = null;

        try {
            BufferStream bs = cs.getBufferStream();
            ParsedMessage pm = null;

            Rfc822ValidationInputStream validator = null;
            if (LC.zimbra_lmtp_validate_messages.booleanValue()) {
                validator = new Rfc822ValidationInputStream(cs, LC.zimbra_lmtp_max_line_length.longValue());
                in = validator;
            }

            blob = StoreManager.getInstance().storeIncoming(in, null);

            if (validator != null && !validator.isValid()) {
                StoreManager.getInstance().delete(blob);
                throw ServiceException.INVALID_REQUEST("Message content is invalid.", null);
            }

            pm = new ParsedMessage(new ParsedMessageOptions(blob, bs.isPartial() ? null : bs.getBuffer(), receivedDate, attachmentsIndexingEnabled()));
            cs.release();
            if (dctxt == null) {
                dctxt = new DeliveryContext();
            }
            dctxt.setIncomingBlob(blob);
            return addMessage(octxt, pm, dopt, dctxt);
        } finally {
            cs.release();
            StoreManager.getInstance().quietDelete(blob);
        }
    }

    public Message addMessage(OperationContext octxt, ParsedMessage pm, DeliveryOptions dopt, DeliveryContext dctxt)
    throws IOException, ServiceException {
        return addMessage(octxt, pm, dopt, dctxt, null);
    }

    public Message addMessage(OperationContext octxt, ParsedMessage pm, DeliveryOptions dopt, DeliveryContext dctxt, Message.DraftInfo dinfo)
    throws IOException, ServiceException {
        return addMessage(octxt, pm, dopt.getFolderId(), dopt.getNoICal(), dopt.getFlags(), dopt.getTags(),
                          dopt.getConversationId(), dopt.getRecipientEmail(), dinfo, dopt.getCustomMetadata(), dctxt);
    }

    private Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags,
            String[] tags, int conversationId, String rcptEmail, Message.DraftInfo dinfo, CustomMetadata customData,
            DeliveryContext dctxt)
    throws IOException, ServiceException {

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
                CalendarPartInfo cpi = pm.getCalendarPartInfo();
                if (cpi != null && CalendarItem.isAcceptableInvite(getAccount(), cpi)) {
                    if (ICalTok.REPLY.equals(cpi.method)) {
                        processICalReplies(octxt, cpi.cal);
                    }
                }
            } catch (Exception e) {
                ZimbraLog.calendar.warn("Error during calendar processing.  Continuing with message add", e);
            }
        }

        // Store the incoming blob if necessary.
        if (dctxt == null) {
            dctxt = new DeliveryContext();
        }

        StoreManager sm = StoreManager.getInstance();
        Blob blob = dctxt.getIncomingBlob();
        boolean deleteIncoming = false;

        if (blob == null) {
            InputStream in = null;
            try {
                in = pm.getRawInputStream();
                blob = sm.storeIncoming(in, null);
            } finally {
                ByteUtil.closeStream(in);
            }
            dctxt.setIncomingBlob(blob);
            deleteIncoming = true;
        }

        StagedBlob staged = sm.stage(blob, this);

        lock.lock();
        try {
            try {
                return addMessageInternal(octxt, pm, folderId, noICal, flags, tags, conversationId,
                        rcptEmail, dinfo, customData, dctxt, staged);
            } finally {
                if (deleteIncoming) {
                    sm.quietDelete(dctxt.getIncomingBlob());
                }
                sm.quietDelete(staged);
            }
        } finally {
            lock.release();
            ZimbraPerf.STOPWATCH_MBOX_ADD_MSG.stop(start);
        }
    }

    private Message addMessageInternal(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal,
            int flags, String[] tags, int conversationId, String rcptEmail, Message.DraftInfo dinfo,
            CustomMetadata customData, DeliveryContext dctxt, StagedBlob staged)
    throws IOException, ServiceException {
        assert lock.isLocked();
        if (pm == null) {
            throw ServiceException.INVALID_REQUEST("null ParsedMessage when adding message to mailbox " + mId, null);
        }

        if (Math.abs(conversationId) <= HIGHEST_SYSTEM_ID) {
            conversationId = ID_AUTO_INCREMENT;
        }

        boolean needRedo = needRedo(octxt);
        CreateMessage redoPlayer = (octxt == null ? null : (CreateMessage) octxt.getPlayer());
        boolean isRedo = redoPlayer != null;

        Blob blob = dctxt.getIncomingBlob();
        if (blob == null) {
            throw ServiceException.FAILURE("Incoming blob not found.", null);
        }

        // make sure we're parsing headers using the target account's charset
        pm.setDefaultCharset(getAccount().getPrefMailDefaultCharset());

        // quick check to make sure we don't deliver 5 copies of the same message
        String msgidHeader = pm.getMessageID();
        boolean isSent = ((flags & Flag.BITMASK_FROM_ME) != 0);
        boolean checkDuplicates = (!isRedo && msgidHeader != null);
        if (checkDuplicates && !isSent && mSentMessageIDs.containsKey(msgidHeader)) {
            Integer sentMsgID = mSentMessageIDs.get(msgidHeader);
            // if the deduping rules say to drop this duplicated incoming message, return null now...
            //   ... but only dedupe messages not carrying a calendar part
            CalendarPartInfo cpi = pm.getCalendarPartInfo();
            if (cpi == null || !CalendarItem.isAcceptableInvite(getAccount(), cpi)) {
                if (dedupe(pm.getMimeMessage())) {
                    ZimbraLog.mailbox.info("not delivering message with Message-ID %s because it is a duplicate of sent message %d",
                            msgidHeader, sentMsgID);
                    return null;
                }
            }
            // if we're not dropping the new message, see if it goes in the same conversation as the old sent message
            if (conversationId == ID_AUTO_INCREMENT) {
                conversationId = getConversationIdFromReferent(pm.getMimeMessage(), sentMsgID.intValue());
                ZimbraLog.mailbox.debug("duplicate detected but not deduped (%s); will try to slot into conversation %d",
                        msgidHeader, conversationId);
            }
        }

        // caller can't set system flags other than \Draft and \Sent
        flags &= ~Flag.FLAGS_SYSTEM | Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME;
        // caller can't specify non-message flags
        flags &= Flag.FLAGS_GENERIC | Flag.FLAGS_MESSAGE;

        String digest;
        int msgSize;
        try {
            digest = blob.getDigest();
            msgSize = (int) blob.getRawSize();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to get message properties.", e);
        }

        CreateMessage redoRecorder = new CreateMessage(mId, rcptEmail, pm.getReceivedDate(), dctxt.getShared(),
                digest, msgSize, folderId, noICal, flags, tags, customData);
        StoreIncomingBlob storeRedoRecorder = null;

        // strip out unread flag for internal storage (don't do this before redoRecorder initialization)
        boolean unread = (flags & Flag.BITMASK_UNREAD) > 0;
        flags &= ~Flag.BITMASK_UNREAD;

        // "having attachments" is currently tracked via flags
        if (pm.hasAttachments()) {
            flags |= Flag.BITMASK_ATTACHED;
        } else {
            flags &= ~Flag.BITMASK_ATTACHED;
        }

        // priority is calculated from headers
        flags &= ~(Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
        flags |= pm.getPriorityBitmask();

        boolean isSpam = folderId == ID_FOLDER_SPAM;
        boolean isDraft = (flags & Flag.BITMASK_DRAFT) != 0;

        // draft replies get slotted in the same conversation as their parent, if possible
        if (isDraft && !isRedo && conversationId == ID_AUTO_INCREMENT && dinfo != null &&
                !Strings.isNullOrEmpty(dinfo.origId)) {
            try {
                ItemId iid = new ItemId(dinfo.origId, getAccountId());
                if (iid.getId() > 0 && iid.belongsTo(this)) {
                    conversationId = getMessageById(octxt, iid.getId()).getConversationId();
                }
            } catch (ServiceException e) {
            }
        }

        Message msg = null;
        boolean success = false;

        CustomMetadata.CustomMetadataList extended = MetadataCallback.preDelivery(pm);
        if (customData != null) {
            if (extended == null) {
                extended = customData.asList();
            } else {
                extended.addSection(customData);
            }
        }

        Threader threading = pm.getThreader(this);
        String subject = pm.getNormalizedSubject();

        try {
            beginTransaction("addMessage", octxt, redoRecorder);
            if (isRedo) {
                rcptEmail = redoPlayer.getRcptEmail();
            }

            Tag.NormalizedTags ntags = new Tag.NormalizedTags(this, tags);

            Folder folder = getFolderById(folderId);

            // step 0: preemptively check for quota issues (actual update is done in Message.create)
            if (!getAccount().isMailAllowReceiveButNotSendWhenOverQuota()) {
                checkSizeChange(getSize() + staged.getSize());
            }

            // step 1: get an ID assigned for the new message
            int messageId = getNextItemId(!isRedo ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());

            List<Conversation> mergeConvs = null;
            if (isRedo) {
                conversationId = redoPlayer.getConvId();

                // fetch the conversations that were merged in as a result of the original delivery...
                List<Integer> mergeConvIds = redoPlayer.getMergedConvIds();
                mergeConvs = new ArrayList<Conversation>(mergeConvIds.size());
                for (int mergeId : mergeConvIds) {
                    try {
                        mergeConvs.add(getConversationById(mergeId));
                    } catch (NoSuchItemException nsie) {
                        ZimbraLog.mailbox.debug("could not find merge conversation %d", mergeId);
                    }
                }
            }

            // step 2: figure out where the message belongs
            Conversation conv = null;
            if (threading.isEnabled()) {
                boolean isReply = pm.isReply();
                if (conversationId != ID_AUTO_INCREMENT) {
                    try {
                        // fetch the requested conversation
                        //   (we'll ensure that it's receiving new mail after the new message is added to it)
                        conv = getConversationById(conversationId);
                        ZimbraLog.mailbox.debug("fetched explicitly-specified conversation %d", conv.getId());
                    } catch (NoSuchItemException nsie) {
                        if (!isRedo) {
                            ZimbraLog.mailbox.debug("could not find explicitly-specified conversation %d",
                                    conversationId);
                            conversationId = ID_AUTO_INCREMENT;
                        }
                    }
                } else if (!isRedo && !isSpam && (isReply || (!isSent && !subject.isEmpty()))) {
                    List<Conversation> matches = threading.lookupConversation();
                    if (matches != null && !matches.isEmpty()) {
                        // file the message into the largest conversation, then later merge any other matching convs
                        Collections.sort(matches, new MailItem.SortSizeDescending());
                        conv = matches.remove(0);
                        mergeConvs = matches;
                    }
                }
            }

            // step 3: create the message and update the cache
            //         and if the message is also an invite, deal with the calendar item
            Conversation convTarget = conv instanceof VirtualConversation ? null : conv;
            if (convTarget != null) {
                ZimbraLog.mailbox.debug("  placing message in existing conversation %d", convTarget.getId());
            }

            CalendarPartInfo cpi = pm.getCalendarPartInfo();
            ZVCalendar iCal = null;
            if (cpi != null && CalendarItem.isAcceptableInvite(getAccount(), cpi)) {
                iCal = cpi.cal;
            }
            msg = Message.create(messageId, folder, convTarget, pm, staged, unread, flags, ntags, dinfo, noICal, iCal, extended);

            redoRecorder.setMessageId(msg.getId());

            // step 4: create a conversation for the message, if necessary
            if (threading.isEnabled() && convTarget == null) {
                if (conv == null && conversationId == ID_AUTO_INCREMENT) {
                    conv = VirtualConversation.create(this, msg);
                    ZimbraLog.mailbox.debug("placed message %d in vconv %d", msg.getId(), conv.getId());
                    redoRecorder.setConvFirstMsgId(-1);
                } else {
                    Message[] contents = null;
                    VirtualConversation vconv = null;
                    if (!isRedo) {
                        vconv = (VirtualConversation) conv;
                        contents = (vconv == null ? new Message[] { msg } : new Message[] { vconv.getMessage(), msg });
                    } else {
                        // Executing redo.
                        int convFirstMsgId = redoPlayer.getConvFirstMsgId();
                        Message convFirstMsg = null;
                        // If there was a virtual conversation, then...
                        if (convFirstMsgId > 0) {
                            try {
                                convFirstMsg = getMessageById(octxt, redoPlayer.getConvFirstMsgId());
                            } catch (MailServiceException e) {
                                if (!MailServiceException.NO_SUCH_MSG.equals(e.getCode())) {
                                    throw e;
                                }
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
                        if (contents == null) {
                            contents = new Message[] { msg };
                        }
                    }
                    redoRecorder.setConvFirstMsgId(vconv != null ? vconv.getMessageId() : -1);
                    conv = createConversation(conversationId, contents);
                    if (vconv != null) {
                        ZimbraLog.mailbox.debug("removed vconv %d", vconv.getId());
                        vconv.removeChild(vconv.getMessage());
                    }
                    // if we're threading by references and promoting a virtual conversation to a real one,
                    //   associate the first message's reference hashes with the new conversation
                    if (contents.length == 2) {
                        threading.changeThreadingTargets(contents[0], conv);
                    }
                }
            } else {
                // conversation feature turned off
                redoRecorder.setConvFirstMsgId(-1);
            }
            redoRecorder.setConvId(conv != null && !(conv instanceof VirtualConversation) ? conv.getId() : -1);
            // if we're threading by references, associate the new message's reference hashes with its conversation
            if (!isSpam && !isDraft) {
                threading.recordAddedMessage(conv);
            }

            if (conv != null && mergeConvs != null) {
                redoRecorder.setMergedConversations(mergeConvs);
                for (Conversation smaller : mergeConvs) {
                    ZimbraLog.mailbox.info("merging conversation %d for references threading", smaller.getId());
                    conv.merge(smaller);
                }
            }

            // step 5: write the redolog entries
            if (dctxt.getShared()) {
                if (dctxt.isFirst() && needRedo) {
                    // Log entry in redolog for blob save.  Blob bytes are logged in the StoreIncoming entry.
                    // Subsequent CreateMessage ops will reference this blob.
                    storeRedoRecorder = new StoreIncomingBlob(digest, msgSize, dctxt.getMailboxIdList());
                    storeRedoRecorder.start(getOperationTimestampMillis());
                    storeRedoRecorder.setBlobBodyInfo(blob.getFile());
                    storeRedoRecorder.log();
                }
                // Link to the file created by StoreIncomingBlob.
                redoRecorder.setMessageLinkInfo(blob.getPath());
            } else {
                // Store the blob data inside the CreateMessage op.
                redoRecorder.setMessageBodyInfo(blob.getFile());
            }

            // step 6: link to existing blob
            MailboxBlob mblob = StoreManager.getInstance().link(staged, this, messageId, getOperationChangeID());
            markOtherItemDirty(mblob);
            // when we created the Message, we used the staged locator/size/digest;
            //   make sure that data actually matches the final blob in the store
            msg.updateBlobData(mblob);

            if (dctxt.getMailboxBlob() == null) {
                // Set mailbox blob for in case we want to add the message to the
                // message cache after delivery.
                dctxt.setMailboxBlob(mblob);
            }

            // step 7: queue new message for indexing
            index.add(msg);
            success = true;

            // step 8: send lawful intercept message
            try {
                Notification.getInstance().interceptIfNecessary(this, pm.getMimeMessage(), "add message", folder);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("unable to send legal intercept message", e);
            }
        } finally {
            if (storeRedoRecorder != null) {
                if (success) {
                    storeRedoRecorder.commit();
                } else {
                    storeRedoRecorder.abort();
                }
            }

            endTransaction(success);

            if (success) {
                // Everything worked.  Update the blob field in ParsedMessage
                // so the next recipient in the multi-recipient case will link
                // to this blob as opposed to saving its own copy.
                dctxt.setFirst(false);
            }
        }

        // step 8: remember the Message-ID header so that we can avoid receiving duplicates
        if (isSent && checkDuplicates) {
            mSentMessageIDs.put(msgidHeader, msg.getId());
        }

        return msg;
    }

    public List<Conversation> lookupConversation(ParsedMessage pm) throws ServiceException {
        boolean success = false;
        beginTransaction("lookupConversation", null);
        try {
            List<Conversation> result = pm.getThreader(this).lookupConversation();
            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }

    public static String getHash(String subject) {
        try {
            return ByteUtil.getSHA1Digest(Strings.nullToEmpty(subject).getBytes("utf-8"), true);
        } catch (UnsupportedEncodingException uee) {
            return ByteUtil.getSHA1Digest(Strings.nullToEmpty(subject).getBytes(), true);
        }
    }

    // please keep this package-visible but not public
    void openConversation(Conversation conv, String subjectHash) throws ServiceException {
        String hash = subjectHash != null ? subjectHash : getHash(conv.getNormalizedSubject());
        conv.open(hash);
        markOtherItemDirty(hash);
        mConvHashes.put(hash, new Integer(conv.getId()));
    }

    // please keep this package-visible but not public
    void closeConversation(Conversation conv, String subjectHash) throws ServiceException {
        String hash = subjectHash != null ? subjectHash : getHash(conv.getNormalizedSubject());
        conv.close(hash);
        mConvHashes.remove(hash);
    }

    // please keep this package-visible but not public
    Conversation createConversation(int convId, Message... contents) throws ServiceException {
        int id = Math.max(convId, ID_AUTO_INCREMENT);
        Conversation conv = Conversation.create(this, getNextItemId(id), contents);
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contents.length; i++) {
                sb.append(i == 0 ? "" : ",").append(contents[i].getId());
            }
            ZimbraLog.mailbox.debug("  created conv " + conv.getId() + " holding msg(s): " + sb);
        }
        return conv;
    }

    public Message saveDraft(OperationContext octxt, ParsedMessage pm, int id) throws IOException, ServiceException {
        return saveDraft(octxt, pm, id, null, null, null, null, 0);
    }

    /**
     * Ensures that we don't attempt multiple
     * SaveDraft operations for the same mailbox at the same time (bug 59287).  We can
     * remove this synchronization after bug 59512 is fixed.
     */
    private final Object saveDraftGuard = new Object();

    /**
     * Saves draft.
     *
     * @param autoSendTime time at which the draft needs to be auto-sent. Note that this method does not schedule
     *                     the task for auto-sending the draft. It just persists this time for tracking purposes.
     * @see com.zimbra.cs.service.mail.SaveDraft#handle(com.zimbra.common.soap.Element, java.util.Map)
     */
    public Message saveDraft(OperationContext octxt, ParsedMessage pm, int id, String origId, String replyType,
            String identityId, String accountId, long autoSendTime)
    throws IOException, ServiceException {
        synchronized (saveDraftGuard) {
            return saveDraftInternal(octxt, pm, id, origId, replyType, identityId, accountId, autoSendTime);
        }
    }

    public Message saveDraftInternal(OperationContext octxt, ParsedMessage pm, int id,
            String origId, String replyType, String identityId, String accountId, long autoSendTime)
            throws IOException, ServiceException {
        Message.DraftInfo dinfo = null;
        if ((replyType != null && origId != null) || identityId != null || accountId != null || autoSendTime != 0) {
            dinfo = new Message.DraftInfo(replyType, origId, identityId, accountId, autoSendTime);
        }

        if (id == ID_AUTO_INCREMENT) {
            // special-case saving a new draft
            return addMessage(octxt, pm, ID_FOLDER_DRAFTS, true, Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME,
                              null, ID_AUTO_INCREMENT, ":API:", dinfo, null, null);
        }

        // write the draft content directly to the mailbox's blob staging area
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged;
        InputStream is = pm.getRawInputStream();
        try {
            staged = sm.stage(is, null, this);
        } finally {
            ByteUtil.closeStream(is);
        }

        String digest = staged.getDigest();
        int size = (int) staged.getSize();

        SaveDraft redoRecorder = new SaveDraft(mId, id, digest, size);
        InputStream redoStream = null;

        boolean success = false;
        try {
            beginTransaction("saveDraft", octxt, redoRecorder);
            SaveDraft redoPlayer = (SaveDraft) currentChange.getRedoPlayer();

            Message msg = getMessageById(id);
            if (!msg.isTagged(Flag.FlagInfo.DRAFT)) {
                throw MailServiceException.IMMUTABLE_OBJECT(id);
            }
            if (!checkItemChangeID(msg)) {
                throw MailServiceException.MODIFY_CONFLICT();
            }
            // content changed, so we're obliged to change the IMAP uid
            int imapID = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapId());
            redoRecorder.setImapId(imapID);
            redoRecorder.setMessageBodyInfo(new ParsedMessageDataSource(pm), size);

            msg.setDraftAutoSendTime(autoSendTime);

                if (dinfo != null) {
                    if (replyType != null) {
                        msg.setDraftReplyType(replyType);
                    }
                    if (origId != null) {
                        msg.setDraftOrigId(origId);
                    }
                    if (identityId != null) {
                        msg.setDraftIdentityId(identityId);
                    }
                    if (accountId != null) {
                        msg.setDraftAccountId(accountId);
                    }
                    if (autoSendTime != 0) {
                        msg.setDraftAutoSendTime(autoSendTime);
                    }
                }

            // update the content and increment the revision number
            msg.setContent(staged, pm);

            index.add(msg);

            success = true;

            try {
                Notification.getInstance().interceptIfNecessary(this, pm.getMimeMessage(), "save draft", msg.getFolder());
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("Unable to send lawful intercept message.", e);
            }

            return msg;
        } finally {
            endTransaction(success);

            ByteUtil.closeStream(redoStream);
            sm.quietDelete(staged);
        }
    }

    /**
     * Modify the Participant-Status of your LOCAL data part of an calendar item -- this is used when you Reply to
     * an Invite so that you can track the fact that you've replied to it.
     */
    public void modifyPartStat(OperationContext octxt, int calItemId, RecurId recurId, String cnStr, String addressStr,
            String cutypeStr, String roleStr, String partStatStr, Boolean rsvp, int seqNo, long dtStamp)
            throws ServiceException {

        ModifyInvitePartStat redoRecorder = new ModifyInvitePartStat(mId, calItemId, recurId, cnStr, addressStr,
                cutypeStr, roleStr, partStatStr, rsvp, seqNo, dtStamp);

        boolean success = false;
        try {
            beginTransaction("updateInvitePartStat", octxt, redoRecorder);
            CalendarItem calItem = getCalendarItemById(calItemId);
            MailItem itemSnapshot = calItem.snapshotItem();
            Account acct = getAccount();
            calItem.modifyPartStat(acct, recurId, cnStr, addressStr, cutypeStr, roleStr, partStatStr, rsvp, seqNo, dtStamp);
            markItemModified(calItem, Change.INVITE, itemSnapshot);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public List<Integer> resetImapUid(OperationContext octxt, List<Integer> itemIds) throws ServiceException {
        SetImapUid redoRecorder = new SetImapUid(mId, itemIds);

        List<Integer> newIds = new ArrayList<Integer>();
        boolean success = false;
        try {
            beginTransaction("resetImapUid", octxt, redoRecorder);
            SetImapUid redoPlayer = (SetImapUid) currentChange.getRedoPlayer();

            for (int id : itemIds) {
                MailItem item = getItemById(id, MailItem.Type.UNKNOWN);
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

    /**
     * Resets all INDEX_ID in MAIL_ITEM table. The caller must hold the mailbox lock.
     */
    void resetIndex() throws ServiceException {
        assert(lock.isLocked());

        boolean success = false;
        try {
            beginTransaction("resetIndex", null);
            DbMailItem.resetIndexId(getOperationConnection(), this);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void setColor(OperationContext octxt, int itemId, MailItem.Type type, byte color) throws ServiceException {
        setColor(octxt, new int[] { itemId }, type, color);
    }

    public void setColor(OperationContext octxt, int[] itemIds, MailItem.Type type, byte color)
    throws ServiceException {
        setColor(octxt, itemIds, type, new Color(color));
    }

    public void setColor(OperationContext octxt, int[] itemIds, MailItem.Type type, Color color)
    throws ServiceException {
        ColorItem redoRecorder = new ColorItem(mId, itemIds, type, color);

        boolean success = false;
        try {
            beginTransaction("setColor", octxt, redoRecorder);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items) {
                if (!checkItemChangeID(item)) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }
            }
            for (MailItem item : items) {
                item.setColor(color);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void setCustomData(OperationContext octxt, int itemId, MailItem.Type type, CustomMetadata custom)
            throws ServiceException {
        String key = custom.getSectionKey();
        if (MetadataCallback.isSectionRegistered(key)) {
            throw ServiceException.PERM_DENIED("custom metadata section '" + key + "' may only be calculated, not set");
        }
        SetCustomData redoRecorder = new SetCustomData(mId, itemId, type, custom);

        boolean success = false;
        try {
            beginTransaction("setCustomData", octxt, redoRecorder);

            MailItem item = checkAccess(getItemById(itemId, type));
            if (!checkItemChangeID(item)) {
                throw MailServiceException.MODIFY_CONFLICT();
            }
            item.setCustomData(custom);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void setDate(OperationContext octxt, int itemId, MailItem.Type type, long date) throws ServiceException {
        DateItem redoRecorder = new DateItem(mId, itemId, type, date);

        boolean success = false;
        try {
            beginTransaction("setDate", octxt, redoRecorder);

            MailItem item = getItemById(itemId, type);
            if (!checkItemChangeID(item))
                throw MailServiceException.MODIFY_CONFLICT();

            item.setDate(date);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void alterTag(OperationContext octxt, int itemId, MailItem.Type type, Flag.FlagInfo finfo,
            boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        alterTag(octxt, new int[] { itemId }, type, finfo, addTag, tcon);
    }

    public void alterTag(OperationContext octxt, int[] itemIds, MailItem.Type type, Flag.FlagInfo finfo,
            boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        AlterItemTag redoRecorder = new AlterItemTag(mId, itemIds, type, finfo.flagName, addTag, tcon);

        boolean success = false;
        try {
            beginTransaction("alterTag", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            alterTag(itemIds, type, finfo.toFlag(this), addTag);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void alterTag(OperationContext octxt, int itemId, MailItem.Type type, String tagName,
            boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        alterTag(octxt, new int[] { itemId }, type, tagName, addTag, tcon);
    }

    public void alterTag(OperationContext octxt, int[] itemIds, MailItem.Type type, String tagName,
            boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        AlterItemTag redoRecorder = new AlterItemTag(mId, itemIds, type, tagName, addTag, tcon);

        boolean success = false;
        try {
            beginTransaction("alterTag", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            Tag tag;
            try {
                tag = getTagByName(tagName);
            } catch (NoSuchItemException nsie) {
                if (tagName.startsWith(Tag.FLAG_NAME_PREFIX)) {
                    throw nsie;
                }
                Tag.NormalizedTags ntags = new NormalizedTags(this, new String[] { tagName }, addTag);
                if (ntags.getTags().length == 0) {
                    success = true;
                    return;
                }
                tag = getTagByName(ntags.getTags()[0]);
            }

            alterTag(itemIds, type, tag, addTag);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    // common code for the two AlterTag variants (Flag.FlagInfo vs. by tag name)
    private void alterTag(int itemIds[], MailItem.Type type, Tag tag, boolean addTag) throws ServiceException {
        MailItem[] items = getItemById(itemIds, type);
        for (MailItem item : items) {
            if (!(item instanceof Conversation)) {
                if (!checkItemChangeID(item) && item instanceof Tag) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }
            }
        }

        for (MailItem item : items) {
            if (item == null)
                continue;

            if (tag.getId() == Flag.ID_UNREAD) {
                item.alterUnread(addTag);
            } else {
                item.alterTag(tag, addTag);
            }
        }
    }

    public void setTags(OperationContext octxt, int itemId, MailItem.Type type, int flags, String[] tags)
    throws ServiceException {
        setTags(octxt, itemId, type, flags, tags, null);
    }

    public void setTags(OperationContext octxt, int itemId, MailItem.Type type, int flags, String[] tags,
            TargetConstraint tcon)
    throws ServiceException {
        setTags(octxt, new int[] { itemId }, type, flags, tags, tcon);
    }

    public void setTags(OperationContext octxt, int[] itemIds, MailItem.Type type, int flags, String[] tags,
            TargetConstraint tcon)
    throws ServiceException {
        if (flags == MailItem.FLAG_UNCHANGED && tags == MailItem.TAG_UNCHANGED)
            return;

        SetItemTags redoRecorder = new SetItemTags(mId, itemIds, type, flags, tags, tcon);

        boolean success = false;
        try {
            beginTransaction("setTags", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items) {
                checkItemChangeID(item);
            }
            Flag unreadFlag = getFlagById(Flag.ID_UNREAD);

            Tag.NormalizedTags ntags = tags == MailItem.TAG_UNCHANGED ? null : new Tag.NormalizedTags(this, tags);

            for (MailItem item : items) {
                if (item == null) {
                    continue;
                }
                int iflags = flags;
                Tag.NormalizedTags itags = ntags;
                if ((iflags & MailItem.FLAG_UNCHANGED) != 0) {
                    iflags = item.getFlagBitmask();
                }
                if (itags == null) {
                    itags = new Tag.NormalizedTags(item.getTags());
                }
                // special-case "unread" -- it's passed in with the flags, but the server process it separately
                boolean iunread = (iflags & Flag.BITMASK_UNREAD) > 0;
                iflags &= ~Flag.BITMASK_UNREAD;

                item.setTags(iflags, itags);
                if (unreadFlag.canTag(item)) {
                    item.alterUnread(iunread);
                }
            }

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Recovers items from dumpster.
     */
    public List<MailItem> recover(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
            throws ServiceException {
        RecoverItem redoRecorder = new RecoverItem(mId, type, folderId);
        boolean success = false;
        try {
            beginTransaction("recover[]", octxt, redoRecorder);
            List<MailItem> result = copyInternal(octxt, itemIds, type, folderId, true);
            deleteFromDumpster(itemIds);
            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }

    public MailItem copy(OperationContext octxt, int itemId, MailItem.Type type, int folderId)
    throws ServiceException {
        return copy(octxt, new int[] { itemId }, type, folderId).get(0);
    }

    public List<MailItem> copy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
    throws ServiceException {
        CopyItem redoRecorder = new CopyItem(mId, type, folderId);
        boolean success = false;
        try {
            beginTransaction("copy[]", octxt, redoRecorder);
            List<MailItem> result = copyInternal(octxt, itemIds, type, folderId, false);
            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }

    private List<MailItem> copyInternal(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId,
            boolean fromDumpster)
    throws ServiceException {
        CopyItem redoRecorder = (CopyItem) currentChange.getRedoRecorder();
        try {
            if (fromDumpster) {
                Folder trash = getFolderById(ID_FOLDER_TRASH);
                if (!trash.canAccess(ACL.RIGHT_READ)) {
                    throw ServiceException.PERM_DENIED("dumpster access denied");
                }
            }
            CopyItem redoPlayer = (CopyItem) currentChange.getRedoPlayer();

            List<MailItem> result = new ArrayList<MailItem>();

            Folder folder = getFolderById(folderId);

            MailItem[] items = getItemById(itemIds, type, fromDumpster);
            for (MailItem item : items) {
                checkItemChangeID(item);
            }
            for (MailItem item : items) {
                MailItem copy;

                if (item instanceof Conversation) {
                    // this should be done in Conversation.copy(), but redolog issues make that impossible
                    Conversation conv = (Conversation) item;
                    List<Message> msgs = new ArrayList<Message>((int) conv.getSize());
                    for (Message original : conv.getMessages()) {
                        if (!original.canAccess(ACL.RIGHT_READ)) {
                            continue;
                        }
                        int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(original.getId()));
                        Message msg = (Message) original.copy(folder, newId, null);
                        msgs.add(msg);
                        redoRecorder.setDestId(original.getId(), newId);
                    }
                    if (msgs.isEmpty()) {
                        throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
                    } else if (msgs.size() == 1) {
                        copy = msgs.get(0).getParent();
                    } else {
                        int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(conv.getId()));
                        copy = Conversation.create(this, newId, msgs.toArray(new Message[msgs.size()]));
                        redoRecorder.setDestId(conv.getId(), newId);
                    }
                } else {
                    int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(item.getId()));
                    int parentId = item.getParentId();
                    MailItem parent = null;
                    if (fromDumpster) {
                        // Parent of dumpstered item may no longer exist.
                        if (parentId > 0) {
                            try {
                                parent = getItemById(parentId, MailItem.Type.UNKNOWN);
                            } catch (MailServiceException.NoSuchItemException e) {
                                // ignore
                            }
                        }
                    }
                    copy = item.copy(folder, newId, parent);
                    if (fromDumpster) {
                        for (MailItem.UnderlyingData data : DbMailItem.getByParent(item, SortBy.DATE_DESC, -1, true)) {
                            MailItem child = getItem(data);
                            Folder destination = (child.getType() == Type.COMMENT) ? getFolderById(ID_FOLDER_COMMENTS) : folder;
                            child.copy(destination, getNextItemId(ID_AUTO_INCREMENT), copy);
                        }
                    }
                    redoRecorder.setDestId(item.getId(), newId);
                }

                result.add(copy);
            }
            return result;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while copying items", e);
        }
    }

    public List<MailItem> imapCopy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
    throws IOException, ServiceException {
        // this is an IMAP command, so we'd better be tracking IMAP changes by now...
        beginTrackingImap();

        for (int id : itemIds) {
            if (id <= 0) {
                throw MailItem.noSuchItem(id, type);
            }
        }

        ImapCopyItem redoRecorder = new ImapCopyItem(mId, type, folderId);

        boolean success = false;
        try {
            beginTransaction("icopy", octxt, redoRecorder);
            ImapCopyItem redoPlayer = (ImapCopyItem) currentChange.getRedoPlayer();

            Folder target = getFolderById(folderId);

            // fetch the items to copy and make sure the caller is up-to-date on change IDs
            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items) {
                checkItemChangeID(item);
            }
            List<MailItem> result = new ArrayList<MailItem>();

            for (MailItem item : items) {
                int srcId = item.getId();
                int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(srcId));

                trainSpamFilter(octxt, item, target, "imap copy");

                MailItem copy = item.icopy(target, newId);
                result.add(copy);
                redoRecorder.setDestId(srcId, newId);
            }

            success = true;
            return result;
        } finally {
            endTransaction(success);
        }
    }

    private <T extends MailItem> T trainSpamFilter(OperationContext octxt, T item, Folder target, String opDescription) {
        if (currentChange.getRedoPlayer() != null) { // don't re-train filter on replayed operation
            return item;
        }
        TargetConstraint tcon = getOperationTargetConstraint();

        try {
            List<? extends MailItem> items = item instanceof Conversation ?
                    ((Conversation) item).getMessages() : Arrays.asList((MailItem) item);
            List<Folder> trashAliases = getTrashAliases(octxt);
            for (MailItem candidate : items) {
                // if it's not a move into or out of Spam, no training is necessary
                //   (moves from Spam to Trash also do not train the filter)
                boolean fromSpam = candidate.inSpam();
                boolean toSpam = target.inSpam();
                if (!fromSpam && !toSpam) {
                    continue;
                }
                if (fromSpam && (toSpam || target.inTrash() || inFolder(trashAliases, target))) {
                    continue;
                }
                if (!TargetConstraint.checkItem(tcon, item) || !item.canAccess(ACL.RIGHT_READ)) {
                    continue;
                }
                try {
                    SpamReport report = new SpamReport(toSpam, opDescription, target.getPath());
                    Folder source = item.getFolder();
                    if (!source.equals(target)) {
                        report.setSourceFolderPath(source.getPath());
                    }
                    SpamHandler.getInstance().handle(octxt, this, candidate.getId(), candidate.getType(), report);
                } catch (Exception e) {
                    ZimbraLog.mailop.info("could not train spam filter: " + MailItem.getMailopContext(candidate), e);
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.mailop.info("could not train spam filter: " + MailItem.getMailopContext(item), e);
        }

        return item;
    }

    private List<Folder> getTrashAliases(OperationContext octx) throws ServiceException {
        String[] aliases = Provisioning.getInstance().getConfig().getSpamTrashAlias();
        List<Folder> result = new ArrayList<Folder>(aliases.length);
        for (String path : aliases) {
            try {
                result.add(getFolderByPath(octx, path));
            } catch (ServiceException ignore) { // NO_SUCH_FOLDER
            }
        }
        return result;
    }

    private boolean inFolder(List<Folder> base, Folder target) throws ServiceException {
        for (Folder folder : base) {
            if (folder.getId() == target.getId() || folder.isDescendant(target)) {
                return true;
            }
        }
        return false;
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
     * @param type      The type of the item or {@link MailItem.Type#UNKNOWN}.
     * @param targetId  The ID of the target folder for the move. */
    public void move(OperationContext octxt, int itemId, MailItem.Type type, int targetId) throws ServiceException {
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
     * @param type      The type of the item or {@link MailItem.Type#UNKNOWN}.
     * @param targetId  The ID of the target folder for the move.
     * @param tcon      An optional constraint on the item being moved. */
    public void move(OperationContext octxt, int itemId, MailItem.Type type, int targetId,
            TargetConstraint tcon) throws ServiceException {
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
     * @param type      The type of the items or {@link MailItem.Type#UNKNOWN}.
     * @param targetId  The ID of the target folder for the move.
     * @param tcon      An optional constraint on the items being moved. */
    public void move(OperationContext octxt, int[] itemIds, MailItem.Type type, int targetId, TargetConstraint tcon)
            throws ServiceException {
        lock.lock();
        try {
            try {
                moveInternal(octxt, itemIds, type, targetId, tcon);
                return;
            } catch (ServiceException e) {
                // make sure that move-to-Trash never fails with a naming conflict
                if (!e.getCode().equals(MailServiceException.ALREADY_EXISTS) || targetId != ID_FOLDER_TRASH) {
                    throw e;
                }
            }

            // if we're here, we hit a naming conflict during move-to-Trash
            if (itemIds.length == 1) {
                // rename the item being moved instead of the one already there...
                rename(octxt, itemIds[0], type, generateAlternativeItemName(octxt, itemIds[0], type), targetId);
            } else {
                // iterate one-by-one and move the items individually
                for (int id : itemIds) {
                    // FIXME: non-transactional
                    try {
                        // still more likely than not to succeed...
                        moveInternal(octxt, new int[] { id }, type, targetId, tcon);
                    } catch (ServiceException e) {
                        // rename the item being moved instead of the one already there...
                        rename(octxt, id, type, generateAlternativeItemName(octxt, id, type), targetId);
                    }
                }
            }
        } finally {
            lock.release();
        }
    }

    private String generateAlternativeItemName(OperationContext octxt, int id, MailItem.Type type)
            throws ServiceException {
        String name = getItemById(octxt, id, type).getName();
        String uuid = '{' + UUID.randomUUID().toString() + '}';
        if (name.length() + uuid.length() > MailItem.MAX_NAME_LENGTH) {
            return name.substring(0, MailItem.MAX_NAME_LENGTH - uuid.length()) + uuid;
        } else {
            return name + uuid;
        }
    }

    private void moveInternal(OperationContext octxt, int[] itemIds, MailItem.Type type, int targetId,
            TargetConstraint tcon) throws ServiceException {
        MoveItem redoRecorder = new MoveItem(mId, itemIds, type, targetId, tcon);

        boolean success = false;
        try {
            beginTransaction("move", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            Folder target = getFolderById(targetId);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items) {
                checkItemChangeID(item);
            }
            int oldUIDNEXT = target.getImapUIDNEXT();
            boolean resetUIDNEXT = false;

            for (MailItem item : items) {

                // train the spam filter if necessary...
                trainSpamFilter(octxt, item, target, "move");

                // ...do the move...
                boolean moved = item.move(target);

                // ...and determine whether the move needs to cause an UIDNEXT change
                if (moved && !resetUIDNEXT && isTrackingImap() &&
                        (item instanceof Conversation || item instanceof Message || item instanceof Contact)) {
                    resetUIDNEXT = true;
                }
            }

            // if this operation should cause the target folder's UIDNEXT value to change but it hasn't yet, do it here
            if (resetUIDNEXT && oldUIDNEXT == target.getImapUIDNEXT()) {
                MoveItem redoPlayer = (MoveItem) currentChange.getRedoPlayer();
                redoRecorder.setUIDNEXT(getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getUIDNEXT()));
                target.updateUIDNEXT();
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void rename(OperationContext octxt, int id, MailItem.Type type, String name, int folderId)
            throws ServiceException {
        if (name != null && name.startsWith("/")) {
            rename(octxt, id, type, name);
            return;
        }

        name = StringUtil.stripControlCharacters(name);
        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("cannot set name to empty string", null);
        }

        RenameItem redoRecorder = new RenameItem(mId, id, type, name, folderId);

        boolean success = false;
        try {
            beginTransaction("rename", octxt, redoRecorder);

            MailItem item = getItemById(id, type);
            checkItemChangeID(item);
            if (folderId <= 0) {
                folderId = item.getFolderId();
            }
            Folder target = getFolderById(folderId);
            trainSpamFilter(octxt, item, target, "rename");

            String oldName = item.getName();
            item.rename(name, target);

            if (item instanceof Tag) {
                mTagCache.remove(oldName.toLowerCase());
                mTagCache.put(name.toLowerCase(), (Tag) item);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void rename(OperationContext octxt, int id, MailItem.Type type, String path) throws ServiceException {
        if (path == null || !path.startsWith("/")) {
            rename(octxt, id, type, path, ID_AUTO_INCREMENT);
            return;
        }

        RenameItemPath redoRecorder = new RenameItemPath(mId, id, type, path);

        boolean success = false;
        try {
            beginTransaction("renameFolderPath", octxt, redoRecorder);
            RenameItemPath redoPlayer = (RenameItemPath) currentChange.getRedoPlayer();

            MailItem item = getItemById(id, type);
            Folder parent;
            checkItemChangeID(item);

            String[] parts = path.substring(1).split("/");
            if (parts.length == 0) {
                throw MailServiceException.ALREADY_EXISTS(path);
            }
            int[] recorderParentIds = new int[parts.length - 1];
            int[] playerParentIds = redoPlayer == null ? null : redoPlayer.getParentIds();
            if (playerParentIds != null && playerParentIds.length != recorderParentIds.length) {
                throw ServiceException.FAILURE("incorrect number of path segments in redo player", null);
            }
            parent = getFolderById(ID_FOLDER_USER_ROOT);
            for (int i = 0; i < parts.length - 1; i++) {
                String name = MailItem.validateItemName(parts[i]);
                int subfolderId = playerParentIds == null ? ID_AUTO_INCREMENT : playerParentIds[i];
                Folder subfolder = parent.findSubfolder(name);
                if (subfolder == null) {
                    subfolder = Folder.create(getNextItemId(subfolderId), this, parent, name);
                } else if (subfolderId != ID_AUTO_INCREMENT && subfolderId != subfolder.getId()) {
                    throw ServiceException.FAILURE("parent folder id changed since operation was recorded", null);
                } else if (!subfolder.getName().equals(name) && subfolder.isMutable()) {
                    // Same folder name, different case.
                    subfolder.rename(name, parent);
                }
                recorderParentIds[i] = subfolder.getId();
                parent = subfolder;
            }
            redoRecorder.setParentIds(recorderParentIds);

            trainSpamFilter(octxt, item, parent, "rename");

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
    public void delete(OperationContext octxt, int itemId, MailItem.Type type) throws ServiceException {
        delete(octxt, new int[] { itemId }, type, null);
    }

    /** Deletes the <tt>MailItem</tt> with the given id.  If there is no such
     *  <tt>MailItem</tt>, nothing happens and no error is generated.  If the
     *  id maps to an existing <tt>MailItem</tt> of an incompatible type,
     *  however, an error is thrown.
     *
     * @param octxt operation context or {@code null}
     * @param itemId item id
     * @param type item type or {@link MailItem.Type#UNKNOWN}
     * @param tcon target constraint or {@code null} */
    public void delete(OperationContext octxt, int itemId, MailItem.Type type, TargetConstraint tcon)
    throws ServiceException {
        delete(octxt, new int[] { itemId }, type, tcon);
    }

    /** Deletes the <tt>MailItem</tt>s with the given id.  If there is no
     *  <tt>MailItem</tt> for a given id, that id is ignored.  If the id maps
     *  to an existing <tt>MailItem</tt> of an incompatible type, however,
     *  an error is thrown.
     *
     * @param octxt operation context or {@code null}
     * @param itemIds item ids
     * @param type item type or {@link MailItem.Type#UNKNOWN}
     * @param tcon target constraint or {@code null} */
    public void delete(OperationContext octxt, int[] itemIds, MailItem.Type type, TargetConstraint tcon)
            throws ServiceException {
        DeleteItem redoRecorder = new DeleteItem(mId, itemIds, type, tcon);

        boolean success = false;
        try {
            beginTransaction("delete", octxt, redoRecorder);
            setOperationTargetConstraint(tcon);

            for (int id : itemIds) {
                if (id == ID_AUTO_INCREMENT) {
                    continue;
                }
                MailItem item;
                try {
                    item = getItemById(id, MailItem.Type.UNKNOWN);
                } catch (NoSuchItemException nsie) {
                    // trying to delete nonexistent things is A-OK!
                    continue;
                }

                // however, trying to delete messages and passing in a folder ID is not OK
                if (!MailItem.isAcceptableType(type, item.getType())) {
                    throw MailItem.noSuchItem(id, type);
                } else if (!checkItemChangeID(item) && item instanceof Tag) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }

                // delete the item, but don't write the tombstone until we're finished...
                item.delete(false);
            }

            // deletes have already been collected, so fetch the tombstones and write once
            TypedIdList tombstones = collectPendingTombstones();
            if (tombstones != null && !tombstones.isEmpty()) {
                DbMailItem.writeTombstones(this, tombstones);
            }

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    TypedIdList collectPendingTombstones() {
        if (!isTrackingSync() || currentChange.deletes == null) {
            return null;
        }
        return new TypedIdList(currentChange.deletes.itemIds);
    }

    private int deleteFromDumpster(int[] itemIds) throws ServiceException {
        Folder trash = getFolderById(ID_FOLDER_TRASH);
        if (!trash.canAccess(ACL.RIGHT_DELETE)) {
            throw ServiceException.PERM_DENIED("dumpster access denied");
        }

        int numDeleted = 0;
        for (int id : itemIds) {
            MailItem item = null;
            try {
                item = getItemById(id, MailItem.Type.UNKNOWN, true);
            } catch (MailServiceException.NoSuchItemException e) {
                ZimbraLog.mailbox.info("ignoring NO_SUCH_ITEM exception during dumpster delete; item id=" + id);
                continue;
            }
            item.delete();
            ++numDeleted;
        }
        return numDeleted;
    }

    public int deleteFromDumpster(OperationContext octxt, int[] itemIds) throws ServiceException {
        boolean isRedo = octxt != null && octxt.isRedo();
        if (!isRedo && !hasFullAdminAccess(octxt)) {
            throw ServiceException.PERM_DENIED("only admins can delete from dumpster");
        }
        DeleteItemFromDumpster redoRecorder = new DeleteItemFromDumpster(mId, itemIds);
        boolean success = false;
        try {
            beginTransaction("deleteFromDumpster[]", octxt, redoRecorder);
            int numDeleted = deleteFromDumpster(itemIds);
            success = true;
            return numDeleted;
        } finally {
            endTransaction(success);
        }
    }

    public int emptyDumpster(OperationContext octxt) throws ServiceException {
        if (!hasFullAdminAccess(octxt)) {
            throw ServiceException.PERM_DENIED("only admins can delete from dumpster");
        }

        int numDeleted = 0;
        int batchSize = Provisioning.getInstance().getLocalServer().getMailEmptyFolderBatchSize();
        ZimbraLog.mailbox.info("Emptying dumpster with batchSize=" + batchSize);
        QueryParams params = new QueryParams();
        // +1 to catch items put into dumpster in the same second
        params.setChangeDateBefore((int) (System.currentTimeMillis() / 1000 + 1)).setRowLimit(batchSize);
        while (true) {
            lock.lock();
            try {
                Set<Integer> itemIds = null;
                DbConnection conn = DbPool.getConnection();
                try {
                    itemIds = DbMailItem.getIds(this, conn, params, true);
                } finally {
                    conn.closeQuietly();
                }

                if (itemIds.isEmpty()) {
                    break;
                }
                numDeleted += deleteFromDumpster(octxt, ArrayUtil.toIntArray(itemIds));
            } finally {
                lock.release();
            }
        }
        return numDeleted;
    }

    private int purgeDumpster(long olderThanMillis, int maxItems) throws ServiceException {
        QueryParams params = new QueryParams();
        params.setChangeDateBefore((int) (olderThanMillis / 1000)).setRowLimit(maxItems);
        lock.lock();
        try {
            Set<Integer> itemIds = null;
            DbConnection conn = DbPool.getConnection();
            try {
                itemIds = DbMailItem.getIds(this, conn, params, true);
            } finally {
                DbPool.quietClose(conn);
            }
            if (!itemIds.isEmpty()) {
                return deleteFromDumpster(ArrayUtil.toIntArray(itemIds));
            } else {
                return 0;
            }
        } finally {
            lock.release();
        }
    }

    public Tag createTag(OperationContext octxt, String name, byte color) throws ServiceException {
        return createTag(octxt, name, new Color(color));
    }

    public Tag createTag(OperationContext octxt, String name, Color color) throws ServiceException {
        name = StringUtil.stripControlCharacters(name);
        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("tag must have a name", null);
        }
        CreateTag redoRecorder = new CreateTag(mId, name, color);

        boolean success = false;
        try {
            beginTransaction("createTag", octxt, redoRecorder);
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }

            CreateTag redoPlayer = (CreateTag) currentChange.getRedoPlayer();
            Tag tag = createTagInternal(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getTagId(), name, color, true);
            redoRecorder.setTagId(tag.getId());
            success = true;
            return tag;
        } finally {
            endTransaction(success);
        }
    }

    Tag createTagInternal(int tagId, String name, Color color, boolean listed) throws ServiceException {
        try {
            Tag tag = getTagByName(name);
            if (tag.isListed()) {
                throw MailServiceException.ALREADY_EXISTS(name);
            }

            // promote an implicitly-created tag to a listed tag
            markItemCreated(tag);
            tag.setListed();
            if (!name.equals(tag.getName())) {
                tag.rename(name);
            }
            tag.setColor(color);
            return tag;
        } catch (NoSuchItemException nsie) {
            return Tag.create(this, getNextItemId(tagId), name, color, listed);
        }
    }

    public Note createNote(OperationContext octxt, String content, Rectangle location, byte color, int folderId)
    throws ServiceException {
        return createNote(octxt, content, location, new Color(color), folderId);
    }

    public Note createNote(OperationContext octxt, String content, Rectangle location, Color color, int folderId)
    throws ServiceException {
        content = StringUtil.stripControlCharacters(content);
        if (Strings.isNullOrEmpty(content)) {
            throw ServiceException.INVALID_REQUEST("note content may not be empty", null);
        }
        CreateNote redoRecorder = new CreateNote(mId, folderId, content, color, location);

        boolean success = false;
        try {
            beginTransaction("createNote", octxt, redoRecorder);
            CreateNote redoPlayer = (CreateNote) currentChange.getRedoPlayer();

            int noteId;
            if (redoPlayer == null) {
                noteId = getNextItemId(ID_AUTO_INCREMENT);
            } else {
                noteId = getNextItemId(redoPlayer.getNoteId());
            }
            redoRecorder.setNoteId(noteId);

            Note note = Note.create(noteId, getFolderById(folderId), content, location, color, null);

            index.add(note);
            success = true;
            return note;
        } finally {
            endTransaction(success);
        }
    }

    public void editNote(OperationContext octxt, int noteId, String content) throws ServiceException {
        content = StringUtil.stripControlCharacters(content);
        if (Strings.isNullOrEmpty(content)) {
            throw ServiceException.INVALID_REQUEST("note content may not be empty", null);
        }
        EditNote redoRecorder = new EditNote(mId, noteId, content);

        boolean success = false;
        try {
            beginTransaction("editNote", octxt, redoRecorder);

            Note note = getNoteById(noteId);
            checkItemChangeID(note);

            note.setContent(content);
            index.add(note);

            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void repositionNote(OperationContext octxt, int noteId, Rectangle location) throws ServiceException {
        Preconditions.checkNotNull(location, "must specify note bounds");
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

    CalendarItem createCalendarItem(int folderId, int flags, Tag.NormalizedTags ntags, String uid,
                                    ParsedMessage pm, Invite invite, CustomMetadata custom)
    throws ServiceException {
        // FIXME: assuming that we're in the middle of a AddInvite op
        CreateCalendarItemPlayer redoPlayer = (CreateCalendarItemPlayer) currentChange.getRedoPlayer();
        CreateCalendarItemRecorder redoRecorder = (CreateCalendarItemRecorder) currentChange.getRedoRecorder();

        int newCalItemId = redoPlayer == null ? Mailbox.ID_AUTO_INCREMENT : redoPlayer.getCalendarItemId();
        int createId = getNextItemId(newCalItemId);

        CalendarItem calItem = CalendarItem.create(createId, getFolderById(folderId), flags, ntags,
                                                   uid, pm, invite, CalendarItem.NEXT_ALARM_FROM_NOW, custom);

        if (redoRecorder != null) {
            redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId());
        }
        return calItem;
    }

    public Contact createContact(OperationContext octxt, ParsedContact pc, int folderId, String[] tags)
    throws ServiceException {
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged = null;
        if (pc.hasAttachment()) {
            // write the contact content directly to the mailbox's blob staging area
            InputStream is = null;
            try {
                staged = sm.stage(is = pc.getContentStream(), (int) pc.getSize(), null, this);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("could not save contact blob", ioe);
            } finally {
                ByteUtil.closeStream(is);
            }
        }

        CreateContact redoRecorder = new CreateContact(mId, folderId, pc, tags);

        boolean success = false;
        try {
            beginTransaction("createContact", octxt, redoRecorder);
            CreateContact redoPlayer = (CreateContact) currentChange.getRedoPlayer();
            boolean isRedo = redoPlayer != null;

            Tag.NormalizedTags ntags = new Tag.NormalizedTags(this, tags);


            int contactId = getNextItemId(isRedo ? redoPlayer.getContactId() : ID_AUTO_INCREMENT);
            redoRecorder.setContactId(contactId);

            MailboxBlob mblob = null;
            if (pc.hasAttachment()) {
                try {
                    mblob = sm.renameTo(staged, this, contactId, getOperationChangeID());
                    markOtherItemDirty(mblob);
                } catch (IOException ioe) {
                    throw ServiceException.FAILURE("could not save contact blob", ioe);
                }
            }

            int flags = 0;
            Contact con = Contact.create(contactId, getFolderById(folderId), mblob, pc, flags, ntags, null);

            index.add(con);

            success = true;
            return con;
        } finally {
            endTransaction(success);
            sm.quietDelete(staged);
        }
    }

    public void modifyContact(OperationContext octxt, int contactId, ParsedContact pc) throws ServiceException {
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged = null;
        if (pc.hasAttachment()) {
            // write the contact content directly to the mailbox's blob staging area
            InputStream is = null;
            try {
                staged = sm.stage(is = pc.getContentStream(), pc.getSize(), null, this);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("could not save contact blob", ioe);
            } finally {
                ByteUtil.closeStream(is);
            }
        }

        ModifyContact redoRecorder = new ModifyContact(mId, contactId, pc);
        boolean success = false;
        try {
            beginTransaction("modifyContact", octxt, redoRecorder);
            Contact con = getContactById(contactId);
            if (!checkItemChangeID(con))
                throw MailServiceException.MODIFY_CONFLICT();

            try {
                // setContent() calls reanalyze(), which also updates the contact fields even when there is no blob
                con.setContent(staged, pc);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("could not save contact blob", ioe);
            }

            index.add(con);
            success = true;
        } finally {
            endTransaction(success);
            sm.quietDelete(staged);
        }
    }

    /**
     * Creates new contacts in AUTO_CONTACTS folder. Email addresses that already exist in any contacts folder are
     * ignored.
     *
     * @param octxt operation context
     * @param addrs email addresses
     * @return newly created contacts
     */
    public List<Contact> createAutoContact(OperationContext octxt, Collection<InternetAddress> addrs)
            throws IOException {
        if (addrs.isEmpty()) {
            return Collections.emptyList();
        }
        if (lock.isLocked()) { //TODO can't search while holding the mailbox lock
            ZimbraLog.mailbox.warn("Unable to auto-add contact while holding Mailbox lock");
            return Collections.emptyList();
        }
        Set<InternetAddress> newAddrs = new HashSet<InternetAddress>();
        for (InternetAddress addr : addrs) {
            if (!Strings.isNullOrEmpty(addr.getAddress()) && !index.existsInContacts(Collections.singleton(addr))) {
                newAddrs.add(addr);
            }
        }
        if (newAddrs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Contact> result = new ArrayList<Contact>(newAddrs.size());
        for (InternetAddress addr : newAddrs) {
            ZimbraLog.mailbox.debug("Auto-adding new contact addr=%s", addr);
            try {
                result.add(createContact(octxt, new ParsedContact(new ParsedAddress(addr).getAttributes()),
                        Mailbox.ID_FOLDER_AUTO_CONTACTS, null));
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("Failed to auto-add contact addr=%s", addr, e);
            }
        }
        return result;
    }

    public Folder createFolder(OperationContext octxt, String name, int parentId, MailItem.Type defaultView, int flags,
            byte color, String url)
    throws ServiceException {
        return createFolder(octxt, name, parentId, (byte)0, defaultView, flags, color, url);
    }

    public Folder createFolder(OperationContext octxt, String name, int parentId, byte attrs, MailItem.Type defaultView,
            int flags, byte color, String url)
    throws ServiceException {
        return createFolder(octxt, name, parentId, attrs, defaultView, flags, new Color(color), url);
    }

    public Folder createFolder(OperationContext octxt, String name, int parentId, byte attrs, MailItem.Type defaultView,
            int flags, Color color, String url)
    throws ServiceException {
        CreateFolder redoRecorder = new CreateFolder(mId, name, parentId, attrs, defaultView, flags, color, url);

        boolean success = false;
        try {
            beginTransaction("createFolder", octxt, redoRecorder);
            CreateFolder redoPlayer = (CreateFolder) currentChange.getRedoPlayer();

            int folderId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getFolderId());
            Folder folder = Folder.create(folderId, this, getFolderById(parentId), name, attrs, defaultView, flags, color, url, null);
            redoRecorder.setFolderId(folder.getId());
            success = true;
            updateRssDataSource(folder);
            return folder;
        } finally {
            endTransaction(success);
        }
    }

    public Folder createFolder(OperationContext octxt, String path, byte attrs, MailItem.Type defaultView)
    throws ServiceException {
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
    public Folder createFolder(OperationContext octxt, String path, byte attrs, MailItem.Type defaultView,
            int flags, byte color, String url)
    throws ServiceException {
        return createFolder(octxt, path, attrs, defaultView, flags, new Color(color), url);
    }

    public Folder createFolder(OperationContext octxt, String path, byte attrs, MailItem.Type defaultView,
            int flags, Color color, String url)
    throws ServiceException {
        if (path == null) {
            throw ServiceException.FAILURE("null path passed to Mailbox.createFolderPath", null);
        }
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        CreateFolderPath redoRecorder = new CreateFolderPath(mId, path, attrs, defaultView, flags, color, url);

        boolean success = false;
        try {
            beginTransaction("createFolderPath", octxt, redoRecorder);
            CreateFolderPath redoPlayer = (CreateFolderPath) currentChange.getRedoPlayer();

            String[] parts = path.substring(1).split("/");
            if (parts.length == 0) {
                throw MailServiceException.ALREADY_EXISTS(path);
            }
            int[] recorderFolderIds = new int[parts.length];
            int[] playerFolderIds = redoPlayer == null ? null : redoPlayer.getFolderIds();
            if (playerFolderIds != null && playerFolderIds.length != recorderFolderIds.length) {
                throw ServiceException.FAILURE("incorrect number of path segments in redo player", null);
            }
            Folder folder = getFolderById(ID_FOLDER_USER_ROOT);
            for (int i = 0; i < parts.length; i++) {
                boolean last = i == parts.length - 1;
                int folderId = playerFolderIds == null ? ID_AUTO_INCREMENT : playerFolderIds[i];
                Folder subfolder = folder.findSubfolder(parts[i]);
                if (subfolder == null) {
                    subfolder = Folder.create(getNextItemId(folderId), this, folder, parts[i], (byte) 0,
                            last ? defaultView : MailItem.Type.UNKNOWN, flags, color, last ? url : null, null);
                } else if (folderId != ID_AUTO_INCREMENT && folderId != subfolder.getId()) {
                    throw ServiceException.FAILURE("parent folder id changed since operation was recorded", null);
                } else if (last) {
                    throw MailServiceException.ALREADY_EXISTS(path);
                }
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

    //for offline override to filter flags
    public String getItemFlagString(MailItem mi) {
        return mi.getFlagString();
    }

    public ACL.Grant grantAccess(OperationContext octxt, int itemId, String grantee, byte granteeType, short rights,
            String args) throws ServiceException {
        GrantAccess redoPlayer = new GrantAccess(mId, itemId, grantee, granteeType, rights, args);

        boolean success = false;
        ACL.Grant grant = null;
        try {
            beginTransaction("grantAccess", octxt, redoPlayer);

            MailItem item = getItemById(itemId, Type.UNKNOWN);
            checkItemChangeID(item);
            grant = item.grantAccess(grantee, granteeType, rights, args);
            success = true;
        } finally {
            endTransaction(success);
        }
        return grant;
    }

    public void revokeAccess(OperationContext octxt, int itemId, String grantee) throws ServiceException {
        RevokeAccess redoPlayer = new RevokeAccess(mId, itemId, grantee);

        boolean success = false;
        try {
            beginTransaction("revokeAccess", octxt, redoPlayer);

            MailItem item = getItemById(itemId, Type.UNKNOWN);
            checkItemChangeID(item);
            item.revokeAccess(grantee);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void setPermissions(OperationContext octxt, int itemId, ACL acl) throws ServiceException {
        SetPermissions redoPlayer = new SetPermissions(mId, itemId, acl);

        boolean success = false;
        try {
            beginTransaction("setPermissions", octxt, redoPlayer);

            MailItem item = getItemById(itemId, Type.UNKNOWN);
            checkItemChangeID(item);
            item.setPermissions(acl);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void setRetentionPolicy(OperationContext octxt, int itemId, MailItem.Type type, RetentionPolicy rp)
    throws ServiceException {
        if (type != MailItem.Type.FOLDER && type != MailItem.Type.TAG) {
            throw ServiceException.FAILURE("Cannot set retention policy for " + type, null);
        }
        if (rp == null) {
            rp = new RetentionPolicy();
        } else {
            validateRetentionPolicy(rp.getKeepPolicy());
            validateRetentionPolicy(rp.getPurgePolicy());
        }

        SetRetentionPolicy redoPlayer = new SetRetentionPolicy(mId, type, itemId, rp);

        boolean success = false;
        try {
            beginTransaction("setRetentionPolicy", octxt, redoPlayer);

            if (type == MailItem.Type.FOLDER) {
                Folder folder = getFolderById(itemId);
                checkItemChangeID(folder);
                folder.setRetentionPolicy(rp);
            } else {
                Tag tag = getTagById(itemId);
                checkItemChangeID(tag);
                tag.setRetentionPolicy(rp);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    private void validateRetentionPolicy(List<Policy> list) throws ServiceException {
        int numUser = 0;

        for (Policy p : list) {
            String lifetime = p.getLifetime();
            if (!StringUtil.isNullOrEmpty(lifetime)) {
                // Validate lifetime string.
                DateUtil.getTimeInterval(lifetime);
            }
            if (p.getType() == Policy.Type.USER) {
                numUser++;
            }
        }

        if (numUser > 1) {
            throw ServiceException.INVALID_REQUEST("Cannot set more than one user retention policy per folder.", null);
        }
    }

    public void setFolderDefaultView(OperationContext octxt, int folderId, MailItem.Type view) throws ServiceException {
        SetFolderDefaultView redoRecorder = new SetFolderDefaultView(mId, folderId, view);

        boolean success = false;
        try {
            beginTransaction("setFolderDefaultView", octxt, redoRecorder);

            Folder folder = getFolderById(folderId);
            if (!checkItemChangeID(folder)) {
                throw MailServiceException.MODIFY_CONFLICT();
            }
            folder.setDefaultView(view);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void setFolderUrl(OperationContext octxt, int folderId, String url) throws ServiceException {
        SetFolderUrl redoRecorder = new SetFolderUrl(mId, folderId, url);

        boolean success = false;
        try {
            beginTransaction("setFolderUrl", octxt, redoRecorder);

            Folder folder = getFolderById(folderId);
            checkItemChangeID(folder);
            folder.setUrl(url);
            success = true;
            updateRssDataSource(folder);
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Updates the data source for an RSS folder.  If the folder URL is set,
     * checks or creates a data source that updates the folder.  If the URL
     * is not set, deletes the data source if necessary.
     */
    protected void updateRssDataSource(Folder folder) {
        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = getAccount();
            DataSource ds = null;
            List<DataSource> dataSources = prov.getAllDataSources(account);
            for (DataSource i : dataSources) {
                if (i.getFolderId() == folder.getId() &&
                    (i.getType() == DataSourceType.rss || i.getType() == DataSourceType.cal)) {
                    ds = i;
                    break;
                }
            }

            if (StringUtil.isNullOrEmpty(folder.getUrl())) {
                if (ds != null) {
                    // URL removed from folder.
                    String dsid = ds.getId();
                    prov.deleteDataSource(account, dsid);
                    DataSourceManager.cancelSchedule(account, dsid);
                }
                return;
            }

            // URL is not null or empty.  Create data source if necessary.
            if (ds == null) {
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
                attrs.put(Provisioning.A_zimbraDataSourceFolderId, Integer.toString(folder.getId()));

                DataSourceType type;
                String name;
                if (folder.getDefaultView() == MailItem.Type.APPOINTMENT) {
                    type = DataSourceType.cal;
                    name = "CAL-" + folder.getId();
                } else {
                    type = DataSourceType.rss;
                    name = "RSS-" + folder.getId();
                }

                ds = prov.createDataSource(account, type, name, attrs);
                DataSourceManager.updateSchedule(account, ds);
            }
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Unable to update data source for folder %s.", folder.getPath(), e);
        }
    }

    public void synchronizeFolder(OperationContext octxt, int folderId) throws ServiceException {
        importFeed(octxt, folderId, getFolderById(octxt, folderId).getUrl(), true);
    }

    public void importFeed(OperationContext octxt, int folderId, String url, boolean subscription) throws ServiceException {
        if (StringUtil.isNullOrEmpty(url))
            return;

        // get the remote data, skipping anything we've already seen (if applicable)
        Folder folder = getFolderById(octxt, folderId);
        Folder.SyncData fsd = subscription ? folder.getSyncData() : null;
        FeedManager.SubscriptionData<?> sdata = FeedManager.retrieveRemoteDatasource(getAccount(), url, fsd);
        if (!sdata.isNotModified()) {
            lock.lock();
            try {
                importFeedInternal(octxt, folder, subscription, sdata);
            } finally {
                lock.release();
            }
        }
    }

    private void importFeedInternal(OperationContext octxt, Folder folder, boolean subscription,
            FeedManager.SubscriptionData<?> sdata) throws ServiceException {
        assert(lock.isLocked());
        // If syncing a folder with calendar items, remember the current items.  After applying the new
        // appointments/tasks, we need to remove ones that were not updated because they are apparently
        // deleted from the source feed.
        boolean isCalendar = folder.getDefaultView() == MailItem.Type.APPOINTMENT ||
                             folder.getDefaultView() == MailItem.Type.TASK;
        Set<Integer> toRemove = new HashSet<Integer>();
        if (subscription && isCalendar) {
            for (int i : listItemIds(octxt, MailItem.Type.UNKNOWN, folder.getId())) {
                toRemove.add(i);
            }
        }

        // if there's nothing to add, we can short-circuit here
        List<?> items = sdata.getItems();
        if (items.isEmpty()) {
            if (subscription && isCalendar)
                emptyFolder(octxt, folder.getId(), false);  // quicker than deleting appointments one at a time
            updateRssDataSource(folder);
            return;
        }

        // disable modification conflict checks, as we've already wiped the folder and we may hit an appoinment >1 times
        OperationContext octxtNoConflicts = null;
        if (octxt != null) {
            octxtNoConflicts = new OperationContext(octxt).unsetChangeConstraint();
        } else {
            octxtNoConflicts = new OperationContext(getAccountId()).unsetChangeConstraint();
        }

        // add the newly-fetched items to the folder
        Set<String> calUidsSeen = new HashSet<String>();
        for (Object obj : items) {
            try {
                if (obj instanceof Invite) {
                    Invite inv = (Invite) obj;
                    String uid = inv.getUid();
                    if (uid == null) {
                        uid = LdapUtilCommon.generateUUID();
                        inv.setUid(uid);
                    }
                    // Create the event in accepted state.  (bug 41639)
                    inv.setPartStat(IcalXmlStrMap.PARTSTAT_ACCEPTED);
                    inv.setRsvp(false);

                    boolean addRevision;
                    if (!calUidsSeen.contains(uid)) {
                        addRevision = true;
                        calUidsSeen.add(uid);
                    } else {
                        addRevision = false;
                    }
                    try {
                        boolean importIt;
                        CalendarItem calItem = getCalendarItemByUid(uid);
                        if (calItem == null) {
                            // New appointment.  Import it.
                            importIt = true;
                        } else {
                            toRemove.remove(calItem.getId());
                            Folder curFolder = calItem.getFolder();
                            boolean sameFolder = curFolder.getId() == folder.getId();
                            boolean inTrashOrSpam = !sameFolder && (curFolder.inTrash() || curFolder.inSpam());
                            if (inTrashOrSpam) {
                                // If appointment is under trash/spam, delete it now to allow the downloaded invite to
                                // be imported cleanly.  Appointment in trash/spam is effectively non-existent, because
                                // it will eventually get purged.
                                delete(octxtNoConflicts, calItem.getId(), MailItem.Type.UNKNOWN);
                                importIt = true;
                            } else {
                                // Don't import if item is in a different folder.  It might be a regular appointment, and
                                // should not be overwritten by a feed version. (bug 14306)
                                // Import only if downloaded version is newer.
                                boolean changed;
                                Invite curInv = calItem.getInvite(inv.getRecurId());
                                if (curInv == null) {
                                    // We have an appointment with the same UID, but don't have an invite
                                    // with the same RECURRENCE-ID.  Treat it as a changed item.
                                    changed = true;
                                } else {
                                    if (inv.getSeqNo() > curInv.getSeqNo()) {
                                        changed = true;
                                    } else if (inv.getSeqNo() == curInv.getSeqNo()) {
                                        // Compare LAST-MODIFIED rather than DTSTAMP. (bug 55735)
                                        changed = inv.getLastModified() > curInv.getLastModified();
                                    } else {
                                        changed = false;
                                    }
                                }
                                importIt = sameFolder && changed;
                            }
                        }
                        if (importIt)
                            addInvite(octxtNoConflicts, inv, folder.getId(), true, addRevision);
                    } catch (ServiceException e) {
                        ZimbraLog.calendar.warn("Skipping bad iCalendar object during import: uid=" + inv.getUid(), e);
                    }
                } else if (obj instanceof ParsedMessage) {
                    DeliveryOptions dopt = new DeliveryOptions().setFolderId(folder).setNoICal(true).setFlags(Flag.BITMASK_UNREAD);
                    addMessage(octxtNoConflicts, (ParsedMessage) obj, dopt, null);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException", e);
            }
        }

        // Delete calendar items that have been deleted in the source feed.
        for (int i : toRemove) {
            delete(octxtNoConflicts, i, MailItem.Type.UNKNOWN);
        }

        // update the subscription to avoid downloading items twice
        long lastModDate = sdata.getLastModifiedDate();
        if (subscription && lastModDate > 0) {
            try {
                setSubscriptionData(octxt, folder.getId(), lastModDate, sdata.getMostRecentGuid());
            } catch (Exception e) {
                ZimbraLog.mailbox.warn("could not update feed metadata", e);
            }
        }

        updateRssDataSource(folder);
    }

    public void setSubscriptionData(OperationContext octxt, int folderId, long date, String guid)
            throws ServiceException {
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

    public void setSyncDate(OperationContext octxt, int folderId, long date) throws ServiceException {
        SetSubscriptionData redoRecorder = new SetSubscriptionData(mId, folderId, date, null);

        boolean success = false;
        try {
            beginTransaction("setSyncDate", octxt, redoRecorder);
            getFolderById(folderId).setSyncDate(date);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public void emptyFolder(OperationContext octxt, int folderId, boolean removeSubfolders)
    throws ServiceException {
        int batchSize = Provisioning.getInstance().getLocalServer().getMailEmptyFolderBatchSize();
        ZimbraLog.mailbox.debug("Emptying folder %s, removeSubfolders=%b, batchSize=%d",
            folderId, removeSubfolders, batchSize);

        List<Integer> folderIds = new ArrayList<Integer>();
        if (!removeSubfolders) {
            folderIds.add(folderId);
        } else {
            List<Folder> folders = getFolderById(octxt, folderId).getSubfolderHierarchy();
            for (Folder folder : folders) {
                folderIds.add(folder.getId());
            }
        }

        // Make sure that the user has the delete permission for all folders in
        // the hierarchy.
        for (int id : folderIds) {
            if ((getEffectivePermissions(octxt, id, MailItem.Type.FOLDER) & ACL.RIGHT_DELETE) == 0) {
               throw ServiceException.PERM_DENIED("not authorized to empty folder " +
                   getFolderById(octxt, id).getPath());
            }
        }
        int lastChangeID = octxt != null && octxt.change != -1 ? octxt.change : getLastChangeID();

        // Delete the items in batches.  (1000 items by default)
        QueryParams params = new QueryParams();
        params.setFolderIds(folderIds).setModifiedSequenceBefore(lastChangeID + 1).setRowLimit(batchSize);
        boolean firstTime = true;
        while (true) {
            // Give other threads a chance to use the mailbox between deletion batches.
            if (firstTime) {
                firstTime = false;
            } else {
                long sleepMillis = LC.empty_folder_batch_sleep_ms.longValue();
                try {
                    ZimbraLog.mailbox.debug("emptyLargeFolder() sleeping for %dms", sleepMillis);
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    ZimbraLog.mailbox.warn("Sleep was interrupted", e);
                }
            }

            // Lock this mailbox to make sure that no one modifies the items we're about to delete.
            lock.lock();
            try {
                DeleteItem redoRecorder = new DeleteItem(mId, MailItem.Type.UNKNOWN, null);
                boolean success = false;
                try {
                    beginTransaction("delete", octxt, redoRecorder);
                    PendingDelete info = DbMailItem.getLeafNodes(this, params);
                    if (info.itemIds.isEmpty()) {
                        break;
                    }
                    redoRecorder.setIds(ArrayUtil.toIntArray(info.itemIds.getAll()));
                    MailItem.delete(this, info, null, true);
                    success = true;
                } finally {
                    endTransaction(success);
                }
            } finally {
                lock.release();
            }
        }

        if (removeSubfolders && folderIds.size() > 1) {
            // delete the subfolders
            folderIds.remove(0);   // 0th position is the folder being emptied which we do not want to delete
            lock.lock();
            try {
                delete(octxt, ArrayUtil.toIntArray(folderIds), MailItem.Type.FOLDER, null);
            } finally {
                lock.release();
            }
        }
    }

    public SearchFolder createSearchFolder(OperationContext octxt, int folderId, String name, String query,
            String types, String sort, int flags, byte color) throws ServiceException {
        return createSearchFolder(octxt, folderId, name, query, types, sort, flags, new Color(color));
    }

    public SearchFolder createSearchFolder(OperationContext octxt, int folderId, String name, String query,
            String types, String sort, int flags, Color color) throws ServiceException {
        CreateSavedSearch redoRecorder = new CreateSavedSearch(mId, folderId, name, query, types, sort, flags, color);

        boolean success = false;
        try {
            beginTransaction("createSearchFolder", octxt, redoRecorder);
            CreateSavedSearch redoPlayer = (CreateSavedSearch) currentChange.getRedoPlayer();

            int searchId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getSearchId());
            SearchFolder search = SearchFolder.create(searchId, getFolderById(folderId), name, query, types, sort,
                    flags, color, null);
            redoRecorder.setSearchId(search.getId());
            success = true;
            return search;
        } finally {
            endTransaction(success);
        }
    }

    public void modifySearchFolder(OperationContext octxt, int id, String query, String types, String sort)
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

    public Mountpoint createMountpoint(OperationContext octxt, int folderId, String name, String ownerId, int remoteId,
            MailItem.Type view, int flags, byte color, boolean showReminders) throws ServiceException {
        return createMountpoint(octxt, folderId, name, ownerId, remoteId, view, flags, new Color(color),
                showReminders);
    }

    public Mountpoint createMountpoint(OperationContext octxt, int folderId, String name, String ownerId, int remoteId,
            MailItem.Type view, int flags, Color color, boolean showReminders) throws ServiceException {
        CreateMountpoint redoRecorder = new CreateMountpoint(mId, folderId, name, ownerId, remoteId, view, flags, color,
                showReminders);

        boolean success = false;
        try {
            beginTransaction("createMountpoint", octxt, redoRecorder);
            CreateMountpoint redoPlayer = (CreateMountpoint) currentChange.getRedoPlayer();

            int mptId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getId());
            Mountpoint mpt = Mountpoint.create(mptId, getFolderById(folderId), name, ownerId, remoteId, view, flags,
                    color, showReminders, null);
            redoRecorder.setId(mpt.getId());
            success = true;
            return mpt;
        } finally {
            endTransaction(success);
        }
    }

    public void enableSharedReminder(OperationContext octxt, int mountpointId, boolean enable) throws ServiceException {
        EnableSharedReminder redoRecorder = new EnableSharedReminder(mId, mountpointId, enable);

        boolean success = false;
        try {
            beginTransaction("enableSharedReminders", octxt, redoRecorder);
            Mountpoint mpt = getMountpointById(octxt, mountpointId);
            mpt.enableReminder(enable);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    /**
     * Purges messages in system folders based on user- and admin-level purge settings
     * on the account.
     */
    public boolean purgeMessages(OperationContext octxt) throws ServiceException {
        // Look up the account outside the synchronized block, so that the mailbox
        // doesn't get locked due to an unresponsive LDAP server (see bug 33650).
        int batchSize = Provisioning.getInstance().getLocalServer().getMailPurgeBatchSize();
        lock.lock();
        try {
            return purgeMessages(octxt, getAccount(), batchSize);
        } finally {
            lock.release();
        }
    }

    /**
     * Returns {@code true} if all messages that meet the purge criteria were purged, {@code false} if the number of
     * messages to purge in any folder exceeded {@code maxItemsPerFolder}.
     */
    private boolean purgeMessages(OperationContext octxt, Account acct, Integer maxItemsPerFolder) throws ServiceException {
        assert(lock.isLocked());
        if (ZimbraLog.purge.isDebugEnabled()) {
            ZimbraLog.purge.debug("System retention policy: Trash=%s, Junk=%s, All messages=%s, Dumpster=%s",
                acct.getMailTrashLifetimeAsString(),
                acct.getMailSpamLifetimeAsString(),
                acct.getMailMessageLifetimeAsString(),
                acct.getMailDumpsterLifetimeAsString());
            ZimbraLog.purge.debug("User-specified retention policy: Inbox read=%s, Inbox unread=%s, Sent=%s, Junk=%s, Trash=%s",
                acct.getPrefInboxReadLifetimeAsString(),
                acct.getPrefInboxUnreadLifetimeAsString(),
                acct.getPrefSentLifetimeAsString(),
                acct.getPrefJunkLifetimeAsString(),
                acct.getPrefTrashLifetimeAsString());
        }

        long globalTimeout = acct.getMailMessageLifetime();
        long systemTrashTimeout = acct.getMailTrashLifetime();
        long systemJunkTimeout = acct.getMailSpamLifetime();
        boolean dumpsterPurgeEnabled = acct.isDumpsterPurgeEnabled();
        long systemDumpsterTimeoutMillis = dumpsterPurgeEnabled ? acct.getMailDumpsterLifetime() : 0;

        long userInboxReadTimeout = acct.getPrefInboxReadLifetime();
        long userInboxUnreadTimeout = acct.getPrefInboxUnreadLifetime();
        long userTrashTimeout = acct.getPrefTrashLifetime();
        long userJunkTimeout = acct.getPrefJunkLifetime();
        long userSentTimeout = acct.getPrefSentLifetime();

        long trashTimeout = pickTimeout(systemTrashTimeout, userTrashTimeout);
        long spamTimeout = pickTimeout(systemJunkTimeout, userJunkTimeout);

        if (globalTimeout <= 0 && trashTimeout <= 0 && spamTimeout <= 0 &&
            userInboxReadTimeout <= 0 && userInboxReadTimeout <= 0 &&
            userInboxUnreadTimeout <= 0 && userSentTimeout <= 0 && systemDumpsterTimeoutMillis <= 0) {
            ZimbraLog.purge.debug("Retention policy does not require purge.");
            return true;
        }

        ZimbraLog.purge.info("Purging messages.");

        // sanity-check the really dangerous value...
        if (globalTimeout > 0 && globalTimeout < Constants.MILLIS_PER_MONTH) {
            // this min is also used by POP3 EXPIRE command. update Pop3Handler.MIN_EPXIRE_DAYS if it changes.
            ZimbraLog.purge.warn("global message timeout < 1 month; defaulting to 31 days");
            globalTimeout = Constants.MILLIS_PER_MONTH;
        }

        PurgeOldMessages redoRecorder = new PurgeOldMessages(mId);

        boolean success = false;
        try {
            beginTransaction("purgeMessages", octxt, redoRecorder);

            // get the folders we're going to be purging
            Folder trash = getFolderById(ID_FOLDER_TRASH);
            Folder spam  = getFolderById(ID_FOLDER_SPAM);
            Folder sent  = getFolderById(ID_FOLDER_SENT);
            Folder inbox = getFolderById(ID_FOLDER_INBOX);

            boolean purgedAll = true;

            if (globalTimeout > 0) {
                int numPurged = Folder.purgeMessages(this, null, getOperationTimestampMillis() - globalTimeout, null, false, false, maxItemsPerFolder);
                ZimbraLog.purge.debug("Purged %d messages from All Folders", numPurged);
                purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
            }
            if (trashTimeout > 0) {
                boolean useChangeDate = acct.getBooleanAttr(Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, true);
                int numPurged = Folder.purgeMessages(this, trash, getOperationTimestampMillis() - trashTimeout, null, useChangeDate, true, maxItemsPerFolder);
                ZimbraLog.purge.debug("Purged %d messages from Trash", numPurged);
                purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
            }
            if (spamTimeout > 0) {
                boolean useChangeDate = acct.isMailPurgeUseChangeDateForSpam();
                int numPurged = Folder.purgeMessages(this, spam, getOperationTimestampMillis() - spamTimeout, null, useChangeDate, false, maxItemsPerFolder);
                purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
                ZimbraLog.purge.debug("Purged %d messages from Spam", numPurged);
            }
            if (userInboxReadTimeout > 0) {
                int numPurged = Folder.purgeMessages(this, inbox, getOperationTimestampMillis() - userInboxReadTimeout, false, false, false, maxItemsPerFolder);
                purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
                ZimbraLog.purge.debug("Purged %d read messages from Inbox", numPurged);
            }
            if (userInboxUnreadTimeout > 0) {
                int numPurged = Folder.purgeMessages(this, inbox, getOperationTimestampMillis() - userInboxUnreadTimeout, true, false, false, maxItemsPerFolder);
                purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
                ZimbraLog.purge.debug("Purged %d unread messages from Inbox", numPurged);
            }
            if (userSentTimeout > 0) {
                int numPurged = Folder.purgeMessages(this, sent, getOperationTimestampMillis() - userSentTimeout, null, false, false, maxItemsPerFolder);
                purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
                ZimbraLog.purge.debug("Purged %d messages from Sent", numPurged);
            }
            if (systemDumpsterTimeoutMillis > 0) {
                int numPurged = purgeDumpster(getOperationTimestampMillis() - systemDumpsterTimeoutMillis, maxItemsPerFolder);
                ZimbraLog.purge.debug("Purged %d messages from Dumpster", numPurged);
                purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
            }

            // Process any folders that have retention policy set.
            for (Folder folder : getFolderList(octxt, SortBy.NONE)) {
                RetentionPolicy rp =
                    RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(folder.getRetentionPolicy());
                for (Policy policy : rp.getPurgePolicy()) {
                    long folderLifetime;

                    try {
                        folderLifetime = DateUtil.getTimeInterval(policy.getLifetime());
                    } catch (ServiceException e) {
                        ZimbraLog.purge.error("Invalid purge lifetime set for folder %s.", folder.getPath(), e);
                        continue;
                    }

                    long folderTimeout = getOperationTimestampMillis() - folderLifetime;
                    int numPurged = Folder.purgeMessages(this, folder, folderTimeout, null, false, true, maxItemsPerFolder);
                    purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
                }
            }

            // Process any tags that have retention policy set.
            for (Tag tag : getTagList(octxt)) {
                RetentionPolicy rp =
                    RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(tag.getRetentionPolicy());
                for (Policy policy : rp.getPurgePolicy()) {
                    long tagLifetime;
                    try {
                        tagLifetime = DateUtil.getTimeInterval(policy.getLifetime());
                    } catch (ServiceException e) {
                        ZimbraLog.purge.error("Invalid purge lifetime set for tag %s.", tag.getName(), e);
                        continue;
                    }

                    long tagTimeout = getOperationTimestampMillis() - tagLifetime;
                    PendingDelete info = DbTag.getLeafNodes(this, tag, (int) (tagTimeout / 1000), maxItemsPerFolder);
                    MailItem.delete(this, info, null, false);
                    List<Integer> ids = info.itemIds.getIds(Type.MESSAGE);
                    int numPurged = (ids == null ? 0 : ids.size());
                    purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
                }
            }

            // deletes have already been collected, so fetch the tombstones and write once
            TypedIdList tombstones = collectPendingTombstones();
            if (tombstones != null && !tombstones.isEmpty()) {
                DbMailItem.writeTombstones(this, tombstones);
            }

            if (Threader.isHashPurgeAllowed(acct)) {
                int convTimeoutSecs = (int) (LC.conversation_max_age_ms.longValue() / Constants.MILLIS_PER_SECOND);
                DbMailItem.closeOldConversations(this, getOperationTimestamp() - convTimeoutSecs);
            }

            if (isTrackingSync()) {
                int tombstoneTimeoutSecs = (int) (LC.tombstone_max_age_ms.longValue() / Constants.MILLIS_PER_SECOND);
                int largestTrimmed = DbMailItem.purgeTombstones(this, getOperationTimestamp() - tombstoneTimeoutSecs);
                if (largestTrimmed > getSyncCutoff()) {
                    currentChange.sync = largestTrimmed;
                    DbMailbox.setSyncCutoff(this, currentChange.sync);
                }
            }

            success = true;
            ZimbraLog.purge.debug("purgedAll=%b", purgedAll);
            return purgedAll;
        } finally {
            endTransaction(success);
        }
    }

    private boolean updatePurgedAll(boolean purgedAll, int numDeleted, Integer maxItems) {
        return purgedAll && (maxItems == null || numDeleted < maxItems);
    }

    /** Returns the smaller non-zero value, or <tt>0</tt> if both
     *  <tt>t1</tt> and <tt>t2</tt> are <tt>0</tt>. */
    private long pickTimeout(long t1, long t2) {
        if (t1 == 0)
            return t2;
        if (t2 == 0)
            return t1;
        return Math.min(t1, t2);
    }

    public void purgeImapDeleted(OperationContext octxt) throws ServiceException {
        PurgeImapDeleted redoRecorder = new PurgeImapDeleted(mId);
        boolean success = false;
        try {
            beginTransaction("purgeImapDeleted", octxt, redoRecorder);

            Set<Folder> purgeable = getAccessibleFolders((short) (ACL.RIGHT_READ | ACL.RIGHT_DELETE));

            // short-circuit the DB call if we're tracking \Deleted counts and they're all 0
            boolean skipDB = false;
            if (getVersion().atLeast(1, 9)) {
                int deleted = 0;
                for (Folder folder : purgeable != null ? purgeable : listAllFolders()) {
                    deleted += folder.getDeletedCount();
                }
                skipDB = deleted == 0;
            }

            if (!skipDB) {
                PendingDelete info = DbTag.getImapDeleted(this, purgeable);
                MailItem.delete(this, info, null, true);
            }
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public WikiItem createWiki(OperationContext octxt, int folderId, String wikiword, String author, String description, InputStream data)
    throws ServiceException {
        return (WikiItem) createDocument(octxt, folderId, wikiword, WikiItem.WIKI_CONTENT_TYPE, author, description, true, data, MailItem.Type.WIKI);
    }

    public Document createDocument(OperationContext octxt, int folderId, String filename, String mimeType,
            String author, String description, InputStream data) throws ServiceException {
        return createDocument(octxt, folderId, filename, mimeType, author, description, true, data, MailItem.Type.DOCUMENT);
    }

    public Document createDocument(OperationContext octxt, int folderId, String filename, String mimeType,
            String author, String description, boolean descEnabled, InputStream data, MailItem.Type type) throws ServiceException {
        try {
            ParsedDocument pd = new ParsedDocument(data, filename, mimeType, System.currentTimeMillis(), author, description, descEnabled);
            return createDocument(octxt, folderId, pd, type, 0);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error writing document blob", ioe);
        }
    }

    public Document createDocument(OperationContext octxt, int folderId, ParsedDocument pd, MailItem.Type type,
            int flags) throws IOException, ServiceException {
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged = sm.stage(pd.getBlob(), this);

        SaveDocument redoRecorder = new SaveDocument(mId, pd.getDigest(), pd.getSize(), folderId, flags);

        boolean success = false;
        try {
            beginTransaction("createDoc", octxt, redoRecorder);

            SaveDocument redoPlayer = (octxt == null ? null : (SaveDocument) octxt.getPlayer());
            int itemId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());

            Document doc;
            switch (type) {
                case DOCUMENT:
                    doc = Document.create(itemId, getFolderById(folderId), pd.getFilename(), pd.getContentType(), pd, null, flags);
                    break;
                case WIKI:
                    doc = WikiItem.create(itemId, getFolderById(folderId), pd.getFilename(), pd, null);
                    break;
                default:
                    throw MailServiceException.INVALID_TYPE(type.toString());
            }

            redoRecorder.setMessageId(itemId);
            redoRecorder.setDocument(pd);
            redoRecorder.setItemType(type);
            redoRecorder.setDescription(pd.getDescription());
            redoRecorder.setFlags(doc.getFlagBitmask());

            // Get the redolog data from the mailbox blob.  This is less than ideal in the
            // HTTP store case because it will result in network access, and possibly an
            // extra write to local disk.  If this becomes a problem, we should update the
            // ParsedDocument constructor to take a DataSource instead of an InputStream.
            MailboxBlob mailboxBlob = doc.setContent(staged, pd);
            redoRecorder.setMessageBodyInfo(new MailboxBlobDataSource(mailboxBlob), mailboxBlob.getSize());

            index.add(doc);

            success = true;
            return doc;
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error writing document blob", ioe);
        } finally {
            endTransaction(success);
            sm.quietDelete(staged);
        }
    }

    public Document addDocumentRevision(OperationContext octxt, int docId, String author, String name,
            String description, InputStream data) throws ServiceException {
        Document doc = getDocumentById(octxt, docId);
        try {
            ParsedDocument pd = new ParsedDocument(data, name, doc.getContentType(), System.currentTimeMillis(), author, description, doc.isDescriptionEnabled());
            return addDocumentRevision(octxt, docId, pd);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error writing document blob", ioe);
        }
    }

    public Document addDocumentRevision(OperationContext octxt, int docId, String author, String name, String description, boolean descEnabled, InputStream data)
    throws ServiceException {
        Document doc = getDocumentById(octxt, docId);
        try {
            ParsedDocument pd = new ParsedDocument(data, name, doc.getContentType(), System.currentTimeMillis(), author, description, descEnabled);
            return addDocumentRevision(octxt, docId, pd);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error writing document blob", ioe);
        }
    }

    public Document addDocumentRevision(OperationContext octxt, int docId, ParsedDocument pd)
    throws IOException, ServiceException {

        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged = sm.stage(pd.getBlob(), this);

        AddDocumentRevision redoRecorder = new AddDocumentRevision(mId, pd.getDigest(), pd.getSize(), 0);

        boolean success = false;
        try {
            beginTransaction("addDocumentRevision", octxt, redoRecorder);

            Document doc = getDocumentById(docId);
            redoRecorder.setDocument(pd);
            redoRecorder.setDocId(docId);
            redoRecorder.setItemType(doc.getType());
            // TODO: simplify the redoRecorder by not subclassing from CreateMessage

            // Get the redolog data from the mailbox blob.  This is less than ideal in the
            // HTTP store case because it will result in network access, and possibly an
            // extra write to local disk.  If this becomes a problem, we should update the
            // ParsedDocument constructor to take a DataSource instead of an InputStream.
            MailboxBlob mailboxBlob = doc.setContent(staged, pd);
            redoRecorder.setMessageBodyInfo(new MailboxBlobDataSource(mailboxBlob), mailboxBlob.getSize());

            index.add(doc);

            success = true;
            return doc;
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error writing document blob", ioe);
        } finally {
            endTransaction(success);
            sm.quietDelete(staged);
        }
    }

    public void purgeRevision(OperationContext octxt, int itemId, int rev, boolean includeOlderRevisions)
            throws ServiceException {
        PurgeRevision redoRecorder = new PurgeRevision(mId, itemId, rev, includeOlderRevisions);
        boolean success = false;
        try {
            beginTransaction("purgeRevision", octxt, redoRecorder);
            MailItem item = getItemById(itemId, MailItem.Type.DOCUMENT);
            item.purgeRevision(rev, includeOlderRevisions);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public Message updateOrCreateChat(OperationContext octxt, ParsedMessage pm, int id) throws IOException, ServiceException {
        // special-case saving a new Chat
        if (id == ID_AUTO_INCREMENT) {
            return createChat(octxt, pm, ID_FOLDER_IM_LOGS, Flag.BITMASK_FROM_ME, null);
        } else {
            return updateChat(octxt, pm, id);
        }
    }

    public Chat createChat(OperationContext octxt, ParsedMessage pm, int folderId, int flags, String[] tags)
    throws IOException, ServiceException {
        if (pm == null) {
            throw ServiceException.INVALID_REQUEST("null ParsedMessage when adding chat to mailbox " + mId, null);
        }

        // write the chat content directly to the mailbox's blob staging area
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged;
        InputStream is = pm.getRawInputStream();
        try {
            staged = sm.stage(is, null, this);
        } finally {
            ByteUtil.closeStream(is);
        }
        String digest = staged.getDigest();
        int size = (int) staged.getSize();

        CreateChat redoRecorder = new CreateChat(mId, digest, size, folderId, flags, tags);
        boolean success = false;
        try {
            beginTransaction("createChat", octxt, redoRecorder);

            Tag.NormalizedTags ntags = new Tag.NormalizedTags(this, tags);

            CreateChat redoPlayer = (octxt == null ? null : (CreateChat) octxt.getPlayer());
            redoRecorder.setMessageBodyInfo(new ParsedMessageDataSource(pm), size);

            int itemId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());

            Chat chat = Chat.create(itemId, getFolderById(folderId), pm, staged, false, flags, ntags);
            redoRecorder.setMessageId(chat.getId());

            MailboxBlob mblob = sm.link(staged, this, itemId, getOperationChangeID());
            markOtherItemDirty(mblob);
            // when we created the Chat, we used the staged locator/size/digest;
            //   make sure that data actually matches the final blob in the store
            chat.updateBlobData(mblob);

            index.add(chat);
            success = true;
            return chat;
        } finally {
            endTransaction(success);
            sm.quietDelete(staged);
        }
    }

    public Chat updateChat(OperationContext octxt, ParsedMessage pm, int id) throws IOException, ServiceException {
        if (pm == null) {
            throw ServiceException.INVALID_REQUEST("null ParsedMessage when updating chat " + id + " in mailbox " + mId, null);
        }

        // write the chat content directly to the mailbox's blob staging area
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged;
        InputStream is = pm.getRawInputStream();
        try {
            staged = sm.stage(is, null, this);
        } finally {
            ByteUtil.closeStream(is);
        }

        String digest = staged.getDigest();
        int size = (int) staged.getSize();

        SaveChat redoRecorder = new SaveChat(mId, id, digest, size, -1, 0, null);
        boolean success = false;
        try {
            beginTransaction("saveChat", octxt, redoRecorder);

            SaveChat redoPlayer = (SaveChat) currentChange.getRedoPlayer();

            redoRecorder.setMessageBodyInfo(new ParsedMessageDataSource(pm), size);

            Chat chat = (Chat) getItemById(id, MailItem.Type.CHAT);

            if (!chat.isMutable())
                throw MailServiceException.IMMUTABLE_OBJECT(id);
            if (!checkItemChangeID(chat))
                throw MailServiceException.MODIFY_CONFLICT();

            // content changed, so we're obliged to change the IMAP uid
            int imapID = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapId());
            redoRecorder.setImapId(imapID);

            // update the content and increment the revision number
            chat.setContent(staged, pm);

            // NOTE: msg is now uncached (will this cause problems during commit/reindex?)
            index.add(chat);

            success = true;
            return chat;
        } finally {
            endTransaction(success);
            sm.quietDelete(staged);
        }
    }

    /**
     * Optimize the underlying database.
     */
    public void optimize(int level) {
        lock.lock();
        try {
            DbConnection conn = DbPool.getConnection(this);

            DbMailbox.optimize(conn, this, level);
            DbPool.quietClose(conn);
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("db optimize failed for mailbox " + getId() + ": " + e);
        } finally {
            lock.release();
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

    private SharedDeliveryCoordinator mSharedDelivCoord = new SharedDeliveryCoordinator();

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

    void addIndexItemToCurrentChange(IndexItemEntry item) {
        assert(lock.isLocked());
        assert(currentChange.isActive());
        currentChange.addIndexItem(item);
    }

    /**
     * for folder view migration.
     */
    void migrateFolderView(OperationContext octxt, Folder f, MailItem.Type newView) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("migrateFolderView", octxt, null);
            f.migrateDefaultView(newView);
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    protected boolean needRedo(OperationContext octxt) {
        // Don't generate redo data for changes made during mailbox version migrations.
        if (!open)
            return false;
        return octxt == null || octxt.needRedo();
    }

    /**
     * Be very careful when changing code in this method.  The order of almost
     * every line of code is important to ensure correct redo logging and crash
     * recovery.
     *
     * @param success true to commit the transaction, false to rollback
     * @throws ServiceException error
     */
    protected void endTransaction(boolean success) throws ServiceException {
        assert !Thread.holdsLock(this) : "Use MailboxLock";
        if (!lock.isLocked()) {
            ZimbraLog.mailbox.warn("transaction canceled because of lock failure");
            return;
        }
        PendingDelete deletes = null; // blob and index to delete
        List<Object> rollbackDeletes = null; // blob to delete for failure cases
        try {
            if (!currentChange.isActive()) {
                // would like to throw here, but it might cover another exception...
                ZimbraLog.mailbox.warn("cannot end a transaction when not inside a transaction", new Exception());
                return;
            }
            if (!currentChange.endChange()) {
                return;
            }
            ServiceException exception = null;

            if (success) {
                List<IndexItemEntry> indexItems = currentChange.indexItems;
                if (!indexItems.isEmpty()) {
                    //TODO: See bug 15072 - we need to clear mCurrentChange.indexItems (it is stored in a temporary) here,
                    // just in case item.reindex() recurses into a new transaction...
                    currentChange.indexItems = new ArrayList<IndexItemEntry>();
                    index.add(indexItems);
                }

                // update mailbox size, folder unread/message counts
                try {
                    snapshotCounts();
                } catch (ServiceException e) {
                    exception = e;
                    success = false;
                }
            }

            DbConnection conn = currentChange.conn;

            // Failure case is very simple.  Just rollback the database and cache
            // and return.  We haven't logged anything to the redo log for this
            // transaction, so no redo cleanup is necessary.
            if (!success) {
                DbPool.quietRollback(conn);
                rollbackDeletes = rollbackCache(currentChange);
                if (exception != null) {
                    throw exception;
                }
                return;
            }

            boolean needRedo = needRedo(currentChange.octxt);
            RedoableOp redoRecorder = currentChange.recorder;
            // Log the change redo record for main transaction.
            if (redoRecorder != null && needRedo) {
                redoRecorder.log(true);
            }
            boolean dbCommitSuccess = false;
            try {
                // Commit the main transaction in database.
                if (conn != null) {
                    try {
                        conn.commit();
                    } catch (Throwable t) {
                        // Any exception during database commit is a disaster
                        // because we don't know if the change is committed or
                        // not.  Force the server to abort.  Next restart will
                        // redo the operation to ensure the change is made and
                        // committed.  (bug 2121)
                        Zimbra.halt("Unable to commit database transaction.  Forcing server to abort.", t);
                    }
                }
                dbCommitSuccess = true;
            } finally {
                if (!dbCommitSuccess) {
                    // Write abort redo records to prevent the transactions from
                    // being redone during crash recovery.

                    // Write abort redo entries before doing database rollback.
                    // If we do rollback first and server crashes, crash
                    // recovery will try to redo the operation.
                    if (needRedo) {
                        if (redoRecorder != null) {
                            redoRecorder.abort();
                        }
                    }
                    DbPool.quietRollback(conn);
                    rollbackDeletes = rollbackCache(currentChange);
                    return;
                }
            }

            if (needRedo) {
                // Write commit record for main transaction. By writing the commit record for main transaction before
                // calling MailItem.reindex(), we are guaranteed to see the commit-main record in the redo stream before
                // commit-index record. This order ensures that during crash recovery the main transaction is redone
                // before indexing. If the order were reversed, crash recovery would attempt to index an item which
                // hasn't been created yet or would attempt to index the item with pre-modification value. The first
                // case would result in a redo error, and the second case would index the wrong value.
                if (redoRecorder != null) {
                    if (currentChange.dirty != null && !currentChange.dirty.changedTypes.isEmpty()) {
                        // if an "all accounts" waitset is active, and this change has an appropriate type,
                        // then we'll need to set a commit-callback
                        AllAccountsRedoCommitCallback cb = AllAccountsRedoCommitCallback.getRedoCallbackIfNecessary(
                                getAccountId(), currentChange.dirty.changedTypes);
                        if (cb != null) {
                            redoRecorder.setCommitCallback(cb);
                        }
                    }
                    redoRecorder.commit();
                }
            }

            if (currentChange.changeId != MailboxChange.NO_CHANGE) {
                index.maybeIndexDeferredItems();
            }
            deletes = currentChange.deletes; // keep a reference for cleanup deletes outside the lock
            // We are finally done with database and redo commits. Cache update comes last.
            commitCache(currentChange);
        } finally {
            lock.release();

            // process cleanup deletes outside the lock as we support alternative blob stores for which a delete may
            // entail a blocking network operation
            if (deletes != null) {
                if (!deletes.indexIds.isEmpty()) {
                    // delete any index entries associated with items deleted from db
                    index.delete(deletes.indexIds);
                }
                if (deletes.blobs != null) {
                    // delete any blobs associated with items deleted from db/index
                    StoreManager sm = StoreManager.getInstance();
                    for (MailboxBlob blob : deletes.blobs) {
                        sm.quietDelete(blob);
                    }
                }
            }
            if (rollbackDeletes != null) {
                StoreManager sm = StoreManager.getInstance();
                for (Object obj : rollbackDeletes) {
                    if (obj instanceof MailboxBlob) {
                        sm.quietDelete((MailboxBlob) obj);
                    } else if (obj instanceof Blob) {
                        sm.quietDelete((Blob) obj);
                    }
                }
            }
        }
    }

    // if the incoming message has one of these flags, don't up our "new messages" counter
    public static final int NON_DELIVERY_FLAGS = Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME | Flag.BITMASK_COPIED | Flag.BITMASK_DELETED;

    void snapshotCounts() throws ServiceException {
        // for write ops, update the "new messages" count in the DB appropriately
        OperationContext octxt = currentChange.octxt;
        RedoableOp player = currentChange.getRedoPlayer();
        RedoableOp recorder = currentChange.recorder;

        if (recorder != null && (player == null || (octxt != null && !octxt.isRedo()))) {
            boolean isNewMessage = recorder.getOperation() == MailboxOperation.CreateMessage;
            if (isNewMessage) {
                CreateMessage cm = (CreateMessage) recorder;
                if (cm.getFolderId() == ID_FOLDER_SPAM || cm.getFolderId() == ID_FOLDER_TRASH) {
                    isNewMessage = false;
                } else if ((cm.getFlags() & NON_DELIVERY_FLAGS) != 0) {
                    isNewMessage = false;
                } else if (octxt != null && octxt.getSession() != null && !octxt.isDelegatedRequest(this)) {
                    isNewMessage = false;
                }

                if (isNewMessage) {
                    String folderList = getAccount().getPrefMailFoldersCheckedForNewMsgIndicator();

                    if (folderList != null) {
                        String[] folderIds = folderList.split(",");

                        isNewMessage = false;
                        for (int i = 0; i < folderIds.length; i++) {
                            if (cm.getFolderId() == Integer.parseInt(folderIds[i])) {
                                isNewMessage = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (isNewMessage) {
                currentChange.recent = mData.recentMessages + 1;
            } else if (octxt != null && mData.recentMessages != 0) {
                Session s = octxt.getSession();
                if (s instanceof SoapSession || (s instanceof SoapSession.DelegateSession &&
                        ((SoapSession.DelegateSession) s).getParentSession().isOfflineSoapSession())) {
                    currentChange.recent = 0;
                }
            }
        }

        if (currentChange.isMailboxRowDirty(mData)) {
            if (currentChange.recent != MailboxChange.NO_CHANGE) {
                ZimbraLog.mailbox.debug("setting recent count to %d", currentChange.recent);
            }
            DbMailbox.updateMailboxStats(this);
        }

        if (currentChange.dirty != null && currentChange.dirty.hasNotifications()) {
            if (currentChange.dirty.created != null) {
                for (MailItem item : currentChange.dirty.created.values()) {
                    if (item instanceof Folder && item.getSize() != 0) {
                        ((Folder) item).saveFolderCounts(false);
                    } else if (item instanceof Tag && item.isUnread()) {
                        ((Tag) item).saveTagCounts();
                    }
                }
            }

            if (currentChange.dirty.modified != null) {
                for (Change change : currentChange.dirty.modified.values()) {
                    if ((change.why & (Change.UNREAD | Change.SIZE)) != 0 && change.what instanceof Folder) {
                        ((Folder) change.what).saveFolderCounts(false);
                    } else if ((change.why & Change.UNREAD | Change.SIZE) != 0 && change.what instanceof Tag) {
                        ((Tag) change.what).saveTagCounts();
                    }
                }
            }
        }

        if (DebugConfig.checkMailboxCacheConsistency && currentChange.dirty != null && currentChange.dirty.hasNotifications()) {
            if (currentChange.dirty.created != null) {
                for (MailItem item : currentChange.dirty.created.values()) {
                    DbMailItem.consistencyCheck(item, item.mData, item.encodeMetadata().toString());
                }
            }
            if (currentChange.dirty.modified != null) {
                for (Change change : currentChange.dirty.modified.values()) {
                    if (change.what instanceof MailItem) {
                        MailItem item = (MailItem) change.what;
                        DbMailItem.consistencyCheck(item, item.mData, item.encodeMetadata().toString());
                    }
                }
            }
        }
    }

    private void commitCache(MailboxChange change) {
        assert(lock.isLocked());
        if (change == null) {
            return;
        }
        ChangeNotification notification = null;

        // save for notifications (below)
        PendingModifications dirty = null;
        if (change.dirty != null && change.dirty.hasNotifications()) {
            dirty = change.dirty;
            change.dirty = new PendingModifications();
        }

        Session source = change.octxt == null ? null : change.octxt.getSession();

        try {
            // the mailbox data has changed, so commit the changes
            if (change.sync != null) {
                mData.trackSync = change.sync;
            }
            if (change.imap != null) {
                mData.trackImap = change.imap;
            }
            if (change.size != MailboxChange.NO_CHANGE) {
                mData.size = change.size;
            }
            if (change.itemId != MailboxChange.NO_CHANGE) {
                mData.lastItemId = change.itemId;
            }
            if (change.contacts != MailboxChange.NO_CHANGE) {
                mData.contacts = change.contacts;
            }
            if (change.changeId != MailboxChange.NO_CHANGE && change.changeId > mData.lastChangeId) {
                mData.lastChangeId   = change.changeId;
                mData.lastChangeDate = change.timestamp;
            }
            if (change.accessed != MailboxChange.NO_CHANGE) {
                mData.lastWriteDate = change.accessed;
            }
            if (change.recent != MailboxChange.NO_CHANGE) {
                mData.recentMessages = change.recent;
            }
            if (change.config != null) {
                if (change.config.getSecond() == null) {
                    if (mData.configKeys != null) {
                        mData.configKeys.remove(change.config.getFirst());
                    }
                } else {
                    if (mData.configKeys == null) {
                        mData.configKeys = new HashSet<String>(1);
                    }
                    mData.configKeys.add(change.config.getFirst());
                }
            }

            if (change.deletes != null && change.deletes.blobs != null) {
                // remove cached messages
                for (String digest : change.deletes.blobDigests) {
                    MessageCache.purge(digest);
                }
            }

            // committed changes, so notify any listeners
            if (dirty != null && dirty.hasNotifications()) {
                try {
                    // try to get a copy of the changeset that *isn't* live
                    dirty = snapshotModifications(dirty);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.warn("error copying notifications; will notify with live set", e);
                }
                try {
                    notification = new ChangeNotification(
                            getAccount(), dirty, change.octxt, mData.lastChangeId, change.timestamp);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.warn("error getting account for the mailbox", e);
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

        if (notification != null) {
            for (Session session : mListeners) {
                try {
                    session.notifyPendingChanges(notification.mods, notification.lastChangeId, source);
                } catch (RuntimeException e) {
                    ZimbraLog.mailbox.error("ignoring error during notification", e);
                }
            }

            MailboxListener.notifyListeners(notification);
        }
    }

    private List<Object> rollbackCache(MailboxChange change) {
        if (change == null) {
            return null;
        }
        try {
            // rolling back changes, so purge dirty items from the various caches
            for (Map<?, ?> map : new Map[] {change.dirty.created, change.dirty.deleted, change.dirty.modified}) {
                if (map != null) {
                    for (Object obj : map.values()) {
                        if (obj instanceof Change) {
                            obj = ((Change) obj).what;
                        }

                        if (obj instanceof Tag || obj == MailItem.Type.TAG) {
                            purge(Type.TAG);
                        } else if (obj instanceof Folder || FOLDER_TYPES.contains(obj)) {
                            purge(Type.FOLDER);
                        } else if (obj instanceof MailItem && change.itemCache != null) {
                            change.itemCache.remove(((MailItem) obj).getId());
                        }
                    }
                }
            }

            // roll back any changes to external items
            List<Object> deletes = new ArrayList<Object>(change.otherDirtyStuff.size());
            for (Object obj : change.otherDirtyStuff) {
                if (obj instanceof MailboxBlob || obj instanceof Blob) {
                    deletes.add(obj);
                } else if (obj instanceof String) {
                    mConvHashes.remove(obj);
                }
            }
            return deletes;
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during cache rollback", e);
            return null;
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
            Map<Integer, MailItem> cache = currentChange.itemCache;
            if (cache == null) {
                return;
            }
            int excess = cache.size() - sizeTarget;
            if (excess <= 0) {
                return;
            }
            // cache the overflow to avoid the Iterator's ConcurrentModificationException
            MailItem[] overflow = new MailItem[excess];
            int i = 0;
            for (MailItem item : cache.values()) {
                overflow[i++] = item;
                if (i >= excess) {
                    break;
                }
            }
            // trim the excess; note that "uncache" can cascade and take out child items
            while (--i >= 0) {
                if (cache.size() <= sizeTarget) {
                    return;
                }
                try {
                    uncache(overflow[i]);
                } catch (ServiceException e) {
                }
            }
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during item cache trim", e);
        }
    }


    public boolean attachmentsIndexingEnabled() throws ServiceException {
        return getAccount().isAttachmentsIndexingEnabled();
    }

    private void logCacheActivity(Integer key, MailItem.Type type, MailItem item) {
        // The global item cache counter always gets updated
        if (!isCachedType(type))
            ZimbraPerf.COUNTER_MBOX_ITEM_CACHE.increment(item == null ? 0 : 100);

        // the per-access log only gets updated when cache or perf debug logging is on
        if (!ZimbraLog.cache.isDebugEnabled())
            return;

        if (item == null) {
            ZimbraLog.cache.debug("Cache miss for item " + key + " in mailbox " + getId());
            return;
        }

        // Don't log cache hits for folders, search folders and tags.  We always
        // keep these in memory, so cache hits are not interesting.
        if (isCachedType(type)) {
            return;
        }
        ZimbraLog.cache.debug("Cache hit for %s %d in mailbox %d", type, key, getId());
    }

    public MailItem lock(OperationContext octxt, int itemId, MailItem.Type type, String accountId)
            throws ServiceException {
        LockItem redoRecorder = new LockItem(mId, itemId, type, accountId);

        boolean success = false;
        try {
            beginTransaction("lock", octxt, redoRecorder);
            MailItem item = getItemById(itemId, type);
            item.lock(Provisioning.getInstance().getAccountById(accountId));
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    public void unlock(OperationContext octxt, int itemId, MailItem.Type type, String accountId)
            throws ServiceException {
        UnlockItem redoRecorder = new UnlockItem(mId, itemId, type, accountId);

        boolean success = false;
        try {
            beginTransaction("unlock", octxt, redoRecorder);
            MailItem item = getItemById(itemId, type);
            item.unlock(Provisioning.getInstance().getAccountById(accountId));
            success = true;
        } finally {
            endTransaction(success);
        }
    }

    public Comment createComment(OperationContext octxt, int parentId, String text, String creatorId)
            throws ServiceException {
        CreateComment redoRecorder = new CreateComment(mId, parentId, text, creatorId);

        boolean success = false;
        try {
            beginTransaction("createComment", octxt, redoRecorder);

            MailItem parent = getItemById(octxt, parentId, Type.UNKNOWN);
            if (parent.getType() != Type.DOCUMENT) {
                throw MailServiceException.CANNOT_PARENT();
            }
            CreateComment redoPlayer = (CreateComment) currentChange.getRedoPlayer();
            int itemId = redoPlayer == null ? getNextItemId(ID_AUTO_INCREMENT) : redoPlayer.getItemId();
            Comment comment = Comment.create(this, parent, itemId, text, creatorId, null);
            redoRecorder.setItemId(comment.getId());
            index.add(comment);
            success = true;
            return comment;
        } finally {
            endTransaction(success);
        }
    }

    public Link createLink(OperationContext octxt, int folderId, String name, String ownerId, int remoteId) throws ServiceException {
        CreateLink redoRecorder = new CreateLink(mId, folderId, name, ownerId, remoteId);

        boolean success = false;
        try {
            beginTransaction("createLink", octxt, redoRecorder);
            CreateLink redoPlayer = (CreateLink) currentChange.getRedoPlayer();
            int itemId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getId());
            Link link = Link.create(getFolderById(folderId), itemId, name, ownerId, remoteId, null);
            redoRecorder.setId(link.getId());
            success = true;
            return link;
        } finally {
            endTransaction(success);
        }
    }

    public Collection<Comment> getComments(OperationContext octxt, int parentId, int offset, int length)
            throws ServiceException {
        return getComments(octxt, parentId, offset, length, false);
    }

    Collection<Comment> getComments(OperationContext octxt, int parentId, int offset, int length, boolean fromDumpster)
            throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("getComments", octxt, null);
            MailItem parent = getItemById(parentId, Type.UNKNOWN, fromDumpster);
            return parent.getComments(SortBy.DATE_DESC, offset, length);
        } finally {
            endTransaction(success);
        }
    }

    public MailItem markMetadataChanged(OperationContext octxt, int itemId) throws ServiceException {
        boolean success = false;
        try {
            beginTransaction("markMetadataChanged", octxt, null);
            MailItem item = getItemById(octxt, itemId, Type.UNKNOWN);
            item.markMetadataChanged();
            success = true;
            return item;
        } finally {
            endTransaction(success);
        }
    }

    protected void migrateWikiFolders() throws ServiceException {
        MigrateToDocuments migrate = new MigrateToDocuments();
        try {
            migrate.handleMailbox(this);
            ZimbraLog.mailbox.info("wiki folder migration finished");
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("wiki folder migration failed for "+getAccount().getName(), e);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", mId)
            .add("account", mData.accountId)
            .add("lastItemId", mData.lastItemId)
            .add("size", mData.size)
            .toString();
    }

    public boolean dumpsterEnabled() {
        boolean enabled = true;
        try {
            enabled = getAccount().isDumpsterEnabled();
        } catch (ServiceException e) {}
        return enabled;
    }

    public boolean useDumpsterForSpam() { return true; }

    /**
     * Return true if the folder is a internally managed system folder which should not normally be modified
     * Used during ZD account import to ignore entries in LocalMailbox 'Notification Mountpoints'.
     *
     * @param folderId not used
     */
    public boolean isImmutableSystemFolder(int folderId) {
        return false;
    }

    boolean isChildFolderPermitted(int folderId) {
        return (folderId != Mailbox.ID_FOLDER_SPAM);
    }

    /**
     * temporarily for bug 46549
     */
    public boolean isNewItemIdValid(int id) {
        return id < 2<<29;
    }
}
