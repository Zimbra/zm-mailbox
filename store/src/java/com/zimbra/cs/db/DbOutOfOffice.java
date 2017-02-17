/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/**
 * @author apalaniswamy
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;

// TODO mailbox migration between servers
// TODO backup/restore
// TODO redo logging!
// TODO have to do GC for sanity

public class DbOutOfOffice {
    public static String TABLE_NAME = "out_of_office";
    private static Log mLog = LogFactory.getLog(DbOutOfOffice.class);

    /**
     * Determines whether an out-of-office reply has already been sent to the
     * specified address within the last <code>numDays</code> days.
     *
     * @param conn the database connection
     * @param mbox the sender's mailbox
     * @param sentTo the recipient's email address
     * @param cacheDurationMillis threshold for determining last sent time
     * @throws ServiceException if a database error occurs
     */
    public static boolean alreadySent(DbConnection conn, Mailbox mbox, String sentTo, long cacheDurationMillis)
    throws ServiceException {
        sentTo = sentTo.toLowerCase();
        boolean result = false;
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - cacheDurationMillis);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + DbMailbox.qualifyZimbraTableName(mbox, TABLE_NAME) +
                    " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "sent_to = ? AND sent_on > ?");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, sentTo);
            stmt.setTimestamp(pos++, cutoff);

            rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            result = (count > 0);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("DbOutOfOffice.getSentTime: sql exception (mailbox_id="
                            + mbox.getId() + " sent_to=" + sentTo + ")", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }

        if (mLog.isDebugEnabled()) {
            mLog.debug("DbOutOfOffice.alreadySent() returning " + result +
                        ".  mailbox_id=" + mbox.getId() + ", sent_to='" + sentTo + "' cutoff=" + cutoff.getTime());
        }
        return result;
    }

    /**
     * Stores a row in the out_of_office table, indicating that we sent an
     * out-of-office reply to the specified address.
     *
     * @param conn the database connection
     * @param mbox the mailbox of the sender
     * @param sentTo the email address of the recipient
     * @throws ServiceException if a database error occurred
     */
    public static void setSentTime(DbConnection conn, Mailbox mbox, String sentTo)
    throws ServiceException {
        setSentTime(conn, mbox, sentTo, System.currentTimeMillis());
    }

    /**
     * Stores a row in the out_of_office table, indicating that we sent an
     * out-of-office reply to the specified address.
     *
     * @param conn the database connection
     * @param mbox the mailbox of the sender
     * @param sentTo the email address of the recipient
     * @param sentOn the timestamp of the out-of-office message
     * @throws ServiceException if a database error occurred
     */
    public static void setSentTime(DbConnection conn, Mailbox mbox, String sentTo, long sentOn) throws ServiceException {
        Timestamp ts = new Timestamp(sentOn);

        PreparedStatement stmt = null;
        try {
            String command = Db.supports(Db.Capability.REPLACE_INTO) ? "REPLACE" : "INSERT";
            stmt = conn.prepareStatement(command + " INTO " + DbMailbox.qualifyZimbraTableName(mbox, TABLE_NAME) +
                    "(" + DbMailItem.MAILBOX_ID + "sent_to, sent_on)" +
                    " VALUES (" + DbMailItem.MAILBOX_ID_VALUE +" ?, ?) ");
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            stmt.setString(pos++, sentTo.toLowerCase());
            stmt.setTimestamp(pos++, ts);
            int num = stmt.executeUpdate();
            if (num > 0) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("DbOutOfOffice.setSentTime: ok (mailbox_id=" + mbox.getId() +
                            " sent_to=" + sentTo + " sent_on=" + sentOn + " rows=" + num + ")");
                }
            } else {
                mLog.error("DbOutOfOffice.setSentTime: no rows updated (mailbox_id=" + mbox.getId()
                        + " sent_to=" + sentTo + " sent_on=" + sentOn + " rows=" + num + ")");
            }
        } catch (SQLException e) {
            if (Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                try {
                    stmt.close();

                    stmt = conn.prepareStatement("UPDATE " + DbMailbox.qualifyZimbraTableName(mbox, TABLE_NAME) +
                            " SET sent_on = ?" +
                            " WHERE " + DbMailItem.IN_THIS_MAILBOX_AND + "sent_to = ?");
                    int pos = 1;
                    stmt.setTimestamp(pos++, ts);
                    pos = DbMailItem.setMailboxId(stmt, mbox, pos);
                    stmt.setString(pos++, sentTo.toLowerCase());
                    stmt.executeUpdate();
                } catch (SQLException nested) {
                    throw ServiceException.FAILURE("DbOutOfOffice.setSentTime: sql exception " +
                            "(mailbox_id=" + mbox.getId() + " sent_to" + sentTo + " sent_on=" + sentOn + ")", nested);
                }
            } else {
                throw ServiceException.FAILURE("DbOutOfOffice.setSentTime: sql exception " +
                        "(mailbox_id=" + mbox.getId() + " sent_to" + sentTo + " sent_on=" + sentOn + ")", e);
            }
        } finally {
            DbPool.closeStatement(stmt);
        }
    }

    /**
     * Clears entries from the <code>out_of_office</code> table for the
     * specified mailbox.
     *
     * @param conn database connection
     * @param mbox mailbox
     * @throws ServiceException if a database error occurred
     */
    public static void clear(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + DbMailbox.qualifyZimbraTableName(mbox, TABLE_NAME) +
                    (DebugConfig.disableMailboxGroups ? "" : " WHERE mailbox_id = ?"));
            DbMailItem.setMailboxId(stmt, mbox, 1);
            int num = stmt.executeUpdate();

            mLog.debug("DbOutOfOffice.clear() mbox=" + mbox.getId() + " rows=" + num);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("DbOutOfOffice.clear acctId=" + mbox.getAccountId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    public static void prune(DbConnection conn, long cacheDurationMillis)
    throws ServiceException {
        // there's no centralized OoO table to prune in the DB-per-user case
        if (DebugConfig.disableMailboxGroups)
            return;

        PreparedStatement stmt = null;
        try {
            Timestamp cutoff = new Timestamp(System.currentTimeMillis() - cacheDurationMillis);
            stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE sent_on <= ?");
            stmt.setTimestamp(1, cutoff);
            int num = stmt.executeUpdate();

            if (mLog.isDebugEnabled())
                mLog.debug("DbOutOfOffice.prune() deleted " + num + " rows with cutoff <= " + cutoff.getTime());
        } catch (SQLException e) {
            throw ServiceException.FAILURE("DbOutOfOffice.prune()", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
}
