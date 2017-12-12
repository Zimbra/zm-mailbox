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

import com.zimbra.client.ZLocalMailboxLock;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.LocalMailboxLockFactory.LockFailedException;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
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
            try (final MailboxLock l = mbox.lock(false)) {
                l.lock();
            Assert.assertFalse(l.isUnlocked());
            Assert.assertFalse(l.isWriteLockedByCurrentThread());            
            boolean good = true;
            try {
                final MailboxLock l2 = mbox.lock(true);
                    l2.lock();
                good = false;
            } catch (AssertionError e) {
                //expected
            }
            Assert.assertTrue(good);            
            }
        } else {
            ZimbraLog.test.debug("skipped testWriteWhileHoldingRead since asserts are not enabled");
            //without this the test times out eventually, but we want tests to be fast so skip this one
        }
    }

    @Test
    public void nestedWrite() throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int holdCount = 0;
        // at this point is no possible to call getHoldCount, we need a lock reference first
        //Assert.assertEquals(holdCount, mbox.lock.getHoldCount());
        final MailboxLock l1 = mbox.lock(true);
        try {
            l1.lock();
            holdCount++;
            Assert.assertEquals(holdCount, l1.getHoldCount());
            Assert.assertFalse(l1.isUnlocked());
            Assert.assertTrue(l1.isWriteLockedByCurrentThread());
            try (final MailboxLock l2 = mbox.lock(false)) {
                l2.lock();
                holdCount++;
                Assert.assertEquals(holdCount, l1.getHoldCount());
                try (final MailboxLock l3 = mbox.lock(true)) {
                    l3.lock();
                    holdCount++;
                    Assert.assertEquals(holdCount, l1.getHoldCount());
                    try (final MailboxLock l4 = mbox.lock(false)) {
                        l4.lock();
                        holdCount++;
                        Assert.assertEquals(holdCount, l1.getHoldCount());
                        try (final MailboxLock l5 = mbox.lock(true)) {
                            l5.lock();
                            holdCount++;
                            Assert.assertEquals(holdCount, l1.getHoldCount());
                            try (final MailboxLock l6 = mbox.lock(true)) {
                                l6.lock();
                                holdCount++;
                                Assert.assertEquals(holdCount, l1.getHoldCount());
                                try (final MailboxLock l7 = mbox.lock(true)) {
                                    l7.lock();
                                    holdCount++;
                                    Assert.assertEquals(holdCount, l1.getHoldCount());
                                }
                                holdCount--;
                                Assert.assertEquals(holdCount, l1.getHoldCount());
                            }
                            holdCount--;
                            Assert.assertEquals(holdCount, l1.getHoldCount());
                        }
                        holdCount--;
                        Assert.assertEquals(holdCount, l1.getHoldCount());
                    }
                    holdCount--;
                    Assert.assertEquals(holdCount, l1.getHoldCount());
                }
                holdCount--;
                Assert.assertEquals(holdCount, l1.getHoldCount());
            }
            holdCount--;
            Assert.assertEquals(holdCount, l1.getHoldCount());
        } finally {
            l1.lock();
            holdCount--;
            Assert.assertEquals(holdCount, l1.getHoldCount());
            Assert.assertEquals(0, holdCount);
        }
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
                        try (final MailboxLock l = mbox.lock(false)) {
                            l.lock();
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
                        }
                    }
                }
            };
            threads.add(reader);

            threadName = "MailboxLockTest-MultiWriter-" + i;
            Thread writer = new Thread(threadName) {
                @Override
                public void run() {
                    for (int i = 0; i < loopCount; i++) {
                        try (final MailboxLock l = mbox.lock(true)) {
                            l.lock();
                        try {
                            mbox.createFolder(null, "foo-" + Thread.currentThread().getName() + "-" + i, new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
                        } catch (ServiceException e) {
                            e.printStackTrace();
                            Assert.fail("ServiceException");
                        }
                        }
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
                List<MailboxLock> listLocks = new ArrayList<>();
                MailboxLock l = null;
                try {
                    int lockCount = 10;
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    //here's the interleaving we are explicitly exercising in this test
                    //1. writer - mbox.lock(write)
                    //2. reader - mbox.lock(read); call gets past the initial isWriteModeRequired() check and into tryLock(read)
                    //3. writer - mbox.purge()
                    //4. writer - mbox.unlock()
                    //5. reader - tryLock(read) returns, then recheck isWriteModeRequired() and promote
					// not possible to call isUnlocked at this point
                    //Assert.assertTrue(mbox.lock.isUnlocked());
                    for (int i = 0; i < lockCount; i++) {
						listLocks.add(l = mbox.lock(false));
						l.lock();
                        //loop so we exercise recursion in promote..
                    }
                    Assert.assertTrue(l.isWriteLockedByCurrentThread());
                    //we're guaranteeing that reader lock is not held before writer
                    //but not guaranteeing that purge is called while reader is waiting
                    //i.e. if purge/release happens in writeThread before we actually get to lock call in this thread
                    //subtle, and shouldn't matter since promote is called either way, but if we see races in test this could be cause
                    mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
                    for (int i = 0; i < lockCount; i++) {
						listLocks.get(i).close();
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
					try (final MailboxLock l = mbox.lock(true)) {
						l.lock();
                    //start read thread only after holding mailbox lock
                    readThread.start();
                    //wait until read thread has tried to obtain mailbox lock
                    //hasQueuedThreads method isn't available
                    //while (!l.hasQueuedThreads()) {
                    //    Thread.sleep(10);
                    //}
                    mbox.purge(MailItem.Type.FOLDER);
                    }
                } catch (ServiceException /*| InterruptedException*/ e) {
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
                        try (final MailboxLock l = mbox.lock(false)) {
                            l.lock();
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        }
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
                    try (final MailboxLock l = mbox.lock(true)) {
                        l.lock();
                        for (Thread waiter : waitThreads) {
                            waiter.start();
                        }
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                } catch (ServiceException e) {
                }
            }
        };

        writeThread.start();

        while (((LocalMailboxLockFactory)mbox.lockFactory).getQueueLength() < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        try {
            // one more reader...this should give too many waiters
            try (final MailboxLock l = mbox.lock(true)) {
                l.lock();
                Assert.fail("expected too many waiters");
            }
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
        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
        }
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
                        try (final MailboxLock l = mbox.lock(true)) {
                            l.lock();
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        }
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
                List<MailboxLock> listLocks = new ArrayList<>();
                MailboxLock l = null;
                try {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    int holdCount = 20;
                    for (int i = 0; i < holdCount; i++) {
						listLocks.add(l = mbox.lock(false));
						l.lock();
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
						listLocks.get(i).close();
                    }
                } catch (ServiceException e) {
                }
            }
        };

        readThread.start();

        while (((LocalMailboxLockFactory)mbox.lockFactory).getQueueLength()  < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        try {
            final MailboxLock l = mbox.lock(false); // one more reader...this should give too many waiters
            l.lock();
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
        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
        }
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
                        try (final MailboxLock l = mbox.lock(true)) {
                            l.lock();
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        }
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
                    List<MailboxLock> listLocks = new ArrayList<>();
                    MailboxLock l = null;
                    try {
                        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                        int holdCount = 20;
                        for (int i = 0; i < holdCount; i++) {
                            listLocks.add(l = mbox.lock(false));
                            l.lock();
                        }
                        while (!done.get()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        for (int i = 0; i < holdCount; i++) {
                            listLocks.get(i).close();
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
                List<MailboxLock> listLocks = new ArrayList<>();
                MailboxLock l = null;
                try {
                    mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
                    int holdCount = 20;
                    for (int i = 0; i < holdCount; i++) {
                        listLocks.add(l = mbox.lock(false));
                        l.lock();
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
                        listLocks.get(i).close();
                    }
                } catch (ServiceException e) {
                }
            }
        };

        lastReadThread.start();

        while (((LocalMailboxLockFactory)mbox.lockFactory).getQueueLength() < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        try {
            final MailboxLock l = mbox.lock(false); //one more reader...this should give too many waiters
            l.lock();
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
        try (final MailboxLock l = mbox.lock(true)) {
            l.lock();
        }
    }

    @Test
    public void testZMailboxReenter() throws Exception {
        ZLocalMailboxLock lock = new ZLocalMailboxLock(1, 1);
        for (int i = 0; i < 3; i++) {
            lock.lock();
        }
        Assert.assertEquals(3, lock.getHoldCount());
        for (int i = 0; i < 3; i++) {
            lock.close();
        }
        Assert.assertEquals(0, lock.getHoldCount());
    }

    @Test
    public void testZMailboxLockTimeout() throws Exception {
        int maxNumThreads = 3;
        int timeout = 0;
        ZLocalMailboxLock lock = new ZLocalMailboxLock(maxNumThreads, timeout);
        Thread thread = new Thread(String.format("MailboxLockTest-ZMailbox")) {
            @Override
            public void run() {
                lock.lock();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.close();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(100);
        try {
            lock.lock();
            Assert.fail("should not be able to acquire the lock; should time out");
        } catch (com.zimbra.client.ZLocalMailboxLock.LockFailedException e) {
            Assert.assertTrue(e.getMessage().startsWith("lock timeout"));
        }
        thread.join();
    }

    @Test
    public void testZMailboxLockTooManyWaiters() throws Exception {
        int maxNumThreads = 3;
        int timeout = 10;
        ZLocalMailboxLock lock = new ZLocalMailboxLock(maxNumThreads, timeout);
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
                    lock.close();
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
        } catch (com.zimbra.client.ZLocalMailboxLock.LockFailedException e) {
            Assert.assertTrue(e.getMessage().startsWith("too many waiters"));
        }
        for (Thread t: threads) {
            t.join();
        }
    }
}
