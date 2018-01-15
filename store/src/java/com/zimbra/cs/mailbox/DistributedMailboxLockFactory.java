package com.zimbra.cs.mailbox;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.util.ZimbraLog;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DistributedMailboxLockFactory implements MailboxLockFactory {
    private final Mailbox mailbox;

    private final RedissonClient redisson;
    private RReadWriteLock readWriteLock;
    private List<RLock> waiters;

    private final static String HOST = "redis";
    private final static String PORT = "6379";

    public DistributedMailboxLockFactory(final Mailbox mailbox) {
        this.mailbox = mailbox;

        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + HOST + ":" + PORT);
        this.redisson = Redisson.create(config);

        try {
            this.readWriteLock = this.redisson.getReadWriteLock("mailbox:" + this.mailbox.getAccountId());
            this.waiters = new ArrayList<>();
        } catch (Exception e) {
            ZimbraLog.system.fatal("Can't instantiate Redisson server", e);

        }
    }

    @Override
    public MailboxLock readLock() {
        return new DistributedMailboxLock(this.readWriteLock, false);
    }

    @Override
    public MailboxLock writeLock() {
        return new DistributedMailboxLock(this.readWriteLock, true);
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
        this.redisson.shutdown();
    }

    @VisibleForTesting
    int getQueueLength() {
        List<RLock> newWaiters = new ArrayList<>();
        RLock lock;
        for (int i = 0; i < waiters.size(); i++) {
            lock = waiters.get(i);
            if (lock.isExists() && lock.remainTimeToLive() > 0) {
                newWaiters.add(lock);
            }
        }
        waiters = newWaiters;
        return waiters.size();
    }

    public class DistributedMailboxLock implements MailboxLock {
        private final RReadWriteLock readWriteLock;
        private boolean write;
        private RLock lock;
        private int counterCalls;


        public DistributedMailboxLock(final RReadWriteLock readWriteLock, final boolean write) {
            this.readWriteLock = readWriteLock;
            this.write = write;
            this.lock = this.write ? readWriteLock.writeLock() : readWriteLock.readLock();
        }

        @Override
        public void lock() {
            assert (neverReadBeforeWrite());
            try {
                if (tryLock()) {
                    counterCalls++;
                    return;
                }

                int queueLength = getQueueLength();
                if (waiters.size() >= LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
                    throw new LockFailedException("too many waiters: " + queueLength);
                }

                synchronized (waiters) {
                    waiters.add(this.lock);
                }
                if (!tryLockWithTimeout()) {
                    throw new LockFailedException("Failed to acquire DistributedMailboxLock { \"lockId\": \"" + this.readWriteLock.getName() + "\" }");
                }
                counterCalls++;
            } catch (final InterruptedException ex) {
                throw new LockFailedException("Failed to acquire DistributedMailboxLock { \"lockId\": \"" + this.readWriteLock.getName() + "\" }", ex);
            }
        }

        private boolean tryLock() throws InterruptedException {
            return this.lock.tryLock(0, TimeUnit.SECONDS);
        }

        private boolean tryLockWithTimeout() throws InterruptedException {
            return this.lock.tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
        }

        @Override
        public void close() {
            if (this.lock.isHeldByCurrentThread()){
                this.lock.unlock();
                if (counterCalls > 1) {
                    counterCalls--;
                    this.close();
                }
            }
        }

        @Override
        public int getHoldCount() {
            // eric: I feel like summing read + write lock hold count here is strange, but this is being done to
            // match the behavior of LocalMailboxLock
            return this.readWriteLock.readLock().getHoldCount() + this.readWriteLock.writeLock().getHoldCount();
        }

        @Override
        public boolean isWriteLock() {
            return this.write;
        }

        @Override
        public boolean isWriteLockedByCurrentThread() {
            return this.readWriteLock.writeLock().isHeldByCurrentThread();
        }

        @Override
        public boolean isUnlocked() {
            return this.getHoldCount() == 0;
        }

        private boolean neverReadBeforeWrite() {
            if (isCurrentThreadReading() && isNewWriteLock()) {
                final LockFailedException lfe = new LockFailedException("read lock held before write");
                ZimbraLog.mailbox.error(lfe.getMessage(), lfe);
                throw lfe;
            }
            return true;
        }

        private boolean isNewWriteLock() {
            return write && !this.isWriteLockedByCurrentThread();
        }

        private boolean isCurrentThreadReading() {
            return this.readWriteLock.readLock().isHeldByCurrentThread();
        }

        public void changeToWriteLock() {
            this.write = true;
            this.lock = readWriteLock.writeLock();
        }
    }
}