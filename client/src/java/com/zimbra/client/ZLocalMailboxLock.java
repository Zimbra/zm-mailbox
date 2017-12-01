package com.zimbra.client;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Monitor;
import com.zimbra.common.mailbox.MailboxLock;
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
        return false;
    }

    @Override
    public boolean isWriteLockedByCurrentThread() {
        return false;
    }

    @Override
    public boolean isUnlocked() {
        return !monitor.isOccupied();
    }

    @Override
    public void lock() {
        try {
            //First, try to enter the monitor if it's not occupied.
            //We do not wait here, since we don't want blocked threads piling up.
            if (monitor.tryEnter()) {
                ZimbraLog.mailbox.debug("acquired zmailbox lock without waiting");
                return;
            }
            //If too many threads are waiting on the lock, throw an exception.
            int queueLength = monitor.getQueueLength();
            if (queueLength >= maxWaitingThreads) {
                throw new LockFailedException("too many waiters: " + queueLength);
            }
            //Wait for the lock up to the allowed limit
            if (monitor.enterInterruptibly(timeoutSeconds, TimeUnit.SECONDS)) {
                ZimbraLog.mailbox.debug("acquired zmailbox lock");
                return;
            } else {
                throw new LockFailedException("lock timeout");
            }
        } catch (InterruptedException e) {
            throw new LockFailedException("lock interrupted", e);
        }
    }

    @Override
    public void close() {
        if (monitor.isOccupiedByCurrentThread()) {
            monitor.leave();
        }
    }

    @Override
    public int getHoldCount() {
        return monitor.getOccupiedDepth();
    }

    public final class LockFailedException extends RuntimeException {
        private static final long serialVersionUID = -6899718561860023270L;

        private LockFailedException(String message) {
            super(message);
        }

        private LockFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
