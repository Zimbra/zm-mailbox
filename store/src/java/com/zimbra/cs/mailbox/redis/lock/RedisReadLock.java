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

import java.util.Arrays;
import java.util.regex.Pattern;

import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.RedisCommands;

import com.google.common.base.MoreObjects.ToStringHelper;

public class RedisReadLock extends RedisLock {

    public RedisReadLock(String accountId, String lockBaseName) {
        super(accountId, lockBaseName);
    }

    private String getReadWriteTimeoutNamePrefix() {
        return lockName + ":" + getThreadLockName() + ":rwlock_timeout";
    }

    protected String getKeyPrefix(String timeoutPrefix) {
        //need to use Pattern.quote() because the lock name contains curly braces
        return timeoutPrefix.split(":" + Pattern.quote(getThreadLockName()))[0];
    }

    private String getWriteLockName() {
        return lockName + ":write";
    }

    @Override
    protected Long tryAcquire() {
        String script =
            "local mode = redis.call('hget', KEYS[1], 'mode'); " +
            "if (mode == false) then " +
              //no one is holding the lock, so set the mode to "read" and set this thread's hold count to 1
              "redis.call('hset', KEYS[1], 'mode', 'read'); " +
              "redis.call('hset', KEYS[1], ARGV[2], 1); " +
              //set an expiration for the read hold
              "redis.call('set', KEYS[2] .. ':1', 1); " +
              "redis.call('pexpire', KEYS[2] .. ':1', ARGV[1]); " +
               //set an expiration for the lock hash
              "redis.call('pexpire', KEYS[1], ARGV[1]); " +
              "return nil; " +
            "end; " +
            "if (mode == 'read') or (mode == 'write' and redis.call('hexists', KEYS[1], ARGV[3]) == 1) then " +
               //either the lock is in read mode, or the requesting thread is holding a write lock,
               //which means it's OK to acquire a read lock too
              "local ind = redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
               //set an expiration on the new read hold
              "local key = KEYS[2] .. ':' .. ind;" +
              "redis.call('set', key, 1); " +
              "redis.call('pexpire', key, ARGV[1]); " +
               //update the expiration on the lock hash
              "redis.call('pexpire', KEYS[1], ARGV[1]); " +
              "return nil; " +
            "end;" +
             //a read lock cannot be acquired, so return the TTL of the lock hash
            "return redis.call('pttl', KEYS[1]);";

        return execute(script, LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
                Arrays.<Object>asList(lockName, getReadWriteTimeoutNamePrefix()),
                getLeaseTime(), getThreadLockName(), getWriteLockName());
    }

    @Override
    protected Boolean unlockInner() {

        String script =
                "local mode = redis.call('hget', KEYS[1], 'mode'); " +
                "if (mode == false) then " +
                    //no one is actually holding the lock (maybe it expired), so publish an unlock message
                    "redis.call('publish', KEYS[2], ARGV[1]); " +
                    "return 1; " +
                "end; " +
                "local lockExists = redis.call('hexists', KEYS[1], ARGV[2]); " +
                "if (lockExists == 0) then " +
                    //the lock exists, but someone else is holding it (returning nil results in a warning message being logged)
                    "return nil;" +
                "end; " +

                //decrement the read hold count for this thread in the lock hash
                "local counter = redis.call('hincrby', KEYS[1], ARGV[2], -1); " +
                "if (counter == 0) then " +
                     //if this thread is not holding any more read locks, delete its key from the hash
                    "redis.call('hdel', KEYS[1], ARGV[2]); " +
                "end;" +
                 //delete the timeout key for this hold number
                "redis.call('del', KEYS[3] .. ':' .. (counter+1)); " +

                //check if there are any other reads holds on this lock, and if there are,
                //see which one has the longest remaining TTL
                "if (redis.call('hlen', KEYS[1]) > 1) then " +
                    "local maxRemainTime = -3; " +
                    "local keys = redis.call('hkeys', KEYS[1]); " +
                    "for n, key in ipairs(keys) do " +
                        "counter = tonumber(redis.call('hget', KEYS[1], key)); " +
                        "if type(counter) == 'number' then " +
                            "for i=counter, 1, -1 do " +
                                "local remainTime = redis.call('pttl', KEYS[4] .. ':' .. key .. ':rwlock_timeout:' .. i); " +
                                "maxRemainTime = math.max(remainTime, maxRemainTime);" +
                            "end; " +
                        "end; " +
                    "end; " +

                    "if maxRemainTime > 0 then " +
                         //update the expiration of the lock hash to the longest of the remaining holds.
                         //since there are other read holds left, we don't notify waiters
                        "redis.call('pexpire', KEYS[1], maxRemainTime); " +
                        "return 0; " +
                    "end;" +

                    "if mode == 'write' then " +
                        //if this thread was holding a write lock, no one else should be able to acquire it at this time,
                        //so return without notifying waiters
                        "return 0;" +
                    "end; " +
                "end; " +

                //if we're here, that means that no one else is holding the lock, so delete the lock hash
                //and publish an unlock message to waiters
                "redis.call('del', KEYS[1]); " +
                "redis.call('publish', KEYS[2], ARGV[1]); " +
                "return 1; ";

        String timeoutPrefix = getReadWriteTimeoutNamePrefix();
        String keyPrefix = getKeyPrefix(timeoutPrefix);

        return execute(script, LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                Arrays.<Object>asList(lockName, lockChannelName, timeoutPrefix, keyPrefix),
                getUnlockMsg(), getThreadLockName());
    }

    @Override
    protected ToStringHelper toStringHelper() {
        return super.toStringHelper().add("type", "read");
    }
}
