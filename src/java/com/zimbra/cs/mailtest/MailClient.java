package com.zimbra.cs.mailtest;

import org.apache.commons.codec.binary.Base64;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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
    protected boolean mSslEnabled;
    protected SSLSocketFactory mSSLSocketFactory;
    protected Map<String, String> mSaslProperties;
    protected MailInputStream mInputStream;
    protected MailOutputStream mOutputStream;
    protected PrintStream mLogPrintStream;
    protected ClientAuthenticator mAuthenticator;
    protected boolean mDebug;
    protected boolean mClosed;

    protected MailClient() {}

    public void connect() throws IOException {
        SocketFactory sf = mSslEnabled ?
            getSSLSocketFactory() : SocketFactory.getDefault();
        mSocket = sf.createSocket(mHost, mPort);
        mInputStream = new MailInputStream(
            new BufferedInputStream(mSocket.getInputStream()));
        mOutputStream = new MailOutputStream(
            new BufferedOutputStream(mSocket.getOutputStream()));
        if (!processGreeting()) {
            throw new IOException(
                "Expected greeting from server, but got: " + getMessage());
        }
    }

    private SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory != null ?
            mSSLSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();
    }
    
    protected abstract boolean processGreeting() throws IOException;
    protected abstract boolean sendAuthenticate() throws IOException;
    protected abstract boolean sendLogin() throws IOException;
    protected abstract boolean sendLogout() throws IOException;
    protected abstract boolean sendStartTLS() throws IOException;
    public abstract String getProtocol();
    public abstract String getMessage();

    public void login() throws LoginException, IOException {
        checkCredentials();
        if (!sendLogin()) {
            throw new LoginException(getMessage());
        }
    }

    public void logout() throws IOException {
        if (!sendLogout()) {
            throw new IOException("Logout failed");
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

    public void startTLS() throws IOException {
        if (!sendStartTLS()) {
            throw new IOException(getMessage());
        }
        SSLSocket sock = (SSLSocket) getSSLSocketFactory().createSocket(
            mSocket, mSocket.getInetAddress().getHostName(), mSocket.getPort(), false);
        try {
            sock.startHandshake();
            mInputStream = new MailInputStream(sock.getInputStream());
            mOutputStream = new MailOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            close();
            throw e;
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

    public void setSSLSocketFactory(SSLSocketFactory sf) {
        mSSLSocketFactory = sf;
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

    public void setSslEnabled(boolean enabled) {
        mSslEnabled = enabled;
    }

    public boolean isSSLEnabled() {
        return mSslEnabled;
    }

    public String getNegotiatedQop() {
        return mAuthenticator != null ?
            mAuthenticator.getNegotiatedProperty(Sasl.QOP) : null;
    }

    protected void sendLine(String line) throws IOException {
        ensureOpen();
        try {
            mOutputStream.writeLine(line);
            mOutputStream.flush();
        } catch (IOException e) {
            close(); // automatically close upon I/O error
            throw e;
        }
        if (mLogPrintStream != null) mLogPrintStream.println("C: " + line);
    }

    protected String recvLine() throws IOException {
        ensureOpen();
        try {
            String line = mInputStream.readLine();
            if (line == null) throw new EOFException();
            if (mLogPrintStream != null) mLogPrintStream.println("S: " + line);
            return line;
        } catch (IOException e) {
            close(); // automatically close upon I/O error
            throw e;
        }
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

    private void ensureOpen() throws IOException {
        if (isClosed()) throw new IOException("Connection closed");
    }
}
