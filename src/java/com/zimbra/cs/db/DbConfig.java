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
 * Created on Apr 8, 2004
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool.Connection;

/**
 * @author schemers
 */
public class DbConfig {

    private static final String CN_NAME = "name";
    private static final String CN_VALUE = "value";
    private static final String CN_DESCRIPTION = "value";
    private static final String CN_MODIFIED = "modified";
    
    // these MUST be kept in sync with the db
    private static final int CI_NAME = 1;
    private static final int CI_VALUE = 2;
    private static final int CI_DESCRIPTION = 3;
    private static final int CI_MODIFIED = 4;
    
    /** database name */
    private String mDbName;
    
    /** database value */
    private String mDbValue;
    
    /** database description */
    private String mDbDescription;
    
    /** database modified */
    private Timestamp mDbModified;
    
    public static DbConfig set(Connection conn, String name, String value) throws ServiceException {
        Timestamp modified = new Timestamp(System.currentTimeMillis());

        PreparedStatement stmt = null;
        try {
            // Try update first
            stmt = conn.prepareStatement("UPDATE config SET value = ?, modified = ? WHERE name = ?");
            stmt.setString(1, value);
            stmt.setTimestamp(2, modified);
            stmt.setString(3, name);
            int numRows = stmt.executeUpdate();
            
            // If update didn't affect any rows, do an insert
            if (numRows == 0) {
                stmt = conn.prepareStatement("INSERT INTO config(name, value, modified) VALUES (?, ?, ?)");
                stmt.setString(1, name);
                stmt.setString(2, value);
                stmt.setTimestamp(3, modified);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing config entry: " + name, e);
        } finally {
            DbPool.closeStatement(stmt);
        }        

        DbConfig c = new DbConfig();
        c.mDbName = name;
        c.mDbValue = value;
        c.mDbModified = modified;
        return c;
    }

    /**
     * delete the specified config entry.
     * @param conn
     * @param name name of config item to delete
     * @return true if it existed and was deleted
     * @throws SQLException
     */
    public static boolean delete(Connection conn, String name) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM config WHERE name = ?");
            stmt.setString(1, name);
            int num = stmt.executeUpdate();
            return num == 1;
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("deleting config entry: " + name, e);
        } finally {
            DbPool.closeStatement(stmt);
        }        
    }

    /**
     * get the specified config entry
     * @param conn
     * @param name
     * @return
     * @throws SQLException
     */
    public static DbConfig get(Connection conn, String name) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DbConfig result = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM config WHERE name = ?");
            stmt.setString(1, name);
            rs = stmt.executeQuery();
            if (rs.next())
                return constructConfig(rs);
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("getting config entry: " + name, e);
        } finally {
        	DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }

    /**
     * Return all config items in a map, where name is the key and a 
     * Config object is the value.
     * @param conn
     * @param ts If not null return entries great than or equal to this Timestamp
     * @return Map containing config items.
     * @throws SQLException
     */
    public static Map<String, DbConfig> getAll(Connection conn, Timestamp ts) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        HashMap<String, DbConfig> result = new HashMap<String, DbConfig>();
        try {
            if (ts == null) {
                stmt = conn.prepareStatement("SELECT * FROM config");
            } else {
                stmt = conn.prepareStatement("SELECT * FROM config WHERE modified >= ?");
                stmt.setTimestamp(1, ts);
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                DbConfig c = constructConfig(rs);
                result.put(c.mDbName, c);
            }
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("getting all config entries", e);
        } finally {
        	DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }

    /**
     * @param rs
     * @return
     */
    private static DbConfig constructConfig(ResultSet rs) throws SQLException {
        DbConfig c = new DbConfig();
        c.mDbName = rs.getString(CI_NAME);
        c.mDbValue = rs.getString(CI_VALUE);
        c.mDbDescription = rs.getString(CI_DESCRIPTION);
        c.mDbModified = rs.getTimestamp(CI_MODIFIED);
        return c;
    }

    public String getName() {
        return mDbName;
    }
    
    public String getValue() {
        return mDbValue;
    }
    
    public Timestamp getModified() {
        return mDbModified;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("config: {");
        sb.append(CN_NAME).append(": ").append(mDbName).append(", ");
        sb.append(CN_VALUE).append(": ").append(mDbValue).append(", ");
        sb.append(CN_DESCRIPTION).append(": ").append(mDbDescription).append(", ");
        sb.append(CN_MODIFIED).append(": ").append(mDbModified);
        sb.append("}");
        return sb.toString();
    }
}
