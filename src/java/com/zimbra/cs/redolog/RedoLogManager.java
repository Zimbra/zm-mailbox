/*
 * Created on 2004. 7. 16.
 */
package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.logger.FileLogWriter;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.AbortTxn;
import com.zimbra.cs.redolog.op.Checkpoint;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;
//import com.zimbra.cs.redolog.op.Rollover;

/**
 * @author jhahm
 */
public class RedoLogManager {

    private static Log mLog = LogFactory.getLog(RedoLogManager.class);

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

		synchronized TransactionId getNext() {
			TransactionId tid = new TransactionId(mTime, mCounter);
			if (mCounter < 0x7fffffffL)
				mCounter++;
			else
				init();
			return tid;
		}
	}

	private boolean mEnabled;
    private boolean mShuttingDown;
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
	private LinkedHashMap /*<TxnId, RedoableOp>*/ mActiveOps;

	private long mLogRolloverSizeLimit;

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
        mActiveOps = new LinkedHashMap(100);
        mTxnIdGenerator = new TxnIdGenerator();
        setRolloverSizeLimit(RedoConfig.redoLogRolloverFileSizeKB() * 1024);
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

    protected LogWriter createLogWriter(RedoLogManager redoMgr,
                                        File logfile,
                                        long fsyncIntervalMS) {
        return new FileLogWriter(redoMgr, logfile, fsyncIntervalMS);
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

        // Recover from crash during rollover.  We do this even when
        // mSupportsCrashRecovery is false.
        try {
            mRolloverMgr.crashRecovery();
        } catch (IOException e) {
            mLog.fatal("Exception during crash recovery");
            signalFatalError(e);
        }

        long fsyncInterval = RedoConfig.redoLogFsyncIntervalMS();
        mLogWriter = createLogWriter(this, mLogFile, fsyncInterval);

        // KC: 1711 fix
        //initRedologSequence();
        
        ArrayList postStartupRecoveryOps = new ArrayList(100);
        int numRecoveredOps = 0;
		if (mSupportsCrashRecovery) {
			mRecoveryMode = true;
			mLog.info("Starting pre-startup crash recovery");
			// Run crash recovery.
			try {
				mLogWriter.open();
                mRolloverMgr.initSequence(mLogWriter.getSequence());
				RedoPlayer redoPlayer = new RedoPlayer();
				numRecoveredOps = redoPlayer.runCrashRecovery(this, postStartupRecoveryOps);
                redoPlayer.shutdown();
				mLogWriter.close();
			} catch (Exception e) {
				mLog.fatal("Exception during crash recovery");
				signalFatalError(e);
			}
			mLog.info("Finished pre-startup crash recovery");
			mRecoveryMode = false;
		}

		// Reopen log after crash recovery.
		try {
			mLogWriter.open();
            mRolloverMgr.initSequence(mLogWriter.getSequence());
			mInitialLogSize = mLogWriter.getSize();
		} catch (IOException e) {
			mLog.fatal("Unable to open redo log");
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
                Thread psrThread =
                    new PostStartupCrashRecoveryThread(postStartupRecoveryOps);
                psrThread.start();
            }
        }
	}

    private void initRedologSequence() {
        try {
            // init the sequence number from db
            mRolloverMgr.initSequence();
            long seq = mRolloverMgr.getCurrentSequence();
            // check against the current sequence number in redo.log
            mLogWriter.open();
            long currSeq = mLogWriter.getSequence();
            if (currSeq != seq)
                throw ServiceException.FAILURE("System redo log file sequence (" + seq + 
                        ") does not match that (" + currSeq + ") in the current redo log file", null);
        } catch (ServiceException e) {
            signalFatalError(e);
        } catch (IOException e) {
            signalFatalError(e);
        } finally {
            try {
                mLogWriter.close();
            } catch (IOException e) {
                mLog.warn("Unable to close redo log writer", e);
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
            mLog.info("Starting post-startup crash recovery");
            for (Iterator iter = mOps.iterator(); iter.hasNext(); ) {
            	RedoableOp op = (RedoableOp) iter.next();
                try {
                    mLog.info("REDOING: " + op);
					op.redo();
				} catch (Exception e) {
                    // If there's any problem, just log the error and move on.
                    // The alternative is to abort the server, but that may be
                    // too drastic.
                    mLog.error("Redo failed for [" + op + "]." +
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
            mLog.info("Finished post-startup crash recovery");

            // Being paranoid...
            mOps.clear();
            mOps = null;
        }

    }

	public synchronized void stop() {
		if (!mEnabled)
			return;

        mShuttingDown = true;

		try {
            forceRollover();
			mLogWriter.close();
		} catch (Exception e) {
			mLog.error("Error closing redo log " + mLogFile.getName(), e);
		}

        double rate = 0.0;
        if (mCounter > 0)
            rate =
                ((double) Math.round(
                    ((double) mElapsed ) / mCounter * 1000
                )) / 1000;
        mLog.info("Logged: " + mCounter + " items, " + rate + "ms/item");
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
					mLogWriter.log(op, op.getSerializedByteArrayVector(), synchronous);
                    long elapsed = System.currentTimeMillis() - start;
                    synchronized (mStatGuard) {
                    	mElapsed += elapsed;
                        mCounter++;
                    }
                } catch (NullPointerException e) {
                    StackTraceElement stack[] = e.getStackTrace();
                    if (stack == null || stack.length == 0) {
                        mLog.warn("Caught NullPointerException during redo logging, but " +
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
                } catch (Throwable e) {
					mLog.error("Redo logging to logger " + mLogWriter.getClass().getName() + " failed", e);
					signalFatalError(e);
				}

				if (mLog.isDebugEnabled())
					mLog.debug(op.toString());
			} finally {
				readLock.release();
			}
		} catch (InterruptedException e) {
            if (!mShuttingDown)
    			mLog.warn("InterruptedException while logging", e);
            else
                mLog.info("Thread interrupted for shutdown");
		}
	}

    /**
     * Should be called with write lock on mRWLock held.
     */
	private void checkpoint() {
		LinkedHashSet txns = null;
		synchronized (mActiveOps) {
            if (mActiveOps.size() == 0)
                return;

            // Create an empty LinkedHashSet and insert keys from mActiveOps
			// by iterating the keyset.
            txns = new LinkedHashSet();
			for (Iterator it = mActiveOps.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry entry = (Map.Entry) it.next();
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
				result = size >= mLogRolloverSizeLimit && size > mInitialLogSize;
			}
		} catch (IOException e) {
			mLog.fatal("Unable to get redo log size");
			signalFatalError(e);
		}
		return result;
	}

	protected void setRolloverSizeLimit(long bytes) {
		mLogRolloverSizeLimit = bytes;
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
        File rolledOverFile = null;
        // Grab a write lock on mRWLock.  No thread will be
        // able to log a new item until rollover is done.
		Sync writeLock = mRWLock.writeLock();
		try {
			writeLock.acquire();
		} catch (InterruptedException e) {
            if (!mShuttingDown)
    			mLog.error("InterruptedException during log rollover", e);
            else
                mLog.debug("Rollover interrupted during shutdown");
			return rolledOverFile;
		}

		try {
			if (isRolloverNeeded(force)) {
				mLog.debug("Redo log rollover started");
                if (!skipCheckpoint)
    				checkpoint();
				synchronized (mActiveOps) {
                    rolledOverFile = mLogWriter.rollover(mActiveOps);
					mInitialLogSize = mLogWriter.getSize();
				}
				mLog.debug("Redo log rollover finished");
			}
		} catch (IOException e) {
			mLog.error("IOException during redo log rollover");
			signalFatalError(e);
		} finally {
			writeLock.release();
		}

        /* TODO: Finish implementing Rollover as a replicated op.
         * Checking in this partial code to work on something else.
        if (rolledOverFile != null) {
            mLog.info("Rollover: " + rolledOverFile.getName());
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
        mShuttingDown = true;
        // TODO: Do we need a more graceful shutdown?  Or is it better to die
        // before any further damage is done?
        Zimbra.halt("Aborting process", e);
	}

    /**
     * @param sequenceAtPrevFullBackup
     * @return
     * @throws IOException
     */
    public File[] getArchivedLogsAfterSequence(long sequenceAtPrevFullBackup) throws IOException {
        File[] archivedLogs = RolloverManager.getArchiveLogs(mArchiveDir);
        
        ArrayList toRet = new ArrayList();
        
        for (int i = 0; i < archivedLogs.length; i++) {
            // if the file sequence comes after our previous backup, then it should be included
            FileLogReader r = new FileLogReader(archivedLogs[i]);
            if (r.getHeader().getSequence() >= sequenceAtPrevFullBackup)
            {
                toRet.add(archivedLogs[i]);
            }
        }
        File[] retArray = new File[0];
        return (File[]) (toRet.toArray(retArray));
    }
}
