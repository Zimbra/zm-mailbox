/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 Synacor, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ExistingParentFolderStoreAndUnmatchedPart;
import com.zimbra.common.mailbox.FolderConstants;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockContext;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.OpContext;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.mailbox.ZimbraQueryHitResults;
import com.zimbra.common.mailbox.ZimbraSearchParams;
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
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ShareLocator;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailItem.QueryParams;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.db.DbVolumeBlobs;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.EventStore;
import com.zimbra.cs.event.logger.EventLogger;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.fb.LocalFreeBusyProvider;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.DomainBrowseTerm;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.history.SavedSearchPromptLog;
import com.zimbra.cs.index.history.SavedSearchPromptLog.SavedSearchStatus;
import com.zimbra.cs.index.history.SearchHistory;
import com.zimbra.cs.index.history.SearchHistory.SearchHistoryParams;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.CalendarItem.Callback;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailItem.PendingDelete;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.MailboxListener.ChangeNotification;
import com.zimbra.cs.mailbox.Message.EventFlag;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.mailbox.Tag.NormalizedTags;
import com.zimbra.cs.mailbox.cache.CachedObjectRegistry;
import com.zimbra.cs.mailbox.cache.FolderCache;
import com.zimbra.cs.mailbox.cache.ItemCache;
import com.zimbra.cs.mailbox.cache.ItemCache.CachedTagsAndFolders;
import com.zimbra.cs.mailbox.cache.LocalFolderCache;
import com.zimbra.cs.mailbox.cache.TagCache;
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
import com.zimbra.cs.ml.ClassificationExecutionContext;
import com.zimbra.cs.ml.classifier.ClassifierManager;
import com.zimbra.cs.pop3.Pop3Message;
import com.zimbra.cs.redolog.op.AddDocumentRevision;
import com.zimbra.cs.redolog.op.AddSearchHistoryEntry;
import com.zimbra.cs.redolog.op.AlterItemTag;
import com.zimbra.cs.redolog.op.ColorItem;
import com.zimbra.cs.redolog.op.CopyItem;
import com.zimbra.cs.redolog.op.CreateCalendarItemPlayer;
import com.zimbra.cs.redolog.op.CreateCalendarItemRecorder;
import com.zimbra.cs.redolog.op.CreateChat;
import com.zimbra.cs.redolog.op.CreateComment;
import com.zimbra.cs.redolog.op.CreateContact;
import com.zimbra.cs.redolog.op.CreateFolder;
import com.zimbra.cs.redolog.op.CreateFolderPath;
import com.zimbra.cs.redolog.op.CreateInvite;
import com.zimbra.cs.redolog.op.CreateLink;
import com.zimbra.cs.redolog.op.CreateMailbox;
import com.zimbra.cs.redolog.op.CreateMessage;
import com.zimbra.cs.redolog.op.CreateMountpoint;
import com.zimbra.cs.redolog.op.CreateNote;
import com.zimbra.cs.redolog.op.CreateSavedSearch;
import com.zimbra.cs.redolog.op.CreateSmartFolder;
import com.zimbra.cs.redolog.op.CreateTag;
import com.zimbra.cs.redolog.op.DateItem;
import com.zimbra.cs.redolog.op.DeleteConfig;
import com.zimbra.cs.redolog.op.DeleteItem;
import com.zimbra.cs.redolog.op.DeleteItemFromDumpster;
import com.zimbra.cs.redolog.op.DeleteMailbox;
import com.zimbra.cs.redolog.op.DismissCalendarItemAlarm;
import com.zimbra.cs.redolog.op.EditNote;
import com.zimbra.cs.redolog.op.EnableSharedReminder;
import com.zimbra.cs.redolog.op.FixCalendarItemEndTime;
import com.zimbra.cs.redolog.op.FixCalendarItemPriority;
import com.zimbra.cs.redolog.op.FixCalendarItemTZ;
import com.zimbra.cs.redolog.op.GrantAccess;
import com.zimbra.cs.redolog.op.ICalReply;
import com.zimbra.cs.redolog.op.ImapCopyItem;
import com.zimbra.cs.redolog.op.LockItem;
import com.zimbra.cs.redolog.op.ModifyContact;
import com.zimbra.cs.redolog.op.ModifyInvitePartStat;
import com.zimbra.cs.redolog.op.ModifySavedSearch;
import com.zimbra.cs.redolog.op.MoveItem;
import com.zimbra.cs.redolog.op.PurgeImapDeleted;
import com.zimbra.cs.redolog.op.PurgeOldMessages;
import com.zimbra.cs.redolog.op.PurgeRevision;
import com.zimbra.cs.redolog.op.PurgeSearchHistory;
import com.zimbra.cs.redolog.op.RecoverItem;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.op.RefreshMountpoint;
import com.zimbra.cs.redolog.op.RenameItem;
import com.zimbra.cs.redolog.op.RenameItemPath;
import com.zimbra.cs.redolog.op.RenameMailbox;
import com.zimbra.cs.redolog.op.RepositionNote;
import com.zimbra.cs.redolog.op.RevokeAccess;
import com.zimbra.cs.redolog.op.SaveChat;
import com.zimbra.cs.redolog.op.SaveDocument;
import com.zimbra.cs.redolog.op.SaveDraft;
import com.zimbra.cs.redolog.op.SetActiveSyncDisabled;
import com.zimbra.cs.redolog.op.SetCalendarItem;
import com.zimbra.cs.redolog.op.SetConfig;
import com.zimbra.cs.redolog.op.SetCustomData;
import com.zimbra.cs.redolog.op.SetFolderDefaultView;
import com.zimbra.cs.redolog.op.SetFolderUrl;
import com.zimbra.cs.redolog.op.SetImapUid;
import com.zimbra.cs.redolog.op.SetItemTags;
import com.zimbra.cs.redolog.op.SetPermissions;
import com.zimbra.cs.redolog.op.SetRetentionPolicy;
import com.zimbra.cs.redolog.op.SetSavedSearchStatus;
import com.zimbra.cs.redolog.op.SetSubscriptionData;
import com.zimbra.cs.redolog.op.SetWebOfflineSyncDays;
import com.zimbra.cs.redolog.op.SnoozeCalendarItemAlarm;
import com.zimbra.cs.redolog.op.StoreIncomingBlob;
import com.zimbra.cs.redolog.op.TrackImap;
import com.zimbra.cs.redolog.op.TrackSync;
import com.zimbra.cs.redolog.op.UnlockItem;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FeedManager;
import com.zimbra.cs.service.mail.CopyActionResult;
import com.zimbra.cs.service.mail.ItemActionHelper;
import com.zimbra.cs.service.mail.SendDeliveryReport;
import com.zimbra.cs.service.util.ItemData;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.service.util.SpamHandler.SpamReport;
import com.zimbra.cs.session.AllAccountsRedoCommitCallback;
import com.zimbra.cs.session.PendingLocalModifications;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.Session.SourceSessionInfo;
import com.zimbra.cs.session.Session.Type;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.MailboxBlobDataSource;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.StoreManager.StoreFeature;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;
import com.zimbra.cs.util.SpoolingCache;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;

/**
 * @since Jun 13, 2004
 */
public class Mailbox implements MailboxStore {

    /* these probably should be ints... */
    public static final String BROWSE_BY_DOMAINS = "domains";
    public static final String BROWSE_BY_OBJECTS = "objects";
    public static final String BROWSE_BY_ATTACHMENTS = "attachments";

    public static final int ID_AUTO_INCREMENT = FolderConstants.ID_AUTO_INCREMENT; // -1;
    public static final int ID_FOLDER_USER_ROOT = FolderConstants.ID_FOLDER_USER_ROOT; // 1;
    public static final int ID_FOLDER_INBOX = FolderConstants.ID_FOLDER_INBOX; // 2;
    public static final int ID_FOLDER_TRASH = FolderConstants.ID_FOLDER_TRASH; // 3;
    public static final int ID_FOLDER_SPAM = FolderConstants.ID_FOLDER_SPAM; // 4;
    public static final int ID_FOLDER_SENT = FolderConstants.ID_FOLDER_SENT; // 5;
    public static final int ID_FOLDER_DRAFTS = FolderConstants.ID_FOLDER_DRAFTS; // 6;
    public static final int ID_FOLDER_CONTACTS = FolderConstants.ID_FOLDER_CONTACTS; // 7;
    public static final int ID_FOLDER_TAGS = FolderConstants.ID_FOLDER_TAGS; // 8;
    public static final int ID_FOLDER_CONVERSATIONS = FolderConstants.ID_FOLDER_CONVERSATIONS; // 9;
    public static final int ID_FOLDER_CALENDAR = FolderConstants.ID_FOLDER_CALENDAR; // 10;
    public static final int ID_FOLDER_ROOT = FolderConstants.ID_FOLDER_ROOT; // 11;

    @Deprecated
    // no longer created in mailboxes since Helix (bug 39647).  old mboxes may still contain a system folder with id 12
    public static final int ID_FOLDER_NOTEBOOK = FolderConstants.ID_FOLDER_NOTEBOOK; // 12;
    public static final int ID_FOLDER_AUTO_CONTACTS = FolderConstants.ID_FOLDER_AUTO_CONTACTS; // 13;
    public static final int ID_FOLDER_IM_LOGS = FolderConstants.ID_FOLDER_IM_LOGS; // 14;
    public static final int ID_FOLDER_TASKS = FolderConstants.ID_FOLDER_TASKS; // 15;
    public static final int ID_FOLDER_BRIEFCASE = FolderConstants.ID_FOLDER_BRIEFCASE; // 16;
    public static final int ID_FOLDER_COMMENTS = FolderConstants.ID_FOLDER_COMMENTS; // 17;
    // ID_FOLDER_PROFILE Was used for folder related to ProfileServlet which was used in pre-release Iron Maiden only.
    // Old mailboxes may still contain a system folder with id 18
    @Deprecated
    public static final int ID_FOLDER_PROFILE = FolderConstants.ID_FOLDER_PROFILE; // 18;
    //This id should be incremented if any new ID_FOLDER_* is added.
    public static final int HIGHEST_SYSTEM_ID = FolderConstants.HIGHEST_SYSTEM_ID; // 18;

    public static final int FIRST_USER_ID = 256;


