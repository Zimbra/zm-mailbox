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
import java.io.IOException;

public interface IRopcWebhookHandler {

    /**
     * Returns the exact provider name used in the zimbraAuthMech ARGS (eg okta, keycloak).
     *
     * @return String
     */
    String getProviderName();

    /**
     * Parses the provider specific payload (JSON, JWT, etc.) and returns the target user's
     * email address.
     * Returns null if the payload is not recognized or invalid for this provider.
     *
     * @param rawPayload
     * @return String
     */
    String extractUsername(String rawPayload);

    /**
     * Handle provider-specific one time verification challenge.
     *
     * @param request
     * @param  response
     */
    void handleVerificationChallenge(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
