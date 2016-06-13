/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import static com.zimbra.common.util.StringUtil.isNullOrEmpty;
import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.TaskRetry.RetryParams;

/**
 * Runs a <tt>Callable</tt> task either once or at a given interval.  Subsequent
 * start times for a task are based on the previous completion time.
 *
 * @param <V> the result type returned by the task
 */
public class TaskScheduler<V> {

    private static Map<Object, TaskRetry> retries = new HashMap<Object, TaskRetry>();

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
        RetryParams retryParams;

        TaskRunner(Object id, Callable<V2> task, boolean recurs, long intervalMillis, List<ScheduledTaskCallback<V2>> callbacks, RetryParams retry) {
            mTask = task;
            mId = id;
            mRecurs = recurs;
            mIntervalMillis = intervalMillis;
            mCallbacks = callbacks;
            retryParams = retry;
        }

        Callable<V2> getTask() { return mTask; }

        @Override
        public V2 call() throws Exception {
            long backoff = 0L;
            boolean retrying = false;
            try {
                ZimbraLog.scheduler.debug("Executing task %s", mId);
                mLastResult = mTask.call();
                TaskRetry retry = retries.remove(mId);
                if (retry == null) {
                    ZimbraLog.scheduler.debug("Task returned result %s", mLastResult);
                } else {
                    ZimbraLog.scheduler.debug("Task returned result %s after %d attempts", mLastResult, retry.getNumRetries());
                }

                if (mCallbacks != null) {
                    for (ScheduledTaskCallback<V2> callback : mCallbacks) {
                        callback.afterTaskRun(mTask, mLastResult);
                    }
                }
            } catch (Throwable t) {
                if (t instanceof OutOfMemoryError) {
                    ZimbraLog.scheduler.fatal("Shutting down", t);
                    System.exit(1);
                } else {
                    ZimbraLog.scheduler.warn("Exception during execution of task %s", mId, t);
                    mLastResult = null;
                    if (t instanceof ServiceException) {
                        TaskRetry retry = retries.get(mId);
                        if (retry == null && retryParams != null) {
                            retry = new TaskRetry(retryParams);
                            retries.put(mId, retry);
                        }
                        if (retry != null) {
                            if (retry.canRetry()) {
                                backoff = retry.getDelayMillis();
                                retry.increment();
                                ZimbraLog.scheduler.debug("Retrying task %s in %s ms", mId, backoff);
                                mSchedule = mThreadPool.schedule(this, backoff, TimeUnit.MILLISECONDS);
                                retrying = true;
                            } else {
                                ZimbraLog.scheduler.debug("reached retry limit for task %s", mId);
                            }
                        }
                    }
                }
            }

            boolean cancelled = false;
            if (mSchedule != null) {
                // mSchedule may have not been set by schedule() if the task runs immediately
                cancelled = mSchedule.isCancelled();
            }
            // Reschedule if this is a recurring task
            if (mRecurs && !cancelled && !retrying) {
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
        name = isNullOrEmpty(name) ? "ScheduledTask" : "ScheduledTask-" + name;
        mThreadPool = new ScheduledThreadPoolExecutor(corePoolSize, newDaemonThreadFactory(name));
        mThreadPool.setMaximumPoolSize(maximumPoolSize);
    }

    public void schedule(Object taskId, Callable<V> task, boolean recurs, long intervalMillis, long delayMillis) {
        schedule(taskId, task, recurs, intervalMillis, delayMillis, null);
    }

    /**
     * Schedules a task that runs once.
     * @param taskId the task id, used for looking up results and cancelling the task
     * @param task the task
     * @param delayMillis number of milliseconds to wait before executing the task
     * @param retry task retry configuration, to be used if the task throws a ServiceException
     *
     */
    public void schedule(Object taskId, Callable<V> task, long delayMillis, RetryParams retry) {
        schedule(taskId, task, false, 0, delayMillis, retry);
    }

    /**
     * Schedules a task.
     * @param taskId the task id, used for looking up results and canceling the task
     * @param task the task
     * @param recurs <tt>true</tt> if this is a recurring task
     * @param intervalMillis number of milliseconds between executions of a recurring
     * task, measured between the end of the last execution and the start of the next
     * @param delayMillis number of milliseconds to wait before the first execution
     * @param retry task retry configuration, to be used if the task throws a ServiceException
     */
    public void schedule(Object taskId, Callable<V> task, boolean recurs, long intervalMillis, long delayMillis, RetryParams retry) {
        ZimbraLog.scheduler.debug("Scheduling task %s", taskId);

        TaskRunner<V> runner = new TaskRunner<V>(taskId, task, recurs, intervalMillis, mCallbacks, retry);
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
