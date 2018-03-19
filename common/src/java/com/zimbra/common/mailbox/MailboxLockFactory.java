package com.zimbra.common.mailbox;

public interface MailboxLockFactory extends AutoCloseable {
    MailboxLock readLock();

    MailboxLock writeLock();

    MailboxLock acquiredWriteLock();

    MailboxLock acquiredReadLock();

    @Deprecated
    MailboxLock lock(boolean write);
}
