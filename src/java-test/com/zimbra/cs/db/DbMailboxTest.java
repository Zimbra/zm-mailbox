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

/**
 * Unit test for {@link DbMailbox}.
 *
 * @author ysasaki
 */
public class DbMailboxTest {

    private DbConnection connection;

    @BeforeClass
    public static void init() throws Exception {
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
