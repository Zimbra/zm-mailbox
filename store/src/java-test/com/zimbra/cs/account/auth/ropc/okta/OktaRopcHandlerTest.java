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

package com.zimbra.cs.account.auth.ropc.okta;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.auth.ropc.IRopcAuthRequest;
import com.zimbra.cs.account.auth.ropc.IRopcAuthResult;
import com.zimbra.cs.account.auth.ropc.MFAChallenge;
import com.zimbra.cs.account.auth.ropc.MFAPollResult;
import com.zimbra.cs.account.auth.ropc.util.HttpResponseWrapper;
import com.zimbra.cs.account.auth.ropc.util.HttpUtilities;
import com.zimbra.cs.account.auth.ropc.util.JsonUtilities;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtilities.class, JsonUtilities.class, Thread.class, IRopcAuthRequest.class,
        MFAChallenge.class, HttpResponseWrapper.class})
@SuppressStaticInitializationFor("com.zimbra.cs.account.auth.ropc.util.HttpUtilities")
public class OktaRopcHandlerTest {

    private OktaRopcHandler handler;

    private IRopcAuthRequest mockAuthRequest;

    private MFAChallenge mockChallenge;

    @Before
    public void setup() {
        handler = new OktaRopcHandler();
        mockAuthRequest  = PowerMockito.mock(IRopcAuthRequest.class);
        mockChallenge = PowerMockito.mock(MFAChallenge.class);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("FACTOR", "PUSH");
        configMap.put("token_endpoint", "https://okta.test/token");
        configMap.put("client_id", "dsvdsvdsvsd");
        configMap.put("client_Secret", "dsvdsvdsv");

        when(mockAuthRequest.getConfig()).thenReturn(configMap);
        when(mockAuthRequest.getUsername()).thenReturn("testuser@zimbra.com");
        when(mockAuthRequest.getPassword()).thenReturn("password");

        PowerMockito.mockStatic(HttpUtilities.class);
        PowerMockito.mockStatic(JsonUtilities.class);
        PowerMockito.mockStatic(Thread.class);
    }

    @Test
    public void testAuthenticateNullrequestReturnserror() throws Exception {
        IRopcAuthResult result = handler.authenticate(null);
        assertEquals("invalid_request", result.getErrorCode());
    }

    @Test
    public void testAuthenticateDirectSuccessNoMfaRequired() throws Exception {
        mockHttpAndJsonResponse(200, "{\"access_token\":\"abc\"}",
                createOktaResponse("token123", null, null, null));
        IRopcAuthResult result = handler.authenticate(mockAuthRequest);
        assertEquals("token123", result.getAccessToken());
    }

    @Test
    public void testAuthenticateInvalidPasswordreturnInvalidCredntials() throws Exception {
        OktaResponse errorRes = createOktaResponse(null, "invalid_grant", "Invalid username or password", null);
        mockHttpAndJsonResponse(401, "{\"error\":\"invalid_grant\"}", errorRes);
        IRopcAuthResult result = handler.authenticate(mockAuthRequest);
        assertEquals(IRopcAuthResult.Status.INVALID_CREDENTIALS, result.getStatus());
    }

    @Test
    public void testAuthenticateSignnPolicyDeniedreturnPolicyDenied() throws Exception {
        OktaResponse errorRes = createOktaResponse(null, "invalid_grant", "sign on policy denied access", null);
        mockHttpAndJsonResponse(401, "{\"error\":\"invalid_grant\"}", errorRes);
        IRopcAuthResult result = handler.authenticate(mockAuthRequest);
        assertEquals(IRopcAuthResult.Status.POLICY_DENIED, result.getStatus());
    }

