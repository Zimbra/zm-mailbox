package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LC.Supported;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockContext;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.redis.lock.RedisLock;
import com.zimbra.cs.mailbox.redis.lock.RedisLock.LockResponse;
import com.zimbra.cs.mailbox.redis.lock.RedisReadWriteLock;
import com.zimbra.cs.mailbox.util.MailboxClusterUtil;

public class DistributedMailboxLockFactory implements MailboxLockFactory {
    private static AtomicInteger lockIdBase = new AtomicInteger();
    private final Mailbox mailbox;
    private final String accountId;
    private final RedissonClient redisson;
    private String zimbraLockBaseName;
    private String lockId;
    private RedisReadWriteLock redisLock;
    private ReentrantReadWriteLock localLock;
    private List<ReentrantReadWriteLock> waiters;
    private final Log log = ZimbraLog.mailboxlock;

    public DistributedMailboxLockFactory(final Mailbox mailbox) {
        this.mailbox = mailbox;
        this.accountId = mailbox.getAccountId();
        this.redisson = RedissonClientHolder.getInstance().getRedissonClient();
        this.lockId = MailboxClusterUtil.getMailboxWorkerName();
        try {
            zimbraLockBaseName = accountId + "-LOCK"; //actual name in redis will be hashtagged to be co-located with pubsub hannel
            this.waiters = new ArrayList<>();
            this.localLock = new ReentrantReadWriteLock();
            this.redisLock = new RedisReadWriteLock(mailbox.getAccountId(), zimbraLockBaseName, lockId);
        } catch (Exception e) {
            ZimbraLog.system.fatal("Can't instantiate Redisson server", e);
        }
    }

    @Override
    public MailboxLock readLock() {
        return new DistributedMailboxLock(localLock, redisLock.readLock(), false);
    }

    @Override
    public MailboxLock writeLock() {
        return new DistributedMailboxLock(localLock, redisLock.writeLock(), true);
    }

    @Override
    public MailboxLock acquiredWriteLock(MailboxLockContext lockContext) throws ServiceException {
        MailboxLock myLock = writeLock();
        myLock.lock(lockContext);
        return myLock;
    }

