/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.net;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.SocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.ProxySelector;

/*
 * Factory class for various SocketFactory types.
 */
public final class SocketFactories {
    private static NetConfig config = NetConfig.getInstance();

    private static boolean registered;
    
    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    /**
     * Registers default HTTP and HTTPS protocol handlers for ZCS and ZDesktop
     * using CustomSSLSocketFactory. If local config attribute "socks_enabled"
     * is true then optional SOCKS proxy support will be enabled. If attribute
     * "ssl_allow_untrusted_certs" is true then a dummy SSLSocketFactory will
     * be configured that trusts all certificates.
     */
    public static void registerProtocolsServer() {
        register(config.isAllowUntrustedCerts() ?
            TrustManagers.dummyTrustManager() : TrustManagers.customTrustManager());
    }

    /**
     * Registers default HTTP and HTTPS protocol handlers for CLI tools and
     * standalone unit tests using the system default SSLSocketFactory. SOCKS
     * proxy support is not enabled. If attribute "ssl_allow_untrusted_certs"
     * is true then a dummy SSLSocketFactory will be configured that trusts
     * all certificates.
     */
    public static void registerProtocols() {
        registerProtocols(config.isAllowUntrustedCerts());
    }

    /**
     * Registers default HTTP and HTTPS protocol handlers for CLI tools and
     * standalone unit tests using the system default SSLSocketFactory. SOCKS
     * proxy support is not enabled. If allowUntrustedCerts is true then a
     * dummy SSLSocketFactory will be configured that trusts all certificates.
     */
    public static void registerProtocols(boolean allowUntrustedCerts) {
        register(allowUntrustedCerts ? TrustManagers.dummyTrustManager() : null);
    }
    
    private synchronized static void register(X509TrustManager tm) {
        if (registered) return;

        // Set default TrustManager
        TrustManagers.setDefaultTrustManager(tm);
        
        // Register Apache Commons HTTP/HTTPS protocol socket factories
        ProtocolSocketFactory psf = defaultProtocolSocketFactory();
        Protocol.registerProtocol(HTTP, new Protocol(HTTP, psf, 80));
        ProtocolSocketFactory spsf = defaultSecureProtocolSocketFactory();
        Protocol.registerProtocol(HTTPS, new Protocol(HTTPS, spsf, 443));

        // HttpURLConnection already uses system ProxySelector by default
        // Set HttpsURLConnection SSL socket factory and optional hostname verifier
        HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSocketFactory(false));
        if (tm instanceof CustomTrustManager) {
            HttpsURLConnection.setDefaultHostnameVerifier(new CustomHostnameVerifier());
        }

        // Set the system-wide default ProxySelector
        ProxySelector.setDefault(ProxySelectors.defaultProxySelector());

        registered = true;
    }

    /**
     * Returns a ProtocolSocketFactory that wraps the specified SocketFactory.
     *
     * @param sf the SocketFactory
     * @return a ProtocolSocketFactory wrapping the SocketFactory
     */
    public static ProtocolSocketFactory protocolSocketFactory(SocketFactory sf) {
        return new ProtocolSocketFactoryWrapper(sf);
    }

    /**
     * Returns a ProtocolSocketFactory that wraps the default SocketFactory.
     *
     * @return ProtocolSocketFactory wrapping the default SocketFactory
     */
    public static ProtocolSocketFactory defaultProtocolSocketFactory() {
        return protocolSocketFactory(defaultSocketFactory());
    }

    /**
     * Returns a SecureProtocolSocketFactory that wraps the specified
     * SSLSocketFactory.
     *
     * @param ssf the SSLSocketFactory
     * @return a SecureProtocolSocketFactory wrapping the SSLSocketFactory
     */
    public static SecureProtocolSocketFactory secureProtocolSocketFactory(SSLSocketFactory ssf) {
        return new SecureProtocolSocketFactoryWrapper(ssf);
    }

    /**
     * Returns a SecureProtocolSocketFactory that wraps the default SSLSocketFactory.
     *
     * @return a SecureProtocolSocketFactory wrapping the default SSLSocketFactory
     */
    public static SecureProtocolSocketFactory defaultSecureProtocolSocketFactory() {
        return secureProtocolSocketFactory(defaultSSLSocketFactory());
    }

    /**
     * Returns a SecureProtocolSocketFactory that trusts all certificates.
     *
     * @return a SecureProtocolSocketFactory trusting all certificates
     */
    public static SecureProtocolSocketFactory dummySecureProtocolSocketFactory() {
        return secureProtocolSocketFactory(dummySSLSocketFactory());
    }

    /**
     * Returns a SocketFactory that uses the specified ProxySelector for
     * new connections.
     * 
     * @param ps the ProxySelector to use
     * @return the SocketFactory using the ProtocolSelector
     */
    public static SocketFactory proxySelectorSocketFactory(ProxySelector ps) {
        return new ProxySelectorSocketFactory(ps);
    }

    /**
     * Returns a SocketFactory that uses the default ProxySelector for new
     * connections.
     *
     * @return the SocketFactory using the default ProxySelector
     */
    public static SocketFactory proxySelectorSocketFactory() {
        return new ProxySelectorSocketFactory();
    }

    /**
     * Returns the default SSLSocketFactory based the default TrustManager
     * (see TrustManagers.defaultTrustManager).
     *
     * @return the default SSLSocketFactory
     */
    public static SSLSocketFactory defaultSSLSocketFactory() {
        return defaultSSLSocketFactory(config.isAllowMismatchedCerts());
    }

    private static SSLSocketFactory defaultSSLSocketFactory(boolean verifyHostname) {
        return defaultSSLSocketFactory(TrustManagers.defaultTrustManager(), verifyHostname);
    }

    private static SSLSocketFactory defaultSSLSocketFactory(TrustManager tm, boolean verifyHostname) {
        SocketFactory sf = config.isSocksEnabled() ? defaultSocketFactory() : null;
        try {
            return new CustomSSLSocketFactory(tm, sf, verifyHostname);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create CustomSSLSocketFactory", e);
        }
    }

    /**
     * Returns the default SocketFactory using SOCKS if configured.
     *
     * @return the default SocketFactoryh
     */
    public static SocketFactory defaultSocketFactory() {
        return config.isSocksEnabled() ?
            proxySelectorSocketFactory() : SocketFactory.getDefault();
    }

    // Factory methods used specifically for testing

    /**
     * Returns an SSLSocketFactory that trusts all certificates.
     * @return an SSLSocketFactory that trusts all certificates
     */
    public static SSLSocketFactory dummySSLSocketFactory() {
        return defaultSSLSocketFactory(TrustManagers.dummyTrustManager(), true);
    }
}
