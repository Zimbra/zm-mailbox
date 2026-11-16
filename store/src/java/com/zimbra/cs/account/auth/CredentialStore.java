package com.zimbra.cs.account.auth;

interface CredentialStore {
    boolean put(String key, String value, long ttlMs);

    String get(String key);

    boolean delete(String key);
}
