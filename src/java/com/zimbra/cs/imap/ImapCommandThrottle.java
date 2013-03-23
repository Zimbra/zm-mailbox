/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.zimbra.common.util.Constants;
import com.zimbra.common.util.MapUtil;

public class ImapCommandThrottle {
    private static final Map<String, ReentrantLock> commandLock = MapUtil.newTimeoutMap(1 * Constants.MILLIS_PER_HOUR);
    private static final long LOCK_TIMEOUT = 10 * Constants.MILLIS_PER_MINUTE;

    /**
     * Obtain a lock for serialization of expensive commands such as FETCH.
     * Each authenticated account may only perform one expensive command at a time regardless of number of sessions/connections
     * @param accountId - the authenticated account ID
     * @return ReentrantLock instance. Caller *must* unlock it when finished with the expensive operation
     * @throws ImapThrottledException
     */
    public static ReentrantLock lock(String accountId) throws ImapThrottledException {
        ReentrantLock lock = null;
        synchronized (commandLock) {
            lock = commandLock.get(accountId);
            if (lock == null) {
                lock = new ReentrantLock();
                commandLock.put(accountId, lock);
            }
        }
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        if (!locked) {
            throw new ImapThrottledException("Unable to obtain command lock " + lock.toString() + " aborting operation");
        } else {
            return lock;
        }
    }
}
