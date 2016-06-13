/*
 * Copyright 2009 Yutaka Obuchi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//The original is modified for handling OAuth token in Zimbra

// Original's copyright and license terms
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.service;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.oauth.OAuthAccessorSerializer;
import com.zimbra.cs.account.oauth.utils.OAuthServiceProvider;
import com.zimbra.soap.SoapServlet;

public class ZimbraAuthProviderForOAuth extends AuthProvider{

    public static final String ZIMBRA_OAUTH_PROVIDER = "sampleoauth";

    public ZimbraAuthProviderForOAuth() {
        this(ZIMBRA_OAUTH_PROVIDER);
    }

    protected ZimbraAuthProviderForOAuth(String name) {
        super(name);
    }

    @Override
    protected AuthToken authToken(HttpServletRequest req, boolean isAdminReq) throws AuthProviderException, AuthTokenException {

        ZimbraLog.extensions.debug("authToken(HttpServletRequest req, boolean isAdminReq) is requested.");
        if (isAdminReq) {
            ZimbraLog.extensions.debug("isAdminReq:true");
            return null;
        }

        String origUrl = req.getHeader("X-Zimbra-Orig-Url");
        OAuthMessage oAuthMessage;
        if (StringUtil.isNullOrEmpty(origUrl)) {
            ZimbraLog.extensions.debug("request.getRequestURL(): " + req.getRequestURL());
            oAuthMessage = OAuthServlet.getMessage(req, null);
        } else {
            ZimbraLog.extensions.debug("X-Zimbra-Orig-Url: " + origUrl);
            oAuthMessage = OAuthServlet.getMessage(req, origUrl);
        }

        String accessToken;
        try {
            accessToken = oAuthMessage.getToken();
        } catch (IOException e) {
            ZimbraLog.extensions.debug("Error in getting OAuth token from request", e);
            throw AuthProviderException.FAILURE(e.getMessage());
        }
        if (accessToken == null) {
            ZimbraLog.extensions.debug("no need for further oauth processing");
            throw AuthProviderException.NO_AUTH_DATA();
        }

        Account account;
        try {
            account = Provisioning.getInstance().getAccountByForeignPrincipal("oAuthAccessToken:" + accessToken);
        } catch (ServiceException e) {
            ZimbraLog.extensions.warn("Error in getting account using OAuth access token", e);
            throw AuthProviderException.FAILURE(e.getMessage());
        }
        if (account == null) {
            throw AuthProviderException.FAILURE("Could not identify account corresponding to the OAuth request");
        }

        OAuthAccessor accessor = null;
        String[] accessors = account.getOAuthAccessor();
        for (String val : accessors) {
            if (val.startsWith(accessToken)) {
                try {
                    accessor = new OAuthAccessorSerializer().deserialize(val.substring(accessToken.length() + 2));
                } catch (ServiceException e) {
                    throw AuthProviderException.FAILURE("Error in deserializing OAuth accessor");
                }
                break;
            }
        }
        if (accessor == null)
            throw new AuthTokenException("invalid OAuth token");
        try {
            OAuthServiceProvider.VALIDATOR.validateMessage(oAuthMessage, accessor);
        } catch (OAuthProblemException e) {
            for (Map.Entry<String, Object> entry : e.getParameters().entrySet()) {
                ZimbraLog.extensions.debug(entry.getKey() + ":" + entry.getValue());
            }
            ZimbraLog.extensions.debug("Exception in validating OAuth token", e);
            throw new AuthTokenException("Exception in validating OAuth token", e);
        } catch (Exception e) {
            ZimbraLog.extensions.debug("Exception in validating OAuth token", e);
            throw new AuthTokenException("Exception in validating OAuth token", e);
        }

        return AuthProvider.getAuthToken(account);
    }

    @Override
    protected AuthToken authToken(Element soapCtxt, Map engineCtxt) throws AuthProviderException, AuthTokenException  {
        HttpServletRequest hsr = (HttpServletRequest) engineCtxt.get(SoapServlet.SERVLET_REQUEST);
        return authToken(hsr, false);
    }

    @Override
    protected AuthToken authToken(String encoded) throws AuthProviderException, AuthTokenException {
        return genAuthToken(encoded);
    }

    private AuthToken genAuthToken(String encodedAuthToken) throws AuthProviderException, AuthTokenException {
        if (StringUtil.isNullOrEmpty(encodedAuthToken))
            throw AuthProviderException.NO_AUTH_DATA();

        return ZimbraAuthToken.getAuthToken(encodedAuthToken);
    }
}
