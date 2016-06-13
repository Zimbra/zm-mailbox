/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool.DbConnection;

/**
 * @since 2005. 1. 26.
 */
public class DbStatus {

    private static Log mLog = LogFactory.getLog(DbStatus.class);

    public static boolean healthCheck() {
        boolean result = false;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("SELECT 'foo' AS STATUS");
            rs = stmt.executeQuery();
            if (rs.next())
                result = true;
        } catch (SQLException e) {
            mLog.warn("Database health check error", e);
        } catch (ServiceException e) {
            mLog.warn("Database health check error", e);
        } finally {
            try {
                DbPool.closeResults(rs);
            } catch (ServiceException e) {
                mLog.info("Ignoring error while closing database result set during health check", e);
            }
            try {
                DbPool.closeStatement(stmt);
            } catch (ServiceException e) {
                mLog.info("Ignoring error while closing database statement during health check", e);
            }
            if (conn != null)
                DbPool.quietClose(conn);
        }
        return result;
    }
}
