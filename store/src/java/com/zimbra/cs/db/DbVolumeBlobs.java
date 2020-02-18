/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.MailboxBlob.MailboxBlobInfo;
import com.zimbra.cs.store.file.BlobReference;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.util.SpoolingCache;
import com.zimbra.cs.volume.Volume;


public final class DbVolumeBlobs {

    private static final String TB_VOLUME_BLOBS = "volume_blobs";
    private static final String CN_ID = "id";
    private static final String CN_VOLUME_ID = "volume_id";
    private static final String CN_MAILBOX_ID = "mailbox_id";
    private static final String CN_ITEM_ID = "item_id";
    private static final String CN_REVISION = "revision";
    private static final String CN_DIGEST = "blob_digest";
    private static final String CN_PROCESSED = "processed";

    private static final String SELECT_BLOB_REFS = "SELECT " + CN_ID + "," + CN_VOLUME_ID + "," + CN_MAILBOX_ID + "," + CN_ITEM_ID + "," + CN_REVISION + "," + CN_DIGEST + "," + CN_PROCESSED + " FROM " + TB_VOLUME_BLOBS + " WHERE ";
    private static final String DELETE_BLOB_REFS = "DELETE FROM " + TB_VOLUME_BLOBS + " WHERE ";

    public static void addBlobReference(DbConnection conn, Mailbox mbox, Volume vol, MailItem item) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + TB_VOLUME_BLOBS + " (" + CN_VOLUME_ID + ", " + CN_MAILBOX_ID + ", " +
                    CN_ITEM_ID + ", " + CN_REVISION + "," + CN_DIGEST + "," + CN_PROCESSED + ") " +
                    "VALUES (?, ?, ?, ?, ?, ?)");
            int pos = 1;
            stmt.setShort(pos++, vol.getId());
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, item.getId());
            stmt.setInt(pos++, item.getSavedSequence());
            stmt.setString(pos++, item.getDigest());
            stmt.setBoolean(pos++, false);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to insert new blob reference for item " + item, e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void addBlobReference(DbConnection conn, MailboxBlobInfo info) throws ServiceException {
        short volId = Short.valueOf(info.locator);
        String path = FileBlobStore.getBlobPath(info.mailboxId, info.itemId, info.revision, volId);
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + TB_VOLUME_BLOBS + " (" + CN_VOLUME_ID + ", " + CN_MAILBOX_ID + ", " +
                            CN_ITEM_ID + ", " + CN_REVISION + "," + CN_DIGEST + "," + CN_PROCESSED + ") " +
                            "VALUES (?, ?, ?, ?, ?, ?)");
            int pos = 1;
            stmt.setShort(pos++, volId);
            stmt.setInt(pos++, info.mailboxId);
            stmt.setInt(pos++, info.itemId);
            stmt.setInt(pos++, info.revision);
            stmt.setString(pos++, info.digest);
            stmt.setBoolean(pos++, false);
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(path, e);
            } else {
                throw ServiceException.FAILURE("unable to insert new blob reference for path " + path, e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateProcessed(DbConnection conn, long id, boolean processed) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + TB_VOLUME_BLOBS + " SET " + CN_PROCESSED + " = ? WHERE " + CN_ID + " = ?");
            int pos = 1;
            stmt.setBoolean(pos++, processed);
            stmt.setLong(pos++, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to update processed", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static List<BlobReference> getBlobReferences(DbConnection conn, String digest, Volume volume) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(SELECT_BLOB_REFS + CN_DIGEST + " = ? AND " + CN_VOLUME_ID + " = ? ORDER BY "+ CN_DIGEST + " ASC");
            int pos = 1;
            stmt.setString(pos++, digest);
            stmt.setShort(pos++, volume.getId());
            rs = stmt.executeQuery();
            return fillBlobReferences(rs);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to query blob references", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static List<BlobReference> fillBlobReferences(ResultSet rs) throws SQLException {
        List<BlobReference> blobs = new ArrayList<BlobReference>();
        while (rs.next()) {
            BlobReference blob = new BlobReference();
            blob.setId(rs.getLong(1));
            blob.setVolumeId(rs.getShort(2));
            blob.setMailboxId(rs.getInt(3));
            blob.setItemId(rs.getInt(4));
            blob.setRevision(rs.getInt(5));
            blob.setDigest(rs.getString(6));
            blob.setProcessed(rs.getBoolean(7));
            blobs.add(blob);
        }
        return blobs;
    }
    
    private static SpoolingCache<String> fillBlobDigests(ResultSet rs) throws SQLException, IOException {
        SpoolingCache<String> digests = new SpoolingCache<String>();
        while (rs.next()) {
            digests.add(rs.getString(1));
        }
        return digests;
    }

    public static List<BlobReference> getBlobReferences(DbConnection conn, Volume vol) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(SELECT_BLOB_REFS + CN_VOLUME_ID + " = ? ORDER BY "+ CN_DIGEST + " ASC");
            int pos = 1;
            stmt.setShort(pos++, vol.getId());
            rs = stmt.executeQuery();
            return fillBlobReferences(rs);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to query blob references", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
    
    public static SpoolingCache<String> getUniqueDigests(DbConnection conn, Volume vol) throws ServiceException, IOException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT DISTINCT " + CN_DIGEST + " FROM " + TB_VOLUME_BLOBS + 
                                         " WHERE " + CN_VOLUME_ID +  " = ? AND " + CN_PROCESSED + " = ? ");
            int pos = 1;
            stmt.setShort(pos++, vol.getId());
            stmt.setBoolean(pos++, false);
            rs = stmt.executeQuery();
            return fillBlobDigests(rs);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to query blob references", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void deleteBlobRef(DbConnection conn, long id) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(DELETE_BLOB_REFS + CN_ID + " = ?");
            int pos = 1;
            stmt.setLong(pos++, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to delete blob references", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void deleteBlobRef(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(DELETE_BLOB_REFS + CN_MAILBOX_ID + " = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to delete blob references", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void deleteBlobRef(DbConnection conn, Volume vol) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(DELETE_BLOB_REFS + CN_VOLUME_ID + " = ?");
            int pos = 1;
            stmt.setShort(pos++, vol.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to delete blob references", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void deleteAllBlobRef(DbConnection conn) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("TRUNCATE TABLE " + TB_VOLUME_BLOBS);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("unable to delete blob references", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

}
