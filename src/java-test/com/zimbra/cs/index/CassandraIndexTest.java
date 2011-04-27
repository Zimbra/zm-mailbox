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
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.CassandraDaemon;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedContact;

/**
 * Unit test for {@link CassandraIndex}.
 *
 * @author ysasaki
 */
public final class CassandraIndexTest {
    private static CassandraDaemon cassandra;
    private static final CassandraIndex.Factory FACTORY = new CassandraIndex.Factory();

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

        FACTORY.createSchema();
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
    public void search() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "First");
        fields.put(ContactConstants.A_lastName, "Last");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        CassandraIndex index = FACTORY.getInstance(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(contact, contact.generateIndexData());
        indexer.close();

        Searcher searcher = index.openSearcher();
        List<Integer> result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "none@zimbra.com")),
                null, null, 100);
        Assert.assertEquals(0, result.size());

        Assert.assertEquals(1, searcher.getTotal());
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "test@zimbra.com")),
                null, null, 100);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(contact.getId(), result.get(0).intValue());
        searcher.close();
    }

    @Test
    public void deleteDocument() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Contact contact1 = mbox.createContact(null, new ParsedContact(Collections.singletonMap(ContactConstants.A_email,
                "test1@zimbra.com")), Mailbox.ID_FOLDER_CONTACTS, null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(Collections.singletonMap(ContactConstants.A_email,
                "test2@zimbra.com")), Mailbox.ID_FOLDER_CONTACTS, null);

        CassandraIndex index = FACTORY.getInstance(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(contact1, contact1.generateIndexData());
        indexer.addDocument(contact2, contact2.generateIndexData());
        indexer.close();

        Searcher searcher = index.openSearcher();
        Assert.assertEquals(2, searcher.getTotal());
        List<Integer> result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")),
                null, null, 100);
        Assert.assertEquals(2, result.size());

        indexer = index.openIndexer();
        indexer.deleteDocument(Collections.singletonList(contact1.getId()));
        indexer.close();

        Assert.assertEquals(1, searcher.getTotal());
        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "@zimbra.com")),
                null, null, 100);
        Assert.assertEquals(1, result.size());

        searcher.close();
    }

}
