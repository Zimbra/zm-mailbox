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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.qa.unittest.TestUtil;

/**
 * Unit test for {@link Message}.
 *
 * @author ysasaki
 */
public final class MessageTest {

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
    public void indexRawMimeMessage() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        byte[] raw = ByteStreams.toByteArray(getClass().getResourceAsStream("raw-jis-msg.txt"));
        ParsedMessage pm = new ParsedMessage(raw, false);
        Message message = mbox.addMessage(null, pm, opt, null);
        Assert.assertEquals("\u65e5\u672c\u8a9e", pm.getFragment());
        List<IndexDocument> docs = message.generateIndexData();
        Assert.assertEquals(2, docs.size());
        String subject = docs.get(0).toDocument().get(LuceneFields.L_H_SUBJECT);
        String body = docs.get(0).toDocument().get(LuceneFields.L_CONTENT);
        Assert.assertEquals("\u65e5\u672c\u8a9e", subject);
        Assert.assertEquals("\u65e5\u672c\u8a9e", body.trim());
    }

    @Test
    public void getSortRecipients() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg1 = mbox.addMessage(null, new ParsedMessage(
                "From: from1@zimbra.com\r\nTo: to1@zimbra.com".getBytes(), false), opt, null);
        Message msg2 = mbox.addMessage(null, new ParsedMessage(
                "From: from2@zimbra.com\r\nTo: to2 <to2@zimbra.com>".getBytes(), false), opt, null);
        Message msg3 = mbox.addMessage(null, new ParsedMessage(
                "From: from3@zimbra.com\r\nTo: to3-1 <to3-1@zimbra.com>, to3-2 <to3-2@zimbra.com>".getBytes(),
                false), opt, null);

        Assert.assertEquals("to1@zimbra.com", msg1.getSortRecipients());
        Assert.assertEquals("to2", msg2.getSortRecipients());
        Assert.assertEquals("to3-1, to3-2", msg3.getSortRecipients());

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals("to1@zimbra.com", DbUtil.executeQuery(conn,
                "SELECT recipients FROM mboxgroup1.mail_item WHERE mailbox_id = ? AND id = ?",
                mbox.getId(), msg1.getId()).getString(1));
        Assert.assertEquals("to2", DbUtil.executeQuery(conn,
                "SELECT recipients FROM mboxgroup1.mail_item WHERE mailbox_id = ? AND id = ?",
                mbox.getId(), msg2.getId()).getString(1));
        Assert.assertEquals("to3-1, to3-2", DbUtil.executeQuery(conn,
                "SELECT recipients FROM mboxgroup1.mail_item WHERE mailbox_id = ? AND id = ?",
                mbox.getId(), msg3.getId()).getString(1));
        conn.closeQuietly();
    }

    @Test
    public void moveOutOfSpam() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox.getAccount().setJunkMessagesIndexingEnabled(false);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_SPAM);
        Message msg = mbox.addMessage(null, new ParsedMessage(
                "From: spammer@zimbra.com\r\nTo: test@zimbra.com".getBytes(), false), opt, null);
        MailboxTestUtil.index(mbox);

        SearchParams params = new SearchParams();
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setQueryString("from:spammer");
        ZimbraQueryResults result = mbox.index.search(SoapProtocol.Soap12, new OperationContext(mbox), params);
        Assert.assertFalse(result.hasNext());

        mbox.move(new OperationContext(mbox), msg.getId(), MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);
        MailboxTestUtil.index(mbox);

        result = mbox.index.search(SoapProtocol.Soap12, new OperationContext(mbox), params);
        Assert.assertTrue(result.hasNext());
        Assert.assertEquals(msg.getId(), result.getNext().getItemId());
    }
    
    @Test
    public void post() throws Exception {
        // Create post.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        opt.setFlags(FlagInfo.POST.toBitmask());
        Message msg = mbox.addMessage(null, new ParsedMessage(
                "From: test@zimbra.com\r\nTo: test@zimbra.com".getBytes(), false), opt, null);

        // Validate flag.
        Assert.assertTrue((msg.getFlagBitmask() & Flag.FlagInfo.POST.toBitmask()) != 0);

        // Search by flag.
        List<Integer> ids = TestUtil.search(mbox, "tag:\\post", MailItem.Type.MESSAGE);
        Assert.assertEquals(1, ids.size());
        Assert.assertEquals(msg.getId(), ids.get(0).intValue());

        // Make sure that the post flag is serialized to XML.
        Element eMsg = ToXML.encodeMessageAsMIME(new XMLElement("test"), new ItemIdFormatter(), null, msg, null, false);
        Assert.assertEquals("^", eMsg.getAttribute(MailConstants.A_FLAGS));
        
        // Try unsetting the post flag.
        mbox.setTags(null, msg.getId(), MailItem.Type.MESSAGE, 0, null);
        msg = mbox.getMessageById(null, msg.getId());
        // make sure post flag is still set
        Assert.assertTrue((msg.getFlagBitmask() & Flag.FlagInfo.POST.toBitmask()) != 0);
    }

    @Test
    public void msgToPost() throws Exception {
        // Create msg.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, new ParsedMessage(
                "From: test@zimbra.com\r\nTo: test@zimbra.com".getBytes(), false), opt, null);
        // try setting the post flag
        mbox.setTags(null, msg.getId(), MailItem.Type.MESSAGE, FlagInfo.POST.toBitmask(), null);        
        msg = mbox.getMessageById(null, msg.getId());
        // make sure post flag is not set
        Assert.assertTrue((msg.getFlagBitmask() & Flag.FlagInfo.POST.toBitmask()) == 0);
    }
}
