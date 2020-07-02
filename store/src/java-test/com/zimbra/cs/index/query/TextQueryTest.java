/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.query;

import java.util.EnumSet;
import org.junit.Ignore;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.mail.Search;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.message.SearchResponse;
import com.zimbra.soap.type.SearchHit;

/**
 * Unit test for {@link TextQuery}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class TextQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void wildcardExpandedToNone() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        ZimbraQueryResults results = mbox.index.search(new OperationContext(mbox), "none*",
                EnumSet.of(MailItem.Type.MESSAGE), SortBy.NONE, 100);
        Assert.assertFalse(results.hasNext());

        results = mbox.index.search(new OperationContext(mbox), "from:none* AND subject:none*",
                EnumSet.of(MailItem.Type.MESSAGE), SortBy.NONE, 100);
        Assert.assertFalse(results.hasNext());

        results = mbox.index.search(new OperationContext(mbox), "from:none* OR subject:none*",
                EnumSet.of(MailItem.Type.MESSAGE), SortBy.NONE, 100);
        Assert.assertFalse(results.hasNext());
    }

    @Test
    public void sortByHasAttach() throws Exception {
        Account acct = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add a message
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        Message msg = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), dopt, null);
        Message msgWithAttach = mbox.addMessage(null,
                MailboxTestUtil.generateMessageWithAttachment("test subject has attach"), dopt, null);

        SearchResponse resp;
        List<SearchHit> hits;
        int msgId;
        int msgWithAttachId;
        SearchRequest sr = new SearchRequest();
        sr.setSearchTypes("message");
        sr.setQuery("test");
        sr.setSortBy(SortBy.ATTACHMENT_ASC.toString());
        resp = doSearch(sr, acct);
        hits = resp.getSearchHits();
        Assert.assertEquals("Number of hits", 2, hits.size());
        msgId = Integer.parseInt(hits.get(0).getId());
        msgWithAttachId = Integer.parseInt(hits.get(1).getId());
        Assert.assertEquals("correct hit ascending no attachments", msg.getId(), msgId);
        Assert.assertEquals("correct hit ascending has attachments", msgWithAttach.getId(), msgWithAttachId);

        /* Check that we get them in the opposite order if we change the search direction */
        sr.setSortBy(SortBy.ATTACHMENT_DESC.toString());
        resp = doSearch(sr, acct);
        hits = resp.getSearchHits();
        Assert.assertEquals("Number of hits", 2, hits.size());
        msgId = Integer.parseInt(hits.get(1).getId());
        msgWithAttachId = Integer.parseInt(hits.get(0).getId());
        Assert.assertEquals("correct hit descending no attachments", msg.getId(), msgId);
        Assert.assertEquals("correct hit descending has attachments", msgWithAttach.getId(), msgWithAttachId);
    }

    @Test
    public void sortByIsFlagged() throws Exception {
        Account acct = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add a message
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        Message msg = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), dopt, null);
        dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(
                Flag.BITMASK_UNREAD | Flag.BITMASK_FLAGGED);
        Message msgWithFlag = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject flag"), dopt, null);

        SearchResponse resp;
        List<SearchHit> hits;
        int msgId;
        int msgWithFlagId;
        SearchRequest sr = new SearchRequest();
        sr.setSearchTypes("message");
        sr.setQuery("test");
        sr.setSortBy(SortBy.FLAG_ASC.toString());
        resp = doSearch(sr, acct);
        hits = resp.getSearchHits();
        Assert.assertEquals("Number of hits", 2, hits.size());
        msgId = Integer.parseInt(hits.get(0).getId());
        msgWithFlagId = Integer.parseInt(hits.get(1).getId());
        Assert.assertEquals("correct hit ascending unflagged", msg.getId(), msgId);
        Assert.assertEquals("correct hit ascending flagged", msgWithFlag.getId(), msgWithFlagId);

        /* Check that we get them in the opposite order if we change the search direction */
        sr.setSortBy(SortBy.FLAG_DESC.toString());
        resp = doSearch(sr, acct);
        hits = resp.getSearchHits();
        Assert.assertEquals("Number of hits", 2, hits.size());
        msgId = Integer.parseInt(hits.get(1).getId());
        msgWithFlagId = Integer.parseInt(hits.get(0).getId());
        Assert.assertEquals("correct hit descending unflagged", msg.getId(), msgId);
        Assert.assertEquals("correct hit descending flagged", msgWithFlag.getId(), msgWithFlagId);
    }

    @Test
    public void sortByPriority() throws Exception {
        Account acct = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add a message
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        Message msg = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), dopt, null);
        Message msgWithHighPri = mbox.addMessage(null,
                MailboxTestUtil.generateHighPriorityMessage("test subject is HI-PRI"), dopt, null);
        Message msgWithLowPri = mbox.addMessage(null,
                MailboxTestUtil.generateLowPriorityMessage("test subject is LOW-PRI"), dopt, null);

        SearchResponse resp;
        List<SearchHit> hits;
        int msgId;
        int msgWithLowPriId;
        int msgWithHiPriId;
        SearchRequest sr = new SearchRequest();
        sr.setSearchTypes("message");
        sr.setQuery("test");
        sr.setSortBy(SortBy.PRIORITY_ASC.toString());
        resp = doSearch(sr, acct);
        hits = resp.getSearchHits();
        Assert.assertEquals("Number of hits", 3, hits.size());
        msgId = Integer.parseInt(hits.get(1).getId());
        msgWithHiPriId = Integer.parseInt(hits.get(2).getId());
        msgWithLowPriId = Integer.parseInt(hits.get(0).getId());
        Assert.assertEquals("correct hit ascending high", msgWithHighPri.getId(), msgWithHiPriId);
        Assert.assertEquals("correct hit ascending med", msg.getId(), msgId);
        Assert.assertEquals("correct hit ascending low", msgWithLowPri.getId(), msgWithLowPriId);

        /* Check that we get them in the opposite order if we change the search direction */
        sr.setSortBy(SortBy.PRIORITY_DESC.toString());
        resp = doSearch(sr, acct);
        hits = resp.getSearchHits();
        Assert.assertEquals("Number of hits", 3, hits.size());
        msgId = Integer.parseInt(hits.get(1).getId());
        msgWithHiPriId = Integer.parseInt(hits.get(0).getId());
        msgWithLowPriId = Integer.parseInt(hits.get(2).getId());
        Assert.assertEquals("correct hit descending high", msgWithHighPri.getId(), msgWithHiPriId);
        Assert.assertEquals("correct hit descending med", msg.getId(), msgId);
        Assert.assertEquals("correct hit descending low", msgWithLowPri.getId(), msgWithLowPriId);
    }

    static SearchResponse doSearch(SearchRequest request, Account acct) throws Exception {
        Element response = new Search().handle(JaxbUtil.jaxbToElement(request, Element.XMLElement.mFactory),
                                ServiceTestUtil.getRequestContext(acct));
        SearchResponse resp = JaxbUtil.elementToJaxb(response, SearchResponse.class);
        return resp;
    }
}
