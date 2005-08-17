/**
 * @author apalaniswamy
 */
package com.liquidsys.coco.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.db.DbPool.Connection;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.Constants;

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
     * @param numDays the number of days
     * @throws ServiceException if a database error occrurs
     */
    public static boolean alreadySent(
            Connection conn, Mailbox mbox, String sentTo, int numDays)
    throws ServiceException
    {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        sentTo = sentTo.toLowerCase();
        boolean result = false;
        Timestamp cutoff = new Timestamp(
            System.currentTimeMillis() - (numDays * Constants.MILLIS_PER_DAY));
        
        try {
            stmt = conn.prepareStatement(
                    "SELECT COUNT(*) " +
                    "FROM " + TABLE_NAME + " " +
                    "WHERE mailbox_id = ? " +
                    "AND sent_to = ? " +
                    "AND sent_on > ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, sentTo);
            stmt.setTimestamp(3, cutoff);
            
            rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            result = (count > 0);
        } catch (SQLException e) {
            throw ServiceException.FAILURE(
                    "DbOutOfOffice.getSentTime: sql exception (mailbox_id="
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
    public static void setSentTime(Connection conn, Mailbox mbox,
            String sentTo, long sentOn) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + "(mailbox_id, sent_to, sent_on) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE sent_on = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, sentTo);
            Timestamp ts = new Timestamp(sentOn);
            stmt.setTimestamp(3, ts);
            stmt.setTimestamp(4, ts);
            int num = stmt.executeUpdate();
            if (num > 0) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("DbOutOfOffice.setSentTime: ok (mailbox_id="
                            + mbox.getId() + " sent_to=" + sentTo + " sent_on="
                            + sentOn + " rows=" + num + ")");
                }
            } else {
                mLog.error("DbOutOfOffice.setSentTime: no rows updated (mailbox_id="
                        + mbox.getId()
                        + " sent_to="
                        + sentTo
                        + " sent_on=" + sentOn + " rows=" + num + ")");
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE(
                    "DbOutOfOffice.setSentTime: sql exception (mailbox_id="
                    + mbox.getId() + " sent_to" + sentTo + " sent_on="
                    + sentOn + ")", e);
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
    public static void clear(Connection conn, Mailbox mbox)
            throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE mailbox_id = ?");
            stmt.setInt(1, mbox.getId());
            int num = stmt.executeUpdate();
            mLog.debug("DbOutOfOffice.clear() mbox=" + mbox + " rows=" + num);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("DbOutOfOffice.clear mbox="
                    + mbox.getId(), e);
        } finally {
            DbPool.closeStatement(stmt);
        }
    }
    
    public static void prune(Connection conn, int numDays)
    throws ServiceException
    {
        PreparedStatement stmt = null;
        try {
            Timestamp cutoff = new Timestamp(
                System.currentTimeMillis() - Constants.MILLIS_PER_DAY * numDays);
            stmt = conn.prepareStatement(
                    "DELETE FROM " + TABLE_NAME + " " +
                    "WHERE sent_on <= ?");
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