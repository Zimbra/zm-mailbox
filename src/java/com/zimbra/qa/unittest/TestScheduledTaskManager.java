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
package com.zimbra.qa.unittest;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import com.zimbra.common.util.Constants;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbScheduledTask;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.ScheduledTaskManager;

public class TestScheduledTaskManager
extends TestCase {

    static final String TASK_NAME = "TestTask";
    private static final String USER_NAME = "user1";
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    /**
     * Confirms that a single task is persisted to the database,
     * runs, and is then removed from the database automatically.
     */
    public void testSingleTask()
    throws Exception {
        checkNumPersistedTasks(0);
        
        // Schedule a single-execution task
        TestTask task = new TestTask();
        long now = System.currentTimeMillis();
        task.setExecTime(new Date(now + 1000));
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        task.setMailboxId(mbox.getId());
        ScheduledTaskManager.schedule(task);
        
        // Make sure the task is persisted
        checkNumPersistedTasks(1);
        Thread.sleep(1250);
        
        assertEquals("TestTask was not called", 1, task.getNumCalls());
        checkNumPersistedTasks(0);
    }
    
    /**
     * Confirms that a recurring task is persisted to the database,
     * runs multiple times, and is then removed from the database
     * when cancelled.
     */
    public void testRecurringTask()
    throws Exception {
        checkNumPersistedTasks(0);
        
        // Schedule a recurring task
        TestTask task = new TestTask();
        task.setIntervalMillis(200);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        task.setMailboxId(mbox.getId());
        ScheduledTaskManager.schedule(task);
        
        // Make sure the task is persisted
        checkNumPersistedTasks(1);
        Thread.sleep(1000);
        
        // Cancel the task and make sure it's removed from the database
        ScheduledTaskManager.cancel(TestTask.class.getName(), TASK_NAME, mbox.getId(), false);
        Thread.sleep(200);
        int numCalls = task.getNumCalls();
        assertTrue("Unexpected number of task runs: " + numCalls, numCalls > 0);
        checkNumPersistedTasks(0);
        
        // Sleep some more and make sure the task doesn't run again
        Thread.sleep(400);
        assertEquals("Task still ran after being cancelled", numCalls, task.getNumCalls());
    }
    
    public void testTaskProperties()
    throws Exception {
        checkNumPersistedTasks(0);
        
        // Schedule a single-execution task
        ScheduledTask task = new TestTask();
        long now = System.currentTimeMillis();
        task.setExecTime(new Date(now + Constants.MILLIS_PER_MINUTE));
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        task.setMailboxId(mbox.getId());
        task.setProperty("prop1", "value1");
        task.setProperty("prop2", "value2");
        task.setProperty("prop3", null);
        ScheduledTaskManager.schedule(task);
        
        // Make sure the task is persisted
        checkNumPersistedTasks(1);
        
        // Check properties
        List<ScheduledTask> tasks = DbScheduledTask.getTasks(TestTask.class.getName(), mbox.getId());
        assertEquals(1, tasks.size());
        task = tasks.get(0);
        assertEquals("value1", task.getProperty("prop1"));
        assertEquals("value2", task.getProperty("prop2"));
        assertEquals(null, task.getProperty("prop3"));
        
        // Cancel task
        ScheduledTaskManager.cancel(TestTask.class.getName(), task.getName(), mbox.getId(), true);
        checkNumPersistedTasks(0);
    }

    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    public void cleanUp()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        ScheduledTaskManager.cancel(TestTask.class.getName(), TASK_NAME, mbox.getId(), true);
    }
    
    private void checkNumPersistedTasks(int expected)
    throws Exception {
        DbResults results = DbUtil.executeQuery(
            "SELECT COUNT(*) FROM " + DbScheduledTask.TABLE_SCHEDULED_TASK +
            " WHERE class_name = '" + TestTask.class.getName() + "'");
        assertEquals("Unexpected number of persisted tasks", expected, results.getInt(1));
    }
}
