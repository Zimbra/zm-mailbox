/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

/**
 * @author apalaniswamy
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.db.DbPool.Connection;
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
     * @throws ServiceException if a database error occrurs
     */
    public static boolean alreadySent(Connection conn, Mailbox mbox, String sentTo, long cacheDurationMillis)
    throws ServiceException {
        sentTo = sentTo.toLowerCase();
        boolean result = false;
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - cacheDurationMillis);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + TABLE_NAME +
                    " WHERE mailbox_id = ? AND sent_to = ? AND sent_on > ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, sentTo);
            stmt.setTimestamp(3, cutoff);
            
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
                        ".  mailbox_id=" + mbox.getId() + ", sent_to='" + sentTo + "'");
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
    public static void setSentTime(Connection conn, Mailbox mbox, String sentTo)
    throws ServiceException
    {
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
    public static void setSentTime(Connection conn, Mailbox mbox, String sentTo, long sentOn) throws ServiceException {
        Timestamp ts = new Timestamp(sentOn);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + TABLE_NAME + "(mailbox_id, sent_to, sent_on) VALUES (?, ?, ?) " +
                    (Db.supports(Db.Capability.ON_DUPLICATE_KEY) ? "ON DUPLICATE KEY UPDATE sent_on = ?" : ""));
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, sentTo.toLowerCase());
            stmt.setTimestamp(3, ts);
            if (Db.supports(Db.Capability.ON_DUPLICATE_KEY))
                stmt.setTimestamp(4, ts);
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
            if (!Db.supports(Db.Capability.ON_DUPLICATE_KEY) && Db.errorMatches(e, Db.Error.DUPLICATE_ROW)) {
                try {
                    stmt.close();

                    stmt = conn.prepareStatement("UPDATE " + TABLE_NAME +
                            " SET sent_on = ?" +
                            " WHERE mailbox_id = ? AND sent_to = ?");
                    stmt.setTimestamp(1, ts);
                    stmt.setInt(2, mbox.getId());
                    stmt.setString(3, sentTo.toLowerCase());
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
    public static void clear(Connection conn, String accountId) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM zimbra.mailbox WHERE account_id = ?");
            stmt.setString(1, accountId);
            rs = stmt.executeQuery();
            if (!rs.next())
                throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
            int mailboxId = rs.getInt(1);
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE mailbox_id = ?");
            stmt.setInt(1, mailboxId);
            int num = stmt.executeUpdate();
            mLog.debug("DbOutOfOffice.clear() mbox=" + mailboxId + " rows=" + num);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("DbOutOfOffice.clear acctId=" + accountId, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }
    
    public static void prune(Connection conn, long cacheDurationMillis)
    throws ServiceException {
        PreparedStatement stmt = null;
        try {
            Timestamp cutoff = new Timestamp(System.currentTimeMillis() - cacheDurationMillis);
            stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE sent_on <= ?");
            stmt.setTimestamp(1, cutoff);
            int num = stmt.executeUpdate();
            if (mLog.isDebugEnabled()) {
                mLog.debug("DbOutOfOffice.prune() deleted " + num + " rows");
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("DbOutOfOffice.prune()", e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
}
