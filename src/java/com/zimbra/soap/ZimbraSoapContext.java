/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.QName;
import org.mortbay.util.ajax.Continuation;

import com.google.common.base.Strings;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession.PushChannel;
import com.zimbra.cs.util.BuildInfo;

/**
 * This class models the soap context (the data from the soap envelope)
 * for a single request.
 *
 * @since May 29, 2004
 */
public final class ZimbraSoapContext {

    final class SessionInfo {
        String sessionId;
        int sequence;
        boolean created;

        SessionInfo(String id, int seqNo, boolean newSession) {
            sessionId = id;  sequence = seqNo;  created = newSession;
        }

        @Override
        public String toString() {
            return sessionId;
        }

        private class SoapPushChannel implements SoapSession.PushChannel {
            private boolean mLocalChangesOnly;

            SoapPushChannel(boolean localOnly)  { mLocalChangesOnly = localOnly; }

            @Override
            public void closePushChannel() {
                // don't allow more than one NoOp to hang on an account
                signalNotification(true);
            }

            @Override
            public int getLastKnownSequence() {
                return sequence;
            }

            @Override
            public ZimbraSoapContext getSoapContext() {
                return ZimbraSoapContext.this;
            }

            @Override
            public boolean localChangesOnly() {
                return mLocalChangesOnly;
            }

            @Override
            public void notificationsReady() {
                signalNotification(false);
            }
        }

        PushChannel getPushChannel(boolean localChangesOnly) {
            return new SoapPushChannel(localChangesOnly);
        }
    }

    private static final Log sLog = LogFactory.getLog(ZimbraSoapContext.class);
    public static final int MAX_HOP_COUNT = 5;

    private ZAuthToken mRawAuthToken;
    private AuthToken mAuthToken;
    private String mAuthTokenAccountId;
    private String mRequestedAccountId;

    private SoapProtocol mRequestProtocol;
    private SoapProtocol mResponseProtocol;

    private boolean mChangeConstraintType = OperationContext.CHECK_MODIFIED;
    private int mMaximumChangeId = -1;

    private boolean mSessionEnabled;  // whether to create a new session for this request
    private boolean mSessionProxied;  // whether to create a new session for this request
    private SessionInfo mSessionInfo;
    private boolean mUnqualifiedItemIds;
    private boolean mWaitForNotifications;
    private boolean mCanceledWaitForNotifications = false;
    private Continuation mContinuation;  // used for blocking requests

    private ProxyTarget mProxyTarget;
    private boolean mIsProxyRequest;
    private int mHopCount;
    private boolean mMountpointTraversed;

    private String mUserAgent;
    private String mRequestIP;
    private String mVia;

    //zdsync: for parsing locally constructed soap requests
    public ZimbraSoapContext(AuthToken authToken, String accountId,
            SoapProtocol reqProtocol, SoapProtocol respProtocol) throws ServiceException {
        this(authToken, accountId, reqProtocol, respProtocol, 0);
    }

    /**
     * For Search-Proxying, allows us to manually specify the HopCount to use.
     * <p>
     * Hop count is not checked for TOO_MANY_HOPS in this route.
     */
    public ZimbraSoapContext(AuthToken authToken, String accountId,
            SoapProtocol reqProtocol, SoapProtocol respProtocol, int hopCount)
        throws ServiceException {

        mAuthToken = authToken;
        mRawAuthToken = authToken.toZAuthToken();
        mAuthTokenAccountId = authToken.getAccountId();
        mRequestedAccountId = accountId;
        mRequestProtocol = reqProtocol;
        mResponseProtocol = respProtocol;
        mSessionEnabled = false;
        mHopCount = hopCount;

        mUserAgent = mRequestIP = mVia = null;
    }

    /**
     * Creates a {@link ZimbraSoapContext} from another existing
     * {@link ZimbraSoapContext} for use in proxying.
     *
     * @param zsc context to clone
     */
    public ZimbraSoapContext(ZimbraSoapContext zsc) throws ServiceException {
        this(zsc, zsc.mRequestedAccountId);
    }

    /**
     * Creates a {@link ZimbraSoapContext} from another existing
     * {@link ZimbraSoapContext} for use in proxying.
     *
     * @param zsc context to clone
     * @param targetAccountId different account ID from the original request
     */
    public ZimbraSoapContext(ZimbraSoapContext zsc, String targetAccountId) throws ServiceException {
        mUserAgent = zsc.mUserAgent;
        mRequestIP = zsc.mRequestIP;
        mVia = zsc.mVia;
        mRawAuthToken = zsc.mRawAuthToken;
        mAuthToken = zsc.mAuthToken;
        mAuthTokenAccountId = zsc.mAuthTokenAccountId;
        mRequestedAccountId = targetAccountId;
        mRequestProtocol = zsc.mRequestProtocol;
        mResponseProtocol = zsc.mResponseProtocol;
        mSessionInfo = zsc.mSessionInfo;
        mSessionEnabled = zsc.mSessionEnabled;
        mSessionProxied = true;
        mUnqualifiedItemIds = zsc.mUnqualifiedItemIds;
        mMountpointTraversed = zsc.mMountpointTraversed;
        setHopCount(zsc.mHopCount + 1);
    }

