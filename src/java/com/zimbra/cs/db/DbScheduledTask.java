/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.ScheduledTask;

/**
 * Database persistence code for <tt>DataSourceTask</tt>s.
 */
public class DbScheduledTask {
    
    public static String TABLE_SCHEDULED_TASK = "scheduled_task";
    
    /**
     * Saves the given task to the database.
     */
    public static void createTask(Connection conn, ScheduledTask task)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        ZimbraLog.scheduler.debug("Creating %s", task);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                "INSERT INTO " + TABLE_SCHEDULED_TASK  +
                " (class_name, name, mailbox_id, exec_time, interval_millis, metadata) " +
                "VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setString(1, task.getClass().getName());
            stmt.setString(2, task.getName());
            stmt.setLong(3, task.getMailboxId());
            stmt.setTimestamp(4, DbUtil.dateToTimestamp(task.getExecTime()));
            if (task.getIntervalMillis() > 0) {
                stmt.setLong(5, task.getIntervalMillis());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            stmt.setString(6, getEncodedMetadata(task));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to create " + task, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
    
    /**
     * Retrieves scheduled tasks from the database.
     * 
     * @param className the <tt>ScheduledTask</tt> class name, or <tt>null</tt>
     * for all classes
     * @param mailboxId the mailbox ID, or <tt>0</tt> for all mailboxes
     */
    public static List<ScheduledTask> getTasks(String className, long mailboxId)
    throws ServiceException {
        ZimbraLog.scheduler.debug("Retrieving tasks for class %s, mailbox %d", className, mailboxId);
        
        List<ScheduledTask> tasks = new ArrayList<ScheduledTask>();

        synchronized (DbMailbox.getSynchronizer()) {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = DbPool.getConnection();
                String sql =
                    "SELECT class_name, name, mailbox_id, exec_time, interval_millis, metadata " +
                    "FROM " + TABLE_SCHEDULED_TASK;
                if (className != null) {
                    sql += " WHERE class_name = ?";
                }
                if (mailboxId > 0) {
                    if (className == null) {
                        sql += " WHERE mailbox_id = ?";
                    } else {
                        sql += " AND mailbox_id = ?";
                    }
                }
                stmt = conn.prepareStatement(sql);
                int i = 1;
                if (className != null) {
                    stmt.setString(i++, className);
                }
                if (mailboxId > 0) {
                    stmt.setLong(i++, mailboxId);
                }
    
                rs = stmt.executeQuery();
                while (rs.next()) {
                    className = rs.getString("class_name");
                    String name = rs.getString("name");
                    ScheduledTask task = null;
                    
                    // Instantiate task
                    try {
                        Object obj = Class.forName(className).newInstance();
                        if (obj instanceof ScheduledTask) {
                            task = (ScheduledTask) obj;
                        } else {
                            ZimbraLog.scheduler.warn("Class %s is not an instance of ScheduledTask for task %s",
                                className, name);
                            continue;
                        }
                    } catch (Exception e) {
                        ZimbraLog.scheduler.warn("Unable to instantiate class %s for task %s.  " +
                            "Class must be an instance of %s and have a constructor with no arguments.",
                            className, name, ScheduledTask.class.getSimpleName(), e);
                        continue;
                    }
                    
                    // Set member vars
                    task.setMailboxId(rs.getLong("mailbox_id"));
                    task.setExecTime(DbUtil.timestampToDate(rs.getTimestamp("exec_time")));
                    task.setIntervalMillis(rs.getLong("interval_millis"));
                    
                    try {
                        setProperties(task, rs.getString("metadata"));
                    } catch (ServiceException e) {
                        ZimbraLog.scheduler.warn("Unable to read metadata for %s.  Not scheduling this task.", task, e);
                        continue;
                    }
                    
                    tasks.add(task);
                }
            } catch (SQLException e) {
                throw ServiceException.FAILURE("Unable to get all DataSourceTasks", e);
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
                DbPool.quietClose(conn);
            }
        }

        ZimbraLog.scheduler.info("Loaded %d scheduled data source tasks", tasks.size());
        return tasks;
    }

    public static void updateTask(Connection conn, ScheduledTask task)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));
        
        ZimbraLog.scheduler.debug("Updating %s", task);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                "UPDATE  " + TABLE_SCHEDULED_TASK  +
                " SET mailbox_id = ?, exec_time = ?, interval_millis = ?, metadata = ? " +
                "WHERE class_name = ? AND name = ?");
            stmt.setLong(1, task.getMailboxId());
            stmt.setTimestamp(2, DbUtil.dateToTimestamp(task.getExecTime()));
            if (task.getIntervalMillis() > 0) {
                stmt.setLong(3, task.getIntervalMillis());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, getEncodedMetadata(task));
            stmt.setString(5, task.getClass().getName());
            stmt.setString(6, task.getName());

            int numRows = stmt.executeUpdate();
            if (numRows != 1) {
                String msg = String.format("Unexpected number of rows (%d) updated for %s", numRows, task);
                throw ServiceException.FAILURE(msg, null);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to update " + task, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void deleteTask(String className, String taskName)
    throws ServiceException {
        synchronized (DbMailbox.getSynchronizer()) {
            Connection conn = null;
            try {
                conn = DbPool.getConnection();
                deleteTask(conn, className, taskName);
                conn.commit();
            } finally {
                DbPool.quietClose(conn);
            }
        }
    }

    public static void deleteTask(Connection conn, String className, String taskName)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));
        
        ZimbraLog.scheduler.debug("Deleting scheduled task from the database.  className=%s, taskName=%s",
            className, taskName);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                "DELETE FROM " + TABLE_SCHEDULED_TASK  +
                " WHERE class_name = ? AND name = ?");
            stmt.setString(1, className);
            stmt.setString(2, taskName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete scheduled task: className=" +
                className+ ", taskName=" + taskName, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
    
    private static String getEncodedMetadata(ScheduledTask task) {
        boolean hasProperties = false;
        Metadata metadata = new Metadata();
        Iterator<String> it = task.getPropertyNames().iterator();
        while (it.hasNext()) {
            String key = it.next();
            hasProperties = true;
            metadata.put(key, task.getProperty(key));
        }
        if (!hasProperties) {
            return null;
        }
        return metadata.toString();
    }
    
    private static void setProperties(ScheduledTask task, String encodedMetadata)
    throws ServiceException {
        if (StringUtil.isNullOrEmpty(encodedMetadata)) {
            return;
        }
        
        Metadata metadata = new Metadata(encodedMetadata);
        Map map = metadata.asMap();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (key instanceof String && value instanceof String) {
                task.setProperty((String) key, (String) value);
            }
        }
    }
}
