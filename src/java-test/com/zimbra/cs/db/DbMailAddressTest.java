/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Unit test for {@link DbMailAddress}.
 *
 * @author ysasaki
 */
public final class DbMailAddressTest {

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-1");
        prov.createAccount("test1@zimbra.com", "secret", attrs);
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-2");
        prov.createAccount("test2@zimbra.com", "secret", attrs);
        Provisioning.setInstance(prov);

        DebugConfig.numMailboxGroups = 1;
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
    public void getId() throws Exception {
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccountId("0-0-2");

        DbConnection conn = DbPool.getConnection();

        int id11 = DbMailAddress.save(conn, mbox1, "test1@zimbra.com", 1);
        int id12 = DbMailAddress.save(conn, mbox1, "test2@zimbra.com", 1);
        int id13 = DbMailAddress.save(conn, mbox1, "test3@zimbra.com", 1);
        int id21 = DbMailAddress.save(conn, mbox2, "test1@zimbra.com", 1);
        int id22 = DbMailAddress.save(conn, mbox2, "test2@zimbra.com", 1);

        Assert.assertEquals(id11, DbMailAddress.getId(conn, mbox1, "test1@zimbra.com"));
        Assert.assertEquals(id12, DbMailAddress.getId(conn, mbox1, "test2@zimbra.com"));
        Assert.assertEquals(id13, DbMailAddress.getId(conn, mbox1, "test3@zimbra.com"));
        Assert.assertEquals(id21, DbMailAddress.getId(conn, mbox2, "test1@zimbra.com"));
        Assert.assertEquals(id22, DbMailAddress.getId(conn, mbox2, "test2@zimbra.com"));
        Assert.assertEquals(-1, DbMailAddress.getId(conn, mbox2, "unknown@zimbra.com"));

        DbPool.quietClose(conn);
    }

    @Test
    public void updateCount() throws Exception {
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccountId("0-0-2");

        DbConnection conn = DbPool.getConnection();

        int id1 = DbMailAddress.save(conn, mbox1, "test1@zimbra.com", 0);
        int id2 = DbMailAddress.save(conn, mbox2, "test1@zimbra.com", 0);

        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox1, id1));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox2, id2));

        DbMailAddress.incCount(conn, mbox1, id1);

        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox1, id1));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox2, id2));

        DbMailAddress.decCount(conn, mbox1, "test1@zimbra.com");

        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox1, id1));
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox2, id2));

        DbPool.quietClose(conn);
    }

    @Test
    public void delete() throws Exception {
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccountId("0-0-1");
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccountId("0-0-2");
    
        DbConnection conn = DbPool.getConnection();
    
        int id11 = DbMailAddress.save(conn, mbox1, "test1@zimbra.com", 0);
        int id12 = DbMailAddress.save(conn, mbox1, "test2@zimbra.com", 0);
        int id21 = DbMailAddress.save(conn, mbox2, "test1@zimbra.com", 0);
        int id22 = DbMailAddress.save(conn, mbox2, "test2@zimbra.com", 0);
    
        Assert.assertEquals(id11, DbMailAddress.getId(conn, mbox1, "test1@zimbra.com"));
        Assert.assertEquals(id12, DbMailAddress.getId(conn, mbox1, "test2@zimbra.com"));
        Assert.assertEquals(id21, DbMailAddress.getId(conn, mbox2, "test1@zimbra.com"));
        Assert.assertEquals(id22, DbMailAddress.getId(conn, mbox2, "test2@zimbra.com"));
    
        DbMailAddress.delete(conn, mbox1);
    
        Assert.assertEquals(-1, DbMailAddress.getId(conn, mbox1, "test1@zimbra.com"));
        Assert.assertEquals(-1, DbMailAddress.getId(conn, mbox1, "test2@zimbra.com"));
        Assert.assertEquals(id21, DbMailAddress.getId(conn, mbox2, "test1@zimbra.com"));
        Assert.assertEquals(id22, DbMailAddress.getId(conn, mbox2, "test2@zimbra.com"));
    
        DbPool.quietClose(conn);
    }

}
