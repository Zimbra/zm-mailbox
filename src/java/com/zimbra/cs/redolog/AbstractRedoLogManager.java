/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.AbortTxn;
import com.zimbra.cs.redolog.op.Checkpoint;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.util.Zimbra;

public abstract class AbstractRedoLogManager implements RedoLogManager {

    protected static class TxnIdGenerator {
            private int mTime;
            private int mCounter;

            public TxnIdGenerator() {
                init();
            }

            private void init() {
                mTime = (int) (System.currentTimeMillis() / 1000);
                mCounter = 1;
            }

            public synchronized TransactionId getNext() {
                TransactionId tid = new TransactionId(mTime, mCounter);
                if (mCounter < 0x7fffffffL)
                    mCounter++;
                else
                    init();
                return tid;
            }
        }

    protected boolean mEnabled;
    private boolean mInCrashRecovery;
    private final Object mInCrashRecoveryGuard = new Object();
    protected boolean mShuttingDown;
    protected final Object mShuttingDownGuard = new Object();
    protected boolean mInPostStartupCrashRecovery;
    protected boolean mSupportsCrashRecovery;
    protected boolean mRecoveryMode;
    protected ReentrantReadWriteLock mRWLock;
    protected LinkedHashMap<TransactionId, RedoableOp> mActiveOps;
    private long mLogRolloverMinAgeMillis;
    private long mLogRolloverSoftMaxBytes;
    private long mLogRolloverHardMaxBytes;
    protected TxnIdGenerator mTxnIdGenerator;
    protected RolloverManager mRolloverMgr;
    private long mInitialLogSize;
    protected LogWriter mLogWriter;
    protected Object mStatGuard;
    protected long mElapsed;
    protected int mCounter;

    public AbstractRedoLogManager() {
        super();
        mTxnIdGenerator = new TxnIdGenerator();

        mEnabled = false;
        mShuttingDown = false;
        mRecoveryMode = false;

        mRWLock = new ReentrantReadWriteLock();
        mActiveOps = new LinkedHashMap<TransactionId, RedoableOp>(100);
        mTxnIdGenerator = new TxnIdGenerator();

        mElapsed = 0;
        mCounter = 0;
        mStatGuard = new Object();
        mElapsed = 0;
        mCounter = 0;

    }

    @Override
    public LogWriter getLogWriter() {
        return mLogWriter;
    }

    private void setInCrashRecovery(boolean b) {
        synchronized (mInCrashRecoveryGuard) {
            mInCrashRecovery = b;
        }
    }

    @Override
    public boolean getInCrashRecovery() {
        synchronized (mInCrashRecoveryGuard) {
            return mInCrashRecovery;
        }
    }

    protected abstract void initRedoLog() throws IOException;

