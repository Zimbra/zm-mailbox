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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;


public class DbImapMessage {

    public static final String TABLE_IMAP_MESSAGE = "imap_message";
    
    /**
     * Persists <code>uid</code> so we remember not to import the message again.
     */
    public static void storeUid(Mailbox mbox, String dataSourceId, long uid, int itemId)
    throws ServiceException
    {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO " + getTableName(mbox) +
                " (mailbox_id, data_source_id, uid, item_id) " +
                "VALUES (?, ?, ?, ?)");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, dataSourceId);
            stmt.setLong(3, uid);
            stmt.setInt(4, itemId);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to store UID", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    /**
     * Deletes all persisted UID's for the given mailbox/data source.
     */
    public static void deleteUids(Mailbox mbox, String dataSourceId)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;

        ZimbraLog.mailbox.debug("Deleting UID's for " + dataSourceId);
        
        if (StringUtil.isNullOrEmpty(dataSourceId)) {
            return;
        }
        
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "DELETE FROM " + getTableName(mbox) +
                " WHERE mailbox_id = ? AND data_source_id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, dataSourceId);
            int numRows = stmt.executeUpdate();
            conn.commit();
            ZimbraLog.mailbox.debug("Deleted " + numRows + " UID's");
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete UID's", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    /**
     * Returns the set of persisted UID's that are also in the <code>uids</code>
     * collection.
     */
    public static Set<Long> getMatchingUids(Mailbox mbox, DataSource ds,
                                            Collection<Long> uids)
    throws ServiceException {
        ZimbraLog.mailbox.debug(ds + ": looking for uids that match a set of size " + uids.size());
        
        List<List<Long>> splitIds = ListUtil.split(uids, DbUtil.IN_CLAUSE_BATCH_SIZE);
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Set<Long> matchingUids = new HashSet<Long>();

        try {
            conn = DbPool.getConnection();
            for (List<Long> curIds : splitIds) {
                stmt = conn.prepareStatement(
                    "SELECT uid " +
                    "FROM " + getTableName(mbox) +
                    " WHERE mailbox_id = ? AND data_source_id = ? AND uid IN " +
                    DbUtil.suitableNumberOfVariables(uids));

                int i = 1;
                stmt.setInt(i++, mbox.getId());
                stmt.setString(i++, ds.getId());
                for (Long uid : curIds) {
                    stmt.setLong(i++, uid);
                }
                rs = stmt.executeQuery();
                while (rs.next()) {
                    matchingUids.add(rs.getLong(1));
                }
                rs.close();
                stmt.close();
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get UID's", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }

        ZimbraLog.mailbox.debug(ds + ": found " + matchingUids.size() + " matching UID's");
        return matchingUids;
    }

    public static String getTableName(int mailboxId, int groupId) {
        return String.format("%s.%s", DbMailbox.getDatabaseName(mailboxId, groupId), TABLE_IMAP_MESSAGE);
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.getDatabaseName(mbox) + "." + TABLE_IMAP_MESSAGE;
    }
}
