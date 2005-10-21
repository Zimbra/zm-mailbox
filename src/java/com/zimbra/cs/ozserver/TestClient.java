/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// client action  -> server action
//
// connect()      -> greeting
//
// helo           -> ok
//
// sum            -> md5sum
//
// nsum <n>       -> md5sum
//
// quit           -> ok and close()

class TestClient {

    private static Log mLog = LogFactory.getLog(TestClient.class);
    
    // TODO test this case... out.write("ab\r\ncd\r\nquit\r\nef".getBytes());
    
    Socket mSocket;
    
    String mResponse;
        
    BufferedReader mSocketIn;
    
    BufferedOutputStream mSocketOut;
    
    public TestClient(String host, int port) throws IOException {
        mSocket = new Socket(host, port);
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

    
    public static void run(int port) throws IOException {
        TestClient client = new TestClient("localhost", port);
        mLog.info("got: " + client.getLastResponse());

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
        run(Integer.parseInt(args[0]));
    }
}