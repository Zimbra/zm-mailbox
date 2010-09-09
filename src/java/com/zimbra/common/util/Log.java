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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Wrapper around Log4j that supports <code>printf</code> functionality
 * via {@link String#format}.
 * 
 * @author bburtin
 *
 */
public class Log {

    private Map<String, Logger> mAccountLoggers = new ConcurrentHashMap<String, Logger>();
    
    private static final Map<Level, org.apache.log4j.Level> ZIMBRA_TO_LOG4J =
        new HashMap<Level, org.apache.log4j.Level>();
    private static final Map<org.apache.log4j.Level, Level> LOG4J_TO_ZIMBRA =
        new HashMap<org.apache.log4j.Level, Level>();
    
    static {
        ZIMBRA_TO_LOG4J.put(Level.error, org.apache.log4j.Level.ERROR);
        ZIMBRA_TO_LOG4J.put(Level.warn, org.apache.log4j.Level.WARN);
        ZIMBRA_TO_LOG4J.put(Level.info, org.apache.log4j.Level.INFO);
        ZIMBRA_TO_LOG4J.put(Level.debug, org.apache.log4j.Level.DEBUG);
        
        LOG4J_TO_ZIMBRA.put(org.apache.log4j.Level.FATAL, Level.error);
        LOG4J_TO_ZIMBRA.put(org.apache.log4j.Level.ERROR, Level.error);
        LOG4J_TO_ZIMBRA.put(org.apache.log4j.Level.WARN, Level.warn);
        LOG4J_TO_ZIMBRA.put(org.apache.log4j.Level.INFO, Level.info);
        LOG4J_TO_ZIMBRA.put(org.apache.log4j.Level.DEBUG, Level.debug);
    }
    
    public enum Level {
        error, warn, info, debug;
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
    
    private static String getAccountCategory(String category, String accountName) {
        // appender additivity
        // The output of a log statement of logger C will go to all the appenders in C and its ancestors.
        // For example; if account logger is added for category zimbra.sync, logs will be sent to logger zimbra.sync
        return String.format("%s.%s", category, accountName);
    }
    
    public boolean isDebugEnabled() {
        return getLogger().isDebugEnabled();
    }
    
    public boolean isInfoEnabled() {
        return getLogger().isInfoEnabled();
    }
    
    public boolean isWarnEnabled() {
        return getLogger().isEnabledFor(Priority.WARN);
    }
    
    public boolean isErrorEnabled() {
        return getLogger().isEnabledFor(Priority.ERROR);
    }

    public boolean isFatalEnabled() {
        return getLogger().isEnabledFor(Priority.FATAL);
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
