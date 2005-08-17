/*
 * Created on May 29, 2004
 */
package com.zimbra.soap;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.QName;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.AuthToken;
import com.liquidsys.coco.account.AuthTokenException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.session.Session;
import com.liquidsys.coco.session.SoapSession;
import com.liquidsys.coco.session.SessionCache;


/**
 * @author schemers
 */
public class LiquidContext {

	private static Log mLog = LogFactory.getLog(LiquidContext.class);

	public static final QName CONTEXT = QName.get("context", LiquidNamespace.LIQUID);
    public static final String E_NO_NOTIFY  = "nonotify";
    public static final String E_FORMAT     = "format";
    public static final String A_TYPE       = "type";
	public static final String E_AUTH_TOKEN = "authToken";
	public static final String A_BY         = "by";
    public static final String E_ACCOUNT    = "account";   
    public static final String E_NO_SESSION = "nosession";
    public static final String E_SESSION_ID = "sessionId";
    public static final String E_CHANGE     = "change";
    public static final String A_CHANGE_ID  = "token";
    public static final String E_TARGET_SERVER = "targetServer";

    public static final String BY_NAME = "name";
    public static final String BY_ID   = "id";
    public static final String TYPE_XML        = "xml";
    public static final String TYPE_JAVASCRIPT = "js";
    public static final String CHANGE_MODIFIED = "mod";
    public static final String CHANGE_CREATED  = "new";

	public static final long UNSPECIFIED_MAILBOX_ID = 0;

    public static final byte MARKUP_XML        = 0;
    public static final byte MARKUP_JAVASCRIPT = 1;

	private AuthToken mAuthToken;
	private String    mAuthTokenAccountId;
	private String    mRequestedAccountId;
    private boolean   mChangeConstraintType = OperationContext.CHECK_MODIFIED;
    private int       mMaximumChangeId = -1;
	private SoapSession mSession = null;
	private boolean   mHasNewSessionId;
    private boolean   mSessionSuppressed;
    private ProxyTarget mProxyTarget;
    private boolean   mIsProxyRequest;
    private SoapProtocol mResponseProtocol;

	public String toString() {
	    return "LC(mbox=" + mAuthTokenAccountId + ", session=" + getSessionIdStr() + ")";
	}

	/**
	 * @param ctxt can be null if not present in request
	 * @param context the engine context, which might have the auth token in it
	 * @param requestProtocol TODO
	 * @throws ServiceException
	 */
	public LiquidContext(Element ctxt, Map context, SoapProtocol requestProtocol) throws ServiceException {
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
		    atoken = (String) context.get(SoapServlet.LIQUID_AUTH_TOKEN);

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
				if (mLog.isDebugEnabled())
					mLog.debug("LiquidContext AuthToken error: " + e.getMessage(), e);
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
                mLog.warn("Missing SERVLET_REQUEST key in request context");
        }

