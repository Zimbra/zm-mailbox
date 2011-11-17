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

package com.zimbra.cs.db;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.BCodec;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.PendingDelete;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.VirtualConversation;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.pop3.Pop3Message;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

/**
 * DAO for MAIL_ITEM table.
 *
 * @since Aug 13, 2004
 */
public class DbMailItem {
    public static final String TABLE_MAIL_ITEM = "mail_item";
    public static final String TABLE_MAIL_ITEM_DUMPSTER = "mail_item_dumpster";
    public static final String TABLE_REVISION = "revision";
    public static final String TABLE_REVISION_DUMPSTER = "revision_dumpster";
    public static final String TABLE_APPOINTMENT = "appointment";
    public static final String TABLE_APPOINTMENT_DUMPSTER = "appointment_dumpster";
    public static final String TABLE_OPEN_CONVERSATION = "open_conversation";
    public static final String TABLE_TOMBSTONE = "tombstone";

    public static final int MAX_SENDER_LENGTH  = 128;
    public static final int MAX_RECIPIENTS_LENGTH = 128;
    public static final int MAX_SUBJECT_LENGTH = 1024;
    public static final int MAX_TEXT_LENGTH    = 65534;
    public static final int MAX_MEDIUMTEXT_LENGTH = 16777216;

    public static final String IN_THIS_MAILBOX_AND = DebugConfig.disableMailboxGroups ? "" : "mailbox_id = ? AND ";
    public static final String MAILBOX_ID = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
    public static final String MAILBOX_ID_VALUE = DebugConfig.disableMailboxGroups ? "" : "?, ";

    static final int RESULTS_STREAMING_MIN_ROWS = 10000;

    public static final int setMailboxId(PreparedStatement stmt, Mailbox mbox, int pos) throws SQLException {
        int nextPos = pos;
        if (!DebugConfig.disableMailboxGroups) {
            stmt.setInt(nextPos++, mbox.getId());
        }
        return nextPos;
    }

