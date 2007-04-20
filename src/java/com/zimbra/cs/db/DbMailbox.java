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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.util.Config;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author kchen
 */
public class DbMailbox {

    public static final int CI_ID;
    public static final int CI_GROUP_ID;
    public static final int CI_ACCOUNT_ID;
    public static final int CI_INDEX_VOLUME_ID;
    public static final int CI_ITEM_ID_CHECKPOINT;
    public static final int CI_CONTACT_COUNT;
    public static final int CI_SIZE_CHECKPOINT;
    public static final int CI_CHANGE_CHECKPOINT;
    public static final int CI_TRACKING_SYNC;
    public static final int CI_TRACKING_IMAP;
    public static final int CI_COMMENT;

    static {
        int pos = 1;
        // Order must match the order of column definition in zimbra.mailbox
        // table in db.sql script.
        CI_ID = pos++;
        CI_GROUP_ID = pos++;
        CI_ACCOUNT_ID = pos++;
        CI_INDEX_VOLUME_ID = pos++;
        CI_ITEM_ID_CHECKPOINT = pos++;
        CI_CONTACT_COUNT = pos++;
        CI_SIZE_CHECKPOINT = pos++;
        CI_CHANGE_CHECKPOINT = pos++;
        CI_TRACKING_SYNC = pos++;
        CI_TRACKING_IMAP = pos++;
        CI_COMMENT = pos++;
    }

    public static final int CI_METADATA_MAILBOX_ID = 1;
    public static final int CI_METADATA_SECTION    = 2;
    public static final int CI_METADATA_METADATA    = 3;

    private static String DB_PREFIX_MAILBOX_GROUP = "mboxgroup";

    private static int MAX_COMMENT_LENGTH = 255;

    public static class NewMboxId {
        public int id;
        public int groupId;
    }

    public synchronized static NewMboxId createMailbox(Connection conn, int mailboxId, String accountId, String comment) throws ServiceException {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH)
            comment = comment.substring(0, MAX_COMMENT_LENGTH);

        NewMboxId ret = new NewMboxId();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean explicitId = (mailboxId != Mailbox.ID_AUTO_INCREMENT);
            String idSource = (explicitId ? "?" : "next_mailbox_id");
            String limitClause = Db.supports(Db.Capability.LIMIT_CLAUSE) ? " ORDER BY index_volume_id LIMIT 1" : "";
            stmt = conn.prepareStatement("INSERT INTO mailbox" +
                    "(account_id, id, group_id, index_volume_id, item_id_checkpoint, comment)" +
                    " SELECT ?, " + idSource + ", 0, index_volume_id, " + (Mailbox.FIRST_USER_ID - 1) + ", ?" +
                    " FROM current_volumes" + limitClause);
            int attr = 1;
            stmt.setString(attr++, accountId);
            if (explicitId)
                stmt.setInt(attr++, mailboxId);
            stmt.setString(attr++, comment);
            stmt.executeUpdate();
            stmt.close();
            stmt = null;

            stmt = conn.prepareStatement("UPDATE current_volumes" +
                    " SET next_mailbox_id = CASE WHEN ? > next_mailbox_id THEN ? + 1 ELSE next_mailbox_id + 1 END");
            stmt.setInt(1, mailboxId);
            stmt.setInt(2, mailboxId);
            stmt.executeUpdate();
            stmt.close();
            stmt = null;

            if (explicitId) {
                ret.id = mailboxId;
                ret.groupId = getMailboxGroupId(ret.id);
                return ret;
            }

            stmt = conn.prepareStatement("SELECT id FROM mailbox WHERE " + Db.equalsSTRING("account_id"));
            stmt.setString(1, accountId.toUpperCase());
            rs = stmt.executeQuery();
            if (rs.next())
                ret.id = rs.getInt(1);
            else
                throw ServiceException.FAILURE("determining new account id for mailbox: " + accountId, null);
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;

