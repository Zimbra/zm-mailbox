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
import java.util.Set;

import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.redolog.logger.HttpLogWriter;
import com.zimbra.cs.redolog.logger.LogWriter;

/**
 * Redolog manager which writes operations to http redolog service
 *
 */
public class HttpRedoLogManager extends AbstractRedoLogManager {

    public HttpRedoLogManager() {
        super();
        mRolloverMgr = new HttpRolloverManager();
        //TODO: different txnid generator
        mTxnIdGenerator = new TxnIdGenerator();
    }

    @Override
    public LogWriter createLogWriter(long fsyncIntervalMS) {
        return new HttpLogWriter();
    }

    @Override
    public File getLogFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initRedoLog() throws IOException {
    }

    @Override
    public File[] getArchivedLogsFromSequence(long seq) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pair<Set<Integer>, CommitId> getChangedMailboxesSince(CommitId cid)
            throws IOException, MailServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isRolloverNeeded(boolean immediate) {
        //rollover can generally be ignored by http redolog clients
        boolean result = false;
        if (immediate) {
            try {
                result = !mLogWriter.isEmpty();
            } catch (IOException e) {
                ZimbraLog.redolog.warn("ioexception determing if rollover is needed; assuming it is not (or wouldn't succeed anyway)", e);
            }
        }
        return result;
    }

}
