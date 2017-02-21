/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Extension of ZLock for debugging; enabled via DebugConfig.debugMailboxLock (debug_mailbox_lock in localconfig)
 * Used to track read lock owners since they are not tracked by base ReentrantReadWriteLock class
 */
public class DebugZLock extends ZLock {

    private static final long serialVersionUID = -3009063384967180207L;

    private final DebugReentrantReadLock readLock;
    private final DebugReentrantWriteLock writeLock;

    public DebugZLock() {
        super();
        this.readLock = new DebugReentrantReadLock(this);
        this.writeLock = new DebugReentrantWriteLock(this);
    }

    @Override
    public ReadLock readLock() {
      return readLock;
    }

    @Override
    public WriteLock writeLock() {
      return writeLock;
    }

    private class DebugReentrantReadLock extends ReentrantReadWriteLock.ReadLock {
        //wrapper around base ReadLock which tracks lock ownership

        private static final long serialVersionUID = -2861690755318306713L;

        private final ConcurrentMap<Long,MailboxLockOwner> debugReadOwners = new ConcurrentHashMap<Long, MailboxLockOwner>();

        DebugReentrantReadLock(DebugZLock readWriteLock) {
            super(readWriteLock);
        }

        void recordOwner() {
            Long key = Thread.currentThread().getId();
            MailboxLockOwner owner = debugReadOwners.get(key);
            if (owner == null) {
                owner = new MailboxLockOwner();
                debugReadOwners.put(key, owner);
            }
            owner.increment();
        }

        void removeOwner() {
            Long key = Thread.currentThread().getId();
            MailboxLockOwner owner = debugReadOwners.get(key);
            if (owner != null) {
                int count = owner.decrement();
                if (count <= 0) {
                    debugReadOwners.remove(key);
                }
            }
        }

        @Override
        public void lock() {
            super.lock();
            recordOwner();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            super.lockInterruptibly();
            recordOwner();
        }

        @Override
        public boolean tryLock() {
            if (super.tryLock()) {
                recordOwner();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            if (super.tryLock(timeout, unit)) {
                recordOwner();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void unlock() {
            super.unlock();
            removeOwner();
        }
    }

    private class DebugReentrantWriteLock extends ReentrantReadWriteLock.WriteLock {
        //just a wrapper around constructor for now; could add more debug capabilities later

        private static final long serialVersionUID = -7969337036084655493L;

        DebugReentrantWriteLock(DebugZLock readWriteLock) {
            super(readWriteLock);
        }
    }

    private ConcurrentMap<Long, MailboxLockOwner> getReadOwners() {
        return readLock.debugReadOwners;
    }

    @Override
    public void printStackTrace(StringBuilder out) {
        Thread owner = getOwner();
        if (owner != null) {
            out.append("Write Lock Owner - ");
            printStackTrace(owner, out);
        }
        int readCount = getReadLockCount();
        if (readCount > 0) {
            out.append("Reader Count - " + readCount + "\n");
        }
        if (getReadOwners() != null) {
            for(MailboxLockOwner lockOwner : getReadOwners().values()) {
                out.append("Read Lock Owner (Holds: " + lockOwner.getCount() + ") - ");
                printStackTrace(lockOwner.getOwnerThread(), out);
            }
        }
        for (Thread waiter : getQueuedThreads()) {
            out.append("Lock Waiter - ");
            printStackTrace(waiter, out);
        }
    }
}
