/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.purge.DataSourcePurge.PurgeableConv;

public class DbDataSource {

    public static class DataSourceItem {
        public int folderId;
        public int itemId;
        public String remoteId;
        public Metadata md;
        public int itemFlags = -1;

        public DataSourceItem(int f, int i, String r, Metadata m) {
            folderId = f;
            itemId = i;
            remoteId = r;
            md = m;
        }

        public DataSourceItem(int f, int i, String r, Metadata m, int fl) {
            this(f, i, r, m);
            itemFlags = fl;
        }
    }

    public static final String TABLE_DATA_SOURCE_ITEM = "data_source_item";
    public static final String TABLE_PURGED_MESSAGES = "purged_messages";
    public static final String TABLE_PURGED_CONVERSATIONS = "purged_conversations";

    public static void addMapping(DataSource ds, DataSourceItem item) throws ServiceException {
        addMapping(ds, item, false);
    }

    public static void addMapping(DataSource ds, DataSourceItem item, final boolean isBatch) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        DbConnection conn = null;
        PreparedStatement stmt = null;
        String dataSourceId = ds.getId();

        if (item.remoteId == null)
            item.remoteId = "";

        ZimbraLog.datasource.debug("Adding mapping for dataSource %s: itemId(%d), remoteId(%s)", ds.getName(), item.itemId, item.remoteId);

