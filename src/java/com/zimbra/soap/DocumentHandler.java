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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.RemoteSoapSession;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.ZimbraSoapContext.SessionInfo;

import org.dom4j.QName;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @since May 26, 2004
 * @author schemers
 */
public abstract class DocumentHandler {

    private QName mResponseQName;
    private static String LOCAL_HOST = "", LOCAL_HOST_ID = "";

    void setResponseQName(QName response) { mResponseQName = response; }

    protected Element getResponseElement(ZimbraSoapContext zc) {
        return zc.createElement(mResponseQName);
    }

    public static String getLocalHost() {
        synchronized(LOCAL_HOST) {
            if (LOCAL_HOST.length() == 0) {
                try {
                    Server localServer = Provisioning.getInstance().getLocalServer();
                    LOCAL_HOST = localServer.getAttr(Provisioning.A_zimbraServiceHostname);
                    LOCAL_HOST_ID = localServer.getId();
                } catch (Exception e) {
                    Zimbra.halt("could not fetch local server name from LDAP for request proxying");
                }
            }
        }
        return LOCAL_HOST;
    }

    public static String getLocalHostId() {
        if (LOCAL_HOST_ID.length() == 0)
            getLocalHost();
        return LOCAL_HOST_ID;
    }

    public void preProxy(Element request, Map<String, Object> context) throws ServiceException {}

    public void postProxy(Element request, Element response, Map<String, Object> context) throws ServiceException {}

    public abstract Element handle(Element request, Map<String, Object> context) throws ServiceException;


    /** Returns the {@link ZimbraSoapContext} object encapsulating the
     *  containing SOAP request's <pre>&lt;context></pre> header element. */
    public static ZimbraSoapContext getZimbraSoapContext(Map<String, Object> context) {
        return (ZimbraSoapContext) context.get(SoapEngine.ZIMBRA_CONTEXT);
    }

    /** Generates a new {@link com.zimbra.cs.mailbox.OperationContext}
     *  object reflecting the constraints serialized in the <tt>&lt;context></tt>
     *  element in the SOAP header.<p>
     *
     *  These optional constraints include:<ul>
     *  <li>the account ID from the auth token</li>
     *  <li>the highest change number the client knows about</li>
     *  <li>how stringently to check accessed items against the known change
     *      highwater mark</li></ul>
     *
     * @return A new OperationContext object */
    public static OperationContext getOperationContext(ZimbraSoapContext zsc, Map<String, Object> context) throws ServiceException {
        return getOperationContext(zsc, context == null ? null : (Session) context.get(SoapEngine.ZIMBRA_SESSION));
    }

    public static OperationContext getOperationContext(ZimbraSoapContext zsc, Session session) throws ServiceException {
        AuthToken at = zsc.getAuthToken();
        OperationContext octxt = new OperationContext(at);
        octxt.setChangeConstraint(zsc.getChangeConstraintType(), zsc.getChangeConstraintLimit());
        octxt.setRequestIP(zsc.getRequestIP()).setSession(session);
        octxt.setUserAgent(zsc.getUserAgent());
        return octxt;
    }

    /** Returns the {@link Account} corresponding to the authenticated user.
     *  The authenticated user is determined from the serialized
     *  {@link com.zimbra.cs.account.AuthToken} in the SOAP request's
     *  <pre>&lt;context></pre> header element. */
    public static Account getAuthenticatedAccount(ZimbraSoapContext zsc) throws ServiceException {
        String id = zsc.getAuthtokenAccountId();
        AuthToken at = zsc.getAuthToken();

        if (GuestAccount.GUID_PUBLIC.equals(id) || (at != null && !at.isZimbraUser()))
            return new GuestAccount(at);

        Account acct = Provisioning.getInstance().get(AccountBy.id, id, zsc.getAuthToken());
        if (acct == null)
            throw ServiceException.AUTH_REQUIRED();
        return acct;
    }

