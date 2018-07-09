/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
/*
 * for SimpleHttpServer
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class TestZimbraHttpConnectionManager {
    
    @BeforeClass
    public static void init() throws Exception {
        CliUtil.toolSetup("INFO");
    }
    

    public static void dumpResponse(int respCode, HttpResponse response, String prefix) throws IOException {
        
        prefix = prefix + " - ";
        
        // status
        int statusCode =response.getStatusLine().getStatusCode();
        String statusLine = response.getStatusLine().toString();
        
        System.out.println(prefix + "respCode=" + respCode);
        System.out.println(prefix + "statusCode=" + statusCode);
        System.out.println(prefix + "statusLine=" + statusLine);
        
        // headers
        System.out.println(prefix + "Headers");
        Header[] respHeaders = response.getAllHeaders();

        for (int i=0; i < respHeaders.length; i++) {
            String header = respHeaders[i].toString();
            // trim the CRLF at the end to save space
            System.out.println(prefix + header.trim());
        }
        
        // body
        byte[] bytes = ByteUtil.getContent(response.getEntity().getContent(), 0);
        System.out.println(prefix + bytes.length + " bytes read");
        System.out.println(new String(bytes));
    }
    
    
    /**
     * A thread that performs a GET.
     */
    private static class TestGetThread extends Thread {
        
        private HttpClient mHttpClient;
        private HttpGet mMethod;
        private int mId;
        
        public TestGetThread(HttpClient httpClient, HttpGet method, int id) {
            mHttpClient = httpClient;
            mMethod = method;
            mId = id;
        }
        
        /**
         * Executes the HttpGet and prints some status information.
         */
        public void run() {
            long startTime = System.currentTimeMillis();
            long endTime;
            
            try {
                System.out.println(mId + " - about to get something from " + mMethod.getURI());
                // execute the method
                HttpResponse response = HttpClientUtil.executeMethod(mHttpClient, mMethod);
                int respCode = response.getStatusLine().getStatusCode();
                
                System.out.println(mId + " - get executed");
                // get the response body as an array of bytes
                // byte[] bytes = method.getResponseBody();
                // dumpResponse(respCode, mMethod, Integer.valueOf(mId).toString());
                
            } catch (Exception e) {
                System.out.println(mId + " - error: " + e);
                e.printStackTrace();
            } finally {
                
                endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                System.out.println("Finished, elapsedTime=" + elapsedTime + " milli seconds");
                
                // always release the connection after we're done 
                mMethod.releaseConnection();
                System.out.println(mId + " - connection released");
            }
        }
    }
    
    /* This test has to be run manually.
     *    
     * before running this test, do:
      
      zmlocalconfig -e httpclient_connmgr_idle_reaper_sleep_interval_external=10000
      zmlocalconfig -e httpclient_connmgr_idle_reaper_connection_timeout_external=2000
      
    */  
    // @Test
    public void testReaper() throws Exception {
        
        // create an array of URIs to perform GETs on
        String[] urisToGet = {
            "http://localhost:7070/zimbra/public/empty.html",
            "http://localhost:7071/service/admin/soap",
            /*
            "http://hc.apache.org:80/",
            "http://hc.apache.org:80/httpclient-3.x/status.html",
            "http://hc.apache.org:80/httpclient-3.x/methods/",
            "http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/"
            */
        };
        
        ZimbraHttpConnectionManager connMgr = ZimbraHttpConnectionManager.getExternalHttpConnMgr();
        
        // create a thread for each URI
        TestGetThread[] threads = new TestGetThread[urisToGet.length];
        for (int i = 0; i < threads.length; i++) {
            HttpGet get = new HttpGet(urisToGet[i]);
            HttpClientBuilder builder = connMgr.newHttpClient().setRedirectStrategy(new DefaultRedirectStrategy());
            threads[i] = new TestGetThread(builder.build(), get, i + 1);
        }
        
        ZimbraHttpConnectionManager.startReaperThread(); // comment out to reproduce the CLOSE_WAIT
        
        // start the threads
        for (int j = 0; j < threads.length; j++) {
            threads[j].start();
        }

        /*
         * This test has to be run manually.
         * 
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
        
        // wait, so we can watch the CLOSE_WAIT going away
        Thread.sleep(Constants.MILLIS_PER_HOUR);
    }
   
    
    private static class SimpleHttpServer implements Runnable {
        
        // server control vars
        private static Thread mServerThread = null;
        private static SimpleHttpServer sServer = null;
        
        // server vars
        private int mPort;
        private ServerSocket mServerSocket;
        private boolean mShutdownRequested = false;
        
        private enum DelayWhen {
            BEFORE_READING_REQ_LINE,
            BEFORE_READING_HEADERS,
            GET_BEFORE_FETCHING_RESOURCE,
            POST_BEFORE_READING_BODY,
            BEFORE_WRITING_RESPONSE_HEADERS,
            BEFORE_WRITING_RESPONSE_BODY,
            DURING_WRITING_RESPONSE_BODY,
            AFTER_WRITING_RESPONSE_BODY
        }
        
        private synchronized static void start(int port) {
            if (mServerThread != null) {
                log("start server: server already started");
                return;
            }
            
            log("starting server");
            sServer = new SimpleHttpServer(port);
            mServerThread = new Thread(sServer);
            mServerThread.start();
        }
        
        private synchronized static void shutdown() {
            if (mServerThread == null) {
                log("shutdown server: server is not running");
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
        
        private static void log(String msg) {
            System.out.println("*** SimpleHttpServer: " + msg);
        }
        
        public void run() {
            try {
                //print out the port number for user
                mServerSocket = new ServerSocket(mPort);
                log("started on port " + mServerSocket.getLocalPort());
                
                // server infinite loop
                while(true && !mShutdownRequested) {
                    Socket socket = mServerSocket.accept();
                    log("new connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
                    
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
                log("exiting server");
            }
        }
    }

    private static class HttpRequestHandler implements Runnable {
        
        final static String CRLF = "\r\n";
        
        private static final String SERVER_LINE = "Server: brain dead java httpServer" + CRLF;
        private static final String STATUS_LINE_200 = "HTTP/1.0 200 OK" + CRLF ;
        private static final String STATUS_LINE_404 = "HTTP/1.0 404 Not Found" + CRLF ;
        
        private static final String HEADER_CONTENT_LENGTH = "Content-Length";
        private static final String HEADER_CONTENT_TYPE = "Content-Type";
        
        Socket socket;
        InputStream input;
        OutputStream output;
        BufferedReader reader;
        
        private String url;
        private Map<String, String> headers = new HashMap<String, String>();
        private Map<String, String> queryParams = new HashMap<String, String>();
        
        
        private HttpRequestHandler(Socket socket) throws Exception {
            this.socket = socket;
            this.input = socket.getInputStream();
            this.output = socket.getOutputStream();
            this.reader = new BufferedReader(new InputStreamReader(input));
        }
        
        // Implement the run() method of the Runnable interface.
        public void run() {
            try {
                processRequest();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        private void getHeaders() throws Exception {
            delayIfRequested(SimpleHttpServer.DelayWhen.BEFORE_READING_HEADERS);
            
            String header;
            
            while (true) {
                header = reader.readLine();
                
                if (header.equals(CRLF) || header.equals("")) {
                    break;
                }
                
                String[] parts = header.split(":");
                headers.put(parts[0].trim(), parts[1].trim());
            }
          
            
        }
        private void parseRequestLine(String req) {
            String[] parts = req.split("\\?");
            url = parts[0];
            
            String qp = null;
            long waitInServer = 0;
            if (parts.length == 2) {
                qp = parts[1];
            
                String[] params = qp.split("&");
                for (String param : params) {
                    String[] nameValue = param.split("=");
                    queryParams.put(nameValue[0], nameValue[1]);
                }
            }
        }
        
        private void delayIfRequested(SimpleHttpServer.DelayWhen delayWhen) throws Exception {
            String delay = queryParams.get(delayWhen.name());
            if (delay != null) {
                int delayMilliSecs = Integer.valueOf(delay);
                if (delayMilliSecs != 0) {
                    SimpleHttpServer.log("Delaying " + delay + " milli seconds: " + delayWhen.name());
                    Thread.sleep(delayMilliSecs);
                }
            }
        }
        
        private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
            // Construct a 1K buffer to hold bytes on their way to the socket.
            byte[] buffer = new byte[1024] ;
            int bytes = 0 ;
            
            // Copy requested file into the socket's output stream.
            while ((bytes = fis.read(buffer)) != -1 ) {
                os.write(buffer, 0, bytes);
            }
        }
        
        private void doGet() throws Exception {
           
            delayIfRequested(SimpleHttpServer.DelayWhen.GET_BEFORE_FETCHING_RESOURCE);
        
            // Open the requested file.
            String fileName = url;
            FileInputStream fis = null ;
            boolean fileExists = true ;
            try {
                fis = new FileInputStream(fileName) ;
            } catch ( FileNotFoundException e) {
                fileExists = false ;
            }
            
            String statusLine = null;
            String contentTypeLine = null;
            String entityBody = null;
            String contentLengthLine = null;
            if (fileExists) {
                statusLine = STATUS_LINE_200;
                contentTypeLine = HEADER_CONTENT_TYPE + ": " + "text/plain" + CRLF;
                contentLengthLine = HEADER_CONTENT_LENGTH + ": " + 
                    (new Integer(fis.available())).toString() + CRLF;
            } else {
                statusLine = STATUS_LINE_404;
                contentTypeLine = HEADER_CONTENT_TYPE + ": " + "text/html" + CRLF;
                entityBody = "<HTML>" + "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
                    "<BODY>404 Not Found" + "<br>File " + fileName + "</BODY></HTML>" ;
                
                contentLengthLine = HEADER_CONTENT_LENGTH + ": " +  + entityBody.length() + CRLF;
            }
            
            delayIfRequested(SimpleHttpServer.DelayWhen.BEFORE_WRITING_RESPONSE_HEADERS);
    
            // Send the status line.
            output.write(statusLine.getBytes());
            
            // Send the server line.
            output.write(SERVER_LINE.getBytes());
            
            // Send the content type line.
            output.write(contentTypeLine.getBytes());
            
            // Send the Content-Length
            output.write(contentLengthLine.getBytes());
            
            // Send a blank line to indicate the end of the header lines.
            output.write(CRLF.getBytes());
            
            delayIfRequested(SimpleHttpServer.DelayWhen.BEFORE_WRITING_RESPONSE_BODY);
            
            // Send the entity body.
            if (fileExists) {
                sendBytes(fis, output) ;
                fis.close();
            } else {
                output.write(entityBody.getBytes());
            }
            
            delayIfRequested(SimpleHttpServer.DelayWhen.AFTER_WRITING_RESPONSE_BODY);
        }
        
        private void doPost() throws Exception {
            
            delayIfRequested(SimpleHttpServer.DelayWhen.POST_BEFORE_READING_BODY);
            
            int contentLength = Integer.valueOf(headers.get(HEADER_CONTENT_LENGTH));
            int firstHalfLen = contentLength / 2;
            int secondHalfLen = contentLength - firstHalfLen;
            
            byte[] bodyFirstHalf = new byte[firstHalfLen];
            input.read(bodyFirstHalf);
            
            // wait while reading
            // waitIfRequested();
            
            byte[] bodySecondHalf = new byte[secondHalfLen];
            input.read(bodySecondHalf);
            
            String entityBody = "all is well!";
            String contentTypeLine = HEADER_CONTENT_TYPE + ": " + "text/plain" + CRLF;
            String contentLengthLine = HEADER_CONTENT_LENGTH + ": " + entityBody.getBytes().length + CRLF;
                
            delayIfRequested(SimpleHttpServer.DelayWhen.BEFORE_WRITING_RESPONSE_HEADERS);
            
            // Send the status line.
            output.write(STATUS_LINE_200.getBytes());
            
            // Send the server line.
            output.write(SERVER_LINE.getBytes());
            
            // Send the content type line.
            output.write(contentTypeLine.getBytes());
            
            // Send the Content-Length
            output.write(contentLengthLine.getBytes());
            
            // Send a blank line to indicate the end of the header lines.
            output.write(CRLF.getBytes());
            
            delayIfRequested(SimpleHttpServer.DelayWhen.BEFORE_WRITING_RESPONSE_BODY);
            
            // Send the entity body.
            output.write(entityBody.getBytes());
            
            delayIfRequested(SimpleHttpServer.DelayWhen.AFTER_WRITING_RESPONSE_BODY);
        }
        
        private void processRequest() throws Exception {
            
            while(true) {
                delayIfRequested(SimpleHttpServer.DelayWhen.BEFORE_READING_REQ_LINE);
                String reqLine = reader.readLine();
                SimpleHttpServer.log(reqLine);
                if (reqLine.equals(CRLF) || reqLine.equals("")) {
                    break;
                }
                
                getHeaders();
                
                StringTokenizer s = new StringTokenizer(reqLine);
                String method = s.nextToken();
                
                String req = s.nextToken();
                parseRequestLine(req);
                
                if (method.equals("GET")) {
                    doGet();
                } else if (method.equals("POST")) {
                    doPost();
                }
             
                // our thread only process one request  :)
                break;
            }
                    
            try {
                output.close();
                reader.close();
                socket.close();
            } catch(Exception e) {}
        }

    }

    // @Test
    public void testSoTimeoutViaHttpMethod() throws Exception {
        
        int serverPort = 7778;
        String resourceToGet = "/opt/zimbra/unittest/rights-unittest.xml";
        long delayInServer = 10000;  // delay 10 seconds in server
        int soTimeout = 3000;  // 3 seconds
        
        String qp = "?" + SimpleHttpServer.DelayWhen.BEFORE_WRITING_RESPONSE_HEADERS.name() + "=" + delayInServer;
        String uri = "http://localhost:" + serverPort + resourceToGet + qp;
        
        // start a http server for testing
        SimpleHttpServer.start(serverPort);
        
        // HttpClient httpClient = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        
        HttpGet method = new HttpGet(uri);
        
        SocketConfig config = SocketConfig.custom().setSoTimeout(soTimeout).build();
        clientBuilder.setDefaultSocketConfig(config);
        HttpClient httpClient = clientBuilder.build();
        
        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            HttpResponse response = HttpClientUtil.executeMethod(httpClient, method);
            int respCode  = response.getStatusLine().getStatusCode();
            dumpResponse(respCode, response, "");
            Assert.fail(); // nope, it should have timed out
        } catch (java.net.SocketTimeoutException e) {
            // good, just what we want
            endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("Client timed out after " + elapsedTime + " msecs");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            method.releaseConnection();
        }
        
        // shutdown the server
        SimpleHttpServer.shutdown();
    }
    
    /*
     * Before running this test:
     * 
     * zmlocalconfig -e httpclient_connmgr_so_timeout_external=3000  (3 seconds)
     */
    // @Test
    public void testSoTimeoutViaConnMgrParam() throws Exception {
        
        int serverPort = 7778;
        String resourceToGet = "/opt/zimbra/unittest/rights-unittest.xml";
        long delayInServer = 10000;  // delay 10 seconds in server
        int soTimeout = 3000;  // 3 seconds
        
        String qp = "?" + SimpleHttpServer.DelayWhen.BEFORE_WRITING_RESPONSE_HEADERS.name() + "=" + delayInServer;
        String uri = "http://localhost:" + serverPort + resourceToGet + qp;
        
        // start a http server for testing
        SimpleHttpServer.start(serverPort);
        
        HttpClient httpClient = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient().build();
        
        HttpGet method = new HttpGet(uri);
        
        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            HttpResponse response = HttpClientUtil.executeMethod(httpClient, method);
            int respCode = response.getStatusLine().getStatusCode();
            
            dumpResponse(respCode, response, "");
            Assert.fail(); // nope, it should have timed out
        } catch (java.net.SocketTimeoutException e) {
            // good, just what we want
            endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("Client timed out after " + elapsedTime + " msecs");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            method.releaseConnection();
        }
        
        // shutdown the server
        SimpleHttpServer.shutdown();
    }
    
    // @Test
    public void testSoTimeoutViaHttpPostMethod() throws Exception {
        
        int serverPort = 7778;
        String resourceToPost = "/opt/zimbra/unittest/rights-unittest.xml";
        long delayInServer = 100000;  // delay 10 seconds in server
        int soTimeout = 60000; // 3000;  // 3 seconds, 0 for infinite wait
            
        String qp = "?" + SimpleHttpServer.DelayWhen.BEFORE_WRITING_RESPONSE_HEADERS.name() + "=" + delayInServer;
        String uri = "http://localhost:" + serverPort + resourceToPost + qp;
        
        // start a http server for testing
        SimpleHttpServer.start(serverPort);

        // post the exported content to the target server
        HttpClientBuilder httpClientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        HttpPost method = new HttpPost(uri);
        SocketConfig config = SocketConfig.custom().setSoTimeout(soTimeout).build();
        httpClientBuilder.setDefaultSocketConfig(config);
         // infinite wait because it can take a long time to import a large mailbox
        HttpClient httpClient = httpClientBuilder.build();
        File file = new File(resourceToPost);
        FileInputStream fis = null;
        
        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            fis = new FileInputStream(file);
            InputStreamEntity isre =
                new InputStreamEntity(fis, file.length(), org.apache.http.entity.ContentType.create(MimeConstants.CT_APPLICATION_OCTET_STREAM));
            method.setEntity(isre);
            HttpResponse httpResp = httpClient.execute(method);
            int respCode = httpResp.getStatusLine().getStatusCode();
            dumpResponse(respCode, httpResp, "");
            Assert.fail(); // nope, it should have timed out
        } catch (java.net.SocketTimeoutException e) {
            e.printStackTrace();
            // good, just what we want
            endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("Client timed out after " + elapsedTime + " msecs");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            method.releaseConnection();
        }
        
        // shutdown the server
        SimpleHttpServer.shutdown();
    }
    
    
    /*
     * before running this test:
     * 
     * zmlocalconfig -e httpclient_connmgr_max_total_connections_external=1  // the connection manager can only hand out one connection
     * zmlocalconfig -e httpclient_client_connection_timeout_external=5000   // time to get a connection from the connection manager
     */
    // @Test
    public void testHttpClientConnectionManagerTimeout() throws Exception {
        
        int serverPort = 7778;
        String path = "/Users/pshao/p4/main/ZimbraServer/src/java/com/zimbra/qa/unittest/TestZimbraHttpConnectionManager.java";  // this file
        long delayInServer = 10000;
        String qp = "?" + SimpleHttpServer.DelayWhen.GET_BEFORE_FETCHING_RESOURCE.name() + "=" + delayInServer;
        
        // start a server for testing
        SimpleHttpServer.start(serverPort);
        
        ZimbraHttpConnectionManager connMgr = ZimbraHttpConnectionManager.getExternalHttpConnMgr();
        
        // first thread
        HttpGet method1 = new HttpGet("http://localhost:" + serverPort + path + qp);
        TestGetThread thread1 = new TestGetThread(connMgr.newHttpClient().build(), method1, 1);
        thread1.start();  // this thread will hog the only one connection this conn mgr can offer for 10 seconds
        
        Thread.sleep(1000); // wait one second let thread one get a head start to grab the one and only connection
        
        // second thread
        HttpGet method2 = new HttpGet("http://localhost:" + serverPort + path + qp);
        TestGetThread thread2 = new TestGetThread(connMgr.newHttpClient().build(), method1, 2);
        thread2.start(); // this thread should timeout (ConnectionPoolTimeoutException) after 5000 milli seconds, 
                         // because zmlocalconfig -e httpclient_client_connection_timeout_external=5000 
        
        Thread.sleep(60000); // wait a little so we can observe the threads run
        
        // shutdown the server
        SimpleHttpServer.shutdown();
    }
    
    private static void runSoapProv(String msg) {
        System.out.println(msg);
        SoapProvisioning sp = new SoapProvisioning();
        String uri = LC.zimbra_admin_service_scheme.value() + 
                     LC.zimbra_zmprov_default_soap_server.value() + ":" +
                     LC.zimbra_admin_service_port.intValue() + 
                     AdminConstants.ADMIN_SERVICE_URI;
        sp.soapSetURI(uri);
        try {
            sp.getDomainInfo(Key.DomainBy.name, "phoebe.mac");
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
    
    // Test
    public void testSoapProv() throws Exception {
        // runSoapProvSerial(3);
        runSoapProvParallel(3);
        
    }
    
    private void runTest(HttpClientBuilder httpClientBuilder, String id, boolean authPreemp) {
        
        HttpGet method = new HttpGet("http://localhost:7070/");
        HttpClient httpClient = httpClientBuilder.build();
        long startTime = System.currentTimeMillis();
        long endTime;
        
        try {
            System.out.println(id + " - about to get something from " + method.getURI());
            // execute the method
            
//            if (authPreemp) {
//                httpClient.getParams().setParameter(arg0, arg1)
//                //.setAuthenticationPreemptive(true);
//            }
            
            HttpResponse reponse = HttpClientUtil.executeMethod(httpClient, method);
            
            System.out.println(id + " - get executed");
            // get the response body as an array of bytes
            // byte[] bytes = method.getResponseBody();
            // dumpResponse(respCode, mMethod, Integer.valueOf(mId).toString());
            
        } catch (Exception e) {
            System.out.println(id + " - error: " + e);
            e.printStackTrace();
        } finally {
            
            endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            // System.out.println(id + " - Finished, elapsedTime=" + elapsedTime + " milli seconds");
            
            // always release the connection after we're done 
            method.releaseConnection();
            System.out.println(id + " - connection released");
        }
    }
    
    // bug 47488
    // @Test
    public void testAuthenticationPreemptive() throws Exception {

        ZimbraHttpConnectionManager.startReaperThread();
        
        for (int i = 0; i < 10; i++) {
            // runTest(new HttpClient(), "PLAIN"+i, true);
            runTest(ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient(), "EXT-authPreemp"+i, true);
            runTest(ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient(), "EXT"+i, false);
            runTest(ZimbraHttpConnectionManager.getInternalHttpConnMgr().getDefaultHttpClient(), "INT"+i, false);
        }
    }

    @Test
    public void junk() throws Exception {
        
        String uri = "http://phoebe.mbp:7070/service/soap/AuthRequest";
        
        HttpClient httpClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient().build();
        
        HttpGet method = new HttpGet(uri);
        
        try {
            HttpResponse response = HttpClientUtil.executeMethod(httpClient, method);
            int respCode = response.getStatusLine().getStatusCode();
            dumpResponse(respCode, response, "");
            Assert.fail(); // nope, it should have timed out
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        } finally {
            method.releaseConnection();
        }

    }

}
