/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple in-memory interface to a JDBC <code>ResultSet</code>.  This
 * implementation reads the entire result set into memory.  This approach avoids database
 * handles being left open too long.  The side effect is a larger
 * memory footprint.  Code that deals with large result
 * sets should use the JDBC <code>ResultSet</code> class directly.<p>
 * 
 * Conveniences:
 * <ul>
 *   <li>try/catch blocks that handle <code>SQLException</code> are not required.</li>
 *   <li>Data can be accessed either by calling {@link #next()} or directly by
 *       the row number.</li>
 *   <li>Calling {@link #next()} is not necessary for one-row results.</li>
 *   <li>Result set size is available via the {@link #size()} method.</li> 
 * </ul>
 * 
 * All API's that reference row and column indexes are 1-based, to match the behavior
 * of the JDBC <code>ResultSet</code> class.
 * 
 * @see com.zimbra.cs.db.DbUtil
 *  
 * @author bburtin
 */
public class DbResults {

    private List<Object[]> mData = new ArrayList<Object[]>();
    private Map<String, Integer> mColumnIndexes = new HashMap<String, Integer>();
    private int mRowNum = 0;
    
    /**
     * Constructs a <code>DbResults</code> object from the specified JDBC
     * <code>ResultSet</code>.  Reads the entire data set into memory
     * and closes the <code>ResultSet</code>.
     * 
     * @throws SQLException
     */
    public DbResults(ResultSet resultSet) throws SQLException {
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet cannot be null");
        }

        boolean isFirst = true;
        int numCols = 0;

        while (resultSet.next()) {
            if (isFirst) {
                // Initialize the column map
                ResultSetMetaData md = resultSet.getMetaData();
                numCols = md.getColumnCount();
                for (int i = 1; i <= numCols; i++) {
                    mColumnIndexes.put(md.getColumnName(i), new Integer(i));
                }
                isFirst = false;
            }

            Object[] row = new Object[numCols];
            for (int i = 0; i < numCols; i++) {
                row[i] = resultSet.getObject(i+1);
            }
            mData.add(row);
        }
        
        resultSet.close();
    }

    /**
     * Returns the number of rows in the result set.
     * 
     * @return the number of rows
     */
    public int size() {
        return mData.size();
    }
    
    /**
     * Iterates to the next row in the result set.
     * 
     * @return <code>true</code> if another row is available.
     */
    public boolean next() {
        if (mRowNum == mData.size()) {
            return false;
        }
        mRowNum++;
        return true;
    }
    
    /**
     * Returns the row of data at the specified index.
     */
    public Object[] getRow(int row) {
        return mData.get(row - 1);
    }
    
    ////////// Null checks //////////

    /**
     * Returns <code>true</code> if the cell at the specified row
     * and column is <code>null</code>.
     */
    public boolean isNull(int row, int col) {
        Object[] rowData = getRow(row);
        return (rowData[col - 1] == null);
    }
    
    /**
     * Returns <code>true</code> if the cell at the specified
     * column for the current row is <code>null</code>.
     */
    public boolean isNull(int col) {
        return isNull(getRowNum(), col);
    }

    /**
     * Returns <code>true</code> if the cell at the specified row
     * and column is <code>null</code>.
     */
    public boolean isNull(int row, String colName) {
        return isNull(row, getIndex(colName));
    }
    
    /**
     * Returns <code>true</code> if the cell at the specified
     * column for the current row is <code>null</code>.
     */
    public boolean isNull(String colName) {
        return isNull(getRowNum(), colName);
    }
    
    ////////// Object accessors //////////

    /**
     * Returns the <code>Object</code> at the specified
     * row and column.
     */
    public Object getObject(int row, int col) {
        Object[] rowData = getRow(row);
        return rowData[col - 1];
    }

    /**
     * Returns the <code>Object</code> at the specified column in the
     * current row.
     */
    public Object getObject(int col) {
        return getObject(getRowNum(), col);
    }
    
    /**
     * Returns the <code>Object</code> at the specified
     * row and column.
     */
    public Object getObject(int row, String colName) {
        return getObject(row, getIndex(colName));
    }
    
    /**
     * Returns the <code>Object</code> at the specified column in the
     * current row.
     */
    public Object getObject(String colName) {
        return getObject(getRowNum(), getIndex(colName));
    }
    
    ////////// String accessors //////////
    
    /**
     * Returns the <code>String</code> at the specified
     * row and column.
     */
    public String getString(int row, int col) {
        return (String) getObject(row, col);
    }
    
    /**
     * Returns the <code>String</code> at the specified column in the
     * current row.
     */
    public String getString(int col) {
        return getString(getRowNum(), col);
    }
    
    /**
     * Returns the <code>String</code> at the specified
     * row and column.
     */
    public String getString(int row, String colName) {
        return getString(row, getIndex(colName));
    }
    
    /**
     * Returns the <code>String</code> at the specified column in the
     * current row.
     */
    public String getString(String colName) {
        return getString(getRowNum(), colName);
    }
    
    ////////// int accessors //////////

    /**
     * Returns the integer at the specified
     * row and column.
     */
    public int getInt(int row, int col) {
        Number i = (Number)getObject(row, col);
        if (i == null) {
            throw new IllegalStateException("null value at (" + row + ", " + col + ")");
        }
        return i.intValue();
    }
    
    /**
     * Returns the integer at the specified column in the
     * current row.
     */
    public int getInt(int col) {
        return getInt(getRowNum(), col);
    }
    
    /**
     * Returns the integer at the specified
     * row and column.
     */
    public int getInt(int row, String colName) {
        return getInt(row, getIndex(colName));
    }
    
    /**
     * Returns the integer at the specified column in the
     * current row.
     */
    public int getInt(String colName) {
        return getInt(getRowNum(), colName);
    }
    
    ////////// boolean accessors //////////

    /**
     * Returns the boolean at the specified
     * row and column.
     */
    public boolean getBoolean(int row, int col) {
        Object o = getObject(row, col);
        if (o == null) {
            throw new IllegalStateException("null value at (" + row + ", " + col + ")");
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        int i = ((Number) o).intValue();
        return (i == 0) ? false : true;
    }
    
    /**
     * Returns the boolean at the specified column in the
     * current row.
     */
    public boolean getBoolean(int col) {
        return getBoolean(getRowNum(), col);
    }
    
    /**
     * Returns the boolean at the specified
     * row and column.
     */
    public boolean getBoolean(int row, String colName) {
        return getBoolean(row, getIndex(colName));
    }
    
    /**
     * Returns the boolean at the specified column in the
     * current row.
     */
    public boolean getBoolean(String colName) {
        return getBoolean(getRowNum(), colName);
    }
    
    ////////// Private methods  //////////

    private int getIndex(String colName) {
        Integer i = (Integer)mColumnIndexes.get(colName);
        if (i == null) {
            throw new IllegalArgumentException("Column '" + colName + "' does not exist");
        }
        return i.intValue();
    }

    private int getRowNum() {
        if (mRowNum < 1) {
            mRowNum = 1;
        }
        return mRowNum;
    }
}
