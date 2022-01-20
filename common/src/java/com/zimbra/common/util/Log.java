/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.LogManager;
import com.google.common.collect.ImmutableMap;

/**
 * Wrapper around Log4j that supports <code>printf</code> functionality
 * via {@link String#format}.
 *
 * @author bburtin
 */
public class Log {

    private final Map<String, org.apache.logging.log4j.Logger> mAccountLoggers = new ConcurrentHashMap<String, org.apache.logging.log4j.Logger>();

    private static final Map<Level, org.apache.logging.log4j.Level> ZIMBRA_TO_LOG4J =
            new EnumMap<Level, org.apache.logging.log4j.Level>(Level.class);
    static {
        ZIMBRA_TO_LOG4J.put(Level.error, org.apache.logging.log4j.Level.ERROR);
        ZIMBRA_TO_LOG4J.put(Level.warn, org.apache.logging.log4j.Level.WARN);
        ZIMBRA_TO_LOG4J.put(Level.info, org.apache.logging.log4j.Level.INFO);
        ZIMBRA_TO_LOG4J.put(Level.debug, org.apache.logging.log4j.Level.DEBUG);
        ZIMBRA_TO_LOG4J.put(Level.trace, org.apache.logging.log4j.Level.TRACE);
    }
    private static final Map<org.apache.logging.log4j.Level, Level> LOG4J_TO_ZIMBRA =
        new ImmutableMap.Builder<org.apache.logging.log4j.Level, Level>()
        .put(org.apache.logging.log4j.Level.FATAL, Level.error)
        .put(org.apache.logging.log4j.Level.ERROR, Level.error)
        .put(org.apache.logging.log4j.Level.WARN, Level.warn)
        .put(org.apache.logging.log4j.Level.INFO, Level.info)
        .put(org.apache.logging.log4j.Level.DEBUG, Level.debug)
        .put(org.apache.logging.log4j.Level.TRACE, Level.trace)
        .build();

    public enum Level {
        // Keep in sync with com.zimbra.soap.type.LoggingLevel
        error, warn, info, debug, trace;
    };

    private final org.apache.logging.log4j.Logger mLogger;

    Log(org.apache.logging.log4j.Logger logger) {
        if (logger == null) {
            throw new IllegalStateException("logger cannot be null");
        }
        mLogger = logger;
    }

    /**
     * Adds an account-level logger whose log level may be different than
     * that of the main log category.
     *
     * @param accountName the account name
     * @param level the log level that applies only to the given account
     */
    public void addAccountLogger(String accountName, Level level) {
        if (accountName == null || level == null) {
            return;
        }

        // Create the account logger if it doesn't already exist.
        org.apache.logging.log4j.Logger accountLogger = mAccountLoggers.get(accountName);
        if (accountLogger == null) {
            String accountCategory = getAccountCategory(getCategory(), accountName);
            accountLogger = LogManager.getLogger(accountCategory);
            mAccountLoggers.put(accountName, accountLogger);
        }
        Configurator.setLevel(accountLogger.getName(), ZIMBRA_TO_LOG4J.get(level));
    }

    /**
     * Removes all account loggers from this log category.
     * @return the number of loggers removed
     */
    public int removeAccountLoggers() {
        int count = mAccountLoggers.size();
        mAccountLoggers.clear();
        return count;
    }

    /**
     * Removes the specified account logger from this log category.
     * @return <tt>true</tt> if the logger was removed
     */
    public boolean removeAccountLogger(String accountName) {
        org.apache.logging.log4j.Logger logger = mAccountLoggers.remove(accountName);
        return (logger != null);
    }

    /**
     * Returns the account logger category name in the following format:
     * {@code <category>.<accountName>.<category>}.  The first {@code <category>}
     * allows account loggers to inherit log settings from the parent category.
     * The second{@code <category>} makes sure that the category name in the
     * log file is written correctly (otherwise the account name would be used).
     */
    private static String getAccountCategory(String category, String accountName) {
        return String.format("%s.%s.%s", category, accountName, category);
    }

    public boolean isTraceEnabled() {
        return getLogger().isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return getLogger().isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return getLogger().isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return getLogger().isEnabled(org.apache.logging.log4j.Level.WARN);
    }

    public boolean isErrorEnabled() {
        return getLogger().isEnabled(org.apache.logging.log4j.Level.ERROR);
    }

