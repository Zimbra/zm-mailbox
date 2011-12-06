/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbPool.PoolConfig;

public final class SQLite extends Db {

    private static final String PRAGMA_JOURNAL_MODE_DEFAULT = "DELETE";
    private static final String PRAGMA_SYNCHRONOUS_DEFAULT  = "FULL";

    private Map<Db.Error, String> mErrorCodes;
    private String cacheSize;
    private String journalMode;
    private String pageSize;
    private String syncMode;

    SQLite() {
        mErrorCodes = new HashMap<Db.Error, String>(6);
        mErrorCodes.put(Db.Error.DUPLICATE_ROW, "not unique");
        mErrorCodes.put(Db.Error.NO_SUCH_TABLE, "no such table");
        mErrorCodes.put(Db.Error.FOREIGN_KEY_CHILD_EXISTS, "foreign key");
        mErrorCodes.put(Db.Error.FOREIGN_KEY_NO_PARENT, "foreign key");
        mErrorCodes.put(Db.Error.TOO_MANY_SQL_PARAMS, "too many SQL variables");
        mErrorCodes.put(Db.Error.BUSY, "SQLITE_BUSY");
        mErrorCodes.put(Db.Error.LOCKED, "database is locked");
        mErrorCodes.put(Db.Error.CANTOPEN, "SQLITE_CANTOPEN");
    }

    @Override
    boolean supportsCapability(Db.Capability capability) {
        switch (capability) {
            case AVOID_OR_IN_WHERE_CLAUSE:   return false;
            case BITWISE_OPERATIONS:         return true;
            case BOOLEAN_DATATYPE:           return false;
            case CASE_SENSITIVE_COMPARISON:  return true;
            case CAST_AS_BIGINT:             return false;
            case CLOB_COMPARISON:            return true;
            case DISABLE_CONSTRAINT_CHECK:   return false;
            case FILE_PER_DATABASE:          return true;
            case FORCE_INDEX_EVEN_IF_NO_SORT:  return false;
            case LIMIT_CLAUSE:               return true;
            case MULTITABLE_UPDATE:          return false;
            case NON_BMP_CHARACTERS:         return true;
            case ON_DUPLICATE_KEY:           return false;
            case ON_UPDATE_CASCADE:          return true;
            case READ_COMMITTED_ISOLATION:   return false;
            case REPLACE_INTO:               return true;
            case REQUEST_UTF8_UNICODE_COLLATION:  return false;
            case ROW_LEVEL_LOCKING:          return false;
            case UNIQUE_NAME_INDEX:          return false;
            case SQL_PARAM_LIMIT:            return true;
        }
        return false;
    }

    @Override
    boolean compareError(SQLException e, Error error) {
        // XXX: the SQLite JDBC driver doesn't yet expose SQLite error codes, which sucks
        String code = mErrorCodes.get(error);
        return code != null && e.getMessage() != null && e.getMessage().contains(code);
    }

    @Override
    String forceIndexClause(String index) {
        // don't think we can direct the sqlite optimizer...
        return "";
    }

    @Override
    String getIFNULLClause(String expr1, String expr2) {
        return "IFNULL(" + expr1 + ", " + expr2 + ")";
    }

    @Override
    public String bitAND(String expr1, String expr2) {
        return expr1 + " & " + expr2;
    }

    @Override
    PoolConfig getPoolConfig() {
        return new SQLiteConfig();
    }


    @Override
    void startup(org.apache.commons.dbcp.PoolingDataSource pool, int poolSize) throws SQLException {
        cacheSize = LC.sqlite_cache_size.value();
        if (cacheSize.equals("0"))
            cacheSize = null;
        journalMode = LC.sqlite_journal_mode.value();
        pageSize = LC.sqlite_page_size.value();
        if (pageSize.equals("0"))
            pageSize = null;
        syncMode = LC.sqlite_sync_mode.value();
        ZimbraLog.dbconn.info("sqlite driver running with " +
            (cacheSize == null ? "default" : cacheSize) + " cache cache, " +
            (pageSize == null ? "default" : pageSize) + " page size, " +
            journalMode + " journal mode, " + syncMode + " sync mode");
        super.startup(pool, poolSize);
    }

