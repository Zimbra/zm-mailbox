/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.PropertyConfigurator;

/**
 * Log categories.
 *
 * @author schemers
 */
public final class ZimbraLog {

    /**
     * "ip" key for context. IP of request
     */
    private static final String C_IP = "ip";

    /**
     * "oip" key for context. originating IP of request
     */
    private static final String C_OIP = "oip";

    /**
     * "id" key for context. Id of the target account
     */
    public static final String C_ID = "id";

    /**
     * "name" key for context. Id of the target account
     */
    public static final String C_NAME = "name";

    /**
     * "aid" key for context. Id in the auth token. Only present if target id is
     * different then auth token id.
     */
    public static final String C_AID = "aid";

    /**
     * "aname" key for context. name in the auth token. Only present if target id is
     * different then auth token id.
     */
    public static final String C_ANAME = "aname";

    /**
     * "cid" is the connection id of a server that is monotonically increasing - useful
     * for tracking individual connections.
     */
    public static final String C_CONNECTIONID = "cid";

    /**
     * "mid" key for context. Id of requested mailbox. Only present if request is
     * dealing with a mailbox.
     */
    public static final String C_MID = "mid";

    /**
     * "ua" key for context.  The name of the client application.
     */
    private static final String C_USER_AGENT = "ua";

    /**
     * List of IP addresses and user-agents of the proxy chain.
     * was sent.
     */
    private static final String C_VIA = "via";

    /**
     * "msgid" key for context.  The Message-ID header of the message being
     * operated on.
     */
    private static final String C_MSG_ID = "msgid";

    /**
     * "item" key for context.
     */
    private static final String C_ITEM = "item";

    /**
     * "ds" key for context.  The name of the Data Source being operated on.
     */
    private static final String C_DATA_SOURCE_NAME = "ds";

    /**
     * "port" key for context.  The server port to which the client connected.
     */
    private static final String C_PORT = "port";

    /**
     * the "zimbra.misc" logger. For all events that don't have a specific-catagory.
     */
    public static final Log misc = LogFactory.getLog("zimbra.misc");

    /**
     * the "zimbra.net" logger. For logging of network activities
     */
    public static final Log net = LogFactory.getLog("zimbra.net");

    /**
     * the "zimbra.index" logger. For general indexing-related events.
     */
    public static final Log index = LogFactory.getLog("zimbra.index");

    /**
     * the "zimbra.index.lucene" logger. For logging of low-level lucene operations (debug-level only)
     */
    public static final Log index_lucene = LogFactory.getLog("zimbra.index.lucene");

    /**
     * the "zimbra.index.search" logger. For logging of the search side of indexing
     */
    public static final Log index_search = LogFactory.getLog("zimbra.index.search");

    /**
     * the "zimbra.index.add" logger. For the add-to-the-index part of indexing
     */
    public static final Log index_add = LogFactory.getLog("zimbra.index.indexadd");


    /**
     * Fhe "zimbra.searchstat" logger.  For logging statistics about what kinds of searches are run
     */
    public static final Log searchstats = LogFactory.getLog("zimbra.searchstats");

    /**
     * the "zimbra.redolog" logger. For redolog-releated events.
     */
    public static final Log redolog = LogFactory.getLog("zimbra.redolog");

    /**
     * the "zimbra.lmtp" logger. For LMTP-related events.
     */
    public static final Log lmtp = LogFactory.getLog("zimbra.lmtp");

    /**
     * the "zimbra.smtp" logger. For SMTP-related events.
     */
    public static final Log smtp = LogFactory.getLog("zimbra.smtp");

    /**
     * the "zimbra.nio" logger. For NIO-related events.
     */
    public static final Log nio = LogFactory.getLog("zimbra.nio");

    /**
     * the "zimbra.imap" logger. For IMAP-related events.
     */
    public static final Log imap = LogFactory.getLog("zimbra.imap");

    /**
     * the "zimbra.pop" logger. For POP-related events.
     */
    public static final Log pop = LogFactory.getLog("zimbra.pop");

