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
