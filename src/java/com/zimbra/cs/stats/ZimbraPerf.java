/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.Accumulator;
import com.zimbra.common.stats.Counter;
import com.zimbra.common.stats.DeltaCalculator;
import com.zimbra.common.stats.RealtimeStats;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.common.stats.StatsDumper;
import com.zimbra.common.stats.StatsDumperDataSource;
import com.zimbra.common.stats.StopWatch;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * A collection of methods for keeping track of server performance statistics.
 */
public class ZimbraPerf {

    static Log sLog = LogFactory.getLog(ZimbraPerf.class);

    public static final String RTS_DB_POOL_SIZE = "db_pool_size";
    public static final String RTS_INNODB_BP_HIT_RATE = "innodb_bp_hit_rate";
    
    public static final String RTS_POP_CONN = "pop_conn";
    public static final String RTS_POP_SSL_CONN = "pop_ssl_conn";
    public static final String RTS_IMAP_CONN = "imap_conn";
    public static final String RTS_IMAP_SSL_CONN = "imap_ssl_conn";
    public static final String RTS_SOAP_SESSIONS = "soap_sessions";
    public static final String RTS_MBOX_CACHE_SIZE = "mbox_cache_size";
    public static final String RTS_MSG_CACHE_SIZE = "msg_cache_size";
    public static final String RTS_MSG_CACHE_BYTES = "msg_cache_bytes";
    public static final String RTS_FD_CACHE_SIZE = "fd_cache_size";
    public static final String RTS_FD_CACHE_HIT_RATE = "fd_cache_hit_rate";
    
    // LDAP provisioning caches.
    public static final String RTS_ACCOUNT_CACHE_SIZE = "account_cache_size";
    public static final String RTS_ACCOUNT_CACHE_HIT_RATE = "account_cache_hit_rate";
    public static final String RTS_COS_CACHE_SIZE = "cos_cache_size";
    public static final String RTS_COS_CACHE_HIT_RATE = "cos_cache_hit_rate";
    public static final String RTS_DOMAIN_CACHE_SIZE = "domain_cache_size";
    public static final String RTS_DOMAIN_CACHE_HIT_RATE = "domain_cache_hit_rate";
    public static final String RTS_SERVER_CACHE_SIZE = "server_cache_size";
    public static final String RTS_SERVER_CACHE_HIT_RATE = "server_cache_hit_rate";
    public static final String RTS_ZIMLET_CACHE_SIZE = "zimlet_cache_size";
    public static final String RTS_ZIMLET_CACHE_HIT_RATE = "zimlet_cache_hit_rate";
    public static final String RTS_GROUP_CACHE_SIZE = "group_cache_size";
    public static final String RTS_GROUP_CACHE_HIT_RATE = "group_cache_hit_rate";
    public static final String RTS_XMPP_CACHE_SIZE = "xmpp_cache_size";
    public static final String RTS_XMPP_CACHE_HIT_RATE = "xmpp_cache_hit_rate";

