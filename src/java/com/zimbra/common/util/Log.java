/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

/**
 * Wrapper around Log4j that supports <code>printf</code> functionality
 * via {@link String#format}.
 *
 * @author bburtin
 */
public class Log {

    private final Map<String, Logger> mAccountLoggers = new ConcurrentHashMap<String, Logger>();

    private static final Map<Level, org.apache.log4j.Level> ZIMBRA_TO_LOG4J =
        new EnumMap<Level, org.apache.log4j.Level>(Level.class);
    static {
        ZIMBRA_TO_LOG4J.put(Level.error, org.apache.log4j.Level.ERROR);
        ZIMBRA_TO_LOG4J.put(Level.warn, org.apache.log4j.Level.WARN);
        ZIMBRA_TO_LOG4J.put(Level.info, org.apache.log4j.Level.INFO);
        ZIMBRA_TO_LOG4J.put(Level.debug, org.apache.log4j.Level.DEBUG);
        ZIMBRA_TO_LOG4J.put(Level.trace, org.apache.log4j.Level.TRACE);
    }
    private static final Map<org.apache.log4j.Level, Level> LOG4J_TO_ZIMBRA =
        new ImmutableMap.Builder<org.apache.log4j.Level, Level>()
        .put(org.apache.log4j.Level.FATAL, Level.error)
        .put(org.apache.log4j.Level.ERROR, Level.error)
        .put(org.apache.log4j.Level.WARN, Level.warn)
        .put(org.apache.log4j.Level.INFO, Level.info)
        .put(org.apache.log4j.Level.DEBUG, Level.debug)
        .put(org.apache.log4j.Level.TRACE, Level.trace)
        .build();

    public enum Level {
        // Keep in sync with com.zimbra.soap.type.LoggingLevel
        error, warn, info, debug, trace;
    };

    private Logger mLogger;

    Log(Logger logger) {
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
        Logger accountLogger = mAccountLoggers.get(accountName);
        if (accountLogger == null) {
            String accountCategory = getAccountCategory(getCategory(), accountName);
            accountLogger = Logger.getLogger(accountCategory);
            mAccountLoggers.put(accountName, accountLogger);
        }
        accountLogger.setLevel(ZIMBRA_TO_LOG4J.get(level));
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
        Logger logger = mAccountLoggers.remove(accountName);
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
        return getLogger().isEnabledFor(org.apache.log4j.Level.WARN);
    }

    public boolean isErrorEnabled() {
        return getLogger().isEnabledFor(org.apache.log4j.Level.ERROR);
    }

    public boolean isFatalEnabled() {
        return getLogger().isEnabledFor(org.apache.log4j.Level.FATAL);
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


    public void setLevel(Level level) {
        mLogger.setLevel(ZIMBRA_TO_LOG4J.get(level));
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
            Logger log4jLogger = mAccountLoggers.get(accountName);
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
    private Logger getLogger() {
        if (mAccountLoggers.size() == 0) {
            return mLogger;
        }
        for (String accountName : ZimbraLog.getAccountNamesFromContext()) {
            Logger logger = mAccountLoggers.get(accountName);
            if (logger != null) {
                return logger;
            }
        }
        return mLogger;
    }
}
