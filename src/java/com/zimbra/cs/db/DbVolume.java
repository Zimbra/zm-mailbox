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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 6. 9.
 *
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.Volume;

/**
 * @author jhahm
 *
 * volume table and current_volumes table
 */
public class DbVolume {

    private static final String CN_ID = "id";
    private static final String CN_NAME = "name";
    private static final String CN_PATH = "path";
    private static final String CN_FILE_BITS = "file_bits";
    private static final String CN_FILE_GROUP_BITS = "file_group_bits";
    private static final String CN_MAILBOX_BITS = "mailbox_bits";
    private static final String CN_MAILBOX_GROUP_BITS = "mailbox_group_bits";
    
    /**
     * delete the specified volume entry.
     * @param conn
     * @param id name of volume to delete
     * @return true if it existed and was deleted
     * @throws SQLException
     */
    public static boolean delete(Connection conn, short id) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM volume WHERE id=?");
            stmt.setShort(1, id);
            int num = stmt.executeUpdate();
            return num == 1;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting volume entry: " + id, e);
        } finally {
            DbPool.closeStatement(stmt);
        }        
    }

    /**
     * get the specified volume entry
     * @param conn
     * @param id
     * @return
     * @throws SQLException
     */
    public static Volume get(Connection conn, short id) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Volume result = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM volume WHERE id=?");
            stmt.setShort(1, id);
            rs = stmt.executeQuery();
            if (rs.next())
                return constructVolume(rs);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting volume entry: " + id, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }

    /**
     * Return all volume entries in a map, where id is the key and a 
     * Volume object is the value.
     * @param conn
     * @return Map containing volume entries
     * @throws SQLException
     */
    public static Map /*<Short,Volume>*/ getAll(Connection conn) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map result = new HashMap();
        try {
            stmt = conn.prepareStatement("SELECT * FROM volume");
            rs = stmt.executeQuery();
            while (rs.next()) {
                Volume v = constructVolume(rs);
                result.put(new Short(v.getId()), v);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting all volume entries", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }

    public static class CurrentVolumes {
    	public short msgVolId = -1;
        public short indexVolId = -1;
    }

    public static CurrentVolumes getCurrentVolumes(Connection conn) throws ServiceException {
        CurrentVolumes currVols = new CurrentVolumes();
        short msgVolume = -1;
        short indexVolume = -1;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT message_volume_id, index_volume_id FROM current_volumes");
            rs = stmt.executeQuery();
            if (rs.next()) {
                currVols.msgVolId = rs.getShort(1);
                currVols.indexVolId = rs.getShort(2);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting current volumes", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        if (currVols.msgVolId != -1 && currVols.indexVolId != -1)
            return currVols;
        else
            return null;
    }

    /**
     * @param rs
     * @return
     */
    private static Volume constructVolume(ResultSet rs) throws SQLException {
        short id = rs.getShort(CN_ID);
        String name = rs.getString(CN_NAME);
        String path = rs.getString(CN_PATH);
        int mboxGroupBits = rs.getInt(CN_MAILBOX_GROUP_BITS);
        int mboxBits = rs.getInt(CN_MAILBOX_BITS);
        int fileGroupBits = rs.getInt(CN_FILE_GROUP_BITS);
        int fileBits = rs.getInt(CN_FILE_BITS);
        Volume v = new Volume(id, name, path, mboxGroupBits, mboxBits, fileGroupBits, fileBits);
        return v;
    }
}
