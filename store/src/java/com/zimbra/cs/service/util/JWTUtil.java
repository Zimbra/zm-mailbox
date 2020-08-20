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

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.google.common.primitives.Bytes;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenKey;
import com.zimbra.cs.account.AuthTokenProperties;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraJWToken;
import com.zimbra.cs.mailbox.cache.JWTInfo;
import com.zimbra.cs.mailbox.cache.RedisJwtCache;
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

    private static final Cache <String, Claims> CLAIMS_CACHE = CacheBuilder.newBuilder()
            .maximumSize(LC.zimbra_authtoken_cache_size.intValue())
            .build();

    /**
     * Generate JWT based on input parameters
     * @param jwtKey        final secret key used for signing jwt
     * @param salt          random key associated with this jwt
     * @param issuedAt      time at which jwt is issued
     * @param expires       time after which jwt expires
     * @param account       account for jwt is being generated
     * @return              jwt
     * @throws ServiceException
     */
    public static final String generateJWT(byte[] jwtKey, String salt, long issuedAt, AuthTokenProperties properties, long keyVersion) throws ServiceException {
        if (properties != null) {
            Key key = new SecretKeySpec(jwtKey, SignatureAlgorithm.HS512.getJcaName());
            JwtBuilder builder = Jwts.builder();
            builder.setSubject(properties.getAccountId());
            builder.setIssuedAt(new Date(issuedAt));
            builder.setExpiration(new Date(properties.getExpires()));
            String jti = UUID.randomUUID().toString();
            builder.setId(jti);
            builder.claim(AuthTokenProperties.C_AID, properties.getAdminAccountId());
            builder.claim(AuthTokenProperties.C_ADMIN, properties.isAdmin());
            builder.claim(AuthTokenProperties.C_DOMAIN, properties.isDomainAdmin());
            builder.claim(AuthTokenProperties.C_DLGADMIN, properties.isDelegatedAdmin());
            builder.claim(AuthTokenProperties.C_TYPE, properties.getType());
            builder.claim(AuthTokenProperties.C_DIGEST, properties.getDigest());
            builder.claim(AuthTokenProperties.C_SERVER_VERSION, properties.getServerVersion());
            builder.claim(AuthTokenProperties.C_CSRF, properties.isCsrfTokenEnabled());
            builder.claim(AuthTokenProperties.C_KEY_VERSION, keyVersion);
            if (properties.getValidityValue() != -1) {
                builder.claim(AuthTokenProperties.C_VALIDITY_VALUE, String.valueOf(properties.getValidityValue()));
            }
            if (properties.getAuthMech() != null) {
                builder.claim(AuthTokenProperties.C_AUTH_MECH, properties.getAuthMech().name());
            }
            if (properties.getUsage() != null) {
                builder.claim(AuthTokenProperties.C_USAGE, properties.getUsage().getCode());
            }
            RedisJwtCache.put(jti, new JWTInfo(salt, properties.getExpires()));
            return builder.signWith(SignatureAlgorithm.HS512, key).compact();
        } else {
            throw AuthFailedServiceException.AUTH_FAILED("properties is required");
        }
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
                            if (ZimbraCookie.COOKIE_ZM_JWT.equals(cookies[i].getName())) {
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
        String jti = getJTI(jwt);
        String salt = getJWTSalt(jwt, jti, salts);
        if (salt == null) {
            ZimbraLog.account.debug("jwt specific salt not found");
            throw AuthFailedServiceException.AUTH_FAILED("no salt value");
        }
        byte[] finalKey = Bytes.concat(getOriginalKey(jwt), salt.getBytes());
        Key key = new SecretKeySpec(finalKey, SignatureAlgorithm.HS512.getJcaName());
        Claims claims = null;
        try {
            claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody();
            Account acct = Provisioning.getInstance().get(AccountBy.id, claims.getSubject());
            if (acct == null ) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(claims.getSubject());
            }
            if (acct.hasInvalidJWTokens(jti)) {
                ZimbraLog.security.debug("jwt: %s is no longer valid, has been invalidated on logout", jti);
                throw AuthFailedServiceException.AUTH_FAILED("Token has been invalidated");
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

    private static byte[] getOriginalKey(String jwt) throws ServiceException {
        AuthTokenKey atkey = AuthTokenKey.getVersion(getKeyVersion(jwt));
        if (atkey == null) {
            throw AuthFailedServiceException.AUTH_FAILED("unknown key version");
        }
        return atkey.getKey();
    }
    /**
     * find salt which was used to sign this jwt
     * @param jwt
     * @param salts
     * @return
     * @throws ServiceException
     */
    private static String getJWTSalt(String jwt, String jti, String salts) throws ServiceException {
        String jwtSpecificSalt = null;
        JWTInfo jwtInfo = RedisJwtCache.get(jti);
        if (jwtInfo != null && salts.contains(jwtInfo.getSalt())) {
            jwtSpecificSalt = jwtInfo.getSalt();
        } else {
            String[] saltArr = salts.split("\\|");
            byte[] tokenKey = getOriginalKey(jwt);
            for (String salt:saltArr) {
                if (!StringUtil.isNullOrEmpty(salt)) {
                    byte[] finalKey = Bytes.concat(tokenKey, salt.getBytes());
                    Key key = new SecretKeySpec(finalKey, SignatureAlgorithm.HS512.getJcaName());
                    try {
                        Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody();
                        jwtSpecificSalt = salt;
                        RedisJwtCache.put(jti, new JWTInfo(salt, claims.getExpiration().getTime()));
                        break;
                    } catch(Exception e) {
                        //invalid salt, continue to try another salt value
                        ZimbraLog.account.debug("invalid salt, continue to try another salt value", e);
                    }
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
    public static String getJTI(String jwt) throws ServiceException {
        String jti = null;
        Claims claims = getClaims(jwt);
        if (claims != null) {
            jti = claims.getId();
        }
        return jti;
    }

    private static Claims getClaims(String jwt) throws ServiceException {
        Claims claims = null;
        if (!StringUtil.isNullOrEmpty(jwt)) {
            claims = CLAIMS_CACHE.getIfPresent(jwt);
            if (claims == null) {
                int i = jwt.lastIndexOf('.');
                String untrustedJwtString = jwt.substring(0, i+1);
                try {
                    claims = Jwts.parser().parseClaimsJwt(untrustedJwtString).getBody();
                    CLAIMS_CACHE.put(jwt, claims);
                } catch(ExpiredJwtException eje) {
                    ZimbraLog.account.debug("jwt expired", eje);
                    throw ServiceException.AUTH_EXPIRED(eje.getMessage());
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
            }
        }
        return claims;
    }

    /**
     * extract the key version from JWT without JWT validation
     * @param jwt
     * @return
     * @throws ServiceException
     */
    public static String getKeyVersion(String jwt) throws ServiceException {
        String version = null;
        Claims claims = getClaims(jwt);
        if (claims != null) {
            version = String.valueOf(claims.get(AuthTokenProperties.C_KEY_VERSION));
        }
        return version;
    }

    /**
     * Removes the salt provided in input from the ZM_JWT cookie value
     * @param zmJWTCookieValue
     * @param saltTobeCleared
     * @return
     */
    public static String clearSalt(String zmJWTCookieValue, String salt) {
        String updatedCookieValue = zmJWTCookieValue;
        String match1  = Constants.JWT_SALT_SEPARATOR + salt + Constants.JWT_SALT_SEPARATOR;
        String match2 = Constants.JWT_SALT_SEPARATOR + salt;
        String match3 = salt + Constants.JWT_SALT_SEPARATOR;
        if (!StringUtil.isNullOrEmpty(updatedCookieValue)) {
            if (updatedCookieValue.contains(match1)) {
                updatedCookieValue = updatedCookieValue.replace(match1, Constants.JWT_SALT_SEPARATOR);
            } else if (updatedCookieValue.contains(match2)) {
                updatedCookieValue = updatedCookieValue.replace(match2, "");
            } else if (updatedCookieValue.contains(match3)) {
                updatedCookieValue = updatedCookieValue.replace(match3, "");
            } else {
                updatedCookieValue = updatedCookieValue.replace(salt, "");
            }
        }
        return updatedCookieValue;
    }

    /**
     * returns the ZM_JWT cookie value if present in http request cookies
     * @param httpReq
     * @return
     */
    public static String getZMJWTCookieValue(HttpServletRequest httpReq) {
        String cookieVal = null;
        if (httpReq != null) {
            Cookie cookies[] =  httpReq.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    if (cookies[i].getName().equals(ZimbraCookie.COOKIE_ZM_JWT)) {
                        cookieVal = cookies[i].getValue();
                        break;
                    }
                }
            }
        }
        return cookieVal;
    }

    public static boolean isJWT(AuthToken token) {
        return token instanceof ZimbraJWToken ? true : false;
    }

    /**
     * get the salt corresponding to the jwt
     * @param jwt
     * @return
     * @throws ServiceException
     */
    public static String getJWTSalt(String jwt) throws ServiceException {
        String jti = getJTI(jwt);
        JWTInfo jwtInfo = RedisJwtCache.get(jti);
        return jwtInfo != null ? jwtInfo.getSalt() : null;
    }

    public static String getJwtSecretFromPath(String secretPath) throws IOException {
        File file = new File(secretPath);
        return Files.asCharSource(file, Charsets.UTF_8).read();
    }
}
