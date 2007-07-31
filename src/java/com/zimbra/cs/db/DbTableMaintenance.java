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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
 * @author bburtin
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxManager.MailboxLock;

public class DbTableMaintenance {
    
    public static String TABLE_NAME = "table_maintenance";

    public static String OPERATION_ANALYZE = "ANALYZE";
    public static String OPERATION_OPTIMIZE = "OPTIMIZE";
    
    public static int getMinRows()
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        return server.getIntAttr(Provisioning.A_zimbraTableMaintenanceMinRows, 0);
    }
    
    public static int getMaxRows() 
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        return server.getIntAttr(Provisioning.A_zimbraTableMaintenanceMaxRows, 0);
    }
    
    public static int getGrowthFactor()
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        return server.getIntAttr(Provisioning.A_zimbraTableMaintenanceGrowthFactor, 0);
    }
    
    public static String getOperation()
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        String attr = Provisioning.A_zimbraTableMaintenanceOperation; 
        String operation = server.getAttr(attr);
        
        if (operation == null || operation.length() == 0) {
            throw ServiceException.FAILURE("No value specified for " + attr, null);
        }
        
        operation = operation.toUpperCase();
        if (!operation.equals(OPERATION_ANALYZE) &&
            !operation.equals(OPERATION_OPTIMIZE)) {
            ZimbraLog.mailbox.error(
                "Invalid table maintenance operation setting: '" + operation + "'."
                + "  Setting operation to " + OPERATION_ANALYZE);
            operation = OPERATION_ANALYZE;
        }
        return operation;
    }
    
    public static int runMaintenance()
    throws ServiceException {
        
        int tableCount = 0;
        
        ZimbraLog.mailbox.info("Starting table maintenance.  MinRows=%d, MaxRows=%d, GrowthFactor=%d",
            getMinRows(), getMaxRows(), getGrowthFactor());

        Map<Integer, Set<Integer>> ids = getIds();
        MailboxManager mgr = MailboxManager.getInstance();
        
        for (int groupId : ids.keySet()) {
            String dbName = DbMailbox.getDatabaseName(groupId);
            List<MailboxLock> locks = null;

            DbResults results = DbUtil.executeQuery("SHOW TABLE STATUS FROM `" + dbName + "`");
            try {
                for (int row = 1; row <= results.size(); row++) {
                    String tableName = results.getString(row, "TABLE_NAME");
                    int numRows = results.getInt(row, "TABLE_ROWS");
                    if (numRows < getMinRows() || numRows > getMaxRows()) {
                        // Skip tables that are too small or too big
                        ZimbraLog.mailbox.debug(
                            "Skipping table " + tableName + ", which has " + numRows + " rows.");
                        continue;
                    }
                    
                    int lastNumRows = getLastNumRows(dbName, tableName);
                    String summary = ".  lastNumRows=" + lastNumRows + ", numRows=" + numRows;
                    
                    // Check for 0 rows is required.  If minRows is set to 0, we'll
                    // unnecessarily run table maintenance on an empty table.
                    if (numRows > 0 && numRows >= lastNumRows * getGrowthFactor()) {
                        if (locks == null) {
                            locks = new ArrayList<MailboxLock>();
                            for (int mailboxId : ids.get(groupId)) {
                                try {
                                    Mailbox mbox = mgr.getMailboxById(mailboxId);
                                    locks.add(mgr.beginMaintenance(mbox.getAccountId(), mbox.getId()));
                                } catch (ServiceException e) {
                                    ZimbraLog.mailbox.warn("Unable to lock mailbox %d.", mailboxId, e);
                                }
                            }
                        }
                        ZimbraLog.mailbox.info("Maintaining table %s%s", tableName, summary);
                        if (maintainTable(dbName, tableName, numRows)) {
                            tableCount++;
                        }
                    } else {
                        ZimbraLog.mailbox.debug("Skipping table %s%s", tableName, summary);
                    }
                }
            } finally {
                if (locks != null) {
                    // Always end maintenance successfully, since table maintenance
                    // doesn't modify the data
                    for (MailboxLock lock : locks) {
                        MailboxManager.getInstance().endMaintenance(lock, true, false);
                    }
                }
            }
        }
        
        return tableCount;
    }
    
    private static int getLastNumRows(String dbName, String tableName)
    throws ServiceException {
        Object[] params = { dbName, tableName };
        DbResults results = DbUtil.executeQuery(
            "SELECT num_rows " +
            "FROM " + TABLE_NAME +
            " WHERE database_name = ? AND table_name = ?",
            params);
        if (results.size() == 0) {
            return 0;
        }
        return results.getInt(1);
    }
    
    private static void updateLastNumRows(String dbName, String tableName, int numRows)
    throws ServiceException {
        String sql;
        Object[] params;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Integer numRowsObject = new Integer(numRows);
        
        if (getOperation().equals(OPERATION_OPTIMIZE)) {
            sql = "INSERT INTO " + TABLE_NAME + " (database_name, table_name, maintenance_date, last_optimize_date, num_rows)" +
                " VALUES (?, ?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE maintenance_date = ?, last_optimize_date = ?, num_rows = ?";
            params = new Object[] { dbName, tableName, now, now, numRowsObject, now, now, numRowsObject };
        } else {
            sql = "INSERT INTO " + TABLE_NAME + " (database_name, table_name, maintenance_date, num_rows)" +
                " VALUES (?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE maintenance_date = ?, num_rows = ?";
            params = new Object[] { dbName, tableName, now, numRowsObject, now, numRowsObject };
        }
        
        DbUtil.executeUpdate(sql, params);
    }
    
    private static boolean maintainTable(String dbName, String tableName, int numRows)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        String sql = getOperation() + " TABLE `" + dbName + "`.`" + tableName + "`";
        ZimbraLog.mailbox.info("Running '" + sql + "'");
        
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
            conn.commit();
            updateLastNumRows(dbName, tableName, numRows);
        } catch (SQLException e) {
            ZimbraLog.mailbox.error("Error while maintaining table %s.%s.", dbName, tableName, e);
            return false;
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        
        return true;
    }

    /**
     * Returns all the mailbox id's and group id's.  The key is the group id
     * and the value is a set of all mailbox id's for that group.
     */
    private static Map<Integer, Set<Integer>> getIds()
    throws ServiceException {
        Map<Integer, Set<Integer>> ids = new TreeMap<Integer, Set<Integer>>();
    
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int currentGroup = 0;
        Set<Integer> mailboxIds = null;
        Connection conn = null;
        
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "SELECT group_id, id FROM mailbox ORDER BY group_id, id");
            rs = stmt.executeQuery();
            while (rs.next()) {
                // Get ids from current row
                int groupId = rs.getInt("group_id");
                int mailboxId = rs.getInt("id");
                
                // If this is a new group, create the set of mailbox id's for it 
                if (groupId != currentGroup) {
                    mailboxIds = new TreeSet<Integer>();
                    ids.put(groupId, mailboxIds);
                    currentGroup = groupId;
                }
                
                mailboxIds.add(mailboxId);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting mailbox and group id's", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    
        return ids;
    }
}
