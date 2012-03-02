/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.Set;

import javax.security.auth.login.LoginException;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.auth.Authenticator;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.BuildInfo;

final class ImapProxy {
    private static final Set<String> UNSTRUCTURED_CODES = ImmutableSet.of("OK", "NO", "BAD", "PREAUTH", "BYE");
    private static final AuthenticatorFactory AUTH_FACTORY = new AuthenticatorFactory();
    static {
        AUTH_FACTORY.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
    }

    private final ImapHandler handler;
    private final ImapPath path;
    private ImapConnection connection;
    private Thread idleThread;

    ImapProxy(final ImapHandler handler, final ImapPath path) throws ServiceException {
        this.handler = handler;
        this.path = path;

        Account acct = handler.getCredentials().getAccount();
        Server server = Provisioning.getInstance().getServer(path.getOwnerAccount());
        String host = server.getServiceHostname();
        if (acct == null) {
            throw ServiceException.PROXY_ERROR(new Exception("no such authenticated user"), path.asImapPath());
        }
        ImapConfig config = new ImapConfig();
        config.setAuthenticationId(acct.getName());
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.setAuthenticatorFactory(AUTH_FACTORY);
        config.setReadTimeout(LC.javamail_imap_timeout.intValue());
        config.setConnectTimeout(config.getReadTimeout());
        config.setHost(host);
        if (server.isImapServerEnabled()) {
            config.setPort(server.getIntAttr(Provisioning.A_zimbraImapBindPort, ImapConfig.DEFAULT_PORT));
        } else if (server.isImapSSLServerEnabled()) {
            config.setPort(server.getIntAttr(Provisioning.A_zimbraImapSSLBindPort, ImapConfig.DEFAULT_SSL_PORT));
            config.setSecurity(MailConfig.Security.SSL);
        } else {
            throw ServiceException.PROXY_ERROR(new Exception("no open IMAP port for server " + host), path.asImapPath());
        }

        ZimbraLog.imap.info("opening proxy connection (user=%s, host=%s, path=%s)",
                acct.getName(), host, path.getReferent().asImapPath());
        connection = new ImapConnection(config);
        try {
            connection.connect();
            connection.id(createIDInfo(handler));
            connection.authenticate(AuthProvider.getAuthToken(acct).getEncoded());
        } catch (Exception e) {
            dropConnection();
            throw ServiceException.PROXY_ERROR(e, null);
        }
    }

    /**
     * For testing.
     */
    ImapProxy(InetSocketAddress remote, String username, String password, ImapHandler handler)
            throws IOException, LoginException {
        this.handler = handler;
        path = null;
        ImapConfig config = new ImapConfig();
        config.setAuthenticationId(username);
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.setAuthenticatorFactory(AUTH_FACTORY);
        config.setHost(remote.getHostName());
        config.setPort(remote.getPort());
        connection = new ImapConnection(config);
        connection.connect();
        connection.id(createIDInfo(handler));
        connection.authenticate(password);
    }

    private IDInfo createIDInfo(ImapHandler handler) {
        IDInfo id = new IDInfo();
        id.put(IDInfo.NAME, "ZCS");
        id.put(IDInfo.VERSION, BuildInfo.VERSION);
        id.put(IDInfo.X_VIA, handler.getNextVia());
        if (handler.getOrigRemoteIp() != null) {
            id.put(IDInfo.X_ORIGINATING_IP, handler.getOrigRemoteIp());
        }
        return id;
    }

    ImapPath getPath() {
        return path;
    }

    void dropConnection() {
        ImapConnection conn = connection;
        connection = null;
        if (conn == null)
            return;

        // FIXME: should close cleanly (i.e. with tagged LOGOUT)
        ZimbraLog.imap.info("closing proxy connection");
        conn.close();
    }


    /**
     * Performs a {@code SELECT} on the remote folder passed into the constructor. Writes all tagged and untagged
     * responses back to the handler's output stream.
     *
     * @return whether the SELECT was successful (i.e. it returned a tagged {@code OK} response)
     * @throws ImapProxyException network error with the remote IMAP server
     */
    boolean select(final String tag, final byte params, final QResyncInfo qri)
            throws ImapProxyException, ServiceException {
        // FIXME: may need to send an ENABLE before the SELECT

        String command = (params & ImapFolder.SELECT_READONLY) == 0 ? "SELECT" : "EXAMINE";
        StringBuilder select = new StringBuilder(100);
        select.append(tag).append(' ').append(command).append(' ');
        select.append(path.getReferent().asUtf7String());
        if ((params & ImapFolder.SELECT_CONDSTORE) != 0) {
            select.append(" (");
            if (qri == null) {
                select.append("CONDSTORE");
            } else {
                select.append("QRESYNC (").append(qri.uvv).append(' ').append(qri.modseq);
                if (qri.knownUIDs != null)
                    select.append(' ').append(qri.knownUIDs);
                if (qri.seqMilestones != null)
                    select.append(" (").append(qri.seqMilestones).append(' ').append(qri.uidMilestones).append(')');
                select.append(')');
            }
            select.append(')');
        }

        return proxyCommand(select.append("\r\n").toString().getBytes(), true, false);
    }

