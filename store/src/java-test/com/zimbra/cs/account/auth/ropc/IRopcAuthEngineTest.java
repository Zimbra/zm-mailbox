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

package com.zimbra.cs.account.auth.ropc;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.ropc.store.IRopcTokenStore;
import com.zimbra.cs.account.auth.ropc.util.IRopcUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IROPCHandlerRegistry.class, IRopcAuthRequest.class, ZimbraLog.class,
        IRopcAuthResult.class, IRopcTokenStore.class, IRopcAuthEngine.class, IRopcUtil.class})
public class IRopcAuthEngineTest {

    private Map<String, String> validConfig;

    private IRopcHandler mockHandler;

    @Before
    public void setUp() throws ServiceException {
        validConfig = new HashMap<>();
        validConfig.put("token_endpoint", "https://okta.test/token");
        validConfig.put("client_id", "daljncvdalnc");
        validConfig.put("provider", "okta");

        mockHandler = PowerMockito.mock(IRopcHandler.class);
        PowerMockito.mockStatic(IROPCHandlerRegistry.class);
        PowerMockito.when(IROPCHandlerRegistry.get(anyString())).thenReturn(mockHandler);
        PowerMockito.suppress(PowerMockito.method(IRopcAuthEngine.class, "persist"));

        PowerMockito.mockStatic(IRopcUtil.class);
        PowerMockito.when(IRopcUtil.findInStore(any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(null);
    }

    @Test
    public void testRequestBuilderSetsAllFields() {
        IRopcAuthRequest request = new IRopcAuthRequest.Builder()
                .username("user")
                .password("secret")
                .config(validConfig)
                .build();

        assertEquals("user", request.getUsername());
        assertEquals("secret", request.getPassword());
        assertNotNull(request.getConfig());
    }

    @Test
    public void testAuthenticateNullUsernameReturnsError() {
        Outcome result = IRopcAuthEngine.authenticate(null, null, "pass", "device_id",
                AuthContext.Protocol.zsync,  AuthContext.SubProtocol.eas, "userAgent", "12312421", validConfig, false);
        assertEquals("Missing username should be caught and return error", Outcome.ERROR, result);
    }

    @Test
    public void testAuthenticateusernameWithoutReturnsError() {
        Outcome result = IRopcAuthEngine.authenticate(null, "test.com", "pass", "device_id",
                AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas, "userAgent", "12312421", validConfig, false);
        assertEquals("Username without @ should fail and return error", Outcome.ERROR, result);
    }

    @Test
    public void testAuthenticateNullPasswordReturnsError() {
        Outcome result = IRopcAuthEngine.authenticate(null, "test@okta.com", null,
                "device_id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals("Missing password should be caught and return error", Outcome.ERROR, result);
    }

    @Test
    public void testAuthenticateMissingTokenEnpointReturnsError() {
        validConfig.remove("token_endpoint");
        Outcome result = IRopcAuthEngine.authenticate(null, "test@okta.com", "pass",
                "device_id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals("Missing token endpoint should be caught and return error", Outcome.ERROR, result);
    }

    @Test
    public void testAuthenticateMissingClientidReturnsError() {
        validConfig.remove("client_id");
        Outcome result = IRopcAuthEngine.authenticate(null, "test@okta.com", "pass",
                "device_id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals("Missing client id should be caught and return error", Outcome.ERROR, result);
    }

    @Test
    public void testAuthenticateMissingProviderReturnsError() {
        validConfig.remove("provider");
        Outcome result = IRopcAuthEngine.authenticate(null, "test@okta.com", "pass",
                "device_id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals("Missing provider should be caught and return error", Outcome.ERROR, result);
    }

    @Test
    public void testEngineReturnsSuccessForOkta() throws ServiceException {
        IRopcAuthResult mockResult = PowerMockito.mock(IRopcAuthResult.class);
        when(mockResult.getStatus()).thenReturn(IRopcAuthResult.Status.SUCCESS);
        when(mockHandler.authenticate(any(IRopcAuthRequest.class))).thenReturn(mockResult);
        Outcome result = IRopcAuthEngine.authenticate(null, "user@test.com", "pass",
                "device-id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals(Outcome.SUCCESS, result);
    }

    @Test
    public void testEngineReturnsInvalidCredntial() throws ServiceException {
        IRopcAuthResult mockResult = PowerMockito.mock(IRopcAuthResult.class);
        when(mockResult.getStatus()).thenReturn(IRopcAuthResult.Status.INVALID_CREDENTIALS);
        when(mockHandler.authenticate(any(IRopcAuthRequest.class))).thenReturn(mockResult);
        Outcome result = IRopcAuthEngine.authenticate(null, "user@test.com", "pass",
                "device-id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals(Outcome.INVALID, result);
    }

    @Test
    public void testEngineReturnsPolicyDenied() throws ServiceException {
        IRopcAuthResult mockResult = PowerMockito.mock(IRopcAuthResult.class);
        when(mockResult.getStatus()).thenReturn(IRopcAuthResult.Status.POLICY_DENIED);
        when(mockHandler.authenticate(any(IRopcAuthRequest.class))).thenReturn(mockResult);
        Outcome result = IRopcAuthEngine.authenticate(null, "user@test.com", "pass",
                "device-id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals(Outcome.POLICY_DENIED, result);
    }

    @Test
    public void testEngineErrorOnDefaultStatus() throws ServiceException {
        IRopcAuthResult mockResult = PowerMockito.mock(IRopcAuthResult.class);
        when(mockResult.getStatus()).thenReturn(IRopcAuthResult.Status.ERROR);
        when(mockHandler.authenticate(any(IRopcAuthRequest.class))).thenReturn(mockResult);
        Outcome result = IRopcAuthEngine.authenticate(null, "user@test.com", "pass",
                "device-id", AuthContext.Protocol.zsync, AuthContext.SubProtocol.eas,
                "userAgent", "12312421", validConfig, false);
        assertEquals(Outcome.ERROR, result);
    }
}
