/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on 2005. 3. 3.
 */
package com.zimbra.soap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author jhahm
 */
public class ProxyTarget {

    private Server mServer;
    private String mAuthToken;
    private String mURL;

    public ProxyTarget(String serverId, String authToken, HttpServletRequest req) throws ServiceException {
        mServer = Provisioning.getInstance().get(ServerBy.id, serverId);
        if (mServer == null)
            throw AccountServiceException.NO_SUCH_SERVER(serverId);

        mAuthToken = authToken;

        String scheme;
        int port;
        String url;
        
        String requestStr = req.getRequestURI();
        String qs = req.getQueryString();
        if (qs != null)
            requestStr =  requestStr + "?" + qs;

        int localAdminPort = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        if (req.getLocalPort() == localAdminPort) {
            url = URLUtil.getAdminURL(mServer, requestStr, true);
        } else {
            url = URLUtil.getMailURL(mServer, requestStr, false);
        }
        
        mURL = url;
    }

    public boolean isTargetLocal() throws ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        return mServer.equals(localServer);
    }

    public Element dispatch(Element request) throws ServiceException {
        SoapProtocol proto = request instanceof Element.JSONElement ? SoapProtocol.SoapJS : SoapProtocol.Soap12;
        SoapHttpTransport transport = new SoapHttpTransport(mURL);
        try {
            transport.setAuthToken(mAuthToken);
            transport.setRequestProtocol(proto);
            return transport.invokeWithoutSession(request);
        } catch (IOException e) {
            throw ServiceException.PROXY_ERROR(e, mURL);
        } finally {
            transport.shutdown();
        }
    }

    public Element dispatch(Element request, ZimbraSoapContext zsc) throws ServiceException {
        if (zsc == null)
            return dispatch(request);

        SoapProtocol proto = request instanceof Element.JSONElement ? SoapProtocol.SoapJS : SoapProtocol.Soap12;
        if (proto == SoapProtocol.Soap12 && zsc.getRequestProtocol() == SoapProtocol.Soap11)
            proto = SoapProtocol.Soap11;
        Element envelope = proto.soapEnvelope(request, zsc.toProxyCtxt());

        Element response = null;
        SoapHttpTransport transport = null;
        try {
            transport = new SoapHttpTransport(mURL);
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
