/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.EnumSet;
import org.junit.Ignore;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbUtil;
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
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class MessageTest {

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

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        byte[] raw = ByteStreams.toByteArray(getClass().getResourceAsStream("raw-jis-msg.txt"));
        ParsedMessage pm = new ParsedMessage(raw, false);
        Message message = mbox.addMessage(null, pm, dopt, null);

        Assert.assertEquals("\u65e5\u672c\u8a9e", pm.getFragment(null));
        List<IndexDocument> docs = message.generateIndexDataAsync(true);
        Assert.assertEquals(2, docs.size());
        String subject = (String) docs.get(0).toInputDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) docs.get(0).toInputDocument().getFieldValue(LuceneFields.L_CONTENT);
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

        SearchParams params = new SearchParams();
        params.setSortBy(SortBy.NONE);
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setQueryString("from:spammer");
        ZimbraQueryResults result = mbox.index.search(SoapProtocol.Soap12, new OperationContext(mbox), params);
        Assert.assertFalse(result.hasNext());

        mbox.move(new OperationContext(mbox), msg.getId(), MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);

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
        Element eMsg = ToXML.encodeMessageAsMIME(new XMLElement("test"), new ItemIdFormatter(), (OperationContext)null,
                msg, (String)null /* part */, false /* mustInline */, false /* mustNotInline */,
                false /* serializeType */, ToXML.NOTIFY_FIELDS);

        Assert.assertEquals("^", eMsg.getAttribute(MailConstants.A_FLAGS));

        // Try unsetting the post flag.
        mbox.setTags(null, msg.getId(), MailItem.Type.MESSAGE, 0, null);
        msg = mbox.getMessageById(null, msg.getId());
        // make sure post flag is still set
        Assert.assertTrue("POST flag set", (msg.getFlagBitmask() & Flag.FlagInfo.POST.toBitmask()) != 0);
        Assert.assertEquals("IMAP UID should be same as ID", msg.getIdInMailbox(), msg.getImapUid());
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
