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
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.net;

import com.zimbra.cs.im.xmpp.util.JiveGlobals;
import com.zimbra.cs.im.xmpp.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;

/**
 * Configuration of Wildfire's SSL settings.
 *
 * @author Iain Shigeoka
 */
public class SSLConfig {

    private static SSLJiveServerSocketFactory sslFactory;
    private static KeyStore keyStore;
    private static String keypass;
    private static KeyStore trustStore;
    private static String trustpass;
    private static String keyStoreLocation;
    private static String trustStoreLocation;

    private SSLConfig() {
    }

    static {
        String algorithm = JiveGlobals.getProperty("xmpp.socket.ssl.algorithm", "TLS");
        String storeType = JiveGlobals.getProperty("xmpp.socket.ssl.storeType", "jks");

        // Get the keystore location. The default location is security/keystore
        keyStoreLocation = JiveGlobals.getProperty("xmpp.socket.ssl.keystore",
                "resources" + File.separator + "security" + File.separator + "keystore");
        keyStoreLocation = JiveGlobals.getHomeDirectory() + File.separator + keyStoreLocation;

        // Get the keystore password. The default password is "changeit".
        keypass = JiveGlobals.getProperty("xmpp.socket.ssl.keypass", "changeit");
        keypass = keypass.trim();

        // Get the truststore location; default at security/truststore
        trustStoreLocation = JiveGlobals.getProperty("xmpp.socket.ssl.truststore",
                "resources" + File.separator + "security" + File.separator + "truststore");
        trustStoreLocation = JiveGlobals.getHomeDirectory() + File.separator + trustStoreLocation;

        // Get the truststore passwprd; default is "changeit".
        trustpass = JiveGlobals.getProperty("xmpp.socket.ssl.trustpass", "changeit");
        trustpass = trustpass.trim();

        try {
            keyStore = KeyStore.getInstance(storeType);
            keyStore.load(new FileInputStream(keyStoreLocation), keypass.toCharArray());

            trustStore = KeyStore.getInstance(storeType);
            trustStore.load(new FileInputStream(trustStoreLocation), trustpass.toCharArray());

            sslFactory = (SSLJiveServerSocketFactory)SSLJiveServerSocketFactory.getInstance(
                    algorithm, keyStore, trustStore);
        }
        catch (Exception e) {
            Log.error("SSLConfig startup problem.\n" +
                    "  storeType: [" + storeType + "]\n" +
                    "  keyStoreLocation: [" + keyStoreLocation + "]\n" +
                    "  keypass: [" + keypass + "]\n" +
                    "  trustStoreLocation: [" + trustStoreLocation+ "]\n" +
                    "  trustpass: [" + trustpass + "]", e);
            keyStore = null;
            trustStore = null;
            sslFactory = null;
        }
    }

    public static String getKeyPassword() {
        return keypass;
    }

    public static String getTrustPassword() {
        return trustpass;
    }

    public static String[] getDefaultCipherSuites() {
        String[] suites;
        if (sslFactory == null) {
            suites = new String[]{};
        }
        else {
            suites = sslFactory.getDefaultCipherSuites();
        }
        return suites;
    }

    public static String[] getSpportedCipherSuites() {
        String[] suites;
        if (sslFactory == null) {
            suites = new String[]{};
        }
        else {
            suites = sslFactory.getSupportedCipherSuites();
        }
        return suites;
    }

    public static KeyStore getKeyStore() throws IOException {
        if (keyStore == null) {
            throw new IOException();
        }
        return keyStore;
    }

    public static KeyStore getTrustStore() throws IOException {
        if (trustStore == null) {
            throw new IOException();
        }
        return trustStore;
    }

    public static void saveStores() throws IOException {
        try {
            keyStore.store(new FileOutputStream(keyStoreLocation), keypass.toCharArray());
            trustStore.store(new FileOutputStream(trustStoreLocation), trustpass.toCharArray());
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static ServerSocket createServerSocket(int port, InetAddress ifAddress) throws
            IOException {
        if (sslFactory == null) {
            throw new IOException();
        }
        else {
            return sslFactory.createServerSocket(port, -1, ifAddress);
        }
    }
}