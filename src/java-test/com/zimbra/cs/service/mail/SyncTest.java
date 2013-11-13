/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTest;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;

public class SyncTest {
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
    public void tags() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        mbox.beginTrackingSync();

        // create one tag implicitly and one explicitly
        ParsedContact pc = new ParsedContact(ImmutableMap.of(ContactConstants.A_firstName, "Bob", ContactConstants.A_lastName, "Smith"));
        mbox.createContact(null, pc, Mailbox.ID_FOLDER_CONTACTS, new String[] { "bar" }).getId();
        int tagId = mbox.createTag(null, "foo", new Color((byte) 4)).getId();

        Element request = new Element.XMLElement(MailConstants.SYNC_REQUEST);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));
        Element response = new Sync().handle(request, context);
        String token = response.getAttribute(MailConstants.A_TOKEN);

        // check that only the explicitly-created tag is returned in the sync response
        Element tagFolder = null;
        for (Element hidden : response.getElement(MailConstants.E_FOLDER).listElements(MailConstants.E_FOLDER)) {
            if (hidden.getAttribute(MailConstants.A_NAME).equals("Tags")) {
                tagFolder = hidden;
                break;
            }
        }
        Assert.assertNotNull("could not find Tags folder in initial sync response", tagFolder);
        Assert.assertEquals("1 tag listed", 1, tagFolder.listElements(MailConstants.E_TAG).size());
        Element t = tagFolder.listElements(MailConstants.E_TAG).get(0);
        Assert.assertEquals("listed tag named 'foo'", "foo", t.getAttribute(MailConstants.A_NAME));
        Assert.assertEquals("correct color for tag 'foo'", 4, t.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR));

        // change tag color and re-sync
        mbox.setColor(null, tagId, MailItem.Type.TAG, (byte) 6);
//        mbox.purge(MailItem.Type.TAG);
        request = new Element.XMLElement(MailConstants.SYNC_REQUEST).addAttribute(MailConstants.A_TOKEN, token);

        response = new Sync().handle(request, context);
        Assert.assertFalse("sync token change after tag color", token.equals(response.getAttribute(MailConstants.A_TOKEN)));
        token = response.getAttribute(MailConstants.A_TOKEN);

        // make sure the modified tag is returned in the sync response
        t = response.getOptionalElement(MailConstants.E_TAG);
        Assert.assertNotNull("modified tag missing", t);
        Assert.assertEquals("new color on serialized tag", 6, t.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR));
    }

    @Test
    public void conversations() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        mbox.beginTrackingSync();


        // message and reply in inbox
        int msgId = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setConversationId(-msgId);
        Message msg = mbox.addMessage(null, MailboxTestUtil.generateMessage("Re: test subject"), dopt, null);
        int convId = msg.getConversationId();
        Set<Integer> expectedDeletes = Sets.newHashSet(convId, msgId, msg.getId());


        //initial sync
        Element request = new Element.XMLElement(MailConstants.SYNC_REQUEST);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));

        Element response = new Sync().handle(request, context);
        String token = response.getAttribute(MailConstants.A_TOKEN);

        Element rootFolder = response.getElement(MailConstants.E_FOLDER);
        List<Element> subFolders = rootFolder.listElements(MailConstants.E_FOLDER);
        boolean found = true;
        for (Element subFolder : subFolders) {
            if (subFolder.getAttributeInt(MailConstants.A_ID, 0) == Mailbox.ID_FOLDER_CONVERSATIONS) {
                Element conversations = subFolder.getElement(MailConstants.E_CONV);
                String ids = conversations.getAttribute(MailConstants.A_IDS);
                String[] convIds = ids.split(",");
                Assert.assertEquals(1, convIds.length);
                Assert.assertEquals(convId+"", convIds[0]);
                break;
            }
        }

        Assert.assertTrue("found expected converation", found);

        //delete original conv
        mbox.delete(null, convId, MailItem.Type.CONVERSATION);

        //new conv
        msgId = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject 2"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setConversationId(-msgId);
        convId = mbox.addMessage(null, MailboxTestUtil.generateMessage("Re: test subject 2"), dopt, null).getConversationId();


        //delta sync
        request = new Element.XMLElement(MailConstants.SYNC_REQUEST);
        request.addAttribute(MailConstants.A_TOKEN, token);
        response = new Sync().handle(request, context);

        Element conv = response.getElement(MailConstants.E_CONV);
        Assert.assertEquals(convId, conv.getAttributeInt(MailConstants.A_ID));

        Element deleted = response.getElement(MailConstants.E_DELETED);
        String ids = deleted.getAttribute(MailConstants.A_IDS);
        String[] deletedIds = ids.split(",");
        Assert.assertEquals(3, deletedIds.length);
        for (String delete : deletedIds) {
            Assert.assertTrue("id " + delete + " deleted", expectedDeletes.remove(Integer.valueOf(delete)));
        }

        Assert.assertTrue("all expected ids deleted", expectedDeletes.isEmpty());
    }
}
