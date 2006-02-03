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
package com.zimbra.cs.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Allows Java code to make SSL connections without certificates.  This
 * class is insecure and should only be used for testing.  See SSLNOTES.txt
 * in the JavaMail distribution for more details.
 *  
 * @author bburtin
 */
public class DummySSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory;
    
    public DummySSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null,
                            new TrustManager[] { new DummyTrustManager()},
                            null);
            factory = sslcontext.getSocketFactory();
        } catch(Exception ex) {
            // Use System.out here instead of Log4j, since this class is likely
            // to be used by client code and Log4J may not be available.
            System.out.println("Unable to initialize SSL:\n" + SystemUtil.getStackTrace(ex));
        }
    }
    
    public static SocketFactory getDefault() {
        return new DummySSLSocketFactory();
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

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}