    /**
     * Proxy IDLE command.
     *
     * @param req IMAP request
     * @param begin true to start, false to stop
     * @return true to continue, false to close the connection
     * @throws ImapProxyException network error with the remote IMAP server
     * @throws IOException error on reading the request data
     */
    boolean idle(final ImapRequest req, final boolean begin) throws ImapProxyException, IOException {
        if (begin == ImapHandler.IDLE_STOP) {
            // check state -- don't want to send DONE if we're somehow not in IDLE
            if (handler == null) {
                throw new ImapProxyException("client connection already closed");
            }
            Thread idle = idleThread;
            if (idle == null) {
                throw new ImapProxyException("bad proxy state: no IDLE thread active when attempting DONE");
            }
            // send the DONE, which elicits the tagged response that causes the IDLE thread (below) to exit
            writeRequest(req.toByteArray());
            // make sure that the idle thread actually exits; otherwise we're in a bad place and we must kill the whole session
            idleThread = null;
            try {
                idle.join(5 * Constants.MILLIS_PER_SECOND);
            } catch (InterruptedException ie) { }
            if (idle.isAlive())
                handler.dropConnection(false);
        } else {
            final ImapConnection conn = connection;
            if (conn == null) {
                throw new ImapProxyException("proxy connection already closed");
            }
            ImapConfig config = conn.getImapConfig();
            final int oldTimeout = config != null ? config.getReadTimeout() : LC.javamail_imap_timeout.intValue();
            // necessary because of subsequent race condition with req.cleanup()
            final byte[] payload = req.toByteArray();

            idleThread = new Thread() {
                @Override
                public void run() {
                    boolean success = false;
                    try {
                        // the standard aggressive read timeout is inappropriate for IDLE
                        conn.setReadTimeout(handler.getConfig().getAuthenticatedMaxIdleTime());
                        // send the IDLE command; this call waits until the subsequent DONE is acknowledged
                        boolean ok = proxyCommand(payload, true, true);
                        // restore the old read timeout
                        conn.setReadTimeout(oldTimeout);
                        // don't set <code>success</code> until we're past things that can throw IOExceptions
                        success = ok;
                    } catch (IOException e) {
                        ZimbraLog.imap.warn("error encountered during IDLE; dropping connection", e);
                    }
                    if (!success) {
                        handler.dropConnection(true);
                    }
                }
            };
            idleThread.setName("Imap-Idle-Proxy-" + Thread.currentThread().getName());
            idleThread.start();
        }
        return true;
    }

    /**
     * Proxies the request to the remote server and writes all tagged and untagged responses back to the handler's
     * output stream.
     *
     * @param req IMAP request
     * @return always true
     * @throws ImapProxyException network error with the remote IMAP server
     * @throws IOException error on reading the request data
     */
    boolean proxy(final ImapRequest req) throws ImapProxyException, IOException {
        proxyCommand(req.toByteArray(), true, false);
        return true;
    }

    /**
     * Proxies the request to the remote server.
     *
     * @param tag IMAP command tag
     * @param command IMAP command
     * @return always true
     * @throws ImapProxyException network error with the remote IMAP server
     */
    boolean proxy(final String tag, final String command) throws ImapProxyException {
        proxyCommand((tag + ' ' + command + "\r\n").getBytes(), true, false);
        return true;
    }

    /**
     * Retrieves the set of notifications pending on the remote server.
     *
     * @throws ImapProxyException network error with the remote IMAP server
     */
    void fetchNotifications() throws ImapProxyException {
        String tag = connection == null ? "1" : connection.newTag();
        proxyCommand((tag + " NOOP\r\n").getBytes(), false, false);
    }

    private ImapConnection writeRequest(final byte[] payload) throws ImapProxyException {
        ImapConnection conn = connection;
        if (conn == null) {
            throw new ImapProxyException("proxy connection already closed");
        }
        // proxy the request over to the remote server
        OutputStream remote = conn.getOutputStream();
        if (remote == null) {
            dropConnection();
            throw new ImapProxyException("proxy connection already closed");
        }
        try {
            remote.write(payload);
            remote.flush();
        } catch (IOException e) {
            throw new ImapProxyException(e);
        }
        return conn;
    }

