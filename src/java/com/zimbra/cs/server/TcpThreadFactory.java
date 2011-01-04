/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.server;

import java.util.concurrent.ThreadFactory;

public class TcpThreadFactory implements ThreadFactory {
    private int count = 0;
    private final String prefix;
    private final boolean isDaemon;
    private final int priority;

    public TcpThreadFactory(String prefix, boolean isDaemon) {
        this(prefix, isDaemon, Thread.NORM_PRIORITY);
    }

    public TcpThreadFactory(String prefix, boolean isDaemon, int priority) {
        this.prefix = prefix;
        this.isDaemon = isDaemon;
        this.priority = priority;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        int n;
        synchronized (this) {
            n = ++count;
        }
        StringBuffer sb = new StringBuffer(prefix);
        sb.append('-').append(n);
        Thread t = new Thread(runnable, sb.toString());
        t.setDaemon(isDaemon);
        t.setPriority(priority);
        return t;
    }
}
