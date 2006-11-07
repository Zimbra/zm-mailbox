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

package com.zimbra.cs.account;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class DataSource extends NamedEntry implements Comparable {

    private static final int SALT_SIZE_BYTES = 16;
    private static final int AES_PAD_SIZE = 16;
    private static final byte[] VERSION = { 1 };
    private static final String SIMPLE_CLASS_NAME =
        StringUtil.getSimpleClassName(DataSource.class.getName());
    
    public enum Type {
        pop3;
        
        public static Type fromString(String s) throws ServiceException {
            try {
                return Type.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid type: "+s+", valid values: "+Arrays.asList(Type.values()), e); 
            }
        }
    };
    
    public static final String CT_CLEARTEXT = "cleartext";
    public static final String CT_SSL = "ssl";
    
    private Type mType;

    public DataSource(Type type, String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
        mType = type;
    }
    
    public Type getType() {
        return mType;
    }
    
    public boolean isEnabled() { return getBooleanAttr(Provisioning.A_zimbraDataSourceEnabled, false); }

    public String getConnectionType() { return getAttr(Provisioning.A_zimbraDataSourceConnectionType); }
    
    public int getFolderId() { return getIntAttr(Provisioning.A_zimbraDataSourceFolderId, -1); }
    
    public String getHost() { return getAttr(Provisioning.A_zimbraDataSourceHost); }
    
    public String getUsername() { return getAttr(Provisioning.A_zimbraDataSourceUsername); }
    
    public Integer getPort() {
        if (getAttr(Provisioning.A_zimbraDataSourcePort) == null) {
            return null;
        }
        return getIntAttr(Provisioning.A_zimbraDataSourcePort, -1);
    }
    
    public String getDecryptedPassword() throws ServiceException {
        String data = getAttr(Provisioning.A_zimbraDataSourcePassword);
        return data == null ? null : decryptData(getId(), data); 
    }

    private static byte[] randomSalt() {
        SecureRandom random = new SecureRandom();
        byte[] pad = new byte[SALT_SIZE_BYTES];
        random.nextBytes(pad);
        return pad;
    }

    private static Cipher getCipher(String dataSourceId, byte[] salt, boolean encrypt) throws GeneralSecurityException, UnsupportedEncodingException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(salt);
        md5.update(dataSourceId.getBytes("utf-8"));
        byte[] key = md5.digest();
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, skeySpec);
        return cipher;
    }
    
    public static String encryptData(String dataSourceId, String data) throws ServiceException {
        try {
            byte[] salt = randomSalt();
            Cipher cipher = getCipher(dataSourceId, salt, true);
            byte[] dataBytes = cipher.doFinal(data.getBytes("utf-8"));
            byte[] toEncode = new byte[VERSION.length + salt.length + dataBytes.length];
            System.arraycopy(VERSION, 0, toEncode, 0, VERSION.length);            
            System.arraycopy(salt, 0, toEncode, VERSION.length, salt.length);
            System.arraycopy(dataBytes, 0, toEncode, VERSION.length+salt.length, dataBytes.length); 
            return new String(Base64.encodeBase64(toEncode));
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("caught unsupport encoding exception", e);
        } catch (GeneralSecurityException e) {
            throw ServiceException.FAILURE("caught security exception", e); 
        }
    }
    
    public static String decryptData(String dataSourceId, String data) throws ServiceException {
        try {
            byte[] encoded = Base64.decodeBase64(data.getBytes());
            if (encoded.length < VERSION.length + SALT_SIZE_BYTES + AES_PAD_SIZE)
                throw ServiceException.FAILURE("invalid encoded size: "+encoded.length, null);
            byte[] version = new byte[VERSION.length];
            byte[] salt = new byte[SALT_SIZE_BYTES];
            System.arraycopy(encoded, 0, version, 0, VERSION.length);
            if (!Arrays.equals(version, VERSION))
                throw ServiceException.FAILURE("unsupported version", null);            
            System.arraycopy(encoded, VERSION.length, salt, 0, SALT_SIZE_BYTES);
            Cipher cipher = getCipher(dataSourceId, salt, false);
            return new String(cipher.doFinal(encoded, VERSION.length + SALT_SIZE_BYTES, encoded.length - SALT_SIZE_BYTES - VERSION.length), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("caught unsupport encoding exception", e);
        } catch (GeneralSecurityException e) {
            throw ServiceException.FAILURE("caught security exception", e); 
        }
    }


    public String toString() {
        List<String> parts = new ArrayList<String>();
        parts.add("id=" + getId());
        parts.add("type=" + getType());
        parts.add("isEnabled=" + isEnabled());
        if (getName() != null) {
            parts.add("name=" + getName());
        }
        if (getHost() != null) {
            parts.add("host=" + getHost());
        }
        if (getPort() != null) {
            parts.add("port=" + getPort());
        }
        if (getConnectionType() != null) {
            parts.add("connectionType=" + getConnectionType());
        }
        if (getUsername() != null) {
            parts.add("username=" + getUsername());
        }
        parts.add("folderId=" + getFolderId());
        return String.format("%s: { %s }",
            SIMPLE_CLASS_NAME, StringUtil.join(", ", parts));
    }
    
    public static void main(String args[]) throws ServiceException {
        String dataSourceId = UUID.randomUUID().toString();
        String enc = encryptData(dataSourceId, "helloworld");
        System.out.println(enc);
        System.out.println(decryptData(dataSourceId, enc));
    }

}


