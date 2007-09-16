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

package com.zimbra.cs.imap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.krb5.Krb5Login;
import org.apache.commons.codec.binary.Base64;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class GssAuthenticator extends Authenticator {
    private SaslServer mSaslServer;
    private LoginContext mLoginContext;

    private static final String MECHANISM = "GSSAPI";
    private static final String PROTOCOL = "imap";

    private static final boolean DEBUG = true;

    static {
        if (DEBUG) {
            System.setProperty("sun.security.krb5.debug", "true");
            System.setProperty("sun.security.jgss.debug", "true");
        }
    }
    
    GssAuthenticator(ImapHandler handler, String tag) {
        super(handler, tag, Mechanism.GSSAPI);
    }                                    

    public boolean initialize() throws IOException {
        final String host = LC.zimbra_server_hostname.value();
        String principle = PROTOCOL + "/" + host;
        debug("kerberos principle = %s", principle);
        File keytab = new File(LC.krb5_keytab.value());
        if (!keytab.exists()) {
            sendFailed();
            ZimbraLog.imap.warn("Keytab file '" + keytab + "' not found");
            return false;
        }
        try {
            mLoginContext = Krb5Login.withKeyTab(principle, keytab.getPath());
            mLoginContext.login();
        } catch (LoginException e) {
            sendFailed();
            ZimbraLog.imap.warn("Login failed", e);
            return false;
        }
        try {
            mSaslServer = (SaslServer) Subject.doAs(mLoginContext.getSubject(),
                new PrivilegedExceptionAction() {
                    public Object run() throws SaslException {
                        return Sasl.createSaslServer(
                            MECHANISM, PROTOCOL, host, null,
                            new GssCallbackHandler());
                    }
                });
        } catch (PrivilegedActionException e) {
            sendFailed();
            ZimbraLog.imap.warn("Could not create SaslServer", e.getCause());
            logout();
            return false;
        }
        return true;
    }
    
    public void handle(final byte[] data) throws IOException {
        if (isComplete()) {
            throw new IllegalStateException("Authentication already completed");
        }
        byte[] bytes;
        try {
            bytes = mSaslServer.evaluateResponse(data);
        } catch (SaslException e) {
            ZimbraLog.imap.warn("SaslServer.evaluateResponse() failed", e);
            sendBadRequest();
            logout();
            return;
        }
        if (isComplete()) {
            // Authentication successful
            logout();
        } else {
            assert !mSaslServer.isComplete();
            String s = new String(Base64.encodeBase64(bytes), "utf-8");
            mHandler.sendContinuation(s);
        }
    }

    private void logout() {
        try {
            mLoginContext.logout();
        } catch (LoginException e) {
            ZimbraLog.imap.warn("Logout unsuccessful", e);
        }
    }
    
    private class GssCallbackHandler implements CallbackHandler {
        public void handle(Callback[] cbs)
                throws IOException, UnsupportedCallbackException {
            if (cbs == null || cbs.length != 1) {
                throw new IOException("Bad callback");
            }
            if (!(cbs[0] instanceof AuthorizeCallback)) {
                throw new UnsupportedCallbackException(cbs[0]);
            }
            AuthorizeCallback cb = (AuthorizeCallback) cbs[0];
            debug("gss authorization_id = %s", cb.getAuthorizationID());
            debug("gss authentication_id = %s", cb.getAuthenticationID());
            cb.setAuthorized(
                login(cb.getAuthorizationID(), cb.getAuthenticationID(), null));
        }
    }

    private static void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG] " + format + "\n", args);
        }
    }
}
