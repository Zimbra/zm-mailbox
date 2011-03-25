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

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailAddress;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mime.MockMimeTypeInfo;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.handler.TextPlainHandler;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;

/**
 * Unit test for {@link Message}.
 *
 * @author ysasaki
 */
public final class MessageTest {

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        MockMimeTypeInfo mime = new MockMimeTypeInfo();
        mime = new MockMimeTypeInfo();
        mime.setHandlerClass(TextPlainHandler.class.getName());
        mime.setIndexingEnabled(true);
        prov.addMimeType("text/plain", mime);
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
        Message message = mbox.addMessage(null, pm, opt);
        Assert.assertEquals("\u65e5\u672c\u8a9e", pm.getFragment());
        List<IndexDocument> docs = message.generateIndexData(false);
        Assert.assertEquals(2, docs.size());
        String subject = docs.get(0).toDocument().getField(LuceneFields.L_H_SUBJECT).stringValue();
        String body = docs.get(0).toDocument().getField(LuceneFields.L_CONTENT).stringValue();
        Assert.assertEquals("\u65e5\u672c\u8a9e", subject);
        Assert.assertEquals("\u65e5\u672c\u8a9e", body.trim());
    }

    @Test
    public void senderId() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg1 = mbox.addMessage(null, new ParsedMessage("From: test1@zimbra.com".getBytes(), false), opt);
        Message msg2 = mbox.addMessage(null, new ParsedMessage("From: test2@zimbra.com".getBytes(), false), opt);
        Message msg3 = mbox.addMessage(null, new ParsedMessage("From: test3@zimbra.com".getBytes(), false), opt);

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals(DbMailAddress.getId(conn, mbox, "test1@zimbra.com"), msg1.mData.senderId);
        Assert.assertEquals(DbMailAddress.getId(conn, mbox, "test2@zimbra.com"), msg2.mData.senderId);
        Assert.assertEquals(DbMailAddress.getId(conn, mbox, "test3@zimbra.com"), msg3.mData.senderId);
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, msg1.mData.senderId));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, msg2.mData.senderId));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, msg3.mData.senderId));
        conn.closeQuietly();
    }

}
