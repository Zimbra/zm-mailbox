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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;

public class DbImapFolder {

    static final String TABLE_IMAP_FOLDER = "imap_folder";
    
    private static Map<Integer, Integer> sNextImapFolderId = new HashMap<Integer, Integer>();

    public static Map<String, ImapFolder> getImapFolders(Mailbox mbox, DataSource ds)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, ImapFolder> imapFolders = new HashMap<String, ImapFolder>();

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "SELECT id, folder_path " +
                "FROM " + getTableName(mbox) +
                " WHERE mailbox_id = ? AND data_source_id = ?");

            int i = 1;
            stmt.setInt(i++, mbox.getId());
            stmt.setString(i++, ds.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String folderPath = rs.getString("folder_path");
                ImapFolder imapFolder =
                    new ImapFolder(mbox.getId(), id, ds.getId(), folderPath);
                imapFolders.put(folderPath, imapFolder);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get IMAP folder data", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }

        ZimbraLog.datasource.debug("Found %d folders for %s", imapFolders.size(), ds);
        return imapFolders;
    }
    
    public static ImapFolder createImapFolder(Mailbox mbox, DataSource ds, String folderPath)
    throws ServiceException
    {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            int imapFolderId = getNextImapFolderId(mbox);
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO " + getTableName(mbox) +
                " (mailbox_id, id, data_source_id, folder_path) " +
                "VALUES (?, ?, ?, ?)");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, imapFolderId);
            stmt.setString(3, ds.getId());
            stmt.setString(4, folderPath);
            stmt.executeUpdate();
            conn.commit();
            
            return new ImapFolder(mbox.getId(), imapFolderId, ds.getId(), folderPath);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to store IMAP message data", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
    
    /**
     * Deletes all IMAP message data for the given mailbox/data source.
     */
    public static void deleteImapData(Mailbox mbox, String dataSourceId)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
    
        ZimbraLog.datasource.info("Deleting IMAP data for DataSource %s", dataSourceId);
        
        if (StringUtil.isNullOrEmpty(dataSourceId)) {
            return;
        }
        
        try {
            // Note: data in imap_message gets deleted implicitly by the
            // foreign key cascading delete
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "DELETE FROM " + getTableName(mbox) +
                " WHERE mailbox_id = ? AND data_source_id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, dataSourceId);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete IMAP data", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    /**
     * Deletes all IMAP message data for the given mailbox/data source.
     */
    public static void deleteImapFolder(Mailbox mbox, DataSource ds, ImapFolder folder)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
    
        ZimbraLog.datasource.info("Deleting IMAP data for %s in %s", folder, ds);
        
        try {
            // Note: data in imap_message gets deleted implicitly by the
            // foreign key cascading delete
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "DELETE FROM " + getTableName(mbox) +
                " WHERE mailbox_id = ? AND data_source_id = ? and id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, ds.getId());
            stmt.setInt(3, folder.getId());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete IMAP folder", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    private static int getNextImapFolderId(Mailbox mbox)
    throws ServiceException {
        synchronized (sNextImapFolderId) {
            // Try the cache first
            Integer nextId = sNextImapFolderId.get(mbox.getId());
            if (nextId != null) {
                ZimbraLog.datasource.debug("Got next IMAP folder id %d for mailbox %d", nextId, mbox.getId());
                sNextImapFolderId.put(mbox.getId(), nextId + 1);
                return nextId.intValue();
            }
        }
            
        // Not in the cache.  Get the next id from the database.
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int nextId = 0;

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "SELECT MAX(id) " +
                "FROM " + getTableName(mbox) +
                " WHERE mailbox_id = ?");

            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();
            rs.next();
            nextId = rs.getInt(1) + 1;
            if (rs.wasNull()) {
                nextId = 1;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get IMAP folder data", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        
        synchronized (sNextImapFolderId) {
            sNextImapFolderId.put(mbox.getId(), nextId + 1);
        }
        return nextId;
    }

    static String getTableName(int mailboxId, int groupId) {
        return String.format("%s.%s", DbMailbox.getDatabaseName(mailboxId, groupId), TABLE_IMAP_FOLDER);
    }

    static String getTableName(Mailbox mbox) {
        return DbMailbox.getDatabaseName(mbox) + "." + TABLE_IMAP_FOLDER;
    }
}
