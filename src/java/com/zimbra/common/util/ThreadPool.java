/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 */
public class ThreadPool implements Executor {
    private static Log mLog = LogFactory.getLog(ThreadPool.class);
    private static long TIMEOUT = 30 * 1000;

    private ThreadCounter mActiveThreadsCounter;
    private String mName;
    private ThreadPoolExecutor mPool;
    private long mTimeout;

    public ThreadPool(String name, int poolSize) {
        this(name, poolSize, TIMEOUT);
    }

    public ThreadPool(String name, int poolSize, long timeout) {
        mName = name;
        mTimeout = timeout;
        NamedThreadFactory tfac = new NamedThreadFactory(name,
            Thread.NORM_PRIORITY);
        mPool = new ThreadPoolExecutor(1, poolSize, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
        mPool.setThreadFactory(tfac);
        mActiveThreadsCounter = new ThreadCounter();
    }

    public String getName() { return mName; }

    /**
     * Returns the number of currently active worker threads. An active worker
     * thread is a thread that is currently executing a task, as opposed to a
     * thread that is waiting for a new task to execute.
     */
    public int getNumActiveThreads() {
        return mActiveThreadsCounter.getValue();
    }

    public void execute(Runnable task) throws RejectedExecutionException {
        mPool.execute(new CountedTask(task, mActiveThreadsCounter));
    }

    /**
     * Gracefully shutdown the thread pool by waiting for all pending tasks to
     * complete.
     */
    public void shutdown() {
        mPool.shutdown();
        awaitTermination();
    }

    public void shutdownNow() {
        mPool.shutdownNow();
        awaitTermination();
    }

    private void awaitTermination() {
        try {
            if (!mPool.awaitTermination(mTimeout, TimeUnit.MILLISECONDS))
                mLog.warn("Thread pool did not terminate within " + mTimeout +
                    " milliseconds");
        } catch (InterruptedException e) {
            mLog.warn("InterruptedException waiting for thread pool shutdown",
                e);
        }
    }

    private class ThreadCounter {
        private int mCount;

        public ThreadCounter() {
            mCount = 0;
        }

        public synchronized int getValue() {
            return mCount;
        }

        public synchronized void inc() {
            mCount++;
        }

        public synchronized void dec() {
            mCount--;
            if (mCount <= 0)
                notifyAll();
        }
    }

    /**
     * Wrapper class that adds counting logic around a task. Thread pool
     * executes all tasks using this wrapper class to keep track of the number
     * of active worker threads.
     */
    public class CountedTask implements Runnable {
        ThreadCounter mCounter;
        Runnable mTask;

        public CountedTask(Runnable task, ThreadCounter counter) {
            mTask = task;
            mCounter = counter;
        }

        public Runnable getTask() {
            return mTask;
        }

        public void run() {
            mCounter.inc();
            mTask.run();
            mCounter.dec();
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private String mName;
        private int mPriority;
        private int mThreadNumber;

        public NamedThreadFactory(String name, int priority) {
            mName = name;
            mThreadNumber = 0;
            mPriority = priority;
        }

        public Thread newThread(Runnable command) {
            int n;
            
            synchronized (this) {
                n = ++mThreadNumber;
            }
            StringBuffer sb = new StringBuffer(mName);
            sb.append('-').append(n);
            Thread t = new Thread(command, sb.toString());
            t.setPriority(mPriority);
            return t;
        }
    }
}
