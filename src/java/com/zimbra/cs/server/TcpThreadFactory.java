/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
