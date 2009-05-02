/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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

	private String mName;
	private ThreadPoolExecutor mPool;
	private ThreadCounter mActiveThreadsCounter;

	public ThreadPool(String name, int poolSize) {
	    mName = name;
		NamedThreadFactory tfac = new NamedThreadFactory(name, Thread.NORM_PRIORITY);
		mPool = new ThreadPoolExecutor(poolSize, poolSize,
		                               Long.MAX_VALUE, TimeUnit.NANOSECONDS, 
		                               new LinkedBlockingQueue<Runnable>());
		mPool.setThreadFactory(tfac);
		mActiveThreadsCounter = new ThreadCounter();
	}

	public void execute(Runnable task) throws RejectedExecutionException {
	    mPool.execute(new CountedTask(task, mActiveThreadsCounter));
	}

	/**
	 * Gracefully shutdown the thread pool by waiting for all pending
	 * tasks to complete.
	 *
	 */
	public void shutdown() {
		mPool.shutdown();

    	try {
    		long timeout = 30;
    		boolean b = mPool.awaitTermination(timeout * 1000, TimeUnit.MILLISECONDS);
    		if (!b) {
    			mLog.warn("Thread pool did not terminate within " + timeout + " seconds");
    		}
		} catch (InterruptedException e) {
		    mLog.warn("InterruptedException waiting for thread pool shutdown", e);
		}
	}

	public void shutdownNow() {
		mPool.shutdownNow();
		try {
		    long timeout = 30;
		    boolean b = mPool.awaitTermination(timeout * 1000, TimeUnit.MILLISECONDS);
		    if (!b) {
		        mLog.warn("Thread pool did not terminate within " + timeout + " seconds");
		    }
		} catch (InterruptedException e) {
		    mLog.warn("InterruptedException waiting for thread pool shutdown", e);
		}
	}
	
	/**
	 * Returns the number of currently active worker threads.  An active
	 * worker thread is a thread that is currently executing a task, as
	 * opposed to a thread that is waiting for a new task to execute.
	 * @return
	 */
	public int getNumActiveThreads() {
		return mActiveThreadsCounter.getValue();
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

		public synchronized boolean waitForZero(long timeoutMS) {
			if (mCount <= 0)
				return true;
			try {
				if (timeoutMS >= 0)
					wait(timeoutMS);
				else
					wait();
			} catch (InterruptedException e) {
			}
			return mCount <= 0;
		}
	}

	/**
	 * Wrapper class that adds counting logic around a task.  Thread pool
	 * executes all tasks using this wrapper class to keep track of the
	 * number of active worker threads.
	 */
	public class CountedTask implements Runnable {
		Runnable mTask;
		ThreadCounter mCounter;

		public CountedTask(Runnable task, ThreadCounter counter) {
			mTask = task;
			mCounter = counter;
		}

		public void run() {
			mCounter.inc();
			mTask.run();
			mCounter.dec();
		}

		public Runnable getTask() {
			return mTask;
		}
	}

    private static class NamedThreadFactory implements ThreadFactory {

        private int mThreadNumber;
        private String mName;
        private int mPriority;

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
