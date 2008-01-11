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
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbSearch.SearchResult;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.MailItem.PendingDelete;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.pop3.Pop3Message;
import com.zimbra.cs.store.StoreManager;


/**
 * @author dkarp
 */
public class DbMailItem {

    public static final String TABLE_MAIL_ITEM = "mail_item";
    public static final String TABLE_REVISION = "revision";
    public static final String TABLE_APPOINTMENT = "appointment";
    public static final String TABLE_OPEN_CONVERSATION = "open_conversation";
    public static final String TABLE_TOMBSTONE = "tombstone";

    private static Log sLog = LogFactory.getLog(DbMailItem.class);

    /** Maps the mailbox id to the set of all tag combinations stored for all
     *  items in the mailbox.  Enables fast database lookup by tag. */
    private static final Map<Integer, TagsetCache> sTagsetCache =
        new TimeoutMap<Integer, TagsetCache>(120 * Constants.MILLIS_PER_MINUTE);

    /** Maps the mailbox id to the set of all flag combinations stored for all
     *  items in the mailbox.  Enables fast database lookup by flag. */
    private static final Map<Integer, TagsetCache> sFlagsetCache =
        new TimeoutMap<Integer, TagsetCache>(120 * Constants.MILLIS_PER_MINUTE);

    public static final int MAX_SENDER_LENGTH  = 128;
    public static final int MAX_SUBJECT_LENGTH = 1024;
    public static final int MAX_TEXT_LENGTH    = 65534;
    public static final int MAX_MEDIUMTEXT_LENGTH = 16777216;

    public static final String IN_THIS_MAILBOX_AND = "mailbox_id = ? AND ";
    
    public static final String getInThisMailboxAnd(int mboxId, String miAlias, String apAlias) {
        StringBuilder sb = new StringBuilder(miAlias).append(".mailbox_id = ").append(mboxId).append(" AND ");
        if (apAlias != null) 
            sb.append(apAlias).append(".mailbox_id = ").append(mboxId).append(" AND ");
        return sb.toString();
    }

