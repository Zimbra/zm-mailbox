/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;

public class PasswordUtil {
    
    /*
     * SSHA  (salted-SHA1)
     */
    public static class SSHA {

        private static int SALT_LEN = 4; // to match LDAP SSHA password encoding
        private static String SSHA_ENCODING = "{SSHA}";
        
        public static boolean isSSHA(String encodedPassword) {
            return encodedPassword.startsWith(SSHA_ENCODING);
        }
        
        public static boolean verifySSHA(String encodedPassword, String password) {
            if (!encodedPassword.startsWith(SSHA_ENCODING))
                return false;
            byte[] encodedBuff = encodedPassword.substring(SSHA_ENCODING.length()).getBytes();
            byte[] buff = Base64.decodeBase64(encodedBuff);
            if (buff.length <= SALT_LEN)
                return false;
            int slen = (buff.length == 28) ? 8 : SALT_LEN;
            byte[] salt = new byte[slen];
            System.arraycopy(buff, buff.length-slen, salt, 0, slen);
            String generated = generateSSHA(password, salt);
            return generated.equals(encodedPassword);
        }
        
        public static String generateSSHA(String password, byte[] salt) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                if (salt == null) {
                    salt = new byte[SALT_LEN];
                    SecureRandom sr = new SecureRandom();
                    sr.nextBytes(salt);
                } 
                md.update(password.getBytes("UTF-8"));
                md.update(salt);
                byte[] digest = md.digest();
                byte[] buff = new byte[digest.length + salt.length];
                System.arraycopy(digest, 0, buff, 0, digest.length);
                System.arraycopy(salt, 0, buff, digest.length, salt.length);
                return SSHA_ENCODING + new String(Base64.encodeBase64(buff));
            } catch (NoSuchAlgorithmException e) {
                // this shouldn't happen unless JDK is foobar
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                // this shouldn't happen unless JDK is foobar
                throw new RuntimeException(e);
            }
        }
        
    }
    
    
    /*
     * SHA1 (unseeded)
     */
    public static class SHA1 {
        
        private static String SHA1_ENCODING = "{SHA1}";
        
        public static boolean isSHA1(String encodedPassword) {
            return encodedPassword.startsWith(SHA1_ENCODING);
        }
        
        public static boolean verifySHA1(String encodedPassword, String password) {
            if (!encodedPassword.startsWith(SHA1_ENCODING))
                return false;
            byte[] encodedBuff = encodedPassword.substring(SHA1_ENCODING.length()).getBytes();
            byte[] buff = Base64.decodeBase64(encodedBuff);
            String generated = generateSHA1(password);
            return generated.equals(encodedPassword);
        }
        
        public static String generateSHA1(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(password.getBytes("UTF-8"));
                
                byte[] digest = md.digest();
                return SHA1_ENCODING + new String(Base64.encodeBase64(digest));
            } catch (NoSuchAlgorithmException e) {
                // this shouldn't happen unless JDK is foobar
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                // this shouldn't happen unless JDK is foobar
                throw new RuntimeException(e);
            }
        }
    }
    
    /*
     * MD5
     */
    public static class MD5 {
        
        private static String MD5_ENCODING = "{MD5}";
        
        public static boolean isMD5(String encodedPassword) {
            return encodedPassword.startsWith(MD5_ENCODING);
        }
        
        public static boolean verifyMD5(String encodedPassword, String password) {
            if (!encodedPassword.startsWith(MD5_ENCODING))
                return false;
            byte[] encodedBuff = encodedPassword.substring(MD5_ENCODING.length()).getBytes();
            byte[] buff = Base64.decodeBase64(encodedBuff);
            String generated = generateMD5(password);
            return generated.equals(encodedPassword);
        }
        
        public static String generateMD5(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(password.getBytes("UTF-8"));
                
                byte[] digest = md.digest();
                return MD5_ENCODING + new String(Base64.encodeBase64(digest));
            } catch (NoSuchAlgorithmException e) {
                // this shouldn't happen unless JDK is foobar
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                // this shouldn't happen unless JDK is foobar
                throw new RuntimeException(e);
            }
        }
    }    

    public static void main(String args[]) {
        
        String plain = "test123";
        System.out.println("plain:        " + plain);
        System.out.println("encoded SSHA: " + SSHA.generateSSHA(plain, null));
        System.out.println("encoded SSH1: " + SHA1.generateSHA1(plain));
        System.out.println("encoded MD5:  " + MD5.generateMD5(plain));
        System.out.println();
        
        plain = "helloWorld";
        System.out.println("plain:        " + plain);
        System.out.println("encoded SSHA: " + SSHA.generateSSHA(plain, null));
        System.out.println("encoded SSH1: " + SHA1.generateSHA1(plain));
        System.out.println("encoded MD5:  " + MD5.generateMD5(plain));
        System.out.println();
        
    }

}
