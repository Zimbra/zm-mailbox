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
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailAddress;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;

/**
 * Unit test for {@link Contact}.
 *
 * @author ysasaki
 */
public final class ContactTest {

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
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
    }

    @Test
    public void updateAddressCount() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        fields.put(ContactConstants.A_workEmail1, "test2@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test2@zimbra.com"));
        fields.clear();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        fields.put(ContactConstants.A_workEmail1, "test1@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);
        Assert.assertEquals(2, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        conn.closeQuietly();
    }

    @Test
    public void updateAddressCountOnSoftDelete() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.move(null, contact.getId(), MailItem.Type.CONTACT, Mailbox.ID_FOLDER_TRASH); // soft-delete
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.move(null, contact.getId(), MailItem.Type.CONTACT, Mailbox.ID_FOLDER_INBOX); // soft-recover
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        conn.closeQuietly();
    }

    @Test
    public void updateAddressCountOnHardDelete() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        Contact contact1 = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);
        Contact contact2 = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        DbConnection conn = DbPool.getConnection(mbox);
        Assert.assertEquals(2, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.delete(null, contact1.getId(), MailItem.Type.CONTACT); // hard-delete from Inbox
        Assert.assertEquals(1, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.move(null, contact2.getId(), MailItem.Type.CONTACT, Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        mbox.delete(null, contact2.getId(), MailItem.Type.CONTACT); // hard-delete from Trash
        Assert.assertEquals(0, DbMailAddress.getCount(conn, mbox, "test1@zimbra.com"));
        conn.closeQuietly();
    }

}
