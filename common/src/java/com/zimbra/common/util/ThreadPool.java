/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        this(name, 1, poolSize, timeout);
    }

    public static ThreadPool newCachedThreadPool(String name) {
        return new ThreadPool(name, 0, Integer.MAX_VALUE, TIMEOUT, new SynchronousQueue<>());
    }

    public ThreadPool(String name, int minPoolSize, int maxPoolSize, long timeout) {
        this(name, minPoolSize, maxPoolSize, timeout, new LinkedBlockingQueue<>());
    }

    private ThreadPool(String name, int minPoolSize, int maxPoolSize, long timeout, BlockingQueue<Runnable> workQueue) {
        mName = name;
        mTimeout = timeout;
        NamedThreadFactory tfac = new NamedThreadFactory(name,
            Thread.NORM_PRIORITY);
        mPool = new ZimbraThreadPoolExecutor(minPoolSize, maxPoolSize, 60, TimeUnit.SECONDS, workQueue);
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

    @Override
    public void execute(Runnable task) throws RejectedExecutionException {
        mPool.execute(task);
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

    public ExecutorService getExecutorService() {
        return mPool;
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

        @Override
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

        @Override
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

    private class ZimbraThreadPoolExecutor extends ThreadPoolExecutor {

        public ZimbraThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        @Override
        public void execute(Runnable command) {
            mLog.trace("submitting task to executor %s - %s active threads, %s queued", mName, mActiveThreadsCounter.getValue(), getQueue().size());
            super.execute(new CountedTask(command, mActiveThreadsCounter));
        }
    }
}