    /**
     * Creates a {@link ZimbraSoapContext} from the {@code <context>}
     * {@link Element} from the SOAP header.
     *
     * @param ctxt {@code <context>} Element (can be null if not present in request)
     * @param context The engine context, which might contain the auth token
     * @param requestProtocol  The SOAP protocol used for the request */
    public ZimbraSoapContext(Element ctxt, Map<String, Object> context,
            SoapProtocol requestProtocol) throws ServiceException {

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

        try {
            mAuthToken = AuthProvider.getAuthToken(ctxt, context);
            if (mAuthToken != null) {
                mRawAuthToken = mAuthToken.toZAuthToken();

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

        // find out if we're executing in another user's context
        Element eAccount = ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_ACCOUNT);
        if (eAccount != null) {
            String key = eAccount.getAttribute(HeaderConstants.A_BY, null);
            String value = eAccount.getText();

            if (key == null) {
                mRequestedAccountId = null;
            } else if (key.equals(HeaderConstants.BY_NAME)) {
                Account account = prov.get(AccountBy.name, value, mAuthToken);
                if (account == null)
                    throw ServiceException.DEFEND_ACCOUNT_HARVEST(value);

                mRequestedAccountId = account.getId();
            } else if (key.equals(HeaderConstants.BY_ID)) {
                Account account = prov.get(AccountBy.id, value, mAuthToken);
                if (account == null)
                    throw ServiceException.DEFEND_ACCOUNT_HARVEST(value);

                mRequestedAccountId = value;
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for by: " + key, null);
            }

            mMountpointTraversed = eAccount.getAttributeBool(HeaderConstants.A_MOUNTPOINT, false);
        } else {
            mRequestedAccountId = null;
        }

        // retrieve hop count from the SOAP context and check the hop count to detect loops
        if (ctxt != null) {
            int hopCount = (int) Math.max(ctxt.getAttributeLong(HeaderConstants.A_HOPCOUNT, 0), 0);
            setHopCount(hopCount);
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
                mProxyTarget = new ProxyTarget(targetServerId, mAuthToken, req);
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
            boolean suppress = ctxt.getOptionalElement(HeaderConstants.E_NO_NOTIFY) != null;
            suppress |= ctxt.getOptionalElement(HeaderConstants.E_NO_SESSION) != null;
            // if sessions are enabled, create a SessionInfo to encapsulate (will fetch the Session object during the request)
            if (!suppress) {
                Element session = ctxt.getOptionalElement(HeaderConstants.E_SESSION);
                if (session != null) {
                    mSessionEnabled = true;
                    mSessionProxied = session.getAttributeBool(HeaderConstants.A_PROXIED, false);

                    String sessionId = null;
                    if ("".equals(sessionId = session.getTextTrim()))
                        sessionId = session.getAttribute(HeaderConstants.A_ID, null);
                    if (sessionId != null)
                        mSessionInfo = new SessionInfo(sessionId, (int) session.getAttributeLong(HeaderConstants.A_SEQNO, seqNo), false);
                }
            }
        }

        // temporary hack: don't qualify item ids in responses, if so requested
        mUnqualifiedItemIds = (ctxt != null && ctxt.getOptionalElement(HeaderConstants.E_NO_QUALIFY) != null);

        // Handle user agent if specified by the client.  The user agent string is formatted
        // as "name/version".
        Element userAgent = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_USER_AGENT));
        if (userAgent != null) {
            String name = userAgent.getAttribute(HeaderConstants.A_NAME, null);
            String version = userAgent.getAttribute(HeaderConstants.A_VERSION, null);
            if (!Strings.isNullOrEmpty(name)) {
                if (!Strings.isNullOrEmpty(version)) {
                    mUserAgent = name + "/" + version;
                } else {
                    mUserAgent = name;
                }
            }
        }

        if (ctxt != null) {
            Element via = ctxt.getOptionalElement(HeaderConstants.E_VIA);
            if (via != null) {
                mVia = via.getText();
            }
        }

        mRequestIP = (String) context.get(SoapEngine.REQUEST_IP);
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

