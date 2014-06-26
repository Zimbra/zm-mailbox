/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.cs.lmtpserver;

import com.zimbra.common.io.TcpServerInputStream;
import com.zimbra.common.util.ZimbraLog;

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
        processMessageData(min);
    }
}
