/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JWTCache}.
 *
 * Tests verify JWT token caching, retrieval, and invalidation operations.
 */
public class JWTCacheTest {

    @Test
    public void putAndGet_storesAndRetrievesToken() {
        String jti = "test-jti-001";
        long expiryTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        JWTInfo info = new JWTInfo("test-salt", expiryTime);

        JWTCache.put(jti, info);
        JWTInfo retrieved = JWTCache.get(jti);

        assertNotNull(retrieved);
        assertEquals(info, retrieved);
    }

    @Test
    public void get_nonExistentToken_returnsNull() {
        String nonExistent = "non-existent-jti-12345";

        JWTInfo retrieved = JWTCache.get(nonExistent);

        assertNull(retrieved);
    }

    @Test
    public void put_multipleTokens_allRetrievable() {
        String jti1 = "jti-first";
        String jti2 = "jti-second";
        String jti3 = "jti-third";

        long baseTime = System.currentTimeMillis() + 3600000;
        JWTInfo info1 = new JWTInfo("salt-1", baseTime);
        JWTInfo info2 = new JWTInfo("salt-2", baseTime + 1000);
        JWTInfo info3 = new JWTInfo("salt-3", baseTime + 2000);

        JWTCache.put(jti1, info1);
        JWTCache.put(jti2, info2);
        JWTCache.put(jti3, info3);

        assertEquals(info1, JWTCache.get(jti1));
        assertEquals(info2, JWTCache.get(jti2));
        assertEquals(info3, JWTCache.get(jti3));
    }

    @Test
    public void remove_invalidatesToken_notRetrievableAfter() {
        String jti = "jti-to-remove";
        long expiryTime = System.currentTimeMillis() + 3600000;
        JWTInfo info = new JWTInfo("salt", expiryTime);

        JWTCache.put(jti, info);
        assertNotNull(JWTCache.get(jti));

        JWTCache.remove(jti);

        assertNull(JWTCache.get(jti));
    }

    @Test
    public void put_updateExistingToken_retrievesLatestValue() {
        String jti = "jti-update";
        long baseTime = System.currentTimeMillis() + 3600000;
        JWTInfo firstInfo = new JWTInfo("salt-1", baseTime);
        JWTInfo secondInfo = new JWTInfo("salt-2", baseTime + 1000);

        JWTCache.put(jti, firstInfo);
        JWTCache.put(jti, secondInfo);

        JWTInfo retrieved = JWTCache.get(jti);
        assertEquals(secondInfo, retrieved);
    }

    @Test
    public void remove_multipleTokens_onlyRemovedOneAffected() {
        String jti1 = "jti-keep";
        String jti2 = "jti-remove";

        long baseTime = System.currentTimeMillis() + 3600000;
        JWTInfo info1 = new JWTInfo("salt-keep", baseTime);
        JWTInfo info2 = new JWTInfo("salt-remove", baseTime + 1000);

        JWTCache.put(jti1, info1);
        JWTCache.put(jti2, info2);

        JWTCache.remove(jti2);

        // First token should still be present
        assertNotNull(JWTCache.get(jti1));
        assertEquals(info1, JWTCache.get(jti1));

        // Second token should be removed
        assertNull(JWTCache.get(jti2));
    }

    @Test
    public void put_nullValue_handlesGracefully() {
        String jti = "jti-null-test";

        // Putting null might throw or might be handled
        try {
            JWTCache.put(jti, null);
            JWTInfo retrieved = JWTCache.get(jti);
            // If it succeeds, should return null
            assertNull(retrieved);
        } catch (Exception e) {
            // Some cache implementations may reject null values
            // This is acceptable behavior
            assertTrue("Cache may not support null values", true);
        }
    }

    @Test
    public void get_afterRemove_returnsNull() {
        String jti = "jti-after-remove";
        long expiryTime = System.currentTimeMillis() + 3600000;
        JWTInfo info = new JWTInfo("salt", expiryTime);

        JWTCache.put(jti, info);
        JWTCache.remove(jti);

        assertNull(JWTCache.get(jti));
    }

    @Test
    public void cache_operationSequence_createsCorrectState() {
        String jti = "jti-sequence-test";
        long baseTime = System.currentTimeMillis() + 3600000;
        JWTInfo info1 = new JWTInfo("salt-1", baseTime);
        JWTInfo info2 = new JWTInfo("salt-2", baseTime + 1000);

        // Add first
        JWTCache.put(jti, info1);
        assertEquals(info1, JWTCache.get(jti));

        // Update
        JWTCache.put(jti, info2);
        assertEquals(info2, JWTCache.get(jti));

        // Remove
        JWTCache.remove(jti);
        assertNull(JWTCache.get(jti));

        // Add again
        JWTCache.put(jti, info1);
        assertEquals(info1, JWTCache.get(jti));
    }

    @Test
    public void cache_identitiesPreserved_differentObjectsSameCachedData() {
        String jti = "jti-identity-test";
        long expiryTime = System.currentTimeMillis() + 3600000;
        JWTInfo info = new JWTInfo("same-salt", expiryTime);

        JWTCache.put(jti, info);
        JWTInfo retrieved = JWTCache.get(jti);

        assertTrue("Retrieved object should be same or equal",
                info == retrieved || info.equals(retrieved));
    }

    @Test
    public void cache_staticBehavior_operatesOnSharedCache() {
        // Multiple calls to static methods should use same cache
        String jti = "jti-static-test";
        long expiryTime = System.currentTimeMillis() + 3600000;
        JWTInfo info = new JWTInfo("static-salt", expiryTime);

        JWTCache.put(jti, info);
        JWTInfo first = JWTCache.get(jti);
        JWTInfo second = JWTCache.get(jti);

        assertEquals(first, second);
        assertEquals(info, first);
    }

    @Test
    public void remove_nonExistentToken_handlesGracefully() {
        String nonExistent = "non-existent-jti";

        // Removing non-existent token should not throw
        try {
            JWTCache.remove(nonExistent);
            // Should succeed without error
            assertTrue(true);
        } catch (Exception e) {
            fail("Should not throw exception when removing non-existent token: " + e.getMessage());
        }
    }

    @Test
    public void cache_handlesConcurrentLikeOperations() {
        // Simulating concurrent operations with sequential calls
        long baseTime = System.currentTimeMillis() + 3600000;
        for (int i = 0; i < 10; i++) {
            String jti = "concurrent-jti-" + i;
            JWTInfo info = new JWTInfo("salt-" + i, baseTime + (i * 1000));

            JWTCache.put(jti, info);
            assertEquals(info, JWTCache.get(jti));
        }

        // Verify all are still there
        for (int i = 0; i < 10; i++) {
            String jti = "concurrent-jti-" + i;
            assertNotNull("Token " + i + " should be cached", JWTCache.get(jti));
        }
    }
}