            ret.groupId = getMailboxGroupId(ret.id);
            stmt = conn.prepareStatement("UPDATE mailbox SET group_id = ? WHERE id = ?");
            stmt.setInt(1, ret.groupId);
            stmt.setInt(2, ret.id);
            stmt.executeUpdate();
            stmt.close();
            stmt = null;

            return ret;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing new mailbox row for account " + accountId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /** Create a database for the specified mailbox.
     * 
     * @throws ServiceException if the database creation fails */
    public static void createMailboxDatabase(Connection conn, int mailboxId, int groupId)
    throws ServiceException {
        ZimbraLog.mailbox.debug("createMailboxDatabase(" + mailboxId + ")");

        File file = Config.getPathRelativeToZimbraHome("db/create_database.sql");

        PreparedStatement stmt = null;
        try {
            if (databaseExists(conn, mailboxId, groupId))
                return;

            // Create the new database
            String dbName = getDatabaseName(mailboxId, groupId);
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
        }
    }

    private static String[] sTables;

    static {
        // Tables are listed in order of creation.  dropMailboxFromGroup() drops them
        // in reverse order.
        sTables = new String[] {
            DbMailItem.TABLE_MAIL_ITEM,
            DbMailItem.TABLE_OPEN_CONVERSATION,
            DbMailItem.TABLE_APPOINTMENT,
            DbMailItem.TABLE_TOMBSTONE,
            DbPop3Message.TABLE_POP3_MESSAGE
        };
    }

