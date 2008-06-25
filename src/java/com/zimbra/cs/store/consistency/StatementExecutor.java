/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.store.consistency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatementExecutor {
    private final Connection conn;

    public StatementExecutor(Connection conn) {
        this.conn = conn;
    }

    public static interface ObjectMapper {
        public void mapRow(ResultSet rs) throws SQLException;
    }

    public Object query(String query) throws SQLException {
        return query(query, (Object[]) null);
    }

    public Object query(String query, Object[] args) throws SQLException {
        final Object[] ref = new Object[1];
        ObjectMapper mapper = new ObjectMapper() {
            public void mapRow(ResultSet rs) throws SQLException {
                ref[0] = rs.getObject(1);
            }
        };
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(query);
            stmt.setFetchSize(Integer.MIN_VALUE);
            for (int i = 1; args != null && i <= args.length; i++)
                stmt.setObject(i, args[i-1]);
            rs = stmt.executeQuery();
            while (rs.next())
                mapper.mapRow(rs);
        }
        finally {
            if (rs   != null) rs.close();
            if (stmt != null) stmt.close();
        }
        return ref[0];
    }

    public void query(String query, ObjectMapper mapper) throws SQLException {
        query(query, null, mapper);
    }
    public void query(String query, Object[] args, ObjectMapper mapper)
    throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(query);
            stmt.setFetchSize(Integer.MIN_VALUE);
            for (int i = 1; args != null && i <= args.length; i++)
                stmt.setObject(i, args[i-1]);
            rs = stmt.executeQuery();
            while (rs.next())
                mapper.mapRow(rs);
        }
        finally {
            if (rs   != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    public int update(String stmt) throws SQLException {
        return update(stmt, null);
    }
    public int update(String stmt, Object[] args) throws SQLException {
        PreparedStatement st = null;
        int updated = 0;
        try {
            st = conn.prepareStatement(stmt);
            for (int i = 1; args != null && i <= args.length; i++)
                st.setObject(i, args[i-1]);
            updated = st.executeUpdate();
        }
        finally {
            if (st != null) st.close();
        }
        return updated;
    }
}
