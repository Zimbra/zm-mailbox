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

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.tcpserver.TcpServerInputStream;

import java.net.Socket;
import java.io.IOException;

public class TcpLmtpHandler extends LmtpHandler {
    private TcpServerInputStream mInputStream;

    TcpLmtpHandler(LmtpServer server) {
        super(server);
    }

    @Override
    protected boolean setupConnection(Socket connection) throws IOException {
        reset();
        mInputStream = new TcpServerInputStream(connection.getInputStream());
        mWriter = new LmtpWriter(connection.getOutputStream());
        return setupConnection(connection.getInetAddress());
    }

    @Override
    protected synchronized void dropConnection() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mWriter != null) {
                mWriter.close();
                mWriter = null;
            }
        } catch (IOException e) {
            ZimbraLog.lmtp.info("exception while closing connection", e);
        }
    }

    @Override
    protected boolean processCommand() throws IOException {
        return processCommand(mInputStream.readLine());
    }

    @Override
    protected void continueDATA() throws IOException {
        LmtpMessageInputStream min = new LmtpMessageInputStream(mInputStream, getAdditionalHeaders());
        processMessageData(min);
    }
}
