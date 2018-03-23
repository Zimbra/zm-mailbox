package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.LockFailedException;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.util.ZimbraLog;

public class DistributedMailboxLockFactory implements MailboxLockFactory {
    private final Mailbox mailbox;
    private final RedissonClient redisson;
    private RReadWriteLock readWriteLock;
    private List<RLock> waiters;

    public DistributedMailboxLockFactory(final Mailbox mailbox) {
        this.mailbox = mailbox;
        this.redisson = RedissonClientHolder.getInstance().getRedissonClient();

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
    public MailboxLock acquiredWriteLock() {
        MailboxLock myLock = writeLock();
        myLock.lock();
        return myLock;
    }

    @Override
    public MailboxLock acquiredReadLock() {
        MailboxLock myLock = readLock();
        myLock.lock();
        return myLock;
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
    public int getQueueLength() {
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

    /**
     * Intended to handle the life-cycle of a single lock/unlock of a mailbox, hence there
     * isn't an <b>unlock</b> method.  Unlocking is intended to be performed by {@link #close()}.
     * In theory, more than one call can be made to {@link #lock()} but all those locks will be unlocked
     * by close()
     * Should NOT be used from multiple threads - this object relies on underlying thread specific
     * hold counts for correct functioning.
     *
     * Note: MUST either use this with "try with resources" or ensure that {@link #close()} is
     *       called in finally block wrapped round the call to the constructor
     */
    public class DistributedMailboxLock implements MailboxLock {
        private final RReadWriteLock rwLock;
        private boolean write;
        private RLock lock;
        private final int initialReadHoldCount;
        private final int initialWriteHoldCount;

        private DistributedMailboxLock(final RReadWriteLock readWriteLock, final boolean write) {
            this.rwLock = readWriteLock;
            this.write = write;
            this.lock = this.write ? readWriteLock.writeLock() : readWriteLock.readLock();
            initialReadHoldCount = readWriteLock.readLock().getHoldCount();
            initialWriteHoldCount = readWriteLock.writeLock().getHoldCount();
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("constructor %s\n%s", this, ZimbraLog.getStackTrace(10));
            }
        }

        public DistributedMailboxLock createAndAcquireWriteLock(final RReadWriteLock rReadWriteLock) {
            DistributedMailboxLock myLock = new DistributedMailboxLock(rReadWriteLock, true);
            myLock.lock();
            return myLock;
        }

        public DistributedMailboxLock createAndAcquireReadLock(final RReadWriteLock rReadWriteLock) {
            DistributedMailboxLock myLock = new DistributedMailboxLock(rReadWriteLock, false);
            myLock.lock();
            return myLock;
        }

        @Override
        public void lock() {
            releaseReadLocksBeforeWriteLock();
            try {
                if (tryLock()) {
                    return;
                }

                int queueLength = getQueueLength();
                if (waiters.size() >= LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
                    if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                        ZimbraLog.mailboxlock.trace("lock Failing lock - too many waiters %d %s\n%s",
                                queueLength, this, ZimbraLog.getStackTrace(10));
                    }
                    throw new LockFailedException("too many waiters: " + queueLength);
                }

                synchronized (waiters) {
                    waiters.add(this.lock);
                }
                if (!tryLockWithTimeout()) {
                    if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                        ZimbraLog.mailboxlock.trace("lock Failed to acquire %s\n%s",
                                this, ZimbraLog.getStackTrace(10));
                    }
                    throw new LockFailedException(
                            "Failed to acquire DistributedMailboxLock { \"lockId\": \"" +
                                    this.rwLock.getName() + "\" }");
                }
            } catch (final InterruptedException ex) {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("lock Failed to acquire %s\n%s",
                            this, ZimbraLog.getStackTrace(10), ex);
                }
                throw new LockFailedException(
                        "Failed to acquire DistributedMailboxLock { \"lockId\": \"" +
                                    this.rwLock.getName() + "\" }", ex);
            }
        }

