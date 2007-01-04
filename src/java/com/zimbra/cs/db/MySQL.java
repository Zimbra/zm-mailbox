/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.util.Properties;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class MySQL extends Db {

    @Override
    void setErrorConstants() {
        Error.DUPLICATE_ROW = 1062;
        Error.DEADLOCK_DETECTED = 1213;
        Error.FOREIGN_KEY_NO_PARENT = 1216;
        Error.FOREIGN_KEY_CHILD_EXISTS = 1217;
        Error.NO_SUCH_TABLE = 1146;
    }

    @Override
    void setCapabilities() {
        Capability.LIMIT_CLAUSE = true;
        Capability.BOOLEAN_DATATYPE = true;
        Capability.ON_DUPLICATE_KEY = true;
        Capability.ON_UPDATE_CASCADE = true;
        Capability.MULTITABLE_UPDATE = true;
        Capability.BITWISE_OPERATIONS = true;
        Capability.DISABLE_CONSTRAINT_CHECK = true;
    }

    @Override
    DbPool.PoolConfig getPoolConfig() {
        return new MySQLConfig();
    }

    static final class MySQLConfig extends DbPool.PoolConfig {
        MySQLConfig() {
            mDriverClassName = "com.mysql.jdbc.Driver";
            mPoolSize = 100;
            mRootUrl = "jdbc:mysql://" + LC.mysql_bind_address.value() + ":" + LC.mysql_port.value() + "/";
            mConnectionUrl = mRootUrl + "zimbra";
            mLoggerUrl = "jdbc:mysql://" + LC.logger_mysql_bind_address.value() + ":" + LC.logger_mysql_port.value() + "/";
            mSupportsStatsCallback = true;
            mDatabaseProperties = getMySQLProperties();

            // override pool size if specified in prefs
            String maxActive = (String) mDatabaseProperties.get("maxActive");
            if (maxActive != null) {
                try {
                    mPoolSize = Integer.parseInt(maxActive);
                } catch (NumberFormatException nfe) {
                    ZimbraLog.system.warn("exception parsing 'maxActive' pref; defaulting pool size to " + mPoolSize, nfe);
                }
            }
            ZimbraLog.misc.debug("Setting connection pool size to " + mPoolSize);
        }

        private static Properties getMySQLProperties() {
            Properties props = new Properties();

            props.put("cacheResultSetMetadata", "true");
            props.put("cachePrepStmts", "true");
            // props.put("cacheCallableStmts", "true");
            props.put("prepStmtCacheSize", "25");        
            // props.put("prepStmtCacheSqlLmiit", "256");
            props.put("autoReconnect", "true");
            props.put("useUnicode", "true");
            props.put("characterEncoding", "UTF-8");
            props.put("dumpQueriesOnException", "true");

            // props.put("connectTimeout", "0");    // connect timeout in msecs
            // props.put("initialTimeout", "2");    // time to wait between re-connects
            // props.put("maxReconnects", "3"");    // max number of reconnects to attempt

            // Set/override MySQL Connector/J connection properties from localconfig.
            // Localconfig keys with "zimbra_mysql_connector_" prefix are used.
            final String prefix = "zimbra_mysql_connector_";
            for (String key : LC.getAllKeys()) {
                if (!key.startsWith(prefix))
                    continue;
                String prop = key.substring(prefix.length());
                if (prop.length() > 0 && !prop.equalsIgnoreCase("logger")) {
                    props.put(prop, LC.get(key));
                    ZimbraLog.system.info("Setting mysql connector property: " + prop + "=" + LC.get(key));
                }
            }

            // These properties cannot be set with "zimbra_mysql_connector_" keys.
            props.put("user", LC.zimbra_mysql_user.value());
            props.put("password", LC.zimbra_mysql_password.value());

            return props;
        }
    }
}
