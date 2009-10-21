/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.dbcp.DelegatingConnection;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbPool.PoolConfig;

public class SQLite extends Db {

    private Map<Db.Error, String> mErrorCodes;
    private String cacheSize;

    SQLite() {
        mErrorCodes = new HashMap<Db.Error, String>(6);
        mErrorCodes.put(Db.Error.DUPLICATE_ROW, "column id is not unique");
        mErrorCodes.put(Db.Error.NO_SUCH_TABLE, "no such table");
    }
    
    @Override boolean supportsCapability(Db.Capability capability) {
        switch (capability) {
            case AVOID_OR_IN_WHERE_CLAUSE:   return false;
            case BITWISE_OPERATIONS:         return true;
            case BOOLEAN_DATATYPE:           return true;
            case BROKEN_IN_CLAUSE:           return false;
            case CASE_SENSITIVE_COMPARISON:  return true;
            case CAST_AS_BIGINT:             return false;
            case CLOB_COMPARISON:            return true;
            case DISABLE_CONSTRAINT_CHECK:   return false;
            case FILE_PER_DATABASE:          return true;
            case FORCE_INDEX_EVEN_IF_NO_SORT:  return false;
            case LIMIT_CLAUSE:               return true;
            case MULTITABLE_UPDATE:          return false;
            case ON_DUPLICATE_KEY:           return false;
            case ON_UPDATE_CASCADE:          return false;
            case READ_COMMITTED_ISOLATION:   return false;
            case REPLACE_INTO:               return true;
            case REQUEST_UTF8_UNICODE_COLLATION:  return false;
            case ROW_LEVEL_LOCKING:          return false;
            case UNIQUE_NAME_INDEX:          return false;
        }
        return false;
    }

    @Override boolean compareError(SQLException e, Error error) {
        // XXX: the SQLite JDBC driver doesn't yet expose SQLite error codes, which sucks
        String code = mErrorCodes.get(error);
        return code != null && e.getMessage().contains(code);
    }

    @Override String forceIndexClause(String index) {
        // don't think we can direct the sqlite optimizer...
        return "";
    }

    @Override String getIFNULLClause(String expr1, String expr2) {
        return "IFNULL(" + expr1 + ", " + expr2 + ")";
    }

    @Override PoolConfig getPoolConfig() {
        return new SQLiteConfig();
    }


    @Override void startup(org.apache.commons.dbcp.PoolingDataSource pool, int poolSize) throws SQLException {
        
        cacheSize = LC.get("sqlite_cache_size");
        ZimbraLog.dbconn.info("sqlite driver running with " + cacheSize + " page cache");
        super.startup(pool, poolSize);
    }

    void postCreate(java.sql.Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(true);
            if (cacheSize != null)
                pragma(conn, "cache_size", cacheSize);
            pragma(conn, "synchronous", "NORMAL");
        } finally {
            conn.setAutoCommit(false);
        }
    }

    private void pragma(java.sql.Connection conn, String key, String value) throws SQLException {
        PreparedStatement stmt = null;
        try {
            (stmt = conn.prepareStatement("PRAGMA " + key + " = " + value)).execute();
        } finally {
            DbPool.quietCloseStatement(stmt);
        }
    }

    private static final int DEFAULT_CONNECTION_POOL_SIZE = 12;

    private static final int MAX_ATTACHED_DATABASES = readConfigInt("sqlite_max_attached_databases", "max # of attached databases", 7);

    private static final HashMap<java.sql.Connection, LinkedHashMap<String, String>> sAttachedDatabases =
            new HashMap<java.sql.Connection, LinkedHashMap<String, String>>(DEFAULT_CONNECTION_POOL_SIZE);

    private LinkedHashMap<String, String> getAttachedDatabases(Connection conn) {
        return sAttachedDatabases.get(getInnermostConnection(conn.getConnection()));
    }

    private java.sql.Connection getInnermostConnection(java.sql.Connection conn) {
        java.sql.Connection retVal = null;
        if (conn instanceof DebugConnection)
            retVal = ((DebugConnection) conn).getConnection();
        if (conn instanceof DelegatingConnection)
            retVal = ((DelegatingConnection) conn).getInnermostDelegate();
        return retVal == null ? conn : retVal;
    }

    @Override public void registerDatabaseInterest(Connection conn, String dbname) throws SQLException, ServiceException {
        LinkedHashMap<String, String> attachedDBs = getAttachedDatabases(conn);
        if (attachedDBs != null && attachedDBs.containsKey(dbname))
            return;

        // if we're using more databases than we're allowed to, detach the least recently used
        if (attachedDBs != null && attachedDBs.size() >= MAX_ATTACHED_DATABASES) {
            for (Iterator<String> it = attachedDBs.keySet().iterator(); attachedDBs.size() >= MAX_ATTACHED_DATABASES && it.hasNext(); ) {
                String name = it.next();
                
                if (!name.equals("zimbra") && detachDatabase(conn, name))
                    it.remove();
            }
        }
        attachDatabase(conn, dbname);
    }

    void attachDatabase(Connection conn, String dbname) throws SQLException, ServiceException {
        PreparedStatement stmt = null;
        
        try {
            boolean autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit)
                conn.getConnection().setAutoCommit(true);

            (stmt = conn.prepareStatement("ATTACH DATABASE \"" + getDatabaseFilename(dbname) + "\" AS " + dbname)).execute();

            if (!autocommit)
                conn.getConnection().setAutoCommit(autocommit);
        } catch (SQLException e) {
            if (!"database is already attached".equals(e.getMessage()))
                return;
        } finally {
            DbPool.quietCloseStatement(stmt);
        }
        
        LinkedHashMap<String, String> attachedDBs = getAttachedDatabases(conn);
        if (attachedDBs != null) {
            attachedDBs.put(dbname, null);
        } else {
            attachedDBs = new LinkedHashMap<String, String>(MAX_ATTACHED_DATABASES * 3 / 2, (float) 0.75, true);
            attachedDBs.put(dbname, null);
            sAttachedDatabases.put(getInnermostConnection(conn.getConnection()), attachedDBs);
        }
    }

    private boolean detachDatabase(Connection conn, String dbname) {
        PreparedStatement stmt = null;
        try {
            boolean autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit)
                conn.getConnection().setAutoCommit(true);

            (stmt = conn.prepareStatement("DETACH DATABASE " + dbname)).execute();

            if (!autocommit)
                conn.getConnection().setAutoCommit(autocommit);
            return true;
        } catch (SQLException e) {
            ZimbraLog.dbconn.warn("database overflow autoclose failed for DB " + dbname, e);
            return false;
        } finally {
            DbPool.quietCloseStatement(stmt);
        }
    }

