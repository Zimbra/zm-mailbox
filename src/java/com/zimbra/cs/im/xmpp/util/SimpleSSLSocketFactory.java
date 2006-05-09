/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.util;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.SocketFactory;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/**
 * SSLSocketFactory that accepts any certificate chain and also accepts expired
 * certificates.
 *
 * @author Matt Tucker
 */
public class SimpleSSLSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory factory;

    public SimpleSSLSocketFactory() {

        try {
            SSLContext sslcontent = SSLContext.getInstance("TLS");
            sslcontent.init(null, // KeyManager not required
                            new TrustManager[] { new DummyTrustManager() },
                            new java.security.SecureRandom());
            factory = sslcontent.getSocketFactory();
        }
        catch (NoSuchAlgorithmException e) {
            Log.error(e);
        }
        catch (KeyManagementException e) {
            Log.error(e);
        }
    }

    public static SocketFactory getDefault() {
        return new SimpleSSLSocketFactory();
    }

    public Socket createSocket(Socket socket, String s, int i, boolean flag)
            throws IOException
    {
        return factory.createSocket(socket, s, i, flag);
    }

    public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr2, int j)
            throws IOException
    {
        return factory.createSocket(inaddr, i, inaddr2, j);
    }

    public Socket createSocket(InetAddress inaddr, int i)
            throws IOException
    {
        return factory.createSocket(inaddr, i);
    }

    public Socket createSocket(String s, int i, InetAddress inaddr, int j)
            throws IOException
    {
        return factory.createSocket(s, i, inaddr, j);
    }

    public Socket createSocket(String s, int i)
            throws IOException
    {
        return factory.createSocket(s, i);
    }

    public String[] getDefaultCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    private static class DummyTrustManager implements X509TrustManager {

        public boolean isClientTrusted(X509Certificate[] cert) {
            return true;
        }

        public boolean isServerTrusted(X509Certificate[] cert) {
            try {
                cert[0].checkValidity();
                return true;
            }
            catch (CertificateExpiredException e) {
                return false;
            }
            catch (CertificateNotYetValidException e) {
                return false;
            }
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates,
                String s) throws CertificateException
        {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates,
                String s) throws CertificateException
        {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}