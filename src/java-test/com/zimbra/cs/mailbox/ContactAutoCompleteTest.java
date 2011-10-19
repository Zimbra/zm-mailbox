/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.ParsedContact;

/**
 * Unit test for {@link ContactAutoComplete}.
 *
 * @author ysasaki
 */
public final class ContactAutoCompleteTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void hitContact() throws Exception {
        ContactAutoComplete.AutoCompleteResult result = new ContactAutoComplete.AutoCompleteResult(10);
        result.rankings = new ContactRankings(MockProvisioning.DEFAULT_ACCOUNT_ID);
        ContactAutoComplete.ContactEntry contact = new ContactAutoComplete.ContactEntry();
        contact.mDisplayName = "C1";
        contact.mEmail = "c1@zimbra.com";
        result.addEntry(contact);
        Assert.assertEquals(result.entries.size(), 1);

        contact = new ContactAutoComplete.ContactEntry();
        contact.mDisplayName = "C2";
        contact.mEmail = "c2@zimbra.com";
        result.addEntry(contact);
        Assert.assertEquals(result.entries.size(), 2);
    }

    @Test
    public void lastNameFirstName() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "First");
        fields.put(ContactConstants.A_lastName, "Last");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(1, autocomplete.query("first last", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("last first", null, 100).entries.size());
    }

    @Test
    public void spaceInFirstName() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "First Second Third Forth");
        fields.put(ContactConstants.A_lastName, "Last");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(1, autocomplete.query("first second third forth", null, 100).entries.size());
    }

    @Test
    public void reservedQueryTerm() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "not and or");
        fields.put(ContactConstants.A_lastName, "subject: from:");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(1, autocomplete.query("not", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("not and", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("not and or", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("subject:", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("subject: from:", null, 100).entries.size());
    }

    @Test
    public void dash() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "Conf - Hillview");
        fields.put(ContactConstants.A_lastName, "test.server-vmware - dash");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(1, autocomplete.query("conf -", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("conf - h", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("test.server-vmware -", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("test.server-vmware - d", null, 100).entries.size());
    }

    @Test
    public void hitGroup() throws Exception {
        ContactAutoComplete.AutoCompleteResult result = new ContactAutoComplete.AutoCompleteResult(10);
        result.rankings = new ContactRankings(MockProvisioning.DEFAULT_ACCOUNT_ID);
        ContactAutoComplete.ContactEntry group = new ContactAutoComplete.ContactEntry();
        group.mDisplayName = "G1";
        group.mIsContactGroup = true;
        result.addEntry(group);
        Assert.assertEquals(result.entries.size(), 1);

        group = new ContactAutoComplete.ContactEntry();
        group.mDisplayName = "G2";
        group.mIsContactGroup = true;
        result.addEntry(group);
        Assert.assertEquals(result.entries.size(), 2);
    }

    @Test
    public void addMatchedContacts() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        ContactAutoComplete comp = new ContactAutoComplete(account, null);
        ContactAutoComplete.AutoCompleteResult result = new ContactAutoComplete.AutoCompleteResult(10);
        result.rankings = new ContactRankings(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> attrs = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "First",
                ContactConstants.A_middleName, "Middle",
                ContactConstants.A_lastName, "Last",
                ContactConstants.A_email, "first.last@zimbra.com");
        comp.addMatchedContacts("first f", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(0, result.entries.size());
        result.clear();


        comp.addMatchedContacts("first mid", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();

        comp.addMatchedContacts("first la", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();

        comp.addMatchedContacts("first mid la", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();

        attrs = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "Conf - hillview",
                ContactConstants.A_lastName, "test.server-vmware - dash",
                ContactConstants.A_email, "conf-hillview@zimbra.com");

        comp.addMatchedContacts("test.server-vmware -", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();

        comp.addMatchedContacts("test.server-vmware - d", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();


        comp.addMatchedContacts("conf - h", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();
    }

    @Test
    public void addMatchedContactsWithUnicodeCase() throws Exception {
        Account account = Provisioning.getInstance().getAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        ContactAutoComplete comp = new ContactAutoComplete(account, null);
        ContactAutoComplete.AutoCompleteResult result = new ContactAutoComplete.AutoCompleteResult(10);
        result.rankings = new ContactRankings(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> attrs = ImmutableMap.<String, Object>of(
                ContactConstants.A_firstName, "\u0421\u0440\u043f\u0441\u043a\u0438 \u0411\u043e\u0441\u043d\u0430 \u0438 \u0425\u0435\u0440\u0446\u0435\u0433\u043e\u0432\u0438\u043d\u0430",
                ContactConstants.A_lastName, "\u0441\u043a\u0438 \u0411\u043e\u0441\u043d\u0430 \u0438 \u0425\u0435\u0440\u0446\u0435\u0433\u043e\u0432\u0438\u043d\u0430",
                ContactConstants.A_email, "sr_BA@i18n.com");
        comp.addMatchedContacts("\u0421\u0440\u043f\u0441\u043a\u0438 \u0411\u043e\u0441\u043d\u0430 \u0438 \u0425\u0435\u0440\u0446\u0435\u0433\u043e\u0432\u0438\u043d\u0430",
                attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();
    }
}
