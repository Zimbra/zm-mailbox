package com.zimbra.common.util;

import org.junit.Assert;
import org.junit.Test;

public class BoundedCachedThreadPoolTest {

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
        private BoundedCachedThreadPool pool;
        private int interval; //time interval in milliseconds between two stats
        
        StatsThread(BoundedCachedThreadPool pool, int interval) {
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
        BoundedCachedThreadPool pool = new BoundedCachedThreadPool("testPool", 5, 5000, 1000);
        Assert.assertTrue(pool.getNumActiveThreads() == 0);
        Assert.assertEquals(pool.getName(), "testPool");
        
        //Start the stats thread- dump out the stats in every 1 second!
        Thread statsThread = new StatsThread(pool, 1000);
        statsThread.start();
        
        //push the tasks
        for (int i = 0; i < 5; i++)
            pool.execute(new MyTask(5000));
        
        //Let all the tasks be scheduled 
        Thread.sleep(100);
       
        Assert.assertTrue(pool.getNumActiveThreads() == 5);

        pool.shutdown();
        
        Assert.assertTrue(pool.getNumActiveThreads() == 0);
    }
    
    @Test
    public void testShutdown() throws InterruptedException {
        BoundedCachedThreadPool pool = new BoundedCachedThreadPool("testPool", 5, 3000, 1000);
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
