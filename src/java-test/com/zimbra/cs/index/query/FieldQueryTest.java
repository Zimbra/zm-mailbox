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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link FieldQuery}.
 *
 * @author ysasaki
 */
public final class FieldQueryTest {
    private static Mailbox mailbox;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        mailbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void textFieldQuery() throws Exception {
        Query query = FieldQuery.create(mailbox, "company", "zimbra");
        Assert.assertEquals("Q(l.field:company:zimbra)", query.toString());
    }

    @Test
    public void numericFieldQuery() throws Exception {
        Query query = FieldQuery.create(mailbox, "capacity", "3");
        Assert.assertEquals("Q(#capacity#:3)", query.toString());

        query = FieldQuery.create(mailbox, "capacity", ">3");
        Assert.assertEquals("Q(#capacity#:>3)", query.toString());

        query = FieldQuery.create(mailbox, "capacity", ">=3");
        Assert.assertEquals("Q(#capacity#:>=3)", query.toString());

        query = FieldQuery.create(mailbox, "capacity", "<-3");
        Assert.assertEquals("Q(#capacity#:<-3)", query.toString());

        query = FieldQuery.create(mailbox, "capacity", "<=-3");
        Assert.assertEquals("Q(#capacity#:<=-3)", query.toString());
    }

    @Test
    public void wildcard() throws Exception {
        Query query = FieldQuery.create(mailbox, "firstname", "*");
        Assert.assertEquals("Q(l.field:firstname:*[*])", query.toString());
    }

}
