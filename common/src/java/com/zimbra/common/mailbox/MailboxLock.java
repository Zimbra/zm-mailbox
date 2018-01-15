package com.zimbra.common.mailbox;

public interface MailboxLock extends AutoCloseable {
    boolean isWriteLock();

    boolean isWriteLockedByCurrentThread();

    boolean isUnlocked();

    int getHoldCount();

    void lock();

    void close();
}
