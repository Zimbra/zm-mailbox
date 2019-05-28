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

import com.zimbra.common.mailbox.MailboxLockContext;



public class RedisReadWriteLock {

    private String accountId;
    private String lockBaseName;
    private String lockId;

    public RedisReadWriteLock(String accountId, String lockBaseName, String lockId) {
        this.accountId = accountId;
        this.lockBaseName = lockBaseName;
        this.lockId = lockId;
    }

    public RedisLock readLock(MailboxLockContext lockContext) {
        return new RedisReadLock(accountId, lockBaseName, lockId, lockContext);
    }

    public RedisLock writeLock(MailboxLockContext lockContext) {
        return new RedisWriteLock(accountId, lockBaseName, lockId, lockContext);
    }
}
