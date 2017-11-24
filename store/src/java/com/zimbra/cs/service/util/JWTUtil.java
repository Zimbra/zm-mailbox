/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.service.util;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import com.google.common.primitives.Bytes;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthTokenKey;
import com.zimbra.cs.account.JWTCache;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraJWT;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

public class JWTUtil {

    /**
     * Generate JWT based on input parameters
     * @param jwtKey        final secret key used for signing jwt
     * @param salt          random key associated with this jwt
     * @param issuedAt      time at which jwt is issued
     * @param expires       time after which jwt expires
     * @param account       account for jwt is being generated
     * @return              jwt
     */
    public static final String generateJWT(byte[] jwtKey, String salt, long issuedAt, long expires, Account account) {
        Key key = new SecretKeySpec(jwtKey, SignatureAlgorithm.HS512.getJcaName());
        JwtBuilder builder = Jwts.builder();
        builder.setSubject(account.getId());
        builder.setIssuedAt(new Date(issuedAt));
        builder.setExpiration(new Date(expires));
        String jti = UUID.randomUUID().toString();
        builder.setId(jti);
        builder.claim(Constants.TOKEN_VALIDITY_VALUE_CLAIM, account.getAuthTokenValidityValue());
        JWTCache.put(jti, new ZimbraJWT(salt, expires));
        return builder.signWith(SignatureAlgorithm.HS512, key).compact();
    }

    /**
     * Look for the salt in request:soap context/soapEngine context/ZM_JWT cookie and return the value if found.
     * @param soapCtxt
     * @param engineCtxt
     * @return
     */
    public static final String getSalt(Element soapCtxt, Map engineCtxt) {
        String salt = null;
        salt = soapCtxt == null ? null : soapCtxt.getAttribute(HeaderConstants.E_JWT_SALT, null);
        if (salt == null && engineCtxt != null) {
            ZimbraLog.account.debug("salt not found in soap context");
            if (engineCtxt.get(SoapEngine.JWT_SALT) != null) {
                salt = (String) engineCtxt.get(SoapEngine.JWT_SALT);
            } else {
                ZimbraLog.account.debug("salt not found in soap context or engine context, looking in cookie");
                HttpServletRequest req = (HttpServletRequest) engineCtxt.get(SoapServlet.SERVLET_REQUEST);
                if (req != null) {
                    javax.servlet.http.Cookie cookies[] =  req.getCookies();
                    if (cookies != null) {
                        for (int i = 0; i < cookies.length; i++) {
                            if (cookies[i].getName().equals(Constants.ZM_JWT_COOKIE)) {
                                salt = cookies[i].getValue();
                                ZimbraLog.account.debug("salt found in zm_jwt cookie");
                                break;
                            }
                        }
                    }
                }
            }
        }
        return salt;
    }

    /**
     * validate the jwt and return claims if jwt is valid.
     * @param jwt
     * @param salts
     * @return
     * @throws ServiceException
     */
    public static Claims validateJWT(String jwt, String salts) throws ServiceException {
        if (StringUtil.isNullOrEmpty(jwt) || StringUtil.isNullOrEmpty(salts)) {
            ZimbraLog.account.debug("Invalid JWT or no salt value");
            throw AuthFailedServiceException.AUTH_FAILED("Invalid JWT or no salt value");
        }

        String salt = getJWTSalt(jwt, salts);
        if (salt == null) {
            ZimbraLog.account.debug("jwt specific salt not found");
            throw AuthFailedServiceException.AUTH_FAILED("no salt value");
        }

        byte[] finalKey = Bytes.concat(getTokenKey(), salt.getBytes());
        Key key = new SecretKeySpec(finalKey, SignatureAlgorithm.HS512.getJcaName());
        Claims claims = null;
        try {
            claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody();
            Account acct = Provisioning.getInstance().get(AccountBy.id, claims.getSubject());
            if (acct == null ) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(claims.getSubject());
            }
            int acctValidityValue = acct.getAuthTokenValidityValue();
            int tokenValidityValue = (Integer) claims.get(Constants.TOKEN_VALIDITY_VALUE_CLAIM);
            if (acctValidityValue != tokenValidityValue){
                ZimbraLog.account.debug("tokenValidityValue does not match");
                throw AuthFailedServiceException.AUTH_FAILED("Token not valid");
            }
        } catch(ExpiredJwtException eje) {
            ZimbraLog.account.debug("jwt expired", eje);
            throw ServiceException.AUTH_EXPIRED(eje.getMessage());
        } catch(SignatureException se) {
            ZimbraLog.account.debug("jwt signature exception", se);
            throw AuthFailedServiceException.AUTH_FAILED("Signature verification failed", se);
        } catch(UnsupportedJwtException uje) {
            ZimbraLog.account.debug("unsupported jwt exception", uje);
            throw AuthFailedServiceException.AUTH_FAILED("Unsupported JWT received", uje);
        } catch(MalformedJwtException mje) {
            ZimbraLog.account.debug("malformed jwt exception", mje);
            throw AuthFailedServiceException.AUTH_FAILED("Malformed JWT received", mje);
        } catch(Exception e) {
            ZimbraLog.account.debug("exception during jwt validation", e);
            throw AuthFailedServiceException.AUTH_FAILED("Exception thrown while validating JWT", e);
        }
        return claims;
    }

    /**
     * find salt which was used to sign this jwt
     * @param jwt
     * @param salts
     * @return
     * @throws ServiceException
     */
    private static String getJWTSalt(String jwt, String salts) throws ServiceException {
        String jwtSpecificSalt = null;
        String jti = getJTI(jwt);
        ZimbraLog.account.debug("jwt: %s, it's jti: %s, and salt values: %s", jwt, jti, salts);
        ZimbraJWT jwtInfo = JWTCache.get(jti);
        if (jwtInfo != null && salts.contains(jwtInfo.getSalt())) {
            jwtSpecificSalt = jwtInfo.getSalt();
        } else {
            String[] saltArr = salts.split("\\|");
            byte[] tokenKey = getTokenKey();
            for (String salt:saltArr) {
                byte[] finalKey = Bytes.concat(tokenKey, salt.getBytes());
                Key key = new SecretKeySpec(finalKey, SignatureAlgorithm.HS512.getJcaName());
                try {
                    Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody();
                    jwtSpecificSalt = salt;
                    JWTCache.put(jti, new ZimbraJWT(salt, claims.getExpiration().getTime()));
                    break;
                } catch(Exception e) {
                    //invalid salt, continue to try another salt value
                }
            }
        }
        return jwtSpecificSalt;
    }

    /**
     * extract the JTI (JWT unique identifier) from JWT without JWT validation
     * @param jwt
     * @return
     * @throws AuthFailedServiceException
     */
    public static String getJTI(String jwt) throws AuthFailedServiceException {
        String jti = null;
        if (!StringUtil.isNullOrEmpty(jwt)) {
            int i = jwt.lastIndexOf('.');
            String untrustedJwtString = jwt.substring(0, i+1);
            try {
                Claims claims = Jwts.parser().parseClaimsJwt(untrustedJwtString).getBody();
                jti = claims.getId();
            } catch (Exception ex) {
                throw AuthFailedServiceException.AUTH_FAILED("invalid jwt");
            }
        }
        return jti;
    }

    public static byte[] getTokenKey() throws ServiceException {
        AuthTokenKey authTokenKey = null;
        try {
            authTokenKey = AuthTokenKey.getCurrentKey();
        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to get latest AuthTokenKey", e);
            throw e;
        }
        return authTokenKey.getKey();
    }
}
