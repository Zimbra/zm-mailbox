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
    protected String host;
    protected int port = -1;
    protected boolean sslEnabled;
    protected boolean tlsEnabled;
    protected String authorizationId;
    protected String authenticationId;
    protected String mechanism;
    protected String realm;
    protected Map<String, String> saslProperties;
    protected boolean debug;
    protected boolean trace;
    protected PrintStream traceOut;
    protected SSLSocketFactory sslSocketFactory;

    public MailConfig() {
        traceOut = System.out;
        authenticationId = System.getProperty("user.name");
    }

    public abstract String getProtocol();
                                                             
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setDebug(boolean debug) { this.debug = debug; }
    public void setTrace(boolean trace) { this.trace = trace; }
    public void setRealm(String realm) { this.realm = realm; }
    
    public boolean isTrace() { return trace; }
    public boolean isSslEnabled() { return sslEnabled; }
    public boolean isTlsEnabled() { return tlsEnabled; }
    public String getMechanism() { return mechanism; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    
    public void setMechanism(String mech) {
        mechanism = mech;
    }

    public void setTraceStream(PrintStream ps) {
        traceOut = ps;
    }

    public void setSslEnabled(boolean enabled) {
        sslEnabled = enabled;
    }

    public void setTlsEnabled(boolean enabled) {
        tlsEnabled = enabled;
    }
    
    public void setAuthorizationId(String id) {
        authorizationId = id;
    }

    public void setAuthenticationId(String id) {
        authenticationId = id;
    }

    public String getAuthenticationId() {
        return authenticationId;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setSaslProperty(String name, String value) {
        if (saslProperties == null) {
            saslProperties = new HashMap<String, String>();
        }
        saslProperties.put(name, value);
    }

    public void setSSLSocketFactory(SSLSocketFactory sf) {
        sslSocketFactory = sf;
    }

    // TODO Stuff below here should probably be moved to another class
    
    public ClientAuthenticator createAuthenticator() {
        if (authenticationId == null) {
            throw new IllegalStateException("Missing required authentication id");
        }
        ClientAuthenticator ca =
            new ClientAuthenticator(mechanism, getProtocol(), host);
        ca.setAuthorizationId(authorizationId);
        ca.setAuthenticationId(authenticationId);
        ca.setRealm(realm);
        ca.setDebug(debug);
        if (saslProperties != null) {
            ca.getProperties().putAll(saslProperties);
        }
        return ca;
    }

    public Socket createSocket() throws IOException {
        SocketFactory sf = isSslEnabled() ?
            getSSLSocketFactory() : SocketFactory.getDefault();
        return sf.createSocket(getHost(), getPort());
    }

    public SSLSocket createSSLSocket(Socket sock) throws IOException {
        return (SSLSocket) getSSLSocketFactory().createSocket(
            sock, sock.getInetAddress().getHostName(), sock.getPort(), false);
    }
        
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory != null ?
            sslSocketFactory : (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public TraceInputStream getTraceInputStream(InputStream is) {
        return new TraceInputStream(is, traceOut);
    }

    public TraceOutputStream getTraceOutputStream(OutputStream os) {
        return new TraceOutputStream(os, traceOut);
    }
}
