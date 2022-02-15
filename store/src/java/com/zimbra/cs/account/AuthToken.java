/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
 * Created on May 30, 2004
 */
package com.zimbra.cs.account;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;

import com.zimbra.common.account.Key;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;


/**
 * @author schemers
 */
public abstract class AuthToken {

    public static final long DEFAULT_AUTH_LIFETIME = 60*60*12;
    public static final long DEFAULT_TWO_FACTOR_AUTH_LIFETIME = 60*60;
    public static final long DEFAULT_TWO_FACTOR_ENABLEMENT_AUTH_LIFETIME = 60*60;

    public static String generateDigest(String a, String b) {
        if (a == null)
            return null;
        StringBuilder buf = new StringBuilder();
        buf.append(a);
        buf.append(":");
        if (b != null)
            buf.append(b);
        return ByteUtil.getSHA1Digest(buf.toString().getBytes(), true);
    }

    public static boolean isAnyAdmin(AuthToken authToken) {
        return authToken.isAdmin() || authToken.isDomainAdmin() || authToken.isDelegatedAdmin();
    }

    /**
     * Returns an auth token that does not require a csrf token along with it for successful auth.
     *
     * This utility method is useful when a mailbox server wants to make a request to a server
     * running within the same system using the auth token inside an op/soap context.
     *
     * @param authToken
     * @return clone of an existing Auth token
     */
    public static AuthToken getCsrfUnsecuredAuthToken(AuthToken authToken)  {
        // Bug :96496
        if (authToken == null) {
            // this is an edge case where the user is changing password during first login and
            // the autToken is null
            return null;
        }

        if (authToken.isCsrfTokenEnabled()) {
            AuthToken token;
            try {
                token = (AuthToken) authToken.clone();
                token.setCsrfTokenEnabled(false);
                return token;
            } catch (CloneNotSupportedException e) {
                ZimbraLog.misc.debug("Error cloning auth token.", e);
                return null;
            }
        } else {
            return authToken;
        }
    }

    @Override
    public abstract String toString();

    public abstract String getAccountId() ;

    public abstract String getAdminAccountId();

    public abstract long getExpires();

    public int getValidityValue() {
        return -1;
    }

    public abstract void deRegister() throws AuthTokenException;

    public abstract boolean isRegistered();

    public abstract boolean isExpired();

    public abstract boolean isAdmin();

    public abstract boolean isDomainAdmin();

    public abstract boolean isDelegatedAdmin();

    public abstract boolean isZimbraUser();

    public abstract String getExternalUserEmail() ;

    public abstract String getDigest();

    public abstract String getCrumb() throws AuthTokenException;

    /**
     * Returns whether this represents an initial/anticipatory auth token issued to a ZMG app.
     * @return
     */
    public boolean isZMGAppBootstrap() {
        return false;
    }

    public boolean isCsrfTokenEnabled() {
        return false;
    }

    public void setCsrfTokenEnabled(boolean csrfEnabled) {
    }

    public boolean isDelegatedAuth() {
        return (getAdminAccountId() != null && !getAdminAccountId().equals(""));
    }

    public String getAccessKey() {
        return null;
    }

    public AuthMech getAuthMech() {
        return null;
    }

    public void setProxyAuthToken(String encoded) {}

    public String getProxyAuthToken() { return null; }

    public void resetProxyAuthToken() {}

    public Account getAccount() throws ServiceException {
        String acctId = getAccountId();
        Account acct = Provisioning.getInstance().get(Key.AccountBy.id, acctId);
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);

