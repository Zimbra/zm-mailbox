/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.qa.QA.Bug;
import com.zimbra.soap.account.message.SearchGalRequest;
import com.zimbra.soap.account.message.SearchGalResponse;
import com.zimbra.soap.account.type.ContactInfo;
import com.zimbra.soap.mail.message.CreateContactRequest;
import com.zimbra.soap.mail.message.CreateContactResponse;
import com.zimbra.soap.mail.message.GetContactsRequest;
import com.zimbra.soap.mail.message.GetContactsResponse;
import com.zimbra.soap.mail.type.ContactGroupMember;
import com.zimbra.soap.mail.type.ContactSpec;
import com.zimbra.soap.mail.type.NewContactAttr;
import com.zimbra.soap.type.Id;

public class TestContactGroup extends SoapTest {
    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    private static Account acct;
    private static Account memberAcct;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
        
        acct = provUtil.createAccount("acct", domain);
        memberAcct = provUtil.createAccount("member-acct", domain);
        
        GalTestUtil.enableGalSyncAccount(prov, domain.getName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {

        // use soap to delete account, the mailbox will also be deleted
        if (acct != null) { // test null in case init failed
            provUtil.deleteAccount(acct);
        }
        
        if (memberAcct != null) { // test null in case init failed
            provUtil.deleteAccount(memberAcct);
        }
        
        Cleanup.deleteAll(baseDomainName());
    }
    
    @Test
    @Bug(bug=70558)
    public void createAndGetContactGroup() throws Exception {
        SoapTransport transport = authUser(acct.getName());
        
        /*
         * search gal to get ref of the member account in GAL
         */
        SearchGalRequest searchGalReq = new SearchGalRequest();
        searchGalReq.setName(memberAcct.getName());
        SearchGalResponse searchGalResp = invokeJaxb(transport, searchGalReq);
        List<ContactInfo> entries = searchGalResp.getContacts();
        
        assertEquals(1, entries.size());
        ContactInfo galEntry = entries.get(0);
        String galMemberRef = galEntry.getReference();
        
        /*
         * create a contact group
         */
        ContactGroupMember contactGroupMemer = 
            ContactGroupMember.createForTypeValueAndContact(
            ContactGroup.Member.Type.GAL_REF.getSoapEncoded(), galMemberRef, null);
        
        NewContactAttr contactAttr = new NewContactAttr(ContactConstants.A_type);
        contactAttr.setValue(ContactConstants.TYPE_GROUP);
        
        ContactSpec contactSpec = new ContactSpec();
        contactSpec.addAttr(contactAttr);
        contactSpec.addContactGroupMember(contactGroupMemer);
        
        CreateContactRequest createContactReq = new CreateContactRequest(contactSpec);
        CreateContactResponse createContactResp = invokeJaxb(transport, createContactReq);
        
        String contactGroupId = createContactResp.getContact().getId();
        
        /*
         * get the contact group, derefed
         */
        GetContactsRequest getContactsReq = new GetContactsRequest();
        getContactsReq.addContact(new Id(contactGroupId));
        getContactsReq.setDerefGroupMember(Boolean.TRUE);
        GetContactsResponse getContactsResp = invokeJaxb(transport, getContactsReq, SoapProtocol.SoapJS);
        
        List<com.zimbra.soap.mail.type.ContactInfo> contacts = getContactsResp.getContacts();
        assertEquals(1, contacts.size());
        com.zimbra.soap.mail.type.ContactInfo contact = contacts.get(0);
        List<ContactGroupMember> members = contact.getContactGroupMembers();
        assertEquals(1, members.size());
        
        ContactGroupMember member = members.get(0);
        String memberType = member.getType();
        String memberValue = member.getValue();
        assertEquals(ContactGroup.Member.Type.GAL_REF.getSoapEncoded(), memberType);
        assertEquals(galMemberRef, memberValue);
    }
    
}
