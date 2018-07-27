/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.QName;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;

/**
 * @since 2005. 3. 3.
 */
public final class ProxyTarget {

    private final Server mServer;
    private final AuthToken mAuthToken;
    private final String mURL;

    private int mMaxAttempts = 0;
    private long mTimeout = -1;

    public ProxyTarget(String serverId, AuthToken authToken, HttpServletRequest req) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        mServer = prov.get(Key.ServerBy.id, serverId);
        if (mServer == null)
            throw AccountServiceException.NO_SUCH_SERVER(serverId);

        mAuthToken = AuthToken.getCsrfUnsecuredAuthToken(authToken);
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
        mServer = server;
        mAuthToken = AuthToken.getCsrfUnsecuredAuthToken(authToken);
        mURL = url;
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
        if (proto == SoapProtocol.Soap12 && zsc.getRequestProtocol() == SoapProtocol.Soap11) {
            proto = SoapProtocol.Soap11;
        }
        /* Bug 77604 When a user has been configured to change their password on next login, the resulting proxied
         * ChangePasswordRequest was failing because account was specified in context but no authentication token
         * was supplied.  The server handler rejects a context which has account information but no authentication
         * info - see ZimbraSoapContext constructor - solution is to exclude the account info from the context.
         */
        boolean excludeAccountDetails = false;
        if (AccountConstants.CHANGE_PASSWORD_REQUEST.equals(request.getQName()) || MailConstants.RECOVER_ACCOUNT_REQUEST.equals(request.getQName())) {
            excludeAccountDetails = true;
        }
        Element envelope = proto.soapEnvelope(request, zsc.toProxyContext(proto, excludeAccountDetails));

        SoapHttpTransport transport = null;
        try {
            transport = new SoapHttpTransport(mURL);
            transport.setTargetAcctId(zsc.getRequestedAccountId());
            if (mMaxAttempts > 0)
                transport.setRetryCount(mMaxAttempts);
            if (mTimeout >= 0)
                transport.setTimeout((int) Math.min(mTimeout, Integer.MAX_VALUE));

            transport.setResponseProtocol(zsc.getResponseProtocol());
            AuthToken authToken = AuthToken.getCsrfUnsecuredAuthToken(zsc.getAuthToken());
            if (authToken != null && !StringUtil.isNullOrEmpty(authToken.getProxyAuthToken())) {
                transport.setAuthToken(authToken.getProxyAuthToken());
            }
            if (ZimbraLog.soap.isDebugEnabled()) {
                ZimbraLog.soap.debug("Proxying request: proxy=%s targetAcctId=%s",
                        toString(), zsc.getRequestedAccountId());
            }

            disableCsrfFlagInAuthToken(envelope, authToken, request.getQName());

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

    /**
     * @param envelope
     * @param authToken
     * @param qName
     * @return
     * @throws ServiceException
     */
    private void disableCsrfFlagInAuthToken(Element envelope, AuthToken authToken, QName qName) throws ServiceException {
        Element header = envelope.getOptionalElement("Header");
        if (header != null) {
            Element context = header.getOptionalElement(HeaderConstants.CONTEXT);
            if (context != null) {
                Element token = context.getOptionalElement(HeaderConstants.E_AUTH_TOKEN);
                if (token != null) {
                    try {
                        token.setText(authToken.getEncoded());
                    } catch (AuthTokenException ate) {
                        throw ServiceException.PROXY_ERROR(ate, qName.getName());
                    }
                }
            }
        }
        ZimbraLog.soap.debug("Modified auth token in soap envelope, csrf token flag is disabled. The new envelope : %s", envelope);
    }

    @Override
    public String toString() {
        return "ProxyTarget(url=" + mURL + ")";
    }
}