    /**
     * the "zimbra.mailbox" logger. For mailbox-related events.
     */
    public static final Log mailbox = LogFactory.getLog("zimbra.mailbox");

    /**
     * the "zimbra.calendar" logger. For calendar-related events.
     */
    public static final Log calendar = LogFactory.getLog("zimbra.calendar");

    /**
     * the "zimbra.im" logger. For instant messaging-related events.
     */
    public static final Log im = LogFactory.getLog("zimbra.im");

    /**
     * the "zimbra.im.intercept" logger. The IM packet interceptor (IM protocol logger)
     */
    public static final Log im_intercept= LogFactory.getLog("zimbra.im.intercept");

    /**
     * the "zimbra.account" logger. For account-related events.
     */
    public static final Log account = LogFactory.getLog("zimbra.account");

    /**
     * the "zimbra.gal" logger. For gal-related events.
     */
    public static final Log gal = LogFactory.getLog("zimbra.gal");

    /**
     * the "zimbra.ldap" logger. For ldap-related events.
     */
    public static final Log ldap = LogFactory.getLog("zimbra.ldap");

    /**
     * the "zimbra.acl" logger. For acl-related events.
     */
    public static final Log acl = LogFactory.getLog("zimbra.acl");

    /**
     * the "zimbra.security" logger. For security-related events
     */
    public static final Log security = LogFactory.getLog("zimbra.security");

    /**
     * the "zimbra.soap" logger. For soap-related events
     */
    public static final Log soap = LogFactory.getLog("zimbra.soap");

    /**
     * the "zimbra.test" logger. For testing-related events
     */
    public static final Log test = LogFactory.getLog("zimbra.test");

    /**
     * the "zimbra.sqltrace" logger. For tracing SQL statements sent to the database
     */
    public static final Log sqltrace = LogFactory.getLog("zimbra.sqltrace");

    /**
     * the "zimbra.dbconn" logger. For tracing database connections
     */
    public static final Log dbconn = LogFactory.getLog("zimbra.dbconn");

    /**
     * the "zimbra.perf" logger. For logging performance statistics
     */
    public static final Log perf = LogFactory.getLog("zimbra.perf");

    /**
     * the "zimbra.cache" logger. For tracing object cache activity
     */
    public static final Log cache = LogFactory.getLog("zimbra.cache");

    /**
     * the "zimbra.filter" logger. For filter-related logs.
     */
    public static final Log filter = LogFactory.getLog("zimbra.filter");

    /**
     * the "zimbra.session" logger. For session- and notification-related logs.
     */
    public static final Log session = LogFactory.getLog("zimbra.session");

    /**
     * the "zimbra.backup" logger. For backup/restore-related logs.
     */
    public static final Log backup = LogFactory.getLog("zimbra.backup");

    /**
     * the "zimbra.system" logger. For startup/shutdown and other related logs.
     */
    public static final Log system = LogFactory.getLog("zimbra.system");

    /**
     * the "zimbra.sync" logger. For sync client interface logs.
     */
    public static final Log sync = LogFactory.getLog("zimbra.sync");

    /**
     * the "zimbra.synctrace" logger. For sync client interface logs.
     */
    public static final Log synctrace = LogFactory.getLog("zimbra.synctrace");

    /**
     * the "zimbra.syncstate" logger. For sync client interface logs.
     */
    public static final Log syncstate = LogFactory.getLog("zimbra.syncstate");

    /**
     * the "zimbra.wbxml" logger. For wbxml client interface logs.
     */
    public static final Log wbxml = LogFactory.getLog("zimbra.wbxml");

    /**
     * the "zimbra.xsync" logger. For xsync client interface logs.
     */
    public static final Log xsync = LogFactory.getLog("zimbra.xsync");

    /**
     * the "zimbra.extensions" logger. For logging extension loading related info.
     */
    public static final Log extensions = LogFactory.getLog("zimbra.extensions");

    /**
     * the "zimbra.zimlet" logger. For logging zimlet related info.
     */
    public static final Log zimlet = LogFactory.getLog("zimbra.zimlet");

    /**
     * the "zimbra.wiki" logger. For wiki and document sharing.
     */
    public static final Log wiki = LogFactory.getLog("zimbra.wiki");

