/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.index.SortBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.util.SyncToken;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

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

    /**
     * Migrate {@code mailbox.highest_indexed}.
     */
    public static void upgradeTo1_11(Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        Connection conn = DbPool.getConnection(mbox);
        try {
            // fetch highest_indexed
            String highestIndexed = null;
            PreparedStatement stmt = conn.prepareStatement("SELECT highest_indexed FROM " +
                    DbMailbox.qualifyZimbraTableName(mbox, "mailbox") + " WHERE id = ?");
            try {
                stmt.setInt(1, mbox.getId());
                ResultSet rs = stmt.executeQuery();
                try {
                    if (rs.next()) {
                        highestIndexed = rs.getString(1);
                    }
                } finally {
                    DbPool.closeResults(rs);
                }
            } finally {
                DbPool.closeStatement(stmt);
            }

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
            try {
                int pos = DbMailItem.setMailboxId(stmt, mbox, 1);
                stmt.setInt(pos++, token.getChangeId());
                stmt.setInt(pos++, token.getChangeId());
                stmt.executeUpdate();
            } finally {
                DbPool.closeStatement(stmt);
            }

            // clear highest_indexed
            stmt = conn.prepareStatement("UPDATE " + DbMailbox.qualifyZimbraTableName(mbox, "mailbox") +
                    " SET highest_indexed = NULL WHERE id = ?");
            try {
                stmt.setInt(1, mbox.getId());
                stmt.executeUpdate();
            } finally {
                DbPool.closeStatement(stmt);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw ServiceException.FAILURE("Failed to migrate highest_indexed", e);
        } finally {
            DbPool.quietClose(conn);
        }
    }

}
