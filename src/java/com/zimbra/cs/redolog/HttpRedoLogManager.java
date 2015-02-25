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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;

import com.ibm.icu.util.StringTokenizer;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
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
        return new HttpLogWriter(this, URL);
    }

    @Override
    protected void signalLogError(Throwable e) throws ServiceException {
        throw ServiceException.FAILURE("redolog failure", e);
    }

    @Override
    protected void initRedoLog() throws IOException {
        //no special init required here
    }

    private HttpRedoLogFile[] getLogRefs(String query) throws IOException {
        GetMethod get = new GetMethod(URL);
        try {
            get.setQueryString(query);
            HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
            int code = client.executeMethod(get);
            if (code == HttpServletResponse.SC_NOT_FOUND) {
                return new HttpRedoLogFile[0];
            } else if (code != HttpServletResponse.SC_OK) {
                throw new IOException("bad response getting redologs ["+code+"] response:"+get.getResponseBodyAsString());
            }
            String body = get.getResponseBodyAsString();
            StringTokenizer stoke = new StringTokenizer(body, "\r\n");
            List<HttpRedoLogFile> logs = new ArrayList<HttpRedoLogFile>();
            HttpRedoLogFile logRef = null;
            while (stoke.hasMoreTokens()) {
                String line = stoke.nextToken();
                logRef = HttpRedoLogFile.decodeFromString(line);
                ZimbraLog.redolog.debug("got logRef %s", logRef.encodeToString());
                logs.add(logRef);
            }
            return logs.toArray(new HttpRedoLogFile[logs.size()]);
        } finally {
            get.releaseConnection();
        }
    }

    @Override
    public File getLogFile() throws IOException {
        HttpRedoLogFile[] logRefs = getLogRefs("fmt=fileref&type=current");
        assert(logRefs != null && logRefs.length == 1);
        return logRefs[0].getFile();
    }

    @Override
    public RedoLogFile[] getArchivedLogsFromSequence(long seq) throws IOException {
        return getLogRefs("fmt=fileref&type=archive&seq="+seq);
    }

    @Override
    public RedoLogFile getArchivedLog(long seq) {
        //TODO: may not need to be implemented here?
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

    //TODO: config
    static String URL = "http://localhost:8080/redolog";

    private long seqNum = -1;
    private long lastSeqUpdateTime = -1;
    private static final int SEQ_UPDATE_INTERVAL = 30000; //TODO:debug config?

    @Override
    public long getCurrentLogSequence() throws IOException {
        //called frequently during full backup
        //return a watermark and update every so often rather than flooding network
        //it should be ok for full backup to have a slightly old marker, as this only
        //means the next incremental might start one seq num too soon
        //TODO: validate assumptions further
        if (seqNum < 0 || (System.currentTimeMillis() - lastSeqUpdateTime > SEQ_UPDATE_INTERVAL)) {
            HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
            GetMethod get = new GetMethod(URL);
            get.setQueryString("fmt=seq");
            int code = client.executeMethod(get);
            if (code != HttpStatus.SC_OK) {
                throw new IOException("unexpected response from redolog servlet [" + code + "] message:[" + get.getResponseBodyAsString() + "]");
            }
            seqNum = Long.parseLong(get.getResponseBodyAsString().trim());
        }
        return seqNum;
    }

    @Override
    public void deleteArchivedLogFiles(long oldestTimestamp) throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PostMethod post = new PostMethod(URL);
        try {
            post.setParameter("cmd", "delete");
            post.setParameter("cutoff", System.currentTimeMillis()+"");
            int code = client.executeMethod(post);
            if (code != HttpServletResponse.SC_OK) {
                throw new IOException("unexpected response from redolog servlet [" + code + "] message:[" + post.getResponseBodyAsString() + "]");
            }
        } finally {
            post.releaseConnection();
        }
    }

}
