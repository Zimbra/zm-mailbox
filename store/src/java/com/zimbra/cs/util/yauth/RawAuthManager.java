/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util.yauth;


import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RawAuthManager {
    private final TokenStore store;
    private final HashMap<String, RawAuth> cookies;

    private static final Logger LOG = LogManager.getLogger(RawAuthManager.class);

    public RawAuthManager(TokenStore store) {
        this.store = store;
        cookies = new HashMap<String, RawAuth>();
    }

    public RawAuth authenticate(String appId, String user, String pass)
        throws AuthenticationException, IOException {
        RawAuth auth = cookies.get(key(appId, user));
        if (auth == null || auth.isExpired()) {
            // Cookie missing or expired, so get a new one
            String token = store.getToken(appId, user);
            if (token == null) {
                // If token not found, generating a new one
                token = store.newToken(appId, user, pass);
            }
            try {
                auth = RawAuth.authenticate(appId, token);
            } catch (AuthenticationException e) {
                // If authentication failed, check for invalid token in which
                // case we will generate a new one and try again...
                switch (e.getErrorCode()) {
                case TOKEN_REQUIRED:
                case INVALID_TOKEN:
                    invalidate(appId, user);
                    token = store.newToken(appId, user, pass);
                    auth = RawAuth.authenticate(appId, token);
                    break;
                default:
                    throw e;
                }

            }
            cookies.put(key(appId, user), auth);
        }
        return auth;
    }

    public void invalidate(String appId, String user) {
        cookies.remove(key(appId, user));
    }
    
    public Authenticator newAuthenticator(final String appId,
                                          final String user,
                                          final String pass) {
        return new Authenticator() {
            @Override
            public RawAuth authenticate() throws AuthenticationException, IOException {
                return RawAuthManager.this.authenticate(appId, user, pass);
            }
            
            @Override
            public void invalidate() {
                RawAuthManager.this.invalidate(appId, user);
            }
        };
    }

    public TokenStore getTokenStore() {
        return store;
    }
    
    private String key(String appId, String user) {
        return appId + " " + user;
    }

    @Override
    public String toString() {
        return String.format("{cookies=%d,tokens=%d}", cookies.size(), store.size());
    }
}
