/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbPool.PoolConfig;

public class SQLite extends Db {

    @Override boolean supportsCapability(Db.Capability capability) {
        switch (capability) {
            case BITWISE_OPERATIONS:         return true;
            case BOOLEAN_DATATYPE:           return true;
            case BROKEN_IN_CLAUSE:           return false;
            case CASE_SENSITIVE_COMPARISON:  return true;
            case CAST_AS_BIGINT:             return false;
            case CLOB_COMPARISON:            return true;
            case DISABLE_CONSTRAINT_CHECK:   return false;
            case FILE_PER_DATABASE:          return true;
            case LIMIT_CLAUSE:               return true;
            case MULTITABLE_UPDATE:          return false;
            case ON_DUPLICATE_KEY:           return false;
            case ON_UPDATE_CASCADE:          return false;
            case READ_COMMITTED_ISOLATION:   return false;
            case REPLACE_INTO:               return true;
            case UNIQUE_NAME_INDEX:          return false;
        }
        return false;
    }

    @Override boolean compareError(SQLException e, Error error) {
        // XXX: the SQLite JDBC driver doesn't yet expose SQLite error codes, which sucks
        return false;
    }

    @Override String forceIndexClause(String index) {
        // don't think we can direct the sqlite optimizer...
        return "";
    }

    @Override public String scriptCommandDelimiter() {
        return "%";
    }

    @Override String getIFNULLClause(String expr1, String expr2) {
        return "IFNULL(" + expr1 + ", " + expr2 + ")";
    }

    @Override PoolConfig getPoolConfig() {
        return new SQLiteConfig();
    }


//    @Override void startup(PoolingDataSource pool, int poolSize) throws SQLException {
//        int groupCount = DebugConfig.numMailboxGroups;
//
//        LinkedList<java.sql.Connection> connections = new LinkedList<java.sql.Connection>();
//        for (int i = 0; i < poolSize; i++) {
//            java.sql.Connection conn = pool.getConnection();
//            conn.setAutoCommit(true);
//            for (int group = 1; group <= groupCount; group++) {
//                String dbpath = SQLiteConfig.dbdir + File.separator + "mboxgroup" + group + ".db";
//                java.sql.PreparedStatement stmt = conn.prepareStatement("ATTACH DATABASE \"" + dbpath + "\" AS mboxgroup" + group);
//                stmt.execute();
//            }
//            conn.setAutoCommit(false);
//            connections.add(conn);
//        }
//        for (java.sql.Connection conn : connections)
//            conn.close();
//
//        super.startup(pool, poolSize);
//    }

    private static final int CONNECTION_POOL_SIZE = 12;

    private static final int MAX_ATTACHED_DATABASES = 7;

    private static final HashMap<java.sql.Connection, LinkedHashMap<String, String>> sAttachedDatabases =
            new HashMap<java.sql.Connection, LinkedHashMap<String, String>>(CONNECTION_POOL_SIZE);

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, String> getAttachedDatabases(Connection conn) {
        return sAttachedDatabases.get(conn.getConnection());
    }

    private void recordAttachedDatabase(Connection conn, String dbname) {
        LinkedHashMap<String, String> attachedDBs = getAttachedDatabases(conn);
        if (attachedDBs != null) {
            attachedDBs.put(dbname, null);
        } else {
            attachedDBs = new LinkedHashMap<String, String>(MAX_ATTACHED_DATABASES * 3 / 2, (float) 0.75, true);
            attachedDBs.put(dbname, null);
            sAttachedDatabases.put(conn.getConnection(), attachedDBs);
        }
    }

    @Override void registerDatabaseInterest(Connection conn, String dbname) throws SQLException {
        LinkedHashMap<String, String> attachedDBs = getAttachedDatabases(conn);
        if (attachedDBs != null && attachedDBs.containsKey(dbname))
            return;

        // if we're using more databases than we're allowed to, detach the least recently used
        if (attachedDBs != null && attachedDBs.size() >= MAX_ATTACHED_DATABASES) {
            for (Iterator<String> it = attachedDBs.keySet().iterator(); attachedDBs.size() >= MAX_ATTACHED_DATABASES && it.hasNext(); ) {
                if (detachDatabase(conn, it.next()))
                    it.remove();
            }
        }

        attachDatabase(conn, dbname);
        recordAttachedDatabase(conn, dbname);
    }

    private void attachDatabase(Connection conn, String dbname) throws SQLException {
        PreparedStatement stmt = null;
        try {
            boolean autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit)
                conn.getConnection().setAutoCommit(true);

            (stmt = conn.prepareStatement("ATTACH DATABASE \"" + getDatabaseFilename(dbname) + "\" AS " + dbname)).execute();

            if (!autocommit)
                conn.getConnection().setAutoCommit(autocommit);
        } finally {
            DbPool.quietCloseStatement(stmt);
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
            ZimbraLog.sqltrace.warn("database overflow autoclose failed for DB " + dbname, e);
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

        // XXX: since it's so easy to end up with an empty SQLite database, make sure that the tables we want are actually in there
        //   (yes, this assumes that we're looking for a MBOXGROUP database, which is beyond the scope of this method's contract)
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit)
                conn.getConnection().setAutoCommit(true);

            registerDatabaseInterest(conn, dbname);

            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + dbname + ".sqlite_master WHERE type='table'");
            rs = stmt.executeQuery();
            boolean complete = rs.next() ? (rs.getInt(1) >= DbMailbox.sTables.length) : false;

            if (!autocommit)
                conn.getConnection().setAutoCommit(autocommit);
            return complete;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("foo", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    @Override void deleteDatabaseFile(String dbname) {
        assert(dbname != null && !dbname.trim().equals(""));
        ZimbraLog.sqltrace.info("deleting database file for DB '" + dbname + "'");
        new File(getDatabaseFilename(dbname)).delete();
    }


    private static final String DATABASE_DIRECTORY = System.getProperty("derby.system.home", LC.zimbra_home.value() + File.separator + "sqlite");

    static String getDatabaseFilename(String dbname) {
        return DATABASE_DIRECTORY + File.separator + dbname + ".db";
    }

    static final class SQLiteConfig extends DbPool.PoolConfig {
        SQLiteConfig() {
            mDriverClassName = "org.sqlite.JDBC";
            mPoolSize = CONNECTION_POOL_SIZE;
            mRootUrl = null;
            mConnectionUrl = "jdbc:sqlite:" + getDatabaseFilename("zimbra"); 
            mLoggerUrl = null;
            mSupportsStatsCallback = false;
            mDatabaseProperties = getSQLiteProperties();

            ZimbraLog.misc.debug("Setting connection pool size to " + mPoolSize);
        }

        private static Properties getSQLiteProperties() {
            Properties props = new Properties();
            props.setProperty("shared_cache", "true");
            return props;
        }
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
            if (output != null)
                output.close();
        } catch (IOException e){
            System.out.println("ERROR - caught exception at\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
