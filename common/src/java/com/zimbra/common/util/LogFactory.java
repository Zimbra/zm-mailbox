/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import com.zimbra.common.localconfig.LC;

/**
 * Factory that creates and manages instances of {@link Log}.
 *
 * @author bburtin
 */
public final class LogFactory {

    private static final ConcurrentMap<String, Log> NAME2LOG = new ConcurrentHashMap<String, Log>();

    private LogFactory() {
    }

    public synchronized static void init() {
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        File file = new File(LC.zimbra_log4j_properties.value());
        // this will force a reconfiguration
        context.setConfigLocation(file.toURI());
    }

    public synchronized static void reset() {
        ZimbraLog.misc.info("Resetting all loggers");
        // LogManager.resetConfiguration() will set all logger's level to null. We don't want to leave account loggers
        // with that state.
        for (Log log : getAllLoggers()) {
            log.removeAccountLoggers();
        }
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        context.reconfigure();
    }

    public static Log getLog(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return getLog(clazz.getName());
    }

    public static Log getLog(String name) {
        Log log = NAME2LOG.get(name);
        if (log == null) {
            log = new Log(LogManager.getLogger(name));
            Log prev = NAME2LOG.putIfAbsent(name, log);
            if (prev != null) {
                log = prev;
            }
        }
        return log;
    }

    /**
     * Returns <tt>true</tt> if a logger with the given name exists.
     */
    public static boolean logExists(String name) {
        return (LogManager.exists(name));
        
    }

    /**
     * Returns all account loggers that have been created since the last server start, or
     * an empty <tt>List</tt>.
     */
    public static List<AccountLogger> getAllAccountLoggers() {
        List<AccountLogger> accountLoggers = new ArrayList<AccountLogger>();
        List<Log> allLogs = new ArrayList<Log>(NAME2LOG.values());
        for (Log log : allLogs) {
            List<AccountLogger> alForCategory = log.getAccountLoggers();
            if (alForCategory != null) {
                accountLoggers.addAll(alForCategory);
            }
        }
        return accountLoggers;
    }

    /**
     * Returns all the loggers that have been created with {@link #getLog}.  The
     * <tt>Collection</tt> does not include account loggers.
     */
    public static Collection<Log> getAllLoggers() {
        return new ArrayList<Log>(NAME2LOG.values());
    }
}
