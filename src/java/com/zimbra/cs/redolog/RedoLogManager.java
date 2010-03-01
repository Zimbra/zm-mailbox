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
 * Created on 2004. 7. 16.
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

import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;

import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import com.zimbra.cs.db.Db;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.logger.FileLogWriter;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.AbortTxn;
import com.zimbra.cs.redolog.op.Checkpoint;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.util.Zimbra;
//import com.zimbra.cs.redolog.op.Rollover;
import com.zimbra.znative.IO;

/**
 * @author jhahm
 */
public class RedoLogManager {

	private static class TxnIdGenerator {
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

	private boolean mEnabled;
    private boolean mInCrashRecovery;
    private final Object mInCrashRecoveryGuard = new Object();
    private boolean mShuttingDown;
    private final Object mShuttingDownGuard = new Object();
    private boolean mInPostStartupCrashRecovery;  // also protected by mShuttingDownGuard
	private boolean mSupportsCrashRecovery;
	private boolean mRecoveryMode;	// Are we in crash-recovery mode?
	private File mArchiveDir;		// where log files are archived as they get rolled over
	private File mLogFile;			// full path to the "redo.log" file

	// This read/write lock is used to allow multiple threads to call log()
	// simultaneously under normal circumstances, while locking them out
	// when checkpoint or rollover is in progress.  Thus, "loggers" are
	// "readers", and threads that do checkpoint/rollover are "writers".
	private ReentrantWriterPreferenceReadWriteLock mRWLock;

	// Insertion-order-preserved map of active transactions.  Each thread
	// reading from or writing to this map must first acquire a read or
	// write lock on mRWLock, then do "synchronzed (mActiveOps) { ... }".
	// This is done to prevent deadlock.
	private LinkedHashMap<TransactionId, RedoableOp> mActiveOps;

	private long mLogRolloverMinAgeMillis;
	private long mLogRolloverSoftMaxBytes;
	private long mLogRolloverHardMaxBytes;

	private TxnIdGenerator mTxnIdGenerator;
	private RolloverManager mRolloverMgr;

    private long mInitialLogSize;	// used in log rollover

	// the actual logger
	private LogWriter mLogWriter;

    private Object mStatGuard;
    private long mElapsed;
    private int mCounter;


    public RedoLogManager(File redolog, File archdir, boolean supportsCrashRecovery) {
		mEnabled = false;
        mShuttingDown = false;
        mRecoveryMode = false;
        mSupportsCrashRecovery = supportsCrashRecovery;

        mLogFile = redolog;
    	mArchiveDir = archdir;

        mRWLock = new ReentrantWriterPreferenceReadWriteLock();
        mActiveOps = new LinkedHashMap<TransactionId, RedoableOp>(100);
        mTxnIdGenerator = new TxnIdGenerator();
        long minAge = RedoConfig.redoLogRolloverMinFileAge() * 60 * 1000;     // milliseconds
        long softMax = RedoConfig.redoLogRolloverFileSizeKB() * 1024;         // bytes
        long hardMax = RedoConfig.redoLogRolloverHardMaxFileSizeKB() * 1024;  // bytes
        setRolloverLimits(minAge, softMax, hardMax);
        mRolloverMgr = new RolloverManager(this, mLogFile);
        mLogWriter = null;

        mStatGuard = new Object();
        mElapsed = 0;
        mCounter = 0;
    }

	protected LogWriter getLogWriter() {
		return mLogWriter;
	}

	/**
	 * Returns the File object for the one and only redo log file "redo.log".
	 * @return
	 */
	public File getLogFile() {
		return mLogFile;
	}

	public File getArchiveDir() {
		return mArchiveDir;
	}

    public File getRolloverDestDir() {
        return mArchiveDir;
    }

    public LogWriter getCurrentLogWriter() {
		return mLogWriter;
	}

    public LogWriter createLogWriter(RedoLogManager redoMgr,
                                        File logfile,
                                        long fsyncIntervalMS) {
        return new FileLogWriter(redoMgr, logfile, fsyncIntervalMS);
    }

