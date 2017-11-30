package com.zimbra.common.mailbox;

import com.zimbra.common.mailbox.MailboxLock;

public interface MailboxLockFactory extends AutoCloseable {
    MailboxLock readLock();

    MailboxLock writeLock();

    @Deprecated
    MailboxLock lock(boolean write);
}
