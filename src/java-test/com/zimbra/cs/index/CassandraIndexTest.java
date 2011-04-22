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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.CassandraDaemon;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;
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

        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @AfterClass
    public static void destroy() {
        if (cassandra != null) {
            cassandra.destroy();
        }
    }

    @Test
    public void search() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "First");
        fields.put(ContactConstants.A_lastName, "Last");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        CassandraIndex.Factory factory = new CassandraIndex.Factory();
        factory.createSchema();
        CassandraIndex index = factory.createIndex(mbox);
        index.deleteIndex();
        Indexer indexer = index.openIndexer();
        indexer.addDocument(contact, contact.generateIndexData());
        indexer.close();

        Searcher searcher = index.openSearcher();
        List<Integer> result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "none@zimbra.com")),
                null, null, 100);
        Assert.assertEquals(0, result.size());

        result = searcher.search(new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, "test@zimbra.com")),
                null, null, 100);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(contact.getId(), result.get(0).intValue());
        searcher.close();
    }

}
