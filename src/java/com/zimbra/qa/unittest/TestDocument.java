/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.client.ZDocument;
import com.zimbra.client.ZItem;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchParams;

public class TestDocument extends TestCase {

    private static final String NAME_PREFIX = TestDocument.class.getSimpleName();
    private static final String USER_NAME = "user1";

    @Override public void setUp()
    throws Exception {
        cleanUp();
    }
    
    /**
     * Tests documents created with the {@code Note} flag set.
     */
    public void testNote()
    throws Exception {
        // Create a document and a note.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String folderId = Integer.toString(Mailbox.ID_FOLDER_BRIEFCASE);
        ZDocument doc = TestUtil.createDocument(mbox, folderId, NAME_PREFIX + "-doc.txt", "text/plain", "doc".getBytes());
        ZDocument note = TestUtil.createDocument(mbox, folderId, NAME_PREFIX + "-note.txt", "text/plain", "note".getBytes(), true);
        String flags = Character.toString(ZItem.Flag.note.getFlagChar());
        mbox.updateItem(note.getId(), null, null, flags, null);
        
        // Confirm that the Note flag is set when getting the documents.
        doc = mbox.getDocument(doc.getId());
        assertEquals(null, doc.getFlags());
        assertEquals(flags, note.getFlags());
        
        // Test searching for notes.
        List<String> ids = TestUtil.search(mbox, "in:briefcase tag:\\note", ZSearchParams.TYPE_DOCUMENT);
        assertEquals(1, ids.size());
        assertEquals(note.getId(), ids.get(0));

    }

    /**
     * Tests the content-type based on file extension.
     */

 public void testContentType()
    throws Exception {
        // Create two documents.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String folderId = Integer.toString(Mailbox.ID_FOLDER_BRIEFCASE);
        ZDocument doc1 = TestUtil.createDocument(mbox, folderId, NAME_PREFIX + "-docOne.doc", "application/octet-stream", "doc1".getBytes());
        ZDocument doc2 = TestUtil.createDocument(mbox, folderId, NAME_PREFIX + "-docTwo.xls", "application/ms-tnef", "doc2".getBytes());
        
        // Confirm that the content-type changed based on file extension
        assertEquals("application/msword", doc1.getContentType());
        assertEquals("application/vnd.ms-excel", doc2.getContentType());
    }

    @Override public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestDocument.class);
    }
}
