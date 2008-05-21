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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import javax.security.auth.login.LoginException;

public final class Pop3Connection extends MailConnection {
    private Pop3Capabilities capabilities;
    private int messageCount;

    private static final Logger LOGGER = Logger.getLogger(Pop3Connection.class);
    
    private static final String PASS = "PASS";
    private static final String USER = "USER";
    private static final String AUTH = "AUTH";
    private static final String STLR = "STLR";
    private static final String QUIT = "QUIT";
    private static final String CAPA = "CAPA";
    private static final String STAT = "STAT";
    private static final String LIST = "LIST";
    private static final String UIDL = "UIDL";
    private static final String DELE = "DELE";
    private static final String NOOP = "NOOP";
    private static final String RSET = "RSET";
    private static final String RETR = "RETR";

    public Pop3Connection(Pop3Config config) {
        super(config);
    }

    @Override
    protected MailInputStream newMailInputStream(InputStream is) {
        return new MailInputStream(is);
    }

    @Override
    protected MailOutputStream newMailOutputStream(OutputStream os) {
        return new MailOutputStream(os);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected void processGreeting() throws IOException {
        Pop3Response res = Pop3Response.read(null, mailIn);
        if (!res.isOK()) {
            throw new MailException(
                "Expected greeting, but got: " + res.getMessage());
        }
        setState(State.NOT_AUTHENTICATED);
        capabilities = capa();
    }

    @Override
    public void authenticate(String pass) throws LoginException, IOException {
        super.authenticate(pass);
        capabilities = capa();
        messageCount = stat();
    }
    
    @Override
    public void logout() throws IOException {
        quit();
    }

    @Override
    protected void sendLogin(String user, String pass) throws IOException {
        sendCommandCheckStatus(USER, user);
        sendCommandCheckStatus(PASS, pass);
    }

    @Override
    protected void sendAuthenticate(boolean ir) throws IOException {
        StringBuffer sb = new StringBuffer(config.getMechanism());
        if (ir) {
            byte[] response = authenticator.getInitialResponse();
            sb.append(' ').append(encodeBase64(response));
        }
        sendCommandCheckStatus(AUTH, sb.toString());
    }

    @Override
    protected boolean isTlsEnabled() {
        return super.isTlsEnabled() && hasCapability(Pop3Capabilities.STLS);
    }

    @Override
    protected void sendStartTls() throws IOException {
        sendCommandCheckStatus(STLR, null);
    }

    private Pop3Capabilities capa() throws IOException {
        Pop3Response res = sendCommandCheckStatus(CAPA, null);
        try {
            return Pop3Capabilities.read(res.getContentInputStream());
        } finally {
            res.dispose();
        }
    }

    private int stat() throws IOException {
        Pop3Response res = sendCommandCheckStatus(STAT, null);
        String[] parts = res.getMessage().split(" ");
        if (parts.length > 0) {
            try {
                return Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                // Fall through...
            }
        }
        throw new CommandFailedException(
            STAT, "Invalid STAT response: " + res.getMessage());
    }

    public int getMessageCount() {
        return messageCount;
    }
    
    public int getMessageSize(int msgno) throws IOException {
        Pop3Response res = sendCommand(LIST, msgno);
        if (res.isOK()) {
            String[] parts = res.getMessage().split(" ");
            if (parts.length > 1) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    // Fall through...
                }
            }
            throw new CommandFailedException(
                LIST, "Invalid LIST response: " + res.getMessage());
        }
        return -1;
    }

    public List<Integer> getMessageSizes() throws IOException {
        List<Integer> sizes = new ArrayList<Integer>(messageCount);
        Pop3Response res = sendCommandCheckStatus(LIST, null);
        try {
            ContentInputStream is = res.getContentInputStream();
            String line;
            while ((line = is.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length < 2) {
                    break;
                }
                try {
                    int msgno = Integer.parseInt(parts[0]);
                    int size = Integer.parseInt(parts[1]);
                    sizes.set(msgno - 1, size);
                } catch (Exception e) {
                    break;
                }
            }
            if (line != null) {
                throw new CommandFailedException(
                    LIST, "Invalid LIST response: " + line);
            }
            return sizes;
        } finally {
            res.dispose();
        }
    }

    public String getMessageUid(int msgno) throws IOException {
        Pop3Response res = sendCommand(UIDL, msgno);
        if (res.isOK()) {
            String[] parts = res.getMessage().split(" ");
            if (parts.length < 2) {
                throw new CommandFailedException(
                    UIDL, "Invalid UIDL response: " + res.getMessage());
            }
            return parts[1];
        }
        return null;
    }

    public List<String> getMessageUids() throws IOException {
        List<String> uids = new ArrayList<String>(messageCount);
        Pop3Response res = sendCommandCheckStatus(UIDL, null);
        try {
            ContentInputStream is = res.getContentInputStream();
            String line;
            while ((line = is.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length < 2) {
                    break;
                }
                try {
                    int msgno = Integer.parseInt(parts[0]);
                    String uid = parts[1];
                    uids.set(msgno - 1, uid);
                } catch (Exception e) {
                    break;
                }
            }
            if (line != null) {
                throw new CommandFailedException(
                    LIST, "Invalid UIDL response: " + line);
            }
            return uids;
        } finally {
            res.dispose();
        }
    }

    public ContentInputStream getMessage(int msgno) throws IOException {
        return sendCommandCheckStatus(RETR, msgno).getContentInputStream();
    }
    
    public boolean delete(int msgno) throws IOException {
        return sendCommand(DELE, msgno).isOK();
    }

    public void reset() throws IOException {
        sendCommandCheckStatus(RSET, null);
    }
    
    public void noop() throws IOException {
        sendCommandCheckStatus(NOOP, null);
    }

    public void quit() throws IOException {
        setState(State.LOGOUT);
        sendCommandCheckStatus(QUIT, null);
        setState(State.CLOSED);
    }
    
    public boolean hasCapability(String cap) {
        return capabilities != null && capabilities.hasCapability(cap);
    }
    
    public Pop3Response sendCommand(String cmd, Object args) throws IOException {
        mailOut.write(cmd);
        if (args != null) {
            mailOut.write(' ');
            if (cmd.equalsIgnoreCase(PASS)) {
                writePass(args.toString());
            } else {
                mailOut.write(args.toString());
            }
        }
        mailOut.newLine();
        mailOut.flush();
        while (true) {
            Pop3Response res = Pop3Response.read(cmd, mailIn);
            if (!res.isContinuation()) {
                return res;
            }
            processContinuation(res.getMessage());
        }
    }

    private void writePass(String pass) throws IOException {
        if (traceOut != null && traceOut.suspendTrace("<password>")) {
            try {
                mailOut.write(pass);
            } finally {
                traceOut.resumeTrace();
            }
        } else {
            mailOut.write(pass);
        }
    }

    public Pop3Response sendCommandCheckStatus(String cmd, Object args)
        throws IOException {
        Pop3Response res = sendCommand(cmd, args);
        if (!res.isOK()) {
            throw new CommandFailedException(cmd, res.getMessage());
        }
        return res;
    }
}
