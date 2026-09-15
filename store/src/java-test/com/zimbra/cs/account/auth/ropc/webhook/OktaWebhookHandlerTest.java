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

import org.junit.Test;

public class OktaWebhookHandlerTest {

    private final OktaWebhookHandler handler = new OktaWebhookHandler();

    @Test
    public void testProviderName() {
        org.junit.Assert.assertEquals("okta", handler.getProviderName());
    }

    @Test
    public void testExtractUsernameSuccess() {
        String payload = "{\"data\":{\"events\":[{\"eventType\":\"user.account.update_password\"," +
                "\"target\":[{\"type\":\"AppInstance\"},{\"type\":\"User\",\"alternateId\":\"user@test.com\"}]}]}}";

        org.junit.Assert.assertEquals("user@test.com", handler.extractUsername(payload));
    }

    @Test
    public void testExtractUsernameNoPasswordEventReturnsNull() {
        String payload = "{\"data\":{\"events\":[{\"eventType\":\"user.session.start\"," +
                "\"target\":[{\"type\":\"User\",\"alternateId\":\"user@test.com\"}]}]}}";

        org.junit.Assert.assertNull(handler.extractUsername(payload));
    }

    @Test
    public void testExtractUsernamePasswordEventButNoUserTargetReturnsNull() {
        String payload = "{\"data\":{\"events\":[{\"eventType\":\"user.account.update_password\"," +
                "\"target\":[{\"type\":\"AppInstance\",\"alternateId\":\"ignored\"}]}]}}";

        org.junit.Assert.assertNull(handler.extractUsername(payload));
    }

    @Test
    public void testExtractUsernameMalformedPayloadReturnsNull() {
        org.junit.Assert.assertNull(handler.extractUsername("not-json"));
    }

    @Test
    public void testHandleVerificationChallengeWithHeaderWritesResponse() throws Exception {
        javax.servlet.http.HttpServletRequest request = org.mockito.Mockito.mock(javax.servlet.http.HttpServletRequest.class);
        javax.servlet.http.HttpServletResponse response = org.mockito.Mockito.mock(javax.servlet.http.HttpServletResponse.class);
        java.io.StringWriter body = new java.io.StringWriter();

        org.mockito.Mockito.when(request.getHeader("X-Okta-Verification-Challenge")).thenReturn("challenge-token");
        org.mockito.Mockito.when(response.getWriter()).thenReturn(new java.io.PrintWriter(body));

        handler.handleVerificationChallenge(request, response);

        org.mockito.Mockito.verify(response).setContentType("application/json");
        org.mockito.Mockito.verify(response).setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
        org.junit.Assert.assertEquals("{\"verification\":\"challenge-token\"}", body.toString());
    }

    @Test
    public void testHandleVerificationChallengeWithoutHeaderReturnsBadRequest() throws Exception {
        javax.servlet.http.HttpServletRequest request = org.mockito.Mockito.mock(javax.servlet.http.HttpServletRequest.class);
        javax.servlet.http.HttpServletResponse response = org.mockito.Mockito.mock(javax.servlet.http.HttpServletResponse.class);

        org.mockito.Mockito.when(request.getHeader("X-Okta-Verification-Challenge")).thenReturn(null);

        handler.handleVerificationChallenge(request, response);

        org.mockito.Mockito.verify(response).setStatus(javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
    }
}

