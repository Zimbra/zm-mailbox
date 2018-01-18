package com.zimbra.cs.mailbox;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.LockFailedException;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.lock.DebugZLock;
import com.zimbra.cs.mailbox.lock.ZLock;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zookeeper.CuratorManager;


public class LocalMailboxLockFactory implements MailboxLockFactory {
    private final ZLock zLock = DebugConfig.debugMailboxLock ? new DebugZLock() : new ZLock();
    private InterProcessSemaphoreMutex dLock = null;
    private final Stack<Boolean> lockStack = new Stack<>();
    private final Mailbox mailbox;
    //for sanity checking, we keep list of read locks. the first time caller obtains write lock they must not already own read lock
    //states - no lock, read lock only, write lock only
    private final ThreadLocal<Boolean> assertReadLocks = new ThreadLocal<>();

    public LocalMailboxLockFactory(final Mailbox mailbox) {
        this.mailbox = mailbox;

        if (Zimbra.isAlwaysOn()) {
            try {
                dLock = CuratorManager.getInstance().createLock(this.mailbox.getAccountId());
            } catch (ServiceException se) {
                ZimbraLog.mailbox.error("could not initialize distributed lock", se);
            }
        }
    }

    @Override
    public MailboxLock readLock() {
        return new LocalMailboxLock(false);
    }

    @Override
    public MailboxLock writeLock() {
        return new LocalMailboxLock(true);
    }

    @Override
    @Deprecated
    public MailboxLock lock(boolean write) {
        if (write || this.mailbox.requiresWriteLock()) {
            return writeLock();
        }
        return readLock();
    }

    @Override
    public void close() throws Exception {
        // noop
    }

    @VisibleForTesting
    int getQueueLength() {
        return zLock.getQueueLength();
    }

    /**
     * {@link MailboxLock} is a replacement of the implicit monitor lock using {@code synchronized} methods or statements on
     * a mailbox instance. This gives extended capabilities such as timeout and limit on number of threads waiting for a
     * particular mailbox lock. It is no longer legal to synchronize on a mailbox, otherwise an assertion error will be
     * thrown. {@code Mailbox.beginTransaction()}) internally acquires the mailbox lock and it's released by
     * {@code Mailbox.endTransaction()}, so that you don't have to explicitly call {@link #lock()} and {@link #close()}
     * wrapping a mailbox transaction.
     */
    public class LocalMailboxLock implements MailboxLock {
        private boolean write;

        LocalMailboxLock(final boolean write) {
            this.write = write;
        }

