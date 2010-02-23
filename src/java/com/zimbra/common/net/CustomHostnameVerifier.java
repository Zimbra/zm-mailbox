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
import com.zimbra.common.util.ZimbraLog;
import sun.security.util.HostnameChecker;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CustomHostnameVerifier implements HostnameVerifier {
    public static void verifyHostname(String hostname, SSLSession session) throws IOException {
        if (LC.ssl_allow_mismatched_certs.booleanValue()) return;

        try {
            InetAddress.getByName(hostname);
        } catch (UnknownHostException uhe) {
            throw new UnknownHostException("Could not resolve SSL sessions server hostname: " + hostname);
        }

        javax.security.cert.X509Certificate[] certs = session.getPeerCertificateChain();
        if (certs == null || certs.length == 0)
            throw new SSLPeerUnverifiedException("No server certificates found: " + hostname);

        X509Certificate cert = certJavax2Java(certs[0]);

        CustomTrustManager ctm = TrustManagers.customTrustManager();
        if (ctm.isCertificateAcceptedForHostname(hostname, cert))
            return;

        HostnameChecker hc = HostnameChecker.getInstance(HostnameChecker.TYPE_TLS);
        try {
            hc.match(hostname, cert);
        } catch (CertificateException x) {
            String certInfo = ctm.handleCertificateCheckFailure(hostname, cert, true);
            throw new SSLPeerUnverifiedException(certInfo);
        }
    }

    private static java.security.cert.X509Certificate certJavax2Java(javax.security.cert.X509Certificate cert) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(cert.getEncoded());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate) cf.generateCertificate(bis);
        } catch (java.security.cert.CertificateEncodingException e) {
        } catch (javax.security.cert.CertificateEncodingException e) {
        } catch (java.security.cert.CertificateException e) {
        }
        return null;
    }
        
    public boolean verify(String hostname, SSLSession session) {
        try {
            verifyHostname(hostname, session);
        } catch (IOException e) {
            ZimbraLog.security.debug(
                "Hostname verification failed: hostname = " + hostname, e);
            return false;
        }
        return true;
    }
}
