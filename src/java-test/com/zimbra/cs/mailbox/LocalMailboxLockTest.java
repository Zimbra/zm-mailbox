/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;

import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.LocalMailboxLock.LockFailedException;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.ZimbraConfig;

public class LocalMailboxLockTest extends AbstractMailboxLockTest {

    protected void resetStoreBetweenTests() throws Exception {}

    protected boolean isLockServiceAvailableForTest() throws Exception {
        return true;
    }

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", LocalZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void promote() {
        final Thread readThread = new Thread("MailboxLockTest-Reader") {
            @Override
            public void run() {
                Mailbox mbox;
                try {
                    int lockCount = 10;
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    //here's the interleaving we are explicitly exercising in this test
                    //1. writer - mbox.lock(write)
                    //2. reader - mbox.lock(read); call gets past the initial isWriteModeRequired() check and into tryLock(read)
                    //3. writer - mbox.purge()
                    //4. writer - mbox.unlock()
                    //5. reader - tryLock(read) returns, then recheck isWriteModeRequired() and promote

                    Assert.assertTrue(mbox.lock.isUnlocked());
                    for (int i = 0; i < lockCount; i++) {
                        mbox.lock.lock(false);
                        //loop so we exercise recursion in promote..
                    }
                    Assert.assertTrue(mbox.lock.isWriteLockedByCurrentThread());
                    //we're guaranteeing that reader lock is not held before writer
                    //but not guaranteeing that purge is called while reader is waiting
                    //i.e. if purge/release happens in writeThread before we actually get to lock call in this thread
                    //subtle, and shouldn't matter since promote is called either way, but if we see races in test this could be cause
                    mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
                    for (int i = 0; i < lockCount; i++) {
                        mbox.lock.release();
                    }
                } catch (ServiceException e) {
                    ZimbraLog.test.error(e.getLocalizedMessage(), e);
                    Assert.fail();
                }
            }
        };
        readThread.setDaemon(true);

        final Thread writeThread = new Thread("MailboxLockTest-Writer") {

            @Override
            public void run() {
                Mailbox mbox;
                try {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    mbox.lock.lock();
                    //start read thread only after holding mailbox lock
                    readThread.start();
                    //wait until read thread has tried to obtain mailbox lock
                    while (!((LocalMailboxLock)mbox.lock).hasQueuedThreads()) {
                        Thread.sleep(10);
                    }
                    mbox.purge(MailItem.Type.FOLDER);
                    mbox.lock.release();
                } catch (ServiceException | InterruptedException e) {
                    ZimbraLog.test.error(e.getLocalizedMessage(), e);
                    Assert.fail();
                }
            }
        };
        writeThread.setDaemon(true);


        writeThread.start();

