/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ThreadPool;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.redis.RedissonRetryClient;

public final class RedissonClientHolder {

    private final RedissonRetryClient client;
    private static class InstanceHolder {
        public static RedissonClientHolder instance = new RedissonClientHolder();
    }

    private RedissonClientHolder() {
        String uri = null;
        try {
            uri = LC.redis_service_uri.value();
            ZimbraLog.system.info("redis_service_uri=%s", uri);
            String servers[] = uri.split(" ");
            ThreadPool pool = ThreadPool.newCachedThreadPool("RedissonExecutor");
            RedissonRetryClient newClient;
            /* First, assume we're connecting to a Redis cluster, note that that can be represented
             * by either a single URI or multiple URIs.
             * If we're actually connecting to a standalone Redis server, will probably get this logged:
             *     ClusterConnectionManager - ERR This instance has cluster support disabled.
             * and then get RedisConnectionException thrown.
             * If that happens, try to connect in standalone mode (if it makes sense to do so) */
            try {
                newClient = getClientForCluster(servers, pool);
            } catch (Exception ex) {
                if (servers.length != 1) {
                    throw ex;  // cannot possibly be a standalone server
                }
                ZimbraLog.system.warnQuietly("Unable to setup RedissonClient to connect to %s in cluster mode",
                        uri, ex);
                newClient = getClientForStandalone(uri, pool);
            }
            client = newClient;
        } catch (Exception ex) {
            ZimbraLog.system.fatal("Cannot setup RedissonClient to connect to %s", uri, ex);
            throw ex;
        }
    }

    private RedissonRetryClient getClientForCluster(String servers[], ThreadPool pool) {
        Config config = getConfig(pool);
        ClusterServersConfig clusterServersConfig = config.useClusterServers();
        clusterServersConfig.setScanInterval(LC.redis_cluster_scan_interval.intValue());
        clusterServersConfig.addNodeAddress(servers);
        clusterServersConfig.setMasterConnectionPoolSize(LC.redis_master_connection_pool_size.intValue());
        clusterServersConfig.setMasterConnectionMinimumIdleSize(LC.redis_master_idle_connection_pool_size.intValue());
        clusterServersConfig.setSubscriptionConnectionPoolSize(LC.redis_subscription_connection_pool_size.intValue());
        clusterServersConfig.setSubscriptionConnectionMinimumIdleSize(
                LC.redis_subscription_idle_connection_pool_size.intValue());
        clusterServersConfig.setSubscriptionsPerConnection(LC.redis_subscriptions_per_connection.intValue());
        clusterServersConfig.setRetryInterval(LC.redis_retry_interval.intValue());
        clusterServersConfig.setTimeout(LC.redis_connection_timeout.intValue());
        clusterServersConfig.setRetryAttempts(LC.redis_num_retries.intValue());
        return new RedissonRetryClient(Redisson.create(config));
    }

    private RedissonRetryClient getClientForStandalone(String uri, ThreadPool pool) {
        Config config = getConfig(pool);
        SingleServerConfig singleServer = config.useSingleServer();
        singleServer.setAddress(uri);
        singleServer.setConnectionPoolSize(LC.redis_master_connection_pool_size.intValue());
        singleServer.setConnectionMinimumIdleSize(LC.redis_master_idle_connection_pool_size.intValue());
        // Despite apparently similar code in getClientForCluster, these method calls are either different or
        // from non-public base class BaseConfig
        singleServer.setSubscriptionConnectionPoolSize(LC.redis_subscription_connection_pool_size.intValue());
        singleServer.setSubscriptionConnectionMinimumIdleSize(
                LC.redis_subscription_idle_connection_pool_size.intValue());
        singleServer.setSubscriptionsPerConnection(LC.redis_subscriptions_per_connection.intValue());
        singleServer.setRetryInterval(LC.redis_retry_interval.intValue());
        singleServer.setTimeout(LC.redis_connection_timeout.intValue());
        singleServer.setRetryAttempts(LC.redis_num_retries.intValue());
        return new RedissonRetryClient(Redisson.create(config));
    }

    private Config getConfig(ThreadPool pool) {
        Config config = new Config();
        config.setExecutor(pool.getExecutorService());
        config.setNettyThreads(LC.redis_netty_threads.intValue());
        return config;
    }

    public static RedissonClientHolder getInstance() {
        return  InstanceHolder.instance;
    }

    public RedissonClient getRedissonClient() {
        return client;
    }
}
