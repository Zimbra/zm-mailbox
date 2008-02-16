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
package com.zimbra.common.util;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.PropertyConfigurator;


/**
 * @author schemers
 *
 */
public class ZimbraLog {
    
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
    private static final String C_MID = "mid";    

    /**
     * "ua" key for context.  The name of the client application. 
     */
    private static final String C_USER_AGENT = "ua";
    
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
     * the "zimbra.index" logger. For indexing-related events.
     */
    public static final Log index = LogFactory.getLog("zimbra.index");
    
    /**
     * the "zimbra.index.lucene" logger. For logging of low-level lucene operations (debug-level only)
     */
    public static final Log index_lucene = LogFactory.getLog("zimbra.index.lucene");
    
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
     * the "zimbra.imap" logger. For POP-related events.
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
     * the "zimbra.account" logger. For account-related events.
     */
    public static final Log account = LogFactory.getLog("zimbra.account");
    
    /**
     * the "zimbra.gal" logger. For account-related events.
     */
    public static final Log gal = LogFactory.getLog("zimbra.gal");
    
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
     * Keeps track of the account associated with the current thread, for
     * per-user logging settings. 
     */
    private static ThreadLocal<Set<String>> sAccountNames = new ThreadLocal<Set<String>>();

    static Set<String> getAccountNamesForThread() {
        Set<String> accountNames = sAccountNames.get();
        if (accountNames == null) {
            accountNames = new HashSet<String>();
            sAccountNames.set(accountNames);
        }
        return accountNames;
    }
    
    private static void addAccountForThread(String accountName) {
        getAccountNamesForThread().add(accountName);
    }

    private static final ThreadLocal<Map<String, String>> sContextMap = new ThreadLocal<Map<String, String>>();
    private static final ThreadLocal<String> sContextString = new ThreadLocal<String>();
    
    private static final Set<String> CONTEXT_KEY_ORDER = new LinkedHashSet<String>();
    
    static {
        CONTEXT_KEY_ORDER.add(C_NAME);
        CONTEXT_KEY_ORDER.add(C_ANAME);
        CONTEXT_KEY_ORDER.add(C_MID);
        CONTEXT_KEY_ORDER.add(C_IP);
    }
    
    static String getContextString() {
        return sContextString.get();
    }
    
    /**
     * Adds a key/value pair to the current thread's logging context.  If
     * <tt>key</tt> is null, does nothing.  If <tt>value</tt> is null,
     * removes the context entry.
     */
    public static void addToContext(String key, String value) {
        if (key == null)
            return;
        if (key.equals(C_NAME) || key.equals(C_ANAME)) {
            addAccountForThread(value);
        }

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
                contextMap = new HashMap<String, String>();
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
     * Updates the context string with the latest
     * data in {@link #sContextMap}.
     */
    private static void updateContextString() {
        Map<String, String> contextMap = sContextMap.get();
        if (contextMap == null || contextMap.size() == 0) {
            sContextString.set(null);
            return;
        }

        StringBuffer sb = new StringBuffer();

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
    public static void addMboxToContext(int mboxId) {
        addToContext(C_MID, Integer.toString(mboxId));
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
     * Clears the current thread's logging context.
     *
     */
    public static void clearContext() {
        Map<String, String> contextMap = sContextMap.get();
        if (contextMap != null) {
            contextMap.clear();
        }
        getAccountNamesForThread().clear();
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

    
    private static void encodeArg(StringBuffer sb, String name, String value) {
        if (value == null) value = "";
        if (value.indexOf(';') != -1) value = value.replaceAll(";", ";;");
        // replace returns ref to original string if char to replace doesn't exist
        value = value.replace('\r', ' ');        
        value = value.replace('\n', ' ');
        sb.append(name);
        sb.append("=");
        sb.append(value);
        sb.append(';');
    }

    /**
     * Take an array of Strings [ "name1", "value1", "name2", "value2", ...] and format them for logging purposes.
     * @param strings
     * @return
     */
    public static String encodeAttrs(String[] args) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i < args.length; i += 2) {
            if (i > 0) sb.append(' ');            
            encodeArg(sb, args[i], args[i+1]);
        }
        return sb.toString();
    }
    

    /**
     * Take an array of Strings [ "name1", "value1", "name2", "value2", ...] and format them for logging purposes
     * into: name1=value1; name2=value; semicolons are escaped with two semicolons (value a;b is encoded as a;;b)
     * @param strings
     * @return
     */
    public static String encodeAttrs(String[] args, Map extraArgs) {
        StringBuffer sb = new StringBuffer();
        boolean needSpace = false;
        for (int i=0; i < args.length; i += 2) {
            if (needSpace) sb.append(' '); else needSpace = true;
            encodeArg(sb, args[i], args[i+1]);
        }
        if (extraArgs != null) {
            for (Iterator it=extraArgs.entrySet().iterator(); it.hasNext();) {
                if (needSpace) sb.append(' '); else needSpace = true;                
                Map.Entry entry = (Entry) it.next();
                String name = (String) entry.getKey();
                Object v = entry.getValue();
                if (v == null) {
                    encodeArg(sb, name, "");
                } else if (v instanceof String) {
                    encodeArg(sb, name, (String)v);
                } else if (v instanceof String[]) {
                    String values[] = (String[]) v;
                    for (int i=0; i < values.length; i++) {
                        encodeArg(sb, name, values[i]);
                    }
                }
            }
        }
        return sb.toString();
    }
}