    public static Account getRequestedAccount(ZimbraSoapContext zsc) throws ServiceException {
        String id = zsc.getRequestedAccountId();

        Account acct = Provisioning.getInstance().get(AccountBy.id, id, zsc.getAuthToken());
        if (acct == null) {
            if (zsc.isDelegatedRequest())
                throw ServiceException.DEFEND_ACCOUNT_HARVEST(id);
            else
                throw ServiceException.AUTH_EXPIRED();
        }

        return acct;
    }

    public static Mailbox getRequestedMailbox(ZimbraSoapContext zsc) throws ServiceException {
        String id = zsc.getRequestedAccountId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(id);
        if (mbox != null)
            ZimbraLog.addMboxToContext(mbox.getId());
        return mbox;
    }

    private static boolean isRequestLocal(ZimbraSoapContext zsc) {
        try {
            Account acct = getAuthenticatedAccount(zsc);
            return (acct != null && Provisioning.onLocalServer(acct));
        } catch (ServiceException e) {
            ZimbraLog.session.info("error determining whether authenticated account is local", e);
            return false;
        }
    }


    /** Returns whether the command's caller must be authenticated. */
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    /** Returns whether this is an administrative command (and thus requires
     *  a valid admin auth token). */
    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

    public Boolean canAccessAccountCommon(ZimbraSoapContext zsc, Account target, boolean allowSelf) throws ServiceException {
        if (zsc.getAuthtokenAccountId() == null || target == null)
            return Boolean.FALSE;

        if (allowSelf && target.getId().equals(zsc.getAuthtokenAccountId()))
            return Boolean.TRUE;

        // 1. delegated auth case has been logged in SoapEngine
        // 2. we do not want to log delegated request, where the target account is specified in
        //    soap context header.  Usages for that route are family mailboxes and sharing access.
        //    we only want to log the "admin" accesses.
        if (!zsc.getAuthToken().isDelegatedAuth() && !zsc.isDelegatedRequest())
            logAuditAccess(null, zsc.getAuthtokenAccountId(), target.getId());

        return null;
    }

    public boolean canAccessAccount(ZimbraSoapContext zsc, Account target) throws ServiceException {
        Boolean canAccess = canAccessAccountCommon(zsc, target, true);
        if (canAccess != null)
            return canAccess.booleanValue();
        return AccessManager.getInstance().canAccessAccount(zsc.getAuthToken(), target);
    }

    public boolean canModifyOptions(ZimbraSoapContext zsc, Account acct) throws ServiceException {
        if (zsc.isDelegatedRequest()) {
            // if we're modifying someone else's options, we need to have admin access to the account
            //   *and* we need to be able to change our own options (this is a standin for finer-grained access control)
            return canAccessAccount(zsc, acct) && getAuthenticatedAccount(zsc).getBooleanAttr(Provisioning.A_zimbraFeatureOptionsEnabled, true);
        } else {
            // if we're modifying our own options, we just need the appropriate feature enabled
            return acct.getBooleanAttr(Provisioning.A_zimbraFeatureOptionsEnabled, true);
        }
    }

