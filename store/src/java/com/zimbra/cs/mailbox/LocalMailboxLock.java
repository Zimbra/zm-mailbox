/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;



import java.util.concurrent.locks.Lock;

import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.cs.mailbox.lock.ZLock;



/**
 * {@link MailboxLock} is a replacement of the implicit monitor lock using {@code synchronized} methods or statements on
 * a mailbox instance. This gives extended capabilities such as timeout and limit on number of threads waiting for a
 * particular mailbox lock. It is no longer legal to synchronize on a mailbox, otherwise an assertion error will be
 * thrown. {@code Mailbox.beginTransaction()}) internally acquires the mailbox lock and it's released by
 * {@code Mailbox.endTransaction()}, so that you don't have to explicitly call {@link #lock()} and {@link #release()}
 * wrapping a mailbox transaction.
 *
 */
public final class LocalMailboxLock implements MailboxLock {

	private final boolean write;
	private final java.util.concurrent.locks.Lock lock;
	private final ZLock zLock;

	public LocalMailboxLock(final Lock lock,final boolean write, ZLock zLock) {
		this.lock = lock;
		this.write = write;
		this.zLock = zLock;
	}

	@Override
	public void close() {
		this.lock.unlock();
	}

	@Override
	public int getHoldCount() {
		return zLock.getReadHoldCount() + zLock.getWriteHoldCount();
	}

	@Override
	public boolean isUnlocked() {
		return !isWriteLockedByCurrentThread() && zLock.getReadHoldCount() == 0;
	}

	@Override
	public boolean isWriteLock() {
		return this.write;
	}

	@Override
	public boolean isWriteLockedByCurrentThread() {
		return zLock.isWriteLockedByCurrentThread();
	}

	@Override
	public void lock() {
		this.lock.lock();
	}



}