/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.Zimbra;

/**
 * Shared unit tests for {@link SharedDeliveryCoordinator} adapters.
 */
public abstract class AbstractSharedDeliveryCoordinatorTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(isExternalCacheAvailableForTest());
        MailboxTestUtil.clearData();
        MailboxTestUtil.cleanupIndexStore(
                MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID));
        try {
            flushCacheBetweenTests();
        } catch (Exception e) {}
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    protected abstract void flushCacheBetweenTests() throws Exception;

    protected abstract boolean isExternalCacheAvailableForTest() throws Exception;

    @Test
    public void testInitState() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        Assert.assertNotNull(sdc);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertTrue(sdc.isSharedDeliveryComplete(mbox));
        Assert.assertTrue(sdc.isSharedDeliveryAllowed(mbox));
    }

    @Test
    public void testToggleAllowedFlag() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // Identity (flip default true to true)
        Assert.assertTrue(sdc.isSharedDeliveryAllowed(mbox));
        sdc.setSharedDeliveryAllowed(mbox, true);
        Assert.assertTrue(sdc.isSharedDeliveryAllowed(mbox));

        // Toggle (flip default true to new false)
        Assert.assertTrue(sdc.isSharedDeliveryAllowed(mbox));
        sdc.setSharedDeliveryAllowed(mbox, false);
        Assert.assertEquals(false, sdc.isSharedDeliveryAllowed(mbox));
    }

    @Test
    public void testSingleSharedDelivery() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Assert.assertTrue(sdc.beginSharedDelivery(mbox));
        Assert.assertEquals(false, sdc.isSharedDeliveryComplete(mbox));

        sdc.endSharedDelivery(mbox);
        Assert.assertTrue(sdc.isSharedDeliveryComplete(mbox));
    }

    @Test
    public void testNestedSharedDelivery() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Assert.assertTrue(sdc.beginSharedDelivery(mbox));
        Assert.assertEquals(false, sdc.isSharedDeliveryComplete(mbox));

        Assert.assertTrue(sdc.beginSharedDelivery(mbox));
        Assert.assertEquals(false, sdc.isSharedDeliveryComplete(mbox));

        sdc.endSharedDelivery(mbox);
        Assert.assertFalse(sdc.isSharedDeliveryComplete(mbox));

        sdc.endSharedDelivery(mbox);
        Assert.assertTrue(sdc.isSharedDeliveryComplete(mbox));
    }

    @Test(timeout=6000)
    public void testWait() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Assert.assertTrue(sdc.beginSharedDelivery(mbox));
        Assert.assertEquals(false, sdc.isSharedDeliveryComplete(mbox));

        WaitOnceThread waitOnceThread = new WaitOnceThread(mbox, sdc);
        waitOnceThread.start();

        Thread.sleep(100); // cause at lease one wait

        sdc.endSharedDelivery(mbox);
        Assert.assertTrue(sdc.isSharedDeliveryComplete(mbox));

        // leave it up to the JUnit test timeout to fail the test if the waiting thread doesn't fall out of
        // its run loop within the test timeout
        waitOnceThread.join();
    }


    class WaitOnceThread extends Thread
    {
        Mailbox mbox;
        SharedDeliveryCoordinator sdc;

        WaitOnceThread(Mailbox mbox, SharedDeliveryCoordinator sdc) {
            this.mbox = mbox;
            this.sdc = sdc;
        }

        @Override
        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                ZimbraLog.test.debug("About to wait for completion of shared delivery");
                sdc.waitUntilSharedDeliveryCompletes(mbox);
                long elapsedTime = System.currentTimeMillis() - startTime;
                ZimbraLog.test.debug("Done waiting for completion of shared delivery after %dms", elapsedTime);
            } catch (ServiceException e) {
                ZimbraLog.test.error("Error while waiting for completion of shared delivery", e);
            }
        }
    }
}
