package com.zimbra.cs.mailclient;

import com.zimbra.cs.security.sasl.SaslInputStream;
import com.zimbra.cs.security.sasl.SaslOutputStream;
import com.zimbra.cs.security.sasl.SaslSecurityLayer;
import com.zimbra.cs.mailclient.util.Password;

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
    private final String mMechanism;
    private final String mProtocol;
    private final String mServerName;
    private Map<String,String> mProperties;
    private String mAuthorizationId;
    private String mAuthenticationId;
    private String mRealm;
    private String mPassword;
    private LoginContext mLoginContext;
    private Subject mSubject;
    private SaslClient mSaslClient;
    private boolean mDebug;

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
        mMechanism = mechanism;
        mProtocol = protocol;
        mServerName = serverName;
        mAuthenticationId = System.getProperty("user.name");
    }

    public void initialize() throws LoginException, SaslException {
        if (mAuthorizationId == null) mAuthorizationId = mAuthenticationId;
        mSaslClient = MECHANISM_GSSAPI.equals(mMechanism) ?
            createGssSaslClient() : createSaslClient();
        String qop = mProperties != null ? mProperties.get(Sasl.QOP) : null;
        debug("Requested QOP is %s", qop != null ? qop : "auth");
    }

    private SaslClient createGssSaslClient() throws LoginException, SaslException {
        mLoginContext = getLoginContext();
        mLoginContext.login();
        mSubject = mLoginContext.getSubject();
        debug("GSS subject = %s", mSubject);
        try {
            return (SaslClient) Subject.doAs(mSubject,
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
        options.put("debug", Boolean.toString(mDebug));
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
        return mRealm != null && mAuthenticationId.indexOf('@') == -1 ?
            mAuthenticationId + '@' + mRealm : mAuthenticationId;
    }

    private SaslClient createSaslClient() throws SaslException {
        return Sasl.createSaslClient(new String[] { mMechanism },
            mAuthorizationId, mProtocol, mServerName, mProperties,
            new SaslCallbackHandler());
    }

    public byte[] evaluateChallenge(final byte[] challenge) throws SaslException {
        checkInitialized();
        if (isComplete()) {
            throw new IllegalStateException("Authentication already completed");
        }
        return mSubject != null ?
            evaluateGssChallenge(challenge) : mSaslClient.evaluateChallenge(challenge);
    }

    private byte[] evaluateGssChallenge(final byte[] challenge) throws SaslException {
        try {
            return (byte[]) Subject.doAs(mSubject,
                new PrivilegedExceptionAction() {
                    public Object run() throws SaslException {
                        return mSaslClient.evaluateChallenge(challenge);
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
        return mSaslClient.evaluateChallenge(new byte[0]);
    }
   
    public boolean hasInitialResponse() {
        checkInitialized();
        return mSaslClient.hasInitialResponse();
    }

    public boolean isComplete() {
        checkInitialized();
        return mSaslClient.isComplete();
    }

    private class SaslCallbackHandler implements CallbackHandler {
        public void handle(Callback[] cbs)
            throws IOException, UnsupportedCallbackException {
            for (Callback cb : cbs) {
                if (cb instanceof NameCallback) {
                    ((NameCallback) cb).setName(mAuthenticationId);
                } else if (cb instanceof PasswordCallback) {
                    ((PasswordCallback) cb).setPassword(getPassword());
                } else if (cb instanceof RealmCallback) {
                    ((RealmCallback) cb).setText(mRealm);
                } else {
                    throw new UnsupportedCallbackException(cb);
                }
            }
        }
    }

    private char[] getPassword() throws IOException {
        if (mPassword != null) return mPassword.toCharArray();
        return Password.getInstance().readPassword("Enter password: ");
    }
    
    public boolean isEncryptionEnabled() {
        checkInitialized();
        return SaslSecurityLayer.getInstance(mSaslClient).isEnabled();
    }

    public OutputStream getWrappedOutputStream(OutputStream os) {
        checkInitialized();
        return isEncryptionEnabled() ?
            new SaslOutputStream(os, mSaslClient) : os;
    }

    public InputStream getUnwrappedInputStream(InputStream is) {
        checkInitialized();
        return isEncryptionEnabled() ?
            new SaslInputStream(is, mSaslClient) : is;
    }
    
    public Map<String,String> getProperties() {
        if (mProperties == null) {
            mProperties = new HashMap<String, String>();
        }
        return mProperties;
    }

    public void setProperty(String name, String value) {
        getProperties().put(name, value);
    }

    public String getNegotiatedProperty(String name) {
        return (String) mSaslClient.getNegotiatedProperty(name);
    }
    
    public void setAuthorizationId(String id) {
        mAuthorizationId = id;
    }

    public void setAuthenticationId(String id) {
        mAuthenticationId = id;
    }

    public void setRealm(String realm) {
        mRealm = realm;
    }                  

    public void setPassword(String password) {
        mPassword = password;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
    }
    
    public void dispose() throws SaslException {
        checkInitialized();
        mSaslClient.dispose();
        if (mLoginContext != null) {
            try {
                mLoginContext.logout();
            } catch (LoginException e) {
                e.printStackTrace();
            }
            mLoginContext = null;
        }
    }

    private void checkInitialized() {
        if (mSaslClient == null) {
            throw new IllegalStateException("Authenticator is not initialized");
        }
    }

    private void debug(String format, Object... args) {
        if (mDebug) {
            System.out.printf("[ClientAuthenticator] " + format + "\n", args);
        }
    }

}
