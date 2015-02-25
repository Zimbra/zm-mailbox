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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexInfo;
import com.zimbra.cs.index.IndexingQueueAdapter;
import com.zimbra.cs.index.IndexingService;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for ReIndex admin operation.
 * <p>
 * This test requires a Zimbra dev server instance.
 *
 * TODO: Add this class to {@link ZimbraSuite} once it supports JUnit 4
 * annotations.
 *
 * @author ysasaki
 * @author Greg Solovyev
 */
public class TestReIndex {
    private static final String NAME_PREFIX = TestReIndex.class.getSimpleName();
    private static final String RECIPIENT = NAME_PREFIX + "user1";
    private boolean originalLCSetting = false;
    @Test
    public void testStartReindex() throws Exception {
        //shut down the service so it does not finish before we can check its status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        
        //check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        Assert.assertEquals("idle", info.getStatus());
        
        //kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        Assert.assertEquals("started", info.getStatus());
    }
    
    @Test
    public void testReindexEmptyMailbox() throws Exception {
        //shut down the service so it does not finish before we can check its status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        
        //check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        Assert.assertEquals("idle", info.getStatus());
        
        //kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        Assert.assertEquals("started", info.getStatus());
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();
        try {
            TestUtil.getMailbox(RECIPIENT).index.waitForIndexing(5000);
        } catch (ServiceException e) {
            //index store does not exist, so this should throw an exception
            assertNotNull(e);
            assertEquals("should throw NOT_FOUND", ServiceException.NOT_FOUND, e.getCode());
        }
        
        //verify that it is not running
        info = prov.reIndex(account, "status", null, null);
        Assert.assertEquals("idle", info.getStatus());
    }
    
    
    @Test
    public void testReindexMailbox() throws Exception {
        //shut down the service so it does not finish before we can check its status
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        
        //check that reindexing is not running
        Account account = TestUtil.getAccount(RECIPIENT);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        Assert.assertEquals("idle", info.getStatus());
        
        //add some messages
        Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT);
        TestUtil.addMessage(recieverMbox, NAME_PREFIX);
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT), String.format("subject:%s", NAME_PREFIX));
        
        //delete index
        recieverMbox.index.deleteIndex();
        
        //kick off re-indexing
        info = prov.reIndex(account, "start", null, null);
        Assert.assertEquals("started", info.getStatus());
        Zimbra.getAppContext().getBean(IndexingService.class).startUp();
        TestUtil.getMailbox(RECIPIENT).index.waitForIndexing(5000);
        
        //verify that it is not running
        info = prov.reIndex(account, "status", null, null);
        Assert.assertEquals("idle", info.getStatus());
        
        TestUtil.waitForMessage(TestUtil.getZMailbox(RECIPIENT), String.format("subject:%s", NAME_PREFIX));
    }
    
    @Test
    public void statusIdle() throws Exception {
        Account account = TestUtil.getAccount(RECIPIENT);
        SoapProvisioning prov = TestProvisioningUtil.getSoapProvisioning();
        ReIndexInfo info = prov.reIndex(account, "status", null, null);
        Assert.assertEquals("idle", info.getStatus());
    }
    
    @Before
    public void setUp() throws Exception {
        cleanup();
        originalLCSetting = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexManualCommit, true);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
        MailboxManager.getInstance().getMailboxByAccount(TestUtil.createAccount(RECIPIENT), true);
    }
    
    @After
    public void tearDown() throws Exception {
        cleanup();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(originalLCSetting);
    }
    
    private void cleanup() throws Exception {
        if(TestUtil.accountExists(RECIPIENT)) {
            TestUtil.deleteAccount(RECIPIENT);
        }
        if(Zimbra.getAppContext().getBean(IndexingQueueAdapter.class) != null) {
            Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).drain();
        }
    }

}
