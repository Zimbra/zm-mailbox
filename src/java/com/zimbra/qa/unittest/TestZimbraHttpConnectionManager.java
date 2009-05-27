/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

/*
 * for SimpleHttpServer
 */
import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.servlet.ZimbraServlet;

import junit.framework.TestCase;

public class TestZimbraHttpConnectionManager extends TestCase {
    
    // hack before I figure out how to run only the selected tests in the unit test framework
    enum Test {
        testReaper(false),
        testHttpState(true),
        testSoTimeoutViaHttpMethod(false),
        testSoapProv(false);
        
        private boolean mRunIt;
        Test(boolean runIt) {
            mRunIt = runIt;
        }
        
        private boolean runIt() {
            return mRunIt; 
        }
    }
    
    private boolean runIt() {
        
        String testName = getName();
        if (!Test.valueOf(testName).runIt()) {
            System.out.println("Skippping test: " + testName);
            return false;
        } else
            return true;
    }

    private static void dumpResponse(int respCode, HttpMethod method, String prefix) throws IOException {
        
        prefix = prefix + " - ";
        
        // status
        int statusCode = method.getStatusCode();
        String statusLine = method.getStatusLine().toString();
        
        System.out.println(prefix + "respCode=" + respCode);
        System.out.println(prefix + "statusCode=" + statusCode);
        System.out.println(prefix + "statusLine=" + statusLine);
        
        // headers
        System.out.println(prefix + "Headers");
        Header[] respHeaders = method.getResponseHeaders();
        for (int i=0; i < respHeaders.length; i++) {
            String header = respHeaders[i].toString();
            // trim the CRLF at the end to save space
            System.out.println(prefix + header.trim());
        }
        
        // body
        byte[] bytes = ByteUtil.getContent(method.getResponseBodyAsStream(), 0);
        System.out.println(prefix + bytes.length + " bytes read");
    }
    
    
    /**
     * A thread that performs a GET.
     */
    private static class TestGetThread extends Thread {
        
        private HttpClient mHttpClient;
        private GetMethod mMethod;
        private int mId;
        
        public TestGetThread(HttpClient httpClient, GetMethod method, int id) {
            mHttpClient = httpClient;
            mMethod = method;
            mId = id;
        }
        
        /**
         * Executes the GetMethod and prints some status information.
         */
        public void run() {
            try {
                System.out.println(mId + " - about to get something from " + mMethod.getURI());
                // execute the method
                int respCode = mHttpClient.executeMethod(mMethod);
                
                System.out.println(mId + " - get executed");
                // get the response body as an array of bytes
                // byte[] bytes = method.getResponseBody();
                // dumpResponse(respCode, mMethod, Integer.valueOf(mId).toString());
                
            } catch (Exception e) {
                System.out.println(mId + " - error: " + e);
            } finally {
                // always release the connection after we're done 
                mMethod.releaseConnection();
                System.out.println(mId + " - connection released");
            }
        }
    }
    
    /*    
     * set in localconfig.xml before running this test

      <key name="httpclient_connmgr_idle_reaper_sleep_interval">
        <value>5000</value>
      </key>
      
        <key name="httpclient_connmgr_idle_reaper_connection_timeout">
        <value>2000</value>
      </key>
      
    */  
    public void testReaper() throws Exception {
        if (!runIt())
            return;
        
        // create an array of URIs to perform GETs on
        String[] urisToGet = {
            "http://hc.apache.org:80/",
            "http://hc.apache.org:80/httpclient-3.x/status.html",
            "http://hc.apache.org:80/httpclient-3.x/methods/",
            "http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/"
        };
        
        ZimbraHttpConnectionManager connMgr = ZimbraHttpConnectionManager.getExternalHttpConnMgr();
        
        // create a thread for each URI
        TestGetThread[] threads = new TestGetThread[urisToGet.length];
        for (int i = 0; i < threads.length; i++) {
            GetMethod get = new GetMethod(urisToGet[i]);
            get.setFollowRedirects(true);
            threads[i] = new TestGetThread(connMgr.newHttpClient(), get, i + 1);
        }
        
        ZimbraHttpConnectionManager.startReaperThread(); // comment out to reproduce the CLOSE_WAIT
        
        // start the threads
        for (int j = 0; j < threads.length; j++) {
            threads[j].start();
        }

        /*
         * not sure how to automate this:
         * 
         * if ZimbraHttpConnectionManager.startReaperThread() was run:
         * after httpclient_connmgr_idle_reaper_sleep_interval,
         * netstat | grep CLOSE_WAIT | grep apache
         * should print nothing
         * 
         * if ZimbraHttpConnectionManager.startReaperThread() is *not* running:
         * netstat | grep CLOSE_WAIT | grep apache
         * will show:
         * tcp4       0      0  goodbyewhen-lm.c.62910 eos.apache.org.http    CLOSE_WAIT
         * tcp4       0      0  goodbyewhen-lm.c.62909 eos.apache.org.http    CLOSE_WAIT
         * tcp4       0      0  goodbyewhen-lm.c.62908 eris.apache.org.http   CLOSE_WAIT
         * tcp4       0      0  goodbyewhen-lm.c.62907 eos.apache.org.http    CLOSE_WAIT
         * 
         * for very long time.
         */
    }
    

