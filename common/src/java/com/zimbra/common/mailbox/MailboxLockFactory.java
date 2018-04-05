package com.zimbra.common.mailbox;

/**
 * Autocloseable so that close() can be called on any resources used to gain access to a
 * locking framework for an instance of the factory.
 */
public interface MailboxLockFactory extends AutoCloseable {
    MailboxLock readLock();

    MailboxLock writeLock();

    MailboxLock acquiredWriteLock();

    MailboxLock acquiredReadLock();

    /**
     * Number of holds on this lock by the current thread (sum of read and write locks)
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    int getHoldCount();

    @Deprecated
    MailboxLock lock(boolean write);
}