    @Override
    public String toString() {
        String sessionPart = mSessionEnabled ? ", session=" + mSessionInfo : "";
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
        return mAuthTokenAccountId != null && !mAuthTokenAccountId.equalsIgnoreCase(getRequestedAccountId());
    }

    public boolean isUsingAdminPrivileges() {
        return mAuthToken != null && AuthToken.isAnyAdmin(mAuthToken);
    }

    public ZimbraSoapContext disableNotifications() {
        mSessionEnabled = false;
        mSessionInfo = null;
        return this;
    }

    public boolean isNotificationEnabled() {
        return mSessionEnabled;
    }

    public boolean isSessionProxied() {
        return mSessionProxied;
    }

    /** Returns the {@link SessionInfo} item associated with this
     *  SOAP request.  SessionInfo objects correspond to either:<ul>
     *  <li>sessions specified in a <tt>&lt;sessionId></tt> element in the
     *      <tt>&lt;context></tt> SOAP header block, or</li>
     *  <li>new sessions created during the course of the SOAP call</li></ul> */
    SessionInfo getSessionInfo() {
        return mSessionInfo;
    }

    public SessionInfo setProxySession(String sessionId) {
        if (!mSessionEnabled || sessionId == null) {
            mSessionInfo = null;
        } else {
            mSessionInfo = new SessionInfo(sessionId, 0, false);
        }
        mSessionProxied = true;
        return mSessionInfo;
    }

    SessionInfo recordNewSession(String sessionId) {
        if (!mSessionEnabled || sessionId == null) {
            mSessionInfo = null;
        } else if (mSessionInfo == null) {
            mSessionInfo = new SessionInfo(sessionId, 0, true);
        } else {
            mSessionInfo.sessionId = sessionId;  mSessionInfo.created = true;
        }
        return mSessionInfo;
    }

    protected void clearSessionInfo() {
        mSessionInfo = null;
        mSessionProxied = false;
    }

    /**
     * Returns {@code TRUE} if our referenced session is brand-new.
     * <p>
     * This special-case API is used to short-circuit blocking handlers so that
     * they return immediately to send a {@code <refresh>} block if one is
     * needed.
     */
    public boolean hasCreatedSession() {
        return mSessionInfo != null && mSessionInfo.created;
    }

    public boolean hasSession() {
        return mSessionInfo != null;
    }