    public void testHttpState() throws Exception {
        if (!runIt())
            return;
    }
    
    
    //**************************************************
    // A simple HTTP Server for testing
    //    
    // orig from http://www.devpapers.com/article/99
    //    
    //**************************************************

    private static class SimpleHttpServer implements Runnable {
        
        // server control vars
        private static Thread mServerThread = null;
        private static SimpleHttpServer sServer = null;
        
        // server vars
        private int mPort;
        private ServerSocket mServerSocket;
        private boolean mShutdownRequested = false;
        
        private synchronized static void start(int port) {
            if (mServerThread != null) {
                System.out.println("SimpleHttpServer start: server already started");
                return;
            }
            
            sServer = new SimpleHttpServer(port);
            mServerThread = new Thread(sServer);
            mServerThread.start();
        }
        
        private synchronized static void shutdown() {
            if (mServerThread == null) {
                System.out.println("SimpleHttpServer shutdown: server is not running");
                return;
            }
            
            sServer.requestShutdown();
            // mServerThread.interrupt();  It turns out that the ServerSocket.accept() is not interruptible.  Just use the close socket hack.
            mServerThread = null;
        }
        
        private SimpleHttpServer(int port) {
            mPort = port;
        }
        
        private synchronized void requestShutdown() {
            mShutdownRequested = true;
            try {
                mServerSocket.close();
            } catch (IOException e) {
                // just what we expect
            }
        }
        
