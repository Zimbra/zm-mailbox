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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.type.BulkAction;
import com.zimbra.soap.type.SearchHit;

public class SearchActionTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();

        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object> newHashMap());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testSearchAction() throws Exception {
        Account acct = Provisioning.getInstance()
            .getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        // Add two messages to inbox, one with search match and other with no
        // match
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
}
