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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.datasource.DataSourceTask;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Database persistence code for <tt>DataSourceTask</tt>s.
 *
 * @author bburtin
 */
public class DbDataSourceTask {
    
    public static String TABLE_DATA_SOURCE_TASK = "data_source_task";
    
    /**
     * Temporary implementation that doesn't perform database operations.  Used until
     * we add the data_source_task table to our schema.
     */
    public static DataSourceTask createDataSourceTask(Mailbox mbox,
                                                      String dataSourceId,
                                                      String accountId,
                                                      Date lastExecTime,
                                                      Date nextExecTime) {
        return new DataSourceTask(mbox.getId(), dataSourceId, accountId, lastExecTime, nextExecTime);
    }
    
    public static DataSourceTask createDataSourceTaskActual(Mailbox mbox,
                                                      String dataSourceId,
                                                      String accountId,
                                                      Date lastExecTime,
                                                      Date nextExecTime)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        ZimbraLog.datasource.debug(
            "Creating DataSourceTask: mboxId=%d, dataSourceId=%s, accountId=%s, lastExecTime=%s, nextExecTime=%s",
            mbox.getId(), dataSourceId, accountId, lastExecTime, nextExecTime);

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO " + TABLE_DATA_SOURCE_TASK  +
                " (mailbox_id, data_source_id, account_id, last_exec_time, next_exec_time) " +
                "VALUES (?, ?, ?, ?, ?)");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, dataSourceId);
            stmt.setString(3, accountId);
            Timestamp tsLast = null;
            if (lastExecTime != null) {
                tsLast = new Timestamp(lastExecTime.getTime());
            }
            Timestamp tsNext = new Timestamp(nextExecTime.getTime());
            stmt.setTimestamp(4, tsLast);
            stmt.setTimestamp(5, tsNext);
            stmt.executeUpdate();
            conn.commit();
            
            return new DataSourceTask(mbox.getId(), dataSourceId, accountId, lastExecTime, nextExecTime);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to create DataSourceTask", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
    
    /**
     * Temporary implementation that doesn't perform database operations.  Used until
     * we add the data_source_task table to our schema.
     */
    public static List<DataSourceTask> getAllDataSourceTasks() {
        return new ArrayList<DataSourceTask>();
    }
    
    /**
     * Returns all <tt>DataSourceTask</tt>s stored in the database.
     */
    public static List<DataSourceTask> getAllDataSourceTasksActual()
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<DataSourceTask> tasks = new ArrayList<DataSourceTask>();

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "SELECT mailbox_id, data_source_id, account_id, last_exec_time, next_exec_time " +
                "FROM " + TABLE_DATA_SOURCE_TASK +
                " ORDER BY next_exec_time");

            rs = stmt.executeQuery();
            while (rs.next()) {
                int mailboxId = rs.getInt("mailbox_id");
                String dataSourceId = rs.getString("data_source_id");
                String accountId = rs.getString("account_id");
                Date lastExecTime = null;
                Timestamp tsLast = rs.getTimestamp("last_exec_time");
                if (tsLast != null) {
                    lastExecTime = new Date(tsLast.getTime());
                }
                Timestamp tsNext = rs.getTimestamp("next_exec_time");
                Date nextExecTime = new Date(tsNext.getTime());
                tasks.add(new DataSourceTask(mailboxId, dataSourceId, accountId, lastExecTime, nextExecTime));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get all DataSourceTasks", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }

        ZimbraLog.datasource.info("Loaded %d scheduled data source tasks", tasks.size());
        return tasks;
    }

    /**
     * Temporary implementation that doesn't perform database operations.  Used until
     * we add the data_source_task table to our schema.
     */
    public void updateLastExecTime(String dataSourceId, Date lastExecTime) {
    }
    
    public void updateLastExecTimeActual(String dataSourceId, Date lastExecTime)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        ZimbraLog.datasource.debug("Setting last exec time to %s for DataSourceTask %s",
            lastExecTime, dataSourceId);

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "UPDATE  " + TABLE_DATA_SOURCE_TASK  +
                " SET last_exec_time = ? WHERE data_source_id = ?");
            Timestamp timestamp = null;
            if (lastExecTime != null) {
                timestamp = new Timestamp(lastExecTime.getTime());
            }
            stmt.setTimestamp(1, timestamp);
            stmt.setString(2, dataSourceId);
            int numRows = stmt.executeUpdate();
            if (numRows != 1) {
                String msg = String.format("Unexpected number of rows (%d) updated for DataSourceTask %s",
                    numRows, dataSourceId);
                throw ServiceException.FAILURE(msg, null);
            }
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to update DataSourceTask", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
    
    /**
     * Temporary implementation that doesn't perform database operations.  Used until
     * we add the data_source_task table to our schema.
     */
    public static void deleteDataSourceTask(String dataSourceId) {
    }
    
    public static void deleteDataSourceTaskActual(String dataSourceId)
    throws ServiceException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        ZimbraLog.datasource.debug("Deleting DataSourceTask %s", dataSourceId);

        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                "DELETE FROM " + TABLE_DATA_SOURCE_TASK  +
                " WHERE data_source_id = ?");
            stmt.setString(1, dataSourceId);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to update DataSourceTask", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
}
