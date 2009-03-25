/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

// trust manager that trusts all clients and servers no matter what
public class GullibleTrustManager extends EasyX509TrustManager {

    public GullibleTrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        super(keystore);
    }

    public boolean isClientTrusted(X509Certificate[] certificates) {
        return true;
    }

    public boolean isServerTrusted(X509Certificate[] certificates) {
        return true;
    }

}
