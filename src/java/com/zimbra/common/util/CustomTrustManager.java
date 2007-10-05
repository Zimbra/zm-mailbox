/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.common.util;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.TrustManagerFactory;
import com.sun.net.ssl.X509TrustManager;

/**
 * So that we make a TrustManager available to application
 *  
 * @author jjzhuang
 */
public class CustomTrustManager implements javax.net.ssl.X509TrustManager {
    
	X509TrustManager stockTrustManager;
	
	public CustomTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length == 0) {
            throw new NoSuchAlgorithmException("SunX509 trust manager not supported");
        }
    	stockTrustManager = (X509TrustManager)trustManagers[0];
	}
	
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        stockTrustManager.isClientTrusted(chain);
    }
    
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        stockTrustManager.isServerTrusted(chain);
    }
    
    public X509Certificate[] getAcceptedIssuers() {
        return stockTrustManager.getAcceptedIssuers();
    }
}