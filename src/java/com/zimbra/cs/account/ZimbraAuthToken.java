/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on May 30, 2004
 */
package com.zimbra.cs.account;

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
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.ZimbraAuthProvider;
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
 * @author schemers
 */
public class ZimbraAuthToken extends AuthToken {
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

    private static Map mCache = MapUtil.newLruMap(LC.zimbra_authtoken_cache_size.intValue());
    
    private static Log mLog = LogFactory.getLog(AuthToken.class); 
    
    private String mAccountId;
    private String mAdminAccountId;
    private int mValidityValue = -1;
    private long mExpires;
    private String mEncoded;
    private boolean mIsAdmin;
    private boolean mIsDomainAdmin;
    private boolean mIsDelegatedAdmin;
//  private static AuthTokenKey mTempKey;
    private String mType;
    private String mExternalUserEmail;
    private String mDigest;
    private String mAccessKey; // just a dummy placeholder for now until accesskey auth is implemented in ZimbraAuthToken
    private String mProxyAuthToken;
    
    public String toString() {
        return "AuthToken(acct="+mAccountId+" admin="+mAdminAccountId+" exp="
        +mExpires+" isAdm="+mIsAdmin+" isDomAd="+mIsDomainAdmin+" isDlgAd="+mIsDelegatedAdmin+")";
    }
    
    protected static AuthTokenKey getCurrentKey() throws AuthTokenException {
        try {
            AuthTokenKey key = AuthTokenKey.getCurrentKey();
            return key;
        } catch (ServiceException e) {
            mLog.fatal("unable to get latest AuthTokenKey", e);
            throw new AuthTokenException("unable to get AuthTokenKey", e);
        }
    }

