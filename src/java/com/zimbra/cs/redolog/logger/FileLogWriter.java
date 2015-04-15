/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.redolog.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.redolog.FileRedoLogManager;
import com.zimbra.cs.redolog.FileRolloverManager;
import com.zimbra.cs.redolog.RedoConfig;
import com.zimbra.cs.redolog.RolloverManager;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.util.RedoLogFileUtil;
import com.zimbra.cs.util.Zimbra;

public class FileLogWriter extends AbstractLogWriter implements LogWriter {

    private static String SERVER_ID;
    static {
        try {
            SERVER_ID = Provisioning.getInstance().getLocalServer().getId();
        } catch (ServiceException e) {
            ZimbraLog.redolog.error("Unable to get local server ID", e);
            SERVER_ID = "unknown";
        }
    }

    // Synchronizes access to mRAF, mFileSize, mLogSeq, mFsyncSeq, mLogCount, and mFsyncCount.
    private final Object lock = new Object();

    // wait/notify between logger threads and fsync thread
    private final Object fsyncCond = new Object();

    private FileHeader header;
    private long firstOpTstamp;
    private long lastOpTstamp;
    private long createTime;

    private File file;
    private RandomAccessFile raf;
    private long fileSize;
    private long lastLogTime;

    private long fsyncIntervalMS;          // how many milliseconds fsync thread sleeps after each fync
    private boolean fsyncDisabled;

    private FsyncThread fsyncer;   // fsync thread

    private int logSeq;            // last item logged
    private int fsyncSeq;          // last item fsynced

    // for gathering some stats; nonessential for functionality
    private int logCount;          // how many times log was called
    private int fsyncCount;        // how many times fsync was called

    public FileLogWriter(FileRedoLogManager redoLogMgr,
                         File logfile,
                         long fsyncIntervalMS) {
        super(redoLogMgr);
        this.redoLogMgr = redoLogMgr;

        this.header = new FileHeader(SERVER_ID);
        this.file = logfile;
        this.fileSize = file.length();
        this.lastLogTime = file.lastModified();

        this.fsyncIntervalMS = fsyncIntervalMS;
        this.fsyncDisabled = DebugConfig.disableRedoLogFsync;

        this.fsyncCount = logCount = 0;
        setCommitNotifyQueue(new FileCommitNotifyQueue(100));
    }

