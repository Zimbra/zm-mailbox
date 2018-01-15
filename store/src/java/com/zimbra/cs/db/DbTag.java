/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.mailbox.MailboxLock;
import junit.framework.Assert;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.PendingDelete;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.RetentionPolicyManager;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.soap.mail.type.RetentionPolicy;

public final class DbTag {
    public static final String TABLE_TAG = "tag";
    public static final String TABLE_TAGGED_ITEM = "tagged_item";

    private static final String TAG_FIELDS = "id, name, color, item_count, unread, listed, sequence, policy";

    private DbTag() {
    }

    private static UnderlyingData asUnderlyingData(ResultSet rs) throws SQLException, ServiceException {
        UnderlyingData data = new UnderlyingData();
        data.id = rs.getInt(1);
        data.type = MailItem.Type.TAG.toByte();
        data.folderId = Mailbox.ID_FOLDER_TAGS;
        data.name = rs.getString(2);
        data.size = rs.getInt(4);
        data.unreadCount = rs.getInt(5);

        boolean listed = rs.getBoolean(6);
        String policy = rs.getString(8);
        RetentionPolicy rp = policy == null ? null : RetentionPolicyManager.retentionPolicyFromMetadata(new Metadata(policy), true);
        long c = rs.getLong(3);
        Color color = rs.wasNull() ? null : Color.fromMetadata(c);
        // FIXME: if Tag weren't a subclass of MailItem, this step wouldn't be necessary
        data.metadata = Tag.encodeMetadata(color, 1, 1, rp, listed);

        data.modMetadata = rs.getInt(7);
        data.modContent = data.modMetadata;
        return data;
    }

    private static final String TAG_SEPARATOR = "\0";
    private static final Joiner TAG_JOINER = Joiner.on(TAG_SEPARATOR).skipNulls();

    public static String serializeTags(String[] tags) {
        if (tags == null || tags.length == 0) {
            return null;
        }

        StringBuilder encoded = TAG_JOINER.appendTo(new StringBuilder(TAG_SEPARATOR), tags);
        return encoded.length() == 1 ? null : encoded.append(TAG_SEPARATOR).toString();
    }

    public static String[] deserializeTags(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        return encoded.substring(encoded.charAt(0) == '\0' ? 1 : 0).split(TAG_SEPARATOR);
    }

    private static String delimitTagName(String name) {
        return TAG_SEPARATOR + name + TAG_SEPARATOR;
    }

    static String tagLIKEPattern(String tagName) {
        return '%' + delimitTagName(tagName).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + '%';
    }

    public static void createTag(Mailbox mbox, UnderlyingData data, Color color, boolean listed) throws ServiceException {
        createTag(mbox.getOperationConnection(), mbox, data, color, listed);
    }

