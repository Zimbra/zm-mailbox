package com.zimbra.soap;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.SoapProtocol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    public static final String DONT_CREATE_SESSION = "zimbra.noSession";
    public static final String IS_AUTH_COMMAND = "zimbra.isAuthCommand";
    private static final String IS_ADMIN_COMMAND = "zimbra.isAdminCommand";

    /** context name of request IP */
    public static final String REQUEST_IP = "request.ip";
    
	private static Log mLog = LogFactory.getLog(SoapEngine.class);
	
	private DocumentDispatcher mDispatcher;
	
    public SoapEngine() {
        mDispatcher = new DocumentDispatcher();
    }

    public Element dispatch(String path, byte[] soapMessage, Map context) {
        InputStream in = new ByteArrayInputStream(soapMessage);
        Element document = null;
        try {
            document = Element.XMLElement.parseText(in);
        } catch (DocumentException de) {
            // FIXME: have to pick 1.1 or 1.2 since we can't parse any
            SoapProtocol soapProto = SoapProtocol.Soap12;
            return soapProto.soapEnvelope(soapProto.soapFault(ServiceException.PARSE_ERROR(de.getMessage(), de)));
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
    private Element dispatch(String path, Element envelope, Map context)
    {
    	//if (mLog.isDebugEnabled()) mLog.debug("dispatch(path, envelope, context: "+envelope.getQualifiedName());
    	
        SoapProtocol soapProto = SoapProtocol.determineProtocol(envelope);

        if (soapProto == null) {
            // FIXME: have to pick 1.1 or 1.2 since we can't parse any
            soapProto = SoapProtocol.Soap12;
            return soapProto.soapEnvelope(soapProto.soapFault(ServiceException.INVALID_REQUEST("unable to determine SOAP version", null)));
        }

        //if (mLog.isDebugEnabled()) mLog.debug("dispatch: soapProto = "+soapProto.getVersion());

        ZimbraContext zimbraContext = null;
        Element ectxt = soapProto.getHeader(envelope, ZimbraContext.CONTEXT);
        try {
            zimbraContext = new ZimbraContext(ectxt, context, soapProto);
        } catch (ServiceException e) {
            return soapProto.soapEnvelope(soapProto.soapFault(e));
        }
        SoapProtocol responseProto = zimbraContext.getResponseProtocol();

        if (zimbraContext.sessionSuppressed())
        	context.put(DONT_CREATE_SESSION, "1");

        ZimbraLog.clearContext();
        try {
            if (context != null) {
                String id = zimbraContext.getRequestedAccountId();
                if (id != null) {
                    ZimbraLog.addAccountToContext(id, ZimbraLog.C_NAME, ZimbraLog.C_ID);
                    String aid = zimbraContext.getAuthtokenAccountId();
                    if (!id.equals(aid)) {
                        ZimbraLog.addAccountToContext(aid, ZimbraLog.C_ANAME, ZimbraLog.C_AID);
                    } else if (zimbraContext.getAuthToken().getAdminAccountId() != null) {
                        ZimbraLog.addAccountToContext(zimbraContext.getAuthToken().getAdminAccountId(), ZimbraLog.C_ANAME, ZimbraLog.C_AID);                        
                    }
                }
            }
            String ip = (String) context.get(REQUEST_IP);
            if (ip != null) ZimbraLog.addToContext(ZimbraLog.C_IP, ip);

            context.put(ZIMBRA_CONTEXT, zimbraContext);

            Element doc = soapProto.getBodyElement(envelope);

            if (mLog.isDebugEnabled())
                mLog.debug("dispatch: doc " + doc.getQualifiedName());
        
            Element responseBody = null;
            if (!zimbraContext.isProxyRequest()) {
                if (doc.getQName().equals(ZimbraNamespace.E_BATCH_REQUEST)) {
                    
                    boolean contOnError = doc.getAttribute(ZimbraNamespace.A_ONERROR,
                                ZimbraNamespace.DEF_ONERROR).equals("continue");
            	        responseBody = zimbraContext.createElement(ZimbraNamespace.E_BATCH_RESPONSE);
            	        for (Iterator it = doc.elementIterator(); it.hasNext(); ) {
            	            Element req = (Element) it.next();
            	            if (mLog.isDebugEnabled())
            	                mLog.debug("dispatch: multi " + req.getQualifiedName());
    
            	            String id = req.getAttribute("id", null);
            	            Element br = dispatchRequest(responseProto, path, req, context, zimbraContext);
            	            if (id != null)
            	                br.addAttribute("id", id);
            	            responseBody.addElement(br);
            	            if (!contOnError && responseProto.isFault(br)) {
            	                break;
            	            }
            	        }
                } else {
                    String id = doc.getAttribute("id", null);
                    responseBody = dispatchRequest(responseProto, path, doc, context, zimbraContext);
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
                	responseBody = zimbraContext.getProxyTarget().dispatch(doc);
                    responseBody.detach();
                } catch (ServiceException e) {
                    responseBody = responseProto.soapFault(e);
                    mLog.warn("proxy handler exception ", e);
                } catch (Throwable e) {
                    responseBody = responseProto.soapFault(ServiceException.FAILURE(e.toString(), e));
                    if (e instanceof OutOfMemoryError) {
                        Zimbra.halt("proxy handler exception", e);
                    }
                    mLog.warn("proxy handler exception ", e);
                }
            }

            // handle refresh block and notifications
            Element responseHeader = null;
            if (context.get(IS_AUTH_COMMAND) == null) {
                // Add refresh block in first response of a new session.
                if (zimbraContext.hasNewSessionId()) {
                    boolean includeRefresh = context.get(IS_ADMIN_COMMAND) == null;
                    responseHeader = zimbraContext.newSessionResponse(includeRefresh);
                }
                // Add any pending notifications.
                SoapSession s = zimbraContext.getSession();
                if (s != null)
                    responseHeader = s.putNotifications(zimbraContext, responseHeader);
            }
            return responseProto.soapEnvelope(responseBody, responseHeader);
        } finally {
            ZimbraLog.clearContext();
        }
    }

    /**
     * Handles batched requests
     * 
     * @param soapProto
     * @param path
     * @param doc
     * @param context
     * @param zimbraContext
     * @return
     */
    private Element dispatchRequest(SoapProtocol soapProto, String path,
                                    Element doc, Map context, ZimbraContext zimbraContext) 
    {
        if (doc == null)
            return soapProto.soapFault(ServiceException.INVALID_REQUEST("no document specified", null));
        
        DocumentHandler handler = mDispatcher.getHandler(doc);
        if (handler == null) 
            return soapProto.soapFault(ServiceException.UNKNOWN_DOCUMENT(doc.getQualifiedName(), null));
        
        if (RedoLogProvider.getInstance().isSlave() && !handler.isReadOnly())
            return soapProto.soapFault(ServiceException.NON_READONLY_OPERATION_DENIED());

        if (handler instanceof AdminDocumentHandler) {
            // cheesy way to tell caller we're dealing with an admin command
            context.put(IS_ADMIN_COMMAND, "1");
        } else {
            if (!Config.userServicesEnabled())
                return soapProto.soapFault(ServiceException.TEMPORARILY_UNAVAILABLE());
        }
        
        boolean needsAuth = handler.needsAuth(context);
        boolean needsAdminAuth = handler.needsAdminAuth(context);
        if (needsAuth || needsAdminAuth) {
            AuthToken at = zimbraContext != null ? zimbraContext.getAuthToken() : null;
            if (at == null)
                return soapProto.soapFault(ServiceException.AUTH_REQUIRED());
            if (needsAdminAuth && !at.isAdmin()) 
                return soapProto.soapFault(ServiceException.PERM_DENIED("need admin token"));

            // Make sure that the account is active and has not been deleted
            // since the last request
            try {
                Account account = handler.getRequestedAccount(zimbraContext);
                if (!account.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE)) {
                    return soapProto.soapFault(ServiceException.AUTH_EXPIRED());
                }
            } catch (ServiceException ex) {
                return soapProto.soapFault(ex);
            }
        }
        
        Element response;
        try {
            response = handler.handle(doc, context);
        } catch (ServiceException e) {
            response = soapProto.soapFault(e);
            if (mLog.isDebugEnabled()) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                mLog.debug("handler exception "+sw.toString());
            }
        } catch (Throwable e) {
            // FIXME: temp hack by tim b/c dogfood not generating stack traces
            e.printStackTrace();
            
            // TODO: better exception stack traces during develope?
            response = soapProto.soapFault(ServiceException.FAILURE(e.toString(),e));
            if (e instanceof OutOfMemoryError) {
                Zimbra.halt("handler exception", e);
            }
            if (mLog.isWarnEnabled())
                mLog.warn("handler exception ", e);
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