    @Override
    public synchronized void start() throws ServiceException {
        mEnabled = true;

        try {
            initRedoLog();
        } catch (IOException e) {
            signalLogError(e);
        }

        setInCrashRecovery(true);

        // Recover from crash during rollover.  We do this even when
        // mSupportsCrashRecovery is false.
        try {
            mRolloverMgr.crashRecovery();
        } catch (IOException e) {
            ZimbraLog.redolog.fatal("Exception during crash recovery");
            signalLogError(e);
        }

        long fsyncInterval = RedoConfig.redoLogFsyncIntervalMS();
        mLogWriter = createLogWriter(fsyncInterval);

        ArrayList<RedoableOp> postStartupRecoveryOps = new ArrayList<RedoableOp>(100);
        int numRecoveredOps = 0;
        if (mSupportsCrashRecovery) {
            mRecoveryMode = true;
            ZimbraLog.redolog.info("Starting pre-startup crash recovery");
            // Run crash recovery.
            try {
                mLogWriter.open();
                mRolloverMgr.initSequence(mLogWriter.getSequence());
                RedoPlayer redoPlayer = new RedoPlayer(true);
                try {
                    Set<String> serverIds = new HashSet<String>();
                    serverIds.add(Provisioning.getInstance().getLocalServer().getId());
                    //for now we only run crash recovery on ops which originated on this server
                    //will need further tooling/improvements to handle crash recovery from a vanished node
                    numRecoveredOps = redoPlayer.runCrashRecovery(this, postStartupRecoveryOps, serverIds);
                } finally {
                    redoPlayer.shutdown();
                }
                mLogWriter.close();
            } catch (Exception e) {
                ZimbraLog.redolog.fatal("Exception during crash recovery");
                signalLogError(e);
            }
            ZimbraLog.redolog.info("Finished pre-startup crash recovery");
            mRecoveryMode = false;
        }

        setInCrashRecovery(false);

        // Reopen log after crash recovery.
        try {
            mLogWriter.open();
            mRolloverMgr.initSequence(mLogWriter.getSequence());
            mInitialLogSize = mLogWriter.getSize();
        } catch (IOException e) {
            ZimbraLog.redolog.fatal("Unable to open redo log");
            signalLogError(e);
        }

        if (numRecoveredOps > 0) {
            // Add post-recovery ops to map before rollover, so the new redolog
            // file after rollover will still list these uncommitted ops.
            if (postStartupRecoveryOps.size() > 0) {
                synchronized (mActiveOps) {
                    for (Iterator iter = postStartupRecoveryOps.iterator(); iter.hasNext(); ) {
                        RedoableOp op = (RedoableOp) iter.next();
                        assert(op.isStartMarker());
                        mActiveOps.put(op.getTransactionId(), op);
                    }
                }
            }

            // Force rollover to clear the current log file.
            forceRollover();

            // Start a new thread to run recovery on the remaining ops.
            // Recovery of these ops will occur in parallel with new client
            // requests.
            if (postStartupRecoveryOps.size() > 0) {
                synchronized (mShuttingDownGuard) {
                    mInPostStartupCrashRecovery = true;
                }
                Thread psrThread =
                    new PostStartupCrashRecoveryThread(postStartupRecoveryOps);
                psrThread.start();
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (!mEnabled)
            return;

        synchronized (mShuttingDownGuard) {
            mShuttingDown = true;
            if (mInPostStartupCrashRecovery) {
                // Wait for PostStartupCrashRecoveryThread to signal us.
                try {
                    mShuttingDownGuard.wait();
                } catch (InterruptedException e) {}
            }
        }

        try {
            forceRollover();
            mLogWriter.flush();
            mLogWriter.close();
        } catch (Exception e) {
            ZimbraLog.redolog.error("Error closing redo log", e);
        }

        double rate = 0.0;
        if (mCounter > 0)
            rate =
                ((double) Math.round(
                    ((double) mElapsed ) / mCounter * 1000
                )) / 1000;
        ZimbraLog.redolog.info("Logged: " + mCounter + " items, " + rate + "ms/item");
        mEnabled = false;
    }

    @Override
    public TransactionId getNewTxnId() {
        return mTxnIdGenerator.getNext();
    }

    @Override
    public void log(RedoableOp op, boolean synchronous) throws ServiceException {
        if (!mEnabled || mRecoveryMode)
            return;

        logOnly(op, synchronous);

        if (isRolloverNeeded(false))
            rollover(false, false);
    }

    @Override
    public void commit(RedoableOp op) throws ServiceException {
        if (mEnabled) {
            long redoSeq = mRolloverMgr.getCurrentSequence();
            CommitTxn commit = new CommitTxn(op);
            // Commit records are written without fsync.  It's okay to
            // allow fsync to happen by itself or wait for one during
            // logging of next redo item.
            log(commit, false);
            commit.setSerializedByteArray(null);
        }
    }

    @Override
    public void abort(RedoableOp op) throws ServiceException {
        if (mEnabled) {
            AbortTxn abort = new AbortTxn(op);
            // Abort records are written with fsync, to prevent triggering
            // redo during crash recovery.
            log(abort, true);
            abort.setSerializedByteArray(null);
        }
    }

    @Override
    public void flush() throws IOException {
        if (mEnabled)
            mLogWriter.flush();
    }

    /**
     * Log an operation to the logger.  Only does logging; doesn't
     * bother with checkpoint, rollover, etc.
     * @param op
     * @param synchronous
     * @throws ServiceException
     */
    @Override
    public void logOnly(RedoableOp op, boolean synchronous) throws ServiceException {
        try {
            // Do the logging while holding a read lock on the RW lock.
            // This prevents checkpoint or rollover from starting when
            // there are any threads in the act of logging.
            ReadLock readLock = mRWLock.readLock();
            readLock.lockInterruptibly();
            try {
                // Update active ops map.
                synchronized (mActiveOps) {
                    if (op.isStartMarker()) {
                        mActiveOps.put(op.getTransactionId(), op);
                    }
                    if (op.isEndMarker())
                        mActiveOps.remove(op.getTransactionId());
                }

                try {
                    long start = System.currentTimeMillis();
                    mLogWriter.log(op, op.getInputStream(), synchronous);
                    long elapsed = System.currentTimeMillis() - start;
                    synchronized (mStatGuard) {
                        mElapsed += elapsed;
                        mCounter++;
                    }
                } catch (NullPointerException e) {
                    StackTraceElement stack[] = e.getStackTrace();
                    if (stack == null || stack.length == 0) {
                        ZimbraLog.redolog.warn("Caught NullPointerException during redo logging, but " +
                                               "there is no stack trace in the exception.  " +
                                               "If you are running Sun server VM, you could be hitting " +
                                               "Java bug 4292742.  (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4292742)  " +
                                               "Re-run the test case with client VM to see the stack trace.", e);
                    }

                    // When running with server VM ("java -server" command line) some NPEs
                    // will not report the stack trace.  This is Java bug 4292742.
                    //
                    //   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4292742
                    //
                    // There is also this related bug:
                    //
                    //   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4761344
                    //
                    // which says NPE might be thrown when it is impossible to
                    // be thrown according to source code.  The bug header says it's fixed
                    // in VM 1.4.2, but I'm getting NPE with 1.4.2_05 VM.  Indeed, further
                    // reading of the bug page reveals there have been reports of variants
                    // of the bug in 1.4.2.
                    //
                    // Most complaints in the bug page say the problem happens with server
                    // VM.  None says it happens with the client VM.
                    //
                    // The second bug does not imply the first bug.  When you get an NPE
                    // with no stack trace, switch to client VM and try to reproduce the
                    // bug to get the stack and fix the bug.  Don't automatically assume
                    // you're hitting the second bug.
                    //

                    signalLogError(e);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);
                } catch (Throwable e) {
                    ZimbraLog.redolog.error("Redo logging to logger " + mLogWriter.getClass().getName() + " failed", e);
                    signalLogError(e);
                }

                if (ZimbraLog.redolog.isDebugEnabled())
                    ZimbraLog.redolog.debug(op.toString());
            } finally {
                readLock.unlock();
            }
        } catch (InterruptedException e) {
            synchronized (mShuttingDownGuard) {
                if (!mShuttingDown)
                    ZimbraLog.redolog.warn("InterruptedException while logging", e);
                else
                    ZimbraLog.redolog.info("Thread interrupted for shutdown");
            }
        }
    }

    /**
     * Should be called with write lock on mRWLock held.
     * @throws ServiceException
     */
    private void checkpoint() throws ServiceException {
        LinkedHashSet<TransactionId> txns = null;
        synchronized (mActiveOps) {
            if (mActiveOps.size() == 0)
                return;

            // Create an empty LinkedHashSet and insert keys from mActiveOps
            // by iterating the keyset.
            txns = new LinkedHashSet<TransactionId>();
            for (Iterator<Map.Entry<TransactionId, RedoableOp>>
                 it = mActiveOps.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<TransactionId, RedoableOp> entry = it.next();
                txns.add(entry.getKey());
            }
        }
        Checkpoint ckpt = new Checkpoint(txns);
        logOnly(ckpt, true);
    }

    /**
     * Determines if a log rollover is needed.  If immediate is true, rollover
     * is deemed needed if current log is non-empty.  If immediate is false,
     * rollover is needed only if the log hits the maximum size limit.
     * @param immediate
     * @return
     */
    protected boolean isRolloverNeeded(boolean immediate) throws ServiceException {
        boolean result = false;
        try {
            if (immediate) {
                result = !mLogWriter.isEmpty();
            } else {
                long size = mLogWriter.getSize();
                if (size >= mLogRolloverHardMaxBytes) {
                    // Log is bigger than hard max.
                    result = true;
                } else if (size >= mLogRolloverSoftMaxBytes && size > mInitialLogSize) {
                    // Log is bigger than soft max, but it it old enough?
                    long now = System.currentTimeMillis();
                    long createTime = Math.min(mLogWriter.getCreateTime(), now);
                    long age = now - createTime;
                    result = age >= mLogRolloverMinAgeMillis;
                }
            }
        } catch (IOException e) {
            ZimbraLog.redolog.fatal("Unable to get redo log size");
            signalLogError(e);
        }
        return result;
    }

    protected void setRolloverLimits(long minAgeMillis, long softMaxBytes, long hardMaxBytes) {
        mLogRolloverMinAgeMillis = minAgeMillis;
        mLogRolloverSoftMaxBytes = softMaxBytes;
        mLogRolloverHardMaxBytes = hardMaxBytes;
    }

    /**
     * Do a log rollover if necessary.  If force is true, rollover occurs if
     * log is non-empty.  If force is false, rollover happens only when it's
     * needed according to isRolloverNeeded().
     * @param force
     * @param skipCheckpoint if true, skips writing Checkpoint entry at end of file
     * @return java.io.File object for rolled over file; null if no rollover occurred
     * @throws ServiceException
     */
    protected void rollover(boolean force, boolean skipCheckpoint) throws ServiceException {
        if (!mEnabled)
            return;

        // Grab a write lock on mRWLock.  No thread will be
        // able to log a new item until rollover is done.
        WriteLock writeLock = mRWLock.writeLock();
        try {
            writeLock.lockInterruptibly();
        } catch (InterruptedException e) {
            synchronized (mShuttingDownGuard) {
                if (!mShuttingDown)
                    ZimbraLog.redolog.error("InterruptedException during log rollover", e);
                else
                    ZimbraLog.redolog.debug("Rollover interrupted during shutdown");
            }
            return;
        }

        try {
            if (isRolloverNeeded(force)) {
                ZimbraLog.redolog.debug("Redo log rollover started");

                long start = System.currentTimeMillis();
                if (isDbFlushNeeded()) {
                    // Force the database to persist the committed changes to disk.
                    // This is very important when running mysql with innodb_flush_log_at_trx_commit=0 (or 2).
                    Db.getInstance().flushToDisk();
                }
                if (!skipCheckpoint)
                    checkpoint();
                synchronized (mActiveOps) {
                    mLogWriter.rollover(mActiveOps);
                    mInitialLogSize = mLogWriter.getSize();
                }
                long elapsed = System.currentTimeMillis() - start;
                ZimbraLog.redolog.info("Redo log rollover took " + elapsed + "ms");
            }
        } catch (IOException e) {
            ZimbraLog.redolog.error("IOException during redo log rollover");
            signalLogError(e);
        } finally {
            writeLock.unlock();
        }

        return;
    }

    private boolean isDbFlushNeeded() {
        //TODO: needs further consideration..do we need to flush galera cluster during redolog rollover?
        return DebugConfig.redologFlushDbOnRollover && Zimbra.isAlwaysOn();
    }

    @Override
    public void forceRollover() throws ServiceException {
        forceRollover(false);
    }

    @Override
    public void forceRollover(boolean skipCheckpoint) throws ServiceException {
        rollover(true, skipCheckpoint);
    }

    @Override
    public RolloverManager getRolloverManager() {
        return mRolloverMgr;
    }

    @Override
    public long getCurrentLogSequence() {
        return mRolloverMgr.getCurrentSequence();
    }

    /**
     * Must be called with write lock on mRWLock held.
     */
    protected void resetActiveOps() {
        synchronized (mActiveOps) {
            mActiveOps.clear();
        }
    }

    /**
     * Acquires an exclusive lock on the log manager.  When the log manager
     * is locked this way, it is guaranteed that no thread is in the act
     * of logging or doing a log rollover.  In other words, the logs are
     * quiesced.
     *
     * The thread calling this method must later release the lock by calling
     * releaseExclusiveLock() method and passing the Sync object that was
     * returned by this method.
     *
     * @return the Sync object to be used later to release the lock
     * @throws InterruptedException
     */
    protected WriteLock acquireExclusiveLock() throws InterruptedException {
        WriteLock writeLock = mRWLock.writeLock();
        writeLock.lockInterruptibly();
        return writeLock;
    }

    /**
     * Releases the exclusive lock on the log manager.
     * See acquireExclusiveLock() method.
     * @param exclusiveLock
     */
    protected void releaseExclusiveLock(WriteLock exclusiveLock) {
        exclusiveLock.unlock();
    }

    protected void signalLogError(Throwable e) throws ServiceException {
        // Die before any further damage is done.
        if (DebugConfig.redologHaltOnFatal) {
            Zimbra.halt("Aborting process", e);
        }
    }

    /**
     * @param seq
     * @return
     * @throws IOException
     */
    @Override
    public abstract File[] getArchivedLogsFromSequence(long seq) throws IOException;

    @Override
    public File[] getArchivedLogs() throws IOException {
        return getArchivedLogsFromSequence(Long.MIN_VALUE);
    }

    @Override
    public abstract Pair<Set<Integer>, CommitId> getChangedMailboxesSince(CommitId cid) throws IOException, MailServiceException;

    class PostStartupCrashRecoveryThread extends Thread {
        List mOps;

        private PostStartupCrashRecoveryThread(List ops) {
            super("PostStartupCrashRecovery");
            setDaemon(true);
            mOps = ops;
        }

        @Override
        public void run() {
            ZimbraLog.redolog.info("Starting post-startup crash recovery");
            boolean interrupted = false;
            for (Iterator iter = mOps.iterator(); iter.hasNext(); ) {
                synchronized (mShuttingDownGuard) {
                    if (mShuttingDown) {
                        interrupted = true;
                        break;
                    }
                }
                RedoableOp op = (RedoableOp) iter.next();
                try {
                    if (ZimbraLog.redolog.isDebugEnabled())
                        ZimbraLog.redolog.debug("REDOING: " + op);
                    op.redo();
                } catch (Exception e) {
                    // If there's any problem, just log the error and move on.
                    // The alternative is to abort the server, but that may be
                    // too drastic.
                    ZimbraLog.redolog.error("Redo failed for [" + op + "]." +
                                            "  Backend state of affected item is indeterminate." +
                                            "  Marking operation as aborted and moving on.", e);
                } finally {
                    // If the redo didn't work, we need to mark this operation
                    // as aborted in the redolog so it doesn't get reattempted
                    // during next startup.
                    //
                    // If the redo did work, we still need to mark our op as
                    // aborted because in the course of the redo a successful
                    // commit of the operation was logged using a different
                    // txn ID.  We must therefore tell the redolog the currnt
                    // op is canceled, to avoid redoing it during next startup.
                    AbortTxn abort = new AbortTxn(op);
                    try {
                        logOnly(abort, true);
                    } catch (ServiceException e) {
                        ZimbraLog.redolog.error("Abort failed for [" + op + "]." +
                                "  Backend state of affected item is indeterminate." +
                                "  moving on.", e);
                    }
                }
            }

            if (!interrupted)
                ZimbraLog.redolog.info("Finished post-startup crash recovery");

            // Being paranoid...
            mOps.clear();
            mOps = null;

            synchronized (mShuttingDownGuard) {
                mInPostStartupCrashRecovery = false;
                if (mShuttingDown)
                    mShuttingDownGuard.notifyAll();  // signals wait() in stop() method
            }
        }
    }

}