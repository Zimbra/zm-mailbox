/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import org.junit.BeforeClass;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraConfig;

public class RedisMailboxLockTest extends AbstractMailboxLockTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", RedisOnLocalhostZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    protected boolean isLockServiceAvailableForTest() throws Exception {
        return Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisAvailable();
    }

    protected void resetStoreBetweenTests() throws Exception {
        JedisPool jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.flushDB();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }
}
