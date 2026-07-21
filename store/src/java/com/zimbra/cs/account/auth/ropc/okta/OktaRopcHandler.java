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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.auth.ropc.*;
import com.zimbra.cs.account.auth.ropc.util.HttpResponseWrapper;
import com.zimbra.cs.account.auth.ropc.util.HttpUtilities;
import com.zimbra.cs.account.auth.ropc.util.JsonUtilities;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.ACCEPT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.ACCESS_DENIED;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.ACCESS_EXPIRY_DEFAULT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.APPLICATION_JSON;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.AUTHORIZATION;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.AUTHORIZATION_PENDING;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.BASIC_HEADER;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CHALLENGE_ENPOINT_CORE;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CHALLENGE_TYPES_SUPPORTED_FOR_PUSH_REQ;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CLIENT_SECRET_AUTH_TYPE_BASIC;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CONNECTION_TIMEOUT_DEFAULT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CONVERT_TO_MILLI;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.ERROR_DESCRIPTION_SIGN_ON_POLICY;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.EXPIRED_TOKEN;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.FACTOR;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.FACTOR_PUSH;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.GRANT_PASSWORD;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.GRANT_TYPE_FOR_POLL;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.HTTP_APPEND;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.INTERVAL;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.INVALID_GRANT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.MAX_LOG_LENGTH;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.MFA_REQUIRED;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.OKTA_REQUEST_TYPE_CHALLENGE;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.OKTA_REQUEST_TYPE_TOKEN;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.POLLING_INTERVAL;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.POLLING_TIMEOUT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.PROVIDER_NAME_OKTA;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.PUSH;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CHALLENGE_TYPES_SUPPORTED;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CHANNEL_HINT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CLIENT_ID;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CLIENT_SECRET;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CLIENT_SECRET_AUTH_TYPE;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CONNECTION_TIMEOUT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_GRANT_TYPE;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_MFA_TOKEN;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_OOB_CODE;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_PASSWORD;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_SCOPE;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_SOCKET_TIMEOUT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_USERNAME;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.SCOPE_DEFAULT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.SLOW_DOWN;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.SOCKET_TIMEOUT_DEFAULT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.TOKEN_ENDPOINT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.TOKEN_ENPOINT_CORE;

public final class OktaRopcHandler implements IRopcHandler {

    @Override
    public String getName() {
        return PROVIDER_NAME_OKTA;
    }

    @Override
    public IRopcAuthResult authenticate(IRopcAuthRequest req) throws ServiceException {
        if (req == null || req.getConfig() == null) {
            return IRopcAuthResult.error("invalid_request", "missing request or config");
        }

        MFAFactorType factor = MFAFactorType.fromConfig(req.getConfig().get(FACTOR));

        switch (factor.name()) {
            case FACTOR_PUSH :
                return ropcWithPush(req);
            default :
                throw new IllegalArgumentException("Unsupported factor type: " + factor.name());
        }
    }

    @Override
    public MFAPollResult pollChallenge(MFAChallenge challenge) throws ServiceException {
        Map<String, String> form = new LinkedHashMap<String, String>();
        form.put(REQUEST_PARAM_GRANT_TYPE, GRANT_TYPE_FOR_POLL);
        form.put(REQUEST_PARAM_MFA_TOKEN, getSafeString(challenge.get(REQUEST_PARAM_MFA_TOKEN)));
        form.put(REQUEST_PARAM_OOB_CODE, getSafeString(challenge.get(REQUEST_PARAM_OOB_CODE)));
        form.put(REQUEST_PARAM_SCOPE, getSafeString(challenge.get(REQUEST_PARAM_SCOPE)));

        Map<String, String> headers = new HashMap<>();
        String tokenEndpoint = challenge.get(TOKEN_ENDPOINT);
        String clientId = challenge.get(REQUEST_PARAM_CLIENT_ID);
        String clientSecret = challenge.get(REQUEST_PARAM_CLIENT_SECRET);
        String clientSecretAuth = challenge.get(REQUEST_PARAM_CLIENT_SECRET_AUTH_TYPE);
        int httpConnectMs = NumberUtils.toInt(challenge.get(REQUEST_PARAM_CONNECTION_TIMEOUT),
                CONNECTION_TIMEOUT_DEFAULT);
        int httpSocketMs = NumberUtils.toInt(challenge.get(REQUEST_PARAM_SOCKET_TIMEOUT), SOCKET_TIMEOUT_DEFAULT);

        headers.put(ACCEPT, APPLICATION_JSON);

        if (CLIENT_SECRET_AUTH_TYPE_BASIC.equalsIgnoreCase(clientSecretAuth)) {
            if (clientSecret == null || clientSecret.trim().isEmpty()) {
                throw ServiceException.FAILURE("Client Secret is mandatory", null);
            }
            headers.put(AUTHORIZATION, BASIC_HEADER + basicAuth(clientId, clientSecret));
        } else {
            form.put(REQUEST_PARAM_CLIENT_ID, clientId);
            if (clientSecret != null && !clientSecret.trim().isEmpty()) {
                form.put(REQUEST_PARAM_CLIENT_SECRET, clientSecret);
            }
        }

        OktaResponse r = callEndpoint(tokenEndpoint, httpConnectMs, httpSocketMs, form, headers);
        if (r.hasAccessToken()) {
            challenge.put(MFAChallenge.REFRESH_TOKEN, getSafeString(r.getRefreshToken()));
            challenge.put(MFAChallenge.ACCESS_TOKEN, getSafeString(r.getAccessToken()));
            challenge.put(MFAChallenge.ACCESS_EXPIRES_AT, Long.toString(expiryMillis(r)));
            return MFAPollResult.SUCCESS;
        }
        String err = getSafeString(r.getError());
        if (AUTHORIZATION_PENDING.equals(err) || SLOW_DOWN.equals(err)) {
            return MFAPollResult.WAITING;
        }
        if (EXPIRED_TOKEN.equals(err) || ACCESS_DENIED.equals(err)) {
            return MFAPollResult.EXPIRED;
        }
        if (INVALID_GRANT.equals(err)) {
            return MFAPollResult.REJECTED;
        }
        ZimbraLog.account.debug("Okta poll unexpected error: %s / %s", err, r.getErrorDescription());
        return MFAPollResult.ERROR;
    }