    /**
     * Return an AuthToken object using an encoded authtoken. Caller should call isExpired on returned
     * authToken before using it.
     * @param encoded
     * @return
     * @throws AuthTokenException
     */
    public synchronized static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        ZimbraAuthToken at = (ZimbraAuthToken) mCache.get(encoded);
        if (at == null) {
            at = new ZimbraAuthToken(encoded);
            if (!at.isExpired())
                mCache.put(encoded, at);
        } else {
            // remove it if expired
            if (at.isExpired())
                mCache.remove(encoded);
        }
        return at;
    }
    
    protected ZimbraAuthToken() {
         
    }
    
    public static Map getInfo(String encoded) throws AuthTokenException {
        String[] parts = encoded.split("_");
        if (parts.length != 3)
            throw new AuthTokenException("invalid authtoken format");
        
        return getAttrs(parts[2]);
    }
    
    private static Map getAttrs(String data) throws AuthTokenException{
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
            mEncoded = encoded;
            int pos = encoded.indexOf('_');
            if (pos == -1)
                throw new AuthTokenException("invalid authtoken format");

            String ver = encoded.substring(0, pos);
            
            int pos2 = encoded.indexOf('_', pos+1);
            if (pos2 == -1)
                throw new AuthTokenException("invalid authtoken format");
            String hmac = encoded.substring(pos+1, pos2);
            String data = encoded.substring(pos2+1);

            AuthTokenKey key = AuthTokenKey.getVersion(ver);
            if (key == null)
                throw new AuthTokenException("unknown key version");
            
            String computedHmac = getHmac(data, key.getKey());
            if (!computedHmac.equals(hmac))
                throw new AuthTokenException("hmac failure");      

            Map map = getAttrs(data);
            
            mAccountId = (String)map.get(C_ID);
            mAdminAccountId = (String)map.get(C_AID);            
            mExpires = Long.parseLong((String)map.get(C_EXP));
            String ia = (String) map.get(C_ADMIN);
            mIsAdmin = "1".equals(ia);
            String da = (String) map.get(C_DOMAIN);            
            mIsDomainAdmin = "1".equals(da);
            String dlga = (String) map.get(C_DLGADMIN);            
            mIsDelegatedAdmin = "1".equals(dlga);
            mType = (String)map.get(C_TYPE);
            mExternalUserEmail = (String)map.get(C_EXTERNAL_USER_EMAIL);
            mDigest = (String)map.get(C_DIGEST);
            String vv = (String)map.get(C_VALIDITY_VALUE);
            if (vv != null) {
                try {
                    mValidityValue = Integer.parseInt(vv);
                } catch (NumberFormatException e) {
                    mValidityValue = -1;
                }
            } else {
                mValidityValue = -1;
            }

        } catch (ServiceException e) {
            throw new AuthTokenException("service exception", e);
        }
    }

    public ZimbraAuthToken(Account acct) {
        this(acct, false);
    }

    public ZimbraAuthToken(Account acct, boolean isAdmin) {
        this(acct, 0, isAdmin, null);
        long lifetime = mIsAdmin || mIsDomainAdmin || mIsDelegatedAdmin ?
                    acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000) :                                    
                    acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000);
        mExpires = System.currentTimeMillis() + lifetime;
    }

    public ZimbraAuthToken(Account acct, long expires) {
        this(acct, expires, false, null);
    }

    /**
     * @param acct account authtoken will be valid for
     * @param expires when the token expires
     * @param isAdmin true if acct is an admin account
     * @param adminAcct the admin account accessing acct's information, if this token was created by an admin. mainly used
     *        for auditing.
     */
    public ZimbraAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct) {
        mAccountId = acct.getId();
        mAdminAccountId = adminAcct != null ? adminAcct.getId() : null;
        mValidityValue = acct.getAuthTokenValidityValue();
        mExpires = expires;
        mIsAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsAdminAccount));
        mIsDomainAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDomainAdminAccount));
        mIsDelegatedAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDelegatedAdminAccount));
        mEncoded = null;
        if (acct instanceof GuestAccount) {
            mType = C_TYPE_EXTERNAL_USER;
            GuestAccount g = (GuestAccount) acct;
            mDigest = g.getDigest();
            mAccessKey = g.getAccessKey();
            mExternalUserEmail = g.getName();
        } else
            mType = C_TYPE_ZIMBRA_USER;
    }

    public ZimbraAuthToken(String acctId, String externalEmail, String pass, String digest, long expires) {
        mAccountId = acctId;
        mExpires = expires;
        mExternalUserEmail = externalEmail == null ? "public" : externalEmail;
        if (digest != null)
            mDigest = digest;
        else
            mDigest = generateDigest(externalEmail, pass);
        
        mType = C_TYPE_EXTERNAL_USER;
    }
    
    public String getAccountId() {
        return mAccountId;
    }

    public String getAdminAccountId() {
        return mAdminAccountId;
    }

    public long getExpires() {
        return mExpires;
    }

    public int getValidityValue() {
        return mValidityValue;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > mExpires;
    }

    public boolean isAdmin() {
        return mIsAdmin;
    }

    public boolean isDomainAdmin() {
        return mIsDomainAdmin;
    }
    
    public boolean isDelegatedAdmin() {
        return mIsDelegatedAdmin;
    }
    
    public boolean isZimbraUser() {
        return mType == null || mType.compareTo(C_TYPE_ZIMBRA_USER) == 0;
    }

    public String getExternalUserEmail() {
        return mExternalUserEmail;
    }
    
    public String getDigest() {
        return mDigest;
    }
    
    public String getAccessKey() {
        return mAccessKey;
    }
    
    public String getEncoded() throws AuthTokenException {
        if (mEncoded == null) {
            StringBuffer encodedBuff = new StringBuffer(64);
            BlobMetaData.encodeMetaData(C_ID, mAccountId, encodedBuff);
            BlobMetaData.encodeMetaData(C_EXP, Long.toString(mExpires), encodedBuff);
            if (mAdminAccountId != null)
                BlobMetaData.encodeMetaData(C_AID, mAdminAccountId, encodedBuff);                
            if (mIsAdmin)
                BlobMetaData.encodeMetaData(C_ADMIN, "1", encodedBuff);
            if (mIsDomainAdmin)
                BlobMetaData.encodeMetaData(C_DOMAIN, "1", encodedBuff);
            if (mIsDelegatedAdmin)
                BlobMetaData.encodeMetaData(C_DLGADMIN, "1", encodedBuff);
            if (mValidityValue != -1)
                BlobMetaData.encodeMetaData(C_VALIDITY_VALUE, mValidityValue, encodedBuff);
            BlobMetaData.encodeMetaData(C_TYPE, mType, encodedBuff);
            BlobMetaData.encodeMetaData(C_EXTERNAL_USER_EMAIL, mExternalUserEmail, encodedBuff);
            BlobMetaData.encodeMetaData(C_DIGEST, mDigest, encodedBuff);
            String data = new String(Hex.encodeHex(encodedBuff.toString().getBytes()));
            AuthTokenKey key = getCurrentKey();
            String hmac = getHmac(data, key.getKey());
            mEncoded = key.getVersion()+"_"+hmac+"_"+data;
        }
        return mEncoded;
    }

    private String getHmac(String data, byte[] key) {
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
    
    /**
     * 
     * @param client
     * @param method
     * @param isAdminReq
     * @throws ServiceException
     */
    public void encode(HttpClient client, HttpMethod method, boolean isAdminReq, String cookieDomain) throws ServiceException {
        String origAuthData = getOrigAuthData();
        
        HttpState state = new HttpState();
        client.setState(state);
        
        state.addCookie(new org.apache.commons.httpclient.Cookie(cookieDomain, ZimbraAuthProvider.cookieName(isAdminReq), origAuthData, "/", null, false));
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }
    
    public void encode(HttpState state, boolean isAdminReq, String cookieDomain) throws ServiceException {
        String origAuthData = getOrigAuthData();
        state.addCookie(new org.apache.commons.httpclient.Cookie(cookieDomain, ZimbraAuthProvider.cookieName(isAdminReq), origAuthData, "/", null, false));
    }
    
    public void encode(HttpServletResponse resp, boolean isAdminReq, boolean secureCookie) throws ServiceException {
        String origAuthData = getOrigAuthData();
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(ZimbraAuthProvider.cookieName(isAdminReq), origAuthData);
        ZimbraCookie.setAuthTokenCookieDomainPath(cookie, ZimbraCookie.PATH_ROOT);
        cookie.setSecure(secureCookie);
        resp.addCookie(cookie);
        
    }
    
    public void encodeAuthResp(Element parent, boolean isAdmin)  throws ServiceException {
        if (isAdmin)
            parent.addElement(AdminConstants.E_AUTH_TOKEN).setText(getOrigAuthData());
        else
            parent.addElement(AccountConstants.E_AUTH_TOKEN).setText(getOrigAuthData());
    }
    
    public ZAuthToken toZAuthToken() throws ServiceException {
        return new ZAuthToken(getOrigAuthData(), mProxyAuthToken);
    }
    
    @Override
    public void setProxyAuthToken(String encoded) {
        mProxyAuthToken = encoded;
    }
    
    @Override
    public String getProxyAuthToken() {
        return mProxyAuthToken;
    }
    
    @Override
    public void resetProxyAuthToken() {
        mProxyAuthToken = null;
    }
    
    /*
    public void encodeAuthReq(Element authRequest)  throws ServiceException {
        String origAuthData = getOrigAuthData();
        Element authTokenEl = authRequest.addElement(AccountConstants.E_AUTH_TOKEN);
        authTokenEl.setText(origAuthData);
    }
    */

    static class ByteKey implements SecretKey {
        private static final long serialVersionUID = -7237091299729195624L;
        private byte[] mKey;
        
        ByteKey(byte[] key) {
            mKey = key.clone();
        }
        
        public byte[] getEncoded() {
            return mKey;
        }

        public String getAlgorithm() {
            return "HmacSHA1";
        }

        public String getFormat() {
            return "RAW";
        }       

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
