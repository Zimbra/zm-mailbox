/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
package com.zimbra.cs.account;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for {@link PreAuthKey}.
 *
 * Tests verify pre-authentication key management and usage.
 */
public class PreAuthKeyTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    /**
     * Test: Generate pre-auth key → verify created.
     * Verifies: Key generation succeeds.
     */
    @Test
    public void generateKey_succeeds() throws Exception {
        // Act
        String key = PreAuthKey.generateRandomPreAuthKey();

        // Assert
        Assert.assertNotNull(key);
    }

    /**
     * Test: Generate pre-auth key → get value → verify non-null.
     * Verifies: Key value is set.
     */
    @Test
    public void getKey_returnsValue() throws Exception {
        // Act
        String value = PreAuthKey.generateRandomPreAuthKey();

        // Assert
        Assert.assertNotNull(value);
        Assert.assertTrue(value.length() > 0);
    }

    /**
     * Test: Generate pre-auth key → use in computation.
     * Verifies: Key can be used for HMAC computation.
     */
    @Test
    public void computePreAuth_withKey_succeeds() throws Exception {
        // Arrange
        String key = PreAuthKey.generateRandomPreAuthKey();
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("account", "user1");
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));

        // Act
        String preAuth = PreAuthKey.computePreAuth(params, key);

        // Assert
        Assert.assertNotNull(preAuth);
        Assert.assertTrue(preAuth.length() > 0);
    }

    /**
     * Test: Generate two keys → verify different.
     * Verifies: Each key is unique.
     */
    @Test
    public void twoKeys_areDifferent() throws Exception {
        // Act
        String key1 = PreAuthKey.generateRandomPreAuthKey();
        String key2 = PreAuthKey.generateRandomPreAuthKey();

        // Assert
        Assert.assertNotEquals(key1, key2);
    }

    /**
     * Test: Pre-auth key is non-empty.
     * Verifies: Key has content.
     */
    @Test
    public void key_isNonEmpty() throws Exception {
        // Act
        String key = PreAuthKey.generateRandomPreAuthKey();

        // Assert
        Assert.assertTrue(key.length() > 0);
    }

    /**
     * Test: Pre-auth key generation is random.
     * Verifies: Keys are different each time.
     */
    @Test
    public void keyGeneration_isRandom_succeeds() throws Exception {
        // Act
        String key1 = PreAuthKey.generateRandomPreAuthKey();
        String key2 = PreAuthKey.generateRandomPreAuthKey();

        // Assert - keys should be different
        Assert.assertNotEquals(key1, key2);
    }
}