    private IRopcAuthResult ropcWithPush(IRopcAuthRequest req) throws ServiceException {
        Map<String, String> form = new LinkedHashMap<String, String>();

        form.put(REQUEST_PARAM_GRANT_TYPE, GRANT_PASSWORD);
        form.put(REQUEST_PARAM_USERNAME, req.getUsername());
        form.put(REQUEST_PARAM_PASSWORD, req.getPassword());
        form.put(REQUEST_PARAM_SCOPE, req.getConfig().get(REQUEST_PARAM_SCOPE) != null ?
                req.getConfig().get(REQUEST_PARAM_SCOPE) : SCOPE_DEFAULT);

        ZimbraLog.account.debug("Authentication with ROPC initiated for user :  %s", req.getUsername());
        OktaResponse r = call(req.getConfig(), form, OKTA_REQUEST_TYPE_TOKEN);
        if (r.hasAccessToken()) {
            return IRopcAuthResult.success(r.getRefreshToken(), r.getAccessToken(), expiryMillis(r));
        }

        if (isMfaRequired(r)) {
            return IRopcAuthResult.challenge(buildPushChallenge(req, r));
        }
        if (INVALID_GRANT.equals(r.getError())) {
            String desc = getSafeString(r.getErrorDescription());
            return desc.toLowerCase().contains(ERROR_DESCRIPTION_SIGN_ON_POLICY)
                    ? IRopcAuthResult.policyDenied(desc)
                    : IRopcAuthResult.invalidCredentials(r.getError(), desc);
        }
        return IRopcAuthResult.error(getSafeString(r.getError()), r.getErrorDescription());
    }

