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

/*
 * Created on Dec 20, 2004
 * @author Greg Solovyev
 * */
package com.zimbra.cs.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.account.names.NameUtil.EmailAddress;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.AccountUtil;

public class PreAuthServlet extends ZimbraServlet {

    public static final String PARAM_PREAUTH = "preauth";
    public static final String PARAM_AUTHTOKEN = "authtoken";
    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_ADMIN = "admin";
    public static final String PARAM_ISREDIRECT = "isredirect";
    public static final String PARAM_BY = "by";
    public static final String PARAM_REDIRECT_URL = "redirectURL";
    public static final String PARAM_TIMESTAMP = "timestamp";
    public static final String PARAM_EXPIRES = "expires";

    // Handed to the web client when preauth succeeds but a second factor is still required.
    public static final String PARAM_TFA = "tfa";
    public static final String PARAM_TFA_EMAIL = "tfaEmail";
    // Set when 2FA is required but has never been set up: tells the client to open enrolment.
    public static final String PARAM_TFA_ENROLL = "tfaEnroll";

    // Path of the client that renders the 2FA challenge, relative to zimbraMailURL.
    private static final String TWO_FACTOR_CHALLENGE_PATH = "modern/";
    // Standalone enrolment page. Deliberately not the SPA: the SPA's auth guards all assume an
    // unauthenticated user belongs on the login screen, which fights this flow.
    private static final String TWO_FACTOR_ENROLL_PATH = "modern/tfa-enroll.html";

    private static final HashSet<String> sPreAuthParams = new HashSet<String>();

    static {
        sPreAuthParams.add(PARAM_PREAUTH);
        sPreAuthParams.add(PARAM_AUTHTOKEN);
        sPreAuthParams.add(PARAM_ACCOUNT);
        sPreAuthParams.add(PARAM_ADMIN);
        sPreAuthParams.add(PARAM_ISREDIRECT);
        sPreAuthParams.add(PARAM_BY);
        sPreAuthParams.add(PARAM_TIMESTAMP);
        sPreAuthParams.add(PARAM_EXPIRES);
    }

