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

package com.zimbra.cs.stats;

import java.util.Map;

import org.eclipse.jetty.util.thread.ThreadPool;

import com.google.common.collect.Maps;
import com.zimbra.common.jetty.JettyMonitor;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * Returns stats about Jetty threads and connections.
 */
public class JettyStats
implements RealtimeStatsCallback {

    private static Log log = LogFactory.getLog(JettyStats.class);
    
    @Override
    public Map<String, Object> getStatData() {
        ThreadPool pool = JettyMonitor.getThreadPool();
        if (pool == null) {
            log.debug("Thread pool has not been initialized.  Not returning stat data.");
            return null;
        }
        Map<String, Object> data = Maps.newHashMap();
        data.put(ZimbraPerf.RTS_HTTP_THREADS, pool.getThreads());
        data.put(ZimbraPerf.RTS_HTTP_IDLE_THREADS, pool.getIdleThreads());
        return data;
    }
}