        int joinTimeout = 10000; //use a timeout so test can fail gracefully in case we have a deadlock
        try {
            writeThread.join(joinTimeout);
            if (writeThread.isAlive()) {
                ZimbraLog.test.debug("Write Thread");
                for (StackTraceElement ste : writeThread.getStackTrace()) {
                    ZimbraLog.test.debug(ste);
                }
                if (readThread.isAlive()) {
                    ZimbraLog.test.debug("Read Thread");
                    for (StackTraceElement ste : readThread.getStackTrace()) {
                        ZimbraLog.test.debug(ste);
                    }
                }
            }
            Assert.assertFalse(writeThread.isAlive());
        } catch (InterruptedException e) {
            ZimbraLog.test.error(e.getLocalizedMessage(), e);
        }
        try {
            readThread.join(joinTimeout);
            Assert.assertFalse(readThread.isAlive());
        } catch (InterruptedException e) {
            ZimbraLog.test.error(e.getLocalizedMessage(), e);
        }
    }

    private void joinWithTimeout(Thread thread, long timeout) {
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
        }
        Assert.assertFalse(thread.isAlive());
    }

    @Test
    public void tooManyWaiters() {
        Mailbox mbox = null;
        try {
            mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        } catch (ServiceException e) {
            Assert.fail();
        }

        int threads  = ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMailboxLockMaxWaitingThreads, 15);
        final AtomicBoolean done = new AtomicBoolean(false);
        final Set<Thread> waitThreads = new HashSet<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread waitThread = new Thread("MailboxLockTest-Waiter") {
                @Override
                public void run() {
                    Mailbox mbox;
                    try {
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                        mbox.lock.lock(false);
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        mbox.lock.release();
                    } catch (ServiceException e) {
                    }
                }
            };
            waitThread.setDaemon(true);
            waitThreads.add(waitThread);
        }

        Thread writeThread = new Thread("MailboxLockTest-Writer") {
            @Override
            public void run() {
                Mailbox mbox;
                try {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    mbox.lock.lock(true);
                    for (Thread waiter : waitThreads) {
                        waiter.start();
                    }
                    while (!done.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    mbox.lock.release();
                } catch (ServiceException e) {
                }
            }
        };

        writeThread.start();

        while (((LocalMailboxLock)mbox.lock).getQueueLength() < ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMailboxLockMaxWaitingThreads, 15)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        try {
            mbox.lock.lock(false); //one more reader...this should give too many waiters
            Assert.fail("expected too many waiters");
        } catch (LockFailedException e) {
            //expected
            Assert.assertTrue(e.getMessage().startsWith("too many waiters"));
            done.set(true); //cause writer to finish
        }

        long joinTimeout = 50000;
        joinWithTimeout(writeThread, joinTimeout);
        for (Thread t: waitThreads) {
            joinWithTimeout(t, joinTimeout);
        }

        //now do a write lock in same thread. previously this would break due to read assert not clearing
        mbox.lock.lock(true);
        mbox.lock.release();
    }

    @Test
    public void tooManyWaitersWithSingleReadOwner() {
        Mailbox mbox = null;
        try {
            mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        } catch (ServiceException e) {
            Assert.fail();
        }

        int threads = ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMailboxLockMaxWaitingThreads, 15);
        final AtomicBoolean done = new AtomicBoolean(false);
        final Set<Thread> waitThreads = new HashSet<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread waitThread = new Thread("MailboxLockTest-Waiter") {
                @Override
                public void run() {
                    Mailbox mbox;
                    try {
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                        mbox.lock.lock(true);
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        mbox.lock.release();
                    } catch (ServiceException e) {
                    }
                }
            };
            waitThread.setDaemon(true);
            waitThreads.add(waitThread);
        }

        Thread readThread = new Thread("MailboxLockTest-Reader") {
            @Override
            public void run() {
                Mailbox mbox;
                try {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    int holdCount = 20;
                    for (int i = 0; i < holdCount; i++) {
                        mbox.lock.lock(false);
                    }
                    for (Thread waiter : waitThreads) {
                        waiter.start();
                    }
                    while (!done.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    for (int i = 0; i < holdCount; i++) {
                        mbox.lock.release();
                    }
                } catch (ServiceException e) {
                }
            }
        };

        readThread.start();

        while (((LocalMailboxLock)mbox.lock).getQueueLength() < ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMailboxLockMaxWaitingThreads, 15)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        try {
            mbox.lock.lock(false); //one more reader...this should give too many waiters
            Assert.fail("expected too many waiters");
        } catch (LockFailedException e) {
            //expected
            Assert.assertTrue(e.getMessage().startsWith("too many waiters"));
            done.set(true); //cause writer to finish
        }

        long joinTimeout = 50000;
        joinWithTimeout(readThread, joinTimeout);
        for (Thread t: waitThreads) {
            joinWithTimeout(t, joinTimeout);
        }

        //now do a write lock in same thread. previously this would break due to read assert not clearing
        mbox.lock.lock(true);
        mbox.lock.release();
    }


    @Test
    public void tooManyWaitersWithMultipleReadOwners() {
        Mailbox mbox = null;
        try {
            mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        } catch (ServiceException e) {
            Assert.fail();
        }

        int threads = ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMailboxLockMaxWaitingThreads, 15);
        final AtomicBoolean done = new AtomicBoolean(false);
        final Set<Thread> waitThreads = new HashSet<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread waitThread = new Thread("MailboxLockTest-Waiter-"+i) {
                @Override
                public void run() {
                    Mailbox mbox;
                    try {
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                        mbox.lock.lock(true);
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        mbox.lock.release();
                    } catch (ServiceException e) {
                    }
                }
            };
            waitThread.setDaemon(true);
            waitThreads.add(waitThread);
        }

        int readThreadCount = 20;
        final Set<Thread> readThreads = new HashSet<Thread>();
        for (int i = 0; i < readThreadCount; i++) {
            Thread readThread = new Thread("MailboxLockTest-Reader-"+i) {
                @Override
                public void run() {
                    Mailbox mbox;
                    try {
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                        int holdCount = 20;
                        for (int i = 0; i < holdCount; i++) {
                            mbox.lock.lock(false);
                        }
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        for (int i = 0; i < holdCount; i++) {
                            mbox.lock.release();
                        }
                    } catch (ServiceException e) {
                    }
                }
            };
            readThreads.add(readThread);
        }


        Thread lastReadThread = new Thread("MailboxLockTest-LastReader") {
            @Override
            public void run() {
                Mailbox mbox;
                try {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    int holdCount = 20;
                    for (int i = 0; i < holdCount; i++) {
                        mbox.lock.lock(false);
                    }
                    //this thread starts the waiters
                    //and the other readers
                    for (Thread reader : readThreads) {
                        reader.start();
                    }
                    for (Thread waiter : waitThreads) {
                        waiter.start();
                    }
                    while (!done.get()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    for (int i = 0; i < holdCount; i++) {
                        mbox.lock.release();
                    }
                } catch (ServiceException e) {
                }
            }
        };

        lastReadThread.start();

        while (((LocalMailboxLock)mbox.lock).getQueueLength() < ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraMailboxLockMaxWaitingThreads, 15)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        try {
            mbox.lock.lock(false); //one more reader...this should give too many waiters
            Assert.fail("expected too many waiters");
        } catch (LockFailedException e) {
            //expected
            Assert.assertTrue(e.getMessage().startsWith("too many waiters"));
            done.set(true); //cause writer to finish
        }

        long joinTimeout = 50000;
        joinWithTimeout(lastReadThread, joinTimeout);
        for (Thread t: readThreads) {
            joinWithTimeout(t, joinTimeout);
        }
        for (Thread t: waitThreads) {
            joinWithTimeout(t, joinTimeout);
        }

        //now do a write lock in same thread. previously this would break due to read assert not clearing
        mbox.lock.lock(true);
        mbox.lock.release();
    }


    @Configuration
    public static class LocalZimbraConfig extends ZimbraConfig {

        @Override
        public boolean isRedisAvailable() throws ServiceException {
            return false;
        }
    }
}
