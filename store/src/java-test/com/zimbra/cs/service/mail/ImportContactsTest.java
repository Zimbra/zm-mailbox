/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.ContactGroup.Member;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.formatter.ContactCSV;
import com.zimbra.cs.service.util.ItemId;

public class ImportContactsTest {
    private static final String USERNAME = "user1@zimbra.com";
    private static final String USERDN = "uid=user1,ou=people,dc=zimbra,dc=com";

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("zimbra.config", "../store/src/java-test/localconfig-test.xml");
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount(USERNAME, "secret", new HashMap<String, Object>());
        Provisioning.setInstance(prov);
    }

    @After
    public void tearDown() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        prov.deleteAccount(USERNAME);
    }

    @Test
    public void testImportContactsOK() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.getAccountByName(USERNAME);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

		String csvText = getCsvFile();
		BufferedReader reader = new BufferedReader(new StringReader(csvText));
        List<Map<String, String>> contactsMap = ContactCSV.getContacts(reader, null);

        List<ItemId> ids = ImportContacts.ImportCsvContacts(null, mbox,
				new ItemId(mbox, Mailbox.ID_FOLDER_CONTACTS), contactsMap);

        Assert.assertFalse(ids.isEmpty());
        Assert.assertEquals(4, ids.size());

		Set<String> expectedContactEmails = getExpectedContactEmails();
        Set<Contact> foundContactGroups = new HashSet<>();
        
        for (ItemId id : ids) {
            Contact contact = mbox.getContactById(null, id.getId());
            Assert.assertNotNull(contact);
            
            for (String addr : contact.getEmailAddresses()) {
                expectedContactEmails.remove(addr);
            }

            if (contact.isContactGroup()) {
                foundContactGroups.add(contact);
            }
        }

        if (!expectedContactEmails.isEmpty()) {
            Assert.fail("Missing expected contact emails: " + expectedContactEmails);
        }

        Assert.assertEquals("Found exactly one contact group after import",
				1, foundContactGroups.size());

		ContactGroup group = ContactGroup.init(foundContactGroups.iterator().next(), true);

        for (Member member : group.getMembers()) {
            Member.Type memberType = member.getType();
            
            if (memberType == Member.Type.INLINE) {
                Assert.assertEquals("Found correct inline group member", "delta.user@example.org",
                        member.getValue());
            } else if (memberType != Member.Type.CONTACT_REF) {
                Assert.fail(String.format("Found unexpected group member of type %s with value \"%s\"", memberType,
                        member.getValue()));
            }
        }
    }

	private String getCsvFile() {
		return ""
			+ "email,fullName,type,dlist\r\n"
			+ "alpha.user@example.org,Alpha User,inline,\"\"\r\n"
			+ "bravo.user@example.org,Bravo User,inline,\"\"\r\n"
			+ "testgroup@example.org,Test Group,group,\"alpha.user@example.org,bravo.user@example.org,charlie.user@example.org,delta.user@example.org\"\r\n"
			+ "charlie.user@example.org,Charlie User,inline,\"\"\r\n";
	}

    private Set<String> getExpectedContactEmails() {
		Set<String> expectedContactEmails = new HashSet<>();
		expectedContactEmails.add("alpha.user@example.org");
        expectedContactEmails.add("bravo.user@example.org");
        expectedContactEmails.add("charlie.user@example.org");
        expectedContactEmails.add("delta.user@example.org");
        expectedContactEmails.add("testgroup@example.org");

		return expectedContactEmails;
	}

    @Test
    public void testFoo() throws Exception {
        Provisioning prov = Provisioning.getInstance();
//        Account account = prov.getAccountBy(USERDN);
        SearchDirectoryOptions sdo = new SearchDirectoryOptions();
        sdo.setFilterString((FilterId) null, USERDN);
        List<NamedEntry> entries = prov.searchDirectory(sdo);
        Assert.assertFalse(entries.isEmpty());
    }
}

