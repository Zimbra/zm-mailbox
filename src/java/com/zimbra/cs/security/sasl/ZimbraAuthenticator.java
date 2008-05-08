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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.security.sasl.SaslServer;

public class ZimbraAuthenticator extends Authenticator {
    public static final String MECHANISM = "ZIMBRA";

    public ZimbraAuthenticator(AuthenticatorUser user) {
        super(MECHANISM, user);
    }

    @Override public boolean initialize()  { return true; }
    @Override public void dispose()        { }

    @Override public boolean isEncryptionEnabled()  { return false; }

    @Override public InputStream unwrap(InputStream is)  { return null; }
    @Override public OutputStream wrap(OutputStream os)  { return null; }

    @Override public SaslServer getSaslServer()  { return null; }

    @Override public void handle(byte[] data) throws IOException {
        if (isComplete())
            throw new IllegalStateException("Authentication already completed");

        String message = new String(data, "utf-8");

        int nul = message.indexOf('\0');
        if (nul == -1) {
            sendBadRequest();
            return;
        }

        String username = message.substring(0, nul);
        String authtoken = message.substring(nul + 1);

        authenticate(username, null, authtoken);
    }
}
