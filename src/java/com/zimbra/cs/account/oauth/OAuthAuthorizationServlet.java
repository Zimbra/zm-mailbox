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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.oauth.utils.OAuthServiceProvider;
import com.zimbra.cs.servlet.ZimbraServlet;

/**
 * Authorization request handler for OAuth.
 *
 * @author pgajjar
 */

public class OAuthAuthorizationServlet extends ZimbraServlet {

    private static final Log LOG = ZimbraLog.oauth;

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        LOG.debug("Authorization Handler doGet requested!");
        try{
            OAuthMessage oAuthMessage = OAuthServlet.getMessage(request, null);
            OAuthAccessor accessor = OAuthServiceProvider.getAccessor(oAuthMessage);

            if (Boolean.TRUE.equals(accessor.getProperty("authorized"))) {
                // already authorized send the user back
                returnToConsumer(request, response, accessor);
            } else {
                sendToAuthorizePage(request, response, accessor);
            }
        } catch (Exception e){
            OAuthServiceProvider.handleException(e, request, response, true);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException{
        LOG.debug("Authorization Handler doPost requested!");

        try{
            OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
            OAuthAccessor accessor = OAuthServiceProvider.getAccessor(requestMessage);

            //status can be yes/no(accept/declined)
            String status = (String) request.getAttribute("STATUS");

            if (null != status && status.equals("no")) {
                LOG.debug("Access to zimbra message is denied.");
                OAuthTokenCache.remove(accessor.requestToken, OAuthTokenCache.REQUEST_TOKEN_TYPE);
                sendUnauthorizedResponse(response, accessor);
                return;
            }

            String username = request.getParameter("username");
            String zmtoken = (String) request.getAttribute("ZM_AUTH_TOKEN");

            LOG.debug(
                    "[AuthorizationHandlerInput] username = %s, oauth_token = %s, ZM_AUTH_TOKEN = %s",
                    username, request.getParameter("oauth_token"), zmtoken);

            if(zmtoken == null) {
                sendToAuthorizePage(request, response, accessor);
            } else {
                OAuthServiceProvider.markAsAuthorized(accessor, request.getParameter("username"), zmtoken);
                OAuthServiceProvider.generateVerifier(accessor);
                returnToConsumer(request, response, accessor);
            }
        } catch (Exception e) {
            LOG.debug("AuthorizationHandler exception", e);
            OAuthServiceProvider.handleException(e, request, response, true);
        }
    }

    private void sendToAuthorizePage(HttpServletRequest request,
            HttpServletResponse response, OAuthAccessor accessor)
                    throws IOException, ServletException{
        String consumer_app_name = (String)accessor.consumer.getProperty("app_name");

        LOG.debug(
                "[AuthorizationHandlerOutputToAuthorizePage] request token = %s, consumer-app = %s, ZM_AUTH_TOKEN = %s",
                accessor.requestToken, consumer_app_name, request.getParameter("oauth_token"));

        request.setAttribute("CONS_APP_NAME", consumer_app_name);
        request.setAttribute("TOKEN", accessor.requestToken);

        RequestDispatcher dispatcher = getServletContext().getContext("/zimbra").getRequestDispatcher("/public/authorize.jsp");
        if (dispatcher != null) {
            dispatcher.forward(request, response);
            return;
        }
    }

    private void returnToConsumer(HttpServletRequest request,
            HttpServletResponse response, OAuthAccessor accessor)
                    throws IOException, ServletException{
        // send the user back to site's callBackUrl
        String callback = (String) accessor.getProperty(OAuth.OAUTH_CALLBACK);

        if("oob".equals(callback) ) {
            // no call back it must be a client
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("You have successfully authorized '"
                    + accessor.consumer.getProperty("app_name")
                    + "'. Your verification code is "
                    + accessor.getProperty(OAuth.OAUTH_VERIFIER).toString()
                    + ". Please close this browser window and click continue"
                    + " in the client.");
            out.close();
        } else {
            String token = accessor.requestToken;
            String verifier = accessor.getProperty(OAuth.OAUTH_VERIFIER).toString();
            if (token != null) {
                callback = OAuth.addParameters(callback, "oauth_token", token, OAuth.OAUTH_VERIFIER, verifier);
            }

            callback = String.format
                    ("%s&zimbra_cn=%s&zimbra_givenname=%s&zimbra_sn=%s&zimbra_email=%s&zimbra_displayname=%s", callback,
                            accessor.getProperty("ZM_ACC_CN"),
                            accessor.getProperty("ZM_ACC_GIVENNAME"),
                            accessor.getProperty("ZM_ACC_SN"),
                            accessor.getProperty("ZM_ACC_EMAIL"),
                            accessor.getProperty("ZM_ACC_DISPLAYNAME"));

            LOG.debug("[AuthorizationHandlerRedirectURL]" + callback);

            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", callback);
            //not sending back ZM_AUTH_TOKEN to consumer
            response.setHeader("Set-Cookie", "");
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response,
            OAuthAccessor accessor) throws IOException {
        String callback = (String) accessor.getProperty(OAuth.OAUTH_CALLBACK);
        callback += "?authorized=false";
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.sendRedirect(callback);
    }

    private static final long serialVersionUID = 6775946952939185091L;
}