    /**
     * the "zimbra.op" logger. Logs server operations
     */
    public static final Log op = LogFactory.getLog("zimbra.op");

    /**
     * the "zimbra.dav" logger. Logs dav operations
     */
    public static final Log dav = LogFactory.getLog("zimbra.dav");

    /**
     * the "zimbra.io" logger.  Logs file IO operations.
     */
    public static final Log io = LogFactory.getLog("zimbra.io");

    /**
     * the "zimbra.datasource" logger.  Logs data source operations.
     */
    public static final Log datasource = LogFactory.getLog("zimbra.datasource");

    /**
     * remote management.
     */
    public static final Log rmgmt = LogFactory.getLog("zimbra.rmgmt");

    /**
     * the "zimbra.webclient" logger. Logs ZimbraWebClient servlet and jsp operations.
     */
    public static final Log webclient = LogFactory.getLog("zimbra.webclient");

    /**
     * the "zimbra.scheduler" logger.  Logs scheduled task operations.
     */
    public static final Log scheduler = LogFactory.getLog("zimbra.scheduler");

    /**
     * the "zimbra.store" logger.  Logs filesystem storage operations.
     */
    public static final Log store = LogFactory.getLog("zimbra.store");

    /**
     * the "zimbra.fb" logger.  Logs free/busy operations.
     */
    public static final Log fb = LogFactory.getLog("zimbra.fb");

    /**
     * the "zimbra.purge" logger.  Logs mailbox purge operations.
     */
    public static final Log purge = LogFactory.getLog("zimbra.purge");

    /**
     * the "zimbra.mailop" logger.  Logs changes to items in the mailbox.
     */
    public static final Log mailop = LogFactory.getLog("zimbra.mailop");

    /**
     * "zimbra.slogger" logger.  Used for "logger service", publishes stats events to syslog
     */
    public static final Log slogger = LogFactory.getLog("zimbra.slogger");

    /**
     * the "zimbra.mbxmgr" logger is used to track mailbox loading/maintenance mode
     */
    public static final Log mbxmgr = LogFactory.getLog("zimbra.mbxmgr");

    /**
     * "zimbra.tnef" logger.  Logs TNEF conversion operations.
     */
    public static final Log tnef = LogFactory.getLog("zimbra.tnef");


    /**
     * Maps the log category name to its description.
     */
    public static final Map<String, String> CATEGORY_DESCRIPTIONS;

    private ZimbraLog() {
    }

    /**
     * Returns a new <tt>Set</tt> that contains the values of
     * {@link #C_NAME} and {@link #C_ANAME} if they are set.
     */
    public static Set<String> getAccountNamesFromContext() {
        Map<String, String> contextMap = sContextMap.get();
        if (contextMap == null) {
            return Collections.emptySet();
        }

        String name = contextMap.get(C_NAME);
        String aname = contextMap.get(C_ANAME);
        if (name == null && aname == null) {
            return Collections.emptySet();
        }

        Set<String> names = new HashSet<String>();
        if (name != null) {
            names.add(name);
        }
        if (aname != null) {
            names.add(aname);
        }
        return names;
    }

    private static final ThreadLocal<Map<String, String>> sContextMap = new ThreadLocal<Map<String, String>>();
    private static final ThreadLocal<String> sContextString = new ThreadLocal<String>();

    private static final Set<String> CONTEXT_KEY_ORDER = new LinkedHashSet<String>();

