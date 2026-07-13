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
import java.util.Map;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.INTERVAL;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.POLLING_TIMEOUT;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.PROVIDER;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CLIENT_ID;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.TOKEN_ENDPOINT;

public final class IRopcAuthEngine {

    private IRopcAuthEngine() {
    }

    public static Outcome authenticate(final String user, final String password, final String deviceId,
                                       AuthContext.Protocol protocol, Map<String, String> configs) {
        return doAuthenticate(user, password, deviceId, protocol, configs);

    }

    private static Outcome doAuthenticate(String user, String password, String deviceId,
                                          AuthContext.Protocol protocol, Map<String, String> configs) {
        try {
            verifyMandatoryParamsforAuth(user, password, configs);

            IRopcHandler handler = IROPCHandlerRegistry.get(configs.get(PROVIDER));

            return fullAuth(handler, user, password, deviceId, protocol, configs);

        } catch (Exception e) {
            ZimbraLog.account.error("Authentication Failed : Error occurred in IDP based auth for %s", user, e);
            return Outcome.ERROR;
        }
    }

    private static Outcome fullAuth(IRopcHandler handler, String user, String password, String deviceId,
                                    AuthContext.Protocol protocol, Map<String, String> configs)
            throws ServiceException {
        IRopcAuthResult ar = handler.authenticate(IRopcAuthRequest.builder()
                .username(user).password(password)
                .deviceId(deviceId).protocol(protocol).config(configs).build());

        switch (ar.getStatus()) {
            case SUCCESS:
                ZimbraLog.account.info("Authentication SUCCESSFUL for user :  %s", user);
                return Outcome.SUCCESS;
            case MFA_CHALLENGE:
                return awaitPush(handler, ar.getChallenge(), user, password);
            case INVALID_CREDENTIALS:
                ZimbraLog.account.error("ROPC failed due to invalid credentials for user %s: %s", user,
                        ar.getErrorDescription());
                return Outcome.INVALID;
            case POLICY_DENIED:
                ZimbraLog.account.error("ROPC policy denied for %s: %s", user, ar.getErrorDescription());
                return Outcome.POLICY_DENIED;
            default:
                ZimbraLog.account.error("ROPC error for %s: %s / %s", user, ar.getErrorCode(),
                        ar.getErrorDescription());
                return Outcome.ERROR;
        }
    }

    private static Outcome awaitPush(IRopcHandler handler, MFAChallenge challenge,
                                     String user, String password) throws ServiceException {
        MFAPollResult pollResult;
        long startTime = System.currentTimeMillis();
        // TODO : set the value of polling_timeout in MFAChallenge in buildPushChallenge
        long timeoutDuration = 30 * 1000L;
        String timeoutStr = challenge.get(POLLING_TIMEOUT);
        if (timeoutStr != null && !timeoutStr.trim().isEmpty()) {
            try {
                timeoutDuration = Long.parseLong(challenge.get(POLLING_TIMEOUT)) * 1000L;
            } catch (NumberFormatException e) {

            }
        }

        long pollingInterval = 2 * 1000L;
        String pollingStr = challenge.get(INTERVAL);
        if (pollingStr != null && !pollingStr.trim().isEmpty()) {
            try {
                pollingInterval = Long.parseLong(challenge.get(INTERVAL)) * 1000L;
            } catch (NumberFormatException e) {

            }
        }

        do {
            pollResult = handler.pollChallenge(challenge);
            switch (pollResult) {
                case WAITING:
                    ZimbraLog.account.debug("MFA polling : WAITING for user %s to approve challenge.", user);
                    try {
                        Thread.sleep(pollingInterval);
                    } catch (InterruptedException e) {
                        // exception
                    }
                    break;
                case SUCCESS:
                    ZimbraLog.account.info("MFA polling : SUCCESS. Push Challenge approved for user %s", user);
                    return Outcome.SUCCESS;
                case REJECTED:
                    ZimbraLog.account.error("MFA polling : REJECTED. Push Challenge denied by user %s", user);
                    return Outcome.REJECTED;
                case EXPIRED:
                    ZimbraLog.account.error("MFA polling : EXPIRED. PUSH challenge was not approved in " +
                            "defined approval window for user  %s", user);
                    return Outcome.MFA_TIMEOUT;
                default:
                    ZimbraLog.account.error("MFA polling : ERROR. Encountered unexpected polling status " +
                            "for user  %s", user);
                    return Outcome.ERROR;
            }
        } while (System.currentTimeMillis() - startTime < timeoutDuration);

        ZimbraLog.account.error("Authentication Failed : Push approval window timeout out for user %s ", user);
        return Outcome.MFA_TIMEOUT;
    }

    private static void verifyMandatoryParamsforAuth(String username, String password, Map<String, String> configs)
            throws ServiceException {
        if (username == null || username.trim().isEmpty() || !username.contains("@")) {
            throw ServiceException.FAILURE("Authentication Failed : Missing or invalid username", null);
        }
        if (password == null || password.trim().isEmpty()) {
            throw ServiceException.FAILURE("Authentication Failed : missing or invalid password provided", null);
        }

        if (configs.get(TOKEN_ENDPOINT) == null || configs.get(TOKEN_ENDPOINT).isEmpty()) {
            throw ServiceException.FAILURE("Authentication configuration error : Missing required " +
                    "parameter 'token_endpoint'", null);
        }

        if (configs.get(REQUEST_PARAM_CLIENT_ID) == null || configs.get(REQUEST_PARAM_CLIENT_ID).isEmpty()) {
            throw ServiceException.FAILURE("Authentication configuration error : Missing required " +
                    "parameter 'client_id'", null);
        }

        if (configs.get(PROVIDER) == null || configs.get(PROVIDER).isEmpty()) {
            throw ServiceException.FAILURE("Authentication configuration error : Missing required " +
                    "parameter 'provider'", null);
        }
    }
}
