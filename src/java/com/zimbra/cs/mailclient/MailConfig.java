/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient;

import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.util.Config;

import javax.net.ssl.SSLSocketFactory;
import java.util.Map;
import java.util.Properties;
import java.io.PrintStream;
import java.io.File;
import java.io.IOException;

/**
 * Represents configuration common to all mail client protocols.
 */
public abstract class MailConfig {
    private String host;
    private int port = -1;
    private boolean sslEnabled;
    private boolean tlsEnabled;
    private String authorizationId;
    private String authenticationId;
    private String mechanism;
    private String realm;
    private Map<String, String> saslProperties;
    private boolean debug;
    private boolean trace;
    private PrintStream traceOut;
    private SSLSocketFactory sslSocketFactory;
    private AuthenticatorFactory authenticatorFactory;
    private int timeout;
    private boolean synchronousMode;

    /**
     * Creates a new <tt>MailConfig</tt> instance.
     */
    protected MailConfig() {
        traceOut = System.out;
        authenticationId = System.getProperty("user.name");
    }

    /**
     * Creates a new <tt>MailConfig</tt> instance for the specified mail
     * server host.
     *
     * @param host the mail server host
     */
    protected MailConfig(String host) {
        this.host = host;
    }

    /**
     * Returns the name of the mail protocol for this configuration.
     *
     * @return the protocol name
     */
    public abstract String getProtocol();

    /**
     * Returns the mail server host name.
     *
     * @return the mail server host name, or <tt>null</tt> if not set
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the mail server host name.
     *
     * @param host the mail server host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the mail server port number
     *
     * @return the server port, or <tt>-1</tt> if not set
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the mail server port number.
     *
     * @param port the mail server port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns true if SSL is enabled for the connection. The default is
     * for SSL to be disabled.
     *
     * @return true if SSL enabled, false if disabled
     */
    public boolean isSslEnabled() {
        return sslEnabled;
    }

    /**
     * Enables or disables SSL for the connection.
     *
     * @param enabled if <tt>true</tt> then enable SSL
     */
    public void setSslEnabled(boolean enabled) {
        sslEnabled = enabled;
    }

    /**
     * Enables or disables TLS for the connection.
     *
     * @param enabled if <tt>true</tt> then enable TLS
     */
    public void setTlsEnabled(boolean enabled) {
        tlsEnabled = enabled;
    }

    /**
     * Returns true if TLS is enabled for the connection. The default is
     * for TLS to be disabled.
     *
     * @return true if TLS enabled, false if disabled
     */
    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    /**
     * Returns the <tt>SSLSocketFactory</tt> to use for creating SSL or TLS
     * connections. The default is to use the factory returned by the method
     * <tt>SSLSocketFactory.getDefault</tt>.
     *
     * @return the <tt>SSLSocketFactory</tt> instance
     * @see javax.net.ssl.SSLSocketFactory
     *
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Sets the <tt>SSLSocketFactory</tt> to use for creating SSL or TLS
     * connections.
     *
     * @param sf the <tt>SSLSocketFactory</tt> instance
     * @see javax.net.ssl.SSLSocketFactory
     */
    public void setSSLSocketFactory(SSLSocketFactory sf) {
        sslSocketFactory = sf;
    }

    /**
     * Returns the authentication id. The default authentication id is the
     * the value of the system property <tt>user.name</tt>.
     *
     * @return the authentication id
     */
    public String getAuthenticationId() {
        return authenticationId;
    }
    
    /**
     * Sets the authentication id. This is the identify that must be
     * authenticated when accessing the mail server and is usually required
     * for authentication.
     *
     * @param id the authentication id
     */
    public void setAuthenticationId(String id) {
        authenticationId = id;
    }

    /**
     * Returns the authorization id. This is an alternate user identity that
     * will be used when accessing the mail server. This is optional and
     * only used for SASL authentication.
     *
     * @return the authorization id, or <tt>null</tt> if none
     */
    public String getAuthorizationId() {
        return authorizationId;
    }

    /**
     * Sets the authorization id to use for SASL authentication. This is
     * an alternate user identity that will be used when accessing the mail
     * server.
     *
     * @param id the authorization id
     */
    public void setAuthorizationId(String id) {
        authorizationId = id;
    }

    /**
     * Returns the SASL authentication mechanism for the connection. If
     * <tt>null</tt> then SASL authentication will not be used (this is
     * the default).
     *
     * @return the SASL authentication mechanism, or null if none
     */
    public String getMechanism() {
        return mechanism;
    }
    
