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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

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
public abstract class AuthToken {
    
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
    
    public abstract String toString();
    
    public abstract String getAccountId() ;

    public abstract String getAdminAccountId();
    
    public abstract long getExpires();

    public abstract boolean isExpired();

    public abstract boolean isAdmin();

    public abstract boolean isDomainAdmin();
    
    public abstract boolean isCCAdmin();
    
    public abstract boolean isZimbraUser();

    public abstract String getExternalUserEmail() ;
    
    public abstract String getDigest();
    
    public abstract int getCCTier();
    
    public abstract String getCrumb() throws AuthTokenException;
    
    /**
     * Encode auth info into an outgoing http request.
     * 
     * @param client
     * @param method
     * @param isAdminReq
     * @param cookieDomain
     * @throws ServiceException
     */
    public abstract void encode(HttpClient client, HttpMethod method, boolean isAdminReq, String cookieDomain) throws ServiceException;

    
    // AP-TODO: REMOVE AFTER CLEANUP
    public abstract String getEncoded() throws AuthTokenException;
    
    // AP-TODO: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String encoded) throws AuthTokenException {
        return ZimbraAuthToken.getAuthToken(encoded);
    }
    
    // AP-TODO: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct) {
        return new ZimbraAuthToken(acct);
    }
    
    // AP-TODO: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct, boolean isAdmin) {
        return new ZimbraAuthToken(acct, isAdmin);
    }
    
    // AP-TODO: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct, long expires) {
        return new ZimbraAuthToken(acct, expires);
    }
    
    // AP-TODO: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(Account acct, long expires, boolean isAdmin, Account adminAcct) {
        return new ZimbraAuthToken(acct, expires, isAdmin, adminAcct);
    }
    
    // AP-TODO: REMOVE AFTER CLEANUP
    public static AuthToken getAuthToken(String acctId, String externalEmail, String pass, String digest, long expires) {
        return new ZimbraAuthToken(acctId, externalEmail, pass, digest, expires);
    }
    
    // AP-TODO: REMOVE AFTER CLEANUP
    public static AuthToken getZimbraAdminAuthToken() throws ServiceException {
        return ZimbraAuthToken.getZimbraAdminAuthToken();
    }

}
