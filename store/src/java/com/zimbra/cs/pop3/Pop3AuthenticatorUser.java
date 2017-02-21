/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.pop3;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.security.sasl.Authenticator;
import com.zimbra.cs.security.sasl.AuthenticatorUser;

import java.io.IOException;

class Pop3AuthenticatorUser implements AuthenticatorUser {
    private final Pop3Handler mHandler;

    Pop3AuthenticatorUser(Pop3Handler handler) {
        mHandler = handler;
    }

    @Override
    public String getProtocol()  { return "pop"; }

    @Override
    public void sendBadRequest(String s) throws IOException {
        mHandler.sendERR(s);
    }

    @Override
    public void sendFailed() throws IOException {
        mHandler.sendERR("authentication failed");
    }

    @Override
    public void sendFailed(String msg) throws IOException {
        mHandler.sendERR("authentication failed: " + msg);
    }

    @Override
    public void sendSuccessful() throws IOException {
        mHandler.sendOK("authentication successful");
    }

    @Override
    public void sendContinuation(String s) throws IOException {
        mHandler.sendContinuation(s);
    }

    @Override
    public boolean authenticate(String authorizationId, String authenticationId, String password, Authenticator auth)
    throws IOException {
        try {
            mHandler.authenticate(authorizationId, authenticationId, password, auth);
        } catch (Pop3CmdException e) {
            auth.sendFailed(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public Log getLog() {
        return ZimbraLog.pop;
    }

    @Override
    public boolean isSSLEnabled() {
        return mHandler.isSSLEnabled();
    }

    @Override
    public boolean allowCleartextLogin() {
        return mHandler.config.isCleartextLoginsEnabled();
    }

    @Override
    public boolean isGssapiAvailable() {
        return mHandler.config.isSaslGssapiEnabled();
    }
}