    static {
        CONTEXT_KEY_ORDER.add(C_NAME);
        CONTEXT_KEY_ORDER.add(C_ANAME);
        CONTEXT_KEY_ORDER.add(C_MID);
        CONTEXT_KEY_ORDER.add(C_IP);

        // Initialize log category descriptions.  Categories that don't have a description
        // won't be listed in zmprov online help.
        Map<String, String> descriptions = new TreeMap<String, String>();
        descriptions.put(misc.getCategory(), "Miscellaneous");
        descriptions.put(index.getCategory(), "Index operations");
        descriptions.put(redolog.getCategory(), "Redo log operations");
        descriptions.put(lmtp.getCategory(), "LMTP operations (incoming mail)");
        descriptions.put(smtp.getCategory(), "SMTP operations (outgoing mail)");
        descriptions.put(imap.getCategory(), "IMAP protocol operations");
        descriptions.put(pop.getCategory(), "POP protocol operations");
        descriptions.put(mailbox.getCategory(), "General mailbox operations");
        descriptions.put(calendar.getCategory(), "Calendar operations");
        descriptions.put(im.getCategory(), "Instant messaging operations");
        descriptions.put(account.getCategory(), "Account operations");
        descriptions.put(gal.getCategory(), "GAL operations");
        descriptions.put(ldap.getCategory(), "LDAP operations");
        descriptions.put(acl.getCategory(), "ACL operations");
        descriptions.put(security.getCategory(), "Security events");
        descriptions.put(soap.getCategory(), "SOAP protocol");
        descriptions.put(sqltrace.getCategory(), "SQL tracing");
        descriptions.put(dbconn.getCategory(), "Database connection tracing");
        descriptions.put(cache.getCategory(), "In-memory cache operations");
        descriptions.put(filter.getCategory(), "Mail filtering");
        descriptions.put(session.getCategory(), "User session tracking");
        descriptions.put(backup.getCategory(), "Backup and restore");
        descriptions.put(system.getCategory(), "Startup/shutdown and other system messages");
        descriptions.put(sync.getCategory(), "Sync client operations");
        descriptions.put(extensions.getCategory(), "Server extension loading");
        descriptions.put(zimlet.getCategory(), "Zimlet operations");
        descriptions.put(wiki.getCategory(), "Wiki operations");
        descriptions.put(mailop.getCategory(), "Changes to mailbox state");
        descriptions.put(dav.getCategory(), "DAV operations");
        descriptions.put(io.getCategory(), "Filesystem operations");
        descriptions.put(store.getCategory(), "Mail store disk operations");
        descriptions.put(purge.getCategory(), "Mailbox purge operations");
        descriptions.put(datasource.getCategory(), "Data Source operations");
        CATEGORY_DESCRIPTIONS = Collections.unmodifiableMap(descriptions);
    }

    static String getContextString() {
        return sContextString.get();
    }

    //this is called from offline and only at LC init so we are taking chances with race
    private static final Set<String> CONTEXT_FILTER = new HashSet<String>();
    public static void addContextFilters(String filters) {
        for (String item : filters.split(","))
            CONTEXT_FILTER.add(item);
    }

    /**
     * Adds a key/value pair to the current thread's logging context.  If
     * <tt>key</tt> is null, does nothing.  If <tt>value</tt> is null,
     * removes the context entry.
     */
    public static void addToContext(String key, String value) {
        if (key == null || CONTEXT_FILTER.contains(key))
            return;

        Map<String, String> contextMap = sContextMap.get();
        boolean contextChanged = false;

        if (StringUtil.isNullOrEmpty(value)) {
            // Remove
            if (contextMap != null) {
                String oldValue = contextMap.remove(key);
                if (oldValue != null) {
                    contextChanged = true;
                }
            }
        } else {
            // Add
            if (contextMap == null) {
                contextMap = new LinkedHashMap<String, String>();
                sContextMap.set(contextMap);
            }
            String oldValue = contextMap.put(key, value);
            if (!StringUtil.equal(oldValue, value)) {
                contextChanged = true;
            }
        }
        if (contextChanged) {
            updateContextString();
        }
    }

    /**
     * Updates the context string with the latest data in {@link #sContextMap}.
     */
    private static void updateContextString() {
        Map<String, String> contextMap = sContextMap.get();
        if (contextMap == null || contextMap.size() == 0) {
            sContextString.set(null);
            return;
        }

        StringBuilder sb = new StringBuilder();

        // Append ordered keys first
        for (String key : CONTEXT_KEY_ORDER) {
            String value = contextMap.get(key);
            if (value != null) {
                encodeArg(sb, key, value);
            }
        }

        // Append the rest
        for (String key : contextMap.keySet()) {
            if (!CONTEXT_KEY_ORDER.contains(key)) {
                String value = contextMap.get(key);
                if (key != null && value != null) {
                    encodeArg(sb, key, value);
                }
            }
        }

        sContextString.set(sb.toString());
    }

