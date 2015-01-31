package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.RedoableOp;

public interface RedoLogManager {

    public abstract boolean getInCrashRecovery();

    /**
     * Start the log manager
     * @throws ServiceException
     */
    public abstract void start() throws ServiceException;

    /**
     * Stop the log manager
     */
    public abstract void stop();

    /**
     * Get a transaction ID
     */
    public abstract TransactionId getNewTxnId();

    /**
     * Log an operation
     * @param op
     * @param synchronous
     * @throws ServiceException
     */
    public abstract void log(RedoableOp op, boolean synchronous) throws ServiceException;

    /**
     * Logs the COMMIT record for an operation.
     * @param op
     * @throws ServiceException
     */
    public abstract void commit(RedoableOp op) throws ServiceException;

    /**
     * Logs the ABORT record for an operation
     * @param op
     * @throws ServiceException
     */
    public abstract void abort(RedoableOp op) throws ServiceException;

    /**
     * Flush the log buffer (if applicable)
     * @throws IOException
     */
    public abstract void flush() throws IOException;

    /**
     * Rollover the log immediately
     * @throws ServiceException
     */
    public abstract void forceRollover() throws ServiceException;

    /**
     * Rollover the log immediately
     * @param skipCheckpoint
     * @throws ServiceException
     */
    public abstract void forceRollover(boolean skipCheckpoint) throws ServiceException;

    /**
     * Log an operation to the logger.  Only does logging; doesn't
     * bother with checkpoint, rollover, etc.
     * @param op
     * @param synchronous
     * @throws ServiceException
     */
    public abstract void logOnly(RedoableOp op, boolean synchronous) throws ServiceException;

    /**
     * Get the current RolloverManager
     */
    public abstract RolloverManager getRolloverManager();

    /**
     * Get the current log sequence number
     */
    public abstract long getCurrentLogSequence();

    /**
     * Create a new log writer with specified sync/flush interval
     * @param fsyncIntervalMS
     */
    public abstract LogWriter createLogWriter(long fsyncIntervalMS);
    //TODO: fsyncinterval smells like file to me; rename or move out of interface..

    /**
     * Get the current log writer
     */
    public abstract LogWriter getLogWriter();

    /**
     * Returns the set of mailboxes that had any committed changes since a
     * particular CommitId in the past, by scanning redologs.  Also returns
     * the last CommitId seen during the scanning process.
     * @param cid
     * @return can be null if server is shutting down
     * @throws IOException
     * @throws MailServiceException
     */
    public abstract Pair<Set<Integer>, CommitId> getChangedMailboxesSince(
            CommitId cid) throws IOException, MailServiceException;

    /**
     * Get a copy of the current log file. This may be a direct link to the file or a locally cached copy if the original data is stored remotely
     */
    public abstract File getLogFile();

    /**
     * Get the archived log files starting with a given sequence number
     * @param seq
     * @throws IOException
     */
    public abstract File[] getArchivedLogsFromSequence(long seq)
            throws IOException;

    /**
     * Get all the archived log files
     * @throws IOException
     */
    public abstract File[] getArchivedLogs() throws IOException;

}