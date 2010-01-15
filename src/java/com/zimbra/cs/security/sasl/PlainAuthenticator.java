/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
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

package com.zimbra.cs.security.sasl;

import javax.security.sasl.SaslServer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class PlainAuthenticator extends Authenticator {
    public static final String MECHANISM = "PLAIN";
    
    public PlainAuthenticator(AuthenticatorUser user) {
        super(MECHANISM, user);
    }

    // RFC 2595 6: "The PLAIN SASL mechanism MUST NOT be advertised or used
    //              unless a strong encryption layer (such as the provided by TLS)
    //              is active or backwards compatibility dictates otherwise."
    @Override protected boolean isSupported() {
        return mAuthUser.isSSLEnabled() || mAuthUser.allowCleartextLogin();
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
        authenticate(authorizeId, authenticateId, password);
    }

    @Override public Account authenticate(String username, String authenticateId, String password,
                                          AuthContext.Protocol protocol, String origRemoteIp, String userAgent)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account authAccount = prov.get(Provisioning.AccountBy.name, authenticateId);
        if (authAccount == null) {
            ZimbraLog.account.info("authentication failed for " + authenticateId + " (no such account)");
            return null;
        }

        // authenticate the authentication principal
        Map<String, Object> authCtxt = new HashMap<String, Object>();
        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, origRemoteIp);
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, authenticateId);
        authCtxt.put(AuthContext.AC_USER_AGENT, userAgent);
        prov.authAccount(authAccount, password, protocol, authCtxt);

        return authorize(authAccount, username, true);
    }
}
