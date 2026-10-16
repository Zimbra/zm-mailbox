package com.zimbra.cs.service.account;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.account.OAuthProvider;

/**
 * Thread-safe singleton registry for NativeOAuthProvider.
 */
public final class OAuthRegistry {

    private static volatile OAuthProvider provider = null;

    private OAuthRegistry() {}

    public static void registerProvider(OAuthProvider p) {
        provider = p;
        if (p != null) {
            ZimbraLog.account.info("Registered NativeOAuthProvider: %s", p.getClass().getName());
        }
    }

    public static OAuthProvider getProvider() {
        return provider;
    }

    public static boolean hasProvider() {
        return provider != null;
    }
}