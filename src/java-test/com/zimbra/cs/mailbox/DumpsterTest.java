/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
        folder = mbox.createFolder(null, "/Briefcase/f", new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT));
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
