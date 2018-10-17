/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.IOException;
import org.junit.Ignore;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PhraseQuery.Builder;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public abstract class AbstractIndexStoreTest {
    static String originalIndexStoreFactory;

    protected abstract String getIndexStoreFactory();

    /**
     * Override this for any Index Store specific cleanup.  Note that for Mock Provisioning, deleting an account
     * does not currently cleanup the index.
     */
    protected void cleanupForIndexStore() {
    }

    /**
     * Override this for any Index Store which might not be available;
     */
    protected boolean indexStoreAvailable() {
        return true;
    }

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
        try {
            IndexStore.getFactory().destroy();
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        IndexStore.setFactory(originalIndexStoreFactory);
    }

    @After
    public void teardown() throws Exception {
        //IndexStore.getFactory().destroy();
        //cleanupForIndexStore();
        MailboxTestUtil.clearData();
    }

    @Before
    public void setup() throws Exception {
        IndexStore.setFactory(getIndexStoreFactory());
        Assert.assertTrue("Index Store NEEDS to be configured and available", indexStoreAvailable());
        MailboxTestUtil.clearData();
        cleanupForIndexStore();
    }

    @Test
    public void termQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST termQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Contact contact = createContact(mbox, "First", "Last", "test@zimbra.com");
        createContact(mbox, "a", "bc", "abc@zimbra.com");
        createContact(mbox, "j", "k", "j.k@zimbra.com");
        createContact(mbox, "Matilda", "Higgs-Bozon", "matilda.higgs.bozon@zimbra.com");

        // Stick with just one IndexStore - the one cached in Mailbox:
        //    IndexStore index = IndexStore.getFactory().getIndexStore(mbox);
        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs result = searcher.search(
                  new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "none@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for none@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'none@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for none@zimbra.com", 0, result.getTotalHits());

        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "test@zimbra.com")), 100);
        Assert.assertNotNull("searcher.search result object - searching for test@zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'test@zimbra.com'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for test@zimbra.com", 1, result.getTotalHits());
        Assert.assertEquals(String.valueOf(contact.getId()), getBlobIdForResultDoc(searcher, result, 0));
        Assert.assertEquals(4, searcher.getIndexReader().numDocs());
        searcher.close();
    }

    @Test
    public void filteredTermQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST filteredTermQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = createContact(mbox, "a", "bc", "abc@zimbra.com");
        Contact contact2 = createContact(mbox, "a", "bcd", "abcd@zimbra.com");
        Contact contact3 = createContact(mbox, "x", "y", "xy@zimbra.com");
        Contact contact4 = createContact(mbox, "x", "yz", "xyz@zimbra.com");
        Contact contact5 = createContact(mbox, "x", "yz", "xyz@zimbra.com");

        IndexStore index = mbox.index.getIndexStore();
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument( contact1, contact1.generateIndexDataAsync(false));
        indexer.addDocument( contact2, contact2.generateIndexDataAsync(false));
        indexer.addDocument( contact3, contact3.generateIndexDataAsync(false));
        indexer.addDocument( contact4, contact4.generateIndexDataAsync(false));
        // Note: NOT indexed contact5
        indexer.close();

        List<Term> terms = Lists.newArrayList();
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact2.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact4.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(contact5.getId())));
        ZimbraTermsFilter filter = new ZimbraTermsFilter(terms);
        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs result;
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), filter, 100);
        Assert.assertNotNull("searcher.search result object - searching for zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'zimbra.com', filtering by IDs\n%s", result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        List<String> expecteds = Lists.newArrayList();
        List<String> matches = Lists.newArrayList();
        matches.add(getBlobIdForResultDoc(searcher, result, 0));
        matches.add(getBlobIdForResultDoc(searcher, result, 1));
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
        Contact con1 = createContact(mbox, "a", "bc", "abc@zimbra.com");
        Contact con2 = createContact(mbox, "abcd@zimbra.com");
        Contact con3 = createContact(mbox, "xy@zimbra.com");
        Thread.sleep(1001);  // To ensure different sort date
        Contact con4 = createContact(mbox, "xyz@zimbra.com");
        Contact con5 = createContact(mbox, "xyz@zimbra.com");

        IndexStore index = mbox.index.getIndexStore();
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument( con1, con1.generateIndexDataAsync(false));
        indexer.addDocument( con2, con2.generateIndexDataAsync(false));
        indexer.addDocument( con3, con3.generateIndexDataAsync(false));
        indexer.addDocument( con4, con4.generateIndexDataAsync(false));
        // Note: NOT indexed contact5
        indexer.close();

        List<Term> terms = Lists.newArrayList();
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(con2.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(con4.getId())));
        terms.add(new Term(LuceneFields.L_MAILBOX_BLOB_ID, String.valueOf(con5.getId())));
        ZimbraTermsFilter filter = new ZimbraTermsFilter(terms);
        ZimbraIndexSearcher srchr = index.openSearcher();
        ZimbraTopDocs result;
        Sort sort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.Type.STRING, false));
        result = srchr.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), filter, 100, sort);
        Assert.assertNotNull("searcher.search result object - searching for zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'zimbra.com', filtering by IDs 2,4 & 5\n%s", result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        Assert.assertEquals("Match Blob ID 1", String.valueOf(con2.getId()), getBlobIdForResultDoc(srchr, result, 0));
        Assert.assertEquals("Match Blob ID 2", String.valueOf(con4.getId()), getBlobIdForResultDoc(srchr, result, 1));
        // Repeat but with a reverse sort this time
        sort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.Type.STRING, true));
        result = srchr.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com")), filter, 100, sort);
        Assert.assertNotNull("searcher.search result object - searching for zimbra.com", result);
        ZimbraLog.test.debug("Result for search for 'zimbra.com' sorted reverse, filter by IDs\n%s", result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        Assert.assertEquals("Match Blob ID 1", String.valueOf(con4.getId()), getBlobIdForResultDoc(srchr, result, 0));
        Assert.assertEquals("Match Blob ID 2", String.valueOf(con2.getId()), getBlobIdForResultDoc(srchr, result, 1));
        srchr.close();
    }

    @Test
    public void leadingWildcardQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST leadingWildcardQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Contact contact = createContact(mbox, "First", "Last", "f.last@zimbra.com", "Leading Wildcard");
        createContact(mbox, "Grand", "Piano", "grand@vmware.com");


        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        //this will only work with SOLR since it requires the WildcardQueryParser
        ZimbraTopDocs result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "*irst")), 100);
        Assert.assertNotNull("searcher.search result object - searching for *irst", result);
        ZimbraLog.test.debug("Result for search for '*irst'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for *irst", 1, result.getTotalHits());
        String expected1Id = String.valueOf(contact.getId());
        String match1Id = searcher.doc(result.getScoreDoc(0).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID);
        Assert.assertEquals("Mailbox Blob ID of match", expected1Id, match1Id);
    }

    @Test
    public void booleanQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST booleanQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Contact contact = createContact(mbox, "First", "Last", "f.last@zimbra.com", "Software Development Engineer");
        createContact(mbox, "Given", "Surname", "GiV.SurN@zimbra.com");

        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        Query wquery = new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "*irst"));
        Query tquery = new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "absent"));
        Query tquery2 = new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "Last"));
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(wquery, Occur.MUST);
        builder.add(tquery, Occur.MUST_NOT);
        builder.add(tquery2, Occur.SHOULD);
        ZimbraTopDocs result = searcher.search(builder.build(), 100);
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
        createContact(mbox, "Non", "Match", "nOn.MaTchiNg@zimbra.com");
        Contact contact2 = createContact(mbox, "First", "Last", "f.last@zimbra.com", "Software Development Engineer");
        createContact(mbox, "Given", "Surname", "GiV.SurN@zimbra.com");

        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        // Lower case required for each term for Lucene
        builder.add(new Term(LuceneFields.L_CONTENT, "software"));
        builder.add(new Term(LuceneFields.L_CONTENT, "development"));
        builder.add(new Term(LuceneFields.L_CONTENT, "engineer"));
        ZimbraTopDocs result = searcher.search(builder.build(), 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search [hits=%d]:%s", result.getTotalHits(), result.toString());
        Assert.assertEquals("Number of hits", 1, result.getTotalHits());
        String expected1Id = String.valueOf(contact2.getId());
        String match1Id = getBlobIdForResultDoc(searcher, result, 0);
        Assert.assertEquals("Mailbox Blob ID of match", expected1Id, match1Id);
        builder = new PhraseQuery.Builder();
        // Try again with words out of order
        builder.add(new Term(LuceneFields.L_CONTENT, "development"));
        builder.add(new Term(LuceneFields.L_CONTENT, "software"));
        builder.add(new Term(LuceneFields.L_CONTENT, "engineer"));
        result = searcher.search(builder.build(), 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search [hits=%d]:%s", result.getTotalHits(), result.toString());
        Assert.assertEquals("Number of hits", 0, result.getTotalHits());
    }

    @Test
    public void phraseQueryWithStopWord() throws Exception {
        ZimbraLog.test.debug("--->TEST phraseQueryWithStopWord");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        createContact(mbox, "Non", "Match", "nOn.MaTchiNg@zimbra.com");
        Contact contact2 = createContact(mbox, "First", "Last", "f.last@zimbra.com",
                "1066 and all that with William the conqueror and others");
        createContact(mbox, "Given", "Surname", "GiV.SurN@zimbra.com");


        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        // Lower case required for each term for Lucene
        builder.add(new Term(LuceneFields.L_CONTENT, "william"));
        // pquery.add(new Term(LuceneFields.L_CONTENT, "the")); - excluded because it is a stop word
        builder.add(new Term(LuceneFields.L_CONTENT, "conqueror"));
        ZimbraTopDocs result = searcher.search(builder.build(), 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search [hits=%d]:%s", result.getTotalHits(), result.toString());
        Assert.assertEquals("Number of hits", 1, result.getTotalHits());
        String expected1Id = String.valueOf(contact2.getId());
        String match1Id = getBlobIdForResultDoc(searcher, result, 0);
        Assert.assertEquals("Mailbox Blob ID of match", expected1Id, match1Id);
    }

    @Test
    public void multiPhraseQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST multiPhraseQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        createContact(mbox, "Non", "Match", "nOn.MaTchiNg@zimbra.com");
        Contact contact1 = createContact(mbox, "Paul", "AA",  "aa@example.net", "Software Development Engineer");
                           createContact(mbox, "Jane", "BB",  "bb@example.net", "Software Planning Engineer");
        Contact contact2 = createContact(mbox, "Peter", "CC", "cc@example.net", "Software Dev Engineer");
                           createContact(mbox, "Avril", "DD", "dd@example.net", "Software Architectural Engineer");
        Contact contact3 = createContact(mbox, "Leo", "EE",   "ee@example.net", "Software Developer Engineer");
        Contact contact4 = createContact(mbox, "Wow", "DD", "dd@example.net", "Softly Development Engineer");
        createContact(mbox, "Given", "Surname", "GiV.SurN@zimbra.com");


        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        MultiPhraseQuery.Builder builder = new MultiPhraseQuery.Builder();
        // Lower case required for each term for Lucene
        Term[] firstWords = { new Term(LuceneFields.L_CONTENT, "softly"),
                new Term(LuceneFields.L_CONTENT, "software")
        };
        builder.add(firstWords);
        Term[] secondWords = { new Term(LuceneFields.L_CONTENT, "dev"),
                new Term(LuceneFields.L_CONTENT, "development"),
                new Term(LuceneFields.L_CONTENT, "developer")
        };
        builder.add(secondWords);
        builder.add(new Term(LuceneFields.L_CONTENT, "engineer"));
        ZimbraTopDocs result = searcher.search(builder.build(), 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search [hits=%d]:%s", result.getTotalHits(), result.toString());
        Assert.assertEquals("Number of hits", 4, result.getTotalHits());
        List<String> expecteds = Lists.newArrayList();
        List<String> matches = Lists.newArrayList();
        matches.add(getBlobIdForResultDoc(searcher, result, 0));
        matches.add(getBlobIdForResultDoc(searcher, result, 1));
        matches.add(getBlobIdForResultDoc(searcher, result, 2));
        matches.add(getBlobIdForResultDoc(searcher, result, 3));
        expecteds.add(String.valueOf(contact1.getId()));
        expecteds.add(String.valueOf(contact2.getId()));
        expecteds.add(String.valueOf(contact3.getId()));
        expecteds.add(String.valueOf(contact4.getId()));
        Collections.sort(matches);
        Collections.sort(expecteds);
        for (int ndx = 0; ndx < 4; ndx++) {
            Assert.assertEquals("Match Blob ID", expecteds.get(0), matches.get(0));
        }
    }

    @Test
    public void prefixQuery() throws Exception {
        ZimbraLog.test.debug("--->TEST prefixQuery");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Contact contact1 = createContact(mbox, "a", "bc", "abc@zimbra.com");
        Contact contact2 = createContact(mbox, "a", "bcd", "abcd@zimbra.com");
        createContact(mbox, "x", "Y", "xy@zimbra.com");
        createContact(mbox, "x", "Yz", "x.Yz@zimbra.com");

        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs result = searcher.search(new PrefixQuery(new Term(LuceneFields.L_CONTACT_DATA, "ab")), 100);
        Assert.assertNotNull("searcher.search result object - searching for 'ab' prefix", result);
        ZimbraLog.test.debug("Result for search for 'ab'\n" + result.toString());
        Assert.assertEquals("Number of hits searching for 'ab' prefix", 2, result.getTotalHits());
        String contact1Id = String.valueOf(contact1.getId());
        String contact2Id = String.valueOf(contact2.getId());
        String match1Id = getBlobIdForResultDoc(searcher, result, 0);
        String match2Id = getBlobIdForResultDoc(searcher, result, 1);
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
        Contact contact1 = createContact(mbox, "James", "Peters", "abc@zimbra.com");
        Contact contact2 = createContact(mbox, "a", "bcd", "abcd@zimbra.com");
        createContact(mbox, "aa", "bcd", "aaaa@zimbra.com");
        createContact(mbox, "aa", "bcd", "zzz@zimbra.com");


        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        TermRangeQuery query = new TermRangeQuery(LuceneFields.L_FIELD,
                new BytesRef("email:aba@zimbra.com"),
                new BytesRef("email:abz@zimbra.com"),
                false, true);
        ZimbraTopDocs result = searcher.search(query, 100);
        Assert.assertNotNull("searcher.search result object", result);
        ZimbraLog.test.debug("Result for search %s", result.toString());
        Assert.assertEquals("Number of hits", 2, result.getTotalHits());
        List<String> expecteds = Lists.newArrayList();
        List<String> matches = Lists.newArrayList();
        matches.add(getBlobIdForResultDoc(searcher, result, 0));
        matches.add(getBlobIdForResultDoc(searcher, result, 1));
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
        IndexStore index = mbox.index.getIndexStore();
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        Contact contact1 = createContact(mbox, "James", "Peters", "test1@zimbra.com");
        createContact(mbox, "Emma", "Peters", "test2@zimbra.com");


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
        IndexStore index = mbox.index.getIndexStore();
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        createContact(mbox, "Jane", "Peters", "test1@zimbra.com");
        createContact(mbox, "Emma", "Peters", "test2@zimbra.com");
        createContact(mbox, "Fiona", "Peters", "test3@zimbra.com");
        createContact(mbox, "Edward", "Peters", "test4@zimbra.com");

        indexer.close();

        ZimbraIndexSearcher searcher = index.openSearcher();
        Assert.assertEquals("numDocs after adding 4 contacts", 4, searcher.getIndexReader().numDocs());
        Assert.assertEquals("docs which match 'test1'", 1,
                searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "test1")));
        Assert.assertEquals("docs which match '@zimbra.com'", 4,
               searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")));
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
        createContact(mbox, "teSt1@ziMBRA.com");
        createContact(mbox, "test2@zimbra.com");


        IndexStore index = mbox.index.getIndexStore();
        ZimbraIndexSearcher searcher = index.openSearcher();
        // Note that TermFieldEnumeration order is defined to be sorted
        try (TermFieldEnumeration fields = searcher.getIndexReader()
            .getTermsForField(LuceneFields.L_CONTACT_DATA)) {
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "@zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test1"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test2"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "test2@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_CONTACT_DATA);
        } 
        try (TermFieldEnumeration fields = searcher.getIndexReader()
            .getTermsForField(LuceneFields.L_CONTENT)) {
            // l.content values:
            // "test1@zimbra.com test1 @zimbra.com zimbra.com zimbra @zimbra  "
            // "test2@zimbra.com test2 @zimbra.com zimbra.com zimbra @zimbra  "
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test1"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test2"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "test2@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_CONTENT, "zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_CONTENT);
        }
        try (TermFieldEnumeration fields = searcher.getIndexReader()
            .getTermsForField(LuceneFields.L_FIELD)) {
            checkNextTerm(fields, new Term(LuceneFields.L_FIELD, "email:test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_FIELD, "email:test2@zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_FIELD);
        }
        try (TermFieldEnumeration fields = searcher.getIndexReader()
            .getTermsForField(LuceneFields.L_PARTNAME)) {
            checkNextTerm(fields, new Term(LuceneFields.L_PARTNAME, "CONTACT"));
            checkAtEnd(fields, LuceneFields.L_PARTNAME);
        }
        try (TermFieldEnumeration fields = searcher.getIndexReader()
            .getTermsForField(LuceneFields.L_H_TO)) {
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "@zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test1"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test1@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test2"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "test2@zimbra.com"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "zimbra"));
            checkNextTerm(fields, new Term(LuceneFields.L_H_TO, "zimbra.com"));
            checkAtEnd(fields, LuceneFields.L_H_TO);
        }
        try (TermFieldEnumeration fields = searcher.getIndexReader()
            .getTermsForField(LuceneFields.L_SORT_DATE)) {
            checkNextTermFieldType(fields, LuceneFields.L_SORT_DATE);
            // Check fails on ES, because ElasticSearch has more.  Not sure why and not sure it matters.
            // Check passes on Solr and on Lucene
            checkAtEnd(fields, LuceneFields.L_SORT_DATE);
        }
        try (TermFieldEnumeration fields = searcher.getIndexReader()
            .getTermsForField(LuceneFields.L_MAILBOX_BLOB_ID)) {
            checkNextTermFieldType(fields, LuceneFields.L_MAILBOX_BLOB_ID);
            checkNextTermFieldType(fields, LuceneFields.L_MAILBOX_BLOB_ID);
            // Check fails on ES, because ElasticSearch has more.  Investigate?  Believe it relates to fact that is a number field
            // Numbers have an associated precision step (number of terms generated for each number value)
            // which defaults to 4.
            //Check passes on Solr and on Lucene
            checkAtEnd(fields, LuceneFields.L_MAILBOX_BLOB_ID);
        }
        searcher.close();
    }

    private Contact createContact(Mailbox mbox, String email)
            throws ServiceException {
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        return mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, email)), folder.getId(), null);
    }

    private Contact createContact(Mailbox mbox, String firstName, String lastName, String email)
            throws ServiceException {
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Map<String, Object> fields;
        fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, firstName,
                ContactConstants.A_lastName, lastName,
                ContactConstants.A_email, email);
        return mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
    }

    private Contact createContact(Mailbox mbox, String firstName, String lastName, String email, String jobTitle)
            throws ServiceException {
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Map<String, Object> fields;
        fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, firstName,
                ContactConstants.A_lastName, lastName,
                ContactConstants.A_jobTitle, jobTitle,
                ContactConstants.A_email, email);
        return mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);
    }

    private static String getBlobIdForResultDoc(ZimbraIndexSearcher searcher, ZimbraTopDocs result, int index)
            throws IOException, ServiceException {
        return searcher.doc(result.getScoreDoc(index).getDocumentID()).get(LuceneFields.L_MAILBOX_BLOB_ID);
    }

}
