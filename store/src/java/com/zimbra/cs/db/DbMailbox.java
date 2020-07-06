/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxVersion;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.redolog.BackupHostManager.BackupHost;

/**
 * @since Oct 28, 2004
 */
public final class DbMailbox {

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
    public static final int CI_INDEX_DEFERRED;
    public static final int CI_HIGHEST_INDEXED;
    public static final int CI_VERSION;
    public static final int CI_LAST_PURGE_AT;
    public static final int CI_ITEMCACHE_CHECKPOINT;
    public static final int CI_BACKUP_HOST_ID;

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
        CI_INDEX_DEFERRED = pos++;
        CI_HIGHEST_INDEXED = pos++;
        CI_VERSION = pos++;
        CI_LAST_PURGE_AT = pos++;
        CI_ITEMCACHE_CHECKPOINT = pos++;
        CI_BACKUP_HOST_ID = pos++;
    }

    public static final int CI_METADATA_MAILBOX_ID = 1;
    public static final int CI_METADATA_SECTION    = 2;
    public static final int CI_METADATA_METADATA   = 3;

    public static final int CI_SCHEDULED_TASK_MAILBOX_ID = 3;
    public static final int CI_OUT_OF_OFFICE_MAILBOX_ID = 1;
    public static final int CI_MOBILE_DEVICES_MAILBOX_ID = 1;
    public static final int CI_PENDING_ACL_PUSH_MAILBOX_ID = 1;

    static final String DB_PREFIX_MAILBOX_GROUP = "mboxgroup";
    static final String TABLE_MAILBOX       = "mailbox";
    static final String TABLE_METADATA      = "mailbox_metadata";
    static final String TABLE_OUT_OF_OFFICE = "out_of_office";

    private static int MAX_COMMENT_LENGTH = 255;

    public static class MailboxIdentifier {
        public final int id;
        public final int groupId;

        public MailboxIdentifier(int mbox_id, int group_id) {
            id = mbox_id;
            groupId = group_id;
        }

        @Override
        public String toString() {
            return "[mailbox " + id + ", group " + groupId + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof Number) {
                return ((Number) obj).intValue() == id;
            } else if (obj instanceof MailboxIdentifier) {
                return ((MailboxIdentifier) obj).id == id;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public interface DbTableCallback {
        public void create(DbConnection conn, int mailboxId, int groupId) throws ServiceException;
        public Collection<String> getTableNames();
    }

    public static void addCreateDatabaseCallback(DbTableCallback callback) {
        callbacks.add(callback);
    }

    private static final HashSet<DbTableCallback> callbacks = new HashSet<DbTableCallback>();

    /**
     * Gets the next mailbox id.  If <tt>mailboxId</tt> is {@link Mailbox#ID_AUTO_INCREMENT} or
     * greater than the current <tt>next_mailbox_id</tt> value in the <tt>current_volumes</tt>
     * table, <tt>next_mailbox_id</tt>.
     */
    public synchronized static MailboxIdentifier getNextMailboxId(DbConnection conn, int mailboxId)
    throws ServiceException {
        boolean explicitId = (mailboxId != Mailbox.ID_AUTO_INCREMENT);
        ZimbraLog.mailbox.debug("Getting next mailbox id.  requested mailboxId=%d.", mailboxId);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (explicitId) {
                stmt = conn.prepareStatement("UPDATE current_volumes" +
                        " SET next_mailbox_id = ? WHERE next_mailbox_id <= ?");
                stmt.setInt(1, mailboxId + 1);
                stmt.setInt(2, mailboxId);
                stmt.executeUpdate();
            } else {
                // Update first, then select, so that two threads don't select the same id.
                // Probably unnecessary due to "synchronized static", but call me old fashioned.
                stmt = conn.prepareStatement("UPDATE current_volumes" +
                        " SET next_mailbox_id = next_mailbox_id + 1");
                stmt.executeUpdate();
                stmt.close();
                stmt = null;

                stmt = conn.prepareStatement("SELECT next_mailbox_id - 1 FROM current_volumes");
                rs = stmt.executeQuery();
                if (rs.next()) {
                    mailboxId = rs.getInt(1);
                } else {
                    throw ServiceException.FAILURE("Unable to assign next new mailbox id", null);
                }
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

    public synchronized static Mailbox.MailboxData createMailbox(DbConnection conn, int requestedMailboxId, String accountId,
            String comment, int lastBackupAt) throws ServiceException {
        return createMailbox(conn, requestedMailboxId, accountId, comment, lastBackupAt, null);
    }

    public synchronized static Mailbox.MailboxData createMailbox(DbConnection conn, int requestedMailboxId, String accountId,
                                                                 String comment, int lastBackupAt, BackupHost backupHost)
    throws ServiceException {
        String limitClause = Db.supports(Db.Capability.LIMIT_CLAUSE) ? " ORDER BY index_volume_id " + Db.getInstance().limit(1) : "";

        // Get mailbox id.
        MailboxIdentifier newMboxId = getNextMailboxId(conn, requestedMailboxId);
        int mailboxId = newMboxId.id;
        int groupId = newMboxId.groupId;

        // Make sure the group database exists before we start doing DMLs.
        createMailboxDatabase(conn, mailboxId, groupId);

        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            comment = comment.substring(0, MAX_COMMENT_LENGTH);
        }
        if (comment != null) {
            removeFromDeletedAccount(conn, comment);
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // first, get the current volume to put Lucene files in
            stmt = conn.prepareStatement("SELECT index_volume_id FROM current_volumes" + limitClause);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw ServiceException.FAILURE("cannot create mailbox: no rows in database table ZIMBRA.CURRENT_VOLUME", null);
            }
            short indexVolume = rs.getShort(1);
            if (rs.next()) {
                ZimbraLog.mbxmgr.warn("bad state: too many rows in database table ZIMBRA.CURRENT_VOLUME");
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;

            if (DebugConfig.disableMailboxGroups) {
                Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(groupId));

                if (!DebugConfig.externalMailboxDirectory) {
                    // then create the primary lookup row in ZIMBRA.MAILBOX
                    stmt = conn.prepareStatement("INSERT INTO mailbox (account_id, id, last_backup_at, comment)" +
                            " VALUES (?, ?, ?, ?)");
                    stmt.setString(1, accountId.toLowerCase());
                    stmt.setInt(2, mailboxId);
                    if (lastBackupAt >= 0) {
                        stmt.setInt(3, lastBackupAt);
                    } else {
                        stmt.setNull(3, Types.INTEGER);
                    }
                    stmt.setString(4, comment);
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = null;
                }

                // finally, create the row in MBOXGROUPnn.MAILBOX for mutable state and counts
                stmt = conn.prepareStatement("INSERT INTO " + qualifyTableName(groupId, TABLE_MAILBOX) +
                        "(id, account_id, index_volume_id, item_id_checkpoint, version)" +
                        " VALUES (?, ?, ?, " + (Mailbox.FIRST_USER_ID - 1) + ", ?)");
                stmt.setInt(1, mailboxId);
                stmt.setString(2, accountId.toLowerCase());
                stmt.setShort(3, indexVolume);
                stmt.setString(4, MailboxVersion.getCurrent().toString());
                stmt.executeUpdate();
            } else {
                // then create the primary lookup row in ZIMBRA.MAILBOX
                stmt = conn.prepareStatement("INSERT INTO mailbox" +
                        "(account_id, id, group_id, index_volume_id, item_id_checkpoint, last_backup_at, comment, version, backup_host_id)" +
                        " VALUES (?, ?, ?, ?, " + (Mailbox.FIRST_USER_ID - 1) + ", ?, ?, ?, ?)");
                stmt.setString(1, accountId.toLowerCase());
                stmt.setInt(2, mailboxId);
                stmt.setInt(3, groupId);
                stmt.setInt(4, indexVolume);
                if (lastBackupAt >= 0) {
                    stmt.setInt(5, lastBackupAt);
                } else {
                    stmt.setNull(5, Types.INTEGER);
                }
                stmt.setString(6, comment);
                stmt.setString(7, MailboxVersion.getCurrent().toString());
                if (backupHost != null) {
                    stmt.setInt(8, backupHost.getHostId());
                } else {
                    stmt.setNull(8, java.sql.Types.INTEGER);
                }
                stmt.executeUpdate();
            }

            Mailbox.MailboxData data = new Mailbox.MailboxData();
            data.accountId = accountId;
            data.id = mailboxId;
            data.lastItemId = Mailbox.FIRST_USER_ID - 1;
            data.schemaGroupId = groupId;
            data.indexVolumeId = indexVolume;
            data.version = MailboxVersion.getCurrent();
            data.backupHostId = backupHost == null ? 0 : backupHost.getHostId();
            return data;
        } catch (SQLException e) {
        	if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS(accountId, e);
        	} else {
               throw ServiceException.FAILURE("writing new mailbox row for account " + accountId, e);
        	}
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    /** Create a database for the specified mailbox.
     *
     * @throws ServiceException if the database creation fails */
    public static void createMailboxDatabase(DbConnection conn, int mailboxId, int groupId)
    throws ServiceException {
        ZimbraLog.mailbox.debug("createMailboxDatabase(mailboxId=%s, groupId=%s)", mailboxId, groupId);

        File file = new File(LC.mailboxd_directory.value() + "/../db/create_database.sql");

        boolean succeeded = false;
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
            ZimbraLog.mailbox.info("Creating database %s", dbname);
            Db.getInstance().registerDatabaseInterest(conn, dbname);

            String template = new String(ByteUtil.getContent(file));
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("DATABASE_NAME", dbname);
            String script = StringUtil.fillTemplate(template, vars);
            // note that DbUtil.executeScript ends with a COMMIT
            DbUtil.executeScript(conn, new StringReader(script));
            succeeded = true;
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to read SQL statements from " + file.getPath(), e);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("createMailboxDatabase(" + mailboxId + ")", e);
        } finally {
            if (succeeded) {
                for (DbTableCallback callback : callbacks) {
                    callback.create(conn, mailboxId, groupId);
                }
            }
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
            sTables.add(DbMailItem.TABLE_REVISION);
            sTables.add(DbMailItem.TABLE_REVISION_DUMPSTER);
            sTables.add(DbTag.TABLE_TAG);
            sTables.add(DbTag.TABLE_TAGGED_ITEM);
            sTables.add(DbMailItem.TABLE_OPEN_CONVERSATION);
            sTables.add(DbMailItem.TABLE_APPOINTMENT);
            sTables.add(DbMailItem.TABLE_APPOINTMENT_DUMPSTER);
            sTables.add(DbMailItem.TABLE_TOMBSTONE);
            sTables.add(DbPop3Message.TABLE_POP3_MESSAGE);
            sTables.add(DbImapFolder.TABLE_IMAP_FOLDER);
            sTables.add(DbImapMessage.TABLE_IMAP_MESSAGE);
            sTables.add(DbDataSource.TABLE_DATA_SOURCE_ITEM);
        };

    private static void dropMailboxFromGroup(DbConnection conn, Mailbox mbox)
    throws ServiceException {
        int mailboxId = mbox.getId();
        ZimbraLog.mailbox.info("clearing contents of mailbox " + mailboxId + ", group " + mbox.getSchemaGroupId());

        if (DebugConfig.disableMailboxGroups && Db.supports(Db.Capability.FILE_PER_DATABASE)) {
            Db.getInstance().deleteDatabaseFile(conn, getDatabaseName(mbox));
            return;
        }

        if (conn == null) {
            conn = mbox.getOperationConnection();
        } else {
            Db.registerDatabaseInterest(conn, mbox);
        }

        try {
            if (Db.supports(Db.Capability.DISABLE_CONSTRAINT_CHECK)) {
                conn.disableForeignKeyConstraints();
            }

            // delete from tables in reverse order
            ArrayList<String> tables = new ArrayList<String>();
            tables.addAll(sTables);
            for (DbTableCallback callback : callbacks) {
                tables.addAll(callback.getTableNames());
            }
            Collections.reverse(tables);

            for (String tableName : tables) {
                if (tableName == null) {
                    continue;
                }

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
                if (Db.supports(Db.Capability.DISABLE_CONSTRAINT_CHECK)) {
                    conn.enableForeignKeyConstraints();
                }
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("error enabling foreign key constraints during mailbox deletion", e);
                // don't rethrow to avoid masking any exception from DELETE statements
            }
        }
    }

    public static void clearMailboxContent(Mailbox mbox) throws ServiceException {
        clearMailboxContent(null, mbox);
    }

    public static void clearMailboxContent(DbConnection conn, Mailbox mbox) throws ServiceException {
        dropMailboxFromGroup(conn, mbox);
    }

    public static void renameMailbox(Mailbox mbox, String newName) throws ServiceException {
        if (DebugConfig.externalMailboxDirectory) {
            return;
        }

        int mailboxId = mbox.getId();
        ZimbraLog.mailbox.info("Renaming email/comment of mailbox " + mailboxId + " to " + newName);

        DbConnection conn = mbox.getOperationConnection();

        try {
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement("UPDATE mailbox SET comment = ?, last_backup_at = NULL WHERE id = ?");
                stmt.setString(1, newName);
                stmt.setInt(2, mailboxId);
                stmt.executeUpdate();
            } finally {
                DbPool.closeStatement(stmt);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("renameMailbox(" + mailboxId + ")", e);
        }
    }

    public static void clearMailboxContactCount(Mailbox mbox) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET contact_count = NULL WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("clearing contact count for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void recordLastSoapAccess(Mailbox mbox) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET last_soap_access = ? WHERE id = ?");
            stmt.setInt(1, (int) (mbox.getLastSoapAccessTime() / 1000));
            stmt.setInt(2, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating last SOAP access time for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateMailboxStats(Mailbox mbox) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET item_id_checkpoint = ?, search_id_checkpoint = ?, contact_count = ?, change_checkpoint = ?," +
                    "  size_checkpoint = ?, new_messages = ? WHERE id = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getLastItemId());
            stmt.setInt(pos++, mbox.getLastSearchId());
            stmt.setInt(pos++, mbox.getContactCount());
            stmt.setInt(pos++, mbox.getLastChangeID());
            stmt.setLong(pos++, mbox.getSize());
            stmt.setInt(pos++, mbox.getRecentMessageCount());
            stmt.setInt(pos++, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("updating mailbox statistics for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void startTrackingSync(Mailbox mbox) throws ServiceException {
        setSyncCutoff(mbox, mbox.getLastChangeID());
    }

    public static void setSyncCutoff(Mailbox mbox, int cutoff) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET tracking_sync = ? WHERE id = ? AND tracking_sync < ?");
            stmt.setInt(1, cutoff);
            stmt.setInt(2, mbox.getId());
            stmt.setInt(3, cutoff);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on sync tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void startTrackingImap(Mailbox mbox) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET tracking_imap = 1 WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("turning on imap tracking for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static String getConfig(Mailbox mbox, String section) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT metadata FROM " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "section = ?");
            int pos = 1;
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setInt(pos++, mbox.getId());
            }
            stmt.setString(pos++, section);

            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting metadata section '" + section + "' in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateConfig(Mailbox mbox, String section, Metadata config) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            if (config == null) {
                stmt = conn.prepareStatement("DELETE FROM " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                        " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "section = ?");
                int pos = 1;
                if (!DebugConfig.disableMailboxGroups) {
                    stmt.setInt(pos++, mbox.getId());
                }
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
                    if (!DebugConfig.disableMailboxGroups) {
                        stmt.setInt(pos++, mbox.getId());
                    }
                    stmt.setString(pos++, section);
                    stmt.setString(pos++, config.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                        stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                                " SET metadata = ? WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "section = ?");
                        int pos = 1;
                        stmt.setString(pos++, config.toString());
                        if (!DebugConfig.disableMailboxGroups) {
                            stmt.setInt(pos++, mbox.getId());
                        }
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

    public static void deleteConfig(Mailbox mbox, String sectionPart) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + qualifyZimbraTableName(mbox, TABLE_METADATA) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "section like ?");
            int pos = 1;
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setInt(pos++, mbox.getId());
            }
            stmt.setString(pos++, sectionPart);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(
                    "delete metadata sections like '" + sectionPart + "' in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateVersion(Mailbox mbox, MailboxVersion version) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET version = ?" +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE id = ?"));
            int pos = 1;
            stmt.setString(pos++, version == null ? null : version.toString());
            pos = DbMailItem.setMailboxId(stmt, mbox, pos++);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("setting mailbox version to '" + version + "' in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void updateLastPurgeAt(Mailbox mbox, long lastPurgeAt) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET last_purge_at = ? WHERE id = ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (lastPurgeAt / 1000));
            pos = DbMailItem.setMailboxId(stmt, mbox, pos++);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("setting mailbox last_purge_at to '" + (int) (lastPurgeAt / 1000) + "' in mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static void incrementItemcacheCheckpoint(Mailbox mbox) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("UPDATE " + qualifyZimbraTableName(mbox, TABLE_MAILBOX) +
                    " SET itemcache_checkpoint = itemcache_checkpoint + 1 WHERE id = ?");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos++);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("incrementing mailbox itemcache_checkpoint", e);
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
    public static Map<String, Integer> listMailboxes(DbConnection conn) throws ServiceException {
        return listMailboxes(conn, MailboxManager.getInstance());
    }

    public static Map<String, Integer> listMailboxes(DbConnection conn, MailboxManager mmgr) throws ServiceException {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        if (DebugConfig.externalMailboxDirectory) {
            return result;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT account_id, id FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next()) {
                result.put(rs.getString(1).toLowerCase(), rs.getInt(2));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching mailboxes", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static int getMailboxId(DbConnection conn, String accountId) throws ServiceException {

        if (DebugConfig.externalMailboxDirectory) {
            return -1;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM mailbox WHERE account_id = ?");
            int pos = 1;
            stmt.setString(pos++, accountId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching mailboxId", e);
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
    public static Map<String, Long> getMailboxSizes(DbConnection conn, List<Integer> mailboxIds) throws ServiceException {
        HashMap<String, Long> sizes = new HashMap<String, Long>();
        if (DebugConfig.externalMailboxDirectory) {
            return sizes;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (!DebugConfig.disableMailboxGroups) {
                stmt = conn.prepareStatement("SELECT account_id, size_checkpoint FROM mailbox");
                rs = stmt.executeQuery();
                while (rs.next()) {
                    sizes.put(rs.getString(1).toLowerCase(), rs.getLong(2));
                }
            } else {
                for (int mailboxId : mailboxIds) {
                    // note that if groups are disabled, mailboxId == groupId
                    Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(mailboxId));

                    stmt = conn.prepareStatement("SELECT account_id, size_checkpoint FROM " + qualifyZimbraTableName(mailboxId, TABLE_MAILBOX));
                    rs = stmt.executeQuery();
                    while (rs.next()) {
                        sizes.put(rs.getString(1).toLowerCase(), rs.getLong(2));
                    }
                    rs.close();
                    rs = null;
                    stmt.close();
                    stmt = null;

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

    /**
     * Returns IDs of mailboxes on which the last purge was run before the given time.
     *
     * @param conn An open database connection.
     * @param time Cut-off time in milliseconds.
     * @return A <code>Set</code> of mailbox IDs.
     * @throws ServiceException
     */

    public static Set<Integer> listPurgePendingMailboxes(DbConnection conn, long time) throws ServiceException {
        Set<Integer> result = new HashSet<Integer>();
        if (DebugConfig.externalMailboxDirectory) {
            return result;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM mailbox WHERE last_purge_at < ?");
            int pos = 1;
            stmt.setInt(pos++, (int) (time / 1000));
            rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getInt(1));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching purge pending mailboxes", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    //id checkpoint increments are disabled in Zimbra X, since they can result
    //in ids not being monotonically increasing, leading to collisions
    public static final int CHANGE_CHECKPOINT_INCREMENT = 1;
    public static final int ITEM_CHECKPOINT_INCREMENT   = 1;
    public static final int SEARCH_ID_CHECKPOINT_INCREMENT = 1;

    public static Mailbox.MailboxData getMailboxStats(DbConnection conn, int mailboxId) throws ServiceException {
        // no locking check because it's a mailbox-level op done before the Mailbox object is instantiated...

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (DebugConfig.disableMailboxGroups) {
                Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(mailboxId));
            }

            // note that if groups are disabled, mailboxId == groupId
            stmt = conn.prepareStatement(
                    "SELECT account_id," + (DebugConfig.disableMailboxGroups ? mailboxId : " group_id") + "," +
                    " size_checkpoint, contact_count, item_id_checkpoint, change_checkpoint, tracking_sync," +
                    " tracking_imap, index_volume_id, last_soap_access, new_messages, version, itemcache_checkpoint," +
                    " search_id_checkpoint, backup_host_id" +
                    " FROM " + qualifyZimbraTableName(mailboxId, TABLE_MAILBOX) + " WHERE id = ?");
            stmt.setInt(1, mailboxId);

            rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            int pos = 1;
            Mailbox.MailboxData mbd = new Mailbox.MailboxData();
            mbd.id            = mailboxId;
            mbd.accountId     = rs.getString(pos++).toLowerCase();
            mbd.schemaGroupId = rs.getInt(pos++);
            mbd.size          = rs.getLong(pos++);
            if (rs.wasNull()) {
                mbd.size = -1;
            }
            mbd.contacts      = rs.getInt(pos++);
            if (rs.wasNull()) {
                mbd.contacts = -1;
            }
            mbd.lastItemId    = rs.getInt(pos++);
            mbd.lastChangeId  = rs.getInt(pos++);
            mbd.trackSync     = rs.getInt(pos++);
            mbd.trackImap     = rs.getBoolean(pos++);
            mbd.indexVolumeId = rs.getShort(pos++);
            mbd.lastWriteDate = rs.getInt(pos++);
            mbd.recentMessages = rs.getInt(pos++);
            mbd.lastBackupDate = -1;

            String version = rs.getString(pos++);
            if (version != null) {
                mbd.version = MailboxVersion.parse(version);
            }
            mbd.itemcacheCheckpoint = rs.getInt(pos++);
            mbd.lastSearchId = rs.getInt(pos++);
            mbd.backupHostId = rs.getInt(pos++);

            // round lastItemId, lastChangeId, and lastSearchId up so that they get written on the next change
            mbd.lastItemId += ITEM_CHECKPOINT_INCREMENT - 1;
            mbd.lastChangeId += CHANGE_CHECKPOINT_INCREMENT - 1;
            mbd.lastSearchId += SEARCH_ID_CHECKPOINT_INCREMENT - 1;
            int rounding = mbd.lastItemId % ITEM_CHECKPOINT_INCREMENT;
            if (rounding != ITEM_CHECKPOINT_INCREMENT - 1) {
                mbd.lastItemId -= rounding + 1;
            }
            rounding = mbd.lastChangeId % CHANGE_CHECKPOINT_INCREMENT;
            if (rounding != CHANGE_CHECKPOINT_INCREMENT - 1) {
                mbd.lastChangeId -= rounding + 1;
            }
            rounding = mbd.lastSearchId % SEARCH_ID_CHECKPOINT_INCREMENT;
            if (rounding != SEARCH_ID_CHECKPOINT_INCREMENT - 1) {
                mbd.lastSearchId -= rounding + 1;
            }

            rs.close();
            rs = null;
            stmt.close();
            stmt = null;

            stmt = conn.prepareStatement("SELECT section FROM " + qualifyZimbraTableName(mailboxId, TABLE_METADATA) +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = ?"));
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setInt(1, mailboxId);
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (mbd.configKeys == null) {
                    mbd.configKeys = new HashSet<String>();
                }
                mbd.configKeys.add(rs.getString(1));
            }

            return mbd;
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.NO_SUCH_TABLE)) {
                return null;
            }
            throw ServiceException.FAILURE("fetching stats on mailbox " + mailboxId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static int calculateMailboxGroupId(int mailboxId) {
        int groups = DebugConfig.numMailboxGroups;
        // -1 / +1 operations are done so that the group id is never 0.
        return (mailboxId - 1) % groups + 1;
    }

    /** Returns the name of the database that contains tables for the
     *  specified <code>mailboxId</code>.  As a side effect, also registers
     *  interest on that database with the specified {@link DbConnection} (if
     *  not <tt>null</tt>). */
    public static String getDatabaseName(Mailbox mbox) {
        return getDatabaseName(mbox.getSchemaGroupId());
    }

    public static String getDatabaseName(int groupId) {
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
    public static String qualifyZimbraTableName(int mailboxId, String tableName) {
        // note that if groups are disabled, mailboxId == groupId
        return DebugConfig.disableMailboxGroups ? qualifyTableName(mailboxId, tableName) : tableName;
    }

    public static String qualifyTableName(Mailbox mbox, String tableName) {
        return qualifyTableName(mbox.getSchemaGroupId(), tableName);
    }

    public static String qualifyTableName(int groupId, String tableName) {
        return DB_PREFIX_MAILBOX_GROUP + groupId + '.' + tableName;
    }


    public static void removeFromDeletedAccount(DbConnection conn, String email)
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

    private static void addToDeletedAccount(DbConnection conn, Mailbox mbox) throws ServiceException {
        if (DebugConfig.externalMailboxDirectory) {
            return;
        }

        // Get email address for mailbox by querying the mailbox table.  We can't get it by
        // calling mbox.getAccount().getName() because the account was already deleted from LDAP.
        String email = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT comment FROM mailbox WHERE id = ?");
            stmt.setInt(1, mbox.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {
                email = rs.getString(1);
            } else {
                throw ServiceException.FAILURE("no email address found for mailbox " + mbox.getId(), null);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting email address for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        boolean supportsReplaceInto = Db.supports(Db.Capability.REPLACE_INTO);
        if (!supportsReplaceInto) {
            removeFromDeletedAccount(conn, email);
        }

        try {
            // add the mailbox's account to deleted_account table
            String command = supportsReplaceInto ? "REPLACE" : "INSERT";
            stmt = conn.prepareStatement(
                    command + " INTO deleted_account (email, account_id, mailbox_id, deleted_at, backup_host_id) " +
                    "SELECT ?, account_id, id, ?, backup_host_id FROM mailbox WHERE id = ?");
            stmt.setString(1, email.toLowerCase());
            stmt.setInt(2, (int) (System.currentTimeMillis() / 1000));
            stmt.setInt(3, mbox.getId());
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
    public static DeletedAccount getDeletedAccount(DbConnection conn, String email)
    throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(
                    "SELECT email, account_id, mailbox_id, deleted_at, backup_host_id " +
                    "FROM deleted_account WHERE email = ?");
            stmt.setString(1, email.toLowerCase());
            rs = stmt.executeQuery();
            if (rs.next()) {
                String emailCol = rs.getString(1);
                String accountId = rs.getString(2).toLowerCase();
                int mailboxId = rs.getInt(3);
                long deletedAt = rs.getLong(4) * 1000;
                int backupHostId = rs.getInt(5);
                return new DeletedAccount(emailCol, accountId, mailboxId, deletedAt, backupHostId);
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
        private final String mEmail;
        private final String mAccountId;
        private final int mMailboxId;
        private final long mDeletedAt;
        private final int mBackupHostId;

        public DeletedAccount(String email, String accountId, int mailboxId, long deletedAt, int backupHostId) {
            mEmail = email;
            mAccountId = accountId;
            mMailboxId = mailboxId;
            mDeletedAt = deletedAt;
            mBackupHostId = backupHostId;
        }

        public String getEmail() {
            return mEmail;
        }

        public String getAccountId() {
            return mAccountId;
        }

        public int getMailboxId() {
            return mMailboxId;
        }

        public long getDeletedAt() {
            return mDeletedAt;
        }

        public int getBackupHostId() {
            return mBackupHostId;
        }
    }

    /**
     * Deletes the row for the specified mailbox from the <code>mailbox</code> table.
     *
     * @throws ServiceException if the database operation failed
     */
    public static void deleteMailbox(DbConnection conn, Mailbox mbox) throws ServiceException {
        addToDeletedAccount(conn, mbox);

        PreparedStatement stmt = null;
        try {
            if (DebugConfig.disableMailboxGroups) {
                // remove entry from MBOXGROUP database (is this redundant?)
                stmt = conn.prepareStatement("DELETE FROM " + qualifyTableName(mbox, TABLE_MAILBOX) +
                        " WHERE id = ?");
                stmt.setInt(1, mbox.getId());
                stmt.executeUpdate();
                stmt.close();
            }

            // remove entry from mailbox table
            if (!DebugConfig.externalMailboxDirectory) {
                stmt = conn.prepareStatement("DELETE FROM mailbox WHERE id = ?");
                stmt.setInt(1, mbox.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Returns the account id's for the current server.
     */
    public static Set<String> listAccountIds(DbConnection conn) throws ServiceException {
        Set<String> accountIds = new HashSet<String>();
        if (DebugConfig.externalMailboxDirectory) {
            return accountIds;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT account_id FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next()) {
                accountIds.add(rs.getString(1).toLowerCase());
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct account ids", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return accountIds;
    }

    public static List<Mailbox.MailboxData> getMailboxRawData(DbConnection conn) throws ServiceException {
        List<Mailbox.MailboxData> results = new ArrayList<Mailbox.MailboxData>();
        if (DebugConfig.externalMailboxDirectory) {
            return results;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (!DebugConfig.disableMailboxGroups) {
                stmt = conn.prepareStatement(
                        "SELECT id, group_id, account_id, index_volume_id, item_id_checkpoint," +
                        " contact_count, size_checkpoint, change_checkpoint, tracking_sync," +
                        " tracking_imap, last_backup_at, last_soap_access, new_messages FROM mailbox");
                rs = stmt.executeQuery();
                readMailboxRawData(results, rs);
            } else {
                // FIXME: need an (impossible) assert for synchronization purposes

                int[] mailboxIds = MailboxManager.getInstance().getMailboxIds();
                for (int mailboxId : mailboxIds) {
                    Db.getInstance().registerDatabaseInterest(conn, getDatabaseName(mailboxId));

                    stmt = conn.prepareStatement(
                            "SELECT id, id, account_id, index_volume_id, item_id_checkpoint, contact_count, size_checkpoint," +
                            " change_checkpoint, tracking_sync, tracking_imap, -1, last_soap_access, new_messages" +
                            " FROM " + qualifyZimbraTableName(mailboxId, TABLE_MAILBOX));
                    rs = stmt.executeQuery();
                    readMailboxRawData(results, rs);
                    rs.close();
                    rs = null;
                    stmt.close();
                    stmt = null;

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

    public static void optimize(DbConnection conn, Mailbox mbox, int level, MailboxLock l) throws ServiceException {
        assert(l.isWriteLockedByCurrentThread());

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
            data.id = rs.getInt(pos++);
            data.schemaGroupId = rs.getInt(pos++);
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
            results.add(data);
        }
    }


    public static Set<Integer> getMboxGroupIds(DbConnection conn) throws ServiceException {
        Set<Integer> groups = new HashSet<Integer>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT DISTINCT group_id FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next()) {
                groups.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting distinct account ids", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return groups;
    }

    public static Integer getMailboxCount(DbConnection conn) throws ServiceException {
       Integer mailboxesCount = 0;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(id) FROM mailbox");
            rs = stmt.executeQuery();
            while (rs.next()) {
                mailboxesCount = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting mailbox count", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        return mailboxesCount;
    }

    public static Integer getMailboxKey(DbConnection conn, String accountId) throws ServiceException {

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM mailbox WHERE account_id = ?");
            int pos = 1;
            stmt.setString(pos++, accountId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching mailboxId", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static String getAccountIdByMailboxId(DbConnection conn, Integer mailboxId) throws ServiceException {

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT account_id FROM mailbox WHERE id = ?");
            int pos = 1;
            stmt.setInt(pos++, mailboxId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error fetching account ID for mailbox %s", mailboxId), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }


    public static void setBackupHostForAccount(DbConnection conn, String accountId, BackupHost host) throws ServiceException {
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        try {
            String sql = "UPDATE mailbox SET backup_host_id = ? WHERE account_id = ?";
            stmt1 = conn.prepareStatement(sql);
            stmt1.setInt(1, host.getHostId());
            stmt1.setString(2, accountId);
            int affected = stmt1.executeUpdate();
            if (affected == 0 ) {
                // perhaps there is a pending entry
                sql = "UPDATE pending_backup_host_assignments SET backup_host_id = ? WHERE account_id = ?";
                stmt2 = conn.prepareStatement(sql);
                stmt2.setInt(1, host.getHostId());
                stmt2.setString(2, accountId);
                stmt2.executeUpdate();
            } else {
                deletePendingBackupHostAssignment(conn, accountId);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error mapping %s to backup host %s", accountId, host.getHost()), e);
        } finally {
            DbPool.closeStatement(stmt1);
            DbPool.closeStatement(stmt2);
        }
    }

    public static void setPendingBackupHostForAccount(String accountId, String email, BackupHost host) throws ServiceException {
        PreparedStatement stmt = null;
        DbConnection conn = null;
        boolean supportsOnDuplicateKey = Db.supports(Db.Capability.ON_DUPLICATE_KEY);
        try {
            StringBuilder sql = new StringBuilder("INSERT INTO pending_backup_host_assignments (account_id, email, backup_host_id) values (?, ?, ?)");
            if (supportsOnDuplicateKey) {
                sql.append(" ON DUPLICATE KEY UPDATE backup_host_id = ?");
            }
            conn = DbPool.getConnection();
            if (!supportsOnDuplicateKey) {
                // first delete any existing mapping
                deletePendingBackupHostAssignment(conn, accountId);
            }
            stmt = conn.prepareStatement(sql.toString());
            stmt.setString(1, accountId);
            stmt.setString(2, email);
            stmt.setInt(3, host.getHostId());
            if (supportsOnDuplicateKey) {
                stmt.setInt(4, host.getHostId());
            }
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error adding %s as a pending backup host for account %s (%s)", host.getHost(), accountId, email), e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static BackupHost getPendingBackupHostAssignment(String accountId) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DbConnection conn = null;
        try {
            String sql = "SELECT id, host, created_at, flags FROM backup_hosts WHERE id = (SELECT backup_host_id from pending_backup_host_assignments where account_id = ?)";
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return DbBackupHosts.toBackupHost(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error getting pending backup host assignment for account %s", accountId), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    private static void deletePendingBackupHostAssignment(DbConnection conn, String accountId) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            String sql = "DELETE FROM pending_backup_host_assignments WHERE account_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error deleting pending backup host assignment for account %s", accountId), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    public static List<String> getAccountsForBackupHost(BackupHost host, boolean includeDeleted) throws ServiceException {
        DbConnection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            List<String> accountIds = new ArrayList<>();
            conn = DbPool.getConnection();
            StringBuilder sql = new StringBuilder("SELECT account_id FROM mailbox WHERE backup_host_id = ?");
            sql.append(" UNION SELECT account_id from pending_backup_host_assignments WHERE backup_host_id = ?");
            if (includeDeleted) {
                sql.append(" UNION SELECT account_id from delete_account WHERE backup_host_id = ?");
            }
            stmt = conn.prepareStatement(sql.toString());
            stmt.setInt(1, host.getHostId());
            stmt.setInt(2, host.getHostId());
            if (includeDeleted) {
                stmt.setInt(3, host.getHostId());
            }
            rs = stmt.executeQuery();
            while (rs.next()) {
                accountIds.add(rs.getString(1));
            }
            return accountIds;
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error getting accounts mapped to host %s", host.getHost()), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static BackupHost getBackupHostForAccount(DbConnection conn, String accountId, boolean checkDeleted) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        PreparedStatement delStmt = null;
        ResultSet delRs = null;
        try {
            String sql = "SELECT id, host, created_at, flags FROM backup_hosts WHERE id = (SELECT backup_host_id from mailbox where account_id = ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return DbBackupHosts.toBackupHost(rs);
            } else if (checkDeleted) {
                sql = "SELECT id, host, created_at, flags FROM backup_hosts WHERE id = (SELECT backup_host_id from deleted_account where account_id = ?)";
                delStmt = conn.prepareStatement(sql);
                delStmt.setString(1, accountId);
                delRs = delStmt.executeQuery();
                if (delRs.next()) {
                    return DbBackupHosts.toBackupHost(delRs);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(String.format("error getting backup host for account %s", accountId), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeResults(delRs);
            DbPool.closeStatement(stmt);
            DbPool.closeStatement(delStmt);
        }
    }
}
