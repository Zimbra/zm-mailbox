package com.zimbra.cs.mailclient.auth;

import com.zimbra.cs.mailclient.MailConfig;

import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import java.util.Map;
import java.util.HashMap;

public final class AuthenticatorFactory {
    private Map<String, Info> authenticators;

    private static class Info {
        Class clazz;
        boolean passwordRequired;
    }

    private static final AuthenticatorFactory DEFAULT =
        new AuthenticatorFactory();

    public static AuthenticatorFactory getDefault() {
        return DEFAULT;
    }

    public AuthenticatorFactory() {
        authenticators = new HashMap<String, Info>();
        register(SaslAuthenticator.MECHANISM_PLAIN, SaslAuthenticator.class);
        register(SaslAuthenticator.MECHANISM_GSSAPI, SaslAuthenticator.class, false);
        register(SaslAuthenticator.MECHANISM_CRAM_MD5, SaslAuthenticator.class);
    }
    
    public Authenticator newAuthenticator(MailConfig config, String password)
        throws LoginException, SaslException {
        String mechanism = config.getMechanism();
        if (mechanism == null) {
            throw new IllegalArgumentException("Missing required mechanism");
        }
        Info info = authenticators.get(mechanism);
        if (info == null) {
            return null;
        }
        Authenticator auth;
        try {
            auth = (Authenticator) info.clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                "Unable to instantiate class: " + info.clazz, e);
        }
        auth.init(config, password);
        return auth;
    }

    public boolean isPasswordRequired(String mechanism) {
        Info info = authenticators.get(mechanism);
        return info != null && info.passwordRequired;
    }
    
    public Authenticator newAuthenticator(MailConfig config)
        throws LoginException, SaslException {
        return newAuthenticator(config, null);
    }

    public void register(String mechanism,
                         Class<? extends Authenticator> clazz,
                         boolean passwordRequired) {
        Info info = new Info();
        info.clazz = clazz;
        info.passwordRequired = passwordRequired;
        authenticators.put(mechanism, info);
    }

    public void register(String mechanism, Class<? extends Authenticator> clazz) {
        register(mechanism, clazz, true);
    }
}
