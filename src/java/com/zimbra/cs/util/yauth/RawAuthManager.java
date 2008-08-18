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
        throws AuthenticationException, IOException {
        RawAuth auth = cookies.get(key(appId, user));
        if (auth == null || auth.isExpired()) {
            // Cookie missing or expired, so get a new one
            String token = store.getToken(appId, user);
            if (token != null) {
                try {
                    auth = RawAuth.authenticate(appId, token);
                } catch (AuthenticationException e) {
                    // Token invalid...
                    if (pass == null) {
                        throw e; // Cannot obtain a new token w/o password
                    }
                    auth = null;
                }
            }
            if (auth == null) {
                // Token invalid or missing, so get a new one
                auth = RawAuth.authenticate(appId, newToken(appId, user, pass));
            }
            cookies.put(key(appId, user), auth);
        }
        return auth;
    }

    private String newToken(String appId, String user, String pass)
        throws AuthenticationException, IOException {
        LOG.debug("newToken: appId=" + appId + ", user=" + user);
        String token = RawAuth.getToken(appId, user, pass);
        store.putToken(appId, user, token);
        return token;
    }

    private String key(String appId, String user) {
        return appId + " " + user;
    }

    public String toString() {
        return String.format("{cookies=%d,tokens=%d}", cookies.size(), store.size());
    }
}
