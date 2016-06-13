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

public abstract class TokenStore {
    public String newToken(String appId, String user, String pass)
        throws AuthenticationException, IOException {
        removeToken(appId, user);
        String token = RawAuth.getToken(appId, user, pass);
        putToken(appId, user, token);
        return token;
    }

    public boolean hasToken(String appId, String user) {
        return getToken(appId, user) != null;
    }

    protected abstract void putToken(String appId, String user, String token);
    public abstract String getToken(String appId, String user);
    public abstract void removeToken(String appId, String user);
    public abstract int size();
}
