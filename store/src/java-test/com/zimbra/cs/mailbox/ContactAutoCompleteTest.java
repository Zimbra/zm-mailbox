/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.account.Key;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactAutoComplete.AutoCompleteResult;
import com.zimbra.cs.mailbox.ContactAutoComplete.ContactEntry;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.util.ZTestWatchman;

/**
 * Unit test for {@link ContactAutoComplete}.
 *
 * @author ysasaki
 */
@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public final class ContactAutoCompleteTest {

    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("zimbra.config", "../store/src/java-test/localconfig-test.xml");
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
       System.out.println(testName.getMethodName());
       Provisioning prov = Provisioning.getInstance();
       prov.createAccount("testContAC@zimbra.com", "secret", new HashMap<String, Object>());
       prov.createAccount("test2@zimbra.com", "secret", new HashMap<String, Object>());
       Provisioning.setInstance(prov);
    }

    @Test
    public void hitContact() throws Exception {
        ContactAutoComplete.AutoCompleteResult result = new ContactAutoComplete.AutoCompleteResult(10);
        Account account = Provisioning.getInstance().getAccountByName("testContAC@zimbra.com");
        result.rankings = new ContactRankings(account.getId());
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
    
    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void lastNameFirstName() throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("testContAC@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "First");
        fields.put(ContactConstants.A_lastName, "Last");
        fields.put(ContactConstants.A_email, "test1@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(1, autocomplete.query("first last", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("last first", null, 100).entries.size());
    }

    @Ignore
    public void spaceInFirstName() throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("testContACEnv@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
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
        Account account = Provisioning.getInstance().getAccountByName("testContAC@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "not and or");
        fields.put(ContactConstants.A_lastName, "subject: from:");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        Thread.sleep(500);
        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(1, autocomplete.query("not", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("not and", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("not and or", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("subject:", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("subject: from:", null, 100).entries.size());
    }

    @Test
    public void dash() throws Exception {
        Account account = Provisioning.getInstance().getAccountByName("testContAC@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "Conf - Hillview");
        fields.put(ContactConstants.A_lastName, "test.server-vmware - dash");
        fields.put(ContactConstants.A_email, "test@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        Thread.sleep(500);
        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(1, autocomplete.query("conf -", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("conf - h", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("test.server-vmware -", null, 100).entries.size());
        Assert.assertEquals(1, autocomplete.query("test.server-vmware - d", null, 100).entries.size());
    }

    @Test
    public void hitGroup() throws Exception {
        ContactAutoComplete.AutoCompleteResult result = new ContactAutoComplete.AutoCompleteResult(10);
        Account account = Provisioning.getInstance().getAccountByName("test2@zimbra.com");
        result.rankings = new ContactRankings(account.getId());
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
        Account account = Provisioning.getInstance().getAccountByName("testContAC@zimbra.com");
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

        comp.addMatchedContacts("middle last", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();

        comp.addMatchedContacts("middle la", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(1, result.entries.size());
        result.clear();

        comp.addMatchedContacts("ddle last", attrs, Mailbox.ID_FOLDER_CONTACTS, null, result);
        Assert.assertEquals(0, result.entries.size());
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
        Account account = Provisioning.getInstance().getAccountByName("testContAC@zimbra.com");

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

    @Test
    public void rankingTestContactWithSameEmailDifferentDisplayName() throws Exception {
        // Autocomplete should show same ranking for a email address present in difference contacts.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(ContactConstants.A_firstName, "Pal");
        fields.put(ContactConstants.A_lastName, "One");
        fields.put(ContactConstants.A_email, "testauto@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields), Mailbox.ID_FOLDER_CONTACTS, null);

        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put(ContactConstants.A_email, "testauto@zimbra.com");
        mbox.createContact(null, new ParsedContact(fields1), Mailbox.ID_FOLDER_CONTACTS, null);

        ContactRankings.increment(mbox.getAccountId(), Collections.singleton(new InternetAddress("testauto@zimbra.com")));
        ContactRankings.increment(mbox.getAccountId(), Collections.singleton(new InternetAddress("testauto@zimbra.com")));

        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        AutoCompleteResult result = autocomplete.query("Pal", null, 10);
        Assert.assertEquals(1, result.entries.size());
        for (ContactEntry ce : result.entries) {
            Assert.assertEquals(2, ce.mRanking);
        }
        result.clear();

        result = autocomplete.query("testauto", null, 10);
        Assert.assertEquals(2, result.entries.size());
        for (ContactEntry ce : result.entries) {
            Assert.assertEquals(2, ce.mRanking);
        }
    }

    @Test
    public void autocompleteTestNonExistingContact() throws Exception {
        //AutoComplete should not return entry present in ranking table but contact does not exist.
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        ContactRankings.increment(mbox.getAccountId(), Collections.singleton(new InternetAddress("noex@zimbra.com")));
        ContactRankings.increment(mbox.getAccountId(), Collections.singleton(new InternetAddress("noex@zimbra.com")));
        ContactAutoComplete autocomplete = new ContactAutoComplete(mbox.getAccount(), new OperationContext(mbox));
        Assert.assertEquals(0, autocomplete.query("noex", null, 10).entries.size());
     }
}
