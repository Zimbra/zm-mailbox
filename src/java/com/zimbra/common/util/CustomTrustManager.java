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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.zimbra.common.localconfig.LC;

/**
 * Custom keystore backed trust manager
 * 
 * @author jjzhuang
 */
public class CustomTrustManager implements X509TrustManager {

	X509TrustManager defaultTrustManager;

	X509TrustManager keyStoreTrustManager;

	KeyStore keyStore;

	Map<String, X509Certificate> pendingCerts = new HashMap<String, X509Certificate>();

	protected CustomTrustManager() {
		try {
			defaultTrustManager = DefaultTrustManager.getInstance();
			loadKeyStore();
			resetKeyStoreTrustManager();
		} catch (GeneralSecurityException x) {
			ZimbraLog.security.error("trust manager init error", x);
		}
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		getDefaultTrustManager().checkClientTrusted(chain, authType);
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			getDefaultTrustManager().checkServerTrusted(chain, authType);
			return;
		} catch (CertificateException x) {}
		try {
			if (keyStore.size() == 0)
				throw new CertificateException("key store empty");
			getKeyStoreTrustManager().checkServerTrusted(chain, authType);
		} catch (CertificateException x) {
			// only accept the very top certificate and not any underlying CAs
			String alias = null;
			if (LC.ssl_allow_accept_untrusted_certs.booleanValue()) {
				alias = chain[0].getSerialNumber().toString(16).toUpperCase();
				synchronized (this) {
					pendingCerts.put(alias, chain[0]);
				}
			}
			
			String certInfo = "";
			try {
				certInfo = new SSLCertInfo(alias, chain[0]).serialize();
			} catch (Exception ex) {}
			throw new CertificateException(certInfo);
		} catch (KeyStoreException x) {
			throw new CertificateException(x);
		}
	}

	public X509Certificate[] getAcceptedIssuers() {
		try {
			return getKeyStoreTrustManager().getAcceptedIssuers();
		} catch (GeneralSecurityException x) {
			return new X509Certificate[0];
		}
	}

	public synchronized void acceptCertificates(String alias)
			throws GeneralSecurityException {
		if (!LC.ssl_allow_accept_untrusted_certs.booleanValue())
			throw new SecurityException("accepting untrusted certificates not allowed: " + alias);
		
		X509Certificate cert = pendingCerts.get(alias);
		if (cert != null) {
			try {
				keyStore.setCertificateEntry(alias, cert);
				saveKeyStore();
				resetKeyStoreTrustManager();
				pendingCerts.remove(alias);
			} catch (KeyStoreException x) {
				ZimbraLog.security.warn("failed to accept certificates of %s", alias);
			}
		} else {
			ZimbraLog.security.warn("Alias %s not found in cache; no certificates accepted.", alias);
		}
	}

	private X509TrustManager getDefaultTrustManager() throws CertificateException {
		if (defaultTrustManager == null)
			throw new CertificateException("no default trust manager");
		return defaultTrustManager;
	}

	private synchronized X509TrustManager getKeyStoreTrustManager() throws CertificateException {
		if (keyStoreTrustManager == null)
			throw new CertificateException("no key store trust manager");
		return keyStoreTrustManager;
	}

	private synchronized void setKeyStoreTrustManager(X509TrustManager trustManager) {
		keyStoreTrustManager = trustManager;
	}

	private synchronized void resetKeyStoreTrustManager() throws GeneralSecurityException {
		TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		factory.init(keyStore);
		TrustManager[] trustManagers = factory.getTrustManagers();
		for (TrustManager tm : trustManagers)
			if (tm instanceof X509TrustManager) {
				setKeyStoreTrustManager((X509TrustManager) tm);
				return;
			}
		throw new KeyStoreException(TrustManagerFactory.getDefaultAlgorithm() + " trust manager not supported");
	}

	private synchronized void loadKeyStore() throws GeneralSecurityException {
		keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		boolean isKeyStoreInitialized = false;
		InputStream kin = null;
		try {
			kin = new FileInputStream(LC.mailboxd_keystore.value());
			try {
				keyStore.load(kin, LC.mailboxd_keystore_password.value().toCharArray());
				isKeyStoreInitialized = true;
			} catch (CertificateException x) {
				ZimbraLog.security.warn("failed to load certificates", x);
			} catch (IOException x) {
				ZimbraLog.security.warn("failed to read keystore file", x);
			}
		} catch (FileNotFoundException x) {
			ZimbraLog.security.info("no keystore found");
		} finally {
			if (kin != null)
				try {
					kin.close();
				} catch (IOException x) {
					ZimbraLog.security.warn("keystore file can't be closed after reading", x);
				}
		}

		if (!isKeyStoreInitialized) {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			try {
				keyStore.load(null, LC.mailboxd_keystore_password.value().toCharArray());
			} catch (IOException x) {
				throw new KeyStoreException(x);
			}
		}
	}

	private synchronized void saveKeyStore() throws GeneralSecurityException {
		OutputStream kout = null;
		try {
			kout = new FileOutputStream(LC.mailboxd_keystore.value());
			try {
				keyStore.store(kout, LC.mailboxd_keystore_password.value().toCharArray());
			} catch (IOException x) {
				throw new KeyStoreException(x);
			}
		} catch (FileNotFoundException x) {
			throw new KeyStoreException(x);
		} finally {
			if (kout != null)
				try {
					kout.close();
				} catch (IOException x) {
					throw new KeyStoreException(x);
				}
		}
	}

	private static CustomTrustManager instance;

	public static synchronized CustomTrustManager getInstance() {
		if (instance == null)
			instance = new CustomTrustManager();
		return instance;
	}
}
