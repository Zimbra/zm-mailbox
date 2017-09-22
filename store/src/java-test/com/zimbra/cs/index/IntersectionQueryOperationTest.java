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
package com.zimbra.cs.index;

import java.util.EnumSet;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * Unit test for {@link IntersectionQueryOperation}.
 *
 * @author ysasaki
 */
public final class IntersectionQueryOperationTest {

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
    public void optimize() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage("From: test1@zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test2@zimbra.com".getBytes(), false), dopt, null);
        mbox.addMessage(null, new ParsedMessage("From: test3@zimbra.com".getBytes(), false), dopt, null);

        SearchParams params = new SearchParams();
        params.setQueryString("in:inbox from:none*"); // wildcard
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setSortBy(SortBy.NONE);
        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        // this wildcard expansion results in no hits
        Assert.assertEquals("ZQ: Q(IN:Inbox) && Q(from:none*[*])", query.toString());
        // then intersection of something and no hits is always no hits
        Assert.assertEquals("((from:none*) AND IN:/Inbox )", query.toQueryString());
        ZimbraQueryResults results = query.execute();
        Assert.assertFalse(results.hasNext());
        results.close();

        params = new SearchParams();
        params.setQueryString("in:inbox content:the"); // stop-word
        params.setTypes(EnumSet.of(MailItem.Type.MESSAGE));
        params.setSortBy(SortBy.NONE);
        query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        Assert.assertEquals("ZQ: Q(IN:Inbox) && Q(l.content:)", query.toString());
        Assert.assertEquals("", query.toQueryString());
    }

}
