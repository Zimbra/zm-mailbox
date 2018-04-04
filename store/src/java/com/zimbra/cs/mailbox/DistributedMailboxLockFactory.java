package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static AtomicInteger lockIdBase = new AtomicInteger();
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

    /** Shutdown RedissonClient used for this factory's access to locking */
    @Override
    public void close() {
        this.redisson.shutdown();
    }

    @VisibleForTesting
    public int getQueueLength() {
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
        private final int id;
        private final long start;
        private boolean write;
        private RLock lock;
        private final int initialReadHoldCount;
        private final int initialWriteHoldCount;
        private final String where = ZimbraLog.getStackTrace(10);

        private DistributedMailboxLock(final RReadWriteLock readWriteLock, final boolean write) {
            this.rwLock = readWriteLock;
            this.write = write;
            this.lock = this.write ? readWriteLock.writeLock() : readWriteLock.readLock();
            id = lockIdBase.incrementAndGet();
            if (id >= 0xfffffff) {
                lockIdBase.set(1);  // keep id relatively short
            }
            start = System.currentTimeMillis();
            initialReadHoldCount = readWriteLock.readLock().getHoldCount();
            initialWriteHoldCount = readWriteLock.writeLock().getHoldCount();
            ZimbraLog.mailboxlock.info("constructor %s", this);
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
                                queueLength, this, ZimbraLog.getStackTrace(16));
                    }
                    throw new LockFailedException("too many waiters: " + queueLength);
                }

                synchronized (waiters) {
                    waiters.add(this.lock);
                }
                try {
                    if (!tryLockWithTimeout()) {
                        if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                            ZimbraLog.mailboxlock.trace("lock Failed to acquire %s\n%s",
                                    this, ZimbraLog.getStackTrace(16));
                        }
                        throw new LockFailedException(
                                "Failed to acquire DistributedMailboxLock { \"lockId\": \"" +
                                        this.rwLock.getName() + "\" }");
                    }
                } finally {
                    synchronized (waiters) {
                        waiters.remove(this.lock);
                    }
                }
            } catch (final InterruptedException ex) {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("lock Failed to acquire %s\n%s",
                            this, ZimbraLog.getStackTrace(16), ex);
                }
                throw new LockFailedException(
                        "Failed to acquire DistributedMailboxLock { \"lockId\": \"" +
                                this.rwLock.getName() + "\" }", ex);
            }
        }

        private long leaseSeconds() {
            return write ? LC.zimbra_mailbox_lock_write_lease_seconds.longValue() :
                LC.zimbra_mailbox_lock_read_lease_seconds.longValue();
        }

        private boolean tryLock() throws InterruptedException {
            boolean result = this.lock.tryLock(0, leaseSeconds(), TimeUnit.SECONDS);
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("tryLock result=%s %s\n%s", result, this,
                        ZimbraLog.getStackTrace(16));
            }
            return result;
        }

        private boolean tryLockWithTimeout() throws InterruptedException {
            boolean result = this.lock.tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), leaseSeconds(),
                    TimeUnit.SECONDS);
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("tryLockWithTimeout result=%s %s\n%s", result, this,
                        ZimbraLog.getStackTrace(16));
            }
            return result;
        }

        @Override
        public void close() {
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("close [START] %s\n%s", this, ZimbraLog.getStackTrace(16));
            }
            int iters;
            iters = rwLock.writeLock().getHoldCount() - initialWriteHoldCount;
            try {
                while (iters > 0) {
                    rwLock.writeLock().unlock();
                    iters--;
                }
            } catch (IllegalMonitorStateException imse) {
                /* most likely cause is that the lease on the lock has run out */
                ZimbraLog.mailboxlock.info("closing writelocks problem (ignoring) %s", this, imse);
            }
            iters = rwLock.readLock().getHoldCount() - initialReadHoldCount;
            try {
                while (iters > 0) {
                    rwLock.readLock().unlock();
                    iters--;
                }
            } catch (IllegalMonitorStateException imse) {
                /* most likely cause is that the lease on the lock has run out */
                ZimbraLog.mailboxlock.info("closing readlocks problem (ignoring) %s", this, imse);
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
            if ((System.currentTimeMillis() - start) > 1000) {
                /* Took a long time.  Log where got constructed. */
                ZimbraLog.mailboxlock.info("close() LONG-LOCK %s\n%s", this, where);
            } else {
                ZimbraLog.mailboxlock.info("close() %s", this);
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
                    iters, this, ZimbraLog.getStackTrace(16));
            } else {
                ZimbraLog.mailboxlock.info(
                    "releaseReadLocksBeforeWriteLock releasing %d read locks before writing %s",
                    iters, this);

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
                        this, ZimbraLog.getStackTrace(16));
            }
            this.write = true;
            this.lock = rwLock.writeLock();
        }

        private void addIfNot(ToStringHelper helper, String desc, int test, int actual) {
            if (actual != test) {
                helper.add(desc, actual);
            }
        }

        private String toString(RLock lck) {
            ToStringHelper helper = Objects.toStringHelper(lck);
            if (lck.isLocked()) {
                helper.add("locked", lck.isLocked());
            }
            if (lck.getHoldCount() != 0) {
                helper.add("holds", lck.getHoldCount());
            }
            if (!lck.isExists()) {
                helper.add("exists", lck.isExists());
            }
            if (lck.remainTimeToLive() == -1) {
                helper.add("ttl", "forever");
            } else if (lck.remainTimeToLive() != -2 /* -2 means key does not exist */) {
                helper.add("ttl", lck.remainTimeToLive());
            }
            return helper.toString();
        }

        private String toString(RReadWriteLock lck) {
            ToStringHelper helper = Objects.toStringHelper(lck)
                .add("name", lck.getName());
            if (lck.remainTimeToLive() == -1) {
                helper.add("ttl", "forever");
            } else if (lck.remainTimeToLive() != -2 /* -2 means key does not exist */) {
                helper.add("ttl", lck.remainTimeToLive());
            }
            helper.add("read", toString(lck.readLock()));
            helper.add("write", toString(lck.writeLock()));
            return helper.toString();
        }

        @Override
        public String toString() {
            ToStringHelper helper = Objects.toStringHelper(this)
                .add("id", Integer.toHexString(id))
                .add("write", write)
                .add("rwLock", toString(rwLock));
            addIfNot(helper, "initialReadHoldCount", 0, initialReadHoldCount);
            addIfNot(helper, "initialWriteHoldCount", 0, initialWriteHoldCount);
            helper.addValue(ZimbraLog.elapsedSince(start));
            return helper.toString();
        }
    }
}