    private boolean proxyCommand(byte[] payload, boolean includeTaggedResponse, boolean isIdle)
            throws ImapProxyException {
        ImapConnection conn = writeRequest(payload);
        MailInputStream min = conn.getInputStream();
        OutputStream out = handler.output;
        if (out == null) {
            dropConnection();
            throw new ImapProxyException("client connection already closed");
        }

        // copy the response back to the handler's output (i.e. the original client)
        boolean success = false;
        int first;
        try {
            while ((first = min.peek()) != -1) {
                // XXX: may want to check that the "tagged" response's tag actually matches the request's tag...
                boolean tagged = first != '*' && first != '+';
                boolean structured = first == '*';
                boolean proxy = (first != '+' || isIdle) && (!tagged || includeTaggedResponse);

                ByteArrayOutputStream line = proxy ? new ByteArrayOutputStream() : null;
                StringBuilder debug = proxy && ZimbraLog.imap.isDebugEnabled() ? new StringBuilder("  pxy: ") : null;
                StringBuilder condition = new StringBuilder(10);

                boolean quoted = false, escaped = false, space1 = false, space2 = false;
                int c, literal = -1;
                while ((c = min.read()) != -1) {
                    // check for success and also determine whether we should be paying attention to structure
                    if (!space2) {
                        if (c == ' ' && !space1) {
                            space1 = true;
                        } else if (c == ' ') {
                            space2 = true;
                            String code = condition.toString().toUpperCase();
                            if ("BYE".equals(code)) { // unexpected BYE
                                dropConnection();
                                throw new ImapProxyException("proxy connection already closed");
                            }
                            if (tagged) {
                                success = "OK".equals(code) || (isIdle && "BAD".equals(code));
                            }
                            structured &= !UNSTRUCTURED_CODES.contains(code);
                        } else if (space1) {
                            condition.append((char) c);
                        }
                    }

                    // if it's a structured response, pay attention to quoting, literals, etc.
                    if (structured) {
                        if (escaped)
                            escaped = false;
                        else if (quoted && c == '\\')
                            escaped = true;
                        else if (c == '"')
                            quoted = !quoted;
                        else if (!quoted && c == '{')
                            literal = 0;
                        else if (literal != -1 && c >= '0' && c <= '9')
                            literal = literal * 10 + (c - '0');
                    }

                    if (!quoted && c == '\r' && min.peek() == '\n') {
                        // skip the terminal LF
                        min.read();
                        // write the line back to the client
                        if (proxy) {
                            out.write(line.toByteArray());
                            out.write(ImapHandler.LINE_SEPARATOR_BYTES);
                            line.reset();
                            if (isIdle)
                                out.flush();
                        }
                        // if it's end of line (i.e. no literal), we're done
                        if (literal == -1)
                            break;
                        // if there's a literal, copy it and then handle the following line
                        byte buffer[] = literal == 0 ? null : new byte[Math.min(literal, 65536)];
                        while (literal > 0) {
                            int read = min.read(buffer, 0, Math.min(literal, buffer.length));
                            if (read == -1)
                                break;
                            if (proxy)
                                out.write(buffer, 0, read);
                            literal -= read;
                        }
                        literal = -1;
                        if (isIdle)
                            out.flush();
                    } else if (proxy) {
                        line.write(c);
                        if (debug != null)
                            debug.append((char) c);
                    }
                }

                if (debug != null)
                    ZimbraLog.imap.debug(debug.toString());

                if (tagged)
                    break;
            }

            out.flush();
        } catch (ImapProxyException e) {
            throw e;
        } catch (IOException e) {
            throw new ImapProxyException(e);
        }
        return success;
    }


    public static final class ZimbraClientAuthenticator extends Authenticator {
        private String username, authtoken;
        private boolean complete;

        @Override public void init(MailConfig config, String password) {
            username = config.getAuthenticationId();  authtoken = password;
        }

        @Override public String getMechanism() { return ZimbraAuthenticator.MECHANISM; }
        @Override public boolean isComplete()  { return complete; }

        @Override public boolean hasInitialResponse()  { return true; }
        @Override public byte[] getInitialResponse()   { return evaluateChallenge(null); }

        @Override public byte[] evaluateChallenge(byte[] challenge) {
            complete = true;
            String response = username + '\0' + username + '\0' + authtoken;
            try {
                return response.getBytes("utf-8");
            } catch (UnsupportedEncodingException uee) {
                return response.getBytes();
            }
        }
    }
}
