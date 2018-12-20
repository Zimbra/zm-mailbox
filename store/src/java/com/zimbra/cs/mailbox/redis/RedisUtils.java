package com.zimbra.cs.mailbox.redis;

import com.zimbra.common.util.Pair;

public class RedisUtils {

    /**
     * Adds a hash tag to the provided key string with the account ID, ensuring that all keys for an account
     * are hashed to the same key slot (and therefore the same redis server).
     * This allows us to perform multi-key operations in a redis cluster.
     */
    public static String createAccountRoutedKey(String accountId, String keyBase) {
        return String.format("{%s}-%s", accountId, keyBase);
    }

    public static RedisKey createHashTaggedKey(String hashTag, String keyBase) {
        String key = String.format("{%s}-%s", hashTag, keyBase);
        return new RedisKey(key, hashTag);
    }

    public static class RedisKey extends Pair<String, String> {

        public RedisKey(String key, String hashTag) {
            super(key, hashTag);
        }

        public String getKey() {
            return getFirst();
        }

        public String getHashTag() {
            return getSecond();
        }

        @Override
        public String toString() {
            return getKey();
        }
    }
}
