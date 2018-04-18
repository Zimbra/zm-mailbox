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
import org.redisson.config.Config;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public final class RedissonClientHolder {

    private final RedissonClient redisson;
    private static class InstanceHolder {
        public static RedissonClientHolder instance = new RedissonClientHolder();
    }

    private RedissonClientHolder() {
        String uri = null;
        try {
            uri = LC.redis_service_uri.value();
            Config config = new Config();
            config.useSingleServer().setAddress(uri);
            this.redisson = Redisson.create(config);
            ZimbraLog.system.info("Setup RedissonClient to %s", uri);
        } catch (Exception ex) {
            ZimbraLog.system.fatal("Cannot setup RedissonClient to connect to %s", uri, ex);
            throw ex;
        }
    }

    public static RedissonClientHolder getInstance() {
        return  InstanceHolder.instance;
    }

    public RedissonClient getRedissonClient() {
        return redisson;
    }
}
