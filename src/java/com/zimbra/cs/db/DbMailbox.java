/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 28, 2004
 */
package com.zimbra.cs.db;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author kchen
 */
public class DbMailbox {

    private static String DATABASE_PREFIX = "mailbox";

    public synchronized static int createMailbox(Connection conn, int mailboxId, String accountId, String comment) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean explicitId = (mailboxId != Mailbox.ID_AUTO_INCREMENT);
            String idSource = (explicitId ? "?" : "next_mailbox_id");
            stmt = conn.prepareStatement("INSERT INTO mailbox(account_id, id, index_volume_id, item_id_checkpoint, comment)" +
                                         " SELECT ?, " + idSource + ", index_volume_id, " + (Mailbox.FIRST_USER_ID - 1) + ", ?" +
                                         " FROM current_volumes ORDER BY index_volume_id LIMIT 1");
            int attr = 1;
            stmt.setString(attr++, accountId);
            if (explicitId)
                stmt.setInt(attr++, mailboxId);
            stmt.setString(attr++, comment);
            stmt.executeUpdate();
            stmt.close();

            stmt = conn.prepareStatement("UPDATE current_volumes SET next_mailbox_id = IF(? > next_mailbox_id, ?, next_mailbox_id) + 1");
            stmt.setInt(1, mailboxId);
            stmt.setInt(2, mailboxId);
            stmt.executeUpdate();
            stmt.close();

            if (explicitId)
                return mailboxId;

