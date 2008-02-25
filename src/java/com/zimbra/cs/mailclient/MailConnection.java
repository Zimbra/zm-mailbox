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
    protected MailConfig mConfig;
    protected Socket mSocket;
    protected SSLSocketFactory mSSLSocketFactory;
    protected ClientAuthenticator mAuthenticator;
    protected TraceInputStream mTraceInputStream;
    protected TraceOutputStream mTraceOutputStream;
    protected MailInputStream mInputStream;
    protected MailOutputStream mOutputStream;
    protected boolean mClosed;
    protected State mState;

    public static enum State {
        NON_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT
    }

    private static final String LOGIN = "LOGIN";
    
    protected MailConnection() {}

    protected MailConnection(MailConfig config) {
        mConfig = config;
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
        mSocket = mConfig.createSocket();
        initStreams(new BufferedInputStream(mSocket.getInputStream()),
                     new BufferedOutputStream(mSocket.getOutputStream()));
        processGreeting();
    }

    protected void initStreams(InputStream is, OutputStream os)
            throws IOException {
        if (mConfig.isTrace()) {
            is = mTraceInputStream = mConfig.getTraceInputStream(is);
            os = mTraceOutputStream = mConfig.getTraceOutputStream(os);
        }
        mInputStream = getMailInputStream(is);
        mOutputStream = getMailInputStream(os);
    }

    protected abstract void processGreeting() throws IOException;
    protected abstract void sendLogin() throws IOException;
    protected abstract void sendAuthenticate(boolean ir) throws IOException;
    protected abstract void sendStartTLS() throws IOException;
    protected abstract MailInputStream getMailInputStream(InputStream is);
    protected abstract MailOutputStream getMailInputStream(OutputStream os);

    public abstract void logout() throws IOException;

    public void login() throws IOException {
        String user = mConfig.getAuthenticationId();
        String pass = mConfig.getPassword();
        if (user == null || pass == null) {
            throw new IllegalStateException(
                "Missing required login username or password");
        }
        sendLogin();
    }
    
    public void authenticate(boolean ir) throws LoginException, IOException {
        String mech = mConfig.getMechanism();
        if (mech == null || mech.equalsIgnoreCase(LOGIN)) {
            login();
            return;
        }
        mAuthenticator = mConfig.createAuthenticator();
        mAuthenticator.initialize();
        sendAuthenticate(ir);
        if (mAuthenticator.isEncryptionEnabled()) {
            initStreams(mAuthenticator.getUnwrappedInputStream(mSocket.getInputStream()),
                        mAuthenticator.getWrappedOutputStream(mSocket.getOutputStream()));
        }
    }

    public void authenticate() throws LoginException, IOException {
        authenticate(false);
    }

    protected void processContinuation(String s) throws IOException {
        byte[] response = mAuthenticator.evaluateChallenge(decodeBase64(s));
        if (response != null) {
            mOutputStream.writeLine(encodeBase64(response));
            mOutputStream.flush();
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

    public void startTLS() throws IOException {
        sendStartTLS();
        SSLSocket sock = mConfig.createSSLSocket(mSocket);
        try {
            sock.startHandshake();
            mInputStream = new MailInputStream(sock.getInputStream());
            mOutputStream = new MailOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public String getNegotiatedQop() {
        return mAuthenticator != null ?
            mAuthenticator.getNegotiatedProperty(Sasl.QOP) : null;
    }

    public void setTraceEnabled(boolean enabled) {
        if (mTraceInputStream != null) {
            mTraceInputStream.setEnabled(enabled);
        }
        if (mTraceOutputStream != null) {
            mTraceOutputStream.setEnabled(enabled);
        }
    }

    public MailInputStream getInputStream() {
        return mInputStream;
    }

    public MailOutputStream getOutputStream() {
        return mOutputStream;
    }

    public MailConfig getConfig() {
        return mConfig;
    }
    
    public void close() {
        if (mClosed) return;
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mAuthenticator != null) {
            try {
                mAuthenticator.dispose();
            } catch (SaslException e) {
                e.printStackTrace();
            }
        }
        mClosed = true;
    }

    public boolean isClosed() {
        return mClosed;
    }
}
