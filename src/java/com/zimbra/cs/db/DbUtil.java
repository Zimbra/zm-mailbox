/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.db.DbPool.Connection;

/**
 * <code>DbUtil</code> contains some database utility methods and
 * automates the task of running SQL statements.  It
 * wraps the JDBC <code>Connection</code>and <code>PreparedStatement</code>
 * classes and frees the caller from having to handle <code>SQLException</code>
 * and allocate and deallocate database resources.<p>
 * 
 * Query results are read entirely into memory and returned in the form of a
 * {@link com.zimbra.cs.db.DbResults} object.  This improves concurrency,
 * but potentially increases memory consumption.  Code that deals with
 * large result sets should use the JDBC API's directly.
 * 
 * @author bburtin
 */
public class DbUtil {

    public static final int INTEGER_TRUE = 1;
    public static final int INTEGER_FALSE = 0;
    
    public static final int getBooleanIntValue(boolean b) {
        return b ? INTEGER_TRUE : INTEGER_FALSE;
    }
    
    private static final Pattern PAT_ESCAPED_QUOTES1 = Pattern.compile("\\\\'");
    private static final Pattern PAT_ESCAPED_QUOTES2 = Pattern.compile("''");
    private static final Pattern PAT_STRING_CONSTANT = Pattern.compile("'[^']+'");
    private static final Pattern PAT_INTEGER_CONSTANT = Pattern.compile("\\d+");
    private static final Pattern PAT_NULL_CONSTANT = Pattern.compile("NULL");
    private static final Pattern PAT_BEGIN_SPACE = Pattern.compile("^\\s+");
    private static final Pattern PAT_TRAILING_SPACE = Pattern.compile("\\s+$");
    private static final Pattern PAT_MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern PAT_IN_CLAUSE = Pattern.compile("IN \\([^\\)]+\\)", Pattern.DOTALL);

    /**
     * Converts a <tt>java.util.Date</tt> to a <tt>java.sql.Timestamp</tt>.
     * If the date is <tt>null</tt>, returns <tt>null</tt>.
     */
    public static Timestamp dateToTimestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    /**
     * Converts a  <tt>java.sql.Timestamp</tt> to a <tt>java.util.Date</tt>.
     * If the timestamp is <tt>null</tt>, returns <tt>null</tt>.
     */
    public static Date timestampToDate(Timestamp timestamp) {
        return timestamp == null ? null : new Date(timestamp.getTime());
    }

    /**
     * Converts the given <code>SQL</code> string to a normalized version.
     * Strips out any numbers, string constants and extra whitespace.  Flattens
     * the contents of <code>IN</code> clauses.
     */
    public static String normalizeSql(String sql) {
        Matcher matcher = PAT_ESCAPED_QUOTES1.matcher(sql);
        sql = matcher.replaceAll("");
        matcher = PAT_ESCAPED_QUOTES2.matcher(sql);
        sql = matcher.replaceAll("XXX");
        matcher = PAT_STRING_CONSTANT.matcher(sql);
        sql = matcher.replaceAll("XXX");
        matcher = PAT_INTEGER_CONSTANT.matcher(sql);
        sql = matcher.replaceAll("XXX");
        matcher = PAT_NULL_CONSTANT.matcher(sql);
        sql = matcher.replaceAll("XXX");
        matcher = PAT_BEGIN_SPACE.matcher(sql);
        sql = matcher.replaceAll("");
        matcher = PAT_TRAILING_SPACE.matcher(sql);
        sql = matcher.replaceAll("");
        matcher = PAT_MULTIPLE_SPACES.matcher(sql);
        sql = matcher.replaceAll(" ");
        matcher = PAT_IN_CLAUSE.matcher(sql);
        sql = matcher.replaceAll("IN (...)");
        return sql;
    }

