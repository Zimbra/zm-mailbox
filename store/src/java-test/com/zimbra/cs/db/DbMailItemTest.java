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
package com.zimbra.cs.db;

import java.util.EnumSet;
import org.junit.Ignore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link DbMailItem}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class DbMailItemTest {

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
        int deleteNow = now + 2000;
        final int beforeNowCount = 9;
        final int afterNowCount = 13;
        final int notDeleteCount = 7;
        final int deleteCount = 17;
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

        id = 300;
        for (int i = 0; i < notDeleteCount; i++) {
            DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                    "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, change_date, mod_content) " +
                    "VALUES(?, ?, ?, 0, 0, 0, 0, 0, 0, ?, 0)", mbox.getId(), id++, MailItem.Type.MESSAGE.toByte(), deleteNow);
        }
        for (int i = 0; i < deleteCount; i++) {
            DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                    "(mailbox_id, id, type, index_id, date, size, flags, tags, mod_metadata, change_date, mod_content) " +
                    "VALUES(?, ?, ?, 0, 0, 0, 128, 0, 0, ?, 0)", mbox.getId(), id++, MailItem.Type.MESSAGE.toByte(), deleteNow);
        }
        params = new QueryParams();
        params.setChangeDateAfter(deleteNow-1);
        Set<Integer> idsForDelete = DbMailItem.getIds(mbox, conn, params, false);
        Assert.assertTrue(idsForDelete.size() == (deleteCount + notDeleteCount));

        params.setFlagToExclude(FlagInfo.DELETED);
        idsForDelete = DbMailItem.getIds(mbox, conn, params, false);
        Assert.assertTrue(idsForDelete.size() == notDeleteCount);
    }

    @Test
    public void readTombstones() throws Exception {
        int now = (int) (System.currentTimeMillis() / 1000);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.tombstone " +
                "(mailbox_id, sequence, date, type, ids) " +
                "VALUES(?, ?, ?, ?, ?)", mbox.getId(), 100, now, MailItem.Type.MESSAGE.toByte(), "1,2,3");
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.tombstone " +
                "(mailbox_id, sequence, date, type, ids) " +
                "VALUES(?, ?, ?, ?, ?)", mbox.getId(), 100, now, MailItem.Type.APPOINTMENT.toByte(), "11,12,13,14");
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.tombstone " +
                "(mailbox_id, sequence, date, type, ids) " +
                "VALUES(?, ?, ?, ?, ?)", mbox.getId(), 100, now, MailItem.Type.TASK.toByte(), "21,22,23,24,25");
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.tombstone " +
                "(mailbox_id, sequence, date, type, ids) " +
                "VALUES(?, ?, ?, ?, ?)", mbox.getId(), 100, now, MailItem.Type.CONTACT.toByte(), "31,32");
        Set<MailItem.Type> types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.MESSAGE);
        List<Integer> tombstones = DbMailItem.readTombstones(mbox, conn, 0, types);
        Assert.assertEquals(tombstones.size(), 3);
        types.add(MailItem.Type.APPOINTMENT);
        tombstones = DbMailItem.readTombstones(mbox, conn, 0, types);
        Assert.assertEquals(tombstones.size(), 7);

        types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.APPOINTMENT);
        tombstones = DbMailItem.readTombstones(mbox, conn, 0, types);
        Assert.assertEquals(tombstones.size(), 4);

        types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.TASK);
        tombstones = DbMailItem.readTombstones(mbox, conn, 0, types);
        Assert.assertEquals(tombstones.size(), 5);

        types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.CONTACT);
        tombstones = DbMailItem.readTombstones(mbox, conn, 0, types);
        Assert.assertEquals(tombstones.size(), 2);

        types = new HashSet<MailItem.Type>();
        types.add(MailItem.Type.MESSAGE);
        types.add(MailItem.Type.APPOINTMENT);
        types.add(MailItem.Type.TASK);
        tombstones = DbMailItem.readTombstones(mbox, conn, 0, types);
        Assert.assertEquals(tombstones.size(), 12);
    }
}
