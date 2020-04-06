/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.account.auth;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;

public class PasswordUtil {

    private static final Scheme DEFAULT_SCHEME = Scheme.SSHA512;
    private static final byte[] FIXED_SALT = {127,127,127,127};

    public enum Scheme {
        // Hardcode digest lengths to avoid exception handling in constructor
        SSHA512 ("SHA-512", 64, 8),
        SSHA    ("SHA1",    20, 4),
        SHA1    ("SHA1",    20, 0),
        SHA     ("SHA1",    20, 0),
        MD5     ("MD5",     16, 0);

        private final String ALGORITHM;
        private final int LENGTH;
        private final int DEFAULT_SALT_LENGTH;
        private final boolean SALTED;
        private final String PREFIX;

        Scheme(String algorithm, int len, int slen) {
            ALGORITHM = algorithm;
            LENGTH = len;
            DEFAULT_SALT_LENGTH = slen;
            SALTED = (DEFAULT_SALT_LENGTH == 0);
            PREFIX = "{" + this.name() + "}";
        }

        public boolean isSalted() {
            return SALTED;
        }

        public boolean validate(String encodedPassword) {
            return encodedPassword.startsWith(PREFIX);
        }

        public boolean verify(String encodedPassword, String password) {
            int index = encodedPassword.indexOf("}");
            if (index == -1)
                return false;

            byte[] encodedBuff = encodedPassword.substring(index).getBytes();
            byte[] buff = Base64.decodeBase64(encodedBuff);

            byte[] digested;

            // Bail out if encoded string too short for scheme
            if (buff.length < LENGTH)
                return false;

            // Don't bother special casing unsalted passwords; not the normal case now
            int slen = buff.length - LENGTH;
            byte[] salt = new byte[slen];
            System.arraycopy(buff, buff.length-slen, salt, 0, slen);
            digested = digest(password, salt);

            return Arrays.equals(buff, digested);
        }    

        public String generate(String password) {
            return generate(password, null);
        }

        public String generate(String password, byte[] salt) {
            return PREFIX + new String(Base64.encodeBase64(digest(password, salt)));
        }

        private byte[] digest(String password) {
            return digest(password, null);
        }

        private byte[] digest(String password, byte[] salt) {
            if (salt == null) {
                if (InMemoryLdapServer.isOn()) {
                    salt = FIXED_SALT;
                } else {
                    salt = new byte[DEFAULT_SALT_LENGTH];
                    SecureRandom sr = new SecureRandom();
                    sr.nextBytes(salt);
                }
            }

            try {
                MessageDigest md = MessageDigest.getInstance(ALGORITHM);

                md.update(password.getBytes(StandardCharsets.UTF_8));
                md.update(salt);

                byte[] digest = md.digest();
                byte[] buff = new byte[digest.length + salt.length];
                System.arraycopy(digest, 0, buff, 0, digest.length);
                System.arraycopy(salt, 0, buff, digest.length, salt.length);
                return buff;
            } catch (NoSuchAlgorithmException e) {
                // this shouldn't happen unless JDK is foobar
                throw new RuntimeException(e);
            }
        }
    }

    public static Scheme getScheme(String encodedPassword) {
        int index = encodedPassword.indexOf("}");

        if (index < 1 ) {
            return null;
        }

        String schemeName = encodedPassword.substring(1,index);
        try {
            return Scheme.valueOf(schemeName);
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean verify(String encodedPassword, String password) {
        Scheme scheme = getScheme(encodedPassword);

        if (scheme == null) {
            return null;
        }

        return scheme.verify(encodedPassword, password);
    }

    public static String generate(String password) {
        return DEFAULT_SCHEME.generate(password);
    }

    public static void main(String[] args) {
        boolean result;
        String plain;
        String encoded;

        plain = "test123";
        System.out.println("plain: " + plain);
        for (Scheme s: Scheme.values()) {
            encoded = s.generate(plain);
            result = verify (encoded, plain);
            System.out.printf("[%s] %10s: %s\n", (result?"PASS":"FAIL"), s.name(), s.generate(plain));
        }
        System.out.println();

        plain = "helloWorld";
        System.out.println("plain: " + plain);
        for (Scheme s: Scheme.values()) {
            encoded = s.generate(plain);
            result = verify (encoded, plain);
            System.out.printf("[%s] %10s: %s\n", (result?"PASS":"FAIL"), s.name(), s.generate(plain));
        }
        System.out.println();

        plain = "testme";
        String encodedSHA1 = Scheme.SHA1.generate(plain);
        String encodedSHA  = Scheme.SHA.generate(plain);

        result = Scheme.SHA1.verify(encodedSHA1, plain);
        System.out.println("result is " + (result?"good":"bad"));
        result = Scheme.SHA1.verify(encodedSHA, plain);
        System.out.println("result is " + (result?"good":"bad"));
    }
}