    public static final String CONF_PREVIOUS_MAILBOX_IDS = "prev_mbox_ids";

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
        public int itemcacheCheckpoint;
        public int lastSearchId;

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
            mbd.lastChangeId = lastChangeId;
            mbd.lastChangeDate = lastChangeDate;
            mbd.lastWriteDate = lastWriteDate;
            mbd.recentMessages = recentMessages;
            mbd.trackSync = trackSync;
            mbd.trackImap = trackImap;
            if (configKeys != null) {
                mbd.configKeys = new HashSet<String>(configKeys);
            }
            mbd.version = version;
            mbd.itemcacheCheckpoint = itemcacheCheckpoint;
            mbd.lastSearchId = lastSearchId;
            return mbd;
        }
    }

    public static final class IndexItemEntry {
        final List<IndexDocument> documents;
        final MailItem item;

        public IndexItemEntry(MailItem item, List<IndexDocument> docs) {
            this.item = item;
            this.documents = docs;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("id", item.getId()).toString();
        }

        public List<IndexDocument> getDocuments() {
            return documents;
        }

        public MailItem getItem() {
            return item;
        }
    }

    private final class MailboxChange {
        private static final int NO_CHANGE = -1;

        long timestamp = System.currentTimeMillis();
        int depth = 0;
        boolean active;
        DbConnection conn = null;
        RedoableOp recorder = null;
        List<MailItem> indexItems = new ArrayList<MailItem>();
        ItemCache itemCache = null;
        OperationContext octxt = null;
        TargetConstraint tcon = null;

        Integer sync = null;
        Boolean imap = null;
        long size = NO_CHANGE;
        int itemId = NO_CHANGE;
        int searchId = NO_CHANGE;
        int changeId = NO_CHANGE;
        int contacts = NO_CHANGE;
        int accessed = NO_CHANGE;
        int recent = NO_CHANGE;
        Pair<String, Metadata> config = null;

        PendingLocalModifications dirty = new PendingLocalModifications();
        final List<Object> otherDirtyStuff = new LinkedList<Object>();
        PendingDelete deletes = null;
        private boolean writeChange;

        MailboxChange() {
        }

        MailboxOperation getOperation() {
            if (recorder != null) {
                return recorder.getOperation();
            }
            return null;
        }

        void setTimestamp(long millis) {
            if (depth == 1) {
                timestamp = millis;
            }
        }

        boolean startChange(String caller, OperationContext ctxt, RedoableOp op, boolean write) {
            active = true;
            if (depth++ == 0) {
                octxt = ctxt;
                recorder = op;
                this.writeChange = write;
                ZimbraLog.mailbox.debug("startChange beginning operation (%s) %s", caller, this);
                return true;
            } else {
                assert (write == false || writeChange); // if this call is a
                                                        // write, the first one
                                                        // must be a write
                ZimbraLog.mailbox.debug("startChange increased stack depth (%s) %s", caller, this);
                return false;
            }
        }

        boolean endChange() {
            depth--;
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                ZimbraLog.mailbox.debug(
                        "endChange decreased stack depth %s%s %s",
                        (depth > 0) ? "" : "(ending operation) ",
                        recorder == null ? "" : recorder.getClass().getSimpleName(), this);
            }
            return (depth == 0);
        }

        boolean isActive() {
            return active;
        }

        DbConnection getConnection() throws ServiceException {
            if (conn == null) {
                conn = DbPool.getConnection(Mailbox.this);
                ZimbraLog.mailbox.debug("fetching new DB connection %s", this);
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
         * Add an item to the list of things to be queued for indexing at the end of the current transaction.
         * This is used when immediate (in-transaction) indexing fails
         */
        void addIndexItem(MailItem item) {
            indexItems.add(item);
        }

        void addPendingDelete(PendingDelete info) {
            if (deletes == null) {
                deletes = info;
            } else {
                deletes.add(info);
            }
        }

        boolean isMailboxRowDirty() {
            if (recent != NO_CHANGE || size != NO_CHANGE || contacts != NO_CHANGE) {
                return true;
            }
            if (itemId != NO_CHANGE && itemId / DbMailbox.ITEM_CHECKPOINT_INCREMENT > mData.lastItemId / DbMailbox.ITEM_CHECKPOINT_INCREMENT) {
                return true;
            }
            if (changeId != NO_CHANGE && changeId / DbMailbox.CHANGE_CHECKPOINT_INCREMENT > mData.lastChangeId / DbMailbox.CHANGE_CHECKPOINT_INCREMENT) {
                return true;
            }
            if (searchId != NO_CHANGE && searchId / DbMailbox.SEARCH_ID_CHECKPOINT_INCREMENT > mData.lastSearchId / DbMailbox.SEARCH_ID_CHECKPOINT_INCREMENT) {
                return true;
            }
            return false;
        }

        void reset() {
            DbPool.quietClose(conn);
            this.active = false;
            this.conn = null;
            this.octxt = null;
            this.tcon = null;
            this.imap = null;
            this.depth = 0;
            this.size = NO_CHANGE;
            this.changeId = NO_CHANGE;
            this.itemId = NO_CHANGE;
            this.searchId = NO_CHANGE;
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
            threadChange.remove();

            ZimbraLog.mailbox.debug("clearing change");
        }

        boolean hasChanges() {
            if (sync != null) {
                return true;
            } else if (imap != null) {
                return true;
            } else if (size != MailboxChange.NO_CHANGE) {
                return true;
            } else if (itemId != MailboxChange.NO_CHANGE) {
                return true;
            } else if (searchId != MailboxChange.NO_CHANGE) {
                return true;
            } else if (contacts != MailboxChange.NO_CHANGE) {
                return true;
            } else if (changeId != MailboxChange.NO_CHANGE /*&& changeId > mData.lastChangeId*/) {
                return true;
            } else if (accessed != MailboxChange.NO_CHANGE) {
                return true;
            } else if (recent != MailboxChange.NO_CHANGE) {
                return true;
            } else if (config != null) {
                return true;
            } else if (deletes != null) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
            helper.add("depth", depth);
            if (changeId != MailboxChange.NO_CHANGE) {
                helper.add("changeId", changeId);
            }

            helper.add("active", active);
            if (size != MailboxChange.NO_CHANGE) {
                helper.add("size", size);
            }
            if (itemId != MailboxChange.NO_CHANGE) {
                helper.add("itemId", itemId);
            }
            if (searchId != MailboxChange.NO_CHANGE) {
                helper.add("searchId", searchId);
            }
            if (contacts != MailboxChange.NO_CHANGE) {
                helper.add("contacts", contacts);
            }
            if (accessed != MailboxChange.NO_CHANGE) {
                helper.add("accessed", accessed);
            }
            if (recent != MailboxChange.NO_CHANGE) {
                helper.add("recent", recent);
            }
            helper.add("sync", sync);
            helper.add("imap", imap);
            helper.add("writeChange", writeChange);
            helper.add("recorder", recorder);
            return helper.omitNullValues().toString();
        }
    }

    // This class handles all the indexing internals for the Mailbox
    public final MailboxIndex index;
    public final MailboxLockFactory lockFactory;
    private Map<MessageCallback.Type, MessageCallback> callbacks;
    /**
     * Bug: 94985 - Only allow one large empty folder operation to run at a time
     * to reduce the danger of having too many expensive operations running
     * concurrently
     */
    private final ReentrantLock emptyFolderOpLock = new ReentrantLock();

    public static int MAX_ITEM_CACHE_SIZE;
    private static final int MAX_MSGID_CACHE = 10;

    private final int mId;
    private final MailboxData mData;
    private final ThreadLocal<MailboxChange> threadChange = new ThreadLocal<MailboxChange>();

    private FolderCache mFolderCache;
    private TagCache mTagCache;
    private FolderTagCacheReadWriteLock folderTagCacheLock = new FolderTagCacheReadWriteLock();
    private FolderTagCacheLock ftCacheReadLock = folderTagCacheLock.readLock();
    private FolderTagCacheLock ftCacheWriteLock = folderTagCacheLock.writeLock();
    private ItemCache mItemCache = null;
    private final Map<String, Integer> mConvHashes = new ConcurrentLinkedHashMap.Builder<String, Integer>()
                    .maximumWeightedCapacity(MAX_MSGID_CACHE).build();
    private final Map<String, Integer> mSentMessageIDs = new ConcurrentLinkedHashMap.Builder<String, Integer>()
                    .maximumWeightedCapacity(MAX_MSGID_CACHE).build();

    private MailboxMaintenance maintenance;
    private volatile boolean open = false;
    private boolean galSyncMailbox = false;
    private volatile boolean requiresWriteLock = true;
    private MailboxState state;
    private NotificationPubSub.Publisher publisher;
    private Set<WeakReference<TransactionListener>> transactionListeners;
    private TransactionCacheTracker cacheTracker;


    protected Mailbox(MailboxData data) {
        mId = data.id;
        mData = data;
        index = new MailboxIndex(this);
        // version init done in open()
        // index init done in open()
        lockFactory = new DistributedMailboxLockFactory(this);
        callbacks = new HashMap<>();
        callbacks.put(MessageCallback.Type.sent, new SentMessageCallback());
        callbacks.put(MessageCallback.Type.received, new ReceivedMessageCallback());
        cacheTracker = ItemCache.getFactory().getTransactionCacheTracker(this);
        transactionListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        addTransactionListener(cacheTracker);
        state = MailboxState.getFactory().getMailboxState(mData, cacheTracker);
        state.setLastChangeDate(System.currentTimeMillis());
        publisher = getNotificationPubSub().getPublisher();
        MAX_ITEM_CACHE_SIZE = LC.zimbra_mailbox_active_cache.intValue();
    }

    public void setGalSyncMailbox(boolean galSyncMailbox) {
        this.galSyncMailbox = galSyncMailbox;
        //update cache size limit
        if (galSyncMailbox) {
            MAX_ITEM_CACHE_SIZE = LC.zimbra_mailbox_galsync_cache.intValue();
        } else {
            MAX_ITEM_CACHE_SIZE = LC.zimbra_mailbox_active_cache.intValue();
        }
    }

    public boolean isGalSyncMailbox() {
        return galSyncMailbox;
    }

    boolean isOpen() {
        return open;
    }

    private MailboxChange currentChange() {
        MailboxChange change = threadChange.get();
        if (change == null) {
            change = new MailboxChange();
            threadChange.set(change);
        }
        return change;
    }

    public boolean requiresWriteLock() {
        //mailbox currently forced to use write lock due to one of the following
        //1. pending tag/flag reload; i.e. cache flush or initial mailbox load
        //2. this is an always on node (distributed lock does not support read/write currently)
        //3. read/write disabled by LC for debugging
        return requiresWriteLock || Zimbra.isAlwaysOn() || !LC.zimbra_mailbox_lock_readwrite.booleanValue();
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

        try (final MailboxLock l = getWriteLockAndLockIt()) {
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

                // PRIORITY flag
                if (!mData.version.atLeast(2, 3)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 2.3", getVersion());
                    MailboxUpgrade.upgradeTo2_3(this);
                    updateVersion(new MailboxVersion((short) 2, (short) 3));
                }

                // POST flag
                if (!mData.version.atLeast(2, 4)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 2.4", getVersion());
                    MailboxUpgrade.upgradeTo2_4(this);
                    updateVersion(new MailboxVersion((short) 2, (short) 4));
                }

                // UUID column
                if (!mData.version.atLeast(2, 5)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 2.5", getVersion());
                    MailboxUpgrade.upgradeTo2_5(this);
                    updateVersion(new MailboxVersion((short) 2, (short) 5));
                }

                // Upgrade step for 2.6 is backed out due to bug 72131

                // MUTED flag
                if (!mData.version.atLeast(2, 7)) {
                    ZimbraLog.mailbox.info("Upgrade mailbox from %s to 2.7", getVersion());
                    MailboxUpgrade.upgradeTo2_7(this);
                    updateVersion(new MailboxVersion((short) 2, (short) 7));
                }
            }

            // done!
            return open = true;
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
    @Override
    public String getAccountId() {
        return mData.accountId;
    }

    public MailboxData getData() {
        return mData; //TODO: should this return the latest state synced with Redis?
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

    /** Returns a {@link MailSender} object that can be used to send mail
     *  using the DataSource SMTP settings */
    public MailSender getDataSourceMailSender(DataSource ds, boolean isCalMessage) throws ServiceException {
        MailSender sender = new MailSender();
        sender.setTrackBadHosts(true);
        sender.setSession(ds);
        sender.setCalendarMode(isCalMessage);
        return sender;
    }

    /** Returns a {@link MailSender} object that can be used to send mail,
     *  save a copy to the Sent folder, etc. */
    public MailSender getMailSender() throws ServiceException {
        MailSender sender = new MailSender();
        sender.setTrackBadHosts(true);

        // Set the SMTP session properties on MailSender, so
        // that we don't require the caller to set them on
        // the message.
        sender.setSession(getAccount());

        return sender;
    }

    boolean hasListeners(Type type) {
        return publisher.getNumListeners(type) > 0;
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
        return currentChange().sync == null ? state.getTrackSync(): currentChange().sync;
    }

    /** Returns whether the server is keeping track of message moves
     *  for imap clients.  By default, imap tracking is off.
     *
     * @see #beginTrackingImap */
    public boolean isTrackingImap() {
        return currentChange().imap == null ? state.isTrackingImap() : currentChange().imap;
    }

    /** Returns the operation timestamp as a UNIX int with 1-second
     *  resolution.  This time is set at the start of the Mailbox
     *  transaction and should match the <tt>long</tt> returned
     *  by {@link #getOperationTimestampMillis}. */
    public int getOperationTimestamp() {
        return (int) (currentChange().timestamp / 1000L);
    }

    /** Returns the operation timestamp as a Java long with full
     *  millisecond resolution.  This time is set at the start of
     *  the Mailbox transaction and should match the <tt>int</tt>
     *  returned by {@link #getOperationTimestamp}. */
    public long getOperationTimestampMillis() {
        return currentChange().timestamp;
    }

    /** Returns the timestamp of the last committed mailbox change.
     *  Note that this time is not persisted across server restart. */
    public long getLastChangeDate() {
        return state.getLastChangeDate();
    }

    public int getItemcacheCheckpoint() {
        return state.getItemcacheCheckpoint();
    }

    public int getLastSearchId() {
        return currentChange().searchId == MailboxChange.NO_CHANGE ? state.getSearchId(): currentChange().searchId;
    }

    /**
     * Returns the change sequence number for the most recent transaction.  This will be either the change number
     * for the current transaction or, if no database changes have yet been made in this transaction, the sequence
     * number for the last committed change.
     * @see #getOperationChangeID */
    @Override
    public int getLastChangeID() {
        return currentChange().changeId == MailboxChange.NO_CHANGE ? state.getChangeId(): Math.max(state.getChangeId(),
                        currentChange().changeId);
    }

    private void setOperationChangeID(int changeFromRedo) throws ServiceException {
        if (currentChange().changeId != MailboxChange.NO_CHANGE) {
            if (currentChange().changeId == changeFromRedo) {
                return;
            }
            throw ServiceException.FAILURE("cannot specify change ID after change is in progress", null);
        }

        int lastId = getLastChangeID();
        int nextId = (changeFromRedo == ID_AUTO_INCREMENT ? lastId + 1 : changeFromRedo);

        // need to keep the current change ID regardless of whether it's a highwater mark
        currentChange().changeId = nextId;
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
        if (currentChange().changeId == MailboxChange.NO_CHANGE) {
            setOperationChangeID(ID_AUTO_INCREMENT);
        }
        return currentChange().changeId;
    }

    /** @return whether the object has changed more recently than the client knows about */
    boolean checkItemChangeID(MailItem item) throws ServiceException {
        if (item == null) {
            return true;
        }
        return checkItemChangeID(item.getModifiedSequence(), item.getSavedSequence());
    }

    public boolean checkItemChangeID(int modMetadata, int modContent) throws ServiceException {
        if (currentChange().octxt == null || currentChange().octxt.change < 0) {
            return true;
        }
        OperationContext octxt = currentChange().octxt;
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
        return currentChange().itemId == MailboxChange.NO_CHANGE ? state.getItemId() : currentChange().itemId;
    }

    // Don't make this method package-visible.  Keep it private.
    //   idFromRedo: specific ID value to use during redo execution, or ID_AUTO_INCREMENT
    private int getNextItemId(int idFromRedo) {
        int lastId = getLastItemId();
        int nextId = idFromRedo == ID_AUTO_INCREMENT ? lastId + 1 : idFromRedo;

        if (nextId > lastId) {
            MailboxChange change = currentChange();
            change.itemId = nextId;
        }
        return nextId;
    }

    public interface ItemIdGetter {
        public int get();
    }

    /** Wrapper for getNextItemId method - should only be used within a transaction */
    private class NextItemIdGetter implements ItemIdGetter {
        @Override
        public int get() {
            if (!currentChange().isActive()) {
                ZimbraLog.mailbox.info("NextItemIdGetter used when not in transaction! %s",
                        ZimbraLog.getStackTrace(10));
                return 0;
            }
            return getNextItemId(ID_AUTO_INCREMENT);
        }
    }

    private int getNextSearchId(int idFromRedo) {
        int lastId = getLastSearchId();
        int nextId = idFromRedo == ID_AUTO_INCREMENT ? lastId + 1 : idFromRedo;

        if (nextId > lastId) {
            MailboxChange change = currentChange();
            change.searchId = nextId;
        }
        return nextId;
    }

    TargetConstraint getOperationTargetConstraint() {
        return currentChange().tcon;
    }

    void setOperationTargetConstraint(TargetConstraint tcon) {
        currentChange().tcon = tcon;
    }

    public OperationContext getOperationContext() {
        return currentChange().active ? currentChange().octxt : null;
    }

    RedoableOp getRedoPlayer() {
        return currentChange().getRedoPlayer();
    }

    RedoableOp getRedoRecorder() {
        return currentChange().recorder;
    }

    PendingLocalModifications getPendingModifications() {
        return currentChange().dirty;
    }


    /** Returns the {@link Account} for the authenticated user for the
     *  transaction.  Returns <tt>null</tt> if none was supplied in the
     *  transaction's {@link OperationContext} or if the authenticated
     *  user is the same as the <tt>Mailbox</tt>'s owner. */
    Account getAuthenticatedAccount() {
        Account authuser = null;
        if (currentChange().active && currentChange().octxt != null) {
            authuser = currentChange().octxt.getAuthenticatedUser();
        }
        // XXX if the current authenticated user is the owner, it will return null.
        // later on in Folder.checkRights(), the same assumption is used to validate
        // the access.
        if (authuser != null && authuser.getId().equals(getAccountId())) {
            authuser = null;
        }
        return authuser;
    }

    Account getLockAccount() throws ServiceException {
        Account authenticatedAccount = getAuthenticatedAccount();
        if (authenticatedAccount == null) {
            authenticatedAccount = getAccount();
        }
        return authenticatedAccount;
    }

    /** Returns whether the authenticated user for the transaction is using
     *  any admin privileges they might have.  Admin users not using privileges
     *  are exactly like any other user and cannot access any folder they have
     *  not explicitly been granted access to.
     *
     * @see #getAuthenticatedAccount() */
    boolean isUsingAdminPrivileges() {
        return currentChange().active && currentChange().octxt != null
                        && currentChange().octxt.isUsingAdminPrivileges();
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
        if (currentChange().active && currentChange().octxt != null) {
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
    @Override
    public long getSize() {
        long additionalSize = getAdditionalSize();
        return (currentChange().size == MailboxChange.NO_CHANGE ? state.getSize(): currentChange().size) +
            additionalSize;
    }

    private long getAdditionalSize() {
        long additionalSize = 0L;
        try {
            for (AdditionalQuotaProvider listener : MailboxManager.getInstance().getAdditionalQuotaProviders()) {
                additionalSize += listener.getAdditionalQuota(this);
            }
        }
        catch (ServiceException e ) {
            ZimbraLog.mailbox.warn("could not get mailbox total size", e);
        }
        return additionalSize;
    }

    /** change the current size of the mailbox */
    void updateSize(long delta) throws ServiceException {
        updateSize(delta, true);
    }

    void updateSize(long delta, boolean checkQuota) throws ServiceException {
        if (delta == 0) {
            return;
        }

        // if we go negative, that's OK! just pretend we're at 0.
        long size = Math.max(0, (currentChange().size == MailboxChange.NO_CHANGE ? state.getSize(): currentChange().size)
                        + delta);
        if (delta > 0 && checkQuota) {
            checkSizeChange(size);
        }

        currentChange().dirty.recordModified(this, Change.SIZE);
        currentChange().size = size;
    }

    public void checkSizeChange(long newSize) throws ServiceException {
        Account acct = getAccount();
        long acctQuota = AccountUtil.getEffectiveQuota(acct);
        if (acctQuota != 0 && newSize + getAdditionalSize() > acctQuota) {
            throw MailServiceException.QUOTA_EXCEEDED(acctQuota);
        }
        Domain domain = Provisioning.getInstance().getDomain(acct);
        if (domain != null && AccountUtil.isOverAggregateQuota(domain)
                        && !AccountUtil.isReceiveAllowedOverAggregateQuota(domain)) {
            throw MailServiceException.DOMAIN_QUOTA_EXCEEDED(domain.getDomainAggregateQuota());
        }
    }

    /** Returns the last time that the mailbox had a write op caused by a SOAP
     *  session.  This value is written both right after the session's first
     *  write op as well as right after the session expires.
     *
     * @see #recordLastSoapAccessTime(long) */
    public long getLastSoapAccessTime() throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            long lastAccess = (currentChange().accessed == MailboxChange.NO_CHANGE ? state.getLastWriteDate()
                            : currentChange().accessed) * 1000L;
            //TODO: no way to get Soap access time on remote mailboxes
//            for (Session s : mListeners) {
//                if (s instanceof SoapSession) {
//                    lastAccess = Math.max(lastAccess, ((SoapSession) s).getLastWriteAccessTime());
//                }
//            }
            return lastAccess;
        }
    }

    /** Records the last time that the mailbox had a write op caused by a SOAP
     *  session.  This value is written both right after the session's first
     *  write op as well as right after the session expires.
     *
     * @see #getLastSoapAccessTime() */
    public void recordLastSoapAccessTime(long time) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("recordLastSoapAccessTime", null)) {
            if (time > state.getLastWriteDate()) {
                currentChange().accessed = (int) (time / 1000);
                DbMailbox.recordLastSoapAccess(this);
            }
            t.commit();
        }
    }

    /** Returns the number of "recent" messages in the mailbox.  A message is
     *  considered "recent" if (a) it's not a draft or a sent message, and
     *  (b) it was added since the last write operation associated with any
     *  SOAP session. */
    public int getRecentMessageCount() {
        return currentChange().recent == MailboxChange.NO_CHANGE ? state.getRecentMessages() : currentChange().recent;
    }

    /** Resets the mailbox's "recent message count" to 0.  A message is considered "recent" if:
     *     (a) it's not a draft or a sent message, and
     *     (b) it was added since the last write operation associated with any SOAP session. */
    public void resetRecentMessageCount(OperationContext octxt) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("resetRecentMessageCount", octxt)) {
            if (getRecentMessageCount() != 0) {
                currentChange().recent = 0;
            }
            t.commit();
        }
    }

    /** Resets the mailbox's "recent message count" to 0.  A message is considered "recent" if:
     *     (a) it's not a draft or a sent message, and
     *     (b) it was added since the last write operation associated with any SOAP session. */
    @Override
    public void resetRecentMessageCount(OpContext octxt) throws ServiceException {
        resetRecentMessageCount((OperationContext)octxt);
    }

    public void refreshMailbox(OperationContext octxt) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("refreshMailboxStats", octxt)) {
            // begin transaction would refresh the mailbox.
            t.commit();
        }
    }

    /** Returns the number of contacts currently in the mailbox.
     *
     * @see #updateContactCount(int) */
    public int getContactCount() {
        return currentChange().contacts == MailboxChange.NO_CHANGE ? state.getNumContacts() : currentChange().contacts;
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
        if (delta == 0) {
            return;
        }
        // if we go negative, that's OK! just pretend we're at 0.
        currentChange().contacts = Math.max(0, (currentChange().contacts == MailboxChange.NO_CHANGE ? state.getNumContacts()
                        : currentChange().contacts) + delta);

        if (delta < 0) {
            return;
        }
        int quota = getAccount().getContactMaxNumEntries();
        if (quota != 0 && currentChange().contacts > quota) {
            throw MailServiceException.TOO_MANY_CONTACTS(quota);
        }
    }


    /** Adds the item to the current change's list of items created during
     *  the transaction.
     * @param item  The created item. */
    void markItemCreated(MailItem item) {
        currentChange().dirty.recordCreated(item);
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
            markItemDeleted(item.getType(), item.getId(), item.getFolderId());
        } else {
            currentChange().dirty.recordDeleted(itemSnapshot);
        }
    }

    /** Adds the item to the current change's list of items deleted during the transaction.
     *
     * @param type item type
     * @param itemId  The id of the item being deleted. */
    void markItemDeleted(MailItem.Type type, int itemId, int folderId) {
        currentChange().dirty.recordDeleted(mData.accountId, itemId, folderId, type);
    }

    /** Adds the items to the current change's list of items deleted during the transaction.
     *
     * @param idlist  The ids of the items being deleted. */
    void markItemDeleted(TypedIdList idlist) {
        currentChange().dirty.recordDeleted(mData.accountId, idlist);
    }

    /** Adds the item to the current change's list of items modified during
     *  the transaction.
     *
     *
     * @param item    The modified item.
     * @param reason  The bitmask describing the modified item properties.
     * @see com.zimbra.cs.session.PendingModifications.Change */
    void markItemModified(MailItem item, int reason) throws ServiceException {
        if (item.inDumpster() && (reason != Change.METADATA)) {
            throw MailServiceException.IMMUTABLE_OBJECT(item.getId());
        }
        currentChange().dirty.recordModified(item, reason);
    }

    /** Returns whether the given item has been marked dirty for a particular
     *  reason during the current transaction.  Outside of a transaction, this
     *  method will return {@code false}.
     * @param item  The {@code MailItem} we're interested in.
     * @param how   A bitmask of {@link Change} "why" flags (e.g. {@code
     *              Change#MODIFIED_FLAGS}).
     * @see Change */
    public boolean isItemModified(MailItem item, int how) {
        PendingLocalModifications dirty = currentChange().dirty;
        if (!dirty.hasNotifications()) {
            return false;
        }
        PendingLocalModifications.ModificationKey mkey = new PendingLocalModifications.ModificationKey(item);
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
            currentChange().addPendingDelete((PendingDelete) obj);
        } else {
            currentChange().otherDirtyStuff.add(obj);
        }
    }

    public DbConnection getOperationConnection() throws ServiceException {
        if (!currentChange().isActive()) {
            throw ServiceException.FAILURE("cannot fetch Connection outside transaction", new Exception());
        }
        return currentChange().getConnection();
    }

    private void setOperationConnection(DbConnection conn) throws ServiceException {
        if (!currentChange().isActive()) {
            throw ServiceException.FAILURE("cannot set Connection outside transaction", new Exception());
        } else if (conn == null) {
            return;
        } else if (currentChange().conn != null) {
            throw ServiceException.FAILURE("cannot set Connection for in-progress transaction", new Exception());
        }
        currentChange().conn = conn;
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
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            if (maintenance != null) {
                maintenance.startInnerMaintenance();
                ZimbraLog.mailbox.info("already in maintenance, nesting access for mailboxId %d", getId());
                return maintenance;
            }
            ZimbraLog.mailbox.info("Putting mailbox %d under maintenance.", getId());
            index.evict();

            maintenance = new MailboxMaintenance(mData.accountId, mId, this);
            return maintenance;
        }
    }


    /**
     * End maintenance mode
     * @param success - whether to mark operation as success or take mailbox out of commision due to failure
     * @return true if maintenance is actually ended; false if maintenance is still wrapped by outer lockout
     * @throws ServiceException
     */
    synchronized boolean endMaintenance(boolean success) throws ServiceException {
        if (maintenance == null) {
            throw ServiceException.FAILURE("mainbox not in maintenance mode", null);
        }

        if (success) {
            ZimbraLog.mailbox.info("Ending maintenance on mailbox %d.", getId());
            if (maintenance.endInnerMaintenance()) {
                ZimbraLog.mailbox.info("decreasing depth for mailboxId %d", getId());
                return false;
            } else {
                maintenance = null;
                return true;
            }
        } else {
            ZimbraLog.mailbox.info("Ending maintenance and marking mailbox %d as unavailable.", getId());
            maintenance.markUnavailable();
            return true;
        }
    }

    boolean isTransactionActive() {
        return currentChange().depth > 0;
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

        try (final MailboxTransaction t = mailboxWriteTransaction("getConfig", octxt, null)) {
            // note: defaulting to commit (no explanation for this was given in original comment)
            t.commit();
            // make sure they have sufficient rights to view the config
            if (!hasFullAccess()) {
                return null;
            }
            Set<String> configKeys = state.getConfigKeys();
            if (configKeys == null || !configKeys.contains(section)) {
                return null;
            }
            String config = DbMailbox.getConfig(this, section);
            if (config == null) {
                return null;
            }
            try {
                return new Metadata(config);
            } catch (ServiceException e) {
                t.rollback();
                ZimbraLog.mailbox.warn("could not decode config metadata for section:" + section);
                return null;
            }
        }
    }

    public String getConfig(OperationContext octxt, Pattern pattern) throws ServiceException {

        String previousDeviceId = null;
        if (!hasFullAccess()) {
            return null;
        }
        if (state.getConfigKeys() == null)/* || !mData.configKeys.contains(section)) */{
            return null;
        } else {

            SortedSet<String> clientIds= new TreeSet<String>(state.getConfigKeys());
            for (String key : clientIds) {
                if (pattern.matcher(key).matches()) {

                    previousDeviceId = key;
                    if (previousDeviceId.indexOf(":") != -1) {
                        int index = previousDeviceId.indexOf(":");
                        previousDeviceId = previousDeviceId.substring(0, index);
                    }
                    String tmp = previousDeviceId.toLowerCase();
                    if (!tmp.contains("build")) {
                        break;
                    }

                }
            }
        }
        return previousDeviceId;
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
        try (final MailboxTransaction t = mailboxWriteTransaction("setConfig", octxt, redoPlayer)) {
            // make sure they have sufficient rights to view the config
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
            currentChange().dirty.recordModified(this, Change.CONFIG);
            currentChange().config = new Pair<String, Metadata>(section, config);
            DbMailbox.updateConfig(this, section, config);
            t.commit();
        }
    }

    /**
     * Delete all of the metadata of a device by given device id. In ActiveSync, section is composed of deviceId<:CollectionClass>
     *
     * @param octxt The context for this request
     * @param sectionPart The config section part
     * @throws ServiceException
     */
    public void deleteConfig(OperationContext octxt, String sectionPart) throws ServiceException {
        Preconditions.checkNotNull(sectionPart);

        DeleteConfig redoPlayer = new DeleteConfig(mId, sectionPart);
        try (final MailboxTransaction t = mailboxWriteTransaction("deleteConfig", octxt, redoPlayer)) {
            // make sure they have sufficient rights to view the config
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
            currentChange().dirty.recordModified(this, Change.CONFIG);
            DbMailbox.deleteConfig(this, sectionPart);
            t.commit();
        }
    }

    private ItemCache getItemCache() throws ServiceException {
        if (!currentChange().isActive()) {
            throw ServiceException.FAILURE("cannot access item cache outside a transaction active="
                            + currentChange().active + ",depth=" + currentChange().depth, null);
        }
        return currentChange().itemCache;
    }

    private void clearItemCache() {
        if (currentChange().isActive()) {
            currentChange().itemCache.clear();
        } else {
            mItemCache.clear();
        }
        try {
            if (Zimbra.isAlwaysOn()) {
                DbMailbox.incrementItemcacheCheckpoint(this);
            }
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("error while clearing item cache", e);
        }
    }

    void cache(MailItem item) throws ServiceException {
        if (item == null || item.isTagged(Flag.FlagInfo.UNCACHED)) {
            return;
        }

        if (item instanceof Tag) {
            try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                if (mTagCache != null) {
                    mTagCache.put((Tag) item);
                }
            }
        } else if (item instanceof Folder) {
            try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                if (mFolderCache != null) {
                    mFolderCache.put((Folder) item);
                }
            }
        } else {
            getItemCache().put(item);
        }

        ZimbraLog.cache.debug("cached %s %d in mailbox %d", item.getType(), item.getId(), getId());
    }

    public void batchUncache(List<MailItem> items) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("UncacheItemList", null)) {
            for(MailItem item : items) {
                uncache(item);
            }
            t.commit();
        } catch (ServiceException e) {
            ZimbraLog.cache.error("Failed to remove a batch of items from cache", e);
        }
    }

    public void uncache(MailItem item) throws ServiceException {
        if (item == null) {
            return;
        }

        if (item instanceof Tag) {
            try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                if (mTagCache == null) {
                    return;
                }
                mTagCache.remove(item.getId());
                mTagCache.remove(item.getName().toLowerCase());
            }
        } else if (item instanceof Folder) {
            try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                if (mFolderCache == null) {
                    return;
                }
                mFolderCache.remove(item.getId());
            }
        } else {
            getItemCache().remove(item);
            MessageCache.purge(item);
        }

        ZimbraLog.cache.debug("uncached %s %d in mailbox %d", item.getType(), item.getId(), getId());

        /*
         * With the item cache stored in redis, uncacheChildren is very inefficient.
         * Since the cache is unbounded, itemCache.values() requires pulling an arbitrarily high number of
         * items into memory (even if it was bounded, it would still be an inefficient operation).
         * Temporarily commenting this out shouldn't do any damage other than keeping stale hash entries in redis.
         * uncacheChildren(item);
         */
    }

    /** Removes an item from the <code>Mailbox</code>'s item cache.
     *  <i>Note: This function
     *  cannot be used to uncache {@link Tag}s and {@link Folder}s.  You must
     *  call {@link #uncache(MailItem)} to remove those items from their
     *  respective caches.</i>
     *
     * @param itemId  The id of the item to uncache */
    void uncacheItem(Integer itemId) throws ServiceException {
        MailItem item = getItemCache().remove(itemId);
        if (ZimbraLog.cache.isDebugEnabled()) {
            ZimbraLog.cache.debug("uncached item " + itemId + " in mailbox " + getId());
        }
        if (item != null) {
            MessageCache.purge(item);
        } else {
            MessageCache.purge(this, itemId);
        }
    }

    /** Removes all this item's children from the <code>Mailbox</code>'s cache.
     *  Does not uncache the item itself. */
    void uncacheChildren(MailItem parent) throws ServiceException {
        if (parent == null || !parent.canHaveChildren()) {
            return;
        }

        Collection<? extends MailItem> cached;
        if (!(parent instanceof Folder)) {
            cached = getItemCache().values();
        } else if (mFolderCache != null) {
            try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                cached = mFolderCache.values();
            }
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

    private void clearFolderCache() throws ServiceException {
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            mFolderCache = null;
            requiresWriteLock = true;
        }
        // Remove from memcached cache
        try {
            FoldersTagsCache.getInstance().purgeMailbox(this);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error deleting folders/tags cache from memcached.");
        }
    }

    private void clearTagCache() throws ServiceException {
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            mTagCache = null;
            requiresWriteLock = true;
        }
        // Remove from memcached cache
        try {
            FoldersTagsCache.getInstance().purgeMailbox(this);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("error deleting folders/tags cache from memcached.");
        }
    }

    /** Removes all items of a specified type from the <tt>Mailbox</tt>'s
     *  caches.  There may be some collateral damage: purging non-tag,
     *  non-folder types will drop the entire item cache.
     *
     * @param type  The type of item to completely uncache.  {@link MailItem#TYPE_UNKNOWN}
     * uncaches all items. */
    public void purge(MailItem.Type type) throws ServiceException {
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            switch (type) {
                case FOLDER:
                case MOUNTPOINT:
                case SEARCHFOLDER:
                    clearFolderCache();
                    break;
                case FLAG:
                case TAG:
                case SMARTFOLDER:
                    clearTagCache();
                    break;
                default:
                    clearItemCache();
                    break;
                case UNKNOWN:
                    clearFolderCache();
                    clearTagCache();
                    clearItemCache();
                    break;
            }
        }

        ZimbraLog.cache.debug("purged type=%s", type);
    }

    public static final Set<Integer> REIFIED_FLAGS = ImmutableSet.of(Flag.ID_FROM_ME, Flag.ID_ATTACHED,
                    Flag.ID_REPLIED, Flag.ID_FORWARDED, Flag.ID_COPIED, Flag.ID_FLAGGED, Flag.ID_DRAFT,
                    Flag.ID_DELETED, Flag.ID_NOTIFIED, Flag.ID_UNREAD, Flag.ID_HIGH_PRIORITY, Flag.ID_LOW_PRIORITY,
                    Flag.ID_VERSIONED, Flag.ID_POPPED, Flag.ID_NOTE, Flag.ID_PRIORITY, Flag.ID_INVITE, Flag.ID_POST,
                    Flag.ID_MUTED);

    /** Creates the default set of immutable system folders in a new mailbox.
     *  These system folders have fixed ids (e.g. {@link #ID_FOLDER_INBOX})
     *  and are hardcoded in the server:<pre>
     *     MAILBOX_ROOT
     *       +--Tags
     *       +--Conversations
     *       +--Comments
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
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            createDefaultFolders();
            createDefaultFlags();

            currentChange().itemId = getInitialItemId();
            currentChange().searchId = 1;
            DbMailbox.updateMailboxStats(this);
        }
    }

    /**
     * @see #initialize() */
    protected void createDefaultFolders() throws ServiceException {
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_DONT_TRACK_COUNTS;
            Folder root = Folder.create(ID_FOLDER_ROOT, UUIDUtil.generateUUID(), this, null, "ROOT", hidden,
                            MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_TAGS, UUIDUtil.generateUUID(), this, root, "Tags", hidden, MailItem.Type.TAG, 0,
                            MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_CONVERSATIONS, UUIDUtil.generateUUID(), this, root, "Conversations", hidden,
                            MailItem.Type.CONVERSATION, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_COMMENTS, UUIDUtil.generateUUID(), this, root, "Comments", hidden,
                            MailItem.Type.COMMENT, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);

            byte system = Folder.FOLDER_IS_IMMUTABLE;
            Folder userRoot = Folder.create(ID_FOLDER_USER_ROOT, UUIDUtil.generateUUID(), this, root, "USER_ROOT",
                            system, MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_INBOX, UUIDUtil.generateUUID(), this, userRoot, "Inbox", system,
                            MailItem.Type.MESSAGE, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_TRASH, UUIDUtil.generateUUID(), this, userRoot, "Trash", system,
                            MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_SPAM, UUIDUtil.generateUUID(), this, userRoot, "Junk", system,
                            MailItem.Type.MESSAGE, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_SENT, UUIDUtil.generateUUID(), this, userRoot, "Sent", system,
                            MailItem.Type.MESSAGE, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_DRAFTS, UUIDUtil.generateUUID(), this, userRoot, "Drafts", system,
                            MailItem.Type.MESSAGE, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_CONTACTS, UUIDUtil.generateUUID(), this, userRoot, "Contacts", system,
                            MailItem.Type.CONTACT, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_CALENDAR, UUIDUtil.generateUUID(), this, userRoot, "Calendar", system,
                            MailItem.Type.APPOINTMENT, Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR_RGB, null, null,
                            null);
            Folder.create(ID_FOLDER_TASKS, UUIDUtil.generateUUID(), this, userRoot, "Tasks", system,
                            MailItem.Type.TASK, Flag.BITMASK_CHECKED, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_AUTO_CONTACTS, UUIDUtil.generateUUID(), this, userRoot, "Emailed Contacts", system,
                            MailItem.Type.CONTACT, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_IM_LOGS, UUIDUtil.generateUUID(), this, userRoot, "Chats", system,
                            MailItem.Type.MESSAGE, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
            Folder.create(ID_FOLDER_BRIEFCASE, UUIDUtil.generateUUID(), this, userRoot, "Briefcase", system,
                            MailItem.Type.DOCUMENT, 0, MailItem.DEFAULT_COLOR_RGB, null, null, null);
        }
    }

    void createDefaultFlags() throws ServiceException {
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            // for flags that we want to be searchable, put an entry in the TAG table
            for (int tagId : REIFIED_FLAGS) {
                DbTag.createTag(this, Flag.of(this, tagId).getUnderlyingData(), null, false);
            }
        }
    }

    int getInitialItemId() {
        return FIRST_USER_ID;
    }

    private void loadFoldersAndTags() throws ServiceException {
        // if the persisted mailbox sizes aren't available, we *must* recalculate
        boolean initial = state.getNumContacts() < 0 || state.getSize() < 0;
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            if (mFolderCache != null && mTagCache != null && !initial) {
                return;
            }
        }
        if (ZimbraLog.cache.isDebugEnabled()) {
            ZimbraLog.cache.debug("loading due to initial? %s folders? %s tags? %s writeChange? %s", initial, mFolderCache == null, mTagCache == null, currentChange().writeChange);
        }
        assert(currentChange().writeChange);
        assert(isWriteLockedByCurrentThread());

        ZimbraLog.cache.info("initializing folder and tag caches for mailbox %d", getId());
        try {
            DbMailItem.FolderTagMap folderData = new DbMailItem.FolderTagMap();
            DbMailItem.FolderTagMap tagData = new DbMailItem.FolderTagMap();
            MailboxData stats = null;

            boolean loadedFromCache = false;
            CachedTagsAndFolders cachedTagsAndFolders = mItemCache.getTagsAndFolders();

            if (cachedTagsAndFolders != null) {
                loadedFromCache = true;
                List<Metadata> cachedFolderMeta = cachedTagsAndFolders.getFolders();
                if (cachedFolderMeta != null) {
                    for (Metadata meta : cachedFolderMeta) {
                        MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                        ud.deserialize(meta);
                        folderData.put(ud, null);
                    }
                }
                List<Metadata> cachedTagMeta = cachedTagsAndFolders.getTags();
                if (cachedTagMeta != null) {
                    for (Metadata meta : cachedTagMeta) {
                        MailItem.UnderlyingData ud = new MailItem.UnderlyingData();
                        ud.deserialize(meta);
                        tagData.put(ud, null);
                    }
                }
            }

            if (!loadedFromCache) {
                stats = DbMailItem.getFoldersAndTags(this, folderData, tagData, initial);
            }

            boolean persist = stats != null;
            if (stats != null) {
                if (state.getSize() != stats.size) {
                    currentChange().dirty.recordModified(this, Change.SIZE);
                    ZimbraLog.mailbox.debug("setting mailbox size to %d (was %d) for mailbox %d", stats.size,
                                    state.getSize(), mId);
                    state.setSize(stats.size);
                }
                if (state.getNumContacts() != stats.contacts) {
                    ZimbraLog.mailbox.debug("setting contact count to %d (was %d) for mailbox %d", stats.contacts,
                            state.getNumContacts(), mId);
                    state.setNumContacts(stats.contacts);
                }
                DbMailbox.updateMailboxStats(this);
            }

            initFolderTagFields(folderData, tagData, initial, persist, loadedFromCache);

            if (requiresWriteLock) {
                requiresWriteLock = false;
                ZimbraLog.mailbox.debug("consuming forceWriteMode");
                //we've reloaded folder/tags so new callers can go back to using read
            }
        } catch (ServiceException e) {
        	ZimbraLog.mailbox.warn("Unexpected exception whilst loading folders and tags - setting mTagCache/mFolderCache as null", e);
        	try(FolderTagCacheLock lock = ftCacheWriteLock.lock()) {
        	    mTagCache = null;
        	    mFolderCache = null;
        	    throw e;
        	}
        }
    }

    private void initFolderTagFields(DbMailItem.FolderTagMap folderData, DbMailItem.FolderTagMap tagData,
    		boolean initial, boolean persist, boolean loadedFromCache) throws ServiceException {
    	/* Use temporary variables for these until we have a write lock to avoid another thread's updates
    	 * to the corresponding fields causing inconsistencies. */
    	FolderCache folderCache = ItemCache.getFactory().getFolderCache(this, cacheTracker);
    	TagCache tagCache = ItemCache.getFactory().getTagCache(this, cacheTracker);
    	try(FolderTagCacheLock lock = ftCacheWriteLock.lock()) {
    		mFolderCache = folderCache;
    		mTagCache = tagCache;
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
    				markItemModified(folder, Change.INTERNAL_ONLY);
    				folder.metadataChanged();
    			}
    			// if we recalculated folder counts or had to fix CHANGE_DATE, persist those values now
    			if (persist || badChangeDate) {
    				folder.saveFolderCounts(initial);
    			}
    		}

    		// create the tag objects and, as a side-effect, populate the new
    		// cache
    		for (Map.Entry<MailItem.UnderlyingData, DbMailItem.FolderTagCounts> entry : tagData.entrySet()) {
    			Tag tag = new Tag(this, entry.getKey());
    			if (persist) {
    				tag.saveTagCounts();
    			}
    		}

    		if (!loadedFromCache && !DebugConfig.disableFoldersTagsCache) {
    			cacheFoldersTagsWhenHaveWriteLock();
    		}
    	}
    }

    private void cacheFoldersTagsWhenHaveWriteLock() throws ServiceException {
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
        	List<Folder> folderList = new ArrayList<>(mFolderCache.values());
        	List<Tag> tagList = new ArrayList<>(mTagCache.values());
        	mItemCache.cacheTagsAndFolders(folderList, tagList);
        }
    }

    private void cacheFoldersTags() throws ServiceException {
    	try (final MailboxLock l = getWriteLockAndLockIt()) {
    		cacheFoldersTagsWhenHaveWriteLock();
    	}
    }

    public void recalculateFolderAndTagCounts() throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("recalculateFolderAndTagCounts", null)) {
            // force the recalculation of all folder/tag/mailbox counts and sizes
            clearFolderCache();
            clearTagCache();
            state.setNumContacts(-1);
            loadFoldersAndTags();
            t.commit();
        }
    }

    public void deleteMailbox() throws ServiceException {
        deleteMailbox(DeleteBlobs.ALWAYS);
    }

    public enum DeleteBlobs {
        ALWAYS, NEVER, UNLESS_CENTRALIZED
    };

    private static class MessageCachePurgeCallback implements DbMailItem.Callback<String> {
        @Override
        public void call(String value) {
            MessageCache.purge(value);
        }
    }

    public void deleteMailbox(DeleteBlobs deleteBlobs) throws ServiceException {
        StoreManager sm = StoreManager.getInstance();
        boolean deleteStore = deleteBlobs == DeleteBlobs.ALWAYS
                        || (deleteBlobs == DeleteBlobs.UNLESS_CENTRALIZED && !sm.supports(StoreFeature.CENTRALIZED));
        SpoolingCache<MailboxBlob.MailboxBlobInfo> blobs = null;

        try (final MailboxLock l = getWriteLockAndLockIt()) {
            // first, throw the mailbox into maintenance mode
            //   (so anyone else with a cached reference to the Mailbox can't use it)
            MailboxMaintenance maint = null;
            try {
                maint = MailboxManager.getInstance().beginMaintenance(getAccountId(), mId);
            } catch (MailServiceException e) {
                // Ignore wrong mailbox exception.  It may be thrown if we're redoing a DeleteMailbox that was interrupted
                // when server crashed in the middle of the operation.  Database says the mailbox has been deleted, but
                // there may be other files that still need to be cleaned up.
                if (!MailServiceException.WRONG_MAILBOX.equals(e.getCode())) {
                    throw e;
                }
            }

            DeleteMailbox redoRecorder = new DeleteMailbox(mId);
            boolean needRedo = needRedo(null, redoRecorder);
            boolean success = false;
            try {
                try (final MailboxTransaction t = mailboxWriteTransaction("deleteMailbox", null, redoRecorder)) {
                    if (needRedo) {
                        redoRecorder.log();
                    }

                    if (deleteStore && !sm.supports(StoreManager.StoreFeature.BULK_DELETE)) {
                        blobs = DbMailItem.getAllBlobs(this);
                    }

                    // Remove the mime messages from MessageCache
                    if (deleteStore) {
                        DbMailItem.visitAllBlobDigests(this, new MessageCachePurgeCallback());
                    }
                    // remove all the relevant entries from the database
                    DbConnection conn = getOperationConnection();
                    DbMailbox.clearMailboxContent(this);
                    DbMailbox.deleteMailbox(conn, this);
                    DbVolumeBlobs.deleteBlobRef(conn, this);
                    // Remove all data related to this mailbox from memcached, so the data doesn't
                    // get used by another user later by mistake if/when mailbox id gets reused.
                    MemcachedCacheManager.purgeMailbox(this);

                    success = true;
                    t.commit();
                }

                if (success) {
                    // remove all traces of the mailbox from the Mailbox cache
                    //   (so anyone asking for the Mailbox gets NO_SUCH_MBOX or creates a fresh new empty one with a different id)
                    MailboxManager.getInstance().markMailboxDeleted(this);

                    // attempt to nuke the store and index
                    try {
                        index.deleteIndex();
                    } catch (IOException iox) {
                        ZimbraLog.store.warn("Unable to delete index data", iox);
                    }

                    if (deleteStore) {
                        try {
                            sm.deleteStore(this, blobs);
                        } catch (IOException iox) {
                            ZimbraLog.store.warn("Unable to delete message data", iox);
                        }
                    }
                    try {
                        EventStore.getFactory().getEventStore(getAccountId()).deleteEvents();
                    } catch (ServiceException e) {
                        ZimbraLog.event.warn("Unable to delete event data for account %s", getAccountId(), e);
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

                if (maint != null) {
                    if (success) {
                        // twiddle the mailbox lock [must be the last command of this function!]
                        //   (so even *we* can't access this Mailbox going forward)
                        maint.markUnavailable();
                    } else {
                        // end the maintenance if the delete is not successful.
                        MailboxManager.getInstance().endMaintenance(maint, success, true);
                    }
                }

                if (blobs != null) {
                    blobs.cleanup();
                }
            }
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
        try (final MailboxTransaction t = mailboxWriteTransaction("renameMailbox", octxt, redoRecorder)) {
            DbMailbox.renameMailbox(this, newName);
            t.commit();
        }
    }

    public MailboxVersion getVersion() throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            return mData.version;
        }
    }

    public void resetMailboxForRestore() throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("resetMailboxForRestore", null)) {
            for (int tagId : REIFIED_FLAGS) {
                DbTag.deleteTagRow(this, tagId);
            }
            DbMailbox.updateVersion(this, null);
            t.commit();
        }
    }

    void updateVersion(MailboxVersion vers) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("updateVersion", null)) {
            // remove the deprecated ZIMBRA.MAILBOX_METADATA version value, if present
            Set<String> configKeys = state.getConfigKeys();
            if (configKeys != null && configKeys.contains(MailboxVersion.MD_CONFIG_VERSION)) {
                currentChange().dirty.recordModified(this, Change.CONFIG);
                currentChange().config = new Pair<String, Metadata>(MailboxVersion.MD_CONFIG_VERSION, null);
                DbMailbox.updateConfig(this, MailboxVersion.MD_CONFIG_VERSION, null);
            }

            // write the new version to ZIMBRA.MAILBOX
            MailboxVersion version = new MailboxVersion(vers);
            DbMailbox.updateVersion(this, version);
            mData.version = version;
            t.commit();
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
        try (final MailboxTransaction t = mailboxWriteTransaction("reanalyze", null)) {
            MailItem item;
            try {
                item = getItemById(id, type, false);
            } catch (NoSuchItemException e) { // fallback to dumpster
                item = getItemById(id, type, true);
            }
            item.reanalyze(data, size);
            t.commit();
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
        try (final MailboxTransaction t = mailboxReadTransaction("getEffectivePermissions", octxt)) {
            // fetch the item without perm check so we get it even if the
            // authenticated user doesn't have read permissions on it
            MailItem item = getItemById(itemId, type);

            // use ~0 to query *all* rights; may need to change this when we do negative rights
            short rights = item.checkRights((short) ~0, getAuthenticatedAccount(), isUsingAdminPrivileges());
            t.commit();
            return rights;
        }
    }

    /**
     * This API uses the credentials in authedAcct/asAdmin parameters.
     *
     * @see #getEffectivePermissions(OperationContext, int, com.zimbra.cs.mailbox.MailItem.Type)
     */
    public short getEffectivePermissions(Account authedAcct, boolean asAdmin, int itemId, MailItem.Type type)
                    throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getEffectivePermissions", new OperationContext(authedAcct, asAdmin))) {
            // fetch the item without perm check so we get it even if the
            // authenticated user doesn't have read permissions on it
            MailItem item = getItemById(itemId, type);

            // use ~0 to query *all* rights; may need to change this when we do negative rights
            short rights = item.checkRights((short) ~0, authedAcct, asAdmin);
            t.commit();
            return rights;
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
            case SMARTFOLDER:
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
        if (item == null || item.isTagged(Flag.FlagInfo.UNCACHED) || currentChange().depth > 1) {
            return item;
        }
        if (item instanceof Folder) {
            try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                return mFolderCache == null ? item : (T) snapshotFolders().get(item.getId());
            }
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
    private FolderCache snapshotFolders() throws ServiceException {
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            if (currentChange().depth > 1 || mFolderCache == null) {
                return mFolderCache;
            } else {
                return copyFolderCache();
            }
        }
    }

    private static Set<MailItem.Type> FOLDER_TYPES = EnumSet.of(MailItem.Type.FOLDER, MailItem.Type.SEARCHFOLDER,
                    MailItem.Type.MOUNTPOINT);

    /** Makes a deep copy of the {@code PendingLocalModifications} object with
     *  {@link Flag#BITMASK_UNCACHED} set on each {@code MailItem} present in
     *  the {@code created} and {@code modified} hashes.  These copied {@code
     *  MailItem}s are not linked to their {@code Mailbox} and thus will not
     *  change when modifications are subsequently made to the contents of the
     *  {@code Mailbox}.  The original {@code PendingLocalModifications} object and
     *  the {@code MailItem}s it references are unchanged.
     *  <p>
     *  This method should only be called <i>immediately</i> before notifying
     *  listeners of the changes from the currently-ending transaction. */
    private PendingLocalModifications snapshotModifications(PendingLocalModifications pms) throws ServiceException {
        if (pms == null) {
            return null;
        }
        assert (currentChange().depth == 0);

        FolderCache folders;
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            folders = mFolderCache == null || Collections.disjoint(pms.changedTypes, FOLDER_TYPES) ? mFolderCache
                    : snapshotFolders();
        }

        PendingLocalModifications snapshot = new PendingLocalModifications();

        if (pms.deleted != null && !pms.deleted.isEmpty()) {
            snapshot.recordDeleted(pms.deleted);
        }

        if (pms.created != null && !pms.created.isEmpty()) {
            for (BaseItemInfo item : pms.created.values()) {
                if (item instanceof Folder && folders != null) {
                    Folder folder = (Folder) item;
                    Folder snapshotted = folders.get(folder.getId());
                    if (snapshotted == null) {
                        ZimbraLog.mailbox.warn("folder missing from snapshotted folder set: %d", folder.getId());
                        snapshotted = folder;
                    }
                    snapshot.recordCreated(snapshotted);
                } else if (item instanceof Tag) {
                    Tag tag = (Tag) item;
                    if (tag.isImapVisible() || isListedTagOrSmartFolder(tag)) {
                        snapshot.recordCreated(snapshotItem(tag));
                    }
                } else if (item instanceof MailItem){
                    MailItem mi = (MailItem) item;
                    // NOTE: if the folder cache is null, folders fall down here and should always get copy == false
                    mi = snapshotItem(mi);
                    snapshot.recordCreated(mi);
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
                    snapshot.recordModified(folder, chg.why, (MailItem) chg.preModifyObj);
                } else if (item instanceof Tag) {
                    if (isListedTagOrSmartFolder(item)) {
                        snapshot.recordModified(snapshotItem(item), chg.why, (MailItem) chg.preModifyObj);
                    }
                } else {
                    // NOTE: if the folder cache is null, folders fall down here and should always get copy == false
                    item = snapshotItem(item);
                    snapshot.recordModified(item, chg.why, (MailItem) chg.preModifyObj);
                }
            }
        }

        return snapshot;
    }

    /**
     * @return the item with the specified ID.
     * @throws NoSuchItemException if the item does not exist
     */
    @Override
    public ZimbraMailItem getItemById(OpContext octxt, ItemIdentifier id, MailItemType type) throws ServiceException {
        return getItemById(OperationContext.asOperationContext(octxt), id.id, MailItem.Type.fromCommon(type), false);
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
        try (final MailboxTransaction t = mailboxReadTransaction("getItemById", octxt)) {
            // tag/folder caches are populated in new MailboxTransaction...
            MailItem item = checkAccess(getItemById(id, type, fromDumpster));
            t.commit();
            return item;
        }
    }

    MailItem getItemById(int id, MailItem.Type type) throws ServiceException {
        return getItemById(id, type, false);
    }

    // Returns true if the item in dumpster is visible to the user.
    // Item is hidden from non-admin user if it is too old or is a spam.
    private boolean isVisibleInDumpster(MailItem item) throws ServiceException {
        OperationContext octxt = getOperationContext();
        if (!hasFullAdminAccess(octxt)) {
            long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
            long threshold = now - getAccount().getDumpsterUserVisibleAge();
            if (item.getChangeDate() < threshold || item.inSpam()) {
                return false;
            }
        }
        return true;
    }

    MailItem getItemById(int id, MailItem.Type type, boolean fromDumpster) throws ServiceException {
        if (fromDumpster) {
            MailItem item = null;
            try {
                item = MailItem.getById(this, id, type, true);
                if (item != null && !isVisibleInDumpster(item)) {
                    item = null;
                }
            } catch (NoSuchItemException e) {
            }
            // Both truly non-existent case and age-filtered case throw
            // noSuchItem exception here. This way the client
            // can't distinguish the two cases from the stack trace in the soap
            // fault.
            if (item == null) {
                throw MailItem.noSuchItem(id, type);
            }
            return item;
        }

        // try the cache first
        MailItem item = getCachedItem(Integer.valueOf(id), type);
        if (item != null) {
            return item;
        }

        // the tag and folder caches contain ALL tags and folders, so cache miss == doesn't exist
        if (isCachedType(type)) {
            throw MailItem.noSuchItem(id, type);
        }
        boolean virtualConv = false;
        boolean cachedMsg = true;
        boolean sameId = true;
        if (id <= -FIRST_USER_ID) {
            virtualConv = true;
            ZimbraLog.mailbox.debug("getting virtual conversation");
            // special-case virtual conversations
            if (type != MailItem.Type.CONVERSATION && type != MailItem.Type.UNKNOWN) {
                throw MailItem.noSuchItem(id, type);
            }
            Message msg = getCachedMessage(Integer.valueOf(-id));
            if (msg == null) {
                ZimbraLog.mailbox.debug("message not cached");
                cachedMsg = false;
                msg = getMessageById(-id);
            }
            int convId = msg.getConversationId();
            if (msg.getConversationId() != id) {
                ZimbraLog.mailbox.debug("message(%d) conv id(%d) != id(%d), getting parent", msg.getId(), msg.getConversationId(), id);
                sameId = false;
                item = msg.getParent();
                if (item == null) {
                    ZimbraLog.mailbox.warn("got null parent for message id [%d] conv id [%d] (before condition [%d]) != id [%d]. equality? [%s:%s] in dumpster? [%s]", msg.getId(), msg.getConversationId(), convId, id, convId == id, msg.getConversationId() == id, msg.inDumpster());
                }
            } else {
                ZimbraLog.mailbox.debug("returning normal virtual conv");
                item = new VirtualConversation(this, msg);
            }
        } else {
            // cache miss, so fetch from the database
            item = MailItem.getById(this, id, type);
        }
        if (item == null) {
            ZimbraLog.mailbox.warn("item is null for id [%d] in mailbox [%d]. Virtual conv? [%s] cachedMsg? [%s] sameId? [%s]", id, this.mId, virtualConv, cachedMsg, sameId);
            throw MailItem.noSuchItem(id, type);
        }
        return item;
    }

    /**
     * Returns the <tt>MailItem</tt> with the specified UUID.
     * @throws NoSuchItemException if the item does not exist
     */
    public MailItem getItemByUuid(OperationContext octxt, String uuid, MailItem.Type type) throws ServiceException {
        return getItemByUuid(octxt, uuid, type, false);
    }

    public MailItem getItemByUuid(OperationContext octxt, String uuid, MailItem.Type type, boolean fromDumpster)
            throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getItemByUuid", octxt)) {
            // tag/folder caches are populated in new MailboxTransaction...
            MailItem item = checkAccess(getItemByUuid(uuid, type, fromDumpster));
            t.commit();
            return item;
        }
    }

    MailItem getItemByUuid(String uuid, MailItem.Type type) throws ServiceException {
        return getItemByUuid(uuid, type, false);
    }

    MailItem getItemByUuid(String uuid, MailItem.Type type, boolean fromDumpster) throws ServiceException {
        if (fromDumpster) {
            MailItem item = null;
            try {
                item = MailItem.getByUuid(this, uuid, type, true);
                if (item != null && !isVisibleInDumpster(item)) {
                    item = null;
                }
            } catch (NoSuchItemException e) {
            }
            // Both truly non-existent case and age-filtered case throw
            // noSuchItem exception here. This way the client
            // can't distinguish the two cases from the stack trace in the soap
            // fault.
            if (item == null) {
                throw MailItem.noSuchItemUuid(uuid, type);
            }
            return item;
        }

        // try the cache first
        MailItem item = getCachedItemByUuid(uuid, type);
        if (item != null) {
            return item;
        }

        // the tag and folder caches contain ALL tags and folders, so cache miss == doesn't exist
        if (isCachedType(type)) {
            throw MailItem.noSuchItemUuid(uuid, type);
        }

        // cache miss, so fetch from the database
        item = MailItem.getByUuid(this, uuid, type);

        return item;
    }

    /**
     * @returns MailItems with the specified ids.
     * @throws NoSuchItemException if any item does not exist
     */
    @Override
    public List<ZimbraMailItem> getItemsById(OpContext octxt, Collection<ItemIdentifier> ids) throws ServiceException {
        List<Integer> idInts = Lists.newArrayListWithCapacity(ids.size());
        for (ItemIdentifier iid : ids) {
            idInts.add(iid.id);
        }
        MailItem[] mitms = getItemById(OperationContext.asOperationContext(octxt), idInts, MailItem.Type.UNKNOWN);
        if (null == mitms || (mitms.length == 0)) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(mitms);
    }

    /**
     * @return <tt>MailItem</tt>s with the specified ids.
     * @throws NoSuchItemException if any item does not exist
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
        try (final MailboxTransaction t = mailboxReadTransaction("getItemById[]", octxt)
             /* tag/folder caches are populated in beginTransaction... */) {
            MailItem[] items = getItemById(ids, type, fromDumpster);
            // make sure all those items are visible...
            for (int i = 0; i < items.length; i++) {
                checkAccess(items[i]);
            }
            t.commit();
            return items;
        }
    }

    MailItem[] getItemById(Collection<Integer> ids, MailItem.Type type) throws ServiceException {
        return getItemById(ArrayUtil.toIntArray(ids), type);
    }

    MailItem[] getItemById(int[] ids, MailItem.Type type) throws ServiceException {
        return getItemById(ids, type, false);
    }

    private MailItem[] getItemById(int[] ids, MailItem.Type type, boolean fromDumpster) throws ServiceException {
        if (!currentChange().active) {
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
                        if (msg.getConversationId() == ids[i]) {
                            item = new VirtualConversation(this, msg);
                        } else {
                            item = getCachedConversation(key = msg.getConversationId());
                        }
                    } else {
                        // need to fetch the message in order to get its conv...
                        key = -ids[i];
                        relaxType = true;
                    }
                }
                items[i] = item;
                if (item == null) {
                    uncached.add(miss = key);
                }
            }
        }
        if (uncached.isEmpty()) {
            return items;
        }

        // the tag and folder caches contain ALL tags and folders, so cache miss == doesn't exist
        if (isCachedType(type)) {
            throw MailItem.noSuchItem(miss.intValue(), type);
        }

        // cache miss, so fetch from the database
        List<MailItem> itemsFromDb = MailItem.getById(this, uncached, relaxType ? MailItem.Type.UNKNOWN : type);
        HashMap<Integer, MailItem> tempCache = new HashMap<Integer, MailItem>();
        for (MailItem item : itemsFromDb) {
            tempCache.put(item.getId(), item);
        }
        uncached.clear();
        Map<Integer, Integer> virtualConvsToRealConvs = new HashMap<Integer, Integer>();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != ID_AUTO_INCREMENT && items[i] == null) {
                if (ids[i] <= -FIRST_USER_ID) {
                    // special-case virtual conversations
                    MailItem item = tempCache.get(-ids[i]);
                    if (!(item instanceof Message)) {
                        throw MailItem.noSuchItem(ids[i], type);
                    } else if (item.getParentId() == ids[i]) {
                        items[i] = new VirtualConversation(this, (Message) item);
                    } else {
                        items[i] = tempCache.get(item.getParentId());
                        if (items[i] == null) {
                            virtualConvsToRealConvs.put(ids[i],item.getParentId());
                        }
                    }
                } else {
                    if ((items[i] = tempCache.get(ids[i])) == null) {
                        throw MailItem.noSuchItem(ids[i], type);
                    }
                }
            }
        }

        // special case asking for VirtualConversation but having it be a real Conversation
        if (!virtualConvsToRealConvs.isEmpty()) {
            itemsFromDb = MailItem.getById(this, virtualConvsToRealConvs.values(), MailItem.Type.CONVERSATION);
            for (MailItem item : itemsFromDb) {
                tempCache.put(item.getId(), item);
            }
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] <= -FIRST_USER_ID && items[i] == null) {
                    items[i] = tempCache.get(virtualConvsToRealConvs.get(ids[i]));
                    if (items[i] == null) {
                        throw MailItem.noSuchItem(ids[i], type);
                    }
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
        //check the item cache first, since folder/tag caches require a network operation
        if (item == null) {
            item = getItemCache().get(key);
        }
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            if (item == null && mTagCache != null) {
                item = mTagCache.get(key);
            }
            if (item == null && mFolderCache != null) {
                item = mFolderCache.get(key);
            }
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
            case SMARTFOLDER:
                try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                    if (key < 0) {
                        item = Flag.of(this, key);
                    } else if (mTagCache != null) {
                        item = mTagCache.get(key);
                    }
                }
                break;
            case MOUNTPOINT:
            case SEARCHFOLDER:
            case FOLDER:
                try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                    if (mFolderCache != null) {
                        item = mFolderCache.get(key);
                    }
                }
                break;
            default:
                item = getItemCache().get(key);
                break;
        }

        if (item != null && !MailItem.isAcceptableType(type, MailItem.Type.of(item.type))) {
            item = null;
        }

        logCacheActivity(key, type, item);
        return item;
    }

    /** retrieve an item from the Mailbox's caches; return null if no item found */
    MailItem getCachedItemByUuid(String uuid) throws ServiceException {
        MailItem item = null;
        item = getItemCache().get(uuid);
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            if (item == null && mFolderCache != null) {
                item = mFolderCache.get(uuid);
            }
        }
        logCacheActivity(uuid, item == null ? MailItem.Type.UNKNOWN : item.getType(), item);
        return item;
    }

    MailItem getCachedItemByUuid(String uuid, MailItem.Type type) throws ServiceException {
        MailItem item = null;
        switch (type) {
            case UNKNOWN:
                return getCachedItemByUuid(uuid);
            case MOUNTPOINT:
            case SEARCHFOLDER:
            case FOLDER:
                try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                    if (mFolderCache != null) {
                        item = mFolderCache.get(uuid);
                    }
                }
                break;
            default:
                item = getItemCache().get(uuid);
                break;
        }

        if (item != null && !MailItem.isAcceptableType(type, MailItem.Type.of(item.type))) {
            item = null;
        }

        logCacheActivity(uuid, type, item);
        return item;
    }

    /** translate from the DB representation of an item to its Mailbox abstraction */
    MailItem getItem(MailItem.UnderlyingData data) throws ServiceException {
        if (data == null) {
            return null;
        }
        MailItem item = getCachedItem(data.id, MailItem.Type.of(data.type));
        // XXX: should we sanity-check the cached version to make sure all the data matches?
        if (item != null) {
            return item;
        }
        return MailItem.constructItem(this, data);
    }

    public MailItem getItemRevision(OperationContext octxt, int id, MailItem.Type type, int version)
            throws ServiceException {
        return getItemRevision(octxt, id, type, version, false);
    }

    /** Returns a current or past revision of an item.  Item version numbers
     *  are 1-based and incremented each time the "content" of the item changes
     *  (e.g. editing a draft, modifying a contact's fields).  If the requested
     *  revision does not exist, either because the version number is out of
     *  range or because the requested revision has not been retained, returns
     *  <tt>null</tt>. */
    public MailItem getItemRevision(OperationContext octxt, int id, MailItem.Type type, int version, boolean fromDumpster)
            throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getItemRevision", octxt)) {
            MailItem revision = checkAccess(getItemById(id, type, fromDumpster)).getRevision(version);
            t.commit();
            return revision;
        }
    }

    /** Returns a {@link List} containing all available revisions of an item,
     *  both current and past.  These revisions are returned in increasing
     *  order of their 1-based "version", with the current revision always
     *  present and listed last. */
    @SuppressWarnings("unchecked")
    public <T extends MailItem> List<T> getAllRevisions(OperationContext octxt, int id, MailItem.Type type)
            throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getAllRevisions", octxt)) {
            T item = (T) checkAccess(getItemById(id, type));
            List<MailItem> previousRevisions = item.loadRevisions();
            List<T> result = new ArrayList<T>(previousRevisions.size());
            for (MailItem rev : previousRevisions) {
                result.add((T) rev);
            }
            result.add(item);

            t.commit();
            return result;
        }
    }

    /**
     * Fetches a <tt>MailItem</tt> by its IMAP id.
     * @throws MailServiceException if there is no <tt>MailItem</tt> with the given id.
     * @see MailServiceException#NO_SUCH_ITEM
     */
    public MailItem getItemByImapId(OperationContext octxt, int imapId, int folderId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getItemByImapId", octxt)
             /* tag/folder caches are populated in beginTransaction... */) {
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
            t.commit();
            return item;
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
        try (final MailboxTransaction t = mailboxWriteTransaction("getItemByPath", octxt)
             /* tag/folder caches are populated in beginTransaction... */) {

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
                item = getTagByName(octxt, name);
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
            t.commit();
            return item;
        }
    }

    /** Returns all the MailItems of a given type, optionally in a specified folder */
    public List<MailItem> getItemList(OperationContext octxt, MailItem.Type type) throws ServiceException {
        return getItemList(octxt, type, -1);
    }

    public List<MailItem> getItemList(OperationContext octxt, MailItem.Type type, int folderId) throws ServiceException {
        return getItemList(octxt, type, folderId, SortBy.NONE);
    }

    public List<MailItem> getItemList(OperationContext octxt, MailItem.Type type, int folderId, SortBy sort)
                    throws ServiceException {
        List<MailItem> result;

        if (type == MailItem.Type.UNKNOWN) {
            return Collections.emptyList();
        }
        try (final MailboxTransaction t = mailboxReadTransaction("getItemList", octxt) /* tag/folder caches are populated in beginTransaction... */) {
            Folder folder = folderId == -1 ? null : getFolderById(folderId);
            validateFolderPermissions(folder);

            switch (type) {
                case FOLDER:
                case SEARCHFOLDER:
                case MOUNTPOINT:
                    try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                        result = new ArrayList<MailItem>(mFolderCache.size());
                        for (Folder subfolder : mFolderCache.values()) {
                            if (subfolder.getType() == type || type == MailItem.Type.FOLDER) {
                                if (folder == null || subfolder.getFolderId() == folderId) {
                                    result.add(subfolder);
                                }
                            }
                        }
                    }
                    t.commit();
                    break;
                case TAG:
                    if (folderId != -1 && folderId != ID_FOLDER_TAGS) {
                        return Collections.emptyList();
                    }
                    result = new ArrayList<MailItem>();
                    try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                        for (Tag tag : mTagCache.values()) {
                            if (tag.isListed() && !(tag instanceof SmartFolder)) {
                                result.add(tag);
                            }
                        }
                    }
                    t.commit();
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
                    t.commit();
                    break;
                case SMARTFOLDER:
                    if (folderId != -1 && folderId != ID_FOLDER_TAGS) {
                        return Collections.emptyList();
                    }
                    result = new ArrayList<MailItem>();
                    try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                        for (Tag tag : mTagCache.values()) {
                            if (tag instanceof SmartFolder) {
                                result.add(tag);
                            }
                        }
                    }
                    t.commit();
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
                    t.commit();
                    break;
            }
        }

        Comparator<MailItem> comp = MailItem.getComparator(sort);
        if (comp != null) {
            Collections.sort(result, comp);
        }
        return result;
    }

    /** returns the list of IDs of items of the given type in the given folder. */
    public List<Integer> listItemIds(OperationContext octxt, MailItem.Type type, int folderId) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("listItemIds", octxt)) {
            Folder folder = getFolderById(folderId);
            List<Integer> ids = DbMailItem.listByFolder(folder, type, true);
            t.commit();
            return ids;
        }
    }

    public TypedIdList getItemIds(OperationContext octxt, int folderId) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("listAllItemIds", octxt)) {
            Folder folder = getFolderById(folderId);
            TypedIdList ids = DbMailItem.listByFolder(folder, true);
            t.commit();
            return ids;
        }
    }

    public Map<String, Integer> getDigestsForItems(OperationContext octxt, MailItem.Type type, int folderId)
    throws ServiceException {
        Map<String, Integer> digestToID = null;
        try (final MailboxTransaction t = mailboxReadTransaction("getDigestsForItems", octxt)) {
            Folder folder = folderId == -1 ? null : getFolderById(folderId);
            validateFolderPermissions(folder);
            digestToID = DbMailItem.getDigestsForItems(folder, type);
            t.commit();
        }

        if (digestToID == null) {
            digestToID = Maps.newHashMap();
        }
        return digestToID;
    }

    private void validateFolderPermissions(final Folder folder) throws ServiceException {
        if (folder == null) {
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
        } else {
            if (!folder.canAccess(ACL.RIGHT_READ, getAuthenticatedAccount(), isUsingAdminPrivileges())) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
        }
    }

    public List<ImapMessage> openImapFolder(OperationContext octxt, int folderId) throws ServiceException {
        return openImapFolder(octxt, folderId, null, null).getFirst();
    }

    public Pair<List<ImapMessage>, Boolean> openImapFolder(OperationContext octxt, int folderId, Integer limit, Integer cursorId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("openImapFolder", octxt)) {
            Folder folder = getFolderById(folderId);
            Pair<List<ImapMessage>, Boolean> i4list = DbMailItem.loadImapFolder(folder, limit, cursorId);
            t.commit();
            return i4list;
        }
    }

    public List<Pop3Message> openPop3Folder(OperationContext octxt, Set<Integer> folderIds, Date popSince) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("openPop3Folder", octxt)) {
            ImmutableSet.Builder<Folder> folders = ImmutableSet.builder();
            for (int folderId : folderIds) {
                folders.add(getFolderById(folderId));
            }
            List<Pop3Message> p3list = DbMailItem.loadPop3Folder(folders.build(), popSince);
            t.commit();
            return p3list;
        }
    }

    public int getImapRecent(OperationContext octxt, int folderId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getImapRecent", octxt)) {
            Folder folder = checkAccess(getFolderById(folderId));
            int recent = folder.getImapRECENT();
            t.commit();
            return recent;
        }
    }

    public int getImapRecentCutoff(OperationContext octxt, int folderId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getImapRecentCutoff", octxt)) {
            Folder folder = checkAccess(getFolderById(folderId));
            int cutoff = folder.getImapRECENTCutoff();
            t.commit();
            return cutoff;
        }
    }

    public void beginTrackingImap() throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            if (isTrackingImap()) {
                return;
            }
        }
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            if (isTrackingImap()) {
                return;
            }
            TrackImap redoRecorder = new TrackImap(mId);
            try (final MailboxTransaction t = mailboxWriteTransaction("beginTrackingImap", null, redoRecorder)) {
                DbMailbox.startTrackingImap(this);
                currentChange().imap = Boolean.TRUE;
                t.commit();
            }
        }
    }

    public void beginTrackingSync() throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            if (isTrackingSync()) {
                return;
            }
        }
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            if (isTrackingSync()) {
                return;
            }
            TrackSync redoRecorder = new TrackSync(mId);
            try (final MailboxTransaction t = mailboxWriteTransaction("beginTrackingSync", null, redoRecorder)) {
                currentChange().sync = getLastChangeID();
                DbMailbox.startTrackingSync(this);
                t.commit();
            }
        }
    }

    /**
     * Record that an IMAP client has seen all the messages in this folder as they are at this time.
     * This is used to determine which messages are considered by IMAP to be RECENT
     */
    public void recordImapSession(int folderId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("recordImapSession", null)) {
            getFolderById(folderId).checkpointRECENT();
            t.commit();
        }
    }

    public List<Integer> getTombstones(int lastSync, Set<MailItem.Type> types) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            if (!isTrackingSync()) {
                throw ServiceException.FAILURE("not tracking sync", null);
            } else if (lastSync < getSyncCutoff()) {
                throw MailServiceException.TOMBSTONES_EXPIRED();
            }

            try (final MailboxTransaction t = mailboxReadTransaction("getTombstones", null)) {
                List<Integer> tombstones = DbMailItem.readTombstones(this, getOperationConnection(), lastSync, types);
                t.commit();
                return tombstones;
            }
        }
    }

    public TypedIdList getTombstones(int lastSync, boolean equalModSeq) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            if (!isTrackingSync()) {
                throw ServiceException.FAILURE("not tracking sync", null);
            } else if (lastSync < getSyncCutoff()) {
                throw MailServiceException.TOMBSTONES_EXPIRED();
            }

            try (final MailboxTransaction t = mailboxReadTransaction("getTombstones", null)) {
                TypedIdList tombstones = DbMailItem.readTombstones(this, lastSync, equalModSeq);
                t.commit();
                return tombstones;
            }
        }
    }

    public TypedIdList getTombstones(int lastSync) throws ServiceException {
        return getTombstones(lastSync, Boolean.FALSE);
    }

    public List<Integer> getDumpsterItems(int lastSync, int folderId, int maxTrack) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getDumpsterItems", null)) {
            List<Integer> items = DbMailItem.getDumpsterItems(this, lastSync, folderId, maxTrack);
            t.commit();
            return items;
        }
    }

    public Map<Integer,Integer> getDumpsterItemAndFolderId(int lastSync) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getDumpsterItemAndFolderId", null)) {
            Map<Integer,Integer> items = DbMailItem.getDumpsterItemAndFolderId(this, lastSync);
            t.commit();
            return items;
        }
    }

    public List<Folder> getModifiedFolders(final int lastSync) throws ServiceException {
        return getModifiedFolders(lastSync, MailItem.Type.UNKNOWN);
    }

    public List<Folder> getModifiedFolders(final int lastSync, final MailItem.Type type) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            if (lastSync >= getLastChangeID()) {
                return Collections.emptyList();
            }

            List<Folder> modified = new ArrayList<Folder>();
            try (final MailboxTransaction t = mailboxReadTransaction("getModifiedFolders", null)) {
                for (Folder subfolder : getFolderById(ID_FOLDER_ROOT).getSubfolderHierarchy()) {
                    if (type == MailItem.Type.UNKNOWN || subfolder.getType() == type) {
                        if (subfolder.getModifiedSequence() > lastSync) {
                            modified.add(subfolder);
                        }
                    }
                }
                t.commit();
                return modified;
            }
        }
    }

    public List<Tag> getModifiedTags(OperationContext octxt, int lastSync) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            if (lastSync >= getLastChangeID()) {
                return Collections.emptyList();
            }
            List<Tag> modified = new ArrayList<Tag>();
            try (final MailboxTransaction t = mailboxReadTransaction("getModifiedTags", octxt)) {
                if (hasFullAccess()) {
                    try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                        for (Tag tag: mTagCache.values()) {
                            if (tag.isListed() && tag.getModifiedSequence() > lastSync) {
                                modified.add(tag);
                            }
                        }
                    }
                }
                t.commit();
                return modified;
            }
        }
    }

    /** Returns the IDs of all items modified since a given change number.
     *  Will not return modified folders or tags; for these you need to call
     * @return a List of IDs of all caller-visible MailItems of the given type modified since the checkpoint
     */
    @Override
    public List<Integer> getIdsOfModifiedItemsInFolder(OpContext octxt, int lastSync, int folderId)
    throws ServiceException {
        Set<Integer> folderIds = Sets.newHashSet(folderId);
        return getModifiedItems(OperationContext.asOperationContext(octxt),
                lastSync, MailItem.Type.UNKNOWN, folderIds).getFirst();
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

    public Pair<List<Integer>, TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, MailItem.Type type,
                    Set<Integer> folderIds) throws ServiceException {
        return getModifiedItems(octxt, lastSync, 0, type, folderIds, -1);
    }

    public Pair<List<Integer>, TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, MailItem.Type type,
        Set<Integer> folderIds, int lastDeleteSync) throws ServiceException {
        return getModifiedItems(octxt, lastSync, 0, type, folderIds, lastDeleteSync);
    }

    public Pair<List<Integer>, TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, int sinceDate,
        MailItem.Type type, Set<Integer> folderIds) throws ServiceException {
        return getModifiedItems(octxt, lastSync, sinceDate, type, folderIds, -1);
    }

    public Pair<List<Integer>, TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, int sinceDate,
        MailItem.Type type, Set<Integer> folderIds, int lastDeleteSync) throws ServiceException {
        return getModifiedItems(octxt, lastSync, sinceDate, type, folderIds, lastDeleteSync, 0);
    }

    /**
     * Returns all of the modified items since a given change number, newly added or modified
     * items within given folderIds will be returned as the First field of Pair.
     * Be noted that deleted items within folderIds as well as items that have been altered outside of given folderIds
     * will be returned as "deleted" (second part of Pair) in order to handle item moves
     * @param octxt     The context for this request.
     * @param lastSync  We return items with change ID larger than this value.
     * @param sinceDate We return items with date larger than this value.
     * @param type      The type of MailItems to return.
     * @param folderIds folders from which add/change items are returned
     * @params limit We limit the number rows returned by the limit value
     * @return
     * @throws ServiceException
     */
    public Pair<List<Integer>, TypedIdList> getModifiedItems(OperationContext octxt, int lastSync, int sinceDate,
            MailItem.Type type, Set<Integer> folderIds, int lastDeleteSync, int limit) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            if (lastSync >= getLastChangeID()) {
                return new Pair<List<Integer>, TypedIdList>(Collections.<Integer> emptyList(), new TypedIdList());
            }
            try (final MailboxTransaction t = mailboxReadTransaction("getModifiedItems", octxt)) {
                Set<Integer> visible = Folder.toId(getAccessibleFolders(ACL.RIGHT_READ));
                if (folderIds == null) {
                    folderIds = visible;
                } else if (visible != null) {
                    folderIds = SetUtil.intersect(folderIds, visible);
                }
                Pair<List<Integer>, TypedIdList> dataList = DbMailItem
                                .getModifiedItems(this, type, lastSync, sinceDate, folderIds, lastDeleteSync, limit);
                if (dataList == null) {
                    return null;
                }
                t.commit();
                return dataList;
            }
        }
    }

    /**
     * Returns count of the modified items since a given change number
     * @param octxt     The context for this request.
     * @param lastSync  We return items with change ID larger than this value.
     * @param sinceDate We return items with date larger than this value.
     * @param type      The type of MailItems to return.
     * @param folderIds folders from which add/change items are returned
     * @return
     * @throws ServiceException
     */
    public int getModifiedItemsCount(OperationContext octxt, int lastSync, int sinceDate,
            MailItem.Type type, Set<Integer> folderIds) throws ServiceException {
        if (lastSync >= getLastChangeID()) {
            return 0;
        }
        try (final MailboxLock l = getReadLockAndLockIt();
            final MailboxTransaction t = mailboxReadTransaction("getModifiedItems", null)) {
            Set<Integer> visible = Folder.toId(getAccessibleFolders(ACL.RIGHT_READ));
            if (folderIds == null) {
                folderIds = visible;
            } else if (visible != null) {
                folderIds = SetUtil.intersect(folderIds, visible);
            }
            int count = DbMailItem.getModifiedItemsCount(this, type, lastSync, sinceDate, folderIds);
            t.commit();
            return count;
        }
    }

    /**
     * Returns a list of all {@link Folder}s the authenticated user has {@link ACL#RIGHT_READ} access to. Returns
     * {@code null} if the authenticated user has read access to the entire Mailbox.
     *
     * @see #getAccessibleFolders(short)
     */
    public Set<Folder> getVisibleFolders(OperationContext octxt) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getVisibleFolders", octxt)) {
            Set<Folder> visible = getAccessibleFolders(ACL.RIGHT_READ);
            t.commit();
            return visible;
        }
    }

    /**
     * Returns a list of all {@link Folder}s that the authenticated user from the current transaction has a certain set
     * of rights on. Returns {@code null} if the authenticated user has the required access on the entire Mailbox.
     *
     * @param rights bitmask representing the required permissions
     */
    Set<Folder> getAccessibleFolders(short rights) throws ServiceException {
        if (!currentChange().isActive()) {
            throw ServiceException.FAILURE("cannot get visible hierarchy outside transaction", null);
        }
        if (hasFullAccess()) {
            return null;
        }
        boolean incomplete = false;
        Set<Folder> visible = new HashSet<Folder>();
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            for (Folder folder : mFolderCache.values()) {
                if (folder.canAccess(rights)) {
                    visible.add(folder);
                } else {
                    incomplete = true;
                }
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
    public Tag getTagByName(OperationContext octxt, String name) throws ServiceException {
        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("tag name may not be null", null);
        }

        try (final MailboxTransaction t = mailboxReadTransaction("getTagByName", octxt)) {
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }

            Tag tag = checkAccess(getTagByName(name));
            t.commit();
            return tag;
        }
    }

    Tag getTagByName(String name) throws ServiceException {
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            Tag tag = (name.startsWith(Tag.FLAG_NAME_PREFIX) && !name.startsWith(Tag.SMARTFOLDER_NAME_PREFIX)) ? Flag.of(this, name) : mTagCache.get(name.toLowerCase());
            if (tag == null) {
                throw MailServiceException.NO_SUCH_TAG(name);
            }
            return tag;
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

    /** Returns the folder with the specified uuid.
     * @throws NoSuchItemException if the folder does not exist */
    public Folder getFolderByUuid(OperationContext octxt, String uuid) throws ServiceException {
        return (Folder) getItemByUuid(octxt, uuid, MailItem.Type.FOLDER);
    }

    /** Returns the folder with the specified uuid.
     * @throws NoSuchItemException if the folder does not exist */
    Folder getFolderByUuid(String uuid) throws ServiceException {
        return (Folder) getItemByUuid(uuid, MailItem.Type.FOLDER);
    }

    /** Returns the folder with the specified parent and name.
     * @throws NoSuchItemException if the folder does not exist */
    public Folder getFolderByName(OperationContext octxt, int parentId, String name) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getFolderByName", octxt)) {
            Folder folder = getFolderById(parentId).findSubfolder(name);
            if (folder == null) {
                throw MailServiceException.NO_SUCH_FOLDER(name);
            } else if (!folder.canAccess(ACL.RIGHT_READ)) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on folder " + name);
            }
            t.commit();
            return folder;
        }
    }

    /**
     * @param id - String representation of the ID which MUST be in this mailbox
     * @return FolderStore or null
     */
    @Override
    public FolderStore getFolderById(OpContext octxt, String id) throws ServiceException {
        try {
            String idString = ItemIdentifier.asSimplestString(id, this.getAccountId()).toString();
            Folder fldr = getFolderById((OperationContext)octxt, Integer.parseInt(idString));
            return fldr;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /** Returns the folder with the specified path, delimited by slashes (<tt>/</tt>).
     * @throws {@link NoSuchItemException} if the folder does not exist */
    @Override
    public FolderStore getFolderByPath(OpContext octxt, String path) throws ServiceException {
        return getFolderByPath((OperationContext) octxt, path);
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

        try (final MailboxTransaction t = mailboxReadTransaction("getFolderByPath", octxt) /* for ACL check */) {
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
            t.commit();
            return folder;
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

        try (final MailboxTransaction t = mailboxReadTransaction("getFolderByPathLongestMatch", octxt) /* for ACL check */) {
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
            // endTransaction will be called with success = false
        }
    }

    /**
     * Given a path, resolves as much of the path as possible and returns the folder and the unmatched part.
     *
     * For path e.g. "/foo/bar/baz/gub" where a mailbox has a Folder at "/foo/bar" but NOT one at "/foo/bar/baz"
     * this class can encapsulate this information where:
     *     parentFolderStore is the folder at path "/foo/bar"
     *     unmatchedPart = "baz/gub".
     *
     * If the returned folder is a mountpoint, then perhaps the remaining part is a subfolder in the remote mailbox.
     */
    @Override
    public ExistingParentFolderStoreAndUnmatchedPart getParentFolderStoreAndUnmatchedPart(OpContext octxt, String path)
    throws ServiceException {
        int baseFolderId = Mailbox.ID_FOLDER_USER_ROOT;
        Pair<Folder, String> pair = getFolderByPathLongestMatch((OperationContext)octxt, baseFolderId, path);
        return new ExistingParentFolderStoreAndUnmatchedPart(pair.getFirst(), pair.getSecond());
    }

    public List<Folder> getFolderList(OperationContext octxt, SortBy sort) throws ServiceException {
        List<Folder> folders = new ArrayList<Folder>();
        for (MailItem item : getItemList(octxt, MailItem.Type.FOLDER, -1, sort)) {
            folders.add((Folder) item);
        }
        return folders;
    }

    List<Folder> listAllFolders() throws ServiceException {
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            return new ArrayList<Folder>(mFolderCache.values());
        }
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
        try (final MailboxLock l = getReadLockAndLockIt()) {
            int folderId = iid != null ? iid.getId() : Mailbox.ID_FOLDER_USER_ROOT;
            Folder folder = getFolderById(returnAllVisibleFolders ? null : octxt, folderId);
            // for each subNode...
            Set<Folder> visibleFolders = getVisibleFolders(octxt);
            return handleFolder(folder, visibleFolders, returnAllVisibleFolders);
        }
    }

    public FolderNode getFolderTreeByUuid(OperationContext octxt, String uuid, boolean returnAllVisibleFolders)
                    throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            Folder folder;
            if (uuid != null) {
                folder = getFolderByUuid(returnAllVisibleFolders ? null : octxt, uuid);
            } else {
                folder = getFolderById(returnAllVisibleFolders ? null : octxt, Mailbox.ID_FOLDER_USER_ROOT);
            }
            // for each subNode...
            Set<Folder> visibleFolders = getVisibleFolders(octxt);
            return handleFolder(folder, visibleFolders, returnAllVisibleFolders);
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

    public static boolean isCalendarFolder(Folder f) {
        MailItem.Type view = f.getDefaultView();
        return (view == MailItem.Type.APPOINTMENT || view == MailItem.Type.TASK);
    }

    public List<Mountpoint> getCalendarMountpoints(OperationContext octxt, SortBy sort) throws ServiceException {
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            List<Mountpoint> calFolders = Lists.newArrayList();
            for (MailItem item : getItemList(octxt, MailItem.Type.MOUNTPOINT, -1, sort)) {
                if (isCalendarFolder((Mountpoint) item)) {
                    calFolders.add((Mountpoint) item);
                }
            }
            return calFolders;
        }
    }

    public List<Folder> getCalendarFolders(OperationContext octxt, SortBy sort) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            List<Folder> calFolders = Lists.newArrayList();
            for (MailItem item : getItemList(octxt, MailItem.Type.FOLDER, -1, sort)) {
                if (isCalendarFolder((Folder) item)) {
                    calFolders.add((Folder) item);
                }
            }
            for (MailItem item : getItemList(octxt, MailItem.Type.MOUNTPOINT, -1, sort)) {
                if (isCalendarFolder((Folder) item)) {
                    calFolders.add((Folder) item);
                }
            }
            return calFolders;
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

    public Mountpoint getMountpointByUuid(OperationContext octxt, String mptUuid) throws ServiceException {
        return (Mountpoint) getItemByUuid(octxt, mptUuid, MailItem.Type.MOUNTPOINT);
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
        if (currentChange().isActive()) {
            return (Message) getItemById(id, MailItem.Type.MESSAGE);
        } else {
            // need to be in a transaction.
            return (Message) getItemById((OperationContext)null, id, MailItem.Type.MESSAGE);
        }
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
        return getMessagesByConversation(octxt, convId, sort, limit, false);
    }

    public List<Message> getMessagesByConversation(OperationContext octxt, int convId, SortBy sort, int limit,
                    boolean excludeSpamAndTrash) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getMessagesByConversation", octxt)) {
            List<Message> msgs = getConversationById(convId).getMessages(sort, limit);
            msgs = filterToAccessibleMessages(msgs, excludeSpamAndTrash);
            t.commit();
            return msgs;
        }
    }

    public List<Message> getMessagesByConversation(OperationContext octxt, Conversation conv,
            SortBy sort, int limit, boolean excludeSpamAndTrash) throws ServiceException {
        /* performance optimization.  If can get the info without the overhead of a transaction
         * then do so. */
        List<Message> msgs = conv.getMessagesIfCanOutsideTransaction(sort, limit);
        if (msgs != null) {
            return filterToAccessibleMessages(conv.getMessages(sort, limit), excludeSpamAndTrash);
        }
        try (final MailboxTransaction t = mailboxReadTransaction("getMessagesByConversation", octxt)) {
            msgs = conv.getMessages(sort, limit);
            msgs = filterToAccessibleMessages(msgs, excludeSpamAndTrash);
            t.commit();
            return msgs;
        }
    }

    public List<Message> getMessagesByConversations(OperationContext octxt, Collection<Conversation> convs,
            SortBy sort, int limit, boolean excludeSpamAndTrash) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getMessagesByConversations", octxt)) {
            List<Message> msgs = filterToAccessibleMessages(
                    Conversation.updateMessageInfoForConversations(convs, sort, limit), excludeSpamAndTrash);
            t.commit();
            return msgs;
        }
    }

    private List<Message> filterToAccessibleMessages(Collection<Message> msgs, boolean excludeSpamAndTrash)
            throws ServiceException {
        boolean hasMailboxAccess = hasFullAccess();
        return msgs.stream().filter(msg -> {
            try {
                if (!hasMailboxAccess && !msg.canAccess(ACL.RIGHT_READ)) {
                    return false;
                }
                if (excludeSpamAndTrash && (msg.inTrash() || msg.inSpam())) {
                    return false;
                }
            } catch (ServiceException e) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
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
        try (final MailboxTransaction t = mailboxReadTransaction("getConversationByHash", octxt)) {
            Conversation conv = checkAccess(getConversationByHash(hash));
            t.commit();
            return conv;
        }
    }

    Conversation getConversationByHash(String hash) throws ServiceException {
        Conversation conv = null;

        Integer convId = mConvHashes.get(hash);
        if (convId != null) {
            conv = getCachedConversation(convId);
        }
        if (conv != null) {
            return conv;
        }

        // XXX: why not just do a "getConversationById()" if convId != null?
        MailItem.UnderlyingData data = DbMailItem.getByHash(this, hash);
        if (data == null || data.type == MailItem.Type.CONVERSATION.toByte()) {
            return getConversation(data);
        }
        return (Conversation) getMessage(data).getParent();
    }

    public SenderList getConversationSenderList(Conversation conv) throws ServiceException {
        /* performance optimization.  If can get the info without the overhead of a transaction
         * then do so. */
        Pair<Boolean, SenderList> pair = conv.getSenderListIfCanOutsideTransaction();
        if (pair.getFirst()) {
            return pair.getSecond();
        }
        try (final MailboxTransaction t = mailboxWriteTransaction("getSenderList", null)) {
            SenderList sl = conv.getSenderList();
            t.commit();
            return sl;
        }
    }

    public SenderList getConversationSenderList(int convId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getSenderList", null)) {
            Conversation conv = getConversationById(convId);
            SenderList sl = conv.getSenderList();
            t.commit();
            return sl;
        }
    }

    public Document getDocumentById(OperationContext octxt, int id) throws ServiceException {
        return (Document) getItemById(octxt, id, MailItem.Type.DOCUMENT);
    }

    public Document getDocumentByUuid(OperationContext octxt, String uuid) throws ServiceException {
        return (Document) getItemByUuid(octxt, uuid, MailItem.Type.DOCUMENT);
    }

    Document getDocumentById(int id) throws ServiceException {
        return (Document) getItemById(id, MailItem.Type.DOCUMENT);
    }

    public List<Document> getDocumentList(OperationContext octxt, int folderId) throws ServiceException {
        return getDocumentList(octxt, folderId, SortBy.NONE);
    }

    public List<Document> getDocumentList(OperationContext octxt, int folderId, SortBy sort) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            List<Document> docs = new ArrayList<Document>();
            for (MailItem item : getItemList(octxt, MailItem.Type.DOCUMENT, folderId, sort)) {
                docs.add((Document) item);
            }
            return docs;
        }
    }

    public Collection<CalendarItem.CalendarMetadata> getCalendarItemMetadata(int folderId, long start, long end) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getCalendarItemMetadata", null)) {
            Folder f = getFolderById(folderId);
            if (!f.canAccess(ACL.RIGHT_READ)) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
            t.commit();
            return DbMailItem.getCalendarItemMetadata(f, start, end);
        }
    }

    private void checkCalendarType(MailItem item) throws ServiceException {
        MailItem.Type type = item.getType();
        if (type != MailItem.Type.APPOINTMENT && type != MailItem.Type.TASK) {
            throw MailServiceException.NO_SUCH_CALITEM(item.getId());
        }
    }

    /**
     * Get Calendar entry associated with {@code id} as long as the calendar belongs to this mailbox
     * Note: This means that entries for mounted calendars are NOT returned.
     */
    public CalendarItem getCalendarItemById(OperationContext octxt, ItemId id) throws ServiceException {
        if ((id == null) || (!id.belongsTo(this))) {
            return null;
        }
        return getCalendarItemById(octxt, id.getId());
    }

    public CalendarItem getCalendarItemById(OperationContext octxt, int id) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            MailItem item = getItemById(octxt, id, MailItem.Type.UNKNOWN);
            checkCalendarType(item);
            return (CalendarItem) item;
        }
    }

    private CalendarItem getCalendarItemById(int id) throws ServiceException {
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

    /**
     * @param start     start time of range, in milliseconds. {@code -1} means to leave the start time unconstrained.
     * @param end       end time of range, in milliseconds. {@code -1} means to leave the end time unconstrained.
     */
    public TypedIdList listCalendarItemsForRange(OperationContext octxt, MailItem.Type type, long start, long end,
                    int folderId) throws ServiceException {
        if (folderId == ID_AUTO_INCREMENT) {
            return new TypedIdList();
        }
        try (final MailboxTransaction t = mailboxReadTransaction("listCalendarItemsForRange", octxt)) {
            // if they specified a folder, make sure it actually exists
            getFolderById(folderId);

            // get the list of all visible calendar items in the specified folder
            TypedIdList ids = DbMailItem.listCalendarItems(this, type, start, end, folderId, null);
            t.commit();
            return ids;
        }
    }

    public List<CalendarItem> getCalendarItems(OperationContext octxt, MailItem.Type type, int folderId)
            throws ServiceException {
        return getCalendarItemsForRange(octxt, type, -1, -1, folderId, null);
    }

    /**
     * @param start     start time of range, in milliseconds. {@code -1} means to leave the start time unconstrained.
     * @param end       end time of range, in milliseconds. {@code -1} means to leave the end time unconstrained.
     */
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
        try (final MailboxTransaction t = mailboxReadTransaction("getCalendarItemsForRange", octxt)) {

            // if they specified a folder, make sure it actually exists
            if (folderId != ID_AUTO_INCREMENT) {
                getFolderById(folderId);
            }

            // get the list of all visible calendar items in the specified folder
            List<CalendarItem> calItems = new ArrayList<CalendarItem>();
            List<MailItem.UnderlyingData> invData = DbMailItem.getCalendarItems(this, type, start, end, folderId,
                            excludeFolders);
            for (MailItem.UnderlyingData data : invData) {
                try {
                    CalendarItem calItem = getCalendarItem(data);
                    if (folderId == calItem.getFolderId() || (folderId == ID_AUTO_INCREMENT && calItem.inMailbox())) {
                        if (calItem.canAccess(ACL.RIGHT_READ)) {
                            calItems.add(calItem);
                        }
                    }
                } catch (ServiceException e) {
                    ZimbraLog.calendar.warn("Error while retrieving calendar item " + data.id + " in mailbox " + mId
                                    + "; skipping item", e);
                }
            }
            t.commit();
            return calItems;
        }
    }

    public List<Integer> getItemIdList(OperationContext octxt, QueryParams params) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getItemIdList", octxt)) {
            List<Integer> msgIds = DbMailItem.getIdsInOrder(this, this.getOperationConnection(), params, false);
            t.commit();
            return msgIds;
        }
    }

    public List<Pair<Integer, Integer>> getDateAndItemIdList(OperationContext octxt, QueryParams params)
            throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getDateAndItemIdList", octxt)) {
            List<Pair<Integer, Integer>> result =
                    DbMailItem.getDatesAndIdsInOrder(this, this.getOperationConnection(), params, false);
            t.commit();
            return result;
        }
    }

    public Pair<Long, Long> getSearchableItemDateBoundaries(OperationContext octxt)
            throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getSearchableItemDateBoundaries", octxt)) {
            Long oldDate = DbMailItem.getOldestSearchableItemDate(this, this.getOperationConnection());
            Long recentDate = DbMailItem.getMostRecentSearchableItemDate(this, this.getOperationConnection());

            t.commit();
            return new Pair<Long,Long>(oldDate,recentDate);
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
            boolean trimCalItemsList, boolean escapeHtmlTags)
    throws ServiceException {
        writeICalendarForCalendarItems(writer, octxt, calItems,
            f, useOutlookCompatMode, ignoreErrors, needAppleICalHacks,
            trimCalItemsList, escapeHtmlTags, false /* includeAttaches */);
    }

    public void writeICalendarForCalendarItems(Writer writer, OperationContext octxt, Collection<CalendarItem> calItems,
            Folder f, boolean useOutlookCompatMode, boolean ignoreErrors, boolean needAppleICalHacks,
            boolean trimCalItemsList, boolean escapeHtmlTags, boolean includeAttaches)
    throws ServiceException {
        try (final MailboxLock l = getWriteLockAndLockIt()) {
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
            for (Iterator<ICalTimeZone> iter = tzmap.tzIterator(); iter.hasNext();) {
                ICalTimeZone tz = iter.next();
                tz.newToVTimeZone().toICalendar(writer, needAppleICalHacks);
            }
            tzmap = null; // help keep memory consumption low

            // build all the event components and add them to the Calendar
            for (Iterator<CalendarItem> iter = calItems.iterator(); iter.hasNext();) {
                CalendarItem calItem = iter.next();
                boolean allowPrivateAccess = calItem.isPublic()
                                || calItem.allowPrivateAccess(octxt.getAuthenticatedUser(),
                                                octxt.isUsingAdminPrivileges());
                if (trimCalItemsList)
                 {
                    iter.remove(); // help keep memory consumption low
                }
                Invite[] invites = calItem.getInvites();
                if (invites != null && invites.length > 0) {
                    boolean appleICalExdateHack = LC.calendar_apple_ical_compatible_canceled_instances.booleanValue();
                    ZComponent[] comps = null;
                    try {
                        comps = Invite.toVComponents(invites, allowPrivateAccess, useOutlookCompatMode,
                                        appleICalExdateHack, includeAttaches);
                    } catch (ServiceException e) {
                        if (ignoreErrors) {
                            ZimbraLog.calendar.warn("Error retrieving iCalendar data for item %s: %s", calItem.getId(),
                                            e.getMessage(), e);
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
        try (final MailboxTransaction t = mailboxWriteTransaction("writeICalendarForRange", octxt)) {
            Collection<CalendarItem> calItems = getCalendarItemsForRange(octxt, start, end, folderId, null);
            writeICalendarForCalendarItems(writer, octxt, calItems, null, useOutlookCompatMode, ignoreErrors,
                    needAppleICalHacks, true, escapeHtmlTags);
        }
    }

    public CalendarDataResult getCalendarSummaryForRange(OperationContext octxt, int folderId, MailItem.Type type,
                    long start, long end) throws ServiceException {
        try (final MailboxLock l = getReadLockAndLockIt()) {
            Folder folder = getFolderById(folderId);
            if (!folder.canAccess(ACL.RIGHT_READ)) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on folder "
                                + folder.getName());
            }
            return CalendarCacheManager.getInstance().getSummaryCache()
                            .getCalendarSummary(octxt, getAccountId(), folderId, type, start, end, true);
        }
    }

    public List<CalendarDataResult> getAllCalendarsSummaryForRange(OperationContext octxt, MailItem.Type type,
                    long start, long end) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getAllCalendarsSummaryForRange", octxt)) {
            // folder cache is populated in new MailboxTransaction...
            t.commit();
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
                try {
                    if (!folder.canAccess(ACL.RIGHT_READ)) {
                        continue;
                    }
                    CalendarDataResult result = CalendarCacheManager.getInstance().getSummaryCache().
                        getCalendarSummary(octxt, getAccountId(), folder.getId(), type, start, end, true);
                    if (result != null) {
                        list.add(result);
                    }
                } catch (ServiceException se) {
                    ZimbraLog.fb.info("Problem getting calendar summary cache for folder '%s' - ignoring",
                            folder.getName(), se);
                    throw se;
                }
            }
            return list;
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
        if (octxt == null) {
            throw ServiceException.INVALID_REQUEST("The OperationContext must not be null", null);
        }

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
        try (final MailboxLock l = getReadLockAndLockIt()) {
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
        try (final MailboxTransaction t = mailboxWriteTransaction("browse", octxt)) {
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

            t.commit();
            return result;
        }
    }

    public void dismissCalendarItemAlarm(OperationContext octxt, int calItemId, long dismissedAt)
            throws ServiceException {
        DismissCalendarItemAlarm redoRecorder = new DismissCalendarItemAlarm(getId(), calItemId, dismissedAt);
        try (final MailboxTransaction t = mailboxWriteTransaction("dismissAlarm", octxt, redoRecorder)) {
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(calItemId);
            }
            markItemModified(calItem, Change.INVITE);
            calItem.snapshotRevision();
            calItem.updateNextAlarm(dismissedAt + 1, true);
            t.commit();
        }
    }

    public void snoozeCalendarItemAlarm(OperationContext octxt, int calItemId, long snoozeUntil)
            throws ServiceException {
        SnoozeCalendarItemAlarm redoRecorder = new SnoozeCalendarItemAlarm(getId(), calItemId, snoozeUntil);
        try (final MailboxTransaction t = mailboxWriteTransaction("snoozeAlarm", octxt, redoRecorder)){
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(calItemId);
            }
            markItemModified(calItem, Change.INVITE);
            calItem.snapshotRevision();
            calItem.updateNextAlarm(snoozeUntil, false);
            t.commit();
        }
    }

    public static final class SetCalendarItemData {
        public Invite invite;
        public ParsedMessage message;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("inv", invite).add("hasBody", message != null).toString();
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

        try (final MailboxTransaction t = mailboxWriteTransaction("setCalendarItem", octxt, redoRecorder)) {
            Tag.NormalizedTags ntags = new Tag.NormalizedTags(this, tags);

            // Make a single list containing default and exceptions.
            int scidLen = (defaultInv != null ? 1 : 0) + (exceptions != null ? exceptions.length : 0);
            List<SetCalendarItemData> scidList = new ArrayList<SetCalendarItemData>(scidLen);
            if (defaultInv != null) {
                defaultInv.invite.setLastFullSeqNo(defaultInv.invite.getSeqNo());
                scidList.add(defaultInv);
            }
            if (exceptions != null) {
                for (SetCalendarItemData scid : exceptions) {
                    scid.invite.setLastFullSeqNo(scid.invite.getSeqNo());
                    scidList.add(scid);
                }
            }

            CalendarItem calItem = null;

            // bug 19868: Preserve invId of existing Invites.  We have to do this before making any
            // calls to processNewInvite() because it'll delete all existing Invites and we'll lose
            // old invId information.
            if (!scidList.isEmpty()) {
                calItem = getCalendarItemByUid(octxt, scidList.get(0).invite.getUid());
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
            }

            if (scidList.size() > 0) {
                SetCalendarItemData scid = scidList.get(0);
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
                                        (scidList.isEmpty() ? "no items in uid list." :
                                            "uid not found in appointment: " + scidList.get(0).invite.getUid() +
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
                    calItem.processNewInvite(scid.message, scid.invite, folderId, nextAlarm,
                            false /* preserveAlarms */, true /* replaceExistingInvites */,
                            false /* updatePrevFolders */);
                }
                redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId());
            }
            if (scidList.size() > 1) {
                // remove the first one. it is already processed
                scidList.remove(0);
                // exceptions
                calItem.processNewInviteExceptions(scidList, folderId, nextAlarm, false, false);
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

            t.commit();
            return calItem;
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
            for (Iterator<MailItem> iter = items.iterator(); iter.hasNext();) {
                Object obj = iter.next();
                if (!(obj instanceof CalendarItem)) {
                    continue;
                }
                CalendarItem calItem = (CalendarItem) obj;
                long end = calItem.getEndTime();
                if (end <= after) {
                    continue;
                }
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
        try (final MailboxTransaction t = mailboxWriteTransaction("fixCalendarItemTimeZone2", octxt, redoRecorder)) {
            CalendarItem calItem = getCalendarItemById(octxt, calItemId);
            Map<String, ICalTimeZone> replaced = new HashMap<String, ICalTimeZone>();
            int numFixed = fixupRules.fixCalendarItem(calItem, replaced);
            if (numFixed > 0) {
                ZimbraLog.calendar.info("Fixed " + numFixed + " timezone entries in calendar item " + calItem.getId());
                redoRecorder.setReplacementMap(replaced);
                markItemModified(calItem, Change.CONTENT | Change.INVITE);
                calItem.snapshotRevision();
                calItem.saveMetadata();
                // Need to uncache and refetch the item because there are fields
                // in the appointment/task that reference the old, pre-fix version
                // of the timezones.  We can either visit them all and update them,
                // or simply invalidate the calendar item and refetch it.
                uncacheItem(calItemId);
                calItem = getCalendarItemById(octxt, calItemId);
                t.commit();

                Callback cb = CalendarItem.getCallback();
                if (cb != null) {
                    cb.modified(calItem);
                }
            }
            return numFixed;
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
            for (Iterator<MailItem> iter = items.iterator(); iter.hasNext();) {
                Object obj = iter.next();
                if (!(obj instanceof CalendarItem)) {
                    continue;
                }
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
        try (final MailboxTransaction t = mailboxWriteTransaction("fixupCalendarItemEndTime", octxt, redoRecorder)) {
            int numFixed = calItem.fixRecurrenceEndTime();
            if (numFixed > 0) {
                ZimbraLog.calendar.info("Fixed calendar item " + calItem.getId());
                calItem.snapshotRevision();
                calItem.saveMetadata();
                t.commit();
            }
            return numFixed;
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
            for (Iterator<MailItem> iter = items.iterator(); iter.hasNext();) {
                Object obj = iter.next();
                if (!(obj instanceof CalendarItem)) {
                    continue;
                }
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
        try (final MailboxTransaction t = mailboxWriteTransaction("fixupCalendarItemPriority", octxt, redoRecorder)) {
            int flags = calItem.state.getFlags() & ~(Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_LOW_PRIORITY);
            Invite[] invs = calItem.getInvites();
            if (invs != null) {
                for (Invite cur : invs) {
                    String method = cur.getMethod();
                    if (method.equals(ICalTok.REQUEST.toString()) ||
                        method.equals(ICalTok.PUBLISH.toString())) {
                        if (cur.isHighPriority()) {
                            flags |= Flag.BITMASK_HIGH_PRIORITY;
                        }
                        if (cur.isLowPriority()) {
                            flags |= Flag.BITMASK_LOW_PRIORITY;
                        }
                    }
                }
            }
            int numFixed = 0;
            if (flags != calItem.state.getFlags()) {
                ZimbraLog.calendar.info("Fixed calendar item " + calItem.getId());
                markItemModified(calItem, Change.INVITE);
                calItem.state.setFlags(flags);
                calItem.snapshotRevision();
                calItem.saveMetadata();
                t.commit();
                numFixed = 1;
            }
            return numFixed;
        }
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId)
            throws ServiceException {
        boolean addRevision = true;  // Always rev the calendar item.
        return addInvite(octxt, inv, folderId, null, false, false, addRevision, false);
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, boolean updatePrevFolders)
            throws ServiceException {
        boolean addRevision = true;  // Always rev the calendar item.
        return addInvite(octxt, inv, folderId, null, false, false, addRevision, updatePrevFolders);
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, ParsedMessage pm)
            throws ServiceException {
        boolean addRevision = true;  // Always rev the calendar item.
        return addInvite(octxt, inv, folderId, pm, false, false, addRevision);
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, ParsedMessage pm, boolean updatePrevFolders)
            throws ServiceException {
        boolean addRevision = true;  // Always rev the calendar item.
        return addInvite(octxt, inv, folderId, pm, false, false, addRevision, updatePrevFolders);
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, boolean preserveExistingAlarms,
            boolean addRevision) throws ServiceException {
        return addInvite(octxt, inv, folderId, null, preserveExistingAlarms, false, addRevision);
    }

    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, ParsedMessage pm,
            boolean preserveExistingAlarms, boolean discardExistingInvites, boolean addRevision)
            throws ServiceException {
        return addInvite(octxt, inv, folderId, pm, preserveExistingAlarms, discardExistingInvites, addRevision, false);
    }
    /**
     * Directly add an Invite into the system...this process also gets triggered when we add a Message
     * that has a text/calendar Mime part: but this API is useful when you don't want to add a corresponding
     * message.
     * @param octxt
     * @param inv
     * @param folderId
     * @param pm NULL is OK here
     * @param preserveExistingAlarms
     * @param discardExistingInvites
     * @param addRevision if true and revisioning is enabled and calendar item exists already, add a revision
     *                    with current snapshot of the calendar item
     * @param updatePrevFolders
     * @return AddInviteData
     * @throws ServiceException
     */
    public AddInviteData addInvite(OperationContext octxt, Invite inv, int folderId, ParsedMessage pm,
            boolean preserveExistingAlarms, boolean discardExistingInvites, boolean addRevision,
            boolean updatePrevFolders)
            throws ServiceException {
        if (pm == null) {
            inv.setDontIndexMimeMessage(true); // the MimeMessage is fake, so we don't need to index it
            String desc = inv.getDescription();
            if (inv.hasAttachment() || (desc != null && (desc.length() > Invite.getMaxDescInMeta()))) {
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

        try (final MailboxTransaction t = mailboxWriteTransaction("addInvite", octxt, redoRecorder)) {
            CreateInvite redoPlayer = (octxt == null ? null : (CreateInvite) octxt.getPlayer());

            if (redoPlayer == null || redoPlayer.getCalendarItemId() == 0) {
                int currId = inv.getMailItemId();
                if (currId <= 0) {
                    inv.setInviteId(getNextItemId(Mailbox.ID_AUTO_INCREMENT));
                } else {
                    inv.setInviteId(currId);
                }
            }

            CalendarItem calItem = getCalendarItemByUid(octxt, inv.getUid());
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
                        preserveExistingAlarms, discardExistingInvites, updatePrevFolders);
            }

            if (Invite.isOrganizerMethod(inv.getMethod())) { // Don't update the index for replies. (bug 55317)
                index.add(calItem);
            }

            redoRecorder.setCalendarItemAttrs(calItem.getId(), calItem.getFolderId());

            t.commit();
            if (processed) {
                return new AddInviteData(calItem.getId(), inv.getMailItemId(), inv.getComponentNum(),
                        calItem.getModifiedSequence(), calItem.getSavedSequence());
            } else {
                return null;
            }
        }
    }

    public CalendarItem getCalendarItemByUid(OperationContext octxt, String uid) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getCalendarItemByUid", octxt)) {
            MailItem.UnderlyingData data = DbMailItem.getCalendarItem(this, uid);
            CalendarItem calItem = (CalendarItem) getItem(data);
            t.commit();
            return calItem;
        }
    }

    public Map<String, CalendarItem> getCalendarItemsByUid(OperationContext octxt, List<String> uids)
    throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getCalendarItemsByUid", octxt)) {
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
            t.commit();
            for (String missingUid : uidList) {
                calItems.put(missingUid, null);
            }
            return calItems;
        }
    }


    public boolean dedupeForSelfMsg(ParsedMessage pm) throws ServiceException {
        if (pm == null) {
            return false;
        }
        CalendarPartInfo cpi = pm.getCalendarPartInfo();
        String msgidHeader = pm.getMessageID();
        boolean dedupe = false;

        if (msgidHeader == null) {
            return false;
        }

        // if the deduping rules say to drop this duplicated incoming message,
        // .... but only dedupe messages not carrying a calendar part
        if (mSentMessageIDs.containsKey(msgidHeader)
            && (cpi == null || !CalendarItem.isAcceptableInvite(getAccount(), cpi))) {
            Account acct = getAccount();
            switch (acct.getPrefDedupeMessagesSentToSelf()) {
            case dedupeAll: {
                dedupe = true;
                break;
            }
            case secondCopyifOnToOrCC:
                try {
                    dedupe = !AccountUtil.isDirectRecipient(acct, pm.getMimeMessage());
                } catch (Exception e) {
                    ZimbraLog.mailbox.info(e.getMessage());
                }
                break;
            case dedupeNone:
            default:
            }
        }
        return dedupe;
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
    public void processICalReply(OperationContext octxt, Invite inv, String sender) throws ServiceException {
        ICalReply redoRecorder = new ICalReply(getId(), inv, sender);
        try (final MailboxTransaction t = mailboxWriteTransaction("iCalReply", octxt, redoRecorder)) {
            String uid = inv.getUid();
            CalendarItem calItem = getCalendarItemByUid(octxt, uid);
            if (calItem == null) {
                ZimbraLog.calendar.warn("Unknown calendar item UID %s", uid);
                return;
            }
            calItem.snapshotRevision();
            calItem.processNewInviteReply(inv, sender, false /* don't update prev folders */,
                    new NextItemIdGetter());
            t.commit();
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

    private void processICalReplies(OperationContext octxt, ZVCalendar cal, String sender)
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
                processICalReply(octxt, inv, sender);
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
                    mbox.processICalReply(orgOctxt, inv, sender);
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
                            inv.setMethod(ICalTok.REPLY.toString());
                            inv.newToICalendar(true).toICalendar(sr);
                            ical = sr.toString();
                        } finally {
                            if (sr != null) {
                                sr.close();
                            }
                        }
                        Options options = new Options();
                        AuthToken authToken = AuthToken.getCsrfUnsecuredAuthToken(getAuthToken(octxt));
                        options.setAuthToken(authToken.toZAuthToken());
                        options.setTargetAccount(orgAccount.getName());
                        options.setTargetAccountBy(AccountBy.name);
                        options.setUri(uri);
                        options.setNoSession(true);
                        ZMailbox zmbox = ZMailbox.getMailbox(options);
                        zmbox.setAccountId(orgAccount.getId());
                        zmbox.iCalReply(ical, sender);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Error while posting REPLY to organizer mailbox host", e);
                    }
                }
            }
        }
    }

    public com.zimbra.soap.mail.type.CalendarItemInfo getRemoteCalItemByUID(Account ownerAccount, String uid,
            boolean includeInvites, boolean includeContent)
    throws ServiceException {
        Options options = new Options();
        AuthToken authToken = AuthToken.getCsrfUnsecuredAuthToken(getAuthToken(getOperationContext()));
        options.setAuthToken(authToken.toZAuthToken());
        options.setTargetAccount(getAccount().getName());
        options.setTargetAccountBy(AccountBy.name);
        options.setUri(AccountUtil.getSoapUri(ownerAccount));
        options.setNoSession(true);
        ZMailbox zmbox = ZMailbox.getMailbox(options);
        try {
            return zmbox.getRemoteCalItemByUID(ownerAccount.getId(), uid, includeInvites, includeContent);
        } catch (ServiceException e) {
            String exceptionCode = e.getCode();
            if (exceptionCode.equals(AccountServiceException.NO_SUCH_ACCOUNT) ||
                    exceptionCode.equals(MailServiceException.NO_SUCH_CALITEM)) {
                ZimbraLog.calendar.debug("Either remote acct or calendar item not found [%s]", exceptionCode);
            } else {
                ZimbraLog.calendar.debug("Unexpected exception thrown when getting remote calendar item - ignoring", e);
            }
            return null;
        }
    }

    public Message addMessage(OperationContext octxt, InputStream in, long sizeHint, Long receivedDate, DeliveryOptions dopt, DeliveryContext dctxt, ItemData id)
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

            blob = StoreManager.getInstance().storeIncoming(in);

            if (id != null && id.ud != null && id.ud.getBlobDigest() != null && !id.ud.getBlobDigest().isEmpty()) {
                blob.setDigest(id.ud.getBlobDigest());
            }

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

    public Message addMessage(OperationContext octxt, InputStream in, long sizeHint, Long receivedDate, DeliveryOptions dopt, DeliveryContext dctxt)
        throws IOException, ServiceException {
        return addMessage(octxt, in, sizeHint, receivedDate, dopt, dctxt, null);
    }

    public Message addMessage(OperationContext octxt, ParsedMessage pm, DeliveryOptions dopt, DeliveryContext dctxt)
    throws IOException, ServiceException {
        return addMessage(octxt, pm, dopt, dctxt, null);
    }

    public Message addMessage(OperationContext octxt, ParsedMessage pm, DeliveryOptions dopt, DeliveryContext dctxt, Message.DraftInfo dinfo)
    throws IOException, ServiceException {
        return addMessage(octxt, pm, dopt.getFolderId(), dopt.getNoICal(), dopt.getFlags(), dopt.getTags(),
                          dopt.getConversationId(), dopt.getRecipientEmail(), dinfo, dopt.getCustomMetadata(), dopt.getCallbackContext(), dctxt, dopt.geDataSourceId());
    }

    private Message addMessage(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal, int flags,
            String[] tags, int conversationId, String rcptEmail, Message.DraftInfo dinfo, CustomMetadata customData,
            MessageCallbackContext callbackContext, DeliveryContext dctxt, String dataSourceId)
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
                        processICalReplies(octxt, cpi.cal, null);
                    } else if (ICalTok.COUNTER.equals(cpi.method)) {
                        processICalReplies(octxt, cpi.cal, pm.getSender());
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

        boolean deleteMailboxSpecificBlob = false;
        StoreManager sm = StoreManager.getInstance();
        Blob blob = dctxt.getMailBoxSpecificBlob(mId);
        if (blob == null) {
            blob = dctxt.getIncomingBlob();
            ZimbraLog.filter.debug("MailBoxSpecificBlob is null for mailbox %d", mId);
        } else {
            deleteMailboxSpecificBlob = true;
            ZimbraLog.filter.debug("got MailBoxSpecificBlob for mailbox %d", mId);
        }
        boolean deleteIncoming = false;

        if (blob == null) {
            InputStream in = null;
            try {
                in = pm.getRawInputStream();
                blob = sm.storeIncoming(in);
            } finally {
                ByteUtil.closeStream(in);
            }
            dctxt.setIncomingBlob(blob);
            deleteIncoming = true;
        }

        StagedBlob staged = sm.stage(blob, this);

        Account account = this.getAccount();
        boolean localMsgMarkedRead = false;
        if (account.getPrefMailForwardingAddress() != null && account.isFeatureMailForwardingEnabled()
            && account.isFeatureMarkMailForwardedAsRead()) {
            ZimbraLog.mailbox.debug("Marking forwarded message as read.");
            flags = flags & ~Flag.BITMASK_UNREAD;
            localMsgMarkedRead = true;
        }
        boolean msgAdded = false;
        int max_tries = 3;
        int tries = 1;
        while (!msgAdded && (tries <= max_tries)) {
            try (final MailboxLock l = this.getWriteLockAndLockIt()) {
                try {
                    Message message =  addMessageInternal(octxt, pm, folderId, noICal, flags, tags, conversationId,
                            rcptEmail, dinfo, customData, dctxt, staged, callbackContext, dataSourceId);
                    msgAdded = true;
                    if (localMsgMarkedRead && account.getPrefMailSendReadReceipts().isAlways()) {
                        SendDeliveryReport.sendReport(account, message, true, null, null);
                    }
                    if (tries > 1) {
                        ZimbraLog.mailbox.info("AddMessage succeeded on attempt %s", tries);
                    }
                    return message;
                } catch (ServiceException.TransactionRollbackException tre) {
                    if (msgAdded) {
                        ZimbraLog.mailbox.error("AddMessage - Exception seen although message already added", tre);
                        throw tre;  /* strange - best to treat as an error rather than try to recover */
                    }
                    if (tries >= max_tries) {
                        ZimbraLog.mailbox.error("AddMessage failed due to Database rollbacks after %s retries - %s",
                                tries, tre.getMessage());
                        throw tre;
                    }
                    ZimbraLog.mailbox.warn("Retry AddMessage %s of %s after Database rollback", tries, max_tries);
                } finally {
                    if (msgAdded || tries >= max_tries) {
                        if (deleteIncoming) {
                            sm.quietDelete(dctxt.getIncomingBlob());
                        }
                        if (deleteMailboxSpecificBlob) {
                            sm.quietDelete(dctxt.getMailBoxSpecificBlob(mId));
                            dctxt.clearMailBoxSpecificBlob(mId);
                        }
                        sm.quietDelete(staged);
                    }
                }
            } finally {
                if (msgAdded || tries >= max_tries) {
                    ZimbraPerf.STOPWATCH_MBOX_ADD_MSG.stop(start);
                }
                tries++;
            }
        }
        /* should never get here */
        throw ServiceException.FAILURE("Mailbox.AddMessage Unexpected problem - coding error", null);
    }

    void indexItem(MailItem item) throws ServiceException {
        if(!index.add(item) && Provisioning.getInstance().getLocalServer().getMaxIndexingRetries() > 0) {
            currentChange().addIndexItem(item);
        }
    }

    private Message addMessageInternal(OperationContext octxt, ParsedMessage pm, int folderId, boolean noICal,
            int flags, String[] tags, int conversationId, String rcptEmail, Message.DraftInfo dinfo,
            CustomMetadata customData, DeliveryContext dctxt, StagedBlob staged, MessageCallbackContext callbackContext, String dsId)
    throws IOException, ServiceException {
        assert isWriteLockedByCurrentThread();
        if (pm == null) {
            throw ServiceException.INVALID_REQUEST("null ParsedMessage when adding message to mailbox " + mId, null);
        }

        if (Math.abs(conversationId) <= HIGHEST_SYSTEM_ID) {
            conversationId = ID_AUTO_INCREMENT;
        }

        CreateMessage redoPlayer = (octxt == null ? null : (CreateMessage) octxt.getPlayer());
        boolean needRedo = needRedo(octxt, redoPlayer);
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
        if (!isRedo && msgidHeader != null && !isSent && mSentMessageIDs.containsKey(msgidHeader)) {
            Integer sentMsgID = mSentMessageIDs.get(msgidHeader);
            if (conversationId == ID_AUTO_INCREMENT) {
                conversationId = getConversationIdFromReferent(pm.getMimeMessage(), sentMsgID.intValue());
                ZimbraLog.mailbox.debug("duplicate detected but not deduped (%s); will try to slot into conversation %d",
                        msgidHeader, conversationId);
            }
        }

        // caller can't set system flags other than \Draft, \Sent and \Post
        flags &= ~Flag.FLAGS_SYSTEM | Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME | Flag.BITMASK_POST;
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

        CustomMetadata.CustomMetadataList extended = MetadataCallback.preDelivery(pm);
        if (customData != null) {
            if (extended == null) {
                extended = customData.asList();
            } else {
                extended.addSection(customData);
            }
        }

        Threader threader = pm.getThreader(this);
        String subject = pm.getNormalizedSubject();
        boolean success = false;

        try (final MailboxTransaction t = mailboxWriteTransaction("addMessage", octxt, redoRecorder)) {
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
            if (threader.isEnabled()) {
                boolean isReply = pm.isReply();
                if (conversationId != ID_AUTO_INCREMENT) {
                    try {
                        // fetch the requested conversation
                        //   (we'll ensure that it's receiving new mail after the new message is added to it)
                        conv = getConversationById(conversationId);
                        ZimbraLog.mailbox.debug("fetched explicitly-specified conversation %d", conv.getId());
                    } catch (NoSuchItemException nsie) {
                        if (!isRedo) {
                            ZimbraLog.mailbox.debug("could not find explicitly-specified conversation %d", conversationId);
                            conversationId = ID_AUTO_INCREMENT;
                        }
                    }
                } else if (!isRedo && !isSpam && (isReply || (!isSent && !subject.isEmpty()))) {
                    List<Conversation> matches = threader.lookupConversation();
                    if (matches != null && !matches.isEmpty()) {
                        // file the message into the largest conversation, then later merge any other matching convs
                        Collections.sort(matches, new MailItem.SortSizeDescending());
                        conv = matches.remove(0);
                        mergeConvs = matches;
                    }
                }
            }
            if (conv != null && conv.isTagged(Flag.FlagInfo.MUTED)) {
                // adding a message to a muted conversation marks it muted and read
                unread = false;
                flags |= Flag.BITMASK_MUTED;
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
            msg = Message.create(messageId, folder, convTarget, pm, staged, unread, flags, ntags, dinfo, noICal, iCal, extended, dsId);

            redoRecorder.setMessageId(msg.getId());

            // step 4: create a conversation for the message, if necessary
            if (threader.isEnabled() && convTarget == null) {
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
                        threader.changeThreadingTargets(contents[0], conv);
                    }
                }
            } else {
                // conversation feature turned off
                redoRecorder.setConvFirstMsgId(-1);
            }
            redoRecorder.setConvId(conv != null && !(conv instanceof VirtualConversation) ? conv.getId() : -1);
            // if we're threading by references, associate the new message's reference hashes with its conversation
            if (!isSpam && !isDraft) {
                threader.recordAddedMessage(conv);
            }

            if (conv != null && mergeConvs != null) {
                redoRecorder.setMergedConversations(mergeConvs);
                for (Conversation smaller : mergeConvs) {
                    ZimbraLog.mailbox.info("merging conversation %d for references threading", smaller.getId());
//                    try {
                        conv.merge(smaller);
//                    } catch (ServiceException e) {
//                        if (!e.getCode().equals(MailServiceException.NO_SUCH_MSG)) {
//                            throw e;
//                        }
//                    }
                }
            }

            // conversations may have shifted, so the threader's cached state is now questionable
            threader.reset();

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
            //update the cache so it reflects the new indexId
            cache(msg);
            success = true;
            t.commit();

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

            if (success) {
                // Everything worked.  Update the blob field in ParsedMessage
                // so the next recipient in the multi-recipient case will link
                // to this blob as opposed to saving its own copy.
                dctxt.setFirst(false);
            }
        }

        // step 8: remember the Message-ID header so that we can avoid receiving duplicates
        if (isSent && !isRedo && msgidHeader != null) {
            mSentMessageIDs.put(msgidHeader, msg.getId());
        }

        // step 9: if necessary, execute sent/received callbacks
        if (callbackContext != null) {
            MessageCallback callback = callbacks.get(callbackContext.getCallbackType());
            if (callback != null) {
                callback.execute(msg, callbackContext);
            }
        }
        return msg;
    }

    public List<Conversation> lookupConversation(ParsedMessage pm) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("lookupConversation", null)) {
            List<Conversation> result = pm.getThreader(this).lookupConversation();
            t.commit();
            return result;
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
        mConvHashes.put(hash, Integer.valueOf(conv.getId()));
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
     * Saves draft.
     *
     * @param autoSendTime time at which the draft needs to be auto-sent. Note that this method does not schedule
     *                     the task for auto-sending the draft. It just persists this time for tracking purposes.
     * @see com.zimbra.cs.service.mail.SaveDraft#handle(com.zimbra.common.soap.Element, java.util.Map)
     */
    public Message saveDraft(OperationContext octxt, ParsedMessage pm, int id, String origId, String replyType,
            String identityId, String accountId, long autoSendTime)
    throws IOException, ServiceException {
        Message.DraftInfo dinfo = null;
        if ((replyType != null && origId != null) || identityId != null || accountId != null || autoSendTime != 0) {
            dinfo = new Message.DraftInfo(replyType, origId, identityId, accountId, autoSendTime);
        }

        if (id == ID_AUTO_INCREMENT) {
            // special-case saving a new draft
            return addMessage(octxt, pm, ID_FOLDER_DRAFTS, true, Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME,
                              null, ID_AUTO_INCREMENT, ":API:", dinfo, null, null, null, null);
        }

        // write the draft content directly to the mailbox's blob staging area
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged;
        InputStream is = pm.getRawInputStream();
        try {
            staged = sm.stage(is, this);
        } finally {
            ByteUtil.closeStream(is);
        }

        String digest = staged.getDigest();
        int size = (int) staged.getSize();

        SaveDraft redoRecorder = new SaveDraft(mId, id, digest, size);
        InputStream redoStream = null;

        try (final MailboxTransaction t = mailboxWriteTransaction("saveDraft", octxt, redoRecorder)) {
            SaveDraft redoPlayer = (SaveDraft) currentChange().getRedoPlayer();

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

            t.commit();

            try {
                Notification.getInstance().interceptIfNecessary(this, pm.getMimeMessage(), "save draft", msg.getFolder());
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("Unable to send lawful intercept message.", e);
            }

            return msg;
        } finally {
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

        try (final MailboxTransaction t = mailboxWriteTransaction("updateInvitePartStat", octxt, redoRecorder)) {
            CalendarItem calItem = getCalendarItemById(calItemId);
            Account acct = getAccount();
            markItemModified(calItem, Change.INVITE);
            calItem.modifyPartStat(acct, recurId, cnStr, addressStr, cutypeStr, roleStr, partStatStr, rsvp, seqNo, dtStamp);
            t.commit();
        }
    }

    /**
     * @return the list of new IMAP UIDs corresponding to the specified items
     */
    @Override
    public List<Integer> resetImapUid(OpContext octxt, List<Integer> itemIds) throws ServiceException {
        SetImapUid redoRecorder = new SetImapUid(mId, itemIds);

        List<Integer> newIds = new ArrayList<Integer>();
        try (final MailboxTransaction t = mailboxWriteTransaction("resetImapUid",
                 OperationContext.asOperationContext(octxt), redoRecorder)) {
            SetImapUid redoPlayer = (SetImapUid) currentChange().getRedoPlayer();

            for (int id : itemIds) {
                MailItem item = getItemById(id, MailItem.Type.UNKNOWN);
                int imapId = redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapUid(id);
                item.setImapUid(getNextItemId(imapId));
                redoRecorder.setImapUid(item.getId(), item.getImapUid());
                newIds.add(item.getImapUid());
            }
            t.commit();
            return newIds;
        }
    }

    /**
     * Resets all INDEX_ID in MAIL_ITEM table. The caller must hold the mailbox lock.
     */
    void resetIndex(MailboxLock l) throws ServiceException {
        assert (l.isWriteLockedByCurrentThread());

        try (final MailboxTransaction t = mailboxReadTransaction("resetIndex", null)) {
            DbMailItem.resetIndexId(getOperationConnection(), this);
            t.commit();
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

        try (final MailboxTransaction t = mailboxWriteTransaction("setColor", octxt, redoRecorder)) {

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items) {
                if (!checkItemChangeID(item)) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }
            }
            for (MailItem item : items) {
                item.setColor(color);
            }
            t.commit();
        }
    }

    public void setCustomData(OperationContext octxt, int itemId, MailItem.Type type, CustomMetadata custom)
    throws ServiceException {
        String key = custom.getSectionKey();
        if (MetadataCallback.isSectionRegistered(key)) {
            throw ServiceException.PERM_DENIED("custom metadata section '" + key + "' may only be calculated, not set");
        }
        SetCustomData redoRecorder = new SetCustomData(mId, itemId, type, custom);

        try (final MailboxTransaction t = mailboxWriteTransaction("setCustomData", octxt, redoRecorder)) {

            MailItem item = checkAccess(getItemById(itemId, type));
            if (!checkItemChangeID(item)) {
                throw MailServiceException.MODIFY_CONFLICT();
            }
            item.setCustomData(custom);
            t.commit();
        }
    }

    public void setDate(OperationContext octxt, int itemId, MailItem.Type type, long date) throws ServiceException {
        DateItem redoRecorder = new DateItem(mId, itemId, type, date);

        try (final MailboxTransaction t = mailboxWriteTransaction("setDate", octxt, redoRecorder)) {

            MailItem item = getItemById(itemId, type);
            if (!checkItemChangeID(item)) {
                throw MailServiceException.MODIFY_CONFLICT();
            }

            item.setDate(date);
            t.commit();
        }
    }

    @Override
    public void flagFolderAsSubscribed(OpContext ctxt, FolderStore folder) throws ServiceException {
        alterTag((OperationContext)ctxt, ((Folder)folder).getId(), MailItem.Type.FOLDER,
                Flag.FlagInfo.SUBSCRIBED, true, null);
    }

    @Override
    public void flagFolderAsUnsubscribed(OpContext ctxt, FolderStore folder) throws ServiceException {
        alterTag((OperationContext)ctxt, ((Folder)folder).getId(), MailItem.Type.FOLDER,
                Flag.FlagInfo.SUBSCRIBED, false, null);
    }

    @Override
    public void flagItemAsRead(OpContext octxt, ItemIdentifier itemId, MailItemType type)
    throws ServiceException {
        alterTag(OperationContext.asOperationContext(octxt), itemId.id, MailItem.Type.fromCommon(type),
                Flag.FlagInfo.UNREAD, false, null);
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

        try (final MailboxTransaction t = mailboxWriteTransaction("alterTag", octxt, redoRecorder)) {
            setOperationTargetConstraint(tcon);

            alterTag(itemIds, type, finfo.toFlag(this), addTag);
            t.commit();
        }
    }

    public void alterTag(OperationContext octxt, int itemId, MailItem.Type type, String tagName,
            boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        alterTag(octxt, new int[] { itemId }, type, tagName, addTag, tcon);
    }

    @Override
    public void alterTag(OpContext octxt, Collection<ItemIdentifier> ids, String tagName, boolean addTag)
    throws ServiceException {
        alterTag(OperationContext.asOperationContext(octxt),
                ArrayUtil.toIntArray(ItemIdentifier.toIds(ids)), MailItem.Type.UNKNOWN, tagName, addTag, null);
    }

    public void alterTag(OperationContext octxt, int[] itemIds, MailItem.Type type, String tagName,
            boolean addTag, TargetConstraint tcon)
    throws ServiceException {
        AlterItemTag redoRecorder = new AlterItemTag(mId, itemIds, type, tagName, addTag, tcon);

        try (final MailboxTransaction t = mailboxWriteTransaction("alterTag", octxt, redoRecorder)) {
            setOperationTargetConstraint(tcon);

            Tag tag;
            try {
                tag = getTagByName(tagName);
            } catch (NoSuchItemException nsie) {
                if (tagName.startsWith(Tag.FLAG_NAME_PREFIX)) {
                    throw nsie;
                }
                Tag.NormalizedTags ntags = new NormalizedTags(this, new String[] { tagName }, addTag, true);
                if (ntags.getTags().length == 0) {
                    t.commit();
                    return;
                }
                tag = getTagByName(ntags.getTags()[0]);
            }

            alterTag(itemIds, type, tag, addTag);
            t.commit();
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
            if (item == null) {
                continue;
            }

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

    @Override
    public void setTags(OpContext octxt, Collection<ItemIdentifier> itemIds, int flags, Collection<String> tags)
    throws ServiceException {
        List<Integer> ids = ItemIdentifier.toIds(itemIds);
        setTags(OperationContext.asOperationContext(octxt), ArrayUtil.toIntArray(ids), MailItem.Type.UNKNOWN,
                flags, tags.toArray(new String[tags.size()]), null);
    }

    public void setTags(OperationContext octxt, int[] itemIds, MailItem.Type type, int flags, String[] tags,
            TargetConstraint tcon)
    throws ServiceException {
        if (flags == MailItem.FLAG_UNCHANGED && tags == MailItem.TAG_UNCHANGED) {
            return;
        }

        SetItemTags redoRecorder = new SetItemTags(mId, itemIds, type, flags, tags, tcon);

        try (final MailboxTransaction t = mailboxWriteTransaction("setTags", octxt, redoRecorder)) {
            setOperationTargetConstraint(tcon);

            MailItem[] items = getItemById(itemIds, type);
            for (MailItem item : items) {
                checkItemChangeID(item);
            }
            Flag unreadFlag = getFlagById(Flag.ID_UNREAD);

            Tag.NormalizedTags ntags = tags == MailItem.TAG_UNCHANGED ? null : new Tag.NormalizedTags(this, tags, true, true);

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

            t.commit();
        }
    }

    /**
     * Recovers items from dumpster.
     */
    public List<MailItem> recover(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
            throws ServiceException {
        RecoverItem redoRecorder = new RecoverItem(mId, type, folderId);
        try (final MailboxTransaction t = mailboxWriteTransaction("recover[]", octxt, redoRecorder)) {
            List<MailItem> result = copyInternal(octxt, itemIds, type, folderId, true);
            deleteFromDumpster(itemIds);
            t.commit();
            return result;
        }
    }

    public MailItem copy(OperationContext octxt, int itemId, MailItem.Type type, int folderId)
    throws ServiceException {
        return copy(octxt, new int[] { itemId }, type, folderId).get(0);
    }

    public List<MailItem> copy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
    throws ServiceException {
        CopyItem redoRecorder = new CopyItem(mId, type, folderId);
        try (final MailboxTransaction t = mailboxWriteTransaction("copy[]", octxt, redoRecorder)) {
            List<MailItem> result = copyInternal(octxt, itemIds, type, folderId, false);
            t.commit();
            return result;
        }
    }

    private List<MailItem> copyInternal(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId,
            boolean fromDumpster)
    throws ServiceException {
        CopyItem redoRecorder = (CopyItem) currentChange().getRedoRecorder();
        try {
            if (fromDumpster) {
                Folder trash = getFolderById(ID_FOLDER_TRASH);
                if (!trash.canAccess(ACL.RIGHT_READ)) {
                    throw ServiceException.PERM_DENIED("dumpster access denied");
                }
            }
            CopyItem redoPlayer = (CopyItem) currentChange().getRedoPlayer();

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
                        String uuid;
                        if (redoPlayer != null) {
                            uuid = redoPlayer.getDestUuid(original.getId());
                        } else if (fromDumpster) {
                            // Keep the same uuid if recovering from dumpster.
                            uuid = original.getUuid();
                        } else {
                            uuid = UUIDUtil.generateUUID();
                        }
                        Message msg = (Message) original.copy(folder, newId, uuid, null);
                        msgs.add(msg);
                        redoRecorder.setDest(original.getId(), newId, msg.getUuid());
                    }
                    if (msgs.isEmpty()) {
                        throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
                    } else if (msgs.size() == 1) {
                        copy = msgs.get(0).getParent();
                    } else {
                        int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(conv.getId()));
                        copy = Conversation.create(this, newId, msgs.toArray(new Message[msgs.size()]));
                        redoRecorder.setDest(conv.getId(), newId, copy.getUuid());
                    }
                } else {
                    int newId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getDestId(item.getId()));
                    String uuid;
                    if (redoPlayer != null) {
                        uuid = redoPlayer.getDestUuid(item.getId());
                    } else if (fromDumpster) {
                        // Keep the same uuid if recovering from dumpster.
                        uuid = item.getUuid();
                    } else {
                        uuid = UUIDUtil.generateUUID();
                    }
                    int parentId = item.getParentId();
                    MailItem parent = null;
                    if (parentId > 0) {
                        try {
                            parent = getItemById(parentId, MailItem.Type.UNKNOWN);
                        } catch (MailServiceException.NoSuchItemException e) {
                            // ignore
                        }
                    }
                    copy = item.copy(folder, newId, uuid, parent);
                    if (fromDumpster) {
                        for (MailItem.UnderlyingData data : DbMailItem.getByParent(item, SortBy.DATE_DESC, -1, true)) {
                            MailItem child = getItem(data);
                            Folder destination = (child.getType() == MailItem.Type.COMMENT) ? getFolderById(ID_FOLDER_COMMENTS) : folder;
                            child.copy(destination, getNextItemId(ID_AUTO_INCREMENT), child.getUuid(), copy);
                        }
                    }
                    redoRecorder.setDest(item.getId(), newId, copy.getUuid());
                }

                result.add(copy);
            }
            return result;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while copying items", e);
        }
    }

    public List<MailItem> imapCopy(OperationContext octxt, int[] itemIds, MailItem.Type type, int folderId)
    throws ServiceException {
        // this is an IMAP command, so we'd better be tracking IMAP changes by now...
        beginTrackingImap(); //TODO: there is likely no way this mailbox is not tracking IMAP already if we are here

        for (int id : itemIds) {
            if (id <= 0) {
                throw MailItem.noSuchItem(id, type);
            }
        }

        ImapCopyItem redoRecorder = new ImapCopyItem(mId, type, folderId);

        try (final MailboxTransaction t = mailboxWriteTransaction("icopy", octxt, redoRecorder)) {
            ImapCopyItem redoPlayer = (ImapCopyItem) currentChange().getRedoPlayer();

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
                String newUuid = redoPlayer == null ? UUIDUtil.generateUUID() : redoPlayer.getDestUuid(srcId);

                trainSpamFilter(octxt, item, target, "imap copy");

                MailItem copy = item.icopy(target, newId, newUuid);
                result.add(copy);
                redoRecorder.setDest(srcId, newId, newUuid);
            }

            t.commit();
            return result;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while copying items for IMAP", e);
        }
    }

    private <T extends MailItem> T trainSpamFilter(OperationContext octxt, T item, Folder target, String opDescription) {
        if (currentChange().getRedoPlayer() != null) { // don't re-train filter
                                                       // on replayed operation
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
        try (final MailboxLock l = getWriteLockAndLockIt()) {
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
            TargetConstraint tcon)
    throws ServiceException {
        MoveItem redoRecorder = new MoveItem(mId, itemIds, type, targetId, tcon);

        try (final MailboxTransaction t = mailboxWriteTransaction("move", octxt, redoRecorder)) {
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
                MoveItem redoPlayer = (MoveItem) currentChange().getRedoPlayer();
                redoRecorder.setUIDNEXT(getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getUIDNEXT()));
                target.updateUIDNEXT();
            }
            t.commit();
        }
    }

    public void rename(OperationContext octxt, int id, MailItem.Type type, String name, int folderId)
    throws ServiceException {
        rename(octxt, id, type, name, folderId, null);
    }

    public void rename(OperationContext octxt, int id, MailItem.Type type, String name, int folderId, Long date)
    throws ServiceException {
        if (name != null && name.startsWith("/")) {
            rename(octxt, id, type, name);
            return;
        }

        name = StringUtil.stripControlCharacters(name);
        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("cannot set name to empty string", null);
        }

        RenameItem redoRecorder = new RenameItem(mId, id, type, name, folderId, date);

        try (final MailboxTransaction t = mailboxWriteTransaction("rename", octxt, redoRecorder)) {
            MailItem item = getItemById(id, type);
            checkItemChangeID(item);
            if (folderId <= 0) {
                folderId = item.getFolderId();
            }
            Folder target = getFolderById(folderId);
            trainSpamFilter(octxt, item, target, "rename");

            String oldName = item.getName();
            item.rename(name, target);

            if (date != null) {
                item.setDate(date);
            }

            if (item instanceof Tag) {
                try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
                    mTagCache.updateName(oldName, name);
                }
            }
            t.commit();
        }
    }

    public void rename(OperationContext octxt, int id, MailItem.Type type, String path) throws ServiceException {
        if (path == null || !path.startsWith("/")) {
            rename(octxt, id, type, path, ID_AUTO_INCREMENT);
            return;
        }

        RenameItemPath redoRecorder = new RenameItemPath(mId, id, type, path);

        try (final MailboxTransaction t = mailboxWriteTransaction("renameFolderPath", octxt, redoRecorder)) {
            RenameItemPath redoPlayer = (RenameItemPath) currentChange().getRedoPlayer();

            MailItem item = getItemById(id, type);
            Folder parent;
            checkItemChangeID(item);

            String[] parts = path.substring(1).split("/");
            if (parts.length == 0) {
                throw MailServiceException.ALREADY_EXISTS(path);
            }
            int[] recorderParentIds = new int[parts.length - 1];
            String[] recorderParentUuids = new String[parts.length - 1];
            int[] playerParentIds = redoPlayer == null ? null : redoPlayer.getParentIds();
            String[] playerParentUuids = redoPlayer == null ? null : redoPlayer.getParentUuids();
            if (playerParentIds != null && playerParentIds.length != recorderParentIds.length) {
                throw ServiceException.FAILURE("incorrect number of path segment ids in redo player", null);
            }
            if (playerParentUuids != null && playerParentUuids.length != recorderParentUuids.length) {
                throw ServiceException.FAILURE("incorrect number of path segment uuids in redo player", null);
            }
            parent = getFolderById(ID_FOLDER_USER_ROOT);
            for (int i = 0; i < parts.length - 1; i++) {
                String name = MailItem.validateItemName(parts[i]);
                int subfolderId = playerParentIds == null ? ID_AUTO_INCREMENT : playerParentIds[i];
                String subfolderUuid = playerParentUuids == null ? UUIDUtil.generateUUID() : playerParentUuids[i];
                Folder subfolder = parent.findSubfolder(name);
                if (subfolder == null) {
                    subfolder = Folder.create(getNextItemId(subfolderId), subfolderUuid, this, parent, name, (byte) 0, MailItem.Type.UNKNOWN, 0, null, null, null, null);
                } else if (subfolderId != ID_AUTO_INCREMENT && subfolderId != subfolder.getId()) {
                    throw ServiceException.FAILURE("parent folder id changed since operation was recorded", null);
                } else if (!subfolder.getName().equals(name) && subfolder.isMutable()) {
                    // Same folder name, different case.
                    subfolder.rename(name, parent);
                }
                recorderParentIds[i] = subfolder.getId();
                recorderParentUuids[i] = subfolder.getUuid();
                parent = subfolder;
            }
            redoRecorder.setParentIdsAndUuids(recorderParentIds, recorderParentUuids);

            trainSpamFilter(octxt, item, parent, "rename");

            String name = parts[parts.length - 1];
            item.rename(name, parent);

            t.commit();
        }
    }

    @Override
    public void renameFolder(OpContext octxt, FolderStore folder, String path) throws ServiceException {
        rename((OperationContext)octxt, ((Folder)folder).getId(), MailItem.Type.FOLDER, path);
    }

    /**
     * Deletes the <tt>MailItem</tt> with the given id.  Does nothing
     * if the <tt>MailItem</tt> doesn't exist.
     */
    public void delete(OperationContext octxt, int itemId, MailItem.Type type) throws ServiceException {
        delete(octxt, new int[] { itemId }, type, null);
    }

    @Override
    public void deleteFolder(OpContext octxt, String itemId) throws ServiceException {
        delete((OperationContext) octxt, Integer.parseInt(itemId), MailItem.Type.FOLDER);
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

    /**
     * Delete the <tt>MailItem</tt>s with the given ids.  If there is no <tt>MailItem</tt> for a given id, that id is
     * ignored.  If the id maps to an existing <tt>MailItem</tt> of an incompatible type, however, an error is thrown.
     *
     * @param octxt operation context or {@code null}
     * @param itemIds item ids
     * @param type item type or {@link MailItem.Type#UNKNOWN}
     * @param tcon target constraint or {@code null}
     * @param useEmptyForFolders empty folder {@code true} or {@code false}
     * @param nonExistingItems object of {@link ArrayList} or {@code null}
     */
    private void delete(OperationContext octxt, int[] itemIds, MailItem.Type type, TargetConstraint tcon,
            boolean useEmptyForFolders, List<Integer> nonExistingItems)
    throws ServiceException {
        DeleteItem redoRecorder = new DeleteItem(mId, itemIds, type, tcon);

        List<Integer> folderIds = Lists.newArrayList();
        try (final MailboxTransaction t = mailboxWriteTransaction("delete", octxt, redoRecorder)) {
            setOperationTargetConstraint(tcon);

            for (int id : itemIds) {
                if (id == ID_AUTO_INCREMENT) {
                    continue;
                }
                MailItem item;
                try {
                    item = getItemById(id, MailItem.Type.UNKNOWN);
                } catch (NoSuchItemException nsie) {
                    if (nonExistingItems != null) {
                        nonExistingItems.add(id);
                    }
                    // trying to delete nonexistent things is A-OK!
                    continue;
                }

                // however, trying to delete messages and passing in a folder ID is not OK
                if (!MailItem.isAcceptableType(type, item.getType())) {
                    throw MailItem.noSuchItem(id, type);
                } else if (!checkItemChangeID(item) && item instanceof Tag) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }
                if (useEmptyForFolders && MailItem.Type.FOLDER.equals(item.getType())) {
                    folderIds.add(id); // removed later to allow batching delete of contents in there own transactions
                } else {
                    // delete the item, but don't write the tombstone until we're finished...
                    item.delete(false);
                }
            }

            // deletes have already been collected, so fetch the tombstones and write once
            TypedIdList tombstones = collectPendingTombstones();
            if (tombstones != null && !tombstones.isEmpty()) {
                DbMailItem.writeTombstones(this, tombstones);
            }

            t.commit();
        }
        for (Integer folderId : folderIds) {
            emptyFolder(octxt, folderId, true /* removeTopLevelFolder */, true /* removeSubfolders */, tcon);
        }
    }

    /**
     * Delete the <tt>MailItem</tt>s with the given ids.  If there is no <tt>MailItem</tt> for a given id, that id is
     * ignored.  If the id maps to an existing <tt>MailItem</tt> of an incompatible type, however, an error is thrown.
     *
     * @param octxt operation context or {@code null}
     * @param itemIds item ids
     * @param type item type or {@link MailItem.Type#UNKNOWN}
     * @param tcon target constraint or {@code null}
     */
    public void delete(OperationContext octxt, int[] itemIds, MailItem.Type type, TargetConstraint tcon)
    throws ServiceException {
        delete(octxt, itemIds, type, tcon, true /* useEmptyForFolders */, null);
    }

    /**
     * Delete the <tt>MailItem</tt>s with the given ids.  If there is no <tt>MailItem</tt> for a given id, that id is
     * ignored.  If the id maps to an existing <tt>MailItem</tt> of an incompatible type, however, an error is thrown.
     *
     * @param octxt operation context or {@code null}
     * @param itemIds item ids
     * @param type item type or {@link MailItem.Type#UNKNOWN}
     * @param tcon target constraint or {@code null}
     * @param nonExistingItems object of {@link ArrayList} or {@code null}
     */
    public void delete(OperationContext octxt, int[] itemIds, MailItem.Type type, TargetConstraint tcon, List<Integer> nonExistingItems)
    throws ServiceException {
        delete(octxt, itemIds, type, tcon, true /* useEmptyForFolders */, nonExistingItems);
    }

    /**
     * Delete <tt>MailItem</tt>s with given ids.  If there is no <tt>MailItem</tt> for a given id, that id is ignored.
     *
     * @param octxt operation context or {@code null}
     * @param itemIds item ids
     * @param nonExistingItems If not null, This gets populated with the item IDs of nonExisting items
     */
    @Override
    public void delete(OpContext octxt, List<Integer> itemIds, List<Integer> nonExistingItems) throws ServiceException {
        delete((OperationContext)octxt, ArrayUtil.toIntArray(itemIds), MailItem.Type.UNKNOWN, null, nonExistingItems);
    }

    TypedIdList collectPendingTombstones() {
        if (!isTrackingSync() || currentChange().deletes == null) {
            return null;
        }
        return new TypedIdList(currentChange().deletes.itemIds);
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
        try (final MailboxTransaction t = mailboxWriteTransaction("deleteFromDumpster[]", octxt, redoRecorder)) {
            int numDeleted = deleteFromDumpster(itemIds);
            t.commit();
            return numDeleted;
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
            try (final MailboxLock l = getWriteLockAndLockIt()) {
                Set<Integer> itemIds = null;
                // XXX: should we be in a txn and using getOperationConnection() instead?
                DbConnection conn = DbPool.getConnection(this);
                try {
                    itemIds = DbMailItem.getIds(this, conn, params, true);
                } finally {
                    conn.closeQuietly();
                }

                if (itemIds.isEmpty()) {
                    break;
                }

                numDeleted += deleteFromDumpster(octxt, ArrayUtil.toIntArray(itemIds));
            }
        }
        return numDeleted;
    }

    private int purgeDumpster(long olderThanMillis, int maxItems) throws ServiceException {
        QueryParams params = new QueryParams();
        params.setChangeDateBefore((int) (olderThanMillis / 1000)).setRowLimit(maxItems);

        Set<Integer> itemIds = DbMailItem.getIds(this, getOperationConnection(), params, true);
        if (!itemIds.isEmpty()) {
            return deleteFromDumpster(ArrayUtil.toIntArray(itemIds));
        } else {
            return 0;
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

        try (final MailboxTransaction t = mailboxWriteTransaction("createTag", octxt, redoRecorder)) {
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }

            CreateTag redoPlayer = (CreateTag) currentChange().getRedoPlayer();
            int tagId = redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getTagId();
            Tag tag = createTagInternal(tagId, name, color, true);
            redoRecorder.setTagId(tag.getId());
            t.commit();
            return tag;
        }
    }

    Tag createTagInternal(int tagId, String name, Color color, boolean listed) throws ServiceException {
        try {
            Tag tag = getTagByName(name);
            if (!listed) {
                // want an implicitly-created tag but there's already a listed tag, so just return it unchanged
                return tag;
            } else if (tag.isListed()) {
                // can't have two listed tags with the same name
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
            // no conflict, so just create the new tag as requested
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

        try (final MailboxTransaction t = mailboxWriteTransaction("createNote", octxt, redoRecorder)) {
            CreateNote redoPlayer = (CreateNote) currentChange().getRedoPlayer();

            int noteId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getNoteId());
            Note note = Note.create(noteId, getFolderById(folderId), content, location, color, null);
            redoRecorder.setNoteId(noteId);

            index.add(note);
            t.commit();
            return note;
        }
    }

    public void editNote(OperationContext octxt, int noteId, String content) throws ServiceException {
        content = StringUtil.stripControlCharacters(content);
        if (Strings.isNullOrEmpty(content)) {
            throw ServiceException.INVALID_REQUEST("note content may not be empty", null);
        }
        EditNote redoRecorder = new EditNote(mId, noteId, content);

        try (final MailboxTransaction t = mailboxWriteTransaction("editNote", octxt, redoRecorder)) {
            Note note = getNoteById(noteId);
            checkItemChangeID(note);

            note.setContent(content);
            index.add(note);

            t.commit();
        }
    }

    public void repositionNote(OperationContext octxt, int noteId, Rectangle location) throws ServiceException {
        Preconditions.checkNotNull(location, "must specify note bounds");
        RepositionNote redoRecorder = new RepositionNote(mId, noteId, location);

        try (final MailboxTransaction t = mailboxWriteTransaction("repositionNote", octxt, redoRecorder)) {
            Note note = getNoteById(noteId);
            checkItemChangeID(note);

            note.reposition(location);
            t.commit();
        }
    }

    CalendarItem createCalendarItem(int folderId, int flags, Tag.NormalizedTags ntags, String uid,
                                    ParsedMessage pm, Invite invite, CustomMetadata custom)
    throws ServiceException {
        // FIXME: assuming that we're in the middle of a AddInvite op
        CreateCalendarItemPlayer redoPlayer = (CreateCalendarItemPlayer) currentChange().getRedoPlayer();
        CreateCalendarItemRecorder redoRecorder = (CreateCalendarItemRecorder) currentChange().getRedoRecorder();

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
                staged = sm.stage(is = pc.getContentStream(), (int) pc.getSize(), this);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("could not save contact blob", ioe);
            } finally {
                ByteUtil.closeStream(is);
            }
        }

        CreateContact redoRecorder = new CreateContact(mId, folderId, pc, tags);

        try (final MailboxLock l = getWriteLockAndLockIt()) {
            final MailboxTransaction t = mailboxWriteTransaction("createContact", octxt, redoRecorder);
            try {
                CreateContact redoPlayer = (CreateContact) currentChange().getRedoPlayer();
                boolean isRedo = redoPlayer != null;

                Tag.NormalizedTags ntags = new Tag.NormalizedTags(this, tags);

                int contactId = getNextItemId(isRedo ? redoPlayer.getContactId() : ID_AUTO_INCREMENT);

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
                redoRecorder.setContactId(contactId);

                index.add(con);

                t.commit();
                return con;
            } finally {
                t.close();
                sm.quietDelete(staged);
            }
        }
    }

    public void modifyContact(OperationContext octxt, int contactId, ParsedContact pc) throws ServiceException {
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged = null;
        if (pc.hasAttachment()) {
            // write the contact content directly to the mailbox's blob staging area
            InputStream is = null;
            try {
                staged = sm.stage(is = pc.getContentStream(), pc.getSize(), this);
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("could not save contact blob", ioe);
            } finally {
                ByteUtil.closeStream(is);
            }
        }

        ModifyContact redoRecorder = new ModifyContact(mId, contactId, pc);
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            final MailboxTransaction t = mailboxWriteTransaction("modifyContact", octxt, redoRecorder);
            try {
                Contact con = getContactById(contactId);
                if (!checkItemChangeID(con)) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }

                try {
                    // setContent() calls reanalyze(), which also updates the contact fields even when there is no blob
                    con.setContent(staged, pc);
                } catch (IOException ioe) {
                    throw ServiceException.FAILURE("could not save contact blob", ioe);
                }

                index.add(con);
                t.commit();
            } finally {
                t.close();
                sm.quietDelete(staged);
            }
        }
    }

    /**
     * Creates new contacts in AUTO_CONTACTS folder.
     * Note: Its upto the caller to check whether the email addresses in the list pre-exist.
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
        List<Contact> result = new ArrayList<Contact>(addrs.size());
        String locale = octxt != null && octxt.getAuthenticatedUser() != null ? octxt.getAuthenticatedUser().getPrefLocale() : null;
        boolean nameFormatLastFirst = false;
        if (locale != null && locale.equals("ja")) {
            nameFormatLastFirst = true;
        }
        for (InternetAddress addr : addrs) {
            ZimbraLog.mailbox.debug("Auto-adding new contact addr=%s", addr);
            try {
                result.add(createContact(octxt, new ParsedContact(new ParsedAddress(addr, nameFormatLastFirst).getAttributes()),
                        Mailbox.ID_FOLDER_AUTO_CONTACTS, null));
            } catch (ServiceException e) {
                if (e.getCode().equals(MailServiceException.TOO_MANY_CONTACTS)) {
                    ZimbraLog.mailbox.warn("Aborting contact addition, " +
                            "Failed to auto-add contact addr=%s", addr, e);
                    return result;
                }
                ZimbraLog.mailbox.warn("Failed to auto-add contact addr=%s", addr, e);
            }
        }
        return result;
    }

    /**
     * Returns a list of contacts don't exist in any contacts folders.
     *
     * @param addrs email addresses
     * @return addresses doesn't exist
     * @throws ServiceException
     */
    public Collection<Address> newContactAddrs(Collection<Address> addrs) throws ServiceException {
        if (addrs.isEmpty()) {
            return Collections.emptySet();
        }

        if (isWriteLockedByCurrentThread()) {
            //TODO can't search while holding the mailbox lock
            ZimbraLog.mailbox.warn("Unable to auto-add contact while holding Mailbox lock");
            return Collections.emptySet();
        }
        Set<Address> newAddrs = new HashSet<Address>();
        for (Address addr : addrs) {
            if (addr instanceof javax.mail.internet.InternetAddress) {
                javax.mail.internet.InternetAddress iaddr = (javax.mail.internet.InternetAddress) addr;
                try {
                    if (!Strings.isNullOrEmpty(iaddr.getAddress()) &&
                            !index.existsInContacts(Collections.singleton(
                                    new com.zimbra.common.mime.InternetAddress(
                                    iaddr.getPersonal(), iaddr.getAddress())))) {
                        newAddrs.add(addr);
                    }
                } catch (IOException | ServiceException e) {
                    //bug 86938: a corrupt index should not interrupt message delivery
                    ZimbraLog.search.error("error searching index for contacts");
                }
            }
        }
        return newAddrs;
    }

    @Deprecated
    public Folder createFolder(OperationContext octxt, String name, int parentId, MailItem.Type defaultView, int flags,
            byte color, String url)
    throws ServiceException {
        return createFolder(octxt, name, parentId, (byte) 0, defaultView, flags, color, url);
    }

    @Deprecated
    public Folder createFolder(OperationContext octxt, String name, int parentId, byte attrs, MailItem.Type defaultView,
            int flags, byte color, String url)
    throws ServiceException {
        return createFolder(octxt, name, parentId, attrs, defaultView, flags, new Color(color), url);
    }

    @Deprecated
    public Folder createFolder(OperationContext octxt, String name, int parentId, byte attrs, MailItem.Type defaultView,
            int flags, Color color, String url)
    throws ServiceException {
        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setAttributes(attrs).setDefaultView(defaultView).setFlags(flags).setColor(color).setUrl(url);
        return createFolder(octxt, name, parentId, fopt);
    }

    public Folder createFolder(OperationContext octxt, String name, int parentId, Folder.FolderOptions fopt) throws ServiceException {
        CreateFolder redoRecorder = new CreateFolder(mId, name, parentId, fopt);

        try (final MailboxTransaction t = mailboxWriteTransaction("createFolder", octxt, redoRecorder)) {
            CreateFolder redoPlayer = (CreateFolder) currentChange().getRedoPlayer();

            int folderId;
            String uuid;
            if (redoPlayer == null) {
                folderId = getNextItemId(ID_AUTO_INCREMENT);
                uuid = Strings.emptyToNull(fopt.getUuid()) == null ? UUIDUtil.generateUUID() : fopt.getUuid();
            } else {
                folderId = getNextItemId(redoPlayer.getFolderId());
                uuid = redoPlayer.getFolderUuid();
            }

            Folder folder = Folder.create(folderId, uuid, this, getFolderById(parentId), name, fopt.getAttributes(),
                    fopt.getDefaultView(), fopt.getFlags(), fopt.getColor(), fopt.getDate(), fopt.getUrl(), fopt.getCustomMetadata());

            redoRecorder.setFolderIdAndUuid(folder.getId(), folder.getUuid());
            t.commit();
            updateRssDataSource(folder);
            return folder;
        }
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
    public Folder createFolder(OperationContext octxt, String path, Folder.FolderOptions fopt)
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
        CreateFolderPath redoRecorder = new CreateFolderPath(mId, path, fopt);

        try (final MailboxTransaction t = mailboxWriteTransaction("createFolderPath", octxt, redoRecorder)) {
            CreateFolderPath redoPlayer = (CreateFolderPath) currentChange().getRedoPlayer();

            String[] parts = path.substring(1).split("/");
            if (parts.length == 0) {
                throw MailServiceException.ALREADY_EXISTS(path);
            }

            int[] recorderFolderIds = new int[parts.length];
            String[] recorderFolderUuids = new String[parts.length];
            int[] playerFolderIds = redoPlayer == null ? null : redoPlayer.getFolderIds();
            String[] playerFolderUuids = redoPlayer == null ? null : redoPlayer.getFolderUuids();
            if (playerFolderIds != null && playerFolderIds.length != recorderFolderIds.length) {
                throw ServiceException.FAILURE("incorrect number of path segment ids in redo player", null);
            }
            if (playerFolderUuids != null && playerFolderUuids.length != recorderFolderUuids.length) {
                throw ServiceException.FAILURE("incorrect number of path segment uuids in redo player", null);
            }

            Folder folder = getFolderById(ID_FOLDER_USER_ROOT);
            for (int i = 0; i < parts.length; i++) {
                boolean last = i == parts.length - 1;
                int folderId = playerFolderIds == null ? ID_AUTO_INCREMENT : playerFolderIds[i];
                String folderUuid = playerFolderUuids == null ? UUIDUtil.generateUUID() : playerFolderUuids[i];

                Folder subfolder = folder.findSubfolder(parts[i]);
                if (subfolder == null) {
                    subfolder = Folder.create(getNextItemId(folderId), folderUuid, this, folder, parts[i],
                            fopt.getAttributes(), last ? fopt.getDefaultView() : MailItem.Type.UNKNOWN,
                            fopt.getFlags(), fopt.getColor(), fopt.getDate(), last ? fopt.getUrl() : null,
                            fopt.getCustomMetadata());
                } else if (folderId != ID_AUTO_INCREMENT && folderId != subfolder.getId()) {
                    throw ServiceException.FAILURE("parent folder id changed since operation was recorded", null);
                } else if (last) {
                    throw MailServiceException.ALREADY_EXISTS(path);
                }

                recorderFolderIds[i] = subfolder.getId();
                recorderFolderUuids[i] = subfolder.getUuid();
                folder = subfolder;
            }
            redoRecorder.setFolderIdsAndUuids(recorderFolderIds, recorderFolderUuids);

            t.commit();
            return folder;
        }
    }

    @Override
    public void createFolderForMsgs(OpContext octxt, String path) throws ServiceException {
        createFolder((OperationContext)octxt, path, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
    }

    //for offline override to filter flags
    public String getItemFlagString(MailItem mi) {
        return mi.getFlagString();
    }

    /**
     * @param perms permission mask ("rwid")
     * @param args extra args
     */
    @Override
    public void modifyFolderGrant(OpContext ctxt, FolderStore folder,
            com.zimbra.common.mailbox.GrantGranteeType granteeType, String granteeId, String perms, String args)
    throws ServiceException {
        grantAccess((OperationContext)ctxt, folder.getFolderIdInOwnerMailbox(), granteeId, granteeType.asByte(),
                     ACL.stringToRights(perms), null);
    }

    public ACL.Grant grantAccess(OperationContext octxt, int itemId, String grantee, byte granteeType, short rights,
            String args)
    throws ServiceException {
         return grantAccess(octxt, itemId, grantee, granteeType, rights, args, 0);
    }

    public ACL.Grant grantAccess(OperationContext octxt, int itemId, String grantee, byte granteeType, short rights,
            String args, long expiry)
    throws ServiceException {
        // Only internal users and groups can be granted the admin right.
        if ((rights & ACL.RIGHT_ADMIN) != 0) {
            if (granteeType != ACL.GRANTEE_USER && granteeType != ACL.GRANTEE_GROUP) {
                // Reject wrong grantee types.
                throw MailServiceException.CANNOT_GRANT("admin right can be granted to users and groups only");
            } else if (granteeType == ACL.GRANTEE_USER) {
                // Reject external virtual accounts.
                Account account = Provisioning.getInstance().get(AccountBy.id, grantee);
                if (account != null && account.isIsExternalVirtualAccount()) {
                    throw MailServiceException.CANNOT_GRANT("admin right cannot be granted to virtual users");
                }
            }
        }

        GrantAccess redoPlayer = new GrantAccess(mId, itemId, grantee, granteeType, rights, args, expiry);

        ACL.Grant grant = null;
        try (final MailboxTransaction t = mailboxWriteTransaction("grantAccess", octxt, redoPlayer)) {
            MailItem item = getItemById(itemId, MailItem.Type.UNKNOWN);
            checkItemChangeID(item);

            grant = item.grantAccess(grantee, granteeType, rights, args, expiry);
            t.commit();
        }
        return grant;
    }

    public void revokeAccess(OperationContext octxt, int itemId, String grantee) throws ServiceException {
        revokeAccess(octxt, false, itemId, grantee);
    }

    @Override
    public void modifyFolderRevokeGrant(OpContext ctxt, String folderId, String granteeId) throws ServiceException
    {
        revokeAccess((OperationContext)ctxt, Integer.parseInt(folderId), granteeId);
    }

    public void revokeAccess(OperationContext octxt, boolean dueToExpiry, int itemId, String grantee)
    throws ServiceException {
        RevokeAccess redoPlayer = new RevokeAccess(dueToExpiry, mId, itemId, grantee);

        try (final MailboxTransaction t = mailboxWriteTransaction(dueToExpiry ? "expireAccess" : "revokeAccess", octxt, redoPlayer)) {
            MailItem item = getItemById(itemId, MailItem.Type.UNKNOWN);
            checkItemChangeID(item);
            item.revokeAccess(grantee);
            t.commit();
        }
    }

    public void setPermissions(OperationContext octxt, int itemId, ACL acl) throws ServiceException {
        SetPermissions redoPlayer = new SetPermissions(mId, itemId, acl);

        try (final MailboxTransaction t = mailboxWriteTransaction("setPermissions", octxt, redoPlayer)) {
            MailItem item = getItemById(itemId, MailItem.Type.UNKNOWN);
            checkItemChangeID(item);
            item.setPermissions(acl);
            t.commit();
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

        try (final MailboxTransaction t = mailboxWriteTransaction("setRetentionPolicy", octxt, redoPlayer)) {
            if (type == MailItem.Type.FOLDER) {
                Folder folder = getFolderById(itemId);
                checkItemChangeID(folder);
                folder.setRetentionPolicy(rp);
            } else {
                Tag tag = getTagById(itemId);
                checkItemChangeID(tag);
                tag.setRetentionPolicy(rp);
            }
            t.commit();
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

        try (final MailboxTransaction t = mailboxWriteTransaction("setFolderDefaultView", octxt, redoRecorder)) {
            Folder folder = getFolderById(folderId);
            if (!checkItemChangeID(folder)) {
                throw MailServiceException.MODIFY_CONFLICT();
            }
            folder.setDefaultView(view);
            t.commit();
        }
    }

    public void setFolderUrl(OperationContext octxt, int folderId, String url) throws ServiceException {
        SetFolderUrl redoRecorder = new SetFolderUrl(mId, folderId, url);

        try (final MailboxTransaction t = mailboxWriteTransaction("setFolderUrl", octxt, redoRecorder)) {
            Folder folder = getFolderById(folderId);
            checkItemChangeID(folder);
            folder.setUrl(url);
            t.commit();
            updateRssDataSource(folder);
        }
    }

    public void setFolderWebOfflineSyncDays(OperationContext octxt, int folderId, int days) throws ServiceException {
        SetWebOfflineSyncDays redoRecorder = new SetWebOfflineSyncDays(mId, folderId, days);

        try (final MailboxTransaction t = mailboxWriteTransaction("setFolderWebOfflineSyncDays", octxt, redoRecorder)) {
            Folder folder = getFolderById(folderId);
            if (!checkItemChangeID(folder)) {
                throw MailServiceException.MODIFY_CONFLICT();
            }
            folder.setWebOfflineSyncDays(days);
            t.commit();
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
        if (StringUtil.isNullOrEmpty(url)) {
            return;
        }

        // get the remote data, skipping anything we've already seen (if applicable)
        Folder folder = getFolderById(octxt, folderId);
        Folder.SyncData fsd = subscription ? folder.getSyncData() : null;
        FeedManager.SubscriptionData<?> sdata = FeedManager.retrieveRemoteDatasource(getAccount(), url, fsd);
        if (!sdata.isNotModified()) {
            try (final MailboxLock l = getWriteLockAndLockIt()) {
                importFeedInternal(octxt, folder, subscription, sdata, l);
            }
        }
    }

    private void importFeedInternal(OperationContext octxt, Folder folder, boolean subscription,
            FeedManager.SubscriptionData<?> sdata, MailboxLock l)
    throws ServiceException {
        assert(l.isWriteLockedByCurrentThread());
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
            if (subscription && isCalendar) {
                emptyFolder(octxt, folder.getId(), false);  // quicker than deleting appointments one at a time
            }
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
        int numSkipped = 0;
        for (Object obj : items) {
            try {
                if (obj instanceof Invite) {
                    Invite inv = (Invite) obj;
                    String uid = inv.getUid();
                    if (uid == null) {
                        uid = UUIDUtil.generateUUID();
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
                        CalendarItem calItem = getCalendarItemByUid(octxtNoConflicts, uid);
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
                                Invite oldInvites[] = calItem.getInvites();
                                if ((oldInvites == null) || (oldInvites.length == 0)) {
                                    // Something is seriously wrong with the existing calendar item.  Delete it so
                                    // new invite will import cleanly
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
                                    if (!importIt && ZimbraLog.calendar.isDebugEnabled()) {
                                        if (sameFolder) {
                                            ZimbraLog.calendar.debug(
                    "Skip importing UID=%s. Already present & not newer.  SEQUENCE/LAST-MODIFIED old=%s,%s new=%s,%s",
                                                    uid, curInv.getSeqNo(), curInv.getLastModified(),
                                                    inv.getSeqNo(), inv.getLastModified());
                                        } else {
                                            ZimbraLog.calendar.debug("Skip importing UID=%s. Already in different folder id=%d",
                                                    uid, curFolder.getId());
                                        }
                                    }
                                }
                            }
                        }
                        if (importIt) {
                            addInvite(octxtNoConflicts, inv, folder.getId(), true, addRevision);
                        } else {
                            numSkipped++;
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.calendar.warn("Skipping bad iCalendar object UID=%s during import into folder id=%d",
                                inv.getUid(), folder.getId(), e);
                    }
                } else if (obj instanceof ParsedMessage) {
                    DeliveryOptions dopt = new DeliveryOptions().setFolderId(folder).setNoICal(true).setFlags(Flag.BITMASK_UNREAD);
                    addMessage(octxtNoConflicts, (ParsedMessage) obj, dopt, null);
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException", e);
            }
        }

        if (numSkipped > 0) {
            ZimbraLog.calendar.warn("Skipped importing %d iCalendar objects with clashing UIDs", numSkipped);
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

    public void setSubscriptionData(OperationContext octxt, int folderId, long date, String uuid)
    throws ServiceException {
        SetSubscriptionData redoRecorder = new SetSubscriptionData(mId, folderId, date, uuid);

        try (final MailboxTransaction t = mailboxWriteTransaction("setSubscriptionData", octxt, redoRecorder)) {
            getFolderById(folderId).setSubscriptionData(uuid, date);
            t.commit();
        }
    }

    public void setSyncDate(OperationContext octxt, int folderId, long date) throws ServiceException {
        SetSubscriptionData redoRecorder = new SetSubscriptionData(mId, folderId, date, null);

        try (final MailboxTransaction t = mailboxWriteTransaction("setSyncDate", octxt, redoRecorder)) {
            getFolderById(folderId).setSyncDate(date);
            t.commit();
        }
    }

    public void setActiveSyncDisabled(OperationContext octxt, int folderId, boolean disableActiveSync) throws ServiceException {
        SetActiveSyncDisabled redoRecorder = new SetActiveSyncDisabled(mId, folderId, disableActiveSync);

        try (final MailboxTransaction t = mailboxWriteTransaction("setActiveSyncDisabled", octxt, redoRecorder)) {
            getFolderById(folderId).setActiveSyncDisabled(disableActiveSync);
            t.commit();
        }
    }

    private boolean deletedBatchOfItemsInFolder(OperationContext octxt, QueryParams params, TargetConstraint tcon)
    throws ServiceException {
        // Lock this mailbox to make sure that no one modifies the items we're about to delete.
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            DeleteItem redoRecorder = new DeleteItem(mId, MailItem.Type.UNKNOWN, tcon);
            try (final MailboxTransaction t = mailboxWriteTransaction("delete", octxt, redoRecorder)) {
                setOperationTargetConstraint(tcon);
                PendingDelete info = DbMailItem.getLeafNodes(this, params);
                if (info.itemIds.isEmpty()) {
                    return false;
                }
                redoRecorder.setIds(ArrayUtil.toIntArray(info.itemIds.getAllIds()));
                MailItem.delete(this, info, null, true /* writeTombstones */, false /* not fromDumpster */);
                t.commit();
            }
        }
        return true;
    }

    /**
     * @param removeTopLevelFolder - delete folder after it has been emptied
     */
    private void emptyFolder(OperationContext octxt, int folderId,
            boolean removeTopLevelFolder, boolean removeSubfolders, TargetConstraint tcon)
    throws ServiceException {
        try {
            if (emptyFolderOpLock.tryLock()
                || emptyFolderOpLock.tryLock(Provisioning.getInstance().getLocalServer().getEmptyFolderOpTimeout(),
                    TimeUnit.SECONDS)) {
                int batchSize = Provisioning.getInstance().getLocalServer().getMailEmptyFolderBatchSize();
                ZimbraLog.mailbox.debug("Emptying folder %s, removeTopLevelFolder=%b, removeSubfolders=%b, batchSize=%d",
                    folderId, removeTopLevelFolder, removeSubfolders, batchSize);
                List<Integer> folderIds = new ArrayList<Integer>();
                if (!removeSubfolders) {
                    folderIds.add(folderId);
                } else {
                    List<Folder> folders = getFolderById(octxt, folderId).getSubfolderHierarchy();
                    for (Folder folder : folders) {
                        folderIds.add(folder.getId());
                    }
                }
                // Make sure that the user has the delete permission for all folders in the hierarchy.
                for (int id : folderIds) {
                    if ((getEffectivePermissions(octxt, id, MailItem.Type.FOLDER) & ACL.RIGHT_DELETE) == 0) {
                        throw ServiceException.PERM_DENIED("not authorized to empty folder "
                            + getFolderById(octxt, id).getPath());
                    }
                }
                int lastChangeID = octxt != null && octxt.change != -1 ? octxt.change : getLastChangeID();
                // Delete the items in batches.  (1000 items by default)
                QueryParams params = new QueryParams();
                params.setFolderIds(folderIds).setModifiedSequenceBefore(lastChangeID + 1).setRowLimit(batchSize);
                boolean firstTime = true;
                do {
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
                } while (deletedBatchOfItemsInFolder(octxt, params, tcon));
                if ((removeTopLevelFolder || removeSubfolders) && (!folderIds.isEmpty())) {
                    if (!removeTopLevelFolder) {
                        folderIds.remove(0); // 0th position is the folder being emptied
                    }
                    if (!folderIds.isEmpty()) {
                        try (final MailboxLock l = getWriteLockAndLockIt()) {
                            delete(octxt, ArrayUtil.toIntArray(folderIds), MailItem.Type.FOLDER, tcon, false /* don't useEmptyForFolders */, null);
                        }
                    }
                }
            } else {
                ZimbraLog.mailbox
                    .info("Empty large folder operation canceled because previous empty folder operation is in progress");
                throw ServiceException
                    .ALREADY_IN_PROGRESS("Empty Folder operation is in progress. Please wait for the operation to complete");
            }
        } catch (InterruptedException e) {
            ZimbraLog.mailbox.warn("Empty folder operation interupted while acquiring emptyFolderOpLock", e);
            throw ServiceException
            .ALREADY_IN_PROGRESS("Empty Folder operation is in progress. Please wait for the operation to complete");
        } finally {
            if (emptyFolderOpLock.isHeldByCurrentThread()) {
                emptyFolderOpLock.unlock();
            }
        }
    }

    public void emptyFolder(OperationContext octxt, int folderId, boolean removeSubfolders)
    throws ServiceException {
        emptyFolder(octxt, folderId, false /* removeTopLevelFolder */, removeSubfolders, null /* TargetConstraint */);
    }

    @Override
    public void emptyFolder(OpContext octxt, String folderId, boolean removeSubfolders)
    throws ServiceException {
        emptyFolder((OperationContext)octxt, Integer.parseInt(folderId), removeSubfolders);
    }

    public SearchFolder createSearchFolder(OperationContext octxt, int folderId, String name, String query,
            String types, String sort, int flags, byte color)
    throws ServiceException {
        return createSearchFolder(octxt, folderId, name, query, types, sort, flags, new Color(color));
    }

    public SearchFolder createSearchFolder(OperationContext octxt, int folderId, String name, String query,
            String types, String sort, int flags, Color color)
    throws ServiceException {
        CreateSavedSearch redoRecorder = new CreateSavedSearch(mId, folderId, name, query, types, sort, flags, color);

        try (final MailboxTransaction t = mailboxWriteTransaction("createSearchFolder", octxt, redoRecorder)) {
            CreateSavedSearch redoPlayer = (CreateSavedSearch) currentChange().getRedoPlayer();

            int searchId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getSearchId());
            String uuid = redoPlayer == null ? UUIDUtil.generateUUID() : redoPlayer.getUuid();
            SearchFolder search = SearchFolder.create(searchId, uuid, getFolderById(folderId), name, query, types, sort,
                    flags, color, null);
            redoRecorder.setSearchIdAndUuid(search.getId(), search.getUuid());
            t.commit();
            return search;
        }
    }

    public void modifySearchFolder(OperationContext octxt, int id, String query, String types, String sort)
    throws ServiceException {
        ModifySavedSearch redoRecorder = new ModifySavedSearch(mId, id, query, types, sort);

        try (final MailboxTransaction t = mailboxWriteTransaction("modifySearchFolder", octxt, redoRecorder)) {
            SearchFolder search = getSearchFolderById(id);
            checkItemChangeID(search);

            search.changeQuery(query, types, sort);
            t.commit();
        }
    }

    public Mountpoint createMountpoint(OperationContext octxt, int folderId, String name,
            String ownerId, int remoteId, String remoteUuid, MailItem.Type view, int flags, byte color, boolean showReminders)
    throws ServiceException {
        return createMountpoint(octxt, folderId, name, ownerId, remoteId, remoteUuid, view, flags, new Color(color),
                showReminders);
    }

    public Mountpoint createMountpoint(OperationContext octxt, int folderId, String name,
            String ownerId, int remoteId, String remoteUuid, MailItem.Type view, int flags, Color color, boolean showReminders)
    throws ServiceException {
        CreateMountpoint redoRecorder = new CreateMountpoint(mId, folderId, name, ownerId, remoteId, remoteUuid,
                view, flags, color, showReminders);

        try (final MailboxTransaction t = mailboxWriteTransaction("createMountpoint", octxt, redoRecorder)) {
            CreateMountpoint redoPlayer = (CreateMountpoint) currentChange().getRedoPlayer();

            int mptId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getId());
            String uuid = redoPlayer == null ? UUIDUtil.generateUUID() : redoPlayer.getUuid();
            Mountpoint mpt = Mountpoint.create(mptId, uuid, getFolderById(folderId), name, ownerId, remoteId, remoteUuid, view, flags,
                    color, showReminders, null);
            redoRecorder.setIdAndUuid(mpt.getId(), mpt.getUuid());
            t.commit();
            return mpt;
        }
    }

    /**
     * Updates the remote owner and item id stored in the mountpoint to match the current location of the
     * target folder.  The target folder is identified by the remote UUID stored in the mountpoint's metadata.
     * @param octxt
     * @param mountpointId item id of the Mountpoint
     * @return
     * @throws ServiceException
     */
    public Mountpoint refreshMountpoint(OperationContext octxt, int mountpointId) throws ServiceException {
        Mountpoint mp = getMountpointById(octxt, mountpointId);
        Provisioning prov = Provisioning.getInstance();
        ShareLocator shloc = prov.getShareLocatorById(mp.getRemoteUuid());
        if (shloc == null || mp.getOwnerId().equalsIgnoreCase(shloc.getShareOwnerAccountId())) {
            // Share apparently did not move.
            return mp;
        }

        // Look up remote folder by UUID to discover the new numeric id.
        Account shareOwner = Provisioning.getInstance().get(Key.AccountBy.id, shloc.getShareOwnerAccountId());
        AuthToken at = AuthToken.getCsrfUnsecuredAuthToken(octxt.getAuthToken());
        String pxyAuthToken = Provisioning.onLocalServer(shareOwner) ? null : at.getProxyAuthToken();
        ZAuthToken zat = null;
        if (pxyAuthToken == null) {
            zat = at.toZAuthToken();
            zat.resetProxyAuthToken();
        } else {
            zat = new ZAuthToken(pxyAuthToken);
        }
        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(shareOwner));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(shareOwner.getId());
        zoptions.setTargetAccountBy(Key.AccountBy.id);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        zmbx.setName(shareOwner.getName()); /* need this when logging in using another user's auth */
        ZFolder zfolder = zmbx.getFolderByUuid(shloc.getUuid());

        if (zfolder != null) {
            ItemId fid = new ItemId(zfolder.getId(), shareOwner.getId());
            return refreshMountpoint(octxt, mountpointId, shareOwner.getId(), fid.getId());
        } else {
            return null;
        }
    }

    public Mountpoint refreshMountpoint(OperationContext octxt, int mountpointId, String ownerId, int remoteId)
    throws ServiceException {
        RefreshMountpoint redoRecorder = new RefreshMountpoint(mId, mountpointId, ownerId, remoteId);
        try (final MailboxTransaction t = mailboxWriteTransaction("refreshMountpoint", octxt, redoRecorder)) {
            Mountpoint mpt = getMountpointById(octxt, mountpointId);
            mpt.setRemoteInfo(ownerId, remoteId);
            t.commit();
        }

        return getMountpointById(octxt, mountpointId);
    }

    public void enableSharedReminder(OperationContext octxt, int mountpointId, boolean enable) throws ServiceException {
        EnableSharedReminder redoRecorder = new EnableSharedReminder(mId, mountpointId, enable);

        try (final MailboxTransaction t = mailboxWriteTransaction("enableSharedReminders", octxt, redoRecorder)) {
            Mountpoint mpt = getMountpointById(octxt, mountpointId);
            mpt.enableReminder(enable);
            t.commit();
        }
    }

    /**
     * Purges messages in system folders based on user- and admin-level purge settings on the account.
     * Returns {@code true} if all messages that meet the purge criteria were purged, {@code false} if the number of
     * messages to purge in any folder exceeded {@code maxItemsPerFolder}.
     */
    public boolean purgeMessages(OperationContext octxt) throws ServiceException {
        Account acct = getAccount();
        int maxItemsPerFolder = Provisioning.getInstance().getLocalServer().getMailPurgeBatchSize();
        if (ZimbraLog.purge.isDebugEnabled()) {
            ZimbraLog.purge.debug("System retention policy: Trash=%s, Junk=%s, All messages=%s, Dumpster=%s",
                acct.getMailTrashLifetimeAsString(),
                acct.getMailSpamLifetimeAsString(),
                acct.getMailMessageLifetimeAsString(),
                acct.getMailDumpsterLifetimeAsString());
            ZimbraLog.purge.debug("User-specified retention policy: Inbox read=%s, Inbox unread=%s, Sent=%s, Junk=%s, Trash=%s, Versions=%s, VersionsEnabled=%s" ,
                acct.getPrefInboxReadLifetimeAsString(),
                acct.getPrefInboxUnreadLifetimeAsString(),
                acct.getPrefSentLifetimeAsString(),
                acct.getPrefJunkLifetimeAsString(),
                acct.getPrefTrashLifetimeAsString(),
                acct.getFileVersionLifetimeAsString(),
                acct.isFileVersioningEnabled());
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

        boolean userFileVersioningEnabled = acct.isFileVersioningEnabled();
        long    userFileVersionLifeTime   = acct.getFileVersionLifetime();

        if (globalTimeout <= 0 && trashTimeout <= 0 && spamTimeout <= 0 &&
            userInboxReadTimeout <= 0 && userInboxReadTimeout <= 0 &&
            userInboxUnreadTimeout <= 0 && userSentTimeout <= 0 && systemDumpsterTimeoutMillis <= 0 && ( !userFileVersioningEnabled || userFileVersionLifeTime<=0 )) {
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

        // call to purge expired messages with IMAP \Deleted flag
        // for expiration check, used zimbraMailTrashLifetime
        purgeExpiredIMAPDeletedMessages(trashTimeout);

        PurgeOldMessages redoRecorder = new PurgeOldMessages(mId);

        try (final MailboxTransaction t = mailboxWriteTransaction("purgeMessages", octxt, redoRecorder)) {

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

            if (userFileVersioningEnabled && userFileVersionLifeTime > 0) {
                int numPurged = MailItem.purgeRevisions(this, getOperationTimestampMillis() - userFileVersionLifeTime);
                ZimbraLog.purge.debug("Purged %d revisions", numPurged);
            }
            // Process any folders that have retention policy set.
            for (Folder folder : getFolderList(octxt, SortBy.NONE)) {
                RetentionPolicy rp = RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(acct, folder.getRetentionPolicy());
                for (Policy policy : rp.getPurgePolicy()) {
                    long folderLifetime;

                    try {
                        folderLifetime = DateUtil.getTimeInterval(policy.getLifetime());
                    } catch (ServiceException e) {
                        ZimbraLog.purge.error("Invalid purge lifetime set for folder %s.", folder.getPath(), e);
                        continue;
                    }

                    long folderTimeout = getOperationTimestampMillis() - folderLifetime;
                    int numPurged = Folder.purgeMessages(this, folder, folderTimeout, null, false, false, maxItemsPerFolder);
                    purgedAll = updatePurgedAll(purgedAll, numPurged, maxItemsPerFolder);
                }
            }

            // Process any tags that have retention policy set.
            for (Tag tag : getTagList(octxt)) {
                RetentionPolicy rp = RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(acct, tag.getRetentionPolicy());
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
                    MailItem.delete(this, info, null, false, false);
                    List<Integer> ids = info.itemIds.getIds(MailItem.Type.MESSAGE);
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
                    currentChange().sync = largestTrimmed;
                    DbMailbox.setSyncCutoff(this, currentChange().sync);
                }
            }

            // record the purge time.
            if (purgedAll) {
                DbMailbox.updateLastPurgeAt(this, System.currentTimeMillis());
            }

            t.commit();
            ZimbraLog.purge.debug("purgedAll=%b", purgedAll);
            return purgedAll;
        }
    }

    private boolean updatePurgedAll(boolean purgedAll, int numDeleted, Integer maxItems) {
        return purgedAll && (maxItems == null || numDeleted < maxItems);
    }

    /** Returns the smaller non-zero value, or <tt>0</tt> if both
     *  <tt>t1</tt> and <tt>t2</tt> are <tt>0</tt>. */
    private long pickTimeout(long t1, long t2) {
        if (t1 == 0) {
            return t2;
        }
        if (t2 == 0) {
            return t1;
        }
        return Math.min(t1, t2);
    }

    public void purgeImapDeleted(OperationContext octxt) throws ServiceException {
        PurgeImapDeleted redoRecorder = new PurgeImapDeleted(mId);
        try (final MailboxTransaction t = mailboxWriteTransaction("purgeImapDeleted", octxt, redoRecorder)) {
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
                MailItem.delete(this, info, null, true, false);
            }
            t.commit();
        }
    }

    public WikiItem createWiki(OperationContext octxt, int folderId, String wikiword, String author, String description, InputStream data)
    throws ServiceException {
        return (WikiItem) createDocument(octxt, folderId, wikiword, WikiItem.WIKI_CONTENT_TYPE, author, description, true, data, MailItem.Type.WIKI);
    }

    public Document createDocument(OperationContext octxt, int folderId, String filename, String mimeType,
            String author, String description, InputStream data)
    throws ServiceException {
        return createDocument(octxt, folderId, filename, mimeType, author, description, true, data, MailItem.Type.DOCUMENT);
    }

    public Document createDocument(OperationContext octxt, int folderId, String filename, String mimeType,
            String author, String description, boolean descEnabled, InputStream data, MailItem.Type type)
    throws ServiceException {
        try {
            ParsedDocument pd = new ParsedDocument(data, filename, mimeType, System.currentTimeMillis(), author, description, descEnabled);
            return createDocument(octxt, folderId, pd, type, 0);
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("error writing document blob", ioe);
        }
    }

    public Document createDocument(OperationContext octxt, int folderId, ParsedDocument pd, MailItem.Type type,
            int flags)
    throws IOException, ServiceException {
        return createDocument(octxt, folderId, pd, type, flags, null, null, true);
    }

    public Document createDocument(OperationContext octxt, int folderId, ParsedDocument pd, MailItem.Type type,
            int flags, MailItem parent, CustomMetadata custom, boolean indexing)
    throws IOException, ServiceException {
        StoreManager sm = StoreManager.getInstance();
        StagedBlob staged = sm.stage(pd.getBlob(), this);

        SaveDocument redoRecorder = new SaveDocument(mId, pd.getDigest(), pd.getSize(), folderId, flags);

        long start = System.currentTimeMillis();
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            final MailboxTransaction t = mailboxWriteTransaction("createDoc", octxt, redoRecorder);
            try {
                SaveDocument redoPlayer = (octxt == null ? null : (SaveDocument) octxt.getPlayer());
                int itemId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getMessageId());
                String uuid = redoPlayer == null ? UUIDUtil.generateUUID() : redoPlayer.getUuid();

                Document doc;
                switch (type) {
                    case DOCUMENT:
                        doc = Document.create(itemId, uuid, getFolderById(folderId), pd.getFilename(), pd.getContentType(), pd, custom, flags, parent);
                        break;
                    case WIKI:
                        doc = WikiItem.create(itemId, uuid, getFolderById(folderId), pd.getFilename(), pd, null);
                        break;
                    default:
                        throw MailServiceException.INVALID_TYPE(type.toString());
                }

                redoRecorder.setMessageId(itemId);
                redoRecorder.setUuid(doc.getUuid());
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

                if (indexing) {
                    index.add(doc);
                }

                t.commit();
                long elapsed = System.currentTimeMillis() - start;
                ZimbraLog.mailbox.debug("createDocument elapsed=" + elapsed);
                return doc;
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("error writing document blob", ioe);
            } finally {
                t.close();
                sm.quietDelete(staged);
            }
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

        try (final MailboxLock l = getWriteLockAndLockIt()) {
            final MailboxTransaction t = mailboxWriteTransaction("addDocumentRevision", octxt, redoRecorder);
            try {
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

                t.commit();
                return doc;
            } catch (IOException ioe) {
                throw ServiceException.FAILURE("error writing document blob", ioe);
            } finally {
                t.close();
                sm.quietDelete(staged);
            }
        }
    }

    public void purgeRevision(OperationContext octxt, int itemId, int rev, boolean includeOlderRevisions)
            throws ServiceException {
        PurgeRevision redoRecorder = new PurgeRevision(mId, itemId, rev, includeOlderRevisions);
        try (final MailboxTransaction t = mailboxWriteTransaction("purgeRevision", octxt, redoRecorder)) {
            MailItem item = getItemById(itemId, MailItem.Type.DOCUMENT);
            item.purgeRevision(rev, includeOlderRevisions);
            t.commit();
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
            staged = sm.stage(is, this);
        } finally {
            ByteUtil.closeStream(is);
        }
        String digest = staged.getDigest();
        int size = (int) staged.getSize();

        CreateChat redoRecorder = new CreateChat(mId, digest, size, folderId, flags, tags);
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            final MailboxTransaction t = mailboxWriteTransaction("createChat", octxt, redoRecorder);
            try {
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
                t.commit();
                return chat;
            } finally {
                t.close();
                sm.quietDelete(staged);
            }
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
            staged = sm.stage(is, this);
        } finally {
            ByteUtil.closeStream(is);
        }

        String digest = staged.getDigest();
        int size = (int) staged.getSize();

        SaveChat redoRecorder = new SaveChat(mId, id, digest, size, -1, 0, null);
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            final MailboxTransaction t = mailboxWriteTransaction("saveChat", octxt, redoRecorder);
            try {
                SaveChat redoPlayer = (SaveChat) currentChange().getRedoPlayer();

                redoRecorder.setMessageBodyInfo(new ParsedMessageDataSource(pm), size);

                Chat chat = (Chat) getItemById(id, MailItem.Type.CHAT);

                if (!chat.isMutable()) {
                    throw MailServiceException.IMMUTABLE_OBJECT(id);
                }
                if (!checkItemChangeID(chat)) {
                    throw MailServiceException.MODIFY_CONFLICT();
                }

                // content changed, so we're obliged to change the IMAP uid
                int imapID = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getImapId());
                redoRecorder.setImapId(imapID);

                // update the content and increment the revision number
                chat.setContent(staged, pm);

                // NOTE: msg is now uncached (will this cause problems during commit/reindex?)
                index.add(chat);

                t.commit();
                return chat;
            } finally {
                t.close();
                sm.quietDelete(staged);
            }
        }
    }

    /**
     * Optimize the underlying database.
     */
    public void optimize(int level) {
        try (final MailboxLock l = getWriteLockAndLockIt()) {
            DbConnection conn = DbPool.getConnection(this);

            DbMailbox.optimize(conn, this, level, l);
            DbPool.quietClose(conn);
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("db optimize failed for mailbox " + getId() + ": " + e);
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

    private final SharedDeliveryCoordinator mSharedDelivCoord = new SharedDeliveryCoordinator();

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
     * for folder view migration.
     */
    void migrateFolderView(OperationContext octxt, Folder f, MailItem.Type newView) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("migrateFolderView", octxt, null)) {
            f.migrateDefaultView(newView);
            t.commit();
        }
    }

    protected boolean needRedo(OperationContext octxt, RedoableOp recorder) {
        // Don't generate redo data for changes made during mailbox version migrations.
        if (!open && !(recorder instanceof CreateMailbox)) {
            return false;
        }
        return octxt == null || octxt.needRedo();
    }

    // if the incoming message has one of these flags, don't up our "new messages" counter
    public static final int NON_DELIVERY_FLAGS = Flag.BITMASK_DRAFT | Flag.BITMASK_FROM_ME | Flag.BITMASK_COPIED | Flag.BITMASK_DELETED;

    void snapshotCounts() throws ServiceException {
        // for write ops, update the "new messages" count in the DB appropriately
        OperationContext octxt = currentChange().octxt;
        RedoableOp player = currentChange().getRedoPlayer();
        RedoableOp recorder = currentChange().recorder;

        if (recorder != null && (player == null || (octxt != null && !octxt.isRedo()))) {
            assert(currentChange().writeChange);
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
                currentChange().recent = state.getRecentMessages() + 1;
            } else if (octxt != null && state.getRecentMessages() != 0) {
                Session s = octxt.getSession();
                if (s instanceof SoapSession
                                || (s instanceof SoapSession.DelegateSession && ((SoapSession.DelegateSession) s)
                                                .getParentSession().isOfflineSoapSession())) {
                    currentChange().recent = 0;
                }
            }
        }

        if (currentChange().isMailboxRowDirty()) {
            assert(currentChange().writeChange);
            if (currentChange().recent != MailboxChange.NO_CHANGE) {
                ZimbraLog.mailbox.debug("setting recent count to %d", currentChange().recent);
            }
            DbMailbox.updateMailboxStats(this);
        }

        boolean foldersTagsDirty = false;
        if (currentChange().dirty != null && currentChange().dirty.hasNotifications()) {
            assert(currentChange().writeChange);
            if (currentChange().dirty.created != null) {
                for (BaseItemInfo item : currentChange().dirty.created.values()) {
                    if (item instanceof Folder) {
                        Folder folder = (Folder) item;
                        foldersTagsDirty = true;
                        if (folder.getSize() != 0) {
                            folder.saveFolderCounts(false);
                        }
                    } else if (item instanceof Tag) {
                        Tag tag = (Tag) item;
                        foldersTagsDirty = true;
                        if (tag.isUnread()) {
                            tag.saveTagCounts();
                        }
                    }
                }
            }

            if (currentChange().dirty.modified != null) {
                for (Change change : currentChange().dirty.modified.values()) {
                    if (change.what instanceof Folder) {
                        foldersTagsDirty = true;
                        if ((change.why & (Change.UNREAD | Change.SIZE)) != 0) {
                            ((Folder) change.what).saveFolderCounts(false);
                        }
                    } else if (change.what instanceof Tag) {
                        foldersTagsDirty = true;
                        if ((change.why & Change.UNREAD | Change.SIZE) != 0) {
                            ((Tag) change.what).saveTagCounts();
                        }
                    } else if ((change.what instanceof MailItem)) {
                        if (change.what instanceof Conversation) {
                            uncache((MailItem) change.what);
                        } else {
                            cache((MailItem) change.what);
                        }
                    }
                }
            }

            if (currentChange().dirty.deleted != null) {
                for (Change change : currentChange().dirty.deleted.values()) {
                    if (change.what instanceof Folder || change.what instanceof Tag) {
                        foldersTagsDirty = true;
                        break;
                    }
                }
            }

            if (foldersTagsDirty) {
                cacheFoldersTags();
            }
        }

        if (DebugConfig.checkMailboxCacheConsistency && currentChange().dirty != null
                        && currentChange().dirty.hasNotifications()) {
            if (currentChange().dirty.created != null) {
                for (BaseItemInfo item : currentChange().dirty.created.values()) {
                    if (item instanceof MailItem) {
                        MailItem mi = (MailItem) item;
                        DbMailItem.consistencyCheck(mi, mi.getUnderlyingData(), mi.encodeMetadata().toString());
                    }
                }
            }
            if (currentChange().dirty.modified != null) {
                for (Change change : currentChange().dirty.modified.values()) {
                    if (change.what instanceof MailItem) {
                        MailItem item = (MailItem) change.what;
                        DbMailItem.consistencyCheck(item, item.getUnderlyingData(), item.encodeMetadata().toString());
                    }
                }
            }
        }
    }

    private ChangeNotification commitCache(MailboxChange change, MailboxLock l) {
        if (change == null) {
            return null;
        }
        ChangeNotification notification = null;

        // save for notifications (below)
        PendingLocalModifications dirty = null;
        if (change.dirty != null && change.dirty.hasNotifications()) {
            assert(currentChange().writeChange);
            dirty = change.dirty;
            change.dirty = new PendingLocalModifications();
        }

        try {
            // the mailbox data has changed, so commit the changes
            if (change.sync != null) {
                state.setTrackSync(change.sync);
            }
            if (change.imap != null) {
                state.setTrackingImap(change.imap);
            }
            if (change.size != MailboxChange.NO_CHANGE) {
                state.setSize(change.size);
            }
            if (change.itemId != MailboxChange.NO_CHANGE) {
                //cluster-wide ID state has already been updated; this is just the local value
                state.setItemId(change.itemId);
            }
            if (change.searchId != MailboxChange.NO_CHANGE) {
                state.setSearchId(change.searchId);
            }
            if (change.contacts != MailboxChange.NO_CHANGE) {
                state.setNumContacts(change.contacts);
            }
            if (change.changeId != MailboxChange.NO_CHANGE && change.changeId > mData.lastChangeId) {
                state.setChangeId(change.changeId);
                state.setLastChangeDate(change.timestamp);
            }
            if (change.accessed != MailboxChange.NO_CHANGE) {
                state.setLastWriteDate(change.accessed);
            }
            if (change.recent != MailboxChange.NO_CHANGE) {
                state.setRecentMessages(change.recent);
            }

            if (change.config != null) {
                if (change.config.getSecond() == null) {
                    state.removeConfigKey(change.config.getFirst());
                } else {
                    state.addConfigKey(change.config.getFirst());
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
                            getAccount(), dirty, change.octxt, mData.lastChangeId, change.getOperation(), change.timestamp);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.warn("error getting account for the mailbox", e);
                }
            }
            Iterator<WeakReference<TransactionListener>> itr = Mailbox.this.transactionListeners.iterator();
            while (itr.hasNext()) {
                TransactionListener listener = itr.next().get();
                if (listener == null) {
                    itr.remove();
                }
                else {
                    listener.commitCache(change.depth == 0);
                }
            }

        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during cache commit", e);
        } finally {
            // keep our MailItem cache at a reasonable size
            trimItemCache();
            //remove local references to MailItems used over the course of the transaction
            if (change.depth == 0) {
                mItemCache.flush();
            }
            // make sure we're ready for the next change
            change.reset();
        }
        return notification;
    }


    private void doNotifyListeners(MailboxChange change, ChangeNotification notification) {
        long start = System.currentTimeMillis();
        try {
            Session source = change.octxt == null ? null : change.octxt.getSession();
            SourceSessionInfo sourceInfo = source != null ? source.toSessionInfo() : null;
            publisher.publish(notification.mods, notification.lastChangeId, sourceInfo, this.hashCode());
            MailboxListener.notifyListeners(notification);
            ZimbraLog.mailbox.trace("Mailbox.notifyListeners %s %s %s",
                    notification, publisher, ZimbraLog.elapsedSince(start));
        } catch (Exception ex) {
            ZimbraLog.mailbox.error("Problem in Mailbox.notifyListeners - swallowing exception. %s %s %s",
                    notification, publisher, ZimbraLog.elapsedSince(start), ex);
        }
    }

    /**
     * Notifying listeners can involve network chatter and therefore be slow, hence don't delay releasing
     * mailbox locks for it.
     */
    private void notifyListeners(MailboxChange change, ChangeNotification notification, MailboxLock lock) {
        if (notification == null) {
            return;
        }
        /* Interrogating the lock to see if current thread has lock has some cost, hence why only do it if
         * have notifications. */
        try {
            if (lock.isLockedByCurrentThread()) {
                /* Still locked, even though we've probably just released a lock.  Use a separate thread
                 * to avoid delaying the outer lock.
                 */
                new Thread(new Runnable() {
                    @Override public void run() {
                        doNotifyListeners(change, notification);
                    }
                }, "notifyMboxListeners-" + Thread.currentThread().getId()).start();
            } else {
                /* we're not holding a lock any more. no need to use a separate thread */
                doNotifyListeners(change, notification);
            }
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("unable to notify listeners for mailbox change %s", change, e);
        }
    }

    private List<Object> rollbackCache(MailboxChange change) throws ServiceException {
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
                            purge(MailItem.Type.TAG);
                        } else if (obj instanceof Folder || FOLDER_TYPES.contains(obj)) {
                            purge(MailItem.Type.FOLDER);
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
            Iterator<WeakReference<TransactionListener>> itr = Mailbox.this.transactionListeners.iterator();
            while (itr.hasNext()) {
                TransactionListener listener = itr.next().get();
                if (listener == null) {
                    itr.remove();
                }
                else {
                    listener.rollbackCache();
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
            ItemCache cache = currentChange().itemCache;
            if (cache != null) {
                cache.trim(MAX_ITEM_CACHE_SIZE);
            }
        } catch (RuntimeException e) {
            ZimbraLog.mailbox.error("ignoring error during item cache trim", e);
        }
    }

    public boolean attachmentsIndexingEnabled() throws ServiceException {
        return getAccount().isAttachmentsIndexingEnabled();
    }

    private void logCacheActivity(Object key, MailItem.Type type, MailItem item) {
        // The global item cache counter always gets updated
        if (!isCachedType(type)) {
            ZimbraPerf.COUNTER_MBOX_ITEM_CACHE.increment(item == null ? 0 : 100);
        }

        // the per-access log only gets updated when cache or perf debug logging is on
        if (!ZimbraLog.cache.isDebugEnabled()) {
            return;
        }

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

        try (final MailboxTransaction t = mailboxWriteTransaction("lock", octxt, redoRecorder)) {
            MailItem item = getItemById(itemId, type);
            item.lock(Provisioning.getInstance().getAccountById(accountId));
            t.commit();
            return item;
        }
    }

    public void unlock(OperationContext octxt, int itemId, MailItem.Type type, String accountId)
    throws ServiceException {
        UnlockItem redoRecorder = new UnlockItem(mId, itemId, type, accountId);

        try (final MailboxTransaction t = mailboxWriteTransaction("unlock", octxt, redoRecorder)) {
            MailItem item = getItemById(itemId, type);
            item.unlock(Provisioning.getInstance().getAccountById(accountId));
            t.commit();
        }
    }

    public Comment createComment(OperationContext octxt, int parentId, String text, String creatorId)
    throws ServiceException {
        CreateComment redoRecorder = new CreateComment(mId, parentId, text, creatorId);

        try (final MailboxTransaction t = mailboxWriteTransaction("createComment", octxt, redoRecorder)) {
            MailItem parent = getItemById(octxt, parentId, MailItem.Type.UNKNOWN);
            if (parent.getType() != MailItem.Type.DOCUMENT) {
                throw MailServiceException.CANNOT_PARENT();
            }
            CreateComment redoPlayer = (CreateComment) currentChange().getRedoPlayer();
            int itemId = redoPlayer == null ? getNextItemId(ID_AUTO_INCREMENT) : redoPlayer.getItemId();
            String uuid = redoPlayer == null ? UUIDUtil.generateUUID() : redoPlayer.getUuid();
            Comment comment = Comment.create(this, parent, itemId, uuid, text, creatorId, null);
            redoRecorder.setItemIdAndUuid(comment.getId(), comment.getUuid());
            index.add(comment);
            t.commit();
            return comment;
        }
    }

    public Link createLink(OperationContext octxt, int folderId, String name, String ownerId, int remoteId) throws ServiceException {
        CreateLink redoRecorder = new CreateLink(mId, folderId, name, ownerId, remoteId);

        try (final MailboxTransaction t = mailboxWriteTransaction("createLink", octxt, redoRecorder)) {
            CreateLink redoPlayer = (CreateLink) currentChange().getRedoPlayer();
            int itemId = getNextItemId(redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getId());
            String uuid = redoPlayer == null ? UUIDUtil.generateUUID() : redoPlayer.getUuid();
            Link link = Link.create(getFolderById(folderId), itemId, uuid, name, ownerId, remoteId, null);
            redoRecorder.setIdAndUuid(link.getId(), link.getUuid());
            t.commit();
            return link;
        }
    }

    public Collection<Comment> getComments(OperationContext octxt, int parentId, int offset, int length)
    throws ServiceException {
        return getComments(octxt, parentId, offset, length, false);
    }

    Collection<Comment> getComments(OperationContext octxt, int parentId, int offset, int length, boolean fromDumpster)
    throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getComments", octxt, null)) {
            MailItem parent = getItemById(parentId, MailItem.Type.UNKNOWN, fromDumpster);
            return parent.getComments(SortBy.DATE_DESC, offset, length);
        }
    }

    public Collection<Comment> getComments(OperationContext octxt, String parentUuid, int offset, int length)
            throws ServiceException {
        return getComments(octxt, parentUuid, offset, length, false);
    }

    Collection<Comment> getComments(OperationContext octxt, String parentUuid, int offset, int length, boolean fromDumpster)
            throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getComments", octxt, null)) {
            MailItem parent = getItemByUuid(parentUuid, MailItem.Type.UNKNOWN, fromDumpster);
            return parent.getComments(SortBy.DATE_DESC, offset, length);
        }
    }

    public UnderlyingData getFirstChildData(OperationContext octxt, MailItem parent)
            throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getFirstChildData", octxt, null)) {
            List<UnderlyingData> keyItems = DbMailItem.getByParent(parent);
            if (!keyItems.isEmpty()) {
                t.commit();
                return keyItems.get(0);
            } else {
                throw ServiceException.NOT_FOUND("Data not found");
            }
        }
    }

    public MailItem markMetadataChanged(OperationContext octxt, int itemId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("markMetadataChanged", octxt, null)) {
            MailItem item = getItemById(octxt, itemId, MailItem.Type.UNKNOWN);
            item.markMetadataChanged();
            t.commit();
            return item;
        }
    }

    protected void migrateWikiFolders() throws ServiceException {
        MigrateToDocuments migrate = new MigrateToDocuments();
        try {
            migrate.handleMailbox(this);
            ZimbraLog.mailbox.info("wiki folder migration finished");
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("wiki folder migration failed for " + getAccount().getName(), e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", mId)
            .add("account", mData.accountId)
            .add("lastItemId", state.getItemId())
            .add("size", state.getSize())
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
        return id < 2 << 29;
    }

    public TypedIdList listItemsForSync(OperationContext octxt, int folderId, MailItem.Type type, long messageSyncStart) throws ServiceException {
        if (folderId == ID_AUTO_INCREMENT) {
            return new TypedIdList();
        }

        try (final MailboxTransaction t = mailboxWriteTransaction("listMessageItemsforgivenDate", octxt)) {

            // if they specified a folder, make sure it actually exists
            Folder folder = getFolderById(folderId);
            TypedIdList ids = DbMailItem.listItems(folder, messageSyncStart, type, true, false);
            t.commit();
            return ids;
        }
    }


    public TypedIdList listConvItemsForSync(OperationContext octxt, int folderId, MailItem.Type type, long messageSyncStart) throws ServiceException {
        if (folderId == ID_AUTO_INCREMENT) {
            return new TypedIdList();
        }

        try (final MailboxTransaction t = mailboxWriteTransaction("listMessageItemsforgivenDate", octxt)) {
            // if they specified a folder, make sure it actually exists
            Folder folder = getFolderById(folderId);
            TypedIdList ids = DbMailItem.listConvItems(folder, messageSyncStart, type, true, false);
            t.commit();
            return ids;
        }
    }

    public Pair<List<Map<String,String>>, TypedIdList> getItemsChangedSince(OperationContext octxt,  int sinceDate) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getModifiedItems", octxt)) {
            Set<Integer> folderIds = Folder.toId(getAccessibleFolders(ACL.RIGHT_READ));
            Pair<List<Map<String,String>>, TypedIdList> dataList = DbMailItem
                    .getItemsChangedSinceDate(this, MailItem.Type.UNKNOWN,  sinceDate, folderIds);
            if (dataList == null) {
                return null;
            }
            t.commit();
            return dataList;
        }
    }

    /**
     * Finds and deletes old mail items deleted by imap client. Here, "old" depends on zimbraMailImapDeletedMessageLifeTime.
     * @throws ServiceException
     */
    public void purgeExpiredIMAPDeletedMessages (long imapDeletedMessageLifeTime) throws ServiceException {
        if(imapDeletedMessageLifeTime > 0) {
            ZimbraLog.purge.debug("Purging expired messages with IMAP \\Deleted flag");
            Server server = getAccount().getServer();
            int purgeBatchSize = server.getMailPurgeBatchSize();
            long cutOff = (System.currentTimeMillis() - imapDeletedMessageLifeTime) / 1000;
            ZimbraLog.purge.debug("IMAP deleted message lifetime = %d, cutOff = %d", imapDeletedMessageLifeTime, cutOff);
            int batch = 0;
            List<Integer> itemIdsWithDeletedFlag = null;
            do {
                itemIdsWithDeletedFlag = DbMailItem.getIMAPDeletedItems(this, cutOff, purgeBatchSize);
                if (itemIdsWithDeletedFlag != null && itemIdsWithDeletedFlag.size() > 0) {
                    batch++;
                    ZimbraLog.purge.debug("Batch %d - Found %d items with \\Deleted flags", batch, itemIdsWithDeletedFlag.size());
                    int itemIds[] = new int[itemIdsWithDeletedFlag.size()];
                    int pos = 0;
                    for (Integer integer : itemIdsWithDeletedFlag) {
                        itemIds[pos] = integer;
                        pos++;
                    }
                    delete(null, itemIds, MailItem.Type.UNKNOWN, null);
                    ZimbraLog.purge.debug("Batch %d - Finished", batch);
                }
            } while (!(itemIdsWithDeletedFlag.size() < purgeBatchSize));
            if (batch == 0 && (itemIdsWithDeletedFlag == null || itemIdsWithDeletedFlag.size() == 0)){
                ZimbraLog.purge.debug("Could not find any expired messages with IMAP \\Deleted flag");
            } else {
                ZimbraLog.purge.debug("Purged total " + ((batch > 1 ? (batch * purgeBatchSize) : 0) + itemIdsWithDeletedFlag.size()) + " expired messages with IMAP \\Deleted flag");
            }
            itemIdsWithDeletedFlag = null;
        } else {
            ZimbraLog.purge.debug("IMAP deleted message life time is not set, so bypassed purging messages with IMAP \\Deleted flag");
        }
    }

    public long getTotalDataSourceUsage() throws ServiceException {
        long usage = 0L;
        for (DataSource ds: getAccount().getAllDataSources()) {
            usage += getDataSourceUsage(ds);
        }
        return usage;
    }

    public void purgeDataSourceMessage(OperationContext octxt, Message msg, String dataSourceId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("purgeMessage", octxt)) {
            DbDataSource.purgeMessage(this, msg, dataSourceId);
            t.commit();
        }
    }

    public boolean dataSourceMessageIsPurged(DataSource ds, String remoteId) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("checkUidPurged", null)) {
            boolean isPurged = DbDataSource.uidIsPurged(ds, remoteId);
            t.commit();
            return isPurged;
        }
    }

    public long getDataSourceUsage(DataSource dataSource) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("dataSourceUsage", null)) {
            long usage = DbDataSource.getDataSourceUsage(dataSource);
            t.commit();
            return usage;
        }
    }

    public void setPreviousFolder(OperationContext octx, int id, String prevFolder) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("setPrevFolder", octx)) {
            DbMailItem.setPreviousFolder(this, id, prevFolder);
            t.commit();
        }
    }

    private boolean referencesOtherMailbox(ItemIdentifier ident) {
        return !((ident.accountId == null) || ident.accountId.equals(getAccountId()));
    }

    /**
     * Copies the items identified in {@param idlist} to folder {@param targetFolder}
     * @param idlist - list of item ids for items to copy
     * @param targetFolder - Destination folder
     * @return The item IDs of the created items - these may be full item IDs in the case of remote folders
     */
    @Override
    public List<String> copyItemAction(OpContext ctxt, ItemIdentifier targetFolder, List<ItemIdentifier> idlist)
    throws ServiceException {
        if ((idlist == null) || idlist.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = Lists.newArrayListWithExpectedSize(idlist.size());
        for (ItemIdentifier ident : idlist) {
            if (this.referencesOtherMailbox(ident)) {
                // Mailbox doesn't support copying from another mailbox because ItemActionHelper
                // only supports a list of int ids.  This isn't a problem for local IMAP because
                // requests are proxied to the selected folder's owner.
                throw ServiceException.FAILURE("Unexpected attempt to copy item from mountpoint", null);
            }
            ids.add(ident.id);
        }
        ItemActionHelper op = ItemActionHelper.COPY((OperationContext) ctxt, this, null, ids,
                MailItem.Type.UNKNOWN, null, new ItemId(targetFolder));
        CopyActionResult caResult = (CopyActionResult) op.getResult();
        return caResult.getCreatedIds();
    }

    @Override
    public List<FolderStore> getUserRootSubfolderHierarchy(OpContext ctxt) throws ServiceException {
        List<Folder> fldrs = getFolderById((OperationContext)ctxt, Mailbox.ID_FOLDER_USER_ROOT).getSubfolderHierarchy();
        List<FolderStore> folderStores = Lists.newArrayList(fldrs);
        return folderStores;
    }

    private static String getLockCaller() {
        StackTraceElement elt = Thread.currentThread().getStackTrace()[3];
        String[] classNameParts = elt.getClassName().split("\\.");
        String className = classNameParts[classNameParts.length - 1];
        return String.format("%s::%s", className, elt.getMethodName());
    }

    @Override
    public MailboxLock getWriteLockAndLockIt() throws ServiceException {
        return getWriteLockAndLockIt(getLockCaller());
    }

    public MailboxLock getWriteLockAndLockIt(String caller) throws ServiceException {
        return lockFactory.acquiredWriteLock(new MailboxLockContext(this, caller));
    }

    @Override
    public MailboxLock getReadLockAndLockIt() throws ServiceException {
        return getReadLockAndLockIt(getLockCaller());
    }

    public MailboxLock getReadLockAndLockIt(String caller) throws ServiceException {
        MailboxLockContext lockContext = new MailboxLockContext(this, caller);
        return requiresWriteLock() ? lockFactory.acquiredWriteLock(lockContext) : lockFactory.acquiredReadLock(lockContext);
    }

    public boolean isWriteLockedByCurrentThread() throws ServiceException {
        return lockFactory.writeLock(new MailboxLockContext(this)).isWriteLockedByCurrentThread();
    }

    @Override
    public ZimbraSearchParams createSearchParams(String queryString) {
        SearchParams sp = new SearchParams();
        sp.setQueryString(queryString);
        return sp;
    }

    @SuppressWarnings("resource")
    @Override
    public ZimbraQueryHitResults searchImap(OpContext octx, ZimbraSearchParams params)
    throws ServiceException {
        ZimbraQueryResults zqr =
                index.search(SoapProtocol.Soap12, OperationContext.asOperationContext(octx), (SearchParams) params);
        return new LocalQueryHitResults(zqr);
    }

    @Override
    public void noOp() throws ServiceException {
        // do nothing
    }

    private SearchHistory getSearchHistory() throws ServiceException {
        return SearchHistory.getFactory().getSearchHistory(getAccount());
    }

    public boolean addToSearchHistory(OperationContext octxt, String searchString, long timestamp) throws ServiceException {
        AddSearchHistoryEntry redoRecorder = new AddSearchHistoryEntry(mId, searchString);
        try (final MailboxTransaction t = mailboxWriteTransaction("addToSearchHistory", octxt, redoRecorder)){
            AddSearchHistoryEntry redoPlayer = (AddSearchHistoryEntry) currentChange().getRedoPlayer();
            boolean isRedo = redoPlayer != null;
            SearchHistory searchHistory = getSearchHistory();
            if (!searchHistory.contains(searchString)) {
                int searchId = getNextSearchId(isRedo ? redoPlayer.getSearchId() : ID_AUTO_INCREMENT);
                redoRecorder.setSearchId(searchId);
                ZimbraLog.search.debug("creating new search history entry '%s' with ID %d", searchString, searchId);
                searchHistory.registerSearch(searchId, searchString);
            }
            ZimbraLog.search.debug("adding '%s' to search history", searchString);
            searchHistory.logSearch(searchString, timestamp);
            boolean shouldPrompt;
            if (isRedo) {
                shouldPrompt = redoPlayer.isPrompted();
            } else {
                int numSearchesForPrompt = getAccount().getNumSearchesForSavedSearchPrompt();
                if (numSearchesForPrompt == 0) {
                    shouldPrompt = false;
                } else {
                    int searchCount = getSearchHistoryCount(octxt, searchString);
                    SavedSearchStatus curStatus = getSavedSearchPromptStatus(octxt, searchString);
                    shouldPrompt = searchCount >= numSearchesForPrompt && curStatus == SavedSearchStatus.NOT_PROMPTED;
                }
            }
            if (shouldPrompt) {
                SavedSearchPromptLog log = searchHistory.getPromptLog();
                log.setPromptStatus(searchString, SavedSearchStatus.PROMPTED);
                redoRecorder.setPrompted(true);
            }
            t.commit();
            return shouldPrompt;
        }
    }

    public List<String> getSearchHistory(OperationContext octxt, SearchHistoryParams params) throws ServiceException {
        try (final MailboxTransaction t = mailboxReadTransaction("getSearchHistory", octxt)) {
            SearchHistory searchHistory = getSearchHistory();
            List<String> searchStrings = searchHistory.search(params);
            t.commit();
            return searchStrings;
        }
    }

    public void purgeSearchHistory(OperationContext octxt) throws ServiceException {
        PurgeSearchHistory redoRecorder = new PurgeSearchHistory(mId);
        try (final MailboxTransaction t = mailboxWriteTransaction("purgeSearchHistory", octxt, redoRecorder)) {
            PurgeSearchHistory redoPlayer = (PurgeSearchHistory) currentChange().getRedoPlayer();
            boolean isRedo = redoPlayer != null;
            SearchHistory searchHistory = getSearchHistory();
            long maxAgeMillis = getAccount().getSearchHistoryAge();
            if (isRedo) {
                maxAgeMillis += redoPlayer.getTimestamp();
            }
            searchHistory.purgeHistory(maxAgeMillis);
            t.commit();
        }
    }

    public void deleteSearchHistory(OperationContext octxt) throws ServiceException {
        DeleteMailbox redoRecorder = new DeleteMailbox(mId);
        try (final MailboxTransaction t = mailboxWriteTransaction("deleteSearchHistory", octxt, redoRecorder)) {
            SearchHistory searchHistory = getSearchHistory();
            searchHistory.deleteHistory();
            t.commit();
        }
    }

    public int getSearchHistoryCount(OperationContext octxt, String searchString) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("searchHistoryCount", octxt)) {
            SearchHistory searchHistory = getSearchHistory();
            int count = searchHistory.getCount(searchString);
            t.commit();
            return count;
        }
    }

    public SavedSearchStatus getSavedSearchPromptStatus(OperationContext octxt, String searchString) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("getSavedSearchStatus", octxt)) {
            SearchHistory searchHistory = getSearchHistory();
            SavedSearchPromptLog log = searchHistory.getPromptLog();
            SavedSearchStatus status = log.getSavedSearchStatus(searchString);
            t.commit();
            return status;
        }
    }

    public void setSavedSearchPromptStatus(OperationContext octxt, String searchString, SavedSearchStatus status) throws ServiceException {
        SetSavedSearchStatus redoRecorder = new SetSavedSearchStatus(mId, searchString, status);
        try (final MailboxTransaction t = mailboxWriteTransaction("setSavedSearchStatus", octxt, redoRecorder)) {
            SearchHistory searchHistory = getSearchHistory();
            SavedSearchPromptLog log = searchHistory.getPromptLog();
            log.setPromptStatus(searchString, status);
            t.commit();
        }
    }

    public List<Integer> advanceMessageEventFlags(OperationContext octxt, List<Message> messages, Message.EventFlag eventFlag) throws ServiceException {
        try (final MailboxTransaction t = mailboxWriteTransaction("advanceMessageEventFlags", octxt)) {
            List<Integer> affectedMsgIds = advanceMessageEventFlags(messages, eventFlag);
            t.commit();
            return affectedMsgIds;
        }
    }

    private List<Integer> advanceMessageEventFlags(List<Message> messages, Message.EventFlag eventFlag) throws ServiceException {
            List<Integer> affectedMsgIds = new ArrayList<>();
            for (Message msg: messages) {
                if (msg.advanceEventFlag(eventFlag)) {
                    affectedMsgIds.add(msg.getId());
                }
            }
            //only update the DB rows for those messages that had their event flag advanced
            if (!affectedMsgIds.isEmpty()) {
                DbMailItem.setEventFlagsIfNecessary(this, affectedMsgIds, eventFlag);
            }
            return affectedMsgIds;
    }

    public void markMsgSeen(OperationContext octxt, Message msg) throws ServiceException {
        if (!EventLogger.getEventLogger().isEnabled()) {
            return;
        }
        advanceMessageEventFlags(octxt, ImmutableList.of(msg), EventFlag.seen);
    }

    /**
     * This method is used when the event flag update happens from within another transaction,
     * such as marking a message as "read" or "replied". In these cases, we don't need to acquire a new write lock.
     */
    void advanceMessageEventFlag(Message msg, EventFlag eventFlag) throws ServiceException {
        advanceMessageEventFlags(ImmutableList.of(msg), eventFlag);
    }

    @Override
    public void markMsgSeen(OpContext octxt, ItemIdentifier itemId) throws ServiceException {
        markMsgSeen(OperationContext.asOperationContext(octxt), getMessageById(itemId.id));
    }

    public void syncSmartFolders(OperationContext octxt, SmartFolderProvider provider) throws ServiceException {
        Set<String> existing = getSmartFolders(octxt).stream().map(sf -> sf.getSmartFolderName()).collect(Collectors.toSet());
        Set<String> provided = provider.getSmartFolderNames();
        Set<String> toAdd = Sets.difference(provided, existing);
        Set<String> toDelete = Sets.difference(existing, provided);
        for (String name: toAdd){
            createSmartFolder(octxt, name);
        }
        for (String name: toDelete) {
            SmartFolder sf = getSmartFolder(octxt, name);
            delete(null, sf.getId(), MailItem.Type.SMARTFOLDER);
        }
    }

    public SmartFolder createSmartFolder(OperationContext octxt, String smartFolderName) throws ServiceException {
        if (smartFolderName.isEmpty()) {
            throw ServiceException.FAILURE("Cannot have an empty SmartFolder name", null);
        }
        //check if this smart folder already exists
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            if (mTagCache.contains(SmartFolder.getInternalTagName(smartFolderName).toLowerCase())) {
                throw MailServiceException.ALREADY_EXISTS(smartFolderName);
            }
        }

        CreateSmartFolder redoRecorder = new CreateSmartFolder(mId, smartFolderName);

        try (final MailboxTransaction t = mailboxWriteTransaction("createSmartFolder", octxt)) {
            CreateSmartFolder redoPlayer = (CreateSmartFolder) currentChange().getRedoPlayer();
            int smartFolderId = redoPlayer == null ? ID_AUTO_INCREMENT : redoPlayer.getId();
            SmartFolder folder = SmartFolder.create(this, getNextItemId(smartFolderId), smartFolderName);
            redoRecorder.setId(folder.getId());
            t.commit();
            return folder;
        }
    }

    public List<SmartFolder> getSmartFolders(OperationContext octxt) throws ServiceException {
        List<SmartFolder> smartFolders = new ArrayList<>();
        for (MailItem item : getItemList(octxt, MailItem.Type.SMARTFOLDER)) {
            smartFolders.add((SmartFolder) item);
        }
        return smartFolders;
    }

    public SmartFolder getSmartFolder(OperationContext octxt, String name) throws ServiceException {
        if (Strings.isNullOrEmpty(name)) {
            throw ServiceException.INVALID_REQUEST("smart folder name may not be null", null);
        }

        try (final MailboxTransaction t = mailboxWriteTransaction("getSmartFolderByName", octxt)) {
            if (!hasFullAccess()) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
            SmartFolder sf = (SmartFolder) checkAccess(getTagByName(SmartFolder.getInternalTagName(name)));
            t.commit();
            return sf;
        }
    }

    private boolean isListedTagOrSmartFolder(MailItem item) {
        if (item instanceof Tag) {
            Tag tag = (Tag) item;
            return tag.isListed() || tag instanceof SmartFolder;
        } else {
            return false;
        }
    }

    public interface MessageCallback {

        public static enum Type {
            sent, received
        }

        public void execute(Message msg, MessageCallbackContext ctxt);
    }

    public class SentMessageCallback implements MessageCallback {

        @Override
        public void execute(Message msg, MessageCallbackContext ctxt) {
            EventLogger eventLogger = EventLogger.getEventLogger();
            if (eventLogger.isEnabled()) {
                long timestamp = ctxt.getTimestamp() == null ? System.currentTimeMillis() : ctxt.getTimestamp();
                List<Event> events = Event.generateSentEvents(msg, timestamp);
                eventLogger.log(events);
            }
        }
    }

    public class ReceivedMessageCallback implements MessageCallback {

        @Override
        public void execute(Message msg, MessageCallbackContext ctxt) {
            String recipient = ctxt.getRecipient();
            if (Strings.isNullOrEmpty(recipient)) {
                ZimbraLog.event.warn("no recipient specified for message %d", msg.getId());
            } else {
                long timestamp = ctxt.getTimestamp() == null ? System.currentTimeMillis() : ctxt.getTimestamp();
                EventLogger logger = EventLogger.getEventLogger();
                if (logger.isEnabled()) {
                    logger.log(Event.generateReceivedEvent(msg, recipient, timestamp));
                    try {
                        if (getAccount().isContactAffinityEventLoggingEnabled()) {
                            logger.log(Event.generateAffinityEvents(msg, recipient, timestamp));
                        }
                    } catch (ServiceException e) {
                        ZimbraLog.event.error("unable to determine whether AFFINITY event logging is enabled", e);
                    }
                }
            }
            try {
                ClassificationExecutionContext<Message> executionContext = ClassifierManager.getInstance().getExecutionContext(msg);
                if (executionContext.hasResolvedTasks()) {
                    executionContext.execute(msg);
                }
            } catch (ServiceException e) {
                ZimbraLog.ml.error("unable to classify message %s", msg.getId(), e);
            }
        }
    }

    public MailboxTransaction mailboxReadTransaction(String caller, OperationContext octxt)
            throws ServiceException {
        return new MailboxTransaction(caller, System.currentTimeMillis(), octxt, false /* write */,
                (RedoableOp)null, (DbConnection)null);
    }

    public MailboxTransaction mailboxReadTransaction(String caller, OperationContext octxt,
            RedoableOp recorder) throws ServiceException {
        return new MailboxTransaction(caller, System.currentTimeMillis(), octxt, false /* write */,
                recorder, (DbConnection)null);
    }

    public MailboxTransaction mailboxWriteTransaction(String caller, OperationContext octxt)
            throws ServiceException {
        return new MailboxTransaction(caller, System.currentTimeMillis(), octxt, true /* write */,
                (RedoableOp)null, (DbConnection)null);
    }

    public MailboxTransaction mailboxWriteTransaction(String caller, OperationContext octxt,
            RedoableOp recorder) throws ServiceException {
        return new MailboxTransaction(caller, System.currentTimeMillis(), octxt, true /* write */,
                recorder, (DbConnection)null);
    }

    public MailboxTransaction mailboxWriteTransaction(String caller, OperationContext octxt,
            RedoableOp recorder, DbConnection conn) throws ServiceException {
        return new MailboxTransaction(caller, System.currentTimeMillis(), octxt, true /* write */,
                recorder, conn);
    }

    public class MailboxTransaction implements AutoCloseable {
        private final MailboxLock lock;
        private boolean success = false;
        private boolean startedChange = false;
        private final long startTime;
        private final String myCaller;

        private MailboxTransaction(String caller, long time, OperationContext octxt, boolean definitelyWrite,
                RedoableOp recorder, DbConnection conn) throws ServiceException {
            startTime = time;
            myCaller = caller;

            boolean write = (definitelyWrite || requiresWriteLock());
            MailboxLockContext lockContext = new MailboxLockContext(Mailbox.this, caller);
            this.lock = write ? lockFactory.writeLock(lockContext) : lockFactory.readLock(lockContext);
            assert recorder == null || write;
            try {
                this.lock.lock();
                if (!write && requiresWriteLock()) {
                    //another call must have purged the cache.
                    //the lock.lock() call should have resulted in write lock already
                    assert(lock.isWriteLockedByCurrentThread());
                    write = true;
                }
                boolean newChange = currentChange().startChange(caller, octxt, recorder, write);
                Iterator<WeakReference<TransactionListener>> itr = Mailbox.this.transactionListeners.iterator();
                while (itr.hasNext()) {
                    TransactionListener listener = itr.next().get();
                    if (listener == null) {
                        itr.remove();
                    }
                    else {
                        listener.transactionBegin(newChange);
                    }
                }

                startedChange = true;

                // if a Connection object was provided, use it
                if (conn != null) {
                    setOperationConnection(conn);
                }
                boolean needRedo = needRedo(octxt, recorder);
                // have a single, consistent timestamp for anything affected by this operation
                currentChange().setTimestamp(time);
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

                if (mItemCache == null) {
                    mItemCache = ItemCache.getFactory().getItemCache(Mailbox.this, Mailbox.this.cacheTracker);
                    ZimbraLog.cache.debug("created a new MailItem cache for mailbox %s %s", getId(), this);
                }
                currentChange().itemCache = mItemCache;

                // don't permit mailbox access during maintenance
                if (maintenance != null && !maintenance.canAccess()) {
                    throw MailServiceException.MAINTENANCE(mId);
                }
                // we can only start a redoable operation as the transaction's base change
                if (recorder != null && needRedo && currentChange().depth > 1) {
                    throw ServiceException.FAILURE(String.format(
                            "cannot start a logged transaction from within another transaction %s %s",
                            currentChange(), this), null);
                }
                // we'll need folders and tags loaded in order to handle ACLs
                loadFoldersAndTags();
            } catch (Exception ex) {
                ZimbraLog.mailbox.debug("constructor Exception %s - calling close() to cleanup %s",
                        ex.getMessage(), this);
                close();
                throw ex;
            }
        }

        /**
         * DOES NOT ACTUALLY COMMIT. But does mark the transaction as successful so that it will be
         * committed when close is called
         */
        public void commit() {
            this.success = true;
        }

        /**
         * DOES NOT ACTUALLY ROLLBACK. But does mark the transaction as failed so that it will NOT be
         * committed when close is called
         */
        public void rollback() {
            this.success = false;
        }

        /**
         * Be very careful when changing code in this method.  The order of almost
         * every line of code is important to ensure correct redo logging and crash
         * recovery.
         *
         * @throws ServiceException error
         */
        @Override
        public void close() throws ServiceException {
            try {
                if (!lock.isLockedByCurrentThread()) {
                    ZimbraLog.mailbox.warn("transaction canceled because of lock failure %s %s",
                            this, ZimbraLog.getStackTrace(16));
                    if (!startedChange) {
                        /* if we haven't started a change yet, shouldn't be any further cleanup needed. */
                        return;
                    }
                    /* This should cause rollback, cleanup of currentChange etc.  If we don't do
                     * any needed tidyup here, things can get very confusing (change depth counter etc). */
                    success = false;
                }
            } catch (ServiceException e) {
                //if there is a problem calling isLockedByCallingThread, abort the transaction
                ZimbraLog.mailbox.warn("transaction canceled because of lock failure %s %s",
                        this, ZimbraLog.getStackTrace(16));
                if (!startedChange) {
                    return;
                }
                success = false;
            }
            PendingDelete deletes = null; // blob and index to delete
            List<Object> rollbackDeletes = null; // blob to delete for failure cases
            ChangeNotification changeNotification = null;
            try {
                if (!currentChange().isActive()) {
                    // would like to throw here, but it might cover another
                    // exception...
                    ZimbraLog.mailbox.warn("cannot end a transaction when not inside a transaction %s",
                            this, new Exception());
                    return;
                }
                if (!currentChange().endChange()) {
                    return;
                }
                ServiceException exception = null;

                if (this.success) {
                    // update mailbox size, folder unread/message counts
                    try {
                        snapshotCounts();
                    } catch (ServiceException e) {
                        exception = e;
                        this.rollback();
                    } catch (Exception e2) {
                        exception = ServiceException.FAILURE("uncaught exception during transaction close, rolling back transaction", e2);
                        this.rollback();
                    }
                }

                DbConnection conn = currentChange().conn;

                // Failure case is very simple.  Just rollback the database and cache
                // and return.  We haven't logged anything to the redo log for this
                // transaction, so no redo cleanup is necessary.
                if (!this.success) {
                    DbPool.quietRollback(conn);
                    rollbackDeletes = rollbackCache(currentChange());
                    if (exception != null) {
                        throw exception;
                    }
                    return;
                }

                RedoableOp redoRecorder = currentChange().recorder;
                boolean needRedo = needRedo(currentChange().octxt, redoRecorder);
                // Log the change redo record for main transaction.
                if (redoRecorder != null && needRedo) {
                    redoRecorder.log(true);
                }
                boolean dbCommitSuccess = false;
                boolean doRollback = true;
                try {
                    // Commit the main transaction in database.
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (ServiceException.TransactionRollbackException tre) {
                            ZimbraLog.mailbox.warn("Mailbox Transaction Rolled Back by DB", tre);
                            doRollback = false;
                            throw tre;
                        } catch (Throwable t) {
                            ZimbraLog.mailbox.error(
                            "!!!ERROR!!! Unable to commit database transaction.  Unknown error encountered.", t);
                            /* Used to call Zimbra.halt() here on the basis that things could be tidied up
                             * on restart but in a multi-replica environment, that isn't realistic, so shouting
                             * in the logs and propagating the error. */
                            throw t;
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
                        if (doRollback) {
                            DbPool.quietRollback(conn);
                        }
                        rollbackDeletes = rollbackCache(currentChange());
                        /* current assumption is that code will throw an exception if we are here.
                         * Code used to return here which could potentially mask an error. */
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
                        if (currentChange().dirty != null && !currentChange().dirty.changedTypes.isEmpty()) {
                            // if an "all accounts" waitset is active, and this change has an appropriate type,
                            // then we'll need to set a commit-callback
                            AllAccountsRedoCommitCallback cb = AllAccountsRedoCommitCallback.getRedoCallbackIfNecessary(
                                    getAccountId(), currentChange().dirty.changedTypes);
                            if (cb != null) {
                                redoRecorder.setCommitCallback(cb);
                            }
                        }
                        redoRecorder.commit();
                    }
                }

                MailboxChange change = currentChange();
                boolean changeMade = change.changeId != MailboxChange.NO_CHANGE;
                deletes = currentChange().deletes; // keep a reference for cleanup
                                                   // deletes outside the lock

                // We are finally done with database and redo commits. Cache update comes last.
                changeNotification = commitCache(currentChange(), lock);
            } finally {
                Iterator<WeakReference<TransactionListener>> itr = Mailbox.this.transactionListeners.iterator();
                while (itr.hasNext()) {
                    TransactionListener listener = itr.next().get();
                    if (listener == null) {
                        itr.remove();
                    }
                    else {
                        try {
                            listener.transactionEnd(success, currentChange().depth == 0);
                        } catch (Exception e) {
                            //don't  want errors here to prevent releasing lock
                            ZimbraLog.mailbox.error("error invoking TransactionListener in transaction end!", e);
                        }
                    }
                }
                lock.close();
                // notify listeners outside lock as can take significant time
                if (changeNotification != null) {
                    notifyListeners(currentChange(), changeNotification, lock);
                }
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

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("caller", myCaller)
                .add("startTime", new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(startTime)))
                .add("success", success)
                .add("lock", lock)
                .omitNullValues()
                .toString();
        }
    }

    public NotificationPubSub getNotificationPubSub() {
        return NotificationPubSub.getFactory().getNotificationPubSub(this);
    }

    private FolderCache copyFolderCache() throws ServiceException {
        FolderCache copy = new LocalFolderCache();
        try(FolderTagCacheLock lock = ftCacheReadLock.lock()) {
            for (Folder folder : mFolderCache.values()) {
                MailItem.UnderlyingData data = folder.getUnderlyingData().clone();
                data.setFlag(Flag.FlagInfo.UNCACHED);
                data.metadata = folder.encodeMetadata().toString();
                copy.put((Folder) MailItem.constructItem(folder.getMailbox(), data));
            }
        }
        for (Folder folder : copy.values()) {
            Folder parent = copy.get(folder.getFolderId());
            if (parent != null) {
                parent.addChild(folder, false);
            }
        }
        return copy;
    }

    /** This method resets default calendar id to ID_FOLDER_CALENDAR in pref */
    public void resetDefaultCalendarId() throws ServiceException {
        getAccount().setPrefDefaultCalendarId(ID_FOLDER_CALENDAR);
    }

    public void addTransactionListener(TransactionListener listener) {
        transactionListeners.add(new WeakReference<>(listener));
    }

    public CachedObjectRegistry getCachedObjects() {
        return cacheTracker == null ? null : cacheTracker.getCachedObjects();
    }

    public void clearStaleCaches(MailboxLockContext context) throws ServiceException {
        String caller = context.getCaller();
        if (caller != null && caller.equals("createMailbox")) {
            //mailbox creation will always reach this point (since there is no lock marker in redis),
            //but we don't actually need to flush any caches
            return;
        }
        if (!LC.redis_cache_synchronize_folders_tags.booleanValue()) {
            clearFolderCache();
            clearTagCache();
        }
        if (!LC.redis_cache_synchronize_mailbox_state.booleanValue()) {
            state.reload();
        }
    }

    private class FolderTagCacheReadWriteLock {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final FolderTagCacheLock readLock = new FolderTagCacheLock(lock.readLock());
        private final FolderTagCacheLock writeLock = new FolderTagCacheLock(lock.writeLock());

        public FolderTagCacheLock readLock() {
            return readLock;
        }

        public FolderTagCacheLock writeLock() {
            return writeLock;
        }
    }

    private class FolderTagCacheLock implements AutoCloseable {

        private final Lock lock;
        private final int timeout;

        private FolderTagCacheLock(Lock lock) {
            this.lock = lock;
            this.timeout = LC.folder_tag_cache_lock_timeout_seconds.intValue();
        }

        public FolderTagCacheLock lock() throws ServiceException {
            try {
                boolean gotLock = lock.tryLock(timeout, TimeUnit.SECONDS);
                if (!gotLock) {
                    throw ServiceException.FAILURE(String.format("unable to get folder/tag cache lock %s", lock), null);
                }
            } catch (InterruptedException e) {
                throw ServiceException.FAILURE(String.format("unable to get folder/tag cache lock %s", lock), e);
            }
            return this;
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }
}
