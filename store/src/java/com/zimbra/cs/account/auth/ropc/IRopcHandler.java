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

/**
 * Contract for all ROPC provider handlers (e.g. Okta).
 */
public interface IRopcHandler {

    /**
     * Returns the unique provider name for this handler (e.g. {@code "okta"}).
     *
     * @return provider name
     */
    String getName();

    /**
     * Authenticates the user via the ROPC flow.
     *
     * @param request the authentication request
     * @return {@link IRopcAuthResult} containing the outcome and tokens or error details
     * @throws ServiceException on unexpected errors during authentication
     */
    IRopcAuthResult authenticate(IRopcAuthRequest request) throws ServiceException;

    /**
     * Polls the IdP for the result of a pending MFA push challenge.
     *
     * @param challenge the active MFA challenge to poll
     * @return {@link MFAPollResult} indicating success, rejection, expiry, or error
     * @throws ServiceException on unexpected errors during polling
     */
    MFAPollResult pollChallenge(MFAChallenge challenge) throws ServiceException;
}
