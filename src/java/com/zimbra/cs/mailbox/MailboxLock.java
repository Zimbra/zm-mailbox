/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;




/**
 * {@link MailboxLock} is a replacement of the implicit monitor lock using {@code synchronized} methods or statements on
 * a mailbox instance. This gives extended capabilities such as timeout and limit on number of threads waiting for a
 * particular mailbox lock. It is no longer legal to synchronize on a mailbox, otherwise an assertion error will be
 * thrown. {@code Mailbox.beginTransaction()}) internally acquires the mailbox lock and it's released by
 * {@code Mailbox.endTransaction()}, so that you don't have to explicitly call {@link #lock()} and {@link #release()}
 * wrapping a mailbox transaction.
 *
 */
public interface MailboxLock {

    /** Returns the hold count */
    public int getHoldCount();

    /** Returns whether the lock is not currently obtained */
    public boolean isUnlocked();

    /** Returns whether the lock is currently obtained in read-write mode, and for the current thread */
    public boolean isWriteLockedByCurrentThread();

    /** Acquire the lock in read-write mode, or increments the hold count if the lock is already acquired */
    public void lock();

    /** Acquire the lock in read or read-write mode, or increments the hold count if the lock is already acquired */
    public void lock(boolean write);

    /** Release the lock */
    public void release();
}
