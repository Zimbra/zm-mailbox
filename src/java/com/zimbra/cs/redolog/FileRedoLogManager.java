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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.logger.FileLogWriter;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.CommitTxn;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.znative.IO;

public class FileRedoLogManager extends AbstractRedoLogManager implements
        RedoLogManager {

    File mArchiveDir; // where log files are archived as they get rolled over
    File mLogFile; // full path to the "redo.log" file

    FileRedoLogManager(File redolog, File archdir, boolean supportsCrashRecovery) {
        mEnabled = false;
        mShuttingDown = false;
        mRecoveryMode = false;
        mSupportsCrashRecovery = supportsCrashRecovery;

        mLogFile = redolog;
        mArchiveDir = archdir;

        mRWLock = new ReentrantReadWriteLock();
        mActiveOps = new LinkedHashMap<TransactionId, RedoableOp>(100);
        mTxnIdGenerator = new TxnIdGenerator();
        long minAge = RedoConfig.redoLogRolloverMinFileAge() * 60 * 1000; // milliseconds
        long softMax = RedoConfig.redoLogRolloverFileSizeKB() * 1024; // bytes
        long hardMax = RedoConfig.redoLogRolloverHardMaxFileSizeKB() * 1024; // bytes
        setRolloverLimits(minAge, softMax, hardMax);
        mRolloverMgr = new FileRolloverManager(this, mLogFile);
        mLogWriter = null;

        mStatGuard = new Object();
        mElapsed = 0;
        mCounter = 0;
    }

    @Override
    protected void initRedoLog() throws IOException {
        File logdir = mLogFile.getParentFile();
        if (!logdir.exists()) {
            if (!logdir.mkdirs())
                throw new IOException("Unable to create directory "
                        + logdir.getAbsolutePath());
        }
        if (!mArchiveDir.exists()) {
            if (!mArchiveDir.mkdirs())
                throw new IOException("Unable to create directory "
                        + mArchiveDir.getAbsolutePath());
        }
    }

    /**
     * Returns the File object for the one and only redo log file "redo.log".
     *
     * @return
     */
    @Override
    public File getLogFile() {
        return mLogFile;
    }

    public File getRolloverDestDir() {
        return mArchiveDir;
    }

    @Override
    public LogWriter createLogWriter(long fsyncIntervalMS) {
        return new FileLogWriter(this, mLogFile, fsyncIntervalMS);
    }

    @Override
    public File[] getArchivedLogsFromSequence(long seq) throws IOException {
        return FileRolloverManager.getArchiveLogs(mArchiveDir, seq);
    }

    @Override
    public Pair<Set<Integer>, CommitId> getChangedMailboxesSince(CommitId cid)
            throws IOException, MailServiceException {
        Set<Integer> mailboxes = new HashSet<Integer>();

        // Grab a read lock to prevent rollover.
        ReadLock readLock = mRWLock.readLock();
        try {
            readLock.lockInterruptibly();
        } catch (InterruptedException e) {
            synchronized (mShuttingDownGuard) {
                if (!mShuttingDown)
                    ZimbraLog.redolog
                            .error("InterruptedException during redo log scan for CommitId",
                                    e);
                else
                    ZimbraLog.redolog
                            .debug("Redo log scan for CommitId interrupted for shutdown");
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
                    throw MailServiceException
                            .INVALID_COMMIT_ID(cid.toString());
                }

                // Create a temp directory and make hard links to all redologs.
                // This prevents the logs from disappearing while being scanned.
                String dirName = "tmp-scan-" + System.currentTimeMillis();
                linkDir = new File(mLogFile.getParentFile(), dirName);
                if (linkDir.exists()) {
                    int suffix = 1;
                    while (linkDir.exists()) {
                        linkDir = new File(mLogFile.getParentFile(), dirName
                                + "-" + suffix);
                    }
                }
                if (!linkDir.mkdir())
                    throw new IOException("Unable to create temp dir "
                            + linkDir.getAbsolutePath());
                for (int i = 0; i < logs.length; i++) {
                    File src = logs[i];
                    File dest = new File(linkDir, logs[i].getName());
                    IO.link(src.getAbsolutePath(), dest.getAbsolutePath());
                    logs[i] = dest;
                }
            } finally {
                // We can let rollover happen now.
                readLock.unlock();
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
                            int mboxId = op.getMailboxId();
                            if (mboxId > 0)
                                mailboxes.add(mboxId);
                        } else {
                            if (cid.matches(lastCommitTxn))
                                foundMarker = true;
                        }
                    }
                } catch (IOException e) {
                    ZimbraLog.redolog.warn(
                            "IOException while reading redolog file", e);
                } finally {
                    logReader.close();
                }
            }
            if (!foundMarker) {
                // Most likely, the CommitId is too old.
                throw MailServiceException.INVALID_COMMIT_ID(cid.toString());
            }
            CommitId lastCommitId = new CommitId(lastSeq, lastCommitTxn);
            return new Pair<Set<Integer>, CommitId>(mailboxes, lastCommitId);
        } finally {
            if (linkDir != null) {
                // Clean up the temp dir with links.
                try {
                    if (linkDir.exists())
                        FileUtil.deleteDir(linkDir);
                } catch (IOException e) {
                    ZimbraLog.redolog.warn(
                            "Unable to delete temporary directory "
                                    + linkDir.getAbsolutePath(), e);
                }
            }
        }
    }

}

