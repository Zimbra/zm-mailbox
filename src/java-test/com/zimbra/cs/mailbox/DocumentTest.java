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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.qa.unittest.TestUtil;

/**
 * Unit test for {@link Document}.
 */
public final class DocumentTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void create() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "filename", "content", false);
        Assert.assertEquals("filename", doc.getName());
        Assert.assertEquals("content", doc.getFragment());
    }

    /**
     * Verifies setting the {@code Note} flag on a document.
     */
    @Test
    public void note() throws Exception {
        // Create document and note.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "doc.txt", "This is a document", false);
        Document note = createDocument(mbox, "note.txt", "This is a note", true);

        // Validate flag.
        doc = mbox.getDocumentById(null, doc.getId());
        note = mbox.getDocumentById(null, note.getId());
        assertTrue((note.getFlagBitmask() & Flag.FlagInfo.NOTE.toBitmask()) != 0);

        // Search by flag.
        List<Integer> ids = TestUtil.search(mbox, "tag:\\note", MailItem.Type.DOCUMENT);
        Assert.assertEquals(1, ids.size());
        Assert.assertEquals(note.getId(), ids.get(0).intValue());

        // Make sure that the note flag is serialized to XML.
        Element eDoc = ToXML.encodeDocument(new XMLElement("test"), new ItemIdFormatter(), null, doc);
        Assert.assertEquals(null, Strings.emptyToNull(eDoc.getAttribute(MailConstants.A_FLAGS, null)));
        Element eNote = ToXML.encodeDocument(new XMLElement("test"), new ItemIdFormatter(), null, note);
        Assert.assertEquals("t", eNote.getAttribute(MailConstants.A_FLAGS));
    }

    /**
     * Verifies that we don't allow changing a document to a note.
     */
    @Test
    public void docToNote() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "doc.txt", "This is a document", false);
        MailboxTestUtil.setFlag(mbox, doc.getId(), Flag.FlagInfo.NOTE);
        doc = mbox.getDocumentById(null, doc.getId());
        Assert.assertEquals(0, doc.getFlagBitmask());
    }

    /**
     * Verifies that we don't allow changing a note to a document.
     */
    @Test
    public void noteToDoc() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "note.txt", "This is a note", true);
        MailboxTestUtil.unsetFlag(mbox, doc.getId(), Flag.FlagInfo.NOTE);
        doc = mbox.getDocumentById(null, doc.getId());
        Assert.assertEquals(Flag.FlagInfo.NOTE.toBitmask(), doc.getFlagBitmask());
    }

    private Document createDocument(Mailbox mbox, String name, String content, boolean isNote) throws Exception {
        InputStream in = new ByteArrayInputStream(content.getBytes());
        ParsedDocument pd = new ParsedDocument(in, name, "text/plain", System.currentTimeMillis(), null, null);
        int flags = (isNote ? Flag.BITMASK_NOTE : 0);
        return mbox.createDocument(null, Mailbox.ID_FOLDER_BRIEFCASE, pd, MailItem.Type.DOCUMENT, flags);
    }

    private static void checkName(Mailbox mbox, String name, boolean valid) throws Exception {
        Document doc = null;
        ParsedDocument pd = new ParsedDocument(new ByteArrayInputStream("test".getBytes()), name, "text/plain", System.currentTimeMillis(), null, null);
        try {
            doc = mbox.createDocument(null, Mailbox.ID_FOLDER_BRIEFCASE, pd, MailItem.Type.DOCUMENT, 0);
            if (!valid) {
                Assert.fail("should not have been allowed to create document: [" + name + "]");
            }
        } catch (ServiceException e) {
            Assert.assertEquals("unexpected error code", MailServiceException.INVALID_NAME, e.getCode());
            if (valid) {
                Assert.fail("should have been allowed to create document: [" + name + "]");
            }
        }

        // clean up after ourselves
        if (doc != null) {
            mbox.delete(null, doc.getId(), MailItem.Type.DOCUMENT);
        }
    }

    @Test
    public void names() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // empty or all-whitespace
        checkName(mbox, "", false);
        checkName(mbox, "   ", false);

        // control characters converted to spaces by ParsedDocument constructor
        checkName(mbox, "sam\rwise", true);
        checkName(mbox, "sam\nwise", true);
        checkName(mbox, "sam\twise", true);
        checkName(mbox, "sam\u0003wise", true);

        // invalid characters (BOM, unpaired surrogates) not converted by ParsedDocument constructor
        checkName(mbox, "sam\uFFFEwise", false);
        checkName(mbox, "sam\uDBFFwise", false);
        checkName(mbox, "sam\uDC00wise", false);

        // invalid path characters
        checkName(mbox, "sam/wise", false);
        checkName(mbox, "sam\"wise", false);
        checkName(mbox, "sam:wise", false);

        // reserved names
        checkName(mbox, ".", false);
        checkName(mbox, "..", false);
        checkName(mbox, ".  ", false);
        checkName(mbox, ".. ", false);

        // valid path characters
        checkName(mbox, "sam\\wise", true);
        checkName(mbox, "sam'wise", true);
        checkName(mbox, "sam*wise", true);
        checkName(mbox, "sam|wise", true);
        checkName(mbox, "sam wise", true);
    }
}
