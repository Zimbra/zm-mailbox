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

package com.zimbra.cs.redolog.logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.HttpStatus;

import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.op.RedoableOp;

public class HttpLogWriter extends AbstractLogWriter implements LogWriter {

    public HttpLogWriter(RedoLogManager redoLogMgr) {
        super(redoLogMgr, new CommitNotifyQueue(100));
    }

    //TODO: config
    private String URL = "http://localhost:8080/redolog";

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    private TransactionId deserializeTxnId(PostMethod post) throws IOException {
        RedoLogInput in = new RedoLogInput(post.getResponseBodyAsStream());
        TransactionId txnId = new TransactionId();
        txnId.deserialize(in);
        return txnId;
    }

    @Override
    public void log(final RedoableOp op, final InputStream data, boolean synchronous) throws IOException {
        HttpClient client = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        PostMethod post = new PostMethod(URL);
        try {
            post.setRequestEntity(new InputStreamRequestEntity(data));
            int code = client.executeMethod(post);
            if (code != HttpStatus.SC_OK) {
                throw new IOException("unexpected response from redolog servlet [" + code + "] message:[" + post.getResponseBodyAsString() + "]");
            }
            if (!op.getTransactionId().isInitialized()) {
                op.setTransactionId(deserializeTxnId(post));
            } else {
                assert(op.getTransactionId().equals(deserializeTxnId(post)));
            }
        } finally {
            post.releaseConnection();
        }
        notifyCallback(op);
    }

    @Override
    protected void notifyCallback(RedoableOp op) throws IOException {
        super.notifyCallback(op);
        getCommitNotifyQueue().flush(); //immediate flush since we're not waiting for filesystem sync on this end
    }

    @Override
    public void flush() throws IOException {
        //maybe do nothing; maybe add buffering...
        //relevant?
    }

    @Override
    public long getSize() {
        //size since rollover
        //relevant?
        return 0;
    }

    @Override
    public long getCreateTime() {
        //timestamp of start or last rollover (first op?)
        //relevant
        return 0;
    }

    @Override
    public long getLastLogTime() {
        //last op
        return 0;
    }

    @Override
    public boolean isEmpty() throws IOException {
        //count > 0
        return false;
    }

    @Override
    public boolean exists() {
        //similar to isEmpty?
        return false;
    }

    @Override
    public boolean delete() {
        //delete since last rollover mark?
        return false;
    }

    @Override
    public void rollover(LinkedHashMap activeOps) throws IOException {
        //could fire a rollover; but does it make sense?...let redo server handle it
    }

    @Override
    public long getSequence() {
        //current sequence of rollover set?
        return 0;
    }

}
