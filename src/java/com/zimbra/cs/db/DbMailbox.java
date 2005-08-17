/*
 * Created on Oct 28, 2004
 */
package com.zimbra.cs.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.util.StringUtil;

/**
 * @author kchen
 */
public class DbMailbox {

    private static String DATABASE_PREFIX = "mailbox";

    public static int createMailbox(Connection conn, int mailboxId, String accountId, String comment) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            boolean explicitId = (mailboxId != Mailbox.ID_AUTO_INCREMENT);
            String idSource = (explicitId ? "?" : "next_mailbox_id");
            stmt = conn.prepareStatement(
                    "INSERT INTO mailbox(account_id, id, message_volume_id, index_volume_id, item_id_checkpoint, comment)" +
                    " SELECT ?, " + idSource + ", message_volume_id, index_volume_id, " + (Mailbox.FIRST_USER_ID - 1) + ", ?" +
                    " FROM current_volumes ORDER BY message_volume_id, index_volume_id LIMIT 1");
            int attr = 1;
            stmt.setString(attr++, accountId);
            if (explicitId)
                stmt.setInt(attr++, mailboxId);
            stmt.setString(attr++, comment);
            stmt.executeUpdate();
            stmt.close();

            stmt = conn.prepareStatement(
                    "UPDATE current_volumes SET next_mailbox_id = LAST_INSERT_ID(IF(? > next_mailbox_id, ?, next_mailbox_id)) + 1");
            stmt.setInt(1, mailboxId);
            stmt.setInt(2, mailboxId);
            stmt.executeUpdate();
            stmt.close();
//            return (num == 1);

            if (explicitId)
                return mailboxId;

            stmt = conn.prepareStatement("SELECT LAST_INSERT_ID()");
            rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1);
            throw ServiceException.FAILURE("determining new account id for mailbox: " + accountId, null);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing new mailbox for account " + accountId, e);
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

        Connection conn = DbPool.getConnection();
        PreparedStatement stmt = null;

        try {
            // Create the new database
            String dbName = getDatabaseName(mailboxId);
            ZimbraLog.mailbox.info("Creating database " + dbName);

            File file = Config.getPathRelativeToZimbraHome("db/create_database.sql");
            Map vars = new HashMap();
            vars.put("DATABASE_NAME", dbName);

            String script;
            try {
                script = StringUtil.fillTemplate(new FileReader(file), vars);
            } catch (FileNotFoundException ex) {
                throw ServiceException.FAILURE("Unable to read " + file.getPath(), ex);
            }

            String[] sql = script.split(";");
            if (sql == null || sql.length == 0)
                throw ServiceException.FAILURE("No SQL statements found in " + file.getPath(), null);

            for (int i = 0; i < sql.length - 1; i++) {
                stmt = conn.prepareStatement(sql[i]);
                stmt.execute();
                stmt.close();
            }

            conn.commit();
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
        Connection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        String dbName = getDatabaseName(mailboxId);
        if (!dbName.startsWith(DATABASE_PREFIX)) {
            // Additional safeguard to make sure we don't drop the wrong database
            throw ServiceException.FAILURE("Attempted to drop database " + dbName, null);
        }
        
        try {
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
            stmt = conn.prepareStatement("UPDATE mailbox SET tracking_sync = 1, size_checkpoint = ? WHERE id = ?");
            stmt.setLong(1, mbox.getSize());
            stmt.setInt(2, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on sync tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateConfig(Mailbox mbox, Metadata config) throws ServiceException {
        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE mailbox SET config = ? WHERE id = ?");
            if (config == null)
                stmt.setNull(1, Types.VARCHAR);
            else
                stmt.setString(1, config.toString());
            stmt.setInt(2, mbox.getId());
            int num = stmt.executeUpdate();
            assert(num == 1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on sync tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static Map getMailboxes(Connection conn) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        HashMap result = new HashMap();
        try {
            stmt = conn.prepareStatement("SELECT id, account_id FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next())
                result.put(rs.getString(2), new Integer(rs.getInt(1)));
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
                    "SELECT account_id, item_id_checkpoint, size_checkpoint, change_checkpoint, tracking_sync, " +
                    "message_volume_id, index_volume_id, config " +
                    "FROM mailbox mb " +
                    "WHERE mb.id = ?");
            stmt.setInt(1, mailboxId);
            rs = stmt.executeQuery();

            if (!rs.next())
                throw MailServiceException.NO_SUCH_MBOX(mailboxId);
            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            mbd.id           = mailboxId;
            mbd.accountId    = rs.getString(1);
            mbd.size         = rs.getLong(3);
            mbd.contacts     = 0;
            mbd.lastItemId   = rs.getInt(2) + ITEM_CHECKPOINT_INCREMENT - 1;
            mbd.lastChangeId = rs.getInt(4) + CHANGE_CHECKPOINT_INCREMENT - 1;
            mbd.trackSync    = rs.getBoolean(5);
            mbd.messageVolumeId   = rs.getShort(6);
            mbd.indexVolumeId     = rs.getShort(7);
            try {
                mbd.config = new Metadata(rs.getString(8)); 
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("unparseable config metadata in mailbox " + mailboxId);
                mbd.config = new Metadata();
            }

            // round lastItemId and lastChangeId up so that they get written on the next change
            long rounding = mbd.lastItemId % ITEM_CHECKPOINT_INCREMENT;
            if (rounding != ITEM_CHECKPOINT_INCREMENT - 1)
                mbd.lastItemId -= rounding + 1;
            rounding = mbd.lastChangeId % CHANGE_CHECKPOINT_INCREMENT;
            if (rounding != CHANGE_CHECKPOINT_INCREMENT - 1)
                mbd.lastChangeId -= rounding + 1;

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
            stmt = conn.prepareStatement(
                    "SELECT message_volume_id, index_volume_id " +
                    "FROM mailbox WHERE id = ?");
            stmt.setInt(1, data.id);
            rs = stmt.executeQuery();
            if (!rs.next())
                throw MailServiceException.NO_SUCH_MBOX(data.id);
            data.messageVolumeId   = rs.getShort(1);
            data.indexVolumeId     = rs.getShort(2);
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

    static Set /* <Long> */ getDistinctTagsets(Connection conn, int mailboxId)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Set tagsets = new HashSet();
        
        try {
            stmt = conn.prepareStatement("SELECT DISTINCT tags " +
                "FROM " + DbMailItem.getMailItemTableName(mailboxId));
            rs = stmt.executeQuery();
            while (rs.next()) {
                long tags = rs.getLong(1);
                tagsets.add(new Long(tags));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct tagsets", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return tagsets;
    }
}