    // Accumulators.  To add a new accumulator, create a static instance here and
    // add it to sAccumulators.
    public static final Counter COUNTER_LMTP_RCVD_MSGS = new Counter();
    public static final Counter COUNTER_LMTP_RCVD_BYTES = new Counter();
    public static final Counter COUNTER_LMTP_RCVD_RCPT = new Counter();
    public static final Counter COUNTER_LMTP_DLVD_MSGS = new Counter();
    public static final Counter COUNTER_LMTP_DLVD_BYTES = new Counter();
    public static final StopWatch STOPWATCH_DB_CONN = new StopWatch();
    public static final StopWatch STOPWATCH_LDAP_DC = new StopWatch();
    public static final StopWatch STOPWATCH_MBOX_ADD_MSG = new StopWatch();
    public static final StopWatch STOPWATCH_MBOX_GET = new StopWatch();         // Mailbox accessor response time
    public static final Counter COUNTER_MBOX_CACHE = new Counter();           // Mailbox cache hit rate
    public static final Counter COUNTER_MBOX_MSG_CACHE = new Counter(); 
    public static final Counter COUNTER_MBOX_ITEM_CACHE = new Counter();
    public static final StopWatch STOPWATCH_SOAP = new StopWatch();
    public static final StopWatch STOPWATCH_IMAP = new StopWatch();
    public static final StopWatch STOPWATCH_POP = new StopWatch();
    public static final Counter COUNTER_IDX_WRT = new Counter();
    public static final Counter COUNTER_IDX_WRT_OPENED = new Counter();
    public static final Counter COUNTER_IDX_WRT_OPENED_CACHE_HIT = new Counter();
    public static final Counter COUNTER_CALENDAR_CACHE_HIT = new Counter();
    public static final Counter COUNTER_CALENDAR_CACHE_MEM_HIT = new Counter();
    public static final Counter COUNTER_CALENDAR_CACHE_LRU_SIZE = new Counter();
    public static final Counter COUNTER_IDX_BYTES_WRITTEN = new Counter();
    public static final Counter COUNTER_IDX_BYTES_READ = new Counter();
    public static final Counter COUNTER_BLOB_INPUT_STREAM_READ = new Counter();
    public static final Counter COUNTER_BLOB_INPUT_STREAM_SEEK_RATE = new Counter();
    
    public static final ActivityTracker SOAP_TRACKER = new ActivityTracker("soap.csv");
    public static final ActivityTracker IMAP_TRACKER = new ActivityTracker("imap.csv");
    public static final ActivityTracker POP_TRACKER = new ActivityTracker("pop3.csv");
    
    private static int sMailboxCacheSize;
    private static long sMailboxCacheSizeTimestamp = 0;
    private static JmxServerStats sJmxServerStats;
    
    private static RealtimeStats sRealtimeStats = 
        new RealtimeStats(new String[] {
            RTS_DB_POOL_SIZE, RTS_INNODB_BP_HIT_RATE,
            RTS_POP_CONN, RTS_POP_SSL_CONN, RTS_IMAP_CONN, RTS_IMAP_SSL_CONN, RTS_SOAP_SESSIONS,
            RTS_MBOX_CACHE_SIZE, RTS_MSG_CACHE_SIZE, RTS_MSG_CACHE_BYTES,
            RTS_FD_CACHE_SIZE, RTS_FD_CACHE_HIT_RATE,
            RTS_ACCOUNT_CACHE_SIZE, RTS_ACCOUNT_CACHE_HIT_RATE,
            RTS_COS_CACHE_SIZE, RTS_COS_CACHE_HIT_RATE,
            RTS_DOMAIN_CACHE_SIZE, RTS_DOMAIN_CACHE_HIT_RATE,
            RTS_SERVER_CACHE_SIZE, RTS_SERVER_CACHE_HIT_RATE,
            RTS_ZIMLET_CACHE_SIZE, RTS_ZIMLET_CACHE_HIT_RATE,
            RTS_GROUP_CACHE_SIZE, RTS_GROUP_CACHE_HIT_RATE,
            RTS_XMPP_CACHE_SIZE, RTS_XMPP_CACHE_HIT_RATE }
        );

