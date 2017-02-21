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
