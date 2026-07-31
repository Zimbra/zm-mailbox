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

package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;

/**
 * Fault codes specific to the SAML test flow, so the admin console can distinguish the
 * "already testing" and "missing test origin" conditions and react (e.g. prompt to force-clear).
 */
public class SamlServiceException extends ServiceException {

    private static final long serialVersionUID = 1L;

    /** A SAML test is already in progress (unexpired {@code zimbraSamlTestNonce}). */
    public static final String TEST_IN_PROGRESS = "saml.TEST_IN_PROGRESS";
    /** No test origin could be resolved ({@code zimbraWebClientLoginURL}/{@code zimbraPublicServiceHostname}). */
    public static final String TEST_ORIGIN_MISSING = "saml.TEST_ORIGIN_MISSING";

    private SamlServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }

    public static SamlServiceException TEST_IN_PROGRESS(String message) {
        return new SamlServiceException(message, TEST_IN_PROGRESS, SENDERS_FAULT);
    }

    public static SamlServiceException TEST_ORIGIN_MISSING(String message) {
        return new SamlServiceException(message, TEST_ORIGIN_MISSING, SENDERS_FAULT);
    }
}
