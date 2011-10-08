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
package com.zimbra.cs.index.query;

import java.util.EnumSet;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;

/**
 * Unit test for {@link TextQuery}.
 *
 * @author ysasaki
 */
public final class TextQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void wildcardExpandedToNone() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        ZimbraQueryResults results = mbox.index.search(new OperationContext(mbox), "none*",
                EnumSet.of(MailItem.Type.MESSAGE), SortBy.NONE, 100);
        Assert.assertFalse(results.hasNext());

        results = mbox.index.search(new OperationContext(mbox), "from:none* AND subject:none*",
                EnumSet.of(MailItem.Type.MESSAGE), SortBy.NONE, 100);
        Assert.assertFalse(results.hasNext());

        results = mbox.index.search(new OperationContext(mbox), "from:none* OR subject:none*",
                EnumSet.of(MailItem.Type.MESSAGE), SortBy.NONE, 100);
        Assert.assertFalse(results.hasNext());
    }

}
