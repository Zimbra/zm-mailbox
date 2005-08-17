/*
 */
package com.liquidsys.coco.util;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.NDC;
import org.apache.log4j.PropertyConfigurator;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author schemers
 *
 */
public class LiquidLog {
    
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
     * "mid" key for context. Id of requested mailbox. Only present if request is
     * dealing with a mailbox.
     */
    private static final String C_MID = "mid";    
    
    /**
     * the "liquid.misc" logger. For all events that don't have a specific-catagory.
     */
    public static final org.apache.commons.logging.Log misc = LogFactory.getLog("liquid.misc");
    
    /**
     * the "liquid.index" logger. For indexing-related events.
     */
    public static final org.apache.commons.logging.Log index = LogFactory.getLog("liquid.index");
    
    /**
     * the "liquid.journal" logger. For journal-releated events.
     */
    public static final org.apache.commons.logging.Log journal = LogFactory.getLog("liquid.journal");
    
    /**
     * the "liquid.lmtp" logger. For LMTP-related events.
     */
    public static final org.apache.commons.logging.Log lmtp = LogFactory.getLog("liquid.lmtp");
    
    /**
     * the "liquid.imap" logger. For IMAP-related events.
     */
    public static final org.apache.commons.logging.Log imap = LogFactory.getLog("liquid.imap");
    
    /**
     * the "liquid.imap" logger. For POP-related events.
     */
    public static final org.apache.commons.logging.Log pop = LogFactory.getLog("liquid.pop");
    
    /**
     * the "liquid.mailbox" logger. For mailbox-related events.
     */
    public static final org.apache.commons.logging.Log mailbox = LogFactory.getLog("liquid.mailbox");
    
    /**
     * the "liquid.calendar" logger. For calendar-related events.
     */
    public static final org.apache.commons.logging.Log calendar = LogFactory.getLog("liquid.calendar");
    
    /**
     * the "liquid.account" logger. For account-related events.
     */
    public static final org.apache.commons.logging.Log account = LogFactory.getLog("liquid.account");
    
    /**
     * the "liquid.security" logger. For security-related events
     */
    public static final org.apache.commons.logging.Log security = LogFactory.getLog("liquid.security");    

    /**
     * the "liquid.soap" logger. For soap-related events
     */
    public static final org.apache.commons.logging.Log soap = LogFactory.getLog("liquid.soap");

    /**
     * the "liquid.test" logger. For testing-related events
     */
    public static final org.apache.commons.logging.Log test = LogFactory.getLog("liquid.test");

    /**
     * the "liquid.sqltrace" logger. For tracing SQL statements sent to the database
     */
    public static final org.apache.commons.logging.Log sqltrace = LogFactory.getLog("liquid.sqltrace");

    /**
     * the "liquid.perf" logger. For logging performance statistics
     */
    public static final org.apache.commons.logging.Log perf = LogFactory.getLog("liquid.perf");

    /**
     * the "liquid.cache" logger. For tracing object cache activity
     */
    public static final org.apache.commons.logging.Log cache = LogFactory.getLog("liquid.cache");
    
    /**
     * the "liquid.ozserver" logger. For tracing ozserver framework
     */
    public static final org.apache.commons.logging.Log ozserver = LogFactory.getLog("liquid.ozserver");
    
    /**
     * the "liquid.filter" logger. For filter-related logs.
     */
    public static final org.apache.commons.logging.Log filter = LogFactory.getLog("liquid.filter");
    
    /**
     * the "liquid.backup" logger. For backup/restore-related logs.
     */
    public static final org.apache.commons.logging.Log backup = LogFactory.getLog("liquid.backup");
    
    /**
     * the "liquid.system" logger. For startup/shutdown and other related logs.
     */
    public static final org.apache.commons.logging.Log system = LogFactory.getLog("liquid.system");
    
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
            LiquidLog.misc.warn("unable to lookup account for log, id: "+id, se);
        }
        if (acct == null) {
            LiquidLog.addToContext(idOnlyKey, id);
        } else {
            LiquidLog.addToContext(nameKey, acct.getName());
            
        }
    }
    
    /**
     * 
     * @param name account name
     */
    public static void addAccountNameToContext(String name) {
        LiquidLog.addToContext(C_NAME, name);
    }
    
    /**
     * 
     * @param ip ip address
     */
    public static void addIpToContext(String ip) {
        LiquidLog.addToContext(C_IP, ip);
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
     * If System.getProperty(liquid.log4j.level) is set then log at that level.
     * Else log at the specified defaultLevel.
     */
    public static void toolSetupLog4j(String defaultLevel) {
        String level = System.getProperty("liquid.log4j.level");
        if (level == null) {
            level = defaultLevel;
        }
        Properties p = new Properties();
        p.put("log4j.rootLogger", level + ",A1");
        p.put("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        p.put("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        p.put("log4j.appender.A1.layout.ConversionPattern", "%p: %m%n");
        PropertyConfigurator.configure(p);
    }

    /**
     * Setup log4j for command line tool using specified log4j.properties file.
     * If file doesn't exist System.getProperty(liquid.home)/conf/log4j.properties
     * file will be used.
     * @param defaultLevel
     * @param propsFile full path to log4j.properties file
     */
    public static void toolSetupLog4j(String defaultLevel, String propsFile) {   
        if (propsFile != null && new File(propsFile).exists()) {
        	PropertyConfigurator.configure(propsFile);
            return;
        }
        toolSetupLog4j(defaultLevel);
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
