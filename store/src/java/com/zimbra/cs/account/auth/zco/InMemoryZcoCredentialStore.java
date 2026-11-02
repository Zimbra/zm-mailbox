package com.zimbra.cs.account.auth.zco;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class InMemoryZcoCredentialStore implements ZcoCredentialStore {

    private static final class Entry {
        final String value;
        final long evictAt;

        Entry(String value, long evictAt) {
            this.value = value;
            this.evictAt = evictAt;
        }
    }

    private final ConcurrentMap<String, Entry> map = new ConcurrentHashMap<>();

    @Override
    public boolean put(String key, String value, long ttlMs) {
        final long now = System.currentTimeMillis();
        map.values().removeIf(e -> e.evictAt < now);
        map.put(key, new Entry(value, now + ttlMs));
        return true;
    }

    @Override
    public String get(String key) {
        final Entry e = map.get(key);
        if (e == null) {
            return null;
        }
        if (e.evictAt < System.currentTimeMillis()) {
            map.remove(key, e);
            return null;
        }
        return e.value;
    }

    @Override
    public boolean delete(String key) {
        final Entry e = map.remove(key);
        return e != null && e.evictAt >= System.currentTimeMillis();
    }
}