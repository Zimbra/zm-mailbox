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
package com.zimbra.cs.service.formatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.util.ItemData;

public class ArchiveFormatterTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void tagDecode() throws Exception {
        Account acct = Provisioning.getInstance().get(AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        ItemData id = new ItemData(mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX));

        id.tags = null;
        String[] tags = ArchiveFormatter.getTagNames(null, mbox, id);
        Assert.assertNotNull(tags);
        Assert.assertEquals("null -> no tags", 0, tags.length);

        tags = new String[] { "foo" };
        id.tags = ItemData.getTagString(tags);
        Assert.assertEquals("single tag encoding", "foo", id.tags);
        Assert.assertArrayEquals("single tag", tags, ArchiveFormatter.getTagNames(null, mbox, id));

        tags = new String[] { "fo:o" };
        id.tags = ItemData.getTagString(tags);
        Assert.assertEquals("single tag encoding w/colon", "fo\\:o", id.tags);
        Assert.assertArrayEquals("single tag w/colon", tags, ArchiveFormatter.getTagNames(null, mbox, id));

        tags = new String[] { "foo", "bar" };
        id.tags = ItemData.getTagString(tags);
        Assert.assertEquals("two tag encoding", "foo:bar", id.tags);
        Assert.assertArrayEquals("two tags", tags, ArchiveFormatter.getTagNames(null, mbox, id));

        tags = new String[] { "fo:o", "ba\\r" };
        id.tags = ItemData.getTagString(tags);
        Assert.assertEquals("two tag encoding w/colon, backslash", "fo\\:o:ba\\\\r", id.tags);
        Assert.assertArrayEquals("two tags w/colon, backslash", tags, ArchiveFormatter.getTagNames(null, mbox, id));

        // old-style tag IDs
        int tagId = mbox.createTag(null, "tag1", (byte) 0).getId();
        id.tags = Integer.toString(tagId);
        Assert.assertArrayEquals("numeric tag ID", new String[] { "tag1" }, ArchiveFormatter.getTagNames(null, mbox, id));
    }
}
