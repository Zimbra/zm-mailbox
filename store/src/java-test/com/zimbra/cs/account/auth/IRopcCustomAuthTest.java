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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.ropc.CacheResponse;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import com.zimbra.cs.account.auth.ropc.Outcome;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static com.zimbra.cs.account.auth.AuthContext.AC_DEVICE_ID;
import static com.zimbra.cs.account.auth.AuthContext.AC_ORIGINATING_CLIENT_IP;
import static com.zimbra.cs.account.auth.AuthContext.AC_PROTOCOL;
import static com.zimbra.cs.account.auth.AuthContext.AC_SUB_PROTOCOL;
import static com.zimbra.cs.account.auth.AuthContext.AC_USER_AGENT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IRopcCustomAuth.class, IRopcCredCache.class})
public class IRopcCustomAuthTest {

    private IRopcCustomAuth ropcCustomAuth;

    private Account mockAccount;

    private Map<String, Object> mockContext;

    private Map<String, String> validConfig;

    private List<String> args = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        IRopcCustomAuth target = new IRopcCustomAuth();
        ropcCustomAuth = PowerMockito.spy(target);
        MailboxTestUtil.initProvisioning();
        IRopcCredCache.clearAll();

        Provisioning prov = Provisioning.getInstance();

        // Create domain FIRST with zimbraAuthMech — shouldSkipEasPassCache reads from domain
        HashMap<String, Object> domainAttrs = new HashMap<>();
        domainAttrs.put(Provisioning.A_zimbraAuthMech, "custom:idp-ropc");
        prov.createDomain("example.zimbra.com", domainAttrs);

        // Create account in that domain
        HashMap<String, Object> attrs = new HashMap<>();
        prov.createAccount("user1@example.zimbra.com", "password", attrs);
        mockAccount = prov.get(Key.AccountBy.name, "user1@example.zimbra.com");

        mockContext = new HashMap<String, Object>();
        mockContext.put(AC_DEVICE_ID, "device-123");
        mockContext.put(AC_PROTOCOL, AuthContext.Protocol.zsync);
        mockContext.put(AC_SUB_PROTOCOL, AuthContext.SubProtocol.eas);
        mockContext.put(AC_ORIGINATING_CLIENT_IP, "1234425");
        mockContext.put(AC_USER_AGENT, "userAgent");

        validConfig = new HashMap<>();
        validConfig.put("token_endpoint", "https://okta.test/token");
        validConfig.put("client_id", "daljncvdalnc");
        validConfig.put("provider", "okta");

        args.add("token_endpoint=https://okta.test/token");
        args.add("client_id=daljncvdalnc");
        args.add("provider=okta");

        CacheResponse mockCacheResponse = PowerMockito.mock(CacheResponse.class);
        PowerMockito.when(mockCacheResponse.isCacheHit()).thenReturn(false);
        PowerMockito.when(mockCacheResponse.getRejectionSkip()).thenReturn(true);
        PowerMockito.when(mockCacheResponse.getAuthType()).thenReturn("SOME_AUTH");

        PowerMockito.doReturn(mockCacheResponse).when(ropcCustomAuth, "checkInCache",
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyBoolean());


    }

    @Test
    public void testCLassIsInstantiated() {
        assertNotNull("IRopcCustomAuthInstance should not be null", ropcCustomAuth);
    }

    @Test
    public void testAuthenticateValidCallDoesnotThrowException() {
        doReturn(Outcome.SUCCESS).when(ropcCustomAuth)
                .callAuthEngine(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(),
                        anyMapOf(String.class, String.class), anyBoolean());
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
        } catch (Exception e) {
            fail("Auth should not throw a exception: " + e.getMessage());
        }
    }

    @Test
    public void testCeckPasswordAgingReturnsFalse() {
        assertFalse("Password aging should return false", ropcCustomAuth.checkPasswordAging());
    }

    @Test
    public void testAuthenticateOutcomeSuccess() throws Exception {
        doReturn(Outcome.SUCCESS).when(ropcCustomAuth)
                .callAuthEngine(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(),
                        anyMapOf(String.class, String.class), anyBoolean());

        ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);

        verify(ropcCustomAuth, times(1)).callAuthEngine(mockAccount, "user1@example.zimbra.com",
                "password", "device-123",
                AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas, "userAgent", "1234425", validConfig, false);
    }

    @Test
    public void testAuthenticateOutcomeInvalid() throws Exception {
        doReturn(Outcome.INVALID).when(ropcCustomAuth)
                .callAuthEngine(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(),
                        anyMapOf(String.class, String.class), anyBoolean());
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
            fail("Expected AuthFailedServiceException was not thrown");
        } catch (AccountServiceException.AuthFailedServiceException e) {
            assertTrue("Message should indicate rejected credentials",
                    e.getMessage().contains("Authentication failed : Invalid credentials provided"));
        }
    }

    @Test
    public void testAuthenticateOutcomePolicyDenied() throws Exception {
        doReturn(Outcome.POLICY_DENIED).when(ropcCustomAuth)
                .callAuthEngine(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(),
                        anyMapOf(String.class, String.class), anyBoolean());
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
            fail("Expected AuthFailedServiceException was not thrown");
        } catch (ServiceException e) {
            assertTrue("Message should indicate rejected credentials",
                    e.getMessage().contains("Authentication failed : Policy Denied"));
        }
    }

    @Test
    public void testAuthenticateOutcomeMFATimeout() throws Exception {
        doReturn(Outcome.MFA_TIMEOUT).when(ropcCustomAuth)
                .callAuthEngine(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(),
                        anyMapOf(String.class, String.class), anyBoolean());
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
            fail("Expected AuthFailedServiceException was not thrown");
        } catch (ServiceException e) {
            assertTrue(ServiceException.TEMPORARILY_UNAVAILABLE().getCode().equals(e.getCode()));
        }
    }

    @Test
    public void testAuthenticateCacheHitSkipEngineCall() throws Exception {
        // Updated to match the new 7-parameter signature of checkInCache
        doReturn(new CacheResponse(true)).when(ropcCustomAuth)
                .checkInCache(anyString(), anyString(), anyString(), any(), anyString(), anyString(),
                        anyString(), any(), anyBoolean());

        mockContext.put(AC_PROTOCOL, AuthContext.Protocol.soap);

        ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);

        verify(ropcCustomAuth, never()).callAuthEngine(any(), anyString(), anyString(), anyString(),
                any(), any(), anyString(), anyString(), anyMapOf(String.class, String.class), anyBoolean());
    }

    @Test
    public void testAuthenticateZsyncProtocolCacheCheck() throws Exception {
        mockContext.put(AC_PROTOCOL, AuthContext.Protocol.zsync);
        mockContext.put(AC_SUB_PROTOCOL, AuthContext.SubProtocol.eas);

        // Mock a cache miss to ensure engine is called
        doReturn(new CacheResponse(false)).when(ropcCustomAuth)
                .checkInCache(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                        anyString(), any(), anyBoolean());

        doReturn(Outcome.SUCCESS).when(ropcCustomAuth)
                .callAuthEngine(any(), anyString(), anyString(), anyString(), any(), any(), anyString(), anyString(),
                        anyMapOf(String.class, String.class), anyBoolean());

        ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);

        verify(ropcCustomAuth, times(1)).callAuthEngine(mockAccount, "user1@example.zimbra.com",
                "password", "device-123", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "1234425", validConfig, false);
    }
}
