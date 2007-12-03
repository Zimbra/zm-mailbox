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
        final Thread thread = Thread.currentThread();
        TimerTask task = new TimerTask() {
            public void run() {
                thread.interrupt();
            }
        };
        getTimer().schedule(task, timeout);
        try {
            V result = c.call();
            // If timeout task cannot be cancelled, just wait to be interrupted
            if (!task.cancel()) new Object().wait();
            return result;
        } catch (InterruptedException e) {
            throw new TimeoutException();
        }
    }
}
