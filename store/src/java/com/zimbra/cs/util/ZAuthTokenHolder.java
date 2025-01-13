package com.zimbra.cs.util;

import com.zimbra.common.auth.ZAuthToken;

public class ZAuthTokenHolder {

    private static ThreadLocal<ZAuthToken> authToken = new ThreadLocal<>();

    public static ZAuthToken getToken() {
        return authToken.get();
    }

    public static void setToken(ZAuthToken token) {
        authToken.set(token);
    }

    public static void clearToken() {
        authToken.remove();
    }

}
