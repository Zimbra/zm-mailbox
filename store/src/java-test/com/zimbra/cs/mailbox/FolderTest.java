/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
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
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        // initial state: empty folder
        Folder f = mbox.createFolder(null, "foo", new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
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
        try {
            account.setDefaultFolderFlags("*");
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
            Folder inbox = mbox.getFolderById(Mailbox.ID_FOLDER_INBOX);
            Assert.assertTrue(inbox.isFlagSet(Flag.BITMASK_SUBSCRIBED));
        } finally {
            account.setDefaultFolderFlags(null); //don't leave account in modified state since other tests (such as create) assume no default flags
        }
    }

    @Test
    public void deleteFolder() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Folder.FolderOptions fopt = new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT);
        Folder root = mbox.createFolder(null, "/Root", fopt);
        mbox.createFolder(null, "/Root/test1", fopt);
        mbox.createFolder(null, "/Root/test2", fopt);
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
        Folder parent = mbox.createFolder(null, "/" + "deleteParent - parent", new Folder.FolderOptions());
        int parentId = parent.getId();
        Folder child = mbox.createFolder(null, "deleteParent - child", parent.getId(), new Folder.FolderOptions());
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
        Folder parent = mbox.createFolder(null, "/" + "parent", new Folder.FolderOptions());
        int parentId = parent.getId();
        Folder child = mbox.createFolder(null, "child", parent.getId(), new Folder.FolderOptions());
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
        Folder parent = mbox.createFolder(null, "/" + "parent", new Folder.FolderOptions());
        int parentId = parent.getId();
        Folder child = mbox.createFolder(null, "child", parent.getId(), new Folder.FolderOptions());
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
            Folder folder = mbox.createFolder(null, "manySubfolders " + i, parentId, new Folder.FolderOptions());
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

        Folder f = mbox.createFolder(null, name, Mailbox.ID_FOLDER_INBOX, new Folder.FolderOptions());
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

        Folder f1 = mbox.createFolder(null, "/f1", new Folder.FolderOptions());
        Folder f2 = mbox.createFolder(null, "/f1/f2", new Folder.FolderOptions());
        mbox.createFolder(null, "/f1/f2/f3", new Folder.FolderOptions());
        Assert.assertEquals("Hierarchy size before delete", 3, f1.getSubfolderHierarchy().size());

        mbox.delete(null, f2.getId(), f2.getType());
        f1 = mbox.getFolderById(null, f1.getId());
        List<Folder> hierarchy = f1.getSubfolderHierarchy();
        Assert.assertEquals("Hierarchy size after delete", 1, hierarchy.size());
        Assert.assertEquals("Folder id", f1.getId(), hierarchy.get(0).getId());
    }

    private static void checkName(Mailbox mbox, String name, boolean valid) {
        try {
            mbox.createFolder(null, name, Mailbox.ID_FOLDER_USER_ROOT, new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT));
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

    @Test
    public void create() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        final String uuid = UUIDUtil.generateUUID();
        final String url = "https://www.google.com/calendar/dav/YOUREMAIL@DOMAIN.COM/user";
        final long date = ((System.currentTimeMillis() - Constants.MILLIS_PER_MONTH) / 1000) * 1000;

        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setAttributes(Folder.FOLDER_DONT_TRACK_COUNTS);
        fopt.setColor((byte) 3);
        fopt.setCustomMetadata(new CustomMetadata("s", "d1:a1:be"));
        fopt.setDate(date);
        fopt.setDefaultView(MailItem.Type.CONTACT);
        fopt.setFlags(Flag.BITMASK_CHECKED);
        fopt.setUuid(uuid);
        // setting folder sync URL triggers an error in MockProvisioning; comment out for now
//        fopt.setUrl(url);

        // create the folder and make sure all the options were applied
        Folder folder = mbox.createFolder(null, "test", Mailbox.ID_FOLDER_CONTACTS, fopt);

        Assert.assertEquals("correct name", "test", folder.getName());
        Assert.assertEquals("correct parent", Mailbox.ID_FOLDER_CONTACTS, folder.getFolderId());
        Assert.assertEquals("correct attributes", Folder.FOLDER_DONT_TRACK_COUNTS, folder.getAttributes());
        Assert.assertEquals("correct color", 3, folder.getColor());
        CustomMetadata custom = folder.getCustomData("s");
        Assert.assertNotNull("custom data set", custom);
        Assert.assertEquals("1 entry in custom data", 1, custom.size());
        Assert.assertEquals("correct custom data", "b", custom.get("a"));
        Assert.assertEquals("correct date", date, folder.getDate());
        Assert.assertEquals("correct view", MailItem.Type.CONTACT, folder.getDefaultView());
        Assert.assertEquals("correct flags", Flag.BITMASK_CHECKED, folder.getFlagBitmask());
        Assert.assertEquals("correct uuid", uuid, folder.getUuid());
//        Assert.assertEquals("correct url", url, folder.getUrl());

        // check again after forcing a reload from disk, just in case
        mbox.purge(MailItem.Type.FOLDER);
        folder = mbox.getFolderById(null, folder.getId());

        Assert.assertEquals("correct name", "test", folder.getName());
        Assert.assertEquals("correct parent", Mailbox.ID_FOLDER_CONTACTS, folder.getFolderId());
        Assert.assertEquals("correct attributes", Folder.FOLDER_DONT_TRACK_COUNTS, folder.getAttributes());
        Assert.assertEquals("correct color", 3, folder.getColor());
        custom = folder.getCustomData("s");
        Assert.assertNotNull("custom data set", custom);
        Assert.assertEquals("1 entry in custom data", 1, custom.size());
        Assert.assertEquals("correct custom data", "b", custom.get("a"));
        Assert.assertEquals("correct date", date, folder.getDate());
        Assert.assertEquals("correct view", MailItem.Type.CONTACT, folder.getDefaultView());
        Assert.assertEquals("correct flags", Flag.BITMASK_CHECKED, folder.getFlagBitmask());
        Assert.assertEquals("correct uuid", uuid, folder.getUuid());
//        Assert.assertEquals("correct url", url, folder.getUrl());
    }
}
