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

import com.zimbra.cs.account.auth.ropc.*;
import java.net.SocketTimeoutException;
import org.junit.Test;
import static org.junit.Assert.*;

public class IRopcAuthEngineTest {

    // Builder with all fields including protocolContext
    @Test
    public void testRequestBuilderSetsAllFields() {
        IRopcAuthRequest request = new IRopcAuthRequest.Builder()
                .username("user")
                .password("secret")
                .provider("okta")
                .protocolContext("zsync")
                .factorType(IRopcAuthRequest.FactorType.PUSH)
                .build();

        assertEquals("user", request.getUsername());
        assertEquals("secret", request.getPassword());
        assertEquals("okta", request.getProvider());
        assertEquals("zsync", request.getProtocolContext());
        assertEquals(IRopcAuthRequest.FactorType.PUSH, request.getFactorType());
    }

    @Test
    public void testRequestDefaultFactorTypeIsNone() {
        IRopcAuthRequest request = new IRopcAuthRequest.Builder()
                .username("user")
                .password("pass")
                .provider("okta")
                .build();

        assertEquals(IRopcAuthRequest.FactorType.NONE, request.getFactorType());
    }

    // Builder validation
    @Test(expected = IllegalArgumentException.class)
    public void testRequestBuilderNullUsernameThrows() {
        new IRopcAuthRequest.Builder().username(null).password("pass").provider("okta").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestBuilderEmptyPasswordThrows() {
        new IRopcAuthRequest.Builder().username("user").password("").provider("okta").build();
    }

    // FactorType.fromConfig
    @Test
    public void testFactorTypeFromConfigNullDefaultsPush() {
        assertEquals(IRopcAuthRequest.FactorType.NONE,
                IRopcAuthRequest.FactorType.fromConfig(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactorTypeFromConfigInvalidThrows() {
        IRopcAuthRequest.FactorType.fromConfig("sms");
    }

    // Handlers
    @Test
    public void testOktaHandler() throws Exception {
        OktaRopcHandler handler = new OktaRopcHandler();
        IRopcAuthRequest request = new IRopcAuthRequest.Builder().username("user").password("pass").provider("okta")
                .build();
        assertEquals("okta", handler.getName());
        try {
            boolean result = handler.authenticate(request);
            assertTrue("Handler should return true", result);
        } catch (SocketTimeoutException e) {
            fail("Handler should not throw SocketTimeoutException: " + e.getMessage());
        }
    }

    @Test
    public void testEngineReturnsSuccessForOkta() {
        assertEquals(Outcome.SUCCESS, IRopcAuthEngine.authenticate("user", "pass", "okta",
                "NONE", "zsync", null));
    }

    @Test
    public void testEngineReturnsErrorForUnknownProvider() {
        assertEquals(Outcome.ERROR, IRopcAuthEngine.authenticate("user", "pass", "unknown",
                "NONE", "zsync", null));
    }

    @Test
    public void testEngineReturnsErrorForNullProvider() {
        assertEquals(Outcome.ERROR, IRopcAuthEngine.authenticate("user", "pass", null,
                "NONE", "zsync", null));
    }

    @Test
    public void testEngineReturnsErrorForInvalidFactor() {
        assertEquals(Outcome.ERROR, IRopcAuthEngine.authenticate("user", "pass", "okta",
                "sms", "zsync", null));
    }

    @Test
    public void testEngineDefaultsFactorWhenConfigNull() {
        assertEquals(Outcome.SUCCESS, IRopcAuthEngine.authenticate("user", "pass", "okta",
                null, "zsync", null));
    }

    @Test
    public void testCaseInsensitiveProviderResolution() {
        assertEquals(Outcome.SUCCESS, IRopcAuthEngine.authenticate("user", "pass", "OKTA",
                "NONE", "zsync", null));
    }
}
