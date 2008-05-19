package com.zimbra.cs.mailclient.auth;

import com.zimbra.cs.mailclient.MailConfig;

import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import java.util.Map;
import java.util.HashMap;

public final class AuthenticatorFactory {
    private Map<String, Class> authenticators;

    private static final AuthenticatorFactory DEFAULT =
        new AuthenticatorFactory();

    public static AuthenticatorFactory getDefault() {
        return DEFAULT;
    }

    public AuthenticatorFactory() {
        authenticators = new HashMap<String, Class>();
        register(SaslAuthenticator.MECHANISM_PLAIN, SaslAuthenticator.class);
        register(SaslAuthenticator.MECHANISM_GSSAPI, SaslAuthenticator.class);
        register(SaslAuthenticator.MECHANISM_CRAM_MD5, SaslAuthenticator.class);
    }
    
    public Authenticator newAuthenticator(MailConfig config, String password)
        throws LoginException, SaslException {
        String mechanism = config.getMechanism();
        if (mechanism == null) {
            throw new IllegalArgumentException("Missing required mechanism");
        }
        Class clazz = authenticators.get(mechanism);
        if (clazz == null) {
            return null;
        }
        Authenticator auth;
        try {
            auth = (Authenticator) clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                "Unable to instantiate class: " + clazz, e);
        }
        auth.init(config, password);
        return auth;
    }

    public Authenticator newAuthenticator(MailConfig config)
        throws LoginException, SaslException {
        return newAuthenticator(config, null);
    }

    public void register(String mechanism, Class<? extends Authenticator> clazz) {
        authenticators.put(mechanism, clazz);
    }
}
