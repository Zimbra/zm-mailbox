/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZAttrProvisioning.MailThreadingAlgorithm;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;

/**
 * Unit test for {@link Mailbox}.
 *
 * @author ysasaki
 */
public final class MailboxTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning prov = new MockProvisioning();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        MailboxManager.setInstance(null);
        MailboxIndex.startup();

        LC.zimbra_class_store.setDefault(MockStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
        File index = new File("build/test/index");
        if (index.isDirectory()) {
            Files.deleteDirectoryContents(index);
        }
    }

    @Test
    public void browse() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test1-2@sub1.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test1-3@sub1.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test1-4@sub1.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test2-1@sub2.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test2-2@sub2.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test2-3@sub2.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test3-1@sub3.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test3-2@sub3.zimbra.com".getBytes(), false), dopt);
        mbox.addMessage(null, new ParsedMessage("From: test4-1@sub4.zimbra.com".getBytes(), false), dopt);
        mbox.index.indexDeferredItems();

        List<BrowseTerm> terms = mbox.browse(null, Mailbox.BrowseBy.domains, null, 100);
        Assert.assertEquals(4, terms.size());
        Assert.assertEquals("sub1.zimbra.com", terms.get(0).getText());
        Assert.assertEquals("sub2.zimbra.com", terms.get(1).getText());
        Assert.assertEquals("sub3.zimbra.com", terms.get(2).getText());
        Assert.assertEquals("sub4.zimbra.com", terms.get(3).getText());
        Assert.assertEquals(4, terms.get(0).getFreq());
        Assert.assertEquals(3, terms.get(1).getFreq());
        Assert.assertEquals(2, terms.get(2).getFreq());
        Assert.assertEquals(1, terms.get(3).getFreq());
    }

    private ParsedMessage generateMessage(String subject) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "Bob Evans <bob@example.com>");
        mm.setHeader("To", "Jimmy Dean <jdean@example.com>");
        mm.setHeader("Subject", subject);
        mm.setText("nothing to see here");
        return new ParsedMessage(mm, false);
    }

    @Test
    public void threadDraft() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add the root message
        ParsedMessage pm = generateMessage("test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int rootId = mbox.addMessage(null, pm, dopt).getId();

        // first draft explicitly references the parent by item ID (how ZWC does it)
        pm = generateMessage("Re: test subject");
        Message draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        Message parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded explicitly", parent.getConversationId(), draft.getConversationId());

        // second draft implicitly references the parent by default threading rules
        pm = generateMessage("Re: test subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [saveDraft]", parent.getConversationId(), draft.getConversationId());

        // threading is set up at first save time, so modifying the second draft should *not* affect threading
        pm = generateMessage("Re: changed the subject");
        draft = mbox.saveDraft(null, pm, draft.getId());
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [resaved]", parent.getConversationId(), draft.getConversationId());

        // third draft is like second draft, but goes via Mailbox.addMessage (how IMAP does it)
        pm = generateMessage("Re: test subject");
        dopt = new DeliveryOptions().setFlags(Flag.BITMASK_DRAFT).setFolderId(Mailbox.ID_FOLDER_DRAFTS);
        draft = mbox.addMessage(null, pm, dopt);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded implicitly [addMessage]", parent.getConversationId(), draft.getConversationId());

        // fourth draft explicitly references the parent by item ID, even though it wouldn't get threaded using the default threader
        pm = generateMessage("changed the subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("threaded explicitly (changed subject)", parent.getConversationId(), draft.getConversationId());

        // fifth draft is not related to the parent and should not be threaded
        pm = generateMessage("Re: unrelated subject");
        draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals("unrelated", -draft.getId(), draft.getConversationId());
    }

}