    private static CopyOnWriteArrayList<Accumulator> sAccumulators = 
        new CopyOnWriteArrayList<Accumulator>(
                    new Accumulator[] {
                        new DeltaCalculator(COUNTER_LMTP_RCVD_MSGS).setTotalName("lmtp_rcvd_msgs"),
                        new DeltaCalculator(COUNTER_LMTP_RCVD_BYTES).setTotalName("lmtp_rcvd_bytes"),
                        new DeltaCalculator(COUNTER_LMTP_RCVD_RCPT).setTotalName("lmtp_rcvd_rcpt"),
                        new DeltaCalculator(COUNTER_LMTP_DLVD_MSGS).setTotalName("lmtp_dlvd_msgs"),
                        new DeltaCalculator(COUNTER_LMTP_DLVD_BYTES).setTotalName("lmtp_dlvd_bytes"),
                        new DeltaCalculator(STOPWATCH_DB_CONN).setCountName("db_conn_count").setAverageName("db_conn_ms_avg"),
                        new DeltaCalculator(STOPWATCH_LDAP_DC).setCountName("ldap_dc_count").setAverageName("ldap_dc_ms_avg"),
                        new DeltaCalculator(STOPWATCH_MBOX_ADD_MSG).setCountName("mbox_add_msg_count").setAverageName("mbox_add_msg_ms_avg"),
                        new DeltaCalculator(STOPWATCH_MBOX_GET).setCountName("mbox_get_count").setAverageName("mbox_get_ms_avg"),
                        new DeltaCalculator(COUNTER_MBOX_CACHE).setAverageName("mbox_cache"),
                        new DeltaCalculator(COUNTER_MBOX_MSG_CACHE).setAverageName("mbox_msg_cache"),
                        new DeltaCalculator(COUNTER_MBOX_ITEM_CACHE).setAverageName("mbox_item_cache"),
                        new DeltaCalculator(STOPWATCH_SOAP).setCountName("soap_count").setAverageName("soap_ms_avg"),
                        new DeltaCalculator(STOPWATCH_IMAP).setCountName("imap_count").setAverageName("imap_ms_avg"),
                        new DeltaCalculator(STOPWATCH_POP).setCountName("pop_count").setAverageName("pop_ms_avg"),
                        new DeltaCalculator(COUNTER_IDX_WRT).setAverageName("idx_wrt_avg"),
                        new DeltaCalculator(COUNTER_IDX_WRT_OPENED).setTotalName("idx_wrt_opened"),
                        new DeltaCalculator(COUNTER_IDX_WRT_OPENED_CACHE_HIT).setTotalName("idx_wrt_opened_cache_hit"),
                        new DeltaCalculator(COUNTER_CALENDAR_CACHE_HIT).setAverageName("calcache_hit"),
                        new DeltaCalculator(COUNTER_CALENDAR_CACHE_MEM_HIT).setAverageName("calcache_mem_hit"),
                        new DeltaCalculator(COUNTER_CALENDAR_CACHE_LRU_SIZE).setAverageName("calcache_lru_size"),
                        new DeltaCalculator(COUNTER_IDX_BYTES_WRITTEN).setTotalName("idx_bytes_written").setAverageName("idx_bytes_written_avg"),
                        new DeltaCalculator(COUNTER_IDX_BYTES_READ).setTotalName("idx_bytes_read").setAverageName("idx_bytes_read_avg"),
                        new DeltaCalculator(COUNTER_BLOB_INPUT_STREAM_READ).setTotalName("bis_read"),
                        new DeltaCalculator(COUNTER_BLOB_INPUT_STREAM_SEEK_RATE).setAverageName("bis_seek_rate"),
                        sRealtimeStats
                    }
        );

    /**
     * Returns all the latest stats as a key-value <tt>Map</tt>.
     */
    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        
        List<Accumulator> accumulators = new ArrayList<Accumulator>();
        accumulators.addAll(sAccumulators);
        accumulators.add(sRealtimeStats);
        
        for (Accumulator a : accumulators) {
            List<String> names = a.getNames();
            List<Object> data = a.getData();
            for (int i = 0; i < names.size(); i++) {
                stats.put(names.get(i), data.get(i));
            }
        }
        
