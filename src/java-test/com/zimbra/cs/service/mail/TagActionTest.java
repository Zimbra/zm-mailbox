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

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.util.TagUtil;

public class TagActionTest {
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

    private static final String name1 = "tag1", name2 = "tag2";

    private static String getTagIds(Tag... tags) {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) {
            sb.append(sb.length() == 0 ? "" : ",").append(tag.getId());
        }
        return sb.toString();
    }

    @Test
    public void byId() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        Tag tag1 = mbox.createTag(null, name1, (byte) 0);
        Tag tag2 = mbox.createTag(null, name2, (byte) 1);
        String tagids = getTagIds(tag1, tag2);

        Element request = new Element.XMLElement(MailConstants.TAG_ACTION_REQUEST);
        Element action = request.addElement(MailConstants.E_ACTION);
        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_COLOR).addAttribute(MailConstants.A_COLOR, 4);
        action.addAttribute(MailConstants.A_ID, tagids);
        Element ack = new TagAction().handle(request, GetFolderTest.getRequestContext(acct)).getElement(MailConstants.E_ACTION);

        Assert.assertEquals(name1 + " color set", 4, mbox.getTagByName(null, name1).getColor());
        Assert.assertEquals(name2 + " color set", 4, mbox.getTagByName(null, name2).getColor());

        Assert.assertEquals(tagids, ack.getAttribute(MailConstants.A_ID));
        Assert.assertEquals(TagUtil.encodeTags(name1, name2), ack.getAttribute(MailConstants.A_TAG_NAMES));
    }

    @Test
    public void byName() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        Tag tag1 = mbox.createTag(null, name1, (byte) 0);
        Tag tag2 = mbox.createTag(null, name2, (byte) 1);
        String tagnames = TagUtil.encodeTags(name1, name2);

        Element request = new Element.XMLElement(MailConstants.TAG_ACTION_REQUEST);
        Element action = request.addElement(MailConstants.E_ACTION);
        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_COLOR).addAttribute(MailConstants.A_COLOR, 4);
        action.addAttribute(MailConstants.A_TAG_NAMES, tagnames);
        Element ack = new TagAction().handle(request, GetFolderTest.getRequestContext(acct)).getElement(MailConstants.E_ACTION);

        Assert.assertEquals(name1 + " color set", 4, mbox.getTagByName(null, name1).getColor());
        Assert.assertEquals(name2 + " color set", 4, mbox.getTagByName(null, name2).getColor());

        Assert.assertEquals(getTagIds(tag1, tag2), ack.getAttribute(MailConstants.A_ID));
        Assert.assertEquals(tagnames, ack.getAttribute(MailConstants.A_TAG_NAMES));
    }

    @Test
    public void invalid() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        mbox.createTag(null, name1, (byte) 0);

        Element request = new Element.XMLElement(MailConstants.TAG_ACTION_REQUEST);
        Element action = request.addElement(MailConstants.E_ACTION);
        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_MOVE).addAttribute(MailConstants.A_FOLDER, Mailbox.ID_FOLDER_USER_ROOT);
        action.addAttribute(MailConstants.A_TAG_NAMES, TagUtil.encodeTags(name1));
        try {
            new TagAction().handle(request, GetFolderTest.getRequestContext(acct));
            Assert.fail("operation should not be permitted: " + ItemAction.OP_MOVE);
        } catch (ServiceException e) {
            Assert.assertEquals("expected error code: " + ServiceException.INVALID_REQUEST, ServiceException.INVALID_REQUEST, e.getCode());
        }

        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_RENAME).addAttribute(MailConstants.A_NAME, "tag3");
        action.addAttribute(MailConstants.A_TAG_NAMES, TagUtil.encodeTags(name2));
        try {
            new TagAction().handle(request, GetFolderTest.getRequestContext(acct));
            Assert.fail("allowed op on nonexistent tag");
        } catch (ServiceException e) {
            Assert.assertEquals("expected error code: " + MailServiceException.NO_SUCH_TAG, MailServiceException.NO_SUCH_TAG, e.getCode());
        }
    }

    @Test
    public void permissions() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        Tag tag1 = mbox.createTag(null, name1, (byte) 0);

        Element request = new Element.XMLElement(MailConstants.TAG_ACTION_REQUEST);
        Element action = request.addElement(MailConstants.E_ACTION);
        action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_COLOR).addAttribute(MailConstants.A_COLOR, 4);
        action.addAttribute(MailConstants.A_TAG_NAMES, TagUtil.encodeTags(name1));
        try {
            new TagAction().handle(request, GetFolderTest.getRequestContext(acct2, acct));
            Assert.fail("colored another user's tags without permissions");
        } catch (ServiceException e) {
            Assert.assertEquals("expected error code: " + ServiceException.PERM_DENIED, ServiceException.PERM_DENIED, e.getCode());
        }

        action.addAttribute(MailConstants.A_TAG_NAMES, (String) null).addAttribute(MailConstants.A_ID, tag1.getId());
        try {
            new TagAction().handle(request, GetFolderTest.getRequestContext(acct2, acct));
            Assert.fail("colored another user's tags without permissions");
        } catch (ServiceException e) {
            Assert.assertEquals("expected error code: " + ServiceException.PERM_DENIED, ServiceException.PERM_DENIED, e.getCode());
        }

        action.addAttribute(MailConstants.A_TAG_NAMES, TagUtil.encodeTags(name2));
        try {
            new TagAction().handle(request, GetFolderTest.getRequestContext(acct2, acct));
            Assert.fail("colored another user's tags without permissions");
        } catch (ServiceException e) {
            Assert.assertEquals("expected error code: " + ServiceException.PERM_DENIED, ServiceException.PERM_DENIED, e.getCode());
        }
    }
}