    @Override
    public MailboxLock acquiredReadLock(MailboxLockContext lockContext) throws ServiceException {
        MailboxLock myLock = readLock();
        myLock.lock(lockContext);
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

    public static long leaseSeconds(boolean write) {
        return write ? LC.zimbra_mailbox_lock_write_lease_seconds.longValue() :
            LC.zimbra_mailbox_lock_read_lease_seconds.longValue();
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
        private final int initialReadHoldCount;
        private final int initialWriteHoldCount;

        private LocalMailboxLock(final ReentrantReadWriteLock localLock, final boolean write) {
            this.localLock = localLock;
            this.write = write;
            id = lockIdBase.incrementAndGet();
            if (id >= 0xfffffff) {
                lockIdBase.set(1);  // keep id relatively short
            }
            construct_start = System.currentTimeMillis();
            initialReadHoldCount = localLock.getReadHoldCount();
            initialWriteHoldCount = localLock.getWriteHoldCount();
            if (log.isTraceEnabled()) {
                log.trace("constructor %s", this);
            }
        }

        @Override
        public void lock(MailboxLockContext lockContext) throws ServiceException {
            releaseReadLocksBeforeWriteLock();
            long lock_start = System.currentTimeMillis();
            try {
                if (tryLock()) {
                    if (log.isTraceEnabled()) {
                        log.trace("lock() tryLock succeeded lock cost=%s", ZimbraLog.elapsedSince(lock_start));
                    }
                    return;
                }

                int queueLength = getQueueLength();
                if (waiters.size() >= LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
                    if (log.isTraceEnabled()) {
                        log.trace("too many waiters (%d) %s", queueLength, this);
                    }
                    throw ServiceException.LOCK_FAILED(String.format("lock failed: too many waiters (%d) %s", queueLength, this));
                }

                synchronized (waiters) {
                    waiters.add(this.localLock);
                }
                try {
                    if (!tryLockWithTimeout()) {
                        if (log.isTraceEnabled()) {
                            log.trace("lock() tryLockWithTimeout failed lock cost=%s", ZimbraLog.elapsedSince(lock_start));
                        }
                        throw ServiceException.LOCK_FAILED(String.format("Failed to acquire %s - tryLockWithTimeout failed", this));
                    }
                } finally {
                    synchronized (waiters) {
                        waiters.remove(this.localLock);
                    }
                }
            } catch (InterruptedException ex) {
                if (log.isTraceEnabled()) {
                    log.trace("lock() Failed to acquire %s lock cost=%s", this, ZimbraLog.elapsedSince(lock_start), ex);
                }
                throw ServiceException.LOCK_FAILED(String.format("Failed to acquire %s - interrupted", this));
            }
            if (log.isTraceEnabled()) {
                log.trace("lock() tryLockWithTimeout succeeded lock cost=%s", ZimbraLog.elapsedSince(lock_start));
            }
        }

        private boolean tryLock() throws InterruptedException {
            boolean result;
            if (write) {
                result = localLock.writeLock().tryLock();
            } else {
                result = localLock.readLock().tryLock();
            }
            if (result) {
                time_got_lock = System.currentTimeMillis();
            }
            if (log.isTraceEnabled()) {
                log.trace("tryLock result=%s %s", result, this);
            }
            return result;
        }

        private boolean tryLockWithTimeout() throws InterruptedException {
            boolean result;
            if (write) {
                result = localLock.writeLock().tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
            } else {
                result = localLock.readLock().tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
            }
            if (result) {
                time_got_lock = System.currentTimeMillis();
            }
            if (log.isTraceEnabled()) {
                log.trace("tryLockWithTimeout result=%s %s", result, this);
            }
            return result;
        }

        protected boolean closeCommon() {
            long close_start = System.currentTimeMillis();
            restoreToInitialLockCount(true);
            restoreToInitialLockCount(false);
            boolean needToReinstate = reinstateReadLocks();
            if ((time_got_lock != null) && (System.currentTimeMillis() - time_got_lock) >
            LC.zimbra_mailbox_lock_long_lock_milliseconds.longValue()) {
                /* Took a long time.*/
                if (log.isTraceEnabled()) {
                    log.warn("close() LONG-LOCK %s %s", this, ZimbraLog.getStackTrace(16));
                } else {
                    log.warn("close() LONG-LOCK %s", this);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("close() unlock cost=%s %s", ZimbraLog.elapsedSince(close_start), this);
                }
            }
            return needToReinstate;
        }

        @Override
        public void close() {
            closeCommon();
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
            while (iters > 0) {
                subLock.unlock();
                iters--;
            }
        }

        /** If we upgraded to a write lock, we may need to reinstate read locks that we released */
        protected boolean reinstateReadLocks() {
            int iters = initialReadHoldCount - localLock.getReadHoldCount();
            boolean needToReinstate = iters > 0;
            if (iters > 0) {
                if (log.isTraceEnabled()) {
                    log.trace("close - re-instating %d read locks", iters);
                }
            }
            while (iters > 0) {
                boolean result = localLock.readLock().tryLock();
                if (log.isTraceEnabled()) {
                    log.trace("close readLock().tryLock() return=%s", result);
                }
                iters--;
            }
            return needToReinstate;
        }

        /**
         * Philosophy is that if we want to write when we have a read lock, then we were only
         * reading before anyway, so it is ok to release all locks with a view to getting a
         * write lock soon - doesn't matter if other things read/write in the mean time
         */
        protected boolean releaseReadLocksBeforeWriteLock() {
            if (!write) {
                return false;  /* we're not trying to write anyway */
            }
            if (isWriteLockedByCurrentThread()) {
                return false; /* if we've got a write lock, then don't need to release read locks */
            }
            int iters = localLock.getReadHoldCount();
            if (iters == 0) {
                return false; /* this thread isn't holding any locks */
            }
            if (log.isTraceEnabled()) {
                log.trace(
                    "releaseReadLocksBeforeWriteLock releasing %d read locks before writing %s\n%s",
                    iters, this, ZimbraLog.getStackTrace(16));
            } else {
                log.info(
                    "releaseReadLocksBeforeWriteLock releasing %d read locks before writing %s",
                    iters, this);

            }
            /* close() should get these locks again later */
            while ((iters > 0) && (localLock.getReadHoldCount() > 0)) {
                localLock.readLock().unlock();
                iters--;
            }
            return true;
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
                .add("account", accountId)
                .add("write", write)
                .add("rwLock", localLock);
            addIfNot(helper, "initialReadHoldCount", 0, initialReadHoldCount);
            addIfNot(helper, "initialWriteHoldCount", 0, initialWriteHoldCount);
            addTimingInfo(helper);
            return helper.toString();
        }
    }

    public class DistributedMailboxLock extends LocalMailboxLock {

        private RedisLock zRedisLock;
        private boolean write;

        public DistributedMailboxLock(ReentrantReadWriteLock localLock, RedisLock zRedisLock, boolean write) {
            super(localLock, write);
            this.zRedisLock = zRedisLock;
            this.write = write;
        }

        @Override
        public synchronized void lock(MailboxLockContext lockContext) throws ServiceException {
            if (getHoldCount() == 0) {
                if (log.isTraceEnabled()) {
                    log.trace("need to acquire redis lock for %s", this);
                }
                LockResponse response = acquireRedisLock();
                try {
                    super.lock(lockContext);
                } catch (ServiceException e) {
                    ZimbraLog.mailboxlock.warn("unable to acquire in-memory lock after acquiring redis lock!");
                    zRedisLock.unlock();
                    throw e;
                }
                if (!LC.lock_based_cache_invalidation_enabled.booleanValue()) {
                    return;
                }
                String lastWriter = response.getLastWriter();
                Mailbox mailbox = (Mailbox) lockContext.getMailboxStore();
                if (Strings.isNullOrEmpty(lastWriter)) {
                    if (ZimbraLog.cache.isDebugEnabled()) {
                        ZimbraLog.cache.debug("unable to determine last mailbox worker to acquire write lock, flushing local caches");
                    }
                    mailbox.clearStaleCaches(lockContext);
                } else if (!lastWriter.equals(lockId)) {
                    if (write) {
                        if (ZimbraLog.cache.isDebugEnabled()) {
                            ZimbraLog.cache.debug("last write lock was acquired by different mailbox worker (%s), flushing local caches prior to write lock", lastWriter);
                        }
                        mailbox.clearStaleCaches(lockContext);
                    } else if (!LC.only_flush_cache_on_first_read_after_foreign_write.booleanValue() || response.isFirstReadSinceLastWrite()) {
                        if (ZimbraLog.cache.isDebugEnabled()) {
                            ZimbraLog.cache.debug("last write lock was acquired by different mailbox worker (%s), flushing local caches prior to read lock", lastWriter);
                        }
                        mailbox.clearStaleCaches(lockContext);
                    } else {
                        if (ZimbraLog.cache.isDebugEnabled()) {
                            ZimbraLog.cache.debug("last write lock was acquired by different mailbox worker (%s), but this worker has acquired a read lock since then", lastWriter);
                        }
                    }
                }
            } else {
                super.lock(lockContext);
            }
        }

        @Override
        protected boolean releaseReadLocksBeforeWriteLock() {
            boolean releaseRedisReadLock = super.releaseReadLocksBeforeWriteLock();
            if (releaseRedisReadLock) {
                if (log.isTraceEnabled()) {
                    log.trace("unlocking redis read lock before acquiring write lock");
                }
                redisLock.readLock().unlock();
            }
            return releaseRedisReadLock;
        }

        private LockResponse acquireRedisLock() throws ServiceException {
            try {
                return zRedisLock.lock();
            } catch (RedisException e) {
                throw ServiceException.LOCK_FAILED("failed to acquire redis lock", e);
            }
        }

        @Override
        public synchronized void close() {
            boolean reinstateRedisReadLock = closeCommon();
            if (getHoldCount() == 0) {
                if (log.isTraceEnabled()) {
                    log.trace("releasing redis lock in close() for %s", this);
                }
                try {
                    zRedisLock.unlock();
                    if (reinstateRedisReadLock) {
                        try {
                            if (log.isTraceEnabled()) {
                                log.trace("reinstating redis read lock in close() for %s", this);
                            }
                            redisLock.readLock().lock(true);
                        } catch (RedisException | ServiceException e) {
                            log.warn("unable to acquire redis read lock when reinstating read locks", e);
                        }
                    }
                } catch (RedisException e) {
                    log.error("failed to release redis lock in close() for %s", this, e);
                } catch (IllegalStateException e) {
                    log.warn("trying to release redis lock but it is not held by this thread", e);
                }
            }
        }
    }
}
