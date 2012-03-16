/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import com.google.common.base.Objects;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.BlobMetaDataEncodingException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import com.zimbra.common.util.MapUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

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
    private static final String C_EXTERNAL_USER_EMAIL = "email";
    private static final String C_DIGEST = "digest";
    private static final String C_VALIDITY_VALUE  = "vv";
    private static final String C_AUTH_MECH = "am";

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
        return getAttrs(parts[2]);
    }

    private static Map<?, ?> getAttrs(String data) throws AuthTokenException{
        try {
            String decoded = new String(Hex.decodeHex(data.toCharArray()));
            return BlobMetaData.decode(decoded);
        } catch (DecoderException e) {
            throw new AuthTokenException("decoding exception", e);
        } catch (BlobMetaDataEncodingException e) {
            throw new AuthTokenException("blob decoding exception", e);
        }
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
            String computedHmac = getHmac(data, key.getKey());
            if (!computedHmac.equals(hmac)) {
                throw new AuthTokenException("hmac failure");
            }
            Map<?, ?> map = getAttrs(data);

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

        } catch (ServiceException e) {
            throw new AuthTokenException("service exception", e);
        }
    }

    public ZimbraAuthToken(Account acct) {
        this(acct, false, null);
    }

    public ZimbraAuthToken(Account acct, boolean isAdmin, AuthMech authMech) {
        this(acct, 0, isAdmin, null, authMech);
        long lifetime = isAdmin || isDomainAdmin || isDelegatedAdmin ?
                    acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000) :
                    acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000);
        expires = System.currentTimeMillis() + lifetime;
    }

    public ZimbraAuthToken(Account acct, long expires) {
        this(acct, expires, false, null, null);
    }

    /**
     * @param acct account authtoken will be valid for
     * @param expires when the token expires
     * @param isAdmin true if acct is an admin account
     * @param adminAcct the admin account accessing acct's information, if this token was created by an admin. mainly used
     *        for auditing.
     */
    public ZimbraAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct, 
            AuthMech authMech) {
        accountId = acct.getId();
        adminAccountId = adminAcct != null ? adminAcct.getId() : null;
        validityValue = acct.getAuthTokenValidityValue();
        this.expires = expires;
        this.isAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsAdminAccount));
        isDomainAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDomainAdminAccount));
        isDelegatedAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDelegatedAdminAccount));
        this.authMech = authMech;
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
    }

    public ZimbraAuthToken(String acctId, String externalEmail, String pass, String digest, long expires) {
        accountId = acctId;
        this.expires = expires;
        externalUserEmail = externalEmail == null ? "public" : externalEmail;
        this.digest = digest != null ? digest : generateDigest(externalEmail, pass);
        type = C_TYPE_EXTERNAL_USER;
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
        return type == null || type.compareTo(C_TYPE_ZIMBRA_USER) == 0;
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
            
            BlobMetaData.encodeMetaData(C_EXTERNAL_USER_EMAIL, externalUserEmail, encodedBuff);
            BlobMetaData.encodeMetaData(C_DIGEST, digest, encodedBuff);
            String data = new String(Hex.encodeHex(encodedBuff.toString().getBytes()));
            AuthTokenKey key = getCurrentKey();
            String hmac = getHmac(data, key.getKey());
            encoded = key.getVersion() + "_" + hmac + "_" + data;
        }
        return encoded;
    }

    public static String getHmac(String data, byte[] key) {
        try {
            ByteKey bk = new ByteKey(key);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(bk);
            return new String(Hex.encodeHex(mac.doFinal(data.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("fatal error", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("fatal error", e);
        }
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

    static class ByteKey implements SecretKey {
        private static final long serialVersionUID = -7237091299729195624L;
        private byte[] mKey;

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
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
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
