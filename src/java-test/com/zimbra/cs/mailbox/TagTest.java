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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

public class TagTest {

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

    private void checkInboxCounts(String msg, Mailbox mbox, int count, int unread, int deleted, int deletedUnread) throws Exception {
        Folder inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Assert.assertEquals(msg + " (folder messages)", count, inbox.getSize());
        Assert.assertEquals(msg + " (folder unread)", unread, inbox.getUnreadCount());
        Assert.assertEquals(msg + " (folder deleted)", deleted, inbox.getDeletedCount());
        Assert.assertEquals(msg + " (folder deleted unread)", deletedUnread, inbox.getDeletedUnreadCount());
    }

    private void checkTagCounts(String msg, Mailbox mbox, int tagId, int unread, int deletedUnread) throws Exception {
        Tag tag = mbox.getTagById(null, tagId);
        Assert.assertEquals(msg + " (tag unread)", unread, tag.getUnreadCount());
        Assert.assertEquals(msg + " (tag deleted unread)", deletedUnread, tag.getDeletedUnreadCount());
    }

    @Test
    public void markRead() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        checkInboxCounts("empty folder", mbox, 0, 0, 0, 0);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        int msgId = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt, null).getId();
        checkInboxCounts("added message", mbox, 1, 1, 0, 0);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.ID_DELETED, true);
        checkInboxCounts("marked message \\Deleted", mbox, 1, 1, 1, 1);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.ID_UNREAD, false);
        checkInboxCounts("marked message read", mbox, 1, 0, 1, 0);

        int tagId = mbox.createTag(null, "foo", (byte) 4).getId();
        checkTagCounts("created tag", mbox, tagId, 0, 0);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tagId, true);
        checkInboxCounts("tagged message", mbox, 1, 0, 1, 0);
        checkTagCounts("tagged message", mbox, tagId, 0, 0);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.ID_UNREAD, true);
        checkInboxCounts("marked message unread", mbox, 1, 1, 1, 1);
        checkTagCounts("marked message unread", mbox, tagId, 1, 1);

        mbox.alterTag(null, tagId, MailItem.Type.TAG, Flag.ID_UNREAD, false);
        checkInboxCounts("marked tag read", mbox, 1, 0, 1, 0);
        checkTagCounts("marked tag read", mbox, tagId, 0, 0);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.ID_UNREAD, true);
        checkInboxCounts("marked message unread", mbox, 1, 1, 1, 1);
        checkTagCounts("marked message unread", mbox, tagId, 1, 1);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.ID_DELETED, false);
        checkInboxCounts("unmarked message \\Deleted", mbox, 1, 1, 0, 0);
        checkTagCounts("unmarked message \\Deleted", mbox, tagId, 1, 0);

        mbox.alterTag(null, tagId, MailItem.Type.TAG, Flag.ID_UNREAD, false);
        checkInboxCounts("marked tag read", mbox, 1, 0, 0, 0);
        checkTagCounts("marked tag read", mbox, tagId, 0, 0);
    }
}
