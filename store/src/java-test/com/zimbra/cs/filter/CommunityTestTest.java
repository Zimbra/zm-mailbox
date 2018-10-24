/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter;

import java.util.HashMap;
import org.junit.Ignore;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class CommunityTestTest {
    Account account;
    Mailbox mbox;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        RuleManager.clearCachedRules(account);
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);

    }

    public void doRequest(String rule, String headerValue, String tag) throws Exception {

        account.setMailSieveScript("if " + rule + " { tag \"" + tag +"\"; }");
        List<ItemId> ids = RuleManager.applyRulesToIncomingMessage(new OperationContext(mbox), mbox,
                new ParsedMessage(("From: \"in.Telligent\" <noreply@in.telligent.com>\n"
                        + "X-Zimbra-Community-Notification-Type: "+headerValue+"\n").getBytes(), false),
                0, account.getName(), new DeliveryContext(), Mailbox.ID_FOLDER_INBOX, true);
        Assert.assertEquals(1, ids.size());
        Message msg = mbox.getMessageById(null, ids.get(0).getId());
        Assert.assertEquals(tag, ArrayUtil.getFirstElement(msg.getTags()));
    }
    @Test
    public void testRequestNotifications() throws Exception {
        doRequest("community_requests", "bb196c30-fad3-4ad8-a644-2a0187fc5617", "request notifications");
    }

    @Test
    public void testContentNotifications() throws Exception {
        doRequest("community_content", "6a3659db-dec2-477f-981c-ada53603ccbb", "content notifications");
    }

    @Test
    public void testConnectionsNotifications() throws Exception {
        doRequest("community_connections", "194d3363-f5a8-43b4-a1bd-92a95f6dd76b", "connection notifications");
    }
}