    @Override
    void postCreate(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(true);
            pragmas(conn, null);
        } finally {
            conn.setAutoCommit(false);
        }
    }

    private void pragma(Connection conn, String dbname, String key, String value) throws SQLException {
        PreparedStatement stmt = null;

        try {
            String prefix = dbname == null || dbname.equals("zimbra") ? "" : dbname + ".";
            (stmt = conn.prepareStatement("PRAGMA " + prefix + key +
                (value == null ? "" : " = " + value))).execute();
        } finally {
            DbPool.quietCloseStatement(stmt);
        }
    }

    void pragmas(Connection conn, String dbname) throws SQLException {
        /*
         * auto_vacuum causes databases to be locked permanently
         * pragma(conn, dbname, "auto_vacuum", "2");
         */
        pragma(conn, dbname, "foreign_keys", "ON");
        if (journalMode != null && !journalMode.equalsIgnoreCase(PRAGMA_JOURNAL_MODE_DEFAULT))
            pragma(conn, dbname, "journal_mode", journalMode);
        if (syncMode != null && !syncMode.equalsIgnoreCase(PRAGMA_SYNCHRONOUS_DEFAULT))
            pragma(conn, dbname, "synchronous", syncMode);
        if (cacheSize != null)
            pragma(conn, dbname, "cache_size", cacheSize);
        if (pageSize != null)
            pragma(conn, dbname, "page_size", pageSize);
    }

    private static final int DEFAULT_CONNECTION_POOL_SIZE = 6;

    private static final int MAX_ATTACHED_DATABASES = readConfigInt("sqlite_max_attached_databases", "max # of attached databases", 7);

    private static final HashMap<Connection, LinkedHashMap<String, String>> sAttachedDatabases =
            new HashMap<Connection, LinkedHashMap<String, String>>(DEFAULT_CONNECTION_POOL_SIZE);

    private LinkedHashMap<String, String> getAttachedDatabases(DbConnection conn) {
        return sAttachedDatabases.get(getInnermostConnection(conn.getConnection()));
    }

    private Connection getInnermostConnection(Connection conn) {
        Connection retVal = null;
        if (conn instanceof DebugConnection)
            retVal = ((DebugConnection) conn).getConnection();
        if (conn instanceof DelegatingConnection)
            retVal = ((DelegatingConnection) conn).getInnermostDelegate();
        return retVal == null ? conn : retVal;
    }

    @Override
    public void optimize(DbConnection conn, String dbname, int level)
        throws ServiceException {
        try {
            boolean autocommit = conn.getConnection().getAutoCommit();
            PreparedStatement stmt = null;

            try {
                if (!autocommit)
                    conn.getConnection().setAutoCommit(true);
                if (dbname == null)
                    dbname = "zimbra";
                registerDatabaseInterest(conn, dbname);
                if (level > 0 && dbname.endsWith("zimbra")) {
                    if (level == 2)
                        (stmt = conn.prepareStatement("VACUUM")).execute();
                    else
                        pragma(conn.getConnection(), dbname, "incremental_vacuum", null);
                }
                (stmt = conn.prepareStatement("ANALYZE " + dbname)).execute();
                ZimbraLog.dbconn.debug("sqlite " +
                    (level > 0 ? "vacuum" : "analyze") + ' ' + dbname);
            } finally {
                if (!autocommit) {
                    try {
                        conn.getConnection().setAutoCommit(autocommit);
                    } catch (SQLException sqle) {
                        ZimbraLog.dbconn.warn("failed to reset autocommit to false. probably caused by prior errors %s", dbname);
                        DbPool.quietClose(conn);
                        throw ServiceException.FAILURE("failed to reset autocommit to false",sqle);
                    }
                }

                DbPool.quietCloseStatement(stmt);
            }
        } catch (Exception e) {
            throw ServiceException.FAILURE("sqlite " +
                (level > 0 ? "vacuum" : "analyze") + ' ' + dbname + " error", e);
        }
    }

    @Override
    public void registerDatabaseInterest(DbConnection conn, String dbname) throws SQLException, ServiceException {
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

    void attachDatabase(DbConnection conn, String dbname) throws SQLException, ServiceException {
        PreparedStatement stmt = null;
        boolean autocommit = true;
        try {
            autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit)
                conn.getConnection().setAutoCommit(true);

            (stmt = conn.prepareStatement("ATTACH DATABASE \"" + getDatabaseFilename(dbname) + "\" AS " + dbname)).execute();
            pragmas(conn.getConnection(), dbname);
        } catch (SQLException e) {
            ZimbraLog.dbconn.error("database " + dbname + " attach failed", e);
            if (!"database is already attached".equals(e.getMessage()))
                throw e;
        } finally {
            if (!autocommit) {
                try {
                    conn.getConnection().setAutoCommit(autocommit);
                } catch (SQLException sqle) {
                    ZimbraLog.dbconn.warn("failed to reset autocommit to false. probably caused by prior errors " + dbname);
                    DbPool.quietClose(conn);
                    throw ServiceException.FAILURE("failed to reset autocommit to false",sqle);
                }
            }
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

    private boolean detachDatabase(DbConnection conn, String dbname) throws ServiceException {
        PreparedStatement stmt = null;
        boolean autocommit = true;
        try {
            autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit) {
                conn.getConnection().setAutoCommit(true);
            }
            (stmt = conn.prepareStatement("DETACH DATABASE " + dbname)).execute();
            return true;
        } catch (SQLException e) {
            if (!deleted.containsKey(dbname)) { 
                ZimbraLog.dbconn.warn("database overflow autoclose failed for DB " + dbname, e);
                return false;
            } else {
                return true;
            }
        } finally {
            if (!autocommit) {
                try {
                    conn.getConnection().setAutoCommit(autocommit);
                } catch (SQLException sqle) {
                    ZimbraLog.dbconn.warn("failed to reset autocommit to false. probably caused by prior errors %s", dbname);
                    DbPool.quietClose(conn);
                    throw ServiceException.FAILURE("failed to reset autocommit to false",sqle);
                }
            }
            DbPool.quietCloseStatement(stmt);
        }
    }

    private void releaseMboxDbLock(Integer mboxId) {
        if (mboxId != null) {
            ReentrantLock lock = null;
            lock = lockMap.get(mboxId);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                ZimbraLog.dbconn.trace("unlocked mbox %d",mboxId);
            }
        }
    }

    @Override
    void preClose(DbConnection conn) {
        releaseMboxDbLock(conn.mboxId);
    }


    private static ConcurrentMap<Integer, ReentrantLock> lockMap = new ConcurrentHashMap<Integer, ReentrantLock>();

    private boolean checkLockMap(int mboxId) {
        for (Entry<Integer, ReentrantLock> entry : lockMap.entrySet()) {
            if (entry.getKey().intValue() != mboxId && entry.getValue().isHeldByCurrentThread()) {
                ZimbraLog.dbconn.debug("already holding db lock for mbox %d",entry.getKey());
                if (entry.getKey().intValue() != -1) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    void preOpen(Integer mboxId) {
        ZimbraLog.dbconn.trace("trying to lock mbox %d",mboxId);
        assert(checkLockMap(mboxId));
        ReentrantLock lock = lockMap.get(mboxId);
        if (lock == null) {
            lock = new ReentrantLock();
            ReentrantLock added = lockMap.putIfAbsent(mboxId, lock);
            if (added != null) {
                lock = added;
            }
        }
        boolean locked = false;
        long timeoutSecs = 180;
        //lock with timeout in case external call sites cause a deadlock
        //(e.g. one site locks some object before opening connection; another incorrectly locks same object after opening connection)
        //in case of timeout we'll fall through and let sqlite_busy retry handler sort it out
        try {
            locked = lock.tryLock(timeoutSecs, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!locked) {
            ZimbraLog.dbconn.warn("Unable to get db lock for mbox %d",mboxId);
        } else {
            ZimbraLog.dbconn.trace("locked mbox %d",mboxId);
        }
    }

    @Override
    void abortOpen(Integer mboxId) {
        releaseMboxDbLock(mboxId);
    }

    @Override
    public boolean databaseExists(DbConnection conn, String dbname) throws ServiceException {
        if (!new File(getDatabaseFilename(dbname)).exists())
            return false;

        // since it's so easy to end up with an empty SQLite database, make
        // sure that at least one table exists
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean autocommit = true;
        try {
            autocommit = conn.getConnection().getAutoCommit();
            if (!autocommit)
                conn.getConnection().setAutoCommit(true);

            registerDatabaseInterest(conn, dbname);
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " +
                (dbname.equals("zimbra") ? "" : dbname + ".") +
                "sqlite_master WHERE type='table'");
            rs = stmt.executeQuery();
            boolean complete = rs.next() ? (rs.getInt(1) >= 1) : false;
            return complete;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("sqlite error", e);
        } finally {
            if (!autocommit) {
                try {
                    conn.getConnection().setAutoCommit(autocommit);
                } catch (SQLException sqle) {
                    ZimbraLog.dbconn.warn("failed to reset autocommit to false. probably caused by prior errors %s", dbname);
                    DbPool.quietClose(conn);
                    throw ServiceException.FAILURE("failed to reset autocommit to false",sqle);
                }
            }
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private ConcurrentMap<String,Boolean> deleted = new ConcurrentHashMap<String, Boolean>();

    @Override
    void deleteDatabaseFile(DbConnection conn, String dbname) {
        assert(dbname != null && !dbname.trim().equals(""));
        try {
            detachDatabase(conn, dbname);
        } catch (ServiceException se) {
            ZimbraLog.dbconn.warn("failed to detach while deleting");
        }
        deleted.put(dbname,true);
        ZimbraLog.dbconn.info("deleting database file for DB '" + dbname + "'");
        new File(getDatabaseFilename(dbname)).delete();
        new File(getDatabaseFilename(dbname) + "-journal").delete();
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
            whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW; //we use a small pool. we can easily starve when any code requires more than one connection to complete a single operation

            // override pool size if specified in prefs
            mPoolSize = readConfigInt("sqlite_pool_size", "connection pool size", DEFAULT_CONNECTION_POOL_SIZE);
        }

        private Properties getSQLiteProperties() {
            Properties props = new Properties();
            if (LC.sqlite_shared_cache_enabled.booleanValue())
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


    @Override
    public void flushToDisk() {
        // not really implemented
    }

    @Override
    public String toString() {
        return "SQLite";
    }

    @Override
    protected int getInClauseBatchSize() {
        return 200;
    }
    
    @Override
    public void checkParamLimit(int numParams) throws ServiceException {
        if (numParams > 999) {
            throw ServiceException.FAILURE("SQLite parameter limit will be exceeded",
                new SQLException(mErrorCodes.get(Db.Error.TOO_MANY_SQL_PARAMS)));
        }
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

    @Override
    public String concat(String... fieldsToConcat) {
        Joiner joiner = Joiner.on(" || ").skipNulls();
        return joiner.join(fieldsToConcat);
    }

    @Override
    public String sign(String field) {
        return "CASE WHEN(" + field + ")>0 THEN '1' WHEN(" + field + ")<0 THEN '-1' ELSE '0' END";
    }

    @Override
    public String lpad(String field, int padSize, String padString) {
        return "SUBSTR('" + Strings.repeat(padString, padSize) + "' || " + field + ", -" + padSize + ", " + padSize + ")";
    }

    @Override
    public String limit(int offset, int limit) {
        return "LIMIT " + offset + "," + limit;
    }
}
