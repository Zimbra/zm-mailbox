/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexInfo;
import com.zimbra.cs.index.IndexingQueueAdapter;
import com.zimbra.cs.index.IndexingService;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ReIndexStatus;
import com.zimbra.cs.service.admin.ReIndex;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for ReIndex admin operation.
 * <p>
 * This test requires a Zimbra dev server instance.
 * @author ysasaki
 * @author Greg Solovyev
 */
public class TestReIndex {
    private static final String NAME_PREFIX = TestReIndex.class.getSimpleName();
    private static final String RECIPIENT1 = NAME_PREFIX + "user1";
    private static final String RECIPIENT2 = NAME_PREFIX + "user2";
    private static final String RECIPIENT3 = NAME_PREFIX + "user3";
    private boolean originalLCSetting = false;

    @Test
    public void testStartReindex() throws Exception {
        // shut down the service so it does not finish before we can check its
        // status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();

        // add some messages
        Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
        TestUtil.addMessage(recieverMbox, NAME_PREFIX);
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), String.format("subject:%s", NAME_PREFIX));

        // check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT1);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        ReIndexInfo.Progress progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_IDLE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());

        // kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_RUNNING, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_STARTED, info.getStatus());
    }

    @Test
    public void testAbortReindex() throws Exception {
        // shut down the service so it does not finish before we can check its
        // status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();

        // add some messages
        Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
        TestUtil.addMessage(recieverMbox, NAME_PREFIX);
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), String.format("subject:%s", NAME_PREFIX));

        // check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT1);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        ReIndexInfo.Progress progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_IDLE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());

        // kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_RUNNING, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_STARTED, info.getStatus());

        info = prov.reIndex(account, "cancel", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_ABORTED, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_CANCELLED, info.getStatus());
    }

    @Test
    public void testAbortRestartReindex() throws Exception {
        // shut down the service so it does not finish before we can check its
        // status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();

        // add some messages
        Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
        TestUtil.addMessage(recieverMbox, NAME_PREFIX);
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), String.format("subject:%s", NAME_PREFIX));

        // check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT1);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        ReIndexInfo.Progress progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_IDLE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());

        // kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_RUNNING, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_STARTED, info.getStatus());

        // abort
        info = prov.reIndex(account, "cancel", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_ABORTED, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_CANCELLED, info.getStatus());
        // restart re-indexing
        info = prov.reIndex(account, "start", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_RUNNING, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_STARTED, info.getStatus());
    }

    @Test
    public void testReindexEmptyMailbox() throws Exception {
        // shut down the service so it does not finish before we can check its
        // status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();

        // check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT1);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        ReIndexInfo.Progress progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals("status code should be 0 (idle)", ReIndexStatus.STATUS_IDLE, progress.getStatusCode());
        Assert.assertEquals("status string should be 'idle'",ReIndex.STATUS_IDLE, info.getStatus());

        // kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals("status code should be 1 (running)", ReIndexStatus.STATUS_RUNNING, progress.getStatusCode()); //status code is used by new SOAP clients
        Assert.assertEquals("status string should be 'started'", ReIndex.STATUS_STARTED, info.getStatus()); //legacy SOAP clients expect this value

        Zimbra.getAppContext().getBean(IndexingService.class).startUp();
        try {
            TestUtil.getMailbox(RECIPIENT1).index.waitForIndexing(0);
            fail("should throw an exception on not finding the index");
        } catch (ServiceException e) {
            // index store does not exist, so this should throw an exception
            assertNotNull(e);
            assertEquals("should throw NOT_FOUND", ServiceException.NOT_FOUND, e.getCode());
        }

        // verify that it is not running
        info = prov.reIndex(account, "status", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals("status code should be 2 (done) ", ReIndexStatus.STATUS_DONE, progress.getStatusCode());
        Assert.assertEquals("status string should be 'idle'", ReIndex.STATUS_IDLE, info.getStatus());
    }

    @Test
    public void testReindexMailbox() throws Exception {
        // shut down the service so it does not finish before we can check its
        // status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();

        // check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT1);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        ReIndexInfo.Progress progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_IDLE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());

        // add some messages
        Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
        TestUtil.addMessage(recieverMbox, NAME_PREFIX);
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), String.format("subject:%s", NAME_PREFIX));

        // delete index
        recieverMbox.index.deleteIndex();

        // kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_RUNNING, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_STARTED, info.getStatus());

        Zimbra.getAppContext().getBean(IndexingService.class).startUp();
        TestUtil.getMailbox(RECIPIENT1).index.waitForIndexing(0);

        // verify that it is not running
        info = prov.reIndex(account, "status", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_DONE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());

        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), String.format("subject:%s", NAME_PREFIX));
    }

    @Test
    public void testReindexMailboxWithMultiDayMessageRange() throws Exception {
        // shut down the service so it does not finish before we can check its status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();

        // check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT1);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        ReIndexInfo.Progress progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_IDLE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());

        // add some messages
        Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
        TestUtil.addMessage(recieverMbox, NAME_PREFIX);
        TestUtil.addMessage(recieverMbox, Mailbox.ID_FOLDER_INBOX, "Shakespeare last month", System.currentTimeMillis() - Constants.MILLIS_PER_MONTH);
        TestUtil.addMessage(recieverMbox, Mailbox.ID_FOLDER_INBOX, "Robert Burns yesterday", System.currentTimeMillis() - Constants.MILLIS_PER_DAY);
        TestUtil.addMessage(recieverMbox, Mailbox.ID_FOLDER_INBOX, "Edgar Allan Poe the day before", System.currentTimeMillis() - Constants.MILLIS_PER_DAY*2);

        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), String.format("subject:%s", NAME_PREFIX));
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), "subject:Shakespeare");
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), "subject:yesterday");
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), "subject:Allan");

        // delete index
        recieverMbox.index.deleteIndex();

        // kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_RUNNING, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_STARTED, info.getStatus());

        Zimbra.getAppContext().getBean(IndexingService.class).startUp();
        TestUtil.getMailbox(RECIPIENT1).index.waitForIndexing(30000);

        // verify that it is not running
        info = prov.reIndex(account, "status", null, null);
        progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_DONE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());

        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), String.format("subject:%s", NAME_PREFIX));
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), "subject:Shakespeare");
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), "subject:yesterday");
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT1), "subject:Allan");
    }

    @Test
    public void reIndexMultipleMailboxes() throws Exception {
        // shut down the service so it does not finish before we can check its status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        ArrayList<String> recipients = Lists.newArrayList(RECIPIENT1, RECIPIENT2, RECIPIENT3);
        ArrayList<Account> accounts = Lists.newArrayList(TestUtil.getAccount(RECIPIENT1), TestUtil.getAccount(RECIPIENT2), TestUtil.getAccount(RECIPIENT3));
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        List<ReIndexInfo.Progress> progress = prov.reIndex(accounts, "status");
        for(ReIndexInfo.Progress prog : progress) {
            Assert.assertNotNull(prog);
            String accountId = prog.getAccountId();
            Assert.assertNotNull("missing account ID from reindex progress status", accountId);
            Assert.assertEquals("reindexing status for account " + accountId + " should be 0 before reindexing starts", ReIndexStatus.STATUS_IDLE,prog.getStatusCode());
        }

        for(String recipient : recipients) {
            // add some messages
            Mailbox recieverMbox = TestUtil.getMailbox(recipient);
            TestUtil.addMessage(recieverMbox, NAME_PREFIX);
            TestUtil.addMessage(recieverMbox, Mailbox.ID_FOLDER_INBOX, "Shakespeare last month", System.currentTimeMillis() - Constants.MILLIS_PER_MONTH);
            TestUtil.addMessage(recieverMbox, Mailbox.ID_FOLDER_INBOX, "Robert Burns yesterday", System.currentTimeMillis() - Constants.MILLIS_PER_DAY);
            TestUtil.addMessage(recieverMbox, Mailbox.ID_FOLDER_INBOX, "Edgar Allan Poe the day before", System.currentTimeMillis() - Constants.MILLIS_PER_DAY*2);
        }

        for(String recipient : recipients) {
            // check that messages got indexed
            ZMailbox recieverMbox = TestUtil.getZMailbox(recipient);
            TestUtil.waitForMessage(recieverMbox, String.format("subject:%s", NAME_PREFIX));
            TestUtil.waitForMessage(recieverMbox, "subject:Shakespeare");
            TestUtil.waitForMessage(recieverMbox, "subject:yesterday");
            TestUtil.waitForMessage(recieverMbox, "subject:Allan");
        }

        for(String recipient : recipients) {
            // delete index
            Mailbox recieverMbox = TestUtil.getMailbox(recipient);
            recieverMbox.index.deleteIndex();
        }

        progress = prov.reIndex(accounts, "start");
        for(ReIndexInfo.Progress prog : progress) {
            Assert.assertNotNull(prog);
            String accountId = prog.getAccountId();
            Assert.assertNotNull("missing account ID from reindex progress status", accountId);
            Assert.assertEquals("reindexing status for account " + accountId + " should be 1 after reindexing starts",
                    ReIndexStatus.STATUS_RUNNING,prog.getStatusCode());
        }

        for(String recipient : recipients) {
            Zimbra.getAppContext().getBean(IndexingService.class).startUp();
            TestUtil.getMailbox(recipient).index.waitForIndexing(30000);
        }

        //verify that reindexing is not running
        progress = prov.reIndex(accounts, "status");
        for(ReIndexInfo.Progress prog : progress) {
            Assert.assertNotNull(prog);
            String accountId = prog.getAccountId();
            Assert.assertNotNull("missing account ID from reindex progress status", accountId);
            Assert.assertEquals("reindexing status for account " + accountId + " should be 2 when reindexing is complete",
                    ReIndexStatus.STATUS_DONE,prog.getStatusCode());
        }
    }

    @Test
    public void statusIdle() throws Exception {
        Account account = TestUtil.getAccount(RECIPIENT1);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        ReIndexInfo.Progress progress = info.getProgress();
        Assert.assertNotNull(progress);
        Assert.assertEquals(ReIndexStatus.STATUS_IDLE, progress.getStatusCode());
        Assert.assertEquals(ReIndex.STATUS_IDLE, info.getStatus());
    }

    @Before
    public void setUp() throws Exception {
        cleanup();
        originalLCSetting = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexManualCommit, true);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
        MailboxManager.getInstance().getMailboxByAccount(TestUtil.createAccount(RECIPIENT1), true);
        MailboxManager.getInstance().getMailboxByAccount(TestUtil.createAccount(RECIPIENT2), true);
        MailboxManager.getInstance().getMailboxByAccount(TestUtil.createAccount(RECIPIENT3), true);
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(originalLCSetting);
    }

    private void cleanup() {
        try {
            if (TestUtil.accountExists(RECIPIENT1)) {
                TestUtil.deleteAccount(RECIPIENT1);
            }
            if (TestUtil.accountExists(RECIPIENT2)) {
                TestUtil.deleteAccount(RECIPIENT2);
            }
            if (TestUtil.accountExists(RECIPIENT3)) {
                TestUtil.deleteAccount(RECIPIENT3);
            }
        } catch (ServiceException e) {
            ZimbraLog.test.error(e);
        }
        if (Zimbra.getAppContext().getBean(IndexingQueueAdapter.class) != null) {
            Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).drain();
        }
    }
}
