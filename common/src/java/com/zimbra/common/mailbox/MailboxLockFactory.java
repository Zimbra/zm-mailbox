package com.zimbra.common.mailbox;

public interface MailboxLockFactory extends AutoCloseable {
    MailboxLock readLock();

    MailboxLock writeLock();

    @Deprecated
    MailboxLock lock(boolean write);
}
