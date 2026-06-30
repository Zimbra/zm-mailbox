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

import java.net.SocketTimeoutException;

/**
 * Contract for all ROPC provider handlers.
 */
public interface IRopcHandler {

    /**
     * Returns the unique provider name/type for this handler.
     * okta, keycloak
     *  @return provider name
     */
    String getName();

    /**
     * Authenticates the user via ROPC flow.
     * @param request the authentication request
     * @return true if authentication is successful, false otherwise   ← this line was added
     */
    boolean authenticate(IRopcAuthRequest request) throws SocketTimeoutException;
}
