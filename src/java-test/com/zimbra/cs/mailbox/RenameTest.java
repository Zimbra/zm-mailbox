/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.ParsedDocument;

public class RenameTest {
    private Mailbox mbox;
    private Folder folder;
    private MailItem doc;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        folder = mbox.createFolder(null, "/Briefcase/f", new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT));
        InputStream in = new ByteArrayInputStream("This is a document".getBytes());
        ParsedDocument pd = new ParsedDocument(in, "doc.txt", "text/plain", System.currentTimeMillis(), null, null);
        doc = mbox.createDocument(null, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
    }

    @Test
    public void renameModContentTest() throws Exception {
        int id = doc.getId();
        int mod_content = doc.getSavedSequence();
        mbox.rename(null, id, doc.getType(), "newdoc.txt", folder.getId());
        mbox.purge(MailItem.Type.UNKNOWN);
        MailItem newdoc = mbox.getItemById(null, id, MailItem.Type.UNKNOWN, false);
        Assert.assertEquals(mod_content, newdoc.getSavedSequence());
    }
}
