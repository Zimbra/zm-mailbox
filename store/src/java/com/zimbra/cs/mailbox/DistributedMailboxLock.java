package com.zimbra.cs.mailbox;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import com.zimbra.common.util.ZimbraLog;


public class DistributedMailboxLock {
    private Config config;
    private RedissonClient redisson;
    private RLock lock;
    private final static String HOST = "192.168.99.100";
    private final static String PORT = "6379";

    public DistributedMailboxLock(final String id, final Mailbox mbox) {
        try {
            config = new Config();
            config.useSingleServer().setAddress(HOST + ":" + PORT);
            redisson = Redisson.create(config);
            lock = redisson.getLock("mailbox:" + id);
        } catch (Exception e) {
            ZimbraLog.system.fatal("Can't instantiate Redisson server", e);
            System.exit(1);
        }
    }

    public void lock() {
        lock(true);
    }

    public void lock(final boolean write) {
        lock.lock();
    }

    public void release() {
        lock.unlock();
    }

    public boolean isWriteLockedByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }

    int getHoldCount() {
        if (lock.isHeldByCurrentThread()) {
            return 1;
        };
        return 0;
    }

    public boolean isUnlocked() {
        return !lock.isHeldByCurrentThread();
    }

    public void shutdown() {
        redisson.shutdown();
    }
}
