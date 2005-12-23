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

package com.zimbra.cs.ozserver;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

// client action  -> server action
//
// connect()      -> greeting
//
// helo           -> ok
//
// sum            -> calculated sum
//
// nsum <n>       -> calculated sum
//
// quit           -> ok and close()

class TestClient {

    private static Log mLog = LogFactory.getLog(TestClient.class);
    
    // TODO test this case... out.write("ab\r\ncd\r\nquit\r\nef".getBytes());
    
    Socket mSocket;
    
    String mResponse;
        
    BufferedReader mSocketIn;
    
    BufferedOutputStream mSocketOut;
    
    private static DummySSLSocketFactory mSocketFactory = new DummySSLSocketFactory();
    
    public TestClient(String host, int port, boolean ssl) throws IOException {
        if (ssl) {
            mSocket = mSocketFactory.createSocket(host, port); 
        } else {
            mSocket = new Socket(host, port);
        }
        mSocketIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        mSocketOut = new BufferedOutputStream(mSocket.getOutputStream());
        mResponse = mSocketIn.readLine();
        mLog.info("got: " + mResponse);
    }
    
    public String getLastResponse() {
        return mResponse;
    }
    
    public void helo() throws IOException {
        mSocketOut.write("helo\r\n".getBytes());
        mSocketOut.flush();
        mResponse = mSocketIn.readLine();
        mLog.info("got: " + mResponse);
    }
    
    public void quit() throws IOException {
        mSocketOut.write("quit\r\n".getBytes());
        mSocketOut.flush();
        mResponse = mSocketIn.readLine();
        mLog.info("got: " + mResponse);
    }
    
    public void sum(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        SocketChannel sc = mSocket.getChannel();
        mSocketOut.write("sum\r\n".getBytes());
        mSocketOut.write(OzSmtpTransparency.apply(buffer).array());
        mSocketOut.write(OzByteArrayMatcher.CRLFDOTCRLF);
        mSocketOut.flush();
        mResponse = mSocketIn.readLine();
        mLog.info("got: " + mResponse);
    }
    
    public void nsum(byte[] bytes) throws IOException {
        mSocketOut.write(("nsum " + bytes.length + "\r\n").getBytes());
        mSocketOut.write(bytes);
        mSocketOut.flush();
        mResponse = mSocketIn.readLine();
        mLog.info("got: " + mResponse);
    }
    
    public void close() {
        try {
            mSocket.close();
        } catch (IOException ioe) {
            mLog.warn("exception occurred closing client socket", ioe);
        }
    }
    
    private static Random random = new Random();
    
    private static final int MAX_DIGEST_BYTES = 20;

    
    public static void run(int port, boolean ssl) throws IOException {
        TestClient client = new TestClient("localhost", port, ssl);

        mLog.info("sending: helo");
        client.helo();
        mLog.info("response: " + client.getLastResponse());

        int nb = random.nextInt(MAX_DIGEST_BYTES) + 1;
        byte bv = (byte)(random.nextInt(126) + 1);
        byte[] ba = new byte[nb];
        Arrays.fill(ba, 0, nb, bv);
        
        mLog.info("sending: sum n=" + nb + " v=" + bv);
        client.sum(ba);
        long sum = new Long(client.getLastResponse()).longValue();
        if (sum != (nb * bv)) {
            mLog.info("response: FAIL client expected=" + (nb * bv) + " got=" + sum);
        } else {
            mLog.info("response: OK expected and got " + sum);
        }

        mLog.info("sending: nsum n=" + nb + " v=" + bv);
        client.nsum(ba);
        long nsum = new Long(client.getLastResponse()).longValue();
        if (nsum != (nb * bv)) {
            mLog.info("response: FAIL client expected=" + (nb * bv) + " got=" + nsum);
        } else {
            mLog.info("response: OK expected and got " + nsum);
        }
            
        mLog.info("sending: quit");
        client.quit();
        mLog.info("response: " + client.getLastResponse());
        
        client.close();
    }
    
    public static void main(String[] args) throws IOException {
        run(Integer.parseInt(args[0]), Boolean.parseBoolean(args[1]));
    }
    
    public static class DummySSLSocketFactory extends SSLSocketFactory {
        private SSLSocketFactory factory;
        
        public DummySSLSocketFactory() {
            try {
                SSLContext sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(null,
                                new TrustManager[] { new DummyTrustManager()},
                                null);
                factory = (SSLSocketFactory)sslcontext.getSocketFactory();
            } catch(Exception ex) {
                // ignore
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

    /**
     * DummyTrustManager - NOT SECURE
     */
    public static class DummyTrustManager implements X509TrustManager {
        
        public void checkClientTrusted(X509Certificate[] cert, String authType) {
            // everything is trusted
        }
        
        public void checkServerTrusted(X509Certificate[] cert, String authType) {
            // everything is trusted
        }
        
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