//    @Override void preClose(Connection conn) {
//        LinkedHashMap<String, String> attachedDBs = getAttachedDatabases(conn);
//        if (attachedDBs == null)
//            return;
//
//        // simplest solution it to just detach all the active databases every time we close the connection
//        for (Iterator<String> it = attachedDBs.keySet().iterator(); it.hasNext(); ) {
//            if (detachDatabase(conn, it.next()))
//                it.remove();
//        }
//    }

    @Override public boolean databaseExists(Connection conn, String dbname) throws ServiceException {
        if (!new File(getDatabaseFilename(dbname)).exists())
            return false;

        // since it's so easy to end up with an empty SQLite database, make
        // sure that at least one table exists 
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit)
                conn.getConnection().setAutoCommit(true);

            registerDatabaseInterest(conn, dbname);
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " +
                (dbname.equals("zimbra") ? "" : dbname + ".") +
                "sqlite_master WHERE type='table'");
            rs = stmt.executeQuery();
            boolean complete = rs.next() ? (rs.getInt(1) >= 1) : false;

            if (!autocommit)
                conn.getConnection().setAutoCommit(autocommit);
            return complete;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("sqlite error", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    @Override void deleteDatabaseFile(String dbname) {
        assert(dbname != null && !dbname.trim().equals(""));
        ZimbraLog.dbconn.info("deleting database file for DB '" + dbname + "'");
        new File(getDatabaseFilename(dbname)).delete();
    }


    public String getDatabaseFilename(String dbname) {
        return LC.zimbra_home.value() + File.separator + "sqlite" + File.separator + dbname + ".db";
    }

    final class SQLiteConfig extends DbPool.PoolConfig {
        SQLiteConfig() {
            mDriverClassName = "org.sqlite.JDBC";
            mPoolSize = DEFAULT_CONNECTION_POOL_SIZE;
            mRootUrl = null;
            mConnectionUrl = "jdbc:sqlite:" + getDatabaseFilename("zimbra"); 
            mLoggerUrl = null;
            mSupportsStatsCallback = false;
            mDatabaseProperties = getSQLiteProperties();

            // override pool size if specified in prefs
            mPoolSize = readConfigInt("sqlite_pool_size", "connection pool size", DEFAULT_CONNECTION_POOL_SIZE);
        }

        private Properties getSQLiteProperties() {
            Properties props = new Properties();
            props.setProperty("shared_cache", "true");
            return props;
        }
    }

    static int readConfigInt(final String keyname, final String description, final int defaultvalue) {
        int value = defaultvalue;
        try {
            String configvalue = LC.get(keyname);
            if (configvalue != null && !configvalue.trim().equals(""))
                value = Math.max(1, Integer.parseInt(configvalue));
        } catch (NumberFormatException nfe) {
            ZimbraLog.dbconn.warn("exception parsing '" + keyname  + "' config; defaulting limit to " + defaultvalue, nfe);
        }
        ZimbraLog.dbconn.info("setting " + description + " to " + value);
        return value;
    }


    @Override public void flushToDisk() {
        // not really implemented
    }

    @Override public String toString() {
        return "SQLite";
    }

    public static void main(String args[]) {
        // command line argument parsing
        Options options = new Options();
        CommandLine cl = Versions.parseCmdlineArgs(args, options);

        String outputDir = cl.getOptionValue("o");
        File outFile = new File(outputDir, "versions-init.sql");
        outFile.delete();

        try {
            String redoVer = com.zimbra.cs.redolog.Version.latest().toString();
            String outStr = "-- AUTO-GENERATED .SQL FILE - Generated by the SQLite versions tool\n" +
                "INSERT INTO config(name, value, description) VALUES\n" +
                "\t('db.version', '" + Versions.DB_VERSION + "', 'db schema version');\n" + 
                "INSERT INTO config(name, value, description) VALUES\n" +
                "\t('index.version', '" + Versions.INDEX_VERSION + "', 'index version');\n" +
                "INSERT INTO config(name, value, description) VALUES\n" +
                "\t('redolog.version', '" + redoVer + "', 'redolog version');\n";

            Writer output = new BufferedWriter(new FileWriter(outFile));
            output.write(outStr);
            output.close();
        } catch (IOException e){
            System.out.println("ERROR - caught exception at\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
