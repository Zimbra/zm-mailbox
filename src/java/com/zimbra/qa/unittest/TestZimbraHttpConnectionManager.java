package com.zimbra.qa.unittest;

/*
 * for SimpleHttpServer
 */
import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraHttpConnectionManager.ZimbraHttpClient;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.servlet.ZimbraServlet;

import junit.framework.TestCase;

public class TestZimbraHttpConnectionManager extends TestCase {
    
    // hack before I figure out how to run only the selected tests in the unit test framework
    enum Test {
        testBasic(false),
        testReaperOld(false),
        testReaper(true),
        testDisabledMethods(false),
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
        
        private ZimbraHttpClient httpClient;
        private GetMethod method;
        private int id;
        
        public TestGetThread(ZimbraHttpClient httpClient, GetMethod method, int id) {
            this.httpClient = httpClient;
            this.method = method;
            this.id = id;
        }
        
        /**
         * Executes the GetMethod and prints some satus information.
         */
        public void run() {
            try {
                System.out.println(id + " - about to get something from " + method.getURI());
                // execute the method
                int respCode = httpClient.executeMethod(method);
                
                System.out.println(id + " - get executed");
                // get the response body as an array of bytes
                // byte[] bytes = method.getResponseBody();
                dumpResponse(respCode, method, Integer.valueOf(id).toString());
                
            } catch (Exception e) {
                System.out.println(id + " - error: " + e);
            } finally {
                // always release the connection after we're done 
                method.releaseConnection();
                System.out.println(id + " - connection released");
            }
        }
    }
    
    public void testBasic() {
        if (!runIt())
            return;
        
        // create an array of URIs to perform GETs on
        String[] urisToGet = {
            "http://hc.apache.org:80/",
            "http://hc.apache.org:80/httpclient-3.x/status.html",
            "http://hc.apache.org:80/httpclient-3.x/methods/",
            "http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/"
        };
        
        // create a thread for each URI
        TestGetThread[] threads = new TestGetThread[urisToGet.length];
        for (int i = 0; i < threads.length; i++) {
            GetMethod get = new GetMethod(urisToGet[i]);
            get.setFollowRedirects(true);
            threads[i] = new TestGetThread(ZimbraHttpConnectionManager.getHttpClient(), get, i + 1);
        }
        
        // start the threads
        for (int j = 0; j < threads.length; j++) {
            threads[j].start();
        }
    }
    
/*    
 * set in localconfig.xml before running this test

  <key name="httpclient_connmgr_idle_connection_reaper_initial_sleep_time">
    <value>3000</value>
  </key>

  <key name="httpclient_connmgr_idle_connection_reaper_sleep_interval">
    <value>5000</value>
  </key>
  
*/  
    public void testReaperOld() throws Exception {
        if (!runIt())
            return;
        
        // ZimbraHttpConnectionManager.startReaperThread();
        
        ZimbraHttpClient httpClient = ZimbraHttpConnectionManager.getHttpClient();
        GetMethod method = new GetMethod("http://hc.apache.org:80/");
        int numConnInPool;
        try {
            numConnInPool = ZimbraHttpConnectionManager.getConnectionsInPool();
            assertEquals(0, numConnInPool);
            httpClient.executeMethod(method);
            numConnInPool = ZimbraHttpConnectionManager.getConnectionsInPool();
            assertEquals(1, numConnInPool);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            method.releaseConnection();
            numConnInPool = ZimbraHttpConnectionManager.getConnectionsInPool();
            assertEquals(1, numConnInPool);
        }
        
        // wait for the reaper to close the idle connection
        Thread.sleep(5000);
        numConnInPool = ZimbraHttpConnectionManager.getConnectionsInPool();
        assertEquals(0, numConnInPool);
        
        // ZimbraHttpConnectionManager.shutdownReaperThread();
    }
    
    public void doTestReaper(String msg) throws Exception {
       String url = "http://localhost:7070/service/preauth?account=bogus@phoebe.mac&by=name";
        ZimbraHttpClient httpClient = ZimbraHttpConnectionManager.getHttpClient();
        GetMethod method = new GetMethod(url);
        int numConnInPool;
        try {
            int respCode = httpClient.executeMethod(method);
            dumpResponse(respCode, method, msg);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            method.releaseConnection();
        }
    }
    
    
    public void testReaper() throws Exception {
        if (!runIt())
            return;
        
        int num = 1;
        
        ZimbraHttpConnectionManager.startReaperThread();
        for (int i = 0; i < num; i++) {
            doTestReaper(Integer.valueOf(i).toString());
        }
        
        // wait for the reaper to close the idle connection
        // Thread.sleep(60000);
        
        // ZimbraHttpConnectionManager.shutdownReaperThread();
    }
    
    public void testDisabledMethods() throws Exception {
        if (!runIt())
            return;
        
        ZimbraHttpClient httpClient = ZimbraHttpConnectionManager.getHttpClient();
        
        boolean good = false;
        try {
            httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        } catch (ServiceException e) {
            good = true;
        }
        assertTrue(good);
        
        good = false;
        try {
            httpClient.setConnectionTimeout(5000);
        } catch (ServiceException e) {
            good = true;
        }
        assertTrue(good);
        
        good = false;
        try {
            httpClient.setHttpConnectionManager(new SimpleHttpConnectionManager());
        } catch (ServiceException e) {
            good = true;
        }
        assertTrue(good);
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
                    
                    // our thread only process ine request  :)
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
        
        ZimbraHttpClient httpClient = ZimbraHttpConnectionManager.getHttpClient();

        GetMethod method = new GetMethod("http://localhost:" + serverPort + path);
        try {
            method.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, Integer.valueOf(soTimeout));
            int respCode = httpClient.executeMethod(method);
            dumpResponse(respCode, method, "");
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
        
        System.out.println("hold");
    }
}
