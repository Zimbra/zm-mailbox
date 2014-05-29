/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.lock;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lock owner and hold count tracker for debugging read lock
 */
public class MailboxLockOwner {

    private AtomicInteger count = new AtomicInteger(0);
    private final Thread owner;

    MailboxLockOwner() {
        owner = Thread.currentThread();
    }

    void increment() {
        count.incrementAndGet();
    }

    int decrement() {
        return count.decrementAndGet();
    }

    int getCount() {
        return count.get();
    }

    Thread getOwnerThread() {
        return owner;
    }
}