    @Test
    public void mfaRequiredSuccessfulTriggeredsPush() throws Exception {
        HttpResponseWrapper mfaRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(mfaRes.getStatusCode()).thenReturn(401);
        when(mfaRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse mfaJson = createOktaResponse(null, "mfa_required", null, "mfa_123");
        HttpResponseWrapper pushHttpRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(pushHttpRes.getStatusCode()).thenReturn(200);
        when(pushHttpRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse pushJson = new OktaResponse();
        pushJson.setOobCode("oob_code_123");

        PowerMockito.when(HttpUtilities.postForm(anyString(), anyMapOf(String.class, String.class),
                anyInt(), anyInt(), anyMapOf(String.class, String.class))).thenReturn(mfaRes, pushHttpRes);


        PowerMockito.when(JsonUtilities.read(any(byte[].class), eq(OktaResponse.class))).thenReturn(mfaJson, pushJson);

        IRopcAuthResult result = handler.authenticate(mockAuthRequest);

        assertEquals(IRopcAuthResult.Status.MFA_CHALLENGE, result.getStatus());
        assertNotNull(result.getChallenge());
    }

    @Test
    public void mfaTriggerPushWithRetryThrowsServiceExcetionon500() throws Exception {
        HttpResponseWrapper mfaRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(mfaRes.getStatusCode()).thenReturn(401);
        when(mfaRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse mfaJson = createOktaResponse(null, "mfa_required", null, "mfa_123");


        HttpResponseWrapper pushHttpRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(pushHttpRes.getStatusCode()).thenReturn(500);
        when(pushHttpRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse pushJson = new OktaResponse();

        PowerMockito.when(HttpUtilities.postForm(anyString(), anyMapOf(String.class, String.class),
                anyInt(), anyInt(), anyMapOf(String.class, String.class))).
                thenReturn(mfaRes, pushHttpRes, pushHttpRes, pushHttpRes);

        PowerMockito.mockStatic(Thread.class);

        PowerMockito.when(JsonUtilities.read(any(byte[].class), eq(OktaResponse.class))).
                thenReturn(mfaJson, pushJson, pushJson, pushJson);

        try {
            handler.authenticate(mockAuthRequest);
            fail("Expected ServcieException.FAILURE due to 500 reties exhausted");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Okta API unavailable after retries")
                    ||  e.getMessage().contains("Network failure during challenge call"));
        }
    }

    @Test
    public void mfaTriggerPushWithRetryThrowsServiceExcetionon400() throws Exception {
        HttpResponseWrapper mfaRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(mfaRes.getStatusCode()).thenReturn(401);
        when(mfaRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse mfaJson = createOktaResponse(null, "mfa_required", null, "mfa_123");
        HttpResponseWrapper pushHttpRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(pushHttpRes.getStatusCode()).thenReturn(400);
        when(pushHttpRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse pushJson = new OktaResponse();

        PowerMockito.when(HttpUtilities.postForm(anyString(), anyMapOf(String.class, String.class),
                        anyInt(), anyInt(), anyMapOf(String.class, String.class))).
                thenReturn(mfaRes, pushHttpRes);

        PowerMockito.mockStatic(Thread.class);

        PowerMockito.when(JsonUtilities.read(any(byte[].class), eq(OktaResponse.class))).
                thenReturn(mfaJson, pushJson);

        try {
            handler.authenticate(mockAuthRequest);
            fail("Expected ServcieException.FAILURE due to 4xx rerror");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Okta returned error while challenge trigger"));
        }
    }

    @Test
    public void mfaTriggerPushWithRetryMissingOOBCodeThrowsServiceExcetionon400() throws Exception {
        HttpResponseWrapper mfaRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(mfaRes.getStatusCode()).thenReturn(401);
        when(mfaRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse mfaJson = createOktaResponse(null, "mfa_required", null, "mfa_123");


        HttpResponseWrapper pushHttpRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(pushHttpRes.getStatusCode()).thenReturn(200);
        when(pushHttpRes.getBody()).thenReturn("empty body".getBytes());
        OktaResponse pushJson = new OktaResponse();
        pushJson.setOobCode("");

        PowerMockito.when(HttpUtilities.postForm(anyString(), anyMapOf(String.class, String.class),
                        anyInt(), anyInt(), anyMapOf(String.class, String.class))).
                thenReturn(mfaRes, pushHttpRes);

        PowerMockito.mockStatic(Thread.class);

        PowerMockito.when(JsonUtilities.read(any(byte[].class), eq(OktaResponse.class))).
                thenReturn(mfaJson, pushJson);

        try {
            handler.authenticate(mockAuthRequest);
            fail("Expected ServcieException.FAILURE due to missing oobcode");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("Missing Required OOB code"));
        }
    }

    @Test
    public void testPollChallengeSuccess() throws Exception {
        mockHttpAndJsonResponse(200, "{\"access_token\":\"abc\"}",
                createOktaResponse("token123", null, null, null));
        MFAPollResult result = handler.pollChallenge((mockChallenge));
        assertEquals(MFAPollResult.SUCCESS, result);
    }

    @Test
    public void testPollChallengeAuthorizationPendingReturnWAIITNG() throws Exception {
        mockHttpAndJsonResponse(400, "{\"authorization_pending\":\"abc\"}",
                createOktaResponse(null, "authorization_pending", null, null));
        MFAPollResult result = handler.pollChallenge((mockChallenge));
        assertEquals(MFAPollResult.WAITING, result);
    }

    @Test
    public void testPollChallengeAccessDeniedReturnEXPIRED() throws Exception {
        mockHttpAndJsonResponse(400, "{\"access_denied\":\"abc\"}",
                createOktaResponse(null, "access_denied", null, null));
        MFAPollResult result = handler.pollChallenge((mockChallenge));
        assertEquals(MFAPollResult.EXPIRED, result);
    }

    @Test
    public void testPollChallengeInvaldiGrantReturnREJECTED() throws Exception {
        mockHttpAndJsonResponse(400, "{\"invalid_grant\":\"abc\"}",
                createOktaResponse(null, "invalid_grant", null, null));
        MFAPollResult result = handler.pollChallenge((mockChallenge));
        assertEquals(MFAPollResult.REJECTED, result);
    }

    private void mockHttpAndJsonResponse(int statusCOde, String rawBody, OktaResponse jsonResponse) throws Exception {
        HttpResponseWrapper mockHttpRes = PowerMockito.mock(HttpResponseWrapper.class);
        when(mockHttpRes.getStatusCode()).thenReturn(statusCOde);
        when(mockHttpRes.getBody()).thenReturn(rawBody.getBytes());

        PowerMockito.when(HttpUtilities.postForm(anyString(), anyMapOf(String.class, String.class),
                anyInt(), anyInt(), anyMapOf(String.class, String.class))).thenReturn(mockHttpRes);

        PowerMockito.when(JsonUtilities.read(any(byte[].class), eq(OktaResponse.class))).
                thenReturn(jsonResponse);
    }

    private OktaResponse createOktaResponse(String accesstoken, String error, String erroDesc, String mfaToken) {
        OktaResponse res = new OktaResponse();
        res.setAccessToken(accesstoken);
        res.setError(error);
        res.setErrorDescription(erroDesc);
        res.setMfaToken(mfaToken);
        return res;
    }
}
