/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zookeeper.CuratorManager;

/**
 * {@link MailboxLock} is a replacement of the implicit monitor lock using {@code synchronized} methods or statements on
 * a mailbox instance. This gives extended capabilities such as timeout and limit on number of threads waiting for a
 * particular mailbox lock. It is no longer legal to synchronize on a mailbox, otherwise an assertion error will be
 * thrown. {@code Mailbox.beginTransaction()}) internally acquires the mailbox lock and it's released by
 * {@code Mailbox.endTransaction()}, so that you don't have to explicitly call {@link #lock()} and {@link #release()}
 * wrapping a mailbox transaction.
 *
 * @author ysasaki
 */
public final class MailboxLock {
    private final ZLock zLock = new ZLock();
    private InterProcessSemaphoreMutex dLock = null;
    private final Stack<Boolean> lockStack = new Stack<Boolean>();
    private Mailbox mbox;

    public MailboxLock(String id, Mailbox mbox) {
        if (Zimbra.isAlwaysOn()) {
            try {
                dLock = CuratorManager.getInstance().createLock(id);
            } catch (ServiceException se) {
                ZimbraLog.mailbox.error("could not initialize distributed lock", se);
            }
        }
        this.mbox = mbox;
    }

    private void acquireDistributedLock(boolean write) throws ServiceException {
        //TODO: consider read/write distributed lock
        if (dLock != null && getHoldCount() == 1) {
            try {
                dLock.acquire(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new LockFailedException("could not acquire distributed lock", e);
            }
        }
    }

    private void releaseDistributedLock(boolean write) {
        //TODO: consider read/write distributed lock
        if (dLock != null && getHoldCount() == 1) {
            try {
                dLock.release();
            } catch (Exception e) {
                ZimbraLog.mailbox.warn("error while releasing distributed lock", e);
            }
        }
    }

    int getHoldCount() {
        return zLock.getReadHoldCount() + zLock.getWriteHoldCount();
    }

    public boolean isWriteLockedByCurrentThread() {
        return zLock.isWriteLockedByCurrentThread();
    }

    public boolean isUnlocked() {
        return !isWriteLockedByCurrentThread() && zLock.getReadHoldCount() == 0;
    }

    /**
     * Acquires the lock.
     *
     * @throws LockFailedException failed to lock
     */
    public void lock() {
        lock(true);
    }

    private boolean tryLock(boolean write) {
        if (write) {
            return zLock.writeLock().tryLock();
        } else {
            return zLock.readLock().tryLock();
        }
    }

    private boolean tryLockWithTimeout(boolean write) throws InterruptedException {
        if (write) {
            return zLock.writeLock().tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
        } else {
            return zLock.readLock().tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS);
        }
    }

    private ThreadLocal<Boolean> assertReadLocks = null;

    private synchronized boolean neverReadBeforeWrite(boolean write) {
        //for sanity checking, we keep list of read locks. the first time caller obtains write lock they must not already own read lock
        //states - no lock, read lock only, write lock only
        if (assertReadLocks == null) {
            assertReadLocks = new ThreadLocal<Boolean>();
        }
        if (zLock.getWriteHoldCount() == 0) {
            if (write) {
                Boolean readLock = assertReadLocks.get();
                if (readLock != null) {
                    ZimbraLog.mailbox.error("read lock held before write", new Exception());
                    assert(false);
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
    int getQueueLength() {
        return zLock.getQueueLength();
    }

    @VisibleForTesting
    boolean hasQueuedThreads() {
        return zLock.hasQueuedThreads();
    }

    public void lock(boolean write) {
        write = write || mbox.requiresWriteLock();
        ZimbraLog.mailbox.trace("LOCK " + (write ? "WRITE" : "READ"));
        assert(neverReadBeforeWrite(write));
        try {
            if (tryLock(write)) {
                if (mbox.requiresWriteLock() && !isWriteLockedByCurrentThread()) {
                    //writer finished a purge while we waited
                    promote();
                    return;
                }
                lockStack.push(write);
                try {
                    acquireDistributedLock(write);
                } catch (ServiceException e) {
                    release();
                    LockFailedException lfe = new LockFailedException("lockdb");
                    lfe.logStackTrace();
                    throw lfe;
                }
                return;
            }
            int queueLength = zLock.getQueueLength();
            if (queueLength >= LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
                // Too many threads are already waiting for the lock, can't let you queued. We don't want to log stack trace
                // here because once requests back up, each new incoming request falls into here, which creates too much
                // noise in the logs.
                throw new LockFailedException("too many waiters: " + queueLength);
            }
            try {
                // Wait for the lock up to the timeout.
                if (tryLockWithTimeout(write)) {
                    if (mbox.requiresWriteLock() && !isWriteLockedByCurrentThread()) {
                        //writer finished a purge while we waited
                        promote();
                        return;
                    }
                    lockStack.push(write);
                    try {
                        acquireDistributedLock(write);
                    } catch (ServiceException e) {
                        release();
                        LockFailedException lfe = new LockFailedException("lockdb");
                        lfe.logStackTrace();
                        throw lfe;
                    }
                    return;
                }

                LockFailedException e = new LockFailedException("timeout");
                e.logStackTrace();
                throw e;
            } catch (InterruptedException e) {
                throw new LockFailedException("interrupted", e);
            }
        } finally {
            assert(!isUnlocked() || debugReleaseReadLock());
        }
    }

    public void release() {
        Boolean write = false;
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
        ZimbraLog.mailbox.trace("RELEASE " + (write ? "WRITE" : "READ"));

        releaseDistributedLock(write);
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
            release();
        }
        zLock.readLock().unlock();
        assert(debugReleaseReadLock());
        for (int i = 0; i < count; i++) {
            lock(true);
        }
    }

    /**
     * Extend {@link ReentrantLock} to access protected methods.
     */
    private static final class ZLock extends ReentrantReadWriteLock {
        private static final long serialVersionUID = -3009063384967180207L;

        void printStackTrace(StringBuilder out) {
            Thread owner = getOwner();
            if (owner != null) {
                out.append("Lock Owner - ");
                printStackTrace(owner, out);
            }
            for (Thread waiter : getQueuedThreads()) {
                out.append("Lock Waiter - ");
                printStackTrace(waiter, out);
            }
        }

        private void printStackTrace(Thread thread, StringBuilder out) {
            out.append(thread.getName());
            if (thread.isDaemon()) {
                out.append(" daemon");
            }
            out.append(" prio=").append(thread.getPriority());
            out.append(" id=").append(thread.getId());
            out.append(" state=").append(thread.getState());
            out.append('\n');
            for (StackTraceElement el : thread.getStackTrace()) {
                out.append("\tat ").append(el.toString()).append('\n');
            }
        }
    }

    public final class LockFailedException extends RuntimeException {
        private static final long serialVersionUID = -6899718561860023270L;

        private LockFailedException(String message) {
            super(message);
        }

        private LockFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        private void logStackTrace() {
            StringBuilder out = new StringBuilder("Failed to lock mailbox\n");
            zLock.printStackTrace(out);
            ZimbraLog.mailbox.error(out, this);
        }
    }

}
