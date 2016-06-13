/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
