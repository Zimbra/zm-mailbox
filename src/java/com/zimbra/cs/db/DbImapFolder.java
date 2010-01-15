/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.datasource.ImapFolder;
import com.zimbra.cs.datasource.ImapFolderCollection;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

public class DbImapFolder {

    static final String TABLE_IMAP_FOLDER = "imap_folder";
    
    /**
     * Returns a <tt>List</tt> of <tt>ImapFolders</tt> for the given <tt>DataSource</tt>.
     */
    public static ImapFolderCollection getImapFolders(Mailbox mbox, DataSource ds)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ImapFolderCollection imapFolders = new ImapFolderCollection();

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "SELECT item_id, local_path, remote_path, uid_validity " +
                "FROM " + getTableName(mbox) +
                " WHERE mailbox_id = ? AND data_source_id = ?");

            int i = 1;
            stmt.setInt(i++, mbox.getId());
            stmt.setString(i++, ds.getId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                String localPath = rs.getString("local_path");
                String remotePath = rs.getString("remote_path");
                Long uidValidity = rs.getLong("uid_validity");
                if (rs.wasNull()) {
                    uidValidity = null;
                }
                ImapFolder imapFolder =
                    new ImapFolder(mbox.getId(), itemId, ds.getId(), localPath, remotePath, uidValidity);
                imapFolders.add(imapFolder);
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
    
    public static ImapFolder createImapFolder(Mailbox mbox, DataSource ds, int itemId,
                                              String localPath, String remotePath, long uidValidity)
    throws ServiceException
    {
        Connection conn = null;
        PreparedStatement stmt = null;

        ZimbraLog.datasource.debug(
            "createImapFolder: itemId = %d, localPath = %s, remotePath = %s, uidValidity = %d",
            itemId, localPath, remotePath, uidValidity);
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO " + getTableName(mbox) +
                " (mailbox_id, item_id, data_source_id, local_path, remote_path, uid_validity) " +
                "VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, itemId);
            stmt.setString(3, ds.getId());
            stmt.setString(4, localPath);
            stmt.setString(5, remotePath);
            stmt.setLong(6, uidValidity);
            stmt.executeUpdate();
            conn.commit();
            
            return new ImapFolder(mbox.getId(), itemId, ds.getId(), localPath, remotePath, uidValidity);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to store IMAP message data", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
    
    /**
     * Updates the database with the latest values stored in this <tt>ImapFolder</tt>. 
     */
    public static void updateImapFolder(ImapFolder imapFolder)
    throws ServiceException
    {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(imapFolder.getMailboxId());
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "UPDATE " + getTableName(mbox) +
                " SET local_path = ?, remote_path = ?, uid_validity = ? " +
                "WHERE mailbox_id = ? AND data_source_id = ? AND item_id = ?");
            stmt.setString(1, imapFolder.getLocalPath());
            stmt.setString(2, imapFolder.getRemotePath());
            stmt.setLong(3, imapFolder.getUidValidity());
            stmt.setInt(4, mbox.getId());
            stmt.setString(5, imapFolder.getDataSourceId());
            stmt.setInt(6, imapFolder.getItemId());
            int numRows = stmt.executeUpdate();
            if (numRows != 1) {
                throw ServiceException.FAILURE(
                    String.format("Incorrect number of rows updated (%d) for %s",
                        numRows, imapFolder), null);
            }
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to update " + imapFolder, e);
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
     * Deletes IMAP folder and message data for the given folder.
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
                " WHERE mailbox_id = ? AND data_source_id = ? and item_id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, ds.getId());
            stmt.setInt(3, folder.getItemId());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete IMAP folder", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static String getTableName(int mailboxId, int groupId) {
        return String.format("%s.%s", DbMailbox.getDatabaseName(groupId), TABLE_IMAP_FOLDER);
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.getDatabaseName(mbox) + "." + TABLE_IMAP_FOLDER;
    }
}
