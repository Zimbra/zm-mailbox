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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.CreateContactRequest;
import com.zimbra.soap.mail.message.CreateContactResponse;
import com.zimbra.soap.mail.message.GetContactsRequest;
import com.zimbra.soap.mail.message.GetContactsResponse;
import com.zimbra.soap.mail.type.ContactSpec;
import com.zimbra.soap.mail.type.NewContactAttr;
import com.zimbra.soap.type.Id;

public class TestGetContactsRequest extends SoapTest {
    private Account acct;
    private List<String> ids;
    private SoapTransport transport;
    private static final String USER_NAME = TestGetContactsRequest.class.getSimpleName() + "user1";

    @After
    public void cleanup() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    @Before
    public void setUp() throws Exception {
        ids = new ArrayList<String>();
        acct = TestUtil.createAccount(USER_NAME);

        //create contacts
        transport = authUser(acct.getName());
        ContactSpec contactSpec = new ContactSpec();
        NewContactAttr contactAttr = new NewContactAttr(ContactConstants.A_fullName);
        contactAttr.setValue("TestGetContactsRequest-contact1");
        contactSpec.addAttr(contactAttr);
        CreateContactRequest createContactReq = new CreateContactRequest(contactSpec);
        CreateContactResponse createContactResp = invokeJaxb(transport, createContactReq);
        ids.add(createContactResp.getContact().getId());

        contactSpec = new ContactSpec();
        contactAttr = new NewContactAttr(ContactConstants.A_fullName);
        contactAttr.setValue("TestGetContactsRequest-contact2");
        contactSpec.addAttr(contactAttr);
        createContactReq = new CreateContactRequest(contactSpec);
        createContactResp = invokeJaxb(transport, createContactReq);
        ids.add(createContactResp.getContact().getId());
    }

    @Test
    public void testWithJaxb() throws Exception {
        GetContactsRequest getContactsReq = new GetContactsRequest();
        getContactsReq.addContact(new Id(ids.get(0)));
        getContactsReq.addContact(new Id(ids.get(1)));
        getContactsReq.setDerefGroupMember(Boolean.TRUE);
        GetContactsResponse getContactsResp = invokeJaxb(transport, getContactsReq, SoapProtocol.SoapJS);

        List<com.zimbra.soap.mail.type.ContactInfo> contacts = getContactsResp.getContacts();
        assertEquals(2, contacts.size());

        getContactsReq = new GetContactsRequest();
        getContactsReq.addContact(new Id(ids.get(0)));
        getContactsReq.setDerefGroupMember(Boolean.TRUE);
        getContactsResp = invokeJaxb(transport, getContactsReq, SoapProtocol.SoapJS);

        contacts = getContactsResp.getContacts();
        assertEquals(1, contacts.size());
    }

    @Test
    public void testComaSeparatedIds() throws Exception {
        Element req = new JSONElement(MailConstants.GET_CONTACTS_REQUEST);
        req.addAttribute(MailConstants.A_SYNC, true);
        req.addNonUniqueElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, String.format("%s,%s",ids.get(0), ids.get(1)));
        Element resp = transport.invoke(req);
        GetContactsResponse getContactsResp = JaxbUtil.elementToJaxb(resp);
        List<com.zimbra.soap.mail.type.ContactInfo> contacts = getContactsResp.getContacts();
        assertEquals(2, contacts.size());
    }
}
