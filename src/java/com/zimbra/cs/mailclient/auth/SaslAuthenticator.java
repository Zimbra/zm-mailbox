package com.zimbra.cs.mailclient.auth;

import com.zimbra.cs.security.sasl.SaslInputStream;
import com.zimbra.cs.security.sasl.SaslOutputStream;
import com.zimbra.cs.security.sasl.SaslSecurityLayer;
import com.zimbra.cs.mailclient.MailConfig;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

public class SaslAuthenticator extends Authenticator {
    private MailConfig config;
    private String password;
    private LoginContext loginContext;
    private Subject subject;
    private SaslClient saslClient;

    public static final String MECHANISM_GSSAPI = "GSSAPI";
    public static final String MECHANISM_PLAIN = "PLAIN";
    public static final String MECHANISM_CRAM_MD5 = "CRAM-MD5";

    public static final String QOP_AUTH = "auth";
    public static final String QOP_AUTH_CONF = "auth-conf";
    public static final String QOP_AUTH_INT = "auth-int";
    
    public SaslAuthenticator() {}

    public void init(MailConfig config, String password)
        throws LoginException, SaslException {
        this.config = config;
        this.password = password;
        String mechanism = config.getMechanism();
        checkRequired("mechanism", mechanism);
        checkRequired("host", config.getHost());
        checkRequired("protocol", config.getProtocol());
        checkRequired("authentication id", config.getAuthenticationId());
        saslClient = mechanism.equals(MECHANISM_GSSAPI) ?
            createGssSaslClient() : createSaslClient();
        Map<String, String> props = config.getSaslProperties();
        String qop = QOP_AUTH;
        if (props != null) {
            qop = props.get(Sasl.QOP);
        }
        debug("Requested QOP is %s", qop != null ? qop : "auth");
    }

    public String getMechanism() {
        return config.getMechanism();
    }
    
    private static void checkRequired(String name, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required " + name);
        }
    }

    private SaslClient createGssSaslClient() throws LoginException, SaslException {
        loginContext = getLoginContext();
        loginContext.login();
        subject = loginContext.getSubject();
        debug("GSS subject = %s", subject);
        try {
            return (SaslClient) Subject.doAs(subject,
                new PrivilegedExceptionAction() {
                    public Object run() throws SaslException {
                        return createSaslClient();
                    }
                });
        } catch (PrivilegedActionException e) {
            dispose();
            Exception cause = e.getException();
            if (cause instanceof SaslException) {
                throw (SaslException) cause;
            } else if (cause instanceof LoginException) {
                throw (LoginException) cause;
            } else {
                throw new IllegalStateException(
                    "Error initialization GSS authenticator", e);
            }
        }
    }

    private static final String LOGIN_MODULE_NAME =
        "com.sun.security.auth.module.Krb5LoginModule";

    private LoginContext getLoginContext() throws LoginException {
        Map<String, String> options = new HashMap<String, String>();
        options.put("debug", Boolean.toString(config.isDebug()));
        options.put("principal", getPrincipal());
        // options.put("useTicketCache", "true");
        // options.put("storeKey", "true");
        final AppConfigurationEntry ace = new AppConfigurationEntry(
            LOGIN_MODULE_NAME,
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
            options);
        Configuration config = new Configuration() {
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[] { ace };
            }
            public void refresh() {}
        };
        return new LoginContext("krb5", null, new SaslCallbackHandler(), config);
    }

    private String getPrincipal() {
        String realm = config.getRealm();
        String authenticationId = config.getAuthenticationId();
        return realm != null && authenticationId.indexOf('@') == -1 ?
            authenticationId + '@' + realm : authenticationId;
    }

    private SaslClient createSaslClient() throws SaslException {
        return Sasl.createSaslClient(
            new String[] { config.getMechanism() },
            config.getAuthorizationId(),
            config.getProtocol(),
            config.getHost(),
            config.getSaslProperties(),
            new SaslCallbackHandler());
    }

    public byte[] evaluateChallenge(final byte[] challenge) throws SaslException {
        if (isComplete()) {
            throw new IllegalStateException("Authentication already completed");
        }
        return subject != null ?
            evaluateGssChallenge(challenge) : saslClient.evaluateChallenge(challenge);
    }

    private byte[] evaluateGssChallenge(final byte[] challenge) throws SaslException {
        try {
            return (byte[]) Subject.doAs(subject,
                new PrivilegedExceptionAction() {
                    public Object run() throws SaslException {
                        return saslClient.evaluateChallenge(challenge);
                    }
                });
        } catch (PrivilegedActionException e) {
            dispose();
            Throwable cause = e.getCause();
            if (cause instanceof SaslException) {
                throw (SaslException) cause;
            } else {
                throw new IllegalStateException("Unknown authentication error", cause);
            }
        }
    }
    
    public byte[] getInitialResponse() throws SaslException {
        if (!hasInitialResponse()) {
            throw new IllegalStateException(
                "Mechanism does not support initial response");
        }
        return saslClient.evaluateChallenge(new byte[0]);
    }
   
    public boolean hasInitialResponse() {
        return saslClient.hasInitialResponse();
    }

    public boolean isComplete() {
        return saslClient.isComplete();
    }

    private class SaslCallbackHandler implements CallbackHandler {
        public void handle(Callback[] cbs)
            throws IOException, UnsupportedCallbackException {
            for (Callback cb : cbs) {
                if (cb instanceof NameCallback) {
                    ((NameCallback) cb).setName(config.getAuthenticationId());
                } else if (cb instanceof PasswordCallback) {
                    if (password == null) {
                        throw new IllegalStateException(
                            "Password missing but required");
                    }
                    ((PasswordCallback) cb).setPassword(password.toCharArray());
                    password = null; // Clear password once finished
                } else if (cb instanceof RealmCallback) {
                    String realm = config.getRealm();
                    if (realm == null) {
                        throw new IllegalStateException(
                            "Realm missing but required");
                    }
                    ((RealmCallback) cb).setText(realm);
                } else {
                    throw new UnsupportedCallbackException(cb);
                }
            }
        }
    }

    public boolean isEncryptionEnabled() {
        return SaslSecurityLayer.getInstance(saslClient).isEnabled();
    }

    public OutputStream wrap(OutputStream os) {
        return isEncryptionEnabled() ?
            new SaslOutputStream(os, saslClient) : os;
    }

    public InputStream unwrap(InputStream is) {
        return isEncryptionEnabled() ?
            new SaslInputStream(is, saslClient) : is;
    }
    
    public String getNegotiatedProperty(String name) {
        return (String) saslClient.getNegotiatedProperty(name);
    }
    
    public void dispose() throws SaslException {
        saslClient.dispose();
        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (LoginException e) {
                e.printStackTrace();
            }
            loginContext = null;
        }
    }

    private void debug(String format, Object... args) {
        if (config.isDebug()) {
            System.out.printf("[SaslAuthenticator] " + format + "\n", args);
        }
    }
}
