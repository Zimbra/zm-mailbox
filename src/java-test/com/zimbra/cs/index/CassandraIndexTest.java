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
package com.zimbra.cs.index;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cassandra.thrift.CassandraDaemon;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedContact;

/**
 * Unit test for {@link CassandraIndex}.
 *
 * @author ysasaki
 */

@Ignore("bug 67353") 
public final class CassandraIndexTest {
    private static CassandraDaemon cassandra;
    private static CassandraIndex.Factory factory;

    @BeforeClass
    public static void init() throws Exception {
        File dir = new File("build/test/cassandra");
        if (dir.exists()) {
            Files.deleteRecursively(dir);
        }
        System.setProperty("log4j.configuration", "log4j-test.properties");
        System.setProperty("cassandra.config", "cassandra-test.yaml");
        cassandra = new CassandraDaemon();
        cassandra.init(null);
        cassandra.start();
        LC.cassandra_host.setDefault("localhost:7160");

        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());

        factory = new CassandraIndex.Factory();
        factory.createSchema();
    }

    @AfterClass
    public static void destroy() {
        if (cassandra != null) {
            cassandra.destroy();
        }
    }

    @Before
    public void setup() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void searchByTermQuery() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Map<String, Object> fields = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "First",
                ContactConstants.A_lastName, "Last",
                ContactConstants.A_email, "test@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), folder.getId(), null);

        CassandraIndex index = factory.getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact, contact.generateIndexData());
        indexer.close();

        IndexSearcher searcher = index.openSearcher();
        TopDocs result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "none@zimbra.com")), 100);
        Assert.assertEquals(0, result.totalHits);

        Assert.assertEquals(1, searcher.getIndexReader().numDocs());
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "test@zimbra.com")), 100);
        Assert.assertEquals(1, result.totalHits);
        Assert.assertEquals(String.valueOf(contact.getId()),
                searcher.doc(result.scoreDocs[0].doc).get(LuceneFields.L_MAILBOX_BLOB_ID));
        searcher.close();
    }

    @Test
    public void searchByPrefixQuery() throws Exception {
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

        CassandraIndex index = factory.getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.addDocument(folder, contact4, contact4.generateIndexData());
        indexer.close();

        IndexSearcher searcher = index.openSearcher();
        TopDocs result = searcher.search(new PrefixQuery(new Term(LuceneFields.L_CONTACT_DATA, "ab")), 100);
        Assert.assertEquals(2, result.totalHits);
        Assert.assertEquals(String.valueOf(contact1.getId()),
                searcher.doc(result.scoreDocs[0].doc).get(LuceneFields.L_MAILBOX_BLOB_ID));
        Assert.assertEquals(String.valueOf(contact2.getId()),
                searcher.doc(result.scoreDocs[1].doc).get(LuceneFields.L_MAILBOX_BLOB_ID));
        searcher.close();
    }

    @Test
    public void deleteDocument() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test2@zimbra.com")), folder.getId(), null);

        CassandraIndex index = factory.getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.close();

        IndexSearcher searcher = index.openSearcher();
        Assert.assertEquals(2, searcher.getIndexReader().numDocs());
        TopDocs result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
        Assert.assertEquals(2, result.totalHits);
        searcher.close();

        indexer = index.openIndexer();
        indexer.deleteDocument(Collections.singletonList(contact1.getId()));
        indexer.close();

        searcher = index.openSearcher();
        Assert.assertEquals(1, searcher.getIndexReader().numDocs());
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")), 100);
        Assert.assertEquals(1, result.totalHits);
        searcher.close();
    }

    @Test
    public void getCount() throws Exception {
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

        CassandraIndex index = factory.getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.addDocument(folder, contact3, contact3.generateIndexData());
        indexer.addDocument(folder, contact4, contact4.generateIndexData());
        indexer.close();

        IndexSearcher searcher = index.openSearcher();
        Assert.assertEquals(4, searcher.getIndexReader().numDocs());
        Assert.assertEquals(1, searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "test1")));
        Assert.assertEquals(4, searcher.docFreq(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")));
        searcher.close();
    }

    @Test
    public void termEnum() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_CONTACTS);
        Contact contact1 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test1@zimbra.com")), folder.getId(), null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(
                Collections.singletonMap(ContactConstants.A_email, "test2@zimbra.com")), folder.getId(), null);

        CassandraIndex index = factory.getIndexStore(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, contact1, contact1.generateIndexData());
        indexer.addDocument(folder, contact2, contact2.generateIndexData());
        indexer.close();

        IndexSearcher searcher = index.openSearcher();
        TermEnum terms = searcher.getIndexReader().terms(new Term(LuceneFields.L_CONTACT_DATA, ""));
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra"), terms.term());
        Assert.assertTrue(terms.next());
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com"), terms.term());
        Assert.assertTrue(terms.next());
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "test1"), terms.term());
        Assert.assertTrue(terms.next());
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "test1@zimbra.com"), terms.term());
        Assert.assertTrue(terms.next());
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "test2"), terms.term());
        Assert.assertTrue(terms.next());
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "test2@zimbra.com"), terms.term());
        Assert.assertTrue(terms.next());
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "zimbra"), terms.term());
        Assert.assertTrue(terms.next());
        Assert.assertEquals(new Term(LuceneFields.L_CONTACT_DATA, "zimbra.com"), terms.term());
        Assert.assertFalse(terms.next());
        searcher.close();
    }

}
