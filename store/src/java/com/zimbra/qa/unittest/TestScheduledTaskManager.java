/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zimbra.common.util.Constants;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbScheduledTask;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.ScheduledTaskManager;

public class TestScheduledTaskManager {
    @Rule
    public static TestName testInfo = new TestName();

    static final String TASK_NAME = "TestTask";
    private static String USER_NAME;
    private static Mailbox mbox;


    @Before
    public void setUp() throws Exception {
        USER_NAME = String.format("%s-%s-user1", TestScheduledTaskManager.class.getSimpleName(), testInfo.getMethodName()).toLowerCase();
        tearDown();
        TestUtil.createAccount(USER_NAME);
        mbox = TestUtil.getMailbox(USER_NAME);
    }

    /**
     * Confirms that a single task is persisted to the database,
     * runs, and is then removed from the database automatically.
     */
    @Test
    public void testSingleTask()
    throws Exception {
        checkNumPersistedTasks(0);

        // Schedule a single-execution task
        TestTask task = new TestTask();
        long now = System.currentTimeMillis();
        task.setExecTime(new Date(now + 1000));
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
    @Test
    public void testRecurringTask()
    throws Exception {
        checkNumPersistedTasks(0);

        // Schedule a recurring task
        TestTask task = new TestTask();
        task.setIntervalMillis(200);
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

    @Test
    public void testTaskProperties()
    throws Exception {
        checkNumPersistedTasks(0);

        // Schedule a single-execution task
        ScheduledTask task = new TestTask();
        long now = System.currentTimeMillis();
        task.setExecTime(new Date(now + Constants.MILLIS_PER_MINUTE));
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

    @After
    public void tearDown() throws Exception {
        if (mbox != null) {
            ScheduledTaskManager.cancel(TestTask.class.getName(), TASK_NAME, mbox.getId(), true);
        }
        TestUtil.deleteAccount(USER_NAME);
    }

    private void checkNumPersistedTasks(int expected)
    throws Exception {
        DbResults results = DbUtil.executeQuery(
            "SELECT COUNT(*) FROM " + DbScheduledTask.TABLE_SCHEDULED_TASK +
            " WHERE class_name = '" + TestTask.class.getName() + "'");
        assertEquals("Unexpected number of persisted tasks", expected, results.getInt(1));
    }
}
