/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Arrays;
import org.junit.Ignore;
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
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ItemActionTest {
    private static final String tag1 = "foo", tag2 = "bar";

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
        new ItemAction().handle(request, ServiceTestUtil.getRequestContext(acct));

        Folder inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Assert.assertFalse("inbox doesn't have \\NoInherit", inbox.isTagged(Flag.FlagInfo.NO_INHERIT));
        Assert.assertNull("no ACL on inbox", inbox.getEffectiveACL());
    }

    @Test
    public void moveConversationToAcctRelativePath() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        int msgId = mbox.addMessage(null, pm, dopt, null).getId();

        String targetFolderName = "folder1";
        ItemActionHelper.MOVE(null, mbox, SoapProtocol.Soap12, Arrays.asList(msgId * -1), null, targetFolderName);
        Folder newFolder = mbox.getFolderByName(null, Mailbox.ID_FOLDER_USER_ROOT, targetFolderName);
        Message msg = mbox.getMessageById(null, msgId);
        Assert.assertEquals(msg.getFolderId(), newFolder.getId());
    }

    @Test
    public void copyMessageFromDraftsToSent() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject copyMessageFromDraftsToSent");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_DRAFTS);
        int msgId = mbox.addMessage(null, pm, dopt, null).getId();
        ItemId iidTarget = new ItemId(mbox, Mailbox.ID_FOLDER_SENT);
        ItemActionHelper op = ItemActionHelper.COPY(null, mbox, SoapProtocol.Soap12, Arrays.asList(msgId), MailItem.Type.MESSAGE, null, iidTarget);
        Assert.assertNotNull("test non-null response", op);
        Assert.assertTrue("test CopyActionResult", op.getResult() instanceof CopyActionResult);
        CopyActionResult copyActionResult = (CopyActionResult)op.getResult();
        Assert.assertNotNull("test non-null success info", copyActionResult.getSuccessIds());
        Assert.assertEquals("test correct success count", 1, copyActionResult.getSuccessIds().size());
        ItemId iid = new ItemId(copyActionResult.getCreatedIds().get(0), acct.getId());
        Assert.assertNotNull("test non-null created info", iid);
        Message copiedMessage = mbox.getMessageById(null, iid.getId());
        Assert.assertNotNull("test non-null message" ,copiedMessage);
        Assert.assertNotNull("test non-null subject in copied message", copiedMessage.getSubject());
        Assert.assertEquals("test subject copyMessageFromDraftsToSent", copiedMessage.getSubject());
        Assert.assertEquals("test parent folder of copied message", Mailbox.ID_FOLDER_SENT, copiedMessage.getFolderId());
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
        draft = mbox.getMessageById(null, draft.getId());
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
        parent = mbox.getMessageById(null, parent.getId());
        draft = mbox.getMessageById(null, draft.getId());
        draft2 = mbox.getMessageById(null, draft2.getId());
        Assert.assertEquals(parent.getFolderId(), Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(draft.getFolderId(), Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(draft2.getFolderId(), Mailbox.ID_FOLDER_TRASH);

        ItemActionHelper.HARD_DELETE(null, mbox, SoapProtocol.Soap12, Collections.singletonList(parent.getConversationId()), MailItem.Type.CONVERSATION, tcon);
        Exception ex = null;
        try {
            mbox.getMessageById(null, parent.getId());
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertTrue("test NoSuchItemException (parent/id)", ex instanceof NoSuchItemException);
        ex = null;
        try {
            mbox.getMessageById(null, draft.getId());
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertTrue("test NoSuchItemException (draft/id)", ex instanceof NoSuchItemException);
        ex = null;
        try {
            mbox.getMessageById(null, draft2.getId());
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertTrue("test NoSuchItemException (draft2/id)", ex instanceof NoSuchItemException);
        ex = null;
        try {
            mbox.getConversationById(null, draft2.getConversationId());
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertTrue("test NoSuchItemException (draft2/conversation)", ex instanceof NoSuchItemException);
    }

    @Test
    public void moveConversationPreviousFolderTest() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        // setup: add the root message
        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg1 = mbox.addMessage(null, pm, dopt, null);
        Folder.FolderOptions fopt = new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE);
        int folder1Id = mbox.createFolder(null, "folder1", fopt).getId();
        ItemId iid1 =  new ItemId(mbox, folder1Id);
        int folder2Id = mbox.createFolder(null, "folder2", fopt).getId();
        new ItemId(mbox, folder2Id);
        int folder3Id = mbox.createFolder(null, "folder3", fopt).getId();
        ItemId iid3 =  new ItemId(mbox, folder3Id);

        ItemActionHelper.MOVE(null, mbox, SoapProtocol.Soap12, Arrays.asList(msg1.getId()), MailItem.Type.MESSAGE, null, iid1);
        String msg1PrevFolder = mbox.getLastChangeID() + ":"+  Mailbox.ID_FOLDER_INBOX;
        Assert.assertEquals(msg1PrevFolder, msg1.getPrevFolders());

        dopt = new DeliveryOptions().setFolderId(folder2Id);
        Message msg2 = mbox.addMessage(null, MailboxTestUtil.generateMessage("Re: test subject"), dopt, null);
        Assert.assertNull(msg2.getPrevFolders());

        Element request = new Element.XMLElement(MailConstants.CONV_ACTION_REQUEST);
        request.addElement(MailConstants.E_ACTION).addAttribute(MailConstants.A_OPERATION, ItemAction.OP_MOVE).addAttribute(MailConstants.A_ID, msg1.getConversationId()).addAttribute("l", iid3.getId());
        new ConvAction().handle(request, ServiceTestUtil.getRequestContext(acct));
        msg1PrevFolder = msg1PrevFolder + ";" + mbox.getLastChangeID() + ":"+  folder1Id;
        String msg2PrevFolder = mbox.getLastChangeID() + ":"+  folder2Id;
        Assert.assertEquals(msg1PrevFolder, msg1.getPrevFolders());
        Assert.assertEquals(msg2PrevFolder, msg2.getPrevFolders());
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
        new ConvAction().handle(request, ServiceTestUtil.getRequestContext(acct));

        msg = mbox.getMessageById(null, msg.getId());
        Assert.assertFalse("root now read", msg.isUnread());
        Assert.assertTrue("root now muted", msg.isTagged(Flag.FlagInfo.MUTED));

        // unmute virtual conv
        action.addAttribute(MailConstants.A_OPERATION, "!" + ItemAction.OP_MUTE);
        new ConvAction().handle(request, ServiceTestUtil.getRequestContext(acct));

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
        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_MUTE).addAttribute(MailConstants.A_ID, msg2.getConversationId());
        new ConvAction().handle(request, ServiceTestUtil.getRequestContext(acct));

        msg2 = mbox.getMessageById(null, msg2.getId());
        Assert.assertFalse("reply now read", msg2.isUnread());
        Assert.assertTrue("reply now muted", msg2.isTagged(Flag.FlagInfo.MUTED));

        // unmute real conv
        action.addAttribute(MailConstants.A_OPERATION, "!" + ItemAction.OP_MUTE);
        new ConvAction().handle(request, ServiceTestUtil.getRequestContext(acct));

        msg2 = mbox.getMessageById(null, msg2.getId());
        Assert.assertFalse("reply still read", msg2.isUnread());
        Assert.assertFalse("reply now unmuted", msg2.isTagged(Flag.FlagInfo.MUTED));
    }

    @Test
    public void deleteAllTagKeepsStatusOfFlags() throws Exception {
        //Bug 76781
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name,
            "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        acct.setMailThreadingAlgorithm(MailThreadingAlgorithm.subject);

        // setup: add the root message
        ParsedMessage pm = MailboxTestUtil.generateMessage("test subject");
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(
            Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, pm, dopt, null);

        // add additional messages for conversation
        pm = MailboxTestUtil.generateMessage("Re: test subject");
        int msgId = mbox.addMessage(null, pm, dopt, null).getId();
        // set flag to unread for  this message
        MailboxTestUtil.setFlag(mbox, msgId, Flag.FlagInfo.UNREAD);


        MailItem item = mbox.getItemById(null, msgId, MailItem.Type.UNKNOWN);
        // verify message unread flag is set
        Assert.assertEquals("Verifying Unread flag is set.", Flag.BITMASK_UNREAD,
            item.getFlagBitmask());

        // add 2 tags
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tag1, true, null);
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tag2, true, null);


        Element request = new Element.XMLElement(
            MailConstants.ITEM_ACTION_REQUEST);
        Element action = request.addElement(MailConstants.E_ACTION);
        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_UPDATE);
        action.addAttribute(MailConstants.A_ITEM_TYPE, "");
        action.addAttribute(MailConstants.A_ID, msgId);

        new ItemAction().handle(request, ServiceTestUtil.getRequestContext(acct));
        Assert.assertEquals("Verifying unread flag is set after tag deletion",
            Flag.BITMASK_UNREAD,
            item.getFlagBitmask());

        Tag tag = mbox.getTagByName(null, tag1);
        Assert.assertEquals(tag1+ " (tag messages)", 0, tag.getSize());

        tag = mbox.getTagByName(null, tag2);
        Assert.assertEquals(tag1+ " (tag messages)", 0, tag.getSize());

    }

}
