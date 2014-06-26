/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;

public class SearchTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();

        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void mute() throws Exception {
        Account acct = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add a message
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD | Flag.BITMASK_MUTED);
        Message msg = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), dopt, null);
        Assert.assertTrue("root unread", msg.isUnread());
        Assert.assertTrue("root muted", msg.isTagged(Flag.FlagInfo.MUTED));

        // search for the conversation (normal)
        Element request = new Element.XMLElement(MailConstants.SEARCH_REQUEST).addAttribute(MailConstants.A_SEARCH_TYPES, "conversation");
        request.addAttribute(MailConstants.E_QUERY, "test", Element.Disposition.CONTENT);
        Element response = new Search().handle(request, ServiceTestUtil.getRequestContext(acct));

        List<Element> hits = response.listElements(MailConstants.E_CONV);
        Assert.assertEquals("1 hit", 1, hits.size());
        Assert.assertEquals("correct hit", msg.getConversationId(), hits.get(0).getAttributeLong(MailConstants.A_ID));

        // search for the conversation (no muted items)
        request.addAttribute(MailConstants.A_INCLUDE_TAG_MUTED, false);
        response = new Search().handle(request, ServiceTestUtil.getRequestContext(acct));

        hits = response.listElements(MailConstants.E_CONV);
        Assert.assertTrue("no hits", hits.isEmpty());
    }
}
