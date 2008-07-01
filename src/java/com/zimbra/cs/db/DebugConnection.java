/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

import com.zimbra.common.util.ZimbraLog;

class DebugConnection implements Connection {

    private final Connection mConn;
    
    DebugConnection(Connection conn) {
        mConn = conn;
    }

    Connection getConnection() {
        return mConn;
    }
    
    public Statement createStatement() throws SQLException {
        return mConn.createStatement();
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new DebugPreparedStatement(mConn.prepareStatement(sql), sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return mConn.prepareCall(sql);
    }

    public String nativeSQL(String sql) throws SQLException {
        return mConn.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        mConn.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return mConn.getAutoCommit();
    }

    public void commit() throws SQLException {
        ZimbraLog.sqltrace.debug("commit, conn=" + mConn.hashCode());
        mConn.commit();
    }

    public void rollback() throws SQLException {
        ZimbraLog.sqltrace.debug("rollback, conn=" + mConn.hashCode());
        mConn.rollback();
    }

    public void close() throws SQLException {
        mConn.close();
    }

    public boolean isClosed() throws SQLException {
        return mConn.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return mConn.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        mConn.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return mConn.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        mConn.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return mConn.getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        mConn.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return mConn.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return mConn.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        mConn.clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
    throws SQLException {
        return mConn.createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency)
    throws SQLException {
        return new DebugPreparedStatement(
            mConn.prepareStatement(sql, resultSetType, resultSetConcurrency), sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException {
        return mConn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public Map<String,Class<?>> getTypeMap() throws SQLException {
        return mConn.getTypeMap();
    }

    public void setTypeMap(Map<String,Class<?>> map) throws SQLException {
        mConn.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        mConn.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return mConn.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return mConn.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return mConn.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        mConn.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        mConn.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                     int resultSetHoldability)
    throws SQLException {
        return mConn.createStatement(
            resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability)
    throws SQLException {
        return new DebugPreparedStatement(
            mConn.prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability), sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability)
    throws SQLException {
        return mConn.prepareCall(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
    throws SQLException {
        return new DebugPreparedStatement(
            mConn.prepareStatement(sql, autoGeneratedKeys), sql);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
    throws SQLException {
        return new DebugPreparedStatement(
            mConn.prepareStatement(sql, columnIndexes), sql);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
    throws SQLException {
        return new DebugPreparedStatement(
            mConn.prepareStatement(sql, columnNames), sql);
    }
}
