/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.zimbra.common.util.ThreadPool.CountedTask;
import com.zimbra.common.util.ThreadPool.NamedThreadFactory;
import com.zimbra.common.util.ThreadPool.ThreadCounter;

/**
 * com.zimbra.common.util.ThreadPool works in the following way:
 * <ul>
 *  <li> # of threads < corePoolSize; create a new Thread to run a new task.
 *  <li> # of threads >= the corePoolSize, put the task into the queue.
 *  <li> If the queue is full, and the number of threads is less than the 
 * maxPoolSize, create a new thread to run tasks in.
 *  <li> If the queue is full, and the number of threads is greater than or equal to
 * maxPoolSize, reject the task.
 * </ul>
 * <p>
 * New threads are only created when the queue fills up. Hence, if we are using an
 * unbounded queue like LinkedBlockingQueue, then # of threads will not exceed
 * corePoolSize which is hard-coded to 1.
 * <p>
 * 
 * CachedThreadPool uses a cachedThreadPool but sets of a limit on approximate max number 
 * of threads can be spawned. Also, it sets of a limit on max # of tasks can be queued.
 * CachedThreadPool solves the problem in the following way:
 * <ul>
 *  <li> Uses a unbounded LinkedBlockingQueue to queue tasks from the caller threads
 *  <li> A sweeper thread sweeps tasks from LinkedBlocking Queue and executes them on a CachedThreadPool
 *  <li> The sweeper blocks on executor if the # of threads on the CachedThreadPool reaches the threshold
 * </ul>
 * 
 * @author smukhopadhyay
 *
 */
public class CachedThreadPool implements Executor {
    private static Log logger = LogFactory.getLog(CachedThreadPool.class);
    private static final int TIMEOUT = 30 * 1000;
    private static final int THROTTLE_TIME = 1 * 1000;
    private static final int UNLIMITED = -1;
    
    private String name;
    private int maxPoolSize; //approximate max # of threads in the pool
    private int maxQueueSize = UNLIMITED; //max # of tasks can be queued
    private int timeout; //Timeout (in milliseconds) for terminating thread pool!!
    private int throttleTime; //Time in milliseconds thread pool should wait before 
                              //taking the next sample when # of active threads > maxPoolSize
    private ExecutorService pool;
    private SweeperThread sweeper;
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    
    //Throttle!!
    private Lock lock = new ReentrantLock();
    private Condition cond = lock.newCondition();
    
    //Stats!!
    private ThreadCounter activeThreadsCounter = new ThreadCounter();
    
    //Shutdown!!
    private AtomicBoolean shutdown = new AtomicBoolean(false);
    private boolean isTerminated = false;
    
    public CachedThreadPool(String name, int maxPoolSize) {
        this(name, maxPoolSize, UNLIMITED, TIMEOUT, THROTTLE_TIME);
    }
    
    public CachedThreadPool(String name, int maxPoolSize, int maxQueueSize) {
        this(name, maxPoolSize, maxQueueSize, TIMEOUT, THROTTLE_TIME);
    }
    
    public CachedThreadPool(String name, int maxPoolSize, int maxQueueSize, int timeout, int throttleTime) {
        this.name = name;
        this.maxPoolSize = maxPoolSize;
        this.maxQueueSize = maxQueueSize;
        this.timeout = timeout;
        this.throttleTime = throttleTime;
        
        NamedThreadFactory tfac = new NamedThreadFactory(name, Thread.NORM_PRIORITY);
        pool = Executors.newCachedThreadPool(tfac);
        sweeper = new SweeperThread();
        sweeper.start();
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Returns the number of currently active worker threads executing tasks.
     * @return
     */
    public int getNumActiveThreads() {
        return activeThreadsCounter.getValue();
    }
    
    /**
     * Executes the tasks asynchronously by putting them onto the blocking queue.
     * Sweeper thread sweeps the tasks and executes them on CachedThreadPool.
     * @param task
     * @throws InterruptedException
     */
    public void execute(Runnable task) {
        if (!shutdown.get()) {
            try {
                if (maxQueueSize == UNLIMITED || (maxQueueSize > 0 && queue.size() < maxQueueSize)) {
                    queue.put(new CountedTask(task, activeThreadsCounter));
                } else {
                    throw new RejectedExecutionException("queue is full");
                }
            } catch (InterruptedException e) {
                throw new RejectedExecutionException(e);
            }
        } else
            throw new RejectedExecutionException("pool is already shutdown!!");
    }
    
    /**
     * All the tasks sitting on the queue are discarded and wait for all the pending task to
     * complete on the pool!!
     */
    public void shutdown() {
        shutdown.set(true);
        sweeper.interrupt();
        
        try {
            lock.lock();
            try {
                cond.await(timeout, TimeUnit.MILLISECONDS);
                if (!isTerminated) {
                    logger.warn(name + "thread pool didn't terminate...");
                }
            } catch (InterruptedException e) {
                return;
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void awaitTermination() {
        try {
            if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS))
                logger.warn(name + " thread pool did not terminate within " + timeout +
                    " milliseconds");
        } catch (InterruptedException e) {
            logger.warn("InterruptedException waiting for thread pool shutdown",
                e);
        }
    }
    
    private void throttle() throws InterruptedException {
        try {
            lock.lock();
            while(true) {
                if (getNumActiveThreads() >= maxPoolSize) {
                    //we need to wait for some time and check back again!!
                    cond.await(throttleTime, TimeUnit.MILLISECONDS);
                } else {
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    private final class SweeperThread extends Thread {
        
        public void run() {
            while (true) {
                try {
                    Runnable task = queue.take();
                    throttle(); //make sure the pool size is bounded!!
                    pool.execute(task);
                } catch (InterruptedException e) {
                    logger.warn("Sweeper thread interrupted");
                    if (shutdown.get()) {
                        logger.warn("Shutdown has been received by sweeper thread");
                        
                        //should we schedule all the pending tasks from the queue here ??
                        queue.clear();
                        
                        pool.shutdown();
                        awaitTermination();
                        
                        //somehow awaitTermination(..) is always returning false...
                        //But, we should be more worried abt the tasks not completed!!
                        if (getNumActiveThreads() > 0)
                            logger.warn(getNumActiveThreads() + " tasks pruned on thread pool " + name);
                        
                        //let others know we are finally terminated!!
                        try {
                            lock.lock();
                            isTerminated = true;
                            cond.signal();
                        } finally {
                            lock.unlock();
                        }
                        
                        return;
                    }
                    
                    assert false; //this should never happen
                }
            }
        }
    }

}
