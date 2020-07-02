/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.acl.AclPushSerializer;
import com.zimbra.cs.service.util.SyncToken;

/**
 * Utility class to upgrade mailbox.
 *
 * @author Andy Clark
 */
public final class MailboxUpgrade {

    private static final Map<Long,Byte> UPGRADE_TO_1_7_COLORS = new HashMap<Long,Byte>();
    private static final Map<Long,Byte> UPGRADE_TO_1_8_COLORS = UPGRADE_TO_1_7_COLORS;

    static {
        Map<Long,Byte> map = UPGRADE_TO_1_7_COLORS;
        // common colors
        map.put(0x1000000L, (byte) 0); // none
        // original RGB colors
        map.put(0x10000ffL, (byte) 1); // blue
        map.put(0x1008284L, (byte) 2); // cyan
        map.put(0x1848284L, (byte) 8); // gray
        map.put(0x1008200L, (byte) 3); // green
        map.put(0x1ff8000L, (byte) 9); // orange
        map.put(0x1840084L, (byte) 4); // purple
        map.put(0x1ff0084L, (byte) 7); // pink
        map.put(0x1ff0000L, (byte) 5); // red
        map.put(0x1848200L, (byte) 6); // yellow
        // newer RGB colors
        map.put(0x19EB6F5L, (byte) 1); // blue
        map.put(0x1A4E6E6L, (byte) 2); // cyan
        map.put(0x1D3D3D3L, (byte) 8); // gray
        map.put(0x197C8B1L, (byte) 3); // green
        map.put(0x1FDBC55L, (byte) 9); // orange
        map.put(0x1FE9BD3L, (byte) 7); // pink
        map.put(0x1BA86E5L, (byte) 4); // purple
        map.put(0x1FC9696L, (byte) 5); // red
        map.put(0x1FFF6B3L, (byte) 6); // yellow
        // newest RGB colors
        map.put(0x10252d4L, (byte) 1); // blue
        map.put(0x1008284L, (byte) 2); // cyan
        map.put(0x1848284L, (byte) 8); // gray
        map.put(0x12ca10bL, (byte) 3); // green
        map.put(0x1f57802L, (byte) 9); // orange
        map.put(0x1b027aeL, (byte) 7); // pink
        map.put(0x1492ba1L, (byte) 4); // purple
        map.put(0x1e51717L, (byte) 5); // red
        map.put(0x1848200L, (byte) 6); // yellow
    }

    /** This class can not be instantiated. */
    private MailboxUpgrade() {}

    /**
     * bug 41893: revert folder colors back to mapped value.
     */
    public static void upgradeTo1_7(Mailbox mbox) throws ServiceException {
        OperationContext octxt = new OperationContext(mbox);
        for (Folder folder : mbox.getFolderList(octxt, SortBy.NONE)) {
            Color color = folder.getRgbColor();
            if (!color.hasMapping()) {
                Byte value = UPGRADE_TO_1_7_COLORS.get(color.getValue());
                if (value != null) {
                    Color newcolor = new Color(value);
                    mbox.setColor(octxt, new int[] { folder.getId() }, folder.getType(), newcolor);
                }
            }
        }
    }

    /**
     * bug 41850: revert tag colors back to mapped value.
     */
    public static void upgradeTo1_8(Mailbox mbox) throws ServiceException {
        OperationContext octxt = new OperationContext(mbox);
        for (Tag tag : mbox.getTagList(octxt)) {
            Color color = tag.getRgbColor();
            if (!color.hasMapping()) {
                Byte value = UPGRADE_TO_1_8_COLORS.get(color.getValue());
                if (value != null) {
                    Color newcolor = new Color(value);
                    mbox.setColor(octxt, new int[] { tag.getId() }, tag.getType(), newcolor);
                }
            }
        }
    }

    public static void upgradeTo2_0(Mailbox mbox) throws ServiceException {
        migrateHighestIndexed(mbox);

        // bug 56772
        migrateContactGroups(mbox);

        // bug 47673
        pushExistingFolderAclsToLdap(mbox);
    }

    private static void pushExistingFolderAclsToLdap(Mailbox mbox) throws ServiceException {
        List<Folder> folders = mbox.getFolderList(null, SortBy.NONE);
        Set<String> sharedItems = new HashSet<String>();
        for (Folder folder : folders) {
            ACL acl = folder.getACL();
            if (acl == null) {
                continue;
            }
            for (ACL.Grant grant : acl.getGrants()) {
                sharedItems.add(AclPushSerializer.serialize(folder, grant));
            }
        }
        if (!sharedItems.isEmpty()) {
            mbox.getAccount().setSharedItem(sharedItems.toArray(new String[sharedItems.size()]));
        }
    }

