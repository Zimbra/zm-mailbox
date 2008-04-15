package com.zimbra.cs.mailclient;

import com.zimbra.cs.security.sasl.SaslInputStream;
import com.zimbra.cs.security.sasl.SaslOutputStream;
import com.zimbra.cs.security.sasl.SaslSecurityLayer;

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

public class ClientAuthenticator {
    private final String mechanism;
    private final String protocol;
    private final String serverName;
    private Map<String,String> properties;
    private String authorizationId;
    private String authenticationId;
    private String realm;
    private String password;
    private LoginContext loginContext;
    private Subject subject;
    private SaslClient saslClient;
    private boolean debug;

    public static final String MECHANISM_GSSAPI = "GSSAPI";
    public static final String MECHANISM_PLAIN = "PLAIN";

    public static final String QOP_AUTH = "auth";
    public static final String QOP_AUTH_CONF = "auth-conf";
    public static final String QOP_AUTH_INT = "auth-int";
    
    protected ClientAuthenticator(String mechanism, String protocol,
                                  String serverName) {
        if (mechanism  == null) throw new NullPointerException("mechanism");
        if (protocol   == null) throw new NullPointerException("protocol");
        if (serverName == null) throw new NullPointerException("serverName");
        this.mechanism = mechanism;
        this.protocol = protocol;
        this.serverName = serverName;
        authenticationId = System.getProperty("user.name");
    }

    public void initialize() throws LoginException, SaslException {
        saslClient = MECHANISM_GSSAPI.equals(mechanism) ?
            createGssSaslClient() : createSaslClient();
        String qop = properties != null ? properties.get(Sasl.QOP) : null;
        debug("Requested QOP is %s", qop != null ? qop : "auth");
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
        options.put("debug", Boolean.toString(debug));
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
        return realm != null && authenticationId.indexOf('@') == -1 ?
            authenticationId + '@' + realm : authenticationId;
    }

    private SaslClient createSaslClient() throws SaslException {
        return Sasl.createSaslClient(new String[] { mechanism },
            authorizationId, protocol, serverName, properties,
            new SaslCallbackHandler());
    }

    public byte[] evaluateChallenge(final byte[] challenge) throws SaslException {
        checkInitialized();
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
        checkInitialized();
        if (!hasInitialResponse()) {
            throw new IllegalStateException(
                "Mechanism does not support initial response");
        }
        return saslClient.evaluateChallenge(new byte[0]);
    }
   
    public boolean hasInitialResponse() {
        checkInitialized();
        return saslClient.hasInitialResponse();
    }

    public boolean isComplete() {
        checkInitialized();
        return saslClient.isComplete();
    }

    private class SaslCallbackHandler implements CallbackHandler {
        public void handle(Callback[] cbs)
            throws IOException, UnsupportedCallbackException {
            for (Callback cb : cbs) {
                if (cb instanceof NameCallback) {
                    ((NameCallback) cb).setName(authenticationId);
                } else if (cb instanceof PasswordCallback) {
                    ((PasswordCallback) cb).setPassword(password.toCharArray());
                    password = null; // Clear password once finished
                } else if (cb instanceof RealmCallback) {
                    ((RealmCallback) cb).setText(realm);
                } else {
                    throw new UnsupportedCallbackException(cb);
                }
            }
        }
    }

    public boolean isEncryptionEnabled() {
        checkInitialized();
        return SaslSecurityLayer.getInstance(saslClient).isEnabled();
    }

    public OutputStream getWrappedOutputStream(OutputStream os) {
        checkInitialized();
        return isEncryptionEnabled() ?
            new SaslOutputStream(os, saslClient) : os;
    }

    public InputStream getUnwrappedInputStream(InputStream is) {
        checkInitialized();
        return isEncryptionEnabled() ?
            new SaslInputStream(is, saslClient) : is;
    }
    
    public Map<String,String> getProperties() {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        return properties;
    }

    public void setProperty(String name, String value) {
        getProperties().put(name, value);
    }

    public String getNegotiatedProperty(String name) {
        return (String) saslClient.getNegotiatedProperty(name);
    }
    
    public void setAuthorizationId(String id) {
        authorizationId = id;
    }

    public void setAuthenticationId(String id) {
        authenticationId = id;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }                  

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public void dispose() throws SaslException {
        checkInitialized();
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

    private void checkInitialized() {
        if (saslClient == null) {
            throw new IllegalStateException("Authenticator is not initialized");
        }
    }

    private void debug(String format, Object... args) {
        if (debug) {
            System.out.printf("[ClientAuthenticator] " + format + "\n", args);
        }
    }

}
