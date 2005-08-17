package com.zimbra.cs.tcpserver;

import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

public class TcpThreadFactory implements ThreadFactory {
	private int mCount;
	private String mPrefix;
	private boolean mIsDaemon;
    private int mPriority;

	public TcpThreadFactory(String prefix, boolean isDaemon) {
        this(prefix, isDaemon, Thread.NORM_PRIORITY);
	}

    public TcpThreadFactory(String prefix, boolean isDaemon, int priority) {
        mCount = 0;
        mPrefix = prefix;
        mIsDaemon = isDaemon;
        mPriority = priority;
    }

	public Thread newThread(Runnable runnable) {
        int n;
        synchronized (this) {
            n = ++mCount;
        }
        StringBuffer sb = new StringBuffer(mPrefix);
        sb.append('-').append(n);
        Thread t = new Thread(runnable, sb.toString());
        t.setDaemon(mIsDaemon);
        t.setPriority(mPriority);
        return t;
	}
}
