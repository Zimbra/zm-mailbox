/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util.yauth;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;

public final class RawAuthManager {
    private final TokenStore store;
    private final HashMap<String, RawAuth> cookies;

    private static final Logger LOG = Logger.getLogger(RawAuthManager.class);

    public RawAuthManager(TokenStore store) {
        this.store = store;
        cookies = new HashMap<String, RawAuth>();
    }

    public RawAuth authenticate(String appId, String user, String pass)
        throws IOException {
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
            public RawAuth authenticate() throws IOException {
                return RawAuthManager.this.authenticate(appId, user, pass);
            }
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

    public String toString() {
        return String.format("{cookies=%d,tokens=%d}", cookies.size(), store.size());
    }
}
