package com.zimbra.common.util;

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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
// import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
// import javax.net.ssl.TrustManager;

import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;

/**
<p>
* EasySSLProtocolSocketFactory can be used to creats SSL {@link Socket}s 
* that accept self-signed certificates. 
* </p>
* <p>
* This socket factory SHOULD NOT be used for productive systems 
* due to security reasons, unless it is a concious decision and 
* you are perfectly aware of security implications of accepting 
* self-signed certificates
* </p>
*/
public class EasySSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory = null;
    
    public EasySSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null,
                            new TrustManager[] { new GullibleTrustManager(null) },
                            null);
            factory = sslcontext.getSocketFactory();
        } catch(Exception ex) {
            // Use System.out here instead of Log4j, since this class is likely
            // to be used by client code and Log4J may not be available.
            System.out.println("Unable to initialize SSL:\n" + SystemUtil.getStackTrace(ex));
        }
    }
    
    public static SSLSocketFactory getDefault() {
        return new EasySSLSocketFactory();
    }
    
    public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
        return factory.createSocket(socket, s, i, flag);
    }
    
    public Socket createSocket(InetAddress inaddr, int i,
                               InetAddress inaddr1, int j) throws IOException {
        return factory.createSocket(inaddr, i, inaddr1, j);
    }
    
    public Socket createSocket(InetAddress inaddr, int i) throws IOException {
        return factory.createSocket(inaddr, i);
    }

    public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException {
        return factory.createSocket(s, i, inaddr, j);
    }

    public Socket createSocket(String s, int i) throws IOException {
        return factory.createSocket(s, i);
    }
    
    public Socket createSocket() throws IOException {
        return factory.createSocket();
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
    

}
