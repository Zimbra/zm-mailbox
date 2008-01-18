/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 11. 12.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.redolog.CommitId;
import com.zimbra.cs.redolog.RedoCommitCallback;
import com.zimbra.cs.redolog.RedoConfig;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RolloverManager;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FileLogWriter implements LogWriter {

    private static Log mLog = LogFactory.getLog(FileLogWriter.class);

    private static String sServerId;
    static {
        try {
			sServerId = Provisioning.getInstance().getLocalServer().getId();
		} catch (ServiceException e) {
            mLog.error("Unable to get local server ID", e);
            sServerId = "unknown";
		}
    }

    protected RedoLogManager mRedoLogMgr;

    // Synchronizes access to mRAF, mFileSize, mLogSeq, mFsyncSeq,
    // mLogCount, mFsyncCount, and mFsyncTime.
    private final Object mLock = new Object();

    // wait/notify between logger threads and fsync thread
    private final Object mFsyncCond = new Object();

    private FileHeader mHeader;
    private long mFirstOpTstamp;
    private long mLastOpTstamp;

    private File mFile;
    private RandomAccessFile mRAF;
    private long mFileSize;
    private long mLastLogTime;

    private long mFsyncIntervalMS;          // how many milliseconds fsync thread sleeps after each fync
    private boolean mFsyncDisabled;

    private FsyncThread mFsyncer;   // fsync thread

    private int mLogSeq;            // last item logged
    private int mFsyncSeq;          // last item fsynced

    // for gathering some stats; nonessential for functionality
    private int mLogCount;          // how many times log was called
    private int mFsyncCount;        // how many times fsync was called
    private long mFsyncTime;        // sum of sync durations

    private CommitNotifyQueue mCommitNotifyQueue;

    public FileLogWriter(RedoLogManager redoLogMgr,
                         File logfile,
                         long fsyncIntervalMS) {
        mRedoLogMgr = redoLogMgr;

        mHeader = new FileHeader(sServerId);
        mFile = logfile;
        mFileSize = mFile.length();
        mLastLogTime = mFile.lastModified();

        mFsyncIntervalMS = fsyncIntervalMS;
        mFsyncDisabled = DebugConfig.disableRedoLogFsync;

        mFsyncCount = mLogCount = 0;
        mFsyncTime = 0;

        mCommitNotifyQueue = new CommitNotifyQueue(100);
    }

    public long getSequence() {
        synchronized (mLock) {
            return mHeader.getSequence();
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#getSize()
     */
    public long getSize() {
        synchronized (mLock) {
            return mFileSize;
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#getLastLogTime()
     */
    public long getLastLogTime() {
        synchronized (mLock) {
        	return mLastLogTime;
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#isEmpty()
     */
    public boolean isEmpty() throws IOException {
        return getSize() <= FileHeader.HEADER_LEN;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#exists()
     */
    public boolean exists() {
        return mFile.exists();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#getAbsolutePath()
     */
    public String getAbsolutePath() {
        return mFile.getAbsolutePath();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#renameTo()
     */
    public boolean renameTo(File dest) {
        return mFile.renameTo(dest);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#delete()
     */
    public boolean delete() {
        return mFile.delete();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#open()
     */
    public synchronized void open() throws IOException {
        synchronized (mLock) {
            if (mRAF != null) return;  // already open

            mRAF = new RandomAccessFile(mFile, "rw");

            if (mRAF.length() >= FileHeader.HEADER_LEN) {
                mHeader.read(mRAF);
                mFirstOpTstamp = mHeader.getFirstOpTstamp();
            } else {
                mHeader.setSequence(mRedoLogMgr.getCurrentLogSequence());
            }
            mHeader.setOpen(true);
            mHeader.write(mRAF);

            // go to the end of file, so we can append
            long len = mRAF.length();
            mRAF.seek(len);
            mFileSize = len;

            mLogSeq = mFsyncSeq = 0;
        }

        if (mFsyncIntervalMS > 0)
            startFsyncThread();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#close()
     */
    public synchronized void close() throws IOException {
        stopFsyncThread();

        synchronized (mLock) {
            if (mRAF != null) {
                if (mLastOpTstamp != 0)
                	mHeader.setLastOpTstamp(mLastOpTstamp);
                mHeader.setOpen(false);
                mHeader.setFileSize(mRAF.length());
                mHeader.write(mRAF);

                mRAF.getChannel().force(true);
                mRAF.close();
                mRAF = null;
            } else
                return;
        }

        // Write some stats, so we can see how many times we were able
        // to avoid calling fsync.
        if (!mNoStat && mLogCount > 0 && mLog.isDebugEnabled()) {
            double msPerSync = 0;
            if (mFsyncCount > 0)
            	msPerSync =
                    ((double) Math.round(
                        ((double) mFsyncTime ) / mFsyncCount * 1000
                    )) / 1000;
            mLog.debug("Logged: " + mLogCount + " items, " +
                      mFsyncCount + " fsyncs, " + msPerSync + " ms/fsync");
        }
    }

    /**
     * Log the supplied bytes.  Depending on the value of synchronous argument
     * and the setting of fsync interval, this method can do one of 3 things:
     * 
     * case 1: !synchronous
     * action: write() only; no flush/fsync
     * 
     * case 2: synchronous && fsyncInterval > 0
     * action: write(), then wait() until notified by fsync thread
     * Current thread only calls write() on the RandomAccessFile, and blocks to
     * wait for the next scheduled fsync.  Fsync thread periodically flushes
     * the output stream and fsync's the file.  After each fsync, all waiting
     * logger threads are notified to continue.  This method batches multiple
     * log items before each fsync, and results in greater throughput than
     * calling fsync after each log item because fsync to physical disk is
     * a high-latency operation.
     * 
     * case 3: synchronous && fsyncInterval <= 0
     * action: write(), then fsync() in the current thread
     * Fsync is required, but the sleep interval for fsync thread is 0.  We
     * special case this condition to mean fsync should be done by the calling
     * thread.
     */
    public void log(RedoableOp op, InputStream data, boolean synchronous) throws IOException {
        int seq;
        boolean sameMboxAsLastOp = false;

        synchronized (mLock) {
            if (mRAF == null)
                throw new IOException("Redolog file closed");

            // Record first transaction in header.
            long tstamp = op.getTimestamp();
            mLastOpTstamp = tstamp;
            if (mFirstOpTstamp == 0) {
            	mFirstOpTstamp = tstamp;
                mHeader.setFirstOpTstamp(mFirstOpTstamp);
                mHeader.setLastOpTstamp(mLastOpTstamp);
                long pos = mRAF.getFilePointer();
                mHeader.write(mRAF);
                mRAF.seek(pos);
            }

            mLogSeq++;
            mLogCount++;
            seq = mLogSeq;
            int numRead;
            byte[] buf = new byte[1024];
            while ((numRead = data.read(buf)) >= 0) {
                mRAF.write(buf, 0, numRead);
                mFileSize += numRead;
            }
            data.close();

            // We do this with log writer lock held, so the commits and any
            // callbacks made on their behalf are truly in the correct order.
            if (op instanceof CommitTxn) {
                CommitTxn cmt = (CommitTxn) op;
                RedoCommitCallback cb = cmt.getCallback();
                if (cb != null) {
                    long redoSeq = mRedoLogMgr.getRolloverManager().getCurrentSequence();
                    CommitId cid = new CommitId(redoSeq, (CommitTxn) op);
                    Notif notif = new Notif(cb, cid);
                    // We queue it instead making the callback right away.
                    // Call it only after the commit record has been fsynced.
                    mCommitNotifyQueue.push(notif);
                }
            }

            mLastLogTime = System.currentTimeMillis();

            sameMboxAsLastOp = mLastOpMboxId == op.getMailboxId();
            mLastOpMboxId = op.getMailboxId();
        }

        // cases 1 above
        if (!synchronous)
            return;

        if (mFsyncIntervalMS > 0) {
            if (!sameMboxAsLastOp) {
                // case 2
                try {
                    // wait for fsync thread to write this entry to disk
                    synchronized (mFsyncCond) {
                        mFsyncCond.wait(10000);
                    }
                } catch (InterruptedException e) {
                    mLog.info("Thread interrupted during fsync");
                }
                synchronized (mLock) {
                    // timed out, so fsync in this thread
                    if (seq > mFsyncSeq)
                        fsync();
                }
            } else {
                // If this op is on same mailbox as last op, let's assume there's a thread issuing
                // many updates on a single mailbox in a loop, such as when importing a large ics file.
                // We don't want to pause for mFsyncIntervalMS between every op (because all writes
                // to a single mailbox are synchronized), so fsync inline and return immediately.
                fsync();
            }
        } else {
            // case 3
            fsync();
        }
    }

    private int mLastOpMboxId;

    public void flush() throws IOException {
        fsync();
    }

    private boolean mNoStat;
    public void noStat(boolean b) {
    	mNoStat = b;
    }

    public synchronized File rollover(LinkedHashMap /*<TxnId, RedoableOp>*/ activeOps)
    throws IOException {
        RolloverManager romgr = mRedoLogMgr.getRolloverManager();

        long lastSeq = getSequence();

        // Close current log, so it's impossible for its content to change.
        noStat(true);
        close();

        romgr.incrementSequence();
        
        String currentPath = mFile.getAbsolutePath();

        // Open a temporary logger.
        File tempLogfile = new File(mFile.getParentFile(), romgr.getTempFilename(lastSeq + 1));
        FileLogWriter tempLogger =
            new FileLogWriter(mRedoLogMgr, tempLogfile, 0);
        tempLogger.open();
        tempLogger.noStat(true);

        // Rewrite change entries for all active operations, maintaining
        // their order of occurrence.  (LinkedHashMap ensures ordering.)
        Set opsSet = activeOps.entrySet();
        for (Iterator it = opsSet.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            RedoableOp op = (RedoableOp) entry.getValue();
            tempLogger.log(op, op.getInputStream(), false);
        }
        tempLogger.close();

        // Rename the current log to rolled-over name.
        File rolloverFile = romgr.getRolloverFile(lastSeq);
        if (RedoConfig.redoLogDeleteOnRollover()) {
            // Delete the current log.  We don't need to hold on to the
            // indexing-only log files after rollover.
            if (!mFile.delete())
                throw new IOException("Unable to delete current redo log " + mFile.getAbsolutePath());
        } else {
            File destDir = rolloverFile.getParentFile();
            if (destDir != null && !destDir.exists())
                destDir.mkdirs();
            if (!mFile.renameTo(rolloverFile))
                throw new IOException("Unable to rename current redo log to " + rolloverFile.getAbsolutePath());
        }

        // Rename the temporary logger to current logfile name.
        String tempPath = tempLogfile.getAbsolutePath();
        mFile = new File(currentPath);
        if (!tempLogfile.renameTo(mFile))
            throw new IOException("Unable to rename " + tempPath + " to " + currentPath);

        // Reopen current log.
        open();
        noStat(false);

        return rolloverFile;
    }

    public synchronized void enableFsync() throws IOException {
        startFsyncThread();
        fsync();
    }

    public synchronized void disableFsync() throws IOException {
        fsync();
        stopFsyncThread();
    }

    private synchronized void startFsyncThread() {
        if (mFsyncer == null && mFsyncIntervalMS > 0) {
            mFsyncer = new FsyncThread(mFsyncIntervalMS);
            mFsyncer.start();
        }
    }

    private synchronized void stopFsyncThread() {
        if (mFsyncer != null) {
            mFsyncer.stopThread();
            mFsyncer = null;
        }
    }

    // do fsync if there are items logged since last fsync
    private void fsync() throws IOException {
        boolean fsyncNeeded = false;
        int seq = 0;
        synchronized (mLock) {
            if (mFsyncSeq < mLogSeq) {
                if (mRAF == null)
                    throw new IOException("Redolog file closed");
                fsyncNeeded = true;
                seq = mLogSeq;
                if (!mFsyncDisabled)
                    mFsyncCount++;
            }
        }
        if (fsyncNeeded) {
            if (!mFsyncDisabled) {
                long start = System.currentTimeMillis();
                synchronized (mLock) {
                    if (mRAF != null)
                        mRAF.getChannel().force(false);
                    else
                        throw new IOException("Redolog file closed");
                    mCommitNotifyQueue.flush(false);
                }
                long elapsed = System.currentTimeMillis() - start;
                mFsyncTime += elapsed;
            }
            synchronized (mLock) {
                mFsyncSeq = seq;
            }
            if (mFsyncIntervalMS > 0) {
                synchronized (mFsyncCond) {
                    mFsyncCond.notifyAll();
                }
            }
        }
    }


    // Thread that calls fsync() periodically.  Threads that call log()
    // will write the log entry and wait for this thread to signal them
    // after sync to disk has occurred.  This way, there are fewer fsyncs
    // than there are calls to log(), resulting in improved throughput.
    private class FsyncThread extends Thread {
        private long mSleepMS;
        private Object mFsyncLock;  // synchronizes access to mRunning
        private boolean mRunning;

        public FsyncThread(long fsyncIntervalMS) {
            super("FileLogWriter.FsyncThread");
            mSleepMS = fsyncIntervalMS;
            mFsyncLock = new Object();
            mRunning = true;
        }

        public void run() {
            boolean running = true;
            while (running) {
                // Sleep between fsyncs.
                try {
                    Thread.sleep(mSleepMS);
                } catch (InterruptedException e) {
                    mLog.warn("Sync thread interrupted", e);
                }

                try {
                    fsync();    // do the sync
                } catch (IOException e) {
                    String message = "Error while fsyncing " + mFile.getAbsolutePath() + "; Aborting.";
                    Zimbra.halt(message, e);
                }

                synchronized (mFsyncLock) {
                    running = mRunning;
                }
            }
        }

        // Stop the fsync thread.  Wait until the thread really stops.
        public void stopThread() {
            synchronized (mFsyncLock) {
                mRunning = false;
            }
            try {
                join();
            } catch (InterruptedException e) {
                mLog.warn("InterruptedException while stopping FsyncThread", e);
            }
        }
    }


    // Commit callback handling

    private static class Notif {
        private RedoCommitCallback mCallback;
        private CommitId mCommitId;

        public Notif(RedoCommitCallback callback, CommitId cid) {
            mCallback = callback;
            mCommitId = cid;
        }
        public RedoCommitCallback getCallback() { return mCallback; }
        public CommitId getCommitId() { return mCommitId; }
    }

    private class CommitNotifyQueue {
        private Notif[] mQueue = new Notif[100];
        private int mHead;  // points to first entry
        private int mTail;  // points to just after last entry (first empty slot)
        private boolean mFull;

        public CommitNotifyQueue(int size) {
            mQueue = new Notif[size];
            mHead = mTail = 0;
            mFull = false;
        }

        public synchronized void push(Notif notif) throws IOException {
            if (notif != null) {
                if (mFull) flush(true);  // queue is full
                assert(!mFull);
                mQueue[mTail] = notif;
                mTail++;
                mTail %= mQueue.length;
                mFull = mTail == mHead;
            }
        }

        private synchronized Notif pop() {
            if (mHead == mTail && !mFull) return null;  // queue is empty
            Notif n = mQueue[mHead];
            mQueue[mHead] = null;  // help with GC
            mHead++;
            mHead %= mQueue.length;
            mFull = false;
            return n;
        }

        public synchronized void flush(boolean fsync) throws IOException {
            if (fsync)
                fsync();
            Notif notif;
            while ((notif = pop()) != null) {
                RedoCommitCallback cb = notif.getCallback();
                assert(cb != null);
                try {
                    cb.callback(notif.getCommitId());
                } catch (Throwable t) {
                    ZimbraLog.misc.error("Error while making commit callback", t);
                }
            }
        }
    }
}
