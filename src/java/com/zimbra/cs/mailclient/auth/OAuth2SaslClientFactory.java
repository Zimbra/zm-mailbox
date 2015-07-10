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

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;

public class OAuth2SaslClientFactory implements SaslClientFactory {

    public SaslClient createSaslClient(String[] mechanisms, String authorizationId,
        String protocol, String serverName, Map<String, ?> props, CallbackHandler callbackHandler) {
        boolean matchedMechanism = false;
        for (int i = 0; i < mechanisms.length; ++i) {
            if (SaslAuthenticator.XOAUTH2.equalsIgnoreCase(mechanisms[i])) {
                matchedMechanism = true;
                break;
            }
        }
        if (!matchedMechanism) {
            return null;
        }
        return new OAuth2SaslClient((String) props.get("mail." + protocol
            + ".sasl.mechanisms.oauth2.oauthToken"), callbackHandler);
    }

    public String[] getMechanismNames(Map<String, ?> props) {
        return new String[] { SaslAuthenticator.XOAUTH2 };
    }
}
