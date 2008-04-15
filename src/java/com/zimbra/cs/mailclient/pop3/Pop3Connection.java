/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.pop3;

import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.MailOutputStream;
import com.zimbra.cs.mailclient.CommandFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Pop3Connection extends MailConnection {
    private Pop3Response response;
    
    public Pop3Connection(Pop3Config config) {
        super(config);
    }

    protected MailInputStream getMailInputStream(InputStream is) {
        return new MailInputStream(is);
    }

    protected MailOutputStream getMailInputStream(OutputStream os) {
        return new MailOutputStream(os);
    }
    
    protected void processGreeting() throws IOException {
        response = Pop3Response.read(null, mailIn);
        if (!response.isOK()) {
            throw new MailException(
                "Expected greeting, but got: " + response.getMessage());
        }
    }

    public void logout() throws IOException {
        sendCommandCheckStatus("QUIT", null);
    }

    protected void sendLogin(String user, String pass) throws IOException {
        sendCommandCheckStatus("USER", user);
        sendCommandCheckStatus("PASS", pass);
    }
    
    protected void sendAuthenticate(boolean ir) throws IOException {
        StringBuffer sb = new StringBuffer(config.getMechanism());
        if (ir) {
            byte[] response = authenticator.getInitialResponse();
            sb.append(' ').append(encodeBase64(response));
        }
        sendCommandCheckStatus("AUTH", sb.toString());
    }

    protected void sendStartTls() throws IOException {
        sendCommandCheckStatus("STLR", null);
    }

    public Pop3Response sendCommand(String cmd, String args) throws IOException {
        String line = cmd;
        if (args != null) line += " " + args;
        mailOut.writeLine(line);
        mailOut.flush();
        while (true) {
            response = Pop3Response.read(cmd, mailIn);
            if (!response.isContinuation()) break;
            processContinuation(response.getMessage());
        }
        return response;
    }

    public Pop3Response sendCommandCheckStatus(String cmd, String args)
            throws IOException {
        Pop3Response res = sendCommand(cmd, args);
        if (!res.isOK()) {
            throw new CommandFailedException(cmd, res.getMessage());
        }
        return res;
    }
}