    public static void createTag(DbConnection conn, Mailbox mbox, UnderlyingData data, Color color, boolean listed) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getTagTableName(mbox) +
                    " (" + DbMailItem.MAILBOX_ID + " id, name, color, listed, sequence)" +
                    " VALUES (" + DbMailItem.MAILBOX_ID_VALUE + "?, ?, ?, ?, ?)");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, data.id);
            stmt.setString(pos++, data.name);
            if (color != null) {
                stmt.setLong(pos++, color.getValue());
            } else {
                stmt.setNull(pos++, Types.BIGINT);
            }
            stmt.setBoolean(pos++, listed);
            stmt.setInt(pos++, data.modMetadata);

            int num = stmt.executeUpdate();
            if (num != 1) {
                throw ServiceException.FAILURE("failed to create object", null);
            }
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(data.id, e);
            } else {
                throw ServiceException.FAILURE("Failed to create tag id=" + data.id + ",name=" + data.name, e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static UnderlyingData getTag(Mailbox mbox, String name) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + TAG_FIELDS + " FROM " + getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "name = ?");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, name);

            rs = stmt.executeQuery();
            return rs.next() ? asUnderlyingData(rs) : null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching tag " + name, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static List<UnderlyingData> getUnreadMessages(Tag tag) throws ServiceException {
        Mailbox mbox = tag.getMailbox();

        ArrayList<UnderlyingData> result = new ArrayList<UnderlyingData>();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "mi.mailbox_id = ti.mailbox_id AND ";
            stmt = conn.prepareStatement("SELECT " + DbMailItem.DB_FIELDS +
                    " FROM " + DbMailItem.getMailItemTableName(mbox, "mi") +
                    " INNER JOIN " + getTaggedItemTableName(mbox, "ti") + " ON " + mailboxesMatchAnd + "ti.item_id = mi.id" +
                    " WHERE " + inThisMailboxAnd("ti") + "type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES +
                    " AND unread > 0 AND ti.tag_id = ?");
            if (tag.getUnreadCount() > DbMailItem.RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tag.getId());

            rs = stmt.executeQuery();
            while (rs.next()) {
                UnderlyingData data = DbMailItem.constructItem(rs);
                if (Mailbox.isCachedType(MailItem.Type.of(data.type))) {
                    throw ServiceException.INVALID_REQUEST("folders and tags must be retrieved from cache", null);
                }
                result.add(data);
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching unread messages for tag " + tag.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    static boolean getAllTags(Mailbox mbox, DbMailItem.FolderTagMap tagData) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT " + TAG_FIELDS + " FROM " + getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id > 0");
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            while (rs.next()) {
                tagData.put(asUnderlyingData(rs), null);
            }
            return false;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching all tags for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static PendingDelete getImapDeleted(Mailbox mbox, Set<Folder> folders) throws ServiceException {
        if (folders != null && folders.isEmpty()) {
            return new PendingDelete();
        }

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "mi.mailbox_id = ti.mailbox_id AND ";
            String folderconstraint = folders == null ? "" : " AND " + DbUtil.whereIn("folder_id", folders.size());
            stmt = conn.prepareStatement("SELECT " + DbMailItem.LEAF_NODE_FIELDS +
                        " FROM " + DbMailItem.getMailItemTableName(mbox, "mi") +
                        " INNER JOIN " + getTaggedItemTableName(mbox, "ti") + " ON " + mailboxesMatchAnd + "ti.item_id = mi.id" +
                        " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id = " + Flag.ID_DELETED +
                        " AND type IN " + DbMailItem.IMAP_TYPES + folderconstraint);
            if (DbMailItem.getTotalFolderSize(folders) > DbMailItem.RESULTS_STREAMING_MIN_ROWS) {
                Db.getInstance().enableStreaming(stmt);
            }
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            if (folders != null) {
                for (Folder folder : folders) {
                    stmt.setInt(pos++, folder.getId());
                }
            }

            PendingDelete info = DbMailItem.accumulateDeletionInfo(mbox, stmt);
            stmt = null;
            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of \\Deleted items for purge", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    static void recalculateTagCounts(Mailbox mbox, Map<Integer, UnderlyingData> lookup) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "mi.mailbox_id = ti.mailbox_id AND ";
            stmt = conn.prepareStatement("SELECT ti.tag_id, COUNT(*), " + Db.clauseIFNULL("SUM(mi.unread)", "0") +
                    " FROM " + DbMailItem.getMailItemTableName(mbox, "mi") +
                    " INNER JOIN " + getTaggedItemTableName(mbox, "ti") + " ON " + mailboxesMatchAnd + "ti.item_id = mi.id" +
                    " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id > 0 AND type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES +
                    " AND " + Db.getInstance().bitAND("mi.flags", "" + Flag.BITMASK_DELETED) + " = 0" +
                    " GROUP BY ti.tag_id");
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            while (rs.next()) {
                int tagId  = rs.getInt(1);
                int count  = rs.getInt(2);
                int unread = rs.getInt(3);

                UnderlyingData data = lookup.get(tagId);
                if (data != null) {
                    data.unreadCount += unread;
                    data.size += count;
                } else {
                    ZimbraLog.mailbox.warn("inconsistent DB state: items with no corresponding tag (tag id %d)", tagId);
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("recalculating tag counts for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    static void updateTagReferences(Mailbox mbox, int itemId, MailItem.Type type, int flags, boolean unread, String[] tags) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getTaggedItemTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "item_id = ?");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("storing flag references in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }

        storeTagReferences(mbox, itemId, type, flags, unread);
        storeTagReferences(mbox, itemId, type, tags);
    }

    static void storeTagReferences(Mailbox mbox, int itemId, MailItem.Type type, int flags, boolean unread) throws ServiceException {
        if (!type.isLeafNode())
            return;

        if (unread) {
            flags |= Flag.BITMASK_UNREAD;
        } else if (flags == 0) {
            return;
        }

        List<Integer> flagIds = Lists.newArrayList();
        for (int tagId : Mailbox.REIFIED_FLAGS) {
            if ((flags & 1 << (-tagId - 1)) != 0) {
                flagIds.add(tagId);
            }
        }
        if (flagIds.isEmpty())
            return;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getTaggedItemTableName(mbox) +
                    "(" + DbMailItem.MAILBOX_ID + "tag_id, item_id)" +
                    " SELECT " + DbMailItem.MAILBOX_ID + "id, ? FROM " + getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + DbUtil.whereIn("id", flagIds.size()));
            int pos = 1;
            stmt.setInt(pos++, itemId);
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            for (int flagId : flagIds) {
                stmt.setInt(pos++, flagId);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("storing flag references in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    static void storeTagReferences(Mailbox mbox, int itemId, MailItem.Type type, String[] tags) throws ServiceException {
        if (!type.isLeafNode() || ArrayUtil.isEmpty(tags))
            return;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getTaggedItemTableName(mbox) +
                    "(" + DbMailItem.MAILBOX_ID + "tag_id, item_id)" +
                    " SELECT " + DbMailItem.MAILBOX_ID + "id, ? FROM " + getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + DbUtil.whereIn("name", tags.length));
            int pos = 1;
            stmt.setInt(pos++, itemId);
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            for (String tag : tags) {
                stmt.setString(pos++, tag);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("storing tag references in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void alterTag(Tag tag, List<Integer> itemIds, boolean add) throws ServiceException {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }

        Mailbox mbox = tag.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            boolean isFlag = tag instanceof Flag;
            boolean altersModseq = !isFlag || !((Flag) tag).isSystemFlag();

            String primaryUpdate, sanityCheckAnd;
            if (isFlag) {
                primaryUpdate = "flags = flags" + (add ? " + ?" : " - ?");
                sanityCheckAnd = Db.getInstance().bitAND("flags", "?") + (add ? " = 0" : " <> 0") + " AND ";
            } else {
                if (add) {
                    primaryUpdate = "tag_names = CASE WHEN tag_names IS NULL THEN ? ELSE " + Db.getInstance().concat("tag_names", "?") + " END";
                    sanityCheckAnd = "(tag_names IS NULL OR tag_names NOT LIKE ?) AND ";
                } else {
                    primaryUpdate = "tag_names = CASE tag_names WHEN ? THEN NULL ELSE REPLACE(tag_names, ?, '\0') END";
                    sanityCheckAnd = "";
                }
            }

            String updateChangeID = altersModseq ? ", mod_metadata = ?, change_date = ?" : "";

            for (int i = 0; i < itemIds.size(); i += Db.getINClauseBatchSize()) {
                int count = Math.min(Db.getINClauseBatchSize(), itemIds.size() - i);
                stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                        " SET " + primaryUpdate + updateChangeID +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + sanityCheckAnd + DbUtil.whereIn("id", count));
                int pos = 1;
                // primary update
                if (isFlag) {
                    stmt.setLong(pos++, ((Flag) tag).toBitmask());
                } else {
                    stmt.setString(pos++, delimitTagName(tag.getName()));
                    stmt.setString(pos++, delimitTagName(tag.getName()).substring(add ? 1 : 0));
                }
                // change ID update
                if (altersModseq) {
                    stmt.setInt(pos++, mbox.getOperationChangeID());
                    stmt.setInt(pos++, mbox.getOperationTimestamp());
                }
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                // sanity check
                if (isFlag) {
                    stmt.setLong(pos++, ((Flag) tag).toBitmask());
                } else if (add) {
                    stmt.setString(pos++, tagLIKEPattern(tag.getName()));
                }
                // item IDs
                for (int index = i; index < i + count; index++) {
                    stmt.setInt(pos++, itemIds.get(index));
                }
                stmt.executeUpdate();
                stmt.close();
                stmt = null;

                if (add) {
                    addTaggedItemEntries(mbox, tag.getId(), itemIds.subList(i, i + count));
                } else {
                    removeTaggedItemEntries(mbox, tag.getId(), itemIds.subList(i, i + count));
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating tag data for " + itemIds.size() + " items: " + DbMailItem.getIdListForLogging(itemIds), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    // FIXME: optimize into single query
    static void addTaggedItemEntries(Mailbox mbox, int tagId, List<Integer> itemIds) throws ServiceException {
        if (tagId < 0 && !Mailbox.REIFIED_FLAGS.contains(tagId))
            return;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;

        String command = Db.supports(Db.Capability.REPLACE_INTO) ? "REPLACE" : "INSERT";
        for (int itemId : itemIds) {
            try {
                stmt = conn.prepareStatement(command + " INTO " + getTaggedItemTableName(mbox) +
                        "(" + DbMailItem.MAILBOX_ID + "tag_id, item_id) VALUES (" + DbMailItem.MAILBOX_ID_VALUE + "?, ?)");
                int pos = 1;
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, tagId);
                stmt.setInt(pos++, itemId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("adding TAGGED_ITEM entries for tag: " + tagId + ", item: " + itemId, e);
            } finally {
                DbPool.closeStatement(stmt);
            }
        }
    }

    static void removeTaggedItemEntries(Mailbox mbox, int tagId, List<Integer> itemIds) throws ServiceException {
        if (tagId < 0 && !Mailbox.REIFIED_FLAGS.contains(tagId))
            return;

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getTaggedItemTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "tag_id = ? AND " + DbUtil.whereIn("item_id", itemIds.size()));
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tagId);
            for (int itemId : itemIds) {
                stmt.setInt(pos++, itemId);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("removing TAGGED_ITEM entries for tag: " + tagId + ", items: " + DbMailItem.getIdListForLogging(itemIds), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void persistCounts(Tag tag) throws ServiceException {
        Mailbox mbox = tag.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getTagTableName(mbox) +
                    " SET item_count = ?, unread = ? WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) tag.getSize());
            stmt.setInt(pos++, tag.getUnreadCount());
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tag.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating counts for tag " + tag.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void saveMetadata(Tag tag) throws ServiceException {
        Mailbox mbox = tag.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + getTagTableName(mbox) +
                    " SET color = ?, policy = ?, listed = ?, sequence = ? WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            Color color = tag.getRgbColor();
            if (color != null && color.getMappedColor() != MailItem.DEFAULT_COLOR) {
                stmt.setLong(pos++, color.getValue());
            } else {
                stmt.setNull(pos++, Types.BIGINT);
            }
            RetentionPolicy rp = tag.getRetentionPolicy();
            if (rp != null && rp.isSet()) {
                stmt.setString(pos++, RetentionPolicyManager.toMetadata(rp, true).toString());
            } else {
                stmt.setNull(pos++, Types.VARCHAR);
            }
            stmt.setBoolean(pos++, tag.isListed());
            stmt.setInt(pos++, tag.getModifiedSequence());
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tag.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating counts for tag " + tag.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void renameTag(Tag tag) throws ServiceException {
        Mailbox mbox = tag.getMailbox();
        String newName = tag.getName();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // first, get the old canonical tag name
            stmt = conn.prepareStatement("SELECT name FROM " + getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tag.getId());

            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw MailServiceException.NO_SUCH_TAG(tag.getId());
            }
            String oldName = rs.getString(1);
            if (newName.equals(oldName)) {
                return;
            }
            DbPool.closeResults(rs);
            rs = null;
            DbPool.closeStatement(stmt);
            stmt = null;

            // next, update the tag's name
            stmt = conn.prepareStatement("UPDATE " + getTagTableName(mbox) +
                    " SET name = ?, sequence = ? WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id = ?");
            pos = 1;
            stmt.setString(pos++, newName);
            stmt.setInt(pos++, tag.getModifiedSequence());
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tag.getId());

            stmt.executeUpdate();
            DbPool.closeStatement(stmt);
            stmt = null;

            // finally, update the denormalized TAG_NAMES column in the MAIL_ITEM table
            if (Db.supports(Db.Capability.MULTITABLE_UPDATE)) {
                String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "mi.mailbox_id = ti.mailbox_id AND ";
                stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox, "mi") +
                        " INNER JOIN " + getTaggedItemTableName(mbox, "ti") + " ON " + mailboxesMatchAnd + "mi.id = ti.item_id" +
                        " SET tag_names = REPLACE(tag_names, ?, ?), mod_metadata = ?, change_date = ?" +
                        " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id = ?");
                pos = 1;
                stmt.setString(pos++, delimitTagName(oldName));
                stmt.setString(pos++, delimitTagName(newName));
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, tag.getId());
            } else {
                stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                        " SET tag_names = REPLACE(tag_names, ?, ?), mod_metadata = ?, change_date = ?" +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id IN" +
                        " (SELECT ti.item_id FROM " + getTaggedItemTableName(mbox, "ti") + " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id = ?)");
                pos = 1;
                stmt.setString(pos++, delimitTagName(oldName));
                stmt.setString(pos++, delimitTagName(newName));
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, tag.getId());
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(newName, e);
            } else {
                throw ServiceException.FAILURE("renaming tag " + tag.getId() + " to " + tag.getName(), e);
            }
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void deleteTag(Tag tag) throws ServiceException {
        Mailbox mbox = tag.getMailbox();

        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            // first, remove the tag name from all items' TAG_NAMES column
            //   (if it was the last tag on the item, TAG_NAMES becomes NULL)
            int pos = 1;
            String delimited = delimitTagName(tag.getName());
            if (Db.supports(Db.Capability.MULTITABLE_UPDATE)) {
                String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "mi.mailbox_id = ti.mailbox_id AND ";
                stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox, "mi") +
                        " INNER JOIN " + getTaggedItemTableName(mbox, "ti") + " ON " + mailboxesMatchAnd + "mi.id = ti.item_id" +
                        " SET mi.tag_names = CASE mi.tag_names WHEN ? THEN NULL ELSE REPLACE(mi.tag_names, ?, '" + Db.getEscapeSequence() + "0') END, mod_metadata = ?, change_date = ?" +
                        " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id = ?");
                stmt.setString(pos++, delimited);
                stmt.setString(pos++, delimited);
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, tag.getId());
            } else {
                stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                        " SET tag_names = CASE tag_names WHEN ? THEN NULL ELSE REPLACE(tag_names, ?, '" + Db.getEscapeSequence() + "0') END, mod_metadata = ?, change_date = ?" +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id IN" +
                        " (SELECT ti.item_id FROM " + getTaggedItemTableName(mbox, "ti") + " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id = ?)");
                stmt.setString(pos++, delimited);
                stmt.setString(pos++, delimited);
                stmt.setInt(pos++, mbox.getOperationChangeID());
                stmt.setInt(pos++, mbox.getOperationTimestamp());
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setInt(pos++, tag.getId());
            }

            stmt.executeUpdate();
            DbPool.closeStatement(stmt);
            stmt = null;

            // then we can delete the TAG row, which automatically cascades into TAGGED_ITEM
            deleteTagRow(mbox, tag.getId());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating counts for tag " + tag.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void deleteTagRow(Mailbox mbox, int tagId) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            // delete the TAG row, which automatically cascades into TAGGED_ITEM
            stmt = conn.prepareStatement("DELETE FROM " + getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id = ?");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tagId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting tag row for tagId " + tagId, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static PendingDelete getLeafNodes(Mailbox mbox, Tag tag, int before, Integer maxItems)
    throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String orderByLimit = "";
            if (maxItems != null && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                orderByLimit = " ORDER BY date " + Db.getInstance().limit(maxItems);
            }

            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "mi.mailbox_id = ti.mailbox_id AND ";
            stmt = conn.prepareStatement("SELECT " + DbMailItem.LEAF_NODE_FIELDS +
                    " FROM " + DbMailItem.getMailItemTableName(mbox, "mi") +
                    " INNER JOIN " + getTaggedItemTableName(mbox, "ti") + " ON " + mailboxesMatchAnd + "ti.item_id = mi.id" +
                    " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id = ? AND date < ? AND type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES +
                    orderByLimit);
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tag.getId());
            stmt.setInt(pos++, before);

            PendingDelete info = DbMailItem.accumulateDeletionInfo(mbox, stmt);
            stmt = null;
            return info;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching list of items for purge", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    // test code; not portable
    @SuppressWarnings("unused")
    private static void recalculateTagStrings(Mailbox mbox, Tag taggedWith) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                    " SET tag_names = " +
                        "(SELECT (CONCAT('" + Db.getEscapeSequence() + "0', GROUP_CONCAT(t.name SEPARATOR '" + Db.getEscapeSequence() + "0'), '" + Db.getEscapeSequence() + "0')" +
                        " FROM tag t INNER JOIN tagged_item ti ON t.mailbox_id = ti.mailbox_id AND t.id = ti.tag_id" +
                        " WHERE ti.item_id = mi.id AND ti.tag_id > 0)" +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE mi.mailbox_id = ?"));
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("recalculating TAG_NAMES column for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static String inThisMailboxAnd(String alias) {
        if (DebugConfig.disableMailboxGroups) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            if (!Strings.isNullOrEmpty(alias)) {
                sb.append(alias).append('.');
            }
            return sb.append("mailbox_id = ? AND ").toString();
        }
    }

    /** Returns the name of the table that stores {@link Tag} data.  The table
     *  name is qualified with the name of the database (e.g. <tt>mboxgroup1.tag</tt>). */
    public static String getTagTableName(int mailboxId, int groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_TAG);
    }

    public static String getTagTableName(MailItem item) throws ServiceException {
        return DbMailbox.qualifyTableName(item.getMailbox(), TABLE_TAG);
    }

    public static String getTagTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_TAG);
    }

    public static String getTagTableName(Mailbox mbox, String alias) {
        return getTagTableName(mbox) + " AS " + alias;
    }

    public static String getTaggedItemTableName(int mailboxId, int groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_TAGGED_ITEM);
    }

    public static String getTaggedItemTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_TAGGED_ITEM);
    }

    public static String getTaggedItemTableName(Mailbox mbox, String alias) {
        return getTaggedItemTableName(mbox) + " AS " + alias;
    }


    @VisibleForTesting
    public static void debugConsistencyCheck(Mailbox mbox, MailboxLock l) throws ServiceException {
        DbConnection conn = l.isWriteLockedByCurrentThread() ? mbox.getOperationConnection() : DbPool.getConnection(mbox);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Map<Integer, UnderlyingData> tdata = Maps.newHashMap();
            // make sure the item counts match the tag totals
            stmt = conn.prepareStatement("SELECT " + TAG_FIELDS + " FROM " + getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id > 0");
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            while (rs.next()) {
                UnderlyingData data = asUnderlyingData(rs);
                tdata.put(data.id, data);
            }
            DbPool.closeResults(rs);
            rs = null;
            DbPool.closeStatement(stmt);
            stmt = null;

            // catch spurious TAGGED_ITEM entries
            validateTaggedItem(conn, mbox, tdata);
            // make sure the TAGGED_ITEM table is accurate
            verifyTaggedItem(conn, mbox, tdata);
            // make sure the item counts match the tag totals
            verifyTagCounts(conn, mbox, tdata);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("consistency checking tags/flags for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            if (!l.isWriteLockedByCurrentThread()) {
                conn.close();
            }
        }
    }

    //TODO remove JUnit dependency from non test class
    private static void validateTaggedItem(DbConnection conn, Mailbox mbox, Map<Integer, UnderlyingData> tdata) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "ti.mailbox_id = mi.mailbox_id AND ";
            stmt = conn.prepareStatement("SELECT mi.id, mi.type, ti.tag_id " +
                    " FROM " + getTaggedItemTableName(mbox, "ti") +
                    " INNER JOIN " + DbMailItem.getMailItemTableName(mbox, "mi") + " ON " + mailboxesMatchAnd + "mi.id = ti.item_id" +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "type IN " + DbMailItem.NON_SEARCHABLE_TYPES);
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            while (rs.next()) {
                int itemId = rs.getInt(1), type = rs.getInt(2), tagId = rs.getInt(3);
                String tname = tagId > 0 ? tdata.get(tagId).name : mbox.getFlagById(tagId).getName();
                Assert.fail("found tag " + tname + " on " + MailItem.Type.of((byte) type) + " " + itemId);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("validating TAGGED_ITEM entries", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    //TODO remove JUnit dependency from non test class
    private static void verifyTaggedItem(DbConnection conn, Mailbox mbox, Map<Integer, UnderlyingData> tdata) throws ServiceException {
        int flagMask = 0;
        for (int flagId : Mailbox.REIFIED_FLAGS) {
            flagMask |= 1 << (-flagId - 1);
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id, flags, tag_names, unread FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES);
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                int flags = (rs.getInt(2) & flagMask) | (rs.getBoolean(4) ? Flag.BITMASK_UNREAD : 0);
                String[] tagset = deserializeTags(rs.getString(3));
                Set<String> tags = tagset == null ? Collections.<String>emptySet() : Sets.newHashSet(tagset);

                PreparedStatement stmtcheck = null;
                ResultSet rscheck = null;
                try {
                    // make sure the item counts match the tag totals
                    String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "tag.mailbox_id = ti.mailbox_id AND ";
                    stmtcheck = conn.prepareStatement("SELECT id, name FROM " + getTagTableName(mbox, "tag") +
                            " INNER JOIN " + getTaggedItemTableName(mbox, "ti") + " ON " + mailboxesMatchAnd + "tag.id = ti.tag_id" +
                            " WHERE " + inThisMailboxAnd("ti") + "ti.item_id = ?");
                    int pos = 1;
                    pos = DbMailItem.setMailboxId(stmtcheck, mbox, pos);
                    stmtcheck.setInt(pos++, id);

                    rscheck = stmtcheck.executeQuery();
                    int flagcheck = 0;
                    Set<String> tagcheck = Sets.newHashSetWithExpectedSize(tags.size());
                    while (rscheck.next()) {
                        int idcheck = rscheck.getInt(1);
                        String namecheck = rscheck.getString(2);
                        if (idcheck < 0) {
                            flagcheck |= 1 << (-idcheck - 1);
                        } else {
                            tagcheck.add(namecheck);
                        }
                    }
                    Assert.assertEquals("flags for item " + id, flags, flagcheck);
                    Assert.assertEquals("tags for item " + id, tags, tagcheck);
                } catch (SQLException e) {
                    throw ServiceException.FAILURE("consistency checking TAGGED_ITEM vs. MAIL_ITEM", e);
                } finally {
                    DbPool.closeResults(rscheck);
                    DbPool.closeStatement(stmtcheck);
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("consistency checking TAGGED_ITEM vs. MAIL_ITEM", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    //TODO remove JUnit dependency from non test class
    private static void verifyTagCounts(DbConnection conn, Mailbox mbox, Map<Integer, UnderlyingData> tdata) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "ti.mailbox_id = mi.mailbox_id AND ";
            stmt = conn.prepareStatement("SELECT ti.tag_id, COUNT(ti.item_id), " + Db.clauseIFNULL("SUM(mi.unread)", "0") +
                    " FROM " + getTaggedItemTableName(mbox, "ti") +
                    " INNER JOIN " + DbMailItem.getMailItemTableName(mbox, "mi") + " ON " + mailboxesMatchAnd + "mi.id = ti.item_id" +
                    " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id > 0 AND " + Db.getInstance().bitAND("mi.flags", String.valueOf(Flag.BITMASK_DELETED)) + " = 0" +
                    " GROUP BY ti.tag_id");
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            Map<Integer, UnderlyingData> tcheck = new HashMap<Integer, UnderlyingData>(tdata);
            while (rs.next()) {
                int id = rs.getInt(1), size = rs.getInt(2), unread = rs.getInt(3);
                UnderlyingData data = tcheck.remove(id);
                Assert.assertNotNull("no TAG row for id " + id, data);
                Assert.assertEquals("size for tag " + data.name, size, data.size);
                Assert.assertEquals("unread for tag " + data.name, unread, data.unreadCount);
            }
            for (UnderlyingData data : tcheck.values()) {
                Assert.assertEquals("size for tag " + data.name, 0, data.size);
                Assert.assertEquals("unread for tag " + data.name, 0, data.unreadCount);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("consistency checking TAGGED_ITEM vs. TAG", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

//    private static void verifyTagCounts(DbConnection conn, Mailbox mbox, Map<Integer, UnderlyingData> tdata) throws ServiceException {
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//        try {
//            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "ti.mailbox_id = mi.mailbox_id AND ";
//            stmt = conn.prepareStatement("SELECT ti.tag_id, mi.id, mi.unread, mi.flags, mi.type" +
//                    " FROM " + getTaggedItemTableName(mbox, "ti") +
//                    " INNER JOIN " + DbMailItem.getMailItemTableName(mbox, "mi") + " ON " + mailboxesMatchAnd + "mi.id = ti.item_id" +
//                    " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id > 0 AND " + Db.getInstance().bitAND("mi.flags", String.valueOf(Flag.BITMASK_DELETED)) + " = 0");
////                    " WHERE " + inThisMailboxAnd("ti") + "ti.tag_id > 0");
//            DbMailItem.setMailboxId(stmt, mbox, 1);
//            rs = stmt.executeQuery();
//
//            while (rs.next()) {
//                int tagId = rs.getInt(1), itemId = rs.getInt(2), unread = rs.getInt(3), flags = rs.getInt(4), type = rs.getInt(5);
//                System.out.println(MailItem.Type.of((byte) type) + " " + itemId + ": tag " + tdata.get(tagId).name + ": unread " + (unread > 0) +
//                        ", deleted " + ((flags & Flag.BITMASK_DELETED) != 0) + " (flags " + flags + ")");
//            }
//            System.out.println("-----------------------");
//        } catch (SQLException e) {
//            throw ServiceException.FAILURE("consistency checking TAGGED_ITEM vs. TAG", e);
//        } finally {
//            DbPool.closeResults(rs);
//            DbPool.closeStatement(stmt);
//        }
//    }

    //TODO remove JUnit dependency from non test class
    public static <T> void assertCollectionsEqual(String msg, java.util.Collection<T> expected, java.util.Collection<T> actual) {
        String prefix = msg == null ? "" : msg + ": ";
        if (expected == null) {
            Assert.assertNull(prefix + "expected <null>", actual);
        } else if (actual == null) {
            Assert.fail(prefix + "was <null>, expected " + expected);
        } else {
            Assert.assertEquals(prefix + "collection size", expected.size(), actual.size());
            for (T t : expected) {
                if (!actual.contains(t)) {
                    Assert.fail(prefix + "actual collection does not contain <" + t + ">");
                }
            }
        }
    }
}