        mSessionSuppressed = ctxt == null ? false : ctxt.getOptionalElement(E_NO_SESSION) != null;
        if (mAuthTokenAccountId != null && !mSessionSuppressed) {
			String sessionStr = ctxt == null ? null : ctxt.getAttribute(E_SESSION_ID, null);
			if (sessionStr != null) {
			    Long sid = new Long(sessionStr);
                Session s = SessionCache.getInstance().lookup(sid, mAuthTokenAccountId);
                if (s instanceof SoapSession)
                	mSession = (SoapSession) s;
			}
			if (mSession == null) {
			    mSession = (SoapSession) SessionCache.getInstance().getNewSession(mAuthTokenAccountId, SessionCache.SESSION_SOAP);
			    mHasNewSessionId = true;
			}
            if (mSession != null && ctxt != null && ctxt.getOptionalElement(E_NO_NOTIFY) != null)
                mSession.haltNotifications();
		}
    }

    public OperationContext getOperationContext() throws ServiceException {
        OperationContext octxt = new OperationContext(mAuthTokenAccountId);
        octxt.setChangeConstraint(mChangeConstraintType, mMaximumChangeId);
        return octxt;
    }

	/**
	 * @return the account id the request is supposed to operate on. If accountId is not present in the context, use
	 * the account id in the auth token.
	 */
	public String getRequestedAccountId() {
	    if (mRequestedAccountId != null)
	        return mRequestedAccountId;
	    else
	        return mAuthTokenAccountId;
	} 

    /**
     * @return always returns the account in the auth token. Should normally use getRequestAccountId.
     */
    public String getAuthtokenAccountId() {
        return mAuthTokenAccountId;
    } 

	/**
	 * @return true if the session ID is changed, and therefore has to be sent to the client
	 */
	public boolean hasNewSessionId() { return mHasNewSessionId; };

    public boolean sessionSuppressed() { return mSessionSuppressed; };

    public String getSessionIdStr() { return mSession != null ? mSession.getSessionId().toString() : null; } 
	
	public SoapSession getSession() { return mSession; }
	
	private LiquidContext() {
	}

	public Element newSessionResponse(boolean includeRefreshBlock) {
        assert(mAuthToken != null && mSession != null);
	    Element ctxt = toResponseCtxt(mResponseProtocol, null, mSession.getSessionId().toString());
        try {
            if (includeRefreshBlock)
                mSession.putRefresh(ctxt);
            return ctxt;
        } catch (ServiceException e) {
            mLog.info("ServiceException while putting soap session refresh data", e);
            return null;
        }
	}

    /**
     * Creates a SOAP request context.  All requests except Auth and a few
     * others must specify an auth token.
     * 
     * By default server will create a session for the client unless a valid
     * sessionId is specified referring to an existing session.  If noSession
     * is true, server will not create a session, and any sessionId specified
     * will be ignored.  sessionId is also ignored when no auth token is
     * present.
     * 
     * By default a session has change notification enabled.  Passing true for
     * noNotify disables notification for the specified session.  noNotify is
     * meaningless when no session is specified.
     * 
     * @param authToken
     * @param noSession if true, don't create/use session
     * @param sessionId used iff noSession == false
     * @param noNotify if true, don't do notification for this session
     *                 used iff noSession == false
     * @return
     */
	static Element toCtxt(SoapProtocol protocol,
                          String authToken,
                          boolean noSession,
                          String sessionId,
                          boolean noNotify)  {
		Element ctxt = protocol.getFactory().createElement(CONTEXT);
		if (authToken != null)
			ctxt.addAttribute(E_AUTH_TOKEN, authToken, com.liquidsys.coco.service.Element.DISP_CONTENT);

        if (noSession)
            ctxt.addUniqueElement(E_NO_SESSION);
        else {
            if (sessionId != null && authToken != null)
                ctxt.addAttribute(E_SESSION_ID, sessionId, Element.DISP_CONTENT);
            if (noNotify)
                ctxt.addUniqueElement(E_NO_NOTIFY);
        }

		//if (mMailboxId != UNSPECIFIED_MAILBOX_ID)
		//	DomUtil.add(ctxt, E_ACCOUNT, mMailboxId);
		return ctxt;
	}

    public Element createElement(String name)  { return mResponseProtocol.getFactory().createElement(name); }
    public Element createElement(QName qname)  { return mResponseProtocol.getFactory().createElement(qname); }
    public SoapProtocol getResponseProtocol()  { return mResponseProtocol; }

    /**
     * Creates a SOAP request context that uses an existing session.
     * Notification is enabled for the session.
     * @param authToken
     * @param sessionId
     * @return
     */
    public static Element toCtxt(SoapProtocol protocol, String authToken, String sessionId) {
    	return toCtxt(protocol, authToken, false, sessionId, false);
    }

    /**
     * Creates a SOAP request context that asks the server not to create a
     * session.  This is appropriate for one-shot requests.
     * @param authToken
     * @return
     */
    public static Element toCtxtWithoutSession(SoapProtocol protocol, String authToken) {
    	return toCtxt(protocol, authToken, true, null, false);
    }

    /**
     * Creates a SOAP request context that uses an existing session.
     * Disable notification for the session.
     * @param authToken
     * @param sessionId
     * @return
     */
    public static Element toCtxtWithoutNotify(SoapProtocol protocol, String authToken, String sessionId) {
    	return toCtxt(protocol, authToken, false, sessionId, true);
    }

    /**
     * Creates a &lt;context&gt; element to be used in SOAP response header.
     * @param authToken
     * @param sessionId
     * @return
     */
    private static Element toResponseCtxt(SoapProtocol protocol, String authToken, String sessionId) {
    	Element ctxt = protocol.getFactory().createElement(CONTEXT);
        if (authToken != null)
            ctxt.addAttribute(E_AUTH_TOKEN, authToken, Element.DISP_CONTENT);
        if (sessionId != null)
            ctxt.addAttribute(E_SESSION_ID, sessionId, Element.DISP_CONTENT);
        return ctxt;
    }
	
	public AuthToken getAuthToken() {
		return mAuthToken;
	}

    public ProxyTarget getProxyTarget() {
    	return mProxyTarget;
    }

    public boolean isProxyRequest() {
        return mIsProxyRequest;
    }
}
