/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.hsqldb.cmdline.SqlFile;

import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbPool.PoolConfig;

/**
 * HSQLDB is for unit test. All data is in memory, not persistent across JVM restarts.
 *
 * @author ysasaki
 */
public final class HSQLDB extends Db {

    /**
     * Populates ZIMBRA and MBOXGROUP1 scheme.
     */
    static void createDatabase() throws Exception {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DbConnection conn = DbPool.getConnection();
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?");
            stmt.setString(1, "ZIMBRA");
            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return;  // already exists
            }
            execute(conn, "src/db/hsqldb/db.sql");
            execute(conn, "src/db/hsqldb/create_database.sql");
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    /**
     * Deletes all records from all tables.
     */
    static void clearDatabase() throws Exception {
        DbConnection conn = DbPool.getConnection();
        try {
            execute(conn, "src/db/hsqldb/clear.sql");
        } finally {
            DbPool.quietClose(conn);
        }
    }

    private static void execute(DbConnection conn, String file) throws Exception {
        Map<String, String> vars = Collections.singletonMap("DATABASE_NAME", DbMailbox.getDatabaseName(1));
        SqlFile sql = new SqlFile(new File(file));
        sql.addUserVars(vars);
        sql.setConnection(conn.getConnection());
        sql.execute();
        conn.commit();
    }

    @Override
    PoolConfig getPoolConfig() {
        return new Config();
    }

    /**
     * TODO
     */
    @Override
    boolean supportsCapability(Capability capability) {
        return true;
    }

    /**
     * TODO
     */
    @Override
    boolean compareError(SQLException e, Error error) {
        return false;
    }

    /**
     * Always returns true. All schema is pre-populated.
     */
    @Override
    public boolean databaseExists(DbConnection connection, String dbname) {
        return true;
    }

    /**
     * TODO
     */
    @Override
    String forceIndexClause(String index) {
        return "";
    }

    /**
     * TODO
     */
    @Override
    String getIFNULLClause(String expr1, String expr2) {
        return "";
    }

    @Override
    public void flushToDisk() {
    }


    private static final class Config extends PoolConfig {
        Config() {
            mDriverClassName = "org.hsqldb.jdbcDriver";
            mPoolSize = 10;
            mRootUrl = "jdbc:hsqldb:mem:";
            mConnectionUrl = "jdbc:hsqldb:mem:zimbra";
            mSupportsStatsCallback = false;
            mDatabaseProperties = new Properties();
        }
    }

}