        private void acquireDistributedLock() throws ServiceException {
            //TODO: consider read/write distributed lock
            if (dLock != null && getHoldCount() == 1) {
                try {
                    dLock.acquire(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new LockFailedException("could not acquire distributed lock", e);
                }
            }
        }

        private void releaseDistributedLock() {
            //TODO: consider read/write distributed lock
            if (dLock != null && getHoldCount() == 1) {
                try {
                    dLock.release();
                } catch (Exception e) {
                    ZimbraLog.mailbox.warn("error while releasing distributed lock", e);
                }
            }
        }

        @Override
        public int getHoldCount() {
            return zLock.getReadHoldCount() + zLock.getWriteHoldCount();
        }

        @Override
        public boolean isWriteLock() {
			return this.write;
        }

        @Override
        public boolean isWriteLockedByCurrentThread() {
            return zLock.isWriteLockedByCurrentThread();
        }

        @Override
        public boolean isUnlocked() {
            return !isWriteLockedByCurrentThread() && zLock.getReadHoldCount() == 0;
        }

        private synchronized boolean neverReadBeforeWrite() {
            if (zLock.getWriteHoldCount() == 0) {
                if (write) {
                    Boolean readLock = assertReadLocks.get();
                    if (readLock != null) {
                        final LockFailedException lfe = new LockFailedException("read lock held before write");
                        ZimbraLog.mailbox.error("read lock held before write", lfe);
                        throw lfe;
                    }
                } else {
                    assertReadLocks.set(true);
                }
            }
            return true;
        }

        private synchronized boolean debugReleaseReadLock() {
            //remove read lock
            if (zLock.getReadHoldCount() == 0) {
                assertReadLocks.remove();
            }
            return true;
        }

        @VisibleForTesting
        boolean hasQueuedThreads() {
            return zLock.hasQueuedThreads();
        }

        /**
         * Acquires the lock.
         *
         * @throws LockFailedException failed to lock
         */
        @Override
        public void lock() {
            write = write || mailbox.requiresWriteLock();
            ZimbraLog.mailbox.trace("LOCK %s", (write ? "WRITE" : "READ"));
            assert(neverReadBeforeWrite());
            try {
                if (tryLock()) {
                    if (mailbox.requiresWriteLock() && !isWriteLockedByCurrentThread()) {
                        //writer finished a purge while we waited
                        promote();
                        return;
                    }
                    lockStack.push(write);
                    try {
                        acquireDistributedLock();
                    } catch (ServiceException e) {
                        close();
                        LockFailedException lfe = new LockFailedException("lockdb");
                        logLockFailedException(lfe);
                        throw lfe;
                    }
                    return;
                }
                int queueLength = zLock.getQueueLength();
                if (queueLength >= LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
                    // Too many threads are already waiting for the lock, can't let you queued. We don't want to log stack trace
                    // here because once requests back up, each new incoming request falls into here, which creates too much
                    // noise in the logs. Unless debug switch is enabled
                    LockFailedException e = new LockFailedException("too many waiters: " + queueLength);
                    if (DebugConfig.debugMailboxLock) {
                        logLockFailedException(e);
                    }
                    throw e;
                }
                // Wait for the lock up to the timeout.
                if (tryLockWithTimeout()) {
                    if (mailbox.requiresWriteLock() && !isWriteLockedByCurrentThread()) {
                        //writer finished a purge while we waited
                        promote();
                        return;
                    }
                    lockStack.push(write);
                    try {
                        acquireDistributedLock();
                    } catch (ServiceException e) {
                        close();
                        LockFailedException lfe = new LockFailedException("lockdb");
                        logLockFailedException(lfe);
                        throw lfe;
                    }
                    return;
                }
                LockFailedException e = new LockFailedException("timeout");
                logLockFailedException(e);
                throw e;
            } catch (InterruptedException e) {
                throw new LockFailedException("interrupted", e);
            } finally {
                assert(!isUnlocked() || debugReleaseReadLock());
            }
        }

        private void logLockFailedException(final LockFailedException ex) {
            final StringBuilder out = new StringBuilder("Failed to lock mailbox\n");
            zLock.printStackTrace(out);
            ZimbraLog.mailbox.error(out, ex);
        }

        private boolean tryLock() throws InterruptedException {
            if (write) {
                return zLock.writeLock().tryLock(0, TimeUnit.SECONDS);
            } else {
                return zLock.readLock().tryLock(0, TimeUnit.SECONDS);
            }
        }

        private boolean tryLockWithTimeout() throws InterruptedException {
            if (write) {
                return zLock.writeLock().tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
            } else {
                return zLock.readLock().tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
            }
        }

        @Override
        public void close() {
            if(isUnlocked()){
                return;
            }
            try {
                write = lockStack.pop();
            } catch (EmptyStackException ese) {
                //should only occur if locked failed; i.e. tryLock() returned error
                //or if call site has unbalanced lock/release
                ZimbraLog.mailbox.trace("release when not locked?");
                assert(getHoldCount() == 0);
                assert(debugReleaseReadLock());
                return;
            }
            //keep release in order so caller doesn't have to manage write/read flag
            ZimbraLog.mailbox.trace("RELEASE %s", (write ? "WRITE" : "READ"));

            releaseDistributedLock();
            if (write) {
                assert(zLock.getWriteHoldCount() > 0);
                zLock.writeLock().unlock();
            } else {
                zLock.readLock().unlock();
                assert(debugReleaseReadLock());
            }
        }

        private void promote() {
            assert(getHoldCount() == zLock.getReadHoldCount());
            int count = zLock.getReadHoldCount();
            for (int i = 0; i < count - 1; i++) {
                close();
            }
            zLock.readLock().unlock();
            assert(debugReleaseReadLock());
            for (int i = 0; i < count; i++) {
                write = true;
                lock();
            }
        }
    }

}
