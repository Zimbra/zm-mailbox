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
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author schemers
 */
public abstract class DocumentHandler {

    public static String LOCAL_HOST;
    static {
        try {
            LOCAL_HOST = Provisioning.getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        } catch (Exception e) {
            Zimbra.halt("could not fetch local server name from LDAP for request proxying");
        }
    }

    public abstract Element handle(Element request, Map context) throws ServiceException, SoapFaultException;

    public static ZimbraContext getZimbraContext(Map context) {
        return (ZimbraContext) context.get(SoapEngine.ZIMBRA_CONTEXT);
    }

    public static Account getRequestedAccount(ZimbraContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();

        Account acct = Provisioning.getInstance().getAccountById(id);
        if (acct == null)
            throw ServiceException.AUTH_EXPIRED();
        return acct;
    }

    public static Mailbox getRequestedMailbox(ZimbraContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();
        Mailbox mbox = Mailbox.getMailboxByAccountId(id);
        if (mbox != null)
            ZimbraLog.addToContext(mbox);
        return mbox; 
    }

    /** Returns whether the command's caller must be authenticated. */
    public boolean needsAuth(Map context) {
        return true;
    }

    /** Returns whether this is an administrative command (and thus requires
     *  a valid admin auth token). */
    public boolean needsAdminAuth(Map context) {
        return false;
    }

    /** Returns <code>true</code> if the operation is read-only, or
     *  <code>false</code> if the operation causes backend state change. */
    public boolean isReadOnly() {
        return true;
    }

    /** Returns whether the client making the SOAP request is localhost. */
    protected boolean clientIsLocal(Map context) {
        HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        if (req == null) return true;
        String peerIP = req.getRemoteAddr();
        return "127.0.0.1".equals(peerIP);
    }

    /** Fetches the in-memory {@link Session} object appropriate for this request.
     *  If none already exists, one is created.
     * 
     * @param context  The Map containing context information for this SOAP request.
     * @return A {@link com.zimbra.cs.session.SoapSession}. */
    public Session getSession(Map context) {
        return getSession(context, SessionCache.SESSION_SOAP);
    }

    /** Fetches or creates a {@link Session} object to persist and manage state
     *  between SOAP requests.
     * 
     * @param context      The Map containing context information for this SOAP request.
     * @param sessionType  The type of session needed.
     * @return An in-memory {@link Session} object of the specified type, fetched
     *         from the request's {@link ZimbraContext} object.  If no matching
     *         session already exists, a new one is created.
     * @see SessionCache SessionCache for valid values for <code>sessionType</code>. */
    protected Session getSession(Map context, int sessionType) {
        ZimbraContext lc = getZimbraContext(context);
        return (lc == null ? null : lc.getSession(sessionType));
    }

