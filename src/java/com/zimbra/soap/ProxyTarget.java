/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on 2005. 3. 3.
 */
package com.zimbra.soap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ProxyTarget {

    private Server mServer;
    private AuthToken mAuthToken;
    private String mURL;

    private int mMaxAttempts = 0;
    private long mTimeout = -1;

    public ProxyTarget(String serverId, AuthToken authToken, HttpServletRequest req) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        mServer = prov.get(ServerBy.id, serverId);
        if (mServer == null)
            throw AccountServiceException.NO_SUCH_SERVER(serverId);

        mAuthToken = authToken;
        String url;
        String requestStr = req.getRequestURI();
        String qs = req.getQueryString();
        if (qs != null)
            requestStr = requestStr + "?" + qs;

        int localAdminPort = prov.getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        if (!prov.isOfflineProxyServer(mServer) && req.getLocalPort() == localAdminPort) {
            url = URLUtil.getAdminURL(mServer, requestStr, true);
        } else {
            url = URLUtil.getServiceURL(mServer, requestStr, false);
        }

        mURL = url;
    }

    public ProxyTarget(Server server, AuthToken authToken, String url) {
        mServer = server;  mAuthToken = authToken;  mURL = url;
    }

    /** Instructs the proxy's underlying {@link SoapHttpTransport} to attempt
     *  the request only once. */
    public ProxyTarget disableRetries() {
        mMaxAttempts = 1;  return this;
    }

    /** Sets both the connect and read timeouts on the proxy's underlying
     *  {@link SoapHttpTransport}.
     * @param timeoutMsecs  The new read/connect timeout, in milliseconds. */
    public ProxyTarget setTimeouts(long timeoutMsecs) {
        mTimeout = timeoutMsecs;  return this;
    }

    public Server getServer() {
        return mServer;
    }

    public boolean isTargetLocal() throws ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        return mServer.getId().equals(localServer.getId());
    }

    public Element dispatch(Element request) throws ServiceException {
        SoapProtocol proto = request instanceof Element.JSONElement ? SoapProtocol.SoapJS : SoapProtocol.Soap12;
        SoapHttpTransport transport = new SoapHttpTransport(mURL);
        try {
            transport.setAuthToken(mAuthToken.toZAuthToken());
            transport.setRequestProtocol(proto);
            return transport.invokeWithoutSession(request);
        } catch (IOException e) {
            throw ServiceException.PROXY_ERROR(e, mURL);
        } finally {
            transport.shutdown();
        }
    }

    public Element dispatch(Element request, ZimbraSoapContext zsc) throws ServiceException {
        return execute(request, zsc).getSecond();
    }

    public Pair<Element, Element> execute(Element request, ZimbraSoapContext zsc) throws ServiceException {
        if (zsc == null)
            return new Pair<Element, Element>(null, dispatch(request));

        SoapProtocol proto = request instanceof Element.JSONElement ? SoapProtocol.SoapJS : SoapProtocol.Soap12;
        if (proto == SoapProtocol.Soap12 && zsc.getRequestProtocol() == SoapProtocol.Soap11)
            proto = SoapProtocol.Soap11;
        Element envelope = proto.soapEnvelope(request, zsc.toProxyCtxt(proto));

        SoapHttpTransport transport = null;
        try {
            transport = new SoapHttpTransport(mURL);
            transport.setTargetAcctId(zsc.getRequestedAccountId());
            if (mMaxAttempts > 0)
                transport.setRetryCount(mMaxAttempts);
            if (mTimeout >= 0)
                transport.setTimeout((int) Math.min(mTimeout, Integer.MAX_VALUE));

            Element response = transport.invokeRaw(envelope);
            Element body = transport.extractBodyElement(response);
            return new Pair<Element, Element>(transport.getZimbraContext(), body);
        } catch (IOException e) {
            throw ServiceException.PROXY_ERROR(e, mURL);
        } finally {
            if (transport != null)
                transport.shutdown();
        }
    }

    @Override public String toString() {
        return "ProxyTarget(url=" + mURL + ")";
    }
}
