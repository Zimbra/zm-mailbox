/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;

public class TestStoreManager extends TestCase {

    private static final String USER_NAME = "user1";

    public static ParsedMessage getMessage() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", " Jimi <jimi@example.com>");
        mm.setHeader("To", " Janis <janis@example.com>");
        mm.setHeader("Subject", "Hello");
        mm.setHeader("Message-ID", "<sakfuslkdhflskjch@oiwm.example.com>");
        mm.setText("nothing to see here" + RandomStringUtils.random(1024));
        return new ParsedMessage(mm, false);
    }

    @Test
    public void testStore() throws Exception {
        ParsedMessage pm = getMessage();
        byte[] mimeBytes = TestUtil.readInputStream(pm.getRawInputStream());

        Mailbox mbox = TestUtil.getMailbox(USER_NAME);

        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(pm.getRawInputStream());

        Assert.assertEquals("blob size = message size", pm.getRawData().length, blob.getRawSize());
        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(mimeBytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("link content = mime content", TestUtil.bytesEqual(mimeBytes, mblob.getLocalBlob().getInputStream()));

        mblob = sm.getMailboxBlob(mbox, 0, 0, staged.getLocator());
        Assert.assertEquals("mblob size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("mailboxblob content = mime content", TestUtil.bytesEqual(mimeBytes, mblob.getLocalBlob().getInputStream()));
    }
}
