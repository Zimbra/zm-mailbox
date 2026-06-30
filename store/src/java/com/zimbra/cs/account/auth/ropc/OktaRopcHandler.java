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
 * ROPC handler implementation for Okta.
 */
public class OktaRopcHandler implements IRopcHandler {

    public static final String NAME = "okta";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean authenticate(IRopcAuthRequest request) throws SocketTimeoutException {
        // Stub implementation for this iteration.
        return true;
    }
}