    private static void migrateHighestIndexed(Mailbox mbox) throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        PreparedStatement stmt;
        ResultSet rs;
        try {
            // fetch highest_indexed
            String highestIndexed = null;
            stmt = conn.prepareStatement("SELECT highest_indexed FROM " +
                    DbMailbox.qualifyZimbraTableName(mbox, "mailbox") + " WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {
                highestIndexed = rs.getString(1);
            }
            rs.close();
            stmt.close();

            if (Strings.isNullOrEmpty(highestIndexed)) {
                return;
            }

            SyncToken token;
            try {
                token = new SyncToken(highestIndexed);
            } catch (ServiceException e) {
                return;
            }

            // update index_id where mod_content/mod_metadata > highest_indexed
            stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                    " SET index_id = 0 WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                    "mod_content > ? AND mod_metadata > ? AND index_id IS NOT NULL");
            int pos = DbMailItem.setMailboxId(stmt, mbox, 1);
            stmt.setInt(pos++, token.getChangeId());
            stmt.setInt(pos++, token.getChangeId());
            stmt.executeUpdate();
            stmt.close();

            // clear highest_indexed
            stmt = conn.prepareStatement("UPDATE " + DbMailbox.qualifyZimbraTableName(mbox, "mailbox") +
                    " SET highest_indexed = NULL WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.executeUpdate();
            stmt.close();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw ServiceException.FAILURE("Failed to migrate highest_indexed", e);
        } finally {
            conn.closeQuietly();
        }
    }

    public static void migrateContactGroups(Mailbox mbox) throws ServiceException {
        ContactGroup.MigrateContactGroup migrate = new ContactGroup.MigrateContactGroup(mbox);
        try {
            migrate.handle();
            ZimbraLog.mailbox.info("contact group migration finished for mailbox " + mbox.getId());
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("contact group migration failed", e);
        }
    }

