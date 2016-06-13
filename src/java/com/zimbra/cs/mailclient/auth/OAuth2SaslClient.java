/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

/**
 * An OAuth2 implementation of SaslClient.
 */
class OAuth2SaslClient implements SaslClient {

    private final String oauthToken;
    private final CallbackHandler callbackHandler;

    private boolean isComplete = false;

    /**
     * Creates a new instance of the OAuth2SaslClient. This will ordinarily only
     * be called from @OAuth2SaslClientFactory OR from @SaslAuthenticator.
     */
    public OAuth2SaslClient(String oauthToken, CallbackHandler callbackHandler) {
        this.oauthToken = oauthToken;
        this.callbackHandler = callbackHandler;
    }

    public String getMechanismName() {
        return SaslAuthenticator.XOAUTH2;
    }

    public boolean hasInitialResponse() {
        return true;
    }

    public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
        if (isComplete) {
            // Empty final response from server, just ignore it.
            return new byte[] {};
        }

        NameCallback nameCallback = new NameCallback("Enter name");
        Callback[] callbacks = new Callback[] { nameCallback };
        try {
            callbackHandler.handle(callbacks);
        } catch (UnsupportedCallbackException e) {
            throw new SaslException("Unsupported callback: " + e);
        } catch (IOException e) {
            throw new SaslException("Failed to execute callback: " + e);
        }
        String username = nameCallback.getName();

        byte[] response = String.format("user=%s\1auth=Bearer %s\1\1", username, oauthToken)
            .getBytes();
        isComplete = true;
        return response;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
        throw new IllegalStateException();
    }

    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
        throw new IllegalStateException();
    }

    public Object getNegotiatedProperty(String propName) {
        if (!isComplete()) {
            throw new IllegalStateException();
        }
        return null;
    }

    public void dispose() throws SaslException {
    }
}
