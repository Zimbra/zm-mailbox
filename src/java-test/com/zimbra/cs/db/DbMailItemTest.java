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
package com.zimbra.cs.db;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem.QueryParams;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link DbMailItem}.
 *
 * @author ysasaki
 */
public final class DbMailItemTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    private DbConnection conn = null;
    private Mailbox mbox = null;
    
    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        conn = DbPool.getConnection(mbox);
    }
    
    @After
    public void tearDown() {
        conn.closeQuietly();
    }

    @Test
    public void getIndexDeferredIds() throws Exception {
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 101, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 102, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 103, MailItem.Type.MESSAGE.toByte(), 103);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 200, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 201, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 202, MailItem.Type.CONTACT.toByte(), 202);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 300, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 301, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 302, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 303, MailItem.Type.MESSAGE.toByte(), 303);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 400, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 401, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 402, MailItem.Type.CONTACT.toByte(), 402);

        Multimap<MailItem.Type, Integer> result = DbMailItem.getIndexDeferredIds(conn, mbox);
        Assert.assertEquals(10, result.size());
        Assert.assertEquals(ImmutableSet.of(100, 101, 102, 300, 301, 302), result.get(MailItem.Type.MESSAGE));
        Assert.assertEquals(ImmutableSet.of(200, 201, 400, 401), result.get(MailItem.Type.CONTACT));
    }

    @Test
    public void setIndexIds() throws Exception {
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 200, MailItem.Type.MESSAGE.toByte(), 0);

        DbMailItem.setIndexIds(conn, mbox, ImmutableList.of(100, 200));
        Assert.assertEquals(100, DbUtil.executeQuery(conn,
                "SELECT index_id FROM mboxgroup1.mail_item WHERE id = ?", 100).getInt(1));
        Assert.assertEquals(200, DbUtil.executeQuery(conn,
                "SELECT index_id FROM mboxgroup1.mail_item_dumpster WHERE id = ?", 200).getInt(1));
    }

    @Test
    public void getReIndexIds() throws Exception {
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 101, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 102, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 103, MailItem.Type.MESSAGE.toByte(), null);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 200, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 201, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 202, MailItem.Type.CONTACT.toByte(), null);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 300, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 301, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 302, MailItem.Type.MESSAGE.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 303, MailItem.Type.MESSAGE.toByte(), null);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 400, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 401, MailItem.Type.CONTACT.toByte(), 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 402, MailItem.Type.CONTACT.toByte(), null);

        Assert.assertEquals(ImmutableList.of(100, 101, 102, 300, 301, 302),
                DbMailItem.getReIndexIds(conn, mbox, EnumSet.<MailItem.Type>of(MailItem.Type.MESSAGE)));
        Assert.assertEquals(ImmutableList.of(200, 201, 400, 401),
                DbMailItem.getReIndexIds(conn, mbox, EnumSet.<MailItem.Type>of(MailItem.Type.CONTACT)));
        Assert.assertEquals(ImmutableList.of(100, 101, 102, 200, 201, 300, 301, 302, 400, 401),
                DbMailItem.getReIndexIds(conn, mbox, EnumSet.<MailItem.Type>noneOf(MailItem.Type.class)));
    }

    @Test
    public void resetIndexId() throws Exception {
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(), 100);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item_dumpster " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0, 0)", mbox.getId(), 200, MailItem.Type.MESSAGE.toByte(), 200);

        DbMailItem.resetIndexId(conn, mbox);
        Assert.assertEquals(0, DbUtil.executeQuery(conn,
                "SELECT index_id FROM mboxgroup1.mail_item WHERE id = ?", 100).getInt(1));
        Assert.assertEquals(0, DbUtil.executeQuery(conn,
                "SELECT index_id FROM mboxgroup1.mail_item_dumpster WHERE id = ?", 200).getInt(1));
    }

    @Test
    public void completeConversation() throws Exception {
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, 0, 0, 0, 0, 0, 0, 0)", mbox.getId(), 200, MailItem.Type.CONVERSATION.toByte());
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, 0, 0, 0, 0, 0, 0, 0)", mbox.getId(), 201, MailItem.Type.CONVERSATION.toByte());

        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, folder_id, parent_id, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, ?, 0, 0, 0, ?, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(),
                Mailbox.ID_FOLDER_INBOX, 200, 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, folder_id, parent_id, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, ?, 0, 0, 0, ?, 0, 0, 0)", mbox.getId(), 101, MailItem.Type.MESSAGE.toByte(),
                Mailbox.ID_FOLDER_INBOX, 200, 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, folder_id, parent_id, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, ?, 0, 0, 0, ?, 0, 0, 0)", mbox.getId(), 102, MailItem.Type.MESSAGE.toByte(),
                Mailbox.ID_FOLDER_INBOX, 200, 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, folder_id, parent_id, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, ?, 0, 0, 0, ?, 0, 0, 0)", mbox.getId(), 103, MailItem.Type.MESSAGE.toByte(),
                Mailbox.ID_FOLDER_INBOX, 201, Flag.BITMASK_FROM_ME);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, folder_id, parent_id, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, ?, 0, 0, 0, ?, 0, 0, 0)", mbox.getId(), 104, MailItem.Type.MESSAGE.toByte(),
                Mailbox.ID_FOLDER_INBOX, 201, 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, folder_id, parent_id, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, ?, 0, 0, 0, ?, 0, 0, 0)", mbox.getId(), 105, MailItem.Type.MESSAGE.toByte(),
                Mailbox.ID_FOLDER_INBOX, 201, 0);

        MailItem.UnderlyingData data = new MailItem.UnderlyingData();
        data.id = 200;
        data.type = MailItem.Type.CONVERSATION.toByte();
        DbMailItem.completeConversation(mbox, conn, data);
        Assert.assertFalse(data.isSet(Flag.FlagInfo.FROM_ME));

        data = new MailItem.UnderlyingData();
        data.id = 201;
        data.type = MailItem.Type.CONVERSATION.toByte();
        DbMailItem.completeConversation(mbox, conn, data);
        Assert.assertTrue(data.isSet(Flag.FlagInfo.FROM_ME));
    }

    @Test
    public void getIds() throws Exception {
        int now = (int) (System.currentTimeMillis()/1000);
        int beforeNow = now - 1000;
        int afterNow = now + 1000;
        final int beforeNowCount = 9;
        final int afterNowCount = 13;
        int id = 100;
        Set<Integer> ids = DbMailItem.getIds(mbox, conn, new QueryParams(), false);
        QueryParams params = new QueryParams();
        params.setChangeDateBefore(now);
        Set<Integer> idsInitBeforeNow = DbMailItem.getIds(mbox, conn, params, false);
        params = new QueryParams();
        params.setChangeDateAfter(now);
        Set<Integer> idsInitAftereNow = DbMailItem.getIds(mbox, conn, params, false);

        int idsInit = ids.size();
        for (int i = 0; i < beforeNowCount; i++) {
            DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                    "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, change_date, mod_content) " +
                    "VALUES(?, ?, ?, 0, 0, 0, 0, 0, 0, ?, 0)", mbox.getId(), id++, MailItem.Type.MESSAGE.toByte(), beforeNow);    
        }
        id = 200;
        Set<Integer> idsAddBeforeNow = DbMailItem.getIds(mbox, conn, new QueryParams(), false);
        Assert.assertTrue(beforeNowCount == idsAddBeforeNow.size() - idsInit);
        for (int i = 0; i < afterNowCount; i++) {
            DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                    "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, change_date, mod_content) " +
                    "VALUES(?, ?, ?, 0, 0, 0, 0, 0, 0, ?, 0)", mbox.getId(), id++, MailItem.Type.MESSAGE.toByte(), afterNow);    
        }
        Set<Integer> idsAddAfterNow = DbMailItem.getIds(mbox, conn, new QueryParams(), false);
        Assert.assertTrue(afterNowCount == idsAddAfterNow.size() - idsAddBeforeNow.size());

        params = new QueryParams();
        params.setChangeDateBefore(now);
        Set<Integer> idsBeforeNow = DbMailItem.getIds(mbox, conn, params, false);
        Assert.assertTrue((idsBeforeNow.size()-idsInitBeforeNow.size()) == beforeNowCount);

        params = new QueryParams();
        params.setChangeDateAfter(now);
        Set<Integer> idsAfterNow = DbMailItem.getIds(mbox, conn, params, false);
        Assert.assertTrue((idsAfterNow.size()-idsInitAftereNow.size()) == afterNowCount);
    }
}