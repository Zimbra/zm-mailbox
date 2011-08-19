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

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.ParsedMessage;

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
    public void testImapMODSEQ() throws Exception {
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
    public void deleteFolder() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder root = mbox.createFolder(null, "/Root", (byte) 0, MailItem.Type.DOCUMENT);
        mbox.createFolder(null, "/Root/test1", (byte) 0, MailItem.Type.DOCUMENT);
        mbox.createFolder(null, "/Root/test2", (byte) 0, MailItem.Type.DOCUMENT);

        Exception ex = null;
        try {
            mbox.getFolderByPath(null, "/Root");
            mbox.getFolderByPath(null, "/Root/test1");
            mbox.getFolderByPath(null, "/Root/test2");
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertNull(ex);
        mbox.delete(null, root.mId, MailItem.Type.FOLDER);
        try {
            mbox.getFolderByPath(null, "/Root");
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
        try {
            mbox.getFolderByPath(null, "/Root/test1");
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
        try {
            mbox.getFolderByPath(null, "/Root/test2");
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
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
}
