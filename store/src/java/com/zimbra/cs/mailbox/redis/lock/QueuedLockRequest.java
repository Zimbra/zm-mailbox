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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.redis.lock.RedisLock.LockResponse;
import com.zimbra.cs.mailbox.redis.lock.RedisLockChannel.LockTimingContext;

public class QueuedLockRequest {

    private RedisLock lock;
    private QueuedLockRequest.LockCallback callback;
    private Semaphore semaphore;
    private LockTimingContext timingContext;
    private boolean tryAcquireNow = false;

    public QueuedLockRequest(RedisLock lock, QueuedLockRequest.LockCallback callback) {
        this.lock = lock;
        this.callback = callback;
        this.semaphore = new Semaphore(0, true);
        this.timingContext = new LockTimingContext();
    }

    private void lockFailed() throws ServiceException {
        ZimbraLog.mailboxlock.warn("failed to acquire %s: %s", lock, timingContext);
        throw ServiceException.LOCK_FAILED("unable to acquire zimbra redis lock; timeout reached");
    }

    public LockResponse waitForUnlock(long timeoutMillis) throws ServiceException {
        timingContext.setTimeout(timeoutMillis);
        while (true) {
            long remainingMillis = timingContext.getRemainingTime();
            if (remainingMillis < 0) {
                lockFailed();
            }
            try {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("%s will wait for %s ms", this, remainingMillis);
                }
                timingContext.attempts++;
                if (semaphore.tryAcquire(remainingMillis, TimeUnit.MILLISECONDS)) {
                    if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                        ZimbraLog.mailboxlock.trace("acquired semaphore for %s", lock);
                    }
                    LockResponse resp = callback.attemptLock(this, timingContext);
                    if (resp.success()) {
                        return resp;
                    } else {
                        //keep trying
                        continue;
                    }
                } else {
                    lockFailed();
                }
            } catch (InterruptedException e) {
                ZimbraLog.mailboxlock.warn("interrupted while waiting for %s to be released!", lock.getLockName());
                throw ServiceException.LOCK_FAILED("failed to acquire redis lock", e);
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