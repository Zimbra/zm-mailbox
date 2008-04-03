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

package com.zimbra.soap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.dom4j.DocumentException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.TrustedNetwork;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.ZimbraSoapContext.SessionInfo;

/**
 * The soap engine.
 */

public class SoapEngine {

    // attribute used to correlate requests and responses
    public static final String A_REQUEST_CORRELATOR = "requestId";

    public static final String ZIMBRA_CONTEXT = "zimbra.context";
    public static final String ZIMBRA_ENGINE  = "zimbra.engine";
    public static final String ZIMBRA_SESSION = "zimbra.session";

    /** context name of request IP
     * 
     *  It can be the IP of the SOAP client, or in the presence of a 
     *  real origin IP address http header(X-ORIGINATING-IP) the IP 
     *  of the real origin client if the SOAP client is in a trusted 
     *  network.
     */
    public static final String REQUEST_IP = "request.ip";
    
    /** context name of IP of the SOAP client */
    public static final String SOAP_REQUEST_IP = "soap.request.ip";

    /** set to true if we log the soap request */
    public static final String SOAP_REQUEST_LOGGED = "soap.request.logged";
    
    /** context name of IP of the origin client */
    public static final String ORIG_REQUEST_IP = "orig.request.ip";
    
	private static Log mLog = LogFactory.getLog(SoapEngine.class);

	private DocumentDispatcher mDispatcher;
    
    public SoapEngine() {
        mDispatcher = new DocumentDispatcher();
    }

    /*
     * returns a SOAP envelope with soap fault in the body
     * 
     * This is for callers that need to:
     *    - wrap a exception in a soap fault and wrap the soap fault in a soap envelope (without throwing the exception)
     *    - log the exception at info level
     */
    private Element soapFaultEnv(SoapProtocol soapProto, String msg, ServiceException e) {
        mLog.warn(msg, e);
        return soapProto.soapEnvelope(soapProto.soapFault(e));
    }
    
    /*
     * returns a SOAP fault
     * 
     * This is for callers that need to:
     *    - wrap a exception in a soap fault (without throwing the exception)
     *    - log the exception at info level
     */
    private Element soapFault(SoapProtocol soapProto, String msg, ServiceException e) {
        mLog.warn(msg, e);
        return soapProto.soapFault(e);
    }
    
    /*
     * This is for callers that need to:
     *    - enclose a SOAP fault in the envelope and return (without throwing the exception)
     *    - log the exception at debug level, which is not normally turned on
     *    - append a unique label to the exception id (exception id is always included in the <trace> 
     *      tag in the fault) so in the case when exception logging is not turned on we can identify 
     *      the location where the exception was thrown.
     */
    private Element soapFaultWithNotes(SoapProtocol soapProto, String msg, ServiceException e) {
        StackTraceElement[] s = Thread.currentThread().getStackTrace();
        StackTraceElement callSite = s[3]; // third frame from top is the caller
        e.setIdLabel(callSite);
        mLog.debug(msg, e);
        return soapProto.soapFault(e);
    }
    
    public Element dispatch(String path, byte[] soapMessage, Map<String, Object> context) {
        if (soapMessage == null || soapMessage.length == 0) {
            SoapProtocol soapProto = SoapProtocol.Soap12;
            return soapFaultEnv(soapProto, "SOAP exception", ServiceException.PARSE_ERROR("empty request payload", null));
        }
        
        InputStream in = new ByteArrayInputStream(soapMessage);
        Element document = null;
        try {
            if (soapMessage[0] == '<')
                document = Element.parseXML(in);
            else
                document = Element.parseJSON(in);
        } catch (DocumentException de) {
            // FIXME: have to pick 1.1 or 1.2 since we can't parse any
            SoapProtocol soapProto = SoapProtocol.Soap12;
            return soapFaultEnv(soapProto, "SOAP exception", ServiceException.PARSE_ERROR(de.getMessage(), de));
        } catch (SoapParseException e) {
            SoapProtocol soapProto = SoapProtocol.SoapJS;
            return soapFaultEnv(soapProto, "SOAP exception", ServiceException.PARSE_ERROR(e.getMessage(), e));
        }
        return dispatch(path, document, context);
    }

