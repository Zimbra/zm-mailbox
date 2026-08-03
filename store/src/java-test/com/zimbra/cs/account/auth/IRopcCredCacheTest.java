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

import com.google.common.cache.CacheBuilder;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import com.zimbra.cs.account.auth.ropc.store.CacheRopcTokenStore;
import com.zimbra.cs.account.auth.ropc.store.IRopcSessionRecord;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

/**
 * Unit tests for IRopcCredCache focusing on composite key isolation,
 * the IP Bridge logic for device ID changes, and MFA rejection tracking.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LC.class, KnownKey.class, IRopcCredCache.class, PasswordUtil.class, PasswordUtil.SSHA512.class})
@SuppressStaticInitializationFor({"com.zimbra.common.localconfig.LC", "com.zimbra.cs.account.auth.ropc.IRopcCredCache"})
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

    private CacheRopcTokenStore store;

    @Before
    public void setUp() throws Exception {
        Field lcFiled = LC.class.getField("mfa_idp_hard_reauth_in_days");
        Object mockLcConfig = PowerMockito.mock(lcFiled.getType());
        Whitebox.setInternalState(LC.class, "mfa_idp_hard_reauth_in_days", mockLcConfig);

        Field lcFiled2 = LC.class.getField("mfa_idp_auth_fail_count");
        Object mockLcConfig2 = PowerMockito.mock(lcFiled2.getType());
        Whitebox.setInternalState(LC.class, "mfa_idp_auth_fail_count", mockLcConfig2);

        store = new CacheRopcTokenStore();

        Whitebox.setInternalState(IRopcCredCache.class, "CRED_CACHE", CacheBuilder.newBuilder().build());
        Whitebox.setInternalState(IRopcCredCache.class, "REJECTION_CACHE", CacheBuilder.newBuilder().build());

        Whitebox.setInternalState(IRopcCredCache.class, "STORE", store);


        Whitebox.setInternalState(IRopcCredCache.class, "GRACE_PERIOD", 60000L);
        Whitebox.setInternalState(IRopcCredCache.class, "CACHE_TIMEOUT", 3600000L);
        Whitebox.setInternalState(IRopcCredCache.class, "REJECTION_CACHE_TIMEOUT", 3600000L);

        // Ensure a fresh cache state for every test run
        IRopcCredCache.clearAll();

        PowerMockito.spy(IRopcCredCache.class);
        PowerMockito.doReturn(true).when(IRopcCredCache.class, "isMatch", any(), anyString(), anyString());

        PowerMockito.mockStatic(PasswordUtil.SSHA512.class);
        PowerMockito.when(PasswordUtil.SSHA512.verifySSHA512(any(), any())).thenReturn(true);


    }

    // ========================================================
    // 1. Basic Store and Validate
    // ========================================================

    @Test
    public void testStoreAndValidateSuccess() throws ServiceException {
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        assertTrue("Cache should return true for valid credentials and context",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, null,
                        false).isCacheHit());
    }

    @Test
    public void testWrongPasswordReturnsFalse() throws Exception {
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        PowerMockito.doReturn(false).when(IRopcCredCache.class, "isMatch", any(), anyString(), anyString());
        assertFalse("Cache should return false for incorrect password",
                IRopcCredCache.isValid(EMAIL, "wrongPass", UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, null,
                false).isCacheHit());
    }

    // ========================================================
    // 2. Key Isolation (UA HashCode & Context)
    // ========================================================

    @Test
    public void testDifferentUserAgentDoesNotMatch() throws ServiceException {
        IRopcCredCache.store(EMAIL, PASSWORD, "Outlook-UA", PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        // Different UA should result in a different hashCode and a cache miss
        assertFalse(IRopcCredCache.isValid(EMAIL, PASSWORD, "Gmail-UA", PROTOCOL, PROVIDER, IP, DEVICE_ID, null,
                false).isCacheHit());
    }

    @Test
    public void testEmailNormalization() throws ServiceException {
        IRopcCredCache.store("  User@Test.Com  ", PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        assertTrue("Cache should handle email trimming and case-insensitivity",
                IRopcCredCache.isValid("user@test.com", PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, null,
                        false).isCacheHit());
    }

    // ========================================================
    // 3. IP Bridge Logic (Scenario A: Missing Device ID)
    // ========================================================

    @Test
    public void testIpBridgeAndRealDeviceId() throws ServiceException {
        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username(EMAIL)
                .userAgent(UA)
                .protocol(PROTOCOL)
                .provider(PROVIDER)
                .passwordHash(PASSWORD)
                .ip(IP)
                .build();

        IRopcCredCache.STORE.upsert(null, session);
        // Step 1: App connects with no Device ID (e.g. Native iOS OPTIONS)
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, null, EXPIRY);

        // Step 2: App connects with real Device ID. System should bridge via IP.
        assertTrue("System should bridge the session using the IP address",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "real-device-id-123", null,
                false).isCacheHit());
    }

    // ========================================================
    // 4. IP Bridge Logic (Scenario B: Changing Device ID)
    // ========================================================

    @Test
    public void testIpBridgeOutlookDeviceIdChange() throws ServiceException {
        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username(EMAIL)
                .userAgent(UA)
                .protocol(PROTOCOL)
                .provider(PROVIDER)
                .passwordHash(PASSWORD)
                .ip(IP)
                .build();

        IRopcCredCache.STORE.upsert(null, session);
        // Step 1: Outlook sends temporary ID (OPCC...)
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "OPCC-TEMP-ID", EXPIRY);

        // Step 2: Outlook sends permanent ID (fef5...). System bridges via IP.
        assertTrue("System should bridge the session when Outlook changes Device ID",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, "fef5-perm-id", null,
                        false).isCacheHit());
    }

    // ========================================================
    // 5. Rejection Tracking (Anti-Flood)
    // ========================================================

    @Test(expected = AuthFailedServiceException.class)
    public void testEleventhRejectionBlocksRequest() throws Exception {
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);

        // Should throw AuthFailedServiceException due to limit reached
        IRopcCredCache.checkRejectionLimit(EMAIL);
    }

    @Test(expected = AuthFailedServiceException.class)
    public void testSuccessfulLoginDoesNotResetsRejections() throws Exception {
        IRopcCredCache.storeRejection(EMAIL);
        IRopcCredCache.storeRejection(EMAIL);

        // Successful login resets the counter
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);

        // Should throw
        IRopcCredCache.checkRejectionLimit(EMAIL);
    }

    // ========================================================
    // 6. Invalidation
    // ========================================================

    @Test
    public void testInvalidateRemovesSession() throws ServiceException {
        IRopcCredCache.store(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, EXPIRY);
        IRopcCredCache.invalidate(EMAIL, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID);

        assertFalse("Session should be removed after invalidation",
                IRopcCredCache.isValid(EMAIL, PASSWORD, UA, PROTOCOL, PROVIDER, IP, DEVICE_ID, null,
                        false).isCacheHit());
    }
}
