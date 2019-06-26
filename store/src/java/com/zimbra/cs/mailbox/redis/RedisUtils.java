package com.zimbra.cs.mailbox.redis;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;

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

    public static String stringify(Object object) {
        ObjectMapper oMapper = new ObjectMapper();
        oMapper.setSerializationInclusion(Include.NON_NULL);
        try {
            String json = oMapper.writeValueAsString(object);
            ZimbraLog.cache.debug(json);
            return json;
        } catch (JsonProcessingException jpe) {
            ZimbraLog.misc.info("Error while creating json string: ", jpe);
        }
        return null;
    }

    public static <T> T objectify(String content, TypeReference<T> valueType) {
        try {
            ZimbraLog.cache.debug(content);
            ObjectMapper oMapper = new ObjectMapper();
            oMapper.setSerializationInclusion(Include.NON_NULL);
            return oMapper.readValue(content, valueType);
        } catch(IOException ioe) {
            ZimbraLog.misc.info("Error occured while creating object from json string: ", ioe);
        }
        return null;
    }
}
