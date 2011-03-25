/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.upgrade;

import com.google.common.base.Strings;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbMailAddress;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.index.SortBy;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.util.SyncToken;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Utility class to upgrade contacts.
 *
 * @author Andy Clark
 */
public final class MailboxUpgrade {

    private static final Map<Long,Byte> UPGRADE_TO_1_7_COLORS = new HashMap<Long,Byte>();
    private static final Map<Long,Byte> UPGRADE_TO_1_8_COLORS = UPGRADE_TO_1_7_COLORS;

    static {
        Map<Long,Byte> map = UPGRADE_TO_1_7_COLORS;
        // common colors
        map.put(0x1000000L,(byte)0); // none
        // original RGB colors
        map.put(0x10000ffL,(byte)1); // blue
        map.put(0x1008284L,(byte)2); // cyan
        map.put(0x1848284L,(byte)8); // gray
        map.put(0x1008200L,(byte)3); // green
        map.put(0x1ff8000L,(byte)9); // orange
        map.put(0x1840084L,(byte)4); // purple
        map.put(0x1ff0084L,(byte)7); // pink
        map.put(0x1ff0000L,(byte)5); // red
        map.put(0x1848200L,(byte)6); // yellow
        // newer RGB colors
        map.put(0x19EB6F5L,(byte)1); // blue
        map.put(0x1A4E6E6L,(byte)2); // cyan
        map.put(0x1D3D3D3L,(byte)8); // gray
        map.put(0x197C8B1L,(byte)3); // green
        map.put(0x1FDBC55L,(byte)9); // orange
        map.put(0x1FE9BD3L,(byte)7); // pink
        map.put(0x1BA86E5L,(byte)4); // purple
        map.put(0x1FC9696L,(byte)5); // red
        map.put(0x1FFF6B3L,(byte)6); // yellow
        // newest RGB colors
        map.put(0x10252d4L,(byte)1); // blue
        map.put(0x1008284L,(byte)2); // cyan
        map.put(0x1848284L,(byte)8); // gray
        map.put(0x12ca10bL,(byte)3); // green
        map.put(0x1f57802L,(byte)9); // orange
        map.put(0x1b027aeL,(byte)7); // pink
        map.put(0x1492ba1L,(byte)4); // purple
        map.put(0x1e51717L,(byte)5); // red
        map.put(0x1848200L,(byte)6); // yellow
    }

    /** This class can not be instantiated. */
    private MailboxUpgrade() {}

    /**
     * bug 41893: revert folder colors back to mapped value.
     */
    public static void upgradeTo1_7(Mailbox mbox) throws ServiceException {
        OperationContext octxt = new OperationContext(mbox);
        for (Folder folder : mbox.getFolderList(octxt, SortBy.NONE)) {
            MailItem.Color color = folder.getRgbColor();
            if (!color.hasMapping()) {
                Byte value = UPGRADE_TO_1_7_COLORS.get(color.getValue());
                if (value != null) {
                    MailItem.Color newcolor = new MailItem.Color(value);
                    mbox.setColor(octxt, new int[] { folder.getId() }, folder.getType(), newcolor);
                }
            }
        }
    }

    /**
     * bug 41850: revert tag colors back to mapped value.
     */
    public static void upgradeTo1_8(Mailbox mbox) throws ServiceException {
        OperationContext octxt = new OperationContext(mbox);
        for (Tag tag : mbox.getTagList(octxt)) {
            MailItem.Color color = tag.getRgbColor();
            if (!color.hasMapping()) {
                Byte value = UPGRADE_TO_1_8_COLORS.get(color.getValue());
                if (value != null) {
                    MailItem.Color newcolor = new MailItem.Color(value);
                    mbox.setColor(octxt, new int[] { tag.getId() }, tag.getType(), newcolor);
                }
            }
        }
    }