    /**
     * Adds a <tt>MailItem</tt> id to the current thread's
     * logging context.
     */
    public static void addItemToContext(int itemId) {
        addToContext(C_ITEM, Integer.toString(itemId));
    }

    /**
     * Removes a key/value pair from the current thread's logging context.
     */
    public static void removeFromContext(String key) {
        if (key != null) {
            addToContext(key, null);
        }
    }

    /**
     * Removes a <tt>MailItem</tt> id from the current thread's
     * logging context.
     */
    public static void removeItemFromContext(int itemId) {
        removeFromContext(C_ITEM);
    }

    /**
     * Adds account name to the current thread's logging context.
     */
    public static void addAccountNameToContext(String accountName) {
        ZimbraLog.addToContext(C_NAME, accountName);
    }

    /**
     * Removes all account-specific values from the current thread's
     * logging context.
     */
    public static void removeAccountFromContext() {
        removeFromContext(C_ID);
        removeFromContext(C_MID);
        removeFromContext(C_NAME);
        removeFromContext(C_ANAME);
        removeFromContext(C_ITEM);
        removeFromContext(C_MSG_ID);
    }

    /**
     * Adds ip to the current thread's logging context.
     */
    public static void addIpToContext(String ipAddress) {
        ZimbraLog.addToContext(C_IP, ipAddress);
    }

    /**
     * Adds oip (originating IP) to the current thread's logging context.
     */
    public static void addOrigIpToContext(String ipAddress) {
        ZimbraLog.addToContext(C_OIP, ipAddress);
    }

    /**
     * Adds connection id to the current thread's logging context.
     */
    public static void addConnectionIdToContext(String connectionId) {
        ZimbraLog.addToContext(C_CONNECTIONID, connectionId);
    }

    /**
     * Adds mailbox id to the current thread's logging context.
     */
    public static void addMboxToContext(long mboxId) {
        addToContext(C_MID, Long.toString(mboxId));
    }

    /**
     * Removes mailbox id from the current thread's logging context.
     */
    public static void removeMboxFromContext() {
        removeFromContext(C_MID);
    }

    /**
     * Adds message id to the current thread's logging context.
     */
    public static void addMsgIdToContext(String messageId) {
        addToContext(C_MSG_ID, messageId);
    }

    /**
     * Adds data source name to the current thread's logging context.
     */
    public static void addDataSourceNameToContext(String dataSourceName) {
        addToContext(C_DATA_SOURCE_NAME, dataSourceName);
    }

    /**
     * Removes data source name from the current thread's logging context.
     */
    public static void removeDataSourceNameFromContext() {
        removeFromContext(C_DATA_SOURCE_NAME);
    }

    /**
     * Adds port to the current thread's logging context.
     */
    public static void addPortToContext(int port) {
        ZimbraLog.addToContext(C_PORT, Integer.toString(port));
    }

    /**
     * Adds user agent to the current thread's logging context.
     */
    public static void addUserAgentToContext(String ua) {
        ZimbraLog.addToContext(C_USER_AGENT, ua);
    }

    /**
     * Adds {@code via} to the current thread's logging context.
     *
     * @param value
     */
    public static void addViaToContext(String value) {
        ZimbraLog.addToContext(C_VIA, value);
    }

    /**
     * Clears the current thread's logging context.
     *
     */
    public static void clearContext() {
        Map<String, String> contextMap = sContextMap.get();
        if (contextMap != null) {
            contextMap.clear();
        }
        sContextString.remove();
    }

