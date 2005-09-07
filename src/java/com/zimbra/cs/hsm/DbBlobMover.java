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

package com.zimbra.cs.hsm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

public class DbBlobMover {
    
    private static final String MAIL_ITEM_TYPES = MailItem.TYPE_MESSAGE + ", " +
        MailItem.TYPE_APPOINTMENT; 

    public static void alterVolume(Connection conn, Mailbox mbox,
                                   long timestamp, int newVolumeId)
    throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                "UPDATE " + DbMailItem.getMailItemTableName(mbox) + " " +
                "SET volume_id = ? " +
                "WHERE date < ? AND type IN (" + MAIL_ITEM_TYPES + ")");
            stmt.setInt(1, newVolumeId);
            stmt.setLong(2, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("altering volume", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static List /* <MovedItemInfo> */ getItemsBeforeTimestamp(Connection conn,
                                                                     Mailbox mbox,
                                                                     long timestamp,
                                                                     short excludeVolumeId)
    throws ServiceException {
        List messages = new ArrayList();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = conn.prepareStatement("SELECT id, volume_id, mod_content, blob_digest " +
                " FROM " + DbMailItem.getMailItemTableName(mbox) +
                " WHERE date < ? AND type IN (" + MAIL_ITEM_TYPES + ") AND volume_id != ?");
            stmt.setLong(1, timestamp);
            stmt.setShort(2, excludeVolumeId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                short volumeId = rs.getShort("volume_id");
                int revision = rs.getInt("mod_content");
                String blobDigest = rs.getString("blob_digest");
                MovedItemInfo info = new MovedItemInfo(id, volumeId, revision, blobDigest);
                messages.add(info);
            }
            
            return messages;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting messages before timestamp " + timestamp, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
}
