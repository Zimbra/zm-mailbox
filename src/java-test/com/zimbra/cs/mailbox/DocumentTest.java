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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
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
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
    }
    
    private void cleanup () throws Exception {
        MailboxTestUtil.clearData();
    }


    @Test
    public void create() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "filename", "content", null, false);
        assertEquals("filename", doc.getName());
        assertEquals("content", doc.getFragment());
    }

    /**
     * Verifies setting the {@code Note} flag on a document.
     */
    @Test
    public void note() throws Exception {
        // Create document and note.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "doc.txt", "This is a document", null, false);
        Document note = createDocument(mbox, "note.txt", "This is a note", null, true);

        // Validate flag.
        doc = mbox.getDocumentById(null, doc.getId());
        note = mbox.getDocumentById(null, note.getId());
        assertTrue((note.getFlagBitmask() & Flag.FlagInfo.NOTE.toBitmask()) != 0);

        // Search by flag.
        List<Integer> ids = TestUtil.search(mbox, "tag:\\note", MailItem.Type.DOCUMENT);
        assertEquals(1, ids.size());
        assertEquals(note.getId(), ids.get(0).intValue());

        // Make sure that the note flag is serialized to XML.
        Element eDoc = ToXML.encodeDocument(new XMLElement("test"), new ItemIdFormatter(), null, doc);
        assertEquals(null, Strings.emptyToNull(eDoc.getAttribute(MailConstants.A_FLAGS, null)));
        Element eNote = ToXML.encodeDocument(new XMLElement("test"), new ItemIdFormatter(), null, note);
        assertEquals("t", eNote.getAttribute(MailConstants.A_FLAGS));
    }

    /**
     * Verifies that we don't allow changing a document to a note.
     */
    @Test
    public void docToNote() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "doc.txt", "This is a document", null, false);
        MailboxTestUtil.setFlag(mbox, doc.getId(), Flag.FlagInfo.NOTE);
        doc = mbox.getDocumentById(null, doc.getId());
        assertEquals(0, doc.getFlagBitmask());
    }

    /**
     * Verifies that we don't allow changing a note to a document.
     */
    @Test
    public void noteToDoc() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "note.txt", "This is a note", null, true);
        MailboxTestUtil.unsetFlag(mbox, doc.getId(), Flag.FlagInfo.NOTE);
        doc = mbox.getDocumentById(null, doc.getId());
        assertEquals(Flag.FlagInfo.NOTE.toBitmask(), doc.getFlagBitmask());
    }

    static Document createDocument(Mailbox mbox, String name, String content, String description, boolean isNote) throws Exception {
        InputStream in = new ByteArrayInputStream(content.getBytes());
        ParsedDocument pd = new ParsedDocument(in, name, "text/plain", System.currentTimeMillis(), null, description);
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
            assertEquals("unexpected error code", MailServiceException.INVALID_NAME, e.getCode());
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

    @Test
    public void testConstructFromData() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Document doc = createDocument(mbox, "doc.txt", "This is a document", "doc description", false);

        UnderlyingData ud = DbMailItem.getById(mbox.getId(), mbox.getSchemaGroupId(), doc.getId(), doc.getType(), doc.inDumpster(), DbPool.getConnection(mbox.getId(), mbox.getSchemaGroupId()));
        assertNotNull("Underlying data is null", ud);
        assertEquals("underlying data has wrong type", MailItem.Type.DOCUMENT,MailItem.Type.of(ud.type));
        assertEquals("underlying data has wrong folder ID", Mailbox.ID_FOLDER_BRIEFCASE,ud.folderId);
        assertEquals("underlying data has wrong UUID", doc.getUuid(),ud.uuid);

        MailItem testItem = MailItem.constructItem(Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID),ud,mbox.getId());
        assertNotNull("reconstructed mail item is null", testItem);
        assertEquals("reconstructed doc has wrong item type", MailItem.Type.DOCUMENT,testItem.getType());
        assertEquals("reconstructed doc has wrong UUID", doc.getUuid(), testItem.getUuid());
        assertEquals("reconstructed doc has wrong ID", doc.getId(), testItem.getId());
        assertEquals("reconstructed doc has wrong folder", doc.getFolderId(), testItem.getFolderId());
        assertEquals("reonstructed doc has wrong content", Arrays.toString(doc.getContent()),Arrays.toString(testItem.getContent()));
        List<IndexDocument> docs = testItem.generateIndexDataAsync(false);
        assertEquals(1, docs.size());
        IndexDocument indexDoc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        Collection<String> docFields = indexDoc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        String subject = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String filename = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_FILENAME);
        ArrayList<String> bodyparts = (ArrayList<String>) indexDoc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("document content is null", bodyparts);
        assertEquals("document should have 2 parts of content", 2, bodyparts.size());
        assertEquals("doc.txt", subject);
        assertEquals("doc.txt", filename);
        assertEquals("This is a document", bodyparts.get(0));
        assertEquals("This is a document doc description", bodyparts.get(1));
        assertEquals("index document has wrong l.partname", "top", indexDoc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));
        assertEquals("index document has wrong content type", "text/plain", indexDoc.toDocument().getFieldValue(LuceneFields.L_MIMETYPE));
    }

    @Test
    public void testGenerateIndexData() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        Document doc = createDocument(mbox, "doc.txt", "This is a document", "test document", false);

        List<IndexDocument> docs = doc.generateIndexData();
        assertEquals(1, docs.size());
        IndexDocument indexDoc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        Collection<String> docFields = indexDoc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        String subject = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String filename = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_FILENAME);
        ArrayList<String> bodyparts = (ArrayList<String>) indexDoc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("document content is null", bodyparts);
        assertEquals("document should have 2 parts of content", 2, bodyparts.size());
        assertEquals("doc.txt", subject);
        assertEquals("doc.txt", filename);
        assertEquals("This is a document", bodyparts.get(0));
        assertEquals("This is a document test document", bodyparts.get(1));
        assertEquals("index document has wrong l.partname", "top", indexDoc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));
        assertEquals("index document has wrong content type", "text/plain", indexDoc.toDocument().getFieldValue(LuceneFields.L_MIMETYPE));
    }

    @Test
    public void testGenerateIndexDataAsync() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        Document doc = createDocument(mbox, "doc.txt", "This is a document", "test document", false);

        List<IndexDocument> docs = doc.generateIndexDataAsync(false);
        assertEquals(1, docs.size());
        IndexDocument indexDoc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        Collection<String> docFields = indexDoc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        String subject = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String filename = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_FILENAME);
        ArrayList<String> bodyparts = (ArrayList<String>) indexDoc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("document content is null", bodyparts);
        assertEquals("document should have 2 parts of content", 2, bodyparts.size());
        assertEquals("doc.txt", subject);
        assertEquals("doc.txt", filename);
        assertEquals("This is a document", bodyparts.get(0));
        assertEquals("This is a document test document", bodyparts.get(1));
        assertEquals("index document has wrong l.partname", "top", indexDoc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));
        assertEquals("index document has wrong content type", "text/plain", indexDoc.toDocument().getFieldValue(LuceneFields.L_MIMETYPE));

        docs = doc.generateIndexDataAsync(true);
        assertEquals(1, docs.size());
        indexDoc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        docFields = indexDoc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        subject = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        filename = (String) indexDoc.toDocument().getFieldValue(LuceneFields.L_FILENAME);
        bodyparts = (ArrayList<String>) indexDoc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("document content is null", bodyparts);
        assertEquals("document should have 2 parts of content", 2, bodyparts.size());
        assertEquals("doc.txt", subject);
        assertEquals("doc.txt", filename);
        assertEquals("This is a document", bodyparts.get(0));
        assertEquals("This is a document test document", bodyparts.get(1));
        assertEquals("index document has wrong l.partname", "top", indexDoc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));
        assertEquals("index document has wrong content type", "text/plain", indexDoc.toDocument().getFieldValue(LuceneFields.L_MIMETYPE));
    }
}
