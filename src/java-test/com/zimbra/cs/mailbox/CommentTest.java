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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mime.ParsedDocument;

public class CommentTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>()).setDumpsterEnabled(true);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }
    
    @Test
    public void createDeleteComments() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.createFolder(null, "/Briefcase/f", (byte)0, Type.DOCUMENT);
        MailItem doc = createDocument(mbox, "doc.txt", "This is a document", folder);
        mbox.createComment(null, doc.mId, "first comment", "test user 2");
        mbox.createComment(null, doc.mId, "comment #2", "the other guy");
        mbox.createComment(null, doc.mId, "numero tres", "some dude");
        Collection<Comment> comments = mbox.getComments(null, doc.mId, 0, 10);
        Assert.assertEquals(comments.size(), 3);

        // hard delete
        mbox.delete(null, doc.mId, MailItem.Type.DOCUMENT);
        List<MailItem> recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, folder.getId());
        Assert.assertEquals(recovered.size(), 1);
        doc = recovered.iterator().next();
        
        comments = mbox.getComments(null, doc.mId, 0, 10);
        Assert.assertEquals(comments.size(), 3);
        
        // trash, empty trash
        mbox.move(null, doc.mId, MailItem.Type.DOCUMENT, Mailbox.ID_FOLDER_TRASH);
        mbox.emptyFolder(null, Mailbox.ID_FOLDER_TRASH, true);
        recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, folder.getId());
        Assert.assertEquals(recovered.size(), 1);
        doc = recovered.iterator().next();
        
        comments = mbox.getComments(null, doc.mId, 0, 10);
        Assert.assertEquals(comments.size(), 3);
        
        // hard delete the parent folder
        mbox.delete(null, folder.mId, MailItem.Type.FOLDER);
        recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, Mailbox.ID_FOLDER_BRIEFCASE);
        Assert.assertEquals(recovered.size(), 1);
        doc = recovered.iterator().next();
        
        comments = mbox.getComments(null, doc.mId, 0, 10);
        Assert.assertEquals(comments.size(), 3);
    }
    
    private Document createDocument(Mailbox mbox, String name, String content, Folder parent) throws Exception {
        InputStream in = new ByteArrayInputStream(content.getBytes());
        ParsedDocument pd = new ParsedDocument(in, name, "text/plain", System.currentTimeMillis(), null, null);
        return mbox.createDocument(null, parent.getId(), pd, MailItem.Type.DOCUMENT, 0);
    }
}
