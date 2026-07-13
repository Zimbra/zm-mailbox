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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.ropc.Outcome;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IRopcCustomAuthTest {
    private IRopcCustomAuth ropcCustomAuth;

    private Account mockAccount;

    private Map<String, Object> mockContext;

    private Map<String, String> validConfig;

    private List<String> args = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        IRopcCustomAuth target = new IRopcCustomAuth();
        ropcCustomAuth = spy(target);
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("user1@example.zimbra.com", "password",
                new HashMap<String, Object>());
        mockAccount = Provisioning.getInstance().get(Key.AccountBy.name, "user1@example.zimbra.com");
        mockContext = new HashMap<String, Object>();
        mockContext.put("did", "device-123");
        mockContext.put("proto", AuthContext.Protocol.zsync);

        validConfig = new HashMap<>();
        validConfig.put("token_endpoint", "https://okta.test/token");
        validConfig.put("client_id", "daljncvdalnc");
        validConfig.put("provider", "okta");

        args.add("token_endpoint=https://okta.test/token");
        args.add("client_id=daljncvdalnc");
        args.add("provider=okta");
    }

    @Test
    public void testCLassIsInstantiated() {
        assertNotNull("IRopcCustomAuthInstance should not be null", ropcCustomAuth);
    }

    @Test
    public void testAuthenticateValidCallDoesnotThrowException() {
        doReturn(Outcome.SUCCESS).when(ropcCustomAuth)
                .callAuthEngine(anyString(), anyString(), anyString(), any(), anyMapOf(String.class, String.class));
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
        } catch (Exception e) {
            fail("Auth should not throw a exception");
        }

    }

    @Test
    public void testCeckPasswordAgingReturnsFalse() {
        assertFalse("Password aging should return false", ropcCustomAuth.checkPasswordAging());
    }

    @Test
    public void testAuthenticateOutcomeSuccess() throws Exception {
        doReturn(Outcome.SUCCESS).when(ropcCustomAuth)
                .callAuthEngine(anyString(), anyString(), anyString(), any(), anyMapOf(String.class, String.class));
        ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
        verify(ropcCustomAuth, times(1))
                .callAuthEngine("user1@example.zimbra.com", "password", "device-123",
                        AuthContext.Protocol.zsync, validConfig);
    }

    @Test
    public void testAuthenticateOutcomeInvalid() throws Exception {
        doReturn(Outcome.INVALID).when(ropcCustomAuth)
                .callAuthEngine(anyString(), anyString(), anyString(), any(), anyMapOf(String.class, String.class));
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
                .callAuthEngine(anyString(), anyString(), anyString(), any(), anyMapOf(String.class, String.class));
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
            fail("Expected AuthFailedServiceException was not thrown");
        } catch (AccountServiceException.AuthFailedServiceException e) {
            assertTrue("Message should indicate rejected credentials",
                    e.getMessage().contains("Authentication failed : Policy Denied"));
        }
    }

    @Test
    public void testAuthenticateOutcomeMFATimeout() throws Exception {
        doReturn(Outcome.MFA_TIMEOUT).when(ropcCustomAuth)
                .callAuthEngine(anyString(), anyString(), anyString(), any(), anyMapOf(String.class, String.class));
        try {
            ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
            fail("Expected AuthFailedServiceException was not thrown");
        } catch (AccountServiceException.AuthFailedServiceException e) {
            assertTrue("Message should indicate timeout",
                    e.getMessage().contains("MFA request timed out. Please try again"));
        }
    }

    @Test
    public void testAuthenticateCacheHitSkipEngineCall() throws Exception {
        doReturn(true).when(ropcCustomAuth).checkInCache(anyString(), anyString());
        mockContext.put("proto", AuthContext.Protocol.soap);
        ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
        verify(ropcCustomAuth, never())
                .callAuthEngine(anyString(), anyString(), anyString(), any(), anyMapOf(String.class, String.class));
    }

    @Test
    public void testAuthenticateZsyncProtocolByPassCacheCheck() throws Exception {
        mockContext.put(AuthContext.AC_PROTOCOL, AuthContext.Protocol.zsync);
        doReturn(true).when(ropcCustomAuth).checkInCache(anyString(), anyString());
        doReturn(Outcome.SUCCESS).when(ropcCustomAuth)
                .callAuthEngine(anyString(), anyString(), anyString(), any(), anyMapOf(String.class, String.class));
        ropcCustomAuth.authenticate(mockAccount, "password", mockContext, args);
        verify(ropcCustomAuth, times(1))
                .callAuthEngine("user1@example.zimbra.com", "password", "device-123",
                        AuthContext.Protocol.zsync, validConfig);
    }
}
