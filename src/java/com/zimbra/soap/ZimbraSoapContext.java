/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on May 29, 2004
 */
package com.zimbra.soap;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.QName;
import org.mortbay.util.ajax.Continuation;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession.PushChannel;

/**
 * This class models the soap context (the data from the soap envelope)  
 * for a single request 
 */
public class ZimbraSoapContext {

    final class SessionInfo {
        String sessionId;
        int sequence;
        boolean created;

        SessionInfo(String id, int seqNo, boolean newSession) {
            sessionId = id;  sequence = seqNo;  created = newSession;
        }

        public String toString()  { return sessionId; }


        private class SoapPushChannel implements SoapSession.PushChannel {
            public void closePushChannel() {  
                signalNotification(true); // don't allow there to be more than one NoOp hanging on a particular account
            }
            public int getLastKnownSeqNo()            { return sequence; }
            public ZimbraSoapContext getSoapContext() { return ZimbraSoapContext.this; }
            public void notificationsReady()          { signalNotification(false); }
        }

        public PushChannel getPushChannel() {
            return new SoapPushChannel();
        }
    }

    private static Log sLog = LogFactory.getLog(ZimbraSoapContext.class);

    public static final int MAX_HOP_COUNT = 5;

    private String    mRawAuthToken;
    private AuthToken mAuthToken;
    private String    mAuthTokenAccountId;
    private String    mRequestedAccountId;

    private SoapProtocol mRequestProtocol;
    private SoapProtocol mResponseProtocol;

    private boolean mChangeConstraintType = OperationContext.CHECK_MODIFIED;
    private int     mMaximumChangeId = -1;

    private SessionInfo mSessionInfo;
    private boolean mSessionSuppressed; // don't create a new session for this request
    private boolean mUnqualifiedItemIds;
    private boolean mWaitForNotifications;
    private boolean mCanceledWaitForNotifications = false;
    private Continuation mContinuation; // used for blocking requests

    private ProxyTarget mProxyTarget;
    private boolean     mIsProxyRequest;
    private int         mHopCount;
    private boolean     mMountpointTraversed;

    private String mUserAgent;
    private String mRequestIP;

    //zdsync: for parsing locally constructed soap requests
    public ZimbraSoapContext(AuthToken authToken, String accountId, SoapProtocol reqProtocol, SoapProtocol respProtocol) throws ServiceException {
        this(authToken, accountId, reqProtocol, respProtocol, 0);
    }
    
    /**
     * For Search-Proxying, allows us to manually specify the HopCount to use
     */
    public ZimbraSoapContext(AuthToken authToken, String accountId, SoapProtocol reqProtocol, SoapProtocol respProtocol, int hopCount) throws ServiceException {
        mAuthToken = authToken;
        try {
            mRawAuthToken = authToken.getEncoded();
        } catch (AuthTokenException x) {
            throw ServiceException.FAILURE("AuthTokenExcepiton", x);
        }
        mAuthTokenAccountId = authToken.getAccountId();
        mRequestedAccountId = accountId;
        mRequestProtocol = reqProtocol;
        mResponseProtocol = respProtocol;

        mSessionSuppressed = true;
        mHopCount = hopCount;
    }
    
    
    /** Creates a <code>ZimbraSoapContext</code> from another existing
     *  <code>ZimbraSoapContext</code> for use in proxying. */
    public ZimbraSoapContext(ZimbraSoapContext zsc, String targetAccountId) throws ServiceException {
        mRawAuthToken = zsc.mRawAuthToken;
        mAuthToken = zsc.mAuthToken;
        mAuthTokenAccountId = zsc.mAuthTokenAccountId;
        mRequestedAccountId = targetAccountId;

        mRequestProtocol = zsc.mRequestProtocol;
        mResponseProtocol = zsc.mResponseProtocol;

        mSessionInfo = zsc.mSessionInfo;
        mSessionSuppressed = zsc.mSessionSuppressed;
        mUnqualifiedItemIds = zsc.mUnqualifiedItemIds;

        mHopCount = zsc.mHopCount + 1;
        if (mHopCount > MAX_HOP_COUNT)
            throw ServiceException.TOO_MANY_HOPS();
        mMountpointTraversed = zsc.mMountpointTraversed;
    }

