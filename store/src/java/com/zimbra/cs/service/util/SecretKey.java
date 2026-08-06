
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2025 Synacor, Inc.
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

package com.zimbra.cs.service.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.mail.MessagingException;

public class SecretKey {

    public static final int KEY_SIZE_BYTES = 32;

    public static final String MSGVRFY_HEADER_PREFIX = "hash=SHA256;guid=";

    public static final String MSGVRFY_ALGORITHM_NAME = "SHA-256";

    public static final String HEX_64_ZERO_PADDED_FORMAT = "%064x";

    /**
     * returns the randomly generated String.
     *
     * @return randomly generated String
     * @throws ServiceException if an error occurred
     */
    public static String generateRandomString() throws ServiceException {
        try {
            SecureRandom random = new SecureRandom();
            byte[] key = new byte[KEY_SIZE_BYTES];
            random.nextBytes(key);
            return Base64.getEncoder().encodeToString(key);
        } catch (IllegalArgumentException e) {
            throw ServiceException.FAILURE("Illegal argument exception occurred during Base64 encoding", e);
        } catch (SecurityException e) {
            throw ServiceException.FAILURE("Security exception occurred while initializing SecureRandom", e);
        }
    }

    /**
     * Provide the Hash for Message-Verification header field.
     *
     * @param id the unique message identifier
     * @param date the date associated with the message
     * @param from the sender's email address
     * @return the computed hash value for the Message-Verification header field
     * @throws MessagingException if a messaging error occurs
     * @throws ServiceException if a service error occurs
     */
    public static String getMessageVerificationHeaderValue(String id, String date, String from)
            throws MessagingException, ServiceException {
        String secretKey = Provisioning.getInstance().getConfig().getFeatureMailRecallSecretKey();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(id);
        stringBuilder.append(date);
        stringBuilder.append(from);
        stringBuilder.append(secretKey);

        String guid = stringBuilder.toString();
        String guidHash = getHashForMessageVerification(guid);
        return MSGVRFY_HEADER_PREFIX.concat(guidHash);
    }

    /**
     * Create a digest of the given input with the given algorithm.
     *
     * @param input the input string to hash
     * @throws ServiceException if the hashing algorithm is not available
     * @return Base64-encoded hex string of the SHA digest of the input
     */
    private static String getHashForMessageVerification(String input) throws ServiceException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(MSGVRFY_ALGORITHM_NAME);
            byte[] hashBytes = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hashBytes);
            String hexString = String.format(HEX_64_ZERO_PADDED_FORMAT, number);
            return Base64.getEncoder().encodeToString(hexString.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw ServiceException.FAILURE("Unable to encrypt", e);
        }
    }
}
