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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
import java.sql.SQLException;
import java.sql.Timestamp;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxManager.MailboxLock;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

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
        int[] mailboxIds = MailboxManager.getInstance().getMailboxIds();
        
        ZimbraLog.mailbox.info(
            "Starting table maintenance.  MinRows=" + getMinRows() + ", MaxRows=" + getMaxRows() +
            ", GrowthFactor=" + getGrowthFactor());
        
        for (int i = 0; i < mailboxIds.length; i++) {
            int id = mailboxIds[i];
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(id);
            MailboxLock lock = null;
            String dbName = DbMailbox.getDatabaseName(mbox);
            DbResults results = DbUtil.executeQuery("SHOW TABLE STATUS FROM `" + dbName + "`");
            
            try {
                for (int row = 1; row <= results.size(); row++) {
                    String tableName = results.getString(row, "Name");
                    int numRows = results.getInt(row, "Rows");
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
                        if (lock == null) {
                            lock = MailboxManager.getInstance().beginMaintenance(mbox.getAccountId(), mbox.getId());
                        }
                        ZimbraLog.mailbox.info("Maintaining table " + tableName + summary);
                        if (maintainTable(mbox, tableName, numRows)) {
                            tableCount++;
                        }
                    } else {
                        ZimbraLog.mailbox.debug("Skipping table " + tableName + summary);
                    }
                }
            } finally {
                if (lock != null) {
                    // Always end maintenance successfully, since table maintenance
                    // doesn't modify the data
                    MailboxManager.getInstance().endMaintenance(lock, true, false);
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
            sql = "INSERT INTO  " + TABLE_NAME +
                " (database_name, table_name, maintenance_date, last_optimize_date, num_rows) " +
                " VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE maintenance_date = ?, last_optimize_date = ?, num_rows = ?";
            params = new Object[] {
                dbName, tableName, now, now, numRowsObject, now, now, numRowsObject };
        }
        else {
            sql = "INSERT INTO  " + TABLE_NAME +
            " (database_name, table_name, maintenance_date, num_rows) " +
            " VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE maintenance_date = ?, num_rows = ?";
            params = new Object[] {
                dbName, tableName, now, numRowsObject, now, numRowsObject };
        }
        
        DbUtil.executeUpdate(sql, params);
    }
    
    private static boolean maintainTable(Mailbox mbox, String tableName, int numRows)
    throws ServiceException {
        String dbName = DbMailbox.getDatabaseName(mbox);
        
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
            String error = 
                DbTableMaintenance.class.getName() + ".maintainTable(" +
                mbox.getId() + ", " + tableName + "): error while running '" + sql + "': " + e.toString();
            ZimbraLog.mailbox.error(error);
            return false;
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        
        return true;
    }
}
