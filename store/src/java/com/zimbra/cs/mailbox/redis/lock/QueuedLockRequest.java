/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.cs.mailbox.redis.lock;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.base.MoreObjects;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.redis.lock.RedisLock.LockResponse;
import com.zimbra.cs.mailbox.redis.lock.RedisLockChannel.LockQueue;
import com.zimbra.cs.mailbox.redis.lock.RedisLockChannel.LockTimingContext;

public class QueuedLockRequest {

    private RedisLock lock;
    private QueuedLockRequest.LockCallback callback;
    private Semaphore semaphore;
    private LockTimingContext timingContext;
    private boolean tryAcquireNow = false;
    private List<String> holders= null;
    private LockQueue lockQueue;
    private volatile boolean resetWait = false;

    public QueuedLockRequest(RedisLock lock, QueuedLockRequest.LockCallback callback, LockQueue queue) {
        this.lock = lock;
        this.callback = callback;
        this.semaphore = new Semaphore(0, true);
        this.timingContext = new LockTimingContext();
        this.lockQueue = queue;
    }

    private void lockFailed() throws ServiceException {
        ZimbraLog.mailboxlock.warn("failed to acquire %s: holders=%s, %s", lock, holders, timingContext);
        throw ServiceException.LOCK_FAILED("unable to acquire redis lock; timeout reached");
    }

    public LockResponse waitForUnlock(long timeoutMillis) throws ServiceException {
        timingContext.setTimeout(timeoutMillis);
        while (true) {
            long remainingMillis = timingContext.getRemainingTime();
            if (remainingMillis < 0) {
                lockFailed();
            }
            try {
                if (ZimbraLog.mailboxlock.isDebugEnabled()) {
                    ZimbraLog.mailboxlock.debug("%s will wait for %s ms (holders=%s)", this, remainingMillis, holders);
                }
                timingContext.attempts++;
                if (semaphore.tryAcquire(remainingMillis, TimeUnit.MILLISECONDS)) {
                    if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                        ZimbraLog.mailboxlock.trace("acquired semaphore for %s", lock);
                    }
                    if (resetWait) {
                        ZimbraLog.mailboxlock.debug("waiting for %s has been reset", lock);
                        resetWait = false;
                        continue;
                    }
                    LockResponse resp = callback.attemptLock(this, timingContext);
                    if (resp.success()) {
                        return resp;
                    } else {
                        //keep trying
                        continue;
                    }
                } else {
                    //try again before failing, just in case something went wrong and the unlock message wasn't delivered
                    LockResponse resp = callback.attemptLock(this, timingContext);
                    if (resp.success()) {
                        ZimbraLog.mailboxlock.debug("acquired %s on timeout", lock);
                        notifyOrResetOtherWaiters();
                        return resp;
                    } else {
                        lockFailed();
                    }
                }
            } catch (InterruptedException e) {
                ZimbraLog.mailboxlock.warn("interrupted while waiting for %s to be released!", lock.getLockName());
                throw ServiceException.LOCK_FAILED("failed to acquire redis lock", e);
            }
        }
    }

    private void notifyOrResetOtherWaiters() {
        //notify the locks that would have been notified if the unlock message had been received, otherwise reset their waits.
        //only do this if we are the first lock in line!
        synchronized (lockQueue) {
            Iterator<QueuedLockRequest> iter = lockQueue.getIterator();
            if (!iter.hasNext()) {
                //really shouldn't happen
                ZimbraLog.mailboxlock.warn("notifyOrResetOtherWaiters: queue is empty!");
                return;
            }
            QueuedLockRequest firstWaiter = iter.next();
            if (firstWaiter == this) {
                ZimbraLog.mailboxlock.debug("notifyOrResetOtherWaiters: %s", lockQueue);
                if (firstWaiter.isWriteLock()) {
                    //extend timeouts for of all waiters
                    while(iter.hasNext()) {
                        QueuedLockRequest waitingLock = iter.next();
                        ZimbraLog.mailboxlock.debug("notifyOrResetOtherWaiters: resetting %s", waitingLock);
                        waitingLock.resetTimeout();
                    }
                } else {
                    //notify all waiting readers up to the first writer, extend timeouts of the rest
                    boolean notify = true;
                    while(iter.hasNext()) {
                        QueuedLockRequest waitingLock = iter.next();
                        if (!waitingLock.isWriteLock() && notify) {
                            ZimbraLog.mailboxlock.debug("notifyOrResetOtherWaiters: notifying %s", waitingLock);
                            waitingLock.notifyUnlock();
                        } else {
                            ZimbraLog.mailboxlock.debug("notifyOrResetOtherWaiters: resetting %s", waitingLock);
                            waitingLock.resetTimeout();
                            notify = false; //don't notify future read locks
                        }
                    }
                }
            }
        }
    }
    public void notifyUnlock() {
        semaphore.release();
    }

    public boolean isWriteLock() {
        return lock instanceof RedisWriteLock;
    }

    public String getUuid() {
        return lock.getUuid();
    }

    public String getAccountId() {
        return lock.getAccountId();
    }

    public void setTryAcquireNow(boolean bool) {
        tryAcquireNow = bool;
    }

    public boolean canTryAcquireNow() {
        return tryAcquireNow;
    }

    public void setHolders(List<String> holders) {
        this.holders = holders;
    }

    public void resetTimeout() {
        this.timingContext.increaseTimeout(LC.zimbra_mailbox_lock_timeout.intValue() * 1000);
        ZimbraLog.mailboxlock.debug("%s timeout increased to %s", this, timingContext.getRemainingTime());
        resetWait = true;
        semaphore.release();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", lock.getUuid())
                .add("accountId", lock.getAccountId())
                .add("type",  isWriteLock() ? "write" : "read")
                .toString();
    }


    @FunctionalInterface
    static interface LockCallback {
        public LockResponse attemptLock(QueuedLockRequest waitingLock, LockTimingContext context) throws ServiceException;
    }

}