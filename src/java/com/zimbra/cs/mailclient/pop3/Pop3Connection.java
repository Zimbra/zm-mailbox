/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.pop3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.security.auth.login.LoginException;

import com.zimbra.common.util.ZimbraLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.MailOutputStream;
import com.zimbra.cs.mailclient.util.Ascii;

public final class Pop3Connection extends MailConnection {
    private Pop3Capabilities capabilities;
    private int messageCount;
    private long maildropSize;

    private static final Logger LOGGER = Logger.getLogger(Pop3Connection.class);
    
    private static final String PASS = "PASS";
    private static final String USER = "USER";
    private static final String AUTH = "AUTH";
    private static final String STLS = "STLS";
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
        greeting = res.getMessage();
        if (!res.isOK()) {
            throw new MailException(
                "Expected greeting, but got: " + res.getMessage());
        }
        setState(State.NOT_AUTHENTICATED);
        capabilities = capa();
    }

    @Override
    public void login(String pass) throws IOException {
        super.login(pass);
        capabilities = capa();
        stat();
    }
    
    @Override
    public void authenticate(String pass) throws LoginException, IOException {
        super.authenticate(pass);
        capabilities = capa();
        stat();
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
        StringBuffer sb = new StringBuffer(authenticator.getMechanism());
        if (ir) {
            byte[] response = authenticator.getInitialResponse();
            sb.append(' ');
            sb.append(Ascii.toString(Base64.encodeBase64(response)));
        }
        sendCommandCheckStatus(AUTH, sb.toString());
    }

    @Override
    protected void sendStartTls() throws IOException {
        sendCommandCheckStatus(STLS, null);
    }

    private Pop3Capabilities capa() throws IOException {
        Pop3Response res = sendCommand(CAPA, null);
        if (!res.isOK()) {
            return null; // RFC 2449 not supported
        }
        try {
            return Pop3Capabilities.read(res.getContentInputStream());
        } finally {
            res.dispose();
        }
    }

    private void stat() throws IOException {
        Pop3Response res = sendCommandCheckStatus(STAT, null);
        String[] parts = split(res.getMessage());
        if (parts.length > 1) {
            try {
                messageCount = Integer.parseInt(parts[0]);
                maildropSize = Long.parseLong(parts[1]);
                return;
            } catch (NumberFormatException e) {
                ZimbraLog.pop.error("Invalid STAT response: " + res.getMessage(), e);
                // Fall through...
            }
        }
        throw new CommandFailedException(
            STAT, "Invalid STAT response: " + res.getMessage());
    }

    public Pop3Capabilities getCapabilities() {
        return capabilities;
    }
    
    public int getMessageCount() {
        return messageCount;
    }

    public long getMaildropSize() {
        return maildropSize;
    }
    
    public int getMessageSize(int msgno) throws IOException {
        Pop3Response res = sendCommand(LIST, msgno);
        if (res.isOK()) {
            String[] parts = split(res.getMessage());
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

    public Integer[] getMessageSizes() throws IOException {
        Integer[] sizes = new Integer[messageCount];
        Pop3Response res = sendCommandCheckStatus(LIST, null);
        try {
            ContentInputStream is = res.getContentInputStream();
            String line;
            while ((line = is.readLine()) != null) {
                String[] parts = split(line);
                if (parts.length < 2) {
                    break;
                }
                try {
                    int msgno = Integer.parseInt(parts[0]);
                    int size = Integer.parseInt(parts[1]);
                    sizes[msgno - 1] = size;
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
            String[] parts = split(res.getMessage());
            if (parts.length < 2) {
                throw new CommandFailedException(
                    UIDL, "Invalid UIDL response: " + res.getMessage());
            }
            return parts[1];
        }
        return null;
    }

    public String[] getMessageUids() throws IOException {
        String[] uids = new String[messageCount];
        Pop3Response res = sendCommandCheckStatus(UIDL, null);
        try {
            ContentInputStream is = res.getContentInputStream();
            String line;
            while ((line = is.readLine()) != null) {
                String[] parts = split(line);
                if (parts.length < 2) {
                    break;
                }
                try {
                    int msgno = Integer.parseInt(parts[0]);
                    String uid = parts[1];
                    uids[msgno - 1] = uid;
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
    
    public boolean deleteMessage(int msgno) throws IOException {
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

    public Pop3Config getPop3Config() {
        return (Pop3Config) config;
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

    private static String[] split(String s) {
        return s.trim().split("[ \\t]+");
    }
}
