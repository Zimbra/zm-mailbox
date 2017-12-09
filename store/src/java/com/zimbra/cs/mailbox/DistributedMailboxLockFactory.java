package com.zimbra.cs.mailbox;

import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.util.ZimbraLog;
import org.redisson.Redisson;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class DistributedMailboxLockFactory implements MailboxLockFactory {
    private final Mailbox mailbox;
    private final Config config;
    private final RedissonClient redisson;
    private RReadWriteLock readWriteLock;
    private final static String HOST = "redis";
    private final static String PORT = "6379";

    public DistributedMailboxLockFactory(final Mailbox mailbox) {
        this.mailbox = mailbox;

        config = new Config();
        config.useSingleServer().setAddress(HOST + ":" + PORT);
        redisson = Redisson.create(config);

        try {
            readWriteLock = redisson.getReadWriteLock("mailbox:" + this.mailbox.getAccountId());
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
        if (write || this.mailbox.requiresWriteLock()) {
            return writeLock();
        }
        return readLock();
    }

    @Override
    public void close() {
        redisson.shutdown();
    }
}