/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on 2004. 6. 23.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.common.util;

import java.util.Iterator;
import java.util.List;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronousChannel;
import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ThreadPool implements Executor {

	private static Log mLog = LogFactory.getLog(ThreadPool.class);

	private String mName;
	private PooledExecutor mPool;
	private Channel mChannel;
	private ThreadCounter mActiveThreadsCounter;

	public ThreadPool(String name, int poolSize) {
		this(name, poolSize, Thread.NORM_PRIORITY, null);
	}

	public ThreadPool(String name, int poolSize, int threadPriority, Channel channel) {
		mName = name;

		if (channel != null)
			mChannel = channel;
		else
			mChannel = new SynchronousChannel();
        NamedThreadFactory tfac = new NamedThreadFactory(name, threadPriority);
		mPool = new PooledExecutor(mChannel, poolSize);
        mPool.setThreadFactory(tfac);
		mPool.waitWhenBlocked();
		mPool.setKeepAliveTime(-1);
		mPool.setMinimumPoolSize(poolSize);
		mPool.createThreads(poolSize);

		mActiveThreadsCounter = new ThreadCounter();
	}

	public void execute(Runnable task) throws InterruptedException {
		mPool.execute(new CountedTask(task, mActiveThreadsCounter));
	}

	/**
	 * Gracefully shutdown the thread pool by waiting for all pending
	 * tasks to complete.
	 *
	 */
	public void shutdown() {
		mPool.shutdownAfterProcessingCurrentlyQueuedTasks();

		// In the worst case scenario, all worker threads in the pool
		// are blocked trying to get a task from the channel.  Unblock
		// them by offering a dummy work item.
    	int numThreads = mPool.getPoolSize();
    	for (int i = 0; i < numThreads; i++) {
    		DummyTask dummy = new DummyTask();
    		boolean offered = false;
    		try {
    			mChannel.offer(new CountedTask(dummy, mActiveThreadsCounter), 100);
    		} catch (InterruptedException e) {
    			// ignore
    		}
    	}

    	// Did we fail to perform any non-dummy tasks?
    	List tasks = mPool.drain();
    	for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
    		Object task = iter.next();
    		if (task instanceof DummyTask)
    			iter.remove();
    	}

    	try {
    		long timeout = 30;
    		boolean b = mPool.awaitTerminationAfterShutdown(timeout * 1000);
    		if (!b) {
    			mLog.warn("Thread pool did not terminate within " + timeout + " seconds");
    		}
		} catch (InterruptedException e) {
			mLog.warn("InterruptedException waiting for thread pool shutdown", e);
		}

		if (tasks.size() > 0) {
			mLog.info("Finishing up remaining " + tasks.size() + " tasks after thread pool termination");
			for (Iterator iter = tasks.iterator(); iter.hasNext(); ) {
				Runnable task = (Runnable) iter.next();
				task.run();
			}
		}
	}

	public void shutdownNow() {
		mPool.shutdownNow();

		// In the worst case scenario, all worker threads in the pool
		// are blocked trying to get a task from the channel.  Unblock
		// them by offering a dummy work item.
    	int numThreads = mPool.getPoolSize();
    	for (int i = 0; i < numThreads; i++) {
    		DummyTask dummy = new DummyTask();
    		boolean offered = false;
    		try {
    			mChannel.offer(new CountedTask(dummy, mActiveThreadsCounter), 100);
    		} catch (InterruptedException e) {
    			// ignore
    		}
    	}

    	try {
    		long timeout = 30;
    		boolean b = mPool.awaitTerminationAfterShutdown(timeout * 1000);
    		if (!b) {
    			mLog.warn("Thread pool did not terminate within " + timeout + " seconds");
    		}
		} catch (InterruptedException e) {
			mLog.warn("InterruptedException waiting for thread pool shutdown", e);
		}
	}

	/**
	 * Wait until task queue becomes empty.  There will be no more tasks
	 * to execute, but an empty queue doesn't mean all previously submitted
	 * tasks are finished, as some of the worker threads may still be
	 * executing the last tasks given to them.
	 * 
	 * @param timeoutMS timeout in milliseconds, negative number for
	 *                  no timeout
	 * @return true if pool became idle within timeoutMS, false otherwise
	 */
	public boolean waitUntilQueueEmpty(long timeoutMS) {
		long timeoutAt = System.currentTimeMillis();
		if (timeoutMS >= 0)
			timeoutAt += timeoutMS;

		while (true) {
			Object task = mChannel.peek();
			if (task == null)
				return true;
			if (timeoutMS >= 0) {
				long now = System.currentTimeMillis();
				if (now >= timeoutAt)
					return false;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}

	/**
	 * Wait indefinitely until task queue becomes empty.
	 */
	public void waitUntilQueueEmpty() {
		waitUntilQueueEmpty(-1);
	}

	/**
	 * Wait until thread pool has finished all tasks submitted to it.  Being
	 * idle means the task queue is empty and all worker threads are waiting
	 * for next task to perform.
	 *
	 * @param timeoutMS timeout in milliseconds, negative number for
	 *                  no timeout
	 * @return true if pool became idle within timeoutMS, false otherwise
	 */
	public boolean waitUntilIdle(long timeoutMS) {
		long startAt = System.currentTimeMillis();
		if (!waitUntilQueueEmpty(timeoutMS))
			return false;

		long emptyAt = System.currentTimeMillis();
		if (timeoutMS >= 0) {
			timeoutMS = timeoutMS - (emptyAt - startAt);
			if (timeoutMS <= 0)
				timeoutMS = 1;
		}
		
		return mActiveThreadsCounter.waitForZero(timeoutMS);
	}

	/**
	 * Wait indefinitely until thread pool becomes idle.
	 */
	public void waitUntilIdle() {
		waitUntilIdle(-1);
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

	// A command object whose sole purpose is to unblock worker threads
	// that are waiting to get something off the channel.  Used during
	// thread pool shutdown.
	private class DummyTask implements Runnable {
		public void run() {
			// do nothing
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
