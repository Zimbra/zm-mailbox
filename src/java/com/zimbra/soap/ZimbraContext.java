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

/*
 * Created on May 29, 2004
 */
package com.zimbra.soap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.QName;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.SessionCache;


/**
 * @author schemers
 */
public class ZimbraContext {

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

	private static Log sLog = LogFactory.getLog(ZimbraContext.class);

    private static final int MAX_HOP_COUNT = 5;

	public static final QName CONTEXT = QName.get("context", ZimbraNamespace.ZIMBRA);
    public static final String E_NO_NOTIFY  = "nonotify";
    public static final String E_FORMAT     = "format";
    public static final String A_TYPE       = "type";
	public static final String E_AUTH_TOKEN = "authToken";
	public static final String A_BY         = "by";
    public static final String E_ACCOUNT    = "account";   
    public static final String E_NO_SESSION = "nosession";
    public static final String E_SESSION_ID = "sessionId";
    public static final String A_ACCOUNT_ID = "acct";
    public static final String A_ID         = "id";
    public static final String A_NOTIFY     = "notify";
    public static final String E_CHANGE     = "change";
    public static final String A_CHANGE_ID  = "token";
    public static final String E_TARGET_SERVER = "targetServer";

    public static final String BY_NAME = "name";
    public static final String BY_ID   = "id";
    public static final String TYPE_XML        = "xml";
    public static final String TYPE_JAVASCRIPT = "js";
    public static final String CHANGE_MODIFIED = "mod";
    public static final String CHANGE_CREATED  = "new";
    public static final String SESSION_MAIL  = "mail";
    public static final String SESSION_ADMIN = "admin";

	private AuthToken mAuthToken;
	private String    mAuthTokenAccountId;
	private String    mRequestedAccountId;

    private SoapProtocol mResponseProtocol;

    private boolean mChangeConstraintType = OperationContext.CHECK_MODIFIED;
    private int     mMaximumChangeId = -1;

    private List    mSessionInfo = new ArrayList();
    private boolean mSessionSuppressed;
    private boolean mHaltNotifications;

    private ProxyTarget mProxyTarget;
    private boolean     mIsProxyRequest;
    private int         mHopCount;


    public ZimbraContext(ZimbraContext lc, String targetAccountId) throws ServiceException {
        mAuthToken = lc.mAuthToken;
        mAuthTokenAccountId = lc.mAuthTokenAccountId;
        mRequestedAccountId = targetAccountId;

        mResponseProtocol = lc.mResponseProtocol;

        mSessionSuppressed = true;

        mHopCount = lc.mHopCount + 1;
        if (mHopCount > MAX_HOP_COUNT)
            throw ServiceException.TOO_MANY_HOPS();
    }

