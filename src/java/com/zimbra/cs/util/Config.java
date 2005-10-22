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

/*
 * Created on Apr 19, 2004
 */
package com.zimbra.cs.util;

import java.io.File;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.DbConfig;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class Config {

    public static final int D_LMTP_THREADS = 10;
    
    public static final int D_LMTP_BIND_PORT = 7025;
    public static final int D_IMAP_BIND_PORT = 143;
    public static final int D_IMAP_SSL_BIND_PORT = 993;
    public static final int D_POP3_BIND_PORT = 110;
    public static final int D_POP3_SSL_BIND_PORT = 995;
    
    public static final int D_SMTP_TIMEOUT = 60;
    public static final int D_SMTP_PORT = 25;

    // update ever 7 days by default
    public static final long D_ZIMBRA_LAST_LOGON_TIMESTAMP_FREQUENCY = 1000*60*60*24*7;
    
    private static Map mConfigMap;
    private static Timestamp mYoungest;
    
    private static void init(Timestamp ts) throws ServiceException {
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            mConfigMap = DbConfig.getAll(conn, ts);
            for (Iterator it = mConfigMap.values().iterator(); it.hasNext();) {
                DbConfig c = (DbConfig) it.next();
                if (mYoungest == null) {
                    mYoungest = c.getModified();
                } else if (c.getModified().after(mYoungest)) {
                    mYoungest = c.getModified();
                }
                //System.out.println("loaded "+c);
            }
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }

        Server serverConfig = Provisioning.getInstance().getLocalServer();
        boolean userServicesEnabled =
            serverConfig.getBooleanAttr(Provisioning.A_zimbraUserServicesEnabled, true);
        synchronized (sUserServicesEnabledGuard) {
            sUserServicesEnabled = userServicesEnabled;
        }
    }

    private static synchronized void initConfig() {
        if (mConfigMap == null) {
            try {
                init(null);
            } catch (Exception e) {
                Zimbra.halt("Config initialization failed", e);
            }
        }
    }
    
    /**
     * @param name
     * @return specified config item as a String, null if it doesn't exist.
     */
    public static synchronized String getString(String name, String defaultValue) {
        initConfig();
        DbConfig c =  (DbConfig) mConfigMap.get(name);
        return c != null ? c.getValue() : defaultValue;
    }

    /**
     * true if value in config equals (ignoring case): "yes", "true", or "1".
     * @param name
     * @return specified config item as a boolean, defaultValue if it doesn't exist.
     */    
    public static synchronized boolean getBoolean(String name, boolean defaultValue) {
        initConfig();
        String value = getString(name, null);
        return value == null ? defaultValue :
            (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") ||
            value.equals("1"));
    }
    
    /**
     * Returns a File object representing the path relative to the
     * Zimbra home directory.
     * @param path
     * @return
     */
    public static File getPathRelativeToZimbraHome(String path) {
        char first = path.charAt(0);
        if (first == File.separatorChar || first == '/')
            return new File(path);

    	String home = LC.zimbra_home.value();
    	return new File(home, path);
    }


    private static boolean sUserServicesEnabled;

    private static final Object sUserServicesEnabledGuard = new Object();

    /**
     * Enable/disable end-user services on SOAP and LMTP interfaces.
     * @param enabled
     * @throws ServiceException
     */
    public static void enableUserServices(boolean enabled) throws ServiceException {
        Server serverConfig = Provisioning.getInstance().getLocalServer();
        serverConfig.setBooleanAttr(Provisioning.A_zimbraUserServicesEnabled, enabled);
    	synchronized (sUserServicesEnabledGuard) {
    		sUserServicesEnabled = enabled;
        }
    }

    /**
     * Returns whether end-user services on SOAP and LMTP interfaces
     * are enabled.
     * @return
     */
    public static boolean userServicesEnabled() {
        initConfig();
    	synchronized (sUserServicesEnabledGuard) {
    		return sUserServicesEnabled;
        }
    }
}
