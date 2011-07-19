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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedContact;

/**
 * Unit test for {@link DbMailAddress}.
 *
 * @author ysasaki
 */
public final class DbMailAddressTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-1");
        prov.createAccount("test1@zimbra.com", "secret", attrs);
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-2");
        prov.createAccount("test2@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void getId() throws Exception {
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccountId("0-0-2");

        DbConnection conn = DbPool.getConnection();

        int id11 = new DbMailAddress(mbox1).setId(1).setAddress("test1@zimbra.com").setContactCount(1).create(conn);
        int id12 = new DbMailAddress(mbox1).setId(2).setAddress("test2@zimbra.com").setContactCount(1).create(conn);
        int id13 = new DbMailAddress(mbox1).setId(3).setAddress("test3@zimbra.com").setContactCount(1).create(conn);
        int id21 = new DbMailAddress(mbox2).setId(4).setAddress("test1@zimbra.com").setContactCount(1).create(conn);
        int id22 = new DbMailAddress(mbox2).setId(5).setAddress("test2@zimbra.com").setContactCount(1).create(conn);

        Assert.assertEquals(id11, DbMailAddress.getId(conn, mbox1, "test1@zimbra.com"));
        Assert.assertEquals(id12, DbMailAddress.getId(conn, mbox1, "test2@zimbra.com"));
        Assert.assertEquals(id13, DbMailAddress.getId(conn, mbox1, "test3@zimbra.com"));
        Assert.assertEquals(id21, DbMailAddress.getId(conn, mbox2, "test1@zimbra.com"));
        Assert.assertEquals(id22, DbMailAddress.getId(conn, mbox2, "test2@zimbra.com"));
        Assert.assertEquals(-1, DbMailAddress.getId(conn, mbox2, "unknown@zimbra.com"));

        conn.closeQuietly();
    }

    @Test
    public void existsInContacts() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");

        DbConnection conn = DbPool.getConnection();
        new DbMailAddress(mbox).setId(1).setAddress("test1@zimbra.com").setContactCount(0).create(conn);
        new DbMailAddress(mbox).setId(2).setAddress("test2@zimbra.com").setContactCount(1).create(conn);
        new DbMailAddress(mbox).setId(3).setAddress("test3@zimbra.com").setContactCount(2).create(conn);

        Set<String> result = DbMailAddress.existsInContacts(conn, mbox,
                Sets.newHashSet("test1@zimbra.com", "test2@zimbra.com", "test3@zimbra.com"));
        Assert.assertEquals(Sets.newHashSet("test2@zimbra.com", "test3@zimbra.com"), result);

        conn.closeQuietly();
    }

    @Test
    public void getLastId() throws Exception {
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccountId("0-0-2");

        DbConnection conn = DbPool.getConnection();

        new DbMailAddress(mbox1).setId(1).setAddress("test1@zimbra.com").setContactCount(1).create(conn);
        new DbMailAddress(mbox1).setId(2).setAddress("test2@zimbra.com").setContactCount(1).create(conn);
        new DbMailAddress(mbox1).setId(3).setAddress("test3@zimbra.com").setContactCount(1).create(conn);
        new DbMailAddress(mbox2).setId(4).setAddress("test1@zimbra.com").setContactCount(1).create(conn);
        new DbMailAddress(mbox2).setId(5).setAddress("test2@zimbra.com").setContactCount(1).create(conn);

        Assert.assertEquals(3, DbMailAddress.getLastId(conn, mbox1));
        Assert.assertEquals(5, DbMailAddress.getLastId(conn, mbox2));

        conn.closeQuietly();
    }

    @Test
    public void updateCount() throws Exception {
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccountId("0-0-2");

        DbConnection conn = DbPool.getConnection();

        int id1 = new DbMailAddress(mbox1).setId(1).setAddress("test1@zimbra.com").create(conn);
        int id2 = new DbMailAddress(mbox2).setId(2).setAddress("test1@zimbra.com").create(conn);

        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox1, id1));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox2, id2));

        DbMailAddress.incCount(conn, mbox1, id1);

        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox1, id1));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox2, id2));

        DbMailAddress.decCount(conn, mbox1, "test1@zimbra.com");

        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox1, id1));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox2, id2));

        conn.closeQuietly();
    }

    @Test
    public void delete() throws Exception {
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccountId("0-0-2");

        DbConnection conn = DbPool.getConnection();

        int id11 = new DbMailAddress(mbox1).setId(1).setAddress("test1@zimbra.com").create(conn);
        int id12 = new DbMailAddress(mbox1).setId(2).setAddress("test2@zimbra.com").create(conn);
        int id21 = new DbMailAddress(mbox2).setId(3).setAddress("test1@zimbra.com").create(conn);
        int id22 = new DbMailAddress(mbox2).setId(4).setAddress("test2@zimbra.com").create(conn);

        Assert.assertEquals(id11, DbMailAddress.getId(conn, mbox1, "test1@zimbra.com"));
        Assert.assertEquals(id12, DbMailAddress.getId(conn, mbox1, "test2@zimbra.com"));
        Assert.assertEquals(id21, DbMailAddress.getId(conn, mbox2, "test1@zimbra.com"));
        Assert.assertEquals(id22, DbMailAddress.getId(conn, mbox2, "test2@zimbra.com"));

        DbMailAddress.delete(conn, mbox1);

        Assert.assertEquals(-1, DbMailAddress.getId(conn, mbox1, "test1@zimbra.com"));
        Assert.assertEquals(-1, DbMailAddress.getId(conn, mbox1, "test2@zimbra.com"));
        Assert.assertEquals(id21, DbMailAddress.getId(conn, mbox2, "test1@zimbra.com"));
        Assert.assertEquals(id22, DbMailAddress.getId(conn, mbox2, "test2@zimbra.com"));

        conn.closeQuietly();
    }

    @Test
    public void purge() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");

        DbConnection conn = DbPool.getConnection();

        int id11 = new DbMailAddress(mbox).setId(1).setAddress("test1@zimbra.com").create(conn);
        new DbMailAddress(mbox).setId(2).setAddress("test2@zimbra.com").create(conn);

        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, sender_id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", mbox.getId(), 100, id11, 0, 0, 0, 0, 0, 0, 0, 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, sender_id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", mbox.getId(), 101, id11, 0, 0, 0, 0, 0, 0, 0, 0);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, sender_id, type, index_id, date, size, flags, tags, mod_metadata, mod_content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", mbox.getId(), 102, id11, 0, 0, 0, 0, 0, 0, 0, 0);

        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test2@zimbra.com"));
        Assert.assertEquals(1, DbMailAddress.purge(conn, mbox));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        Assert.assertEquals(-1, DbMailAddress.getCount(conn, mbox, "test2@zimbra.com"));

        conn.closeQuietly();
    }

    @Test
    public void rebuild() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        fields.put(ContactConstants.A_workEmail1, "test2@zimbra.com");
        fields.put(ContactConstants.A_workEmail2, "test3@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        DbConnection conn = DbPool.getConnection();

        DbMailItem.resetSenderId(conn, mbox);
        DbMailAddress.delete(conn, mbox);
        int lastAddressId = DbMailAddress.rebuild(conn, mbox, 0);
        Assert.assertEquals(3, lastAddressId);
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test2@zimbra.com"));
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test3@zimbra.com"));

        conn.closeQuietly();
    }

    @Test
    public void normailzeAddress() {
        Assert.assertEquals("test@zimbra.com", DbMailAddress.normalizeAddress(" TEST@ZIMBRA.COM "));
        try {
            DbMailAddress.normalizeAddress(null);
            Assert.fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            DbMailAddress.normalizeAddress("");
            Assert.fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            DbMailAddress.normalizeAddress(Strings.repeat("x", 128) + "@zimbra.com");
            Assert.fail();
        } catch (IllegalArgumentException expected) {
        }
    }

}
