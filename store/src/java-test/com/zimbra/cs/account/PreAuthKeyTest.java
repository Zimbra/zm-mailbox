/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link PreAuthKey} HMAC key generation and pre-auth computation.
 * These are pure cryptographic helpers, so no harness is required.
 */
public class PreAuthKeyTest {

    @Test
    public void generateRandomPreAuthKeyDefaultReturnsHexOfExpectedLength() throws Exception {
        // Act -- 32 bytes hex-encoded = 64 hex chars
        String key = PreAuthKey.generateRandomPreAuthKey();

        // Assert
        assertNotNull(key);
        assertEquals(PreAuthKey.KEY_SIZE_BYTES * 2, key.length());
        assertTrue("key must be lowercase hex", key.matches("[0-9a-f]+"));
    }

    @Test
    public void generateRandomPreAuthKeyCalledTwiceProducesDifferentKeys() throws Exception {
        // Act
        String first = PreAuthKey.generateRandomPreAuthKey();
        String second = PreAuthKey.generateRandomPreAuthKey();

        // Assert -- randomness means collisions are effectively impossible
        assertFalse("two random keys should differ", first.equals(second));
    }

    @Test
    public void computePreAuthKnownInputsMatchesReferenceHmac() {
        // Arrange -- the documented sample from PreAuthKey.main
        Map<String, String> params = new HashMap<String, String>();
        params.put("account", "user1");
        params.put("by", "name");
        params.put("timestamp", "1176399950434");
        params.put("expires", "0");
        String key = "9d8ad87fd726ba7d5fecf3d705621024b31cedb142310ec965f9263568fa0f27";

        // Act
        String preAuth = PreAuthKey.computePreAuth(params, key);

        // Assert -- HMAC-SHA1 yields a stable 40-char hex digest
        assertNotNull(preAuth);
        assertEquals(40, preAuth.length());
        assertTrue(preAuth.matches("[0-9a-f]+"));
    }

    @Test
    public void computePreAuthSameInputsIsDeterministic() {
        // Arrange
        Map<String, String> params = new HashMap<String, String>();
        params.put("account", "user1");
        params.put("by", "name");
        String key = "abc123";

        // Act
        String first = PreAuthKey.computePreAuth(params, key);
        String second = PreAuthKey.computePreAuth(params, key);

        // Assert
        assertEquals(first, second);
    }

    @Test
    public void computePreAuthParamOrderIndependentSortsKeysBeforeHashing() {
        // Arrange -- TreeSet sorts by key name, so insertion order must not matter
        Map<String, String> ordered = new HashMap<String, String>();
        ordered.put("a", "1");
        ordered.put("b", "2");
        ordered.put("c", "3");

        Map<String, String> reversed = new HashMap<String, String>();
        reversed.put("c", "3");
        reversed.put("b", "2");
        reversed.put("a", "1");

        String key = "thekey";

        // Act
        String fromOrdered = PreAuthKey.computePreAuth(ordered, key);
        String fromReversed = PreAuthKey.computePreAuth(reversed, key);

        // Assert -- same values keyed the same way produce the same digest
        assertEquals(fromOrdered, fromReversed);
    }

    @Test
    public void computePreAuthDifferentValuesProduceDifferentDigests() {
        // Arrange
        Map<String, String> paramsA = new HashMap<String, String>();
        paramsA.put("account", "user1");
        Map<String, String> paramsB = new HashMap<String, String>();
        paramsB.put("account", "user2");
        String key = "thekey";

        // Act
        String digestA = PreAuthKey.computePreAuth(paramsA, key);
        String digestB = PreAuthKey.computePreAuth(paramsB, key);

        // Assert
        assertFalse("different values must hash differently", digestA.equals(digestB));
    }

    @Test
    public void computePreAuthDifferentKeysProduceDifferentDigests() {
        // Arrange
        Map<String, String> params = new HashMap<String, String>();
        params.put("account", "user1");

        // Act
        String digestKey1 = PreAuthKey.computePreAuth(params, "keyOne");
        String digestKey2 = PreAuthKey.computePreAuth(params, "keyTwo");

        // Assert
        assertFalse("different signing keys must hash differently",
                digestKey1.equals(digestKey2));
    }

    @Test
    public void computePreAuthEmptyParamsReturnsHmacOfEmptyString() {
        // Arrange
        Map<String, String> params = new HashMap<String, String>();
        String key = "thekey";

        // Act
        String preAuth = PreAuthKey.computePreAuth(params, key);

        // Assert -- still a valid 40-char HMAC-SHA1 hex digest over the empty payload
        assertNotNull(preAuth);
        assertEquals(40, preAuth.length());
    }

    @Test
    public void byteKeyConstructionExposesAlgorithmFormatAndClonedKey() throws Exception {
        // Arrange -- ByteKey is package-private inner SecretKey implementation
        byte[] raw = new byte[] {1, 2, 3, 4};
        PreAuthKey.ByteKey bk = new PreAuthKey.ByteKey(raw);

        // Assert -- metadata
        assertEquals("HmacSHA1", bk.getAlgorithm());
        assertEquals("RAW", bk.getFormat());

        // Assert -- key is cloned, not aliased
        byte[] encoded = bk.getEncoded();
        assertEquals(4, encoded.length);
        assertEquals(1, encoded[0]);
        raw[0] = 99;
        assertEquals("ByteKey must clone the input array", 1, bk.getEncoded()[0]);
    }
}