    /**
     * dispatch to the given serviceName the specified document,
     * which should be a soap envelope containing a document to 
     * execute.
     *
     * @param path  the path (i.e., /service/foo) of the service to dispatch to
     * @param envelope the top-level element of the message
     * @param context user context parameters
     * @param loggedRequest <tt>true</tt> if the SOAP message has already been logged
     * @return an XmlObject which is a SoapEnvelope containing the response
     *         
     */
    private Element dispatch(String path, Element envelope, Map<String, Object> context) {
    	// if (mLog.isDebugEnabled()) mLog.debug("dispatch(path, envelope, context: " + envelope.getQualifiedName());
    	
        SoapProtocol soapProto = SoapProtocol.determineProtocol(envelope);
        if (soapProto == null) {
            // FIXME: have to pick 1.1 or 1.2 since we can't parse any
            soapProto = SoapProtocol.Soap12;
            return soapFaultEnv(soapProto, "SOAP exception", ServiceException.INVALID_REQUEST("unable to determine SOAP version", null));
        }

        // if (mLog.isDebugEnabled()) mLog.debug("dispatch: soapProto = " + soapProto.getVersion());

        ZimbraSoapContext zsc = null;
        Element ectxt = soapProto.getHeader(envelope, HeaderConstants.CONTEXT);
        try {
            zsc = new ZimbraSoapContext(ectxt, context, soapProto);
        } catch (ServiceException e) {
            return soapFaultEnv(soapProto, "unable to construct SOAP context", e);
        }
        SoapProtocol responseProto = zsc.getResponseProtocol();

        String rid = zsc.getRequestedAccountId();
        if (rid != null) {
            Account.addAccountToLogContext(rid, ZimbraLog.C_NAME, ZimbraLog.C_ID, zsc.getAuthToken());
            String aid = zsc.getAuthtokenAccountId();
            if (aid != null && !rid.equals(aid))
                Account.addAccountToLogContext(aid, ZimbraLog.C_ANAME, ZimbraLog.C_AID, zsc.getAuthToken());
            else if (zsc.getAuthToken() != null && zsc.getAuthToken().getAdminAccountId() != null)
                Account.addAccountToLogContext(zsc.getAuthToken().getAdminAccountId(), ZimbraLog.C_ANAME, ZimbraLog.C_AID, zsc.getAuthToken());                        
        }
        
        String ip = (String) context.get(SOAP_REQUEST_IP);
        String origip = (String) context.get(ORIG_REQUEST_IP);
        
        // don't log ip if oip is present and ip is localhost
        if (ip != null && (!TrustedNetwork.isLocalhost(ip) || origip == null))
            ZimbraLog.addIpToContext(ip);
        
        if (origip != null)
            ZimbraLog.addOrigIpToContext(origip);
        
        if (zsc.getUserAgent() != null)
            ZimbraLog.addUserAgentToContext(zsc.getUserAgent());

        if (ZimbraLog.soap.isDebugEnabled()) {
            ZimbraLog.soap.debug("SOAP request:\n" + envelope.prettyPrint());
            context.put(SOAP_REQUEST_LOGGED, true);
        }

        context.put(ZIMBRA_CONTEXT, zsc);
        context.put(ZIMBRA_ENGINE, this);

        Element doc = soapProto.getBodyElement(envelope);

        if (mLog.isDebugEnabled())
            mLog.debug("dispatch: doc " + doc.getQualifiedName());

        Element responseBody = null;
        if (!zsc.isProxyRequest()) {
            // if the client's told us that they've seen through notification block 50, we can drop old notifications up to that point
            acknowledgeNotifications(zsc);

            if (doc.getQName().equals(ZimbraNamespace.E_BATCH_REQUEST)) {
                boolean contOnError = doc.getAttribute(ZimbraNamespace.A_ONERROR, ZimbraNamespace.DEF_ONERROR).equals("continue");
                responseBody = zsc.createElement(ZimbraNamespace.E_BATCH_RESPONSE);
                for (Element req : doc.listElements()) {
                    if (mLog.isDebugEnabled())
                        mLog.debug("dispatch: multi " + req.getQualifiedName());

                    String id = req.getAttribute(A_REQUEST_CORRELATOR, null);
                    Element br = dispatchRequest(req, context, zsc);
                    if (id != null)
                        br.addAttribute(A_REQUEST_CORRELATOR, id);
                    responseBody.addElement(br);
                    if (!contOnError && responseProto.isFault(br))
                        break;
                }
            } else {
                String id = doc.getAttribute(A_REQUEST_CORRELATOR, null);
                responseBody = dispatchRequest(doc, context, zsc);
                if (id != null)
                    responseBody.addAttribute(A_REQUEST_CORRELATOR, id);
            }
        } else {
            // Proxy the request to target server.  Proxy dispatcher
            // discards any session information with remote server.
            // We stick to local server's session when talking to the
            // client.
            try {
                // Detach doc from its current parent, because it will be
                // added as a child element of a new SOAP envelope in the
                // proxy dispatcher.  IllegalAddException will be thrown
                // if we don't detach it first.
                doc.detach();
                ZimbraSoapContext zscTarget = new ZimbraSoapContext(zsc, null);
                responseBody = zsc.getProxyTarget().dispatch(doc, zscTarget);
                responseBody.detach();
            } catch (SoapFaultException e) {
                responseBody = e.getFault() != null ? e.getFault().detach() : responseProto.soapFault(e); 
                mLog.debug("proxy handler exception", e);
            } catch (ServiceException e) {
                responseBody = responseProto.soapFault(e);
                mLog.info("proxy handler exception", e);
            } catch (Throwable e) {
                responseBody = responseProto.soapFault(ServiceException.FAILURE(e.toString(), e));
                if (e instanceof OutOfMemoryError)
                    Zimbra.halt("proxy handler exception", e);
                mLog.warn("proxy handler exception", e);
            }
        }

        // put notifications (new sessions and incremental change notifications)
        Element responseHeader = generateResponseHeader(zsc);

        Element response = responseProto.soapEnvelope(responseBody, responseHeader);

        return response;
    }