        return acct;
    }

    /**
     * Encode original auth info into an outgoing http request.
     *
     * @param client
     * @param method
     * @param isAdminReq
     * @param cookieDomain
     * @throws ServiceException
     */
    public abstract void encode(HttpClient client, HttpRequestBase method, boolean isAdminReq, String cookieDomain) throws ServiceException;

    /**
     * AP-TODO-4:
     *     This API is called only from ZimbraServlet.proxyServletRequest when the first hop is http basic auth(REST basic auth and DAV).
     *     For the next hop we encode the String auth token in cookie.  For all other cases, the original cookies are copied "as is"
     *     to the next hop.  See ZimbraServlet.proxyServletRequest(HttpServletRequest req, HttpServletResponse resp, HttpMethod method, BasicCookieStore state)
     *     We should clean this after AP-TODO-3 is resolved.
     *
     * Encode original auth info into an outgoing http request cookie.
     *
     * @param state
     * @param isAdminReq
     * @param cookieDomain
     * @throws ServiceException
     */
    public abstract void encode(BasicCookieStore state, boolean isAdminReq, String cookieDomain) throws ServiceException;

    /**
     * Encode original auth info into an HttpServletResponse
     *
     * @param resp
     * @param isAdminReq
     */
    public void encode(HttpServletResponse resp, boolean isAdminReq, boolean secureCookie)
    throws ServiceException {
        encode(resp, isAdminReq, secureCookie, false);
    }

    public abstract void encode(HttpServletResponse resp, boolean isAdminReq,
            boolean secureCookie, boolean rememberMe)
    throws ServiceException;

    /**
     *  It's overridden in ZimbraJWToken class where actions specific to jwt flow have been implemented.
     *  For other implementations of AuthToken, this method calls an already existing abstract method.
     */
    public void encode(HttpServletRequest reqst, HttpServletResponse resp, boolean isAdminReq, boolean secureCookie, boolean rememberMe)
    throws ServiceException {
        encode(resp, isAdminReq, secureCookie, rememberMe);
    }

    /**
     * This method is needed for ZimbraJWToken class which overrides this method.
     * At present, it has no significance for other implementations of AuthToken class.
     * If any implementation want to use this method, they can override it.
     * Can't keep this method abstract in AuthToken as that would force all existing implementations
     * of AuthToken to implement this method and it would break code of other people who have overridden
     * AuthToken class.
     */
    public void encode(HttpServletRequest reqst, HttpServletResponse resp, boolean deregister) throws ServiceException {
        //do nothing
    }

    /**
     *
     * @param clientBuilder
     * @param method
     * @param isAdminReq
     * @param cookieDomain
     * @throws ServiceException
     */
    public void encode(HttpClientBuilder clientBuilder, HttpRequestBase method, boolean isAdminReq, String cookieDomain) throws ServiceException{
        // EMpty method which can be implemented by  extending classes, avoiding changing existing signature
    }
    public abstract void encodeAuthResp(Element parent, boolean isAdmin) throws ServiceException;

    public abstract ZAuthToken toZAuthToken() throws ServiceException;

    // AP-TODO-5: REMOVE AFTER CLEANUP
    public abstract String getEncoded() throws AuthTokenException;

    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        return ZimbraAuthToken.getAuthToken(encoded);
    }

    // AP-TODO-5: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String acctId, String externalEmail, String pass, String digest, long expires) {
        return new ZimbraAuthToken(acctId, externalEmail, pass, digest, expires);
    }

    public static Map getInfo(String encoded) throws AuthTokenException {
        return ZimbraAuthToken.getInfo(encoded);
    }

    public abstract Usage getUsage();

    public static enum Usage {
        AUTH("a"), ENABLE_TWO_FACTOR_AUTH("etfa"), TWO_FACTOR_AUTH("tfa"), RESET_PASSWORD("rp");

        private String code;

        private Usage(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static Usage fromCode(String code) throws ServiceException {
            for (Usage usage: Usage.values()) {
                if (usage.code.equals(code)) {
                    return usage;
                }
            }
            throw ServiceException.FAILURE("unknown auth token usage value: " + code, null);
        }
    }

    public static enum TokenType {
        AUTH("auth"), JWT("jwt");
        private String code;

        private TokenType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static TokenType fromCode(String code) throws ServiceException {
             if ("jwt".equalsIgnoreCase(code)) {
                 return TokenType.JWT;
             } else {
                 return TokenType.AUTH;
             }
        }
    }
}
