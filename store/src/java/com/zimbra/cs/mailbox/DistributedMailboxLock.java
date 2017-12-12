package com.zimbra.cs.mailbox;

import com.zimbra.common.mailbox.MailboxLock;
import org.redisson.api.RLock;

public class DistributedMailboxLock implements MailboxLock {
    private final RLock lock;
    private final boolean write;

    public DistributedMailboxLock(final RLock lock, final boolean write) {
        this.lock = lock;
        this.write = write;
    }

    @Override
    public void lock() {
        this.lock.lock();
    }

    @Override
    public void close() {
        this.lock.unlock();
    }

    @Override
    public int getHoldCount() {
        return this.lock.getHoldCount();
    }

    @Override
    public boolean isWriteLock() {
        return this.write;
    }

    @Override
    public boolean isWriteLockedByCurrentThread() {
        if (this.write) {
            return this.lock.isHeldByCurrentThread();
        }
        return false;
    }

    @Override
    public boolean isUnlocked() {
        return !this.lock.isLocked();
    }
}