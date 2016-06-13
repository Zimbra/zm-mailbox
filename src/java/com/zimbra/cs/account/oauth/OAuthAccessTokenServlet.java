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

package com.zimbra.cs.account.oauth;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.oauth.utils.OAuthServiceProvider;
import com.zimbra.cs.servlet.ZimbraServlet;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.server.OAuthServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Servlet to handle OAuth access token request(service/oauth/access_token)
 *
 * @author pgajjar
 */
public class OAuthAccessTokenServlet extends ZimbraServlet {

    private static final Log LOG = ZimbraLog.oauth;

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        LOG.debug("Access Token Handler doGet requested!");
        processRequest(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        LOG.debug("Access Token Handler doPost requested!");
        processRequest(request, response);
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try{
            String origUrl = request.getHeader("X-Zimbra-Orig-Url");
            OAuthMessage oAuthMessage =
                    StringUtil.isNullOrEmpty(origUrl) ?
                            OAuthServlet.getMessage(request, null) : OAuthServlet.getMessage(request, origUrl);

            OAuthAccessor accessor = OAuthServiceProvider.getAccessor(oAuthMessage);
            OAuthServiceProvider.VALIDATOR.validateAccTokenMessage(oAuthMessage, accessor);

            // make sure token is authorized
            if (!Boolean.TRUE.equals(accessor.getProperty("authorized"))) {
                 OAuthProblemException problem = new OAuthProblemException("permission_denied");
                 LOG.debug("permission_denied");
                 throw problem;
            }

            AuthToken userAuthToken = ZimbraAuthToken.getAuthToken((String) accessor.getProperty("ZM_AUTH_TOKEN"));
            String accountId = userAuthToken.getAccountId();
            Account account = Provisioning.getInstance().getAccountById(accountId);

            // generate access token and secret
            OAuthServiceProvider.generateAccessToken(accessor);

            account.addForeignPrincipal("oAuthAccessToken:" + accessor.accessToken);
            account.addOAuthAccessor(accessor.accessToken + "::" + new OAuthAccessorSerializer().serialize(accessor));

            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();
            OAuth.formEncode(OAuth.newList("oauth_token", accessor.accessToken,
                            "oauth_token_secret", accessor.tokenSecret), out);
            out.close();
        } catch (Exception e) {
            LOG.debug("AccessTokenHandler exception", e);
            OAuthServiceProvider.handleException(e, request, response, true);
        }
    }

    private static final long serialVersionUID = 4514844700722250184L;
}
