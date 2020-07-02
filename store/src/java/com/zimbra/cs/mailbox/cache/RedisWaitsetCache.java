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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RBuckets;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.redis.RedisUtils;
import com.zimbra.cs.session.WaitSetBase;
import com.zimbra.cs.session.WaitSetMgr;

public class RedisWaitsetCache {

    private static final String WAITSETS_BY_ACCOUNT_ID_PREFIX = "Waitset-By-Account-";
    private static final long TIME_TO_LIVE =
            LC.zimbra_active_waitset_timeout_minutes.longValue()
            * Constants.MILLIS_PER_MINUTE;
    private static RedissonClient client;

    static {
        client = RedissonClientHolder.getInstance().getRedissonClient();
    }

    /**
     * Used RedisUtils.stringify() and RedisUtils.objectify() methods to serialize and deserialize WaitSetBase,
     * since JsonJacksonCodec used in Redis was not able to serialize and deserialize the WaitSetBase object.
     * HashMap objects in WaitSetBase were not serialized correctly JsonJacksonCodec and FstCodec.
     */
    // waitset base

    private static RBucket<String> getBucket(String wsId) {
        return client.getBucket(wsId);
    }

    public static WaitSetBase remove(String wsId) {
        RBucket<String> bucket = getBucket(wsId);
        String json = bucket.getAndDelete();
        if (!StringUtil.isNullOrEmpty(json)) {
            WaitSetBase ws = RedisUtils.objectify(json, new TypeReference<WaitSetBase>() {});
            return ws;
        }
        return null;
    }

    public static WaitSetBase get(String wsId) {
        RBucket<String> bucket = getBucket(wsId);
        String json = bucket.get();
        if (!StringUtil.isNullOrEmpty(json)) {
            WaitSetBase ws = RedisUtils.objectify(json, new TypeReference<WaitSetBase>() {});
            return ws;
        }
        return null;
    }

    public static void put(String wsId, WaitSetBase ws) {
        RBucket<String> bucket = getBucket(wsId);
        String json = RedisUtils.stringify(ws);
        bucket.set(json, TIME_TO_LIVE, TimeUnit.MILLISECONDS);
    }

    public static List<WaitSetBase> getAll() {
        List<WaitSetBase> returnList = new  ArrayList<WaitSetBase>();
        RKeys keys = client.getKeys();
        List<String> keysToGet = new ArrayList<String>();
        keys.getKeysByPattern(WaitSetMgr.WAITSET_PREFIX).forEach(key -> keysToGet.add(key));
        keys.getKeysByPattern(WaitSetMgr.ALL_ACCOUNTS_ID_PREFIX).forEach(key -> keysToGet.add(key));

        RBuckets rBuckets = client.getBuckets();
        Map<String, String> wsMap = rBuckets.get(keysToGet.toArray(new String[] {}));

        wsMap.values()
            .stream()
            .forEach(json -> {
                if (!StringUtil.isNullOrEmpty(json)) {
                    WaitSetBase ws = RedisUtils.objectify(json, new TypeReference<WaitSetBase>() {});
                    returnList.add(ws);
                }
            });
        return returnList;
    }

    // account waitsets

    private static RBucket<String> getBucketForAccountWaitsets(String wsAccountId) {
        return client.getBucket(wsAccountId);
    }

    public static List<String> removeAllWaitsetsForAccount(String accountId) {
        String wsAccountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<String> bucket = getBucketForAccountWaitsets(wsAccountId);
        String json = bucket.getAndDelete();
        if (!StringUtil.isNullOrEmpty(json)) {
            List<String> list = RedisUtils.objectify(json, new TypeReference<List<String>>() {});
            return list;
        }
        return null;
    }

    public static List<String> removeWaitsetsForAccount(String accountId, String wsId) {
        String wsAccountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<String> bucket = getBucketForAccountWaitsets(wsAccountId);
        String json = bucket.getAndDelete();
        if (!StringUtil.isNullOrEmpty(json)) {
            List<String> list = RedisUtils.objectify(json, new TypeReference<List<String>>() {});
            list.remove(wsId);
            putAccountWaitsets(accountId, list);
            return list;
        }
        return null;
    }

    public static List<String> getAccountWaitsets(String accountId) {
        accountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<String> bucket = getBucketForAccountWaitsets(accountId);
        String json = bucket.get();
        if (!StringUtil.isNullOrEmpty(json)) {
            return RedisUtils.objectify(json, new TypeReference<List<String>>() {});
        }
        return null;
    }

    public static void putAccountWaitsets(String accountId, List<String> listOfWaitsets) {
        accountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<String> bucket = getBucketForAccountWaitsets(accountId);
        if (listOfWaitsets == null) {
            listOfWaitsets = new ArrayList<String>()	;
        }
        String json = RedisUtils.stringify(listOfWaitsets);
        bucket.set(json, TIME_TO_LIVE, TimeUnit.MILLISECONDS);
    }
}
