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
