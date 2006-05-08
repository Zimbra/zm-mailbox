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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.net;

import com.zimbra.cs.im.xmpp.util.Log;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;

/**
 * Securue socket factory wrapper allowing simple setup of all security
 * SSL related parameters.
 *
 * @author Iain Shigeoka
 */
public class SSLJiveServerSocketFactory extends SSLServerSocketFactory {

    public static SSLServerSocketFactory getInstance(String algorithm,
                                                     KeyStore keystore,
                                                     KeyStore truststore) throws
            IOException {

        try {
            SSLContext sslcontext = SSLContext.getInstance(algorithm);
            SSLServerSocketFactory factory;
            KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyFactory.init(keystore, SSLConfig.getKeyPassword().toCharArray());
            TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustFactory.init(truststore);

            sslcontext.init(keyFactory.getKeyManagers(),
                    trustFactory.getTrustManagers(),
                    new java.security.SecureRandom());
            factory = sslcontext.getServerSocketFactory();
            return new SSLJiveServerSocketFactory(factory);
        }
        catch (Exception e) {
            Log.error(e);
            throw new IOException(e.getMessage());
        }
    }

    private SSLServerSocketFactory factory;

    private SSLJiveServerSocketFactory(SSLServerSocketFactory factory) {
        this.factory = factory;
    }

    public ServerSocket createServerSocket(int i) throws IOException {
        return factory.createServerSocket(i);
    }

    public ServerSocket createServerSocket(int i, int i1) throws IOException {
        return factory.createServerSocket(i, i1);
    }

    public ServerSocket createServerSocket(int i, int i1, InetAddress inetAddress) throws IOException {
        return factory.createServerSocket(i, i1, inetAddress);
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}
