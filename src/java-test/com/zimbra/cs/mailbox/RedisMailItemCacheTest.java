/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link RedisMailItemCache}.
 */
public final class RedisMailItemCacheTest extends AbstractMailItemCacheTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", RedisOnLocalhostZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Override
    protected MailItemCache constructCache() throws ServiceException {
        MailItemCache cache = new RedisMailItemCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        return cache;
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        JedisPool jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
        try {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.get("");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        JedisPool jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
        }
    }

    @Test
    public void testRemoveAllOfOneMailboxesItemsRemovesKeys() throws Exception {
        Assume.assumeTrue(isExternalCacheAvailableForTest());
        cache = constructCache();
        Assert.assertNotNull(cache);

        // Put an inbox folder into the cache
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        cache.put(mbox, mbox.getFolderById(Mailbox.ID_FOLDER_INBOX));

        // Remove all cached items for the mailbox
        cache.remove(mbox);

        // Ensure key are gone
        JedisPool jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
        try (Jedis jedis = jedisPool.getResource()) {
            Assert.assertNull(jedis.get(RedisMailItemCache.idsPerMailboxKey(mbox)));
            Assert.assertNull(jedis.get(RedisMailItemCache.uuidsPerMailboxKey(mbox)));
        }

    }
}
