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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 29, 2004
 */
package com.zimbra.soap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import org.dom4j.QName;

import com.zimbra.cs.account.*;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapProtocol;


/**
 * @author schemers
 * 
 * This class models the soap context (the data from the soap envelope)  
 * for a single request 
 * 
 */
public class ZimbraSoapContext {

    private final class SessionInfo {
        Session session;
        boolean created;

        SessionInfo(Session s) {
            this(s, false);
        }
        SessionInfo(Session s, boolean newSession) {
            session = s;
            created = newSession;
        }

        void clearSession() {
            SessionCache.clearSession(session.getSessionId(), getAuthtokenAccountId());
            session = null;
        }

        public String toString()  { return session.getSessionId(); }
    }

    private static Log sLog = LogFactory.getLog(ZimbraSoapContext.class);

    private static final int MAX_HOP_COUNT = 5;

    private String    mRawAuthToken;
    private AuthToken mAuthToken;
    private String    mAuthTokenAccountId;
    private String    mRequestedAccountId;

    private SoapProtocol mRequestProtocol;
    private SoapProtocol mResponseProtocol;

    private boolean mChangeConstraintType = OperationContext.CHECK_MODIFIED;
    private int     mMaximumChangeId = -1;

    private int mNotificationSeqNo = -1;

    private List<SessionInfo> mSessionInfo = new ArrayList<SessionInfo>();
    private boolean mSessionSuppressed; // don't create a new session for this request
    private boolean mHaltNotifications; // if true, then no notifications are sent to this context
    private boolean mUnqualifiedItemIds;
    private boolean mWaitForNotifications = false;

    private ProxyTarget mProxyTarget;
    private boolean     mIsProxyRequest;
    private int         mHopCount;
    private boolean     mMountpointTraversed;

    private String      mUserAgent;
    private String      mRequestIP;

    public ZimbraSoapContext(ZimbraSoapContext lc, String targetAccountId) throws ServiceException {
        mRawAuthToken = lc.mRawAuthToken;
        mAuthToken = lc.mAuthToken;
        mAuthTokenAccountId = lc.mAuthTokenAccountId;
        mRequestedAccountId = targetAccountId;

        mRequestProtocol = lc.mRequestProtocol;
        mResponseProtocol = lc.mResponseProtocol;

        mSessionSuppressed = true;
        mUnqualifiedItemIds = lc.mUnqualifiedItemIds;

        mHopCount = lc.mHopCount + 1;
        if (mHopCount > MAX_HOP_COUNT)
            throw ServiceException.TOO_MANY_HOPS();
        mMountpointTraversed = lc.mMountpointTraversed;
    }

    /**
     * @param ctxt can be null if not present in request
     * @param context the engine context, which might have the auth token in it
     * @param requestProtocol TODO
     * @throws ServiceException
     */
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

        // check for auth token in engine context if not in header  
        mRawAuthToken = (ctxt == null ? null : ctxt.getAttribute(HeaderConstants.E_AUTH_TOKEN, null));
        if (mRawAuthToken == null)
            mRawAuthToken = (String) context.get(SoapServlet.ZIMBRA_AUTH_TOKEN);

        // parse auth token and check validity
        if (mRawAuthToken != null && !mRawAuthToken.equals("")) {
            try {
                mAuthToken = AuthToken.getAuthToken(mRawAuthToken);
                if (mAuthToken.isExpired())
                    throw ServiceException.AUTH_EXPIRED();
                mAuthTokenAccountId = mAuthToken.getAccountId();
            } catch (AuthTokenException e) {
                // ignore and leave null
                mAuthToken = null;
                if (sLog.isDebugEnabled())
                    sLog.debug("ZimbraContext AuthToken error: " + e.getMessage(), e);
            }
        }

        // look for the notification sequence id, for notification reliability
        // <notify seq="nn">
        Element notify = (ctxt == null ? null : ctxt.getOptionalElement(HeaderConstants.E_NOTIFY));
        if (notify != null) 
            mNotificationSeqNo = (int) notify.getAttributeLong(HeaderConstants.A_SEQNO, 0);

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

