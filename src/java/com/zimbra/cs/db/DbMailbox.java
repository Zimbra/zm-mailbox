/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.service.util.SyncToken;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

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
    public static final int CI_LAST_BACKUP_AT;
    public static final int CI_COMMENT;
    public static final int CI_LAST_SOAP_ACCESS;
    public static final int CI_NEW_MESSAGES;
    public static final int CI_IDX_DEFERRED_COUNT;
    public static final int CI_HIGHEST_INDEXED;    
    
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
        CI_LAST_BACKUP_AT = pos++;
        CI_COMMENT = pos++;
        CI_LAST_SOAP_ACCESS = pos++;
        CI_NEW_MESSAGES = pos++;
        CI_IDX_DEFERRED_COUNT = pos++;
        CI_HIGHEST_INDEXED = pos++;
    }

    public static final int CI_METADATA_MAILBOX_ID = 1;
    public static final int CI_METADATA_SECTION    = 2;
    public static final int CI_METADATA_METADATA   = 3;

    public static final int CI_SCHEDULED_TASK_MAILBOX_ID = 3;
    public static final int CI_OUT_OF_OFFICE_MAILBOX_ID = 1;

    static final String DB_PREFIX_MAILBOX_GROUP = "mboxgroup";
    static final String TABLE_MAILBOX       = "mailbox";
    static final String TABLE_METADATA      = "mailbox_metadata";
    static final String TABLE_OUT_OF_OFFICE = "out_of_office";

    private static int MAX_COMMENT_LENGTH = 255;

    public static class MailboxIdentifier {
        public final long id;
        public final long groupId;

        public MailboxIdentifier(long mbox_id, long group_id) {
            id = mbox_id;  groupId = group_id;
        }

        @Override public String toString() {
            return "[mailbox " + id + ", group " + groupId + "]";
        }

        @Override public boolean equals(Object obj) {
            if (obj == this)
                return true;
            else if (obj instanceof Number)
                return ((Number) obj).intValue() == id;
            else if (obj instanceof MailboxIdentifier)
                return ((MailboxIdentifier) obj).id == id;
            else
                return false;
        }

        @Override public int hashCode() {
            return (int) (id % Integer.MAX_VALUE);
        }
    }

    /**
     * Gets the next mailbox id.  If <tt>mailboxId</tt> is {@link Mailbox#ID_AUTO_INCREMENT} or
     * greater than the current <tt>next_mailbox_id</tt> value in the <tt>current_volumes</tt>
     * table, <tt>next_mailbox_id</tt>.
     */
    public synchronized static MailboxIdentifier getNextMailboxId(Connection conn, long mailboxId)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        boolean explicitId = (mailboxId != Mailbox.ID_AUTO_INCREMENT);
        ZimbraLog.mailbox.debug("Getting next mailbox id.  requested mailboxId=%d.", mailboxId);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (explicitId) {
                stmt = conn.prepareStatement("UPDATE current_volumes" +
                        " SET next_mailbox_id = ? WHERE next_mailbox_id <= ?");
                stmt.setLong(1, mailboxId + 1);
                stmt.setLong(2, mailboxId);
                stmt.executeUpdate();
            } else {
                // Update first, then select, so that two threads don't select the same id.
                // Probably unnecessary due to "synchronized static", but call me old fashioned.
                stmt = conn.prepareStatement("UPDATE current_volumes" +
                        " SET next_mailbox_id = next_mailbox_id + 1");
                stmt.executeUpdate();
                stmt.close();  stmt = null;

                stmt = conn.prepareStatement("SELECT next_mailbox_id - 1 FROM current_volumes");
                rs = stmt.executeQuery();
                if (rs.next())
                    mailboxId = rs.getLong(1);
                else
                    throw ServiceException.FAILURE("Unable to assign next new mailbox id", null);
            }

            MailboxIdentifier newId = new MailboxIdentifier(mailboxId, calculateMailboxGroupId(mailboxId));
            ZimbraLog.mailbox.debug("Returning mailboxId=%d, groupId=%d.", newId.id, newId.groupId);
            return newId;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting next mailbox id, mailboxId=" + mailboxId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public synchronized static Mailbox.MailboxData createMailbox(Connection conn, long requestedMailboxId, String accountId,
                                                                 String comment, int lastBackupAt)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        String limitClause = Db.supports(Db.Capability.LIMIT_CLAUSE) ? " ORDER BY index_volume_id LIMIT 1" : "";

        // Get mailbox id.
        MailboxIdentifier newMboxId = getNextMailboxId(conn, requestedMailboxId);
        long mailboxId = newMboxId.id;
        long groupId = newMboxId.groupId;

        // Make sure the group database exists before we start doing DMLs.
        createMailboxDatabase(conn, mailboxId, groupId);

        if (comment != null && comment.length() > MAX_COMMENT_LENGTH)
            comment = comment.substring(0, MAX_COMMENT_LENGTH);
        if (comment != null)
            removeFromDeletedAccount(conn, comment);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // first, get the current volume to put Lucene files in
            stmt = conn.prepareStatement("SELECT index_volume_id FROM current_volumes" + limitClause);
            rs = stmt.executeQuery();
            if (!rs.next())
                throw ServiceException.FAILURE("cannot create mailbox: no rows in database table ZIMBRA.CURRENT_VOLUME", null);
            short indexVolume = rs.getShort(1);
            if (rs.next())
                ZimbraLog.mbxmgr.warn("bad state: too many rows in database table ZIMBRA.CURRENT_VOLUME");
            rs.close();    rs = null;
            stmt.close();  stmt = null;

            if (DebugConfig.disableMailboxGroups) {
                Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(groupId));

                if (!DebugConfig.externalMailboxDirectory) {
                    // then create the primary lookup row in ZIMBRA.MAILBOX
                    stmt = conn.prepareStatement("INSERT INTO mailbox (account_id, id, last_backup_at, comment)" +
                            " VALUES (?, ?, ?, ?)");
                    stmt.setString(1, accountId.toLowerCase());
                    stmt.setLong(2, mailboxId);
                    if (lastBackupAt >= 0)
                        stmt.setInt(3, lastBackupAt);
                    else
                        stmt.setNull(3, Types.INTEGER);
                    stmt.setString(4, comment);
                    stmt.executeUpdate();
                    stmt.close();  stmt = null;
                }

                // finally, create the row in MBOXGROUPnn.MAILBOX for mutable state and counts 
                stmt = conn.prepareStatement("INSERT INTO " + qualifyTableName(groupId, TABLE_MAILBOX) +
                        "(id, account_id, index_volume_id, item_id_checkpoint)" +
                        " VALUES (?, ?, ?, " + (Mailbox.FIRST_USER_ID - 1) + ")");
                stmt.setLong(1, mailboxId);
                stmt.setString(2, accountId.toLowerCase());
                stmt.setShort(3, indexVolume);
                stmt.executeUpdate();
            } else {
                // then create the primary lookup row in ZIMBRA.MAILBOX
                stmt = conn.prepareStatement("INSERT INTO mailbox" +
                        "(account_id, id, group_id, index_volume_id, item_id_checkpoint, last_backup_at, comment)" +
                        " VALUES (?, ?, ?, ?, " + (Mailbox.FIRST_USER_ID - 1) + ", ?, ?)");
                stmt.setString(1, accountId.toLowerCase());
                stmt.setLong(2, mailboxId);
                stmt.setLong(3, groupId);
                stmt.setInt(4, indexVolume);
                if (lastBackupAt >= 0)
                    stmt.setInt(5, lastBackupAt);
                else
                    stmt.setNull(5, Types.INTEGER);
                stmt.setString(6, comment);
                stmt.executeUpdate();
            }

            Mailbox.MailboxData data = new Mailbox.MailboxData();
            data.accountId = accountId;
            data.id = mailboxId;
            data.lastItemId = Mailbox.FIRST_USER_ID - 1;
            data.schemaGroupId = groupId;
            data.indexVolumeId = indexVolume;
            return data;
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
    public static void createMailboxDatabase(Connection conn, long mailboxId, long groupId)
    throws ServiceException {
        ZimbraLog.mailbox.debug("createMailboxDatabase(" + mailboxId + ")");

        File file = new File(LC.mailboxd_directory.value() + "/../db/create_database.sql");

        PreparedStatement stmt = null;
        try {
            String dbname = getDatabaseName(groupId);
            if (Db.getInstance().databaseExists(conn, dbname)) {
                // If database didn't exist we would end up doing CREATE DATABASE which does implicit commit.
                // Let's do an explicit here so pending transactions always committed on exit from this method
                // whether we create a database or not.
                conn.commit();
                return;
            }

            Db.getInstance().precreateDatabase(dbname);

            // create the new database
            ZimbraLog.mailbox.info("Creating database " + dbname);
            Db.getInstance().registerDatabaseInterest(conn, dbname);

            String template = new String(ByteUtil.getContent(file));
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("DATABASE_NAME", dbname);
            String script = StringUtil.fillTemplate(template, vars);
            // note that DbUtil.executeScript ends with a COMMIT
            DbUtil.executeScript(conn, new StringReader(script));
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to read SQL statements from " + file.getPath(), e);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("createMailboxDatabase(" + mailboxId + ")", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    // Tables are listed in order of creation.  dropMailboxFromGroup() drops them
    // in reverse order.
    static final List<String> sTables = new ArrayList<String>();
        static {
            if (DebugConfig.disableMailboxGroups) {
                sTables.add(TABLE_MAILBOX);
                sTables.add(TABLE_METADATA);
                sTables.add(TABLE_OUT_OF_OFFICE);
            }
            sTables.add(DbMailItem.TABLE_MAIL_ITEM);
            sTables.add(DbMailItem.TABLE_MAIL_ITEM_DUMPSTER);
            sTables.add(DbMailItem.TABLE_OPEN_CONVERSATION);
            sTables.add(DbMailItem.TABLE_APPOINTMENT);
            sTables.add(DbMailItem.TABLE_APPOINTMENT_DUMPSTER);
            sTables.add(DbMailItem.TABLE_REVISION);
            sTables.add(DbMailItem.TABLE_REVISION_DUMPSTER);
            sTables.add(DbMailItem.TABLE_TOMBSTONE);
            sTables.add(DbImapFolder.TABLE_IMAP_FOLDER);
            sTables.add(DbImapMessage.TABLE_IMAP_MESSAGE);
            sTables.add(DbPop3Message.TABLE_POP3_MESSAGE);
            sTables.add(DbDataSource.TABLE_DATA_SOURCE_ITEM);
        };

    private static void dropMailboxFromGroup(Connection conn, Mailbox mbox)
    throws ServiceException {
        long mailboxId = mbox.getId();
        ZimbraLog.mailbox.info("clearing contents of mailbox " + mailboxId + ", group " + mbox.getSchemaGroupId());

        if (DebugConfig.disableMailboxGroups && Db.supports(Db.Capability.FILE_PER_DATABASE)) {
            Db.getInstance().deleteDatabaseFile(getDatabaseName(mbox));
            return;
        }

        if (conn == null)
            conn = mbox.getOperationConnection();
        else
            Db.registerDatabaseInterest(conn, mbox);

        try {
            if (Db.supports(Db.Capability.DISABLE_CONSTRAINT_CHECK))
                conn.disableForeignKeyConstraints();

            // delete from tables in reverse order
            for (int i = sTables.size() - 1; i >= 0; i--) {
                String tableName = sTables.get(i);
                if (tableName == null)
                    continue;
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement("DELETE FROM " + qualifyTableName(mbox, tableName) +
                            (DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = " + mailboxId));
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
                ZimbraLog.mailbox.error("error enabling foreign key constraints during mailbox deletion", e);
                // don't rethrow to avoid masking any exception from DELETE statements
            }
        }
    }

    public static void clearMailboxContent(Mailbox mbox) throws ServiceException {
        clearMailboxContent(null, mbox);
    }

    public static void clearMailboxContent(Connection conn, Mailbox mbox) throws ServiceException {
        dropMailboxFromGroup(conn, mbox);
    }

    public static void renameMailbox(Mailbox mbox, String newName) throws ServiceException {
        if (DebugConfig.externalMailboxDirectory)
            return;

        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        long mailboxId = mbox.getId();
        ZimbraLog.mailbox.info("Renaming email/comment of mailbox " + mailboxId + " to " + newName);

        Connection conn = mbox.getOperationConnection();

        try {
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement("UPDATE mailbox SET comment = ?, last_backup_at = NULL WHERE id = ?");
                stmt.setString(1, newName);
                stmt.setLong(2, mailboxId);
                stmt.executeUpdate();
            } finally {
                DbPool.closeStatement(stmt);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("renameMailbox(" + mailboxId + ")", e);
        }
    }

    public static void clearMailboxContactCount(Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET contact_count = NULL WHERE id = ?");
            stmt.setLong(1, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("clearing contact count for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void recordLastSoapAccess(Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET last_soap_access = ? WHERE id = ?");
            stmt.setInt(1, (int) (mbox.getLastSoapAccessTime() / 1000));
            stmt.setLong(2, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating last SOAP access time for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateMailboxStats(Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET item_id_checkpoint = ?, contact_count = ?, change_checkpoint = ?," +
                    "  size_checkpoint = ?, new_messages = ?, idx_deferred_count = ?, highest_indexed = ?" +
                    " WHERE id = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getLastItemId());
            stmt.setInt(pos++, mbox.getContactCount());
            stmt.setInt(pos++, mbox.getLastChangeID());
            stmt.setLong(pos++, mbox.getSize());
            stmt.setInt(pos++, mbox.getRecentMessageCount());
            stmt.setInt(pos++, mbox.getIndexDeferredCount());
            stmt.setString(pos++, mbox.getHighestFlushedToIndex().toString());
            stmt.setLong(pos++, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating mailbox statistics for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void startTrackingSync(Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET tracking_sync = ? WHERE id = ? AND tracking_sync <= 0");
            stmt.setInt(1, mbox.getLastChangeID());
            stmt.setLong(2, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on sync tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void startTrackingImap(Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET tracking_imap = 1 WHERE id = ?");
            stmt.setLong(1, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on imap tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static String getConfig(Mailbox mbox, String section) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT metadata FROM " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "section = ?");
            int pos = 1;
            if (!DebugConfig.disableMailboxGroups)
                stmt.setLong(pos++, mbox.getId());
            stmt.setString(pos++, section);
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
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        Connection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            if (config == null) {
                stmt = conn.prepareStatement("DELETE FROM " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "section = ?");
                int pos = 1;
                if (!DebugConfig.disableMailboxGroups)
                    stmt.setLong(pos++, mbox.getId());
                stmt.setString(pos++, section.toUpperCase());
                stmt.executeUpdate();
                stmt.close();
            } else {
                // We try INSERT first even though it's the less common case, to avoid MySQL
                // deadlock.  See bug 19404 for more info.
                try {
                    String command = Db.supports(Db.Capability.REPLACE_INTO) ? "REPLACE" : "INSERT";
                    stmt = conn.prepareStatement(command + " INTO " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                            " (" + (DebugConfig.disableMailboxGroups ? "" : "mailbox_id, ") + "section, metadata)" +
                            " VALUES (" + (DebugConfig.disableMailboxGroups ? "" : "?, ") + "?, ?)");
                    int pos = 1;
                    if (!DebugConfig.disableMailboxGroups)
                        stmt.setLong(pos++, mbox.getId());
                    stmt.setString(pos++, section);
                    stmt.setString(pos++, config.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                        stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                                " SET metadata = ? WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "section = ?");
                        int pos = 1;
                        stmt.setString(pos++, config.toString());
                        if (!DebugConfig.disableMailboxGroups)
                            stmt.setLong(pos++, mbox.getId());
                        stmt.setString(pos++, section);
                        int numRows = stmt.executeUpdate();
                        if (numRows != 1) {
                            String msg = String.format("Unexpected number of rows (%d) updated for section %s", numRows, section);
                            throw ServiceException.FAILURE(msg, e);
                        }
                    }
                }
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
    public static Map<String, Long> listMailboxes(Connection conn) throws ServiceException {
        return listMailboxes(conn, MailboxManager.getInstance());
    }

    public static Map<String, Long> listMailboxes(Connection conn, MailboxManager mmgr) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mmgr));

        HashMap<String, Long> result = new HashMap<String, Long>();
        if (DebugConfig.externalMailboxDirectory)
            return result;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT account_id, id FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next())
                result.put(rs.getString(1).toLowerCase(), rs.getLong(2));
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
    public static Map<String, Long> getMailboxSizes(Connection conn, List<Long> mailboxIds) throws ServiceException {
        // FIXME: wrong locking check for DB-per-user case
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        HashMap<String, Long> sizes = new HashMap<String, Long>();
        if (DebugConfig.externalMailboxDirectory)
            return sizes;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (!DebugConfig.disableMailboxGroups) {
                stmt = conn.prepareStatement("SELECT account_id, size_checkpoint FROM mailbox");
                rs = stmt.executeQuery();
                while (rs.next())
                    sizes.put(rs.getString(1).toLowerCase(), rs.getLong(2));
            } else {
                // FIXME: not taking mailbox locks in the non-ROW_LEVEL_LOCKING case
                for (long mailboxId : mailboxIds) {
                    // note that if groups are disabled, mailboxId == groupId
                    Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(mailboxId));

                    stmt = conn.prepareStatement("SELECT account_id, size_checkpoint FROM " + qualifyZimbraTableName(mailboxId, TABLE_MAILBOX));
                    rs = stmt.executeQuery();
                    while (rs.next())
                        sizes.put(rs.getString(1).toLowerCase(), rs.getLong(2));
                    rs.close();    rs = null;
                    stmt.close();  stmt = null;

                    // XXX: have to avoid having too many attached databases referenced in the same transaction?
                    conn.commit();
                }
            }
            return sizes;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching mailboxes", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static final int CHANGE_CHECKPOINT_INCREMENT = Math.max(1, LC.zimbra_mailbox_change_checkpoint_frequency.intValue());
    public static final int ITEM_CHECKPOINT_INCREMENT   = 20;

    public static Mailbox.MailboxData getMailboxStats(Connection conn, long mailboxId) throws ServiceException {
        // no locking check because it's a mailbox-level op done before the Mailbox object is instantiated...

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (DebugConfig.disableMailboxGroups)
                Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(mailboxId));

            // note that if groups are disabled, mailboxId == groupId
            stmt = conn.prepareStatement(
                    "SELECT account_id," + (DebugConfig.disableMailboxGroups ? mailboxId : " group_id") + "," +
                    " size_checkpoint, contact_count, item_id_checkpoint, change_checkpoint, tracking_sync," +
                    " tracking_imap, index_volume_id, last_soap_access, new_messages, idx_deferred_count, highest_indexed " +
                    "FROM " + qualifyZimbraTableName(mailboxId, TABLE_MAILBOX) + " WHERE id = ?");
            stmt.setLong(1, mailboxId);
            rs = stmt.executeQuery();

            if (!rs.next())
                return null;
            int pos = 1;
            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            mbd.id            = mailboxId;
            mbd.accountId     = rs.getString(pos++).toLowerCase();
            mbd.schemaGroupId = rs.getLong(pos++);
            mbd.size          = rs.getLong(pos++);
            if (rs.wasNull())
                mbd.size = -1;
            mbd.contacts      = rs.getInt(pos++);
            if (rs.wasNull())
                mbd.contacts = -1;
            mbd.lastItemId    = rs.getInt(pos++);
            mbd.lastChangeId  = rs.getInt(pos++);
            mbd.trackSync     = rs.getInt(pos++);
            mbd.trackImap     = rs.getBoolean(pos++);
            mbd.indexVolumeId = rs.getShort(pos++);
            mbd.lastWriteDate = rs.getInt(pos++);
            mbd.recentMessages = rs.getInt(pos++);
            mbd.idxDeferredCount = rs.getInt(pos++);

            String highestModContentIndexed = rs.getString(pos++);
            if (highestModContentIndexed == null || highestModContentIndexed.length() == 0)
                mbd.highestModContentIndexed = new SyncToken(mbd.lastChangeId);
            else {
                try {
                    mbd.highestModContentIndexed = new SyncToken(highestModContentIndexed);
                } catch (MailServiceException e) {
                    ZimbraLog.mailbox.warn("Exception loading index high water mark from DB.  " +
                    		"Using current mod_content value: "+mbd.lastChangeId, e);
                    mbd.highestModContentIndexed = new SyncToken(mbd.lastChangeId);
                }
            }
            
            mbd.lastBackupDate = -1;

            // round lastItemId and lastChangeId up so that they get written on the next change
            mbd.lastItemId += ITEM_CHECKPOINT_INCREMENT - 1;
            mbd.lastChangeId += CHANGE_CHECKPOINT_INCREMENT - 1;
            long rounding = mbd.lastItemId % ITEM_CHECKPOINT_INCREMENT;
            if (rounding != ITEM_CHECKPOINT_INCREMENT - 1)
                mbd.lastItemId -= rounding + 1;
            rounding = mbd.lastChangeId % CHANGE_CHECKPOINT_INCREMENT;
            if (rounding != CHANGE_CHECKPOINT_INCREMENT - 1)
                mbd.lastChangeId -= rounding + 1;

            rs.close();    rs = null;
            stmt.close();  stmt = null;

            stmt = conn.prepareStatement("SELECT section FROM " + qualifyZimbraTableName(mailboxId, TABLE_METADATA) +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = ?"));
            if (!DebugConfig.disableMailboxGroups)
                stmt.setLong(1, mailboxId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (mbd.configKeys == null)
                    mbd.configKeys = new HashSet<String>();
                mbd.configKeys.add(rs.getString(1));
            }

            return mbd;
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.NO_SUCH_TABLE))
                return null;
            throw ServiceException.FAILURE("fetching stats on mailbox " + mailboxId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }


    /** Returns the object to synchronize all accesses to tables in the ZIMBRA
     *  database on.  When the underlying database supports row-level locking,
     *  this will be a new <code>Object</code> -- that is, effectively no
     *  synchronization, since none is necessary.  If synchronization is needed
     *  but the code is not encapsulated in a 
     *     <code>synchronized (DbMailbox.getSynchronizer()) { }</code>
     *  block, calls to DbMailbox methods will assert.
     * @see Db.Capability#ROW_LEVEL_LOCKING */
    public static Object getSynchronizer() {
        try {
            if (!Db.supports(Db.Capability.ROW_LEVEL_LOCKING))
                return MailboxManager.getInstance();
        } catch (ServiceException e) { }

        return new Object();
    }

    public static Object getZimbraSynchronizer(Mailbox mbox) throws ServiceException {
        return DebugConfig.disableMailboxGroups ? mbox : MailboxManager.getInstance();
    }


    public static long calculateMailboxGroupId(long mailboxId) {
        int groups = DebugConfig.numMailboxGroups;
        // -1 / +1 operations are done so that the group id is never 0.
        return (mailboxId - 1) % groups + 1;
    }

    /** Returns the name of the database that contains tables for the
     *  specified <code>mailboxId</code>.  As a side effect, also registers
     *  interest on that database with the specified {@link Connection} (if
     *  not <tt>null</tt>). */
    public static String getDatabaseName(Mailbox mbox) {
        return getDatabaseName(mbox.getSchemaGroupId());
    }

    public static String getDatabaseName(long groupId) {
        return DB_PREFIX_MAILBOX_GROUP + groupId;
    }

    /** Qualifies the name of a database table that may be in the per-user
     *  database if {@link DebugConfig.disableMailboxGroups} is set and in
     *  the <tt>ZIMBRA</tt> database otherwise. */
    public static String qualifyZimbraTableName(Mailbox mbox, String tableName) {
        return DebugConfig.disableMailboxGroups ? qualifyTableName(mbox, tableName) : tableName;
    }

    /** Qualifies the name of a database table that may be in the per-user
     *  database if {@link DebugConfig.disableMailboxGroups} is set and in
     *  the <tt>ZIMBRA</tt> database otherwise. */
    public static String qualifyZimbraTableName(long mailboxId, String tableName) {
        // note that if groups are disabled, mailboxId == groupId
        return DebugConfig.disableMailboxGroups ? qualifyTableName(mailboxId, tableName) : tableName;
    }

    public static String qualifyTableName(Mailbox mbox, String tableName) {
        return qualifyTableName(mbox.getSchemaGroupId(), tableName);
    }

    public static String qualifyTableName(long groupId, String tableName) {
        return DB_PREFIX_MAILBOX_GROUP + groupId + '.' + tableName;
    }


    public static void removeFromDeletedAccount(Connection conn, String email)
    throws ServiceException {
        PreparedStatement stmt = null;
        try {
            // add the mailbox's account to deleted_account table
            stmt = conn.prepareStatement("DELETE FROM deleted_account WHERE email = ?");
            stmt.setString(1, email.toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting row for " + email + " from deleted_account table", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    private static void addToDeletedAccount(Connection conn, Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        if (DebugConfig.externalMailboxDirectory)
            return;

        // Get email address for mailbox by querying the mailbox table.  We can't get it by
        // calling mbox.getAccount().getName() because the account was already deleted from LDAP.
        String email = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT comment FROM mailbox WHERE id = ?");
            stmt.setLong(1, mbox.getId());
            rs = stmt.executeQuery();
            if (rs.next())
                email = rs.getString(1);
            else
                throw ServiceException.FAILURE("no email address found for mailbox " + mbox.getId(), null);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting email address for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        boolean supportsReplaceInto = Db.supports(Db.Capability.REPLACE_INTO);
        if (!supportsReplaceInto)
            removeFromDeletedAccount(conn, email);

        try {
            // add the mailbox's account to deleted_account table
            String command = supportsReplaceInto ? "REPLACE" : "INSERT";
            stmt = conn.prepareStatement(
                    command + " INTO deleted_account (email, account_id, mailbox_id, deleted_at) " +
                    "SELECT ?, account_id, id, ? FROM mailbox WHERE id = ?");
            stmt.setString(1, email.toLowerCase());
            stmt.setLong(2, System.currentTimeMillis() / 1000);
            stmt.setLong(3, mbox.getId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("marking mailbox " + mbox.getId() + " as deleted", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Returns a DeletedAccount object for the given email, if the account for the email
     * address was previously deleted.  Returns null if account for the email was not
     * deleted.
     * @param conn
     * @param email
     * @return
     * @throws ServiceException
     */
    public static DeletedAccount getDeletedAccount(Connection conn, String email)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT email, account_id, mailbox_id, deleted_at " +
                    "FROM deleted_account WHERE email = ?");
            stmt.setString(1, email.toLowerCase());
            rs = stmt.executeQuery();
            if (rs.next()) {
                String emailCol = rs.getString(1);
                String accountId = rs.getString(2).toLowerCase();
                long mailboxId = rs.getLong(3);
                long deletedAt = rs.getLong(4) * 1000;
                return new DeletedAccount(emailCol, accountId, mailboxId, deletedAt);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("checking if account " + email + " is deleted", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static class DeletedAccount {
        private String mEmail;
        private String mAccountId;
        private long mMailboxId;
        private long mDeletedAt;

        public DeletedAccount(String email, String accountId, long mailboxId, long deletedAt) {
            mEmail = email;
            mAccountId = accountId;
            mMailboxId = mailboxId;
            mDeletedAt = deletedAt;
        }

        public String getEmail()      { return mEmail; }
        public String getAccountId()  { return mAccountId; }
        public long getMailboxId()    { return mMailboxId; }
        public long getDeletedAt()    { return mDeletedAt; }
    }

    /**
     * Deletes the row for the specified mailbox from the <code>mailbox</code> table.
     *  
     * @throws ServiceException if the database operation failed
     */
    public static void deleteMailbox(Connection conn, Mailbox mbox) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(getZimbraSynchronizer(mbox)));

        addToDeletedAccount(conn, mbox);

        PreparedStatement stmt = null;
        try {
            if (DebugConfig.disableMailboxGroups) {
                // remove entry from MBOXGROUP database (is this redundant?)
                stmt = conn.prepareStatement("DELETE FROM " + qualifyTableName(mbox, TABLE_MAILBOX) +
                        " WHERE id = ?");
                stmt.setLong(1, mbox.getId());
                stmt.executeUpdate();
                stmt.close();
            }

            // remove entry from mailbox table
            if (!DebugConfig.externalMailboxDirectory) {
                stmt = conn.prepareStatement("DELETE FROM mailbox WHERE id = ?");
                stmt.setLong(1, mbox.getId());
                stmt.executeUpdate();
            }
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
            stmt = conn.prepareStatement("SELECT DISTINCT(tags) FROM " + DbMailItem.getMailItemTableName(mbox) +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = ?"));
            if (!DebugConfig.disableMailboxGroups)
                stmt.setLong(1, mbox.getId());
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
            stmt = conn.prepareStatement("SELECT DISTINCT(flags) FROM " + DbMailItem.getMailItemTableName(mbox) +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = ?"));
            if (!DebugConfig.disableMailboxGroups)
                stmt.setLong(1, mbox.getId());
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

    /**
     * Returns the account id's for the current server.
     */
    public static Set<String> listAccountIds(Connection conn) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

        Set<String> accountIds = new HashSet<String>();
        if (DebugConfig.externalMailboxDirectory)
            return accountIds;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT account_id FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next())
                accountIds.add(rs.getString(1).toLowerCase());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct account ids", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return accountIds;
    }
  
    public static List<Mailbox.MailboxData> getMailboxRawData(Connection conn) throws ServiceException {
        List<Mailbox.MailboxData> results = new ArrayList<Mailbox.MailboxData>();
        if (DebugConfig.externalMailboxDirectory)
            return results;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (!DebugConfig.disableMailboxGroups) {
                assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(MailboxManager.getInstance()));

                stmt = conn.prepareStatement(
                        "SELECT id, group_id, account_id, index_volume_id, item_id_checkpoint," +
                        " contact_count, size_checkpoint, change_checkpoint, tracking_sync," +
                        " tracking_imap, last_backup_at, last_soap_access, new_messages," +
                        " idx_deferred_count, highest_indexed " +
                        "FROM mailbox");
                rs = stmt.executeQuery();
                readMailboxRawData(results, rs);
            } else {
                // FIXME: need an (impossible) assert for synchronization purposes

                long[] mailboxIds = MailboxManager.getInstance().getMailboxIds();
                for (long mailboxId : mailboxIds) {
                    Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(mailboxId));

                    stmt = conn.prepareStatement(
                            "SELECT id, id, account_id, index_volume_id, item_id_checkpoint, contact_count, size_checkpoint," +
                            " change_checkpoint, tracking_sync, tracking_imap, -1, last_soap_access, new_messages," +
                            " idx_deferred_count, highest_indexed" +
                            "FROM " + qualifyZimbraTableName(mailboxId, TABLE_MAILBOX));
                    rs = stmt.executeQuery();
                    readMailboxRawData(results, rs);
                    rs.close();    rs = null;
                    stmt.close();  stmt = null;

                    // XXX: have to avoid having too many attached databases referenced in the same transaction?
                    conn.commit();
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct account id's", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return results;
    }

    public static void optimize(Connection conn, Mailbox mbox, int level) throws ServiceException {
        assert(Thread.holdsLock(getZimbraSynchronizer(mbox)));

        String name = getDatabaseName(mbox);
        
        try {
            Db.getInstance().optimize(conn, name, level);
        } catch (Exception e) {
            throw ServiceException.FAILURE("optimizing mailbox db " + name, e);
        }
    }

    private static void readMailboxRawData(List<Mailbox.MailboxData> results, ResultSet rs) throws SQLException {
        while (rs.next()) {
            Mailbox.MailboxData data = new Mailbox.MailboxData();
            int pos = 1;
            data.id = rs.getLong(pos++);
            data.schemaGroupId = rs.getLong(pos++);
            data.accountId = rs.getString(pos++).toLowerCase();
            data.indexVolumeId = rs.getShort(pos++);
            data.lastItemId = rs.getInt(pos++);
            data.contacts = rs.getInt(pos++);
            data.size = rs.getLong(pos++);
            data.lastChangeId = rs.getInt(pos++);
            data.trackSync = rs.getInt(pos++);
            data.trackImap = rs.getBoolean(pos++);
            data.lastBackupDate = rs.getInt(pos++);
            // data.comment = rs.getString(pos++);
            data.lastWriteDate = rs.getInt(pos++);
            data.recentMessages = rs.getInt(pos++);
            data.idxDeferredCount = rs.getInt(pos++);
            
            String highestModContentIndexed = rs.getString(pos++);
            if (highestModContentIndexed == null || highestModContentIndexed.length() == 0)
                data.highestModContentIndexed = new SyncToken(data.lastChangeId);
            else {
                try {
                    data.highestModContentIndexed = new SyncToken(highestModContentIndexed);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.warn("Exception loading index high water mark from DB.  " +
                                           "Using current mod_content value: "+data.lastChangeId, e);
                    data.highestModContentIndexed = new SyncToken(data.lastChangeId);
                }
            }
            
            results.add(data);
        }
    }
}
