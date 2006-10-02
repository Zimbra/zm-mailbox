/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jan 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Hex;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.common.util.Constants;

/**
 * @author schemers
 */
public class PreAuthKey {
    
    public static final int KEY_SIZE_BYTES = 32;

    public static String generateRandomPreAuthKey() throws ServiceException {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            byte[] key = new byte[KEY_SIZE_BYTES];
            random.nextBytes(key);
            return new String(Hex.encodeHex(key));
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("unable to initialize SecureRandom", e);
        }
    }
    
    public static  String computePreAuth(Map<String,String> params, String key) {
        TreeSet<String> names = new TreeSet<String>(params.keySet());
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            if (sb.length() > 0) sb.append('|');
            sb.append(params.get(name));
        }
        return getHmac(sb.toString(), key.getBytes());
    }

    private static String getHmac(String data, byte[] key) {
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
    
    public static void main(String args[]) throws ServiceException {
        long now = System.currentTimeMillis();
        HashMap<String,String> params = new HashMap<String,String>();
        params.put("account", "user1@slapshot");
        params.put("timestamp", now+"");
        params.put("expires", (now+Constants.MILLIS_PER_MINUTE*4)+"");
        String key = generateRandomPreAuthKey();
        System.out.printf("key=%s preAuth=%s\n", key, computePreAuth(params, key));
        System.out.printf("key=%s preAuth=%s\n", "helloworld", computePreAuth(params, "helloworld"));
    }
}
