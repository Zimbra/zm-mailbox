/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeServiceException;

/**
 * volume table and current_volumes table.
 *
 * @since 2005. 6. 9.
 * @author jhahm
 */
public final class DbVolume {

    private static final String CN_ID = "id";
    private static final String CN_TYPE = "type";
    private static final String CN_NAME = "name";
    private static final String CN_PATH = "path";
    private static final String CN_FILE_BITS = "file_bits";
    private static final String CN_FILE_GROUP_BITS = "file_group_bits";
    private static final String CN_MAILBOX_BITS = "mailbox_bits";
    private static final String CN_MAILBOX_GROUP_BITS = "mailbox_group_bits";
    private static final String CN_COMPRESS_BLOBS = "compress_blobs";
    private static final String CN_COMPRESSION_THRESHOLD = "compression_threshold";

    public static synchronized Volume create(DbConnection conn, Volume volume) throws ServiceException {
        short nextId = volume.getId();
        if (nextId == Volume.ID_AUTO_INCREMENT) {
            nextId = getNextVolumeID(conn);
        }
        if (nextId <= 0 || nextId > Volume.ID_MAX) {
            throw VolumeServiceException.ID_OUT_OF_RANGE(nextId);
        }
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO volume (id, type, name, path, mailbox_group_bits, " +
                    "mailbox_bits, file_group_bits, file_bits, compress_blobs, compression_threshold) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int pos = 1;
            stmt.setShort(pos++, nextId);
            stmt.setShort(pos++, volume.getType());
            stmt.setString(pos++, volume.getName());
            stmt.setString(pos++, volume.getRootPath());
            stmt.setShort(pos++, volume.getMboxGroupBits());
            stmt.setShort(pos++, volume.getMboxBits());
            stmt.setShort(pos++, volume.getFileGroupBits());
            stmt.setShort(pos++, volume.getFileBits());
            stmt.setBoolean(pos++, volume.isCompressBlobs());
            stmt.setLong(pos++, volume.getCompressionThreshold());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw VolumeServiceException.ALREADY_EXISTS(nextId, volume.getName(), volume.getRootPath(), e);
            } else {
                throw ServiceException.FAILURE("inserting new volume " + nextId, e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
        return get(conn, nextId);
    }

    public static Volume update(DbConnection conn, Volume volume) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE volume SET type = ?, name = ?, path = ?, " +
                    "mailbox_group_bits = ?, mailbox_bits = ?, file_group_bits = ?, file_bits = ?, " +
                    "compress_blobs = ?, compression_threshold = ? WHERE id = ?");
            int pos = 1;
            stmt.setShort(pos++, volume.getType());
            stmt.setString(pos++, volume.getName());
            stmt.setString(pos++, volume.getRootPath());
            stmt.setShort(pos++, volume.getMboxGroupBits());
            stmt.setShort(pos++, volume.getMboxBits());
            stmt.setShort(pos++, volume.getFileGroupBits());
            stmt.setShort(pos++, volume.getFileBits());
            stmt.setBoolean(pos++, volume.isCompressBlobs());
            stmt.setLong(pos++, volume.getCompressionThreshold());
            stmt.setShort(pos++, volume.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw VolumeServiceException.ALREADY_EXISTS(volume.getId(), volume.getName(), volume.getRootPath(), e);
            } else {
                throw ServiceException.FAILURE("updating volume " + volume.getId(), e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
        return get(conn, volume.getId());
    }

    /**
     * delete the specified volume entry.
     * @param conn
     * @param id name of volume to delete
     * @return true if it existed and was deleted
     * @throws SQLException
     */
    public static boolean delete(DbConnection conn, short id) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM volume WHERE id=?");
            stmt.setShort(1, id);
            int num = stmt.executeUpdate();
            return num == 1;
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.FOREIGN_KEY_CHILD_EXISTS))
                throw VolumeServiceException.CANNOT_DELETE_VOLUME_IN_USE(id, e);
            else
                throw ServiceException.FAILURE("deleting volume entry: " + id, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static short getNextVolumeID(DbConnection conn) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
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
     * Get the specified volume entry.
     */
    public static Volume get(DbConnection conn, short id) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM volume WHERE id = ?");
            stmt.setShort(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return constructVolume(rs);
            } else {
                throw VolumeServiceException.NO_SUCH_VOLUME(id);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting volume entry: " + id, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Return all {@link Volume} entries in a map, where ID is the key and a {@link Volume} object is the value.
     */
    public static Map<Short, Volume> getAll(DbConnection conn) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<Short, Volume> result = new HashMap<Short, Volume>();
        try {
            stmt = conn.prepareStatement("SELECT * FROM volume");
            rs = stmt.executeQuery();
            while (rs.next()) {
                Volume v = constructVolume(rs);
                result.put(v.getId(), v);
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
        public short msgVolId = Volume.ID_NONE;
        public short secondaryMsgVolId = Volume.ID_NONE;
        public short indexVolId = Volume.ID_NONE;
    }

    public static CurrentVolumes getCurrentVolumes(DbConnection conn) throws ServiceException {
        CurrentVolumes currVols = new CurrentVolumes();
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

    public static void updateCurrentVolume(DbConnection conn, short type, short id) throws ServiceException {
        String col;
        switch (type) {
            case Volume.TYPE_MESSAGE:
                col = "message_volume_id";
                break;
            case Volume.TYPE_MESSAGE_SECONDARY:
                col = "secondary_message_volume_id";
                break;
            case Volume.TYPE_INDEX:
                col = "index_volume_id";
                break;
            default:
                throw new IllegalArgumentException("invalid volume type: " + type);
        }
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE current_volumes SET " + col + " = ?");
            if (id >= 0) {
                stmt.setShort(1, id);
            } else {
                stmt.setNull(1, Types.TINYINT);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating current volume", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static Volume constructVolume(ResultSet rs) throws SQLException, VolumeServiceException {
        return Volume.builder().setId(rs.getShort(CN_ID)).setType(rs.getShort(CN_TYPE)).setName(rs.getString(CN_NAME))
                .setPath(Volume.getAbsolutePath(rs.getString(CN_PATH)), false)
                .setMboxGroupBits(rs.getShort(CN_MAILBOX_GROUP_BITS)).setMboxBit(rs.getShort(CN_MAILBOX_BITS))
                .setFileGroupBits(rs.getShort(CN_FILE_GROUP_BITS)).setFileBits(rs.getShort(CN_FILE_BITS))
                .setCompressBlobs(rs.getBoolean(CN_COMPRESS_BLOBS))
                .setCompressionThreshold(rs.getLong(CN_COMPRESSION_THRESHOLD)).build();
    }
}