        public void run() {
            try {
                //print out the port number for user
                mServerSocket = new ServerSocket(mPort);
                System.out.println("SimpleHttpServer: started on port " + mServerSocket.getLocalPort());
                
                // server infinite loop
                while(true && !mShutdownRequested) {
                    Socket socket = mServerSocket.accept();
                    System.out.println("SimpleHttpServer: new connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
                    
                    // Construct handler to process the HTTP request message.
                    try {
                        HttpRequestHandler request = new HttpRequestHandler(socket);
                        // Create a new thread to process the request.
                        Thread thread = new Thread(request);
                        // Start the thread.
                        thread.start();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                 }
            } catch (IOException e) {
                if (!mShutdownRequested)
                    e.printStackTrace();
                // else, the IOException is expected
            } finally {
                System.out.println("SimpleHttpServer: existing server");
            }
        }
    }

    private static class HttpRequestHandler implements Runnable {
        final static String CRLF = "\r\n";
        Socket socket;
        InputStream input;
        OutputStream output;
        BufferedReader br;
    
        // Constructor
        private HttpRequestHandler(Socket socket) throws Exception {
            this.socket = socket;
            this.input = socket.getInputStream();
            this.output = socket.getOutputStream();
            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        
        // Implement the run() method of the Runnable interface.
        public void run() {
            try {
                processRequest();
            } catch(Exception e) {
                System.out.println(e);
            }
        }
        
        private void processRequest() throws Exception {
            while(true) {
                String headerLine = br.readLine();
                System.out.println(headerLine);
                if(headerLine.equals(CRLF) || headerLine.equals("")) break;
                
                StringTokenizer s = new StringTokenizer(headerLine);
                String temp = s.nextToken();
                
                if (temp.equals("GET")) {
            
                    String fileName = s.nextToken();
                    // fileName = "." + fileName ;
                
                    // Open the requested file.
                    FileInputStream fis = null ;
                    boolean fileExists = true ;
                    try {
                        fis = new FileInputStream(fileName) ;
                    } catch ( FileNotFoundException e) {
                        fileExists = false ;
                    }
            
                    // Construct the response message.
                    String serverLine = "Server: fpont simple java httpServer" + CRLF;
                    String statusLine = null;
                    String contentTypeLine = null;
                    String entityBody = null;
                    String contentLengthLine = null;
                    if (fileExists) {
                        statusLine = "HTTP/1.0 200 OK" + CRLF ;
                        contentTypeLine = "Content-type: " + "text/plain" + CRLF;
                        contentLengthLine = "Content-Length: "
                             + (new Integer(fis.available())).toString()
                             + CRLF;
                    } else {
                        statusLine = "HTTP/1.0 404 Not Found" + CRLF ;
                        contentTypeLine = "Content-type: " + "text/html" + CRLF;
                        entityBody = "<HTML>" +
                            "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
                            "<BODY>404 Not Found"
                            +"<br>usage:http://www.snaip.com:4444/"
                            +"fileName.html</BODY></HTML>" ;
                        
                        contentLengthLine = "Content-Length: " + entityBody.length() + CRLF;
                    }
            
                    // Send the status line.
                    output.write(statusLine.getBytes());
                    
                    // Send the server line.
                    output.write(serverLine.getBytes());
                    
                    // Send the content type line.
                    output.write(contentTypeLine.getBytes());
                    
                    // Send the Content-Length
                    output.write(contentLengthLine.getBytes());
                    
                    // Send a blank line to indicate the end of the header lines.
                    output.write(CRLF.getBytes());
            
                    // Send the entity body.
                    if (fileExists) {
                        sendBytes(fis, output) ;
                        fis.close();
                    } else {
                        output.write(entityBody.getBytes());
                    }
                    
                    // our thread only process one request  :)
                    break;
                }
            }
                    
            try {
                output.close();
                br.close();
                socket.close();
            } catch(Exception e) {}
        }
        
        private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
            // Construct a 1K buffer to hold bytes on their way to the socket.
            byte[] buffer = new byte[1024] ;
            int bytes = 0 ;
            
            // Copy requested file into the socket's output stream.
            while ((bytes = fis.read(buffer)) != -1 ) {
                os.write(buffer, 0, bytes);
                
                // wait a little so the client will timeout
                System.out.println("Server is hanging for 60 seconds...");
                Thread.sleep(60000);
            }
        }
    }

    public void testSoTimeoutViaHttpMethod() throws Exception {
        if (!runIt())
            return;
        
        int serverPort = 7778;
        String path = "/Users/pshao/p4/main/ZimbraServer/src/java/com/zimbra/qa/unittest/TestZimbraHttpConnectionManager.java";  // this file
        int soTimeout = 3000;  // 3 seconds
        
        // start a server for testing
        SimpleHttpServer.start(serverPort);
        
        HttpClient httpClient = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();

        GetMethod method = new GetMethod("http://localhost:" + serverPort + path);
        method.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, Integer.valueOf(soTimeout));
        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            int respCode = httpClient.executeMethod(method);
            dumpResponse(respCode, method, "");
        } catch (java.net.SocketTimeoutException e) {
            // just what we want
            endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("Timed out after " + elapsedTime + " msecs");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            method.releaseConnection();
        }
        
        // shutdown the server
        SimpleHttpServer.shutdown();
    }
    
    private static void runSoapProv(String msg) {
        System.out.println(msg);
        SoapProvisioning sp = new SoapProvisioning();
        String uri = LC.zimbra_admin_service_scheme.value() + 
                     LC.zimbra_zmprov_default_soap_server.value() + ":" +
                     LC.zimbra_admin_service_port.intValue() + 
                     ZimbraServlet.ADMIN_SERVICE_URI;
        sp.soapSetURI(uri);
        try {
            sp.getDomainInfo(DomainBy.name, "phoebe.mac");
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    
    private static class SoapProvThread extends Thread {
        
        private String mId;
        
        public SoapProvThread(String id) {
            mId = id;
        }
        
        public void run() {
            runSoapProv(mId);
            try {
                // wait 1 hour
                Thread.sleep(3600000);
            } catch (InterruptedException e) {
            }
        }
    }
    
    private void runSoapProvParallel(int num) {
        for (int i = 0; i < num; i++) {
            SoapProvThread thread = new SoapProvThread(Integer.valueOf(i).toString());
            thread.start();
        }
    }
    
    private void runSoapProvSerial(int num) {
        for (int i = 0; i < num; i++) {
            runSoapProv(Integer.valueOf(i).toString());
        }
    }
    
    public void testSoapProv() throws Exception {
        if (!runIt())
            return;
        
        // runSoapProvSerial(3);
        runSoapProvParallel(3);
        
    }
    
    public static void main(String[] args) throws Exception {
        // TestUtil.cliSetup();  uncomment will default to SoapProvisioning
        CliUtil.toolSetup("INFO");
        TestUtil.runTest(TestZimbraHttpConnectionManager.class);

        // sleep for a long time
        System.out.println("Waiting...");
        Thread.sleep(Constants.MILLIS_PER_DAY);
    }
}