    public void create(UnderlyingData data) throws ServiceException {
        if (data.id <= 0 || data.folderId <= 0 || data.parentId == 0) {
            throw ServiceException.FAILURE("invalid data for DB item create", null);
        }
        assert mailbox.isNewItemIdValid(data.id) : "[bug 46549] illegal id for mail item";   //temporarily for bug 46549
        checkNamingConstraint(mailbox, data.folderId, data.name, data.id);

        DbConnection conn = mailbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            MailItem.Type type = MailItem.Type.of(data.type);

            stmt = conn.prepareStatement("INSERT INTO " + getMailItemTableName(mailbox) + "(" + MAILBOX_ID +
                    " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest, unread," +
                    " flags, tag_names, sender, recipients, subject, name, metadata, mod_metadata, change_date," +
                    " mod_content) VALUES (" + MAILBOX_ID_VALUE +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mailbox, pos);
            stmt.setInt(pos++, data.id);
            stmt.setByte(pos++, data.type);
            if (data.parentId <= 0) {
                // Messages in virtual conversations are stored with a null parent_id
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, data.parentId);
            }
            stmt.setInt(pos++, data.folderId);
            if (data.indexId == MailItem.IndexStatus.NO.id()) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, data.indexId);
            }
            if (data.imapId <= 0) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, data.imapId);
            }
            stmt.setInt(pos++, data.date);
            stmt.setLong(pos++, data.size);
            if (data.locator != null) {
                stmt.setString(pos++, data.locator);
            } else {
                stmt.setNull(pos++, Types.VARCHAR);
            }
            stmt.setString(pos++, data.getBlobDigest());
            switch (type) {
                case MESSAGE:
                case CHAT:
                case FOLDER:
                    stmt.setInt(pos++, data.unreadCount);
                    break;
                default:
                    stmt.setNull(pos++, Types.INTEGER);
                    break;
            }
            stmt.setInt(pos++, data.getFlags());
            stmt.setString(pos++, DbTag.serializeTags(data.getTags()));
            stmt.setString(pos++, sender);
            stmt.setString(pos++, recipients);
            stmt.setString(pos++, data.getSubject());
            stmt.setString(pos++, data.name);
            stmt.setString(pos++, checkMetadataLength(data.metadata));
            stmt.setInt(pos++, data.modMetadata);
            if (data.dateChanged > 0) {
                stmt.setInt(pos++, data.dateChanged);
            } else {
                stmt.setNull(pos++, Types.INTEGER);
            }
            stmt.setInt(pos++, data.modContent);
            int num = stmt.executeUpdate();
            if (num != 1) {
                throw ServiceException.FAILURE("failed to create object", null);
            }

            DbTag.storeTagReferences(mailbox, data.id, type, data.getFlags(), data.unreadCount > 0);
            DbTag.storeTagReferences(mailbox, data.id, type, data.getTags());
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(data.id, e);
            } else {
                throw ServiceException.FAILURE("Failed to create id=" + data.id + ",type=" + data.type, e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static void checkNamingConstraint(Mailbox mbox, int folderId, String name, int modifiedItemId) throws ServiceException {
        if (name == null || name.equals(""))
            return;
        if (Db.supports(Db.Capability.UNIQUE_NAME_INDEX) && !Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON))
            return;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + getMailItemTableName(mbox) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND id <> ? AND " + Db.equalsSTRING("name"));
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folderId);
            stmt.setInt(pos++, modifiedItemId);
            stmt.setString(pos++, StringUtil.trimTrailingSpaces(name));
            rs = stmt.executeQuery();
            if (!rs.next() || rs.getInt(1) > 0)
                throw MailServiceException.ALREADY_EXISTS(name);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("checking for naming conflicts", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void copy(MailItem item, int id, Folder folder, int indexId, int parentId, String locator, String metadata, boolean fromDumpster)
    throws ServiceException {
        Mailbox mbox = item.getMailbox();
        if (id <= 0 || folder == null || parentId == 0) {
            throw ServiceException.FAILURE("invalid data for DB item copy", null);
        }

        checkNamingConstraint(mbox, folder.getId(), item.getName(), id);

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String srcTable = getMailItemTableName(mbox, fromDumpster);
            String destTable = getMailItemTableName(mbox);
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement("INSERT INTO " + destTable +
                        "(" + mailbox_id +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tag_names, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "SELECT " + MAILBOX_ID_VALUE +
                        " ?, type, ?, ?, ?, ?, date, size, ?, blob_digest, unread," +
                        " flags, tag_names, sender, subject, name, ?, ?, ?, ? FROM " + srcTable +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, id);                            // ID
            if (parentId <= 0) {
                stmt.setNull(pos++, Types.INTEGER);            // PARENT_ID null for messages in virtual convs
            } else {
                stmt.setInt(pos++, parentId);                  //   or, PARENT_ID specified by caller
            }
            stmt.setInt(pos++, folder.getId());                // FOLDER_ID
            if (indexId == MailItem.IndexStatus.NO.id()) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, indexId);
            }
            stmt.setInt(pos++, id);                            // IMAP_ID is initially the same as ID
            if (locator != null) {
                stmt.setString(pos++, locator);                // VOLUME_ID specified by caller
            } else {
                stmt.setNull(pos++, Types.VARCHAR);            //   or, no VOLUME_ID
            }
            stmt.setString(pos++, checkMetadataLength(metadata));  // METADATA
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_METADATA
            stmt.setInt(pos++, mbox.getOperationTimestamp());  // CHANGE_DATE
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_CONTENT
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            int num = stmt.executeUpdate();
            if (num != 1) {
                throw ServiceException.FAILURE("failed to create object", null);
            }

            DbTag.storeTagReferences(mbox, id, item.getType(), item.getInternalFlagBitmask(), item.isUnread());
            DbTag.storeTagReferences(mbox, id, item.getType(), item.getTags());
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(id, e);
            } else {
                throw ServiceException.FAILURE("copying " + item.getType() + ": " + item.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void copyCalendarItem(CalendarItem calItem, int newId, boolean fromDumpster)
    throws ServiceException {
        Mailbox mbox = calItem.getMailbox();
        if (newId <= 0) {
            throw ServiceException.FAILURE("invalid data for DB item copy", null);
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement("INSERT INTO " + getCalendarItemTableName(mbox) +
                    "(" + mailbox_id +
                    " uid, item_id, start_time, end_time) " +
                    "SELECT " + MAILBOX_ID_VALUE +
                    " uid, ?, start_time, end_time FROM " + getCalendarItemTableName(mbox, fromDumpster) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "item_id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, newId);
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, calItem.getId());
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to create object", null);
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(newId, e);
            } else {
                throw ServiceException.FAILURE("copying " + calItem.getType() + ": " + calItem.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void copyRevision(MailItem revision, int newId, String locator, boolean fromDumpster)
    throws ServiceException {
        Mailbox mbox = revision.getMailbox();
        if (newId <= 0) {
            throw ServiceException.FAILURE("invalid data for DB item copy", null);
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement("INSERT INTO " + getRevisionTableName(mbox) +
                    "(" + mailbox_id +
                    " item_id, version, date, size, volume_id, blob_digest, name, metadata," +
                    " mod_metadata, change_date, mod_content) " +
                    "SELECT " + MAILBOX_ID_VALUE +
                    " ?, version, date, size, ?, blob_digest, name, metadata," +
                    " mod_metadata, change_date, mod_content" +
                    " FROM " + getRevisionTableName(mbox, fromDumpster) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "item_id = ? AND version = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, newId);
            if (locator != null) {
                stmt.setString(pos++, locator);       // VOLUME_ID specified by caller
            } else {
                stmt.setNull(pos++, Types.VARCHAR);      //   or, no VOLUME_ID
            }
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, revision.getId());
            stmt.setInt(pos++, revision.getVersion());
            int num = stmt.executeUpdate();
            if (num != 1) {
                throw ServiceException.FAILURE("failed to create object", null);
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(newId, e);
            } else {
                throw ServiceException.FAILURE("copying revision " + revision.getVersion() + " for " +
                        revision.getType() + ": " + revision.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void icopy(MailItem source, UnderlyingData data, boolean shared) throws ServiceException {
        Mailbox mbox = source.getMailbox();
        if (data == null || data.id <= 0 || data.folderId <= 0 || data.parentId == 0) {
            throw ServiceException.FAILURE("invalid data for DB item i-copy", null);
        }
        checkNamingConstraint(mbox, data.folderId, source.getName(), data.id);

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String table = getMailItemTableName(mbox);
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            String flags;
            if (!shared) {
                flags = "flags";
            } else if (Db.supports(Db.Capability.BITWISE_OPERATIONS)) {
                flags = "flags | " + Flag.BITMASK_COPIED;
            } else {
                flags = "CASE WHEN " + Db.getInstance().bitAND("flags", String.valueOf(Flag.BITMASK_COPIED)) +
                        " <> 0 THEN flags ELSE flags + " + Flag.BITMASK_COPIED + " END";
            }
            stmt = conn.prepareStatement("INSERT INTO " + table +
                        "(" + mailbox_id +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tag_names, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "SELECT " + mailbox_id +
                        " ?, type, parent_id, ?, ?, ?, date, size, ?, blob_digest," +
                        " unread, " + flags + ", tag_names, sender, subject, name, metadata, ?, ?, ? FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, data.id);                       // ID
            stmt.setInt(pos++, data.folderId);                 // FOLDER_ID
            if (data.indexId == MailItem.IndexStatus.NO.id()) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, data.indexId);
            }
            stmt.setInt(pos++, data.imapId);                   // IMAP_ID
            if (data.locator != null) {
                stmt.setString(pos++, data.locator);           // VOLUME_ID
            } else {
                stmt.setNull(pos++, Types.TINYINT);            //   or, no VOLUME_ID
            }
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_METADATA
            stmt.setInt(pos++, mbox.getOperationTimestamp());  // CHANGE_DATE
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_CONTENT
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, source.getId());
            stmt.executeUpdate();
            stmt.close();

            boolean needsTag = shared && !source.isTagged(Flag.FlagInfo.COPIED);

            if (needsTag || source.getParentId() > 0) {
                boolean altersMODSEQ = source.getParentId() > 0;
                String updateChangeID = (altersMODSEQ ? ", mod_metadata = ?, change_date = ?" : "");
                stmt = conn.prepareStatement("UPDATE " + table +
                            " SET parent_id = NULL, flags = " + flags + updateChangeID +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
                pos = 1;
                if (altersMODSEQ) {
                    stmt.setInt(pos++, mbox.getOperationChangeID());
                    stmt.setInt(pos++, mbox.getOperationTimestamp());
                }
                pos = setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, source.getId());
                stmt.executeUpdate();
                stmt.close();
            }

            if (source instanceof Message && source.getParentId() <= 0) {
                changeOpenTargets(source, data.id);
            }

            MailItem.Type type = MailItem.Type.of(data.type);
            DbTag.storeTagReferences(mbox, data.id, type, data.getFlags() | (shared ? Flag.BITMASK_COPIED : 0), data.unreadCount > 0);
            DbTag.storeTagReferences(mbox, data.id, type, data.getTags());
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(data.id, e);
            } else {
                throw ServiceException.FAILURE("i-copying " + source.getType() + ": " + source.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void snapshotRevision(MailItem item, int version) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(version >= 1);

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String command = Db.supports(Db.Capability.REPLACE_INTO) ? "REPLACE" : "INSERT";
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement(command + " INTO " + getRevisionTableName(mbox) +
                        "(" + mailbox_id + "item_id, version, date, size, volume_id, blob_digest," +
                        " name, metadata, mod_metadata, change_date, mod_content) " +
                        "SELECT " + mailbox_id + "id, ?, date, size, volume_id, blob_digest," +
                        " name, metadata, mod_metadata, change_date, mod_content" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, version);
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(item.getId(), e);
            } else {
                throw ServiceException.FAILURE("saving revision info for " + item.getType() + ": " + item.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void purgeRevisions(MailItem item, int revision, boolean includeOlderRevisions) throws ServiceException {
        if (revision <= 0)
            return;
        Mailbox mbox = item.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getRevisionTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "item_id = ? AND version " +
                        (includeOlderRevisions ? "<= ?" : "= ?"));
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.setInt(pos++, revision);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("purging revisions for " + item.getType() + ": " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void changeType(MailItem item, byte type) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET type = ? WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, type);
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing new type for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void setFolder(MailItem item, Folder folder) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        if (mbox != folder.getMailbox())
            throw MailServiceException.WRONG_MAILBOX();
        checkNamingConstraint(mbox, folder.getId(), item.getName(), item.getId());

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String imapRenumber = mbox.isTrackingImap() ? ", imap_id = CASE WHEN imap_id IS NULL THEN NULL ELSE 0 END" : "";
            int pos = 1;
            boolean hasIndexId = false;
            if (item instanceof Folder) {
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                            " SET parent_id = ?, folder_id = ?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
                stmt.setInt(pos++, folder.getId());
            } else if (item instanceof Conversation && !(item instanceof VirtualConversation)) {
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                            " SET folder_id = ?, mod_metadata = ?, change_date = ?" + imapRenumber +
                            " WHERE " + IN_THIS_MAILBOX_AND + "parent_id = ?");
            } else {
                // set the indexId, in case it changed (moving items out of junk can trigger an index ID change)
                hasIndexId = true;
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                            " SET folder_id = ?, index_id = ?, mod_metadata = ?, change_date = ? " + imapRenumber +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            }
            stmt.setInt(pos++, folder.getId());
            if (hasIndexId) {
                if (item.getIndexStatus() == MailItem.IndexStatus.NO) {
                    stmt.setNull(pos++, Types.INTEGER);
                } else {
                    stmt.setInt(pos++, item.getIndexId());
                }
            }
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item instanceof VirtualConversation ? ((VirtualConversation) item).getMessageId() : item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
                throw MailServiceException.ALREADY_EXISTS(item.getName(), e);
            else
                throw ServiceException.FAILURE("writing new folder data for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void setFolder(List<Message> msgs, Folder folder) throws ServiceException {
        if (msgs == null || msgs.isEmpty())
            return;
        Mailbox mbox = folder.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            // commented out because at present messages cannot have names (and thus can't have naming conflicts)
//            if (!Db.supports(Db.Capability.UNIQUE_NAME_INDEX) || Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON)) {
//                stmt = conn.prepareStatement("SELECT mi.name" +
//                        " FROM " + getMailItemTableName(mbox, "mi") + ", " + getMailItemTableName(mbox, "m2") +
//                        " WHERE mi.id IN " + DbUtil.suitableNumberOfVariables(itemIDs) +
//                        " AND mi.name IS NOT NULL and m2.name IS NOT NULL" +
//                        " AND m2.folder_id = ? AND mi.id <> m2.id" +
//                        " AND " + (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) ? "UPPER(mi.name) = UPPER(m2.name)" : "mi.name = m2.name") +
//                        " AND mi.mailbox_id = ? AND m2.mailbox_id = ?");
//                int pos = 1;
//                for (Message msg : msgs)
//                    stmt.setInt(pos++, msg.getId());
//                stmt.setInt(pos++, folder.getId());
//                stmt.setInt(pos++, mbox.getId());
//                stmt.setInt(pos++, mbox.getId());
//                rs = stmt.executeQuery();
//                if (rs.next())
//                    throw MailServiceException.ALREADY_EXISTS(rs.getString(1));
//                rs.close();
//                stmt.close();
//            }

            String imapRenumber = mbox.isTrackingImap() ? ", imap_id = CASE WHEN imap_id IS NULL THEN NULL ELSE 0 END" : "";
            for (int i = 0; i < msgs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), msgs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(folder) +
                            " SET folder_id = ?, mod_metadata = ?, change_date = ?" + imapRenumber +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                int pos = 1;
                stmt.setInt(pos++, folder.getId());
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, msgs.get(index).getId());
                stmt.executeUpdate();
                stmt.close();
                stmt = null;
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
//            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
//                throw MailServiceException.ALREADY_EXISTS(msgs.toString(), e);
//            else
            throw ServiceException.FAILURE("writing new folder data for messages", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static SetMultimap<MailItem.Type, Integer> getIndexDeferredIds(DbConnection conn, Mailbox mbox)
            throws ServiceException {
        SetMultimap<MailItem.Type, Integer> result = Multimaps.newSetMultimap(
                new EnumMap<MailItem.Type, Collection<Integer>>(MailItem.Type.class),
                new Supplier<Set<Integer>>() {
                    @Override
                    public Set<Integer> get() {
                        return new HashSet<Integer>();
                    }
                });
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try { // from MAIL_ITEM table
            stmt = conn.prepareStatement("SELECT type, id FROM " + getMailItemTableName(mbox, false) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "index_id <= 1"); // 0: deferred, 1: stale
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();
            while (rs.next()) {
                result.put(MailItem.Type.of(rs.getByte(1)), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to query index deferred IDs", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
        if (mbox.dumpsterEnabled()) {
            try { // also from MAIL_ITEM_DUMPSTER table
                stmt = conn.prepareStatement("SELECT type, id FROM " + getMailItemTableName(mbox, true) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "index_id <= 1");
                setMailboxId(stmt, mbox, 1);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    result.put(MailItem.Type.of(rs.getByte(1)), rs.getInt(2));
                }
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Failed to query index deferred IDs from dumpster", e);
            } finally {
                conn.closeQuietly(rs);
                conn.closeQuietly(stmt);
            }
        }
        return result;
    }

    public static List<Integer> getReIndexIds(DbConnection conn, Mailbox mbox, Set<MailItem.Type> types)
            throws ServiceException {
        List<Integer> ids = new ArrayList<Integer>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try { // from MAIL_ITEM table
            stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(mbox, false) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "index_id IS NOT NULL" +
                    (types.isEmpty() ? "" : " AND " + DbUtil.whereIn("type", types.size())));
            int pos = setMailboxId(stmt, mbox, 1);
            for (MailItem.Type type : types) {
                stmt.setByte(pos++, type.toByte());
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to query re-index IDs", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
        if (mbox.dumpsterEnabled()) {
            try { // also from MAIL_ITEM_DUMPSTER table
                stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(mbox, true) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "index_id IS NOT NULL" +
                        (types.isEmpty() ? "" : " AND " + DbUtil.whereIn("type", types.size())));
                int pos = setMailboxId(stmt, mbox, 1);
                for (MailItem.Type type : types) {
                    stmt.setByte(pos++, type.toByte());
                }
                rs = stmt.executeQuery();
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Failed to query re-index IDs from dumpster", e);
            } finally {
                conn.closeQuietly(rs);
                conn.closeQuietly(stmt);
            }
        }
        return ids;
    }

    public static void setIndexIds(DbConnection conn, Mailbox mbox, List<Integer> ids) throws ServiceException {
        if (ids.isEmpty()) {
            return;
        }
        for (int i = 0; i < ids.size(); i += Db.getINClauseBatchSize()) {
            int count = Math.min(Db.getINClauseBatchSize(), ids.size() - i);
            int updated;
            PreparedStatement stmt = null;
            try { // update MAIL_ITEM table
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox, false) +
                        " SET index_id = id WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                int pos = setMailboxId(stmt, mbox, 1);
                for (int j = i; j < i + count; j++) {
                    stmt.setInt(pos++, ids.get(j));
                }
                updated = stmt.executeUpdate();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Failed to set index_id", e);
            } finally {
                conn.closeQuietly(stmt);
            }
            if (updated == count) { // all updates were in MAIL_ITEM table, no need to update MAIL_ITEM_DUMPSTER table
                continue;
            }
            if (mbox.dumpsterEnabled()) {
                try { // also update MAIL_ITEM_DUMPSTER table
                    stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox, true) +
                            " SET index_id = id WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                    int pos = setMailboxId(stmt, mbox, 1);
                    for (int j = i; j < i + count; j++) {
                        stmt.setInt(pos++, ids.get(j));
                    }
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw ServiceException.FAILURE("Failed to set index_id in dumpster", e);
                } finally {
                    conn.closeQuietly(stmt);
                }
            }
        }
    }

    public static void resetIndexId(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        try { // update MAIL_ITEM table
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox, false) +
                    " SET index_id = 0 WHERE " + IN_THIS_MAILBOX_AND + "index_id > 0");
            setMailboxId(stmt, mbox, 1);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to reset index_id", e);
        } finally {
            conn.closeQuietly(stmt);
        }
        if (mbox.dumpsterEnabled()) {
            try { // also update MAIL_ITEM_DUMPSTER table
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox, true) +
                        " SET index_id = 0 WHERE " + IN_THIS_MAILBOX_AND + "index_id > 0");
                setMailboxId(stmt, mbox, 1);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Failed to reset index_id in dumpster", e);
            } finally {
                conn.closeQuietly(stmt);
            }
        }
    }

    public static void setParent(MailItem child, MailItem parent) throws ServiceException {
        setParent(new MailItem[] { child }, parent);
    }

    public static void setParent(MailItem[] children, MailItem parent) throws ServiceException {
        if (children == null || children.length == 0)
            return;
        Mailbox mbox = children[0].getMailbox();
        if (parent != null && mbox != parent.getMailbox())
            throw MailServiceException.WRONG_MAILBOX();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            for (int i = 0; i < children.length; i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), children.length - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET parent_id = ?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                int pos = 1;
                if (parent == null || parent instanceof VirtualConversation)
                    stmt.setNull(pos++, Types.INTEGER);
                else
                    stmt.setInt(pos++, parent.getId());
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, children[index].getId());
                stmt.executeUpdate();
                stmt.close();
                stmt = null;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("adding children to parent " + (parent == null ? "NULL" : parent.getId() + ""), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void reparentChildren(MailItem oldParent, MailItem newParent) throws ServiceException {
        if (oldParent == newParent)
            return;
        Mailbox mbox = oldParent.getMailbox();
        if (mbox != newParent.getMailbox())
            throw MailServiceException.WRONG_MAILBOX();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String relation = (oldParent instanceof VirtualConversation ? "id = ?" : "parent_id = ?");

            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(oldParent) +
                        " SET parent_id = ?, mod_metadata = ?, change_date = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + relation);
            int pos = 1;
            if (newParent instanceof VirtualConversation) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, newParent.getId());
            }
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, oldParent instanceof VirtualConversation ? ((VirtualConversation) oldParent).getMessageId() : oldParent.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing new parent for children of item " + oldParent.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveMetadata(MailItem item, String metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET date = ?, size = ?, metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setLong(pos++, item.getSize());
            stmt.setString(pos++, checkMetadataLength(metadata));
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, item.getSavedSequence());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing metadata for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void persistCounts(MailItem item, Metadata metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET size = ?, unread = ?, metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setLong(pos++, item.getSize());
            stmt.setInt(pos++, item.getUnreadCount());
            stmt.setString(pos++, checkMetadataLength(metadata.toString()));
            stmt.setInt(pos++, item.getModifiedSequence());
            if (item.getChangeDate() > 0)
                stmt.setInt(pos++, (int) (item.getChangeDate() / 1000));
            else
                stmt.setNull(pos++, Types.INTEGER);
            stmt.setInt(pos++, item.getSavedSequence());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing metadata for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    // need to kill the Note class sooner rather than later
    public static void saveSubject(Note note) throws ServiceException {
        Mailbox mbox = note.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(note) +
                        " SET date = ?, size = ?, subject = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (note.getDate() / 1000));
            stmt.setLong(pos++, note.getSize());
            stmt.setString(pos++, note.getSubject());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, note.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing subject for mailbox " + note.getMailboxId() + ", note " + note.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveName(MailItem item, int folderId, Metadata metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        String name = item.getName().isEmpty() ? null : item.getName();
        checkNamingConstraint(mbox, folderId, name, item.getId());

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            boolean isFolder = item instanceof Folder;
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET date = ?, size = ?, flags = ?, name = ?, subject = ?," +
                        "  folder_id = ?," + (isFolder ? " parent_id = ?," : "") +
                        "  metadata = ?, mod_metadata = ?, change_date = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setLong(pos++, item.getSize());
            stmt.setLong(pos++, item.getInternalFlagBitmask());
            stmt.setString(pos++, name);
            stmt.setString(pos++, name);
            stmt.setInt(pos++, folderId);
            if (isFolder) {
                stmt.setInt(pos++, folderId);
            }
            stmt.setString(pos++, metadata.toString());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();

            if (mbox.isItemModified(item, Change.FLAGS)) {
                DbTag.updateTagReferences(mbox, item.getId(), item.getType(), item.getInternalFlagBitmask(),
                        item.isUnread(), item.getTags());
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(name, e);
            } else {
                throw ServiceException.FAILURE("writing name for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public void update(MailItem item, Metadata metadata) throws ServiceException {
        String name = item.getName().isEmpty() ? null : item.getName();
        checkNamingConstraint(mailbox, item.getFolderId(), name, item.getId());

        DbConnection conn = mailbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET type = ?, imap_id = ?, index_id = ?, parent_id = ?, date = ?, size = ?, flags = ?," +
                    "  blob_digest = ?, sender = ?, recipients = ?, subject = ?, name = ?," +
                    "  metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?, volume_id = ?" +
                    " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setByte(pos++, item.getType().toByte());
            if (item.getImapUid() >= 0) {
                stmt.setInt(pos++, item.getImapUid());
            } else {
                stmt.setNull(pos++, Types.INTEGER);
            }
            if (item.getIndexStatus() == MailItem.IndexStatus.NO) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, item.getIndexId());
            }
            // messages in virtual conversations are stored with a null parent_id
            if (item.getParentId() <= 0) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, item.getParentId());
            }
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setLong(pos++, item.getSize());
            stmt.setLong(pos++, item.getInternalFlagBitmask());
            stmt.setString(pos++, item.getDigest());
            stmt.setString(pos++, item.getSortSender());
            stmt.setString(pos++, item.getSortRecipients());
            stmt.setString(pos++, item.getSortSubject());
            stmt.setString(pos++, name);
            stmt.setString(pos++, checkMetadataLength(metadata.toString()));
            stmt.setInt(pos++, mailbox.getOperationChangeID());
            stmt.setInt(pos++, mailbox.getOperationTimestamp());
            stmt.setInt(pos++, item.getSavedSequence());
            if (item.getLocator() != null) {
                stmt.setString(pos++, item.getLocator());
            } else {
                stmt.setNull(pos++, Types.TINYINT);
            }
            pos = setMailboxId(stmt, mailbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();

            if (mailbox.isItemModified(item, Change.FLAGS)) {
                DbTag.updateTagReferences(mailbox, item.getId(), item.getType(), item.getInternalFlagBitmask(),
                        item.isUnread(), item.getTags());
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(item.getName(), e);
            } else {
                throw ServiceException.FAILURE("Failed to update item mbox=" + mailbox.getId() + ",id=" + item.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveBlobInfo(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET size = ?, blob_digest = ?, volume_id = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setLong(pos++, item.getSize());
            stmt.setString(pos++, item.getDigest());
            if (item.getLocator() != null) {
                stmt.setString(pos++, item.getLocator());
            } else {
                stmt.setNull(pos++, Types.TINYINT);
            }
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating blob info for mailbox " + mbox.getId() + ", item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void openConversation(String hash, MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String command = Db.supports(Db.Capability.REPLACE_INTO) ? "REPLACE" : "INSERT";
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement(command + " INTO " + getConversationTableName(item) +
                        "(" + mailbox_id + "hash, conv_id)" +
                        " VALUES (" + (DebugConfig.disableMailboxGroups ? "" : "?, ") + "?, ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, hash);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                try {
                    DbPool.closeStatement(stmt);

                    stmt = conn.prepareStatement("UPDATE " + getConversationTableName(item) +
                            " SET conv_id = ? WHERE " + IN_THIS_MAILBOX_AND + "hash = ?");
                    int pos = 1;
                    stmt.setInt(pos++, item.getId());
                    pos = setMailboxId(stmt, mbox, pos);
                    stmt.setString(pos++, hash);
                    stmt.executeUpdate();
                } catch (SQLException nested) {
                    throw ServiceException.FAILURE("updating open conversation association for hash " + hash, nested);
                }
            } else {
                throw ServiceException.FAILURE("writing open conversation association for hash " + hash, e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void closeConversation(String hash, MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getConversationTableName(item) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "hash = ? AND conv_id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, hash);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("closing open conversation association for hash " + hash, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Deletes rows from <tt>open_conversation</tt> whose items are older than
     * the given date.
     *
     * @param mbox the mailbox
     * @param beforeDate the cutoff date in seconds
     */
    public static void closeOldConversations(Mailbox mbox, int beforeDate) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ZimbraLog.purge.debug("Closing conversations dated before %d.", beforeDate);
        try {
            String mailboxJoin = (DebugConfig.disableMailboxGroups ? "" : " AND mi.mailbox_id = open_conversation.mailbox_id");
            stmt = conn.prepareStatement("DELETE FROM " + getConversationTableName(mbox) +
                " WHERE " + IN_THIS_MAILBOX_AND + "conv_id IN (" +
                "  SELECT id FROM " + getMailItemTableName(mbox, "mi") +
                "  WHERE mi.id = open_conversation.conv_id" +
                   mailboxJoin +
                "  AND date < ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, beforeDate);
            int numRows = stmt.executeUpdate();
            if (numRows > 0) {
                ZimbraLog.purge.info("Closed %d conversations dated before %d.", numRows, beforeDate);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("closing open conversations dated before " + beforeDate, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void changeOpenTargets(MailItem oldTarget, int newTargetId) throws ServiceException {
        Mailbox mbox = oldTarget.getMailbox();
        int oldTargetId = oldTarget instanceof VirtualConversation ? ((VirtualConversation) oldTarget).getMessageId() : oldTarget.getId();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getConversationTableName(oldTarget) +
                        " SET conv_id = ? WHERE " + IN_THIS_MAILBOX_AND + "conv_id = ?");
            int pos = 1;
            stmt.setInt(pos++, newTargetId);
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, oldTargetId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("switching open conversation association for item " + oldTarget.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveDate(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                        " SET date = ?, mod_metadata = ?, change_date = ? WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("setting IMAP UID for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveImapUid(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                        " SET imap_id = ?, mod_metadata = ?, change_date = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, item.getImapUid());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("setting IMAP UID for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void alterUnread(Mailbox mbox, List<Integer> itemIDs, boolean unread)
    throws ServiceException {
        if (itemIDs == null || itemIDs.isEmpty())
            return;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            for (int i = 0; i < itemIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), itemIDs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET unread = ?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "unread = ?" +
                            "  AND " + DbUtil.whereIn("id", count) +
                            "  AND " + typeIn(MailItem.Type.MESSAGE));
                int pos = 1;
                stmt.setInt(pos++, unread ? 1 : 0);
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, unread ? 0 : 1);
                for (int index = i; index < i + count; index++) {
                    stmt.setInt(pos++, itemIDs.get(index));
                }
                stmt.executeUpdate();
                stmt.close();
                stmt = null;

                if (unread) {
                    DbTag.addTaggedItemEntries(mbox, Flag.ID_UNREAD, itemIDs.subList(i, i + count));
                } else {
                    DbTag.removeTaggedItemEntries(mbox, Flag.ID_UNREAD, itemIDs.subList(i, i + count));
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating unread state for " +
                itemIDs.size() + " items: " + getIdListForLogging(itemIDs), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Updates all conversations affected by a folder deletion.  For all conversations
     * that have messages in the given folder, updates their message count and nulls out
     * metadata so that the sender list is recalculated the next time the conversation
     * is instantiated.
     *
     * @param folder the folder that is being deleted
     * @return the ids of any conversation that were purged as a result of this operation
     */
    public static List<Integer> markDeletionTargets(Folder folder, Set<Integer> candidates) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (Db.supports(Db.Capability.MULTITABLE_UPDATE)) {
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(folder) + ", " +
                        "(SELECT parent_id pid, COUNT(*) count FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND parent_id IS NOT NULL GROUP BY parent_id) AS x" +
                        " SET size = size - count, metadata = NULL, mod_metadata = ?, change_date = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = pid AND type = " + MailItem.Type.CONVERSATION.toByte());
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, folder.getId());
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = setMailboxId(stmt, mbox, pos);
                stmt.executeUpdate();
                stmt.close();
            } else {
                stmt = conn.prepareStatement("SELECT parent_id, COUNT(*) FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND parent_id IS NOT NULL" +
                        " GROUP BY parent_id");
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, folder.getId());
                rs = stmt.executeQuery();
                Map<Integer, List<Integer>> counts = new HashMap<Integer, List<Integer>>();
                while (rs.next()) {
                    int convId = rs.getInt(1), count = rs.getInt(2);
                    List<Integer> targets = counts.get(count);
                    if (targets == null)
                        counts.put(count, targets = new ArrayList<Integer>());
                    targets.add(convId);
                }
                rs.close();
                stmt.close();

                for (Map.Entry<Integer, List<Integer>> update : counts.entrySet()) {
                    List<Integer> convIDs = update.getValue();
                    for (int i = 0; i < convIDs.size(); i += Db.getINClauseBatchSize()) {
                        int count = Math.min(Db.getINClauseBatchSize(), convIDs.size() - i);
                        stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(folder) +
                                " SET size = size - ?, metadata = NULL, mod_metadata = ?, change_date = ?" +
                                " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count) +
                                "  AND type = " + MailItem.Type.CONVERSATION.toByte());
                        pos = 1;
                        stmt.setInt(pos++, update.getKey());
                        stmt.setInt(pos++, mbox.getOperationChangeID());
                        stmt.setInt(pos++, mbox.getOperationTimestamp());
                        pos = setMailboxId(stmt, mbox, pos);
                        for (int index = i; index < i + count; index++)
                            stmt.setInt(pos++, convIDs.get(index));
                        stmt.executeUpdate();
                        stmt.close();
                    }
                }
            }

            return getPurgedConversations(mbox, candidates);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("marking deletions for conversations crossing folder " + folder.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Updates all affected conversations when a <code>List</code> of <code>MailItem</code>s
     * is deleted.  Updates each conversation's message count and nulls out
     * metadata so that the sender list is recalculated the next time the conversation
     * is instantiated.
     *
     * @param mbox the mailbox
     * @param ids of the items being deleted
     * @return the ids of any conversation that were purged as a result of this operation
     */
    public static List<Integer> markDeletionTargets(Mailbox mbox, List<Integer> ids, Set<Integer> candidates) throws ServiceException {
        if (ids == null)
            return null;
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String table = getMailItemTableName(mbox);
            if (Db.supports(Db.Capability.MULTITABLE_UPDATE)) {
                for (int i = 0; i < ids.size(); i += Db.getINClauseBatchSize()) {
                    int count = Math.min(Db.getINClauseBatchSize(), ids.size() - i);
                    stmt = conn.prepareStatement("UPDATE " + table + ", " +
                            "(SELECT parent_id pid, COUNT(*) count FROM " + getMailItemTableName(mbox) +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count) +
                            " AND parent_id IS NOT NULL GROUP BY parent_id) AS x" +
                            " SET size = size - count, metadata = NULL, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = pid AND type = " + MailItem.Type.CONVERSATION.toByte());
                    int pos = 1;
                    pos = setMailboxId(stmt, mbox, pos);
                    for (int index = i; index < i + count; index++)
                        stmt.setInt(pos++, ids.get(index));
                    stmt.setInt(pos++, mbox.getOperationChangeID());
                    stmt.setInt(pos++, mbox.getOperationTimestamp());
                    pos = setMailboxId(stmt, mbox, pos);
                    stmt.executeUpdate();
                    stmt.close();
                }
            } else {
                stmt = conn.prepareStatement("SELECT parent_id, COUNT(*) FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", ids.size()) + "AND parent_id IS NOT NULL" +
                        " GROUP BY parent_id");
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int id : ids)
                    stmt.setInt(pos++, id);
                rs = stmt.executeQuery();
                Map<Integer, List<Integer>> counts = new HashMap<Integer, List<Integer>>();
                while (rs.next()) {
                    int convId = rs.getInt(1), count = rs.getInt(2);
                    List<Integer> targets = counts.get(count);
                    if (targets == null)
                        counts.put(count, targets = new ArrayList<Integer>());
                    targets.add(convId);
                }
                rs.close();
                stmt.close();

                for (Map.Entry<Integer, List<Integer>> update : counts.entrySet()) {
                    stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET size = size - ?, metadata = NULL, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", update.getValue().size()) +
                            " AND type = " + MailItem.Type.CONVERSATION.toByte());
                    pos = 1;
                    stmt.setInt(pos++, update.getKey());
                    stmt.setInt(pos++, mbox.getOperationChangeID());
                    stmt.setInt(pos++, mbox.getOperationTimestamp());
                    pos = setMailboxId(stmt, mbox, pos);
                    for (int convId : update.getValue())
                        stmt.setInt(pos++, convId);
                    stmt.executeUpdate();
                    stmt.close();
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("marking deletions for conversations touching " +
                ids.size() + " items: " + getIdListForLogging(ids), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return getPurgedConversations(mbox, candidates);
    }

    private static List<Integer> getPurgedConversations(Mailbox mbox, Set<Integer> candidates) throws ServiceException {
        if (candidates == null || candidates.isEmpty())
            return null;
        List<Integer> purgedConvs = new ArrayList<Integer>();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // note: be somewhat careful making changes here, as <tt>i</tt> and <tt>it</tt> operate separately
            Iterator<Integer> it = candidates.iterator();
            for (int i = 0; i < candidates.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), candidates.size() - i);
                stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(mbox) +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count) + " AND size <= 0");
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, it.next());
                rs = stmt.executeQuery();

                while (rs.next())
                    purgedConvs.add(rs.getInt(1));
                rs.close(); rs = null;
                stmt.close(); stmt = null;
            }

            return purgedConvs;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting list of purged conversations", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Deletes the specified <code>MailItem</code> from the <code>mail_item</code>
     * table.  If the object is a <code>Folder</code> or <code>Conversation</code>,
     * deletes any corresponding messages.  Also deletes all the children
     * items associated to the deleted item, and their children.
     */
    public static void delete(Mailbox mbox, MailItem item, PendingDelete info, boolean fromDumpster)
            throws ServiceException {
        if (item instanceof Tag)
            return;
        List<Integer> allIds = info.itemIds.getAll();
        if (item != null) {
            allIds.remove(Integer.valueOf(item.getId()));
        }
        // first delete the Comments
        List<Integer> allComments = info.itemIds.getIds(MailItem.Type.COMMENT);
        if (allComments != null && allComments.size() != 0) {
            delete(mbox, allComments, fromDumpster);
            allIds.removeAll(allComments);
        }
        // delete all non-folder items
        List<Integer> allFolders = info.itemIds.getIds(MailItem.Type.FOLDER);
        if (allFolders != null) {
            allIds.removeAll(allFolders);
        }
        delete(mbox, allIds, fromDumpster);
        // then delete the folders
        delete(mbox, allFolders, fromDumpster);
        if (item instanceof VirtualConversation)
            return;
        if (item instanceof Conversation && info.incomplete)
            return;
        // delete the item itself
        if (item != null) {
            delete(mbox, Collections.singletonList(item.getId()), fromDumpster);
        }
    }

    /**
     * Deletes <code>MailItem</code>s with the specified ids from the <code>mail_item</code>
     * table.  Assumes that there is no data referencing the specified id's.
     */
    public static void delete(Mailbox mbox, Collection<Integer> ids, boolean fromDumpster) throws ServiceException {
        // trim out any non-persisted items
        if (ids == null || ids.size() == 0)
            return;
        List<Integer> targets = new ArrayList<Integer>();
        for (int id : ids) {
            if (id > 0) {
                targets.add(id);
            }
        }
        if (targets.isEmpty())
            return;

        DbConnection conn = mbox.getOperationConnection();
        for (int offset = 0; offset < targets.size(); offset += Db.getINClauseBatchSize()) {
            PreparedStatement stmt = null;
            try {
                int count = Math.min(Db.getINClauseBatchSize(), targets.size() - offset);
                if (!fromDumpster && mbox.dumpsterEnabled())
                    copyToDumpster(conn, mbox, targets, offset, count);
                stmt = conn.prepareStatement("DELETE FROM " + getMailItemTableName(mbox, fromDumpster) +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int i = offset; i < offset + count; ++i)
                    stmt.setInt(pos++, targets.get(i));
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("deleting " + ids.size() + " item(s): " + getIdListForLogging(ids), e);
            } finally {
                DbPool.closeStatement(stmt);
            }
        }
    }

    // parent_id = null, change_date = ? (to be set to deletion time)
    private static String MAIL_ITEM_DUMPSTER_COPY_SRC_FIELDS =
        (DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ") +
        "id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest, " +
        "unread, flags, tag_names, sender, recipients, subject, name, metadata, mod_metadata, ?, mod_content";
    private static String MAIL_ITEM_DUMPSTER_COPY_DEST_FIELDS =
        (DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ") +
        "id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest, " +
        "unread, flags, tag_names, sender, recipients, subject, name, metadata, mod_metadata, change_date, mod_content";

    /**
     * Copy rows from mail_item, appointment and revision table to the corresponding dumpster tables.
     * @param conn
     * @param mbox
     * @param ids
     * @param offset offset of the first item to copy in ids
     * @param count number of items to copy
     * @throws SQLException
     * @throws ServiceException
     */
    private static void copyToDumpster(DbConnection conn, Mailbox mbox, List<Integer> ids, int offset, int count)
    throws SQLException, ServiceException {
        String miTableName = getMailItemTableName(mbox, false);
        String dumpsterMiTableName = getMailItemTableName(mbox, true);
        String ciTableName = getCalendarItemTableName(mbox, false);
        String dumpsterCiTableName = getCalendarItemTableName(mbox, true);
        String revTableName = getRevisionTableName(mbox, false);
        String dumpsterRevTableName = getRevisionTableName(mbox, true);

        // Copy the mail_item rows being deleted to dumpster_mail_item.  Copy the corresponding rows
        // from appointment and revision tables to their dumpster counterparts.  They are copied here,
        // just before cascaded deletes following mail_item deletion.
        String command = Db.supports(Db.Capability.REPLACE_INTO) ? "REPLACE" : "INSERT";
        String miWhere = DbUtil.whereIn("id", count) +
                         " AND type IN " + DUMPSTER_TYPES +
                         (!mbox.useDumpsterForSpam() ? " AND folder_id != " + Mailbox.ID_FOLDER_SPAM : "") +
                         " AND folder_id != " + Mailbox.ID_FOLDER_DRAFTS;
        PreparedStatement miCopyStmt = null;
        try {
            miCopyStmt = conn.prepareStatement(command + " INTO " + dumpsterMiTableName +
                    " (" + MAIL_ITEM_DUMPSTER_COPY_DEST_FIELDS + ")" +
                    " SELECT " + MAIL_ITEM_DUMPSTER_COPY_SRC_FIELDS + " FROM " + miTableName +
                    " WHERE " + IN_THIS_MAILBOX_AND + miWhere);
            int pos = 1;
            miCopyStmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(miCopyStmt, mbox, pos);
            for (int i = offset; i < offset + count; ++i)
                miCopyStmt.setInt(pos++, ids.get(i));
            miCopyStmt.executeUpdate();
        } finally {
            DbPool.closeStatement(miCopyStmt);
        }

        PreparedStatement ciCopyStmt = null;
        try {
            ciCopyStmt = conn.prepareStatement(command + " INTO " + dumpsterCiTableName +
                    " SELECT * FROM " + ciTableName +
                    " WHERE " + IN_THIS_MAILBOX_AND + "item_id IN" +
                    " (SELECT id FROM " + miTableName + " WHERE " + IN_THIS_MAILBOX_AND + miWhere + ")");
            int pos = 1;
            pos = setMailboxId(ciCopyStmt, mbox, pos);
            pos = setMailboxId(ciCopyStmt, mbox, pos);
            for (int i = offset; i < offset + count; ++i)
                ciCopyStmt.setInt(pos++, ids.get(i));
            ciCopyStmt.executeUpdate();
        } finally {
            DbPool.closeStatement(ciCopyStmt);
        }

        PreparedStatement revCopyStmt = null;
        try {
            revCopyStmt = conn.prepareStatement(command + " INTO " + dumpsterRevTableName +
                    " SELECT * FROM " + revTableName +
                    " WHERE " + IN_THIS_MAILBOX_AND + "item_id IN" +
                    " (SELECT id FROM " + miTableName + " WHERE " + IN_THIS_MAILBOX_AND + miWhere + ")");
            int pos = 1;
            pos = setMailboxId(revCopyStmt, mbox, pos);
            pos = setMailboxId(revCopyStmt, mbox, pos);
            for (int i = offset; i < offset + count; ++i)
                revCopyStmt.setInt(pos++, ids.get(i));
            revCopyStmt.executeUpdate();
        } finally {
            DbPool.closeStatement(revCopyStmt);
        }
    }

    public static void writeTombstones(Mailbox mbox, TypedIdList tombstones) throws ServiceException {
        if (tombstones == null || tombstones.isEmpty())
            return;
        for (Map.Entry<MailItem.Type, List<Integer>> entry : tombstones) {
            MailItem.Type type = entry.getKey();
            switch (type) {
                case CONVERSATION:
                case VIRTUAL_CONVERSATION:
                    continue;
            }
            StringBuilder ids = new StringBuilder();
            for (Integer id : entry.getValue()) {
                ids.append(ids.length() == 0 ? "" : ",").append(id);

                // catch overflows of TEXT values; since all chars are ASCII, no need to convert to UTF-8 for length check beforehand
                if (ids.length() > MAX_TEXT_LENGTH - 50) {
                    writeTombstone(mbox, type, ids.toString());
                    ids.setLength(0);
                }
            }

            writeTombstone(mbox, type, ids.toString());
        }
    }

    private static void writeTombstone(Mailbox mbox, MailItem.Type type, String ids) throws ServiceException {
        if (Strings.isNullOrEmpty(ids)) {
            return;
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement("INSERT INTO " + getTombstoneTableName(mbox) +
                        "(" + mailbox_id + "sequence, date, type, ids)" +
                        " VALUES (" + MAILBOX_ID_VALUE + "?, ?, ?, ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setByte(pos++, type.toByte());
            stmt.setString(pos++, ids);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing tombstones for " + type + "(s): " + ids, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static TypedIdList readTombstones(Mailbox mbox, long lastSync) throws ServiceException {
        TypedIdList tombstones = new TypedIdList();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT type, ids FROM " + getTombstoneTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "sequence > ? AND ids IS NOT NULL" +
                        " ORDER BY sequence");
            Db.getInstance().enableStreaming(stmt);
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setLong(pos++, lastSync);
            rs = stmt.executeQuery();

            while (rs.next()) {
                MailItem.Type type = MailItem.Type.of(rs.getByte(1));
                String row = rs.getString(2);
                if (row == null || row.equals(""))
                    continue;
                for (String entry : row.split(",")) {
                    try {
                        tombstones.add(type, Integer.parseInt(entry));
                    } catch (NumberFormatException nfe) {
                        ZimbraLog.sync.warn("unparseable TOMBSTONE entry: " + entry);
                    }
                }
            }
            return tombstones;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("reading tombstones since change: " + lastSync, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Deletes tombstones dated earlier than the given timestamp.
     *
     * @param mbox the mailbox
     * @param beforeDate timestamp in seconds
     * @return the change number of the most recent tombstone that was deleted, or 0 if none were removed
     */
    public static int purgeTombstones(Mailbox mbox, int beforeDate)
    throws ServiceException {
        int cutoff = 0;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT MAX(sequence) FROM " + getTombstoneTableName(mbox) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "date <= ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setLong(pos++, beforeDate);
            rs = stmt.executeQuery();
            if (rs.next()) {
                cutoff = rs.getInt(1);
            }

            if (cutoff > 0) {
                stmt = conn.prepareStatement("DELETE FROM " + getTombstoneTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "sequence <= ?");
                pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                stmt.setLong(pos++, cutoff);
                int numRows = stmt.executeUpdate();
                if (numRows > 0) {
                    ZimbraLog.mailbox.info("Purged %d tombstones dated before %d.", numRows, beforeDate);
                }
            }

            return cutoff;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("purging tombstones with date before " + beforeDate, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static final String FOLDER_TYPES = "(" +
        MailItem.Type.FOLDER.toByte() + ',' +
        MailItem.Type.SEARCHFOLDER.toByte() + ',' +
        MailItem.Type.MOUNTPOINT.toByte() + ')';

    private static final String MESSAGE_TYPES = "(" +
        MailItem.Type.MESSAGE.toByte() + ',' +
        MailItem.Type.CHAT.toByte() + ')';

    @SuppressWarnings("deprecation")
    private static final String DOCUMENT_TYPES = "(" +
        MailItem.Type.DOCUMENT.toByte() + ',' +
        MailItem.Type.LINK.toByte() + ',' +
        MailItem.Type.WIKI.toByte() + ')';

    private static final String CALENDAR_TYPES = "(" +
        MailItem.Type.APPOINTMENT.toByte() + ',' +
        MailItem.Type.TASK.toByte() + ')';

    private static final String DUMPSTER_TYPES = "(" +
        MailItem.Type.MESSAGE.toByte() + ',' +
        MailItem.Type.CONTACT.toByte() + ',' +
        MailItem.Type.DOCUMENT.toByte() + ',' +
        MailItem.Type.LINK.toByte() + ',' +
        MailItem.Type.APPOINTMENT.toByte() + ',' +
        MailItem.Type.TASK.toByte() + ',' +
        MailItem.Type.CHAT.toByte() + ',' +
        MailItem.Type.COMMENT.toByte() + ')';

    public static final String NON_SEARCHABLE_TYPES = "(" +
        MailItem.Type.FOLDER.toByte() + ',' +
        MailItem.Type.SEARCHFOLDER.toByte() + ',' +
        MailItem.Type.MOUNTPOINT.toByte() + ',' +
        MailItem.Type.TAG.toByte() + ',' +
        MailItem.Type.CONVERSATION.toByte() + ')';

    private static String typeIn(MailItem.Type type) {
        switch (type) {
            case FOLDER:    return "type IN " + FOLDER_TYPES;
            case MESSAGE:   return "type IN " + MESSAGE_TYPES;
            case DOCUMENT:  return "type IN " + DOCUMENT_TYPES;
            default:        return "type = " + type.toByte();
        }
    }

    @SuppressWarnings("serial")
    public static class FolderTagMap extends HashMap<UnderlyingData, FolderTagCounts> { }

    public static class FolderTagCounts {
        public long totalSize;
        public int deletedCount, deletedUnreadCount;

        @Override
        public String toString() {
            return totalSize + "/" + deletedCount + "/" + deletedUnreadCount;
        }
    }

    public static Mailbox.MailboxData getFoldersAndTags(Mailbox mbox, FolderTagMap folderData, FolderTagMap tagData, boolean forceReload)
    throws ServiceException {
        boolean reload = forceReload;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String table = getMailItemTableName(mbox, "mi");

            // folder data is loaded from the MAIL_ITEM table...
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS + " FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "type IN " + FOLDER_TYPES);
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();
            while (rs.next()) {
                UnderlyingData data = constructItem(rs);
                MailItem.Type type = MailItem.Type.of(data.type);
                if (MailItem.isAcceptableType(MailItem.Type.FOLDER, type)) {
                    folderData.put(data, null);
                } else if (MailItem.isAcceptableType(MailItem.Type.TAG, type)) {
                    tagData.put(data, null);
                }

                rs.getInt(CI_UNREAD);
                reload |= rs.wasNull();
            }
            rs.close();

            // tag data is loaded from a different table...
            reload |= DbTag.getAllTags(mbox, tagData);

            for (UnderlyingData data : folderData.keySet()) {
                if (data.parentId != data.folderId) {
                    // we had a small folder data inconsistency issue, so resolve it here
                    //   rather than returning it up to the caller
                    stmt.close();
                    stmt = conn.prepareStatement("UPDATE " + table +
                            " SET parent_id = folder_id" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
                    int pos = 1;
                    pos = setMailboxId(stmt, mbox, pos);
                    stmt.setInt(pos++, data.id);
                    stmt.executeUpdate();

                    data.parentId = data.folderId;
                    ZimbraLog.mailbox.info("correcting PARENT_ID column for %s %d", MailItem.Type.of(data.type), data.id);
                }
            }

            if (!reload) {
                return null;
            }

            Map<Integer, UnderlyingData> lookup = new HashMap<Integer, UnderlyingData>(folderData.size() + tagData.size());

            // going to recalculate counts, so discard any existing counts...
            for (FolderTagMap itemData : new FolderTagMap[] { folderData, tagData }) {
                for (Map.Entry<UnderlyingData, FolderTagCounts> entry : itemData.entrySet()) {
                    UnderlyingData data = entry.getKey();
                    lookup.put(data.id, data);
                    data.size = data.unreadCount = 0;
                    entry.setValue(new FolderTagCounts());
                }
            }

            rs.close();
            stmt.close();

            // recalculate the counts for all folders and the overall mailbox size...
            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            stmt = conn.prepareStatement("SELECT folder_id, type, flags, COUNT(*), SUM(unread), SUM(size)" +
                        " FROM " + table + " WHERE " + IN_THIS_MAILBOX_AND + "type NOT IN " + NON_SEARCHABLE_TYPES +
                        " GROUP BY folder_id, type, flags");
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int folderId = rs.getInt(1);
                byte type  = rs.getByte(2);
                boolean deleted = (rs.getInt(3) & Flag.BITMASK_DELETED) != 0;
                int count  = rs.getInt(4);
                int unread = rs.getInt(5);
                long size  = rs.getLong(6);

                if (type == MailItem.Type.CONTACT.toByte()) {
                    mbd.contacts += count;
                }
                mbd.size += size;

                UnderlyingData data = lookup.get(folderId);
                if (data != null) {
                    data.unreadCount += unread;
                    data.size += count;

                    FolderTagCounts fcounts = folderData.get(data);
                    fcounts.totalSize += size;
                    if (deleted) {
                        fcounts.deletedCount += count;
                        fcounts.deletedUnreadCount += unread;
                    }
                } else {
                    ZimbraLog.mailbox.warn("inconsistent DB state: items with no corresponding folder (folder id %d)", folderId);
                }
            }

            // recalculate the counts for all tags...
            DbTag.recalculateTagCounts(mbox, lookup);

            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("SELECT mi.folder_id, SUM(rev.size)" +
                        " FROM " + table + ", " + getRevisionTableName(mbox, "rev") +
                        " WHERE mi.id = rev.item_id" +
                        (DebugConfig.disableMailboxGroups ? "" : " AND rev.mailbox_id = ? AND mi.mailbox_id = rev.mailbox_id") +
                        " GROUP BY folder_id");
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int folderId = rs.getInt(1);
                long size    = rs.getLong(2);

                mbd.size += size;

                UnderlyingData data = lookup.get(folderId);
                if (data != null) {
                    folderData.get(data).totalSize += size;
                } else {
                    ZimbraLog.mailbox.warn("inconsistent DB state: revisions with no corresponding folder (folder ID " + folderId + ")");
                }
            }

            return mbd;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching folder data for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getByType(Mailbox mbox, MailItem.Type type, SortBy sort) throws ServiceException {
        if (Mailbox.isCachedType(type)) {
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        }
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox, " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + typeIn(type) + DbSearch.orderBy(sort, false));
            if (type == MailItem.Type.MESSAGE) {
                Db.getInstance().enableStreaming(stmt);
            }
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(constructItem(rs));
            }
            rs.close(); rs = null;
            stmt.close(); stmt = null;

            if (type == MailItem.Type.CONVERSATION) {
                completeConversations(mbox, conn, result);
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching items of type " + type, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getByParent(MailItem parent) throws ServiceException {
        return getByParent(parent, SortBy.DATE_DESC, -1, false);
    }

    public static List<UnderlyingData> getByParent(MailItem parent, SortBy sort, int limit, boolean fromDumpster)
            throws ServiceException {
        Mailbox mbox = parent.getMailbox();

        List<UnderlyingData> result = new ArrayList<UnderlyingData>();

        StringBuilder sql = new StringBuilder("SELECT ").append(DB_FIELDS).append(" FROM ")
                .append(getMailItemTableName(parent.getMailbox(), " mi", fromDumpster))
                .append(" WHERE ").append(IN_THIS_MAILBOX_AND).append("parent_id = ? ")
                .append(DbSearch.orderBy(sort, false));
        if (limit > 0) {
            sql.append(" LIMIT ?");
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql.toString());
            if (parent.getSize() > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, parent.getId());
            if (limit > 0) {
                stmt.setInt(pos++, limit);
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                UnderlyingData data = constructItem(rs, fromDumpster);
                if (Mailbox.isCachedType(MailItem.Type.of(data.type))) {
                    throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
                }
                result.add(data);
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching children of item " + parent.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getUnreadMessages(MailItem relativeTo) throws ServiceException {
        if (relativeTo instanceof Tag) {
            return DbTag.getUnreadMessages((Tag) relativeTo);
        }

        Mailbox mbox = relativeTo.getMailbox();
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String relation;
            if (relativeTo instanceof VirtualConversation) {
                relation = "id = ?";
            } else if (relativeTo instanceof Conversation) {
                relation = "parent_id = ?";
            } else if (relativeTo instanceof Folder) {
                relation = "folder_id = ?";
            } else {
                relation = "id = ?";
            }

            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(relativeTo.getMailbox(), " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + "unread > 0 AND " + relation + " AND type NOT IN " + NON_SEARCHABLE_TYPES);
            if (relativeTo.getUnreadCount() > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            if (relativeTo instanceof VirtualConversation) {
                stmt.setInt(pos++, ((VirtualConversation) relativeTo).getMessageId());
            } else {
                stmt.setInt(pos++, relativeTo.getId());
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                UnderlyingData data = constructItem(rs);
                if (Mailbox.isCachedType(MailItem.Type.of(data.type))) {
                    throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
                }
                result.add(data);
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching unread messages for item " + relativeTo.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getByFolder(Folder folder, MailItem.Type type, SortBy sort)
            throws ServiceException {
        if (Mailbox.isCachedType(type)) {
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        }
        Mailbox mbox = folder.getMailbox();

        List<UnderlyingData> result = new ArrayList<UnderlyingData>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(folder.getMailbox(), " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND " + typeIn(type) +
                    DbSearch.orderBy(sort, false));
            if (folder.getSize() > RESULTS_STREAMING_MIN_ROWS && type == MailItem.Type.MESSAGE) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folder.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(constructItem(rs));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching items in folder " + folder.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static UnderlyingData getById(Mailbox mbox, int id, MailItem.Type type, boolean fromDumpster)
    throws ServiceException {
        if (Mailbox.isCachedType(type)) {
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi", fromDumpster) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, id);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                throw MailItem.noSuchItem(id, type);
            }
            UnderlyingData data = constructItem(rs, fromDumpster);
            if (!MailItem.isAcceptableType(type, MailItem.Type.of(data.type))) {
                throw MailItem.noSuchItem(id, type);
            }
            if (!fromDumpster && data.type == MailItem.Type.CONVERSATION.toByte()) {
                completeConversation(mbox, conn, data);
            }
            return data;
        } catch (SQLException e) {
            if (!fromDumpster) {
                throw ServiceException.FAILURE("fetching item " + id, e);
            } else {
                throw ServiceException.FAILURE("fetching item " + id + " from dumpster", e);
            }
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static UnderlyingData getByImapId(Mailbox mbox, int imapId, int folderId) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND imap_id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folderId);
            stmt.setInt(pos++, imapId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                throw MailServiceException.NO_SUCH_ITEM(imapId);
            }
            UnderlyingData data = constructItem(rs);
            if (data.type == MailItem.Type.CONVERSATION.toByte()) {
                throw MailServiceException.NO_SUCH_ITEM(imapId);
            }
            return data;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item " + imapId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getById(Mailbox mbox, Collection<Integer> ids, MailItem.Type type)
    throws ServiceException {
        if (Mailbox.isCachedType(type)) {
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        }

        List<UnderlyingData> result = new ArrayList<UnderlyingData>();
        if (ids.isEmpty()) {
            return result;
        }
        List<UnderlyingData> conversations = new ArrayList<UnderlyingData>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Iterator<Integer> it = ids.iterator();
        for (int i = 0; i < ids.size(); i += Db.getINClauseBatchSize()) {
            try {
                int count = Math.min(Db.getINClauseBatchSize(), ids.size() - i);
                stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                            " FROM " + getMailItemTableName(mbox, "mi") +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++) {
                    stmt.setInt(pos++, it.next());
                }

                rs = stmt.executeQuery();
                while (rs.next()) {
                    UnderlyingData data = constructItem(rs);
                    MailItem.Type resultType = MailItem.Type.of(data.type);
                    if (!MailItem.isAcceptableType(type, resultType)) {
                        throw MailItem.noSuchItem(data.id, type);
                    } else if (Mailbox.isCachedType(resultType)) {
                        throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
                    }
                    if (resultType == MailItem.Type.CONVERSATION) {
                        conversations.add(data);
                    }
                    result.add(data);
                }
            } catch (SQLException e) {
                throw ServiceException.FAILURE("fetching " + ids.size() + " items: " + getIdListForLogging(ids), e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
            }
        }

        if (!conversations.isEmpty())
            completeConversations(mbox, conn, conversations);
        return result;
    }

    public static UnderlyingData getByName(Mailbox mbox, int folderId, String name, MailItem.Type type)
    throws ServiceException {
        if (Mailbox.isCachedType(type)) {
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND " + typeIn(type) +
                        " AND " + Db.equalsSTRING("name"));
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folderId);
            stmt.setString(pos++, name);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                throw MailItem.noSuchItem(-1, type);
            }
            UnderlyingData data = constructItem(rs);
            MailItem.Type resultType = MailItem.Type.of(data.type);
            if (!MailItem.isAcceptableType(type, resultType)) {
                throw MailItem.noSuchItem(data.id, type);
            }
            if (resultType == MailItem.Type.CONVERSATION) {
                completeConversation(mbox, conn, data);
            }
            return data;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item by name ('" + name + "' in folder " + folderId + ")", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static UnderlyingData getByHash(Mailbox mbox, String hash) throws ServiceException {
        return ListUtil.getFirstElement(getByHashes(mbox, Arrays.asList(hash)));
    }

    public static List<UnderlyingData> getByHashes(Mailbox mbox, List<String> hashes) throws ServiceException {
        if (ListUtil.isEmpty(hashes)) {
            return null;
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox, "mi") + ", " + getConversationTableName(mbox, "oc") +
                    " WHERE mi.id = oc.conv_id AND " + DbUtil.whereIn("oc.hash", hashes.size()) +
                    (DebugConfig.disableMailboxGroups ? "" : " AND oc.mailbox_id = ? AND mi.mailbox_id = oc.mailbox_id"));
            int pos = 1;
            for (String hash : hashes) {
                stmt.setString(pos++, hash);
            }
            pos = setMailboxId(stmt, mbox, pos);
            rs = stmt.executeQuery();

            List<UnderlyingData> dlist = new ArrayList<UnderlyingData>(3);
            Set<Integer> convIds = Sets.newHashSetWithExpectedSize(3);
            while (rs.next()) {
                int id = rs.getInt(CI_ID);
                if (convIds.contains(id))
                    continue;

                UnderlyingData data = constructItem(rs);
                if (data.type == MailItem.Type.CONVERSATION.toByte()) {
                    completeConversation(mbox, conn, data);
                }
                dlist.add(data);
                convIds.add(data.id);
            }
            return dlist.isEmpty() ? null : dlist;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching conversation for hash " + hashes, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static Pair<List<Integer>,TypedIdList> getModifiedItems(Mailbox mbox, MailItem.Type type, long lastSync, Set<Integer> visible)
    throws ServiceException {
        if (Mailbox.isCachedType(type)) {
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        }
        List<Integer> modified = new ArrayList<Integer>();
        TypedIdList missed = new TypedIdList();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String typeConstraint = type == MailItem.Type.UNKNOWN ? "type NOT IN " + NON_SEARCHABLE_TYPES : typeIn(type);
            stmt = conn.prepareStatement("SELECT id, type, folder_id" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "mod_metadata > ? AND " + typeConstraint +
                        " ORDER BY mod_metadata, id");
            if (type == MailItem.Type.MESSAGE) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setLong(pos++, lastSync);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (visible == null || visible.contains(rs.getInt(3))) {
                    modified.add(rs.getInt(1));
                } else {
                    missed.add(MailItem.Type.of(rs.getByte(2)), rs.getInt(1));
                }
            }

            return new Pair<List<Integer>,TypedIdList>(modified, missed);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting items modified since " + lastSync, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void completeConversation(Mailbox mbox, DbConnection conn, UnderlyingData data)
            throws ServiceException {
        completeConversations(mbox, conn, Collections.singletonList(data));
    }

    private static void completeConversations(Mailbox mbox, DbConnection conn, List<UnderlyingData> convData)
            throws ServiceException {
        if (convData == null || convData.isEmpty()) {
            return;
        }
        for (UnderlyingData data : convData) {
            if (data.type != MailItem.Type.CONVERSATION.toByte()) {
                throw ServiceException.FAILURE("attempting to complete a non-conversation", null);
            }
        }

        Map<Integer, UnderlyingData> conversations = new HashMap<Integer, UnderlyingData>(Db.getINClauseBatchSize() * 3 / 2);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        for (int i = 0; i < convData.size(); i += Db.getINClauseBatchSize()) {
            try {
                int count = Math.min(Db.getINClauseBatchSize(), convData.size() - i);
                stmt = conn.prepareStatement("SELECT parent_id, unread, flags, tag_names" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("parent_id", count));
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++) {
                    UnderlyingData data = convData.get(index);
                    stmt.setInt(pos++, data.id);
                    conversations.put(data.id, data);
                    // don't assume that the UnderlyingData structure was new...
                    data.unreadCount = 0;
                    data.setFlags(0);
                    data.setTags(null);
                }
                rs = stmt.executeQuery();

                HashMultimap<Integer, String> convTags = HashMultimap.create();

                while (rs.next()) {
                    Integer convId = rs.getInt(1);
                    UnderlyingData data = conversations.get(convId);
                    assert data != null;

                    data.unreadCount += rs.getInt(2);
                    data.setFlags(data.getFlags() | rs.getInt(3));

                    String[] tags = DbTag.deserializeTags(rs.getString(4));
                    if (tags != null) {
                        convTags.putAll(convId, Arrays.asList(tags));
                    }
                }

                for (Map.Entry<Integer, Collection<String>> entry : convTags.asMap().entrySet()) {
                    UnderlyingData data = conversations.get(entry.getKey());
                    if (data != null) {
                        Collection<String> tags = entry.getValue();
                        data.setTags(new Tag.NormalizedTags(tags));
                    }
                }
            } catch (SQLException e) {
                throw ServiceException.FAILURE("completing conversation data", e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
            }

            conversations.clear();
        }
    }

    static final String LEAF_NODE_FIELDS = "id, size, type, unread, folder_id, parent_id, blob_digest," +
                                           " mod_content, mod_metadata, flags, index_id, volume_id, tag_names";

    private static final int LEAF_CI_ID           = 1;
    private static final int LEAF_CI_SIZE         = 2;
    private static final int LEAF_CI_TYPE         = 3;
    private static final int LEAF_CI_IS_UNREAD    = 4;
    private static final int LEAF_CI_FOLDER_ID    = 5;
    private static final int LEAF_CI_PARENT_ID    = 6;
    private static final int LEAF_CI_BLOB_DIGEST  = 7;
    private static final int LEAF_CI_MOD_CONTENT  = 8;
    private static final int LEAF_CI_MOD_METADATA = 9;
    private static final int LEAF_CI_FLAGS        = 10;
    private static final int LEAF_CI_INDEX_ID     = 11;
    private static final int LEAF_CI_VOLUME_ID    = 12;
    private static final int LEAF_CI_TAGS         = 13;

    public static PendingDelete getLeafNodes(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        int folderId = folder.getId();
        QueryParams params = new QueryParams();
        params.setFolderIds(Collections.singletonList(folderId));
        PendingDelete info = getLeafNodes(mbox, params);
        // make sure that the folder is in the list of deleted item ids
        info.itemIds.add(folder.getType(), folderId);
        return info;
    }

    public static PendingDelete getLeafNodes(Mailbox mbox, QueryParams params) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT " + LEAF_NODE_FIELDS + " FROM " + getMailItemTableName(mbox) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "type NOT IN " + FOLDER_TYPES);
            String whereClause = params.getWhereClause();
            if (!StringUtil.isNullOrEmpty(whereClause)) {
                buf.append(" AND ").append(whereClause);
            }
            String limitClause = params.getLimitClause();
            if (!StringUtil.isNullOrEmpty(limitClause)) {
                buf.append(" ").append(limitClause);
            }
            stmt = conn.prepareStatement(buf.toString());
            Db.getInstance().enableStreaming(stmt);  // Assume we're dealing with a very large result set.
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);

            PendingDelete info = accumulateDeletionInfo(mbox, stmt);
            stmt = null;

            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of items to delete", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static PendingDelete getLeafNodes(Mailbox mbox, List<Folder> folders, int before, boolean globalMessages,
                                             Boolean unread, boolean useChangeDate, Integer maxItems)
    throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        if (folders == null) {
            folders = Collections.emptyList();
        }

        try {
            StringBuilder constraint = new StringBuilder();
            String dateColumn = (useChangeDate ? "change_date" : "date");

            if (globalMessages) {
                constraint.append(dateColumn).append(" < ? AND ").append(typeIn(MailItem.Type.MESSAGE));
            } else {
                constraint.append(dateColumn).append(" < ? AND type NOT IN ").append(NON_SEARCHABLE_TYPES);
                if (!folders.isEmpty()) {
                    constraint.append(" AND ").append(DbUtil.whereIn("folder_id", folders.size()));
                }
            }
            if (unread != null) {
                constraint.append(" AND unread = ?");
            }
            String orderByLimit = "";
            if (maxItems != null && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                orderByLimit = " ORDER BY " + dateColumn + " " + Db.getInstance().limit(maxItems);
            }

            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + constraint + orderByLimit);
            if (globalMessages || getTotalFolderSize(folders) > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, before);
            if (!globalMessages) {
                for (Folder folder : folders) {
                    stmt.setInt(pos++, folder.getId());
                }
            }
            if (unread != null) {
                stmt.setBoolean(pos++, unread);
            }

            PendingDelete info = accumulateDeletionInfo(mbox, stmt);
            stmt = null;
            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of items for purge", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    static PendingDelete accumulateDeletionInfo(Mailbox mbox, PreparedStatement stmt)
    throws ServiceException {
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery();

            PendingDelete info = new PendingDelete();
            info.size = 0;

            List<Integer> versionedIds = accumulateLeafNodes(info, mbox, rs);
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;

            accumulateLeafRevisions(info, mbox, versionedIds);
            accumulateComments(info, mbox);

            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("accumulating deletion info", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    static int getTotalFolderSize(Collection<Folder> folders) {
        int totalSize = 0;
        if (folders != null) {
            for (Folder folder : folders) {
                totalSize += folder.getSize();
            }
        }
        return totalSize;
    }

    public static class LocationCount {
        public int count;
        public int deleted;
        public long size;

        public LocationCount(int c, int d, long sz)            { count = c;  deleted += d;  size = sz; }
        public LocationCount(LocationCount lc)                 { count = lc.count;  deleted = lc.deleted;  size = lc.size; }
        public LocationCount increment(int c, int d, long sz)  { count += c;  deleted += d;  size += sz;  return this; }
        public LocationCount increment(LocationCount lc)       { count += lc.count;  deleted += lc.deleted;  size += lc.size;  return this; }
    }

    /**
     * Accumulates <tt>PendingDelete</tt> info for the given <tt>ResultSet</tt>.
     * @return a <tt>List</tt> of all versioned items, to be used in a subsequent call to
     * {@link DbMailItem#accumulateLeafRevisions}, or an empty list.
     */
    static List<Integer> accumulateLeafNodes(PendingDelete info, Mailbox mbox, ResultSet rs) throws SQLException, ServiceException {
        boolean dumpsterEnabled = mbox.dumpsterEnabled();
        boolean useDumpsterForSpam = mbox.useDumpsterForSpam();
        StoreManager sm = StoreManager.getInstance();
        List<Integer> versioned = new ArrayList<Integer>();

        while (rs.next()) {
            // first check to make sure we don't have a modify conflict
            int revision = rs.getInt(LEAF_CI_MOD_CONTENT);
            int modMetadata = rs.getInt(LEAF_CI_MOD_METADATA);
            if (!mbox.checkItemChangeID(modMetadata, revision)) {
                info.incomplete = true;
                continue;
            }

            int id = rs.getInt(LEAF_CI_ID);
            long size = rs.getLong(LEAF_CI_SIZE);
            MailItem.Type type = MailItem.Type.of(rs.getByte(LEAF_CI_TYPE));

            Integer item = new Integer(id);
            info.itemIds.add(type, item);
            info.size += size;

            if (rs.getBoolean(LEAF_CI_IS_UNREAD)) {
                info.unreadIds.add(item);
            }
            boolean isMessage = false;
            switch (type) {
                case CONTACT:
                    info.contacts++;
                    break;
                case CHAT:
                case MESSAGE:
                    isMessage = true;
                    break;
            }

            // record deleted virtual conversations and modified-or-deleted real conversations
            if (isMessage) {
                int parentId = rs.getInt(LEAF_CI_PARENT_ID);
                if (rs.wasNull() || parentId <= 0) {
                    info.itemIds.add(MailItem.Type.VIRTUAL_CONVERSATION, -id);
                } else {
                    info.modifiedIds.add(parentId);
                }
            }

            int flags = rs.getInt(LEAF_CI_FLAGS);
            if ((flags & Flag.BITMASK_VERSIONED) != 0) {
                versioned.add(id);
            }

            Integer folderId = rs.getInt(LEAF_CI_FOLDER_ID);
            boolean isDeleted = (flags & Flag.BITMASK_DELETED) != 0;
            LocationCount fcount = info.folderCounts.get(folderId);
            if (fcount == null) {
                info.folderCounts.put(folderId, new LocationCount(1, isDeleted ? 1 : 0, size));
            } else {
                fcount.increment(1, isDeleted ? 1 : 0, size);
            }

            String[] tags = DbTag.deserializeTags(rs.getString(LEAF_CI_TAGS));
            if (tags != null) {
                for (String tag : tags) {
                    LocationCount tcount = info.tagCounts.get(tag);
                    if (tcount == null) {
                        info.tagCounts.put(tag, new LocationCount(1, isDeleted ? 1 : 0, size));
                    } else {
                        tcount.increment(1, isDeleted ? 1 : 0, size);
                    }
                }
            }

            int fid = folderId != null ? folderId.intValue() : -1;
            if (!dumpsterEnabled || fid == Mailbox.ID_FOLDER_DRAFTS || (fid == Mailbox.ID_FOLDER_SPAM && !useDumpsterForSpam)) {
                String blobDigest = rs.getString(LEAF_CI_BLOB_DIGEST);
                if (blobDigest != null) {
                    info.blobDigests.add(blobDigest);
                    String locator = rs.getString(LEAF_CI_VOLUME_ID);
                    try {
                        MailboxBlob mblob = sm.getMailboxBlob(mbox, id, revision, locator);
                        if (mblob == null) {
                            ZimbraLog.mailbox.warn("missing blob for id: %d, change: %d", id, revision);
                        } else {
                            info.blobs.add(mblob);
                        }
                    } catch (Exception e1) { }
                }

                int indexId = rs.getInt(LEAF_CI_INDEX_ID);
                boolean indexed = !rs.wasNull();
                if (indexed) {
                    if (info.sharedIndex == null) {
                        info.sharedIndex = new HashSet<Integer>();
                    }
                    boolean shared = (flags & Flag.BITMASK_COPIED) != 0;
                    if (shared) {
                        info.sharedIndex.add(indexId);
                    } else {
                        info.indexIds.add(indexId > MailItem.IndexStatus.STALE.id() ? indexId : id);
                    }
                }
            }
        }

        return versioned;
    }

    static void accumulateLeafRevisions(PendingDelete info, Mailbox mbox, List<Integer> versioned) throws ServiceException {
        if (versioned == null || versioned.size() == 0) {
            return;
        }
        boolean dumpsterEnabled = mbox.dumpsterEnabled();
        boolean useDumpsterForSpam = mbox.useDumpsterForSpam();
        DbConnection conn = mbox.getOperationConnection();
        StoreManager sm = StoreManager.getInstance();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT mi.id, mi.folder_id, rev.size, rev.mod_content, rev.volume_id, rev.blob_digest " +
                    " FROM " + getMailItemTableName(mbox, "mi") + ", " + getRevisionTableName(mbox, "rev") +
                    " WHERE mi.id = rev.item_id AND " + DbUtil.whereIn("mi.id", versioned.size()) +
                    (DebugConfig.disableMailboxGroups ? "" : " AND mi.mailbox_id = ? AND mi.mailbox_id = rev.mailbox_id"));
            int pos = 1;
            for (int vid : versioned)
                stmt.setInt(pos++, vid);
            pos = setMailboxId(stmt, mbox, pos);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer folderId = rs.getInt(2);
                LocationCount count = info.folderCounts.get(folderId);
                if (count == null) {
                    info.folderCounts.put(folderId, new LocationCount(0, 0, rs.getLong(3)));
                } else {
                    count.increment(0, 0, rs.getLong(3));
                }

                int fid = folderId != null ? folderId.intValue() : -1;
                if (!dumpsterEnabled || fid == Mailbox.ID_FOLDER_DRAFTS || (fid == Mailbox.ID_FOLDER_SPAM && !useDumpsterForSpam)) {
                    String blobDigest = rs.getString(6);
                    if (blobDigest != null) {
                        info.blobDigests.add(blobDigest);
                        try {
                            MailboxBlob mblob = sm.getMailboxBlob(mbox, rs.getInt(1), rs.getInt(4), rs.getString(5));
                            if (mblob == null) {
                                ZimbraLog.mailbox.error("missing blob for id: %d, change: %s",
                                        rs.getInt(1), rs.getInt(4));
                            } else {
                                info.blobs.add(mblob);
                            }
                        } catch (Exception e1) { }
                    }
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting version deletion info for items: " + versioned, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    static void accumulateComments(PendingDelete info, Mailbox mbox) throws ServiceException {
        List<Integer> documents = info.itemIds.getIds(MailItem.Type.DOCUMENT);
        if (documents == null || documents.size() == 0) {
            return;
        }
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT type, id " +
                    " FROM " + getMailItemTableName(mbox) +
                    " WHERE " + DbUtil.whereIn("parent_id", documents.size()) +
                    (DebugConfig.disableMailboxGroups ? "" : " AND mailbox_id = ?"));
            int pos = 1;
            for (int id : documents)
                stmt.setInt(pos++, id);
            pos = setMailboxId(stmt, mbox, pos);
            rs = stmt.executeQuery();

            while (rs.next()) {
                info.itemIds.add(MailItem.Type.of(rs.getByte(1)), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting Comment deletion info for items: " + documents, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
    
    /**
     * Returns the blob digest for the item with the given id, or <tt>null</tt>
     * if either the id doesn't exist in the table or there is no associated blob.
     */
    public static String getBlobDigest(Mailbox mbox, int itemId) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT blob_digest " +
                    " FROM " + getMailItemTableName(mbox) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, itemId);
            rs = stmt.executeQuery();

            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to get blob digest for id " + itemId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void resolveSharedIndex(Mailbox mbox, PendingDelete info) throws ServiceException {
        if (info.sharedIndex == null || info.sharedIndex.isEmpty())
            return;
        List<Integer> indexIDs = new ArrayList<Integer>(info.sharedIndex);

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            for (int i = 0; i < indexIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), indexIDs.size() - i);
                stmt = conn.prepareStatement("SELECT index_id FROM " + getMailItemTableName(mbox) +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("index_id", count));
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, indexIDs.get(index));
                rs = stmt.executeQuery();
                while (rs.next()) {
                    info.sharedIndex.remove(rs.getInt(1));
                }
                rs.close(); rs = null;
                stmt.close(); stmt = null;
            }

            info.indexIds.addAll(info.sharedIndex);
            info.sharedIndex.clear();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("resolving shared index entries", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }


    private static final String IMAP_FIELDS = "mi.id, mi.type, mi.imap_id, mi.unread, mi.flags, mi.tag_names";

    static final String IMAP_TYPES = "(" +
        MailItem.Type.MESSAGE.toByte() + "," +
        MailItem.Type.CHAT.toByte() + ',' +
        MailItem.Type.CONTACT.toByte() + ")";

    public static List<ImapMessage> loadImapFolder(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        List<ImapMessage> result = new ArrayList<ImapMessage>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + IMAP_FIELDS +
                        " FROM " + getMailItemTableName(folder.getMailbox(), " mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + IMAP_TYPES);
            if (folder.getSize() > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folder.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                int flags = rs.getBoolean(4) ? Flag.BITMASK_UNREAD | rs.getInt(5) : rs.getInt(5);
                result.add(new ImapMessage(rs.getInt(1), MailItem.Type.of(rs.getByte(2)), rs.getInt(3), flags, DbTag.deserializeTags(rs.getString(6))));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("loading IMAP folder data: " + folder.getPath(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static int countImapRecent(Folder folder, int uidCutoff) throws ServiceException {
        Mailbox mbox = folder.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + getMailItemTableName(folder.getMailbox()) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + IMAP_TYPES +
                        " AND (imap_id IS NULL OR imap_id = 0 OR imap_id > ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folder.getId());
            stmt.setInt(pos++, uidCutoff);
            rs = stmt.executeQuery();

            return (rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("counting IMAP \\Recent messages: " + folder.getPath(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<Pop3Message> loadPop3Folder(Set<Folder> folders, Date popSince) throws ServiceException {
        assert !folders.isEmpty() : folders;
        Mailbox mbox = Iterables.get(folders, 0).getMailbox();
        long popDate = popSince == null ? -1 : Math.max(popSince.getTime(), -1);
        List<Pop3Message> result = new ArrayList<Pop3Message>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String dateConstraint = popDate < 0 ? "" : " AND date > ?";
            stmt = conn.prepareStatement(
                    "SELECT mi.id, mi.size, mi.blob_digest FROM " + getMailItemTableName(mbox, " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("folder_id", folders.size()) +
                    " AND type = " + MailItem.Type.MESSAGE.toByte() + " AND " +
                    Db.getInstance().bitAND("flags", String.valueOf(Flag.BITMASK_DELETED | Flag.BITMASK_POPPED)) +
                    " = 0" + dateConstraint);
            if (getTotalFolderSize(folders) > RESULTS_STREAMING_MIN_ROWS) {
                //TODO: Because of POPPED flag, the folder size no longer represent the count.
                Db.getInstance().enableStreaming(stmt);
            }

            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            for (Folder folder : folders) {
                stmt.setInt(pos++, folder.getId());
            }
            if (popDate >= 0) {
                stmt.setInt(pos++, (int) (popDate / 1000L));
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(new Pop3Message(rs.getInt(1), rs.getLong(2), rs.getString(3)));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("loading POP3 folder data: " + folders, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getRevisionInfo(MailItem item, boolean fromDumpster) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        List<UnderlyingData> dlist = new ArrayList<UnderlyingData>();
        if (!item.isTagged(Flag.FlagInfo.VERSIONED)) {
            return dlist;
        }
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + REVISION_FIELDS + " FROM " + getRevisionTableName(mbox, fromDumpster) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "item_id = ?" +
                        " ORDER BY version");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            rs = stmt.executeQuery();

            while (rs.next())
                dlist.add(constructRevision(rs, item, fromDumpster));
            return dlist;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting old revisions for item: " + item.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<Integer> listByFolder(Folder folder, MailItem.Type type, boolean descending) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        boolean allTypes = type == MailItem.Type.UNKNOWN;
        List<Integer> result = new ArrayList<Integer>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String typeConstraint = allTypes ? "" : "type = ? AND ";
            stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + typeConstraint + "folder_id = ?" +
                        " ORDER BY date" + (descending ? " DESC" : ""));
            if (type == MailItem.Type.MESSAGE && folder.getSize() > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            if (!allTypes) {
                stmt.setByte(pos++, type.toByte());
            }
            stmt.setInt(pos++, folder.getId());
            rs = stmt.executeQuery();

            while (rs.next())
                result.add(rs.getInt(1));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item list for folder " + folder.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static TypedIdList listByFolder(Folder folder, boolean descending) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        TypedIdList result = new TypedIdList();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id, type FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ?" +
                        " ORDER BY date" + (descending ? " DESC" : ""));
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folder.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(MailItem.Type.of(rs.getByte(2)), rs.getInt(1));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item list for folder " + folder.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    // these columns are specified by DB_FIELDS, below
    public static final int CI_ID          = 1;
    public static final int CI_TYPE        = 2;
    public static final int CI_PARENT_ID   = 3;
    public static final int CI_FOLDER_ID   = 4;
    public static final int CI_INDEX_ID    = 5;
    public static final int CI_IMAP_ID     = 6;
    public static final int CI_DATE        = 7;
    public static final int CI_SIZE        = 8;
    public static final int CI_VOLUME_ID   = 9;
    public static final int CI_BLOB_DIGEST = 10;
    public static final int CI_UNREAD      = 11;
    public static final int CI_FLAGS       = 12;
    public static final int CI_TAGS        = 13;
    public static final int CI_SUBJECT     = 14;
    public static final int CI_NAME        = 15;
    public static final int CI_METADATA    = 16;
    public static final int CI_MODIFIED    = 17;
    public static final int CI_MODIFY_DATE = 18;
    public static final int CI_SAVED       = 19;

    static final String DB_FIELDS = "mi.id, mi.type, mi.parent_id, mi.folder_id, mi.index_id, mi.imap_id, mi.date, " +
        "mi.size, mi.volume_id, mi.blob_digest, mi.unread, mi.flags, mi.tag_names, mi.subject, mi.name, " +
        "mi.metadata, mi.mod_metadata, mi.change_date, mi.mod_content";

    static UnderlyingData constructItem(ResultSet rs) throws SQLException, ServiceException {
        return constructItem(rs, 0, false);
    }

    private static UnderlyingData constructItem(ResultSet rs, boolean fromDumpster) throws SQLException, ServiceException {
        return constructItem(rs, 0, fromDumpster);
    }

    static UnderlyingData constructItem(ResultSet rs, int offset) throws SQLException, ServiceException {
        return constructItem(rs, offset, false);
    }

    static UnderlyingData constructItem(ResultSet rs, int offset, boolean fromDumpster) throws SQLException, ServiceException {
        UnderlyingData data = new UnderlyingData();
        data.id = rs.getInt(CI_ID + offset);
        data.type = rs.getByte(CI_TYPE + offset);
        data.parentId = rs.getInt(CI_PARENT_ID + offset);
        data.folderId = rs.getInt(CI_FOLDER_ID + offset);
        data.indexId = rs.getInt(CI_INDEX_ID + offset);
        if (rs.wasNull()) {
            data.indexId = MailItem.IndexStatus.NO.id();
        }
        data.imapId = rs.getInt(CI_IMAP_ID + offset);
        if (rs.wasNull()) {
            data.imapId = -1;
        }
        data.date = rs.getInt(CI_DATE + offset);
        data.size = rs.getLong(CI_SIZE + offset);
        data.locator = rs.getString(CI_VOLUME_ID + offset);
        data.setBlobDigest(rs.getString(CI_BLOB_DIGEST + offset));
        data.unreadCount = rs.getInt(CI_UNREAD + offset);
        int flags = rs.getInt(CI_FLAGS + offset);
        if (!fromDumpster) {
            data.setFlags(flags);
        } else {
            data.setFlags(flags | Flag.BITMASK_UNCACHED | Flag.BITMASK_IN_DUMPSTER);
        }
        data.setTags(new Tag.NormalizedTags(DbTag.deserializeTags(rs.getString(CI_TAGS + offset))));
        data.setSubject(rs.getString(CI_SUBJECT + offset));
        data.name = rs.getString(CI_NAME + offset);
        data.metadata = decodeMetadata(rs.getString(CI_METADATA + offset));
        data.modMetadata = rs.getInt(CI_MODIFIED + offset);
        data.modContent = rs.getInt(CI_SAVED + offset);
        data.dateChanged = rs.getInt(CI_MODIFY_DATE + offset);
        // make sure to handle NULL column values
        if (data.parentId == 0) {
            data.parentId = -1;
        }
        if (data.dateChanged == 0) {
            data.dateChanged = -1;
        }
        return data;
    }

    private static final String REVISION_FIELDS = "date, size, volume_id, blob_digest, name, " +
                                                  "metadata, mod_metadata, change_date, mod_content";

    private static UnderlyingData constructRevision(ResultSet rs, MailItem item, boolean fromDumpster) throws SQLException, ServiceException {
        UnderlyingData data = new UnderlyingData();
        data.id          = item.getId();
        data.type        = item.getType().toByte();
        data.parentId    = item.getParentId();
        data.folderId    = item.getFolderId();
        data.indexId = MailItem.IndexStatus.NO.id();
        data.imapId      = -1;
        data.date        = rs.getInt(1);
        data.size        = rs.getLong(2);
        data.locator    = rs.getString(3);
        data.setBlobDigest(rs.getString(4));
        data.unreadCount = item.getUnreadCount();
        if (!fromDumpster) {
            data.setFlags(item.getInternalFlagBitmask() | Flag.BITMASK_UNCACHED);
        } else {
            data.setFlags(item.getInternalFlagBitmask() | Flag.BITMASK_UNCACHED | Flag.BITMASK_IN_DUMPSTER);
        }
        data.setTags(new Tag.NormalizedTags(item.getTags()));
        data.setSubject(item.getSubject());
        data.name        = rs.getString(5);
        data.metadata    = decodeMetadata(rs.getString(6));
        data.modMetadata = rs.getInt(7);
        data.dateChanged = rs.getInt(8);
        data.modContent  = rs.getInt(9);
        // make sure to handle NULL column values
        if (data.parentId <= 0) {
            data.parentId = -1;
        }
        if (data.dateChanged == 0) {
            data.dateChanged = -1;
        }
        return data;
    }

    //////////////////////////////////////
    // CALENDAR STUFF BELOW HERE!
    //////////////////////////////////////

    public static UnderlyingData getCalendarItem(Mailbox mbox, String uid) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getCalendarItemTableName(mbox, "ci") + ", " + getMailItemTableName(mbox, "mi") +
                    " WHERE ci.uid = ? AND mi.id = ci.item_id AND mi.type IN " + CALENDAR_TYPES +
                    (DebugConfig.disableMailboxGroups ? "" : " AND ci.mailbox_id = ? AND mi.mailbox_id = ci.mailbox_id"));

            int pos = 1;
            stmt.setString(pos++, uid);
            pos = setMailboxId(stmt, mbox, pos);
            rs = stmt.executeQuery();

            if (rs.next())
                return constructItem(rs);
            return null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching calendar items for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Return all of the Invite records within the range start&lt;=Invites&lt;end.  IE "Give me all the
     * invites between 7:00 and 9:00" will return you everything from 7:00 to 8:59:59.99
     * @param start
     * @param end
     * @param folderId
     * @return list of invites
     */
    public static List<UnderlyingData> getCalendarItems(Mailbox mbox, MailItem.Type type, long start, long end,
            int folderId, int[] excludeFolderIds) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = calendarItemStatement(conn, DB_FIELDS, mbox, type, start, end, folderId, excludeFolderIds);
            rs = stmt.executeQuery();

            List<UnderlyingData> result = new ArrayList<UnderlyingData>();
            while (rs.next())
                result.add(constructItem(rs));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching calendar items for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getCalendarItems(Mailbox mbox, List<String> uids) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<UnderlyingData> result = new ArrayList<UnderlyingData>();
        try {
            for (int i = 0; i < uids.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), uids.size() - i);
                stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getCalendarItemTableName(mbox, "ci") + ", " + getMailItemTableName(mbox, "mi") +
                        " WHERE mi.id = ci.item_id AND mi.type IN " + CALENDAR_TYPES +
                        (DebugConfig.disableMailboxGroups ? "" : " AND ci.mailbox_id = ? AND mi.mailbox_id = ci.mailbox_id") +
                        " AND " + DbUtil.whereIn("ci.uid", count));
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++)
                    stmt.setString(pos++, uids.get(index));
                rs = stmt.executeQuery();
                while (rs.next())
                    result.add(constructItem(rs));
                stmt.close();
                stmt = null;
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching calendar items for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static TypedIdList listCalendarItems(Mailbox mbox, MailItem.Type type, long start, long end, int folderId,
            int[] excludeFolderIds) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = calendarItemStatement(conn, "mi.id, mi.type", mbox, type, start, end, folderId, excludeFolderIds);
            rs = stmt.executeQuery();

            TypedIdList result = new TypedIdList();
            while (rs.next()) {
                result.add(MailItem.Type.of(rs.getByte(2)), rs.getInt(1));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("listing calendar items for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static PreparedStatement calendarItemStatement(DbConnection conn, String fields,
            Mailbox mbox, MailItem.Type type, long start, long end, int folderId, int[] excludeFolderIds)
            throws SQLException {
        boolean folderSpecified = folderId != Mailbox.ID_AUTO_INCREMENT;

        String endConstraint = end > 0 ? " AND ci.start_time < ?" : "";
        String startConstraint = start > 0 ? " AND ci.end_time > ?" : "";
        String typeConstraint = type == MailItem.Type.UNKNOWN ? "type IN " + CALENDAR_TYPES : typeIn(type);

        String excludeFolderPart = "";
        if (excludeFolderIds != null && excludeFolderIds.length > 0)
            excludeFolderPart = " AND " + DbUtil.whereNotIn("folder_id", excludeFolderIds.length);

        PreparedStatement stmt = conn.prepareStatement("SELECT " + fields +
                " FROM " + getCalendarItemTableName(mbox, "ci") + ", " + getMailItemTableName(mbox, "mi") +
                " WHERE mi.id = ci.item_id" + endConstraint + startConstraint + " AND mi." + typeConstraint +
                (DebugConfig.disableMailboxGroups? "" : " AND ci.mailbox_id = ? AND mi.mailbox_id = ci.mailbox_id") +
                (folderSpecified ? " AND folder_id = ?" : "") + excludeFolderPart);

        int pos = 1;
        if (end > 0)
            stmt.setTimestamp(pos++, new Timestamp(end));
        if (start > 0)
            stmt.setTimestamp(pos++, new Timestamp(start));
        pos = setMailboxId(stmt, mbox, pos);
        if (folderSpecified)
            stmt.setInt(pos++, folderId);
        if (excludeFolderIds != null) {
            for (int id : excludeFolderIds)
                stmt.setInt(pos++, id);
        }

        return stmt;
    }

    public static List<Integer> getItemListByDates(Mailbox mbox, MailItem.Type type, long start, long end, int folderId,
            boolean descending) throws ServiceException {
        boolean allTypes = type == MailItem.Type.UNKNOWN;
        List<Integer> result = new ArrayList<Integer>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String typeConstraint = allTypes ? "" : "type = ? AND ";
            stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + typeConstraint + "folder_id = ?" +
                        " AND date > ? AND date < ?" +
                        " ORDER BY date" + (descending ? " DESC" : ""));
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            if (!allTypes) {
                stmt.setByte(pos++, type.toByte());
            }
            stmt.setInt(pos++, folderId);
            stmt.setInt(pos++, (int)(start / 1000));
            stmt.setInt(pos++, (int)(end / 1000));

            rs = stmt.executeQuery();

            while (rs.next())
                result.add(rs.getInt(1));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("finding items between dates", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static class QueryParams {
        private SortedSet<Integer> folderIds = new TreeSet<Integer>();
        private Integer changeDateBefore;
        private Integer modifiedSequenceBefore;
        private Integer rowLimit;
        private Integer offset;
        private Set<MailItem.Type> includedTypes = EnumSet.noneOf(MailItem.Type.class);
        private Set<MailItem.Type> excludedTypes = EnumSet.noneOf(MailItem.Type.class);

        public SortedSet<Integer> getFolderIds() {
            return Collections.unmodifiableSortedSet(folderIds);
        }

        public QueryParams setFolderIds(Collection<Integer> ids) {
            folderIds.clear();
            if (ids != null) {
                folderIds.addAll(ids);
            }
            return this;
        }

        public Set<MailItem.Type> getIncludedTypes() {
            return Collections.unmodifiableSet(includedTypes);
        }

        public QueryParams setIncludedTypes(Collection<MailItem.Type> types) {
            includedTypes.clear();
            includedTypes.addAll(types);
            return this;
        }

        public Set<MailItem.Type> getExcludedTypes() {
            return Collections.unmodifiableSet(excludedTypes);
        }

        public QueryParams setExcludedTypes(Collection<MailItem.Type> types) {
            excludedTypes.clear();
            excludedTypes.addAll(types);
            return this;
        }

        /**
         * @return the timestamp, in seconds
         */
        public Integer getChangeDateBefore() { return changeDateBefore; }
        /**
         * Return items modified earlier than the given timestamp.
         * @param timestamp the timestamp, in seconds
         */
        public QueryParams setChangeDateBefore(Integer timestamp) { changeDateBefore = timestamp; return this; }
        public Integer getModifiedSequenceBefore() { return modifiedSequenceBefore; }
        public QueryParams setModifiedSequenceBefore(Integer changeId) { modifiedSequenceBefore = changeId; return this; }

        public Integer getRowLimit() { return rowLimit; }
        public QueryParams setRowLimit(Integer rowLimit) { this.rowLimit = rowLimit; return this; }
        public Integer getOffset() { return offset; }
        public QueryParams setOffset(Integer offset) { this.offset = offset; return this; }

        public String getWhereClause() {
            StringBuilder buf = new StringBuilder();
            if (!includedTypes.isEmpty()) {
                if (buf.length() > 0) {
                    buf.append(" AND ");
                }
                if (includedTypes.size() == 1) {
                    for (MailItem.Type type : includedTypes) {
                        buf.append("type = ").append(type.toByte());
                        break;
                    }
                } else {
                    buf.append("type IN (");
                    boolean first = true;
                    for (MailItem.Type type : includedTypes) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(", ");
                        }
                        buf.append(type.toByte());
                    }
                    buf.append(")");
                }
            }
            if (!excludedTypes.isEmpty()) {
                if (buf.length() > 0) {
                    buf.append(" AND ");
                }
                if (excludedTypes.size() == 1) {
                    for (MailItem.Type type : excludedTypes) {
                        buf.append("type != ").append(type.toByte());
                        break;
                    }
                } else {
                    buf.append("type NOT IN (");
                    boolean first = true;
                    for (MailItem.Type type : excludedTypes) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(", ");
                        }
                        buf.append(type.toByte());
                    }
                    buf.append(")");
                }
            }
            if (!folderIds.isEmpty()) {
                if (buf.length() > 0) {
                    buf.append(" AND ");
                }
                if (folderIds.size() == 1) {
                    buf.append("folder_id = ").append(folderIds.first());
                } else {
                    buf.append("folder_id IN (");
                    boolean first = true;
                    for (Integer fid : folderIds) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(", ");
                        }
                        buf.append(fid);
                    }
                    buf.append(")");
                }
            }
            if (changeDateBefore != null) {
                if (buf.length() > 0) {
                    buf.append(" AND ");
                }
                buf.append("change_date < ").append(changeDateBefore);
            }
            if (modifiedSequenceBefore != null) {
                if (buf.length() > 0) {
                    buf.append(" AND ");
                }
                buf.append("mod_metadata < ").append(modifiedSequenceBefore);
            }
            return buf.toString();
        }

        public String getLimitClause() {
            if (rowLimit != null && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                if (offset != null) {
                    return Db.getInstance().limit(offset, rowLimit);
                } else {
                    return Db.getInstance().limit(rowLimit);
                }
            }
            return "";
        }
    }

    /**
     * Returns the ids of items that match the given query parameters.
     * @return the matching ids, or an empty <tt>Set</tt>
     */
    public static Set<Integer> getIds(Mailbox mbox, DbConnection conn, QueryParams params, boolean fromDumpster)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Set<Integer> ids = new HashSet<Integer>();

        try {
            // Prepare the statement based on query parameters.
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT id FROM " + getMailItemTableName(mbox, fromDumpster) + " WHERE " + IN_THIS_MAILBOX_AND);
            String whereClause = params.getWhereClause();
            if (!StringUtil.isNullOrEmpty(whereClause)) {
                buf.append(whereClause);
            } else {
                buf.append("1 = 1");
            }
            String limitClause = params.getLimitClause();
            if (!StringUtil.isNullOrEmpty(limitClause)) {
                buf.append(" ").append(limitClause);
            }
            stmt = conn.prepareStatement(buf.toString());

            // Bind values, execute query, return results.
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);

            rs = stmt.executeQuery();

            while (rs.next())
                ids.add(rs.getInt(1));
            return ids;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting ids", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void addToCalendarItemTable(CalendarItem calItem) throws ServiceException {
        Mailbox mbox = calItem.getMailbox();

        long end = calItem.getEndTime();
        Timestamp startTs = new Timestamp(calItem.getStartTime());
        Timestamp endTs = new Timestamp(end <= 0 ? MAX_DATE : end);

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement("INSERT INTO " + getCalendarItemTableName(mbox) +
                        " (" + mailbox_id + "uid, item_id, start_time, end_time)" +
                        " VALUES (" + (DebugConfig.disableMailboxGroups ? "" : "?, ") + "?, ?, ?, ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, calItem.getUid());
            stmt.setInt(pos++, calItem.getId());
            stmt.setTimestamp(pos++, startTs);
            stmt.setTimestamp(pos++, endTs);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing invite to calendar item table: UID=" + calItem.getUid(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static long MAX_DATE = new GregorianCalendar(9999, 1, 1).getTimeInMillis();

    public static void updateInCalendarItemTable(CalendarItem calItem) throws ServiceException {
        Mailbox mbox = calItem.getMailbox();
        long end = calItem.getEndTime();
        Timestamp startTs = new Timestamp(calItem.getStartTime());
        Timestamp endTs = new Timestamp(end <= 0 ? MAX_DATE : end);

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String command = Db.supports(Db.Capability.REPLACE_INTO) ? "REPLACE" : "INSERT";
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement(command + " INTO " + getCalendarItemTableName(mbox) +
                        " (" + mailbox_id + "uid, item_id, start_time, end_time)" +
                        " VALUES (" + MAILBOX_ID_VALUE + "?, ?, ?, ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, calItem.getUid());
            stmt.setInt(pos++, calItem.getId());
            stmt.setTimestamp(pos++, startTs);
            stmt.setTimestamp(pos++, endTs);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                try {
                    DbPool.closeStatement(stmt);

                    stmt = conn.prepareStatement("UPDATE " + getCalendarItemTableName(mbox) +
                            " SET item_id = ?, start_time = ?, end_time = ? WHERE " + IN_THIS_MAILBOX_AND + "uid = ?");
                    int pos = 1;
                    stmt.setInt(pos++, calItem.getId());
                    stmt.setTimestamp(pos++, startTs);
                    stmt.setTimestamp(pos++, endTs);
                    pos = setMailboxId(stmt, mbox, pos);
                    stmt.setString(pos++, calItem.getUid());
                    stmt.executeUpdate();
                } catch (SQLException nested) {
                    throw ServiceException.FAILURE("updating data in calendar item table " + calItem.getUid(), nested);
                }
            } else {
                throw ServiceException.FAILURE("writing invite to calendar item table " + calItem.getUid(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static List<CalendarItem.CalendarMetadata> getCalendarItemMetadata(Folder folder, long start, long end) throws ServiceException {
        Mailbox mbox = folder.getMailbox();

        ArrayList<CalendarItem.CalendarMetadata> result = new ArrayList<CalendarItem.CalendarMetadata>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String startConstraint = start > 0 ? " AND ci.end_time > ?" : "";
            String endConstraint = end > 0 ? " AND ci.start_time < ?" : "";
            String folderConstraint = " AND mi.folder_id = ?";
            stmt = conn.prepareStatement("SELECT mi.mailbox_id, mi.id, ci.uid, mi.mod_metadata, mi.mod_content, ci.start_time, ci.end_time" +
                        " FROM " + getMailItemTableName(mbox, "mi") + ", " + getCalendarItemTableName(mbox, "ci") +
                        " WHERE mi.mailbox_id = ci.mailbox_id AND mi.id = ci.item_id" +
                        (DebugConfig.disableMailboxGroups ? "" : " AND mi.mailbox_id = ? ") +
                        startConstraint + endConstraint + folderConstraint);
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            if (start > 0)
                stmt.setTimestamp(pos++, new Timestamp(start));
            if (end > 0)
                stmt.setTimestamp(pos++, new Timestamp(end));
            stmt.setInt(pos++, folder.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new CalendarItem.CalendarMetadata(
                            rs.getInt(1),
                            rs.getInt(2),
                            rs.getString(3),
                            rs.getInt(4),
                            rs.getInt(5),
                            rs.getTimestamp(6).getTime(),
                            rs.getTimestamp(7).getTime()));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching CalendarItem Metadata for mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }


    public static void consistencyCheck(MailItem item, UnderlyingData data, String metadata) throws ServiceException {
        if (item.getId() <= 0)
            return;
        Mailbox mbox = item.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT mi.sender, " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            rs = stmt.executeQuery();

            if (!rs.next()) {
                throw ServiceException.FAILURE("consistency check failed: " + item.getType() + " " + item.getId() +
                        " not found in DB", null);
            }

            UnderlyingData dbdata = constructItem(rs, 1);
            String dbsender = rs.getString(1);

            String dataBlobDigest = data.getBlobDigest(), dbdataBlobDigest = dbdata.getBlobDigest();
            String dataSender = item.getSortSender(), dbdataSender = dbsender == null ? "" : dbsender;
            String failures = "";

            if (data.id != dbdata.id)                    failures += " ID";
            if (data.type != dbdata.type)                failures += " TYPE";
            if (data.folderId != dbdata.folderId)        failures += " FOLDER_ID";
            if (data.indexId != dbdata.indexId)          failures += " INDEX_ID";
            if (data.imapId != dbdata.imapId)            failures += " IMAP_ID";
            if (data.locator != dbdata.locator)          failures += " VOLUME_ID";
            if (data.date != dbdata.date)                failures += " DATE";
            if (data.size != dbdata.size)                failures += " SIZE";
            if (dbdata.type != MailItem.Type.CONVERSATION.toByte()) {
                if (data.unreadCount != dbdata.unreadCount)  failures += " UNREAD";
                if (data.getFlags() != dbdata.getFlags())    failures += " FLAGS";
                if (!TagUtil.tagsMatch(data.getTags(), dbdata.getTags()))  failures += " TAGS";
            }
            if (data.modMetadata != dbdata.modMetadata)  failures += " MOD_METADATA";
            if (data.dateChanged != dbdata.dateChanged)  failures += " CHANGE_DATE";
            if (data.modContent != dbdata.modContent)    failures += " MOD_CONTENT";
            if (Math.max(data.parentId, -1) != dbdata.parentId)  failures += " PARENT_ID";
            if (dataBlobDigest != dbdataBlobDigest && (dataBlobDigest == null || !dataBlobDigest.equals(dbdataBlobDigest)))  failures += " BLOB_DIGEST";
            if (dataSender != dbdataSender && (dataSender == null || !dataSender.equalsIgnoreCase(dbdataSender)))  failures += " SENDER";
            if (data.getSubject() != dbdata.getSubject() &&
                    (data.getSubject() == null || !data.getSubject().equals(dbdata.getSubject()))) {
                failures += " SUBJECT";
            }
            if (data.name != dbdata.name && (data.name == null || !data.name.equals(dbdata.name)))                 failures += " NAME";
            if (metadata != dbdata.metadata && (metadata == null || !metadata.equals(dbdata.metadata)))            failures += " METADATA";

            if (item instanceof Folder && dbdata.folderId != dbdata.parentId)  failures += " FOLDER!=PARENT";

            if (!failures.equals("")) {
                throw ServiceException.FAILURE("consistency check failed: " + item.getType() + " " + item.getId() +
                        " differs from DB at" + failures, null);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item " + item.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }


    /**
     * Makes sure that the argument won't overflow the maximum length by truncating the string if necessary.  Strips
     * surrogate characters from the string if needed so that the database (i.e. MySQL) doesn't choke on Unicode
     * characters outside the BMP (U+10000 and higher).
     *
     * @param value the string to check (can be null).
     * @param max maximum length
     * @return The passed-in String, truncated to the maximum length.
     */
    public static String normalize(String value, int max) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        String trimmed = value.length() <= max ? value : value.substring(0, max).trim();
        if (!Db.supports(Db.Capability.NON_BMP_CHARACTERS)) {
            trimmed = StringUtil.removeSurrogates(trimmed);
        }
        return trimmed;
    }

    /** Makes sure that the argument won't overflow the maximum length of a
     *  MySQL MEDIUMTEXT column (16,777,216 bytes) after conversion to UTF-8.
     *
     * @param metadata  The string to check (can be null).
     * @return The passed-in String, possibly re-encoded as base64 to hide
     *         non-BMP characters from the database.
     * @throws ServiceException <code>service.FAILURE</code> if the
     *         parameter would be silently truncated when inserted. */
    public static String checkMetadataLength(String metadata) throws ServiceException {
        if (metadata == null) {
            return null;
        }

        String result = encodeMetadata(metadata);
        int len = result.length();
        if (len > MAX_MEDIUMTEXT_LENGTH / 4) {  // every char uses 4 bytes in worst case
            if (StringUtil.isAsciiString(result)) {
                if (len > MAX_MEDIUMTEXT_LENGTH)
                    throw ServiceException.FAILURE("metadata too long", null);
            } else {
                try {
                    if (result.getBytes("utf-8").length > MAX_MEDIUMTEXT_LENGTH)
                        throw ServiceException.FAILURE("metadata too long", null);
                } catch (UnsupportedEncodingException uee) { }
            }
        }
        return result;
    }

    public static String encodeMetadata(String metadata) throws ServiceException {
        if (Db.supports(Db.Capability.NON_BMP_CHARACTERS) || !StringUtil.containsSurrogates(metadata)) {
            return metadata;
        }

        try {
            return new BCodec("utf-8").encode(metadata);
        } catch (EncoderException ee) {
            throw ServiceException.FAILURE("encoding non-BMP metadata", ee);
        }
    }

    public static String decodeMetadata(String metadata) throws ServiceException {
        if (StringUtil.isNullOrEmpty(metadata) || !metadata.startsWith("=?")) {
            return metadata;
        } else {
            try {
                return new BCodec("utf-8").decode(metadata);
            } catch (DecoderException de) {
                throw ServiceException.FAILURE("encoding non-BMP metadata", de);
            }
        }
    }

    /**
     * Returns the name of the table that stores {@link MailItem} data.  The table name is qualified
     * with the name of the database (e.g. <tt>mboxgroup1.mail_item</tt>).
     */
    public static String getMailItemTableName(int mailboxId, int groupId, boolean dumpster) {
        return DbMailbox.qualifyTableName(groupId, !dumpster ? TABLE_MAIL_ITEM : TABLE_MAIL_ITEM_DUMPSTER);
    }
    public static String getMailItemTableName(MailItem item) {
        return getMailItemTableName(item, false);
    }
    public static String getMailItemTableName(MailItem item, boolean dumpster) {
        return DbMailbox.qualifyTableName(item.getMailbox(), !dumpster ? TABLE_MAIL_ITEM : TABLE_MAIL_ITEM_DUMPSTER);
    }
    public static String getMailItemTableName(Mailbox mbox) {
        return getMailItemTableName(mbox, false);
    }
    public static String getMailItemTableName(Mailbox mbox, boolean dumpster) {
        return DbMailbox.qualifyTableName(mbox, !dumpster ? TABLE_MAIL_ITEM : TABLE_MAIL_ITEM_DUMPSTER);
    }
    public static String getMailItemTableName(Mailbox mbox, String alias) {
        return getMailItemTableName(mbox, alias, false);
    }
    public static String getMailItemTableName(Mailbox mbox, String alias, boolean dumpster) {
        return getMailItemTableName(mbox, dumpster) + " AS " + alias;
    }

    /**
     * Returns the name of the table that stores data on old revisions of {@link MailItem}s.
     * The table name is qualified by the name of the database (e.g. <tt>mailbox1.revision</tt>).
     */
    public static String getRevisionTableName(int mailboxId, int groupId, boolean dumpster) {
        return DbMailbox.qualifyTableName(groupId, !dumpster ? TABLE_REVISION : TABLE_REVISION_DUMPSTER);
    }
    public static String getRevisionTableName(MailItem item) {
        return getRevisionTableName(item, false);
    }
    public static String getRevisionTableName(MailItem item, boolean dumpster) {
        return DbMailbox.qualifyTableName(item.getMailbox(), !dumpster ? TABLE_REVISION : TABLE_REVISION_DUMPSTER);
    }
    public static String getRevisionTableName(Mailbox mbox) {
        return getRevisionTableName(mbox, false);
    }
    public static String getRevisionTableName(Mailbox mbox, boolean dumpster) {
        return DbMailbox.qualifyTableName(mbox, !dumpster ? TABLE_REVISION : TABLE_REVISION_DUMPSTER);
    }
    public static String getRevisionTableName(Mailbox mbox, String alias) {
        return getRevisionTableName(mbox, alias, false);
    }
    public static String getRevisionTableName(Mailbox mbox, String alias, boolean dumpster) {
        return getRevisionTableName(mbox, dumpster) + " AS " + alias;
    }

    /**
     * Returns the name of the table that stores {@link CalendarItem} data.  The table name is qualified
     * by the name of the database (e.g. <tt>mailbox1.appointment</tt>).
     */
    public static String getCalendarItemTableName(int mailboxId, int groupId, boolean dumpster) {
        return DbMailbox.qualifyTableName(groupId, !dumpster ? TABLE_APPOINTMENT : TABLE_APPOINTMENT_DUMPSTER);
    }
    public static String getCalendarItemTableName(Mailbox mbox) {
        return getCalendarItemTableName(mbox, false);
    }
    public static String getCalendarItemTableName(Mailbox mbox, boolean dumpster) {
        return DbMailbox.qualifyTableName(mbox, !dumpster ? TABLE_APPOINTMENT : TABLE_APPOINTMENT_DUMPSTER);
    }
    public static String getCalendarItemTableName(Mailbox mbox, String alias) {
        return getCalendarItemTableName(mbox, alias, false);
    }
    public static String getCalendarItemTableName(Mailbox mbox, String alias, boolean dumpster) {
        return getCalendarItemTableName(mbox, dumpster) + " AS " + alias;
    }

    /**
     * Returns the name of the table that maps subject hashes to {@link Conversation} ids.  The table
     * name is qualified by the name of the database (e.g. <tt>mailbox1.open_conversation</tt>).
     */
    public static String getConversationTableName(int mailboxId, int groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_OPEN_CONVERSATION);
    }
    public static String getConversationTableName(MailItem item) {
        return DbMailbox.qualifyTableName(item.getMailbox(), TABLE_OPEN_CONVERSATION);
    }
    public static String getConversationTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_OPEN_CONVERSATION);
    }
    public static String getConversationTableName(Mailbox mbox, String alias) {
        return getConversationTableName(mbox) + " AS " + alias;
    }

    /**
     * Returns the name of the table that stores data on deleted items for the purpose of sync.
     * The table name is qualified by the name of the database (e.g. <tt>mailbox1.tombstone</tt>).
     */
    public static String getTombstoneTableName(int mailboxId, int groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_TOMBSTONE);
    }
    public static String getTombstoneTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_TOMBSTONE);
    }

    /**
     * Returns a comma-separated list of ids for logging.  If the <tt>String</tt> is
     * more than 200 characters long, cuts off the list and appends &quot...&quot.
     */
    static String getIdListForLogging(Collection<Integer> ids) {
        if (ids == null)
            return null;
        StringBuilder idList = new StringBuilder();
        boolean firstTime = true;
        for (Integer id : ids) {
            if (firstTime) {
                firstTime = false;
            } else {
                idList.append(',');
            }
            idList.append(id);
            if (idList.length() > 200) {
                idList.append("...");
                break;
            }
        }
        return idList.toString();
    }

    public static TypedIdList listMsgItems(Folder folder, long messageSyncStart, boolean descending, boolean older) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        TypedIdList result = new TypedIdList();
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (older) {
                stmt = conn.prepareStatement("SELECT id, type FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND date < ?" +
                        " ORDER BY date" + (descending ? " DESC" : ""));
            } else {
                stmt = conn.prepareStatement("SELECT id, type FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND date >= ?" +
                        " ORDER BY date" + (descending ? " DESC" : ""));
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folder.getId());
            stmt.setLong(pos++, messageSyncStart);
            rs = stmt.executeQuery();

            while (rs.next()) {
                MailItem.Type type = MailItem.Type.of(rs.getByte(2));
                String row = rs.getString(1);
                if (row == null || row.equals(""))
                    continue;
                for (String entry : row.split(",")) {
                    try {
                        result.add(type, Integer.parseInt(entry));
                    } catch (NumberFormatException nfe) {
                        ZimbraLog.sync.warn("unparseable result entry: " + entry);
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item list for folder " + folder.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private final Mailbox mailbox;
    // The following data are only used for INSERT/UPDATE, not loaded by SELECT.
    private String sender;
    private String recipients;

    /**
     * {@link UnderlyingData} + extra data used for INSERT/UPDATE into MAIL_ITEM table.
     */
    public DbMailItem(Mailbox mbox) {
        mailbox = mbox;
    }

    public DbMailItem setSender(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            sender = DbMailItem.normalize(value, DbMailItem.MAX_SENDER_LENGTH);
        }
        return this;
    }

    public DbMailItem setRecipients(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            recipients = DbMailItem.normalize(value, DbMailItem.MAX_RECIPIENTS_LENGTH);
        }
        return this;
    }
}
