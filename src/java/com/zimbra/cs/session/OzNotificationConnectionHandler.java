package com.zimbra.cs.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.HttpRequest;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.ozserver.OzByteArrayMatcher;
import com.zimbra.cs.ozserver.OzByteBufferGatherer;
import com.zimbra.cs.ozserver.OzConnection;
import com.zimbra.cs.ozserver.OzConnectionHandler;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.SoapProtocol;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author tim
 * 
 * Handle a connection on the NOTIFY port.  
 *
 */
public class OzNotificationConnectionHandler implements OzConnectionHandler 
{
    private final int MAX_HTTPHEADER_BYTES = 32768;
    private final OzConnection mConnection;
    
    private OzByteArrayMatcher mHTTPHeaderMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLFCRLF, ZimbraLog.imap);
    
    private OzByteBufferGatherer mCurrentData;
    
    private AuthToken mAuthToken = null;
    
    private ZimbraSoapContext mContext = null;
    
    private int mLastKnownSeqNo; // the highest seq no the client is known to have received
    public int getLastKnownSeqNo() { return mLastKnownSeqNo; }
    
    public ZimbraSoapContext getZimbraContext() { return mContext; }
    
    public OzNotificationConnectionHandler(OzConnection connection) {
        mConnection = connection;
    }
    
    public void sendOverflowed() throws IOException {
        mConnection.writeAsciiWithCRLF("OVERFLOW reading HTTP header");
    }
    
    private void writeAsciiWithCRLF(String str) throws IOException {
        mConnection.writeAsciiWithCRLF(str);
    }
    
    private static File getJSFile() {
        return sJSFile;
    }
    
    private static byte[] readFile(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            
            byte[] toRet = new byte[in.available()];
            
            in.read(toRet);
            
            return toRet;
            
        } catch (Exception e) {
            System.out.println("Exception: "+e+" initializing OzNotificationConnectionHandler");
            e.printStackTrace();
            try {
                return new String("<BODY>Could not load "+f.toString()+"</BODY>").getBytes("UTF8");
            } catch(Exception ex) {
                // bite me java
                return new String("<BODY>Could not load "+f.toString()+"</BODY>").getBytes();
            }
        }
    }
    
    private static File sJSFile;
    private static byte[] sJSFileBytes;
    private static long sJSFileLastMod;
    
    
    static {
        sJSFile = new File(LC.zimbra_home.value()+File.separator+"conf"+File.separator+"notify.html");
        File JSFile = getJSFile();
        sJSFileBytes = readFile(JSFile);
        sJSFileLastMod = JSFile.lastModified();
    }
    
    private synchronized static byte[] getJSFileBytes() {
        File JSFile = getJSFile();
        if (JSFile.lastModified() > sJSFileLastMod) {
            sJSFileBytes = readFile(JSFile);
        }
        return sJSFileBytes;
    }
    
    private static final String HTTP_PROTOCOL = "HTTP/1.1";
    
    private void putResponseHeader(int code, String str, String contentType) throws IOException
    {
        StringBuilder hdr = new StringBuilder();
        Formatter fmt = new Formatter();
        hdr.append(fmt.format("%s %d %s\r\n", HTTP_PROTOCOL, code, str));
        hdr.append("Server: Zimbra Collaboration Server\r\n");
        hdr.append("Content-Type: "+contentType+";charset=utf-8\r\n");
        try {
            byte[] bytes = hdr.toString().getBytes("UTF8");
            mConnection.write(ByteBuffer.wrap(bytes));
        } catch (UnsupportedEncodingException e) {
            System.out.println("foo "+e);
            e.printStackTrace();
        }
        
    }
    
    public void writeHttpResponse(int code, String status, String contentType, String data) throws IOException {
        byte[] bytes = data.getBytes("UTF8");
        writeHttpResponse(code, status, contentType, bytes);
    }
    
    public void writeHttpResponse(int code, String status, String contentType, byte[] data) throws IOException {
        putResponseHeader(code, status, contentType);
        
        int length = data.length;

        mConnection.writeAsciiWithCRLF("Content-Length: "+Integer.toString(length)+"\r\n");
        mConnection.write(ByteBuffer.wrap(data));
        mConnection.close();
    }
    
    
    public void close() {
        mConnection.close();
    }

    public void handleConnect() throws IOException {
        mHTTPHeaderMatcher.reset();
        mConnection.setMatcher(mHTTPHeaderMatcher);
        mConnection.enableReadInterest();
        mCurrentData = new OzByteBufferGatherer(256, MAX_HTTPHEADER_BYTES);
        
        if (ZimbraLog.mailbox.isDebugEnabled()) 
            ZimbraLog.mailbox.debug("Notification Handler got new connection: "+mConnection.getRemoteAddress().toString());
    }
    
    public void handleInput(ByteBuffer buffer, boolean matched) throws IOException {
        mCurrentData.add(buffer);
        if (!matched) 
            return;

        if (mCurrentData.overflowed()) {
            sendOverflowed();
            return;
        }
        
        // at this point we know we've received CRLFCRLF -- so we have the entire
        // HTTP header, which is all we care about.  Parse the HTTP header and look for
        // the following parameters:
        //
        // ZimbraAuthToken (or URL param "a") -- encoded auth token
        // ZimbraSession (or URL param "session") -- ID of session
        // url param "seq" -- sequence no of last notification received by this client
        //
        // 
        // All three parameters are required, if any is missing then the server 
        // assumes the client does not have the correct javascript code to make the
        // proper request, and so it returns the javascript page 
        // 
        //
        // 
        // 
        
        
        String sessionId;
        String authToken;
        int seqNo = 0;
        
        HttpRequest request = new HttpRequest(mCurrentData.getInputStream());
        System.out.println(request.toString());
        
        Map uriParams = request.getRequestLine().getUriParams();
        
        sessionId = (String)uriParams.get("session");
        authToken = (String)uriParams.get("a");
        String seqNoStr = (String)uriParams.get("seq");
        if (seqNoStr != null) 
            seqNo = Integer.parseInt(seqNoStr);
        
        if (seqNo < 0) {
            writeAsciiWithCRLF("Invalid sequence number");
            mConnection.close();
            return;
        }
        
        if (sessionId == null) {
            sessionId = (String)request.getHeaders().get("ZimbraSession");
        }
        if (authToken == null) {
            authToken = (String)request.getHeaders().get("ZimbraAuthToken");
        }
        
        //
        // They didn't give us a session, authtoken, or seqNo -- therefore they
        // must not have the javascript yet: give it to them
        //
        if (sessionId == null && authToken == null && seqNoStr == null) {
            byte[] js = getJSFileBytes();
            writeHttpResponse(200, "OK", "text/html", js);
            return;
        }
        
        if (sessionId == null || authToken == null) {
            writeHttpResponse(400, "Bad Request", "text/html", "Missing ZimbraSession or ZimbraAuthToken");
            mConnection.close();
            return;
        }
        
        try {
            mAuthToken = AuthToken.getAuthToken(authToken);
            
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                ZimbraLog.mailbox.debug("NOTIFY: acct="+mAuthToken.getAccountId()+" session="+sessionId);
            }
        } catch(Exception e) {
            writeAsciiWithCRLF("Invalid Auth Token");
            mConnection.close();
            return;
        }
        
        try {
            Map ctxt = new HashMap<String, String>();
            ctxt.put(SoapServlet.ZIMBRA_AUTH_TOKEN, authToken);
            
            mContext = new ZimbraSoapContext(null, ctxt, SoapProtocol.Soap12);
        } catch(ServiceException e) {
            ZimbraLog.mailbox.info("Could not fetch ZimbraContext: acct="+mAuthToken.getAccountId()+" session="+sessionId);
            writeAsciiWithCRLF("Error getting ZimbraContext: "+e);
            mConnection.close();
        }
        
        Session s = SessionCache.lookup(sessionId, mAuthToken.getAccountId());
        
        if (s != null) {
            ZimbraLog.mailbox.info("Found Session: "+s.toString());
            try {
                mLastKnownSeqNo = seqNo;
                Session.RegisterNotificationResult result = s.registerNotificationConnection(this);
                if (result != Session.RegisterNotificationResult.WAITING) {
                    mConnection.close();
                }
            } catch (ServiceException e) {
                writeAsciiWithCRLF("Caught exception: "+e.toString());
                mConnection.close();
                return;
            }
        } else {
            ZimbraLog.mailbox.info("NOTIFY COULD NOT FIND REQUESTED SESSION: acct="+mAuthToken.getAccountId()+" session="+sessionId);
            writeAsciiWithCRLF("No such session");
            mConnection.close();
        }

    }

    public void handleDisconnect() {
        if (ZimbraLog.mailbox.isDebugEnabled()) 
            ZimbraLog.mailbox.debug("Notification Handler dropped connection: "+mConnection.getRemoteAddress().toString());
        
    }

    public void handleAlarm() throws IOException { }

}