    /**
     * Sets the SASL authentication mechanism.
     *
     * @param mech the SASL authentication mechanism
     */
    public void setMechanism(String mech) {
        mechanism = mech;
    }

    /**
     * Returns the SASL authentication realm.
     *
     * @return the SASL authentication realm, or <tt>null</tt> if none
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the realm for SASL authentication.
     *
     * @param realm the SASL realm
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Returns SASL authentication properties. These properties will be
     * specified when
     * {@link javax.security.sasl.Sasl#createSaslClient Sasl.createSaslClient}
     * is called to create the <tt>SaslClient</tt>.
     *
     * @return the SASL authentication properties, or <tt>null</tt> if none
     * @see javax.security.sasl.SaslClient
     */
    public Map<String, String> getSaslProperties() {
        return saslProperties;
    }

    /**
     * Sets optional properties to use for SASL authentication.
     *
     * @param properties the SASL properties to use
     */
    public void setSaslProperties(Map<String, String> properties) {
        saslProperties = properties;
    }

    /**
     * Returns the <tt>AuthenticatorFactory</tt> to use for SASL authentication.
     * The default value is what is returned by calling
     * {@link AuthenticatorFactory#getDefault}
     *
     * @return the <tt>AuthenticatorFactory</tt>
     */
    public AuthenticatorFactory getAuthenticatorFactory() {
        return authenticatorFactory;
    }

    /**
     * Sets the <tt>AuthenticationFactory</tt> to use for SASL authentication.
     *
     * @param factory the new <tt>AuthenticationFactory</tt>
     */
    public void setAuthenticatorFactory(AuthenticatorFactory factory) {
        authenticatorFactory = factory;
    }

    /**
     * Returns the request timeout value in seconds. A sent request will fail
     * if no response has been received within the specified time period.
     *
     * @return the request timeout in seconds, or <tt>-1</tt> if none
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the request timeout value in seconds.
     *
     * @param secs the new request timeout in seconds
     */
    public void setTimeout(int secs) {
        timeout = secs;
    }
    
    /**
     * Returns true if debug output is enabled. The default is for debug
     * output to be disabled.
     *
     * @return true if debug output enabled, false if disabled
     */
    public boolean isDebug() {
        return debug;
    }
    
    /**
     * Enables debug output.
     *
     * @param enabled if true then enable debug output
     */
    public void setDebug(boolean enabled) {
        debug = enabled;
    }

    /**
     * Returns true if trace output is enabled. The default is for trace
     * output to be disabled.
     *
     * @return true if trace enabled, false if disabled
     */
    public boolean isTrace() {
        return trace;
    }

    /**
     * Enables protocol trace output to the trace log.
     * 
     * @param enabled if true then enable trace logging
     */
    public void setTrace(boolean enabled) {
        trace = enabled;
    }

    /**
     * Returns the <tt>PrintStream</tt> to use for logging trace output if
     * enabled. The default is to use <tt>System.out</tt>.
     *
     * @return the trace output stream
     */
    public PrintStream getTraceOut() {
        return traceOut;
    }

    /**
     * Sets the <tt>PrintStream</tt> to use for logging trace output if
     * enabled.
     *
     * @param ps the trace output stream
     */
    public void setTraceStream(PrintStream ps) {
        traceOut = ps;
    }

    /**
     * Optionally disables asynchronous reader thread. This is useful when
     * debugging IMAP connection problems.
     * 
     * @param enabled if true then disable asynchronous reader
     */
    public void setSynchronousMode(boolean enabled) {
        synchronousMode = enabled;
    }

    /**
     * Returns true if asynchronous reader thread should be disabled.
     * The default is to enable the asynchronous reader.
     * 
     * @return true if asynchronous reader disabled, false if not
     */
    public boolean isSynchronousMode() {
        return synchronousMode;
    }

    /**
     * Returns properties for current mail configuration.
     *
     * @return the <tt>Properties</tt> for the configuration
     */
    public Properties toProperties() {
        return Config.toProperties(this);
    }

    /**
     * Applies specified properties to current mail configuration.
     *
     * @param props the <tt>Properties</tt> for the configuration
     */
    public void applyProperties(Properties props) {
        Config.applyProperties(this, props);
    }

    /**
     * Saves configuration properties to specified file.
     *
     * @param file the file to which the configuration should be written
     * @throws IOException if an I/O error occurred
     */
    public void save(File file) throws IOException {
        Config.saveProperties(file, toProperties());
    }
}