	/**
	 * @param ctxt can be null if not present in request
	 * @param context the engine context, which might have the auth token in it
	 * @param requestProtocol TODO
	 * @throws ServiceException
	 */
	public ZimbraContext(Element ctxt, Map context, SoapProtocol requestProtocol) throws ServiceException {
	    if (ctxt != null && !ctxt.getQName().equals(CONTEXT)) 
	        throw new IllegalArgumentException("expected ctxt, got: " + ctxt.getQualifiedName());
        
        // figure out if we're explicitly asking for a return format
        mResponseProtocol = requestProtocol;
        Element eFormat = ctxt == null ? null : ctxt.getOptionalElement(E_FORMAT);
        if (eFormat != null) {
            String format = eFormat.getAttribute(A_TYPE, TYPE_XML);
            if (format.equals(TYPE_XML) && requestProtocol == SoapProtocol.SoapJS)
                mResponseProtocol = SoapProtocol.Soap12;
            else if (format.equals(TYPE_JAVASCRIPT))
                mResponseProtocol = SoapProtocol.SoapJS;
        }

        Element eAccount = ctxt == null ? null : ctxt.getOptionalElement(E_ACCOUNT);
		if (eAccount != null) {
		    String key = eAccount.getAttribute(A_BY);
		    String value = eAccount.getText();

	        if (key.equals(BY_NAME)) {
	            Account account = Provisioning.getInstance().getAccountByName(value);
	            if (account == null)
                    throw AccountServiceException.NO_SUCH_ACCOUNT(value);
	            mRequestedAccountId = account.getId();
	        } else if (key.equals(BY_ID))
	            mRequestedAccountId = value;
	        else
	            throw ServiceException.INVALID_REQUEST("unknown value for by: " + key, null);
		} else {
		    mRequestedAccountId = null;
		}

        // check for atoken in engine context if not in header  
		String atoken = (ctxt == null ? null : ctxt.getAttribute(E_AUTH_TOKEN, null));
		if (atoken == null)
		    atoken = (String) context.get(SoapServlet.ZIMBRA_AUTH_TOKEN);

		if (atoken != null && !atoken.equals("")) {
			try {
				mAuthToken = AuthToken.getAuthToken(atoken);
				if (mAuthToken.isExpired())
					throw ServiceException.AUTH_EXPIRED();
				mAuthTokenAccountId = mAuthToken.getAccountId();
				
				if (mRequestedAccountId != null && 
				        !mAuthTokenAccountId.equals(mRequestedAccountId) && 
				        !mAuthToken.isAdmin()) { 
			    	throw ServiceException.PERM_DENIED("requested id not the same as auth token id and not an admin");
				}
			} catch (AuthTokenException e) {
				// ignore and leave null
				mAuthToken = null;
				if (sLog.isDebugEnabled())
					sLog.debug("ZimbraContext AuthToken error: " + e.getMessage(), e);
			}
		}

        Element change = (ctxt == null ? null : ctxt.getOptionalElement(E_CHANGE));
        if (change != null)
            try {
                mMaximumChangeId = (int) change.getAttributeLong(A_CHANGE_ID, -1);
                if (CHANGE_MODIFIED.equals(change.getAttribute(A_TYPE, CHANGE_MODIFIED)))
                    mChangeConstraintType = OperationContext.CHECK_MODIFIED;
                else
                    mChangeConstraintType = OperationContext.CHECK_CREATED;
            } catch (ServiceException e) { }

        mIsProxyRequest = false;
        String targetServerId = ctxt == null ? null : ctxt.getAttribute(E_TARGET_SERVER, null);
        if (targetServerId != null) {
            HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            if (req != null) {
            	mProxyTarget = new ProxyTarget(targetServerId, atoken, req);
                mIsProxyRequest = mProxyTarget != null && !mProxyTarget.isTargetLocal();
            } else
                sLog.warn("Missing SERVLET_REQUEST key in request context");
        }

        // record session-related info and validate any specified sessions
        //   (don't create new sessions yet)
        if (ctxt != null) {
            mHaltNotifications = ctxt.getOptionalElement(E_NO_NOTIFY) != null;
            for (Iterator it = ctxt.elementIterator(E_SESSION_ID); it.hasNext(); ) {
                // they specified it, so create a SessionInfo and thereby ping the session as a keepalive
                parseSessionElement((Element) it.next());
            }
            mSessionSuppressed = ctxt.getOptionalElement(E_NO_SESSION) != null;
        }
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
            sessionId = elt.getAttribute(A_ID, null);
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
                if (mHaltNotifications || !elt.getAttributeBool(A_NOTIFY, true))
                    ((SoapSession) session).haltNotifications();
        } catch (ServiceException e) { }

