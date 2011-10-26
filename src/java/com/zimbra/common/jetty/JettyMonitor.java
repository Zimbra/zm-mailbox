/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
