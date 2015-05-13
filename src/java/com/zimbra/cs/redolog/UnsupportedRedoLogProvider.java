/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.redolog.op.RedoableOp;

/**
 * stubbed implementation for call sites which need a non-null provider but don't actually use it
 *
 */
public class UnsupportedRedoLogProvider extends RedoLogProvider {

    @Override
    public boolean isMaster() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSlave() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startup(boolean runCrashRecovery) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() throws ServiceException {
    }

    @Override
    public void initRedoLogManager() {
        mRedoLogManager = new RedoLogManager() {

            @Override
            public boolean getInCrashRecovery() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void start(boolean runCrashRecovery) throws ServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stop() {
                throw new UnsupportedOperationException();
            }

            @Override
            public TransactionId getNewTxnId() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void log(RedoableOp op, boolean synchronous)
                    throws ServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void commit(RedoableOp op) throws ServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void abort(RedoableOp op) throws ServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void flush() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forceRollover() throws ServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forceRollover(boolean skipCheckpoint)
                    throws ServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void logOnly(RedoableOp op, boolean synchronous)
                    throws ServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public RolloverManager getRolloverManager() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getCurrentLogSequence() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public LogWriter createLogWriter(long fsyncIntervalMS) {
                throw new UnsupportedOperationException();
            }

            @Override
            public LogWriter getLogWriter() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Pair<Set<Integer>, CommitId> getChangedMailboxesSince(
                    CommitId cid) throws IOException, MailServiceException {
                throw new UnsupportedOperationException();
            }

            @Override
            public File getLogFile() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public RedoLogFile[] getArchivedLogsFromSequence(long seq)
                    throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public RedoLogFile getArchivedLog(long seq) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RedoLogFile[] getArchivedLogs() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteArchivedLogFiles(long oldestTimestamp)
                    throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean supportsCrashRecovery() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void crashRecoverMailboxes(Map<Integer, Integer> mboxIdMap) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean getInCrashRecovery(int mboxId) {
                throw new UnsupportedOperationException();
            }

        };
    }
}
