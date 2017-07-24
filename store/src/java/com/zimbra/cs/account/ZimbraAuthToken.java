/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import com.google.common.base.Objects;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralInput.AbsoluteExpiration;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.LdapEntryLocation;

/**
 * @since May 30, 2004
 * @author schemers
 */
public class ZimbraAuthToken extends AuthToken implements Cloneable {
    private static final String C_ID  = "id";
    // original admin id
    private static final String C_AID  = "aid";
    private static final String C_EXP = "exp";
    private static final String C_ADMIN = "admin";
    private static final String C_DOMAIN = "domain";
    private static final String C_DLGADMIN = "dlgadmin";
    private static final String C_TYPE = "type";
    private static final String C_TYPE_ZIMBRA_USER = "zimbra";
    private static final String C_TYPE_EXTERNAL_USER = "external";
    private static final String C_TYPE_ZMG_APP = "zmgapp";
    private static final String C_EXTERNAL_USER_EMAIL = "email";
    private static final String C_DIGEST = "digest";
    private static final String C_VALIDITY_VALUE  = "vv";
    private static final String C_AUTH_MECH = "am";
    private static final String C_USAGE = "u";
    //cookie ID for keeping track of account's cookies
    private static final String C_TOKEN_ID = "tid";
    //mailbox server version where this account resides
    private static final String C_SERVER_VERSION = "version";
    private static final String C_CSRF = "csrf";
    private static final Map<String, ZimbraAuthToken> CACHE = MapUtil.newLruMap(LC.zimbra_authtoken_cache_size.intValue());
    private static final Log LOG = LogFactory.getLog(AuthToken.class);