    public static void create(Mailbox mbox, UnderlyingData data) throws ServiceException {
        if (data == null || data.id <= 0 || data.folderId <= 0 || data.parentId == 0 || data.indexId == 0)
            throw ServiceException.FAILURE("invalid data for DB item create", null);

        checkNamingConstraint(mbox, data.folderId, data.name, data.id);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getMailItemTableName(mbox) +
                        "(" + "mailbox_id, " +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tags, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "VALUES (" + "?," +
                        " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, data.id);
            stmt.setByte(pos++, data.type);
            if (data.parentId <= 0)
                // Messages in virtual conversations are stored with a null parent_id
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, data.parentId);
            stmt.setInt(pos++, data.folderId);
            if (data.indexId <= 0)
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, data.indexId);
            if (data.imapId <= 0)
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, data.imapId);
            stmt.setInt(pos++, data.date);
            stmt.setLong(pos++, data.size);
            if (data.volumeId >= 0)
                stmt.setShort(pos++, data.volumeId);
            else
                stmt.setNull(pos++, Types.TINYINT);
            stmt.setString(pos++, data.getBlobDigest());
            if (data.type == MailItem.TYPE_MESSAGE || data.type == MailItem.TYPE_CHAT || data.type == MailItem.TYPE_FOLDER)
                stmt.setInt(pos++, data.unreadCount);
            else
                stmt.setNull(pos++, java.sql.Types.BOOLEAN);
            stmt.setInt(pos++, data.flags);
            stmt.setLong(pos++, data.tags);
            stmt.setString(pos++, checkSenderLength(data.sender));
            stmt.setString(pos++, checkSubjectLength(data.subject));
            stmt.setString(pos++, data.name);
            stmt.setString(pos++, checkMetadataLength(data.metadata));
            stmt.setInt(pos++, data.modMetadata);
            stmt.setInt(pos++, data.dateChanged);
            stmt.setInt(pos++, data.modContent);
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to create object", null);

            // Track the tags and flags for fast lookup later
            if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox);
                tagsets.addTagset(data.tags);
            }
            if (areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox);
                flagsets.addTagset(data.flags);
            }
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
            stmt.setInt(pos++, mbox.getId());
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

    public static void copy(MailItem item, int id, Folder folder, int indexId, int parentId, short volumeId, String metadata)
    throws ServiceException {
        Mailbox mbox = item.getMailbox();
        if (id <= 0 || indexId <= 0 || folder == null || parentId == 0)
            throw ServiceException.FAILURE("invalid data for DB item copy", null);

        checkNamingConstraint(mbox, folder.getId(), item.getName(), id);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String table = getMailItemTableName(mbox);
            stmt = conn.prepareStatement("INSERT INTO " + table +
                        "(" + "mailbox_id, " +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tags, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "(SELECT " + "?, " +
                        " ?, type, ?, ?, ?, ?, date, size, ?, blob_digest, unread," +
                        " flags, tags, sender, subject, name, ?, ?, ?, ? FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?)");
            int mboxId = mbox.getId();
            int pos = 1;
            stmt.setInt(pos++, mboxId);
            stmt.setInt(pos++, id);                            // ID
            if (parentId <= 0)
                stmt.setNull(pos++, Types.INTEGER);            // PARENT_ID null for messages in virtual convs
            else
                stmt.setInt(pos++, parentId);                  //   or, PARENT_ID specified by caller
            stmt.setInt(pos++, folder.getId());                // FOLDER_ID
            stmt.setInt(pos++, indexId);                       // INDEX_ID
            stmt.setInt(pos++, id);                            // IMAP_ID is initially the same as ID
            if (volumeId >= 0)
                stmt.setShort(pos++, volumeId);                // VOLUME_ID specified by caller
            else
                stmt.setNull(pos++, Types.TINYINT);            //   or, no VOLUME_ID
            stmt.setString(pos++, checkMetadataLength(metadata));  // METADATA
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_METADATA
            stmt.setInt(pos++, mbox.getOperationTimestamp());  // CHANGE_DATE
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_CONTENT
            stmt.setInt(pos++, mboxId);
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
        if (data == null || data.id <= 0 || data.folderId <= 0 || data.parentId == 0 || data.indexId == 0)
            throw ServiceException.FAILURE("invalid data for DB item i-copy", null);

        checkNamingConstraint(mbox, data.folderId, source.getName(), data.id);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String table = getMailItemTableName(mbox);
            String flags = shared ? "flags | " + Flag.BITMASK_COPIED : "flags";
            stmt = conn.prepareStatement("INSERT INTO " + table +
                        "(" + "mailbox_id, " +
                        " id, type, parent_id, folder_id, index_id, imap_id, date, size, volume_id, blob_digest," +
                        " unread, flags, tags, sender, subject, name, metadata, mod_metadata, change_date, mod_content) " +
                        "(SELECT " + "?, " +
                        " ?, type, parent_id, ?, ?, ?, date, size, ?, blob_digest," +
                        " unread, " + flags + ", tags, sender, subject, name, metadata, ?, ?, ? FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?)");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, data.id);                       // ID
            stmt.setInt(pos++, data.folderId);                 // FOLDER_ID
            stmt.setInt(pos++, data.indexId);                  // INDEX_ID
            stmt.setInt(pos++, data.imapId);                   // IMAP_ID
            if (data.volumeId >= 0)
                stmt.setShort(pos++, data.volumeId);           // VOLUME_ID
            else
                stmt.setNull(pos++, Types.TINYINT);            //   or, no VOLUME_ID
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_METADATA
            stmt.setInt(pos++, mbox.getOperationTimestamp());  // CHANGE_DATE
            stmt.setInt(pos++, mbox.getOperationChangeID());   // MOD_CONTENT
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, source.getId());
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to create object", null);
            stmt.close();

            boolean needsTag = !source.isTagged(mbox.mCopiedFlag);

            if (needsTag)
                getFlagsetCache(conn, mbox).addTagset(source.getInternalFlagBitmask() | Flag.BITMASK_COPIED);

            if (needsTag || source.getParentId() > 0) {
                stmt = conn.prepareStatement("UPDATE " + table +
                            " SET parent_id = NULL, flags = " + flags + ", mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
                pos = 1;
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                stmt.setInt(pos++, mbox.getId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getRevisionTableName(mbox) +
                        "(mailbox_id, item_id, version, date, size, volume_id, blob_digest," +
                        " name, metadata, mod_metadata, change_date, mod_content) " +
                        "(SELECT mailbox_id, id, ?, date, size, volume_id, blob_digest," +
                        " name, metadata, mod_metadata, change_date, mod_content" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?)");
            int mboxId = mbox.getId();
            int pos = 1;
            stmt.setInt(pos++, version);
            stmt.setInt(pos++, mboxId);
            stmt.setInt(pos++, item.getId());
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to copy revision data", null);
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

    public static void purgeRevisions(MailItem item, int highestPurged) throws ServiceException {
        if (highestPurged <= 0)
            return;
        Mailbox mbox = item.getMailbox();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getRevisionTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "item_id = ? AND version <= ?");
            int mboxId = mbox.getId();
            int pos = 1;
            stmt.setInt(pos++, mboxId);
            stmt.setInt(pos++, item.getId());
            stmt.setInt(pos++, highestPurged);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("purging revisions for " + MailItem.getNameForType(item) + ": " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void changeType(MailItem item, byte type) throws ServiceException {
        Connection conn = item.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET type = ? WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, type);
            stmt.setInt(pos++, item.getMailboxId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String imapRenumber = mbox.isTrackingImap() ? ", imap_id = CASE WHEN imap_id IS NULL THEN NULL ELSE 0 END" : "";
            int pos = 1;
            boolean hasIndexId = false;
            if (item instanceof Folder) {
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                            " SET parent_id = ?, folder_id = ?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = ? AND folder_id != ?");
                stmt.setInt(pos++, folder.getId());
            } else if (item instanceof Conversation && !(item instanceof VirtualConversation)) {
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                            " SET folder_id = ?, mod_metadata = ?, change_date = ?" + imapRenumber +
                            " WHERE " + IN_THIS_MAILBOX_AND + "parent_id = ? AND folder_id != ?");
            } else {
                // set the indexId, in case it changed (moving items out of junk can trigger an index ID change)
                hasIndexId = true;
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                            " SET folder_id = ?, index_id = ?, mod_metadata = ?, change_date = ? " + imapRenumber +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id = ? AND folder_id != ?");
            }
            stmt.setInt(pos++, folder.getId());
            if (hasIndexId)
                if (item.getIndexId() <= 0)
                    stmt.setNull(pos++, Types.INTEGER);
                else
                    stmt.setInt(pos++, item.getIndexId());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, item instanceof VirtualConversation ? ((VirtualConversation) item).getMessageId() : item.getId());
            stmt.setInt(pos++, folder.getId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
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
                            " WHERE " + IN_THIS_MAILBOX_AND + "id IN " + DbUtil.suitableNumberOfVariables(count));
                int pos = 1;
                stmt.setInt(pos++, folder.getId());
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                stmt.setInt(pos++, mbox.getId());
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, msgs.get(index).getId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
//            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
//                throw MailServiceException.ALREADY_EXISTS(msgs.toString(), e);
//            else
            throw ServiceException.FAILURE("writing new folder data for messages", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
    
    public static void setIndexIds(Mailbox mbox, List<Message> msgs) throws ServiceException {
        if (msgs == null || msgs.isEmpty())
            return;
        
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            for (int i = 0; i < msgs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), msgs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET index_id = id  WHERE " + IN_THIS_MAILBOX_AND + "id IN " + DbUtil.suitableNumberOfVariables(count));
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, msgs.get(index).getId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
//            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW))
//                throw MailServiceException.ALREADY_EXISTS(msgs.toString(), e);
//            else
            throw ServiceException.FAILURE("writing new folder data for messages", e);
        } finally {
            DbPool.closeResults(rs);
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            for (int i = 0; i < children.length; i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), children.length - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET parent_id = ?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id IN " + DbUtil.suitableNumberOfVariables(count));
                int pos = 1;
                if (parent == null || parent instanceof VirtualConversation)
                    stmt.setNull(pos++, Types.INTEGER);
                else
                    stmt.setInt(pos++, parent.getId());
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                stmt.setInt(pos++, mbox.getId());
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, children[index].getId());
                stmt.executeUpdate();
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
            stmt.setInt(pos++, mbox.getId());
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
            stmt.setInt(pos++, mbox.getId());
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
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, item.getSavedSequence());
            stmt.setInt(pos++, mbox.getId());
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
            stmt.setInt(pos++, mbox.getId());
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

        checkNamingConstraint(mbox, folderId, name, item.getId());

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET date = ?, size = ?, flags = ?, name = ?, subject = ?, folder_id = ?," +
                        " metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setLong(pos++, item.getSize());
            stmt.setInt(pos++, item.getInternalFlagBitmask());
            stmt.setString(pos++, name);
            stmt.setString(pos++, name);
            stmt.setInt(pos++, folderId);
            stmt.setString(pos++, metadata);
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getId());
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

    public static void saveData(MailItem item, String sender, String metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        String name = item.getName().equals("") ? null : item.getName();

        String subject = item.getSubject();
        if (item instanceof Conversation)
            subject = ((Conversation) item).getNormalizedSubject();
        else if (item instanceof Message)
            subject = ((Message) item).getNormalizedSubject();

        checkNamingConstraint(mbox, item.getFolderId(), name, item.getId());

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET type = ?, parent_id = ?, date = ?, size = ?, blob_digest = ?, flags = ?," +
                        "  sender = ?, subject = ?, name = ?, metadata = ?," +
                        "  mod_metadata = ?, change_date = ?, mod_content = ?, volume_id = ?" +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setByte(pos++, item.getType());
            if (item.getParentId() <= 0)
                // Messages in virtual conversations are stored with a null parent_id
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, item.getParentId());
            stmt.setInt(pos++, (int) (item.getDate() / 1000));
            stmt.setLong(pos++, item.getSize());
            stmt.setString(pos++, item.getDigest());
            stmt.setInt(pos++, item.getInternalFlagBitmask());
            stmt.setString(pos++, checkSenderLength(sender));
            stmt.setString(pos++, checkSubjectLength(subject));
            stmt.setString(pos++, name);
            stmt.setString(pos++, checkMetadataLength(metadata));
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, item.getSavedSequence());
            short vol = item.getVolumeId();
            if (vol > 0)
                stmt.setShort(pos++, item.getVolumeId());
            else
                stmt.setNull(pos++, Types.TINYINT);
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();

            // Update the flagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox);
                flagsets.addTagset(item.getInternalFlagBitmask());
            }
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

    public static void openConversation(String hash, MailItem item) throws ServiceException {
        Connection conn = item.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getConversationTableName(item) +
                        "(" + ("mailbox_id, ") + "hash, conv_id)" +
                        " VALUES (" + ("?, ") + "?, ?)" +
                        (Db.supports(Db.Capability.ON_DUPLICATE_KEY) ? " ON DUPLICATE KEY UPDATE conv_id = ?" : ""));
            int pos = 1;
            stmt.setInt(pos++, item.getMailboxId());
            stmt.setString(pos++, hash);
            stmt.setInt(pos++, item.getId());
            if (Db.supports(Db.Capability.ON_DUPLICATE_KEY))
                stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (!Db.supports(Db.Capability.ON_DUPLICATE_KEY) && Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                try {
                    stmt.close();

                    stmt = conn.prepareStatement("UPDATE " + getConversationTableName(item) +
                            " SET conv_id = ? WHERE " + IN_THIS_MAILBOX_AND + "hash = ?");
                    int pos = 1;
                    stmt.setInt(pos++, item.getId());
                    stmt.setInt(pos++, item.getMailboxId());
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
        Connection conn = item.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getConversationTableName(item) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "hash = ? AND conv_id = ?");
            int pos = 1;
            stmt.setInt(pos++, item.getMailboxId());
            stmt.setString(pos++, hash);
            stmt.setInt(pos++, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing open conversation association for hash " + hash, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void changeOpenTarget(String hash, MailItem oldTarget, int newTargetId) throws ServiceException {
        Connection conn = oldTarget.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getConversationTableName(oldTarget) +
                        " SET conv_id = ? WHERE " + IN_THIS_MAILBOX_AND + "hash = ? AND conv_id = ?");
            int pos = 1;
            stmt.setInt(pos++, newTargetId);
            stmt.setInt(pos++, oldTarget.getMailboxId());
            stmt.setString(pos++, hash);
            stmt.setInt(pos++, oldTarget.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("switching open conversation association for item " + oldTarget.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveImapUid(MailItem item) throws ServiceException {
        Mailbox mbox = item.getMailbox();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                        " SET imap_id = ?, mod_metadata = ?, change_date = ? WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, item.getImapUid());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String relation, column = (tag instanceof Flag ? "flags" : "tags");
            String operation = (add ? " + " : " - ");
            String precondition = (add ? "NOT " : "") + Db.bitmaskAND(column);

            if (item instanceof VirtualConversation)  relation = "id = ?";
            else if (item instanceof Conversation)    relation = "parent_id = ?";
            else if (item instanceof Folder)          relation = "folder_id = ?";
            else if (item instanceof Flag)            relation = Db.bitmaskAND("flags");
            else if (item instanceof Tag)             relation = Db.bitmaskAND("tags");
            else                                      relation = "id = ?";

            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET " + column + " = " + column + operation + "?, mod_metadata = ?, change_date = ?" +
                    " WHERE " + IN_THIS_MAILBOX_AND + precondition + " AND " + relation);

            int pos = 1;
            stmt.setLong(pos++, tag.getBitmask());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getId());
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
            if (tag instanceof Flag && areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox);
                flagsets.addTagset(item.getInternalFlagBitmask());
            } else if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox);
                tagsets.addTagset(item.getTagBitmask());
            }
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String column = (tag instanceof Flag ? "flags" : "tags");
            String operation = (add ? " + " : " - ");
            String precondition = (add ? "NOT " : "") + Db.bitmaskAND(column);

            for (int i = 0; i < itemIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), itemIDs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(tag) +
                            " SET " + column + " = " + column + operation + "?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + precondition + " AND id IN " + DbUtil.suitableNumberOfVariables(count));
                int pos = 1;
                stmt.setLong(pos++, tag.getBitmask());
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                stmt.setInt(pos++, mbox.getId());
                stmt.setLong(pos++, tag.getBitmask());
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, itemIDs.get(index));
                stmt.executeUpdate();
            }

            // Update the flagset or tagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (tag instanceof Flag && areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox);
                flagsets.applyMask(tag.getBitmask(), add);
            } else if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox);
                tagsets.applyMask(tag.getBitmask(), add);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating tag data for " + itemIDs.size() + " items: " + getIdListForLogging(itemIDs), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void clearTag(Tag tag) throws ServiceException {
        Mailbox mbox = tag.getMailbox();

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
            stmt.setInt(pos++, mbox.getId());
            stmt.setLong(pos++, tag.getBitmask());
            stmt.executeUpdate();

            if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox);
                tagsets.applyMask(tag.getTagBitmask(), false);
            }
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
                        " WHERE " + IN_THIS_MAILBOX_AND + "unread = ? AND " + relation + " AND type IN  " + typeConstraint(MailItem.TYPE_MESSAGE));
            int pos = 1;
            stmt.setInt(pos++, unread ? 1 : 0);
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            for (int i = 0; i < itemIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), itemIDs.size() - i);
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                            " SET unread = ?, mod_metadata = ?, change_date = ?" +
                            " WHERE " + IN_THIS_MAILBOX_AND + "unread = ? AND id IN " + DbUtil.suitableNumberOfVariables(count) +
                            " AND type IN " + typeConstraint(MailItem.TYPE_MESSAGE));
                int pos = 1;
                stmt.setInt(pos++, unread ? 1 : 0);
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                stmt.setInt(pos++, mbox.getId());
                stmt.setInt(pos++, unread ? 0 : 1);
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, itemIDs.get(index));
                stmt.executeUpdate();
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
                stmt.setInt(pos++, mbox.getId());
                stmt.setInt(pos++, folder.getId());
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                stmt.setInt(pos++, mbox.getId());
                stmt.executeUpdate();
                stmt.close();
            } else {
                stmt = conn.prepareStatement("SELECT parent_id, COUNT(*) FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND parent_id IS NOT NULL" +
                        " GROUP BY parent_id");
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
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
                                " WHERE " + IN_THIS_MAILBOX_AND + "id IN " + DbUtil.suitableNumberOfVariables(count) +
                                " AND type = " + MailItem.TYPE_CONVERSATION);
                        pos = 1;
                        stmt.setInt(pos++, update.getKey());
                        stmt.setInt(pos++, mbox.getOperationChangeID());
                        stmt.setInt(pos++, mbox.getOperationTimestamp());
                        stmt.setInt(pos++, mbox.getId());
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
        String table = getMailItemTableName(mbox);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (Db.supports(Db.Capability.MULTITABLE_UPDATE)) {
                for (int i = 0; i < ids.size(); i += Db.getINClauseBatchSize()) {
                    int count = Math.min(Db.getINClauseBatchSize(), ids.size() - i);
                    stmt = conn.prepareStatement("UPDATE " + table + ", " +
                                "(SELECT parent_id pid, COUNT(*) count FROM " + getMailItemTableName(mbox) +
                                " WHERE " + IN_THIS_MAILBOX_AND + "id IN" + DbUtil.suitableNumberOfVariables(count) + "AND parent_id IS NOT NULL GROUP BY parent_id) AS x" +
                                " SET size = size - count, metadata = NULL, mod_metadata = ?, change_date = ?" +
                                " WHERE " + IN_THIS_MAILBOX_AND + "id = pid AND type = " + MailItem.TYPE_CONVERSATION);
                    int attr = 1;
                    stmt.setInt(attr++, mbox.getId());
                    for (int index = i; index < i + count; index++)
                        stmt.setInt(attr++, ids.get(index));
                    stmt.setInt(attr++, mbox.getOperationChangeID());
                    stmt.setInt(attr++, mbox.getOperationTimestamp());
                    stmt.setInt(attr++, mbox.getId());
                    stmt.executeUpdate();
                    stmt.close();
                }
            } else {
                stmt = conn.prepareStatement("SELECT parent_id, COUNT(*) FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id IN" + DbUtil.suitableNumberOfVariables(ids) + "AND parent_id IS NOT NULL" +
                        " GROUP BY parent_id");
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
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
                            " WHERE " + IN_THIS_MAILBOX_AND + "id IN " + DbUtil.suitableNumberOfVariables(update.getValue()) +
                            " AND type = " + MailItem.TYPE_CONVERSATION);
                    pos = 1;
                    stmt.setInt(pos++, update.getKey());
                    stmt.setInt(pos++, mbox.getOperationChangeID());
                    stmt.setInt(pos++, mbox.getOperationTimestamp());
                    stmt.setInt(pos++, mbox.getId());
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
            return Collections.emptyList();
        List<Integer> convIDs = new ArrayList<Integer>(candidates);

        List<Integer> purgedConvs = new ArrayList<Integer>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            for (int i = 0; i < convIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), convIDs.size() - i);
                stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(mbox) +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id IN" + DbUtil.suitableNumberOfVariables(count) + "AND size <= 0");
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, convIDs.get(index));
                rs = stmt.executeQuery();
                while (rs.next())
                    purgedConvs.add(rs.getInt(1));
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
        for (int id : ids)
            if (id > 0)
                targets.add(id);
        if (targets.size() == 0)
            return;

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        for (int i = 0; i < targets.size(); i += Db.getINClauseBatchSize()) {
            try {
                int count = Math.min(Db.getINClauseBatchSize(), targets.size() - i);
                stmt = conn.prepareStatement("DELETE FROM " + getMailItemTableName(mbox) +
                            " WHERE " + IN_THIS_MAILBOX_AND + "id IN" + DbUtil.suitableNumberOfVariables(count));
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
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
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, item instanceof VirtualConversation ? ((VirtualConversation) item).getMessageId() : item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting contents for " + MailItem.getNameForType(item) + " " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void writeTombstones(Mailbox mbox, MailItem.TypedIdList tombstones) throws ServiceException {
        if (tombstones == null || tombstones.isEmpty())
            return;

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
            stmt = conn.prepareStatement("INSERT INTO " + getTombstoneTableName(mbox) +
                        "(" + ("mailbox_id, ") + "sequence, date, type, ids)" +
                        " VALUES (" + ("?, ") + "?, ?, ?, ?)");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setByte(pos++, type);
            stmt.setString(pos++, ids);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing tombstones for " + MailItem.getNameForType(type) + "(s): " + ids, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static MailItem.TypedIdList readTombstones(Mailbox mbox, long lastSync) throws ServiceException {
        MailItem.TypedIdList tombstones = new MailItem.TypedIdList();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT type, ids FROM " + getTombstoneTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "sequence > ? AND ids IS NOT NULL" +
                        " ORDER BY sequence");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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


    private static final String FOLDER_TYPES         = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ')';
    private static final String FOLDER_AND_TAG_TYPES = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ',' + MailItem.TYPE_TAG + ')';
    private static final String MESSAGE_TYPES        = "(" + MailItem.TYPE_MESSAGE + ',' + MailItem.TYPE_CHAT + ')';
    private static final String DOCUMENT_TYPES       = "(" + MailItem.TYPE_DOCUMENT + ',' + MailItem.TYPE_WIKI + ')';
    private static final String CALENDAR_TYPES       = "(" + MailItem.TYPE_APPOINTMENT + ',' + MailItem.TYPE_TASK + ')';

    static final String NON_SEARCHABLE_TYPES = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ',' + MailItem.TYPE_TAG + ',' + MailItem.TYPE_CONVERSATION + ')';

    private static String typeConstraint(byte type) {
        if (type == MailItem.TYPE_FOLDER)
            return FOLDER_TYPES;
        else if (type == MailItem.TYPE_MESSAGE)
            return MESSAGE_TYPES;
        else if (type == MailItem.TYPE_DOCUMENT)
            return DOCUMENT_TYPES;
        else
            return "(" + type + ')';
    }


    public static Mailbox.MailboxData getFoldersAndTags(Mailbox mbox, Map<UnderlyingData, Long> folderData, Map<UnderlyingData, Long> tagData, boolean reload)
    throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String table = getMailItemTableName(mbox, "mi");

            stmt = conn.prepareStatement("SELECT " + DB_FIELDS + " FROM " + table +
                        " WHERE " + IN_THIS_MAILBOX_AND + "type IN " + FOLDER_AND_TAG_TYPES);
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                UnderlyingData data = constructItem(rs);
                if (MailItem.isAcceptableType(MailItem.TYPE_FOLDER, data.type))
                    folderData.put(data, -1L);
                else if (MailItem.isAcceptableType(MailItem.TYPE_TAG, data.type))
                    tagData.put(data, -1L);

                rs.getInt(CI_UNREAD);
                reload |= rs.wasNull();
            }
            rs.close();
            stmt.close();

            if (!reload)
                return null;

            Map<Integer, UnderlyingData> lookup = new HashMap<Integer, UnderlyingData>(folderData.size() + tagData.size());

            // going to recalculate counts, so discard any existing counts...
            for (Map.Entry<UnderlyingData, Long> entry : folderData.entrySet()) {
                UnderlyingData data = entry.getKey();
                lookup.put(data.id, data);
                data.size = data.unreadCount = 0;
                entry.setValue(0L);
            }

            for (UnderlyingData data : tagData.keySet()) {
                lookup.put(data.id, data);
                data.size = data.unreadCount = 0;
            }

            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            stmt = conn.prepareStatement("SELECT folder_id, type, tags, COUNT(*), SUM(unread), SUM(size)" +
                        " FROM " + table + " WHERE " + IN_THIS_MAILBOX_AND + "type NOT IN " + NON_SEARCHABLE_TYPES +
                        " GROUP BY folder_id, type, tags");
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                byte type  = rs.getByte(2);
                int count  = rs.getInt(4);
                int unread = rs.getInt(5);
                long size  = rs.getLong(6);

                if (type == MailItem.TYPE_CONTACT)
                    mbd.contacts += count;
                mbd.size += size;

                UnderlyingData data = lookup.get(rs.getInt(1));
                assert(data != null);
                data.unreadCount += unread;
                data.size += count;
                Long folderSize = folderData.get(data);
                folderData.put(data, folderSize == null ? size : folderSize + size);

                long tags = rs.getLong(3);
                for (int i = 0; tags != 0 && i < MailItem.MAX_TAG_COUNT - 1; i++) {
                    if ((tags & (1L << i)) != 0) {
                        data = lookup.get(i + MailItem.TAG_ID_OFFSET);
                        if (data != null)
                            data.unreadCount += unread;
                        // could track cumulative count if desired...
                        tags &= ~(1L << i);
                    }
                }
            }

            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("SELECT mi.folder_id, SUM(rev.size)" +
                        " FROM " + table + ", " + getRevisionTableName(mbox, "rev") +
                        " WHERE rev.mailbox_id = ? AND mi.mailbox_id = rev.mailbox_id AND mi.id = rev.item_id" +
                        " GROUP BY folder_id");
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                UnderlyingData data = lookup.get(rs.getInt(1));
                assert(data != null);
                Long folderSize = folderData.get(data);
                folderData.put(data, folderSize == null ? rs.getLong(2) : folderSize + rs.getLong(2));
            }

            return mbd;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching folder data for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getByType(Mailbox mbox, byte type, byte sort) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox, " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + "type IN " + typeConstraint(type) + DbSearch.sortQuery(sort));
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();
            while (rs.next())
                result.add(constructItem(rs));

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
        return getByParent(parent, DbSearch.DEFAULT_SORT_ORDER);
    }

    public static List<UnderlyingData> getByParent(MailItem parent, byte sort) throws ServiceException {
        Mailbox mbox = parent.getMailbox();
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(parent.getMailbox(), " mi") +
                    " WHERE " + IN_THIS_MAILBOX_AND + "parent_id = ? " + DbSearch.sortQuery(sort));
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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

    public static List<UnderlyingData> getByFolder(Folder folder, byte type, byte sort) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        Mailbox mbox = folder.getMailbox();
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(folder.getMailbox(), " mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + typeConstraint(type) +
                        DbSearch.sortQuery(sort));
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND imap_id = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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
                            " WHERE " + IN_THIS_MAILBOX_AND + "id IN " + DbUtil.suitableNumberOfVariables(count));
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + typeConstraint(type) +
                        " AND " + Db.equalsSTRING("name"));
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox, "mi") + ", " + getConversationTableName(mbox, "oc") +
                        " WHERE oc.hash = ? AND mi.id = oc.conv_id" +
                        (" AND oc.mailbox_id = ? AND mi.mailbox_id = oc.mailbox_id"));
            int pos = 1;
            stmt.setString(pos++, hash);
            stmt.setInt(pos++, mbox.getId());
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

    public static Pair<List<Integer>,MailItem.TypedIdList> getModifiedItems(Mailbox mbox, byte type, long lastSync, Set<Integer> visible)
    throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);

        List<Integer> modified = new ArrayList<Integer>();
        MailItem.TypedIdList missed = new MailItem.TypedIdList();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String typeConstraint = type == MailItem.TYPE_UNKNOWN ? "type NOT IN " + NON_SEARCHABLE_TYPES : "type IN " + typeConstraint(type);
            stmt = conn.prepareStatement("SELECT id, type, folder_id" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "mod_metadata > ? AND " + typeConstraint +
                        " ORDER BY mod_metadata, id");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setLong(pos++, lastSync);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (visible == null || visible.contains(rs.getInt(3)))
                    modified.add(rs.getInt(1));
                else
                    missed.add(rs.getByte(2), rs.getInt(1));
            }

            return new Pair<List<Integer>,MailItem.TypedIdList>(modified, missed);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting items modified since " + lastSync, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static void completeConversation(Mailbox mbox, UnderlyingData data) throws ServiceException {
        List<UnderlyingData> list = new ArrayList<UnderlyingData>();
        list.add(data);
        completeConversations(mbox, list);
    }

    private static void completeConversations(Mailbox mbox, List<UnderlyingData> convData) throws ServiceException {
        if (convData == null || convData.isEmpty())
            return;
        Map<Integer, UnderlyingData> conversations = new HashMap<Integer, UnderlyingData>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        for (int i = 0; i < convData.size(); i += Db.getINClauseBatchSize()) {
            try {
                int count = Math.min(Db.getINClauseBatchSize(), convData.size() - i);
                stmt = conn.prepareStatement("SELECT parent_id, id, unread, flags, tags" +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "parent_id IN " + DbUtil.suitableNumberOfVariables(count) +
                        " ORDER BY parent_id");
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
                for (int index = i; index < i + count; index++) {
                    UnderlyingData data = convData.get(index);
                    assert(data.type == MailItem.TYPE_CONVERSATION);
                    stmt.setInt(pos++, data.id);
                    conversations.put(data.id, data);
                }
                rs = stmt.executeQuery();

                int lastConvId = -1;
                List<Long> inheritedTags = new ArrayList<Long>();
                List<Integer> children = new ArrayList<Integer>();
                int unreadCount = 0;

                while (rs.next()) {
                    int convId = rs.getInt(1);
                    if (convId != lastConvId) {
                        // New conversation.  Update stats for the last one and reset counters.
                        if (lastConvId != -1) {
                            // Update stats for the previous conversation
                            UnderlyingData data = conversations.get(lastConvId);
                            data.children      = children;
                            data.unreadCount   = unreadCount;
                            data.inheritedTags = StringUtil.join(",", inheritedTags);
                        }
                        lastConvId = convId;
                        children = new ArrayList<Integer>();
                        inheritedTags.clear();
                        unreadCount = 0;
                    }

                    // Read next row
                    children.add(rs.getInt(2));
                    if (rs.getBoolean(3))
                        unreadCount++;
                    inheritedTags.add(-rs.getLong(4));
                    inheritedTags.add(rs.getLong(5));
                }

                // Update the last conversation.
                UnderlyingData data = conversations.get(lastConvId);
                if (data != null) {
                    data.children      = children;
                    data.unreadCount   = unreadCount;
                    data.inheritedTags = StringUtil.join(",", inheritedTags);
                } else {
                    // Data error: no messages found
                    StringBuilder msg = new StringBuilder("No messages found for conversations:");
                    for (UnderlyingData ud : convData)
                        msg.append(' ').append(ud.id);
                    msg.append(".  lastConvId=").append(lastConvId);
                    sLog.error(msg);
                }
            } catch (SQLException e) {
                throw ServiceException.FAILURE("completing conversation data", e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
            }
        }
    }

    private static final String LEAF_NODE_FIELDS = "id, size, type, unread, folder_id, parent_id, " +
                                                   Db.selectBOOLEAN("blob_digest IS NOT NULL") + ',' +
                                                   " mod_content, mod_metadata, flags, index_id, volume_id";

    private static final int LEAF_CI_ID           = 1;
    private static final int LEAF_CI_SIZE         = 2;
    private static final int LEAF_CI_TYPE         = 3;
    private static final int LEAF_CI_IS_UNREAD    = 4;
    private static final int LEAF_CI_FOLDER_ID    = 5;
    private static final int LEAF_CI_PARENT_ID    = 6;
    private static final int LEAF_CI_HAS_BLOB     = 7;
    private static final int LEAF_CI_MOD_CONTENT  = 8;
    private static final int LEAF_CI_MOD_METADATA = 9;
    private static final int LEAF_CI_FLAGS        = 10;
    private static final int LEAF_CI_INDEX_ID     = 11;
    private static final int LEAF_CI_VOLUME_ID    = 12;

    public static PendingDelete getLeafNodes(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        PendingDelete info = new PendingDelete();
        int folderId = folder.getId();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String constraint = IN_THIS_MAILBOX_AND + "folder_id = ? AND type NOT IN " + FOLDER_TYPES;
            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + constraint);
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, folderId);
            rs = stmt.executeQuery();

            info.rootId = folderId;
            info.size   = 0;
            accumulateLeafNodes(info, mbox, rs);
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

    public static PendingDelete getLeafNodes(Mailbox mbox, List<Folder> folders, int before, boolean globalMessages, Boolean unread)
    throws ServiceException {
        PendingDelete info = new PendingDelete();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String constraint;
            if (globalMessages)
                constraint = "date < ? AND type IN " + typeConstraint(MailItem.TYPE_MESSAGE);
            else
                constraint = "date < ? AND type NOT IN " + NON_SEARCHABLE_TYPES +
                             " AND folder_id IN" + DbUtil.suitableNumberOfVariables(folders);
            if (unread != null)
                constraint += " AND unread = ?";

            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + constraint);
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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
            return accumulateLeafNodes(info, mbox, rs);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of items for purge", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static PendingDelete getImapDeleted(Mailbox mbox, Set<Folder> folders) throws ServiceException {
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

            String flagconstraint = flagsets == null ? "" : " AND flags IN" + DbUtil.suitableNumberOfVariables(flagsets);
            String folderconstraint = folders == null ? "" : " AND folder_id IN" + DbUtil.suitableNumberOfVariables(folders);

            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                        " FROM " + getMailItemTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "type IN " + IMAP_TYPES + flagconstraint + folderconstraint);
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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
            return accumulateLeafNodes(info, mbox, rs);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of \\Deleted items for purge", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static class LocationCount {
        public int count;
        public long size;
        public LocationCount(int c, long sz)              { count = c;  size = sz; }
        public LocationCount(LocationCount lc)            { count = lc.count;  size = lc.size; }
        public LocationCount increment(int c, long sz)    { count += c;  size += sz;  return this; }
        public LocationCount increment(LocationCount lc)  { count += lc.count;  size += lc.size;  return this; }
    }

    private static PendingDelete accumulateLeafNodes(PendingDelete info, Mailbox mbox, ResultSet rs) throws SQLException, ServiceException {
        StoreManager sm = StoreManager.getInstance();
        List<Integer> versioned = null;

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

            Integer folderId = rs.getInt(LEAF_CI_FOLDER_ID);
            LocationCount count = info.messages.get(folderId);
            if (count == null)
                info.messages.put(folderId, new LocationCount(1, size));
            else
                count.increment(1, size);

            boolean hasBlob = rs.getBoolean(LEAF_CI_HAS_BLOB);
            if (hasBlob) {
                short volumeId = rs.getShort(LEAF_CI_VOLUME_ID);
                try {
                    MailboxBlob mblob = sm.getMailboxBlob(mbox, id, revision, volumeId);
                    if (mblob == null)
                        sLog.error("missing blob for id: " + id + ", change: " + revision);
                    else
                        info.blobs.add(mblob);
                } catch (Exception e1) { }
            }

            int flags = rs.getInt(LEAF_CI_FLAGS);
            if ((flags & Flag.BITMASK_VERSIONED) != 0)
                (versioned == null ? new ArrayList<Integer>() : versioned).add(id);

            Integer indexId = new Integer(rs.getInt(LEAF_CI_INDEX_ID));
            boolean indexed = !rs.wasNull();
            if (indexed) {
                if (info.sharedIndex == null)
                    info.sharedIndex = new HashSet<Integer>();
                boolean shared = (flags & Flag.BITMASK_COPIED) != 0;
                if (!shared)  info.indexIds.add(indexId);
                else          info.sharedIndex.add(indexId);
            }
        }

        if (versioned != null)
            accumulateLeafRevisions(info, mbox, versioned);

        return info;
    }

    private static void accumulateLeafRevisions(PendingDelete info, Mailbox mbox, List<Integer> versioned) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        StoreManager sm = StoreManager.getInstance();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT mi.id, mi.folder_id, rev.size, rev.mod_content, rev.volume_id, " + Db.selectBOOLEAN("rev.blob_digest IS NOT NULL") +
                    " FROM " + getMailItemTableName(mbox, "mi") + ", " + getRevisionTableName(mbox, "rev") +
                    " WHERE mi.mailbox_id = ? AND mi.id IN " + DbUtil.suitableNumberOfVariables(versioned) +
                    " AND mi.mailbox_id = rev.mailbox_id AND mi.id = rev.item_id");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            for (int vid : versioned)
                stmt.setInt(pos++, vid);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer folderId = rs.getInt(2);
                LocationCount count = info.messages.get(folderId);
                if (count == null)
                    info.messages.put(folderId, new LocationCount(0, rs.getLong(3)));
                else
                    count.increment(0, rs.getLong(3));

                boolean hasBlob = rs.getBoolean(6);
                if (hasBlob) {
                    try {
                        MailboxBlob mblob = sm.getMailboxBlob(mbox, rs.getInt(1), rs.getInt(4), rs.getShort(5));
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

    public static void resolveSharedIndex(Mailbox mbox, PendingDelete info) throws ServiceException {
        if (info.sharedIndex == null || info.sharedIndex.isEmpty())
            return;
        List<Integer> indexIDs = new ArrayList<Integer>(info.sharedIndex);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            for (int i = 0; i < indexIDs.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), indexIDs.size() - i);
                stmt = conn.prepareStatement("SELECT index_id FROM " + getMailItemTableName(mbox) +
                            " WHERE " + IN_THIS_MAILBOX_AND + "index_id IN " + DbUtil.suitableNumberOfVariables(count));
                int pos = 1;
                stmt.setInt(pos++, mbox.getId());
                for (int index = i; index < i + count; index++)
                    stmt.setInt(pos++, indexIDs.get(index));
                rs = stmt.executeQuery();
                while (rs.next())
                    info.sharedIndex.remove(rs.getInt(1));
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
        List<ImapMessage> result = new ArrayList<ImapMessage>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + IMAP_FIELDS +
                        " FROM " + getMailItemTableName(folder.getMailbox(), " mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + IMAP_TYPES);
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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

    public static int countImapRecent(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + getMailItemTableName(folder.getMailbox()) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + IMAP_TYPES +
                        " AND (imap_id IS NULL OR imap_id = 0 OR imap_id > ?)");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, folder.getId());
            stmt.setInt(pos++, folder.getImapRECENT());
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

    public static List<Pop3Message> loadPop3Folder(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        List<Pop3Message> result = new ArrayList<Pop3Message>();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + POP3_FIELDS +
                        " FROM " + getMailItemTableName(mbox, " mi") +
                        " WHERE " + IN_THIS_MAILBOX_AND + "folder_id = ? AND type IN " + POP3_TYPES +
                        " AND NOT " + Db.bitmaskAND("flags", Flag.BITMASK_DELETED));
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, folder.getId());
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

        List<UnderlyingData> dlist = new ArrayList<UnderlyingData>();
        if (!item.isTagged(mbox.mVersionedFlag))
            return dlist;

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + REVISION_FIELDS + " FROM " + getRevisionTableName(mbox) +
                        " WHERE " + IN_THIS_MAILBOX_AND + "item_id = ?" +
                        " ORDER BY version");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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

    
    public static List<SearchResult> listByFolder(Folder folder, byte type) throws ServiceException {
        return listByFolder(folder, type, true);
    }

    public static List<SearchResult> listByFolder(Folder folder, byte type, boolean descending) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        Connection conn = mbox.getOperationConnection();
        boolean allTypes = type == MailItem.TYPE_UNKNOWN;

        ArrayList<SearchResult> result = new ArrayList<SearchResult>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String typeConstraint = allTypes ? "" : "type = ? AND ";
            stmt = conn.prepareStatement("SELECT id, index_id, type, date FROM " + getMailItemTableName(folder) +
                        " WHERE " + IN_THIS_MAILBOX_AND + typeConstraint + "folder_id = ?" +
                        " ORDER BY date" + (descending ? " DESC" : ""));
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            if (!allTypes)
                stmt.setByte(pos++, type);
            stmt.setInt(pos++, folder.getId());
            rs = stmt.executeQuery();

            while (rs.next())
                result.add(SearchResult.createResult(rs, DbSearch.SORT_BY_DATE));
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
        data.indexId     = rs.getInt(CI_INDEX_ID + offset);
        if (rs.wasNull())
            data.indexId = -1;
        data.imapId      = rs.getInt(CI_IMAP_ID + offset);
        if (rs.wasNull())
            data.imapId = -1;
        data.date        = rs.getInt(CI_DATE + offset);
        data.size        = rs.getLong(CI_SIZE + offset);
        data.volumeId    = rs.getShort(CI_VOLUME_ID + offset);
        if (rs.wasNull())
            data.volumeId = -1;
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
        if (data.indexId == 0)      data.indexId = -1;
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
        data.indexId     = -1;
        data.imapId      = -1;
        data.date        = rs.getInt(1);
        data.size        = rs.getLong(2);
        data.volumeId    = rs.getShort(3);
        if (rs.wasNull())
            data.volumeId = -1;
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

    private static final String APPOINTMENT_TYPE = "(" + MailItem.TYPE_APPOINTMENT + ")";
    private static final String TASK_TYPE = "(" + MailItem.TYPE_TASK + ")";

    public static UnderlyingData getCalendarItem(Mailbox mbox, String uid)
    throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getCalendarItemTableName(mbox, "ci") + ", " + getMailItemTableName(mbox, "mi") +
                    " WHERE ci.uid = ? AND mi.id = ci.item_id AND mi.type IN " + CALENDAR_TYPES +
                    (" AND ci.mailbox_id = ? AND mi.mailbox_id = ci.mailbox_id"));

            int pos = 1;
            stmt.setString(pos++, uid);
            stmt.setInt(pos++, mbox.getId());
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
     * Return all of the Invite records within the range start<=Invites<end.  IE "Give me all the 
     * invites between 7:00 and 9:00 will return you everything from 7:00 to 8:59:59.99
     * @param start
     * @param end
     * @param folderId 
     * @return list of invites
     */
    public static List<UnderlyingData> getCalendarItems(Mailbox mbox, byte type, long start, long end, int folderId, int[] excludeFolderIds) 
    throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean folderSpecified = folderId != Mailbox.ID_AUTO_INCREMENT;

            String excludeFolderPart = "";
            if (excludeFolderIds != null) 
                excludeFolderPart = " AND folder_id NOT IN" + DbUtil.suitableNumberOfVariables(excludeFolderIds);

            String typeList;
            if (type == MailItem.TYPE_APPOINTMENT)
                typeList = APPOINTMENT_TYPE;
            else if (type == MailItem.TYPE_TASK)
                typeList = TASK_TYPE;
            else
                typeList = CALENDAR_TYPES;
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getCalendarItemTableName(mbox, "ci") + ", " + getMailItemTableName(mbox, "mi") +
                        " WHERE ci.start_time < ? AND ci.end_time > ? AND mi.id = ci.item_id AND mi.type IN " + typeList +
                        " AND ci.mailbox_id = ? AND mi.mailbox_id = ci.mailbox_id" +
                        (folderSpecified ? " AND folder_id = ?" : "") + excludeFolderPart);

            int param = 1;
            stmt.setTimestamp(param++, new Timestamp(end));
            stmt.setTimestamp(param++, new Timestamp(start));
            stmt.setInt(param++, mbox.getId());
            if (folderSpecified)
                stmt.setInt(param++, folderId);
            if (excludeFolderIds != null) {
                for (int id : excludeFolderIds)
                    stmt.setInt(param++, id);
            }

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


     public static List<UnderlyingData> getCalendarItemsAll(Mailbox mbox, byte type,
                                                            int folderId, int[] excludeFolderIds) 
    throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean folderSpecified = folderId != Mailbox.ID_AUTO_INCREMENT;

            String excludeFolderPart = "";
            if (excludeFolderIds != null) 
                excludeFolderPart = " AND folder_id NOT IN" + DbUtil.suitableNumberOfVariables(excludeFolderIds);

            String typeList;
            if (type == MailItem.TYPE_APPOINTMENT)
                typeList = APPOINTMENT_TYPE;
            else if (type == MailItem.TYPE_TASK)
                typeList = TASK_TYPE;
            else
                typeList = CALENDAR_TYPES;
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getCalendarItemTableName(mbox, "ci") + ", " + getMailItemTableName(mbox, "mi") +
                        " WHERE mi.id = ci.item_id AND mi.type IN " + typeList +
                        " AND ci.mailbox_id = ? AND mi.mailbox_id = ci.mailbox_id" +
                        (folderSpecified ? " AND folder_id = ?" : "") + excludeFolderPart);

            int param = 1;

            stmt.setInt(param++, mbox.getId());
            if (folderSpecified)
                stmt.setInt(param++, folderId);
            if (excludeFolderIds != null) {
                for (int id : excludeFolderIds)
                    stmt.setInt(param++, id);
            }

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

    public static void addToCalendarItemTable(CalendarItem calItem) throws ServiceException {
        Mailbox mbox = calItem.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            Timestamp startTs = new Timestamp(calItem.getStartTime());

            long end = calItem.getEndTime();
            Timestamp endTs = new Timestamp(end <= 0 ? MAX_DATE : end);

            stmt = conn.prepareStatement("INSERT INTO " + getCalendarItemTableName(mbox) +
                        " (" + ("mailbox_id, ") + "uid, item_id, start_time, end_time)" +
                        " VALUES (" + ("?, ") + "?, ?, ?, ?)");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
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

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getCalendarItemTableName(mbox) +
                        " (" + ("mailbox_id, ") + "uid, item_id, start_time, end_time)" +
                        " VALUES (" + ("?, ") + "?, ?, ?, ?)" +
                        (Db.supports(Db.Capability.ON_DUPLICATE_KEY) ? " ON DUPLICATE KEY UPDATE uid = ?, item_id = ?, start_time = ?, end_time = ?" : ""));
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, calItem.getUid());
            stmt.setInt(pos++, calItem.getId());
            stmt.setTimestamp(pos++, startTs);
            stmt.setTimestamp(pos++, endTs);
            if (Db.supports(Db.Capability.ON_DUPLICATE_KEY)) {
                stmt.setString(pos++, calItem.getUid());
                stmt.setInt(pos++, calItem.getId());
                stmt.setTimestamp(pos++, startTs);
                stmt.setTimestamp(pos++, endTs);
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            if (!Db.supports(Db.Capability.ON_DUPLICATE_KEY) && Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                try {
                    stmt.close();

                    stmt = conn.prepareStatement("UPDATE " + getCalendarItemTableName(mbox) +
                            " SET item_id = ?, start_time = ?, end_time = ? WHERE " + IN_THIS_MAILBOX_AND + "uid = ?");
                    int pos = 1;
                    stmt.setInt(pos++, calItem.getId());
                    stmt.setTimestamp(pos++, startTs);
                    stmt.setTimestamp(pos++, endTs);
                    stmt.setInt(pos++, mbox.getId());
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


    /** Makes sure that the argument won't overflow the maximum length of a
     *  MySQL VARCHAR(128) column (128 characters) by truncating the string
     *  if necessary.
     * 
     * @param sender  The string to check (can be null).
     * @return The passed-in String, truncated to 128 chars. */
    static String checkSenderLength(String sender) {
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
    static String checkSubjectLength(String subject) throws ServiceException {
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
    static String checkMetadataLength(String metadata) throws ServiceException {
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
    public static String getMailItemTableName(int mailboxId, int groupId) {
        return DbMailbox.getDatabaseName(groupId) + '.' + TABLE_MAIL_ITEM;
    }
    public static String getMailItemTableName(Mailbox mbox) {
        return getMailItemTableName(mbox.getId(), mbox.getSchemaGroupId());
    }
    public static String getMailItemTableName(Mailbox mbox, String alias) {
        return getMailItemTableName(mbox) + " AS " + alias;
    }
    public static String getMailItemTableName(MailItem item) {
        return getMailItemTableName(item.getMailbox());
    }

    /**
     * Returns the name of the table that stores data on old revisions of {@link MailItem}s.
     * The table name is qualified by the name of the database (e.g. <tt>mailbox1.mail_item</tt>).
     */
    public static String getRevisionTableName(int mailboxId, int groupId) {
        return DbMailbox.getDatabaseName(groupId) + '.' + TABLE_REVISION;
    }
    public static String getRevisionTableName(Mailbox mbox) {
        return getRevisionTableName(mbox.getId(), mbox.getSchemaGroupId());
    }
    public static String getRevisionTableName(Mailbox mbox, String alias) {
        return getRevisionTableName(mbox) + " AS " + alias;
    }
    public static String getRevisionTableName(MailItem item) {
        return getRevisionTableName(item.getMailbox());
    }

    /**
     * Returns the name of the table that stores {@link CalendarItem} data.  The table name is qualified
     * by the name of the database (e.g. <tt>mailbox1.appointment</tt>).
     */
    public static String getCalendarItemTableName(int mailboxId, int groupId) {
        return DbMailbox.getDatabaseName(groupId) + '.' + TABLE_APPOINTMENT;
    }
    public static String getCalendarItemTableName(Mailbox mbox) {
        return getCalendarItemTableName(mbox.getId(), mbox.getSchemaGroupId());
    }
    public static String getCalendarItemTableName(Mailbox mbox, String alias) {
        return getCalendarItemTableName(mbox) + " AS " + alias;
    }

    /**
     * Returns the name of the table that maps subject hashes to {@link Conversation} ids.  The table 
     * name is qualified by the name of the database (e.g. <tt>mailbox1.open_conversation</tt>).
     */
    public static String getConversationTableName(int mailboxId, int groupId) {
        return DbMailbox.getDatabaseName(groupId) + '.' + TABLE_OPEN_CONVERSATION;
    }
    public static String getConversationTableName(Mailbox mbox) {
        return getConversationTableName(mbox.getId(), mbox.getSchemaGroupId());
    }
    public static String getConversationTableName(Mailbox mbox, String alias) {
        return getConversationTableName(mbox) + " AS " + alias;
    }
    public static String getConversationTableName(MailItem item) {
        return getConversationTableName(item.getMailbox());
    }

    /**
     * Returns the name of the table that stores data on deleted items for the purpose of sync.
     * The table name is qualified by the name of the database (e.g. <tt>mailbox1.tombstone</tt>).
     */
    public static String getTombstoneTableName(int mailboxId, int groupId) {
        return DbMailbox.getDatabaseName(groupId) + '.' + TABLE_TOMBSTONE;
    }
    public static String getTombstoneTableName(Mailbox mbox) {
        return getTombstoneTableName(mbox.getId(), mbox.getSchemaGroupId());
    }

    private static boolean areTagsetsLoaded(int mailboxId) {
        synchronized(sTagsetCache) {
            return sTagsetCache.containsKey(new Integer(mailboxId));
        }
    }

    static TagsetCache getTagsetCache(Connection conn, Mailbox mbox)
    throws ServiceException {
        int mailboxId = mbox.getId();
        Integer id = new Integer(mailboxId);
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

    private static boolean areFlagsetsLoaded(int mailboxId) {
        synchronized(sFlagsetCache) {
            return sFlagsetCache.containsKey(new Integer(mailboxId));
        }
    }

    static TagsetCache getFlagsetCache(Connection conn, Mailbox mbox)
    throws ServiceException {
        int mailboxId = mbox.getId();
        Integer id = new Integer(mailboxId);
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
        if (ids == null) {
            return null;
        }
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
}
