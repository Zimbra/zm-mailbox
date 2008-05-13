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

import org.apache.log4j.Logger;

public final class Pop3Connection extends MailConnection {
    private Pop3Response response;

    private static final Logger LOGGER = Logger.getLogger(Pop3Connection.class);
    
    private static final String PASS = "PASS";
    private static final String USER = "USER";
    private static final String AUTH = "AUTH";
    private static final String STLR = "STLR";
    private static final String QUIT = "QUIT";

    public Pop3Connection(Pop3Config config) {
        super(config);
    }

    protected MailInputStream newMailInputStream(InputStream is) {
        return new MailInputStream(is);
    }

    protected MailOutputStream newMailOutputStream(OutputStream os) {
        return new MailOutputStream(os);
    }

    public Logger getLogger() {
        return LOGGER;
    }
    
    protected void processGreeting() throws IOException {
        response = Pop3Response.read(null, mailIn);
        if (!response.isOK()) {
            throw new MailException(
                "Expected greeting, but got: " + response.getMessage());
        }
        setState(State.NOT_AUTHENTICATED);
    }

    public void logout() throws IOException {
        setState(State.LOGOUT);
        sendCommandCheckStatus(QUIT, null);
        setState(State.CLOSED);
    }

    protected void sendLogin(String user, String pass) throws IOException {
        sendCommandCheckStatus(USER, user);
        sendCommandCheckStatus(PASS, pass);
    }
    
    protected void sendAuthenticate(boolean ir) throws IOException {
        StringBuffer sb = new StringBuffer(config.getMechanism());
        if (ir) {
            byte[] response = authenticator.getInitialResponse();
            sb.append(' ').append(encodeBase64(response));
        }
        sendCommandCheckStatus(AUTH, sb.toString());
    }

    protected void sendStartTls() throws IOException {
        sendCommandCheckStatus(STLR, null);
    }

    public Pop3Response sendCommand(String cmd, String args) throws IOException {
        mailOut.write(cmd);
        if (args != null) {
            mailOut.write(' ');
            if (cmd.equalsIgnoreCase(PASS)) {
                writeUntraced(args);
            } else {
                mailOut.write(args);
            }
        }
        mailOut.newLine();
        mailOut.flush();
        while (true) {
            response = Pop3Response.read(cmd, mailIn);
            if (!response.isContinuation()) break;
            processContinuation(response.getMessage());
        }
        return response;
    }

    private void writeUntraced(String s) throws IOException {
        if (traceOut != null && traceOut.isEnabled()) {
            traceOut.setEnabled(false);
            try {
                traceOut.getTraceStream().print("<password>");
                mailOut.write(s);
            } finally {
                traceOut.setEnabled(true);
            }
        } else {
            mailOut.write(s);
        }
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
