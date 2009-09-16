/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.store.file.BlobConsistencyChecker.BlobInfo;

public class DbBlobConsistency {
    
    /**
     * Returns blob info for items in the specified id range.  The key is the
     * filename of the blob.
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
}
