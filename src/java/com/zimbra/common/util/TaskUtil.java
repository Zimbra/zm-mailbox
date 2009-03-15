/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

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
