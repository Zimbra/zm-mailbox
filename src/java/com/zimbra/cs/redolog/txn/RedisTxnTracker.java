/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.txn;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.redolog.TransactionId;

/**
 * TxnTracker which stores data in Redis
 * Preferred for multi-server installations; and required for always on cluster installations.
 * Allows resume/replay of failed transactions from a crashed server on another server without restart or full log scan
 * Redis keys are prefixed by serverId or clusterId to avoid mailboxId collisions in case Redis is shared across multiple SQL DB instances.
 */
public class RedisTxnTracker implements TxnTracker {

    private static final String KEY_PREFIX = "zmRedoLogActiveTxn:";
    protected Pool<Jedis> jedisPool;
    private Server localServer;

    public RedisTxnTracker(Pool<Jedis> jedisPool) throws ServiceException {
        this.jedisPool = jedisPool;
        localServer = Provisioning.getInstance().getLocalServer();
    }

    private String key(int mboxId) {
        return KEY_PREFIX +
            (localServer.getAlwaysOnClusterId() == null ? "server:" + localServer.getId() :
            "cluster:" + localServer.getAlwaysOnClusterId()) + ":" +
            mboxId;
    }

    @Override
    public void addActiveTxn(int mboxId, TransactionId txnId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.rpush(key(mboxId), txnId.encodeToString());
        }
    }

    @Override
    public void removeActiveTxn(int mboxId, TransactionId txnId) {
        try (Jedis jedis = jedisPool.getResource()) {
            //we send count=0 to remove all matching elements; a few esoteric cases such as mailbox delete add twice but remove once
            jedis.lrem(key(mboxId), 0, txnId.encodeToString());
        }
    }

    @Override
    public List<TransactionId> getActiveTransactions(int mboxId) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> txnStrings = jedis.lrange(key(mboxId), 0, -1);
            if (txnStrings != null && txnStrings.size() > 0) {
                List<TransactionId> txnIds = new ArrayList<TransactionId>(txnStrings.size());
                for (String txnString : txnStrings) {
                    try {
                        txnIds.add(TransactionId.decodeFromString(txnString));
                    } catch (ServiceException se) {
                        ZimbraLog.redolog.warn("failed to parse txnId string %s", txnString);
                    }
                }
                return txnIds;
            }
        }
        return null;
    }

    @Override
    public boolean hasActiveTransactions(int mboxId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.llen(key(mboxId)) > 0;
        }
    }
}
