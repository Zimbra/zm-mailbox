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

package com.zimbra.cs.octosync;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedDocument;

/**
 * Unit test for {@link PatchInputStream}.
 *
 * @author grzes
 */
public final class PatchInputStreamTest
{

    @BeforeClass
    public static void init() throws Exception
    {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception
    {
        MailboxTestUtil.clearData();
    }

    @Test
    public void createNewDocOneDataNoRef() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();
        patchBuilder.addData("hello world");

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        PatchInputStream pis = PatchInputStream.create(is, mbox, null, 0, 0, null, null);

        Document doc = createDocument(mbox, "filename", pis);

        Assert.assertEquals("filename", doc.getName());
        Assert.assertEquals("hello world", doc.getFragment());
    }

    @Test
    public void createNewDocManyDataNoRef() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();
        patchBuilder.addData("hello world ");
        patchBuilder.addData("vmware rocks");
        patchBuilder.addData(" foo bar");

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        PatchInputStream pis = PatchInputStream.create(is, mbox, null, 0, 0, null, null);

        Document doc = createDocument(mbox, "filename", pis);

        Assert.assertEquals("filename", doc.getName());
        Assert.assertEquals("hello world vmware rocks foo bar", doc.getFragment());
    }

    @Test
    public void createNewDocRefNoData() throws Exception
    {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Document doc = createDocument(mbox, "filename", "hello world");

        Assert.assertEquals("filename", doc.getName());
        Assert.assertEquals("hello world", doc.getFragment());

        PatchBuilder patchBuilder = new PatchBuilder();

        PatchRef pref = new PatchRef();
        pref.fileId = doc.getId();
        pref.fileVersion = doc.getVersion();
        pref.offset = 6;
        pref.length = 5;
        pref.hashKey = patchBuilder.calcSHA256("world");

        patchBuilder.addRef(pref);

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());
        PatchInputStream pis = PatchInputStream.create(is, mbox, null, 0, 0, null, null);

        Document doc2 = createDocument(mbox, "filename2", pis);

        Assert.assertEquals("filename2", doc2.getName());
        Assert.assertEquals("world", doc2.getFragment());
    }

    @Test
    public void createNewRevisionRefAndData() throws Exception
    {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Document doc = createDocument(mbox, "filename", "hello world");

        Assert.assertEquals("filename", doc.getName());
        Assert.assertEquals("hello world", doc.getFragment());

        PatchBuilder patchBuilder = new PatchBuilder();

        PatchRef pref = new PatchRef();
        pref.fileId = doc.getId();
        pref.fileVersion = doc.getVersion();
        pref.offset = 0;
        pref.length = 5;
        pref.hashKey = patchBuilder.calcSHA256("hello");

        patchBuilder.addRef(pref);
        patchBuilder.addData(" foobar");

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());
        PatchInputStream pis = PatchInputStream.create(is, mbox, null, 0, 0, null, null);

        Document doc2 = addDocumentVersion(mbox, "filename", doc.getId(), pis);

        Assert.assertEquals("filename", doc2.getName());
        Assert.assertEquals("hello foobar", doc2.getFragment());
    }

    @Test(expected=InvalidPatchReferenceException.class)
    public void negativeZeroLengthRef() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();

        PatchRef pref = new PatchRef();
        pref.fileId = 1;
        pref.fileVersion = 1;
        pref.offset = 3;
        pref.length = 0;
        pref.hashKey = patchBuilder.calcSHA256("doesn't matter");

        patchBuilder.addRef(pref);
        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        PatchInputStream pis = PatchInputStream.create(is, mbox, null, 0, 0, null, null);

        createDocument(mbox, "filename", pis);
    }

    @Test(expected=BadPatchFormatException.class)
    public void negativeZeroLengthData() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();
        patchBuilder.addData("");

        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        PatchInputStream pis = PatchInputStream.create(is, mbox, null, 0, 0, null, null);

        createDocument(mbox, "filename", pis);
    }

    @Test(expected=InvalidPatchReferenceException.class)
    public void negativeInvalidRef() throws Exception
    {
        PatchBuilder patchBuilder = new PatchBuilder();

        PatchRef pref = new PatchRef();
        pref.fileId = 1;
        pref.fileVersion = 1;
        pref.offset = 0;
        pref.length = 10;
        pref.hashKey = patchBuilder.calcSHA256("doesn't matter");

        patchBuilder.addRef(pref);
        InputStream is = new ByteArrayInputStream(patchBuilder.toByteArray());

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        PatchInputStream pis = PatchInputStream.create(is, mbox, null, 0, 0, null, null);

        createDocument(mbox, "filename", pis);
    }

    private Document createDocument(Mailbox mbox, String name, InputStream in) throws Exception
    {
        ParsedDocument pd = new ParsedDocument(in, name, "text/plain", System.currentTimeMillis(), null, null);
        return mbox.createDocument(null, Mailbox.ID_FOLDER_BRIEFCASE, pd, MailItem.Type.DOCUMENT, 0);
    }

    private Document addDocumentVersion(Mailbox mbox, String name, int itemId, InputStream in) throws Exception
    {
        ParsedDocument pd = new ParsedDocument(in, name, "text/plain", System.currentTimeMillis(), null, null);
        return mbox.addDocumentRevision(null, itemId, pd);
    }

    private Document createDocument(Mailbox mbox, String name, String contents) throws Exception
    {
        return this.createDocument(mbox, name, new ByteArrayInputStream(contents.getBytes()));
    }
}