    private void setInCrashRecovery(boolean b) {
        synchronized (mInCrashRecoveryGuard) {
            mInCrashRecovery = b;
        }
    }

    public boolean getInCrashRecovery() {
        synchronized (mInCrashRecoveryGuard) {
            return mInCrashRecovery;
        }
    }

    public synchronized void start() {
		mEnabled = true;

		try {
			File logdir = mLogFile.getParentFile();
	        if (!logdir.exists()) {
	        	if (!logdir.mkdirs())
	        		throw new IOException("Unable to create directory " + logdir.getAbsolutePath());
	        }
	        if (!mArchiveDir.exists()) {
	        	if (!mArchiveDir.mkdirs())
	        		throw new IOException("Unable to create directory " + mArchiveDir.getAbsolutePath());
	        }
		} catch (IOException e) {
			signalFatalError(e);
		}

        setInCrashRecovery(true);

        // Recover from crash during rollover.  We do this even when
        // mSupportsCrashRecovery is false.
        try {
            mRolloverMgr.crashRecovery();
        } catch (IOException e) {
            ZimbraLog.redolog.fatal("Exception during crash recovery");
            signalFatalError(e);
        }

        long fsyncInterval = RedoConfig.redoLogFsyncIntervalMS();
        mLogWriter = createLogWriter(this, mLogFile, fsyncInterval);

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
				    numRecoveredOps = redoPlayer.runCrashRecovery(this, postStartupRecoveryOps);
				} finally {
				    redoPlayer.shutdown();
				}
				mLogWriter.close();
			} catch (Exception e) {
				ZimbraLog.redolog.fatal("Exception during crash recovery");
				signalFatalError(e);
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
			signalFatalError(e);
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

    private class PostStartupCrashRecoveryThread extends Thread {
        List mOps;

        private PostStartupCrashRecoveryThread(List ops) {
            super("PostStartupCrashRecovery");
            setDaemon(true);
    		mOps = ops;
        }

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
                    logOnly(abort, true);
                }
            }

