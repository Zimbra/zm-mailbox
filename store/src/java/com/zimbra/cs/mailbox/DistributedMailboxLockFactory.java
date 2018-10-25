package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.redis.RedisUtils;

public class DistributedMailboxLockFactory implements MailboxLockFactory {
    private static AtomicInteger lockIdBase = new AtomicInteger();
    private final Mailbox mailbox;
    private final RedissonClient redisson;
    private RReadWriteLock readWriteLock;
    private ReentrantReadWriteLock localLock;
    private List<RLock> waiters;

    public DistributedMailboxLockFactory(final Mailbox mailbox) {
        this.mailbox = mailbox;
        this.redisson = RedissonClientHolder.getInstance().getRedissonClient();

        try {
            String lockName = RedisUtils.createAccountRoutedKey(this.mailbox.getAccountId(), "LOCK");
            this.readWriteLock = this.redisson.getReadWriteLock(lockName);
            this.waiters = new ArrayList<>();
            this.localLock = new ReentrantReadWriteLock();
        } catch (Exception e) {
            ZimbraLog.system.fatal("Can't instantiate Redisson server", e);
        }
    }

    @Override
    public MailboxLock readLock() {
        return new LocalMailboxLock(localLock, false);
    }

    @Override
    public MailboxLock writeLock() {
        return new LocalMailboxLock(localLock, true);
    }

    @Override
    public MailboxLock acquiredWriteLock() throws ServiceException {
        MailboxLock myLock = writeLock();
        myLock.lock();
        return myLock;
    }

    @Override
    public MailboxLock acquiredReadLock() throws ServiceException {
        MailboxLock myLock = readLock();
        myLock.lock();
        return myLock;
    }

