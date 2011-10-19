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

public class DumpsterTest {

    private Mailbox mbox;
    private Folder folder;
    
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
    public void recoverItem() throws Exception {
        MailItem doc = createDocument("doc1.txt", "This is a document");
        // move to trash
        mbox.move(null, doc.mId, MailItem.Type.DOCUMENT, Mailbox.ID_FOLDER_TRASH);
        doc = mbox.getItemById(null, doc.mId, MailItem.Type.DOCUMENT);
        // delete the item from trash to move to dumpster
        mbox.delete(null, doc.mId, MailItem.Type.DOCUMENT);
        // recover
        List<MailItem> recovered = mbox.recover(null, new int[] { doc.mId }, MailItem.Type.DOCUMENT, folder.getId());
        Assert.assertEquals(recovered.size(), 1);
    }

    // Verify items in dumpster are immutable.  You can't flag them, for example.
    @Test
    public void flagDumpsterItem() throws Exception {
        MailItem doc = createDocument("doc2.txt", "This is a document");
        // hard delete to move to dumpster
        mbox.delete(null, doc.mId, MailItem.Type.DOCUMENT);
        doc = mbox.getItemById(null, doc.mId, MailItem.Type.DOCUMENT, true);
        boolean success = false;
        boolean immutableException = false;
        try {
            mbox.beginTransaction("alterTag", null);
            doc.alterTag(Flag.FlagInfo.FLAGGED.toFlag(mbox), true);
            success = true;
        } catch (MailServiceException e) {
            immutableException = MailServiceException.IMMUTABLE_OBJECT.equals(e.getCode());
            if (!immutableException) {
                throw e;
            }
        } finally {
            mbox.endTransaction(success);
        }
        Assert.assertTrue("expected IMMUTABLE_OBJECT exception", immutableException);
    }

    private MailItem createDocument(String name, String content) throws Exception {
        InputStream in = new ByteArrayInputStream(content.getBytes());
        ParsedDocument pd = new ParsedDocument(in, name, "text/plain", System.currentTimeMillis(), null, null);
        return mbox.createDocument(null, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
    }
}