    private MFAChallenge buildPushChallenge(IRopcAuthRequest req, OktaResponse initial)
            throws ServiceException {
        String oobCode = getSafeString(initial.getOobCode());
        String mfaToken = getSafeString(initial.getMfaToken());
        OktaResponse pushResponse = null;
        if (oobCode.isEmpty()) {
            pushResponse = triggerPushWithRetry(req, initial);
        }

        oobCode = pushResponse.getOobCode();

        if (pushResponse.getMfaToken() != null && !pushResponse.getMfaToken().isEmpty()) {
            mfaToken = pushResponse.getMfaToken();
        }

        if (oobCode.isEmpty()) {
            throw ServiceException.FAILURE("Authentication failed : Mandatory parameter is not returned " +
                    "in challenge", null);
        }

        Map<String, String> state = new HashMap<String, String>();
        state.put(REQUEST_PARAM_USERNAME, req.getUsername());
        state.put(REQUEST_PARAM_MFA_TOKEN, getSafeString(mfaToken));
        state.put(REQUEST_PARAM_OOB_CODE, getSafeString(oobCode));
        // carry config so pollChallenge can reach the token endpoint / timeouts
        state.put(TOKEN_ENDPOINT, getSafeString(req.getConfig().get(TOKEN_ENDPOINT)));
        state.put(REQUEST_PARAM_CLIENT_ID, getSafeString(req.getConfig().get(REQUEST_PARAM_CLIENT_ID)));
        state.put(REQUEST_PARAM_CLIENT_SECRET, getSafeString(req.getConfig().get(REQUEST_PARAM_CLIENT_SECRET)));
        state.put(REQUEST_PARAM_CONNECTION_TIMEOUT, getSafeString(req.getConfig().
                get(REQUEST_PARAM_CONNECTION_TIMEOUT)));
        state.put(REQUEST_PARAM_SOCKET_TIMEOUT, getSafeString(req.getConfig().get(REQUEST_PARAM_SOCKET_TIMEOUT)));
        state.put(REQUEST_PARAM_CLIENT_SECRET_AUTH_TYPE, getSafeString(req.getConfig().
                get(REQUEST_PARAM_CLIENT_SECRET_AUTH_TYPE)));
        state.put(REQUEST_PARAM_SCOPE, req.getConfig().get(REQUEST_PARAM_SCOPE) != null ?
                req.getConfig().get(REQUEST_PARAM_SCOPE) : SCOPE_DEFAULT);
        state.put(INTERVAL, req.getConfig().get(POLLING_INTERVAL) == null ?
                getSafeString(pushResponse.getInterval()) : getSafeString(req.getConfig().get(POLLING_INTERVAL)));
        state.put(POLLING_TIMEOUT, req.getConfig().get(POLLING_TIMEOUT) == null ?
                getSafeString(pushResponse.getExpiresIn()) : getSafeString(req.getConfig().get(POLLING_TIMEOUT)));

        String dedupeKey = req.getUserAgent() + "|" + req.getUsername() + "|" +  PROVIDER_NAME_OKTA + "|"
                + (req.getProtocol() == null ? "" : req.getProtocol()) + "|"
                + (req.getDeviceId() == null ? "" : req.getDeviceId());
        return new MFAChallenge(PROVIDER_NAME_OKTA, dedupeKey, MFAFactorType.PUSH, state);
    }