    /** Creates a <code>ZimbraSoapContext</code> from the <tt>&lt;context></tt>
     *  {@link Element} from the SOAP header.
     *  
     * @param ctxt     The <tt>&lt;context></tt> Element (can be null if not
     *                 present in request)
     * @param context  The engine context, which might contain the auth token
     * @param requestProtocol  The SOAP protocol used for the request */
    public ZimbraSoapContext(Element ctxt, Map context, SoapProtocol requestProtocol) throws ServiceException {
        if (ctxt != null && !ctxt.getQName().equals(HeaderConstants.CONTEXT))
            throw new IllegalArgumentException("expected ctxt, got: " + ctxt.getQualifiedName());

        Provisioning prov = Provisioning.getInstance();

        // figure out if we're explicitly asking for a return format
        mResponseProtocol = mRequestProtocol = requestProtocol;
        Element eFormat = ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_FORMAT);
        if (eFormat != null) {
            String format = eFormat.getAttribute(HeaderConstants.A_TYPE, HeaderConstants.TYPE_XML);
            if (format.equals(HeaderConstants.TYPE_XML) && requestProtocol == SoapProtocol.SoapJS)
                mResponseProtocol = SoapProtocol.Soap12;
            else if (format.equals(HeaderConstants.TYPE_JAVASCRIPT))
                mResponseProtocol = SoapProtocol.SoapJS;
        }

