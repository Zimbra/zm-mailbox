/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zimbra.common.util.StringUtil.isNullOrEmpty;

public final class TaskUtil {
    private static Timer timer;

    private TaskUtil() {}
    
    private static synchronized Timer getTimer() {
        if (timer == null) {
            timer = new Timer("task timeout timer", true);
        }
        return timer;
    }

    public static <V> V call(Callable<V> c, long timeout) throws Exception {
        TimeoutTask task = new TimeoutTask(Thread.currentThread());
        getTimer().schedule(task, timeout);
        try {
            return c.call();
        } catch (InterruptedException e) {
            throw new TimeoutException();
        } finally {
            // Stop timeout task so won't get interrupted. Also, clear
            // interrupt status in case we got interrupted right after
            // the call was completed.
            task.stop();
            Thread.interrupted();
        }
    }

    /**
     * Like {@link Executors#defaultThreadFactory()} except it creates daemon threads
     * and allows specifying the thread name prefix.
     *
     * @param threadNamePrefix a counter is appended to this value to generate a name for each new thread
     * @return the thread factory
     */
    public static ThreadFactory newDaemonThreadFactory(final String threadNamePrefix) {
        return new DaemonThreadFactory(threadNamePrefix);
    }

    /**
     * A modified version of {@link Executors.DefaultThreadFactory}
     * which creates daemon threads instead of user threads and allows specifying
     * the thread name prefix.
     */
    public static class DaemonThreadFactory implements ThreadFactory {
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final String namePrefix;

        DaemonThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = (isNullOrEmpty(name) ? "pool" : name) + "-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (!t.isDaemon())
                t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private static class TimeoutTask extends TimerTask {
        final Thread thread;
        final Object lock = new Object();
        boolean stopped;

        TimeoutTask(Thread t) {
            thread = t;
        }

        public void stop() {
            synchronized (lock) {
                if (stopped) return;
                cancel();
                stopped = true;
            }
        }
        
        public void run() {
            synchronized (lock) {
                if (stopped) return;
                thread.interrupt();
                stopped = true;
            }
        }
    }
}
