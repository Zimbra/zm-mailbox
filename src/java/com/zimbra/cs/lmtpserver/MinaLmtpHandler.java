/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.lmtpserver;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaIoSessionOutputStream;
import com.zimbra.cs.mina.MinaOutputStream;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaTextLineRequest;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MinaLmtpHandler extends LmtpHandler implements MinaHandler {
    private final IoSession mSession;
    private boolean expectingMessageData;
    private MinaOutputStream mOutputStream;

    private static final long WRITE_TIMEOUT = 5000;
    
    public MinaLmtpHandler(MinaLmtpServer server, IoSession session) {
        super(server);
        mSession = session;
    }
    
    public void requestReceived(MinaRequest req) throws IOException {
        if (expectingMessageData) {
            // XXX bburtin: This code is currently instantiating a byte array.  It should
            // be rewritten to use streams.
            byte[] data = ((MinaLmtpDataRequest) req).getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            LmtpMessageInputStream messageStream = new LmtpMessageInputStream(bais, getAdditionalHeaders());
            processMessageData(messageStream);
            expectingMessageData = false;
        } else {
            processCommand(((MinaTextLineRequest) req).getLine());
        }
    }

    MinaRequest createRequest() {
        return expectingMessageData ?
            new MinaLmtpDataRequest(mEnvelope.getSize(), getAdditionalHeaders()) :
            new MinaTextLineRequest();
    }
    
    public void connectionOpened() {
        reset();
        mOutputStream = new MinaIoSessionOutputStream(mSession);
        mWriter = new LmtpWriter(mOutputStream);
        mSession.setIdleTime(IdleStatus.BOTH_IDLE, mConfig.getMaxIdleSeconds());
        setupConnection(((InetSocketAddress) mSession.getRemoteAddress()).getAddress());
    }

    public void connectionClosed() {
        mSession.close();
    }

    public void connectionIdle() {
        notifyIdleConnection();
        dropConnection();
    }

    @Override
    protected void continueDATA() {
        expectingMessageData = true;
    }
    
    @Override
    protected void dropConnection() {
        dropConnection(WRITE_TIMEOUT);
    }

    public void dropConnection(long timeout) {
        if (!mSession.isConnected()) return;
        try {
            mOutputStream.close();
        } catch (IOException e) {
            // Should never happen...
        }
        if (timeout >= 0) {
            // Wait for all remaining bytes to be written
            if (!mOutputStream.join(timeout)) {
                ZimbraLog.lmtp.warn("Force closing session because write timed out: " + mSession);
            }
        }
        mSession.close();
    }
    
    @Override
    protected boolean setupConnection(Socket connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }
}
