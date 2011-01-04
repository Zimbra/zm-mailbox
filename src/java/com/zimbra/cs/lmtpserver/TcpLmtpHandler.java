/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.tcpserver.TcpServerInputStream;

import java.net.Socket;
import java.io.IOException;

public class TcpLmtpHandler extends LmtpHandler {
    private TcpServerInputStream inputStream;

    TcpLmtpHandler(LmtpServer server) {
        super(server);
    }

    @Override
    protected boolean setupConnection(Socket connection) throws IOException {
        reset();
        connection.setSoTimeout(mConfig.getMaxIdleTime() * 1000);
        inputStream = new TcpServerInputStream(connection.getInputStream());
        mWriter = new LmtpWriter(connection.getOutputStream());
        return setupConnection(connection.getInetAddress());
    }

    @Override
    protected synchronized void dropConnection() {
        ZimbraLog.addIpToContext(mRemoteAddress);
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (mWriter != null) {
                mWriter.close();
                mWriter = null;
            }
        } catch (IOException e) {
            if (ZimbraLog.lmtp.isDebugEnabled()) {
                ZimbraLog.lmtp.info("I/O error while closing connection", e);
            } else {
                ZimbraLog.lmtp.info("I/O error while closing connection: " + e);
            }
        } finally {
            ZimbraLog.clearContext();
        }
    }

    @Override
    protected boolean processCommand() throws IOException {
        // make sure that the connection wasn't dropped during a preceding command processing
        if (inputStream != null)
            return processCommand(inputStream.readLine());
        return false;
    }

    @Override
    protected void continueDATA() throws IOException {
        LmtpMessageInputStream min = new LmtpMessageInputStream(inputStream, getAdditionalHeaders());
        try {
            processMessageData(min);
        } catch (UnrecoverableLmtpException e) {
            dropConnection();
        }
    }
}
