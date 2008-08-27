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

public abstract class TokenStore {
    public static final Logger LOG = Logger.getLogger(TokenStore.class);
    
    public String newToken(String appId, String user, String pass)
        throws IOException {
        LOG.debug("newToken: appId=" + appId + ", user=" + user);
        String token = RawAuth.getToken(appId, user, pass);
        putToken(appId, user, token);
        return token;
    }

    public boolean hasToken(String appId, String user) {
        return getToken(appId, user) != null;
    }

    protected abstract void putToken(String appId, String user, String token);
    public abstract String getToken(String appId, String user);
    public abstract int size();
}
