/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.google.common.collect.Maps;
import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.util.ZTestWatchman;

public class SearchTest {
    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new ZTestWatchman();
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());
        prov.createAccount("testZCS3705@zimbra.com", "secret", Maps.<String, Object>newHashMap());
    }

    @Test
    public void mute() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
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

    @Test
    public void testZCS3705() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("testZCS3705@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // add two messages - msg1 and msg2
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX)
            .setFlags(Flag.BITMASK_UNREAD | Flag.BITMASK_MUTED);
        Message msg1 = mbox.addMessage(null, MailboxTestUtil.generateMessage("read subject"), dopt,
            null);
        Message msg2 = mbox.addMessage(null, MailboxTestUtil.generateMessage("unread subject"),
            dopt, null);
        Assert.assertTrue("msg unread", msg1.isUnread());
        Assert.assertTrue("msg unread", msg2.isUnread());

        // read msg1
        Element request = new Element.XMLElement(MailConstants.GET_MSG_REQUEST);
        Element action = request.addElement(MailConstants.E_MSG);
        action.addAttribute(MailConstants.A_ID, msg1.getId());
        action.addAttribute(MailConstants.A_MARK_READ, 1);
        new GetMsg().handle(request, ServiceTestUtil.getRequestContext(mbox.getAccount()))
            .getElement(MailConstants.E_MSG);
        Assert.assertFalse("msg read", msg1.isUnread());
        Assert.assertTrue("msg unread", msg2.isUnread());

        // search for the conversation (sortBy readDesc) - msg2 should be listed before msg1
        Element searchRequest = new Element.XMLElement(MailConstants.SEARCH_REQUEST)
            .addAttribute(MailConstants.A_SEARCH_TYPES, "conversation");
        searchRequest.addAttribute(MailConstants.E_QUERY, "subject", Element.Disposition.CONTENT);
        searchRequest.addAttribute("sortBy", "readDesc");
        Element searchResponse = new Search().handle(searchRequest,
            ServiceTestUtil.getRequestContext(acct));
        List<Element> hits = searchResponse.listElements(MailConstants.E_CONV);
        Assert.assertEquals("2 hits", 2, hits.size());
        Assert.assertEquals("correct hit", msg2.getConversationId(),
            hits.get(0).getAttributeLong(MailConstants.A_ID));
        Assert.assertEquals("correct hit", msg1.getConversationId(),
            hits.get(1).getAttributeLong(MailConstants.A_ID));

        // search for the conversation (sortBy unreadDesc) - msg1 should be listed before msg2
        searchRequest = new Element.XMLElement(MailConstants.SEARCH_REQUEST)
            .addAttribute(MailConstants.A_SEARCH_TYPES, "conversation");
        searchRequest.addAttribute(MailConstants.E_QUERY, "subject", Element.Disposition.CONTENT);
        searchRequest.addAttribute("sortBy", "readAsc");
        searchResponse = new Search().handle(searchRequest,
            ServiceTestUtil.getRequestContext(acct));
        hits = searchResponse.listElements(MailConstants.E_CONV);
        Assert.assertEquals("2 hits", 2, hits.size());
        Assert.assertEquals("correct hit", msg1.getConversationId(),
            hits.get(0).getAttributeLong(MailConstants.A_ID));
        Assert.assertEquals("correct hit", msg2.getConversationId(),
            hits.get(1).getAttributeLong(MailConstants.A_ID));
    }

    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