    /** Returns whether domain admin auth is sufficient to run this command.
     *  This should be overriden only on admin commands that can be run in a
     *  restricted "domain admin" mode. */
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return false;
    }

    /** Returns whether the command is in the administration command set. */
    public boolean isAdminCommand() {
        return false;
    }

    /** Returns <tt>true</tt> if the operation is read-only, or
     *  <tt>false</tt> if the operation causes backend state change. */
    public boolean isReadOnly() {
        return true;
    }

    /** Returns whether the client making the SOAP request is localhost. */
    protected boolean clientIsLocal(Map<String, Object> context) {
        HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        return (req == null ? true : "127.0.0.1".equals(req.getRemoteAddr()));
    }

    /** Updates the {@link ZimbraSoapContext} to treat the specified account
     *  as the request's authenticated account.  If the new account differs
     *  from the previously authenticated account, we forget about all other
     *  {@link Session}s.  (Those sessions are not deleted from the cache,
     *  though perhaps that's the right thing to do...)  If requested, also
     *  creates a new Session object associated with the given account.
     *
     * @param zsc         The parsed SOAP header for the auth request.
     * @param authToken   The new auth token created for the user.
     * @param context     The <code>SoapEngine</code>'s request state.
     * @param getSession  Whether to try to generate a new Session.
     * @return A new Session object of the appropriate type, or <tt>null</tt>. */
    public Session updateAuthenticatedAccount(ZimbraSoapContext zsc, AuthToken authToken, Map<String, Object> context, boolean getSession) {
        String oldAccountId = zsc.getAuthtokenAccountId();
        String accountId = authToken.getAccountId();
        if (accountId != null && !accountId.equals(oldAccountId))
            zsc.clearSessionInfo();
        zsc.setAuthToken(authToken);

        Session session = (getSession ? getSession(zsc) : null);
        if (context != null)
            context.put(SoapEngine.ZIMBRA_SESSION, session);
        return session;
    }

    public static Session getReferencedSession(ZimbraSoapContext zsc) {
        if (zsc == null)
            return null;
        SessionInfo sinfo = zsc.getSessionInfo();
        return sinfo == null ? null : SessionCache.lookup(sinfo.sessionId, zsc.getAuthtokenAccountId());
    }

    public Session.Type getDefaultSessionType() {
        return Session.Type.SOAP;
    }

    protected final Session getSession(ZimbraSoapContext zsc, Map<String, Object> context) {
        Session session = (Session) context.get(SoapEngine.ZIMBRA_SESSION);
        return (session != null ? session : getSession(zsc));
    }

    /** Fetches the in-memory {@link Session} object appropriate for this
     *  request.  If none already exists, one is created if possible.
     *
     * @param zsc The encapsulation of the SOAP request's <tt>&lt;context</tt>
     *            element.
     * @return A {@link com.zimbra.cs.session.SoapSession}, or <tt>null</tt>. */
    protected final Session getSession(ZimbraSoapContext zsc) {
        return getSession(zsc, getDefaultSessionType());
    }

    /** Fetches a {@link Session} object to persist and manage state between
     *  SOAP requests.  If no appropriate session already exists, a new one
     *  is created if possible.
     *
     * @param zsc The encapsulation of the SOAP request's <tt>&lt;context</tt>
     *            element.
     * @param stype  The type of session needed.
     * @return An in-memory {@link Session} object of the specified type,
     *         referenced by the request's {@link ZimbraSoapContext} object,
     *         or <tt>null</tt>.
     * @see SessionCache#SESSION_SOAP
     * @see SessionCache#SESSION_ADMIN */
    protected Session getSession(ZimbraSoapContext zsc, Session.Type stype) {
        if (zsc == null || stype == null || !zsc.isNotificationEnabled())
            return null;
        String authAccountId = zsc.getAuthtokenAccountId();
        if (authAccountId == null)
            return null;

        // if they asked for a SOAP session on a remote host and it's a non-proxied request, we don't notify
        boolean isLocal = isRequestLocal(zsc);
        if (stype == Session.Type.SOAP && !isLocal && !zsc.isSessionProxied())
            return null;

        Session s = null;

        // if the caller referenced a session of this type, fetch it from the session cache
        SessionInfo sinfo = zsc.getSessionInfo();
        if (sinfo != null) {
            s = SessionCache.lookup(sinfo.sessionId, authAccountId);
            if (s == null) {
                // purge dangling references from the context's list of referenced sessions
                ZimbraLog.session.info("requested session no longer exists: " + sinfo.sessionId);
                zsc.clearSessionInfo();
            } else if (s.getSessionType() != stype) {
                // only want a session of the appropriate type
                s = null;
            }
        }

        // if there's no valid referenced session, create a new session of the requested type
        if (s == null) {
            try {
                if (stype == Session.Type.SOAP) {
                    if (isLocal)
                        s = new SoapSession(authAccountId).register();
                    else
                        s = new RemoteSoapSession(authAccountId).register();
                } else if (stype == Session.Type.ADMIN) {
                    s = new AdminSession(authAccountId).register();
                }
            } catch (ServiceException e) {
                ZimbraLog.session.info("exception while creating session", e);
            }
            if (s != null)
                zsc.recordNewSession(s.getSessionId());
        }

        // if it's a delegated request, try to get a session on the local requested mailbox
        //   (note that if the requested account is remote, getDelegateSession returns null)
        if (s instanceof SoapSession && zsc.isDelegatedRequest()) {
            Session delegate = ((SoapSession) s).getDelegateSession(zsc.getRequestedAccountId());
            if (delegate != null)
                s = delegate;
        }

        return s;
    }

    /**
     * End the session immediately, removing it from the session cache and cleaning it up
     *
     * @param s
     */
    protected void endSession(Session s) {
        SessionCache.clearSession(s);
    }


    /** Returns the {@link Server} object where an Account (specified by ID)
     *  is homed.  This is similar to {@link Provisioning#getServer(Account),
     *  except that the account is specified by ID and exceptions are thrown
     *  on failure rather than returning null.
     *
     * @param acctId  The Zimbra ID of the account.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><tt>account.NO_SUCH_ACCOUNT</tt> - if there is no Account
     *        with the specified ID
     *    <li><tt>account.PROXY_ERROR</tt> - if the Server associated
     *        with the Account does not exist</ul> */
    protected static Server getServer(String acctId) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.id, acctId);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);

        String hostname = acct.getAttr(Provisioning.A_zimbraMailHost);
        if (hostname == null)
            throw ServiceException.PROXY_ERROR(AccountServiceException.NO_SUCH_SERVER(""), "");
        Server server = Provisioning.getInstance().get(ServerBy.name, hostname);
        if (server == null)
            throw ServiceException.PROXY_ERROR(AccountServiceException.NO_SUCH_SERVER(hostname), "");
        return server;
    }

    protected static String getXPath(Element request, String[] xpath) {
        int depth = 0;
        while (depth < xpath.length - 1 && request != null)
            request = request.getOptionalElement(xpath[depth++]);
        return (request == null ? null : request.getAttribute(xpath[depth], null));
    }

    protected static Element getXPathElement(Element request, String[] xpath) {
        int depth = 0;
        while (depth < xpath.length && request != null)
            request = request.getOptionalElement(xpath[depth++]);
        return request;
    }

    protected static void setXPath(Element request, String[] xpath, String value) throws ServiceException {
        if (xpath == null || xpath.length == 0)
            return;
        int depth = 0;
        while (depth < xpath.length - 1 && request != null)
            request = request.getOptionalElement(xpath[depth++]);
        if (request == null)
            throw ServiceException.INVALID_REQUEST("could not find path", null);
        request.addAttribute(xpath[depth], value);
    }

    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        // if the "target account" is remote and the command is non-admin, proxy.
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String acctId = zsc.getRequestedAccountId();
        if (acctId != null && zsc.getProxyTarget() == null && !isAdminCommand() && !Provisioning.onLocalServer(getRequestedAccount(zsc)))
            return proxyRequest(request, context, acctId);

        return null;
    }

    protected Element proxyRequest(Element request, Map<String, Object> context, String acctId) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        // new context for proxied request has a different "requested account"
        // and an incremented hop count
        ZimbraSoapContext zscTarget = new ZimbraSoapContext(zsc, acctId);

        return proxyRequest(request, context, getServer(acctId), zscTarget);
    }


    protected Element proxyRequest(Element request, Map<String, Object> context, Server server)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        // new context for proxied request has an incremented hop count
        ZimbraSoapContext pxyCtxt = new ZimbraSoapContext(zsc);
        return proxyRequest(request, context, server, pxyCtxt);
    }

    protected Element proxyRequest(Element request, Map<String, Object> context, Server server, ZimbraSoapContext zsc)
    throws ServiceException {
        // figure out whether we can just re-dispatch or if we need to proxy via HTTP
        SoapEngine engine = (SoapEngine) context.get(SoapEngine.ZIMBRA_ENGINE);
        boolean isLocal = getLocalHostId().equalsIgnoreCase(server.getId());

        if (isLocal)
            zsc.resetProxyAuthToken();

        Element response = null;
        request.detach();
        if (isLocal && engine != null) {
            // executing on same server; just hand back to the SoapEngine
            Map<String, Object> contextTarget = new HashMap<String, Object>(context);
            contextTarget.put(SoapEngine.ZIMBRA_ENGINE, engine);
            contextTarget.put(SoapEngine.ZIMBRA_CONTEXT, zsc);
            response = engine.dispatchRequest(request, contextTarget, zsc);
            if (zsc.getResponseProtocol().isFault(response)) {
                zsc.getResponseProtocol().updateArgumentsForRemoteFault(response, zsc.getRequestedAccountId());
                throw new SoapFaultException("error in proxied request", true, response);
            }
        } else {
            // do any necessary operations before doing a cross-server proxy
            preProxy(request, context);
            // executing remotely; find our target and proxy there
            HttpServletRequest httpreq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            ProxyTarget proxy = new ProxyTarget(server.getId(), zsc.getAuthToken(), httpreq);
            response = proxyWithNotification(request, proxy, zsc, (Session) context.get(SoapEngine.ZIMBRA_SESSION));
            // do any necessary operations after doing a cross-server proxy
            postProxy(request, response, context);
        }
        return response;
    }

    public static Element proxyWithNotification(Element request, ProxyTarget proxy, ZimbraSoapContext zscProxy, ZimbraSoapContext zscInbound)
    throws ServiceException {
        return proxyWithNotification(request, proxy, zscProxy, getReferencedSession(zscInbound));
    }

    public static Element proxyWithNotification(Element request, ProxyTarget proxy, ZimbraSoapContext zscProxy, Session localSession)
    throws ServiceException {
        Server server = proxy.getServer();
        boolean isLocal = getLocalHostId().equalsIgnoreCase(server.getId());

        if (isLocal)
            zscProxy.resetProxyAuthToken();

        if (zscProxy.isNotificationEnabled()) {
            // if we've got a SOAP session, make sure to use the appropriate remote session ID
            if (localSession instanceof SoapSession.DelegateSession)
                localSession = ((SoapSession.DelegateSession) localSession).getParentSession();
            // note that requests proxied to *this same host* shouldn't request notification, as the existing local session collects the notifications
            if (!(localSession instanceof SoapSession) || localSession.getMailbox() == null)
                zscProxy.disableNotifications();
            else if (!isLocal)
                zscProxy.setProxySession(((SoapSession) localSession).getRemoteSessionId(server));
            else
                zscProxy.setProxySession(localSession.getSessionId());
        }

        Pair<Element, Element> envelope = proxy.execute(request, zscProxy);
        // if we've got a SOAP session, handle the returned notifications and session ID
        if (localSession instanceof SoapSession && zscProxy.isNotificationEnabled())
            ((SoapSession) localSession).handleRemoteNotifications(server, envelope.getFirst());
        return envelope.getSecond().detach();
    }

    /**
     * This is for logging usage only:
     *     returns the account name if the account can be found by acctId,
     *     otherwise returns the acctId if not null,
     *     otherwise returns empty string.
     *
     * @param prov
     * @param acctId
     * @return
     */
    private String getAccountLogName(Provisioning prov, String acctId) {
        if (acctId == null)
            return "";

        try {
            Account acct = prov.get(AccountBy.id, acctId);
            if (acct != null)
                return acct.getName();
        } catch (ServiceException e) { }

        return acctId;
    }

    public void logAuditAccess(String delegatingAcctId, String authedAcctId, String targetAcctId) {
        if (!ZimbraLog.misc.isInfoEnabled())
            return;

        // 8 => "Response".length()
        String reqName = mResponseQName.getQualifiedName().substring(0, mResponseQName.getQualifiedName().length()-8);

        Provisioning prov = Provisioning.getInstance();
        String delegatingAcctName = getAccountLogName(prov, delegatingAcctId);
        String authedAcctName = getAccountLogName(prov, authedAcctId);
        String targetAcctName = getAccountLogName(prov, targetAcctId);

        ZimbraLog.misc.info("delegated access: doc=" + reqName +
            (delegatingAcctId==null?"" : ", delegating account="+delegatingAcctName) +
            ", authenticated account=" + authedAcctName +
            ", target account=" + targetAcctName);
    }
}
