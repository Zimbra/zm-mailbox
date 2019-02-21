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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.redis.RedisUtils.RedisKey;
import com.zimbra.cs.mailbox.redis.lock.RedisLock.LockResponse;


public class RedisLockChannel implements MessageListener<String> {

    private Map<String, LockQueue> waitingLocksQueues = new HashMap<>();
    private boolean isActive = false;
    private RedisKey channelName;
    private RTopic topic;

    public RedisLockChannel(RedisKey channelName) {
        this.channelName = channelName;
        this.topic = RedissonClientHolder.getInstance().getRedissonClient().getTopic(channelName.getKey(), StringCodec.INSTANCE);
    }

    public RedisKey getChannelName() {
        return channelName;
    }

    private void subscribe() {
        ZimbraLog.mailboxlock.info("beginning listening on channel %s", channelName);
        topic.addListener(String.class, this);
    }

    private LockQueue getQueue(String accountId) {
        return waitingLocksQueues.computeIfAbsent(accountId, k -> new LockQueue(accountId));
    }

    public synchronized QueuedLockRequest add(RedisLock lock, QueuedLockRequest.LockCallback callback) {
        if (!isActive && waitingLocksQueues.isEmpty()) {
            isActive = true; //lazily activate the channel
            subscribe();
        }
        QueuedLockRequest waitingLock = new QueuedLockRequest(lock, callback);
        boolean tryAcquireNow = getQueue(lock.getAccountId()).add(waitingLock);
        waitingLock.setTryAcquireNow(tryAcquireNow);
        return waitingLock;
    }

    public void remove(QueuedLockRequest waitingLock) {
        getQueue(waitingLock.getAccountId()).remove(waitingLock);
    }

    public LockResponse waitForUnlock(QueuedLockRequest waitingLock, long timeoutMillis) throws ServiceException {
        try {
            return waitingLock.waitForUnlock(timeoutMillis);
        } finally {
            remove(waitingLock);
        }
    }

    @Override
    public void onMessage(CharSequence channel, String accountId) {
        List<String> notifiedLockUuids = getQueue(accountId).notifyWaitingLocks();
        if (notifiedLockUuids.size() > 0) {
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.debug("notified %s waiting locks for account %s: %s", notifiedLockUuids.size(), accountId, Joiner.on(", ").join(notifiedLockUuids));
            } else {
                ZimbraLog.mailboxlock.debug("notified %s waiting locks for account %s", notifiedLockUuids.size(), accountId);
            }
        }
    }

    private class LockQueue {

        private String accountId;
        private LinkedList<QueuedLockRequest> queue;
        private int numWriters = 0;

        public LockQueue(String accountId) {
            this.accountId = accountId;
            this.queue = new LinkedList<>();
            ZimbraLog.mailboxlock.info("initializing lock queue for account %s on channel %s", accountId, RedisLockChannel.this.channelName);
        }

        /**
         * returns TRUE if the thread should try to acquire the lock in redis; FALSE if it needs to wait.
         * Specifically:
         *  - if acquiring a read lock, a thread should only wait for unlock if there is a writer ahead of it in the queue.
         *  - if acquiring a write lock, a thread should wait for unlock if there is anything ahead of it in the queue
         */
        public boolean add(QueuedLockRequest lock) {
            synchronized(this) {
                if (lock.isWriteLock()) {
                    numWriters++;
                }
                int curSize = queue.size();
                queue.add(lock);
                if (curSize == 0) {
                    trace("adding %s to queue (no locks queued)", lock);
                    return true;
                } else if (lock.isWriteLock()) {
                    trace("adding %s to queue: %s", lock, this);
                    return false;
                } else {
                    trace("adding %s to queue (%d writers): %s", lock, numWriters, this);
                    return numWriters == 0;
                }
            }
        }

        public boolean remove(QueuedLockRequest lock) {
            synchronized(this) {
                boolean removed = queue.remove(lock);
                if (removed && lock.isWriteLock()) {
                    numWriters--;
                }
                return removed;
            }
        }

        public List<String> notifyWaitingLocks() {
            synchronized(this) {
                List<String> notifiedLockUuids = new ArrayList<>();
                QueuedLockRequest firstLock = queue.peek();
                if (firstLock != null && firstLock.isWriteLock()) {
                    trace("notifying write lock at head of %s", this);
                    firstLock.notifyUnlock();
                    notifiedLockUuids.add(firstLock.getUuid());
                } else if (firstLock != null) {
                    trace("notifying read locks in %s", this);
                    Iterator<QueuedLockRequest> iter = queue.iterator();
                    while (iter.hasNext()) {
                        QueuedLockRequest waitingLock = iter.next();
                        if (waitingLock.isWriteLock()) {
                            break;
                        } else {
                            waitingLock.notifyUnlock();
                            notifiedLockUuids.add(waitingLock.getUuid());
                        }
                    }
                }
                return notifiedLockUuids;
            }
        }

        private void trace(String msg, Object... objects) {
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace(msg, objects);
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("accountId", accountId)
                    .add("size", queue.size())
                    .add("writers", numWriters)
                    .add("queue", queue.stream().map(l -> l.getUuid()).collect(Collectors.toList())).toString();
        }
    }

    public static class LockTimingContext {
        public int attempts;
        public long startTime;
        public long timeoutTime;

        public LockTimingContext() {
            attempts = 0;
            startTime = System.currentTimeMillis();
        }

        public void setTimeout(long timeoutMillis) {
            timeoutTime = startTime + timeoutMillis;
        }

        public long getRemainingTime() {
            return timeoutTime - System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("attempts", attempts)
                    .add("elapsed", System.currentTimeMillis() - startTime).toString();
        }
    }
}