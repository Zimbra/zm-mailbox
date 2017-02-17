/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.auth.twofactor;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Strings;
import com.zimbra.common.auth.twofactor.TwoFactorOptions.Encoding;
import com.zimbra.common.service.ServiceException;

/** Verify validity of a time-based one-time password for a given secret and point in time
 *
 * @author iraykin
 *
 */
public class TOTPAuthenticator {

    private AuthenticatorConfig config;

    public TOTPAuthenticator(AuthenticatorConfig config) {
        this.config = config;
    }

    public boolean validateCode(String secret, long timestamp, String code, Encoding encoding) throws ServiceException {
        return validateCode(decode(secret, encoding), timestamp, code);
    }

    private byte[] decode(String secret, Encoding encoding) throws ServiceException {
        byte[] decoded;
        switch (encoding) {
        case BASE32:
            decoded = new Base32().decode(secret);
            break;
        case BASE64:
            decoded = Base64.decodeBase64(secret);
            break;
        default:
            throw ServiceException.FAILURE("unknown encoding", new Throwable());
        }
        return decoded;
    }

    public boolean validateCode(byte[] secret, long timestamp, String code) throws ServiceException {
        long curWindow = getWindow(timestamp);
        int range = config.getWindowRange(); //will look ahead and behind this many windows
        for (int offset = -1 * range; offset <= range; offset++) {
            String actual = generateCodeInternal(secret, curWindow + offset);
            if (code.equals(actual)) {
                //success!
                return true;
            }
        }
        return false;
    }

    public String generateCode(String secret, long timestamp, Encoding encoding) throws ServiceException {
        return generateCode(decode(secret, encoding), timestamp);
    }

    private long getWindow(long timestamp) {
        return timestamp / config.getWindowSize();
    }

    public String generateCode(byte [] secret, long timestamp) throws ServiceException {
        return generateCodeInternal(secret, getWindow(timestamp));
    }

    private String generateCodeInternal(byte[] secret, long window) throws ServiceException {
        byte[] decodedTime = toBigEndian(window);
        return calculateHOTP(secret, decodedTime);
    }

    private static byte[] toBigEndian(long num) {
        byte[] data = new byte[8];
        for (int i = 8; i-- > 0; num >>>= 8) {
            data[i] = (byte) num;
        }
        return data;
    }

    private byte[] calculateHash(byte[] K, byte[] C) throws ServiceException {
        try {
            Mac mac = Mac.getInstance(config.getHashAlgorithm().getLabel());
            mac.init(new SecretKeySpec(K, config.getHashAlgorithm().getLabel()));
            byte[] hash = mac.doFinal(C);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("no such algorithm", e);
        } catch (InvalidKeyException e) {
            throw ServiceException.FAILURE("invalid key", e);
        }
    }

    private static int dynamicTruncate(byte[] hash) {
        int offset = hash[hash.length - 1] & 0xF;
        int code = 0;
        for (int i = 0; i < 4; i++) {
            byte b = hash[offset + i];
            code <<= 8;
            int value = b & 0xFF;
            code |= value;
        }
        code &= 0x7FFFFFFF;
        return code;
    }

    private int generateFinalCode(int truncatedCode) {
        return truncatedCode % (int) Math.pow(10, config.getNumCodeDigits());
    }

    /* Implements HOTP algorithm as per RFC 4226 section 5.3 */
    public String calculateHOTP(byte[] K, byte[] C) throws ServiceException {
        //step 1
        byte[] hash = calculateHash(K, C);
        //step 2
        int truncated = dynamicTruncate(hash);
        //step 3
        int code = generateFinalCode(truncated);

        String codeString = String.valueOf(code);
        int numDigits = config.getNumCodeDigits();
        if (codeString.length() == numDigits) {
            return codeString;
        } else {
            //pad with zeros
            return Strings.repeat("0", numDigits - codeString.length()) + codeString;
        }
    }
}