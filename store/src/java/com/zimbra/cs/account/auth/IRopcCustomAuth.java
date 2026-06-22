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

package com.zimbra.cs.account.auth;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import java.util.List;
import java.util.Map;

public class IRopcCustomAuth extends ZimbraCustomAuth {

    private static final String EXTENSION_NAME = "idp-ropc";

    @Override
    public void authenticate(Account acct, String password,
                             Map<String, Object> context, List<String> args) throws Exception {

        String user = acct.getName();
        Protocol proto = (Protocol) context.get(AuthContext.AC_PROTOCOL);
        String deviceId = (String) context.get(AuthContext.AC_DEVICE_ID);

        ZimbraLog.account.debug("%s auth mech initiated for user : %s", EXTENSION_NAME, user);

        boolean isAuthenticated = executeRopcAuth();

        if (isAuthenticated) {
            ZimbraLog.account.debug("Auth successful for user %s", user);
        } else {
            ZimbraLog.account.debug("Auth failed for user %s", user);
        }
    }

    protected boolean executeRopcAuth() {
        return true;
    }

    @Override
    public boolean checkPasswordAging() {
        return false;
    }
}