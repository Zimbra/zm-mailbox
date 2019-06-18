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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.mailbox.RedissonClientHolder;
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

    // waitset base

    private static RBucket<WaitSetBase> getBucket(String wsId) {
        return client.getBucket(wsId);
    }

    public static WaitSetBase remove(String wsId) {
        RBucket<WaitSetBase> bucket = getBucket(wsId);
        return bucket.getAndDelete();
    }

    public static WaitSetBase get(String wsId) {
        RBucket<WaitSetBase> bucket = getBucket(wsId);
        return bucket.get();
    }

    public static void put(String wsId, WaitSetBase ws) {
        RBucket<WaitSetBase> bucket = getBucket(wsId);
        bucket.set(ws, TIME_TO_LIVE, TimeUnit.MILLISECONDS);
    }

    public static List<WaitSetBase> getAll() {
        List<WaitSetBase> returnList = new  ArrayList<WaitSetBase>();
        RKeys keys = client.getKeys();
        List<String> keysToGet = new ArrayList<String>();
        keys.getKeysByPattern(WaitSetMgr.WAITSET_PREFIX).forEach(key -> keysToGet.add(key));
        keys.getKeysByPattern(WaitSetMgr.ALL_ACCOUNTS_ID_PREFIX).forEach(key -> keysToGet.add(key));

        RBuckets rBuckets = client.getBuckets();
        Map<String, WaitSetBase> wsMap = rBuckets.get(keysToGet.toArray(new String[] {}));

        wsMap.values()
            .stream()
            .forEach(ws -> {
                if (ws != null) {
                    returnList.add(ws);
                }
            });
        return returnList;
    }

    // account waitsets

    private static RBucket<List<String>> getBucketForAccountWaitsets(String wsAccountId) {
        return client.getBucket(wsAccountId);
    }

    public static List<String> removeAllWaitsetsForAccount(String accountId) {
        String wsAccountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<List<String>> bucket = getBucketForAccountWaitsets(wsAccountId);
        return bucket.getAndDelete();
    }

    public static List<String> removeWaitsetsForAccount(String accountId, String wsId) {
        String wsAccountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<List<String>> bucket = getBucketForAccountWaitsets(wsAccountId);
        List<String> list = bucket.getAndDelete();
        list.remove(wsId);
        putAccountWaitsets(accountId, list);
        return list;
    }

    public static List<String> getAccountWaitsets(String accountId) {
        accountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<List<String>> bucket = getBucketForAccountWaitsets(accountId);
        return bucket.get();
    }

    public static void putAccountWaitsets(String accountId, List<String> listOfWaitsets) {
        accountId = WAITSETS_BY_ACCOUNT_ID_PREFIX + accountId;
        RBucket<List<String>> bucket = getBucketForAccountWaitsets(accountId);
        if (listOfWaitsets == null) {
            listOfWaitsets = new ArrayList<String>()	;
        }
        bucket.set(listOfWaitsets, TIME_TO_LIVE, TimeUnit.MILLISECONDS);
    }
}
