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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraConfig;
import com.zimbra.qless.QlessClient;

public class RedisCoordinatedLocalMailboxLockTest extends AbstractMailboxLockTest {
    static final int TESTKEYS_EXPIRE_SECS = 15;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", MyConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    protected boolean isLockServiceAvailableForTest() throws Exception {
        if (Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisClusterAvailable()) {
            return false;
        }
        return Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisAvailable();
    }

    @Before
    @SuppressWarnings("unchecked")
    public void resetStoreBetweenTests() throws Exception {
        Pool<Jedis> jedisPool = Zimbra.getAppContext().getBean(Pool.class);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
        }
    }

    /** Test timing out waiting for a lock that we won't get */
    @Test(timeout=3000)
    @SuppressWarnings("unchecked")
    public void multiProcessLockTimeout() throws Exception {
        Pool<Jedis> jedisPool = Zimbra.getAppContext().getBean(Pool.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RedisCoordinatedLocalMailboxLock lock = new RedisCoordinatedLocalMailboxLock(jedisPool, mbox);

        // Setup state in Redis to act as if another server is holding the mailbox locks
        String key = lock.key(RedisCoordinatedLocalMailboxLock.WORKER_SUFFIX);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, TESTKEYS_EXPIRE_SECS, "server.acme.org-123");
        }

        // Request lock, and wait
        long startTime = System.currentTimeMillis();
        Provisioning.getInstance().getLocalServer().setMailBoxLockTimeout(1);
        try {
            lock.lock();
            Assert.fail("Expected a lock timeout");
        } catch (RuntimeException e) {}
        long elapsedTime = System.currentTimeMillis() - startTime;
        assert(elapsedTime < 2000);
    }

    /** Test acquiring a lock that first required a subscribe-and-wait */
    @Test(timeout=5000)
    @SuppressWarnings("unchecked")
    public void multiProcessLockSubscribeWaitNotifyThenAcquire() throws Exception {
        Pool<Jedis> jedisPool = Zimbra.getAppContext().getBean(Pool.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RedisCoordinatedLocalMailboxLock lock = new RedisCoordinatedLocalMailboxLock(jedisPool, mbox);

        // Setup state in Redis to act as if another server is holding the mailbox locks
        String key = lock.key(RedisCoordinatedLocalMailboxLock.WORKER_SUFFIX);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, TESTKEYS_EXPIRE_SECS, "server.acme.org-123");
        }

        // Perform the unlock in 1 seconds
        Thread thread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.del(key);
                    jedis.publish(lock.key(RedisCoordinatedLocalMailboxLock.CHANNEL_SUFFIX), RedisCoordinatedLocalMailboxLock.UNLOCK_NOTIFY_MESSAGE);
                }
            }
        };
        thread.start();

        // Request lock, waiting for up to 2 seconds
        Provisioning.getInstance().getLocalServer().setMailBoxLockTimeout(2);
        try {
            lock.lock();
        } catch (RuntimeException e) {
            Assert.fail("Failed to get notified of unlock before timeout");
        }
    }

    static class MyConfig extends RedisOnLocalhostZimbraConfig {
        @Bean
        @Override
        public List<MailboxListenerTransport> externalMailboxListeners() throws Exception {
            return Collections.emptyList();
        }

        @Bean
        @Override
        public QlessClient qlessClient() throws Exception {
            return null;
        }

        @Bean
        @Override
        // Unit tests need Mailbox to use a non-Redis MailboxLock adapter, so that mailbox ops don't get in the way of isolation testing
        public MailboxLockFactory mailboxLockFactory() throws ServiceException {
            return new MailboxLockFactory() {
                public MailboxLock create(String accountId, Mailbox mbox) throws ServiceException {
                    return new LocalMailboxLock(accountId, mbox);
                }
            };
        }
    }
}