        // record session-related info and validate any specified sessions
        //   (don't create new sessions yet)
        if (ctxt != null) {
            mHaltNotifications = ctxt.getOptionalElement(HeaderConstants.E_NO_NOTIFY) != null;
            for (Iterator it = ctxt.elementIterator(HeaderConstants.E_SESSION_ID); it.hasNext(); ) {
                // they specified it, so create a SessionInfo and thereby ping the session as a keepalive
                parseSessionElement((Element) it.next());
            }
            mSessionSuppressed = ctxt.getOptionalElement(HeaderConstants.E_NO_SESSION) != null;
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

    /** Parses a <code>&lt;sessionId></code> {@link Element} from the SOAP Header.<p>
     * 
     *  If the XML specifies a valid {@link Session} owned by the authenticted user,
     *  a {@link SessionInfo} object wrapping the appropriate Session is added to the
     *  {@link #mSessionInfo} list.
     * 
     * @param elt  The <code>&lt;sessionId></code> Element. */
    private void parseSessionElement(Element elt) {
        if (elt == null)
            return;

        String sessionId = null;
        if ("".equals(sessionId = elt.getTextTrim()))
            sessionId = elt.getAttribute(HeaderConstants.A_ID, null);
        if (sessionId == null)
            return;

        // actually fetch the session from the cache
        Session session = SessionCache.lookup(sessionId, getAuthtokenAccountId());
        if (session == null) {
            sLog.info("requested session no longer exists: " + sessionId);
            return;
        }
        try {
            // turn off notifications if so directed
            if (session.getSessionType() == SessionCache.SESSION_SOAP)
                if (mHaltNotifications || !elt.getAttributeBool(HeaderConstants.A_NOTIFY, true))
                    ((SoapSession) session).haltNotifications();
        } catch (ServiceException e) { }

        mSessionInfo.add(new SessionInfo(session));
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
     *    <li><code>service.PERM_DENIED</code> - if you try to traverse two
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

    /** Generates a new {@link com.zimbra.cs.mailbox.Mailbox.OperationContext}
     *  object reflecting the constraints serialized in the <code>&lt;context></code>
     *  element in the SOAP header.<p>
     * 
     *  These optional constraints include:<ul>
     *  <li>the account ID from the auth token</li>
     *  <li>the highest change number the client knows about</li>
     *  <li>how stringently to check accessed items against the known change
     *      highwater mark</li></ul>
     * 
     * @return A new OperationContext object */
    public OperationContext getOperationContext() throws ServiceException {
        OperationContext octxt = new OperationContext(mAuthTokenAccountId, mAuthToken != null && (mAuthToken.isAdmin() || mAuthToken.isDomainAdmin()));
        octxt.setChangeConstraint(mChangeConstraintType, mMaximumChangeId);
        octxt.setRequestIP(mRequestIP);
        return octxt;
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


    /** Returns the account of the auth token. 
     * @throws ServiceException 
     */
    public Account getAuthtokenAccount() throws ServiceException {
        return Provisioning.getInstance().get(AccountBy.id, mAuthTokenAccountId);
    }

    /** Returns whether the authenticated user is the same as the user whose
     *  context the operation is set to execute in. */
    public boolean isDelegatedRequest() {
        return !mAuthTokenAccountId.equalsIgnoreCase(getRequestedAccountId());
    }

    /** Gets an existing valid {@link SessionInfo} item of the specified type.
     *  SessionInfo objects correspond to either:<ul>
     *  <li>existing, unexpired sessions specified in a <code>&lt;sessionId></code>
     *      element in the <code>&lt;context></code> SOAP header block, or</li>
     *  <li>new sessions created during the course of the SOAP call</li></ul>
     * 
     * @param type  One of the types defined in the {@link SessionCache} class.
     * @return A matching SessionInfo object or <code>null</code>. */
    private SessionInfo findSessionInfo(int type) {
        for (SessionInfo sinfo : mSessionInfo)
            if (sinfo.session.getSessionType() == type)
                return sinfo;
        return null;
    }

    /** Creates a new {@link Session} object associated with the given account.<p>
     * 
     *  As a side effect, the specfied account is considered to be authenticated.
     *  If that causes the "authenticated id" to change, we forget about all other
     *  sessions.  Those sessions are not deleted from the cache, though perhaps
     *  that's the right thing to do...
     * 
     * @param accountId  The account ID to create the new session for.
     * @param type       One of the types defined in the {@link SessionCache} class.
     * @return A new Session object of the appropriate type. */
    public Session getNewSession(String accountId, int type) {
        if (accountId != null && !accountId.equals(mAuthTokenAccountId))
            mSessionInfo.clear();
        mAuthTokenAccountId = accountId;
        return getSession(type);
    }

    /** Gets a {@link Session} object of the specified type.<p>
     * 
     *  If such a Session was mentioned in a <code>sessionId</code> element
     *  of the <code>context</code> SOAP header, the first matching one is
     *  returned.  Otherwise, we create a new Session and return it.  However,
     *  if <code>&lt;nosession></code> was specified in the <code>&lt;context></code>
     *  SOAP header, new Sessions are not created.
     * 
     * @param type  One of the types defined in the {@link SessionCache} class.
     * @return A new or existing Session object, or <code>null</code> if
     *         <code>&lt;nosession></code> was specified. */
    public Session getSession(int type) {
        Session s = null;
        SessionInfo sinfo = findSessionInfo(type);
        if (sinfo != null)
            s = sinfo.session;
        if (s == null && !mSessionSuppressed) {
            s = SessionCache.getNewSession(mAuthTokenAccountId, type);
            if (s != null)
                mSessionInfo.add(sinfo = new SessionInfo(s, true));
        }
        if (s instanceof SoapSession && mHaltNotifications)
            ((SoapSession) s).haltNotifications();
        return s;
    }

    private class SoapPushChannel implements SoapSession.PushChannel {
        public void close() { }
        public int getLastKnownSeqNo() { return mNotificationSeqNo; }
        public ZimbraSoapContext getSoapContext() { return ZimbraSoapContext.this; }
        public void notificationsReady(SoapSession session) throws ServiceException {
            signalNotification();
        }
    }

    synchronized public boolean beginWaitForNotifications() throws ServiceException {
        boolean someBlocked = false;
        boolean someReady = false;
        
        // synchronized against 
        for (SessionInfo sinfo : mSessionInfo) {
            if (sinfo.session instanceof SoapSession) {
                SoapSession ss = (SoapSession) sinfo.session;
                SoapSession.RegisterNotificationResult result = ss.registerNotificationConnection(new SoapPushChannel());
                switch (result) {
                    case NO_NOTIFY: break;
                    case DATA_READY: someReady = true; break;
                    case BLOCKING: someBlocked = true; break;
                }
            }
        }
        
        if (someBlocked && !someReady) {
            mWaitForNotifications = true;
        } else { 
            mWaitForNotifications = false;
        }
        
        return mWaitForNotifications;
    }

    /**
     * Called by the Session object if a new notification comes in 
     */
    synchronized public void signalNotification() {
        mWaitForNotifications = false;
        this.notifyAll();
    }
    
    synchronized public boolean waitingForNotifications() {
        return mWaitForNotifications;
    }

    /** Creates a <code>&lt;context></code> element for the SOAP Header containing
     *  session information and change notifications.<p>
     * 
     *  Sessions -- those passed in the SOAP request <code>&lt;context></code>
     *  block and those created during the course of processing the request -- are
     *  listed:<p>
     *     <code>&lt;sessionId [type="admin"] id="12">12&lt;/sessionId></code>
     * 
     * @return A new <code>&lt;context></code> {@link Element}, or <code>null</code>
     *         if there is no relevant information to encapsulate. */
    Element generateResponseHeader() {
        Element ctxt = null;
        try {
            for (SessionInfo sinfo : mSessionInfo) {
                Session session = sinfo.session;
                if (ctxt == null)
                    ctxt = createElement(HeaderConstants.CONTEXT);

                // session ID is valid, so ping it back to the client:
                encodeSession(ctxt, session, false);
                // put <refresh> blocks back for any newly-created SoapSession objects
                if (sinfo.created && session instanceof SoapSession)
                    ((SoapSession) session).putRefresh(ctxt, this);
                // put <notify> blocks back for any SoapSession objects
                if (session instanceof SoapSession)
                    ((SoapSession) session).putNotifications(this, ctxt, mNotificationSeqNo);
            }
//          if (ctxt != null && mAuthToken != null)
//          ctxt.addAttribute(E_AUTH_TOKEN, mAuthToken.toString(), Element.DISP_CONTENT);
            return ctxt;
        } catch (ServiceException e) {
            sLog.info("ServiceException while putting soap session refresh data", e);
            return null;
        }
    }

    /** Serializes this object for use in a proxied SOAP request.  The
     *  attributes encapsulated by the <code>ZimbraContext</code> -- the
     *  response protocol, the auth token, etc. -- are carried forward.
     *  Notification is expressly declined. */
    Element toProxyCtxt() {
        return toProxyCtxt(mRequestProtocol);
    }

    /** Serializes this object for use in a proxied SOAP request.  The
     *  attributes encapsulated by the <code>ZimbraContext</code> -- the
     *  response protocol, the auth token, etc. -- are carried forward.
     *  Notification is expressly declined. */
    Element toProxyCtxt(SoapProtocol proto) {
        Element ctxt = proto.getFactory().createElement(HeaderConstants.CONTEXT);
        if (mRawAuthToken != null)
            ctxt.addAttribute(HeaderConstants.E_AUTH_TOKEN, mRawAuthToken, Element.DISP_CONTENT);
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
     *      <code>&lt;sessionId [type="admin"] id="12">12&lt;/sessionId></code>
     * 
     * @param parent   The {@link Element} to add the serialized Session to.
     * @param session  The Session object to be serialized.
     * @param unique   Whether there can be more than one Session serialized to the
     *                 <code>parent</code> Element.
     * @return The created <code>&lt;sessionId></code> Element. */
    public static Element encodeSession(Element parent, Session session, boolean unique) {
        String sessionType = null;
        if (session.getSessionType() == SessionCache.SESSION_ADMIN)
            sessionType = HeaderConstants.SESSION_ADMIN;

        Element eSession = unique ? parent.addUniqueElement(HeaderConstants.E_SESSION_ID) : parent.addElement(HeaderConstants.E_SESSION_ID);
        eSession.addAttribute(HeaderConstants.A_TYPE, sessionType).addAttribute(HeaderConstants.A_ID, session.getSessionId())
        .setText(session.getSessionId());
        return eSession;
    }

    public SoapProtocol getRequestProtocol()   { return mRequestProtocol; }
    public SoapProtocol getResponseProtocol()  { return mResponseProtocol; }
    public Element createElement(String name)  { return mResponseProtocol.getFactory().createElement(name); }
    public Element createElement(QName qname)  { return mResponseProtocol.getFactory().createElement(qname); }

    /** Returns the parsed {@link AuthToken} for this SOAP request.  This can
     *  come either from an HTTP cookie attached to the SOAP request or from
     *  an <code>&lt;authToken></code> element in the SOAP Header. */
    public AuthToken getAuthToken() {
        return mAuthToken;
    }

    /** Returns the raw, encoded {@link AuthToken} for this SOAP request.
     *  This can come either from an HTTP cookie attached to the SOAP request
     *  or from an <code>&lt;authToken></code> element in the SOAP Header. */
    public String getRawAuthToken() {
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

    /** Formats the {@link MailItem}'s ID into a <code>String</code> that's
     *  addressable by the request's originator.  In other words, if the owner
     *  of the item matches the auth token's principal, you just get a bare
     *  ID.  But if the owners don't match, you get a formatted ID that refers
     *  to the correct <code>Mailbox</code> as well as the item in question.
     * 
     * @param item  The item whose ID we want to encode.
     * @see ItemId */
    public String formatItemId(MailItem item) {
        return mUnqualifiedItemIds ? formatItemId(item.getId()) : new ItemId(item).toString(this);
    }

    /** Formats the ({@link MailItem}'s ID, subpart ID) pair into a
     *  <code>String</code> that's addressable by the request's originator.
     *  In other words, if the owner of the item matches the auth token's
     *  principal, you just get a bare ID.  But if the owners don't match,
     *  you get a formatted ID that refers to the correct <code>Mailbox</code>
     *  as well as the item in question.
     * 
     * @param item   The item whose ID we want to encode.
     * @param subId  The subpart's ID.
     * @see ItemId */
    public String formatItemId(MailItem item, int subId) {
        return mUnqualifiedItemIds ? formatItemId(item.getId(), subId) : new ItemId(item, subId).toString(this);
    }

    /** Formats the item ID in the requested <code>Mailbox</code> into a
     *  <code>String</code> that's addressable by the request's originator.
     *  In other words, if the owner of the <code>Mailbox</code> matches the
     *  auth token's principal, you just get a bare ID.  But if the owners
     *  don't match, you get a formatted ID that refers to the correct
     *  <code>Mailbox</code> as well as the item in question.
     * 
     * @param itemId  The item's (local) ID.
     * @see ItemId */
    public String formatItemId(int itemId) {
        return new ItemId(mUnqualifiedItemIds ? null : getRequestedAccountId(), itemId).toString(this);
    }

    /** Formats the (item ID, subpart ID) pair in the requested account's
     *  <code>Mailbox</code> into a <code>String</code> that's addressable
     *  by the request's originator.  In other words, if the owner of the
     *  <code>Mailbox</code> matches the auth token's principal, you just
     *  get a bare ID.  But if the owners don't match, you get a formatted
     *  ID that refers to the correct <code>Mailbox</code> as well as the
     *  item in question.
     * 
     * @param itemId  The item's (local) ID.
     * @param subId   The subpart's ID.
     * @see ItemId */
    public String formatItemId(int itemId, int subId) {
        return new ItemId(mUnqualifiedItemIds ? null : getRequestedAccountId(), itemId, subId).toString(this);
    }

    /** Formats the item ID into a <code>String</code> that's addressable by
     *  the request's originator.  In other words, if the owner of the item
     *  ID matches the auth token's principal, you just get a bare ID.  But if
     *  the owners don't match, you get a formatted ID that refers to the
     *  correct <code>Mailbox</code> as well as the item in question.
     * 
     * @param iid  The item's account, item, and subpart IDs.
     * @see ItemId */
    public String formatItemId(ItemId iid) {
        return mUnqualifiedItemIds ? formatItemId(iid.getId(), iid.getSubpartId()) : iid.toString(this);
    }
}