    /**
     * Setup log4j for our command line tools.
     *
     * If System.getProperty(zimbra.log4j.level) is set then log at that level.
     * Else log at the specified defaultLevel.
     */
    public static void toolSetupLog4j(String defaultLevel, String logFile, boolean showThreads) {
        String level = System.getProperty("zimbra.log4j.level");
        if (level == null) {
            level = defaultLevel;
        }
        Properties p = new Properties();
        p.put("log4j.rootLogger", level + ",A1");
        if (logFile != null) {
            p.put("log4j.appender.A1", "org.apache.log4j.FileAppender");
            p.put("log4j.appender.A1.File", logFile);
            p.put("log4j.appender.A1.Append", "false");
        } else {
            p.put("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        }
        p.put("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        if (showThreads) {
            p.put("log4j.appender.A1.layout.ConversionPattern", "[%t] [%x] %p: %m%n");
        } else {
            p.put("log4j.appender.A1.layout.ConversionPattern", "[%x] %p: %m%n");
        }
        PropertyConfigurator.configure(p);
    }

    public static void toolSetupLog4jConsole(String defaultLevel, boolean stderr, boolean showThreads) {
        String level = System.getProperty("zimbra.log4j.level");
        if (level == null) {
            level = defaultLevel;
        }
        Properties p = new Properties();
        p.put("log4j.rootLogger", level + ",A1");

        p.put("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        if (stderr)
            p.put("log4j.appender.A1.target", "System.err");

        p.put("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        if (showThreads) {
            p.put("log4j.appender.A1.layout.ConversionPattern", "[%t] [%x] %p: %m%n");
        } else {
            p.put("log4j.appender.A1.layout.ConversionPattern", "[%x] %p: %m%n");
        }
        PropertyConfigurator.configure(p);
    }

    /**
     * Setup log4j for command line tool using specified log4j.properties file.
     * If file doesn't exist System.getProperty(zimbra.home)/conf/log4j.properties
     * file will be used.
     * @param defaultLevel
     * @param propsFile full path to log4j.properties file
     */
    public static void toolSetupLog4j(String defaultLevel, String propsFile) {
        if (propsFile != null && new File(propsFile).exists()) {
            PropertyConfigurator.configure(propsFile);
        } else {
            toolSetupLog4j(defaultLevel, null, false);
        }
    }

    private static void encodeArg(StringBuilder sb, String name, String value) {
        if (value == null) {
            value = "";
        }
        if (value.indexOf(';') != -1) {
            value = value.replaceAll(";", ";;");
        }
        // replace returns ref to original string if char to replace doesn't exist
        value = value.replace('\r', ' ');
        value = value.replace('\n', ' ');
        sb.append(name);
        sb.append("=");
        sb.append(value);
        sb.append(';');
    }

    /**
     * Take an array of Strings [ "name1", "value1", "name2", "value2", ...] and
     * format them for logging purposes.
     *
     * @param strings
     * @return formatted string
     */
    public static String encodeAttrs(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i += 2) {
            if (i > 0) {
                sb.append(' ');
            }
            encodeArg(sb, args[i], args[i + 1]);
        }
        return sb.toString();
    }

    /**
     * Take an array of Strings [ "name1", "value1", "name2", "value2", ...] and
     * format them for logging purposes into: <tt>name1=value1; name2=value;</tt>.
     * Semicolons are escaped with two semicolons (value a;b is encoded as a;;b).
     *
     * @param strings
     * @return formatted string
     */
    public static String encodeAttrs(String[] args, Map<String, ?> extraArgs) {
        StringBuilder sb = new StringBuilder();
        boolean needSpace = false;
        for (int i = 0; i < args.length; i += 2) {
            if (needSpace) {
                sb.append(' ');
            } else {
                needSpace = true;
            }
            encodeArg(sb, args[i], args[i + 1]);
        }
        if (extraArgs != null) {
            for (Map.Entry<String, ?> entry : extraArgs.entrySet()) {
                if (needSpace) {
                    sb.append(' ');
                } else {
                    needSpace = true;
                }
                String name = entry.getKey();
                Object value = entry.getValue();
                if (value == null) {
                    encodeArg(sb, name, "");
                } else if (value instanceof String) {
                    encodeArg(sb, name, (String) value);
                } else if (value instanceof String[]) {
                    for (String arg : (String[]) value) {
                        encodeArg(sb, name, arg);
                    }
                }
            }
        }
        return sb.toString();
    }
}
