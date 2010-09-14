/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Aug 13, 2004
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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.VirtualConversation;
import com.zimbra.cs.mailbox.MailItem.PendingDelete;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.pop3.Pop3Message;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

public class DbMailItem {

    public static final String TABLE_MAIL_ITEM = "mail_item";
    public static final String TABLE_REVISION = "revision";
    public static final String TABLE_APPOINTMENT = "appointment";
    public static final String TABLE_OPEN_CONVERSATION = "open_conversation";
    public static final String TABLE_TOMBSTONE = "tombstone";

    private static Log sLog = LogFactory.getLog(DbMailItem.class);

    /** Maps the mailbox id to the set of all tag combinations stored for all
     *  items in the mailbox.  Enables fast database lookup by tag. */
    private static final Map<Long, TagsetCache> sTagsetCache =
        new TimeoutMap<Long, TagsetCache>(120 * Constants.MILLIS_PER_MINUTE);

    /** Maps the mailbox id to the set of all flag combinations stored for all
     *  items in the mailbox.  Enables fast database lookup by flag. */
    private static final Map<Long, TagsetCache> sFlagsetCache =
        new TimeoutMap<Long, TagsetCache>(120 * Constants.MILLIS_PER_MINUTE);

    public static final int MAX_SENDER_LENGTH  = 128;
    public static final int MAX_SUBJECT_LENGTH = 1024;
    public static final int MAX_TEXT_LENGTH    = 65534;
    public static final int MAX_MEDIUMTEXT_LENGTH = 16777216;

    public static final String IN_THIS_MAILBOX_AND = DebugConfig.disableMailboxGroups ? "" : "mailbox_id = ? AND ";
    public static final String MAILBOX_ID = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
    public static final String MAILBOX_ID_VALUE = DebugConfig.disableMailboxGroups ? "" : "?, ";

    private static final int RESULTS_STREAMING_MIN_ROWS = 10000; 
    
    public static final int setMailboxId(PreparedStatement stmt, Mailbox mbox, int pos) throws SQLException {
        if (!DebugConfig.disableMailboxGroups)
            stmt.setLong(pos++, mbox.getId());
        return pos;
    }
    
    public static final String getInThisMailboxAnd(long mboxId, String miAlias, String apAlias) {
        if (DebugConfig.disableMailboxGroups)
            return "";

        StringBuilder sb = new StringBuilder(miAlias).append(".mailbox_id = ").append(mboxId).append(" AND ");
        if (apAlias != null) 
            sb.append(apAlias).append(".mailbox_id = ").append(mboxId).append(" AND ");
        return sb.toString();
    }


    public static void create(Mailbox mbox, UnderlyingData data, String sender) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        if (data == null || data.id <= 0 || data.folderId <= 0 || data.parentId == 0)
            throw ServiceException.FAILURE("invalid data for DB item create", null);

