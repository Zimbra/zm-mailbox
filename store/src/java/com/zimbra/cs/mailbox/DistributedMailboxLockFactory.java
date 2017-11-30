package com.zimbra.cs.mailbox;

import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.util.ZimbraLog;
import org.redisson.Redisson;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class DistributedMailboxLockFactory implements MailboxLockFactory {
    private Config config;
    private RedissonClient redisson;
    private RReadWriteLock readWriteLock;
    private final static String HOST = "redis";
    private final static String PORT = "6379";

    public DistributedMailboxLockFactory(final String id, final Mailbox mbox) {
        try {
            config = new Config();
            config.useSingleServer().setAddress(HOST + ":" + PORT);
            redisson = Redisson.create(config);
            readWriteLock = redisson.getReadWriteLock("mailbox:" + id);
        } catch (Exception e) {
            ZimbraLog.system.fatal("Can't instantiate Redisson server", e);
            System.exit(1);
        }
    }

    @Override
    public MailboxLock readLock() {
        return new DistributedMailboxLock(readWriteLock.readLock(), false);
    }

    @Override
    public MailboxLock writeLock() {
        return new DistributedMailboxLock(readWriteLock.writeLock(), true);
    }

    @Override
    @Deprecated
    public MailboxLock lock(final boolean write) {
        if (write) {
            return writeLock();
        }
        return readLock();
    }

    @Override
    public void close() {
        redisson.shutdown();
    }
}