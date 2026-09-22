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
 * Functional tests for {@link TrustedTokenKey}: key generation, encode/decode round trip,
 * accessors, the bootstrap {@code getCurrentKey()} flow against in-memory provisioning config,
 * and {@code getVersion()} cache lookups.
 */
public class TrustedTokenKeyTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    private static TrustedTokenKey fromEncoded(String encoded) throws Exception {
        Constructor<TrustedTokenKey> ctor = TrustedTokenKey.class.getDeclaredConstructor(String.class);
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
        TrustedTokenKey key = new TrustedTokenKey(5L, null);

        // Assert
        assertNotNull(key.getKey());
        assertEquals(TrustedTokenKey.KEY_SIZE_BYTES, key.getKey().length);
        assertEquals(5L, key.getVersion());
        assertTrue("created timestamp should be set", key.getCreated() > 0);
    }

    @Test
    public void constructorProvidedKeyRetainsExactBytes() throws Exception {
        // Arrange
        byte[] raw = new byte[] {11, 22, 33 };

        // Act
        TrustedTokenKey key = new TrustedTokenKey(2L, raw);

        // Assert
        assertArrayEquals(raw, key.getKey());
        assertEquals(2L, key.getVersion());
    }

    @Test
    public void setKeyReplacesKeyMaterial() throws Exception {
        // Arrange
        TrustedTokenKey key = new TrustedTokenKey(1L, null);
        byte[] replacement = new byte[] {4, 5, 6 };

        // Act
        key.setKey(replacement);

        // Assert
        assertArrayEquals(replacement, key.getKey());
    }

    @Test
    public void getEncodedThenDecodeRoundTripsAllFields() throws Exception {
        // Arrange
        byte[] raw = new byte[] {0x0A, 0x0B, 0x0C, 0x0D };
        TrustedTokenKey original = new TrustedTokenKey(13L, raw);

        // Act
        String encoded = original.getEncoded();
        TrustedTokenKey decoded = fromEncoded(encoded);

        // Assert
        assertEquals(original.getVersion(), decoded.getVersion());
        assertEquals(original.getCreated(), decoded.getCreated());
        assertArrayEquals(original.getKey(), decoded.getKey());
    }

    @Test
    public void decodeWrongPartCountThrowsInvalidRequest() {
        // Act + Assert
        try {
            fromEncoded("onlyonepart");
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
            fromEncoded("xx:123:abcd");
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
            fromEncoded("1:xx:abcd");
            fail("expected ServiceException for bad created");
        } catch (Exception e) {
            assertTrue(e instanceof ServiceException);
            assertTrue(e.getMessage().toLowerCase().contains("created"));
        }
    }

    @Test
    public void decodeInvalidHexDataThrowsInvalidRequest() {
        // Act + Assert
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
        // Act
        TrustedTokenKey current = TrustedTokenKey.getCurrentKey();

        // Assert
        assertNotNull("getCurrentKey must bootstrap a key", current);
        assertNotNull(current.getKey());
        assertEquals(TrustedTokenKey.KEY_SIZE_BYTES, current.getKey().length);
    }

    @Test
    public void getVersionCurrentKeyVersionReturnsSameKey() throws Exception {
        // Arrange
        TrustedTokenKey current = TrustedTokenKey.getCurrentKey();

        // Act
        TrustedTokenKey byVersion = TrustedTokenKey.getVersion(Long.toString(current.getVersion()));

        // Assert
        assertNotNull("known version must resolve", byVersion);
        assertArrayEquals(current.getKey(), byVersion.getKey());
        assertEquals(current.getVersion(), byVersion.getVersion());
    }

    @Test
    public void getVersionUnknownVersionReturnsNull() throws Exception {
        // Act
        TrustedTokenKey result = TrustedTokenKey.getVersion("88888888");

        // Assert
        assertNull("unknown version must resolve to null after refresh attempts", result);
    }
}
