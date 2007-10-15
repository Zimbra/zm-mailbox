package com.zimbra.cs.mailtest;

import org.apache.commons.codec.binary.Base64;

import javax.security.auth.login.LoginException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public abstract class MailClient {
    protected String mHost;
    protected int mPort;
    protected Socket mSocket;
    protected String mAuthorizationId;
    protected String mAuthenticationId;
    protected String mPassword;
    protected String mMechanism;
    protected String mRealm;
    protected Map<String, String> mSaslProperties;
    protected MailInputStream mInputStream;
    protected MailOutputStream mOutputStream;
    protected PrintStream mLogPrintStream;
    protected ClientAuthenticator mAuthenticator;
    protected boolean mDebug;

    protected MailClient(String host, int port) {
        mHost = host;
        mPort = port;
    }
    
    protected MailClient() {}

    public void connect() throws IOException {
        if (mHost == null || mPort == 0) {
            throw new IllegalStateException("Missing required host or port");
        }
        mSocket = new Socket(mHost, mPort);
        mInputStream = new MailInputStream(
            new BufferedInputStream(mSocket.getInputStream()));
        mOutputStream = new MailOutputStream(
            new BufferedOutputStream(mSocket.getOutputStream()));
        if (!processGreeting()) {
            throw new IOException(
                "Expected greeting from server, but got: " + getMessage());
        }
    }

    protected abstract boolean processGreeting() throws IOException;
    protected abstract boolean sendAuthenticate() throws IOException;
    protected abstract boolean sendLogin() throws IOException;
    public abstract String getProtocol();
    public abstract String getMessage();

    public void login() throws LoginException, IOException {
        checkCredentials();
        if (!sendLogin()) {
            throw new LoginException(getMessage());
        }
    }

    public void authenticate() throws LoginException, IOException {
        if (mMechanism == null || "LOGIN".equals(mMechanism)) {
            login();
            return;
        }
        checkCredentials();
        mAuthenticator = new ClientAuthenticator(mMechanism, getProtocol(), mHost);
        mAuthenticator.setAuthorizationId(mAuthorizationId);
        mAuthenticator.setAuthenticationId(mAuthenticationId);
        mAuthenticator.setPassword(mPassword);
        mAuthenticator.setRealm(mRealm);
        mAuthenticator.setDebug(mDebug);
        if (mSaslProperties != null) {
            mAuthenticator.getProperties().putAll(mSaslProperties);
        }
        mAuthenticator.initialize();
        if (!sendAuthenticate()) {
            throw new LoginException(getMessage());
        }
        if (mAuthenticator.isEncryptionEnabled()) {
            mInputStream = new MailInputStream(
                mAuthenticator.getUnwrappedInputStream(mSocket.getInputStream()));
            mOutputStream = new MailOutputStream(
                mAuthenticator.getWrappedOutputStream(mSocket.getOutputStream()));
        }
    }

    protected void processContinuation(String line) throws IOException {
        byte[] response = mAuthenticator.evaluateChallenge(
            decodeBase64(line.substring(2)));
        if (response != null) sendLine(encodeBase64(response));
    }

    private static byte[] decodeBase64(String s) throws SaslException {
        try {
            return Base64.decodeBase64(s.getBytes("us-ascii"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("US-ASCII encoding unsupported");
        }
    }

    private static String encodeBase64(byte[] b) {
        try {
            return new String(Base64.encodeBase64(b), "us-ascii");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("US-ASCII encoding unsupported");
        }
    }

    private void checkCredentials() {
        if (mAuthenticationId == null) {
            throw new IllegalStateException("Missing authentication id");
        }
    }

    public MailInputStream getInputStream() {
        return mInputStream;
    }

    public MailOutputStream getOutputStream() {
        return mOutputStream;
    }

    public void setHost(String host) {
        mHost = host;
    }

    public void setPort(int port) {
        mPort = port;
    }

    public void setAuthorizationId(String id) {
        mAuthorizationId = id;
    }

    public void setAuthenticationId(String id) {
        mAuthenticationId = id;
    }

    public void setSaslProperty(String name, String value) {
        if (mSaslProperties == null) {
            mSaslProperties = new HashMap<String, String>();
        }
        mSaslProperties.put(name, value);
    }
    
    public void setPassword(String password) {
        mPassword = password;
    }

    public void setRealm(String realm) {
        mRealm = realm;
    }

    public void setMechanism(String mechanism) {
        mMechanism = mechanism;
    }

    public void setLogPrintStream(PrintStream ps) {
        mLogPrintStream = ps;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
    }

    public String getNegotiatedQop() {
        return mAuthenticator != null ?
            mAuthenticator.getNegotiatedProperty(Sasl.QOP) : null;
    }
    
    protected void sendLine(String line) throws IOException {
        if (mLogPrintStream != null) mLogPrintStream.println("C: " + line);
        mOutputStream.writeLine(line);
        mOutputStream.flush();
    }
    
    protected String recvLine() throws IOException {
        String line = mInputStream.readLine();
        if (mLogPrintStream != null) mLogPrintStream.println("S: " + line);
        if (line == null) throw new EOFException();
        return line;
    }

    public void dispose() {
        if (mAuthenticator != null) {
            try {
                mAuthenticator.dispose();
            } catch (SaslException e) {
                e.printStackTrace();
            }
            mAuthenticator = null;
        }
    }
}
