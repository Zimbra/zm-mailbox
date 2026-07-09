/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for IRopcCredCache focusing on composite key isolation,
 * the IP Bridge logic for device ID changes, and MFA rejection tracking.
 */
public class IRopcCredCacheTest {

    // --- Constants for test context ---
    private static final String EMAIL = "user@test.com";

    private static final String PASSWORD = "password123";

    private static final String UA = "Outlook-iOS-Android/1.0 (com.microsoft.office.outlook/1.0.1)";

    private static final String PROTOCOL = "zsync";

    private static final String PROVIDER = "okta";

    private static final String IP = "192.168.1.100";

    private static final String DEVICE_ID = "fef5eb84";

    private static final long EXPIRY = 3600000L;

    @Before
    public void setUp() {
        // Ensure a fresh cache state for every test run
        IRopcCredCache.clearAll();
    }

    // ========================================================
    // 1. Basic Store and Validate
    // ========================================================

    @Test
    public void testStoreAndValidateSuccess() {
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        assertTrue("Cache should return true for valid credentials and context",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID));
    }

    @Test
    public void testWrongPasswordReturnsFalse() {
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        assertFalse("Cache should return false for incorrect password",
                IRopcCredCache.isValid(EMAIL, "wrongPass", UA, PROTOCOL, PROVIDER, IP, DEVICE_ID));
    }

    // ========================================================
    // 2. Key Isolation (UA HashCode & Context)
    // ========================================================

    @Test
    public void testDifferentUserAgentDoesNotMatch() {
        IRopcCredCache.store(EMAIL, PASSWORD, "Outlook-UA", PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        // Different UA should result in a different hashCode and a cache miss
        assertFalse(IRopcCredCache.isValid(EMAIL, PASSWORD, "Gmail-UA", PROTOCOL, PROVIDER, IP, DEVICE_ID));
    }

    @Test
    public void testEmailNormalization() {
        IRopcCredCache.store("  User@Test.Com  ", PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        assertTrue("Cache should handle email trimming and case-insensitivity",
                IRopcCredCache.isValid("user@test.com", PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID));
    }

    // ========================================================
    // 3. IP Bridge Logic (Scenario A: Missing Device ID)
    // ========================================================

    @Test
    public void testIpBridgeAndRealDeviceId() {
        // Step 1: App connects with no Device ID (e.g. Native iOS OPTIONS)
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, null, EXPIRY);

        // Step 2: App connects with real Device ID. System should bridge via IP.
        assertTrue("System should bridge the session using the IP address",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "real-device-id-123"));
    }

    // ========================================================
    // 4. IP Bridge Logic (Scenario B: Changing Device ID)
    // ========================================================

    @Test
    public void testIpBridgeOutlookDeviceIdChange() {
        // Step 1: Outlook sends temporary ID (OPCC...)
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "OPCC-TEMP-ID", EXPIRY);

        // Step 2: Outlook sends permanent ID (fef5...). System bridges via IP.
        assertTrue("System should bridge the session when Outlook changes Device ID",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "fef5-perm-id"));
    }

    @Test
    public void testIpBridgeRemovedAfterUpgrade() {
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "OPCC-TEMP-ID", EXPIRY);

        // First upgrade works
        assertTrue(IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "fef5-perm-id"));

        // The IP bridge should be deleted after one use. A third ID should fail.
        assertFalse("IP bridge should be removed after a successful upgrade",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "third-device-id"));
    }

    // ========================================================
    // 5. Rejection Tracking (Anti-Flood)
    // ========================================================

    @Test(expected = AuthFailedServiceException.class)
    public void testThirdRejectionBlocksRequest() throws Exception {
        IRopcCredCache.storeRejection(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);
        IRopcCredCache.storeRejection(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);
        IRopcCredCache.storeRejection(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);

        // Should throw AuthFailedServiceException due to limit reached
        IRopcCredCache.checkRejectionLimit(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);
    }

    @Test
    public void testSuccessfulLoginResetsRejections() throws Exception {
        IRopcCredCache.storeRejection(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);
        IRopcCredCache.storeRejection(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);

        // Successful login resets the counter
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);

        // Should not throw
        IRopcCredCache.checkRejectionLimit(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);
    }

    // ========================================================
    // 6. Invalidation
    // ========================================================

    @Test
    public void testInvalidateRemovesSession() {
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        IRopcCredCache.invalidate(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);

        assertFalse("Session should be removed after invalidation",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID));
    }
}