            stmt = conn.prepareStatement("SELECT id FROM mailbox WHERE account_id = ?");
            stmt.setString(1, accountId);
            rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1);
            throw ServiceException.FAILURE("determining new account id for mailbox: " + accountId, null);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing new mailbox row for account " + accountId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Create a database for the specified mailbox.  The DDL runs on its own connection
     * so that it doesn't interfere with transactions on the main connection.
     * 
     * @throws ServiceException if the database creation fails
     */
    public static void createMailboxDatabase(int mailboxId)
    throws ServiceException {
        ZimbraLog.mailbox.debug("createMailboxDatabase(" + mailboxId + ")");

        File file = Config.getPathRelativeToZimbraHome("db/create_database.sql");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();

            // Create the new database
            String dbName = getDatabaseName(mailboxId);
            ZimbraLog.mailbox.info("Creating database " + dbName);

            String template = new String(ByteUtil.getContent(file));
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("DATABASE_NAME", dbName);
            String script = StringUtil.fillTemplate(template, vars);
            DbUtil.executeScript(conn, new StringReader(script));

        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to read SQL statements from " + file.getPath(), e);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("createMailboxDatabase(" + mailboxId + ")", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    /**
     * Drop the database for the specified mailbox.  The DDL runs on its own connection
     * so that it doesn't interfere with transactions on the main connection.
     * 
     * @throws ServiceException if the database creation fails
     */
    public static void dropMailboxDatabase(int mailboxId)
    throws ServiceException {
        ZimbraLog.mailbox.debug("dropMailboxDatabase(" + mailboxId + ")");

        String dbName = getDatabaseName(mailboxId);
        if (!dbName.startsWith(DATABASE_PREFIX)) {
            // Additional safeguard to make sure we don't drop the wrong database
            throw ServiceException.FAILURE("Attempted to drop database " + dbName, null);
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DbPool.getConnection();

            // Create the new database
            ZimbraLog.mailbox.info("Dropping database " + dbName);

            stmt = conn.prepareStatement("DROP DATABASE IF EXISTS " + dbName);
            stmt.execute(); 
            stmt.close();

            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("dropMailboxDatabase(" + mailboxId + ")", e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static void updateHighestItem(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox SET item_id_checkpoint = ?, size_checkpoint = ? WHERE id = ?");
            stmt.setInt(1, mbox.getLastItemId());
            stmt.setLong(2, mbox.getSize());
            stmt.setInt(3, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating item ID highwater mark for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateHighestChange(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox SET change_checkpoint = ?, size_checkpoint = ? WHERE id = ?");
            stmt.setInt(1, mbox.getLastChangeID());
            stmt.setLong(2, mbox.getSize());
            stmt.setInt(3, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("checkpointing change sequence number for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void startTrackingSync(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox SET tracking_sync = ?, size_checkpoint = ? " +
                    "WHERE id = ? AND tracking_sync <= 0");
            stmt.setInt(1, mbox.getLastChangeID());
            stmt.setLong(2, mbox.getSize());
            stmt.setInt(3, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on sync tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void startTrackingImap(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox SET tracking_imap = 1, size_checkpoint = ? WHERE id = ?");
            stmt.setLong(1, mbox.getSize());
            stmt.setInt(2, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on imap tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static String getConfig(Mailbox mbox, String section) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT metadata FROM mailbox_metadata WHERE mailbox_id = ? AND section = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, section);
            rs = stmt.executeQuery();
            if (rs.next())
                return rs.getString(1);
            return null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting metadata section '" + section + "' in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateConfig(Mailbox mbox, String section, Metadata config) throws ServiceException {
        boolean unset = config == null;
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            if (unset)
                stmt = conn.prepareStatement("DELETE FROM mailbox_metadata WHERE mailbox_id = ? AND section = ?");
            else
                stmt = conn.prepareStatement("INSERT INTO mailbox_metadata (mailbox_id, section, metadata) VALUES (?, ?, ?)" +
                        " ON DUPLICATE KEY UPDATE metadata = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, section);
            if (!unset) {
                String serialized = config.toString();
                stmt.setString(3, serialized);
                stmt.setString(4, serialized);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("setting metadata section '" + section + "' in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /** Returns the zimbra IDs and mailbox IDs for all mailboxes on the
     *  system.  Note that mailboxes are created lazily, so there may be
     *  accounts homed on this system for whom there is is not yet a mailbox
     *  and hence are not included in the returned <code>Map</code>.
     * 
     * @param conn  An open database connection.
     * @return A <code>Map</code> whose keys are zimbra IDs and whose values
     *         are the corresponding numeric mailbox IDs.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - an error occurred while accessing
     *        the database; a SQLException is encapsulated</ul> */
    public static Map<String, Integer> getMailboxes(Connection conn) throws ServiceException {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT account_id, id FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next())
                result.put(rs.getString(1), rs.getInt(2));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching mailboxes", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /** Returns the zimbra IDs and approximate sizes for all mailboxes on
     *  the system.  Note that mailboxes are created lazily, so there may be
     *  accounts homed on this system for whom there is is not yet a mailbox
     *  and hence are not included in the returned <code>Map</code>.  Sizes
     *  are checkpointed frequently, but there is no guarantee that the
     *  approximate sizes are currently accurate.
     * 
     * @param conn  An open database connection.
     * @return A <code>Map</code> whose keys are zimbra IDs and whose values
     *         are the corresponding approximate mailbox sizes.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - an error occurred while accessing
     *        the database; a SQLException is encapsulated</ul> */
    public static Map<String, Long> getMailboxSizes(Connection conn) throws ServiceException {
        HashMap<String, Long> result = new HashMap<String, Long>();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT account_id, size_checkpoint FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next())
                result.put(rs.getString(1), rs.getLong(2));
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching mailboxes", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static final int CHANGE_CHECKPOINT_INCREMENT = 100;
    public static final int ITEM_CHECKPOINT_INCREMENT   = 20;

    public static Mailbox.MailboxData getMailboxStats(Connection conn, int mailboxId) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT account_id, item_id_checkpoint, size_checkpoint, change_checkpoint, tracking_sync," +
                    " tracking_imap, index_volume_id " +
                    "FROM mailbox mb WHERE mb.id = ?");
            stmt.setInt(1, mailboxId);
            rs = stmt.executeQuery();

            if (!rs.next())
                return null;
            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            mbd.id            = mailboxId;
            mbd.accountId     = rs.getString(1);
            mbd.size          = rs.getLong(3);
            mbd.contacts      = 0;
            mbd.lastItemId    = rs.getInt(2) + ITEM_CHECKPOINT_INCREMENT - 1;
            mbd.lastChangeId  = rs.getInt(4) + CHANGE_CHECKPOINT_INCREMENT - 1;
            mbd.trackSync     = rs.getInt(5);
            mbd.trackImap     = rs.getBoolean(6);
            mbd.indexVolumeId = rs.getShort(7);

            // round lastItemId and lastChangeId up so that they get written on the next change
            long rounding = mbd.lastItemId % ITEM_CHECKPOINT_INCREMENT;
            if (rounding != ITEM_CHECKPOINT_INCREMENT - 1)
                mbd.lastItemId -= rounding + 1;
            rounding = mbd.lastChangeId % CHANGE_CHECKPOINT_INCREMENT;
            if (rounding != CHANGE_CHECKPOINT_INCREMENT - 1)
                mbd.lastChangeId -= rounding + 1;

            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("SELECT section FROM mailbox_metadata WHERE mailbox_id = ?");
            stmt.setInt(1, mailboxId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (mbd.configKeys == null)
                    mbd.configKeys = new HashSet<String>();
                mbd.configKeys.add(rs.getString(1));
            }

            return mbd;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching stats on mailbox " + mailboxId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Fills in the "data" arg with volume information on the mailbox.
     * @param conn
     * @param data must have id field set to valid mailbox id
     * @throws ServiceException
     */
    public static void getMailboxVolumeInfo(Connection conn, Mailbox.MailboxData data) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT index_volume_id FROM mailbox WHERE id = ?");
            stmt.setInt(1, data.id);
            rs = stmt.executeQuery();
            if (!rs.next())
                throw MailServiceException.NO_SUCH_MBOX(data.id);
            data.indexVolumeId = rs.getShort(1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting mailbox volume info", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /** @return the name of the database that contains tables for the specified <code>mailboxId</code>. */
    public static String getDatabaseName(int mailboxId) {
        return DATABASE_PREFIX + mailboxId;
    }

    /**
     * Deletes the row for the specified mailbox from the <code>mailbox</code> table.
     * To completely clean up the mailbox data, a call to this method must
     * be followed by a call to {@link DbMailbox#dropMailboxDatabase(int)}.
     *  
     * @throws ServiceException if the database operation failed
     */
    public static void deleteMailbox(Connection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            // remove entry from mailbox table
            stmt = conn.prepareStatement("DELETE FROM mailbox WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    static Set<Long> getDistinctTagsets(Connection conn, int mailboxId) throws ServiceException {
        Set<Long> tagsets = new HashSet<Long>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT DISTINCT tags " +
                "FROM " + DbMailItem.getMailItemTableName(mailboxId));
            rs = stmt.executeQuery();
            while (rs.next())
                tagsets.add(rs.getLong(1));
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct tagsets", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return tagsets;
    }

    static Set<Long> getDistinctFlagsets(Connection conn, int mailboxId) throws ServiceException {
        Set<Long> flagsets = new HashSet<Long>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT DISTINCT flags " +
                "FROM " + DbMailItem.getMailItemTableName(mailboxId));
            rs = stmt.executeQuery();
            while (rs.next())
                flagsets.add(rs.getLong(1));
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct flagsets", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return flagsets;
    }
}
