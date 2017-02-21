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

import java.util.concurrent.Callable;

import junit.framework.TestCase;

import com.zimbra.common.util.TaskScheduler;

public class TestTaskScheduler
extends TestCase
{
    private class TestTask
    implements Callable<Integer> {
        int mNumCalls = 0;
        String mName;
        
        public Integer call() throws Exception {
            mNumCalls++;
            return mNumCalls;
        }
    }
    
    /**
     * Submits two tasks and confirms that they were executed the correct
     * number of times.
     */
    public void testTaskScheduler()
    throws Exception {
        
        TaskScheduler<Integer> scheduler = null;
        
        // Run test.
        TestTask task1 = new TestTask();
        TestTask task2 = new TestTask();
        scheduler = new TaskScheduler<Integer>("TestTaskScheduler", 1, 2);
        scheduler.schedule(1, task1, true, 1000, 0);
        scheduler.schedule(2, task2, true, 1500, 1500);
        Thread.sleep(1800);
        scheduler.cancel(2, false);
        scheduler.cancel(1, false);

        // Wait some more to make sure no more tasks run.
        Thread.sleep(1000);

        // Validate number of calls.
        assertEquals("Task 1 calls", 2, scheduler.getLastResult(1).intValue());
        assertEquals("Task 2 calls", 1, scheduler.getLastResult(2).intValue());
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestTaskScheduler.class);
    }
}