    /**
     * Number of holds on this lock by the current thread (sum of read and write locks)
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    @Override
    public int getHoldCount() {
        //returns only the local hold count
        return localLock.getReadHoldCount() + localLock.getWriteHoldCount();
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
    public class LocalMailboxLock implements MailboxLock {
        private final ReentrantReadWriteLock localLock;
        private final int id;
        private final long construct_start;
        private Long time_got_lock = null;
        private boolean write;
        private RLock lock;
        private final int initialReadHoldCount;
        private final int initialWriteHoldCount;

        private LocalMailboxLock(final ReentrantReadWriteLock localLock, final boolean write) {
            this.localLock = localLock;
            this.write = write;
            this.lock = this.write ? readWriteLock.writeLock() : readWriteLock.readLock();
            id = lockIdBase.incrementAndGet();
            if (id >= 0xfffffff) {
                lockIdBase.set(1);  // keep id relatively short
            }
            construct_start = System.currentTimeMillis();
            initialReadHoldCount = localLock.getReadHoldCount();
            initialWriteHoldCount = localLock.getWriteHoldCount();
            ZimbraLog.mailboxlock.trace("constructor %s", this);
        }

        @Override
        public void lock() throws ServiceException {
            releaseReadLocksBeforeWriteLock();
            long lock_start = System.currentTimeMillis();
            try {
                if (tryLock()) {
                    ZimbraLog.mailboxlock.trace("lock() tryLock succeeded lock cost=%s",
                                ZimbraLog.elapsedSince(lock_start));
                    return;
                }

                int queueLength = getQueueLength();
                if (waiters.size() >= LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
                    ZimbraLog.mailboxlock.trace("too many waiters (%d) %s", queueLength, this);
                    throw ServiceException.LOCK_FAILED(String.format("lock failed: too many waiters (%d) %s", queueLength, this));
                }

                synchronized (waiters) {
                    waiters.add(this.lock);
                }
                try {
                    if (!tryLockWithTimeout()) {
                        ZimbraLog.mailboxlock.trace("lock() tryLockWithTimeout failed lock cost=%s",
                            ZimbraLog.elapsedSince(lock_start));
                        throw ServiceException.LOCK_FAILED(String.format("Failed to acquire %s - tryLockWithTimeout failed", this));
                    }
                } finally {
                    synchronized (waiters) {
                        waiters.remove(this.lock);
                    }
                }
            } catch (InterruptedException ex) {
                ZimbraLog.mailboxlock.trace("lock() Failed to acquire %s lock cost=%s", this,
                        ZimbraLog.elapsedSince(lock_start), ex);
                throw ServiceException.LOCK_FAILED(String.format("Failed to acquire %s - interrupted", this));
            }
            ZimbraLog.mailboxlock.trace("lock() tryLockWithTimeout succeeded lock cost=%s",
                    ZimbraLog.elapsedSince(lock_start));
        }

        private long leaseSeconds() {
            return write ? LC.zimbra_mailbox_lock_write_lease_seconds.longValue() :
                LC.zimbra_mailbox_lock_read_lease_seconds.longValue();
        }

        private boolean tryLock() throws InterruptedException {
            boolean result = this.lock.tryLock(0, leaseSeconds(), TimeUnit.SECONDS);
            if (result) {
                time_got_lock = System.currentTimeMillis();
            }
            ZimbraLog.mailboxlock.trace("tryLock result=%s %s", result, this);
            return result;
        }

        private boolean tryLockWithTimeout() throws InterruptedException {
            boolean result = this.lock.tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), leaseSeconds(),
                    TimeUnit.SECONDS);
            if (result) {
                time_got_lock = System.currentTimeMillis();
            }
            ZimbraLog.mailboxlock.trace("tryLockWithTimeout result=%s %s", result, this);
            return result;
        }

        @Override
        public void close() {
            long close_start = System.currentTimeMillis();
            restoreToInitialLockCount(true);
            restoreToInitialLockCount(false);
            reinstateReadLocks();
            if ((time_got_lock != null) && (System.currentTimeMillis() - time_got_lock) >
            LC.zimbra_mailbox_lock_long_lock_milliseconds.longValue()) {
                /* Took a long time.*/
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.warn("close() LONG-LOCK %s %s", this, ZimbraLog.getStackTrace(16));
                } else {
                    ZimbraLog.mailboxlock.warn("close() LONG-LOCK %s", this);
                }
            } else {
                ZimbraLog.mailboxlock.trace("close() unlock cost=%s %s", ZimbraLog.elapsedSince(close_start), this);
            }
        }

        /**
         * Number of holds on this lock by the current thread (sum of read and write locks)
         * @return holds or <code>0</code> if this lock is not held by current thread
         */
        @Override
        public int getHoldCount() {
            return localLock.getReadHoldCount() + localLock.getWriteHoldCount();
        }

        @Override
        public boolean isWriteLock() {
            return this.write;
        }

        @Override
        public boolean isWriteLockedByCurrentThread() {
            return this.localLock.isWriteLockedByCurrentThread();
        }

        @Override
        public boolean isReadLockedByCurrentThread() {
            return this.localLock.getReadHoldCount() > 0;
        }

        @Override
        public boolean isLockedByCurrentThread() {
            return isWriteLockedByCurrentThread() || isReadLockedByCurrentThread();
        }

        private void restoreToInitialLockCount(boolean write) {
            int holdCount = write ? localLock.getWriteHoldCount() : localLock.getReadHoldCount();
            int initialHoldCount = write ? initialWriteHoldCount : initialReadHoldCount;
            Lock subLock = write ? localLock.writeLock() : localLock.readLock();
            int iters = holdCount - initialHoldCount;
            try {
                while (iters > 0) {
                    subLock.unlock();
                    iters--;
                }
            } catch (IllegalMonitorStateException imse) {
                /* most likely cause is that the lease on the lock has run out */
                String lockType = write ? "write" : "read";
                ZimbraLog.mailboxlock.info("closing %slocks problem (ignoring) %s", lockType, this, imse);
            }
        }

        /** If we upgraded to a write lock, we may need to reinstate read locks that we released */
        private void reinstateReadLocks() {
            int iters = initialReadHoldCount - localLock.getReadHoldCount();
            if (ZimbraLog.mailboxlock.isTraceEnabled() && (iters > 0)) {
                ZimbraLog.mailboxlock.trace("close - re-instating %d read locks", iters);
            }
            while (iters > 0) {
                boolean result = localLock.readLock().tryLock();
                ZimbraLog.mailboxlock.trace("close readLock().tryLock() return=%s", result);
                iters--;
            }
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
                return; /* if we've got a write lock, then don't need to release read locks */
            }
            int iters = localLock.getReadHoldCount();
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
            while ((iters > 0) && (localLock.getReadHoldCount() > 0)) {
                localLock.readLock().unlock();
                iters--;
            }
        }

        private void addIfNot(MoreObjects.ToStringHelper helper, String desc, int test, int actual) {
            if (actual != test) {
                helper.add(desc, actual);
            }
        }

        private void addTimingInfo(MoreObjects.ToStringHelper helper) {
            if (time_got_lock == null) {
                helper.add("sinceConstruction", ZimbraLog.elapsedSince(construct_start));
                return;
            }
            helper.add("waited4lock", String.format("%dms", time_got_lock - construct_start));
            helper.add("sinceLocked", ZimbraLog.elapsedSince(time_got_lock));
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this)
                .add("id", Integer.toHexString(id))
                .add("write", write)
                .add("rwLock", localLock);
            addIfNot(helper, "initialReadHoldCount", 0, initialReadHoldCount);
            addIfNot(helper, "initialWriteHoldCount", 0, initialWriteHoldCount);
            addTimingInfo(helper);
            return helper.toString();
        }
    }
}