    @Override public long getSequence() {
        synchronized (lock) {
            return header.getSequence();
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#getSize()
     */
    @Override public long getSize() {
        synchronized (lock) {
            return fileSize;
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#getCreateTime()
     */
    @Override public long getCreateTime() {
        synchronized (lock) {
            return createTime;
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#getLastLogTime()
     */
    @Override public long getLastLogTime() {
        synchronized (lock) {
        	return lastLogTime;
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#isEmpty()
     */
    @Override public boolean isEmpty() throws IOException {
        return getSize() <= FileHeader.HEADER_LEN;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#exists()
     */
    @Override public boolean exists() {
        return file.exists();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.logger.LogWriter#delete()
     */
    @Override public boolean delete() {
        return file.delete();
    }

    public RolloverManager getRolloverManager() {
        return redoLogMgr.getRolloverManager();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#open()
     */
    @Override public synchronized void open() throws IOException {
        synchronized (lock) {
            if (raf != null) return;  // already open

            raf = new RandomAccessFile(file, "rw");

            if (raf.length() >= FileHeader.HEADER_LEN) {
                header.read(raf);
                createTime = header.getCreateTime();
                if (createTime == 0) {
                    createTime = System.currentTimeMillis();
                    header.setCreateTime(createTime);
                }
                firstOpTstamp = header.getFirstOpTstamp();
                lastOpTstamp = header.getLastOpTstamp();
                if (header.getSequence() > getRolloverManager().getCurrentSequence()) {
                    getRolloverManager().initSequence(header.getSequence());
                }
            } else {
                createTime = System.currentTimeMillis();
                header.setCreateTime(createTime);
                header.setSequence(getRolloverManager().incrementSequence());
            }
            header.setOpen(true);
            header.write(raf);

            // go to the end of file, so we can append
            long len = raf.length();
            raf.seek(len);
            fileSize = len;

            logSeq = fsyncSeq = 0;
        }

        if (fsyncIntervalMS > 0)
            startFsyncThread();
    }

    protected synchronized void populateHeader() throws IOException {
        synchronized(lock) {
            if (!file.exists()) {
                return;
            }
            RandomAccessFile localRaf = new RandomAccessFile(file, "r");
            if (localRaf.length() >= FileHeader.HEADER_LEN) {
                header.read(localRaf);
                createTime = header.getCreateTime();
                firstOpTstamp = header.getFirstOpTstamp();
                lastOpTstamp = header.getLastOpTstamp();
            }
            localRaf.close();
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.LogWriter#close()
     */
    @Override public synchronized void close() throws IOException {
        stopFsyncThread();

        synchronized (lock) {
            if (raf != null) {
                if (lastOpTstamp != 0)
                	header.setLastOpTstamp(lastOpTstamp);
                header.setOpen(false);
                header.setFileSize(raf.length());
                header.write(raf);

                raf.getChannel().force(true);
                raf.close();
                raf = null;
            } else
                return;
        }

        // Write some stats, so we can see how many times we were able to avoid calling fsync.
        if (!mNoStat && logCount > 0 && ZimbraLog.redolog.isDebugEnabled())
            ZimbraLog.redolog.debug("Logged: " + logCount + " items, " + fsyncCount + " fsyncs");
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
    @Override public void log(RedoableOp op, InputStream data, boolean synchronous) throws IOException {
        int seq;
        boolean sameMboxAsLastOp = false;

        synchronized (lock) {
            if (raf == null) {
                throw new IOException("Redolog file closed");
            }

            // Record first transaction in header.
            long tstamp = op.getTimestamp();
            lastOpTstamp = Math.max(tstamp, lastOpTstamp);
            if (firstOpTstamp == 0) {
            	firstOpTstamp = tstamp;
                header.setFirstOpTstamp(firstOpTstamp);
                header.setLastOpTstamp(lastOpTstamp);
                long pos = raf.getFilePointer();
                header.write(raf);
                raf.seek(pos);
            }

            logSeq++;
            logCount++;
            seq = logSeq;
            int numRead;
            byte[] buf = new byte[1024];
            while ((numRead = data.read(buf)) >= 0) {
                raf.write(buf, 0, numRead);
                fileSize += numRead;
            }
            data.close();

            // We do this with log writer lock held, so the commits and any
            // callbacks made on their behalf are truly in the correct order.
            notifyCallback(op);

            lastLogTime = System.currentTimeMillis();

            sameMboxAsLastOp = mLastOpMboxId == op.getMailboxId();
            mLastOpMboxId = op.getMailboxId();
        }

        // cases 1 above
        if (!synchronous) {
            return;
        }

        if (fsyncIntervalMS > 0) {
            if (!sameMboxAsLastOp) {
                // case 2
                try {
                    // wait for fsync thread to write this entry to disk
                    synchronized (fsyncCond) {
                        fsyncCond.wait(10000);
                    }
                } catch (InterruptedException e) {
                    ZimbraLog.redolog.info("Thread interrupted during fsync");
                }
                synchronized (lock) {
                    // timed out, so fsync in this thread
                    if (seq > fsyncSeq) {
                        fsync();
                    }
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

    @Override
    public void flush() throws IOException {
        fsync();
    }

    private boolean mNoStat;
    public void noStat(boolean b) {
    	mNoStat = b;
    }

    protected boolean deleteOnRollover() {
        return RedoConfig.redoLogDeleteOnRollover();
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void rollover(LinkedHashMap /*<TxnId, RedoableOp>*/ activeOps)
    throws IOException {
        FileRolloverManager romgr = (FileRolloverManager) getRolloverManager();

        //cast OK since file writer depends on file rollover...but could be cleaner code
        long lastSeq = getSequence();

        // Close current log, so it's impossible for its content to change.
        noStat(true);
        close();

        String currentPath = file.getAbsolutePath();

        // Open a temporary logger.
        // temp (arbitrary) filename here uses current sequence+1 as rough guess
        // real sequence number assigned on open
        File tempLogfile = new File(file.getParentFile(), RedoLogFileUtil.getTempFilename(romgr.getCurrentSequence() + 1));
        FileLogWriter tempLogger =
            new FileLogWriter((FileRedoLogManager) redoLogMgr, tempLogfile, 0);
        tempLogger.open();
        tempLogger.noStat(true);

        if (activeOps != null) {
            // Rewrite change entries for all active operations, maintaining
            // their order of occurrence.  (LinkedHashMap ensures ordering.)
            Set opsSet = activeOps.entrySet();
            for (Iterator it = opsSet.iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                RedoableOp op = (RedoableOp) entry.getValue();
                tempLogger.log(op, op.getInputStream(), false);
            }
        }
        tempLogger.close();

        // Rename the current log to rolled-over name.
        File rolloverFile = romgr.getRolloverFile(lastSeq);
        if (deleteOnRollover()) {
            // Delete the current log.  We don't need to hold on to the
            // indexing-only log files after rollover.
            ZimbraLog.redolog.debug("deleting current redolog %s", file.getAbsolutePath());
            if (!file.delete()) {
                throw new IOException("Unable to delete current redo log " + file.getAbsolutePath());
            }
        } else {
            ZimbraLog.redolog.debug("renaming %s to %s during rollover", file.getAbsolutePath(), rolloverFile.getAbsolutePath());
            File destDir = rolloverFile.getParentFile();
            if (destDir != null && !destDir.exists()) {
                destDir.mkdirs();
            }
            if (!file.renameTo(rolloverFile)) {
                throw new IOException("Unable to rename current redo log to " + rolloverFile.getAbsolutePath());
            }
        }

        // Rename the temporary logger to current logfile name.
        String tempPath = tempLogfile.getAbsolutePath();
        file = new File(currentPath);
        if (!tempLogfile.renameTo(file)) {
            throw new IOException("Unable to rename " + tempPath + " to " + currentPath);
        }
        // Reopen current log.
        open();
        noStat(false);
        ZimbraLog.redolog.info("file %s rolled over from %d to %d", file.getAbsolutePath(), lastSeq, getSequence());
        return;
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
        if (fsyncer == null && fsyncIntervalMS > 0) {
            fsyncer = new FsyncThread(fsyncIntervalMS);
            fsyncer.start();
        }
    }

    private synchronized void stopFsyncThread() {
        if (fsyncer != null) {
            fsyncer.stopThread();
            fsyncer = null;
        }
    }

    // do fsync if there are items logged since last fsync
    void fsync() throws IOException {
        boolean fsyncNeeded = false;
        int seq = 0;
        synchronized (lock) {
            if (fsyncSeq < logSeq) {
                if (raf == null)
                    throw new IOException("Redolog file closed");
                fsyncNeeded = true;
                seq = logSeq;
                if (!fsyncDisabled)
                    fsyncCount++;
            }
        }
        if (fsyncNeeded) {
            if (!fsyncDisabled) {
                synchronized (lock) {
                    if (raf != null) {
                        raf.getChannel().force(false);
                    } else {
                        throw new IOException("Redolog file closed");
                    }
                    //TODO: move to abstract?
                    getCommitNotifyQueue().flushInternal(false);
                }
            }
            synchronized (lock) {
                fsyncSeq = seq;
            }
            if (fsyncIntervalMS > 0) {
                synchronized (fsyncCond) {
                    fsyncCond.notifyAll();
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

        private static final long MIN_SLEEP_MILLIS = 1;
        private static final long MAX_SLEEP_MILLIS = 1000;  // never sleep longer than 1 seconds

        public FsyncThread(long fsyncIntervalMS) {
            super("FileLogWriter.FsyncThread-"+System.currentTimeMillis());
            // Sanity check the sleep interval.
            if (fsyncIntervalMS < MIN_SLEEP_MILLIS) {
                ZimbraLog.redolog.warn("Invalid fsync thread sleep interval %dms; using %dms instead",
                        fsyncIntervalMS, MIN_SLEEP_MILLIS);
                mSleepMS = MIN_SLEEP_MILLIS;
            } else if (fsyncIntervalMS > MAX_SLEEP_MILLIS) {
                ZimbraLog.redolog.warn("Fsync thread sleep interval %ms is too long; using %dms instead",
                        fsyncIntervalMS, MAX_SLEEP_MILLIS);
                mSleepMS = MAX_SLEEP_MILLIS;
            } else {
                mSleepMS = fsyncIntervalMS;
            }
            mFsyncLock = new Object();
            mRunning = true;
        }

        @Override public void run() {
            boolean running = true;
        	ZimbraLog.redolog.info("Starting fsync thread with interval %d", mSleepMS);
            while (running) {
                // Sleep between fsyncs.
                try {
                	ZimbraLog.redolog.trace("Sleeping for %s", mSleepMS);
                    Thread.sleep(mSleepMS);
                	ZimbraLog.redolog.trace("Slept for %s running? %s", mSleepMS, mRunning);
                } catch (InterruptedException e) {
                    ZimbraLog.redolog.warn("Sync thread interrupted", e);
                }

                try {
                    fsync();    // do the sync
                } catch (IOException e) {
                    String message = "Error while fsyncing " + file.getAbsolutePath() + "; Aborting.";
                    Zimbra.halt(message, e);
                }

                synchronized (mFsyncLock) {
                    running = mRunning;
                }
            }
            ZimbraLog.redolog.info("fsync thread exiting");
        }

        // Stop the fsync thread.  Wait until the thread really stops.
        public void stopThread() {
            synchronized (mFsyncLock) {
                mRunning = false;
            }
            try {
            	while (isAlive()) {
            		if (ZimbraLog.redolog.isTraceEnabled()) {
            			ZimbraLog.redolog.trace("waiting for %s to finish. running? %s", getName(), mRunning);
            		} else {
            			ZimbraLog.redolog.info("waiting for %s to finish.", getName());
            		}
            		join(Constants.MILLIS_PER_MINUTE);
            	}
            	ZimbraLog.redolog.info("%s finished", getName());
            } catch (InterruptedException e) {
                ZimbraLog.redolog.warn("InterruptedException while stopping FsyncThread", e);
            }
        }
    }

    @Override
    protected FileCommitNotifyQueue getCommitNotifyQueue() {
        return (FileCommitNotifyQueue) super.getCommitNotifyQueue();
    }

    private class FileCommitNotifyQueue extends CommitNotifyQueue {
        public FileCommitNotifyQueue(int size) {
            super(size);
        }

        @Override
        public synchronized void flush() throws IOException {
            flushInternal(true);
        }

        private synchronized void flushInternal(boolean fsync) throws IOException {
            if (fsync) {
                fsync();
            }
            super.flush();
        }
    }
}
