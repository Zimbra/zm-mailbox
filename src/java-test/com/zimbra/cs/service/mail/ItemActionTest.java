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
package com.zimbra.cs.service.mail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.MailThreadingAlgorithm;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

public class ItemActionTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void inherit() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        mbox.grantAccess(null, Mailbox.ID_FOLDER_INBOX, acct2.getId(), ACL.GRANTEE_USER, ACL.RIGHT_READ, null);
        String targets = Mailbox.ID_FOLDER_INBOX + "," + Mailbox.ID_FOLDER_TRASH;

        Element request = new Element.XMLElement(MailConstants.ITEM_ACTION_REQUEST);
        request.addElement(MailConstants.E_ACTION).addAttribute(MailConstants.A_OPERATION, ItemAction.OP_INHERIT).addAttribute(MailConstants.A_ID, targets);
        new ItemAction().handle(request, GetFolderTest.getRequestContext(acct));

        Folder inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Assert.assertFalse("inbox doesn't have \\NoInherit", inbox.isTagged(Flag.FlagInfo.NO_INHERIT));
        Assert.assertNull("no ACL on inbox", inbox.getEffectiveACL());
    }

    @Test
    public void deleteIncompleteConversation() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        // setup: add the root message
        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int rootId = mbox.addMessage(null, pm, dopt, null).getId();

        // add additional messages
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        Message draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        Message parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals(parent.getConversationId(), draft.getConversationId());

        pm = MailboxTestUtil.generateMessage("Re: test subject");
        Message draft2 = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals(parent.getConversationId(), draft2.getConversationId());

        MailItem.TargetConstraint tcon = new MailItem.TargetConstraint(mbox, MailItem.TargetConstraint.INCLUDE_TRASH);
        ItemId iid = new ItemId(mbox, Mailbox.ID_FOLDER_TRASH);

        // trash one message in conversation
        ItemActionHelper.MOVE(null, mbox, SoapProtocol.Soap12, Collections.singletonList(draft.getId()), MailItem.Type.MESSAGE, tcon, iid);
        Assert.assertEquals(draft.getFolderId(), Mailbox.ID_FOLDER_TRASH);

        ItemActionHelper.HARD_DELETE(null, mbox, SoapProtocol.Soap12, Collections.singletonList(draft.getConversationId()), MailItem.Type.CONVERSATION, tcon);

        // the messages not in the trash should still exist and attached to the same conversation
        parent = mbox.getMessageById(null, rootId);
        Message m = mbox.getMessageById(null, draft2.getId());
        Assert.assertEquals(parent.getConversationId(), m.getConversationId());
    }

    @Test
    public void deleteConversation() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        // setup: add the root message
        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int rootId = mbox.addMessage(null, pm, dopt, null).getId();

        // add additional messages
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        Message draft = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, rootId + "", MailSender.MSGTYPE_REPLY, null, null, 0);
        Message parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals(parent.getConversationId(), draft.getConversationId());

        pm = MailboxTestUtil.generateMessage("Re: test subject");
        Message draft2 = mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT);
        parent = mbox.getMessageById(null, rootId);
        Assert.assertEquals(parent.getConversationId(), draft2.getConversationId());

        MailItem.TargetConstraint tcon = new MailItem.TargetConstraint(mbox, MailItem.TargetConstraint.INCLUDE_TRASH);
        ItemId iid = new ItemId(mbox, Mailbox.ID_FOLDER_TRASH);

        // trash the conversation
        ItemActionHelper.MOVE(null, mbox, SoapProtocol.Soap12, Arrays.asList(parent.getId(), draft.getId(), draft2.getId()), MailItem.Type.MESSAGE, tcon, iid);
        Assert.assertEquals(parent.getFolderId(), Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(draft.getFolderId(), Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(draft2.getFolderId(), Mailbox.ID_FOLDER_TRASH);

        ItemActionHelper.HARD_DELETE(null, mbox, SoapProtocol.Soap12, Collections.singletonList(parent.getConversationId()), MailItem.Type.CONVERSATION, tcon);
        Exception ex = null;
        try {
            mbox.getMessageById(null, parent.getId());
        } catch (Exception e) {
            ex = e;
            Assert.assertTrue(e instanceof NoSuchItemException);
        }
        Assert.assertNotNull(ex);
        ex = null;
        try {
            mbox.getMessageById(null, draft.getId());
        } catch (Exception e) {
            ex = e;
            Assert.assertTrue(e instanceof NoSuchItemException);
        }
        Assert.assertNotNull(ex);
        ex = null;
        try {
            mbox.getMessageById(null, draft2.getId());
        } catch (Exception e) {
            ex = e;
            Assert.assertTrue(e instanceof NoSuchItemException);
        }
        Assert.assertNotNull(ex);
        ex = null;
        try {
            mbox.getConversationById(null, draft2.getConversationId());
        } catch (Exception e) {
            ex = e;
            Assert.assertTrue(e instanceof NoSuchItemException);
        }
        Assert.assertNotNull(ex);
    }

    @Test
    public void mute() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // setup: add a message
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        Message msg = mbox.addMessage(null, MailboxTestUtil.generateMessage("test subject"), dopt, null);
        Assert.assertTrue("root unread", msg.isUnread());
        Assert.assertFalse("root not muted", msg.isTagged(Flag.FlagInfo.MUTED));

        // mute virtual conv
        Element request = new Element.XMLElement(MailConstants.CONV_ACTION_REQUEST);
        Element action = request.addElement(MailConstants.E_ACTION).addAttribute(MailConstants.A_OPERATION, ItemAction.OP_MUTE).addAttribute(MailConstants.A_ID, msg.getConversationId());
        new ConvAction().handle(request, GetFolderTest.getRequestContext(acct));

        msg = mbox.getMessageById(null, msg.getId());
        Assert.assertFalse("root now read", msg.isUnread());
        Assert.assertTrue("root now muted", msg.isTagged(Flag.FlagInfo.MUTED));

        // unmute virtual conv
        action.addAttribute(MailConstants.A_OPERATION, "!" + ItemAction.OP_MUTE);
        new ConvAction().handle(request, GetFolderTest.getRequestContext(acct));

        msg = mbox.getMessageById(null, msg.getId());
        Assert.assertFalse("root still read", msg.isUnread());
        Assert.assertFalse("root now unmuted", msg.isTagged(Flag.FlagInfo.MUTED));

        // add another message to create a real conv
        dopt.setConversationId(msg.getConversationId());
        Message msg2 = mbox.addMessage(null, MailboxTestUtil.generateMessage("Re: test subject"), dopt, null);
        Assert.assertTrue("reply unread", msg2.isUnread());
        Assert.assertFalse("reply not muted", msg2.isTagged(Flag.FlagInfo.MUTED));
        Assert.assertFalse("reply in real conv", msg2.getConversationId() < 0);

        // mute real conv
        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_MUTE).addAttribute(MailConstants.A_ID, msg2.getConversationId());;
        new ConvAction().handle(request, GetFolderTest.getRequestContext(acct));

        msg2 = mbox.getMessageById(null, msg2.getId());
        Assert.assertFalse("reply now read", msg2.isUnread());
        Assert.assertTrue("reply now muted", msg2.isTagged(Flag.FlagInfo.MUTED));

        // unmute real conv
        action.addAttribute(MailConstants.A_OPERATION, "!" + ItemAction.OP_MUTE);
        new ConvAction().handle(request, GetFolderTest.getRequestContext(acct));

        msg2 = mbox.getMessageById(null, msg2.getId());
        Assert.assertFalse("reply still read", msg2.isUnread());
        Assert.assertFalse("reply now unmuted", msg2.isTagged(Flag.FlagInfo.MUTED));
    }
}
