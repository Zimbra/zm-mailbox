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
import java.util.Map;

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
import com.zimbra.cs.mailbox.Folder;
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
    private static final String GRANTEE1 = "11111111-1111-1111-1111-111111111111";
    private static final String GRANTEE2 = "22222222-2222-2222-2222-222222222222";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, GRANTEE1);
        prov.createAccount("user1@zimbra.com", "secret", attrs);
        attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, GRANTEE2);
        prov.createAccount("user2@zimbra.com", "secret", attrs);
        HBaseIndexTestUtils.initSchema();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void search() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_BRIEFCASE);
        ParsedDocument pdoc = new ParsedDocument(IOUtils.toInputStream("test"),
                "filename.txt", "text/plain", 12345L, "creator", "description");
        Document doc = mbox.createDocument(null, folder.getId(), pdoc, MailItem.Type.DOCUMENT, 0);

        HBaseIndex index = HBaseIndexTestUtils.createIndex(mbox);
        index.deleteIndex();

        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, doc, doc.generateIndexData());
        indexer.close();

        TermQuery query = new TermQuery(new Term(LuceneFields.L_CONTENT, "test"));
        List<GlobalDocument> hits = index.getGlobalIndex().search(MockProvisioning.DEFAULT_ACCOUNT_ID, query);
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals(doc.getName(), hits.get(0).getFilename());
        Assert.assertEquals(doc.getDate(), hits.get(0).getDate());
    }

    @Test
    public void acl() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_BRIEFCASE);
        mbox.grantAccess(null, folder.getId(), GRANTEE1, ACL.GRANTEE_USER, ACL.RIGHT_READ, null);
        ParsedDocument pdoc = new ParsedDocument(IOUtils.toInputStream("ACL"),
                "acl-test.txt", "text/plain", 12345L, "creator", "description");
        Document doc = mbox.createDocument(null, folder.getId(), pdoc, MailItem.Type.DOCUMENT, 0);

        HBaseIndex index = HBaseIndexTestUtils.createIndex(mbox);
        index.deleteIndex();

        Indexer indexer = index.openIndexer();
        indexer.addDocument(folder, doc, doc.generateIndexData());
        indexer.close();

        TermQuery query = new TermQuery(new Term(LuceneFields.L_CONTENT, "acl"));
        List<GlobalDocument> hits = index.getGlobalIndex().search(MockProvisioning.DEFAULT_ACCOUNT_ID, query);
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals(doc.getName(), hits.get(0).getFilename());
        Assert.assertEquals(doc.getDate(), hits.get(0).getDate());

        hits = index.getGlobalIndex().search(GRANTEE1, query);
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals(doc.getName(), hits.get(0).getFilename());
        Assert.assertEquals(doc.getDate(), hits.get(0).getDate());

        hits = index.getGlobalIndex().search(GRANTEE2, query);
        Assert.assertEquals(0, hits.size());
    }

    @Test
    public void updateACL() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder personal = mbox.createFolder(null, "closed", Mailbox.ID_FOLDER_BRIEFCASE, MailItem.Type.DOCUMENT,
                0, (byte) 0, null);
        Folder shared = mbox.createFolder(null, "shared", Mailbox.ID_FOLDER_BRIEFCASE, MailItem.Type.DOCUMENT,
                0, (byte) 0, null);
        mbox.grantAccess(null, shared.getId(), GRANTEE1, ACL.GRANTEE_USER, ACL.RIGHT_READ, null);
        ParsedDocument pdoc = new ParsedDocument(IOUtils.toInputStream("shared"),
                "acl-test.txt", "text/plain", 12345L, "creator", "description");
        Document doc = mbox.createDocument(null, personal.getId(), pdoc, MailItem.Type.DOCUMENT, 0);

        HBaseIndex index = HBaseIndexTestUtils.createIndex(mbox);
        index.deleteIndex();

        Indexer indexer = index.openIndexer();
        indexer.addDocument(personal, doc, doc.generateIndexData());
        indexer.close();

        TermQuery query = new TermQuery(new Term(LuceneFields.L_CONTENT, "shared"));
        List<GlobalDocument> hits = index.getGlobalIndex().search(GRANTEE1, query);
        hits = index.getGlobalIndex().search(GRANTEE1, query);
        Assert.assertEquals(0, hits.size());

        mbox.move(null, doc.getId(), doc.getType(), shared.getId()); // move closed to shared
        hits = index.getGlobalIndex().search(GRANTEE1, query);
        Assert.assertEquals(1, hits.size());
        Assert.assertEquals(doc.getName(), hits.get(0).getFilename());
        Assert.assertEquals(doc.getDate(), hits.get(0).getDate());

        mbox.revokeAccess(null, shared.getId(), GRANTEE1); // revoke the right
        hits = index.getGlobalIndex().search(GRANTEE1, query);
        Assert.assertEquals(0, hits.size());
    }

}
