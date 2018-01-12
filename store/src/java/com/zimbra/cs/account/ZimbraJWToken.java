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
package com.zimbra.cs.account;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.text.RandomStringGenerator;

import com.google.common.primitives.Bytes;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.service.util.JWTUtil;

import io.jsonwebtoken.Claims;

public class ZimbraJWToken extends ZimbraAuthToken {
    private static final int  SALT_LENGTH = 20;
    private TokenType tokenType;
    private String salt = null;
    private String jwtId = null;
    private long issuedAt;

    @Override
    public TokenType getTokenType() {
        return tokenType;
    }

    @Override
    public boolean isJWT() {
        return TokenType.JWT.equals(this.tokenType);
    }

    @Override
    public String getSalt() {
        return salt;
    }

    @Override
    public String getTokenId() {
        return jwtId;
    }

    public ZimbraJWToken(Account acct) {
        this(acct, 0, null, null);
    }

    public ZimbraJWToken(Account acct, long expires) {
        this(acct, expires, false, null, null, Usage.AUTH, null, null);
    }

    public ZimbraJWToken(Account acct, Usage usage) {
        this(acct, 0, false, null, null, usage, null, null);
    }

    public ZimbraJWToken(Account acct, long expires, String tokenId, String jwt) {
        this(acct, expires, false, null, null, Usage.AUTH, tokenId, jwt);
    }

    public ZimbraJWToken(Account acct, long expires, boolean isAdmin, Account adminAcct,
            AuthMech authMech, Usage usage, String tokenId, String jwt) {
        this.tokenType = TokenType.JWT;
        long expireTime = expires;
        if (!StringUtil.isNullOrEmpty(tokenId)) {
            jwtId = tokenId;
        }
        if(expireTime == 0) {
            long lifetime = acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000);
            issuedAt = System.currentTimeMillis();
            expireTime = issuedAt + lifetime;
        }
        initAuthToken(acct, isAdmin, adminAcct, expireTime, authMech, usage);
        this.encoded = jwt;
    }

    @Override
    public void deRegister() throws AuthTokenException {
        try {
            Account acct = Provisioning.getInstance().getAccountById(accountId);
            if(acct != null) {
                acct.cleanExpiredJWTokens();
                Expiration expiration = new AbsoluteExpiration(this.expires);
                acct.addInvalidJWTokens(jwtId, server_version, expiration);
                ZimbraLog.account.debug("added jti: %s to invalid list", jwtId);
                if(acct.getBooleanAttr(Provisioning.A_zimbraLogOutFromAllServers, false)) {
                    AuthTokenRegistry.addTokenToQueue(this);
                }
            }
        } catch (ServiceException e) {
            throw new AuthTokenException("unable to de-register auth token", e);
        }
    }

    @Override
    public String getEncoded() throws AuthTokenException {
        if (encoded == null) {
            try {
                Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
                ZimbraLog.account.debug("auth: generating jwt token for account id: %s", accountId);
                UniformRandomProvider rng = RandomSource.create(RandomSource.MWC_256);
                RandomStringGenerator generator = new RandomStringGenerator.Builder().withinRange('a', 'z').usingRandom(rng::nextInt).build();
                salt = generator.generate(SALT_LENGTH);
                byte[] finalKey = Bytes.concat(getCurrentKey().getKey(), salt.getBytes());
                encoded = JWTUtil.generateJWT(finalKey, salt, issuedAt, expires, acct);
            } catch (ServiceException e) {
                throw new AuthTokenException("unable to generate jwt", e);
            }
        }
        return encoded;
    }

    public static AuthToken getAuthToken(String jwt, String salt) throws AuthTokenException {
        AuthToken at = null;
        try {
            Claims body = JWTUtil.validateJWT(jwt, salt);
            Account acct = Provisioning.getInstance().getAccountById(body.getSubject());
            if (acct == null ) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(body.getSubject());
            }
            at = new ZimbraJWToken(acct, body.getExpiration().getTime(), body.getId(), jwt);
            ZimbraLog.account.debug("issued authtoken based on jti %s", body.getId());
        } catch (ServiceException exception) {
            throw new AuthTokenException("JWT validation failed", exception);
        }
        return at;
    }
}
