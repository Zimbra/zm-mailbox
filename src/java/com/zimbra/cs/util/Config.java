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
import com.zimbra.cs.service.ServiceException;


/**
 * @author schemers
 */
public class Config {

	public static final String C_COMMON_LIQUID_HOME = "common.liquidHome";
	public static final String D_COMMON_LIQUID_HOME = "/opt/liquid";

    public static final String C_STORE_COMPRESS_BLOBS = "store.compressBlobs";
    public static final boolean D_STORE_COMPRESS_BLOBS = false;

    public static final int D_LMTP_THREADS = 10;
    public static final int D_LMTP_BIND_PORT = 7025;
    public static final String D_LMTP_BIND_ADDRESS = null;
    public static final String D_LMTP_ANNOUNCE_NAME = null;

    public static final int D_SMTP_TIMEOUT = 60;
    public static final int D_SMTP_PORT = 25;

    // update ever 7 days by default
    public static final long D_LIQUID_LAST_LOGON_TIMESTAMP_FREQUENCY = 1000*60*60*24*7;
    
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
            serverConfig.getBooleanAttr(Provisioning.A_liquidUserServicesEnabled, true);
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
     * @param name
     * @return specified config item as a long, -1 if it doesn't exist.
     */
    public static synchronized long getLong(String name, long defaultValue) {
        initConfig();
        String value = getString(name, null);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    /**
     * Returns a File object representing the path relative to the
     * Liquid home directory.
     * @param path
     * @return
     */
    public static File getPathRelativeToLiquidHome(String path) {
        char first = path.charAt(0);
        if (first == File.separatorChar || first == '/')
            return new File(path);

    	String home = Config.getString(C_COMMON_LIQUID_HOME, D_COMMON_LIQUID_HOME);
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
        serverConfig.setBooleanAttr(Provisioning.A_liquidUserServicesEnabled, enabled);
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
    	synchronized (sUserServicesEnabledGuard) {
    		return sUserServicesEnabled;
        }
    }
}