        checkNamingConstraint(mbox, data.folderId, data.name, data.id);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement("INSERT INTO " + getMailItemTableName(mbox) +
                        "(" + mailbox_id +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tags, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "VALUES (" + MAILBOX_ID_VALUE + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, data.id);
            stmt.setByte(pos++, data.type);
            if (data.parentId <= 0)
                // Messages in virtual conversations are stored with a null parent_id
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, data.parentId);
            stmt.setInt(pos++, data.folderId);
            if (data.indexId == null)
                stmt.setNull(pos++, Types.VARCHAR);
            else
                stmt.setString(pos++, data.indexId);
            if (data.imapId <= 0)
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, data.imapId);
            stmt.setInt(pos++, data.date);
            stmt.setLong(pos++, data.size);
            if (data.locator != null)
                stmt.setString(pos++, data.locator);
            else
                stmt.setNull(pos++, Types.VARCHAR);
            stmt.setString(pos++, data.getBlobDigest());
            if (data.type == MailItem.TYPE_MESSAGE || data.type == MailItem.TYPE_CHAT || data.type == MailItem.TYPE_FOLDER)
                stmt.setInt(pos++, data.unreadCount);
            else
                stmt.setNull(pos++, Types.BOOLEAN);
            stmt.setInt(pos++, data.flags);
            stmt.setLong(pos++, data.tags);
            stmt.setString(pos++, checkSenderLength(sender));
            stmt.setString(pos++, checkSubjectLength(data.subject));
            stmt.setString(pos++, data.name);
            stmt.setString(pos++, checkMetadataLength(data.metadata));
            stmt.setInt(pos++, data.modMetadata);
            if (data.dateChanged > 0)
                stmt.setInt(pos++, data.dateChanged);
            else
                stmt.setNull(pos++, Types.INTEGER);
            stmt.setInt(pos++, data.modContent);
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to create object", null);

            // Track the tags and flags for fast lookup later
            if (areTagsetsLoaded(mbox))
                getTagsetCache(conn, mbox).addTagset(data.tags);
            if (areFlagsetsLoaded(mbox))
                getFlagsetCache(conn, mbox).addTagset(data.flags);
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
                throw MailServiceException.ALREADY_EXISTS(data.id, e);
            else
                throw ServiceException.FAILURE("writing new object of type " + data.type, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static void checkNamingConstraint(Mailbox mbox, int folderId, String name, int modifiedItemId) throws ServiceException {
        if (name == null || name.equals(""))
            return;
        if (Db.supports(Db.Capability.UNIQUE_NAME_INDEX) && !Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON))
            return;

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + getMailItemTableName(mbox) +
                    " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND id <> ? AND " + Db.equalsSTRING("name"));
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folderId);
            stmt.setInt(pos++, modifiedItemId);
            stmt.setString(pos++, StringUtil.trimTrailingSpaces(name.toUpperCase()));
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

    public static void copy(MailItem item, int id, Folder folder, String indexId, int parentId, String locator, String metadata)
    throws ServiceException {
        Mailbox mbox = item.getMailbox();
        if (id <= 0 || indexId == null || folder == null || parentId == 0)
            throw ServiceException.FAILURE("invalid data for DB item copy", null);

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        checkNamingConstraint(mbox, folder.getId(), item.getName(), id);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String table = getMailItemTableName(mbox);
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement("INSERT INTO " + table +
                        "(" + mailbox_id +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tags, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "SELECT " + MAILBOX_ID_VALUE +
                        " ?, type, ?, ?, ?, ?, date, size, ?, blob_digest, unread," +
                        " flags, tags, sender, subject, name, ?, ?, ?, ? FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, id);                            // ID
            if (parentId <= 0)
                stmt.setNull(pos++, Types.INTEGER);            // PARENT_ID null for messages in virtual convs
            else
                stmt.setInt(pos++, parentId);                  //   or, PARENT_ID specified by caller
            stmt.setInt(pos++, folder.getId());                // FOLDER_ID
            stmt.setString(pos++, indexId);                       // INDEX_ID
            stmt.setInt(pos++, id);                            // IMAP_ID is initially the same as ID
            if (locator != null)
                stmt.setString(pos++, locator);                // VOLUME_ID specified by caller
            else
                stmt.setNull(pos++, Types.VARCHAR);            //   or, no VOLUME_ID
            stmt.setString(pos++, checkMetadataLength(metadata));  // METADATA
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_METADATA
            stmt.setInt(pos++, mbox.getOperationTimestamp());  // CHANGE_DATE
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_CONTENT
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to create object", null);
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
                throw MailServiceException.ALREADY_EXISTS(id, e);
            else
                throw ServiceException.FAILURE("copying " + MailItem.getNameForType(item ) + ": " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void icopy(MailItem source, UnderlyingData data, boolean shared) throws ServiceException {
        Mailbox mbox = source.getMailbox();
        if (data == null || data.id <= 0 || data.folderId <= 0 || data.parentId == 0)
            throw ServiceException.FAILURE("invalid data for DB item i-copy", null);

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        checkNamingConstraint(mbox, data.folderId, source.getName(), data.id);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String table = getMailItemTableName(mbox);
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            String flags;
            if (!shared)
                flags = "flags";
            else if (Db.supports(Db.Capability.BITWISE_OPERATIONS))
                flags = "flags | " + Flag.BITMASK_COPIED;
            else
                flags = "CASE WHEN " + Db.bitmaskAND("flags", Flag.BITMASK_COPIED) + " THEN flags ELSE flags + " + Flag.BITMASK_COPIED + " END";
            stmt = conn.prepareStatement("INSERT INTO " + table +
                        "(" + mailbox_id +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tags, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "SELECT " + mailbox_id +
                        " ?, type, parent_id, ?, ?, ?, date, size, ?, blob_digest," +
                        " unread, " + flags + ", tags, sender, subject, name, metadata, ?, ?, ? FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, data.id);                       // ID
            stmt.setInt(pos++, data.folderId);                 // FOLDER_ID
            stmt.setString(pos++, data.indexId);               // INDEX_ID
            stmt.setInt(pos++, data.imapId);                   // IMAP_ID
            if (data.locator != null)
                stmt.setString(pos++, data.locator);           // VOLUME_ID
            else
                stmt.setNull(pos++, Types.TINYINT);            //   or, no VOLUME_ID
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_METADATA
            stmt.setInt(pos++, mbox.getOperationTimestamp());  // CHANGE_DATE
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_CONTENT
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, source.getId());
            stmt.executeUpdate();
            stmt.close();

            boolean needsTag = shared && !source.isTagged(Flag.ID_FLAG_COPIED);

            if (needsTag && areFlagsetsLoaded(mbox))
                getFlagsetCache(conn, mbox).addTagset(source.getInternalFlagBitmask() | Flag.BITMASK_COPIED);

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

            if (source instanceof Message && source.getParentId() <= 0)
                changeOpenTarget(Mailbox.getHash(((Message) source).getNormalizedSubject()), source, data.id);
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
                throw MailServiceException.ALREADY_EXISTS(data.id, e);
            else
                throw ServiceException.FAILURE("i-copying " + MailItem.getNameForType(source) + ": " + source.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void snapshotRevision(MailItem item, int version) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(version >= 1);
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
                throw MailServiceException.ALREADY_EXISTS(item.getId(), e);
            else
                throw ServiceException.FAILURE("saving revision info for " + MailItem.getNameForType(item) + ": " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void purgeRevisions(MailItem item, int revision, boolean includeOlderRevisions) throws ServiceException {
        if (revision <= 0)
            return;
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
            throw ServiceException.FAILURE("purging revisions for " + MailItem.getNameForType(item) + ": " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
    
    public static void changeType(MailItem item, byte type) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        checkNamingConstraint(mbox, folder.getId(), item.getName(), item.getId());

        Connection conn = mbox.getOperationConnection();
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
            if (hasIndexId)
                if (item.getIndexId() == null)
                    stmt.setNull(pos++, Types.VARCHAR);
                else
                    stmt.setString(pos++, item.getIndexId());
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
    
    public static void setIndexIds(Mailbox mbox, List<Message> msgs) throws ServiceException {
        if (msgs == null || msgs.isEmpty())
            return;
        
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            for (int i = 0; i < msgs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), msgs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET index_id = id" +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                int pos = 1;
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
    

    public static void setParent(MailItem child, MailItem parent) throws ServiceException {
        setParent(new MailItem[] { child }, parent);
    }

    public static void setParent(MailItem[] children, MailItem parent) throws ServiceException {
        if (children == null || children.length == 0)
            return;
        Mailbox mbox = children[0].getMailbox();
        if (mbox != parent.getMailbox())
            throw MailServiceException.WRONG_MAILBOX();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String relation = (oldParent instanceof VirtualConversation ? "id = ?" : "parent_id = ?");

            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(oldParent) +
                        " SET parent_id = ?, mod_metadata = ?, change_date = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + relation);
            int pos = 1;
            if (newParent instanceof VirtualConversation)
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, newParent.getId());
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

    public static void persistCounts(MailItem item, String metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET size = ?, unread = ?, metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setLong(pos++, item.getSize());
            stmt.setInt(pos++, item.getUnreadCount());
            stmt.setString(pos++, checkMetadataLength(metadata));
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(note) +
                        " SET date = ?, size = ?, subject = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (note.getDate() / 1000));
            stmt.setLong(pos++, note.getSize());
            stmt.setString(pos++, checkSubjectLength(note.getSubject()));
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

    public static void saveName(MailItem item, int folderId, String metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        String name = item.getName().equals("") ? null : item.getName();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        checkNamingConstraint(mbox, folderId, name, item.getId());

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            boolean isFolder = item instanceof Folder;
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET date = ?, size = ?, flags = ?, name = ?, subject = ?," +
                        "  folder_id = ?," + (isFolder ? " parent_id = ?," : "") +
                        "  metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setLong(pos++, item.getSize());
            stmt.setInt(pos++, item.getInternalFlagBitmask());
            stmt.setString(pos++, name);
            stmt.setString(pos++, name);
            stmt.setInt(pos++, folderId);
            if (isFolder)
                stmt.setInt(pos++, folderId);
            stmt.setString(pos++, metadata);
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
                throw MailServiceException.ALREADY_EXISTS(name, e);
            else
                throw ServiceException.FAILURE("writing name for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveData(MailItem item, String subject, String sender, String metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        String name = item.getName().equals("") ? null : item.getName();

        if (item instanceof Conversation)
            subject = ((Conversation) item).getNormalizedSubject();
        else if (item instanceof Message)
            subject = ((Message) item).getNormalizedSubject();

        checkNamingConstraint(mbox, item.getFolderId(), name, item.getId());

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET type = ?, imap_id = ?, parent_id = ?, date = ?, size = ?, flags = ?," +
                        "  blob_digest = ?, sender = ?, subject = ?, name = ?, metadata = ?," +
                        "  mod_metadata = ?, change_date = ?, mod_content = ?, volume_id = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setByte(pos++, item.getType());
            if (item.getImapUid() >= 0)
                stmt.setInt(pos++, item.getImapUid());
            else
                stmt.setNull(pos++, Types.INTEGER);
            // messages in virtual conversations are stored with a null parent_id
            if (item.getParentId() <= 0)
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, item.getParentId());
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setLong(pos++, item.getSize());
            stmt.setInt(pos++, item.getInternalFlagBitmask());
            stmt.setString(pos++, item.getDigest());
            stmt.setString(pos++, checkSenderLength(sender));
            stmt.setString(pos++, checkSubjectLength(subject));
            stmt.setString(pos++, name);
            stmt.setString(pos++, checkMetadataLength(metadata));
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, item.getSavedSequence());
            if (item.getLocator() != null)
                stmt.setString(pos++, item.getLocator());
            else
                stmt.setNull(pos++, Types.TINYINT);
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();

            // Update the flagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (areFlagsetsLoaded(mbox))
                getFlagsetCache(conn, mbox).addTagset(item.getInternalFlagBitmask());
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
                throw MailServiceException.ALREADY_EXISTS(item.getName(), e);
            else
                throw ServiceException.FAILURE("rewriting row data for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveBlobInfo(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET size = ?, blob_digest = ?, volume_id = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setLong(pos++, item.getSize());
            stmt.setString(pos++, item.getDigest());
            if (item.getLocator() != null)
                stmt.setString(pos++, item.getLocator());
            else
                stmt.setNull(pos++, Types.TINYINT);
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
    
    public static void changeOpenTarget(String hash, MailItem oldTarget, int newTargetId) throws ServiceException {
        Mailbox mbox = oldTarget.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getConversationTableName(oldTarget) +
                        " SET conv_id = ? WHERE " + IN_THIS_MAILBOX_AND + "hash = ? AND conv_id = ?");
            int pos = 1;
            stmt.setInt(pos++, newTargetId);
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, hash);
            stmt.setInt(pos++, oldTarget.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("switching open conversation association for item " + oldTarget.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveDate(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

    public static void alterTag(MailItem item, Tag tag, boolean add) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        if (mbox != tag.getMailbox())
            throw MailServiceException.WRONG_MAILBOX();
        if (tag.getId() == Flag.ID_FLAG_UNREAD)
            throw ServiceException.FAILURE("unread state must be updated with alterUnread()", null);

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            boolean isFlag = tag instanceof Flag;
            boolean altersModseq = !isFlag || (tag.getBitmask() & Flag.FLAG_SYSTEM) == 0;
            String column = (isFlag ? "flags" : "tags");

            String primaryUpdate = column + " = " + column + (add ? " + ?" : " - ?");
            String updateChangeID = (altersModseq ? ", mod_metadata = ?, change_date = ?" : "");
            String precondition = (add ? "NOT " : "") + Db.bitmaskAND(column);

            String relation;
            if (item instanceof VirtualConversation)  relation = "id = ?";
            else if (item instanceof Conversation)    relation = "parent_id = ?";
            else if (item instanceof Folder)          relation = "folder_id = ?";
            else if (item instanceof Flag)            relation = Db.bitmaskAND("flags");
            else if (item instanceof Tag)             relation = Db.bitmaskAND("tags");
            else                                      relation = "id = ?";

            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET " + primaryUpdate + updateChangeID +
                    " WHERE " + IN_THIS_MAILBOX_AND + precondition + " AND " + relation);

            int pos = 1;
            stmt.setLong(pos++, tag.getBitmask());
            if (altersModseq) {
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
            }
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setLong(pos++, tag.getBitmask());
            if (item instanceof Tag)
                stmt.setLong(pos++, ((Tag) item).getBitmask());
            else if (item instanceof VirtualConversation)
                stmt.setInt(pos++, ((VirtualConversation) item).getMessageId());
            else
                stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();

            // Update the flagset or tagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (tag instanceof Flag && areFlagsetsLoaded(mbox))
                getFlagsetCache(conn, mbox).addTagset(item.getInternalFlagBitmask());
            else if (areTagsetsLoaded(mbox))
                getTagsetCache(conn, mbox).addTagset(item.getTagBitmask());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating tag data for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void alterTag(Tag tag, List<Integer> itemIDs, boolean add)
    throws ServiceException {
        if (itemIDs == null || itemIDs.isEmpty())
            return;
        Mailbox mbox = tag.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            boolean isFlag = tag instanceof Flag;
            boolean altersModseq = !isFlag || (tag.getBitmask() & Flag.FLAG_SYSTEM) == 0;
            String column = (isFlag ? "flags" : "tags");

            String primaryUpdate = column + " = " + column + (add ? " + ?" : " - ?");
            String updateChangeID = (altersModseq ? ", mod_metadata = ?, change_date = ?" : "");
            String precondition = (add ? "NOT " : "") + Db.bitmaskAND(column);

            for (int i = 0; i < itemIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), itemIDs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(tag) +
                            " SET " + primaryUpdate + updateChangeID +
                            " WHERE " + IN_THIS_MAILBOX_AND + precondition + " AND " + DbUtil.whereIn("id", count));

                int pos = 1;
                stmt.setLong(pos++, tag.getBitmask());
                if (altersModseq) {
                    stmt.setInt(pos++, mbox.getOperationChangeID());
                    stmt.setInt(pos++, mbox.getOperationTimestamp());
                }
                pos = setMailboxId(stmt, mbox, pos);
                stmt.setLong(pos++, tag.getBitmask());
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, itemIDs.get(index));
                stmt.executeUpdate();
                stmt.close();
                stmt = null;
            }

            // Update the flagset or tagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (tag instanceof Flag && areFlagsetsLoaded(mbox))
                getFlagsetCache(conn, mbox).applyMask(tag.getBitmask(), add);
            else if (areTagsetsLoaded(mbox))
                getTagsetCache(conn, mbox).applyMask(tag.getBitmask(), add);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating tag data for " + itemIDs.size() + " items: " + getIdListForLogging(itemIDs), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void clearTag(Tag tag) throws ServiceException {
        Mailbox mbox = tag.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(tag) +
                        " SET tags = tags - ?, mod_metadata = ?, change_date = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + Db.bitmaskAND("tags"));
            int pos = 1;
            stmt.setLong(pos++, tag.getBitmask());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setLong(pos++, tag.getBitmask());
            stmt.executeUpdate();

            if (areTagsetsLoaded(mbox))
                getTagsetCache(conn, mbox).applyMask(tag.getTagBitmask(), false);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("clearing all references to tag " + tag.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Sets the <code>unread</code> column for the specified <code>MailItem</code>.
     * If the <code>MailItem</code> is a <code>Conversation</code>, <code>Tag</code>
     * or <code>Folder</code>, sets the <code>unread</code> column for all related items.
     */
    public static void alterUnread(MailItem item, boolean unread)
    throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String relation;
            if (item instanceof VirtualConversation)  relation = "id = ?";
            else if (item instanceof Conversation)    relation = "parent_id = ?";
            else if (item instanceof Folder)          relation = "folder_id = ?";
            else if (item instanceof Flag)            relation = Db.bitmaskAND("flags");
            else if (item instanceof Tag)             relation = Db.bitmaskAND("tags");
            else                                      relation = "id = ?";

            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET unread = ?, mod_metadata = ?, change_date = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "unread = ? AND " + relation +
                        "  AND " + typeIn(MailItem.TYPE_MESSAGE));
            int pos = 1;
            stmt.setInt(pos++, unread ? 1 : 0);
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, unread ? 0 : 1);
            if (item instanceof Tag)
                stmt.setLong(pos++, ((Tag) item).getBitmask());
            else if (item instanceof VirtualConversation)
                stmt.setInt(pos++, ((VirtualConversation) item).getMessageId());
            else
                stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating unread state for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void alterUnread(Mailbox mbox, List<Integer> itemIDs, boolean unread)
    throws ServiceException {
        if (itemIDs == null || itemIDs.isEmpty())
            return;

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            for (int i = 0; i < itemIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), itemIDs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET unread = ?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "unread = ?" +
                            "  AND " + DbUtil.whereIn("id", count) +
                            "  AND " + typeIn(MailItem.TYPE_MESSAGE));
                int pos = 1;
                stmt.setInt(pos++, unread ? 1 : 0);
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, unread ? 0 : 1);
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, itemIDs.get(index));
                stmt.executeUpdate();
                stmt.close();
                stmt = null;
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (Db.supports(Db.Capability.MULTITABLE_UPDATE)) {
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(folder) + ", " +
                            "(SELECT parent_id pid, COUNT(*) count FROM " + getMailItemTableName(folder) +
                            " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND parent_id IS NOT NULL GROUP BY parent_id) AS x" +
                            " SET size = size - count, metadata = NULL, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = pid AND type = " + MailItem.TYPE_CONVERSATION);
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
                                "  AND type = " + MailItem.TYPE_CONVERSATION);
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
                                " WHERE " + IN_THIS_MAILBOX_AND + "id = pid AND type = " + MailItem.TYPE_CONVERSATION);
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
                            " AND type = " + MailItem.TYPE_CONVERSATION);
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
     * deletes any corresponding messages.  Does not delete subfolders.
     */
    public static void delete(MailItem item) throws ServiceException {
        deleteContents(item);
        if (item instanceof VirtualConversation)
            return;

        List<Integer> ids = new ArrayList<Integer>();
        ids.add(item.getId());
        delete(item.getMailbox(), ids);
    }

    /**
     * Deletes <code>MailItem</code>s with the specified ids from the <code>mail_item</code>
     * table.  Assumes that there is no data referencing the specified id's.
     */
    public static void delete(Mailbox mbox, List<Integer> ids) throws ServiceException {
        // trim out any non-persisted items
        if (ids == null || ids.size() == 0)
            return;
        List<Integer> targets = new ArrayList<Integer>();
        for (int id : ids) {
            if (id > 0)
                targets.add(id);
        }
        if (targets.size() == 0)
            return;

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        for (int i = 0; i < targets.size(); i += Db.getINClauseBatchSize()) {
            try {
                int count = Math.min(Db.getINClauseBatchSize(), targets.size() - i);
                stmt = conn.prepareStatement("DELETE FROM " + getMailItemTableName(mbox) +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, targets.get(index));
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("deleting " + ids.size() + " item(s): " + getIdListForLogging(ids), e);
            } finally {
                DbPool.closeStatement(stmt);
            }
        }
    }

    public static void deleteContents(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        String target;
        if (item instanceof VirtualConversation)  target = "id = ?";
        else if (item instanceof Conversation)    target = "parent_id = ?";
        else if (item instanceof SearchFolder)    return;
        else if (item instanceof Folder)          target = "folder_id = ?";
        else                                      return;

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getMailItemTableName(item) +
                        " WHERE " + IN_THIS_MAILBOX_AND + target + " AND type NOT IN " + FOLDER_TYPES);
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item instanceof VirtualConversation ? ((VirtualConversation) item).getMessageId() : item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting contents for " + MailItem.getNameForType(item) + " " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void writeTombstones(Mailbox mbox, TypedIdList tombstones) throws ServiceException {
        if (tombstones == null || tombstones.isEmpty())
            return;

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        for (Map.Entry<Byte, List<Integer>> entry : tombstones) {
            byte type = entry.getKey();
            if (type == MailItem.TYPE_CONVERSATION || type == MailItem.TYPE_VIRTUAL_CONVERSATION)
                continue;
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

    private static void writeTombstone(Mailbox mbox, byte type, String ids) throws ServiceException {
        if (ids == null || ids.equals(""))
            return;

        Connection conn = mbox.getOperationConnection();
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
            stmt.setByte(pos++, type);
            stmt.setString(pos++, ids);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing tombstones for " + MailItem.getNameForType(type) + "(s): " + ids, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static TypedIdList readTombstones(Mailbox mbox, long lastSync) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        TypedIdList tombstones = new TypedIdList();

        Connection conn = mbox.getOperationConnection();
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
                byte type = rs.getByte(1);
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
     * @return the number of tombstones deleted
     */
    public static int purgeTombstones(Mailbox mbox, int beforeDate)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(
                "DELETE FROM " + getTombstoneTableName(mbox) +
                " WHERE " + IN_THIS_MAILBOX_AND + "date < ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setLong(pos++, beforeDate);
            int numRows = stmt.executeUpdate();
            if (numRows > 0) {
                ZimbraLog.mailbox.info("Purged %d tombstones dated before %d.", numRows, beforeDate);
            }
            return numRows;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("purging tombstones with date before " + beforeDate, null);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
    
    private static final String FOLDER_TYPES         = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ')';
    private static final String FOLDER_AND_TAG_TYPES = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ',' + MailItem.TYPE_TAG + ')';
    private static final String MESSAGE_TYPES        = "(" + MailItem.TYPE_MESSAGE + ',' + MailItem.TYPE_CHAT + ')';
    private static final String DOCUMENT_TYPES       = "(" + MailItem.TYPE_DOCUMENT + ',' + MailItem.TYPE_WIKI + ')';
    private static final String CALENDAR_TYPES       = "(" + MailItem.TYPE_APPOINTMENT + ',' + MailItem.TYPE_TASK + ')';

    static final String NON_SEARCHABLE_TYPES = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ',' + MailItem.TYPE_TAG + ',' + MailItem.TYPE_CONVERSATION + ')';

    private static String typeIn(byte type) {
        if (type == MailItem.TYPE_FOLDER)
            return "type IN " + FOLDER_TYPES;
        else if (type == MailItem.TYPE_MESSAGE)
            return "type IN " + MESSAGE_TYPES;
        else if (type == MailItem.TYPE_DOCUMENT)
            return "type IN " + DOCUMENT_TYPES;
        else
            return "type = " + type;
    }

    public static class FolderTagMap extends HashMap<UnderlyingData, FolderTagCounts> { }

    public static class FolderTagCounts {
        public int totalSize, deletedCount, deletedUnreadCount;
        @Override public String toString()  { return totalSize + "/" + deletedCount + "/" + deletedUnreadCount; }
    }

    public static Mailbox.MailboxData getFoldersAndTags(Mailbox mbox, FolderTagMap folderData, FolderTagMap tagData, boolean reload)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String table = getMailItemTableName(mbox, "mi");

            stmt = conn.prepareStatement("SELECT " + DB_FIELDS + " FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "type IN " + FOLDER_AND_TAG_TYPES);
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();
            while (rs.next()) {
                UnderlyingData data = constructItem(rs);
                if (MailItem.isAcceptableType(MailItem.TYPE_FOLDER, data.type))
                    folderData.put(data, null);
                else if (MailItem.isAcceptableType(MailItem.TYPE_TAG, data.type))
                    tagData.put(data, null);

                rs.getInt(CI_UNREAD);
                reload |= rs.wasNull();
            }
            rs.close();

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
                    ZimbraLog.mailbox.info("correcting PARENT_ID column for " + MailItem.getNameForType(data.type) + " " + data.id);
                }
            }

            if (!reload)
                return null;

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

            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            stmt = conn.prepareStatement("SELECT folder_id, type, tags, flags, COUNT(*), SUM(unread), SUM(size)" +
                        " FROM " + table + " WHERE " + IN_THIS_MAILBOX_AND + "type NOT IN " + NON_SEARCHABLE_TYPES +
                        " GROUP BY folder_id, type, tags, flags");
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int folderId = rs.getInt(1);
                byte type  = rs.getByte(2);
                long tags  = rs.getLong(3);
                boolean deleted = (rs.getInt(4) & Flag.BITMASK_DELETED) != 0;
                int count  = rs.getInt(5);
                int unread = rs.getInt(6);
                long size  = rs.getLong(7);

                if (type == MailItem.TYPE_CONTACT)
                    mbd.contacts += count;
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
                    ZimbraLog.mailbox.warn("inconsistent DB state: items with no corresponding folder (folder ID " + folderId + ")");
                }

                for (int i = 0; tags != 0 && i < MailItem.MAX_TAG_COUNT - 1; i++) {
                    if ((tags & (1L << i)) != 0) {
                        data = lookup.get(i + MailItem.TAG_ID_OFFSET);
                        if (data != null) {
                            // not keeping track of item counts on tags, just unread counts
                            data.unreadCount += unread;
                            if (deleted)
                                tagData.get(data).deletedUnreadCount += unread;
                        } else {
                            ZimbraLog.mailbox.warn("inconsistent DB state: items with no corresponding tag (tag ID " + (i + MailItem.TAG_ID_OFFSET) + ")");
                        }
                        // could track cumulative count if desired...
                        tags &= ~(1L << i);
                    }
                }
            }

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
                if (data != null)
                    folderData.get(data).totalSize += size;
                else
                    ZimbraLog.mailbox.warn("inconsistent DB state: revisions with no corresponding folder (folder ID " + folderId + ")");
            }

            return mbd;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching folder data for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getByType(Mailbox mbox, byte type, SortBy sort) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox, " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + typeIn(type) + DbSearch.sortQuery(sort));
            if (type == MailItem.TYPE_MESSAGE)
                Db.getInstance().enableStreaming(stmt);
            setMailboxId(stmt, mbox, 1);
            rs = stmt.executeQuery();
            while (rs.next())
                result.add(constructItem(rs));
            rs.close(); rs = null;
            stmt.close(); stmt = null;

            if (type == MailItem.TYPE_CONVERSATION)
                completeConversations(mbox, result);
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching items of type " + type, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getByParent(MailItem parent) throws ServiceException {
        return getByParent(parent, SortBy.DATE_DESCENDING);
    }

    public static List<UnderlyingData> getByParent(MailItem parent, SortBy sort) throws ServiceException {
        Mailbox mbox = parent.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(parent.getMailbox(), " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + "parent_id = ? " + DbSearch.sortQuery(sort));
            if (parent.getSize() > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, parent.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                UnderlyingData data = constructItem(rs);
                if (Mailbox.isCachedType(data.type))
                    throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
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
        Mailbox mbox = relativeTo.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String relation;
            if (relativeTo instanceof VirtualConversation)  relation = "id = ?";
            else if (relativeTo instanceof Conversation)    relation = "parent_id = ?";
            else if (relativeTo instanceof Folder)          relation = "folder_id = ?";
            else if (relativeTo instanceof Flag)            relation = Db.bitmaskAND("flags");
            else if (relativeTo instanceof Tag)             relation = Db.bitmaskAND("tags");
            else                                            relation = "id = ?";

            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(relativeTo.getMailbox(), " mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "unread > 0 AND " + relation + " AND type NOT IN " + NON_SEARCHABLE_TYPES);
            if (relativeTo.getUnreadCount() > RESULTS_STREAMING_MIN_ROWS)
                Db.getInstance().enableStreaming(stmt);
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            if (relativeTo instanceof Tag)
                stmt.setLong(pos++, ((Tag) relativeTo).getBitmask());
            else if (relativeTo instanceof VirtualConversation)
                stmt.setInt(pos++, ((VirtualConversation) relativeTo).getMessageId());
            else
                stmt.setInt(pos++, relativeTo.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                UnderlyingData data = constructItem(rs);
                if (Mailbox.isCachedType(data.type))
                    throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
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

    public static List<UnderlyingData> getByFolder(Folder folder, byte type, SortBy sort) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        Mailbox mbox = folder.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(folder.getMailbox(), " mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND " + typeIn(type) +
                        DbSearch.sortQuery(sort));
            if (folder.getSize() > RESULTS_STREAMING_MIN_ROWS && type == MailItem.TYPE_MESSAGE) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folder.getId());
            rs = stmt.executeQuery();

            while (rs.next())
                result.add(constructItem(rs));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching items in folder " + folder.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static UnderlyingData getById(Mailbox mbox, int id, byte type) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, id);
            rs = stmt.executeQuery();

            if (!rs.next())
                throw MailItem.noSuchItem(id, type);
            UnderlyingData data = constructItem(rs);
            if (!MailItem.isAcceptableType(type, data.type))
                throw MailItem.noSuchItem(id, type);
            if (data.type == MailItem.TYPE_CONVERSATION)
                completeConversation(mbox, data);
            return data;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item " + id, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static UnderlyingData getByImapId(Mailbox mbox, int imapId, int folderId) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

            if (!rs.next())
                throw MailServiceException.NO_SUCH_ITEM(imapId);
            UnderlyingData data = constructItem(rs);
            if (data.type == MailItem.TYPE_CONVERSATION)
                throw MailServiceException.NO_SUCH_ITEM(imapId);
            return data;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item " + imapId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getById(Mailbox mbox, Collection<Integer> ids, byte type) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        List<UnderlyingData> result = new ArrayList<UnderlyingData>();
        if (ids.isEmpty())
            return result;
        List<UnderlyingData> conversations = new ArrayList<UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
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
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, it.next());

                rs = stmt.executeQuery();
                while (rs.next()) {
                    UnderlyingData data = constructItem(rs);
                    if (!MailItem.isAcceptableType(type, data.type))
                        throw MailItem.noSuchItem(data.id, type);
                    else if (Mailbox.isCachedType(data.type))
                        throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
                    if (data.type == MailItem.TYPE_CONVERSATION)
                        conversations.add(data);
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
            completeConversations(mbox, conversations);
        return result;
    }

    public static UnderlyingData getByName(Mailbox mbox, int folderId, String name, byte type) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
            stmt.setString(pos++, name.toUpperCase());
            rs = stmt.executeQuery();

            if (!rs.next())
                throw MailItem.noSuchItem(-1, type);
            UnderlyingData data = constructItem(rs);
            if (!MailItem.isAcceptableType(type, data.type))
                throw MailItem.noSuchItem(data.id, type);
            if (data.type == MailItem.TYPE_CONVERSATION)
                completeConversation(mbox, data);
            return data;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item by name ('" + name + "' in folder " + folderId + ")", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static UnderlyingData getByHash(Mailbox mbox, String hash) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") + ", " + getConversationTableName(mbox, "oc") +
                        " WHERE oc.hash = ? AND mi.id = oc.conv_id" +
                        (DebugConfig.disableMailboxGroups ? "" : " AND oc.mailbox_id = ? AND mi.mailbox_id = oc.mailbox_id"));
            int pos = 1;
            stmt.setString(pos++, hash);
            pos = setMailboxId(stmt, mbox, pos);
            rs = stmt.executeQuery();

            if (!rs.next())
                return null;
            UnderlyingData data = constructItem(rs);
            if (data.type == MailItem.TYPE_CONVERSATION)
                completeConversation(mbox, data);
            return data;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching conversation for hash " + hash, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static Pair<List<Integer>,TypedIdList> getModifiedItems(Mailbox mbox, byte type, long lastSync, Set<Integer> visible)
    throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        List<Integer> modified = new ArrayList<Integer>();
        TypedIdList missed = new TypedIdList();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String typeConstraint = type == MailItem.TYPE_UNKNOWN ? "type NOT IN " + NON_SEARCHABLE_TYPES : typeIn(type);
            stmt = conn.prepareStatement("SELECT id, type, folder_id" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "mod_metadata > ? AND " + typeConstraint +
                        " ORDER BY mod_metadata, id");
            if (type == MailItem.TYPE_MESSAGE) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setLong(pos++, lastSync);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (visible == null || visible.contains(rs.getInt(3)))
                    modified.add(rs.getInt(1));
                else
                    missed.add(rs.getByte(2), rs.getInt(1));
            }

            return new Pair<List<Integer>,TypedIdList>(modified, missed);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting items modified since " + lastSync, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void completeConversation(Mailbox mbox, UnderlyingData data) throws ServiceException {
        completeConversations(mbox, Arrays.asList(data));
    }

    private static void completeConversations(Mailbox mbox, List<UnderlyingData> convData) throws ServiceException {
        if (convData == null || convData.isEmpty())
            return;
        for (UnderlyingData data : convData) {
            if (data.type != MailItem.TYPE_CONVERSATION)
                throw ServiceException.FAILURE("attempting to complete a non-conversation", null);
        }

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Map<Integer, UnderlyingData> conversations = new HashMap<Integer, UnderlyingData>(Db.getINClauseBatchSize() * 3 / 2);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        for (int i = 0; i < convData.size(); i += Db.getINClauseBatchSize()) {
            try {
                int count = Math.min(Db.getINClauseBatchSize(), convData.size() - i);
                stmt = conn.prepareStatement("SELECT parent_id, unread, flags, tags" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("parent_id", count));
                int pos = 1;
                pos = setMailboxId(stmt, mbox, pos);
                for (int index = i; index < i + count; index++) {
                    UnderlyingData data = convData.get(index);
                    stmt.setInt(pos++, data.id);
                    conversations.put(data.id, data);
                    // don't assume that the UnderlyingData structure was new...
                    data.tags = data.flags = data.unreadCount = 0;
                }
                rs = stmt.executeQuery();

                while (rs.next()) {
                    UnderlyingData data = conversations.get(rs.getInt(1));
                    assert(data != null);
                    data.unreadCount += rs.getInt(2);
                    data.flags       |= rs.getInt(3);
                    data.tags        |= rs.getLong(4);
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

    private static final String LEAF_NODE_FIELDS = "id, size, type, unread, folder_id, parent_id, blob_digest," +
                                                   " mod_content, mod_metadata, flags, index_id, volume_id";

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

    public static PendingDelete getLeafNodes(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        PendingDelete info = new PendingDelete();
        int folderId = folder.getId();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type NOT IN " + FOLDER_TYPES);
            if (folder.getSize() > RESULTS_STREAMING_MIN_ROWS)
                Db.getInstance().enableStreaming(stmt);
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folderId);
            rs = stmt.executeQuery();

            info.rootId = folderId;
            info.size   = 0;
            List<Integer> versionedIds = accumulateLeafNodes(info, mbox, rs);
            rs.close(); rs = null;
            stmt.close(); stmt = null;
            accumulateLeafRevisions(info, mbox, versionedIds);
            
            // make sure that the folder is in the list of deleted item ids
            info.itemIds.add(folder.getType(), folderId);

            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of items within item " + folder.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
    
    public static PendingDelete getLeafNodes(Mailbox mbox, List<Folder> folders, int before, boolean globalMessages,
                                             Boolean unread, boolean useChangeDate, Integer maxItems)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        PendingDelete info = new PendingDelete();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String constraint;
            String dateColumn = (useChangeDate ? "change_date" : "date");
            if (globalMessages)
                constraint = dateColumn + " < ? AND " + typeIn(MailItem.TYPE_MESSAGE);
            else
                constraint = dateColumn + " < ? AND type NOT IN " + NON_SEARCHABLE_TYPES +
                             " AND " + DbUtil.whereIn("folder_id", folders.size());
            if (unread != null)
                constraint += " AND unread = ?";
            String orderByLimit = "";
            if (maxItems != null && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                orderByLimit = " ORDER BY " + dateColumn + " LIMIT " + maxItems;
            }

            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + constraint + orderByLimit);
            if (globalMessages || getTotalFolderSize(folders) > RESULTS_STREAMING_MIN_ROWS)
                Db.getInstance().enableStreaming(stmt);
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, before);
            if (!globalMessages) {
                for (Folder folder : folders)
                    stmt.setInt(pos++, folder.getId());
            }
            if (unread != null)
                stmt.setBoolean(pos++, unread);
            rs = stmt.executeQuery();

            info.rootId = 0;
            info.size   = 0;
            List<Integer> versionedIds = accumulateLeafNodes(info, mbox, rs);
            rs.close(); rs = null;
            stmt.close(); stmt = null;
            accumulateLeafRevisions(info, mbox, versionedIds);
            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of items for purge", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
    
    private static int getTotalFolderSize(Collection<Folder> folders) {
        int totalSize = 0;
        if (folders != null) {
            for (Folder folder : folders) {
                totalSize += folder.getSize();
            }
        }
        return totalSize;
    }

    public static PendingDelete getImapDeleted(Mailbox mbox, Set<Folder> folders) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        PendingDelete info = new PendingDelete();
        if (folders != null && folders.isEmpty())
            return info;

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // figure out the set of FLAGS bitmasks containing the \Deleted flag
            Set<Long> flagsets = getFlagsetCache(conn, mbox).getMatchingTagsets(Flag.BITMASK_DELETED, Flag.BITMASK_DELETED);
            if (flagsets != null && flagsets.isEmpty())
                return info;

            String flagconstraint = flagsets == null ? "" : " AND " + DbUtil.whereIn("flags", flagsets.size());
            String folderconstraint = folders == null ? "" : " AND " + DbUtil.whereIn("folder_id", folders.size());

            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "type IN " + IMAP_TYPES + flagconstraint + folderconstraint);
            if (getTotalFolderSize(folders) > RESULTS_STREAMING_MIN_ROWS)
                Db.getInstance().enableStreaming(stmt);
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            if (flagsets != null) {
                for (long flags : flagsets)
                    stmt.setInt(pos++, (int) flags);
            }
            if (folders != null) {
                for (Folder folder : folders)
                    stmt.setInt(pos++, folder.getId());
            }
            rs = stmt.executeQuery();

            info.rootId = 0;
            info.size   = 0;
            List<Integer> versionedIds = accumulateLeafNodes(info, mbox, rs);
            rs.close(); rs = null;
            stmt.close(); stmt = null;
            accumulateLeafRevisions(info, mbox, versionedIds);
            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of \\Deleted items for purge", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
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
    private static List<Integer> accumulateLeafNodes(PendingDelete info, Mailbox mbox, ResultSet rs) throws SQLException, ServiceException {
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
            byte type = rs.getByte(LEAF_CI_TYPE);

            Integer item = new Integer(id);
            info.itemIds.add(type, item);
            info.size += size;
            
            if (rs.getBoolean(LEAF_CI_IS_UNREAD))
                info.unreadIds.add(item);

            boolean isMessage = false;
            switch (type) {
                case MailItem.TYPE_CONTACT:  info.contacts++;  break;
                case MailItem.TYPE_CHAT:
                case MailItem.TYPE_MESSAGE:  isMessage = true;    break;
            }

            // record deleted virtual conversations and modified-or-deleted real conversations
            if (isMessage) {
                int parentId = rs.getInt(LEAF_CI_PARENT_ID);
                if (rs.wasNull() || parentId <= 0)
                    info.itemIds.add(MailItem.TYPE_VIRTUAL_CONVERSATION, -id);
                else
                    info.modifiedIds.add(parentId);
            }

            int flags = rs.getInt(LEAF_CI_FLAGS);
            if ((flags & Flag.BITMASK_VERSIONED) != 0)
                versioned.add(id);

            Integer folderId = rs.getInt(LEAF_CI_FOLDER_ID);
            boolean isDeleted = (flags & Flag.BITMASK_DELETED) != 0;
            LocationCount count = info.messages.get(folderId);
            if (count == null)
                info.messages.put(folderId, new LocationCount(1, isDeleted ? 1 : 0, size));
            else
                count.increment(1, isDeleted ? 1 : 0, size);

            String blobDigest = rs.getString(LEAF_CI_BLOB_DIGEST);
            if (blobDigest != null) {
                info.blobDigests.add(blobDigest);
                String locator = rs.getString(LEAF_CI_VOLUME_ID);
                try {
                    MailboxBlob mblob = sm.getMailboxBlob(mbox, id, revision, locator);
                    if (mblob == null)
                        sLog.warn("missing blob for id: " + id + ", change: " + revision);
                    else
                        info.blobs.add(mblob);
                } catch (Exception e1) { }
            }

            String indexId = rs.getString(LEAF_CI_INDEX_ID);
            boolean indexed = !rs.wasNull();
            if (indexed) {
                if (info.sharedIndex == null)
                    info.sharedIndex = new HashSet<String>();
                boolean shared = (flags & Flag.BITMASK_COPIED) != 0;
                if (!shared)  info.indexIds.add(indexId);
                else          info.sharedIndex.add(indexId);
            }
        }
        return versioned;
    }

    private static void accumulateLeafRevisions(PendingDelete info, Mailbox mbox, List<Integer> versioned) throws ServiceException {
        if (versioned == null || versioned.size() == 0) {
            return;
        }
        Connection conn = mbox.getOperationConnection();
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
                LocationCount count = info.messages.get(folderId);
                if (count == null)
                    info.messages.put(folderId, new LocationCount(0, 0, rs.getLong(3)));
                else
                    count.increment(0, 0, rs.getLong(3));

                String blobDigest = rs.getString(6);
                if (blobDigest != null) {
                    info.blobDigests.add(blobDigest);
                    try {
                        MailboxBlob mblob = sm.getMailboxBlob(mbox, rs.getInt(1), rs.getInt(4), rs.getString(5));
                        if (mblob == null)
                            sLog.error("missing blob for id: " + rs.getInt(1) + ", change: " + rs.getInt(4));
                        else
                            info.blobs.add(mblob);
                    } catch (Exception e1) { }
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting version deletion info for items: " + versioned, e);
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
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        List<String> indexIDs = new ArrayList<String>(info.sharedIndex);

        Connection conn = mbox.getOperationConnection();
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
                    stmt.setString(pos++, indexIDs.get(index));
                rs = stmt.executeQuery();
                while (rs.next())
                    info.sharedIndex.remove(rs.getString(1));
                rs.close(); rs = null;
                stmt.close(); stmt = null;
            }

            info.indexIds.addAll(info.sharedIndex);
            info.sharedIndex.clear();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("resolving shared index entries: " + info.rootId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }


    private static final String IMAP_FIELDS = "mi.id, mi.type, mi.imap_id, mi.unread, mi.flags, mi.tags";
    private static final String IMAP_TYPES = "(" + MailItem.TYPE_MESSAGE + "," + MailItem.TYPE_CHAT + ',' + MailItem.TYPE_CONTACT + ")";

    public static List<ImapMessage> loadImapFolder(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        List<ImapMessage> result = new ArrayList<ImapMessage>();

        Connection conn = mbox.getOperationConnection();
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
                result.add(new ImapMessage(rs.getInt(1), rs.getByte(2), rs.getInt(3), flags, rs.getLong(6)));
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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


    private static final String POP3_FIELDS = "mi.id, mi.size, mi.blob_digest";
    private static final String POP3_TYPES = "(" + MailItem.TYPE_MESSAGE + ")";

    public static List<Pop3Message> loadPop3Folder(Folder folder, Date popSince) throws ServiceException {
        Mailbox mbox = folder.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        long popDate = popSince == null ? -1 : Math.max(popSince.getTime(), -1);
        List<Pop3Message> result = new ArrayList<Pop3Message>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String dateConstraint = popDate < 0 ? "" : " AND date > ?";
            stmt = conn.prepareStatement("SELECT " + POP3_FIELDS +
                        " FROM " + getMailItemTableName(mbox, " mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + POP3_TYPES +
                        " AND NOT " + Db.bitmaskAND("flags", Flag.BITMASK_DELETED) + dateConstraint);
            if (folder.getSize() > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, folder.getId());
            if (popDate >= 0)
                stmt.setInt(pos++, (int) (popDate / 1000L));
            rs = stmt.executeQuery();

            while (rs.next())
                result.add(new Pop3Message(rs.getInt(1), rs.getLong(2), rs.getString(3)));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("loading POP3 folder data: " + folder.getPath(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getRevisionInfo(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        List<UnderlyingData> dlist = new ArrayList<UnderlyingData>();
        if (!item.isTagged(Flag.ID_FLAG_VERSIONED))
            return dlist;

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + REVISION_FIELDS + " FROM " + getRevisionTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "item_id = ?" +
                        " ORDER BY version");
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            rs = stmt.executeQuery();

            while (rs.next())
                dlist.add(constructRevision(rs, item));
            return dlist;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting old revisions for item: " + item.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<Integer> listByFolder(Folder folder, byte type, boolean descending) throws ServiceException {
        Mailbox mbox = folder.getMailbox();

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        boolean allTypes = type == MailItem.TYPE_UNKNOWN;
        List<Integer> result = new ArrayList<Integer>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String typeConstraint = allTypes ? "" : "type = ? AND ";
            stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + typeConstraint + "folder_id = ?" +
                        " ORDER BY date" + (descending ? " DESC" : ""));
            if (type == MailItem.TYPE_MESSAGE && folder.getSize() > RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            if (!allTypes)
                stmt.setByte(pos++, type);
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
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        TypedIdList result = new TypedIdList();

        Connection conn = mbox.getOperationConnection();
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

            while (rs.next())
                result.add(rs.getByte(2), rs.getInt(1));
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
//  public static final int CI_SENDER      = 14;
    public static final int CI_SUBJECT     = 14;
    public static final int CI_NAME        = 15;
    public static final int CI_METADATA    = 16;
    public static final int CI_MODIFIED    = 17;
    public static final int CI_MODIFY_DATE = 18;
    public static final int CI_SAVED       = 19;

    static final String DB_FIELDS = "mi.id, mi.type, mi.parent_id, mi.folder_id, mi.index_id, " +
                                    "mi.imap_id, mi.date, mi.size, mi.volume_id, mi.blob_digest, " +
                                    "mi.unread, mi.flags, mi.tags, mi.subject, mi.name, " +
                                    "mi.metadata, mi.mod_metadata, mi.change_date, mi.mod_content";
    

    private static UnderlyingData constructItem(ResultSet rs) throws SQLException {
        return constructItem(rs, 0);
    }

    static UnderlyingData constructItem(ResultSet rs, int offset) throws SQLException {
        UnderlyingData data = new UnderlyingData();
        data.id          = rs.getInt(CI_ID + offset);
        data.type        = rs.getByte(CI_TYPE + offset);
        data.parentId    = rs.getInt(CI_PARENT_ID + offset);
        data.folderId    = rs.getInt(CI_FOLDER_ID + offset);
        data.indexId     = rs.getString(CI_INDEX_ID + offset);
        if (rs.wasNull())
            data.indexId = null;
        data.imapId      = rs.getInt(CI_IMAP_ID + offset);
        if (rs.wasNull())
            data.imapId = -1;
        data.date        = rs.getInt(CI_DATE + offset);
        data.size        = rs.getLong(CI_SIZE + offset);
        data.locator    = rs.getString(CI_VOLUME_ID + offset);
        data.setBlobDigest(rs.getString(CI_BLOB_DIGEST + offset));
        data.unreadCount = rs.getInt(CI_UNREAD + offset);
        data.flags       = rs.getInt(CI_FLAGS + offset);
        data.tags        = rs.getLong(CI_TAGS + offset);
        data.subject     = rs.getString(CI_SUBJECT + offset);
        data.name        = rs.getString(CI_NAME + offset);
        data.metadata    = rs.getString(CI_METADATA + offset);
        data.modMetadata = rs.getInt(CI_MODIFIED + offset);
        data.modContent  = rs.getInt(CI_SAVED + offset);
        data.dateChanged = rs.getInt(CI_MODIFY_DATE + offset);
        // make sure to handle NULL column values
        if (data.parentId == 0)     data.parentId = -1;
        if (data.dateChanged == 0)  data.dateChanged = -1;
        return data;
    }

    private static final String REVISION_FIELDS = "date, size, volume_id, blob_digest, name, " +
                                                  "metadata, mod_metadata, change_date, mod_content";

    private static UnderlyingData constructRevision(ResultSet rs, MailItem item) throws SQLException {
        UnderlyingData data = new UnderlyingData();
        data.id          = item.getId();
        data.type        = item.getType();
        data.parentId    = item.getParentId();
        data.folderId    = item.getFolderId();
        data.indexId     = null;
        data.imapId      = -1;
        data.date        = rs.getInt(1);
        data.size        = rs.getLong(2);
        data.locator    = rs.getString(3);
        data.setBlobDigest(rs.getString(4));
        data.unreadCount = item.getUnreadCount();
        data.flags       = item.getInternalFlagBitmask() | Flag.BITMASK_UNCACHED;
        data.tags        = item.getTagBitmask();
        data.subject     = item.getSubject();
        data.name        = rs.getString(5);
        data.metadata    = rs.getString(6);
        data.modMetadata = rs.getInt(7);
        data.dateChanged = rs.getInt(8);
        data.modContent  = rs.getInt(9);
        // make sure to handle NULL column values
        if (data.parentId <= 0)     data.parentId = -1;
        if (data.dateChanged == 0)  data.dateChanged = -1;
        return data;
    }

    //////////////////////////////////////
    // CALENDAR STUFF BELOW HERE!
    //////////////////////////////////////

    public static UnderlyingData getCalendarItem(Mailbox mbox, String uid) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
    public static List<UnderlyingData> getCalendarItems(Mailbox mbox, byte type, long start, long end, int folderId, int[] excludeFolderIds) 
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<UnderlyingData> result = new ArrayList<UnderlyingData>();
        try {
            for (int i = 0; i < uids.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), uids.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET index_id = id" +
                            " WHERE " + IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", count));
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
    
    public static TypedIdList listCalendarItems(Mailbox mbox, byte type, long start, long end, int folderId, int[] excludeFolderIds) 
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = calendarItemStatement(conn, "mi.id, mi.type", mbox, type, start, end, folderId, excludeFolderIds);
            rs = stmt.executeQuery();

            TypedIdList result = new TypedIdList();
            while (rs.next())
                result.add(rs.getByte(2), rs.getInt(1));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("listing calendar items for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static PreparedStatement calendarItemStatement(Connection conn, String fields,
            Mailbox mbox, byte type, long start, long end, int folderId, int[] excludeFolderIds)
    throws SQLException {
        boolean folderSpecified = folderId != Mailbox.ID_AUTO_INCREMENT;

        String endConstraint = end > 0 ? " AND ci.start_time < ?" : "";
        String startConstraint = start > 0 ? " AND ci.end_time > ?" : "";
        String typeConstraint = type == MailItem.TYPE_UNKNOWN ? "type IN " + CALENDAR_TYPES : typeIn(type);

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

    public static List<Integer> getItemListByDates(Mailbox mbox, byte type, long start, long end, int folderId, boolean descending) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        boolean allTypes = type == MailItem.TYPE_UNKNOWN;
        List<Integer> result = new ArrayList<Integer>();

        Connection conn = mbox.getOperationConnection();
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
            if (!allTypes)
                stmt.setByte(pos++, type);
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
        private SortedSet<Integer> mFolderIds = new TreeSet<Integer>();
        private Long mModifiedBefore;
        private Integer mRowLimit;
        private SortedSet<Byte> mIncludedTypes = new TreeSet<Byte>();
        private SortedSet<Byte> mExcludedTypes = new TreeSet<Byte>();

        public SortedSet<Integer> getFolderIds() { return Collections.unmodifiableSortedSet(mFolderIds); }
        public QueryParams setFolderIds(Collection<Integer> ids) {
            mFolderIds.clear();
            if (ids != null) {
                mFolderIds.addAll(ids);
            }
            return this;
        }
        
        public SortedSet<Byte> getIncludedTypes() { return Collections.unmodifiableSortedSet(mIncludedTypes); }
        public QueryParams setIncludedTypes(byte ... types) {
            mIncludedTypes.clear();
            if (types != null) {
                for (byte type : types) {
                    mIncludedTypes.add(type);
                }
            }
            return this;
        }
        
        public SortedSet<Byte> getExcludedTypes() { return Collections.unmodifiableSortedSet(mExcludedTypes); }
        public QueryParams setExcludedTypes(byte ... types) {
            mExcludedTypes.clear();
            if (types != null) {
                for (byte type : types) {
                    mExcludedTypes.add(type);
                }
            }
            return this;
        }

        /**
         * @return the timestamp, in milliseconds
         */
        public Long getModifiedBefore() { return mModifiedBefore; }
        /**
         * Return items modified earlier than the given timestamp.
         * @param timestamp the timestamp, in milliseconds
         */
        public QueryParams setModifiedBefore(Long timestamp) { mModifiedBefore = timestamp; return this; }
        
        public Integer getRowLimit() { return mRowLimit; }
        public QueryParams setRowLimit(Integer rowLimit) { mRowLimit = rowLimit; return this; }
    }

    /**
     * Returns the ids of items that match the given query parameters.
     * @return the matching ids, or an empty <tt>Set</tt>
     */
    public static Set<Integer> getIds(Mailbox mbox, Connection conn, QueryParams params)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Set<Integer> ids = new HashSet<Integer>();
        
        try {
            // Prepare the statement based on query parameters.
            StringBuilder buf = new StringBuilder();
            buf.append("SELECT id FROM " + getMailItemTableName(mbox) + " WHERE " + IN_THIS_MAILBOX_AND + "1 = 1");
            
            Set<Byte> includedTypes = params.getIncludedTypes();
            Set<Byte> excludedTypes = params.getExcludedTypes();
            Set<Integer> folderIds = params.getFolderIds();
            Long modifiedBefore = params.getModifiedBefore();
            Integer rowLimit = params.getRowLimit();
            
            if (!includedTypes.isEmpty()) {
                buf.append(" AND ").append(DbUtil.whereIn("type", includedTypes.size()));
            }
            if (!excludedTypes.isEmpty()) {
                buf.append(" AND ").append(DbUtil.whereNotIn("type", excludedTypes.size()));
            }
            if (!folderIds.isEmpty()) {
                buf.append(" AND ").append(DbUtil.whereIn("folder_id", folderIds.size()));
            }
            if (modifiedBefore != null) {
                buf.append(" AND ").append("mod_content < ?");
            }
            if (rowLimit != null && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                buf.append(" LIMIT ").append(rowLimit);
            }
            stmt = conn.prepareStatement(buf.toString());
            
            // Bind values, execute query, return results.
            int pos = 1;
            pos = setMailboxId(stmt, mbox, pos);
            for (byte type : includedTypes) {
                stmt.setByte(pos++, type);
            }
            for (byte type : excludedTypes) {
                stmt.setByte(pos++, type);
            }
            for (int id : folderIds) {
                stmt.setInt(pos++, id);
            }
            if (modifiedBefore != null) {
                stmt.setInt(pos++, (int) (modifiedBefore / 1000));
            }

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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        long end = calItem.getEndTime();
        Timestamp startTs = new Timestamp(calItem.getStartTime());
        Timestamp endTs = new Timestamp(end <= 0 ? MAX_DATE : end);

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        long end = calItem.getEndTime();
        Timestamp startTs = new Timestamp(calItem.getStartTime());
        Timestamp endTs = new Timestamp(end <= 0 ? MAX_DATE : end);

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        ArrayList<CalendarItem.CalendarMetadata> result = new ArrayList<CalendarItem.CalendarMetadata>();

        Connection conn = mbox.getOperationConnection();
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

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = mbox.getOperationConnection();
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

            if (!rs.next())
                throw ServiceException.FAILURE("consistency check failed: " + MailItem.getNameForType(item) + " " + item.getId() + " not found in DB", null);

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
            if (data.locator != dbdata.locator)        failures += " VOLUME_ID";
            if (data.date != dbdata.date)                failures += " DATE";
            if (data.size != dbdata.size)                failures += " SIZE";
            if (dbdata.type != MailItem.TYPE_CONVERSATION) {
                if (data.unreadCount != dbdata.unreadCount)  failures += " UNREAD";
                if (data.flags != dbdata.flags)              failures += " FLAGS";
                if (data.tags != dbdata.tags)                failures += " TAGS";
            }
            if (data.modMetadata != dbdata.modMetadata)  failures += " MOD_METADATA";
            if (data.dateChanged != dbdata.dateChanged)  failures += " CHANGE_DATE";
            if (data.modContent != dbdata.modContent)    failures += " MOD_CONTENT";
            if (Math.max(data.parentId, -1) != dbdata.parentId)  failures += " PARENT_ID";
            if (dataBlobDigest != dbdataBlobDigest && (dataBlobDigest == null || !dataBlobDigest.equals(dbdataBlobDigest)))  failures += " BLOB_DIGEST";
            if (dataSender != dbdataSender && (dataSender == null || !dataSender.equalsIgnoreCase(dbdataSender)))  failures += " SENDER";
            if (data.subject != dbdata.subject && (data.subject == null || !data.subject.equals(dbdata.subject)))  failures += " SUBJECT";
            if (data.name != dbdata.name && (data.name == null || !data.name.equals(dbdata.name)))                 failures += " NAME";
            if (metadata != dbdata.metadata && (metadata == null || !metadata.equals(dbdata.metadata)))            failures += " METADATA";

            if (item instanceof Folder && dbdata.folderId != dbdata.parentId)  failures += " FOLDER!=PARENT";

            if (!failures.equals(""))
                throw ServiceException.FAILURE("consistency check failed: " + MailItem.getNameForType(item) + " " + item.getId() + " differs from DB at" + failures, null);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching item " + item.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /** Makes sure that the argument won't overflow the maximum length of a
     *  MySQL VARCHAR(128) column (128 characters) by truncating the string
     *  if necessary.
     * 
     * @param sender  The string to check (can be null).
     * @return The passed-in String, truncated to 128 chars. */
    public static String checkSenderLength(String sender) {
        if (sender == null || sender.length() <= MAX_SENDER_LENGTH)
            return sender;
        return sender.substring(0, MAX_SENDER_LENGTH);
    }

    /** Makes sure that the argument won't overflow the maximum length of a
     *  MySQL VARCHAR(1024) column (1024 characters).
     * 
     * @param subject  The string to check (can be null).
     * @return The passed-in String.
     * @throws ServiceException <code>service.FAILURE</code> if the
     *         parameter would be silently truncated when inserted. */
    public static String checkSubjectLength(String subject) throws ServiceException {
        if (subject == null || subject.length() <= MAX_SUBJECT_LENGTH)
            return subject;
        throw ServiceException.FAILURE("subject too long", null);
    }

    /** Makes sure that the argument won't overflow the maximum length of a
     *  MySQL MEDIUMTEXT column (16,777,216 bytes) after conversion to UTF-8.
     * 
     * @param metadata  The string to check (can be null).
     * @return The passed-in String.
     * @throws ServiceException <code>service.FAILURE</code> if the
     *         parameter would be silently truncated when inserted. */
    public static String checkMetadataLength(String metadata) throws ServiceException {
        if (metadata == null)
            return null;
        int len = metadata.length();
        if (len > MAX_MEDIUMTEXT_LENGTH / 4) {  // every char uses 4 bytes in worst case
            if (StringUtil.isAsciiString(metadata)) {
                if (len > MAX_MEDIUMTEXT_LENGTH)
                    throw ServiceException.FAILURE("metadata too long", null);
            } else {
                try {
                    if (metadata.getBytes("utf-8").length > MAX_MEDIUMTEXT_LENGTH)
                        throw ServiceException.FAILURE("metadata too long", null);
                } catch (UnsupportedEncodingException uee) { }
            }
        }
        return metadata;
    }

    /**
     * Returns the name of the table that stores {@link MailItem} data.  The table name is qualified
     * by the name of the database (e.g. <tt>mailbox1.mail_item</tt>).
     */
    public static String getMailItemTableName(long mailboxId, long groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_MAIL_ITEM);
    }
    public static String getMailItemTableName(MailItem item) {
        return DbMailbox.qualifyTableName(item.getMailbox(), TABLE_MAIL_ITEM);
    }
    public static String getMailItemTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_MAIL_ITEM);
    }
    public static String getMailItemTableName(Mailbox mbox, String alias) {
        return getMailItemTableName(mbox) + " AS " + alias;
    }

    /**
     * Returns the name of the table that stores data on old revisions of {@link MailItem}s.
     * The table name is qualified by the name of the database (e.g. <tt>mailbox1.mail_item</tt>).
     */
    public static String getRevisionTableName(long mailboxId, long groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_REVISION);
    }
    public static String getRevisionTableName(MailItem item) {
        return DbMailbox.qualifyTableName(item.getMailbox(), TABLE_REVISION);
    }
    public static String getRevisionTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_REVISION);
    }
    public static String getRevisionTableName(Mailbox mbox, String alias) {
        return getRevisionTableName(mbox) + " AS " + alias;
    }

    /**
     * Returns the name of the table that stores {@link CalendarItem} data.  The table name is qualified
     * by the name of the database (e.g. <tt>mailbox1.appointment</tt>).
     */
    public static String getCalendarItemTableName(long mailboxId, long groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_APPOINTMENT);
    }
    public static String getCalendarItemTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_APPOINTMENT);
    }
    public static String getCalendarItemTableName(Mailbox mbox, String alias) {
        return getCalendarItemTableName(mbox) + " AS " + alias;
    }

    /**
     * Returns the name of the table that maps subject hashes to {@link Conversation} ids.  The table 
     * name is qualified by the name of the database (e.g. <tt>mailbox1.open_conversation</tt>).
     */
    public static String getConversationTableName(long mailboxId, long groupId) {
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
    public static String getTombstoneTableName(long mailboxId, long groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_TOMBSTONE);
    }
    public static String getTombstoneTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_TOMBSTONE);
    }


    /** If the database doesn't support row-level locking, try to synchronize
     *  database accesses on the Mailbox object to avoid having read/write
     *  conflicts with Mailbox write transactions.  In the case where the DB
     *  <u>does</u> support row-level locking, synchronizing on a local
     *  <code>Object</code> shouldn't add problematic overhead. */
    public static Object getSynchronizer(Mailbox mbox) {
        return Db.supports(Db.Capability.ROW_LEVEL_LOCKING) ? new Object() : mbox;
    }


    private static boolean areTagsetsLoaded(Mailbox mbox) {
        synchronized (sTagsetCache) {
            return sTagsetCache.containsKey(mbox.getId());
        }
    }

    static TagsetCache getTagsetCache(Connection conn, Mailbox mbox) throws ServiceException {
        long mailboxId = mbox.getId();
        Long id = new Long(mailboxId);
        TagsetCache tagsets = null;

        synchronized (sTagsetCache) {
            tagsets = sTagsetCache.get(id);
        }

        // All access to a mailbox is synchronized, so we can initialize
        // the tagset cache for a single mailbox outside the
        // synchronized block.
        if (tagsets == null) {
            ZimbraLog.cache.info("Loading tagset cache");
            tagsets = new TagsetCache("Mailbox " + mailboxId + " tags");
            tagsets.addTagsets(DbMailbox.getDistinctTagsets(conn, mbox));

            synchronized (sTagsetCache) {
                sTagsetCache.put(id, tagsets);
            }
        }

        return tagsets;
    }

    private static boolean areFlagsetsLoaded(Mailbox mbox) {
        synchronized(sFlagsetCache) {
            return sFlagsetCache.containsKey(mbox.getId());
        }
    }

    static TagsetCache getFlagsetCache(Connection conn, Mailbox mbox) throws ServiceException {
        long mailboxId = mbox.getId();
        Long id = new Long(mailboxId);
        TagsetCache flagsets = null;

        synchronized (sFlagsetCache) {
            flagsets = sFlagsetCache.get(id);
        }

        // All access to a mailbox is synchronized, so we can initialize
        // the flagset cache for a single mailbox outside the
        // synchronized block.
        if (flagsets == null) {
            ZimbraLog.cache.info("Loading flagset cache");
            flagsets = new TagsetCache("Mailbox " + mailboxId + " flags");
            flagsets.addTagsets(DbMailbox.getDistinctFlagsets(conn, mbox));

            synchronized (sFlagsetCache) {
                sFlagsetCache.put(id, flagsets);
            }
        }

        return flagsets;
    }
    
    /**
     * Returns a comma-separated list of ids for logging.  If the <tt>String</tt> is
     * more than 200 characters long, cuts off the list and appends &quot...&quot.
     */
    private static String getIdListForLogging(Collection<Integer> ids) {
        if (ids == null)
            return null;
        StringBuilder idList = new StringBuilder();
        boolean firstTime = true;
        for (Integer id : ids) {
            if (firstTime)
                firstTime = false;
            else
                idList.append(',');
            idList.append(id);
            if (idList.length() > 200) {
                idList.append("...");
                break;
            }
        }
        return idList.toString();
    }
}
