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
package com.zimbra.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.zimbra.common.util.Log.Level;

/**
 * Runs a <tt>Callable</tt> task either once or at a given interval.  Subsequent
 * start times for a task are based on the previous completion time.
 *
 * @param <V> the result type returned by the task 
 */
public class TaskScheduler<V> {
    
    private static Log sLog = LogFactory.getLog(TaskScheduler.class);
    
    static {
        sLog.setLevel(Level.DEBUG);
    }

    /**
     * A modified version of java.util.concurrent.Executors.DefaultThreadFactory
     * which creates daemon threads instead of user threads.
     */
    static class TaskSchedulerThreadFactory implements ThreadFactory {
        final ThreadGroup mGroup;
        final AtomicInteger mThreadNumber = new AtomicInteger(1);
        final String mNamePrefix;

        TaskSchedulerThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            mGroup = (s != null)? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
            mNamePrefix = "TaskScheduler-" + name + "-"; 
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(mGroup, r, 
                mNamePrefix + mThreadNumber.getAndIncrement(),
                0);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private class TaskRunner<V2>
    implements Callable<V2> {
        Object mId;
        boolean mRecurs;
        long mIntervalMillis;
        Callable<V2> mTask;
        V2 mLastResult;
        ScheduledFuture<V2> mSchedule;
        
        TaskRunner(Object id, Callable<V2> task, boolean recurs, long intervalMillis) {
            mTask = task;
            mId = id;
            mRecurs = recurs;
            mIntervalMillis = intervalMillis;
        }

        Callable<V2> getTask() { return mTask; }

        public V2 call() throws Exception {
            try {
                sLog.debug("Executing task %s", mId);
                mLastResult = mTask.call();
                sLog.debug("Task returned value %s", mLastResult);
            } catch (Throwable t) {
                if (t instanceof OutOfMemoryError) {
                    sLog.error("Shutting down", t);
                    System.exit(1);
                }
                sLog.warn("Exception during execution of task %s", mId, t);
                mLastResult = null;
            }
            
            // Reschedule if this is a recurring task
            if (mRecurs && !mSchedule.isCancelled()) {
                sLog.debug("Rescheduling task %s", mId);
                mSchedule = mThreadPool.schedule(this, mIntervalMillis, TimeUnit.MILLISECONDS);
            }
            return mLastResult;
        }
    }
    
    private Map<Object, TaskRunner<V>> mRunnerMap =
        Collections.synchronizedMap(new HashMap<Object, TaskRunner<V>>());
    
    private ScheduledThreadPoolExecutor mThreadPool;

    /**
     * Creates a new <tt>TaskScheduler</tt>
     * @param name the name to use when creating threads that run tasks
     * @param corePoolSize the minimum number of threads to allocate
     * @param maximumPoolSize the maximum number of threads to allocate
     * 
     * @see ScheduledThreadPoolExecutor#setCorePoolSize(int)
     * @see ScheduledThreadPoolExecutor#setMaximumPoolSize(int)
     */
    public TaskScheduler(String name, int corePoolSize, int maximumPoolSize) {
        mThreadPool = new ScheduledThreadPoolExecutor(corePoolSize, new TaskSchedulerThreadFactory(name));
        mThreadPool.setMaximumPoolSize(maximumPoolSize);
    }

    /**
     * Schedules a task that runs once.
     * @param taskId the task id, used for looking up results and cancelling the task
     * @param task the task
     * @param delayMillis number of milliseconds to wait before executing the task
     */
    public void schedule(Object taskId, Callable<V> task, long delayMillis) {
        schedule(taskId, task, false, 0, delayMillis);
    }
    
    /**
     * Schedules a task.
     * @param taskId the task id, used for looking up results and cancelling the task
     * @param task the task
     * @param recurs <tt>true</tt> if this is a recurring task
     * @param intervalMillis number of milliseconds between executions of a recurring
     * task, measured between the end of the last execution and the start of the next
     * @param delayMillis number of milliseconds to wait before the first execution
     */
    public void schedule(Object taskId, Callable<V> task, boolean recurs, long intervalMillis, long delayMillis) {
        TaskRunner<V> runner = new TaskRunner<V>(taskId, task, recurs, intervalMillis);
        runner.mSchedule = mThreadPool.schedule(runner, delayMillis, TimeUnit.MILLISECONDS);
        mRunnerMap.put(taskId, runner);
    }

    /**
     * Returns the result of the last task excecution
     * @param taskId
     */
    public V getLastResult(Object taskId) {
        TaskRunner<V> runner = mRunnerMap.get(taskId);
        if (runner == null) {
            return null;
        }
        return runner.mLastResult;
    }

    /**
     * Cancels a task.
     * @return the task, or <tt>null</tt> if the task couldn't be found.
     */
    public Callable<V> cancel(Object taskId, boolean mayInterruptIfRunning) {
        // Don't remove the task runner, so that users can get the last result
        // after a task has been canceled.
        TaskRunner<V> runner = mRunnerMap.get(taskId);
        if (runner == null) {
            return null;
        }
        sLog.debug("Cancelling task %s", taskId);
        runner.mSchedule.cancel(mayInterruptIfRunning);
        return runner.getTask();
    }
    
    /**
     * Shuts down the task scheduler.
     *
     * @see ScheduledThreadPoolExecutor#shutdown()
     */
    public void shutdown() {
        mThreadPool.shutdown();
    }
}
