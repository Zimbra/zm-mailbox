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

import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PlainAuthenticator extends Authenticator {
    public static final String MECHANISM = "PLAIN";
    
    public PlainAuthenticator(AuthenticatorUser user) {
        super(MECHANISM, user);
    }

    public boolean initialize() { return true; }
    public void dispose() {}
    public boolean isEncryptionEnabled() { return false; }
    public InputStream unwrap(InputStream is) { return null; }
    public OutputStream wrap(OutputStream os) { return null; }
    public SaslServer getSaslServer() { return null; }

    public void handle(byte[] data) throws IOException {
        if (isComplete())
            throw new IllegalStateException("Authentication already completed");

        // RFC 2595 6: "Non-US-ASCII characters are permitted as long as they are
        //              represented in UTF-8 [UTF-8]."
        String message = new String(data, "utf-8");

        // RFC 2595 6: "The client sends the authorization identity (identity to
        //              login as), followed by a US-ASCII NUL character, followed by the
        //              authentication identity (identity whose password will be used),
        //              followed by a US-ASCII NUL character, followed by the clear-text
        //              password.  The client may leave the authorization identity empty to
        //              indicate that it is the same as the authentication identity."
        int nul1 = message.indexOf('\0'), nul2 = message.indexOf('\0', nul1 + 1);
        if (nul1 == -1 || nul2 == -1) {
            sendBadRequest();
            return;
        }

        String authorizeId = message.substring(0, nul1);
        String authenticateId = message.substring(nul1 + 1, nul2);
        String password = message.substring(nul2 + 1);
        if (authorizeId.equals(""))
            authorizeId = authenticateId;
        authenticate(authorizeId, authenticateId, password);
    }
}
