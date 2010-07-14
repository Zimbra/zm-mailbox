/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.tcpserver;

import java.util.concurrent.ThreadFactory;

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
