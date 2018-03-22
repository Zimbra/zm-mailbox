/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZLocalMailboxLock;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.LockFailedException;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.DistributedMailboxLockFactory;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.service.util.ItemId;


public class TestMailboxLock {
    @Rule
    public TestName testInfo = new TestName();
    private static String USER_NAME = "testmailboxlock";

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtil.createAccount(USER_NAME);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    public void writeWhileHoldingRead() throws ServiceException {
        final Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        try (final MailboxLock l = mbox.getReadLockAndLockIt()) {
            assertFalse("isUnlocked", l.isUnlocked());
            assertFalse("isWriteLockedByCurrentThread", l.isWriteLockedByCurrentThread());
            assertEquals("hold count", 1, l.getHoldCount());
            try (final MailboxLock l2 = mbox.getWriteLockAndLockIt()) {
                assertFalse("isUnlocked", l2.isUnlocked());
                assertTrue("isWriteLock", l2.isWriteLock());
                assertEquals("hold count", 2, l2.getHoldCount());
            }
        } catch (LockFailedException lfe) {
            ZimbraLog.test.info("Unexpected exception thrown", lfe);
            fail("Locking for write when this thread already has a read lock should be allowed - " +
                lfe.getMessage());
        }
    }

    @Test
    public void simpleNestedWrite() throws Exception {
        final Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        try (final MailboxLock l1 = mbox.getWriteLockAndLockIt()) {
            assertFalse("isUnlocked", l1.isUnlocked());
            assertTrue("isWriteLock", l1.isWriteLock());
            assertEquals("hold count", 1, l1.getHoldCount());
            try (final MailboxLock l2 = mbox.getWriteLockAndLockIt()) {
                assertFalse("isUnlocked", l2.isUnlocked());
                assertTrue("isWriteLock", l2.isWriteLock());
                assertEquals("hold count", 2, l2.getHoldCount());
            }
        }
    }

    @Test
    public void simpleNestedRead() throws Exception {
        final Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        try (final MailboxLock l1 = mbox.getWriteLockAndLockIt()) {
            assertFalse("isUnlocked", l1.isUnlocked());
            assertTrue("isWriteLock", l1.isWriteLock());
            assertEquals("hold count", 1, l1.getHoldCount());
            try (final MailboxLock l2 = mbox.getReadLockAndLockIt()) {
                assertFalse("isUnlocked", l2.isUnlocked());
                assertTrue("isWriteLock", l2.isWriteLock());
                assertEquals("hold count", 2, l2.getHoldCount());
            }
        }
    }

    @Test
    public void nestedWrite() throws ServiceException {
        final Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        try (final MailboxLock l1 = mbox.getWriteLockAndLockIt()) {
            assertEquals("hold count", 1, l1.getHoldCount());
            assertFalse("isUnlocked", l1.isUnlocked());
            assertTrue("isWriteLockedByCurrentThread", l1.isWriteLockedByCurrentThread());
            try (final MailboxLock l2 = mbox.getWriteLockAndLockIt()) {
                assertEquals("hold count", 2, l1.getHoldCount());
                try (final MailboxLock l3 = mbox.getWriteLockAndLockIt()) {
                    assertEquals("hold count", 3, l1.getHoldCount());
                    try (final MailboxLock l4 = mbox.getReadLockAndLockIt()) {
                        assertEquals("hold count", 4, l4.getHoldCount());
                        try (final MailboxLock l5 = mbox.getWriteLockAndLockIt()) {
                            assertEquals("hold count", 5, l1.getHoldCount());
                            try (final MailboxLock l6 = mbox.getWriteLockAndLockIt()) {
                                assertEquals("hold count", 6, l1.getHoldCount());
                                try (final MailboxLock l7 = mbox.getWriteLockAndLockIt()) {
                                    assertEquals("hold count", 7, l1.getHoldCount());
                                }
                                assertEquals("hold count", 6, l1.getHoldCount());
                            }
                            assertEquals("hold count", 5, l1.getHoldCount());
                        }
                        assertEquals("hold count", 4, l1.getHoldCount());
                    }
                    assertEquals("hold count", 3, l1.getHoldCount());
                }
                assertEquals("hold count", 2, l1.getHoldCount());
            }
            assertEquals("hold count", 1, l1.getHoldCount());
        }
    }

