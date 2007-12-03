/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 30, 2004
 */
package com.zimbra.cs.account;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.map.LRUMap;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.ACL.GuestAccount;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BlobMetaData;
import com.zimbra.common.util.BlobMetaDataEncodingException;
import com.zimbra.common.util.StringUtil;

/**
 * @author schemers
 */
public class AuthToken {

    private static final long DEFAULT_AUTH_LIFETIME = 60*60*12;
    
    // TODO: config
    private static final int AUTHTOKEN_CACHE_SIZE = 5000;
    
    private static final String C_ID  = "id";
    // original admin id
    private static final String C_AID  = "aid";    
	private static final String C_EXP = "exp";
    private static final String C_ADMIN = "admin";
    private static final String C_DOMAIN = "domain";    
    private static final String C_TYPE = "type";
    private static final String C_TYPE_ZIMBRA_USER = "zimbra";
    private static final String C_TYPE_EXTERNAL_USER = "external";
    private static final String C_EXTERNAL_USER_EMAIL = "email";
    private static final String C_DIGEST = "digest";
    private static final String C_MAILHOST = "mailhost";
    
	private static LRUMap mCache = new LRUMap(AUTHTOKEN_CACHE_SIZE);
    
    private static Log mLog = LogFactory.getLog(AuthToken.class); 
    
    private String mAccountId;
    private String mAdminAccountId;    
	private long mExpires;
	private String mEncoded;
    private boolean mIsAdmin;
    private boolean mIsDomainAdmin;    
//	private static AuthTokenKey mTempKey;
    private String mType;
    private String mExternalUserEmail;
    private String mDigest;
    private String[] mMailHostAddrs;
    
    public String toString() {
        return "AuthToken(acct="+mAccountId+" admin="+mAdminAccountId+" exp="
        +mExpires+" isAdm="+mIsAdmin+" isDomAd="+mIsDomainAdmin+")";
    }
    
    private static AuthTokenKey getCurrentKey() throws AuthTokenException {
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
        AuthToken at = (AuthToken) mCache.get(encoded);
        if (at == null) {
            at = new AuthToken(encoded);
            if (!at.isExpired())
                mCache.put(encoded, at);
        } else {
            // remove it if expired
            if (at.isExpired())
                mCache.remove(encoded);
        }
        return at;
    }
    
    public static AuthToken getZimbraAdminAuthToken() throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.adminName, LC.zimbra_ldap_user.value());
        return new AuthToken(acct, true);
    }
    
	private AuthToken(String encoded) throws AuthTokenException {
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

            data = new String(Hex.decodeHex(data.toCharArray()));

            Map map = BlobMetaData.decode(data);
            mAccountId = (String)map.get(C_ID);
            mAdminAccountId = (String)map.get(C_AID);            
            mExpires = Long.parseLong((String)map.get(C_EXP));
            String ia = (String) map.get(C_ADMIN);
            mIsAdmin = "1".equals(ia);
            String da = (String) map.get(C_DOMAIN);            
            mIsDomainAdmin = "1".equals(da);
            mType = (String)map.get(C_TYPE);
            mExternalUserEmail = (String)map.get(C_EXTERNAL_USER_EMAIL);
            mDigest = (String)map.get(C_DIGEST);
            String mailhostAddrs = (String)map.get(C_MAILHOST);
            if (mailhostAddrs != null) {
            	mMailHostAddrs = mailhostAddrs.split (",");
            }
        } catch (ServiceException e) {
            throw new AuthTokenException("service exception", e);
        } catch (DecoderException e) {
            throw new AuthTokenException("decoding exception", e);
		} catch (BlobMetaDataEncodingException e) {
			throw new AuthTokenException("blob decoding exception", e);
		}
	}

    public AuthToken(Account acct) {
        this(acct, false);
    }

    public AuthToken(Account acct, boolean isAdmin) {
        this(acct, 0, isAdmin, null);
        long lifetime = mIsAdmin || mIsDomainAdmin ?
                    acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000) :                                    
                    acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME * 1000);
        mExpires = System.currentTimeMillis() + lifetime;
    }

    public AuthToken(Account acct, long expires) {
        this(acct, expires, false, null);
    }

    /**
     * @param acct account authtoken will be valid for
     * @param expires when the token expires
     * @param isAdmin true if acct is an admin account
     * @param adminAcct the admin account accessing acct's information, if this token was created by an admin. mainly used
     *        for auditing.
     */
	public AuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct) {
        mAccountId = acct.getId();
        mAdminAccountId = adminAcct != null ? adminAcct.getId() : null;
		mExpires = expires;
        mIsAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsAdminAccount));
        mIsDomainAdmin = isAdmin && "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsDomainAdminAccount));
		mEncoded = null;
		if (acct instanceof ACL.GuestAccount) {
            mType = C_TYPE_EXTERNAL_USER;
            GuestAccount g = (ACL.GuestAccount) acct;
            mDigest = g.getDigest();
            mExternalUserEmail = g.getName();
        } else
            mType = C_TYPE_ZIMBRA_USER;
		
		/* build up the server:port strings for the account's mailhost */
		String mailhost = acct.getAttr(Provisioning.A_zimbraMailHost);
		if (mailhost != null) {
			List<String> mailHostAddrs = new ArrayList <String> ();
			try {
				Server server = Provisioning.getInstance().getServer(acct);
				String serverPort = server.getAttr(Provisioning.A_zimbraMailPort);
				InetAddress[] serverAddrs = InetAddress.getAllByName(mailhost);
				for (InetAddress serverAddr: serverAddrs) {
					mailHostAddrs.add(serverAddr.getHostAddress() + ":" + serverPort);
				}
			} catch (ServiceException e) {
				// cannot get server object
			} catch (UnknownHostException e) {
				// cannot look up server name
			}
			
			mMailHostAddrs = mailHostAddrs.toArray(new String[0]); 
		}
	}

    public AuthToken(String acctId, String externalEmail, String pass, String digest, long expires) {
        mAccountId = acctId;
        mExpires = expires;
        mExternalUserEmail = externalEmail == null ? "public" : externalEmail;
        if (digest != null)
            mDigest = digest;
        else
            mDigest = generateDigest(externalEmail, pass);
        
        mType = C_TYPE_EXTERNAL_USER;
    }
    
    public static String generateDigest(String a, String b) {
        if (a == null)
            return null;
        StringBuilder buf = new StringBuilder();
        buf.append(a);
        buf.append(":");
        if (b != null)
            buf.append(b);
        return ByteUtil.getDigest(buf.toString().getBytes());
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

	public boolean isExpired() {
		return System.currentTimeMillis() > mExpires;
	}

    public boolean isAdmin() {
        return mIsAdmin;
    }

    public boolean isDomainAdmin() {
        return mIsDomainAdmin;
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
            BlobMetaData.encodeMetaData(C_TYPE, mType, encodedBuff);
            BlobMetaData.encodeMetaData(C_EXTERNAL_USER_EMAIL, mExternalUserEmail, encodedBuff);
            BlobMetaData.encodeMetaData(C_DIGEST, mDigest, encodedBuff);
            if ((mMailHostAddrs != null) && (mMailHostAddrs.length > 0)) {
            	BlobMetaData.encodeMetaData(C_MAILHOST, StringUtil.join(",", mMailHostAddrs), encodedBuff);
            }
            
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
        AuthToken at = new AuthToken(a);
        long start = System.currentTimeMillis();
        String encoded = at.getEncoded();
        for (int i = 0; i < 1000; i++) {
            new AuthToken(encoded);
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
