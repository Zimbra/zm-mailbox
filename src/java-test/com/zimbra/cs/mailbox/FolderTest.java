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
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.qa.unittest.TestUtil;

/**
 * Unit test for {@link Folder}.
 */
public final class FolderTest {

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

    private int checkMODSEQ(String msg, Mailbox mbox, int folderId, int lastMODSEQ) throws Exception {
        int modseq = mbox.getFolderById(null, folderId).getImapMODSEQ();
        Assert.assertTrue("modseq change after " + msg, modseq != lastMODSEQ);
        return modseq;
    }

    @Test
    public void imapMODSEQ() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // initial state: empty folder
        Folder f = mbox.createFolder(null, "foo", (byte) 0, MailItem.Type.MESSAGE);
        int folderId = f.getId(), modseq = f.getImapMODSEQ();

        // add a message to the folder
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setFlags(Flag.BITMASK_UNREAD);
        int msgId = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt, null).getId();
        modseq = checkMODSEQ("message add", mbox, folderId, modseq);

        // mark message read
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, false, null);
        modseq = checkMODSEQ("mark read", mbox, folderId, modseq);

        // move message out of folder
        mbox.move(null, msgId, MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_INBOX);
        modseq = checkMODSEQ("move msg out", mbox, folderId, modseq);

        // move message back into folder
        mbox.move(null, msgId, MailItem.Type.MESSAGE, folderId);
        modseq = checkMODSEQ("move msg in", mbox, folderId, modseq);

        // mark message answered
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.FlagInfo.REPLIED, true, null);
        modseq = checkMODSEQ("mark answered", mbox, folderId, modseq);

        // move virtual conversation out of folder
        mbox.move(null, -msgId, MailItem.Type.CONVERSATION, Mailbox.ID_FOLDER_INBOX);
        modseq = checkMODSEQ("move vconv out", mbox, folderId, modseq);

        // move virtual conversation back into folder
        mbox.move(null, -msgId, MailItem.Type.CONVERSATION, folderId);
        modseq = checkMODSEQ("move vconv in", mbox, folderId, modseq);

        // add a draft reply to the message (don't care about modseq change)
        ParsedMessage pm = new ParsedMessage(ThreaderTest.getSecondMessage(), false);
        mbox.saveDraft(null, pm, Mailbox.ID_AUTO_INCREMENT, Integer.toString(msgId), MailSender.MSGTYPE_REPLY, null, null, 0L);
        modseq = mbox.getFolderById(null, folderId).getImapMODSEQ();

        // move conversation out of folder
        int convId = mbox.getMessageById(null, msgId).getConversationId();
        mbox.move(null, convId, MailItem.Type.CONVERSATION, Mailbox.ID_FOLDER_INBOX);
        modseq = checkMODSEQ("move conv out", mbox, folderId, modseq);

        // move conversation back into folder
        mbox.move(null, convId, MailItem.Type.CONVERSATION, folderId);
        modseq = checkMODSEQ("move conv in", mbox, folderId, modseq);

        // tag message
        Tag tag = mbox.createTag(null, "taggity", (byte) 3);
        modseq = mbox.getFolderById(null, folderId).getImapMODSEQ();
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tag.getName(), true, null);
        modseq = checkMODSEQ("add tag", mbox, folderId, modseq);

        // rename tag
        mbox.rename(null, tag.getId(), MailItem.Type.TAG, "blaggity", Mailbox.ID_AUTO_INCREMENT);
        modseq = checkMODSEQ("rename tag", mbox, folderId, modseq);

        // untag message
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tag.getName(), false, null);
        modseq = checkMODSEQ("remove tag", mbox, folderId, modseq);

        // retag message
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tag.getName(), true, null);
        modseq = checkMODSEQ("re-add tag", mbox, folderId, modseq);

        // delete tag
        mbox.delete(null, tag.getId(), MailItem.Type.TAG);
        modseq = checkMODSEQ("tag delete", mbox, folderId, modseq);

        // hard delete message
        mbox.delete(null, msgId, MailItem.Type.MESSAGE);
        modseq = checkMODSEQ("hard delete", mbox, folderId, modseq);
    }

    @Test
    public void checkpointRECENT() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int changeId = mbox.getLastChangeID();
        Folder inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        int modMetadata = inbox.getModifiedSequence();
        int modContent = inbox.getSavedSequence();
        Assert.assertEquals(0, inbox.getImapRECENTCutoff());

        mbox.recordImapSession(Mailbox.ID_FOLDER_INBOX);

        inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Assert.assertEquals(changeId, mbox.getLastChangeID());
        Assert.assertEquals(modMetadata, inbox.getModifiedSequence());
        Assert.assertEquals(modContent, inbox.getSavedSequence());
        Assert.assertEquals(mbox.getLastItemId(), inbox.getImapRECENTCutoff());
    }

    @Test
    public void defaultFolderFlags() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setDefaultFolderFlags("*");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder inbox = mbox.getFolderById(Mailbox.ID_FOLDER_INBOX);
        Assert.assertTrue(inbox.isFlagSet(Flag.BITMASK_SUBSCRIBED));
    }

    @Test
    public void deleteFolder() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Folder root = mbox.createFolder(null, "/Root", (byte) 0, MailItem.Type.DOCUMENT);
        mbox.createFolder(null, "/Root/test1", (byte) 0, MailItem.Type.DOCUMENT);
        mbox.createFolder(null, "/Root/test2", (byte) 0, MailItem.Type.DOCUMENT);
        try {
            mbox.getFolderByPath(null, "/Root");
            mbox.getFolderByPath(null, "/Root/test1");
            mbox.getFolderByPath(null, "/Root/test2");
        } catch (Exception e) {
            Assert.fail();
        }

        // delete the root folder and make sure it and all the leaves are gone
        mbox.delete(null, root.mId, MailItem.Type.FOLDER);
        try {
            mbox.getFolderByPath(null, "/Root");
            Assert.fail();
        } catch (Exception e) {
        }
        try {
            mbox.getFolderByPath(null, "/Root/test1");
            Assert.fail();
        } catch (Exception e) {
        }
        try {
            mbox.getFolderByPath(null, "/Root/test2");
            Assert.fail();
        } catch (Exception e) {
        }
    }

    /**
     * Confirms that deleting a parent folder also deletes the child.
     */
    @Test
    public void deleteParent() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder parent = mbox.createFolder(null, "/" + "deleteParent - parent", (byte) 0, MailItem.Type.UNKNOWN);
        int parentId = parent.getId();
        Folder child = mbox.createFolder(null, "deleteParent - child", parent.getId(), MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mbox.delete(null, parent.getId(), parent.getType());

        // Look up parent by id
        try {
            mbox.getFolderById(null, parentId);
            Assert.fail("Parent folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        Assert.assertEquals("Parent folder query returned data.  id=" + parentId, 0, results.size());

        // Look up child by id
        try {
            mbox.getFolderById(null, childId);
            Assert.fail("Child folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        Assert.assertEquals("Child folder query returned data.  id=" + childId, 0, results.size());
    }

    /**
     * Confirms that emptying a folder removes subfolders only when requested.
     */
    @Test
    public void emptyFolderNonrecursive() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder parent = mbox.createFolder(null, "/" + "parent", (byte) 0, MailItem.Type.UNKNOWN);
        int parentId = parent.getId();
        Folder child = mbox.createFolder(null, "child", parent.getId(), MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mbox.emptyFolder(null, parent.getId(), false);

        // Look up parent by id
        mbox.getFolderById(null, parentId);

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        Assert.assertEquals("Parent folder query returned no data.  id=" + parentId, 1, results.size());

        // Look up child by id
        mbox.getFolderById(null, childId);

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        Assert.assertEquals("Child folder query returned no data.  id=" + childId, 1, results.size());
    }

    /**
     * Confirms that emptying a folder removes subfolders only when requested.
     */
    @Test
    public void testEmptyFolderRecursive() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder parent = mbox.createFolder(null, "/" + "parent", (byte) 0, MailItem.Type.UNKNOWN);
        int parentId = parent.getId();
        Folder child = mbox.createFolder(null, "child", parent.getId(), MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        int childId = child.getId();
        mbox.emptyFolder(null, parent.getId(), true);

        // Look up parent by id
        mbox.getFolderById(null, parentId);

        // Look up parent by query
        String sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + parentId;
        DbResults results = DbUtil.executeQuery(sql);
        Assert.assertEquals("Parent folder query returned no data.  id=" + parentId, 1, results.size());

        // Look up child by id
        try {
            mbox.getFolderById(null, childId);
            Assert.fail("Child folder lookup by id should have not succeeded");
        } catch (NoSuchItemException e) {
        }

        // Look up child by query
        sql =
            "SELECT id " +
            "FROM " + DbMailItem.getMailItemTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND id = " + childId;
        results = DbUtil.executeQuery(sql);
        Assert.assertEquals("Child folder query returned data.  id=" + childId, 0, results.size());
    }

    /**
     * Creates a hierarchy twenty folders deep.
     */
    @Test
    public void manySubfolders() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        final int NUM_LEVELS = 20;
        int parentId = Mailbox.ID_FOLDER_INBOX;
        Folder top = null;

        for (int i = 1; i <= NUM_LEVELS; i++) {
            Folder folder = mbox.createFolder(null, "manySubfolders " + i, parentId, MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
            if (i == 1) {
                top = folder;
            }
            parentId = folder.getId();
        }

        mbox.delete(null, top.getId(), top.getType());
    }

    /**
     * Deletes a folder that contains messages in a conversation.  Confirms
     * that the conversation size was correctly decremented.
     */
    @Test
    public void markDeletionTargets() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String name = "MDT";

        // Create three messages and move two of them into a new folder.
        Message m1 = TestUtil.addMessage(mbox, name);
        ZimbraLog.test.debug("Created message 1, id=" + m1.getId());
        Message m2 = TestUtil.addMessage(mbox, "RE: " + name);
        ZimbraLog.test.debug("Created message 2, id=" + m2.getId());
        Message m3 = TestUtil.addMessage(mbox, "RE: " + name);
        ZimbraLog.test.debug("Created message 3, id=" + m3.getId());

        Folder f = mbox.createFolder(null, name, Mailbox.ID_FOLDER_INBOX, MailItem.Type.UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        mbox.move(null, m1.getId(), m1.getType(), f.getId());
        mbox.move(null, m2.getId(), m2.getType(), f.getId());

        // Verify conversation size
        Conversation conv = mbox.getConversationById(null, m1.getConversationId());
        int convId = conv.getId();
        Assert.assertEquals("Conversation size before folder delete", 3, conv.getSize());

        // Delete the folder and confirm that the conversation size was decremented
        mbox.delete(null, f.getId(), f.getType());
        conv = mbox.getConversationById(null, convId);
        Assert.assertEquals("Conversation size after folder delete", 1, conv.getSize());
    }

    /**
     * Confirms that deleting a subfolder correctly updates the subfolder hierarchy.
     */
    @Test
    public void updateHierarchy() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Folder f1 = mbox.createFolder(null, "/f1", (byte) 0, MailItem.Type.UNKNOWN);
        Folder f2 = mbox.createFolder(null, "/f1/f2", (byte) 0, MailItem.Type.UNKNOWN);
        mbox.createFolder(null, "/f1/f2/f3", (byte) 0, MailItem.Type.UNKNOWN);
        Assert.assertEquals("Hierarchy size before delete", 3, f1.getSubfolderHierarchy().size());

        mbox.delete(null, f2.getId(), f2.getType());
        f1 = mbox.getFolderById(null, f1.getId());
        List<Folder> hierarchy = f1.getSubfolderHierarchy();
        Assert.assertEquals("Hierarchy size after delete", 1, hierarchy.size());
        Assert.assertEquals("Folder id", f1.getId(), hierarchy.get(0).getId());
    }

    private static void checkName(Mailbox mbox, String name, boolean valid) {
        try {
            mbox.createFolder(null, name, Mailbox.ID_FOLDER_USER_ROOT, MailItem.Type.DOCUMENT, 0, (byte) 0, null);
            if (!valid) {
                Assert.fail("should not have been allowed to create folder: [" + name + "]");
            }
        } catch (ServiceException e) {
            Assert.assertEquals("unexpected error code", MailServiceException.INVALID_NAME, e.getCode());
            if (valid) {
                Assert.fail("should have been allowed to create folder: [" + name + "]");
            }
        }
    }

    @Test
    public void names() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // empty or all-whitespace
        checkName(mbox, "", false);
        checkName(mbox, "   ", false);

        // invalid path characters
        checkName(mbox, "sam\rwise", false);
        checkName(mbox, "sam\nwise", false);
        checkName(mbox, "sam\twise", false);
        checkName(mbox, "sam\u0003wise", false);
        checkName(mbox, "sam\uFFFEwise", false);
        checkName(mbox, "sam\uDBFFwise", false);
        checkName(mbox, "sam\uDC00wise", false);
        checkName(mbox, "sam/wise", false);
        checkName(mbox, "sam\"wise", false);
        checkName(mbox, "sam:wise", false);

        // reserved names
        checkName(mbox, ".", false);
        checkName(mbox, "..", false);
        checkName(mbox, ".  ", false);
        checkName(mbox, ".. ", false);

        // valid path characters
        checkName(mbox, "sam\\wise", true);
        checkName(mbox, "sam'wise", true);
        checkName(mbox, "sam*wise", true);
        checkName(mbox, "sam|wise", true);
        checkName(mbox, "sam wise", true);
    }
}
