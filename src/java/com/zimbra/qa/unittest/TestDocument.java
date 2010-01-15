/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.File;

import junit.framework.TestCase;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.Volume;

public class TestDocument extends TestCase {

    private static final String NAME_PREFIX = TestDocument.class.getSimpleName();
    private static final String USER_NAME = "user1";
    
    public void setUp()
    throws Exception {
        cleanUp();
    }
    
    /**
     * Server-side test that confirms that all blobs are cleaned up when
     * a document with multiple revisions is deleted.
     */
    public void testDeleteRevisions()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        
        // Create first revision.
        String content = "one";
        ParsedDocument pd = new ParsedDocument(
            new ByteArrayInputStream(content.getBytes()), "testDeleteRevisions.txt", "text/plain", System.currentTimeMillis(), USER_NAME);
        Document doc = mbox.createDocument(null, Mailbox.ID_FOLDER_BRIEFCASE, pd, MailItem.TYPE_DOCUMENT);
        int docId = doc.getId();
        byte type = doc.getType();
        File blobDir = getBlobDir(mbox, doc);
        assertEquals(1, mbox.getAllRevisions(null, docId, type).size());
        assertEquals(1, getBlobCount(blobDir, docId));
        
        // Add a second revision.
        content = "two";
        pd = new ParsedDocument(
            new ByteArrayInputStream(content.getBytes()), NAME_PREFIX + "-testDeleteRevisions.txt", "text/plain", System.currentTimeMillis(), USER_NAME);
        doc = mbox.addDocumentRevision(null, docId, type, pd);
        assertEquals(2, mbox.getAllRevisions(null, docId, type).size());
        assertEquals(2, getBlobCount(blobDir, docId));
        
        // Move to trash, empty trash, and confirm that both blobs were deleted.
        mbox.move(null, doc.getId(), doc.getType(), Mailbox.ID_FOLDER_TRASH);
        mbox.emptyFolder(null, Mailbox.ID_FOLDER_TRASH, false);
        assertEquals(0, getBlobCount(blobDir, docId));
    }
    
    private int getBlobCount(File dir, int id)
    throws Exception {
        int count = 0;
        String prefix = id + "-";
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }
    
    private File getBlobDir(Mailbox mbox, Document doc)
    throws Exception {
        Volume vol = Volume.getCurrentMessageVolume();
        MailboxBlob mblob = StoreManager.getInstance().getMailboxBlob(mbox, doc.getId(), doc.getModifiedSequence(), vol.getId());
        File blobFile = mblob.getBlob().getFile();
        return blobFile.getParentFile();
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        
        // Delete documents.
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        for (MailItem item : mbox.getItemList(null, MailItem.TYPE_DOCUMENT)) {
            mbox.delete(null, item.getId(), item.getType());
        }
    }
}
