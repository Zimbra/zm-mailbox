package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

@Ignore // class is unfinished and not used yet
public class RedissonLockTest {
    JedisPool jedisPool;
    final UUID id = UUID.randomUUID();

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", RedisOnLocalhostZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setup() throws Exception {
        jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
        flushCacheBetweenTests();
    }

    protected void flushCacheBetweenTests() {
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.flushDB();
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

    @Test(timeout=5000)
    public void testExpire() throws InterruptedException {
        RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
        lock.lock(2, TimeUnit.SECONDS);

        final long startTime = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            public void run() {
                RedissonLock lock1 = new RedissonLock(jedisPool, lock.name, id);
                lock1.lock();
                long spendTime = System.currentTimeMillis() - startTime;
                Assert.assertTrue(spendTime < 2005);
                lock1.unlock();
                latch.countDown();
            };
        }.start();

        latch.await();

        lock.unlock();
    }

    @Test(timeout=5000)
    public void testGetHoldCount() {
        RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
        Assert.assertEquals(0, lock.getHoldCount());
        lock.lock();
        Assert.assertEquals(1, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(0, lock.getHoldCount());

        lock.lock();
        lock.lock();
        Assert.assertEquals(2, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(1, lock.getHoldCount());
        lock.unlock();
        Assert.assertEquals(0, lock.getHoldCount());
    }

    @Test(timeout=5000)
    public void testIsHeldByCurrentThreadOtherThread() throws InterruptedException {
        RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
        lock.lock();

        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            public void run() {
                RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
                Assert.assertFalse(lock.isHeldByCurrentThread());
                latch.countDown();
            };
        }.start();

        latch.await();
        lock.unlock();

        final CountDownLatch latch2 = new CountDownLatch(1);
        new Thread() {
            public void run() {
                RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
                Assert.assertFalse(lock.isHeldByCurrentThread());
                latch2.countDown();
            };
        }.start();

        latch2.await();
    }

    @Test(timeout=5000)
    public void testIsHeldByCurrentThread() {
        RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
        Assert.assertFalse(lock.isHeldByCurrentThread());
        lock.lock();
        Assert.assertTrue(lock.isHeldByCurrentThread());
        lock.unlock();
        Assert.assertFalse(lock.isHeldByCurrentThread());
    }

    @Test(timeout=5000)
    public void testIsLockedOtherThread() throws InterruptedException {
        RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
        lock.lock();

        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            public void run() {
                RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
                Assert.assertTrue(lock.isLocked());
                latch.countDown();
            };
        }.start();

        latch.await();
        lock.unlock();

        final CountDownLatch latch2 = new CountDownLatch(1);
        new Thread() {
            public void run() {
                RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
                Assert.assertFalse(lock.isLocked());
                latch2.countDown();
            };
        }.start();

        latch2.await();
    }

    @Test(timeout=5000)
    public void testIsLocked() {
        RedissonLock lock = new RedissonLock(jedisPool, "lock", id);
        Assert.assertFalse(lock.isLocked());
        lock.lock();
        Assert.assertTrue(lock.isLocked());
        lock.unlock();
        Assert.assertFalse(lock.isLocked());
    }

    @Test(timeout=5000)
    public void testLockUnlock() {
        RedissonLock lock = new RedissonLock(jedisPool, "lock1", id);
        lock.lock();
        lock.unlock();

        lock.lock();
        lock.unlock();
    }

    @Test(timeout=5000)
    public void testReentrancy() {
        RedissonLock lock = new RedissonLock(jedisPool, "lock1", id);
        lock.lock();
        lock.lock();
        lock.unlock();
        lock.unlock();
    }


    @Test(timeout=15000)
    @Ignore
    public void testConcurrency_SingleInstance() throws InterruptedException {
        final AtomicInteger lockedCounter = new AtomicInteger();

        int iterations = 15;
        testSingleInstanceConcurrency(iterations, new Runnable() {
            @Override
            public void run() {
                Lock lock = new RedissonLock(jedisPool, "testConcurrency_SingleInstance", id);
                ZimbraLog.test.debug("About to lock");
                lock.lock();
                ZimbraLog.test.debug("Locked");
                lockedCounter.set(lockedCounter.get() + 1);
                lock.unlock();
                ZimbraLog.test.debug("Unlocked");
            }
        });

        Assert.assertEquals(iterations, lockedCounter.get());
    }

    protected void testMultiInstanceConcurrency(int iterations, final Runnable runnable) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long watch = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            executor.execute(runnable);
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        ZimbraLog.test.debug("Elapsed time: %dms", System.currentTimeMillis() - watch);
    }

    protected void testSingleInstanceConcurrency(int iterations, final Runnable runnable) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            executor.execute(runnable);
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long elapsedTime = System.currentTimeMillis() - startTime;
        ZimbraLog.test.debug("Elapsed Time: %dms", elapsedTime);
    }
}
