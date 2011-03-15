/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

/**
 * Unit test for {@link FieldQuery}.
 *
 * @author ysasaki
 */
public final class FieldQueryTest {
    private static Mailbox mailbox;

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-0");
        attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        prov.createAccount("test@zimbra.com", "secret", attrs);
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        mailbox = MailboxManager.getInstance().getMailboxByAccountId("0-0-0");
        MailboxIndex.startup();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
        Files.deleteDirectoryContents(new File("build/test/index"));
    }

    @Test
    public void textFieldQuery() throws Exception {
        Query query = FieldQuery.newQuery(mailbox, "company", "zimbra");
        Assert.assertEquals("Q(l.field,company:zimbra)", query.toString());
    }

    @Test
    public void numericFieldQuery() throws Exception {
        Query query = FieldQuery.newQuery(mailbox, "capacity", "3");
        Assert.assertEquals("Q(#capacity#:3)", query.toString());

        query = FieldQuery.newQuery(mailbox, "capacity", ">3");
        Assert.assertEquals("Q(#capacity#:>3)", query.toString());

        query = FieldQuery.newQuery(mailbox, "capacity", ">=3");
        Assert.assertEquals("Q(#capacity#:>=3)", query.toString());

        query = FieldQuery.newQuery(mailbox, "capacity", "<-3");
        Assert.assertEquals("Q(#capacity#:<-3)", query.toString());

        query = FieldQuery.newQuery(mailbox, "capacity", "<=-3");
        Assert.assertEquals("Q(#capacity#:<=-3)", query.toString());
    }

    @Test
    public void wildcard() throws Exception {
        Query query = FieldQuery.newQuery(mailbox, "firstname", "*");
        Assert.assertEquals("Q(l.field,firstname: *=firstname: [0 terms])", query.toString());
    }

}
