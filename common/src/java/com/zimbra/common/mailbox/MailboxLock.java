package com.zimbra.common.mailbox;

public interface MailboxLock extends AutoCloseable {
    boolean isWriteLock();

    boolean isWriteLockedByCurrentThread();
    boolean isReadLockedByCurrentThread();
    boolean isLockedByCurrentThread();

    /**
     * Number of holds on this lock by the current thread
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    int getHoldCount();

    void lock();

    /* override which doesn't throw any exceptions */
    @Override
    void close();
}
