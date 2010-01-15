/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.auth;

import com.zimbra.cs.mailclient.MailConfig;

import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * Base class for authenticator implementations.
 */
public abstract class Authenticator {
    protected Authenticator() {}

    public abstract void init(MailConfig config, String password)
        throws LoginException, SaslException;

    public abstract byte[] evaluateChallenge(byte[] challenge) throws SaslException;

    public abstract String getMechanism();

    public abstract boolean isComplete();

    public boolean hasInitialResponse() {
        return false;
    }

    public byte[] getInitialResponse() throws SaslException {
        return null;
    }

    public boolean isEncryptionEnabled() {
        return false;
    }

    public OutputStream wrap(OutputStream os) {
        return null;
    }

    public InputStream unwrap(InputStream is) {
        return null;
    }

    public String getNegotiatedProperty(String name) {
        return null;
    }

    public void dispose() throws SaslException {}
}
