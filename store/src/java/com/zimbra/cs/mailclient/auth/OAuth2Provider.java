/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailclient.auth;

import java.security.Provider;

public final class OAuth2Provider extends Provider {

    private static final long serialVersionUID = 1L;
    private static final String ZIMBRA_OAUTH2_PROVIDER = "Zimbra OAuth2 SASL Client";
    private static final String ZIMBRA_OAUTH2_PROVIDER_KEY = "SaslClientFactory.XOAUTH2";
    private static final String ZIMBRA_OAUTH2_PROVIDER_VALUE = "com.zimbra.cs.mailclient.auth.OAuth2SaslClientFactory";

    public OAuth2Provider(int version) {
        super(ZIMBRA_OAUTH2_PROVIDER, version, "Provides XOAUTH2 SASL Mechanism");
        put(ZIMBRA_OAUTH2_PROVIDER_KEY, ZIMBRA_OAUTH2_PROVIDER_VALUE);
    }
}
