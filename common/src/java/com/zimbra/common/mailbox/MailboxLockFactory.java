package com.zimbra.common.mailbox;

import com.zimbra.common.service.ServiceException;

/**
 * Autocloseable so that close() can be called on any resources used to gain access to a
 * locking framework for an instance of the factory.
 */
public interface MailboxLockFactory extends AutoCloseable {
    MailboxLock readLock(MailboxLockContext lockContext);

    MailboxLock writeLock(MailboxLockContext lockContext);

    MailboxLock acquiredWriteLock(MailboxLockContext lockContext) throws ServiceException;

    MailboxLock acquiredReadLock(MailboxLockContext lockContext) throws ServiceException;

    /**
     * Number of holds on this lock by the current thread (sum of read and write locks)
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    int getHoldCount() throws ServiceException;

    @Deprecated
    MailboxLock lock(MailboxLockContext lockContext, boolean write);
}
