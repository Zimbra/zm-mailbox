/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
