package com.liquidsys.coco.db;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liquidsys.coco.service.util.LiquidPerf;
import com.liquidsys.coco.util.LiquidLog;
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
                        LiquidPerf.writeSlowQuery(matcher.group(2), slowDuration);
                        LiquidLog.perf.debug(arg);
                        return;
                    }
                }
            }
        }

        // Log the SQL and write timing data if necessary.  This code
        // gets triggered if either sqltrace or sqlperf is on, so we
        // need to check if the logger is enabled before logging anything.
        if (sql != null) {
            if (LiquidLog.perf.isDebugEnabled()) {
                LiquidPerf.updateDbStats(sql, duration);
            }
            
            // Skip uninteresting messages
            if (sql.startsWith("SET autocommit") ||
                sql.startsWith("SET SESSION") ||
                sql.startsWith("SELECT 1")) {
                return;
            }

            if (LiquidLog.sqltrace.isDebugEnabled()) {
                LiquidLog.sqltrace.debug(sql + ", duration: " + duration);
            }
            
            return;
        }

        LiquidLog.sqltrace.debug(arg, t);
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
        return LiquidLog.sqltrace.isDebugEnabled() || LiquidLog.perf.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return LiquidLog.sqltrace.isErrorEnabled() || LiquidLog.perf.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return LiquidLog.sqltrace.isFatalEnabled() || LiquidLog.perf.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return LiquidLog.sqltrace.isInfoEnabled() || LiquidLog.perf.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return LiquidLog.sqltrace.isTraceEnabled() || LiquidLog.perf.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return LiquidLog.sqltrace.isWarnEnabled() || LiquidLog.perf.isWarnEnabled();
    }

    public void logDebug(Object arg0) {
        log(arg0, null);
    }

    public void logDebug(Object arg0, Throwable arg1) {
        log(arg0, arg1);
    }

    public void logError(Object arg0) {
        LiquidLog.sqltrace.error(arg0);
    }

    public void logError(Object arg0, Throwable arg1) {
        LiquidLog.sqltrace.error(arg0, arg1);
    }

    public void logFatal(Object arg0) {
        LiquidLog.sqltrace.fatal(arg0);
    }

    public void logFatal(Object arg0, Throwable arg1) {
        LiquidLog.sqltrace.fatal(arg0, arg1);
    }

    public void logInfo(Object arg0) {
        log(arg0, null);
    }

    public void logInfo(Object arg0, Throwable arg1) {
        log(arg0, arg1);
    }

    public void logTrace(Object arg0) {
        LiquidLog.sqltrace.trace(arg0);
    }

    public void logTrace(Object arg0, Throwable arg1) {
        LiquidLog.sqltrace.trace(arg0, arg1);
    }

    public void logWarn(Object arg0) {
        log(arg0, null);
    }

    public void logWarn(Object arg0, Throwable arg1) {
        log(arg0, arg1);
    }
}
