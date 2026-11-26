package com.zimbra.cs.account.auth;

import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.memcached.MemcachedConnector;

final class MemcachedCredentialStore implements CredentialStore {

    private static final String KEY_PREFIX = "cred-exchange:";

    @Override
    public boolean put(String key, String value, long ttlMs) {
        final int ttlSec = (int) Math.max(1, (ttlMs + 999) / 1000);
        return MemcachedConnector.getClient().put(KEY_PREFIX + key, value, ttlSec,
                ZimbraMemcachedClient.DEFAULT_TIMEOUT, true);
    }

    @Override
    public String get(String key) {
        final Object v = MemcachedConnector.getClient().get(KEY_PREFIX + key);
        return v instanceof String ? (String) v : null;
    }

    @Override
    public boolean delete(String key) {
        return MemcachedConnector.getClient().remove(KEY_PREFIX + key, true);
    }
}
