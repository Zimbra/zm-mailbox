/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.mailclient.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;

import com.zimbra.cs.mailclient.MailConfig;

public class OAuth2Authenticator extends Authenticator {
    private MailConfig config;
    private String oauthToken;
    private CallbackHandler callbackHandler;
    private boolean isComplete = false;
    
    public static final String XOAUTH2 = "XOAUTH2";

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailclient.auth.Authenticator#init(com.zimbra.cs.mailclient.MailConfig, java.lang.String)
     */
    @Override
    public void init(MailConfig config, String password) {
        this.config = config;
        this.oauthToken = password;
        checkRequired("mechanism", config.getMechanism());
        checkRequired("host", config.getHost());
        checkRequired("protocol", config.getProtocol());
        checkRequired("authentication id", config.getAuthenticationId());
        callbackHandler = new OAuth2CallbackHandler();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailclient.auth.Authenticator#evaluateChallenge(byte[])
     */
    @Override
    public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
        if (isComplete) {
            return new byte[] {};
        }

        NameCallback nameCallback = new NameCallback("Enter name");
        Callback[] callbacks = new Callback[] { nameCallback };
        try {
            callbackHandler.handle(callbacks);
        } catch (UnsupportedCallbackException e) {
            throw new SaslException("Unsupported callback", e);
        } catch (IOException e) {
            throw new SaslException("Failed to execute callback", e);
        }
        String username = nameCallback.getName();

        byte[] response = String.format("user=%s\1auth=Bearer %s\1\1", username, oauthToken).getBytes();
        isComplete = true;
        return response;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailclient.auth.Authenticator#getMechanism()
     */
    @Override
    public String getMechanism() {
        return config.getMechanism();
    }
    
    private static void checkRequired(String name, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required " + name);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.mailclient.auth.Authenticator#isComplete()
     */
    @Override
    public boolean isComplete() {
        return isComplete;
    }
    
    @Override
    public byte[] getInitialResponse() throws SaslException {
        return evaluateChallenge(new byte[0]);
    }

    @Override
    public boolean hasInitialResponse() {
        return true;
    }
    
    private class OAuth2CallbackHandler implements CallbackHandler {

        @Override
        public void handle(Callback[] cbs) throws UnsupportedCallbackException {
            for (Callback cb : cbs) {
                if (cb instanceof NameCallback) {
                    ((NameCallback) cb).setName(config.getAuthenticationId());
                } else {
                    throw new UnsupportedCallbackException(cb);
                }
            }
        }
    }

}