        mSessionInfo.add(new SessionInfo(session));
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
        OperationContext octxt = new OperationContext(mAuthTokenAccountId);
        octxt.setChangeConstraint(mChangeConstraintType, mMaximumChangeId);
        return octxt;
    }

	/** @return the account id the request is supposed to operate on.
     *  If accountId is not present in the context, use the account id in the auth token. */
	public String getRequestedAccountId() {
	    return (mRequestedAccountId != null ? mRequestedAccountId : mAuthTokenAccountId);
	} 

    /** @return always returns the account in the auth token.  (should normally use getRequestAccountId) */
    public String getAuthtokenAccountId() {
        return mAuthTokenAccountId;
    } 

    /** Gets an existing valid {@link SessionInfo} item of the specified type.<p>
     * 
     *  SessionInfo objects correspond to either:<ul>
     *  <li>existing, unexpired sessions specified in a <code>&lt;sessionId></code>
     *      element in the <code>&lt;context></code> SOAP header block, or</li>
     *  <li>new sessions created during the course of the SOAP call</li></ul>
     * 
     * @param type  One of the types defined in the {@link SessionCache} class.
     * @return A matching SessionInfo object or <code>null</code>. */
    private SessionInfo findSessionInfo(int type) {
        for (Iterator it = mSessionInfo.iterator(); it.hasNext(); ) {
            SessionInfo sinfo = (SessionInfo) it.next();
            if (sinfo.session.getSessionType() == type)
                return sinfo;
        }
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
            for (Iterator it = mSessionInfo.iterator(); it.hasNext(); ) {
                SessionInfo sinfo = (SessionInfo) it.next();
                Session session = sinfo.session;
                if (ctxt == null)
                    ctxt = createElement(CONTEXT);

                // session ID is valid, so ping it back to the client:
                encodeSession(ctxt, session, false);
                // put <refresh> blocks back for any newly-created SoapSession objects
                if (sinfo.created && session instanceof SoapSession)
                    ((SoapSession) session).putRefresh(ctxt, this);
                // put <notify> blocks back for any SoapSession objects
                if (session instanceof SoapSession)
                    ((SoapSession) session).putNotifications(this, ctxt);
            }
//            if (ctxt != null && mAuthToken != null)
//                ctxt.addAttribute(E_AUTH_TOKEN, mAuthToken.toString(), Element.DISP_CONTENT);
            return ctxt;
        } catch (ServiceException e) {
            sLog.info("ServiceException while putting soap session refresh data", e);
            return null;
        }
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
            sessionType = SESSION_ADMIN;

        Element eSession = unique ? parent.addUniqueElement(E_SESSION_ID) : parent.addElement(E_SESSION_ID);
        eSession.addAttribute(A_TYPE, sessionType).addAttribute(A_ID, session.getSessionId())
                .setText(session.getSessionId());
        return eSession;
    }

    /** Creates a SOAP request <code>&lt;context></code> {@link Element}.<p>
     * 
     *  All requests except Auth and a few others must specify an auth token.
     *  If noSession is true, the server will not create a session and any
     *  sessionId specified will be ignored.
     * 
     * @param protocol   The markup to use when creating the <code>context</code>.
     * @param authToken  The serialized authorization token for the user.
     * @param noSession  Whether to suppress the default new session creation.
     * @return A new <code>context</code> Element in the appropriate markup. */
	static Element toCtxt(SoapProtocol protocol, String authToken, boolean noSession) {
		Element ctxt = protocol.getFactory().createElement(CONTEXT);
		if (authToken != null)
			ctxt.addAttribute(E_AUTH_TOKEN, authToken, com.zimbra.cs.service.Element.DISP_CONTENT);
        if (noSession)
            ctxt.addUniqueElement(E_NO_SESSION);
		return ctxt;
	}

    /** Adds session information to a <code>&lt;context></code> {@link Element}
     *  created by a call to {@link #toCtxt}.  By default, the server creates
     *  a session for the client unless a valid sessionId is specified referring
     *  to an existing session.<p>
     * 
     *  By default, all sessions have change notification enabled.  Passing true
     *  for noNotify disables notification for the specified session.<p>
     * 
     *  No changes to the context occur if no auth token is present.
     * @param ctxt       A <code>&lt;context></code> Element as created by toCtxt.
     * @param sessionId  The ID of the session to add to the <code>&lt;context></code>
     * @param noNotify   Whether to suppress notification on the session from here out.
     * 
     * @return The passed-in <code>&lt;context></code> Element.
     * @see #toCtxt */
    static Element addSessionToCtxt(Element ctxt, String sessionId, boolean noNotify) {
        if (ctxt == null || sessionId == null || sessionId.trim().equals(""))
            return ctxt;
        if (ctxt.getAttribute(E_AUTH_TOKEN, null) == null)
            return ctxt;

        Element eSession = ctxt.addElement(E_SESSION_ID).addAttribute(A_ID, sessionId)
                               .setText(sessionId);
        if (noNotify)
            eSession.addAttribute(A_NOTIFY, false);
        return ctxt;
    }

    /** Creates a SOAP request <code>&lt;context></code> {@link Element} with
     *  an associated session.  (All requests except Auth and a few others must
     *  specify an auth token.)
     * 
     * @param protocol   The markup to use when creating the <code>context</code>.
     * @param authToken  The serialized authorization token for the user.
     * @param sessionId  The ID of the session to add to the <code>context</code>.
     * @return A new <code>context</code> Element in the appropriate markup.
     * @see #toCtxt(SoapProtocol, String, boolean) */
    public static Element toCtxt(SoapProtocol protocol, String authToken, String sessionId) {
    	Element ctxt = toCtxt(protocol, authToken, false);
        return addSessionToCtxt(ctxt, sessionId, false);
    }

    public Element createElement(String name)  { return mResponseProtocol.getFactory().createElement(name); }
    public Element createElement(QName qname)  { return mResponseProtocol.getFactory().createElement(qname); }
    public SoapProtocol getResponseProtocol()  { return mResponseProtocol; }

    /** Returns the {@link AuthToken} for this SOAP request.
     * 
     * @return The parsed AuthToken object for the auth token, either from a cookie
     *         in the SOAP request or from an <code>&lt;authToken></code> element
     *         in the SOAP header. */
    public AuthToken getAuthToken() {
		return mAuthToken;
	}

    public ProxyTarget getProxyTarget() {
    	return mProxyTarget;
    }

    public boolean isProxyRequest() {
        return mIsProxyRequest;
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
        return new ItemId(item).toString(this);
    }

    /** Formats the ({@link Mailbox}, ID) pair into a <code>String</code>
     *  that's addressable by the request's originator.  In other words, if
     *  the owner of the <code>Mailbox</code> matches the auth token's
     *  principal, you just get a bare ID.  But if the owners don't match,
     *  you get a formatted ID that refers to the <code>Mailbox</code> as
     *  well as the item in question.
     * 
     * @param mbox    The item's containing Mailbox.
     * @param itemId  The item's (local) ID.
     * @see ItemId */
    public String formatItemId(Mailbox mbox, int itemId) {
        return new ItemId(mbox, itemId).toString(this);
    }
}
