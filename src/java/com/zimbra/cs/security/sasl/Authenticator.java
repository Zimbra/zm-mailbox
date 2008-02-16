/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.security.sasl;

import com.zimbra.common.util.Log;

import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Authenticator {
    protected final String mProtocol;
    protected final String mMechanism;
    protected final AuthenticatorUser mAuthUser;
    protected boolean mComplete;
    protected boolean mAuthenticated;

    protected Authenticator(String mechanism, AuthenticatorUser authUser) {
        mProtocol  = authUser.getProtocol();
        mMechanism = mechanism;
        mAuthUser  = authUser;
    }

    public abstract boolean initialize() throws IOException;

    public abstract void handle(byte[] data) throws IOException;

    public abstract boolean isEncryptionEnabled();

    public abstract InputStream unwrap(InputStream is);

    public abstract OutputStream wrap(OutputStream os);

    public abstract SaslServer getSaslServer();

    public abstract void dispose();
    
    public boolean isComplete() {
        return mComplete;
    }

    public boolean isAuthenticated() {
        return mAuthenticated;
    }

    public String getProtocol() {
        return mProtocol;
    }

    public String getMechanism() {
        return mMechanism;
    }

    public AuthenticatorUser getAuthenticatorUser() {
        return mAuthUser;
    }
    
    public void sendSuccess() throws IOException {
        mAuthUser.sendSuccessful();
    }
    
    public void sendFailed() throws IOException {
        mAuthUser.sendFailed();
        mComplete = true;
    }

    public void sendFailed(String msg) throws IOException {
        mAuthUser.sendFailed(msg);
    }
    
    public void sendBadRequest() throws IOException {
        mAuthUser.sendBadRequest("malformed authentication request");
        mComplete = true;
    }

    protected void sendContinuation(String s) throws IOException {
        mAuthUser.sendContinuation(s);
    }

    protected Log getLog() {
        return mAuthUser.getLog();
    }

    protected boolean authenticate(String authorizationId, String authenticationId, String password)
    throws IOException {
        mAuthenticated = mAuthUser.authenticate(authorizationId, authenticationId, password, this);
        mComplete = true;
        return mAuthenticated;
    }
}