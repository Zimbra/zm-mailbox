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
