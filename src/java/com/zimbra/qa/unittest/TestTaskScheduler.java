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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.TaskScheduler;


public class TestTaskScheduler
extends TestCase
{
    private static class TestTask
    implements Callable<Integer> {
        static List<Integer> sResults =
            Collections.synchronizedList(new ArrayList<Integer>());
        
        int mValue;
        
        TestTask(int value) {
            mValue = value;
        }
        
        public Integer call() throws Exception {
            sResults.add(mValue);
            return mValue;
        }
    }
    
    /**
     * Submits the following tasks and confirms the result:
     * 
     * <ul>
     *   <li>0 ms: 1</li>
     *   <li>200ms: 2</li>
     *   <li>400ms: 2</li>
     *   <li>500ms: 1</li>
     *   <li>600ms: 2</li>
     *   <li>800ms: 2</li>
     * </ul>
     * @throws Exception
     */
    public void testTaskScheduler()
    throws Exception {
        
        TaskScheduler<Integer> scheduler = null;
        
        // Run test
        TestTask task1 = new TestTask(1);
        TestTask task2 = new TestTask(2);
        scheduler = new TaskScheduler<Integer>("TestTaskScheduler", 1, 2);
        scheduler.schedule(1, task1, true, 1000, 0);
        scheduler.schedule(2, task2, true, 400, 400);
        Thread.sleep(2000);
        scheduler.cancel(2, false);
        scheduler.cancel(1, false);
        int numResults = TestTask.sResults.size();

        // Wait some more to make sure no more tasks run
        Thread.sleep(500);
        assertEquals("Result count", TestTask.sResults.size(), numResults);

        // Validate results
        assertEquals("Task 1's last result", 1, scheduler.getLastResult(1).intValue());
        assertEquals("Task 2's last result", 2, scheduler.getLastResult(2).intValue());

        int[] expected = new int[] { 1, 2, 2, 1, 2, 2 };
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Result " + i, expected[i], TestTask.sResults.get(i).intValue());
        }
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(new TestSuite(TestTaskScheduler.class), System.out);
    }
}
