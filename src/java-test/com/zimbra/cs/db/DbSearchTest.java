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
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.index.DbLeafNode;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Unit test for {@link DbSearch}.
 *
 * @author ysasaki
 */
public final class DbSearchTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning prov = new MockProvisioning();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
    }

    @Test
    public void sortByAttachment() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DbConnection conn = DbPool.getConnection();
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_REPLIED);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 101, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_ATTACHED | Flag.BITMASK_REPLIED);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 102, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_REPLIED);

        List<DbSearch.Result> result = DbSearch.search(conn, mbox, new DbLeafNode(), SortBy.ATTACHMENT_ASC,
                0, 100, DbSearch.FetchMode.ID, false);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(100, result.get(0).getId());
        Assert.assertEquals(0, result.get(0).getSortValue());
        Assert.assertEquals(102, result.get(1).getId());
        Assert.assertEquals(0, result.get(1).getSortValue());
        Assert.assertEquals(101, result.get(2).getId());
        Assert.assertEquals(1, result.get(2).getSortValue());

        result = DbSearch.search(conn, mbox, new DbLeafNode(), SortBy.ATTACHMENT_DESC,
                0, 100, DbSearch.FetchMode.ID, false);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(101, result.get(0).getId());
        Assert.assertEquals(1, result.get(0).getSortValue());
        Assert.assertEquals(100, result.get(1).getId());
        Assert.assertEquals(0, result.get(1).getSortValue());
        Assert.assertEquals(102, result.get(2).getId());
        Assert.assertEquals(0, result.get(2).getSortValue());

        conn.closeQuietly();
    }

    @Test
    public void sortByFlag() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DbConnection conn = DbPool.getConnection();
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_REPLIED);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 101, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_FLAGGED | Flag.BITMASK_REPLIED);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 102, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_REPLIED);

        List<DbSearch.Result> result = DbSearch.search(conn, mbox, new DbLeafNode(), SortBy.FLAG_ASC,
                0, 100, DbSearch.FetchMode.ID, false);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(100, result.get(0).getId());
        Assert.assertEquals(0, result.get(0).getSortValue());
        Assert.assertEquals(102, result.get(1).getId());
        Assert.assertEquals(0, result.get(1).getSortValue());
        Assert.assertEquals(101, result.get(2).getId());
        Assert.assertEquals(1, result.get(2).getSortValue());

        result = DbSearch.search(conn, mbox, new DbLeafNode(), SortBy.FLAG_DESC,
                0, 100, DbSearch.FetchMode.ID, false);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(101, result.get(0).getId());
        Assert.assertEquals(1, result.get(0).getSortValue());
        Assert.assertEquals(100, result.get(1).getId());
        Assert.assertEquals(0, result.get(1).getSortValue());
        Assert.assertEquals(102, result.get(2).getId());
        Assert.assertEquals(0, result.get(2).getSortValue());;

        conn.closeQuietly();
    }

    @Test
    public void sortByPriority() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        DbConnection conn = DbPool.getConnection();
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 100, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_LOW_PRIORITY | Flag.BITMASK_REPLIED);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 101, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_HIGH_PRIORITY | Flag.BITMASK_REPLIED);
        DbUtil.executeUpdate(conn, "INSERT INTO mboxgroup1.mail_item " +
                "(mailbox_id, id, type, flags, date, size, tags, mod_metadata, mod_content) " +
                "VALUES(?, ?, ?, ?, 0, 0, 0, 0, 0)", mbox.getId(), 102, MailItem.Type.MESSAGE.toByte(),
                Flag.BITMASK_REPLIED);

        List<DbSearch.Result> result = DbSearch.search(conn, mbox, new DbLeafNode(), SortBy.PRIORITY_ASC,
                0, 100, DbSearch.FetchMode.ID, false);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(100, result.get(0).getId());
        Assert.assertEquals(-1, result.get(0).getSortValue());
        Assert.assertEquals(102, result.get(1).getId());
        Assert.assertEquals(0, result.get(1).getSortValue());
        Assert.assertEquals(101, result.get(2).getId());
        Assert.assertEquals(1, result.get(2).getSortValue());

        result = DbSearch.search(conn, mbox, new DbLeafNode(), SortBy.PRIORITY_DESC,
                0, 100, DbSearch.FetchMode.ID, false);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(101, result.get(0).getId());
        Assert.assertEquals(1, result.get(0).getSortValue());
        Assert.assertEquals(102, result.get(1).getId());
        Assert.assertEquals(0, result.get(1).getSortValue());
        Assert.assertEquals(100, result.get(2).getId());
        Assert.assertEquals(-1, result.get(2).getSortValue());

        conn.closeQuietly();
    }

}
