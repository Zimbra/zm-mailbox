package com.zimbra.cs.account.auth.zco;

interface ZcoCredentialStore {
    boolean put(String key, String value, long ttlMs);
    String get(String key);
    boolean delete(String key);
}