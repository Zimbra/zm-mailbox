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
package com.zimbra.cs.mailclient;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.SocketFactory;
import javax.security.auth.login.LoginException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import com.zimbra.cs.mailclient.util.TraceInputStream;
import com.zimbra.cs.mailclient.util.TraceOutputStream;
import com.zimbra.cs.mailclient.util.Ascii;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.auth.Authenticator;

/**
 * Base class for all mail protocol connection types.
 */
public abstract class MailConnection {
    protected MailConfig config;
    protected Socket socket;
    protected Authenticator authenticator;
    protected TraceInputStream traceIn;
    protected TraceOutputStream traceOut;
    protected MailInputStream mailIn;
    protected MailOutputStream mailOut;
    protected State state = State.CLOSED;
    protected String greeting;

    /** Connection states */
    protected enum State {
        CLOSED, NOT_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT
    }
    
    /**
     * Creates a new <tt>MailConnection<tt> for the specified configuration.
     *
     * @param config the <tt>MailConfig</tt> for the connection
     */
    protected MailConnection(MailConfig config) {
        this.config = config;
        if (config.isDebug()) {
            getLogger().setLevel(Level.DEBUG);
        }
    }

    /**
     * Opens connection to the mail server. Does nothing if the connection
     * is already open.
     *
     * @throws IOException if an I/O error occurs
     */
    public synchronized void connect() throws IOException {
        if (!isClosed()) return;
        try {
            socket = newSocket();
            initStreams(new BufferedInputStream(socket.getInputStream()),
                        new BufferedOutputStream(socket.getOutputStream()));
            processGreeting();
            switch (config.getSecurity()) {
            case TLS:
                startTls();
                break;
            case TLS_IF_AVAILABLE:
                try {
                    startTls();
                } catch (CommandFailedException e) {
                    getLogger().debug("STARTTLS failed", e);
                }
                break;
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    protected void startTls() throws IOException {
        checkState(State.NOT_AUTHENTICATED);
        sendStartTls();
        SSLSocket sock = newSSLSocket(socket);
        sock.startHandshake();
        initStreams(sock.getInputStream(), sock.getOutputStream());
    }

    private void initStreams(InputStream is, OutputStream os)
        throws IOException {
        if (config.isTrace()) {
            is = traceIn = new TraceInputStream(is, config.getTraceOut());
            os = traceOut = new TraceOutputStream(os, config.getTraceOut());
        }
        mailIn = newMailInputStream(is);
        mailOut = newMailOutputStream(os);
    }

    /**
     * Processes greeting message from mail server.
     *
     * @throws IOException if an I/O error occurs
     */
    protected abstract void processGreeting() throws IOException;

    /**
     * Sends login information to server. This is used if SASL authentication
     * has not been specified in the mail configuration.
     *
     * @param user the login username
     * @param pass the login password
     * @throws CommandFailedException if the login command failed
     * @throws IOException if an I/O error occurs
     */
    protected abstract void sendLogin(String user, String pass) throws IOException;

    /**
     * Sends authentication information to server. This is used if SASL
     * authentication has been specified in the mail configuration for
     * the connection. The method {@link MailConfig#getMechanism()} returns
     * the SASL mechanism to use for authentication.
     *
     * @param ir if <tt>true</tt> then sends initial response
     * @throws CommandFailedException if the authentication command failed
     * @throws IOException if an I/O error occurs
     */
    protected abstract void sendAuthenticate(boolean ir) throws IOException;

    /**
     * Sends TLS start command to the server.
     *
     * @throws CommandFailedException if the start TLS command failed
     * @throws IOException if an I/O error occurs
     */
    protected abstract void sendStartTls() throws IOException;

    /**
     * Creates a new <tt>MailInputStream</tt> for the specified input stream.
     * This method should be overridden to return a <tt>MailInputStream</tt>
     * suitable for the specific protocol type.
     *
     * @param is the underlying input stream
     * @return the new <tt>MailInputStream</tt>
     */
    protected abstract MailInputStream newMailInputStream(InputStream is);

    /**
     * Creates a new <tt>MailOutputStream</tt> for the specified output stream.
     * This method should be overriden to return a <tt>MailOutputStream</tt>
     * suitable for the specific protocol type.
     *
     * @param os the underlying output stream
     * @return the new <tt>MailOutputStream</tt>
     */
    protected abstract MailOutputStream newMailOutputStream(OutputStream os);

    /**
     * Returns the <tt>Logger</tt> to use for logging mail client errors.
     * @return the <tt>Logger</tt> for mail client errors
     */
    public abstract Logger getLogger();

    /**
     * Logs out current user from server.
     *
     * @throws IOException if an I/O error occurs
     */
    public abstract void logout() throws IOException;

    /**
     * Logs in configured user using the specified password. The login user
     * is obtained by calling {@link MailConfig#getAuthenticationId()}.
     *
     * @param pass the login password
     * @throws CommandFailedException if the login command failed
     * @throws IOException if an I/O error occurs
     */
    public synchronized void login(String pass) throws IOException {
        if (pass == null) throw new NullPointerException("password");
        checkState(State.NOT_AUTHENTICATED);
        String user = config.getAuthenticationId();
        if (user == null) {
            throw new IllegalStateException("Authentication id missing");
        }
        sendLogin(user, pass);
        setState(State.AUTHENTICATED);
    }

    /**
     * Authenticates the user with the specified optional password. The
     * SASL authentication method to use is obtained by calling
     * {@link MailConfig#getMechanism()}. Various other configuration
     * properties are used to support authentication.
     *
     * @param pass the authentication password, or <tt>null</tt> if not required
     * @throws CommandFailedException if the authentication command failed
     * @throws LoginException if the login failed due to a SASL authenticator error
     * @throws IOException if an I/O error occurs
     */
    public synchronized void authenticate(String pass)
        throws LoginException, IOException {
        String mech = config.getMechanism();
        if (mech == null || mech.equalsIgnoreCase("LOGIN")) {
            login(pass);
        } else {
            authenticate(newAuthenticator(pass));
        }
    }

    public synchronized void authenticate(Authenticator auth)
        throws LoginException, IOException {
        authenticator = auth;
        checkState(State.NOT_AUTHENTICATED);
        sendAuthenticate(false);
        if (authenticator.isEncryptionEnabled()) {
            initStreams(authenticator.unwrap(socket.getInputStream()),
                        authenticator.wrap(socket.getOutputStream()));
        }
        setState(State.AUTHENTICATED);
    }

    private Authenticator newAuthenticator(String pass)
        throws LoginException, SaslException {
        AuthenticatorFactory af = config.getAuthenticatorFactory();
        if (af == null) {
            af = AuthenticatorFactory.getDefault();
        }
        return af.newAuthenticator(config, pass);
    }

    /**
     * Returns the greeting message returned by the server.
     *
     * @return the server greeting, or <tt>null</tt> if unknown
     */
    public String getGreeting() {
        return greeting;
    }

    /**
     * Processes an authentication continuation request from the server.
     * In response, this may write another continuation response to the
     * server.
     * 
     * @param s the continuation request to be processed
     * @throws IOException if an I/O error occurs
     */
    protected void processContinuation(String s) throws IOException {
        byte[] decoded = Base64.decodeBase64(Ascii.getBytes(s));
        byte[] request = authenticator.evaluateChallenge(decoded);
        String data = Ascii.toString(Base64.encodeBase64(request));
        if (traceOut != null && traceOut.suspendTrace("<authentication data>\n")) {
            try {
                mailOut.writeLine(data);
            } finally {
                traceOut.resumeTrace();
            }
        } else {
            mailOut.writeLine(data);
        }
        mailOut.flush();
    }

    /**
     * If SASL authentication was used, then returns the negotiated quality
     * of protection for the connection.
     *
     * @return the SASL quality of protection, or <tt>null</tt> if not yet
     *         authenticated or SASL authentication was not used
     */
    public String getNegotiatedQop() {
        return authenticator != null ?
            authenticator.getNegotiatedProperty(Sasl.QOP) : null;
    }

    /**
     * Optionally enables protocol tracing for the connection.
     *
     * @param enabled tracing enabled if <tt>true</tt>, disabled if <tt>false</tt>
     */
    public void setTraceEnabled(boolean enabled) {
        if (traceIn != null) {
            traceIn.setEnabled(enabled);
        }
        if (traceOut != null) {
            traceOut.setEnabled(enabled);
        }
    }

    /**
     * Returns the input stream for reading mail data.
     *
     * @return the connection input stream
     */
    public MailInputStream getInputStream() {
        return mailIn;
    }

    /**
     * Returns the output stream for writing mail data.
     * 
     * @return the connection output stream
     */
    public MailOutputStream getOutputStream() {
        return mailOut;
    }

    /**
     * Returns the configuration for the connection.
     *
     * @return the mail configuration
     */
    public MailConfig getConfig() {
        return config;
    }

    /**
     * Sets the read timeout for the connection.
     *
     * @param readTimeout  The new read timeout, in seconds.
     *                     <tt>0</tt> means no timeout.
     * @throws SocketException if a socket I/O error occurs
     */
    public void setReadTimeout(final int readTimeout) throws SocketException {
        int timeout = (int) Math.min(readTimeout * 1000L, Integer.MAX_VALUE);
        if (socket != null && !isClosed())
            socket.setSoTimeout(timeout > 0 ? timeout : Integer.MAX_VALUE);
    }

    /**
     * Returns <tt>true</tt> if the connection is closed.
     *
     * @return <tt>true</tt> if connection closed, <tt>false</tt> if not
     */
    public synchronized boolean isClosed() {
        return state == State.CLOSED;
    }

    /**
     * Returns <tt>true</tt> if the connection has been authenticated.
     *
     * @return <tt>true</tt> if connection authenticated, <tt>false<tt> if not
     */
    public synchronized boolean isAuthenticated() {
        return state == State.AUTHENTICATED;
    }

    /**
     * Returns <tt>true</tt> if connection logout is in progress.
     * @return <tt>true</tt> if logout in progress, <tt>false</tt> if not
     */
    public synchronized boolean isLogout() {
        return state == State.LOGOUT;
    }

    /**
     * Sets the new connection state.
     *
     * @param state the new connection <tt>State</tt>
     */
    protected synchronized void setState(State state) {
        if (this.state != state) {
            getLogger().debug("setState: " + this.state + " -> " + state);
            this.state = state;
        }
    }

    /**
     * Compares current connection state with expected state. If the states
     * are not the same then throws <tt>IllegalStateException</tt>.
     *
     * @param expected the <tt>State</tt> that is expected
     * @throws IllegalStateException if the current and expected states differ
     */
    protected void checkState(State expected) {
        if (state != expected) {
            throw new IllegalStateException(
                "Operation not supported in " + state + " state");
        }
    }

    /**
     * Closes the current connection and cleans up any associated resources.
     * The connections state is set to <tt>CLOSED</tt>.
     */
    public synchronized void close() {
        if (isClosed()) return;
        setState(State.CLOSED);
        try {
            mailIn.close();
        } catch (IOException e) {
            // Ignore
        }
        try {
            mailOut.close();
        } catch (IOException e) {
            // Ignore
        }
        try {
            socket.close();
        } catch (IOException e) {
            getLogger().info("Error while closing connection", e);
        }
        if (authenticator != null) {
            try {
                authenticator.dispose();
            } catch (SaslException e) {
                getLogger().debug("Error while deleting authenticator", e);
            }
        }
    }

    private Socket newSocket() throws IOException {
        SocketFactory sf = config.getSecurity() != MailConfig.Security.SSL ?
                getSocketFactory() : getSSLSocketFactory();
        Socket sock = sf.createSocket();
        int connectTimeout = (int)
            Math.min(config.getConnectTimeout() * 1000L, Integer.MAX_VALUE);
        sock.connect(new InetSocketAddress(config.getHost(), config.getPort()),
            connectTimeout > 0 ? connectTimeout : Integer.MAX_VALUE);
        int readTimeout = (int)
            Math.min(config.getReadTimeout() * 1000L, Integer.MAX_VALUE);
        sock.setSoTimeout(readTimeout > 0 ? readTimeout : Integer.MAX_VALUE);
        return sock;
    }

    private SSLSocket newSSLSocket(Socket sock) throws IOException {
        return (SSLSocket) getSSLSocketFactory().createSocket(
            sock, sock.getInetAddress().getHostName(), sock.getPort(), false);
    }

    private SocketFactory getSocketFactory() {
        SocketFactory sf = config.getSocketFactory();
        return sf != null ? sf : SocketFactory.getDefault();
    }
    
    private SSLSocketFactory getSSLSocketFactory() {
        SSLSocketFactory ssf = config.getSSLSocketFactory();
        return ssf != null ? ssf : (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    @Override
    public String toString() {
        return String.format("{host=%s,port=%d,type=%s,state=%s}",
            config.getHost(), config.getPort(), config.getSecurity(), state);
    }
}
