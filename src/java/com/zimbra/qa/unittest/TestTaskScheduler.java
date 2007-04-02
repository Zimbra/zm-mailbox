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
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.TaskScheduler;

import junit.framework.TestCase;
import junit.framework.TestSuite;


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
        scheduler.schedule(1, task1, true, 500, 0);
        scheduler.schedule(2, task2, true, 200, 200);
        Thread.sleep(1000);
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
        CliUtil.toolSetup();
        TestUtil.runTest(new TestSuite(TestTaskScheduler.class), System.out);
    }
}
