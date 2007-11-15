package com.zimbra.cs.mina;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MinaThreadFactory implements ThreadFactory {
    private final ThreadGroup mGroup;
    private final AtomicInteger mThreadCount = new AtomicInteger(1);
    private final String mPrefix;

    public MinaThreadFactory(String prefix) {
        SecurityManager sm = System.getSecurityManager();
        mPrefix = prefix + "-";
        mGroup = (sm != null) ?
            sm.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(mGroup, r, mPrefix + mThreadCount.getAndIncrement());
        t.setDaemon(false);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
