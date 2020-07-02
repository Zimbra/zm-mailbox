/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.zimbra.common.util.Log;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.util.Config;

/**
 * Represents configuration common to all mail client protocols.
 */
public abstract class MailConfig {
    private String host;
    private int port = -1;
    private Security security;
    private String authorizationId;
    private String authenticationId;
    private String mechanism;
    private String realm;
    private Map<String, String> saslProperties;
    private Log logger;
    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private AuthenticatorFactory authenticatorFactory;
    private int readTimeout;
    private int connectTimeout;

    public static enum Security {
        NONE, SSL, TLS, TLS_IF_AVAILABLE
    }

    /**
     * Creates a new {@link MailConfig} instance.
     *
     * @param log default logger
     */
    protected MailConfig(Log log) {
        logger = Preconditions.checkNotNull(log);
        security = Security.NONE;
    }

    /**
     * Creates a new {@link MailConfig} instance for the specified mail
     * server host.
     *
     * @param log default logger
     * @param host the mail server host
     */
    protected MailConfig(Log log, String host) {
        this(log);
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

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        if (security == null) {
            throw new NullPointerException();
        }
        this.security = security;
    }

    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    public void setSocketFactory(SocketFactory sf) {
        socketFactory = sf;
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
     * Returns the authentication id.
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
        if (id == null) {
            throw new NullPointerException();
        }
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
     * Returns the current socket read timeout value in seconds. This is the
     * maximum number of seconds to wait for data when reading from the connection.
     *
     * @return the read timeout in seconds, or <tt>0</tt> if infinite
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the socket read timeout value in seconds.
     *
     * @param secs the new read timeout in seconds
     */
    public void setReadTimeout(int secs) {
        readTimeout = secs;
    }

    /**
     * Returns the socket connect timeout value in seconds. This is the maximum
     * number of seconds to a new connection to get established.
     *
     * @return the socket connect timeout in seconds, or <tt>0</tt> if infinite
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the socket connect timeout value in seconds.
     *
     * @param secs the new socket connect timeout in seconds
     */
    public void setConnectTimeout(int secs) {
        connectTimeout = secs;
    }

    /**
     * Returns the logger.
     *
     * @return logger
     */
    public Log getLogger() {
        return logger;
    }

    /**
     * Sets the logger.
     *
     * @param log logger
     */
    public void setLogger(Log log) {
        logger = Preconditions.checkNotNull(log);
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper
            .add("host", host)
            .add("port", port)
            .add("security", security)
            .add("readTimeout", readTimeout)
            .add("connectTimeout", connectTimeout);
        if (null != mechanism) {
            helper.add("mechanism", mechanism);
        }
        if (null != realm) {
            helper.add("realm", realm);
        }
        if (null != authorizationId) {
            helper.add("authorizationId", "<not null>");
        }
        if (null != authorizationId) {
            helper.add("authenticationId", "<not null>");
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
