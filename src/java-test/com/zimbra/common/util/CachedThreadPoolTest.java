/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.concurrent.RejectedExecutionException;

import org.junit.Assert;
import org.junit.Test;

public class CachedThreadPoolTest {

    private static class MyTask implements Runnable {
        private int execTime; //execution time in milliseconds!!
        
        public MyTask(int execTime) {
            this.execTime = execTime;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(execTime);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " Interrupted");
            }
        }
    }
    
    private static class StatsThread extends Thread {
        private CachedThreadPool pool;
        private int interval; //time interval in milliseconds between two stats
        
        StatsThread(CachedThreadPool pool, int interval) {
            this.pool = pool;
            this.interval = interval;
        }
        
        public void run() {
            try {
            while(true) {
                System.out.println("Active threads in pool: " + pool.getNumActiveThreads());
                Thread.sleep(interval);
            }
            } catch (InterruptedException e) {
                System.out.println("StatsThread interrupted...");
                return;
            }
        }
    }
    
    @Test
    public void testPoolSize() throws InterruptedException {
        CachedThreadPool pool = new CachedThreadPool("testPool", 5, 10, 5000, 1000);
        Assert.assertTrue(pool.getNumActiveThreads() == 0);
        Assert.assertEquals(pool.getName(), "testPool");
        
        //Start the stats thread- dump out the stats in every 1 second!
        Thread statsThread = new StatsThread(pool, 1000);
        statsThread.start();
        
        //push the tasks
        for (int i = 0; i < 5; i++)
            pool.execute(new MyTask(5000));
        
        //Let all the tasks be scheduled 
        Thread.sleep(1000);
       
        Assert.assertTrue(pool.getNumActiveThreads() == 5);

        pool.shutdown();
        
        Assert.assertTrue(pool.getNumActiveThreads() == 0);
    }
    
    @Test
    public void testPoolQueueSize() throws InterruptedException {
        CachedThreadPool pool = new CachedThreadPool("testPool", 1, 5, 10000, 1000);
        Assert.assertTrue(pool.getNumActiveThreads() == 0);
        Assert.assertEquals(pool.getName(), "testPool");
        
        boolean success = false;
        
        //push the tasks
        for (int i = 0; i < 10; i++) {
            try {
                pool.execute(new MyTask(5000));
            } catch (RejectedExecutionException e) {
                success = true;
                break;
            }
        }
        
        assert success;

        pool.shutdown();
        
        Assert.assertTrue(pool.getNumActiveThreads() == 0);
    }
    
    @Test
    public void testShutdown() throws InterruptedException {
        CachedThreadPool pool = new CachedThreadPool("testPool", 5, 10, 3000, 1000);
        Assert.assertTrue(pool.getNumActiveThreads() == 0);
        Assert.assertEquals(pool.getName(), "testPool");
        
        //Start the stats thread- dump out the stats in every 1 second!
        Thread statsThread = new StatsThread(pool, 1000);
        statsThread.start();
        
        //push some tasks
        for (int i = 0; i < 10; i++)
            pool.execute(new MyTask(5000));
        
        Thread.sleep(100);
        
        //call shutdown
        pool.shutdown();
        
        Assert.assertTrue(pool.getNumActiveThreads() > 0);        
    }

}
