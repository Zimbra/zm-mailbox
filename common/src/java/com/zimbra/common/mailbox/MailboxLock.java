package com.zimbra.common.mailbox;

import com.zimbra.common.service.ServiceException;

public interface MailboxLock extends AutoCloseable {
    boolean isWriteLock();

    boolean isWriteLockedByCurrentThread() throws ServiceException;
    boolean isReadLockedByCurrentThread() throws ServiceException;
    boolean isLockedByCurrentThread() throws ServiceException;

    /**
     * Number of holds on this lock by the current thread
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    int getHoldCount() throws ServiceException;

    /**
     * Acquire a lock
     * @return
     * @throws ServiceException
     */
    void lock(MailboxLockContext lockContext) throws ServiceException;

    /* override which doesn't throw any exceptions */
    @Override
    void close();

}
