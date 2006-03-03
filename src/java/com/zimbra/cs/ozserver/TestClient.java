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
import java.io.EOFException;
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

import com.zimbra.cs.util.ZimbraLog;

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
// echo num_bytes num_chunks -> write (num_chunks * num_bytes), where first chunk is 'A', second chunk is 'B', etc. 
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
        ZimbraLog.clearContext();
        if (ssl) {
            mSocket = mSocketFactory.createSocket(host, port); 
        } else {
            mSocket = new Socket(host, port);
        }
        if (!mSocket.isConnected()) {
            throw new IOException("not connected");
        }
        mSocketIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        mSocketOut = new BufferedOutputStream(mSocket.getOutputStream());
        readResponse();
        String cid = mResponse.substring(mResponse.indexOf('=') + 1);
        ZimbraLog.addToContext("cid", cid);
    }
    
    private void readResponse() throws IOException {
        mResponse = mSocketIn.readLine();
        if (mResponse == null) {
            throw new EOFException("expecting response");
        }
        mLog.info("cgot: " + mResponse);
    }
    
    public String getLastResponse() {
        return mResponse;
    }
    
    public void helo() throws IOException {
        mSocketOut.write("helo\r\n".getBytes());
        mSocketOut.flush();
        readResponse();
    }
    
    public void quit() throws IOException {
        mSocketOut.write("quit\r\n".getBytes());
        mSocketOut.flush();
        readResponse();
    }
    
    public void sum(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        SocketChannel sc = mSocket.getChannel();
        mSocketOut.write("sum\r\n".getBytes());
        mSocketOut.write(OzSmtpTransparency.apply(buffer).array());
        mSocketOut.write(OzByteArrayMatcher.CRLFDOTCRLF);
        mSocketOut.flush();
        readResponse();
    }
    
    public void nsum(byte[] bytes) throws IOException {
        mSocketOut.write(("nsum " + bytes.length + "\r\n").getBytes());
        mSocketOut.write(bytes);
        mSocketOut.flush();
        readResponse();
    }
    
    public boolean echo(int nb, int nc) throws IOException {
    	mSocketOut.write(("echo " + nb + " " + nc + "\r\n").getBytes());
    	mSocketOut.flush();
    	char[] arr = new char[nb];
    	
    	int target = nb * nc;
    	int totalRead = 0;
        int bv = 'A';
    	do {
    		int nread = mSocketIn.read(arr);
    		TestServer.mLog.info("client read " + nread + " bytes");
    		if (nread < 0) {
    			mResponse = "EOF reached";
    			return false;
    		}
    		for (int k = 0; k < nread; k++) {
    			if (arr[k] != bv) {
    				mResponse = "client found " + arr[k] + " at index " +  k + " when expecting " + (char)bv;
    				return false;
    			}
                totalRead++;
                if ((totalRead % nb) == 0) {
                    bv++;
                }
    		}
    	} while (totalRead < target);
    		
    	if (totalRead == target) {
    		mResponse = "read " + (nb * nc) + " bytes";
    		return true;
    	} else {
    		mResponse = "client read " + totalRead + " bytes total while expecting " + target + " bytes";
    		return false;
    	}
    }
    
    public void close() {
        try {
            mSocket.close();
        } catch (IOException ioe) {
            mLog.warn("exception occurred closing client socket", ioe);
        }
    }
    
    private static Random mRandom = new Random();
    
    public static final int DATA_SIZE_MINIMUM = 28000; 
    public static final int DATA_SIZE_VARIANCE = 1024;
    private static final int MAX_ECHO_CHUNKS = 16;
    
    private static byte randomPrintableAsciiByte(Random r) {
    	return (byte)(' ' + r.nextInt(127 - '!') + 1);
    }
    
    public static void run(String host, int port, boolean ssl) throws IOException {
        TestClient client = new TestClient(host, port, ssl);

        mLog.info("sending: helo");
        client.helo();
        mLog.info("response: " + client.getLastResponse());

        int nb = DATA_SIZE_MINIMUM + mRandom.nextInt(DATA_SIZE_VARIANCE) + 1;
        int nc = mRandom.nextInt(MAX_ECHO_CHUNKS) + 1;
        byte bv = (byte)(mRandom.nextInt(126) + 1);
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

        mLog.info("sending: echo " + nb + " " + nc);
        if (client.echo(nb, nc)) {
            mLog.info("response: OK " + client.getLastResponse());
        } else { 
        	mLog.info("response: FAIL " + client.getLastResponse());
        }
        
        mLog.info("sending: quit");
        client.quit();
        mLog.info("response: " + client.getLastResponse());

        client.close();
    }
    
    public static void main(String[] args) throws IOException {
        run(args[0], Integer.parseInt(args[1]), Boolean.parseBoolean(args[2]));
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