    public static void upgradeTo2_0(Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));
        migrateHighestIndexed(mbox);
        migrateMailAddress(mbox);
    }

    private static void migrateHighestIndexed(Mailbox mbox) throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // fetch highest_indexed
            String highestIndexed = null;
            stmt = conn.prepareStatement("SELECT highest_indexed FROM " +
                    DbMailbox.qualifyZimbraTableName(mbox, "mailbox") + " WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {
                highestIndexed = rs.getString(1);
            }
            rs.close();
            stmt.close();

            if (Strings.isNullOrEmpty(highestIndexed)) {
                return;
            }

            SyncToken token;
            try {
                token = new SyncToken(highestIndexed);
            } catch (ServiceException e) {
                return;
            }

            // update index_id where mod_content/mod_metadata > highest_indexed
            stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                    " SET index_id = 0 WHERE " + DbMailItem.IN_THIS_MAILBOX_AND +
                    "mod_content > ? AND mod_metadata > ? AND index_id IS NOT NULL");
            int pos = DbMailItem.setMailboxId(stmt, mbox, 1);
            stmt.setInt(pos++, token.getChangeId());
            stmt.setInt(pos++, token.getChangeId());
            stmt.executeUpdate();
            stmt.close();

            // clear highest_indexed
            stmt = conn.prepareStatement("UPDATE " + DbMailbox.qualifyZimbraTableName(mbox, "mailbox") +
                    " SET highest_indexed = NULL WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.executeUpdate();
            stmt.close();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw ServiceException.FAILURE("Failed to migrate highest_indexed", e);
        } finally {
            conn.closeQuietly();
        }
    }

    private static void migrateMailAddress(Mailbox mbox) throws ServiceException {
        String[] emailFields = Contact.getEmailFields(mbox.getAccount());
        DbConnection conn = DbPool.getConnection(mbox);
        PreparedStatement stmt = null;
        PreparedStatement inner = null;
        ResultSet rs = null;
        try {
            // clear all sender_id in mail_item
            stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                    " SET sender_id = NULL WHERE mailbox_id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.executeQuery();
            stmt.close();
            // delete all rows in mail_address
            DbMailAddress.delete(conn, mbox);

            // extract email addresses from all contacts, and put them into mail_address
            stmt = conn.prepareStatement("SELECT metadata FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE mailbox_id = ? AND type = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setByte(2, MailItem.Type.CONTACT.toByte());
            rs = stmt.executeQuery();
            while (rs.next()) {
                Metadata metadata = new Metadata(rs.getString(1));
                Metadata fields = metadata.getMap("fld");
                Set<String> emails = new HashSet<String>(); // merge duplicates
                for (String name : emailFields) {
                    String addr = fields.get(name);
                    if (!Strings.isNullOrEmpty(addr)) {
                        emails.add(addr.trim().toLowerCase());
                    }
                }
                for (String addr : emails) {
                    int senderId = DbMailAddress.getId(conn, mbox, addr);
                    if (senderId < 0) {
                        DbMailAddress.save(conn, mbox, addr, 1);
                    } else {
                        DbMailAddress.incCount(conn, mbox, senderId);
                    }
                }
            }
            rs.close();
            stmt.close();

            // extract sender addresses from all messages, put them into mail_address, and set sender_id
            stmt = conn.prepareStatement("SELECT id, sender FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE mailbox_id = ? AND type = ? AND sender IS NOT NULL");
            stmt.setInt(1, mbox.getId());
            stmt.setByte(2, MailItem.Type.MESSAGE.toByte());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int itemId = rs.getInt(1);
                String sender = rs.getString(2);
                for (InternetAddress iaddr : InternetAddress.parseHeader(sender)) {
                    String addr = iaddr.getAddress();
                    if (Strings.isNullOrEmpty(addr)) {
                        addr = addr.trim().toLowerCase();
                        int senderId = DbMailAddress.getId(conn, mbox, addr);
                        if (senderId < 0) {
                            senderId = DbMailAddress.save(conn, mbox, addr, 0);
                        }
                        inner = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                                " SET sender_id = ? WHERE mailbox_id = ? AND id = ?");
                        stmt.setInt(1, senderId);
                        stmt.setInt(2, mbox.getId());
                        stmt.setInt(3, itemId);
                        inner.executeUpdate();
                        inner.close();
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw ServiceException.FAILURE("Failed to migrate mail_address", e);
        } finally {
            conn.closeQuietly(inner);
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
            conn.closeQuietly();
        }
    }

}
