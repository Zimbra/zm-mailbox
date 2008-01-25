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
    protected PrintStream mLogStream;
    protected StringBuilder mLogBuffer;
    protected ClientAuthenticator mAuthenticator;
    protected boolean mDebug;
    protected boolean mClosed;
    protected State mState;
    protected String mStatus;
    protected String mResponse;

    public static enum State {
        NON_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT
    }

    protected MailClient() {}

    protected MailClient(String host, int port) {
        mHost = host;
        mPort = port;
    }
    
    public void connect() throws IOException {
        SocketFactory sf = mSslEnabled ?
            getSSLSocketFactory() : SocketFactory.getDefault();
        mSocket = sf.createSocket(mHost, mPort);
        mInputStream = new MailInputStream(
            new BufferedInputStream(mSocket.getInputStream()));
        mOutputStream = new MailOutputStream(
            new BufferedOutputStream(mSocket.getOutputStream()));
        processGreeting();
    }

    private SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory != null ?
            mSSLSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();
    }
    
    protected abstract void processGreeting() throws IOException;
    protected abstract void sendAuthenticate(boolean ir) throws LoginException, IOException;
    protected abstract void sendStartTLS() throws IOException;

    public abstract void login() throws IOException;
    public abstract void logout() throws IOException;
    public abstract void sendCommand(String cmd, String args) throws IOException;
    public abstract String getProtocol();
    public abstract void selectFolder(String folder) throws IOException;

    public void sendCommand(String cmd) throws IOException {
        sendCommand(cmd, null);
    }
    
    public void authenticate(boolean ir) throws LoginException, IOException {
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
        sendAuthenticate(ir);
        if (mAuthenticator.isEncryptionEnabled()) {
            mInputStream = new MailInputStream(
                mAuthenticator.getUnwrappedInputStream(mSocket.getInputStream()));
            mOutputStream = new MailOutputStream(
                mAuthenticator.getWrappedOutputStream(mSocket.getOutputStream()));
        }
    }

    public void authenticate() throws LoginException, IOException {
        authenticate(false);
    }

    protected void processContinuation(String line) throws IOException {
        byte[] response = mAuthenticator.evaluateChallenge(
            decodeBase64(line.substring(2)));
        if (response != null) writeLine(encodeBase64(response));
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

    protected void checkCredentials() {
        if (mAuthenticationId == null) {
            mAuthenticationId = mAuthorizationId;
        }
        if (mAuthenticationId == null) {
            throw new IllegalStateException("Missing authentication id");
        }
    }

    public void startTLS() throws IOException {
        sendStartTLS();
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

    public String getResponse() {
        return mResponse;
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

    public void setLogStream(PrintStream ps) {
        mLogStream = ps;
        if (mLogBuffer == null) {
            mLogBuffer = new StringBuilder(132);
        }
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

    protected void write(String s) throws IOException {
        ensureOpen();
        try {
            mOutputStream.write(s);
        } catch (IOException e) {
            close(); // automatically close upon I/O error
            throw e;
        }
        if (mLogBuffer != null) {
            mLogBuffer.append(s);
        }
    }

    protected void write(byte[] b) throws IOException {
        ensureOpen();
        try {
            mOutputStream.write(b);
        } catch (IOException e) {
            close();
            throw e;
        }
        if (mLogBuffer != null) {
            mLogBuffer.append("<<...>>");
        }
    }
    
    protected void newLine() throws IOException {
        ensureOpen();
        try {
            mOutputStream.newLine();
            mOutputStream.flush();
        } catch (IOException e) {
            close();
            throw e;
        }
        if (mLogBuffer != null) {
            mLogStream.println("C: " + mLogBuffer.toString());
            mLogBuffer.setLength(0);
        }
    }
    
    protected void writeLine(String line) throws IOException {
        write(line);
        newLine();
    }

    protected String readLine() throws IOException {
        ensureOpen();
        try {
            String line = mInputStream.readLine();
            if (line == null) throw new EOFException();
            if (mLogStream != null) mLogStream.println("S: " + line);
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