    @Test
    public void multiAccess() throws ServiceException {
        final Mailbox mbox = TestUtil.getMailbox(USER_NAME);

        //just do some read/write in different threads to see if we trigger any deadlocks or other badness
        int numThreads = 5;
        final int loopCount = 10;
        final long sleepTime = 10;
        int joinTimeout = 10000;

        List<Thread> threads = new ArrayList<Thread>(numThreads * 2);
        for (int i = 0; i < numThreads; i++) {
            String threadName = "TestMailboxLock-MultiReader-" + i;
            Thread reader = new Thread(threadName) {
                @Override
                public void run() {
                    for (int i = 0; i < loopCount; i++) {
                        try (final MailboxLock l = mbox.getReadLockAndLockIt()) {
                        try {
                            ItemId iid = new ItemId(mbox, Mailbox.ID_FOLDER_USER_ROOT);
                            FolderNode node = mbox.getFolderTree(null, iid, true);
                        } catch (ServiceException e) {
                            e.printStackTrace();
                            fail("ServiceException");
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

            threadName = "TestMailboxLock-MultiWriter-" + i;
            Thread writer = new Thread(threadName) {
                @Override
                public void run() {
                    for (int i = 0; i < loopCount; i++) {
                        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
                            try {
                                mbox.createFolder(null, "foo-" + Thread.currentThread().getName() + "-" + i,
                                        new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
                            } catch (ServiceException e) {
                                e.printStackTrace();
                                fail("ServiceException");
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
                assertFalse("thread is alive", t.isAlive());
            } catch (InterruptedException e) {
            }
        }
    }

    @Test
    public void promote() {
        final Thread readThread = new Thread("TestMailboxLock-Reader") {
            @Override
            public void run() {
                Mailbox mbox;
                List<MailboxLock> listLocks = new ArrayList<>();
                MailboxLock l = null;
                int lockCount = 10;
                try {
                    mbox = TestUtil.getMailbox(USER_NAME);
                    //here's the interleaving we are explicitly exercising in this test
                    //1. writer - mbox.lock(write)
                    //2. reader - mbox.lock(read); call gets past the initial isWriteModeRequired() check and into tryLock(read)
                    //3. writer - mbox.purge()
                    //4. writer - mbox.unlock()
                    //5. reader - tryLock(read) returns, then recheck isWriteModeRequired() and promote
                    // not possible to call isUnlocked at this point
                    //assertTrue(mbox.lock.isUnlocked());
                    for (int i = 0; i < lockCount; i++) {
                        listLocks.add(l = mbox.getReadLockAndLockIt());
                        //loop so we exercise recursion in promote..
                    }
                    assertTrue("isWriteLockedByCurrentThread", l.isWriteLockedByCurrentThread());
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
                    fail();
                } finally {
                    for (int i = 0; i < listLocks.size(); i++) {
                        listLocks.get(i).close();
                    }
                }
            }
        };
        readThread.setDaemon(true);

        final Thread writeThread = new Thread("TestMailboxLock-Writer") {

            @Override
            public void run() {
                Mailbox mbox;
                try {
                    mbox = TestUtil.getMailbox(USER_NAME);
                    try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
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
                    fail();
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
            assertFalse("thread is alive", writeThread.isAlive());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            readThread.join(joinTimeout);
            assertFalse("thread is alive", readThread.isAlive());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void joinWithTimeout(Thread thread, long timeout) {
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
        }
        assertFalse("thread is alive", thread.isAlive());
    }

    @Test
    public void tooManyWaiters() {
        Mailbox mbox = null;
        try {
            mbox = TestUtil.getMailbox(USER_NAME);
        } catch (ServiceException e) {
            fail("Failed to get mailbox " + e.getMessage());
        }

        int threads = LC.zimbra_mailbox_lock_max_waiting_threads.intValue();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Set<Thread> waitThreads = new HashSet<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread waitThread = new Thread("TestMailboxLock-Waiter") {
                @Override
                public void run() {
                    Mailbox mbox;
                    try {
                        mbox = TestUtil.getMailbox(USER_NAME);
                        try (final MailboxLock l = mbox.getReadLockAndLockIt()) {
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

        Thread writeThread = new Thread("TestMailboxLock-Writer") {
            @Override
            public void run() {
                Mailbox mbox;
                try {
                    mbox = TestUtil.getMailbox(USER_NAME);
                    try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
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

        while (((DistributedMailboxLockFactory)mbox.lockFactory).getQueueLength()
                < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        try {
            // one more reader...this should give too many waiters
            try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
                fail("expected too many waiters");
            }
        } catch (LockFailedException e) {
            //expected
            assertTrue("Lock failed because too many waiters", e.getMessage().startsWith("too many waiters"));
            done.set(true); //cause writer to finish
        }

        long joinTimeout = 50000;
        joinWithTimeout(writeThread, joinTimeout);
        for (Thread t: waitThreads) {
            joinWithTimeout(t, joinTimeout);
        }

        //now do a write lock in same thread. previously this would break due to read assert not clearing
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
        }
    }

    @Test
    public void tooManyWaitersWithSingleReadOwner() {
        Mailbox mbox = null;
        try {
            mbox = TestUtil.getMailbox(USER_NAME);
        } catch (ServiceException e) {
            fail();
        }

        int threads = LC.zimbra_mailbox_lock_max_waiting_threads.intValue();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Set<Thread> waitThreads = new HashSet<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread waitThread = new Thread("TestMailboxLock-Waiter") {
                @Override
                public void run() {
                    Mailbox mbox;
                    try {
                        mbox = TestUtil.getMailbox(USER_NAME);
                        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
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

        Thread readThread = new Thread("TestMailboxLock-Reader") {
            @Override
            public void run() {
                Mailbox mbox;
                List<MailboxLock> listLocks = new ArrayList<>();
                MailboxLock l = null;
                try {
                    mbox = TestUtil.getMailbox(USER_NAME);
                    int holdCount = 20;
                    for (int i = 0; i < holdCount; i++) {
                        listLocks.add(mbox.getReadLockAndLockIt());
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

        while (((DistributedMailboxLockFactory)mbox.lockFactory).getQueueLength()  < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

            // one more reader...this should give too many waiters
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            fail("expected too many waiters");
        } catch (LockFailedException e) {
            //expected
            assertTrue("Lock failed because too many waiters", e.getMessage().startsWith("too many waiters"));
            done.set(true); //cause writer to finish
        }

        long joinTimeout = 50000;
        joinWithTimeout(readThread, joinTimeout);
        for (Thread t: waitThreads) {
            joinWithTimeout(t, joinTimeout);
        }

        //now do a write lock in same thread. previously this would break due to read assert not clearing
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
        }
    }

    @Test
    public void tooManyWaitersWithMultipleReadOwners() {
        Mailbox mbox = null;
        try {
            mbox = TestUtil.getMailbox(USER_NAME);
        } catch (ServiceException e) {
            fail();
        }

        int threads = LC.zimbra_mailbox_lock_max_waiting_threads.intValue();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Set<Thread> waitThreads = new HashSet<Thread>();
        for (int i = 0; i < threads; i++) {
            Thread waitThread = new Thread("TestMailboxLock-Waiter-"+i) {
                @Override
                public void run() {
                    Mailbox mbox;
                    try {
                        mbox = TestUtil.getMailbox(USER_NAME);
                        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
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
            Thread readThread = new Thread("TestMailboxLock-Reader-"+i) {
                @Override
                public void run() {
                    Mailbox mbox;
                    List<MailboxLock> listLocks = new ArrayList<>();
                    try {
                        mbox = TestUtil.getMailbox(USER_NAME);
                        int holdCount = 20;
                        for (int i = 0; i < holdCount; i++) {
                            listLocks.add(mbox.getReadLockAndLockIt());
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


        Thread lastReadThread = new Thread("TestMailboxLock-LastReader") {
            @Override
            public void run() {
                Mailbox mbox;
                List<MailboxLock> listLocks = new ArrayList<>();
                try {
                    mbox = TestUtil.getMailbox(USER_NAME);
                    int holdCount = 20;
                    for (int i = 0; i < holdCount; i++) {
                        listLocks.add(mbox.getReadLockAndLockIt());
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

        while (((DistributedMailboxLockFactory)mbox.lockFactory).getQueueLength() < LC.zimbra_mailbox_lock_max_waiting_threads.intValue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        //one more reader...this should give too many waiters
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            fail("expected too many waiters");
        } catch (LockFailedException e) {
            //expected
            assertTrue("Lock failed because too many waiters", e.getMessage().startsWith("too many waiters"));
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
        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
        }
    }

    @Test
    public void testZMailboxReenter() throws Exception {
        ZLocalMailboxLock lock = new ZLocalMailboxLock(1, 1);
        for (int i = 0; i < 3; i++) {
            lock.lock();
        }
        assertEquals("hold count", 3, lock.getHoldCount());
        for (int i = 0; i < 3; i++) {
            lock.close();
        }
        assertEquals("hold count", 0, lock.getHoldCount());
    }

    @Test
    public void testZMailboxLockTimeout() throws Exception {
        int maxNumThreads = 3;
        int timeout = 0;
        ZLocalMailboxLock lock = new ZLocalMailboxLock(maxNumThreads, timeout);
        Thread thread = new Thread(String.format("TestMailboxLock-ZMailbox")) {
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
            fail("should not be able to acquire the lock; should time out");
        } catch (LockFailedException e) {
            assertTrue("Lock failed due to timeout", e.getMessage().startsWith("lock timeout"));
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
            Thread thread = new Thread(String.format("TestMailboxLock-ZMailbox-%s", i)) {
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
            fail("should not be able to acquire lock due to too many waiting threads");
        } catch (LockFailedException e) {
            assertTrue("Lock failed because too many waiters", e.getMessage().startsWith("too many waiters"));
        }
        for (Thread t: threads) {
            t.join();
        }
    }
}
