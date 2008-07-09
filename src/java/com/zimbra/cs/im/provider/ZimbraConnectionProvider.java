/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.im.provider;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

import org.jivesoftware.database.ConnectionProvider;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool;

public class ZimbraConnectionProvider implements ConnectionProvider {

    public boolean isPooled() {
        return true;
    }
    
    public Connection getConnection() throws SQLException {
        try {
            DbPool.Connection conn = DbPool.getConnection();
            if (conn == null)
                return null;
            return new ZimbraConnection(conn);
        } catch (ServiceException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SQLException) 
                throw (SQLException)cause;
            throw new SQLException("Caught ServiceException: "+ex.toString());
        }
    }

    public void restart() {
    }
    public void start() {
    }
    public void destroy() {
    }
    
    private static class ZimbraConnection implements java.sql.Connection {
        
        private DbPool.Connection mConnection;
        
        ZimbraConnection(DbPool.Connection conn) {
            mConnection = conn;
        }

        public void clearWarnings() throws SQLException {
            mConnection.getConnection().clearWarnings();
        }

        public void close() throws SQLException {
            try {
                mConnection.close();
            } catch (ServiceException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof SQLException) 
                    throw (SQLException)cause;
                throw new SQLException("Caught ServiceException "+ex.toString());
            }
        }

        public void commit() throws SQLException {
            try {
                mConnection.commit();
            } catch (ServiceException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof SQLException) 
                    throw (SQLException)cause;
                throw new SQLException("Caught ServiceException "+ex.toString());
            }
        }

        public Statement createStatement() throws SQLException {
            return mConnection.getConnection().createStatement();
        }

        public Statement createStatement(int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability)
        throws SQLException {
            return mConnection.getConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public Statement createStatement(int resultSetType,
                                         int resultSetConcurrency)
        throws SQLException {
            return mConnection.getConnection().createStatement(resultSetType, resultSetConcurrency);
        }
        
        public boolean getAutoCommit() throws SQLException {
            return mConnection.getConnection().getAutoCommit();
        }
        
        public String getCatalog() throws SQLException {
            return mConnection.getConnection().getCatalog();
        }

        public int getHoldability() throws SQLException {
            return mConnection.getConnection().getHoldability();
        }

        public DatabaseMetaData getMetaData() throws SQLException {
            return mConnection.getConnection().getMetaData();
        }

        public int getTransactionIsolation() throws SQLException {
            return mConnection.getConnection().getTransactionIsolation();
        }

        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return mConnection.getConnection().getTypeMap();
        }

        public SQLWarning getWarnings() throws SQLException {
            return mConnection.getConnection().getWarnings();
        }

        public boolean isClosed() throws SQLException {
            return mConnection.getConnection().isClosed();
        }

        public boolean isReadOnly() throws SQLException {
            return mConnection.getConnection().isReadOnly();
        }

        public String nativeSQL(String sql) throws SQLException {
            return mConnection.getConnection().nativeSQL(sql);
        }

        public CallableStatement prepareCall(String sql, int resultSetType,
                                             int resultSetConcurrency,
                                             int resultSetHoldability)
        throws SQLException {
            return mConnection.getConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public CallableStatement prepareCall(String sql, int resultSetType,
                                             int resultSetConcurrency)
        throws SQLException {
            return mConnection.getConnection().prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        public CallableStatement prepareCall(String sql) throws SQLException {
            return mConnection.getConnection().prepareCall(sql);
        }

        public PreparedStatement prepareStatement(String sql,
                                                  int resultSetType,
                                                  int resultSetConcurrency,
                                                  int resultSetHoldability)
        throws SQLException {
            return mConnection.getConnection().prepareStatement(sql,resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public PreparedStatement prepareStatement(String sql,
                                                  int resultSetType,
                                                  int resultSetConcurrency)
        throws SQLException {
            return mConnection.getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        public PreparedStatement prepareStatement(String sql,
                                                  int autoGeneratedKeys)
        throws SQLException {
            return mConnection.prepareStatement(sql, autoGeneratedKeys);
        }

        public PreparedStatement prepareStatement(String sql,
                                                  int[] columnIndexes)
        throws SQLException {
            return mConnection.getConnection().prepareStatement(sql, columnIndexes);
        }

        public PreparedStatement prepareStatement(String sql,
                                                  String[] columnNames)
        throws SQLException {
            return mConnection.getConnection().prepareStatement(sql, columnNames);
        }

        public PreparedStatement prepareStatement(String sql)
        throws SQLException {
            return mConnection.prepareStatement(sql);
        }

        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            mConnection.getConnection().releaseSavepoint(savepoint);
        }

        public void rollback() throws SQLException {
            try {
                mConnection.rollback();
            } catch (ServiceException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof SQLException) 
                    throw (SQLException)cause;
                throw new SQLException("Caught ServiceException: "+ex);
            }
        }

        public void rollback(Savepoint savepoint) throws SQLException {
            mConnection.getConnection().rollback();
        }

        public void setAutoCommit(boolean autoCommit) throws SQLException {
            mConnection.getConnection().setAutoCommit(autoCommit);
        }

        public void setCatalog(String catalog) throws SQLException {
            mConnection.getConnection().setCatalog(catalog);
        }

        public void setHoldability(int holdability) throws SQLException {
            mConnection.getConnection().setHoldability(holdability);
        }

        public void setReadOnly(boolean readOnly) throws SQLException {
            mConnection.getConnection().setReadOnly(readOnly);
        }

        public Savepoint setSavepoint() throws SQLException {
            return mConnection.getConnection().setSavepoint();
        }

        public Savepoint setSavepoint(String name) throws SQLException {
            return mConnection.getConnection().setSavepoint(name);
        }

        public void setTransactionIsolation(int level) throws SQLException {
            try {
                mConnection.setTransactionIsolation(level);
            } catch (ServiceException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof SQLException) 
                    throw (SQLException)cause;
                throw new SQLException("Caught ServiceException: "+ex);
            }
        }

        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            mConnection.getConnection().setTypeMap(map);
        }
    }
}
