package com.zimbra.cs.mailclient;

import com.zimbra.cs.mailclient.util.TraceInputStream;
import com.zimbra.cs.mailclient.util.TraceOutputStream;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.SocketFactory;
import java.util.Map;
import java.util.HashMap;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Mail client configuration settings.
 */
public abstract class MailConfig {
    protected String mHost;
    protected int mPort = -1;
    protected boolean isSSLEnabled;
    protected String mAuthorizationId;
    protected String mAuthenticationId;
    protected String mPassword;
    protected String mMechanism;
    protected String mRealm;
    protected Map<String, String> mSaslProperties;
    protected boolean mDebug;
    protected boolean mTrace;
    protected PrintStream mTraceStream;
    protected SSLSocketFactory mSSLSocketFactory;

    public MailConfig() {
        mTraceStream = System.out;
        mAuthenticationId = System.getProperty("user.name");
    }

    public abstract String getProtocol();
                                                             
    public void setHost(String host) { mHost = host; }
    public void setPort(int port) { mPort = port; }
    public void setDebug(boolean enabled) { mDebug = enabled; }
    public void setTrace(boolean enabled) { mTrace = enabled; }
    public void setRealm(String realm) { mRealm = realm; }
    public boolean isTrace() { return mTrace; }
    public String getMechanism() { return mMechanism; }
    public boolean isSSLEnabled() { return isSSLEnabled; }
    public void setPassword(String pass) { mPassword = pass; }
    public String getPassword() { return mPassword; }

    public String getHost() { return mHost; }
    public int getPort() { return mPort; }
    
    public void setMechanism(String mech) {
        mMechanism = mech;
    }

    public void setTraceStream(PrintStream ps) {
        mTraceStream = ps;
    }

    public TraceInputStream getTraceInputStream(InputStream is) {
        return new TraceInputStream(is, mTraceStream);
    }

    public TraceOutputStream getTraceOutputStream(OutputStream os) {
        return new TraceOutputStream(os, mTraceStream);
    }
    
    public void setSSLEnabled(boolean enabled) {
        isSSLEnabled = enabled;
    }

    public void setAuthorizationId(String id) {
        mAuthorizationId = id;
    }

    public void setAuthenticationId(String id) {
        mAuthenticationId = id;
    }

    public String getAuthenticationId() {
        return mAuthenticationId;
    }

    public String getAuthorizationId() {
        return mAuthorizationId;
    }

    public void setSaslProperty(String name, String value) {
        if (mSaslProperties == null) {
            mSaslProperties = new HashMap<String, String>();
        }
        mSaslProperties.put(name, value);
    }

    public void setSSLSocketFactory(SSLSocketFactory sf) {
        mSSLSocketFactory = sf;
    }
    
    public ClientAuthenticator createAuthenticator() {
        if (mAuthenticationId == null) {
            throw new IllegalStateException("Missing required authentication id");
        }
        ClientAuthenticator ca =
            new ClientAuthenticator(mMechanism, getProtocol(), mHost);
        ca.setAuthorizationId(mAuthorizationId);
        ca.setAuthenticationId(mAuthenticationId);
        ca.setRealm(mRealm);
        ca.setDebug(mDebug);
        ca.setPassword(mPassword);
        if (mSaslProperties != null) {
            ca.getProperties().putAll(mSaslProperties);
        }
        return ca;
    }

    public Socket createSocket() throws IOException {
        SocketFactory sf = isSSLEnabled() ?
            getSSLSocketFactory() : SocketFactory.getDefault();
        return sf.createSocket(getHost(), getPort());
    }

    public SSLSocket createSSLSocket(Socket sock) throws IOException {
        return (SSLSocket) getSSLSocketFactory().createSocket(
            sock, sock.getInetAddress().getHostName(), sock.getPort(), false);
    }
        
    public SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory != null ?
            mSSLSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();
    }
}
