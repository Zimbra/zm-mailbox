package com.zimbra.cs.mailbox.redis;

public class RedisUtils {

    /**
     * Adds a hash tag to the provided key string with the account ID, ensuring that all keys for an account
     * are hashed to the same key slot (and therefore the same redis server).
     * This allows us to perform multi-key operations in a redis cluster.
     */
    public static String createAccountRoutedKey(String accountId, String keyBase) {
        return String.format("{%s}-%s", accountId, keyBase);
    }
}
