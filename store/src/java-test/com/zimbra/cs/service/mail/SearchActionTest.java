/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.util.ZTestWatchman;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.ConvActionRequest;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.type.BulkAction;
import com.zimbra.soap.type.SearchHit;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SoapHttpTransport.class })
@PowerMockIgnore({ "javax.crypto.*", "javax.xml.bind.annotation.*", "javax.net.ssl.*" })

public class SearchActionTest {

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new ZTestWatchman();

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("testMove@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("testRead@zimbra.com", "secret", attrs);
    }

    @Test
    public void testSearchActionMove() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("testMove@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        // Add two messages to inbox, one with search match and other with no match
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX)
            .setFlags(Flag.BITMASK_UNREAD | Flag.BITMASK_MUTED);
        mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), dopt, null);
        mbox.addMessage(null, MailboxTestUtil.generateMessage("unmatched subject"), dopt, null);
        TypedIdList ids = mbox.getItemIds(null, 2);
        Assert.assertEquals(2, ids.size());
        SearchRequest sRequest = new SearchRequest();
        sRequest.setSearchTypes("conversation");
        // search with query 'test'
        sRequest.setQuery("test");
        BulkAction bAction = new BulkAction();
        // search action - move search result to 'Trash'
        bAction.setOp(BulkAction.Operation.move);
        bAction.setFolder("Trash");
        Map<String, Object> context = ServiceTestUtil.getRequestContext(acct);
        ZimbraSoapContext zsc = (ZimbraSoapContext) context.get(SoapEngine.ZIMBRA_CONTEXT);
        Element searchResponse = new Search().handle(zsc.jaxbToElement(sRequest),
            ServiceTestUtil.getRequestContext(acct));
        com.zimbra.soap.mail.message.SearchResponse sResponse = zsc.elementToJaxb(searchResponse);
        List<SearchHit> searchHits = sResponse.getSearchHits();
        SearchAction.performAction(bAction, sRequest, searchHits, mbox, null);
        // check inbox contains only 1 unmatched mail item after move
        List<MailItem> mailItems = mbox.getItemList(null, MailItem.Type.MESSAGE, 2,
            com.zimbra.cs.index.SortBy.DATE_DESC);
        Assert.assertEquals(1, mailItems.size());
        Assert.assertEquals("unmatched subject", mailItems.get(0).getSubject());
        // check trash contains mail item having 'test subject' after move
        mailItems = mbox.getItemList(null, MailItem.Type.MESSAGE, 3,
            com.zimbra.cs.index.SortBy.DATE_DESC);
        Assert.assertEquals(1, mailItems.size());
        Assert.assertEquals("test subject", mailItems.get(0).getSubject());
    }

    @Test
    public void testSearchActionRead() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("testRead@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        // Add two messages to inbox, one with search match and other with no match
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX)
            .setFlags(Flag.BITMASK_UNREAD | Flag.BITMASK_MUTED);
        Message message1 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"),
            dopt, null);
        Message message2 = mbox.addMessage(null, MailboxTestUtil.generateMessage("unmatched subject"), dopt, null);
        TypedIdList ids = mbox.getItemIds(null, 2);
        Assert.assertEquals(2, ids.size());
        Assert.assertEquals(true, message1.isUnread());
        Assert.assertEquals(true, message2.isUnread());
        SearchRequest sRequest = new SearchRequest();
        sRequest.setSearchTypes("conversation");
        // search with query 'test'
        sRequest.setQuery("test");
        BulkAction bAction = new BulkAction();
        // search action - mark search result with 'read'
        bAction.setOp(BulkAction.Operation.read);
        Map<String, Object> context = ServiceTestUtil.getRequestContext(acct);
        ZimbraSoapContext zsc = (ZimbraSoapContext) context.get(SoapEngine.ZIMBRA_CONTEXT);
        Element searchResponse = new Search().handle(zsc.jaxbToElement(sRequest),
            ServiceTestUtil.getRequestContext(acct));
        com.zimbra.soap.mail.message.SearchResponse sResponse = zsc.elementToJaxb(searchResponse);
        List<SearchHit> searchHits = sResponse.getSearchHits();
        ConvActionRequest req = SearchAction.getConvActionRequest(searchHits, "read");
        ConvAction convAction = new ConvAction();
        PowerMockito.stub(PowerMockito.method(SoapHttpTransport.class, "invokeWithoutSession"))
            .toReturn(
                convAction.handle(zsc.jaxbToElement(req), ServiceTestUtil.getRequestContext(acct)));
        SearchAction.performAction(bAction, sRequest, searchHits, mbox, null);
        // check search result message is marked read
        Assert.assertEquals(false, message1.isUnread());
        Assert.assertEquals(true, message2.isUnread());
    }

    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


