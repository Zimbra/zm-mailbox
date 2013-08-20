/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.BlobConsistencyChecker.BlobInfo;
import com.zimbra.cs.store.file.FileBlobStore;

public class DbBlobConsistency {

    /**
     * Returns blob info for items in the specified id range.
     */
    public static Collection<BlobInfo> getBlobInfo(DbConnection conn, Mailbox mbox, int minId, int maxId, short volumeId)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<BlobInfo> blobs = new ArrayList<BlobInfo>();

        try {
            stmt = conn.prepareStatement(
                "SELECT id, mod_content, 0, size " +
                "FROM " + DbMailItem.getMailItemTableName(mbox, false) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                " id BETWEEN " + minId + " AND " + maxId +
                " AND blob_digest IS NOT NULL " +
                "AND locator = " + volumeId +
                " UNION " +
                "SELECT id, mod_content, 0, size " +
                "FROM " + DbMailItem.getMailItemTableName(mbox, true) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                " id BETWEEN " + minId + " AND " + maxId +
                " AND blob_digest IS NOT NULL " +
                "AND locator = " + volumeId +
                " UNION " +
                "SELECT item_id, mod_content, version, size " +
                "FROM " + DbMailItem.getRevisionTableName(mbox, false) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                " item_id BETWEEN " + minId + " AND " + maxId +
                " AND blob_digest IS NOT NULL " +
                "AND locator = " + volumeId +
                " UNION " +
                "SELECT item_id, mod_content, version, size " +
                "FROM " + DbMailItem.getRevisionTableName(mbox, true) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                " item_id BETWEEN " + minId + " AND " + maxId +
                " AND blob_digest IS NOT NULL " +
                "AND locator = " + volumeId);
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setInt(1, mbox.getId());
                stmt.setInt(2, mbox.getId());
                stmt.setInt(3, mbox.getId());
                stmt.setInt(4, mbox.getId());
            }
            Db.getInstance().enableStreaming(stmt);
            rs = stmt.executeQuery();
            while (rs.next()) {
                BlobInfo info = new BlobInfo();
                info.itemId = rs.getInt(1);
                info.modContent = rs.getInt(2);
                info.version = rs.getInt(3);
                info.dbSize = rs.getLong(4);
                info.volumeId = volumeId;
                info.path = FileBlobStore.getBlobPath(mbox, info.itemId, info.modContent, volumeId);
                blobs.add(info);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting items with blobs for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
        }

