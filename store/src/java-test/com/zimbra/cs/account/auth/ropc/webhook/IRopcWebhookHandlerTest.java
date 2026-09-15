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

package com.zimbra.cs.account.auth.ropc.webhook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class IRopcWebhookHandlerTest {

    private static class DummyHandler implements IRopcWebhookHandler {
        @Override
        public String getProviderName() {
            return "dummy";
        }

        @Override
        public String extractUsername(String rawPayload) {
            return rawPayload;
        }

        @Override
        public void handleVerificationChallenge(javax.servlet.http.HttpServletRequest request,
                                                javax.servlet.http.HttpServletResponse response)
                throws java.io.IOException {
            response.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
        }
    }

    @Test
    public void interfaceContractCanBeImplementedAndInvoked() throws Exception {
        IRopcWebhookHandler handler = new DummyHandler();
        HttpServletRequest request = mock(javax.servlet.http.HttpServletRequest.class);
        HttpServletResponse response = mock(javax.servlet.http.HttpServletResponse.class);

        assertEquals("dummy", handler.getProviderName());
        assertEquals("user@test.com", handler.extractUsername("user@test.com"));
        handler.handleVerificationChallenge(request, response);

        org.mockito.Mockito.verify(response).setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
    }
}

