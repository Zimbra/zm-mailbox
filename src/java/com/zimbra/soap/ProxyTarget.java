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

/*
 * Created on 2005. 3. 3.
 */
package com.zimbra.soap;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class ProxyTarget {

    private Server mServer;
    private String mAuthToken;
    private String mURL;

    public ProxyTarget(String serverId, String authToken, HttpServletRequest req) throws ServiceException {
        mServer = Provisioning.getInstance().getServerById(serverId);
        if (mServer == null)
            throw AccountServiceException.NO_SUCH_SERVER(serverId);

        mAuthToken = authToken;

        String scheme;
        int port;

        int localAdminPort = Provisioning.getInstance().getLocalServer().
            getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        if (req.getServerPort() == localAdminPort) {
            scheme = "https";
            port = mServer.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
            if (port <= 0)
                throw ServiceException.FAILURE(
                        "remote server " + mServer.getName() +
                        " does not have admin port enabled", null);
        } else {
            port = mServer.getIntAttr(Provisioning.A_zimbraMailPort, 0);
            if (port > 0) {
                scheme = "http";
            } else {
                scheme = "https";
                port = mServer.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
                if (port <= 0)
                    throw ServiceException.FAILURE(
                            "remote server " + mServer.getName() +
                            " has neither http nor https port enabled", null);
            }
        }

        String hostname = mServer.getAttr(Provisioning.A_zimbraServiceHostname);
        StringBuffer url = new StringBuffer(scheme);
        url.append("://").append(hostname).append(':').append(port);
        url.append(req.getRequestURI());
        String qs = req.getQueryString();
        if (qs != null)
            url.append('?').append(qs);
        mURL = url.toString();
    }

    public boolean isTargetLocal() throws ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        return mServer.equals(localServer);
    }

    public Element dispatch(Element request) throws ServiceException, SoapFaultException {
        SoapHttpTransport transport = new SoapHttpTransport(mURL);
        try {
            transport.setAuthToken(mAuthToken);
            transport.setSoapProtocol(request instanceof Element.JavaScriptElement ? SoapProtocol.SoapJS : SoapProtocol.Soap12);
            return transport.invokeWithoutSession(request);
        } catch (IOException e) {
            throw ServiceException.PROXY_ERROR(e, mURL);
        } finally {
            transport.shutdown();
        }
    }

    public Element dispatch(Element request, ZimbraSoapContext lc) throws ServiceException, SoapFaultException {
        if (lc == null)
            return dispatch(request);

        Element response = null;
        SoapHttpTransport transport = null;
        try {
            transport = new SoapHttpTransport(mURL);
            Element envelope = lc.getRequestProtocol().soapEnvelope(request, lc.toProxyCtxt());
            response = transport.invokeRaw(envelope);
            response = transport.extractBodyElement(response);
        } catch (IOException e) {
            throw ServiceException.PROXY_ERROR(e, mURL);
        } finally {
            if (transport != null)
                transport.shutdown();
        }
        return response;
    }
}
