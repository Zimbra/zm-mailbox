/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
