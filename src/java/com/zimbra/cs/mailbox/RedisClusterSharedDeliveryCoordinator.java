package com.zimbra.cs.mailbox;
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.JedisCluster;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class RedisClusterSharedDeliveryCoordinator extends RedisSharedDeliveryCoordinator {
    @Autowired protected JedisCluster jedisCluster;

    /** Constructor */
    public RedisClusterSharedDeliveryCoordinator() {
    }

    @Override
    public boolean beginSharedDelivery(Mailbox mbox) throws ServiceException {

        // If request for other ops is pending on this mailbox, don't allow
        // any more shared deliveries from starting.
        if (!isSharedDeliveryAllowed(mbox)) {
            return false;
        }

        String key = getCountKeyName(mbox);
        long newCount = jedisCluster.incr(key);
        jedisCluster.expire(key, EXPIRY_SECS);
        ZimbraLog.mailbox.debug("# of shared deliv incr to " + newCount + " for mailbox " + mbox.getId());
        return true;
    }

    @Override
    public void endSharedDelivery(Mailbox mbox) throws ServiceException {
        String key = getCountKeyName(mbox);
        long newCount = jedisCluster.decr(key);
        jedisCluster.expire(key, EXPIRY_SECS);
        ZimbraLog.mailbox.debug("# of shared deliv decr to " + newCount + " for mailbox " + mbox.getId());
    }

    @Override
    public boolean isSharedDeliveryAllowed(Mailbox mbox) throws ServiceException {
        String key = getAllowedKeyName(mbox);
        String value = jedisCluster.get(key);
        return value == null || Boolean.TRUE.toString().equals(value);
    }

    @Override
    public void setSharedDeliveryAllowed(Mailbox mbox, boolean allow) throws ServiceException {
        // If someone is changing allow from true to false, wait for completion of active shared deliveries
        // before allowing the change
        if (allow == false && isSharedDeliveryAllowed(mbox)) {
            ZimbraLog.mailbox.debug("Waiting for completion of active shared delivery before allowing disabling; mailbox={}", mbox.getId());
            waitUntilSharedDeliveryCompletes(mbox);
        }

        String key = getAllowedKeyName(mbox);
        jedisCluster.setex(key, EXPIRY_SECS, Boolean.toString(allow));
    }

    @Override
    public void waitUntilSharedDeliveryCompletes(Mailbox mbox) throws ServiceException {
        String key = getCountKeyName(mbox);
        while (true) {
            int count;
            String countStr = jedisCluster.get(key);
            count = countStr == null ? 0 : Integer.parseInt(countStr);
            if (count < 1) {
                break;
            }

            try {
                synchronized (this) {
                    wait(WAIT_MS);
                }
                ZimbraLog.misc.info("wake up from wait for completion of shared delivery; mailbox=%d, # of shared deliv=%d",
                        mbox.getId(), count);
            } catch (InterruptedException e) {}
        }
    }

    @Override
    public boolean isSharedDeliveryComplete(Mailbox mbox) throws ServiceException {
        String key = getCountKeyName(mbox);
        String countStr = jedisCluster.get(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        return count < 1;
    }
}
