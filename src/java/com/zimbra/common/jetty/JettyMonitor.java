/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.jetty;

import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * Holds on to a reference to the Jetty thread pool.  Called
 * by the Jetty startup code (see jetty.xml).
 */
public class JettyMonitor {

    private static ThreadPool threadPool = null;
    
    public synchronized static void setThreadPool(ThreadPool pool) {
        System.out.println(JettyMonitor.class.getSimpleName() + " monitoring thread pool " + pool);
        threadPool = pool;
    }
    
    public synchronized static ThreadPool getThreadPool() {
        return threadPool;
    }
}
