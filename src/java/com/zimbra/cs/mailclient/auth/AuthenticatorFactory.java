/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.auth;

import com.zimbra.cs.mailclient.MailConfig;

import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import java.util.Map;
import java.util.HashMap;

public final class AuthenticatorFactory {
    private Map<String, Info> authenticators;

    private static class Info {
        Class<? extends Authenticator> clazz;
        boolean passwordRequired;
    }

    private static final AuthenticatorFactory DEFAULT =
        new AuthenticatorFactory();

    public static AuthenticatorFactory getDefault() {
        return DEFAULT;
    }

    public AuthenticatorFactory() {
        authenticators = new HashMap<String, Info>();
        register(SaslAuthenticator.PLAIN, SaslAuthenticator.class);
        register(SaslAuthenticator.GSSAPI, SaslAuthenticator.class, false);
        register(SaslAuthenticator.CRAM_MD5, SaslAuthenticator.class);
        register(SaslAuthenticator.DIGEST_MD5, SaslAuthenticator.class);
        register(SaslAuthenticator.XOAUTH2, SaslAuthenticator.class, false);
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
            auth = info.clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate class: " + info.clazz, e);
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

    public void register(String mechanism, Class<? extends Authenticator> clazz, boolean passwordRequired) {
        Info info = new Info();
        info.clazz = clazz;
        info.passwordRequired = passwordRequired;
        authenticators.put(mechanism, info);
    }

    public void register(String mechanism, Class<? extends Authenticator> clazz) {
        register(mechanism, clazz, true);
    }
}
