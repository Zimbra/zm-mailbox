/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.im.provider;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

/**
 * Wrapper around an LDAP-stored shared secret key, used by the  XMPP Server Dialback protocol. 
 */
public class ServerDialbackKey {
    
    public static final int KEY_SIZE_BYTES = 32;
    private byte[] mKey;
    private long mVersion;
    private long mCreated;
    private static HashMap<String, ServerDialbackKey> mCache = new HashMap<String, ServerDialbackKey>();
    private static ServerDialbackKey sLatestKey;
    
    
    private ServerDialbackKey(long version, byte[] key) throws ServiceException {
        mVersion = version;
        mCreated = System.currentTimeMillis();
        if (key != null) { 
            mKey = key;
        } else {
            SecureRandom random = new SecureRandom();
            mKey = new byte[KEY_SIZE_BYTES];
            random.nextBytes(mKey);
        }
    }
    
    private ServerDialbackKey(String k) throws ServiceException {
        String parts[] = k.split(":");
        if (parts.length != 3)
            throw ServiceException.INVALID_REQUEST("invalid auth token key", null);
        String ver = parts[0];
        String created = parts[1];
        String data = parts[2];
        
        try {
            mVersion = Long.parseLong(ver);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key version", e);
        }
        
        try {
            mCreated = Long.parseLong(created);
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key created data", e);
        }

        try {
            mKey = Hex.decodeHex(data.toCharArray());
        } catch (DecoderException e) {
            throw ServiceException.INVALID_REQUEST("invalid auth token key data", e);
        }
    }

    public String getEncoded() {
        return mVersion+":"+mCreated+":"+new String(Hex.encodeHex(mKey));
    }
    
    private static synchronized void refresh(boolean reload) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        // force reload
        if (reload)
            prov.reload(config);
        
        String[] keys = config.getMultiAttr(Provisioning.A_zimbraAuthTokenKey);

        if (keys.length == 0) {
            prov.reload(config);
            keys = config.getMultiAttr(Provisioning.A_zimbraAuthTokenKey);
        }

        // bootstrap. automatically create new random key
        if (keys.length == 0) {
            ServerDialbackKey key = new ServerDialbackKey(0, null);
            HashMap<String, String> attrs = new HashMap<String, String>();
            attrs.put(Provisioning.A_zimbraAuthTokenKey, key.getEncoded());
            Provisioning.getInstance().modifyAttrs(config, attrs);
            keys = config.getMultiAttr(Provisioning.A_zimbraAuthTokenKey);
        }

        for (int i=0; i < keys.length; i++) {
            ServerDialbackKey key = mCache.get(keys[i]);
            if (key == null) {
                key = new ServerDialbackKey(keys[i]);
                mCache.put(keys[i], key);
                mCache.put(Long.toString(key.mVersion), key);
                if (sLatestKey == null || sLatestKey.mVersion < key.mVersion)
                    sLatestKey = key;
            }
        }        
    }

    static synchronized ServerDialbackKey getCurrentKey() throws ServiceException {
        if (sLatestKey == null) {
            refresh(false);
        }
        return sLatestKey;
    }
    
    byte[] getKey() { return mKey; }
    
    public static String getHmac(String data) throws ServiceException {
        try {
            ByteKey bk = new ByteKey(ServerDialbackKey.getCurrentKey().getKey());
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
        private static final long serialVersionUID = 7300100462083389328L;
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
    
}
