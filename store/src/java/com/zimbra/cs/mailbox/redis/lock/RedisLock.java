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
import java.util.List;
import java.util.Random;

import org.redisson.client.codec.Codec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.decoder.MultiDecoder;
import org.redisson.command.CommandExecutor;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLockContext;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.redis.RedisUtils;
import com.zimbra.cs.mailbox.redis.RedissonRetryClient;

public abstract class RedisLock {

    private static final Random random = new Random();
    protected String lockName;
    protected RedisLockChannel lockChannel;
    protected String lockChannelName;
    protected String accountId;
    protected RedissonRetryClient client;
    protected MailboxLockContext lockContext;
    protected String uuid; //unique for each lock instance
    protected String lockNode; //unique for each top-level RedisReadWriteLock
    protected String hashtaggedNodeKey;

    protected static RedisCommand<LockResponse> LOCK_RESPONSE_CMD = new RedisCommand<>("EVAL", new LockResponseConvertor());

    public RedisLock(String accountId, String lockBaseName, String lockNode, MailboxLockContext lockContext) {
        this.lockChannel = RedisLockChannelManager.getInstance().getLockChannel(accountId);
        this.lockChannelName = lockChannel.getChannelName().getKey();
        this.lockName = RedisUtils.createAccountRoutedKey(lockChannel.getChannelName().getHashTag(), lockBaseName);
        this.accountId = accountId;
        this.client = (RedissonRetryClient) RedissonClientHolder.getInstance().getRedissonClient();
        this.lockContext = lockContext;
        this.uuid = generateUuid();
        this.lockNode = lockNode;
        this.hashtaggedNodeKey = RedisUtils.createAccountRoutedKey(lockChannel.getChannelName().getHashTag(), lockNode);
    }

