/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.authenticator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.Arrays;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SpnegoLoginService;
import org.eclipse.jetty.security.SpnegoUserIdentity;
import org.eclipse.jetty.security.SpnegoUserPrincipal;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.krb5.Krb5Principal;
import com.zimbra.cs.servlet.util.AuthUtil;

public class SpnegoAuthenticator extends SSOAuthenticator {

    private final SpnegoLoginService spnegoUserRealm;
    private String error401Page;

    public SpnegoAuthenticator(HttpServletRequest req, HttpServletResponse resp, SpnegoLoginService spnegoUserRealm) {
        super(req, resp);
        this.spnegoUserRealm = spnegoUserRealm;
    }

    public SpnegoAuthenticator(HttpServletRequest req, HttpServletResponse resp, SpnegoLoginService spnegoUserRealm, String error401Page) {
        this(req, resp, spnegoUserRealm);
        this.error401Page = error401Page;
    }

    @Override
    public String getAuthType() {
        return "Spnego";
    }

    @Override
    public ZimbraPrincipal authenticate() throws ServiceException {
        Request request = (req instanceof Request) ? (Request)req : null;

        if (request == null) {
            throw ServiceException.FAILURE("not supported", null);
        }
        return getPrincipal(request);
    }

    private ZimbraPrincipal getPrincipal(Request request) throws ServiceException {
        ZimbraPrincipal principal = null;

        try {
            principal = authenticate(spnegoUserRealm, request, resp);

            // comment out the above and uncomment the line below for quick testing
            // non-spenogo related issues, like redirect.
            // principal = MockSpnegoUser.getMockPrincipal();
        } catch (IOException e) {
            throw AuthFailedServiceException.AUTH_FAILED("spnego authenticate failed", e);
        }

        if (principal == null) {
            throw AuthFailedServiceException.AUTH_FAILED("spnego authenticate failed", (Throwable)null);
        }

        return principal;
    }


    private Account getAccountByPrincipal(Principal principal) throws ServiceException {

        String krb5Name = principal.getName();

        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.krb5Principal, krb5Name);

        if (acct == null) {
            Domain domain = Krb5Principal.getDomainByKrb5Principal(krb5Name);
            if (domain != null) {
                acct = prov.autoProvAccountLazy(domain, krb5Name, null, AutoProvAuthMech.SPNEGO);
            }
        }
        return acct;
    }

    /* =========================================================
     *
     * Based on org.eclipse.jetty.security.SpnegoAuthenticator
     *
     * =========================================================
     */
    private ZimbraPrincipal authenticate(LoginService realm, Request request, HttpServletResponse response)
    throws ServiceException, IOException {
        Principal user = null;
        String header = request.getHeader(HttpHeader.AUTHORIZATION.toString());

        /*
         * if the header is null then we need to challenge...this is after the error page check
         */
        if (header == null) {
            sendChallenge(realm,request,response);
            throw SSOAuthenticatorServiceException.SENT_CHALLENGE();

        } else if (header != null && header.startsWith(HttpHeader.NEGOTIATE.toString())) {
            /*
             * we have gotten a negotiate header to try and authenticate
             */

            // skip over "Negotiate "
            String token = header.substring(10);

            UserIdentity identity = realm.login(null, token, request);
            if (identity == null) {
                throw AuthFailedServiceException.AUTH_FAILED("SpnegoAuthenticator: unable to login", (Throwable)null);
            }
            user = identity.getUserPrincipal();

            if (user != null) {
                ZimbraLog.account.debug("SpnegoAuthenticator: obtained principal: " + user.getName());

                Account acct = getAccountByPrincipal(user);
                ZimbraPrincipal zimbraPrincipal = new ZimbraPrincipal(user.getName(), acct);
                String clientName = ((SpnegoUserPrincipal)user).getName();
                String role = clientName.substring(clientName.indexOf('@') + 1);
                String[] roles = new String[] {role};
                DefaultUserIdentity defaultUserIdentity = new DefaultUserIdentity(identity.getSubject(), zimbraPrincipal, roles);
                SpnegoUserIdentity spnegoUserIdentity = new SpnegoUserIdentity(identity.getSubject(), zimbraPrincipal, defaultUserIdentity);
                Authentication authentication = new UserAuthentication(getAuthType(), spnegoUserIdentity);
                request.setAuthentication(authentication);
                response.addHeader(HttpHeader.WWW_AUTHENTICATE.toString(), HttpHeader.NEGOTIATE.toString() + " " + ((SpnegoUserPrincipal)user).getToken());

                return zimbraPrincipal;
            }
            else {
                /*
                 * no user was returned from the authentication which means something failed
                 * so process error logic
                 */
                ZimbraLog.account.debug("SpnegoAuthenticator: no user found, authentication failed");
                throw AuthFailedServiceException.AUTH_FAILED("SpnegoAuthenticator: no user found, authentication failed", (Throwable)null);
            }
        } else {
            /*
             * the header was not null, but we didn't get a negotiate so process error logic
             */
            throw AuthFailedServiceException.AUTH_FAILED(
                    "SpnegoAuthenticator: authentication failed, unknown header (browser is likely misconfigured for SPNEGO)", (Throwable)null);
        }
    }

    public void sendChallenge(LoginService realm, Request request, HttpServletResponse response) throws IOException {
        ZimbraLog.account.debug("SpnegoAuthenticator: sending challenge");
        response.setHeader(HttpHeader.WWW_AUTHENTICATE.toString(), HttpHeader.NEGOTIATE.toString());
        //Custom 401 error page.
        try {
            String redirectUrl = Provisioning.getInstance().getConfig().getSpnegoAuthErrorURL();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (!StringUtil.isNullOrEmpty(redirectUrl)) {
                request.setAttribute("spnego.auto.redirect", true);
            }
            if (!StringUtil.isNullOrEmpty(error401Page)) {
                request.setAttribute("spnego.redirect.url", getErrorRedirectUrl(request));
                RequestDispatcher requestDispatcher = request.getRequestDispatcher(error401Page);
                requestDispatcher.forward(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            //jetty default error page.
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String getErrorRedirectUrl(HttpServletRequest httpServletReq)
        throws ServiceException, MalformedURLException {
        String redirectUrl = Provisioning.getInstance().getConfig().getSpnegoAuthErrorURL();
        if (redirectUrl == null) {
            Server server = Provisioning.getInstance().getLocalServer();
            boolean isAdminRequest = AuthUtil.isAdminRequest(httpServletReq);
            redirectUrl = AuthUtil.getRedirectURL(httpServletReq, server, isAdminRequest, true);
            // always append the ignore loginURL query so we do not get into a redirect loop.
            redirectUrl = redirectUrl.endsWith("/") ? redirectUrl : redirectUrl + "/";
            redirectUrl = redirectUrl + AuthUtil.IGNORE_LOGIN_URL;
        }
        return redirectUrl;
    }

    private static class MockSpnegoUser implements Principal {
        private final String name;
        private final String token;

        private static ZimbraPrincipal getMockPrincipal() throws IOException {
            Principal principal = new MockSpnegoUser("spnego@SPNEGO.LOCAL", "blah");
            ZimbraPrincipal zimbraPrincipal = null;
            try {
                zimbraPrincipal = new ZimbraPrincipal(principal.getName(), GuestAccount.ANONYMOUS_ACCT);
            } catch (ServiceException e) {
            }
            return zimbraPrincipal;
        }

        MockSpnegoUser(String name, String token) {
            this.name = name;
            this.token = token;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getToken() {
            return token;
        }

    }

}
