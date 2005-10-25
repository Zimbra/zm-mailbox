/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.ZimbraLog;

/**
 * A collection of methods for keeping track of server performance statistics.
 */
public class ZimbraPerf {

    private static Map /* <String, StatementStats */ sSqlToStats = new HashMap();
    
    /**
     * The number of statements that were executed, as reported by
     * <code>DebugPreparedStatement</code>. This number only gets updated
     * when debug logging is turned on for the <code>zimbra.sqltrace</code> or
     * <code>zimbra.perf</code> categories.
     */
    private static int sStatementCount = 0;
    
    /**
     * The number of statements that were prepared, as reported by
     * {@link DbPool.Connection#prepareStatement}.
     */
    private static int sPrepareCount = 0;

    private static final long STATS_WRITE_INTERVAL = Constants.MILLIS_PER_MINUTE;
    private static final long SLOW_QUERY_THRESHOLD = 300;  // ms
    private static long sLastStatsWrite = System.currentTimeMillis();
    
    private static class StatementStats {
        String mSql;
        int mCount = 0;
        int mMinTime = Integer.MAX_VALUE;
        int mMaxTime = 0;
        int mTotalTime = 0;

        StatementStats(String sql) {
            mSql = sql;
        }
        
        int getAvgTime() {
            if (mCount == 0) {
                return 0;
            }
            return mTotalTime / mCount;
        }
    }
    
    public static int getStatementCount() {
        return sStatementCount;
    }
    
    public static int getPrepareCount() {
        return sPrepareCount;
    }
    
    public static void incrementPrepareCount() {
        sPrepareCount++;
    }
    
    /**
     * Increments the total SQL statement count.  If <code>sqlperf</code> debug
     * logging is on,
     * 
     * <ul>
     *   <li>Updates aggregate timing data for the given statement</li>
     *   <li>Writes timing data to <code>sqlperf.csv</code> once a minute</li>
     *   <li>Writes slow queries to <code>slowQueries.csv</code></li>
     * </ul>
     *  
     * @param sql the SQL statement
     * @param durationMillis execution time
     * @see #getStatementCount()
     */
    public static void updateDbStats(String sql, int durationMillis) {
        sStatementCount++;
        if (!ZimbraLog.perf.isDebugEnabled()) {
            return;
        }
        
        sql = DbUtil.normalizeSql(sql);
        StatementStats stats = null;
        
        synchronized (sSqlToStats) {
            stats = (StatementStats) sSqlToStats.get(sql);
            if (stats == null) {
                stats = new StatementStats(sql);
                sSqlToStats.put(sql, stats);
            }

            stats.mCount++;
            stats.mTotalTime += durationMillis;
            if (durationMillis < stats.mMinTime) {
                stats.mMinTime = durationMillis;
            }
            if (durationMillis > stats.mMaxTime) {
                stats.mMaxTime = durationMillis;
            }
        }
        
        ThreadLocalData.incrementDbTime(durationMillis);
        
        if (durationMillis > SLOW_QUERY_THRESHOLD) {
            writeSlowQuery(sql, durationMillis);
        }
        long now = System.currentTimeMillis();
        if (now - sLastStatsWrite > STATS_WRITE_INTERVAL) {
            writeDbStats();
            sLastStatsWrite = now;
        }
    }

    /**
     * Sorts stats in reverse order by count. 
     */
    private static class StatsComparator
    implements Comparator {
        public int compare(Object o1, Object o2) {
            StatementStats stats1 = (StatementStats) o1;
            StatementStats stats2 = (StatementStats) o2;
            if (stats1.mCount < stats2.mCount) {
                return 1;
            } else if (stats1.mCount > stats2.mCount) {
                return -1;
            }
            return 0;
        }
    }
    private static final StatsComparator STATS_COMPARATOR = new StatsComparator(); 

    /**
     * Writes all the SQL statements and their latest timing data to
     * <code>sqlperf.csv</code>, in descending order by count.
     */
    private static void writeDbStats() {
        String filename = LC.zimbra_log_directory.value() + "/sqlperf.csv";
        FileWriter writer = null;
        try {
            synchronized (sSqlToStats) {
                // Calculate total of totals
                int totalTotal = 0;
                Iterator i = sSqlToStats.values().iterator();
                while (i.hasNext()) {
                    StatementStats stats = (StatementStats) i.next();
                    totalTotal += stats.mTotalTime;
                }
                
                writer = new FileWriter(filename);
                writer.write("sql,count,min_time,avg_time,max_time,total_time,percent_total_time\n");
                List statsList = new ArrayList(sSqlToStats.values());
                Collections.sort(statsList, STATS_COMPARATOR);
                i = statsList.iterator();
                while (i.hasNext()) {
                    StatementStats stats = (StatementStats) i.next();
                    int percent = 0;
                    if (totalTotal != 0) {
                        percent = stats.mTotalTime * 100 / totalTotal;
                    }
                    writer.write("\"" + stats.mSql + "\"," + stats.mCount + "," + stats.mMinTime + "," +
                        stats.getAvgTime() + "," + stats.mMaxTime + "," + stats.mTotalTime + "," +
                        percent + "%,\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            ZimbraLog.perf.warn("Unable to write to " + filename + ": " + e.toString());
        }
    }

    private static final SimpleDateFormat TIMESTAMP_FORMATTER =
        new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private static void writeSlowQuery(String sql, int durationMillis) {
        String filename = LC.zimbra_log_directory.value() + "/slowQueries.csv";
        FileWriter writer = null;
        try {
            File file = new File(filename);
            if (!file.exists()) {
                // There's a remote possibility that two processes will do this at the same time.
                // Worst case, we'll lose one line of data.
                writer = new FileWriter(file);
                writer.write("timestamp,exec_time,sql,normalized\n");
            } else {
                writer = new FileWriter(file, true);
            }
            writer.write(TIMESTAMP_FORMATTER.format(new Date()) + "," + durationMillis +
                ",\"" + sql + "\",\"" + DbUtil.normalizeSql(sql) + "\"\n");
            writer.close();
        } catch (IOException e) {
            ZimbraLog.perf.warn("Unable to write to " + filename + ": " + e.toString());
        }
    }
    
    public static void writeResponseStats(String responseName, long requestMillis,
        long dbMillis, int statementCount) {
        String filename = LC.zimbra_log_directory.value() + "/responses.csv";
        FileWriter writer = null;
        try {
            File file = new File(filename);
            if (!file.exists()) {
                // There's a remote possibility that two processes will do this at the same time.
                // Worst case, we'll lose one line of data.
                writer = new FileWriter(file);
                writer.write("timestamp,response_name,exec_time,db_time,statement_count\n");
            } else {
                writer = new FileWriter(file, true);
            }
            writer.write(TIMESTAMP_FORMATTER.format(new Date()) + "," + responseName + "," +
                requestMillis + "," + dbMillis + "," + statementCount + "\n");
            writer.close();
        } catch (IOException e) {
            ZimbraLog.perf.warn("Unable to write to " + filename + ": " + e.toString());
        }
    }
}
