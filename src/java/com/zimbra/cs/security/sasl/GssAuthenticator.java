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

import com.zimbra.cs.security.kerberos.Krb5Keytab;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

import javax.security.sasl.SaslServer;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import org.apache.commons.codec.binary.Base64;

public class GssAuthenticator extends Authenticator {
    private SaslServer mSaslServer;
    private boolean mEncryptionEnabled;

    private static final String QOP_AUTH = "auth";
    private static final String QOP_AUTH_INT = "auth-int";
    private static final String QOP_AUTH_CONF = "auth-conf";

    private static final int MAX_RECEIVE_SIZE = 4096;
    private static final int MAX_SEND_SIZE = 4096;

    public static final String KRB5_DEBUG_ENABLED_PROP = "ZimbraKrb5DebugEnabled";
    
    private static final boolean DEBUG =
        LC.krb5_debug_enabled.booleanValue() || Boolean.getBoolean(KRB5_DEBUG_ENABLED_PROP);

    public static final String MECHANISM = "GSSAPI";
    
    // SASL properties to enable encryption
    private static final Map<String, String> ENCRYPTION_PROPS =
        new HashMap<String, String>();

    static {
        ENCRYPTION_PROPS.put(Sasl.QOP,
            QOP_AUTH + "," + QOP_AUTH_INT + "," + QOP_AUTH_CONF);
        ENCRYPTION_PROPS.put(Sasl.MAX_BUFFER, String.valueOf(MAX_RECEIVE_SIZE));
        ENCRYPTION_PROPS.put(Sasl.RAW_SEND_SIZE, String.valueOf(MAX_SEND_SIZE));
        if (DEBUG) {
            System.setProperty("sun.security.krb5.debug", "true");
            System.setProperty("sun.security.jgss.debug", "true");
        }
    }

    public GssAuthenticator(AuthenticatorUser user) {
        super(MECHANISM, user);
    }

    @Override
    public boolean initialize() throws IOException {
        Krb5Keytab keytab = getKeytab(LC.krb5_keytab.value());
        if (keytab == null) {
            sendFailed("mechanism not supported");
            return false;
        }
        debug("keytab file = %s", keytab.getFile());

        final String host = LC.zimbra_server_hostname.value();
        KerberosPrincipal kp = new KerberosPrincipal(getProtocol() + '/' + host);
        debug("kerberos principle = %s", kp);
        Subject subject = getSubject(keytab, kp);
        if (subject == null) {
            sendFailed();
            return false;
        }
        debug("subject = %s", subject);

        final Map<String, String> props = getSaslProperties();
        if (DEBUG && props != null) {
            String qop = props.get(Sasl.QOP);
            debug("Sent QOP = " + (qop != null ? qop : "auth"));
        }

        try {
            mSaslServer = (SaslServer) Subject.doAs(subject,
                new PrivilegedExceptionAction() {
                    public Object run() throws SaslException {
                        return Sasl.createSaslServer(
                            getMechanism(), getProtocol(), host, props,
                            new GssCallbackHandler());
                    }
                });
        } catch (PrivilegedActionException e) {
            sendFailed();
            getLog().warn("Could not create SaslServer", e.getCause());
            return false;
        }
        return true;
    }


    private Krb5Keytab getKeytab(String path) {
        try {
            return Krb5Keytab.getInstance(path);
        } catch (IOException e) {
            getLog().warn("Keytab file '" + path + "' not found");
            return null;
        }
    }

    private Subject getSubject(Krb5Keytab keytab, KerberosPrincipal kp)
            throws IOException {
        List<KerberosKey> keys = keytab.getKeys(kp);
        if (keys == null) {
            getLog().warn(
                "Key not found in keystore for service principal '" + kp + "'");
            return null;
        }
        Subject subject = new Subject();
        subject.getPrincipals().add(kp);
        subject.getPrivateCredentials().addAll(keys);
        return subject;
    }

    @Override
    public void handle(final byte[] data) throws IOException {
        if (isComplete()) {
            throw new IllegalStateException("Authentication already completed");
        }
        // Evaluate client response and get challenge bytes
        byte[] bytes;
        try {
            bytes = mSaslServer.evaluateResponse(data);
        } catch (SaslException e) {
            ZimbraLog.imap.warn("SaslServer.evaluateResponse() failed", e);
            // Only send if the callback hadn't already sent an error response
            if (!isComplete()) sendBadRequest();
            return;
        }
        // If exchange not complete, send additional challenge
        if (!isComplete()) {
            assert !mSaslServer.isComplete();
            String s = new String(Base64.encodeBase64(bytes), "US-ASCII");
            sendContinuation(s);
            return;
        }
        // Authentication complete, so finish up
        assert mSaslServer.isComplete();
        if (DEBUG) dumpNegotiatedProperties();
        // If authentication failed, dispose of SaslServer instance
        if (!isAuthenticated()) {
            debug("Authentication failed");
            dispose();
            return;
        }
        // Authentication successful, so check if encryption enabled
        String qop = (String) mSaslServer.getNegotiatedProperty(Sasl.QOP);
        if (QOP_AUTH_INT.equals(qop) || QOP_AUTH_CONF.equals(qop)) {
            debug("SASL encryption enabled (%s)", qop);
            mEncryptionEnabled = true;
        } else {
            dispose(); // No need for SaslServer any longer
        }
    }

    private Map<String, String> getSaslProperties() {
        // Don't offer encryption if SSL is enabled
        return mAuthUser.isSSLEnabled() ? null : ENCRYPTION_PROPS;
    }

    @Override
    public boolean isEncryptionEnabled() {
        return mEncryptionEnabled;
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
    public SaslServer getSaslServer() {
        return mSaslServer;
    }

    @Override
    public void dispose() {
        debug("dispose called");
        try {
            mSaslServer.dispose();
        } catch (SaslException e) {
            ZimbraLog.imap.warn("SaslServer.dispose() failed", e);
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
            cb.setAuthorized(authenticate(cb.getAuthorizationID(),
                                          cb.getAuthenticationID(), null));
        }
    }

    private void dumpNegotiatedProperties() {
        pp("QOP", Sasl.QOP);
        pp("MAX_BUFFER", Sasl.MAX_BUFFER);
        pp("MAX_RECEIVE_SIZE", Sasl.RAW_SEND_SIZE);
        pp("STRENGTH", Sasl.STRENGTH);
    }

    private void pp(String printName, String propName) {
        Object obj = mSaslServer.getNegotiatedProperty(propName);
        if (obj != null) debug("Negotiated property %s = %s", printName, obj);
    }

    private static void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf("[DEBUG GssAuthenticator] " + format + "\n", args);
        }
    }
}
