/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedContact;

public abstract class AbstractIndexStoreTest {
    static String originalIndexStoreFactory;

    protected abstract String getIndexStoreFactory();

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("log4j.configuration", "log4j-test.properties");
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        originalIndexStoreFactory = IndexStore.getFactory().getClass().getName();
    }

    @AfterClass
    public static void destroy() {
        IndexStore.getFactory().destroy();
        IndexStore.setFactory(originalIndexStoreFactory);
    }

    @After
    public void teardown() {
        IndexStore.getFactory().destroy();
    }

    @Before
    public void setup() throws Exception {
        MailboxTestUtil.clearData();
        IndexStore.setFactory(getIndexStoreFactory());
    }

    @Test
    public void searchByTermQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST searchByTermQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Map<String, Object> fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "First",
                ContactConstants.A_lastName, "Last",
                ContactConstants.A_email, "test@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact, contact.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs result = searcher.search(
                new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "none@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for none@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'none@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for none@zimbra.com", 0, result.getTotalHits());

        Assert.assertEquals(1, searcher.getIndexReader().numDocs());
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "test@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for test@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'test@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for test@zimbra.com", 1, result.getTotalHits());
        Assert.assertEquals(String.valueOf(contact.getId()),
                searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        searcher.close();
    }

    @Test
    public void searchByPrefixQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST searchByPrefixQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "abc@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "abcd@zimbra.com")), folder.getId(), null);
        Contact contact3 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "xy@zimbra.com")), folder.getId(), null);
        Contact contact4 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "xyz@zimbra.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.addDocument(folder, contact4, contact4.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs result = searcher.search(new PrefixQuery(new Term(LuceneFields.L_CONTACT_DATA, "ab")), 100);
        Assert.assertNotNull("searcher.search result object - searching for 'ab' prefix", result);
        ZimbraLog.test.debug("Result for search for 'ab'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for 'ab' prefix", 2, result.getTotalHits());
        String contact1Id = String.valueOf(contact1.getId());
        String contact2Id = String.valueOf(contact2.getId());
        String match1Id = searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID);
        String match2Id = searcher.doc(result.getScoreDoc(1).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID);
        ZimbraLog.test.debug("Contact1ID=%s Contact2ID=%s match1id=%s match2id=%s",
                contact1Id, contact2Id, match1Id, match2Id);
        if (contact1Id.equals(match1Id)) {
            Assert.assertEquals("2nd match isn't contact2's ID", contact2Id, match2Id);
        } else if (contact1Id.equals(match2Id)) {
            Assert.assertEquals("2nd match isn't contact1's ID", contact2Id, match1Id);
        } else {
            Assert.fail(String.format("Contact 1 ID [%s] doesn't match either [%s] or [%s]",
                    contact1Id, match1Id, match2Id));
        }
    }

    @Test
    public void deleteDocument() throws Exception {
        ZimbraLog.test.debug("--->TEST deleteDocument");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test2@zimbra.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        Assert.assertEquals("maxDocs at start", 0, indexer.maxDocs());
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        Assert.assertEquals("numDocs after 2 adds", 2, searcher.getIndexReader().numDocs());
        ZimbraTopDocs result = searcher.search(
                new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for '@zimbra.com'", result);
        ZimbraLog.test.debug("Result for search for '@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Total hits after 2 adds", 2, result.getTotalHits());
        searcher.close();

        indexer = index.openIndexer();
        indexer.deleteDocument(Collections.singletonList(contact1.getId()));
        indexer.close();

        searcher = index.openSearcher();
        Assert.assertEquals("numDocs after 2 adds/1 del", 1, searcher.getIndexReader().numDocs());
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object after 2 adds/1 del", result);
        ZimbraLog.test.debug("Result for search for '@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Total hits after 2 adds/1 del", 1, result.getTotalHits());
        searcher.close();
    }

    @Test
    public void getCount() throws Exception {
        ZimbraLog.test.debug("--->TEST getCount");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test2@zimbra.com")), folder.getId(), null);
        Contact contact3 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test3@zimbra.com")), folder.getId(), null);
        Contact contact4 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test4@zimbra.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        Assert.assertEquals("maxDocs at start", 0, indexer.maxDocs());
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.addDocument(folder, contact4, contact4.generateIndexData());
        Assert.assertEquals("maxDocs after adding 4 contacts", 4, indexer.maxDocs());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        Assert.assertEquals("numDocs after adding 4 contacts", 4, searcher.getIndexReader().numDocs());
        Assert.assertEquals("docs which match 'test1'", 1,
                searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "test1")));
        Assert.assertEquals("docs which match '@zimbra.com'", 4,
                searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")));
        searcher.close();
    }

    private void checkNextTerm(TermEnum terms, Term term) throws IOException {
        Assert.assertTrue("terms.next() null when expecting term:" + term.toString(), terms.next());
        Assert.assertEquals("terms.next() is not the expected term", term, terms.term());
    }
    private void checkNextTermFieldType(TermEnum terms, String field) throws IOException {
        Assert.assertTrue("terms.next() null when expecting term with field type:" + field, terms.next());
        Assert.assertEquals("terms.next() field type unexpected", field, terms.term().field());
        ZimbraLog.test.debug("Actual value for term :%s", terms.term().toString());
    }

    @Test
    public void termEnum() throws Exception {
        ZimbraLog.test.debug("--->TEST termEnum");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test2@zimbra.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        // Note that terms.next() order is defined to be sorted
        TermEnum terms = searcher.getIndexReader().terms(new Term(LuceneFields.L_CONTACT_DATA, ""));
        Assert.assertEquals("@zimbra contact term present",
                new Term(LuceneFields.L_CONTACT_DATA, "@zimbra"), terms.term());
        checkNextTerm(terms, new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTACT_DATA, "test1"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTACT_DATA, "test1@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTACT_DATA, "test2"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTACT_DATA, "test2@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTACT_DATA, "zimbra"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTENT, "test1"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTENT, "test1@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTENT, "test2"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTENT, "test2@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTENT, "zimbra"));
        checkNextTerm(terms, new Term(LuceneFields.L_CONTENT, "zimbra.com"));
        checkNextTermFieldType(terms, LuceneFields.L_SORT_DATE);
        checkNextTerm(terms, new Term(LuceneFields.L_FIELD, "email:test1@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_FIELD, "email:test2@zimbra.com"));
        checkNextTermFieldType(terms, LuceneFields.L_MAILBOX_BLOB_ID);
        checkNextTermFieldType(terms, LuceneFields.L_MAILBOX_BLOB_ID);
        checkNextTerm(terms, new Term(LuceneFields.L_PARTNAME, "CONTACT"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "@zimbra"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "test1"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "test1@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "test2"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "test2@zimbra.com"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "zimbra"));
        checkNextTerm(terms, new Term(LuceneFields.L_H_TO, "zimbra.com"));
        while (terms.next()) {
            ZimbraLog.test.info("Extra term " + terms.term().toString());
        }
        searcher.close();
    }
}