        return blobs;
    }


    public static Collection<BlobInfo> getExternalMailItemBlobInfo(DbConnection conn, Mailbox mbox, int minId, int maxId) throws ServiceException {
        return getExternalMailItemBlobInfo(conn, mbox, minId, maxId, false);
    }

    public static Collection<BlobInfo> getExternalMailItemDumpsterBlobInfo(DbConnection conn, Mailbox mbox, int minId, int maxId) throws ServiceException {
        return getExternalMailItemBlobInfo(conn, mbox, minId, maxId, true);
    }

    private static Collection<BlobInfo> getExternalMailItemBlobInfo(DbConnection conn, Mailbox mbox, int minId, int maxId, boolean dumpster) throws ServiceException {
        String query = "SELECT id, mod_content, 0, size, locator " +
                        "FROM " + DbMailItem.getMailItemTableName(mbox, dumpster) +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                        " id BETWEEN " + minId + " AND " + maxId +
                        " AND blob_digest IS NOT NULL";
        return getExternalBlobInfo(conn, mbox, query);
    }

    public static Collection<BlobInfo> getExternalRevisionBlobInfo(DbConnection conn, Mailbox mbox, int minId, int maxId) throws ServiceException {
        return getExternalRevisionBlobInfo(conn, mbox, minId, maxId, false);
    }

    public static Collection<BlobInfo> getExternalRevisionDumpsterBlobInfo(DbConnection conn, Mailbox mbox, int minId, int maxId) throws ServiceException {
        return getExternalRevisionBlobInfo(conn, mbox, minId, maxId, true);
    }

    private static Collection<BlobInfo> getExternalRevisionBlobInfo(DbConnection conn, Mailbox mbox, int minId, int maxId, boolean dumpster) throws ServiceException {
        String query = "SELECT item_id, mod_content, version, size, locator " +
                        "FROM " + DbMailItem.getRevisionTableName(mbox, dumpster) +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                        " item_id BETWEEN " + minId + " AND " + maxId +
                        " AND blob_digest IS NOT NULL";
        return getExternalBlobInfo(conn, mbox, query);
    }

    private static Collection<BlobInfo> getExternalBlobInfo(DbConnection conn, Mailbox mbox, String query)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<BlobInfo> blobs = new ArrayList<BlobInfo>();

        try {
            stmt = conn.prepareStatement(query);
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setInt(1, mbox.getId());
            }
            Db.getInstance().enableStreaming(stmt);
            rs = stmt.executeQuery();
            while (rs.next()) {
                BlobInfo info = new BlobInfo();
                info.itemId = rs.getInt(1);
                info.modContent = rs.getInt(2);
                info.version = rs.getInt(3);
                info.dbSize = rs.getLong(4);
                info.path = rs.getString(5);
                info.external = true;
                blobs.add(info);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting items with blobs for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
        }

        return blobs;
    }



    public static int getMaxId(DbConnection conn, Mailbox mbox)
    throws ServiceException {
        int maxId = 0;
        boolean[] dumpster = new boolean[] { false, true };
        for (boolean fromDumpster : dumpster) {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "SELECT MAX(id) " +
                    "FROM " + DbMailItem.getMailItemTableName(mbox, fromDumpster);
                if (!DebugConfig.disableMailboxGroups) {
                    sql += " WHERE mailbox_id = " + mbox.getId();
                }
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
                rs.next();
                int id = rs.getInt(1);
                maxId = Math.max(id, maxId);
            } catch (SQLException e) {
                throw ServiceException.FAILURE("getting max id for mailbox " + mbox.getId(), e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.quietCloseStatement(stmt);
            }
        }
        return maxId;
    }

    public static int getNumRows(DbConnection conn, Mailbox mbox, String tableName,
                                 String idColName, Multimap<Integer, Integer> idRevs)
    throws ServiceException {
        Set<Integer> mail_itemIds = new HashSet<Integer>();
        Multimap<Integer, Integer> rev_itemIds = HashMultimap.create();
        for (Integer itemId : idRevs.keySet()) {
            Collection<Integer> revs = idRevs.get(itemId);
            for (int rev : revs) {
                if (rev == 0) {
                    mail_itemIds.add(itemId);
                } else {
                    rev_itemIds.put(itemId, rev);
                }
            }
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuffer sql = new StringBuffer();
            boolean revisionTable = tableName.startsWith(DbMailItem.TABLE_REVISION);
            sql.append("SELECT COUNT(*) FROM ").append(
                    DbMailbox.qualifyTableName(mbox, tableName)).append(" WHERE ").
                    append(DbMailItem.IN_THIS_MAILBOX_AND);

            if (!revisionTable || mail_itemIds.size() > 0) {
                if (mail_itemIds.size() == 0) {
                    sql.append(idColName).append(" in ('')");
                } else {
                    sql.append(DbUtil.whereIn(idColName, mail_itemIds.size()));
                }
            }
            if (revisionTable) {
                if (mail_itemIds.size() > 0 && rev_itemIds.size() > 0) {
                    sql.append(" OR ");
                }

                if (rev_itemIds.size() > 0) {
                    sql.append(DbUtil.whereIn(Db.getInstance().concat(idColName, "'-'", "version"), rev_itemIds.size()));
                }
            }

            stmt = conn.prepareStatement(sql.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);

            for (int itemId : mail_itemIds) {
                stmt.setInt(pos++, itemId);
            }

            if (revisionTable) {
                for (Integer itemId : rev_itemIds.keySet()) {
                    Collection<Integer> revs = rev_itemIds.get(itemId);
                    for (int rev : revs) {
                        stmt.setString(pos++, itemId + "-" + rev);
                    }
                }
            }
            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting number of rows for matching id's in " + tableName, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
        }
    }

    public static void export(DbConnection conn, Mailbox mbox, String tableName,
                              String idColName, Multimap<Integer, Integer> idRevs, String path)
    throws ServiceException {
        Set<Integer> mail_itemIds = new HashSet<Integer>();
        Multimap<Integer, Integer> rev_itemIds = HashMultimap.create();
        for (Integer itemId : idRevs.keySet()) {
            Collection<Integer> revs = idRevs.get(itemId);
            for (int rev : revs) {
                if (rev == 0) {
                    mail_itemIds.add(itemId);
                } else {
                    rev_itemIds.put(itemId, rev);
                }
            }
        }
        PreparedStatement stmt = null;

        if (!(Db.getInstance() instanceof MySQL)) {
            throw ServiceException.INVALID_REQUEST("export is only supported for MySQL", null);
        }
        ZimbraLog.sqltrace.info("Exporting %d items in table %s to %s.", idRevs.size(), tableName, path);

        try {
            StringBuffer sql = new StringBuffer();
            boolean revisionTable = tableName.startsWith(DbMailItem.TABLE_REVISION);
            sql.append("SELECT * FROM ").append(
                    DbMailbox.qualifyTableName(mbox, tableName)).append(" WHERE ").append(
                            DbMailItem.IN_THIS_MAILBOX_AND);

            if (!revisionTable || mail_itemIds.size() > 0) {
                if (mail_itemIds.size() == 0) {
                    sql.append(idColName).append(" in ('')");
                } else {
                    sql.append(DbUtil.whereIn(idColName, mail_itemIds.size()));
                }
            }
            if (revisionTable) {
                if (mail_itemIds.size() > 0 && rev_itemIds.size() > 0) {
                    sql.append(" OR ");
                }
                if (rev_itemIds.size() > 0) {
                    sql.append(DbUtil.whereIn(Db.getInstance().concat(idColName, "'-'", "version"), rev_itemIds.size()));
                }
            }
            sql.append(" INTO OUTFILE ?");
            stmt = conn.prepareStatement(sql.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            for (int itemId : mail_itemIds) {
                stmt.setInt(pos++, itemId);
            }

            if (revisionTable) {
                for (Integer itemId : rev_itemIds.keySet()) {
                    Collection<Integer> revs = rev_itemIds.get(itemId);
                    for (int rev : revs) {
                        stmt.setString(pos++, itemId + "-" + rev);
                    }
                }
            }
            stmt.setString(pos++, path);
            stmt.execute();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("exporting table " + tableName + " to " + path, e);
        } finally {
            DbPool.quietCloseStatement(stmt);
        }
    }

    public static void delete(DbConnection conn, Mailbox mbox, Multimap<Integer, Integer> idRevs)
    throws ServiceException {
        Set<Integer> mail_itemIds = new HashSet<Integer>();
        Multimap<Integer, Integer> rev_itemIds = HashMultimap.create();
        for (Integer itemId : idRevs.keySet()) {
            Collection<Integer> revs = idRevs.get(itemId);
            for (int rev : revs) {
                if (rev == 0) {
                    mail_itemIds.add(itemId);
                } else {
                    rev_itemIds.put(itemId, rev);
                }
            }
        }

        if (mail_itemIds.size() > 0) {
            PreparedStatement miDumpstmt = null;
            try {
                StringBuffer sql = new StringBuffer();
                sql.append("DELETE FROM ").append(
                        DbMailItem.getMailItemTableName(mbox, true)).append(" WHERE ").append(
                                DbMailItem.IN_THIS_MAILBOX_AND).append(DbUtil.whereIn("id", mail_itemIds.size()));

                miDumpstmt = conn.prepareStatement(sql.toString());
                int pos = 1;
                pos = DbMailItem.setMailboxId(miDumpstmt, mbox, pos);
                for (int itemId : mail_itemIds) {
                    miDumpstmt.setInt(pos++, itemId);
                }
                miDumpstmt.execute();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("deleting " + idRevs.size() + " item(s): " + DbMailItem.
                        getIdListForLogging(idRevs.keys()) + " from " + DbMailItem.TABLE_MAIL_ITEM_DUMPSTER + " table", e);
            } finally {
                DbPool.quietCloseStatement(miDumpstmt);
            }
        }

        if (rev_itemIds.size() > 0) {
            PreparedStatement revDumpstmt = null;
            try {
                StringBuffer sql = new StringBuffer();
                sql.append("DELETE FROM ").append(
                        DbMailItem.getRevisionTableName(mbox, true)).append(" WHERE ").append(
                                DbMailItem.IN_THIS_MAILBOX_AND).append(DbUtil.whereIn(
                                        Db.getInstance().concat("item_id", "'-'", "version"), rev_itemIds.size()));

                revDumpstmt = conn.prepareStatement(sql.toString());
                int pos = 1;
                pos = DbMailItem.setMailboxId(revDumpstmt, mbox, pos);
                for (Integer itemId : rev_itemIds.keySet()) {
                    Collection<Integer> revs = rev_itemIds.get(itemId);
                    for (int rev : revs) {
                        revDumpstmt.setString(pos++, itemId + "-" + rev);
                    }
                }
                revDumpstmt.execute();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("deleting " + idRevs.size() + " item(s): " + DbMailItem.
                        getIdListForLogging(idRevs.keys()) + " from " + DbMailItem.TABLE_REVISION_DUMPSTER + " table", e);
            } finally {
                DbPool.quietCloseStatement(revDumpstmt);
            }
        }
    }
}
