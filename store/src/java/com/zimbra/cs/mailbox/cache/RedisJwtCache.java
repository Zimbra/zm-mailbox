/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

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
