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
package com.zimbra.cs.mailbox;

import java.util.Random;
import java.util.concurrent.Callable;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ScheduledTaskCallback;
import com.zimbra.common.util.TaskScheduler;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbScheduledTask;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.alerts.CalItemReminderTaskCallback;

/**
 * Manages persistent scheduled tasks.  Properties of recurring tasks
 * are stored in a database table, to allow the tasks to be rescheduled
 * on server startup. 
 */
public class ScheduledTaskManager {

    private static TaskScheduler<ScheduledTaskResult> sScheduler;
    private static Random sRandom = new Random();
    
    public static void startup()
    throws ServiceException {
        if (sScheduler != null) {
            ZimbraLog.scheduler.info("Scheduled tasks have already been initialized", new Exception());
            return;
        }
        
        // Start scheduled task threads
        Provisioning prov = Provisioning.getInstance();
        int numThreads = prov.getLocalServer().getIntAttr(Provisioning.A_zimbraScheduledTaskNumThreads, 20);
        int minThreads = numThreads / 2;
        sScheduler = new TaskScheduler<ScheduledTaskResult>(null, minThreads, numThreads);
        sScheduler.addCallback(new TaskCleanup());
        sScheduler.addCallback(new CalItemReminderTaskCallback());
        
        for (ScheduledTask task : DbScheduledTask.getTasks(null, 0)) {
            try {
                schedule(null, task);
            } catch (ServiceException e) {
                ZimbraLog.scheduler.warn("Unable to schedule %s.", task, e);
            }
        }
        
    }
    
    /**
     * Schedules a persistent task.
     * @param task the task
     */
    public static void schedule(ScheduledTask task)
    throws ServiceException {
        Connection conn = null;
        synchronized (DbMailbox.getSynchronizer()) {
            try {
                conn = DbPool.getConnection();
                schedule(conn, task);
                conn.commit();
            } finally {
                DbPool.quietClose(conn);
            }
        }
    }
    
    
    /**
     * Schedules a task.  If the task is a recurring task, the first execution time is
     * delayed a random amount of time between <tt>0</tt> and the recurrence interval.
     * 
     * @param conn a database connection used for persisting the task or <tt>null</tt> if
     * the task is not persistent
     * @param task the task
     * @see ScheduledTask#getIntervalMillis()
     */
    public static void schedule(Connection conn, ScheduledTask task)
    throws ServiceException {
        if (conn != null) {
            DbScheduledTask.createTask(conn, task);
        }
        
        if (task.isRecurring()) {
            // Delay each recurring task by a random time up to its recurrence interval,
            // so that all recurring tasks don't run at once.
            long delay = Math.abs(sRandom.nextLong()) % task.getIntervalMillis();
            sScheduler.schedule(getKey(task), task, true, task.getIntervalMillis(), delay);
        } else {
            if (task.getExecTime() == null) {
                throw ServiceException.FAILURE("Exec time not set for scheduled task.", null);
            }
            long delay = task.getExecTime().getTime() - System.currentTimeMillis();
            if (delay < 0) {
                delay = 0;
            }
            sScheduler.schedule(getKey(task), task, delay);
        }
    }
    
    public static ScheduledTask getTask(String className, String taskName, long mailboxId) {
        return (ScheduledTask) sScheduler.getTask(getKey(className, taskName, mailboxId));
    }
    
    /**
     * Cancels a persistent task.
     */
    public static ScheduledTask cancel(String className, String taskName, long mailboxId, boolean mayInterruptIfRunning)
    throws ServiceException {
        Connection conn = null;
        ScheduledTask task = null;
        synchronized (DbMailbox.getSynchronizer()) {
            try {
                conn = DbPool.getConnection();
                task = cancel(conn, className, taskName, mailboxId, mayInterruptIfRunning);
                conn.commit();
            } finally {
                DbPool.quietClose(conn);
            }
        }
        return task;
    }
    
    /**
     * Cancels a task.
     * 
     * @param conn a database connection used for deleting a persistent task,
     * or <tt>null</tt> if the task is not persistent
     * @return the task, or <tt>null</tt> if the task could not be found
     */
    public static ScheduledTask cancel(Connection conn, String className, String taskName,
                                       long mailboxId, boolean mayInterruptIfRunning)
    throws ServiceException {
        if (conn != null) {
            DbScheduledTask.deleteTask(conn, className, taskName);
        }
        return (ScheduledTask) sScheduler.cancel(getKey(className, taskName, mailboxId), mayInterruptIfRunning);
    }
    
    private static String getKey(String className, String taskName, long mailboxId) {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append(':').append(taskName);
        if (mailboxId > 0) {
            sb.append(':').append(mailboxId);
        }
        return sb.toString();
    }
    
    private static String getKey(ScheduledTask task) {
        return getKey(task.getClass().getName(), task.getName(), task.getMailboxId());
    }
    
    /**
     * Deletes a scheduled task from the database after the task it has run.
     * Only deletes single-run tasks, not recurring tasks. 
     */
    private static class TaskCleanup
    implements ScheduledTaskCallback<ScheduledTaskResult> {
        TaskCleanup()  { }

        public void afterTaskRun(Callable<ScheduledTaskResult> c, ScheduledTaskResult lastResult) {
            Connection conn = null;
            ScheduledTask task = (ScheduledTask) c;
            if (task.isRecurring()) {
                // Nothing to do
                return;
            }
            
            synchronized (DbMailbox.getSynchronizer()) {
                try {
                    conn = DbPool.getConnection();
                    DbScheduledTask.deleteTask(conn, task.getClass().getName(), task.getName());
                    conn.commit();
                } catch (ServiceException e) {
                    ZimbraLog.scheduler.warn("Unable to clean up %s", task, e);
                } finally {
                    DbPool.quietClose(conn);
                }
            }
        }
    }
}
