package com.zimbra.cs.service.account;

import com.zimbra.common.util.ZimbraLog;

/**
 * Thread-safe singleton registry for NativeOAuthProvider.
 */
public final class NativeOAuthRegistry {

    private static volatile NativeOAuthProvider provider = null;

    private NativeOAuthRegistry() {}

    public static void registerProvider(NativeOAuthProvider p) {
        provider = p;
        if (p != null) {
            ZimbraLog.account.info("Registered NativeOAuthProvider: %s", p.getClass().getName());
        }
    }

    public static NativeOAuthProvider getProvider() {
        return provider;
    }

    public static boolean hasProvider() {
        return provider != null;
    }
}