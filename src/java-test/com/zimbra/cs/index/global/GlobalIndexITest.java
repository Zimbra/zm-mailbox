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
package com.zimbra.cs.index.global;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.ParsedDocument;

/**
 * Integration test for {@link GlobalIndex}.
 *
 * @author ysasaki
 */
public final class GlobalIndexITest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        HBaseIndexTestUtils.initSchema();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void search() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        mbox.grantAccess(null, Mailbox.ID_FOLDER_BRIEFCASE, null, ACL.GRANTEE_AUTHUSER, ACL.RIGHT_READ, null);
        ParsedDocument pdoc = new ParsedDocument(IOUtils.toInputStream("test"),
                "filename.txt", "text/plain", 12345L, "creator", "description");
        Document doc = mbox.createDocument(null, Mailbox.ID_FOLDER_BRIEFCASE, pdoc, MailItem.Type.DOCUMENT, 0);

        HBaseIndex index = HBaseIndexTestUtils.createIndex(mbox);
        index.deleteIndex();

        Indexer indexer = index.openIndexer();
        indexer.addDocument(doc, doc.generateIndexData());
        indexer.close();

        TermQuery query = new TermQuery(new Term(LuceneFields.L_CONTENT, "test"));
        List<GlobalDocument> hits = index.getGlobalIndex().search(query);
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals(doc.getName(), hits.get(0).getFilename());
        Assert.assertEquals(doc.getDate(), hits.get(0).getDate());
    }

}
