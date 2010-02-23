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

import com.zimbra.common.localconfig.LC;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.SocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.ProxySelector;

public final class SocketFactories {
    private static final boolean SOCKS_ENABLED = LC.socks_enabled.booleanValue();

    private static boolean registered;
    
    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    public static void registerProtocolsServer() {
        register(LC.ssl_allow_untrusted_certs.booleanValue() ?
            TrustManagers.dummyTrustManager() : TrustManagers.customTrustManager());
    }
    
    public static void registerProtocols() {
        registerProtocols(LC.ssl_allow_untrusted_certs.booleanValue());
    }
    
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

        registered = true;
    }

    public static ProtocolSocketFactory protocolSocketFactory(SocketFactory sf) {
        return new ProtocolSocketFactoryWrapper(sf);
    }

    public static ProtocolSocketFactory defaultProtocolSocketFactory() {
        return protocolSocketFactory(defaultSocketFactory());
    }

    public static SecureProtocolSocketFactory secureProtocolSocketFactory(SSLSocketFactory ssf) {
        return new SecureProtocolSocketFactoryWrapper(ssf);
    }

    public static SecureProtocolSocketFactory defaultSecureProtocolSocketFactory() {
        return secureProtocolSocketFactory(defaultSSLSocketFactory());
    }

    public static SecureProtocolSocketFactory dummySecureProtocolSocketFactory() {
        return secureProtocolSocketFactory(dummySSLSocketFactory());
    }

    public static SocketFactory proxySelectorSocketFactory(ProxySelector ps) {
        return new ProxySelectorSocketFactory(ps);
    }

    public static SocketFactory proxySelectorSocketFactory() {
        return new ProxySelectorSocketFactory();
    }

    public static SSLSocketFactory defaultSSLSocketFactory() {
        return defaultSSLSocketFactory(LC.ssl_allow_mismatched_certs.booleanValue());
    }

    public static SSLSocketFactory defaultSSLSocketFactory(boolean verifyHostname) {
        return defaultSSLSocketFactory(TrustManagers.defaultTrustManager(), verifyHostname);
    }

    public static SSLSocketFactory defaultSSLSocketFactory(TrustManager tm, boolean verifyHostname) {
        SocketFactory sf = SOCKS_ENABLED ? defaultSocketFactory() : null;
        try {
            return new CustomSSLSocketFactory(tm, sf, verifyHostname);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create CustomSSLSocketFactory", e);
        }
    }
    
    public static SocketFactory defaultSocketFactory() {
        return SOCKS_ENABLED ? proxySelectorSocketFactory() : SocketFactory.getDefault();
    }

    // Factory methods used specifically for testing

    public static SSLSocketFactory dummySSLSocketFactory() {
        return defaultSSLSocketFactory(TrustManagers.dummyTrustManager(), true);
    }
}
