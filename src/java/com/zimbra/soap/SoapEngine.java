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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.SoapProtocol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;

/**
 * The soap engine.
 */

public class SoapEngine {

    public static final String ZIMBRA_CONTEXT = "zimbra.context";
    public static final String ZIMBRA_ENGINE = "zimbra.engine";

    /** context name of request IP */
    public static final String REQUEST_IP = "request.ip";
    
	private static Log mLog = LogFactory.getLog(SoapEngine.class);
	
	private DocumentDispatcher mDispatcher;
	
    public SoapEngine() {
        mDispatcher = new DocumentDispatcher();
    }

    public Element dispatch(String path, byte[] soapMessage, Map<String, Object> context) {
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
            return soapProto.soapEnvelope(soapProto.soapFault(ServiceException.PARSE_ERROR(de.getMessage(), de)));
        } catch (SoapParseException e) {
            SoapProtocol soapProto = SoapProtocol.SoapJS;
            return soapProto.soapEnvelope(soapProto.soapFault(ServiceException.PARSE_ERROR(e.getMessage(), e)));
        }
        return dispatch(path, document, context);
    }

    /**
     * dispatch to the given serviceName the specified document,
     * which should be a soap envelope containing a document to 
     * execute.
     *
     * @param path  the path (i.e., /service/foo) of the service to dispatch to
     * @param 
     * @return an XmlObject which is a SoapEnvelope containing the response
     *         
     */
    private Element dispatch(String path, Element envelope, Map<String, Object> context) {
    	// if (mLog.isDebugEnabled()) mLog.debug("dispatch(path, envelope, context: " + envelope.getQualifiedName());
    	
        SoapProtocol soapProto = SoapProtocol.determineProtocol(envelope);
        if (soapProto == null) {
            // FIXME: have to pick 1.1 or 1.2 since we can't parse any
            soapProto = SoapProtocol.Soap12;
            return soapProto.soapEnvelope(soapProto.soapFault(ServiceException.INVALID_REQUEST("unable to determine SOAP version", null)));
        }

        // if (mLog.isDebugEnabled()) mLog.debug("dispatch: soapProto = " + soapProto.getVersion());

        ZimbraSoapContext zsc = null;
        Element ectxt = soapProto.getHeader(envelope, ZimbraSoapContext.CONTEXT);
        try {
            zsc = new ZimbraSoapContext(ectxt, context, soapProto);
        } catch (ServiceException e) {
            return soapProto.soapEnvelope(soapProto.soapFault(e));
        }
        SoapProtocol responseProto = zsc.getResponseProtocol();

        ZimbraLog.clearContext();
        try {
            String rid = zsc.getRequestedAccountId();
            if (rid != null) {
                Account.addAccountToLogContext(rid, ZimbraLog.C_NAME, ZimbraLog.C_ID);
                String aid = zsc.getAuthtokenAccountId();
                if (!rid.equals(aid))
                    Account.addAccountToLogContext(aid, ZimbraLog.C_ANAME, ZimbraLog.C_AID);
                else if (zsc.getAuthToken().getAdminAccountId() != null)
                    Account.addAccountToLogContext(zsc.getAuthToken().getAdminAccountId(), ZimbraLog.C_ANAME, ZimbraLog.C_AID);                        
            }
            String ip = (String) context.get(REQUEST_IP);
            if (ip != null)
                ZimbraLog.addToContext(ZimbraLog.C_IP, ip);
            if (zsc.getUserAgent() != null)
                ZimbraLog.addToContext(ZimbraLog.C_USER_AGENT, zsc.getUserAgent());

            context.put(ZIMBRA_CONTEXT, zsc);
            context.put(ZIMBRA_ENGINE, this);

            Element doc = soapProto.getBodyElement(envelope);

            if (mLog.isDebugEnabled())
                mLog.debug("dispatch: doc " + doc.getQualifiedName());

            Element responseBody = null;
            if (!zsc.isProxyRequest()) {
                if (doc.getQName().equals(ZimbraNamespace.E_BATCH_REQUEST)) {
                    boolean contOnError = doc.getAttribute(ZimbraNamespace.A_ONERROR, ZimbraNamespace.DEF_ONERROR).equals("continue");
        	        responseBody = zsc.createElement(ZimbraNamespace.E_BATCH_RESPONSE);
                    for (Element req : doc.listElements()) {
        	            if (mLog.isDebugEnabled())
        	                mLog.debug("dispatch: multi " + req.getQualifiedName());

        	            String id = req.getAttribute("id", null);
        	            Element br = dispatchRequest(req, context, zsc);
        	            if (id != null)
        	                br.addAttribute("id", id);
        	            responseBody.addElement(br);
        	            if (!contOnError && responseProto.isFault(br))
        	                break;
        	        }
                } else {
                    String id = doc.getAttribute("id", null);
                    responseBody = dispatchRequest(doc, context, zsc);
                    if (id != null)
                        responseBody.addAttribute("id", id);
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
            Element responseHeader = zsc.generateResponseHeader();

            return responseProto.soapEnvelope(responseBody, responseHeader);
        } finally {
            ZimbraLog.clearContext();
        }
    }

    /**
     * Handles individual requests, either direct or from a batch
     * @param request
     * @param context
     * @param zsc
     * @return
     */
    Element dispatchRequest(Element request, Map<String, Object> context, ZimbraSoapContext zsc) {
        SoapProtocol soapProto = zsc.getResponseProtocol();

        if (request == null)
            return soapProto.soapFault(ServiceException.INVALID_REQUEST("no document specified", null));

        DocumentHandler handler = mDispatcher.getHandler(request);
        if (handler == null) 
            return soapProto.soapFault(ServiceException.UNKNOWN_DOCUMENT(request.getQualifiedName(), null));

        if (RedoLogProvider.getInstance().isSlave() && !handler.isReadOnly())
            return soapProto.soapFault(ServiceException.NON_READONLY_OPERATION_DENIED());

        if (!Config.userServicesEnabled() && !(handler instanceof AdminDocumentHandler))
            return soapProto.soapFault(ServiceException.TEMPORARILY_UNAVAILABLE());

        boolean needsAuth = handler.needsAuth(context);
        boolean needsAdminAuth = handler.needsAdminAuth(context);
        if (needsAuth || needsAdminAuth) {
            AuthToken at = zsc != null ? zsc.getAuthToken() : null;
            if (at == null)
                return soapProto.soapFault(ServiceException.AUTH_REQUIRED());
            if (needsAdminAuth && !at.isAdmin()) {
                boolean ok = handler.domainAuthSufficient(context) && at.isDomainAdmin();
                if (!ok)
                    return soapProto.soapFault(ServiceException.PERM_DENIED("need admin token"));
            }

            // Make sure that the account is active and has not been deleted
            // since the last request
            try {
                Account account = DocumentHandler.getRequestedAccount(zsc);
                if (!account.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE)) {
                    if (zsc.isDelegatedRequest())
                        return soapProto.soapFault(AccountServiceException.ACCOUNT_INACTIVE(account.getName()));
                    else
                        return soapProto.soapFault(ServiceException.AUTH_EXPIRED());
                }
            } catch (ServiceException ex) {
                return soapProto.soapFault(ex);
            }
        }
        
        Element response = null;
        try {
            // fault in a session for this handler (if necessary) before executing the command
            handler.getSession(context);
            // try to proxy the request if necesary (don't proxy commands that don't require auth)
            if (needsAuth || needsAdminAuth)
                response = handler.proxyIfNecessary(request, context);
            // if no proxy, execute the request locally
            if (response == null)
                response = handler.handle(request, context);
        } catch (SoapFaultException e) {
            response = e.getFault() != null ? e.getFault().detach() : soapProto.soapFault(ServiceException.FAILURE(e.toString(), e)); 
            if (!e.isSourceLocal())
                mLog.debug("handler exception", e);
        } catch (ServiceException e) {
            response = soapProto.soapFault(e);
            mLog.info("handler exception", e);
            // XXX: if the session was new, do we want to delete it?
        } catch (Throwable e) {
            // TODO: better exception stack traces during develope?
            response = soapProto.soapFault(ServiceException.FAILURE(e.toString(), e));
            if (e instanceof OutOfMemoryError)
                Zimbra.halt("handler exception", e);
            mLog.warn("handler exception", e);
            // XXX: if the session was new, do we want to delete it?
        }
        return response;
    }
    
	/**
	 * @return
	 */
	public DocumentDispatcher getDocumentDispatcher() {
		return mDispatcher;
	}
}
