/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.ZimbraSortBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.CreateContactRequest;
import com.zimbra.soap.mail.message.CreateContactResponse;
import com.zimbra.soap.mail.message.GetContactsRequest;
import com.zimbra.soap.mail.message.GetContactsResponse;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.message.SearchResponse;
import com.zimbra.soap.mail.type.ContactInfo;
import com.zimbra.soap.mail.type.ContactSpec;
import com.zimbra.soap.mail.type.NewContactAttr;
import com.zimbra.soap.mail.type.NewContactGroupMember;
import com.zimbra.soap.type.ContactAttr;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.SearchHit;

public class TestGetContactsRequest extends SoapTest {
    @Rule
    public TestName testInfo = new TestName();
    private Account acct;
    private List<String> ids;
    private SoapTransport transport;
    private SoapTransport transport2;
    private static String USER_NAME;
    private static String USER2_NAME;

    @After
    public void cleanup() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(USER2_NAME);
    }

    @Before
    public void setUp() throws Exception {
        USER_NAME = testInfo.getMethodName() + "-user1";
        USER2_NAME = testInfo.getMethodName() + "-user2";
        ids = Lists.newArrayListWithExpectedSize(3);
        acct = TestUtil.createAccount(USER_NAME);
        transport = authUser(acct.getName());
        ids = createContacts(transport, USER_NAME + "-contact%s", 2);
    }

    public List<String> createContacts(SoapTransport soapTrans, String nameTemplate, int numContacts)
    throws ServiceException, IOException, HttpException{
        List<String> contactIds = Lists.newArrayListWithExpectedSize(numContacts);
        for (int i = 1; i<=numContacts; i++) {
            contactIds.add(createContact(soapTrans, String.format(nameTemplate, i)).getId());
        }
        return contactIds;
    }

    public ContactInfo createContact(SoapTransport soapTrans, String fullName) throws ServiceException, IOException, HttpException {
        ContactSpec contactSpec = new ContactSpec();
        contactSpec.addAttr(NewContactAttr.fromNameAndValue(ContactConstants.A_fullName, fullName));
        CreateContactResponse resp = invokeJaxb(soapTrans, new CreateContactRequest(contactSpec));
        assertNotNull(String.format("CreateContactResponse when creating %s should not be null", fullName), resp);
        ContactInfo contact = resp.getContact();
        assertNotNull(String.format("CreateContactResponse/cn when creating %s should not be null", fullName), contact);
        return contact;
    }

    public ContactInfo createContactGroup(SoapTransport soapTrans, String fullName, String...memberIds)
            throws ServiceException, IOException, HttpException {
        ContactSpec contactSpec = new ContactSpec();
        contactSpec.addAttr(NewContactAttr.fromNameAndValue(ContactConstants.A_fullName, fullName));
        contactSpec.addAttr(NewContactAttr.fromNameAndValue(ContactConstants.A_type, ContactConstants.TYPE_GROUP));
        for (String id : memberIds) {
            contactSpec.addContactGroupMember(NewContactGroupMember.createForTypeAndValue(
                    ContactGroup.Member.Type.CONTACT_REF.getSoapEncoded(), id));
        }
        CreateContactResponse resp = invokeJaxb(soapTrans, new CreateContactRequest(contactSpec));
        assertNotNull(String.format(
                "CreateContactResponse when creating ContactGroup %s should not be null", fullName), resp);
        ContactInfo contact = resp.getContact();
        assertNotNull(String.format(
                "CreateContactResponse/cn when creating ContactGroup %s should not be null", fullName), contact);
        return contact;
    }

    public ContactInfo getContactWithId(List<ContactInfo> contacts, String id) {
        for (ContactInfo contact : contacts) {
            if (id.equals(contact.getId())) {
                return contact;
            }
        }
        return null;
    }

    @Test
    public void testWithJaxb() throws Exception {
        GetContactsRequest getContactsReq = new GetContactsRequest();
        getContactsReq.addContact(new Id(ids.get(0)));
        getContactsReq.addContact(new Id(ids.get(1)));
        getContactsReq.setDerefGroupMember(Boolean.TRUE);
        GetContactsResponse getContactsResp = invokeJaxb(transport, getContactsReq, SoapProtocol.SoapJS);

        List<com.zimbra.soap.mail.type.ContactInfo> contacts = getContactsResp.getContacts();
        assertEquals("List of contacts returned when ask for 2 contacts by ID", 2, contacts.size());

        getContactsReq = new GetContactsRequest();
        getContactsReq.addContact(new Id(ids.get(0)));
        getContactsReq.setDerefGroupMember(Boolean.TRUE);
        getContactsResp = invokeJaxb(transport, getContactsReq);

        contacts = getContactsResp.getContacts();
        assertEquals("List of contacts returned when ask for 1 contact by ID", 1, contacts.size());
    }

    @Test
    public void testComaSeparatedIds() throws Exception {
        Element req = new JSONElement(MailConstants.GET_CONTACTS_REQUEST);
        req.addAttribute(MailConstants.A_SYNC, true);
        req.addNonUniqueElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, String.format("%s,%s",ids.get(0), ids.get(1)));
        Element resp = transport.invoke(req);
        GetContactsResponse getContactsResp = JaxbUtil.elementToJaxb(resp);
        List<ContactInfo> contacts = getContactsResp.getContacts();
        assertEquals(2, contacts.size());
    }

    private void checkMemberOf(List<ContactInfo> contacts, String id, String...grpIds) {
        ContactInfo contact = getContactWithId(contacts, id);
        assertNotNull(String.format("No contact in list with id=%s", id), contact);
        List<ContactAttr> attrs = contact.getAttrs();
        String name = "unknown";
        for (ContactAttr attr : attrs) {
            if (ContactConstants.A_fullName.equals(attr.getKey())) {
                name = attr.getValue();
                break;
            }
        }
        Collection<String> memberOf = contact.getMemberOf();
        List<String> groups = Lists.newArrayList(grpIds);
        if (groups.isEmpty()) {
            assertTrue("memberOf info should be empty or null", (memberOf == null) || memberOf.isEmpty());
            return;
        }
        assertNotNull(String.format("No memberOf for contact in list with id=%s/name=%s", id, name), memberOf);
        assertEquals(String.format("Number of groups contact with id=%s/name=%s is a member of", id, name),
                groups.size(), memberOf.size());
        for (String grp : groups) {
            assertTrue(String.format("Contact with id=%s/name=%s should be a member of %s", id, name, grp),
                    memberOf.contains(grp));
        }
    }

    private void checkGetContactsMemberOf(SoapTransport soapTrans, GetContactsRequest getContactsReq,
        List<ContactInfo> groups, List<String> remIds, SoapProtocol proto) throws ServiceException, IOException, HttpException {
        GetContactsResponse getContactsResp;
        getContactsResp = invokeJaxb(soapTrans, getContactsReq, proto);
        List<ContactInfo> contacts = getContactsResp.getContacts();
        assertEquals("List of contacts returned when ask for contacts by ID", 2 + remIds.size(), contacts.size());
        checkMemberOf(contacts, ids.get(0), groups.get(0).getId(), groups.get(1).getId(), groups.get(2).getId());
        checkMemberOf(contacts, ids.get(1), groups.get(1).getId());
        checkMemberOf(contacts, remIds.get(0), remIds.get(4), remIds.get(5));
        getContactsReq.setIncludeMemberOf(false);
        getContactsResp = invokeJaxb(soapTrans, getContactsReq, proto);
        contacts = getContactsResp.getContacts();
        assertEquals("List of contacts returned when ask for contacts by ID without memberOf info",
                2 + remIds.size(), contacts.size());
        checkMemberOf(contacts, ids.get(0));
        checkMemberOf(contacts, ids.get(1));
        checkMemberOf(contacts, remIds.get(0));
        getContactsReq.setIncludeMemberOf(true);  // Back to what it should have been at the start
    }

    @Test
    public void contactGroupMembership() throws Exception {
        Account acct2 = TestUtil.createAccount(USER2_NAME);
        transport2 = authUser(acct2.getName());
        List<ContactInfo> groups = Lists.newArrayListWithExpectedSize(3);
        groups.add(createContactGroup(transport, USER_NAME + "-GROUP1", ids.get(0)));
        groups.add(createContactGroup(transport, USER_NAME + "-GROUP2", ids.get(0), ids.get(1)));
        groups.add(createContactGroup(transport, USER_NAME + "-GROUP3", ids.get(0)));
        List<String> u2ids = createContacts(transport2, USER2_NAME + "-contact%s", 4);
        List<ContactInfo> u2grps = Lists.newArrayListWithExpectedSize(3);
        u2grps.add(createContactGroup(transport2, USER2_NAME + "-GROUP1", u2ids.get(0)));
        u2grps.add(createContactGroup(transport2, USER2_NAME + "-GROUP2", u2ids.get(0), u2ids.get(1)));
        u2grps.add(createContactGroup(transport2, USER2_NAME + "-GROUP3", u2ids.get(3)));
        ZMailbox u1zmbox = TestUtil.getZMailbox(USER_NAME);
        ZMailbox u2zmbox = TestUtil.getZMailbox(USER2_NAME);
        TestUtil.createMountpoint(u2zmbox, "/Contacts", u1zmbox, "U2-Contacts");
        List<String> remIds = Lists.newArrayList();
        for (String u2id : u2ids) {
            remIds.add(ItemIdentifier.fromOwnerAndRemoteId(acct2.getId(), u2id).toString(acct.getId()));
        }
        for (ContactInfo grp : u2grps) {
            remIds.add(ItemIdentifier.fromOwnerAndRemoteId(acct2.getId(), grp.getId()).toString(acct.getId()));
        }
        GetContactsRequest getContactsReq = new GetContactsRequest();
        getContactsReq.addContact(new Id(ids.get(0)));
        getContactsReq.addContact(new Id(ids.get(1)));
        for (String remId : remIds) {
            getContactsReq.addContact(new Id(remId));
        }
        getContactsReq.setIncludeMemberOf(true);
        getContactsReq.setSortBy(ZimbraSortBy.nameAsc.name());
        checkGetContactsMemberOf(transport, getContactsReq, groups, remIds, SoapProtocol.SoapJS);
        checkGetContactsMemberOf(transport, getContactsReq, groups, remIds, SoapProtocol.Soap12);
        SearchRequest searchReq = new SearchRequest();
        searchReq.setIncludeMemberOf(true);
        searchReq.setQuery("in:Contacts or in:\"U2-Contacts\"");
        searchReq.setSearchTypes("contact");
        searchReq.setSortBy(ZimbraSortBy.nameAsc.name());
        SearchResponse searchResp = invokeJaxb(transport, searchReq);
        assertNotNull("SearchResponse should not be null", searchResp);
        List<SearchHit> hits = searchResp.getSearchHits();
        assertNotNull("SearchResponse Hits should not be null", hits);
        List<ContactInfo> cHits = Lists.newArrayListWithExpectedSize(hits.size());
        for (SearchHit hit : hits) {
            if (hit instanceof ContactInfo) {
                cHits.add((ContactInfo) hit);
            } else {
                fail(String.format("SearchHit %s should be a ContactInfo", hit.getClass().getName()));
            }
        }
        checkMemberOf(cHits, ids.get(0), groups.get(0).getId(), groups.get(1).getId(), groups.get(2).getId());
        checkMemberOf(cHits, ids.get(1), groups.get(1).getId());
        checkMemberOf(cHits, remIds.get(0), remIds.get(4), remIds.get(5));
    }
}
