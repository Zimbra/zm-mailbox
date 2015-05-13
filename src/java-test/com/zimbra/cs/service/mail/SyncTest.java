/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
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
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.solr.MockSolrIndex;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTest;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.mail.Sync.SyncToken;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.SyncRequest;
import com.zimbra.soap.mail.message.SyncResponse;
import com.zimbra.soap.mail.type.MessageSummary;
import com.zimbra.soap.mail.type.SyncDeletedInfo;

public class SyncTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        /** overwrite index URL. It gets reset by MailboxTestUtil.initServer() */
        Provisioning.getInstance().getLocalServer().setIndexURL("mock:local");
        IndexStore.setFactory(MockSolrIndex.Factory.class.getName());
        
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        Sync.setMaximumChangeCount(1000);
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

    /**
     * Test Sync should not send duplicate deletes.
     * @throws Exception
     */
    @Test
    public void detaSyncTest() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.beginTrackingSync();
        SyncRequest request = new SyncRequest();
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));
        Element response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        SyncResponse syncRes = JaxbUtil.elementToJaxb(response);
        String token = syncRes.getToken();
        int msgId1 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId2= mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        request = new SyncRequest();
        request.setToken(token);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //delta set setMaximumChangeCount=2 Add 3 message and delete 2 message.
        Sync.setMaximumChangeCount(2);
        int msgId3 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.delete(null, msgId1, Type.MESSAGE);
        mbox.delete(null, msgId2, Type.MESSAGE);
        request = new SyncRequest();
        request.setToken(token);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //Expected all deletes and 2 added message. and hasMore=true
        SyncDeletedInfo sdi1 = syncRes.getDeleted();
        String [] deletes = sdi1.getIds().split(",");
        Assert.assertEquals(2, syncRes.getItems().size());
        Assert.assertEquals(2, deletes.length);

        mbox.delete(null, msgId3, Type.MESSAGE);
        request = new SyncRequest();
        request.setToken(token);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);

        // It should return 1 modified and 1 deleted.
        SyncDeletedInfo sdi2 = syncRes.getDeleted();
        deletes = sdi2.getIds().split(",");
        Assert.assertEquals(1, syncRes.getItems().size());
        Assert.assertEquals(1, deletes.length);
    }

    /**
     * Test Sync should not send duplicate deletes.
     * @throws Exception
     */
    @Test
    public void detaSyncMovedMessageTest() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.beginTrackingSync();
        SyncRequest request = new SyncRequest();
        request.setFolderId("2");
        Map<String, Object> context = new HashMap<String, Object>();
        Set<Integer> deleted = new HashSet<Integer>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));
        Element response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        SyncResponse syncRes = JaxbUtil.elementToJaxb(response);
        String token = syncRes.getToken();
        int msgId1 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId2= mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        request = new SyncRequest();
        request.setFolderId("2");
        request.setToken(token);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //delta set setMaximumChangeCount=2 Add 3 message and delete 2 message.
        Sync.setMaximumChangeCount(2);
        int msgId3 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.move(null, msgId1, Type.MESSAGE, 5);
        mbox.move(null, msgId2, Type.MESSAGE, 5);
        deleted.add(msgId1);
        deleted.add(msgId2);
        request = new SyncRequest();
        request.setToken(token);
        request.setFolderId("2");
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //Expected all deletes and 2 added message. and hasMore=true
        Assert.assertEquals(2, syncRes.getItems().size());
        SyncDeletedInfo sdi1 = syncRes.getDeleted();
        String [] deletes = sdi1.getIds().split(",");
        removeDeleteFromList(deleted, deletes);
        Assert.assertTrue(syncRes.getMore());
        Assert.assertTrue(deleted.isEmpty());

        // Move one more message.
        mbox.move(null, msgId3, Type.MESSAGE,5);
        request = new SyncRequest();
        request.setFolderId("2");
        request.setToken(token);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);

        // It should return 1 modified and 1 deleted.
        SyncDeletedInfo sdi2 = syncRes.getDeleted();
        deletes = sdi2.getIds().split(",");
        Assert.assertEquals(1, syncRes.getItems().size());
        Assert.assertEquals(1, deletes.length);
    }

    /**
     * Test Paginations.
     * 1. Modified paged , delete paged (del cutoff modseq <  modified cutoff mod Seq)
     * 2. All modified , delete paged.
     * 3. Modified paged , delete paged (del cutoff modseq >  modified cutoff mod Seq)
     * 4. Modified paged , all deletes.
     * 5. Modified and delete unpaged.
     * @throws Exception
     */
    @Test
    public void testPaginatedSync() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        mbox.beginTrackingSync();
        SyncRequest request = new SyncRequest();
        request.setFolderId("2");
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));
        Element response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        SyncResponse syncRes = JaxbUtil.elementToJaxb(response);
        Set<Integer> itemsDeleted = new HashSet<Integer>();
        Set<Integer> itemsAddedOrModified = new HashSet<Integer>();

        Sync.setMaximumChangeCount(100);
        String token = syncRes.getToken();
        int msgId1 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId2 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId3 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId4 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId5 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId6 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();

        itemsAddedOrModified.add(msgId1);
        itemsAddedOrModified.add(msgId2);
        itemsAddedOrModified.add(msgId3);
        itemsAddedOrModified.add(msgId4);
        itemsAddedOrModified.add(msgId5);
        itemsAddedOrModified.add(msgId6);
        request = new SyncRequest();
        request.setFolderId("2");
        request.setToken(token);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        List<Object> listObj = syncRes.getItems();
        removeItemsFromList(itemsAddedOrModified, listObj);
        Assert.assertTrue(itemsAddedOrModified.isEmpty());
        token = syncRes.getToken();

        // Modified paged , delete paged (del cutoff modseq < modified cutoff mod Seq)
        Sync.setMaximumChangeCount(2);
        mbox.move(null, msgId1, Type.MESSAGE, 5);
        mbox.move(null, msgId2, Type.MESSAGE, 5);
        mbox.delete(null, msgId3, Type.MESSAGE);
        mbox.alterTag(null, new int[] { msgId5, msgId6, msgId4}, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, true, null);
        itemsDeleted.add(msgId1);
        itemsDeleted.add(msgId2);
        itemsDeleted.add(msgId3);
        itemsAddedOrModified.add(msgId4);
        itemsAddedOrModified.add(msgId5);
        itemsAddedOrModified.add(msgId6);

        request = new SyncRequest();
        request.setToken(token);
        request.setFolderId("2");
        request.setDeleteLimit(2);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //Expected 2 deletes and 2 added message. and hasMore=true
        SyncDeletedInfo sdi1 = syncRes.getDeleted();
        String [] deletes = sdi1.getIds().split(",");
        Assert.assertEquals(2, deletes.length);
        removeDeleteFromList(itemsDeleted, deletes);
        Assert.assertEquals(1, itemsDeleted.size());//pending to sync.
        listObj = syncRes.getItems();
        Assert.assertEquals(2, listObj.size());
        removeItemsFromList(itemsAddedOrModified, listObj);
        Assert.assertEquals(1, itemsAddedOrModified.size()); //pending to sync.
        Assert.assertTrue(syncRes.getMore());
        SyncToken syncToken = new SyncToken(token);
        int lastChange = syncToken.getChangeModSeq();
        int lastDel = syncToken.getDeleteModSeq();
        Assert.assertTrue(lastDel < lastChange);

        // Test3: previous interrupted sync. All modified and delete paged.
        mbox.move(null, msgId5, Type.MESSAGE, 5);
        mbox.delete(null, msgId4, Type.MESSAGE);
        itemsDeleted.add(msgId4);
        itemsDeleted.add(msgId5);

        request = new SyncRequest();
        request.setToken(token);
        request.setFolderId("2");
        request.setDeleteLimit(2);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //Expected all modified and 2 deleted message. and hasMore=true
        SyncDeletedInfo sdi2 = syncRes.getDeleted();
        deletes = sdi2.getIds().split(",");
        Assert.assertEquals(2, deletes.length);
        removeDeleteFromList(itemsDeleted, deletes);
        Assert.assertEquals(1, itemsDeleted.size());//pending to sync.
        listObj = syncRes.getItems();
        Assert.assertEquals(1, listObj.size());
        removeItemsFromList(itemsAddedOrModified, listObj);
        Assert.assertEquals(0, itemsAddedOrModified.size()); //All synced.
        Assert.assertTrue(syncRes.getMore());
        syncToken = new SyncToken(token);
        lastChange = syncToken.getChangeModSeq();
        lastDel = syncToken.getDeleteModSeq();
        Assert.assertTrue(lastDel < lastChange);
        Assert.assertTrue(syncRes.getMore());

        // Test3: previous interrupted sync. modified and delete paged.(del cutoff modseq > modified cutoff mod Seq)
        int msgId7 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId8 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId9 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.alterTag(null, new int[] {msgId9}, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, true, null);
        mbox.delete(null, msgId6, Type.MESSAGE);
        itemsAddedOrModified.add(msgId7);
        itemsAddedOrModified.add(msgId8);
        itemsAddedOrModified.add(msgId9);
        itemsDeleted.add(msgId6);
        request = new SyncRequest();
        request.setToken(token);
        request.setFolderId("2");
        request.setDeleteLimit(1);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //Expected 1 deletes and 2 added message. and hasMore=true
        SyncDeletedInfo sdi3 = syncRes.getDeleted();
        deletes = sdi3.getIds().split(",");
        Assert.assertEquals(1, deletes.length);
        removeDeleteFromList(itemsDeleted, deletes);
        Assert.assertEquals(1, itemsDeleted.size());//pending to sync.
        listObj = syncRes.getItems();
        Assert.assertEquals(2, listObj.size());
        removeItemsFromList(itemsAddedOrModified, listObj);
        Assert.assertEquals(1, itemsAddedOrModified.size()); //pending to sync.
        Assert.assertTrue(syncRes.getMore());
        syncToken = new SyncToken(token);
        lastChange = syncToken.getChangeModSeq();
        lastDel = syncToken.getDeleteModSeq();
        Assert.assertTrue(lastDel > lastChange);
        Assert.assertTrue(syncRes.getMore());

     // Test4: previous interrupted sync. Modified paged and All Deletes.
        int msgId10 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId11 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        int msgId12 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.alterTag(null, new int[] {msgId9}, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, true, null);
        mbox.delete(null, msgId9, Type.MESSAGE);
        itemsAddedOrModified.add(msgId10);
        itemsAddedOrModified.add(msgId11);
        itemsAddedOrModified.add(msgId12);
        itemsAddedOrModified.remove(msgId9);
        itemsDeleted.add(msgId9);
        request = new SyncRequest();
        request.setToken(token);
        request.setFolderId("2");
        request.setDeleteLimit(2);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //Expected 2 deletes and 2 added message. and hasMore=true.
        //trick: deleted the remaining to sync modified message from last sync.
        SyncDeletedInfo sdi4 = syncRes.getDeleted();
        deletes = sdi4.getIds().split(",");
        Assert.assertEquals(2, deletes.length);
        removeDeleteFromList(itemsDeleted, deletes);
        Assert.assertEquals(0, itemsDeleted.size());//All synced.
        listObj = syncRes.getItems();
        Assert.assertEquals(2, listObj.size());
        removeItemsFromList(itemsAddedOrModified, listObj);
        Assert.assertEquals(1, itemsAddedOrModified.size()); //pending to sync.
        Assert.assertTrue(syncRes.getMore());
        syncToken = new SyncToken(token);
        lastChange = syncToken.getChangeModSeq();
        lastDel = syncToken.getDeleteModSeq();
        Assert.assertTrue(lastDel == mbox.getLastChangeID());
        Assert.assertTrue(syncRes.getMore());

        //Test5: All deletes and all Modified.
        int msgId13 = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), MailboxTest.STANDARD_DELIVERY_OPTIONS, null).getId();
        mbox.alterTag(null, new int[] {msgId11}, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, true, null);
        mbox.delete(null, msgId10, Type.MESSAGE);
        itemsAddedOrModified.add(msgId13);
        itemsAddedOrModified.add(msgId11);
        itemsDeleted.add(msgId10);

        Sync.setMaximumChangeCount(10);
        request = new SyncRequest();
        request.setToken(token);
        request.setFolderId("2");
        request.setDeleteLimit(0);
        response = new Sync().handle(JaxbUtil.jaxbToElement(request), context);
        syncRes = JaxbUtil.elementToJaxb(response);
        token = syncRes.getToken();

        //Expected 2 deletes and 2 added message. and hasMore=true.
        //trick: deleted the remaining to sync modified message from last sync.
        SyncDeletedInfo sdi5 = syncRes.getDeleted();
        deletes = sdi5.getIds().split(",");
        Assert.assertEquals(1, deletes.length);
        removeDeleteFromList(itemsDeleted, deletes);
        Assert.assertEquals(0, itemsDeleted.size());// All synced.
        listObj = syncRes.getItems();
        Assert.assertEquals(3, listObj.size());
        removeItemsFromList(itemsAddedOrModified, listObj);
        Assert.assertEquals(0, itemsAddedOrModified.size()); //All synced.
        syncToken = new SyncToken(token);
        lastChange = syncToken.getChangeModSeq();
        lastDel = syncToken.getDeleteModSeq();
        Assert.assertTrue(lastChange == mbox.getLastChangeID());
        Assert.assertTrue(lastDel == -1);
        Assert.assertNull(syncRes.getMore());
    }

    @Test
    public void syncTokenParseTest() throws Exception {
        String token = "123";
        Sync.SyncToken synctoken = new Sync.SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeModSeq());
        Assert.assertEquals(token, synctoken.toString());

        token = "123-032";
        synctoken = new Sync.SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeModSeq());
        Assert.assertEquals(32, synctoken.getChangeItemId());
        Assert.assertEquals("123-32", synctoken.toString());

        token = "123-032:d0345-908";
        synctoken = new Sync.SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeModSeq());
        Assert.assertEquals(32, synctoken.getChangeItemId());
        Assert.assertEquals(345, synctoken.getDeleteModSeq());
        Assert.assertEquals(908, synctoken.getDeleteItemId());
        Assert.assertEquals("123-32:d345-908", synctoken.toString());

        token = "123:d0345-908";
        synctoken = new Sync.SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeModSeq());
        Assert.assertEquals(345, synctoken.getDeleteModSeq());
        Assert.assertEquals(908, synctoken.getDeleteItemId());
        Assert.assertEquals("123:d345-908", synctoken.toString());

        token = "123:d0345";
        synctoken = new Sync.SyncToken(token);
        Assert.assertEquals(123, synctoken.getChangeModSeq());
        Assert.assertEquals(345, synctoken.getDeleteModSeq());
        Assert.assertEquals("123:d345", synctoken.toString());
    }

    private static void removeDeleteFromList(Set<Integer> deleted, String [] deletes) {
        for (String del : deletes) {
            deleted.remove(Integer.valueOf(del));
        }
    }

    private static void removeItemsFromList(Set<Integer> modified, List<Object> listObj) {
        for (Object object : listObj) {
            MessageSummary ms = (MessageSummary) object;
            modified.remove(Integer.valueOf(ms.getId()));
        }
    }

}
