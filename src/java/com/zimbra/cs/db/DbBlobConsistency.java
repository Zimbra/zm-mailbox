/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.store.file.BlobConsistencyChecker.BlobInfo;

public class DbBlobConsistency {
    
    /**
     * Returns blob info for items in the specified id range.
     */
    public static Collection<BlobInfo> getBlobInfo(Connection conn, Mailbox mbox, long minId, long maxId, short volumeId)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<BlobInfo> blobs = new ArrayList<BlobInfo>(); 
        
        try {
            stmt = conn.prepareStatement(
                "SELECT id, mod_content, 0, size " +
                "FROM " + DbMailItem.getMailItemTableName(mbox) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                " id BETWEEN " + minId + " AND " + maxId +
                " AND blob_digest IS NOT NULL " +
                "AND volume_id = " + volumeId +
                " UNION " +
                "SELECT item_id, mod_content, version, size " +
                "FROM " + DbMailItem.getRevisionTableName(mbox) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                " item_id BETWEEN " + minId + " AND " + maxId +
                " AND blob_digest IS NOT NULL " +
                "AND volume_id = " + volumeId);
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setLong(1, mbox.getId());
                stmt.setLong(2, mbox.getId());
            }
            Db.getInstance().enableStreaming(stmt);
            rs = stmt.executeQuery();
            while (rs.next()) {
                BlobInfo info = new BlobInfo();
                info.itemId = rs.getLong(1);
                info.modContent = rs.getInt(2);
                info.version = rs.getInt(3);
                info.dbSize = rs.getLong(4);
                info.volumeId = volumeId;
                info.path = FileBlobStore.getBlobPath(mbox, (int) info.itemId, info.modContent, volumeId);
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
    
    public static long getMaxId(Connection conn, Mailbox mbox)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String sql = "SELECT MAX(id) " +
                "FROM " + DbMailItem.getMailItemTableName(mbox);
            if (!DebugConfig.disableMailboxGroups) {
                sql += " WHERE mailbox_id = " + mbox.getId();
            }
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting max id for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
        }
    }
    
    public static int getNumRows(Connection conn, Mailbox mbox, String tableName,
                                 String idColName, Collection<Integer> itemIds)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) " +
                "FROM " + DbMailbox.qualifyTableName(mbox, tableName) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                DbUtil.whereIn(idColName, itemIds.size()));
            int i = 1;
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setLong(i++, mbox.getId());
            }
            for (int id : itemIds) {
                stmt.setInt(i++, id);
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
    
    public static void export(Connection conn, Mailbox mbox, String tableName,
                              String idColName, Collection<Integer> itemIds, String path)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        if (!(Db.getInstance() instanceof MySQL)) {
            throw ServiceException.INVALID_REQUEST("export is only supported for MySQL", null);
        }
        ZimbraLog.sqltrace.info("Exporting %d items in table %s to %s.", itemIds.size(), tableName, path);
        
        try {
            stmt = conn.prepareStatement("SELECT * " +
                "FROM " + DbMailbox.qualifyTableName(mbox, tableName) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                DbUtil.whereIn(idColName, itemIds.size()) +
                " INTO OUTFILE ?");
            int i = 1;
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setLong(i++, mbox.getId());
            }
            for (int id : itemIds) {
                stmt.setInt(i++, id);
            }
            stmt.setString(i++, path);
            rs = stmt.executeQuery();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("exporting table " + tableName + " to " + path, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
        }
    }
}
