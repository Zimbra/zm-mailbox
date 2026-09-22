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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link CsrfTokenKey}: key generation, encode/decode round trip,
 * version/created accessors, the bootstrap {@code getCurrentKey()} flow against the
 * in-memory provisioning config, and {@code getVersion()} cache lookups.
 */
public class CsrfTokenKeyTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    /* Build a CsrfTokenKey from its encoded "version:created:hexdata" form via the private ctor. */
    private static CsrfTokenKey fromEncoded(String encoded) throws Exception {
        Constructor<CsrfTokenKey> ctor = CsrfTokenKey.class.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        try {
            return ctor.newInstance(encoded);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ServiceException) {
                throw (ServiceException) e.getCause();
            }
            throw e;
        }
    }

    @Test
    public void constructorNullKeyGeneratesRandomKeyOfExpectedSize() throws Exception {
        // Act
        CsrfTokenKey key = new CsrfTokenKey(7L, null);

        // Assert — random key material, full size, version stored, timestamp set
        assertNotNull("generated key bytes must not be null", key.getKey());
        assertEquals(CsrfTokenKey.KEY_SIZE_BYTES, key.getKey().length);
        assertEquals(7L, key.getVersion());
        assertTrue("created timestamp should be set", key.getCreated() > 0);
    }

    @Test
    public void constructorProvidedKeyRetainsExactBytes() throws Exception {
        // Arrange
        byte[] raw = new byte[] {1, 2, 3, 4, 5};

        // Act
        CsrfTokenKey key = new CsrfTokenKey(3L, raw);

        // Assert
        assertArrayEquals(raw, key.getKey());
        assertEquals(3L, key.getVersion());
    }

    @Test
    public void setKeyReplacesKeyMaterial() throws Exception {
        // Arrange
        CsrfTokenKey key = new CsrfTokenKey(1L, null);
        byte[] replacement = new byte[] {9, 8, 7};

        // Act
        key.setKey(replacement);

        // Assert
        assertArrayEquals(replacement, key.getKey());
    }

    @Test
    public void getEncodedThenDecodeRoundTripsAllFields() throws Exception {
        // Arrange — a known key
        byte[] raw = new byte[] {0x10, 0x20, 0x30, 0x40};
        CsrfTokenKey original = new CsrfTokenKey(42L, raw);

        // Act — encode then rebuild from the encoded string
        String encoded = original.getEncoded();
        CsrfTokenKey decoded = fromEncoded(encoded);

        // Assert — version, created, and key bytes survive the round trip
        assertEquals(original.getVersion(), decoded.getVersion());
        assertEquals(original.getCreated(), decoded.getCreated());
        assertArrayEquals(original.getKey(), decoded.getKey());
    }

    @Test
    public void getEncodedFormatIsVersionColonCreatedColonHex() throws Exception {
        // Arrange
        CsrfTokenKey key = new CsrfTokenKey(8L, new byte[] {(byte) 0xAB, (byte) 0xCD});

        // Act
        String encoded = key.getEncoded();
        String[] parts = encoded.split(":");

        // Assert — three colon-separated fields with the version first
        assertEquals(3, parts.length);
        assertEquals("8", parts[0]);
        assertEquals(Long.toString(key.getCreated()), parts[1]);
        assertEquals("abcd", parts[2].toLowerCase());
    }

    @Test
    public void decodeWrongPartCountThrowsInvalidRequest() {
        // Act + Assert — only two colon-separated parts
        try {
            fromEncoded("1:2");
            fail("expected ServiceException for malformed key string");
        } catch (Exception e) {
            assertTrue(e instanceof ServiceException);
            assertEquals(ServiceException.INVALID_REQUEST, ((ServiceException) e).getCode());
        }
    }

    @Test
    public void decodeNonNumericVersionThrowsInvalidRequest() {
        // Act + Assert
        try {
            fromEncoded("notalong:123:abcd");
            fail("expected ServiceException for bad version");
        } catch (Exception e) {
            assertTrue(e instanceof ServiceException);
            assertTrue(e.getMessage().toLowerCase().contains("version"));
        }
    }

    @Test
    public void decodeNonNumericCreatedThrowsInvalidRequest() {
        // Act + Assert
        try {
            fromEncoded("1:notalong:abcd");
            fail("expected ServiceException for bad created");
        } catch (Exception e) {
            assertTrue(e instanceof ServiceException);
            assertTrue(e.getMessage().toLowerCase().contains("created"));
        }
    }

    @Test
    public void decodeInvalidHexDataThrowsInvalidRequest() {
        // Act + Assert — "zz" is not valid hex
        try {
            fromEncoded("1:2:zz");
            fail("expected ServiceException for bad hex data");
        } catch (Exception e) {
            assertTrue(e instanceof ServiceException);
            assertTrue(e.getMessage().toLowerCase().contains("data"));
        }
    }

    @Test
    public void getCurrentKeyBootstrapsKeyFromConfig() throws Exception {
        // Act — no key configured initially; getCurrentKey must bootstrap one into config
        CsrfTokenKey current = CsrfTokenKey.getCurrentKey();

        // Assert — a real key exists and is internally consistent
        assertNotNull("getCurrentKey must bootstrap a key", current);
        assertNotNull(current.getKey());
        assertEquals(CsrfTokenKey.KEY_SIZE_BYTES, current.getKey().length);
    }

    @Test
    public void getCurrentKeyCalledTwiceReturnsSameCachedKey() throws Exception {
        // Act — second call must reuse the already-bootstrapped latest key
        CsrfTokenKey first = CsrfTokenKey.getCurrentKey();
        CsrfTokenKey second = CsrfTokenKey.getCurrentKey();

        // Assert — same instance, identical key material
        assertNotNull(first);
        assertEquals(first.getVersion(), second.getVersion());
        assertArrayEquals(first.getKey(), second.getKey());
    }

    @Test
    public void getVersionCurrentKeyVersionReturnsSameKey() throws Exception {
        // Arrange — ensure the cache is populated
        CsrfTokenKey current = CsrfTokenKey.getCurrentKey();

        // Act — look it up by its numeric version string
        CsrfTokenKey byVersion = CsrfTokenKey.getVersion(Long.toString(current.getVersion()));

        // Assert — same key material returned from cache
        assertNotNull("known version must resolve", byVersion);
        assertArrayEquals(current.getKey(), byVersion.getKey());
        assertEquals(current.getVersion(), byVersion.getVersion());
    }

    @Test
    public void getVersionUnknownVersionReturnsNull() throws Exception {
        // Act — a version that was never created
        CsrfTokenKey result = CsrfTokenKey.getVersion("99999999");

        // Assert
        assertNull("unknown version must resolve to null after refresh attempts", result);
    }
}
