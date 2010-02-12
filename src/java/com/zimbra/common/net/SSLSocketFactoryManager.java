/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.zimbra.common.util.ZimbraLog;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import com.zimbra.common.localconfig.LC;

public class SSLSocketFactoryManager {

    private static boolean isInitialzied = false;

    public synchronized static void init() {
        if (isInitialzied)
            return;

        //ProtocolSocketFactory is used by HttpClient

        ProtocolSocketFactory psFactory = null;

        String className = LC.zimbra_class_sslprotocolsocketfactory.value();
        if (className != null && !className.equals("")) {
            try {
                psFactory = (ProtocolSocketFactory)Class.forName(className).newInstance();
            } catch (Exception x) {
                ZimbraLog.security.error("could not instantiate ProtocolSocketFactory interface of class '%s'", className, x);
            }
        }

        if (psFactory == null && LC.ssl_allow_untrusted_certs.booleanValue())
            psFactory = new EasySSLProtocolSocketFactory();

        if (psFactory != null) {
            Protocol https = new Protocol("https", psFactory, 443);
            Protocol.registerProtocol("https", https);
        }

        //Init HttpsURLConnection

        try {
            SSLSocketFactory sockFactory = LC.data_source_trust_self_signed_certs.booleanValue() ?
                new DummySSLSocketFactory() : new CustomSSLSocketFactory(false); //HttpsURLConnection has the HostnameVerifier interface
            HttpsURLConnection.setDefaultSSLSocketFactory(sockFactory);
        } catch (GeneralSecurityException x) {
            ZimbraLog.security.error("could not init HttpsURLConnection with SSLSocketFactory", x);
        }
        //HttpsURLConnection.setDefaultHostnameVerifier(new CustomSSLSocketUtil.HostnameVerifier());
    }

    public static String getDefaultSSLSocketFactoryClassName() {
        return LC.data_source_trust_self_signed_certs.booleanValue() ? DummySSLSocketFactory.class.getName() : CustomSSLSocketFactory.class.getName();
    }

    public static SSLSocketFactory getDefaultSSLSocketFactory() {
        try {
            return LC.data_source_trust_self_signed_certs.booleanValue() ? new DummySSLSocketFactory() : new CustomSSLSocketFactory(true);
        } catch (GeneralSecurityException x) {
            throw new RuntimeException(x);
        }
    }
}
