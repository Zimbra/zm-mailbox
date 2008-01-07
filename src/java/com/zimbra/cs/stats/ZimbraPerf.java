/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.zimbra.common.stats.Accumulator;
import com.zimbra.common.stats.Counter;
import com.zimbra.common.stats.RealtimeStats;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.common.stats.StatsDumper;
import com.zimbra.common.stats.StatsDumperDataSource;
import com.zimbra.common.stats.StopWatch;
import com.zimbra.common.stats.SystemStats;
import com.zimbra.common.stats.ThreadStats;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.db.DbPool;

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

    // Accumulators.  To add a new accumulator, create a static instance here,
    // add it to the CORE_ACCUMULATORS array and if necessary, set options
    // in the static init code below.
    public static Counter COUNTER_LMTP_RCVD_MSGS = new Counter("lmtp_rcvd_msgs");
    public static Counter COUNTER_LMTP_RCVD_BYTES = new Counter("lmtp_rcvd_bytes");
    public static Counter COUNTER_LMTP_RCVD_RCPT = new Counter("lmtp_rcvd_rcpt");
    public static Counter COUNTER_LMTP_DLVD_MSGS = new Counter("lmtp_dlvd_msgs");
    public static Counter COUNTER_LMTP_DLVD_BYTES = new Counter("lmtp_dlvd_bytes");
    public static StopWatch STOPWATCH_DB_CONN = new StopWatch("db_conn");
    public static StopWatch STOPWATCH_LDAP_DC = new StopWatch("ldap_dc");
    public static StopWatch STOPWATCH_MBOX_ADD_MSG = new StopWatch("mbox_add_msg");
    public static StopWatch STOPWATCH_MBOX_GET = new StopWatch("mbox_get");         // Mailbox accessor response time
    public static Counter COUNTER_MBOX_CACHE = new Counter("mbox_cache");           // Mailbox cache hit rate
    public static Counter COUNTER_MBOX_MSG_CACHE = new Counter("mbox_msg_cache"); 
    public static Counter COUNTER_MBOX_ITEM_CACHE = new Counter("mbox_item_cache");
    public static StopWatch STOPWATCH_SOAP = new StopWatch("soap");
    public static StopWatch STOPWATCH_IMAP = new StopWatch("imap");
    public static StopWatch STOPWATCH_POP = new StopWatch("pop");
    public static Counter COUNTER_IDX_WRT = new Counter("idx_wrt");
    public static Counter COUNTER_IDX_WRT_OPENED = new Counter("idx_wrt_opened");
    public static Counter COUNTER_IDX_WRT_OPENED_CACHE_HIT = new Counter("idx_wrt_opened_cache_hit");
    
    public static ActivityTracker SOAP_TRACKER = new ActivityTracker("soap.csv");
    public static ActivityTracker IMAP_TRACKER = new ActivityTracker("imap.csv");
    public static ActivityTracker POP_TRACKER = new ActivityTracker("pop3.csv");
    
    private static RealtimeStats sRealtimeStats = 
        new RealtimeStats(new String[] {
            RTS_DB_POOL_SIZE, RTS_INNODB_BP_HIT_RATE,
            RTS_POP_CONN, RTS_POP_SSL_CONN, RTS_IMAP_CONN, RTS_IMAP_SSL_CONN, RTS_SOAP_SESSIONS, 
            }
        );

    private static CopyOnWriteArrayList<Accumulator> sAccumulators = 
        new CopyOnWriteArrayList<Accumulator>(
                    new Accumulator[] {
                        COUNTER_LMTP_RCVD_MSGS, COUNTER_LMTP_RCVD_BYTES, COUNTER_LMTP_RCVD_RCPT,
                        COUNTER_LMTP_DLVD_MSGS, COUNTER_LMTP_DLVD_BYTES,
                        STOPWATCH_DB_CONN,
                        STOPWATCH_LDAP_DC,
                        STOPWATCH_MBOX_ADD_MSG, STOPWATCH_MBOX_GET, COUNTER_MBOX_CACHE,
                        COUNTER_MBOX_MSG_CACHE, COUNTER_MBOX_ITEM_CACHE,
                        STOPWATCH_SOAP,
                        STOPWATCH_IMAP,
                        STOPWATCH_POP,
                        COUNTER_IDX_WRT,
                        COUNTER_IDX_WRT_OPENED,
                        COUNTER_IDX_WRT_OPENED_CACHE_HIT,        
                        sRealtimeStats
                    }
        );
    
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
    
    private static final long CSV_DUMP_FREQUENCY = Constants.MILLIS_PER_MINUTE / 6;
    private static boolean sIsInitialized = false;

    private static final String[] THREAD_NAME_PREFIXES = new String[] { 
        "btpool",
        "pool",
        "LmtpHandler",
        "ImapHandler",
        "Pop3Handler",
        "ScheduledTask",
        "Timer",
        "AnonymousIoService",
        "FLAP processor",
        "GC",
        "SocketAcceptor",
        "Thread"
    };
    
    public synchronized static void initialize() {
        if (sIsInitialized) {
            sLog.warn("Detected a second call to ZimbraPerf.initialize()", new Exception());
            return;
        }
        
        addStatsCallback(new SystemStats());
        
        // Only the average is interesting for these counters
        COUNTER_MBOX_CACHE.setShowAverage(true);
        COUNTER_MBOX_CACHE.setAverageName("mbox_cache");
        COUNTER_MBOX_CACHE.setShowCount(false);
        COUNTER_MBOX_CACHE.setShowTotal(false);
        
        COUNTER_MBOX_MSG_CACHE.setShowAverage(true);
        COUNTER_MBOX_MSG_CACHE.setAverageName("mbox_msg_cache");
        COUNTER_MBOX_MSG_CACHE.setShowCount(false);
        COUNTER_MBOX_MSG_CACHE.setShowTotal(false);
        
        COUNTER_MBOX_ITEM_CACHE.setShowAverage(true);
        COUNTER_MBOX_ITEM_CACHE.setAverageName("mbox_item_cache");
        COUNTER_MBOX_ITEM_CACHE.setShowCount(false);
        COUNTER_MBOX_ITEM_CACHE.setShowTotal(false);
        
        COUNTER_IDX_WRT.setShowAverage(true);
        COUNTER_IDX_WRT.setShowCount(false);
        COUNTER_IDX_WRT.setShowTotal(false);

        StatsDumper.schedule(new MailboxdStats(), CSV_DUMP_FREQUENCY);
        ThreadStats threadStats = new ThreadStats(THREAD_NAME_PREFIXES, "threads.csv");
        StatsDumper.schedule(threadStats, CSV_DUMP_FREQUENCY);

        StatsDumper.schedule(SOAP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(IMAP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(POP_TRACKER, CSV_DUMP_FREQUENCY);

        sIsInitialized = true;
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
            return retVal;
        }

        public boolean hasTimestampColumn() {
            return true;
        }
    }
}
