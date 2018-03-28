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

    @Deprecated
    MailboxLock lock(boolean write);
}
