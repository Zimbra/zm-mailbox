package com.zimbra.client;

import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;

public class ZLocalMailboxLockFactory implements MailboxLockFactory {
    private ZLocalMailboxLock lock;

    public ZLocalMailboxLockFactory(Integer maxWaitingThreads, Integer timeoutSeconds) {
        this.lock = new ZLocalMailboxLock(maxWaitingThreads, timeoutSeconds);
    }

    @Override
    public MailboxLock readLock() {
        return this.lock;
    }

    @Override
    public MailboxLock writeLock() {
        return this.lock;
    }

    @Override
    public MailboxLock acquiredWriteLock() {
        MailboxLock myLock = writeLock();
        myLock.lock();
        return myLock;
    }

    @Override
    public MailboxLock acquiredReadLock() {
        MailboxLock myLock = readLock();
        myLock.lock();
        return myLock;
    }

    @Override
    public MailboxLock lock(boolean write) {
        if (write) {
            return writeLock();
        }
        return this.readLock();
    }

    @Override
    public void close() throws Exception {
        //Override method
    }
}
