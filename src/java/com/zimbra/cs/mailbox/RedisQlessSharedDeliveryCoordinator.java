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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.qless.Job;
import com.zimbra.qless.QlessClient;
import com.zimbra.qless.Queue;

public class RedisQlessSharedDeliveryCoordinator extends RedisSharedDeliveryCoordinator {
    @Autowired QlessClient qless;
    Map<String, String> jidByAccountId = new HashMap<>();

    /** Constructor */
    public RedisQlessSharedDeliveryCoordinator() {
    }

    protected String getJobClass() {
        return RedisQlessSharedDeliveryCoordinator.class.getName();
    }

    protected Job getJob(Mailbox mbox) throws IOException {
        String jid = jidByAccountId.get(mbox.getAccountId());
        if (jid != null) {
            return qless.getJob(jid);
        }
        return null;
    }

    protected Queue getQueue(Mailbox mbox) {
        return qless.queue("mbox:" + mbox.getAccountId());
    }

    /**
     * @return true if shared delivery may begin; false if shared delivery may
     *         not begin because of a pending operation
     */
    @Override
    public boolean beginSharedDelivery(Mailbox mbox) throws ServiceException {

        // If request for other ops is pending on this mailbox, don't allow
        // any more shared deliveries from starting.
        if (!isSharedDeliveryAllowed(mbox)) {
            return false;
        }

        try {

            // If a pending operation is already running for this mailbox, the shared delivery may not begin.
            Queue queue = getQueue(mbox);
            if (queue.jobs().jobs("running").size() > 0) {
                return false;
            }

            String jid = jidByAccountId.get(mbox.getAccountId());

            if (jid == null) {
                // Create job
                jid = queue.put(getJobClass(), null, null);
                jidByAccountId.put(mbox.getAccountId(), jid);
                queue.pop(); // mark job as started by this worker
            }

            // Increment nesting count (or initialize to 1)
            try (Jedis jedis = jedisPool.getResource()) {
                String key = getCountKeyName(mbox);
                Transaction transaction = jedis.multi();
                transaction.incr(key);
                transaction.expire(key, EXPIRY_SECS);
                List<Object> result = transaction.exec();
                int newCount = Integer.parseInt(result.get(0).toString());
                ZimbraLog.mailbox.debug("# of shared deliv incr to " + newCount + " for mailbox " + mbox.getId());
            }

            return true;

        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed queuing shared delivery job", e);
        }
    }


    @Override
    public void endSharedDelivery(Mailbox mbox) throws ServiceException {

        // Decrement nesting count
        int newCount;
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getCountKeyName(mbox);
            Transaction transaction = jedis.multi();
            transaction.decr(key);
            transaction.expire(key, EXPIRY_SECS);
            List<Object> result = transaction.exec();
            newCount = Integer.parseInt(result.get(0).toString());
            ZimbraLog.mailbox.debug("# of shared deliv decr to " + newCount +
                    " for mailbox " + mbox.getId());
        }

        // If nesting count has returned to 1, mark qless job as completed
        if (newCount < 1) {
            try {
                Job job = getJob(mbox);
                if (job != null) {
                    job.complete();
                }
                jidByAccountId.remove(mbox.getAccountId());
            } catch (IOException e) {
                throw ServiceException.FAILURE("Failure occurred while ending shared delivery", e);
            }
        }
    }


    @Override
    public void waitUntilSharedDeliveryCompletes(Mailbox mbox) throws ServiceException {
        try {
            while (true) {
                Job job = getJob(mbox);
                if (job == null || "complete".equals(job.getState())) {
                    return;
                }

                try {
                    synchronized (this) {
                        wait(WAIT_MS);
                    }
                    ZimbraLog.misc.debug("wake up from wait for completion of shared delivery; mailbox=%d, # of shared deliv=%d",
                            mbox.getId(), job.data("count"));
                } catch (InterruptedException e) {}
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failure occurred while waiting for shared delivery to complete", e);
        }
    }


    @Override
    public boolean isSharedDeliveryComplete(Mailbox mbox) throws ServiceException {
        try {
            Job job = getJob(mbox);
            if (job == null) {
                return true;
            }
            return "complete".equals(job.getState());
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed determining shared delivery completion status", e);
        }
    }
}
