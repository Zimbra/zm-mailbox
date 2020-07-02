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
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.mailbox.MailboxLockContext;

public class RedisReadLock extends RedisLock {

    public RedisReadLock(String accountId, String lockBaseName, String lockNode, MailboxLockContext lockContext) {
        super(accountId, lockBaseName, lockNode, lockContext);
    }

    private String getReadWriteTimeoutNamePrefix() {
        return lockName + ":" + getThreadLockName() + ":rwlock_timeout";
    }

    protected String getKeyPrefix(String timeoutPrefix) {
        //need to use Pattern.quote() because the lock name contains curly braces
        return timeoutPrefix.split(":" + Pattern.quote(getThreadLockName()))[0];
    }

    private String getWriteLockName() {
        return getThreadLockName() + ":write";
    }

    @Override
    protected LockResponse tryAcquire() {
        String script =
            "local mode = redis.call('hget', KEYS[1], 'mode'); " +
            "if (mode == false) then " +
              //no one is holding the lock, so set the mode to "read" and set this thread's hold count to 1
              "redis.call('hset', KEYS[1], 'mode', 'read'); " +
              "redis.call('hset', KEYS[1], ARGV[2], 1); " +
              //set an expiration for the read hold, using the uuid of the holder as the value
              "redis.call('set', KEYS[2] .. ':1', ARGV[5]); " +
              "redis.call('pexpire', KEYS[2] .. ':1', ARGV[1]); " +
               //set an expiration for the lock hash
              "redis.call('pexpire', KEYS[1], ARGV[1]); " +
               //add the acquiring thread to the map of locks held by this node
              "redis.call('hset', KEYS[3], ARGV[2], KEYS[1]); " +
               //get the name of the last mailbox worker that acquired a write lock
              "local last_writer = redis.call('get', KEYS[1] .. ':last_writer'); " +
               //add this worker's name to the set of workers that have acquired a read lock since the last write
              "local is_first_read_since_last_write = redis.call('sadd', KEYS[1] .. ':reads_since_last_write', ARGV[4]); " +
              "return {1, last_writer, is_first_read_since_last_write}; " +
            "end; " +
            "if (mode == 'read') or (mode == 'write' and redis.call('hexists', KEYS[1], ARGV[3]) == 1) then " +
               //either the lock is in read mode, or the requesting thread is holding a write lock,
               //which means it's OK to acquire a read lock too
              "local ind = redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
               //set an expiration on the new read hold, using the uuid of the holder as the value
              "local key = KEYS[2] .. ':' .. ind; " +
              "redis.call('set', key, ARGV[5]); " +
              "redis.call('pexpire', key, ARGV[1]); " +
               //update the expiration on the lock hash
              "redis.call('pexpire', KEYS[1], ARGV[1]); " +
               //add the acquiring thread to the map of locks held by this node
              "redis.call('hset', KEYS[3], ARGV[2], KEYS[1]); " +
               //get the name of the last mailbox worker that acquired a write lock
              "local last_writer = redis.call('get', KEYS[1] .. ':last_writer'); " +
               //add this worker's name to the set of workers that have acquired a read lock since the last write
              "local is_first_read_since_last_write = redis.call('sadd', KEYS[1] .. ':reads_since_last_write', ARGV[4]); " +
              "return {1, last_writer, is_first_read_since_last_write}; " +
            "end;" +
            //a read lock cannot be acquired, so return the TTL of the lock hash
            // and the uuids of all active write lock holders
            "local retvals = {0, redis.call('pttl', KEYS[1])}; " +
            "for n, key in ipairs(redis.call('hkeys', KEYS[1])) do " +
                "local counter = tonumber(redis.call('hget', KEYS[1], key)); " +
                "if type(counter) == 'number' then " +
                    "for i=counter, 1, -1 do " +
                        //write lock uuids are stored in dedicated "{lock name}:{thread}:uuid:{#}" key
                        "local uuid_key = KEYS[1] .. ':' .. key .. ':uuid:' .. i; " +
                        "local uuid = redis.call('get', uuid_key); " +
                        "if uuid ~= false then " +
                            "table.insert(retvals, uuid); " +
                        "end; " +
                    "end; " +
                "end; " +
            "end; " +
             //return the TTL of the lock hash and the uuids of all instances holding this lock
            "return retvals;";

        return execute(script, StringCodec.INSTANCE, RedisLock.LOCK_RESPONSE_CMD,
                Arrays.<Object>asList(lockName, getReadWriteTimeoutNamePrefix(), hashtaggedNodeKey),
                getLeaseTime(), getThreadLockName(), getWriteLockName(), lockNode, uuid);
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
                     //delete this thread from the map of locks held by this node
                    "redis.call('hdel', KEYS[5], ARGV[2]); " +
                "end;" +
                 //delete the timeout key for this hold number
                "redis.call('del', KEYS[3] .. ':' .. (counter+1)); " +

                //check if there are any other reads holds on this lock, and if there are,
                //see which one has the longest remaining TTL
                "if (redis.call('hlen', KEYS[1]) > 1) then " +
                    "local maxRemainTime = -3; " +
                    "local keys = redis.call('hkeys', KEYS[1]); " +
                    "for n, key in ipairs(keys) do " +
                        "local counter = tonumber(redis.call('hget', KEYS[1], key)); " +
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
                Arrays.<Object>asList(lockName, lockChannelName, timeoutPrefix, keyPrefix, hashtaggedNodeKey),
                getUnlockMsg(), getThreadLockName());
    }

    @Override
    protected ToStringHelper toStringHelper() {
        return super.toStringHelper().add("type", "read");
    }
}
