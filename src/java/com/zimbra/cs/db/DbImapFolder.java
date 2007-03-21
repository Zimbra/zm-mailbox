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
                "SELECT item_id, local_path, remote_path " +
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
                ImapFolder imapFolder =
                    new ImapFolder(mbox.getId(), itemId, ds.getId(), localPath, remotePath);
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
    
    public static ImapFolder createImapFolder(Mailbox mbox, DataSource ds, int itemId, String localPath, String remotePath)
    throws ServiceException
    {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO " + getTableName(mbox) +
                " (mailbox_id, item_id, data_source_id, local_path, remote_path) " +
                "VALUES (?, ?, ?, ?, ?)");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, itemId);
            stmt.setString(3, ds.getId());
            stmt.setString(4, localPath);
            stmt.setString(5, remotePath);
            stmt.executeUpdate();
            conn.commit();
            
            return new ImapFolder(mbox.getId(), itemId, ds.getId(), localPath, remotePath);
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
                " SET local_path = ?, remote_path = ?) " +
                "WHERE mailbox_id = ? AND data_source_id = ? AND item_id = ?");
            stmt.setString(1, imapFolder.getLocalPath());
            stmt.setString(2, imapFolder.getRemotePath());
            stmt.setInt(3, mbox.getId());
            stmt.setString(4, imapFolder.getDataSourceId());
            stmt.setInt(5, imapFolder.getItemId());
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

    static String getTableName(int mailboxId, int groupId) {
        return String.format("%s.%s", DbMailbox.getDatabaseName(mailboxId, groupId), TABLE_IMAP_FOLDER);
    }

    static String getTableName(Mailbox mbox) {
        return DbMailbox.getDatabaseName(mbox) + "." + TABLE_IMAP_FOLDER;
    }
}