    /**
     * Executes the specified query using the specified connection.
     * 
     * @throws ServiceException if the query cannot be executed or an error
     * occurs while retrieving results
     */
    public static DbResults executeQuery(Connection conn, String query, Object[] params)
    throws ServiceException {
        PreparedStatement stmt = null;
        DbResults results = null;
        
        try {
            stmt = conn.prepareStatement(query);
            
            if (params != null) {
                for (int i = 0; i < params.length; i++)
                    stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            results = new DbResults(rs);
        } catch (SQLException e) {
            String message = "SQL: '" + query + "'";
            throw ServiceException.FAILURE(message, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
        
        return results;
    }
    
    /**
     * Executes the specified query using a connection from the connection pool.
     * 
     * @throws ServiceException if the query cannot be executed or an error
     * occurs while retrieving results
     */
    public static DbResults executeQuery(Connection conn, String query, Object param)
    throws ServiceException {
        Object[] params = { param };
        return executeQuery(conn, query, params);
    }
    
    /**
     * Executes the specified query using the specified connection.
     * 
     * @throws ServiceException if the query cannot be executed or an error
     * occurs while retrieving results
     */
    public static DbResults executeQuery(Connection conn, String query)
    throws ServiceException {
        return executeQuery(conn, query, null);
    }
    
    /**
     * Executes the specified query using a connection from the connection pool.
     * 
     * @throws ServiceException if the query cannot be executed or an error
     * occurs while retrieving results
     */
    public static DbResults executeQuery(String query, Object[] params)
    throws ServiceException {
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            return executeQuery(conn, query, params);
        } finally {
            DbPool.quietClose(conn);
        }
    }
    
    /**
     * Executes the specified query using a connection from the connection pool.
     * 
     * @throws ServiceException if the query cannot be executed or an error
     * occurs while retrieving results
     */
    public static DbResults executeQuery(String query, Object param)
    throws ServiceException {
        Object[] params = { param };
        return executeQuery(query, params);
    }
    
    /**
     * Executes the specified query using a connection from the connection pool.
     * 
     * @throws ServiceException if the query cannot be executed or an error
     * occurs while retrieving results
     */
    public static DbResults executeQuery(String query)
    throws ServiceException {
        return executeQuery(query, null);
    }
    
    /**
     * Executes the specified update using the specified connection.
     * 
     * @return the number of rows affected
     * @throws ServiceException if the update cannot be executed
     */
    public static int executeUpdate(Connection conn, String sql, Object[] params)
    throws ServiceException {
        PreparedStatement stmt = null;
        int numRows = 0;
        
        try {
            stmt = conn.prepareStatement(sql);
            
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            numRows = stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL: '" + sql + "'";
            throw ServiceException.FAILURE(message, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
        
        return numRows;
    }
    
    /**
     * Executes the specified update using the specified connection.
     * 
     * @return the number of rows affected
     * @throws ServiceException if the update cannot be executed
     */
    public static int executeUpdate(Connection conn, String sql, Object param)
    throws ServiceException {
        Object[] params = { param };
        return executeUpdate(conn, sql, params);
    }
    
    /**
     * Executes the specified update using the specified connection.
     * 
     * @return the number of rows affected
     * @throws ServiceException if the update cannot be executed
     */
    public static int executeUpdate(Connection conn, String sql)
    throws ServiceException {
        return executeUpdate(conn, sql, null);
    }
    
    /**
     * Executes the specified update using a connection from the connection pool.
     * 
     * @return the number of rows affected
     * @throws ServiceException if the update cannot be executed
     */
    public static int executeUpdate(String sql, Object[] params)
    throws ServiceException {
        Connection conn = DbPool.getConnection();
        try {
            int numRows = executeUpdate(conn, sql, params);
            conn.commit();
            return numRows;
        } finally {
            DbPool.quietClose(conn);
        }
    }
    
    /**
     * Executes the specified update using a connection from the connection pool.
     * 
     * @return the number of rows affected
     * @throws ServiceException if the update cannot be executed
     */
    public static int executeUpdate(String sql, Object param)
    throws ServiceException {
        Object[] params = { param };
        return executeUpdate(sql, params);
    }
    
    public static int executeUpdate(String sql)
    throws ServiceException {
        return executeUpdate(sql, null);
    }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>, which
     * contains as many question marks as the number of elements in the
     * given array.  Used for generating SQL IN clauses.
     */
    public static String suitableNumberOfVariables(byte[] array)        { return DbUtil.suitableNumberOfVariables(array.length); }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>, which
     * contains as many question marks as the number of elements in the
     * given array.  Used for generating SQL IN clauses.
     */
    public static String suitableNumberOfVariables(short[] array)         { return DbUtil.suitableNumberOfVariables(array.length); }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>, which
     * contains as many question marks as the number of elements in the
     * given array.  Used for generating SQL IN clauses.
     */
    public static String suitableNumberOfVariables(int[] array)         { return DbUtil.suitableNumberOfVariables(array.length); }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>, which
     * contains as many question marks as the number of elements in the
     * given array.  Used for generating SQL IN clauses.
     */
    public static String suitableNumberOfVariables(Object[] array)      { return DbUtil.suitableNumberOfVariables(array.length); }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>, which
     * contains as many question marks as the number of elements in the
     * given array.  Used for generating SQL IN clauses.
     */
    public static String suitableNumberOfVariables(Collection<?> c)        { return DbUtil.suitableNumberOfVariables(c.size()); }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>.
     * Used for generating SQL IN clauses.
     * @param size the number of question marks
     */
    public static String suitableNumberOfVariables(int size) {
        StringBuilder sb = new StringBuilder(" (");
        for (int i = 0; i < size; i++)
            sb.append(i == 0 ? "?" : ", ?");
        return sb.append(")").toString();
    }
    
    /**
     * Executes all SQL statements in the specified SQL script.  Statements are
     * separated by semicolons.
     * 
     * @param conn the database connection to use for executing the SQL statements
     * @param scriptReader the source of the SQL script file. The reader is closed
     * when this method returns.
     */
    public static void executeScript(DbPool.Connection conn, Reader scriptReader)
    throws IOException, ServiceException, SQLException {
        PreparedStatement stmt = null;
        String[] statements = parseScript(scriptReader);

        try {
            for (String sql : statements) {
                if (sql.length() == 0)
                    continue;
                stmt = conn.prepareStatement(sql);
                stmt.execute();
                stmt.close();
            }
            conn.commit();
        } catch (SQLException e) {
            DbPool.quietRollback(conn);
            throw e;
        } 
    }
    
    private static String[] addScript(String s1[], String s2[]) {
        if (s1 == null)
            return s2;
        
        String temp[] = new String[s1.length + s2.length];
        
        System.arraycopy(s1, 0, temp, 0, s1.length);
        System.arraycopy(s2, 0, temp, s1.length, s2.length);
        return temp;
    }

    /**
     * Parses a SQL script into separate SQL statements, separated by semicolons.
     * Removes comments that begin with <code>--</code> or <code>#</code>.
     */
    public static String[] parseScript(Reader scriptReader) throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader br = new BufferedReader(scriptReader);
        String line;
        String ret[] = null;
        
        while ((line = br.readLine()) != null) {
            line = removeComments(line);
            if (line.startsWith(".")) {
                // SQLite does not suport read statements so emulate them
                if (line.endsWith(";"))
                    line = line.substring(0, line.length() - 1);
                if (line.startsWith(".read ")) {
                    String sql;
                    
                    line = line.substring(".read".length());
                    line = line.trim();
                    line = line.replace("\"", "");
                    sql = new String(ByteUtil.getContent(new File(line)));
                    ret = addScript(ret, parseScript(new StringReader(sql)));
                }
                continue;
            }
            buf.append(line);
            line = line.trim();
            if (line.length() == 0) {
                // ignore comments or blank lines
                continue;
            }
            buf.append('\n');
        }
        br.close();
        if (buf.length() != 0 || ret == null)
            ret = addScript(ret, buf.toString().split("\\s*" +
                Db.getInstance().scriptCommandDelimiter() + "\\s*"));
        for (int i = 0; i < ret.length; i++) {
            if (ret[i].startsWith("END")) {
                ret[i - 1] += ";\n" + ret[i];
                ret[i] = "";
            }
        }
        return ret;
    }

    /**
     * Removes trailing comments.  Comments begin with <code>--</code> or <code>#</code>.
     * @return <code>sqlLine</code> without trailing comments
     */
    public static String removeComments(String sqlLine) {
        // remove comments that start with "#" or "--"
        int commentIndex = sqlLine.indexOf("--");
        int hashIndex = sqlLine.indexOf('#');
        if (hashIndex >= 0) {
            if (commentIndex >= 0) {
                commentIndex = Math.min(commentIndex, hashIndex);
            } else {
                commentIndex = hashIndex;
            }
        }
        if (commentIndex != -1) {
            sqlLine = sqlLine.substring(0, commentIndex);
        }
        return sqlLine;
    }

    /**
     * Returns a string that optimizes SQL WHERE IN clauses to an operator when
     * the parm list is small. A single IN can prevent some DBs like sqlite from
     * choosing proper indexes and a small # uses more CPU resources
     */
    public static String whereIn(String column, boolean in, int size) {
        if (size == 1) {
            return column + (in ? " = ?" : " <> ?");
        } else if (size <= 3) {
            StringBuffer sb = new StringBuffer("(");
            
            do {
                sb.append(column).append((in ? " = ?" : " <> ?"));
                if (size > 1) {
                    if (in)
                        // col IN(A,B,C) --> (col=A OR col=B OR col=C)
                        sb.append(" OR ");
                    else
                        // col NOT IN(A,B,C) --> (col<>A AND col<>B AND col<>C)
                        sb.append(" AND ");
                }
            } while (--size > 0);
            sb.append(')');
            return sb.toString();
        }
        return column + (in ? " IN" : " NOT IN") + suitableNumberOfVariables(size);
    }

    public static String whereIn(String column, int size) {
        return whereIn(column, true, size);
    }
    
    public static String whereNotIn(String column, int size) {
        return whereIn(column, false, size);
    }
}