    private OktaResponse triggerPushWithRetry(IRopcAuthRequest req, OktaResponse initial) throws ServiceException {
        int maxRetries = 2;
        long retryDelayMs = 500L;
        String mfaToken = getSafeString(initial.getMfaToken());

        Map<String, String> init = new LinkedHashMap<String, String>();
        init.put(REQUEST_PARAM_GRANT_TYPE, GRANT_PASSWORD);
        init.put(REQUEST_PARAM_MFA_TOKEN, mfaToken);
        init.put(REQUEST_PARAM_CHANNEL_HINT, PUSH);
        init.put(REQUEST_PARAM_CHALLENGE_TYPES_SUPPORTED, CHALLENGE_TYPES_SUPPORTED_FOR_PUSH_REQ);

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                ZimbraLog.account.debug("MFA triggered with PUSH factor for user :  %s ," +
                        " Attempt : %d ", req.getUsername(), attempt);
                OktaResponse ch = call(req.getConfig(), init, OKTA_REQUEST_TYPE_CHALLENGE);
                int status = ch.getHttpStatusCode();
                if (status >= 500 && status <= 504) {
                    if (attempt == maxRetries) {
                        throw ServiceException.FAILURE("Okta API unavailable after retries", null);
                    }
                    ZimbraLog.account.debug("Attempt %d failed with error : %s", attempt, ch.getErrorDescription());
                    sleepsafely(retryDelayMs);
                    continue;
                }

                if (status >= 400 && status < 500) {
                    throw ServiceException.FAILURE("Okta returned error while challenge trigger : " +
                            ch.getErrorDescription(), null);
                }

                String oobCode = getSafeString(ch.getOobCode());
                if (oobCode.isEmpty()) {
                    throw ServiceException.FAILURE("Missing Required OOB code in okta response", null);
                }

                return ch;

            } catch (ServiceException e) {
                if (e.getMessage() != null && (e.getMessage().contains("returned error") ||
                        e.getMessage().contains("Missing Required"))) {
                    throw e;
                }
                if (attempt == maxRetries) {
                    throw ServiceException.FAILURE("Network failure during challenge call", null);
                }
                sleepsafely(retryDelayMs);
            }
        }
        throw ServiceException.FAILURE("Failed to retrieve required code due to unknown error", null);
    }

    private void sleepsafely(long retryDelayMs) {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException ie) {
        }
    }

    private OktaResponse call(Map<String, String> configs, Map<String, String> form, String requestType)
            throws ServiceException {
        Map<String, String> headers = new HashMap<>();
        String tokenEndpoint = configs.get(TOKEN_ENDPOINT);
        String clientId = configs.get(REQUEST_PARAM_CLIENT_ID);
        String clientSecret = configs.get(REQUEST_PARAM_CLIENT_SECRET);
        String clientSecretAuth = configs.get(REQUEST_PARAM_CLIENT_SECRET_AUTH_TYPE);
        int httpConnectMs = NumberUtils.toInt(configs.get(REQUEST_PARAM_CONNECTION_TIMEOUT),
                CONNECTION_TIMEOUT_DEFAULT);
        int httpSocketMs = NumberUtils.toInt(configs.get(REQUEST_PARAM_SOCKET_TIMEOUT), SOCKET_TIMEOUT_DEFAULT);

        headers.put(ACCEPT, APPLICATION_JSON);

        if (OKTA_REQUEST_TYPE_CHALLENGE.equalsIgnoreCase(requestType)) {
            tokenEndpoint = tokenEndpoint.replace(TOKEN_ENPOINT_CORE, CHALLENGE_ENPOINT_CORE);
        }

        if (CLIENT_SECRET_AUTH_TYPE_BASIC.equalsIgnoreCase(clientSecretAuth)) {
            if (clientSecret == null || clientSecret.trim().isEmpty()) {
                throw ServiceException.FAILURE("Client Secret is mandatory", null);
            }
            headers.put(AUTHORIZATION, BASIC_HEADER + basicAuth(clientId, clientSecret));
        } else {
            form.put(REQUEST_PARAM_CLIENT_ID, clientId);
            if (clientSecret != null && !clientSecret.trim().isEmpty()) {
                form.put(REQUEST_PARAM_CLIENT_SECRET, clientSecret);
            }
        }
        return callEndpoint(tokenEndpoint, httpConnectMs, httpSocketMs, form, headers);
    }

    private static String basicAuth(String clientId, String clientSecret) {
        String creds = (clientId == null ? "" : clientId) + ":" + (clientSecret == null ? "" : clientSecret);
        return Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    private OktaResponse callEndpoint(String endpoint, int connectMs, int socketMs,
                                      Map<String, String> form, Map<String, String> headers)
            throws ServiceException {
        HttpResponseWrapper resp = HttpUtilities.postForm(endpoint, form, connectMs, socketMs, headers);
        int statusCode = resp.getStatusCode();
        byte[] body = resp.getBody();
        if (body == null || body.length == 0) {
            OktaResponse empty = new OktaResponse();
            empty.setError(HTTP_APPEND + statusCode);
            ZimbraLog.account.error("Response body is empty");
            return empty;
        }

        try {
            OktaResponse parsedResponse = JsonUtilities.read(body, OktaResponse.class);
            if (parsedResponse == null) {
                throw ServiceException.FAILURE("Okta response was empty or could not be parsed into JSON", null);
            }
            parsedResponse.setHttpStatusCode(statusCode);
            if (statusCode >= 400) {
                if (parsedResponse.getError() == null) {
                    String rawBody = new String(body, StandardCharsets.UTF_8);
                    parsedResponse.setError(HTTP_APPEND + statusCode);
                    parsedResponse.setErrorDescription(rawBody);
                }
            }
            return parsedResponse;
        } catch (Exception e) {
            String rawBody = new String(body, StandardCharsets.UTF_8);
            String safeLogBody = rawBody.replaceAll("[\\r\\n]+", ";");
            int maxLogLength = MAX_LOG_LENGTH;
            if (safeLogBody.length() > maxLogLength) {
                safeLogBody = safeLogBody.substring(0, maxLogLength) + "....[TRUNCATED]";
            }
            ZimbraLog.account.debug("Failed to parse Okta response JSON.StatusCode : " +
                    "%d , Error : %s", statusCode, safeLogBody);
            throw ServiceException.FAILURE("Invalid JSON response received " +
                    "from Okta IDP (HTTP " + statusCode + ")", null);
        }
    }

    private static long expiryMillis(OktaResponse r) {
        try {
            return r.getExpiresIn() == null ? ACCESS_EXPIRY_DEFAULT :
                    System.currentTimeMillis() + Long.parseLong(r.getExpiresIn()) * CONVERT_TO_MILLI;
        } catch (Exception e) {
            return ACCESS_EXPIRY_DEFAULT;
        }
    }

    private static boolean isMfaRequired(OktaResponse r) {
        return (r.getMfaToken() != null && !r.getMfaToken().isEmpty())
                || (r.getOobCode() != null && !r.getOobCode().isEmpty())
                || MFA_REQUIRED.equals(r.getError());
    }

    private static String getSafeString(String s) {
        return s == null ? "" : s;
    }
}
