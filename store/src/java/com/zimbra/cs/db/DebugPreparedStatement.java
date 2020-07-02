/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.DelegatingPreparedStatement;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;

class DebugPreparedStatement extends DelegatingPreparedStatement {

    private static final int MAX_STRING_LENGTH = 1024;
    private static long sSlowSqlThreshold = Long.MAX_VALUE;

    private final PreparedStatement mStmt;
    private String mSql;
    private long mStartTime;

    /**
     * A list that implicitly resizes when {@link #set} is called.
     */
    @SuppressWarnings("serial")
    private class AutoSizeList<E>
    extends ArrayList<E> {
        @Override
        public E set(int index, E element) {
            if (index >= size()) {
                for (int i = size(); i <= index; i++) {
                    add(null);
                }
            }
            return super.set(index, element);
        }
    }
    private List<Object> mParams = new AutoSizeList<Object>();

    DebugPreparedStatement(DelegatingConnection conn, PreparedStatement stmt, String sql) {
        super(conn, stmt);
        mStmt = stmt;
        mSql = sql;
    }

    public static void setSlowSqlThreshold(long millis) {
        ZimbraLog.sqltrace.info("Setting slow SQL threshold to %dms.", millis);
        sSlowSqlThreshold = millis;
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
            Object o = mParams.get(paramIndex);
            if (o == null) {
                o = "NULL";
            } else if (o instanceof String) {
                // Escape single-quotes
                String s = (String) o;
                if (s.indexOf('\'') >= 0) {
                    s = s.replaceAll("'", "''");
                }
                o = "'" + s + "'";
            }
            buf.append(o);

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
        if (time > sSlowSqlThreshold) {
            String sql = getSql();
            ZimbraLog.sqltrace.info("Slow execution (%dms): %s", time,  sql);
        } else if (ZimbraLog.sqltrace.isDebugEnabled()) {
            String sql = getSql();
            ZimbraLog.sqltrace.debug("%s - %sms%s", sql, time, getHashCodeString());
        }
    }

    private void logException(SQLException e) {
        if (ZimbraLog.sqltrace.isDebugEnabled()) {
            ZimbraLog.sqltrace.debug(e.toString() + ": " + getSql() + getHashCodeString());
        }
    }

    private void processDbError(SQLException e) {
        if (Db.errorMatches(e, Db.Error.TABLE_FULL))
            Zimbra.halt("DB out of space", e);
    }

    private String getHashCodeString() {
        String hashCodeString = "";
        try {
            hashCodeString= ", conn=" + mStmt.getConnection().hashCode();
        } catch (SQLException e) {
            ZimbraLog.sqltrace.warn("Unable to determine connection hashcode", e);
        }
        return hashCodeString;
    }

    private void startTimer() {
        mStartTime = System.currentTimeMillis();
    }

    /////////// PreparedStatement implementation ///////////////

    @Override
    public ResultSet executeQuery() throws SQLException {
        startTimer();
        ResultSet rs;
        try {
            rs = mStmt.executeQuery();
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return rs;
    }

    @Override
    public int executeUpdate() throws SQLException {
        startTimer();
        int numRows;
        try {
            numRows = mStmt.executeUpdate();
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return numRows;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        mParams.set(parameterIndex, null);
        mStmt.setNull(parameterIndex, sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setShort(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setInt(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setLong(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        String loggedValue = x;
        if (x != null && x.length() > MAX_STRING_LENGTH) {
            loggedValue = loggedValue.substring(0, MAX_STRING_LENGTH) + "...";
        }
        mParams.set(parameterIndex, loggedValue);
        mStmt.setString(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        mParams.set(parameterIndex, "<byte[]>");
        mStmt.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Ascii Stream>");
        mStmt.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Unicode Stream>");
        mStmt.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Binary Stream>");
        mStmt.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x)
    throws SQLException {
        mParams.set(parameterIndex, "<Binary Stream>");
        mStmt.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void clearParameters() throws SQLException {
        mParams.clear();
        mStmt.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale)
    throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setObject(parameterIndex, x, targetSqlType, scale);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType)
    throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setObject(parameterIndex, x);
    }

    @Override
    public boolean execute() throws SQLException {
        startTimer();
        boolean result;
        try {
            result = mStmt.execute();
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return result;
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length)
    throws SQLException {
        mParams.set(parameterIndex, "<Character Stream>");
        mStmt.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int i, Ref x) throws SQLException {
        mParams.set(i, "<Ref>");
        mStmt.setRef(i, x);
    }

    @Override
    public void setBlob(int i, Blob x) throws SQLException {
        mParams.set(i, "<Blob>");
        mStmt.setBlob(i, x);
    }

    @Override
    public void setClob(int i, Clob x) throws SQLException {
        mParams.set(i, "<Clob>");
        mStmt.setClob(i, x);
    }

    @Override
    public void setArray(int i, Array x) throws SQLException {
        mParams.set(i, "<Array>");
        mStmt.setArray(i, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        mParams.set(paramIndex, null);
        mStmt.setNull(paramIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        mParams.set(parameterIndex, x);
        mStmt.setURL(parameterIndex, x);
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        mSql = sql;
        startTimer();
        ResultSet rs;
        try {
            rs = mStmt.executeQuery(sql);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return rs;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        mSql = sql;
        startTimer();
        int numRows = 0;
        try {
            mStmt.executeUpdate(sql);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return numRows;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        mSql = sql;
        startTimer();
        boolean result = false;
        try {
            mStmt.execute(sql);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return result;
    }

    @Override
    public int[] executeBatch() throws SQLException {
        startTimer();
        int[] result;
        try {
            result = mStmt.executeBatch();
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return result;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        startTimer();
        int numRows;
        try {
            numRows = mStmt.executeUpdate(sql, autoGeneratedKeys);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return numRows;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        startTimer();
        int numRows;
        try {
            numRows = mStmt.executeUpdate(sql, columnIndexes);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return numRows;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        startTimer();
        int numRows;
        try {
            numRows = mStmt.executeUpdate(sql, columnNames);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return numRows;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        startTimer();
        boolean result;
        try {
            result = mStmt.execute(sql, autoGeneratedKeys);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return result;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        startTimer();
        boolean result;
        try {
            result = mStmt.execute(sql, columnIndexes);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return result;
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        startTimer();
        boolean result;
        try {
            result = mStmt.execute(sql, columnNames);
        } catch (SQLException e) {
            logException(e);
            processDbError(e);
            throw e;
        }
        log();
        return result;
    }
}
