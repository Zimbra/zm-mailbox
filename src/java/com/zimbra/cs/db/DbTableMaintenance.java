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
import com.zimbra.cs.mailbox.Mailbox.MailboxLock;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public class DbTableMaintenance {
    
    public static String TABLE_NAME = "table_maintenance";

    public static String OPERATION_ANALYZE = "ANALYZE";
    public static String OPERATION_OPTIMIZE = "OPTIMIZE";
    
    public static int getMinRows()
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        return server.getIntAttr(Provisioning.A_liquidTableMaintenanceMinRows, 0);
    }
    
    public static int getMaxRows() 
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        return server.getIntAttr(Provisioning.A_liquidTableMaintenanceMaxRows, 0);
    }
    
    public static int getGrowthFactor()
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        return server.getIntAttr(Provisioning.A_liquidTableMaintenanceGrowthFactor, 0);
    }
    
    public static String getOperation()
    throws ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        String attr = Provisioning.A_liquidTableMaintenanceOperation; 
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
        int[] mailboxIds = Mailbox.getMailboxIds();
        
        ZimbraLog.mailbox.info(
            "Starting table maintenance.  MinRows=" + getMinRows() + ", MaxRows=" + getMaxRows() +
            ", GrowthFactor=" + getGrowthFactor());
        
        for (int i = 0; i < mailboxIds.length; i++) {
            int id = mailboxIds[i];
            String dbName = DbMailbox.getDatabaseName(id);
            DbResults results = DbUtil.executeQuery("SHOW TABLE STATUS FROM `" + dbName + "`");
            
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
                
                // Check for 0 rows is necessary, since (0 >= 0 * GrowthFactor) evaluates to true
                if (numRows > 0 && numRows >= lastNumRows * getGrowthFactor()) {
                    ZimbraLog.mailbox.info("Maintaining table " + tableName + summary);
                    Mailbox mbox = Mailbox.getMailboxById(id);
                    if (maintainTable(mbox, tableName, numRows)) {
                        tableCount++;
                    }
                } else {
                    ZimbraLog.mailbox.debug("Skipping table " + tableName + summary);
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
        String dbName = DbMailbox.getDatabaseName(mbox.getId());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        String sql = getOperation() + " TABLE `" + dbName + "`.`" + tableName + "`";
        ZimbraLog.mailbox.info("Running '" + sql + "'");
        MailboxLock lock = null;
        boolean success = false;
        
        try {
            lock = Mailbox.beginMaintenance(mbox.getAccountId(), mbox.getId());
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
            conn.commit();
            updateLastNumRows(dbName, tableName, numRows);
            success = true;
        } catch (SQLException e) {
            String error = 
                DbTableMaintenance.class.getName() + ".maintainTable(" +
                mbox.getId() + ", " + tableName + "): error while running '" + sql + "': " + e.toString();
            ZimbraLog.mailbox.error(error);
            return false;
        } finally {
            if (lock != null)
                Mailbox.endMaintenance(lock, success, false);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        
        return true;
    }
}
