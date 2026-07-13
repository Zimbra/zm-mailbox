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
import com.zimbra.cs.account.auth.ropc.okta.OktaRopcHandler;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IROPCHandlerRegistryTest {

    @Test
    public void testOktaHandler() throws Exception {
        OktaRopcHandler handler = new OktaRopcHandler();
        IRopcAuthRequest request = new IRopcAuthRequest.Builder().username("user").password("pass")
                .build();
        assertEquals("okta", handler.getName());
    }

    @Test
    public void testGetOktaHandlerFromRegistry() throws Exception {
        IRopcHandler handler = IROPCHandlerRegistry.get("okta");
        assertTrue(handler instanceof OktaRopcHandler);
    }

    @Test
    public void testGetUnknownHandlerThrowsServiceException() {
        try {
            IROPCHandlerRegistry.get("unknown");
            fail("Expected ServiceException for unknown ROPC handler");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("No ROPC handler registered for type"));
        }
    }
}

