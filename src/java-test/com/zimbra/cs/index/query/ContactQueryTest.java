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
package com.zimbra.cs.index.query;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedContact;

/**
 * Unit test for {@link ContactQuery}.
 *
 * @author ysasaki
 */
public final class ContactQueryTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void tokenize() throws Exception {
        Assert.assertEquals("Q(CONTACT:john,smith)", new ContactQuery("John Smith").toString());
    }

    @Test
    public void search() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "Michael");
        fields.put(ContactConstants.A_lastName, "Smith");
        fields.put(ContactConstants.A_email, "michael.smith@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);
        fields.put(ContactConstants.A_firstName, "Jonathan");
        fields.put(ContactConstants.A_lastName, "Smith");
        fields.put(ContactConstants.A_email, "jonathan.smith@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ZimbraQueryResults results = mbox.index.search(new OperationContext(mbox), "contact:\"Jon Smith\"",
                EnumSet.of(MailItem.Type.CONTACT), SortBy.NONE, 100);
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(contact.getId(), results.getNext().getItemId());
        results.close();
    }

    @Test
    public void wildcard() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "First*");
        fields.put(ContactConstants.A_lastName, "Las*t");
        fields.put(ContactConstants.A_email, "first.last@zimbra.com");
        Contact contact = mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ZimbraQueryResults results = mbox.index.search(new OperationContext(mbox), "contact:\"First\"",
                EnumSet.of(MailItem.Type.CONTACT), SortBy.NONE, 100);
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(contact.getId(), results.getNext().getItemId());
        results.close();

        results = mbox.index.search(new OperationContext(mbox), "contact:\"First*\"",
                EnumSet.of(MailItem.Type.CONTACT), SortBy.NONE, 100);
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(contact.getId(), results.getNext().getItemId());
        results.close();

        results = mbox.index.search(new OperationContext(mbox), "contact:\"Las*\"",
                EnumSet.of(MailItem.Type.CONTACT), SortBy.NONE, 100);
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(contact.getId(), results.getNext().getItemId());
        results.close();

        results = mbox.index.search(new OperationContext(mbox), "contact:\"Las*t\"",
                EnumSet.of(MailItem.Type.CONTACT), SortBy.NONE, 100);
        Assert.assertTrue(results.hasNext());
        Assert.assertEquals(contact.getId(), results.getNext().getItemId());
        results.close();
    }

}
