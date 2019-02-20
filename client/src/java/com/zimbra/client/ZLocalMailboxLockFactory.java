package com.zimbra.client;

import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockContext;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;

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
    public MailboxLock acquiredWriteLock(MailboxLockContext lockContext) throws ServiceException {
        MailboxLock myLock = writeLock();
        myLock.lock(lockContext);
        return myLock;
    }

    @Override
    public MailboxLock acquiredReadLock(MailboxLockContext lockContext) throws ServiceException {
        MailboxLock myLock = readLock();
        myLock.lock(lockContext);
        return myLock;
    }

    /**
     * Number of holds on this lock by the current thread (sum of read and write locks)
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    @Override
    public int getHoldCount() {
        return lock.getHoldCount();
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
