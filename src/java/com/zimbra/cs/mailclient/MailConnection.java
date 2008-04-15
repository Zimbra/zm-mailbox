package com.zimbra.cs.mailclient;

import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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

import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.pop3.Pop3Config;
import com.zimbra.cs.mailclient.pop3.Pop3Connection;
import com.zimbra.cs.mailclient.util.TraceInputStream;
import com.zimbra.cs.mailclient.util.TraceOutputStream;

public abstract class MailConnection {
    protected MailConfig config;
    protected Socket socket;
    protected SSLSocketFactory sslSocketFactory;
    protected ClientAuthenticator authenticator;
    protected TraceInputStream traceIn;
    protected TraceOutputStream traceOut;
    protected MailInputStream mailIn;
    protected MailOutputStream mailOut;
    protected State state;

    public static enum State {
        NON_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT
    }

    private static final String LOGIN = "LOGIN";
    
    protected MailConnection() {}

    protected MailConnection(MailConfig config) {
        this.config = config;
    }

    public static MailConnection getInstance(MailConfig config) {
        if (config instanceof ImapConfig) {
            return new ImapConnection((ImapConfig) config);
        } else if (config instanceof Pop3Config) {
            return new Pop3Connection((Pop3Config) config);
        } else {
            throw new IllegalArgumentException(
                "Unsupported protocol: " + config.getProtocol());
        }
    }

    public void connect() throws IOException {
        if (isConnected()) return;
        socket = config.createSocket();
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
            is = traceIn = config.getTraceInputStream(is);
            os = traceOut = config.getTraceOutputStream(os);
        }
        mailIn = getMailInputStream(is);
        mailOut = getMailInputStream(os);
    }

    protected abstract void processGreeting() throws IOException;
    protected abstract void sendLogin(String user, String pass) throws IOException;
    protected abstract void sendAuthenticate(boolean ir) throws IOException;
    protected abstract void sendStartTls() throws IOException;
    protected abstract MailInputStream getMailInputStream(InputStream is);
    protected abstract MailOutputStream getMailInputStream(OutputStream os);

    public abstract void logout() throws IOException;

    public void login(String pass) throws IOException {
        if (pass == null) throw new NullPointerException("password");
        String user = config.getAuthenticationId();
        if (user == null) {
            throw new IllegalStateException("Authentication id missing");
        }
        sendLogin(user, pass);
    }
    
    public void authenticate(String pass) throws LoginException, IOException {
        String mech = config.getMechanism();
        if (mech == null || mech.equalsIgnoreCase(LOGIN)) {
            login(pass);
            return;
        }                  
        authenticator = config.createAuthenticator();
        authenticator.setPassword(pass);
        authenticator.initialize();
        sendAuthenticate(false);
        if (authenticator.isEncryptionEnabled()) {
            initStreams(authenticator.getUnwrappedInputStream(socket.getInputStream()),
                        authenticator.getWrappedOutputStream(socket.getOutputStream()));
        }
    }

    protected void processContinuation(String s) throws IOException {
        byte[] response = authenticator.evaluateChallenge(decodeBase64(s));
        if (response != null) {
            mailOut.writeLine(encodeBase64(response));
            mailOut.flush();
        }
    }

    private static byte[] decodeBase64(String s) throws SaslException {
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

    public void startTls() throws IOException {
        sendStartTls();
        SSLSocket sock = config.createSSLSocket(socket);
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

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }
    
    public void close() {
        if (!isConnected()) return;
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
        socket = null;
        authenticator = null;
    }
}
