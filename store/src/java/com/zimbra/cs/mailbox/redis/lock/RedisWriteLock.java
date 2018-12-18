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

import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.RedisCommands;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.zimbra.common.localconfig.LC;

public class RedisWriteLock extends RedisLock {

    public RedisWriteLock(String accountId, String lockBaseName) {
        super(accountId, lockBaseName);
    }

    @Override
    protected long getLeaseTime() {
        return LC.zimbra_mailbox_lock_write_lease_seconds.intValue() *1000;
    }

    @Override
    protected Long tryAcquire() {
        String script =
                "local mode = redis.call('hget', KEYS[1], 'mode'); " +
                "if (mode == false) then " +
                      "redis.call('hset', KEYS[1], 'mode', 'write'); " +
                      "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                      "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                      "return nil; " +
                  "end; " +
                  "if (mode == 'write') then " +
                      "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                          "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                          "local currentExpire = redis.call('pttl', KEYS[1]); " +
                          "redis.call('pexpire', KEYS[1], currentExpire + ARGV[1]); " +
                          "return nil; " +
                      "end; " +
                    "end;" +
                    "return redis.call('pttl', KEYS[1]);";
        return execute(script, LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
            Arrays.<Object>asList(lockName),
            getLeaseTime(), getThreadLockName());
    }

    @Override
    protected String getThreadLockName() {
        return super.getThreadLockName() + ":write";
    }

    @Override
    protected Boolean unlockInner() {
        String script =
                "local mode = redis.call('hget', KEYS[1], 'mode'); " +
                "if (mode == false) then " +
                    "redis.call('publish', KEYS[2], ARGV[1]); " +
                    "return 1; " +
                "end;" +
                "if (mode == 'write') then " +
                    "local lockExists = redis.call('hexists', KEYS[1], ARGV[3]); " +
                    "if (lockExists == 0) then " +
                        "return nil;" +
                    "else " +
                        "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
                        "if (counter > 0) then " +
                            "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                            "return 0; " +
                        "else " +
                            "redis.call('hdel', KEYS[1], ARGV[3]); " +
                            "if (redis.call('hlen', KEYS[1]) == 1) then " +
                                "redis.call('del', KEYS[1]); " +
                                "redis.call('publish', KEYS[2], ARGV[1]); " +
                            "else " +
                                // has unlocked read-locks
                                "redis.call('hset', KEYS[1], 'mode', 'read'); " +
                            "end; " +
                            "return 1; "+
                        "end; " +
                    "end; " +
                "end; "
                + "return nil;";
        return execute(script, LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
        Arrays.<Object>asList(lockName, lockChannelName),
        getUnlockMsg(), getLeaseTime(), getThreadLockName());
    }

    @Override
    protected ToStringHelper toStringHelper() {
        return super.toStringHelper().add("type", "write");
    }
}
