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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.datasource.ImapFolder;
import com.zimbra.cs.datasource.ImapMessage;
import com.zimbra.cs.datasource.ImapMessageCollection;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;


public class DbImapMessage {

    public static final String TABLE_IMAP_MESSAGE = "imap_message";
    
    /**
     * Stores IMAP message tracker data.
     */
    public static void storeImapMessage(Mailbox mbox, int localFolderId, long remoteUid, int localItemId)
    throws ServiceException
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        ZimbraLog.datasource.debug(
            "Storing IMAP message tracker: mboxId=%d, localFolderId=%d, remoteUid=%d, localItemId=%d",
            mbox.getId(), localFolderId, remoteUid, localItemId);

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO " + getTableName(mbox) +
                " (mailbox_id, imap_folder_id, uid, item_id) " +
                "VALUES (?, ?, ?, ?)");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, localFolderId);
            stmt.setLong(3, remoteUid);
            stmt.setInt(4, localItemId);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to store IMAP message data", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    /**
     * Deletes IMAP message tracker data.
     */
    public static void deleteImapMessage(Mailbox mbox, int localFolderId, long remoteUid)
    throws ServiceException
    {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        ZimbraLog.datasource.debug(
            "Deleting IMAP message tracker: mboxId=%d, localFolderId=%d, remoteUid=%d",
            mbox.getId(), localFolderId, remoteUid);

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "DELETE FROM " + getTableName(mbox) +
                " WHERE mailbox_id = ? AND imap_folder_id = ? AND uid = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, localFolderId);
            stmt.setLong(3, remoteUid);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete IMAP message data", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    /**
     * Returns a collection of tracked IMAP messages for the given data source.
     */
    public static ImapMessageCollection getImapMessages(Mailbox mbox, DataSource ds, ImapFolder imapFolder)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ImapMessageCollection imapMessages = new ImapMessageCollection();

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "SELECT imap.uid, imap.item_id, mi.flags " +
                "FROM " + getTableName(mbox) + " imap " +
                "  LEFT OUTER JOIN " + DbMailItem.getMailItemTableName(mbox) + " mi " +
                "  ON imap.mailbox_id = mi.mailbox_id AND imap.item_id = mi.id " + 
                "WHERE imap.mailbox_id = ? AND imap.imap_folder_id = ?");

            int i = 1;
            stmt.setInt(i++, mbox.getId());
            stmt.setInt(i++, imapFolder.getItemId());
            rs = stmt.executeQuery();
            while (rs.next()) {
                long uid = rs.getLong("imap.uid");
                int itemId = rs.getInt("imap.item_id");
                int flags = rs.getInt("mi.flags");
                imapMessages.add(new ImapMessage(uid, itemId, flags));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get IMAP message data", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }

        ZimbraLog.mailbox.debug("Found %d tracked IMAP messages for %s",
            imapMessages.size(), imapFolder.getRemotePath());
        return imapMessages;
    }

    public static String getTableName(int mailboxId, int groupId) {
        return String.format("%s.%s", DbMailbox.getDatabaseName(mailboxId, groupId), TABLE_IMAP_MESSAGE);
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.getDatabaseName(mbox) + "." + TABLE_IMAP_MESSAGE;
    }
}
