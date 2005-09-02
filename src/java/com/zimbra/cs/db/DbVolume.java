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

/*
 * Created on 2005. 6. 9.
 *
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.store.VolumeServiceException;

/**
 * @author jhahm
 *
 * volume table and current_volumes table
 */
public class DbVolume {

    private static final String CN_ID = "id";
    private static final String CN_TYPE = "type";
    private static final String CN_NAME = "name";
    private static final String CN_PATH = "path";
    private static final String CN_FILE_BITS = "file_bits";
    private static final String CN_FILE_GROUP_BITS = "file_group_bits";
    private static final String CN_MAILBOX_BITS = "mailbox_bits";
    private static final String CN_MAILBOX_GROUP_BITS = "mailbox_group_bits";

    public static synchronized Volume create(Connection conn, short id,
                                             short type, String name, String path,
                                             short mboxGroupBits, short mboxBits,
                                             short fileGroupBits, short fileBits)
    throws ServiceException {
        short nextId = id;
        if (nextId == Volume.ID_AUTO_INCREMENT)
            nextId = getNextVolumeID(conn);
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                    "INSERT INTO volume " +
                    "(id, type, name, path, " +
                    "mailbox_group_bits, mailbox_bits, " +
                    "file_group_bits, file_bits) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            int pos = 1;
            stmt.setShort(pos++, nextId);
            stmt.setShort(pos++, type);
            stmt.setString(pos++, name);
            stmt.setString(pos++, path);
            stmt.setShort(pos++, mboxGroupBits);
            stmt.setShort(pos++, mboxBits);
            stmt.setShort(pos++, fileGroupBits);
            stmt.setShort(pos++, fileBits);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == Db.Error.DUPLICATE_ROW)
                throw VolumeServiceException.ALREADY_EXISTS(nextId, e);
            else
                throw ServiceException.FAILURE("inserting new volume " + nextId, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
        return get(conn, nextId);
    }

    public static Volume update(Connection conn, short id,
                                short type, String name, String path,
                                short mboxGroupBits, short mboxBits,
                                short fileGroupBits, short fileBits)
    throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                    "UPDATE volume SET " +
                    "type=?, name=?, path=?, " +
                    "mailbox_group_bits=?, mailbox_bits=?, " +
                    "file_group_bits=?, file_bits=? " +
                    "WHERE id=?");
            int pos = 1;
            stmt.setShort(pos++, type);
            stmt.setString(pos++, name);
            stmt.setString(pos++, path);
            stmt.setShort(pos++, mboxGroupBits);
            stmt.setShort(pos++, mboxBits);
            stmt.setShort(pos++, fileGroupBits);
            stmt.setShort(pos++, fileBits);
            stmt.setShort(pos++, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating volume " + id, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
        return get(conn, id);
    }

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

    private static short getNextVolumeID(Connection conn) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Volume result = null;
        try {
            stmt = conn.prepareStatement("SELECT MAX(id) FROM volume");
            rs = stmt.executeQuery();
            if (rs.next()) {
                short id = rs.getShort(1);
                return ++id;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting max volume ID", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return 1;
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

    /**
     * Return volumes on which a mailbox has blobs.
     * @param conn
     * @param mailboxId
     * @return
     * @throws ServiceException
     */
    public static List /*<Volume>*/ getVolumesForMailbox(Connection conn, int mailboxId) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List result = new ArrayList();
        try {
            stmt = conn.prepareStatement("SELECT * FROM volume WHERE id in " +
                    "(SELECT DISTINCT volume_id FROM " + DbMailItem.getMailItemTableName(mailboxId) + ")");
            rs = stmt.executeQuery();
            while (rs.next()) {
                Volume v = constructVolume(rs);
                result.add(v);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting volume entries for mailbox " + mailboxId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        return result;
    }
    
    public static class CurrentVolumes {
    	public short msgVolId = Volume.ID_NONE;
        public short secondaryMsgVolId = Volume.ID_NONE;
        public short indexVolId = Volume.ID_NONE;
    }

    public static CurrentVolumes getCurrentVolumes(Connection conn) throws ServiceException {
        CurrentVolumes currVols = new CurrentVolumes();
        short msgVolume = Volume.ID_NONE;
        short indexVolume = Volume.ID_NONE;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT message_volume_id, secondary_message_volume_id, index_volume_id FROM current_volumes");
            rs = stmt.executeQuery();
            if (rs.next()) {
                currVols.msgVolId = rs.getShort(1);
                short s = rs.getShort(2);
                if (!rs.wasNull())
                    currVols.secondaryMsgVolId = s;
                currVols.indexVolId = rs.getShort(3);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting current volumes", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
        if (currVols.msgVolId != Volume.ID_NONE && currVols.indexVolId != Volume.ID_NONE)
            return currVols;
        else
            return null;
    }

    public static void updateCurrentVolume(Connection conn, short volType, short volumeId)
    throws ServiceException {
        String colName;
        if (volType == Volume.TYPE_MESSAGE)
            colName = "message_volume_id";
        else if (volType == Volume.TYPE_MESSAGE_SECONDARY)
            colName = "secondary_message_volume_id";
        else
            colName = "index_volume_id";
        PreparedStatement stmt = null;
        try {
            String sql = "UPDATE current_volumes SET " + colName + " = ?";
            stmt = conn.prepareStatement(sql);
            if (volumeId >= 0)
                stmt.setShort(1, volumeId);
            else
                stmt.setNull(1, Types.TINYINT);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating current volume", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * @param rs
     * @return
     */
    private static Volume constructVolume(ResultSet rs) throws SQLException {
        short id = rs.getShort(CN_ID);
        short type = rs.getShort(CN_TYPE);
        String name = rs.getString(CN_NAME);
        String path = rs.getString(CN_PATH);
        short mboxGroupBits = rs.getShort(CN_MAILBOX_GROUP_BITS);
        short mboxBits = rs.getShort(CN_MAILBOX_BITS);
        short fileGroupBits = rs.getShort(CN_FILE_GROUP_BITS);
        short fileBits = rs.getShort(CN_FILE_BITS);
        Volume v = new Volume(id, type, name, path, mboxGroupBits, mboxBits, fileGroupBits, fileBits);
        return v;
    }
}
