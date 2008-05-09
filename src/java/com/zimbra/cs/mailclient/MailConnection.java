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
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;

import com.zimbra.cs.mailclient.util.TraceInputStream;
import com.zimbra.cs.mailclient.util.TraceOutputStream;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.auth.Authenticator;

public abstract class MailConnection {
    protected MailConfig config;
    protected Socket socket;
    protected Authenticator authenticator;
    protected TraceInputStream traceIn;
    protected TraceOutputStream traceOut;
    protected MailInputStream mailIn;
    protected MailOutputStream mailOut;
    protected State state = State.CLOSED;

    protected enum State {
        CLOSED, NOT_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT
    }
    private static final String LOGIN = "LOGIN";

    protected MailConnection() {}

    protected MailConnection(MailConfig config) {
        this.config = config;
        if (config.isDebug()) {
            getLogger().setLevel(Level.DEBUG);
        }
    }

    public synchronized void connect() throws IOException {
        if (!isClosed()) return;
        socket = newSocket();
        initStreams(new BufferedInputStream(socket.getInputStream()),
                    new BufferedOutputStream(socket.getOutputStream()));
        processGreeting();
        if (config.isTlsEnabled() && !config.isSslEnabled()) {
            startTls();
        }
    }

    protected void initStreams(InputStream is, OutputStream os)
        throws IOException {
        if (config.isTrace()) {
            is = traceIn = newTraceInputStream(is);
            os = traceOut = newTraceOutputStream(os);
        }
        mailIn = newMailInputStream(is);
        mailOut = newMailOutputStream(os);
    }

    protected abstract void processGreeting() throws IOException;
    protected abstract void sendLogin(String user, String pass) throws IOException;
    protected abstract void sendAuthenticate(boolean ir) throws IOException;
    protected abstract void sendStartTls() throws IOException;
    protected abstract MailInputStream newMailInputStream(InputStream is);
    protected abstract MailOutputStream newMailOutputStream(OutputStream os);
    public abstract Logger getLogger();

    public abstract void logout() throws IOException;

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
    
    public synchronized void authenticate(String pass)
        throws LoginException, IOException {
        checkState(State.NOT_AUTHENTICATED);
        String mech = config.getMechanism();
        if (mech == null || mech.equalsIgnoreCase(LOGIN)) {
            login(pass);
            return;
        }                  
        authenticator = newAuthenticator(pass);
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
    
    protected void processContinuation(String s) throws IOException {
        byte[] response = authenticator.evaluateChallenge(decodeBase64(s));
        if (response != null) {
            mailOut.writeLine(encodeBase64(response));
            mailOut.flush();
        }
    }

    protected static byte[] decodeBase64(String s) throws SaslException {
        try {
            return Base64.decodeBase64(s.getBytes("us-ascii"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("US-ASCII encoding unsupported");
        }
    }

    protected static String encodeBase64(byte[] b) {
        try {
            return new String(Base64.encodeBase64(b), "us-ascii");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("US-ASCII encoding unsupported");
        }
    }

    private void startTls() throws IOException {
        checkState(State.NOT_AUTHENTICATED);
        sendStartTls();
        SSLSocket sock = newSSLSocket(socket);
        try {
            sock.startHandshake();
            initStreams(sock.getInputStream(), sock.getOutputStream());
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public String getNegotiatedQop() {
        return authenticator != null ?
            authenticator.getNegotiatedProperty(Sasl.QOP) : null;
    }

    public void setTraceEnabled(boolean enabled) {
        if (traceIn != null) {
            traceIn.setEnabled(enabled);
        }
        if (traceOut != null) {
            traceOut.setEnabled(enabled);
        }
    }

    public MailInputStream getInputStream() {
        return mailIn;
    }

    public MailOutputStream getOutputStream() {
        return mailOut;
    }

    public MailConfig getConfig() {
        return config;
    }

    public synchronized boolean isClosed() {
        return state == State.CLOSED;
    }

    public synchronized boolean isAuthenticated() {
        return state == State.AUTHENTICATED;
    }

    public synchronized boolean isLogout() {
        return state == State.LOGOUT;
    }
    
    protected synchronized void setState(State state) {
        getLogger().debug("setState: " + this.state + " -> " + state);
        this.state = state;
        notifyAll();
    }

    protected void checkState(State expected) {
        if (state != expected) {
            throw new IllegalStateException(
                "Operation not supported in " + state + " state");
        }
    }
    
    public synchronized void close() {
        if (isClosed()) return;
        setState(State.CLOSED);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (authenticator != null) {
            try {
                authenticator.dispose();
            } catch (SaslException e) {
                e.printStackTrace();
            }
        }
    }

    private Socket newSocket() throws IOException {
        SocketFactory sf = config.isSslEnabled() ?
            getSSLSocketFactory() : SocketFactory.getDefault();
        return sf.createSocket(config.getHost(), config.getPort());
    }

    private SSLSocket newSSLSocket(Socket sock) throws IOException {
        return (SSLSocket) getSSLSocketFactory().createSocket(
            sock, sock.getInetAddress().getHostName(), sock.getPort(), false);
    }

    private SSLSocketFactory getSSLSocketFactory() {
        SSLSocketFactory ssf = config.getSSLSocketFactory();
        return ssf != null ? ssf : (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private TraceInputStream newTraceInputStream(InputStream is) {
        return new TraceInputStream(is, config.getTraceOut());
    }

    private TraceOutputStream newTraceOutputStream(OutputStream os) {
        return new TraceOutputStream(os, config.getTraceOut());
    }
}
