/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.common.collect.Maps;
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
import com.zimbra.cs.util.MemoryStats;

/**
 * A collection of methods for keeping track of server performance statistics.
 */
public class ZimbraPerf {

    private static Log log = LogFactory.getLog(ZimbraPerf.class);

    @Description("Number of database connections in use")
    public static final String RTS_DB_POOL_SIZE = "db_pool_size";

    @Description("InnoDB buffer pool hit rate")
    public static final String RTS_INNODB_BP_HIT_RATE = "innodb_bp_hit_rate";

    @Description("Number of LMTP connections")
    public static final String RTS_LMTP_CONN = "lmtp_conn";

    @Description("Number of LMTP handler threads")
    public static final String RTS_LMTP_THREADS = "lmtp_threads";

    @Description("Number of cleartext POP3 connections")
    public static final String RTS_POP_CONN = "pop_conn";

    @Description("Number of POP3 handler threads")
    public static final String RTS_POP_THREADS = "pop_threads";

    @Description("Number of SSL POP3 connections")
    public static final String RTS_POP_SSL_CONN = "pop_ssl_conn";

    @Description("Number of POP3 SSL handler threads")
    public static final String RTS_POP_SSL_THREADS = "pop_ssl_threads";

    @Description("Number of cleartext IMAP connections")
    public static final String RTS_IMAP_CONN = "imap_conn";

    @Description("Number of IMAP handler threads")
    public static final String RTS_IMAP_THREADS = "imap_threads";

    @Description("Number of SSL IMAP connections")
    public static final String RTS_IMAP_SSL_CONN = "imap_ssl_conn";

    @Description("Number of IMAP SSL handler threads")
    public static final String RTS_IMAP_SSL_THREADS = "imap_ssl_threads";

    @Description("Number of HTTP handler threads")
    public static final String RTS_HTTP_THREADS = "http_threads";

    @Description("Number of idle HTTP handler threads")
    public static final String RTS_HTTP_IDLE_THREADS = "http_idle_threads";

    @Description("Number of SOAP sessions")
    public static final String RTS_SOAP_SESSIONS = "soap_sessions";

    @Description("Number of mailboxes cached in memory")
    public static final String RTS_MBOX_CACHE_SIZE = "mbox_cache_size";

    @Description("Number of message structures cached in memory")
    public static final String RTS_MSG_CACHE_SIZE = "msg_cache_size";

    @Description("Number of open file descriptors that reference message content")
    public static final String RTS_FD_CACHE_SIZE = "fd_cache_size";

    @Description("File descriptor cache hit rate")
    public static final String RTS_FD_CACHE_HIT_RATE = "fd_cache_hit_rate";

    // LDAP provisioning caches.
    @Description("LDAP ACL cache hit rate")
    public static final String RTS_ACL_CACHE_HIT_RATE = "acl_cache_hit_rate";

    @Description("LDAP account cache size")
    public static final String RTS_ACCOUNT_CACHE_SIZE = "account_cache_size";

    @Description("LDAP account cache hit rate")
    public static final String RTS_ACCOUNT_CACHE_HIT_RATE = "account_cache_hit_rate";

    @Description("LDAP COS cache size")
    public static final String RTS_COS_CACHE_SIZE = "cos_cache_size";

    @Description("LDAP COS cache hit rate")
    public static final String RTS_COS_CACHE_HIT_RATE = "cos_cache_hit_rate";

    @Description("LDAP domain cache size")
    public static final String RTS_DOMAIN_CACHE_SIZE = "domain_cache_size";

    @Description("LDAP domain cache hit rate")
    public static final String RTS_DOMAIN_CACHE_HIT_RATE = "domain_cache_hit_rate";

    @Description("LDAP server cache size")
    public static final String RTS_SERVER_CACHE_SIZE = "server_cache_size";

    @Description("LDAP server cache hit rate")
    public static final String RTS_SERVER_CACHE_HIT_RATE = "server_cache_hit_rate";

    @Description("LDAP UC service cache size")
    public static final String RTS_UCSERVICE_CACHE_SIZE = "ucservice_cache_size";

    @Description("LDAP UC service cache hit rate")
    public static final String RTS_UCSERVICE_CACHE_HIT_RATE = "ucservice_cache_hit_rate";