    public boolean beginWaitForNotifications(Continuation continuation, boolean includeDelegates) throws ServiceException {
        mWaitForNotifications = true;
        mContinuation = continuation;

        Session session = SessionCache.lookup(mSessionInfo.sessionId, mAuthTokenAccountId);
        if (!(session instanceof SoapSession))
            return false;

        SoapSession ss = (SoapSession) session;
        SoapSession.RegisterNotificationResult result = ss.registerNotificationConnection(mSessionInfo.getPushChannel(!includeDelegates));
        switch (result) {
            case NO_NOTIFY:   return false;
            case DATA_READY:  return false;
            case BLOCKING:    return true;
            default:          return false;
        }
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


    /**
     * Serializes this object for use in a proxied SOAP request.
     * <p>
     * The attributes encapsulated by the {@link ZimbraSoapContext} -- the
     * response protocol, the auth token, etc. -- are carried forward.
     */
    Element toProxyContext(SoapProtocol proto) {
        Element ctxt = proto.getFactory().createElement(HeaderConstants.CONTEXT);

        ctxt.addAttribute(HeaderConstants.A_HOPCOUNT, mHopCount);

        String proxyAuthToken = mAuthToken.getProxyAuthToken();
        if (proxyAuthToken != null) {
            new ZAuthToken(proxyAuthToken).encodeSoapCtxt(ctxt);
        } else if (mRawAuthToken != null) {
            mRawAuthToken.encodeSoapCtxt(ctxt);
        }
        if (mResponseProtocol != mRequestProtocol) {
            ctxt.addElement(HeaderConstants.E_FORMAT).addAttribute(HeaderConstants.A_TYPE,
                    mResponseProtocol == SoapProtocol.SoapJS ?
                            HeaderConstants.TYPE_JAVASCRIPT : HeaderConstants.TYPE_XML);
        }

        if (!mSessionEnabled) {
            // be backwards-compatible for sanity-preservation purposes
            ctxt.addUniqueElement(HeaderConstants.E_NO_SESSION);
        } else if (mSessionInfo != null) {
            encodeSession(ctxt, mSessionInfo.sessionId, null).addAttribute(HeaderConstants.A_PROXIED, true);
        } else {
            ctxt.addUniqueElement(HeaderConstants.E_SESSION).addAttribute(HeaderConstants.A_PROXIED, true);
        }

        Element eAcct = ctxt.addElement(HeaderConstants.E_ACCOUNT).addAttribute(HeaderConstants.A_MOUNTPOINT, mMountpointTraversed);
        if (mRequestedAccountId != null && !mRequestedAccountId.equalsIgnoreCase(mAuthTokenAccountId)) {
            eAcct.addAttribute(HeaderConstants.A_BY, HeaderConstants.BY_ID).setText(mRequestedAccountId);
        }

        if (mUnqualifiedItemIds) {
            ctxt.addUniqueElement(HeaderConstants.E_NO_QUALIFY);
        }
        Element ua = ctxt.addUniqueElement(HeaderConstants.E_USER_AGENT);
        ua.addAttribute(HeaderConstants.A_NAME, "ZSC");
        ua.addAttribute(HeaderConstants.A_VERSION, BuildInfo.VERSION);
        ctxt.addUniqueElement(HeaderConstants.E_VIA).setText(getNextVia());
        return ctxt;
    }

    /**
     * Serializes a {@link Session} object to return it to a client.
     * <p>
     * The serialized XML representation of a Session is:
     * {@code <sessionId [type="admin"] id="12">12</sessionId>}
     *
     * @param parent The {@link Element} to add the serialized Session to
     * @param sessionId TODO
     * @param sessionType TODO
     * @return The created {@code <sessionId>} Element
     */
    public static Element encodeSession(Element parent, String sessionId, Session.Type sessionType) {
        Element oldSession = parent.getOptionalElement(HeaderConstants.E_SESSION);
        if (oldSession != null)
            oldSession.detach();

        String typeStr = (sessionType == Session.Type.ADMIN ? HeaderConstants.SESSION_ADMIN : null);
        Element eSession = parent.addUniqueElement(HeaderConstants.E_SESSION);
        eSession.addAttribute(HeaderConstants.A_TYPE, typeStr).addAttribute(HeaderConstants.A_ID, sessionId).setText(sessionId);
        return eSession;
    }

    public SoapProtocol getRequestProtocol() {
        return mRequestProtocol;
    }

    public SoapProtocol getResponseProtocol() {
        return mResponseProtocol;
    }

    public Element createElement(String name) {
        return mResponseProtocol.getFactory().createElement(name);
    }

    public Element createElement(QName qname) {
        return mResponseProtocol.getFactory().createElement(qname);
    }

    public Element createRequestElement(String name) {
        return mRequestProtocol.getFactory().createElement(name);
    }

    public Element createRequestElement(QName qname) {
        return mRequestProtocol.getFactory().createElement(qname);
    }

    /**
     * Returns the parsed {@link AuthToken} for this SOAP request.
     * <p>
     * This can come either from an HTTP cookie attached to the SOAP request or
     * from an {@code <authToken>} element in the SOAP Header.
     */
    public AuthToken getAuthToken() {
        return mAuthToken;
    }

    /**
     * Returns the raw, encoded {@link AuthToken} for this SOAP request.
     * <p>
     * This can come either from an HTTP cookies attached to the SOAP request
     * or from an {@code <authToken>} element in the SOAP Header.
     */
    public ZAuthToken getRawAuthToken() {
        return mRawAuthToken;
    }

    public ProxyTarget getProxyTarget() {
        return mProxyTarget;
    }

    public boolean isProxyRequest() {
        return mIsProxyRequest;
    }

    /**
     * Returns the name and version of the client that's making the current
     * request, in the format "name/version".
     */
    public String getUserAgent() {
        return mUserAgent;
    }

    public String getRequestIP() {
        return mRequestIP;
    }

    /**
     * Returns {@code via} header value of the SOAP request.
     *
     * @return {@code via} header value
     */
    String getVia() {
        return mVia;
    }

    /**
     * Creates a {@code via} header value for next proxy hop.
     *
     * @return new {@code via}
     */
    String getNextVia() {
        StringBuilder result = new StringBuilder();

        if (mVia != null) {
            result.append(mVia);
            result.append(',');
        }

        result.append(mRequestIP != null ? mRequestIP : '?');
        result.append('(');
        result.append(mUserAgent != null ? mUserAgent : '?');
        result.append(')');

        return result.toString();
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

    private void setHopCount(int hopCount) throws ServiceException {
        if (hopCount > MAX_HOP_COUNT)
            throw ServiceException.TOO_MANY_HOPS(mRequestedAccountId);
        mHopCount = hopCount;
    }

    public int getHopCount() {
        return mHopCount;
    }

    public void resetProxyAuthToken() {
        mAuthToken.resetProxyAuthToken();
        mRawAuthToken.resetProxyAuthToken();
    }
}
