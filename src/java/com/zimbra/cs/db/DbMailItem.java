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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Aug 13, 2004
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.MailItem.PendingDelete;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.ArrayUtil;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.TimeoutMap;
import com.zimbra.cs.util.ZimbraLog;


/**
 * @author dkarp
 */
public class DbMailItem {

    private static Log sLog = LogFactory.getLog(DbMailItem.class);

    /** Maps the mailbox id to the set of all tag combinations stored for all
     *  items in the mailbox.  Enables fast database lookup by tag. */
    private static final Map<Integer, TagsetCache> sTagsetCache =
        new TimeoutMap<Integer, TagsetCache>(120 * Constants.MILLIS_PER_MINUTE);

    /** Maps the mailbox id to the set of all flag combinations stored for all
     *  items in the mailbox.  Enables fast database lookup by flag. */
    private static final Map<Integer, TagsetCache> sFlagsetCache =
        new TimeoutMap<Integer, TagsetCache>(120 * Constants.MILLIS_PER_MINUTE);


    public static void create(Mailbox mbox, UnderlyingData data) throws ServiceException {
        if (data == null || data.id <= 0 || data.folderId <= 0 || data.parentId == 0 || data.indexId == 0)
            throw ServiceException.FAILURE("invalid data for DB item create", null);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getMailItemTableName(mbox) +
                    "(id, type, parent_id, folder_id, index_id, date, size, volume_id, blob_digest, unread," +
                    " flags, tags, sender, subject, metadata, mod_metadata, change_date, mod_content) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int pos = 1;
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
            stmt.setInt(pos++, data.date);
            stmt.setInt(pos++, (int) data.size);
            if (data.volumeId >= 0)
                stmt.setShort(pos++, data.volumeId);
            else
                stmt.setNull(pos++, Types.TINYINT);
            stmt.setString(pos++, data.blobDigest);
//            if (data.type == MailItem.TYPE_MESSAGE || data.type == MailItem.TYPE_INVITE)
            if (data.type == MailItem.TYPE_MESSAGE)
                stmt.setBoolean(pos++, data.isUnread());
            else
                stmt.setNull(pos++, java.sql.Types.BOOLEAN);
            stmt.setInt(pos++, data.flags);
            stmt.setLong(pos++, data.tags);
            stmt.setString(pos++, data.sender);
            stmt.setString(pos++, data.subject);
            stmt.setString(pos++, data.metadata);
            stmt.setInt(pos++, data.modMetadata);
            stmt.setInt(pos++, data.dateChanged);
            stmt.setInt(pos++, data.modContent);
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to create object", null);

            // Track the tags and flags for fast lookup later
            if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox.getId());
                tagsets.addTagset(data.tags);
            }
            if (areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox.getId());
                flagsets.addTagset(data.flags);
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (e.getErrorCode() == Db.Error.DUPLICATE_ROW)
                throw MailServiceException.ALREADY_EXISTS(data.id, e);
            else
                throw ServiceException.FAILURE("writing new object of type " + data.type, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void copy(MailItem item, int id, int folderId, int parentId, short volumeId, String metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        if (id <= 0 || folderId <= 0 || parentId == 0)
            throw ServiceException.FAILURE("invalid data for DB item copy", null);

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            String table = getMailItemTableName(mbox);
            stmt = conn.prepareStatement("INSERT INTO " + table +
                    "(id, type, parent_id, folder_id, index_id, date, size, volume_id, blob_digest," +
                    " unread, flags, tags, sender, subject, metadata, mod_metadata, change_date, mod_content) " +
                    "(SELECT ?, type, ?, ?, index_id, date, size, ?, blob_digest, unread," +
                    " flags, tags, sender, subject, ?, ?, ?, ? FROM " + table + " WHERE id = ?)");
            int pos = 1;
            stmt.setInt(pos++, id);
            if (parentId <= 0)
                // Messages in virtual conversations are stored with a null parent_id
                stmt.setNull(pos++, Types.INTEGER);
            else
                stmt.setInt(pos++, parentId);
            stmt.setInt(pos++, folderId);
            if (volumeId >= 0)
                stmt.setShort(pos++, volumeId);
            else
                stmt.setNull(pos++, Types.TINYINT);
            stmt.setString(pos++, metadata);
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, mbox.getOperationTimestamp());
            stmt.setInt(pos++, mbox.getOperationChangeID());
            stmt.setInt(pos++, item.getId());
            int num = stmt.executeUpdate();
            if (num != 1)
                throw ServiceException.FAILURE("failed to create object", null);
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (e.getErrorCode() == Db.Error.DUPLICATE_ROW)
                throw MailServiceException.ALREADY_EXISTS(id, e);
            else
                throw ServiceException.FAILURE("copying " + MailItem.getNameForType(item.getType()) + ": " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void changeType(MailItem item, byte type) throws ServiceException {
        Connection conn = item.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET type = ? WHERE id = ?");
            stmt.setInt(1, type);
            stmt.setInt(2, item.getId());
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
        Connection conn = mbox.getOperationConnection();

        PreparedStatement stmt = null;
        try {
            int attr = 1;
            if (item instanceof Folder) {
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET parent_id = ?, folder_id = ?, mod_metadata = ?, change_date = ? WHERE id = ?");
                stmt.setInt(attr++, folder.getId());
            } else if (item instanceof Conversation && !(item instanceof VirtualConversation))
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET folder_id = ?, mod_metadata = ?, change_date = ? WHERE parent_id = ?");
            else
                stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                        " SET folder_id = ?, mod_metadata = ?, change_date = ? WHERE id = ?");
            stmt.setInt(attr++, folder.getId());
            stmt.setInt(attr++, mbox.getOperationChangeID());
            stmt.setInt(attr++, mbox.getOperationTimestamp());
            stmt.setInt(attr++, item instanceof VirtualConversation ? ((VirtualConversation) item).getMessageId() : item.getId());
            stmt.executeUpdate();
//            return (num > 0);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing new folder data for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void setFolder(MailItem.Array itemIDs, Folder folder) throws ServiceException {
        if (itemIDs == null || itemIDs.length == 0)
            return;
        Mailbox mbox = folder.getMailbox();
        Connection conn = mbox.getOperationConnection();

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(folder) +
                    " SET folder_id = ?, mod_metadata = ?, change_date = ? WHERE id IN " + DbUtil.suitableNumberOfVariables(itemIDs));
            stmt.setInt(1, folder.getId());
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            for (int i = 0; i < itemIDs.length; i++)
                stmt.setInt(i + 4, itemIDs.array[i]);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing new folder data for item [" + itemIDs + ']', e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void setParent(MailItem parent, MailItem child) throws ServiceException {
        setParent(parent, new MailItem[] { child });
    }
    public static void setParent(MailItem parent, MailItem[] children) throws ServiceException {
        Mailbox mbox = parent.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(parent) +
                    " SET parent_id = ?, mod_metadata = ?, change_date = ?" +
                    " WHERE id IN " + DbUtil.suitableNumberOfVariables(children));
            int arg = 1;
            if (parent == null || parent instanceof VirtualConversation)
                stmt.setNull(arg++, Types.INTEGER);
            else
                stmt.setInt(arg++, parent.getId());
            stmt.setInt(arg++, mbox.getOperationChangeID());
            stmt.setInt(arg++, mbox.getOperationTimestamp());
            for (int i = 0; i < children.length; i++)
                stmt.setInt(arg++, children[i].getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("adding children to parent " + parent.getId(), e);
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
                    " SET parent_id = ?, mod_metadata = ?, change_date = ? WHERE " + relation);
            if (newParent instanceof VirtualConversation)
                stmt.setNull(1, Types.INTEGER);
            else
                stmt.setInt(1, newParent.getId());
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            stmt.setInt(4, oldParent instanceof VirtualConversation ? ((VirtualConversation) oldParent).getMessageId() : oldParent.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("writing new parent for children of item " + oldParent.getId(), e);
        } finally {
        	DbPool.closeStatement(stmt);
        }
    }

    public static void saveMetadata(MailItem item, long size, String metadata) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        Connection conn = mbox.getOperationConnection();
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
			        " SET date = ?, size = ?, metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                    " WHERE id = ?");
			stmt.setInt(1, (int) (item.getDate() / 1000));
			stmt.setInt(2, (int) size);
            stmt.setString(3, metadata);
            stmt.setInt(4, mbox.getOperationChangeID());
            stmt.setInt(5, mbox.getOperationTimestamp());
            stmt.setInt(6, item.getSavedSequence());
            stmt.setInt(7, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("writing metadata for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
        } finally {
        	DbPool.closeStatement(stmt);
        }
	}

    public static void saveSubject(MailItem item, long size) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;

        String subject = item.getSubject();
        if (item instanceof Conversation)
            subject = ((Conversation) item).getNormalizedSubject();
        else if (item instanceof Message)
            subject = ((Message) item).getNormalizedSubject();
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET date = ?, size = ?, subject = ?, mod_metadata = ?, change_date = ?" +
                    " WHERE id = ?");
            stmt.setInt(1, (int) (item.getDate() / 1000));
            stmt.setInt(2, (int) size);
            stmt.setString(3, subject);
            stmt.setInt(4, mbox.getOperationChangeID());
            stmt.setInt(5, mbox.getOperationTimestamp());
            stmt.setInt(6, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing subject for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveData(MailItem item, long size, String sender, String metadata)
    throws ServiceException {
        Mailbox mbox = item.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;

        String subject = item.getSubject();
        if (item instanceof Conversation)
            subject = ((Conversation) item).getNormalizedSubject();
        else if (item instanceof Message)
            subject = ((Message) item).getNormalizedSubject();
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET type = ?, parent_id = ?, date = ?, size = ?, blob_digest = ?, flags = ?," +
                    "     sender = ?, subject = ?, metadata = ?, mod_metadata = ?, change_date = ?, mod_content = ?" +
                    " WHERE id = ?");
            stmt.setByte(1, item.getType());
            if (item.getParentId() <= 0)
                // Messages in virtual conversations are stored with a null parent_id
                stmt.setNull(2, Types.INTEGER);
            else
            	stmt.setInt(2, item.getParentId());
            stmt.setInt(3, (int) (item.getDate() / 1000));
            stmt.setInt(4, (int) size);
            stmt.setString(5, item.getDigest());
            stmt.setInt(6, item.getInternalFlagBitmask());
            stmt.setString(7, sender);
            stmt.setString(8, subject);
            stmt.setString(9, metadata);
            stmt.setInt(10, mbox.getOperationChangeID());
            stmt.setInt(11, mbox.getOperationTimestamp());
            stmt.setInt(12, item.getSavedSequence());
            stmt.setInt(13, item.getId());
            stmt.executeUpdate();
            
            // Update the flagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox.getId());
                flagsets.addTagset(item.getInternalFlagBitmask());
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("rewriting row data for mailbox " + item.getMailboxId() + ", item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void openConversation(String hash, MailItem item) throws ServiceException {
        Connection conn = item.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getConversationTableName(item.getMailboxId()) +
                "(hash, conv_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE conv_id = ?");
            stmt.setString(1, hash);
            stmt.setInt(2, item.getId());
            stmt.setInt(3, item.getId());
            stmt.executeUpdate();
//            return (num == 1);  // This doesn't work.  In the UPDATE case MySQL returns 2 instead of 1. (bug)
//            return num > 0;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing open conversation association for hash " + hash, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void closeConversation(String hash, MailItem item) throws ServiceException {
        Connection conn = item.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getConversationTableName(item.getMailboxId()) +
                " WHERE hash = ? AND conv_id = ?");
            stmt.setString(1, hash);
            stmt.setInt(2, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing open conversation association for hash " + hash, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void changeOpenTarget(MailItem oldTarget, MailItem newTarget) throws ServiceException {
        Connection conn = oldTarget.getMailbox().getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getConversationTableName(oldTarget.getMailboxId()) +
                " SET conv_id = ? WHERE conv_id = ?");
            stmt.setInt(1, newTarget.getId());
            stmt.setInt(2, oldTarget.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("switching open conversation association for item " + oldTarget.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static int getTaggedUnreadChildCount(MailItem item, Tag tag) throws ServiceException {
        Mailbox mbox = item.getMailbox();
        if (mbox != tag.getMailbox())
            throw MailServiceException.WRONG_MAILBOX();
        Connection conn = mbox.getOperationConnection();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String relation = (item instanceof Folder ? "folder_id = ?" : "parent_id = ?");
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + getMailItemTableName(item) +
                    " WHERE " + relation + " AND unread = ? AND tags & ?");
            stmt.setInt(1, item.getId());
            stmt.setBoolean(2, true);
            stmt.setLong(3, tag.getBitmask());
            rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1);
            throw ServiceException.FAILURE("no data when fetching unread child count for item " + item.getId(), null);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching unread child count for item " + item.getId(), e);
        } finally {
            DbPool.closeResults(rs);
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
            if (item instanceof VirtualConversation)  relation = "id = ?";
            else if (item instanceof Conversation)    relation = "parent_id = ?";
            else if (item instanceof Folder)          relation = "folder_id = ?";
            else if (item instanceof Flag)            relation = "flags & ?";
            else if (item instanceof Tag)             relation = "tags & ?";
            else                                      relation = "id = ?";

            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET " + column + " = " + column + (add ? " | ?" : " & ?") + ", mod_metadata = ?, change_date = ?" +
                    " WHERE " + relation);
            stmt.setLong(1, add ? tag.getBitmask() : ~tag.getBitmask());
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            if (item instanceof Tag)
                stmt.setLong(4, ((Tag) item).getBitmask());
            else if (item instanceof VirtualConversation)
                stmt.setInt(4, ((VirtualConversation) item).getMessageId());
            else
                stmt.setInt(4, item.getId());
            stmt.executeUpdate();

            // Update the flagset or tagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (tag instanceof Flag && areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox.getId());
                flagsets.addTagset(item.getInternalFlagBitmask());
            } else if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox.getId());
                tagsets.addTagset(item.getTagBitmask());
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating tag data for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void alterTag(Tag tag, MailItem.Array itemIDs, boolean add)
    throws ServiceException {
        if (itemIDs == null || itemIDs.length == 0)
            return;
        Mailbox mbox = tag.getMailbox();
        Connection conn = mbox.getOperationConnection();

        PreparedStatement stmt = null;
        try {
            String column = (tag instanceof Flag ? "flags" : "tags");
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(tag) +
                    " SET " + column + " = " + column + (add ? " | ?" : " & ?") + ", mod_metadata = ?, change_date = ?" +
                    " WHERE id IN " + DbUtil.suitableNumberOfVariables(itemIDs));
            stmt.setLong(1, add ? tag.getBitmask() : ~tag.getBitmask());
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            for (int i = 0; i < itemIDs.length; i++)
                stmt.setInt(i + 4, itemIDs.array[i]);
            stmt.executeUpdate();

            // Update the flagset or tagset cache.  Assume that the item's in-memory
            // data has already been updated.
            if (tag instanceof Flag && areFlagsetsLoaded(mbox.getId())) {
                TagsetCache flagsets = getFlagsetCache(conn, mbox.getId());
                flagsets.applyMask(tag.getBitmask(), add);
            } else if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox.getId());
                tagsets.applyMask(tag.getBitmask(), add);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating tag data for items [" + itemIDs + "]", e);
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
            else if (item instanceof Flag)            relation = "flags & ?";
            else if (item instanceof Tag)             relation = "tags & ?";
            else                                      relation = "id = ?";

            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(item) +
                    " SET unread = ?, mod_metadata = ?, change_date = ?" +
                    " WHERE " + relation + " AND type = " + MailItem.TYPE_MESSAGE);
            stmt.setBoolean(1, unread);
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            if (item instanceof Tag)
                stmt.setLong(4, ((Tag) item).getBitmask());
            else if (item instanceof VirtualConversation)
                stmt.setInt(4, ((VirtualConversation) item).getMessageId());
            else
                stmt.setInt(4, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating unread state for item " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void alterUnread(Mailbox mbox, MailItem.Array itemIDs, boolean unread)
    throws ServiceException {
        if (itemIDs == null || itemIDs.length == 0)
            return;
        Connection conn = mbox.getOperationConnection();

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getMailItemTableName(mbox) +
                    " SET unread = ?, mod_metadata = ?, change_date = ?" +
                    " WHERE id IN" + DbUtil.suitableNumberOfVariables(itemIDs));
            stmt.setBoolean(1, unread);
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            for (int i = 0; i < itemIDs.length; i++)
                stmt.setInt(i + 4, itemIDs.array[i]);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating tag data for items [" + itemIDs + "]", e);
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
                    " SET tags = tags & ?, mod_metadata = ?, change_date = ?" +
                    " WHERE tags & ?");
            stmt.setLong(1, ~tag.getBitmask());
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            stmt.setLong(4, tag.getBitmask());
        	stmt.executeUpdate();

            if (areTagsetsLoaded(mbox.getId())) {
                TagsetCache tagsets = getTagsetCache(conn, mbox.getId());
                tagsets.applyMask(tag.getTagBitmask(), false);
            }
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("clearing all references to tag " + tag.getId(), e);
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
    public static List<Integer> markDeletionTargets(Folder folder) throws ServiceException {
        Mailbox mbox = folder.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String table = getMailItemTableName(folder);
            stmt = conn.prepareStatement("UPDATE " + table + ", " +
                    "(SELECT parent_id pid, COUNT(*) count FROM " +
                    getMailItemTableName(folder) + " WHERE folder_id = ? AND parent_id IS NOT NULL GROUP BY parent_id) x" +
                    " SET size = size - count, metadata = NULL, mod_metadata = ?, change_date = ?" +
                    " WHERE id = pid AND type = " + MailItem.TYPE_CONVERSATION);
            stmt.setInt(1, folder.getId());
            stmt.setInt(2, mbox.getOperationChangeID());
            stmt.setInt(3, mbox.getOperationTimestamp());
            stmt.executeUpdate();
            stmt.close();

            return getPurgedConversations(mbox);
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
    public static List<Integer> markDeletionTargets(Mailbox mbox, List<Integer> ids) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        String table = getMailItemTableName(mbox);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        for (int i = 0; i < ids.size(); i += DbUtil.IN_CLAUSE_BATCH_SIZE)
            try {
                int count = Math.min(DbUtil.IN_CLAUSE_BATCH_SIZE, ids.size() - i);
                stmt = conn.prepareStatement("UPDATE " + table + ", " +
                        "(SELECT parent_id pid, COUNT(*) count FROM " + getMailItemTableName(mbox) +
                        " WHERE id IN" + DbUtil.suitableNumberOfVariables(count) + "AND parent_id IS NOT NULL GROUP BY parent_id) x" +
                        " SET size = size - count, metadata = NULL, mod_metadata = ?, change_date = ?" +
                        " WHERE id = pid AND type = " + MailItem.TYPE_CONVERSATION);
                int attr = 1;
                for (int index = i; index < i + count; index++)
                	stmt.setInt(attr++, ids.get(index));
                stmt.setInt(attr++, mbox.getOperationChangeID());
                stmt.setInt(attr++, mbox.getOperationTimestamp());
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("marking deletions for conversations touching items " + ids, e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
            }

        return getPurgedConversations(mbox);
    }

    private static List<Integer> getPurgedConversations(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        ArrayList<Integer> purgedConvs = new ArrayList<Integer>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM " + getMailItemTableName(mbox) +
                    " WHERE type = " + MailItem.TYPE_CONVERSATION + " AND size <= 0");
            rs = stmt.executeQuery();
            while (rs.next())
                purgedConvs.add(rs.getInt(1));
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
        for (int i = 0; i < targets.size(); i += DbUtil.IN_CLAUSE_BATCH_SIZE)
            try {
                int count = Math.min(DbUtil.IN_CLAUSE_BATCH_SIZE, targets.size() - i);
                stmt = conn.prepareStatement("DELETE FROM " + getMailItemTableName(mbox) +
                        " WHERE id IN" + DbUtil.suitableNumberOfVariables(count));
                for (int index = i, attr = 1; index < i + count; index++)
                	stmt.setInt(attr++, targets.get(index));
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("deleting item(s): " + ids, e);
            } finally {
                DbPool.closeStatement(stmt);
            }
    }

    public static void deleteContents(MailItem item) throws ServiceException {
        String target;
        if (item instanceof VirtualConversation)  target = "id = ?";
        else if (item instanceof Conversation)    target = "parent_id = ?";
        else if (item instanceof SearchFolder)    return;
        else if (item instanceof Folder)          target = "folder_id = ?";
        else                                      return;

        Mailbox mbox = item.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getMailItemTableName(item) +
                    " WHERE " + target + " AND type NOT IN " + FOLDER_TYPES);
            stmt.setInt(1, item instanceof VirtualConversation ? ((VirtualConversation) item).getMessageId() : item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting contents for " + MailItem.getNameForType(item) + " " + item.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void writeCheckpoint(Mailbox mbox) throws ServiceException {
        writeTombstone(mbox, null);
    }
    public static void writeTombstone(Mailbox mbox, PendingDelete info) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            StringBuffer ids = null;
            if (info != null) {
                if (info.itemIds == null || info.itemIds.size() == 0)
                    return;
                ids = new StringBuffer();
                for (int i = 0; i < info.itemIds.size(); i++)
                    ids.append(i == 0 ? "" : ",").append(info.itemIds.get(i));
            }

            stmt = conn.prepareStatement("INSERT INTO " + getTombstoneTableName(mbox.getId()) + "(sequence, date, ids) VALUES (?,?,?)");
            stmt.setInt(1, mbox.getOperationChangeID());
            stmt.setInt(2, mbox.getOperationTimestamp());
            if (ids != null)
                stmt.setString(3, ids.toString());
            else
                stmt.setNull(3, Types.VARCHAR);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (info == null)
                throw ServiceException.FAILURE("writing change sequence checkpoint", e);
            else
                throw ServiceException.FAILURE("writing tombstones for item(s): " + info.itemIds, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static String readTombstones(Mailbox mbox, long lastSync) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        StringBuffer result = new StringBuffer();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT ids FROM " + getTombstoneTableName(mbox.getId()) +
                    " WHERE sequence > ? AND ids IS NOT NULL ORDER BY sequence");
            stmt.setLong(1, lastSync);
            rs = stmt.executeQuery();
            while (rs.next())
                result.append(result.length() == 0 ? "" : ",").append(rs.getString(1));
            return result.toString();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("reading tombstones since change: " + lastSync, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }


    private static final String FOLDER_TYPES         = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ')';
    private static final String FOLDER_AND_TAG_TYPES = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ',' + MailItem.TYPE_TAG + ')';
    private static final String NON_SEARCHABLE_TYPES = "(" + MailItem.TYPE_FOLDER + ',' + MailItem.TYPE_SEARCHFOLDER + ',' + MailItem.TYPE_MOUNTPOINT + ',' + MailItem.TYPE_TAG + ',' + MailItem.TYPE_CONVERSATION + ')';

    private static String typeConstraint(byte type) {
        if (type == MailItem.TYPE_FOLDER)
            return FOLDER_TYPES;
        else
            return "(" + type + ')';
    }

    public static final byte SORT_DESCENDING = 0x00;
    public static final byte SORT_ASCENDING  = 0x01;

    public static final byte SORT_BY_DATE    = 0x00;
    public static final byte SORT_BY_SENDER  = 0x02;
    public static final byte SORT_BY_SUBJECT = 0x04;
    public static final byte SORT_BY_ID      = 0x08;
    public static final byte SORT_NONE       = 0x10;

    public static final byte DEFAULT_SORT_ORDER = SORT_BY_DATE | SORT_DESCENDING;

    public static final byte SORT_DIRECTION_MASK = 0x01;
    public static final byte SORT_FIELD_MASK     = 0x4E;

    private static String sortField(byte sort) {
        switch (sort & SORT_FIELD_MASK) {
            case SORT_BY_SENDER:   return "sender";
            case SORT_BY_SUBJECT:  return "subject";
            case SORT_BY_ID:       return "id";
            case SORT_NONE:        return "NULL";
            case SORT_BY_DATE:
            default:               return "date";
        }
    }

    private static String sortQuery(byte sort) {
        return sortQuery(sort, "");
    }
    private static String sortQuery(byte sort, String prefix) {
        String field = sortField(sort);
        if ("NULL".equalsIgnoreCase(field))
            return "";
        StringBuffer statement = new StringBuffer(" ORDER BY ");
        statement.append(prefix).append(field);
        if ((sort & SORT_DIRECTION_MASK) == SORT_DESCENDING)
            statement.append(" DESC");
        return statement.toString();
    }


    public static Mailbox.MailboxData getFoldersAndTags(Mailbox mbox, Map<Integer, MailItem.UnderlyingData> folderData, Map<Integer, MailItem.UnderlyingData> tagData) throws ServiceException {
        boolean fetchFolders = folderData != null;
        boolean fetchTags    = tagData != null;
        if (!fetchFolders && !fetchTags)
            return null;
        Connection conn = mbox.getOperationConnection();

        Mailbox.MailboxData mbd = new Mailbox.MailboxData();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String table = getMailItemTableName(mbox.getId(), "mi");

            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + table + " WHERE type IN " + FOLDER_AND_TAG_TYPES);
            rs = stmt.executeQuery();
            while (rs.next()) {
                UnderlyingData data = constructItem(rs);
                if (fetchFolders && MailItem.isAcceptableType(MailItem.TYPE_FOLDER, data.type))
                    folderData.put(data.id, data);
                else if (fetchTags && MailItem.isAcceptableType(MailItem.TYPE_TAG, data.type))
                    tagData.put(data.id, data);
            }
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("SELECT folder_id, type, tags, COUNT(*), SUM(unread), SUM(size)" +
                    " FROM " + table + " GROUP BY folder_id, type, tags");
            rs = stmt.executeQuery();
            while (rs.next()) {
                byte type  = rs.getByte(2);
                int count  = rs.getInt(4);
                int unread = rs.getInt(5);
                long size  = rs.getLong(6);

                if (type == MailItem.TYPE_CONTACT)
                    mbd.contacts += count;
                mbd.size += size;

                if (fetchFolders) {
	                UnderlyingData data = folderData.get(rs.getInt(1));
	                assert(data != null);
                    data.size += size;
	                data.unreadCount += unread;
//                    if (type == MailItem.TYPE_MESSAGE || type == MailItem.TYPE_INVITE)
	                if (type == MailItem.TYPE_MESSAGE)
                        data.messageCount += count;
                }

                if (fetchTags) {
                    long tags = rs.getLong(3);
                    for (int i = 0; tags != 0 && i < MailItem.MAX_TAG_COUNT - 1; i++)
                        if ((tags & (1L << i)) != 0) {
        	                UnderlyingData data = tagData.get(i + MailItem.TAG_ID_OFFSET);
        	                if (data != null)
        	                    data.unreadCount += unread;
                            // could track cumulative size if desired...
                            tags &= ~(1L << i);
                        }
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

    public static List<UnderlyingData> getByType(Mailbox mbox, byte type, byte sort) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
        Connection conn = mbox.getOperationConnection();

        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox.getId(), " mi") +
                    " WHERE type IN " + typeConstraint(type) + sortQuery(sort));
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
    	return getByParent(parent, DEFAULT_SORT_ORDER);
    }
    public static List<UnderlyingData> getByParent(MailItem parent, byte sort) throws ServiceException {
        Mailbox mbox = parent.getMailbox();
        Connection conn = mbox.getOperationConnection();
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(parent.getMailboxId(), " mi") +
                    " WHERE parent_id = ? " + sortQuery(sort));
            stmt.setInt(1, parent.getId());
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
        Connection conn = mbox.getOperationConnection();
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String relation;
            if (relativeTo instanceof VirtualConversation)  relation = " id = ?";
            else if (relativeTo instanceof Conversation)    relation = " parent_id = ?";
            else if (relativeTo instanceof Folder)          relation = " folder_id = ?";
            else if (relativeTo instanceof Flag)            relation = " flags & ?";
            else if (relativeTo instanceof Tag)             relation = " tags & ?";
            else                                            relation = " id = ?";

            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(relativeTo.getMailboxId(), " mi") +
                    " WHERE unread AND " + relation);
            if (relativeTo instanceof Tag)
                stmt.setLong(1, ((Tag) relativeTo).getBitmask());
            else if (relativeTo instanceof VirtualConversation)
                stmt.setInt(1, ((VirtualConversation) relativeTo).getMessageId());
            else
                stmt.setInt(1, relativeTo.getId());
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
        Connection conn = mbox.getOperationConnection();

        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(folder.getMailboxId(), " mi") +
                    " WHERE folder_id = ? AND type IN " + typeConstraint(type) +
                    sortQuery(sort));
            stmt.setInt(1, folder.getId());
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
                    " FROM " + getMailItemTableName(mbox.getId(), "mi") +
                    " WHERE id = ?");
            stmt.setInt(1, id);
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
        for (int i = 0; i < ids.size(); i += DbUtil.IN_CLAUSE_BATCH_SIZE)
            try {
                int count = Math.min(DbUtil.IN_CLAUSE_BATCH_SIZE, ids.size() - i);
                stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                        " FROM " + getMailItemTableName(mbox.getId(), "mi") +
                        " WHERE id IN " + DbUtil.suitableNumberOfVariables(count));
                for (int index = i, attr = 1; index < i + count; index++)
                    stmt.setInt(attr++, it.next());

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
                throw ServiceException.FAILURE("fetching items: " + ids, e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
            }

        if (!conversations.isEmpty())
            completeConversations(mbox, conversations);
        return result;
    }

    public static UnderlyingData getByHash(Mailbox mbox, String hash) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox.getId(), "mi") + ", " +
                               getConversationTableName(mbox.getId(), "oc") +
                    " WHERE oc.hash = ? AND oc.conv_id = mi.id");
            stmt.setString(1, hash);
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

    public static List<UnderlyingData> getModifiedItems(Mailbox mbox, byte type, long lastSync) throws ServiceException {
        if (Mailbox.isCachedType(type))
            throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);

        Connection conn = mbox.getOperationConnection();
        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox.getId(), "mi") +
                    " WHERE type IN " + typeConstraint(type) +
                    " AND mi.mod_metadata > ? ORDER BY mi.mod_metadata");
            stmt.setLong(1, lastSync);
            rs = stmt.executeQuery();
            while (rs.next())
                result.add(constructItem(rs));

            if (type == MailItem.TYPE_CONVERSATION)
                completeConversations(mbox, result);
            return result;
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
        
        for (int i = 0; i < convData.size(); i += DbUtil.IN_CLAUSE_BATCH_SIZE)
            try {
                int count = Math.min(DbUtil.IN_CLAUSE_BATCH_SIZE, convData.size() - i);
                String sql = "SELECT parent_id, id, unread, flags, tags" +
                             " FROM " + getMailItemTableName(mbox.getId()) +
                             " WHERE parent_id IN " + DbUtil.suitableNumberOfVariables(count) +
                             " ORDER BY parent_id";
                stmt = conn.prepareStatement(sql);
                for (int index = i, attr = 1; index < i + count; index++) {
                    UnderlyingData data = convData.get(index);
                    assert(data.type == MailItem.TYPE_CONVERSATION);
                    stmt.setInt(attr++, data.id);
                    conversations.put(data.id, data);
                }
                rs = stmt.executeQuery();

                int lastConvId = -1;
                boolean firstTime = true;
                List<Long> inheritedTags = new ArrayList<Long>();
                List<Integer> children = new ArrayList<Integer>();
                int unreadCount = 0;
                
                while (rs.next()) {
                    int convId = rs.getInt(1);
                    if (convId != lastConvId) {
                        // New conversation.  Update stats for the last one and reset counters.
                        if (!firstTime) {
                            // Update stats for the previous conversation
                            UnderlyingData data = conversations.get(lastConvId);
                            data.children      = StringUtil.join(",", children);
                            data.unreadCount   = unreadCount;
                            data.inheritedTags = StringUtil.join(",", inheritedTags);
                            data.messageCount  = children.size();
                        } else {
                            firstTime = false;
                        }
                        lastConvId = convId;
                        children.clear();
                        inheritedTags.clear();
                        unreadCount = 0;
                    }
                    
                    // Read next row
                    children.add(rs.getInt(2));
                    boolean unread = rs.getBoolean(3);
                    if (unread) {
                        unreadCount++;
                    }
                    inheritedTags.add(-rs.getLong(4));
                    inheritedTags.add(rs.getLong(5));
                }
                
                // Update the last conversation.
                UnderlyingData data = conversations.get(lastConvId);
                if (data != null) {
                    data.children      = StringUtil.join(",", children);
                    data.unreadCount   = unreadCount;
                    data.inheritedTags = StringUtil.join(",", inheritedTags);
                    data.messageCount  = children.size();
                } else {
                    // Data error: no messages found
                    StringBuilder msg = new StringBuilder("No messages found for conversations:");
                    for (UnderlyingData ud : convData) {
                        msg.append(' ').append(ud.id);
                    }
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

    private static final String LEAF_NODE_FIELDS = "id, size, type, unread, folder_id," +
                                                   " parent_id IS NULL, blob_digest IS NOT NULL," +
                                                   " mod_content, mod_metadata," +
                                                   " flags & " + Flag.FLAG_COPIED + ", index_id, volume_id";

    private static final int LEAF_CI_ID           = 1;
    private static final int LEAF_CI_SIZE         = 2;
    private static final int LEAF_CI_TYPE         = 3;
    private static final int LEAF_CI_IS_UNREAD    = 4;
    private static final int LEAF_CI_FOLDER_ID    = 5;
    private static final int LEAF_CI_IS_NOT_CHILD = 6;
    private static final int LEAF_CI_HAS_BLOB     = 7;
    private static final int LEAF_CI_MOD_CONTENT  = 8;
    private static final int LEAF_CI_MOD_METADATA = 9;
    private static final int LEAF_CI_IS_COPIED    = 10;
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
            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                    " FROM " + getMailItemTableName(folder) +
                    " WHERE folder_id = ? AND type NOT IN " + FOLDER_TYPES);
            stmt.setInt(1, folderId);
            rs = stmt.executeQuery();

            info.rootId = folderId;
            info.size   = 0;
            accumulateLeafNodes(info, mbox, rs);
            // make sure that the folder is in the list of deleted item ids
            info.itemIds.add(folderId);

            return info;
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("fetching list of items within item " + folder.getId(), e);
        } finally {
        	DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static PendingDelete getLeafNodes(Mailbox mbox, List<Folder> folders, int before, boolean globalMessages) throws ServiceException {
        PendingDelete info = new PendingDelete();

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String constraint;
            if (globalMessages)
                constraint = "date < ? AND type = " + MailItem.TYPE_MESSAGE;
            else
                constraint = "date < ? AND type NOT IN " + NON_SEARCHABLE_TYPES +
                             " AND folder_id IN" + DbUtil.suitableNumberOfVariables(folders);

            stmt = conn.prepareStatement("SELECT " + LEAF_NODE_FIELDS +
                    " FROM " + getMailItemTableName(mbox) +
                    " WHERE " + constraint);
            int attr = 1;
            stmt.setInt(attr++, before);
            if (!globalMessages)
                for (Folder folder : folders)
                    stmt.setInt(attr++, folder.getId());
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

    public static class LocationCount {
        public int count;
        public long size;
        public LocationCount(int c, long sz)            { count = c;  size = sz; }
        public LocationCount increment(int c, long sz)  { count += c;  size += sz;  return this; }
    }

    private static PendingDelete accumulateLeafNodes(PendingDelete info, Mailbox mbox, ResultSet rs) throws SQLException, ServiceException {
        StoreManager sm = StoreManager.getInstance();

        while (rs.next()) {
            // first check to make sure we don't have a modify conflict
            int revision = rs.getInt(LEAF_CI_MOD_CONTENT);
            int modMetadata = rs.getInt(LEAF_CI_MOD_METADATA);
            if (!mbox.checkItemChangeID(modMetadata, revision)) {
                info.incomplete = true;
                continue;
            }

            int id = rs.getInt(LEAF_CI_ID);
            int size = rs.getInt(LEAF_CI_SIZE);
            Integer item = new Integer(id);
            info.itemIds.add(item);

            info.size += size;
            if (rs.getBoolean(LEAF_CI_IS_UNREAD))
            	info.unreadIds.add(item);
            int isMessage = 0;
            switch (rs.getByte(LEAF_CI_TYPE)) {
                case MailItem.TYPE_CONTACT:  info.contacts++;  break;
                case MailItem.TYPE_MESSAGE:  isMessage = 1;    break;
            }
            // detect deleted virtual conversations
            if (isMessage > 0 && rs.getBoolean(LEAF_CI_IS_NOT_CHILD))
                info.itemIds.add(-id);

            if (isMessage > 0 || size > 0) {
                if (info.messages == null)
                    info.messages = new HashMap<Integer, DbMailItem.LocationCount>();
                Integer folderID = new Integer(rs.getInt(LEAF_CI_FOLDER_ID));
                LocationCount count = info.messages.get(folderID);
                if (count == null)
                    info.messages.put(folderID, new LocationCount(isMessage, size));
                else
                    count.increment(isMessage, size);
            }

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

            Integer indexId = new Integer(rs.getInt(LEAF_CI_INDEX_ID));
            boolean indexed = !rs.wasNull();
            if (indexed) {
                if (info.sharedIndex == null)
                    info.sharedIndex = new HashSet<Integer>();
                boolean shared = rs.getBoolean(LEAF_CI_IS_COPIED);
                if (!shared)  info.indexIds.add(indexId);
                else          info.sharedIndex.add(indexId);
            }
        }

        return info;
    }

    public static void resolveSharedIndex(Mailbox mbox, PendingDelete info) throws ServiceException {
        if (info.sharedIndex == null || info.sharedIndex.isEmpty())
            return;
        Connection conn = mbox.getOperationConnection();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT index_id FROM " + getMailItemTableName(mbox) +
                    " WHERE index_id IN " + DbUtil.suitableNumberOfVariables(info.sharedIndex));
            int attr = 1;
            for (int id : info.sharedIndex)
            	stmt.setInt(attr++, id);
            rs = stmt.executeQuery();

            while (rs.next())
                info.sharedIndex.remove(rs.getInt(1));
            info.indexIds.addAll(info.sharedIndex);
            info.sharedIndex.clear();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("resolving shared index entries: " + info.rootId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * @author tim
     *
     * A class which encapsulates all of the constraints we can do on a mailbox search
     * 
     * "required" entries must be set, or you won't get what you want
     * 
     * "optional" entries are ignored if the default value is passed 
     */
    public static class SearchConstraints {
        public static class Range {
            public boolean negated = false;
            public long lowest = -1;
            public long highest = -1;
            
            public boolean equals(Object o) {
                Range other = (Range) o;
                return ((other.negated == negated) && (other.lowest == lowest) && (other.highest == highest));
            }

            boolean isValid()  { return lowest > 0 || highest > 0; }
        }
        
        public int mailboxId = 0;               /* required */
        public byte sort;                       /* required */

        public Tag[] tags = null;               /* optional */
        public Tag[] excludeTags = null;        /* optional */
        public Boolean hasTags = null;          /* optional */
        public Folder[] folders = null;         /* optional */
        public Folder[] excludeFolders = null;  /* optional */
        public int convId = -1;                 /* optional */
        public int[] prohibitedConvIds = null;  /* optional */
        public int[] itemIds = null;            /* optional */
        public int[] prohibitedItemIds = null;  /* optional */
        public int[] indexIds = null;           /* optional */
        public byte[] types = null;             /* optional */
        public byte[] excludeTypes = null;      /* optional */
        public Range[] dates = null;            /* optional */
        public Range[] modified = null;         /* optional */
        public Range[] sizes = null;            /* optional */
        public int offset = -1;                 /* optional */
        public int limit = -1;                  /* optional */

        boolean automaticEmptySet() {
            // Check for tags and folders that are both included and excluded.
            Set<Integer> s = new HashSet<Integer>();
            addIdsToSet(s, tags);
            addIdsToSet(s, folders);
            assert(!(setContainsAnyId(s, excludeTags) || setContainsAnyId(s, excludeFolders)));
//                return true;
            if (hasTags == Boolean.FALSE && tags != null && tags.length != 0)
                return true;
            
            // lots more optimizing we could do here...
            if (dates != null)
                for (int i = 0; i < dates.length; i++)
                    if (dates[i].lowest < -1 && dates[i].negated)
                        return true;
                    else if (dates[i].highest < -1 && !dates[i].negated)
                        return true;
            if (modified != null)
                for (int i = 0; i < modified.length; i++)
                    if (modified[i].lowest < -1 && modified[i].negated)
                        return true;
                    else if (modified[i].highest < -1 && !modified[i].negated)
                        return true;
            return false;
        }

        void checkDates() {
            dates    = checkIntervals(dates);
            modified = checkIntervals(modified);
        }
        Range[] checkIntervals(Range[] intervals) {
            if (intervals == null)
                return intervals;
            HashSet<Range> badDates = new HashSet<Range>();
            for (Range range : intervals)
                if (!range.isValid())
                    badDates.add(range);
            if (badDates.size() == 0)
                return intervals;
            else if (badDates.size() == intervals.length)
                intervals = null;
            else {
                Range[] fixedDates = new Range[intervals.length - badDates.size()];
                for (int i = 0, j = 0; i < intervals.length; i++)
                    if (!badDates.contains(intervals[i]))
                        fixedDates[j++] = intervals[i];
                intervals = fixedDates;
            }
            return intervals;
        }

        private void addIdsToSet(Set<Integer> s, MailItem[] items) {
            if (items != null)
                for (MailItem item : items)
                    s.add(item.getId());
        }
        
        private boolean setContainsAnyId(Set<Integer> s, MailItem[] items) {
            if (items != null)
                for (MailItem item : items)
                    if (s.contains(item.getId()))
                        return true;
            return false;
        }
    }

    public static final class SearchResult {
        public int    id;
        public int    indexId;
        public byte   type;
        public Object sortkey;
        public UnderlyingData data; // OPTIONAL

        public static SearchResult createResult(ResultSet rs, byte sort) throws SQLException {
            return createResult(rs, sort, false);
        }
        public static SearchResult createResult(ResultSet rs, byte sort, boolean fullRow) throws SQLException {
            SearchResult result = new SearchResult();
            result.id      = rs.getInt(1);
            result.indexId = rs.getInt(2);
            result.type    = rs.getByte(3);
            if ((sort & SORT_FIELD_MASK) == SORT_BY_SUBJECT || (sort & SORT_FIELD_MASK) == SORT_BY_SENDER)
                result.sortkey = rs.getString(4);
            else
                result.sortkey = new Long(rs.getInt(4) * 1000L);

            if (fullRow)
                result.data = constructItem(rs, 4);
            return result;
        }

        public String toString() {
            return sortkey + " => (" + id + "," + type + ")";
        }
        
        public int hashCode() {
            return id;
        }
        
        public boolean equals(Object obj) {
            SearchResult other = (SearchResult) obj;
            return other.id == id;
        }
    }

    public static Collection<SearchResult> search(Connection conn, int mailboxId,
            Tag[] tags, Tag[] excludeTags, Folder[] folders, Folder[] excludeFolders,
            byte type, byte sort)
    throws ServiceException {
        SearchConstraints c = new SearchConstraints();
        c.mailboxId = mailboxId;
        c.tags      = tags;
        c.excludeTags = excludeTags;
        c.folders   = folders;
        c.excludeFolders = excludeFolders;
        c.types     = new byte[1];
        c.types[0]  = type;
        c.sort      = sort;
        return search(conn, c);
    }

    public static Collection<SearchResult> search(Connection conn, SearchConstraints c) throws ServiceException {
        return search(new ArrayList<SearchResult>(), conn, c, false);
    }
    public static Collection<SearchResult> search(Connection conn, SearchConstraints c, boolean fullRows) throws ServiceException {
        return search(new ArrayList<SearchResult>(), conn, c, fullRows);
    }
    public static Collection<SearchResult> search(Collection<SearchResult> result, Connection conn, SearchConstraints c) throws ServiceException {
        return search(result, conn, c, false);
    }
    public static Collection<SearchResult> search(Collection<SearchResult> result, Connection conn, SearchConstraints c, boolean fullRows) throws ServiceException {
        if (c.automaticEmptySet())
            return result;
        c.checkDates();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            /*
             * SELECT id,date FROM mail_item mi WHERE mi.acccount_id = ? AND type = ? AND tags & ? = ? AND flags & ? = ?
             *    (AND folder_id [NOT] IN (?,?,?)) (AND date > ?) (AND date < ?) (AND mod_metadata > ?) (AND mod_metadata < ?)
             *    ORDER BY date|subject|sender (DESC)? LIMIT ?, ?
             */

            // Determine the set of matching tags
            Set<Long> searchTagsets = null;
            Set<Long> searchFlagsets = null;
            Boolean unread = null;
            
            if (!ArrayUtil.isEmpty(c.tags) || !ArrayUtil.isEmpty(c.excludeTags)) {
                int setFlagMask = 0;
                long setTagMask = 0;
                for (int i = 0; c.tags != null && i < c.tags.length; i++)
                    if (c.tags[i].getId() == Flag.ID_FLAG_UNREAD) {
                        unread = Boolean.TRUE; 
                    } else if (c.tags[i] instanceof Flag) {
                        setFlagMask |= c.tags[i].getBitmask();
                    } else {
                        setTagMask |= c.tags[i].getBitmask();
                    }
                int flagMask = setFlagMask;
                long tagMask = setTagMask;
                
                for (int i = 0; c.excludeTags != null && i < c.excludeTags.length; i++)
                    if (c.excludeTags[i].getId() == Flag.ID_FLAG_UNREAD) {
                        unread = Boolean.FALSE;
                    } else if (c.excludeTags[i] instanceof Flag) {
                        flagMask |= c.excludeTags[i].getBitmask();
                    } else {
                        tagMask |= c.excludeTags[i].getBitmask();
                    }
                
                TagsetCache tcFlags = getFlagsetCache(conn, c.mailboxId);
                TagsetCache tcTags  = getTagsetCache(conn, c.mailboxId);
                if (setTagMask != 0 || tagMask != 0) {
                    searchTagsets = tcTags.getMatchingTagsets(tagMask, setTagMask);
                    if (searchTagsets.size() == 0) {
                        // No items match the specified tags
                        return result;
                    }
                }
                if (setFlagMask != 0 || flagMask != 0) {
                    searchFlagsets = tcFlags.getMatchingTagsets(flagMask, setFlagMask);
                    if (searchFlagsets.size() == 0) {
                        // No items match the specified flags
                        return result;
                    }
                }
            }
            
            // Assemble the search query
            StringBuffer statement = new StringBuffer("SELECT id, index_id, type, " + sortField(c.sort));
            if (fullRows)
                statement.append(", " + DB_FIELDS);
            statement.append(" FROM " + getMailItemTableName(c.mailboxId, "mi"));
            statement.append(" WHERE ");
            if (c.types == null)
                statement.append("type NOT IN " + NON_SEARCHABLE_TYPES);
            else
                statement.append("type IN").append(DbUtil.suitableNumberOfVariables(c.types));

            if (c.excludeTypes != null)
                statement.append(" AND type NOT IN").append(DbUtil.suitableNumberOfVariables(c.excludeTypes));

            if (c.hasTags != null)
                statement.append(" AND tags ").append(c.hasTags.booleanValue() ? "!= 0" : "= 0");
            if (searchTagsets != null)
                statement.append(" AND tags IN").append(DbUtil.suitableNumberOfVariables(searchTagsets));
            if (searchFlagsets != null)
                statement.append(" AND flags IN").append(DbUtil.suitableNumberOfVariables(searchFlagsets));
            if (unread != null)
                statement.append(" AND unread = ?");

            Folder[] targetFolders = (c.folders != null && c.folders.length > 0) ? c.folders : c.excludeFolders;
            if (targetFolders != null && targetFolders.length > 0)
                statement.append(" AND folder_id").append(targetFolders == c.folders ? "" : " NOT").append(" IN").append(DbUtil.suitableNumberOfVariables(targetFolders));

            if (c.convId > 0)
                statement.append(" AND parent_id = ?");
            else if (c.prohibitedConvIds != null)
                statement.append(" AND parent_id NOT IN").append(DbUtil.suitableNumberOfVariables(c.prohibitedConvIds));

            if (c.itemIds != null)
                statement.append(" AND id IN").append(DbUtil.suitableNumberOfVariables(c.itemIds));
            if (c.prohibitedItemIds != null)
                statement.append(" AND id NOT IN").append(DbUtil.suitableNumberOfVariables(c.prohibitedItemIds));

            if (c.indexIds != null)
                statement.append(" AND index_id IN").append(DbUtil.suitableNumberOfVariables(c.indexIds));

            if (c.dates != null) {
                for (int i = 0; i < c.dates.length; i++) {
                    statement.append(" AND ");
                    if (c.dates[i].negated)
                        statement.append(" NOT ");
                    statement.append('(');
                    boolean hasOne = false;
                    if (c.dates[i].lowest > 0) { 
                        statement.append(" date > ?");
                        hasOne = true;
                    }
                    if (c.dates[i].highest > 0) {
                        if (hasOne)
                            statement.append(" AND");
                        statement.append(" date < ?");
                    }
                    statement.append(')');
                }
            }
            if (c.modified != null) {
                for (int i = 0; i < c.modified.length; i++) {
                    statement.append(" AND ");
                    if (c.modified[i].negated)
                        statement.append(" NOT ");
                    statement.append('(');
                    boolean hasOne = false;
                    if (c.modified[i].lowest > 0) { 
                        statement.append(" mod_metadata > ?");
                        hasOne = true;
                    }
                    if (c.modified[i].highest > 0) {
                        if (hasOne)
                            statement.append(" AND");
                        statement.append(" mod_metadata < ?");
                    }
                    statement.append(')');
                }
            }
            if (c.sizes != null) {
                for (int i = 0; i < c.sizes.length; i++) {
                    statement.append(" AND ");
                    if (c.sizes[i].negated)
                        statement.append(" NOT ");
                    statement.append('(');
                    boolean hasOne = false;
                    if (c.sizes[i].lowest >= 0) { 
                        statement.append(" size > ?");
                        hasOne = true;
                    }
                    if (c.sizes[i].highest >= 0) {
                        if (hasOne)
                            statement.append(" AND");
                        statement.append(" size < ?");
                    }
                    statement.append(')');
                }
            }

            statement.append(sortQuery(c.sort));

            if (c.offset >= 0 && c.limit >= 0)
                statement.append(" LIMIT ?, ?");

            stmt = conn.prepareStatement(statement.toString());
            int param = 1;
            if (c.types != null)
                for (byte type : c.types)
                    stmt.setByte(param++, type);
            if (c.excludeTypes != null && c.types != null)
                for (byte type : c.excludeTypes)
                    stmt.setByte(param++, type);
            if (searchTagsets != null)
                for (long tagset : searchTagsets)
                    stmt.setLong(param++, tagset);
            if (searchFlagsets != null)
                for (long flagset : searchFlagsets)
                    stmt.setLong(param++, flagset);
            if (unread != null)
                stmt.setBoolean(param++, unread.booleanValue());
            if (targetFolders != null)
                for (Folder folder : targetFolders)
                    stmt.setInt(param++, folder.getId());
            if (c.convId > 0)
                stmt.setInt(param++, c.convId);
            else if (c.prohibitedConvIds != null)
                for (int id : c.prohibitedConvIds)
                    stmt.setInt(param++, id);
            if (c.itemIds != null)
                for (int id : c.itemIds)
                    stmt.setInt(param++, id);
            if (c.prohibitedItemIds != null)
                for (int id : c.prohibitedItemIds)
                    stmt.setInt(param++, id);
            if (c.indexIds != null)
                for (int id : c.indexIds)
                    stmt.setInt(param++, id);
            if (c.dates != null)
                for (SearchConstraints.Range date : c.dates) {
                    if (date.lowest > 0)
                        stmt.setInt(param++, (int) Math.min(date.lowest / 1000, Integer.MAX_VALUE));
                    if (date.highest > 0)
                        stmt.setInt(param++, (int) Math.min(date.highest / 1000, Integer.MAX_VALUE));
                }
            if (c.sizes != null)
                for (SearchConstraints.Range size : c.sizes) {
                    if (size.lowest >= 0)
                        stmt.setInt(param++, (int) size.lowest);
                    if (size.highest >= 0)
                        stmt.setInt(param++, (int) size.highest);
                }
            if (c.modified != null)
                for (SearchConstraints.Range modified : c.modified) {
                    if (modified.lowest > 0)
                        stmt.setLong(param++, modified.lowest);
                    if (modified.highest > 0)
                        stmt.setLong(param++, modified.highest);
                }

            // FIXME: include COLLATION for sender/subject sort

            if (c.offset >= 0 && c.limit >= 0) {
                stmt.setInt(param++, c.offset);
                stmt.setInt(param++, c.limit);
            }
            
            if (sLog.isDebugEnabled())
                sLog.debug("SQL: " + statement);
            
            rs = stmt.executeQuery();
            while (rs.next())
                result.add(SearchResult.createResult(rs, c.sort, fullRows));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching search metadata", e);
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

        ArrayList<SearchResult> result = new ArrayList<SearchResult>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id, index_id, type, date FROM " + getMailItemTableName(folder) +
                    " WHERE type = ? AND folder_id = ?" +
            		" ORDER BY date" + (descending ? " DESC" : ""));
            stmt.setByte(1, type);
            stmt.setInt(2, folder.getId());
            rs = stmt.executeQuery();
            while (rs.next())
            	result.add(SearchResult.createResult(rs, SORT_BY_DATE));
            return result;
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("fetching item list for folder " + folder.getId(), e);
        } finally {
        	DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    // these columns are specified by DB_FIELDS, below
    private static final int CI_ID          = 1;
    private static final int CI_TYPE        = 2;
    private static final int CI_PARENT_ID   = 3;
    private static final int CI_FOLDER_ID   = 4;
    private static final int CI_INDEX_ID    = 5;
    private static final int CI_DATE        = 6;
    private static final int CI_SIZE        = 7;
    private static final int CI_VOLUME_ID   = 8;
    private static final int CI_BLOB_DIGEST = 9;
    private static final int CI_UNREAD      = 10;
    private static final int CI_FLAGS       = 11;
    private static final int CI_TAGS        = 12;
//    private static final int CI_SENDER      = 13;
    private static final int CI_SUBJECT     = 13;
    private static final int CI_METADATA    = 14;
    private static final int CI_MODIFIED    = 15;
    private static final int CI_MODIFY_DATE = 16;
    private static final int CI_SAVED       = 17;

    private static final String DB_FIELDS = "mi.id, mi.type, mi.parent_id, mi.folder_id, mi.index_id, mi.date, " +
                                            "mi.size, mi.volume_id, mi.blob_digest, mi.unread, mi.flags, mi.tags, " +
                                            "mi.subject, mi.metadata, mi.mod_metadata, mi.change_date, mi.mod_content";

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
        data.date        = rs.getInt(CI_DATE + offset);
        data.size        = rs.getInt(CI_SIZE + offset);
        data.volumeId    = rs.getShort(CI_VOLUME_ID + offset);
        if (rs.wasNull())
            data.volumeId = -1;
        data.blobDigest  = rs.getString(CI_BLOB_DIGEST + offset);
        data.unreadCount = rs.getInt(CI_UNREAD + offset);
        data.flags       = rs.getInt(CI_FLAGS + offset);
        data.tags        = rs.getLong(CI_TAGS + offset);
        data.subject     = rs.getString(CI_SUBJECT + offset);
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


    //////////////////////////////////////
    // INVITE STUFF BELOW HERE!
    //////////////////////////////////////
    public static UnderlyingData getAppointment(Mailbox mbox, String uid)
    throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT " + DB_FIELDS +
                    " FROM " + getMailItemTableName(mbox.getId(), "mi") + ", " + 
                               getAppointmentTableName(mbox.getId(), " appt") +
                    " WHERE mi.type = " + MailItem.TYPE_APPOINTMENT +
                    " AND appt.uid = ? AND mi.id = appt.item_id" +
                    " GROUP BY mi.id";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, uid);
            rs = stmt.executeQuery();
            
            if (rs.next())
                return constructItem(rs);
            return null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching appointments for mailbox " + mbox.getId(), e);
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
    public static List<UnderlyingData> getAppointments(Mailbox mbox, long start, long end, 
                                                       int folderId, int[] excludeFolderIds) 
    throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean folderSpecified = folderId != Mailbox.ID_AUTO_INCREMENT;

            String excludeFolderPart = "";
            if (excludeFolderIds != null) 
                excludeFolderPart = "AND folder_id NOT IN" + DbUtil.suitableNumberOfVariables(excludeFolderIds);
            
            stmt = conn.prepareStatement("SELECT " + DB_FIELDS +
                     " FROM " + getMailItemTableName(mbox.getId(), "mi") + ", " + 
                         getAppointmentTableName(mbox.getId(), " appt") +
                     " WHERE mi.type = " + MailItem.TYPE_APPOINTMENT +
                     " AND mi.id = appt.item_id" +
                     " AND appt.start_time < ? AND appt.end_time > ?" +
                     (folderSpecified ? " AND folder_id = ?" : "") +
                     excludeFolderPart +
                     " GROUP BY mi.id");
            
            int param = 1;
            stmt.setTimestamp(param++, new Timestamp(end));
            stmt.setTimestamp(param++, new Timestamp(start));
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
            throw ServiceException.FAILURE("fetching appointments for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
    
    public static void addToAppointmentTable(Appointment appt) throws ServiceException {
        Mailbox mbox = appt.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            Timestamp startTs = new Timestamp(appt.getStartTime());
            
            long end = appt.getEndTime();
            Timestamp endTs;
            if (end <= 0) {
                endTs = new Timestamp(MAX_DATE);
            } else {
                endTs = new Timestamp(end);
            }
            
            stmt = conn.prepareStatement("INSERT INTO " +
                    getAppointmentTableName(mbox.getId()) +
                    " (uid, item_id, start_time, end_time)" +
                    " VALUES (?, ?, ?, ?)");
            stmt.setString(1, appt.getUid());
            stmt.setInt(2, appt.getId());
            stmt.setTimestamp(3, startTs);
            stmt.setTimestamp(4, endTs);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing invite to appt table: UID=" + appt.getUid(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static long MAX_DATE = new GregorianCalendar(9999, 1, 1).getTimeInMillis();
    
    public static void updateInAppointmentTable(Appointment appt) throws ServiceException {
        Mailbox mbox = appt.getMailbox();
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            Timestamp startTs = new Timestamp(appt.getStartTime());
            
            long end = appt.getEndTime();
            Timestamp endTs;
            if (end <= 0) {
                endTs = new Timestamp(MAX_DATE);
            } else {
                endTs = new Timestamp(end);
            }
            
            stmt = conn.prepareStatement("INSERT INTO " +
                    getAppointmentTableName(mbox.getId()) +
                    " (uid, item_id, start_time, end_time)" +
                    " VALUES (?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE uid = ?, item_id = ?, start_time = ?, end_time = ?");
            stmt.setString(1, appt.getUid());
            stmt.setInt(2, appt.getId());
            stmt.setTimestamp(3, startTs);
            stmt.setTimestamp(4, endTs);
            
            stmt.setString(5, appt.getUid());
            stmt.setInt(6, appt.getId());
            stmt.setTimestamp(7, startTs);
            stmt.setTimestamp(8, endTs);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing invite to appt table" + appt.getUid(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }


    /**
     * Returns the name of the table that stores {@link MailItem} data.  The table name is qualified
     * by the name of the database (e.g. <code>mailbox1.mail_item</code>).
     */
    public static String getMailItemTableName(Mailbox mbox) {
        return getMailItemTableName(mbox.getId());
    }
    public static String getMailItemTableName(MailItem item) {
        return getMailItemTableName(item.getMailboxId());
    }
    public static String getMailItemTableName(int mailboxId) {
        return DbMailbox.getDatabaseName(mailboxId) + ".mail_item";
    }
    public static String getMailItemTableName(int mailboxId, String alias) {
        return DbMailbox.getDatabaseName(mailboxId) + ".mail_item " + alias;
    }

    /**
     * Returns the name of the table that stores {@link Appointment} data.  The table name is qualified
     * by the name of the database (e.g. <code>mailbox1.appointment</code>).
     */
    public static String getAppointmentTableName(int mailboxId) {
        return DbMailbox.getDatabaseName(mailboxId) + ".appointment";
    }
    public static String getAppointmentTableName(int mailboxId, String alias) {
        return DbMailbox.getDatabaseName(mailboxId) + ".appointment " + alias;
    }

    /**
     * Returns the name of the table that maps subject hashes to {@link Conversation} ids.  The table 
     * name is qualified by the name of the database (e.g. <code>mailbox1.open_conversation</code>).
     */
    public static String getConversationTableName(int mailboxId) {
        return DbMailbox.getDatabaseName(mailboxId) + ".open_conversation";
    }
    public static String getConversationTableName(int mailboxId, String alias) {
        return DbMailbox.getDatabaseName(mailboxId) + ".open_conversation " + alias;
    }

    /**
     * Returns the name of the table that stores data on deleted items for the purpose of sync.
     * The table name is qualified by the name of the database (e.g. <code>mailbox1.tombstone</code>).
     */
    public static String getTombstoneTableName(int mailboxId) {
        return DbMailbox.getDatabaseName(mailboxId) + ".tombstone";
    }
    
    private static boolean areTagsetsLoaded(int mailboxId) {
        synchronized(sTagsetCache) {
            return sTagsetCache.containsKey(new Integer(mailboxId));
        }
    }

    private static TagsetCache getTagsetCache(Connection conn, int mailboxId)
    throws ServiceException {
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
            tagsets.addTagsets(DbMailbox.getDistinctTagsets(conn, mailboxId));
        }
        
        synchronized (sTagsetCache) {
            sTagsetCache.put(id, tagsets);
        }
        
        return tagsets;
    }

    private static boolean areFlagsetsLoaded(int mailboxId) {
        synchronized(sFlagsetCache) {
            return sFlagsetCache.containsKey(new Integer(mailboxId));
        }
    }

    private static TagsetCache getFlagsetCache(Connection conn, int mailboxId)
    throws ServiceException {
        Integer id = new Integer(mailboxId);
        TagsetCache flagsets = null;

        synchronized (sFlagsetCache) {
            flagsets = sFlagsetCache.get(id);
        }

        // All access to a mailbox is synchronized, so we can initialize
        // the flagset cache for a single mailbox outside the
        // synchronized block.
        if (flagsets == null) {
            ZimbraLog.cache.info("Loading tagset cache");
            flagsets = new TagsetCache("Mailbox " + mailboxId + " flags");
            flagsets.addTagsets(DbMailbox.getDistinctFlagsets(conn, mailboxId));
        }
        
        synchronized (sFlagsetCache) {
            sFlagsetCache.put(id, flagsets);
        }
        
        return flagsets;
    }
}
