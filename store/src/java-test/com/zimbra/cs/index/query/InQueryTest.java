/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.query;

import java.util.HashMap;
import org.junit.Ignore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.util.ItemId;

/**
 * Unit test for {@link InQuery}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class InQueryTest {

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
    public void inAnyFolder() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Query query = InQuery.create(mbox, new ItemId(MockProvisioning.DEFAULT_ACCOUNT_ID, 1), null, true);
        Assert.assertEquals("Q(UNDER:ANY_FOLDER)", query.toString());

        query = InQuery.create(mbox, new ItemId(MockProvisioning.DEFAULT_ACCOUNT_ID, 1), null, false);
        Assert.assertEquals("Q(IN:USER_ROOT)", query.toString());

        query = InQuery.create(mbox, new ItemId("1-1-1", 1), null, true);
        Assert.assertEquals("Q(UNDER:1-1-1:1)", query.toString());

        query = InQuery.create(mbox, new ItemId("1-1-1", 1), null, false);
        Assert.assertEquals("Q(IN:1-1-1:1)", query.toString());
    }

}
