package com.zimbra.cs.mailbox.cache;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import com.zimbra.cs.mailbox.RedissonClientHolder;

public class RedisJwtCache {

    private static RedissonClient client;

    static {
        client = RedissonClientHolder.getInstance().getRedissonClient();
    }

    private static RBucket<JWTInfo> getBucket(String key) {
        return client.getBucket(key);
    }

    public static JWTInfo remove(String jti) {
        RBucket<JWTInfo> bucket = getBucket(jti);
        JWTInfo jwtInfo = bucket.getAndDelete();
        return jwtInfo;
    }

    public static JWTInfo get(String jti) {
        RBucket<JWTInfo> bucket = getBucket(jti);
        return bucket.get();
    }

    public static void put(String jti, JWTInfo jwtInfo) {
        RBucket<JWTInfo> bucket = getBucket(jti);
        long timeToLive = jwtInfo.getExpiryTime() - System.currentTimeMillis();
        bucket.set(jwtInfo, timeToLive, TimeUnit.MILLISECONDS);
    }
}