    private String accountId;
    private String adminAccountId;
    private int validityValue = -1;
    private long expires;
    private String encoded;
    private boolean isAdmin;
    private boolean isDomainAdmin;
    private boolean isDelegatedAdmin;
    private String type;
    private String externalUserEmail;
    private String digest;
    private String accessKey; // just a dummy placeholder for now until accesskey auth is implemented in ZimbraAuthToken
    private String proxyAuthToken;
    private AuthMech authMech;
    private Integer tokenID = -1;
    private String server_version;   // version of the mailbox server where this account resides
    private boolean csrfTokenEnabled;
    private Usage usage; // what this token will be used for

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("acct", accountId)
            .add("admin", adminAccountId)
            .add("exp", expires)
            .add("isAdm", isAdmin)
            .add("isDlgAd", isDelegatedAdmin)
            .toString();
    }

    protected static AuthTokenKey getCurrentKey() throws AuthTokenException {
        try {
            AuthTokenKey key = AuthTokenKey.getCurrentKey();
            return key;
        } catch (ServiceException e) {
            LOG.fatal("unable to get latest AuthTokenKey", e);
            throw new AuthTokenException("unable to get AuthTokenKey", e);
        }
    }

    /**
     * Return an AuthToken object using an encoded authtoken. Caller should call isExpired on returned
     * authToken before using it.
     */
    public synchronized static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        ZimbraAuthToken at = CACHE.get(encoded);
        if (at == null) {
            at = new ZimbraAuthToken(encoded);
            if (!at.isExpired()) {
                CACHE.put(encoded, at);
            }
        } else {
            // remove it if expired
            if (at.isExpired()) {
                CACHE.remove(encoded);
            }
        }
        return at;
    }

    protected ZimbraAuthToken() {
    }

    public static Map<?, ?> getInfo(String encoded) throws AuthTokenException {
        String[] parts = encoded.split("_");
        if (parts.length != 3) {
            throw new AuthTokenException("invalid authtoken format");
        }
        return TokenUtil.getAttrs(parts[2]);
    }

    protected ZimbraAuthToken(String encoded) throws AuthTokenException {
        try {
            this.encoded = encoded;
            int pos = encoded.indexOf('_');
            if (pos == -1) {
                throw new AuthTokenException("invalid authtoken format");
            }
            String ver = encoded.substring(0, pos);

            int pos2 = encoded.indexOf('_', pos+1);
            if (pos2 == -1) {
                throw new AuthTokenException("invalid authtoken format");
            }
            String hmac = encoded.substring(pos+1, pos2);
            String data = encoded.substring(pos2+1);

            AuthTokenKey key = AuthTokenKey.getVersion(ver);
            if (key == null) {
                throw new AuthTokenException("unknown key version");
            }
            String computedHmac = TokenUtil.getHmac(data, key.getKey());
            if (!computedHmac.equals(hmac)) {
                throw new AuthTokenException("hmac failure");
            }
            Map<?, ?> map = TokenUtil.getAttrs(data);

            accountId = (String) map.get(C_ID);
            adminAccountId = (String) map.get(C_AID);
            expires = Long.parseLong((String) map.get(C_EXP));
            String ia = (String) map.get(C_ADMIN);
            isAdmin = "1".equals(ia);
            String da = (String) map.get(C_DOMAIN);
            isDomainAdmin = "1".equals(da);
            String dlga = (String) map.get(C_DLGADMIN);
            isDelegatedAdmin = "1".equals(dlga);
            type = (String)map.get(C_TYPE);

            String authMechStr = (String)map.get(C_AUTH_MECH);
            authMech = AuthMech.fromString(authMechStr);
            String usageCode = (String)map.get(C_USAGE);
            if (usageCode != null) {
                usage = Usage.fromCode(usageCode);
            } else {
                usage = Usage.AUTH;
            }
            externalUserEmail = (String)map.get(C_EXTERNAL_USER_EMAIL);
            digest = (String)map.get(C_DIGEST);
            String vv = (String)map.get(C_VALIDITY_VALUE);
            if (vv != null) {
                try {
                    validityValue = Integer.parseInt(vv);
                } catch (NumberFormatException e) {
                    validityValue = -1;
                }
            } else {
                validityValue = -1;
            }

            String tid = (String)map.get(C_TOKEN_ID);
            if(tid !=null) {
            	try {
            		tokenID = Integer.parseInt(tid);
            	} catch (NumberFormatException e) {
            		tokenID = -1;
                }
            } else {
            	tokenID = -1;
            }
            server_version = (String)map.get(C_SERVER_VERSION);

            String csrf = (String) map.get(C_CSRF);
            if (csrf != null) {
                csrfTokenEnabled = "1".equals(map.get(C_CSRF));
            }
        } catch (ServiceException e) {
            throw new AuthTokenException("service exception", e);
        }
    }

    public ZimbraAuthToken(Account acct) {
        this(acct, false, null);
    }

    public ZimbraAuthToken(Account acct, Usage usage) {
        this(acct, 0, false, null, null, usage);
    }

    public ZimbraAuthToken(Account acct, boolean isAdmin, AuthMech authMech) {
        this(acct, 0, isAdmin, null, authMech);
    }

    public ZimbraAuthToken(Account acct, long expires) {
        this(acct, expires, false, null, null);
    }

    public ZimbraAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct,
            AuthMech authMech) {
        this(acct, expires, isAdmin, adminAcct, authMech, Usage.AUTH);
    }

    /**
     * @param acct account authtoken will be valid for
     * @param expires when the token expires
     * @param isAdmin true if acct is an admin account
     * @param adminAcct the admin account accessing acct's information, if this token was created by an admin. mainly used
     *        for auditing.
     * @throws AuthTokenException
     */
    public ZimbraAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct,
            AuthMech authMech, Usage usage) {
        if(expires == 0) {
            long lifetime;
            switch (usage) {
            case ENABLE_TWO_FACTOR_AUTH:
                lifetime = acct.getTimeInterval(Provisioning.A_zimbraTwoFactorAuthTokenLifetime, DEFAULT_TWO_FACTOR_AUTH_LIFETIME * 1000);
                break;
            case TWO_FACTOR_AUTH:
                lifetime = acct.getTimeInterval(Provisioning.A_zimbraTwoFactorAuthEnablementTokenLifetime, DEFAULT_TWO_FACTOR_ENABLEMENT_AUTH_LIFETIME * 1000);
                break;
            case AUTH:
            default:
                lifetime = isAdmin || isDomainAdmin || isDelegatedAdmin ?
                        acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000) :
                        acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000);
                break;
            }
            expires = System.currentTimeMillis() + lifetime;
        }
        accountId = acct.getId();
        adminAccountId = adminAcct != null ? adminAcct.getId() : null;
        validityValue = acct.getAuthTokenValidityValue();
        this.expires = expires;
        this.isAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsAdminAccount));
        isDomainAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDomainAdminAccount));
        isDelegatedAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDelegatedAdminAccount));
        this.authMech = authMech;
        this.usage = usage;
        encoded = null;
        if (acct instanceof GuestAccount) {
            type = C_TYPE_EXTERNAL_USER;
            GuestAccount g = (GuestAccount) acct;
            digest = g.getDigest();
            accessKey = g.getAccessKey();
            externalUserEmail = g.getName();
        } else {
            type = C_TYPE_ZIMBRA_USER;
        }
        tokenID = new Random().nextInt(Integer.MAX_VALUE-1) + 1;
        try {
            Server server = acct.getServer();
            if (server != null) {
                server_version = server.getServerVersion();
            } else {
                server_version = Provisioning.getInstance().getLocalServer().getServerVersion();
            }
        } catch (ServiceException e) {
            LOG.error("Unable to fetch server version for the user account", e);
        }
        register();
    }

    public ZimbraAuthToken(String acctId, String externalEmail, String pass, String digest, long expires)  {
        this(acctId, false, externalEmail, pass, digest, expires);
    }

    public ZimbraAuthToken(String acctId, boolean zmgApp, String externalEmail, String pass, String digest, long expires)  {
        accountId = acctId;
        this.expires = expires;
        externalUserEmail = externalEmail == null && !zmgApp ? "public" : externalEmail;
        this.digest = digest != null ? digest : generateDigest(externalEmail, pass);
        this.type = zmgApp ? C_TYPE_ZMG_APP : C_TYPE_EXTERNAL_USER;
        tokenID = new Random().nextInt(Integer.MAX_VALUE-1) + 1;
        try {
            Account acct = Provisioning.getInstance().getAccountById(accountId);
            if (acct != null) {
                Server server = acct.getServer();
                if (server != null) {
                    server_version = server.getAttr(Provisioning.A_zimbraServerVersion, "");
                }
        	}
        } catch (ServiceException e) {
            LOG.error("Unable to fetch server version for the user account", e);
        }
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getAdminAccountId() {
        return adminAccountId;
    }

    @Override
    public long getExpires() {
        return expires;
    }

    @Override
    public int getValidityValue() {
        return validityValue;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() > expires;
    }

    @Override
    public boolean isAdmin() {
        return isAdmin;
    }

    @Override
    public boolean isDomainAdmin() {
        return isDomainAdmin;
    }

    @Override
    public boolean isDelegatedAdmin() {
        return isDelegatedAdmin;
    }

    @Override
    public boolean isZimbraUser() {
        return type == null || C_TYPE_ZIMBRA_USER.equals(type) || C_TYPE_ZMG_APP.equals(type);
        // C_TYPE_ZMG_APP type indicates the bootstrap auth token issued for ZMG app. Technically
        // that too represents a Zimbra account/user
    }

    @Override
    public String getExternalUserEmail() {
        return externalUserEmail;
    }

    @Override
    public String getDigest() {
        return digest;
    }

    @Override
    public String getAccessKey() {
        return accessKey;
    }

    @Override
    public AuthMech getAuthMech() {
        return authMech;
    }


    private void register() {
        if (!isZimbraUser() || isZMGAppBootstrap()) {
            return;
        }
        try {
            Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
            if (Provisioning.getInstance().getLocalServer().getLowestSupportedAuthVersion() > 1) {
                try {
                    acct.cleanExpiredTokens(); //house keeping. If we are issuing a new token, clean up old ones.
                } catch (ServiceException e) {
                    LOG.error("unable to de-register auth token", e);
                }
                Expiration expiration = new AbsoluteExpiration(this.expires);
                acct.addAuthTokens(String.valueOf(tokenID), server_version, expiration);
            }
        } catch (ServiceException e) {
            LOG.error("unable to register auth token", e);
        }
    }

    //remove the token from LDAP (token will be invalid for cookie-based auth after that
    @Override
    public void deRegister() throws AuthTokenException {
		try {
		    Account acct = Provisioning.getInstance().getAccountById(accountId);
		    if(acct != null) {
		        acct.removeAuthTokens(String.valueOf(tokenID), server_version);
		    }
		    if(acct.getBooleanAttr(Provisioning.A_zimbraLogOutFromAllServers, false)) {
		        AuthTokenRegistry.addTokenToQueue(this);
		    }
		} catch (ServiceException e) {
			throw new AuthTokenException("unable to de-register auth token", e);
		}

    }

    @Override
    public String getEncoded() throws AuthTokenException {
        if (encoded == null) {
            StringBuilder encodedBuff = new StringBuilder(64);
            BlobMetaData.encodeMetaData(C_ID, accountId, encodedBuff);
            BlobMetaData.encodeMetaData(C_EXP, Long.toString(expires), encodedBuff);
            if (adminAccountId != null) {
                BlobMetaData.encodeMetaData(C_AID, adminAccountId, encodedBuff);
            }
            if (isAdmin) {
                BlobMetaData.encodeMetaData(C_ADMIN, "1", encodedBuff);
            }
            if (isDomainAdmin) {
                BlobMetaData.encodeMetaData(C_DOMAIN, "1", encodedBuff);
            }
            if (isDelegatedAdmin) {
                BlobMetaData.encodeMetaData(C_DLGADMIN, "1", encodedBuff);
            }
            if (validityValue != -1) {
                BlobMetaData.encodeMetaData(C_VALIDITY_VALUE, validityValue, encodedBuff);
            }
            BlobMetaData.encodeMetaData(C_TYPE, type, encodedBuff);

            if (authMech != null) {
                BlobMetaData.encodeMetaData(C_AUTH_MECH, authMech.name(), encodedBuff);
            }

            if (usage != null) {
                BlobMetaData.encodeMetaData(C_USAGE, usage.getCode(), encodedBuff);
            }
            BlobMetaData.encodeMetaData(C_TOKEN_ID, tokenID, encodedBuff);
            BlobMetaData.encodeMetaData(C_EXTERNAL_USER_EMAIL, externalUserEmail, encodedBuff);
            BlobMetaData.encodeMetaData(C_DIGEST, digest, encodedBuff);
            BlobMetaData.encodeMetaData(C_SERVER_VERSION, server_version, encodedBuff);
            if (this.csrfTokenEnabled) {
                BlobMetaData.encodeMetaData(C_CSRF, "1", encodedBuff);
            }

            String data = new String(Hex.encodeHex(encodedBuff.toString().getBytes()));
            AuthTokenKey key = getCurrentKey();
            String hmac = TokenUtil.getHmac(data, key.getKey());
            encoded = key.getVersion() + "_" + hmac + "_" + data;
        }
        return encoded;
    }

    @Override
    public String getCrumb() throws AuthTokenException {
        String authToken = getEncoded();
        try {
            ByteKey bk = new ByteKey(getCurrentKey().getKey());
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(bk);
            return new String(Hex.encodeHex(mac.doFinal(authToken.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal error", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("fatal error", e);
        }
    }

    private String getOrigAuthData() throws ServiceException {
        String origAuthData = null;
        try {
            origAuthData = getEncoded();
            if (origAuthData == null)
                throw ServiceException.FAILURE("unable to get encoded auth token", null);
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("unable to get encoded auth token", e);
        }

        return origAuthData;
    }

    @Override
    public boolean isRegistered() {
        if (!isZimbraUser() || isZMGAppBootstrap()) {
            return true;
        }
        try {
            Provisioning prov = Provisioning.getInstance();
            // support older clients if zimbraLowestSupportedAuthVersion is less
            // than 2
            Server localServer = Provisioning.getInstance().getLocalServer();
            if (localServer.getLowestSupportedAuthVersion() < 2) {
                return true;
            }
            Account acct = prov.getAccountById(accountId);
            if (acct != null) {
                if (isRegisteredInternal(acct)) {
                    return true;
                } else {
                    /*
                     * If we are here, we did not find the token in the cached
                     * account object. Either token has been invalidated or it
                     * was generated by another server, therefore reload the
                     * object and check one more time.
                     */
                    prov.reload(acct);
                    return isRegisteredInternal(acct);
                }
            }
        } catch (ServiceException e) {
            LOG.fatal("Unable to verify auth token registration in ephemeral store", e);
        }

        return false;
    }

    private boolean isRegisteredInternal(Account acct) throws ServiceException {
        return acct.hasAuthTokens(String.valueOf(tokenID));
    }

    @Override
    public void encode(HttpClient client, HttpMethod method, boolean isAdminReq, String cookieDomain)
    throws ServiceException {
        String origAuthData = getOrigAuthData();

        HttpState state = new HttpState();
        client.setState(state);

        state.addCookie(new org.apache.commons.httpclient.Cookie(cookieDomain,
                ZimbraCookie.authTokenCookieName(isAdminReq), origAuthData, "/", null, false));
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    @Override
    public void encode(HttpState state, boolean isAdminReq, String cookieDomain) throws ServiceException {
        String origAuthData = getOrigAuthData();
        state.addCookie(new org.apache.commons.httpclient.Cookie(cookieDomain,
                ZimbraCookie.authTokenCookieName(isAdminReq), origAuthData, "/", null, false));
    }

    @Override
    public void encode(HttpServletResponse resp, boolean isAdminReq,
            boolean secureCookie, boolean rememberMe)
    throws ServiceException {
        String origAuthData = getOrigAuthData();

        Integer maxAge;
        if (rememberMe) {
            long timeLeft = expires - System.currentTimeMillis();
            maxAge = Integer.valueOf((int)(timeLeft/1000));
        } else {
            maxAge = Integer.valueOf(-1);
        }

        ZimbraCookie.addHttpOnlyCookie(resp,
                ZimbraCookie.authTokenCookieName(isAdminReq), origAuthData,
                ZimbraCookie.PATH_ROOT, maxAge, secureCookie);
    }

    @Override
    public void encodeAuthResp(Element parent, boolean isAdmin)  throws ServiceException {
        if (isAdmin) {
            parent.addElement(AdminConstants.E_AUTH_TOKEN).setText(getOrigAuthData());
        } else {
            parent.addElement(AccountConstants.E_AUTH_TOKEN).setText(getOrigAuthData());
        }
    }

    @Override
    public ZAuthToken toZAuthToken() throws ServiceException {
        return new ZAuthToken(getOrigAuthData(), proxyAuthToken);
    }

    @Override
    public void setProxyAuthToken(String encoded) {
        proxyAuthToken = encoded;
    }

    @Override
    public String getProxyAuthToken() {
        return proxyAuthToken;
    }

    @Override
    public void resetProxyAuthToken() {
        proxyAuthToken = null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.AuthToken#isCsrfTokenEnabled()
     */
    @Override
    public boolean isCsrfTokenEnabled() {
        return csrfTokenEnabled;
    }

    @Override
    public void setCsrfTokenEnabled(boolean csrfEnabled) {
        if (csrfEnabled != csrfTokenEnabled) {
            synchronized (ZimbraAuthToken.class) {
                if (encoded != null) {
                    CACHE.remove(encoded);
                }
            }
            csrfTokenEnabled = csrfEnabled;
            // force re-encoding of the token
            encoded = null;
        }
    }

    @Override
    public Usage getUsage() {
        return usage;
    }

    /**
     * This method is used to reset tokenID.
     * One use case is when new authtoken is created by cloning existing authtoken,
     * tokenID of newly authtoken is reset to make it unique and all the other properties are
     * same as of authtoken from which it is cloned.
     * Cached encoded string is also reset as the due to change in TokenID.
     */
    public void resetTokenId() {
         tokenID = new Random().nextInt(Integer.MAX_VALUE-1) + 1;
         encoded = null;
         this.register();
    }

    @Override
    public boolean isZMGAppBootstrap() {
        return C_TYPE_ZMG_APP.equals(type);
    }


    static class ByteKey implements SecretKey {
        private static final long serialVersionUID = -7237091299729195624L;
        private final byte[] mKey;

        ByteKey(byte[] key) {
            mKey = key.clone();
        }

        @Override
        public byte[] getEncoded() {
            return mKey;
        }

        @Override
        public String getAlgorithm() {
            return "HmacSHA1";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }

    }

    @Override
    public ZimbraAuthToken clone() throws CloneNotSupportedException {
        return (ZimbraAuthToken)super.clone();
    }

    /*
     * Used when the auth token needs to be registered with a non-default
     * ephemeral backend
     */
    public void registerWithEphemeralStore(EphemeralStore store) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.id, accountId);
        Expiration expiration = new AbsoluteExpiration(this.expires);
        EphemeralLocation location = new LdapEntryLocation(acct);
        EphemeralKey key = new EphemeralKey(Provisioning.A_zimbraAuthTokens, String.valueOf(tokenID));
        EphemeralInput input = new EphemeralInput(key, server_version, expiration);
        store.update(input, location);
    }

    public static void main(String args[]) throws ServiceException, AuthTokenException {
        Account a = Provisioning.getInstance().get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        long start = System.currentTimeMillis();
        String encoded = at.getEncoded();
        for (int i = 0; i < 1000; i++) {
            new ZimbraAuthToken(encoded);
        }
        long finish = System.currentTimeMillis();
        System.out.println(finish-start);

        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            getAuthToken(encoded);
        }
        finish = System.currentTimeMillis();
        System.out.println(finish-start);
    }
}
