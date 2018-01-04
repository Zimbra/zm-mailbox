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

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link DbMailbox}.
 *
 * @author ysasaki
 */
public class DbMailboxTest {

    private DbConnection connection;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.setInstance(new MockProvisioning());
        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        connection = DbPool.getConnection();
    }

    @After
    public void tearDown() throws Exception {
        connection.close();
    }

    @Test
    public void getMailboxRawData() throws Exception {
        Assert.assertEquals(0, DbMailbox.getMailboxRawData(connection).size());

        DbMailbox.createMailbox(connection, 100, "0", "test0", 0);
        DbMailbox.createMailbox(connection, 101, "1", "test1", 0);
        DbMailbox.createMailbox(connection, 102, "2", "test2", 0);

        List<Mailbox.MailboxData> list = DbMailbox.getMailboxRawData(connection);
        Assert.assertEquals(100, list.get(0).id);
        Assert.assertEquals("0", list.get(0).accountId);
        Assert.assertEquals(101, list.get(1).id);
        Assert.assertEquals("1", list.get(1).accountId);
        Assert.assertEquals(102, list.get(2).id);
        Assert.assertEquals("2", list.get(2).accountId);
    }

    @Test
    public void getMailboxCount() throws Exception {
        Integer mailboxCount = DbMailbox.getMailboxCount(connection);

        Assert.assertEquals(0, mailboxCount.intValue());

        DbMailbox.createMailbox(connection, 100, "0", "test0", 0);
        DbMailbox.createMailbox(connection, 101, "1", "test1", 0);
        DbMailbox.createMailbox(connection, 102, "2", "test2", 0);

        mailboxCount = DbMailbox.getMailboxCount(connection);

        Assert.assertEquals(3, mailboxCount.intValue());

    }

    @Test
    public void getMailboxKey() throws Exception {

        DbMailbox.createMailbox(connection, 100, "0", "test0", 0);

        Integer mailboxKey = DbMailbox.getMailboxKey(connection, "0");

        Assert.assertEquals(100, mailboxKey.intValue());

    }
    @Test
    public void getNullMailboxKey() throws Exception {

        Integer mailboxKey = DbMailbox.getMailboxKey(connection, "0");
        Assert.assertEquals(null,mailboxKey);


    }

    @Test
    public void listAccountIds() throws Exception {
        Assert.assertEquals(0, DbMailbox.listAccountIds(connection).size());

        DbMailbox.createMailbox(connection, 100, "0", "test0", 0);
        DbMailbox.createMailbox(connection, 101, "1", "test1", 0);
        DbMailbox.createMailbox(connection, 102, "2", "test2", 0);

        Set<String> ids = DbMailbox.listAccountIds(connection);
        Assert.assertEquals(3, ids.size());
        Assert.assertTrue(ids.contains("0"));
        Assert.assertTrue(ids.contains("1"));
        Assert.assertTrue(ids.contains("2"));
    }

}
