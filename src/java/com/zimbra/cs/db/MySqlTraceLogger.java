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

package com.zimbra.cs.db;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.service.util.ZimbraPerf;
import com.zimbra.cs.util.ZimbraLog;
import com.mysql.jdbc.log.Log;

/**
 * @author bburtin
 */
public class MySqlTraceLogger implements Log {

    private static Map /* <String, String> */ sIdToSql = new HashMap();
    public MySqlTraceLogger(String instanceName) {
        // This constructor's presence is required by log factory.
    }

    private static final Pattern PAT_QUERY =
        Pattern.compile("QUERY.*duration: (\\d+).*message: (.*)", Pattern.DOTALL);
    private static final Pattern PAT_PREPARE =
        Pattern.compile("PREPARE.*connection: (\\d+).*statement: (\\d+).*message: (.*)", Pattern.DOTALL);
    private static final Pattern PAT_EXECUTE =
        Pattern.compile("EXECUTE.*duration: (\\d+).*connection: (\\d+).*statement: (\\d+)");
    private static final Pattern PAT_SLOW =
        Pattern.compile("Slow query.*duration: (\\d+) ms\\): (.*)", Pattern.DOTALL);
    
    private void log(Object arg, Throwable t) {
        String message = arg.toString();
        
        // Skip uninteresting operations
        if (message.startsWith("FETCH")) {
            return;
        }

        // Parse the SQL statement and timing info
        String sql = null;
        int duration = -1;

        Matcher matcher = PAT_PREPARE.matcher(message);
        if (matcher.matches()) {
            String connectionId = matcher.group(1);
            String statementId = matcher.group(2);
            sql = matcher.group(3);
            putSql(connectionId, statementId, sql);
            return;
        } else {
            matcher = PAT_EXECUTE.matcher(message);
            if (matcher.find()) {
                duration = Integer.parseInt(matcher.group(1));
                String connectionId = matcher.group(2);
                String statementId = matcher.group(3);
                sql = removeSql(connectionId, statementId);
            } else {
                matcher = PAT_QUERY.matcher(message);
                if (matcher.matches()) {
                    duration = Integer.parseInt(matcher.group(1));
                    sql = matcher.group(2);
                } else {
                    matcher = PAT_SLOW.matcher(message);
                    if (matcher.matches()) {
                        int slowDuration = Integer.parseInt(matcher.group(1));
                        ZimbraPerf.writeSlowQuery(matcher.group(2), slowDuration);
                        ZimbraLog.perf.debug(arg);
                        return;
                    }
                }
            }
        }

        // Log the SQL and write timing data if necessary.  This code
        // gets triggered if either sqltrace or sqlperf is on, so we
        // need to check if the logger is enabled before logging anything.
        if (sql != null) {
            if (ZimbraLog.perf.isDebugEnabled()) {
                ZimbraPerf.updateDbStats(sql, duration);
            }
            
            // Skip uninteresting messages
            if (sql.startsWith("SET autocommit") ||
                sql.startsWith("SET SESSION") ||
                sql.startsWith("SELECT 1")) {
                return;
            }

            if (ZimbraLog.sqltrace.isDebugEnabled()) {
                ZimbraLog.sqltrace.debug(sql + ", duration: " + duration);
            }
            
            return;
        }

        ZimbraLog.sqltrace.debug(arg, t);
    }

    /**
     * Keeps track of the given SQL string for later lookup by
     * the <code>connectionId</code> and <code>statementId</code>.
     */
    private void putSql(String connectionId, String statementId, String sql) {
        synchronized (sIdToSql) {
            sIdToSql.put(connectionId + "-" + statementId, sql);
        }
    }

    /**
     * Looks up and removes the SQL string that has been stored by {@link #putSql}.
     */
    private String removeSql(String connectionId, String statementId) {
        synchronized (sIdToSql) {
            return (String) sIdToSql.remove(connectionId + "-" + statementId);
        }
    }

    //////////// com.mysql.jdbc.log.Log implementation ////////////////

    public boolean isDebugEnabled() {
        return ZimbraLog.sqltrace.isDebugEnabled() || ZimbraLog.perf.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return ZimbraLog.sqltrace.isErrorEnabled() || ZimbraLog.perf.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return ZimbraLog.sqltrace.isFatalEnabled() || ZimbraLog.perf.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return ZimbraLog.sqltrace.isInfoEnabled() || ZimbraLog.perf.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return ZimbraLog.sqltrace.isTraceEnabled() || ZimbraLog.perf.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return ZimbraLog.sqltrace.isWarnEnabled() || ZimbraLog.perf.isWarnEnabled();
    }

    public void logDebug(Object arg0) {
        log(arg0, null);
    }

    public void logDebug(Object arg0, Throwable arg1) {
        log(arg0, arg1);
    }

    public void logError(Object arg0) {
        ZimbraLog.sqltrace.error(arg0);
    }

    public void logError(Object arg0, Throwable arg1) {
        ZimbraLog.sqltrace.error(arg0, arg1);
    }

    public void logFatal(Object arg0) {
        ZimbraLog.sqltrace.fatal(arg0);
    }

    public void logFatal(Object arg0, Throwable arg1) {
        ZimbraLog.sqltrace.fatal(arg0, arg1);
    }

    public void logInfo(Object arg0) {
        log(arg0, null);
    }

    public void logInfo(Object arg0, Throwable arg1) {
        log(arg0, arg1);
    }

    public void logTrace(Object arg0) {
        ZimbraLog.sqltrace.trace(arg0);
    }

    public void logTrace(Object arg0, Throwable arg1) {
        ZimbraLog.sqltrace.trace(arg0, arg1);
    }

    public void logWarn(Object arg0) {
        log(arg0, null);
    }

    public void logWarn(Object arg0, Throwable arg1) {
        log(arg0, arg1);
    }
}
