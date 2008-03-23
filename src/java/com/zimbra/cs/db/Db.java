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

/*
 * Created on Apr 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.db;

import java.sql.SQLException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.Connection;

/**
 * @author schemers
 */
public abstract class Db {

    public static enum Error {
        DEADLOCK_DETECTED,
        DUPLICATE_ROW,
        FOREIGN_KEY_CHILD_EXISTS,
        FOREIGN_KEY_NO_PARENT,
        NO_SUCH_DATABASE,
        NO_SUCH_TABLE;
    }

    public static enum Capability {
        BITWISE_OPERATIONS,
        BOOLEAN_DATATYPE,
        BROKEN_IN_CLAUSE,
        CASE_SENSITIVE_COMPARISON,
        CAST_AS_BIGINT,
        CLOB_COMPARISON,
        DISABLE_CONSTRAINT_CHECK,
        LIMIT_CLAUSE,
        MULTITABLE_UPDATE,
        ON_DUPLICATE_KEY,
        ON_UPDATE_CASCADE,
        UNIQUE_NAME_INDEX;
    }


    private static Db sDatabase;

    public synchronized static Db getInstance() {
        if (sDatabase == null) {
            String className = LC.zimbra_class_database.value();
            if (className != null && !className.equals("")) {
                try {
                    sDatabase = (Db) Class.forName(className).newInstance();
                } catch (Exception e) {
                    ZimbraLog.system.error("could not instantiate database configuration '" + className + "'; defaulting to MySQL", e);
                }
            }
            if (sDatabase == null)
                sDatabase = new MySQL();
        }
        return sDatabase;
    }


    private static final int DEFAULT_IN_CLAUSE_BATCH_SIZE = 400;

    /** Returns the maximum number of items to include in an "IN (?, ?, ...)"
     *  clause.  For databases with a broken or hugely nonperformant IN clause,
     *  e.g. Derby pre-10.3 (see DERBY-47 JIRA), we hardcode the IN clause
     *  batch size to 1. */
    public static int getINClauseBatchSize() {
        return getInstance().supportsCapability(Capability.BROKEN_IN_CLAUSE) ? 1 : DEFAULT_IN_CLAUSE_BATCH_SIZE;
    }


    /** Returns whether the currently-configured database supports the given
     *  {@link Db.Capability}. */
    public static boolean supports(Db.Capability capability) {
        return getInstance().supportsCapability(capability);
    }

    abstract boolean supportsCapability(Db.Capability capability);


    /** Returns whether the given {@link SQLException} is an instance of the
     *  specified {@link Db.Error}. */
    public static boolean errorMatches(SQLException e, Db.Error error) {
        return getInstance().compareError(e, error);
    }

    abstract boolean compareError(SQLException e, Db.Error error);


    /** Returns the set of configuration settings necessary to initialize the
     *  appropriate database connection pool.
     * @see DbPool#getPool() */
    abstract DbPool.PoolConfig getPoolConfig();

    /** Returns <tt>true</tt> if the database with the given name exists. */
    abstract public boolean databaseExists(Connection conn, String databaseName)
    throws ServiceException;

    /** Generates the correct SQL to direct the current database engine to use
     *  a particular index to perform a SELECT query.  This string should come
     *  after the FROM clause and before the WHERE clause in the final SQL
     *  query. */
    public static String forceIndex(String index) {
        if (index == null || index.trim().equals(""))
            return "";
        return getInstance().forceIndexClause(index);
    }

    abstract String forceIndexClause(String index);

    /**
     * WARN: only call to completely shutdown db.  Currently only applicable to derby
     */
    void shutdown() {
    	//default is to do nothing
    }

    /** Generates a SELECT expression representing a BOOLEAN.  For databases
     *  that don't support a BOOLEAN datatype, returns an appropriate CASE
     *  clause that evaluates to 1 when the given BOOLEAN clause is true and
     *  0 when it's false. */
    static String selectBOOLEAN(String clause) {
        if (supports(Capability.BOOLEAN_DATATYPE))
            return clause;
        else
            return "CASE WHEN " + clause + " THEN 1 ELSE 0 END";
    }


    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column equals a string later specified by <tt>stmt.setString()</tt>
     *  under a case-insensitive comparison.  Note that the caller should
     *  pass an upcased version of the comparison string in the subsequent
     *  call to <tt>stmt.setString()</tt>. */
    static String equalsSTRING(String column) {
        if (supports(Capability.CASE_SENSITIVE_COMPARISON))
            return "UPPER(" + column + ") = ?";
        else
            return column + " = ?";
    }

    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column is a case-insensitive match to a SQL pattern string later
     *  specified by <tt>stmt.setString()</tt> under a  comparison.  Note that
     *  the caller should pass an upcased version of the comparison string in
     *  the subsequent call to <tt>stmt.setString()</tt>. */
    static String likeSTRING(String column) {
        if (supports(Capability.CASE_SENSITIVE_COMPARISON))
            return "UPPER(" + column + ") LIKE ?";
        else
            return column + " LIKE ?";
    }


    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column matches a bitmask later specified by <tt>stmt.setLong()</tt>.
     *  Note that this is only valid when the bitmask has only 1 bit set. */
    static String bitmaskAND(String column) {
        if (supports(Capability.BITWISE_OPERATIONS))
            return column + " & ?";
        else
            return "MOD(" + column + " / ?, 2) = 1";
    }

    /** Generates a WHERE-type clause that evaluates to true when the given
     *  column matches the given bitmask.  Note that this is only valid when
     *  the bitmask has only 1 bit set. */
    static String bitmaskAND(String column, long bitmask) {
        if (supports(Capability.BITWISE_OPERATIONS))
            return column + " & " + bitmask;
        else
            return "MOD(" + column + " / " + bitmask + ", 2) = 1";
    }
}
