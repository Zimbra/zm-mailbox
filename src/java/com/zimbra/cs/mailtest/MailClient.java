package com.zimbra.cs.mailtest;

import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public abstract class MailClient {
    protected String mHost;
    protected int mPort;
    protected Socket mSocket;
    protected String mAuthorizationId;
    protected String mAuthenticationId;
    protected String mPassword;
    protected String mMechanism;
    protected String mRealm;
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
        mAuthenticator.initialize();
        if (!sendAuthenticate()) {
            throw new LoginException(getMessage());
        }
    }

    protected void processContinuation(String line) throws IOException {
        if (!mAuthenticator.evaluateChallengeBase64(line.substring(2))) {
            sendContinuation();
        }
    }
    
    private void sendContinuation() throws IOException {
        sendLine(mAuthenticator.getResponseBase64());
    }

    private void checkCredentials() {
        if (mAuthenticationId == null) {
            throw new IllegalStateException("Missing authentication id");
        }
        /*
        if (mPassword == null) {
            throw new IllegalStateException("Missing password");
        }
        */
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
