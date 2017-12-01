package com.zimbra.cs.mailbox;

import java.util.Stack;

import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.mailbox.MailboxLockFactory;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.lock.DebugZLock;
import com.zimbra.cs.mailbox.lock.ZLock;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zookeeper.CuratorManager;


public class LocalMailboxLockFactory implements MailboxLockFactory {
	private final ZLock zLock = DebugConfig.debugMailboxLock ? new DebugZLock() : new ZLock();
    private InterProcessSemaphoreMutex dLock = null;
    private Mailbox mbox;

    public LocalMailboxLockFactory(final String id, final Mailbox mbox) {
    	if (Zimbra.isAlwaysOn()) {
            try {
                dLock = CuratorManager.getInstance().createLock(id);
            } catch (ServiceException se) {
                ZimbraLog.mailbox.error("could not initialize distributed lock", se);
            }
        }
        this.mbox = mbox;
    }

    @Override
    public MailboxLock readLock() {
        return new LocalMailboxLock (zLock.readLock(), false,zLock,dLock,mbox);
    }

    @Override
    public MailboxLock writeLock() {
    	 return new LocalMailboxLock (zLock.writeLock(), true,zLock,dLock,mbox);
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
