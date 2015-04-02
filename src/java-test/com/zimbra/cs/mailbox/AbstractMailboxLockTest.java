/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.util.ItemId;

public abstract class AbstractMailboxLockTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(isLockServiceAvailableForTest());
        MailboxTestUtil.clearData();
        try {
            resetStoreBetweenTests();
        } catch (Exception e) {}
    }

    protected abstract void resetStoreBetweenTests() throws Exception;

    protected abstract boolean isLockServiceAvailableForTest() throws Exception;

    @Test(timeout=10000)
    public void expectAssertIfWriteRequestedWhileHoldingRead() throws ServiceException {
        boolean assertsEnabled = false;
        assert (assertsEnabled = true);
        if (assertsEnabled) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
            mbox.lock.lock(false);
            Assert.assertFalse(mbox.lock.isUnlocked());
            Assert.assertFalse(mbox.lock.isWriteLockedByCurrentThread());
            try {
                mbox.lock.lock(true);
                Assert.fail("Expected assert after request for write lock after read lock already acquired");
            } catch (AssertionError e) {
                //expected
            }
        } else {
            ZimbraLog.test.debug("skipped testWriteWhileHoldingRead since asserts are not enabled");
            //without this the test times out eventually, but we want tests to be fast so skip this one
        }
    }

    @Test(timeout=10000)
    public void singleProcessNestedWrite() throws ServiceException {
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

    @Test(timeout=11000)
    public void singleProcessMultiThreadedAccess() throws ServiceException {
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
                            mbox.getFolderTree(null, iid, true);
                        } catch (ServiceException e) {
                            ZimbraLog.test.error(e.getLocalizedMessage(), e);
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
                            ZimbraLog.test.error(e.getLocalizedMessage(), e);
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
}
