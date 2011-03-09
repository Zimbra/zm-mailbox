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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;

/**
 * Unit test for {@link Mailbox}.
 *
 * @author ysasaki
 */
public final class MailboxTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning prov = new MockProvisioning();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, "0-0-0");
        attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        prov.createAccount("test@zimbra.com", "secret", attrs);
        Provisioning.setInstance(prov);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase();

        MailboxManager.setInstance(null);
        MailboxIndex.startup();

        LC.zimbra_class_store.setDefault(MockStoreManager.class.getName());
        StoreManager.getInstance().startup();
    }

    @Before
    public void setUp() throws Exception {
        HSQLDB.clearDatabase();
        MailboxManager.getInstance().clearCache();
        MailboxManager.getInstance().getMailboxByAccountId("0-0-0").index.deleteIndex();
    }

    @Test
    public void browse() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId("0-0-0");
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.addMessage(null, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test1-2@sub1.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test1-3@sub1.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test1-4@sub1.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test2-1@sub2.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test2-2@sub2.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test2-3@sub2.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test3-1@sub3.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test3-2@sub3.zimbra.com".getBytes(), false), opt);
        mbox.addMessage(null, new ParsedMessage("From: test4-1@sub4.zimbra.com".getBytes(), false), opt);
        mbox.index.indexDeferredItems();

        List<BrowseTerm> terms = mbox.browse(null, Mailbox.BrowseBy.domains, null, 100);
        Assert.assertEquals(4, terms.size());
        Assert.assertEquals("sub1.zimbra.com", terms.get(0).getText());
        Assert.assertEquals("sub2.zimbra.com", terms.get(1).getText());
        Assert.assertEquals("sub3.zimbra.com", terms.get(2).getText());
        Assert.assertEquals("sub4.zimbra.com", terms.get(3).getText());
        Assert.assertEquals(4, terms.get(0).getFreq());
        Assert.assertEquals(3, terms.get(1).getFreq());
        Assert.assertEquals(2, terms.get(2).getFreq());
        Assert.assertEquals(1, terms.get(3).getFreq());
    }

}
