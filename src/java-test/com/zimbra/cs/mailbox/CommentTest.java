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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mime.ParsedDocument;

public class CommentTest {

    private Mailbox mbox;
    private Folder folder;
    private MailItem doc;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>()).setDumpsterEnabled(true);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        folder = mbox.createFolder(null, "/Briefcase/f", (byte)0, Type.DOCUMENT);
        createDocument("doc.txt", "This is a document");
    }

    @Test
    public void deleteRecoverDocumentWithoutComments() throws Exception {
        // hard delete
        mbox.delete(null, doc.mId, MailItem.Type.DOCUMENT);
        List<MailItem> recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, folder.getId());
        Assert.assertEquals(recovered.size(), 1);
        doc = recovered.iterator().next();
    }

    @Test
    public void deleteRecoverDocumentWithComments() throws Exception {
        // hard delete
        createComments();
        mbox.delete(null, doc.mId, MailItem.Type.DOCUMENT);
        List<MailItem> recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, folder.getId());
        Assert.assertEquals(recovered.size(), 1);
        doc = recovered.iterator().next();

        Collection<Comment> comments = mbox.getComments(null, doc.mId, 0, 10);
        Assert.assertEquals(comments.size(), 3);
    }

    @Test
    public void emptyFolderRecoverDocumentsWithComments() throws Exception {
        // trash, empty trash
        createComments();
        mbox.move(null, doc.mId, MailItem.Type.DOCUMENT, Mailbox.ID_FOLDER_TRASH);
        mbox.emptyFolder(null, Mailbox.ID_FOLDER_TRASH, true);
        List<MailItem> recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, folder.getId());
        Assert.assertEquals(recovered.size(), 1);
        doc = recovered.iterator().next();

        Collection<Comment> comments = mbox.getComments(null, doc.mId, 0, 10);
        Assert.assertEquals(comments.size(), 3);
    }

    @Test
    public void deleteFolderRecoverDocumentsWithComments() throws Exception {
        // hard delete the parent folder
        createComments();
        mbox.delete(null, folder.mId, MailItem.Type.FOLDER);
        List<MailItem> recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, Mailbox.ID_FOLDER_BRIEFCASE);
        Assert.assertEquals(recovered.size(), 1);
        doc = recovered.iterator().next();

        Collection<Comment> comments = mbox.getComments(null, doc.mId, 0, 10);
        Assert.assertEquals(comments.size(), 3);
    }

    @Test
    public void deleteOwnComment() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "17dd075e-2b47-44e6-8cb8-7fdfa18c1a9f");
        Account grantee = prov.createAccount("grantee@zimbra.com", "secret", attrs);
        Account owner = prov.get(Key.AccountBy.name, "test@zimbra.com");
        mbox.grantAccess(new OperationContext(owner), folder.getId(), grantee.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwd"), null);
        Comment ownComment = mbox.createComment(new OperationContext(grantee), doc.mId, "own comment", grantee.getId());
        mbox.delete(new OperationContext(grantee), ownComment.mId, Type.COMMENT);
    }

    @Test
    public void deleteOthersComment() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "17dd075e-2b47-44e6-8cb8-7fdfa18c1a9f");
        Account grantee1 = prov.createAccount("grantee1@zimbra.com", "secret", attrs);
        attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "a4e41fbe-9c3e-4ab5-8b34-c42f17e251cd");
        Account grantee2 = prov.createAccount("grantee2@zimbra.com", "secret", attrs);
        Account owner = prov.get(Key.AccountBy.name, "test@zimbra.com");
        mbox.grantAccess(new OperationContext(owner), folder.getId(), grantee1.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwd"), null);
        mbox.grantAccess(new OperationContext(owner), folder.getId(), grantee2.getId(), ACL.GRANTEE_USER, ACL.stringToRights("rwd"), null);
        Comment ownComment = mbox.createComment(new OperationContext(grantee1), doc.mId, "grantee1's comment", grantee1.getId());
        try {
            mbox.delete(new OperationContext(grantee2), ownComment.mId, Type.COMMENT);
            Assert.fail("deleting someone else's comment succeeded");
        } catch (ServiceException se) {
            if (!se.getCode().equalsIgnoreCase(MailServiceException.PERM_DENIED)) {
                Assert.fail("exception thrown deleting comment "+se.getCode());
            }
        }
    }

    private void createDocument(String name, String content) throws Exception {
        InputStream in = new ByteArrayInputStream(content.getBytes());
        ParsedDocument pd = new ParsedDocument(in, name, "text/plain", System.currentTimeMillis(), null, null);
        doc = mbox.createDocument(null, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
    }

    private void createComments() throws Exception {
        mbox.createComment(null, doc.mId, "first comment", "test user 2");
        mbox.createComment(null, doc.mId, "comment #2", "the other guy");
        mbox.createComment(null, doc.mId, "numero tres", "some dude");
    }
}