    /**
     * Handles individual requests, either direct or from a batch
     * @param request
     * @param context
     * @param zsc
     * @return
     */
    Element dispatchRequest(Element request, Map<String, Object> context, ZimbraSoapContext zsc) {
        long startTime = System.currentTimeMillis();
        SoapProtocol soapProto = zsc.getResponseProtocol();

        if (request == null)
            return soapFault(soapProto, "cannot dispatch request", ServiceException.INVALID_REQUEST("no document specified", null));

        DocumentHandler handler = mDispatcher.getHandler(request);
        if (handler == null) 
            return soapFault(soapProto, "cannot dispatch request", ServiceException.UNKNOWN_DOCUMENT(request.getQualifiedName(), null));

        if (RedoLogProvider.getInstance().isSlave() && !handler.isReadOnly())
            return soapFault(soapProto, "cannot dispatch request", ServiceException.NON_READONLY_OPERATION_DENIED());

        if (!Config.userServicesEnabled() && !(handler instanceof AdminDocumentHandler))
            return soapFault(soapProto, "cannot dispatch request", ServiceException.TEMPORARILY_UNAVAILABLE());

        AuthToken at = zsc.getAuthToken();
        boolean needsAuth = handler.needsAuth(context);
        boolean needsAdminAuth = handler.needsAdminAuth(context);
        if ((needsAuth || needsAdminAuth) && at == null)
            return soapFault(soapProto, "cannot dispatch request", ServiceException.AUTH_REQUIRED());

        if (needsAdminAuth && !at.isAdmin()) {
            boolean ok = handler.domainAuthSufficient(context) && at.isDomainAdmin();
            if (!ok)
                return soapFault(soapProto, "cannot dispatch request", ServiceException.PERM_DENIED("need admin token"));
        }

        Element response = null;
        
        try {
            String acctId = null;
            boolean isGuestAccount = true;
            boolean delegatedAuth = false;
            if (at != null) {
                acctId = at.getAccountId();
                isGuestAccount = acctId.equals(ACL.GUID_PUBLIC);
                delegatedAuth = at.isDelegatedAuth();
            }
            
            if (!isGuestAccount) {
                if (needsAuth || needsAdminAuth) {
                    // make sure that the authenticated account is still active and has not been deleted since the last request
                    //   note that delegated auth allows access unless the account's in maintenance mode
                    Account account = Provisioning.getInstance().get(AccountBy.id, acctId, at);
                    if (account == null)
                        return soapFaultWithNotes(soapProto, "acount " + acctId + " not found", ServiceException.AUTH_EXPIRED());
                    
                    if (delegatedAuth && account.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE))
                        return soapFaultWithNotes(soapProto, "delegated account in MAINTENANCE mode", ServiceException.AUTH_EXPIRED());
                    
                    if (!delegatedAuth && !account.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                        return soapFaultWithNotes(soapProto, "account not active", ServiceException.AUTH_EXPIRED());
                    
                    // if using delegated auth, make sure the "admin" is really an active admin account
                    if (delegatedAuth) {
                        Account admin = Provisioning.getInstance().get(AccountBy.id, at.getAdminAccountId());
                        if (admin == null)
                            return soapFaultWithNotes(soapProto, "delegating account " + at.getAdminAccountId() + " not found", ServiceException.AUTH_EXPIRED());
                        boolean isAdmin = admin.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false) ||
                        admin.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
                        if (!isAdmin)
                            return soapFaultWithNotes(soapProto, "delegating account is not an admin account", ServiceException.AUTH_EXPIRED());
                        if (!admin.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                            return soapFaultWithNotes(soapProto, "delegating account is not active", ServiceException.AUTH_EXPIRED());
                    }

                    // also, make sure that the target account (if any) is active
                    if (zsc.isDelegatedRequest() && !handler.isAdminCommand()) {
                        Account target = Provisioning.getInstance().get(AccountBy.id, zsc.getRequestedAccountId());
                        // treat the account as inactive if (a) it doesn't exist, (b) it's in maintenance mode, or (c) we're non-admins and it's not "active"
                        boolean inactive = target == null || target.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE);
                        if (!inactive && (!at.isAdmin() || !AccessManager.getInstance().canAccessAccount(at, target)))
                            inactive = !target.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE);
                        if (inactive)
                            return soapFaultWithNotes(soapProto, "target account is not active", AccountServiceException.ACCOUNT_INACTIVE(target == null ? zsc.getRequestedAccountId() : target.getName()));
                    }
                }

                // fault in a session for this handler (if necessary) before executing the command
                context.put(ZIMBRA_SESSION, handler.getSession(zsc));

                // try to proxy the request if necesary (don't proxy commands that don't require auth)
                if (needsAuth || needsAdminAuth)
                    response = handler.proxyIfNecessary(request, context);
            }

