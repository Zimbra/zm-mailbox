/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.google.common.primitives.Bytes;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.auth.ZJWToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.service.util.JWTUtil;

import io.jsonwebtoken.Claims;

public class ZimbraJWToken extends AuthToken {
    private static final int  SALT_LENGTH = 20;
    private long issuedAt;
    private String salt;
    private AuthTokenProperties properties;

    public ZimbraJWToken(Account acct) {
        this(acct, 0);
    }

    public ZimbraJWToken(Account acct, long expires) {
        this(acct, expires, false, null, null, Usage.AUTH);
    }

    public ZimbraJWToken(Account acct, Usage usage) {
        this(acct, 0, false, null, null, usage);
    }

    ZimbraJWToken(Account acct, long expires, boolean isAdmin, Account adminAcct,
            AuthMech authMech, Usage usage) {
        long expireTime = expires;
        if(expireTime == 0) {
            long lifetime = acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000);
            issuedAt = System.currentTimeMillis();
            expireTime = issuedAt + lifetime;
        }
        properties = new AuthTokenProperties(acct, isAdmin, adminAcct, expireTime, authMech, usage);
    }

    ZimbraJWToken(Claims body, String jwt) throws AuthTokenException {
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(AuthTokenProperties.C_ID, body.getSubject());
        if (body.getExpiration() != null) {
            map.put(AuthTokenProperties.C_EXP, String.valueOf(body.getExpiration().getTime()));
        }
        if ((boolean) body.get(AuthTokenProperties.C_ADMIN)) {
            map.put(AuthTokenProperties.C_ADMIN, "1");
        }
        if ((boolean) body.get(AuthTokenProperties.C_DOMAIN)) {
            map.put(AuthTokenProperties.C_DOMAIN, "1");
        }
        if ((boolean) body.get(AuthTokenProperties.C_DLGADMIN)) {
            map.put(AuthTokenProperties.C_DLGADMIN, "1");
        }
        if ((boolean) body.get(AuthTokenProperties.C_CSRF)) {
            map.put(AuthTokenProperties.C_CSRF, "1");
        }
        populateMap(map, body, AuthTokenProperties.C_AID);
        populateMap(map, body, AuthTokenProperties.C_AUTH_MECH);
        populateMap(map, body, AuthTokenProperties.C_USAGE);
        populateMap(map, body, AuthTokenProperties.C_EXTERNAL_USER_EMAIL);
        populateMap(map, body, AuthTokenProperties.C_VALIDITY_VALUE);
        populateMap(map, body, AuthTokenProperties.C_SERVER_VERSION);
        properties = new AuthTokenProperties(map);
        properties.setEncoded(jwt);
    }

    private void populateMap(Map<Object, Object> map, Claims body, String key) {
        map.put(key, body.get(key));
    }

    public static AuthToken getJWToken(String jwt, String currentSalt) throws AuthTokenException {
        AuthToken at = null;
        try {
            Claims body = JWTUtil.validateJWT(jwt, currentSalt);
            at = new ZimbraJWToken(body, jwt);
        } catch (ServiceException exception) {
            throw new AuthTokenException("JWT validation failed", exception);
        }
        return at;
    }

    @Override
    public void deRegister() throws AuthTokenException {
        if (!isExpired()) {
               try {
                   Account acct = Provisioning.getInstance().getAccountById(properties.getAccountId());
                   if(acct != null) {
                       acct.cleanExpiredJWTokens();
                       String jwtId = JWTUtil.getJTI(properties.getEncoded());
                       if (jwtId != null) {
                           Expiration expiration = new AbsoluteExpiration(properties.getExpires());
                           acct.addInvalidJWTokens(jwtId, properties.getServerVersion(), expiration);
                           JWTCache.remove(jwtId);
                           ZimbraLog.account.debug("added jti: %s to invalid list", jwtId);
                           if(acct.getBooleanAttr(Provisioning.A_zimbraLogOutFromAllServers, false)) {
                               AuthTokenRegistry.addTokenToQueue(this);
                           }
                       }
                   }
               } catch (ServiceException e) {
                   throw new AuthTokenException("unable to de-register auth token", e);
               }
           }
    }

    @Override
    public String getEncoded() throws AuthTokenException {
        if (properties.getEncoded() == null) {
            try {
                ZimbraLog.account.debug("auth: generating jwt token for account id: %s", properties.getAccountId());
                UniformRandomProvider rng = RandomSource.create(RandomSource.MWC_256);
                RandomStringGenerator generator = new RandomStringGenerator.Builder().withinRange('a', 'z').usingRandom(rng::nextInt).build();
                salt = generator.generate(SALT_LENGTH);
                AuthTokenKey key = AuthTokenUtil.getCurrentKey();
                byte[] finalKey = Bytes.concat(key.getKey(), salt.getBytes());
                String jwt = JWTUtil.generateJWT(finalKey, salt, issuedAt, properties, key.getVersion());
                properties.setEncoded(jwt);
            } catch (ServiceException e) {
                throw new AuthTokenException("unable to generate jwt", e);
            }
        }
        return properties.getEncoded();
    }

    @Override
    public String toString() {
        return properties.getEncoded();
    }

    @Override
    public String getAccountId() {
        return properties.getAccountId();
    }

    @Override
    public String getAdminAccountId() {
        return properties.getAdminAccountId();
    }

    @Override
    public long getExpires() {
        return properties.getExpires();
    }

    @Override
    public boolean isRegistered() {
        return true;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() > properties.getExpires();
    }

    @Override
    public boolean isAdmin() {
        return properties.isAdmin();
    }

    @Override
    public boolean isDomainAdmin() {
        return properties.isDomainAdmin();
    }

    @Override
    public boolean isDelegatedAdmin() {
        return properties.isDelegatedAdmin();
    }

    @Override
    public boolean isZimbraUser() {
        return AuthTokenUtil.isZimbraUser(properties.getType());
    }

    @Override
    public String getExternalUserEmail() {
        return properties.getExternalUserEmail();
    }

    @Override
    public String getDigest() {
        return properties.getDigest();
    }

    @Override
    public String getCrumb() throws AuthTokenException {
        return properties.getEncoded();
    }

    @Override
    public int getValidityValue() {
        return properties.getValidityValue();
    }


    @Override
    public void encode(HttpClient client, HttpRequestBase method, boolean isAdminReq, String cookieDomain)
            throws ServiceException {
        String jwt = properties.getEncoded();
        method.addHeader(Constants.AUTH_HEADER, Constants.BEARER + " " + jwt);
        String jwtSalt = JWTUtil.getJWTSalt(jwt);
        BasicCookieStore state = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie( ZimbraCookie.COOKIE_ZM_JWT, jwtSalt);
        cookie.setDomain(cookieDomain);
        cookie.setPath("/");
        cookie.setSecure(false);
        state.addCookie(cookie);
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        clientBuilder.setDefaultCookieStore(state);

        RequestConfig reqConfig = RequestConfig.copy(
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
            .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

        clientBuilder.setDefaultRequestConfig(reqConfig);
    }

    @Override
    public void encode(BasicCookieStore state, boolean isAdminReq, String cookieDomain) throws ServiceException {
        BasicClientCookie cookie = new BasicClientCookie(ZimbraCookie.COOKIE_ZM_JWT, JWTUtil.getJWTSalt(properties.getEncoded()));
        cookie.setDomain(cookieDomain);
        cookie.setPath("/");
        cookie.setSecure(false);
        state.addCookie(cookie);
    }

    @Override
    public void encode(HttpServletRequest reqst, HttpServletResponse resp, boolean isAdminReq, boolean secureCookie, boolean rememberMe)
    throws ServiceException {
        encode(reqst, resp, false);
    }

    @Override
    public void encode(HttpServletRequest reqst, HttpServletResponse resp, boolean deregister)
    throws ServiceException {
        if (reqst != null && resp != null) {
            String finalValue = null;
            String zmJwtCookieVal = JWTUtil.getZMJWTCookieValue(reqst);
            if (deregister) {
                String jwtId = JWTUtil.getJTI(properties.getEncoded());
                JWTInfo jwt = JWTCache.get(jwtId);
                if (jwt != null) {
                    finalValue = JWTUtil.clearSalt(zmJwtCookieVal, jwt.getSalt());
                    ZimbraLog.security.debug("EndSession: found salt in cache for jti: %s",jwtId);
                }
            } else {
                finalValue = StringUtil.isNullOrEmpty(zmJwtCookieVal) ? salt : salt + Constants.JWT_SALT_SEPARATOR + zmJwtCookieVal;
                if (!StringUtil.isNullOrEmpty(finalValue)) {
                    int cookieLength = finalValue.length() + ZimbraCookie.COOKIE_ZM_JWT.length();
                    if (cookieLength > LC.zimbra_jwt_cookie_size_limit.intValue()) {
                        ZimbraLog.security.debug("JWT Cookie size limit exceeded, limit: %d", LC.zimbra_jwt_cookie_size_limit.intValue());
                        throw AuthFailedServiceException.AUTH_FAILED("JWT Cookie size limit exceeded");
                    }
                }
            }
            ZimbraCookie.addHttpOnlyCookie(resp, ZimbraCookie.COOKIE_ZM_JWT, finalValue, ZimbraCookie.PATH_ROOT, -1, true, this.isIgnoreSameSite());
        }
    }

    @Override
    public void encode(HttpServletResponse resp, boolean isAdminReq, boolean secureCookie, boolean rememberMe)
            throws ServiceException {
        encode(null, resp, false);
    }

    @Override
    public void encodeAuthResp(Element parent, boolean isAdmin) throws ServiceException {
        AuthTokenUtil.encodeAuthResp(this, parent, isAdmin);
    }

    @Override
    public ZAuthToken toZAuthToken() throws ServiceException {
        String jwt = AuthTokenUtil.getOrigAuthData(this);
        return new ZJWToken(jwt, JWTUtil.getJWTSalt(jwt));
    }

    @Override
    public Usage getUsage() {
        return properties.getUsage();
    }

    @Override
    public AuthMech getAuthMech() {
        return properties.getAuthMech();
    }

    @Override
    public String getAccessKey() {
        return properties.getAccessKey();
    }

    @Override
    public void setProxyAuthToken(String encoded) {
        properties.setProxyAuthToken(encoded);
    }

    @Override
    public String getProxyAuthToken() {
        return properties.getProxyAuthToken();
    }

    @Override
    public void resetProxyAuthToken() {
        properties.setProxyAuthToken(null);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.AuthToken#encode(org.apache.http.impl.client.HttpClientBuilder, boolean, java.lang.String)
     */
    @Override
    public void encode(HttpClientBuilder clientBuilder, HttpRequestBase method, boolean isAdminReq, String cookieDomain)
        throws ServiceException {
        String jwt = properties.getEncoded();
        method.addHeader(Constants.AUTH_HEADER, Constants.BEARER + " " + jwt);
        String jwtSalt = JWTUtil.getJWTSalt(jwt);
        BasicCookieStore state = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie( ZimbraCookie.COOKIE_ZM_JWT, jwtSalt);
        cookie.setDomain(cookieDomain);
        cookie.setPath("/");
        cookie.setSecure(false);
        state.addCookie(cookie);
        clientBuilder.setDefaultCookieStore(state);

        RequestConfig reqConfig = RequestConfig.copy(
            ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
            .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

        clientBuilder.setDefaultRequestConfig(reqConfig);

    }
}
