/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;


public class DbPop3Message {

    public static final String TABLE_POP3_MESSAGE = "pop3_message";
    
    /**
     * Persists <code>uid</code> so we remember not to import the message again.
     */
    public static void storeUid(Mailbox mbox, String dataSourceId, String uid, int itemId)
    throws ServiceException {
        synchronized (DbMailItem.getSynchronizer(mbox)) {
            if (StringUtil.isNullOrEmpty(uid))
                return;
    
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DbPool.getConnection(mbox);
    
                stmt = conn.prepareStatement(
                    "INSERT INTO " + getTableName(mbox) +
                    " (" + DbMailItem.MAILBOX_ID + "data_source_id, uid, item_id) " +
                    "VALUES (" + DbMailItem.MAILBOX_ID_VALUE + "?, ?, ?)");
                int pos = 1;
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setString(pos++, dataSourceId);
                stmt.setString(pos++, uid);
                stmt.setInt(pos++, itemId);
                stmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Unable to store UID", e);
            } finally {
                DbPool.closeStatement(stmt);
                DbPool.quietClose(conn);
            }
        }
    }

    /**
     * Deletes all persisted UID's for the given mailbox/data source.
     */
    public static void deleteUids(Mailbox mbox, String dataSourceId)
    throws ServiceException {
        synchronized (DbMailItem.getSynchronizer(mbox)) {
            ZimbraLog.mailbox.debug("Deleting UID's for %s", dataSourceId);
    
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DbPool.getConnection(mbox);
    
                stmt = conn.prepareStatement(
                    "DELETE FROM " + getTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "data_source_id = ?");
                int pos = 1;
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setString(pos++, dataSourceId);
                int numRows = stmt.executeUpdate();
                conn.commit();
                ZimbraLog.mailbox.debug("Deleted %d UID's", numRows);
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Unable to delete UID's", e);
            } finally {
                DbPool.closeStatement(stmt);
                DbPool.quietClose(conn);
            }
        }
    }

    /**
     * Returns the map of persisted itemId to UID mappings
     */
    public static Map<Integer, String> getMappings(Mailbox mbox, String dataSourceId)
    throws ServiceException {
        synchronized (DbMailItem.getSynchronizer(mbox)) {
            Map<Integer, String> mappings = new HashMap<Integer, String>();
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
    
            ZimbraLog.mailbox.debug("get all POP mappings for %s", dataSourceId);
            try {
                conn = DbPool.getConnection(mbox);
                stmt = conn.prepareStatement(
                    "SELECT item_id, uid FROM " + getTableName(mbox) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + " data_source_id = ?");
                int pos = 1;
                pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                stmt.setString(pos++, dataSourceId);
                rs = stmt.executeQuery();
                while (rs.next())
                    mappings.put(rs.getInt(1), rs.getString(2));
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Unable to get UID's", e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
                DbPool.quietClose(conn);
            }
            ZimbraLog.mailbox.debug("Found %d POP mappings for %s", mappings.size(),
                dataSourceId);
            return mappings;
        }
    }

    /**
     * Returns the set of persisted UID's that are also in the <code>uids</code>
     * collection.
     */
    public static Set<String> getMatchingUids(Mailbox mbox, DataSource ds,
                                              Collection<String> uids)
    throws ServiceException {
        synchronized (DbMailItem.getSynchronizer(mbox)) {
            ZimbraLog.mailbox.debug("%s: looking for uids that match a set of size %d", ds, uids.size());
            
            List<List<String>> splitIds = ListUtil.split(uids, Db.getINClauseBatchSize());
            Set<String> matchingUids = new HashSet<String>();
    
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = DbPool.getConnection(mbox);
    
                for (List<String> curIds : splitIds) {
                    stmt = conn.prepareStatement(
                        "SELECT uid FROM " + getTableName(mbox) +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "data_source_id = ?" +
                        " AND " + DbUtil.whereIn("uid", curIds.size()));
                    int pos = 1;
                    pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                    stmt.setString(pos++, ds.getId());
                    for (String uid : curIds)
                        stmt.setString(pos++, uid);
                    rs = stmt.executeQuery();
    
                    while (rs.next())
                        matchingUids.add(rs.getString(1));
                    rs.close(); rs = null;
                    stmt.close(); stmt = null;
                }
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Unable to get UID's", e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
                DbPool.quietClose(conn);
            }
    
            ZimbraLog.mailbox.debug("Found %d matching UID's", matchingUids.size());
            return matchingUids;
        }
    }


    public static String getTableName(long mailboxId, long groupId) {
        return DbMailbox.qualifyTableName(groupId, TABLE_POP3_MESSAGE);
    }

    public static String getTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_POP3_MESSAGE);
    }
}
