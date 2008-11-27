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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.zimbra.common.localconfig.LC;

/**
 * Default cacerts backed trust manager
 *  
 * @author jjzhuang
 */
public class DefaultTrustManager implements X509TrustManager {
    
	X509TrustManager keyStoreTrustManager;
	
	protected DefaultTrustManager() throws GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream kin = null;
    	try {
    		kin = new FileInputStream(LC.mailboxd_truststore.value());
    		try {
    			keyStore.load(kin, LC.mailboxd_truststore_password.value().toCharArray());
    		} catch (IOException x) {
    			throw new KeyStoreException(x);
    		}
    	} catch (FileNotFoundException x) {
    		throw new KeyStoreException(x);
    	} finally {
    		if (kin != null)
    			try {
    				kin.close();
    			} catch (IOException x) {
    				throw new KeyStoreException(x);
    			}
    	}
		
    	TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    	factory.init(keyStore);
    	TrustManager[] trustManagers = factory.getTrustManagers();
    	for (TrustManager tm : trustManagers)
    		if (tm instanceof X509TrustManager) {
    			keyStoreTrustManager = (X509TrustManager)tm;
    			return;
    		}
        throw new KeyStoreException(TrustManagerFactory.getDefaultAlgorithm() + " trust manager not supported");
	}
	
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    	keyStoreTrustManager.checkClientTrusted(chain, authType);
    }
    
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    	keyStoreTrustManager.checkServerTrusted(chain, authType);
    }
    
    public X509Certificate[] getAcceptedIssuers() {
        return keyStoreTrustManager.getAcceptedIssuers();
    }
    
    private static DefaultTrustManager instance;
    
    public static synchronized DefaultTrustManager getInstance() throws GeneralSecurityException {
    	if (instance == null)
    		instance = new DefaultTrustManager();
    	return instance;
    }
}