    private static boolean databaseExists(Connection conn, int mailboxId, int groupId)
    throws ServiceException {
        for (int i = 0; i < sTables.length; i++) {
            String table = getDatabaseName(mailboxId, groupId) + "." + sTables[i];

            String sql = "SELECT * FROM " + table + (Db.supports(Db.Capability.LIMIT_CLAUSE) ? " LIMIT 1" : "");
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
                rs.next();
            } catch (SQLException e) {
                return false;
            } finally {
                DbPool.closeResults(rs);
                DbPool.closeStatement(stmt);
            }
        }
        return true;
    }

    private static void dropMailboxFromGroup(Connection conn, Mailbox mbox)
    throws ServiceException {
        int mailboxId = mbox.getId();
        int groupId = mbox.getSchemaGroupId();        
        ZimbraLog.mailbox.info("Clearing contents of mailbox " + mailboxId + ", group " + groupId);

        if (conn == null)
            conn = mbox.getOperationConnection();
        if (Db.supports(Db.Capability.DISABLE_CONSTRAINT_CHECK))
            conn.disableForeignKeyConstraints();

        try {
            String dbname = getDatabaseName(mailboxId, groupId);

            // Delete in reverse order.
            for (int i = sTables.length - 1; i >= 0; i--) {
                String table = dbname + "." + sTables[i];
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement("DELETE FROM " + table + " WHERE mailbox_id = " + mailboxId);
                    stmt.executeUpdate();
                } finally {
                    DbPool.closeStatement(stmt);
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("dropMailboxFromGroup(" + mailboxId + ")", e);
        } finally {
            try {
                if (Db.supports(Db.Capability.DISABLE_CONSTRAINT_CHECK))
                    conn.enableForeignKeyConstraints();
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("Error enabling foreign key constraints during mailbox deletion", e);
                // Don't rethrow to avoid masking any exception from DELETE statements.
            }
        }
    }

    public static void clearMailboxContent(Mailbox mbox) throws ServiceException {
        clearMailboxContent(null, mbox);
    }

    public static void clearMailboxContent(Connection conn, Mailbox mbox) throws ServiceException {
        dropMailboxFromGroup(conn, mbox);
    }

    public static void clearMailboxContactCount(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox SET contact_count = NULL WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("clearing contact count for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateMailboxStats(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox" +
                    " SET item_id_checkpoint = ?, contact_count = ?, change_checkpoint = ?, size_checkpoint = ?" +
                    " WHERE id = ?");
            stmt.setInt(1, mbox.getLastItemId());
            stmt.setInt(2, mbox.getContactCount());
            stmt.setInt(3, mbox.getLastChangeID());
            stmt.setLong(4, mbox.getSize());
            stmt.setInt(5, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating item ID highwater mark for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void startTrackingSync(Mailbox mbox) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox SET tracking_sync = ? WHERE id = ? AND tracking_sync <= 0");
            stmt.setInt(1, mbox.getLastChangeID());
            stmt.setInt(2, mbox.getId());
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
            stmt = conn.prepareStatement("UPDATE mailbox SET tracking_imap = 1 WHERE id = ?");
            stmt.setInt(1, mbox.getId());
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
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM mailbox_metadata" +
                    " WHERE mailbox_id = ? AND " + Db.equalsSTRING("section"));
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, section.toUpperCase());
            stmt.executeUpdate();
            stmt.close();

            if (config != null) {
                stmt = conn.prepareStatement("INSERT INTO mailbox_metadata (mailbox_id, section, metadata)" +
                        " VALUES (?, ?, ?)");
                stmt.setInt(1, mbox.getId());
                stmt.setString(2, section);
                stmt.setString(3, config.toString());
                stmt.executeUpdate();
            }
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
                    "SELECT account_id, group_id," +
                    " size_checkpoint, contact_count, item_id_checkpoint, change_checkpoint, tracking_sync," +
                    " tracking_imap, index_volume_id " +
                    "FROM mailbox WHERE id = ?");
            stmt.setInt(1, mailboxId);
            rs = stmt.executeQuery();

            if (!rs.next())
                return null;
            int pos = 1;
            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            mbd.id            = mailboxId;
            mbd.accountId     = rs.getString(pos++);
            mbd.schemaGroupId = rs.getInt(pos++);
            mbd.size          = rs.getLong(pos++);
            if (rs.wasNull())
                mbd.size = -1;
            mbd.contacts      = rs.getInt(pos++);
            if (rs.wasNull())
                mbd.contacts = -1;
            mbd.lastItemId    = rs.getInt(pos++) + ITEM_CHECKPOINT_INCREMENT - 1;
            mbd.lastChangeId  = rs.getInt(pos++) + CHANGE_CHECKPOINT_INCREMENT - 1;
            mbd.trackSync     = rs.getInt(pos++);
            mbd.trackImap     = rs.getBoolean(pos++);
            mbd.indexVolumeId = rs.getShort(pos++);

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

    public static int getMailboxGroupId(int mailboxId) {
        int groups = DebugConfig.numMailboxGroups;
        // -1 / +1 operations are done so that the group
        // id is never 0.
        return (mailboxId - 1) % groups + 1;
    }

    /** @return the name of the database that contains tables for the specified <code>mailboxId</code>. */
    public static String getDatabaseName(int mailboxId, int groupId) {
        return DB_PREFIX_MAILBOX_GROUP + groupId;
    }
    public static String getDatabaseName(Mailbox mbox) {
        int id = mbox.getId();
        int gid = mbox.getSchemaGroupId();
        return getDatabaseName(id, gid);
    }

    /**
     * Deletes the row for the specified mailbox from the <code>mailbox</code> table.
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

    static Set<Long> getDistinctTagsets(Connection conn, Mailbox mbox) throws ServiceException {
        Set<Long> tagsets = new HashSet<Long>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT DISTINCT(tags) FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE mailbox_id = ?");
            stmt.setInt(1, mbox.getId());
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

    static Set<Long> getDistinctFlagsets(Connection conn, Mailbox mbox) throws ServiceException {
        Set<Long> flagsets = new HashSet<Long>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT DISTINCT(flags) FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE mailbox_id = ?");
            stmt.setInt(1, mbox.getId());
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
