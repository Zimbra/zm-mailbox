/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs a <tt>Callable</tt> task either once or at a given interval.  Subsequent
 * start times for a task are based on the previous completion time.
 *
 * @param <V> the result type returned by the task 
 */
public class TaskScheduler<V> {
    
    /**
     * A modified version of java.util.concurrent.Executors.DefaultThreadFactory
     * which creates daemon threads instead of user threads.
     */
    static class TaskSchedulerThreadFactory implements ThreadFactory {
        final ThreadGroup mGroup;
        final AtomicInteger mThreadNumber = new AtomicInteger(1);
        String mNamePrefix;

        TaskSchedulerThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            mGroup = (s != null)? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
            mNamePrefix = "ScheduledTask-";
            if (!StringUtil.isNullOrEmpty(name)) {
                mNamePrefix += name + "-"; 
            }
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

    /**
     * <tt>Callable</tt> wrapper that takes care of catching exceptions and
     * logging errors that occur while running the task.
     *
     * @param <V2> the type returned by the <tt>Callable</tt> task
     */
    private class TaskRunner<V2>
    implements Callable<V2> {
        Object mId;
        boolean mRecurs;
        long mIntervalMillis;
        Callable<V2> mTask;
        V2 mLastResult;
        ScheduledFuture<V2> mSchedule;
        List<ScheduledTaskCallback<V2>> mCallbacks;
        
        TaskRunner(Object id, Callable<V2> task, boolean recurs, long intervalMillis, List<ScheduledTaskCallback<V2>> callbacks) {
            mTask = task;
            mId = id;
            mRecurs = recurs;
            mIntervalMillis = intervalMillis;
            mCallbacks = callbacks;
        }

        Callable<V2> getTask() { return mTask; }

        public V2 call() throws Exception {
            try {
                ZimbraLog.scheduler.debug("Executing task %s", mId);
                mLastResult = mTask.call();
                ZimbraLog.scheduler.debug("Task returned result %s", mLastResult);

                if (mCallbacks != null) {
                    for (ScheduledTaskCallback<V2> callback : mCallbacks) {
                        callback.afterTaskRun(mTask, mLastResult);
                    }
                }
            } catch (Throwable t) {
                if (t instanceof OutOfMemoryError) {
                    ZimbraLog.scheduler.fatal("Shutting down", t);
                    System.exit(1);
                }
                ZimbraLog.scheduler.warn("Exception during execution of task %s", mId, t);
                mLastResult = null;
            }
            
            boolean cancelled = false;
            if (mSchedule != null) {
                // mSchedule may have not been set by schedule() if the task runs immediately 
                cancelled = mSchedule.isCancelled();
            }
            
            // Reschedule if this is a recurring task
            if (mRecurs && !cancelled) {
                ZimbraLog.scheduler.debug("Rescheduling task %s", mId);
                mSchedule = mThreadPool.schedule(this, mIntervalMillis, TimeUnit.MILLISECONDS);
            } else {
                ZimbraLog.scheduler.debug("Not rescheduling task %s.  mRecurs=%b", mId, mRecurs);
            }
            return mLastResult;
        }
    }
    
    private final Map<Object, TaskRunner<V>> mRunnerMap =
        Collections.synchronizedMap(new HashMap<Object, TaskRunner<V>>());
    private final ScheduledThreadPoolExecutor mThreadPool;
    private final List<ScheduledTaskCallback<V>> mCallbacks =
        Collections.synchronizedList(new ArrayList<ScheduledTaskCallback<V>>());

    /**
     * Creates a new <tt>TaskScheduler</tt>.  Task threads will be named
     * <tt>ScheduledTask-[name-]</tt>.
     * 
     * @param name the name to use when creating task threads, or <tt>null</tt>
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
     * @param taskId the task id, used for looking up results and canceling the task
     * @param task the task
     * @param recurs <tt>true</tt> if this is a recurring task
     * @param intervalMillis number of milliseconds between executions of a recurring
     * task, measured between the end of the last execution and the start of the next
     * @param delayMillis number of milliseconds to wait before the first execution
     */
    public void schedule(Object taskId, Callable<V> task, boolean recurs, long intervalMillis, long delayMillis) {
        ZimbraLog.scheduler.debug("Scheduling task %s", taskId);
        
        TaskRunner<V> runner = new TaskRunner<V>(taskId, task, recurs, intervalMillis, mCallbacks);
        runner.mSchedule = mThreadPool.schedule(runner, delayMillis, TimeUnit.MILLISECONDS);
        mRunnerMap.put(taskId, runner);
    }

    /**
     * Returns the <tt>Callable</tt> object for the given <tt>taskId</tt>, or <tt>null</tt>
     * if no matching task exists.
     */
    public Callable<V> getTask(Object taskId) {
        TaskRunner<V> runner = mRunnerMap.get(taskId);
        if (runner == null) {
            return null;
        }
        if (runner.mSchedule.isCancelled()) {
            return null;
        }
        return runner.getTask();
    }
    
    /**
     * Returns the result of the last task execution.
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
        ZimbraLog.scheduler.debug("Cancelling task %s", taskId);
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
    
    public void addCallback(ScheduledTaskCallback<V> callback) {
        mCallbacks.add(callback);
    }
}
