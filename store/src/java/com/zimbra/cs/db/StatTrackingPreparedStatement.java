/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import com.zimbra.cs.stats.ActivityTracker;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * A decorator class for the PreparedStatement interface that tracks the performance
 * of database calls for various request types in zmstats CSV files.
 * The actual SQL query string can be passed in the constructor or via setQueryString,
 * since the actual SQL query can't reliably be retrieved from the PreparedStatement.
 * Calls to trackQueryStats happen in all "execute" methods that take no arguments;
 * all other calls simply delegate to the wrapped PreparedStatement instance.
 *
 * Added for bug #56968
 * @author iraykin
 */
public class StatTrackingPreparedStatement implements PreparedStatement {
    private final PreparedStatement stmt;
    private String queryString;
    private ActivityTracker tracker = ZimbraPerf.SQL_TRACKER;

    public StatTrackingPreparedStatement(PreparedStatement stmt) {
        this.stmt = stmt;
        ZimbraPerf.incrementPrepareCount();
    }

    public StatTrackingPreparedStatement(PreparedStatement prepareStatement, String sql) {
        this(prepareStatement);
        setQueryString(sql);
    }

    /* This method is mainly for testing, so that a custom tracker
     * can be injected to avoid dependency on ZimbraPerf.SQL_TRACKER
     */
    public void setTracker(ActivityTracker tracker) {
        this.tracker = tracker;
    }

    /* The PreparedStatement interface doesn't allow access the actual SQL query
     * to be executed, so it needs to be provided explicitly in the constructor
     * or using this method. If the sql string is not available, nothing will be tracked.
     */
    public void setQueryString(String sql) {
        this.queryString = sql;
    }

    /* Not sure if the query types should be mutually exclusive like this.
     * Maybe should check for presence of all types and track all that apply?
     */
    private String getQueryType(String sql) {
        sql = sql.toLowerCase();
        if (sql.startsWith("select")) {
            return "SELECT";
        }
        else if (sql.startsWith("insert")) {
            return "INSERT";
        }
        else if (sql.startsWith("update")) {
            return "UPDATE";
        }
        else if (sql.startsWith("delete")) {
            return "DELETE";
        }
        else {
            return "other";
        }
    }

    private void trackQueryStats(String sql, long startTime) {
        if (sql != null) {
            tracker.addStat(getQueryType(sql), startTime);
        }
    }
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        long startTime = System.currentTimeMillis();
        ResultSet results = stmt.executeQuery(sql);
        trackQueryStats(sql, startTime);
        return results;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {

        DeadlockRetry exec = new DeadlockRetry() {

            @Override
            public ExecuteResult<Integer> execute() throws SQLException {
                long startTime = System.currentTimeMillis();
                int results = stmt.executeUpdate(sql);
                trackQueryStats(sql, startTime);
                return new ExecuteResult<Integer>(results);
            }
        };
        return exec.doRetry().getResult();
    }

    @Override
    public void close() throws SQLException {
        stmt.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        stmt.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        stmt.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        stmt.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        stmt.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        stmt.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {

        return stmt.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {

        stmt.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        stmt.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        long startTime = System.currentTimeMillis();
        boolean results = stmt.execute(sql);
        trackQueryStats(sql, startTime);
        return results;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return stmt.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        stmt.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        stmt.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return stmt.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return stmt.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return stmt.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException {

        DeadlockRetry exec = new DeadlockRetry() {

            @Override
            public ExecuteResult<Integer> execute() throws SQLException {
                return new ExecuteResult<Integer>(stmt.executeUpdate(sql, autoGeneratedKeys));
            }
        };
        return exec.doRetry().getResult();
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes)
            throws SQLException {

        DeadlockRetry exec = new DeadlockRetry() {

            @Override
            public ExecuteResult<Integer> execute() throws SQLException {
                return new ExecuteResult<Integer>(stmt.executeUpdate(sql, columnIndexes));
            }
        };
        return exec.doRetry().getResult();
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames)
            throws SQLException {
        DeadlockRetry exec = new DeadlockRetry() {

            @Override
            public ExecuteResult<Integer> execute() throws SQLException {
                return new ExecuteResult<Integer>(stmt.executeUpdate(sql, columnNames));
            }
        };
        return exec.doRetry().getResult();
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys)
            throws SQLException {
        return stmt.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return stmt.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames)
            throws SQLException {
        return stmt.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        stmt.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        stmt.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return stmt.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return stmt.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return stmt.isWrapperFor(iface);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        long startTime = System.currentTimeMillis();
        ResultSet results = stmt.executeQuery();
        trackQueryStats(queryString, startTime);
        return results;
    }

    @Override
    public int executeUpdate() throws SQLException {

        DeadlockRetry exec = new DeadlockRetry() {

            @Override
            public ExecuteResult<Integer> execute() throws SQLException {
                long startTime = System.currentTimeMillis();
                int results = stmt.executeUpdate();
                trackQueryStats(queryString, startTime);
                return new ExecuteResult<Integer>(results);
            }
        };
        return exec.doRetry().getResult();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        stmt.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        stmt.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        stmt.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        stmt.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        stmt.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        stmt.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        stmt.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        stmt.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x)
            throws SQLException {
        stmt.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        stmt.setString(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        stmt.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        stmt.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        stmt.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x)
            throws SQLException {
        stmt.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        stmt.setAsciiStream(parameterIndex, x, length);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        stmt.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        stmt.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {
        stmt.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType)
            throws SQLException {
        stmt.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        stmt.setObject(parameterIndex, x);
    }

    @Override
    public boolean execute() throws SQLException {
        long startTime = System.currentTimeMillis();
        boolean results = stmt.execute();
        trackQueryStats(queryString, startTime);
        return results;
    }

    @Override
    public void addBatch() throws SQLException {
        stmt.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length)
            throws SQLException {
        stmt.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        stmt.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        stmt.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        stmt.setClob(parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        stmt.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return stmt.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal)
            throws SQLException {
        stmt.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal)
            throws SQLException {
        stmt.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
            throws SQLException {
        stmt.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName)
            throws SQLException {
        stmt.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        stmt.setURL(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return stmt.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        stmt.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value)
            throws SQLException {
        stmt.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value,
            long length) throws SQLException {
        stmt.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        stmt.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length)
            throws SQLException {
        stmt.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length)
            throws SQLException {
        stmt.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length)
            throws SQLException {
        stmt.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject)
            throws SQLException {
        stmt.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType,
            int scaleOrLength) throws SQLException {
        stmt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length)
            throws SQLException {
        stmt.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length)
            throws SQLException {
        stmt.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader,
            long length) throws SQLException {
        stmt.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x)
            throws SQLException {
        stmt.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x)
            throws SQLException {
        stmt.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader)
            throws SQLException {
        stmt.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value)
            throws SQLException {
        stmt.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        stmt.setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream)
            throws SQLException {
        stmt.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        stmt.setNClob(parameterIndex, reader);
    }

    private static abstract class DeadlockRetry extends AbstractRetry<Integer> {

        @Override
        protected boolean retryException(SQLException sqle) {
            return sqle.getMessage().toLowerCase().contains("deadlock");
        }
    }
}
