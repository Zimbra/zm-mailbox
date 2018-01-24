/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;

import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;


import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;



public class TestStoreManager {

    @Rule
    public TestName testInfo = new TestName();
    private static String USER_NAME = null;
    private static final String NAME_PREFIX = TestStoreManager.class.getSimpleName();

    public static ParsedMessage getMessage() throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", " Jimi <jimi@example.com>");
        mm.setHeader("To", " Janis <janis@example.com>");
        mm.setHeader("Subject", "Hello");
        mm.setHeader("Message-ID", "<sakfuslkdhflskjch@oiwm.example.com>");
        mm.setText("nothing to see here" + RandomStringUtils.random(1024));
        return new ParsedMessage(mm, false);
    }
	
    @Before
    public void setUp()
    throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user";
        cleanUp();
        TestUtil.createAccount(USER_NAME);
    }
	
    @After
    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp() 
	throws Exception{
        TestUtil.deleteAccountIfExists(USER_NAME);
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
