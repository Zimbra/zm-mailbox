/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool.DbConnection;

/**
 * current_sessions table.
 *
 * @since 2013. 3. 1.
 * @author smadiraju
 */
public final class DbSession {

    private static final String CN_ID = "id";
    private static final String CN_SERVER_ID = "server_id";

    public static void create(DbConnection conn, int id, String server_id) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO current_sessions (id, server_id) " +
                    "VALUES (?, ?)");
            int pos = 1;
            stmt.setInt(pos++, id);
            stmt.setString(pos++, server_id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
            	//ignore, duplication session for the same mailbox id and server_id.
            } else {
            	throw ServiceException.FAILURE("inserting new session " + id +"," + "server_id", e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * delete the specified id entry and all its server ids.
     * @param conn
     * @param id name of session to delete
     * @return true if it existed and was deleted
     * @throws SQLException
     */
    public static boolean delete(DbConnection conn, int id, String serverId) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM current_sessions WHERE id=? AND server_id = ?");
            int pos = 1;
            stmt.setInt(pos++, id);
            stmt.setString(pos++, serverId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("deleting session entry: " + id, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * delete the specified id entry and all its server ids.
     * @param conn
     * @param id name of session to delete
     * @return true if it existed and was deleted
     * @throws SQLException
     */
    public static boolean deleteAll(DbConnection conn) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM current_sessions");
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("deleting session entries: " , e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Get the specified session entry.
     */
    public static List<String> get(DbConnection conn, int id) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<String> list = new ArrayList<String>();
        try {
            stmt = conn.prepareStatement("SELECT server_id FROM current_sessions WHERE id = ?");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(CN_SERVER_ID));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting mailbox entry: " + id, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return list;
    }

	public static void deleteSessions(DbConnection conn, String serverId) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM current_sessions where server_id = ?");
            stmt.setString(1, serverId);
            stmt.executeUpdate();
        } catch (SQLException e) {
        	throw ServiceException.FAILURE("deleting session entries for server_id: " + serverId , e);
        } finally {
            DbPool.closeStatement(stmt);
        }
	}
}
