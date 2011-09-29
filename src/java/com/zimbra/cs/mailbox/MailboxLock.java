/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

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
    private final Lock lock = new Lock();

    /**
     * Acquires the lock.
     *
     * @throws LockFailedException failed to lock
     */
    public void lock() {
        if (lock.tryLock()) { // This succeeds in most cases.
            return;
        }
        int queueLength = lock.getQueueLength();
        if (queueLength >= LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
            // Too many threads are already waiting for the lock, can't let you queued. We don't want to log stack trace
            // here because once requests back up, each new incoming request falls into here, which creates too much
            // noise in the logs.
            throw new LockFailedException("too many waiters: " + queueLength);
        }
        try {
            // Wait for the lock up to the timeout.
            if (lock.tryLock(LC.zimbra_mailbox_lock_timeout.intValue(), TimeUnit.SECONDS)) {
                return;
            }

            LockFailedException e = new LockFailedException("timeout");
            e.logStackTrace();
            throw e;
        } catch (InterruptedException e) {
            throw new LockFailedException("interrupted", e);
        }
    }

    public boolean isLocked() {
        return lock.isHeldByCurrentThread();
    }

    public void release() {
        if (isLocked()) {
            lock.unlock();
        }
    }

    /**
     * Extend {@link ReentrantLock} to access protected methods.
     */
    private static final class Lock extends ReentrantLock {
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
            lock.printStackTrace(out);
            ZimbraLog.mailbox.error(out, this);
        }
    }

}
