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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
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
        mTxnIdGenerator = new TxnIdGenerator() {
            @Override
            public TransactionId getNext() {
                //return uninitialized txnid so service can assign them
                return new TransactionId();
            }
        };

    }

    @Override
    public LogWriter createLogWriter(long fsyncIntervalMS) {
        return new HttpLogWriter(this);
    }

    @Override
    protected void signalLogError(Throwable e) throws ServiceException {
        throw ServiceException.FAILURE("redolog failure", e);
    }

    @Override
    public File getLogFile() {
        //TODO: move to a different interface
        //http redolog manager cannot return 'files'
        //any file interaction must occur on redolog host
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initRedoLog() throws IOException {
        //no special init required here
    }

    @Override
    public File[] getArchivedLogsFromSequence(long seq) throws IOException {
        //TODO: move this into a different interface
        //http redolog manager cannot return 'files'
        //any file interaction must occur on the redolog host
        throw new UnsupportedOperationException();
    }

    @Override
    public Pair<Set<Integer>, CommitId> getChangedMailboxesSince(CommitId cid)
            throws IOException, MailServiceException {
        //this is used by AllAccountsWaitSet if a waitset is no longer in memory (i.e. a JVM restart)
        //rather than increasing the coupling between waitset and redolog; we just return null here
        //this will cause the request to return 'unable to sync to commitId' and client can create a new waitset
        //TODO: eventually should have AllAccountsWaitSet wait on shared notification channel rather than redolog
        return null;
    }

    @Override
    protected boolean isRolloverNeeded(boolean immediate) {
        //HTTP log manager does not need to deal with rollover
        return false;
    }

}
