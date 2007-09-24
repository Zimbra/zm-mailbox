/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
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
    
    public String getProtocol() { return "pop"; }

    public void sendBadRequest(String s) throws IOException {
        mHandler.sendERR(s);
    }

    public void sendFailed(String s) throws IOException {
        mHandler.sendERR(s);
    }

    public void sendSuccessful(String s) throws IOException {
        mHandler.sendOK(s);
    }

    public void sendContinuation(String s) throws IOException {
        mHandler.sendContinuation(s);
    }

    public boolean authenticate(String authorizationId,
                                String authenticationId,
                                String password,
                                Authenticator auth) throws IOException {
        try {
            mHandler.authenticate(authorizationId, password, auth.getMechanism());
        } catch (Pop3CmdException e) {
            auth.sendFailed(e.getMessage());
            return false;
        }
        return true;
    }

    public Log getLog() { return ZimbraLog.pop; }

    public boolean isSSLEnabled() { return mHandler.isSSLEnabled(); }
}
