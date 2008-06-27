/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;

public class MySQL extends Db {

    private Map<Db.Error, Integer> mErrorCodes;

    MySQL() {
        mErrorCodes = new HashMap<Db.Error, Integer>(6);
        mErrorCodes.put(Db.Error.DEADLOCK_DETECTED,        1213);
        mErrorCodes.put(Db.Error.DUPLICATE_ROW,            1062);
        mErrorCodes.put(Db.Error.FOREIGN_KEY_NO_PARENT,    1216);
        mErrorCodes.put(Db.Error.FOREIGN_KEY_CHILD_EXISTS, 1217);
        mErrorCodes.put(Db.Error.NO_SUCH_DATABASE,         1146);
        mErrorCodes.put(Db.Error.NO_SUCH_TABLE,            1146);
    }

    @Override
    boolean supportsCapability(Db.Capability capability) {
        switch (capability) {
            case BITWISE_OPERATIONS:         return true;
            case BOOLEAN_DATATYPE:           return true;
            case BROKEN_IN_CLAUSE:           return false;
            case CASE_SENSITIVE_COMPARISON:  return false;
            case CAST_AS_BIGINT:             return false;
            case CLOB_COMPARISON:            return true;
            case DISABLE_CONSTRAINT_CHECK:   return true;
            case LIMIT_CLAUSE:               return true;
            case MULTITABLE_UPDATE:          return true;
            case ON_DUPLICATE_KEY:           return true;
            case ON_UPDATE_CASCADE:          return true;
            case UNIQUE_NAME_INDEX:          return true;
        }
        return false;
    }

    @Override
    boolean compareError(SQLException e, Db.Error error) {
        Integer code = mErrorCodes.get(error);
        return (code != null && e.getErrorCode() == code);
    }

    @Override
    String forceIndexClause(String index) {
        return " FORCE INDEX (" + index + ')';
    }

    @Override String getIFNULL(String column1, String column2) {
        return "IFNULL(" + column1 + ", " + column2 + ")";
    }

    @Override
    DbPool.PoolConfig getPoolConfig() {
        return new MySQLConfig();
    }
    
    @Override
    public boolean databaseExists(Connection conn, String databaseName)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int numSchemas = 0;
        
        try {
            stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA " +
                "WHERE schema_name = ?");
            stmt.setString(1, databaseName);
            rs = stmt.executeQuery();
            rs.next();
            numSchemas = rs.getInt(1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to determine whether database exists", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return (numSchemas > 0);
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
