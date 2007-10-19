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

import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaIoSessionOutputStream;
import com.zimbra.cs.mina.MinaTextLineRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.IdleStatus;

public class MinaLmtpHandler extends LmtpHandler implements MinaHandler {
    private final IoSession mSession;
    private boolean expectingMessageData;

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
        mWriter = new LmtpWriter(new MinaIoSessionOutputStream(mSession));
        mSession.setIdleTime(IdleStatus.BOTH_IDLE, mConfig.getMaxIdleSeconds());
        setupConnection(((InetSocketAddress) mSession.getRemoteAddress()).getAddress());
    }

    public void connectionClosed() {
        dropConnection();
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
        if (!mSession.isClosing()) {
            mWriter.close();
            mSession.close();
        }
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
