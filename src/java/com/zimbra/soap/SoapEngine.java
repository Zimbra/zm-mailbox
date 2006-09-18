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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.SoapProtocol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
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

        ZimbraSoapContext lc = null;
        Element ectxt = soapProto.getHeader(envelope, ZimbraSoapContext.CONTEXT);
        try {
            lc = new ZimbraSoapContext(ectxt, context, soapProto);
        } catch (ServiceException e) {
            return soapProto.soapEnvelope(soapProto.soapFault(e));
        }
        SoapProtocol responseProto = lc.getResponseProtocol();

        ZimbraLog.clearContext();
        try {
            String rid = lc.getRequestedAccountId();
            if (rid != null) {
                ZimbraLog.addAccountToContext(rid, ZimbraLog.C_NAME, ZimbraLog.C_ID);
                String aid = lc.getAuthtokenAccountId();
                if (!rid.equals(aid))
                    ZimbraLog.addAccountToContext(aid, ZimbraLog.C_ANAME, ZimbraLog.C_AID);
                else if (lc.getAuthToken().getAdminAccountId() != null)
                    ZimbraLog.addAccountToContext(lc.getAuthToken().getAdminAccountId(), ZimbraLog.C_ANAME, ZimbraLog.C_AID);                        
            }
            String ip = (String) context.get(REQUEST_IP);
            if (ip != null)
                ZimbraLog.addToContext(ZimbraLog.C_IP, ip);
            if (lc.getUserAgent() != null) {
                ZimbraLog.addToContext(ZimbraLog.C_USER_AGENT, lc.getUserAgent());
            }

            context.put(ZIMBRA_CONTEXT, lc);
            context.put(ZIMBRA_ENGINE, this);

            Element doc = soapProto.getBodyElement(envelope);

            if (mLog.isDebugEnabled())
                mLog.debug("dispatch: doc " + doc.getQualifiedName());

            Element responseBody = null;
            if (!lc.isProxyRequest()) {
                if (doc.getQName().equals(ZimbraNamespace.E_BATCH_REQUEST)) {

                    boolean contOnError = doc.getAttribute(ZimbraNamespace.A_ONERROR,
                                ZimbraNamespace.DEF_ONERROR).equals("continue");
            	        responseBody = lc.createElement(ZimbraNamespace.E_BATCH_RESPONSE);
            	        for (Iterator it = doc.elementIterator(); it.hasNext(); ) {
            	            Element req = (Element) it.next();
            	            if (mLog.isDebugEnabled())
            	                mLog.debug("dispatch: multi " + req.getQualifiedName());
    
            	            String id = req.getAttribute("id", null);
            	            Element br = dispatchRequest(req, context, lc);
            	            if (id != null)
            	                br.addAttribute("id", id);
            	            responseBody.addElement(br);
            	            if (!contOnError && responseProto.isFault(br))
            	                break;
            	        }
                } else {
                    String id = doc.getAttribute("id", null);
                    responseBody = dispatchRequest(doc, context, lc);
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
                	responseBody = lc.getProxyTarget().dispatch(doc);
                    responseBody.detach();
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
            Element responseHeader = lc.generateResponseHeader();

            return responseProto.soapEnvelope(responseBody, responseHeader);
        } finally {
            ZimbraLog.clearContext();
        }
    }

    /**
     * Handles individual requests, either direct or from a batch
     * @param request
     * @param context
     * @param lc
     * @return
     */
    Element dispatchRequest(Element request, Map<String, Object> context, ZimbraSoapContext lc) {
        SoapProtocol soapProto = lc.getResponseProtocol();

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
            AuthToken at = lc != null ? lc.getAuthToken() : null;
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
                Account account = DocumentHandler.getRequestedAccount(lc);
                if (!account.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                    return soapProto.soapFault(ServiceException.AUTH_EXPIRED());
            } catch (ServiceException ex) {
                return soapProto.soapFault(ex);
            }
        }
        
        Element response;
        try {
            // first, try to proxy the request if necessary
            response = handler.proxyIfNecessary(request, context);
            // if no proxy, execute the request locally
            if (response == null)
                response = handler.handle(request, context);
            // fault in a session for this handler after executing the command
            handler.getSession(context);
        } catch (ServiceException e) {
            response = soapProto.soapFault(e);
            mLog.info("handler exception", e);
        } catch (Throwable e) {
            // TODO: better exception stack traces during develope?
            response = soapProto.soapFault(ServiceException.FAILURE(e.toString(), e));
            if (e instanceof OutOfMemoryError)
                Zimbra.halt("handler exception", e);
            mLog.warn("handler exception", e);
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
