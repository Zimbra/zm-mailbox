/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
package com.zimbra.cs.service.sso;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.service.AuthProvider;

/**
 * Second-factor gate for single-sign-on doors.
 * <p>
 * An SSO door -- PreAuth's HMAC signature, a validated SAML assertion -- proves only that some
 * upstream system vouched for the user. If the account additionally requires a second factor, the
 * door must NOT mint a usable auth token; it must hand the browser off to the code challenge, or to
 * enrolment when 2FA is available but has never been set up.
 * <p>
 * This logic was originally inline in {@code PreAuthServlet} (ZCS-20575). It is hoisted here so the
 * SAML consumer extension -- which ships in a separate jar ({@code zm-saml-consumer-store}) and
 * cannot see {@code PreAuthServlet}'s private methods -- gates identically. Every SSO door should
 * call {@link #gate} immediately after it has resolved and status-checked the account, and before it
 * writes any session cookie.
 * <p>
 * Typical use:
 * <pre>
 *     if (SsoTwoFactorGate.gate(req, resp, acct, "SAML")) {
 *         return;   // a redirect has been written; do not issue the session
 *     }
 * </pre>
 *
 * @see com.zimbra.cs.service.account.PreAuthTwoFactorSetup  password-free enrolment leg
 */
public final class SsoTwoFactorGate {

    private SsoTwoFactorGate() {
    }

    /** Handed to the web client when SSO succeeds but a second factor is still required. */
    public static final String PARAM_TFA = "tfa";
    /** Masked address the code was sent to, so the challenge page can say where to look. */
    public static final String PARAM_TFA_EMAIL = "tfaEmail";
    /** Set when 2FA is available but has never been set up: tells the client to open enrolment. */
    public static final String PARAM_TFA_ENROLL = "tfaEnroll";
    /** Account the challenge belongs to. */
    public static final String PARAM_ACCOUNT = "account";

    private static final String DEFAULT_MAIL_URL = "/zimbra";
    /**
     * Standalone challenge page, relative to zimbraMailURL. Deliberately not the SPA, for the same
     * reason as enrolment below: the SPA's auth guards all assume an unauthenticated user belongs on
     * the login screen, and the challenge path issues no session cookie by design -- so the SPA
     * bounces straight back to its own login form and the handoff is lost.
     */
    private static final String TWO_FACTOR_CHALLENGE_PATH = "modern/tfa-challenge.html";
    /**
     * Standalone enrolment page. Deliberately not the SPA: the SPA's auth guards all assume an
     * unauthenticated user belongs on the login screen, which fights this flow.
     */
    private static final String TWO_FACTOR_ENROLL_PATH = "modern/tfa-enroll.html";

    /** What this account needs before it can be given a usable auth token. */
    public enum Decision {
        /** No second factor required -- mint the auth token as normal. */
        NONE,
        /** 2FA is set up: challenge for a code. */
        CHALLENGE,
        /** 2FA is available but has never been set up: send the user to enrolment. */
        SETUP
    }

    /**
     * Decide what an account needs before it may be handed a usable session.
     *
     * @param acct the account the SSO door has vouched for
     * @return the gate decision; never {@code null}
     */
    public static Decision evaluate(Account acct) throws ServiceException {
        TwoFactorAuth mgr = TwoFactorAuth.getFactory().getTwoFactorAuth(acct);

        // Already enrolled -> challenge for a code before issuing a session.
        if (mgr.twoFactorAuthEnabled()) {
            return Decision.CHALLENGE;
        }

        // Not enrolled. Offer enrolment whenever the feature is available to the account -- not only
        // when it is mandatory. The extension's twoFactorAuthRequired() is
        // "available && (required || enabled)", so an account with the feature merely available
        // reports required == false; keying off availability is what makes the prompt appear for
        // accounts allowed to use 2FA that have not set it up yet. Enrolment is skippable, and the
        // prompt returns on the next SSO login until they enrol.
        if (acct.isFeatureTwoFactorAuthAvailable()) {
            return Decision.SETUP;
        }

        return Decision.NONE;
    }

    /**
     * Apply the gate. If the account needs a second factor this writes a redirect to the challenge
     * or to enrolment and returns {@code true} -- the caller must then return without issuing a
     * session. If nothing is required it returns {@code false} and writes nothing.
     *
     * @param door short label for the calling door ("PreAuth", "SAML"), used in the security log
     * @return {@code true} if a redirect was written and the caller must stop
     */
    public static boolean gate(HttpServletRequest req, HttpServletResponse resp, Account acct, String door)
    throws ServiceException, IOException {
        switch (evaluate(acct)) {
            case CHALLENGE:
                redirectToChallenge(req, resp, acct, door);
                return true;
            case SETUP:
                redirectToEnrolment(req, resp, acct, door);
                return true;
            default:
                return false;
        }
    }

    /**
     * Send an enrolled user to the code challenge. No session cookie is written: the browser leaves
     * here holding only a {@link Usage#TWO_FACTOR_AUTH} token, which buys nothing but the right to
     * present a code.
     */
    public static void redirectToChallenge(HttpServletRequest req, HttpServletResponse resp, Account acct,
            String door) throws ServiceException, IOException {
        AuthToken tfaToken = AuthProvider.getAuthToken(acct, Usage.TWO_FACTOR_AUTH, null);

        StringBuilder sb = new StringBuilder(challengeBaseUrl(acct));
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
                "cmd", door, "account", acct.getName(), "info", "two-factor auth required" }));

        resp.sendRedirect(sb.toString());
    }

    /**
     * Send a user whose account offers two-factor auth but has never set it up to enrolment.
     * <p>
     * Unlike the challenge path, this issues a normal session cookie before enrolment. That is
     * deliberate: skipping enrolment is permitted (the user has already been vouched for by the SSO
     * door), so withholding the session would gain nothing a skip would not give back. Issuing it up
     * front lets the stock two-factor setup dialog run, which authenticates
     * EnableTwoFactorAuthRequest by session rather than by password -- the password an SSO flow
     * never sees.
     * <p>
     * The account is left un-enrolled if the user dismisses the dialog, and is prompted again on the
     * next SSO login.
     */
    public static void redirectToEnrolment(HttpServletRequest req, HttpServletResponse resp, Account acct,
            String door) throws ServiceException, IOException {
        AuthToken at = AuthProvider.getAuthToken(acct);
        at.setCsrfTokenEnabled(true);
        at.encode(resp, false, "https".equals(req.getScheme()));

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {
                "cmd", door, "account", acct.getName(), "info", "two-factor auth enrolment required" }));

        String base = mailUrlBase(acct);

        // The enrolment page cannot authenticate with the session: SOAP here rejects cookie-only
        // auth and the cookie is HttpOnly. Give it a token scoped to enrolment only -- strictly less
        // than the session cookie the browser already holds at this point.
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

    /** zimbraMailURL for the account's server, always with a trailing slash. */
    private static String mailUrlBase(Account acct) throws ServiceException {
        Server server = Provisioning.getInstance().getServer(acct);
        String baseUrl = server.getAttr(Provisioning.A_zimbraMailURL, DEFAULT_MAIL_URL);
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private static String challengeBaseUrl(Account acct) throws ServiceException {
        String baseUrl = mailUrlBase(acct) + TWO_FACTOR_CHALLENGE_PATH;
        return baseUrl + (baseUrl.indexOf('?') < 0 ? '?' : '&');
    }
}
