/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
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
package com.zimbra.soap.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.soap.mail.message.GetFolderRequest;
import com.zimbra.soap.mail.message.SyncResponse;
import com.zimbra.soap.mail.type.ConvIdsAttr;
import com.zimbra.soap.mail.type.ConversationSummary;
import com.zimbra.soap.mail.type.IdsAttr;
import com.zimbra.soap.mail.type.MessageSummary;
import com.zimbra.soap.mail.type.MsgIdsAttr;
import com.zimbra.soap.mail.type.SyncDeletedInfo;
import com.zimbra.soap.mail.type.SyncFolder;

/**
 * Unit test for {@link GetFolderRequest}.
 */
public final class SyncResponseTest {

    private static Unmarshaller unmarshaller;

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(SyncResponse.class);
        unmarshaller = jaxb.createUnmarshaller();
    }

    @Test
    public void unmarshallSyncResponse() throws Exception {

        //expected items structure
        List<Object> expected = new ArrayList<Object>();
        MessageSummary msg = new MessageSummary("" + 1460);
        expected.add(msg);
        msg = new MessageSummary("" + 1463);
        expected.add(msg);
        msg = new MessageSummary("" + 1464);
        expected.add(msg);

        ConversationSummary conv = new ConversationSummary("" + 1461);
        expected.add(conv);

        List<IdsAttr> ids = new ArrayList<IdsAttr>();
        IdsAttr messageIds = new MsgIdsAttr("1462,1459,1458,1456,1446,1438,1435,1432,576,564,560");
        ids.add(messageIds);
        SyncFolder folder = new SyncFolder(ids);
        folder.setId(5);
        expected.add(folder);

        ids = new ArrayList<IdsAttr>();
        IdsAttr convIds = new ConvIdsAttr("1461,1457");
        ids.add(convIds);
        folder = new SyncFolder(ids);
        folder.setId(9);
        expected.add(folder);

        SyncResponse resp = (SyncResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("SyncResponse.xml"));

        Assert.assertEquals("1244", resp.getToken());
        List<Object> items = resp.getItems();

        //verify items
        Assert.assertEquals(expected.size(), items.size());

        for (Iterator<Object> it = expected.iterator(); it.hasNext(); ) {
            Object expect = it.next();
            MessageSummary expectedMsg = null;
            ConversationSummary expectedConv = null;
            SyncFolder expectedSyncFolder = null;
            if (expect instanceof MessageSummary) {
                expectedMsg = (MessageSummary) expect;
            } else if (expect instanceof ConversationSummary) {
                expectedConv = (ConversationSummary) expect;
            } else if (expect instanceof SyncFolder) {
                expectedSyncFolder = (SyncFolder) expect;
            }
            boolean matched = false;
            for (Object item : items) {
                if (expect.getClass() == item.getClass()) {
                    if (expectedMsg != null) {
                        msg = (MessageSummary) item;
                        if (expectedMsg.getId().equals(msg.getId())) {
                            it.remove();
                            matched = true;
                            break;
                        }
                    } else if (expectedConv != null) {
                        conv = (ConversationSummary) item;
                        if (expectedConv.getId().equals(conv.getId())) {
                            it.remove();
                            matched = true;
                            break;
                        }
                    } else if (expectedSyncFolder != null) {
                        folder = (SyncFolder) item;
                        if (expectedSyncFolder.getId().equals(folder.getId())) {
                            List<IdsAttr> expectedItems = expectedSyncFolder.getItemIds();
                            List<IdsAttr> folderItems = folder.getItemIds();
                            for (Iterator<IdsAttr> expectIdsIterator = expectedItems.iterator(); expectIdsIterator.hasNext(); ) {
                                IdsAttr expectedIdsAttr = expectIdsIterator.next();
                                for (IdsAttr encodedIds : folderItems) {
                                    if (encodedIds.getClass() == expectedIdsAttr.getClass()) {
                                        if (encodedIds.getIds().equals(expectedIdsAttr.getIds())) {
                                            expectIdsIterator.remove();
                                        }
                                    }
                                }
                            }
                            if (expectedItems.isEmpty()) {
                                it.remove();
                                matched = true;
                                break;
                            }
                        }
                    }
                }
            }
            Assert.assertTrue("item not found " + expected, matched);
        }
        Assert.assertTrue(expected.isEmpty());

        //typed deletes
        SyncDeletedInfo deleted = resp.getDeleted();
        Assert.assertEquals("1455,578,1448,1443,1451,1441,1454", deleted.getIds());
        List<IdsAttr> deletesByType = deleted.getTypes();
        //should be two
        boolean foundConvs = false;
        boolean foundMsg = false;
        for (IdsAttr deleteIds : deletesByType) {
            if (deleteIds instanceof ConvIdsAttr && deleteIds.getIds().equals("1455,999")) {
                foundConvs = true;
            } else if (deleteIds instanceof MsgIdsAttr && deleteIds.getIds().equals("578,1448,1443,1451,1441,1454")) {
                foundMsg = true;
            }
        }
        Assert.assertTrue(foundConvs);
        Assert.assertTrue(foundMsg);
    }
}
