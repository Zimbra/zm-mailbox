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
package com.zimbra.cs.util;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.NDC;
import org.apache.log4j.PropertyConfigurator;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 */
public class ZimbraLog {
    
    /**
     * "ip" key for context. IP of requset
     */
    public static final String C_IP = "ip";
    
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
     * the "zimbra.misc" logger. For all events that don't have a specific-catagory.
     */
    public static final org.apache.commons.logging.Log misc = LogFactory.getLog("zimbra.misc");
    
    /**
     * the "zimbra.index" logger. For indexing-related events.
     */
    public static final org.apache.commons.logging.Log index = LogFactory.getLog("zimbra.index");
    
    /**
     * the "zimbra.journal" logger. For journal-releated events.
     */
    public static final org.apache.commons.logging.Log journal = LogFactory.getLog("zimbra.journal");
    
    /**
     * the "zimbra.lmtp" logger. For LMTP-related events.
     */
    public static final org.apache.commons.logging.Log lmtp = LogFactory.getLog("zimbra.lmtp");
    
    /**
     * the "zimbra.imap" logger. For IMAP-related events.
     */
    public static final org.apache.commons.logging.Log imap = LogFactory.getLog("zimbra.imap");
    
    /**
     * the "zimbra.imap" logger. For POP-related events.
     */
    public static final org.apache.commons.logging.Log pop = LogFactory.getLog("zimbra.pop");
    
    /**
     * the "zimbra.mailbox" logger. For mailbox-related events.
     */
    public static final org.apache.commons.logging.Log mailbox = LogFactory.getLog("zimbra.mailbox");
    
    /**
     * the "zimbra.calendar" logger. For calendar-related events.
     */
    public static final org.apache.commons.logging.Log calendar = LogFactory.getLog("zimbra.calendar");
    
    /**
     * the "zimbra.account" logger. For account-related events.
     */
    public static final org.apache.commons.logging.Log account = LogFactory.getLog("zimbra.account");
    
    /**
     * the "zimbra.security" logger. For security-related events
     */
    public static final org.apache.commons.logging.Log security = LogFactory.getLog("zimbra.security");    

    /**
     * the "zimbra.soap" logger. For soap-related events
     */
    public static final org.apache.commons.logging.Log soap = LogFactory.getLog("zimbra.soap");

    /**
     * the "zimbra.test" logger. For testing-related events
     */
    public static final org.apache.commons.logging.Log test = LogFactory.getLog("zimbra.test");

    /**
     * the "zimbra.sqltrace" logger. For tracing SQL statements sent to the database
     */
    public static final org.apache.commons.logging.Log sqltrace = LogFactory.getLog("zimbra.sqltrace");

    /**
     * the "zimbra.perf" logger. For logging performance statistics
     */
    public static final org.apache.commons.logging.Log perf = LogFactory.getLog("zimbra.perf");

    /**
     * the "zimbra.cache" logger. For tracing object cache activity
     */
    public static final org.apache.commons.logging.Log cache = LogFactory.getLog("zimbra.cache");
    
    /**
     * the "zimbra.filter" logger. For filter-related logs.
     */
    public static final org.apache.commons.logging.Log filter = LogFactory.getLog("zimbra.filter");
    
    /**
     * the "zimbra.backup" logger. For backup/restore-related logs.
     */
    public static final org.apache.commons.logging.Log backup = LogFactory.getLog("zimbra.backup");
    
    /**
     * the "zimbra.system" logger. For startup/shutdown and other related logs.
     */
    public static final org.apache.commons.logging.Log system = LogFactory.getLog("zimbra.system");
    
    /**
     * the "zimbra.extensions" logger. For logging extension loading related info. 
     */
    public static final org.apache.commons.logging.Log extensions = LogFactory.getLog("zimbra.extensions");
    
    public static String getContext() {
        return NDC.peek();
    }

    /**
     * adds key/value to context. Doesn't check to see if already in context.
     * @param key
     * @param value
     */
    public static void addToContext(String key, String value) {
        if (key == null)
            return;
        String ndc = NDC.pop();
        if (checkContext(ndc, key))
        	NDC.push(key+"="+value+";"+ndc);
        else
            NDC.push(ndc);
    }
    
    /**
     * 
     * @param id account id to lookup
     * @param nameKey name key to add to context if account lookup is ok
     * @param idOnlyKey id key to add to context if account lookup fails
     */
    public static void addAccountToContext(String id, String nameKey, String idOnlyKey) {
        Account acct = null;
        try {
            acct = Provisioning.getInstance().getAccountById(id);
        } catch (ServiceException se) {
            ZimbraLog.misc.warn("unable to lookup account for log, id: "+id, se);
        }
        if (acct == null) {
            ZimbraLog.addToContext(idOnlyKey, id);
        } else {
            ZimbraLog.addToContext(nameKey, acct.getName());
            
        }
    }
    
    public static void addAccountNameToContext(String accountName) {
        ZimbraLog.addToContext(C_NAME, accountName);
    }
    
    public static void addIpToContext(String ipAddress) {
        ZimbraLog.addToContext(C_IP, ipAddress);
    }  
    
    public static void addConnectionIdToContext(String connectionId) {
        ZimbraLog.addToContext(C_CONNECTIONID, connectionId);
    }
    
    public static void addToContext(Mailbox mbx) {
        addToContext(C_MID, Integer.toString(mbx.getId()));
    }
    
    private static boolean checkContext(String context, String key) {
        if (context == null || key == null)
            return false;
        return (!context.startsWith(key + "=") && context.indexOf(";" + key + "=") == -1);
    }

    public static void clearContext() {
        NDC.clear();
    }

    /**
     * Setup log4j for our command line tools.  
     * 
     * If System.getProperty(zimbra.log4j.level) is set then log at that level.
     * Else log at the specified defaultLevel.
     */
    public static void toolSetupLog4j(String defaultLevel, boolean showThreads) {
        String level = System.getProperty("zimbra.log4j.level");
        if (level == null) {
            level = defaultLevel;
        }
        Properties p = new Properties();
        p.put("log4j.rootLogger", level + ",A1");
        p.put("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
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
            return;
        }
        toolSetupLog4j(defaultLevel, false);
    }

    
    private static void encodeArg(StringBuffer sb, String name, String value) {
        if (value.indexOf(';') != -1) value = value.replaceAll(";", ";;");
        // replace returns ref to original string if char to replace doesn't exist
        value = value.replace('\r', ' ');        
        value = value.replace('\n', ' ');
        sb.append(name);
        sb.append("=");
        sb.append(value != null ? value : "");
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
