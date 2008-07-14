/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

public class DbImapFolder {

    static final String TABLE_IMAP_FOLDER = "imap_folder";
    
    /**
     * Returns a <tt>List</tt> of <tt>ImapFolders</tt> for the given <tt>DataSource</tt>.
     */
    public static ImapFolderCollection getImapFolders(Mailbox mbox, DataSource ds)
    throws ServiceException {
        ImapFolderCollection imapFolders = new ImapFolderCollection();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DbPool.getConnection(mbox);

            stmt = conn.prepareStatement(
                "SELECT item_id, local_path, remote_path, uid_validity" +
                " FROM " + getTableName(mbox) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "data_source_id = ?");
            int pos = 1;
            if (!DebugConfig.disableMailboxGroups)
                stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, ds.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                String localPath = rs.getString("local_path");
                String remotePath = rs.getString("remote_path");
                Long uidValidity = rs.getLong("uid_validity");
                if (rs.wasNull())
                    uidValidity = null;

                ImapFolder imapFolder = new ImapFolder(mbox.getId(), itemId, ds.getId(), localPath, remotePath, uidValidity);
                imapFolders.add(imapFolder);
            }
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
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection(mbox);

        ZimbraLog.datasource.debug(
            "createImapFolder: itemId = %d, localPath = %s, remotePath = %s, uidValidity = %d",
            itemId, localPath, remotePath, uidValidity);
            String mailbox_id = DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ";
            stmt = conn.prepareStatement(
                "INSERT INTO " + getTableName(mbox) +
                " (" + mailbox_id + "item_id, data_source_id, local_path, remote_path, uid_validity) " +
                "VALUES (?, ?, ?, ?, ?, ?)");
            int pos = 1;
            if (!DebugConfig.disableMailboxGroups)
                stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, itemId);
            stmt.setString(pos++, ds.getId());
            stmt.setString(pos++, localPath);
            stmt.setString(pos++, remotePath);
            stmt.setLong(pos++, uidValidity);
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
    throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(imapFolder.getMailboxId());

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection(mbox);

            stmt = conn.prepareStatement(
                "UPDATE " + getTableName(mbox) +
                " SET local_path = ?, remote_path = ?, uid_validity = ?" +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "data_source_id = ? AND item_id = ?");
            int pos = 1;
            stmt.setString(pos++, imapFolder.getLocalPath());
            stmt.setString(pos++, imapFolder.getRemotePath());
            stmt.setLong(pos++, imapFolder.getUidValidity());
            if (!DebugConfig.disableMailboxGroups)
                stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, imapFolder.getDataSourceId());
            stmt.setInt(pos++, imapFolder.getItemId());
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
        ZimbraLog.datasource.info("Deleting IMAP data for DataSource %s", dataSourceId);

        if (StringUtil.isNullOrEmpty(dataSourceId))
            return;

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Note: data in imap_message gets deleted implicitly by the
            // foreign key cascading delete
            conn = DbPool.getConnection(mbox);

            stmt = conn.prepareStatement(
                "DELETE FROM " + getTableName(mbox) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "data_source_id = ?");
            int pos = 1;
            if (!DebugConfig.disableMailboxGroups)
                stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, dataSourceId);
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
        ZimbraLog.datasource.info("Deleting IMAP data for %s in %s", folder, ds);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Note: data in imap_message gets deleted implicitly by the
            // foreign key cascading delete
            conn = DbPool.getConnection(mbox);

            stmt = conn.prepareStatement(
                "DELETE FROM " + getTableName(mbox) +
                " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "data_source_id = ? and item_id = ?");
            int pos = 1;
            if (!DebugConfig.disableMailboxGroups)
                stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, ds.getId());
            stmt.setInt(pos++, folder.getItemId());
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
        return DbMailbox.qualifyTableName(groupId, TABLE_IMAP_FOLDER);
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_IMAP_FOLDER);
    }
}