    @Description("LDAP zimlet cache size")
    public static final String RTS_ZIMLET_CACHE_SIZE = "zimlet_cache_size";

    @Description("LDAP zimlet cache hit rate")
    public static final String RTS_ZIMLET_CACHE_HIT_RATE = "zimlet_cache_hit_rate";

    @Description("LDAP group cache size")
    public static final String RTS_GROUP_CACHE_SIZE = "group_cache_size";

    @Description("LDAP group cache hit rate")
    public static final String RTS_GROUP_CACHE_HIT_RATE = "group_cache_hit_rate";

    @Description("LDAP XMPP cache size")
    public static final String RTS_XMPP_CACHE_SIZE = "xmpp_cache_size";

    @Description("LDAP XMPP cache hit rate")
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
    public static final StopWatch STOPWATCH_EWS = new StopWatch();

    public static final ActivityTracker SOAP_TRACKER = new ActivityTracker("soap.csv");
    public static final ActivityTracker IMAP_TRACKER = new ActivityTracker("imap.csv");
    public static final ActivityTracker IMAPD_TRACKER = new ActivityTracker("imapd.csv");
    public static final ActivityTracker POP_TRACKER = new ActivityTracker("pop3.csv");
    public static final ActivityTracker LDAP_TRACKER = new ActivityTracker("ldap.csv");
    public static final ActivityTracker SYNC_TRACKER = new ActivityTracker("sync.csv");
    public static final ActivityTracker SQL_TRACKER  = new ActivityTracker("sql.csv");
    public static final ActivityTracker REDIS_TRACKER  = new ActivityTracker("redis.csv");