        return stats;
    }
    
    /**
     * This may only be called BEFORE ZimbraPerf.initialize is called, otherwise the column
     * names will not be output correctly into the logs
     */
    public static void addRealtimeStatName(String name) {
        if (sIsInitialized)
            throw new IllegalStateException("Cannot add stat name after ZimbraPerf.initialize() is called");
        sRealtimeStats.addName(name);
    }
    
    /**
     * This may only be called BEFORE ZimbraPerf.initialize is called, otherwise the column
     * names will not be output correctly into the logs
     */
    public static void addAccumulator(Accumulator toAdd) {
        if (sIsInitialized)
            throw new IllegalStateException("Cannot add stat name after ZimbraPerf.initialize() is called");
        sAccumulators.add(toAdd);
    }
    
    public static JmxServerStatsMBean getMonitoringStats() {
        return sJmxServerStats;
    }
    
    /**
     * The number of statements that were prepared, as reported by
     * {@link DbPool.Connection#prepareStatement}.
     */
    private static volatile int sPrepareCount = 0;

    public static int getPrepareCount() {
        return sPrepareCount;
    }
    
    public static void incrementPrepareCount() {
        sPrepareCount++;
    }
    
    /**
     * Adds the given callback to the list of callbacks that are called
     * during realtime stats collection. 
     */
    public static void addStatsCallback(RealtimeStatsCallback callback) {
        sRealtimeStats.addCallback(callback);
    }
    
    private static final long CSV_DUMP_FREQUENCY = Constants.MILLIS_PER_MINUTE;
    private static boolean sIsInitialized = false;

    public synchronized static void initialize() {
        if (sIsInitialized) {
            sLog.warn("Detected a second call to ZimbraPerf.initialize()", new Exception());
            return;
        }
        
        addStatsCallback(new ServerStatsCallback());
        
        StatsDumper.schedule(new MailboxdStats(), CSV_DUMP_FREQUENCY);

        StatsDumper.schedule(SOAP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(IMAP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(POP_TRACKER, CSV_DUMP_FREQUENCY);

        ThreadStats threadStats = new ThreadStats("threads.csv");
        StatsDumper.schedule(threadStats, CSV_DUMP_FREQUENCY);
        
        // Initialize JMX
        MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        sJmxServerStats = new JmxServerStats();
        try {
            jmxServer.registerMBean(sJmxServerStats, new ObjectName("ZimbraCollaborationSuite:type=ServerStats"));
        } catch (Exception e) {
            ZimbraLog.perf.warn("Unable to register JMX interface.", e);
        }

        sIsInitialized = true;
    }
    
    /**
     * Returns the mailbox cache size.  The real value is reread once a minute so that cache
     * performance is not affected.
     */
    static int getMailboxCacheSize() {
        long now = System.currentTimeMillis();
        if (now - sMailboxCacheSizeTimestamp > Constants.MILLIS_PER_MINUTE) {
            try {
                sMailboxCacheSize = MailboxManager.getInstance().getCacheSize();
            } catch (ServiceException e) {
                ZimbraLog.perf.warn("Unable to determine mailbox cache size.", e);
            }
            sMailboxCacheSizeTimestamp = now;
        }
        return sMailboxCacheSize;
    }

    /**
     * Scheduled task that writes a row to zimbrastats.csv with the latest
     * <tt>Accumulator</tt> data.
     */
    private static final class MailboxdStats
    implements StatsDumperDataSource
    {
        public String getFilename() {
            return "mailboxd.csv"; 
        }
        
        public String getHeader() {
            List<String> columns = new ArrayList<String>();
            for (Accumulator a : sAccumulators) {
                for (String column : a.getNames()) {
                    columns.add(column);
                }
            }
            return StringUtil.join(",", columns);
        }

        public Collection<String> getDataLines() {
            List<Object> data = new ArrayList<Object>();
            for (Accumulator a : sAccumulators) {
                synchronized (a) {
                    data.addAll(a.getData());
                    a.reset();
                }
            }

            // Clean up nulls 
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i) == null) {
                    data.set(i, "");
                }
            }
            
            // Return data
            String line = StringUtil.join(",", data);
            List<String> retVal = new ArrayList<String>(1);
            retVal.add(line);
            
            // Piggyback off timer to reset realtime stats.
            sJmxServerStats.reset();
            
            return retVal;
        }

        public boolean hasTimestampColumn() {
            return true;
        }
    }
}
