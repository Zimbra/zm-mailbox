/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.client.ZMailboxLock;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.MailboxLock.LockFailedException;
import com.zimbra.cs.service.util.ItemId;

public class MailboxLockTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setup() throws Exception {
        MailboxTestUtil.clearData();
        MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
    }

    @Test
    public void badWriteWhileHoldingRead() throws ServiceException {
        boolean check = false;
        assert (check = true);
        if (check) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
            mbox.lock.lock(false);
            Assert.assertFalse(mbox.lock.isUnlocked());
            Assert.assertFalse(mbox.lock.isWriteLockedByCurrentThread());
            boolean good = true;
            try {
                mbox.lock.lock(true);
                good = false;
            } catch (AssertionError e) {
                //expected
            }
            Assert.assertTrue(good);
        } else {
            ZimbraLog.test.debug("skipped testWriteWhileHoldingRead since asserts are not enabled");
            //without this the test times out eventually, but we want tests to be fast so skip this one
        }
    }

    @Test
    public void nestedWrite() throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int holdCount = 0;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.lock(true);
        holdCount++;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        Assert.assertFalse(mbox.lock.isUnlocked());
        Assert.assertTrue(mbox.lock.isWriteLockedByCurrentThread());
        mbox.lock.lock(false);
        holdCount++;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.lock(true);
        holdCount++;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.lock(false);
        holdCount++;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.lock(true);
        holdCount++;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.lock(true);
        holdCount++;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.lock(true);
        holdCount++;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());

        mbox.lock.release();
        holdCount--;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.release();
        holdCount--;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.release();
        holdCount--;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.release();
        holdCount--;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.release();
        holdCount--;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.release();
        holdCount--;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        mbox.lock.release();
        holdCount--;
        Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        Assert.assertEquals(0, holdCount);
    }

    @Test
    public void multiAccess() throws ServiceException {
        final Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        //just do some read/write in different threads to see if we trigger any deadlocks or other badness
        int numThreads = 5;
        final int loopCount = 10;
        final long sleepTime = 10;
        int joinTimeout = 10000;

        List<Thread> threads = new ArrayList<Thread>(numThreads * 2);
        for (int i = 0; i < numThreads; i++) {
            String threadName = "MailboxLockTest-MultiReader-" + i;
            Thread reader = new Thread(threadName) {
                @Override
                public void run() {
                    for (int i = 0; i < loopCount; i++) {
                        mbox.lock.lock(false);
                        try {
                            ItemId iid = new ItemId(mbox, Mailbox.ID_FOLDER_USER_ROOT);
                            FolderNode node = mbox.getFolderTree(null, iid, true);
                        } catch (ServiceException e) {
                            e.printStackTrace();
                            Assert.fail("ServiceException");
                        }
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                        }
                        mbox.lock.release();
                    }
                }
            };
            threads.add(reader);

            threadName = "MailboxLockTest-MultiWriter-" + i;
            Thread writer = new Thread(threadName) {
                @Override
                public void run() {
                    for (int i = 0; i < loopCount; i++) {
                        mbox.lock.lock(true);
                        try {
                            mbox.createFolder(null, "foo-" + Thread.currentThread().getName() + "-" + i, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
                        } catch (ServiceException e) {
                            e.printStackTrace();
                            Assert.fail("ServiceException");
                        }
                        mbox.lock.release();
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            };
            threads.add(writer);
//            writer.start();
//            reader.start();
        }

        for (Thread t : threads){
            t.start();
        }
        for (Thread t : threads) {
            try {
                t.join(joinTimeout);
                Assert.assertFalse(t.isAlive());
            } catch (InterruptedException e) {
            }
        }
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
                    e.printStackTrace();
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
                    while (!mbox.lock.hasQueuedThreads()) {
                        Thread.sleep(10);
                    }
                    mbox.purge(MailItem.Type.FOLDER);
                    mbox.lock.release();
                } catch (ServiceException | InterruptedException e) {
                    e.printStackTrace();
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
                System.out.println("Write Thread");
                for (StackTraceElement ste : writeThread.getStackTrace()) {
                    System.out.println(ste);
                }
                if (readThread.isAlive()) {
                    System.out.println("Read Thread");
                    for (StackTraceElement ste : readThread.getStackTrace()) {
                        System.out.println(ste);
                    }
                }
            }
            Assert.assertFalse(writeThread.isAlive());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            readThread.join(joinTimeout);
            Assert.assertFalse(readThread.isAlive());
        } catch (InterruptedException e) {
            e.printStackTrace();
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

        int threads = LC.zimbra_mailbox_lock_max_waiting_threads.intValue();
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

        while (mbox.lock.getQueueLength() < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
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

        int threads = LC.zimbra_mailbox_lock_max_waiting_threads.intValue();
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

        while (mbox.lock.getQueueLength() < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
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

        int threads = LC.zimbra_mailbox_lock_max_waiting_threads.intValue();
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

        while (mbox.lock.getQueueLength() < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
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

    @Test
    public void testZMailboxReenter() throws Exception {
        ZMailboxLock lock = new ZMailboxLock(1, 1);
        for (int i = 0; i < 3; i++) {
            lock.lock();
        }
        Assert.assertEquals(3, lock.getHoldCount());
        for (int i = 0; i < 3; i++) {
            lock.release();
        }
        Assert.assertEquals(0, lock.getHoldCount());
    }

    @Test
    public void testZMailboxLockTimeout() throws Exception {
        int maxNumThreads = 3;
        int timeout = 0;
        ZMailboxLock lock = new ZMailboxLock(maxNumThreads, timeout);
        Thread thread = new Thread(String.format("MailboxLockTest-ZMailbox")) {
            @Override
            public void run() {
                lock.lock();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.release();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(100);
        try {
            lock.lock();
            Assert.fail("should not be able to acquire the lock; should time out");
        } catch (com.zimbra.client.ZMailboxLock.LockFailedException e) {
            Assert.assertTrue(e.getMessage().startsWith("lock timeout"));
        }
        thread.join();
    }

    @Test
    public void testZMailboxLockTooManyWaiters() throws Exception {
        int maxNumThreads = 3;
        int timeout = 10;
        ZMailboxLock lock = new ZMailboxLock(maxNumThreads, timeout);
        final Set<Thread> threads = new HashSet<Thread>();
        for (int i = 0; i < maxNumThreads + 1; i++) {
            // one thread will acquire the lock, 3 will wait
            Thread thread = new Thread(String.format("MailboxLockTest-ZMailbox-%s", i)) {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    lock.release();
                }
            };
            thread.setDaemon(true);
            threads.add(thread);
        }
        for (Thread t: threads) {
            t.start();
        }
        Thread.sleep(100);
        try {
            lock.lock();
            Assert.fail("should not be able to acquire lock due to too many waiting threads");
        } catch (com.zimbra.client.ZMailboxLock.LockFailedException e) {
            Assert.assertTrue(e.getMessage().startsWith("too many waiters"));
        }
        for (Thread t: threads) {
            t.join();
        }
    }
}
