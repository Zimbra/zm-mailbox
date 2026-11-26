package com.zimbra.cs.account.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

final class InMemoryCredentialStore implements CredentialStore {

    private static final int MAX_ENTRIES = 10_000;

    private final AtomicInteger puts = new AtomicInteger();

    private static final class Entry {
        private final String value;

        private final long evictAt;

        Entry(String value, long evictAt) {
            this.value = value;
            this.evictAt = evictAt;
        }

        String getValue() {
            return value;
        }

        long getEvictAt() {
            return evictAt;
        }
    }

    private final ConcurrentMap<String, Entry> map = new ConcurrentHashMap<>();

    @Override
    public boolean put(String key, String value, long ttlMs) {
        final long now = System.currentTimeMillis();
        // cleanup only every 256 writes or when full
        if ((puts.incrementAndGet() & 0xFF) == 0 || map.size() >= MAX_ENTRIES) {
            map.values().removeIf(e -> e.getEvictAt() < now);
        }
        synchronized (map) {
            if (map.size() >= MAX_ENTRIES) {
                return false;
            }
            map.put(key, new Entry(value, now + ttlMs));
        }
        return true;
    }

    @Override
    public String get(String key) {
        final Entry e = map.get(key);
        if (e == null) {
            return null;
        }
        if (e.getEvictAt() < System.currentTimeMillis()) {
            map.remove(key, e);
            return null;
        }
        return e.getValue();
    }

    @Override
    public boolean delete(String key) {
        final Entry e = map.remove(key);
        return e != null && e.getEvictAt() >= System.currentTimeMillis();
    }
}