        // find out if we're executing in another user's context
        Account account = null;
        Element eAccount = ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_ACCOUNT);
        if (eAccount != null) {
            String key = eAccount.getAttribute(HeaderConstants.A_BY, null);
            String value = eAccount.getText();

            if (key == null) {
                mRequestedAccountId = null;
            } else if (key.equals(HeaderConstants.BY_NAME)) {
                account = prov.get(AccountBy.name, value);
                if (account == null)
                    throw AccountServiceException.NO_SUCH_ACCOUNT(value);
                mRequestedAccountId = account.getId();
            } else if (key.equals(HeaderConstants.BY_ID)) {
                mRequestedAccountId = value;
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for by: " + key, null);
            }

            // while we're here, check the hop count to detect loops
            mHopCount = (int) Math.max(eAccount.getAttributeLong(HeaderConstants.A_HOPCOUNT, 0), 0);
            if (mHopCount > MAX_HOP_COUNT)
                throw ServiceException.TOO_MANY_HOPS();
            mMountpointTraversed = eAccount.getAttributeBool(HeaderConstants.A_MOUNTPOINT, false);
        } else {
            mRequestedAccountId = null;
        }

        try {
            mAuthToken = AuthProvider.getAuthToken(ctxt, context);
            if (mAuthToken != null) {
                /*
                 * AP-TODO-1:
                 * Note, there is a behavior change on setting the mRawAuthToken.
                 * 
                 * before: mRawAuthToken is always set to either the <authToken> in soap context
                 *         header, or the value set in the engine context, even if the raw 
                 *         auth token *cannot* be resolved to an AuthToken object.
                 *         
                 * now:    mRawAuthToken is set only when the raw auth token can be resolved into 
                 *         an AuthToken object, and the raw value is obtained by calling the 
                 *         getEncoded method of the AuthToken object.
                 *      
                 * This should be fine because no call sites that access mRawAuthToken can be reached 
                 * without a good AuthToken object.  If raw auth token cannot turn into an AuthToken,
                 * handling of the request would have been stopped at SoapEngine:302, returning a 
                 * AUTH_REQUIRED SOAP fault.    
                 * 
                 * Check: is there a case where AuthToken object is not required (and thus gets passed 
                 * SoapEngine:302) but mRawAuthToken is needed for proxying?  e.g. commands that don't 
                 * need auth on the first hop server but would need auth and can auth with whatever enabled 
                 * providers on the target serve?  Doesn't look like there is such cases.
                 *          
                 */
                mRawAuthToken = mAuthToken.getEncoded();
            
                if (mAuthToken.isExpired())
                    throw ServiceException.AUTH_EXPIRED();
                mAuthTokenAccountId = mAuthToken.getAccountId();
            }
        } catch (AuthTokenException e) {
            // ignore and leave null
            mAuthToken = null;
            if (sLog.isDebugEnabled())
                sLog.debug("ZimbraContext AuthToken error: " + e.getMessage(), e);
        }
                
        
        // constrain operations if we know the max change number the client knows about
        Element change = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_CHANGE));
        if (change != null) {
            try {
                String token = change.getAttribute(HeaderConstants.A_CHANGE_ID, "-1");
                int delimiter = token.indexOf('-');

                mMaximumChangeId = Integer.parseInt(delimiter < 1 ? token : token.substring(0, delimiter));
                if (change.getAttribute(HeaderConstants.A_TYPE, HeaderConstants.CHANGE_MODIFIED).equals(HeaderConstants.CHANGE_MODIFIED))
                    mChangeConstraintType = OperationContext.CHECK_MODIFIED;
                else
                    mChangeConstraintType = OperationContext.CHECK_CREATED;
            } catch (NumberFormatException nfe) { }
        }

        // if the caller specifies an execution host or if we're on the wrong host, proxy
        mIsProxyRequest = false;
        String targetServerId = ctxt == null ? null : ctxt.getAttribute(HeaderConstants.E_TARGET_SERVER, null);
        if (targetServerId != null) {
            HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            if (req != null) {
                mProxyTarget = new ProxyTarget(targetServerId, mRawAuthToken, req);
                mIsProxyRequest = !mProxyTarget.isTargetLocal();
            } else {
                sLog.warn("Missing SERVLET_REQUEST key in request context");
            }
        }

        // look for the notification sequence id, for notification reliability
        //   <notify seq="nn">
        int seqNo = -1;
        Element notify = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_NOTIFY));
        if (notify != null) 
            seqNo = (int) notify.getAttributeLong(HeaderConstants.A_SEQNO, 0);

        // record session-related info and validate any specified sessions
        //   (don't create new sessions yet)
        if (ctxt != null) {
            mSessionSuppressed  = ctxt.getOptionalElement(HeaderConstants.E_NO_NOTIFY) != null;
            mSessionSuppressed |= ctxt.getOptionalElement(HeaderConstants.E_NO_SESSION) != null;
            // if sessions are enabled, create a SessionInfo to encapsulate (will fetch the Session object during the request)
            if (!mSessionSuppressed) {
                for (Element session : ctxt.listElements(HeaderConstants.E_SESSION_ID)) {
                    String sessionId = null;
                    if ("".equals(sessionId = session.getTextTrim()))
                        sessionId = session.getAttribute(HeaderConstants.A_ID, null);
                    if (sessionId != null)
                        mSessionInfo = new SessionInfo(sessionId, (int) session.getAttributeLong(HeaderConstants.A_SEQNO, seqNo), false);
                }
            }
        }

        // temporary hack: don't qualify item ids in reponses, if so requested
        mUnqualifiedItemIds = (ctxt != null && ctxt.getOptionalElement(HeaderConstants.E_NO_QUALIFY) != null);

        // Handle user agent if specified by the client.  The user agent string is formatted
        // as "name/version".
        Element userAgent = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_USER_AGENT));
        if (userAgent != null) {
            String name = userAgent.getAttribute(HeaderConstants.A_NAME, null);
            String version = userAgent.getAttribute(HeaderConstants.A_VERSION, null);
            if (!StringUtil.isNullOrEmpty(name)) {
                mUserAgent = name;
                if (!StringUtil.isNullOrEmpty(version))
                    mUserAgent = mUserAgent + "/" + version;
            }
        }

        mRequestIP = (String)context.get(SoapEngine.REQUEST_IP);
    }

    /** Records in this context that we've traversed a mountpoint to get here.
     *  Enforces the "cannot mount a mountpoint" rule forbidding one link from
     *  pointing to another link.  The rationale for this limitation is that
     *  we want to avoid having the indirectly addressed resource completely
     *  change without the user knowing, and we want to know that the target
     *  of the link is also the owner of the items found therein.  Basically,
     *  things are just simpler this way.
     * 
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>service.PERM_DENIED</tt> - if you try to traverse two
     *        mountpoints in the course of a single proxy</ul> */
    public void recordMountpointTraversal() throws ServiceException {
        if (mMountpointTraversed)
            throw ServiceException.PERM_DENIED("cannot mount a mountpoint");
        mMountpointTraversed = true;
    }

    public String toString() {
        String sessionPart = "";
        if (!mSessionSuppressed)
            sessionPart = ", sessions=" + mSessionInfo;
        return "LC(mbox=" + mAuthTokenAccountId + sessionPart + ")";
    }

    /** Returns the account id the request is supposed to operate on.  This
     *  can be explicitly specified in the supplied context; it defaults to
     *  the account id in the auth token. */
    public String getRequestedAccountId() {
        return (mRequestedAccountId != null ? mRequestedAccountId : mAuthTokenAccountId);
    } 

    /** Returns the id of the account in the auth token.  Operations should
     *  normally use {@link #getRequestedAccountId}, as that's the context
     *  that the operations is executing in. */
    public String getAuthtokenAccountId() {
        return mAuthTokenAccountId;
    }

    /** Sets the AuthToken of the authenticated account.  This should only be called
     *  in the course of some flavor of <tt>Auth</tt> request. */
    void setAuthToken(AuthToken auth) {
        mAuthToken = auth;
        mAuthTokenAccountId = auth.getAccountId();
    }

    /** Returns whether the authenticated user is the same as the user whose
     *  context the operation is set to execute in. */
    public boolean isDelegatedRequest() {
        return !mAuthTokenAccountId.equalsIgnoreCase(getRequestedAccountId());
    }


    public boolean isNotificationEnabled() {
        return !mSessionSuppressed;
    }

    /** Returns the {@link SessionInfo} item associated with this
     *  SOAP request.  SessionInfo objects correspond to either:<ul>
     *  <li>sessions specified in a <tt>&lt;sessionId></tt> element in the 
     *      <tt>&lt;context></tt> SOAP header block, or</li>
     *  <li>new sessions created during the course of the SOAP call</li></ul> */
    SessionInfo getSessionInfo() {
        return mSessionInfo;
    }

    SessionInfo recordNewSession(String sessionId) {
        return (mSessionInfo = new SessionInfo(sessionId, 0, true));
    }

    void clearSessionInfo() {
        mSessionInfo = null;
    }

    /** Returns <tt>TRUE</tt> if our referenced session is brand-new.  This
     *  special-case API is used to short-circuit blocking handlers so that
     *  they return immediately to send a <refresh> block if one is needed. */
    public boolean hasCreatedSession() {
        return mSessionInfo != null && mSessionInfo.created;
    }


    public boolean beginWaitForNotifications(Continuation continuation) throws ServiceException {
        boolean someBlocked = false;
        boolean someReady = false;
        mWaitForNotifications = true;
        mContinuation = continuation;

        Session session = SessionCache.lookup(mSessionInfo.sessionId, mAuthTokenAccountId);
        if (session instanceof SoapSession) {
            SoapSession ss = (SoapSession) session;
            SoapSession.RegisterNotificationResult result = ss.registerNotificationConnection(mSessionInfo.getPushChannel());
            switch (result) {
                case NO_NOTIFY: break;
                case DATA_READY: someReady = true; break;
                case BLOCKING: someBlocked = true; break;
            }
        }

        return (someBlocked && !someReady);
    }

    /** Called by the Session object if a new notification comes in. */
    synchronized public void signalNotification(boolean canceled) {
        mWaitForNotifications = false;
        mCanceledWaitForNotifications = canceled;
        mContinuation.resume();
    }
    
    synchronized public boolean isCanceledWaitForNotifications() {
        return mCanceledWaitForNotifications;
    }
    
    synchronized public boolean waitingForNotifications() {
        return mWaitForNotifications;
    }


    /** Serializes this object for use in a proxied SOAP request.  The
     *  attributes encapsulated by the <code>ZimbraContext</code> -- the
     *  response protocol, the auth token, etc. -- are carried forward.
     *  Notification is expressly declined. */
    public Element toProxyCtxt() {
        return toProxyCtxt(mRequestProtocol);
    }

    /** Serializes this object for use in a proxied SOAP request.  The
     *  attributes encapsulated by the <code>ZimbraContext</code> -- the
     *  response protocol, the auth token, etc. -- are carried forward.
     *  Notification is expressly declined. */
    public Element toProxyCtxt(SoapProtocol proto) {
        Element ctxt = proto.getFactory().createElement(HeaderConstants.CONTEXT);
        if (mRawAuthToken != null)
            ctxt.addAttribute(HeaderConstants.E_AUTH_TOKEN, mRawAuthToken, Element.Disposition.CONTENT);
        if (mResponseProtocol != mRequestProtocol)
            ctxt.addElement(HeaderConstants.E_FORMAT).addAttribute(HeaderConstants.A_TYPE, mResponseProtocol == SoapProtocol.SoapJS ? HeaderConstants.TYPE_JAVASCRIPT : HeaderConstants.TYPE_XML);
        Element eAcct = ctxt.addElement(HeaderConstants.E_ACCOUNT).addAttribute(HeaderConstants.A_HOPCOUNT, mHopCount).addAttribute(HeaderConstants.A_MOUNTPOINT, mMountpointTraversed);
        if (mRequestedAccountId != null && !mRequestedAccountId.equalsIgnoreCase(mAuthTokenAccountId))
            eAcct.addAttribute(HeaderConstants.A_BY, HeaderConstants.BY_ID).setText(mRequestedAccountId);
        if (mSessionSuppressed)
            ctxt.addUniqueElement(HeaderConstants.E_NO_SESSION);
        if (mUnqualifiedItemIds)
            ctxt.addUniqueElement(HeaderConstants.E_NO_QUALIFY);
        return ctxt;
    }

    /** Serializes a {@link Session} object to return it to a client.  The serialized
     *  XML representation of a Session is:<p>
     *      <tt>&lt;sessionId [type="admin"] id="12">12&lt;/sessionId></tt>
     * 
     * @param parent   The {@link Element} to add the serialized Session to.
     * @param sessionId TODO
     * @param sessionType TODO
     * @param unique   Whether there can be more than one Session serialized to the
     *                 <tt>parent</tt> Element.
     * @return The created <tt>&lt;sessionId></tt> Element. */
    public static Element encodeSession(Element parent, String sessionId, Session.Type sessionType, boolean unique) {
        String typeStr = (sessionType == Session.Type.ADMIN ? HeaderConstants.SESSION_ADMIN : null);

        Element elt = unique ? parent.addUniqueElement(HeaderConstants.E_SESSION_ID) : parent.addElement(HeaderConstants.E_SESSION_ID);
        elt.addAttribute(HeaderConstants.A_TYPE, typeStr).addAttribute(HeaderConstants.A_ID, sessionId).setText(sessionId);
        return elt;
    }

    public SoapProtocol getRequestProtocol()   { return mRequestProtocol; }
    public SoapProtocol getResponseProtocol()  { return mResponseProtocol; }

    public Element createElement(String name)  { return mResponseProtocol.getFactory().createElement(name); }
    public Element createElement(QName qname)  { return mResponseProtocol.getFactory().createElement(qname); }

    public Element createRequestElement(String name)  { return mRequestProtocol.getFactory().createElement(name); }
    public Element createRequestElement(QName qname)  { return mRequestProtocol.getFactory().createElement(qname); }

    /** Returns the parsed {@link AuthToken} for this SOAP request.  This can
     *  come either from an HTTP cookie attached to the SOAP request or from
     *  an <tt>&lt;authToken></tt> element in the SOAP Header. */
    public AuthToken getAuthToken() {
        return mAuthToken;
    }

    /** Returns the raw, encoded {@link AuthToken} for this SOAP request.
     *  This can come either from an HTTP cookie attached to the SOAP request
     *  or from an <tt>&lt;authToken></tt> element in the SOAP Header. */
    // AP-TODO-2: retire
    public String getRawAuthToken() {
        return mRawAuthToken;
    }

    public ProxyTarget getProxyTarget() {
        return mProxyTarget;
    }

    public boolean isProxyRequest() {
        return mIsProxyRequest;
    }

    /** Returns the name and version of the client that's making the current
     *  request, in the format "name/version". s*/
    public String getUserAgent() {
        return mUserAgent;
    }

    public String getRequestIP() {
        return mRequestIP;
    }

    boolean getChangeConstraintType() {
        return mChangeConstraintType;
    }

    int getChangeConstraintLimit() {
        return mMaximumChangeId;
    }

    public boolean wantsUnqualifiedIds() {
        return mUnqualifiedItemIds;
    }
    
    public int getHopCount() {
        return mHopCount;
    }
}