        try {
            if (isBatch) {
                conn = mbox.getOperationConnection();
            } else {
                conn = DbPool.getConnection(mbox);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ");
            sb.append(getTableName(mbox));
            sb.append(" (");
            sb.append(DbMailItem.MAILBOX_ID);
            sb.append("data_source_id, item_id, folder_id, remote_id, metadata) VALUES (");
            sb.append(DbMailItem.MAILBOX_ID_VALUE);
            sb.append("?, ?, ?, ?, ?)");
            if (Db.supports(Db.Capability.ON_DUPLICATE_KEY)) {
                sb.append(" ON DUPLICATE KEY UPDATE data_source_id = ?, item_id = ?, folder_id = ?, remote_id = ?, metadata = ?");
            }
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, dataSourceId);
            stmt.setInt(i++, item.itemId);
            stmt.setInt(i++, item.folderId);
            stmt.setString(i++, item.remoteId);
            stmt.setString(i++, DbMailItem.checkMetadataLength((item.md == null) ? null : item.md.toString()));
            if (Db.supports(Db.Capability.ON_DUPLICATE_KEY)) {
                stmt.setString(i++, dataSourceId);
                stmt.setInt(i++, item.itemId);
                stmt.setInt(i++, item.folderId);
                stmt.setString(i++, item.remoteId);
                stmt.setString(i++, DbMailItem.checkMetadataLength((item.md == null) ? null : item.md.toString()));
            }
            stmt.executeUpdate();
            if (!isBatch) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (!Db.supports(Db.Capability.ON_DUPLICATE_KEY) && Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                DbPool.closeStatement(stmt);
                if (!isBatch) {
                    DbPool.quietClose(conn);
                }
                updateMapping(ds, item, isBatch);
            } else {
                throw ServiceException.FAILURE("Unable to add mapping for dataSource " + ds.getName(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
            if (!isBatch) {
                DbPool.quietClose(conn);
            }
        }
    }

    public static void updateMapping(DataSource ds, DataSourceItem item) throws ServiceException {
        updateMapping(ds, item, false);
    }

    public static void updateMapping(DataSource ds, DataSourceItem item, final boolean isBatch) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        ZimbraLog.datasource.debug("Updating mapping for dataSource %s: itemId(%d), remoteId(%s)", ds.getName(), item.itemId, item.remoteId);

        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            if (isBatch) {
                conn = mbox.getOperationConnection();
            } else {
                conn = DbPool.getConnection(mbox);
            }
            if (!Db.supports(Db.Capability.ON_DUPLICATE_KEY) && !hasMapping(ds, item.itemId)) {
                // if we need to update due to unique remoteId then itemid
                // isn't going to be the same
                StringBuilder sb = new StringBuilder();
                sb.append("UPDATE ");
                sb.append(getTableName(mbox));
                sb.append(" SET folder_id = ?, item_id = ?, metadata = ? WHERE ");
                sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
                sb.append(" remote_id = ?");
                stmt = conn.prepareStatement(sb.toString());
                int i = 1;
                stmt.setInt(i++, item.folderId);
                stmt.setInt(i++, item.itemId);
                stmt.setString(i++, DbMailItem.checkMetadataLength((item.md == null) ? null : item.md.toString()));
                i = DbMailItem.setMailboxId(stmt, mbox, i);
                stmt.setString(i++, item.remoteId);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("UPDATE ");
                sb.append(getTableName(mbox));
                sb.append(" SET folder_id = ?, remote_id = ?, metadata = ? WHERE ");
                sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
                sb.append(" item_id = ?");
                stmt = conn.prepareStatement(sb.toString());
                int i = 1;
                stmt.setInt(i++, item.folderId);
                stmt.setString(i++, item.remoteId);
                stmt.setString(i++, DbMailItem.checkMetadataLength((item.md == null) ? null : item.md.toString()));
                i = DbMailItem.setMailboxId(stmt, mbox, i);
                stmt.setInt(i++, item.itemId);
            }
            stmt.executeUpdate();
            if (!isBatch) {
                conn.commit();
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to update mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            if (!isBatch) {
                DbPool.quietClose(conn);
            }
        }
    }

    public static void deleteMappings(DataSource ds, Collection<Integer> itemIds) throws ServiceException {
        deleteMappings(ds, itemIds, false);
    }

    public static void deleteMappings(DataSource ds, Collection<Integer> itemIds, boolean isBatch) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        ZimbraLog.datasource.debug("Deleting %d mappings for dataSource %s", itemIds.size(), ds.getName());
        List<List<Integer>> splitIds = ListUtil.split(itemIds, Db.getINClauseBatchSize());
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            if (isBatch) {
                conn = mbox.getOperationConnection();
            } else {
                conn = DbPool.getConnection(mbox);
            }
            int numRows = 0;
            for (List<Integer> curIds : splitIds) {
                StringBuilder sb = new StringBuilder();
                sb.append("DELETE FROM ");
                sb.append(getTableName(mbox));
                sb.append(" WHERE ");
                sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
                sb.append(" data_source_id = ? AND ");
                sb.append(DbUtil.whereIn("item_id", curIds.size()));
                stmt = conn.prepareStatement(sb.toString());

                int i = 1;
                i = DbMailItem.setMailboxId(stmt, mbox, i);
                stmt.setString(i++, ds.getId());
                for (int itemId : curIds)
                    stmt.setInt(i++, itemId);

                numRows += stmt.executeUpdate();
                if (!isBatch) {
                    conn.commit();
                }
                stmt.close();
            }
            ZimbraLog.datasource.debug("Deleted %d mappings for %s", numRows, ds.getName());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            if (!isBatch) {
                DbPool.quietClose(conn);
            }
        }
    }

    public static void deleteAllMappings(DataSource ds) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        ZimbraLog.datasource.debug("Deleting all mappings for dataSource %s", ds.getName());
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection(mbox);
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append(" data_source_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            int numRows = stmt.executeUpdate();
            conn.commit();
            ZimbraLog.datasource.debug("Deleted %d mappings for %s", numRows, ds.getName());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void deleteMapping(DataSource ds, int itemId) throws ServiceException {
        deleteMapping(ds, itemId, false);
    }

    public static void deleteMapping(DataSource ds, int itemId, boolean isBatch) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

    	ZimbraLog.datasource.debug("Deleting mappings for dataSource %s itemId %d", ds.getName(), itemId);

        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            if (isBatch) {
                conn = mbox.getOperationConnection();
            } else {
                conn = DbPool.getConnection(mbox);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append(" data_source_id = ? AND");
            sb.append(" item_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            stmt.setInt(i++, itemId);
            int numRows = stmt.executeUpdate();
            if (!isBatch) {
                conn.commit();
            }
            ZimbraLog.datasource.debug("Deleted %d mappings for %s", numRows, ds.getName());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeStatement(stmt);
            if (!isBatch) {
                DbPool.quietClose(conn);
            }
        }
    }

    public static Collection<DataSourceItem> deleteAllMappingsInFolder(DataSource ds, int folderId) throws ServiceException {
        return deleteAllMappingsInFolder(ds, folderId, false);
    }

    public static Collection<DataSourceItem> deleteAllMappingsInFolder(DataSource ds, int folderId, boolean isBatch) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();

        ZimbraLog.datasource.debug("Deleting all mappings for dataSource %s in folder %d", ds.getName(), folderId);

        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
            DbConnection conn = null;
            PreparedStatement stmt = null;
            try {
                if (isBatch) {
                    conn = mbox.getOperationConnection();
                } else {
                    conn = DbPool.getConnection(mbox);
                }
                String dataSourceTable = getTableName(mbox);
                String IN_THIS_MAILBOX_AND = DebugConfig.disableMailboxGroups ? "" : dataSourceTable + ".mailbox_id = ? AND ";
                StringBuilder sb = new StringBuilder();
                sb.append("DELETE FROM ");
                sb.append(dataSourceTable);
                sb.append(" WHERE ");
                sb.append(IN_THIS_MAILBOX_AND);
                sb.append("  data_source_id = ? AND folder_id = ?");
                stmt = conn.prepareStatement(sb.toString());
                int pos = 1;
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setString(pos++, ds.getId());
                stmt.setInt(pos++, folderId);
                int numRows = stmt.executeUpdate();
                if (!isBatch) {
                    conn.commit();
                }
                stmt.close();
                ZimbraLog.datasource.debug("Deleted %d mappings for %s", numRows, ds.getName());
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Unable to delete mapping for dataSource "+ds.getName(), e);
            } finally {
                DbPool.closeStatement(stmt);
                if (!isBatch) {
                    DbPool.quietClose(conn);
                }
            }
        }
        return items;
    }

    public static boolean hasMapping(DataSource ds, int itemId) throws ServiceException {
        DataSourceItem item = getMapping(ds, itemId);
        return item.remoteId != null;
    }

    public static Collection<DataSourceItem> getAllMappings(DataSource ds) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();

        ZimbraLog.datasource.debug("Get all mappings for %s", ds.getName());

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection(mbox);
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT item_id, folder_id, remote_id, metadata FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                Metadata md = null;
                String buf = DbMailItem.decodeMetadata(rs.getString(4));
                if (buf != null)
                    md = new Metadata(buf);
                items.add(new DataSourceItem(rs.getInt(2), rs.getInt(1), rs.getString(3), md));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return items;
    }

    public static Collection<DataSourceItem> getAllMappingsInFolder(DataSource ds, int folderId) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();

        ZimbraLog.datasource.debug("Get all mappings for %s in folder %d", ds.getName(), folderId);

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection(mbox);

            String thisTable = getTableName(mbox);
            String IN_THIS_MAILBOX_AND = DebugConfig.disableMailboxGroups ? "" : thisTable + ".mailbox_id = ? AND ";
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT item_id, remote_id, ").append(thisTable).append(".metadata FROM ");
            sb.append(thisTable);
            sb.append(" WHERE ");
            sb.append(IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ? AND folder_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, ds.getId());
            stmt.setInt(pos++, folderId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Metadata md = null;
                String buf = DbMailItem.decodeMetadata(rs.getString(3));
                if (buf != null)
                    md = new Metadata(buf);
                items.add(new DataSourceItem(folderId, rs.getInt(1), rs.getString(2), md));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return items;
    }

    public static Collection<DataSourceItem> getAllMappingsAndFlagsInFolder(DataSource ds, int folderId) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();

        ZimbraLog.datasource.debug("Get all mappings for %s in folder %d", ds.getName(), folderId);

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String thisTable = getTableName(mbox);
            String IN_THIS_MAILBOX_AND = DebugConfig.disableMailboxGroups ? "" : thisTable + ".mailbox_id = ? AND ";
            String MBOX_JOIN = DebugConfig.disableMailboxGroups ? " " : thisTable + ".mailbox_id = mi.mailbox_id AND ";
            conn = DbPool.getConnection(mbox);
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT item_id, remote_id, ").append(thisTable).append(".metadata, mi.unread, mi.flags FROM ");
            sb.append(thisTable);
            sb.append("  LEFT OUTER JOIN " + DbMailItem.getMailItemTableName(mbox)).append(" mi ");
            sb.append("  ON ").append(MBOX_JOIN).append(thisTable).append(".item_id = mi.id ");
            sb.append(" WHERE ");
            sb.append(IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ? AND ").append(thisTable).append(".folder_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, ds.getId());
            stmt.setInt(pos++, folderId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Metadata md = null;
                String buf = DbMailItem.decodeMetadata(rs.getString(3));
                int unread = rs.getInt(4);
                int flags = rs.getInt(5);

                if (buf != null)
                    md = new Metadata(buf);
                flags = unread > 0 ? (flags | Flag.BITMASK_UNREAD) : (flags & ~Flag.BITMASK_UNREAD);
                items.add(new DataSourceItem(folderId, rs.getInt(1), rs.getString(2), md, flags));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return items;
    }

    public static Collection<DataSourceItem> getAllMappingsForRemoteIdPrefix(DataSource ds, int folderId, String prefix)
            throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        List<DataSourceItem> items = new ArrayList<DataSourceItem>();

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection(mbox);
            String db = DbMailbox.getDatabaseName(mbox);
            String dst = db + ".data_source_item";
            String mit = db + ".mail_item";
            Formatter fmt = new Formatter();
            fmt.format("SELECT item_id, remote_id, %s.metadata FROM %s", dst, dst);
            fmt.format(" INNER JOIN %s ON %s.item_id = %s.id", mit, dst, mit);
            fmt.format(" WHERE %s.mailbox_id = ?", dst);
            fmt.format(" AND data_source_id = ? AND folder_id = ?");
            if (prefix != null) {
                fmt.format(" AND remote_id LIKE '%s%%'", prefix);
            }
            stmt = conn.prepareStatement(fmt.toString());
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, ds.getId());
            stmt.setInt(3, folderId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                String s = DbMailItem.decodeMetadata(rs.getString(3));
                Metadata md = s != null ? new Metadata(s) : null;
                items.add(new DataSourceItem(folderId, rs.getInt(1), rs.getString(2), md));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for data source " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return items;
    }

    public static DataSourceItem getMapping(DataSource ds, int itemId) throws ServiceException {
        return getMapping(ds, itemId, false);
    }

    public static DataSourceItem getMapping(DataSource ds, int itemId, boolean isBatch) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        int folderId = 0;
        String remoteId = null;
        Metadata md = null;

        ZimbraLog.datasource.debug("Get mapping for %s, itemId=%d", ds.getName(), itemId);

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (isBatch) {
                conn = mbox.getOperationConnection();
            } else {
                conn = DbPool.getConnection(mbox);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT folder_id, remote_id, metadata FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ? AND item_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            stmt.setInt(i++, itemId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                folderId = rs.getInt(1);
                remoteId = rs.getString(2);
                String buf = DbMailItem.decodeMetadata(rs.getString(3));
                if (buf != null)
                    md = new Metadata(buf);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            if (!isBatch) {
                DbPool.quietClose(conn);
            }
        }
        return new DataSourceItem(folderId, itemId, remoteId, md);
    }

    public static DataSourceItem getReverseMapping(DataSource ds, String remoteId) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);

        int folderId = 0;
        int itemId = 0;
        Metadata md = null;

        ZimbraLog.datasource.debug("Get reverse mapping for %s, remoteId=%s", ds.getName(), remoteId);

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection(mbox);
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT item_id, folder_id, metadata FROM ");
            sb.append(getTableName(mbox));
            sb.append(" WHERE ");
            sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
            sb.append("  data_source_id = ? AND remote_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int i = 1;
            i = DbMailItem.setMailboxId(stmt, mbox, i);
            stmt.setString(i++, ds.getId());
            stmt.setString(i++, remoteId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                itemId = rs.getInt(1);
                folderId = rs.getInt(2);
                String buf = DbMailItem.decodeMetadata(rs.getString(3));
                if (buf != null)
                    md = new Metadata(buf);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get reverse mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return new DataSourceItem(folderId, itemId, remoteId, md);
    }

    public static Collection<DataSourceItem> getMappings(DataSource ds, Collection<Integer> ids)
            throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
        int folderId = 0;
        int itemId = 0;
        String remoteId = null;
        Metadata md = null;
        List<List<Integer>> splitIds = ListUtil.split(ids, Db.getINClauseBatchSize());
        ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();

        ZimbraLog.datasource.debug("Get mappings for %s", ds.getName());

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection(mbox);
            for (List<Integer> curIds : splitIds) {
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT item_id, remote_id, folder_id, metadata FROM ");
                sb.append(getTableName(mbox));
                sb.append(" WHERE ");
                sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
                sb.append(" data_source_id = ? AND ");
                sb.append(DbUtil.whereIn("item_id", curIds.size()));
                stmt = conn.prepareStatement(sb.toString());
                int i = 1;
                i = DbMailItem.setMailboxId(stmt, mbox, i);
                stmt.setString(i++, ds.getId());
                for (int uid : curIds)
                    stmt.setInt(i++, uid);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    itemId = rs.getInt(1);
                    remoteId = rs.getString(2);
                    folderId = rs.getInt(3);
                    String buf = DbMailItem.decodeMetadata(rs.getString(4));
                    if (buf != null)
                        md = new Metadata(buf);
                    items.add(new DataSourceItem(folderId, itemId, remoteId, md));
                }
                rs.close();
                stmt.close();
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return items;
    }

    public static Collection<DataSourceItem> getReverseMappings(DataSource ds, Collection<String> remoteIds)
            throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
        int folderId = 0;
        int itemId = 0;
        String remoteId;
        Metadata md = null;
        List<List<String>> splitIds = ListUtil.split(remoteIds, Db.getINClauseBatchSize());
        ArrayList<DataSourceItem> items = new ArrayList<DataSourceItem>();

        ZimbraLog.datasource.debug("Get reverse mappings for %s", ds.getName());

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection(mbox);
            for (List<String> curIds : splitIds) {
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT item_id, remote_id, folder_id, metadata FROM ");
                sb.append(getTableName(mbox));
                sb.append(" WHERE ");
                sb.append(DbMailItem.IN_THIS_MAILBOX_AND);
                sb.append(" data_source_id = ? AND ");
                sb.append(DbUtil.whereIn("remote_id", curIds.size()));
                stmt = conn.prepareStatement(sb.toString());
                int i = 1;
                i = DbMailItem.setMailboxId(stmt, mbox, i);
                stmt.setString(i++, ds.getId());
                for (String uid : curIds)
                    stmt.setString(i++, uid);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    itemId = rs.getInt(1);
                    remoteId = rs.getString(2);
                    folderId = rs.getInt(3);
                    String buf = DbMailItem.decodeMetadata(rs.getString(4));
                    if (buf != null)
                        md = new Metadata(buf);
                    items.add(new DataSourceItem(folderId, itemId, remoteId, md));
                }
                rs.close();
                stmt.close();
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get reverse mapping for dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return items;
    }

    public static int getDataSourceUsage(DataSource ds) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
        ZimbraLog.datasource.debug("Getting size of %s", ds.getName());

        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int totalSize = 0;
        try {
            conn = mbox.getOperationConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT sum(mi.size) FROM ");
            sb.append(getTableName(mbox)).append(" ds");
            sb.append(" INNER JOIN ");
            sb.append(DbMailItem.getMailItemTableName(mbox)).append(" mi");
            sb.append(" ON ds.mailbox_id = mi.mailbox_id AND ds.item_id = mi.id");
            sb.append(" WHERE ds.mailbox_id = ? AND ds.data_source_id = ? AND mi.type = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, ds.getId());
            stmt.setByte(pos++, MailItem.Type.MESSAGE.toByte());
            rs = stmt.executeQuery();
            while (rs.next()) {
                totalSize = rs.getInt(1);
                break;
            }
            rs.close();
            stmt.close();
            return totalSize;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get size of dataSource " + ds.getName(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }


    public static List<PurgeableConv> getOldestConversationsUpToSize(DataSource ds, long targetSize) throws ServiceException {
        return getOldestConversationsUpToSize(Collections.singletonList(ds), targetSize, 0);
    }

    public static List<PurgeableConv> getOldestConversationsUpToSize(List<DataSource> dataSources, long targetSize) throws ServiceException {
        return getOldestConversationsUpToSize(dataSources, targetSize, 0);
    }

    public static List<PurgeableConv> getOldestConversationsUpToSize(DataSource ds, long targetSize, long startDate) throws ServiceException {
        return getOldestConversationsUpToSize(Collections.singletonList(ds), targetSize, 0);
    }

    public static List<PurgeableConv> getOldestConversationsUpToSize(List<DataSource> dataSources, long targetSize, long startDate) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(dataSources.get(0));
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long totalSize = 0;
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT DISTINCT COALESCE(msgs.parent_id, msgs.msg_id) AS item_id,"
                    + " msgs.data_source_id AS data_source,"
                    + " COALESCE(convs.latest_date, msgs.mdate) AS newest_msg_date,"
                    + " COALESCE(convs.num_msgs, 1) AS num_msgs,"
                    + " COALESCE(convs.conv_size, msgs.msize) AS size");
            sb.append(" FROM ");
            sb.append("(SELECT di.data_source_id AS data_source_id, "
                    + " mi.id AS msg_id,"
                    + " mi.parent_id AS parent_id,"
                    + " mi.size AS msize,"
                    + " mi.date AS mdate FROM ");
            sb.append(getTableName(mbox)).append(" di ");
            sb.append(" INNER JOIN ");
            sb.append(DbMailItem.getMailItemTableName(mbox)).append(" mi ");
            sb.append(" ON di.mailbox_id = mi.mailbox_id AND di.item_id = mi.id");
            sb.append(" WHERE di.mailbox_id = ? AND mi.type = ? AND ");
            sb.append(DbUtil.whereIn("di.data_source_id", dataSources.size()));
            sb.append(") AS msgs");
            sb.append(" LEFT JOIN ");
            sb.append("(SELECT parent_id, max(date) AS latest_date, count(date) AS num_msgs, sum(size) AS conv_size");
            sb.append(" FROM ").append(DbMailItem.getMailItemTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND type = ? GROUP BY parent_id) AS convs");
            sb.append(" ON msgs.parent_id = convs.parent_id");
            if (startDate > 0L) {
                sb.append(" WHERE COALESCE(convs.latest_date, msgs.mdate) > ?");
            }
            sb.append(" ORDER BY COALESCE(convs.latest_date, msgs.mdate) ASC");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setByte(pos++, MailItem.Type.MESSAGE.toByte());
            for (DataSource ds: dataSources) {
                stmt.setString(pos++, ds.getId());
            }
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setByte(pos++, MailItem.Type.MESSAGE.toByte());
            if (startDate > 0L) {
                stmt.setLong(pos++, startDate);
            }
            rs = stmt.executeQuery();
            List<PurgeableConv> convs = new LinkedList<PurgeableConv>();
            while (rs.next() && totalSize < targetSize) {
                long convSize = rs.getLong("size");
                int itemId = rs.getInt("item_id");
                int numMsgs = rs.getInt("num_msgs");
                long convDate = rs.getLong("newest_msg_date");
                String dataSourceId = rs.getString("data_source");
                convs.add(new PurgeableConv(itemId, convSize, convDate, dataSourceId, numMsgs));
                totalSize += convSize;
            }
            rs.close();
            stmt.close();
            return convs;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get oldest conversations for data sources", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static Set<Integer> getConvMessageIdsInDataSource(Mailbox mbox, Integer convId, String dsId) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT mi.id FROM ");
            sb.append("(SELECT id, mailbox_id FROM ");
            sb.append(DbMailItem.getMailItemTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND parent_id = ?");
            sb.append(") mi");
            sb.append(" INNER JOIN ").append(getTableName(mbox)).append(" dsi ");
            sb.append("ON dsi.mailbox_id = mi.mailbox_id AND dsi.item_id = mi.id ");
            sb.append("WHERE dsi.data_source_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, convId);
            stmt.setString(pos++, dsId);
            rs = stmt.executeQuery();
            Set<Integer> ids = new HashSet<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            rs.close();
            stmt.close();
            return ids;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get message IDs for conversation in data source", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
    public static String getTableName(int mailboxId, int groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_DATA_SOURCE_ITEM);
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_DATA_SOURCE_ITEM);
    }

    public static String getPurgedConvsTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_PURGED_CONVERSATIONS);
    }

    public static String getPurgedMessagesTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_PURGED_MESSAGES);
    }

    public static void purgeMessage(Mailbox mbox, Message msg, String dsId) throws ServiceException {
        ZimbraLog.datasource.debug("Purging message %d from data source %s", msg.getId(), dsId);
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = mbox.getOperationConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(getPurgedMessagesTableName(mbox));
            sb.append(" (mailbox_id, data_source_id, item_id, parent_id, remote_id, remote_folder_id, purge_date) ");
            sb.append("SELECT dsi.mailbox_id, dsi.data_source_id, dsi.item_id, ?, dsi.remote_id, dsi_inner.remote_id, ?");
            sb.append(" FROM ");
            sb.append("(SELECT * FROM ").append(getTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ? AND item_id = ?) dsi");
            sb.append(" INNER JOIN ");
            sb.append("(SELECT item_id, remote_id FROM ").append(getTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ?) dsi_inner");
            sb.append(" ON dsi.folder_id = dsi_inner.item_id");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            if (msg.getConversationId() < 0) {
                stmt.setNull(pos++, Types.INTEGER);
            } else {
                stmt.setInt(pos++, msg.getConversationId());
            }
            stmt.setInt(pos++, (int)(System.currentTimeMillis() / 1000));
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, dsId);
            stmt.setInt(pos++, msg.getId());
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, dsId);
            stmt.execute();
            conn.commit();
            stmt.close();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                ZimbraLog.datasource.warn(String.format("purging message %d more than once", msg.getId()));
                return;
            }
            throw ServiceException.FAILURE("Unable to record purged message", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void moveToPurgedConversations(Mailbox mbox, MailItem item, String dsId) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(getPurgedConvsTableName(mbox));
            sb.append("(mailbox_id, data_source_id, item_id, hash)");
            sb.append(" SELECT ?, ").append(String.format("'%s', ", dsId)).append(" conv_id, hash");
            sb.append(" FROM ").append(DbMailItem.getConversationTableName(mbox)).append(" ci");
            sb.append(" WHERE ci.mailbox_id = ? AND ci.conv_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setInt(pos++, item.getId());
            stmt.execute();
            conn.commit();
            stmt.close();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                ZimbraLog.datasource.warn(String.format("moving item %d to purged conversations table more than once", item.getId()));
                return;
            }
            throw ServiceException.FAILURE("Unable to move conversation to purged_conversations table", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static boolean uidIsPurged(DataSource ds, String remoteId) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        try {
            conn = mbox.getOperationConnection();
            StringBuilder sb = new StringBuilder();
            sb.append(" SELECT COUNT(*) FROM ").append(getPurgedMessagesTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ? AND remote_id = ?");
            sb.append(" GROUP BY remote_id");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, ds.getId());
            stmt.setString(pos++, remoteId);
            rs = stmt.executeQuery();
            boolean exists = false;
            while (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    exists = true;
                    if (count > 1) {
                        ZimbraLog.datasource.warn("remote id should not show up in the purged messages table more than once");
                    }
                }
            }
            rs.close();
            stmt.close();
            return exists;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to determine if UID is purged", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static void removeFromPurgedMessageTable(Mailbox mbox, DataSource ds, PurgedMessage msg) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ").append(getPurgedMessagesTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ? AND item_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, ds.getId());
            stmt.setInt(pos++, msg.getMsgId());
            stmt.executeUpdate();
            stmt.close();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable delete purged message", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    private static void removeFromPurgedConversationTable(Mailbox mbox, DataSource ds, PurgedMessage msg) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ").append(getPurgedConvsTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ? AND item_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, ds.getId());
            Integer parentId = msg.getParentId();
            stmt.setInt(pos++, parentId != null && parentId > 0 ? parentId : msg.getMsgId());
            stmt.executeUpdate();
            stmt.close();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable delete purged conversation", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
    public static void removePurgedMessage(DataSource ds, PurgedMessage msg)  throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
        removeFromPurgedMessageTable(mbox, ds, msg);
        removeFromPurgedConversationTable(mbox, ds, msg);
    }

    public static List<PurgedConversation> lookupPurgedConversationsByHash(DataSource ds, List<String> hashes) throws ServiceException {
        Mailbox mbox = DataSourceManager.getInstance().getMailbox(ds);
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        Map<Integer, PurgedConversation> convMap = new HashMap<Integer, PurgedConversation>();
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT pm.item_id, pm.parent_id, pm.remote_id, pm.remote_folder_id FROM");
            sb.append(" (SELECT item_id, parent_id, remote_id, remote_folder_id FROM ").append(getPurgedMessagesTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ?)");
            sb.append(" pm ");
            sb.append(" INNER JOIN ");
            sb.append("(SELECT DISTINCT item_id FROM ").append(getPurgedConvsTableName(mbox));
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ? AND ");
            sb.append(DbUtil.whereIn("hash", hashes.size()));
            sb.append(") pc ");
            sb.append("ON COALESCE(pm.parent_id, pm.item_id) = pc.item_id");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, ds.getId());
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, ds.getId());
            for (String hash: hashes) {
                stmt.setString(pos++, hash);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                Integer msgId = rs.getInt(1);
                Integer parentId = rs.getInt(2);
                String remoteId =  rs.getString(3);
                String remoteFolderId = rs.getString(4);
                PurgedConversation conv = null;
                if (convMap.containsKey(parentId)) {
                    conv = convMap.get(parentId);
                } else {
                    conv = new PurgedConversation(ds, parentId);
                    convMap.put(parentId, conv);
                }
                conv.addMessage(new PurgedMessage(msgId, parentId, remoteId, remoteFolderId));
            }
            rs.close();
            stmt.close();
            Collection<PurgedConversation> values = convMap.values();
            return new ArrayList<PurgedConversation>(values);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get purged conversations", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    private static void deletePurgeDataByDataSource(Mailbox mbox, String dataSourceId, String tableName) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ").append(tableName);
            sb.append(" WHERE mailbox_id = ? AND data_source_id = ?");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, dataSourceId);
            stmt.executeUpdate();
            stmt.close();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable delete purged conversations for data source", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void deletePurgedDataForDataSource(Mailbox mbox, String dataSourceId) throws ServiceException {
        deletePurgeDataByDataSource(mbox, dataSourceId, getPurgedConvsTableName(mbox));
        deletePurgeDataByDataSource(mbox, dataSourceId, getPurgedMessagesTableName(mbox));
    }

    public static void storePurgedConversationHash(Mailbox mbox, String dataSourceId, Integer convId, String hash)
    throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(getPurgedConvsTableName(mbox));
            sb.append(" (mailbox_id, data_source_id, item_id, hash) VALUES (?, ?, ?, ?)");
            stmt = conn.prepareStatement(sb.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++,  dataSourceId);
            stmt.setInt(pos++, convId);
            stmt.setString(pos++, hash);
            stmt.executeUpdate();
            stmt.close();
            conn.commit();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                ZimbraLog.datasource.warn("inserting duplicate hash");
            } else {
                throw ServiceException.FAILURE("Unable to insert purged conversation hash", e);
            }
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static class PurgedConversation {
        private Integer id;
        private DataSource ds;
        private List<PurgedMessage> messages = new ArrayList<PurgedMessage>();

        public PurgedConversation(DataSource ds, Integer convId) {
            this.ds = ds;
            this.id = convId;
        }

        public void addMessage(PurgedMessage pm) {
            messages.add(pm);
        }

        public List<PurgedMessage> getMessages() {
            return messages;
        }

        public Integer getId() {
            return id;
        }

        public void unpurge() throws ServiceException {
            for (PurgedMessage msg: messages) {
                DbDataSource.removePurgedMessage(ds, msg);
            }
        }
    }

    public static class PurgedMessage {
        private Integer msgId;
        private Integer parentId;
        private String remoteId;
        private String remoteFolder;

        public PurgedMessage(Integer msgId, Integer parentId, String remoteId, String remoteFolder) {
            this.msgId = msgId;
            this.parentId = parentId;
            this.remoteId = remoteId;
            this.remoteFolder = remoteFolder;
        }

        public Integer getMsgId() { return msgId; }

        public Integer getParentId() { return parentId; }

        public String getRemoteId() { return remoteId; }

        public String getRemoteFolder() { return remoteFolder; }

        public String getUid() { return remoteId.split("_")[1]; }

        public Integer getLocalFolderId() { return Integer.valueOf(remoteId.split("_")[0]); }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
            helper.add("id", msgId);
            helper.add("remote id", remoteId);
            helper.add("folder", remoteFolder);
            return helper.toString();
        }
    }
}