    public static void migrateFlagsAndTags(Mailbox mbox) throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        try {
            migrateFlagColumn(conn, mbox, true);
            migrateTagColumn(conn, mbox, true);
            // the tag load when the Mailbox object was constructed returned no tags
            //   because we hadn't migrated the tags yet, so force a reload
            mbox.purge(MailItem.Type.TAG);
            // any items already in the item cache don't have their tags set because
            //   we hadn't migrated the tags when they were loaded, so purge them
            mbox.purge(MailItem.Type.CONTACT);
            conn.commit();
        } catch (ServiceException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.closeQuietly();
        }
    }

    public static void upgradeTo2_1(Mailbox mbox) throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        try {
            if (alreadyUpgradedTo2_1(conn, mbox)) {
                ZimbraLog.mailbox.warn("detected already-migrated mailbox %d during migration to version 2.1; skipping.", mbox.getId());
            } else {
                // for flags that we want to be searchable, put an entry in the TAG table
                for (int tagId : Mailbox.REIFIED_FLAGS) {
                    DbTag.createTag(conn, mbox, Flag.of(mbox, tagId).getUnderlyingData(), null, false);
                }
                migrateFlagColumn(conn, mbox, false);
                migrateTagColumn(conn, mbox, false);

                // the tag load when the Mailbox object was constructed returned no tags
                //   because we hadn't migrated the tags yet, so force a reload
                mbox.purge(MailItem.Type.TAG);

                // any items already in the item cache don't have their tags set because
                //   we hadn't migrated the tags when they were loaded, so purge them
                mbox.purge(MailItem.Type.CONTACT);
            }
            conn.commit();
        } catch (ServiceException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.closeQuietly();
        }
    }

    private static boolean alreadyUpgradedTo2_1(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM " + DbTag.getTagTableName(mbox) +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = ?"));
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);

            rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("checking for repeated 2.1 upgrade for mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Create PRIORITY flag in TAG table and migrate FLAGS column to TAGGED_ITEM table.
     */
    public static void upgradeTo2_3(Mailbox mbox) throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (flagExists(conn, mbox, Flag.FlagInfo.PRIORITY))
                return;

            // put PRIORITY flag in the TAG table
            DbTag.createTag(conn, mbox, Flag.FlagInfo.PRIORITY.toFlag(mbox).getUnderlyingData(), null, false);
            // get all the different FLAGS column values for the mailbox
            List<Long> flagsets = getTagsets(conn, mbox, "flags");
            // create rows in the TAGGED_ITEM table for flagged items
            migrateColumnToTaggedItem(conn, mbox, "flags", Flag.ID_PRIORITY, matchTagsets(flagsets, Flag.BITMASK_PRIORITY), false);
            conn.commit();
        } catch (ServiceException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
            conn.closeQuietly();
        }
    }

    /** Create POST flag in TAG table. */
    public static void upgradeTo2_4(Mailbox mbox) throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        try {
            if (flagExists(conn, mbox, Flag.FlagInfo.POST))
                return;

            DbTag.createTag(conn, mbox, Flag.FlagInfo.POST.toFlag(mbox).getUnderlyingData(), null, false);
            conn.commit();
        } catch (ServiceException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.closeQuietly();
        }
    }

    /** Create MUTED flag in TAG table. */
    public static void upgradeTo2_7(Mailbox mbox) throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        try {
            if (flagExists(conn, mbox, Flag.FlagInfo.MUTED))
                return;

            DbTag.createTag(conn, mbox, Flag.FlagInfo.MUTED.toFlag(mbox).getUnderlyingData(), null, false);
            conn.commit();
        } catch (ServiceException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.closeQuietly();
        }
    }

    /** Check for the existence of a flag in the TAG table. */
    private static boolean flagExists(DbConnection conn, Mailbox mbox, Flag.FlagInfo finfo) throws ServiceException {
        assert Mailbox.REIFIED_FLAGS.contains(finfo.id) : "inserting non-reified flag";

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + DbTag.getTagTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id = ?");
            int pos = DbMailItem.setMailboxId(stmt, mbox, 1);
            stmt.setInt(pos, finfo.id);
            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                ZimbraLog.mailbox.debug(finfo.name() + " flag already exists");
                return true;
            }

            return false;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to check for " + finfo.name() + " flag", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    /**
     * Populate mail_item[_dumpster].uuid column.
     */
    public static void upgradeTo2_5(Mailbox mbox) throws ServiceException {
        DbMailItem.assignUuids(mbox, false);
    }

    private static void migrateFlagColumn(DbConnection conn, Mailbox mbox, boolean checkDuplicates) throws ServiceException {
        // get all the different FLAGS column values for the mailbox
        List<Long> flagsets = getTagsets(conn, mbox, "flags");

        // create rows in the new TAGGED_ITEM table for flagged items
        for (int tagId : Mailbox.REIFIED_FLAGS) {
            long bitmask = Flag.of(mbox, tagId).toBitmask();
            migrateColumnToTaggedItem(conn, mbox, "flags", tagId, matchTagsets(flagsets, bitmask), checkDuplicates);
        }

        // create rows in the new TAGGED_ITEM table for unread items
        migrateColumnToTaggedItem(conn, mbox, "unread", Flag.ID_UNREAD, ImmutableList.of(1L), checkDuplicates);
    }

    private static void migrateTagColumn(DbConnection conn, Mailbox mbox, boolean checkDuplicates) throws ServiceException {
        // create rows in the new TAG table for existing tags
        Map<Integer, String> tagNames = createTagRows(conn, mbox, checkDuplicates);
        if (tagNames.isEmpty())
            return;

        // get all the distinct TAGS column values for the mailbox
        List<Long> tagsets = getTagsets(conn, mbox, "tags");
        if (tagsets.isEmpty())
            return;

        // create rows in the new TAGGED_ITEM table for tagged items
        for (int tagId : tagNames.keySet()) {
            long bitmask = 1L << (tagId - 64);
            migrateColumnToTaggedItem(conn, mbox, "tags", tagId, matchTagsets(tagsets, bitmask), checkDuplicates);
        }

        // calculate tag unread/item counts
        calculateTagCounts(conn, mbox);

        // set MAIL_ITEM.TAG_NAME appropriately
        populateTagNameColumn(conn, mbox, tagNames, tagsets);

        // delete the old-style tags from the MAIL_ITEM table
        deleteTagRows(conn, mbox);
    }

    private static void migrateColumnToTaggedItem(DbConnection conn, Mailbox mbox, String column, int tagId, List<Long> matches, boolean checkDuplicates)
    throws ServiceException {
        if (matches.isEmpty())
            return;

        PreparedStatement stmt = null;
        try {
            String sql = "INSERT INTO " + DbTag.getTaggedItemTableName(mbox) + "(mailbox_id, tag_id, item_id)" +
                    " SELECT " + DbMailItem.MAILBOX_ID_VALUE + "?, id FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " t1 WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES +
                    " AND " + DbUtil.whereIn(column, matches.size());
            if (checkDuplicates) {
                sql = sql + " AND NOT EXISTS(SELECT mailbox_id, tag_id, item_id FROM " + DbTag.getTaggedItemTableName(mbox) +
                        " t2 WHERE mailbox_id = ? AND tag_id = ? AND t2.item_id = t1.id)";
            }
            stmt = conn.prepareStatement(sql);
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, tagId);
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            for (long match : matches) {
                stmt.setLong(pos++, match);
            }
            if (checkDuplicates) {
                stmt.setInt(pos++, mbox.getId());
                stmt.setInt(pos++, tagId);
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("creating rows in TAGGED_ITEM for tag/flag " + tagId + " in mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static List<Long> getTagsets(DbConnection conn, Mailbox mbox, String column) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT DISTINCT(" + column + ") FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "type NOT IN" + DbMailItem.NON_SEARCHABLE_TYPES);
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            List<Long> tagsets = Lists.newArrayList();
            while (rs.next()) {
                tagsets.add(rs.getLong(1));
            }
            return tagsets;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct values for column '" + column + "' in mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static List<Long> matchTagsets(List<Long> tagsets, long bitmask) {
        List<Long> matches = Lists.newArrayList();
        for (long tags : tagsets) {
            if ((tags & bitmask) != 0) {
                matches.add(tags);
            }
        }
        return matches;
    }

    private static String serializedTagset(long tags, Map<Integer, String> tagNames) {
        if (tags == 0) {
            return null;
        }

        List<String> matches = Lists.newArrayList();
        // There can only be 63 tags in 7.x and the ids can range from 64 to 126.
        for (int i = 0; i < 63; i++) {
            if ((tags & (1L << i)) != 0) {
                matches.add(tagNames.get(64 + i));
            }
        }
        return DbTag.serializeTags(matches.toArray(new String[matches.size()]));
    }

    private static Map<Integer, String> createTagRows(DbConnection conn, Mailbox mbox, boolean checkDuplicates) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id, name, mod_metadata, metadata FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "type = " + MailItem.Type.TAG.toByte());
            DbMailItem.setMailboxId(stmt, mbox, 1);

            rs = stmt.executeQuery();
            Map<Integer, String> tagNames = Maps.newHashMap();
            while (rs.next()) {
                UnderlyingData data = new UnderlyingData();
                data.id = rs.getInt(1);
                data.name = rs.getString(2);
                data.modMetadata = rs.getInt(3);
                Color color = Color.fromMetadata(new Metadata(rs.getString(4)).getLong(Metadata.FN_COLOR, MailItem.DEFAULT_COLOR));

                try {
                    DbTag.createTag(conn, mbox, data, color.getMappedColor() == MailItem.DEFAULT_COLOR ? null : color, true);
                } catch (ServiceException se) {
                    if (checkDuplicates && se.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                        // tag name already exist. append a random number to the tag name.
                        SecureRandom sr = new SecureRandom();
                        data.name = data.name + sr.nextInt();
                        DbTag.createTag(conn, mbox, data, color.getMappedColor() == MailItem.DEFAULT_COLOR ? null : color, true);
                    } else {
                        throw se;
                    }
                }

                tagNames.put(data.id, data.name);
            }
            return tagNames;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("creating TAG rows in mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static void calculateTagCounts(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            String mailboxesMatchAnd = DebugConfig.disableMailboxGroups ? "" : "ti.mailbox_id = mi.mailbox_id AND ";
            stmt = conn.prepareStatement("UPDATE " + DbTag.getTagTableName(mbox) + ", " +
                    "(SELECT ti.tag_id tid, COUNT(ti.item_id) count, " + Db.clauseIFNULL("SUM(mi.unread)", "0") + " unread_count" +
                    "  FROM " + DbTag.getTaggedItemTableName(mbox, "ti") +
                    "  INNER JOIN " + DbMailItem.getMailItemTableName(mbox, "mi") + " ON " + mailboxesMatchAnd + "mi.id = ti.item_id" +
                    "  WHERE " + DbTag.inThisMailboxAnd("ti") + "ti.tag_id > 0 AND " + Db.getInstance().bitAND("mi.flags", String.valueOf(Flag.BITMASK_DELETED)) + " = 0" +
                    "  GROUP BY ti.tag_id) AS x" +
                    " SET item_count = count, unread = unread_count WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "id = tid");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting MAIL_ITEM tag rows in mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static void populateTagNameColumn(DbConnection conn, Mailbox mbox, Map<Integer, String> tagNames, List<Long> tagsets)
    throws ServiceException {
        PreparedStatement stmt = null;
        try {
            StringBuilder update = new StringBuilder();
            for (int i = 0; i < tagsets.size(); i++) {
                update.append(" WHEN ? THEN ?");
            }
            stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                    " SET tag_names = CASE tags" + update + " END" +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "type NOT IN" + DbMailItem.NON_SEARCHABLE_TYPES);
            int pos = 1;
            for (long tags : tagsets) {
                stmt.setLong(pos++, tags);
                stmt.setString(pos++, serializedTagset(tags, tagNames));
            }
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("populating MAIL_ITEM.TAG_NAMES values in mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static void deleteTagRows(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "type = " + MailItem.Type.TAG.toByte());
            DbMailItem.setMailboxId(stmt, mbox, 1);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting MAIL_ITEM tag rows in mbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
}