    protected static String getXPath(Element request, String[] xpath) {
        int depth = 0;
        while (depth < xpath.length - 1 && request != null)
            request = request.getOptionalElement(xpath[depth++]);
        return (request == null ? null : request.getAttribute(xpath[depth], null));
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

    private void insertMountpointReferences(Element response, String[] xpath, ItemId iidMountpoint, ItemId iidLocal, ZimbraContext lc) {
        int depth = 0;
        while (depth < xpath.length && response != null)
            response = response.getOptionalElement(xpath[depth++]);
        if (response == null)
            return;
        String local = iidLocal.toString(lc);
        for (Iterator it = response.elementIterator(); it.hasNext(); ) {
            Element elt = (Element) it.next();
            String folder = elt.getAttribute(MailService.A_FOLDER, null);
            if (local.equalsIgnoreCase(folder))
                elt.addAttribute(MailService.A_FOLDER, iidMountpoint.toString(lc));
        }
    }

    protected static ItemId getProxyTarget(ZimbraContext lc, ItemId iid, boolean checkMountpoint) throws ServiceException {
        if (lc == null || iid == null)
            return null;
        if (!iid.belongsTo(getRequestedAccount(lc)))
            return iid;

        if (!checkMountpoint)
            return null;
        Mailbox mbox = getRequestedMailbox(lc);
        MailItem item = mbox.getItemById(lc.getOperationContext(), iid.getId(), MailItem.TYPE_FOLDER);
        if (!(item instanceof Mountpoint))
            return null;
        Mountpoint mpt = (Mountpoint) item;
        return new ItemId(mpt.getOwnerId(), mpt.getRemoteId());
    }

    protected String[] getProxiedIdPath()     { return null; }
    protected boolean checkMountpointProxy()  { return false; }
    protected String[] getResponseItemPath()  { return null; }

    protected Element proxyIfNecessary(Element request, Map context) throws ServiceException, SoapFaultException {
        String[] xpath = getProxiedIdPath();
        if (xpath == null)
            return null;
        String id = getXPath(request, xpath);
        if (id == null)
            return null;
        ItemId iid = new ItemId(id);

        ZimbraContext lc = getZimbraContext(context);
        ItemId iidTarget = getProxyTarget(lc, iid, checkMountpointProxy());
        return iidTarget == null ? null : proxyRequest(request, context, iid, iidTarget);
    }

    protected Element proxyRequest(Element request, Map context, ItemId iidRequested, ItemId iidResolved) throws ServiceException, SoapFaultException {
        // prepare the request for re-processing
        if (iidRequested != iidResolved)
            setXPath(request, getProxiedIdPath(), iidResolved.toString());

        Element response = proxyRequest(request, context, iidResolved.getAccountId());

        // translate remote folder IDs back into local mountpoint IDs
        ZimbraContext lc = getZimbraContext(context);
        String[] xpathResponse = getResponseItemPath();
        if (iidRequested != iidResolved && xpathResponse != null) 
            insertMountpointReferences(response, xpathResponse, iidRequested, iidResolved, lc);
        return response;
    }

    protected static Element proxyRequest(Element request, Map context, String acctId) throws SoapFaultException, ServiceException {
        SoapEngine engine = (SoapEngine) context.get(SoapEngine.ZIMBRA_ENGINE);
        ZimbraContext lc = getZimbraContext(context);
        // new context for proxied request has a different "requested account"
        ZimbraContext lcTarget = new ZimbraContext(lc, acctId);

        // figure out whether we can just re-dispatch or if we need to proxy via HTTP
        Account acctTarget = Provisioning.getInstance().getAccountById(acctId);
        if (acctTarget == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);
        String hostTarget = acctTarget.getAttr(Provisioning.A_zimbraMailHost);
        boolean isLocal = LOCAL_HOST.equalsIgnoreCase(hostTarget) && engine != null;

        Element response = null;
        request.detach();
        if (isLocal) {
            // executing on same server; just hand back to the SoapEngine
            Map contextTarget = new HashMap(context);
            contextTarget.put(SoapEngine.ZIMBRA_CONTEXT, lcTarget);
            response = engine.dispatchRequest(request, contextTarget, lcTarget);
            if (lcTarget.getResponseProtocol().isFault(response))
                throw new SoapFaultException("error in proxied request", response);
        } else {
            // executing remotely; find out target and proxy there
            Server serverTarget = Provisioning.getInstance().getServerByName(hostTarget);
            if (serverTarget == null)
                throw AccountServiceException.NO_SUCH_SERVER(hostTarget);
            HttpServletRequest httpreq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            ProxyTarget proxy = new ProxyTarget(serverTarget.getId(), lc.getRawAuthToken(), httpreq);
            response = proxy.dispatch(request, lcTarget);
            response.detach();
        }
        return response;
    }
    
    protected static Element fwdRequestToRemoteHost(ZimbraContext zc, Element request, Map context, String hostTarget) throws SoapFaultException, ServiceException {
        SoapEngine engine = (SoapEngine) context.get(SoapEngine.ZIMBRA_ENGINE);
        
        boolean isLocal = LOCAL_HOST.equalsIgnoreCase(hostTarget) && engine != null;
        Element response = null;
        
        if (isLocal) {
            // executing on same server; just hand back to the SoapEngine
            Map contextTarget = new HashMap(context);
            contextTarget.put(SoapEngine.ZIMBRA_CONTEXT, zc);
            response = engine.dispatchRequest(request, contextTarget, zc);
            if (zc.getResponseProtocol().isFault(response)) 
                throw new SoapFaultException("error in proxied request", response);
        } else {
            // executing remotely; find out target and proxy there
            Server serverTarget = Provisioning.getInstance().getServerByName(hostTarget);
            if (serverTarget == null)
                throw AccountServiceException.NO_SUCH_SERVER(hostTarget);
            HttpServletRequest httpreq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            ProxyTarget proxy = new ProxyTarget(serverTarget.getId(), zc.getRawAuthToken(), httpreq);
            response = proxy.dispatch(request, zc);
            response.detach();
        }
        return response;
    }    
}
