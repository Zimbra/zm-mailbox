/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.imap;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.security.sasl.Authenticator;
import com.zimbra.cs.security.sasl.AuthenticatorUser;

import java.io.IOException;

class ImapAuthenticatorUser implements AuthenticatorUser {
    private final ImapHandler mHandler;
    private final String mTag;
    private boolean canContinue = true;

    ImapAuthenticatorUser(ImapHandler handler, String tag) {
        mHandler = handler;
        mTag = tag;
    }

    String getTag()  { return mTag; }

    boolean canContinue()  { return canContinue; }

    public String getProtocol()  { return "imap"; }
    
    public void sendBadRequest(String s) throws IOException {
        mHandler.sendBAD(mTag, s);
    }

    public void sendFailed() throws IOException {
        mHandler.sendNO(mTag, "AUTHENTICATE failed");
    }

    public void sendFailed(String msg) throws IOException {
        mHandler.sendNO(mTag, "AUTHENTICATE failed: " + msg);
    }

    public void sendSuccessful() throws IOException {
        // 6.2.2: "A server MAY include a CAPABILITY response code in the tagged OK
        //         response of a successful AUTHENTICATE command in order to send
        //         capabilities automatically."
        mHandler.sendOK(mTag, '[' + mHandler.getCapabilityString() + "] AUTHENTICATE completed");
    }

    public void sendContinuation(String s) throws IOException {
        mHandler.sendContinuation(s);
    }

    public boolean authenticate(String authorizationId, String authenticationId, String password, Authenticator auth)
    throws IOException {
        canContinue = mHandler.authenticate(authorizationId, authenticationId, password, mTag, auth);
        return mHandler.isAuthenticated();
    }

    public Log getLog() {
        return ZimbraLog.imap;
    }

    public boolean isSSLEnabled() {
        return mHandler.isSSLEnabled();
    }

    public boolean allowCleartextLogin() {
        return mHandler.getConfig().isCleartextLoginEnabled();
    }

    public boolean isGssapiAvailable() {
        return mHandler.getConfig().isSaslGssapiEnabled();
    }
}
