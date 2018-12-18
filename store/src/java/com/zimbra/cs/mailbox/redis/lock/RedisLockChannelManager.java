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

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.redis.RedisUtils;
import com.zimbra.cs.mailbox.redis.RedisUtils.RedisKey;


public class RedisLockChannelManager {

    private static RedisLockChannelManager instance = new RedisLockChannelManager();
    private RedisLockChannel[] channels;

    private RedisLockChannelManager() {
        int numChannels = LC.redis_num_lock_channels.intValue();
        channels = new RedisLockChannel[numChannels];
        for (int i=0; i<numChannels; i++) {
            String hashTag = String.format("LOCK-%d",i);
            RedisKey channelName = RedisUtils.createHashTaggedKey(hashTag, "LOCK-CHANNEL");
            channels[i] = new RedisLockChannel(channelName);
        }
    }

    public static RedisLockChannelManager getInstance() {
        return instance;
    }

    public RedisLockChannel getLockChannel(String accountId) {
        return channels[Math.abs(accountId.hashCode() % channels.length)];
    }
}
