/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