        private boolean tryLock() throws InterruptedException {
            long start = System.currentTimeMillis();
            boolean result = this.lock.tryLock(0, TimeUnit.SECONDS);
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("tryLock result=%s %s %s\n%s", result, this,
                        ZimbraLog.elapsedSince(start), ZimbraLog.getStackTrace(10));
            }
            return result;
        }

        private boolean tryLockWithTimeout() throws InterruptedException {
            long start = System.currentTimeMillis();
            boolean result = this.lock.tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("tryLockWithTimeout result=%s %s %s\n%s", result,
                        this, ZimbraLog.elapsedSince(start), ZimbraLog.getStackTrace(10));
            }
            return result;
        }

        @Override
        public void close() {
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("close [START] %s\n%s", this, ZimbraLog.getStackTrace(10));
            }
            int iters;
            iters = rwLock.writeLock().getHoldCount() - initialWriteHoldCount;
            while (iters > 0) {
                rwLock.writeLock().unlock();
                iters--;
            }
            iters = rwLock.readLock().getHoldCount() - initialReadHoldCount;
            while (iters > 0) {
                rwLock.readLock().unlock();
                iters--;
            }
            /* If we upgraded to a write lock, we may need to reinstate a read lock, that we released */
            iters = initialReadHoldCount - rwLock.readLock().getHoldCount();
            if (ZimbraLog.mailboxlock.isTraceEnabled() && (iters > 0)) {
                ZimbraLog.mailboxlock.trace("close - re-instating %d read locks", iters);
            }
            while (iters > 0) {
                boolean result = rwLock.readLock().tryLock();
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("close readLock().tryLock() return=%s", result);
                }
                iters--;
            }
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("close[END] %s", this);
            }
        }

        /**
         * Number of holds on this lock by the current thread (sum of read and write locks)
         * @return holds or <code>0</code> if this lock is not held by current thread
         */
        @Override
        public int getHoldCount() {
            // eric: I feel like summing read + write lock hold count here is strange, but this is being done to
            // match the behavior of LocalMailboxLock
            return this.rwLock.readLock().getHoldCount() + this.rwLock.writeLock().getHoldCount();
        }

        @Override
        public boolean isWriteLock() {
            return this.write;
        }

        @Override
        public boolean isWriteLockedByCurrentThread() {
            return this.rwLock.writeLock().isHeldByCurrentThread();
        }

        @Override
        public boolean isUnlocked() {
            return this.getHoldCount() == 0;
        }

        /**
         * Philosophy is that if we want to write when we have a read lock, then we were only
         * reading before anyway, so it is ok to release all locks with a view to getting a
         * write lock soon - doesn't matter if other things read/write in the mean time
         */
        private void releaseReadLocksBeforeWriteLock() {
            if (!write) {
                return;  /* we're not trying to write anyway */
            }
            if (isWriteLockedByCurrentThread()) {
                return; /* if we've got a write lock, then we can't possibly have a read lock */
            }
            int iters = rwLock.readLock().getHoldCount();
            if (iters == 0) {
                return; /* this thread isn't holding any locks */
            }
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace(
                    "releaseReadLocksBeforeWriteLock releasing %d read locks before writing %s\n%s",
                    iters, this, ZimbraLog.getStackTrace(10));
            }
            /* close() should get these locks again later */
            while ((iters > 0) && (rwLock.readLock().getHoldCount() > 0)) {
                rwLock.readLock().unlock();
                iters--;
            }
        }

        public void changeToWriteLock() {
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("changeToWriteLock %s\n%s",
                        this, ZimbraLog.getStackTrace(10));
            }
            this.write = true;
            this.lock = rwLock.writeLock();
        }

        private void addIfNot(ToStringHelper helper, String desc, int test, int actual) {
            if (actual != test) {
                helper.add(desc, actual);
            }
        }

        @Override
        public String toString() {
            ToStringHelper helper = Objects.toStringHelper(this)
                .add("write", write)
                .add("rwLock", rwLock)
                .add("lock", String.format("(%s class=%s)", lock.getName(), lock.getClass().getName()));
            boolean isLocked;
            int hCount;
            isLocked = rwLock.readLock().isLocked();
            hCount = rwLock.readLock().getHoldCount();
            if (isLocked || hCount != 0) {
                helper.add("readLock", String.format("(locked=%s holds=%d)", isLocked, hCount));
            }
            isLocked = rwLock.writeLock().isLocked();
            hCount = rwLock.writeLock().getHoldCount();
            if (isLocked || hCount != 0) {
                helper.add("writeLock", String.format("(locked=%s holds=%d)", isLocked, hCount));
            }
            addIfNot(helper, "initialReadHoldCount", 0, initialReadHoldCount);
            addIfNot(helper, "initialWriteHoldCount", 0, initialWriteHoldCount);
            return helper.toString();
        }
    }
}