    @Override
    public void init() throws ServletException {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " starting up");
        super.init();
    }

    @Override
    public void destroy() {
        String name = getServletName();
        ZimbraLog.account.info("Servlet " + name + " shutting down");
        super.destroy();
    }

    private String getRequiredParam(HttpServletRequest req, HttpServletResponse resp, String paramName) throws ServiceException {
        String param = req.getParameter(paramName);
        if (param == null) throw ServiceException.INVALID_REQUEST("missing required param: "+paramName, null);
        else return param;
    }

    private String getOptionalParam(HttpServletRequest req, String paramName, String def) {
        String param = req.getParameter(paramName);
        if (param == null) return def;
        else return param;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        ZimbraLog.clearContext();
        try {
            Provisioning prov = Provisioning.getInstance();

            Server server = prov.getLocalServer();
            String referMode = server.getAttr(Provisioning.A_zimbraMailReferMode, "wronghost");
            boolean isRedirect = getOptionalParam(req, PARAM_ISREDIRECT, "0").equals("1");
            String rawAuthToken = getOptionalParam(req, PARAM_AUTHTOKEN, null);
            AuthToken authToken = null;
            if (rawAuthToken != null) {
                authToken = AuthProvider.getAuthToken(rawAuthToken);
                if (authToken == null) {
                    throw new AuthTokenException("unable to get auth token from " + PARAM_AUTHTOKEN);
                } else if (authToken.isExpired()) {
                    throw new AuthTokenException("auth token expired");
                } else if (!authToken.isRegistered()) {
                    throw new AuthTokenException("authtoken is invalid");
                }
            }

            if (rawAuthToken != null) {
                if (!authToken.isRegistered()) {
                    throw new AuthTokenException("authtoken is not registered");
                }
                if (authToken.isExpired()) {
                    throw new AuthTokenException("authtoken is expired registered");
                }
                // we've got an auth token in the request:
                // See if we need a redirect to the correct server
                boolean isAdmin = authToken != null && AuthToken.isAnyAdmin(authToken);
                Account acct = prov.get(AccountBy.id, authToken.getAccountId(), authToken);
                if (isAdmin || !needReferral(acct, referMode, isRedirect)) {
                    //authtoken in get request is for one time use only. Deregister and generate new one.
                    if (authToken instanceof ZimbraAuthToken) {
                        ZimbraAuthToken  oneTimeToken = (ZimbraAuthToken) authToken;
                        ZimbraAuthToken newZimbraAuthToken = null;
                        try {
                            newZimbraAuthToken = oneTimeToken.clone();
                        } catch (CloneNotSupportedException e) {
                            throw new ServletException(e);
                        }
                        newZimbraAuthToken.resetTokenId();
                        
                        oneTimeToken.deRegister();
                        authToken = newZimbraAuthToken;
                        ZimbraLog.account.debug("Deregistered the one time preauth token and issuing new one to the user.");
                    }

                    authToken.setCsrfTokenEnabled(true); // ZBUG-2662
                    // no need to redirect to the correct server, just send them off to do business
                    setCookieAndRedirect(req, resp, authToken, acct);
                } else {
                    // redirect to the correct server with the incoming auth token
                    // we no longer send the auth token we generate over when we redirect to the correct server,
                    // but customer can be sending a token in their preauth URL, in this case, just
                    // send over the auth token as is.
                    redirectToCorrectServer(req, resp, acct, rawAuthToken);
                }
            } else {
                // no auth token in the request URL.  See if we should redirect this request
                // to the correct server, or should do the preauth locally.

                String preAuth = getRequiredParam(req, resp, PARAM_PREAUTH);
                String account = getRequiredParam(req, resp, PARAM_ACCOUNT);
                String accountBy = getOptionalParam(req, PARAM_BY, AccountBy.name.name());
                AccountBy by = AccountBy.fromString(accountBy);

                boolean admin = getOptionalParam(req, PARAM_ADMIN, "0").equals("1") && isAdminRequest(req);
                long timestamp = Long.parseLong(getRequiredParam(req, resp, PARAM_TIMESTAMP));
                long expires = Long.parseLong(getRequiredParam(req, resp, PARAM_EXPIRES));

                Account acct = null;
                acct = prov.get(by, account, authToken);

                Map<String, Object> authCtxt = new HashMap<String, Object>();
                authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, ZimbraServlet.getOrigIp(req));
                authCtxt.put(AuthContext.AC_REMOTE_IP, ZimbraServlet.getClientIp(req));
                authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, account);
                authCtxt.put(AuthContext.AC_USER_AGENT, req.getHeader("User-Agent"));

                boolean acctAutoProvisioned = false;
                if (acct == null) {
                    //
                    // try auto provision the account
                    //
                    if (by == AccountBy.name && !admin) {
                        try {
                            EmailAddress email = new EmailAddress(account, false);
                            String domainName = email.getDomain();
                            Domain domain = domainName == null ? null : prov.get(Key.DomainBy.name, domainName);
                            if (domain == null) {
                                throw AccountServiceException.NO_SUCH_DOMAIN(domainName);
                            }
                            prov.preAuthAccount(domain, account, accountBy, timestamp, expires, preAuth, authCtxt);
                            acct = prov.autoProvAccountLazy(domain, account, null, AutoProvAuthMech.PREAUTH);

                            if (acct != null) {
                                acctAutoProvisioned = true;
                            }
                        } catch (AuthFailedServiceException e) {
                            ZimbraLog.account.debug("auth failed, unable to auto provision acct " + account, e);
                        } catch (ServiceException e) {
                            ZimbraLog.account.info("unable to auto provision acct " + account, e);
                        }
                    }
                }

                if (acct == null) {
                    throw AuthFailedServiceException.AUTH_FAILED(account, account, "account not found");
                }
                
                String accountStatus = acct.getAccountStatus(prov);
                if (!Provisioning.ACCOUNT_STATUS_ACTIVE.equalsIgnoreCase(accountStatus)) {
                    if(Provisioning.ACCOUNT_STATUS_MAINTENANCE.equalsIgnoreCase(accountStatus)) {
                        throw AccountServiceException.MAINTENANCE_MODE();
                    } else {
                        throw AccountServiceException.ACCOUNT_INACTIVE(acct.getName());
                    }
                } 

                if (admin) {
                    boolean isDomainAdminAccount = acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
                    boolean isAdminAccount = acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
                    boolean isDelegatedAdminAccount = acct.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false);
                    boolean ok = (isDomainAdminAccount || isAdminAccount || isDelegatedAdminAccount);
                    if (!ok)
                        throw ServiceException.PERM_DENIED("not an admin account");
                }

                // all params are well, now see if we should preauth locally or redirect to the correct server.

                if (admin || !needReferral(acct, referMode, isRedirect)) {
                    // do preauth locally
                    if (!acctAutoProvisioned) {
                        prov.preAuthAccount(acct, account, accountBy, timestamp, expires, preAuth, admin, authCtxt);
                    }

                    // The preauth signature has been verified at this point, but that only proves the
                    // upstream system vouched for the user.  If the account additionally requires a
                    // second factor, do NOT mint a usable auth token here -- mint a limited-usage
                    // token instead and hand the browser off to the challenge (or to enrolment,
                    // when 2FA is required but has never been set up).
                    if (!admin) {
                        switch (twoFactorState(acct)) {
                            case CHALLENGE:
                                redirectToTwoFactorChallenge(req, resp, acct);
                                return;
                            case SETUP:
                                redirectToTwoFactorEnrolment(req, resp, acct);
                                return;
                            default:
                                break;
                        }
                    }

                    AuthToken at;

                    if (admin)
                        at = (expires ==  0) ? AuthProvider.getAuthToken(acct, admin) : AuthProvider.getAuthToken(acct, expires, admin, null);
                    else
                        at = (expires ==  0) ? AuthProvider.getAuthToken(acct) : AuthProvider.getAuthToken(acct, expires);

                    at.setCsrfTokenEnabled(true); // ZBUG-2662
                    setCookieAndRedirect(req, resp, at, acct);
                } else {
                    // redirect to the correct server.
                    // Note: we do not send over the generated auth token (the auth token param passed to
                    // redirectToCorrectServer is null).
                    redirectToCorrectServer(req, resp, acct, null);
                }
            }
        }catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid numeric parameter");
        }catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }catch (AuthTokenException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private boolean needReferral(Account acct, String referMode, boolean isRedirect) throws ServiceException {
        // if this request is already a redirect, don't redirect again
        if (isRedirect)
            return false;

        return (Provisioning.MAIL_REFER_MODE_ALWAYS.equals(referMode) ||
                (Provisioning.MAIL_REFER_MODE_WRONGHOST.equals(referMode) && !Provisioning.onLocalServer(acct)));
    }

    /** What this account needs before it can be given a usable auth token. */
    private enum TwoFactorState {
        /** No second factor required -- mint the auth token as normal. */
        NONE,
        /** 2FA is set up: challenge for a code. */
        CHALLENGE,
        /** 2FA is required but has never been set up: send the user to enrolment. */
        SETUP
    }

    private TwoFactorState twoFactorState(Account acct) throws ServiceException {
        TwoFactorAuth mgr = TwoFactorAuth.getFactory().getTwoFactorAuth(acct);

        // Already enrolled -> challenge for a code before issuing a session.
        if (mgr.twoFactorAuthEnabled()) {
            return TwoFactorState.CHALLENGE;
        }

        // Not enrolled. Offer enrolment whenever the feature is available to the account -- not
        // only when it is mandatory. Note the extension's twoFactorAuthRequired() is
        // "available && (required || enabled)", so an account with the feature merely available
        // reports required == false; keying off availability is what makes the prompt appear for
        // accounts that are allowed to use 2FA but have not set it up yet. Enrolment is skippable,
        // and the prompt returns on the next preauth login until they enrol.
        if (acct.isFeatureTwoFactorAuthAvailable()) {
            return TwoFactorState.SETUP;
        }

        return TwoFactorState.NONE;
    }

    /**
     * Send a user whose account requires two-factor auth but has never set it up to enrolment.
     * <p>
     * Unlike the challenge path, this issues a normal session cookie before enrolment. That is
     * deliberate: skipping enrolment is permitted (the user has already been vouched for by the
     * preauth signature), so withholding the session would gain nothing a skip would not give
     * back. Issuing it up front lets the stock two-factor setup dialog run, which authenticates
     * EnableTwoFactorAuthRequest by session rather than by password -- the password the preauth
     * flow never sees.
     * <p>
     * The account is left un-enrolled if the user dismisses the dialog, and is prompted again on
     * the next preauth login.
     */
    private void redirectToTwoFactorEnrolment(HttpServletRequest req, HttpServletResponse resp, Account acct)
    throws ServiceException, IOException {
        AuthToken at = AuthProvider.getAuthToken(acct);
        at.setCsrfTokenEnabled(true);
        at.encode(resp, false, req.getScheme().equals("https"));

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {
                "cmd", "PreAuth", "account", acct.getName(),
                "info", "two-factor auth enrolment required" }));

        String base = Provisioning.getInstance().getServer(acct)
                .getAttr(Provisioning.A_zimbraMailURL, DEFAULT_MAIL_URL);
        if (!base.endsWith("/")) {
            base = base + "/";
        }

        // The enrolment page cannot authenticate with the session: SOAP here rejects cookie-only
        // auth and the cookie is HttpOnly. Give it a token scoped to enrolment only -- strictly
        // less than the session cookie the browser already holds at this point.
        AuthToken setupToken = AuthProvider.getAuthToken(acct, Usage.ENABLE_TWO_FACTOR_AUTH, null);
        String url;
        try {
            url = base + TWO_FACTOR_ENROLL_PATH + "?t="
                    + URLEncoder.encode(setupToken.getEncoded(), "utf-8");
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("unable to encode enrolment token", e);
        }
        resp.sendRedirect(url);
    }

    /** Base URL of the client that renders the 2FA challenge/enrolment, with a trailing '?' or '&'. */
    private String twoFactorChallengeBaseUrl(Account acct) throws ServiceException {
        Server server = Provisioning.getInstance().getServer(acct);
        String baseUrl = server.getAttr(Provisioning.A_zimbraMailURL, DEFAULT_MAIL_URL);
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        baseUrl = baseUrl + TWO_FACTOR_CHALLENGE_PATH;
        return baseUrl + (baseUrl.indexOf('?') < 0 ? '?' : '&');
    }

    /**
     * Mint a limited-usage {@link Usage#TWO_FACTOR_AUTH} token and send the browser to the web
     * client, which renders the existing 2FA challenge and completes the login with
     * AuthRequest{authToken, twoFactorCode}. Possession of this token alone grants nothing --
     * the second factor is still required to exchange it for a real auth token.
     */
    private void redirectToTwoFactorChallenge(HttpServletRequest req, HttpServletResponse resp, Account acct)
    throws ServiceException, IOException {
        AuthToken tfaToken = AuthProvider.getAuthToken(acct, Usage.TWO_FACTOR_AUTH, null);

        StringBuilder sb = new StringBuilder(twoFactorChallengeBaseUrl(acct));
        try {
            sb.append(PARAM_TFA).append('=').append(URLEncoder.encode(tfaToken.getEncoded(), "utf-8"));
            sb.append('&').append(PARAM_ACCOUNT).append('=').append(URLEncoder.encode(acct.getName(), "utf-8"));

            // Masked, exactly as AccountUtil.addTwoFactorAttributes() already exposes it in the SOAP
            // AuthResponse at this same pre-2FA trust level -- lets the challenge page say where the
            // code was sent without revealing the full address.
            String recoveryAddress = acct.getPrefPasswordRecoveryAddress();
            if (!StringUtil.isNullOrEmpty(recoveryAddress)) {
                sb.append('&').append(PARAM_TFA_EMAIL).append('=')
                  .append(URLEncoder.encode(StringUtil.maskEmail(recoveryAddress), "utf-8"));
            }
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("unable to encode two-factor auth token", e);
        }

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {
                "cmd", "PreAuth", "account", acct.getName(), "info", "two-factor auth required" }));

        resp.sendRedirect(sb.toString());
    }

    private void addQueryParams(HttpServletRequest req, StringBuilder sb, boolean first, boolean nonPreAuthParamsOnly) {
        Enumeration names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            if (nonPreAuthParamsOnly && sPreAuthParams.contains(name))
                continue;

            String values[] = req.getParameterValues(name);
            if (values != null) {
                for (String value : values) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append('&');
                    }
                    try {
                        sb.append(name).append("=").append(URLEncoder.encode(value, "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        // this should never happen...
                        sb.append(name).append("=").append(URLEncoder.encode(value));
                    }
                }
            }
        }
    }

    /*
     * As a fix for bug 35088, the preauth handler(servlet) will no longer send the authtoken
     * it generates over when it needs to redirect to the correct server, it now sends the original
     * preauth params instead, validating of the preauth will happen on the home server.
     * In this case the token parameter will be null.
     *
     * Although we no longer pass authtoken, some existing customers might be using it
     * as a way to inject an authtoken from a URL into a cookie so we might need to leave it.
     * In this case the token parameter will be non-null.
     *
     */
    private void redirectToCorrectServer(HttpServletRequest req, HttpServletResponse resp, Account acct, String token) throws ServiceException, IOException {
        StringBuilder sb = new StringBuilder();
        Provisioning prov = Provisioning.getInstance();
        sb.append(URLUtil.getServiceURL(prov.getServer(acct), req.getRequestURI(), true));
        sb.append('?').append(PARAM_ISREDIRECT).append('=').append('1');

        if (token != null) {
            sb.append('&').append(PARAM_AUTHTOKEN).append('=').append(token);
            // send only non-preauth (i.e. customer's) params over, since there is already an auth token, the preauth params would be useless anyway
            addQueryParams(req, sb, false, true);
        } else {
            // send all incoming params over
            addQueryParams(req, sb, false, false);
        }
        resp.sendRedirect(sb.toString());
    }

    private static final String DEFAULT_MAIL_URL = "/zimbra";
    private static final String DEFAULT_ADMIN_URL = "/zimbraAdmin";

    private void setCookieAndRedirect(HttpServletRequest req, HttpServletResponse resp, AuthToken authToken, Account acct) throws IOException, ServiceException {
        boolean isAdmin = AuthToken.isAnyAdmin(authToken);
        boolean secureCookie = req.getScheme().equals("https");
        authToken.encode(resp, isAdmin, secureCookie);

        String redirectURL = getOptionalParam(req, PARAM_REDIRECT_URL, null);
        URL url = null;
        try {
            url = new URL(redirectURL);
        } catch (MalformedURLException exp) {
            ZimbraLog.account.debug(String.format("URL %s is a malformed URL", redirectURL), exp);
        }

        if (url != null) {
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            String protocolPattern = "^(http|https|ftp|file)://.*$";
            if (redirectURL.matches(protocolPattern)) {
                String replaceProtocol = String.format("%s://", protocol);
                redirectURL = redirectURL.replace(replaceProtocol, "");
            }
            if (host != null) {
                String strToReplace = null;
                if (port == -1) {
                    strToReplace = String.format("%s", host);
                } else {
                    strToReplace = String.format("%s:%d", host, port);
                }
                if (strToReplace != "") {
                    redirectURL = redirectURL.replace(strToReplace, "");
                }
            }
        }

        if (redirectURL != null) {
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getServer(acct);
            Domain domain = prov.getDomain(acct);
            String publicURLForDomain = null;
            if (server != null) {
                publicURLForDomain = URLUtil.getPublicURLForDomain(server, domain, "", true);
            }
            String zimbraAllowedRedirectURL = LC.zimbra_allowed_redirect_url.value();

            // ZBUG-3105: we are allowing redirectURL only from zimbraPublicServiceHostname and
            // also url from zimbra_allowed_redirect_url
            ZimbraLog.account.debug("redirectURL: %s received for user account: %s", redirectURL, acct.getName());
            if ((!StringUtil.isNullOrEmpty(publicURLForDomain) && redirectURL.startsWith(publicURLForDomain))
                    || (!StringUtil.isNullOrEmpty(zimbraAllowedRedirectURL)
                            && redirectURL.startsWith(zimbraAllowedRedirectURL))) {
                resp.sendRedirect(redirectURL);
            } else {
                ZimbraLog.account.warn("Invalid redirectURL received for user account: %s", acct.getName());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            addQueryParams(req, sb, true, true);
            Provisioning prov = Provisioning.getInstance();
            Server server = prov.getServer(authToken.getAccount());
            String redirectUrl;

            if (isAdmin) {
                redirectUrl = server.getAttr(Provisioning.A_zimbraAdminURL, DEFAULT_ADMIN_URL);
            } else {
                redirectUrl = server.getAttr(Provisioning.A_zimbraMailURL, DEFAULT_MAIL_URL);
                // NB: do we really have to add the mail app to the end?
                if (redirectUrl.charAt(redirectUrl.length() - 1) == '/') {
                    redirectUrl += "mail";
                } else {
                    redirectUrl += "/mail";
                }
            }
            if (sb.length() > 0) {
                resp.sendRedirect(redirectUrl + "?" + sb.toString());
            } else {
                resp.sendRedirect(redirectUrl);
            }
        }
    }

}