    public boolean isFatalEnabled() {
        return getLogger().isEnabled(org.apache.logging.log4j.Level.FATAL);
    }

    public void trace(Object o) {
        getLogger().trace(o);
    }

    public void trace(Object o, Throwable t) {
        getLogger().trace(o, t);
    }

    public void trace(String format, Object ... objects) {
        if (isTraceEnabled()) {
            getLogger().trace(String.format(format, objects));
        }
    }

    public void trace(String format, Object o, Throwable t) {
        if (isTraceEnabled()) {
            getLogger().trace(String.format(format, o), t);
        }
    }

    public void trace(String format, Object o1, Object o2, Throwable t) {
        if (isTraceEnabled()) {
            getLogger().trace(String.format(format, o1, o2), t);
        }
    }

    public void trace(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isTraceEnabled()) {
            getLogger().trace(String.format(format, o1, o2, o3), t);
        }
    }

    public void trace(String format, Object o1, Object o2, Object o3, Object o4, Throwable t) {
        if (isTraceEnabled()) {
            getLogger().trace(String.format(format, o1, o2, o3, o4), t);
        }
    }

    public void trace(String format, Object o1, Object o2, Object o3, Object o4, Object o5, Throwable t) {
        if (isTraceEnabled()) {
            getLogger().trace(String.format(format, o1, o2, o3, o4, o5), t);
        }
    }

    public void debug(Object o) {
        getLogger().debug(o);
    }

    public void debug(Object o, Throwable t) {
        getLogger().debug(o, t);
    }

    public void debug(String format, Object ... objects) {
        if (isDebugEnabled()) {
            getLogger().debug(String.format(format, objects));
        }
    }

    public void debug(String format, Object o, Throwable t) {
        if (isDebugEnabled()) {
            getLogger().debug(String.format(format, o), t);
        }
    }

    public void debug(String format, Object o1, Object o2, Throwable t) {
        if (isDebugEnabled()) {
            getLogger().debug(String.format(format, o1, o2), t);
        }
    }

    public void debug(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isDebugEnabled()) {
            getLogger().debug(String.format(format, o1, o2, o3), t);
        }
    }

    public void debug(String format, Object o1, Object o2, Object o3, Object o4, Throwable t) {
        if (isDebugEnabled()) {
            getLogger().debug(String.format(format, o1, o2, o3, o4), t);
        }
    }

    public void debug(String format, Object o1, Object o2, Object o3, Object o4, Object o5, Throwable t) {
        if (isDebugEnabled()) {
            getLogger().debug(String.format(format, o1, o2, o3, o4, o5), t);
        }
    }


    public void info(Object o) {
        getLogger().info(o);
    }

    public void info(Object o, Throwable t) {
        getLogger().info(o, t);
    }

    public void info(String format, Object ... objects) {
        if (isInfoEnabled()) {
            getLogger().info(String.format(format, objects));
        }
    }

    public void info(String format, Object o, Throwable t) {
        if (isInfoEnabled()) {
            getLogger().info(String.format(format, o), t);
        }
    }

    public void info(String format, Object o1, Object o2, Throwable t) {
        if (isInfoEnabled()) {
            getLogger().info(String.format(format, o1, o2), t);
        }
    }

    public void info(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isInfoEnabled()) {
            getLogger().info(String.format(format, o1, o2, o3), t);
        }
    }

    public void info(String format, Object o1, Object o2, Object o3, Object o4, Throwable t) {
        if (isInfoEnabled()) {
            getLogger().info(String.format(format, o1, o2, o3, o4), t);
        }
    }

    public void info(String format, Object o1, Object o2, Object o3, Object o4, Object o5, Throwable t) {
        if (isInfoEnabled()) {
            getLogger().info(String.format(format, o1, o2, o3, o4, o5), t);
        }
    }



    public void warn(Object o) {
        getLogger().warn(o);
    }

    public void warn(Object o, Throwable t) {
        getLogger().warn(o, t);
    }

    public void warn(String format, Object ... objects) {
        if (isWarnEnabled()) {
            getLogger().warn(String.format(format, objects));
        }
    }

    public void warn(String format, Object o, Throwable t) {
        if (isWarnEnabled()) {
            getLogger().warn(String.format(format, o), t);
        }
    }

    public void warn(String format, Object o1, Object o2, Throwable t) {
        if (isWarnEnabled()) {
            getLogger().warn(String.format(format, o1, o2), t);
        }
    }

    public void warn(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isWarnEnabled()) {
            getLogger().warn(String.format(format, o1, o2, o3), t);
        }
    }

    public void warn(String format, Object o1, Object o2, Object o3, Object o4, Throwable t) {
        if (isWarnEnabled()) {
            getLogger().warn(String.format(format, o1, o2, o3, o4), t);
        }
    }

    public void warn(String format, Object o1, Object o2, Object o3, Object o4, Object o5, Throwable t) {
        if (isWarnEnabled()) {
            getLogger().warn(String.format(format, o1, o2, o3, o4, o5), t);
        }
    }

    /** Like warn but will only include full details of throwable at debug level. */
    public void warnQuietly(Object o, Throwable t) {
        if (!isWarnEnabled()) {
            return;
        }
        if (isDebugEnabled()) {
            getLogger().warn(o, t);
        } else {
            getLogger().warn(String.format("%s : %s", o, t.getMessage()));
        }
    }

    /** Like warn but provides stack trace info if debug is enabled */
    public void warnQuietlyFmt(String format, Object o) {
        if (!isWarnEnabled()) {
            return;
        }
        if (isDebugEnabled()) {
            warnQuietly(format, o, new Throwable("Throwable provided for stack detail"));
        }
        warn(format, o);
    }

    /** Like warn but will only include full details of throwable at debug level. */
    public void warnQuietly(String format, Object o, Throwable t) {
        if (!isWarnEnabled()) {
            return;
        }
        if (isDebugEnabled()) {
            getLogger().warn(String.format(format, o), t);
        } else {
            String msg = String.format(format, o);
            getLogger().warn(String.format("%s : %s", msg, t.getMessage()));
        }
    }

    public void error(Object o) {
        getLogger().error(o);
    }

    public void error(Object o, Throwable t) {
        getLogger().error(o, t);
    }

    public void error(String format, Object ... objects) {
        if (isErrorEnabled()) {
            getLogger().error(String.format(format, objects));
        }
    }

    public void error(String format, Object o, Throwable t) {
        if (isErrorEnabled()) {
            getLogger().error(String.format(format, o), t);
        }
    }

    public void error(String format, Object o1, Object o2, Throwable t) {
        if (isErrorEnabled()) {
            getLogger().error(String.format(format, o1, o2), t);
        }
    }

    public void error(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isErrorEnabled()) {
            getLogger().error(String.format(format, o1, o2, o3), t);
        }
    }

    public void error(String format, Object o1, Object o2, Object o3, Object o4, Throwable t) {
        if (isErrorEnabled()) {
            getLogger().error(String.format(format, o1, o2, o3, o4), t);
        }
    }

    public void error(String format, Object o1, Object o2, Object o3, Object o4, Object o5, Throwable t) {
        if (isErrorEnabled()) {
            getLogger().error(String.format(format, o1, o2, o3, o4, o5), t);
        }
    }

    /** Like error but will only include full details of throwable at debug level. */
    public void errorQuietly(Object o, Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        if (isDebugEnabled()) {
            getLogger().error(o, t);
        } else {
            getLogger().error(String.format("%s : %s", o, t.getMessage()));
        }
    }

    /** Like error but provides stack trace info if debug is enabled */
    public void errorQuietlyFmt(String format, Object o) {
        if (!isErrorEnabled()) {
            return;
        }
        if (isDebugEnabled()) {
            errorQuietly(format, o, new Throwable("Throwable provided for stack detail"));
        }
        error(format, o);
    }

    /** Like error but will only include full details of throwable at debug level. */
    public void errorQuietly(String format, Object o, Throwable t) {
        if (!isErrorEnabled()) {
            return;
        }
        if (isDebugEnabled()) {
            getLogger().error(String.format(format, o), t);
        } else {
            String msg = String.format(format, o);
            getLogger().error(String.format("%s : %s", msg, t.getMessage()));
        }
    }

    /**
     * Returns Log4j equivalent of {@code level} or org.apache.log4j.Level.TRACE
     */
    private static final org.apache.logging.log4j.Level log4jLevel(Level level) {
        org.apache.logging.log4j.Level log4jlevel = ZIMBRA_TO_LOG4J.get(level);
        if (log4jlevel == null) {
            log4jlevel = org.apache.logging.log4j.Level.TRACE;
        }
        return log4jlevel;
    }

    public boolean isEnabledFor(Level level) {
        return getLogger().isEnabled(log4jLevel(level));
    }

    public void fatal(Object o) {
        getLogger().fatal(o);
    }

    public void fatal(Object o, Throwable t) {
        getLogger().fatal(o, t);
    }

    public void fatal(String format, Object ... objects) {
        if (isFatalEnabled()) {
            getLogger().fatal(String.format(format, objects));
        }
    }

    public void fatal(String format, Object o, Throwable t) {
        if (isFatalEnabled()) {
            getLogger().fatal(String.format(format, o), t);
        }
    }

    public void fatal(String format, Object o1, Object o2, Throwable t) {
        if (isFatalEnabled()) {
            getLogger().fatal(String.format(format, o1, o2), t);
        }
    }

    public void fatal(String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isFatalEnabled()) {
            getLogger().fatal(String.format(format, o1, o2, o3), t);
        }
    }

    public void fatal(String format, Object o1, Object o2, Object o3, Object o4, Throwable t) {
        if (isFatalEnabled()) {
            getLogger().fatal(String.format(format, o1, o2, o3, o4), t);
        }
    }

    public void fatal(String format, Object o1, Object o2, Object o3, Object o4, Object o5, Throwable t) {
        if (isFatalEnabled()) {
            getLogger().fatal(String.format(format, o1, o2, o3, o4, o5), t);
        }
    }

    public void log(Level level, Object o) {
        getLogger().log(log4jLevel(level), o);
    }

    public void log(Level level, Object o, Throwable t) {
        getLogger().log(log4jLevel(level), o, t);
    }

    public void log(Level level, String format, Object ... objects) {
        if (isEnabledFor(level)) {
            getLogger().log(log4jLevel(level), String.format(format, objects));
        }
    }

    public void log(Level level, String format, Object o, Throwable t) {
        if (isEnabledFor(level)) {
            getLogger().log(log4jLevel(level), String.format(format, o), t);
        }
    }

    public void log(Level level, String format, Object o1, Object o2, Throwable t) {
        if (isEnabledFor(level)) {
            getLogger().log(log4jLevel(level), String.format(format, o1, o2), t);
        }
    }

    public void log(Level level, String format, Object o1, Object o2, Object o3, Throwable t) {
        if (isEnabledFor(level)) {
            getLogger().log(log4jLevel(level), String.format(format, o1, o2, o3), t);
        }
    }

    public void log(Level level, String format, Object o1, Object o2, Object o3, Object o4, Throwable t) {
        if (isEnabledFor(level)) {
            getLogger().log(log4jLevel(level), String.format(format, o1, o2, o3, o4), t);
        }
    }

    public void log(Level level, String format, Object o1, Object o2, Object o3, Object o4, Object o5, Throwable t) {
        if (isEnabledFor(level)) {
            getLogger().log(log4jLevel(level), String.format(format, o1, o2, o3, o4, o5), t);
        }
    }


    public void setLevel(Level level) {
        Configurator.setLevel(mLogger.getName(), ZIMBRA_TO_LOG4J.get(level));
    }

    public Level getLevel() {
        Level lev = LOG4J_TO_ZIMBRA.get(mLogger.getLevel());
        return (lev != null) ? lev : Level.info;
    }

    public String getCategory() {
        return mLogger.getName();
    }

    List<AccountLogger> getAccountLoggers() {
        if (mAccountLoggers == null) {
            return null;
        }
        List<AccountLogger> accountLoggers = new ArrayList<AccountLogger>();
        for (String accountName : mAccountLoggers.keySet()) {
            org.apache.logging.log4j.Logger log4jLogger = mAccountLoggers.get(accountName);
            AccountLogger al = new AccountLogger(mLogger.getName(), accountName,
                LOG4J_TO_ZIMBRA.get(log4jLogger.getLevel()));
            accountLoggers.add(al);
        }
        return accountLoggers;
    }

    /**
     * Returns the Log4j logger for this <tt>Log</tt>'s category.  If a custom
     * logger has been defined for an account associated with the current thread,
     * returns that logger instead.
     *
     * @see #addAccountLogger
     */
    private org.apache.logging.log4j.Logger getLogger() {
        if (mAccountLoggers.size() == 0) {
            return mLogger;
        }
        for (String accountName : ZimbraLog.getAccountNamesFromContext()) {
            org.apache.logging.log4j.Logger logger = mAccountLoggers.get(accountName);
            if (logger != null) {
                return logger;
            }
        }
        return mLogger;
    }
}
