/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.zimbra.cs.service.util.ZimbraPerf;
import com.zimbra.cs.util.ZimbraLog;


class DebugPreparedStatement implements PreparedStatement {

    private String mSql;
    private PreparedStatement mStmt;
    private long mStartTime;
    
    /**
     * A list that implicitly resizes when {@link #set} is called.
     */
    private class AutoSizeList
    extends ArrayList {
        public Object set(int index, Object element) {
            if (index >= size()) {
                for (int i = size(); i <= index; i++) {
                    add(null);
                }
            }
            return super.set(index, element);
        }
    }
    private List /* <Object> */ mParams = new AutoSizeList();
    
    DebugPreparedStatement(PreparedStatement stmt, String sql) {
        mStmt = stmt;
        mSql = sql;
    }

    private String getSql() {
        if (mSql == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        int start = 0;
        int qPos = mSql.indexOf('?', start);
        int paramIndex = 1;
        
        while (qPos >= 0) {
            buf.append(mSql.substring(start, qPos));
            if (paramIndex == mParams.size()) {
                throw new IllegalStateException("Not enough parameters bound for SQL: " + mSql);
            }
            buf.append(mParams.get(paramIndex));
            
            // Increment indexes
            start = qPos + 1;
            if (start >= mSql.length()) {
                break;
            }
            qPos = mSql.indexOf('?', start);
            paramIndex++;
        }
        
        if (start < mSql.length()) {
            // Append the rest of the string
            buf.append(mSql.substring(start, mSql.length()));
        }
        return buf.toString();
    }
    
    private void log() {
        long time = System.currentTimeMillis() - mStartTime;
        String sql = getSql();
        ZimbraLog.sqltrace.debug(sql + " - " + time + "ms");
        ZimbraPerf.updateDbStats(sql, (int) time);
    }
    
    private void startTimer() {
        mStartTime = System.currentTimeMillis();
    }
    
    /////////// PreparedStatement implementation ///////////////
    
    public ResultSet executeQuery() throws SQLException {
        startTimer();
        ResultSet rs = mStmt.executeQuery();
        log();
        return rs;
    }

    public int executeUpdate() throws SQLException {
        startTimer();
        int numRows = mStmt.executeUpdate();
        log();
        return numRows;
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        mParams.set(parameterIndex, "NULL");
        mStmt.setNull(parameterIndex, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        mParams.set(parameterIndex, new Boolean(x));
        mStmt.setBoolean(parameterIndex, x);
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        mParams.set(parameterIndex, new Byte(x));
        mStmt.setByte(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        mParams.set(parameterIndex, new Short(x));
        mStmt.setShort(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        mParams.set(parameterIndex, new Integer(x));
        mStmt.setInt(parameterIndex, x);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        mParams.set(parameterIndex, new Long(x));
        mStmt.setLong(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        mParams.set(parameterIndex, new Float(x));
        mStmt.setFloat(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        mParams.set(parameterIndex, new Double(x));
        mStmt.setDouble(parameterIndex, x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setBigDecimal(parameterIndex, x);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setString(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        mParams.set(parameterIndex, "<byte[]>");
        mStmt.setBytes(parameterIndex, x);
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setDate(parameterIndex, x);
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTime(parameterIndex, x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Ascii Stream>");
        mStmt.setAsciiStream(parameterIndex, x, length);
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Unicode Stream>");
        mStmt.setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Binary Stream>");
        mStmt.setBinaryStream(parameterIndex, x, length);
    }

    public void clearParameters() throws SQLException {
        mParams.clear();
        mStmt.clearParameters();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale)
    throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setObject(parameterIndex, x, targetSqlType, scale);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType)
    throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setObject(parameterIndex, x);
    }

    public boolean execute() throws SQLException {
        startTimer();
        boolean result = mStmt.execute();
        log();
        return result;
    }

    public void addBatch() throws SQLException {
        mStmt.addBatch();
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Character Stream>");
        mStmt.setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(int i, Ref x) throws SQLException {
        mParams.set(i, "<Ref>");
        mStmt.setRef(i, x);
    }

    public void setBlob(int i, Blob x) throws SQLException {
        mParams.set(i, "<Blob>");
        mStmt.setBlob(i, x);
    }

    public void setClob(int i, Clob x) throws SQLException {
        mParams.set(i, "<Clob>");
        mStmt.setClob(i, x);
    }

    public void setArray(int i, Array x) throws SQLException {
        mParams.set(i, "<Array>");
        mStmt.setArray(i, x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return mStmt.getMetaData();
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        mParams.set(paramIndex, "NULL");
        mStmt.setNull(paramIndex, sqlType, typeName);
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setURL(parameterIndex, x);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return mStmt.getParameterMetaData();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        mSql = sql;
        startTimer();
        ResultSet rs = mStmt.executeQuery(sql);
        log();
        return rs;
    }

    public int executeUpdate(String sql) throws SQLException {
        mSql = sql;
        startTimer();
        int numRows = mStmt.executeUpdate(sql);
        log();
        return numRows;
    }

    public void close() throws SQLException {
        mStmt.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return mStmt.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        mStmt.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return mStmt.getMaxRows();
    }

    public void setMaxRows(int max) throws SQLException {
        mStmt.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        mStmt.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return mStmt.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        mStmt.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        mStmt.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return mStmt.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        mStmt.clearWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        mStmt.setCursorName(name);
    }

    public boolean execute(String sql) throws SQLException {
        mSql = sql;
        startTimer(); 
        boolean result = mStmt.execute(sql);
        log();
        return result;
    }

    public ResultSet getResultSet() throws SQLException {
        return mStmt.getResultSet();
    }

    public int getUpdateCount() throws SQLException {
        return mStmt.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return mStmt.getMoreResults();
    }

    public void setFetchDirection(int direction) throws SQLException {
        mStmt.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return mStmt.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        mStmt.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return mStmt.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return mStmt.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return mStmt.getResultSetType();
    }

    public void addBatch(String sql) throws SQLException {
        mStmt.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        mStmt.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        startTimer();
        int[] result = mStmt.executeBatch();
        log();
        return result;
    }

    public Connection getConnection() throws SQLException {
        return mStmt.getConnection();
    }

    public boolean getMoreResults(int current) throws SQLException {
        return mStmt.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return mStmt.getGeneratedKeys();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        startTimer();
        int numRows = mStmt.executeUpdate(sql, autoGeneratedKeys);
        log();
        return numRows;
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        startTimer();
        int numRows = mStmt.executeUpdate(sql, columnIndexes);
        log();
        return numRows;
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        startTimer();
        int numRows = mStmt.executeUpdate(sql, columnNames);
        log();
        return numRows;
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        startTimer();
        boolean result = mStmt.execute(sql, autoGeneratedKeys);
        log();
        return result;
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        startTimer();
        boolean result = mStmt.execute(sql, columnIndexes);
        log();
        return result;
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        startTimer();
        boolean result = mStmt.execute(sql, columnNames);
        log();
        return result;
    }

    public int getResultSetHoldability() throws SQLException {
        return mStmt.getResultSetHoldability();
    }

}
