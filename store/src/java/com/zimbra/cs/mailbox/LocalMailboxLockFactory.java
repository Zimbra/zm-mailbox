package com.zimbra.cs.mailbox;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.cs.mailbox.lock.DebugZLock;
import com.zimbra.cs.mailbox.lock.ZLock;


public class LocalMailboxLockFactory implements MailboxLockFactory {
    private final ZLock zLock = DebugConfig.debugMailboxLock ? new DebugZLock() : new ZLock();
    private Mailbox mbox;

    public LocalMailboxLockFactory(final String id, final Mailbox mbox) {
        this.mbox = mbox;
    }

    @Override
    public MailboxLock readLock() {
        return new LocalMailboxLock (zLock.readLock(), false,zLock);
    }

    @Override
    public MailboxLock writeLock() {
    	 return new LocalMailboxLock (zLock.writeLock(), true,zLock);
    }

    @Override
    @Deprecated
    public MailboxLock lock(boolean write) {
    	if (write) {
            return writeLock();
        }
        return readLock();
    }

    @Override
    public void close() throws Exception {
    	
    }
}
