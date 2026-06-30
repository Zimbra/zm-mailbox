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

import com.zimbra.common.util.ZimbraLog;
import java.net.SocketTimeoutException;

public final class IRopcAuthEngine {

    private IRopcAuthEngine() {
    }

    public static Outcome authenticate(String username,
            String password,
            String provider,
            String factorConfig,
            String protocolContext,
            String deviceId) {

        try {
            IRopcAuthRequest.FactorType factorType;
            try {
                factorType = IRopcAuthRequest.FactorType.fromConfig(factorConfig);
            } catch (IllegalArgumentException e) {
                ZimbraLog.account.warn(
                        "ROPC auth: unsupported factor type '%s' for user=%s",
                        factorConfig, username);
                ZimbraLog.account.info("Authentication outcome: ERROR for user: %s", username);
                return Outcome.ERROR;
            }

            ZimbraLog.account.debug(
                    "ROPC auth: resolving provider=%s for user=%s, factorType=%s",
                    provider, username, factorType);

            IRopcHandler handler = IROPCHandlerRegistry.get(provider);

            IRopcAuthRequest request = new IRopcAuthRequest.Builder()
                    .username(username)
                    .password(password)
                    .provider(provider)
                    .protocolContext(protocolContext)
                    .factorType(factorType)
                    .build();

            ZimbraLog.account.info("Invoking handler: %s for user: %s",
                    handler.getName(), username);

            Boolean result = handler.authenticate(request);

            if (result == null) {
                ZimbraLog.account.warn(
                        "ROPC auth: handler returned null for user: %s, provider: %s",
                        username, provider);
                ZimbraLog.account.info("Authentication outcome: ERROR for user: %s", username);
                return Outcome.ERROR;
            }

            Outcome outcome;
            if (result) {
                outcome = Outcome.SUCCESS;
            } else {
                outcome = Outcome.INVALID;
            }

            ZimbraLog.account.info("Authentication outcome: %s for user: %s",
                    outcome, username);
            return outcome;

        } catch (SocketTimeoutException e) {
            ZimbraLog.account.error(
                    "ROPC auth timeout for user: %s, provider: %s — %s",
                    username, provider, e.getMessage(), e);
            ZimbraLog.account.info("Authentication outcome: TIMEOUT for user: %s", username);
            return Outcome.MFA_TIMEOUT;

        } catch (Exception e) {
            ZimbraLog.account.error(
                    "ROPC auth error for user: %s, provider: %s — %s",
                    username, provider, e.getMessage(), e);
            ZimbraLog.account.info("Authentication outcome: ERROR for user: %s", username);
            return Outcome.ERROR;
        }
    }
}
