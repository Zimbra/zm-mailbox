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
import com.zimbra.cs.mina.MinaServer;
import org.apache.commons.codec.binary.Base64;
import org.apache.mina.common.IoSession;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

public class GssAuthenticator extends Authenticator {
    private SaslServer mSaslServer;
    private LoginContext mLoginContext;

    private static final String MECHANISM = "GSSAPI";
    private static final String PROTOCOL = "imap";
    
    private static final String QOP_AUTH = "auth";
    private static final String QOP_AUTH_INT = "auth-int";
    private static final String QOP_AUTH_CONF = "auth-conf";

    private static final int MAX_RECEIVE_SIZE = 4096;
    private static final int MAX_SEND_SIZE = 4096;

    private static final boolean DEBUG = true;

    // SASL properties to enable encryption
    private static final Map<String, String> ENCRYPTION_PROPS =
        new HashMap<String, String>();
    
    static {
        ENCRYPTION_PROPS.put(Sasl.QOP, QOP_AUTH + "," + QOP_AUTH_INT + "," +
                                       QOP_AUTH_CONF);
        ENCRYPTION_PROPS.put(Sasl.MAX_BUFFER, String.valueOf(MAX_RECEIVE_SIZE));
        ENCRYPTION_PROPS.put(Sasl.RAW_SEND_SIZE, String.valueOf(MAX_SEND_SIZE));
        if (DEBUG) {
            System.setProperty("sun.security.krb5.debug", "true");
            System.setProperty("sun.security.jgss.debug", "true");
        }
    }
    
    GssAuthenticator(ImapHandler handler, String tag) {
        super(handler, tag, Mechanism.GSSAPI);
    }                                    

    @Override
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
        final Map<String, String> props = getSaslProperties();
        if (DEBUG && props != null) {
            String qop = props.get(Sasl.QOP);
            debug("Sent QOP = " + (qop != null ? qop : "auth"));
        }
        try {
            mSaslServer = (SaslServer) Subject.doAs(mLoginContext.getSubject(),
                new PrivilegedExceptionAction() {
                    public Object run() throws SaslException {
                        return Sasl.createSaslServer(
                            MECHANISM, PROTOCOL, host, props, new GssCallbackHandler());
                    }
                });
        } catch (PrivilegedActionException e) {
            sendFailed();
            e.printStackTrace();
            ZimbraLog.imap.warn("Could not create SaslServer", e.getCause());
            logout();
            return false;
        }
        return true;
    }

    protected Map<String, String> getSaslProperties() {
        // Don't enable encryption if SSL is being used
        return mHandler.isSSLEnabled() ? null : ENCRYPTION_PROPS;
    }
    
    @Override
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
            if (DEBUG) {
                for (String name : getSaslProperties().keySet()) {
                    debug("Negotiated property %s = %s", name,
                          mSaslServer.getNegotiatedProperty(name));
                }
            }
            logout();
        } else {
            assert !mSaslServer.isComplete();
            String s = new String(Base64.encodeBase64(bytes), "utf-8");
            mHandler.sendContinuation(s);
        }
    }

    @Override
    public boolean isEncryptionEnabled() {
        String qop = (String) mSaslServer.getNegotiatedProperty(Sasl.QOP);
        return QOP_AUTH_INT.equals(qop) || QOP_AUTH_CONF.equals(qop);
    }

    @Override                       
    public InputStream unwrap(InputStream is) {
        return new SaslInputStream(is, mSaslServer);
    }

    @Override
    public OutputStream wrap(OutputStream os) {
        return new SaslOutputStream(os, mSaslServer);
    }

    @Override
    public void addSaslFilter(IoSession session) {
        MinaServer.addSaslFilter(session, mSaslServer);
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
            System.out.printf("[DEBUG GssAuthenticator] " + format + "\n", args);
        }
    }
}
