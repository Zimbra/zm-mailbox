/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.solr.MockSolrIndex;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
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

        LC.zimbra_class_index_store_factory.setDefault(MockSolrIndex.Factory.class.getName());
        IndexStore.setFactory(LC.zimbra_class_index_store_factory.value());

        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
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
        List<IndexDocument> docs = message.generateIndexData();
        Assert.assertEquals(2, docs.size());
        String subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
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

    @Test
    public void testConstructFromData() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        byte[] raw = ByteStreams.toByteArray(getClass().getResourceAsStream("raw-jis-msg.txt"));
        ParsedMessage pm = new ParsedMessage(raw, false);
        Message message = mbox.addMessage(null, pm, dopt, null);

        UnderlyingData ud = DbMailItem.getById(mbox.getId(), mbox.getSchemaGroupId(), message.getId(), message.getType(), message.inDumpster(), DbPool.getConnection(mbox.getId(), mbox.getSchemaGroupId()));
        assertNotNull("Underlying data is null", ud);
        assertEquals("underlying data has wrong type", MailItem.Type.MESSAGE,MailItem.Type.of(ud.type));
        assertEquals("underlying data has wrong subject", "\u65e5\u672c\u8a9e",ud.getSubject());
        assertEquals("underlying data has wrong UUID", message.getUuid(),ud.uuid);

        MailItem testItem = MailItem.constructItem(account,ud,mbox.getId());
        assertNotNull("reconstructed mail item is null", testItem);
        assertTrue("reconstructed item is not an instance of Message", testItem instanceof Message);

        assertEquals("reconstructed message has wrong item type", MailItem.Type.MESSAGE,testItem.getType());
        assertEquals("reconstructed message has wrong UUID", message.getUuid(), testItem.getUuid());
        assertEquals("reconstructed message has wrong ID", message.getId(), testItem.getId());
        assertEquals("reconstructed message has wrong folder", message.getFolderId(), testItem.getFolderId());
        assertEquals("reonstructed message has wrong content", Arrays.toString(message.getContent()),Arrays.toString(testItem.getContent()));
        assertEquals("reonstructed message has wrong recipients", message.getRecipients(),  ((Message)testItem).getRecipients());
        assertEquals("reonstructed message has wrong sender", message.getSender(),  ((Message)testItem).getSender());
        assertEquals("reonstructed message has wrong date", message.getDate(),  ((Message)testItem).getDate());
        assertEquals("reonstructed message has wrong fragment", message.getFragment(),  ((Message)testItem).getFragment());

        List<IndexDocument> docs = testItem.generateIndexDataAsync(false);
        Assert.assertEquals(1, docs.size());
        String subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
        Assert.assertEquals("\u65e5\u672c\u8a9e", subject);
        Assert.assertEquals("\u65e5\u672c\u8a9e", body.trim());

        docs = testItem.generateIndexDataAsync(true);
        Assert.assertEquals(2, docs.size());
        subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
        Assert.assertEquals("\u65e5\u672c\u8a9e", subject);
        Assert.assertEquals("\u65e5\u672c\u8a9e", body.trim());
    }

    @Test
    public void testGenerateIndexData() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        byte[] raw = ByteStreams.toByteArray(getClass().getResourceAsStream("raw-jis-msg.txt"));
        ParsedMessage pm = new ParsedMessage(raw, false);
        Message message = mbox.addMessage(null, pm, dopt, null);

        Assert.assertEquals("\u65e5\u672c\u8a9e", pm.getFragment(null));
        List<IndexDocument> docs = message.generateIndexData();
        Assert.assertEquals(2, docs.size());
        String subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
        Assert.assertEquals("\u65e5\u672c\u8a9e", subject);
        Assert.assertEquals("\u65e5\u672c\u8a9e", body.trim());
    }

    @Test
    public void testGenerateIndexDataAsync() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        byte[] raw = ByteStreams.toByteArray(getClass().getResourceAsStream("raw-jis-msg.txt"));
        ParsedMessage pm = new ParsedMessage(raw, false);
        Message message = mbox.addMessage(null, pm, dopt, null);

        Assert.assertEquals("\u65e5\u672c\u8a9e", pm.getFragment(null));
        List<IndexDocument> docs = message.generateIndexDataAsync(false);
        Assert.assertEquals(1, docs.size());
        String subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
        Assert.assertEquals("\u65e5\u672c\u8a9e", subject);
        Assert.assertEquals("\u65e5\u672c\u8a9e", body.trim());

        docs = message.generateIndexDataAsync(true);
        Assert.assertEquals(2, docs.size());
        subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
        Assert.assertEquals("\u65e5\u672c\u8a9e", subject);
        Assert.assertEquals("\u65e5\u672c\u8a9e", body.trim());
    }
}
