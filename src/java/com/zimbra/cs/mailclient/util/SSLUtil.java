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
package com.zimbra.cs.mailclient.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Various utility methods for SSL support.
 */
public final class SSLUtil {
    /**
     * Returns an SSLContext that can be used to create SSL connections without
     * certificates. This is obviously insecure and should only be used for
     * testing.
     *
     * @return an SSLContext that trusts all certificates
     */
    public static SSLContext getDummySSLContext() {
        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] cert, String authType) {
                // trust all certs
            }
            public void checkServerTrusted(X509Certificate[] cert, String authType) {
                // trust all certs
            }
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { tm }, null);
            return sc;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create SSL context", e);
        }
    }
}