            // Flush out all uncommitted Lucene index writes.
            MailboxIndex.flushAllWriters();

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
			ZimbraLog.redolog.error("Error closing redo log " + mLogFile.getName(), e);
		}

        double rate = 0.0;
        if (mCounter > 0)
            rate =
                ((double) Math.round(
                    ((double) mElapsed ) / mCounter * 1000
                )) / 1000;
        ZimbraLog.redolog.info("Logged: " + mCounter + " items, " + rate + "ms/item");
    }

	public TransactionId getNewTxnId() {
		return mTxnIdGenerator.getNext();
	}

	public void log(RedoableOp op, boolean synchronous) {
		if (!mEnabled || mRecoveryMode)
			return;

		logOnly(op, synchronous);

		if (isRolloverNeeded(false))
			rollover(false, false);
	}

	/**
	 * Logs the COMMIT record for an operation.
	 * @param op
	 */
	public void commit(RedoableOp op) {
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

	public void abort(RedoableOp op) {
		if (mEnabled) {
			AbortTxn abort = new AbortTxn(op);
            // Abort records are written with fsync, to prevent triggering
            // redo during crash recovery.
			log(abort, true);
            abort.setSerializedByteArray(null);
		}
	}

    public void flush() throws IOException {
        if (mEnabled)
            mLogWriter.flush();
    }

    /**
	 * Log an operation to the logger.  Only does logging; doesn't
	 * bother with checkpoint, rollover, etc.
	 * @param op
	 * @param synchronous
	 */
	protected void logOnly(RedoableOp op, boolean synchronous) {
		try {
			// Do the logging while holding a read lock on the RW lock.
			// This prevents checkpoint or rollover from starting when
			// there are any threads in the act of logging.
			Sync readLock = mRWLock.readLock();
			readLock.acquire();
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

                    signalFatalError(e);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);
                } catch (Throwable e) {
					ZimbraLog.redolog.error("Redo logging to logger " + mLogWriter.getClass().getName() + " failed", e);
					signalFatalError(e);
				}

				if (ZimbraLog.redolog.isDebugEnabled())
					ZimbraLog.redolog.debug(op.toString());
			} finally {
				readLock.release();
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
     */
	private void checkpoint() {
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
	protected boolean isRolloverNeeded(boolean immediate) {
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
			signalFatalError(e);
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
	 */
	protected File rollover(boolean force, boolean skipCheckpoint) {
        if (!mEnabled)
            return null;

        File rolledOverFile = null;
        // Grab a write lock on mRWLock.  No thread will be
        // able to log a new item until rollover is done.
		Sync writeLock = mRWLock.writeLock();
		try {
			writeLock.acquire();
		} catch (InterruptedException e) {
		    synchronized (mShuttingDownGuard) {
                if (!mShuttingDown)
        			ZimbraLog.redolog.error("InterruptedException during log rollover", e);
                else
                    ZimbraLog.redolog.debug("Rollover interrupted during shutdown");
		    }
			return rolledOverFile;
		}

		try {
			if (isRolloverNeeded(force)) {
				ZimbraLog.redolog.debug("Redo log rollover started");

				long start = System.currentTimeMillis();
				// Force the database to persist the committed changes to disk.
                // This is very important when running mysql with innodb_flush_log_at_trx_commit=0 (or 2).
                Db.getInstance().flushToDisk();

                if (!skipCheckpoint)
    				checkpoint();
                synchronized (mActiveOps) {
                    rolledOverFile = mLogWriter.rollover(mActiveOps);
					mInitialLogSize = mLogWriter.getSize();
				}
                long elapsed = System.currentTimeMillis() - start;
				ZimbraLog.redolog.info("Redo log rollover took " + elapsed + "ms");
			}
		} catch (IOException e) {
			ZimbraLog.redolog.error("IOException during redo log rollover");
			signalFatalError(e);
        } finally {
			writeLock.release();
		}

        /* TODO: Finish implementing Rollover as a replicated op.
         * Checking in this partial code to work on something else.
        if (rolledOverFile != null) {
            ZimbraLog.redolog.info("Rollover: " + rolledOverFile.getName());
            // Log rollover marker to redolog stream.
            Rollover ro = new Rollover(rolledOverFile);
            ro.start(System.currentTimeMillis());
            logOnly(ro, false); // Don't call log() as it may call rollover() in infinite loop.
            CommitTxn commit = new CommitTxn(ro);
            logOnly(commit, true);
        }
        */
		return rolledOverFile;
	}

    public File forceRollover() {
    	return forceRollover(false);
    }

    public File forceRollover(boolean skipCheckpoint) {
        return rollover(true, skipCheckpoint);
    }

    public RolloverManager getRolloverManager() {
    	return mRolloverMgr;
    }

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
	protected Sync acquireExclusiveLock() throws InterruptedException {
		Sync writeLock = mRWLock.writeLock();
		writeLock.acquire();
		return writeLock;
	}

	/**
	 * Releases the exclusive lock on the log manager.
	 * See acquireExclusiveLock() method.
	 * @param exclusiveLock
	 */
	protected void releaseExclusiveLock(Sync exclusiveLock) {
		exclusiveLock.release();
	}

	protected void signalFatalError(Throwable e) {
        // Die before any further damage is done.
        Zimbra.halt("Aborting process", e);
	}

    /**
     * @param seq
     * @return
     * @throws IOException
     */
    public File[] getArchivedLogsFromSequence(long seq) throws IOException {
        return RolloverManager.getArchiveLogs(mArchiveDir, seq);
    }

    public File[] getArchivedLogs() throws IOException {
        return getArchivedLogsFromSequence(Long.MIN_VALUE);
    }

    /**
     * Returns the set of mailboxes that had any committed changes since a
     * particular CommitId in the past, by scanning redologs.  Also returns
     * the last CommitId seen during the scanning process.
     * @param cid
     * @return can be null if server is shutting down
     * @throws IOException
     * @throws MailServiceException
     */
    public Pair<Set<Long>, CommitId> getChangedMailboxesSince(CommitId cid)
    throws IOException, MailServiceException {
        Set<Long> mailboxes = new HashSet<Long>();

        // Grab a read lock to prevent rollover.
        Sync readLock = mRWLock.readLock();
        try {
            readLock.acquire();
        } catch (InterruptedException e) {
            synchronized (mShuttingDownGuard) {
                if (!mShuttingDown)
                    ZimbraLog.redolog.error("InterruptedException during redo log scan for CommitId", e);
                else
                    ZimbraLog.redolog.debug("Redo log scan for CommitId interrupted for shutdown");
            }
            return null;
        }

        File linkDir = null;
        File[] logs;
        try {
            try {
                long seq = cid.getRedoSeq();
                File[] archived = getArchivedLogsFromSequence(seq);
                if (archived != null) {
                    logs = new File[archived.length + 1];
                    System.arraycopy(archived, 0, logs, 0, archived.length);
                    logs[archived.length] = mLogFile;
                } else {
                    logs = new File[] { mLogFile };
                }
                // Make sure the first log has the sequence in cid.
                FileLogReader firstLog = new FileLogReader(logs[0]);
                if (firstLog.getHeader().getSequence() != seq) {
                    // Most likely, the CommitId is too old.
                    throw MailServiceException.INVALID_COMMIT_ID(cid.toString());
                }
    
                // Create a temp directory and make hard links to all redologs.
                // This prevents the logs from disappearing while being scanned.
                String dirName = "tmp-scan-" + System.currentTimeMillis();
                linkDir = new File(mLogFile.getParentFile(), dirName);
                if (linkDir.exists()) {
                    int suffix = 1;
                    while (linkDir.exists()) {
                        linkDir = new File(mLogFile.getParentFile(), dirName + "-" + suffix);
                    }
                }
                if (!linkDir.mkdir())
                    throw new IOException("Unable to create temp dir " + linkDir.getAbsolutePath());
                for (int i = 0; i < logs.length; i++) {
                    File src = logs[i];
                    File dest = new File(linkDir, logs[i].getName());
                    IO.link(src.getAbsolutePath(), dest.getAbsolutePath());
                    logs[i] = dest;
                }
            } finally {
                // We can let rollover happen now.
                readLock.release();
            }

            // Scan redologs to get list with IDs of mailboxes that have
            // committed changes since the given commit id.
            long lastSeq = -1;
            CommitTxn lastCommitTxn = null;
            boolean foundMarker = false;
            for (File logfile : logs) {
                FileLogReader logReader = new FileLogReader(logfile);
                logReader.open();
                lastSeq = logReader.getHeader().getSequence();
                try {
                    RedoableOp op = null;
                    while ((op = logReader.getNextOp()) != null) {
                        if (ZimbraLog.redolog.isDebugEnabled())
                            ZimbraLog.redolog.debug("Read: " + op);
                        if (!(op instanceof CommitTxn))
                            continue;

                        lastCommitTxn = (CommitTxn) op;
                        if (foundMarker) {
                            long mboxId = op.getMailboxId();
                            if (mboxId > 0)
                                mailboxes.add(mboxId);
                        } else {
                            if (cid.matches(lastCommitTxn))
                                foundMarker = true;
                        }
                    }
                } catch (IOException e) {
                    ZimbraLog.redolog.warn("IOException while reading redolog file", e);
                } finally {
                    logReader.close();
                }
            }
            if (!foundMarker) {
                // Most likely, the CommitId is too old.
                throw MailServiceException.INVALID_COMMIT_ID(cid.toString());
            }
            CommitId lastCommitId = new CommitId(lastSeq, lastCommitTxn);
            return new Pair(mailboxes, lastCommitId);
        } finally {
            if (linkDir != null) {
                // Clean up the temp dir with links.
                try {
                    if (linkDir.exists())
                        FileUtil.deleteDir(linkDir);
                } catch (IOException e) {
                    ZimbraLog.redolog.warn(
                            "Unable to delete temporary directory " +
                            linkDir.getAbsolutePath(), e);
                }
            }
        }
    }
}
