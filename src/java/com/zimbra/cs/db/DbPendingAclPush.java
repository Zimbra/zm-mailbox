/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.ZimbraApplication;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class DbPendingAclPush {

    public static String TABLE_PENDING_ACL_PUSH = "pending_acl_push";

    private static boolean supported;

    static {
         supported = ZimbraApplication.getInstance().supports(DbPendingAclPush.class);
    }

    public static void queue(Mailbox mbox, int itemId) throws ServiceException {
        if (!supported)
            return;
        if (mbox == null)
            return;
        ZimbraLog.mailbox.debug("Queuing for ACL push - mailbox %s item %s", mbox.getId(), itemId);
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        boolean supportsReplace = Db.supports(Db.Capability.REPLACE_INTO);
        try {
            String command = supportsReplace ? "REPLACE" : "INSERT";
            stmt = conn.prepareStatement(
                    command + " INTO " + TABLE_PENDING_ACL_PUSH + " (mailbox_id, item_id, date) VALUES (?, ?, ?)");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, itemId);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(
                    "Unable to queue for ACL push - mailbox " + mbox.getId() + " item " + itemId, e);
        } finally {
            conn.closeQuietly(stmt);
        }

        // unit tests using hsqldb could potentially cause integrity constraint violation
        // because hsqldb does not support REPLACE command and if this method is called twice in
        // the same millisecond with the same (mailbox_id, item_id). Sleeping for 2ms would
        // take care of this.
        if (!supportsReplace) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static Multimap<Integer, Integer> getEntries(Date uptoTime) throws ServiceException {
        Multimap<Integer, Integer> mboxIdToItemIds = HashMultimap.create();
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ZimbraLog.misc.debug("Getting entries recorded before %s for ACL push", uptoTime);
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                    "SELECT mailbox_id, item_id FROM " + TABLE_PENDING_ACL_PUSH + " WHERE date < ?");
            stmt.setLong(1, uptoTime.getTime());
            rs = stmt.executeQuery();
            while (rs.next()) {
                mboxIdToItemIds.put(rs.getInt(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to get entries recorded before " + uptoTime + " for ACL push", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return mboxIdToItemIds;
    }

    public static void deleteEntries(Date uptoTime) throws ServiceException {
        ZimbraLog.misc.debug("Deleting entries for ACL push before %s", uptoTime);
        DbConnection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(
                    "DELETE FROM " + TABLE_PENDING_ACL_PUSH + " WHERE date < ?");
            stmt.setLong(1, uptoTime.getTime());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Unable to delete UID's", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
}
