package com.zimbra.client;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Monitor;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Locking mechanism with logic similar to that of MailboxLock.
 * It is simpler than MailboxLock, since it does not differentiate between read and write locks.
 *
 * @author iraykin
 *
 */
public class ZLocalMailboxLock implements MailboxLock {
    private Monitor monitor;
    private Integer maxWaitingThreads;
    private Integer timeoutSeconds;

    public ZLocalMailboxLock(Integer maxWaitingThreads, Integer timeoutSeconds) {
        this.maxWaitingThreads = maxWaitingThreads;
        this.timeoutSeconds = timeoutSeconds;
        monitor = new Monitor();
    }

    @Override
    public boolean isWriteLock() {
        return true;  /* it is both a read and a write lock */
    }

    @Override
    public boolean isWriteLockedByCurrentThread() {
        return monitor.isOccupied();
    }

    @Override
    public boolean isReadLockedByCurrentThread() {
        return monitor.isOccupied();
    }

    @Override
    public boolean isLockedByCurrentThread() {
        return isWriteLockedByCurrentThread() || isReadLockedByCurrentThread();
    }

    @Override
    public void lock() throws ServiceException {
        try {
            //First, try to enter the monitor if it's not occupied.
            //We do not wait here, since we don't want blocked threads piling up.
            if (monitor.tryEnter()) {
                ZimbraLog.mailboxlock.debug("acquired zmailbox lock without waiting");
                return;
            }
            //If too many threads are waiting on the lock, throw an exception.
            int queueLength = monitor.getQueueLength();
            if (queueLength >= maxWaitingThreads) {
                throw ServiceException.LOCK_FAILED("too many waiters: " + queueLength);
            }
            //Wait for the lock up to the allowed limit
            if (monitor.enterInterruptibly(timeoutSeconds, TimeUnit.SECONDS)) {
                ZimbraLog.mailboxlock.debug("acquired zmailbox lock");
                return;
            } else {
                throw ServiceException.LOCK_FAILED("lock timeout");
            }
        } catch (InterruptedException e) {
            throw ServiceException.LOCK_FAILED("lock interrupted");
        }
    }

    @Override
    public void close() {
        if (monitor.isOccupiedByCurrentThread()) {
            ZimbraLog.mailboxlock.debug("releasing zmailbox lock");
            monitor.leave();
        } else {
            ZimbraLog.mailboxlock.debug("close() called but not holding a zmailbox lock");
        }
    }

    /**
     * Number of holds on this lock by the current thread
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    @Override
    public int getHoldCount() {
        return monitor.getOccupiedDepth();
    }
}