    private static int mailboxCacheSize;
    private static long mailboxCacheSizeTimestamp = 0;
    private static JmxServerStats jmxServerStats;
    private static JmxImapDaemonStats jmxImapDaemonStats;
    private static Map<String, String> descriptions = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);

    private static String[] mboxRealtimeStatsNames = new String[] {
            RTS_DB_POOL_SIZE, RTS_INNODB_BP_HIT_RATE,
            RTS_LMTP_CONN, RTS_LMTP_THREADS,
            RTS_POP_CONN, RTS_POP_THREADS, RTS_POP_SSL_CONN, RTS_POP_SSL_THREADS,
            RTS_IMAP_CONN, RTS_IMAP_THREADS, RTS_IMAP_SSL_CONN, RTS_IMAP_SSL_THREADS,
            RTS_HTTP_IDLE_THREADS, RTS_HTTP_THREADS, RTS_SOAP_SESSIONS,
            RTS_MBOX_CACHE_SIZE, RTS_MSG_CACHE_SIZE,
            RTS_FD_CACHE_SIZE, RTS_FD_CACHE_HIT_RATE,
            RTS_ACL_CACHE_HIT_RATE,
            RTS_ACCOUNT_CACHE_SIZE, RTS_ACCOUNT_CACHE_HIT_RATE,
            RTS_COS_CACHE_SIZE, RTS_COS_CACHE_HIT_RATE,
            RTS_DOMAIN_CACHE_SIZE, RTS_DOMAIN_CACHE_HIT_RATE,
            RTS_SERVER_CACHE_SIZE, RTS_SERVER_CACHE_HIT_RATE,
            RTS_UCSERVICE_CACHE_SIZE, RTS_UCSERVICE_CACHE_HIT_RATE,
            RTS_ZIMLET_CACHE_SIZE, RTS_ZIMLET_CACHE_HIT_RATE,
            RTS_GROUP_CACHE_SIZE, RTS_GROUP_CACHE_HIT_RATE,
            RTS_XMPP_CACHE_SIZE, RTS_XMPP_CACHE_HIT_RATE
            };
    private static String[] imapdRealtimeStatsNames = new String[] {
                RTS_IMAP_CONN, RTS_IMAP_THREADS, RTS_IMAP_SSL_CONN, RTS_IMAP_SSL_THREADS
            };

    private static RealtimeStats realtimeStats = null;

    @Description("Number of messages received over LMTP")
    private static final String DC_LMTP_RCVD_MSGS = "lmtp_rcvd_msgs";

    @Description("Number of bytes received over LMTP")
    private static final String DC_LMTP_RCVD_BYTES = "lmtp_rcvd_bytes";

    @Description("Number of LMTP recipients")
    private static final String DC_LMTP_RCVD_RCPT = "lmtp_rcvd_rcpt";

    @Description("Number of messages delivered to mailboxes as a result of LMTP delivery")
    private static final String DC_LMTP_DLVD_MSGS = "lmtp_dlvd_msgs";

    @Description("Number of bytes of data delivered to mailboxes as a result of LMTP delivery")
    private static final String DC_LMTP_DLVD_BYTES = "lmtp_dlvd_bytes";

    @Description("Number of times that the server got a database connection from the pool")
    private static final String DC_DB_CONN_COUNT = "db_conn_count";

    @Description("Average latency (ms) of getting a database connection from the pool")
    private static final String DC_DB_CONN_MS_AVG = "db_conn_ms_avg";

    @Description("Number of times that the server got an LDAP directory context")
    private static final String DC_LDAP_DC_COUNT = "ldap_dc_count";

    @Description("Average latency (ms) of getting an LDAP directory context")
    private static final String DC_LDAP_DC_MS_AVG = "ldap_dc_ms_avg";

    @Description("Number of messages that were added to a mailbox")
    private static final String DC_MBOX_ADD_MSG_COUNT = "mbox_add_msg_count";

    @Description("Average latency (ms) of adding a message to a mailbox")
    private static final String DC_MBOX_ADD_MSG_MS_AVG = "mbox_add_msg_ms_avg";

    @Description("Number of times that the server got a mailbox from the cache")
    private static final String DC_MBOX_GET_COUNT = "mbox_get_count";

    @Description("Average latency (ms) of getting a mailbox from the cache")
    private static final String DC_MBOX_GET_MS_AVG = "mbox_get_ms_avg";

    @Description("Mailbox cache hit rate")
    private static final String DC_MBOX_CACHE = "mbox_cache";

    @Description("Message cache hit rate")
    private static final String DC_MBOX_MSG_CACHE = "mbox_msg_cache";

    @Description("Item cache hit rate")
    private static final String DC_MBOX_ITEM_CACHE = "mbox_item_cache";

    @Description("Number of SOAP requests received")
    private static final String DC_SOAP_COUNT = "soap_count";

    @Description("Average processing time (ms) of SOAP requests")
    private static final String DC_SOAP_MS_AVG = "soap_ms_avg";

    @Description("Number of IMAP requests received")
    private static final String DC_IMAP_COUNT = "imap_count";

    @Description("Average processing time (ms) of IMAP requests")
    private static final String DC_IMAP_MS_AVG = "imap_ms_avg";

    @Description("Number of POP3 requests received")
    private static final String DC_POP_COUNT = "pop_count";

    @Description("Average processing time (ms) of POP3 requests")
    private static final String DC_POP_MS_AVG = "pop_ms_avg";

    @Description("Number of times that the file descriptor cache read message data from disk")
    private static final String DC_BIS_READ = "bis_read";

    @Description("Percentage of file descriptor cache disk reads that required a seek")
    private static final String DC_BIS_SEEK_RATE = "bis_seek_rate";

    @Description("Average number of concurrent index writers")
    private static final String DC_IDX_WRT_AVG = "idx_wrt_avg";

    @Description("Accumulated number of index writers opened")
    private static final String DC_IDX_WRT_OPENED = "idx_wrt_opened";

    @Description("Accumulated number of cache hits when opening an index writer")
    private static final String DC_IDX_WRT_OPENED_CACHE_HIT = "idx_wrt_opened_cache_hit";

    @Description("Accumulated bytes written by Lucene")
    private static final String DC_IDX_BYTES_WRITTEN = "idx_bytes_written";

    @Description("Average of idx_bytes_written")
    private static final String DC_IDX_BYTES_WRITTTEN_AVG = "idx_bytes_written_avg";

    @Description("Accumulated bytes read by Lucene")
    private static final String DC_IDX_BYTES_READ = "idx_bytes_read";

    @Description("Average of idx_bytes_read")
    private static final String DC_IDX_BYTES_READ_AVG = "idx_bytes_read_avg";

    @Description("Hit rate of calendar summary cache, counting cache hit from both memory and file")
    private static final String DC_CALCACHE_HIT = "calcache_hit";

    @Description("Hit rate of calendar summary cache, counting cache hit from memory only")
    private static final String DC_CALCACHE_MEM_HIT = "calcache_mem_hit";

    @Description("Number of calendars (folders) in the calendar summary cache LRU in Java heap")
    private static final String DC_CALCACHE_LRU_SIZE = "calcache_lru_size";

    private static CopyOnWriteArrayList<Accumulator> sAccumulators = null;

    private static final long CSV_DUMP_FREQUENCY = Constants.MILLIS_PER_MINUTE;
    private static boolean sIsInitialized = false;
    private static boolean isPrepared = false;
    /**
     * The number of statements that were prepared, as reported by
     * {@link DbPool.DbConnection#prepareStatement}.
     */
    private static AtomicInteger sPrepareCount = new AtomicInteger(0);

    public enum ServerID {ZIMBRA, IMAP_DAEMON};

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Description {
        String value();
    }

    public static String getDescription(String statName) {
        return descriptions.get(statName);
    }

    private static void initDescriptions() {
        descriptions = Collections.synchronizedMap(descriptions);

        for (Field f : ZimbraPerf.class.getDeclaredFields()) {
            if (f.isAnnotationPresent(Description.class)) {
                try {
                    Object o = f.get(null);
                    if (o instanceof String) {
                        String description = f.getAnnotation(Description.class).value();
                        descriptions.put((String) o, description);
                    }
                } catch (IllegalAccessException e) {
                    ZimbraLog.perf.warn("Unexpected @Description annotation on field %s.", f.getName(), e);
                }
            }
        }
    }

    /**
     * Returns all the latest stats as a key-value <tt>Map</tt>.
     */
    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<String, Object>();

        List<Accumulator> accumulators = new ArrayList<Accumulator>();
        accumulators.addAll(sAccumulators);
        accumulators.add(realtimeStats);

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
    public static void addRealtimeStatName(String name, String description) {
        if (sIsInitialized) {
            throw new IllegalStateException("Cannot add stat name after ZimbraPerf has been initialized");
        }
        ZimbraLog.perf.debug("Adding realtime stat '%s': %s", name, description);
        realtimeStats.addName(name);
        descriptions.put(name, description);
    }

    public static JmxServerStatsMBean getMonitoringStats() {
        return jmxServerStats;
    }

    public static int getPrepareCount() {
        return sPrepareCount.get();
    }

    public static void incrementPrepareCount() {
        sPrepareCount.getAndIncrement();
    }

    /**
     * Adds the given callback to the list of callbacks that are called
     * during realtime stats collection.
     */
    public static void addStatsCallback(RealtimeStatsCallback callback) {
        if (realtimeStats == null) {
            ZimbraLog.perf.debug("Call to addStatsCallback when realtimeStats has not been initialized\n%s",
                    ZimbraLog.getStackTrace(15));
            /* This probably happens inside a commandline tool like zmmetadump where it doesn't
             * make sense to mix in stats with those of the main Zimbra process.
             */
            return;
        }
        realtimeStats.addCallback(callback);
    }

    /**
     *       MUST be called before anything else
     *
     * Some things need to be done before initialize is called but require some static variables to
     * have already been setup.
     * @param serverID
     */
    public synchronized static void prepare(ServerID serverID) {
        if (isPrepared) {
            log.warn("Detected call to ZimbraPerf.prepare() after already prepared", new Exception());
            return;
        }
        if (sIsInitialized) {
            throw new IllegalStateException(
                    "Must not call ZimbraPerf.prepare() after ZimbraPerf.initialize()");
        }
        switch (serverID) {
        case ZIMBRA:
            realtimeStats = new RealtimeStats(mboxRealtimeStatsNames);
            sAccumulators = new CopyOnWriteArrayList<Accumulator>(
                    new Accumulator[] {
                            new DeltaCalculator(COUNTER_LMTP_RCVD_MSGS).setTotalName(DC_LMTP_RCVD_MSGS),
                            new DeltaCalculator(COUNTER_LMTP_RCVD_BYTES).setTotalName(DC_LMTP_RCVD_BYTES),
                            new DeltaCalculator(COUNTER_LMTP_RCVD_RCPT).setTotalName(DC_LMTP_RCVD_RCPT),
                            new DeltaCalculator(COUNTER_LMTP_DLVD_MSGS).setTotalName(DC_LMTP_DLVD_MSGS),
                            new DeltaCalculator(COUNTER_LMTP_DLVD_BYTES).setTotalName(DC_LMTP_DLVD_BYTES),
                            new DeltaCalculator(STOPWATCH_DB_CONN).setCountName(DC_DB_CONN_COUNT)
                                    .setAverageName(DC_DB_CONN_MS_AVG),
                            new DeltaCalculator(STOPWATCH_LDAP_DC).setCountName(DC_LDAP_DC_COUNT)
                                    .setAverageName(DC_LDAP_DC_MS_AVG),
                            new DeltaCalculator(STOPWATCH_MBOX_ADD_MSG).setCountName(DC_MBOX_ADD_MSG_COUNT)
                                    .setAverageName(DC_MBOX_ADD_MSG_MS_AVG),
                            new DeltaCalculator(STOPWATCH_MBOX_GET).setCountName(DC_MBOX_GET_COUNT)
                                    .setAverageName(DC_MBOX_GET_MS_AVG),
                            new DeltaCalculator(COUNTER_MBOX_CACHE).setAverageName(DC_MBOX_CACHE),
                            new DeltaCalculator(COUNTER_MBOX_MSG_CACHE).setAverageName(DC_MBOX_MSG_CACHE),
                            new DeltaCalculator(COUNTER_MBOX_ITEM_CACHE).setAverageName(DC_MBOX_ITEM_CACHE),
                            new DeltaCalculator(STOPWATCH_SOAP).setCountName(DC_SOAP_COUNT)
                                    .setAverageName(DC_SOAP_MS_AVG),
                            new DeltaCalculator(STOPWATCH_IMAP).setCountName(DC_IMAP_COUNT)
                                    .setAverageName(DC_IMAP_MS_AVG),
                            new DeltaCalculator(STOPWATCH_POP).setCountName(DC_POP_COUNT)
                                    .setAverageName(DC_POP_MS_AVG),
                            new DeltaCalculator(COUNTER_IDX_WRT).setAverageName(DC_IDX_WRT_AVG),
                            new DeltaCalculator(COUNTER_IDX_WRT_OPENED).setTotalName(DC_IDX_WRT_OPENED),
                            new DeltaCalculator(COUNTER_IDX_WRT_OPENED_CACHE_HIT)
                                    .setTotalName(DC_IDX_WRT_OPENED_CACHE_HIT),
                            new DeltaCalculator(COUNTER_CALENDAR_CACHE_HIT).setAverageName(DC_CALCACHE_HIT),
                            new DeltaCalculator(COUNTER_CALENDAR_CACHE_MEM_HIT)
                                    .setAverageName(DC_CALCACHE_MEM_HIT),
                            new DeltaCalculator(COUNTER_CALENDAR_CACHE_LRU_SIZE)
                                    .setAverageName(DC_CALCACHE_LRU_SIZE),
                            new DeltaCalculator(COUNTER_IDX_BYTES_WRITTEN)
                                    .setTotalName(DC_IDX_BYTES_WRITTEN)
                                    .setAverageName(DC_IDX_BYTES_WRITTTEN_AVG),
                            new DeltaCalculator(COUNTER_IDX_BYTES_READ)
                                    .setTotalName(DC_IDX_BYTES_READ).setAverageName(DC_IDX_BYTES_READ_AVG),
                            new DeltaCalculator(COUNTER_BLOB_INPUT_STREAM_READ).setTotalName(DC_BIS_READ),
                            new DeltaCalculator(COUNTER_BLOB_INPUT_STREAM_SEEK_RATE)
                                    .setAverageName(DC_BIS_SEEK_RATE),
                            realtimeStats
                    }
                );
            break;
        case IMAP_DAEMON:
            realtimeStats = new RealtimeStats(imapdRealtimeStatsNames);
            sAccumulators = new CopyOnWriteArrayList<Accumulator>(
                    new Accumulator[] {
                            new DeltaCalculator(STOPWATCH_IMAP)
                                    .setCountName(DC_IMAP_COUNT).setAverageName(DC_IMAP_MS_AVG),
                            realtimeStats
                    }
                );
            break;
        default:
        }
        isPrepared = true;
    }

    public synchronized static void initialize(ServerID serverID) {
        if (!isPrepared) {
            throw new IllegalStateException("Must call prepare() before ZimbraPerf.initialize()");
        }
        if (sIsInitialized) {
            log.warn("Detected a second call to ZimbraPerf.initialize()", new Exception());
            return;
        }
        initDescriptions();
        switch (serverID) {
        case ZIMBRA:
            initializeForMainZimbraJVM();
            break;
        case IMAP_DAEMON:
            initializeForImapDaemon();
            break;
        default:
        }
        sIsInitialized = true;
    }

    private synchronized static void initializeForMainZimbraJVM() {
        addStatsCallback(new ServerStatsCallback());
        addStatsCallback(new JettyStats());
        // Initialize JMX
        MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        jmxServerStats = new JmxServerStats();
        try {
            jmxServer.registerMBean(jmxServerStats,
                    new ObjectName("ZimbraCollaborationSuite:type=ServerStats"));
        } catch (Exception e) {
            ZimbraLog.perf.warn("Unable to register JMX interface.", e);
        }
        StatsDumper.schedule(new Stats("mailboxd.csv", sAccumulators, jmxServerStats), CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(SOAP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(IMAP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(POP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(LDAP_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(SYNC_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(SQL_TRACKER, CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(REDIS_TRACKER, CSV_DUMP_FREQUENCY);
        ThreadStats threadStats = new ThreadStats("threads.csv");
        StatsDumper.schedule(threadStats, CSV_DUMP_FREQUENCY);
    }

    private synchronized static void initializeForImapDaemon() {
        // Initialize JMX
        MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        jmxImapDaemonStats = new JmxImapDaemonStats();
        try {
            jmxServer.registerMBean(jmxImapDaemonStats, new ObjectName("ZimbraImapDaemon:type=ServerStats"));
        } catch (Exception e) {
            ZimbraLog.perf.warn("Unable to register JMX interface.", e);
        }
        StatsDumper.schedule(new Stats("imapd_stats.csv", sAccumulators, jmxImapDaemonStats),
                CSV_DUMP_FREQUENCY);
        StatsDumper.schedule(IMAPD_TRACKER, CSV_DUMP_FREQUENCY);
    }

    /**
     * Returns the mailbox cache size.  The real value is reread once a minute so that cache
     * performance is not affected.
     */
    protected static int getMailboxCacheSize() {
        long now = System.currentTimeMillis();
        if (now - mailboxCacheSizeTimestamp > Constants.MILLIS_PER_MINUTE) {
            try {
                mailboxCacheSize = MailboxManager.getInstance().getCacheSize();
            } catch (ServiceException e) {
                ZimbraLog.perf.warn("Unable to determine mailbox cache size.", e);
            }
            mailboxCacheSizeTimestamp = now;
        }
        return mailboxCacheSize;
    }

    /**
     * Scheduled task that writes a row to a CSV file with the latest <tt>Accumulator</tt> data.
     */
    private static final class Stats implements StatsDumperDataSource {
        private final String filename;
        private final List<Accumulator> accumulators;
        private final JmxStatsMBeanBase statsBean;

        public Stats(String filename, List<Accumulator> accumulators, JmxStatsMBeanBase statsBean) {
            this.filename = filename;
            this.accumulators = accumulators;
            this.statsBean = statsBean;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public String getHeader() {
            List<String> columns = new ArrayList<String>();
            for (Accumulator a : accumulators) {
                for (String column : a.getNames()) {
                    columns.add(column);
                }
            }
            return StringUtil.join(",", columns);
        }

        @Override
        public Collection<String> getDataLines() {
            List<Object> data = new ArrayList<Object>();
            for (Accumulator a : accumulators) {
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
            statsBean.reset();

            return retVal;
        }

        @Override
        public boolean hasTimestampColumn() {
            return true;
        }
    }

    public static void main(String[] args) {
        initDescriptions();
        MemoryStats.startup();
        for (String field : descriptions.keySet()) {
            System.out.println(field + ": " + descriptions.get(field));
        }
    }
}
