/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 VMware, Inc.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.ZimbraIndexReader.TermFieldEnumeration;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedContact;

public abstract class AbstractIndexStoreTest {
    static String originalIndexStoreFactory;

    protected abstract String getIndexStoreFactory();
    static Provisioning prov;
    static Account testAcct;

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("log4j.configuration", "log4j-test.properties");
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        testAcct = prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        originalIndexStoreFactory = IndexStore.getFactory().getClass().getName();
    }

    @AfterClass
    public static void destroy() {
        try {
            prov.deleteAccount(testAcct.getId());
        } catch (ServiceException e) {
            ZimbraLog.test.error("Problem cleaning up test@zimbra.com account", e);
        }
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
    public void termQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST termQuery");
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
        // TODO:  For Lucene, we use special analyzers which mean that "@" is included as part of a term.
        //        Need to do something similar for ElasticSearch?
        ZimbraTopDocs result = searcher.search(
                  new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "none@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for none@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'none@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for none@zimbra.com", 0, result.getTotalHits());

        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "test@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for test@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'test@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for test@zimbra.com", 1, result.getTotalHits());
        Assert.assertEquals(String.valueOf(contact.getId()),
                searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        Assert.assertEquals(1, searcher.getIndexReader().numDocs());
        searcher.close();
    }

    @Test
    public void filteredTermQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST filteredTermQuery");
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
        Contact contact5 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "xyz@vmware.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.addDocument(folder, contact4, contact4.generateIndexData());
        indexer.close();

        List<Term> terms = Lists.newArrayList();
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact2.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact4.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact5.getId())));
        ZimbraTermsFilter filter = new ZimbraTermsFilter(terms);
        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs result;
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), filter, 100);
        Assert.assertNotNull("searcher.search result object - searching for test@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'zimbra.com', filtering by IDs\n%s" + result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        List<String> expecteds = Lists.newArrayList();
        List<String> matches = Lists.newArrayList();
        matches.add(searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        matches.add(searcher.doc(result.getScoreDoc(1).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        expecteds.add(String.valueOf(contact2.getId()));
        expecteds.add(String.valueOf(contact4.getId()));
        Collections.sort(matches);
        Collections.sort(expecteds);
        Assert.assertEquals("Match Blob ID", expecteds.get(0), matches.get(0));
        Assert.assertEquals("Match Blob ID", expecteds.get(1), matches.get(1));
        searcher.close();
    }

    @Test
    public void sortedFilteredTermQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST sortedFilteredTermQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "abc@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "abcd@zimbra.com")), folder.getId(), null);
        Contact contact3 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "xy@zimbra.com")), folder.getId(), null);
        Thread.sleep(1234);
        Contact contact4 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "xyz@zimbra.com")), folder.getId(), null);
        Contact contact5 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "xyz@vmware.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.addDocument(folder, contact4, contact4.generateIndexData());
        indexer.close();

        List<Term> terms = Lists.newArrayList();
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact2.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact4.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact5.getId())));
        ZimbraTermsFilter filter = new ZimbraTermsFilter(terms);
        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs result;
        Sort sort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, false));
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), filter, 100, sort);
        Assert.assertNotNull("searcher.search result object - searching for test@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'zimbra.com', filtering by IDs\n%s" + result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        Assert.assertEquals("Match Blob ID 1", String.valueOf(contact2.getId()),
                searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        Assert.assertEquals("Match Blob ID 2", String.valueOf(contact4.getId()),
                searcher.doc(result.getScoreDoc(1).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        sort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true));
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), filter, 100, sort);
        Assert.assertNotNull("searcher.search result object - searching for test@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'zimbra.com', filtering by IDs\n%s" + result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        Assert.assertEquals("Match Blob ID 1", String.valueOf(contact4.getId()),
                searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        Assert.assertEquals("Match Blob ID 2", String.valueOf(contact2.getId()),
                searcher.doc(result.getScoreDoc(1).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        searcher.close();
    }

    @Test
    public void leadingWildcardQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST leadingWildcardQuery");
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
        // This seems to be the supported way of enabling leading wildcard queries
        QueryParser queryParser = new QueryParser(LuceneIndex.VERSION, LuceneFields.L_CONTACT_DATA,
                new StandardAnalyzer(LuceneIndex.VERSION));
        queryParser.setAllowLeadingWildcard(true);
        Query query = queryParser.parse("*irst");
        ZimbraTopDocs result = searcher.search(query, 100);
        Assert.assertNotNull("searcher.search result object - searching for *irst", result);
        ZimbraLog.test.debug("Result for search for '*irst'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for *irst", 1, result.getTotalHits());
    }

    @Test
    public void booleanQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST booleanQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Map<String, Object> fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "First",
                ContactConstants.A_lastName, "Last",
                ContactConstants.A_jobTitle, "Software Development Engineer",
                ContactConstants.A_email, "first.last@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
        fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "Given",
                ContactConstants.A_lastName, "Surname",
                ContactConstants.A_email, "GiV.SurN@zimbra.com");
        Contact contact2 = mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact, contact.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        // This seems to be the supported way of enabling leading wildcard queries
        QueryParser queryParser = new QueryParser(LuceneIndex.VERSION, LuceneFields.L_CONTACT_DATA,
                new StandardAnalyzer(LuceneIndex.VERSION));
        queryParser.setAllowLeadingWildcard(true);
        Query wquery = queryParser.parse("*irst");
        Query tquery = new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "absent"));
        Query tquery2 = new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "Last"));
        BooleanQuery bquery = new BooleanQuery();
        bquery.add(wquery, Occur.MUST);
        bquery.add(tquery, Occur.MUST_NOT);
        bquery.add(tquery2, Occur.SHOULD);
        ZimbraTopDocs result = searcher.search(bquery, 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search [hits=%d]:%s", result.getTotalHits(), result.toString());
        Assert.assertEquals("Number of hits", 1, result.getTotalHits());
        String expected1Id = String.valueOf(contact.getId());
        String match1Id = searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID);
        Assert.assertEquals("Mailbox Blob ID of match", expected1Id, match1Id);
    }

    @Test
    public void phraseQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST phraseQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Map<String, Object> fields;
        fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "Non",
                ContactConstants.A_lastName, "Match",
                ContactConstants.A_email, "nOn.MaTchiNg@zimbra.com");
        Contact contact1 = mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
        fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "First",
                ContactConstants.A_lastName, "Last",
                ContactConstants.A_jobTitle, "Software Development Engineer",
                ContactConstants.A_email, "first.last@zimbra.com");
        Contact contact2 = mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
        fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "Given",
                ContactConstants.A_lastName, "Surname",
                ContactConstants.A_email, "GiV.SurN@zimbra.com");
        Contact contact3 = mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        PhraseQuery pquery = new PhraseQuery();
        pquery.add(new Term(LuceneFields.L_CONTENT, "Software Development Engineer"));
        ZimbraTopDocs result = searcher.search(pquery, 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search [hits=%d]:%s", result.getTotalHits(), result.toString());
        Assert.assertEquals("Number of hits", 1, result.getTotalHits());
        String expected1Id = String.valueOf(contact2.getId());
        String match1Id = searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID);
        Assert.assertEquals("Mailbox Blob ID of match", expected1Id, match1Id);
    }

    @Test
    public void prefixQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST prefixQuery");
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
    public void termRangeQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST termRangeQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "abc@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "abcd@zimbra.com")), folder.getId(), null);
        Contact contact3 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "aaaa@zimbra.com")), folder.getId(), null);
        Contact contact4 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "zzz@zimbra.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.addDocument(folder, contact4, contact4.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        TermRangeQuery query = new TermRangeQuery(LuceneFields.L_FIELD,
                "email:aba@zimbra.com",
                "email:abz@zimbra.com",
                false, true);
        ZimbraTopDocs result = searcher.search(query, 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search %s", result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        List<String> expecteds = Lists.newArrayList();
        List<String> matches = Lists.newArrayList();
        matches.add(searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        matches.add(searcher.doc(result.getScoreDoc(1).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID));
        expecteds.add(String.valueOf(contact1.getId()));
        expecteds.add(String.valueOf(contact2.getId()));
        Collections.sort(matches);
        Collections.sort(expecteds);
        Assert.assertEquals("Match Blob ID", expecteds.get(0), matches.get(0));
        Assert.assertEquals("Match Blob ID", expecteds.get(1), matches.get(1));
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
        // TODO        new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
                       new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for '@zimbra.com'", result);
        ZimbraLog.test.debug("Result for search for '@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Total hits after 2 adds", 2, result.getTotalHits());
        searcher.close();

        indexer = index.openIndexer();
        indexer.deleteDocument(Collections.singletonList(contact1.getId()));
        indexer.close();

        searcher = index.openSearcher();
        Assert.assertEquals("numDocs after 2 adds/1 del", 1, searcher.getIndexReader().numDocs());
        // TODO result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
                result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), 100);
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
        // TODO Assert.assertEquals("docs which match '@zimbra.com'", 4,
        // TODO        searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")));
                Assert.assertEquals("docs which match 'zimbra.com'", 4,
                       searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")));
        searcher.close();
    }

    private void checkNextTerm(TermFieldEnumeration fields, Term term) {
        Assert.assertTrue("fields.hasMoreElements() value when expecting:" + term.toString(), fields.hasMoreElements());
        BrowseTerm browseTerm = fields.nextElement();
        Assert.assertNotNull("fields.nextElement() value when expecting:" + term.toString(), browseTerm);
        ZimbraLog.test.debug("Expecting %s=%s value is %s docFreq=%d",
                term.field(), term.text(), browseTerm.getText(), browseTerm.getFreq());
        Assert.assertEquals("field value", term.text(), browseTerm.getText());
    }
    private void checkNextTermFieldType(TermFieldEnumeration fields, String field) {
        Assert.assertTrue("fields.hasMoreElements() value when expecting:" + field, fields.hasMoreElements());
        BrowseTerm browseTerm = fields.nextElement();
        Assert.assertNotNull("fields.nextElement() value when expecting:" + field, browseTerm);
        ZimbraLog.test.debug("Expecting %s=?anyvalue? value is %s docFreq=%d",
                field, browseTerm.getText(), browseTerm.getFreq());
    }

    private void checkAtEnd(TermFieldEnumeration fields, String field) {
        Assert.assertFalse("fields.hasMoreElements() at end of list for field:" + field, fields.hasMoreElements());
        try {
            fields.nextElement();
            Assert.fail("fields.nextElement() at end of list for field:" + field + " contact data succeeded");
        } catch (NoSuchElementException ex) {
        }
    }

    /**
     * The result of getTermsForField can be good for seeing the effects of {@code ZimbraAnalyzer} on how fields get
     * tokenized. TODO:  Add tests for different types of tokenizers.
     * @throws Exception
     */
    @Test
    public void termEnum() throws Exception {
        ZimbraLog.test.debug("--->TEST termEnum");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "teSt1@ziMBRA.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test2@zimbra.com")), folder.getId(), null);

        IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        // Note that TermFieldEnumeration order is defined to be sorted
        TermFieldEnumeration fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_CONTACT_DATA, "");
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "@zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test1"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test2"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test2@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_CONTACT_DATA);
        } finally {
            Closeables.closeQuietly(fields);
        }
        fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_CONTENT, "");
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test1"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test2"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test2@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_CONTENT);
        } finally {
            Closeables.closeQuietly(fields);
        }
        fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_FIELD, "");
            checkNextTerm(fields, new Term(LuceneFields.L_FIELD, "email:test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_FIELD, "email:test2@zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_FIELD);
        } finally {
            Closeables.closeQuietly(fields);
        }
        fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_PARTNAME, "");
            checkNextTerm(fields, new Term(LuceneFields.L_PARTNAME, "CONTACT"));
            checkAtEnd(fields, LuceneFields.L_PARTNAME);
        } finally {
            Closeables.closeQuietly(fields);
        }
        fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_H_TO, "");
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "@zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test1"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test2"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test2@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_H_TO);
        } finally {
            Closeables.closeQuietly(fields);
        }
        fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_H_TO, "tess");
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test1"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test2"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test2@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_H_TO + "(sublist)");
        } finally {
            Closeables.closeQuietly(fields);
        }
        fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_SORT_DATE, "");
            checkNextTermFieldType(fields, LuceneFields.L_SORT_DATE);
            checkAtEnd(fields, LuceneFields.L_H_TO);
        } finally {
            Closeables.closeQuietly(fields);
        }
        fields = null;
        try {
            fields = searcher.getIndexReader().getTermsForField(LuceneFields.L_MAILBOX_BLOB_ID, "");
            checkNextTermFieldType(fields, LuceneFields.L_MAILBOX_BLOB_ID);
            checkNextTermFieldType(fields, LuceneFields.L_MAILBOX_BLOB_ID);
            checkAtEnd(fields, LuceneFields.L_H_TO);
        } finally {
            Closeables.closeQuietly(fields);
        }
        searcher.close();
    }
}
