/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.DelegateAuthResponse;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.zclient.ZContact;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMailbox.ContactSortBy;

import junit.framework.TestCase;


public class TestContacts
extends TestCase {

    private static final String NAME_PREFIX = TestContacts.class.getSimpleName();
    private static final String USER_NAME = "user1";
    String mOriginalMaxContacts;
    
    public void setUp()
    throws Exception {
        cleanUp();
        mOriginalMaxContacts = TestUtil.getAccountAttr(USER_NAME, Provisioning.A_zimbraContactMaxNumEntries);
    }
    /**
     * Confirms that volumeId is not set for contacts.
     */
    public void testVolumeId()
    throws Exception {
        Account account = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user1"));
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE type = %d AND volume_id IS NOT NULL",
            DbMailItem.getMailItemTableName(mbox), MailItem.TYPE_CONTACT);
        DbResults results = DbUtil.executeQuery(sql);
        int count = results.getInt(1);
        assertEquals("Found non-null volumeId values for contacts", 0, count);
    }
    
    /**
     * Confirms that {@link Provisioning#A_zimbraContactMaxNumEntries} is enforced (bug 29627).
     */
    public void testMaxContacts()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZContact> contacts = mbox.getAllContacts(null, ContactSortBy.nameAsc, false, null);
        int max = contacts.size() + 2;
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraContactMaxNumEntries, Integer.toString(max));
        Map<String, String> attrs = new HashMap<String, String>();
        int i;
        for (i = 1; i <= 10; i++) {
            attrs.put("fullName", NAME_PREFIX + i);
            try {
                mbox.createContact(Integer.toString(Mailbox.ID_FOLDER_CONTACTS), null, attrs);
            } catch (SoapFaultException e) {
                assertEquals(MailServiceException.TOO_MANY_CONTACTS, e.getCode());
                break;
            }
        }
        assertEquals("Unexpected contact number", 3, i);
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraContactMaxNumEntries, mOriginalMaxContacts);
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
    
    public static void main(String[] args) throws Exception {
    	String username = "user1";
    	String id = args[0];
    	String name = args[1];
    	String value = args[2];
    	String rev = args[3];
		SoapProvisioning prov = new SoapProvisioning();            
		CliUtil.toolSetup();
		String server = LC.zimbra_zmprov_default_soap_server.value();
		prov.soapSetURI(URLUtil.getAdminURL(server));
		prov.soapAdminAuthenticate(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
		DelegateAuthResponse dar = prov.delegateAuth(AccountBy.name, username, 60*60*24);
		Account acct = prov.get(AccountBy.name, username);
		Server remoteServer = prov.getServer(acct);
		String url = URLUtil.getSoapURL(remoteServer, true);
		SoapHttpTransport transport = new SoapHttpTransport(url);
		ZAuthToken zat = dar.getAuthToken();
		transport.setAuthToken(zat);
		transport.setTargetAcctName(username);
		try {
			Element req = new Element.XMLElement(MailConstants.CONTACT_ACTION_REQUEST);
			Element action = req.addElement(MailConstants.E_ACTION);
			action.addAttribute(MailConstants.A_OPERATION, "update");
			action.addAttribute(MailConstants.A_ID, id);
			Element a = action.addElement(MailConstants.E_A);
			a.addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
			a.addText(value);
			transport.invoke(req, false, true, null, rev, null);
		} finally {
			if (transport != null)
				transport.shutdown();
		}
    }
}
