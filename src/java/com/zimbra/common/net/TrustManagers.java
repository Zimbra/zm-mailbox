/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

public final class TrustManagers {
    private static CustomTrustManager customTrustManager;

    public static synchronized X509TrustManager defaultTrustManager() {
        return LC.ssl_allow_untrusted_certs.booleanValue() ?
            dummyTrustManager() : customTrustManager();
    }
    
    public static synchronized CustomTrustManager customTrustManager() {
        if (customTrustManager == null) {
            try {
                customTrustManager = new CustomTrustManager();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Unable to create CustomTrustManager", e);
            }
        }
        return customTrustManager;
    }
    
    public static X509TrustManager dummyTrustManager() {
        return new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // Trust all certs from client
            }
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // Trust all certs from server
            }
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
