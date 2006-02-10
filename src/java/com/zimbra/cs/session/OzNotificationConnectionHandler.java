package com.zimbra.cs.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.HttpRequest;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.ozserver.OzByteArrayMatcher;
import com.zimbra.cs.ozserver.OzByteBufferGatherer;
import com.zimbra.cs.ozserver.OzConnection;
import com.zimbra.cs.ozserver.OzConnectionHandler;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.SoapProtocol;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraContext;

public class OzNotificationConnectionHandler implements OzConnectionHandler 
{
    private final int MAX_HTTPHEADER_BYTES = 32768;
    private final OzConnection mConnection;
    
    private OzByteArrayMatcher mHTTPHeaderMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLFCRLF, ZimbraLog.imap);
    
    private OzByteBufferGatherer mCurrentData;
    
    private AuthToken mAuthToken = null;
    
    private ZimbraContext mContext = null;
    
    private int mLastKnownSeqNo; // the highest seq no the client is known to have received
    public int getLastKnownSeqNo() { return mLastKnownSeqNo; }
    
    public ZimbraContext getZimbraContext() { return mContext; }
    
    public OzNotificationConnectionHandler(OzConnection connection) {
        mConnection = connection;
    }
    
    public void sendOverflowed() throws IOException {
        mConnection.writeAsciiWithCRLF("OVERFLOW reading HTTP header");
    }    
    
    public void writeData(byte[] data) throws ServiceException {
        try {
            mConnection.write(ByteBuffer.wrap(data));
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException trying to flush Notification Channel "+this.toString(), e);
        }
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
            mConnection.writeAsciiWithCRLF("Invalid sequence number");
            mConnection.close();
            return;
        }
        
        if (sessionId == null) {
            sessionId = (String)request.getHeaders().get("ZimbraSession");
        }
        if (authToken == null) {
            authToken = (String)request.getHeaders().get("ZimbraAuthToken");
        }
        
        if (sessionId == null || authToken == null) {
            mConnection.writeAsciiWithCRLF("Missing ZimbraSession or ZimbraAuthToken");
            mConnection.close();
            return;
        }
        
        try {
            mAuthToken = AuthToken.getAuthToken(authToken);
            
            if (ZimbraLog.mailbox.isDebugEnabled()) {
                ZimbraLog.mailbox.debug("NOTIFY: acct="+mAuthToken.getAccountId()+" session="+sessionId);
            }
        } catch(Exception e) {
            mConnection.writeAsciiWithCRLF("Invalid Auth Token");
            mConnection.close();
            return;
        }
        
        try {
            Map ctxt = new HashMap<String, String>();
            ctxt.put(SoapServlet.ZIMBRA_AUTH_TOKEN, authToken);
            
            mContext = new ZimbraContext(null, ctxt, SoapProtocol.Soap12);
        } catch(ServiceException e) {
            ZimbraLog.mailbox.info("Could not fetch ZimbraContext: acct="+mAuthToken.getAccountId()+" session="+sessionId);
            mConnection.writeAsciiWithCRLF("Error getting ZimbraContext: "+e);
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
                mConnection.writeAsciiWithCRLF("Caught exception: "+e.toString());
                mConnection.close();
                return;
            }
        } else {
            ZimbraLog.mailbox.info("NOTIFY COULD NOT FIND REQUESTED SESSION: acct="+mAuthToken.getAccountId()+" session="+sessionId);
            mConnection.writeAsciiWithCRLF("No such session");
            mConnection.close();
        }

    }

    public void handleDisconnect() {
        if (ZimbraLog.mailbox.isDebugEnabled()) 
            ZimbraLog.mailbox.debug("Notification Handler dropped connection: "+mConnection.getRemoteAddress().toString());
        
    }

    public void handleAlarm() throws IOException { }

}