    private String generateUuid() {
        String caller = lockContext.getCaller();
        byte[] buffer = new byte[4];
        random.nextBytes(buffer);
        String uuid = BaseEncoding.base16().omitPadding().encode(buffer);
        if (!Strings.isNullOrEmpty(caller)) {
            uuid = String.format("%s-[%s]", uuid, caller);
        }
        return uuid;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getLockName() {
        return lockName;
    }

    protected String getThreadLockName() {
        long threadId = Thread.currentThread().getId();
        return lockNode + ":" + threadId;
    }

    protected long getLeaseTime() {
        return LC.zimbra_mailbox_lock_read_lease_seconds.intValue() *1000;
    }

    private long getTimeout() {
        return LC.zimbra_mailbox_lock_timeout.intValue() * 1000;
    }

    protected String getLastAccessKey() {
        return lockName + ":" + "last_accessed";
    }

    /**
     * Try to acquire the lock in redis
     * @return LockResponse containing information about the lock acquisition
     */
    protected abstract LockResponse tryAcquire();

    /**
     * Release the lock in redis
     */
    protected abstract Boolean unlockInner();

    public LockResponse lock() throws ServiceException {
        return lock(false);
    }

    public LockResponse lock(boolean skipQueue) throws ServiceException {
        QueuedLockRequest waitingLock = lockChannel.add(this, (queuedLock, context) -> {
            LockResponse response;
            try {
                response = tryAcquire();
            } catch (Exception e) {
                throw ServiceException.LOCK_FAILED(String.format("exception encountered trying to acquire %s", queuedLock), e);
            }
            if (!response.isValid()) {
                throw ServiceException.LOCK_FAILED("invalid redis lock response", null);
            }
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("lock response: %s", response);
            }
            if (response.success()) {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("successfully acquired %s from thread %s: %s", this, getThreadLockName(), context);
                }
            } else {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("%s received unlock message but it was acquired elsewhere, will try again (%s ms left)", this, context.getRemainingTime());
                }
            }
            return response;
        }, skipQueue);
        if (waitingLock.canTryAcquireNow()) {
            LockResponse response;
            try {
                response = tryAcquire();
            } catch (Exception e) {
                lockChannel.remove(waitingLock);
                throw ServiceException.LOCK_FAILED(String.format("exception encountered trying to acquire %s", waitingLock), e);
            }
            if (!response.isValid()) {
                throw ServiceException.LOCK_FAILED("invalid redis lock response", null);
            }
            if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                ZimbraLog.mailboxlock.trace("lock response: %s", response);
            }
            if (response.success()) {
                if (ZimbraLog.mailboxlock.isTraceEnabled()) {
                    ZimbraLog.mailboxlock.trace("successfully acquired %s without waiting from thread %s", this, getThreadLockName());
                }
                lockChannel.remove(waitingLock);
                return response;
            }
            long timeout = Math.min(response.getTTL(), getTimeout());
            return lockChannel.waitForUnlock(waitingLock, timeout);
        } else {
            return lockChannel.waitForUnlock(waitingLock, getTimeout());
        }
    }

    protected <T> T execute(String script, Codec codec, RedisCommand<T> commandType, List<Object> keys, Object... values) {
        CommandExecutor executor = client.getCommandExecutor();
        return executor.evalWrite(lockName, codec, commandType, script, keys, values);
    }

    public void unlock() {
        if (ZimbraLog.mailboxlock.isTraceEnabled()) {
            ZimbraLog.mailboxlock.trace("releasing redis lock %s from thread %s", this, getThreadLockName());
        }
        if (unlockInner() == null) {
            ZimbraLog.mailboxlock.warn("%s attempted to release a lock it did not hold!", getThreadLockName());
        }
    }

    protected String getUnlockMsg() {
        return String.format("%s|%s", accountId, uuid);
    }

    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", uuid)
                .add("accountId", accountId);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }

    public static class LockResponse {

        private Long ttl;
        private String lastWriter;
        private boolean firstReadSinceLastWrite;
        private boolean validResponse;
        private List<String> holderUuids;

        private LockResponse(boolean validResponse) {
            this.validResponse = validResponse;
        }

        public LockResponse(Long ttl, List<String> holderUuids) {
            this.ttl = ttl;
            this.validResponse = true;
            this.holderUuids = holderUuids;
        }

        public LockResponse(String lastWriter, boolean firstReadSinceLastWrite) {
            this.lastWriter = lastWriter;
            this.firstReadSinceLastWrite = firstReadSinceLastWrite;
            this.validResponse = true;
        }

        public boolean success() {
            return ttl == null;
        }

        public Long getTTL() {
            return ttl;
        }

        public String getLastWriter() {
            return lastWriter;
        }

        public boolean isFirstReadSinceLastWrite() {
            return firstReadSinceLastWrite;
        }

        public boolean isValid() {
            return validResponse;
        }

        @Override
        public String toString() {
            ToStringHelper helper = MoreObjects.toStringHelper(this).add("success", ttl == null);
            if (validResponse) {
                if (ttl == null) {
                    helper.add("lastWriter", lastWriter);
                    helper.add("firstRead", firstReadSinceLastWrite);
                } else {
                    helper.add("ttl", ttl);
                    helper.add("holders", holderUuids);
                }
            } else {
                helper.add("validResponse", false);
            }
            return helper.toString();
        }

        private static final LockResponse INVALID_LOCK_RESPONSE = new LockResponse(false);

    }

    private static class LockResponseConvertor implements MultiDecoder<LockResponse> {

        @Override
        public Decoder<Object> getDecoder(int paramNum, State state) {
            return null;
        }

        @Override
        public LockResponse decode(List<Object> parts, State state) {
            if (parts.size() < 2) {
                return LockResponse.INVALID_LOCK_RESPONSE;
            }
            if (parts.get(0).equals(Long.valueOf(1))) {
                //lock success
                String lastWriter = (String) parts.get(1);
                boolean isFirstReadSinceLastWrite = parts.size() == 3 ? (parts.get(2)).equals(Long.valueOf(1)) : true;
                return new LockResponse(lastWriter, isFirstReadSinceLastWrite);
            } else {
                //failed to acquire lock, returning TTL
                Long ttl = (Long) parts.get(1);
                List<String> holderUuids = new ArrayList<>();
                for (Object i: parts.subList(2, parts.size())) {
                    holderUuids.add(i.toString());
                }
                return new LockResponse(ttl, holderUuids);
            }
        }
    }
}
