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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;

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
        if (b) {
            return INTEGER_TRUE;
        }
        return INTEGER_FALSE;
    }

    /**
     * MySQL Connector/J JDBC driver has a bug when fetching an unsigned
     * TINYINT value.  It is internally handled as a Java byte and all
     * numbers are returned as signed value between -128 and 127.
     * Fix it up by adding 256 to negative values, yielding the correct
     * unsigned 8-bit range of 0 to 255.
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    public static short getUnsignedTinyIntAsShort(ResultSet rs, int columnIndex)
    throws SQLException {
        short v = rs.getShort(columnIndex);
        if (v < 0)
            v += 256;
        return v;
    }

    public static short getUnsignedTinyIntAsShort(ResultSet rs, String columnName)
    throws SQLException {
        short v = rs.getShort(columnName);
        if (v < 0)
            v += 256;
        return v;
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
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
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

    public static final int IN_CLAUSE_BATCH_SIZE = 100;

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
    public static String suitableNumberOfVariables(Collection c)        { return DbUtil.suitableNumberOfVariables(c.size()); }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>, which
     * contains as many question marks as the number of elements in the
     * given <code>Collection</code>.  Used for generating SQL IN clauses.
     */
    public static String suitableNumberOfVariables(MailItem.Array arr)  { return DbUtil.suitableNumberOfVariables(arr.length); }

    /**
     * Returns a string with the form <code>(?, ?, ?, ...)</code>.
     * Used for generating SQL IN clauses.
     * @param size the number of question marks
     */
    public static String suitableNumberOfVariables(int size) {
        StringBuffer sb = new StringBuffer(" (");
        for (int i = 0; i < size; i++)
            sb.append(i == 0 ? "?" : ", ?");
        return sb.append(") ").toString();
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
        StringBuffer buf = new StringBuffer();
        BufferedReader br = new BufferedReader(scriptReader);
        String line;
        while ((line = br.readLine()) != null) {
            // remove comments
            int hash = line.indexOf('#');
            if (hash != -1) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.length() == 0) {
                // ignore comments or blank lines
                continue;
            }
            buf.append(line).append('\n');
        }
        br.close();
        String script = buf.toString();
        String[] sqls = script.split("\\s*;\\s*");
        PreparedStatement stmt = null;
        try {
            for (int i = 0; i < sqls.length; i++) {
                stmt = conn.prepareStatement(sqls[i]);
                stmt.execute();
                stmt.close();
            }
            conn.commit();
        } catch (SQLException e) {
            DbPool.quietRollback(conn);
            throw e;
        } 
    }
}