            // if no proxy, execute the request locally
            if (response == null) {
                Object userObj = handler.preHandle(request, context);
                if (delegatedAuth)
                    handler.logAuditAccess(at.getAdminAccountId(), acctId, acctId);
                try {
                    response = handler.handle(request, context);
                } finally {
                    handler.postHandle(userObj);
                }
                ZimbraPerf.SOAP_TRACKER.addStat(request.getName(), startTime);
            }
        } catch (SoapFaultException e) {
            response = e.getFault() != null ? e.getFault().detach() : soapProto.soapFault(ServiceException.FAILURE(e.toString(), e)); 
            if (!e.isSourceLocal())
                mLog.debug("handler exception", e);
        } catch (AuthFailedServiceException e) {
            response = soapProto.soapFault(e);
            // Don't log stack trace for auth failures, since they commonly happen
            mLog.info("handler exception: %s%s", e.getMessage(), e.getReason(", %s"));
        } catch (ServiceException e) {
            response = soapProto.soapFault(e);
            mLog.info("handler exception", e);
            // XXX: if the session was new, do we want to delete it?
        } catch (Throwable e) {
            // don't interfere with Jetty Continuations -- pass the exception on up
            if (e.getClass().getName().equals("org.mortbay.jetty.RetryRequest"))
                throw ((RuntimeException)e);
            // TODO: better exception stack traces during develope?
            response = soapProto.soapFault(ServiceException.FAILURE(e.toString(), e));
            if (e instanceof OutOfMemoryError)
                Zimbra.halt("handler exception", e);
            mLog.warn("handler exception", e);
            // XXX: if the session was new, do we want to delete it?
        }
        return response;
    }
    
	public DocumentDispatcher getDocumentDispatcher() {
		return mDispatcher;
	}

    private void acknowledgeNotifications(ZimbraSoapContext zsc) {
        String authAccountId = zsc.getAuthtokenAccountId();
        SessionInfo sinfo = zsc.getSessionInfo();
        if (sinfo != null && sinfo.sequence > 0) {
            Session session = SessionCache.lookup(sinfo.sessionId, authAccountId);
            if (session instanceof SoapSession)
                ((SoapSession) session).acknowledgeNotifications(sinfo.sequence);
        }
    }

    /** Creates a <tt>&lt;context></tt> element for the SOAP Header containing
     *  session information and change notifications.<p>
     * 
     *  Sessions -- those passed in the SOAP request <tt>&lt;context></tt>
     *  block and those created during the course of processing the request -- are
     *  listed:<p>
     *     <tt>&lt;sessionId [type="admin"] id="12">12&lt;/sessionId></tt>
     * 
     * @return A new <tt>&lt;context></tt> {@link Element}, or <tt>null</tt>
     *         if there is no relevant information to encapsulate. */
    private Element generateResponseHeader(ZimbraSoapContext zsc) {
        String authAccountId = zsc.getAuthtokenAccountId();
        String requestedAccountId = zsc.getRequestedAccountId();

        Element ctxt = zsc.createElement(HeaderConstants.CONTEXT);
        boolean requiresChangeHeader = requestedAccountId != null;
        try {
            SessionInfo sinfo = zsc.getSessionInfo();
            Session session = sinfo == null ? null : SessionCache.lookup(sinfo.sessionId, authAccountId);
            if (session != null) {
                // session ID is valid, so ping it back to the client:
                ZimbraSoapContext.encodeSession(ctxt, session.getSessionId(), session.getSessionType());

                if (session instanceof SoapSession) {
                    SoapSession soap = (SoapSession) session;
                    if (session.getTargetAccountId().equals(requestedAccountId))
                        requiresChangeHeader = false;

                    // put <refresh> blocks back for any newly-created SoapSession objects
                    if (sinfo.created || soap.requiresRefresh(sinfo.sequence))
                        soap.putRefresh(ctxt, zsc);
                    // put <notify> blocks back for any SoapSession objects
                    soap.putNotifications(ctxt, zsc, sinfo.sequence);
                    // add any extension headers
                    SoapContextExtension.addExtensionHeaders(ctxt, zsc, requestedAccountId != null ? requestedAccountId : session.getTargetAccountId());
                }
            }
            
            // bug: 17481 if <nosession> is specified, then the SessionInfo list will be empty...but
            // we still want to encode the <change> element at least for the directly-requested accountId...
            // so encode it here as a last resort
            if (requiresChangeHeader) {
                try {
                    String explicitAcct = requestedAccountId.equals(authAccountId) ? null : requestedAccountId;
                    // send the <change> block
                    // <change token="555" [acct="4f778920-1a84-11da-b804-6b188d2a20c4"]/>
                    Mailbox mbox = DocumentHandler.getRequestedMailbox(zsc);
                    if (mbox != null)
                        ctxt.addUniqueElement(HeaderConstants.E_CHANGE)
                            .addAttribute(HeaderConstants.A_CHANGE_ID, mbox.getLastChangeID())
                            .addAttribute(HeaderConstants.A_ACCOUNT_ID, explicitAcct);
                } catch (ServiceException e) {
                    // eat error for right now
                }
            }
//          if (ctxt != null && mAuthToken != null)
//              ctxt.addAttribute(E_AUTH_TOKEN, mAuthToken.toString(), Element.DISP_CONTENT);
            return ctxt;
        } catch (ServiceException e) {
            ZimbraLog.session.info("ServiceException while putting soap session refresh data", e);
            return null;
        }
    }

}
